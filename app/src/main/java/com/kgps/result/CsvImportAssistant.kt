package com.kgps.result

import com.kgps.result.ai.ClassSheetImportScope
import com.kgps.result.ai.ClassSheetKind

enum class CsvImportModule(
    val id: String,
    val titleBn: String,
    val subtitleBn: String
) {
    STUDENT_PROFILE(
        id = "profile",
        titleBn = "শিক্ষার্থী তথ্য",
        subtitleBn = "রোল, নাম, অভিভাবক, মোবাইল, ঠিকানা ও ধর্ম"
    ),
    TERM_MARKS(
        id = "marks",
        titleBn = "বিষয়ভিত্তিক নম্বর",
        subtitleBn = "শ্রেণি ও ১/২/৩ প্রান্তিকের সব বিষয়ের নম্বর"
    ),
    SUBJECT_ALL_TERMS(
        id = "subject_3terms",
        titleBn = "বিষয়ভিত্তিক ৩ প্রান্তিক",
        subtitleBn = "উপরে শ্রেণি ও বিষয়; রোল, নাম ও ৩ প্রান্তিকের ধারাবাহিক-সামষ্টিক"
    ),
    ATTENDANCE_TRAITS(
        id = "attendance",
        titleBn = "উপস্থিতি ও আচরণ",
        subtitleBn = "১–৩ প্রান্তিকের কার্যদিবস, উপস্থিতি, শৃঙ্খলা ও মন্তব্য"
    ),
    FULL_RECORD(
        id = "full",
        titleBn = "সম্পূর্ণ সম্মিলিত CSV",
        subtitleBn = "প্রোফাইল + ১–৩ প্রান্তিকের নম্বর + উপস্থিতি"
    )
}

enum class CsvDelimiterFormat(
    val id: String,
    val labelBn: String,
    val sep: String,
    val aiDescBn: String
) {
    COMMA("comma", "Comma CSV (,)", ",", "কমা (,) দিয়ে আলাদা করা CSV"),
    TAB("tab", "Excel Tab (TSV)", "\t", "ট্যাব (Tab / \\t) দিয়ে আলাদা করা TSV"),
    SEMICOLON("semicolon", "Semicolon (;)", ";", "সেমিকোলন (;) দিয়ে আলাদা করা CSV")
}

enum class CsvDigitMode(
    val id: String,
    val labelBn: String,
    val aiRuleBn: String
) {
    ENGLISH("en", "ইংরেজি সংখ্যা (1, 2, 18)", "সকল সংখ্যা ইংরেজি অঙ্কে (0-9) লিখুন"),
    BANGLA("bn", "বাংলা সংখ্যা (১, ২, ১৮)", "সকল সংখ্যা বাংলা অঙ্কে (০-৯) লিখুন")
}

data class CsvColumnOption(
    val key: String,
    val labelBn: String,
    val group: String,
    val required: Boolean,
    val sampleEn: String,
    val ruleHint: String
)

data class CsvPromptConfig(
    val module: CsvImportModule = CsvImportModule.STUDENT_PROFILE,
    val targetCls: Int = 1, // 1..20 or 0 for multi-class
    val targetTerm: Int = 1, // 1..3
    val selectedSubjectKeys: Set<String> = emptySet(),
    val selectedColumnKeys: Set<String> = defaultSelectedColumnKeys(CsvImportModule.STUDENT_PROFILE, 1, 1),
    val format: CsvDelimiterFormat = CsvDelimiterFormat.COMMA,
    val digitMode: CsvDigitMode = CsvDigitMode.ENGLISH,
    val includeCustomFields: Boolean = true,
    val extraInstruction: String = "",
    val allTermsSelection: Set<Int> = setOf(1, 2, 3),
    val useTwoRowGroupedHeader: Boolean = true,
    val multiTermSelection: Set<Int> = emptySet(),
    val conciseMarks: Boolean = false,
    val includeRosterInPrompt: Boolean = false
)

fun effectiveTermsForConfig(config: CsvPromptConfig): List<Int> = when (config.module) {
    CsvImportModule.SUBJECT_ALL_TERMS ->
        config.allTermsSelection.filter { it in 1..3 }.sorted().ifEmpty { listOf(1, 2, 3) }
    CsvImportModule.STUDENT_PROFILE -> emptyList()
    else ->
        config.multiTermSelection.filter { it in 1..3 }.sorted().ifEmpty { listOf(config.targetTerm.coerceIn(1, 3)) }
}

enum class CsvRowStatus { VALID, WARNING, ERROR }

data class CsvValidatedRow(
    val dataRowIndex: Int,
    val lineNumber: Int,
    val status: CsvRowStatus,
    val isUpdate: Boolean,
    val resolvedCls: Int?,
    val resolvedRoll: Int?,
    val resolvedName: String,
    val parsedMarksCount: Int,
    val parsedFieldsPreview: String,
    val messages: List<String>,
    val rawCells: List<String>
)

data class CsvValidationReport(
    val rows: List<CsvValidatedRow>,
    val totalRows: Int,
    val validAddCount: Int,
    val validUpdateCount: Int,
    val warningCount: Int,
    val errorCount: Int,
    val hasMissingRollOrNameMapping: Boolean
)

fun defaultSelectedColumnKeys(
    module: CsvImportModule,
    targetCls: Int,
    targetTerm: Int,
    customFields: List<CustomFieldDef> = emptyList(),
    selectedSubjects: Set<String> = emptySet(),
    allTermsSelection: Set<Int> = setOf(1, 2, 3),
    multiTermSelection: Set<Int> = emptySet(),
    conciseMarks: Boolean = false
): Set<String> {
    val effectiveCls = if (targetCls in 1..20) targetCls else 3
    val allClassSubs = subjectsFor(effectiveCls, "ইসলাম")
    val subs = allClassSubs.let { all ->
        if (selectedSubjects.isEmpty()) all else all.filter { selectedSubjects.contains(it.key) }
    }
    val generalTerms = multiTermSelection.filter { it in 1..3 }.sorted().ifEmpty { listOf(targetTerm.coerceIn(1, 3)) }
    val useConcise = conciseMarks || generalTerms.size > 1
    return when (module) {
        CsvImportModule.STUDENT_PROFILE -> linkedSetOf(
            "cls", "roll", "name", "guardian", "mobile", "village", "religion"
        ).apply {
            addAll(customFields.map { it.label })
        }

        CsvImportModule.TERM_MARKS -> linkedSetOf("cls", "roll", "name").apply {
            generalTerms.forEach { t ->
                subs.forEach { s ->
                    if (useConcise) {
                        add(mk(s.key, t, "CONT"))
                    } else {
                        s.comps.forEach { c -> add(mk(s.key, t, c.key)) }
                    }
                    if (s.written > 0) add(mk(s.key, t, "WR"))
                }
            }
        }

        CsvImportModule.SUBJECT_ALL_TERMS -> linkedSetOf("roll", "name").apply {
            val singleSub = selectedSubjects.firstOrNull()?.let { k ->
                allClassSubs.find { it.key == k }
            } ?: allClassSubs.first()
            val terms = allTermsSelection.filter { it in 1..3 }.sorted().ifEmpty { listOf(1, 2, 3) }
            terms.forEach { t ->
                add(mk(singleSub.key, t, "CONT"))
                if (singleSub.written > 0) add(mk(singleSub.key, t, "WR"))
            }
        }

        CsvImportModule.ATTENDANCE_TRAITS -> linkedSetOf("cls", "roll", "name").apply {
            generalTerms.forEach { t ->
                addAll(listOf("DAYS_$t", "PRES_$t", "ACT_$t", "DISC_$t", "COND_$t", "REM_$t"))
            }
        }

        CsvImportModule.FULL_RECORD -> linkedSetOf(
            "cls", "roll", "name", "guardian", "mobile", "village", "union", "upazila", "district", "religion"
        ).apply {
            addAll(customFields.map { it.label })
            generalTerms.forEach { t ->
                subs.forEach { s ->
                    if (useConcise) {
                        add(mk(s.key, t, "CONT"))
                    } else {
                        s.comps.forEach { c -> add(mk(s.key, t, c.key)) }
                    }
                    if (s.written > 0) add(mk(s.key, t, "WR"))
                }
                addAll(listOf("DAYS_$t", "PRES_$t", "ACT_$t", "DISC_$t", "COND_$t", "REM_$t"))
            }
        }
    }
}

