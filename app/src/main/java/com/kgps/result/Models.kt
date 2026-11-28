package com.kgps.result

const val DEFAULT_UPAZILA = "আলফাডাঙ্গা"
const val DEFAULT_DISTRICT = "ফরিদপুর"

data class Student(
    val id: String,
    val cls: Int,
    val roll: Int,
    val name: String,
    val guardian: String = "",
    val village: String = "",
    val union: String = "",
    val post: String = "",
    val upazila: String = DEFAULT_UPAZILA,
    val district: String = DEFAULT_DISTRICT,
    val mobile: String = "",
    val religion: String = "ইসলাম",
    val m: Map<String, Double> = emptyMap(),   // নম্বর: BAN_1_TB, BAN_1_WR, ART_2_TOTAL ...
    val x: Map<String, String> = emptyMap()    // DAYS_1, PRES_1, ACT_1, DISC_1, COND_1, REM_1 ...
)

data class CustomFieldDef(
    val key: String,
    val label: String
)

data class CustomClassDef(
    val id: Int,
    val nameBn: String,
    val shortBn: String = "",
    val syllabusTier: Int = 3 // 1 = ১ম–২য় ধরন (সমন্বিত), 3 = ৩য়–৫ম ধরন (বিজ্ঞান ও বাওবি)
)

data class SchoolClassDef(
    val id: Int,
    val nameBn: String,
    val shortBn: String,
    val syllabusTier: Int,
    val isCustom: Boolean
)

data class Settings(
    val school: String = "",
    val schoolAddress: String = "",
    val year: String = "২০২৬",
    val teachers: Map<Int, String> = emptyMap(),
    val teacherMobiles: Map<Int, String> = emptyMap(),
    val pdfFont: String = "noto_serif",
    val pdfEnglishFont: String = "times_new_roman",
    val dpeLogo: String = "simple",
    val customFields: List<CustomFieldDef> = emptyList(),
    val customClasses: List<CustomClassDef> = emptyList(),
    val resultRules: ResultEvaluationRules = ResultEvaluationRules()
)

data class DpeLogoInfo(val id: String, val labelBn: String, val descriptionBn: String)

val DPE_LOGO_LIST = listOf(
    DpeLogoInfo("simple", "সাদাকালো লোগো (Simple)", "সাদাকালো ফটোকপি ও সাধারণ প্রিন্টের জন্য স্পষ্ট লাইন-আর্ট লোগো"),
    DpeLogoInfo("color", "রঙিন লোগো (Color)", "রঙিন মুদ্রণ ও ডিজিটাল প্রদর্শনের জন্য মূল রঙিন লোগো")
)

data class PdfFontInfo(val id: String, val labelBn: String, val descriptionBn: String)

val BENGALI_PDF_FONT_LIST = listOf(
    PdfFontInfo("noto_serif", "নোতো সেরিফ বাংলা (প্রাতিষ্ঠানিক)", "সরকারি নথি ও আনুষ্ঠানিক প্রিন্টের জন্য আদর্শ সেরিফ ফন্ট"),
    PdfFontInfo("nikosh", "নিকষ (Nikosh — সরকারি দাপ্তরিক)", "সরকারি প্রজ্ঞাপন, দাপ্তরিক নথি ও ফর্মের জনপ্রিয় অফিশিয়াল ফন্ট"),
    PdfFontInfo("sutonny_mj", "সুতন্নী এমজে (SutonnyMJ / OMJ)", "ছাপাখানা, অফিস শিট ও ঐতিহ্যবাহী দাপ্তরিক কাগজপত্রের শৈলী"),
    PdfFontInfo("kalpurush", "কালপুরুষ (Kalpurush)", "স্পষ্ট, সুষম ও আরামদায়ক পাঠযোগ্যতার জনপ্রিয় ইউনিকোড ফন্ট"),
    PdfFontInfo("solaiman_lipi", "সোলাইমান লিপি (SolaimanLipi)", "প্রতিবেদন, নোটিশ ও দাপ্তরিক মুদ্রণের সুপরিচিত বাংলা ফন্ট"),
    PdfFontInfo("tiro_bangla", "তিরো বাংলা (সরকারি নথি ও সনদ)", "মুদ্রিত বই, প্রশংসাপত্র ও মার্জিত কভার শিরোনামের জন্য ধ্রুপদী সেরিফ"),
    PdfFontInfo("noto_sans", "নোতো স্যান্স বাংলা (স্ট্যান্ডার্ড)", "পরিচ্ছন্ন ও আধুনিক ডিজিটাল ফরম্যাট"),
    PdfFontInfo("hind_siliguri", "হিন্দ শিলিগুড়ি (পরিচ্ছন্ন টেবিল)", "নম্বর টেবিল ও সংখ্যার স্পষ্ট পাঠযোগ্যতার জন্য আধুনিক স্যান্স ফন্ট"),
    PdfFontInfo("anek_bangla", "অনেক বাংলা (আধুনিক দাপ্তরিক)", "আধুনিক দাপ্তরিক শিট, নম্বরপত্র ও পরিচ্ছন্ন প্রিন্টের জন্য উপযোগী"),
    PdfFontInfo("mina", "মীনা বাংলা (স্পষ্ট জ্যামিতিক)", "সহজ, স্পষ্ট ও সমানুপাতিক জ্যামিতিক গড়নের অফিশিয়াল বাংলা ফন্ট"),
    PdfFontInfo("baloo_da_2", "বালু দা ২ (মসৃণ ও প্রাঞ্জল)", "আকর্ষণীয় কভার শিরোনাম ও প্রাঞ্জল পাঠযোগ্যতার জন্য")
)

