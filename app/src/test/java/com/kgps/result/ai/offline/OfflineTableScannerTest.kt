package com.kgps.result.ai.offline

import com.kgps.result.Student
import com.kgps.result.ai.ClassSheetImportScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.GZIPInputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

class OfflineTableScannerTest {

    private fun resolveProjectFile(relativePath: String): File {
        val candidates = listOf(
            File(relativePath),
            File("app/$relativePath"),
            File("../$relativePath")
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("Could not find file $relativePath from cwd=${File(".").absolutePath}")
    }

    private fun createRecognizer(): DualScriptDigitRecognizer {
        val enFile = resolveProjectFile("src/main/assets/ml/digits_en.bin")
        val bnFile = resolveProjectFile("src/main/assets/ml/digits_bn.bin")
        return DualScriptDigitRecognizer(
            openEnglishStream = { FileInputStream(enFile) },
            openBengaliStream = { FileInputStream(bnFile) }
        )
    }

    private fun newCanvas(w: Int, h: Int): FloatArray = FloatArray(w * h) { x ->
        val px = x % w
        val py = x / w
        246f - (px * 6f / w) - (py * 8f / h) // slight realistic paper gradient
    }

    private fun drawDarkLine(
        luma: FloatArray,
        w: Int,
        h: Int,
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        radius: Float = 1.4f,
        darkness: Float = 25f
    ) {
        val steps = max(4, (hypot((x1 - x0).toDouble(), (y1 - y0).toDouble()) * 2.2).toInt())
        val rCeil = radius.roundToInt() + 1
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val cx = x0 + (x1 - x0) * t
            val cy = y0 + (y1 - y0) * t
            val ix0 = max(0, (cx - rCeil).toInt())
            val ix1 = min(w - 1, (cx + rCeil).toInt())
            val iy0 = max(0, (cy - rCeil).toInt())
            val iy1 = min(h - 1, (cy + rCeil).toInt())
            for (y in iy0..iy1) {
                for (x in ix0..ix1) {
                    val d = hypot((x - cx).toDouble(), (y - cy).toDouble()).toFloat()
                    if (d <= radius + 0.8f) {
                        val wgt = (1f - (d / (radius + 0.8f))).coerceIn(0f, 1f)
                        val idx = y * w + x
                        luma[idx] = min(luma[idx], luma[idx] * (1f - wgt) + darkness * wgt)
                    }
                }
            }
        }
    }

    private fun drawWordLikeNameAndHeader(
        luma: FloatArray,
        w: Int,
        h: Int,
        cx: Int,
        cy: Int,
        wordWidth: Int,
        wordHeight: Int,
        isHeaderWord: Boolean
    ) {
        // Draw a matra bar + vertical & curved strokes simulating Bangla/English word text
        val xStart = cx - wordWidth / 2f
        val xEnd = cx + wordWidth / 2f
        val yTop = cy - wordHeight * 0.35f
        val yBot = cy + wordHeight * 0.38f
        drawDarkLine(luma, w, h, xStart, yTop, xEnd, yTop, radius = 1.3f)
        val count = if (isHeaderWord) 5 else 7
        for (i in 0 until count) {
            val sx = xStart + (wordWidth * (i + 0.5f) / count)
            drawDarkLine(luma, w, h, sx, yTop, sx + (if (i % 2 == 0) 3f else -2f), yBot, radius = 1.2f)
            if (i % 2 == 0) {
                drawDarkLine(luma, w, h, sx, (yTop + yBot) * 0.5f, sx + wordWidth * 0.08f, yBot - 2f, radius = 1.1f)
            }
        }
    }

