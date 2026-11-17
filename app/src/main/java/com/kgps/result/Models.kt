package com.kgps.result

data class Student(
    val id: String,
    val cls: Int,
    val roll: Int,
    val name: String,
    val guardian: String = "",
    val village: String = "",
    val post: String = "",
    val upazila: String = "",
    val district: String = "",
    val mobile: String = "",
    val religion: String = "ইসলাম",
    val m: Map<String, Double> = emptyMap(),   // নম্বর: BAN_1_TB, BAN_1_WR, ART_2_TOTAL ...
    val x: Map<String, String> = emptyMap()    // DAYS_1, PRES_1, ACT_1, DISC_1, COND_1, REM_1 ...
)

data class Settings(
    val school: String = "",
    val year: String = "২০২৬",
    val teachers: Map<Int, String> = emptyMap(),
    val teacherMobiles: Map<Int, String> = emptyMap(),
    val pdfFont: String = "noto_serif",
    val dpeLogo: String = "simple"
)

data class DpeLogoInfo(val id: String, val labelBn: String, val descriptionBn: String)

val DPE_LOGO_LIST = listOf(
    DpeLogoInfo("simple", "সাদাকালো লোগো (Simple)", "সাদাকালো ফটোকপি ও সাধারণ প্রিন্টের জন্য স্পষ্ট লাইন-আর্ট লোগো"),
    DpeLogoInfo("color", "রঙিন লোগো (Color)", "রঙিন মুদ্রণ ও ডিজিটাল প্রদর্শনের জন্য মূল রঙিন লোগো")
)

data class PdfFontInfo(val id: String, val labelBn: String, val descriptionBn: String)

val PDF_FONT_LIST = listOf(
    PdfFontInfo("noto_serif", "নোতো সেরিফ বাংলা (প্রাতিষ্ঠানিক)", "সরকারি নথি ও আনুষ্ঠানিক প্রিন্টের জন্য আদর্শ সেরিফ ফন্ট"),
    PdfFontInfo("noto_sans", "নোতো স্যান্স বাংলা (স্ট্যান্ডার্ড)", "পরিচ্ছন্ন ও আধুনিক ডিজিটাল ফরম্যাট"),
    PdfFontInfo("default", "সিস্টেম ডিফল্ট", "ডিভাইসের নিজস্ব বাংলা ফন্ট")
)

val PDF_FONTS = PDF_FONT_LIST.map { it.id to it.labelBn }

data class Comp(val key: String, val bn: String, val max: Int)
data class Sub(val key: String, val bn: String, val max: Int, val comps: List<Comp>, val written: Int)

val RELIGIONS = listOf("ইসলাম", "হিন্দু", "বৌদ্ধ", "খ্রীষ্ট")
val CLSN = listOf("প্রথম", "দ্বিতীয়", "তৃতীয়", "চতুর্থ", "পঞ্চম")
val CLS_SHORT = listOf("১ম", "২য়", "৩য়", "৪র্থ", "৫ম")

private fun comps(tb: Int, ct: Int) = listOf(
    Comp("TB", "পাঠ্যপুস্তক/শিক্ষক সহায়িকার কাজ", tb),
    Comp("ACT", "শ্রেণিকাজে সক্রিয়তা", 5),
    Comp("LANG", "ভাষাদক্ষতা/বোধগম্যতা", 5),
    Comp("CT", "ক্লাস টেস্ট", ct)
)

fun subjectsFor(cls: Int, religion: String): List<Sub> {
    val low = cls <= 2
    fun full(k: String, n: String) =
        if (low) Sub(k, n, 100, comps(20, 20), 50) else Sub(k, n, 100, comps(10, 10), 70)
    fun half(k: String, n: String) = Sub(k, n, 50, comps(20, 20), 0)
    val art = Sub("ART", "শিল্পকলা", 50,
        listOf(Comp("TOTAL", "মোট নম্বর (চারু ও কারুকলা, সংগীত, নৃত্য, নাট্যকলা)", 50)), 0)
    val rel = "ধর্মশিক্ষা ($religion)"
    return if (low) listOf(
        full("BAN", "বাংলা"), full("ENG", "ইংরেজি"), full("MATH", "গণিত"),
        half("SOC", "সামাজিক বিজ্ঞান ও বিজ্ঞান (সমন্বিত)"), half("REL", rel),
        half("PE", "শারীরিক ও মানসিক স্বাস্থ্য শিক্ষা"), art
    ) else listOf(
        full("BAN", "বাংলা"), full("ENG", "ইংরেজি"), full("MATH", "গণিত"),
        full("SCI", "বিজ্ঞান"), full("BGW", "বাংলাদেশ ও বিশ্বপরিচয়"), full("REL", rel),
        half("PE", "শারীরিক ও মানসিক স্বাস্থ্য শিক্ষা"), art
    )
}

