package com.kgps.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TeacherRemarkEngineTest {

    @Test
    fun `empty term does not auto prefill but still offers ranked alternatives`() {
        val emptyStudent = createEmptyStudent(cls = 3, roll = 5).copy(name = "সাদিয়া")
        val profile = TeacherRemarkEngine.analyzeProfile(emptyStudent, term = 1)
        assertFalse(profile.hasAnyData)
        assertNull(TeacherRemarkEngine.autoPrefillIfApplicable(emptyStudent, term = 1))

        val alts = TeacherRemarkEngine.getRankedAlternatives(emptyStudent, term = 1)
        assertTrue("Should offer plenty of alternative templates", alts.size >= 25)
    }

    @Test
    fun `high performer with regular attendance and top traits gets laudatory prefill and subject highlights`() {
        val student = Student(
            id = "s1",
            cls = 3,
            roll = 1,
            name = "রাফি",
            m = mapOf(
                "BAN_1_TB" to 9.0, "BAN_1_ACT" to 5.0, "BAN_1_LANG" to 5.0, "BAN_1_CT" to 9.0, "BAN_1_WR" to 62.0,
                "ENG_1_TB" to 9.0, "ENG_1_ACT" to 5.0, "ENG_1_LANG" to 4.0, "ENG_1_CT" to 9.0, "ENG_1_WR" to 60.0,
                "MATH_1_TB" to 10.0, "MATH_1_ACT" to 5.0, "MATH_1_LANG" to 5.0, "MATH_1_CT" to 10.0, "MATH_1_WR" to 66.0
            ),
            x = mapOf(
                "DAYS_1" to "60",
                "PRES_1" to "58",
                "ACT_1" to "ক",
                "DISC_1" to "ক",
                "COND_1" to "ক"
            )
        )

        val profile = TeacherRemarkEngine.analyzeProfile(student, term = 1)
        assertEquals(ResultBand.EXCELLENT, profile.resultBand)
        assertEquals(AttendanceBand.REGULAR, profile.attendanceBand)
        assertEquals(TraitsBand.EXEMPLARY, profile.traitsBand)
        assertTrue(profile.strongSubjects.isNotEmpty())

        val prefill = TeacherRemarkEngine.autoPrefillIfApplicable(student, term = 1)
        assertNotNull(prefill)
        assertTrue(prefill!!.length <= 200)

        val alts = TeacherRemarkEngine.getRankedAlternatives(student, term = 1)
        assertTrue(alts.first().isTopPick)
        assertTrue(alts.first().matchScore >= 85)
        assertTrue("Multiple nearby high-scoring alternatives should exist", alts.count { it.matchScore >= 75 } >= 6)
    }

    @Test
    fun `weak attendance with good marks highlights attendance improvement`() {
        val student = Student(
            id = "s2",
            cls = 4,
            roll = 2,
            name = "তানহা",
            m = mapOf(
                "BAN_1_TB" to 9.0, "BAN_1_ACT" to 5.0, "BAN_1_LANG" to 4.0, "BAN_1_CT" to 9.0, "BAN_1_WR" to 60.0
            ),
            x = mapOf(
                "DAYS_1" to "60",
                "PRES_1" to "30",
                "ACT_1" to "ক",
                "DISC_1" to "ক",
                "COND_1" to "ক"
            )
        )

        val profile = TeacherRemarkEngine.analyzeProfile(student, term = 1)
        assertEquals(ResultBand.EXCELLENT, profile.resultBand)
        assertEquals(AttendanceBand.LOW, profile.attendanceBand)

        val top = TeacherRemarkEngine.bestRemark(student, term = 1)
        assertTrue(
            "Top remark should mention attendance when attendance is low: ${top.text}",
            top.text.contains("উপস্থিত")
        )
    }

    @Test
    fun `category filtering returns focused alternatives`() {
        val student = SAMPLE_STUDENTS.first()
        val combined = TeacherRemarkEngine.getRankedAlternatives(student, term = 3, RemarkCategory.COMBINED)
        val attendance = TeacherRemarkEngine.getRankedAlternatives(student, term = 3, RemarkCategory.ATTENDANCE)
        val traits = TeacherRemarkEngine.getRankedAlternatives(student, term = 3, RemarkCategory.TRAITS)

        assertTrue(combined.isNotEmpty() && combined.all { it.category == RemarkCategory.COMBINED })
        assertTrue(attendance.isNotEmpty() && attendance.all { it.category == RemarkCategory.ATTENDANCE })
        assertTrue(traits.isNotEmpty() && traits.all { it.category == RemarkCategory.TRAITS })
    }
}
