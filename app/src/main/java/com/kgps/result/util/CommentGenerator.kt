package com.kgps.result.util

import com.kgps.result.Student
import com.kgps.result.Sub
import com.kgps.result.bn
import com.kgps.result.fromBn
import com.kgps.result.hasSub
import com.kgps.result.mk
import com.kgps.result.subs
import com.kgps.result.total
import kotlin.math.abs
import kotlin.math.roundToInt

enum class CommentGradeBand(val labelBn: String) {
    OTI_UTTOM("অতি উত্তম"),
    UTTOM("উত্তম"),
    SONTOSHJONOK("সন্তোষজনক"),
    SOHAYOTA_PROYOJON("সহায়তা প্রয়োজন")
}

enum class CommentCategory(val labelBn: String) {
    COMBINED("সমন্বিত"),
    SUBJECT_ANALYSIS("বিষয় খুঁটিনাটি"),
    ACADEMIC("ফলাফল ও প্রগতি"),
    ATTENDANCE("উপস্থিতি"),
    TRAITS("গুণাবলি ও আচরণ"),
    ENCOURAGEMENT("উৎসাহ ও পরামর্শ")
}

data class GeneratedCommentOption(
    val id: String,
    val text: String,
    val category: CommentCategory,
    val toneLabel: String,
    val matchScore: Int,
    val wordCount: Int
)

data class SubjectEval(
    val sub: Sub,
    val shortName: String,
    val ultraShortName: String,
    val obtained: Double,
    val fullMarks: Int,
    val percent: Double,
    val contPercent: Double?,
    val wrPercent: Double?,
    val tbPercent: Double?,
    val actPercent: Double?,
    val langPercent: Double?,
    val ctPercent: Double?,
    val prevDelta: Double?
)

data class DetailedTermAnalysis(
    val term: Int,
    val seed: Int,
    val subjectEvals: List<SubjectEval>,
    val enteredCount: Int,
    val totalSubjectsCount: Int,
    val averagePercent: Double?,
    val gradeBand: CommentGradeBand?,
    val prevTermAveragePercent: Double?,
    val termDelta: Double?,
    val strongSubjects: List<SubjectEval>,
    val weakSubjects: List<SubjectEval>,
    val borderlineSubjects: List<SubjectEval>,
    val allSubjectsWeak: Boolean,
    val topSubject: SubjectEval?,
    val lowestSubject: SubjectEval?,
    val mostImprovedSubject: SubjectEval?,
    val mostDeclinedSubject: SubjectEval?,
    val avgContPct: Double?,
    val avgWrPct: Double?,
    val avgTbPct: Double?,
    val avgLangPct: Double?,
    val avgCtPct: Double?,
    val workingDays: Int?,
    val presentDays: Int?,
    val attendancePercent: Double?,
    val actRating: String?,
    val discRating: String?,
    val condRating: String?
) {
    val hasMarks: Boolean get() = subjectEvals.isNotEmpty()
    val hasAttendance: Boolean get() = attendancePercent != null
    val hasTraits: Boolean get() = actRating != null || discRating != null || condRating != null
    val hasAnyData: Boolean get() = hasMarks || hasAttendance || hasTraits
}

object CommentGenerator {

    const val MAX_WORDS = 30

    private val WHITESPACE_REGEX = Regex("\\s+")

    // Gender-neutrality guard words (never emitted by generator)
    private val FORBIDDEN_GENDERED_WORDS = listOf("সে ", "তার ", "ছেলে", "মেয়ে", "ছাত্র ", "ছাত্রী ")

    fun countWords(text: String): Int {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0
        return trimmed.split(WHITESPACE_REGEX).count { it.isNotBlank() }
    }

    fun shortSubName(sub: Sub): String = when (sub.key) {
        "REL" -> "ধর্মশিক্ষা"
        "SOC" -> "সামাজিক ও বিজ্ঞান"
        "BGW" -> "বাংলাদেশ ও বিশ্বপরিচয়"
        "PE" -> "শারীরিক শিক্ষা"
        else -> sub.bn
    }

    fun ultraShortSubName(sub: Sub): String = when (sub.key) {
        "REL" -> "ধর্ম"
        "SOC" -> "বিজ্ঞান"
        "BGW" -> "বাওবি"
        "PE" -> "স্বাস্থ্য"
        else -> sub.bn
    }

    private fun joinNames(names: List<String>): String = when (names.size) {
        0 -> ""
        1 -> names[0]
        else -> "${names[0]} ও ${names[1]}"
    }

    private fun gradeBandOf(avgPct: Double): CommentGradeBand = when {
        avgPct >= 80.0 -> CommentGradeBand.OTI_UTTOM
        avgPct >= 60.0 -> CommentGradeBand.UTTOM
        avgPct >= 40.0 -> CommentGradeBand.SONTOSHJONOK
        else -> CommentGradeBand.SOHAYOTA_PROYOJON
    }

