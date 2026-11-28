package com.kgps.result

import android.graphics.Bitmap
import java.util.Calendar

enum class TestimonialType(
    val labelBn: String,
    val defaultBannerTitle: String,
    val defaultPreset: TestimonialBodyPreset
) {
    PROSHONGSHAPATRA(
        labelBn = "প্রশংসাপত্র",
        defaultBannerTitle = "প্রশংসাপত্র",
        defaultPreset = TestimonialBodyPreset.PASSED_CHARACTER
    ),
    PROTTAYONPATRA(
        labelBn = "প্রত্যয়নপত্র",
        defaultBannerTitle = "প্রত্যয়নপত্র",
        defaultPreset = TestimonialBodyPreset.STUDIED_CLASSIC
    )
}

enum class TestimonialBodyPreset(
    val labelBn: String,
    val bodyTemplate: String,
    val defaultClosing: String
) {
    PASSED_CHARACTER(
        labelBn = "প্রশংসাপত্র (উত্তীর্ণ ও চরিত্র)",
        bodyTemplate = "এই মর্মে প্রত্যয়ন ও প্রশংসা করা যাচ্ছে যে, {{NAME}}, পিতা: {{FATHER}}, মাতা: {{MOTHER}}, জন্মতারিখ: {{DOB}}, রোল নম্বর: {{ROLL}} এই বিদ্যালয়ে {{YEAR}} শিক্ষাবর্ষে {{CLASS}} শ্রেণিতে অধ্যয়ন করে সাফল্যের সাথে উত্তীর্ণ হয়েছে। অধ্যয়নকালে তার স্বভাব-চরিত্র ভালো ছিল এবং সে বিদ্যালয়ের নিয়ম-শৃঙ্খলা মেনে চলেছে।",
        defaultClosing = "আমি তার সর্বাঙ্গীণ সাফল্য ও উজ্জ্বল ভবিষ্যৎ কামনা করি।"
    ),
    STUDIED_CLASSIC(
        labelBn = "প্রত্যয়নপত্র (অধ্যয়ন করেছে)",
        bodyTemplate = "এই মর্মে প্রত্যয়ন করা যাচ্ছে যে, {{NAME}}, পিতা: {{FATHER}}, মাতা: {{MOTHER}}, জন্মতারিখ: {{DOB}}, রোল নম্বর: {{ROLL}} এই বিদ্যালয়ে {{YEAR}} শিক্ষাবর্ষে {{CLASS}} শ্রেণিতে অধ্যয়ন করেছে। তার স্বভাব চরিত্র ভালো।",
        defaultClosing = "আমি তার সর্বাঙ্গীণ সাফল্য কামনা করি।"
    ),
    CURRENTLY_STUDYING(
        labelBn = "অধ্যয়নরত প্রত্যয়ন",
        bodyTemplate = "এই মর্মে প্রত্যয়ন করা যাচ্ছে যে, {{NAME}}, পিতা: {{FATHER}}, মাতা: {{MOTHER}}, জন্মতারিখ: {{DOB}}, রোল নম্বর: {{ROLL}} এই বিদ্যালয়ে {{YEAR}} শিক্ষাবর্ষে {{CLASS}} শ্রেণিতে নিয়মিত অধ্যয়নরত আছে। তার স্বভাব চরিত্র ভালো।",
        defaultClosing = "আমি তার সর্বাঙ্গীণ মঙ্গল ও সাফল্য কামনা করি।"
    ),
    CUSTOM(
        labelBn = "নিজের মতো লিখুন",
        bodyTemplate = "এই মর্মে প্রত্যয়ন করা যাচ্ছে যে, {{NAME}}, পিতা: {{FATHER}}, মাতা: {{MOTHER}}, জন্মতারিখ: {{DOB}}, রোল নম্বর: {{ROLL}} এই বিদ্যালয়ে {{YEAR}} শিক্ষাবর্ষে {{CLASS}} শ্রেণিতে অধ্যয়ন করেছে। তার স্বভাব চরিত্র ভালো।",
        defaultClosing = "আমি তার সর্বাঙ্গীণ সাফল্য কামনা করি।"
    )
}

