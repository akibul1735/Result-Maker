package com.kgps.result.ai.offline

import android.graphics.Bitmap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

data class GrayImage(
    val width: Int,
    val height: Int,
    val luma: FloatArray,      // 0f (black) .. 255f (white)
    val ink: FloatArray,       // 0f (paper) .. 1f (dark ink)
    val binary: BooleanArray   // true = foreground ink / rule line
) {
    companion object {
        fun fromBitmap(src: Bitmap, maxLongestSide: Int = 1600): GrayImage {
            val longest = max(src.width, src.height)
            val working = if (longest > maxLongestSide) {
                val ratio = maxLongestSide.toFloat() / longest
                val nw = (src.width * ratio).roundToInt().coerceAtLeast(1)
                val nh = (src.height * ratio).roundToInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(src, nw, nh, true)
            } else {
                src
            }
            val safeBmp = if (working.config == Bitmap.Config.ARGB_8888) {
                working
            } else {
                working.copy(Bitmap.Config.ARGB_8888, false)
            }
            val pixels = IntArray(safeBmp.width * safeBmp.height)
            safeBmp.getPixels(pixels, 0, safeBmp.width, 0, 0, safeBmp.width, safeBmp.height)
            return fromArgbPixels(pixels, safeBmp.width, safeBmp.height)
        }

        fun fromArgbPixels(pixels: IntArray, width: Int, height: Int): GrayImage {
            val n = width * height
            val luma = FloatArray(n)
            for (i in 0 until n) {
                val p = pixels[i]
                val r = (p ushr 16) and 0xFF
                val g = (p ushr 8) and 0xFF
                val b = p and 0xFF
                luma[i] = 0.299f * r + 0.587f * g + 0.114f * b
            }
            return buildFromLuma(width, height, luma)
        }

        fun buildFromLuma(width: Int, height: Int, luma: FloatArray): GrayImage {
            val n = width * height
            // Integral image of luma for adaptive local paper-background estimation
            val stride = width + 1
            val integral = DoubleArray((height + 1) * stride)
            for (y in 0 until height) {
                var rowSum = 0.0
                val srcOff = y * width
                val prevOff = y * stride
                val curOff = (y + 1) * stride
                for (x in 0 until width) {
                    rowSum += luma[srcOff + x].toDouble()
                    integral[curOff + x + 1] = integral[prevOff + x + 1] + rowSum
                }
            }

            val radius = max(14, min(width, height) / 26)
            val ink = FloatArray(n)
            val binary = BooleanArray(n)
            for (y in 0 until height) {
                val y0 = max(0, y - radius)
                val y1 = min(height - 1, y + radius)
                val r0 = y0 * stride
                val r1 = (y1 + 1) * stride
                val rowOff = y * width
                for (x in 0 until width) {
                    val x0 = max(0, x - radius)
                    val x1 = min(width - 1, x + radius)
                    val area = ((x1 - x0 + 1) * (y1 - y0 + 1)).toDouble()
                    val sum = integral[r1 + x1 + 1] - integral[r0 + x1 + 1] - integral[r1 + x0] + integral[r0 + x0]
                    val bg = (sum / area).toFloat()
                    val px = luma[rowOff + x]
                    val delta = bg - px
                    val isDark = (px < bg * 0.87f - 6f) || (bg < 75f && px < 45f)
                    binary[rowOff + x] = isDark
                    val normDenom = max(26f, bg * 0.40f)
                    ink[rowOff + x] = if (delta > 6f) ((delta - 6f) / normDenom).coerceIn(0f, 1f) else 0f
                }
            }
            return GrayImage(width, height, luma, ink, binary)
        }
    }
}

data class CellBounds(
    val rowIndex: Int,
    val colIndex: Int,
    val x0: Int,
    val y0: Int,
    val x1: Int,
    val y1: Int
) {
    val width: Int get() = x1 - x0
    val height: Int get() = y1 - y0
}

data class TableGridResult(
    val image: GrayImage,
    val deskewDegrees: Float,
    val colBounds: List<IntRange>,
    val rowBounds: List<IntRange>,
    val headerRowsSkipped: Int,
    val dataRowBounds: List<IntRange>,
    val allRowBoundsIncludingHeader: List<IntRange>,
    val usedLineDetectionHorizontal: Boolean,
    val usedLineDetectionVertical: Boolean
)

object TableGridSlicer {