    private fun stampPatch(
        luma: FloatArray,
        w: Int,
        h: Int,
        patch: FloatArray,
        pw: Int,
        ph: Int,
        dstX: Int,
        dstY: Int,
        scaleW: Int,
        scaleH: Int
    ) {
        for (dy in 0 until scaleH) {
            val gy = dstY + dy
            if (gy !in 0 until h) continue
            val sy = (dy + 0.5f) * (ph / scaleH.toFloat()) - 0.5f
            val y0 = floor(sy.toDouble()).toInt().coerceIn(0, ph - 1)
            val y1 = (y0 + 1).coerceIn(0, ph - 1)
            val fy = (sy - y0).coerceAtLeast(0f)
            for (dx in 0 until scaleW) {
                val gx = dstX + dx
                if (gx !in 0 until w) continue
                val sx = (dx + 0.5f) * (pw / scaleW.toFloat()) - 0.5f
                val x0 = floor(sx.toDouble()).toInt().coerceIn(0, pw - 1)
                val x1 = (x0 + 1).coerceIn(0, pw - 1)
                val fx = (sx - x0).coerceAtLeast(0f)
                val v00 = patch[y0 * pw + x0]
                val v10 = patch[y0 * pw + x1]
                val v01 = patch[y1 * pw + x0]
                val v11 = patch[y1 * pw + x1]
                val ink = ((v00 * (1f - fx) + v10 * fx) * (1f - fy) +
                    (v01 * (1f - fx) + v11 * fx) * fy).coerceIn(0f, 1f)
                if (ink > 0.05f) {
                    val idx = gy * w + gx
                    luma[idx] = min(luma[idx], luma[idx] * (1f - ink) + 18f * ink)
                }
            }
        }
    }

    private fun rotateLuma(src: FloatArray, w: Int, h: Int, deg: Double): FloatArray {
        val rad = Math.toRadians(deg)
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()
        val cx = (w - 1) * 0.5f
        val cy = (h - 1) * 0.5f
        val out = FloatArray(w * h) { 244f }
        for (y in 0 until h) {
            val dy = y - cy
            for (x in 0 until w) {
                val dx = x - cx
                val sx = cx + dx * c + dy * s
                val sy = cy - dx * s + dy * c
                val x0 = floor(sx.toDouble()).toInt()
                val y0 = floor(sy.toDouble()).toInt()
                if (x0 in 0 until (w - 1) && y0 in 0 until (h - 1)) {
                    val fx = sx - x0
                    val fy = sy - y0
                    val v00 = src[y0 * w + x0]
                    val v10 = src[y0 * w + x0 + 1]
                    val v01 = src[(y0 + 1) * w + x0]
                    val v11 = src[(y0 + 1) * w + x0 + 1]
                    out[y * w + x] = (v00 * (1f - fx) + v10 * fx) * (1f - fy) +
                        (v01 * (1f - fx) + v11 * fx) * fy
                }
            }
        }
        return out
    }

    /**
     * Loads clean, high-confidence real handwritten digit samples per class (0..9)
     * from MNIST or BHaND test files so synthetic tables contain REAL human handwriting!
     */
    private fun loadRealDigitBank(
        isBengali: Boolean,
        recognizer: DualScriptDigitRecognizer
    ): Array<List<Pair<FloatArray, Int>>>? {
        val bank = Array(10) { ArrayList<Pair<FloatArray, Int>>() }
        if (!isBengali) {
            val imgFile = File("/tmp/ml_train/mnist_test_images.gz")
            val lblFile = File("/tmp/ml_train/mnist_test_labels.gz")
            if (!imgFile.exists() || !lblFile.exists()) return null
            DataInputStream(GZIPInputStream(FileInputStream(imgFile))).use { disImg ->
                DataInputStream(GZIPInputStream(FileInputStream(lblFile))).use { disLbl ->
                    disImg.readInt(); disImg.readInt(); disImg.readInt(); disImg.readInt()
                    disLbl.readInt(); disLbl.readInt()
                    val buf = ByteArray(784)
                    for (i in 0 until 800) {
                        disImg.readFully(buf)
                        val label = disLbl.readUnsignedByte()
                        if (bank[label].size >= 6) continue
                        val raw = FloatArray(784) { (buf[it].toInt() and 0xFF) / 255f }
                        val canon = DigitCnnClassifier.canonicalNormalize(raw, 28, 28)
                        val pred = recognizer.predict(canon, OfflineDigitScript.ENGLISH)
                        if (pred.digit == label && pred.confidence >= 0.90f) {
                            bank[label].add(raw to 28)
                        }
                        if (bank.all { it.size >= 4 }) break
                    }
                }
            }
        } else {
            val bhandFile = File("/tmp/ml_train/bhand_test.u8")
            if (!bhandFile.exists()) return null
            FileInputStream(bhandFile).use { fis ->
                val dis = DataInputStream(fis)
                val count = dis.readInt()
                val dim = dis.readInt()
                val nPx = dim * dim
                val buf = ByteArray(nPx)
                for (i in 0 until min(count, 1500)) {
                    val label = dis.readUnsignedByte()
                    dis.readFully(buf)
                    if (label !in 0..9 || bank[label].size >= 6) continue
                    val raw = FloatArray(nPx) { (buf[it].toInt() and 0xFF) / 255f }
                    val canon = DigitCnnClassifier.canonicalNormalize(raw, dim, dim)
                    val pred = recognizer.predict(canon, OfflineDigitScript.BENGALI)
                    if (pred.digit == label && pred.confidence >= 0.90f) {
                        bank[label].add(raw to dim)
                    }
                    if (bank.all { it.size >= 4 }) break
                }
            }
        }
        return if (bank.all { it.isNotEmpty() }) bank.map { it.toList() }.toTypedArray() else null
    }