// ---------- হিসাব ----------
fun mk(sub: String, t: Int, c: String) = "${sub}_${t}_$c"

fun bn(s: String): String = s.map { if (it in '0'..'9') '০' + (it - '0') else it }.joinToString("")
fun fromBn(s: String): String = s.map { if (it in '০'..'৯') '0' + (it - '০') else it }.joinToString("")
fun fmtEn(d: Double): String { val r = Math.round(d * 10) / 10.0; return if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString() }
fun fmt(d: Double): String = bn(fmtEn(d))

fun Student.subs() = subjectsFor(cls, religion)
fun Student.hasTerm(t: Int) = m.keys.any { it.contains("_${t}_") }
fun Student.hasSub(s: Sub, t: Int) = m.keys.any { it.startsWith("${s.key}_${t}_") }
fun Student.cont(s: Sub, t: Int) = s.comps.sumOf { m[mk(s.key, t, it.key)] ?: 0.0 }
fun Student.wr(s: Sub, t: Int) = if (s.written > 0) (m[mk(s.key, t, "WR")] ?: 0.0) else 0.0
fun Student.total(s: Sub, t: Int) = cont(s, t) + wr(s, t)
fun Student.avg(s: Sub) = (1..3).sumOf { total(s, it) } / 3.0
fun gradeOf(p: Double) = when { p >= 80 -> "A"; p >= 60 -> "B"; p >= 40 -> "C"; else -> "D" }
fun gradeLabelBn(g: String) = when (g) {
    "A" -> "অতি উত্তম"
    "B" -> "উত্তম"
    "C" -> "সন্তোষজনক"
    else -> "সহায়তা প্রয়োজন"
}
fun Student.overallPct(): Double {
    val ss = subs()
    val maxSum = ss.sumOf { it.max }
    return if (maxSum == 0) 0.0 else ss.sumOf { avg(it) } / maxSum * 100
}

val DEFAULT_SETTINGS = Settings(
    school = "কামারখালী সরকারি প্রাথমিক বিদ্যালয়",
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
    pdfFont = "noto_serif"
)

