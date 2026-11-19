package com.kgps.result.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.auth.auth
import com.kgps.result.CLSN
import com.kgps.result.RELIGIONS
import com.kgps.result.bn
import com.kgps.result.fmtEn
import com.kgps.result.fromBn
import com.kgps.result.subjectsFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.math.abs

/**
 * Cloud Run প্রক্সি কনফিগারেশন।
 * চাইলে ডিপ্লয় করার পর DEFAULT_CLOUD_RUN_URL-এ URL বসিয়ে APK বিল্ড করতে পারেন,
 * অথবা অ্যাপের সেটিংস থেকে প্রতিটি ডিভাইসে Cloud Run URL সংরক্ষণ করা যাবে।
 */
object AiProxyConfig {
    const val DEFAULT_CLOUD_RUN_URL: String = ""
    private const val PREF_NAME = "ai_proxy_prefs"
    private const val KEY_CLOUD_RUN_URL = "cloud_run_url"

    fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        if (trimmed.isEmpty()) return ""
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
    }

    fun getSavedUrl(context: Context): String {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return normalizeUrl(sp.getString(KEY_CLOUD_RUN_URL, "").orEmpty())
    }

    fun getEffectiveUrl(context: Context): String {
        val local = getSavedUrl(context)
        if (local.isNotBlank()) return local
        return normalizeUrl(DEFAULT_CLOUD_RUN_URL)
    }

    fun saveUrl(context: Context, rawUrl: String) {
        val norm = normalizeUrl(rawUrl)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CLOUD_RUN_URL, norm)
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
    val resolvedFields: Set<String> = emptySet()
)

data class ExtractedClassSheet(
    val cls: Int,
    val term: Int,
    val subKey: String,
    val summary: String,
    val warnings: List<String> = emptyList(),
    val rows: List<ScannedStudentRow>,
    val isSample: Boolean = false
)

data class RowValidationReport(
    val hardErrors: Map<String, String>,
    val uncertainWarnings: Map<String, String>,
    val arithmeticWarning: String? = null,
    val calculatedContinuous: Double? = null,
    val calculatedGrandTotal: Double? = null
) {
    val hasHardError: Boolean get() = hardErrors.isNotEmpty()
    val hasAnyIssue: Boolean get() = hardErrors.isNotEmpty() || uncertainWarnings.isNotEmpty() || arithmeticWarning != null
}