    /**
     * Fallback stroke-drawn English digit patch (28x28) used when /tmp datasets aren't present.
     */
    private fun drawSyntheticEnglishDigitPatch(d: Int): Pair<FloatArray, Int> {
        val sz = 28
        val luma = FloatArray(sz * sz) { 255f }
        when (d) {
            0 -> {
                val pts = listOf(14f to 4f, 20f to 8f, 21f to 14f, 19f to 21f, 14f to 24f, 9f to 21f, 7f to 14f, 8f to 8f, 14f to 4f)
                for (i in 0 until pts.size - 1) drawDarkLine(luma, sz, sz, pts[i].first, pts[i].second, pts[i + 1].first, pts[i + 1].second, 1.5f, 0f)
            }
            1 -> drawDarkLine(luma, sz, sz, 14f, 4f, 14f, 24f, 1.6f, 0f)
            2 -> {
                drawDarkLine(luma, sz, sz, 8f, 8f, 14f, 4f, 1.5f, 0f)
                drawDarkLine(luma, sz, sz, 14f, 4f, 20f, 8f, 1.5f, 0f)
                drawDarkLine(luma, sz, sz, 20f, 8f, 8f, 23f, 1.5f, 0f)
                drawDarkLine(luma, sz, sz, 8f, 23f, 21f, 23f, 1.5f, 0f)
            }
            3 -> {
                drawDarkLine(luma, sz, sz, 8f, 6f, 19f, 6f, 1.5f, 0f)
                drawDarkLine(luma, sz, sz, 19f, 6f, 12f, 14f, 1.5f, 0f)
                drawDarkLine(luma, sz, sz, 12f, 14f, 20f, 19f, 1.5f, 0f)
                drawDarkLine(luma, sz, sz, 20f, 19f, 9f, 23f, 1.5f, 0f)
            }
            4 -> {
                drawDarkLine(luma, sz, sz, 17f, 4f, 7f, 17f, 1.5f, 0f)
                drawDarkLine(luma, sz, sz, 7f, 17f, 22f, 17f, 1.5f, 0f)
                drawDarkLine(luma, sz, sz, 18f, 4f, 18f, 24f, 1.5f, 0f)
            }
            5 -> {
                drawDarkLine(luma, sz, sz, 20f, 5f, 9f, 5f, 1.5f, 0f)
                drawDarkLine(luma, sz, sz, 9f, 5f, 9f, 13f, 1.5f, 0f)
                drawDarkLine(luma, sz, sz, 9f, 13f, 20f, 15f, 1.5f, 0f)
                drawDarkLine(luma, sz, sz, 20f, 15f, 19f, 22f, 1.5f, 0f)
                drawDarkLine(luma, sz, sz, 19f, 22f, 8f, 23f, 1.5f, 0f)
            }
            6 -> {
                drawDarkLine(luma, sz, sz, 18f, 5f, 9f, 14f, 1.5f, 0f)
                drawDarkLine(luma, sz, sz, 9f, 14f, 9f, 22f, 1.5f, 0f)
                drawDarkLine(luma, sz, sz, 9f, 22f, 19f, 22f, 1.5f, 0f)
                drawDarkLine(luma, sz, sz, 19f, 22f, 19f, 15f, 1.5f, 0f)
                drawDarkLine(luma, sz, sz, 19f, 15f, 9f, 15f, 1.5f, 0f)
            }
            7 -> {
                drawDarkLine(luma, sz, sz, 7f, 5f, 21f, 5f, 1.5f, 0f)
                drawDarkLine(luma, sz, sz, 21f, 5f, 12f, 24f, 1.5f, 0f)
            }
            8 -> {
                val p = listOf(14f to 4f, 19f to 8f, 14f to 13f, 9f to 19f, 14f to 24f, 19f to 19f, 14f to 13f, 9f to 8f, 14f to 4f)
                for (i in 0 until p.size - 1) drawDarkLine(luma, sz, sz, p[i].first, p[i].second, p[i + 1].first, p[i + 1].second, 1.5f, 0f)
            }
            else -> {
                drawDarkLine(luma, sz, sz, 19f, 6f, 9f, 6f, 1.5f, 0f)
                drawDarkLine(luma, sz, sz, 9f, 6f, 9f, 13f, 1.5f, 0f)
                drawDarkLine(luma, sz, sz, 9f, 13f, 19f, 13f, 1.5f, 0f)
                drawDarkLine(luma, sz, sz, 19f, 6f, 19f, 24f, 1.5f, 0f)
            }
        }
        return FloatArray(sz * sz) { ((255f - luma[it]) / 255f).coerceIn(0f, 1f) } to sz
    }

