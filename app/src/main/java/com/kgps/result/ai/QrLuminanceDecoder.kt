package com.kgps.result.ai

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader
import com.google.zxing.qrcode.QRCodeReader
import kotlin.math.max
import kotlin.math.min

/**
 * Pure-Kotlin QR luminance enhancement & decoding pipeline.
 * Recovers dense right-side K2 offline QR codes from:
 * - low-contrast / uneven phone shadows,
 * - camera blur & JPEG compression,
 * - tight dark frame borders / insufficient quiet zones on legacy sheets,
 * - small px-per-module crops via 2x bilinear interpolation + adaptive thresholding.
 */
object QrLuminanceDecoder {

    private val qrHints = mapOf(
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.CHARACTER_SET to "UTF-8"
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

    fun decodeAllQrs(
        width: Int,
        height: Int,
        luma: ByteArray,
        addQuietZone: Boolean = true,
        stopWhen: ((List<String>) -> Boolean)? = null
    ): List<String> {
        if (width < 20 || height < 20 || luma.size < width * height) return emptyList()

        val found = linkedSetOf<String>()
        fun done(): Boolean {
            if (found.isEmpty()) return false
            return stopWhen?.invoke(found.toList()) ?: true
        }

        // Pass 1: Direct decode (+ clean white quiet-zone border)
        val (rawW, rawH, rawPadded) = maybePadQuietZone(luma, width, height, addQuietZone)
        tryDecodeQrInto(rawW, rawH, rawPadded, found)
        if (done()) return found.toList()

        // Pass 2: Percentile contrast normalization [p2..p98] -> [0..255]
        val stretched = contrastNormalize(luma, width, height)
        val (stW, stH, stPadded) = maybePadQuietZone(stretched, width, height, addQuietZone)
        tryDecodeQrInto(stW, stH, stPadded, found)
        if (done()) return found.toList()

        // Pass 3: 2D unsharp mask to recover soft-focus & dense 29x29..37x37 modules
        val sharpened = sharpen2d(stretched, width, height)
        val (shW, shH, shPadded) = maybePadQuietZone(sharpened, width, height, addQuietZone)
        tryDecodeQrInto(shW, shH, shPadded, found)
        if (done()) return found.toList()

        // Pass 4: 2x bilinear upscale for small crops so HybridBinarizer 8x8 blocks resolve modules cleanly
        val minSide = min(width, height)
        val maxSide = max(width, height)
        val (workW, workH, workStretched, workSharpened) = if (minSide < 420 || maxSide < 520) {
            val upSt = upscaleLuma2x(stretched, width, height)
            val upSh = sharpen2d(upSt, width * 2, height * 2)
            Quad(width * 2, height * 2, upSt, upSh)
        } else {
            Quad(width, height, stretched, sharpened)
        }

        if (workW != width) {
            val (usW, usH, usPadded) = maybePadQuietZone(workStretched, workW, workH, addQuietZone)
            tryDecodeQrInto(usW, usH, usPadded, found)
            if (done()) return found.toList()

            val (ushW, ushH, ushPadded) = maybePadQuietZone(workSharpened, workW, workH, addQuietZone)
            tryDecodeQrInto(ushW, ushH, ushPadded, found)
            if (done()) return found.toList()
        }

        // Pass 5: Integral-image adaptive local-mean thresholding (defeats uneven shadows & black borders)
        val radius = (min(workW, workH) / 12).coerceIn(8, 36)
        val adaptiveBin = ByteArray(workW * workH)
        for (bias in intArrayOf(6, 16, -4)) {
            binarizeAdaptiveMean(workStretched, workW, workH, radius, bias, adaptiveBin)
            val (aw, ah, aPadded) = maybePadQuietZone(adaptiveBin, workW, workH, addQuietZone)
            tryDecodeQrInto(aw, ah, aPadded, found)
            if (done()) return found.toList()
        }

        // Pass 6: Global threshold sweep on sharpened luminance (recovers ink bleed / faint print)
        val globalBin = ByteArray(workW * workH)
        for (thresh in intArrayOf(134, 154, 114, 172)) {
            for (i in workSharpened.indices) {
                val v = workSharpened[i].toInt() and 0xFF
                globalBin[i] = if (v < thresh) 0 else 0xFF.toByte()
            }
            val (gw, gh, gPadded) = maybePadQuietZone(globalBin, workW, workH, addQuietZone)
            tryDecodeQrInto(gw, gh, gPadded, found)
            if (done()) return found.toList()
        }

        return found.toList()
    }

    private data class Quad(
        val w: Int,
        val h: Int,
        val stretched: ByteArray,
        val sharpened: ByteArray
    )

    private fun tryDecodeQrInto(
        width: Int,
        height: Int,
        luma: ByteArray,
        found: LinkedHashSet<String>
    ) {
        val source = ByteArrayLuminanceSource(width, height, luma)
        val multiReader = QRCodeMultiReader()
        var anyAdded = false

        runCatching {
            multiReader.decodeMultiple(BinaryBitmap(HybridBinarizer(source)), qrHints)
                .mapNotNull { it.text?.trim()?.takeIf(String::isNotEmpty) }
                .forEach { if (found.add(it)) anyAdded = true }
        }
        if (!anyAdded) {
            multiReader.reset()
            runCatching {
                multiReader.decodeMultiple(BinaryBitmap(GlobalHistogramBinarizer(source)), qrHints)
                    .mapNotNull { it.text?.trim()?.takeIf(String::isNotEmpty) }
                    .forEach { if (found.add(it)) anyAdded = true }
            }
        }
        if (!anyAdded) {
            val single = QRCodeReader()
            runCatching {
                single.decode(BinaryBitmap(HybridBinarizer(source)), qrHints)
                    .text?.trim()?.takeIf(String::isNotEmpty)
                    ?.let { if (found.add(it)) anyAdded = true }
            }
            if (!anyAdded) {
                single.reset()
                runCatching {
                    single.decode(BinaryBitmap(GlobalHistogramBinarizer(source)), qrHints)
                        .text?.trim()?.takeIf(String::isNotEmpty)
                        ?.let { found.add(it) }
                }
            }
        }
    }

    private fun maybePadQuietZone(
        src: ByteArray,
        width: Int,
        height: Int,
        addQuietZone: Boolean
    ): Triple<Int, Int, ByteArray> {
        if (!addQuietZone) return Triple(width, height, src)
        val pad = (min(width, height) / 14).coerceIn(16, 40)
        val nw = width + pad * 2
        val nh = height + pad * 2
        val out = ByteArray(nw * nh) { 0xFF.toByte() }
        for (y in 0 until height) {
            System.arraycopy(src, y * width, out, (y + pad) * nw + pad, width)
        }
        return Triple(nw, nh, out)
    }

    private fun contrastNormalize(luma: ByteArray, width: Int, height: Int): ByteArray {
        val hist = IntArray(256)
        var sampled = 0
        val step = if (width * height > 240_000) 3 else 1
        var i = 0
        while (i < luma.size) {
            hist[luma[i].toInt() and 0xFF]++
            sampled++
            i += step
        }
        if (sampled == 0) return luma

        val lowTarget = (sampled * 0.015f).toInt()
        val highTarget = (sampled * 0.985f).toInt().coerceAtLeast(lowTarget + 1)
        var acc = 0
        var pLow = 0
        var pHigh = 255
        var foundLow = false
        for (v in 0..255) {
            acc += hist[v]
            if (!foundLow && acc >= lowTarget) {
                pLow = v
                foundLow = true
            }
            if (acc >= highTarget) {
                pHigh = v
                break
            }
        }

        val range = (pHigh - pLow).coerceAtLeast(1)
        if (pLow <= 6 && pHigh >= 249) return luma

        val lut = ByteArray(256)
        for (v in 0..255) {
            val norm = ((v - pLow) * 255f / range).toInt().coerceIn(0, 255)
            lut[v] = norm.toByte()
        }
        val out = ByteArray(luma.size)
        for (idx in luma.indices) {
            out[idx] = lut[luma[idx].toInt() and 0xFF]
        }
        return out
    }

    private fun sharpen2d(luma: ByteArray, width: Int, height: Int): ByteArray {
        if (width < 3 || height < 3) return luma
        val out = ByteArray(luma.size)
        for (y in 0 until height) {
            val rowOff = y * width
            val topOff = max(0, y - 1) * width
            val botOff = min(height - 1, y + 1) * width
            for (x in 0 until width) {
                val xl = max(0, x - 1)
                val xr = min(width - 1, x + 1)
                val c = luma[rowOff + x].toInt() and 0xFF
                val n = luma[topOff + x].toInt() and 0xFF
                val s = luma[botOff + x].toInt() and 0xFF
                val w = luma[rowOff + xl].toInt() and 0xFF
                val e = luma[rowOff + xr].toInt() and 0xFF
                // Crisp gentle 2D unsharp: 3*c - 0.5*(n+s+w+e)
                val v = ((c * 6 - n - s - w - e) shr 1).coerceIn(0, 255)
                out[rowOff + x] = v.toByte()
            }
        }
        return out
    }

    private fun upscaleLuma2x(src: ByteArray, width: Int, height: Int): ByteArray {
        return BarcodeLuminanceDecoder.upscaleLuma2x(src, width, height)
    }

    private fun binarizeAdaptiveMean(
        src: ByteArray,
        width: Int,
        height: Int,
        radius: Int,
        bias: Int,
        dst: ByteArray
    ) {
        val iw = width + 1
        val integral = LongArray(iw * (height + 1))
        for (y in 0 until height) {
            var rowSum = 0L
            val srcRow = y * width
            val curRow = (y + 1) * iw
            val prevRow = y * iw
            for (x in 0 until width) {
                rowSum += (src[srcRow + x].toInt() and 0xFF)
                integral[curRow + x + 1] = integral[prevRow + x + 1] + rowSum
            }
        }

        for (y in 0 until height) {
            val y0 = max(0, y - radius)
            val y1 = min(height - 1, y + radius)
            val iy0 = y0 * iw
            val iy1 = (y1 + 1) * iw
            val boxH = (y1 - y0 + 1)
            val rowOff = y * width
            for (x in 0 until width) {
                val x0 = max(0, x - radius)
                val x1 = min(width - 1, x + radius)
                val count = boxH * (x1 - x0 + 1)
                val sum = integral[iy1 + x1 + 1] - integral[iy0 + x1 + 1] - integral[iy1 + x0] + integral[iy0 + x0]
                val mean = (sum / count).toInt()
                val v = src[rowOff + x].toInt() and 0xFF
                dst[rowOff + x] = if (v < mean - bias) 0 else 0xFF.toByte()
            }
        }
    }
}