    fun analyze(student: Student, term: Int, previousTermAvg: Double? = null): DetailedTermAnalysis {
        val safeTerm = term.coerceIn(1, 3)
        val allSubs = student.subs()
        val enteredSubs = allSubs.filter { student.hasSub(it, safeTerm) }

        val prevSubjectPctMap = if (safeTerm > 1) {
            allSubs.filter { student.hasSub(it, safeTerm - 1) }.associate { sub ->
                val pct = if (sub.max > 0) (student.total(sub, safeTerm - 1) / sub.max) * 100.0 else 0.0
                sub.key to pct
            }
        } else emptyMap()

        val subjectEvals = enteredSubs.map { sub ->
            val obtained = student.total(sub, safeTerm)
            val pct = if (sub.max > 0) (obtained / sub.max) * 100.0 else 0.0
            val isArt = sub.key == "ART"

            fun compPct(compKey: String, maxVal: Int): Double? {
                if (isArt || maxVal <= 0) return null
                val raw = student.m[mk(sub.key, safeTerm, compKey)] ?: return null
                return (raw / maxVal) * 100.0
            }

            val contMax = sub.comps.sumOf { it.max }
            val hasCont = !isArt && (sub.comps.any { student.m.containsKey(mk(sub.key, safeTerm, it.key)) } ||
                student.m.containsKey(mk(sub.key, safeTerm, "CONT")))
            val contPct = if (hasCont && contMax > 0) {
                val contVal = sub.comps.fold(0.0) { acc, c ->
                    acc + (student.m[mk(sub.key, safeTerm, c.key)] ?: 0.0)
                }.let { sum ->
                    if (sub.comps.any { student.m.containsKey(mk(sub.key, safeTerm, it.key)) }) sum
                    else student.m[mk(sub.key, safeTerm, "CONT")] ?: 0.0
                }
                (contVal / contMax) * 100.0
            } else null

            val wrPct = if (!isArt && sub.written > 0 && student.m.containsKey(mk(sub.key, safeTerm, "WR"))) {
                ((student.m[mk(sub.key, safeTerm, "WR")] ?: 0.0) / sub.written) * 100.0
            } else null

            val tbMax = sub.comps.find { it.key == "TB" }?.max ?: 0
            val actMax = sub.comps.find { it.key == "ACT" }?.max ?: 0
            val langMax = sub.comps.find { it.key == "LANG" }?.max ?: 0
            val ctMax = sub.comps.find { it.key == "CT" }?.max ?: 0

            val prevPct = prevSubjectPctMap[sub.key]
            SubjectEval(
                sub = sub,
                shortName = shortSubName(sub),
                ultraShortName = ultraShortSubName(sub),
                obtained = obtained,
                fullMarks = sub.max,
                percent = pct,
                contPercent = contPct,
                wrPercent = wrPct,
                tbPercent = compPct("TB", tbMax),
                actPercent = compPct("ACT", actMax),
                langPercent = compPct("LANG", langMax),
                ctPercent = compPct("CT", ctMax),
                prevDelta = prevPct?.let { pct - it }
            )
        }

        val averagePercent = if (subjectEvals.isNotEmpty()) {
            val totalGot = subjectEvals.sumOf { it.obtained }
            val totalMax = subjectEvals.sumOf { it.fullMarks }
            if (totalMax > 0) (totalGot / totalMax) * 100.0 else null
        } else null

        val computedPrevAvg = previousTermAvg ?: if (safeTerm > 1) {
            val prevSubs = allSubs.filter { student.hasSub(it, safeTerm - 1) }
            if (prevSubs.isNotEmpty()) {
                val pg = prevSubs.sumOf { student.total(it, safeTerm - 1) }
                val pm = prevSubs.sumOf { it.max }
                if (pm > 0) (pg / pm) * 100.0 else null
            } else null
        } else null

        val termDelta = if (averagePercent != null && computedPrevAvg != null) {
            averagePercent - computedPrevAvg
        } else null

        val gradeBand = averagePercent?.let(::gradeBandOf)

        // Prioritize core academic subjects before PE/ART when picking representative highlights
        val rankedForStrong = subjectEvals
            .filter { it.percent >= 80.0 }
            .sortedWith(
                compareBy<SubjectEval> { it.sub.key in setOf("PE", "ART") }
                    .thenByDescending { it.percent }
            )
        val rankedForWeak = subjectEvals
            .filter { it.percent < 50.0 }
            .sortedWith(
                compareBy<SubjectEval> { it.sub.key in setOf("PE", "ART") }
                    .thenBy { it.percent }
            )
        val borderlineSubjects = subjectEvals
            .filter { it.percent >= 50.0 && it.percent < 65.0 }
            .sortedWith(
                compareBy<SubjectEval> { it.sub.key in setOf("PE", "ART") }
                    .thenBy { it.percent }
            )

        val allWeak = subjectEvals.isNotEmpty() && subjectEvals.all { it.percent < 50.0 }

        val days = student.x["DAYS_$safeTerm"]?.let { fromBn(it).toIntOrNull() }?.takeIf { it > 0 }
        val present = student.x["PRES_$safeTerm"]?.let { fromBn(it).toIntOrNull() }?.takeIf { it >= 0 }
        val attPct = if (days != null && present != null) {
            ((present.toDouble() / days.toDouble()) * 100.0).coerceIn(0.0, 100.0)
        } else null

        fun traitOf(prefix: String): String? =
            student.x["${prefix}_$safeTerm"]?.trim()?.takeIf { it in setOf("ক", "খ", "গ") }

        val contVals = subjectEvals.mapNotNull { it.contPercent }
        val wrVals = subjectEvals.mapNotNull { it.wrPercent }
        val tbVals = subjectEvals.mapNotNull { it.tbPercent }
        val langVals = subjectEvals.mapNotNull { it.langPercent }
        val ctVals = subjectEvals.mapNotNull { it.ctPercent }

        return DetailedTermAnalysis(
            term = safeTerm,
            seed = (student.roll * 31 + safeTerm * 7).coerceAtLeast(1),
            subjectEvals = subjectEvals,
            enteredCount = subjectEvals.size,
            totalSubjectsCount = allSubs.size,
            averagePercent = averagePercent,
            gradeBand = gradeBand,
            prevTermAveragePercent = computedPrevAvg,
            termDelta = termDelta,
            strongSubjects = rankedForStrong.take(2),
            weakSubjects = rankedForWeak.take(2),
            borderlineSubjects = borderlineSubjects.take(2),
            allSubjectsWeak = allWeak,
            topSubject = subjectEvals.maxByOrNull { it.percent },
            lowestSubject = subjectEvals.minByOrNull { it.percent },
            mostImprovedSubject = subjectEvals.filter { (it.prevDelta ?: 0.0) >= 6.0 }
                .maxByOrNull { it.prevDelta ?: 0.0 },
            mostDeclinedSubject = subjectEvals.filter { (it.prevDelta ?: 0.0) <= -6.0 }
                .minByOrNull { it.prevDelta ?: 0.0 },
            avgContPct = if (contVals.isNotEmpty()) contVals.average() else null,
            avgWrPct = if (wrVals.isNotEmpty()) wrVals.average() else null,
            avgTbPct = if (tbVals.isNotEmpty()) tbVals.average() else null,
            avgLangPct = if (langVals.isNotEmpty()) langVals.average() else null,
            avgCtPct = if (ctVals.isNotEmpty()) ctVals.average() else null,
            workingDays = days,
            presentDays = present,
            attendancePercent = attPct,
            actRating = traitOf("ACT"),
            discRating = traitOf("DISC"),
            condRating = traitOf("COND")
        )
    }

    private fun pickVariant(seed: Int, offset: Int, options: List<String>): String {
        if (options.isEmpty()) return ""
        val idx = abs(seed + offset) % options.size
        return options[idx]
    }

    /**
     * Generates the primary structured 5-part Bengali comment (<= 30 words),
     * independently per student and per term, using roll number as a deterministic seed.
     * Returns null if there is no entered data for the term.
     */
    fun generateComment(
        student: Student,
        term: Int,
        previousTermAvg: Double? = null,
        variantOffset: Int = 0
    ): String? {
        val a = analyze(student, term, previousTermAvg)
        if (!a.hasAnyData) return null
        return composeFivePartComment(a, variantOffset = variantOffset, allowStrongEvenForOtiUttom = false)
    }

    private data class SentenceSlot(
        val key: String,
        val text: String,
        val droppableOrder: Int // lower drops first; Int.MAX_VALUE = never drop
    )

