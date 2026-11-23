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

enum class BlankBottomCodeStyle(val labelBn: String) {
    THIN_QR("নিচে ছোট QR"),
    CODE_128("নিচে বারকোড")
}

data class BlankSheetColumn(
    val id: String,
    val titleBn: String,
    val widthWeight: Float = 1f,
    val fillWith: BlankColFill = BlankColFill.NONE,
    val align: BlankColAlign = BlankColAlign.CENTER,
    val compKey: String? = null,
    val isCustom: Boolean = false,
    val groupBn: String? = null,
    val pickerLabelBn: String = titleBn
)

enum class BlankSheetPreset(val labelBn: String, val columnIds: List<String>) {
    THREE_TERM_MARKS(
        "৩ প্রান্তিক নম্বর",
        listOf(
            "ROLL", "NAME",
            "T1_CONT", "T1_WR",
            "T2_CONT", "T2_WR",
            "T3_CONT", "T3_WR"
        )
    ),
    THREE_TERM_ATTENDANCE_TRAITS(
        "৩ প্রান্তিক উপস্থিতি ও গুণাবলি",
        listOf(
            "ROLL", "NAME",
            "T1_PRES", "T1_ACT", "T1_DISC", "T1_COND",
            "T2_PRES", "T2_ACT", "T2_DISC", "T2_COND",
            "T3_PRES", "T3_ACT", "T3_DISC", "T3_COND"
        )
    ),
    SINGLE_TERM_ATTENDANCE_TRAITS(
        "উপস্থিতি ও গুণাবলি",
        listOf("ROLL", "NAME", "ATT", "TRAIT_ACT", "TRAIT_DISC", "TRAIT_COND")
    ),
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

    val THREE_TERM_MARK_IDS: Set<String> = setOf(
        "T1_CONT", "T1_WR", "T1_TOTAL",
        "T2_CONT", "T2_WR", "T2_TOTAL",
        "T3_CONT", "T3_WR", "T3_TOTAL"
    )

    val THREE_TERM_TOTAL_IDS: Set<String> = setOf("T1_TOTAL", "T2_TOTAL", "T3_TOTAL")

    val THREE_TERM_TRAIT_IDS: Set<String> = setOf(
        "T1_DAYS", "T1_PRES", "T1_ACT", "T1_DISC", "T1_COND",
        "T2_DAYS", "T2_PRES", "T2_ACT", "T2_DISC", "T2_COND",
        "T3_DAYS", "T3_PRES", "T3_ACT", "T3_DISC", "T3_COND"
    )

    val SINGLE_TERM_TRAIT_IDS: Set<String> = setOf("TRAIT_ACT", "TRAIT_DISC", "TRAIT_COND")

    private fun threeTermMarkColsFor(term: Int): List<BlankSheetColumn> {
        val tOrd = termOrdinalBn(term)
        val grp = termNameBn(term)
        return listOf(
            BlankSheetColumn("T${term}_CONT", "ধারাবাহিক", widthWeight = 1.00f, compKey = "CONT", groupBn = grp, pickerLabelBn = "$tOrd-ধারাবাহিক"),
            BlankSheetColumn("T${term}_WR", "সামষ্টিক", widthWeight = 0.92f, compKey = "WR", groupBn = grp, pickerLabelBn = "$tOrd-সামষ্টিক"),
            BlankSheetColumn("T${term}_TOTAL", "মোট", widthWeight = 0.84f, compKey = "TOTAL", groupBn = grp, pickerLabelBn = "$tOrd-মোট")
        )
    }

    private fun threeTermTraitColsFor(term: Int): List<BlankSheetColumn> {
        val tOrd = termOrdinalBn(term)
        val grp = termNameBn(term)
        return listOf(
            BlankSheetColumn("T${term}_DAYS", "কার্যদিবস", widthWeight = 0.78f, groupBn = grp, pickerLabelBn = "$tOrd-কার্যদিবস"),
            BlankSheetColumn("T${term}_PRES", "উপস্থিতি", widthWeight = 0.78f, groupBn = grp, pickerLabelBn = "$tOrd-উপস্থিতি"),
            BlankSheetColumn("T${term}_ACT", "সক্রিয়তা", widthWeight = 0.85f, groupBn = grp, pickerLabelBn = "$tOrd-সক্রিয়তা"),
            BlankSheetColumn("T${term}_DISC", "শৃঙ্খলা", widthWeight = 0.82f, groupBn = grp, pickerLabelBn = "$tOrd-শৃঙ্খলা"),
            BlankSheetColumn("T${term}_COND", "আচরণ", widthWeight = 0.82f, groupBn = grp, pickerLabelBn = "$tOrd-আচরণ")
        )
    }