enum class TestimonialBorderStyle(val labelBn: String) {
    ORNAMENTAL_CLASSIC("ক্লাসিক নকশা"),
    DOUBLE_CLASSIC("ডাবল বর্ডার"),
    SIMPLE_CLEAN("সিম্পল বর্ডার"),
    NONE("বর্ডার নেই")
}

enum class TestimonialSideDesignMode(val labelBn: String) {
    FOUR_CORNERS("৪ কোণায় ডিজাইন"),
    FULL_FRAME("পুরো ফ্রেম/বর্ডার")
}

enum class TestimonialAssetSlot(
    val fileName: String,
    val labelBn: String,
    val stripWhiteBackground: Boolean,
    val maxDimensionPx: Int
) {
    LEFT_LOGO("testimonial_left_logo.png", "বাম লোগো (সিল)", false, 640),
    RIGHT_LOGO("testimonial_right_logo.png", "ডান লোগো (DPE/বিদ্যালয়)", false, 640),
    WATERMARK("testimonial_watermark.png", "জলছাপ লোগো", true, 960),
    SIDE_ORNAMENT("testimonial_side_ornament.png", "পাশের / কর্নার ডিজাইন", true, 1200)
}

data class TestimonialCustomAssets(
    val leftLogo: Bitmap? = null,
    val rightLogo: Bitmap? = null,
    val watermarkLogo: Bitmap? = null,
    val sideOrnament: Bitmap? = null
) {
    fun bitmapFor(slot: TestimonialAssetSlot): Bitmap? = when (slot) {
        TestimonialAssetSlot.LEFT_LOGO -> leftLogo
        TestimonialAssetSlot.RIGHT_LOGO -> rightLogo
        TestimonialAssetSlot.WATERMARK -> watermarkLogo
        TestimonialAssetSlot.SIDE_ORNAMENT -> sideOrnament
    }

    fun withUpdated(slot: TestimonialAssetSlot, bmp: Bitmap?): TestimonialCustomAssets = when (slot) {
        TestimonialAssetSlot.LEFT_LOGO -> copy(leftLogo = bmp)
        TestimonialAssetSlot.RIGHT_LOGO -> copy(rightLogo = bmp)
        TestimonialAssetSlot.WATERMARK -> copy(watermarkLogo = bmp)
        TestimonialAssetSlot.SIDE_ORNAMENT -> copy(sideOrnament = bmp)
    }
}

enum class TestimonialSerialMode(val labelBn: String) {
    AUTO_INCREMENT("ক্রমিক নম্বর"),
    BASE_PLUS_ROLL("Prefix + রোল"),
    BLANK("ফাঁকা রাখুন")
}

data class TestimonialStudentOverride(
    val fatherName: String = "",
    val motherName: String = "",
    val dob: String = ""
)

