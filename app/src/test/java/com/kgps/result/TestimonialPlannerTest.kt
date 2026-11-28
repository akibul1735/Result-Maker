package com.kgps.result

import com.kgps.result.ui.screens.applyStudentProfileFieldUpdates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TestimonialPlannerTest {

    private fun sampleSettings(customFields: List<CustomFieldDef> = emptyList()): Settings {
        return Settings(
            school = "উদাহরণ সরকারি প্রাথমিক বিদ্যালয়",
            schoolAddress = "উপজেলা: কলাপাড়া, জেলা: পটুয়াখালী।",
            year = "২০২৬",
            customFields = customFields
        )
    }

    @Test
    fun resolvesFatherMotherDobFromCustomFieldsAndOverrides() {
        val settings = sampleSettings(
            customFields = listOf(
                CustomFieldDef(key = "CF_MOTHER", label = "মাতার নাম"),
                CustomFieldDef(key = "CF_DOB", label = "জন্মতারিখ")
            )
        )
        val student = Student(
            id = "s1",
            name = "আরিফুল ইসলাম",
            guardian = "মোঃ করিম উদ্দিন",
            cls = 5,
            roll = 1,
            x = mapOf(
                "CF_MOTHER" to "মোছাঃ আমেনা বেগম",
                "CF_DOB" to "12/05/2014"
            )
        )

        assertEquals("মোঃ করিম উদ্দিন", TestimonialPlanner.resolveFatherName(student, settings))
        assertEquals("মোছাঃ আমেনা বেগম", TestimonialPlanner.resolveMotherName(student, settings))
        assertEquals(
            "১২/০৫/২০১৪ খ্রি.",
            TestimonialPlanner.resolveDob(student, settings, mode = SeatPlanNumeralMode.BENGALI)
        )

        val override = TestimonialStudentOverride(
            fatherName = "জনাব করিম",
            motherName = "বেগম আমেনা",
            dob = "2014-05-12 AD"
        )
        assertEquals("জনাব করিম", TestimonialPlanner.resolveFatherName(student, settings, override))
        assertEquals("বেগম আমেনা", TestimonialPlanner.resolveMotherName(student, settings, override = override))
        assertEquals(
            "2014-05-12 AD",
            TestimonialPlanner.resolveDob(student, settings, mode = SeatPlanNumeralMode.ENGLISH, override = override)
        )
    }

    @Test
    fun serialModesGenerateExpectedStrings() {
        val prefixCfg = TestimonialConfig(
            serialMode = TestimonialSerialMode.BASE_PLUS_ROLL,
            serialStartOrPrefix = "২০২৬৫",
            padRollTwoDigits = true,
            numeralMode = SeatPlanNumeralMode.BENGALI
        )
        assertEquals("২০২৬৫২২", TestimonialPlanner.resolveSerial(0, 22, prefixCfg))
        assertEquals("২০২৬৫০১", TestimonialPlanner.resolveSerial(21, 1, prefixCfg))

        val autoCfg = TestimonialConfig(
            serialMode = TestimonialSerialMode.AUTO_INCREMENT,
            serialStartOrPrefix = "0099",
            numeralMode = SeatPlanNumeralMode.ENGLISH
        )
        assertEquals("0099", TestimonialPlanner.resolveSerial(0, 1, autoCfg))
        assertEquals("0100", TestimonialPlanner.resolveSerial(1, 2, autoCfg))

        val blankCfg = TestimonialConfig(serialMode = TestimonialSerialMode.BLANK)
        assertEquals("", TestimonialPlanner.resolveSerial(0, 1, blankCfg))
    }

    @Test
    fun parseBodyTokensPopulatesAllPlaceholdersAndPreservesOrder() {
        val settings = sampleSettings()
        val cfg = TestimonialConfig(
            testimonialType = TestimonialType.PROTTAYONPATRA,
            bodyPreset = TestimonialBodyPreset.STUDIED_CLASSIC,
            padRollTwoDigits = true,
            numeralMode = SeatPlanNumeralMode.BENGALI
        )
        val item = TestimonialItem(
            studentId = "s5",
            studentName = "মাহিয়া আক্তার",
            fatherName = "রফিকুল ইসলাম",
            motherName = "সালমা বেগম",
            dateOfBirth = "০৩/০২/২০১৫ খ্রি.",
            cls = 5,
            roll = 4,
            serialText = "২০২৬৫০৪",
            sessionYear = "২০২৫"
        )

        val tokens = TestimonialPlanner.parseBodyTokens(item, cfg, settings)
        val fieldValues = tokens.filterIsInstance<TestimonialBodyToken.Field>().map { it.value }
        assertEquals(
            listOf("মাহিয়া আক্তার", "রফিকুল ইসলাম", "সালমা বেগম", "০৩/০২/২০১৫ খ্রি.", "০৪", "২০২৫", "৫ম"),
            fieldValues
        )
        assertTrue(tokens.first() is TestimonialBodyToken.Plain)
    }

    @Test
    fun buildPagesSupportsOrderingRangeFilteringBlankFormsAndMultiClass() {
        val settings = sampleSettings()
        val students = listOf(
            Student(id = "c4_1", name = "নাদিম", cls = 4, roll = 1),
            Student(id = "c5_1", name = "রনি", cls = 5, roll = 1),
            Student(id = "c5_2", name = "জনি", cls = 5, roll = 2),
            Student(id = "c5_3", name = "তনি", cls = 5, roll = 3)
        )

        val descCfg = TestimonialConfig(
            selectedClass = 5,
            rollOrder = SeatPlanRollOrder.DESCENDING,
            rollFrom = 2,
            rollTo = 3,
            extraBlankCount = 1,
            serialMode = TestimonialSerialMode.AUTO_INCREMENT,
            serialStartOrPrefix = "101",
            numeralMode = SeatPlanNumeralMode.ENGLISH
        )
        val singlePages = TestimonialPlanner.buildPages(
            students = students,
            settings = settings,
            config = descCfg,
            allowedClassIds = listOf(4, 5)
        )
        assertEquals(3, singlePages.size)
        assertEquals(listOf("তনি", "জনি", ""), singlePages.map { it.item.studentName })
        assertTrue(singlePages.last().item.isBlank)
        assertEquals(listOf("101", "102", "103"), singlePages.map { it.item.serialText })

        val multiPages = TestimonialPlanner.buildMultiClassPages(
            students = students,
            settings = settings,
            baseConfig = descCfg.copy(
                rollOrder = SeatPlanRollOrder.ASCENDING,
                rollFrom = null,
                rollTo = null,
                extraBlankCount = 0
            ),
            selectedClasses = listOf(4, 5),
            allowedClassIds = listOf(4, 5)
        )
        assertEquals(4, multiPages.size)
        assertEquals(listOf(1, 2, 3, 4), multiPages.map { it.pageNumber })
        assertEquals(listOf("101", "102", "103", "104"), multiPages.map { it.item.serialText })
    }

    @Test
    fun applyStudentProfileFieldUpdatesCreatesMissingCustomFieldsAndUpdatesStudents() {
        val settings = sampleSettings()
        val st = Student(id = "s1", name = "তানভীর", guardian = "", cls = 5, roll = 7)
        val cfg = TestimonialConfig()

        val (updatedStudents, nextSettings) = applyStudentProfileFieldUpdates(
            students = listOf(st),
            settings = settings,
            config = cfg,
            updatedMap = mapOf(
                "s1" to TestimonialStudentOverride(
                    fatherName = "মোঃ হাসিব",
                    motherName = "নাজমা বেগম",
                    dob = "১০/১০/২০১৫"
                )
            )
        )

        assertTrue(nextSettings.customFields.any { it.key == "CF_MOTHER_NAME" && it.label == "মাতার নাম" })
        assertTrue(nextSettings.customFields.any { it.key == "CF_DOB" && it.label == "জন্মতারিখ" })
        assertEquals(1, updatedStudents.size)
        assertEquals("মোঃ হাসিব", updatedStudents.first().guardian)
        assertEquals("নাজমা বেগম", updatedStudents.first().x["CF_MOTHER_NAME"])
        assertEquals("১০/১০/২০১৫", updatedStudents.first().x["CF_DOB"])
    }
}
