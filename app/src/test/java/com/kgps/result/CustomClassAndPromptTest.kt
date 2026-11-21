package com.kgps.result

import com.kgps.result.cloud.TeacherPermissions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomClassAndPromptTest {

    @After
    fun resetRegistry() {
        SchoolClassRegistry.update(emptyList())
    }

    @Test
    fun customClassesIntegrateWithSyllabusTierAndRegistry() {
        val prePrimary = CustomClassDef(id = 6, nameBn = "প্রাক-প্রাথমিক", shortBn = "প্রাক", syllabusTier = 1)
        val classSix = CustomClassDef(id = 7, nameBn = "ষষ্ঠ", shortBn = "৬ষ্ঠ", syllabusTier = 3)
        val settings = Settings(customClasses = listOf(prePrimary, classSix))
        SchoolClassRegistry.update(settings.customClasses)

        val classes = activeSchoolClasses(settings)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), classes.map { it.id })
        assertEquals("প্রাক-প্রাথমিক", classNameBn(6, settings))
        assertEquals("প্রাক", classShortBn(6, settings))
        assertEquals("ষষ্ঠ", classNameBn(7, settings))
        assertEquals("৬ষ্ঠ", classShortBn(7, settings))

        // Tier 1 custom class -> SOC + 50 written for BAN
        val preSubs = subjectsFor(6, "ইসলাম")
        assertTrue(preSubs.any { it.key == "SOC" })
        assertFalse(preSubs.any { it.key == "SCI" })
        assertEquals(50, preSubs.first { it.key == "BAN" }.written)

        // Tier 3 custom class -> SCI + BGW + 70 written for BAN
        val sixSubs = subjectsFor(7, "ইসলাম")
        assertTrue(sixSubs.any { it.key == "SCI" })
        assertTrue(sixSubs.any { it.key == "BGW" })
        assertEquals(70, sixSubs.first { it.key == "BAN" }.written)
    }

    @Test
    fun repoCsvRoundTripAndParseClassPreservesCustomClasses() {
        val custom = CustomClassDef(id = 6, nameBn = "ষষ্ঠ", shortBn = "৬ষ্ঠ", syllabusTier = 3)
        val settings = Settings(school = "টেস্ট বিদ্যালয়", customClasses = listOf(custom))
        SchoolClassRegistry.update(settings.customClasses)

        val st6 = Student(
            id = "s-6-1",
            cls = 6,
            roll = 1,
            name = "তানভীর আহমেদ",
            m = mapOf("BAN_1_TB" to 9.0, "SCI_1_WR" to 62.0)
        )
        val csv = Repo.toCsv(listOf(st6), settings.customFields, CsvHeaderStyle.FRIENDLY_BN, targetCls = 6)
        assertTrue(csv.contains("তানভীর আহমেদ"))
        val rows = Repo.parseCsv(csv)
        assertTrue(rows.size >= 2)

        assertEquals(6, Repo.parseClassNum("ষষ্ঠ"))
        assertEquals(6, Repo.parseClassNum("৬ষ্ঠ শ্রেণি"))
        assertEquals(6, Repo.parseClassNum("6"))
    }

    @Test
    fun resetAndPermissionsIncludeCustomClasses() {
        val prePrimary = CustomClassDef(id = 6, nameBn = "প্রাক-প্রাথমিক", shortBn = "প্রাক", syllabusTier = 1)
        val classSix = CustomClassDef(id = 7, nameBn = "ষষ্ঠ", shortBn = "৬ষ্ঠ", syllabusTier = 3)
        val settings = Settings(customClasses = listOf(prePrimary, classSix))
        SchoolClassRegistry.update(settings.customClasses)

        val allPerm = TeacherPermissions(allClasses = true)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), allPerm.effectiveClasses(activeClassIds(settings)))

        val restrictedPerm = TeacherPermissions(allClasses = false, allowedClasses = listOf(6))
        assertEquals(listOf(6), restrictedPerm.effectiveClasses(activeClassIds(settings)))

        val tier1Opts = DataResetEngine.subjectOptionsForClasses(setOf(6)).map { it.key }
        assertTrue("SOC" in tier1Opts)
        assertFalse("SCI" in tier1Opts)

        val tier3Opts = DataResetEngine.subjectOptionsForClasses(setOf(7)).map { it.key }
        assertTrue("SCI" in tier3Opts)
        assertTrue("BGW" in tier3Opts)
    }

    @Test
    fun csvPromptStaysCompactAndIncludesCustomClassInfo() {
        val custom = CustomClassDef(id = 6, nameBn = "ষষ্ঠ", shortBn = "৬ষ্ঠ", syllabusTier = 3)
        val settings = Settings(customClasses = listOf(custom))
        SchoolClassRegistry.update(settings.customClasses)

        val cfg = CsvPromptConfig(
            module = CsvImportModule.TERM_MARKS,
            targetCls = 6,
            targetTerm = 1,
            selectedSubjectKeys = setOf("BAN", "MATH"),
            selectedColumnKeys = defaultSelectedColumnKeys(
                module = CsvImportModule.TERM_MARKS,
                targetCls = 6,
                targetTerm = 1,
                selectedSubjects = setOf("BAN", "MATH")
            )
        )
        val prompt = CsvImportAssistant.buildAiPrompt(cfg, settings)
        assertTrue(prompt.contains("ষষ্ঠ"))
        assertTrue(prompt.contains("```csv"))
        assertTrue("Prompt should stay concise (< 900 chars), was ${prompt.length}", prompt.length < 900)
    }

    @Test
    fun csvColumnsIncludeContAndTotalAndValidateCorrectly() {
        val cfg = CsvPromptConfig(
            module = CsvImportModule.TERM_MARKS,
            targetCls = 3,
            targetTerm = 2,
            selectedSubjectKeys = setOf("SCI")
        )
        val cols = CsvImportAssistant.availableColumnsForConfig(cfg, emptyList())
        val contCol = cols.firstOrNull { it.key == "SCI_2_CONT" }
        val totalCol = cols.firstOrNull { it.key == "SCI_2_TOTAL" }
        assertTrue("Expected SCI_2_CONT option", contCol != null)
        assertTrue("Expected SCI_2_TOTAL option", totalCol != null)
        assertTrue(contCol!!.labelBn.contains("মোট ধারাবাহিক"))
        assertTrue(totalCol!!.labelBn.contains("মোট"))

        // Valid CONT (<= 30) & TOTAL (<= 100) should produce zero warnings
        val validCsv = "রোল,নাম,SCI_2_CONT,SCI_2_TOTAL\n1,রহিম,28,92"
        val validParsed = Repo.parseCsv(validCsv)
        val validCols = Repo.detectCsvColumns(validParsed, emptyList())
        val validReport = CsvImportAssistant.validateCsvData(validParsed, validCols, emptyList(), 3)
        assertEquals(
            "Expected no warnings for valid CONT/TOTAL, got ${validReport.rows.flatMap { it.messages }}",
            0,
            validReport.warningCount
        )

        // Out-of-range CONT (> 30) & TOTAL (> 100) should produce warnings
        val badCsv = "রোল,নাম,SCI_2_CONT,SCI_2_TOTAL\n1,রহিম,35,110"
        val badParsed = Repo.parseCsv(badCsv)
        val badCols = Repo.detectCsvColumns(badParsed, emptyList())
        val badReport = CsvImportAssistant.validateCsvData(badParsed, badCols, emptyList(), 3)
        assertTrue("Expected warning for CONT > 30 and TOTAL > 100", badReport.warningCount >= 1)
        assertTrue(badReport.rows.first().messages.any { it.contains("SCI_2_CONT") })
        assertTrue(badReport.rows.first().messages.any { it.contains("SCI_2_TOTAL") })
    }

    @Test
    fun studentTotalFallsBackToTotalKeyAndTermOrdinalsAreCorrect() {
        val sci = subjectsFor(3, "ইসলাম").first { it.key == "SCI" }
        val stTotalOnly = Student(id = "s1", cls = 3, roll = 1, name = "আরিফ", m = mapOf("SCI_2_TOTAL" to 85.0))
        assertEquals(85.0, stTotalOnly.total(sci, 2), 0.001)

        val stWithParts = Student(
            id = "s2",
            cls = 3,
            roll = 2,
            name = "করিম",
            m = mapOf("SCI_2_CONT" to 25.0, "SCI_2_WR" to 60.0, "SCI_2_TOTAL" to 99.0)
        )
        assertEquals(85.0, stWithParts.total(sci, 2), 0.001)

        assertEquals("১ম", termOrdinalBn(1))
        assertEquals("২য়", termOrdinalBn(2))
        assertEquals("৩য়", termOrdinalBn(3))
        assertEquals("১ম প্রান্তিক", termNameBn(1))
        assertEquals("২য় প্রান্তিক", termNameBn(2))
        assertEquals("৩য় প্রান্তিক", termNameBn(3))
    }
}