val ENGLISH_PDF_FONT_LIST = listOf(
    PdfFontInfo("times_new_roman", "টাইমস নিউ রোমান (Times New Roman)", "সরকারি, একাডেমিক ও আন্তর্জাতিক মানের অফিসিয়াল ডকুমেন্ট ফন্ট"),
    PdfFontInfo("tinos", "টিনোস (Tinos — টাইমস স্টাইল)", "Times New Roman ঘরানার বিকল্প ওপেন-সোর্স অফিসিয়াল ডকুমেন্ট ফন্ট"),
    PdfFontInfo("arimo", "অ্যারিমো / এরিয়াল স্টাইল (Arimo)", "Arial ঘরানার পরিচ্ছন্ন দাপ্তরিক ফর্ম, কোড ও নম্বরপত্র ফন্ট"),
    PdfFontInfo("roboto", "রোবোটো (Roboto — আধুনিক পরিচ্ছন্ন)", "আধুনিক, স্পষ্ট ও সুষম ইংরেজি সংখ্যা ও টেক্সট ফন্ট"),
    PdfFontInfo("merriweather", "মেরিওয়েদার (Merriweather — সেরিফ)", "ইংরেজি রিপোর্ট কার্ড, নম্বরপত্র ও প্রাতিষ্ঠানিক ডকুমেন্টের ক্লাসিক সেরিফ"),
    PdfFontInfo("plus_jakarta_sans", "প্লাস জাকার্তা স্যান্স (Plus Jakarta Sans)", "আধুনিক ও মার্জিত জ্যামিতিক ইংরেজি ডকুমেন্ট ফন্ট")
)

val PDF_FONT_LIST = BENGALI_PDF_FONT_LIST + ENGLISH_PDF_FONT_LIST

val PDF_FONTS = PDF_FONT_LIST.map { it.id to it.labelBn }

val BENGALI_APP_FONT_LIST = listOf(
    PdfFontInfo("system", "সিস্টেম ডিফল্ট (ডিভাইস বাংলা ফন্ট)", "অ্যান্ড্রয়েড ডিভাইসের নিজস্ব স্বাভাবিক বাংলা ফন্ট"),
    PdfFontInfo("noto_sans", "নোতো স্যান্স বাংলা (স্ট্যান্ডার্ড)", "অ্যাপের মেনু, তালিকা ও বাটনে পরিচ্ছন্ন ও দ্রুত পাঠযোগ্য ফন্ট"),
    PdfFontInfo("nikosh", "নিকষ (Nikosh — সরকারি দাপ্তরিক)", "সরকারি অফিশিয়াল ফন্টে ড্যাশবোর্ড ও সকল লেখা প্রদর্শন"),
    PdfFontInfo("kalpurush", "কালপুরুষ (Kalpurush)", "চোখের আরামদায়ক ও ক্লাসিক ইউনিকোড বাংলা ইন্টারফেস"),
    PdfFontInfo("solaiman_lipi", "সোলাইমান লিপি (SolaimanLipi)", "সুপরিচিত প্রচলিত বাংলা ছাপায় পুরো অ্যাপ প্রদর্শন"),
    PdfFontInfo("sutonny_mj", "সুতন্নী এমজে (SutonnyMJ / OMJ)", "ঐতিহ্যবাহী সুতন্নী অক্ষরশৈলীতে অ্যাপের লেখা প্রদর্শন"),
    PdfFontInfo("hind_siliguri", "হিন্দ শিলিগুড়ি (স্পষ্ট সংখ্যা ও তালিকা)", "ড্যাশবোর্ড, টেবিল ও নম্বর এন্ট্রিতে স্পষ্ট দৃশ্যমানতা"),
    PdfFontInfo("anek_bangla", "অনেক বাংলা (আধুনিক দাপ্তরিক UI)", "আধুনিক সরকারি অ্যাপ ও কার্ড লেআউটের জন্য আদর্শ ফন্ট"),
    PdfFontInfo("mina", "মীনা বাংলা (সহজ জ্যামিতিক)", "হালকা, পরিচ্ছন্ন ও আরামদায়ক পঠন অভিজ্ঞতার জন্য"),
    PdfFontInfo("baloo_da_2", "বালু দা ২ (মসৃণ ও বন্ধুত্বপূর্ণ)", "মসৃণ শিরোনাম ও নরম গোলাকার অক্ষরশৈলী"),
    PdfFontInfo("noto_serif", "নোতো সেরিফ বাংলা (প্রাতিষ্ঠানিক)", "সারা অ্যাপেই দাপ্তরিক ও বইয়ের মতো সেরিফ আবহ"),
    PdfFontInfo("tiro_bangla", "তিরো বাংলা (ধ্রুপদী বাংলা)", "মার্জিত ধ্রুপদী বাংলা অক্ষরশৈলীতে অ্যাপ প্রদর্শন")
)

