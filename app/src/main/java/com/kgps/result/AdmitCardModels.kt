package com.kgps.result

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

data class ExamRoutineEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: String = "",
    val day: String = "",
    val subject: String = ""
)

data class AdmitCardConfig(
    val selectedClass: Int = 5,
    val rollOrder: SeatPlanRollOrder = SeatPlanRollOrder.DESCENDING,
    val rollFrom: Int? = null,
    val rollTo: Int? = null,
    val term: Int = 3,
    val cardsPerPage: Int = 4,
    val marginInches: Float = 0.35f,
    val verticalGapPt: Float = 8f,
    val bnFontId: String = "kalpurush",
    val enFontId: String = "times_new_roman",
    val borderStyle: SeatPlanBorderStyle = SeatPlanBorderStyle.SOLID,
    val borderWeight: SeatPlanBorderWeight = SeatPlanBorderWeight.THIN,
    val cornerRadiusPt: Float = 6f,
    val showCutLines: Boolean = true,
    val padRollTwoDigits: Boolean = true,
    val numeralMode: SeatPlanNumeralMode = SeatPlanNumeralMode.BENGALI,
    val customSchoolName: String = "",
    val customSchoolAddress: String = "",
    val showSchoolAddress: Boolean = true,
    val customExamTitle: String = "",
    val badgeLabel: String = "প্রবেশপত্র",
    val customRoutineTitle: String = "",
    val examTimeLabel: String = "দুপুর ০১:০০-০৩:৩০ টা",
    val headTeacherTitle: String = "প্রধান শিক্ষকের স্বাক্ষর",
    val showRoutineTable: Boolean = true,
    val includeBlankRoutineRow: Boolean = true,
    val showSignatureImage: Boolean = true,
    val signatureScale: Float = 1.0f,
    val extraBlankCards: Int = 0
) {
    val safeCardsPerPage: Int get() = cardsPerPage.coerceIn(2, 5)
    val safeMarginInches: Float get() = marginInches.coerceIn(0.15f, 0.85f)
    val safeMarginPt: Float get() = safeMarginInches * 72f
    val safeVerticalGapPt: Float get() = verticalGapPt.coerceIn(0f, 24f)
    val safeCornerRadiusPt: Float get() = cornerRadiusPt.coerceIn(0f, 24f)
    val safeSignatureScale: Float get() = signatureScale.coerceIn(0.6f, 1.5f)
}

data class AdmitCardItem(
    val studentName: String,
    val cls: Int,
    val roll: Int?,
    val isBlank: Boolean = false
)

data class AdmitCardPageSpec(
    val pageNumber: Int,
    val totalPages: Int,
    val items: List<AdmitCardItem>,
    val routineRows: List<ExamRoutineEntry>,
    val config: AdmitCardConfig
)

object AdmitCardPlanner {

    val BANGLA_WEEKDAYS = listOf(
        "রবিবার",
        "সোমবার",
        "মঙ্গলবার",
        "বুধবার",
        "বৃহস্পতিবার",
        "শুক্রবার",
        "শনিবার"
    )

    fun formatDigits(raw: String, mode: SeatPlanNumeralMode): String = when (mode) {
        SeatPlanNumeralMode.BENGALI -> bn(raw)
        SeatPlanNumeralMode.ENGLISH -> fromBn(raw)
    }

    fun formatRoll(roll: Int?, cfg: AdmitCardConfig): String {
        if (roll == null) return "........"
        val base = if (cfg.padRollTwoDigits) roll.toString().padStart(2, '0') else roll.toString()
        return formatDigits(base, cfg.numeralMode)
    }

    fun resolvedSchoolName(cfg: AdmitCardConfig, settings: Settings): String =
        cfg.customSchoolName.trim().ifBlank {
            settings.school.trim().ifBlank { "প্রাথমিক বিদ্যালয়" }
        }

    fun resolvedSchoolAddress(cfg: AdmitCardConfig, settings: Settings): String {
        if (!cfg.showSchoolAddress) return ""
        return cfg.customSchoolAddress.trim().ifBlank { settings.schoolAddress.trim() }
    }

    fun resolvedExamTitle(cfg: AdmitCardConfig, settings: Settings): String {
        val custom = cfg.customExamTitle.trim()
        if (custom.isNotEmpty()) return custom
        val rawYear = settings.year.trim().ifBlank { "২০২৬" }
        val yearStr = formatDigits(rawYear, cfg.numeralMode)
        val termLabel = termNameBn(cfg.term.coerceIn(1, 3))
        return "$termLabel মূল্যায়ন - $yearStr"
    }

    fun resolvedRoutineTitle(cfg: AdmitCardConfig, settings: Settings): String {
        val custom = cfg.customRoutineTitle.trim()
        if (custom.isNotEmpty()) return custom
        return "${resolvedExamTitle(cfg, settings)} এর রুটিন"
    }