    private fun buildHandwrittenTableGrayImage(
        colWidths: IntArray,
        numericRows: List<List<String>>,
        digitBank: Array<List<Pair<FloatArray, Int>>>,
        skewDegrees: Double = 0.0,
        hasNameColumn: Boolean = true
    ): GrayImage {
        val margin = 44
        val headerH = 74
        val rowH = 82
        val tableW = colWidths.sum()
        val tableH = headerH + numericRows.size * rowH
        val width = tableW + margin * 2
        val height = tableH + margin * 2
        val luma = newCanvas(width, height)

        // Outer border + horizontal/vertical lines
        drawDarkLine(luma, width, height, margin.toFloat(), margin.toFloat(), (margin + tableW).toFloat(), margin.toFloat(), 1.5f)
        drawDarkLine(luma, width, height, margin.toFloat(), (margin + tableH).toFloat(), (margin + tableW).toFloat(), (margin + tableH).toFloat(), 1.5f)
        drawDarkLine(luma, width, height, margin.toFloat(), margin.toFloat(), margin.toFloat(), (margin + tableH).toFloat(), 1.5f)
        drawDarkLine(luma, width, height, (margin + tableW).toFloat(), margin.toFloat(), (margin + tableW).toFloat(), (margin + tableH).toFloat(), 1.5f)

        var xCur = margin
        for (i in 0 until colWidths.size - 1) {
            xCur += colWidths[i]
            drawDarkLine(luma, width, height, xCur.toFloat(), margin.toFloat(), xCur.toFloat(), (margin + tableH).toFloat(), 1.4f)
        }
        drawDarkLine(luma, width, height, margin.toFloat(), (margin + headerH).toFloat(), (margin + tableW).toFloat(), (margin + headerH).toFloat(), 1.4f)
        for (r in 1 until numericRows.size) {
            val y = (margin + headerH + r * rowH).toFloat()
            drawDarkLine(luma, width, height, margin.toFloat(), y, (margin + tableW).toFloat(), y, 1.4f)
        }

        // Draw header row word-like shapes
        xCur = margin
        for (c in colWidths.indices) {
            drawWordLikeNameAndHeader(
                luma, width, height,
                cx = xCur + colWidths[c] / 2,
                cy = margin + headerH / 2,
                wordWidth = (colWidths[c] * 0.68f).toInt(),
                wordHeight = 32,
                isHeaderWord = true
            )
            xCur += colWidths[c]
        }

        // Draw data rows: Col 0 = Roll, Col 1 = Name (skipped word shape!), Col 2..N = numeric cells
        for (r in numericRows.indices) {
            val yTop = margin + headerH + r * rowH
            xCur = margin
            for (c in colWidths.indices) {
                if (hasNameColumn && c == 1) {
                    drawWordLikeNameAndHeader(
                        luma, width, height,
                        cx = xCur + colWidths[c] / 2,
                        cy = yTop + rowH / 2,
                        wordWidth = (colWidths[c] * 0.72f).toInt(),
                        wordHeight = 34,
                        isHeaderWord = false
                    )
                } else {
                    val text = numericRows[r][if (!hasNameColumn || c == 0) c else c - 1]
                    val digitW = 30
                    val digitH = 38
                    val spacing = 5
                    val totalW = text.length * digitW + (text.length - 1) * spacing
                    var dx = xCur + (colWidths[c] - totalW) / 2
                    val dy = yTop + (rowH - digitH) / 2
                    for (chIdx in text.indices) {
                        val d = text[chIdx] - '0'
                        val variants = digitBank[d]
                        val (patch, pSize) = variants[(r + c + chIdx) % variants.size]
                        stampPatch(luma, width, height, patch, pSize, pSize, dx, dy, digitW, digitH)
                        dx += digitW + spacing
                    }
                }
                xCur += colWidths[c]
            }
        }

        val finalLuma = if (skewDegrees != 0.0) rotateLuma(luma, width, height, skewDegrees) else luma
        return GrayImage.buildFromLuma(width, height, finalLuma)
    }

