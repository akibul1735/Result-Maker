package com.kgps.result.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Base64
import com.kgps.result.RELIGIONS
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
    const val DEFAULT_MODEL = "gemini-3.8-flash"
    const val FAST_FALLBACK_MODEL = "gemini-3.5-flash-lite"

    private const val PREF_NAME = "gemini_api_prefs"
    private const val KEY_API_KEY = "gemini_api_key"
    private const val KEY_MODEL = "gemini_model"
    private const val KEY_CACHED_MODELS = "gemini_cached_models_json"
    private const val KEY_CUSTOM_MODELS = "gemini_custom_models_json"

    val SUGGESTED_MODELS: List<GeminiModelSuggestion> = listOf(
        GeminiModelSuggestion(
            id = "gemini-3.8-flash",
            title = "Gemini 3.8 Flash",
            badge = "প্রস্তাবিত (ডিফল্ট)",
            description = "নম্বরপত্র ছবি OCR, বাংলা চ্যাট ও দ্রুত কাজের জন্য সবচেয়ে ভারসাম্যপূর্ণ"
        ),
        GeminiModelSuggestion(
            id = "gemini-3.5-flash-lite",
            title = "Gemini 3.5 Flash-Lite",
            badge = "অতি দ্রুত",
            description = "কম টোকেন খরচে দ্রুত চ্যাট, মন্তব্য খসড়া ও হালকা স্ক্যানের জন্য"
        ),
        GeminiModelSuggestion(
            id = "gemini-4-argon",
            title = "Gemini 4 Argon",
            badge = "উন্নত বিশ্লেষণ",
            description = "জটিল বা অস্পষ্ট হাতের লেখার নম্বরপত্র নিখুঁতভাবে বিশ্লেষণের জন্য"
        ),
        GeminiModelSuggestion(
            id = "gemini-4-hydrogen",
            title = "Gemini 4 Hydrogen",
            badge = "সর্বশেষ",
            description = "সর্বশেষ প্রজন্মের উচ্চক্ষমতাসম্পন্ন মাল্টিমোডাল মডেল"
        )
    )

    val SUGGESTED_MODEL_IDS: Set<String> = SUGGESTED_MODELS.map { it.id }.toSet()

    fun normalizeApiKey(raw: String): String = raw.trim()

    fun normalizeModelId(raw: String): String {
        val cleaned = raw.trim()
            .removePrefix("models/")
            .trim()
        return cleaned.ifBlank { DEFAULT_MODEL }
    }

    fun getApiKey(context: Context): String {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return normalizeApiKey(sp.getString(KEY_API_KEY, "").orEmpty())
    }

    fun getSelectedModel(context: Context): String {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return normalizeModelId(sp.getString(KEY_MODEL, DEFAULT_MODEL).orEmpty())
    }

    fun isConfigured(context: Context): Boolean = getApiKey(context).isNotBlank()

    fun getCachedModels(context: Context): List<DiscoveredGeminiModel> {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY_CACHED_MODELS, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = normalizeModelId(obj.optString("id", ""))
                    if (id.isNotBlank()) {
                        add(
                            DiscoveredGeminiModel(
                                id = id,
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
        val raw = sp.getString(KEY_CUSTOM_MODELS, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val id = arr.optString(i, "").trim().removePrefix("models/").trim()
                    if (id.isNotBlank() && id !in SUGGESTED_MODEL_IDS) add(id)
                }
            }.distinct()
        }.getOrDefault(emptyList())
    }

    fun rememberCustomModel(context: Context, rawModel: String): List<String> {
        val cleaned = rawModel.trim().removePrefix("models/").trim()
        val existing = getCustomModels(context)
        if (cleaned.isBlank() || cleaned in SUGGESTED_MODEL_IDS) return existing
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

    fun saveConfig(context: Context, rawApiKey: String, rawModel: String) {
        val key = normalizeApiKey(rawApiKey)
        val model = normalizeModelId(rawModel)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_API_KEY, key)
            .putString(KEY_MODEL, model)
            .apply()
        rememberCustomModel(context, model)
    }

    fun clearApiKey(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_API_KEY)
            .remove(KEY_CACHED_MODELS)
            .apply()
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

data class ExtractedClassSheet(
    val cls: Int,
    val term: Int,
    val subKey: String,
    val summary: String,
    val warnings: List<String> = emptyList(),
    val rows: List<ScannedStudentRow>,
    val isSample: Boolean = false,
    val scope: ClassSheetImportScope = ClassSheetImportScope.BOTH
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
    val isSummaryOnlyMode: Boolean = false
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
    scope: ClassSheetImportScope = ClassSheetImportScope.BOTH
): RowValidationReport = ScanConstraintSolver.validateRow(row, cls, subKey, duplicateRolls, hasExistingDbName, scope)

data class AiChatReply(
    val text: String,
    val modelLabel: String,
    val isPlaceholder: Boolean = false
)

data class AiDraftRemarkResult(
    val remark: String,
    val isPlaceholder: Boolean = false
)

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
        documentEnhance: Boolean = true
    ): ExtractedClassSheet

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

