package com.kgps.result

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
        subtitleBn = "শ্রেণি ও প্রান্তিক অনুযায়ী মূল্যায়ন নম্বর"
    ),
    ATTENDANCE_TRAITS(
        id = "attendance",
        titleBn = "উপস্থিতি ও আচরণ",
        subtitleBn = "কার্যদিবস, উপস্থিতি, শৃঙ্খলা ও মন্তব্য"
    ),
    FULL_RECORD(
        id = "full",
        titleBn = "সম্পূর্ণ সম্মিলিত CSV",
        subtitleBn = "প্রোফাইল + প্রান্তিক নম্বর + উপস্থিতি ও আচরণ"
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
    val extraInstruction: String = ""
)

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
    selectedSubjects: Set<String> = emptySet()
): Set<String> {
    val effectiveCls = if (targetCls in 1..20) targetCls else 3
    val subs = subjectsFor(effectiveCls, "ইসলাম").let { all ->
        if (selectedSubjects.isEmpty()) all else all.filter { selectedSubjects.contains(it.key) }
    }
    return when (module) {
        CsvImportModule.STUDENT_PROFILE -> linkedSetOf(
            "cls", "roll", "name", "guardian", "mobile", "village", "religion"
        ).apply {
            addAll(customFields.map { it.label })
        }

        CsvImportModule.TERM_MARKS -> linkedSetOf("cls", "roll", "name").apply {
            subs.forEach { s ->
                s.comps.forEach { c -> add(mk(s.key, targetTerm, c.key)) }
                if (s.written > 0) add(mk(s.key, targetTerm, "WR"))
            }
        }

        CsvImportModule.ATTENDANCE_TRAITS -> linkedSetOf(
            "cls", "roll", "name",
            "DAYS_$targetTerm", "PRES_$targetTerm",
            "ACT_$targetTerm", "DISC_$targetTerm", "COND_$targetTerm", "REM_$targetTerm"
        )

        CsvImportModule.FULL_RECORD -> linkedSetOf(
            "cls", "roll", "name", "guardian", "mobile", "village", "union", "upazila", "district", "religion"
        ).apply {
            addAll(customFields.map { it.label })
            subs.forEach { s ->
                s.comps.forEach { c -> add(mk(s.key, targetTerm, c.key)) }
                if (s.written > 0) add(mk(s.key, targetTerm, "WR"))
            }
            addAll(
                listOf(
                    "DAYS_$targetTerm", "PRES_$targetTerm",
                    "ACT_$targetTerm", "DISC_$targetTerm", "COND_$targetTerm", "REM_$targetTerm"
                )
            )
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

    private fun attendanceColumns(term: Int): List<CsvColumnOption> = listOf(
        CsvColumnOption("DAYS_$term", "মোট কার্যদিবস (DAYS_$term)", "উপস্থিতি ও আচরণ", false, "62", "পূর্ণসংখ্যা (যেমন 62)"),
        CsvColumnOption("PRES_$term", "মোট উপস্থিতি (PRES_$term)", "উপস্থিতি ও আচরণ", false, "59", "কার্যদিবসের সমান বা কম"),
        CsvColumnOption("ACT_$term", "সহপাঠক্রমিক (ACT_$term)", "উপস্থিতি ও আচরণ", false, "ক", "কেবল ক, খ বা গ"),
        CsvColumnOption("DISC_$term", "শৃঙ্খলাবোধ (DISC_$term)", "উপস্থিতি ও আচরণ", false, "ক", "কেবল ক, খ বা গ"),
        CsvColumnOption("COND_$term", "আচরণ (COND_$term)", "উপস্থিতি ও আচরণ", false, "ক", "কেবল ক, খ বা গ"),
        CsvColumnOption("REM_$term", "শিক্ষকের মন্তব্য (REM_$term)", "উপস্থিতি ও আচরণ", false, "পাঠ্যবিষয়ে মনোযোগী ও নিয়মিত।", "সংক্ষিপ্ত মূল্যায়ন মন্তব্য")
    )

    private fun markColumns(effectiveCls: Int, term: Int, selectedSubjects: Set<String>): List<CsvColumnOption> {
        val subs = subjectsFor(effectiveCls, "ইসলাম").let { list ->
            if (selectedSubjects.isEmpty()) list else list.filter { selectedSubjects.contains(it.key) }
        }
        val out = mutableListOf<CsvColumnOption>()
        subs.forEach { s ->
            val subShort = s.bn.substringBefore(" (")
            s.comps.forEach { c ->
                val sample = ((c.max * 8) / 10).coerceAtLeast(1).toString()
                val compLabel = if (s.key == "ART" && c.key == "TOTAL") "মোট" else c.key
                out.add(
                    CsvColumnOption(
                        key = mk(s.key, term, c.key),
                        labelBn = "$subShort · $compLabel (সর্বোচ্চ ${bn(c.max.toString())})",
                        group = "নম্বর ($subShort)",
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
                        labelBn = "$subShort · মোট ধারাবাহিক (সর্বোচ্চ ${bn(contMax.toString())})",
                        group = "নম্বর ($subShort)",
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
                        labelBn = "$subShort · WR লিখিত (সর্বোচ্চ ${bn(s.written.toString())})",
                        group = "নম্বর ($subShort)",
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
                    labelBn = "$subShort · মোট (সর্বোচ্চ ${bn(s.max.toString())})",
                    group = "নম্বর ($subShort)",
                    required = false,
                    sampleEn = totalSample,
                    ruleHint = "0 থেকে ${s.max}"
                )
            )
        }
        return out
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
                list.addAll(markColumns(effectiveCls, config.targetTerm, config.selectedSubjectKeys))
            }

            CsvImportModule.ATTENDANCE_TRAITS -> {
                list.addAll(PROFILE_COLUMNS.take(3)) // cls, roll, name
                list.addAll(attendanceColumns(config.targetTerm))
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
                list.addAll(markColumns(effectiveCls, config.targetTerm, config.selectedSubjectKeys))
                list.addAll(attendanceColumns(config.targetTerm))
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

    fun buildHeaderLine(config: CsvPromptConfig, customFields: List<CustomFieldDef>): String {
        val cols = selectedColumnSpecs(config, customFields)
        return cols.joinToString(config.format.sep) { csvQuoteIfNeeded(it.key, config.format.sep) }
    }

    fun buildAiPrompt(config: CsvPromptConfig, settings: Settings): String {
        val cols = selectedColumnSpecs(config, settings.customFields)
        val sep = config.format.sep
        val headerLine = cols.joinToString(sep) { csvQuoteIfNeeded(it.key, sep) }
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
        val termLabel = termNameBn(config.targetTerm)

        val rulesSummary = buildList {
            val effectiveCls = if (config.targetCls in 1..20) config.targetCls else 3
            val selectedKeys = cols.map { it.key }.toSet()
            if (cols.any { it.key == "cls" }) add("cls=$classLabel")
            if (cols.any { it.key == "religion" }) add("religion=ইসলাম/হিন্দু/বৌদ্ধ/খ্রীষ্ট")
            val subs = subjectsFor(effectiveCls, "ইসলাম").filter { sub ->
                selectedKeys.any { it.startsWith("${sub.key}_${config.targetTerm}_") }
            }
            if (subs.isNotEmpty()) {
                val maxSummary = subs.joinToString("; ") { s ->
                    val parts = buildList {
                        s.comps.forEach { c ->
                            if (mk(s.key, config.targetTerm, c.key) in selectedKeys) add("${c.key}≤${c.max}")
                        }
                        if (mk(s.key, config.targetTerm, "CONT") in selectedKeys) {
                            add("CONT≤${s.comps.sumOf { it.max }}")
                        }
                        if (s.written > 0 && mk(s.key, config.targetTerm, "WR") in selectedKeys) {
                            add("WR≤${s.written}")
                        }
                        if (mk(s.key, config.targetTerm, "TOTAL") in selectedKeys) {
                            add("TOTAL≤${s.max}")
                        }
                    }.joinToString(",")
                    "${s.bn.substringBefore(" (")}: $parts"
                }
                add("পূর্ণমান [$maxSummary]")
            }
            if (selectedKeys.any { it.startsWith("ACT_") || it.startsWith("DISC_") || it.startsWith("COND_") }) {
                add("ACT/DISC/COND=ক/খ/গ")
            }
        }.joinToString(" | ")

        return buildString {
            val targetInfo = buildString {
                append("${config.module.titleBn} · $classLabel")
                if (config.module != CsvImportModule.STUDENT_PROFILE) append(" · $termLabel")
            }
            appendLine("ছবি/তালিকা থেকে নিচের মতো ডাটা দিন ($targetInfo):")
            appendLine("ফরম্যাট: ${config.format.aiDescBn} | ${config.digitMode.aiRuleBn} | না থাকলে খালি রাখুন (N/A নয়) | কমা থাকলে \"...\" দিন।")
            appendLine("Header: $headerLine")
            if (rulesSummary.isNotBlank()) {
                appendLine("নিয়ম: $rulesSummary")
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
        fallbackCls: Int
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
                                hasWarning = true
                                messages.add("${sub.bn.substringBefore(" (")} বিষয়ে $compKey অংশ প্রযোজ্য নয়")
                            } else if (num > maxMark) {
                                hasWarning = true
                                messages.add("${col.targetKey} (${fmt(num)}) সর্বোচ্চ সীমা ${bn(maxMark.toString())}-এর বেশি")
                            }
                        }
                    }
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
        fallbackCls: Int
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
            defaultCls = fallbackCls
        )
    }
}
