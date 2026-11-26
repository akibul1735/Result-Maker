package com.kgps.result.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Base64
import com.kgps.result.BlankSheetCatalog
import com.kgps.result.BlankSheetQrMeta
import com.kgps.result.RELIGIONS
import com.kgps.result.Repo
import com.kgps.result.SheetQrCodec
import com.kgps.result.bn
import com.kgps.result.classNameBn
import com.kgps.result.fmtEn
import com.kgps.result.fromBn
import com.kgps.result.subjectsFor
import com.kgps.result.termNameBn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLException
import kotlin.math.abs
import kotlin.math.ceil

data class GeminiModelSuggestion(
    val id: String,
    val title: String,
    val badge: String,
    val description: String
)

data class DiscoveredGeminiModel(
    val id: String,
    val displayName: String = "",
    val description: String = ""
)

data class GeminiConnectionCheckResult(
    val ok: Boolean,
    val message: String,
    val availableModels: List<DiscoveredGeminiModel> = emptyList()
)

/**
 * ডিভাইস-লোকাল ম্যানুয়াল Gemini API Key এবং মডেল কনফিগারেশন।
 * ব্যবহারকারীর নিজস্ব Google AI Studio API Key শুধুমাত্র এই ডিভাইসেই সংরক্ষিত থাকে।
 */
object GeminiApiConfig {
    const val MAX_ORDERED_MODELS = 3

    private const val PREF_NAME = "gemini_api_prefs"
    private const val KEY_API_KEY = "gemini_api_key"
    private const val KEY_MODEL = "gemini_model"
    private const val KEY_FALLBACK_MODELS = "gemini_fallback_models_json"
    private const val KEY_ORDERED_MODELS = "gemini_ordered_models_json"
    private const val KEY_CACHED_MODELS = "gemini_cached_models_json"
    private const val KEY_CUSTOM_MODELS = "gemini_custom_models_json"

    internal val LEGACY_INVALID_MODELS: Set<String> = setOf(
        "gemini-3.8-flash",
        "gemini-4-argon",
        "gemini-4-hydrogen"
    )

    fun normalizeApiKey(raw: String): String = raw.trim()

    fun normalizeModelId(raw: String, cachedIds: Set<String> = emptySet()): String {
        val cleaned = raw.trim()
            .removePrefix("models/")
            .trim()
        if (cleaned.isBlank()) return ""
        if (cleaned in LEGACY_INVALID_MODELS && cleaned !in cachedIds) return ""
        if (isExcludedModelId(cleaned) && cleaned !in cachedIds) return ""
        return cleaned
    }

    fun normalizeOrderedModels(
        rawModels: List<String>,
        cachedIds: Set<String> = emptySet()
    ): List<String> {
        val out = mutableListOf<String>()
        for (raw in rawModels) {
            val cleaned = normalizeModelId(raw, cachedIds)
            if (cleaned.isBlank() || cleaned in out) continue
            out.add(cleaned)
            if (out.size >= MAX_ORDERED_MODELS) break
        }
        return out
    }

    fun toggleOrderedModel(
        current: List<String>,
        rawModelId: String,
        cachedIds: Set<String> = emptySet()
    ): Pair<List<String>, Boolean> {
        val clean = normalizeModelId(rawModelId, cachedIds)
        val normalizedCurrent = normalizeOrderedModels(current, cachedIds)
        if (clean.isBlank()) return normalizedCurrent to false
        return if (clean in normalizedCurrent) {
            normalizedCurrent.filter { it != clean } to true
        } else if (normalizedCurrent.size < MAX_ORDERED_MODELS) {
            (normalizedCurrent + clean) to true
        } else {
            normalizedCurrent to false
        }
    }

    fun assignOrderedModelSlot(
        current: List<String>,
        slotIndex: Int,
        rawModelId: String?,
        cachedIds: Set<String> = emptySet()
    ): List<String> {
        val normalized = normalizeOrderedModels(current, cachedIds).toMutableList()
        val cleanedNew = rawModelId?.let { normalizeModelId(it, cachedIds) }.orEmpty()
        if (cleanedNew.isBlank()) {
            if (slotIndex in normalized.indices) {
                normalized.removeAt(slotIndex)
            }
            return normalized
        }
        val existingIdx = normalized.indexOf(cleanedNew)
        if (slotIndex in normalized.indices) {
            if (existingIdx >= 0 && existingIdx != slotIndex) {
                val displaced = normalized[slotIndex]
                normalized[slotIndex] = cleanedNew
                normalized[existingIdx] = displaced
            } else {
                normalized[slotIndex] = cleanedNew
            }
        } else if (existingIdx < 0 && normalized.size < MAX_ORDERED_MODELS) {
            normalized.add(cleanedNew)
        }
        return normalizeOrderedModels(normalized, cachedIds)
    }

    fun moveOrderedModelSlot(current: List<String>, index: Int, delta: Int): List<String> {
        val target = index + delta
        if (index !in current.indices || target !in current.indices || index == target) return current
        return current.toMutableList().apply {
            val item = removeAt(index)
            add(target, item)
        }
    }

    fun getApiKey(context: Context): String {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return normalizeApiKey(sp.getString(KEY_API_KEY, "").orEmpty())
    }

    fun getOrderedModels(context: Context): List<String> {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val cachedIds = getCachedModels(context).map { it.id }.toSet()

        if (sp.contains(KEY_ORDERED_MODELS)) {
            val raw = sp.getString(KEY_ORDERED_MODELS, null).orEmpty()
            val parsed = runCatching {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) add(arr.optString(i, ""))
                }
            }.getOrDefault(emptyList())
            return normalizeOrderedModels(parsed, cachedIds)
        }

        // পূর্ববর্তী সংস্করণ থেকে আপগ্রেড হলে কেবল আগে থেকেই সংরক্ষিত মডেলগুলোই মাইগ্রেট হবে;
        // নতুন ইনস্টলে কোনো ডিফল্ট/প্রস্তাবিত মডেল স্বয়ংক্রিয়ভাবে যুক্ত হবে না।
        val legacyList = mutableListOf<String>()
        val storedPrimary = sp.getString(KEY_MODEL, "").orEmpty()
        val cleanPrimary = normalizeModelId(storedPrimary, cachedIds)
        if (cleanPrimary.isNotBlank()) legacyList.add(cleanPrimary)
        val rawFallbacks = sp.getString(KEY_FALLBACK_MODELS, null).orEmpty()
        if (rawFallbacks.isNotBlank()) {
            runCatching {
                val arr = JSONArray(rawFallbacks)
                for (i in 0 until arr.length()) {
                    legacyList.add(arr.optString(i, ""))
                }
            }
        }
        return normalizeOrderedModels(legacyList, cachedIds)
    }

    fun saveOrderedModels(context: Context, rawModels: List<String>): List<String> {
        val cachedIds = getCachedModels(context).map { it.id }.toSet()
        val clean = normalizeOrderedModels(rawModels, cachedIds)
        val arr = JSONArray().apply { clean.forEach { put(it) } }
        val fallbackArr = JSONArray().apply { clean.drop(1).forEach { put(it) } }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ORDERED_MODELS, arr.toString())
            .putString(KEY_MODEL, clean.firstOrNull().orEmpty())
            .putString(KEY_FALLBACK_MODELS, fallbackArr.toString())
            .apply()
        return clean
    }

    fun getSelectedModel(context: Context): String =
        getOrderedModels(context).firstOrNull().orEmpty()

    fun getFallbackModels(
        context: Context,
        @Suppress("UNUSED_PARAMETER") primaryModel: String = ""
    ): List<String> = getOrderedModels(context).drop(1)

    fun isConfigured(context: Context): Boolean = getApiKey(context).isNotBlank()

    fun hasSelectedModels(context: Context): Boolean = getOrderedModels(context).isNotEmpty()

    fun getCachedModels(context: Context): List<DiscoveredGeminiModel> {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY_CACHED_MODELS, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val rawId = obj.optString("id", "").trim().removePrefix("models/").trim()
                    if (rawId.isNotBlank() && !isExcludedModelId(rawId)) {
                        add(
                            DiscoveredGeminiModel(
                                id = rawId,
                                displayName = obj.optString("displayName", ""),
                                description = obj.optString("description", "")
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveCachedModels(context: Context, models: List<DiscoveredGeminiModel>) {
        val arr = JSONArray()
        models.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id)
                put("displayName", m.displayName)
                put("description", m.description)
            })
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CACHED_MODELS, arr.toString())
            .apply()
    }

    fun getCustomModels(context: Context): List<String> {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val cachedIds = getCachedModels(context).map { it.id }.toSet()
        val raw = sp.getString(KEY_CUSTOM_MODELS, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val id = normalizeModelId(arr.optString(i, ""), cachedIds)
                    if (id.isNotBlank()) add(id)
                }
            }.distinct()
        }.getOrDefault(emptyList())
    }

    fun rememberCustomModel(context: Context, rawModel: String): List<String> {
        val cachedIds = getCachedModels(context).map { it.id }.toSet()
        val cleaned = normalizeModelId(rawModel, cachedIds)
        val existing = getCustomModels(context)
        if (cleaned.isBlank() || cleaned in cachedIds) return existing
        val updated = (listOf(cleaned) + existing.filter { it != cleaned }).take(10)
        val arr = JSONArray()
        updated.forEach { arr.put(it) }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_MODELS, arr.toString())
            .apply()
        return updated
    }

    fun removeCustomModel(context: Context, modelId: String): List<String> {
        val cleaned = modelId.trim().removePrefix("models/").trim()
        val updated = getCustomModels(context).filter { it != cleaned }
        val arr = JSONArray()
        updated.forEach { arr.put(it) }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_MODELS, arr.toString())
            .apply()
        return updated
    }

    fun saveApiKey(context: Context, rawApiKey: String): String {
        val key = normalizeApiKey(rawApiKey)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_API_KEY, key)
            .apply()
        return key
    }

    fun saveConfig(context: Context, rawApiKey: String, orderedModels: List<String>) {
        saveApiKey(context, rawApiKey)
        saveOrderedModels(context, orderedModels)
    }

    fun clearApiKey(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_API_KEY)
            .remove(KEY_CACHED_MODELS)
            .apply()
    }

    internal fun isExcludedModelId(id: String): Boolean {
        val lower = id.lowercase()
        val excludedTokens = listOf(
            "embedding", "aqa", "attribution", "tts", "imagen",
            "veo", "robotics", "computer-use", "deep-research",
            "native-audio", "-audio", "live-", "-image"
        )
        return excludedTokens.any { lower.contains(it) }
    }
}

enum class AiModelMode {
    GENERAL,
    FAST
}

data class AiChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val modelLabel: String? = null,
    val isPlaceholder: Boolean = false
)

data class ExtractedMarkItem(
    val key: String,
    val value: Double,
    val subjectLabel: String
)

data class ExtractedStudentInfo(
    val name: String? = null,
    val cls: Int? = null,
    val roll: Int? = null,
    val guardian: String? = null,
    val village: String? = null,
    val union: String? = null,
    val upazila: String? = null,
    val district: String? = null,
    val mobile: String? = null,
    val religion: String? = null
)

data class ExtractedAttendanceAndTraits(
    val days: String? = null,
    val present: String? = null,
    val act: String? = null,
    val disc: String? = null,
    val cond: String? = null,
    val remark: String? = null
)

data class ExtractedMarksheet(
    val summary: String,
    val student: ExtractedStudentInfo,
    val term: Int,
    val marks: List<ExtractedMarkItem>,
    val attendanceAndTraits: ExtractedAttendanceAndTraits,
    val uncertainNotes: List<String> = emptyList(),
    val isPlaceholder: Boolean = false
)

/**
 * শ্রেণিভিত্তিক বহু-শিক্ষার্থী নম্বরপত্র স্ক্যানের প্রতিটি সারির ডেটা (প্রিভিউ ও ম্যানুয়াল সংশোধনের জন্য)।
 */
data class ScannedStudentRow(
    val id: String = UUID.randomUUID().toString(),
    val selected: Boolean = true,
    val rollText: String,
    val name: String,
    val marksText: Map<String, String>,
    val sheetContinuousTotal: String? = null,
    val sheetGrandTotal: String? = null,
    val remark: String = "",
    val traitValues: Map<String, String> = emptyMap(),
    val rawNotes: String = "",
    val aiIssues: Map<String, String> = emptyMap(),
    val resolvedFields: Set<String> = emptySet(),
    val deducedFields: Map<String, String> = emptyMap(),
    val useScannedNameForExisting: Boolean = false
)

enum class ClassSheetImportScope {
    AUTO,
    BOTH,
    CONT_ONLY,
    WR_ONLY
}

enum class ClassSheetKind(val labelBn: String) {
    SINGLE_TERM_MARKS("১ প্রান্তিক নম্বর"),
    THREE_TERM_MARKS("৩ প্রান্তিক নম্বর"),
    ATTENDANCE_TRAITS("উপস্থিতি ও গুণাবলি")
}

data class ExtractedClassSheet(
    val cls: Int,
    val term: Int,
    val subKey: String,
    val summary: String,
    val warnings: List<String> = emptyList(),
    val rows: List<ScannedStudentRow>,
    val isSample: Boolean = false,
    val scope: ClassSheetImportScope = ClassSheetImportScope.BOTH,
    val sheetKind: ClassSheetKind = ClassSheetKind.SINGLE_TERM_MARKS,
    val sharedWorkingDaysByTerm: Map<Int, String> = emptyMap(),
    val templateColumnIds: List<String> = emptyList()
)

data class RowValidationReport(
    val hardErrors: Map<String, String>,
    val uncertainWarnings: Map<String, String>,
    val arithmeticWarning: String? = null,
    val calculatedContinuous: Double? = null,
    val calculatedGrandTotal: Double? = null,
    val fieldConfidences: Map<String, FieldConfidence> = emptyMap(),
    val mismatchFields: Set<String> = emptySet(),
    val quickFixes: List<ArithFixSuggestion> = emptyList(),
    val isSummaryOnlyMode: Boolean = false,
    val isGrandTotalOnlyMode: Boolean = false
) {
    val hasHardError: Boolean get() = hardErrors.isNotEmpty()
    val hasAnyIssue: Boolean get() = hardErrors.isNotEmpty() || uncertainWarnings.isNotEmpty() || arithmeticWarning != null
}

fun validateScannedStudentRow(
    row: ScannedStudentRow,
    cls: Int,
    subKey: String,
    duplicateRolls: Set<Int>,
    hasExistingDbName: Boolean = false,
    scope: ClassSheetImportScope = ClassSheetImportScope.BOTH,
    sheetKind: ClassSheetKind = ClassSheetKind.SINGLE_TERM_MARKS,
    sharedWorkingDaysByTerm: Map<Int, String> = emptyMap()
): RowValidationReport = ScanConstraintSolver.validateRow(
    row = row,
    cls = cls,
    subKey = subKey,
    duplicateRolls = duplicateRolls,
    hasExistingDbName = hasExistingDbName,
    scope = scope,
    sheetKind = sheetKind,
    sharedWorkingDaysByTerm = sharedWorkingDaysByTerm
)

data class AiChatReply(
    val text: String,
    val modelLabel: String,
    val isPlaceholder: Boolean = false
)

data class AiDraftRemarkResult(
    val remark: String,
    val isPlaceholder: Boolean = false
)

sealed class AutoScanResult {
    data class ClassSheet(val sheet: ExtractedClassSheet) : AutoScanResult()
    data class SingleStudent(val marksheet: ExtractedMarksheet) : AutoScanResult()
}

interface AiRepository {
    val isPlaceholderBackend: Boolean
    suspend fun chat(
        history: List<AiChatMessage>,
        latestUserMessage: String,
        modelMode: AiModelMode,
        studentContext: String?
    ): AiChatReply

    suspend fun analyzeMarksheetImage(
        bitmap: Bitmap,
        teacherNote: String,
        documentEnhance: Boolean = true
    ): ExtractedMarksheet