    @Test
    fun testRealHandwrittenHeldOutAccuracyAndModelSanity() {
        val recognizer = createRecognizer()

        // Sanity test on clean stroke-drawn digits 0..9
        for (d in 0..9) {
            val (patch, sz) = drawSyntheticEnglishDigitPatch(d)
            val canon = DigitCnnClassifier.canonicalNormalize(patch, sz, sz)
            val pred = recognizer.predict(canon, OfflineDigitScript.ENGLISH)
            assertEquals("Synthetic digit $d sanity mismatch", d, pred.digit)
        }

        // If real MNIST/BHaND datasets exist in /tmp/ml_train/, evaluate 1,000 real test digits each
        val mnistImgFile = File("/tmp/ml_train/mnist_test_images.gz")
        val mnistLblFile = File("/tmp/ml_train/mnist_test_labels.gz")
        if (mnistImgFile.exists() && mnistLblFile.exists()) {
            var enCorrect = 0
            val enTotal = 1000
            DataInputStream(GZIPInputStream(FileInputStream(mnistImgFile))).use { disImg ->
                DataInputStream(GZIPInputStream(FileInputStream(mnistLblFile))).use { disLbl ->
                    disImg.readInt(); disImg.readInt(); disImg.readInt(); disImg.readInt()
                    disLbl.readInt(); disLbl.readInt()
                    val buf = ByteArray(784)
                    val raw = FloatArray(784)
                    for (i in 0 until enTotal) {
                        disImg.readFully(buf)
                        val lbl = disLbl.readUnsignedByte()
                        for (p in 0 until 784) raw[p] = (buf[p].toInt() and 0xFF) / 255f
                        val pred = recognizer.predict(DigitCnnClassifier.canonicalNormalize(raw, 28, 28), OfflineDigitScript.ENGLISH)
                        if (pred.digit == lbl) enCorrect++
                    }
                }
            }
            val enAcc = enCorrect.toDouble() / enTotal
            assertTrue("Expected MNIST accuracy >= 96%, got ${enAcc * 100}%", enAcc >= 0.96)
        }

        val bhandTestFile = File("/tmp/ml_train/bhand_test.u8")
        if (bhandTestFile.exists()) {
            var bnCorrect = 0
            val bnTotal = 1000
            FileInputStream(bhandTestFile).use { fis ->
                val dis = DataInputStream(fis)
                dis.readInt() // count
                val dim = dis.readInt()
                val nPx = dim * dim
                val buf = ByteArray(nPx)
                val raw = FloatArray(nPx)
                for (i in 0 until bnTotal) {
                    val lbl = dis.readUnsignedByte()
                    dis.readFully(buf)
                    for (p in 0 until nPx) raw[p] = (buf[p].toInt() and 0xFF) / 255f
                    val pred = recognizer.predict(DigitCnnClassifier.canonicalNormalize(raw, dim, dim), OfflineDigitScript.BENGALI)
                    if (pred.digit == lbl) bnCorrect++
                }
            }
            val bnAcc = bnCorrect.toDouble() / bnTotal
            assertTrue("Expected BHaND accuracy >= 93%, got ${bnAcc * 100}%", bnAcc >= 0.93)
        }
    }