internal fun extractBalancedJsonObject(raw: String): String {
    val cleaned = stripJsonFences(raw)
    val start = cleaned.indexOf('{')
    if (start < 0) return cleaned
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
            if (c == '{') depth++
            else if (c == '}') {
                depth--
                if (depth == 0) {
                    return cleaned.substring(start, i + 1)
                }
            }
        }
    }
    return cleaned.substring(start)
}

internal fun parseClassSheetJsonRobust(
    raw: String,
    clsHint: Int?,
    termHint: Int?,
    subKeyHint: String?
): JSONObject {
    val slice = extractBalancedJsonObject(raw)
    try {
        return JSONObject(slice)
    } catch (primaryErr: Exception) {
        // If output was truncated mid-array (e.g. MAX_TOKENS on large class), salvage all complete row objects
        val rowsMatch = Regex("\"rows\"\\s*:\\s*\\[").find(slice) ?: throw primaryErr
        val rowsStart = rowsMatch.range.last + 1
        val salvagedRows = JSONArray()
        var i = rowsStart
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
        if (salvagedRows.length() == 0) throw primaryErr

        val headerPrefix = slice.substring(0, rowsMatch.range.first)
        val clsVal = clsHint ?: Regex("\"cls\"\\s*:\\s*(\\d)").find(headerPrefix)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 3
        val termVal = termHint ?: Regex("\"term\"\\s*:\\s*(\\d)").find(headerPrefix)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
        val subVal = subKeyHint ?: Regex("\"subKey\"\\s*:\\s*\"([A-Za-z0-9_]+)\"").find(headerPrefix)?.groupValues?.getOrNull(1) ?: "MATH"

        return JSONObject().apply {
            put("cls", clsVal)
            put("term", termVal)
            put("subKey", subVal)
            put("summary", "")
            put("warnings", JSONArray().apply {
                put("মডেলের উত্তর দীর্ঘ হওয়ায় শেষ দিকের কিছু সারি অসম্পূর্ণ থাকতে পারে — প্রাপ্ত ${bn(salvagedRows.length().toString())}টি সারি দেখানো হয়েছে।")
            })
            put("rows", salvagedRows)
        }
    }
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
            cls = sObj.optInt("cls", 0).takeIf { it in 1..5 },
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

internal fun parseExtractedClassSheetJson(
    json: JSONObject,
    clsHint: Int?,
    termHint: Int?,
    subKeyHint: String?,
    requestedScope: ClassSheetImportScope = ClassSheetImportScope.AUTO
): ExtractedClassSheet {
    val detectedCls = (clsHint ?: json.optInt("cls", 3)).coerceIn(1, 20)
    val detectedTerm = (termHint ?: json.optInt("term", 1)).coerceIn(1, 3)
    val rawSub = json.optString("subKey").trim().uppercase()
    val validSubs = subjectsFor(detectedCls, RELIGIONS.first()).map { it.key }.toSet()
    val detectedSub = when {
        !subKeyHint.isNullOrBlank() && subKeyHint in validSubs -> subKeyHint
        rawSub in validSubs -> rawSub
        else -> validSubs.first()
    }
    val activeSub = subjectsFor(detectedCls, RELIGIONS.first()).find { it.key == detectedSub }
    val hasWritten = (activeSub?.written ?: 0) > 0

    val warningsArr = json.optJSONArray("warnings") ?: JSONArray()
    val warnings = (0 until warningsArr.length()).mapNotNull { i ->
        warningsArr.optString(i)?.trim()?.takeIf { it.isNotBlank() }
    }.toMutableList()

    val metaFound = json.optJSONObject("metaFound")
    val missingMeta = mutableListOf<String>()
    if (clsHint == null && metaFound != null && !metaFound.optBoolean("cls", true)) missingMeta.add("শ্রেণি")
    if (termHint == null && metaFound != null && !metaFound.optBoolean("term", true)) missingMeta.add("প্রান্তিক")
    if (subKeyHint.isNullOrBlank() && (metaFound != null && !metaFound.optBoolean("sub", true) || rawSub !in validSubs)) {
        missingMeta.add("বিষয়")
    }
    if (missingMeta.isNotEmpty()) {
        warnings.add(
            "ছবির উপরে ${missingMeta.joinToString(", ")} স্পষ্ট লেখা না থাকায় নিচ থেকে সঠিকটি যাচাই করে নিন।"
        )
    }

    val rowsArr = json.optJSONArray("rows") ?: JSONArray()
    val rawParsed = (0 until rowsArr.length()).mapNotNull { idx ->
        val obj = rowsArr.optJSONObject(idx) ?: return@mapNotNull null
        val rollStr = fromBn(obj.optString("roll", "").trim())
        val nameStr = obj.optString("name", "").trim()
        val marksObj = obj.optJSONObject("marks") ?: JSONObject()
        val marksMap = mutableMapOf<String, String>()
        marksObj.keys().forEach { k ->
            val v = marksObj.optString(k, "").trim()
            if (v.isNotEmpty() && !v.equals("null", ignoreCase = true)) {
                marksMap[k.uppercase()] = fromBn(v)
            }
        }
        val uncObj = obj.optJSONObject("uncertainFields") ?: JSONObject()
        val aiIssues = mutableMapOf<String, String>()
        uncObj.keys().forEach { k ->
            val reason = uncObj.optString(k, "").trim()
            if (reason.isNotEmpty()) {
                val normKey = when (k.trim().lowercase()) {
                    "roll", "name" -> k.trim().lowercase()
                    "cont", "continuous", "sheetcontinuoustotal" -> "CONT"
                    "grand", "grandtotal", "sheetgrandtotal" -> "GRAND"
                    else -> k.trim().uppercase()
                }
                aiIssues[normKey] = reason
            }
        }
        val sheetCont = obj.optString("sheetContinuousTotal", "").trim()
            .takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
            ?.let { fromBn(it) }
        val sheetGrand = obj.optString("sheetGrandTotal", "").trim()
            .takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
            ?.let { fromBn(it) }

        ScannedStudentRow(
            id = UUID.randomUUID().toString(),
            selected = true,
            rollText = rollStr,
            name = nameStr,
            marksText = marksMap,
            sheetContinuousTotal = sheetCont,
            sheetGrandTotal = sheetGrand,
            rawNotes = obj.optString("rawNotes", "").trim(),
            aiIssues = aiIssues
        )
    }

    val compKeysSet = setOf("TB", "ACT", "LANG", "CT")
    val sheetHasAnyName = rawParsed.any { it.name.isNotBlank() }
    val sheetHasAnyBreakdown = rawParsed.any { r -> compKeysSet.any { !r.marksText[it].isNullOrBlank() } }
    val sheetHasAnyCont = sheetHasAnyBreakdown || rawParsed.any { !it.sheetContinuousTotal.isNullOrBlank() }
    val sheetHasAnyWr = rawParsed.any { !it.marksText["WR"].isNullOrBlank() }
    val sheetHasAnyGrand = rawParsed.any { !it.sheetGrandTotal.isNullOrBlank() }

    val resolvedScope = when (requestedScope) {
        ClassSheetImportScope.CONT_ONLY -> ClassSheetImportScope.CONT_ONLY
        ClassSheetImportScope.WR_ONLY -> if (hasWritten) ClassSheetImportScope.WR_ONLY else ClassSheetImportScope.BOTH
        ClassSheetImportScope.BOTH -> ClassSheetImportScope.BOTH
        ClassSheetImportScope.AUTO -> when {
            hasWritten && sheetHasAnyCont && !sheetHasAnyWr -> ClassSheetImportScope.CONT_ONLY
            hasWritten && sheetHasAnyWr && !sheetHasAnyCont -> ClassSheetImportScope.WR_ONLY
            else -> ClassSheetImportScope.BOTH
        }
    }

    val parsedRows = rawParsed.map { row ->
        val cleanedIssues = row.aiIssues.filterKeys { key ->
            when {
                key in compKeysSet && !sheetHasAnyBreakdown && row.marksText[key].isNullOrBlank() -> false
                key == "name" && !sheetHasAnyName && row.name.isBlank() -> false
                key == "GRAND" && !sheetHasAnyGrand && row.sheetGrandTotal.isNullOrBlank() -> false
                resolvedScope == ClassSheetImportScope.CONT_ONLY && (key == "WR" || key == "GRAND") -> false
                resolvedScope == ClassSheetImportScope.WR_ONLY && (key in compKeysSet || key == "CONT" || key == "GRAND") -> false
                else -> true
            }
        }
        ScanConstraintSolver.reconcileInterdependencies(
            row.copy(aiIssues = cleanedIssues),
            detectedCls,
            detectedSub,
            resolvedScope
        )
    }

    return ExtractedClassSheet(
        cls = detectedCls,
        term = detectedTerm,
        subKey = detectedSub,
        summary = json.optString("summary", "").ifBlank {
            val scopeNote = when (resolvedScope) {
                ClassSheetImportScope.CONT_ONLY -> " · শুধু ধারাবাহিক"
                ClassSheetImportScope.WR_ONLY -> " · শুধু সামষ্টিক"
                else -> ""
            }
            "${classNameBn(detectedCls)} শ্রেণি · ${termNameBn(detectedTerm)}$scopeNote — ${bn(parsedRows.size.toString())} জন শনাক্ত।"
        },
        warnings = warnings.distinct(),
        rows = parsedRows,
        isSample = false,
        scope = resolvedScope
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

    private fun resolveModelId(mode: AiModelMode? = null): String {
        val selected = GeminiApiConfig.getSelectedModel(appContext)
        return if (mode == AiModelMode.FAST && selected == GeminiApiConfig.DEFAULT_MODEL) {
            GeminiApiConfig.FAST_FALLBACK_MODEL
        } else {
            selected
        }
    }

    private val modelCooldownUntilMs = ConcurrentHashMap<String, Long>()
    private val cacheLock = Any()
    private val singleSheetCache = object : LinkedHashMap<String, ExtractedMarksheet>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ExtractedMarksheet>?): Boolean = size > 8
    }
    private val classSheetCache = object : LinkedHashMap<String, ExtractedClassSheet>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ExtractedClassSheet>?): Boolean = size > 8
    }
    private val remarkCache = object : LinkedHashMap<String, AiDraftRemarkResult>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, AiDraftRemarkResult>?): Boolean = size > 16
    }

    private fun remainingCooldownSeconds(modelId: String): Int {
        val clean = GeminiApiConfig.normalizeModelId(modelId)
        val until = modelCooldownUntilMs[clean] ?: return 0
        val diff = until - System.currentTimeMillis()
        if (diff <= 0L) {
            modelCooldownUntilMs.remove(clean)
            return 0
        }
        return ceil(diff / 1000.0).toInt().coerceAtLeast(1)
    }

    private fun recordRateLimitCooldown(modelId: String, retryAfterSec: Int?) {
        val clean = GeminiApiConfig.normalizeModelId(modelId)
        val waitSec = (retryAfterSec ?: DEFAULT_RATE_LIMIT_COOLDOWN_SEC).coerceIn(5, 180)
        modelCooldownUntilMs[clean] = System.currentTimeMillis() + waitSec * 1000L
    }

    private fun selectFallbackCandidates(
        primaryModel: String,
        cachedModels: List<DiscoveredGeminiModel>,
        maxCount: Int
    ): List<String> {
        val excludedTokens = listOf(
            "embedding", "aqa", "attribution", "tts", "image", "imagen",
            "veo", "robotics", "computer-use", "deep-research", "audio", "live"
        )
        // Prefer standard capable Flash models before Lite models to keep OCR accuracy high
        val discoveredFlashFirst = cachedModels
            .map { it.id }
            .filter { id ->
                id != primaryModel &&
                    id.startsWith("gemini-", ignoreCase = true) &&
                    excludedTokens.none { tok -> id.contains(tok, ignoreCase = true) }
            }
            .sortedWith(
                compareBy<String> { it.contains("lite", ignoreCase = true) }
                    .thenByDescending { it.contains("flash", ignoreCase = true) }
            )

        val builtinSafetyFallbacks = listOf(
            GeminiApiConfig.DEFAULT_MODEL,
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            GeminiApiConfig.FAST_FALLBACK_MODEL,
            "gemini-2.5-flash-lite"
        ).filter { it != primaryModel }

        return (discoveredFlashFirst + builtinSafetyFallbacks)
            .distinct()
            .filter { remainingCooldownSeconds(it) == 0 }
            .take(maxCount)
    }

    private suspend fun callGenerateContentResilient(
        apiKey: String,
        primaryModelId: String,
        body: JSONObject
    ): Pair<String, String> {
        if (!hasNetworkTransport(appContext)) {
            throw GeminiNetworkException(
                isTimeout = false,
                isConnectionError = true,
                userMessage = "মোবাইল ডেটা বা Wi-Fi চালু নেই। ইন্টারনেট সংযোগ চালু করে আবার চেষ্টা করুন।"
            )
        }

        val serializedBody = body.toString().toByteArray(Charsets.UTF_8)
        val isVisionOrHeavy = serializedBody.size > 80_000
        var lastError: Exception? = null
        val primaryCooldown = remainingCooldownSeconds(primaryModelId)

        // 1. Only hit primary model if not currently in 429 cooldown
        if (primaryCooldown == 0) {
            for (attempt in 0..1) {
                try {
                    val text = callGenerateContent(apiKey, primaryModelId, body, serializedBody)
                    modelCooldownUntilMs.remove(GeminiApiConfig.normalizeModelId(primaryModelId))
                    return text to primaryModelId
                } catch (e: GeminiHttpException) {
                    lastError = e
                    if (e.httpCode == 401 || e.httpCode == 403 || e.httpCode == 400) throw e
                    if (e.httpCode == 429) {
                        recordRateLimitCooldown(e.modelId, e.retryAfterSeconds)
                        break // Never burst-retry same model on 429
                    }
                    val isTransient = e.httpCode in setOf(500, 502, 503, 504)
                    if (isTransient && attempt == 0) {
                        delay(1_200L)
                        continue
                    }
                    break
                } catch (e: GeminiNetworkException) {
                    lastError = e
                    // For vision scans that already timed out, switch directly to a faster healthy model
                    if (e.isTimeout && isVisionOrHeavy) {
                        break
                    }
                    if (attempt == 0 && (e.isTimeout || e.isConnectionError)) {
                        delay(1_000L)
                        continue
                    }
                    break
                } catch (e: Exception) {
                    lastError = e
                    throw e
                }
            }
        } else {
            lastError = GeminiHttpException(
                httpCode = 429,
                modelId = primaryModelId,
                userMessage = formatRateLimitMessage(primaryModelId, primaryCooldown),
                retryAfterSeconds = primaryCooldown
            )
        }

        // 2. Bounded fallback: on HTTP 429 or timeout try at most 1 healthy lite/flash model;
        //    on HTTP 503/404/5xx try up to 2 healthy fallback models.
        val httpErr = lastError as? GeminiHttpException
        val netErr = lastError as? GeminiNetworkException
        val allowHttpFallback = httpErr != null && httpErr.httpCode in setOf(404, 429, 500, 502, 503, 504)
        val allowTimeoutFallback = netErr != null && netErr.isTimeout
        if (allowHttpFallback || allowTimeoutFallback) {
            val cached = GeminiApiConfig.getCachedModels(appContext)
            val maxFallbacks = if (httpErr?.httpCode == 429 || allowTimeoutFallback) 1 else 2
            val fallbacks = selectFallbackCandidates(primaryModelId, cached, maxFallbacks)
            for (fallbackModel in fallbacks) {
                try {
                    val text = callGenerateContent(apiKey, fallbackModel, body, serializedBody)
                    modelCooldownUntilMs.remove(GeminiApiConfig.normalizeModelId(fallbackModel))
                    return text to fallbackModel
                } catch (e: GeminiHttpException) {
                    if (e.httpCode == 401 || e.httpCode == 403) throw e
                    if (e.httpCode == 429) {
                        recordRateLimitCooldown(e.modelId, e.retryAfterSeconds)
                        if (httpErr?.httpCode == 429) {
                            lastError = e
                            break
                        }
                    }
                } catch (e: GeminiNetworkException) {
                    lastError = e
                    break
                } catch (_: Exception) {
                    // Preserve primary lastError
                }
            }
        }

        throw (lastError ?: IllegalStateException("Gemini API সংযোগ ব্যর্থ হয়েছে।"))
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
            val parsed = parseExtractedMarksheetJson(JSONObject(extractBalancedJsonObject(raw)))
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
        documentEnhance: Boolean
    ): ExtractedClassSheet {
        val apiKey = requireApiKey()
        val modelId = resolveModelId(null)
        val encoded = withContext(Dispatchers.Default) {
            VisionImageOptimizer.prepareForVisionUpload(bitmap, documentEnhance)
        }
        val cleanNote = teacherNote.trim()
        val cacheKey = "$modelId|${encoded.sha256Hex}|$documentEnhance|$clsHint|$termHint|$subKeyHint|${importScope.name}|$cleanNote"
        synchronized(cacheLock) {
            classSheetCache[cacheKey]?.let { return it }
        }

        val targetCls = clsHint ?: 3
        val allSubs = subjectsFor(targetCls, RELIGIONS.first())
        val scopedSubs = if (!subKeyHint.isNullOrBlank()) {
            allSubs.filter { it.key == subKeyHint }.ifEmpty { allSubs }
        } else {
            allSubs
        }
        val subsInfo = scopedSubs.joinToString("; ") { s ->
            val compDesc = s.comps.joinToString(",") { "${it.key}=${it.max}" }
            val wrDesc = if (s.written > 0) ",WR=${s.written}" else ""
            "${s.key}(${s.bn}):$compDesc$wrDesc"
        }

        val scopeRule = when (importScope) {
            ClassSheetImportScope.CONT_ONLY ->
                "4. এই ছবিতে কেবল রোল (ও থাকলে নাম) এবং ধারাবাহিক নম্বর (`sheetContinuousTotal` অথবা থাকলে `TB`/`ACT`/`LANG`/`CT`) আছে — সামষ্টিক (`WR`) ও `sheetGrandTotal` ফাঁকা রাখুন।"
            ClassSheetImportScope.WR_ONLY ->
                "4. এই ছবিতে কেবল রোল (ও থাকলে নাম) এবং সামষ্টিক/লিখিত নম্বর (`WR`) আছে — `TB`/`ACT`/`LANG`/`CT`, `sheetContinuousTotal` ও `sheetGrandTotal` ফাঁকা রাখুন।"
            ClassSheetImportScope.BOTH ->
                "4. ছবিতে থাকা ধারাবাহিক (`TB`,`ACT`,`LANG`,`CT` অথবা `sheetContinuousTotal`), সামষ্টিক (`WR`) ও থাকলে মোট (`sheetGrandTotal`) পড়ুন।"
            ClassSheetImportScope.AUTO ->
                """
                4. ছবির কলাম অনুযায়ী যেকোনো ফরম্যাট বুঝুন:
                   (ক) পূর্ণাঙ্গ: নাম + TB,ACT,LANG,CT + ধারাবাহিক মোট + সামষ্টিক + মোট
                   (খ) সংক্ষিপ্ত উভয়: ধারাবাহিক মোট + সামষ্টিক (+ থাকলে মোট)
                   (গ) শুধু ধারাবাহিক: কেবল ধারাবাহিক মোট (বা TB,ACT,LANG,CT)
                   (ঘ) শুধু সামষ্টিক: কেবল সামষ্টিক (`WR`)
                """.trimIndent()
        }

        val prompt = """
            বাংলাদেশ প্রাথমিক শ্রেণি-নম্বরপত্র OCR। সব সারির রোল ও থাকা নম্বর পড়ে শুধু JSON দিন।
            ইঙ্গিত: শ্রেণি=${clsHint ?: "ছবির_উপরে_থেকে_শনাক্ত(1-5)"}, প্রান্তিক=${termHint ?: "ছবির_উপরে_থেকে_শনাক্ত(1-3)"}, বিষয়=${subKeyHint ?: "ছবির_উপরে_থেকে_শনাক্ত"}, নোট=${cleanNote.ifBlank { "নেই" }}
            কাঠামো: $subsInfo
            নিয়ম:
            1. সব সংখ্যা ইংরেজি অঙ্কে (০৭->"7"); নাম থাকলে বাংলায়, না থাকলে ""।
            2. CT ঘরে "০৬+০৭=১৩/২=০৭" থাকলে "=" পরের গড় মানটি ("7") `CT`-তে এবং `rawNotes`-এ সংক্ষেপে লিখুন।
            3. `A`/`অনু` থাকলে "0" ও `rawNotes`-এ "অনুপস্থিত (A)"; ফাঁকা ঘর ""।
            $scopeRule
            5. যে কলামগুলো ছবিতে নেই (`name`, `TB`, `ACT`, `LANG`, `CT`, `sheetContinuousTotal`, `WR`, `sheetGrandTotal`) সেগুলো অনুমান করে হিসাব করবেন না — ফাঁকা রাখুন এবং JSON ছোট রাখতে অনুপস্থিত বা ফাঁকা কীগুলো বাদ দিন।
            6. ছবিতে আসলে কোনো সংখ্যা/লেখা অস্পষ্ট হলে তবেই `uncertainFields`-এ কারণ দিন। `metaFound`-এ ছবির উপরে শ্রেণি/প্রান্তিক/বিষয় লেখা পেলে true দিন, না পেলে false।
            JSON: {"cls":${clsHint ?: 3},"term":${termHint ?: 1},"subKey":"${subKeyHint ?: "MATH"}","metaFound":{"cls":${clsHint != null},"term":${termHint != null},"sub":${!subKeyHint.isNullOrBlank()}},"summary":"","warnings":[],"rows":[{"roll":"1","name":"","marks":{"TB":"","ACT":"","LANG":"","CT":"","WR":""},"sheetContinuousTotal":"","sheetGrandTotal":"","rawNotes":"","uncertainFields":{}}]}
        """.trimIndent()

        val payload = buildVisionJsonPayload(encoded.base64, prompt, maxOutputTokens = 8192)
        val (raw, usedModel) = callGenerateContentResilient(apiKey, modelId, payload)
        return try {
            val parsed = parseExtractedClassSheetJson(
                parseClassSheetJsonRobust(raw, clsHint, termHint, subKeyHint),
                clsHint,
                termHint,
                subKeyHint,
                importScope
            )
            val withFallbackWarning = if (usedModel != modelId) {
                parsed.copy(
                    warnings = parsed.warnings +
                        "নির্বাচিত মডেল ($modelId) ব্যস্ত থাকায় বিকল্প মডেল ($usedModel) দিয়ে স্ক্যান সম্পন্ন হয়েছে।"
                )
            } else parsed
            synchronized(cacheLock) { classSheetCache[cacheKey] = withFallbackWarning }
            withFallbackWarning
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException("ছবিটি থেকে শ্রেণি-নম্বরপত্র পড়া যায়নি ($usedModel)। আরও স্পষ্ট ও সোজা ছবি তুলে আবার চেষ্টা করুন।", e)
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
        private const val DEFAULT_RATE_LIMIT_COOLDOWN_SEC = 15

        internal class GeminiHttpException(
            val httpCode: Int,
            val modelId: String,
            userMessage: String,
            val retryAfterSeconds: Int? = null
        ) : IllegalStateException(userMessage)

        internal class GeminiNetworkException(
            val isTimeout: Boolean,
            val isConnectionError: Boolean,
            userMessage: String,
            cause: Throwable? = null
        ) : IllegalStateException(userMessage, cause)

        internal fun hasNetworkTransport(context: Context): Boolean {
            return runCatching {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    ?: return true
                val active = cm.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(active) ?: return false
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }.getOrDefault(true)
        }

        private fun thinkingStepsForModel(cleanModel: String, body: JSONObject): List<JSONObject?> {
            val genCfg = body.optJSONObject("generationConfig")
            val isJsonExtraction = genCfg?.optString("responseMimeType") == "application/json"
            if (!isJsonExtraction) return listOf(null)
            val lower = cleanModel.lowercase()
            return when {
                lower.contains("2.5") -> {
                    val budget = if (lower.contains("pro")) 128 else 0
                    listOf(JSONObject().put("thinkingBudget", budget), null)
                }
                Regex("gemini-(?:3|4)(?:\\.|-)").containsMatchIn(lower) -> {
                    if (lower.contains("flash")) {
                        listOf(
                            JSONObject().put("thinkingLevel", "MINIMAL"),
                            JSONObject().put("thinkingLevel", "LOW"),
                            null
                        )
                    } else {
                        listOf(
                            JSONObject().put("thinkingLevel", "LOW"),
                            null
                        )
                    }
                }
                else -> listOf(null)
            }
        }

        private fun cloneBodyWithThinkingConfig(baseBody: JSONObject, thinkingCfg: JSONObject?): JSONObject {
            if (thinkingCfg == null) return baseBody
            val oldGen = baseBody.optJSONObject("generationConfig") ?: JSONObject()
            val newGen = JSONObject(oldGen.toString()).apply {
                put("thinkingConfig", thinkingCfg)
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
            val cleanModel = GeminiApiConfig.normalizeModelId(modelId)
            val thinkingSteps = thinkingStepsForModel(cleanModel, body)
            for (idx in thinkingSteps.indices) {
                val thinkingCfg = thinkingSteps[idx]
                val effectiveBytes = if (thinkingCfg == null && preSerializedBody != null) {
                    preSerializedBody
                } else {
                    cloneBodyWithThinkingConfig(body, thinkingCfg).toString().toByteArray(Charsets.UTF_8)
                }
                try {
                    return@withContext executeSingleGenerateCall(apiKey, cleanModel, effectiveBytes)
                } catch (e: GeminiHttpException) {
                    val canStepDownThinking = e.httpCode == 400 &&
                        thinkingCfg != null &&
                        idx < thinkingSteps.lastIndex
                    if (canStepDownThinking) {
                        continue
                    }
                    throw e
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
                readTimeout = if (isVisionOrLarge) 80_000 else 55_000
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
                    throw GeminiHttpException(
                        httpCode = code,
                        modelId = cleanModel,
                        userMessage = friendlyHttpError(code, respText, cleanModel, retrySec),
                        retryAfterSeconds = retrySec
                    )
                }
                return extractCandidateText(JSONObject(respText))
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

        private fun extractCandidateText(json: JSONObject): String {
            val candidates = json.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""
            val first = candidates.optJSONObject(0) ?: return ""
            val finishReason = first.optString("finishReason", "").uppercase()
            val content = first.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = if (parts != null) {
                buildString {
                    for (i in 0 until parts.length()) {
                        val p = parts.optJSONObject(i) ?: continue
                        // Skip internal thought summary parts from reasoning models
                        if (p.optBoolean("thought", false)) continue
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
                    "SAFETY", "RECITATION", "PROHIBITED_CONTENT" -> throw IllegalStateException(
                        "সার্ভার ফিল্টারের কারণে ছবিটি প্রসেস করা যায়নি ($finishReason)। পরিষ্কার ছবি দিয়ে আবার চেষ্টা করুন।"
                    )
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
            val modelId = GeminiApiConfig.normalizeModelId(rawModel)
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

            // 2. Ping the selected model with a minimal generateContent call
            try {
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
                callGenerateContent(apiKey, modelId, pingPayload)
                GeminiConnectionCheckResult(
                    ok = true,
                    message = "সংযোগ সফল! সক্রিয় মডেল: $modelId · মোট ${bn(fetchedModels.size.toString())}টি মডেল পাওয়া গেছে।",
                    availableModels = fetchedModels
                )
            } catch (e: Exception) {
                val hint = if (fetchedModels.isNotEmpty()) {
                    "\nউপলব্ধ মডেল তালিকা থেকে অন্য একটি মডেল বেছে নিন অথবা কাস্টম মডেল আইডি যাচাই করুন।"
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
                        if (lower.contains("embedding") || lower.contains("aqa") || lower.contains("attribution")) {
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
                    .thenBy { hasDatedSuffix(it.id) }
                    .thenBy { it.id }
            )
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

        private fun formatRateLimitMessage(modelId: String, retryAfterSec: Int?): String {
            val waitText = retryAfterSec?.let { "${bn(it.toString())} সেকেন্ড পর আবার চেষ্টা করুন" }
                ?: "কিছুক্ষণ পর আবার চেষ্টা করুন"
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
                    formatRateLimitMessage(modelId, retryAfterSec)
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
        documentEnhance: Boolean
    ): ExtractedClassSheet {
        delay(500)
        val cls = (clsHint ?: 3).coerceIn(1, 20)
        val term = (termHint ?: 1).coerceIn(1, 3)
        val sub = subKeyHint ?: "MATH"
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
            rows = sampleRows.map { ScanConstraintSolver.reconcileInterdependencies(it, cls, sub, resolvedScope) },
            isSample = true,
            scope = resolvedScope
        )
    }

    override suspend fun draftRemark(studentSummary: String, term: Int): AiDraftRemarkResult {
        delay(350)
        return AiDraftRemarkResult(
            remark = "${termNameBn(term)}ের মূল্যায়নে শিক্ষার্থীর অংশগ্রহণ ভালো।",
            isPlaceholder = true
        )
    }
}
