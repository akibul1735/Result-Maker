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
    const val DEFAULT_MODEL = "gemini-2.5-flash"
    const val FAST_FALLBACK_MODEL = "gemini-2.5-flash-lite"
    const val MAX_FALLBACK_MODELS = 3

    private const val PREF_NAME = "gemini_api_prefs"
    private const val KEY_API_KEY = "gemini_api_key"
    private const val KEY_MODEL = "gemini_model"
    private const val KEY_FALLBACK_MODELS = "gemini_fallback_models_json"
    private const val KEY_CACHED_MODELS = "gemini_cached_models_json"
    private const val KEY_CUSTOM_MODELS = "gemini_custom_models_json"

    internal val LEGACY_INVALID_MODELS: Set<String> = setOf(
        "gemini-3.8-flash",
        "gemini-3.5-flash-lite",
        "gemini-4-argon",
        "gemini-4-hydrogen"
    )

    private val FALLBACK_DEFAULT_POOL: List<String> = listOf(
        "gemini-2.5-flash-lite",
        "gemini-2.0-flash",
        "gemini-2.5-flash",
        "gemini-2.5-pro",
        "gemini-2.0-flash-lite"
    )

    val SUGGESTED_MODELS: List<GeminiModelSuggestion> = listOf(
        GeminiModelSuggestion(
            id = "gemini-2.5-flash",
            title = "Gemini 2.5 Flash",
            badge = "প্রস্তাবিত (ডিফল্ট)",
            description = "নম্বরপত্র ছবি OCR, বাংলা চ্যাট ও দ্রুত কাজের জন্য সবচেয়ে ভারসাম্যপূর্ণ"
        ),
        GeminiModelSuggestion(
            id = "gemini-2.5-flash-lite",
            title = "Gemini 2.5 Flash-Lite",
            badge = "অতি দ্রুত",
            description = "কম টোকেন খরচে দ্রুত চ্যাট, মন্তব্য খসড়া ও হালকা স্ক্যানের জন্য"
        ),
        GeminiModelSuggestion(
            id = "gemini-2.0-flash",
            title = "Gemini 2.0 Flash",
            badge = "স্থিতিশীল",
            description = "ব্যস্ত সময়ে নির্ভরযোগ্য মাল্টিমোডাল ও উচ্চ-গতির বিকল্প মডেল"
        ),
        GeminiModelSuggestion(
            id = "gemini-2.5-pro",
            title = "Gemini 2.5 Pro",
            badge = "উন্নত বিশ্লেষণ",
            description = "জটিল বা অস্পষ্ট হাতের লেখার নম্বরপত্র নিখুঁতভাবে বিশ্লেষণের জন্য"
        )
    )

    val SUGGESTED_MODEL_IDS: Set<String> = SUGGESTED_MODELS.map { it.id }.toSet()

    fun normalizeApiKey(raw: String): String = raw.trim()

    fun normalizeModelId(raw: String, cachedIds: Set<String> = emptySet()): String {
        val cleaned = raw.trim()
            .removePrefix("models/")
            .trim()
        if (cleaned.isBlank()) return DEFAULT_MODEL
        if (cleaned in LEGACY_INVALID_MODELS && cleaned !in cachedIds) return DEFAULT_MODEL
        return cleaned
    }

    fun defaultFallbacksFor(primaryModel: String): List<String> {
        val cleanPrimary = normalizeModelId(primaryModel)
        return FALLBACK_DEFAULT_POOL
            .filter { it != cleanPrimary }
            .take(MAX_FALLBACK_MODELS)
    }

    fun normalizeFallbackList(
        rawFallbacks: List<String>,
        primaryModel: String,
        cachedIds: Set<String> = emptySet()
    ): List<String> {
        val cleanPrimary = normalizeModelId(primaryModel, cachedIds)
        val out = mutableListOf<String>()
        for (raw in rawFallbacks) {
            val cleaned = raw.trim().removePrefix("models/").trim()
            if (cleaned.isBlank()) continue
            if (cleaned in LEGACY_INVALID_MODELS && cleaned !in cachedIds) continue
            if (cleaned == cleanPrimary || cleaned in out) continue
            out.add(cleaned)
            if (out.size >= MAX_FALLBACK_MODELS) break
        }
        return out
    }

    fun assignFallbackSlot(
        current: List<String>,
        slotIndex: Int,
        rawModelId: String?,
        primaryModel: String,
        cachedIds: Set<String> = emptySet()
    ): List<String> {
        val safePrimary = normalizeModelId(primaryModel, cachedIds)
        val normalizedCurrent = normalizeFallbackList(current, safePrimary, cachedIds).toMutableList()
        val cleanedNew = rawModelId?.trim()?.removePrefix("models/")?.trim().orEmpty()
        if (cleanedNew.isBlank()) {
            if (slotIndex in normalizedCurrent.indices) {
                normalizedCurrent.removeAt(slotIndex)
            }
            return normalizedCurrent
        }
        if (cleanedNew == safePrimary || (cleanedNew in LEGACY_INVALID_MODELS && cleanedNew !in cachedIds)) {
            return normalizedCurrent
        }
        val existingIdx = normalizedCurrent.indexOf(cleanedNew)
        if (slotIndex in normalizedCurrent.indices) {
            if (existingIdx >= 0 && existingIdx != slotIndex) {
                val displaced = normalizedCurrent[slotIndex]
                normalizedCurrent[slotIndex] = cleanedNew
                normalizedCurrent[existingIdx] = displaced
            } else {
                normalizedCurrent[slotIndex] = cleanedNew
            }
        } else if (existingIdx < 0 && normalizedCurrent.size < MAX_FALLBACK_MODELS) {
            normalizedCurrent.add(cleanedNew)
        }
        return normalizeFallbackList(normalizedCurrent, safePrimary, cachedIds)
    }

    fun moveFallbackSlot(current: List<String>, index: Int, delta: Int): List<String> {
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

    fun getSelectedModel(context: Context): String {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val cachedIds = getCachedModels(context).map { it.id }.toSet()
        val stored = sp.getString(KEY_MODEL, DEFAULT_MODEL).orEmpty()
        val resolved = normalizeModelId(stored, cachedIds)
        if (stored.isNotBlank() && stored.trim().removePrefix("models/").trim() != resolved && stored in LEGACY_INVALID_MODELS) {
            sp.edit().putString(KEY_MODEL, resolved).apply()
        }
        return resolved
    }

    fun getFallbackModels(
        context: Context,
        primaryModel: String = getSelectedModel(context)
    ): List<String> {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val cachedIds = getCachedModels(context).map { it.id }.toSet()
        val cleanPrimary = normalizeModelId(primaryModel, cachedIds)
        val raw = sp.getString(KEY_FALLBACK_MODELS, null)
            ?: return defaultFallbacksFor(cleanPrimary)
        val parsed = runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    add(arr.optString(i, ""))
                }
            }
        }.getOrNull() ?: return defaultFallbacksFor(cleanPrimary)

        val hadLegacyOnly = parsed.isNotEmpty() && parsed.all {
            val c = it.trim().removePrefix("models/").trim()
            c.isBlank() || (c in LEGACY_INVALID_MODELS && c !in cachedIds) || c == cleanPrimary
        }
        if (hadLegacyOnly) {
            val defaults = defaultFallbacksFor(cleanPrimary)
            saveFallbackModels(context, defaults, cleanPrimary)
            return defaults
        }
        return normalizeFallbackList(parsed, cleanPrimary, cachedIds)
    }

    fun saveFallbackModels(
        context: Context,
        rawFallbacks: List<String>,
        primaryModel: String = getSelectedModel(context)
    ): List<String> {
        val cachedIds = getCachedModels(context).map { it.id }.toSet()
        val clean = normalizeFallbackList(rawFallbacks, primaryModel, cachedIds)
        val arr = JSONArray()
        clean.forEach { arr.put(it) }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FALLBACK_MODELS, arr.toString())
            .apply()
        clean.forEach { rememberCustomModel(context, it) }
        return clean
    }

    fun resetDefaultFallbacks(
        context: Context,
        primaryModel: String = getSelectedModel(context)
    ): List<String> = saveFallbackModels(context, defaultFallbacksFor(primaryModel), primaryModel)

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
                    val id = arr.optString(i, "").trim().removePrefix("models/").trim()
                    if (id.isNotBlank() && id !in SUGGESTED_MODEL_IDS && (id !in LEGACY_INVALID_MODELS || id in cachedIds)) {
                        add(id)
                    }
                }
            }.distinct()
        }.getOrDefault(emptyList())
    }

    fun rememberCustomModel(context: Context, rawModel: String): List<String> {
        val cleaned = rawModel.trim().removePrefix("models/").trim()
        val existing = getCustomModels(context)
        if (cleaned.isBlank() || cleaned in SUGGESTED_MODEL_IDS || cleaned in LEGACY_INVALID_MODELS) return existing
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
        val cachedIds = getCachedModels(context).map { it.id }.toSet()
        val model = normalizeModelId(rawModel, cachedIds)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_API_KEY, key)
            .putString(KEY_MODEL, model)
            .apply()
        rememberCustomModel(context, model)
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (sp.contains(KEY_FALLBACK_MODELS)) {
            val curFallbacks = getFallbackModels(context, model)
            if (curFallbacks.any { it == model }) {
                saveFallbackModels(context, curFallbacks, model)
            }
        }
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
    val sharedWorkingDaysByTerm: Map<Int, String> = emptyMap()
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
    val rowsArr = json.optJSONArray("rows")
    val marksArr = json.optJSONArray("marks")
    val hasStudentObj = json.optJSONObject("student")?.let { st ->
        st.optString("name", "").isNotBlank() || st.optInt("roll", 0) > 0
    } == true

    val isSingle = when {
        rawMode.contains("SINGLE") -> true
        rawMode.contains("CLASS") -> false
        (rowsArr == null || rowsArr.length() == 0) && ((marksArr?.length() ?: 0) > 0 || hasStudentObj) -> true
        else -> false
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

internal fun parseExtractedClassSheetJson(
    json: JSONObject,
    clsHint: Int?,
    termHint: Int?,
    subKeyHint: String?,
    requestedScope: ClassSheetImportScope = ClassSheetImportScope.AUTO,
    sheetKindHint: ClassSheetKind? = null,
    qrMeta: BlankSheetQrMeta? = null
): ExtractedClassSheet {
    val refCode = json.optString("refCode", "").trim().uppercase()
        .ifBlank { qrMeta?.refCode.orEmpty() }
    val refMatch = Regex("REF\\s*:\\s*C(\\d{1,2})-([A-Z]+)-([A-Z0-9]+)(?:-([A-Z0-9]+))?").find(refCode)
    val refCls = refMatch?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..20 }
    val refSub = refMatch?.groupValues?.getOrNull(2)?.takeIf { it.isNotBlank() && it != "ATT" && it != "GEN" }
    val refTerm = when (refMatch?.groupValues?.getOrNull(3)) {
        "T1" -> 1
        "T2" -> 2
        "T3" -> 3
        else -> null
    }
    val refLayout = refMatch?.groupValues?.getOrNull(4).orEmpty()

    val detectedCls = (clsHint ?: qrMeta?.cls ?: refCls ?: json.optInt("cls", 3)).coerceIn(1, 20)
    val detectedTerm = (termHint ?: qrMeta?.term?.takeIf { it in 1..3 } ?: refTerm ?: json.optInt("term", 1)).coerceIn(1, 3)
    val rawSub = (qrMeta?.subjectKey?.takeIf { it.isNotBlank() } ?: refSub ?: json.optString("subKey")).trim()
    val validSubs = subjectsFor(detectedCls, RELIGIONS.first()).map { it.key }.toSet()
    val normalizedSub = normalizeDetectedSubjectKey(rawSub, detectedCls, validSubs)
    val detectedSub = when {
        !subKeyHint.isNullOrBlank() && subKeyHint in validSubs -> subKeyHint
        normalizedSub != null -> normalizedSub
        else -> validSubs.first()
    }
    val activeSub = subjectsFor(detectedCls, RELIGIONS.first()).find { it.key == detectedSub }
    val hasWritten = (activeSub?.written ?: 0) > 0

    val warningsArr = json.optJSONArray("warnings") ?: JSONArray()
    val warnings = (0 until warningsArr.length()).mapNotNull { i ->
        warningsArr.optString(i)?.trim()?.takeIf { it.isNotBlank() }
    }.toMutableList()

    val sharedWdObj = json.optJSONObject("sharedWorkingDays") ?: JSONObject()
    val sharedWorkingDays = linkedMapOf<Int, String>()
    for (t in 1..3) {
        val rawWd = sharedWdObj.optString(t.toString(), "").ifBlank {
            sharedWdObj.optString("T$t", "")
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

    val rawSheetKind = json.optString("sheetKind", "").trim().uppercase()
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

    val rowsArr = json.optJSONArray("rows") ?: JSONArray()
    val rawParsed = (0 until rowsArr.length()).mapNotNull { idx ->
        val obj = rowsArr.optJSONObject(idx) ?: return@mapNotNull null
        val rollStr = fromBn(obj.optString("roll", "").trim())
        val nameStr = obj.optString("name", "").trim()
        val marksObj = obj.optJSONObject("marks") ?: JSONObject()
        val marksMap = mutableMapOf<String, String>()
        val traitsMap = mutableMapOf<String, String>()

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
            val v = marksObj.optString(k, "").trim()
            if (v.isNotEmpty() && !v.equals("null", ignoreCase = true)) {
                val salvagedTraitKey = if (looksLikeTraitsSheet) normalizeTraitJsonKey(k) else null
                if (salvagedTraitKey != null) {
                    putTraitValue(salvagedTraitKey, v)
                } else {
                    marksMap[k.trim().uppercase()] = fromBn(v)
                }
            }
        }
        val traitsObj = obj.optJSONObject("traits") ?: JSONObject()
        traitsObj.keys().forEach { k ->
            val v = traitsObj.optString(k, "").trim()
            if (v.isNotEmpty() && !v.equals("null", ignoreCase = true)) {
                val normKey = normalizeTraitJsonKey(k) ?: k.trim().uppercase()
                putTraitValue(normKey, v)
            }
        }
        val remarkStr = obj.optString("remark", "").trim()
            .takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
            .orEmpty()

        val uncObj = obj.optJSONObject("uncertainFields") ?: JSONObject()
        val aiIssues = mutableMapOf<String, String>()
        uncObj.keys().forEach { k ->
            val reason = uncObj.optString(k, "").trim()
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
            remark = remarkStr,
            traitValues = traitsMap,
            rawNotes = obj.optString("rawNotes", "").trim(),
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
        val qrHasSub = !qrMeta?.subjectKey.isNullOrBlank()
        if (subKeyHint.isNullOrBlank() && !qrHasSub && refSub == null && (metaFound != null && !metaFound.optBoolean("sub", true) || normalizedSub == null)) {
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
                key == "name" && !sheetHasAnyName && row.name.isBlank() -> false
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

    return ExtractedClassSheet(
        cls = detectedCls,
        term = detectedTerm,
        subKey = detectedSub,
        summary = json.optString("summary", "").ifBlank {
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
        sharedWorkingDaysByTerm = sharedWorkingDays
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
            val fallbacks = GeminiApiConfig.getFallbackModels(appContext, selected)
            fallbacks.firstOrNull { it.contains("lite", ignoreCase = true) }
                ?: fallbacks.firstOrNull { it.contains("flash", ignoreCase = true) }
                ?: GeminiApiConfig.FAST_FALLBACK_MODEL
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
    private val autoScanCache = object : LinkedHashMap<String, AutoScanResult>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, AutoScanResult>?): Boolean = size > 8
    }
    private val remarkCache = object : LinkedHashMap<String, AiDraftRemarkResult>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, AiDraftRemarkResult>?): Boolean = size > 16
    }

    fun clearCooldowns() {
        modelCooldownUntilMs.clear()
    }

    fun clearCooldownFor(modelId: String) {
        modelCooldownUntilMs.remove(GeminiApiConfig.normalizeModelId(modelId))
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

    private fun recordServerBusyCooldown(modelId: String, waitSec: Int = 15) {
        val clean = GeminiApiConfig.normalizeModelId(modelId)
        modelCooldownUntilMs[clean] = System.currentTimeMillis() + waitSec.coerceIn(5, 60) * 1000L
    }

    private fun selectFallbackCandidates(
        primaryModel: String,
        configuredFallbacks: List<String>
    ): List<String> {
        val cleanPrimary = GeminiApiConfig.normalizeModelId(primaryModel)
        val ordered = configuredFallbacks
            .map { GeminiApiConfig.normalizeModelId(it) }
            .filter { it != cleanPrimary }
            .distinct()
            .take(GeminiApiConfig.MAX_FALLBACK_MODELS)

        val healthy = ordered.filter { remainingCooldownSeconds(it) == 0 }
        // If all configured fallbacks happen to be in transient cooldown, still keep
        // the one whose cooldown expires earliest as a last resort so user action succeeds if ready.
        return if (healthy.isNotEmpty()) {
            healthy
        } else {
            ordered.sortedBy { remainingCooldownSeconds(it) }
        }
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

        val cleanPrimary = GeminiApiConfig.normalizeModelId(primaryModelId)
        val serializedBody = body.toString().toByteArray(Charsets.UTF_8)
        val isVisionOrHeavy = serializedBody.size > 80_000
        var lastError: Exception? = null
        val attemptedModels = mutableListOf<String>()
        val primaryCooldown = remainingCooldownSeconds(cleanPrimary)

        // 1. Try primary model first if not in cooldown
        if (primaryCooldown == 0) {
            attemptedModels.add(cleanPrimary)
            for (attempt in 0..1) {
                try {
                    val text = callGenerateContent(apiKey, cleanPrimary, body, serializedBody)
                    modelCooldownUntilMs.remove(cleanPrimary)
                    return text to cleanPrimary
                } catch (e: GeminiHttpException) {
                    lastError = e
                    if (e.httpCode == 401 || e.httpCode == 403) throw e
                    if (e.httpCode == 429) {
                        recordRateLimitCooldown(e.modelId, e.retryAfterSeconds)
                        break
                    }
                    if (e.httpCode == 503) {
                        recordServerBusyCooldown(e.modelId)
                        break
                    }
                    if (e.httpCode == 400 || e.httpCode == 404) {
                        break
                    }
                    val isTransient = e.httpCode in setOf(500, 502, 504)
                    if (isTransient && attempt == 0) {
                        delay(800L)
                        continue
                    }
                    break
                } catch (e: GeminiNetworkException) {
                    lastError = e
                    if (e.isTimeout && isVisionOrHeavy) {
                        break
                    }
                    if (attempt == 0 && (e.isTimeout || e.isConnectionError)) {
                        delay(800L)
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
                modelId = cleanPrimary,
                userMessage = formatRateLimitMessage(cleanPrimary, primaryCooldown),
                retryAfterSeconds = primaryCooldown
            )
        }

        // 2. Try all user-configured fallback models in priority order (up to 3)
        val httpErr = lastError as? GeminiHttpException
        val netErr = lastError as? GeminiNetworkException
        val allowHttpFallback = httpErr != null && httpErr.httpCode in setOf(400, 404, 429, 500, 502, 503, 504)
        val allowTimeoutFallback = netErr != null && (netErr.isTimeout || netErr.isConnectionError)
        if (allowHttpFallback || allowTimeoutFallback) {
            val selectedPrimary = GeminiApiConfig.getSelectedModel(appContext)
            val configuredFallbacks = GeminiApiConfig.getFallbackModels(appContext, selectedPrimary)
            val fallbackPool = if (cleanPrimary != selectedPrimary) {
                configuredFallbacks + selectedPrimary
            } else {
                configuredFallbacks
            }
            val fallbacks = selectFallbackCandidates(cleanPrimary, fallbackPool)
            for (fallbackModel in fallbacks) {
                if (!hasNetworkTransport(appContext)) break
                attemptedModels.add(fallbackModel)
                try {
                    val text = callGenerateContent(apiKey, fallbackModel, body, serializedBody)
                    modelCooldownUntilMs.remove(fallbackModel)
                    return text to fallbackModel
                } catch (e: GeminiHttpException) {
                    lastError = e
                    if (e.httpCode == 401 || e.httpCode == 403) throw e
                    if (e.httpCode == 429) {
                        recordRateLimitCooldown(e.modelId, e.retryAfterSeconds)
                    } else if (e.httpCode == 503) {
                        recordServerBusyCooldown(e.modelId)
                    }
                } catch (e: GeminiNetworkException) {
                    lastError = e
                } catch (_: Exception) {
                    // Continue to next configured fallback
                }
            }
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
        val effSubHint = subKeyHint ?: resolvedQr?.subjectKey?.takeIf {
            it.isNotBlank() && it != BlankSheetCatalog.ALL_SUBJECTS_KEY
        }
        val effKindHint = sheetKindHint ?: resolvedQr?.sheetKind
        val effScope = if (importScope == ClassSheetImportScope.AUTO && resolvedQr != null && resolvedQr.importScope != ClassSheetImportScope.AUTO) {
            resolvedQr.importScope
        } else {
            importScope
        }

        val cleanNote = teacherNote.trim()
        val qrKeyPart = resolvedQr?.let { "${it.refCode}|${it.columnIds.joinToString(",")}|${it.workingDaysByTerm}" }.orEmpty()
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
                "4. উপস্থিতি ও গুণাবলি শিট (`ATTENDANCE_TRAITS`): উপরের মোট কার্যদিবস স্ট্রিপ থাকলে `sharedWorkingDays` (`\"1\"`,`\"2\"`,`\"3\"`)-এ দিন, এবং প্রতি সারিতে `traits`-এ `DAYS_t`, `PRES_t`, `ACT_t` (সক্রিয়তা/সহপাঠ), `DISC_t` (শৃঙ্খলা), `COND_t` (আচরণ), `REM_t` (মন্তব্য) দিন (১ প্রান্তিকের শিট হলে সংশ্লিষ্ট প্রান্তিকের `_t`, যেমন `PRES_${effTermHint ?: 1}`, `ACT_${effTermHint ?: 1}` দিন; গুণাবলি মান `ক`, `খ` বা `গ`; `marks` ফাঁকা `{}` রাখুন)।"
            effKindHint == ClassSheetKind.THREE_TERM_MARKS ->
                "4. ৩ প্রান্তিক নম্বর শিট (`THREE_TERM_MARKS`): পাশাপাশি ১ম, ২য়, ৩য় প্রান্তিকের ধারাবাহিক/সামষ্টিক/মোট `marks`-এ `" +
                    "\"T1_CONT\",\"T1_WR\",\"T1_TOTAL\",\"T2_CONT\",\"T2_WR\",\"T2_TOTAL\",\"T3_CONT\",\"T3_WR\",\"T3_TOTAL\"` হিসেবে দিন এবং থাকলে `remark` দিন।"
            effScope == ClassSheetImportScope.CONT_ONLY ->
                "4. একক প্রান্তিক নম্বর হলে কেবল রোল (ও থাকলে নাম) এবং ধারাবাহিক নম্বর (`sheetContinuousTotal` বা `TB`/`ACT`/`LANG`/`CT`) পড়ুন — `WR` ও `sheetGrandTotal` ফাঁকা রাখুন।"
            effScope == ClassSheetImportScope.WR_ONLY ->
                "4. একক প্রান্তিক নম্বর হলে কেবল রোল (ও থাকলে নাম) এবং সামষ্টিক/লিখিত (`WR`) পড়ুন — ধারাবাহিক ও `sheetGrandTotal` ফাঁকা রাখুন।"
            effScope == ClassSheetImportScope.BOTH ->
                "4. একক প্রান্তিক নম্বর হলে থাকা ধারাবাহিক, সামষ্টিক (`WR`), মোট (`sheetGrandTotal`) এবং থাকলে মন্তব্য (`remark`) পড়ুন।"
            else ->
                """
                4. ছবির কলাম ও শিরোনাম দেখে সঠিক `sheetKind` বেছে নিন:
                   (ক) "SINGLE_TERM_MARKS": ১ প্রান্তিক নম্বর শিট (পূর্ণাঙ্গ `TB,ACT,LANG,CT,CONT,WR,TOTAL`, সংক্ষিপ্ত `CONT,WR,TOTAL`, শুধু ধারাবাহিক, শুধু সামষ্টিক, অথবা সহজ মোট `TOTAL` + থাকলে `remark`)। কেবল `মোট` থাকলে `sheetGrandTotal` দিন, `CONT`/`WR` অনুমান করবেন না।
                   (খ) "THREE_TERM_MARKS": ৩ প্রান্তিক নম্বর শিট (পাশাপাশি ১ম, ২য়, ৩য় প্রান্তিকের ধারাবাহিক/সামষ্টিক/মোট) → `marks`-এ `"T1_CONT","T1_WR","T1_TOTAL","T2_CONT","T2_WR","T2_TOTAL","T3_CONT","T3_WR","T3_TOTAL"` দিন।
                   (গ) "ATTENDANCE_TRAITS": উপস্থিতি ও গুণাবলি/হাজিরা শিট (যেমন: রোল, নাম, উপস্থিত/উপস্থিতি, সক্রিয়তা/সহপাঠ, শৃঙ্খলা, আচরণ) → উপরের মোট কার্যদিবস স্ট্রিপ থাকলে `sharedWorkingDays: {"1":"","2":"","3":""}`-এ দিন, এবং প্রতি সারিতে `traits`-এ `DAYS_1..3`, `PRES_1..3`, `ACT_1..3`, `DISC_1..3`, `COND_1..3`, `REM_1..3` দিন (১ প্রান্তিকের শিট হলে কেবল সংশ্লিষ্ট প্রান্তিকের কী যেমন `PRES_1`,`ACT_1`,`DISC_1`,`COND_1`,`REM_1` দিন; গুণাবলি `ক`/`খ`/`গ`)।
                """.trimIndent()
        }
        val forcedKindHint = effKindHint?.let { "ফরম্যাট অবশ্যই: ${it.name}" } ?: "ফরম্যাট: ছবি অনুযায়ী স্বয়ংক্রিয়"
        val qrInstructionsBlock = resolvedQr?.let { SheetQrCodec.buildAiPromptInstructions(it) }.orEmpty()
        val structLine = if (effKindHint == ClassSheetKind.ATTENDANCE_TRAITS) {
            "উপস্থিতি ও গুণাবলি কাঠামো: কার্যদিবস=DAYS_t, উপস্থিত/উপস্থিতি=PRES_t, সক্রিয়তা/সহপাঠ=ACT_t, শৃঙ্খলা=DISC_t, আচরণ=COND_t, মন্তব্য=REM_t; গুণাবলি মান: ক (উত্তম), খ (সন্তোষজনক), গ (উন্নতি প্রয়োজন)"
        } else {
            "বিষয় কাঠামো: $subsInfo"
        }

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
            appendLine("5. অনুপস্থিত বা ফাঁকা কীগুলো বাদ দিন। `refCode`-এ ফুটারে `REF:...` পেলে সেটি দিন, নাহলে \"${resolvedQr?.refCode.orEmpty()}\"।")
            appendLine("6. ছবিতে আসলে কোনো লেখা/সংখ্যা অস্পষ্ট হলে তবেই `uncertainFields`-এ কারণ দিন। `metaFound`-এ শ্রেণি/প্রান্তিক/বিষয় পেলে true দিন।")
            append("JSON: {\"sheetKind\":\"${effKindHint?.name ?: "SINGLE_TERM_MARKS"}\",\"refCode\":\"${resolvedQr?.refCode.orEmpty()}\",\"cls\":${effClsHint ?: 3},\"term\":${effTermHint ?: 1},\"subKey\":\"${if (effKindHint == ClassSheetKind.ATTENDANCE_TRAITS) "" else effSubHint ?: "MATH"}\",\"metaFound\":{\"cls\":${effClsHint != null},\"term\":${effTermHint != null},\"sub\":${!effSubHint.isNullOrBlank()}},\"sharedWorkingDays\":{\"1\":\"\",\"2\":\"\",\"3\":\"\"},\"summary\":\"\",\"warnings\":[],\"rows\":[{\"roll\":\"1\",\"name\":\"\",\"marks\":{\"TB\":\"\",\"ACT\":\"\",\"LANG\":\"\",\"CT\":\"\",\"WR\":\"\",\"T1_CONT\":\"\",\"T1_WR\":\"\"},\"sheetContinuousTotal\":\"\",\"sheetGrandTotal\":\"\",\"remark\":\"\",\"traits\":{\"PRES_1\":\"\",\"ACT_1\":\"\",\"DISC_1\":\"\",\"COND_1\":\"\",\"REM_1\":\"\"},\"rawNotes\":\"\",\"uncertainFields\":{}}]}")
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
            val cleanModel = GeminiApiConfig.normalizeModelId(modelId)
            val hasJsonMime = body.optJSONObject("generationConfig")?.optString("responseMimeType") == "application/json"
            val thinkingSteps = thinkingStepsForModel(cleanModel, body)
            val isGemmaModel = cleanModel.startsWith("gemma-", ignoreCase = true)

            val candidateConfigs: List<Pair<JSONObject?, Boolean>> = buildList {
                if (isGemmaModel && hasJsonMime) {
                    add(null to true)
                } else {
                    thinkingSteps.forEach { tc -> add(tc to false) }
                    if (hasJsonMime) add(null to true)
                }
            }

            for (idx in candidateConfigs.indices) {
                val (thinkingCfg, stripJson) = candidateConfigs[idx]
                val effectiveBytes = if (thinkingCfg == null && !stripJson && preSerializedBody != null) {
                    preSerializedBody
                } else {
                    cloneBodyWithGenerationTweaks(body, thinkingCfg, stripJson)
                        .toString()
                        .toByteArray(Charsets.UTF_8)
                }
                try {
                    return@withContext executeSingleGenerateCall(apiKey, cleanModel, effectiveBytes)
                } catch (e: GeminiHttpException) {
                    if (e.httpCode == 400 && idx < candidateConfigs.lastIndex) {
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
            val fetchedIds = fetchedModels.map { it.id }.toSet()
            val effectiveModelId = GeminiApiConfig.normalizeModelId(rawModel, fetchedIds)

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
                callGenerateContent(apiKey, effectiveModelId, pingPayload)
                GeminiConnectionCheckResult(
                    ok = true,
                    message = "সংযোগ সফল! সক্রিয় মডেল: $effectiveModelId · মোট ${bn(fetchedModels.size.toString())}টি মডেল পাওয়া গেছে।",
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
