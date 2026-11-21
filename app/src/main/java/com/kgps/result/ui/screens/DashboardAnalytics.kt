package com.kgps.result.ui.screens

import com.kgps.result.Student
import com.kgps.result.bn
import com.kgps.result.classShortBn
import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.fromBn
import com.kgps.result.gradeOf
import com.kgps.result.hasSub
import com.kgps.result.hasTerm
import com.kgps.result.overallPct
import com.kgps.result.subs
import com.kgps.result.total
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class GapFilter(val labelBn: String) {
    ALL("সব ঘাটতি"),
    MARKS("নম্বর বাকি"),
    ATTENDANCE("উপস্থিতি বাকি"),
    INFO("তথ্য ও মন্তব্য")
}

data class MeritEntry(
    val rank: Int,
    val student: Student,
    val pct: Double,
    val obtained: Double,
    val maxMarks: Double,
    val grade: String
)

data class ClassProgressItem(
    val cls: Int,
    val totalStudents: Int,
    val evaluatedCount: Int,
    val avgPct: Double,
    val attendanceRate: Double?
)

data class GapStudentItem(
    val student: Student,
    val missingMarks: Boolean,
    val missingAttendance: Boolean,
    val missingInfo: Boolean,
    val tags: List<String>
)

data class DashboardSummary(
    val totalStudents: Int,
    val evaluatedInScope: Int,
    val term1Count: Int,
    val term2Count: Int,
    val term3Count: Int,
    val attendanceFilledCount: Int,
    val traitsFilledCount: Int,
    val avgAttendancePct: Double?,
    val avgScorePct: Double,
    val gradeCounts: Map<String, Int>,
    val meritList: List<MeritEntry>,
    val classBreakdown: List<ClassProgressItem>,
    val gapItems: List<GapStudentItem>
)

private fun parseNum(raw: String?): Double? {
    if (raw.isNullOrBlank()) return null
    return fromBn(raw.trim()).toDoubleOrNull()
}