    suspend fun analyzeClassSheetImage(
        bitmap: Bitmap,
        clsHint: Int?,
        termHint: Int?,
        subKeyHint: String?,
        teacherNote: String,
        importScope: ClassSheetImportScope = ClassSheetImportScope.AUTO,
        documentEnhance: Boolean = true,
        sheetKindHint: ClassSheetKind? = null,
        qrMeta: BlankSheetQrMeta? = null
    ): ExtractedClassSheet

    suspend fun analyzeAutoSheetImage(
        bitmap: Bitmap,
        clsHint: Int? = null,
        termHint: Int? = null,
        subKeyHint: String? = null,
        teacherNote: String = "",
        importScope: ClassSheetImportScope = ClassSheetImportScope.AUTO,
        documentEnhance: Boolean = true,
        sheetKindHint: ClassSheetKind? = null,
        qrMeta: BlankSheetQrMeta? = null
    ): AutoScanResult

    suspend fun draftRemark(
        studentSummary: String,
        term: Int
    ): AiDraftRemarkResult
}

internal fun stripJsonFences(raw: String): String {
    var trimmed = raw.trim()
    if (trimmed.startsWith("```")) {
        trimmed = trimmed
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .trim()
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.removeSuffix("```").trim()
        }
    }
    return trimmed
}

internal fun extractBalancedJsonPayload(raw: String): String {
    val cleaned = stripJsonFences(raw)
    val objStart = cleaned.indexOf('{')
    val arrStart = cleaned.indexOf('[')
    val start = when {
        objStart < 0 && arrStart < 0 -> return cleaned
        objStart < 0 -> arrStart
        arrStart < 0 -> objStart
        else -> minOf(objStart, arrStart)
    }
    val openChar = cleaned[start]
    val closeChar = if (openChar == '[') ']' else '}'
    var depth = 0
    var inString = false
    var escaped = false
    for (i in start until cleaned.length) {
        val c = cleaned[i]
        if (escaped) {
            escaped = false
            continue
        }
        if (c == '\\' && inString) {
            escaped = true
            continue
        }
        if (c == '"') {
            inString = !inString
            continue
        }
        if (!inString) {
            if (c == openChar) depth++
            else if (c == closeChar) {
                depth--
                if (depth == 0) {
                    return cleaned.substring(start, i + 1)
                }
            }
        }
    }
    return cleaned.substring(start)
}

internal fun extractBalancedJsonObject(raw: String): String {
    val payload = extractBalancedJsonPayload(raw)
    if (payload.startsWith("[")) {
        val unwrapped = runCatching {
            val arr = JSONArray(payload)
            if (arr.length() == 1) arr.optJSONObject(0)?.toString() else null
        }.getOrNull()
        if (!unwrapped.isNullOrBlank()) return unwrapped
    }
    return payload
}

private val ROW_ARRAY_KEYS = listOf("rows", "students", "records", "items", "entries", "list", "table")
private val WRAPPER_OBJECT_KEYS = listOf("data", "result", "response", "output", "sheet", "marksheet")

internal fun findRowArray(json: JSONObject): JSONArray? {
    for (key in ROW_ARRAY_KEYS) {
        json.optJSONArray(key)?.let { return it }
    }
    json.optJSONArray("data")?.let { return it }
    return null
}

internal fun unwrapRowContainer(json: JSONObject): JSONObject {
    if (findRowArray(json) != null ||
        json.optJSONObject("student") != null ||
        json.optJSONArray("marks") != null ||
        json.has("scanMode")
    ) {
        return json
    }
    for (key in WRAPPER_OBJECT_KEYS) {
        val childObj = json.optJSONObject(key)
        if (childObj != null && (
                findRowArray(childObj) != null ||
                    childObj.optJSONObject("student") != null ||
                    childObj.optJSONArray("marks") != null
                )
        ) {
            return childObj
        }
        val childArr = json.optJSONArray(key)
        if (childArr != null) {
            return JSONObject().put("rows", childArr)
        }
    }
    return json
}

private fun salvageCompleteJsonObjectsFromSlice(slice: String, startOffset: Int): JSONArray {
    val salvagedRows = JSONArray()
    var i = startOffset
    var inString = false
    var escaped = false
    var depth = 0
    var objStart = -1
    while (i < slice.length) {
        val c = slice[i]
        if (escaped) {
            escaped = false
            i++
            continue
        }
        if (c == '\\' && inString) {
            escaped = true
            i++
            continue
        }
        if (c == '"') {
            inString = !inString
            i++
            continue
        }
        if (!inString) {
            when (c) {
                '{' -> {
                    if (depth == 0) objStart = i
                    depth++
                }
                '}' -> {
                    if (depth > 0) {
                        depth--
                        if (depth == 0 && objStart >= 0) {
                            val rowText = slice.substring(objStart, i + 1)
                            runCatching { JSONObject(rowText) }.getOrNull()?.let { salvagedRows.put(it) }
                            objStart = -1
                        }
                    }
                }
                ']' -> if (depth == 0) break
            }
        }
        i++
    }
    return salvagedRows
}

internal fun parseClassSheetJsonRobust(
    raw: String,
    clsHint: Int?,
    termHint: Int?,
    subKeyHint: String?
): JSONObject {
    val slice = extractBalancedJsonPayload(raw)
    if (slice.trimStart().startsWith("[")) {
        try {
            val arr = JSONArray(slice)
            return JSONObject().apply {
                clsHint?.let { put("cls", it) }
                termHint?.let { put("term", it) }
                subKeyHint?.let { put("subKey", it) }
                put("rows", arr)
            }
        } catch (arrErr: Exception) {
            val salvaged = salvageCompleteJsonObjectsFromSlice(slice, 1)
            if (salvaged.length() == 0) throw arrErr
            return JSONObject().apply {
                put("cls", clsHint ?: 3)
                put("term", termHint ?: 1)
                put("subKey", subKeyHint ?: "MATH")
                put("summary", "")
                put("warnings", JSONArray().apply {
                    put("মডেলের উত্তর দীর্ঘ হওয়ায় শেষ দিকের কিছু সারি অসম্পূর্ণ থাকতে পারে — প্রাপ্ত ${bn(salvaged.length().toString())}টি সারি দেখানো হয়েছে।")
                })
                put("rows", salvaged)
            }
        }
    }

    try {
        return unwrapRowContainer(JSONObject(slice))
    } catch (primaryErr: Exception) {
        // If output was truncated mid-array (e.g. MAX_TOKENS on large class), salvage all complete row objects
        val rowsMatch = Regex("\"(?:rows|students|records|items|entries|list|table)\"\\s*:\\s*\\[").find(slice)
            ?: throw primaryErr
        val rowsStart = rowsMatch.range.last + 1
        val salvagedRows = salvageCompleteJsonObjectsFromSlice(slice, rowsStart)
        if (salvagedRows.length() == 0) throw primaryErr

        val headerPrefix = slice.substring(0, rowsMatch.range.first)
        val clsVal = clsHint ?: Regex("\"cls\"\\s*:\\s*(\\d)").find(headerPrefix)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 3
        val termVal = termHint ?: Regex("\"term\"\\s*:\\s*(\\d)").find(headerPrefix)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
        val subVal = subKeyHint ?: Regex("\"subKey\"\\s*:\\s*\"([A-Za-z0-9_]+)\"").find(headerPrefix)?.groupValues?.getOrNull(1) ?: "MATH"
        val kindVal = Regex("\"sheetKind\"\\s*:\\s*\"([A-Za-z0-9_]+)\"").find(headerPrefix)?.groupValues?.getOrNull(1)

        return JSONObject().apply {
            put("cls", clsVal)
            put("term", termVal)
            put("subKey", subVal)
            if (kindVal != null) put("sheetKind", kindVal)
            put("summary", "")
            put("warnings", JSONArray().apply {
                put("মডেলের উত্তর দীর্ঘ হওয়ায় শেষ দিকের কিছু সারি অসম্পূর্ণ থাকতে পারে — প্রাপ্ত ${bn(salvagedRows.length().toString())}টি সারি দেখানো হয়েছে।")
            })
            put("rows", salvagedRows)
        }
    }
}

internal fun parseMarksheetJsonRobust(raw: String): JSONObject {
    val slice = extractBalancedJsonPayload(raw)
    if (slice.trimStart().startsWith("[")) {
        val arr = JSONArray(slice)
        return arr.optJSONObject(0)?.let(::unwrapRowContainer)
            ?: throw IllegalStateException("একক নম্বরপত্রের কোনো ডেটা পাওয়া যায়নি।")
    }
    return unwrapRowContainer(JSONObject(slice))
}

internal fun normalizeDetectedSubjectKey(
    raw: String,
    cls: Int,
    validSubs: Set<String> = subjectsFor(cls, RELIGIONS.first()).map { it.key }.toSet()
): String? {
    val cleaned = raw.trim()
    if (cleaned.isBlank()) return null
    val upper = cleaned.uppercase()
    if (upper in validSubs) return upper
    val lower = cleaned.lowercase()
    val candidate = when {
        upper == "BAN" || lower.contains("বাংলা") || lower.contains("bangla") || lower.contains("bengali") -> "BAN"
        upper == "ENG" || lower.contains("ইংরেজ") || lower.contains("english") -> "ENG"
        upper == "MATH" || lower.contains("গণিত") || lower.contains("গনিত") || lower.contains("math") -> "MATH"
        upper == "BGW" || lower.contains("বাওবি") || lower.contains("বিশ্বপরিচয়") || lower.contains("বিশ্ব পরিচয়") || lower.contains("bgs") ->
            if ("BGW" in validSubs) "BGW" else "SOC"
        upper == "SCI" || lower == "বিজ্ঞান" || lower.contains("প্রাথমিক বিজ্ঞান") || lower.contains("science") ->
            if ("SCI" in validSubs) "SCI" else "SOC"
        upper == "SOC" || lower.contains("সামাজিক") || lower.contains("সমন্বিত") ->
            if ("SOC" in validSubs) "SOC" else if ("BGW" in validSubs) "BGW" else "SCI"
        upper == "REL" || lower.contains("ধর্ম") || lower.contains("ইসলাম") || lower.contains("হিন্দু") ||
            lower.contains("বৌদ্ধ") || lower.contains("খ্রীষ্ট") || lower.contains("religion") -> "REL"
        upper == "PE" || lower.contains("শারীরিক") || lower.contains("স্বাস্থ্য") -> "PE"
        upper == "ART" || lower.contains("শিল্প") || lower.contains("চারু") || lower.contains("কারু") -> "ART"
        else -> null
    }
    return candidate?.takeIf { it in validSubs }
}

internal fun parseExtractedMarksheetJson(json: JSONObject): ExtractedMarksheet {
    val sObj = json.optJSONObject("student") ?: JSONObject()
    val marksArr = json.optJSONArray("marks") ?: JSONArray()
    val marks = (0 until marksArr.length()).mapNotNull { i ->
        val m = marksArr.optJSONObject(i) ?: return@mapNotNull null
        val k = m.optString("key").trim()
        if (k.isBlank()) return@mapNotNull null
        ExtractedMarkItem(
            key = k,
            value = m.optDouble("value", 0.0),
            subjectLabel = m.optString("subjectLabel", k)
        )
    }
    val attObj = json.optJSONObject("attendanceAndTraits") ?: JSONObject()
    val notesArr = json.optJSONArray("uncertainNotes") ?: JSONArray()
    val notes = (0 until notesArr.length()).mapNotNull { i ->
        notesArr.optString(i)?.takeIf { it.isNotBlank() }
    }
    return ExtractedMarksheet(
        summary = json.optString("summary", "ছবি থেকে তথ্য শনাক্ত হয়েছে।").ifBlank { "ছবি থেকে তথ্য শনাক্ত হয়েছে।" },
        student = ExtractedStudentInfo(
            name = sObj.optString("name").takeIf { it.isNotBlank() },
            cls = sObj.optInt("cls", 0).takeIf { it in 1..20 },
            roll = sObj.optInt("roll", 0).takeIf { it > 0 },
            guardian = sObj.optString("guardian").takeIf { it.isNotBlank() },
            village = sObj.optString("village").takeIf { it.isNotBlank() },
            union = sObj.optString("union").takeIf { it.isNotBlank() },
            upazila = sObj.optString("upazila").takeIf { it.isNotBlank() },
            district = sObj.optString("district").takeIf { it.isNotBlank() },
            mobile = sObj.optString("mobile").takeIf { it.isNotBlank() },
            religion = sObj.optString("religion").takeIf { it.isNotBlank() }
        ),
        term = json.optInt("term", 1).coerceIn(1, 3),
        marks = marks,
        attendanceAndTraits = ExtractedAttendanceAndTraits(
            days = attObj.optString("days").takeIf { it.isNotBlank() },
            present = attObj.optString("present").takeIf { it.isNotBlank() },
            act = attObj.optString("act").takeIf { it.isNotBlank() },
            disc = attObj.optString("disc").takeIf { it.isNotBlank() },
            cond = attObj.optString("cond").takeIf { it.isNotBlank() },
            remark = attObj.optString("remark").takeIf { it.isNotBlank() }
        ),
        uncertainNotes = notes,
        isPlaceholder = false
    )
}

internal fun parseAutoScanJson(
    raw: String,
    clsHint: Int?,
    termHint: Int?,
    subKeyHint: String?,
    requestedScope: ClassSheetImportScope = ClassSheetImportScope.AUTO,
    sheetKindHint: ClassSheetKind? = null,
    qrMeta: BlankSheetQrMeta? = null
): AutoScanResult {
    val json = parseClassSheetJsonRobust(raw, clsHint, termHint, subKeyHint)
    val rawMode = json.optString("scanMode", "").trim().uppercase()
    val rowsArr = findRowArray(json)
    val marksArr = json.optJSONArray("marks")
    val hasStudentObj = json.optJSONObject("student")?.let { st ->
        st.optString("name", "").isNotBlank() || st.optInt("roll", 0) > 0
    } == true

    val isSingle = when {
        rawMode.contains("SINGLE") -> true
        rawMode.contains("CLASS") -> false
        (rowsArr == null || rowsArr.length() == 0) && ((marksArr?.length() ?: 0) > 0 || hasStudentObj) -> true
        rowsArr != null -> false
        else -> throw IllegalStateException(
            "মডেলের উত্তরে কোনো নম্বরপত্র বা শিক্ষার্থীর তালিকা পাওয়া যায়নি। ছবি স্পষ্ট করে আবার চেষ্টা করুন।"
        )
    }

    return if (isSingle) {
        AutoScanResult.SingleStudent(parseExtractedMarksheetJson(json))
    } else {
        AutoScanResult.ClassSheet(
            parseExtractedClassSheetJson(
                json = json,
                clsHint = clsHint,
                termHint = termHint,
                subKeyHint = subKeyHint,
                requestedScope = requestedScope,
                sheetKindHint = sheetKindHint,
                qrMeta = qrMeta
            )
        )
    }
}

private fun JSONObject.optCleanString(key: String): String {
    if (!has(key) || isNull(key)) return ""
    val raw = optString(key, "").trim()
    return if (raw.equals("null", ignoreCase = true)) "" else raw
}