val SAMPLE_STUDENTS = listOf(
    Student(
        id = "sample-1-1",
        cls = 1,
        roll = 1,
        name = "তানভীর আহমেদ",
        guardian = "মোঃ করিম উদ্দিন",
        village = "কামারখালী",
        post = "কামারখালী",
        upazila = "মধুখালী",
        district = "ফরিদপুর",
        mobile = "01711223344",
        religion = "ইসলাম",
        m = mapOf(
            "BAN_1_TB" to 18.0, "BAN_1_ACT" to 5.0, "BAN_1_LANG" to 4.0, "BAN_1_CT" to 17.0, "BAN_1_WR" to 44.0,
            "ENG_1_TB" to 17.0, "ENG_1_ACT" to 4.0, "ENG_1_LANG" to 4.0, "ENG_1_CT" to 16.0, "ENG_1_WR" to 41.0,
            "MATH_1_TB" to 19.0, "MATH_1_ACT" to 5.0, "MATH_1_LANG" to 5.0, "MATH_1_CT" to 18.0, "MATH_1_WR" to 46.0,
            "SOC_1_TB" to 18.0, "SOC_1_ACT" to 4.0, "SOC_1_LANG" to 5.0, "SOC_1_CT" to 17.0,
            "REL_1_TB" to 19.0, "REL_1_ACT" to 5.0, "REL_1_LANG" to 4.0, "REL_1_CT" to 18.0,
            "PE_1_TB" to 18.0, "PE_1_ACT" to 5.0, "PE_1_LANG" to 4.0, "PE_1_CT" to 18.0,
            "ART_1_TOTAL" to 45.0,
            "BAN_2_TB" to 19.0, "BAN_2_ACT" to 5.0, "BAN_2_LANG" to 5.0, "BAN_2_CT" to 18.0, "BAN_2_WR" to 45.0,
            "ENG_2_TB" to 18.0, "ENG_2_ACT" to 4.0, "ENG_2_LANG" to 5.0, "ENG_2_CT" to 17.0, "ENG_2_WR" to 43.0,
            "MATH_2_TB" to 19.0, "MATH_2_ACT" to 5.0, "MATH_2_LANG" to 5.0, "MATH_2_CT" to 19.0, "MATH_2_WR" to 47.0,
            "SOC_2_TB" to 18.0, "SOC_2_ACT" to 5.0, "SOC_2_LANG" to 4.0, "SOC_2_CT" to 18.0,
            "REL_2_TB" to 19.0, "REL_2_ACT" to 5.0, "REL_2_LANG" to 5.0, "REL_2_CT" to 19.0,
            "PE_2_TB" to 19.0, "PE_2_ACT" to 5.0, "PE_2_LANG" to 5.0, "PE_2_CT" to 18.0,
            "ART_2_TOTAL" to 47.0,
            "BAN_3_TB" to 19.0, "BAN_3_ACT" to 5.0, "BAN_3_LANG" to 5.0, "BAN_3_CT" to 19.0, "BAN_3_WR" to 46.0,
            "ENG_3_TB" to 18.0, "ENG_3_ACT" to 5.0, "ENG_3_LANG" to 5.0, "ENG_3_CT" to 18.0, "ENG_3_WR" to 44.0,
            "MATH_3_TB" to 20.0, "MATH_3_ACT" to 5.0, "MATH_3_LANG" to 5.0, "MATH_3_CT" to 19.0, "MATH_3_WR" to 48.0,
            "SOC_3_TB" to 19.0, "SOC_3_ACT" to 5.0, "SOC_3_LANG" to 5.0, "SOC_3_CT" to 18.0,
            "REL_3_TB" to 19.0, "REL_3_ACT" to 5.0, "REL_3_LANG" to 5.0, "REL_3_CT" to 19.0,
            "PE_3_TB" to 19.0, "PE_3_ACT" to 5.0, "PE_3_LANG" to 5.0, "PE_3_CT" to 19.0,
            "ART_3_TOTAL" to 48.0
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
        post = "কামারখালী",
        upazila = "মধুখালী",
        district = "ফরিদপুর",
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
            "ART_1_TOTAL" to 44.0,
            "BAN_2_TB" to 9.0, "BAN_2_ACT" to 5.0, "BAN_2_LANG" to 5.0, "BAN_2_CT" to 9.0, "BAN_2_WR" to 62.0,
            "ENG_2_TB" to 9.0, "ENG_2_ACT" to 5.0, "ENG_2_LANG" to 4.0, "ENG_2_CT" to 9.0, "ENG_2_WR" to 59.0,
            "MATH_2_TB" to 10.0, "MATH_2_ACT" to 5.0, "MATH_2_LANG" to 5.0, "MATH_2_CT" to 10.0, "MATH_2_WR" to 65.0,
            "SCI_2_TB" to 9.0, "SCI_2_ACT" to 5.0, "SCI_2_LANG" to 5.0, "SCI_2_CT" to 9.0, "SCI_2_WR" to 61.0,
            "BGW_2_TB" to 9.0, "BGW_2_ACT" to 5.0, "BGW_2_LANG" to 4.0, "BGW_2_CT" to 9.0, "BGW_2_WR" to 60.0,
            "REL_2_TB" to 10.0, "REL_2_ACT" to 5.0, "REL_2_LANG" to 5.0, "REL_2_CT" to 9.0, "REL_2_WR" to 64.0,
            "PE_2_TB" to 19.0, "PE_2_ACT" to 5.0, "PE_2_LANG" to 5.0, "PE_2_CT" to 18.0,
            "ART_2_TOTAL" to 46.0,
            "BAN_3_TB" to 10.0, "BAN_3_ACT" to 5.0, "BAN_3_LANG" to 5.0, "BAN_3_CT" to 9.0, "BAN_3_WR" to 63.0,
            "ENG_3_TB" to 9.0, "ENG_3_ACT" to 5.0, "ENG_3_LANG" to 5.0, "ENG_3_CT" to 9.0, "ENG_3_WR" to 61.0,
            "MATH_3_TB" to 10.0, "MATH_3_ACT" to 5.0, "MATH_3_LANG" to 5.0, "MATH_3_CT" to 10.0, "MATH_3_WR" to 67.0,
            "SCI_3_TB" to 9.0, "SCI_3_ACT" to 5.0, "SCI_3_LANG" to 5.0, "SCI_3_CT" to 9.0, "SCI_3_WR" to 63.0,
            "BGW_3_TB" to 9.0, "BGW_3_ACT" to 5.0, "BGW_3_LANG" to 5.0, "BGW_3_CT" to 9.0, "BGW_3_WR" to 62.0,
            "REL_3_TB" to 10.0, "REL_3_ACT" to 5.0, "REL_3_LANG" to 5.0, "REL_3_CT" to 10.0, "REL_3_WR" to 65.0,
            "PE_3_TB" to 19.0, "PE_3_ACT" to 5.0, "PE_3_LANG" to 5.0, "PE_3_CT" to 19.0,
            "ART_3_TOTAL" to 47.0
        ),
        x = mapOf(
            "DAYS_1" to "62", "PRES_1" to "58", "ACT_1" to "ক", "DISC_1" to "ক", "COND_1" to "ক",
            "REM_1" to "শ্রেণিকক্ষে অত্যন্ত শৃঙ্খলাপরায়ণ এবং পাঠদানে মনোযোগী।",
            "DAYS_2" to "65", "PRES_2" to "63", "ACT_2" to "ক", "DISC_2" to "ক", "COND_2" to "ক",
            "REM_2" to "বিজ্ঞান ও গণিতে বিশ্লেষণী দক্ষতা বেড়েছে। ধারাবাহিকতা বজায় রাখুক।",
            "DAYS_3" to "64", "PRES_3" to "62", "ACT_3" to "ক", "DISC_3" to "ক", "COND_3" to "ক",
            "REM_3" to "বার্ষিক মূল্যায়নে অতি উত্তম ফলাফল অর্জন করেছে। অভিনন্দন।"
        )
    ),
    Student(
        id = "sample-5-2",
        cls = 5,
        roll = 2,
        name = "অনিক চক্রবর্তী",
        guardian = "সুমন চক্রবর্তী",
        village = "বাগাট",
        post = "বাগাট",
        upazila = "মধুখালী",
        district = "ফরিদপুর",
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
            "ART_1_TOTAL" to 41.0,
            "BAN_2_TB" to 9.0, "BAN_2_ACT" to 4.0, "BAN_2_LANG" to 4.0, "BAN_2_CT" to 8.0, "BAN_2_WR" to 55.0,
            "ENG_2_TB" to 8.0, "ENG_2_ACT" to 4.0, "ENG_2_LANG" to 4.0, "ENG_2_CT" to 8.0, "ENG_2_WR" to 50.0,
            "MATH_2_TB" to 9.0, "MATH_2_ACT" to 5.0, "MATH_2_LANG" to 4.0, "MATH_2_CT" to 9.0, "MATH_2_WR" to 56.0,
            "SCI_2_TB" to 8.0, "SCI_2_ACT" to 4.0, "SCI_2_LANG" to 4.0, "SCI_2_CT" to 8.0, "SCI_2_WR" to 53.0,
            "BGW_2_TB" to 8.0, "BGW_2_ACT" to 4.0, "BGW_2_LANG" to 4.0, "BGW_2_CT" to 8.0, "BGW_2_WR" to 52.0,
            "REL_2_TB" to 9.0, "REL_2_ACT" to 5.0, "REL_2_LANG" to 4.0, "REL_2_CT" to 9.0, "REL_2_WR" to 58.0,
            "PE_2_TB" to 18.0, "PE_2_ACT" to 4.0, "PE_2_LANG" to 4.0, "PE_2_CT" to 17.0,
            "ART_2_TOTAL" to 43.0
        ),
        x = mapOf(
            "DAYS_1" to "62", "PRES_1" to "54", "ACT_1" to "খ", "DISC_1" to "ক", "COND_1" to "ক",
            "REM_1" to "নিয়মিত উপস্থিত থাকলে এবং ইংরেজি অনুশীলন বাড়ালে আরও ভালো করবে।",
            "DAYS_2" to "65", "PRES_2" to "59", "ACT_2" to "ক", "DISC_2" to "ক", "COND_2" to "ক",
            "REM_2" to "২য় প্রান্তিকে উল্লেখযোগ্য উন্নতি হয়েছে। গণিতে আগ্রহ প্রশংসনীয়।"
        )
    )
)
