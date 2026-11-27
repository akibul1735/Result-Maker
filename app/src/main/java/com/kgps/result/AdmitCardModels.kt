package com.kgps.result

import java.util.Calendar
import java.util.TimeZone
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

enum class AdmitBorderStyle(val labelBn: String) {
    DASHED("ড্যাশড"),
    SOLID("সলিড"),
    DOUBLE("ডাবল"),
    THICK_THIN("মোটা-চিকন"),
    CORNER_ACCENT("কোনা সজ্জা"),
    NONE("বর্ডার নেই")
}

enum class AdmitDashStyle(val labelBn: String, val intervals: FloatArray) {
    NORMAL("সাধারণ ড্যাশ", floatArrayOf(4.2f, 3.0f)),
    SHORT("ছোট ড্যাশ", floatArrayOf(2.2f, 1.8f)),
    LONG("লম্বা ড্যাশ", floatArrayOf(8.0f, 3.5f)),
    DOTTED("ডটেড (বিন্দু)", floatArrayOf(1.2f, 2.3f)),
    DASH_DOT("ড্যাশ-ডট", floatArrayOf(6.5f, 2.4f, 1.4f, 2.4f))
}

data class ExamRoutineEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: String = "",
    val day: String = "",
    val subjects: List<String> = listOf("")
) {
    constructor(date: String, day: String, subject: String) : this(
        id = UUID.randomUUID().toString(),
        date = date,
        day = day,
        subjects = parseSubjects(subject)
    )

    val nonEmptySubjects: List<String>
        get() = subjects.map { it.trim() }.filter { it.isNotEmpty() }

    val subject: String
        get() = nonEmptySubjects.joinToString(" / ")

    companion object {
        fun parseSubjects(raw: String): List<String> {
            val parts = raw.split(Regex("\\s*/\\s*|\\s+অথবা\\s+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            return if (parts.isEmpty()) listOf("") else parts
        }
    }
}

data class BaseRoutineTemplate(
    val id: String,
    val name: String,
    val examTimeLabel: String = "দুপুর ০১:০০-০৩:৩০ টা",
    val rows: List<ExamRoutineEntry> = emptyList()
)

data class AdmitCardConfig(
    val selectedClass: Int = 5,
    val rollOrder: SeatPlanRollOrder = SeatPlanRollOrder.DESCENDING,
    val rollFrom: Int? = null,
    val rollTo: Int? = null,
    val term: Int = 3,
    val cardsPerPage: Int = 4,
    val marginInches: Float = 0.28f,
    val verticalGapPt: Float = 10f,
    val bnFontId: String = "kalpurush",
    val enFontId: String = "times_new_roman",
    val borderStyle: AdmitBorderStyle = AdmitBorderStyle.DASHED,
    val dashStyle: AdmitDashStyle = AdmitDashStyle.NORMAL,
    val borderWeight: SeatPlanBorderWeight = SeatPlanBorderWeight.THIN,
    val cornerRadiusPt: Float = 5f,
    val showCutLines: Boolean = true,
    val showCenterDivider: Boolean = true,
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
    val showSignatureLine: Boolean = false,
    val signatureLabelToLineGapPt: Float = 4.2f,
    val signatureLineToImageGapPt: Float = 4.8f,
    val showRoutineTable: Boolean = true,
    val includeBlankRoutineRow: Boolean = false,
    val routineNote: String = "",
    val showSignatureImage: Boolean = true,
    val signatureScale: Float = 1.0f,
    val extraBlankCards: Int = 0,
    val headerToInfoGapPt: Float = 15.5f,
    val studentInfoOffsetXPt: Float = 0f,
    val schoolFontPt: Float = 10.0f,
    val headerMetaFontPt: Float = 9.0f,
    val headerBold: Boolean = false,
    val badgeFontPt: Float = 10.0f,
    val badgeBold: Boolean = true,
    val badgeUnderline: Boolean = true,
    val studentInfoFontPt: Float = 9.5f,
    val studentLabelBold: Boolean = true,
    val studentValueBold: Boolean = false,
    val routineHeaderFontPt: Float = 8.5f,
    val routineHeaderBold: Boolean = false,
    val routineBodyFontPt: Float = 8.5f,
    val routineBodyBold: Boolean = false,
    val signatureTitleFontPt: Float = 8.5f,
    val signatureTitleBold: Boolean = false
) {
    val safeCardsPerPage: Int get() = cardsPerPage.coerceIn(1, 6)
    val safeMarginInches: Float get() = marginInches.coerceIn(0.08f, 1.25f)
    val safeMarginPt: Float get() = safeMarginInches * 72f
    val safeVerticalGapPt: Float get() = verticalGapPt.coerceIn(0f, 64f)
    val safeCornerRadiusPt: Float get() = cornerRadiusPt.coerceIn(0f, 36f)
    val safeSignatureScale: Float get() = signatureScale.coerceIn(0.35f, 2.5f)
    val safeSignatureLabelToLineGapPt: Float get() = signatureLabelToLineGapPt.coerceIn(0f, 24f)
    val safeSignatureLineToImageGapPt: Float get() = signatureLineToImageGapPt.coerceIn(0f, 24f)
    val safeHeaderToInfoGapPt: Float get() = headerToInfoGapPt.coerceIn(0f, 64f)
    val safeStudentInfoOffsetXPt: Float get() = studentInfoOffsetXPt.coerceIn(-8f, 110f)
    val safeSchoolFontPt: Float get() = schoolFontPt.coerceIn(5f, 24f)
    val safeHeaderMetaFontPt: Float get() = headerMetaFontPt.coerceIn(5f, 24f)
    val safeBadgeFontPt: Float get() = badgeFontPt.coerceIn(5f, 24f)
    val safeStudentInfoFontPt: Float get() = studentInfoFontPt.coerceIn(5f, 24f)
    val safeRoutineHeaderFontPt: Float get() = routineHeaderFontPt.coerceIn(4.5f, 20f)
    val safeRoutineBodyFontPt: Float get() = routineBodyFontPt.coerceIn(4.5f, 20f)
    val safeSignatureTitleFontPt: Float get() = signatureTitleFontPt.coerceIn(4.5f, 20f)
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

    val EXAM_TIME_PRESETS = listOf(
        "১০:০০-১১:৩০ টা",
        "১০:০০-১২:৩০ টা",
        "১১:৩০-০১:০০ টা",
        "০১:০০-০৩:০০ টা",
        "০১:০০-০৩:৩০ টা"
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

    fun defaultExamTimeForClass(cls: Int, settings: Settings): String =
        if (syllabusTierForClass(cls, settings) <= 2) {
            "সকাল ১০:০০-১১:৩০ টা"
        } else {
            "দুপুর ০১:০০-০৩:৩০ টা"
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

    fun classSubjectLabelsFor(cls: Int, settings: Settings): List<String> {
        val allSubs = subjectsFor(cls, RELIGIONS.first(), settings)
        val preferredOrder = listOf("ENG", "BAN", "BGW", "MATH", "REL", "SCI", "SOC", "PE", "ART", "MUSIC")
        return allSubs
            .sortedBy { sub ->
                val idx = preferredOrder.indexOf(sub.key)
                if (idx >= 0) idx else preferredOrder.size
            }
            .map { routineSubjectLabel(it) }
            .distinct()
    }

    fun allSchoolSubjectLabels(settings: Settings): List<String> {
        val out = linkedSetOf<String>()
        activeClassIds(settings).forEach { cls ->
            out.addAll(classSubjectLabelsFor(cls, settings))
        }
        return out.toList()
    }

    fun defaultRoutineForClass(
        cls: Int,
        settings: Settings
    ): List<ExamRoutineEntry> {
        val allSubs = subjectsFor(cls, RELIGIONS.first(), settings)
        val writtenFirst = allSubs.filter { it.written > 0 }
        val selected = if (writtenFirst.isNotEmpty()) writtenFirst else allSubs
        val preferredOrder = listOf("ENG", "BAN", "BGW", "MATH", "REL", "SCI", "SOC")
        val ordered = selected.sortedBy { sub ->
            val idx = preferredOrder.indexOf(sub.key)
            if (idx >= 0) idx else preferredOrder.size
        }
        return ordered.map { sub ->
            ExamRoutineEntry(
                date = "",
                day = "",
                subjects = listOf(routineSubjectLabel(sub))
            )
        }
    }

    fun defaultBaseRoutines(settings: Settings): List<BaseRoutineTemplate> = listOf(
        BaseRoutineTemplate(
            id = "base_1",
            name = "মূল বেস ১ (৩য়–৫ম)",
            examTimeLabel = "দুপুর ০১:০০-০৩:৩০ টা",
            rows = defaultRoutineForClass(5, settings)
        ),
        BaseRoutineTemplate(
            id = "base_2",
            name = "বেস ২ (প্রাক–২য়)",
            examTimeLabel = "সকাল ১০:০০-১১:৩০ টা",
            rows = defaultRoutineForClass(2, settings)
        )
    )

    private fun canonicalKey(label: String): String {
        val norm = label.trim()
            .replace("য়", "য়")
            .replace("ড়", "ড়")
            .lowercase()
        return when {
            norm.startsWith("বাংলাদেশ") -> "BGW"
            norm == "বাংলা" -> "BAN"
            norm == "ইংরেজি" || norm == "english" -> "ENG"
            norm.contains("গণিত") -> "MATH"
            norm.contains("সামাজিক বিজ্ঞান") -> "SOC"
            norm.contains("বিজ্ঞান") -> "SCI"
            norm.contains("ধর্ম") -> "REL"
            norm.contains("শারীরিক") || norm.contains("স্বাস্থ্য") -> "PE"
            norm.contains("শিল্পকলা") || norm.contains("চারু") -> "ART"
            norm.contains("সংগীত") -> "MUSIC"
            else -> norm
        }
    }

    /**
     * Resolves a Base Routine into a class-specific routine:
     * - Handles multiple subjects on the same day AND "অথবা (/)" alternatives.
     * - Automatically picks the subject(s) belonging to [targetCls] when curriculum differs,
     *   omitting days whose subjects belong exclusively to other class tiers,
     *   while preserving custom non-curriculum subjects cleanly.
     */
    fun resolveRoutineForClass(
        baseRows: List<ExamRoutineEntry>,
        targetCls: Int,
        settings: Settings,
        matchClassSubjects: Boolean = true
    ): List<ExamRoutineEntry> {
        if (!matchClassSubjects) {
            return baseRows.map { row ->
                row.copy(
                    id = UUID.randomUUID().toString(),
                    subjects = row.subjects.ifEmpty { listOf("") }
                )
            }
        }

        val targetSubs = subjectsFor(targetCls, RELIGIONS.first(), settings)
        val targetMap = targetSubs.associate { sub ->
            sub.key to routineSubjectLabel(sub)
        }
        val allOtherKeys = activeClassIds(settings)
            .flatMap { c -> subjectsFor(c, RELIGIONS.first(), settings).map { it.key } }
            .toSet() - targetMap.keys

        return baseRows.mapNotNull { row ->
            val rawSlots = row.nonEmptySubjects
            if (rawSlots.isEmpty()) {
                return@mapNotNull row.copy(id = UUID.randomUUID().toString(), subjects = listOf(""))
            }
            val resolvedSubjects = linkedSetOf<String>()
            var sawAnyOtherTierCurriculum = false

            rawSlots.forEach { slot ->
                val alternatives = slot.split(Regex("\\s*/\\s*|\\s+অথবা\\s+"))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                alternatives.forEach { cand ->
                    val key = canonicalKey(cand)
                    val matchedTarget = targetMap[key]
                    if (matchedTarget != null) {
                        resolvedSubjects.add(matchedTarget)
                    } else if (key in allOtherKeys) {
                        sawAnyOtherTierCurriculum = true
                    } else {
                        // Custom user subject (e.g., মৌখিক, সাধারণ জ্ঞান) — always keep
                        resolvedSubjects.add(cand)
                    }
                }
            }

            if (resolvedSubjects.isEmpty() && sawAnyOtherTierCurriculum) {
                null
            } else {
                ExamRoutineEntry(
                    id = UUID.randomUUID().toString(),
                    date = row.date,
                    day = row.day,
                    subjects = if (resolvedSubjects.isEmpty()) listOf("") else resolvedSubjects.toList()
                )
            }
        }
    }

    fun formatDateAndDayFromUtcMillis(utcMillis: Long): Pair<String, String> {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        val d = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val m = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val y = cal.get(Calendar.YEAR).toString()
        val dateBn = bn("$d/$m/$y")
        val dayBn = weekdayFromCalendar(cal)
        return dateBn to dayBn
    }

    fun parseDateToUtcMillis(rawDate: String): Long? {
        val ascii = fromBn(rawDate.trim())
        val parts = ascii.split('/', '-', '.')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (parts.size != 3) return null
        val day = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val yearRaw = parts[2].toIntOrNull() ?: return null
        val year = if (yearRaw < 100) 2000 + yearRaw else yearRaw
        if (month !in 1..12 || day !in 1..31 || year !in 1900..2100) return null
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            isLenient = false
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
        }
        return try {
            cal.timeInMillis
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parses DD/MM/YYYY, DD-MM-YYYY, or DD.MM.YYYY (with Bengali or English digits)
     * and returns the matching Bangla weekday name, or empty string if unparseable.
     */
    fun inferBanglaWeekday(rawDate: String): String {
        val millis = parseDateToUtcMillis(rawDate) ?: return ""
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = millis
        }
        return weekdayFromCalendar(cal)
    }

    private fun weekdayFromCalendar(cal: Calendar): String = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> "রবিবার"
        Calendar.MONDAY -> "সোমবার"
        Calendar.TUESDAY -> "মঙ্গলবার"
        Calendar.WEDNESDAY -> "বুধবার"
        Calendar.THURSDAY -> "বৃহস্পতিবার"
        Calendar.FRIDAY -> "শুক্রবার"
        Calendar.SATURDAY -> "শনিবার"
        else -> ""
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
        val blankCards = List(config.extraBlankCards.coerceIn(0, 100)) {
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

    fun buildMultiClassPages(
        students: List<Student>,
        settings: Settings,
        baseConfig: AdmitCardConfig,
        selectedClasses: List<Int>,
        routineProvider: (Int) -> List<ExamRoutineEntry>,
        examTimeProvider: (Int) -> String,
        allowedClassIds: List<Int> = activeClassIds(settings)
    ): List<AdmitCardPageSpec> {
        val orderedClasses = allowedClassIds.filter { it in selectedClasses }.ifEmpty {
            selectedClasses.filter { it in allowedClassIds }.distinct()
        }
        if (orderedClasses.isEmpty()) return emptyList()

        val combined = mutableListOf<AdmitCardPageSpec>()
        orderedClasses.forEach { cls ->
            val clsConfig = baseConfig.copy(
                selectedClass = cls,
                examTimeLabel = examTimeProvider(cls).ifBlank { defaultExamTimeForClass(cls, settings) }
            )
            val clsPages = buildPages(
                students = students,
                settings = settings,
                config = clsConfig,
                routineRows = routineProvider(cls),
                allowedClassIds = allowedClassIds
            )
            combined.addAll(clsPages)
        }

        val total = combined.size
        if (total == 0) return emptyList()
        return combined.mapIndexed { idx, page ->
            page.copy(pageNumber = idx + 1, totalPages = total)
        }
    }
}
