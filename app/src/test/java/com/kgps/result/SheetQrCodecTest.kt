package com.kgps.result

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.kgps.result.ai.ClassSheetImportScope
import com.kgps.result.ai.ClassSheetKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetQrCodecTest {

    @Test
    fun payloadRoundTripPreservesAllSheetFormatsCustomColsAndWorkingDays() {
        val customCol = BlankSheetColumn(
            id = "CUSTOM_99",
            titleBn = "মৌখিক পরীক্ষা",
            isCustom = true
        )
        val fullWithCustom = BlankSheetCatalog.presetColumns(BlankSheetPreset.FULL_MARKS) + customCol
        val pageSpec = BlankSheetPageSpec(
            cls = 4,
            classTitleBn = "চতুর্থ",
            subjectTitleBn = "গণিত",
            termTitleBn = "২য় প্রান্তিক",
            classPageIndex = 1,
            classTotalPages = 2,
            globalPageNumber = 1,
            totalPages = 2,
            totalStudentsInClass = 35,
            rowsPerPageCapacity = 20,
            rows = emptyList(),
            columns = fullWithCustom,
            showSubjectMaxInHeader = true,
            subjectKey = "MATH",
            numeralStyle = BlankNumeralStyle.ENGLISH,
            workingDaysByTerm = emptyMap(),
            sheetRefCode = BlankSheetCatalog.buildSheetRefCode(4, "MATH", 2, fullWithCustom)
        )

        val meta = SheetQrCodec.fromPageConfig(pageSpec, term = 2)
        val encoded = SheetQrCodec.encodePayload(meta)
        val decoded = SheetQrCodec.decodePayload(encoded)

        assertNotNull(decoded)
        assertEquals(4, decoded!!.cls)
        assertEquals("MATH", decoded.subjectKey)
        assertEquals(2, decoded.term)
        assertEquals(ClassSheetKind.SINGLE_TERM_MARKS, decoded.sheetKind)
        assertEquals(ClassSheetImportScope.BOTH, decoded.importScope)
        assertEquals(BlankNumeralStyle.ENGLISH, decoded.numeralStyle)
        assertEquals(1, decoded.pageIndex)
        assertEquals(2, decoded.totalPages)
        assertEquals(listOf("মৌখিক পরীক্ষা"), decoded.customColumnTitles)
        assertTrue(decoded.columnIds.contains("CUS:0"))

        val aiPrompt = SheetQrCodec.buildAiPromptInstructions(decoded)
        assertTrue(aiPrompt.contains("cls=4"))
        assertTrue(aiPrompt.contains("subKey=MATH"))
        assertTrue(aiPrompt.contains("মৌখিক পরীক্ষা"))
        assertTrue(aiPrompt.contains("পাঠ্যবই"))
    }

    @Test
    fun attendanceTraitsAndContOnlyScopesRoundTripCleanly() {
        val traitsCols = BlankSheetCatalog.presetColumns(BlankSheetPreset.THREE_TERM_ATTENDANCE_TRAITS)
        val pages = BlankSheetPlanner.buildPages(
            students = listOf(Student(id = "s1", cls = 5, roll = 1, name = "সাদিয়া")),
            settings = Settings(),
            config = BlankSheetConfig(
                selectedClass = 5,
                term = 1,
                columns = traitsCols,
                workingDaysByTerm = mapOf(1 to "62", 2 to "65"),
                includeQrCode = true
            ),
            allowedClassIds = listOf(1, 2, 3, 4, 5)
        )
        assertTrue(pages.first().qrPayload.isNotBlank())
        val decodedTraits = SheetQrCodec.decodePayload(pages.first().qrPayload)
        assertNotNull(decodedTraits)
        assertEquals(ClassSheetKind.ATTENDANCE_TRAITS, decodedTraits!!.sheetKind)
        assertEquals("62", decodedTraits.workingDaysByTerm[1])
        assertEquals("65", decodedTraits.workingDaysByTerm[2])

        // Only continuous marks scope detection
        val contOnlyCols = listOfNotNull(
            BlankSheetCatalog.byId("ROLL"),
            BlankSheetCatalog.byId("NAME"),
            BlankSheetCatalog.byId("CONT")
        )
        val (kind, scope) = SheetQrCodec.inferSheetKindAndScope(contOnlyCols)
        assertEquals(ClassSheetKind.SINGLE_TERM_MARKS, kind)
        assertEquals(ClassSheetImportScope.CONT_ONLY, scope)

        // When QR disabled, qrPayload stays empty
        val noQrPages = BlankSheetPlanner.buildPages(
            students = listOf(Student(id = "s1", cls = 3, roll = 1, name = "রাফি")),
            settings = Settings(),
            config = BlankSheetConfig(selectedClass = 3, includeQrCode = false),
            allowedClassIds = listOf(3)
        )
        assertEquals("", noQrPages.first().qrPayload)
    }

    @Test
    fun zxingMatrixEncodeAndDecodeRoundTripWithoutAndroidRuntime() {
        val cols = BlankSheetCatalog.presetColumns(BlankSheetPreset.THREE_TERM_MARKS)
        val meta = BlankSheetQrMeta(
            cls = 3,
            subjectKey = "BAN",
            term = 1,
            sheetKind = ClassSheetKind.THREE_TERM_MARKS,
            importScope = ClassSheetImportScope.BOTH,
            columnIds = cols.map { it.id }
        )
        val payload = SheetQrCodec.encodePayload(meta)
        val matrix = SheetQrCodec.encodeQrMatrix(payload)
        assertNotNull(matrix)

        // Render BitMatrix onto a crisp white/black ARGB pixel canvas with 4px per module + quiet zone
        val scale = 4
        val quiet = 4
        val w = (matrix!!.width + quiet * 2) * scale
        val h = (matrix.height + quiet * 2) * scale
        val pixels = IntArray(w * h) { 0xFFFFFFFF.toInt() }
        for (my in 0 until matrix.height) {
            for (mx in 0 until matrix.width) {
                if (matrix.get(mx, my)) {
                    val startX = (mx + quiet) * scale
                    val startY = (my + quiet) * scale
                    for (dy in 0 until scale) {
                        for (dx in 0 until scale) {
                            pixels[(startY + dy) * w + (startX + dx)] = 0xFF000000.toInt()
                        }
                    }
                }
            }
        }

        val source = RGBLuminanceSource(w, h, pixels)
        val result = QRCodeReader().decode(
            BinaryBitmap(HybridBinarizer(source)),
            mapOf(
                DecodeHintType.TRY_HARDER to true,
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.CHARACTER_SET to "UTF-8"
            )
        )
        val decoded = SheetQrCodec.decodePayload(result.text)
        assertNotNull(decoded)
        assertEquals(3, decoded!!.cls)
        assertEquals("BAN", decoded.subjectKey)
        assertEquals(ClassSheetKind.THREE_TERM_MARKS, decoded.sheetKind)
        assertEquals(cols.map { it.id }, decoded.columnIds)
    }
}
