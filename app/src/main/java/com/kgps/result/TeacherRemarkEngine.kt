package com.kgps.result

import com.kgps.result.util.CommentCategory
import com.kgps.result.util.CommentGenerator
import kotlin.math.abs

enum class RemarkCategory(val labelBn: String) {
    ALL("সব কাছাকাছি"),
    COMBINED("সমন্বিত"),
    SUBJECT_DETAIL("বিষয় খুঁটিনাটি"),
    ACADEMIC("ফলাফল ও প্রগতি"),
    ATTENDANCE("উপস্থিতি"),
    TRAITS("গুণাবলি ও আচরণ"),
    ENCOURAGEMENT("উৎসাহ ও উন্নতি")
}

enum class ResultBand(val labelBn: String) {
    EXCELLENT("অতি উত্তম"),
    GOOD("উত্তম"),
    AVERAGE("সন্তোষজনক"),
    NEEDS_SUPPORT("সহায়তা প্রয়োজন"),
    UNKNOWN("নম্বর নেই")
}

enum class AttendanceBand(val labelBn: String) {
    REGULAR("নিয়মিত"),
    MODERATE("মোটামুটি"),
    LOW("অনিয়মিত"),
    UNKNOWN("তথ্য নেই")
}

enum class TraitsBand(val labelBn: String) {
    EXEMPLARY("অত্যন্ত প্রশংসনীয়"),
    GOOD("ভালো"),
    NEEDS_GUIDANCE("যত্ন প্রয়োজন"),
    UNKNOWN("তথ্য নেই")
}

data class StudentTermProfile(
    val term: Int,
    val hasMarks: Boolean,
    val overallPct: Double?,
    val grade: String?,
    val resultBand: ResultBand,
    val strongSubjects: List<String>,
    val weakSubjects: List<String>,
    val prevDelta: Double?,
    val attPct: Double?,
    val attendanceBand: AttendanceBand,
    val act: String?,
    val disc: String?,
    val cond: String?,
    val traitsBand: TraitsBand
) {
    val hasAnyData: Boolean
        get() = hasMarks ||
            attendanceBand != AttendanceBand.UNKNOWN ||
            traitsBand != TraitsBand.UNKNOWN
}

data class TeacherRemarkOption(
    val id: String,
    val text: String,
    val category: RemarkCategory,
    val toneLabel: String,
    val matchScore: Int,
    val isTopPick: Boolean = false,
    val wordCount: Int = CommentGenerator.countWords(text)
)

object TeacherRemarkEngine {

    private data class CandidateTemplate(
        val id: String,
        val text: String,
        val category: RemarkCategory,
        val toneLabel: String,
        val idealResults: Set<ResultBand> = emptySet(),
        val nearbyResults: Set<ResultBand> = emptySet(),
        val idealAttendance: Set<AttendanceBand> = emptySet(),
        val nearbyAttendance: Set<AttendanceBand> = emptySet(),
        val idealTraits: Set<TraitsBand> = emptySet(),
        val nearbyTraits: Set<TraitsBand> = emptySet(),
        val preferredTerms: Set<Int> = emptySet(),
        val requiresImprovement: Boolean = false,
        val requiresDecline: Boolean = false,
        val requiresStrongSub: Boolean = false,
        val requiresWeakSub: Boolean = false,
        val bonusPriority: Int = 0
    )

    fun analyzeProfile(student: Student, term: Int): StudentTermProfile {
        val safeTerm = term.coerceIn(1, 3)
        val subs = student.subs()
        val enteredSubs = subs.filter { student.hasSub(it, safeTerm) }
        val hasMarks = enteredSubs.isNotEmpty()

        val subPcts = enteredSubs.map { sub ->
            val pct = if (sub.max > 0) (student.total(sub, safeTerm) / sub.max) * 100.0 else 0.0
            sub to pct
        }

        val overallPct = if (enteredSubs.isNotEmpty()) {
            val sumGot = enteredSubs.sumOf { student.total(it, safeTerm) }
            val sumMax = enteredSubs.sumOf { it.max }
            if (sumMax > 0) (sumGot / sumMax) * 100.0 else null
        } else null

        val grade = overallPct?.let { gradeOf(it) }
        val resultBand = when {
            overallPct == null -> ResultBand.UNKNOWN
            overallPct >= 80.0 -> ResultBand.EXCELLENT
            overallPct >= 60.0 -> ResultBand.GOOD
            overallPct >= 40.0 -> ResultBand.AVERAGE
            else -> ResultBand.NEEDS_SUPPORT
        }

        val coreSubPcts = subPcts.filter { (sub, _) -> sub.key !in setOf("PE", "ART") }
            .ifEmpty { subPcts }

        val strongSubjects = coreSubPcts
            .filter { (_, pct) -> pct >= 75.0 }
            .sortedByDescending { it.second }
            .take(2)
            .map { shortSubName(it.first) }

        val weakSubjects = coreSubPcts
            .filter { (_, pct) -> pct < 55.0 }
            .sortedBy { it.second }
            .take(2)
            .map { shortSubName(it.first) }
            .ifEmpty {
                val lowest = coreSubPcts.minByOrNull { it.second }
                if (lowest != null && overallPct != null && lowest.second < overallPct - 10.0 && lowest.second < 72.0) {
                    listOf(shortSubName(lowest.first))
                } else emptyList()
            }

        val prevDelta = if (safeTerm > 1 && overallPct != null) {
            val prevEntered = subs.filter { student.hasSub(it, safeTerm - 1) }
            if (prevEntered.isNotEmpty()) {
                val prevGot = prevEntered.sumOf { student.total(it, safeTerm - 1) }
                val prevMax = prevEntered.sumOf { it.max }
                if (prevMax > 0) overallPct - (prevGot / prevMax) * 100.0 else null
            } else null
        } else null

        val days = student.x["DAYS_$safeTerm"]?.let { fromBn(it).toDoubleOrNull() }
        val present = student.x["PRES_$safeTerm"]?.let { fromBn(it).toDoubleOrNull() }
        val attPct = if (days != null && present != null && days > 0.0) {
            ((present / days) * 100.0).coerceIn(0.0, 100.0)
        } else null

        val attendanceBand = when {
            attPct == null -> AttendanceBand.UNKNOWN
            attPct >= 85.0 -> AttendanceBand.REGULAR
            attPct >= 65.0 -> AttendanceBand.MODERATE
            else -> AttendanceBand.LOW
        }

        val act = student.x["ACT_$safeTerm"]?.trim()?.takeIf { it in setOf("ক", "খ", "গ") }
        val disc = student.x["DISC_$safeTerm"]?.trim()?.takeIf { it in setOf("ক", "খ", "গ") }
        val cond = student.x["COND_$safeTerm"]?.trim()?.takeIf { it in setOf("ক", "খ", "গ") }
        val traitValues = listOfNotNull(act, disc, cond)

        val traitsBand = when {
            traitValues.isEmpty() -> TraitsBand.UNKNOWN
            traitValues.any { it == "গ" } -> TraitsBand.NEEDS_GUIDANCE
            traitValues.count { it == "ক" } >= 2 || (traitValues.size == 1 && traitValues.first() == "ক") -> TraitsBand.EXEMPLARY
            else -> TraitsBand.GOOD
        }

        return StudentTermProfile(
            term = safeTerm,
            hasMarks = hasMarks,
            overallPct = overallPct,
            grade = grade,
            resultBand = resultBand,
            strongSubjects = strongSubjects,
            weakSubjects = weakSubjects,
            prevDelta = prevDelta,
            attPct = attPct,
            attendanceBand = attendanceBand,
            act = act,
            disc = disc,
            cond = cond,
            traitsBand = traitsBand
        )
    }

