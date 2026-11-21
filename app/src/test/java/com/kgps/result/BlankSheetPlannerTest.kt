package com.kgps.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlankSheetPlannerTest {

    private fun sampleStudents(): List<Student> {
        val c1 = (1..12).map { r ->
            Student(id = "c1-$r", cls = 1, roll = r, name = "শিক্ষার্থী ১-$r")
        }
        val c3 = (1..25).map { r ->
            Student(id = "c3-$r", cls = 3, roll = r, name = "শিক্ষার্থী ৩-$r")
        }
        return (c3 + c1).shuffled()
    }

    @Test
    fun singleClassPaginationAndRollNameFilling() {
        val settings = Settings(school = "কাজীপাড়া সরকারি প্রাথমিক বিদ্যালয়")
        val cfg = BlankSheetConfig(
            selectedClass = 3,
            subjectKey = "SCI",
            term = 2,
            rowsPerPage = 10,
            autoFillRollAndName = true
        )

        val pages = BlankSheetPlanner.buildPages(
            students = sampleStudents(),
            settings = settings,
            config = cfg,
            allowedClassIds = listOf(1, 2, 3, 4, 5)
        )

        assertEquals(3, pages.size)
        assertEquals(listOf(10, 10, 5), pages.map { it.rows.size })
        assertEquals("তৃতীয় শ্রেণি", pages[0].classTitleBn)
        assertEquals("বিজ্ঞান", pages[0].subjectTitleBn)
        assertEquals("২য় প্রান্তিক", pages[0].termTitleBn)

        // Sorted by roll and formatted in Bengali numerals
        assertEquals("০১", pages[0].rows[0].rollText)
        assertEquals("শিক্ষার্থী ৩-1", pages[0].rows[0].nameText)
        assertEquals("২৫", pages[2].rows[4].rollText)
        assertEquals(1, pages[0].classPageIndex)
        assertEquals(3, pages[0].classTotalPages)
        assertEquals(3, pages[2].globalPageNumber)
        assertEquals(3, pages[2].totalPages)
    }

    @Test
    fun allClassesGeneratesSeparateClassHeadersAndSkipsEmptyClasses() {
        val settings = Settings(school = "টেস্ট বিদ্যালয়")
        val cfg = BlankSheetConfig(
            selectedClass = null, // সব শ্রেণি
            subjectKey = "BAN",
            term = 1,
            rowsPerPage = 15,
            extraBlankRowsPerClass = 2,
            fillRemainingPageRows = false
        )

        val pages = BlankSheetPlanner.buildPages(
            students = sampleStudents(),
            settings = settings,
            config = cfg,
            allowedClassIds = listOf(1, 2, 3, 4, 5)
        )

        // Class 1: 12 + 2 = 14 rows (1 page), Class 3: 25 + 2 = 27 rows (2 pages: 15, 12)
        assertEquals(3, pages.size)
        assertEquals(1, pages[0].cls)
        assertEquals("প্রথম শ্রেণি", pages[0].classTitleBn)
        assertEquals(14, pages[0].rows.size)
        assertEquals("", pages[0].rows[12].rollText)
        assertEquals("", pages[0].rows[12].nameText)

        assertEquals(3, pages[1].cls)
        assertEquals(3, pages[2].cls)
        assertEquals(listOf(15, 12), listOf(pages[1].rows.size, pages[2].rows.size))
    }

    @Test
    fun autoFillDisabledProducesBlankRollAndName() {
        val cfg = BlankSheetConfig(
            selectedClass = 1,
            subjectKey = null,
            customSubjectTitle = "হাতের লেখা",
            term = -1,
            customTermTitle = "মাসিক পরীক্ষা",
            rowsPerPage = 10,
            autoFillRollAndName = false,
            fillRemainingPageRows = true
        )

        val pages = BlankSheetPlanner.buildPages(
            students = sampleStudents(),
            settings = Settings(),
            config = cfg,
            allowedClassIds = listOf(1, 2, 3)
        )

        assertEquals(2, pages.size) // 12 padded to 20 -> 2 pages of 10
        assertEquals(10, pages[0].rows.size)
        assertEquals(10, pages[1].rows.size)
        assertTrue(pages.all { p -> p.rows.all { it.rollText.isEmpty() && it.nameText.isEmpty() } })
        assertEquals("হাতের লেখা", pages[0].subjectTitleBn)
        assertEquals("মাসিক পরীক্ষা", pages[0].termTitleBn)
    }

    @Test
    fun subjectMaxHintsAdaptPerClassTier() {
        assertEquals(50, BlankSheetCatalog.resolveMaxHint(1, "BAN", "WR"))
        assertEquals(70, BlankSheetCatalog.resolveMaxHint(3, "BAN", "WR"))
        assertEquals(50, BlankSheetCatalog.resolveMaxHint(1, "BAN", "CONT"))
        assertEquals(30, BlankSheetCatalog.resolveMaxHint(3, "SCI", "CONT"))
        assertEquals(100, BlankSheetCatalog.resolveMaxHint(3, "SCI", "TOTAL"))
    }
}