object CsvImportAssistant {

    private val PROFILE_COLUMNS = listOf(
        CsvColumnOption("cls", "শ্রেণি (cls)", "প্রোফাইল", required = true, sampleEn = "3", ruleHint = "১–২০ / শ্রেণির নাম"),
        CsvColumnOption("roll", "রোল (roll)", "প্রোফাইল", required = true, sampleEn = "1", ruleHint = "ধনাত্মক পূর্ণসংখ্যা"),
        CsvColumnOption("name", "শিক্ষার্থীর নাম (name)", "প্রোফাইল", required = true, sampleEn = "নুসরাত জাহান মিম", ruleHint = "শিক্ষার্থীর পূর্ণ নাম"),
        CsvColumnOption("guardian", "পিতা / অভিভাবক (guardian)", "প্রোফাইল", required = false, sampleEn = "মোঃ শফিকুল ইসলাম", ruleHint = "পিতা বা অভিভাবকের নাম"),
        CsvColumnOption("mobile", "মোবাইল (mobile)", "প্রোফাইল", required = false, sampleEn = "01712345678", ruleHint = "১১ ডিজিটের মোবাইল নম্বর"),
        CsvColumnOption("village", "গ্রাম (village)", "প্রোফাইল", required = false, sampleEn = "বানা", ruleHint = "গ্রামের নাম"),
        CsvColumnOption("union", "ইউনিয়ন (union)", "প্রোফাইল", required = false, sampleEn = "বানা", ruleHint = "ইউনিয়নের নাম (ঐচ্ছিক)"),
        CsvColumnOption("upazila", "উপজেলা (upazila)", "প্রোফাইল", required = false, sampleEn = DEFAULT_UPAZILA, ruleHint = "উপজেলার নাম (ফাঁকা থাকলে $DEFAULT_UPAZILA)"),
        CsvColumnOption("district", "জেলা (district)", "প্রোফাইল", required = false, sampleEn = DEFAULT_DISTRICT, ruleHint = "জেলার নাম (ফাঁকা থাকলে $DEFAULT_DISTRICT)"),
        CsvColumnOption("religion", "ধর্ম (religion)", "প্রোফাইল", required = false, sampleEn = "ইসলাম", ruleHint = "ইসলাম / হিন্দু / বৌদ্ধ / খ্রীষ্ট")
    )

    private fun attendanceColumns(term: Int, includeTermPrefix: Boolean = false): List<CsvColumnOption> {
        val groupName = if (includeTermPrefix) "${termOrdinalBn(term)} · উপস্থিতি ও আচরণ" else "উপস্থিতি ও আচরণ"
        val pfx = if (includeTermPrefix) "${termOrdinalBn(term)} · " else ""
        return listOf(
            CsvColumnOption("DAYS_$term", "${pfx}মোট কার্যদিবস (DAYS_$term)", groupName, false, "62", "পূর্ণসংখ্যা (যেমন 62)"),
            CsvColumnOption("PRES_$term", "${pfx}মোট উপস্থিতি (PRES_$term)", groupName, false, "59", "কার্যদিবসের সমান বা কম"),
            CsvColumnOption("ACT_$term", "${pfx}সহপাঠক্রমিক (ACT_$term)", groupName, false, "ক", "কেবল ক, খ বা গ"),
            CsvColumnOption("DISC_$term", "${pfx}শৃঙ্খলাবোধ (DISC_$term)", groupName, false, "ক", "কেবল ক, খ বা গ"),
            CsvColumnOption("COND_$term", "${pfx}আচরণ (COND_$term)", groupName, false, "ক", "কেবল ক, খ বা গ"),
            CsvColumnOption("REM_$term", "${pfx}শিক্ষকের মন্তব্য (REM_$term)", groupName, false, "পাঠ্যবিষয়ে মনোযোগী ও নিয়মিত।", "সংক্ষিপ্ত মূল্যায়ন মন্তব্য")
        )
    }

    private fun markColumns(
        effectiveCls: Int,
        term: Int,
        selectedSubjects: Set<String>,
        includeTermPrefix: Boolean = false
    ): List<CsvColumnOption> {
        val subs = subjectsFor(effectiveCls, "ইসলাম").let { list ->
            if (selectedSubjects.isEmpty()) list else list.filter { selectedSubjects.contains(it.key) }
        }
        val out = mutableListOf<CsvColumnOption>()
        val tPfx = if (includeTermPrefix) "${termOrdinalBn(term)} · " else ""
        subs.forEach { s ->
            val subShort = s.bn.substringBefore(" (")
            val groupName = if (includeTermPrefix) "${termOrdinalBn(term)} · $subShort" else "নম্বর ($subShort)"
            s.comps.forEach { c ->
                val sample = ((c.max * 8) / 10).coerceAtLeast(1).toString()
                val compLabel = if (s.key == "ART" && c.key == "TOTAL") "মোট" else c.key
                out.add(
                    CsvColumnOption(
                        key = mk(s.key, term, c.key),
                        labelBn = "$tPfx$subShort · $compLabel (সর্বোচ্চ ${bn(c.max.toString())})",
                        group = groupName,
                        required = false,
                        sampleEn = sample,
                        ruleHint = "0 থেকে ${c.max}"
                    )
                )
            }
            val contMax = s.comps.sumOf { it.max }
            if (contMax > 0) {
                val contSample = ((contMax * 8) / 10).coerceAtLeast(1).toString()
                out.add(
                    CsvColumnOption(
                        key = mk(s.key, term, "CONT"),
                        labelBn = "$tPfx$subShort · মোট ধারাবাহিক (সর্বোচ্চ ${bn(contMax.toString())})",
                        group = groupName,
                        required = false,
                        sampleEn = contSample,
                        ruleHint = "0 থেকে $contMax"
                    )
                )
            }
            if (s.written > 0) {
                val sample = ((s.written * 8) / 10).coerceAtLeast(1).toString()
                out.add(
                    CsvColumnOption(
                        key = mk(s.key, term, "WR"),
                        labelBn = "$tPfx$subShort · WR সামষ্টিক (সর্বোচ্চ ${bn(s.written.toString())})",
                        group = groupName,
                        required = false,
                        sampleEn = sample,
                        ruleHint = "0 থেকে ${s.written}"
                    )
                )
            }
            val totalSample = ((s.max * 8) / 10).coerceAtLeast(1).toString()
            out.add(
                CsvColumnOption(
                    key = mk(s.key, term, "TOTAL"),
                    labelBn = "$tPfx$subShort · মোট (সর্বোচ্চ ${bn(s.max.toString())})",
                    group = groupName,
                    required = false,
                    sampleEn = totalSample,
                    ruleHint = "0 থেকে ${s.max}"
                )
            )
        }
        return out
    }