val ENGLISH_APP_FONT_LIST = listOf(
    PdfFontInfo("system", "সিস্টেম ডিফল্ট (ডিভাইস ইংরেজি ফন্ট)", "অ্যান্ড্রয়েড ডিভাইসের নিজস্ব স্বাভাবিক ইংরেজি ফন্ট"),
    PdfFontInfo("roboto", "রোবোটো (Roboto — অফিসিয়াল UI)", "অ্যান্ড্রয়েড ও গুগলের অফিসিয়াল মানসম্মত ইংরেজি ইন্টারফেস ফন্ট"),
    PdfFontInfo("times_new_roman", "টাইমস নিউ রোমান (Times New Roman)", "ক্লাসিক অফিসিয়াল সেরিফ শৈলীতে অ্যাপের ইংরেজি লেখা ও সংখ্যা"),
    PdfFontInfo("arimo", "অ্যারিমো / এরিয়াল স্টাইল (Arimo)", "Arial ঘরানার পরিচ্ছন্ন দাপ্তরিক ইংরেজি UI ও নম্বর এন্ট্রি"),
    PdfFontInfo("plus_jakarta_sans", "প্লাস জাকার্তা স্যান্স (Plus Jakarta Sans)", "ইংরেজি লেখা, সংখ্যা ও আধুনিক অ্যাপ ইন্টারফেসের পরিচ্ছন্ন UI ফন্ট"),
    PdfFontInfo("tinos", "টিনোস (Tinos — টাইমস স্টাইল)", "Times New Roman ঘরানার স্পষ্ট সেরিফ ইংরেজি UI"),
    PdfFontInfo("merriweather", "মেরিওয়েদার (Merriweather — সেরিফ)", "ক্লাসিক সেরিফ শৈলীতে অ্যাপের ইংরেজি ও সংখ্যা")
)

val APP_FONT_LIST = BENGALI_APP_FONT_LIST + ENGLISH_APP_FONT_LIST.filter { it.id != "system" }

data class Comp(val key: String, val bn: String, val max: Int)
data class Sub(val key: String, val bn: String, val max: Int, val comps: List<Comp>, val written: Int)

val RELIGIONS = listOf("ইসলাম", "হিন্দু", "বৌদ্ধ", "খ্রীষ্ট")
val CLSN = listOf("প্রথম", "দ্বিতীয়", "তৃতীয়", "চতুর্থ", "পঞ্চম")
val CLS_SHORT = listOf("১ম", "২য়", "৩য়", "৪র্থ", "৫ম")

val DEFAULT_SCHOOL_CLASSES: List<SchoolClassDef> = listOf(
    SchoolClassDef(1, "প্রথম", "১ম", 1, isCustom = false),
    SchoolClassDef(2, "দ্বিতীয়", "২য়", 1, isCustom = false),
    SchoolClassDef(3, "তৃতীয়", "৩য়", 3, isCustom = false),
    SchoolClassDef(4, "চতুর্থ", "৪র্থ", 3, isCustom = false),
    SchoolClassDef(5, "পঞ্চম", "৫ম", 3, isCustom = false)
)

object SchoolClassRegistry {
    @Volatile
    private var customList: List<CustomClassDef> = emptyList()

    fun update(custom: List<CustomClassDef>) {
        customList = custom
            .filter { it.id in 6..20 && it.nameBn.isNotBlank() }
            .distinctBy { it.id }
            .sortedBy { it.id }
    }

    fun currentCustom(): List<CustomClassDef> = customList

    fun activeClasses(custom: List<CustomClassDef> = customList): List<SchoolClassDef> {
        val extras = custom
            .filter { it.id in 6..20 && it.nameBn.isNotBlank() }
            .distinctBy { it.id }
            .sortedBy { it.id }
            .map { cc ->
                SchoolClassDef(
                    id = cc.id,
                    nameBn = cc.nameBn.trim(),
                    shortBn = cc.shortBn.trim().ifBlank { defaultShortClassLabel(cc.nameBn, cc.id) },
                    syllabusTier = if (cc.syllabusTier <= 2) 1 else 3,
                    isCustom = true
                )
            }
        return DEFAULT_SCHOOL_CLASSES + extras
    }

    fun activeIds(custom: List<CustomClassDef> = customList): List<Int> =
        activeClasses(custom).map { it.id }

    fun findClass(cls: Int, custom: List<CustomClassDef> = customList): SchoolClassDef? =
        activeClasses(custom).find { it.id == cls }
}

fun defaultShortClassLabel(nameBn: String, id: Int): String {
    val clean = nameBn.trim()
        .removeSuffix("শ্রেণি")
        .removeSuffix("শ্রেণী")
        .trim()
    return when {
        clean.contains("প্রাক") || clean.contains("শিশু") -> "প্রাক"
        clean.isNotBlank() && clean.length <= 6 -> clean
        clean.isNotBlank() -> clean.take(5)
        else -> ordinalBn(id)
    }
}

fun activeSchoolClasses(settings: Settings? = null): List<SchoolClassDef> =
    if (settings != null) SchoolClassRegistry.activeClasses(settings.customClasses)
    else SchoolClassRegistry.activeClasses()

fun activeClassIds(settings: Settings? = null): List<Int> =
    activeSchoolClasses(settings).map { it.id }

fun classNameBn(cls: Int, settings: Settings? = null): String {
    val found = if (settings != null) {
        SchoolClassRegistry.findClass(cls, settings.customClasses)
    } else {
        SchoolClassRegistry.findClass(cls)
    }
    return found?.nameBn ?: CLSN.getOrNull(cls - 1) ?: bn(cls.toString())
}