    @Test
    fun testEndToEndEnglishSkewedMarksheetWithNameSkipAndRosterMatch() {
        val recognizer = createRecognizer()
        val bank = loadRealDigitBank(isBengali = false, recognizer = recognizer)
            ?: Array(10) { listOf(drawSyntheticEnglishDigitPatch(it)) }

        val roster = listOf(
            Student(id = "e1", cls = 4, roll = 1, name = "মেহেদী হাসান"),
            Student(id = "e2", cls = 4, roll = 2, name = "নুসরাত জাহান"),
            Student(id = "e3", cls = 4, roll = 3, name = "সাকিব আল মাহমুদ"),
            Student(id = "e4", cls = 4, roll = 4, name = "ফারহানা ইয়াসমিন")
        )

        // [Roll, Cont, Wr, Grand]
        val rows = listOf(
            listOf("1", "25", "60", "85"),
            listOf("2", "28", "64", "92"),
            listOf("3", "18", "49", "67"),
            listOf("4", "30", "70", "100")
        )

        val gray = buildHandwrittenTableGrayImage(
            colWidths = intArrayOf(96, 280, 125, 125, 136),
            numericRows = rows,
            digitBank = bank,
            skewDegrees = 2.5
        )

        val out = OfflineTableScanner.scanGrayImage(
            grayImage = gray,
            recognizer = recognizer,
            clsHint = 4,
            termHint = 1,
            subKeyHint = "MAT",
            importScope = ClassSheetImportScope.BOTH,
            digitScript = OfflineDigitScript.ENGLISH,
            layoutPreset = OfflineTableLayoutPreset.AUTO,
            existingStudents = roster
        )

        assertTrue("Expected deskew for 2.5° skew, got ${out.diagnostics.usedDeskewDegrees}", abs(out.diagnostics.usedDeskewDegrees) >= 1.5f)
        assertEquals(1, out.diagnostics.skippedNameColumnIndex)
        assertEquals(1, out.diagnostics.headerRowsSkipped)
        assertEquals(4, out.sheet.rows.size)

        for (i in rows.indices) {
            val r = out.sheet.rows[i]
            assertEquals("Row $i roll", rows[i][0], r.rollText)
            assertEquals("Row $i name from DB", roster[i].name, r.name)
            assertEquals("Row $i CONT", rows[i][1], r.sheetContinuousTotal)
            assertEquals("Row $i WR", rows[i][2], r.marksText["WR"])
            assertEquals("Row $i GRAND", rows[i][3], r.sheetGrandTotal)
        }
    }

