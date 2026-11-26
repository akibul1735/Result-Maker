package com.kgps.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdmitCardPlannerTest {

    private fun student(cls: Int, roll: Int, name: String): Student =
        Student(id = "st_${cls}_$roll", cls = cls, roll = roll, name = name)

    @Test
    fun defaultRoutineForClass_ordersWrittenSubjectsProperly() {
        val settings = Settings()
        val class5Routine = AdmitCardPlanner.defaultRoutineForClass(5, settings)
        assertEquals(
            listOf(
                "ইংরেজি",
                "বাংলা",
                "বাংলাদেশ ও বিশ্বপরিচয়",
                "প্রাথমিক গণিত",
                "ধর্ম ও নৈতিক শিক্ষা",
                "প্রাথমিক বিজ্ঞান"
            ),
            class5Routine.map { it.subject }
        )

        val class2Routine = AdmitCardPlanner.defaultRoutineForClass(2, settings)
        assertEquals(
            listOf("ইংরেজি", "বাংলা", "প্রাথমিক গণিত"),
            class2Routine.map { it.subject }
        )
    }

    @Test
    fun inferBanglaWeekday_parsesEnglishAndBengaliDates() {
        assertEquals("মঙ্গলবার", AdmitCardPlanner.inferBanglaWeekday("01/12/2026"))
        assertEquals("বুধবার", AdmitCardPlanner.inferBanglaWeekday("02.12.2026"))
        assertEquals("বৃহস্পতিবার", AdmitCardPlanner.inferBanglaWeekday("০৩/১২/২০২৬"))
        assertEquals("রবিবার", AdmitCardPlanner.inferBanglaWeekday("০৬/১২/২০২৬"))
        assertEquals("সোমবার", AdmitCardPlanner.inferBanglaWeekday("07-12-2026"))
        assertEquals("মঙ্গলবার", AdmitCardPlanner.inferBanglaWeekday("০৮-১২-২৬"))
        assertEquals("", AdmitCardPlanner.inferBanglaWeekday("31/02/2026"))
        assertEquals("", AdmitCardPlanner.inferBanglaWeekday("invalid"))
    }

    @Test
    fun buildPages_paginatesTwoToFivePerPage_andSortsRolls() {
        val settings = Settings()
        val students = (1..9).map { r -> student(5, r, "শিক্ষার্থী $r") }
        val routine = AdmitCardPlanner.defaultRoutineForClass(5, settings)

        val p4 = AdmitCardPlanner.buildPages(
            students = students,
            settings = settings,
            config = AdmitCardConfig(selectedClass = 5, cardsPerPage = 4, rollOrder = SeatPlanRollOrder.DESCENDING),
            routineRows = routine
        )
        assertEquals(3, p4.size)
        assertEquals(listOf(9, 8, 7, 6), p4[0].items.map { it.roll })
        assertEquals(listOf(5, 4, 3, 2), p4[1].items.map { it.roll })
        assertEquals(listOf(1), p4[2].items.map { it.roll })

        val p2 = AdmitCardPlanner.buildPages(
            students = students,
            settings = settings,
            config = AdmitCardConfig(selectedClass = 5, cardsPerPage = 2, rollOrder = SeatPlanRollOrder.ASCENDING),
            routineRows = routine
        )
        assertEquals(5, p2.size)
        assertEquals(listOf(1, 2), p2[0].items.map { it.roll })

        val p5 = AdmitCardPlanner.buildPages(
            students = students,
            settings = settings,
            config = AdmitCardConfig(selectedClass = 5, cardsPerPage = 5),
            routineRows = routine
        )
        assertEquals(2, p5.size)
        assertEquals(5, p5[0].items.size)
        assertEquals(4, p5[1].items.size)
    }

    @Test
    fun buildPages_supportsRollRange_extraBlankCards_andFormatting() {
        val settings = Settings(school = "৩৮ নং কটুরাকান্দি সরকারি প্রাথমিক বিদ্যালয়", year = "2026")
        val students = (1..10).map { r -> student(5, r, "নাম $r") }
        val cfg = AdmitCardConfig(
            selectedClass = 5,
            rollOrder = SeatPlanRollOrder.ASCENDING,
            rollFrom = 4,
            rollTo = 6,
            extraBlankCards = 2,
            cardsPerPage = 3,
            padRollTwoDigits = true,
            numeralMode = SeatPlanNumeralMode.BENGALI
        )
        val pages = AdmitCardPlanner.buildPages(students, settings, cfg, emptyList())
        assertEquals(2, pages.size)
        assertEquals(listOf(4, 5, 6), pages[0].items.map { it.roll })
        assertTrue(pages[1].items.all { it.isBlank && it.roll == null })
        assertEquals(2, pages[1].items.size)

        assertEquals("০৪", AdmitCardPlanner.formatRoll(4, cfg))
        assertEquals("14", AdmitCardPlanner.formatRoll(14, cfg.copy(numeralMode = SeatPlanNumeralMode.ENGLISH)))
        assertEquals("........", AdmitCardPlanner.formatRoll(null, cfg))
        assertEquals(AdmitBorderStyle.DASHED, cfg.borderStyle)
        assertEquals(AdmitDashStyle.NORMAL, cfg.dashStyle)
        assertTrue(cfg.showCenterDivider)
        assertEquals("৩য় প্রান্তিক মূল্যায়ন - ২০২৬", AdmitCardPlanner.resolvedExamTitle(cfg, settings))
        assertEquals("৩য় প্রান্তিক মূল্যায়ন - ২০২৬ এর রুটিন", AdmitCardPlanner.resolvedRoutineTitle(cfg, settings))
    }

    @Test
    fun calendarDateHelpers_and_multiSubjectResolution_workPerClass() {
        val settings = Settings()
        val millis = AdmitCardPlanner.parseDateToUtcMillis("২৬/১১/২০২৬")
        assertTrue(millis != null)
        val (dateBn, dayBn) = AdmitCardPlanner.formatDateAndDayFromUtcMillis(millis!!)
        assertEquals("২৬/১১/২০২৬", dateBn)
        assertEquals("বৃহস্পতিবার", dayBn)

        val baseRows = listOf(
            ExamRoutineEntry(date = "০১/১২/২০২৬", day = "মঙ্গলবার", subjects = listOf("ইংরেজি")),
            ExamRoutineEntry(
                date = "০৩/১২/২০২৬",
                day = "বৃহস্পতিবার",
                subjects = listOf("বাংলাদেশ ও বিশ্বপরিচয়", "সামাজিক বিজ্ঞান ও বিজ্ঞান")
            ),
            ExamRoutineEntry(date = "০৬/১২/২০২৬", day = "রবিবার", subjects = listOf("প্রাথমিক বিজ্ঞান অথবা মৌখিক")),
            ExamRoutineEntry(date = "০৭/১২/২০২৬", day = "সোমবার", subjects = listOf("বাংলা", "ধর্ম ও নৈতিক শিক্ষা"))
        )

        assertEquals("বাংলা / ধর্ম ও নৈতিক শিক্ষা", baseRows[3].subject)

        val cls5 = AdmitCardPlanner.resolveRoutineForClass(baseRows, targetCls = 5, settings = settings)
        assertEquals(4, cls5.size)
        assertEquals(listOf("বাংলাদেশ ও বিশ্বপরিচয়"), cls5[1].nonEmptySubjects)
        assertEquals(listOf("প্রাথমিক বিজ্ঞান", "মৌখিক"), cls5[2].nonEmptySubjects)
        assertEquals(listOf("বাংলা", "ধর্ম ও নৈতিক শিক্ষা"), cls5[3].nonEmptySubjects)

        val cls2 = AdmitCardPlanner.resolveRoutineForClass(baseRows, targetCls = 2, settings = settings)
        assertEquals(4, cls2.size)
        assertEquals(listOf("সামাজিক বিজ্ঞান ও বিজ্ঞান"), cls2[1].nonEmptySubjects)
        // Class 2 doesn't have প্রাথমিক বিজ্ঞান, so it resolves cleanly to the custom alternative "মৌখিক"
        assertEquals(listOf("মৌখিক"), cls2[2].nonEmptySubjects)

        // A day containing ONLY upper-tier subjects is omitted cleanly for lower classes
        val upperOnlyDay = listOf(ExamRoutineEntry(date = "০৮/১২/২০২৬", day = "মঙ্গলবার", subjects = listOf("প্রাথমিক বিজ্ঞান")))
        val cls1Resolved = AdmitCardPlanner.resolveRoutineForClass(upperOnlyDay, targetCls = 1, settings = settings)
        assertTrue(cls1Resolved.isEmpty())

        val defaults = AdmitCardPlanner.defaultBaseRoutines(settings)
        assertEquals(2, defaults.size)
    }

    @Test
    fun typographyAndSpacingDefaults_clampSafely() {
        val defaultCfg = AdmitCardConfig()
        assertEquals(15.5f, defaultCfg.safeHeaderToInfoGapPt, 0.01f)
        assertEquals(10.0f, defaultCfg.safeSchoolFontPt, 0.01f)
        assertEquals(9.0f, defaultCfg.safeHeaderMetaFontPt, 0.01f)
        assertEquals(10.0f, defaultCfg.safeBadgeFontPt, 0.01f)
        assertEquals(9.5f, defaultCfg.safeStudentInfoFontPt, 0.01f)
        assertEquals(8.5f, defaultCfg.safeRoutineHeaderFontPt, 0.01f)
        assertEquals(8.5f, defaultCfg.safeRoutineBodyFontPt, 0.01f)
        assertEquals(8.5f, defaultCfg.safeSignatureTitleFontPt, 0.01f)
        assertEquals(false, defaultCfg.includeBlankRoutineRow)
        assertEquals(false, defaultCfg.showSignatureLine)
        assertEquals("", defaultCfg.routineNote)
        assertTrue(defaultCfg.badgeBold)
        assertTrue(defaultCfg.badgeUnderline)
        assertTrue(defaultCfg.studentLabelBold)

        val clamped = AdmitCardConfig(
            headerToInfoGapPt = 0f,
            verticalGapPt = 40f,
            schoolFontPt = 2f,
            routineBodyFontPt = 30f
        )
        assertEquals(4f, clamped.safeHeaderToInfoGapPt, 0.01f)
        assertEquals(28f, clamped.safeVerticalGapPt, 0.01f)
        assertEquals(6f, clamped.safeSchoolFontPt, 0.01f)
        assertEquals(16f, clamped.safeRoutineBodyFontPt, 0.01f)
    }
}
