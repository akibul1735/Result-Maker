package com.kgps.result

enum class BlankColFill {
    NONE,
    ROLL,
    NAME
}

enum class BlankColAlign {
    LEFT,
    CENTER
}

enum class BlankSheetOrientation(val labelBn: String) {
    PORTRAIT("খাড়া A4"),
    LANDSCAPE("আড়াআড়ি A4")
}

enum class BlankNumeralStyle(val shortLabelBn: String, val labelBn: String) {
    BENGALI("বাংলা (১২৩)", "বাংলা সংখ্যা (১, ২, ৩)"),
    ENGLISH("English (123)", "ইংরেজি সংখ্যা (1, 2, 3)")
}

data class BlankSheetColumn(
    val id: String,
    val titleBn: String,
    val widthWeight: Float = 1f,
    val fillWith: BlankColFill = BlankColFill.NONE,
    val align: BlankColAlign = BlankColAlign.CENTER,
    val compKey: String? = null,
    val isCustom: Boolean = false
)

enum class BlankSheetPreset(val labelBn: String, val columnIds: List<String>) {
    FULL_MARKS(
        "পূর্ণাঙ্গ নম্বর",
        listOf("ROLL", "NAME", "TB", "ACT", "LANG", "CT", "CONT", "WR", "TOTAL")
    ),
    COMPACT_MARKS(
        "সংক্ষিপ্ত নম্বর",
        listOf("ROLL", "NAME", "CONT", "WR", "TOTAL", "REMARK")
    ),
    ROLL_NAME_TOTAL(
        "সহজ মোট",
        listOf("ROLL", "NAME", "TOTAL", "REMARK")
    ),
    ATTENDANCE_SIGN(
        "হাজিরা/স্বাক্ষর",
        listOf("ROLL", "NAME", "ATT", "REMARK", "SIGN")
    )
}

data class BlankSubjectOption(
    val key: String?,
    val labelBn: String
)

object BlankSheetCatalog {
    const val ALL_SUBJECTS_KEY = "__ALL__"

    val ALL_COLUMNS: List<BlankSheetColumn> = listOf(
        BlankSheetColumn("ROLL", "রোল", widthWeight = 0.68f, fillWith = BlankColFill.ROLL, align = BlankColAlign.CENTER),
        BlankSheetColumn("NAME", "নাম", widthWeight = 2.40f, fillWith = BlankColFill.NAME, align = BlankColAlign.LEFT),
        BlankSheetColumn("TB", "পাঠ্যবই", widthWeight = 0.90f, compKey = "TB"),
        BlankSheetColumn("ACT", "কাজ", widthWeight = 0.80f, compKey = "ACT"),
        BlankSheetColumn("LANG", "ভাষা", widthWeight = 0.80f, compKey = "LANG"),
        BlankSheetColumn("CT", "অভীক্ষা", widthWeight = 0.90f, compKey = "CT"),
        BlankSheetColumn("CONT", "ধারাবাহিক", widthWeight = 1.05f, compKey = "CONT"),
        BlankSheetColumn("WR", "লিখিত", widthWeight = 0.88f, compKey = "WR"),
        BlankSheetColumn("TOTAL", "মোট", widthWeight = 0.88f, compKey = "TOTAL"),
        BlankSheetColumn("GRADE", "গ্রেড", widthWeight = 0.78f),
        BlankSheetColumn("GP", "জিপি", widthWeight = 0.72f),
        BlankSheetColumn("MERIT", "মেধাক্রম", widthWeight = 0.88f),
        BlankSheetColumn("ATT", "উপস্থিতি", widthWeight = 0.90f),
        BlankSheetColumn("REMARK", "মন্তব্য", widthWeight = 1.50f, align = BlankColAlign.LEFT),
        BlankSheetColumn("SIGN", "স্বাক্ষর", widthWeight = 1.00f)
    )

    val DEFAULT_COLUMNS: List<BlankSheetColumn> =
        BlankSheetPreset.COMPACT_MARKS.columnIds.mapNotNull { id -> ALL_COLUMNS.find { it.id == id } }

    val SUBJECT_OPTIONS: List<BlankSubjectOption> = listOf(
        BlankSubjectOption(ALL_SUBJECTS_KEY, "সব বিষয় (একসাথে সব)"),
        BlankSubjectOption("BAN", "বাংলা"),
        BlankSubjectOption("ENG", "ইংরেজি"),
        BlankSubjectOption("MATH", "গণিত"),
        BlankSubjectOption("SOC", "সমাজ ও বিজ্ঞান"),
        BlankSubjectOption("SCI", "বিজ্ঞান"),
        BlankSubjectOption("BGW", "বাংলাদেশ ও বিশ্বপরিচয়"),
        BlankSubjectOption("REL", "ধর্ম ও নৈতিক শিক্ষা"),
        BlankSubjectOption("PE", "শারীরিক শিক্ষা"),
        BlankSubjectOption("ART", "শিল্পকলা"),
        BlankSubjectOption(null, "সাধারণ / কাস্টম")
    )

