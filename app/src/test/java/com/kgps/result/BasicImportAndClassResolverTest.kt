package com.kgps.result

import com.kgps.result.backup.XlsxImporter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BasicImportAndClassResolverTest {

    @After
    fun resetRegistry() {
        SchoolClassRegistry.update(emptyList())
    }

    @Test
    fun screenshotScenario_prePrimaryAndClassOne_autoCreatesClassAndCustomFieldsWithoutError() {
        val csv = """
            শ্রেণি,রোল,নাম,আইডি,মাতার নাম,জন্মতারিখ,শিক্ষাবর্ষ
            প্রাক-প্রাথমিক,২৬,মো. আলিফ মোল্যা,০২৬,,০১/০১/২০০০,২০২৬
            ১ম,১,মোসাঃ ছিনতিয়া,১০১,মোসাঃ রেশমী বেগম,০১/০১/২০২০,
        """.trimIndent()

        val parsed = Repo.parseSmartCsvSheet(csv)
        val cols = Repo.detectCsvColumns(parsed.rows, emptyList(), profileOnly = true)

        val report = CsvImportAssistant.validateCsvData(
            parsedRows = parsed.rows,
            columns = cols,
            existingStudents = emptyList(),
            fallbackCls = 1,
            customClasses = emptyList()
        )

        assertEquals(0, report.errorCount)
        assertEquals(2, report.validAddCount)
        assertEquals(1, report.autoCreatedClasses.size)
        val createdPre = report.autoCreatedClasses.first()
        assertEquals("প্রাক-প্রাথমিক", createdPre.nameBn)
        assertEquals("প্রাক", createdPre.shortBn)
        assertEquals(1, createdPre.syllabusTier)

        val firstRow = report.rows[0]
        assertTrue(firstRow.isAutoCreatedClass)
        assertEquals(createdPre.id, firstRow.resolvedCls)
        assertEquals("প্রাক", firstRow.resolvedClassLabel)
        assertEquals(26, firstRow.resolvedRoll)

        val secondRow = report.rows[1]
        assertFalse(secondRow.isAutoCreatedClass)
        assertEquals(1, secondRow.resolvedCls)
        assertEquals("১ম", secondRow.resolvedClassLabel)
        assertEquals(1, secondRow.resolvedRoll)

        val summary = CsvImportAssistant.buildFilteredImportSummary(
            parsedRows = parsed.rows,
            columns = cols,
            selectedDataRowIndices = setOf(0, 1),
            existingStudents = emptyList(),
            existingSettings = Settings(),
            fallbackCls = 1
        )!!

        assertEquals(2, summary.addedCount)
        assertEquals(1, summary.createdClasses.size)
        assertEquals(1, summary.settings.customClasses.size)
        assertEquals(listOf("আইডি", "মাতার নাম", "জন্মতারিখ", "শিক্ষাবর্ষ"), summary.createdFields.map { it.label })

        val alif = summary.students.first { it.roll == 26 }
        assertEquals(createdPre.id, alif.cls)
        val idKey = summary.createdFields.first { it.label == "আইডি" }.key
        val dobKey = summary.createdFields.first { it.label == "জন্মতারিখ" }.key
        val yearKey = summary.createdFields.first { it.label == "শিক্ষাবর্ষ" }.key
        assertEquals("০২৬", alif.x[idKey])
        assertEquals("০১/০১/২০০০", alif.x[dobKey])
        assertEquals("২০২৬", alif.x[yearKey])

        val chhintia = summary.students.first { it.roll == 1 }
        assertEquals(1, chhintia.cls)
        val motherKey = summary.createdFields.first { it.label == "মাতার নাম" }.key
        assertEquals("মোসাঃ রেশমী বেগম", chhintia.x[motherKey])
    }

    @Test
    fun classVariantsResolveEquivalentlyAcrossBengaliEnglishOrdinalsAndSections() {
        val c1Variants = listOf("১ম", "1ম", "প্রথম", "1st", "Class 1", "Class One", "I", "১ম (ক)", "Class I - Section B", "প্রথম শ্রেণি")
        c1Variants.forEach { variant ->
            assertEquals("Expected class 1 for '$variant'", 1, Repo.parseClassNum(variant, emptyList()))
        }

        val c2Variants = listOf("২য়", "২য়", "2য়", "2য়", "দ্বিতীয়", "দ্বিতীয়", "2nd", "Class 2 (A)", "Class Two")
        c2Variants.forEach { variant ->
            assertEquals("Expected class 2 for '$variant'", 2, Repo.parseClassNum(variant, emptyList()))
        }

        assertEquals(3, Repo.parseClassNum("তৃতীয়", emptyList()))
        assertEquals(4, Repo.parseClassNum("Class 4", emptyList()))
        assertEquals(5, Repo.parseClassNum("৫ম (খ)", emptyList()))

        // Pre-primary never falsely resolves to 1 despite containing "প্রাথমিক"
        val preVariants = listOf("প্রাক-প্রাথমিক", "প্রাক প্রাথমিক", "শিশু", "শিশু শ্রেণি", "Pre-Primary", "Pre Primary")
        preVariants.forEach { variant ->
            assertNull("Expected unregistered null without customClass for '$variant'", Repo.parseClassNum(variant, emptyList()))
        }

        val existingPre = listOf(CustomClassDef(id = 6, nameBn = "শিশু শ্রেণি", shortBn = "শিশু", syllabusTier = 1))
        preVariants.forEach { variant ->
            assertEquals("Expected custom id 6 for '$variant'", 6, Repo.parseClassNum(variant, existingPre))
        }
    }

    @Test
    fun deduplicatesClassVariantsAndAvoidsIdCollisionAndRejectsGarbage() {
        val rawClasses = listOf(
            "প্রাক-প্রাথমিক",
            "প্রাক প্রাথমিক",
            "Pre-Primary",
            "শিশু শ্রেণি",
            "৬ষ্ঠ",
            "Class 6",
            "01/01/2020",
            "01712345678",
            "2026"
        )
        val existingCustom = listOf(CustomClassDef(id = 6, nameBn = "নার্সারি", shortBn = "নার্সারি", syllabusTier = 1))

        val detected = ClassResolver.detectUnregisteredClasses(rawClasses, existingCustom)
        assertEquals(2, detected.size)
        assertEquals("PRE_PRIMARY", detected[0].canonicalKey)
        assertEquals(4, detected[0].rowCount)
        assertEquals("CLASS_6", detected[1].canonicalKey)
        assertEquals(2, detected[1].rowCount)

        val plan = ClassResolver.planForImport(rawClasses, existingCustom)
        // ID 6 is already taken by Nursery -> Pre-primary gets 7, Class 6 gets 8
        assertEquals(listOf(7, 8), plan.plannedClasses.map { it.id })
        assertEquals(7, plan.resolve("Pre-Primary"))
        assertEquals(7, plan.resolve("প্রাক-প্রাথমিক"))
        assertEquals(8, plan.resolve("Class 6"))
        assertEquals(8, plan.resolve("৬ষ্ঠ"))
        assertNull(plan.resolve("01/01/2020"))
        assertNull(plan.resolve("01712345678"))
        assertNull(plan.resolve("2026"))
        assertTrue(ClassResolver.normalizeLookupKey("01/01/2020") in plan.rejectedRaws)
    }

    @Test
    fun profileOnlyDetection_neverMisroutesRemarksOrBasicColumns_andMapsPostAndExistingCustomFields() {
        val rows = listOf(
            listOf("রোল", "নাম", "ডাকঘর", "মন্তব্য", "কাজ", "জন্ম তারিখ", "মাতার নাম", "শিক্ষাবর্ষ"),
            listOf("1", "আরিফ", "আলফাডাঙ্গা", "ভালো", "খ", "০২/০২/২০১৯", "আয়েশা", "২০২৬")
        )
        val existingCustom = listOf(CustomFieldDef(key = "cf_dob", label = "জন্মতারিখ"))

        val mapped = Repo.detectCsvColumns(rows, existingCustom, profileOnly = true)
        assertEquals("standard" to "roll", mapped[0].targetType to mapped[0].targetKey)
        assertEquals("standard" to "name", mapped[1].targetType to mapped[1].targetKey)
        assertEquals("standard" to "post", mapped[2].targetType to mapped[2].targetKey)
        // Remark and Activity words must become create_new instead of REM_1 / ACT_1
        assertEquals("create_new", mapped[3].targetType)
        assertEquals("মন্তব্য", mapped[3].newLabel)
        assertEquals("create_new", mapped[4].targetType)
        assertEquals("কাজ", mapped[4].newLabel)
        // Space-insensitive custom field match ("জন্ম তারিখ" -> "জন্মতারিখ")
        assertEquals("custom" to "cf_dob", mapped[5].targetType to mapped[5].targetKey)
        assertEquals("create_new", mapped[6].targetType)
        assertEquals("create_new", mapped[7].targetType)
    }

    @Test
    fun firestoreSchemaLimitsAreEnforcedForCustomFieldsAndCustomClasses() {
        val existingFields = (1..28).map { CustomFieldDef("cf_$it", "ফিল্ড_$it") }
        val rows = listOf(
            listOf("শ্রেণি", "রোল", "নাম", "নতুন১", "নতুন২", "নতুন৩", "নতুন৪"),
            listOf("1", "1", "রহিম", "ক", "খ", "গ", "ঘ")
        )
        val cols = Repo.detectCsvColumns(rows, existingFields, profileOnly = true)
        val report = CsvImportAssistant.validateCsvData(
            parsedRows = rows,
            columns = cols,
            existingStudents = emptyList(),
            fallbackCls = 1,
            existingCustomFieldsCount = existingFields.size
        )
        assertTrue(report.customFieldLimitWarning)

        val summary = Repo.applyMappedCsvImport(
            rows = rows,
            columns = cols,
            existingStudents = emptyList(),
            existingSettings = Settings(customFields = existingFields),
            defaultCls = 1
        )
        assertEquals(30, summary.settings.customFields.size)
        assertEquals(2, summary.createdFields.size)

        // Now test 15-custom-class cap (6..20)
        val maxedClasses = (6..20).map { CustomClassDef(id = it, nameBn = "শ্রেণি_$it", shortBn = "শ$it", syllabusTier = 3) }
        val overflowClassRows = listOf(
            listOf("শ্রেণি", "রোল", "নাম"),
            listOf("প্রাক-প্রাথমিক", "1", "করিম")
        )
        val overflowCols = Repo.detectCsvColumns(overflowClassRows, emptyList(), profileOnly = true)
        val overflowReport = CsvImportAssistant.validateCsvData(
            parsedRows = overflowClassRows,
            columns = overflowCols,
            existingStudents = emptyList(),
            fallbackCls = 1,
            customClasses = maxedClasses
        )
        assertEquals(1, overflowReport.errorCount)
        assertEquals(1, overflowReport.classLimitErrorCount)
    }

    @Test
    fun xlsxImporter_infersCustomClassNameFromSheetTabWhenClsColumnMissing() {
        val xlsxBytes = buildMinimalXlsx(
            sheets = listOf(
                "প্রাক-প্রাথমিক" to listOf(
                    listOf("রোল", "নাম"),
                    listOf("১", "মাহি")
                ),
                "নার্সারি" to listOf(
                    listOf("রোল", "নাম"),
                    listOf("২", "তানহা")
                )
            )
        )
        val csvText = XlsxImporter.importWorkbookAsCsvText(xlsxBytes)
        val parsed = Repo.parseSmartCsvSheet(csvText)
        assertEquals(3, parsed.rows.size)
        assertTrue(parsed.rows[0].any { it == "cls" || it.trim() == "শ্রেণি" || it.trim() == "শ্রেণী" })

        val cols = Repo.detectCsvColumns(parsed.rows, emptyList(), profileOnly = true)
        val summary = Repo.applyMappedCsvImport(
            rows = parsed.rows,
            columns = cols,
            existingStudents = emptyList(),
            existingSettings = Settings(),
            defaultCls = 1
        )
        assertEquals(2, summary.addedCount)
        assertEquals(2, summary.createdClasses.size)
        assertEquals(setOf("প্রাক-প্রাথমিক", "নার্সারি"), summary.createdClasses.map { it.nameBn }.toSet())
    }

    private fun buildMinimalXlsx(sheets: List<Pair<String, List<List<String>>>>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            val workbookXml = buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>""")
                sheets.forEachIndexed { idx, (name, _) ->
                    append("""<sheet name="${escapeXml(name)}" sheetId="${idx + 1}" r:id="rId${idx + 1}"/>""")
                }
                append("</sheets></workbook>")
            }
            zos.putNextEntry(ZipEntry("xl/workbook.xml"))
            zos.write(workbookXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            val relsXml = buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
                sheets.forEachIndexed { idx, _ ->
                    append("""<Relationship Id="rId${idx + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${idx + 1}.xml"/>""")
                }
                append("</Relationships>")
            }
            zos.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            zos.write(relsXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            sheets.forEachIndexed { idx, (_, rows) ->
                val sheetXml = buildString {
                    append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                    append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
                    rows.forEachIndexed { rIdx, r ->
                        append("""<row r="${rIdx + 1}">""")
                        r.forEachIndexed { cIdx, cellVal ->
                            val colLetter = ('A' + cIdx)
                            append("""<c r="${colLetter}${rIdx + 1}" t="inlineStr"><is><t>${escapeXml(cellVal)}</t></is></c>""")
                        }
                        append("</row>")
                    }
                    append("</sheetData></worksheet>")
                }
                zos.putNextEntry(ZipEntry("xl/worksheets/sheet${idx + 1}.xml"))
                zos.write(sheetXml.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    private fun escapeXml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