    private fun composeFivePartComment(
        a: DetailedTermAnalysis,
        variantOffset: Int = 0,
        allowStrongEvenForOtiUttom: Boolean = false,
        useUltraShortNames: Boolean = false
    ): String {
        val seed = a.seed + variantOffset * 13
        val slots = mutableListOf<SentenceSlot>()

        // (1) Overall one-liner (never drop if present)
        val overallSentence = when (a.gradeBand) {
            CommentGradeBand.OTI_UTTOM -> pickVariant(
                seed, 0, listOf(
                    "সার্বিক ফলাফল অত্যন্ত প্রশংসনীয়।",
                    "সার্বিক ফলাফল অতি উত্তম হয়েছে।",
                    "মূল্যায়নে অসাধারণ কৃতিত্ব দেখিয়েছে।"
                )
            )
            CommentGradeBand.UTTOM -> pickVariant(
                seed, 1, listOf(
                    "সার্বিক ফলাফল ভালো।",
                    "মূল্যায়নের ফলাফল বেশ ভালো।",
                    "সার্বিক ফলাফল উত্তম হয়েছে।"
                )
            )
            CommentGradeBand.SONTOSHJONOK -> pickVariant(
                seed, 2, listOf(
                    "ফলাফল সন্তোষজনক।",
                    "সার্বিক ফলাফল মোটামুটি সন্তোষজনক।",
                    "ফলাফল সাধারণ মানের হয়েছে।"
                )
            )
            CommentGradeBand.SOHAYOTA_PROYOJON -> pickVariant(
                seed, 3, listOf(
                    "শিখন অর্জন আশানুরূপ নয়।",
                    "সার্বিক ফলাফলে বিশেষ সহায়তা প্রয়োজন।",
                    "মূল্যায়নে কাঙ্ক্ষিত মান অর্জিত হয়নি।"
                )
            )
            null -> ""
        }
        if (overallSentence.isNotEmpty()) {
            slots += SentenceSlot("overall", overallSentence, Int.MAX_VALUE)
        }

        // (2) Subject highlights (strong skipped if overall is already অতি উত্তম unless explicitly requested)
        val nameFn: (SubjectEval) -> String = { if (useUltraShortNames) it.ultraShortName else it.shortName }
        val showStrong = a.strongSubjects.isNotEmpty() &&
            (allowStrongEvenForOtiUttom || a.gradeBand != CommentGradeBand.OTI_UTTOM)
        val strongStr = joinNames(a.strongSubjects.map(nameFn))
        val weakStr = joinNames(a.weakSubjects.map(nameFn))

        when {
            a.allSubjectsWeak && a.hasMarks -> {
                val txt = pickVariant(
                    seed, 4, listOf(
                        "সব বিষয়ে সহায়তা প্রয়োজন।",
                        "সকল বিষয়ে নিবিড় যত্ন প্রয়োজন।"
                    )
                )
                slots += SentenceSlot("weak_all", txt, Int.MAX_VALUE)
            }
            showStrong && a.weakSubjects.isNotEmpty() -> {
                val combinedTxt = pickVariant(
                    seed, 5, listOf(
                        "$strongStr বিষয়ে ভালো করেছে, তবে $weakStr বিষয়ে উন্নতি প্রয়োজন।",
                        "$strongStr-এ দক্ষতা থাকলেও $weakStr বিষয়ে চর্চা বাড়ানো জরুরি।"
                    )
                )
                // Contains critical weak subject warning so treat paired as protected unless overflow requires splitting
                slots += SentenceSlot("sub_paired", combinedTxt, 100)
            }
            showStrong -> {
                val txt = pickVariant(
                    seed, 6, listOf(
                        "$strongStr বিষয়ে বিশেষ দক্ষতা দেখিয়েছে।",
                        "$strongStr বিষয়ে ভালো পারদর্শিতা রয়েছে।"
                    )
                )
                slots += SentenceSlot("strong", txt, 3) // drop 3rd
            }
            a.weakSubjects.isNotEmpty() -> {
                val txt = pickVariant(
                    seed, 7, listOf(
                        "$weakStr বিষয়ে উন্নতি প্রয়োজন।",
                        "বিশেষ করে $weakStr বিষয়ে সহায়তা প্রয়োজন।"
                    )
                )
                slots += SentenceSlot("weak", txt, Int.MAX_VALUE)
            }
            a.borderlineSubjects.isNotEmpty() && a.gradeBand == CommentGradeBand.SONTOSHJONOK -> {
                val bName = nameFn(a.borderlineSubjects.first())
                slots += SentenceSlot("borderline", "$bName বিষয়ে আরও চর্চা প্রয়োজন।", 5)
            }
        }

        // (3) Attendance
        a.attendancePercent?.let { att ->
            when {
                att >= 90.0 -> {
                    val txt = pickVariant(
                        seed, 8, listOf(
                            "উপস্থিতি নিয়মিত ও প্রশংসনীয়।",
                            "বিদ্যালয়ে নিয়মিত উপস্থিত ছিল।",
                            "উপস্থিতির হার অত্যন্ত প্রশংসনীয়।"
                        )
                    )
                    slots += SentenceSlot("att_good", txt, 2) // drop 2nd
                }
                att >= 75.0 -> {
                    val txt = pickVariant(
                        seed, 9, listOf(
                            "উপস্থিতি মোটামুটি ভালো।",
                            "শ্রেণিতে উপস্থিতি মোটামুটি নিয়মিত।"
                        )
                    )
                    slots += SentenceSlot("att_mod", txt, 2) // drop 2nd
                }
                else -> {
                    val txt = pickVariant(
                        seed, 10, listOf(
                            "উপস্থিতি অনিয়মিত।",
                            "বিদ্যালয়ে উপস্থিতি বেশ কম।"
                        )
                    )
                    slots += SentenceSlot("att_poor", txt, Int.MAX_VALUE)
                }
            }
        }

        // (4) গুণাবলি (সক্রিয়তা, শৃঙ্খলা, আচরণ)
        if (a.hasTraits) {
            val traitPairs = listOfNotNull(
                a.actRating?.let { "সক্রিয়তা" to it },
                a.discRating?.let { "শৃঙ্খলা" to it },
                a.condRating?.let { "আচরণ" to it }
            )
            val weakTraitNames = traitPairs.filter { it.second == "গ" }.map { it.first }
            val topTraitNames = traitPairs.filter { it.second == "ক" }.map { it.first }

            when {
                weakTraitNames.isNotEmpty() -> {
                    val txt = when (weakTraitNames.size) {
                        1 -> "${weakTraitNames[0]}-এ উন্নতি প্রয়োজন।"
                        2 -> "${weakTraitNames[0]} ও ${weakTraitNames[1]}-এ উন্নতি প্রয়োজন।"
                        else -> "সক্রিয়তা, শৃঙ্খলা ও আচরণে উন্নতি প্রয়োজন।"
                    }
                    slots += SentenceSlot("traits_weak", txt, Int.MAX_VALUE)
                }
                traitPairs.size == 3 && topTraitNames.size == 3 -> {
                    val txt = pickVariant(
                        seed, 11, listOf(
                            "সক্রিয়তা, শৃঙ্খলা ও আচরণে আদর্শ।",
                            "সকল গুণাবলিতে প্রশংসনীয় মান দেখিয়েছে।"
                        )
                    )
                    slots += SentenceSlot("traits_pos", txt, 1) // drop 1st
                }
                topTraitNames.isNotEmpty() -> {
                    val label = if (topTraitNames.size == 1) topTraitNames[0] else "${topTraitNames[0]} ও ${topTraitNames[1]}"
                    val txt = "$label গুণে প্রশংসনীয়।"
                    slots += SentenceSlot("traits_pos", txt, 1) // drop 1st
                }
                else -> {
                    slots += SentenceSlot("traits_pos", "গুণাবলিতে সন্তোষজনক।", 1) // drop 1st
                }
            }
        }

        // Optional trend note (if >=5 improvement and won't duplicate guidance)
        val significantImprovement = (a.termDelta ?: 0.0) >= 5.0
        val significantDecline = (a.termDelta ?: 0.0) <= -5.0

        // (5) Guidance (always last, short, never drop)
        val guidanceSentence = when {
            (a.attendancePercent ?: 100.0) < 75.0 && a.weakSubjects.isNotEmpty() -> pickVariant(
                seed, 12, listOf(
                    "নিয়মিত উপস্থিত থেকে দুর্বল বিষয়ে চর্চা বাড়াতে হবে।",
                    "নিয়মিত বিদ্যালয়ে উপস্থিত ও অনুশীলন জরুরি।"
                )
            )
            (a.attendancePercent ?: 100.0) < 75.0 -> pickVariant(
                seed, 13, listOf(
                    "নিয়মিত বিদ্যালয়ে উপস্থিত থাকতে হবে।",
                    "শ্রেণিতে নিয়মিত উপস্থিত থাকা জরুরি।"
                )
            )
            a.gradeBand == CommentGradeBand.SOHAYOTA_PROYOJON -> pickVariant(
                seed, 14, listOf(
                    "অভিভাবকের সহযোগিতা ও তদারকি কাম্য।",
                    "বাড়িতে পড়ার নিবিড় তদারকি ও সহায়তা প্রয়োজন।"
                )
            )
            significantImprovement -> pickVariant(
                seed, 15, listOf(
                    "এই অগ্রগতি ধরে রাখতে হবে।",
                    "উন্নতির এই ধারাবাহিকতা বজায় থাকুক।"
                )
            )
            significantDecline -> pickVariant(
                seed, 16, listOf(
                    "মনোযোগ বাড়িয়ে চর্চা জোরদার করতে হবে।",
                    "পূর্বের মান ফেরাতে নিয়মিত চর্চা দরকার।"
                )
            )
            a.weakSubjects.isNotEmpty() -> pickVariant(
                seed, 17, listOf(
                    "দুর্বল বিষয়ে প্রতিদিন অনুশীলন করতে হবে।",
                    "দুর্বল বিষয়ে চর্চা আরও বাড়াতে হবে।"
                )
            )
            a.gradeBand in setOf(CommentGradeBand.OTI_UTTOM, CommentGradeBand.UTTOM) -> pickVariant(
                seed, 18, listOf(
                    "এই ধারাবাহিকতা বজায় রাখতে হবে।",
                    "ধারাবাহিকতা বজায় রেখে এগিয়ে চলুক।"
                )
            )
            else -> pickVariant(
                seed, 19, listOf(
                    "নিয়মিত অনুশীলন আরও বাড়াতে হবে।",
                    "প্রতিদিন মনোযোগসহ অনুশীলন করতে হবে।"
                )
            )
        }
        slots += SentenceSlot("guidance", guidanceSentence, Int.MAX_VALUE)

        // Assemble and enforce 30-word hard limit by priority drop order:
        // (1) positive গুণাবলি -> (2) good attendance -> (3) strong subjects -> (4) trend
        val working = slots.toMutableList()
        while (countWords(working.joinToString(" ") { it.text }) > MAX_WORDS) {
            val candidateToDrop = working
                .filter { it.droppableOrder < Int.MAX_VALUE }
                .minByOrNull { it.droppableOrder }
                ?: break
            if (candidateToDrop.key == "sub_paired" && a.weakSubjects.isNotEmpty()) {
                // Convert paired strong+weak into concise weak-only rather than losing weak warning!
                val idx = working.indexOf(candidateToDrop)
                working[idx] = SentenceSlot("weak", "$weakStr বিষয়ে উন্নতি প্রয়োজন।", Int.MAX_VALUE)
            } else {
                working.remove(candidateToDrop)
            }
        }

        val result = working.joinToString(" ") { it.text }.trim()
        if (countWords(result) > MAX_WORDS && !useUltraShortNames) {
            return composeFivePartComment(
                a = a,
                variantOffset = variantOffset,
                allowStrongEvenForOtiUttom = allowStrongEvenForOtiUttom,
                useUltraShortNames = true
            )
        }
        return clampToMaxWords(result)
    }