private fun normalizeMarkJsonKey(rawKey: String): String? {
    val trimmed = rawKey.trim()
    if (trimmed.isEmpty()) return null
    val up = trimmed.uppercase()
    val low = trimmed.lowercase()

    // ৩ প্রান্তিক কলাম কী (T1_CONT, T1_WR, T1_TOTAL বা বাংলা নাম)
    val threeMatch = Regex("^T([1-3])_(.+)$").find(up)
    if (threeMatch != null) {
        val t = threeMatch.groupValues[1]
        val suffix = threeMatch.groupValues[2]
        val suffixLow = suffix.lowercase()
        val mapped = when {
            suffix in setOf("CONT", "CONTINUOUS") || suffixLow.contains("ধারাবাহিক") -> "CONT"
            suffix in setOf("WR", "WRITTEN") || suffixLow.contains("সামষ্টিক") || suffixLow.contains("লিখিত") -> "WR"
            suffix in setOf("TOTAL", "GRAND") || suffixLow.contains("মোট") -> "TOTAL"
            else -> return null
        }
        return "T${t}_$mapped"
    }

    // স্কোর নয় এমন কলাম বাদ দিন
    if (up in setOf("GRADE", "GP", "GPA", "MERIT", "POSITION", "SIGN", "SIGNATURE") ||
        low.contains("গ্রেড") || low.contains("জিপি") || low.contains("মেধাক্রম") || low.contains("স্বাক্ষর")
    ) {
        return null
    }

    return when {
        up in setOf("TB", "TEXTBOOK") || low.contains("পাঠ্যবই") || low == "পাঠ্য" -> "TB"
        up in setOf("ACT", "CLASSWORK", "ACTIVITY") || low == "কাজ" || low.contains("সক্রিয়") || low.contains("সক্রিয়") -> "ACT"
        up in setOf("LANG", "LANGUAGE", "ORAL") || low.contains("ভাষা") || low.contains("মৌখিক") -> "LANG"
        up in setOf("CT", "CLASSTEST", "TEST") || low.contains("অভীক্ষা") || low.contains("টেস্ট") -> "CT"
        up in setOf("WR", "WRITTEN") || low.contains("লিখিত") || low.contains("সামষ্টিক") -> "WR"
        up in setOf("CONT", "CONTINUOUS", "SHEETCONTINUOUSTOTAL") || low.contains("ধারাবাহিক") -> "CONT"
        up in setOf("TOTAL", "GRAND", "GRANDTOTAL", "SHEETGRANDTOTAL") || low == "মোট" || low.contains("সর্বমোট") -> "TOTAL"
        else -> up
    }
}

internal fun parseExtractedClassSheetJson(
    json: JSONObject,
    clsHint: Int?,
    termHint: Int?,
    subKeyHint: String?,
    requestedScope: ClassSheetImportScope = ClassSheetImportScope.AUTO,
    sheetKindHint: ClassSheetKind? = null,
    qrMeta: BlankSheetQrMeta? = null
): ExtractedClassSheet {
    val refCode = json.optCleanString("refCode").uppercase()
        .ifBlank { qrMeta?.refCode.orEmpty() }
    val refMatch = Regex("REF\\s*:\\s*C(\\d{1,2})-([A-Z]+)-([A-Z0-9]+)(?:-([A-Z0-9]+))?").find(refCode)
    val refCls = refMatch?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..20 }
    val refSub = refMatch?.groupValues?.getOrNull(2)?.takeIf {
        it.isNotBlank() &&
            it != "ATT" &&
            it != "GEN" &&
            !it.equals("ALL", ignoreCase = true) &&
            !it.equals(BlankSheetCatalog.ALL_SUBJECTS_KEY, ignoreCase = true)
    }
    val refTerm = when (refMatch?.groupValues?.getOrNull(3)) {
        "T1" -> 1
        "T2" -> 2
        "T3" -> 3
        else -> null
    }
    val refLayout = refMatch?.groupValues?.getOrNull(4).orEmpty()

    val qrSub = qrMeta?.subjectKey?.trim()?.takeIf {
        it.isNotBlank() &&
            !it.equals("ALL", ignoreCase = true) &&
            !it.equals(BlankSheetCatalog.ALL_SUBJECTS_KEY, ignoreCase = true)
    }
    val cleanSubHint = subKeyHint?.trim()?.takeIf {
        it.isNotBlank() &&
            !it.equals("ALL", ignoreCase = true) &&
            !it.equals(BlankSheetCatalog.ALL_SUBJECTS_KEY, ignoreCase = true)
    }

    val detectedCls = (clsHint ?: qrMeta?.cls ?: refCls ?: json.optInt("cls", 3)).coerceIn(1, 20)
    val detectedTerm = (termHint ?: qrMeta?.term?.takeIf { it in 1..3 } ?: refTerm ?: json.optInt("term", 1)).coerceIn(1, 3)
    val rawSub = (qrSub ?: refSub ?: json.optCleanString("subKey")).trim()
    val validSubs = subjectsFor(detectedCls, RELIGIONS.first()).map { it.key }.toSet()
    val normalizedSub = normalizeDetectedSubjectKey(rawSub, detectedCls, validSubs)
    val detectedSub = when {
        cleanSubHint != null && cleanSubHint in validSubs -> cleanSubHint
        normalizedSub != null -> normalizedSub
        else -> validSubs.first()
    }
    val activeSub = subjectsFor(detectedCls, RELIGIONS.first()).find { it.key == detectedSub }
    val hasWritten = (activeSub?.written ?: 0) > 0
    val isArtSub = activeSub?.comps?.size == 1 && activeSub.comps.first().key == "TOTAL" && activeSub.written == 0

    val warningsArr = json.optJSONArray("warnings") ?: JSONArray()
    val warnings = (0 until warningsArr.length()).mapNotNull { i ->
        warningsArr.optString(i)?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
    }.toMutableList()

    val sharedWdObj = json.optJSONObject("sharedWorkingDays") ?: JSONObject()
    val sharedWorkingDays = linkedMapOf<Int, String>()
    for (t in 1..3) {
        val rawWd = sharedWdObj.optCleanString(t.toString()).ifBlank {
            sharedWdObj.optCleanString("T$t")
        }
        val cleanWd = fromBn(rawWd).filter { it.isDigit() }.toIntOrNull()?.takeIf { it in 1..365 }?.toString()
        if (!cleanWd.isNullOrBlank()) {
            sharedWorkingDays[t] = cleanWd
        } else {
            qrMeta?.workingDaysByTerm?.get(t)?.takeIf { it.isNotBlank() }?.let { qrWd ->
                sharedWorkingDays[t] = qrWd
            }
        }
    }

    val rawSheetKind = json.optCleanString("sheetKind").uppercase()
    val looksLikeTraitsSheet =
        sheetKindHint == ClassSheetKind.ATTENDANCE_TRAITS ||
            qrMeta?.sheetKind == ClassSheetKind.ATTENDANCE_TRAITS ||
            rawSheetKind == "ATTENDANCE_TRAITS" ||
            refLayout == "TRAITS" ||
            refLayout == "ATT"

    fun normalizeTraitJsonKey(rawKey: String): String? {
        val trimmed = rawKey.trim()
        if (trimmed.isEmpty()) return null
        val up = trimmed.uppercase()
        val low = trimmed.lowercase()
        val tPrefixMatch = Regex("^T([1-3])_(.+)$").find(up)
        val bareOrSuffix = tPrefixMatch?.groupValues?.getOrNull(2) ?: up
        val tPrefixNum = tPrefixMatch?.groupValues?.getOrNull(1)?.toIntOrNull()

        val numberedMatch = Regex("^(DAYS|PRES|ATT|PRESENT|ACT|TRAIT_ACT|DISC|TRAIT_DISC|COND|TRAIT_COND|REM|REMARK)_([1-3])$").find(up)
        if (numberedMatch != null) {
            val baseRaw = numberedMatch.groupValues[1]
            val tNum = numberedMatch.groupValues[2]
            val base = when (baseRaw) {
                "ATT", "PRESENT" -> "PRES"
                "TRAIT_ACT" -> "ACT"
                "TRAIT_DISC" -> "DISC"
                "TRAIT_COND" -> "COND"
                "REMARK" -> "REM"
                else -> baseRaw
            }
            return "${base}_$tNum"
        }

        val mappedBase = when {
            bareOrSuffix in setOf("DAYS", "WORKING_DAYS", "WORK_DAYS") ||
                low.contains("কার্যদিবস") -> "DAYS"
            bareOrSuffix in setOf("PRES", "ATT", "ATTENDANCE", "PRESENT") ||
                low.contains("উপস্থিত") || low.contains("হাজিরা") -> "PRES"
            bareOrSuffix in setOf("ACT", "TRAIT_ACT", "CO_CURRICULAR", "ACTIVITIES") ||
                low.contains("সক্রিয়") || low.contains("সক্রিয়") || low.contains("সহপাঠ") -> "ACT"
            bareOrSuffix in setOf("DISC", "TRAIT_DISC", "DISCIPLINE") ||
                low.contains("শৃঙ্খলা") || low.contains("শৃংখলা") -> "DISC"
            bareOrSuffix in setOf("COND", "TRAIT_COND", "CONDUCT", "BEHAVIOR", "BEHAVIOUR") ||
                low.contains("আচরণ") || low.contains("আচরন") -> "COND"
            bareOrSuffix in setOf("REM", "REMARK", "COMMENT") ||
                low.contains("মন্তব্য") -> "REM"
            else -> null
        } ?: return null

        val termNum = tPrefixNum ?: detectedTerm
        return "${mappedBase}_$termNum"
    }

    val rowsArr = findRowArray(json) ?: JSONArray()
    val rawParsed = (0 until rowsArr.length()).mapNotNull { idx ->
        val obj = rowsArr.optJSONObject(idx) ?: return@mapNotNull null
        val rollStr = fromBn(
            obj.optCleanString("roll")
                .ifBlank { obj.optCleanString("rollNo") }
                .ifBlank { obj.optCleanString("roll_no") }
        )
        val nameStr = obj.optCleanString("name")
            .ifBlank { obj.optCleanString("studentName") }
            .ifBlank { obj.optCleanString("student_name") }
        val marksObj = obj.optJSONObject("marks") ?: JSONObject()
        val marksMap = mutableMapOf<String, String>()
        val traitsMap = mutableMapOf<String, String>()
        var promotedCont = ""
        var promotedGrand = ""

        fun putTraitValue(normKey: String, rawVal: String) {
            val cleanVal = when {
                normKey.startsWith("DAYS_") || normKey.startsWith("PRES_") ->
                    fromBn(rawVal).filter { it.isDigit() }
                normKey.startsWith("ACT_") || normKey.startsWith("DISC_") || normKey.startsWith("COND_") ->
                    Repo.normalizeTraitCode(rawVal)
                else -> rawVal.trim()
            }
            if (cleanVal.isNotEmpty()) traitsMap[normKey] = cleanVal
        }

        marksObj.keys().forEach { k ->
            val v = marksObj.optCleanString(k)
            if (v.isNotEmpty()) {
                val salvagedTraitKey = if (looksLikeTraitsSheet) normalizeTraitJsonKey(k) else null
                if (salvagedTraitKey != null) {
                    putTraitValue(salvagedTraitKey, v)
                } else {
                    val normMark = normalizeMarkJsonKey(k)
                    when (normMark) {
                        null -> Unit
                        "CONT" -> if (promotedCont.isBlank()) promotedCont = fromBn(v)
                        "TOTAL" -> {
                            val numStr = fromBn(v)
                            if (promotedGrand.isBlank()) promotedGrand = numStr
                            if (isArtSub) marksMap["TOTAL"] = numStr
                        }
                        else -> marksMap[normMark] = fromBn(v)
                    }
                }
            }
        }
        val traitsObj = obj.optJSONObject("traits") ?: JSONObject()
        traitsObj.keys().forEach { k ->
            val v = traitsObj.optCleanString(k)
            if (v.isNotEmpty()) {
                val normKey = normalizeTraitJsonKey(k) ?: k.trim().uppercase()
                putTraitValue(normKey, v)
            }
        }
        val remarkStr = obj.optCleanString("remark")

        val uncObj = obj.optJSONObject("uncertainFields") ?: JSONObject()
        val aiIssues = mutableMapOf<String, String>()
        uncObj.keys().forEach { k ->
            val reason = uncObj.optCleanString(k)
            if (reason.isNotEmpty()) {
                val up = k.trim().uppercase()
                val normKey = when (k.trim().lowercase()) {
                    "roll", "name", "remark" -> k.trim().lowercase()
                    "cont", "continuous", "sheetcontinuoustotal" -> "CONT"
                    "grand", "grandtotal", "sheetgrandtotal" -> "GRAND"
                    else -> up
                }
                aiIssues[normKey] = reason
            }
        }
        val sheetCont = obj.optCleanString("sheetContinuousTotal")
            .ifBlank { promotedCont }
            .takeIf { it.isNotEmpty() }
            ?.let { fromBn(it) }
        val sheetGrand = obj.optCleanString("sheetGrandTotal")
            .ifBlank { promotedGrand }
            .takeIf { it.isNotEmpty() }
            ?.let { fromBn(it) }

        ScannedStudentRow(
            id = UUID.randomUUID().toString(),
            selected = true,
            rollText = rollStr,
            name = nameStr,
            marksText = marksMap,
            sheetContinuousTotal = sheetCont,
            sheetGrandTotal = sheetGrand,
            remark = remarkStr,
            traitValues = traitsMap,
            rawNotes = obj.optCleanString("rawNotes"),
            aiIssues = aiIssues
        )
    }

    val hasThreeTermKeys = rawParsed.any { r -> r.marksText.keys.any { it.startsWith("T1_") || it.startsWith("T2_") || it.startsWith("T3_") } }
    val hasTraitKeys = rawParsed.any { it.traitValues.isNotEmpty() } || sharedWorkingDays.isNotEmpty()

    val resolvedSheetKind = when {
        sheetKindHint != null -> sheetKindHint
        qrMeta != null -> qrMeta.sheetKind
        refLayout == "TRAITS" || refLayout == "ATT" -> ClassSheetKind.ATTENDANCE_TRAITS
        refLayout == "3MARKS" -> ClassSheetKind.THREE_TERM_MARKS
        rawSheetKind == "THREE_TERM_MARKS" -> ClassSheetKind.THREE_TERM_MARKS
        rawSheetKind == "ATTENDANCE_TRAITS" -> ClassSheetKind.ATTENDANCE_TRAITS
        hasThreeTermKeys -> ClassSheetKind.THREE_TERM_MARKS
        hasTraitKeys && rawParsed.none { r -> r.marksText.isNotEmpty() || !r.sheetContinuousTotal.isNullOrBlank() || !r.sheetGrandTotal.isNullOrBlank() } ->
            ClassSheetKind.ATTENDANCE_TRAITS
        else -> ClassSheetKind.SINGLE_TERM_MARKS
    }

    val metaFound = json.optJSONObject("metaFound")
    val missingMeta = mutableListOf<String>()
    if (clsHint == null && qrMeta == null && refCls == null && metaFound != null && !metaFound.optBoolean("cls", true)) missingMeta.add("শ্রেণি")
    if (resolvedSheetKind == ClassSheetKind.SINGLE_TERM_MARKS) {
        if (termHint == null && qrMeta?.term !in 1..3 && refTerm == null && metaFound != null && !metaFound.optBoolean("term", true)) missingMeta.add("প্রান্তিক")
    }
    if (resolvedSheetKind != ClassSheetKind.ATTENDANCE_TRAITS) {
        val qrHasSub = qrSub != null
        if (cleanSubHint == null && !qrHasSub && refSub == null && (metaFound != null && !metaFound.optBoolean("sub", true) || normalizedSub == null)) {
            missingMeta.add("বিষয়")
        }
    }
    if (missingMeta.isNotEmpty()) {
        warnings.add(
            "ছবির উপরে ${missingMeta.joinToString(", ")} স্পষ্ট লেখা না থাকায় নিচ থেকে সঠিকটি যাচাই করে নিন।"
        )
    }

    val compKeysSet = setOf("TB", "ACT", "LANG", "CT")
    val qrHasBreakdownCol = qrMeta?.columnIds?.any { it in compKeysSet }
    val qrHasContCol = qrMeta?.columnIds?.any { it == "CONT" || it in compKeysSet }
    val qrHasWrCol = qrMeta?.columnIds?.any { it == "WR" }
    val qrHasTotalCol = qrMeta?.columnIds?.any { it == "TOTAL" }

    val sheetHasAnyName = rawParsed.any { it.name.isNotBlank() }
    val sheetHasAnyBreakdown = rawParsed.any { r -> compKeysSet.any { !r.marksText[it].isNullOrBlank() } }
    val sheetHasAnyCont = sheetHasAnyBreakdown || rawParsed.any { !it.sheetContinuousTotal.isNullOrBlank() }
    val sheetHasAnyWr = rawParsed.any { !it.marksText["WR"].isNullOrBlank() }
    val sheetHasAnyGrand = rawParsed.any { !it.sheetGrandTotal.isNullOrBlank() }

    val resolvedScope = when {
        resolvedSheetKind != ClassSheetKind.SINGLE_TERM_MARKS -> ClassSheetImportScope.BOTH
        requestedScope == ClassSheetImportScope.CONT_ONLY -> ClassSheetImportScope.CONT_ONLY
        requestedScope == ClassSheetImportScope.WR_ONLY -> if (hasWritten) ClassSheetImportScope.WR_ONLY else ClassSheetImportScope.BOTH
        requestedScope == ClassSheetImportScope.BOTH -> ClassSheetImportScope.BOTH
        qrMeta != null && qrMeta.importScope == ClassSheetImportScope.CONT_ONLY -> ClassSheetImportScope.CONT_ONLY
        qrMeta != null && qrMeta.importScope == ClassSheetImportScope.WR_ONLY -> if (hasWritten) ClassSheetImportScope.WR_ONLY else ClassSheetImportScope.BOTH
        hasWritten && sheetHasAnyCont && !sheetHasAnyWr -> ClassSheetImportScope.CONT_ONLY
        hasWritten && sheetHasAnyWr && !sheetHasAnyCont -> ClassSheetImportScope.WR_ONLY
        else -> ClassSheetImportScope.BOTH
    }

    val parsedRows = rawParsed.map { row ->
        val cleanedIssues = row.aiIssues.filterKeys { key ->
            when {
                key == "name" && ((qrMeta?.columnIds?.contains("NAME") == false) || (!sheetHasAnyName && row.name.isBlank())) -> false
                resolvedSheetKind == ClassSheetKind.SINGLE_TERM_MARKS && key in compKeysSet && ((qrHasBreakdownCol == false) || (!sheetHasAnyBreakdown && row.marksText[key].isNullOrBlank())) -> false
                resolvedSheetKind == ClassSheetKind.SINGLE_TERM_MARKS && key == "CONT" && qrHasContCol == false -> false
                resolvedSheetKind == ClassSheetKind.SINGLE_TERM_MARKS && key == "WR" && qrHasWrCol == false -> false
                resolvedSheetKind == ClassSheetKind.SINGLE_TERM_MARKS && key == "GRAND" && ((qrHasTotalCol == false) || (!sheetHasAnyGrand && row.sheetGrandTotal.isNullOrBlank())) -> false
                resolvedSheetKind == ClassSheetKind.SINGLE_TERM_MARKS && resolvedScope == ClassSheetImportScope.CONT_ONLY && (key == "WR" || key == "GRAND") -> false
                resolvedSheetKind == ClassSheetKind.SINGLE_TERM_MARKS && resolvedScope == ClassSheetImportScope.WR_ONLY && (key in compKeysSet || key == "CONT" || key == "GRAND") -> false
                else -> true
            }
        }
        ScanConstraintSolver.reconcileInterdependencies(
            row = row.copy(aiIssues = cleanedIssues),
            cls = detectedCls,
            subKey = detectedSub,
            scope = resolvedScope,
            sheetKind = resolvedSheetKind
        )
    }

    if (parsedRows.isEmpty() && warnings.none { it.contains("কোনো শিক্ষার্থীর সারি") }) {
        warnings.add("ছবিটি থেকে কোনো শিক্ষার্থীর সারি শনাক্ত হয়নি — কাগজটি সোজা ও স্পষ্ট আলোতে রেখে পুনরায় চেষ্টা করুন।")
    }

    return ExtractedClassSheet(
        cls = detectedCls,
        term = detectedTerm,
        subKey = detectedSub,
        summary = json.optCleanString("summary").ifBlank {
            when (resolvedSheetKind) {
                ClassSheetKind.THREE_TERM_MARKS ->
                    "${classNameBn(detectedCls)} শ্রেণি · ৩ প্রান্তিক নম্বর — ${bn(parsedRows.size.toString())} জন শনাক্ত।"
                ClassSheetKind.ATTENDANCE_TRAITS ->
                    "${classNameBn(detectedCls)} শ্রেণি · উপস্থিতি ও গুণাবলি — ${bn(parsedRows.size.toString())} জন শনাক্ত।"
                ClassSheetKind.SINGLE_TERM_MARKS -> {
                    val scopeNote = when (resolvedScope) {
                        ClassSheetImportScope.CONT_ONLY -> " · শুধু ধারাবাহিক"
                        ClassSheetImportScope.WR_ONLY -> " · শুধু সামষ্টিক"
                        else -> ""
                    }
                    "${classNameBn(detectedCls)} শ্রেণি · ${termNameBn(detectedTerm)}$scopeNote — ${bn(parsedRows.size.toString())} জন শনাক্ত।"
                }
            }
        },
        warnings = warnings.distinct(),
        rows = parsedRows,
        isSample = false,
        scope = resolvedScope,
        sheetKind = resolvedSheetKind,
        sharedWorkingDaysByTerm = sharedWorkingDays,
        templateColumnIds = qrMeta?.columnIds.orEmpty()
    )
}