fun validateScannedStudentRow(
    row: ScannedStudentRow,
    cls: Int,
    subKey: String,
    duplicateRolls: Set<Int>
): RowValidationReport {
    val hard = mutableMapOf<String, String>()
    val warn = mutableMapOf<String, String>()

    row.aiIssues.forEach { (field, reason) ->
        if (field !in row.resolvedFields) {
            warn[field] = reason.ifBlank { "লেখা অস্পষ্ট — যাচাই করুন" }
        }
    }

    val rollClean = fromBn(row.rollText.trim())
    val rollNum = rollClean.toIntOrNull()
    if (rollClean.isBlank() || rollNum == null || rollNum <= 0) {
        hard["roll"] = "সঠিক রোল লিখুন (১, ২, ৩...)"
    } else if (rollNum in duplicateRolls) {
        hard["roll"] = "রোল ${bn(rollNum.toString())} একাধিক সারিতে আছে"
    }

    if (row.name.isBlank() && "name" !in row.resolvedFields && "name" !in warn) {
        warn["name"] = "নাম পড়া যায়নি — প্রয়োজনে লিখে দিন"
    }

    val subDef = subjectsFor(cls, RELIGIONS.first()).find { it.key == subKey }
    val expectedComps = buildList {
        subDef?.comps?.forEach { add(it.key to it.max) }
        if ((subDef?.written ?: 0) > 0) {
            add("WR" to (subDef?.written ?: 70))
        }
    }

    var contSum = 0.0
    var hasAnyCont = false
    expectedComps.forEach { (cKey, maxVal) ->
        val raw = row.marksText[cKey]?.trim().orEmpty()
        if (raw.isEmpty()) return@forEach
        val normalized = fromBn(raw)
        val num = normalized.toDoubleOrNull()
        if (num == null) {
            hard[cKey] = "\"$raw\" সঠিক সংখ্যা নয় (যেমন ${bn(maxVal.toString())} বা ফাঁকা)"
        } else if (num < 0.0 || num > maxVal.toDouble()) {
            hard[cKey] = "সর্বোচ্চ ${bn(maxVal.toString())}-এর মধ্যে হতে হবে"
        } else {
            if (cKey != "WR") {
                contSum += num
                hasAnyCont = true
            }
        }
    }

    val wrVal = row.marksText["WR"]?.trim()?.takeIf { it.isNotEmpty() }?.let { fromBn(it).toDoubleOrNull() }
    val calcCont = if (hasAnyCont) contSum else null
    val calcGrand = if (calcCont != null && wrVal != null && (subDef?.written ?: 0) > 0) {
        calcCont + wrVal
    } else if (calcCont != null && (subDef?.written ?: 0) == 0) {
        calcCont
    } else {
        null
    }

    var arithWarn: String? = null
    if ("arithmetic" !in row.resolvedFields && hard.isEmpty()) {
        val sheetCont = row.sheetContinuousTotal?.trim()?.takeIf { it.isNotEmpty() }?.let { fromBn(it).toDoubleOrNull() }
        val sheetGrand = row.sheetGrandTotal?.trim()?.takeIf { it.isNotEmpty() }?.let { fromBn(it).toDoubleOrNull() }
        val contMismatch = sheetCont != null && calcCont != null && abs(sheetCont - calcCont) > 0.01
        val grandMismatch = sheetGrand != null && calcGrand != null && abs(sheetGrand - calcGrand) > 0.01
        if (contMismatch && grandMismatch) {
            arithWarn = "খাতার ধারাবাহিক (${fmtEn(sheetCont!!)}) ও সর্বমোট (${fmtEn(sheetGrand!!)}) এর সাথে যোগফল (${fmtEn(calcCont!!)} / ${fmtEn(calcGrand!!)}) মিলছে না"
        } else if (contMismatch) {
            arithWarn = "খাতায় ধারাবাহিক মোট ${bn(fmtEn(sheetCont!!))}, কিন্তু অংশগুলোর যোগফল ${bn(fmtEn(calcCont!!))}"
        } else if (grandMismatch) {
            arithWarn = "খাতায় সর্বমোট ${bn(fmtEn(sheetGrand!!))}, কিন্তু ধারাবাহিক+সামষ্টিক ${bn(fmtEn(calcGrand!!))}"
        }
    }

    return RowValidationReport(
        hardErrors = hard,
        uncertainWarnings = warn,
        arithmeticWarning = arithWarn,
        calculatedContinuous = calcCont,
        calculatedGrandTotal = calcGrand
    )
}

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
        teacherNote: String
    ): ExtractedMarksheet

    suspend fun analyzeClassSheetImage(
        bitmap: Bitmap,
        clsHint: Int?,
        termHint: Int?,
        subKeyHint: String?,
        teacherNote: String
    ): ExtractedClassSheet

    suspend fun draftRemark(
        studentSummary: String,
        term: Int
    ): AiDraftRemarkResult
}

