package com.kgps.result

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object BlankSheetLocalStore {
    private const val PREFS_NAME = "blank_sheet_prefs"
    private const val CONFIG_KEY = "blank_sheet_config_v1"

    fun saveConfig(ctx: Context, cfg: BlankSheetConfig) {
        val colsArray = JSONArray().apply {
            cfg.columns.forEach { col ->
                put(
                    JSONObject().apply {
                        put("id", col.id)
                        put("titleBn", col.titleBn)
                        put("widthWeight", col.widthWeight.toDouble())
                        put("fillWith", col.fillWith.name)
                        put("align", col.align.name)
                        if (col.compKey != null) put("compKey", col.compKey)
                        put("isCustom", col.isCustom)
                        put("groupBn", col.groupBn ?: "")
                        put("pickerLabelBn", col.pickerLabelBn)
                    }
                )
            }
        }
        val wdObj = JSONObject().apply {
            cfg.workingDaysByTerm.forEach { (k, v) ->
                put(k.toString(), v)
            }
        }
        val obj = JSONObject().apply {
            if (cfg.subjectKey != null) put("subjectKey", cfg.subjectKey) else put("subjectKeyIsNull", true)
            put("customSubjectTitle", cfg.customSubjectTitle)
            put("term", cfg.term)
            put("customTermTitle", cfg.customTermTitle)
            put("columns", colsArray)
            put("rowsPerPage", cfg.rowsPerPage)
            put("rowFitMode", cfg.rowFitMode.name)
            put("autoFitPageCount", cfg.autoFitPageCount)
            put("orientation", cfg.orientation.name)
            put("numeralStyle", cfg.numeralStyle.name)
            put("autoFillRollAndName", cfg.autoFillRollAndName)
            put("showSubjectMaxInHeader", cfg.showSubjectMaxInHeader)
            put("extraBlankRowsPerClass", cfg.extraBlankRowsPerClass)
            put("fillRemainingPageRows", cfg.fillRemainingPageRows)
            put("showSharedWorkingDaysHeader", cfg.showSharedWorkingDaysHeader)
            put("workingDaysByTerm", wdObj)
            put("includeQrCode", cfg.includeQrCode)
            put("includeBarcode", cfg.includeBarcode)
            put("bottomCodeStyle", cfg.bottomCodeStyle.name)
        }
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(CONFIG_KEY, obj.toString())
            .apply()
    }

    fun loadConfig(ctx: Context, fallback: BlankSheetConfig): BlankSheetConfig {
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(CONFIG_KEY, null) ?: return fallback
        return try {
            val o = JSONObject(raw)
            val restoredCols = mutableListOf<BlankSheetColumn>()
            val colsArr = o.optJSONArray("columns")
            if (colsArr != null) {
                for (i in 0 until colsArr.length()) {
                    val cObj = colsArr.optJSONObject(i) ?: continue
                    val id = cObj.optString("id")
                    if (id.isBlank()) continue
                    val catalogCol = BlankSheetCatalog.byId(id)
                    if (catalogCol != null && !cObj.optBoolean("isCustom", false)) {
                        val savedWeight = cObj.optDouble("widthWeight", catalogCol.widthWeight.toDouble())
                            .toFloat()
                            .coerceIn(0.4f, 4.0f)
                        val savedTitle = cObj.optString("titleBn").takeIf { it.isNotBlank() } ?: catalogCol.titleBn
                        val savedGroup = if (cObj.has("groupBn")) {
                            cObj.optString("groupBn").takeIf { it.isNotBlank() }
                        } else {
                            catalogCol.groupBn
                        }
                        val savedPicker = cObj.optString("pickerLabelBn").takeIf { it.isNotBlank() }
                            ?: catalogCol.pickerLabelBn
                        restoredCols.add(
                            catalogCol.copy(
                                titleBn = savedTitle,
                                widthWeight = savedWeight,
                                groupBn = savedGroup,
                                pickerLabelBn = savedPicker
                            )
                        )
                    } else {
                        val title = cObj.optString("titleBn")
                        if (title.isNotBlank()) {
                            restoredCols.add(
                                BlankSheetColumn(
                                    id = id,
                                    titleBn = title,
                                    widthWeight = cObj.optDouble("widthWeight", 1.0).toFloat().coerceIn(0.4f, 4.0f),
                                    fillWith = runCatching { BlankColFill.valueOf(cObj.optString("fillWith")) }
                                        .getOrDefault(BlankColFill.NONE),
                                    align = runCatching { BlankColAlign.valueOf(cObj.optString("align")) }
                                        .getOrDefault(BlankColAlign.CENTER),
                                    compKey = cObj.optString("compKey").takeIf { it.isNotBlank() },
                                    isCustom = cObj.optBoolean("isCustom", true),
                                    groupBn = cObj.optString("groupBn").takeIf { it.isNotBlank() },
                                    pickerLabelBn = cObj.optString("pickerLabelBn", title).ifBlank { title }
                                )
                            )
                        }
                    }
                }
            }
            val wdMap = mutableMapOf<Int, String>()
            o.optJSONObject("workingDaysByTerm")?.let { wdObj ->
                val keys = wdObj.keys()
                while (keys.hasNext()) {
                    val kStr = keys.next()
                    val kInt = kStr.toIntOrNull() ?: continue
                    val v = wdObj.optString(kStr)
                    if (v.isNotBlank()) wdMap[kInt] = v
                }
            }
            val subjectKey = if (o.optBoolean("subjectKeyIsNull", false)) {
                null
            } else {
                o.optString("subjectKey", fallback.subjectKey ?: "BAN").ifBlank { null }
            }
            fallback.copy(
                subjectKey = subjectKey,
                customSubjectTitle = o.optString("customSubjectTitle", fallback.customSubjectTitle),
                term = o.optInt("term", fallback.term).coerceIn(-1, 3),
                customTermTitle = o.optString("customTermTitle", fallback.customTermTitle),
                columns = restoredCols.ifEmpty { fallback.columns },
                rowsPerPage = o.optInt("rowsPerPage", fallback.rowsPerPage).coerceIn(5, 50),
                rowFitMode = runCatching { BlankSheetRowFitMode.valueOf(o.optString("rowFitMode")) }
                    .getOrDefault(fallback.rowFitMode),
                autoFitPageCount = o.optInt("autoFitPageCount", fallback.autoFitPageCount).coerceIn(1, 3),
                orientation = runCatching { BlankSheetOrientation.valueOf(o.optString("orientation")) }
                    .getOrDefault(fallback.orientation),
                numeralStyle = runCatching { BlankNumeralStyle.valueOf(o.optString("numeralStyle")) }
                    .getOrDefault(fallback.numeralStyle),
                autoFillRollAndName = o.optBoolean("autoFillRollAndName", fallback.autoFillRollAndName),
                showSubjectMaxInHeader = o.optBoolean("showSubjectMaxInHeader", fallback.showSubjectMaxInHeader),
                extraBlankRowsPerClass = o.optInt("extraBlankRowsPerClass", fallback.extraBlankRowsPerClass).coerceIn(0, 20),
                fillRemainingPageRows = o.optBoolean("fillRemainingPageRows", fallback.fillRemainingPageRows),
                showSharedWorkingDaysHeader = o.optBoolean("showSharedWorkingDaysHeader", fallback.showSharedWorkingDaysHeader),
                workingDaysByTerm = wdMap.ifEmpty { fallback.workingDaysByTerm },
                includeQrCode = o.optBoolean("includeQrCode", fallback.includeQrCode),
                includeBarcode = o.optBoolean("includeBarcode", fallback.includeBarcode),
                bottomCodeStyle = runCatching { BlankBottomCodeStyle.valueOf(o.optString("bottomCodeStyle")) }
                    .getOrDefault(fallback.bottomCodeStyle)
            )
        } catch (_: Exception) {
            fallback
        }
    }
}
