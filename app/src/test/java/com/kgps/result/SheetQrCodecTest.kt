package com.kgps.result

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.oned.Code128Reader
import com.google.zxing.qrcode.QRCodeReader
import com.kgps.result.ai.BarcodeLuminanceDecoder
import com.kgps.result.ai.ClassSheetImportScope
import com.kgps.result.ai.ClassSheetKind
import com.kgps.result.ai.QrLuminanceDecoder
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
        assertEquals(meta.templateId, SheetQrCodec.decodeTemplateId(encoded))

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
        assertTrue("Compact QR without UTF-8 ECI should stay <= 25x25 modules, was ${matrix!!.width}", matrix.width <= 25)
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
        assertEquals(BlankBottomCodeStyle.THIN_QR, first.bottomCodeStyle)
        assertEquals(first.thinQrPayload, first.bottomThinQrPayload)
        assertTrue(first.barcodeText.contains(first.templateId))

        // Switching bottom style to CODE_128 disables bottomThinQrPayload while preserving barcodeText
        val code128Pages = BlankSheetPlanner.buildPages(
            students = listOf(Student(id = "s1", cls = 5, roll = 1, name = "সাদিয়া")),
            settings = Settings(),
            config = BlankSheetConfig(
                selectedClass = 5,
                term = 1,
                columns = traitsCols,
                includeBarcode = true,
                bottomCodeStyle = BlankBottomCodeStyle.CODE_128
            ),
            allowedClassIds = listOf(1, 2, 3, 4, 5)
        )
        assertEquals(BlankBottomCodeStyle.CODE_128, code128Pages.first().bottomCodeStyle)
        assertEquals("", code128Pages.first().bottomThinQrPayload)
        assertTrue(code128Pages.first().barcodeText.contains(code128Pages.first().templateId))

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
        assertEquals("", noCodePages.first().bottomThinQrPayload)
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

    @Test
    fun qrLuminanceDecoderRecoversDenseK2QrWithTightBlackFrameShadowBlurAndLowPxPerModule() {
        val customCols = BlankSheetCatalog.presetColumns(BlankSheetPreset.THREE_TERM_MARKS) +
            BlankSheetColumn(id = "CUSTOM_99", titleBn = "মৌখিক", isCustom = true)
        val meta = BlankSheetQrMeta(
            cls = 4,
            subjectKey = "MATH",
            term = 2,
            sheetKind = ClassSheetKind.THREE_TERM_MARKS,
            importScope = ClassSheetImportScope.BOTH,
            columnIds = customCols.mapIndexed { idx, col ->
                if (col.isCustom) "CUS:0" else col.id
            },
            customColumnTitles = listOf("মৌখিক"),
            workingDaysByTerm = mapOf(1 to "62", 2 to "65"),
            pageIndex = 2,
            totalPages = 3
        )
        val payload = SheetQrCodec.encodePayload(meta)
        val matrix = SheetQrCodec.encodeQrMatrix(payload)!!
        val mW = matrix.width

        // 1) Simulate LEGACY printed sheet geometry: 2.2-module quiet zone + dark frame border
        // + diagonal camera shadow + small fractional 2.3 px/module scale + blur
        val pxPerMod = 2.3f
        val quietPx = 5 // ~2.17 modules (below QR spec 4 modules, matching old 2.8pt padding!)
        val qrSizePx = (mW * pxPerMod).toInt() + quietPx * 2
        val cropW = qrSizePx + 20
        val cropH = qrSizePx + 20
        val frameL = 10
        val frameT = 10
        val frameR = frameL + qrSizePx - 1
        val frameB = frameT + qrSizePx - 1

        fun sampleLegacyQr(x: Float, y: Float): Boolean {
            val ix = kotlin.math.round(x).toInt()
            val iy = kotlin.math.round(y).toInt()
            // Legacy 1-2px black border around box
            if ((ix == frameL || ix == frameR) && iy in frameT..frameB) return true
            if ((iy == frameT || iy == frameB) && ix in frameL..frameR) return true
            val mx = kotlin.math.floor(((x - (frameL + quietPx)) / pxPerMod).toDouble()).toInt()
            val my = kotlin.math.floor(((y - (frameT + quietPx)) / pxPerMod).toDouble()).toInt()
            return mx in 0 until mW && my in 0 until mW && matrix.get(mx, my)
        }

        val luma = ByteArray(cropW * cropH)
        for (y in 0 until cropH) {
            for (x in 0 until cropW) {
                var darkWeight = 0f
                // 3x3 subpixel box-blur simulating camera softness & anti-aliasing
                val offsets = floatArrayOf(-0.35f, 0f, 0.35f)
                for (oy in offsets) {
                    for (ox in offsets) {
                        if (sampleLegacyQr(x + ox, y + oy)) darkWeight += 1f / 9f
                    }
                }
                val diag = (x + y).toFloat() / (cropW + cropH - 2).coerceAtLeast(1)
                val blackLum = 38f + 92f * diag // shadowy left/top -> brighter right/bottom
                val whiteLum = 128f + 106f * diag
                val lum = (whiteLum - (whiteLum - blackLum) * darkWeight).toInt().coerceIn(0, 255)
                luma[y * cropW + x] = lum.toByte()
            }
        }

        val decodedTexts = QrLuminanceDecoder.decodeAllQrs(
            width = cropW,
            height = cropH,
            luma = luma,
            addQuietZone = true
        )
        assertTrue("Expected QrLuminanceDecoder to recover legacy framed blurry K2 QR, got $decodedTexts", decodedTexts.isNotEmpty())
        val recovered = decodedTexts.firstNotNullOfOrNull { SheetQrCodec.decodePayload(it) }
        assertNotNull(recovered)
        assertEquals(4, recovered!!.cls)
        assertEquals("MATH", recovered.subjectKey)
        assertEquals(2, recovered.pageIndex)
        assertEquals(listOf("মৌখিক"), recovered.customColumnTitles)
    }

    @Test
    fun barcodeLuminanceDecoderRecoversShadowedLowContrastBlurAndZeroQuietZone() {
        val barcodeText = "C3-BAN-T1-FULL-ABCDEF1234"
        val matrix = SheetQrCodec.encodeBarcodeMatrix(barcodeText)
        assertNotNull(matrix)
        val mw = matrix!!.width

        // 1) Zero quiet zone + uneven camera shadow gradient + washed-out contrast
        val scaleX = 2
        val w1 = mw * scaleX
        val h1 = 44
        val lowContrastGradLuma = ByteArray(w1 * h1)
        for (y in 0 until h1) {
            for (x in 0 until w1) {
                val mx = x / scaleX
                val isBar = matrix.get(mx, 0)
                // Left side dark shadow (black=35, white=95), right side bright washed-out (black=155, white=220)
                val t = x.toFloat() / (w1 - 1).coerceAtLeast(1)
                val blackLevel = 35f + 120f * t
                val whiteLevel = 95f + 125f * t
                val noise = if (((x * 7 + y * 13) % 5) == 0) 4 else -3
                val lum = ((if (isBar) blackLevel else whiteLevel) + noise).toInt().coerceIn(0, 255)
                lowContrastGradLuma[y * w1 + x] = lum.toByte()
            }
        }
        val decodedShadowed = BarcodeLuminanceDecoder.decodeCode128(w1, h1, lowContrastGradLuma, addQuietZone = true)
        assertEquals(barcodeText, decodedShadowed)

        // 2) Blurry + anti-aliased narrow barcode simulating old-PDF fractional camera scaling (~1.85 px/module)
        val fractionalScale = 1.85f
        val quietPx = 6
        val w2 = (mw * fractionalScale).toInt() + quietPx * 2
        val h2 = 36
        val softLuma = ByteArray(w2 * h2) { 0xE6.toByte() }
        fun sampleAt(mxf: Float): Float {
            val idx = kotlin.math.floor(mxf.toDouble()).toInt()
            return if (idx in 0 until mw && matrix.get(idx, 0)) 45f else 225f
        }
        for (x in 0 until w2) {
            val sx = (x - quietPx) / fractionalScale
            val c0 = sampleAt(sx)
            val cLeft = sampleAt(sx - 0.42f)
            val cRight = sampleAt(sx + 0.42f)
            val blurred = ((cLeft * 0.20f + c0 * 0.60f + cRight * 0.20f)).toInt().coerceIn(0, 255)
            for (y in 4 until (h2 - 4)) {
                softLuma[y * w2 + x] = blurred.toByte()
            }
        }
        val decodedSoft = BarcodeLuminanceDecoder.decodeCode128(w2, h2, softLuma, addQuietZone = true)
        assertEquals(barcodeText, decodedSoft)
    }

    @Test
    fun barcodeRegexSupportsExtendedSubjectKeys() {
        val ch = SheetQrCodec.decodeChannel("C5-SOC_1-T2-COMPACT-ABCDEF1234")
        assertEquals("ABCDEF1234", ch.templateId)
        assertNotNull(ch.meta)
        assertEquals(5, ch.meta!!.cls)
        assertEquals("SOC_1", ch.meta.subjectKey)
        assertEquals(2, ch.meta.term)
    }

    @Test
    fun sheetScanDetectionPriorityAndLabelsReflectThreeQrChannels() {
        val meta = BlankSheetQrMeta(
            cls = 3,
            subjectKey = "BAN",
            term = 1,
            sheetKind = ClassSheetKind.SINGLE_TERM_MARKS,
            importScope = ClassSheetImportScope.BOTH,
            columnIds = listOf("ROLL", "NAME", "CONT", "WR", "TOTAL")
        )
        val rightOffline = com.kgps.result.ai.SheetScanDetection(
            templateId = meta.templateId,
            offlineQrRawText = SheetQrCodec.encodePayload(meta),
            offlineQrMeta = meta,
            detectedFromOfflineQr = true
        )
        assertEquals("ডান পাশের অফলাইন QR", rightOffline.sourceLabelBn)
        assertEquals(1, rightOffline.detectedCount)
        assertEquals(meta, rightOffline.bestLocalMeta)

        val leftThin = com.kgps.result.ai.SheetScanDetection(
            templateId = meta.templateId,
            thinQrRawText = SheetQrCodec.encodeThinTemplateQr(meta.templateId),
            detectedFromThinQr = true
        )
        assertEquals("বাম পাশের QR", leftThin.sourceLabelBn)
        assertEquals(1, leftThin.detectedCount)
        assertEquals(null, leftThin.bestLocalMeta)

        val bottomThin = com.kgps.result.ai.SheetScanDetection(
            templateId = meta.templateId,
            bottomQrRawText = SheetQrCodec.encodeThinTemplateQr(meta.templateId),
            detectedFromBottomQr = true
        )
        assertEquals("নিচের QR", bottomThin.sourceLabelBn)
        assertEquals(1, bottomThin.detectedCount)
        assertEquals(null, bottomThin.bestLocalMeta)
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