data class TestimonialConfig(
    val selectedClass: Int = 5,
    val selectedClasses: List<Int> = emptyList(),
    val rollOrder: SeatPlanRollOrder = SeatPlanRollOrder.DESCENDING,
    val rollFrom: Int? = null,
    val rollTo: Int? = null,
    val extraBlankCount: Int = 0,
    val testimonialType: TestimonialType = TestimonialType.PROSHONGSHAPATRA,
    val customBannerTitle: String = "",
    val bodyPreset: TestimonialBodyPreset = TestimonialBodyPreset.PASSED_CHARACTER,
    val customBodyTemplate: String = "",
    val customClosingLine: String = "",
    val issueDate: String = "",
    val sessionYear: String = "",
    val establishedYear: String = "",
    val customSchoolName: String = "",
    val customSchoolAddress: String = "",
    val customSignPlace: String = "",
    val showGovtHeader: Boolean = true,
    val govtLine1: String = "গণপ্রজাতন্ত্রী বাংলাদেশ সরকার",
    val govtLine2: String = "প্রাথমিক শিক্ষা অধিদপ্তর",
    val govtLine3: String = "প্রাথমিক ও গণশিক্ষা মন্ত্রণালয়",
    val showGovtSeal: Boolean = true,
    val showDpeLogo: Boolean = true,
    val showWatermark: Boolean = true,
    val showCounterfoil: Boolean = true,
    val borderStyle: TestimonialBorderStyle = TestimonialBorderStyle.ORNAMENTAL_CLASSIC,
    val sideDesignMode: TestimonialSideDesignMode = TestimonialSideDesignMode.FOUR_CORNERS,
    val sideDesignSizePt: Float = 88f,
    val logoSizePt: Float = 52f,
    val watermarkAlpha: Float = 0.11f,
    val watermarkUseRightLogo: Boolean = true,
    val serialMode: TestimonialSerialMode = TestimonialSerialMode.BASE_PLUS_ROLL,
    val serialStartOrPrefix: String = "২০২৬৫",
    val padRollTwoDigits: Boolean = true,
    val numeralMode: SeatPlanNumeralMode = SeatPlanNumeralMode.BENGALI,
    val bnFontId: String = "noto_serif",
    val enFontId: String = "times_new_roman",
    val marginInches: Float = 0.28f,
    val headTeacherTitle: String = "(প্রধান শিক্ষক)",
    val showSignatureImage: Boolean = true,
    val signatureScale: Float = 1.0f,
    val motherFieldKeyHint: String = "",
    val dobFieldKeyHint: String = ""
) {
    val resolvedSelectedClasses: List<Int>
        get() = if (selectedClasses.isNotEmpty()) selectedClasses.distinct() else listOf(selectedClass)

    val safeMarginInches: Float get() = marginInches.coerceIn(0.14f, 0.75f)
    val safeMarginPt: Float get() = safeMarginInches * 72f
    val safeSignatureScale: Float get() = signatureScale.coerceIn(0.35f, 2.5f)
    val safeSideDesignSizePt: Float get() = sideDesignSizePt.coerceIn(48f, 160f)
    val safeLogoSizePt: Float get() = logoSizePt.coerceIn(34f, 76f)
    val safeWatermarkAlpha: Float get() = watermarkAlpha.coerceIn(0.04f, 0.28f)
}

data class TestimonialItem(
    val studentId: String,
    val studentName: String,
    val fatherName: String,
    val motherName: String,
    val dateOfBirth: String,
    val cls: Int,
    val roll: Int?,
    val serialText: String,
    val sessionYear: String,
    val isBlank: Boolean = false
)

data class TestimonialPageSpec(
    val pageNumber: Int,
    val totalPages: Int,
    val item: TestimonialItem,
    val config: TestimonialConfig
)

sealed interface TestimonialBodyToken {
    data class Plain(val text: String) : TestimonialBodyToken
    data class Field(val value: String, val minWidthPt: Float) : TestimonialBodyToken
}

object TestimonialPlanner {

    private val MOTHER_KEYWORDS = listOf("মাতা", "মায়ের", "মায়ের", "mother")
    private val FATHER_KEYWORDS = listOf("পিতা", "বাবা", "father", "guardian")
    private val DOB_KEYWORDS = listOf("জন্ম", "dob", "birth")

    fun todayBanglaDateString(mode: SeatPlanNumeralMode = SeatPlanNumeralMode.BENGALI): String {
        val cal = Calendar.getInstance()
        val d = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val m = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val y = cal.get(Calendar.YEAR).toString()
        val formatted = "$d/$m/$y খ্রি."
        return formatDigits(formatted, mode)
    }

    fun formatDigits(raw: String, mode: SeatPlanNumeralMode): String = when (mode) {
        SeatPlanNumeralMode.BENGALI -> bn(raw)
        SeatPlanNumeralMode.ENGLISH -> fromBn(raw)
    }

    fun formatRoll(roll: Int?, cfg: TestimonialConfig): String {
        if (roll == null) return ""
        val base = if (cfg.padRollTwoDigits) roll.toString().padStart(2, '0') else roll.toString()
        return formatDigits(base, cfg.numeralMode)
    }