    val ALL_COLUMNS: List<BlankSheetColumn> = listOf(
        BlankSheetColumn("ROLL", "রোল", widthWeight = 0.65f, fillWith = BlankColFill.ROLL, align = BlankColAlign.CENTER),
        BlankSheetColumn("NAME", "নাম", widthWeight = 2.35f, fillWith = BlankColFill.NAME, align = BlankColAlign.LEFT),
        BlankSheetColumn("TB", "পাঠ্যবই", widthWeight = 0.90f, compKey = "TB"),
        BlankSheetColumn("ACT", "কাজ", widthWeight = 0.80f, compKey = "ACT"),
        BlankSheetColumn("LANG", "ভাষা", widthWeight = 0.80f, compKey = "LANG"),
        BlankSheetColumn("CT", "অভীক্ষা", widthWeight = 0.90f, compKey = "CT"),
        BlankSheetColumn("CONT", "ধারাবাহিক", widthWeight = 1.05f, compKey = "CONT"),
        BlankSheetColumn("WR", "লিখিত", widthWeight = 0.88f, compKey = "WR"),
        BlankSheetColumn("TOTAL", "মোট", widthWeight = 0.88f, compKey = "TOTAL")
    ) +
        threeTermMarkColsFor(1) + threeTermMarkColsFor(2) + threeTermMarkColsFor(3) +
        threeTermTraitColsFor(1) + threeTermTraitColsFor(2) + threeTermTraitColsFor(3) +
        listOf(
            BlankSheetColumn("GRADE", "গ্রেড", widthWeight = 0.78f),
            BlankSheetColumn("GP", "জিপি", widthWeight = 0.72f),
            BlankSheetColumn("MERIT", "মেধাক্রম", widthWeight = 0.88f),
            BlankSheetColumn("ATT", "উপস্থিতি", widthWeight = 0.90f),
            BlankSheetColumn("TRAIT_ACT", "সক্রিয়তা", widthWeight = 0.90f, pickerLabelBn = "সহপাঠ/সক্রিয়তা"),
            BlankSheetColumn("TRAIT_DISC", "শৃঙ্খলা", widthWeight = 0.88f),
            BlankSheetColumn("TRAIT_COND", "আচরণ", widthWeight = 0.88f),
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

    fun threeTermMarksColumns(includeTotal: Boolean): List<BlankSheetColumn> {
        val ids = buildList {
            add("ROLL")
            add("NAME")
            for (t in 1..3) {
                add("T${t}_CONT")
                add("T${t}_WR")
                if (includeTotal) add("T${t}_TOTAL")
            }
        }
        return ids.mapNotNull { byId(it) }
    }

    fun isThreeTermMarksActive(columns: List<BlankSheetColumn>): Boolean =
        columns.any { it.id in THREE_TERM_MARK_IDS }

    fun hasThreeTermTotals(columns: List<BlankSheetColumn>): Boolean =
        columns.any { it.id in THREE_TERM_TOTAL_IDS }

    fun isThreeTermAttendanceTraitsActive(columns: List<BlankSheetColumn>): Boolean =
        columns.any { it.id in THREE_TERM_TRAIT_IDS }

    fun hasSingleTermTraitColumns(columns: List<BlankSheetColumn>): Boolean =
        columns.any { it.id in SINGLE_TERM_TRAIT_IDS }

    fun isSingleTermAttendanceTraitsActive(columns: List<BlankSheetColumn>): Boolean =
        !isThreeTermMarksActive(columns) &&
            !isThreeTermAttendanceTraitsActive(columns) &&
            columns.none { it.compKey != null } &&
            (hasSingleTermTraitColumns(columns) || columns.any { it.id == "ATT" })

    fun isThreeTermGroupedActive(columns: List<BlankSheetColumn>): Boolean =
        isThreeTermMarksActive(columns) || isThreeTermAttendanceTraitsActive(columns)

    fun toggleThreeTermTotals(columns: List<BlankSheetColumn>, includeTotal: Boolean): List<BlankSheetColumn> {
        val withoutTotals = columns.filterNot { it.id in THREE_TERM_TOTAL_IDS }
        if (!includeTotal) return withoutTotals
        val out = mutableListOf<BlankSheetColumn>()
        val catalogIdx = ALL_COLUMNS.mapIndexed { idx, col -> col.id to idx }.toMap()
        val extraTotals = mutableListOf<BlankSheetColumn>()
        for (t in 1..3) {
            val hasTermCol = withoutTotals.any { it.id.startsWith("T${t}_") }
            if (hasTermCol) byId("T${t}_TOTAL")?.let { extraTotals.add(it) }
        }
        out.addAll(withoutTotals)
        extraTotals.forEach { totCol ->
            val targetPos = catalogIdx[totCol.id] ?: return@forEach
            val insertAt = out.indexOfFirst { (catalogIdx[it.id] ?: Int.MAX_VALUE) > targetPos }
            if (insertAt < 0) out.add(totCol) else out.add(insertAt, totCol)
        }
        return out
    }

    fun pickerColumnsFor(current: List<BlankSheetColumn>): List<BlankSheetColumn> = when {
        isThreeTermAttendanceTraitsActive(current) -> ALL_COLUMNS.filter { col ->
            col.id in setOf("ROLL", "NAME", "REMARK", "SIGN") || col.id in THREE_TERM_TRAIT_IDS
        }
        isThreeTermMarksActive(current) -> ALL_COLUMNS.filter { col ->
            col.id in setOf("ROLL", "NAME", "GRADE", "GP", "MERIT", "REMARK", "SIGN") || col.id in THREE_TERM_MARK_IDS
        }
        else -> ALL_COLUMNS.filter { col ->
            col.id !in THREE_TERM_MARK_IDS && col.id !in THREE_TERM_TRAIT_IDS
        }
    }

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

    fun resolveCommonWorkingDaysByTerm(
        students: List<Student>,
        selectedClass: Int?
    ): Map<Int, String> {
        val pool = if (selectedClass != null) {
            students.filter { it.cls == selectedClass }.ifEmpty { students }
        } else {
            students
        }
        val out = linkedMapOf<Int, String>()
        for (t in 1..3) {
            val counts = pool.mapNotNull { st ->
                val clean = fromBn(st.x["DAYS_$t"].orEmpty()).filter { it.isDigit() }
                clean.takeIf { it.isNotEmpty() }
            }.groupingBy { it }.eachCount()
            val top = counts.maxByOrNull { it.value }?.key
            if (!top.isNullOrBlank()) out[t] = top
        }
        return out
    }
    fun buildSheetRefCode(
        cls: Int,
        subjectKey: String?,
        term: Int,
        columns: List<BlankSheetColumn>
    ): String {
        val is3TMarks = isThreeTermMarksActive(columns)
        val is3TTraits = isThreeTermAttendanceTraitsActive(columns)
        val hasAtt = columns.any { it.id == "ATT" }
        val hasSingleTraits = hasSingleTermTraitColumns(columns)
        val hasMarks = columns.any { it.compKey != null }
        val subPart = when {
            is3TTraits && !is3TMarks && !hasMarks -> "ATT"
            !is3TTraits && (hasAtt || hasSingleTraits) && !hasMarks -> "ATT"
            !subjectKey.isNullOrBlank() && subjectKey != ALL_SUBJECTS_KEY -> subjectKey
            else -> "GEN"
        }
        val termPart = when {
            is3TMarks || is3TTraits -> "3T"
            term in 1..3 -> "T$term"
            term == 0 -> "ANN"
            else -> "TC"
        }
        val layoutPart = when {
            is3TTraits && !is3TMarks -> "TRAITS"
            is3TMarks -> "3MARKS"
            !is3TTraits && hasSingleTraits && !hasMarks -> "TRAITS"
            hasAtt && !hasMarks -> "ATT"
            columns.any { it.compKey in setOf("TB", "ACT", "LANG", "CT") } -> "FULL"
            columns.any { it.compKey in setOf("CONT", "WR") } -> "COMPACT"
            columns.any { it.compKey == "TOTAL" } -> "TOTAL"
            else -> "CUSTOM"
        }
        return "REF:C$cls-$subPart-$termPart-$layoutPart"
    }

    fun buildExternalAiCsvPrompt(
        config: BlankSheetConfig,
        settings: Settings,
        allowedClassIds: List<Int>
    ): String {
        val cols = config.columns.ifEmpty { DEFAULT_COLUMNS }
        val is3TMarks = isThreeTermMarksActive(cols)
        val is3TTraits = isThreeTermAttendanceTraitsActive(cols)
        val hasMarks = cols.any { it.compKey != null }
        val hasSingleTraits = hasSingleTermTraitColumns(cols) && !is3TTraits && !hasMarks
        val hasAttSingle = (cols.any { it.id == "ATT" } || hasSingleTraits) && !is3TTraits && !hasMarks
        val clsLabel = config.selectedClass?.let { "${classNameBn(it, settings)} শ্রেণি ($it)" }
            ?: "ছবির উপরের ব্যানার অনুযায়ী শ্রেণি (${allowedClassIds.joinToString("/")})"
        val termNum = if (config.term in 1..3) config.term else 1
        val subName = subjectTitleFor(config.subjectKey, config.customSubjectTitle).ifBlank { "ছবি অনুযায়ী" }
        val subKeyLabel = config.subjectKey?.takeIf { it != ALL_SUBJECTS_KEY } ?: "AUTO"

        val headers = mutableListOf<String>()
        if (!is3TMarks) headers.add("শ্রেণি")
        headers.addAll(listOf("রোল নম্বর", "শিক্ষার্থীর নাম"))

        cols.forEach { col ->
            when (col.id) {
                "ROLL", "NAME", "GRADE", "GP", "MERIT", "SIGN" -> {}
                "TB" -> headers.add("পাঠ্যবই")
                "ACT" -> headers.add("কাজ")
                "LANG" -> headers.add("ভাষা")
                "CT" -> headers.add("অভীক্ষা")
                "CONT" -> headers.add("ধারাবাহিক")
                "WR" -> headers.add("লিখিত")
                "TOTAL" -> headers.add("মোট")
                "ATT" -> headers.add("${termNameBn(termNum)} - মোট উপস্থিতি")
                "TRAIT_ACT" -> headers.add("${termNameBn(termNum)} - সহপাঠক্রমিক")
                "TRAIT_DISC" -> headers.add("${termNameBn(termNum)} - শৃঙ্খলাবোধ")
                "TRAIT_COND" -> headers.add("${termNameBn(termNum)} - আচরণ")
                "REMARK" -> headers.add("${termNameBn(termNum)} - শিক্ষকের মন্তব্য")
                "T1_CONT" -> headers.add("১ম প্রান্তিক - ধারাবাহিক")
                "T1_WR" -> headers.add("১ম প্রান্তিক - সামষ্টিক")
                "T1_TOTAL" -> headers.add("১ম প্রান্তিক - মোট")
                "T2_CONT" -> headers.add("২য় প্রান্তিক - ধারাবাহিক")
                "T2_WR" -> headers.add("২য় প্রান্তিক - সামষ্টিক")
                "T2_TOTAL" -> headers.add("২য় প্রান্তিক - মোট")
                "T3_CONT" -> headers.add("৩য় প্রান্তিক - ধারাবাহিক")
                "T3_WR" -> headers.add("৩য় প্রান্তিক - সামষ্টিক")
                "T3_TOTAL" -> headers.add("৩য় প্রান্তিক - মোট")
                "T1_DAYS" -> headers.add("১ম প্রান্তিক - মোট কার্যদিবস")
                "T1_PRES" -> headers.add("১ম প্রান্তিক - মোট উপস্থিতি")
                "T1_ACT" -> headers.add("১ম প্রান্তিক - সহপাঠক্রমিক")
                "T1_DISC" -> headers.add("১ম প্রান্তিক - শৃঙ্খলাবোধ")
                "T1_COND" -> headers.add("১ম প্রান্তিক - আচরণ")
                "T2_DAYS" -> headers.add("২য় প্রান্তিক - মোট কার্যদিবস")
                "T2_PRES" -> headers.add("২য় প্রান্তিক - মোট উপস্থিতি")
                "T2_ACT" -> headers.add("২য় প্রান্তিক - সহপাঠক্রমিক")
                "T2_DISC" -> headers.add("২য় প্রান্তিক - শৃঙ্খলাবোধ")
                "T2_COND" -> headers.add("২য় প্রান্তিক - আচরণ")
                "T3_DAYS" -> headers.add("৩য় প্রান্তিক - মোট কার্যদিবস")
                "T3_PRES" -> headers.add("৩য় প্রান্তিক - মোট উপস্থিতি")
                "T3_ACT" -> headers.add("৩য় প্রান্তিক - সহপাঠক্রমিক")
                "T3_DISC" -> headers.add("৩য় প্রান্তিক - শৃঙ্খলাবোধ")
                "T3_COND" -> headers.add("৩য় প্রান্তিক - আচরণ")
                else -> if (col.isCustom) headers.add(col.titleBn)
            }
        }

        val effectiveSubLabel = if (is3TTraits || hasAttSingle) "উপস্থিতি ও গুণাবলি (ATT)" else "$subName ($subKeyLabel)"
        val effectiveMetaSub = if (is3TTraits || hasAttSingle) "উপস্থিতি ও গুণাবলি" else subName

        return buildString {
            appendLine("আমি একটি প্রাথমিক বিদ্যালয়ের পূরণকৃত নম্বর/উপস্থিতি ফর্মের ছবি/PDF দিচ্ছি। ছবির সব সারির তথ্য হুবহু পড়ে কেবল একটি ```csv কোড ব্লকের ভেতরে CSV দিন।")
            appendLine("তথ্য: শ্রেণি: $clsLabel | বিষয়: $effectiveSubLabel | প্রান্তিক: ${if (is3TMarks || is3TTraits) "৩ প্রান্তিক" else termNameBn(termNum)}")
            appendLine("নিয়ম:")
            appendLine("১) প্রথম সারিতে মেটা-তথ্য হিসেবে লিখুন: শ্রেণি: ${config.selectedClass ?: "<ছবি থেকে>"}, বিষয়: $effectiveMetaSub, প্রান্তিক: ${if (is3TMarks || is3TTraits) "৩ প্রান্তিক" else termNameBn(termNum)}")
            if (is3TTraits || hasAttSingle) {
                appendLine("২) ছবির উপরে 'মোট কার্যদিবস' লেখা থাকলে দ্বিতীয় সারিতে 'মোট কার্যদিবস — ১ম প্রান্তিক: <দিন> দিন ...' লিখুন। গুণাবলির (সক্রিয়তা/সহপাঠ, শৃঙ্খলা, আচরণ) ঘরে কেবল ক (উত্তম), খ (সন্তোষজনক) বা গ (উন্নতি প্রয়োজন) দিন।")
            }
            appendLine("৩) এরপর টেবিলের হেডার হুবহু এই কলামে রাখুন:")
            appendLine(headers.joinToString(","))
            appendLine("৪) সব সংখ্যা ইংরেজি অঙ্কে (0-9) দিন; ফাঁকা ঘর ফাঁকা রাখুন এবং গ্রেড/জিপি/স্বাক্ষর কলাম বাদ দিন।")
            if (config.includeQrCode || config.includeBarcode) {
                appendLine("৫) ছবির উপরে বাম/ডান কোণায় থাকা QR কোড বা নিচের বারকোডে কলাম-ক্রম ও ফর্মের মেটাডেটা দেওয়া আছে — প্রয়োজনে সেটিও মিলিয়ে নিন।")
                append("৬) কেবল ```csv ... ``` ব্লকের ভেতর আউটপুট দিন, বাড়তি ব্যাখ্যা নয়।")
            } else {
                append("৫) কেবল ```csv ... ``` ব্লকের ভেতর আউটপুট দিন, বাড়তি ব্যাখ্যা নয়।")
            }
        }.trim()
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
    val fillRemainingPageRows: Boolean = false,
    val showSharedWorkingDaysHeader: Boolean = true,
    val workingDaysByTerm: Map<Int, String> = emptyMap(),
    val includeQrCode: Boolean = true,
    val includeBarcode: Boolean = true,
    val bottomCodeStyle: BlankBottomCodeStyle = BlankBottomCodeStyle.THIN_QR
)

data class BlankSheetRow(
    val rollText: String,
    val nameText: String
)

data class BlankSheetPageSpec(
    val cls: Int,
    val term: Int = 1,
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
    val numeralStyle: BlankNumeralStyle = BlankNumeralStyle.BENGALI,
    val isClassLevelForm: Boolean = false,
    val sharedWorkingDaysTerms: List<Int> = emptyList(),
    val workingDaysByTerm: Map<Int, String> = emptyMap(),
    val sheetRefCode: String = "",
    val templateId: String = "",
    val thinQrPayload: String = "",
    val qrPayload: String = "",
    val bottomThinQrPayload: String = "",
    val bottomCodeStyle: BlankBottomCodeStyle = BlankBottomCodeStyle.THIN_QR,
    val barcodeText: String = ""
) {
    val showSharedWorkingDaysStrip: Boolean
        get() = sharedWorkingDaysTerms.isNotEmpty()
}

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
        val isThreeTermMarks = BlankSheetCatalog.isThreeTermMarksActive(safeCols)
        val isThreeTermTraits = BlankSheetCatalog.isThreeTermAttendanceTraitsActive(safeCols)
        val isSingleTermTraits = BlankSheetCatalog.isSingleTermAttendanceTraitsActive(safeCols)
        val isClassLevelTraitsOnly = (isThreeTermTraits || isSingleTermTraits) && !isThreeTermMarks && safeCols.none { it.compKey != null }
        val hasPerRowDaysCol = safeCols.any { it.id in setOf("T1_DAYS", "T2_DAYS", "T3_DAYS") }
        val hasSingleAttCol = safeCols.any { it.id == "ATT" } || BlankSheetCatalog.hasSingleTermTraitColumns(safeCols)
        val sharedWorkingDaysTerms = if (config.showSharedWorkingDaysHeader) {
            when {
                isThreeTermTraits && !hasPerRowDaysCol -> listOf(1, 2, 3)
                !isThreeTermTraits && hasSingleAttCol && config.term in 1..3 -> listOf(config.term)
                else -> emptyList()
            }
        } else {
            emptyList()
        }
        val termTitle = if (isThreeTermMarks || isThreeTermTraits) {
            "৩ প্রান্তিক"
        } else {
            BlankSheetCatalog.termLabelFor(config.term, config.customTermTitle)
        }
        val isAllSubjects = config.subjectKey == BlankSheetCatalog.ALL_SUBJECTS_KEY && !isClassLevelTraitsOnly

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

            val subjectPairs: List<Pair<String?, String>> = when {
                isClassLevelTraitsOnly -> listOf(null to "উপস্থিতি ও গুণাবলি")
                isAllSubjects -> subjectsFor(cls, "ইসলাম").map { sub ->
                    sub.key to BlankSheetCatalog.shortSubjectTitleForSub(sub)
                }
                else -> listOf(config.subjectKey to BlankSheetCatalog.subjectTitleFor(config.subjectKey, config.customSubjectTitle))
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
            val baseSpec = BlankSheetPageSpec(
                cls = raw.cls,
                term = config.term,
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
                showSubjectMaxInHeader = if (isClassLevelTraitsOnly) false else config.showSubjectMaxInHeader,
                subjectKey = raw.subjectKey,
                numeralStyle = config.numeralStyle,
                isClassLevelForm = isClassLevelTraitsOnly,
                sharedWorkingDaysTerms = sharedWorkingDaysTerms,
                workingDaysByTerm = config.workingDaysByTerm,
                sheetRefCode = BlankSheetCatalog.buildSheetRefCode(
                    cls = raw.cls,
                    subjectKey = raw.subjectKey,
                    term = config.term,
                    columns = safeCols
                )
            )
            val meta = SheetQrCodec.fromPageConfig(baseSpec, config.term)
            val thinPayload = SheetQrCodec.encodeThinTemplateQr(meta.templateId)
            baseSpec.copy(
                templateId = meta.templateId,
                thinQrPayload = if (config.includeQrCode) thinPayload else "",
                qrPayload = if (config.includeQrCode) SheetQrCodec.encodePayload(meta) else "",
                bottomThinQrPayload = if (config.includeBarcode && config.bottomCodeStyle == BlankBottomCodeStyle.THIN_QR) thinPayload else "",
                bottomCodeStyle = config.bottomCodeStyle,
                barcodeText = if (config.includeBarcode) {
                    SheetQrCodec.encodeBarcodeText(
                        cls = raw.cls,
                        subjectKey = raw.subjectKey,
                        term = config.term,
                        columns = safeCols,
                        templateId = meta.templateId
                    )
                } else ""
            )
        }
    }
}
