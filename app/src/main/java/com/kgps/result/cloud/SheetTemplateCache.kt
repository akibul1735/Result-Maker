package com.kgps.result.cloud

import android.content.Context
import com.kgps.result.BlankNumeralStyle
import com.kgps.result.BlankSheetQrMeta
import com.kgps.result.SheetQrCodec
import com.kgps.result.ai.ClassSheetImportScope
import com.kgps.result.ai.ClassSheetKind
import org.json.JSONArray
import org.json.JSONObject

/**
 * ডিভাইসে ফাঁকা শীট টেমপ্লেটগুলোর অফলাইন ক্যাশ (অ্যাপসে শীট তৈরি বা অনলাইন থেকে নামানোর পর জমা থাকে)।
 */
class SheetTemplateCache(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val lock = Any()

    fun get(templateId: String): BlankSheetQrMeta? {
        val cleanId = templateId.trim().uppercase()
        if (cleanId.isBlank()) return null
        val map = readAllLocked()
        return map[cleanId]
    }

    fun getAll(): List<BlankSheetQrMeta> = synchronized(lock) {
        readAllLocked().values.toList()
    }

    fun put(meta: BlankSheetQrMeta) {
        val tid = meta.templateId.trim().uppercase().ifBlank {
            SheetQrCodec.computeTemplateId(meta)
        }
        val normalized = meta.copy(templateId = tid)
        synchronized(lock) {
            val map = readAllLocked().toMutableMap()
            map.remove(tid) // Re-insert at tail for LRU ordering
            map[tid] = normalized
            while (map.size > MAX_TEMPLATES) {
                val oldestKey = map.keys.firstOrNull() ?: break
                map.remove(oldestKey)
            }
            writeAllLocked(map)
        }
    }

    fun putAll(items: List<BlankSheetQrMeta>) {
        if (items.isEmpty()) return
        synchronized(lock) {
            val map = readAllLocked().toMutableMap()
            items.forEach { item ->
                val tid = item.templateId.trim().uppercase().ifBlank {
                    SheetQrCodec.computeTemplateId(item)
                }
                map.remove(tid)
                map[tid] = item.copy(templateId = tid)
            }
            while (map.size > MAX_TEMPLATES) {
                val oldestKey = map.keys.firstOrNull() ?: break
                map.remove(oldestKey)
            }
            writeAllLocked(map)
        }
    }

    private fun readAllLocked(): LinkedHashMap<String, BlankSheetQrMeta> {
        val raw = prefs.getString(KEY_TEMPLATES_JSON, null) ?: return linkedMapOf()
        val out = linkedMapOf<String, BlankSheetQrMeta>()
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return out
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val obj = root.optJSONObject(key) ?: continue
            parseMeta(key, obj)?.let { out[key] = it }
        }
        return out
    }

    private fun writeAllLocked(map: Map<String, BlankSheetQrMeta>) {
        val root = JSONObject()
        map.forEach { (id, meta) ->
            root.put(id, serializeMeta(meta))
        }
        prefs.edit().putString(KEY_TEMPLATES_JSON, root.toString()).apply()
    }

    private fun serializeMeta(meta: BlankSheetQrMeta): JSONObject =
        SheetTemplateJson.serialize(meta)

    private fun parseMeta(tid: String, obj: JSONObject): BlankSheetQrMeta? =
        SheetTemplateJson.parse(tid, obj)

    companion object {
        private const val PREFS_NAME = "blank_sheet_template_cache_v1"
        private const val KEY_TEMPLATES_JSON = "templates_map"
        private const val MAX_TEMPLATES = 250
    }
}

object SheetTemplateJson {
    fun serialize(meta: BlankSheetQrMeta): JSONObject = JSONObject().apply {
        val tid = meta.templateId.trim().uppercase().ifBlank {
            SheetQrCodec.computeTemplateId(meta)
        }
        put("templateId", tid)
        put("cls", meta.cls.coerceIn(1, 20))
        put("subjectKey", meta.subjectKey.orEmpty())
        put("term", meta.term.coerceIn(-1, 3))
        put("sheetKind", meta.sheetKind.name)
        put("importScope", meta.importScope.name)
        put("columnIds", JSONArray(meta.columnIds))
        put("customColumnTitles", JSONArray(meta.customColumnTitles))
        put("numeralStyle", meta.numeralStyle.name)
        put("showSubjectMaxInHeader", meta.showSubjectMaxInHeader)
        put("workingDaysByTerm", JSONObject().apply {
            meta.workingDaysByTerm.forEach { (k, v) -> put(k.toString(), v) }
        })
        put("pageIndex", meta.pageIndex.coerceAtLeast(1))
        put("totalPages", meta.totalPages.coerceAtLeast(1))
        put("refCode", meta.refCode)
    }

    fun parse(fallbackTid: String, obj: JSONObject): BlankSheetQrMeta? {
        val tid = obj.optString("templateId", fallbackTid).trim().uppercase()
        if (tid.isBlank()) return null
        val cls = obj.optInt("cls", -1).takeIf { it in 1..20 } ?: return null
        val sub = obj.optString("subjectKey", "").trim().takeIf { it.isNotEmpty() }
        val term = obj.optInt("term", 1).coerceIn(-1, 3)
        val kind = runCatching {
            ClassSheetKind.valueOf(obj.optString("sheetKind", ClassSheetKind.SINGLE_TERM_MARKS.name))
        }.getOrDefault(ClassSheetKind.SINGLE_TERM_MARKS)
        val scope = runCatching {
            ClassSheetImportScope.valueOf(obj.optString("importScope", ClassSheetImportScope.AUTO.name))
        }.getOrDefault(ClassSheetImportScope.AUTO)
        val numStyle = runCatching {
            BlankNumeralStyle.valueOf(obj.optString("numeralStyle", BlankNumeralStyle.BENGALI.name))
        }.getOrDefault(BlankNumeralStyle.BENGALI)

        val colArr = obj.optJSONArray("columnIds") ?: JSONArray()
        val colIds = (0 until colArr.length()).mapNotNull { colArr.optString(it)?.takeIf(String::isNotBlank) }
        if (colIds.isEmpty()) return null

        val cuArr = obj.optJSONArray("customColumnTitles") ?: JSONArray()
        val customTitles = (0 until cuArr.length()).mapNotNull { cuArr.optString(it) }

        val wdObj = obj.optJSONObject("workingDaysByTerm")
        val wdMap = linkedMapOf<Int, String>()
        if (wdObj != null) {
            val wKeys = wdObj.keys()
            while (wKeys.hasNext()) {
                val wk = wKeys.next()
                val t = wk.toIntOrNull()
                val d = wdObj.optString(wk, "").filter { it.isDigit() }
                if (t in 1..3 && d.isNotEmpty()) wdMap[t!!] = d
            }
        }

        return BlankSheetQrMeta(
            version = 2,
            templateId = tid,
            cls = cls,
            subjectKey = sub,
            term = term,
            sheetKind = kind,
            importScope = scope,
            columnIds = colIds,
            customColumnTitles = customTitles,
            numeralStyle = numStyle,
            showSubjectMaxInHeader = obj.optBoolean("showSubjectMaxInHeader", true),
            workingDaysByTerm = wdMap,
            pageIndex = obj.optInt("pageIndex", 1).coerceAtLeast(1),
            totalPages = obj.optInt("totalPages", 1).coerceAtLeast(1),
            refCode = obj.optString("refCode", "")
        )
    }
}
