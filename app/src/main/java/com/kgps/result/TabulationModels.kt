package com.kgps.result

enum class TabulationPageSize(
    val labelBn: String,
    val widthPt: Int,
    val heightPt: Int,
    val isLandscape: Boolean
) {
    A4_LANDSCAPE("A4 আড়াআড়ি", 842, 595, true),
    LEGAL_LANDSCAPE("Legal আড়াআড়ি", 1008, 612, true),
    A4_PORTRAIT("A4 খাড়া", 595, 842, false)
}

enum class TabulationTermScope(val term: Int, val labelBn: String, val shortLabelBn: String) {
    TERM_1(1, "১ম প্রান্তিক", "১ম"),
    TERM_2(2, "২য় প্রান্তিক", "২য়"),
    TERM_3(3, "৩য় প্রান্তিক", "৩য়"),
    FINAL_AVG(0, "চূড়ান্ত গড় (৩ প্রান্তিক)", "চূড়ান্ত")
}

enum class TabulationSubjectDisplay(val labelBn: String) {
    CONT_WR_TOT("ধা + সা + মোট"),
    TOTAL_ONLY("শুধু মোট")
}

enum class TabulationFinalDisplay(val labelBn: String) {
    TERMS_AND_AVG("৩ প্রান্তিক+গড়"),
    AVG_ONLY("শুধু গড়")
}

enum class TabulationHeaderStyle(val labelBn: String) {
    SHORT_SUB("সংক্ষিপ্ত (ধা/সা/মোট)"),
    VERTICAL_SUB("লম্বা ৯০° (ধারাবাহিক)")
}

enum class TabulationSortOrder(val labelBn: String) {
    ROLL_ASC("রোল অনুযায়ী"),
    MERIT_ASC("মেধাক্রম অনুযায়ী")
}

enum class TabulationNumeralMode(val labelBn: String) {
    BENGALI("বাংলা (১২৩)"),
    ENGLISH("English (123)")
}

data class TabulationConfig(
    val selectedClass: Int = 5,
    val termScope: TabulationTermScope = TabulationTermScope.TERM_3,
    val subjectDisplay: TabulationSubjectDisplay = TabulationSubjectDisplay.CONT_WR_TOT,
    val finalDisplay: TabulationFinalDisplay = TabulationFinalDisplay.TERMS_AND_AVG,
    val headerStyle: TabulationHeaderStyle = TabulationHeaderStyle.SHORT_SUB,
    val pageSize: TabulationPageSize = TabulationPageSize.A4_LANDSCAPE,
    val sortOrder: TabulationSortOrder = TabulationSortOrder.ROLL_ASC,
    val numeralMode: TabulationNumeralMode = TabulationNumeralMode.BENGALI,
    val bnFontId: String = "",
    val enFontId: String = "",
    val marginInches: Float = 0.30f,
    val rowsPerPage: Int = 25,
    val skipWrittenOnHalfSubjects: Boolean = true,
    val showTotal: Boolean = true,
    val showHighest: Boolean = false,
    val showPercentage: Boolean = true,
    val showGrade: Boolean = true,
    val showMeritRank: Boolean = true,
    val showSummaryFooter: Boolean = true,
    val customTitle: String = ""
) {
    val safeMarginInches: Float get() = marginInches.coerceIn(0.15f, 0.75f)
    val safeMarginPt: Float get() = safeMarginInches * 72f
    val safeRowsPerPage: Int get() = rowsPerPage.coerceIn(12, 36)
}

enum class TabulationSubColType(val shortBn: String, val fullBn: String) {
    CONT("ধা", "ধারাবাহিক"),
    WR("সা", "সামষ্টিক"),
    TOTAL("মোট", "মোট"),
    TERM_1("১ম", "১ম প্রান্তিক"),
    TERM_2("২য়", "২য় প্রান্তিক"),
    TERM_3("৩য়", "৩য় প্রান্তিক"),
    AVG("গড়", "চূড়ান্ত গড়")
}

data class TabulationSubjectColumnGroup(
    val key: String,
    val shortTitleBn: String,
    val maxMarks: Int,
    val hasWritten: Boolean,
    val subCols: List<TabulationSubColType>
)

