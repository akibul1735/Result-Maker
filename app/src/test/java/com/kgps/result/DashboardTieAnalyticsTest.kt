package com.kgps.result

import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.ui.screens.buildDashboardSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardTieAnalyticsTest {

    private fun term1BanMathMarks(banCont: Double, banWr: Double): Map<String, Double> = mapOf(
        mk("BAN", 1, "CONT") to banCont,
        mk("BAN", 1, "WR") to banWr
    )

    @Test
    fun buildDashboardSummaryDetectsPerClassTotalTiesWithoutCrossClassCollision() {
        val perms = TeacherPermissions(allClasses = true, allSubjects = true)
        val students = listOf(
            // Class 3: Roll 1 and Roll 2 tie with 85 total; Roll 3 has 90
            Student(id = "c3_r1", cls = 3, roll = 1, name = "রাফি", m = term1BanMathMarks(25.0, 60.0)),
            Student(id = "c3_r2", cls = 3, roll = 2, name = "সাদিয়া", m = term1BanMathMarks(27.0, 58.0)),
            Student(id = "c3_r3", cls = 3, roll = 3, name = "তানভীর", m = term1BanMathMarks(28.0, 62.0)),
            // Class 4: Roll 1 also has 85 total (must NOT tie with Class 3); Roll 2 has 70
            Student(id = "c4_r1", cls = 4, roll = 1, name = "মিতু", m = term1BanMathMarks(25.0, 60.0)),
            Student(id = "c4_r2", cls = 4, roll = 2, name = "নাফিস", m = term1BanMathMarks(20.0, 50.0))
        )

        val summaryAll = buildDashboardSummary(
            allStudents = students,
            visibleClasses = listOf(3, 4),
            classFilter = null,
            termFilter = 1,
            permissions = perms
        )
        assertEquals(1, summaryAll.tieGroups.size)
        val group = summaryAll.tieGroups.single()
        assertEquals(3, group.cls)
        assertEquals(85.0, group.obtained, 0.01)
        assertEquals(listOf("c3_r1", "c3_r2"), group.students.map { it.student.id })

        // Filtering to Class 4 should show zero ties
        val summaryCls4 = buildDashboardSummary(
            allStudents = students,
            visibleClasses = listOf(3, 4),
            classFilter = 4,
            termFilter = 1,
            permissions = perms
        )
        assertTrue(summaryCls4.tieGroups.isEmpty())
    }
}