    fun bestRemark(student: Student, term: Int): TeacherRemarkOption {
        return getRankedAlternatives(student, term).first()
    }

    fun autoPrefillIfApplicable(student: Student, term: Int): String? {
        return CommentGenerator.generateComment(student, term)
    }

    fun getRankedAlternatives(
        student: Student,
        term: Int,
        category: RemarkCategory = RemarkCategory.ALL
    ): List<TeacherRemarkOption> {
        val profile = analyzeProfile(student, term)

        val generated = CommentGenerator.generateDetailedAlternatives(student, term).map { g ->
            val mappedCat = when (g.category) {
                CommentCategory.COMBINED -> RemarkCategory.COMBINED
                CommentCategory.SUBJECT_ANALYSIS -> RemarkCategory.SUBJECT_DETAIL
                CommentCategory.ACADEMIC -> RemarkCategory.ACADEMIC
                CommentCategory.ATTENDANCE -> RemarkCategory.ATTENDANCE
                CommentCategory.TRAITS -> RemarkCategory.TRAITS
                CommentCategory.ENCOURAGEMENT -> RemarkCategory.ENCOURAGEMENT
            }
            TeacherRemarkOption(
                id = g.id,
                text = g.text,
                category = mappedCat,
                toneLabel = g.toneLabel,
                matchScore = g.matchScore,
                isTopPick = g.id == "gen_comb_var_0",
                wordCount = g.wordCount
            )
        }

        val templates = buildTemplates(profile)
        val curated = templates.map { tpl ->
            val score = computeMatchScore(profile, tpl)
            TeacherRemarkOption(
                id = tpl.id,
                text = tpl.text,
                category = tpl.category,
                toneLabel = tpl.toneLabel,
                matchScore = score.coerceAtMost(94),
                isTopPick = false,
                wordCount = CommentGenerator.countWords(tpl.text)
            )
        }

        val merged = (generated + curated)
            .distinctBy { it.text.trim() }
            .sortedWith(
                compareByDescending<TeacherRemarkOption> { it.isTopPick }
                    .thenByDescending { it.matchScore }
                    .thenBy { it.category.ordinal }
            )

        val topText = merged.firstOrNull()?.text
        val marked = merged.mapIndexed { idx, opt ->
            opt.copy(isTopPick = idx == 0 || opt.text == topText)
        }

        return if (category == RemarkCategory.ALL) {
            marked
        } else {
            marked.filter { it.category == category }
        }
    }

    private fun shortSubName(sub: Sub): String = when (sub.key) {
        "REL" -> "ধর্মশিক্ষা"
        "SOC" -> "সামাজিক ও বিজ্ঞান"
        "BGW" -> "বাংলাদেশ ও বিশ্বপরিচয়"
        "PE" -> "শারীরিক শিক্ষা"
        else -> sub.bn
    }

    private fun joinBnList(items: List<String>): String = when (items.size) {
        0 -> ""
        1 -> items[0]
        else -> "${items[0]} ও ${items[1]}"
    }

    private fun termLabel(term: Int): String = when (term) {
        1 -> "১ম প্রান্তিক"
        2 -> "২য় প্রান্তিক"
        else -> "বার্ষিক মূল্যায়ন"
    }

    private fun computeMatchScore(p: StudentTermProfile, t: CandidateTemplate): Int {
        var score = 56 + t.bonusPriority

        // 1. Result alignment (up to +20, or -18 mismatch)
        if (p.resultBand != ResultBand.UNKNOWN) {
            when {
                t.idealResults.isEmpty() -> score += 4
                p.resultBand in t.idealResults -> score += 20
                p.resultBand in t.nearbyResults -> score += 10
                else -> score -= 18
            }
        } else {
            if (t.idealResults.isEmpty()) score += 10 else score -= 6
        }

        // 2. Attendance alignment (up to +14, or -14 mismatch)
        if (p.attendanceBand != AttendanceBand.UNKNOWN) {
            when {
                t.idealAttendance.isEmpty() -> score += 3
                p.attendanceBand in t.idealAttendance -> score += 14
                p.attendanceBand in t.nearbyAttendance -> score += 6
                else -> score -= 14
            }
        } else {
            if (t.idealAttendance.isEmpty()) score += 6 else score -= 5
        }

        // 3. Traits alignment (up to +14, or -14 mismatch)
        if (p.traitsBand != TraitsBand.UNKNOWN) {
            when {
                t.idealTraits.isEmpty() -> score += 3
                p.traitsBand in t.idealTraits -> score += 14
                p.traitsBand in t.nearbyTraits -> score += 6
                else -> score -= 14
            }
        } else {
            if (t.idealTraits.isEmpty()) score += 6 else score -= 5
        }

        // 4. Term preference
        if (t.preferredTerms.isNotEmpty()) {
            score += if (p.term in t.preferredTerms) 8 else -12
        } else if (p.term == 3) {
            // slightly prefer term-agnostic or annual remarks in term 3
            score += 1
        }

        // 5. Personalization bonuses
        if (t.requiresStrongSub) {
            score += if (p.strongSubjects.isNotEmpty()) 7 else -16
        }
        if (t.requiresWeakSub) {
            score += if (p.weakSubjects.isNotEmpty()) 7 else -16
        }
        if (t.requiresImprovement) {
            score += when {
                p.prevDelta != null && p.prevDelta >= 4.0 -> 11
                p.prevDelta != null && p.prevDelta >= 1.5 -> 3
                else -> -14
            }
        }
        if (t.requiresDecline) {
            score += when {
                p.prevDelta != null && p.prevDelta <= -5.0 -> 10
                else -> -14
            }
        }

        // 6. Combined remarks slightly preferred as #1 default when multiple dimensions exist
        val filledDims = listOf(
            p.resultBand != ResultBand.UNKNOWN,
            p.attendanceBand != AttendanceBand.UNKNOWN,
            p.traitsBand != TraitsBand.UNKNOWN
        ).count { it }
        if (filledDims >= 2 && t.category == RemarkCategory.COMBINED) {
            score += 4
        }

        return score.coerceIn(18, 99)
    }