    private fun subjectAllTermsColumns(effectiveCls: Int, config: CsvPromptConfig): List<CsvColumnOption> {
        val allSubs = subjectsFor(effectiveCls, "ইসলাম")
        val sub = config.selectedSubjectKeys.firstOrNull()?.let { key ->
            allSubs.find { it.key == key }
        } ?: allSubs.first()
        val subShort = sub.bn.substringBefore(" (")
        val contMax = sub.comps.sumOf { it.max }
        val terms = config.allTermsSelection.filter { it in 1..3 }.sorted().ifEmpty { listOf(1, 2, 3) }
        val out = mutableListOf<CsvColumnOption>()
        out.addAll(PROFILE_COLUMNS.subList(1, 3)) // roll, name (required)

        terms.forEach { t ->
            val tName = termNameBn(t)
            if (contMax > 0) {
                out.add(
                    CsvColumnOption(
                        key = mk(sub.key, t, "CONT"),
                        labelBn = "$tName · ধারাবাহিক (সর্বোচ্চ ${bn(contMax.toString())})",
                        group = "$subShort · $tName",
                        required = false,
                        sampleEn = ((contMax * 8) / 10).coerceAtLeast(1).toString(),
                        ruleHint = "0 থেকে $contMax"
                    )
                )
            }
            if (sub.written > 0) {
                out.add(
                    CsvColumnOption(
                        key = mk(sub.key, t, "WR"),
                        labelBn = "$tName · সামষ্টিক (সর্বোচ্চ ${bn(sub.written.toString())})",
                        group = "$subShort · $tName",
                        required = false,
                        sampleEn = ((sub.written * 8) / 10).coerceAtLeast(1).toString(),
                        ruleHint = "0 থেকে ${sub.written}"
                    )
                )
            }
            out.add(
                CsvColumnOption(
                    key = mk(sub.key, t, "TOTAL"),
                    labelBn = "$tName · মোট (সর্বোচ্চ ${bn(sub.max.toString())})",
                    group = "$subShort · $tName",
                    required = false,
                    sampleEn = ((sub.max * 8) / 10).coerceAtLeast(1).toString(),
                    ruleHint = "0 থেকে ${sub.max}"
                )
            )
        }
        return out
    }

    fun resolveSubjectForAllTerms(config: CsvPromptConfig): Sub {
        val effectiveCls = if (config.targetCls in 1..20) config.targetCls else 3
        val allSubs = subjectsFor(effectiveCls, "ইসলাম")
        return config.selectedSubjectKeys.firstOrNull()?.let { key ->
            allSubs.find { it.key == key }
        } ?: allSubs.first()
    }

    fun availableColumnsForConfig(
        config: CsvPromptConfig,
        customFields: List<CustomFieldDef>
    ): List<CsvColumnOption> {
        val effectiveCls = if (config.targetCls in 1..20) config.targetCls else 3
        val list = mutableListOf<CsvColumnOption>()

        when (config.module) {
            CsvImportModule.STUDENT_PROFILE -> {
                list.addAll(PROFILE_COLUMNS)
                if (config.includeCustomFields) {
                    customFields.forEach { cf ->
                        list.add(
                            CsvColumnOption(
                                key = cf.label,
                                labelBn = "${cf.label} (কাস্টম)",
                                group = "কাস্টম ফিল্ড",
                                required = false,
                                sampleEn = "নমুনা",
                                ruleHint = "কাস্টম তথ্য"
                            )
                        )
                    }
                }
            }

            CsvImportModule.TERM_MARKS -> {
                list.addAll(PROFILE_COLUMNS.take(3)) // cls, roll, name
                val terms = effectiveTermsForConfig(config)
                terms.forEach { t ->
                    list.addAll(markColumns(effectiveCls, t, config.selectedSubjectKeys, includeTermPrefix = terms.size > 1))
                }
            }

            CsvImportModule.SUBJECT_ALL_TERMS -> {
                list.addAll(subjectAllTermsColumns(effectiveCls, config))
            }

            CsvImportModule.ATTENDANCE_TRAITS -> {
                list.addAll(PROFILE_COLUMNS.take(3)) // cls, roll, name
                val terms = effectiveTermsForConfig(config)
                terms.forEach { t ->
                    list.addAll(attendanceColumns(t, includeTermPrefix = terms.size > 1))
                }
            }

            CsvImportModule.FULL_RECORD -> {
                list.addAll(PROFILE_COLUMNS)
                if (config.includeCustomFields) {
                    customFields.forEach { cf ->
                        list.add(
                            CsvColumnOption(
                                key = cf.label,
                                labelBn = "${cf.label} (কাস্টম)",
                                group = "কাস্টম ফিল্ড",
                                required = false,
                                sampleEn = "নমুনা",
                                ruleHint = "কাস্টম তথ্য"
                            )
                        )
                    }
                }
                val terms = effectiveTermsForConfig(config)
                terms.forEach { t ->
                    list.addAll(markColumns(effectiveCls, t, config.selectedSubjectKeys, includeTermPrefix = terms.size > 1))
                    list.addAll(attendanceColumns(t, includeTermPrefix = terms.size > 1))
                }
            }
        }
        return list
    }

    fun selectedColumnSpecs(
        config: CsvPromptConfig,
        customFields: List<CustomFieldDef>
    ): List<CsvColumnOption> {
        val available = availableColumnsForConfig(config, customFields)
        return available.filter { it.required || config.selectedColumnKeys.contains(it.key) }
    }