    fun sliceTable(srcImage: GrayImage): TableGridResult {
        val (img, skewDeg) = deskewIfNeeded(srcImage)
        val w = img.width
        val h = img.height

        val rawHLines = detectHorizontalLines(img, 0, w, 0, h)
        val rawVLines = detectVerticalLines(img, 0, w, 0, h)

        val tableY0 = if (rawHLines.size >= 2) rawHLines.first() else 0
        val tableY1 = if (rawHLines.size >= 2) rawHLines.last() else h - 1
        val tableX0 = if (rawVLines.size >= 2) rawVLines.first() else 0
        val tableX1 = if (rawVLines.size >= 2) rawVLines.last() else w - 1

        // Re-detect internal lines restricted to active table box for sharper precision
        val hLines = if (rawHLines.size >= 3) {
            rawHLines
        } else {
            detectHorizontalLines(img, tableX0, tableX1 + 1, 0, h)
        }
        val vLines = if (rawVLines.size >= 3) {
            rawVLines
        } else {
            detectVerticalLines(img, 0, w, tableY0, tableY1 + 1)
        }

        val usedHLine = hLines.size >= 3
        val usedVLine = vLines.size >= 3

        val colBounds = if (usedVLine) {
            linesToSpans(vLines, minSpan = max(14, w / 45))
        } else {
            fallbackWhitespaceCols(img, tableX0, tableX1 + 1, tableY0, tableY1 + 1)
        }

        val allRows = if (usedHLine) {
            linesToSpans(hLines, minSpan = max(14, h / 65))
        } else {
            fallbackWhitespaceRows(img, tableX0, tableX1 + 1, tableY0, tableY1 + 1)
        }

        val (headerSkipped, dataRows) = filterHeaderAndEmptyRows(img, allRows, colBounds)

        return TableGridResult(
            image = img,
            deskewDegrees = skewDeg,
            colBounds = colBounds,
            rowBounds = dataRows,
            headerRowsSkipped = headerSkipped,
            dataRowBounds = dataRows,
            allRowBoundsIncludingHeader = allRows,
            usedLineDetectionHorizontal = usedHLine,
            usedLineDetectionVertical = usedVLine
        )
    }

    /**
     * Estimates table skew in [-5.5°, +5.5°] using horizontal projection energy on a 320px proxy.
     */
    private fun deskewIfNeeded(src: GrayImage): Pair<GrayImage, Float> {
        val w = src.width
        val h = src.height
        if (w < 120 || h < 120) return src to 0f

        val step = max(1, max(w, h) / 340)
        val sw = (w + step - 1) / step
        val sh = (h + step - 1) / step
        val ptsX = IntArray(sw * sh)
        val ptsY = IntArray(sw * sh)
        var ptCount = 0
        for (sy in 0 until sh) {
            val y = min(h - 1, sy * step)
            val rowOff = y * w
            for (sx in 0 until sw) {
                val x = min(w - 1, sx * step)
                if (src.binary[rowOff + x]) {
                    ptsX[ptCount] = sx - sw / 2
                    ptsY[ptCount] = sy - sh / 2
                    ptCount++
                }
            }
        }
        if (ptCount < 40) return src to 0f

        var bestAngle = 0f
        var bestScore = -1.0
        val hist = IntArray(sh + 80)
        val offset = hist.size / 2

        var deg = -5.5f
        while (deg <= 5.51f) {
            val rad = deg * (PI / 180.0)
            val sinA = sin(rad)
            val cosA = cos(rad)
            hist.fill(0)
            for (i in 0 until ptCount) {
                val ry = (ptsX[i] * sinA + ptsY[i] * cosA).roundToInt() + offset
                if (ry in 0 until hist.size) hist[ry]++
            }
            var energy = 0.0
            for (v in hist) energy += v.toDouble() * v.toDouble()
            if (energy > bestScore) {
                bestScore = energy
                bestAngle = deg
            }
            deg += 0.25f
        }

        if (abs(bestAngle) < 0.35f) return src to 0f

        // Map output (x', y') back to source (sx, sy) via inverse of forward projection R(+bestAngle)
        val rad = bestAngle * (PI / 180.0)
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()
        val cx = (w - 1) * 0.5f
        val cy = (h - 1) * 0.5f
        val outLuma = FloatArray(w * h) { 255f }
        for (y in 0 until h) {
            val dy = y - cy
            val outOff = y * w
            for (x in 0 until w) {
                val dx = x - cx
                val sx = cx + dx * cosA + dy * sinA
                val sy = cy - dx * sinA + dy * cosA
                val x0 = floor(sx.toDouble()).toInt()
                val y0 = floor(sy.toDouble()).toInt()
                if (x0 in 0 until (w - 1) && y0 in 0 until (h - 1)) {
                    val fx = sx - x0
                    val fy = sy - y0
                    val v00 = src.luma[y0 * w + x0]
                    val v10 = src.luma[y0 * w + x0 + 1]
                    val v01 = src.luma[(y0 + 1) * w + x0]
                    val v11 = src.luma[(y0 + 1) * w + x0 + 1]
                    outLuma[outOff + x] = (v00 * (1f - fx) + v10 * fx) * (1f - fy) +
                        (v01 * (1f - fx) + v11 * fx) * fy
                }
            }
        }
        return GrayImage.buildFromLuma(w, h, outLuma) to bestAngle
    }

