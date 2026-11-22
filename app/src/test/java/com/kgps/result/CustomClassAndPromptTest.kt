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

    @Test
    fun smartCsvParsesTopMetadataAndTwoRowGroupedHeadersAcrossAllThreeTerms() {
        val raw = """
            শ্রেণি: ৩য়, বিষয়: বাংলা
            রোল,নাম,১ম প্রান্তিক,,২য় প্রান্তিক,,৩য় প্রান্তিক,
            ,,ধারাবাহিক,সামস্টিক,ধারাবাহিক,সামষ্টিক,ধারাবাহিক,সামস্টিক
            ১,আফিয়া তাসনিম,২৫,৬০,২৭,৬৪,২৮,৬৮
            ২,তানভীর হাসান,22,55,24,-,26,61
        """.trimIndent()

        val sheet = Repo.parseSmartCsvSheet(raw)
        assertEquals(3, sheet.detectedCls)
        assertEquals("BAN", sheet.detectedSubjectKey)
        assertTrue(sheet.usedTwoRowHeader)
        assertEquals(1, sheet.skippedMetadataLines)
        assertEquals(3, sheet.rows.size) // 1 combined header + 2 students

        val cols = Repo.detectCsvColumns(
            rows = sheet.rows,
            existingCustomFields = emptyList(),
            contextSubjectKey = sheet.detectedSubjectKey,
            contextTerm = sheet.detectedTerm
        )
        assertEquals("roll", cols[0].targetKey)
        assertEquals("name", cols[1].targetKey)
        assertEquals("BAN_1_CONT", cols[2].targetKey)
        assertEquals("BAN_1_WR", cols[3].targetKey)
        assertEquals("BAN_2_CONT", cols[4].targetKey)
        assertEquals("BAN_2_WR", cols[5].targetKey)
        assertEquals("BAN_3_CONT", cols[6].targetKey)
        assertEquals("BAN_3_WR", cols[7].targetKey)

        val report = CsvImportAssistant.validateCsvData(
            parsedRows = sheet.rows,
            columns = cols,
            existingStudents = emptyList(),
            fallbackCls = sheet.detectedCls ?: 1
        )
        assertEquals("Expected zero warnings, got ${report.rows.flatMap { it.messages }}", 0, report.warningCount)
        assertEquals(0, report.errorCount)

        val summary = Repo.applyMappedCsvImport(
            rows = sheet.rows,
            columns = cols,
            existingStudents = emptyList(),
            existingSettings = DEFAULT_SETTINGS,
            defaultCls = sheet.detectedCls ?: 1
        )
        assertEquals(2, summary.addedCount)
        val st1 = summary.students.first { it.roll == 1 && it.cls == 3 }
        val st2 = summary.students.first { it.roll == 2 && it.cls == 3 }
        assertEquals(25.0, st1.m["BAN_1_CONT"]!!, 0.001)
        assertEquals(60.0, st1.m["BAN_1_WR"]!!, 0.001)
        assertEquals(27.0, st1.m["BAN_2_CONT"]!!, 0.001)
        assertEquals(64.0, st1.m["BAN_2_WR"]!!, 0.001)
        assertEquals(28.0, st1.m["BAN_3_CONT"]!!, 0.001)
        assertEquals(68.0, st1.m["BAN_3_WR"]!!, 0.001)
        assertFalse("Dash placeholder should not populate BAN_2_WR", st2.m.containsKey("BAN_2_WR"))

        val ban = subjectsFor(3, "ইসলাম").first { it.key == "BAN" }
        assertEquals(85.0, st1.total(ban, 1), 0.001)
        assertEquals(91.0, st1.total(ban, 2), 0.001)
        assertEquals(96.0, st1.total(ban, 3), 0.001)
    }

    @Test
    fun smartCsvReconcilesContOverOldGranularComponentsAndSupportsMarkdown() {
        val existing = listOf(
            Student(
                id = "st-4-1",
                cls = 4,
                roll = 1,
                name = "সাদিয়া ইসলাম",
                m = mapOf(
                    "MATH_1_TB" to 5.0,
                    "MATH_1_ACT" to 3.0,
                    "MATH_1_LANG" to 2.0,
                    "MATH_1_CT" to 5.0,
                    "MATH_1_WR" to 40.0
                )
            )
        )
        val markdown = """
            ```markdown
            শ্রেণি,৪র্থ
            বিষয়,গণিত
            | রোল | নাম | ১ম প্রান্তিক ধারাবাহিক | ১ম প্রান্তিক সামষ্টিক | ২য় প্রান্তিক ধারাবাহিক | ২য় প্রান্তিক সামষ্টিক |
            | --- | --- | --- | --- | --- | --- |
            | ১ | সাদিয়া ইসলাম | 26 | 62 | — | N/A |
            ```
        """.trimIndent()

        val sheet = Repo.parseSmartCsvSheet(markdown)
        assertEquals(4, sheet.detectedCls)
        assertEquals("MATH", sheet.detectedSubjectKey)
        assertFalse(sheet.usedTwoRowHeader)

        val cols = Repo.detectCsvColumns(sheet.rows, emptyList(), sheet.detectedSubjectKey, sheet.detectedTerm)
        val summary = Repo.applyMappedCsvImport(
            rows = sheet.rows,
            columns = cols,
            existingStudents = existing,
            existingSettings = DEFAULT_SETTINGS,
            defaultCls = sheet.detectedCls ?: 1
        )
        assertEquals(1, summary.updatedCount)
        val updated = summary.students.first { it.id == "st-4-1" }
        val math = subjectsFor(4, "ইসলাম").first { it.key == "MATH" }
        assertEquals(26.0, updated.cont(math, 1), 0.001)
        assertEquals(62.0, updated.wr(math, 1), 0.001)
        assertEquals(88.0, updated.total(math, 1), 0.001)
        assertFalse(updated.m.containsKey("MATH_1_TB"))
        assertFalse(updated.m.containsKey("MATH_2_CONT"))
        assertFalse(updated.m.containsKey("MATH_2_WR"))
    }

    @Test
    fun prefilledSubjectAllTermsTemplateAndNonWrittenSubjectWorkCleanly() {
        val roster = listOf(
            Student(id = "s-2-1", cls = 2, roll = 1, name = "রাফি"),
            Student(id = "s-2-2", cls = 2, roll = 2, name = "মীম")
        )
        val cfg = CsvPromptConfig(
            module = CsvImportModule.SUBJECT_ALL_TERMS,
            targetCls = 2,
            selectedSubjectKeys = setOf("ART"),
            allTermsSelection = setOf(1, 2, 3),
            useTwoRowGroupedHeader = true,
            selectedColumnKeys = defaultSelectedColumnKeys(
                module = CsvImportModule.SUBJECT_ALL_TERMS,
                targetCls = 2,
                targetTerm = 1,
                selectedSubjects = setOf("ART"),
                allTermsSelection = setOf(1, 2, 3)
            )
        )
        val template = CsvImportAssistant.buildBlankTemplateCsv(cfg, roster, DEFAULT_SETTINGS, prefillRoster = true)
        assertTrue(template.contains("শ্রেণি"))
        assertTrue(template.contains("শিল্পকলা"))
        assertTrue(template.contains("রাফি"))
        assertTrue(template.contains("মীম"))

        // Teacher fills in continuous marks and leaves 0 or dash in WR if extra WR column appears
        val filled = """
            শ্রেণি,২য়,বিষয়,শিল্পকলা
            রোল,নাম,১ম প্রান্তিক,,২য় প্রান্তিক,,৩য় প্রান্তিক,
            ,,ধারাবাহিক,সামষ্টিক,ধারাবাহিক,সামষ্টিক,ধারাবাহিক,সামষ্টিক
            1,রাফি,45,0,46,-,48,নাই
            2,মীম,42,,44,,47,
        """.trimIndent()
        val sheet = Repo.parseSmartCsvSheet(filled)
        assertEquals(2, sheet.detectedCls)
        assertEquals("ART", sheet.detectedSubjectKey)
        val cols = Repo.detectCsvColumns(sheet.rows, emptyList(), sheet.detectedSubjectKey, sheet.detectedTerm)
        val report = CsvImportAssistant.validateCsvData(sheet.rows, cols, roster, sheet.detectedCls ?: 2)
        assertEquals("Expected zero warnings for ART written==0 with 0/dash WR, got ${report.rows.flatMap { it.messages }}", 0, report.warningCount)

        val summary = Repo.applyMappedCsvImport(sheet.rows, cols, roster, DEFAULT_SETTINGS, 2)
        val rafi = summary.students.first { it.roll == 1 && it.cls == 2 }
        assertEquals(45.0, rafi.m["ART_1_CONT"]!!, 0.001)
        assertEquals(46.0, rafi.m["ART_2_CONT"]!!, 0.001)
        assertEquals(48.0, rafi.m["ART_3_CONT"]!!, 0.001)
        assertFalse("WR=0 on written==0 subject should not pollute keys", rafi.m.containsKey("ART_1_WR"))
    }

    @Test
    fun multiTermSelectionAcrossModulesAndMixedTsvCommaBannerWorkCleanly() {
        // 1. TERM_MARKS with all 3 terms selected
        val multiMarkCfg = CsvPromptConfig(
            module = CsvImportModule.TERM_MARKS,
            targetCls = 4,
            selectedSubjectKeys = setOf("BAN", "MATH"),
            multiTermSelection = setOf(1, 2, 3),
            conciseMarks = true,
            selectedColumnKeys = defaultSelectedColumnKeys(
                module = CsvImportModule.TERM_MARKS,
                targetCls = 4,
                targetTerm = 1,
                selectedSubjects = setOf("BAN", "MATH"),
                multiTermSelection = setOf(1, 2, 3),
                conciseMarks = true
            )
        )
        val header = CsvImportAssistant.buildHeaderLine(multiMarkCfg, emptyList())
        assertTrue(header.contains("BAN_1_CONT"))
        assertTrue(header.contains("BAN_2_WR"))
        assertTrue(header.contains("MATH_3_CONT"))
        assertTrue(header.contains("MATH_3_WR"))

        val attCfg = CsvPromptConfig(
            module = CsvImportModule.ATTENDANCE_TRAITS,
            targetCls = 4,
            multiTermSelection = setOf(1, 2, 3),
            selectedColumnKeys = defaultSelectedColumnKeys(
                module = CsvImportModule.ATTENDANCE_TRAITS,
                targetCls = 4,
                targetTerm = 1,
                multiTermSelection = setOf(1, 2, 3)
            )
        )
        val attHdr = CsvImportAssistant.buildHeaderLine(attCfg, emptyList())
        assertTrue(attHdr.contains("DAYS_1") && attHdr.contains("DAYS_2") && attHdr.contains("DAYS_3"))

        // 2. Excel TSV paste where banner line has a comma, table uses tabs
        val tsvWithCommaBanner =
            "শ্রেণি: ৩য়, বিষয়: ইংরেজি\n" +
                "ক্রমিক\tনাম\t১ম প্রান্তিক\t\t২য় প্রান্তিক\t\t৩য় প্রান্তিক\t\n" +
                "\t\tধারাবাহিক\tসামস্টিক\tধারাবাহিক\tসামষ্টিক\tধারাবাহিক\tসামস্টিক\n" +
                "১\tআরিফ\t24\t58\t25\t61\t27\t64"
        val sheetTsv = Repo.parseSmartCsvSheet(tsvWithCommaBanner)
        assertEquals(3, sheetTsv.detectedCls)
        assertEquals("ENG", sheetTsv.detectedSubjectKey)
        assertTrue(sheetTsv.usedTwoRowHeader)
        val tsvCols = Repo.detectCsvColumns(sheetTsv.rows, emptyList(), sheetTsv.detectedSubjectKey, sheetTsv.detectedTerm)
        assertEquals("roll", tsvCols[0].targetKey)
        assertEquals("ENG_1_CONT", tsvCols[2].targetKey)
        assertEquals("ENG_2_WR", tsvCols[5].targetKey)
        assertEquals("ENG_3_CONT", tsvCols[6].targetKey)
        assertEquals("ENG_3_WR", tsvCols[7].targetKey)

        // 3. Short headers without the word "প্রান্তিক"
        val shortHdr = "রোল,নাম,১ম ধারাবাহিক,১ম সামস্টিক,২য় ধারাবাহিক,২য় সামষ্টিক,৩য় ধারাবাহিক,৩য় সামষ্টিক\n1,মীম,22,50,24,55,26,60"
        val shortSheet = Repo.parseSmartCsvSheet(shortHdr)
        val shortCols = Repo.detectCsvColumns(shortSheet.rows, emptyList(), contextSubjectKey = "BAN")
        assertEquals("BAN_1_CONT", shortCols[2].targetKey)
        assertEquals("BAN_1_WR", shortCols[3].targetKey)
        assertEquals("BAN_2_CONT", shortCols[4].targetKey)
        assertEquals("BAN_2_WR", shortCols[5].targetKey)
        assertEquals("BAN_3_CONT", shortCols[6].targetKey)
        assertEquals("BAN_3_WR", shortCols[7].targetKey)
    }

    @Test
    fun artIsLastSubjectAcrossAllClassesAndSettingsRoundtrip() {
        val customLow = CustomClassDef(id = 6, nameBn = "প্রাক-প্রাথমিক", shortBn = "প্রাক", syllabusTier = 1)
        val customHigh = CustomClassDef(id = 7, nameBn = "ষষ্ঠ", shortBn = "৬ষ্ঠ", syllabusTier = 3)
        val st = Settings(
            school = "উদাহরণ সরকারি প্রাথমিক বিদ্যালয়",
            schoolAddress = "আলফাডাঙ্গা, ফরিদপুর",
            customClasses = listOf(customLow, customHigh)
        )
        SchoolClassRegistry.update(st.customClasses)

        for (cls in listOf(1, 2, 3, 4, 5, 6, 7)) {
            val subs = subjectsFor(cls, "ইসলাম")
            assertEquals("ART should be last for class $cls", "ART", subs.last().key)
            assertEquals("শিল্পকলা", subs.last().bn)
        }

        val updated = st.copy(schoolAddress = "বোয়ালমারী, ফরিদপুর")
        assertEquals("বোয়ালমারী, ফরিদপুর", updated.schoolAddress)
    }

    @Test
    fun threeTermBlankSheetsPlanMarksWithOptionalTotalAndAttendanceTraits() {
        val sample = listOf(
            Student(id = "s1", cls = 4, roll = 1, name = "তানভীর"),
            Student(id = "s2", cls = 4, roll = 2, name = "নুসরাত")
        )
        val settings = Settings(school = "কামারখালী সরকারি প্রাথমিক বিদ্যালয়", year = "২০২৬")

        // 1. 3-term marks without total: Roll, Name + 3*(CONT, WR) = 8 cols
        val baseMarksCols = BlankSheetCatalog.presetColumns(BlankSheetPreset.THREE_TERM_MARKS)
        assertEquals(8, baseMarksCols.size)
        assertTrue(BlankSheetCatalog.isThreeTermMarksActive(baseMarksCols))
        assertFalse(BlankSheetCatalog.hasThreeTermTotals(baseMarksCols))
        assertEquals(listOf("১ম প্রান্তিক", "২য় প্রান্তিক", "৩য় প্রান্তিক"), baseMarksCols.mapNotNull { it.groupBn }.distinct())

        // Toggle optional total -> Roll, Name + 3*(CONT, WR, TOTAL) = 11 cols
        val withTotalsCols = BlankSheetCatalog.toggleThreeTermTotals(baseMarksCols, true)
        assertEquals(11, withTotalsCols.size)
        assertTrue(BlankSheetCatalog.hasThreeTermTotals(withTotalsCols))
        assertEquals(
            listOf("ROLL", "NAME", "T1_CONT", "T1_WR", "T1_TOTAL", "T2_CONT", "T2_WR", "T2_TOTAL", "T3_CONT", "T3_WR", "T3_TOTAL"),
            withTotalsCols.map { it.id }
        )

        val marksPages = BlankSheetPlanner.buildPages(
            students = sample,
            settings = settings,
            config = BlankSheetConfig(selectedClass = 4, subjectKey = "MATH", columns = withTotalsCols),
            allowedClassIds = listOf(4)
        )
        assertEquals(1, marksPages.size)
        assertEquals("গণিত", marksPages.first().subjectTitleBn)
        assertEquals("৩ প্রান্তিক", marksPages.first().termTitleBn)
        assertFalse(marksPages.first().isClassLevelForm)

        // 2. 3-term attendance & traits: Roll, Name + 3*(PRES, ACT, DISC, COND) = 14 cols + shared header strip
        val traitsCols = BlankSheetCatalog.presetColumns(BlankSheetPreset.THREE_TERM_ATTENDANCE_TRAITS)
        assertEquals(14, traitsCols.size)
        assertTrue(BlankSheetCatalog.isThreeTermAttendanceTraitsActive(traitsCols))
        assertFalse(traitsCols.any { it.id.endsWith("_DAYS") })

        val sampleWithDays = listOf(
            Student(id = "s1", cls = 4, roll = 1, name = "তানভীর", x = mapOf("DAYS_1" to "62", "DAYS_2" to "65")),
            Student(id = "s2", cls = 4, roll = 2, name = "নুসরাত", x = mapOf("DAYS_1" to "62"))
        )
        val resolvedCommon = BlankSheetCatalog.resolveCommonWorkingDaysByTerm(sampleWithDays, 4)
        assertEquals("62", resolvedCommon[1])
        assertEquals("65", resolvedCommon[2])

        val traitsPages = BlankSheetPlanner.buildPages(
            students = sampleWithDays,
            settings = settings,
            config = BlankSheetConfig(
                selectedClass = 4,
                subjectKey = BlankSheetCatalog.ALL_SUBJECTS_KEY,
                columns = traitsCols,
                workingDaysByTerm = resolvedCommon
            ),
            allowedClassIds = listOf(4)
        )
        assertEquals(1, traitsPages.size)
        assertTrue(traitsPages.first().isClassLevelForm)
        assertEquals("উপস্থিতি ও গুণাবলি", traitsPages.first().subjectTitleBn)
        assertEquals("৩ প্রান্তিক", traitsPages.first().termTitleBn)
        assertTrue(traitsPages.first().showSharedWorkingDaysStrip)
        assertEquals(listOf(1, 2, 3), traitsPages.first().sharedWorkingDaysTerms)
        assertEquals("62", traitsPages.first().workingDaysByTerm[1])

        // Adding explicit per-row T1_DAYS column disables duplicate shared strip
        val perRowDaysCols = traitsCols + listOfNotNull(BlankSheetCatalog.byId("T1_DAYS"))
        val perRowPages = BlankSheetPlanner.buildPages(
            students = sampleWithDays,
            settings = settings,
            config = BlankSheetConfig(selectedClass = 4, columns = perRowDaysCols),
            allowedClassIds = listOf(4)
        )
        assertFalse(perRowPages.first().showSharedWorkingDaysStrip)
    }

    @Test
    fun topBannerSharedWorkingDaysParseAndImportAcrossStudents() {
        val csv = """
            শ্রেণি: ৩য়, শিক্ষাবর্ষ: ২০২৬
            মোট কার্যদিবস — ১ম প্রান্তিক: ৬২ দিন | ২য় প্রান্তিক: ৬৫ দিন | ৩য় প্রান্তিক: ৬৪ দিন
            রোল,নাম,১ম প্রান্তিক - উপস্থিতি,১ম প্রান্তিক - সক্রিয়তা,২য় প্রান্তিক - উপস্থিতি
            ১,তানভীর হাসান,৫৯,ক,৬৩
            ২,নুসরাত জাহান,৫৮,খ,৬২
        """.trimIndent()

        val sheet = Repo.parseSmartCsvSheet(csv)
        assertEquals(3, sheet.detectedCls)
        assertEquals("62", sheet.detectedWorkingDaysByTerm[1])
        assertEquals("65", sheet.detectedWorkingDaysByTerm[2])
        assertEquals("64", sheet.detectedWorkingDaysByTerm[3])

        val cols = Repo.detectCsvColumns(sheet.rows, emptyList(), sheet.detectedSubjectKey, sheet.detectedTerm)
        val report = CsvImportAssistant.validateCsvData(
            parsedRows = sheet.rows,
            columns = cols,
            existingStudents = emptyList(),
            fallbackCls = sheet.detectedCls ?: 3,
            sharedWorkingDaysByTerm = sheet.detectedWorkingDaysByTerm
        )
        assertEquals(2, report.validAddCount)
        assertEquals(0, report.warningCount)

        val summary = Repo.applyMappedCsvImport(
            rows = sheet.rows,
            columns = cols,
            existingStudents = emptyList(),
            existingSettings = DEFAULT_SETTINGS,
            defaultCls = sheet.detectedCls ?: 3,
            sharedWorkingDaysByTerm = sheet.detectedWorkingDaysByTerm
        )
        assertEquals(2, summary.students.size)
        val s1 = summary.students.first { it.roll == 1 }
        val s2 = summary.students.first { it.roll == 2 }
        assertEquals("62", s1.x["DAYS_1"])
        assertEquals("65", s1.x["DAYS_2"])
        assertEquals("64", s1.x["DAYS_3"])
        assertEquals("৫৯", s1.x["PRES_1"])
        assertEquals("ক", s1.x["ACT_1"])
        assertEquals("62", s2.x["DAYS_1"])
        assertEquals("65", s2.x["DAYS_2"])
        assertEquals("64", s2.x["DAYS_3"])
    }
}
