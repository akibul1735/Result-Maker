package com.kgps.result.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

data class PreparedImageStats(
    val width: Int,
    val height: Int,
    val byteSize: Int,
    val qualityUsed: Int,
    val documentEnhanced: Boolean
)

data class PreparedVisionImage(
    val base64: String,
    val byteSize: Int,
    val sha256Hex: String,
    val width: Int,
    val height: Int,
    val qualityUsed: Int,
    val documentEnhanced: Boolean
)

object VisionImageOptimizer {

    private const val TARGET_MAX_LONGEST_PX = 2200
    private const val FALLBACK_MAX_LONGEST_PX = 1850
    private const val SOFT_MAX_BYTES = 820_000
    private const val HARD_MAX_BYTES = 1_050_000
    private const val RESCALE_TRIGGER_BYTES = 1_200_000

    fun rotate90Clockwise(src: Bitmap): Bitmap {
        val matrix = Matrix().apply { postRotate(90f) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    fun cropNormalized(
        src: Bitmap,
        leftFrac: Float,
        topFrac: Float,
        rightFrac: Float,
        bottomFrac: Float
    ): Bitmap {
        val l = leftFrac.coerceIn(0f, 0.9f)
        val t = topFrac.coerceIn(0f, 0.9f)
        val r = rightFrac.coerceIn(l + 0.08f, 1f)
        val b = bottomFrac.coerceIn(t + 0.08f, 1f)

        val x = (l * src.width).toInt().coerceIn(0, (src.width - 32).coerceAtLeast(0))
        val y = (t * src.height).toInt().coerceIn(0, (src.height - 32).coerceAtLeast(0))
        val w = ((r - l) * src.width).toInt().coerceIn(32.coerceAtMost(src.width - x), src.width - x)
        val h = ((b - t) * src.height).toInt().coerceIn(32.coerceAtMost(src.height - y), src.height - y)
        return Bitmap.createBitmap(src, x, y, w, h)
    }

    fun scaleDownIfNeeded(src: Bitmap, maxLongestSide: Int): Bitmap {
        val longest = maxOf(src.width, src.height)
        if (longest <= maxLongestSide) return src
        val ratio = maxLongestSide.toFloat() / longest
        val newW = (src.width * ratio).toInt().coerceAtLeast(1)
        val newH = (src.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, newW, newH, true)
    }

    /**
     * Converts a marksheet photo into a crisp high-contrast luminance image:
     * removes RGB color noise (saving ~45-55% JPEG bytes at the same resolution/quality)
     * while keeping handwritten & printed digits sharp and legible.
     */
    fun enhanceDocumentBitmap(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        val grayMatrix = ColorMatrix().apply { setSaturation(0f) }
        // Gentle contrast boost + slight paper lift without blowing out light pen/pencil digits
        val contrast = 1.16f
        val translate = -12f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        grayMatrix.postConcat(contrastMatrix)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(grayMatrix)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    fun computeStatsOnly(src: Bitmap, documentEnhance: Boolean): PreparedImageStats {
        val prepared = prepareInternal(src, documentEnhance, encodeBase64 = false)
        return PreparedImageStats(
            width = prepared.width,
            height = prepared.height,
            byteSize = prepared.byteSize,
            qualityUsed = prepared.qualityUsed,
            documentEnhanced = prepared.documentEnhanced
        )
    }

    fun prepareForVisionUpload(src: Bitmap, documentEnhance: Boolean): PreparedVisionImage {
        return prepareInternal(src, documentEnhance, encodeBase64 = true)
    }

    private fun prepareInternal(
        src: Bitmap,
        documentEnhance: Boolean,
        encodeBase64: Boolean
    ): PreparedVisionImage {
        var working = scaleDownIfNeeded(src, TARGET_MAX_LONGEST_PX)
        if (documentEnhance) {
            working = enhanceDocumentBitmap(working)
        }

        var usedQuality = 86
        var jpegBytes = compressJpeg(working, usedQuality)

        if (jpegBytes.size > SOFT_MAX_BYTES) {
            usedQuality = 82
            jpegBytes = compressJpeg(working, usedQuality)
        }
        if (jpegBytes.size > HARD_MAX_BYTES) {
            usedQuality = 78
            jpegBytes = compressJpeg(working, usedQuality)
        }
        if (jpegBytes.size > RESCALE_TRIGGER_BYTES) {
            working = scaleDownIfNeeded(working, FALLBACK_MAX_LONGEST_PX)
            usedQuality = 82
            jpegBytes = compressJpeg(working, usedQuality)
        }

        val shaHex = if (encodeBase64) {
            MessageDigest.getInstance("SHA-256").digest(jpegBytes).joinToString("") { "%02x".format(it) }
        } else {
            ""
        }
        val b64 = if (encodeBase64) Base64.encodeToString(jpegBytes, Base64.NO_WRAP) else ""

        return PreparedVisionImage(
            base64 = b64,
            byteSize = jpegBytes.size,
            sha256Hex = shaHex,
            width = working.width,
            height = working.height,
            qualityUsed = usedQuality,
            documentEnhanced = documentEnhance
        )
    }

    private fun compressJpeg(bmp: Bitmap, quality: Int): ByteArray {
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return out.toByteArray()
    }
}
