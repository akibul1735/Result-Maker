package com.kgps.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataResetEngineTest {

    private val class1Student = SAMPLE_STUDENTS.first { it.cls == 1 }.copy(
        union = "কামারখালী",
        x = SAMPLE_STUDENTS.first { it.cls == 1 }.x + ("CF_ROLL_REG" to "12345")
    )
    private val class3Student = SAMPLE_STUDENTS.first { it.cls == 3 }
    private val roster = listOf(class1Student, class3Student)

    @Test
    fun resetClass1AllResultsPreservesClass3AndBasicInfo() {
        val req = DataResetRequest(
            category = DataResetCategory.RESULTS,
            selectedClasses = setOf(1),
            selectedTerms = setOf(1, 2, 3),
            allSubjects = true
        )
        val (updated, count) = DataResetEngine.applyReset(roster, req)
        assertEquals(1, count)

        val st1 = updated.first { it.id == class1Student.id }
        val st3 = updated.first { it.id == class3Student.id }

        assertTrue(st1.m.isEmpty())
        assertEquals(class1Student.name, st1.name)
        assertEquals(class1Student.guardian, st1.guardian)
        assertFalse(st1.x.isEmpty())
        assertEquals(class3Student.m, st3.m)
    }

    @Test
    fun resetSpecificSubjectAndTermOnlyDropsMatchingKeys() {
        val req = DataResetRequest(
            category = DataResetCategory.RESULTS,
            selectedClasses = setOf(1, 3),
            selectedTerms = setOf(2),
            allSubjects = false,
            selectedSubjectKeys = setOf("MATH")
        )
        val impact = DataResetEngine.computeImpact(roster, req)
        assertEquals(2, impact.affectedStudentCount)

        val (updated, count) = DataResetEngine.applyReset(roster, req)
        assertEquals(2, count)

        val st1 = updated.first { it.id == class1Student.id }
        assertFalse(st1.m.keys.any { it.startsWith("MATH_2_") })
        assertTrue(st1.m.keys.any { it.startsWith("MATH_1_") })
        assertTrue(st1.m.keys.any { it.startsWith("BAN_2_") })
    }

    @Test
    fun resetAttendanceTraitsAndRemarksOnlyClearsSelectedParts() {
        val req = DataResetRequest(
            category = DataResetCategory.ATTENDANCE_TRAITS,
            selectedClasses = setOf(1),
            selectedTerms = setOf(1, 3),
            resetAttendance = true,
            resetTraits = false,
            resetRemarks = true
        )
        val (updated, count) = DataResetEngine.applyReset(roster, req)
        assertEquals(1, count)

        val st1 = updated.first { it.id == class1Student.id }
        assertFalse(st1.x.containsKey("DAYS_1"))
        assertFalse(st1.x.containsKey("PRES_1"))
        assertFalse(st1.x.containsKey("REM_1"))
        assertFalse(st1.x.containsKey("DAYS_3"))
        assertFalse(st1.x.containsKey("REM_3"))
        // Traits in term 1 and attendance/remarks in term 2 remain intact
        assertTrue(st1.x.containsKey("ACT_1"))
        assertTrue(st1.x.containsKey("DAYS_2"))
        assertTrue(st1.x.containsKey("REM_2"))
    }

    @Test
    fun resetBasicInfoPreservesMarksAndKeepsNonEmptyReligionForFirestore() {
        val hinduStudent = class1Student.copy(religion = "হিন্দু")
        val req = DataResetRequest(
            category = DataResetCategory.BASIC_INFO,
            selectedClasses = setOf(1),
            resetGuardian = true,
            resetMobile = true,
            resetAddress = true,
            resetReligionToDefault = true,
            resetCustomFields = true
        )
        val (updated, count) = DataResetEngine.applyReset(
            students = listOf(hinduStudent, class3Student),
            request = req,
            customFieldKeys = setOf("CF_ROLL_REG")
        )
        assertEquals(1, count)

        val st1 = updated.first { it.id == hinduStudent.id }
        assertEquals("", st1.guardian)
        assertEquals("", st1.mobile)
        assertEquals("", st1.village)
        assertEquals("", st1.union)
        assertEquals("", st1.post)
        assertEquals(DEFAULT_UPAZILA, st1.upazila)
        assertEquals(DEFAULT_DISTRICT, st1.district)
        // Must remain non-empty per firestore.rules (religion.size() >= 1)
        assertEquals("ইসলাম", st1.religion)
        assertFalse(st1.x.containsKey("CF_ROLL_REG"))
        // Marks and name preserved
        assertEquals(hinduStudent.name, st1.name)
        assertEquals(hinduStudent.m, st1.m)
    }

    @Test
    fun deleteClassStudentsRemovesOnlySelectedClass() {
        val req = DataResetRequest(
            category = DataResetCategory.CLASS_STUDENTS,
            selectedClasses = setOf(1)
        )
        val (updated, deletedCount) = DataResetEngine.applyReset(roster, req)
        assertEquals(1, deletedCount)
        assertEquals(listOf(class3Student), updated)
    }
}
