package com.kgps.result

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.oned.Code128Reader
import com.google.zxing.qrcode.QRCodeReader
import com.kgps.result.ai.ClassSheetImportScope
import com.kgps.result.ai.ClassSheetKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
        assertTrue(encoded.startsWith("K2/"))
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
        assertEquals(meta.templateId, decoded.templateId)

        val aiPrompt = SheetQrCodec.buildAiPromptInstructions(decoded)
        assertTrue(aiPrompt.contains("cls=4"))
        assertTrue(aiPrompt.contains("subKey=MATH"))
        assertTrue(aiPrompt.contains("মৌখিক পরীক্ষা"))
        assertTrue(aiPrompt.contains("পাঠ্যবই"))
    }

    @Test
    fun thinTemplateQrIsVersion1AndDeterministicAcrossPages() {
        val cols = BlankSheetCatalog.presetColumns(BlankSheetPreset.FULL_MARKS)
        val page1 = BlankSheetQrMeta(
            cls = 3,
            subjectKey = "BAN",
            term = 1,
            sheetKind = ClassSheetKind.SINGLE_TERM_MARKS,
            importScope = ClassSheetImportScope.BOTH,
            columnIds = cols.map { it.id },
            pageIndex = 1,
            totalPages = 3
        )
        val page2 = page1.copy(pageIndex = 2, totalPages = 3)
        val tid1 = SheetQrCodec.computeTemplateId(page1)
        val tid2 = SheetQrCodec.computeTemplateId(page2)
        assertEquals(10, tid1.length)
        assertEquals(tid1, tid2)

        val thinPayload = SheetQrCodec.encodeThinTemplateQr(tid1)
        assertEquals("KT1:$tid1", thinPayload)
        val thinMatrix = SheetQrCodec.encodeQrMatrix(thinPayload)
        assertNotNull(thinMatrix)
        assertTrue("Thin QR should be Version 1 (21x21), was ${thinMatrix!!.width}", thinMatrix.width <= 21)

        val decodedQrText = decodeQrMatrixWithZxing(thinMatrix)
        val channel = SheetQrCodec.decodeChannel(decodedQrText)
        assertEquals(tid1, channel.templateId)
        assertEquals(tid1, SheetQrCodec.decodeTemplateId(decodedQrText))

        // Different class produces a different templateId
        val otherTid = SheetQrCodec.computeTemplateId(page1.copy(cls = 4))
        assertNotEquals(tid1, otherTid)
    }

    @Test
    fun compactK2QrStaysSparseForStandardPresets() {
        val fullCols = BlankSheetCatalog.presetColumns(BlankSheetPreset.FULL_MARKS)
        val meta = BlankSheetQrMeta(
            cls = 3,
            subjectKey = "BAN",
            term = 1,
            sheetKind = ClassSheetKind.SINGLE_TERM_MARKS,
            importScope = ClassSheetImportScope.BOTH,
            columnIds = fullCols.map { it.id }
        )
        val payload = SheetQrCodec.encodePayload(meta)
        assertTrue("Compact K2 payload should stay <= 45 chars, was ${payload.length}: $payload", payload.length <= 45)

        val matrix = SheetQrCodec.encodeQrMatrix(payload)
        assertNotNull(matrix)
        assertTrue("Compact QR should stay <= 29x29 modules, was ${matrix!!.width}", matrix.width <= 29)
    }

    @Test
    fun code128BarcodeEncodeAndDecodeRoundTripsTemplateIdAndMeta() {
        val cols = BlankSheetCatalog.presetColumns(BlankSheetPreset.FULL_MARKS)
        val tid = "ABCD2345EF"
        val barcodeText = SheetQrCodec.encodeBarcodeText(
            cls = 4,
            subjectKey = "MATH",
            term = 2,
            columns = cols,
            templateId = tid
        )
        assertEquals("C4-MATH-T2-FULL-$tid", barcodeText)

        val matrix = SheetQrCodec.encodeBarcodeMatrix(barcodeText)
        assertNotNull(matrix)

        val scaleX = 2
        val quietX = 16
        val heightPx = 48
        val widthPx = matrix!!.width * scaleX + quietX * 2
        val pixels = IntArray(widthPx * heightPx) { 0xFFFFFFFF.toInt() }
        for (x in 0 until matrix.width) {
            if (matrix.get(x, 0)) {
                val startX = quietX + x * scaleX
                for (y in 4 until (heightPx - 4)) {
                    for (dx in 0 until scaleX) {
                        pixels[y * widthPx + startX + dx] = 0xFF000000.toInt()
                    }
                }
            }
        }

        val source = RGBLuminanceSource(widthPx, heightPx, pixels)
        val result = Code128Reader().decode(
            BinaryBitmap(HybridBinarizer(source)),
            mapOf(
                DecodeHintType.TRY_HARDER to true,
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.CODE_128)
            )
        )
        val channel = SheetQrCodec.decodeChannel(result.text)
        assertEquals(tid, channel.templateId)
        assertNotNull(channel.meta)
        assertEquals(4, channel.meta!!.cls)
        assertEquals("MATH", channel.meta.subjectKey)
        assertEquals(2, channel.meta.term)
        assertEquals(ClassSheetKind.SINGLE_TERM_MARKS, channel.meta.sheetKind)
    }

    @Test
    fun legacyJsonAndRefCodeRemainDecodable() {
        val legacyJson = """{"v":1,"c":3,"s":"BAN","t":1,"k":"1M","sc":"B","cols":["ROLL","NAME","CONT","WR"]}"""
        val decodedLegacy = SheetQrCodec.decodePayload(legacyJson)
        assertNotNull(decodedLegacy)
        assertEquals(3, decodedLegacy!!.cls)
        assertEquals("BAN", decodedLegacy.subjectKey)
        assertEquals(1, decodedLegacy.term)
        assertEquals(listOf("ROLL", "NAME", "CONT", "WR"), decodedLegacy.columnIds)

        val refDecoded = SheetQrCodec.decodePayload("REF:C5-ENG-T3-3MARKS")
        assertNotNull(refDecoded)
        assertEquals(5, refDecoded!!.cls)
        assertEquals("ENG", refDecoded.subjectKey)
        assertEquals(3, refDecoded.term)
        assertEquals(ClassSheetKind.THREE_TERM_MARKS, refDecoded.sheetKind)
    }

    @Test
    fun attendanceTraitsContOnlyAndPlannerFlagsWorkCleanly() {
        val traitsCols = BlankSheetCatalog.presetColumns(BlankSheetPreset.THREE_TERM_ATTENDANCE_TRAITS)
        val pages = BlankSheetPlanner.buildPages(
            students = listOf(Student(id = "s1", cls = 5, roll = 1, name = "সাদিয়া")),
            settings = Settings(),
            config = BlankSheetConfig(
                selectedClass = 5,
                term = 1,
                columns = traitsCols,
                workingDaysByTerm = mapOf(1 to "62", 2 to "65"),
                includeQrCode = true,
                includeBarcode = true
            ),
            allowedClassIds = listOf(1, 2, 3, 4, 5)
        )
        val first = pages.first()
        assertTrue(first.templateId.isNotBlank())
        assertTrue(first.thinQrPayload.startsWith("KT1:"))
        assertTrue(first.qrPayload.startsWith("K2/"))
        assertTrue(first.barcodeText.contains(first.templateId))

        val decodedTraits = SheetQrCodec.decodePayload(first.qrPayload)
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

        // When QR and Barcode disabled, both payloads stay empty while templateId remains valid
        val noCodePages = BlankSheetPlanner.buildPages(
            students = listOf(Student(id = "s1", cls = 3, roll = 1, name = "রাফি")),
            settings = Settings(),
            config = BlankSheetConfig(selectedClass = 3, includeQrCode = false, includeBarcode = false),
            allowedClassIds = listOf(3)
        )
        assertTrue(noCodePages.first().templateId.isNotBlank())
        assertEquals("", noCodePages.first().thinQrPayload)
        assertEquals("", noCodePages.first().qrPayload)
        assertEquals("", noCodePages.first().barcodeText)
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

        val decodedText = decodeQrMatrixWithZxing(matrix!!)
        val decoded = SheetQrCodec.decodePayload(decodedText)
        assertNotNull(decoded)
        assertEquals(3, decoded!!.cls)
        assertEquals("BAN", decoded.subjectKey)
        assertEquals(ClassSheetKind.THREE_TERM_MARKS, decoded.sheetKind)
        assertEquals(cols.map { it.id }, decoded.columnIds)
    }

    private fun decodeQrMatrixWithZxing(matrix: com.google.zxing.common.BitMatrix): String {
        val scale = 4
        val quiet = 4
        val w = (matrix.width + quiet * 2) * scale
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
        return result.text
    }
}