    private fun clampToMaxWords(text: String): String {
        if (countWords(text) <= MAX_WORDS) return text.trim()
        // Drop whole sentences from the middle before touching overall or guidance if still over 30
        val sentences = text.split(Regex("(?<=।)\\s+")).filter { it.isNotBlank() }.toMutableList()
        while (sentences.size > 2 && countWords(sentences.joinToString(" ")) > MAX_WORDS) {
            sentences.removeAt(sentences.size - 2)
        }
        val joined = sentences.joinToString(" ").trim()
        if (countWords(joined) <= MAX_WORDS) return joined
        val words = joined.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
        return words.take(MAX_WORDS).joinToString(" ").trimEnd(',', ';') + if (!words.take(MAX_WORDS).last().endsWith("।")) "।" else ""
    }

    /**
     * Generates a rich, multi-angle library of concise (<= 30 words), teacher-crafted Bengali
     * alternatives reflecting granular subject marks, continuous vs written components,
     * attendance rate, trait breakdown, and term progression.
     */
    fun generateDetailedAlternatives(
        student: Student,
        term: Int,
        previousTermAvg: Double? = null
    ): List<GeneratedCommentOption> {
        val a = analyze(student, term, previousTermAvg)
        val out = mutableListOf<GeneratedCommentOption>()

        fun addOpt(
            id: String,
            rawText: String,
            cat: CommentCategory,
            tone: String,
            score: Int
        ) {
            val clean = clampToMaxWords(rawText.replace(Regex("\\s+"), " ").trim())
            if (clean.isBlank()) return
            if (FORBIDDEN_GENDERED_WORDS.any { clean.contains(it) }) return
            val wc = countWords(clean)
            if (wc == 0 || wc > MAX_WORDS) return
            out += GeneratedCommentOption(
                id = id,
                text = clean,
                category = cat,
                toneLabel = tone,
                matchScore = score.coerceIn(25, 99),
                wordCount = wc
            )
        }

        val termTitle = when (a.term) {
            1 -> "১ম প্রান্তিক"
            2 -> "২য় প্রান্তিক"
            else -> "বার্ষিক মূল্যায়ন"
        }

        // ------------------------------------------------------------------
        // 1. COMBINED SYNTHESES (Multiple Roll-Seeded & Tone Variants)
        // ------------------------------------------------------------------
        if (a.hasAnyData) {
            (0..2).forEach { v ->
                val text = composeFivePartComment(a, variantOffset = v, allowStrongEvenForOtiUttom = false)
                addOpt(
                    id = "gen_comb_var_$v",
                    rawText = text,
                    cat = CommentCategory.COMBINED,
                    tone = when (v) {
                        0 -> "সেরা অটো বিশ্লেষণ"
                        1 -> "সমন্বিত (ভিন্ন শৈলী)"
                        else -> "সমন্বিত (সংক্ষিপ্ত)"
                    },
                    score = 98 - v * 2
                )
            }

            if (a.gradeBand == CommentGradeBand.OTI_UTTOM && a.strongSubjects.isNotEmpty()) {
                val withSub = composeFivePartComment(a, variantOffset = 0, allowStrongEvenForOtiUttom = true)
                addOpt(
                    id = "gen_comb_with_strong",
                    rawText = withSub,
                    cat = CommentCategory.COMBINED,
                    tone = "বিষয় উল্লেখসহ শীর্ষ",
                    score = 96
                )
            }
        }

        // Term-3 / Annual promotion phrasing
        if (a.term == 3 && a.hasMarks) {
            when (a.gradeBand) {
                CommentGradeBand.OTI_UTTOM -> addOpt(
                    "gen_t3_exc",
                    "বার্ষিক মূল্যায়নে অসাধারণ কৃতিত্ব অর্জন করেছে। পরবর্তী শ্রেণিতে উত্তীর্ণ হওয়ায় অভিনন্দন; সাফল্যের ধারা অব্যাহত থাকুক।",
                    CommentCategory.COMBINED, "বার্ষিক উত্তীর্ণ", 95
                )
                CommentGradeBand.UTTOM -> addOpt(
                    "gen_t3_good",
                    "বার্ষিক মূল্যায়নে উত্তম ফলাফল ও প্রশংসনীয় আচরণ দেখিয়েছে। পরবর্তী শ্রেণিতে আরও ভালো ফলাফলের শুভকামনা।",
                    CommentCategory.COMBINED, "বার্ষিক উত্তীর্ণ", 93
                )
                CommentGradeBand.SONTOSHJONOK -> addOpt(
                    "gen_t3_avg",
                    "বার্ষিক মূল্যায়নে সন্তোষজনক ফলাফল হয়েছে। পরবর্তী শ্রেণির শুরু থেকেই নিয়মিত অনুশীলনে মনোযোগী হতে হবে।",
                    CommentCategory.COMBINED, "বার্ষিক অগ্রগতি", 90
                )
                CommentGradeBand.SOHAYOTA_PROYOJON -> addOpt(
                    "gen_t3_supp",
                    "বার্ষিক ফলাফল কাঙ্ক্ষিত নয়। আগামী শিক্ষাবর্ষের শুরু থেকে নিয়মিত উপস্থিতি ও নিবিড় চর্চায় অভিভাবকের সহযোগিতা কাম্য।",
                    CommentCategory.COMBINED, "বার্ষিক পরামর্শ", 91
                )
                null -> {}
            }
        }

        // ------------------------------------------------------------------
        // 2. GRANULAR SUBJECT-BY-SUBJECT & COMPONENT ANALYSIS (বিষয় খুঁটিনাটি)
        // ------------------------------------------------------------------
        a.subjectEvals.forEach { se ->
            val name = se.shortName
            val pRound = se.percent.roundToInt()
            // Subject-specific pedagogical detail lines
            when {
                se.percent >= 85.0 -> {
                    val detail = subjectStrengthPhrase(se)
                    addOpt(
                        "gen_sub_top_${se.sub.key}",
                        "$detail। সার্বিকভাবে পাঠ্যবিষয়ে আগ্রহ ও পরিশ্রম প্রশংসনীয়; এই দক্ষতা ধরে রাখতে হবে।",
                        CommentCategory.SUBJECT_ANALYSIS,
                        "$name (${bn(pRound.toString())}%)",
                        if (se == a.topSubject) 91 else 84
                    )
                }
                se.percent in 60.0..79.9 -> {
                    val growthHint = subjectActionPhrase(se)
                    addOpt(
                        "gen_sub_mid_${se.sub.key}",
                        "$name বিষয়ে অর্জন ভালো (${bn(pRound.toString())}%); $growthHint আরও চর্চা করলে অতি উত্তম মান অর্জন সম্ভব।",
                        CommentCategory.SUBJECT_ANALYSIS,
                        "$name বিশ্লেষণ",
                        80
                    )
                }
                se.percent < 50.0 -> {
                    val weakAction = subjectActionPhrase(se)
                    addOpt(
                        "gen_sub_weak_${se.sub.key}",
                        "$name বিষয়ে ফলাফল দুর্বল। $weakAction ও প্রতিদিন নিয়মিত অনুশীলন করা অত্যন্ত জরুরি।",
                        CommentCategory.SUBJECT_ANALYSIS,
                        "$name উন্নতি দরকার",
                        92
                    )
                }
                se.percent in 50.0..59.9 -> {
                    val nudge = subjectActionPhrase(se)
                    addOpt(
                        "gen_sub_border_${se.sub.key}",
                        "$name বিষয়ে মোটামুটি মান অর্জিত হয়েছে। $nudge বাড়ালে ফলাফল দ্রুত উত্তম পর্যায়ে পৌঁছাবে।",
                        CommentCategory.SUBJECT_ANALYSIS,
                        "$name তাগিদ",
                        85
                    )
                }
            }

            // Per-subject Written vs Continuous comparison
            val contP = se.contPercent
            val wrP = se.wrPercent
            if (contP != null && wrP != null) {
                when {
                    contP - wrP >= 15.0 -> addOpt(
                        "gen_sub_cont_gt_wr_${se.sub.key}",
                        "$name-এর শ্রেণিকাজ ও ধারাবাহিকে ভালো করলেও লিখিত অংশে নম্বর কমেছে; প্রশ্নোত্তর লেখার চর্চা বাড়াতে হবে।",
                        CommentCategory.SUBJECT_ANALYSIS,
                        "$name: ধারাবাহিক বনাম লিখিত",
                        87
                    )
                    wrP - contP >= 15.0 -> addOpt(
                        "gen_sub_wr_gt_cont_${se.sub.key}",
                        "$name-এর লিখিত অংশে ভালো করেছে, তবে ক্লাস টেস্ট ও ধারাবাহিক শ্রেণিকাজে আরও সক্রিয় হতে হবে।",
                        CommentCategory.SUBJECT_ANALYSIS,
                        "$name: শ্রেণিকাজ তাগিদ",
                        86
                    )
                }
            }

            // Per-subject term-over-term delta
            val delta = se.prevDelta
            if (delta != null) {
                when {
                    delta >= 8.0 -> addOpt(
                        "gen_sub_imp_${se.sub.key}",
                        "পূর্বের চেয়ে $name বিষয়ে উল্লেখযোগ্য উন্নতি করেছে। এই পরিশ্রম ও আগ্রহ ধরে রাখতে হবে।",
                        CommentCategory.SUBJECT_ANALYSIS,
                        "$name অগ্রগতি (+${bn(delta.roundToInt().toString())}%)",
                        89
                    )
                    delta <= -8.0 -> addOpt(
                        "gen_sub_dec_${se.sub.key}",
                        "গত প্রান্তিকের চেয়ে $name বিষয়ে নম্বর কিছুটা কমেছে। বাড়িতে নিয়মিত অনুশীলনে ফিরে আসা প্রয়োজন।",
                        CommentCategory.SUBJECT_ANALYSIS,
                        "$name সতর্কতা (-${bn(abs(delta).roundToInt().toString())}%)",
                        88
                    )
                }
            }
        }

        // Cross-subject component insights (CT, LANG, TB, Cont vs Wr overall)
        val avgCont = a.avgContPct
        val avgWr = a.avgWrPct
        if (avgCont != null && avgWr != null) {
            when {
                avgCont - avgWr >= 12.0 -> addOpt(
                    "gen_comp_cont_gt_wr",
                    "শ্রেণিকাজে অংশগ্রহণ ভালো হলেও সামষ্টিক লিখিত পরীক্ষায় প্রত্যাশিত নম্বর আসেনি। সময় ধরে লেখার অনুশীলন বাড়াতে হবে।",
                    CommentCategory.SUBJECT_ANALYSIS, "লিখিত চর্চা", 88
                )
                avgWr - avgCont >= 12.0 -> addOpt(
                    "gen_comp_wr_gt_cont",
                    "লিখিত মূল্যায়নে দক্ষতা ভালো, তবে প্রতিদিনের শ্রেণিকাজ ও ধারাবাহিক মূল্যায়নে আরও মনোযোগী হওয়া প্রয়োজন।",
                    CommentCategory.SUBJECT_ANALYSIS, "ধারাবাহিক মনোযোগ", 87
                )
                avgCont >= 80.0 && avgWr >= 80.0 -> addOpt(
                    "gen_comp_both_high",
                    "ধারাবাহিক শ্রেণিকাজ ও লিখিত পরীক্ষা—উভয় ক্ষেত্রের ভারসাম্যপূর্ণ প্রস্তুতি অত্যন্ত প্রশংসনীয়। এই ধারা বজায় থাকুক।",
                    CommentCategory.SUBJECT_ANALYSIS, "সুষম প্রস্তুতি", 88
                )
            }
        }

        if (a.avgLangPct != null && a.avgLangPct < 60.0) {
            addOpt(
                "gen_comp_lang_low",
                "ভাষাদক্ষতা ও পাঠ্যবইয়ের মূলভাব অনুধাবনে বাড়তি চর্চা প্রয়োজন। প্রতিদিন উচ্চৈঃস্বরে পাঠ ও শব্দার্থ অনুশীলন করতে হবে।",
                CommentCategory.SUBJECT_ANALYSIS, "ভাষা ও পঠনদক্ষতা", 87
            )
        }
        if (a.avgCtPct != null && a.avgCtPct < 60.0) {
            addOpt(
                "gen_comp_ct_low",
                "ক্লাস টেস্টের প্রস্তুতি দুর্বল ছিল। শ্রেণির পড়া নিয়মিত আয়ত্ত করে সাপ্তাহিক প্রস্তুতি জোরদার করতে হবে।",
                CommentCategory.SUBJECT_ANALYSIS, "ক্লাস টেস্ট প্রস্তুতি", 86
            )
        }
        if (a.avgTbPct != null && a.avgTbPct < 60.0) {
            addOpt(
                "gen_comp_tb_low",
                "পাঠ্যপুস্তক ও ওয়ার্কশিটের কাজগুলো নিয়মিত শেষ করার অভ্যাস গড়তে হবে। বাড়ির কাজে যত্নশীল হওয়া জরুরি।",
                CommentCategory.SUBJECT_ANALYSIS, "পাঠ্যবই ও অনুশীলন", 85
            )
        }

        // Strong vs Weak Subject Gap Analysis
        val topSub = a.topSubject
        val lowSub = a.lowestSubject
        if (topSub != null && lowSub != null && topSub.sub.key != lowSub.sub.key) {
            if (topSub.percent - lowSub.percent >= 22.0) {
                addOpt(
                    "gen_sub_gap",
                    "${topSub.shortName}-এ চমৎকার দখল থাকলেও ${lowSub.shortName} বিষয়ে তুলনামূলকভাবে পিছিয়ে আছে। উভয় বিষয়ে ভারসাম্য আনা প্রয়োজন।",
                    CommentCategory.SUBJECT_ANALYSIS, "বিষয়গত ভারসাম্য", 88
                )
            } else if (a.gradeBand in setOf(CommentGradeBand.OTI_UTTOM, CommentGradeBand.UTTOM)) {
                addOpt(
                    "gen_sub_balanced",
                    "বাংলা, গণিতসহ প্রায় সব বিষয়েই সুষম ও ধারাবাহিক দক্ষতা রয়েছে। সামগ্রিক প্রস্তুতি সন্তোষজনক ও আশাব্যঞ্জক।",
                    CommentCategory.SUBJECT_ANALYSIS, "সকল বিষয়ে ভারসাম্য", 84
                )
            }
        }

        // Paired core-skills observations (always useful alternatives)
        addOpt(
            "gen_sub_ban_eng_math_combo",
            when (a.gradeBand) {
                CommentGradeBand.OTI_UTTOM -> "মৌলিক পঠন, লিখন ও গাণিতিক যুক্তিতে দৃঢ় ভিত্তি তৈরি হয়েছে। সৃজনশীল অনুশীলনে আরও উৎকর্ষ সাধন করবে।"
                CommentGradeBand.UTTOM -> "পঠন ও হিসাবের ভিত্তি ভালো। শুদ্ধ বানান ও দ্রুত লেখার চর্চা বাড়ালে শীর্ষ মান অর্জন করবে।"
                CommentGradeBand.SONTOSHJONOK -> "পঠনদক্ষতা ও মৌলিক হিসাব অনুশীলনে প্রতিদিন সময় দেওয়া দরকার। সহজ ধাপে চর্চা করলে দ্রুত উন্নতি করবে।"
                else -> "বর্ণ-শব্দ চেনা, শুদ্ধ পঠন ও প্রাথমিক গণনায় নিবিড় সহায়তা প্রয়োজন। অভিভাবক ও শিক্ষকের যৌথ তদারকি কাম্য।"
            },
            CommentCategory.SUBJECT_ANALYSIS, "মৌলিক দক্ষতা বিশ্লেষণ", 83
        )

        // ------------------------------------------------------------------
        // 3. ACADEMIC & PROGRESS (ফলাফল ও প্রগতি)
        // ------------------------------------------------------------------
        if (a.hasMarks && a.averagePercent != null) {
            val pctBn = bn(a.averagePercent.roundToInt().toString())
            when (a.gradeBand) {
                CommentGradeBand.OTI_UTTOM -> {
                    addOpt("gen_acad_exc_1", "$termTitle-এ প্রায় $pctBn% নম্বরসহ অসাধারণ মেধা ও পরিশ্রমের স্বাক্ষর রেখেছে। এই ধারা বজায় থাকুক।", CommentCategory.ACADEMIC, "অতি উত্তম", 90)
                    addOpt("gen_acad_exc_2", "পাঠ্যবিষয় অনুধাবন ও প্রকাশক্ষমতা খুবই উঁচু মানের। সৃজনশীল চর্চা অব্যাহত রাখলে আরও উজ্জ্বল সাফল্য পাবে।", CommentCategory.ACADEMIC, "উৎকর্ষ মূল্যায়ন", 86)
                }
                CommentGradeBand.UTTOM -> {
                    addOpt("gen_acad_good_1", "$termTitle-এ $pctBn% নম্বর পেয়ে ভালো ফলাফল করেছে। আরেকটু গভীর মনোযোগে অতি উত্তম মানে পৌঁছাবে।", CommentCategory.ACADEMIC, "উত্তম মান", 89)
                    addOpt("gen_acad_good_2", "মূল পাঠ্যবিষয়গুলোতে ভালো দখল রয়েছে। নিয়মিত রিভিশন ও পরিচ্ছন্ন লেখার চর্চা আরও এগিয়ে নেবে।", CommentCategory.ACADEMIC, "গঠনমূলক", 85)
                }
                CommentGradeBand.SONTOSHJONOK -> {
                    addOpt("gen_acad_avg_1", "$termTitle-এ ফলাফল সন্তোষজনক ($pctBn%)। প্রতিদিন নির্দিষ্ট রুটিনে পড়াশোনা করলে পরের প্রান্তিকে অনেক ভালো করবে।", CommentCategory.ACADEMIC, "উন্নতির সুযোগ", 88)
                    addOpt("gen_acad_avg_2", "যোগ্যতা অনুযায়ী আরও ভালো করার সামর্থ্য রয়েছে। শ্রেণির পড়া নিয়মিত বাসায় চর্চা করা প্রয়োজন।", CommentCategory.ACADEMIC, "অনুশীলন তাগিদ", 84)
                }
                CommentGradeBand.SOHAYOTA_PROYOJON -> {
                    addOpt("gen_acad_supp_1", "$termTitle-এ প্রাপ্ত ফলাফল ($pctBn%) আশানুরূপ নয়। মৌলিক বিষয়গুলোতে নিয়মিত সহায়তা ও বাড়ির তদারকি একান্ত প্রয়োজন।", CommentCategory.ACADEMIC, "বিশেষ যত্ন", 90)
                    addOpt("gen_acad_supp_2", "পাঠ্যবিষয়ে দুর্বলতা কাটিয়ে উঠতে সহজ পাঠ-অনুশীলন ও প্রতিদিন নিয়মিত সময় দেওয়া আবশ্যক।", CommentCategory.ACADEMIC, "ধাপে ধাপে উন্নয়ন", 86)
                }
                null -> {}
            }
        }

        val overallDelta = a.termDelta
        if (overallDelta != null) {
            when {
                overallDelta >= 5.0 -> addOpt(
                    "gen_prog_up",
                    "$termTitle-এ পূর্বের প্রান্তিকের চেয়ে ${bn(overallDelta.roundToInt().toString())}% নম্বর বেড়েছে। এই প্রশংসনীয় অগ্রগতি ধরে রাখতে হবে।",
                    CommentCategory.ACADEMIC, "উল্লেখযোগ্য উন্নতি", 94
                )
                overallDelta <= -5.0 -> addOpt(
                    "gen_prog_down",
                    "পূর্বের তুলনায় এবার গড় নম্বর কিছুটা কমেছে। অমনোযোগিতা দূর করে নিয়মিত অনুশীলনে মনোযোগী হতে হবে।",
                    CommentCategory.ACADEMIC, "ফলাফল হ্রাস সতর্কতা", 91
                )
            }
        }

        // ------------------------------------------------------------------
        // 4. ATTENDANCE (উপস্থিতি ও নিয়মানুবর্তিতা)
        // ------------------------------------------------------------------
        a.attendancePercent?.let { att ->
            val attBn = bn(att.roundToInt().toString())
            val absent = ((a.workingDays ?: 0) - (a.presentDays ?: 0)).coerceAtLeast(0)
            when {
                att >= 90.0 -> {
                    addOpt("gen_att_reg_1", "$attBn% উপস্থিতি নিয়ে অত্যন্ত নিয়মিত ও সময়ানুবর্তী। শ্রেণি-কার্যক্রমে ধারাবাহিক উপস্থিতি ভালো ফলাফলে সহায়তা করেছে।", CommentCategory.ATTENDANCE, "নিয়মিত উপস্থিতি", 90)
                    addOpt("gen_att_reg_2", "বিদ্যালয়ে প্রায় শতভাগ উপস্থিতি ও সময়ের প্রতি নিষ্ঠা প্রশংসনীয়। নিয়মানুবর্তিতার এই চর্চা অব্যাহত থাকুক।", CommentCategory.ATTENDANCE, "সময়ানুবর্তিতা", 85)
                }
                att >= 75.0 -> {
                    addOpt("gen_att_mod_1", "উপস্থিতি মোটামুটি ভালো ($attBn%), তবে ${bn(absent.toString())} দিন অনুপস্থিত ছিল। শতভাগ উপস্থিত থাকলে আরও ভালো করবে।", CommentCategory.ATTENDANCE, "উপস্থিতি উন্নতি", 88)
                    addOpt("gen_att_mod_2", "মাঝে মাঝে অনুপস্থিতির কারণে পাঠের ধারাবাহিকতা ব্যাহত হয়েছে। প্রতি কার্যদিবসে বিদ্যালয়ে আসা জরুরি।", CommentCategory.ATTENDANCE, "নিয়মিত থাকার পরামর্শ", 84)
                }
                else -> {
                    addOpt("gen_att_low_1", "উপস্থিতি মাত্র $attBn% যা অত্যন্ত কম। পাঠের ধারাবাহিকতা রক্ষায় অভিভাবকের বিশেষ উদ্যোগে নিয়মিত উপস্থিতি কাম্য।", CommentCategory.ATTENDANCE, "অনিয়মিত উপস্থিতি", 93)
                    addOpt("gen_att_low_2", "অতিরিক্ত অনুপস্থিতির (${bn(absent.toString())} দিন) কারণে শিখন ঘাটতি তৈরি হয়েছে। নিয়মিত বিদ্যালয়ে উপস্থিত থাকা আবশ্যক।", CommentCategory.ATTENDANCE, "উপস্থিতি তাগিদ", 90)
                }
            }
        }

        // ------------------------------------------------------------------
        // 5. TRAITS & CHARACTER (গুণাবলি: সক্রিয়তা, শৃঙ্খলা, আচরণ)
        // ------------------------------------------------------------------
        if (a.hasTraits) {
            val weakNames = listOfNotNull(
                if (a.actRating == "গ") "সক্রিয়তা" else null,
                if (a.discRating == "গ") "শৃঙ্খলা" else null,
                if (a.condRating == "গ") "আচরণ" else null
            )
            val goodNames = listOfNotNull(
                if (a.actRating == "ক") "সক্রিয়তা" else null,
                if (a.discRating == "ক") "শৃঙ্খলা" else null,
                if (a.condRating == "ক") "আচরণ" else null
            )
            val moderateNames = listOfNotNull(
                if (a.actRating == "খ") "সক্রিয়তা" else null,
                if (a.discRating == "খ") "শৃঙ্খলা" else null,
                if (a.condRating == "খ") "আচরণ" else null
            )

            when {
                weakNames.isNotEmpty() -> {
                    val weakLbl = weakNames.joinToString(" ও ")
                    addOpt("gen_tr_weak_1", "শ্রেণিকক্ষে $weakLbl-এ বিশেষ উন্নতি প্রয়োজন। শিক্ষকের নির্দেশনা অনুসরণ ও ভালো অভ্যাসের চর্চা বাড়াতে হবে।", CommentCategory.TRAITS, "গুণাবলি নির্দেশনা", 91)
                    if (goodNames.isNotEmpty()) {
                        addOpt("gen_tr_mixed", "${goodNames.joinToString(" ও ")} প্রশংসনীয় হলেও $weakLbl-এ আরেকটু যত্নবান হলে সার্বিকভাবে আরও উজ্জ্বল করবে।", CommentCategory.TRAITS, "মিশ্র মূল্যায়ন", 89)
                    }
                }
                goodNames.size == 3 -> {
                    addOpt("gen_tr_all_k_1", "সক্রিয়তা, শৃঙ্খলা ও আচরণে আদর্শ শিক্ষার্থী। সহপাঠীদের প্রতি সহযোগিতাপূর্ণ মনোভাব সবার জন্য অনুকরণীয়।", CommentCategory.TRAITS, "আদর্শ গুণাবলি", 91)
                    addOpt("gen_tr_all_k_2", "ভদ্র, বিনয়ী ও দলগত কাজে অত্যন্ত প্রাণবন্ত। পরিচ্ছন্নতা ও নিয়ম-শৃঙ্খলার চর্চা অত্যন্ত প্রশংসনীয়।", CommentCategory.TRAITS, "ব্যবহারিক উৎকর্ষ", 86)
                }
                goodNames.isNotEmpty() -> {
                    val gLbl = goodNames.joinToString(" ও ")
                    val modHint = if (moderateNames.isNotEmpty()) " এবং ${moderateNames.joinToString(" ও ")}-এ আরও সক্রিয়তা" else ""
                    addOpt("gen_tr_some_k", "$gLbl গুণে প্রশংসনীয়$modHint কাম্য। শ্রেণিকাজে স্বতঃস্ফূর্ত অংশগ্রহণ আরও বাড়াতে উৎসাহিত করা হলো।", CommentCategory.TRAITS, "গুণাবলি প্রশংসা", 88)
                }
                else -> {
                    addOpt("gen_tr_sat_1", "গুণাবলিতে সন্তোষজনক; ভদ্র ও শান্ত স্বভাবের। দলগত আলোচনা ও সহপাঠক্রমিক কাজে আরও সক্রিয় হলে চমৎকার করবে।", CommentCategory.TRAITS, "সক্রিয়তা উৎসাহ", 85)
                }
            }
        }

        // ------------------------------------------------------------------
        // 6. ENCOURAGEMENT & GUARDIAN COLLABORATION (উৎসাহ ও পরামর্শ)
        // ------------------------------------------------------------------
        addOpt("gen_enc_1", "নিয়মিত হাতের লেখা সুন্দর করার চর্চা ও পাঠ্যবই মনোযোগ দিয়ে পড়লে ভবিষ্যতে আরও চমৎকার ফলাফল করবে।", CommentCategory.ENCOURAGEMENT, "গঠনমূলক পরামর্শ", 76)
        addOpt("gen_enc_2", "শেখার প্রতি আগ্রহ ও পরিশ্রম ধরে রাখলে সামনে আরও বড় সাফল্য অর্জন সম্ভব। উজ্জ্বল ভবিষ্যতের শুভকামনা।", CommentCategory.ENCOURAGEMENT, "আশাবাদী উৎসাহ", 77)
        addOpt("gen_enc_3", "প্রতিদিন নির্দিষ্ট সময়ে পড়াশোনা, বাড়ির কাজ সম্পন্ন ও পরিষ্কার-পরিচ্ছন্নতার চর্চা সার্বিক উন্নতিতে সহায়ক হবে।", CommentCategory.ENCOURAGEMENT, "অভ্যাস গঠন", 75)
        addOpt("gen_enc_4", "বিদ্যালয়ের পাঠ ঘরে নিয়মিত পুনরাবৃত্তি করলে আত্মবিশ্বাস বাড়বে এবং আগামী প্রান্তিকে আরও ভালো অর্জন করবে।", CommentCategory.ENCOURAGEMENT, "আত্মবিশ্বাস বৃদ্ধি", 78)
        addOpt("gen_enc_5", "মনোবল দৃঢ় রেখে নিয়মিত চর্চা চালিয়ে গেলে এবং অভিভাবকের সার্বক্ষণিক উৎসাহ পেলে অবশ্যই কাঙ্ক্ষিত মান অর্জন করবে।", CommentCategory.ENCOURAGEMENT, "অভিভাবক-সহযোগিতা", 79)

        return out.distinctBy { it.text }
    }

