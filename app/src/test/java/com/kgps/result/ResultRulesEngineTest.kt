package com.kgps.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultRulesEngineTest {

    private fun makeClass3Student(
        id: String,
        roll: Int,
        name: String = "শিক্ষার্থী $roll",
        term1Marks: Map<String, Double> = emptyMap(),
        term2Marks: Map<String, Double> = emptyMap(),
        term3Marks: Map<String, Double> = emptyMap(),
        x: Map<String, String> = emptyMap()
    ): Student = Student(
        id = id,
        cls = 3,
        roll = roll,
        name = name,
        m = term1Marks + term2Marks + term3Marks,
        x = x
    )

    private fun fullTermMarks(term: Int, perSubjectTotalOf100: Double, per50Total: Double = perSubjectTotalOf100 / 2.0): Map<String, Double> {
        // Class 3: BAN(100:30+70), ENG(100:30+70), MATH(100:30+70), BGW(100:30+70), SCI(100:30+70), REL(100:30+70), PE(50:CONT), ART(50:CONT)
        val wr100 = (perSubjectTotalOf100 * 0.7).coerceIn(0.0, 70.0)
        val cont100 = (perSubjectTotalOf100 - wr100).coerceIn(0.0, 30.0)
        val out = linkedMapOf<String, Double>()
        listOf("BAN", "ENG", "MATH", "BGW", "SCI", "REL").forEach { sub ->
            out[mk(sub, term, "CONT")] = cont100
            out[mk(sub, term, "WR")] = wr100
        }
        out[mk("PE", term, "CONT")] = per50Total.coerceIn(0.0, 50.0)
        out[mk("ART", term, "CONT")] = per50Total.coerceIn(0.0, 50.0)
        return out
    }

    @Test
    fun rule1_attendanceRuleIsOffByDefaultAndConfigurableWhenEnabled() {
        val defaultRules = ResultEvaluationRules()
        assertFalse(defaultRules.attendanceRuleEnabled)
        assertEquals(85.0, defaultRules.minAttendancePct, 0.001)

        val lowAttStudent = makeClass3Student(
            id = "s1",
            roll = 1,
            term1Marks = fullTermMarks(1, 60.0),
            x = mapOf("DAYS_1" to "100", "PRES_1" to "70") // 70%
        )
        val evalDefault = ResultRulesEngine.evaluateStudent(lowAttStudent, defaultRules, listOf(1))
        assertTrue(evalDefault.isPromoted)

        val strictRules = defaultRules.copy(
            attendanceRuleEnabled = true,
            minAttendancePct = 80.0,
            promotionCheckAttendance = true
        )
        val evalStrict = ResultRulesEngine.evaluateStudent(lowAttStudent, strictRules, listOf(1))
        assertFalse(evalStrict.isPromoted)
        assertFalse(evalStrict.attendancePassed)

        val customRelaxedRules = strictRules.copy(minAttendancePct = 70.0)
        val evalRelaxed = ResultRulesEngine.evaluateStudent(lowAttStudent, customRelaxedRules, listOf(1))
        assertTrue(evalRelaxed.isPromoted)
    }

    @Test
    fun rule2_subjectPassAndFailIndicatorStylesWorkCorrectly() {
        val mathSub = subjectsFor(3, "ইসলাম").first { it.key == "MATH" }
        val student = makeClass3Student(
            id = "s1",
            roll = 1,
            term1Marks = fullTermMarks(1, 60.0).toMutableMap().apply {
                put(mk("MATH", 1, "CONT"), 10.0)
                put(mk("MATH", 1, "WR"), 29.0) // total 39 < 40
            }
        )
        val rules = ResultEvaluationRules(showFailIndicator = true, failIndicatorStyle = FailIndicatorStyle.CROSS)
        assertTrue(ResultRulesEngine.isSubjectTermFailed(student, mathSub, 1, rules))
        assertEquals("৩৯×", ResultRulesEngine.formatTermTotalCell("৩৯", failed = true, rules = rules))

        val hiddenRules = rules.copy(showFailIndicator = false)
        assertEquals("৩৯", ResultRulesEngine.formatTermTotalCell("৩৯", failed = true, rules = hiddenRules))

        val redOnlyRules = rules.copy(failIndicatorStyle = FailIndicatorStyle.RED_ONLY)
        assertEquals("৩৯", ResultRulesEngine.formatTermTotalCell("৩৯", failed = true, rules = redOnlyRules))
    }

    @Test
    fun rule3_specialNeedsStudent33PercentPassAndCanRankAheadOfRegular() {
        val rules = ResultEvaluationRules(
            specialNeedsRuleEnabled = true,
            meritTierAllSubPassFirst = true,
            meritTierAvgPassEnabled = true
        )
        // Special needs student gets 36% across all subjects -> passes with 33% rule (Tier 0)
        val specialStudent = makeClass3Student(
            id = "sp1",
            roll = 5,
            term1Marks = fullTermMarks(1, 36.0),
            x = mapOf(X_KEY_SPECIAL_NEEDS to "1")
        )
        // Regular student gets 50% overall avg, but failed MATH (35) -> Tier 1
        val regPartialFail = makeClass3Student(
            id = "reg1",
            roll = 1,
            term1Marks = fullTermMarks(1, 55.0).toMutableMap().apply {
                put(mk("MATH", 1, "CONT"), 10.0)
                put(mk("MATH", 1, "WR"), 25.0) // 35 < 40
            }
        )
        val ranked = ResultRulesEngine.rankClassStudents(listOf(regPartialFail, specialStudent), rules)
        assertEquals("sp1", ranked[0].student.id)
        assertEquals(1, ranked[0].rank)
        assertTrue(ranked[0].eval.allSubjectsPassed)
        assertEquals("reg1", ranked[1].student.id)
        assertEquals(2, ranked[1].rank)

        // When specialNeedsRuleEnabled = false, special student has <40% everywhere -> Tier 2 (behind regPartialFail Tier 1)
        val disabledSpecial = ResultRulesEngine.rankClassStudents(
            listOf(regPartialFail, specialStudent),
            rules.copy(specialNeedsRuleEnabled = false)
        )
        assertEquals("reg1", disabledSpecial[0].student.id)
        assertEquals("sp1", disabledSpecial[1].student.id)
    }

    @Test
    fun rule5_meritTiersRespectBothTogglesAccurately() {
        val allPass = makeClass3Student("all_pass", 3, term1Marks = fullTermMarks(1, 45.0)) // avg 45%, all pass
        val avgPassOnly = makeClass3Student(
            id = "avg_only",
            roll = 2,
            term1Marks = fullTermMarks(1, 80.0).toMutableMap().apply {
                put(mk("MATH", 1, "CONT"), 10.0)
                put(mk("MATH", 1, "WR"), 20.0) // 30 < 40, but overall avg > 40%
            }
        )
        val failAll = makeClass3Student("fail_all", 1, term1Marks = fullTermMarks(1, 35.0)) // avg 35%

        // Both ON: allPass (Tier 0) -> avgPassOnly (Tier 1) -> failAll (Tier 2)
        val rulesBoth = ResultEvaluationRules(meritTierAllSubPassFirst = true, meritTierAvgPassEnabled = true)
        val rBoth = ResultRulesEngine.rankClassStudents(listOf(failAll, avgPassOnly, allPass), rulesBoth)
        assertEquals(listOf("all_pass", "avg_only", "fail_all"), rBoth.map { it.student.id })

        // Only Avg ON: avgPassOnly (higher total, Tier 0) -> allPass (lower total, Tier 0) -> failAll (Tier 1)
        val rulesAvgOnly = ResultEvaluationRules(meritTierAllSubPassFirst = false, meritTierAvgPassEnabled = true)
        val rAvg = ResultRulesEngine.rankClassStudents(listOf(failAll, allPass, avgPassOnly), rulesAvgOnly)
        assertEquals(listOf("avg_only", "all_pass", "fail_all"), rAvg.map { it.student.id })

        // Both OFF: pure score descending regardless of subject fail
        val rulesPure = ResultEvaluationRules(meritTierAllSubPassFirst = false, meritTierAvgPassEnabled = false)
        val rPure = ResultRulesEngine.rankClassStudents(listOf(failAll, allPass, avgPassOnly), rulesPure)
        assertEquals(listOf("avg_only", "all_pass", "fail_all"), rPure.map { it.student.id })
    }

    @Test
    fun rule6_dynamicTermAveragingDividesByEnteredTermsOnly() {
        val rules = ResultEvaluationRules(dynamicTermAveraging = true)
        val oneTermStudent = makeClass3Student("s1", 1, term1Marks = fullTermMarks(1, 80.0))
        val active1 = ResultRulesEngine.activeTermsForClass(listOf(oneTermStudent), rules.dynamicTermAveraging)
        assertEquals(listOf(1), active1)
        val eval1 = ResultRulesEngine.evaluateStudent(oneTermStudent, rules, active1)
        assertEquals(80.0, eval1.pct, 0.01)

        val twoTermStudent = makeClass3Student(
            "s1",
            1,
            term1Marks = fullTermMarks(1, 80.0),
            term2Marks = fullTermMarks(2, 60.0)
        )
        val active2 = ResultRulesEngine.activeTermsForClass(listOf(twoTermStudent), rules.dynamicTermAveraging)
        assertEquals(listOf(1, 2), active2)
        val eval2 = ResultRulesEngine.evaluateStudent(twoTermStudent, rules, active2)
        assertEquals(70.0, eval2.pct, 0.01)

        val staticRules = rules.copy(dynamicTermAveraging = false)
        val evalStatic = ResultRulesEngine.evaluateStudent(twoTermStudent, staticRules, listOf(1, 2, 3))
        assertEquals((80.0 + 60.0) / 3.0, evalStatic.pct, 0.01)
    }

    @Test
    fun rule7_absentVsZeroDistinctionAndCsvRoundTrip() {
        val mathSub = subjectsFor(3, "ইসলাম").first { it.key == "MATH" }
        val banSub = subjectsFor(3, "ইসলাম").first { it.key == "BAN" }

        // Student scored numeric 0 in BAN WR, Absent in MATH WR
        val base = makeClass3Student(
            id = "s_abs",
            roll = 4,
            term1Marks = mapOf(mk("BAN", 1, "CONT") to 20.0, mk("BAN", 1, "WR") to 0.0, mk("MATH", 1, "CONT") to 20.0),
            x = mapOf(absKey("MATH", 1, "WR") to "1")
        )
        assertTrue(base.hasTerm(1))
        assertFalse(base.isSubTermAbsent(banSub, 1))
        assertTrue(base.isSubTermAbsent(mathSub, 1))
        assertTrue(ResultRulesEngine.isSubjectTermFailed(base, banSub, 1, ResultEvaluationRules()))
        assertTrue(ResultRulesEngine.isSubjectTermFailed(base, mathSub, 1, ResultEvaluationRules()))

        val (headers, rows) = Repo.toCsvTable(
            list = listOf(base),
            customFields = emptyList(),
            style = CsvHeaderStyle.SYSTEM_KEYS,
            targetCls = 3
        )
        val mathWrIdx = headers.indexOf("MATH_1_WR")
        val banWrIdx = headers.indexOf("BAN_1_WR")
        assertTrue(mathWrIdx >= 0 && banWrIdx >= 0)
        assertEquals("A", rows[0][mathWrIdx])
        assertEquals("0", rows[0][banWrIdx])

        // Re-import via Repo.applyMappedCsvImport
        val csvText = Repo.toCsv(listOf(base), emptyList(), CsvHeaderStyle.SYSTEM_KEYS, targetCls = 3)
        val parsed = Repo.parseCsv(csvText)
        val cols = Repo.detectCsvColumns(parsed, emptyList())
        val imported = Repo.applyMappedCsvImport(
            rows = parsed,
            columns = cols,
            existingStudents = emptyList(),
            existingSettings = Settings()
        )
        val reimported = imported.students.single()
        assertTrue(reimported.isCompAbsent("MATH", 1, "WR"))
        assertFalse(reimported.isCompAbsent("BAN", 1, "WR"))
        assertEquals(0.0, reimported.m["BAN_1_WR"]!!, 0.001)
    }

    @Test
    fun rule8_jointMeritAndTieBreakersWorkInExactPriority() {
        val sRoll5 = makeClass3Student(
            id = "r5",
            roll = 5,
            term1Marks = fullTermMarks(1, 75.0),
            x = mapOf("DAYS_1" to "100", "PRES_1" to "95", "ACT_1" to "খ", "DISC_1" to "খ", "COND_1" to "খ")
        )
        val sRoll2 = makeClass3Student(
            id = "r2",
            roll = 2,
            term1Marks = fullTermMarks(1, 75.0),
            x = mapOf("DAYS_1" to "100", "PRES_1" to "90", "ACT_1" to "ক", "DISC_1" to "ক", "COND_1" to "ক")
        )

        // Default: joint OFF, all extra tie-breakers OFF, prevRoll ON -> Roll 2 ranks 1, Roll 5 ranks 2
        val rankedPrevRoll = ResultRulesEngine.rankClassStudents(
            listOf(sRoll5, sRoll2),
            ResultEvaluationRules(jointMeritEnabled = false, tieBreakPrevRoll = true)
        )
        assertEquals("r2", rankedPrevRoll[0].student.id)
        assertEquals(1, rankedPrevRoll[0].rank)
        assertFalse(rankedPrevRoll[0].isJointRank)

        // Attendance tie-breaker ON -> sRoll5 (95%) beats sRoll2 (90%)
        val rankedAtt = ResultRulesEngine.rankClassStudents(
            listOf(sRoll5, sRoll2),
            ResultEvaluationRules(jointMeritEnabled = false, tieBreakAttendance = true, tieBreakPrevRoll = true)
        )
        assertEquals("r5", rankedAtt[0].student.id)

        // Joint Merit ON with all tie-breakers OFF -> both share Joint Rank 1
        val rankedJoint = ResultRulesEngine.rankClassStudents(
            listOf(sRoll5, sRoll2),
            ResultEvaluationRules(
                jointMeritEnabled = true,
                tieBreakAttendance = false,
                tieBreakTraits = false,
                tieBreakPrevRoll = false
            )
        )
        assertEquals(1, rankedJoint[0].rank)
        assertEquals(1, rankedJoint[1].rank)
        assertTrue(rankedJoint[0].isJointRank)
        assertTrue(rankedJoint[1].isJointRank)

        // Traits tie-breaker ON (without attendance) -> 'ক' (3) beats 'খ' (2)
        val rankedTraits = ResultRulesEngine.rankClassStudents(
            listOf(sRoll5, sRoll2),
            ResultEvaluationRules(
                jointMeritEnabled = true,
                tieBreakAttendance = false,
                tieBreakTraits = true,
                tieBreakPrevRoll = false
            )
        )
        assertEquals("r2", rankedTraits[0].student.id)
        assertEquals(1, rankedTraits[0].rank)
        assertFalse(rankedTraits[0].isJointRank)
        assertEquals(2, rankedTraits[1].rank)
    }

    @Test
    fun settingsResultRulesRoundTripJsonAndFirestoreMap() {
        val rules = ResultEvaluationRules(
            attendanceRuleEnabled = true,
            minAttendancePct = 82.5,
            subjectPassRuleEnabled = true,
            regularPassPct = 40.0,
            showFailIndicator = true,
            failIndicatorStyle = FailIndicatorStyle.FAIL_BN,
            specialNeedsRuleEnabled = true,
            specialNeedsPassPct = 33.0,
            promotionCheckAvgPass = true,
            promotionCheckAttendance = true,
            promotionCheckAllSubjects = true,
            meritTierAllSubPassFirst = false,
            meritTierAvgPassEnabled = true,
            dynamicTermAveraging = true,
            jointMeritEnabled = true,
            tieBreakAttendance = true,
            tieBreakTraits = true,
            tieBreakPrevRoll = false,
            ignoredGuidelineIds = setOf("GOVT_3_1_ATTENDANCE", "GOVT_4_2_PROGRESS")
        )
        val fromMap = ResultEvaluationRules.fromMap(rules.toFirestoreMap())
        assertEquals(rules, fromMap)

        val settings = Settings(school = "টেস্ট স্কুল", resultRules = rules)
        val json = Repo.toJson(emptyList(), settings)
        val restored = Repo.fromBackupJson(json)
        assertEquals(rules, restored.settings.resultRules)
    }
}