internal fun stripJsonFences(raw: String): String {
    val trimmed = raw.trim()
    if (!trimmed.startsWith("```")) return trimmed
    return trimmed
        .removePrefix("```json")
        .removePrefix("```JSON")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()
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
    subKeyHint: String?
): ExtractedClassSheet {
    val detectedCls = (clsHint ?: json.optInt("cls", 3)).coerceIn(1, 5)
    val detectedTerm = (termHint ?: json.optInt("term", 1)).coerceIn(1, 3)
    val rawSub = json.optString("subKey").trim().uppercase()
    val validSubs = subjectsFor(detectedCls, RELIGIONS.first()).map { it.key }.toSet()
    val detectedSub = when {
        !subKeyHint.isNullOrBlank() && subKeyHint in validSubs -> subKeyHint
        rawSub in validSubs -> rawSub
        else -> validSubs.first()
    }

    val warningsArr = json.optJSONArray("warnings") ?: JSONArray()
    val warnings = (0 until warningsArr.length()).mapNotNull { i ->
        warningsArr.optString(i)?.trim()?.takeIf { it.isNotBlank() }
    }

    val rowsArr = json.optJSONArray("rows") ?: JSONArray()
    val parsedRows = (0 until rowsArr.length()).mapNotNull { idx ->
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
                val normKey = when (k.lowercase()) {
                    "roll", "name" -> k.lowercase()
                    else -> k.uppercase()
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

    return ExtractedClassSheet(
        cls = detectedCls,
        term = detectedTerm,
        subKey = detectedSub,
        summary = json.optString("summary", "").ifBlank {
            "${CLSN[detectedCls - 1]} শ্রেণি · ${bn(detectedTerm.toString())} প্রান্তিক — ${bn(parsedRows.size.toString())} জন শিক্ষার্থী শনাক্ত হয়েছে।"
        },
        warnings = warnings,
        rows = parsedRows,
        isSample = false
    )
}

/**
 * Firebase Auth ID Token-ভিত্তিক Cloud Run Gemini প্রক্সি ক্লায়েন্ট।
 * Play Store বা App Check ছাড়াই সরাসরি APK-তে নিরাপদভাবে কাজ করে।
 */
class CloudRunAiRepository(
    private val baseUrlProvider: () -> String
) : AiRepository {
    override val isPlaceholderBackend: Boolean = false

    private fun requireBaseUrl(): String {
        val url = AiProxyConfig.normalizeUrl(baseUrlProvider())
        if (url.isBlank()) {
            throw IllegalStateException("Cloud Run সার্ভার URL সেট করা নেই।")
        }
        return url
    }

    private suspend fun fetchFirebaseIdToken(forceRefresh: Boolean): String {
        val user = Firebase.auth.currentUser
            ?: throw IllegalStateException("AI ব্যবহার করতে অনুগ্রহ করে Google দিয়ে সাইন-ইন করুন।")
        val res = user.getIdToken(forceRefresh).await()
        return res.token?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Firebase Auth ID টোকেন পাওয়া যায়নি। আবার সাইন-ইন করুন।")
    }

    private fun encodeBitmapJpegBase64(bitmap: Bitmap): String {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private suspend fun postAuthorizedJson(path: String, body: JSONObject): JSONObject {
        val baseUrl = requireBaseUrl()
        var token = fetchFirebaseIdToken(forceRefresh = false)
        var attempt = 0
        while (true) {
            val (code, respText) = withContext(Dispatchers.IO) {
                val conn = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20_000
                    readTimeout = 70_000
                    doOutput = true
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                        writer.write(body.toString())
                    }
                    val statusCode = conn.responseCode
                    val stream = if (statusCode in 200..299) conn.inputStream else conn.errorStream
                    val txt = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    statusCode to txt
                } finally {
                    conn.disconnect()
                }
            }

            if (code == 401 && attempt == 0) {
                attempt++
                token = fetchFirebaseIdToken(forceRefresh = true)
                continue
            }

            val parsed = runCatching { JSONObject(respText) }.getOrNull()
            if (code !in 200..299) {
                val errMsg = parsed?.optString("error")?.takeIf { it.isNotBlank() }
                    ?: "Cloud Run সার্ভার ত্রুটি (HTTP $code)"
                throw IllegalStateException(errMsg)
            }
            return parsed ?: throw IllegalStateException("সার্ভার থেকে অবৈধ JSON উত্তর এসেছে।")
        }
    }

    override suspend fun chat(
        history: List<AiChatMessage>,
        latestUserMessage: String,
        modelMode: AiModelMode,
        studentContext: String?
    ): AiChatReply {
        val histArr = JSONArray().apply {
            history.takeLast(6).forEach { m ->
                put(JSONObject().apply {
                    put("isUser", m.isUser)
                    put("text", m.text)
                })
            }
        }
        val payload = JSONObject().apply {
            put("history", histArr)
            put("message", latestUserMessage)
            put("mode", modelMode.name)
            put("studentContext", studentContext ?: "")
        }
        val res = postAuthorizedJson("/api/ai/chat", payload)
        return AiChatReply(
            text = res.optString("text", "").ifBlank { "উত্তর পাওয়া যায়নি।" },
            modelLabel = res.optString("modelLabel", "Cloud Run · Gemini AI"),
            isPlaceholder = false
        )
    }

    override suspend fun draftRemark(studentSummary: String, term: Int): AiDraftRemarkResult {
        val payload = JSONObject().apply {
            put("studentSummary", studentSummary)
            put("term", term)
        }
        val res = postAuthorizedJson("/api/ai/draft-remark", payload)
        return AiDraftRemarkResult(
            remark = res.optString("remark", "").ifBlank {
                "${bn(term.toString())} প্রান্তিকে শিক্ষার্থীর অগ্রগতি সন্তোষজনক।"
            },
            isPlaceholder = false
        )
    }

    override suspend fun analyzeMarksheetImage(bitmap: Bitmap, teacherNote: String): ExtractedMarksheet {
        val b64 = withContext(Dispatchers.Default) { encodeBitmapJpegBase64(bitmap) }
        val payload = JSONObject().apply {
            put("imageBase64", b64)
            put("mimeType", "image/jpeg")
            put("teacherNote", teacherNote)
        }
        val res = postAuthorizedJson("/api/ai/analyze-marksheet", payload)
        val dataObj = res.optJSONObject("data") ?: res
        return parseExtractedMarksheetJson(dataObj)
    }

    override suspend fun analyzeClassSheetImage(
        bitmap: Bitmap,
        clsHint: Int?,
        termHint: Int?,
        subKeyHint: String?,
        teacherNote: String
    ): ExtractedClassSheet {
        val targetCls = clsHint ?: 3
        val subsInfo = subjectsFor(targetCls, RELIGIONS.first()).joinToString("; ") { s ->
            val compDesc = s.comps.joinToString(", ") { "${it.key}=${it.max}" }
            val wrDesc = if (s.written > 0) ", WR=${s.written}" else ""
            "${s.key}(${s.bn}): $compDesc$wrDesc"
        }
        val b64 = withContext(Dispatchers.Default) { encodeBitmapJpegBase64(bitmap) }
        val payload = JSONObject().apply {
            put("imageBase64", b64)
            put("mimeType", "image/jpeg")
            if (clsHint != null) put("clsHint", clsHint)
            if (termHint != null) put("termHint", termHint)
            if (!subKeyHint.isNullOrBlank()) put("subKeyHint", subKeyHint)
            put("subsInfo", subsInfo)
            put("teacherNote", teacherNote)
        }
        val res = postAuthorizedJson("/api/ai/analyze-class-sheet", payload)
        val dataObj = res.optJSONObject("data") ?: res
        return parseExtractedClassSheetJson(dataObj, clsHint, termHint, subKeyHint)
    }

    companion object {
        suspend fun testHealth(rawUrl: String): Result<String> = withContext(Dispatchers.IO) {
            runCatching {
                val base = AiProxyConfig.normalizeUrl(rawUrl)
                require(base.isNotBlank()) { "Cloud Run URL খালি রাখা যাবে না।" }
                val conn = (URL("$base/health").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15_000
                    readTimeout = 15_000
                }
                try {
                    val code = conn.responseCode
                    val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        .orEmpty()
                    if (code !in 200..299) {
                        error("সার্ভার সাড়া দেয়নি (HTTP $code)")
                    }
                    val json = JSONObject(body)
                    val projectId = json.optString("projectId", "")
                    val geminiOk = json.optBoolean("geminiConfigured", false)
                    val model = json.optString("model", "")
                    if (!geminiOk) {
                        "সার্ভার সংযুক্ত হয়েছে ($projectId), তবে Cloud Run-এ GEMINI_API_KEY সেট করা নেই।"
                    } else {
                        "সংযোগ সফল! প্রজেক্ট: $projectId · মডেল: $model"
                    }
                } finally {
                    conn.disconnect()
                }
            }
        }
    }
}

/**
 * Cloud Run URL কনফিগার করা থাকলে CloudRunAiRepository (Firebase Auth ID Token) ব্যবহার করে,
 * নতুবা Firebase AI Logic (FirebaseAiRepository)-এ চলে।
 */
class HybridAiRepository(private val appContext: Context) : AiRepository {
    private val cloudRunRepo = CloudRunAiRepository { AiProxyConfig.getEffectiveUrl(appContext) }
    private val firebaseAiRepo = FirebaseAiRepository()

    fun isUsingCloudRun(): Boolean = AiProxyConfig.getEffectiveUrl(appContext).isNotBlank()

    private fun activeRepo(): AiRepository = if (isUsingCloudRun()) cloudRunRepo else firebaseAiRepo

    override val isPlaceholderBackend: Boolean = false

    override suspend fun chat(
        history: List<AiChatMessage>,
        latestUserMessage: String,
        modelMode: AiModelMode,
        studentContext: String?
    ): AiChatReply = activeRepo().chat(history, latestUserMessage, modelMode, studentContext)

    override suspend fun analyzeMarksheetImage(
        bitmap: Bitmap,
        teacherNote: String
    ): ExtractedMarksheet = activeRepo().analyzeMarksheetImage(bitmap, teacherNote)

    override suspend fun analyzeClassSheetImage(
        bitmap: Bitmap,
        clsHint: Int?,
        termHint: Int?,
        subKeyHint: String?,
        teacherNote: String
    ): ExtractedClassSheet = activeRepo().analyzeClassSheetImage(bitmap, clsHint, termHint, subKeyHint, teacherNote)

    override suspend fun draftRemark(
        studentSummary: String,
        term: Int
    ): AiDraftRemarkResult = activeRepo().draftRemark(studentSummary, term)
}

/**
 * Firebase AI Logic (Gemini Developer API) ব্যবহার করে সরাসরি লাইভ AI ইমপ্লিমেন্টেশন।
 */
class FirebaseAiRepository : AiRepository {
    override val isPlaceholderBackend: Boolean = false

    private fun modelFor(mode: AiModelMode, jsonMode: Boolean = false) = Firebase.ai(
        backend = GenerativeBackend.googleAI()
    ).generativeModel(
        modelName = "gemini-2.5-flash",
        generationConfig = generationConfig {
            temperature = if (jsonMode) 0.1f else if (mode == AiModelMode.FAST) 0.3f else 0.5f
            if (jsonMode) {
                responseMimeType = "application/json"
            }
        }
    )

    override suspend fun chat(
        history: List<AiChatMessage>,
        latestUserMessage: String,
        modelMode: AiModelMode,
        studentContext: String?
    ): AiChatReply {
        val recentTurns = history.takeLast(6).joinToString("\n") { m ->
            val who = if (m.isUser) "শিক্ষক" else "সহায়ক"
            "$who: ${m.text.take(400)}"
        }
        val prompt = buildString {
            appendLine("আপনি বাংলাদেশের সরকারি প্রাথমিক বিদ্যালয়ের (১ম–৫ম শ্রেণি) একজন অভিজ্ঞ শিক্ষামূলক AI সহায়ক।")
            appendLine("সব উত্তর সহজ, প্রাঞ্জল ও প্রাতিষ্ঠানিক বাংলায় দিন।")
            appendLine("NCTB প্রাথমিক মূল্যায়ন কাঠামো: ১ম–২য় শ্রেণি (বাংলা/ইংরেজি/গণিত = ধারাবাহিক ৫০ + সামষ্টিক ৫০), ৩য়–৫ম শ্রেণি (বাংলা/ইংরেজি/গণিত/বিজ্ঞান/বাওবি/ধর্ম = ধারাবাহিক ৩০ [পাঠ্যবই ১০ + সক্রিয়তা ৫ + ভাষা ৫ + ক্লাস টেস্ট ১০] + সামষ্টিক ৭০)।")
            if (!studentContext.isNullOrBlank()) {
                appendLine("নির্বাচিত শিক্ষার্থীর তথ্য: $studentContext")
            }
            if (recentTurns.isNotBlank()) {
                appendLine("পূর্ববর্তী কথোপকথন:\n$recentTurns")
            }
            appendLine("শিক্ষকের বর্তমান প্রশ্ন: $latestUserMessage")
        }
        try {
            val response = modelFor(modelMode, jsonMode = false).generateContent(prompt)
            val replyText = response.text?.trim().orEmpty()
                .ifBlank { "দুঃখিত, এই মুহূর্তে উত্তর পাওয়া যায়নি। আবার চেষ্টা করুন।" }
            val label = if (modelMode == AiModelMode.FAST) "Gemini Flash · দ্রুত" else "Gemini AI · বিস্তারিত"
            return AiChatReply(text = replyText, modelLabel = label, isPlaceholder = false)
        } catch (e: Exception) {
            throw IllegalStateException(friendlyAiError(e), e)
        }
    }

    override suspend fun draftRemark(
        studentSummary: String,
        term: Int
    ): AiDraftRemarkResult {
        val prompt = """
            বাংলাদেশের প্রাথমিক বিদ্যালয়ের ${bn(term.toString())} প্রান্তিক মূল্যায়নের জন্য নিচের শিক্ষার্থীর তথ্যের ভিত্তিতে ২–৩ বাক্যের একটি ইতিবাচক, গঠনমূলক ও প্রাতিষ্ঠানিক বাংলা মন্তব্য (Remark) লিখে দিন। শুধু মন্তব্যটি লিখুন, কোনো শিরোনাম বা অতিরিক্ত ভূমিকা দেবেন না।
            তথ্য: $studentSummary
        """.trimIndent()
        try {
            val res = modelFor(AiModelMode.FAST, jsonMode = false).generateContent(prompt)
            val txt = res.text?.trim().orEmpty()
                .ifBlank { "${bn(term.toString())} প্রান্তিকে শিক্ষার্থীর অগ্রগতি সন্তোষজনক; নিয়মিত অনুশীলন চালিয়ে গেলে আরও ভালো করবে।" }
            return AiDraftRemarkResult(remark = txt, isPlaceholder = false)
        } catch (e: Exception) {
            throw IllegalStateException(friendlyAiError(e), e)
        }
    }

    override suspend fun analyzeMarksheetImage(
        bitmap: Bitmap,
        teacherNote: String
    ): ExtractedMarksheet {
        val prompt = """
            আপনি বাংলাদেশের প্রাথমিক বিদ্যালয়ের নম্বরপত্র OCR বিশেষজ্ঞ। ছবি থেকে একজন শিক্ষার্থীর তথ্য ও নম্বর JSON আকারে বের করুন।
            অতিরিক্ত নির্দেশনা: ${teacherNote.ifBlank { "নেই" }}
            বিষয় কোড: BAN (বাংলা), ENG (ইংরেজি), MATH (গণিত), SOC (সামাজিক ও বিজ্ঞান ১-২য়), SCI (বিজ্ঞান ৩-৫ম), BGW (বাওবি), REL (ধর্ম), PE (শারীরিক), ART (শিল্পকলা)।
            অংশ কোড: TB (পাঠ্যবই), ACT (সক্রিয়তা), LANG (ভাষা), CT (ক্লাস টেস্ট), WR (সামষ্টিক লিখিত), TOTAL (শিল্পকলা মোট)।
            কী ফরম্যাট: "<SUB>_<TERM>_<COMP>", যেমন "MATH_1_TB", "MATH_1_WR"।
            যদি কোনো লেখা বুঝতে অসুবিধা হয় বা কাটাকাটি থাকে, uncertainNotes লিস্টে বাংলায় উল্লেখ করুন।
            ঠিক এই JSON স্কিমায় উত্তর দিন:
            {
              "summary": "সংক্ষিপ্ত বাংলা বিবরণ",
              "term": 1,
              "student": { "name": "", "cls": 3, "roll": 1, "guardian": "", "religion": "ইসলাম" },
              "marks": [ { "key": "MATH_1_TB", "value": 9.0, "subjectLabel": "গণিত (পাঠ্যবই)" } ],
              "attendanceAndTraits": { "days": "", "present": "", "act": "", "disc": "", "cond": "", "remark": "" },
              "uncertainNotes": ["যদি কোনো লেখা অস্পষ্ট থাকে তার বিবরণ"]
            }
        """.trimIndent()
        try {
            val input = Content(role = "user", parts = listOf(ImagePart(bitmap), TextPart(prompt)))
            val raw = modelFor(AiModelMode.GENERAL, jsonMode = true).generateContent(input).text.orEmpty()
            return parseExtractedMarksheetJson(JSONObject(stripJsonFences(raw)))
        } catch (e: Exception) {
            throw IllegalStateException(friendlyAiError(e), e)
        }
    }

    override suspend fun analyzeClassSheetImage(
        bitmap: Bitmap,
        clsHint: Int?,
        termHint: Int?,
        subKeyHint: String?,
        teacherNote: String
    ): ExtractedClassSheet {
        val targetCls = clsHint ?: 3
        val subsInfo = subjectsFor(targetCls, RELIGIONS.first()).joinToString("; ") { s ->
            val compDesc = s.comps.joinToString(", ") { "${it.key}=${it.max}" }
            val wrDesc = if (s.written > 0) ", WR=${s.written}" else ""
            "${s.key}(${s.bn}): $compDesc$wrDesc"
        }
        val prompt = """
            আপনি বাংলাদেশের প্রাথমিক বিদ্যালয়ের (১ম–৫ম শ্রেণি) হাতে লেখা শ্রেণি-নম্বরপত্র (Marksheet Register) OCR বিশেষজ্ঞ।
            ছবিতে একাধিক শিক্ষার্থীর রোল, নাম ও একটি বিষয়ের অংশভিত্তিক নম্বর রয়েছে। সব সারি নিখুঁতভাবে পড়ে শুধু JSON দিন।

            ব্যবহারকারীর নির্বাচন/ইঙ্গিত:
            - শ্রেণি: ${clsHint?.let { "${CLSN[it - 1]} ($it)" } ?: "ছবি থেকে শনাক্ত করুন (1 থেকে 5)"}
            - প্রান্তিক: ${termHint?.let { "${bn(it.toString())}ম ($it)" } ?: "ছবি থেকে শনাক্ত করুন (1, 2 বা 3)"}
            - বিষয় কোড: ${subKeyHint ?: "ছবি থেকে শনাক্ত করুন (BAN, ENG, MATH, SCI, BGW, SOC, REL, PE, ART)"}
            - অতিরিক্ত নোট: ${teacherNote.ifBlank { "নেই" }}

            বিষয় ও সর্বোচ্চ নম্বরের কাঠামো:
            $subsInfo

            গুরুত্বপূর্ণ OCR ও যাচাইকরণ নিয়ম:
            1. বাংলা ও ইংরেজি সব সংখ্যা JSON-এ ইংরেজি অঙ্কে লিখুন (যেমন ০৭ -> "7", ২৫ -> "25")। শিক্ষার্থীর নাম বাংলাতেই পরিষ্কারভাবে লিখুন।
            2. ক্লাস টেস্ট (CT) ঘরে যদি "০৬+০৭=১৩/২=০৭" বা "A+০৮=০৮/২=০৪" ধরনের হিসাব থাকে, তবে "=" চিহ্নের পরের চূড়ান্ত গড় মানটি ("7" বা "4") `CT` ফিল্ডে দিন এবং `rawNotes`-এ সংক্ষেপে লিখুন।
            3. কোনো ঘরে `A` বা `অনু` লেখা থাকলে `0` বসান এবং `rawNotes`-এ "অনুপস্থিত (A)" লিখুন। কোনো ঘর সম্পূর্ণ ফাঁকা থাকলে খালি স্ট্রিং "" দিন।
            4. যদি খাতায় "মোট নম্বর (ধারাবাহিক)" বা "প্রান্তিক সর্বমোট" কলাম থাকে, সেগুলোও `sheetContinuousTotal` ও `sheetGrandTotal` ফিল্ডে দিন।
            5. কোনো শিক্ষার্থীর নাম, রোল বা কোনো নম্বর অস্পষ্ট, ঝাপসা, কাটা-ছেঁড়া বা বুঝতে অসুবিধা হলে সেরা অনুমানটি সেই ফিল্ডে দিন এবং `uncertainFields` অবজেক্টে সেই ফিল্ড কী ("roll", "name", "TB", "ACT", "LANG", "CT", "WR", "TOTAL") দিয়ে বাংলায় সংক্ষিপ্ত কারণ লিখুন। একেবারেই পড়া না গেলে মান "" দিন এবং `uncertainFields`-এ অবশ্যই উল্লেখ করুন।

            ঠিক নিচের JSON ফরম্যাটে উত্তর দিন:
            {
              "cls": ${clsHint ?: 3},
              "term": ${termHint ?: 1},
              "subKey": "${subKeyHint ?: "MATH"}",
              "summary": "ছবি থেকে শনাক্ত শ্রেণি, বিষয় ও মোট সারির সংক্ষিপ্ত বিবরণ",
              "warnings": [],
              "rows": [
                {
                  "roll": "1",
                  "name": "শিক্ষার্থীর নাম",
                  "marks": { "TB": "9", "ACT": "5", "LANG": "5", "CT": "7", "WR": "50" },
                  "sheetContinuousTotal": "26",
                  "sheetGrandTotal": "76",
                  "rawNotes": "",
                  "uncertainFields": {}
                }
              ]
            }
        """.trimIndent()

        try {
            val input = Content(role = "user", parts = listOf(ImagePart(bitmap), TextPart(prompt)))
            val raw = modelFor(AiModelMode.GENERAL, jsonMode = true).generateContent(input).text.orEmpty()
            return parseExtractedClassSheetJson(JSONObject(stripJsonFences(raw)), clsHint, termHint, subKeyHint)
        } catch (e: Exception) {
            throw IllegalStateException(friendlyAiError(e), e)
        }
    }

    private fun friendlyAiError(e: Exception): String {
        val msg = (e.message ?: "") + " " + (e.cause?.message ?: "")
        val lower = msg.lowercase()
        return when {
            lower.contains("appcheck") || lower.contains("app check") ->
                "Firebase Console-এ App Check চালু (Enforced) থাকার কারণে অনুরোধটি আটকে গেছে। সেটিংস থেকে আপনার Cloud Run প্রক্সি URL যোগ করুন অথবা Firebase Console-এ App Check → Firebase AI Logic অংশে 'Unenforce' করুন।"
            lower.contains("api has not been used") ||
                lower.contains("not enabled") ||
                lower.contains("firebasevertexai") ||
                lower.contains("generativeai") ||
                lower.contains("403") ->
                "Firebase AI Logic চালু করা নেই অথবা Cloud Run প্রক্সি URL সেট করা নেই। সেটিংসে Cloud Run URL দিন অথবা Firebase Console-এ AI Logic চালু করুন।"
            lower.contains("unable to resolve host") ||
                lower.contains("network") ||
                lower.contains("timeout") ->
                "ইন্টারনেট সংযোগ পাওয়া যায়নি। নেটওয়ার্ক যাচাই করে আবার চেষ্টা করুন।"
            lower.contains("quota") || lower.contains("429") ->
                "Gemini API-এর সাময়িক ব্যবহার সীমা পূর্ণ হয়েছে। কিছুক্ষণ পর আবার চেষ্টা করুন।"
            lower.contains("json") ->
                "ছবিটি থেকে কাঠামোবদ্ধ ডেটা পড়া যায়নি। আরও স্পষ্ট বা সোজা ছবি তুলে আবার চেষ্টা করুন।"
            else -> "AI বিশ্লেষণ ব্যর্থ হয়েছে: ${e.localizedMessage ?: "অজানা ত্রুটি"}"
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
        teacherNote: String
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
            uncertainNotes = listOf("নমুনা মোড: বাস্তব স্ক্যানের জন্য লাইভ Gemini AI ব্যবহার করুন"),
            isPlaceholder = true
        )
    }

    override suspend fun analyzeClassSheetImage(
        bitmap: Bitmap,
        clsHint: Int?,
        termHint: Int?,
        subKeyHint: String?,
        teacherNote: String
    ): ExtractedClassSheet {
        delay(500)
        val cls = (clsHint ?: 3).coerceIn(1, 5)
        val term = (termHint ?: 1).coerceIn(1, 3)
        val sub = subKeyHint ?: "MATH"
        return ExtractedClassSheet(
            cls = cls,
            term = term,
            subKey = sub,
            summary = "[নমুনা প্রিভিউ] অস্পষ্ট ঘরগুলো কীভাবে হাইলাইট হয় ও ঠিক করতে হয় তা দেখার জন্য নমুনা তালিকা।",
            warnings = listOf("এটি একটি নমুনা স্ক্যান — প্রকৃত ছবির জন্য 'স্ক্যান ও প্রিভিউ দেখুন' ব্যবহার করুন।"),
            rows = listOf(
                ScannedStudentRow(
                    rollText = "1",
                    name = "আরিয়ান ইসলাম খালিদ",
                    marksText = mapOf("TB" to "9", "ACT" to "5", "LANG" to "5", "CT" to "7", "WR" to "50"),
                    sheetContinuousTotal = "26",
                    sheetGrandTotal = "76",
                    rawNotes = "CT: ০৬+০৭=১৩/২=০৭"
                ),
                ScannedStudentRow(
                    rollText = "2",
                    name = "মোঃ ইয়ামিন মোল্যা",
                    marksText = mapOf("TB" to "9", "ACT" to "5", "LANG" to "5", "CT" to "7", "WR" to "46"),
                    sheetContinuousTotal = "26",
                    sheetGrandTotal = "72",
                    aiIssues = mapOf("TB" to "৭ না কি ৯ — লেখা কিছুটা অস্পষ্ট")
                ),
                ScannedStudentRow(
                    rollText = "3",
                    name = "খাদিজা তুল কুবরা",
                    marksText = mapOf("TB" to "7", "ACT" to "5", "LANG" to "5", "CT" to "5", "WR" to "34"),
                    sheetContinuousTotal = "25",
                    sheetGrandTotal = "56",
                    aiIssues = mapOf("name" to "বানান যাচাই করুন")
                ),
                ScannedStudentRow(
                    rollText = "4",
                    name = "মোঃ সোয়াইব মোল্যা",
                    marksText = mapOf("TB" to "7", "ACT" to "4", "LANG" to "3", "CT" to "5", "WR" to ""),
                    sheetContinuousTotal = "19",
                    rawNotes = "সামষ্টিক ঘর খাতায় ফাঁকা"
                )
            ),
            isSample = true
        )
    }

    override suspend fun draftRemark(studentSummary: String, term: Int): AiDraftRemarkResult {
        delay(350)
        return AiDraftRemarkResult(
            remark = "${bn(term.toString())} প্রান্তিকের মূল্যায়নে শিক্ষার্থীর অংশগ্রহণ ভালো।",
            isPlaceholder = true
        )
    }
}