data class TabulationCellValue(
    val text: String,
    val isFailed: Boolean = false,
    val isAbsent: Boolean = false,
    val isBold: Boolean = false
)

data class TabulationStudentRow(
    val student: Student,
    val rollText: String,
    val subjectCells: List<List<TabulationCellValue>>,
    val totalText: String,
    val highestText: String,
    val pctText: String,
    val gradeText: String,
    val meritRankText: String,
    val isPromoted: Boolean,
    val hasAnyData: Boolean
)

data class TabulationClassStats(
    val totalStudents: Int,
    val evaluatedStudents: Int,
    val promotedStudents: Int,
    val notPromotedStudents: Int,
    val passRateText: String,
    val highestTotalText: String
)

data class TabulationPageSpec(
    val pageNumber: Int,
    val totalPages: Int,
    val classId: Int,
    val classNameBn: String,
    val scopeTitleBn: String,
    val subjectGroups: List<TabulationSubjectColumnGroup>,
    val rows: List<TabulationStudentRow>,
    val classStats: TabulationClassStats,
    val config: TabulationConfig
)

object TabulationPlanner {

    fun formatDigits(raw: String, mode: TabulationNumeralMode): String = when (mode) {
        TabulationNumeralMode.BENGALI -> bn(raw)
        TabulationNumeralMode.ENGLISH -> fromBn(raw)
    }

    fun formatNum(value: Double, mode: TabulationNumeralMode): String =
        formatDigits(fmtEn(value), mode)

    fun shortSubjectTitle(sub: Sub): String = when (sub.key) {
        "BAN" -> "বাংলা"
        "ENG" -> "ইংরেজি"
        "MATH" -> "গণিত"
        "BGW" -> "বাওবি"
        "SOC" -> "সম.বিজ্ঞান"
        "SCI" -> "বিজ্ঞান"
        "REL" -> "ধর্ম"
        "PE" -> "স্বাস্থ্য"
        "ART" -> "শিল্প"
        else -> sub.bn.take(8)
    }

    fun buildSubjectGroups(
        cls: Int,
        settings: Settings,
        config: TabulationConfig
    ): List<TabulationSubjectColumnGroup> {
        val baseSubs = subjectsFor(cls, RELIGIONS.first(), settings)

        return baseSubs.map { sub ->
            val cols = when {
                config.termScope == TabulationTermScope.FINAL_AVG &&
                    config.finalDisplay == TabulationFinalDisplay.TERMS_AND_AVG -> listOf(
                    TabulationSubColType.TERM_1,
                    TabulationSubColType.TERM_2,
                    TabulationSubColType.TERM_3,
                    TabulationSubColType.AVG
                )
                config.termScope == TabulationTermScope.FINAL_AVG -> listOf(TabulationSubColType.TOTAL)
                config.subjectDisplay == TabulationSubjectDisplay.TOTAL_ONLY -> listOf(TabulationSubColType.TOTAL)
                sub.written == 0 && config.skipWrittenOnHalfSubjects -> listOf(TabulationSubColType.TOTAL)
                sub.written == 0 -> listOf(TabulationSubColType.CONT, TabulationSubColType.TOTAL)
                else -> listOf(TabulationSubColType.CONT, TabulationSubColType.WR, TabulationSubColType.TOTAL)
            }
            TabulationSubjectColumnGroup(
                key = sub.key,
                shortTitleBn = shortSubjectTitle(sub),
                maxMarks = sub.max,
                hasWritten = sub.written > 0,
                subCols = cols
            )
        }
    }