fun classShortBn(cls: Int, settings: Settings? = null): String {
    val found = if (settings != null) {
        SchoolClassRegistry.findClass(cls, settings.customClasses)
    } else {
        SchoolClassRegistry.findClass(cls)
    }
    return found?.shortBn ?: CLS_SHORT.getOrNull(cls - 1) ?: ordinalBn(cls)
}

fun syllabusTierForClass(cls: Int, settings: Settings? = null): Int {
    if (cls <= 2) return 1
    if (cls <= 5) return 3
    val found = if (settings != null) {
        SchoolClassRegistry.findClass(cls, settings.customClasses)
    } else {
        SchoolClassRegistry.findClass(cls)
    }
    return found?.syllabusTier ?: 3
}

fun nextAvailableCustomClassId(customClasses: List<CustomClassDef>): Int? {
    val used = customClasses.map { it.id }.toSet()
    return (6..20).firstOrNull { it !in used }
}

private fun comps(tb: Int, ct: Int) = listOf(
    Comp("TB", "পাঠ্যপুস্তক/শিক্ষক সহায়িকার কাজ", tb),
    Comp("ACT", "শ্রেণিকাজে সক্রিয়তা", 5),
    Comp("LANG", "ভাষাদক্ষতা/বোধগম্যতা", 5),
    Comp("CT", "ক্লাস টেস্ট", ct)
)

@JvmOverloads
fun subjectsFor(cls: Int, religion: String, settings: Settings? = null): List<Sub> {
    val low = syllabusTierForClass(cls, settings) <= 2
    fun full(k: String, n: String) =
        if (low) Sub(k, n, 100, comps(20, 20), 50) else Sub(k, n, 100, comps(10, 10), 70)
    fun half(k: String, n: String) = Sub(k, n, 50, comps(20, 20), 0)
    val rel = "ধর্মশিক্ষা ($religion)"
    return if (low) listOf(
        full("BAN", "বাংলা"),
        full("ENG", "ইংরেজি"),
        full("MATH", "গণিত"),
        half("SOC", "সামাজিক বিজ্ঞান ও বিজ্ঞান (সমন্বিত)"),
        half("REL", rel),
        half("PE", "শারীরিক ও মানসিক স্বাস্থ্য শিক্ষা"),
        half("ART", "শিল্পকলা")
    ) else listOf(
        full("BAN", "বাংলা"),
        full("ENG", "ইংরেজি"),
        full("MATH", "গণিত"),
        full("BGW", "বাংলাদেশ ও বিশ্বপরিচয়"),
        full("SCI", "বিজ্ঞান"),
        full("REL", rel),
        half("PE", "শারীরিক ও মানসিক স্বাস্থ্য শিক্ষা"),
        half("ART", "শিল্পকলা")
    )
}

// ---------- হিসাব ----------
fun mk(sub: String, t: Int, c: String) = "${sub}_${t}_$c"

fun bn(s: String): String = s.map { if (it in '0'..'9') '০' + (it - '0') else it }.joinToString("")
fun ordinalBn(n: Int): String = when (n) {
    1 -> "১ম"
    2 -> "২য়"
    3 -> "৩য়"
    4 -> "৪র্থ"
    5 -> "৫ম"
    6 -> "৬ষ্ঠ"
    else -> "${bn(n.toString())}ম"
}
fun termOrdinalBn(term: Int): String = ordinalBn(term)
fun termNameBn(term: Int): String = "${termOrdinalBn(term)} প্রান্তিক"
fun fromBn(s: String): String = s.map { if (it in '০'..'৯') '0' + (it - '০') else it }.joinToString("")
fun fmtEn(d: Double): String { val r = Math.round(d * 10) / 10.0; return if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString() }
fun fmt(d: Double): String = bn(fmtEn(d))

fun Student.subs() = subjectsFor(cls, religion)
fun Student.hasTerm(t: Int) = m.keys.any { it.contains("_${t}_") } || hasTermAbsent(t)
fun Student.hasSub(s: Sub, t: Int) = m.keys.any { it.startsWith("${s.key}_${t}_") } || hasSubAbsent(s, t)
fun Student.cont(s: Sub, t: Int): Double {
    if (isCompAbsent(s.key, t, "CONT")) return 0.0
    val hasBreakdown = s.comps.any { m.containsKey(mk(s.key, t, it.key)) }
    return if (hasBreakdown) {
        s.comps.sumOf { m[mk(s.key, t, it.key)] ?: 0.0 }
    } else {
        m[mk(s.key, t, "CONT")]
            ?: (if (s.key == "ART" || s.written == 0) m[mk(s.key, t, "TOTAL")] else null)
            ?: 0.0
    }
}
fun Student.wr(s: Sub, t: Int) =
    if (s.written > 0 && !isCompAbsent(s.key, t, "WR")) (m[mk(s.key, t, "WR")] ?: 0.0) else 0.0