    @Test
    fun testEndToEndBengaliHandwrittenMarksheetAndAutoDetection() {
        val recognizer = createRecognizer()
        val bank = loadRealDigitBank(isBengali = true, recognizer = recognizer) ?: return

        val roster = listOf(
            Student(id = "b1", cls = 3, roll = 1, name = "আরিয়ান ইসলাম"),
            Student(id = "b2", cls = 3, roll = 2, name = "খাদিজা তুল কুবরা"),
            Student(id = "b3", cls = 3, roll = 3, name = "তানভীর আহমেদ"),
            Student(id = "b4", cls = 3, roll = 4, name = "সুমাইয়া আক্তার"),
            Student(id = "b5", cls = 3, roll = 5, name = "রাফি হাসান")
        )

        // [Roll, Cont, Wr, Grand] using real handwritten BHaND Bengali numerals
        val rows = listOf(
            listOf("1", "24", "58", "82"),
            listOf("2", "27", "63", "90"),
            listOf("3", "19", "46", "65"),
            listOf("4", "28", "65", "93"),
            listOf("5", "22", "51", "73")
        )

        val gray = buildHandwrittenTableGrayImage(
            colWidths = intArrayOf(96, 285, 130, 130, 130),
            numericRows = rows,
            digitBank = bank,
            skewDegrees = 0.0
        )

        val out = OfflineTableScanner.scanGrayImage(
            grayImage = gray,
            recognizer = recognizer,
            clsHint = 3,
            termHint = 1,
            subKeyHint = "BAN",
            importScope = ClassSheetImportScope.BOTH,
            digitScript = OfflineDigitScript.AUTO,
            layoutPreset = OfflineTableLayoutPreset.AUTO,
            existingStudents = roster
        )

        assertEquals(OfflineDigitScript.BENGALI, out.diagnostics.detectedScript)
        assertEquals(1, out.diagnostics.skippedNameColumnIndex)
        assertEquals(1, out.diagnostics.headerRowsSkipped)
        assertEquals(5, out.sheet.rows.size)

        for (i in rows.indices) {
            val r = out.sheet.rows[i]
            assertEquals("Row $i roll", rows[i][0], r.rollText)
            assertEquals("Row $i DB name", roster[i].name, r.name)
            assertEquals("Row $i CONT", rows[i][1], r.sheetContinuousTotal)
            assertEquals("Row $i WR", rows[i][2], r.marksText["WR"])
            assertEquals("Row $i GRAND", rows[i][3], r.sheetGrandTotal)
        }
    }

    @Test
    fun testFullEightColumnBreakdownPreset() {
        val recognizer = createRecognizer()
        val bank = loadRealDigitBank(isBengali = false, recognizer = recognizer)
            ?: Array(10) { listOf(drawSyntheticEnglishDigitPatch(it)) }

        val roster = listOf(
            Student(id = "f1", cls = 5, roll = 1, name = "আহমেদ সাদ"),
            Student(id = "f2", cls = 5, roll = 2, name = "রাইসা তাবাসসুম")
        )

        // [Roll, TB, ACT, LANG, CT, CONT, WR, GRAND] (plus skipped Name col 1)
        val rows = listOf(
            listOf("1", "8", "4", "4", "9", "25", "56", "81"),
            listOf("2", "9", "5", "4", "8", "26", "62", "88")
        )

        val gray = buildHandwrittenTableGrayImage(
            colWidths = intArrayOf(86, 260, 88, 88, 88, 88, 98, 98, 108),
            numericRows = rows,
            digitBank = bank
        )

        val out = OfflineTableScanner.scanGrayImage(
            grayImage = gray,
            recognizer = recognizer,
            clsHint = 5,
            termHint = 1,
            subKeyHint = "BAN",
            importScope = ClassSheetImportScope.BOTH,
            digitScript = OfflineDigitScript.ENGLISH,
            layoutPreset = OfflineTableLayoutPreset.ROLL_NAME_FULL_BREAKDOWN,
            existingStudents = roster
        )

        assertEquals(2, out.sheet.rows.size)
        val r0 = out.sheet.rows[0]
        assertEquals("1", r0.rollText)
        assertEquals(roster[0].name, r0.name)
        assertEquals("8", r0.marksText["TB"])
        assertEquals("4", r0.marksText["ACT"])
        assertEquals("4", r0.marksText["LANG"])
        assertEquals("9", r0.marksText["CT"])
        assertEquals("25", r0.sheetContinuousTotal)
        assertEquals("56", r0.marksText["WR"])
        assertEquals("81", r0.sheetGrandTotal)

        val r1 = out.sheet.rows[1]
        assertEquals("2", r1.rollText)
        assertEquals(roster[1].name, r1.name)
        assertEquals("9", r1.marksText["TB"])
        assertEquals("5", r1.marksText["ACT"])
        assertEquals("4", r1.marksText["LANG"])
        assertEquals("8", r1.marksText["CT"])
        assertEquals("26", r1.sheetContinuousTotal)
        assertEquals("62", r1.marksText["WR"])
        assertEquals("88", r1.sheetGrandTotal)
    }