/**
 * ব্যবহারকারীর নিজস্ব Gemini API Key-ভিত্তিক সরাসরি Gemini Developer API ক্লায়েন্ট।
 * Firebase AI Logic / App Check বা কোনো ব্যাকএন্ড প্রক্সি ছাড়াই সরাসরি কাজ করে।
 */
class DirectGeminiAiRepository(
    private val appContext: Context
) : AiRepository {
    override val isPlaceholderBackend: Boolean
        get() = !GeminiApiConfig.isConfigured(appContext)

    private fun requireApiKey(): String =
        GeminiApiConfig.getApiKey(appContext).takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(
                "Gemini API Key সেট করা নেই। সেটিংস → Gemini AI ও মডেল অংশে গিয়ে আপনার API Key সংরক্ষণ করুন।"
            )

    private fun requireOrderedModels(): List<String> {
        val ordered = GeminiApiConfig.getOrderedModels(appContext)
        if (ordered.isEmpty()) {
            throw IllegalStateException(
                "কোনো Gemini মডেল নির্বাচিত নেই। সেটিংস → Gemini AI অংশে গিয়ে প্রথমে 'লাইভ মডেল আনুন' এবং আপনার পছন্দের ১–৩টি মডেল নির্বাচন করুন।"
            )
        }
        return ordered
    }

    private fun resolveModelId(@Suppress("UNUSED_PARAMETER") mode: AiModelMode? = null): String =
        requireOrderedModels().first()

    @Volatile
    private var lastSeenApiKey: String? = null
    private val modelCooldownUntilMs = ConcurrentHashMap<String, Long>()
    private val cacheLock = Any()
    private val singleSheetCache = object : LinkedHashMap<String, ExtractedMarksheet>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ExtractedMarksheet>?): Boolean = size > 8
    }
    private val classSheetCache = object : LinkedHashMap<String, ExtractedClassSheet>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ExtractedClassSheet>?): Boolean = size > 8
    }
    private val autoScanCache = object : LinkedHashMap<String, AutoScanResult>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, AutoScanResult>?): Boolean = size > 8
    }
    private val remarkCache = object : LinkedHashMap<String, AiDraftRemarkResult>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, AiDraftRemarkResult>?): Boolean = size > 16
    }

    private fun syncApiKeyChange(apiKey: String) {
        val prev = lastSeenApiKey
        if (prev != null && prev != apiKey) {
            modelCooldownUntilMs.clear()
            synchronized(cacheLock) {
                singleSheetCache.clear()
                classSheetCache.clear()
                autoScanCache.clear()
                remarkCache.clear()
            }
        }
        lastSeenApiKey = apiKey
    }

    fun clearCooldowns() {
        modelCooldownUntilMs.clear()
    }

    fun clearCooldownFor(modelId: String) {
        val clean = stripModelPrefix(modelId)
        if (clean.isNotBlank()) modelCooldownUntilMs.remove(clean)
    }

    private fun remainingCooldownSeconds(modelId: String): Int {
        val clean = stripModelPrefix(modelId)
        if (clean.isBlank()) return 0
        val until = modelCooldownUntilMs[clean] ?: return 0
        val diff = until - System.currentTimeMillis()
        if (diff <= 0L) {
            modelCooldownUntilMs.remove(clean)
            return 0
        }
        return ceil(diff / 1000.0).toInt().coerceAtLeast(1)
    }

    private fun recordRateLimitCooldown(modelId: String, retryAfterSec: Int?, isZeroQuota: Boolean = false) {
        val clean = stripModelPrefix(modelId)
        if (clean.isBlank()) return
        val defaultWait = if (isZeroQuota) ZERO_QUOTA_COOLDOWN_SEC else DEFAULT_RATE_LIMIT_COOLDOWN_SEC
        val waitSec = maxOf(retryAfterSec ?: defaultWait, if (isZeroQuota) ZERO_QUOTA_COOLDOWN_SEC else 0)
            .coerceIn(5, ZERO_QUOTA_COOLDOWN_SEC)
        modelCooldownUntilMs[clean] = System.currentTimeMillis() + waitSec * 1000L
    }

    private fun recordServerBusyCooldown(modelId: String, waitSec: Int = 20) {
        val clean = stripModelPrefix(modelId)
        if (clean.isBlank()) return
        modelCooldownUntilMs[clean] = System.currentTimeMillis() + waitSec.coerceIn(5, 60) * 1000L
    }

    private suspend fun callGenerateContentResilient(
        apiKey: String,
        @Suppress("UNUSED_PARAMETER") primaryModelId: String,
        body: JSONObject
    ): Pair<String, String> {
        if (!hasNetworkTransport(appContext)) {
            throw GeminiNetworkException(
                isTimeout = false,
                isConnectionError = true,
                userMessage = "মোবাইল ডেটা বা Wi-Fi চালু নেই। ইন্টারনেট সংযোগ চালু করে আবার চেষ্টা করুন।"
            )
        }

        syncApiKeyChange(apiKey)
        val userOrdered = requireOrderedModels()
        val cachedIds = GeminiApiConfig.getCachedModels(appContext).map { it.id }.toSet() +
            userOrdered.map { stripModelPrefix(it) }.toSet()
        val plan = buildModelQueuePlan(userOrdered, modelCooldownUntilMs, System.currentTimeMillis(), cachedIds)
        plan.expiredKeysToPrune.forEach { modelCooldownUntilMs.remove(it) }

        if (plan.healthyModels.isEmpty()) {
            val waitText = bn(plan.shortestCooldownSec.toString())
            throw GeminiHttpException(
                httpCode = 429,
                modelId = userOrdered.first(),
                userMessage = "সব নির্বাচিত মডেলে রেট-লিমিট কুলডাউন চলছে (${plan.coolingSummary}) — অযথা কল এড়াতে ${waitText} সেকেন্ড অপেক্ষা করে আবার চেষ্টা করুন।",
                retryAfterSeconds = plan.shortestCooldownSec
            )
        }

        val serializedBody = body.toString().toByteArray(Charsets.UTF_8)
        val isVisionOrHeavy = serializedBody.size > 80_000 || bodyHasInlineImage(body)
        val maxAttemptsPerModel = if (isVisionOrHeavy) 1 else 2
        var lastError: Exception? = null
        val attemptedModels = mutableListOf<String>()
        var consecutiveTransientRateLimits = 0

        for ((idx, candidateModel) in plan.healthyModels.withIndex()) {
            if (!hasNetworkTransport(appContext)) break
            if (idx > 0) {
                delay(450L)
            }
            attemptedModels.add(candidateModel)
            for (attempt in 0 until maxAttemptsPerModel) {
                try {
                    val text = callGenerateContent(apiKey, candidateModel, body, serializedBody)
                    modelCooldownUntilMs.remove(candidateModel)
                    return text to candidateModel
                } catch (e: GeminiHttpException) {
                    lastError = e
                    if (e.httpCode == 401 || e.httpCode == 403) throw e
                    if (e.httpCode == 429) {
                        recordRateLimitCooldown(e.modelId, e.retryAfterSeconds, e.isZeroQuota)
                        if (!e.isZeroQuota) {
                            consecutiveTransientRateLimits++
                            if (consecutiveTransientRateLimits >= 2) {
                                // Stop immediately on consecutive per-minute/TPM 429s so we don't keep tripping project rate limits
                                break
                            }
                        }
                        break
                    }
                    consecutiveTransientRateLimits = 0
                    if (e.httpCode == 503) {
                        recordServerBusyCooldown(e.modelId)
                        break
                    }
                    if (e.httpCode == 400 || e.httpCode == 404) {
                        break
                    }
                    val isTransient = e.httpCode in setOf(500, 502, 504)
                    if (isTransient && attempt == 0 && maxAttemptsPerModel > 1 && idx == plan.healthyModels.lastIndex) {
                        delay(700L)
                        continue
                    }
                    break
                } catch (e: GeminiNetworkException) {
                    lastError = e
                    if (e.isTimeout || maxAttemptsPerModel == 1) {
                        break
                    }
                    if (attempt == 0 && idx == plan.healthyModels.lastIndex && e.isConnectionError) {
                        delay(700L)
                        continue
                    }
                    break
                } catch (e: Exception) {
                    lastError = e
                    if (idx == plan.healthyModels.lastIndex) throw e
                    break
                }
            }
            if (consecutiveTransientRateLimits >= 2) break
        }

        val baseErr = lastError ?: IllegalStateException("Gemini API সংযোগ ব্যর্থ হয়েছে।")
        if (attemptedModels.size > 1) {
            val msg = "${baseErr.message ?: "Gemini API ত্রুটি"}\n(চেষ্টা করা হয়েছে: ${attemptedModels.distinct().joinToString(" → ")})"
            throw IllegalStateException(msg, baseErr)
        }
        throw baseErr
    }

    override suspend fun chat(
        history: List<AiChatMessage>,
        latestUserMessage: String,
        modelMode: AiModelMode,
        studentContext: String?
    ): AiChatReply {
        val apiKey = requireApiKey()
        val modelId = resolveModelId(modelMode)
        val recentTurns = history.takeLast(4).joinToString("\n") { m ->
            val who = if (m.isUser) "শ" else "স"
            "$who: ${m.text.take(220)}"
        }
        val prompt = buildString {
            appendLine("বাংলাদেশের সরকারি প্রাথমিক (১ম–৫ম) শিক্ষামূলক সহায়ক। সহজ ও প্রাতিষ্ঠানিক বাংলায় উত্তর দিন।")
            appendLine("NCTB: ১–২য়(বাং/ইং/গণিত=ধারাবাহিক ৫০+সামষ্টিক ৫০), ৩–৫ম(ধারাবাহিক ৩০[TB ১০+ACT ৫+LANG ৫+CT ১০]+সামষ্টিক ৭০)।")
            if (!studentContext.isNullOrBlank()) {
                appendLine("শিক্ষার্থী: ${studentContext.take(280)}")
            }
            if (recentTurns.isNotBlank()) {
                appendLine("আলাপ:\n$recentTurns")
            }
            append("প্রশ্ন: $latestUserMessage")
        }

        val payload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", if (modelMode == AiModelMode.FAST) 0.3 else 0.5)
                put("maxOutputTokens", 1024)
            })
        }

        val (rawReply, usedModel) = callGenerateContentResilient(apiKey, modelId, payload)
        val replyText = rawReply.ifBlank { "দুঃখিত, এই মুহূর্তে উত্তর পাওয়া যায়নি। আবার চেষ্টা করুন।" }
        val modeTag = if (modelMode == AiModelMode.FAST) "দ্রুত" else "বিস্তারিত"
        val fallbackNote = if (usedModel != modelId) " · বিকল্প" else ""
        return AiChatReply(
            text = replyText,
            modelLabel = "$usedModel · $modeTag$fallbackNote",
            isPlaceholder = false
        )
    }

    override suspend fun draftRemark(
        studentSummary: String,
        term: Int
    ): AiDraftRemarkResult {
        val apiKey = requireApiKey()
        val modelId = resolveModelId(AiModelMode.FAST)
        val trimmedSummary = studentSummary.trim().take(260)
        val cacheKey = "$modelId|$term|${trimmedSummary.hashCode()}"
        synchronized(cacheLock) {
            remarkCache[cacheKey]?.let { return it }
        }

        val prompt = "${termNameBn(term)}ের জন্য নিচের তথ্যে ২–৩ বাক্যের প্রাতিষ্ঠানিক বাংলা মন্তব্য লিখুন (শুধু মন্তব্য):\n$trimmedSummary"

        val payload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.35)
                put("maxOutputTokens", 220)
            })
        }

        val (txt, _) = callGenerateContentResilient(apiKey, modelId, payload)
        val cleanRemark = txt.ifBlank {
            "${termNameBn(term)}ে শিক্ষার্থীর অগ্রগতি সন্তোষজনক; নিয়মিত অনুশীলন চালিয়ে গেলে আরও ভালো করবে।"
        }
        return AiDraftRemarkResult(remark = cleanRemark, isPlaceholder = false).also { result ->
            synchronized(cacheLock) { remarkCache[cacheKey] = result }
        }
    }

    internal suspend fun requestVisionJson(
        bitmap: Bitmap,
        prompt: String,
        maxOutputTokens: Int = 4096,
        documentEnhance: Boolean = true
    ): Pair<String, String> {
        val apiKey = requireApiKey()
        val modelId = resolveModelId(null)
        val encoded = withContext(Dispatchers.Default) {
            VisionImageOptimizer.prepareForVisionUpload(bitmap, documentEnhance)
        }
        val payload = buildVisionJsonPayload(encoded.base64, prompt, maxOutputTokens = maxOutputTokens)
        return callGenerateContentResilient(apiKey, modelId, payload)
    }

    override suspend fun analyzeMarksheetImage(
        bitmap: Bitmap,
        teacherNote: String,
        documentEnhance: Boolean
    ): ExtractedMarksheet {
        val apiKey = requireApiKey()
        val modelId = resolveModelId(null)
        val encoded = withContext(Dispatchers.Default) {
            VisionImageOptimizer.prepareForVisionUpload(bitmap, documentEnhance)
        }
        val cleanNote = teacherNote.trim()
        val cacheKey = "$modelId|${encoded.sha256Hex}|$documentEnhance|$cleanNote"
        synchronized(cacheLock) {
            singleSheetCache[cacheKey]?.let { return it }
        }

        val prompt = """
            বাংলাদেশ প্রাথমিক নম্বরপত্র OCR। শুধু JSON দিন।
            নোট: ${cleanNote.ifBlank { "নেই" }}
            বিষয়: BAN, ENG, MATH, SOC(১-২), SCI(৩-৫), BGW, REL, ART, PE। অংশ: TB, ACT, LANG, CT, WR, CONT, TOTAL। কী: "<SUB>_<TERM>_<COMP>"।
            নম্বর নিয়ম: ১ম-২য় শ্রেণিতে SOC, REL, ART, PE এবং ৩য়-৫ম শ্রেণিতে ART, PE বিষয়ে সামষ্টিক (WR) নেই (শুধু ধারাবাহিক ৫০)।
            অস্পষ্ট থাকলে uncertainNotes-এ বাংলায় লিখুন।
            {"summary":"","term":1,"student":{"name":"","cls":3,"roll":1,"guardian":"","religion":"ইসলাম"},"marks":[{"key":"MATH_1_TB","value":9.0,"subjectLabel":"গণিত (পাঠ্যবই)"}],"attendanceAndTraits":{"days":"","present":"","act":"","disc":"","cond":"","remark":""},"uncertainNotes":[]}
        """.trimIndent()

        val payload = buildVisionJsonPayload(encoded.base64, prompt, maxOutputTokens = 4096)
        val (raw, usedModel) = callGenerateContentResilient(apiKey, modelId, payload)
        return try {
            val parsed = parseExtractedMarksheetJson(parseMarksheetJsonRobust(raw))
            val withFallbackNote = if (usedModel != modelId) {
                parsed.copy(
                    uncertainNotes = parsed.uncertainNotes +
                        "মূল মডেল ($modelId) ব্যস্ত থাকায় বিকল্প মডেল ($usedModel) দিয়ে স্ক্যান সম্পন্ন হয়েছে।"
                )
            } else parsed
            synchronized(cacheLock) { singleSheetCache[cacheKey] = withFallbackNote }
            withFallbackNote
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException("ছবিটি থেকে কাঠামোবদ্ধ ডেটা পড়া যায়নি ($usedModel)। আরও স্পষ্ট ছবি তুলে আবার চেষ্টা করুন।", e)
        }
    }

    override suspend fun analyzeClassSheetImage(
        bitmap: Bitmap,
        clsHint: Int?,
        termHint: Int?,
        subKeyHint: String?,
        teacherNote: String,
        importScope: ClassSheetImportScope,
        documentEnhance: Boolean,
        sheetKindHint: ClassSheetKind?,
        qrMeta: BlankSheetQrMeta?
    ): ExtractedClassSheet {
        val apiKey = requireApiKey()
        val modelId = resolveModelId(null)
        val (encoded, resolvedQr) = withContext(Dispatchers.Default) {
            val qr = qrMeta ?: SheetQrScanner.decodeFromBitmap(bitmap)
            val enc = VisionImageOptimizer.prepareForVisionUpload(bitmap, documentEnhance)
            enc to qr
        }

        val effClsHint = clsHint ?: resolvedQr?.cls
        val effTermHint = termHint ?: resolvedQr?.term?.takeIf { it in 1..3 }
        val effSubHint = subKeyHint?.takeIf {
            it.isNotBlank() && !it.equals(BlankSheetCatalog.ALL_SUBJECTS_KEY, ignoreCase = true)
        } ?: resolvedQr?.subjectKey?.takeIf {
            it.isNotBlank() && !it.equals(BlankSheetCatalog.ALL_SUBJECTS_KEY, ignoreCase = true)
        }
        val effKindHint = sheetKindHint ?: resolvedQr?.sheetKind
        val effScope = if (importScope == ClassSheetImportScope.AUTO && resolvedQr != null && resolvedQr.importScope != ClassSheetImportScope.AUTO) {
            resolvedQr.importScope
        } else {
            importScope
        }

        val cleanNote = teacherNote.trim()
        val qrKeyPart = resolvedQr?.let {
            "${it.templateId}|${it.sheetKind.name}|${it.refCode}|${it.numeralStyle.name}|${it.columnIds.joinToString(",")}|${it.customColumnTitles.joinToString(",")}|${it.workingDaysByTerm}"
        }.orEmpty()
        val cacheKey = "$modelId|${encoded.sha256Hex}|$documentEnhance|$effClsHint|$effTermHint|$effSubHint|${effScope.name}|${effKindHint?.name}|$qrKeyPart|$cleanNote"
        synchronized(cacheLock) {
            classSheetCache[cacheKey]?.let { return it }
        }

        val targetCls = effClsHint ?: 3
        val allSubs = subjectsFor(targetCls, RELIGIONS.first())
        val scopedSubs = if (!effSubHint.isNullOrBlank()) {
            allSubs.filter { it.key == effSubHint }.ifEmpty { allSubs }
        } else {
            allSubs
        }
        val subsInfo = scopedSubs.joinToString("; ") { s ->
            val compDesc = s.comps.joinToString(",") { "${it.key}=${it.max}" }
            val wrDesc = if (s.written > 0) ",WR=${s.written}" else ""
            "${s.key}(${s.bn}):$compDesc$wrDesc"
        }

        val scopeRule = when {
            effKindHint == ClassSheetKind.ATTENDANCE_TRAITS ->
                "4. উপস্থিতি ও গুণাবলি শিট (`ATTENDANCE_TRAITS`): উপরের মোট কার্যদিবস স্ট্রিপ থাকলে `sharedWorkingDays` (`\"1\"`,`\"2\"`,`\"3\"`)-এ দিন, এবং প্রতি সারিতে `traits`-এ `DAYS_t`, `PRES_t`, `ACT_t` (সক্রিয়তা/সহপাঠ), `DISC_t` (শৃঙ্খলা), `COND_t` (আচরণ), `REM_t` (মন্তব্য) দিন (গুণাবলি মান `ক`, `খ` বা `গ`; `marks` দেবেন না)।"
            effKindHint == ClassSheetKind.THREE_TERM_MARKS ->
                "4. ৩ প্রান্তিক নম্বর শিট (`THREE_TERM_MARKS`): পাশাপাশি ১ম, ২য়, ৩য় প্রান্তিকের ধারাবাহিক/সামষ্টিক/মোট `marks`-এ `" +
                    "\"T1_CONT\",\"T1_WR\",\"T1_TOTAL\",\"T2_CONT\",\"T2_WR\",\"T2_TOTAL\",\"T3_CONT\",\"T3_WR\",\"T3_TOTAL\"` হিসেবে কেবল টেবিলে থাকা কলামগুলো দিন এবং থাকলে `remark` দিন।"
            effScope == ClassSheetImportScope.CONT_ONLY ->
                "4. একক প্রান্তিক নম্বর হলে কেবল রোল (ও থাকলে নাম) এবং ধারাবাহিক নম্বর (`sheetContinuousTotal` বা `TB`/`ACT`/`LANG`/`CT`) পড়ুন — `WR` ও `sheetGrandTotal` দেবেন না।"
            effScope == ClassSheetImportScope.WR_ONLY ->
                "4. একক প্রান্তিক নম্বর হলে কেবল রোল (ও থাকলে নাম) এবং সামষ্টিক/লিখিত (`WR`) পড়ুন — ধারাবাহিক ও `sheetGrandTotal` দেবেন না।"
            effScope == ClassSheetImportScope.BOTH ->
                "4. একক প্রান্তিক নম্বর হলে টেবিলের থাকা কলাম অনুযায়ী `marks`, `sheetContinuousTotal`, `sheetGrandTotal` এবং থাকলে `remark` পড়ুন। সংক্ষিপ্ত ফর্মে (TB/ACT/LANG/CT না থাকলে) ধারাবাহিক সরাসরি `sheetContinuousTotal`-এ দিন।"
            else ->
                """
                4. ছবির কলাম ও শিরোনাম দেখে সঠিক `sheetKind` বেছে নিন:
                   (ক) "SINGLE_TERM_MARKS": ১ প্রান্তিক নম্বর শিট (পূর্ণাঙ্গ `TB,ACT,LANG,CT,CONT,WR,TOTAL`, সংক্ষিপ্ত `CONT,WR,TOTAL`, শুধু ধারাবাহিক, শুধু সামষ্টিক, অথবা সহজ মোট `TOTAL` + থাকলে `remark`)। কেবল `মোট` থাকলে `sheetGrandTotal` দিন, `CONT`/`WR` অনুমান করবেন না।
                   (খ) "THREE_TERM_MARKS": ৩ প্রান্তিক নম্বর শিট (পাশাপাশি ১ম, ২য়, ৩য় প্রান্তিকের ধারাবাহিক/সামষ্টিক/মোট) → `marks`-এ `"T1_CONT","T1_WR","T1_TOTAL","T2_CONT","T2_WR","T2_TOTAL","T3_CONT","T3_WR","T3_TOTAL"` দিন।
                   (গ) "ATTENDANCE_TRAITS": উপস্থিতি ও গুণাবলি/হাজিরা শিট → উপরের মোট কার্যদিবস স্ট্রিপ থাকলে `sharedWorkingDays` ও প্রতি সারির `traits`-এ `DAYS_1..3`, `PRES_1..3`, `ACT_1..3`, `DISC_1..3`, `COND_1..3`, `REM_1..3` দিন (গুণাবলি `ক`/`খ`/`গ`)।
                """.trimIndent()
        }
        val forcedKindHint = effKindHint?.let { "ফরম্যাট অবশ্যই: ${it.name}" } ?: "ফরম্যাট: ছবি অনুযায়ী স্বয়ংক্রিয়"
        val qrInstructionsBlock = resolvedQr?.let { SheetQrCodec.buildAiPromptInstructions(it) }.orEmpty()
        val structLine = if (effKindHint == ClassSheetKind.ATTENDANCE_TRAITS) {
            "উপস্থিতি ও গুণাবলি কাঠামো: কার্যদিবস=DAYS_t, উপস্থিত/উপস্থিতি=PRES_t, সক্রিয়তা/সহপাঠ=ACT_t, শৃঙ্খলা=DISC_t, আচরণ=COND_t, মন্তব্য=REM_t; গুণাবলি মান: ক (উত্তম), খ (সন্তোষজনক), গ (উন্নতি প্রয়োজন)"
        } else {
            "বিষয় কাঠামো: $subsInfo"
        }
        val dynamicSchemaJson = SheetQrCodec.buildDynamicRowJsonSchema(
            meta = resolvedQr,
            kind = effKindHint,
            effCls = effClsHint,
            effTerm = effTermHint,
            effSub = effSubHint,
            effScope = effScope
        )

        val prompt = buildString {
            appendLine("বাংলাদেশ প্রাথমিক শ্রেণি-ফর্ম/নম্বরপত্র OCR। নিচের ফুটারে `REF:C...` কোড বা উপরের ব্যানার ও কলাম পড়ে সব সারির তথ্য শুধু JSON-এ দিন।")
            appendLine("ইঙ্গিত: শ্রেণি=${effClsHint ?: "ছবি_থেকে_শনাক্ত(1-20)"}, প্রান্তিক=${effTermHint ?: "ছবি_থেকে_শনাক্ত(1-3)"}, বিষয়=${if (effKindHint == ClassSheetKind.ATTENDANCE_TRAITS) "উপস্থিতি ও গুণাবলি (প্রযোজ্য নয়)" else effSubHint ?: "ছবি_থেকে_শনাক্ত"}, $forcedKindHint, নোট=${cleanNote.ifBlank { "নেই" }}")
            if (qrInstructionsBlock.isNotBlank()) {
                appendLine(qrInstructionsBlock)
            }
            appendLine(structLine)
            appendLine("নিয়ম:")
            appendLine("1. সব সংখ্যা ইংরেজি অঙ্কে (০৭->\"7\"); নাম ও মন্তব্য থাকলে বাংলায়, না থাকলে \"\"।")
            appendLine("2. CT ঘরে \"০৬+০৭=১৩/২=০৭\" থাকলে \"=\" পরের গড় মান (\"7\") ও `rawNotes`-এ সংক্ষেপে লিখুন।")
            appendLine("3. `A`/`অনু` থাকলে \"0\" ও `rawNotes`-এ \"অনুপস্থিত (A)\"; ফাঁকা ঘর \"\"। গ্রেড/জিপি/স্বাক্ষর কলাম বাদ দিন।")
            appendLine(scopeRule)
            appendLine("5. টেবিলে মুদ্রিত প্রতিটি শিক্ষার্থীর সারি (`roll` ও থাকলে `name`) অবশ্যই `rows`-এ রাখুন, এমনকি তাদের নম্বর ঘরগুলো ফাঁকা থাকলেও (`marks:{}`)। যে ঘরগুলোতে কোনো নম্বর বা লেখা নেই, সেগুলোর ফাঁকা কী (`\"\"`) বাদ দিয়ে JSON সংক্ষিপ্ত রাখুন। `refCode`-এ ফুটারে `REF:...` পেলে সেটি দিন, নাহলে \"${resolvedQr?.refCode.orEmpty()}\"।")
            appendLine("6. ছবিতে আসলে কোনো লেখা/সংখ্যা অস্পষ্ট হলে তবেই `uncertainFields`-এ কারণ দিন। `metaFound`-এ শ্রেণি/প্রান্তিক/বিষয় পেলে true দিন।")
            append("JSON: $dynamicSchemaJson")
        }

        val payload = buildVisionJsonPayload(encoded.base64, prompt, maxOutputTokens = 8192)
        val (raw, usedModel) = callGenerateContentResilient(apiKey, modelId, payload)
        return try {
            val parsed = parseExtractedClassSheetJson(
                json = parseClassSheetJsonRobust(raw, effClsHint, effTermHint, effSubHint),
                clsHint = effClsHint,
                termHint = effTermHint,
                subKeyHint = effSubHint,
                requestedScope = effScope,
                sheetKindHint = effKindHint,
                qrMeta = resolvedQr
            )
            val withExtraInfo = if (usedModel != modelId) {
                parsed.copy(
                    warnings = parsed.warnings +
                        "নির্বাচিত মডেল ($modelId) ব্যস্ত থাকায় বিকল্প মডেল ($usedModel) দিয়ে স্ক্যান সম্পন্ন হয়েছে।"
                )
            } else parsed
            synchronized(cacheLock) { classSheetCache[cacheKey] = withExtraInfo }
            withExtraInfo
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException("ছবিটি থেকে শ্রেণি-নম্বরপত্র পড়া যায়নি ($usedModel)। আরও স্পষ্ট ও সোজা ছবি তুলে আবার চেষ্টা করুন।", e)
        }
    }

    override suspend fun analyzeAutoSheetImage(
        bitmap: Bitmap,
        clsHint: Int?,
        termHint: Int?,
        subKeyHint: String?,
        teacherNote: String,
        importScope: ClassSheetImportScope,
        documentEnhance: Boolean,
        sheetKindHint: ClassSheetKind?,
        qrMeta: BlankSheetQrMeta?
    ): AutoScanResult {
        val resolvedQr = qrMeta ?: withContext(Dispatchers.Default) {
            SheetQrScanner.decodeFromBitmap(bitmap)
        }
        if (resolvedQr != null) {
            return AutoScanResult.ClassSheet(
                analyzeClassSheetImage(
                    bitmap = bitmap,
                    clsHint = clsHint,
                    termHint = termHint,
                    subKeyHint = subKeyHint,
                    teacherNote = teacherNote,
                    importScope = importScope,
                    documentEnhance = documentEnhance,
                    sheetKindHint = sheetKindHint,
                    qrMeta = resolvedQr
                )
            )
        }

        val apiKey = requireApiKey()
        val modelId = resolveModelId(null)
        val encoded = withContext(Dispatchers.Default) {
            VisionImageOptimizer.prepareForVisionUpload(bitmap, documentEnhance)
        }
        val cleanNote = teacherNote.trim()
        val cacheKey = "auto|$modelId|${encoded.sha256Hex}|$documentEnhance|$clsHint|$termHint|$subKeyHint|${importScope.name}|${sheetKindHint?.name}|$cleanNote"
        synchronized(cacheLock) {
            autoScanCache[cacheKey]?.let { return it }
        }

        val targetCls = clsHint ?: 3
        val subsInfo = subjectsFor(targetCls, RELIGIONS.first()).joinToString("; ") { s ->
            val compDesc = s.comps.joinToString(",") { "${it.key}=${it.max}" }
            val wrDesc = if (s.written > 0) ",WR=${s.written}" else ""
            "${s.key}(${s.bn}):$compDesc$wrDesc"
        }

        val prompt = buildString {
            appendLine("বাংলাদেশ প্রাথমিক বিদ্যালয়ের নম্বরপত্র / শ্রেণি-তালিকা অটো-ডিটেক্ট OCR। শুধু JSON দিন।")
            appendLine("প্রথমে ছবির ধরন (`scanMode`) এবং লক্ষ্য (`sheetKind`, `cls`, `term`, `subKey`) স্বয়ংক্রিয়ভাবে শনাক্ত করুন:")
            appendLine("• বহু-শিক্ষার্থীর শ্রেণি তালিকা / রোল-ভিত্তিক নম্বর শিট / হাজিরা ও গুণাবলি শিট হলে `scanMode`: \"CLASS_SHEET\" দিন এবং সব সারি `rows`-এ দিন।")
            appendLine("• শুধুমাত্র ১ জন শিক্ষার্থীর একক প্রগ্রেসিভ রিপোর্ট কার্ড / একক নম্বরপত্র হলে `scanMode`: \"SINGLE_STUDENT\" দিন এবং `student`, `marks`, `attendanceAndTraits` পূরণ করুন।")
            appendLine("ঐচ্ছিক ইঙ্গিত: শ্রেণি=${clsHint ?: "ছবি_থেকে_শনাক্ত(1-20)"}, প্রান্তিক=${termHint ?: "ছবি_থেকে_শনাক্ত(1-3)"}, বিষয়=${subKeyHint ?: "ছবি_থেকে_শনাক্ত(BAN/ENG/MATH/SOC/SCI/BGW/REL/PE/ART)"}, শিট=${sheetKindHint?.name ?: "অটো"}, নোট=${cleanNote.ifBlank { "নেই" }}")
            appendLine("বিষয় কাঠামো: $subsInfo")
            appendLine("নিয়ম:")
            appendLine("1. সব সংখ্যা ইংরেজি অঙ্কে (০৭->\"7\"); ফুটারে `REF:...` পেলে `refCode`-এ দিন।")
            appendLine("2. `CLASS_SHEET` হলে `sheetKind`:")
            appendLine("   - \"SINGLE_TERM_MARKS\": ১ প্রান্তিক নম্বর (রোল, নাম, `TB,ACT,LANG,CT,CONT,WR,TOTAL`)।")
            appendLine("   - \"THREE_TERM_MARKS\": ৩ প্রান্তিক নম্বর (`T1_CONT,T1_WR,T1_TOTAL .. T3_TOTAL`)।")
            appendLine("   - \"ATTENDANCE_TRAITS\": উপস্থিতি ও গুণাবলি শিট (যেমন: রোল, নাম, উপস্থিত/উপস্থিতি, সক্রিয়তা/সহপাঠ, শৃঙ্খলা, আচরণ — ১ প্রান্তিক বা ৩ প্রান্তিক)। উপরের মোট কার্যদিবস থাকলে `sharedWorkingDays` ও প্রতি সারির `traits`-এ `DAYS_t, PRES_t, ACT_t, DISC_t, COND_t, REM_t` দিন (গুণাবলি `ক`/`খ`/`গ`, `marks` `{}` রাখুন)।")
            appendLine("3. টেবিলে মুদ্রিত প্রতিটি শিক্ষার্থীর সারি (`roll` ও থাকলে `name`) অবশ্যই `rows`-এ রাখুন, এমনকি তাদের নম্বর ঘরগুলো ফাঁকা থাকলেও (`marks:{}`)। ফাঁকা ঘরগুলোর অপ্রয়োজনীয় `\"\"` কী বাদ দিয়ে JSON সংক্ষিপ্ত রাখুন।")
            append("JSON স্কিমা: {\"scanMode\":\"CLASS_SHEET\",\"sheetKind\":\"SINGLE_TERM_MARKS\",\"refCode\":\"\",\"cls\":3,\"term\":1,\"subKey\":\"MATH\",\"metaFound\":{\"cls\":true,\"term\":true,\"sub\":true},\"sharedWorkingDays\":{\"1\":\"\",\"2\":\"\",\"3\":\"\"},\"summary\":\"\",\"warnings\":[],\"rows\":[{\"roll\":\"1\",\"name\":\"\",\"marks\":{\"TB\":\"\",\"ACT\":\"\",\"LANG\":\"\",\"CT\":\"\",\"WR\":\"\",\"T1_CONT\":\"\",\"T1_WR\":\"\"},\"sheetContinuousTotal\":\"\",\"sheetGrandTotal\":\"\",\"remark\":\"\",\"traits\":{\"PRES_1\":\"\",\"ACT_1\":\"\",\"DISC_1\":\"\",\"COND_1\":\"\",\"REM_1\":\"\"},\"rawNotes\":\"\",\"uncertainFields\":{}}],\"student\":{\"name\":\"\",\"cls\":3,\"roll\":1,\"guardian\":\"\",\"religion\":\"ইসলাম\"},\"marks\":[],\"attendanceAndTraits\":{\"days\":\"\",\"present\":\"\",\"act\":\"\",\"disc\":\"\",\"cond\":\"\",\"remark\":\"\"},\"uncertainNotes\":[]}")
        }

        val payload = buildVisionJsonPayload(encoded.base64, prompt, maxOutputTokens = 8192)
        val (raw, usedModel) = callGenerateContentResilient(apiKey, modelId, payload)
        return try {
            val result = parseAutoScanJson(
                raw = raw,
                clsHint = clsHint,
                termHint = termHint,
                subKeyHint = subKeyHint,
                requestedScope = importScope,
                sheetKindHint = sheetKindHint,
                qrMeta = null
            )
            val enriched = if (usedModel != modelId) {
                when (result) {
                    is AutoScanResult.ClassSheet -> AutoScanResult.ClassSheet(
                        result.sheet.copy(
                            warnings = result.sheet.warnings +
                                "নির্বাচিত মডেল ($modelId) ব্যস্ত থাকায় বিকল্প মডেল ($usedModel) দিয়ে স্ক্যান সম্পন্ন হয়েছে।"
                        )
                    )
                    is AutoScanResult.SingleStudent -> AutoScanResult.SingleStudent(
                        result.marksheet.copy(
                            uncertainNotes = result.marksheet.uncertainNotes +
                                "মূল মডেল ($modelId) ব্যস্ত থাকায় বিকল্প মডেল ($usedModel) দিয়ে স্ক্যান সম্পন্ন হয়েছে।"
                        )
                    )
                }
            } else result
            synchronized(cacheLock) { autoScanCache[cacheKey] = enriched }
            enriched
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException("ছবিটি অটো-স্ক্যান করা যায়নি ($usedModel)। আরও স্পষ্ট ও সোজা ছবি তুলে আবার চেষ্টা করুন।", e)
        }
    }

    private fun buildVisionJsonPayload(
        imageBase64: String,
        prompt: String,
        maxOutputTokens: Int = 2048
    ): JSONObject =
        JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", imageBase64)
                            })
                        })
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
                put("maxOutputTokens", maxOutputTokens)
                put("responseMimeType", "application/json")
            })
        }

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        internal const val DEFAULT_RATE_LIMIT_COOLDOWN_SEC = 15
        internal const val ZERO_QUOTA_COOLDOWN_SEC = 45

        internal enum class Config400Kind {
            THINKING_CONFIG,
            RESPONSE_MIME_TYPE,
            OTHER
        }

        internal data class ModelQueuePlan(
            val healthyModels: List<String>,
            val shortestCooldownSec: Int,
            val coolingSummary: String,
            val expiredKeysToPrune: List<String>
        )

        internal class GeminiHttpException(
            val httpCode: Int,
            val modelId: String,
            userMessage: String,
            val retryAfterSeconds: Int? = null,
            val rawErrorBody: String = "",
            val isZeroQuota: Boolean = false
        ) : IllegalStateException(userMessage)

        internal class GeminiNetworkException(
            val isTimeout: Boolean,
            val isConnectionError: Boolean,
            userMessage: String,
            cause: Throwable? = null
        ) : IllegalStateException(userMessage, cause)

        internal fun stripModelPrefix(raw: String): String =
            raw.trim().removePrefix("models/").trim()

        internal fun hasNetworkTransport(context: Context): Boolean {
            return runCatching {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    ?: return true
                val active = cm.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(active) ?: return false
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }.getOrDefault(true)
        }

        internal fun buildModelQueuePlan(
            userOrdered: List<String>,
            cooldownUntilMs: Map<String, Long>,
            nowMs: Long,
            cachedIds: Set<String> = emptySet()
        ): ModelQueuePlan {
            val healthy = mutableListOf<String>()
            val cooling = mutableListOf<Pair<String, Int>>()
            val expired = mutableListOf<String>()

            for (raw in userOrdered) {
                val clean = GeminiApiConfig.normalizeModelId(raw, cachedIds)
                if (clean.isBlank()) continue
                val until = cooldownUntilMs[clean]
                if (until == null || until <= nowMs) {
                    if (until != null) expired.add(clean)
                    healthy.add(clean)
                } else {
                    val waitSec = ceil((until - nowMs) / 1000.0).toInt().coerceAtLeast(1)
                    cooling.add(clean to waitSec)
                }
            }
            val sortedCooling = cooling.sortedBy { it.second }
            val shortest = sortedCooling.firstOrNull()?.second ?: DEFAULT_RATE_LIMIT_COOLDOWN_SEC
            val summary = sortedCooling.joinToString(", ") { "${it.first} (${bn(it.second.toString())}সে.)" }
            return ModelQueuePlan(
                healthyModels = healthy,
                shortestCooldownSec = shortest,
                coolingSummary = summary,
                expiredKeysToPrune = expired
            )
        }

        internal fun bodyHasInlineImage(body: JSONObject): Boolean {
            val contents = body.optJSONArray("contents") ?: return false
            for (i in 0 until contents.length()) {
                val parts = contents.optJSONObject(i)?.optJSONArray("parts") ?: continue
                for (j in 0 until parts.length()) {
                    val part = parts.optJSONObject(j) ?: continue
                    if (part.has("inline_data") || part.has("inlineData")) return true
                }
            }
            return false
        }

        internal fun isKnownTextOnlyModel(modelId: String): Boolean {
            val lower = GeminiApiConfig.normalizeModelId(modelId).lowercase()
            return lower.startsWith("gemma-1") ||
                lower.startsWith("gemma-2") ||
                lower.startsWith("gemma-3-1b") ||
                lower.startsWith("gemma-3n-e2b")
        }

        internal fun classify400Error(rawBody: String): Config400Kind {
            val lower = rawBody.lowercase()
            return when {
                lower.contains("thinkingconfig") ||
                    lower.contains("thinking_config") ||
                    lower.contains("thinking_budget") ||
                    lower.contains("thinkingbudget") ||
                    lower.contains("thinking_level") ||
                    lower.contains("thinkinglevel") ||
                    lower.contains("thinking mode") ||
                    lower.contains("thinking is not supported") -> Config400Kind.THINKING_CONFIG
                lower.contains("responsemimetype") ||
                    lower.contains("response_mime_type") ||
                    lower.contains("json mode") ||
                    lower.contains("application/json") -> Config400Kind.RESPONSE_MIME_TYPE
                else -> Config400Kind.OTHER
            }
        }

        internal fun isZeroQuotaError(respText: String): Boolean {
            val lower = respText.lowercase()
            return Regex("limit\\s*[:=]\\s*0\\b").containsMatchIn(lower) ||
                lower.contains("per_day") ||
                lower.contains("perday") ||
                lower.contains("generaterequestsperday")
        }

        private fun thinkingStepsForModel(cleanModel: String, body: JSONObject): List<JSONObject?> {
            val genCfg = body.optJSONObject("generationConfig")
            val isJsonExtraction = genCfg?.optString("responseMimeType") == "application/json"
            if (!isJsonExtraction) return listOf(null)
            val lower = cleanModel.lowercase()
            return when {
                lower.contains("2.5") -> {
                    val budget = if (lower.contains("pro")) 128 else 0
                    listOf(JSONObject().put("thinkingBudget", budget))
                }
                Regex("gemini-(?:3|4)(?:\\.|-)").containsMatchIn(lower) -> {
                    if (lower.contains("flash")) {
                        listOf(
                            JSONObject().put("thinkingLevel", "MINIMAL"),
                            JSONObject().put("thinkingLevel", "LOW")
                        )
                    } else {
                        listOf(JSONObject().put("thinkingLevel", "LOW"))
                    }
                }
                else -> listOf(null)
            }
        }

        private fun cloneBodyWithGenerationTweaks(
            baseBody: JSONObject,
            thinkingCfg: JSONObject?,
            stripJsonMimeType: Boolean
        ): JSONObject {
            if (thinkingCfg == null && !stripJsonMimeType) return baseBody
            val oldGen = baseBody.optJSONObject("generationConfig") ?: JSONObject()
            val newGen = JSONObject(oldGen.toString()).apply {
                if (thinkingCfg != null) put("thinkingConfig", thinkingCfg)
                if (stripJsonMimeType) remove("responseMimeType")
            }
            return JSONObject().apply {
                val keys = baseBody.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    if (k != "generationConfig") put(k, baseBody.get(k))
                }
                put("generationConfig", newGen)
            }
        }

        internal suspend fun callGenerateContent(
            apiKey: String,
            modelId: String,
            body: JSONObject,
            preSerializedBody: ByteArray? = null
        ): String = withContext(Dispatchers.IO) {
            val cleanModel = stripModelPrefix(modelId)
            val hasImage = bodyHasInlineImage(body)
            if (hasImage && isKnownTextOnlyModel(cleanModel)) {
                throw GeminiHttpException(
                    httpCode = 400,
                    modelId = cleanModel,
                    userMessage = "নির্বাচিত মডেল ($cleanModel) ছবি স্ক্যান সমর্থন করে না। সেটিংস থেকে একটি Gemini Flash মডেল নির্বাচন করুন।"
                )
            }

            val hasJsonMime = body.optJSONObject("generationConfig")?.optString("responseMimeType") == "application/json"
            val thinkingSteps = thinkingStepsForModel(cleanModel, body)
            val isGemmaModel = cleanModel.startsWith("gemma-", ignoreCase = true)

            var activeThinkingIdx = 0
            var stripJson = isGemmaModel && hasJsonMime
            var clearedThinkingFallback = false
            var attempts = 0

            while (attempts < 3) {
                attempts++
                val currentThinking = if (clearedThinkingFallback || isGemmaModel) {
                    null
                } else {
                    thinkingSteps.getOrNull(activeThinkingIdx)
                }

                val effectiveBytes = if (currentThinking == null && !stripJson && preSerializedBody != null) {
                    preSerializedBody
                } else {
                    cloneBodyWithGenerationTweaks(body, currentThinking, stripJson)
                        .toString()
                        .toByteArray(Charsets.UTF_8)
                }
                try {
                    return@withContext executeSingleGenerateCall(apiKey, cleanModel, effectiveBytes)
                } catch (e: GeminiHttpException) {
                    if (e.httpCode != 400) throw e
                    when (classify400Error(e.rawErrorBody)) {
                        Config400Kind.THINKING_CONFIG -> {
                            if (!clearedThinkingFallback) {
                                if (activeThinkingIdx + 1 < thinkingSteps.size) {
                                    activeThinkingIdx++
                                    continue
                                } else if (currentThinking != null) {
                                    clearedThinkingFallback = true
                                    continue
                                }
                            }
                            throw e
                        }
                        Config400Kind.RESPONSE_MIME_TYPE -> {
                            if (hasJsonMime && !stripJson) {
                                stripJson = true
                                continue
                            }
                            throw e
                        }
                        Config400Kind.OTHER -> throw e
                    }
                }
            }
            throw IllegalStateException("Gemini API অনুরোধ সম্পন্ন করা যায়নি।")
        }

        private fun executeSingleGenerateCall(
            apiKey: String,
            cleanModel: String,
            bodyBytes: ByteArray
        ): String {
            val isVisionOrLarge = bodyBytes.size > 80_000
            val encodedModel = URLEncoder.encode(cleanModel, "UTF-8")
            val endpoint = "$BASE_URL/models/$encodedModel:generateContent"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 30_000
                readTimeout = if (isVisionOrLarge) 90_000 else 55_000
                doOutput = true
                setFixedLengthStreamingMode(bodyBytes.size)
                setRequestProperty("x-goog-api-key", apiKey)
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }
            try {
                conn.outputStream.use { out ->
                    out.write(bodyBytes)
                    out.flush()
                }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val respText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (code !in 200..299) {
                    val headerRetry = conn.getHeaderField("Retry-After")?.trim()?.toIntOrNull()
                    val retrySec = if (code == 429) parseRetryAfterSeconds(headerRetry, respText) else null
                    val zeroQuota = code == 429 && isZeroQuotaError(respText)
                    throw GeminiHttpException(
                        httpCode = code,
                        modelId = cleanModel,
                        userMessage = friendlyHttpError(code, respText, cleanModel, retrySec),
                        retryAfterSeconds = retrySec,
                        rawErrorBody = respText,
                        isZeroQuota = zeroQuota
                    )
                }
                val json = runCatching { JSONObject(respText) }.getOrElse { parseErr ->
                    throw IllegalStateException(
                        "সার্ভার থেকে অপ্রত্যাশিত প্রতিক্রিয়া এসেছে ($cleanModel)। আবার চেষ্টা করুন।",
                        parseErr
                    )
                }
                return extractCandidateText(json)
            } catch (e: GeminiHttpException) {
                throw e
            } catch (e: GeminiNetworkException) {
                throw e
            } catch (e: IllegalStateException) {
                throw e
            } catch (e: Exception) {
                throw buildNetworkException(e)
            } finally {
                conn.disconnect()
            }
        }

        internal fun extractCandidateText(json: JSONObject): String {
            val apiErrorMsg = json.optJSONObject("error")?.optString("message")?.trim().orEmpty()
            if (apiErrorMsg.isNotEmpty()) {
                throw IllegalStateException("Gemini API ত্রুটি: $apiErrorMsg")
            }

            val blockReason = json.optJSONObject("promptFeedback")?.optString("blockReason", "")?.trim().orEmpty()
            val candidates = json.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                if (blockReason.isNotEmpty()) {
                    throw IllegalStateException(
                        "সার্ভার ফিল্টারের কারণে ছবিটি গ্রহণ করা হয়নি ($blockReason)। পরিষ্কার ছবি দিয়ে আবার চেষ্টা করুন।"
                    )
                }
                throw IllegalStateException("AI মডেল থেকে কোনো উত্তর পাওয়া যায়নি। আবার চেষ্টা করুন।")
            }

            val first = candidates.optJSONObject(0)
                ?: throw IllegalStateException("AI মডেল থেকে কোনো উত্তর পাওয়া যায়নি। আবার চেষ্টা করুন।")
            val finishReason = first.optString("finishReason", "").trim().uppercase()
            val content = first.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            var hasThoughtParts = false
            val text = if (parts != null) {
                buildString {
                    for (i in 0 until parts.length()) {
                        val p = parts.optJSONObject(i) ?: continue
                        if (p.optBoolean("thought", false)) {
                            hasThoughtParts = true
                            continue
                        }
                        val t = p.optString("text", "")
                        if (t.isNotEmpty()) append(t)
                    }
                }.trim()
            } else ""

            if (text.isBlank()) {
                when (finishReason) {
                    "MAX_TOKENS" -> throw IllegalStateException(
                        "মডেলের আউটপুট টোকেন সীমায় আটকে গেছে। ছবিটি ছোট করে ক্রপ করুন অথবা ২ ভাগে স্ক্যান করুন।"
                    )
                    "SAFETY", "IMAGE_SAFETY", "RECITATION", "PROHIBITED_CONTENT", "SPII", "BLOCKLIST" -> throw IllegalStateException(
                        "সার্ভার ফিল্টারের কারণে ছবিটি প্রসেস করা যায়নি ($finishReason)। পরিষ্কার ছবি দিয়ে আবার চেষ্টা করুন।"
                    )
                    else -> {
                        val reasonTag = when {
                            finishReason.isNotBlank() -> finishReason
                            hasThoughtParts -> "THOUGHT_ONLY"
                            else -> "EMPTY_RESPONSE"
                        }
                        throw IllegalStateException(
                            "AI মডেল থেকে কোনো টেক্সট পাওয়া যায়নি ($reasonTag)। আবার চেষ্টা করুন বা অন্য Flash মডেল নির্বাচন করুন।"
                        )
                    }
                }
            }
            return text
        }

        suspend fun fetchModelsOnly(rawApiKey: String): Result<List<DiscoveredGeminiModel>> = withContext(Dispatchers.IO) {
            val apiKey = GeminiApiConfig.normalizeApiKey(rawApiKey)
            if (apiKey.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("API Key দেওয়া হয়নি"))
            }
            runCatching { fetchAvailableModels(apiKey) }
        }

        suspend fun testConnectionAndListModels(
            rawApiKey: String,
            rawModel: String
        ): GeminiConnectionCheckResult = withContext(Dispatchers.IO) {
            val apiKey = GeminiApiConfig.normalizeApiKey(rawApiKey)
            if (apiKey.isBlank()) {
                return@withContext GeminiConnectionCheckResult(
                    ok = false,
                    message = "পরীক্ষা করার আগে আপনার Gemini API Key লিখুন।"
                )
            }

            // 1. Fetch all available generateContent models for this API key
            val fetchedModels = try {
                fetchAvailableModels(apiKey)
            } catch (e: Exception) {
                return@withContext GeminiConnectionCheckResult(
                    ok = false,
                    message = e.message ?: "API Key যাচাই করা যায়নি।"
                )
            }
            val fetchedIds = fetchedModels.map { it.id }.toSet()
            val cleanRequested = GeminiApiConfig.normalizeModelId(rawModel, fetchedIds)
            val effectiveModelId = cleanRequested.ifBlank {
                fetchedModels.firstOrNull()?.id.orEmpty()
            }
            if (effectiveModelId.isBlank()) {
                return@withContext GeminiConnectionCheckResult(
                    ok = false,
                    message = "এই API Key-তে কোনো উপযোগী লাইভ মডেল পাওয়া যায়নি।",
                    availableModels = fetchedModels
                )
            }

            // 2. Ping the effective model with a minimal generateContent call
            val pingPayload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "OK"))
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("maxOutputTokens", 8)
                    put("temperature", 0.0)
                })
            }
            try {
                callGenerateContent(apiKey, effectiveModelId, pingPayload)
                val statusText = if (cleanRequested.isBlank()) {
                    "API Key যাচাই সফল ($effectiveModelId)! ${bn(fetchedModels.size.toString())}টি লাইভ মডেল পাওয়া গেছে — এখন তালিকা থেকে পছন্দের ৩টি মডেল নির্বাচন করুন।"
                } else {
                    "সংযোগ সফল! সক্রিয় মডেল: $effectiveModelId · মোট ${bn(fetchedModels.size.toString())}টি মডেল পাওয়া গেছে।"
                }
                GeminiConnectionCheckResult(
                    ok = true,
                    message = statusText,
                    availableModels = fetchedModels
                )
            } catch (e: Exception) {
                val fallbackProbe = if (cleanRequested.isBlank() && e is GeminiHttpException && e.httpCode in setOf(404, 429)) {
                    fetchedModels.firstOrNull {
                        it.id != effectiveModelId && it.id.contains("flash", ignoreCase = true)
                    }?.id
                } else null
                if (fallbackProbe != null) {
                    val secondAttempt = runCatching {
                        callGenerateContent(apiKey, fallbackProbe, pingPayload)
                    }
                    if (secondAttempt.isSuccess) {
                        return@withContext GeminiConnectionCheckResult(
                            ok = true,
                            message = "API Key যাচাই সফল ($fallbackProbe)! ${bn(fetchedModels.size.toString())}টি লাইভ মডেল পাওয়া গেছে — এখন তালিকা থেকে পছন্দের ৩টি মডেল নির্বাচন করুন।",
                            availableModels = fetchedModels
                        )
                    }
                }
                val hint = if (fetchedModels.isNotEmpty()) {
                    "\nউপলব্ধ লাইভ তালিকা থেকে অন্য একটি মডেল বেছে নিন।"
                } else ""
                GeminiConnectionCheckResult(
                    ok = false,
                    message = "${e.message ?: "মডেল পরীক্ষা ব্যর্থ হয়েছে"}$hint",
                    availableModels = fetchedModels
                )
            }
        }

        private fun fetchAvailableModels(apiKey: String): List<DiscoveredGeminiModel> {
            val collected = linkedMapOf<String, DiscoveredGeminiModel>()
            var pageToken: String? = null
            var pages = 0

            while (pages < 5) {
                pages++
                val urlStr = buildString {
                    append("$BASE_URL/models?pageSize=100")
                    if (!pageToken.isNullOrBlank()) {
                        append("&pageToken=")
                        append(URLEncoder.encode(pageToken, "UTF-8"))
                    }
                }
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 25_000
                    readTimeout = 35_000
                    setRequestProperty("x-goog-api-key", apiKey)
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    val code = conn.responseCode
                    val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                    val respText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    if (code !in 200..299) {
                        throw IllegalStateException(friendlyHttpError(code, respText, ""))
                    }
                    val json = JSONObject(respText)
                    val arr = json.optJSONArray("models") ?: JSONArray()
                    for (i in 0 until arr.length()) {
                        val m = arr.optJSONObject(i) ?: continue
                        val methods = m.optJSONArray("supportedGenerationMethods") ?: continue
                        var supportsGenerate = false
                        for (j in 0 until methods.length()) {
                            if (methods.optString(j) == "generateContent") {
                                supportsGenerate = true
                                break
                            }
                        }
                        if (!supportsGenerate) continue
                        val id = m.optString("name", "").removePrefix("models/").trim()
                        if (id.isBlank()) continue
                        val lower = id.lowercase()
                        if (GeminiApiConfig.isExcludedModelId(id)) {
                            continue
                        }
                        if (lower.startsWith("gemini-") || lower.startsWith("gemma-")) {
                            collected[id] = DiscoveredGeminiModel(
                                id = id,
                                displayName = m.optString("displayName", "").trim(),
                                description = m.optString("description", "").trim()
                            )
                        }
                    }
                    pageToken = json.optString("nextPageToken", "").takeIf { it.isNotBlank() }
                    if (pageToken == null) break
                } catch (e: IllegalStateException) {
                    throw e
                } catch (e: Exception) {
                    throw buildNetworkException(e)
                } finally {
                    conn.disconnect()
                }
            }

            return collected.values.sortedWith(
                compareByDescending<DiscoveredGeminiModel> { it.id.startsWith("gemini-", ignoreCase = true) }
                    .thenByDescending { parseModelMajorMinorScore(it.id) }
                    .thenBy { modelFamilySortRank(it.id) }
                    .thenBy { hasDatedSuffix(it.id) }
                    .thenBy { it.id }
            )
        }

        private fun modelFamilySortRank(id: String): Int {
            val lower = id.lowercase()
            return when {
                lower.contains("flash-lite") -> 0
                lower.contains("flash") -> 1
                lower.contains("pro") -> 3
                else -> 2
            }
        }

        private fun parseModelMajorMinorScore(id: String): Double {
            val match = Regex("(?:gemini|gemma)-(\\d+)(?:\\.(\\d+))?").find(id.lowercase())
                ?: return 0.0
            val major = match.groupValues.getOrNull(1)?.toIntOrNull() ?: 0
            val minor = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
            return major * 100.0 + minor
        }

        private fun hasDatedSuffix(id: String): Boolean {
            return Regex("-(?:\\d{3}|\\d{2}-\\d{2}|\\d{4}|preview-\\d+)").containsMatchIn(id.lowercase())
        }

        private fun parseRetryAfterSeconds(headerRetry: Int?, respText: String): Int? {
            if (headerRetry != null && headerRetry > 0) return headerRetry.coerceIn(5, 180)
            val retryMatch = Regex("retry\\s*(?:in|after|delay\"?\\s*[:=]\\s*\"?)\\s*(\\d+(?:\\.\\d+)?)s?", RegexOption.IGNORE_CASE)
                .find(respText)
            val parsed = retryMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull()?.let { ceil(it).toInt() }
            return parsed?.coerceIn(5, 180)
        }

        private fun formatRateLimitMessage(modelId: String, retryAfterSec: Int?, isZeroQuota: Boolean = false): String {
            if (isZeroQuota) {
                return "এই মডেলে ($modelId) ফ্রি কোটা নেই বা দৈনিক সীমা পূর্ণ — সেটিংস থেকে Flash বা Flash-Lite মডেল নির্বাচন করুন।"
            }
            val waitText = retryAfterSec?.let { "${bn(it.toString())} সেকেন্ড পর আবার চেষ্টা করুন" }
                ?: "৩০ সেকেন্ড পর আবার চেষ্টা করুন"
            return "ফ্রি-টিয়ার রেট-লিমিট পূর্ণ ($modelId) — $waitText অথবা সেটিংস থেকে Flash-Lite/অন্য মডেল বেছে নিন।"
        }

        private fun friendlyHttpError(
            code: Int,
            respText: String,
            modelId: String,
            retryAfterSec: Int? = null
        ): String {
            val apiMsg = runCatching {
                JSONObject(respText).optJSONObject("error")?.optString("message").orEmpty()
            }.getOrDefault("")
            val lower = (apiMsg + " " + respText).lowercase()
            return when {
                lower.contains("api_key_invalid") || lower.contains("api key not valid") || code == 401 ->
                    "আপনার Gemini API Key সঠিক নয়। Google AI Studio থেকে বৈধ কী কপি করে আবার চেষ্টা করুন।"
                code == 403 || lower.contains("permission_denied") ->
                    "এই Gemini API Key-এর অনুমতি নেই বা প্রজেক্টে Generative Language API চালু করা নেই।"
                code == 404 || lower.contains("not found") || lower.contains("not supported for generatecontent") ->
                    "নির্বাচিত মডেল ($modelId) এই API Key-এর জন্য উপলব্ধ নয়। সেটিংস থেকে প্রস্তাবিত বা লোড করা অন্য একটি মডেল বেছে নিন।"
                code == 429 || lower.contains("quota") || lower.contains("resource_exhausted") ->
                    formatRateLimitMessage(modelId, retryAfterSec, isZeroQuota = isZeroQuotaError(respText))
                code == 503 || lower.contains("high demand") || lower.contains("overloaded") || lower.contains("unavailable") ->
                    "নির্বাচিত মডেলটিতে ($modelId) এই মুহূর্তে অতিরিক্ত চাপ (HTTP 503) চলছে। কিছুক্ষণ পর আবার চেষ্টা করুন অথবা সেটিংস থেকে অন্য একটি মডেল নির্বাচন করুন।"
                code in listOf(500, 502, 504) ->
                    "Gemini সার্ভারে সাময়িক সমস্যা হচ্ছে (HTTP $code)। কিছুক্ষণ পর আবার চেষ্টা করুন বা অন্য মডেল বেছে নিন।"
                else ->
                    "Gemini API ত্রুটি (HTTP $code): ${apiMsg.ifBlank { "অনুরোধ সম্পন্ন হয়নি" }}"
            }
        }

        private fun buildNetworkException(e: Exception): GeminiNetworkException {
            val msg = (e.message ?: "").lowercase()
            return when {
                e is UnknownHostException || msg.contains("unable to resolve host") ->
                    GeminiNetworkException(
                        isTimeout = false,
                        isConnectionError = true,
                        userMessage = "ইন্টারনেট সংযোগ পাওয়া যায়নি (সার্ভার খুঁজে পাওয়া যায়নি)। Wi-Fi বা মোবাইল ডেটা চালু আছে কিনা যাচাই করুন।",
                        cause = e
                    )
                e is SocketTimeoutException || msg.contains("timed out") || msg.contains("timeout") -> {
                    val isConnectTimeout = msg.contains("connect")
                    GeminiNetworkException(
                        isTimeout = true,
                        isConnectionError = isConnectTimeout,
                        userMessage = if (isConnectTimeout) {
                            "Gemini সার্ভারে সংযোগের সময় শেষ হয়েছে। নেটওয়ার্ক স্পিড যাচাই করে আবার চেষ্টা করুন।"
                        } else {
                            "AI মডেল থেকে উত্তর আসতে অতিরিক্ত সময় লেগেছে। ছবিতে শুধু টেবিল অংশ ক্রপ করুন এবং 'ডকুমেন্ট স্পষ্ট' মোড চালু রাখুন (অথবা বড় পাতা ২ ভাগে স্ক্যান করুন)।"
                        },
                        cause = e
                    )
                }
                e is ConnectException || e is NoRouteToHostException || msg.contains("failed to connect") ->
                    GeminiNetworkException(
                        isTimeout = false,
                        isConnectionError = true,
                        userMessage = "Gemini সার্ভারে সংযোগ করা যায়নি। ইন্টারনেট বা VPN সংযোগ পরীক্ষা করে আবার চেষ্টা করুন।",
                        cause = e
                    )
                e is SSLException ->
                    GeminiNetworkException(
                        isTimeout = false,
                        isConnectionError = true,
                        userMessage = "নিরাপদ HTTPS সংযোগ ব্যর্থ হয়েছে। ফোনের তারিখ ও সময় ঠিক আছে কিনা দেখুন এবং আবার চেষ্টা করুন।",
                        cause = e
                    )
                e is SocketException ->
                    GeminiNetworkException(
                        isTimeout = false,
                        isConnectionError = true,
                        userMessage = "নেটওয়ার্ক সংযোগ মাঝপথে বিচ্ছিন্ন হয়েছে। অনুগ্রহ করে আবার চেষ্টা করুন।",
                        cause = e
                    )
                else ->
                    GeminiNetworkException(
                        isTimeout = false,
                        isConnectionError = false,
                        userMessage = "AI সংযোগ ব্যর্থ হয়েছে: ${e.localizedMessage ?: e.javaClass.simpleName}",
                        cause = e
                    )
            }
        }
    }
}