    private fun buildTemplates(p: StudentTermProfile): List<CandidateTemplate> {
        val strongStr = joinBnList(p.strongSubjects)
        val weakStr = joinBnList(p.weakSubjects)
        val tLbl = termLabel(p.term)
        val actGood = p.act == "ক"
        val discGood = p.disc == "ক"
        val condGood = p.cond == "ক"

        val list = mutableListOf<CandidateTemplate>()

        // ============================================================
        // A. COMBINED (সমন্বিত: ফলাফল + উপস্থিতি + গুণাবলি)
        // ============================================================
        list += CandidateTemplate(
            id = "comb_exc_reg_exm_1",
            text = "পাঠ্যবিষয়ে অত্যন্ত মনোযোগী ও নিয়মিত। অসাধারণ ফলাফল এবং শৃঙ্খলাবোধ প্রশংসনীয়।",
            category = RemarkCategory.COMBINED,
            toneLabel = "সমন্বিত ও প্রশংসামূলক",
            idealResults = setOf(ResultBand.EXCELLENT),
            nearbyResults = setOf(ResultBand.GOOD),
            idealAttendance = setOf(AttendanceBand.REGULAR),
            nearbyAttendance = setOf(AttendanceBand.MODERATE),
            idealTraits = setOf(TraitsBand.EXEMPLARY),
            nearbyTraits = setOf(TraitsBand.GOOD),
            bonusPriority = 3
        )
        if (strongStr.isNotEmpty()) {
            list += CandidateTemplate(
                id = "comb_exc_sub_1",
                text = "নিয়মিত উপস্থিতি ও ভদ্র আচরণ প্রশংসনীয়। বিশেষ করে $strongStr-এ চমৎকার দক্ষতা দেখিয়েছে।",
                category = RemarkCategory.COMBINED,
                toneLabel = "বিষয়ভিত্তিক ও সমন্বিত",
                idealResults = setOf(ResultBand.EXCELLENT, ResultBand.GOOD),
                nearbyResults = setOf(ResultBand.AVERAGE),
                idealAttendance = setOf(AttendanceBand.REGULAR),
                nearbyAttendance = setOf(AttendanceBand.MODERATE),
                idealTraits = setOf(TraitsBand.EXEMPLARY, TraitsBand.GOOD),
                requiresStrongSub = true,
                bonusPriority = 4
            )
        }
        list += CandidateTemplate(
            id = "comb_exc_reg_exm_2",
            text = "শ্রেণিকক্ষে সক্রিয় অংশগ্রহণ, নিয়মানুবর্তিতা ও অসাধারণ ফলাফল সবার জন্য অনুকরণীয়।",
            category = RemarkCategory.COMBINED,
            toneLabel = "প্রশংসামূলক",
            idealResults = setOf(ResultBand.EXCELLENT),
            nearbyResults = setOf(ResultBand.GOOD),
            idealAttendance = setOf(AttendanceBand.REGULAR),
            nearbyAttendance = setOf(AttendanceBand.MODERATE),
            idealTraits = setOf(TraitsBand.EXEMPLARY),
            nearbyTraits = setOf(TraitsBand.GOOD),
            bonusPriority = 2
        )
        list += CandidateTemplate(
            id = "comb_exc_low_att",
            text = "মেধাবী ও পাঠে দক্ষ, ফলাফল অত্যন্ত ভালো। তবে শ্রেণিতে আরও নিয়মিত উপস্থিত থাকা প্রয়োজন।",
            category = RemarkCategory.COMBINED,
            toneLabel = "প্রশংসা ও পরামর্শ",
            idealResults = setOf(ResultBand.EXCELLENT, ResultBand.GOOD),
            idealAttendance = setOf(AttendanceBand.LOW, AttendanceBand.MODERATE),
            idealTraits = setOf(TraitsBand.EXEMPLARY, TraitsBand.GOOD),
            bonusPriority = 4
        )
        list += CandidateTemplate(
            id = "comb_exc_trait_guide",
            text = "পড়াশোনায় বেশ ভালো ফলাফল করেছে। এর পাশাপাশি শ্রেণিকক্ষে শৃঙ্খলা ও আচরণে আরেকটু যত্নবান হলে আরও উজ্জ্বল করবে।",
            category = RemarkCategory.COMBINED,
            toneLabel = "গঠনমূলক পরামর্শ",
            idealResults = setOf(ResultBand.EXCELLENT, ResultBand.GOOD),
            idealAttendance = setOf(AttendanceBand.REGULAR, AttendanceBand.MODERATE),
            idealTraits = setOf(TraitsBand.NEEDS_GUIDANCE),
            bonusPriority = 4
        )
        list += CandidateTemplate(
            id = "comb_good_reg_good_1",
            text = "নিয়মিত উপস্থিতি ও ভদ্র আচরণ প্রশংসনীয়। সার্বিক ফলাফল ভালো হয়েছে; এই ধারাবাহিকতা বজায় রাখুক।",
            category = RemarkCategory.COMBINED,
            toneLabel = "সমন্বিত",
            idealResults = setOf(ResultBand.GOOD),
            nearbyResults = setOf(ResultBand.EXCELLENT, ResultBand.AVERAGE),
            idealAttendance = setOf(AttendanceBand.REGULAR),
            nearbyAttendance = setOf(AttendanceBand.MODERATE),
            idealTraits = setOf(TraitsBand.EXEMPLARY, TraitsBand.GOOD),
            bonusPriority = 3
        )
        if (strongStr.isNotEmpty() && weakStr.isNotEmpty()) {
            list += CandidateTemplate(
                id = "comb_good_strong_weak",
                text = "$strongStr-এ ভালো অগ্রগতি দেখিয়েছে। পাশাপাশি $weakStr বিষয়ে নিয়মিত অনুশীলন করলে আরও ভালো করবে।",
                category = RemarkCategory.COMBINED,
                toneLabel = "বিষয়-নির্দেশিত",
                idealResults = setOf(ResultBand.GOOD, ResultBand.AVERAGE),
                nearbyResults = setOf(ResultBand.EXCELLENT),
                idealAttendance = setOf(AttendanceBand.REGULAR, AttendanceBand.MODERATE),
                idealTraits = setOf(TraitsBand.EXEMPLARY, TraitsBand.GOOD),
                requiresStrongSub = true,
                requiresWeakSub = true,
                bonusPriority = 5
            )
        }
        if (weakStr.isNotEmpty()) {
            list += CandidateTemplate(
                id = "comb_good_weak_only",
                text = "শ্রেণিতে উপস্থিতি ও আচরণ সন্তোষজনক। $weakStr বিষয়ে একটু বাড়তি মনোযোগ দিলে ফলাফল আরও উন্নত হবে।",
                category = RemarkCategory.COMBINED,
                toneLabel = "উৎসাহ ও পরামর্শ",
                idealResults = setOf(ResultBand.GOOD, ResultBand.AVERAGE),
                idealAttendance = setOf(AttendanceBand.REGULAR, AttendanceBand.MODERATE),
                idealTraits = setOf(TraitsBand.EXEMPLARY, TraitsBand.GOOD),
                requiresWeakSub = true,
                bonusPriority = 4
            )
        }
        list += CandidateTemplate(
            id = "comb_good_mod_att",
            text = "পাঠ্যবিষয়ে আগ্রহ ও ফলাফল ভালো। নিয়মিত বিদ্যালয়ে উপস্থিত থাকলে আরও চমৎকার সাফল্য অর্জন করবে।",
            category = RemarkCategory.COMBINED,
            toneLabel = "সমন্বিত",
            idealResults = setOf(ResultBand.GOOD),
            nearbyResults = setOf(ResultBand.AVERAGE),
            idealAttendance = setOf(AttendanceBand.MODERATE, AttendanceBand.LOW),
            idealTraits = setOf(TraitsBand.EXEMPLARY, TraitsBand.GOOD),
            bonusPriority = 3
        )
        list += CandidateTemplate(
            id = "comb_avg_reg_good",
            text = "নিয়মিত উপস্থিতি ও ভালো আচরণ ইতিবাচক। তবে পাঠ্যবিষয়ে আরেকটু মনোযোগ ও অনুশীলন বাড়ালে ফলাফল অনেক ভালো হবে।",
            category = RemarkCategory.COMBINED,
            toneLabel = "উৎসাহমূলক",
            idealResults = setOf(ResultBand.AVERAGE),
            nearbyResults = setOf(ResultBand.GOOD, ResultBand.NEEDS_SUPPORT),
            idealAttendance = setOf(AttendanceBand.REGULAR, AttendanceBand.MODERATE),
            idealTraits = setOf(TraitsBand.EXEMPLARY, TraitsBand.GOOD),
            bonusPriority = 4
        )
        list += CandidateTemplate(
            id = "comb_avg_low_att",
            text = "অনিয়মিত উপস্থিতির কারণে কাঙ্ক্ষিত মান অর্জন করতে পারেনি। নিয়মিত শ্রেণিতে উপস্থিতি ও বাসায় চর্চা বাড়ানো জরুরি।",
            category = RemarkCategory.COMBINED,
            toneLabel = "সতর্ক ও পরামর্শমূলক",
            idealResults = setOf(ResultBand.AVERAGE, ResultBand.NEEDS_SUPPORT),
            idealAttendance = setOf(AttendanceBand.LOW, AttendanceBand.MODERATE),
            idealTraits = setOf(TraitsBand.GOOD, TraitsBand.EXEMPLARY, TraitsBand.NEEDS_GUIDANCE),
            bonusPriority = 4
        )
        list += CandidateTemplate(
            id = "comb_avg_trait_guide",
            text = "ফলাফল মোটামুটি সন্তোষজনক। শ্রেণিকক্ষে মনোযোগ, নিয়মানুবর্তিতা ও আচরণে আরও যত্নশীল হলে দ্রুত উন্নতি করবে।",
            category = RemarkCategory.COMBINED,
            toneLabel = "উপদেশমূলক",
            idealResults = setOf(ResultBand.AVERAGE, ResultBand.GOOD),
            idealAttendance = setOf(AttendanceBand.REGULAR, AttendanceBand.MODERATE),
            idealTraits = setOf(TraitsBand.NEEDS_GUIDANCE),
            bonusPriority = 4
        )
        list += CandidateTemplate(
            id = "comb_supp_good_trait",
            text = "শিক্ষার্থীর আচরণ ভদ্র ও আন্তরিক, তবে পাঠ্যবিষয়ে অতিরিক্ত যত্ন ও নিয়মিত চর্চা প্রয়োজন। অভিভাবকের সহযোগিতা কাম্য।",
            category = RemarkCategory.COMBINED,
            toneLabel = "সহানুভূতিশীল পরামর্শ",
            idealResults = setOf(ResultBand.NEEDS_SUPPORT),
            nearbyResults = setOf(ResultBand.AVERAGE),
            idealAttendance = setOf(AttendanceBand.REGULAR, AttendanceBand.MODERATE),
            idealTraits = setOf(TraitsBand.EXEMPLARY, TraitsBand.GOOD),
            bonusPriority = 4
        )
        list += CandidateTemplate(
            id = "comb_supp_low_att",
            text = "অনিয়মিত উপস্থিতি ও অনুশীলনের ঘাটতিতে ফলাফল দুর্বল হয়েছে। নিয়মিত উপস্থিতি ও নিবিড় তত্ত্বাবধান একান্ত প্রয়োজন।",
            category = RemarkCategory.COMBINED,
            toneLabel = "সতর্কতামূলক",
            idealResults = setOf(ResultBand.NEEDS_SUPPORT),
            nearbyResults = setOf(ResultBand.AVERAGE),
            idealAttendance = setOf(AttendanceBand.LOW),
            nearbyAttendance = setOf(AttendanceBand.MODERATE),
            idealTraits = setOf(TraitsBand.GOOD, TraitsBand.NEEDS_GUIDANCE),
            bonusPriority = 5
        )
        list += CandidateTemplate(
            id = "comb_supp_trait_guide",
            text = "পড়াশোনা ও শ্রেণি-শৃঙ্খলায় বিশেষ মনোযোগ প্রয়োজন। নিয়মিত বিদ্যালয়ে আসা এবং শিক্ষক-অভিভাবকের সম্মিলিত যত্ন আবশ্যক।",
            category = RemarkCategory.COMBINED,
            toneLabel = "সমন্বিত উপদেশ",
            idealResults = setOf(ResultBand.NEEDS_SUPPORT),
            nearbyResults = setOf(ResultBand.AVERAGE),
            idealTraits = setOf(TraitsBand.NEEDS_GUIDANCE),
            bonusPriority = 5
        )

        // Term 3 / Annual Specific Combined Remarks
        list += CandidateTemplate(
            id = "comb_t3_exc",
            text = "বার্ষিক মূল্যায়নে সকল বিষয়ে ধারাবাহিক উৎকর্ষ অর্জন করেছে। পরবর্তী শ্রেণিতে উত্তীর্ণ হওয়ার যোগ্য; আন্তরিক অভিনন্দন।",
            category = RemarkCategory.COMBINED,
            toneLabel = "বার্ষিক উত্তীর্ণ (উৎকর্ষ)",
            idealResults = setOf(ResultBand.EXCELLENT),
            nearbyResults = setOf(ResultBand.GOOD),
            idealAttendance = setOf(AttendanceBand.REGULAR),
            nearbyAttendance = setOf(AttendanceBand.MODERATE),
            idealTraits = setOf(TraitsBand.EXEMPLARY, TraitsBand.GOOD),
            preferredTerms = setOf(3),
            bonusPriority = 5
        )
        list += CandidateTemplate(
            id = "comb_t3_good",
            text = "বার্ষিক মূল্যায়নে উত্তম ফলাফল ও প্রশংসনীয় আচরণ প্রদর্শন করেছে। পরবর্তী শ্রেণির জন্য শুভকামনা ও অভিনন্দন।",
            category = RemarkCategory.COMBINED,
            toneLabel = "বার্ষিক উত্তীর্ণ (উত্তম)",
            idealResults = setOf(ResultBand.GOOD),
            nearbyResults = setOf(ResultBand.EXCELLENT),
            idealAttendance = setOf(AttendanceBand.REGULAR, AttendanceBand.MODERATE),
            idealTraits = setOf(TraitsBand.EXEMPLARY, TraitsBand.GOOD),
            preferredTerms = setOf(3),
            bonusPriority = 4
        )
        list += CandidateTemplate(
            id = "comb_t3_avg",
            text = "বার্ষিক মূল্যায়নে সন্তোষজনক ফলাফল অর্জন করেছে। পরবর্তী শ্রেণিতে আরও ভালো করার লক্ষ্যে নিয়মিত অনুশীলন প্রয়োজন।",
            category = RemarkCategory.COMBINED,
            toneLabel = "বার্ষিক অগ্রগতি",
            idealResults = setOf(ResultBand.AVERAGE),
            nearbyResults = setOf(ResultBand.GOOD),
            preferredTerms = setOf(3),
            bonusPriority = 3
        )
        list += CandidateTemplate(
            id = "comb_t3_supp",
            text = "বার্ষিক মূল্যায়নে ফলাফল উন্নতির সুযোগ রয়েছে। আগামী শিক্ষাবর্ষের শুরু থেকেই নিয়মিত উপস্থিতি ও নিবিড় অনুশীলন জরুরি।",
            category = RemarkCategory.COMBINED,
            toneLabel = "বার্ষিক পরামর্শ",
            idealResults = setOf(ResultBand.NEEDS_SUPPORT),
            nearbyResults = setOf(ResultBand.AVERAGE),
            preferredTerms = setOf(3),
            bonusPriority = 4
        )

        // ============================================================
        // B. ACADEMIC / RESULT & PROGRESS (ফলাফল ও প্রগতি)
        // ============================================================
        list += CandidateTemplate(
            id = "acad_exc_1",
            text = "$tLbl মূল্যায়নে অসাধারণ মেধা ও যোগ্যতার স্বাক্ষর রেখেছে। সাফল্যের এই ধারা ভবিষ্যতেও অব্যাহত থাকুক।",
            category = RemarkCategory.ACADEMIC,
            toneLabel = "ফলাফল-কেন্দ্রিক",
            idealResults = setOf(ResultBand.EXCELLENT),
            nearbyResults = setOf(ResultBand.GOOD),
            bonusPriority = 2
        )
        list += CandidateTemplate(
            id = "acad_exc_2",
            text = "সকল বিষয়ে অত্যন্ত সন্তোষজনক ও ভারসাম্যপূর্ণ দক্ষতা অর্জন করেছে। সৃজনশীল চর্চা চালিয়ে গেলে আরও এগিয়ে যাবে।",
            category = RemarkCategory.ACADEMIC,
            toneLabel = "দক্ষতা-মূল্যায়ন",
            idealResults = setOf(ResultBand.EXCELLENT),
            nearbyResults = setOf(ResultBand.GOOD)
        )
        if (strongStr.isNotEmpty()) {
            list += CandidateTemplate(
                id = "acad_strong_sub",
                text = "$strongStr-এ বিশেষ পারদর্শিতা দেখিয়েছে। প্রতিটি বিষয়েই ধারাবাহিক অনুশীলন ও আগ্রহ প্রশংসার দাবিদার।",
                category = RemarkCategory.ACADEMIC,
                toneLabel = "বিষয়-উৎকর্ষ",
                idealResults = setOf(ResultBand.EXCELLENT, ResultBand.GOOD),
                requiresStrongSub = true,
                bonusPriority = 2
            )
        }
        list += CandidateTemplate(
            id = "acad_good_1",
            text = "$tLbl মূল্যায়নে উত্তম ফলাফল করেছে। পাঠ্যবিষয় অনুধাবন ও লেখার মান বেশ ভালো; আরেকটু চেষ্টায় শীর্ষে পৌঁছাবে।",
            category = RemarkCategory.ACADEMIC,
            toneLabel = "উত্তম মূল্যায়ন",
            idealResults = setOf(ResultBand.GOOD),
            nearbyResults = setOf(ResultBand.EXCELLENT, ResultBand.AVERAGE),
            bonusPriority = 1
        )
        list += CandidateTemplate(
            id = "acad_good_2",
            text = "মূল বিষয়গুলোতে ভালো ভিত্তি তৈরি হয়েছে। পাঠ্যবই নিয়মিত চর্চা করলে আগামীতে আরও উৎকৃষ্ট ফলাফল আশা করা যায়।",
            category = RemarkCategory.ACADEMIC,
            toneLabel = "গঠনমূলক",
            idealResults = setOf(ResultBand.GOOD),
            nearbyResults = setOf(ResultBand.AVERAGE)
        )
        if (weakStr.isNotEmpty()) {
            list += CandidateTemplate(
                id = "acad_weak_sub",
                text = "অন্যান্য বিষয়ে অগ্রগতি ভালো হলেও $weakStr-এ বাড়তি মনোযোগ দেওয়া প্রয়োজন। নিয়মিত অনুশীলন করলে দ্রুত কাটিয়ে উঠবে।",
                category = RemarkCategory.ACADEMIC,
                toneLabel = "বিষয়-সহায়তা",
                idealResults = setOf(ResultBand.GOOD, ResultBand.AVERAGE, ResultBand.NEEDS_SUPPORT),
                requiresWeakSub = true,
                bonusPriority = 3
            )
        }
        list += CandidateTemplate(
            id = "acad_avg_1",
            text = "মোটামুটি সন্তোষজনক ফলাফল হয়েছে। মৌলিক দক্ষতা বাড়াতে প্রতিদিন নির্দিষ্ট সময় ধরে পড়াশোনা অনুশীলন করা উচিত।",
            category = RemarkCategory.ACADEMIC,
            toneLabel = "পরামর্শমূলক",
            idealResults = setOf(ResultBand.AVERAGE),
            nearbyResults = setOf(ResultBand.GOOD, ResultBand.NEEDS_SUPPORT)
        )
        list += CandidateTemplate(
            id = "acad_avg_2",
            text = "লিখিত ও শ্রেণি মূল্যায়নে আরও মনোযোগী হলে ফলাফল দ্রুত উন্নত হবে। শিক্ষকের নির্দেশনা নিয়মিত অনুসরণ করা দরকার।",
            category = RemarkCategory.ACADEMIC,
            toneLabel = "উন্নয়নমূলক",
            idealResults = setOf(ResultBand.AVERAGE),
            nearbyResults = setOf(ResultBand.NEEDS_SUPPORT)
        )
        list += CandidateTemplate(
            id = "acad_supp_1",
            text = "পঠন, লিখন ও মৌলিক হিসাব দক্ষতায় নিবিড় সহায়তা প্রয়োজন। সহজ ধাপে নিয়মিত চর্চা ও অভিভাবকের তদারকি জরুরি।",
            category = RemarkCategory.ACADEMIC,
            toneLabel = "বিশেষ যত্ন",
            idealResults = setOf(ResultBand.NEEDS_SUPPORT),
            nearbyResults = setOf(ResultBand.AVERAGE)
        )
        list += CandidateTemplate(
            id = "acad_improved",
            text = "$tLbl-এ পূর্বের তুলনায় উল্লেখযোগ্য উন্নতি করেছে, যা অত্যন্ত আশাব্যঞ্জক। এই পরিশ্রম ও আগ্রহ ধরে রাখুক।",
            category = RemarkCategory.ACADEMIC,
            toneLabel = "অগ্রগতি (উন্নতি)",
            idealResults = setOf(ResultBand.EXCELLENT, ResultBand.GOOD, ResultBand.AVERAGE),
            requiresImprovement = true,
            bonusPriority = 5
        )
        list += CandidateTemplate(
            id = "acad_declined",
            text = "পূর্বের তুলনায় এবারের ফলাফল কিছুটা কমেছে। পড়াশোনায় মনোযোগ বাড়িয়ে দ্রুত আগের অবস্থান ফিরে পেতে সচেষ্ট হতে হবে।",
            category = RemarkCategory.ACADEMIC,
            toneLabel = "মনোযোগ আকর্ষণ",
            idealResults = setOf(ResultBand.GOOD, ResultBand.AVERAGE, ResultBand.NEEDS_SUPPORT),
            requiresDecline = true,
            bonusPriority = 4
        )

        // ============================================================
        // C. ATTENDANCE (উপস্থিতি ও নিয়মানুবর্তিতা)
        // ============================================================
        list += CandidateTemplate(
            id = "att_reg_1",
            text = "বিদ্যালয়ে নিয়মিত ও সময়মতো উপস্থিতি অত্যন্ত প্রশংসনীয়। এই নিষ্ঠা পড়াশোনা ও সার্বিক বিকাশে সহায়ক হয়েছে।",
            category = RemarkCategory.ATTENDANCE,
            toneLabel = "নিয়মিত উপস্থিতি",
            idealAttendance = setOf(AttendanceBand.REGULAR),
            nearbyAttendance = setOf(AttendanceBand.MODERATE),
            bonusPriority = 1
        )
        list += CandidateTemplate(
            id = "att_reg_2",
            text = "প্রায় শতভাগ উপস্থিতি ও শ্রেণি-কার্যক্রমে ধারাবাহিক অংশগ্রহণ প্রশংসার যোগ্য। নিয়মানুবর্তিতার এ ধারা অব্যাহত থাকুক।",
            category = RemarkCategory.ATTENDANCE,
            toneLabel = "উপস্থিতি প্রশংসা",
            idealAttendance = setOf(AttendanceBand.REGULAR)
        )
        list += CandidateTemplate(
            id = "att_mod_1",
            text = "উপস্থিতি মোটামুটি ভালো, তবে মাঝে মাঝে অনুপস্থিতির কারণে পাঠের ধারাবাহিকতা ব্যাহত হয়। নিয়মিত উপস্থিত থাকার পরামর্শ রইল।",
            category = RemarkCategory.ATTENDANCE,
            toneLabel = "উপস্থিতি উন্নতি",
            idealAttendance = setOf(AttendanceBand.MODERATE),
            nearbyAttendance = setOf(AttendanceBand.REGULAR, AttendanceBand.LOW),
            bonusPriority = 1
        )
        list += CandidateTemplate(
            id = "att_mod_2",
            text = "অল্প কয়েকদিনের অনুপস্থিতি ছাড়া বিদ্যালয়ে নিয়মিত ছিল। শতভাগ উপস্থিতি নিশ্চিত করতে পারলে আরও ভালো করবে।",
            category = RemarkCategory.ATTENDANCE,
            toneLabel = "সহজ পরামর্শ",
            idealAttendance = setOf(AttendanceBand.MODERATE, AttendanceBand.REGULAR)
        )
        list += CandidateTemplate(
            id = "att_low_1",
            text = "বিদ্যালয়ে অনুপস্থিতির হার বেশি। নিয়মিত শ্রেণিতে উপস্থিতি নিশ্চিত করতে অভিভাবকের বিশেষ দৃষ্টি ও সহযোগিতা একান্ত কাম্য।",
            category = RemarkCategory.ATTENDANCE,
            toneLabel = "অনিয়মিত উপস্থিতি",
            idealAttendance = setOf(AttendanceBand.LOW),
            nearbyAttendance = setOf(AttendanceBand.MODERATE),
            bonusPriority = 2
        )
        list += CandidateTemplate(
            id = "att_low_2",
            text = "নিয়মিত বিদ্যালয়ে না আসার ফলে গুরুত্বপূর্ণ পাঠ বাদ পড়ছে। প্রতি কার্যদিবসে উপস্থিতি নিশ্চিত করা অত্যন্ত জরুরি।",
            category = RemarkCategory.ATTENDANCE,
            toneLabel = "উপস্থিতি তাগিদ",
            idealAttendance = setOf(AttendanceBand.LOW),
            nearbyAttendance = setOf(AttendanceBand.MODERATE)
        )

        // ============================================================
        // D. TRAITS & CHARACTER (গুণাবলি, সক্রিয়তা, শৃঙ্খলা, আচরণ)
        // ============================================================
        list += CandidateTemplate(
            id = "trait_exm_1",
            text = "অত্যন্ত ভদ্র, বিনয়ী ও শৃঙ্খলাপরায়ণ শিক্ষার্থী। সহপাঠীদের সাথে সহযোগিতামূলক আচরণ ও দলগত কাজে সক্রিয়তা প্রশংসনীয়।",
            category = RemarkCategory.TRAITS,
            toneLabel = "আদর্শ গুণাবলি",
            idealTraits = setOf(TraitsBand.EXEMPLARY),
            nearbyTraits = setOf(TraitsBand.GOOD),
            bonusPriority = 2
        )
        list += CandidateTemplate(
            id = "trait_exm_2",
            text = "শ্রেণিকক্ষে মনোযোগী, পরিচ্ছন্ন ও নিয়মানুবর্তী। শিক্ষকদের প্রতি শ্রদ্ধাশীল এবং সহপাঠক্রমিক কাজে উৎসাহী অংশগ্রহণ লক্ষণীয়।",
            category = RemarkCategory.TRAITS,
            toneLabel = "গুণাবলি প্রশংসা",
            idealTraits = setOf(TraitsBand.EXEMPLARY),
            nearbyTraits = setOf(TraitsBand.GOOD)
        )
        if (actGood) {
            list += CandidateTemplate(
                id = "trait_act_good",
                text = "শ্রেণিকাজ ও সহপাঠক্রমিক কার্যক্রমে দারুণ সক্রিয় ও প্রাণবন্ত। নেতৃত্বগুণ ও সৃজনশীল আগ্রহ আরও বিকশিত হোক।",
                category = RemarkCategory.TRAITS,
                toneLabel = "সক্রিয়তা ও নেতৃত্ব",
                idealTraits = setOf(TraitsBand.EXEMPLARY, TraitsBand.GOOD),
                bonusPriority = 2
            )
        }
        if (discGood && condGood) {
            list += CandidateTemplate(
                id = "trait_disc_cond",
                text = "চমৎকার শৃঙ্খলাবোধ ও মার্জিত আচরণে সবার প্রিয়পাত্র। বিদ্যালয়ের নিয়মকানুন নিষ্ঠার সাথে মেনে চলে।",
                category = RemarkCategory.TRAITS,
                toneLabel = "শৃঙ্খলা ও আচরণ",
                idealTraits = setOf(TraitsBand.EXEMPLARY, TraitsBand.GOOD),
                bonusPriority = 1
            )
        }
        list += CandidateTemplate(
            id = "trait_good_1",
            text = "শ্রেণিকক্ষে আচরণ ভালো এবং শিক্ষকের কথা মনোযোগ দিয়ে শোনে। সহপাঠক্রমিক কাজে আরেকটু স্বতঃস্ফূর্ত হলে আরও উজ্জ্বল হবে।",
            category = RemarkCategory.TRAITS,
            toneLabel = "ভালো আচরণ",
            idealTraits = setOf(TraitsBand.GOOD),
            nearbyTraits = setOf(TraitsBand.EXEMPLARY)
        )
        list += CandidateTemplate(
            id = "trait_good_2",
            text = "ভদ্র ও শান্ত স্বভাবের শিক্ষার্থী। দলগত আলোচনা ও শ্রেণি কার্যক্রমে আরও সক্রিয়ভাবে অংশগ্রহণ করতে উৎসাহিত করা হলো।",
            category = RemarkCategory.TRAITS,
            toneLabel = "সক্রিয়তার উৎসাহ",
            idealTraits = setOf(TraitsBand.GOOD),
            nearbyTraits = setOf(TraitsBand.EXEMPLARY, TraitsBand.NEEDS_GUIDANCE)
        )
        list += CandidateTemplate(
            id = "trait_guide_1",
            text = "শ্রেণিকক্ষে মনোযোগ ধরে রাখা এবং শৃঙ্খলা ও সময়ানুবর্তিতার চর্চা বাড়ানো প্রয়োজন। সঠিক দিকনির্দেশনায় সহজেই উন্নতি করবে।",
            category = RemarkCategory.TRAITS,
            toneLabel = "আচরণগত নির্দেশনা",
            idealTraits = setOf(TraitsBand.NEEDS_GUIDANCE),
            nearbyTraits = setOf(TraitsBand.GOOD),
            bonusPriority = 2
        )
        list += CandidateTemplate(
            id = "trait_guide_2",
            text = "পাঠদানের সময় অমনোযোগিতা কমিয়ে শান্ত ও সুশৃঙ্খল আচরণ চর্চা করা উচিত। ভালো অভ্যাসের বিকাশে শিক্ষক ও অভিভাবকের যত্ন কাম্য।",
            category = RemarkCategory.TRAITS,
            toneLabel = "শৃঙ্খলা উন্নতি",
            idealTraits = setOf(TraitsBand.NEEDS_GUIDANCE),
            nearbyTraits = setOf(TraitsBand.GOOD)
        )

        // ============================================================
        // E. ENCOURAGEMENT & ALTERNATIVE TONES (উৎসাহ ও কাছাকাছি সুর)
        // ============================================================
        list += CandidateTemplate(
            id = "enc_top_1",
            text = "উজ্জ্বল সম্ভাবনাময় শিক্ষার্থী। পাঠ্যবইয়ের পাশাপাশি সাধারণ জ্ঞান ও সৃজনশীল কাজে আগ্রহ ধরে রাখলে অসাধারণ কৃতিত্ব গড়বে।",
            category = RemarkCategory.ENCOURAGEMENT,
            toneLabel = "উচ্চাকাঙ্ক্ষী উৎসাহ",
            idealResults = setOf(ResultBand.EXCELLENT),
            nearbyResults = setOf(ResultBand.GOOD)
        )
        list += CandidateTemplate(
            id = "enc_top_2",
            text = "পরিশ্রম, সততা ও শেখার আগ্রহ প্রশংসনীয়। ভবিষ্যতে আরও বড় সাফল্যের জন্য আন্তরিক দোয়া ও শুভকামনা।",
            category = RemarkCategory.ENCOURAGEMENT,
            toneLabel = "শুভকামনামূলক",
            idealResults = setOf(ResultBand.EXCELLENT, ResultBand.GOOD),
            idealTraits = setOf(TraitsBand.EXEMPLARY, TraitsBand.GOOD)
        )
        list += CandidateTemplate(
            id = "enc_mid_1",
            text = "মেধা ও সামর্থ্য যথেষ্ট রয়েছে। প্রতিদিন নিয়মিত পড়াশোনা ও হাতের লেখার চর্চা করলে অচিরেই সেরা ফলাফল অর্জন করবে।",
            category = RemarkCategory.ENCOURAGEMENT,
            toneLabel = "আত্মবিশ্বাস বর্ধক",
            idealResults = setOf(ResultBand.GOOD, ResultBand.AVERAGE),
            nearbyResults = setOf(ResultBand.EXCELLENT, ResultBand.NEEDS_SUPPORT),
            bonusPriority = 1
        )
        list += CandidateTemplate(
            id = "enc_mid_2",
            text = "ধীরে ধীরে ভালো করছে এবং শেখার প্রতি আগ্রহ বাড়ছে। নিয়মিত অনুশীলন অব্যাহত রাখলে পরের প্রান্তিকে আরও চমৎকার করবে।",
            category = RemarkCategory.ENCOURAGEMENT,
            toneLabel = "উৎসাহব্যঞ্জক",
            idealResults = setOf(ResultBand.GOOD, ResultBand.AVERAGE),
            nearbyResults = setOf(ResultBand.NEEDS_SUPPORT)
        )
        list += CandidateTemplate(
            id = "enc_mid_3",
            text = "চেষ্টা ও মনোযোগ বাড়ালে খুব দ্রুত এগিয়ে যাওয়ার সম্ভাবনা আছে। নিয়মিত হোমওয়ার্ক ও শ্রেণির কাজ সম্পন্ন করার পরামর্শ রইল।",
            category = RemarkCategory.ENCOURAGEMENT,
            toneLabel = "সহযোগী ও ইতিবাচক",
            idealResults = setOf(ResultBand.AVERAGE, ResultBand.GOOD),
            nearbyResults = setOf(ResultBand.NEEDS_SUPPORT)
        )
        list += CandidateTemplate(
            id = "enc_supp_1",
            text = "মনোবল না হারিয়ে ছোট ছোট ধাপে নিয়মিত অনুশীলন চালিয়ে যাক। শিক্ষক ও অভিভাবকের সম্মিলিত উৎসাহে অবশ্যই ভালো করবে।",
            category = RemarkCategory.ENCOURAGEMENT,
            toneLabel = "সহানুভূতিশীল উৎসাহ",
            idealResults = setOf(ResultBand.NEEDS_SUPPORT, ResultBand.AVERAGE),
            bonusPriority = 1
        )
        list += CandidateTemplate(
            id = "enc_supp_2",
            text = "ব্যক্তিগত যত্ন ও আনন্দময় পরিবেশে শেখার সুযোগ পেলে সুপ্ত প্রতিভা বিকশিত হবে। নিয়মিত পাঠাভ্যাস গড়ে তোলা প্রয়োজন।",
            category = RemarkCategory.ENCOURAGEMENT,
            toneLabel = "ইতিবাচক পরামর্শ",
            idealResults = setOf(ResultBand.NEEDS_SUPPORT, ResultBand.AVERAGE)
        )
        list += CandidateTemplate(
            id = "enc_clean_short_1",
            text = "পাঠ্যবিষয়ে মনোযোগী ও নিয়মিত। সার্বিক অগ্রগতি সন্তোষজনক।",
            category = RemarkCategory.ENCOURAGEMENT,
            toneLabel = "সংক্ষিপ্ত ও প্রাঞ্জল",
            idealResults = setOf(ResultBand.EXCELLENT, ResultBand.GOOD),
            idealAttendance = setOf(AttendanceBand.REGULAR, AttendanceBand.MODERATE),
            idealTraits = setOf(TraitsBand.EXEMPLARY, TraitsBand.GOOD)
        )
        list += CandidateTemplate(
            id = "enc_clean_short_2",
            text = "আচরণ ভালো ও নিয়মানুবর্তী। নিয়মিত অনুশীলন করলে আরও ভালো ফলাফল করবে।",
            category = RemarkCategory.ENCOURAGEMENT,
            toneLabel = "সংক্ষিপ্ত",
            idealResults = setOf(ResultBand.GOOD, ResultBand.AVERAGE),
            idealTraits = setOf(TraitsBand.EXEMPLARY, TraitsBand.GOOD)
        )
        list += CandidateTemplate(
            id = "enc_clean_short_3",
            text = "নিয়মিত উপস্থিতি ও পাঠ্যবিষয়ে অতিরিক্ত যত্ন প্রয়োজন। আন্তরিক চেষ্টায় উন্নতি সম্ভব।",
            category = RemarkCategory.ENCOURAGEMENT,
            toneLabel = "সংক্ষিপ্ত পরামর্শ",
            idealResults = setOf(ResultBand.NEEDS_SUPPORT, ResultBand.AVERAGE),
            idealAttendance = setOf(AttendanceBand.LOW, AttendanceBand.MODERATE)
        )

        return list
    }
}