fun buildDashboardSummary(
    allStudents: List<Student>,
    visibleClasses: List<Int>,
    classFilter: Int?, // null = সব অনুমোদিত শ্রেণি
    termFilter: Int,   // 0 = বার্ষিক (গড়), 1..3 = নির্দিষ্ট প্রান্তিক
    permissions: TeacherPermissions
): DashboardSummary {
    val allowedClsSet = visibleClasses.toSet()
    val allowedSubKeys = permissions.effectiveSubjects().toSet()
    val showAllSubjects = permissions.allSubjects || permissions.canViewOtherSubjects

    val visibleStudents = allStudents
        .filter { it.cls in allowedClsSet }
        .sortedWith(compareBy({ it.cls }, { it.roll }))

    val scopedStudents = if (classFilter == null) {
        visibleStudents
    } else {
        visibleStudents.filter { it.cls == classFilter }
    }

    fun scopedSubsFor(s: Student) = if (showAllSubjects) {
        s.subs()
    } else {
        s.subs().filter { it.key in allowedSubKeys }
    }

    fun hasTermMarks(s: Student, term: Int): Boolean {
        val subs = scopedSubsFor(s)
        return subs.any { s.hasSub(it, term) }
    }

    fun hasAttendanceForTerm(s: Student, term: Int): Boolean {
        val days = parseNum(s.x["DAYS_$term"])
        val pres = parseNum(s.x["PRES_$term"])
        return days != null && days > 0.0 && pres != null && pres >= 0.0
    }

    fun attendancePairForTerms(s: Student, terms: List<Int>): Pair<Double, Double>? {
        var sumDays = 0.0
        var sumPres = 0.0
        terms.forEach { t ->
            val d = parseNum(s.x["DAYS_$t"]) ?: 0.0
            val p = parseNum(s.x["PRES_$t"]) ?: 0.0
            if (d > 0.0 && p >= 0.0) {
                sumDays += d
                sumPres += p.coerceAtMost(d)
            }
        }
        return if (sumDays > 0.0) sumPres to sumDays else null
    }

    fun hasTraitsOrRemarkForTerm(s: Student, term: Int): Boolean {
        val hasTraits = !s.x["ACT_$term"].isNullOrBlank() &&
            !s.x["DISC_$term"].isNullOrBlank() &&
            !s.x["COND_$term"].isNullOrBlank()
        val hasRem = !s.x["REM_$term"].isNullOrBlank()
        return hasTraits || hasRem
    }

    fun scoreFor(s: Student): Triple<Double, Double, Double>? {
        val subs = scopedSubsFor(s)
        if (subs.isEmpty()) return null
        val maxSum = subs.sumOf { it.max }.toDouble()
        if (maxSum <= 0.0) return null

        if (termFilter in 1..3) {
            if (!hasTermMarks(s, termFilter)) return null
            val obtained = subs.sumOf { s.total(it, termFilter) }
            val pct = (obtained / maxSum) * 100.0
            return Triple(obtained, maxSum, pct)
        } else {
            val hasAny = (1..3).any { hasTermMarks(s, it) }
            if (!hasAny) return null
            if (showAllSubjects) {
                val pct = s.overallPct()
                val obtained = subs.sumOf { sub -> (1..3).sumOf { t -> s.total(sub, t) } / 3.0 }
                return Triple(obtained, maxSum, pct)
            } else {
                val obtained = subs.sumOf { sub -> (1..3).sumOf { t -> s.total(sub, t) } / 3.0 }
                val pct = (obtained / maxSum) * 100.0
                return Triple(obtained, maxSum, pct)
            }
        }
    }

    // Determine which terms are active/started in the current class scope for smart gap detection in annual mode
    val startedTerms = if (termFilter in 1..3) {
        listOf(termFilter)
    } else {
        val detected = (1..3).filter { t ->
            scopedStudents.any { it.hasTerm(t) || hasAttendanceForTerm(it, t) }
        }
        if (detected.isEmpty()) listOf(1) else detected
    }

    val term1Count = scopedStudents.count { hasTermMarks(it, 1) }
    val term2Count = scopedStudents.count { hasTermMarks(it, 2) }
    val term3Count = scopedStudents.count { hasTermMarks(it, 3) }

    val targetCheckTerms = if (termFilter in 1..3) listOf(termFilter) else startedTerms
    val attendanceFilledCount = scopedStudents.count { s ->
        targetCheckTerms.all { hasAttendanceForTerm(s, it) }
    }
    val traitsFilledCount = scopedStudents.count { s ->
        targetCheckTerms.all { t ->
            !s.x["ACT_$t"].isNullOrBlank() &&
                !s.x["DISC_$t"].isNullOrBlank() &&
                !s.x["COND_$t"].isNullOrBlank()
        }
    }

    var totalPresAll = 0.0
    var totalDaysAll = 0.0
    scopedStudents.forEach { s ->
        attendancePairForTerms(s, if (termFilter in 1..3) listOf(termFilter) else listOf(1, 2, 3))?.let { (p, d) ->
            totalPresAll += p
            totalDaysAll += d
        }
    }
    val avgAttendancePct = if (totalDaysAll > 0.0) (totalPresAll / totalDaysAll) * 100.0 else null

    val scored = scopedStudents.mapNotNull { s ->
        scoreFor(s)?.let { (obt, max, pct) -> Triple(s, obt to max, pct) }
    }

    val gradeCounts = mutableMapOf("A" to 0, "B" to 0, "C" to 0, "D" to 0)
    scored.forEach { (_, _, pct) ->
        val g = gradeOf(pct)
        gradeCounts[g] = (gradeCounts[g] ?: 0) + 1
    }

    val avgScorePct = if (scored.isNotEmpty()) scored.sumOf { it.third } / scored.size else 0.0

    val meritList = scored
        .sortedWith(
            compareByDescending<Triple<Student, Pair<Double, Double>, Double>> { it.third }
                .thenBy { it.first.cls }
                .thenBy { it.first.roll }
        )
        .mapIndexed { idx, (student, pair, pct) ->
            MeritEntry(
                rank = idx + 1,
                student = student,
                pct = pct,
                obtained = pair.first,
                maxMarks = pair.second,
                grade = gradeOf(pct)
            )
        }

    val classBreakdown = visibleClasses.map { c ->
        val inCls = visibleStudents.filter { it.cls == c }
        val clsScored = inCls.mapNotNull { scoreFor(it)?.third }
        var cPres = 0.0
        var cDays = 0.0
        inCls.forEach { s ->
            attendancePairForTerms(s, if (termFilter in 1..3) listOf(termFilter) else listOf(1, 2, 3))?.let { (p, d) ->
                cPres += p
                cDays += d
            }
        }
        ClassProgressItem(
            cls = c,
            totalStudents = inCls.size,
            evaluatedCount = clsScored.size,
            avgPct = if (clsScored.isNotEmpty()) clsScored.sum() / clsScored.size else 0.0,
            attendanceRate = if (cDays > 0.0) (cPres / cDays) * 100.0 else null
        )
    }

    val gapItems = scopedStudents.mapNotNull { s ->
        val subs = scopedSubsFor(s)
        val tags = mutableListOf<String>()
        var missingMarks = false
        var missingAtt = false
        var missingInfo = false

        if (s.name.isBlank()) {
            missingInfo = true
            tags += "নাম নেই"
        }

        val termsForStudent = if (termFilter in 1..3) {
            listOf(termFilter)
        } else {
            val classActiveTerms = (1..3).filter { t ->
                visibleStudents.any { it.cls == s.cls && (it.hasTerm(t) || hasAttendanceForTerm(it, t)) }
            }
            if (classActiveTerms.isEmpty()) listOf(1) else classActiveTerms
        }

        termsForStudent.forEach { t ->
            val termPrefix = if (termsForStudent.size > 1) "${bn(t.toString())}য়: " else ""
            if (subs.isNotEmpty()) {
                val missingSubCount = subs.count { !s.hasSub(it, t) }
                if (missingSubCount == subs.size) {
                    missingMarks = true
                    tags += "${termPrefix}নম্বর দেওয়া হয়নি"
                } else if (missingSubCount > 0) {
                    missingMarks = true
                    tags += "${termPrefix}${bn(missingSubCount.toString())}টি বিষয় বাকি"
                }
            }
            if (!hasAttendanceForTerm(s, t)) {
                missingAtt = true
                tags += "${termPrefix}উপস্থিতি বাকি"
            }
            if (!hasTraitsOrRemarkForTerm(s, t)) {
                missingInfo = true
                tags += "${termPrefix}গুণাবলি/মন্তব্য বাকি"
            }
        }

        if (missingMarks || missingAtt || missingInfo) {
            GapStudentItem(
                student = s,
                missingMarks = missingMarks,
                missingAttendance = missingAtt,
                missingInfo = missingInfo,
                tags = tags.distinct().take(3)
            )
        } else {
            null
        }
    }

    return DashboardSummary(
        totalStudents = scopedStudents.size,
        evaluatedInScope = scored.size,
        term1Count = term1Count,
        term2Count = term2Count,
        term3Count = term3Count,
        attendanceFilledCount = attendanceFilledCount,
        traitsFilledCount = traitsFilledCount,
        avgAttendancePct = avgAttendancePct,
        avgScorePct = avgScorePct,
        gradeCounts = gradeCounts,
        meritList = meritList,
        classBreakdown = classBreakdown,
        gapItems = gapItems
    )
}

fun formatBackupShortTime(ms: Long): String {
    if (ms <= 0L) return "এখনো হয়নি"
    val diffMin = ((System.currentTimeMillis() - ms) / 60_000L).coerceAtLeast(0L)
    return when {
        diffMin < 1L -> "এইমাত্র"
        diffMin < 60L -> "${bn(diffMin.toString())} মি. আগে"
        diffMin < 24 * 60L -> "${bn((diffMin / 60L).toString())} ঘণ্টা আগে"
        else -> SimpleDateFormat("dd/MM, HH:mm", Locale.getDefault()).format(Date(ms))
    }
}

fun classShortLabel(cls: Int): String = classShortBn(cls)
