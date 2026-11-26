package com.kgps.result.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import com.kgps.result.AdmitCardPlanner
import com.kgps.result.ExamRoutineEntry
import com.kgps.result.Settings
import com.kgps.result.bn
import com.kgps.result.classNameBn
import com.kgps.result.fromBn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.roundToInt

data class ExtractedClassRoutineGroup(
    val classes: List<Int>,
    val classGroupLabel: String,
    val examTime: String,
    val rows: List<ExamRoutineEntry>
)

data class ExtractedExamRoutineNotice(
    val examTitle: String,
    val detectedTerm: Int?,
    val detectedYear: String,
    val layoutType: String,
    val groups: List<ExtractedClassRoutineGroup>,
    val footnote: String,
    val warnings: List<String> = emptyList(),
    val usedModel: String = ""
)

object ExamRoutineNoticeExtractor {

    suspend fun decodeNoticeUriToBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        val mime = runCatching { context.contentResolver.getType(uri).orEmpty().lowercase() }.getOrDefault("")
        val displayName = queryDisplayName(context, uri).lowercase()
        val isPdf = mime.contains("pdf") || displayName.endsWith(".pdf") || uri.toString().lowercase().endsWith(".pdf")
        if (isPdf) {
            decodeFirstPdfPageBitmap(context, uri)
        } else {
            VisionImageOptimizer.decodeUriBitmap(context, uri, maxLongestSide = 2400)
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return@runCatching cursor.getString(idx).orEmpty()
            }
        }
        ""
    }.getOrDefault("")

    private fun decodeFirstPdfPageBitmap(context: Context, uri: Uri): Bitmap? {
        return runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount <= 0) return@runCatching null
                    renderer.openPage(0).use { page ->
                        val maxSide = 2200f
                        val longest = maxOf(page.width, page.height).coerceAtLeast(1)
                        val scale = (maxSide / longest).coerceIn(1.5f, 3.5f)
                        val w = (page.width * scale).roundToInt().coerceAtLeast(1)
                        val h = (page.height * scale).roundToInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        Canvas(bmp).drawColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp
                    }
                }
            }
        }.getOrNull()
    }

    suspend fun extractFromBitmap(
        repository: DirectGeminiAiRepository,
        bitmap: Bitmap,
        allowedClasses: List<Int>,
        settings: Settings,
        documentEnhance: Boolean = true
    ): ExtractedExamRoutineNotice {
        val activeClasses = allowedClasses.ifEmpty { listOf(1, 2, 3, 4, 5) }
        val prompt = buildNoticePrompt(activeClasses, settings)
        val (raw, usedModel) = repository.requestVisionJson(
            bitmap = bitmap,
            prompt = prompt,
            maxOutputTokens = 4096,
            documentEnhance = documentEnhance
        )
        return try {
            parseNoticeJson(
                rawJson = raw,
                allowedClasses = activeClasses,
                settings = settings,
                usedModel = usedModel
            )
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException(
                "নোটিশটি থেকে রুটিন কাঠামো পড়া যায়নি ($usedModel)। আরও স্পষ্ট ছবি বা সরাসরি প্রথম পাতার ছবি/PDF দিন।",
                e
            )
        }
    }

    internal fun buildNoticePrompt(
        allowedClasses: List<Int>,
        settings: Settings
    ): String {
        val classMapDesc = allowedClasses.joinToString(", ") { cls ->
            "$cls=${classNameBn(cls, settings)}"
        }
        return buildString {
            appendLine("বাংলাদেশের বিদ্যালয়ের পরীক্ষার সময়সূচি/নোটিশ (যেকোনো লেআউট) পর্যবেক্ষণ করে শুধু JSON দিন।")
            appendLine("সক্রিয় শ্রেণি আইডি ম্যাপিং: $classMapDesc")
            appendLine("ছবি বা নোটিশের ফরম্যাট যেকোনো রকম হতে পারে—প্রথমে টেবিলের গঠন পর্যবেক্ষণ করে `layoutType` নির্ধারণ করুন:")
            appendLine("• MATRIX_GROUPED_CLASSES: বামে তারিখ ও বার, ডানে শ্রেণি-গ্রুপ কলাম (যেমন: '১ম ও ২য় শ্রেণি', '৩য় শ্রেণি', '৪র্থ ও ৫ম শ্রেণি') এবং প্রতিটি কলামের উপরে/নিচে পরীক্ষার সময়।")
            appendLine("• MATRIX_PER_CLASS: প্রতিটি শ্রেণির জন্য আলাদা কলাম।")
            appendLine("• TRANSPOSED_TABLE: উপরে তারিখ ও বার এবং বামের সারিতে শ্রেণি।")
            appendLine("• SINGLE_SHARED_TABLE: সবার জন্য একই কলাম ('তারিখ | বার | বিষয় | সময়')।")
            appendLine("• SECTION_OR_TEXT_LIST: শ্রেণি অনুযায়ী আলাদা অনুচ্ছেদ বা তালিকা।")
            appendLine("কঠোর নিয়ম:")
            appendLine("1. প্রতিটি শ্রেণি/গ্রুপ কলামের জন্য আলাদা `classRoutines` অবজেক্ট দিন, যেখানে `classes` হলো ম্যাচিং পূর্ণসংখ্যা আইডি তালিকা (যেমন `[1, 2]`, `[3]`, `[4, 5]`)।")
            appendLine("2. যে তারিখে কোনো শ্রেণির ঘর ফাঁকা/নেই (অথবা '-', 'ছুটি', 'বন্ধ'), সেই শ্রেণির `rows`-এ ঐ তারিখ রাখবেন না।")
            appendLine("3. একই দিনে ও একই কলামে একাধিক বিষয় থাকলে (দুই লাইন বা '/' বা '+') `subjects` অ্যারেতে আলাদা স্ট্রিং হিসেবে দিন।")
            appendLine("4. তারিখ অবশ্যই 'DD/MM/YYYY' ফরম্যাটে বাংলা অঙ্কে দিন (যেমন '০১/১২/২০২৬'); বার বাংলায় ('মঙ্গলবার')।")
            appendLine("5. `examTime` সংক্ষিপ্ত ও পরিষ্কার রাখুন (যেমন 'সকাল ১০:০০-১২:০০ টা', 'বেলা ০১:০০-০৩:৩০ টা')। প্রতিটি সারিতে আলাদা সময় থাকলে প্রথম/প্রধান সময় বা সংক্ষিপ্ত সময় দিন।")
            appendLine("6. শিরোনাম থেকে `term` (১ম/প্রথম/১ম সাময়িক=1, ২য়/দ্বিতীয়/অর্ধবার্ষিক=2, ৩য়/তৃতীয়/বার্ষিক=3, না বুঝলে 0) এবং `year` দিন।")
            appendLine("7. টেবিলের নিচে যদি শিক্ষার্থীদের/মৌখিক ও ধারাবাহিক মূল্যায়নের বিশেষ `বিঃদ্রঃ` থাকে, সেটি সংক্ষেপে `footnote`-এ দিন; সাধারণ প্রশাসনিক নির্দেশনা (১..১০) বাদ দিন।")
            append(
                """JSON ফরম্যাট: {"examTitle":"","term":3,"year":"২০২৬","layoutType":"MATRIX_GROUPED_CLASSES","footnote":"","warnings":[],"classRoutines":[{"classes":[1,2],"classGroupLabel":"১ম ও ২য় শ্রেণি","examTime":"সকাল ১০:০০-১২:০০ টা","rows":[{"date":"০১/১২/২০২৬","day":"মঙ্গলবার","subjects":["ইংরেজি"]}]}]}"""
            )
        }
    }

    internal fun parseNoticeJson(
        rawJson: String,
        allowedClasses: List<Int>,
        settings: Settings,
        usedModel: String = ""
    ): ExtractedExamRoutineNotice {
        val slice = extractBalancedJsonPayload(rawJson)
        val root = if (slice.trimStart().startsWith("[")) {
            JSONObject().put("classRoutines", JSONArray(slice))
        } else {
            JSONObject(slice)
        }

        val examTitle = root.optString("examTitle").trim()
        val yearRaw = root.optString("year").trim()
        val detectedYear = if (yearRaw.isNotBlank()) bn(yearRaw) else ""
        val detectedTerm = parseTerm(root.opt("term"), examTitle)
        val layoutType = root.optString("layoutType").trim().ifBlank { "AUTO" }
        val footnote = cleanFootnote(root.optString("footnote").trim())

        val warnings = mutableListOf<String>()
        val warnArr = root.optJSONArray("warnings")
        if (warnArr != null) {
            for (i in 0 until warnArr.length()) {
                val w = warnArr.optString(i).trim()
                if (w.isNotBlank()) warnings.add(w)
            }
        }

        val routinesArr = root.optJSONArray("classRoutines")
            ?: root.optJSONArray("routines")
            ?: root.optJSONArray("groups")
            ?: JSONArray()

        val parsedGroups = mutableListOf<ExtractedClassRoutineGroup>()
        for (i in 0 until routinesArr.length()) {
            val gObj = routinesArr.optJSONObject(i) ?: continue
            val groupLabel = gObj.optString("classGroupLabel").trim()
                .ifBlank { gObj.optString("classLabel").trim() }
            val resolvedClasses = resolveClassIds(
                arr = gObj.optJSONArray("classes"),
                label = groupLabel,
                allowedClasses = allowedClasses,
                settings = settings,
                isSharedLayout = layoutType.equals("SINGLE_SHARED_TABLE", ignoreCase = true)
            )
            if (resolvedClasses.isEmpty()) continue

            val rawTime = gObj.optString("examTime").trim()
                .ifBlank { gObj.optString("time").trim() }
            val normalizedTime = normalizeExamTime(rawTime)

            val rowsArr = gObj.optJSONArray("rows") ?: gObj.optJSONArray("entries") ?: JSONArray()
            val rawEntries = mutableListOf<ExamRoutineEntry>()
            for (rIdx in 0 until rowsArr.length()) {
                val rObj = rowsArr.optJSONObject(rIdx) ?: continue
                val rawDate = rObj.optString("date").trim()
                val rawDay = rObj.optString("day").trim()
                val subjList = extractSubjectsFromRowJson(rObj)
                if (subjList.isEmpty()) continue

                val (normDate, normDay) = normalizeDateAndDay(rawDate, rawDay)
                if (normDate.isBlank() && subjList.isEmpty()) continue
                rawEntries.add(
                    ExamRoutineEntry(
                        id = UUID.randomUUID().toString(),
                        date = normDate,
                        day = normDay,
                        subjects = subjList
                    )
                )
            }
            val mergedRows = mergeSameDateEntries(rawEntries)
            if (mergedRows.isEmpty()) continue

            val finalLabel = groupLabel.ifBlank {
                resolvedClasses.joinToString(", ") { classNameBn(it, settings) }
            }
            parsedGroups.add(
                ExtractedClassRoutineGroup(
                    classes = resolvedClasses,
                    classGroupLabel = finalLabel,
                    examTime = normalizedTime,
                    rows = mergedRows
                )
            )
        }

        if (parsedGroups.isEmpty()) {
            throw IllegalStateException("নোটিশ থেকে কোনো পরীক্ষার রুটিনের সারি পাওয়া যায়নি। ছবিতে টেবিল অংশ স্পষ্ট রেখে আবার চেষ্টা করুন।")
        }

        return ExtractedExamRoutineNotice(
            examTitle = examTitle,
            detectedTerm = detectedTerm,
            detectedYear = detectedYear,
            layoutType = layoutType,
            groups = parsedGroups,
            footnote = footnote,
            warnings = warnings.distinct(),
            usedModel = usedModel
        )
    }

    internal fun canonicalizeSubjectForClass(
        rawSubject: String,
        cls: Int,
        settings: Settings
    ): String {
        val trimmed = rawSubject.trim()
        if (trimmed.isBlank()) return ""
        val classSubs = AdmitCardPlanner.classSubjectLabelsFor(cls, settings)
        classSubs.find { it.equals(trimmed, ignoreCase = true) }?.let { return it }

        val lower = trimmed.lowercase().replace(Regex("\\s+"), " ")
        val mapped = when {
            lower == "বাংলা" || lower.contains("bangla") -> "বাংলা"
            lower == "ইংরেজি" || lower == "ইংরেজী" || lower.contains("english") -> "ইংরেজি"
            lower == "গণিত" || lower == "গনিত" || lower == "প্রাথমিক গনিত" || lower.contains("math") -> "প্রাথমিক গণিত"
            lower == "বিজ্ঞান" || lower == "প্রাথমিক বিজ্ঞান" || lower.contains("science") ->
                if ("প্রাথমিক বিজ্ঞান" in classSubs) "প্রাথমিক বিজ্ঞান" else "সামাজিক বিজ্ঞান ও বিজ্ঞান"
            lower == "বাওবি" || lower.contains("বিশ্বপরিচয়") || lower.contains("বিশ্ব পরিচয়") || lower.contains("বিশ্ব পরিচয়") || lower.contains("bgs") ->
                if ("বাংলাদেশ ও বিশ্বপরিচয়" in classSubs) "বাংলাদেশ ও বিশ্বপরিচয়" else "সামাজিক বিজ্ঞান ও বিজ্ঞান"
            lower.contains("ধর্ম") || lower.contains("ইসলাম") || lower.contains("হিন্দু") ||
                lower.contains("বৌদ্ধ") || lower.contains("খ্রীষ্ট") || lower.contains("খ্রিস্ট") ->
                "ধর্ম ও নৈতিক শিক্ষা"
            lower.contains("সামাজিক") || lower.contains("সমাজ") ->
                if ("সামাজিক বিজ্ঞান ও বিজ্ঞান" in classSubs) "সামাজিক বিজ্ঞান ও বিজ্ঞান" else "বাংলাদেশ ও বিশ্বপরিচয়"
            lower.contains("শারীরিক") || lower.contains("স্বাস্থ্য") -> "শারীরিক ও স্বাস্থ্য শিক্ষা"
            lower.contains("শিল্প") || lower.contains("চারু") -> "শিল্পকলা"
            lower.contains("সংগীত") || lower.contains("সঙ্গীত") -> "সংগীত"
            else -> trimmed
        }
        return if (mapped in classSubs) mapped else mapped
    }

    private fun parseTerm(termAny: Any?, examTitle: String): Int? {
        val intVal = when (termAny) {
            is Number -> termAny.toInt()
            is String -> fromBn(termAny).trim().toIntOrNull()
            else -> null
        }
        if (intVal != null && intVal in 1..3) return intVal

        val lower = examTitle.lowercase()
        return when {
            lower.contains("তৃতীয়") || lower.contains("৩য়") || lower.contains("বার্ষিক") || lower.contains("3rd") || lower.contains("final") -> 3
            lower.contains("দ্বিতীয়") || lower.contains("২য়") || lower.contains("অর্ধবার্ষিক") || lower.contains("2nd") || lower.contains("half") -> 2
            lower.contains("প্রথম") || lower.contains("১ম") || lower.contains("1st") -> 1
            else -> null
        }
    }

    private fun cleanFootnote(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .replace(Regex("^[*\\s]*বিঃদ্রঃ[:：\\s-]*"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    internal fun resolveClassIds(
        arr: JSONArray?,
        label: String,
        allowedClasses: List<Int>,
        settings: Settings,
        isSharedLayout: Boolean
    ): List<Int> {
        val out = linkedSetOf<Int>()
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val rawItem = arr.opt(i)
                when (rawItem) {
                    is Number -> {
                        val id = rawItem.toInt()
                        if (id in allowedClasses) out.add(id)
                    }
                    is String -> {
                        parseClassTokens(rawItem, allowedClasses, settings, out)
                    }
                }
            }
        }
        if (out.isEmpty() && label.isNotBlank()) {
            parseClassTokens(label, allowedClasses, settings, out)
        }
        if (out.isEmpty() && isSharedLayout) {
            out.addAll(allowedClasses)
        }
        return out.toList().sorted()
    }

    private fun parseClassTokens(
        raw: String,
        allowedClasses: List<Int>,
        settings: Settings,
        out: MutableSet<Int>
    ) {
        val text = fromBn(raw).lowercase()
        if (text.contains("সকল") || text.contains("সব শ্রেণি") || text.contains("all")) {
            out.addAll(allowedClasses)
            return
        }
        if (0 in allowedClasses && (text.contains("প্রাক") || text.contains("প্লে") || text.contains("নার্সারি") || text.contains("pre"))) {
            out.add(0)
        }
        allowedClasses.forEach { cls ->
            val customLabel = fromBn(classNameBn(cls, settings)).lowercase()
            if (customLabel.isNotBlank() && text.contains(customLabel)) {
                out.add(cls)
            }
        }
        val wordToNum = mapOf(
            "প্রথম" to 1, "দ্বিতীয়" to 2, "তৃতীয়" to 3, "চতুর্থ" to 4, "পঞ্চম" to 5,
            "ষষ্ঠ" to 6, "সপ্তম" to 7, "অষ্টম" to 8, "নবম" to 9, "দশম" to 10
        )
        wordToNum.forEach { (word, num) ->
            if (text.contains(word) && num in allowedClasses) out.add(num)
        }

        val rangeMatch = Regex("(\\d+)\\s*(?:য়|ম|ষ্ঠ|র্থ|st|nd|rd|th)?\\s*(?:ও|থেকে|হতে|[-–—]|to)\\s*(\\d+)").findAll(text)
        rangeMatch.forEach { m ->
            val a = m.groupValues[1].toIntOrNull()
            val b = m.groupValues[2].toIntOrNull()
            if (a != null && b != null) {
                val lo = minOf(a, b)
                val hi = maxOf(a, b)
                if (hi - lo <= 12) {
                    for (c in lo..hi) {
                        if (c in allowedClasses) out.add(c)
                    }
                }
            }
        }

        Regex("\\d+").findAll(text).forEach { m ->
            val n = m.value.toIntOrNull()
            if (n != null && n in allowedClasses) out.add(n)
        }
    }

    internal fun normalizeExamTime(raw: String): String {
        if (raw.isBlank()) return ""
        val s = fromBn(raw.trim())
            .replace(Regex("\\s+"), " ")
            .replace(Regex("\\s*(?:থেকে|হতে|[-–—]|to)\\s*"), "-")
            .replace(Regex("(\\d{1,2}:\\d{2})\\s*টা-"), "$1-")
            .replace(Regex("(\\d{1,2}:\\d{2})(?:\\s*টা)?$"), "$1 টা")
            .replace(Regex("(\\d)\\s*টা"), "$1 টা")
        return bn(s.trim())
    }

    private val NON_SUBJECT_TOKENS = setOf(
        "-", "--", "---", "—", "–", "×", "x", "n/a", "nil", "none", "নাই", "নেই", "খালি", "ফাঁকা", "ছুটি", "বন্ধ"
    )

    private val COMPOSITE_SINGLE_SUBJECTS = listOf(
        "বাংলাদেশ ও বিশ্বপরিচয়",
        "বাংলাদেশ ও বিশ্ব পরিচয়",
        "ধর্ম ও নৈতিক শিক্ষা",
        "সামাজিক বিজ্ঞান ও বিজ্ঞান",
        "শারীরিক ও স্বাস্থ্য শিক্ষা",
        "বিজ্ঞান ও প্রযুক্তি",
        "ইসলাম ও নৈতিক শিক্ষা",
        "হিন্দু ধর্ম ও নৈতিক শিক্ষা",
        "বৌদ্ধ ধর্ম ও নৈতিক শিক্ষা",
        "খ্রিস্ট ধর্ম ও নৈতিক শিক্ষা",
        "খ্রীষ্ট ধর্ম ও নৈতিক শিক্ষা"
    )

    private fun extractSubjectsFromRowJson(rObj: JSONObject): List<String> {
        val collected = mutableListOf<String>()
        val arr = rObj.optJSONArray("subjects")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val piece = arr.optString(i).trim()
                if (piece.isNotBlank()) splitSubjectCellText(piece, collected)
            }
        }
        val single = rObj.optString("subject").trim()
        if (collected.isEmpty() && single.isNotBlank()) {
            splitSubjectCellText(single, collected)
        }
        return collected
            .map { it.replace(Regex("\\s+"), " ").trim() }
            .filter { token ->
                token.isNotBlank() && token.lowercase() !in NON_SUBJECT_TOKENS
            }
            .distinct()
    }

    private fun splitSubjectCellText(raw: String, out: MutableList<String>) {
        val lines = raw.split(Regex("[\\r\\n]+|/+|\\s*\\+\\s*|\\s*,\\s*|\\s+অথবা\\s+"))
        for (line in lines) {
            val clean = line.trim()
            if (clean.isBlank() || clean.lowercase() in NON_SUBJECT_TOKENS) continue
            val isCompositeKnown = COMPOSITE_SINGLE_SUBJECTS.any { clean.contains(it, ignoreCase = true) }
            if (!isCompositeKnown && clean.contains(" ও ")) {
                val parts = clean.split(Regex("\\s+ও\\s+")).map { it.trim() }.filter { it.isNotBlank() }
                if (parts.size >= 2 && parts.all { it.length >= 3 }) {
                    out.addAll(parts)
                    continue
                }
            }
            out.add(clean)
        }
    }

    internal fun normalizeDateAndDay(rawDate: String, rawDay: String): Pair<String, String> {
        val asciiDate = fromBn(rawDate).trim()
        val parsedMillis = AdmitCardPlanner.parseDateToUtcMillis(asciiDate)
        if (parsedMillis != null) {
            return AdmitCardPlanner.formatDateAndDayFromUtcMillis(parsedMillis)
        }
        val cleanedDate = bn(rawDate.trim())
        val inferredDay = AdmitCardPlanner.inferBanglaWeekday(cleanedDate)
        val normalizedDay = inferredDay.ifBlank { normalizeBanglaWeekdayName(rawDay) }
        return cleanedDate to normalizedDay
    }

    private fun normalizeBanglaWeekdayName(raw: String): String {
        val s = raw.trim()
        if (s.isBlank()) return ""
        return when {
            s.contains("রবি") -> "রবিবার"
            s.contains("সোম") -> "সোমবার"
            s.contains("মঙ্গল") -> "মঙ্গলবার"
            s.contains("বুধ") -> "বুধবার"
            s.contains("বৃহ") -> "বৃহস্পতিবার"
            s.contains("শুক্র") -> "শুক্রবার"
            s.contains("শনি") -> "শনিবার"
            else -> s
        }
    }

    private fun mergeSameDateEntries(entries: List<ExamRoutineEntry>): List<ExamRoutineEntry> {
        if (entries.size <= 1) return entries
        val byKey = linkedMapOf<String, ExamRoutineEntry>()
        entries.forEach { entry ->
            val key = entry.date.trim().ifBlank { entry.id }
            val existing = byKey[key]
            if (existing == null) {
                byKey[key] = entry
            } else {
                val mergedSubjects = (existing.nonEmptySubjects + entry.nonEmptySubjects).distinct()
                byKey[key] = existing.copy(
                    day = existing.day.ifBlank { entry.day },
                    subjects = mergedSubjects.ifEmpty { existing.subjects }
                )
            }
        }
        return byKey.values.toList()
    }
}
