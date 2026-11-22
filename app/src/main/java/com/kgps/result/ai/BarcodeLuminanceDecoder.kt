package com.kgps.result.ai

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.oned.Code128Reader
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pure-Kotlin Code-128 luminance enhancement & decoding pipeline.
 * Recovers camera-captured, shadowy, washed-out, slightly blurred, or edge-cropped
 * barcodes without relying on Android graphics APIs so it stays fast and JVM-testable.
 */
object BarcodeLuminanceDecoder {

    private val barcodeHints = mapOf(
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.CODE_128)
    )

    private class ByteArrayLuminanceSource(
        private val lumWidth: Int,
        private val lumHeight: Int,
        private val lum: ByteArray
    ) : LuminanceSource(lumWidth, lumHeight) {

        override fun getRow(y: Int, row: ByteArray?): ByteArray {
            val out = if (row == null || row.size < lumWidth) ByteArray(lumWidth) else row
            System.arraycopy(lum, y * lumWidth, out, 0, lumWidth)
            return out
        }

        override fun getMatrix(): ByteArray = lum
    }

    fun argbToLuminance(pixels: IntArray): ByteArray {
        val out = ByteArray(pixels.size)
        for (i in pixels.indices) {
            val p = pixels[i]
            val a = (p ushr 24) and 0xFF
            val r = (p ushr 16) and 0xFF
            val g = (p ushr 8) and 0xFF
            val b = p and 0xFF
            val lum = (r + (g shl 1) + b) shr 2
            val composited = when {
                a >= 250 -> lum
                a <= 8 -> 255
                else -> (lum * a + 255 * (255 - a)) / 255
            }
            out[i] = composited.toByte()
        }
        return out
    }

    fun decodeCode128(
        width: Int,
        height: Int,
        luma: ByteArray,
        addQuietZone: Boolean = true
    ): String? {
        if (width < 16 || height < 4 || luma.size < width * height) return null

        // Pass 1: Direct decode (fastest path for clean PDFs/screenshots/sharp photos)
        val (rawW, rawH, rawPadded) = maybePadQuietZone(luma, width, height, addQuietZone)
        tryDecodeRawBytes(rawW, rawH, rawPadded)?.let { return it }

        // Pass 2: Enhance BEFORE adding synthetic 255 white quiet-zone padding so edge segments
        // normalize accurately to the true local paper/shadow ceiling instead of synthetic white.
        val smoothed = verticalSmooth3(luma, width, height)
        val stretched = segmentedContrastStretch(smoothed, width, height, segments = 8)
        val (stW, stH, stPadded) = maybePadQuietZone(stretched, width, height, addQuietZone)
        tryDecodeRawBytes(stW, stH, stPadded)?.let { return it }

        // Pass 3: Mild horizontal bar unsharp filter + midtone split
        val sharpened = horizontalSharpenBars(stretched, width, height)
        val (shW, shH, shPadded) = maybePadQuietZone(sharpened, width, height, addQuietZone)
        tryDecodeRawBytes(shW, shH, shPadded)?.let { return it }

        // Pass 4: For narrow crops (<1100px wide) with sub-2px bars, 2x bilinear interpolation
        // resolves fractional bar widths before binarization.
        if (width < 1100) {
            val upW = width * 2
            val upH = height * 2
            val upStretched = upscaleLuma2x(stretched, width, height)
            val (usW, usH, usPadded) = maybePadQuietZone(upStretched, upW, upH, addQuietZone)
            tryDecodeRawBytes(usW, usH, usPadded)?.let { return it }

            val upPreSharpened = upscaleLuma2x(sharpened, width, height)
            val (upW2, upH2, upPadded) = maybePadQuietZone(upPreSharpened, upW, upH, addQuietZone)
            tryDecodeRawBytes(upW2, upH2, upPadded)?.let { return it }

            tryThresholdSweep(upStretched, upW, upH, addQuietZone)?.let { return it }
            tryThresholdSweep(upPreSharpened, upW, upH, addQuietZone)?.let { return it }
        } else {
            tryThresholdSweep(stretched, width, height, addQuietZone)?.let { return it }
            tryThresholdSweep(sharpened, width, height, addQuietZone)?.let { return it }
        }

        return null
    }

    private fun tryThresholdSweep(
        normLuma: ByteArray,
        width: Int,
        height: Int,
        addQuietZone: Boolean
    ): String? {
        val thresholds = intArrayOf(132, 152, 112, 168, 92)
        val bin = ByteArray(normLuma.size)
        for (thresh in thresholds) {
            for (i in normLuma.indices) {
                val v = normLuma[i].toInt() and 0xFF
                bin[i] = if (v < thresh) 0 else 0xFF.toByte()
            }
            val (bw, bh, padded) = maybePadQuietZone(bin, width, height, addQuietZone)
            tryDecodeRawBytes(bw, bh, padded)?.let { return it }
        }
        return null
    }

    private fun maybePadQuietZone(
        src: ByteArray,
        width: Int,
        height: Int,
        addQuietZone: Boolean
    ): Triple<Int, Int, ByteArray> {
        return if (addQuietZone) {
            padQuietZone(src, width, height, padX = 48, padY = 8)
        } else {
            Triple(width, height, src)
        }
    }

    internal fun upscaleLuma2x(src: ByteArray, width: Int, height: Int): ByteArray {
        val outW = width * 2
        val outH = height * 2
        val dst = ByteArray(outW * outH)
        for (oy in 0 until outH) {
            val sy = oy * 0.5f
            val y0 = sy.toInt().coerceIn(0, height - 1)
            val y1 = min(height - 1, y0 + 1)
            val fy = sy - y0
            val r0 = y0 * width
            val r1 = y1 * width
            val outRow = oy * outW
            for (ox in 0 until outW) {
                val sx = ox * 0.5f
                val x0 = sx.toInt().coerceIn(0, width - 1)
                val x1 = min(width - 1, x0 + 1)
                val fx = sx - x0
                val p00 = src[r0 + x0].toInt() and 0xFF
                val p10 = src[r0 + x1].toInt() and 0xFF
                val p01 = src[r1 + x0].toInt() and 0xFF
                val p11 = src[r1 + x1].toInt() and 0xFF
                val top = p00 + (p10 - p00) * fx
                val bot = p01 + (p11 - p01) * fx
                dst[outRow + ox] = (top + (bot - top) * fy).roundToInt().coerceIn(0, 255).toByte()
            }
        }
        return dst
    }

    internal fun padQuietZone(
        src: ByteArray,
        width: Int,
        height: Int,
        padX: Int = 48,
        padY: Int = 8
    ): Triple<Int, Int, ByteArray> {
        val outW = width + padX * 2
        val outH = height + padY * 2
        val dst = ByteArray(outW * outH) { 0xFF.toByte() }
        for (y in 0 until height) {
            System.arraycopy(src, y * width, dst, (y + padY) * outW + padX, width)
        }
        return Triple(outW, outH, dst)
    }

    internal fun verticalSmooth3(src: ByteArray, width: Int, height: Int): ByteArray {
        if (height < 3) return src.copyOf()
        val dst = ByteArray(width * height)
        System.arraycopy(src, 0, dst, 0, width)
        System.arraycopy(src, (height - 1) * width, dst, (height - 1) * width, width)
        for (y in 1 until height - 1) {
            val prev = (y - 1) * width
            val cur = y * width
            val next = (y + 1) * width
            for (x in 0 until width) {
                val p0 = src[prev + x].toInt() and 0xFF
                val p1 = src[cur + x].toInt() and 0xFF
                val p2 = src[next + x].toInt() and 0xFF
                dst[cur + x] = ((p0 + (p1 shl 1) + p2) shr 2).toByte()
            }
        }
        return dst
    }

    internal fun segmentedContrastStretch(
        src: ByteArray,
        width: Int,
        height: Int,
        segments: Int = 8
    ): ByteArray {
        val segCount = segments.coerceIn(1, 16)
        val yStart = (height * 0.08f).toInt().coerceIn(0, (height - 1).coerceAtLeast(0))
        val yEnd = (height * 0.92f).toInt().coerceIn(yStart + 1, height)

        val (globalLow, globalHigh) = percentileRange(src, width, 0, width, yStart, yEnd, 0.03f, 0.92f)
        val globalSpan = (globalHigh - globalLow).coerceAtLeast(1)
        if (globalSpan < 14) return src.copyOf()

        val segFloors = FloatArray(segCount)
        val segCeils = FloatArray(segCount)
        val segCenters = FloatArray(segCount)
        val sliceW = width.toFloat() / segCount.toFloat()
        val margin = (sliceW * 0.25f).roundToInt()

        for (s in 0 until segCount) {
            val sx0 = ((s * sliceW).toInt() - margin).coerceIn(0, width - 1)
            val sx1 = (((s + 1) * sliceW).toInt() + margin).coerceIn(sx0 + 1, width)
            segCenters[s] = (s + 0.5f) * sliceW

            val (low, high) = percentileRange(src, width, sx0, sx1, yStart, yEnd, 0.03f, 0.92f)
            val span = high - low
            // Guard against blank white margins amplification
            if (span >= max(24, (globalSpan * 0.42f).roundToInt())) {
                segFloors[s] = low.toFloat()
                segCeils[s] = high.toFloat()
            } else {
                segFloors[s] = globalLow.toFloat()
                segCeils[s] = globalHigh.toFloat()
            }
        }

        // Smooth segment bounds slightly so illumination changes smoothly across wide barcodes
        if (segCount >= 3) {
            val smoothF = segFloors.copyOf()
            val smoothC = segCeils.copyOf()
            for (s in 1 until segCount - 1) {
                smoothF[s] = (segFloors[s - 1] + segFloors[s] * 2f + segFloors[s + 1]) / 4f
                smoothC[s] = (segCeils[s - 1] + segCeils[s] * 2f + segCeils[s + 1]) / 4f
            }
            smoothF.copyInto(segFloors)
            smoothC.copyInto(segCeils)
        }

        // Precompute per-column floor & inverse scale factor for O(W*H) fast mapping
        val colFloor = FloatArray(width)
        val colInvSpan = FloatArray(width)
        for (x in 0 until width) {
            val xf = x.toFloat()
            val (f, c) = when {
                segCount == 1 || xf <= segCenters[0] -> segFloors[0] to segCeils[0]
                xf >= segCenters[segCount - 1] -> segFloors[segCount - 1] to segCeils[segCount - 1]
                else -> {
                    var idx = 0
                    while (idx + 1 < segCount && segCenters[idx + 1] < xf) idx++
                    val spanX = (segCenters[idx + 1] - segCenters[idx]).coerceAtLeast(1f)
                    val t = ((xf - segCenters[idx]) / spanX).coerceIn(0f, 1f)
                    val fInterp = segFloors[idx] + (segFloors[idx + 1] - segFloors[idx]) * t
                    val cInterp = segCeils[idx] + (segCeils[idx + 1] - segCeils[idx]) * t
                    fInterp to cInterp
                }
            }
            colFloor[x] = f
            colInvSpan[x] = 255f / max(16f, c - f)
        }

        val dst = ByteArray(width * height)
        for (y in 0 until height) {
            val rowOff = y * width
            for (x in 0 until width) {
                val v = src[rowOff + x].toInt() and 0xFF
                val norm = ((v - colFloor[x]) * colInvSpan[x]).roundToInt().coerceIn(0, 255)
                dst[rowOff + x] = norm.toByte()
            }
        }
        return dst
    }

    internal fun horizontalSharpenBars(src: ByteArray, width: Int, height: Int): ByteArray {
        val dst = ByteArray(width * height)
        val dx = (width / 320f).roundToInt().coerceIn(1, 3)
        for (y in 0 until height) {
            val rowOff = y * width
            for (x in 0 until width) {
                val c = src[rowOff + x].toInt() and 0xFF
                val sharp = if (x >= dx && x + dx < width) {
                    val l = src[rowOff + x - dx].toInt() and 0xFF
                    val r = src[rowOff + x + dx].toInt() and 0xFF
                    (c * 2 - ((l + r) shr 1)).coerceIn(0, 255)
                } else {
                    c
                }
                // Gentle midtone contrast push so narrow anti-aliased bars split cleanly
                val pushed = when {
                    sharp < 110 -> (sharp * 0.78f).roundToInt()
                    sharp > 155 -> min(255, 255 - ((255 - sharp) * 0.78f).roundToInt())
                    else -> sharp
                }
                dst[rowOff + x] = pushed.coerceIn(0, 255).toByte()
            }
        }
        return dst
    }

    private fun percentileRange(
        src: ByteArray,
        stride: Int,
        x0: Int,
        x1: Int,
        y0: Int,
        y1: Int,
        lowFrac: Float,
        highFrac: Float
    ): Pair<Int, Int> {
        val hist = IntArray(256)
        var total = 0
        for (y in y0 until y1) {
            val off = y * stride
            for (x in x0 until x1) {
                hist[src[off + x].toInt() and 0xFF]++
                total++
            }
        }
        if (total <= 0) return 0 to 255
        val lowTarget = (total * lowFrac).roundToInt().coerceAtLeast(1)
        val highTarget = (total * highFrac).roundToInt().coerceIn(lowTarget, total)
        var acc = 0
        var lowVal = 0
        var highVal = 255
        var foundLow = false
        for (b in 0..255) {
            acc += hist[b]
            if (!foundLow && acc >= lowTarget) {
                lowVal = b
                foundLow = true
            }
            if (acc >= highTarget) {
                highVal = b
                break
            }
        }
        return lowVal to highVal
    }

    private fun tryDecodeRawBytes(width: Int, height: Int, lum: ByteArray): String? {
        val source = ByteArrayLuminanceSource(width, height, lum)
        val reader = Code128Reader()
        try {
            val text = reader.decode(BinaryBitmap(GlobalHistogramBinarizer(source)), barcodeHints).text
            if (!text.isNullOrBlank()) return text
        } catch (_: Exception) {
            // Fall through to HybridBinarizer
        } finally {
            reader.reset()
        }
        return try {
            reader.decode(BinaryBitmap(HybridBinarizer(source)), barcodeHints).text
                ?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        } finally {
            reader.reset()
        }
    }
}