fun Student.total(s: Sub, t: Int): Double {
    val hasParts = s.comps.any { m.containsKey(mk(s.key, t, it.key)) } ||
        m.containsKey(mk(s.key, t, "CONT")) ||
        (s.written > 0 && m.containsKey(mk(s.key, t, "WR")))
    return if (hasParts) {
        cont(s, t) + wr(s, t)
    } else {
        if (isCompAbsent(s.key, t, "CONT") && (s.written == 0 || isCompAbsent(s.key, t, "WR"))) 0.0
        else m[mk(s.key, t, "TOTAL")] ?: 0.0
    }
}
fun Student.avg(s: Sub, activeTerms: List<Int>): Double {
    if (activeTerms.isEmpty()) return 0.0
    return activeTerms.sumOf { total(s, it) } / activeTerms.size.toDouble()
}
fun Student.avg(s: Sub): Double {
    val active = ResultRulesEngine.activeTermsForStudent(this, dynamicTermAveraging = true)
    return if (active.isEmpty()) 0.0 else avg(s, active)
}
fun gradeOf(p: Double) = when { p >= 80 -> "A"; p >= 60 -> "B"; p >= 40 -> "C"; else -> "D" }
fun gradeLabelBn(g: String) = when (g) {
    "A" -> "অতি উত্তম"
    "B" -> "উত্তম"
    "C" -> "সন্তোষজনক"
    else -> "সহায়তা প্রয়োজন"
}
fun Student.overallPct(activeTerms: List<Int>): Double {
    if (activeTerms.isEmpty()) return 0.0
    val ss = subs()
    val maxSum = ss.sumOf { it.max }
    return if (maxSum == 0) 0.0 else ss.sumOf { avg(it, activeTerms) } / maxSum * 100.0
}
fun Student.overallPct(): Double {
    val active = ResultRulesEngine.activeTermsForStudent(this, dynamicTermAveraging = true)
    return overallPct(active)
}

fun createEmptyStudent(cls: Int, roll: Int, religion: String = "ইসলাম"): Student = Student(
    id = java.util.UUID.randomUUID().toString(),
    cls = cls.coerceIn(1, 20),
    roll = roll.coerceAtLeast(1),
    name = "",
    religion = religion
)

enum class ReportSummaryColumn(
    val id: String,
    val headerBn: String,
    val compactHeaderBn: String,
    val shortLabelBn: String,
    val sampleValueBn: String,
    val tableWeight: Float
) {
    PERCENTAGE(
        id = "PERCENTAGE",
        headerBn = "প্রাপ্ত মোট নম্বরের\nশতকরা হার",
        compactHeaderBn = "মোট নম্বরের\nশতকরা হার",
        shortLabelBn = "শতকরা হার",
        sampleValueBn = "৮৫%",
        tableWeight = 1.05f
    ),
    GRADE_POSITION(
        id = "GRADE_POSITION",
        headerBn = "অবস্থানগত মান",
        compactHeaderBn = "অবস্থানগত\nমান",
        shortLabelBn = "অবস্থানগত মান",
        sampleValueBn = "অতি উত্তম",
        tableWeight = 1.22f
    ),
    MERIT_RANK(
        id = "MERIT_RANK",
        headerBn = "শ্রেণিতে মেধাক্রম",
        compactHeaderBn = "শ্রেণিতে\nমেধাক্রম",
        shortLabelBn = "মেধাক্রম",
        sampleValueBn = "১",
        tableWeight = 0.92f
    ),
    OVERALL_GRADE(
        id = "OVERALL_GRADE",
        headerBn = "সামগ্রিক গ্রেড",
        compactHeaderBn = "সামগ্রিক\nগ্রেড",
        shortLabelBn = "সমন্বিত গ্রেড",
        sampleValueBn = "A",
        tableWeight = 0.80f
    ),
    ATTENDANCE_RATE(
        id = "ATTENDANCE_RATE",
        headerBn = "গড় উপস্থিতির হার",
        compactHeaderBn = "গড় উপস্থিতির\nহার",
        shortLabelBn = "উপস্থিতির হার",
        sampleValueBn = "৯৫%",
        tableWeight = 1.00f
    ),
    TOTAL_AVERAGE(
        id = "TOTAL_AVERAGE",
        headerBn = "প্রাপ্ত গড় মোট\nনম্বর",
        compactHeaderBn = "প্রাপ্ত গড়\nমোট নম্বর",
        shortLabelBn = "গড় মোট নম্বর",
        sampleValueBn = "৫৫০/৬৫০",
        tableWeight = 1.05f
    ),
    PROMOTION_STATUS(
        id = "PROMOTION_STATUS",
        headerBn = "ফলাফল\nঅবস্থা",
        compactHeaderBn = "ফলাফল\nঅবস্থা",
        shortLabelBn = "উত্তীর্ণ/অবস্থা",
        sampleValueBn = "উত্তীর্ণ",
        tableWeight = 0.96f
    );

    companion object {
        val DEFAULT_SELECTED: List<ReportSummaryColumn> = listOf(
            PERCENTAGE,
            GRADE_POSITION,
            MERIT_RANK
        )

        fun serializeList(cols: List<ReportSummaryColumn>): String =
            cols.distinct().joinToString(",") { it.id }

        fun deserializeList(raw: String?): List<ReportSummaryColumn> {
            if (raw.isNullOrBlank()) return DEFAULT_SELECTED
            val parsed = raw.split(",")
                .mapNotNull { token -> entries.find { it.id == token.trim() } }
                .distinct()
            return parsed.ifEmpty { DEFAULT_SELECTED }
        }
    }
}

val DEFAULT_SETTINGS = Settings(
    school = "কামারখালী সরকারি প্রাথমিক বিদ্যালয়",
    schoolAddress = "আলফাডাঙ্গা, ফরিদপুর",
    year = "২০২৬",
    teachers = mapOf(
        1 to "মোসাঃ তাসলিমা খাতুন",
        2 to "মোঃ রফিকুল ইসলাম",
        3 to "শারমিন আক্তার",
        4 to "মোঃ আকিবুল ইসলাম",
        5 to "নাসরিন সুলতানা"
    ),
    teacherMobiles = mapOf(
        1 to "01712345601",
        2 to "01712345602",
        3 to "01712345603",
        4 to "01712345604",
        5 to "01712345605"
    ),
    pdfFont = "noto_serif",
    pdfEnglishFont = "times_new_roman"
)