/**
 * অফলাইন বা প্রিভিউ পরীক্ষার জন্য নমুনা ডেটা (ম্যানুয়াল সংশোধন ফ্লো প্রদর্শনের জন্য)।
 */
class PlaceholderAiRepository : AiRepository {
    override val isPlaceholderBackend: Boolean = true

    override suspend fun chat(
        history: List<AiChatMessage>,
        latestUserMessage: String,
        modelMode: AiModelMode,
        studentContext: String?
    ): AiChatReply {
        delay(350)
        return AiChatReply(
            text = "নমুনা উত্তর (অফলাইন মোড): \"${latestUserMessage.take(60)}\"",
            modelLabel = "Sample Mode",
            isPlaceholder = true
        )
    }

    override suspend fun analyzeMarksheetImage(
        bitmap: Bitmap,
        teacherNote: String,
        documentEnhance: Boolean
    ): ExtractedMarksheet {
        delay(450)
        val t = 1
        return ExtractedMarksheet(
            summary = "নমুনা একক নম্বরপত্র প্রিভিউ (${bitmap.width}×${bitmap.height}px)",
            student = ExtractedStudentInfo(name = "রাফিউজ্জামান রাফি (নমুনা)", cls = 3, roll = 10, religion = "ইসলাম"),
            term = t,
            marks = listOf(
                ExtractedMarkItem("MATH_1_TB", 9.0, "গণিত (পাঠ্যবই)"),
                ExtractedMarkItem("MATH_1_WR", 61.0, "গণিত (সামষ্টিক)")
            ),
            attendanceAndTraits = ExtractedAttendanceAndTraits(days = "64", present = "60", act = "ক", disc = "ক", cond = "ক"),
            uncertainNotes = listOf("নমুনা মোড: বাস্তব স্ক্যানের জন্য সেটিংসে Gemini API Key যুক্ত করুন"),
            isPlaceholder = true
        )
    }

