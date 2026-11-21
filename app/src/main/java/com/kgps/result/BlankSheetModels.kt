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
    val ALL_COLUMNS: List<BlankSheetColumn> = listOf(
        BlankSheetColumn("ROLL", "রোল", widthWeight = 0.72f, fillWith = BlankColFill.ROLL, align = BlankColAlign.CENTER),
        BlankSheetColumn("NAME", "শিক্ষার্থীর নাম", widthWeight = 2.45f, fillWith = BlankColFill.NAME, align = BlankColAlign.LEFT),
        BlankSheetColumn("TB", "TB", widthWeight = 0.85f, compKey = "TB"),
        BlankSheetColumn("ACT", "ACT", widthWeight = 0.85f, compKey = "ACT"),
        BlankSheetColumn("LANG", "LANG", widthWeight = 0.85f, compKey = "LANG"),
        BlankSheetColumn("CT", "CT", widthWeight = 0.85f, compKey = "CT"),
        BlankSheetColumn("CONT", "মোট ধারাবাহিক", widthWeight = 1.15f, compKey = "CONT"),
        BlankSheetColumn("WR", "লিখিত", widthWeight = 0.95f, compKey = "WR"),
        BlankSheetColumn("TOTAL", "মোট", widthWeight = 0.95f, compKey = "TOTAL"),
        BlankSheetColumn("GRADE", "গ্রেড", widthWeight = 0.78f),
        BlankSheetColumn("GP", "জিপি", widthWeight = 0.72f),
        BlankSheetColumn("MERIT", "মেধাক্রম", widthWeight = 0.85f),
        BlankSheetColumn("ATT", "উপস্থিতি", widthWeight = 0.92f),
        BlankSheetColumn("REMARK", "মন্তব্য", widthWeight = 1.55f, align = BlankColAlign.LEFT),
        BlankSheetColumn("SIGN", "স্বাক্ষর", widthWeight = 1.05f)
    )

    val DEFAULT_COLUMNS: List<BlankSheetColumn> =
        BlankSheetPreset.COMPACT_MARKS.columnIds.mapNotNull { id -> ALL_COLUMNS.find { it.id == id } }

    val SUBJECT_OPTIONS: List<BlankSubjectOption> = listOf(
        BlankSubjectOption("BAN", "বাংলা"),
        BlankSubjectOption("ENG", "ইংরেজি"),
        BlankSubjectOption("MATH", "গণিত"),
        BlankSubjectOption("SOC", "পরিবেশ পরিচিতি সমাজ"),
        BlankSubjectOption("SCI", "বিজ্ঞান"),
        BlankSubjectOption("BGW", "বাংলাদেশ ও বিশ্বপরিচয়"),
        BlankSubjectOption("REL", "ধর্ম ও নৈতিক শিক্ষা"),
        BlankSubjectOption("PE", "শারীরিক ও মানসিক স্বাস্থ্য"),
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

    fun subjectTitleFor(subjectKey: String?, customTitle: String): String {
        val custom = customTitle.trim()
        if (subjectKey == null) return custom
        return SUBJECT_OPTIONS.find { it.key == subjectKey }?.labelBn ?: custom
    }

    fun termLabelFor(term: Int, customTermTitle: String = ""): String = when (term) {
        1, 2, 3 -> termNameBn(term)
        0 -> "বার্ষিক মূল্যায়ন"
        -1 -> customTermTitle.trim()
        else -> ""
    }

    fun resolveMaxHint(cls: Int, subjectKey: String?, compKey: String?): Int? {
        if (subjectKey.isNullOrBlank() || compKey.isNullOrBlank()) return null
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
    val subjectKey: String? = "BAN",
    val customSubjectTitle: String = "",
    val term: Int = 1, // 1..3, 0 = বার্ষিক, -1 = কাস্টম/ফাঁকা
    val customTermTitle: String = "",
    val columns: List<BlankSheetColumn> = BlankSheetCatalog.DEFAULT_COLUMNS,
    val rowsPerPage: Int = 20,
    val orientation: BlankSheetOrientation = BlankSheetOrientation.PORTRAIT,
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
    val subjectKey: String?
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
        val subjectTitle = BlankSheetCatalog.subjectTitleFor(config.subjectKey, config.customSubjectTitle)
        val termTitle = BlankSheetCatalog.termLabelFor(config.term, config.customTermTitle)

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

        data class RawClassChunk(
            val cls: Int,
            val totalStudentsInClass: Int,
            val classPageIndex: Int,
            val classTotalPages: Int,
            val rows: List<BlankSheetRow>
        )

        val rawPages = mutableListOf<RawClassChunk>()

        targetClasses.forEach { cls ->
            val clsStudents = sortedByClass[cls].orEmpty()
            // When exporting All Classes, skip completely empty classes unless no class has any students
            if (config.selectedClass == null && clsStudents.isEmpty() && students.any { effectiveAllowed.isEmpty() || it.cls in effectiveAllowed }) {
                return@forEach
            }

            val mappedRows = clsStudents.map { s ->
                if (config.autoFillRollAndName) {
                    BlankSheetRow(
                        rollText = bn(s.roll.toString().padStart(2, '0')),
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
            chunks.forEachIndexed { idx, chunk ->
                rawPages.add(
                    RawClassChunk(
                        cls = cls,
                        totalStudentsInClass = clsStudents.size,
                        classPageIndex = idx + 1,
                        classTotalPages = chunks.size,
                        rows = chunk
                    )
                )
            }
        }

        val totalPages = rawPages.size
        return rawPages.mapIndexed { idx, raw ->
            val clsName = classNameBn(raw.cls, settings)
            BlankSheetPageSpec(
                cls = raw.cls,
                classTitleBn = "$clsName শ্রেণি",
                subjectTitleBn = subjectTitle,
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
                subjectKey = config.subjectKey
            )
        }
    }
}