    fun byId(id: String): BlankSheetColumn? = ALL_COLUMNS.find { it.id == id }

    fun presetColumns(preset: BlankSheetPreset): List<BlankSheetColumn> =
        preset.columnIds.mapNotNull { byId(it) }

    fun createCustomColumn(title: String): BlankSheetColumn {
        val clean = title.trim()
        val weight = when {
            clean.length >= 12 -> 1.45f
            clean.length >= 7 -> 1.15f
            else -> 0.95f
        }
        return BlankSheetColumn(
            id = "CUSTOM_${System.nanoTime()}_${clean.hashCode()}",
            titleBn = clean,
            widthWeight = weight,
            fillWith = BlankColFill.NONE,
            align = BlankColAlign.CENTER,
            compKey = null,
            isCustom = true
        )
    }

    fun formatDigits(raw: String, style: BlankNumeralStyle): String = when (style) {
        BlankNumeralStyle.BENGALI -> bn(fromBn(raw))
        BlankNumeralStyle.ENGLISH -> fromBn(raw)
    }

    fun formatInt(value: Int, style: BlankNumeralStyle, padTwo: Boolean = false): String {
        val en = if (padTwo) value.toString().padStart(2, '0') else value.toString()
        return if (style == BlankNumeralStyle.BENGALI) bn(en) else en
    }

    fun subjectTitleFor(subjectKey: String?, customTitle: String): String {
        val custom = customTitle.trim()
        if (subjectKey == null) return custom
        if (subjectKey == ALL_SUBJECTS_KEY) return "সব বিষয়"
        return SUBJECT_OPTIONS.find { it.key == subjectKey }?.labelBn ?: custom
    }

    fun shortSubjectTitleForSub(sub: Sub): String = when (sub.key) {
        "SOC" -> "সমাজ ও বিজ্ঞান"
        "REL" -> "ধর্ম ও নৈতিক শিক্ষা"
        "PE" -> "শারীরিক শিক্ষা"
        else -> SUBJECT_OPTIONS.find { it.key == sub.key }?.labelBn ?: sub.bn.substringBefore(" (")
    }

    fun termLabelFor(
        term: Int,
        customTermTitle: String = ""
    ): String = when (term) {
        1, 2, 3 -> termNameBn(term)
        0 -> "বার্ষিক মূল্যায়ন"
        -1 -> customTermTitle.trim()
        else -> ""
    }

    fun resolveMaxHint(cls: Int, subjectKey: String?, compKey: String?): Int? {
        if (subjectKey.isNullOrBlank() || subjectKey == ALL_SUBJECTS_KEY || compKey.isNullOrBlank()) return null
        val sub = subjectsFor(cls, "ইসলাম").find { it.key == subjectKey }
            ?: subjectsFor(3, "ইসলাম").find { it.key == subjectKey }
            ?: return null
        val maxVal = when (compKey) {
            "CONT" -> sub.comps.sumOf { it.max }
            "WR" -> sub.written
            "TOTAL" -> sub.max
            else -> sub.comps.find { it.key == compKey }?.max ?: 0
        }
        return maxVal.takeIf { it > 0 }
    }
}

data class BlankSheetConfig(
    val selectedClass: Int? = null, // null = সব শ্রেণি
    val subjectKey: String? = "BAN", // BlankSheetCatalog.ALL_SUBJECTS_KEY = সব বিষয়
    val customSubjectTitle: String = "",
    val term: Int = 1, // 1..3, 0 = বার্ষিক, -1 = কাস্টম/ফাঁকা
    val customTermTitle: String = "",
    val columns: List<BlankSheetColumn> = BlankSheetCatalog.DEFAULT_COLUMNS,
    val rowsPerPage: Int = 20,
    val orientation: BlankSheetOrientation = BlankSheetOrientation.PORTRAIT,
    val numeralStyle: BlankNumeralStyle = BlankNumeralStyle.BENGALI,
    val autoFillRollAndName: Boolean = true,
    val showSubjectMaxInHeader: Boolean = true,
    val extraBlankRowsPerClass: Int = 0,
    val fillRemainingPageRows: Boolean = false
)

data class BlankSheetRow(
    val rollText: String,
    val nameText: String
)

data class BlankSheetPageSpec(
    val cls: Int,
    val classTitleBn: String,
    val subjectTitleBn: String,
    val termTitleBn: String,
    val classPageIndex: Int,
    val classTotalPages: Int,
    val globalPageNumber: Int,
    val totalPages: Int,
    val totalStudentsInClass: Int,
    val rowsPerPageCapacity: Int,
    val rows: List<BlankSheetRow>,
    val columns: List<BlankSheetColumn>,
    val showSubjectMaxInHeader: Boolean,
    val subjectKey: String?,
    val numeralStyle: BlankNumeralStyle = BlankNumeralStyle.BENGALI
)

