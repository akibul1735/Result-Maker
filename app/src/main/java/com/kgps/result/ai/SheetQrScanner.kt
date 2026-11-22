package com.kgps.result.ai

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.kgps.result.BlankSheetQrMeta
import com.kgps.result.SheetQrCodec
import kotlin.math.roundToInt

/**
 * নির্বাচিত/তোলা শীটের ছবি থেকে সম্পূর্ণ অফলাইনে ZXing দিয়ে QR কোড ডিকোড করে।
 * আগে শীর্ষ-ডান কোণায় খোঁজে, না পেলে অন্যান্য কোণা ও পূর্ণ ছবিতে খুঁজে বের করে।
 */
object SheetQrScanner {

    private data class CropRegion(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    )

    private val priorityRegions = listOf(
        // ১. স্বাভাবিক ফাঁকা শীটের শীর্ষ-ডান কোণা (সর্বোচ্চ অগ্রাধিকার)
        CropRegion(0.56f, 0.00f, 1.00f, 0.34f),
        // ২. শীর্ষ-অর্ধেক অংশ (ক্যামেরা একটু বেঁকে থাকলে বা শিরোনাম নিচে নামলে)
        CropRegion(0.42f, 0.00f, 1.00f, 0.44f),
        // ৩. ঘোরানো বা উল্টানো ছবির অন্য ৩ কোণা
        CropRegion(0.00f, 0.00f, 0.44f, 0.34f),
        CropRegion(0.56f, 0.66f, 1.00f, 1.00f),
        CropRegion(0.00f, 0.66f, 0.44f, 1.00f)
    )

    private val decodeHints = mapOf(
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.CHARACTER_SET to "UTF-8"
    )

    fun decodeFromBitmap(bitmap: Bitmap): BlankSheetQrMeta? {
        if (bitmap.width < 32 || bitmap.height < 32) return null

        // ১) কোণা ও হেডার ক্রপ অঞ্চলে দ্রুত ডিকোড চেষ্টা (উচ্চ রেজোলিউশন সংরক্ষিত থাকে)
        for (region in priorityRegions) {
            val crop = extractNormalizedCrop(bitmap, region) ?: continue
            try {
                val upscaled = upscaleIfSmall(crop, minSideTarget = 420)
                try {
                    decodeSingleBitmap(upscaled)?.let { return it }
                } finally {
                    if (upscaled !== crop) upscaled.recycle()
                }
            } finally {
                if (crop !== bitmap) crop.recycle()
            }
        }

        // ২) পূর্ণ ছবিতে চেষ্টা (যেমন: ব্যবহারকারী কেবল QR অংশ বা উপরের ব্যানার ক্রপ করে দিলে)
        val fullScaled = downscaleIfTooLarge(bitmap, maxLongestSide = 1600)
        return try {
            val working = upscaleIfSmall(fullScaled, minSideTarget = 380)
            try {
                decodeSingleBitmap(working)
            } finally {
                if (working !== fullScaled) working.recycle()
            }
        } finally {
            if (fullScaled !== bitmap) fullScaled.recycle()
        }
    }

    private fun decodeSingleBitmap(bmp: Bitmap): BlankSheetQrMeta? {
        val w = bmp.width
        val h = bmp.height
        if (w <= 0 || h <= 0) return null
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        val source = RGBLuminanceSource(w, h, pixels)
        val reader = QRCodeReader()

        val rawText = try {
            reader.decode(BinaryBitmap(HybridBinarizer(source)), decodeHints).text
        } catch (_: Exception) {
            reader.reset()
            try {
                reader.decode(BinaryBitmap(GlobalHistogramBinarizer(source)), decodeHints).text
            } catch (_: Exception) {
                null
            }
        }

        return rawText?.let { SheetQrCodec.decodePayload(it) }
    }

    private fun extractNormalizedCrop(src: Bitmap, region: CropRegion): Bitmap? {
        val x = (region.left * src.width).roundToInt().coerceIn(0, (src.width - 16).coerceAtLeast(0))
        val y = (region.top * src.height).roundToInt().coerceIn(0, (src.height - 16).coerceAtLeast(0))
        val w = ((region.right - region.left) * src.width).roundToInt().coerceIn(16, src.width - x)
        val h = ((region.bottom - region.top) * src.height).roundToInt().coerceIn(16, src.height - y)
        if (w < 24 || h < 24) return null
        return runCatching { Bitmap.createBitmap(src, x, y, w, h) }.getOrNull()
    }

    private fun upscaleIfSmall(src: Bitmap, minSideTarget: Int): Bitmap {
        val minSide = minOf(src.width, src.height)
        if (minSide >= minSideTarget || minSide <= 0) return src
        val factor = (minSideTarget.toFloat() / minSide).coerceIn(1f, 3f)
        val nw = (src.width * factor).roundToInt().coerceAtLeast(src.width)
        val nh = (src.height * factor).roundToInt().coerceAtLeast(src.height)
        return Bitmap.createScaledBitmap(src, nw, nh, false)
    }

    private fun downscaleIfTooLarge(src: Bitmap, maxLongestSide: Int): Bitmap {
        val longest = maxOf(src.width, src.height)
        if (longest <= maxLongestSide) return src
        val ratio = maxLongestSide.toFloat() / longest
        val nw = (src.width * ratio).roundToInt().coerceAtLeast(1)
        val nh = (src.height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, nw, nh, true)
    }
}