    fun routineSubjectLabel(sub: Sub): String = when (sub.key) {
        "BAN" -> "বাংলা"
        "ENG" -> "ইংরেজি"
        "MATH" -> "প্রাথমিক গণিত"
        "BGW" -> "বাংলাদেশ ও বিশ্বপরিচয়"
        "SCI" -> "প্রাথমিক বিজ্ঞান"
        "REL" -> "ধর্ম ও নৈতিক শিক্ষা"
        "SOC" -> "সামাজিক বিজ্ঞান ও বিজ্ঞান"
        "PE" -> "শারীরিক ও স্বাস্থ্য শিক্ষা"
        "ART" -> "শিল্পকলা"
        "MUSIC" -> "সংগীত"
        else -> sub.bn
    }

    fun defaultRoutineForClass(
        cls: Int,
        settings: Settings
    ): List<ExamRoutineEntry> {
        val allSubs = subjectsFor(cls, RELIGIONS.first(), settings)
        val writtenFirst = allSubs.filter { it.written > 0 }
        val selected = if (writtenFirst.isNotEmpty()) writtenFirst else allSubs
        // Arrange English -> Bangla -> BGW -> Math -> Rel -> Science if matching standard 6 subjects, else natural order
        val preferredOrder = listOf("ENG", "BAN", "BGW", "MATH", "REL", "SCI", "SOC")
        val ordered = selected.sortedBy { sub ->
            val idx = preferredOrder.indexOf(sub.key)
            if (idx >= 0) idx else preferredOrder.size
        }
        return ordered.map { sub ->
            ExamRoutineEntry(
                date = "",
                day = "",
                subject = routineSubjectLabel(sub)
            )
        }
    }

    /**
     * Parses DD/MM/YYYY, DD-MM-YYYY, or DD.MM.YYYY (with Bengali or English digits)
     * and returns the matching Bangla weekday name, or empty string if unparseable.
     */
    fun inferBanglaWeekday(rawDate: String): String {
        val ascii = fromBn(rawDate.trim())
        val parts = ascii.split('/', '-', '.')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (parts.size != 3) return ""
        val day = parts[0].toIntOrNull() ?: return ""
        val month = parts[1].toIntOrNull() ?: return ""
        val yearRaw = parts[2].toIntOrNull() ?: return ""
        val year = if (yearRaw < 100) 2000 + yearRaw else yearRaw
        if (month !in 1..12 || day !in 1..31 || year !in 1900..2100) return ""
        val cal = Calendar.getInstance().apply {
            isLenient = false
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
        }
        return try {
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "রবিবার"
                Calendar.MONDAY -> "সোমবার"
                Calendar.TUESDAY -> "মঙ্গলবার"
                Calendar.WEDNESDAY -> "বুধবার"
                Calendar.THURSDAY -> "বৃহস্পতিবার"
                Calendar.FRIDAY -> "শুক্রবার"
                Calendar.SATURDAY -> "শনিবার"
                else -> ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    fun buildPages(
        students: List<Student>,
        settings: Settings,
        config: AdmitCardConfig,
        routineRows: List<ExamRoutineEntry>,
        allowedClassIds: List<Int> = activeClassIds(settings)
    ): List<AdmitCardPageSpec> {
        val cls = config.selectedClass.takeIf { it in allowedClassIds }
            ?: allowedClassIds.firstOrNull()
            ?: return emptyList()

        val minRoll = config.rollFrom?.coerceAtLeast(1)
        val maxRoll = config.rollTo?.coerceAtLeast(1)
        val filtered = students.filter { st ->
            st.cls == cls &&
                (minRoll == null || st.roll >= minRoll) &&
                (maxRoll == null || st.roll <= maxRoll)
        }
        val ordered = when (config.rollOrder) {
            SeatPlanRollOrder.DESCENDING -> filtered.sortedWith(compareByDescending<Student> { it.roll }.thenBy { it.name })
            SeatPlanRollOrder.ASCENDING -> filtered.sortedWith(compareBy<Student> { it.roll }.thenBy { it.name })
        }

        val studentCards = ordered.map { st ->
            AdmitCardItem(
                studentName = st.name.ifBlank { "(নাম নেই)" },
                cls = st.cls,
                roll = st.roll,
                isBlank = false
            )
        }
        val blankCards = List(config.extraBlankCards.coerceIn(0, 40)) {
            AdmitCardItem(
                studentName = "",
                cls = cls,
                roll = null,
                isBlank = true
            )
        }
        val allCards = studentCards + blankCards
        if (allCards.isEmpty()) return emptyList()

        val cleanedRoutine = routineRows.filter {
            it.date.isNotBlank() || it.day.isNotBlank() || it.subject.isNotBlank()
        }
        val perPage = config.safeCardsPerPage
        val chunks = allCards.chunked(perPage)
        val totalPages = chunks.size.coerceAtLeast(1)

        return chunks.mapIndexed { idx, chunk ->
            AdmitCardPageSpec(
                pageNumber = idx + 1,
                totalPages = totalPages,
                items = chunk,
                routineRows = cleanedRoutine,
                config = config
            )
        }
    }
}

object AdmitCardLocalStore {
    private const val PREFS_NAME = "admit_card_prefs"
    private const val SIGNATURE_FILE = "admit_head_signature.png"

    fun loadRoutine(ctx: Context, cls: Int, settings: Settings): List<ExamRoutineEntry> {
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("routine_cls_$cls", null)
        if (!raw.isNullOrBlank()) {
            try {
                val arr = JSONArray(raw)
                val parsed = buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        add(
                            ExamRoutineEntry(
                                id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
                                date = o.optString("date"),
                                day = o.optString("day"),
                                subject = o.optString("subject")
                            )
                        )
                    }
                }
                if (parsed.isNotEmpty()) return parsed
            } catch (_: Exception) {
                // Fall back to default class subjects
            }
        }
        return AdmitCardPlanner.defaultRoutineForClass(cls, settings)
    }

