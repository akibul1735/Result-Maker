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
    fun conciseBengaliColumnHeadersAreUsed() {
        val expected = listOf("রোল", "নাম", "পাঠ্যবই", "কাজ", "ভাষা", "অভীক্ষা", "ধারাবাহিক", "লিখিত", "মোট")
        val actual = BlankSheetCatalog.presetColumns(BlankSheetPreset.FULL_MARKS).map { it.titleBn }
        assertEquals(expected, actual)
    }

    @Test
    fun singleClassPaginationAndBengaliOrEnglishNumeralsWork() {
        val settings = Settings(school = "কাজীপাড়া সরকারি প্রাথমিক বিদ্যালয়")
        val bnCfg = BlankSheetConfig(
            selectedClass = 3,
            subjectKey = "SCI",
            term = 2,
            columns = BlankSheetCatalog.presetColumns(BlankSheetPreset.COMPACT_MARKS),
            rowsPerPage = 10,
            numeralStyle = BlankNumeralStyle.BENGALI,
            autoFillRollAndName = true
        )
        val bnPages = BlankSheetPlanner.buildPages(
            students = sampleStudents(),
            settings = settings,
            config = bnCfg,
            allowedClassIds = listOf(1, 2, 3, 4, 5)
        )

        assertEquals(3, bnPages.size)
        assertEquals(listOf(10, 10, 5), bnPages.map { it.rows.size })
        assertEquals("৩য়", bnPages[0].classTitleBn)
        assertEquals("বিজ্ঞান", bnPages[0].subjectTitleBn)
        assertEquals("২য় প্রান্তিক", bnPages[0].termTitleBn)
        assertEquals("০১", bnPages[0].rows[0].rollText)
        assertEquals("২৫", bnPages[2].rows[4].rollText)

        // Same config with English numerals: roll/count switch to English while class & term stay Bengali
        val enPages = BlankSheetPlanner.buildPages(
            students = sampleStudents(),
            settings = settings,
            config = bnCfg.copy(numeralStyle = BlankNumeralStyle.ENGLISH),
            allowedClassIds = listOf(1, 2, 3, 4, 5)
        )
        assertEquals("৩য়", enPages[0].classTitleBn)
        assertEquals("২য় প্রান্তিক", enPages[0].termTitleBn)
        assertEquals("01", enPages[0].rows[0].rollText)
        assertEquals("25", enPages[2].rows[4].rollText)
    }

    @Test
    fun singleClassAllSubjectsGeneratesEverySubjectInOrder() {
        val cfg = BlankSheetConfig(
            selectedClass = 3,
            subjectKey = BlankSheetCatalog.ALL_SUBJECTS_KEY,
            term = 1,
            rowsPerPage = 15
        )
        val pages = BlankSheetPlanner.buildPages(
            students = sampleStudents(),
            settings = Settings(),
            config = cfg,
            allowedClassIds = listOf(1, 2, 3, 4, 5)
        )
        // Class 3 has 8 subjects, 25 students @ 15/page = 2 pages per subject -> 16 pages total
        assertEquals(16, pages.size)
        val distinctSubjects = pages.map { it.subjectTitleBn }.distinct()
        assertEquals(8, distinctSubjects.size)
        assertTrue(distinctSubjects.contains("বাংলা"))
        assertTrue(distinctSubjects.contains("বিজ্ঞান"))
        assertTrue(distinctSubjects.contains("বাংলাদেশ ও বিশ্বপরিচয়"))
        assertEquals(1, pages[0].classPageIndex)
        assertEquals(2, pages[0].classTotalPages)
        assertEquals(1, pages[2].classPageIndex)
        assertEquals("ইংরেজি", pages[2].subjectTitleBn)
    }

    @Test
    fun allClassesAllSubjectsGeneratesFullBatchAcrossClasses() {
        val cfg = BlankSheetConfig(
            selectedClass = null, // সব শ্রেণি
            subjectKey = BlankSheetCatalog.ALL_SUBJECTS_KEY, // সব বিষয়
            term = 1,
            rowsPerPage = 15,
            extraBlankRowsPerClass = 0
        )

        val pages = BlankSheetPlanner.buildPages(
            students = sampleStudents(),
            settings = Settings(),
            config = cfg,
            allowedClassIds = listOf(1, 2, 3, 4, 5)
        )

        // Class 1: 12 students -> 1 page * 7 subjects = 7 pages
        // Class 3: 25 students -> 2 pages * 8 subjects = 16 pages
        // Total = 23 pages; Class 2, 4, 5 empty so skipped
        assertEquals(23, pages.size)
        assertEquals(7, pages.count { it.cls == 1 })
        assertEquals(16, pages.count { it.cls == 3 })
        assertEquals("১ম", pages[0].classTitleBn)
        assertEquals("৩য়", pages[7].classTitleBn)
        assertEquals(23, pages.last().globalPageNumber)
        assertEquals(23, pages.last().totalPages)
    }

    @Test
    fun autoFillDisabledProducesBlankRollAndName() {
        val cfg = BlankSheetConfig(
            selectedClass = 1,
            subjectKey = null,
            customSubjectTitle = "হাতের লেখা",
            term = -1,
            customTermTitle = "মাসিক পরীক্ষা",
            columns = BlankSheetCatalog.presetColumns(BlankSheetPreset.COMPACT_MARKS),
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

        assertEquals(2, pages.size)
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

    @Test
    fun defaultSettingsUsePortraitEnglishNumeralsAndThreeTermTemplateFirst() {
        val defaultCfg = BlankSheetConfig()
        assertEquals(BlankSheetOrientation.PORTRAIT, defaultCfg.orientation)
        assertEquals(BlankNumeralStyle.ENGLISH, defaultCfg.numeralStyle)
        assertEquals(BlankSheetPreset.THREE_TERM_MARKS, BlankSheetPreset.entries.first())
        assertEquals(
            BlankSheetCatalog.presetColumns(BlankSheetPreset.THREE_TERM_MARKS).map { it.id },
            defaultCfg.columns.map { it.id }
        )

        val pages = BlankSheetPlanner.buildPages(
            students = listOf(Student(id = "s1", cls = 3, roll = 1, name = "রাফি")),
            settings = Settings(),
            config = defaultCfg.copy(selectedClass = 3),
            allowedClassIds = listOf(3)
        )
        assertEquals(1, pages.size)
        assertEquals("01", pages.first().rows.first().rollText)
        assertEquals("৩ প্রান্তিক", pages.first().termTitleBn)
        assertEquals(1, pages.first().studentCountOnPage)
    }

    @Test
    fun twentyPerPageStrictlyCapsPageAtTwentyStudentsEvenWithTwentyTwoStudentsOrExtraRows() {
        val twentyTwoStudents = (1..22).map { r ->
            Student(id = "s-$r", cls = 3, roll = r, name = "শিক্ষার্থী $r")
        }
        val cfg = BlankSheetConfig(
            selectedClass = 3,
            subjectKey = "BAN",
            term = 1,
            rowsPerPage = 20,
            extraBlankRowsPerClass = 2
        )
        val pages = BlankSheetPlanner.buildPages(
            students = twentyTwoStudents,
            settings = Settings(),
            config = cfg,
            allowedClassIds = listOf(3)
        )

        // 22 students + 2 extra blank = 24 rows -> Page 1: 20 students, Page 2: 2 students + 2 blank = 4 rows
        assertEquals(2, pages.size)
        assertEquals(20, pages[0].rows.size)
        assertEquals(20, pages[0].studentCountOnPage)
        assertEquals("01", pages[0].rows.first().rollText)
        assertEquals("20", pages[0].rows.last().rollText)

        assertEquals(4, pages[1].rows.size)
        assertEquals(2, pages[1].studentCountOnPage)
        assertEquals("21", pages[1].rows[0].rollText)
        assertEquals("22", pages[1].rows[1].rollText)
        assertEquals("", pages[1].rows[2].rollText)
    }
}