    private fun detectHorizontalLines(
        img: GrayImage,
        xStart: Int,
        xEnd: Int,
        yStart: Int,
        yEnd: Int
    ): List<Int> {
        val spanW = (xEnd - xStart).coerceAtLeast(1)
        val minRunRequired = max(30, (spanW * 0.36f).toInt())
        val maxBridgeGap = max(3, spanW / 220)
        val hits = ArrayList<Int>()
        for (y in yStart until yEnd) {
            val rowOff = y * img.width
            var curRun = 0
            var gap = 0
            var bestRun = 0
            for (x in xStart until xEnd) {
                // Check y or y+-1 to handle slight 1px curvature/antialiasing
                val on = img.binary[rowOff + x] ||
                    (y > 0 && img.binary[(y - 1) * img.width + x]) ||
                    (y + 1 < img.height && img.binary[(y + 1) * img.width + x])
                if (on) {
                    curRun += 1 + gap
                    gap = 0
                    if (curRun > bestRun) bestRun = curRun
                } else {
                    gap++
                    if (gap > maxBridgeGap) {
                        curRun = 0
                        gap = 0
                    }
                }
            }
            if (bestRun >= minRunRequired) hits.add(y)
        }
        return clusterLinePositions(hits, mergeGap = max(4, img.height / 180))
    }

    private fun detectVerticalLines(
        img: GrayImage,
        xStart: Int,
        xEnd: Int,
        yStart: Int,
        yEnd: Int
    ): List<Int> {
        val spanH = (yEnd - yStart).coerceAtLeast(1)
        val minRunRequired = max(30, (spanH * 0.32f).toInt())
        val maxBridgeGap = max(3, spanH / 200)
        val hits = ArrayList<Int>()
        for (x in xStart until xEnd) {
            var curRun = 0
            var gap = 0
            var bestRun = 0
            for (y in yStart until yEnd) {
                val off = y * img.width + x
                val on = img.binary[off] ||
                    (x > 0 && img.binary[off - 1]) ||
                    (x + 1 < img.width && img.binary[off + 1])
                if (on) {
                    curRun += 1 + gap
                    gap = 0
                    if (curRun > bestRun) bestRun = curRun
                } else {
                    gap++
                    if (gap > maxBridgeGap) {
                        curRun = 0
                        gap = 0
                    }
                }
            }
            if (bestRun >= minRunRequired) hits.add(x)
        }
        return clusterLinePositions(hits, mergeGap = max(4, img.width / 180))
    }

    private fun clusterLinePositions(raw: List<Int>, mergeGap: Int): List<Int> {
        if (raw.isEmpty()) return emptyList()
        val out = ArrayList<Int>()
        var sum = raw[0]
        var count = 1
        var prev = raw[0]
        for (i in 1 until raw.size) {
            val v = raw[i]
            if (v - prev <= mergeGap) {
                sum += v
                count++
            } else {
                out.add((sum.toFloat() / count).roundToInt())
                sum = v
                count = 1
            }
            prev = v
        }
        out.add((sum.toFloat() / count).roundToInt())
        return out
    }

    private fun linesToSpans(lines: List<Int>, minSpan: Int): List<IntRange> {
        if (lines.size < 2) return emptyList()
        val spans = ArrayList<IntRange>()
        var start = lines[0]
        for (i in 1 until lines.size) {
            val end = lines[i]
            if (end - start >= minSpan) {
                spans.add(start..end)
                start = end
            } else {
                // Merge tiny double-line gap
                start = (start + end) / 2
            }
        }
        return spans
    }