    fun saveRoutine(ctx: Context, cls: Int, rows: List<ExamRoutineEntry>) {
        val arr = JSONArray()
        rows.forEach { r ->
            arr.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("date", r.date)
                    put("day", r.day)
                    put("subject", r.subject)
                }
            )
        }
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("routine_cls_$cls", arr.toString())
            .apply()
    }

    fun loadExamTime(ctx: Context): String {
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("exam_time", null) ?: "দুপুর ০১:০০-০৩:৩০ টা"
    }

    fun saveExamTime(ctx: Context, examTime: String) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("exam_time", examTime)
            .apply()
    }

    fun signatureFile(ctx: Context): File = File(ctx.filesDir, SIGNATURE_FILE)

    fun loadSignatureBitmap(ctx: Context): Bitmap? {
        val f = signatureFile(ctx)
        if (!f.exists() || f.length() <= 0L) return null
        return try {
            BitmapFactory.decodeFile(f.absolutePath)
        } catch (_: Exception) {
            null
        }
    }

    fun clearSignature(ctx: Context) {
        runCatching {
            val f = signatureFile(ctx)
            if (f.exists()) f.delete()
        }
    }

    /**
     * Reads picked image via ContentResolver, scales to compact stamp dimensions,
     * removes bright white paper background cleanly for crisp PDF stamping, and persists PNG.
     */
    fun saveSignatureFromUri(ctx: Context, uri: Uri): Bitmap? {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { inp ->
            BitmapFactory.decodeStream(inp, null, boundsOpts)
        }
        val srcW = boundsOpts.outWidth
        val srcH = boundsOpts.outHeight
        if (srcW <= 0 || srcH <= 0) return null

        var sample = 1
        while ((srcW / sample) > 960 || (srcH / sample) > 480) {
            sample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        val rawBmp = ctx.contentResolver.openInputStream(uri)?.use { inp ->
            BitmapFactory.decodeStream(inp, null, decodeOpts)
        } ?: return null

        val maxW = 480
        val maxH = 240
        val scale = min(maxW.toFloat() / rawBmp.width, maxH.toFloat() / rawBmp.height).coerceAtMost(1f)
        val targetW = (rawBmp.width * scale).toInt().coerceAtLeast(1)
        val targetH = (rawBmp.height * scale).toInt().coerceAtLeast(1)
        val scaled = if (targetW != rawBmp.width || targetH != rawBmp.height) {
            Bitmap.createScaledBitmap(rawBmp, targetW, targetH, true).also {
                if (it !== rawBmp) rawBmp.recycle()
            }
        } else {
            rawBmp
        }

        val cleaned = stripWhitePaperBackground(scaled)
        if (cleaned !== scaled) scaled.recycle()

        FileOutputStream(signatureFile(ctx)).use { out ->
            cleaned.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return cleaned
    }

    private fun stripWhitePaperBackground(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val px = pixels[i]
            val a = Color.alpha(px)
            if (a == 0) continue
            val r = Color.red(px)
            val g = Color.green(px)
            val b = Color.blue(px)
            val maxC = max(r, max(g, b))
            val minC = min(r, min(g, b))
            val spread = maxC - minC
            val lum = (r + g + b) / 3
            // Make near-white neutral paper pixels transparent while keeping ink crisp
            if (lum >= 225 && spread <= 28) {
                pixels[i] = Color.TRANSPARENT
            } else if (lum >= 195 && spread <= 32) {
                val nextA = ((225 - lum).coerceIn(0, 30) * a / 30).coerceIn(0, a)
                pixels[i] = Color.argb(nextA, r, g, b)
            }
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }
}
