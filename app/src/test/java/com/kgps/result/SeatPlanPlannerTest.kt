package com.kgps.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeatPlanPlannerTest {

    private fun makeStudents(cls: Int, count: Int): List<Student> =
        (1..count).map { r ->
            Student(id = "c$cls-$r", cls = cls, roll = r, name = "শিক্ষার্থী $cls-$r")
        }.shuffled()

    @Test
    fun defaultTwoBySixDescendingLayoutMatchesTemplate() {
        val students = makeStudents(cls = 5, count = 19)
        val settings = Settings(
            school = "৩৮ নং কটুরাকান্দি সরকারি প্রাথমিক বিদ্যালয়",
            schoolAddress = "আলফাডাঙ্গা, ফরিদপুর।",
            year = "2026"
        )
        val config = SeatPlanConfig(selectedClasses = listOf(5))
        val pages = SeatPlanPlanner.buildPages(students, settings, config, listOf(1, 2, 3, 4, 5))

        assertEquals(2, pages.size)
        assertEquals(12, pages[0].slips.size)
        assertEquals(12, pages[1].slips.size)

        // Page 1: rolls 19 down to 8
        val page1Rolls = pages[0].slips.map { it?.roll }
        assertEquals((19 downTo 8).toList(), page1Rolls)

        // Page 2: rolls 7 down to 1, then 5 empty slots
        val page2NonNull = pages[1].slips.take(7).map { it?.roll }
        assertEquals((7 downTo 1).toList(), page2NonNull)
        assertTrue(pages[1].slips.drop(7).all { it == null })

        assertEquals("৩৮ নং কটুরাকান্দি সরকারি প্রাথমিক বিদ্যালয়", SeatPlanPlanner.resolvedSchoolName(config, settings))
        assertEquals("আলফাডাঙ্গা, ফরিদপুর।", SeatPlanPlanner.resolvedSchoolAddress(config, settings))
        assertEquals("৩য় প্রান্তিক মূল্যায়ন - ২০২৬", SeatPlanPlanner.resolvedExamTitle(config, settings))
        assertEquals("আসন বিন্যাস", SeatPlanPlanner.resolvedSubtitle(config))
        assertEquals("০৭", SeatPlanPlanner.formatRoll(7, config))
        assertEquals("১৯", SeatPlanPlanner.formatRoll(19, config))
    }

    @Test
    fun rollRangeFilterAscendingOrderAndExtraBlankSlipsWork() {
        val students = makeStudents(cls = 3, count = 15)
        val config = SeatPlanConfig(
            selectedClasses = listOf(3),
            rollOrder = SeatPlanRollOrder.ASCENDING,
            rollFrom = 5,
            rollTo = 10,
            extraBlankSlips = 2,
            columns = 2,
            rows = 4
        )
        val pages = SeatPlanPlanner.buildPages(students, Settings(), config, listOf(1, 2, 3, 4, 5))

        assertEquals(1, pages.size)
        val slips = pages[0].slips
        assertEquals(8, slips.size)
        assertEquals(listOf(5, 6, 7, 8, 9, 10), slips.take(6).map { it?.roll })
        assertTrue(slips.take(6).all { it?.isBlank == false })
        assertTrue(slips[6]?.isBlank == true)
        assertTrue(slips[7]?.isBlank == true)
        assertNull(slips[6]?.roll)
        assertEquals(3, slips[6]?.cls)
    }

    @Test
    fun sideBySideBenchPairingPlacesLeftAndRightClassesInColumns() {
        val c4 = makeStudents(cls = 4, count = 5)
        val c5 = makeStudents(cls = 5, count = 3)
        val config = SeatPlanConfig(
            pairMode = SeatPlanPairMode.SIDE_BY_SIDE,
            leftClass = 4,
            rightClass = 5,
            rollOrder = SeatPlanRollOrder.DESCENDING,
            columns = 2,
            rows = 6
        )
        val pages = SeatPlanPlanner.buildPages(c4 + c5, Settings(), config, listOf(1, 2, 3, 4, 5))

        assertEquals(1, pages.size)
        val slips = pages[0].slips
        assertEquals(12, slips.size)

        // Row 0: left (col 0) = class 4 roll 5, right (col 1) = class 5 roll 3
        assertEquals(4, slips[0]?.cls)
        assertEquals(5, slips[0]?.roll)
        assertEquals(5, slips[1]?.cls)
        assertEquals(3, slips[1]?.roll)

        // Row 2: left = class 4 roll 3, right = class 5 roll 1
        assertEquals(4, slips[4]?.cls)
        assertEquals(3, slips[4]?.roll)
        assertEquals(5, slips[5]?.cls)
        assertEquals(1, slips[5]?.roll)

        // Row 3: left = class 4 roll 2, right = blank slip for class 5 (since c5 had 3 and c4 had 5)
        assertEquals(4, slips[6]?.cls)
        assertEquals(2, slips[6]?.roll)
        assertFalse(slips[6]!!.isBlank)
        assertTrue(slips[7]!!.isBlank)
        assertEquals(5, slips[7]?.cls)

        // Row 5 (beyond totalRows=5): both null
        assertNull(slips[10])
        assertNull(slips[11])
    }

    @Test
    fun englishNumeralsAndRoomOverrideAreFormattedCorrectly() {
        val settings = Settings(school = "Demo School", schoolAddress = "Dhaka", year = "2026")
        val config = SeatPlanConfig(
            numeralMode = SeatPlanNumeralMode.ENGLISH,
            padRollTwoDigits = true,
            roomLabel = "কক্ষ ০২",
            showSchoolAddress = false
        )
        assertEquals("05", SeatPlanPlanner.formatRoll(5, config))
        assertEquals("14", SeatPlanPlanner.formatRoll(14, config))
        assertEquals("..........", SeatPlanPlanner.formatRoll(null, config))
        assertEquals("", SeatPlanPlanner.resolvedSchoolAddress(config, settings))
        assertEquals("৩য় প্রান্তিক মূল্যায়ন - 2026", SeatPlanPlanner.resolvedExamTitle(config, settings))
        assertEquals("আসন বিন্যাস · কক্ষ ০২", SeatPlanPlanner.resolvedSubtitle(config))
    }
}