object BlankSheetPlanner {

    fun buildPages(
        students: List<Student>,
        settings: Settings,
        config: BlankSheetConfig,
        allowedClassIds: List<Int>
    ): List<BlankSheetPageSpec> {
        val safeCols = config.columns.ifEmpty { BlankSheetCatalog.DEFAULT_COLUMNS }
        val perPage = config.rowsPerPage.coerceIn(5, 50)
        val extraBlank = config.extraBlankRowsPerClass.coerceIn(0, 20)
        val termTitle = BlankSheetCatalog.termLabelFor(config.term, config.customTermTitle)
        val isAllSubjects = config.subjectKey == BlankSheetCatalog.ALL_SUBJECTS_KEY

        val effectiveAllowed = allowedClassIds.distinct().sorted()
        val targetClasses = if (config.selectedClass != null) {
            listOf(config.selectedClass)
        } else {
            val classesWithStudents = students
                .map { it.cls }
                .filter { effectiveAllowed.isEmpty() || it in effectiveAllowed }
                .distinct()
                .sorted()
            if (classesWithStudents.isNotEmpty()) classesWithStudents
            else effectiveAllowed.ifEmpty { activeClassIds(settings) }
        }

        val sortedByClass = students.groupBy { it.cls }.mapValues { (_, list) ->
            list.sortedBy { it.roll }
        }

        data class RawSheetPage(
            val cls: Int,
            val subjectKey: String?,
            val subjectTitleBn: String,
            val totalStudentsInClass: Int,
            val classPageIndex: Int,
            val classTotalPages: Int,
            val rows: List<BlankSheetRow>
        )

        val rawPages = mutableListOf<RawSheetPage>()

        targetClasses.forEach { cls ->
            val clsStudents = sortedByClass[cls].orEmpty()
            // When exporting All Classes, skip completely empty classes unless no class has any students
            if (config.selectedClass == null && clsStudents.isEmpty() && students.any { effectiveAllowed.isEmpty() || it.cls in effectiveAllowed }) {
                return@forEach
            }

            val mappedRows = clsStudents.map { s ->
                if (config.autoFillRollAndName) {
                    BlankSheetRow(
                        rollText = BlankSheetCatalog.formatInt(s.roll, config.numeralStyle, padTwo = true),
                        nameText = s.name
                    )
                } else {
                    BlankSheetRow(rollText = "", nameText = "")
                }
            }.toMutableList()

            repeat(extraBlank) {
                mappedRows.add(BlankSheetRow(rollText = "", nameText = ""))
            }

            if (mappedRows.isEmpty()) {
                repeat(perPage) {
                    mappedRows.add(BlankSheetRow(rollText = "", nameText = ""))
                }
            } else if (config.fillRemainingPageRows) {
                val remainder = mappedRows.size % perPage
                if (remainder != 0) {
                    repeat(perPage - remainder) {
                        mappedRows.add(BlankSheetRow(rollText = "", nameText = ""))
                    }
                }
            }

            val chunks = mappedRows.chunked(perPage)

            val subjectPairs: List<Pair<String?, String>> = if (isAllSubjects) {
                subjectsFor(cls, "ইসলাম").map { sub ->
                    sub.key to BlankSheetCatalog.shortSubjectTitleForSub(sub)
                }
            } else {
                listOf(config.subjectKey to BlankSheetCatalog.subjectTitleFor(config.subjectKey, config.customSubjectTitle))
            }

            subjectPairs.forEach { (subKey, subTitle) ->
                chunks.forEachIndexed { idx, chunk ->
                    rawPages.add(
                        RawSheetPage(
                            cls = cls,
                            subjectKey = subKey,
                            subjectTitleBn = subTitle,
                            totalStudentsInClass = clsStudents.size,
                            classPageIndex = idx + 1,
                            classTotalPages = chunks.size,
                            rows = chunk
                        )
                    )
                }
            }
        }

        val totalPages = rawPages.size
        return rawPages.mapIndexed { idx, raw ->
            val clsShort = classShortBn(raw.cls, settings)
            BlankSheetPageSpec(
                cls = raw.cls,
                classTitleBn = clsShort,
                subjectTitleBn = raw.subjectTitleBn,
                termTitleBn = termTitle,
                classPageIndex = raw.classPageIndex,
                classTotalPages = raw.classTotalPages,
                globalPageNumber = idx + 1,
                totalPages = totalPages,
                totalStudentsInClass = raw.totalStudentsInClass,
                rowsPerPageCapacity = perPage,
                rows = raw.rows,
                columns = safeCols,
                showSubjectMaxInHeader = config.showSubjectMaxInHeader,
                subjectKey = raw.subjectKey,
                numeralStyle = config.numeralStyle
            )
        }
    }
}