    fun buildPages(
        students: List<Student>,
        settings: Settings,
        config: TabulationConfig,
        allowedClassIds: List<Int> = activeClassIds(settings)
    ): List<TabulationPageSpec> {
        val cls = config.selectedClass.takeIf { it in allowedClassIds }
            ?: allowedClassIds.firstOrNull()
            ?: return emptyList()

        val classStudents = students.filter { it.cls == cls }
        if (classStudents.isEmpty()) return emptyList()

        val rules = settings.resultRules
        val scopedTerm = config.termScope.term
        val rankedList = ResultRulesEngine.rankClassStudents(
            classStudents = classStudents,
            rules = rules,
            scopedTerm = scopedTerm
        )
        val rankedById = rankedList.associateBy { it.student.id }

        val activeTerms = if (scopedTerm in 1..3) {
            listOf(scopedTerm)
        } else {
            ResultRulesEngine.activeTermsForClass(classStudents, rules.dynamicTermAveraging)
        }

        val highestObtained = rankedList
            .filter { it.eval.hasAnyData }
            .maxOfOrNull { it.eval.obtainedAvgTotal }
        val highestStr = highestObtained?.let { formatNum(it, config.numeralMode) } ?: "—"

        val subjectGroups = buildSubjectGroups(cls, settings, config)

        val orderedStudents = when (config.sortOrder) {
            TabulationSortOrder.ROLL_ASC -> classStudents.sortedBy { it.roll }
            TabulationSortOrder.MERIT_ASC -> rankedList.map { it.student }
        }

        val rows = orderedStudents.map { student ->
            val ranked = rankedById[student.id]
            val eval = ranked?.eval
            val anyData = eval?.hasAnyData == true
            val studentSubsByKey = subjectsFor(cls, student.religion, settings).associateBy { it.key }

            val subjectCells = subjectGroups.map { grp ->
                val sub = studentSubsByKey[grp.key]
                if (sub == null) {
                    grp.subCols.map { TabulationCellValue("") }
                } else if (scopedTerm in 1..3) {
                    val hasSub = student.hasSub(sub, scopedTerm)
                    val contAbs = student.isCompAbsent(sub.key, scopedTerm, "CONT")
                    val wrAbs = sub.written > 0 && student.isCompAbsent(sub.key, scopedTerm, "WR")
                    val subAbs = student.isSubFullyAbsent(sub, scopedTerm)
                    val subFailed = ResultRulesEngine.isSubjectTermFailed(student, sub, scopedTerm, rules)

                    grp.subCols.map { colType ->
                        when (colType) {
                            TabulationSubColType.CONT -> when {
                                contAbs -> TabulationCellValue("A", isAbsent = true, isBold = true)
                                hasSub -> TabulationCellValue(formatNum(student.cont(sub, scopedTerm), config.numeralMode))
                                else -> TabulationCellValue("")
                            }
                            TabulationSubColType.WR -> when {
                                sub.written == 0 -> TabulationCellValue("—")
                                wrAbs -> TabulationCellValue("A", isAbsent = true, isBold = true)
                                hasSub -> TabulationCellValue(formatNum(student.wr(sub, scopedTerm), config.numeralMode))
                                else -> TabulationCellValue("")
                            }
                            TabulationSubColType.TOTAL -> when {
                                subAbs -> TabulationCellValue("A", isAbsent = true, isBold = true)
                                hasSub -> {
                                    val raw = formatNum(student.total(sub, scopedTerm), config.numeralMode)
                                    val txt = ResultRulesEngine.formatTermTotalCell(raw, subFailed, rules)
                                    TabulationCellValue(txt, isFailed = subFailed && rules.showFailIndicator, isBold = true)
                                }
                                else -> TabulationCellValue("")
                            }
                            TabulationSubColType.TERM_1,
                            TabulationSubColType.TERM_2,
                            TabulationSubColType.TERM_3,
                            TabulationSubColType.AVG -> TabulationCellValue("")
                        }
                    }
                } else {
                    // Final average mode: supports either 3-term breakdown + final avg OR single avg column
                    val entered = activeTerms.filter { student.hasSub(sub, it) }
                    val anySubData = entered.isNotEmpty()
                    val anySubAbs = entered.any { student.isSubTermAbsent(sub, it) }
                    val subAvgFailed = ResultRulesEngine.isSubjectAnnualFailed(student, sub, activeTerms, rules)
                    grp.subCols.map { colType ->
                        when (colType) {
                            TabulationSubColType.TERM_1,
                            TabulationSubColType.TERM_2,
                            TabulationSubColType.TERM_3 -> {
                                val tNum = when (colType) {
                                    TabulationSubColType.TERM_1 -> 1
                                    TabulationSubColType.TERM_2 -> 2
                                    else -> 3
                                }
                                val hasTermSub = student.hasSub(sub, tNum)
                                val termAbs = student.isSubFullyAbsent(sub, tNum)
                                val termFailed = ResultRulesEngine.isSubjectTermFailed(student, sub, tNum, rules)
                                when {
                                    termAbs -> TabulationCellValue("A", isAbsent = true, isBold = false)
                                    hasTermSub -> {
                                        val raw = formatNum(student.total(sub, tNum), config.numeralMode)
                                        val txt = ResultRulesEngine.formatTermTotalCell(raw, termFailed, rules)
                                        TabulationCellValue(
                                            text = txt,
                                            isFailed = termFailed && rules.showFailIndicator,
                                            isBold = false
                                        )
                                    }
                                    else -> TabulationCellValue("")
                                }
                            }
                            TabulationSubColType.AVG,
                            TabulationSubColType.TOTAL,
                            TabulationSubColType.CONT,
                            TabulationSubColType.WR -> {
                                if (!anySubData) {
                                    TabulationCellValue("")
                                } else {
                                    val avg = ResultRulesEngine.subjectAvg(student, sub, activeTerms)
                                    val raw = formatNum(avg, config.numeralMode)
                                    val txt = ResultRulesEngine.formatTermTotalCell(raw, subAvgFailed, rules)
                                    TabulationCellValue(
                                        text = txt,
                                        isFailed = (subAvgFailed && rules.showFailIndicator) || anySubAbs,
                                        isBold = true
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val totalText = if (anyData && eval != null) formatNum(eval.obtainedAvgTotal, config.numeralMode) else ""
            val pctText = if (anyData && eval != null) "${formatNum(eval.pct, config.numeralMode)}%" else ""
            val gradeText = if (anyData && eval != null) eval.grade else ""
            val meritText = if (anyData && ranked != null && ranked.rank > 0) {
                val baseRank = formatDigits(ranked.rank.toString(), config.numeralMode)
                if (ranked.isJointRank) "$baseRank*" else baseRank
            } else {
                ""
            }

            TabulationStudentRow(
                student = student,
                rollText = formatDigits(student.roll.toString(), config.numeralMode),
                subjectCells = subjectCells,
                totalText = totalText,
                highestText = if (anyData) highestStr else "",
                pctText = pctText,
                gradeText = gradeText,
                meritRankText = meritText,
                isPromoted = eval?.isPromoted == true,
                hasAnyData = anyData
            )
        }

        val evaluatedRows = rows.filter { it.hasAnyData }
        val promotedCount = evaluatedRows.count { it.isPromoted }
        val notPromotedCount = (evaluatedRows.size - promotedCount).coerceAtLeast(0)
        val passPct = if (evaluatedRows.isNotEmpty()) {
            (promotedCount.toDouble() / evaluatedRows.size.toDouble()) * 100.0
        } else 0.0

        val stats = TabulationClassStats(
            totalStudents = rows.size,
            evaluatedStudents = evaluatedRows.size,
            promotedStudents = promotedCount,
            notPromotedStudents = notPromotedCount,
            passRateText = if (evaluatedRows.isNotEmpty()) "${formatNum(passPct, config.numeralMode)}%" else "—",
            highestTotalText = highestStr
        )

        val yearStr = formatDigits(settings.year.ifBlank { "2026" }, config.numeralMode)
        val scopeTitle = config.customTitle.trim().ifBlank {
            when (config.termScope) {
                TabulationTermScope.FINAL_AVG -> "চূড়ান্ত ফলাফল সারসংক্ষেপ (বার্ষিক গড়) — $yearStr"
                else -> "${config.termScope.labelBn} ফলাফল ট্যাবুলেশন — $yearStr"
            }
        }
        val clsName = "${classNameBn(cls, settings)} শ্রেণি"
        val perPage = config.safeRowsPerPage
        val chunks = rows.chunked(perPage)
        val totalPages = chunks.size.coerceAtLeast(1)

        return chunks.mapIndexed { idx, chunk ->
            TabulationPageSpec(
                pageNumber = idx + 1,
                totalPages = totalPages,
                classId = cls,
                classNameBn = clsName,
                scopeTitleBn = scopeTitle,
                subjectGroups = subjectGroups,
                rows = chunk,
                classStats = stats,
                config = config
            )
        }
    }
}
