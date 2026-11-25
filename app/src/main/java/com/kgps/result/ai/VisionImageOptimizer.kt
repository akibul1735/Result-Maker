package com.kgps.result.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
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

    private const val TARGET_MAX_LONGEST_PX = 1800
    private const val FALLBACK_MAX_LONGEST_PX = 1440
    private const val SOFT_MAX_BYTES = 480_000
    private const val HARD_MAX_BYTES = 640_000
    private const val RESCALE_TRIGGER_BYTES = 750_000

    /**
     * গ্যালারি, ফটো-পিকার বা ক্যামেরা URI থেকে নিরাপদে Bitmap ডিকোড করে:
     * ১. Android 9+ এ ImageDecoder দিয়ে সরাসরি স্কেলড ডিকোড।
     * ২. ব্যর্থ হলে URI-র বাইট একবারেই অস্থায়ী ফাইলে কপি করে (one-shot cloud/picker stream সাপোর্টসহ)
     *    BitmapFactory (inSampleSize) + EXIF রোটেশন প্রয়োগ।
     * ৩. openInputStream ব্যর্থ হলে openFileDescriptor ফ্লোব্যাক।
     */
    fun decodeUriBitmap(context: Context, uri: Uri, maxLongestSide: Int = 2400): Bitmap? {
        val resolver = context.contentResolver

        // Tier 1: ImageDecoder on Android P+
        val fromImageDecoder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val source = ImageDecoder.createSource(resolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val w = info.size.width
                    val h = info.size.height
                    val longest = maxOf(w, h)
                    if (longest > maxLongestSide) {
                        val ratio = maxLongestSide.toFloat() / longest
                        decoder.setTargetSize(
                            (w * ratio).toInt().coerceAtLeast(1),
                            (h * ratio).toInt().coerceAtLeast(1)
                        )
                    }
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } catch (_: Throwable) {
                null
            }
        } else null

        if (fromImageDecoder != null && fromImageDecoder.width > 0 && fromImageDecoder.height > 0) {
            return scaleDownIfNeeded(fromImageDecoder, maxLongestSide)
        }

        // Tier 2 & 3: Single-pass copy to temp cache file -> BitmapFactory + ExifInterface
        var tempFile: File? = null
        return try {
            tempFile = File.createTempFile("pick_img_", ".tmp", context.cacheDir)
            val copiedBytes = copyUriToFile(resolver, uri, tempFile)
            if (copiedBytes <= 0L) {
                return null
            }

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(tempFile.absolutePath, bounds)
            val rawW = bounds.outWidth
            val rawH = bounds.outHeight
            if (rawW <= 0 || rawH <= 0) {
                return null
            }

            val targetCap = (maxLongestSide * 1.35f).toInt().coerceAtLeast(maxLongestSide)
            var sampleSize = 1
            while ((maxOf(rawW, rawH) / sampleSize) > targetCap) {
                sampleSize *= 2
            }

            var decoded: Bitmap? = null
            var attemptSample = sampleSize
            for (attempt in 0 until 2) {
                decoded = try {
                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = attemptSample
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    BitmapFactory.decodeFile(tempFile.absolutePath, opts)
                } catch (_: OutOfMemoryError) {
                    attemptSample *= 2
                    null
                } catch (_: Throwable) {
                    null
                }
                if (decoded != null) break
            }
            val bmp = decoded ?: return null
            val oriented = applyExifOrientation(bmp, tempFile)
            scaleDownIfNeeded(oriented, maxLongestSide)
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { tempFile?.delete() }
        }
    }

    private fun copyUriToFile(
        resolver: android.content.ContentResolver,
        uri: Uri,
        dest: File
    ): Long {
        val fromInput = try {
            resolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { out -> input.copyTo(out) }
            } ?: 0L
        } catch (_: Throwable) {
            0L
        }
        if (fromInput > 0L) return fromInput

        return try {
            resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                FileInputStream(pfd.fileDescriptor).use { input ->
                    dest.outputStream().use { out -> input.copyTo(out) }
                }
            } ?: 0L
        } catch (_: Throwable) {
            0L
        }
    }

    private fun applyExifOrientation(src: Bitmap, file: File): Bitmap {
        val orientation = try {
            ExifInterface(file.absolutePath)
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (_: Throwable) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return src
        }
        return try {
            val rotated = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
            if (rotated !== src) src.recycle()
            rotated
        } catch (_: Throwable) {
            src
        }
    }

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