val SAMPLE_STUDENTS = listOf(
    Student(
        id = "sample-1-1",
        cls = 1,
        roll = 1,
        name = "তানভীর আহমেদ",
        guardian = "মোঃ করিম উদ্দিন",
        village = "কামারখালী",
        post = "",
        upazila = DEFAULT_UPAZILA,
        district = DEFAULT_DISTRICT,
        mobile = "01711223344",
        religion = "ইসলাম",
        m = mapOf(
            "BAN_1_TB" to 18.0, "BAN_1_ACT" to 5.0, "BAN_1_LANG" to 4.0, "BAN_1_CT" to 17.0, "BAN_1_WR" to 44.0,
            "ENG_1_TB" to 17.0, "ENG_1_ACT" to 4.0, "ENG_1_LANG" to 4.0, "ENG_1_CT" to 16.0, "ENG_1_WR" to 41.0,
            "MATH_1_TB" to 19.0, "MATH_1_ACT" to 5.0, "MATH_1_LANG" to 5.0, "MATH_1_CT" to 18.0, "MATH_1_WR" to 46.0,
            "SOC_1_TB" to 18.0, "SOC_1_ACT" to 4.0, "SOC_1_LANG" to 5.0, "SOC_1_CT" to 17.0,
            "REL_1_TB" to 19.0, "REL_1_ACT" to 5.0, "REL_1_LANG" to 4.0, "REL_1_CT" to 18.0,
            "PE_1_TB" to 18.0, "PE_1_ACT" to 5.0, "PE_1_LANG" to 4.0, "PE_1_CT" to 18.0,
            "ART_1_TB" to 18.0, "ART_1_ACT" to 5.0, "ART_1_LANG" to 4.0, "ART_1_CT" to 18.0,
            "BAN_2_TB" to 19.0, "BAN_2_ACT" to 5.0, "BAN_2_LANG" to 5.0, "BAN_2_CT" to 18.0, "BAN_2_WR" to 45.0,
            "ENG_2_TB" to 18.0, "ENG_2_ACT" to 4.0, "ENG_2_LANG" to 5.0, "ENG_2_CT" to 17.0, "ENG_2_WR" to 43.0,
            "MATH_2_TB" to 19.0, "MATH_2_ACT" to 5.0, "MATH_2_LANG" to 5.0, "MATH_2_CT" to 19.0, "MATH_2_WR" to 47.0,
            "SOC_2_TB" to 18.0, "SOC_2_ACT" to 5.0, "SOC_2_LANG" to 4.0, "SOC_2_CT" to 18.0,
            "REL_2_TB" to 19.0, "REL_2_ACT" to 5.0, "REL_2_LANG" to 5.0, "REL_2_CT" to 19.0,
            "PE_2_TB" to 19.0, "PE_2_ACT" to 5.0, "PE_2_LANG" to 5.0, "PE_2_CT" to 18.0,
            "ART_2_TB" to 19.0, "ART_2_ACT" to 5.0, "ART_2_LANG" to 5.0, "ART_2_CT" to 18.0,
            "BAN_3_TB" to 19.0, "BAN_3_ACT" to 5.0, "BAN_3_LANG" to 5.0, "BAN_3_CT" to 19.0, "BAN_3_WR" to 46.0,
            "ENG_3_TB" to 18.0, "ENG_3_ACT" to 5.0, "ENG_3_LANG" to 5.0, "ENG_3_CT" to 18.0, "ENG_3_WR" to 44.0,
            "MATH_3_TB" to 20.0, "MATH_3_ACT" to 5.0, "MATH_3_LANG" to 5.0, "MATH_3_CT" to 19.0, "MATH_3_WR" to 48.0,
            "SOC_3_TB" to 19.0, "SOC_3_ACT" to 5.0, "SOC_3_LANG" to 5.0, "SOC_3_CT" to 18.0,
            "REL_3_TB" to 19.0, "REL_3_ACT" to 5.0, "REL_3_LANG" to 5.0, "REL_3_CT" to 19.0,
            "PE_3_TB" to 19.0, "PE_3_ACT" to 5.0, "PE_3_LANG" to 5.0, "PE_3_CT" to 19.0,
            "ART_3_TB" to 19.0, "ART_3_ACT" to 5.0, "ART_3_LANG" to 5.0, "ART_3_CT" to 19.0
        ),
        x = mapOf(
            "DAYS_1" to "62", "PRES_1" to "59", "ACT_1" to "ক", "DISC_1" to "ক", "COND_1" to "ক",
            "REM_1" to "পাঠ্যবিষয়ে মনোযোগী ও নিয়মিত। বাংলা ও গণিতে চমৎকার অগ্রগতি দেখিয়েছে।",
            "DAYS_2" to "65", "PRES_2" to "62", "ACT_2" to "ক", "DISC_2" to "ক", "COND_2" to "ক",
            "REM_2" to "দলগত কাজে সক্রিয় অংশগ্রহণ প্রশংসনীয়। ইংরেজি পঠনে আরও অনুশীলন ফলপ্রসূ হবে।",
            "DAYS_3" to "64", "PRES_3" to "63", "ACT_3" to "ক", "DISC_3" to "ক", "COND_3" to "ক",
            "REM_3" to "সকল বিষয়ে ধারাবাহিক উৎকর্ষ অর্জন করেছে। পরবর্তী শ্রেণিতে উত্তীর্ণ হওয়ার যোগ্য।"
        )
    ),
    Student(
        id = "sample-3-1",
        cls = 3,
        roll = 1,
        name = "নুসরাত জাহান মিম",
        guardian = "মোঃ শফিকুল ইসলাম",
        village = "গোয়ালপাড়া",
        post = "",
        upazila = DEFAULT_UPAZILA,
        district = DEFAULT_DISTRICT,
        mobile = "01819445566",
        religion = "ইসলাম",
        m = mapOf(
            "BAN_1_TB" to 9.0, "BAN_1_ACT" to 5.0, "BAN_1_LANG" to 4.0, "BAN_1_CT" to 9.0, "BAN_1_WR" to 60.0,
            "ENG_1_TB" to 9.0, "ENG_1_ACT" to 4.0, "ENG_1_LANG" to 4.0, "ENG_1_CT" to 8.0, "ENG_1_WR" to 56.0,
            "MATH_1_TB" to 10.0, "MATH_1_ACT" to 5.0, "MATH_1_LANG" to 5.0, "MATH_1_CT" to 9.0, "MATH_1_WR" to 64.0,
            "SCI_1_TB" to 9.0, "SCI_1_ACT" to 5.0, "SCI_1_LANG" to 4.0, "SCI_1_CT" to 9.0, "SCI_1_WR" to 58.0,
            "BGW_1_TB" to 9.0, "BGW_1_ACT" to 4.0, "BGW_1_LANG" to 4.0, "BGW_1_CT" to 8.0, "BGW_1_WR" to 57.0,
            "REL_1_TB" to 9.0, "REL_1_ACT" to 5.0, "REL_1_LANG" to 5.0, "REL_1_CT" to 9.0, "REL_1_WR" to 62.0,
            "PE_1_TB" to 18.0, "PE_1_ACT" to 5.0, "PE_1_LANG" to 4.0, "PE_1_CT" to 17.0,
            "ART_1_TB" to 18.0, "ART_1_ACT" to 5.0, "ART_1_LANG" to 4.0, "ART_1_CT" to 17.0,
            "BAN_2_TB" to 9.0, "BAN_2_ACT" to 5.0, "BAN_2_LANG" to 5.0, "BAN_2_CT" to 9.0, "BAN_2_WR" to 62.0,
            "ENG_2_TB" to 9.0, "ENG_2_ACT" to 5.0, "ENG_2_LANG" to 4.0, "ENG_2_CT" to 9.0, "ENG_2_WR" to 59.0,
            "MATH_2_TB" to 10.0, "MATH_2_ACT" to 5.0, "MATH_2_LANG" to 5.0, "MATH_2_CT" to 10.0, "MATH_2_WR" to 65.0,
            "SCI_2_TB" to 9.0, "SCI_2_ACT" to 5.0, "SCI_2_LANG" to 5.0, "SCI_2_CT" to 9.0, "SCI_2_WR" to 61.0,
            "BGW_2_TB" to 9.0, "BGW_2_ACT" to 5.0, "BGW_2_LANG" to 4.0, "BGW_2_CT" to 9.0, "BGW_2_WR" to 60.0,
            "REL_2_TB" to 10.0, "REL_2_ACT" to 5.0, "REL_2_LANG" to 5.0, "REL_2_CT" to 9.0, "REL_2_WR" to 64.0,
            "PE_2_TB" to 19.0, "PE_2_ACT" to 5.0, "PE_2_LANG" to 5.0, "PE_2_CT" to 18.0,
            "ART_2_TB" to 18.0, "ART_2_ACT" to 5.0, "ART_2_LANG" to 5.0, "ART_2_CT" to 18.0,
            "BAN_3_TB" to 10.0, "BAN_3_ACT" to 5.0, "BAN_3_LANG" to 5.0, "BAN_3_CT" to 9.0, "BAN_3_WR" to 63.0,
            "ENG_3_TB" to 9.0, "ENG_3_ACT" to 5.0, "ENG_3_LANG" to 5.0, "ENG_3_CT" to 9.0, "ENG_3_WR" to 61.0,
            "MATH_3_TB" to 10.0, "MATH_3_ACT" to 5.0, "MATH_3_LANG" to 5.0, "MATH_3_CT" to 10.0, "MATH_3_WR" to 67.0,
            "SCI_3_TB" to 9.0, "SCI_3_ACT" to 5.0, "SCI_3_LANG" to 5.0, "SCI_3_CT" to 9.0, "SCI_3_WR" to 63.0,
            "BGW_3_TB" to 9.0, "BGW_3_ACT" to 5.0, "BGW_3_LANG" to 5.0, "BGW_3_CT" to 9.0, "BGW_3_WR" to 62.0,
            "REL_3_TB" to 10.0, "REL_3_ACT" to 5.0, "REL_3_LANG" to 5.0, "REL_3_CT" to 10.0, "REL_3_WR" to 65.0,
            "PE_3_TB" to 19.0, "PE_3_ACT" to 5.0, "PE_3_LANG" to 5.0, "PE_3_CT" to 19.0,
            "ART_3_TB" to 19.0, "ART_3_ACT" to 5.0, "ART_3_LANG" to 5.0, "ART_3_CT" to 18.0
        ),
        x = mapOf(
            "DAYS_1" to "62", "PRES_1" to "58", "ACT_1" to "ক", "DISC_1" to "ক", "COND_1" to "ক",
            "REM_1" to "শ্রেণিকক্ষে অত্যন্ত শৃঙ্খলাপরায়ণ এবং পাঠদানে মনোযোগী।",
            "DAYS_2" to "65", "PRES_2" to "63", "ACT_2" to "ক", "DISC_2" to "ক", "COND_2" to "ক",
            "REM_2" to "বিজ্ঞান ও গণিতে বিশ্লেষণী দক্ষতা বেড়েছে। ধারাবাহিকতা বজায় রাখুক।",
            "DAYS_3" to "64", "PRES_3" to "62", "ACT_3" to "ক", "DISC_3" to "ক", "COND_3" to "ক",
            "REM_3" to "চূড়ান্ত মূল্যায়নে অতি উত্তম ফলাফল অর্জন করেছে। অভিনন্দন।"
        )
    ),
    Student(
        id = "sample-5-2",
        cls = 5,
        roll = 2,
        name = "অনিক চক্রবর্তী",
        guardian = "সুমন চক্রবর্তী",
        village = "বাগাট",
        post = "",
        upazila = DEFAULT_UPAZILA,
        district = DEFAULT_DISTRICT,
        mobile = "01915667788",
        religion = "হিন্দু",
        m = mapOf(
            "BAN_1_TB" to 8.0, "BAN_1_ACT" to 4.0, "BAN_1_LANG" to 4.0, "BAN_1_CT" to 8.0, "BAN_1_WR" to 52.0,
            "ENG_1_TB" to 8.0, "ENG_1_ACT" to 4.0, "ENG_1_LANG" to 4.0, "ENG_1_CT" to 7.0, "ENG_1_WR" to 48.0,
            "MATH_1_TB" to 9.0, "MATH_1_ACT" to 4.0, "MATH_1_LANG" to 4.0, "MATH_1_CT" to 8.0, "MATH_1_WR" to 54.0,
            "SCI_1_TB" to 8.0, "SCI_1_ACT" to 4.0, "SCI_1_LANG" to 4.0, "SCI_1_CT" to 8.0, "SCI_1_WR" to 51.0,
            "BGW_1_TB" to 8.0, "BGW_1_ACT" to 4.0, "BGW_1_LANG" to 4.0, "BGW_1_CT" to 7.0, "BGW_1_WR" to 49.0,
            "REL_1_TB" to 9.0, "REL_1_ACT" to 5.0, "REL_1_LANG" to 4.0, "REL_1_CT" to 8.0, "REL_1_WR" to 56.0,
            "PE_1_TB" to 17.0, "PE_1_ACT" to 4.0, "PE_1_LANG" to 4.0, "PE_1_CT" to 16.0,
            "ART_1_TB" to 16.0, "ART_1_ACT" to 5.0, "ART_1_LANG" to 4.0, "ART_1_CT" to 16.0,
            "BAN_2_TB" to 9.0, "BAN_2_ACT" to 4.0, "BAN_2_LANG" to 4.0, "BAN_2_CT" to 8.0, "BAN_2_WR" to 55.0,
            "ENG_2_TB" to 8.0, "ENG_2_ACT" to 4.0, "ENG_2_LANG" to 4.0, "ENG_2_CT" to 8.0, "ENG_2_WR" to 50.0,
            "MATH_2_TB" to 9.0, "MATH_2_ACT" to 5.0, "MATH_2_LANG" to 4.0, "MATH_2_CT" to 9.0, "MATH_2_WR" to 56.0,
            "SCI_2_TB" to 8.0, "SCI_2_ACT" to 4.0, "SCI_2_LANG" to 4.0, "SCI_2_CT" to 8.0, "SCI_2_WR" to 53.0,
            "BGW_2_TB" to 8.0, "BGW_2_ACT" to 4.0, "BGW_2_LANG" to 4.0, "BGW_2_CT" to 8.0, "BGW_2_WR" to 52.0,
            "REL_2_TB" to 9.0, "REL_2_ACT" to 5.0, "REL_2_LANG" to 4.0, "REL_2_CT" to 9.0, "REL_2_WR" to 58.0,
            "PE_2_TB" to 18.0, "PE_2_ACT" to 4.0, "PE_2_LANG" to 4.0, "PE_2_CT" to 17.0,
            "ART_2_TB" to 17.0, "ART_2_ACT" to 5.0, "ART_2_LANG" to 4.0, "ART_2_CT" to 17.0
        ),
        x = mapOf(
            "DAYS_1" to "62", "PRES_1" to "54", "ACT_1" to "খ", "DISC_1" to "ক", "COND_1" to "ক",
            "REM_1" to "নিয়মিত উপস্থিত থাকলে এবং ইংরেজি অনুশীলন বাড়ালে আরও ভালো করবে।",
            "DAYS_2" to "65", "PRES_2" to "59", "ACT_2" to "ক", "DISC_2" to "ক", "COND_2" to "ক",
            "REM_2" to "২য় প্রান্তিকে উল্লেখযোগ্য উন্নতি হয়েছে। গণিতে আগ্রহ প্রশংসনীয়।"
        )
    )
)