    fun resolvedBannerTitle(cfg: TestimonialConfig): String =
        cfg.customBannerTitle.trim().ifBlank { cfg.testimonialType.defaultBannerTitle }

    fun resolvedSchoolName(cfg: TestimonialConfig, settings: Settings): String =
        cfg.customSchoolName.trim().ifBlank {
            settings.school.trim().ifBlank { "প্রাথমিক বিদ্যালয়" }
        }

    fun resolvedSchoolAddress(cfg: TestimonialConfig, settings: Settings, students: List<Student>): String {
        val custom = cfg.customSchoolAddress.trim()
        if (custom.isNotEmpty()) return custom
        val fromSettings = settings.schoolAddress.trim()
        if (fromSettings.isNotEmpty()) return fromSettings
        val sample = students.firstOrNull { it.upazila.isNotBlank() || it.district.isNotBlank() }
        val upazila = sample?.upazila?.trim().orEmpty()
        val district = sample?.district?.trim().orEmpty()
        return when {
            upazila.isNotEmpty() && district.isNotEmpty() -> "উপজেলা: $upazila, জেলা: $district।"
            upazila.isNotEmpty() -> "উপজেলা: $upazila"
            district.isNotEmpty() -> "জেলা: $district"
            else -> ""
        }
    }

    fun resolvedSignPlace(cfg: TestimonialConfig, settings: Settings, students: List<Student>): String {
        val custom = cfg.customSignPlace.trim()
        if (custom.isNotEmpty()) return custom
        val sample = students.firstOrNull { it.upazila.isNotBlank() || it.district.isNotBlank() }
        val upazila = sample?.upazila?.trim().orEmpty()
        val district = sample?.district?.trim().orEmpty()
        if (upazila.isNotEmpty() && district.isNotEmpty()) {
            return "$upazila, $district।"
        }
        return resolvedSchoolAddress(cfg, settings, students)
    }

    fun resolvedIssueDate(cfg: TestimonialConfig): String {
        val raw = cfg.issueDate.trim().ifBlank { todayBanglaDateString(cfg.numeralMode) }
        return formatDigits(raw, cfg.numeralMode)
    }

    fun resolvedSessionYear(cfg: TestimonialConfig, settings: Settings): String {
        val raw = cfg.sessionYear.trim().ifBlank { settings.year.trim().ifBlank { "২০২৬" } }
        return formatDigits(raw, cfg.numeralMode)
    }

    fun resolvedBodyTemplate(cfg: TestimonialConfig): String =
        if (cfg.bodyPreset == TestimonialBodyPreset.CUSTOM && cfg.customBodyTemplate.isNotBlank()) {
            cfg.customBodyTemplate
        } else {
            cfg.bodyPreset.bodyTemplate
        }

    fun resolvedClosingLine(cfg: TestimonialConfig): String =
        cfg.customClosingLine.trim().ifBlank { cfg.bodyPreset.defaultClosing }

    fun findCustomFieldKey(
        settings: Settings,
        hintKey: String,
        keywords: List<String>
    ): String? {
        if (hintKey.isNotBlank() && settings.customFields.any { it.key == hintKey }) {
            return hintKey
        }
        return settings.customFields.firstOrNull { cf ->
            val labelLower = cf.label.lowercase()
            val keyLower = cf.key.lowercase()
            keywords.any { kw -> labelLower.contains(kw) || keyLower.contains(kw) }
        }?.key
    }