    private fun subjectStrengthPhrase(se: SubjectEval): String = when (se.sub.key) {
        "BAN" -> "বাংলায় পঠন, বানান ও বোধগম্যতায় চমৎকার দক্ষতা দেখিয়েছে"
        "ENG" -> "ইংরেজি পঠন ও শব্দভাণ্ডারে প্রশংসনীয় দক্ষতা অর্জন করেছে"
        "MATH" -> "গণিতের হিসাব ও সমস্যা সমাধানে প্রখর দক্ষতা দেখিয়েছে"
        "SCI", "SOC" -> "বিজ্ঞানের মূল ধারণা ও পর্যবেক্ষণে চমৎকার আগ্রহ দেখিয়েছে"
        "BGW" -> "বাংলাদেশ ও বিশ্বপরিচয়ের পাঠ্যজ্ঞান ও অনুধাবনে দক্ষতা দেখিয়েছে"
        "REL" -> "${se.shortName} বিষয়ে সুন্দর জ্ঞান ও নৈতিক বোধ প্রদর্শন করেছে"
        "PE" -> "শারীরিক শিক্ষা ও স্বাস্থ্যসচেতনতায় সক্রিয় অংশগ্রহণ দেখিয়েছে"
        "ART" -> "শিল্পকলায় সৃজনশীলতা ও নান্দনিক রুচির পরিচয় দিয়েছে"
        else -> "${se.shortName} বিষয়ে বিশেষ পারদর্শিতা দেখিয়েছে"
    }

    private fun subjectActionPhrase(se: SubjectEval): String = when (se.sub.key) {
        "BAN" -> "বাংলায় শুদ্ধ বানান, পঠন ও সুন্দর হাতের লেখার"
        "ENG" -> "ইংরেজি রিডিং, শব্দার্থ ও সহজ বাক্য গঠনের"
        "MATH" -> "গণিতের মৌলিক হিসাব, নামতা ও নিয়মিত অঙ্কের"
        "SCI", "SOC" -> "${se.ultraShortName} পাঠ্যবই বুঝে পড়া ও সংক্ষিপ্ত প্রশ্নের"
        "BGW" -> "বাংলাদেশ ও বিশ্বপরিচয়ের পাঠ্যবিষয় বুঝে পড়ার"
        "REL" -> "${se.shortName} পাঠ ও নৈতিক শিক্ষার"
        "PE" -> "স্বাস্থ্যবিধি ও শ্রেণি-কার্যক্রমে অংশগ্রহণের"
        "ART" -> "শিল্পকলা ও চারু-কারুর হাতেকলমে অনুশীলনের"
        else -> "${se.shortName} বিষয়ে নিয়মিত"
    }
}