    @Test
    fun testNoNameThreeColumnRollContWr() {
        val recognizer = createRecognizer()
        val bank = loadRealDigitBank(isBengali = false, recognizer = recognizer)
            ?: Array(10) { listOf(drawSyntheticEnglishDigitPatch(it)) }

        val roster = listOf(
            Student(id = "n1", cls = 3, roll = 1, name = "সাদিয়া রহমান"),
            Student(id = "n2", cls = 3, roll = 2, name = "জাহিদ হাসান"),
            Student(id = "n3", cls = 3, roll = 3, name = "মালিহা তাসনিম")
        )

        // [Roll, Cont, Wr] — NO Name column, narrow Roll (72px) vs Cont/Wr (120px)
        val rows = listOf(
            listOf("1", "24", "58"),
            listOf("2", "27", "61"),
            listOf("3", "21", "52")
        )

        val gray = buildHandwrittenTableGrayImage(
            colWidths = intArrayOf(72, 120, 120),
            numericRows = rows,
            digitBank = bank,
            hasNameColumn = false
        )

        val out = OfflineTableScanner.scanGrayImage(
            grayImage = gray,
            recognizer = recognizer,
            clsHint = 3,
            termHint = 1,
            subKeyHint = "BAN",
            importScope = ClassSheetImportScope.BOTH,
            digitScript = OfflineDigitScript.ENGLISH,
            layoutPreset = OfflineTableLayoutPreset.AUTO,
            existingStudents = roster
        )

        assertEquals(null, out.diagnostics.skippedNameColumnIndex)
        assertEquals(3, out.sheet.rows.size)
        for (i in rows.indices) {
            val r = out.sheet.rows[i]
            assertEquals("Row $i roll", rows[i][0], r.rollText)
            assertEquals("Row $i matched DB name", roster[i].name, r.name)
            assertEquals("Row $i CONT", rows[i][1], r.sheetContinuousTotal)
            assertEquals("Row $i WR", rows[i][2], r.marksText["WR"])
            assertEquals("Row $i computed GRAND", (rows[i][1].toInt() + rows[i][2].toInt()).toString(), r.sheetGrandTotal)
        }
    }

    @Test
    fun testNoNameFourColumnRollContWrGrand() {
        val recognizer = createRecognizer()
        val bank = loadRealDigitBank(isBengali = true, recognizer = recognizer) ?: return

        val roster = listOf(
            Student(id = "m1", cls = 4, roll = 1, name = "নাফিস করিম"),
            Student(id = "m2", cls = 4, roll = 2, name = "তাসফিয়া নূর"),
            Student(id = "m3", cls = 4, roll = 3, name = "ইফতেখার আলম")
        )

        // [Roll, Cont, Wr, Grand] — NO Name column, real BHaND Bengali digits
        val rows = listOf(
            listOf("1", "25", "55", "80"),
            listOf("2", "28", "62", "90"),
            listOf("3", "20", "48", "68")
        )

        val gray = buildHandwrittenTableGrayImage(
            colWidths = intArrayOf(74, 118, 118, 118),
            numericRows = rows,
            digitBank = bank,
            hasNameColumn = false
        )

        val out = OfflineTableScanner.scanGrayImage(
            grayImage = gray,
            recognizer = recognizer,
            clsHint = 4,
            termHint = 1,
            subKeyHint = "BAN",
            importScope = ClassSheetImportScope.BOTH,
            digitScript = OfflineDigitScript.BENGALI,
            layoutPreset = OfflineTableLayoutPreset.ROLL_NAME_CONT_WR_TOTAL,
            existingStudents = roster
        )

        assertEquals(null, out.diagnostics.skippedNameColumnIndex)
        assertEquals(3, out.sheet.rows.size)
        for (i in rows.indices) {
            val r = out.sheet.rows[i]
            assertEquals("Row $i roll", rows[i][0], r.rollText)
            assertEquals("Row $i matched DB name", roster[i].name, r.name)
            assertEquals("Row $i CONT", rows[i][1], r.sheetContinuousTotal)
            assertEquals("Row $i WR", rows[i][2], r.marksText["WR"])
            assertEquals("Row $i GRAND", rows[i][3], r.sheetGrandTotal)
        }
    }
}