    private fun fallbackWhitespaceRows(
        img: GrayImage,
        x0: Int,
        x1: Int,
        y0: Int,
        y1: Int
    ): List<IntRange> {
        val spanW = max(1, x1 - x0)
        val rowCount = FloatArray(y1 - y0)
        for (y in y0 until y1) {
            var c = 0
            val off = y * img.width
            for (x in x0 until x1) if (img.binary[off + x]) c++
            rowCount[y - y0] = c / spanW.toFloat()
        }
        return extractDenseBands(rowCount, offset = y0, thresh = 0.012f, minBandSize = 14, minGap = 3)
    }

    private fun fallbackWhitespaceCols(
        img: GrayImage,
        x0: Int,
        x1: Int,
        y0: Int,
        y1: Int
    ): List<IntRange> {
        val spanH = max(1, y1 - y0)
        val colCount = FloatArray(x1 - x0)
        for (x in x0 until x1) {
            var c = 0
            for (y in y0 until y1) if (img.binary[y * img.width + x]) c++
            colCount[x - x0] = c / spanH.toFloat()
        }
        return extractDenseBands(colCount, offset = x0, thresh = 0.010f, minBandSize = 14, minGap = 4)
    }

    private fun extractDenseBands(
        density: FloatArray,
        offset: Int,
        thresh: Float,
        minBandSize: Int,
        minGap: Int
    ): List<IntRange> {
        val bands = ArrayList<IntRange>()
        var inBand = false
        var start = 0
        var emptyRun = 0
        var lastActive = 0
        for (i in density.indices) {
            if (density[i] >= thresh) {
                if (!inBand) {
                    inBand = true
                    start = i
                }
                lastActive = i
                emptyRun = 0
            } else if (inBand) {
                emptyRun++
                if (emptyRun >= minGap) {
                    val len = lastActive - start + 1
                    if (len >= minBandSize) {
                        val pad = min(4, minGap)
                        bands.add((offset + max(0, start - pad))..(offset + min(density.size - 1, lastActive + pad)))
                    }
                    inBand = false
                    emptyRun = 0
                }
            }
        }
        if (inBand && lastActive - start + 1 >= minBandSize) {
            bands.add((offset + start)..(offset + lastActive))
        }
        return bands
    }

    /**
     * Skips header row(s) at the top (printed Bengali/English column titles)
     * and strips completely blank trailing rows.
     */
    private fun filterHeaderAndEmptyRows(
        img: GrayImage,
        rows: List<IntRange>,
        cols: List<IntRange>
    ): Pair<Int, List<IntRange>> {
        if (rows.isEmpty() || cols.isEmpty()) return 0 to rows

        // Measure ink fill & component complexity inside each row across columns
        val rowIsBlank = BooleanArray(rows.size)
        val rowIsHeaderLike = BooleanArray(rows.size)
        for (rIdx in rows.indices) {
            val yr = rows[rIdx]
            var activeCols = 0
            var wideWordCols = 0
            for (cIdx in cols.indices) {
                val xr = cols[cIdx]
                val stats = CellDigitSegmenter.inspectCellStats(img, xr.first, yr.first, xr.last, yr.last)
                if (!stats.isEmpty) activeCols++
                // Header cells have either >=4 disconnected glyphs or matra-connected Bengali words (wide aspect)
                val looksLikeWord = (stats.componentCount >= 4 && stats.inkSpanFraction > 0.48f) ||
                    (stats.maxComponentAspect >= 1.65f)
                if (cIdx >= 2 && looksLikeWord) {
                    wideWordCols++
                }
            }
            if (activeCols == 0) rowIsBlank[rIdx] = true
            // Also check row 0..1: if col 0 has >= 4 blobs or wide matra word (like "রোল" / "ক্রমিক"), likely header
            val col0Stats = CellDigitSegmenter.inspectCellStats(img, cols[0].first, yr.first, cols[0].last, yr.last)
            val rollLooksText = (col0Stats.componentCount >= 4 && col0Stats.inkSpanFraction > 0.46f) ||
                col0Stats.maxComponentAspect >= 1.65f
            val markColsText = cols.size >= 3 && wideWordCols >= max(1, (cols.size - 2 + 1) / 2)
            if (rIdx <= 2 && (markColsText || (rIdx == 0 && rollLooksText && activeCols >= 2))) {
                rowIsHeaderLike[rIdx] = true
            }
        }

        var startIdx = 0
        while (startIdx < min(3, rows.size) && (rowIsHeaderLike[startIdx] || rowIsBlank[startIdx])) {
            startIdx++
        }
        val filtered = ArrayList<IntRange>()
        for (i in startIdx until rows.size) {
            if (!rowIsBlank[i]) filtered.add(rows[i])
        }
        return startIdx to filtered
    }
}