    private fun csvQuoteIfNeeded(value: String, sep: String): String {
        val needsQuote = value.contains(sep) || value.contains('"') || value.contains('\n') || value.contains('\r')
        return if (needsQuote) "\"${value.replace("\"", "\"\"")}\"" else value
    }

    private fun buildSubjectAllTermsHeaderRows(
        config: CsvPromptConfig,
        settings: Settings,
        cols: List<CsvColumnOption>
    ): List<String> {
        val sep = config.format.sep
        val effectiveCls = if (config.targetCls in 1..20) config.targetCls else 3
        val clsLabel = classShortBn(effectiveCls, settings)
        val sub = resolveSubjectForAllTerms(config)
        val subLabel = sub.bn.substringBefore(" (")
        val metaRow = listOf("শ্রেণি", clsLabel, "বিষয়", subLabel).joinToString(sep) { csvQuoteIfNeeded(it, sep) }

        fun subLabelBn(compKey: String): String = when (compKey) {
            "CONT" -> "ধারাবাহিক"
            "WR" -> "সামষ্টিক"
            "TOTAL" -> "মোট"
            else -> compKey
        }

        if (!config.useTwoRowGroupedHeader) {
            val single = cols.joinToString(sep) { col ->
                val label = when (col.key) {
                    "roll" -> "রোল"
                    "name" -> "নাম"
                    else -> {
                        val parts = col.key.split("_")
                        val t = parts.getOrNull(1)?.toIntOrNull()
                        val c = parts.getOrNull(2).orEmpty()
                        if (t != null) "${termNameBn(t)} - ${subLabelBn(c)}" else col.key
                    }
                }
                csvQuoteIfNeeded(label, sep)
            }
            return listOf(metaRow, single)
        }

        var prevTerm: Int? = null
        val groupRowCells = cols.map { col ->
            when (col.key) {
                "roll" -> "রোল"
                "name" -> "নাম"
                else -> {
                    val t = col.key.split("_").getOrNull(1)?.toIntOrNull()
                    if (t != null && t != prevTerm) {
                        prevTerm = t
                        termNameBn(t)
                    } else ""
                }
            }
        }
        val subRowCells = cols.map { col ->
            when (col.key) {
                "roll", "name" -> ""
                else -> subLabelBn(col.key.split("_").getOrNull(2).orEmpty())
            }
        }
        return listOf(
            metaRow,
            groupRowCells.joinToString(sep) { csvQuoteIfNeeded(it, sep) },
            subRowCells.joinToString(sep) { csvQuoteIfNeeded(it, sep) }
        )
    }

    fun buildHeaderLine(
        config: CsvPromptConfig,
        customFields: List<CustomFieldDef>,
        settings: Settings = DEFAULT_SETTINGS
    ): String {
        val cols = selectedColumnSpecs(config, customFields)
        if (config.module == CsvImportModule.SUBJECT_ALL_TERMS) {
            return buildSubjectAllTermsHeaderRows(config, settings, cols).joinToString("\n")
        }
        return cols.joinToString(config.format.sep) { csvQuoteIfNeeded(it.key, config.format.sep) }
    }

    fun buildBlankTemplateCsv(
        config: CsvPromptConfig,
        students: List<Student>,
        settings: Settings,
        prefillRoster: Boolean = true
    ): String {
        val cols = selectedColumnSpecs(config, settings.customFields)
        val sep = config.format.sep
        val headerLines = if (config.module == CsvImportModule.SUBJECT_ALL_TERMS) {
            buildSubjectAllTermsHeaderRows(config, settings, cols)
        } else {
            listOf(cols.joinToString(sep) { csvQuoteIfNeeded(it.key, sep) })
        }

        val effectiveCls = if (config.targetCls in 1..20) config.targetCls else 3
        val classStudents = if (prefillRoster) {
            students
                .filter { config.targetCls == 0 || it.cls == effectiveCls }
                .sortedWith(compareBy<Student> { it.cls }.thenBy { it.roll })
        } else emptyList()

        val dataLines = if (classStudents.isNotEmpty()) {
            classStudents.map { st ->
                cols.joinToString(sep) { col ->
                    val raw = when (col.key) {
                        "cls" -> st.cls.toString()
                        "roll" -> st.roll.toString()
                        "name" -> st.name
                        "guardian" -> st.guardian
                        "mobile" -> st.mobile
                        "village" -> st.village
                        "union" -> st.union
                        "upazila" -> st.upazila
                        "district" -> st.district
                        "religion" -> st.religion
                        else -> ""
                    }
                    val formatted = if (config.digitMode == CsvDigitMode.BANGLA && raw.isNotBlank() && raw.all { it in '0'..'9' }) {
                        bn(raw)
                    } else raw
                    csvQuoteIfNeeded(formatted, sep)
                }
            }
        } else {
            (1..3).map { idx ->
                cols.joinToString(sep) { col ->
                    val raw = when (col.key) {
                        "cls" -> effectiveCls.toString()
                        "roll" -> idx.toString()
                        else -> ""
                    }
                    val formatted = if (config.digitMode == CsvDigitMode.BANGLA && raw.isNotBlank() && raw.all { it in '0'..'9' }) {
                        bn(raw)
                    } else raw
                    csvQuoteIfNeeded(formatted, sep)
                }
            }
        }
        return (headerLines + dataLines).joinToString("\n")
    }

    fun buildScannedSheetSummaryBn(
        meta: BlankSheetQrMeta,
        settings: Settings = DEFAULT_SETTINGS
    ): String {
        val clsLabel = "${classNameBn(meta.cls.coerceIn(1, 20), settings)} শ্রেণি"
        val cleanSub = meta.subjectKey?.trim()?.uppercase()?.takeIf {
            it.isNotBlank() && it != BlankSheetCatalog.ALL_SUBJECTS_KEY
        }
        val subLabel = cleanSub?.let { k ->
            subjectsFor(meta.cls.coerceIn(1, 20), "ইসলাম").find { it.key == k }?.bn?.substringBefore(" (") ?: k
        }
        return when (meta.sheetKind) {
            ClassSheetKind.THREE_TERM_MARKS -> {
                val hasTotal = meta.columnIds.any { it.endsWith("_TOTAL") }
                val colsDesc = if (hasTotal) "ধারাবাহিক, সামষ্টিক ও মোট" else "ধারাবাহিক ও সামষ্টিক"
                listOfNotNull(clsLabel, subLabel, "সব ৩ প্রান্তিক ($colsDesc)").joinToString(" · ")
            }
            ClassSheetKind.ATTENDANCE_TRAITS -> {
                val termDesc = if (meta.term in 1..3 && meta.columnIds.none { it.startsWith("T2_") || it.startsWith("T3_") }) {
                    termNameBn(meta.term)
                } else {
                    "৩ প্রান্তিক"
                }
                "$clsLabel · উপস্থিতি ও আচরণ ($termDesc)"
            }
            ClassSheetKind.SINGLE_TERM_MARKS -> {
                val tName = termNameBn(meta.term.coerceIn(1, 3))
                val scopeDesc = when (meta.importScope) {
                    ClassSheetImportScope.CONT_ONLY -> "ধারাবাহিক"
                    ClassSheetImportScope.WR_ONLY -> "সামষ্টিক"
                    else -> "নম্বর"
                }
                listOfNotNull(clsLabel, subLabel ?: "বিষয়সমূহ", "$tName ($scopeDesc)").joinToString(" · ")
            }
        }
    }

    fun configFromScannedSheetMeta(
        meta: BlankSheetQrMeta,
        settings: Settings = DEFAULT_SETTINGS,
        currentConfig: CsvPromptConfig = CsvPromptConfig()
    ): CsvPromptConfig {
        val safeCls = meta.cls.coerceIn(1, 20)
        val digitMode = if (meta.numeralStyle == BlankNumeralStyle.ENGLISH) {
            CsvDigitMode.ENGLISH
        } else {
            CsvDigitMode.BANGLA
        }
        val clsSubjects = subjectsFor(safeCls, "ইসলাম")
        val cleanSub = meta.subjectKey?.trim()?.uppercase()?.takeIf {
            it.isNotBlank() && it != BlankSheetCatalog.ALL_SUBJECTS_KEY
        }
        val colSet = meta.columnIds.toSet()

        val notes = buildList {
            if (meta.customColumnTitles.isNotEmpty()) {
                add("শীটের কাস্টম কলাম (${meta.customColumnTitles.joinToString(", ")}) বাদ দিন")
            }
            if (meta.workingDaysByTerm.isNotEmpty()) {
                val wdStr = meta.workingDaysByTerm.toSortedMap().entries.joinToString(", ") { (t, d) ->
                    "${termOrdinalBn(t)}=${if (digitMode == CsvDigitMode.BANGLA) bn(d) else d}"
                }
                add("কার্যদিবস: $wdStr")
            }
        }

        return when (meta.sheetKind) {
            ClassSheetKind.THREE_TERM_MARKS -> {
                val targetSub = cleanSub?.let { k -> clsSubjects.find { it.key == k } } ?: clsSubjects.first()
                val detectedTerms = listOf(1, 2, 3)
                    .filter { t -> meta.columnIds.any { it.startsWith("T${t}_") } }
                    .toSet()
                    .ifEmpty { setOf(1, 2, 3) }
                val cols = linkedSetOf("roll", "name").apply {
                    detectedTerms.sorted().forEach { t ->
                        if ("T${t}_CONT" in colSet) add(mk(targetSub.key, t, "CONT"))
                        if ("T${t}_WR" in colSet && targetSub.written > 0) add(mk(targetSub.key, t, "WR"))
                        if ("T${t}_TOTAL" in colSet) add(mk(targetSub.key, t, "TOTAL"))
                    }
                    if (size <= 2) {
                        addAll(
                            defaultSelectedColumnKeys(
                                module = CsvImportModule.SUBJECT_ALL_TERMS,
                                targetCls = safeCls,
                                targetTerm = 1,
                                customFields = settings.customFields,
                                selectedSubjects = setOf(targetSub.key),
                                allTermsSelection = detectedTerms
                            )
                        )
                    }
                }
                currentConfig.copy(
                    module = CsvImportModule.SUBJECT_ALL_TERMS,
                    targetCls = safeCls,
                    targetTerm = 1,
                    selectedSubjectKeys = setOf(targetSub.key),
                    selectedColumnKeys = cols,
                    digitMode = digitMode,
                    allTermsSelection = detectedTerms,
                    useTwoRowGroupedHeader = true,
                    includeRosterInPrompt = true,
                    extraInstruction = notes.joinToString(" | ")
                )
            }

            ClassSheetKind.ATTENDANCE_TRAITS -> {
                val termColsFound = listOf(1, 2, 3).filter { t ->
                    meta.columnIds.any { it.startsWith("T${t}_") }
                }
                val activeTerms = if (termColsFound.isNotEmpty()) {
                    termColsFound.toSet()
                } else {
                    setOf(meta.term.takeIf { it in 1..3 } ?: 1)
                }
                val hasRemark = "REMARK" in colSet
                val cols = linkedSetOf("cls", "roll", "name").apply {
                    if (termColsFound.isNotEmpty()) {
                        activeTerms.sorted().forEach { t ->
                            if ("T${t}_DAYS" in colSet || meta.workingDaysByTerm.containsKey(t)) add("DAYS_$t")
                            if ("T${t}_PRES" in colSet) add("PRES_$t")
                            if ("T${t}_ACT" in colSet) add("ACT_$t")
                            if ("T${t}_DISC" in colSet) add("DISC_$t")
                            if ("T${t}_COND" in colSet) add("COND_$t")
                            if (hasRemark) add("REM_$t")
                        }
                    } else {
                        val t = activeTerms.first()
                        if ("ATT" in colSet) add("PRES_$t")
                        if (hasRemark) add("REM_$t")
                        if (size <= 3) {
                            addAll(listOf("DAYS_$t", "PRES_$t", "ACT_$t", "DISC_$t", "COND_$t"))
                        }
                    }
                }
                currentConfig.copy(
                    module = CsvImportModule.ATTENDANCE_TRAITS,
                    targetCls = safeCls,
                    targetTerm = activeTerms.first(),
                    multiTermSelection = activeTerms,
                    selectedSubjectKeys = emptySet(),
                    selectedColumnKeys = cols,
                    digitMode = digitMode,
                    includeRosterInPrompt = true,
                    extraInstruction = notes.joinToString(" | ")
                )
            }

            ClassSheetKind.SINGLE_TERM_MARKS -> {
                val safeTerm = meta.term.takeIf { it in 1..3 } ?: 1
                val selectedSubs = if (cleanSub != null && clsSubjects.any { it.key == cleanSub }) {
                    setOf(cleanSub)
                } else {
                    emptySet()
                }
                val subsToMap = if (selectedSubs.isEmpty()) clsSubjects else clsSubjects.filter { it.key in selectedSubs }
                val granularKeys = listOf("TB", "ACT", "LANG", "CT").filter { it in colSet }
                val hasCont = "CONT" in colSet
                val hasWr = "WR" in colSet
                val hasTotal = "TOTAL" in colSet
                val concise = granularKeys.isEmpty()

                val cols = linkedSetOf("cls", "roll", "name").apply {
                    subsToMap.forEach { s ->
                        var addedAny = false
                        granularKeys.forEach { ck ->
                            if (s.comps.any { it.key == ck }) {
                                add(mk(s.key, safeTerm, ck))
                                addedAny = true
                            }
                        }
                        if (hasCont && s.comps.sumOf { it.max } > 0) {
                            add(mk(s.key, safeTerm, "CONT"))
                            addedAny = true
                        }
                        if (hasWr && s.written > 0) {
                            add(mk(s.key, safeTerm, "WR"))
                            addedAny = true
                        }
                        if (hasTotal) {
                            add(mk(s.key, safeTerm, "TOTAL"))
                            addedAny = true
                        }
                        if (!addedAny) {
                            when (meta.importScope) {
                                ClassSheetImportScope.CONT_ONLY -> {
                                    if (s.comps.sumOf { it.max } > 0) add(mk(s.key, safeTerm, "CONT"))
                                }
                                ClassSheetImportScope.WR_ONLY -> {
                                    if (s.written > 0) add(mk(s.key, safeTerm, "WR"))
                                }
                                else -> {
                                    if (s.comps.sumOf { it.max } > 0) add(mk(s.key, safeTerm, "CONT"))
                                    if (s.written > 0) add(mk(s.key, safeTerm, "WR"))
                                }
                            }
                        }
                    }
                }
                currentConfig.copy(
                    module = CsvImportModule.TERM_MARKS,
                    targetCls = safeCls,
                    targetTerm = safeTerm,
                    multiTermSelection = setOf(safeTerm),
                    selectedSubjectKeys = selectedSubs,
                    selectedColumnKeys = cols,
                    conciseMarks = concise,
                    digitMode = digitMode,
                    includeRosterInPrompt = true,
                    extraInstruction = notes.joinToString(" | ")
                )
            }
        }
    }

    fun buildAiPrompt(
        config: CsvPromptConfig,
        settings: Settings,
        students: List<Student> = emptyList()
    ): String {
        val cols = selectedColumnSpecs(config, settings.customFields)
        val sep = config.format.sep
        val headerLines = if (config.module == CsvImportModule.SUBJECT_ALL_TERMS) {
            buildSubjectAllTermsHeaderRows(config, settings, cols)
        } else {
            listOf(cols.joinToString(sep) { csvQuoteIfNeeded(it.key, sep) })
        }
        val headerLine = headerLines.joinToString("\n")
        val sampleRow = cols.joinToString(sep) { col ->
            val rawSample = when (col.key) {
                "cls" -> if (config.targetCls in 1..20) config.targetCls.toString() else "3"
                else -> col.sampleEn
            }
            val formatted = if (config.digitMode == CsvDigitMode.BANGLA && rawSample.all { it in '0'..'9' }) {
                bn(rawSample)
            } else {
                rawSample
            }
            csvQuoteIfNeeded(formatted, sep)
        }

        val activeClasses = activeSchoolClasses(settings)
        val classLabel = if (config.targetCls in 1..20) {
            "${classNameBn(config.targetCls, settings)} শ্রেণি (${config.targetCls})"
        } else {
            activeClasses.joinToString(", ") { "${it.shortBn}=${it.id}" }
        }
        val activeTerms = effectiveTermsForConfig(config)
        val termLabel = when {
            activeTerms.size == 3 -> "সব ৩ প্রান্তিক (১ম, ২য়, ৩য়)"
            activeTerms.size > 1 -> activeTerms.joinToString(", ") { termNameBn(it) }
            else -> termNameBn(activeTerms.firstOrNull() ?: config.targetTerm)
        }

        val rulesSummary = buildList {
            val effectiveCls = if (config.targetCls in 1..20) config.targetCls else 3
            val selectedKeys = cols.map { it.key }.toSet()
            if (config.module == CsvImportModule.SUBJECT_ALL_TERMS) {
                val sub = resolveSubjectForAllTerms(config)
                val contMax = sub.comps.sumOf { it.max }
                add("উপরে শ্রেণি=${classShortBn(effectiveCls, settings)}, বিষয়=${sub.bn.substringBefore(" (")}")
                if (sub.written > 0) {
                    add("পূর্ণমান: ধারাবাহিক≤${contMax}, সামষ্টিক≤${sub.written}")
                } else {
                    add("পূর্ণমান: ধারাবাহিক≤${contMax} (সামষ্টিক পরীক্ষা নেই)")
                }
            } else {
                if (cols.any { it.key == "cls" }) add("cls=$classLabel")
                if (cols.any { it.key == "religion" }) add("religion=ইসলাম/হিন্দু/বৌদ্ধ/খ্রীষ্ট")
                val subs = subjectsFor(effectiveCls, "ইসলাম").filter { sub ->
                    activeTerms.any { t -> selectedKeys.any { it.startsWith("${sub.key}_${t}_") } }
                }
                if (subs.isNotEmpty()) {
                    val sampleTerm = activeTerms.firstOrNull { t ->
                        selectedKeys.any { it.contains("_${t}_") }
                    } ?: config.targetTerm
                    val maxSummary = subs.joinToString("; ") { s ->
                        val parts = buildList {
                            s.comps.forEach { c ->
                                if (mk(s.key, sampleTerm, c.key) in selectedKeys) add("${c.key}≤${c.max}")
                            }
                            if (mk(s.key, sampleTerm, "CONT") in selectedKeys) {
                                add("CONT≤${s.comps.sumOf { it.max }}")
                            }
                            if (s.written > 0 && mk(s.key, sampleTerm, "WR") in selectedKeys) {
                                add("WR≤${s.written}")
                            }
                            if (mk(s.key, sampleTerm, "TOTAL") in selectedKeys) {
                                add("TOTAL≤${s.max}")
                            }
                        }.joinToString(",")
                        "${s.bn.substringBefore(" (")}: $parts"
                    }
                    add("পূর্ণমান [$maxSummary]")
                }
            }
            if (selectedKeys.any { it.startsWith("ACT_") || it.startsWith("DISC_") || it.startsWith("COND_") }) {
                add("ACT/DISC/COND=ক/খ/গ")
            }
            if (config.module == CsvImportModule.ATTENDANCE_TRAITS) {
                add("মোট কার্যদিবস কাগজের উপরে এক জায়গায় থাকলে ১ম সারি বা '১ম কার্যদিবস: ৬২' আকারে দিন অথবা সব সারিতে বসান")
            }
        }.joinToString(" | ")

        val classRoster = if (config.includeRosterInPrompt && config.targetCls in 1..20 && students.isNotEmpty()) {
            students.filter { it.cls == config.targetCls && it.name.isNotBlank() }
                .sortedBy { it.roll }
        } else {
            emptyList()
        }

        return buildString {
            val targetInfo = buildString {
                append("${config.module.titleBn} · $classLabel")
                when (config.module) {
                    CsvImportModule.SUBJECT_ALL_TERMS -> append(" · ${resolveSubjectForAllTerms(config).bn.substringBefore(" (")} · $termLabel")
                    CsvImportModule.STUDENT_PROFILE -> {}
                    else -> append(" · $termLabel")
                }
            }
            appendLine("ছবি/তালিকা থেকে নিচের মতো ডাটা দিন ($targetInfo):")
            appendLine("ফরম্যাট: ${config.format.aiDescBn} | ${config.digitMode.aiRuleBn} | না থাকলে খালি রাখুন (N/A নয়) | কমা থাকলে \"...\" দিন।")
            if (config.module != CsvImportModule.SUBJECT_ALL_TERMS) {
                appendLine("Header: $headerLine")
            }
            if (rulesSummary.isNotBlank()) {
                appendLine("নিয়ম: $rulesSummary")
            }
            if (classRoster.isNotEmpty()) {
                val rosterText = classRoster.joinToString(", ") { "${it.roll}=${it.name.trim()}" }
                appendLine("রোল ও নাম (${classShortBn(config.targetCls, settings)} - বানান হুবহু রাখুন): $rosterText")
            }
            if (config.extraInstruction.isNotBlank()) {
                appendLine("নোট: ${config.extraInstruction.trim()}")
            }
            appendLine("শুধু ```csv ব্লকে আউটপুট দিন। নমুনা:")
            appendLine("```csv")
            appendLine(headerLine)
            appendLine(sampleRow)
            append("```")
        }
    }

    fun validateCsvData(
        parsedRows: List<List<String>>,
        columns: List<Repo.CsvColMapping>,
        existingStudents: List<Student>,
        fallbackCls: Int,
        sharedWorkingDaysByTerm: Map<Int, String> = emptyMap()
    ): CsvValidationReport {
        if (parsedRows.size < 2 || columns.isEmpty()) {
            return CsvValidationReport(
                rows = emptyList(),
                totalRows = 0,
                validAddCount = 0,
                validUpdateCount = 0,
                warningCount = 0,
                errorCount = 0,
                hasMissingRollOrNameMapping = true
            )
        }

        val hasRollOrNameMapping = columns.any {
            it.targetType == "standard" && (it.targetKey == "roll" || it.targetKey == "name")
        }

        val existingPairs = existingStudents.map { it.cls to it.roll }.toSet()
        val seenPairsInCsv = mutableSetOf<Pair<Int, Int>>()
        val duplicatePairsInCsv = mutableSetOf<Pair<Int, Int>>()

        // Pre-pass to detect duplicates inside the CSV itself
        for (r in parsedRows.drop(1)) {
            if (r.all { it.isBlank() }) continue
            var clsVal: Int? = null
            var rollVal: Int? = null
            columns.forEach { col ->
                if (col.targetType == "standard") {
                    val raw = r.getOrNull(col.colIndex)?.trim().orEmpty()
                    if (col.targetKey == "cls" && raw.isNotBlank()) clsVal = Repo.parseClassNum(raw)
                    if (col.targetKey == "roll" && raw.isNotBlank()) rollVal = fromBn(raw).toIntOrNull()?.takeIf { it > 0 }
                }
            }
            val effectiveCls = clsVal ?: fallbackCls
            if (effectiveCls in 1..20 && rollVal != null) {
                val pair = effectiveCls to rollVal!!
                if (!seenPairsInCsv.add(pair)) duplicatePairsInCsv.add(pair)
            }
        }

        val validatedRows = mutableListOf<CsvValidatedRow>()
        val dataRows = parsedRows.drop(1)

        dataRows.forEachIndexed { idx, r ->
            if (r.all { it.isBlank() }) return@forEachIndexed
            val lineNumber = idx + 2
            val messages = mutableListOf<String>()
            var hasError = false
            var hasWarning = false

            var rawClsText = ""
            var parsedCls: Int? = null
            var rawRollText = ""
            var parsedRoll: Int? = null
            val stdMap = mutableMapOf<String, String>()
            val previewItems = mutableListOf<String>()
            var parsedMarksCount = 0
            val termDays = mutableMapOf<Int, Int>()
            val termPres = mutableMapOf<Int, Int>()

            columns.forEach { col ->
                if (col.targetType == "skip") return@forEach
                val raw = r.getOrNull(col.colIndex)?.trim().orEmpty()
                if (raw.isBlank()) return@forEach
                when (col.targetType) {
                    "standard" -> when (col.targetKey) {
                        "cls" -> {
                            rawClsText = raw
                            parsedCls = Repo.parseClassNum(raw)
                            if (parsedCls == null) {
                                hasError = true
                                messages.add("অবৈধ শ্রেণি: \"$raw\"")
                            }
                        }
                        "roll" -> {
                            rawRollText = raw
                            parsedRoll = fromBn(raw).toIntOrNull()?.takeIf { it > 0 }
                            if (parsedRoll == null) {
                                hasError = true
                                messages.add("অবৈধ রোল নম্বর: \"$raw\"")
                            }
                        }
                        else -> {
                            stdMap[col.targetKey] = raw
                        }
                    }

                    "mark" -> {
                        if (!Repo.isBlankOrDashPlaceholder(raw)) {
                            val num = fromBn(raw).toDoubleOrNull()
                            if (num == null) {
                                hasWarning = true
                                messages.add("${col.targetKey} এর মান \"$raw\" সংখ্যা নয় (বাদ যাবে)")
                            } else {
                                parsedMarksCount++
                                if (num < 0) {
                                    hasWarning = true
                                    messages.add("${col.targetKey} ঋণাত্মক নম্বর (${fmt(num)})")
                                }
                                if (previewItems.size < 3) {
                                    previewItems.add("${col.targetKey}=${fmt(num)}")
                                }
                            }
                        }
                    }

                    "text" -> {
                        val parts = col.targetKey.split("_")
                        val prefix = parts.getOrNull(0).orEmpty()
                        val term = parts.getOrNull(1)?.toIntOrNull()
                        when (prefix) {
                            "DAYS" -> {
                                val d = fromBn(raw).toIntOrNull()
                                if (d == null || d < 0) {
                                    hasWarning = true
                                    messages.add("${col.targetKey} সংখ্যা নয়: \"$raw\"")
                                } else if (term != null) termDays[term] = d
                            }
                            "PRES" -> {
                                val p = fromBn(raw).toIntOrNull()
                                if (p == null || p < 0) {
                                    hasWarning = true
                                    messages.add("${col.targetKey} সংখ্যা নয়: \"$raw\"")
                                } else if (term != null) termPres[term] = p
                            }
                            "ACT", "DISC", "COND" -> {
                                if (raw !in listOf("ক", "খ", "গ")) {
                                    hasWarning = true
                                    messages.add("${col.targetKey} মান \"$raw\" (প্রত্যাশিত: ক, খ, গ)")
                                }
                            }
                        }
                        if (previewItems.size < 3) {
                            previewItems.add("${col.targetKey}=$raw")
                        }
                    }

                    "custom", "create_new" -> {
                        if (previewItems.size < 3) {
                            val lbl = if (col.targetType == "create_new") col.newLabel else col.header
                            previewItems.add("$lbl: $raw")
                        }
                    }
                }
            }

            val effectiveCls = parsedCls ?: if (rawClsText.isBlank()) fallbackCls else null
            if (effectiveCls == null || effectiveCls !in 1..20) {
                hasError = true
                if (rawClsText.isBlank()) messages.add("বৈধ শ্রেণি নির্বাচন করুন")
            }

            val studentName = stdMap["name"].orEmpty()
            if (parsedRoll == null && studentName.isBlank()) {
                hasError = true
                messages.add("রোল ও নাম উভয়ই ফাঁকা — সারি ইমপোর্ট করা যাবে না")
            } else if (parsedRoll == null && rawRollText.isBlank()) {
                hasWarning = true
                messages.add("রোল ফাঁকা — স্বয়ংক্রিয় রোল বরাদ্দ হবে")
            }

            // Check mark max limits against effectiveCls
            if (effectiveCls != null && effectiveCls in 1..20) {
                val clsSubjects = subjectsFor(effectiveCls, stdMap["religion"] ?: "ইসলাম")
                columns.filter { it.targetType == "mark" }.forEach { col ->
                    val raw = r.getOrNull(col.colIndex)?.trim().orEmpty()
                    if (Repo.isBlankOrDashPlaceholder(raw)) return@forEach
                    val num = fromBn(raw).toDoubleOrNull() ?: return@forEach
                    val parts = col.targetKey.split("_")
                    if (parts.size == 3) {
                        val subKey = parts[0]
                        val compKey = parts[2]
                        val sub = clsSubjects.find { it.key == subKey }
                        if (sub == null) {
                            hasWarning = true
                            messages.add("${classShortBn(effectiveCls)} শ্রেণিতে \"$subKey\" বিষয় নেই")
                        } else {
                            val maxMark = when (compKey) {
                                "WR" -> sub.written
                                "CONT" -> sub.comps.sumOf { it.max }
                                "TOTAL" -> sub.max
                                else -> sub.comps.find { it.key == compKey }?.max ?: 0
                            }
                            if (maxMark <= 0) {
                                if (num > 0.0) {
                                    hasWarning = true
                                    messages.add("${sub.bn.substringBefore(" (")} বিষয়ে সামষ্টিক পরীক্ষা নেই (শুধু ধারাবাহিক)")
                                }
                            } else if (num > maxMark) {
                                hasWarning = true
                                messages.add("${col.targetKey} (${fmt(num)}) সর্বোচ্চ সীমা ${bn(maxMark.toString())}-এর বেশি")
                            }
                        }
                    }
                }
            }

            sharedWorkingDaysByTerm.forEach { (termNum, rawDays) ->
                if (termNum in 1..3 && !termDays.containsKey(termNum)) {
                    fromBn(rawDays).filter { it.isDigit() }.toIntOrNull()?.let { termDays[termNum] = it }
                }
            }

            // Check attendance consistency
            termPres.forEach { (term, pres) ->
                val days = termDays[term]
                if (days != null && pres > days) {
                    hasWarning = true
                    messages.add("${termNameBn(term)}ে উপস্থিতি ($pres) কার্যদিবসের ($days) বেশি")
                }
            }

            // Check mobile & religion sanity
            stdMap["mobile"]?.let { mob ->
                val digits = fromBn(mob).filter { it.isDigit() }
                if (digits.length !in 10..14) {
                    hasWarning = true
                    messages.add("মোবাইল নম্বর যাচাই করুন: $mob")
                }
            }
            stdMap["religion"]?.let { rel ->
                if (rel !in RELIGIONS) {
                    hasWarning = true
                    messages.add("অপ্রচলিত ধর্ম \"$rel\" (আদর্শ: ${RELIGIONS.joinToString("/")})")
                }
            }

            val isUpdate = effectiveCls != null && parsedRoll != null && existingPairs.contains(effectiveCls to parsedRoll!!)
            if (!isUpdate && !hasError && studentName.isBlank()) {
                hasWarning = true
                messages.add("নতুন শিক্ষার্থীর নাম ফাঁকা")
            }
            if (effectiveCls != null && parsedRoll != null && duplicatePairsInCsv.contains(effectiveCls to parsedRoll!!)) {
                hasWarning = true
                messages.add("CSV-তে একই শ্রেণি ও রোল একাধিকবার আছে")
            }

            stdMap["guardian"]?.let { if (previewItems.size < 3) previewItems.add("অভিভাবক: $it") }
            stdMap["union"]?.let { if (previewItems.size < 3) previewItems.add("ইউনিয়ন: $it") }
            stdMap["mobile"]?.let { if (previewItems.size < 3) previewItems.add("মোবাইল: $it") }

            val status = when {
                hasError -> CsvRowStatus.ERROR
                hasWarning -> CsvRowStatus.WARNING
                else -> CsvRowStatus.VALID
            }

            validatedRows.add(
                CsvValidatedRow(
                    dataRowIndex = idx,
                    lineNumber = lineNumber,
                    status = status,
                    isUpdate = isUpdate,
                    resolvedCls = effectiveCls,
                    resolvedRoll = parsedRoll,
                    resolvedName = studentName.ifBlank {
                        existingStudents.find { it.cls == effectiveCls && it.roll == parsedRoll }?.name.orEmpty()
                    },
                    parsedMarksCount = parsedMarksCount,
                    parsedFieldsPreview = previewItems.joinToString(" · "),
                    messages = messages.distinct(),
                    rawCells = r
                )
            )
        }

        val usableRows = validatedRows.filter { it.status != CsvRowStatus.ERROR }
        return CsvValidationReport(
            rows = validatedRows,
            totalRows = validatedRows.size,
            validAddCount = usableRows.count { !it.isUpdate },
            validUpdateCount = usableRows.count { it.isUpdate },
            warningCount = validatedRows.count { it.status == CsvRowStatus.WARNING },
            errorCount = validatedRows.count { it.status == CsvRowStatus.ERROR },
            hasMissingRollOrNameMapping = !hasRollOrNameMapping
        )
    }

    fun buildFilteredImportSummary(
        parsedRows: List<List<String>>,
        columns: List<Repo.CsvColMapping>,
        selectedDataRowIndices: Set<Int>,
        existingStudents: List<Student>,
        existingSettings: Settings,
        fallbackCls: Int,
        sharedWorkingDaysByTerm: Map<Int, String> = emptyMap()
    ): Repo.CsvImportSummary? {
        if (parsedRows.size < 2 || columns.isEmpty()) return null
        val header = parsedRows.first()
        val dataRows = parsedRows.drop(1)
        val filteredRows = dataRows.filterIndexed { idx, _ -> selectedDataRowIndices.contains(idx) }
        if (filteredRows.isEmpty()) return null
        return Repo.applyMappedCsvImport(
            rows = listOf(header) + filteredRows,
            columns = columns,
            existingStudents = existingStudents,
            existingSettings = existingSettings,
            defaultCls = fallbackCls,
            sharedWorkingDaysByTerm = sharedWorkingDaysByTerm
        )
    }
}
