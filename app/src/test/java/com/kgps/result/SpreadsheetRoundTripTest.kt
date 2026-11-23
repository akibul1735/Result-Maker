package com.kgps.result

import com.kgps.result.backup.XlsxExporter
import com.kgps.result.backup.XlsxImporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SpreadsheetRoundTripTest {

    @Test
    fun xlsxExportAndImportRoundTripsAllClassesAndCustomClassWithoutDataLoss() {
        val customField = CustomFieldDef(key = "CF_NID", label = "জন্ম নিবন্ধন")
        val settings = Settings(
            school = "উদয়ন সরকারি প্রাথমিক বিদ্যালয়",
            year = "২০২৬",
            customFields = listOf(customField),
            customClasses = listOf(
                CustomClassDef(id = 6, nameBn = "ষষ্ঠ শ্রেণি", shortBn = "ষষ্ঠ", syllabusTier = 3)
            )
        )
        SchoolClassRegistry.update(settings.customClasses)

        val originalStudents = listOf(
            Student(
                id = "s-cls1",
                cls = 1,
                roll = 1,
                name = "নুসরাত জাহান",
                guardian = "জাহাঙ্গীর আলম",
                village = "নয়ানগর",
                union = "শিমুলিয়া",
                post = "পোস্ট-১০১",
                upazila = "সাভার",
                district = "ঢাকা",
                mobile = "01700001111",
                religion = "ইসলাম",
                m = mapOf(
                    "BAN_1_TB" to 8.5,
                    "BAN_1_ACT" to 9.0,
                    "BAN_1_LANG" to 8.0,
                    "BAN_1_CT" to 18.0,
                    "BAN_1_WR" to 44.0,
                    "SOC_1_CONT" to 42.0,
                    "ART_1_TOTAL" to 85.0
                ),
                x = mapOf(
                    "DAYS_1" to "60",
                    "PRES_1" to "58",
                    "ACT_1" to "A+",
                    "REM_1" to "অসাধারণ",
                    "CF_NID" to "20171234567890123"
                )
            ),
            Student(
                id = "s-cls6",
                cls = 6,
                roll = 4,
                name = "মাহমুদুল হাসান",
                guardian = "নজরুল ইসলাম",
                village = "পূর্বপাড়া",
                union = "আমতলী",
                post = "পোস্ট-২০২",
                mobile = "01812345678",
                religion = "ইসলাম",
                m = mapOf(
                    "MATH_2_WR" to 48.0,
                    "SCI_2_TB" to 9.5,
                    "SCI_2_WR" to 41.0
                ),
                x = mapOf(
                    "DAYS_2" to "54",
                    "PRES_2" to "50",
                    "CF_NID" to "20139876543210987"
                )
            )
        )

        val workbookBytes = XlsxExporter.exportAllClassesWorkbook(
            students = originalStudents,
            customFields = settings.customFields,
            style = CsvHeaderStyle.FRIENDLY_BN,
            classes = listOf(1, 6)
        )
        assertTrue(XlsxImporter.isXlsxBytes(workbookBytes))
        assertFalse(XlsxImporter.isLegacyXlsBytes(workbookBytes))

        val combinedCsv = XlsxImporter.importWorkbookAsCsvText(workbookBytes)
        val parsed = Repo.parseSmartCsvSheet(combinedCsv)
        val cols = Repo.detectCsvColumns(
            rows = parsed.rows,
            existingCustomFields = settings.customFields,
            contextSubjectKey = parsed.detectedSubjectKey,
            contextTerm = parsed.detectedTerm
        )

        // Start with existing students containing base post metadata & verify round-trip merges cleanly
        val seedWithPostOnly = originalStudents.map { it.copy(m = emptyMap(), x = emptyMap()) }
        val summary = Repo.applyMappedCsvImport(
            rows = parsed.rows,
            columns = cols,
            existingStudents = seedWithPostOnly,
            existingSettings = settings
        )

        assertEquals(2, summary.students.size)

        val imp1 = summary.students.first { it.cls == 1 && it.roll == 1 }
        assertEquals("01700001111", imp1.mobile)
        assertEquals("20171234567890123", imp1.x["CF_NID"])
        assertEquals("পোস্ট-১০১", imp1.post)
        assertEquals("শিমুলিয়া", imp1.union)
        assertEquals(8.5, imp1.m["BAN_1_TB"]!!, 0.001)
        assertEquals(44.0, imp1.m["BAN_1_WR"]!!, 0.001)
        // Must NOT duplicate calculated CONT / TOTAL when granular TB..CT marks exist
        assertFalse(imp1.m.containsKey("BAN_1_CONT"))
        assertFalse(imp1.m.containsKey("BAN_1_TOTAL"))
        // SOC continuous total & ART total survive round-trip cleanly
        assertEquals(42.0, imp1.m["SOC_1_CONT"]!!, 0.001)
        assertEquals(85.0, imp1.m["ART_1_TOTAL"]!!, 0.001)

        val imp6 = summary.students.first { it.cls == 6 && it.roll == 4 }
        assertEquals("01812345678", imp6.mobile)
        assertEquals("20139876543210987", imp6.x["CF_NID"])
        assertEquals("পোস্ট-২০২", imp6.post)
        assertEquals(48.0, imp6.m["MATH_2_WR"]!!, 0.001)
        assertFalse(imp6.m.containsKey("MATH_2_TOTAL"))
    }

    @Test
    fun xlsxImporterReadsSharedStringsRichTextAndSparseCellsWithTabClassInference() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            fun add(name: String, xml: String) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(xml.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            add(
                "xl/workbook.xml",
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets>
                    <sheet name="চতুর্থ শ্রেণি" sheetId="1" r:id="rId1"/>
                  </sheets>
                </workbook>
                """.trimIndent()
            )
            add(
                "xl/_rels/workbook.xml.rels",
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                </Relationships>
                """.trimIndent()
            )
            add(
                "xl/sharedStrings.xml",
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="2" uniqueCount="2">
                  <si><t>রোল নম্বর</t></si>
                  <si><r><t>তানভীর </t></r><r><t>আহমেদ</t></r><rPh sb="0" eb="2"><t>ignored</t></rPh></si>
                </sst>
                """.trimIndent()
            )
            add(
                "xl/worksheets/sheet1.xml",
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>
                    <row r="1">
                      <c r="A1" t="s"><v>0</v></c>
                      <c r="B1" t="inlineStr"><is><t>শিক্ষার্থীর নাম</t></is></c>
                      <c r="D1" t="inlineStr"><is><t>MATH_1_WR</t></is></c>
                    </row>
                    <row r="2">
                      <c r="A2" t="n"><v>7</v></c>
                      <c r="B2" t="s"><v>1</v></c>
                      <c r="D2" t="n"><v>45</v></c>
                    </row>
                  </sheetData>
                </worksheet>
                """.trimIndent()
            )
        }

        val csv = XlsxImporter.importWorkbookAsCsvText(baos.toByteArray())
        val parsed = Repo.parseSmartCsvSheet(csv)
        val cols = Repo.detectCsvColumns(parsed.rows, emptyList())
        val summary = Repo.applyMappedCsvImport(
            rows = parsed.rows,
            columns = cols,
            existingStudents = emptyList(),
            existingSettings = DEFAULT_SETTINGS
        )

        assertEquals(1, summary.students.size)
        val student = summary.students.first()
        assertEquals(4, student.cls)
        assertEquals(7, student.roll)
        assertEquals("তানভীর আহমেদ", student.name)
        assertEquals(45.0, student.m["MATH_1_WR"]!!, 0.001)
    }

    @Test
    fun standaloneTotalOnContinuousOnlyNonArtSubjectMapsToContForFirestoreParity() {
        val csv = """
            cls,roll,name,SOC_1_TOTAL,ART_1_TOTAL
            1,1,সুমাইয়া,46,88
        """.trimIndent()
        val parsed = Repo.parseSmartCsvSheet(csv)
        val cols = Repo.detectCsvColumns(parsed.rows, emptyList())
        val summary = Repo.applyMappedCsvImport(
            rows = parsed.rows,
            columns = cols,
            existingStudents = emptyList(),
            existingSettings = DEFAULT_SETTINGS
        )

        val s = summary.students.single()
        assertEquals(46.0, s.m["SOC_1_CONT"]!!, 0.001)
        assertFalse(s.m.containsKey("SOC_1_TOTAL"))
        assertEquals(88.0, s.m["ART_1_TOTAL"]!!, 0.001)
    }
}