    private fun resolveExtraByKeywords(
        student: Student,
        settings: Settings,
        hintKey: String,
        keywords: List<String>
    ): String {
        if (hintKey.isNotBlank()) {
            val v = student.x[hintKey]?.trim().orEmpty()
            if (v.isNotEmpty()) return v
        }
        settings.customFields.forEach { cf ->
            val labelLower = cf.label.lowercase()
            val keyLower = cf.key.lowercase()
            if (keywords.any { kw -> labelLower.contains(kw) || keyLower.contains(kw) }) {
                val v = student.x[cf.key]?.trim().orEmpty()
                if (v.isNotEmpty()) return v
            }
        }
        student.x.entries.forEach { (k, v) ->
            val keyLower = k.lowercase()
            if (keywords.any { kw -> keyLower.contains(kw) } && v.trim().isNotEmpty()) {
                return v.trim()
            }
        }
        return ""
    }

    fun resolveFatherName(
        student: Student,
        settings: Settings,
        override: TestimonialStudentOverride? = null
    ): String {
        val ov = override?.fatherName?.trim().orEmpty()
        if (ov.isNotEmpty()) return ov
        val direct = student.guardian.trim()
        if (direct.isNotEmpty()) return direct
        return resolveExtraByKeywords(student, settings, "", FATHER_KEYWORDS)
    }

    fun resolveMotherName(
        student: Student,
        settings: Settings,
        hintKey: String = "",
        override: TestimonialStudentOverride? = null
    ): String {
        val ov = override?.motherName?.trim().orEmpty()
        if (ov.isNotEmpty()) return ov
        return resolveExtraByKeywords(student, settings, hintKey, MOTHER_KEYWORDS)
    }

    fun resolveDob(
        student: Student,
        settings: Settings,
        hintKey: String = "",
        mode: SeatPlanNumeralMode = SeatPlanNumeralMode.BENGALI,
        override: TestimonialStudentOverride? = null
    ): String {
        val ov = override?.dob?.trim().orEmpty()
        val raw = if (ov.isNotEmpty()) ov else resolveExtraByKeywords(student, settings, hintKey, DOB_KEYWORDS)
        if (raw.isBlank()) return ""
        val withSuffix = if (raw.contains("খ্রি") || raw.contains("AD", ignoreCase = true)) {
            raw
        } else if (mode == SeatPlanNumeralMode.BENGALI) {
            "$raw খ্রি."
        } else {
            raw
        }
        return formatDigits(withSuffix, mode)
    }

    fun resolveSerial(
        indexZeroBased: Int,
        roll: Int?,
        cfg: TestimonialConfig
    ): String {
        val rawBase = fromBn(cfg.serialStartOrPrefix.trim()).filter { it.isDigit() }
        val result = when (cfg.serialMode) {
            TestimonialSerialMode.BLANK -> ""
            TestimonialSerialMode.AUTO_INCREMENT -> {
                val startNum = rawBase.toLongOrNull() ?: 2026001L
                val valNum = startNum + indexZeroBased
                val padded = if (rawBase.length > valNum.toString().length) {
                    valNum.toString().padStart(rawBase.length, '0')
                } else {
                    valNum.toString()
                }
                padded
            }
            TestimonialSerialMode.BASE_PLUS_ROLL -> {
                val prefix = rawBase.ifBlank { "20265" }
                if (roll == null) {
                    ""
                } else {
                    val rStr = if (cfg.padRollTwoDigits) roll.toString().padStart(2, '0') else roll.toString()
                    "$prefix$rStr"
                }
            }
        }
        return if (result.isBlank()) "" else formatDigits(result, cfg.numeralMode)
    }

