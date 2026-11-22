package com.kgps.result

import com.kgps.result.ai.ClassSheetImportScope
import com.kgps.result.ai.ClassSheetKind
import com.kgps.result.ai.FieldConfidence
import com.kgps.result.ai.ScanConstraintSolver
import com.kgps.result.ai.ScannedStudentRow
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
}