    override suspend fun analyzeClassSheetImage(
        bitmap: Bitmap,
        clsHint: Int?,
        termHint: Int?,
        subKeyHint: String?,
        teacherNote: String,
        importScope: ClassSheetImportScope,
        documentEnhance: Boolean,
        sheetKindHint: ClassSheetKind?,
        qrMeta: BlankSheetQrMeta?
    ): ExtractedClassSheet {
        delay(500)
        val cls = (clsHint ?: qrMeta?.cls ?: 3).coerceIn(1, 20)
        val term = (termHint ?: qrMeta?.term?.takeIf { it in 1..3 } ?: 1).coerceIn(1, 3)
        val sub = subKeyHint ?: qrMeta?.subjectKey ?: "MATH"
        val resolvedKind = sheetKindHint ?: qrMeta?.sheetKind ?: ClassSheetKind.SINGLE_TERM_MARKS
        if (resolvedKind == ClassSheetKind.THREE_TERM_MARKS) {
            val rows3T = listOf(
                ScannedStudentRow(
                    rollText = "1",
                    name = "আরিয়ান ইসলাম খালিদ",
                    marksText = mapOf("T1_CONT" to "25", "T1_WR" to "58", "T2_CONT" to "26", "T2_WR" to "60", "T3_CONT" to "27", "T3_WR" to "62")
                ),
                ScannedStudentRow(
                    rollText = "2",
                    name = "খাদিজা তুল কুবরা",
                    marksText = mapOf("T1_CONT" to "24", "T1_WR" to "50", "T1_TOTAL" to "74", "T2_CONT" to "26", "T2_WR" to "55"),
                    aiIssues = mapOf("T2_WR" to "হাতের লেখা কিছুটা অস্পষ্ট")
                )
            ).map { ScanConstraintSolver.reconcileInterdependencies(it, cls, sub, ClassSheetImportScope.BOTH, ClassSheetKind.THREE_TERM_MARKS) }
            return ExtractedClassSheet(
                cls = cls,
                term = term,
                subKey = sub,
                summary = "[নমুনা] ৩ প্রান্তিক নম্বর শিট প্রিভিউ।",
                warnings = listOf("এটি একটি নমুনা স্ক্যান — প্রকৃত ছবির জন্য 'স্ক্যান করুন' ব্যবহার করুন।"),
                rows = rows3T,
                isSample = true,
                scope = ClassSheetImportScope.BOTH,
                sheetKind = ClassSheetKind.THREE_TERM_MARKS
            )
        }
        if (resolvedKind == ClassSheetKind.ATTENDANCE_TRAITS) {
            val rowsAtt = listOf(
                ScannedStudentRow(
                    rollText = "1",
                    name = "আরিয়ান ইসলাম খালিদ",
                    marksText = emptyMap(),
                    traitValues = mapOf(
                        "PRES_1" to "59", "ACT_1" to "ক", "DISC_1" to "ক", "COND_1" to "ক",
                        "PRES_2" to "62", "ACT_2" to "ক", "DISC_2" to "ক", "COND_2" to "খ",
                        "REM_1" to "নিয়মিত ও মনোযোগী"
                    )
                ),
                ScannedStudentRow(
                    rollText = "2",
                    name = "খাদিজা তুল কুবরা",
                    marksText = emptyMap(),
                    traitValues = mapOf(
                        "PRES_1" to "55", "ACT_1" to "খ", "DISC_1" to "ক", "COND_1" to "ক",
                        "PRES_2" to "60", "ACT_2" to "ক", "DISC_2" to "ক", "COND_2" to "ক"
                    ),
                    aiIssues = mapOf("PRES_1" to "উপস্থিতি সংখ্যা যাচাই করুন")
                )
            )
            return ExtractedClassSheet(
                cls = cls,
                term = term,
                subKey = sub,
                summary = "[নমুনা] উপস্থিতি ও গুণাবলি শিট প্রিভিউ।",
                warnings = listOf("এটি একটি নমুনা স্ক্যান — প্রকৃত ছবির জন্য 'স্ক্যান করুন' ব্যবহার করুন।"),
                rows = rowsAtt,
                isSample = true,
                scope = ClassSheetImportScope.BOTH,
                sheetKind = ClassSheetKind.ATTENDANCE_TRAITS,
                sharedWorkingDaysByTerm = mapOf(1 to "62", 2 to "65")
            )
        }

        val hasWritten = (subjectsFor(cls, RELIGIONS.first()).find { it.key == sub }?.written ?: 0) > 0
        val resolvedScope = when (importScope) {
            ClassSheetImportScope.CONT_ONLY -> ClassSheetImportScope.CONT_ONLY
            ClassSheetImportScope.WR_ONLY -> if (hasWritten) ClassSheetImportScope.WR_ONLY else ClassSheetImportScope.BOTH
            else -> ClassSheetImportScope.BOTH
        }
        val sampleRows = when (resolvedScope) {
            ClassSheetImportScope.CONT_ONLY -> listOf(
                ScannedStudentRow(rollText = "1", name = "", marksText = emptyMap(), sheetContinuousTotal = "26"),
                ScannedStudentRow(rollText = "2", name = "", marksText = emptyMap(), sheetContinuousTotal = "24", aiIssues = mapOf("CONT" to "লেখা কিছুটা অস্পষ্ট")),
                ScannedStudentRow(rollText = "3", name = "", marksText = emptyMap(), sheetContinuousTotal = "28")
            )
            ClassSheetImportScope.WR_ONLY -> listOf(
                ScannedStudentRow(rollText = "1", name = "", marksText = mapOf("WR" to "58")),
                ScannedStudentRow(rollText = "2", name = "", marksText = mapOf("WR" to "46"), aiIssues = mapOf("WR" to "সামষ্টিক নম্বর কিছুটা অস্পষ্ট")),
                ScannedStudentRow(rollText = "3", name = "", marksText = mapOf("WR" to "62"))
            )
            else -> listOf(
                ScannedStudentRow(
                    rollText = "1",
                    name = "আরিয়ান ইসলাম খালিদ",
                    marksText = mapOf("WR" to "58"),
                    sheetContinuousTotal = "26",
                    rawNotes = "রোল + ধারাবাহিক + সামষ্টিক"
                ),
                ScannedStudentRow(
                    rollText = "2",
                    name = "",
                    marksText = mapOf("WR" to "46"),
                    sheetContinuousTotal = "24",
                    aiIssues = mapOf("WR" to "সামষ্টিক নম্বর কিছুটা অস্পষ্ট")
                ),
                ScannedStudentRow(
                    rollText = "3",
                    name = "খাদিজা তুল কুবরা",
                    marksText = mapOf("TB" to "7", "ACT" to "5", "LANG" to "5", "CT" to "5", "WR" to "34"),
                    sheetContinuousTotal = "25",
                    sheetGrandTotal = "56"
                )
            )
        }
        return ExtractedClassSheet(
            cls = cls,
            term = term,
            subKey = sub,
            summary = when (resolvedScope) {
                ClassSheetImportScope.CONT_ONLY -> "[নমুনা] শুধু ধারাবাহিক মোড (বিদ্যমান সামষ্টিক অক্ষত থাকবে)।"
                ClassSheetImportScope.WR_ONLY -> "[নমুনা] শুধু সামষ্টিক মোড (বিদ্যমান ধারাবাহিক অক্ষত থাকবে)।"
                else -> "[নমুনা] পূর্ণাঙ্গ ও সংক্ষিপ্ত উভয় ফরম্যাটের নমুনা প্রিভিউ।"
            },
            warnings = listOf("এটি একটি নমুনা স্ক্যান — প্রকৃত ছবির জন্য 'স্ক্যান ও প্রিভিউ' ব্যবহার করুন।"),
            rows = sampleRows.map { ScanConstraintSolver.reconcileInterdependencies(it, cls, sub, resolvedScope, ClassSheetKind.SINGLE_TERM_MARKS) },
            isSample = true,
            scope = resolvedScope,
            sheetKind = ClassSheetKind.SINGLE_TERM_MARKS
        )
    }

    override suspend fun analyzeAutoSheetImage(
        bitmap: Bitmap,
        clsHint: Int?,
        termHint: Int?,
        subKeyHint: String?,
        teacherNote: String,
        importScope: ClassSheetImportScope,
        documentEnhance: Boolean,
        sheetKindHint: ClassSheetKind?,
        qrMeta: BlankSheetQrMeta?
    ): AutoScanResult = AutoScanResult.ClassSheet(
        analyzeClassSheetImage(
            bitmap = bitmap,
            clsHint = clsHint,
            termHint = termHint,
            subKeyHint = subKeyHint,
            teacherNote = teacherNote,
            importScope = importScope,
            documentEnhance = documentEnhance,
            sheetKindHint = sheetKindHint,
            qrMeta = qrMeta
        )
    )

    override suspend fun draftRemark(studentSummary: String, term: Int): AiDraftRemarkResult {
        delay(350)
        return AiDraftRemarkResult(
            remark = "${termNameBn(term)}ের মূল্যায়নে শিক্ষার্থীর অংশগ্রহণ ভালো।",
            isPlaceholder = true
        )
    }
}
