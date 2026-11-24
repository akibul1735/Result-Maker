package com.kgps.result

import com.kgps.result.ai.AutoScanResult
import com.kgps.result.ai.ClassSheetImportScope
import com.kgps.result.ai.ClassSheetKind
import com.kgps.result.ai.FieldConfidence
import com.kgps.result.ai.ScanConstraintSolver
import com.kgps.result.ai.ScannedStudentRow
import com.kgps.result.ai.normalizeDetectedSubjectKey
import com.kgps.result.ai.parseAutoScanJson
import com.kgps.result.ai.validateScannedStudentRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlankSheetAndAiImportFlowTest {

    @Test
    fun blankSheetRefCodeAndExternalAiPromptCoverAllLayouts() {
        val fullCols = BlankSheetCatalog.presetColumns(BlankSheetPreset.FULL_MARKS)
        val threeTermCols = BlankSheetCatalog.presetColumns(BlankSheetPreset.THREE_TERM_MARKS)
        val traitsCols = BlankSheetCatalog.presetColumns(BlankSheetPreset.THREE_TERM_ATTENDANCE_TRAITS)
        val totalCols = BlankSheetCatalog.presetColumns(BlankSheetPreset.ROLL_NAME_TOTAL)

        assertEquals("REF:C3-BAN-T1-FULL", BlankSheetCatalog.buildSheetRefCode(3, "BAN", 1, fullCols))
        assertEquals("REF:C4-MATH-3T-3MARKS", BlankSheetCatalog.buildSheetRefCode(4, "MATH", 1, threeTermCols))
        assertEquals("REF:C5-ATT-3T-TRAITS", BlankSheetCatalog.buildSheetRefCode(5, null, 1, traitsCols))
        assertEquals("REF:C2-ENG-T2-TOTAL", BlankSheetCatalog.buildSheetRefCode(2, "ENG", 2, totalCols))

        val prompt = BlankSheetCatalog.buildExternalAiCsvPrompt(
            config = BlankSheetConfig(
                selectedClass = 3,
                subjectKey = "SCI",
                term = 2,
                columns = fullCols
            ),
            settings = Settings(school = "কাজীপাড়া সপ্রাবি"),
            allowedClassIds = listOf(1, 2, 3, 4, 5)
        )
        assertTrue(prompt.contains("বিজ্ঞান (SCI)"))
        assertTrue(prompt.contains("২য় প্রান্তিক"))
        assertTrue(prompt.contains("শ্রেণি,রোল নম্বর,শিক্ষার্থীর নাম,পাঠ্যবই,কাজ,ভাষা,অভীক্ষা,ধারাবাহিক,লিখিত,মোট"))

        // Also check page spec refCode population in BlankSheetPlanner
        val pages = BlankSheetPlanner.buildPages(
            students = listOf(Student(id = "s1", cls = 3, roll = 1, name = "রাফি")),
            settings = Settings(),
            config = BlankSheetConfig(
                selectedClass = 3,
                subjectKey = "SCI",
                term = 2,
                columns = fullCols
            ),
            allowedClassIds = listOf(1, 2, 3)
        )
        assertEquals("REF:C3-SCI-T2-FULL", pages.first().sheetRefCode)
    }

    @Test
    fun csvParserReadsRefCodeBlankSheetHeadersAndTraitsCorrectly() {
        val marksCsv = """
            "কাজীপাড়া সপ্রাবি — শ্রেণি: ৩য় | বিষয়: বাংলা | প্রান্তিক: ২য় | REF:C3-BAN-T2-FULL"
            "রোল","নাম","পাঠ্যবই","কাজ","ভাষা","অভীক্ষা","ধারাবাহিক","লিখিত","মোট","গ্রেড","শিক্ষকের স্বাক্ষর"
            1,"রাফি",8,8,4,8,28,60,88,"A",""
        """.trimIndent()

        val parsed = Repo.parseSmartCsvSheet(marksCsv)
        assertEquals(3, parsed.detectedCls)
        assertEquals("BAN", parsed.detectedSubjectKey)
        assertEquals(2, parsed.detectedTerm)

        val cols = Repo.detectCsvColumns(
            rows = parsed.rows,
            existingCustomFields = emptyList(),
            contextSubjectKey = parsed.detectedSubjectKey,
            contextTerm = parsed.detectedTerm
        )
        assertEquals("mark" to "BAN_2_TB", cols[2].targetType to cols[2].targetKey)
        assertEquals("mark" to "BAN_2_ACT", cols[3].targetType to cols[3].targetKey)
        assertEquals("mark" to "BAN_2_LANG", cols[4].targetType to cols[4].targetKey)
        assertEquals("mark" to "BAN_2_CT", cols[5].targetType to cols[5].targetKey)
        assertEquals("mark" to "BAN_2_CONT", cols[6].targetType to cols[6].targetKey)
        assertEquals("mark" to "BAN_2_WR", cols[7].targetType to cols[7].targetKey)
        assertEquals("mark" to "BAN_2_TOTAL", cols[8].targetType to cols[8].targetKey)
        assertEquals("skip", cols[9].targetType)
        assertEquals("skip", cols[10].targetType)

        // Attendance & traits sheet: "সক্রিয়তা" must map to ACT_t (not BAN_t_ACT)
        val traitsCsv = """
            "REF:C4-ATT-3T-TRAITS | মোট কার্যদিবস — ১ম প্রান্তিক: ৬০ দিন"
            "রোল","নাম","১ম উপস্থিতি","১ম সক্রিয়তা","১ম শৃঙ্খলা","১ম আচরণ"
            1,"সাদিয়া",56,"ক","ক","খ"
        """.trimIndent()
        val parsedTraits = Repo.parseSmartCsvSheet(traitsCsv)
        assertEquals(4, parsedTraits.detectedCls)
        assertEquals("60", parsedTraits.detectedWorkingDaysByTerm[1])
        val traitCols = Repo.detectCsvColumns(
            rows = parsedTraits.rows,
            existingCustomFields = emptyList(),
            contextSubjectKey = "BAN",
            contextTerm = 1
        )
        assertEquals("text" to "PRES_1", traitCols[2].targetType to traitCols[2].targetKey)
        assertEquals("text" to "ACT_1", traitCols[3].targetType to traitCols[3].targetKey)
        assertEquals("text" to "DISC_1", traitCols[4].targetType to traitCols[4].targetKey)
        assertEquals("text" to "COND_1", traitCols[5].targetType to traitCols[5].targetKey)
    }

    @Test
    fun solverSupportsGrandTotalOnlyThreeTermAndAttendanceTraits() {
        // 1. ROLL_NAME_TOTAL (grand total only)
        val totalOnlyRow = ScannedStudentRow(
            rollText = "1",
            name = "মিতু",
            marksText = emptyMap(),
            sheetGrandTotal = "84"
        )
        val reconciledTotal = ScanConstraintSolver.reconcileInterdependencies(
            row = totalOnlyRow,
            cls = 3,
            subKey = "BAN",
            scope = ClassSheetImportScope.BOTH,
            sheetKind = ClassSheetKind.SINGLE_TERM_MARKS
        )
        val totalReport = validateScannedStudentRow(
            row = reconciledTotal,
            cls = 3,
            subKey = "BAN",
            duplicateRolls = emptySet(),
            sheetKind = ClassSheetKind.SINGLE_TERM_MARKS
        )
        assertTrue(totalReport.isGrandTotalOnlyMode)
        assertFalse(totalReport.hasAnyIssue)
        assertEquals(FieldConfidence.SURE, totalReport.fieldConfidences["GRAND"])
        assertEquals(FieldConfidence.NEUTRAL, totalReport.fieldConfidences["CONT"])
        assertEquals(84.0, totalReport.calculatedGrandTotal!!, 0.01)

        // 2. THREE_TERM_MARKS deduction + mismatch + blank Term 3 stays NEUTRAL
        val threeTermRow = ScannedStudentRow(
            rollText = "2",
            name = "তানভীর",
            marksText = mapOf(
                "T1_CONT" to "25",
                "T1_WR" to "60",
                "T2_CONT" to "24",
                "T2_WR" to "55",
                "T2_TOTAL" to "80" // mismatch: 24+55=79 != 80
            )
        )
        val reconciled3 = ScanConstraintSolver.reconcileInterdependencies(
            row = threeTermRow,
            cls = 3,
            subKey = "BAN",
            sheetKind = ClassSheetKind.THREE_TERM_MARKS
        )
        assertEquals("85", reconciled3.marksText["T1_TOTAL"])
        val report3 = validateScannedStudentRow(
            row = reconciled3,
            cls = 3,
            subKey = "BAN",
            duplicateRolls = emptySet(),
            sheetKind = ClassSheetKind.THREE_TERM_MARKS
        )
        assertTrue(report3.mismatchFields.containsAll(listOf("T2_CONT", "T2_WR", "T2_TOTAL")))
        assertEquals(FieldConfidence.SURE, report3.fieldConfidences["T1_TOTAL"])
        assertEquals(FieldConfidence.NEUTRAL, report3.fieldConfidences["T3_CONT"])
        assertEquals(FieldConfidence.NEUTRAL, report3.fieldConfidences["T3_TOTAL"])

        // 3. ATTENDANCE_TRAITS normalization + working day check
        val traitRow = ScannedStudentRow(
            rollText = "3",
            name = "নাফিস",
            marksText = emptyMap(),
            traitValues = mapOf(
                "PRES_1" to "65",
                "ACT_1" to "উত্তম",
                "DISC_1" to "খ"
            )
        )
        val reconciledTrait = ScanConstraintSolver.reconcileInterdependencies(
            row = traitRow,
            cls = 4,
            subKey = "BAN",
            sheetKind = ClassSheetKind.ATTENDANCE_TRAITS
        )
        assertEquals("ক", reconciledTrait.traitValues["ACT_1"])
        val traitReport = validateScannedStudentRow(
            row = reconciledTrait,
            cls = 4,
            subKey = "BAN",
            duplicateRolls = emptySet(),
            sheetKind = ClassSheetKind.ATTENDANCE_TRAITS,
            sharedWorkingDaysByTerm = mapOf(1 to "60")
        )
        assertTrue(traitReport.hasHardError)
        assertTrue(traitReport.hardErrors.containsKey("PRES_1"))
    }

    @Test
    fun autoScanParserDetectsModeClassTermSubjectAndKindFromImageJson() {
        assertEquals("BAN", normalizeDetectedSubjectKey("বাংলা", cls = 3))
        assertEquals("MATH", normalizeDetectedSubjectKey("গণিত", cls = 4))
        assertEquals("BGW", normalizeDetectedSubjectKey("বাওবি", cls = 5))
        assertEquals("SCI", normalizeDetectedSubjectKey("প্রাথমিক বিজ্ঞান", cls = 3))
        assertEquals("SOC", normalizeDetectedSubjectKey("বিজ্ঞান", cls = 2))

        val classSheetJson = """
            {
              "scanMode": "CLASS",
              "sheetKind": "THREE_TERM_MARKS",
              "cls": 4,
              "term": 2,
              "subKey": "গণিত",
              "rows": [
                {
                  "roll": "০১",
                  "name": "তাসনিম",
                  "marks": {"T1_CONT": "25", "T1_WR": "60", "T2_CONT": "26", "T2_WR": "62"}
                }
              ]
            }
        """.trimIndent()

        val classParsed = parseAutoScanJson(
            raw = classSheetJson,
            clsHint = null,
            termHint = null,
            subKeyHint = null,
            requestedScope = ClassSheetImportScope.AUTO,
            sheetKindHint = null
        )
        assertTrue(classParsed is AutoScanResult.ClassSheet)
        val sheet = (classParsed as AutoScanResult.ClassSheet).sheet
        assertEquals(4, sheet.cls)
        assertEquals(2, sheet.term)
        assertEquals("MATH", sheet.subKey)
        assertEquals(ClassSheetKind.THREE_TERM_MARKS, sheet.sheetKind)
        assertEquals(1, sheet.rows.size)
        assertEquals("01", sheet.rows.first().rollText)
        assertEquals(1, sheet.rows.first().rollText.toIntOrNull())

        val traitsJson = """
            {
              "scanMode": "CLASS",
              "sheetKind": "ATTENDANCE_TRAITS",
              "cls": 5,
              "term": 3,
              "sharedWorkingDays": {"3": "65"},
              "rows": [
                {
                  "roll": "2",
                  "name": "রাফিদ",
                  "traits": {"PRES_3": "60", "ACT_3": "ক"}
                }
              ]
            }
        """.trimIndent()
        val traitsParsed = parseAutoScanJson(
            raw = traitsJson,
            clsHint = null,
            termHint = null,
            subKeyHint = null
        ) as AutoScanResult.ClassSheet
        assertEquals(ClassSheetKind.ATTENDANCE_TRAITS, traitsParsed.sheet.sheetKind)
        assertEquals(5, traitsParsed.sheet.cls)
        assertEquals("65", traitsParsed.sheet.sharedWorkingDaysByTerm[3])

        val singleJson = """
            {
              "scanMode": "SINGLE",
              "term": 2,
              "student": {"name": "মালিহা", "cls": 3, "roll": 5},
              "marks": [
                {"key": "BAN_2_TB", "value": 9.0, "subjectLabel": "বাংলা (পাঠ্যবই)"}
              ]
            }
        """.trimIndent()
        val singleParsed = parseAutoScanJson(
            raw = singleJson,
            clsHint = null,
            termHint = null,
            subKeyHint = null
        )
        assertTrue(singleParsed is AutoScanResult.SingleStudent)
        val ms = (singleParsed as AutoScanResult.SingleStudent).marksheet
        assertEquals(2, ms.term)
        assertEquals(3, ms.student.cls)
        assertEquals(5, ms.student.roll)
        assertEquals("BAN_2_TB", ms.marks.first().key)
    }

    @Test
    fun singleTermAttendanceAndTraitsWorkAcrossBlankSheetQrCsvAndInternalAi() {
        val singleTraitCols = BlankSheetCatalog.presetColumns(BlankSheetPreset.SINGLE_TERM_ATTENDANCE_TRAITS)
        assertEquals(
            listOf("ROLL", "NAME", "ATT", "TRAIT_ACT", "TRAIT_DISC", "TRAIT_COND"),
            singleTraitCols.map { it.id }
        )
        assertEquals("REF:C3-ATT-T2-TRAITS", BlankSheetCatalog.buildSheetRefCode(3, "BAN", 2, singleTraitCols))

        val pages = BlankSheetPlanner.buildPages(
            students = listOf(Student(id = "s1", cls = 3, roll = 1, name = "রাফি")),
            settings = Settings(school = "কাজীপাড়া সপ্রাবি"),
            config = BlankSheetConfig(
                selectedClass = 3,
                subjectKey = "BAN",
                term = 2,
                columns = singleTraitCols,
                workingDaysByTerm = mapOf(2 to "65")
            ),
            allowedClassIds = listOf(3)
        )
        assertEquals(1, pages.size)
        val page = pages.first()
        assertTrue(page.isClassLevelForm)
        assertEquals("উপস্থিতি ও গুণাবলি", page.subjectTitleBn)
        assertEquals("২য় প্রান্তিক", page.termTitleBn)
        assertTrue(page.showSharedWorkingDaysStrip)
        assertEquals(listOf(2), page.sharedWorkingDaysTerms)
        assertEquals("REF:C3-ATT-T2-TRAITS", page.sheetRefCode)

        val meta = SheetQrCodec.fromPageConfig(page = page, term = 2)
        assertEquals(ClassSheetKind.ATTENDANCE_TRAITS, meta.sheetKind)
        assertEquals(2, meta.term)
        val qrDecoded = SheetQrCodec.decodePayload(SheetQrCodec.encodePayload(meta))
        assertEquals(ClassSheetKind.ATTENDANCE_TRAITS, qrDecoded?.sheetKind)
        assertEquals(2, qrDecoded?.term)
        assertEquals(singleTraitCols.map { it.id }, qrDecoded?.columnIds)
        val refDecoded = SheetQrCodec.decodePayload("REF:C3-ATT-T2-TRAITS")
        assertEquals(ClassSheetKind.ATTENDANCE_TRAITS, refDecoded?.sheetKind)
        assertEquals(2, refDecoded?.term)

        val externalPrompt = BlankSheetCatalog.buildExternalAiCsvPrompt(
            config = BlankSheetConfig(
                selectedClass = 3,
                subjectKey = "BAN",
                term = 2,
                columns = singleTraitCols,
                workingDaysByTerm = mapOf(2 to "65")
            ),
            settings = Settings(school = "কাজীপাড়া সপ্রাবি"),
            allowedClassIds = listOf(3)
        )
        assertTrue(externalPrompt.contains("উপস্থিতি ও গুণাবলি"))
        assertTrue(externalPrompt.contains("২য় প্রান্তিক"))
        assertTrue(externalPrompt.contains("সক্রিয়তা"))
        assertTrue(externalPrompt.contains("শৃঙ্খলা"))
        assertTrue(externalPrompt.contains("আচরণ"))

        // Bare header CSV with Term 2 context, Bengali day suffix, grade synonyms, and dash placeholder
        val bareTraitsCsv = """
            রোল,নাম,উপস্থিত,সক্রিয়তা,শৃঙ্খলা,আচরণ
            ১,রাফি,৫৬ দিন,উত্তম,খ,-
        """.trimIndent()
        val parsedCsv = Repo.parseSmartCsvSheet(bareTraitsCsv)
        val mappedCols = Repo.detectCsvColumns(
            rows = parsedCsv.rows,
            existingCustomFields = emptyList(),
            contextSubjectKey = "BAN",
            contextTerm = 2
        )
        assertEquals("text" to "PRES_2", mappedCols[2].targetType to mappedCols[2].targetKey)
        assertEquals("text" to "ACT_2", mappedCols[3].targetType to mappedCols[3].targetKey)
        assertEquals("text" to "DISC_2", mappedCols[4].targetType to mappedCols[4].targetKey)
        assertEquals("text" to "COND_2", mappedCols[5].targetType to mappedCols[5].targetKey)

        val csvValidation = CsvImportAssistant.validateCsvData(
            parsedRows = parsedCsv.rows,
            columns = mappedCols,
            existingStudents = emptyList(),
            fallbackCls = 3,
            sharedWorkingDaysByTerm = mapOf(2 to "65")
        )
        assertEquals(1, csvValidation.validAddCount)
        assertEquals(0, csvValidation.warningCount)

        val imported = Repo.applyMappedCsvImport(
            rows = parsedCsv.rows,
            columns = mappedCols,
            existingStudents = emptyList(),
            existingSettings = Settings(),
            defaultCls = 3,
            sharedWorkingDaysByTerm = mapOf(2 to "65")
        )
        val student = imported.students.single()
        assertEquals("65", student.x["DAYS_2"])
        assertEquals("৫৬", student.x["PRES_2"])
        assertEquals("ক", student.x["ACT_2"])
        assertEquals("খ", student.x["DISC_2"])
        assertFalse(student.x.containsKey("COND_2"))

        // Internal AI JSON parser handles single-term ATT/TRAIT_ACT keys even if misplaced in marks
        val aiSingleTraitsJson = """
            {
              "scanMode": "CLASS",
              "sheetKind": "ATTENDANCE_TRAITS",
              "refCode": "REF:C3-ATT-T2-TRAITS",
              "cls": 3,
              "term": 2,
              "sharedWorkingDays": {"2": "65"},
              "rows": [
                {
                  "roll": "1",
                  "name": "রাফি",
                  "marks": {"ATT": "56"},
                  "traits": {"TRAIT_ACT": "উত্তম", "TRAIT_DISC": "b", "আচরণ": "গ"}
                }
              ]
            }
        """.trimIndent()
        val aiParsed = parseAutoScanJson(
            raw = aiSingleTraitsJson,
            clsHint = null,
            termHint = null,
            subKeyHint = null
        ) as AutoScanResult.ClassSheet
        assertEquals(ClassSheetKind.ATTENDANCE_TRAITS, aiParsed.sheet.sheetKind)
        assertEquals(2, aiParsed.sheet.term)
        val aiRow = aiParsed.sheet.rows.single()
        assertTrue(aiRow.marksText.isEmpty())
        assertEquals("56", aiRow.traitValues["PRES_2"])
        assertEquals("ক", aiRow.traitValues["ACT_2"])
        assertEquals("খ", aiRow.traitValues["DISC_2"])
        assertEquals("গ", aiRow.traitValues["COND_2"])
    }

    @Test
    fun threeQrBlankSheetPromptSchemaAndJsonParserCoordinateCleanly() {
        // 1. Compact blank sheet with ALL_SUBJECTS_KEY & term=0 should not clamp term or leak "ALL" as subject
        val compactMeta = BlankSheetQrMeta(
            templateId = "AB12CD34",
            cls = 3,
            subjectKey = BlankSheetCatalog.ALL_SUBJECTS_KEY,
            term = 0,
            columnIds = listOf("ROLL", "NAME", "CONT", "WR", "TOTAL"),
            importScope = ClassSheetImportScope.BOTH,
            sheetKind = ClassSheetKind.SINGLE_TERM_MARKS,
            refCode = "REF:C3-ALL-T0-COMPACT"
        )
        val compactPrompt = SheetQrCodec.buildAiPromptInstructions(compactMeta)
        assertFalse(compactPrompt.contains("ALL", ignoreCase = false))
        val compactSchema = SheetQrCodec.buildDynamicRowJsonSchema(
            meta = compactMeta,
            kind = ClassSheetKind.SINGLE_TERM_MARKS,
            effCls = 3,
            effTerm = 1,
            effSub = "BAN",
            effScope = ClassSheetImportScope.BOTH
        )
        assertTrue(compactSchema.contains("\"sheetContinuousTotal\""))
        assertTrue(compactSchema.contains("\"sheetGrandTotal\""))
        assertFalse(compactSchema.contains("\"T1_CONT\""))

        // 2. Roll+Name+Total only omits marks/traits noise
        val totalMeta = compactMeta.copy(
            subjectKey = "MATH",
            term = 2,
            columnIds = listOf("ROLL", "NAME", "TOTAL")
        )
        val totalSchema = SheetQrCodec.buildDynamicRowJsonSchema(
            meta = totalMeta,
            kind = ClassSheetKind.SINGLE_TERM_MARKS,
            effCls = 3,
            effTerm = 2,
            effSub = "MATH",
            effScope = ClassSheetImportScope.BOTH
        )
        assertTrue(totalSchema.contains("\"sheetGrandTotal\""))
        assertFalse(totalSchema.contains("\"marks\""))

        // 3. Parser handles misplaced CONT/TOTAL in marks, Bengali keys, "ALL" subject in QR, and "students" key
        val compactJson = """
            {
              "scanMode": "CLASS",
              "sheetKind": "SINGLE_TERM_MARKS",
              "cls": 3,
              "term": 2,
              "subKey": "গণিত",
              "students": [
                {
                  "roll": "৫",
                  "name": "null",
                  "marks": {"ধারাবাহিক": "২৬", "সামষ্টিক": "৬০", "মোট": "৮৬", "গ্রেড": "A"}
                }
              ]
            }
        """.trimIndent()
        val parsedCompact = parseAutoScanJson(
            raw = compactJson,
            clsHint = null,
            termHint = null,
            subKeyHint = null,
            qrMeta = compactMeta.copy(columnIds = listOf("ROLL", "CONT", "WR", "TOTAL"))
        ) as AutoScanResult.ClassSheet
        assertEquals("MATH", parsedCompact.sheet.subKey)
        assertEquals(2, parsedCompact.sheet.term)
        assertEquals(listOf("ROLL", "CONT", "WR", "TOTAL"), parsedCompact.sheet.templateColumnIds)
        val row = parsedCompact.sheet.rows.single()
        assertEquals("5", row.rollText)
        assertEquals("", row.name)
        assertEquals("26", row.sheetContinuousTotal)
        assertEquals("60", row.marksText["WR"])
        assertEquals("86", row.sheetGrandTotal)
        assertFalse(row.marksText.containsKey("GRADE"))
        // NAME column is omitted in template -> empty name must not generate missing-name hard error
        val validation = validateScannedStudentRow(
            row = row,
            cls = 3,
            subKey = "MATH",
            duplicateRolls = emptySet(),
            hasExistingDbName = false,
            scope = parsedCompact.sheet.scope,
            sheetKind = parsedCompact.sheet.sheetKind
        )
        assertFalse(validation.hardErrors.containsKey("name"))
    }
}

