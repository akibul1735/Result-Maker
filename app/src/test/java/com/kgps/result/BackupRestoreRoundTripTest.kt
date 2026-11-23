package com.kgps.result

import com.kgps.result.ai.ClassSheetImportScope
import com.kgps.result.ai.ClassSheetKind
import com.kgps.result.backup.BackupScheduleCalculator
import com.kgps.result.backup.BackupScheduleConfig
import com.kgps.result.backup.BackupScheduleMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class BackupRestoreRoundTripTest {

    @Test
    fun fullBackupBundlePreservesStudentsPostUnionSettingsReportPrefsAndTemplates() {
        val customFields = listOf(
            CustomFieldDef(key = "CF_BIRTH_REG", label = "জন্ম নিবন্ধন নম্বর")
        )
        val customClasses = listOf(
            CustomClassDef(id = 6, nameBn = "ষষ্ঠ শ্রেণি", shortBn = "ষষ্ঠ", syllabusTier = 3)
        )
        val settings = Settings(
            school = "করিমপুর সরকারি প্রাথমিক বিদ্যালয়",
            schoolAddress = "ডাকঘর: করিমপুর, উপজেলা: সদর",
            year = "২০২৬",
            teachers = mapOf(1 to "আবদুল করিম", 6 to "ফাতেমা বেগম"),
            teacherMobiles = mapOf(1 to "01711000001", 6 to "01811000002"),
            pdfFont = "solaiman_lipi",
            pdfEnglishFont = "roboto",
            dpeLogo = "color",
            customFields = customFields,
            customClasses = customClasses
        )
        val students = listOf(
            Student(
                id = "stu-1",
                cls = 1,
                roll = 1,
                name = "রাফি আহমেদ",
                guardian = "শফিক আহমেদ",
                village = "দক্ষিণপাড়া",
                union = "রামপুর",
                post = "করিমপুর-৪২০০",
                upazila = "সদর",
                district = "যশোর",
                mobile = "01712345678",
                religion = "ইসলাম",
                m = mapOf("BAN_1_TB" to 8.5, "BAN_1_WR" to 42.0, "ENG_1_TOTAL" to 88.5),
                x = mapOf(
                    "DAYS_1" to "58",
                    "PRES_1" to "55",
                    "REM_1" to "খুব ভালো অগ্রগতি",
                    "CF_BIRTH_REG" to "20181234567890123"
                )
            ),
            Student(
                id = "stu-6",
                cls = 6,
                roll = 12,
                name = "তানিয়া খাতুন",
                guardian = "রহিম উদ্দিন",
                village = "উত্তরপাড়া",
                union = "চন্দ্রপুর",
                post = "নবীনগর",
                upazila = "সদর",
                district = "যশোর",
                mobile = "01911223344",
                religion = "সকল",
                m = mapOf("MATH_2_WR" to 49.0, "SCI_2_TB" to 9.0),
                x = mapOf("ACT_2" to "A+")
            )
        )
        val reportPrefs = ReportBackupPrefs(
            includeClassHighest = true,
            includeSummaryTable = true,
            summaryTableColumns = listOf(
                ReportSummaryColumn.PERCENTAGE,
                ReportSummaryColumn.MERIT_RANK
            ),
            showCoverAddress = false
        )
        val template = BlankSheetQrMeta(
            version = 2,
            templateId = "TMPL001",
            cls = 6,
            subjectKey = "MATH",
            term = 2,
            sheetKind = ClassSheetKind.SINGLE_TERM_MARKS,
            importScope = ClassSheetImportScope.BOTH,
            columnIds = listOf("TB", "ACT", "WR"),
            customColumnTitles = listOf("বই", "কাজ", "লিখিত"),
            numeralStyle = BlankNumeralStyle.ENGLISH,
            showSubjectMaxInHeader = false,
            workingDaysByTerm = mapOf(1 to "55", 2 to "62"),
            pageIndex = 1,
            totalPages = 2,
            refCode = "C6-MATH-T2"
        )

        val json = Repo.toJson(
            list = students,
            st = settings,
            reportPrefs = reportPrefs,
            sheetTemplates = listOf(template)
        )
        val restored = Repo.fromBackupJson(json)

        assertEquals(settings, restored.settings)
        assertEquals(2, restored.students.size)

        val r1 = restored.students[0]
        assertEquals("stu-1", r1.id)
        assertEquals("রামপুর", r1.union)
        assertEquals("করিমপুর-৪২০০", r1.post)
        assertEquals("20181234567890123", r1.x["CF_BIRTH_REG"])
        assertEquals(8.5, r1.m["BAN_1_TB"]!!, 0.001)
        assertEquals(88.5, r1.m["ENG_1_TOTAL"]!!, 0.001)

        val r6 = restored.students[1]
        assertEquals(6, r6.cls)
        assertEquals("নবীনগর", r6.post)
        assertEquals("A+", r6.x["ACT_2"])

        assertEquals(reportPrefs, restored.reportPrefs)
        assertEquals(1, restored.sheetTemplates.size)
        val rt = restored.sheetTemplates.first()
        assertEquals("TMPL001", rt.templateId)
        assertEquals(6, rt.cls)
        assertEquals("MATH", rt.subjectKey)
        assertEquals(listOf("TB", "ACT", "WR"), rt.columnIds)
        assertEquals(BlankNumeralStyle.ENGLISH, rt.numeralStyle)
        assertEquals(mapOf(1 to "55", 2 to "62"), rt.workingDaysByTerm)
    }

    @Test
    fun legacyV1JsonWithoutPostReportPrefsOrTemplatesStillLoadsAndDeduplicatesIds() {
        val v1Json = """
            {
              "version": 1,
              "school": "নমুনা বিদ্যালয়",
              "year": "২০২৫",
              "students": [
                {
                  "id": "dup-id",
                  "cls": 2,
                  "roll": 1,
                  "name": "প্রথম শিক্ষার্থী",
                  "x": { "UNION": "ইউনিয়ন ক", "DAYS_1": "60" },
                  "m": { "BAN_1_WR": 40.0 }
                },
                {
                  "id": "dup-id",
                  "cls": 2,
                  "roll": 2,
                  "name": "দ্বিতীয় শিক্ষার্থী",
                  "m": { "BAN_1_WR": 38.0 }
                },
                {
                  "id": "",
                  "cls": 2,
                  "roll": 3,
                  "name": "তৃতীয় শিক্ষার্থী"
                }
              ]
            }
        """.trimIndent()

        val bundle = Repo.fromBackupJson(v1Json)
        assertNull(bundle.reportPrefs)
        assertTrue(bundle.sheetTemplates.isEmpty())
        assertEquals(3, bundle.students.size)

        assertEquals("ইউনিয়ন ক", bundle.students[0].union)
        assertFalse(bundle.students[0].x.containsKey("UNION"))
        assertEquals("", bundle.students[0].post)

        val ids = bundle.students.map { it.id }
        assertTrue(ids.all { it.isNotBlank() })
        assertEquals(3, ids.toSet().size)
    }

    @Test
    fun backupScheduleCalculatorComputesDueSlotsAccurately() {
        val anchor = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 15, 22, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val dailyAt21 = BackupScheduleConfig(mode = BackupScheduleMode.DAILY, hour = 21, minute = 0)
        val before21 = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 15, 20, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val after21SameDay = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 15, 21, 15, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertTrue(BackupScheduleCalculator.isScheduledDue(dailyAt21, before21, anchor))
        assertFalse(BackupScheduleCalculator.isScheduledDue(dailyAt21, after21SameDay, anchor))

        val sixHourly = BackupScheduleConfig(mode = BackupScheduleMode.EVERY_6_HOURS, hour = 0, minute = 0)
        val sevenHoursAgo = anchor - 7L * 3_600_000L
        assertTrue(BackupScheduleCalculator.isScheduledDue(sixHourly, sevenHoursAgo, anchor))
        assertNotNull(BackupScheduleCalculator.formatTime12hBn(21, 5))
    }
}
