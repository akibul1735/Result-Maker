package com.akibul.shikhon

data class Student(
    val id: String,
    val cls: Int,
    val roll: Int,
    val name: String,
    val guardian: String = "",
    val village: String = "",
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
    val teacherMobiles: Map<Int, String> = emptyMap()
)

data class Comp(val key: String, val bn: String, val max: Int)
data class Sub(val key: String, val bn: String, val max: Int, val comps: List<Comp>, val written: Int)

val RELIGIONS = listOf("ইসলাম", "হিন্দু", "বৌদ্ধ", "খ্রীষ্ট")
val CLSN = listOf("প্রথম", "দ্বিতীয়", "তৃতীয়", "চতুর্থ", "পঞ্চম")

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
fun Student.overallPct(): Double {
    val ss = subs()
    return ss.sumOf { avg(it) } / ss.sumOf { it.max } * 100
}
