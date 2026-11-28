package com.kgps.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabulationPlannerTest {

    private fun makeSampleStudent(
        id: String,
        cls: Int,
        roll: Int,
        name: String,
        banCont: Double,
        banWr: Double,
        engCont: Double,
        engWr: Double,
        mathCont: Double,
        mathWr: Double
    ): Student {
        val m = mutableMapOf(
            mk("BAN", 1, "CONT") to banCont,
            mk("BAN", 1, "WR") to banWr,
            mk("ENG", 1, "CONT") to engCont,
            mk("ENG", 1, "WR") to engWr,
            mk("MATH", 1, "CONT") to mathCont,
            mk("MATH", 1, "WR") to mathWr
        )
        if (cls >= 3) {
            m[mk("BGW", 1, "CONT")] = 25.0
            m[mk("BGW", 1, "WR")] = 60.0
            m[mk("SCI", 1, "CONT")] = 24.0
            m[mk("SCI", 1, "WR")] = 58.0
            m[mk("REL", 1, "CONT")] = 26.0
            m[mk("REL", 1, "WR")] = 62.0
            m[mk("PE", 1, "CONT")] = 42.0
            m[mk("ART", 1, "CONT")] = 40.0
        } else {
            m[mk("REL", 1, "CONT")] = 20.0
            m[mk("PE", 1, "CONT")] = 22.0
            m[mk("ART", 1, "CONT")] = 21.0
            m[mk("MUSIC", 1, "CONT")] = 20.0
        }
        return Student(
            id = id,
            cls = cls,
            roll = roll,
            name = name,
            religion = "ইসলাম",
            m = m
        )
    }

    @Test
    fun buildSubjectGroups_class5_skipsWrittenOnHalfSubjectsByDefault() {
        val settings = Settings()
        val cfgSkip = TabulationConfig(
            selectedClass = 5,
            termScope = TabulationTermScope.TERM_1,
            subjectDisplay = TabulationSubjectDisplay.CONT_WR_TOT,
            skipWrittenOnHalfSubjects = true
        )
        val groupsSkip = TabulationPlanner.buildSubjectGroups(5, settings, cfgSkip)
        assertEquals(8, groupsSkip.size)
        // 6 full subjects * 3 + 2 half subjects (PE, ART) * 1 = 20 sub-columns
        assertEquals(20, groupsSkip.sumOf { it.subCols.size })

        val cfgNoSkip = cfgSkip.copy(skipWrittenOnHalfSubjects = false)
        val groupsNoSkip = TabulationPlanner.buildSubjectGroups(5, settings, cfgNoSkip)
        // 6 full subjects * 3 + 2 half subjects * 2 (CONT, TOTAL) = 22 sub-columns
        assertEquals(22, groupsNoSkip.sumOf { it.subCols.size })
    }

    @Test
    fun buildSubjectGroups_class2_and_totalOnly_collapseProperly() {
        val settings = Settings()
        val cfgCls2 = TabulationConfig(
            selectedClass = 2,
            termScope = TabulationTermScope.TERM_1,
            subjectDisplay = TabulationSubjectDisplay.CONT_WR_TOT,
            skipWrittenOnHalfSubjects = true
        )
        val groupsCls2 = TabulationPlanner.buildSubjectGroups(2, settings, cfgCls2)
        // Class 2 has BAN/ENG/MATH (3*3=9) + REL/PE/ART/MUSIC (4*1=4) = 13
        assertEquals(7, groupsCls2.size)
        assertEquals(13, groupsCls2.sumOf { it.subCols.size })

        val cfgTotalOnly = cfgCls2.copy(subjectDisplay = TabulationSubjectDisplay.TOTAL_ONLY)
        val groupsTotalOnly = TabulationPlanner.buildSubjectGroups(2, settings, cfgTotalOnly)
        assertEquals(7, groupsTotalOnly.sumOf { it.subCols.size })
        assertTrue(groupsTotalOnly.all { it.subCols == listOf(TabulationSubColType.TOTAL) })

        val cfgFinalAvgOnly = TabulationConfig(
            selectedClass = 5,
            termScope = TabulationTermScope.FINAL_AVG,
            subjectDisplay = TabulationSubjectDisplay.CONT_WR_TOT,
            finalDisplay = TabulationFinalDisplay.AVG_ONLY
        )
        val groupsFinalAvgOnly = TabulationPlanner.buildSubjectGroups(5, settings, cfgFinalAvgOnly)
        assertEquals(8, groupsFinalAvgOnly.sumOf { it.subCols.size })
        assertTrue(groupsFinalAvgOnly.all { it.subCols == listOf(TabulationSubColType.TOTAL) })

        val cfgFinalWithTerms = cfgFinalAvgOnly.copy(finalDisplay = TabulationFinalDisplay.TERMS_AND_AVG)
        val groupsFinalWithTerms = TabulationPlanner.buildSubjectGroups(5, settings, cfgFinalWithTerms)
        assertEquals(32, groupsFinalWithTerms.sumOf { it.subCols.size })
        assertTrue(
            groupsFinalWithTerms.all {
                it.subCols == listOf(
                    TabulationSubColType.TERM_1,
                    TabulationSubColType.TERM_2,
                    TabulationSubColType.TERM_3,
                    TabulationSubColType.AVG
                )
            }
        )
    }

    @Test
    fun buildPages_finalModeWithTerms_populatesThreeTermsAndAverage() {
        val settings = Settings()
        val stu = Student(
            id = "fin1",
            cls = 5,
            roll = 1,
            name = "তানহা",
            religion = "ইসলাম",
            m = mapOf(
                mk("BAN", 1, "CONT") to 20.0,
                mk("BAN", 1, "WR") to 50.0, // total 70
                mk("BAN", 2, "CONT") to 25.0,
                mk("BAN", 2, "WR") to 55.0, // total 80
                mk("BAN", 3, "CONT") to 25.0,
                mk("BAN", 3, "WR") to 65.0  // total 90 -> avg = 80
            )
        )
        val pages = TabulationPlanner.buildPages(
            students = listOf(stu),
            settings = settings,
            config = TabulationConfig(
                selectedClass = 5,
                termScope = TabulationTermScope.FINAL_AVG,
                finalDisplay = TabulationFinalDisplay.TERMS_AND_AVG,
                numeralMode = TabulationNumeralMode.ENGLISH
            )
        )
        assertEquals(1, pages.size)
        val banCells = pages[0].rows[0].subjectCells[0]
        assertEquals(listOf("70", "80", "90", "80"), banCells.map { it.text })
        assertFalse(banCells[0].isBold)
        assertTrue(banCells[3].isBold)
    }

    @Test
    fun buildPages_supportsRollAndMeritSort_pagination_and_pageSizes() {
        val settings = Settings()
        val s1 = makeSampleStudent("s1", 5, 1, "আরিফ", 20.0, 50.0, 20.0, 45.0, 18.0, 40.0)
        val s2 = makeSampleStudent("s2", 5, 2, "সাদিয়া", 28.0, 68.0, 27.0, 65.0, 29.0, 66.0)
        val s3 = makeSampleStudent("s3", 5, 3, "মাহি", 25.0, 58.0, 24.0, 54.0, 25.0, 56.0)
        val extraStudents = (4..13).map { r ->
            makeSampleStudent("s$r", 5, r, "শিক্ষার্থী $r", 21.0, 51.0, 21.0, 48.0, 20.0, 45.0)
        }
        val allStudents = listOf(s1, s2, s3) + extraStudents

        val meritCfg = TabulationConfig(
            selectedClass = 5,
            termScope = TabulationTermScope.TERM_1,
            pageSize = TabulationPageSize.LEGAL_LANDSCAPE,
            sortOrder = TabulationSortOrder.MERIT_ASC,
            rowsPerPage = 12
        )
        val pages = TabulationPlanner.buildPages(allStudents, settings, meritCfg)
        assertEquals(2, pages.size)
        assertEquals(1008, pages.first().config.pageSize.widthPt)
        assertEquals(612, pages.first().config.pageSize.heightPt)
        assertEquals(12, pages[0].rows.size)
        assertEquals(1, pages[1].rows.size)
        // Highest scorer (roll 2, সাদিয়া) comes first in merit order, followed by মাহি
        assertEquals("সাদিয়া", pages[0].rows[0].student.name)
        assertEquals("মাহি", pages[0].rows[1].student.name)
        assertEquals("আরিফ", pages[1].rows[0].student.name)

        // Highest total matches top student's total
        assertEquals(pages[0].rows[0].totalText, pages[0].classStats.highestTotalText)
        assertFalse(pages[0].rows[0].gradeText.isBlank())

        // Roll sort restores roll 1 -> 2 -> 3
        val rollPages = TabulationPlanner.buildPages(
            listOf(s3, s1, s2),
            settings,
            meritCfg.copy(sortOrder = TabulationSortOrder.ROLL_ASC, rowsPerPage = 25, pageSize = TabulationPageSize.A4_LANDSCAPE)
        )
        assertEquals(1, rollPages.size)
        assertEquals(listOf(1, 2, 3), rollPages[0].rows.map { it.student.roll })
        assertEquals(842, rollPages[0].config.pageSize.widthPt)
    }

    @Test
    fun buildPages_marksAbsentAsA() {
        val settings = Settings()
        val absentStudent = Student(
            id = "abs1",
            cls = 5,
            roll = 4,
            name = "রাফি",
            m = mapOf(
                mk("ENG", 1, "CONT") to 20.0,
                mk("ENG", 1, "WR") to 50.0
            ),
            x = mapOf(
                absKey("BAN", 1, "CONT") to "1",
                absKey("BAN", 1, "WR") to "1"
            )
        )
        val pages = TabulationPlanner.buildPages(
            listOf(absentStudent),
            settings,
            TabulationConfig(selectedClass = 5, termScope = TabulationTermScope.TERM_1)
        )
        assertEquals(1, pages.size)
        val banCells = pages[0].rows[0].subjectCells[0]
        assertTrue(banCells.all { it.text == "A" && it.isAbsent })
    }
}