    fun parseBodyTokens(
        item: TestimonialItem,
        cfg: TestimonialConfig,
        settings: Settings
    ): List<TestimonialBodyToken> {
        val template = resolvedBodyTemplate(cfg)
        val classLabel = classShortBn(item.cls, settings).ifBlank { classNameBn(item.cls, settings) }
        val formattedRoll = if (item.isBlank) "" else formatRoll(item.roll, cfg)
        val yearLabel = if (item.isBlank) "" else item.sessionYear

        val replacements = mapOf(
            "{{NAME}}" to (item.studentName to 175f),
            "{{FATHER}}" to (item.fatherName to 145f),
            "{{MOTHER}}" to (item.motherName to 145f),
            "{{DOB}}" to (item.dateOfBirth to 92f),
            "{{ROLL}}" to (formattedRoll to 46f),
            "{{YEAR}}" to (yearLabel to 56f),
            "{{CLASS}}" to (classLabel to 54f)
        )

        val pattern = Regex("\\{\\{(NAME|FATHER|MOTHER|DOB|ROLL|YEAR|CLASS)\\}\\}")
        val tokens = mutableListOf<TestimonialBodyToken>()
        var cursor = 0
        pattern.findAll(template).forEach { match ->
            if (match.range.first > cursor) {
                val plain = template.substring(cursor, match.range.first)
                if (plain.isNotEmpty()) tokens.add(TestimonialBodyToken.Plain(plain))
            }
            val (fieldVal, minW) = replacements[match.value] ?: ("" to 80f)
            tokens.add(TestimonialBodyToken.Field(value = fieldVal, minWidthPt = minW))
            cursor = match.range.last + 1
        }
        if (cursor < template.length) {
            val tail = template.substring(cursor)
            if (tail.isNotEmpty()) tokens.add(TestimonialBodyToken.Plain(tail))
        }
        return tokens
    }

    fun buildPages(
        students: List<Student>,
        settings: Settings,
        config: TestimonialConfig,
        overrides: Map<String, TestimonialStudentOverride> = emptyMap(),
        allowedClassIds: List<Int> = activeClassIds(settings),
        serialOffset: Int = 0
    ): List<TestimonialPageSpec> {
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

        val sessionYear = resolvedSessionYear(config, settings)
        val items = mutableListOf<TestimonialItem>()

        ordered.forEachIndexed { idx, st ->
            val ov = overrides[st.id]
            items.add(
                TestimonialItem(
                    studentId = st.id,
                    studentName = st.name.ifBlank { "(নাম নেই)" },
                    fatherName = resolveFatherName(st, settings, ov),
                    motherName = resolveMotherName(st, settings, config.motherFieldKeyHint, ov),
                    dateOfBirth = resolveDob(st, settings, config.dobFieldKeyHint, config.numeralMode, ov),
                    cls = st.cls,
                    roll = st.roll,
                    serialText = resolveSerial(serialOffset + idx, st.roll, config),
                    sessionYear = sessionYear,
                    isBlank = false
                )
            )
        }

        val blankCount = config.extraBlankCount.coerceIn(0, 100)
        repeat(blankCount) { bIdx ->
            items.add(
                TestimonialItem(
                    studentId = "blank_${cls}_$bIdx",
                    studentName = "",
                    fatherName = "",
                    motherName = "",
                    dateOfBirth = "",
                    cls = cls,
                    roll = null,
                    serialText = resolveSerial(serialOffset + ordered.size + bIdx, null, config),
                    sessionYear = sessionYear,
                    isBlank = true
                )
            )
        }

        if (items.isEmpty()) return emptyList()
        val total = items.size
        return items.mapIndexed { idx, item ->
            TestimonialPageSpec(
                pageNumber = idx + 1,
                totalPages = total,
                item = item,
                config = config
            )
        }
    }

    fun buildMultiClassPages(
        students: List<Student>,
        settings: Settings,
        baseConfig: TestimonialConfig,
        selectedClasses: List<Int>,
        overrides: Map<String, TestimonialStudentOverride> = emptyMap(),
        allowedClassIds: List<Int> = activeClassIds(settings)
    ): List<TestimonialPageSpec> {
        val orderedClasses = allowedClassIds.filter { it in selectedClasses }.ifEmpty {
            selectedClasses.filter { it in allowedClassIds }.distinct()
        }
        if (orderedClasses.isEmpty()) return emptyList()

        val combined = mutableListOf<TestimonialPageSpec>()
        orderedClasses.forEach { cls ->
            val clsConfig = baseConfig.copy(selectedClass = cls)
            val clsPages = buildPages(
                students = students,
                settings = settings,
                config = clsConfig,
                overrides = overrides,
                allowedClassIds = allowedClassIds,
                serialOffset = combined.size
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
