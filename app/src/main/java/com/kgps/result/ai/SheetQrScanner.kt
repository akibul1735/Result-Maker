package com.kgps.result.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
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

data class SheetScanDetection(
    val templateId: String? = null,
    val thinQrRawText: String? = null,
    val offlineQrRawText: String? = null,
    val offlineQrMeta: BlankSheetQrMeta? = null,
    val barcodeRawText: String? = null,
    val barcodeMeta: BlankSheetQrMeta? = null,
    val detectedFromThinQr: Boolean = false,
    val detectedFromOfflineQr: Boolean = false,
    val detectedFromBarcode: Boolean = false
) {
    val bestLocalMeta: BlankSheetQrMeta?
        get() = offlineQrMeta ?: barcodeMeta

    val detectedCount: Int
        get() = (if (detectedFromThinQr) 1 else 0) +
            (if (detectedFromOfflineQr) 1 else 0) +
            (if (detectedFromBarcode) 1 else 0)

    val sourceLabelBn: String
        get() = when {
            detectedFromThinQr && detectedFromOfflineQr -> "ডুয়াল স্মার্ট QR"
            detectedFromOfflineQr -> "অফলাইন QR"
            detectedFromThinQr -> "Firestore/ক্যাশ QR"
            detectedFromBarcode -> "নিচের বারকোড"
            else -> ""
        }
}

/**
 * নির্বাচিত/তোলা শীটের ছবি থেকে সম্পূর্ণ অফলাইনে ZXing দিয়ে ৩টি চ্যানেল স্ক্যান করে:
 * ১) শীর্ষ-বাম অতি-পাতলা Firestore/Cache ID QR (`KT1:...`)
 * ২) শীর্ষ-ডান কমপ্যাক্ট অফলাইন QR (`K2/...` বা পুরনো JSON)
 * ৩) নিচের ফুটার Code-128 বারকোড (`C3-BAN-T1-FULL-TID`)
 */
object SheetQrScanner {

    private data class CropRegion(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    )

    // বাম ও ডান QR কোণাকে প্রথমে অগ্রাধিকার দেওয়া হয়
    private val qrTopLeftRegion = CropRegion(0.00f, 0.00f, 0.44f, 0.34f)
    private val qrTopRightRegion = CropRegion(0.56f, 0.00f, 1.00f, 0.34f)
    private val qrSecondaryRegions = listOf(
        CropRegion(0.00f, 0.00f, 0.50f, 0.42f),
        CropRegion(0.50f, 0.00f, 1.00f, 0.42f),
        CropRegion(0.00f, 0.64f, 0.46f, 1.00f),
        CropRegion(0.54f, 0.64f, 1.00f, 1.00f)
    )

    private val barcodePrimaryRegions = listOf(
        CropRegion(0.00f, 0.84f, 0.66f, 0.99f), // পুরনো শীটের বাম-নিচের টাইট বারকোড স্ট্রিপ
        CropRegion(0.00f, 0.82f, 1.00f, 0.99f), // প্রশস্ত ফুটার বারকোড স্ট্রিপ
        CropRegion(0.00f, 0.70f, 1.00f, 1.00f), // সামান্য বাড়তি নিচের মার্জিনসহ স্ট্রিপ
        CropRegion(0.00f, 0.54f, 1.00f, 0.84f), // নিচে টেবিল/ডেস্কের ফাঁকা অংশ থাকলে
        CropRegion(0.00f, 0.00f, 1.00f, 0.24f)  // ১৮০° উল্টানো ছবির জন্য
    )

    private val barcodeTiltCandidateRegions = listOf(
        CropRegion(0.00f, 0.74f, 1.00f, 1.00f),
        CropRegion(0.00f, 0.54f, 1.00f, 0.82f)
    )

    private val barcodeTiltAnglesDeg = floatArrayOf(-2.5f, 2.5f, -5.0f, 5.0f, -8.5f, 8.5f, -12.5f, 12.5f)

    private val barcodeSideRegions = listOf(
        CropRegion(0.00f, 0.04f, 0.30f, 0.96f), // ৯০°/২৭০° বাঁকা ছবির বাম পাশ
        CropRegion(0.70f, 0.04f, 1.00f, 0.96f), // ৯০°/২৭০° বাঁকা ছবির ডান পাশ
        CropRegion(0.08f, 0.04f, 0.42f, 0.96f), // মার্জিনসহ বাম পাশ
        CropRegion(0.58f, 0.04f, 0.92f, 0.96f)  // মার্জিনসহ ডান পাশ
    )

    private val barcodeSlidingBands = listOf(
        CropRegion(0.00f, 0.36f, 1.00f, 0.64f),
        CropRegion(0.00f, 0.18f, 1.00f, 0.44f)
    )

    private val qrHints = mapOf(
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.CHARACTER_SET to "UTF-8"
    )

    private val bitmapFilterPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    fun decodeFromBitmap(bitmap: Bitmap): BlankSheetQrMeta? =
        decodeAllFromBitmap(bitmap).bestLocalMeta

    fun decodeAllFromBitmap(bitmap: Bitmap): SheetScanDetection {
        if (bitmap.width < 32 || bitmap.height < 12) return SheetScanDetection()

        var templateId: String? = null
        var thinQrRaw: String? = null
        var offlineQrRaw: String? = null
        var offlineMeta: BlankSheetQrMeta? = null
        var barcodeRaw: String? = null
        var barcodeMeta: BlankSheetQrMeta? = null
        var fromThinQr = false
        var fromOfflineQr = false
        var fromBarcode = false

        fun handleDecodedQrText(raw: String) {
            val ch = SheetQrCodec.decodeChannel(raw)
            if (ch.templateId != null && templateId == null) {
                templateId = ch.templateId
            }
            if (ch.meta == null && ch.templateId != null && !fromThinQr) {
                fromThinQr = true
                thinQrRaw = raw.trim()
            }
            if (ch.meta != null && offlineMeta == null) {
                offlineMeta = ch.meta
                fromOfflineQr = true
                offlineQrRaw = raw.trim()
                if (templateId == null && ch.meta.templateId.isNotBlank()) {
                    templateId = ch.meta.templateId
                }
            }
        }

        fun handleDecodedBarcodeText(rawBar: String): Boolean {
            val ch = SheetQrCodec.decodeChannel(rawBar)
            if (ch.templateId != null && templateId == null) {
                templateId = ch.templateId
            }
            return if (ch.meta != null || ch.templateId != null) {
                barcodeMeta = ch.meta
                barcodeRaw = rawBar.trim()
                fromBarcode = true
                true
            } else {
                false
            }
        }

        // ১) শীর্ষ-বাম (পাতলা Firestore/Cache QR) এবং শীর্ষ-ডান (অফলাইন কমপ্যাক্ট QR)
        for (region in listOf(qrTopLeftRegion, qrTopRightRegion)) {
            scanQrInRegion(bitmap, region)?.let(::handleDecodedQrText)
        }

        // ২) যদি উপরের কোনো একটি বাকি থাকে, সেকেন্ডারি/ঘোরানো কোণা পরীক্ষা
        if (!fromThinQr || !fromOfflineQr) {
            for (region in qrSecondaryRegions) {
                if (fromThinQr && fromOfflineQr) break
                scanQrInRegion(bitmap, region)?.let(::handleDecodedQrText)
            }
        }

        // ৩ক) নিচের ফুটার Code-128 বারকোড (সরাসরি + উচ্চ রেজল্যুশন আপস্কেল + কনট্রাস্ট ও শার্পেনিং পাস)
        val primaryRegions = if (fromThinQr || fromOfflineQr) {
            barcodePrimaryRegions.take(4)
        } else {
            barcodePrimaryRegions
        }
        for (region in primaryRegions) {
            val rawBar = scanBarcodeInRegion(bitmap, region, rotateDegrees = 0f) ?: continue
            if (handleDecodedBarcodeText(rawBar)) break
        }

        // ৩খ) সামান্য বাঁকা (+/- ২.৫° থেকে ১২.৫°) ক্যামেরা ছবির জন্য ফুটার ডেসকিউ সুইপ
        if (!fromBarcode) {
            outerTilt@ for (region in barcodeTiltCandidateRegions) {
                val crop = extractNormalizedCrop(bitmap, region) ?: continue
                try {
                    val prepared = prepareBarcodeStrip(crop, targetWidth = 1650)
                    try {
                        for (angle in barcodeTiltAnglesDeg) {
                            val tilted = rotateBitmapWhiteBg(prepared, angle)
                            try {
                                val rawBar = decodeBarcodeRaw(tilted)
                                if (rawBar != null && handleDecodedBarcodeText(rawBar)) {
                                    break@outerTilt
                                }
                            } finally {
                                tilted.recycle()
                            }
                        }
                    } finally {
                        if (prepared !== crop) prepared.recycle()
                    }
                } finally {
                    crop.recycle()
                }
            }
        }

        // ৩গ) ৯০°/২৭০° বাঁকা ছবির ক্ষেত্রে পাশের স্ট্রিপ ঘুরে চেষ্টা
        if (!fromBarcode) {
            for (region in barcodeSideRegions) {
                val rawBar = scanBarcodeInRegion(bitmap, region, rotateDegrees = 90f) ?: continue
                if (handleDecodedBarcodeText(rawBar)) break
            }
        }

        // ৩ঘ) মাঝের ব্যান্ড (যদি ছবি খুব অদ্ভুতভাবে ক্রপ/ফ্রেম করা হয়)
        if (!fromBarcode) {
            for (region in barcodeSlidingBands) {
                val rawBar = scanBarcodeInRegion(bitmap, region, rotateDegrees = 0f) ?: continue
                if (handleDecodedBarcodeText(rawBar)) break
            }
        }

        // ৪) শেষ চেষ্টা: পূর্ণ ছবিতে QR (না পাওয়া গেলে) ও পূর্ণ ছবিতে বারকোড (যেমন শুধু বারকোড ক্রপ করলে)
        if (!fromThinQr && !fromOfflineQr) {
            val fullScaled = downscaleIfTooLarge(bitmap, maxLongestSide = 1600)
            try {
                val working = upscaleIfSmall(fullScaled, minSideTarget = 420)
                try {
                    decodeQrRaw(working)?.let(::handleDecodedQrText)
                } finally {
                    if (working !== fullScaled) working.recycle()
                }
            } finally {
                if (fullScaled !== bitmap) fullScaled.recycle()
            }
        }

        if (!fromBarcode) {
            val fullForBar = downscaleIfTooLarge(bitmap, maxLongestSide = 2600)
            try {
                val workingBar = upscaleWidthIfSmall(fullForBar, minWidthTarget = 1600)
                try {
                    decodeBarcodeRaw(workingBar)?.let(::handleDecodedBarcodeText)
                    if (!fromBarcode) {
                        for (angle in floatArrayOf(-3.5f, 3.5f, 90f)) {
                            val rot = rotateBitmapWhiteBg(workingBar, angle)
                            try {
                                val rawBar = decodeBarcodeRaw(rot)
                                if (rawBar != null && handleDecodedBarcodeText(rawBar)) break
                            } finally {
                                rot.recycle()
                            }
                        }
                    }
                } finally {
                    if (workingBar !== fullForBar) workingBar.recycle()
                }
            } finally {
                if (fullForBar !== bitmap) fullForBar.recycle()
            }
        }

        return SheetScanDetection(
            templateId = templateId,
            thinQrRawText = thinQrRaw,
            offlineQrRawText = offlineQrRaw,
            offlineQrMeta = offlineMeta,
            barcodeRawText = barcodeRaw,
            barcodeMeta = barcodeMeta,
            detectedFromThinQr = fromThinQr,
            detectedFromOfflineQr = fromOfflineQr,
            detectedFromBarcode = fromBarcode
        )
    }

    private fun scanQrInRegion(bitmap: Bitmap, region: CropRegion): String? {
        val crop = extractNormalizedCrop(bitmap, region) ?: return null
        return try {
            val upscaled = upscaleIfSmall(crop, minSideTarget = 440)
            try {
                decodeQrRaw(upscaled)
            } finally {
                if (upscaled !== crop) upscaled.recycle()
            }
        } finally {
            if (crop !== bitmap) crop.recycle()
        }
    }

    private fun scanBarcodeInRegion(
        bitmap: Bitmap,
        region: CropRegion,
        rotateDegrees: Float
    ): String? {
        val crop = extractNormalizedCrop(bitmap, region) ?: return null
        return try {
            val oriented = if (rotateDegrees == 0f) crop else rotateBitmapWhiteBg(crop, rotateDegrees)
            try {
                val prepared = prepareBarcodeStrip(oriented, targetWidth = 1800)
                try {
                    decodeBarcodeRaw(prepared)
                } finally {
                    if (prepared !== oriented) prepared.recycle()
                }
            } finally {
                if (oriented !== crop) oriented.recycle()
            }
        } finally {
            if (crop !== bitmap) crop.recycle()
        }
    }

    private fun prepareBarcodeStrip(src: Bitmap, targetWidth: Int): Bitmap {
        val downscaled = downscaleIfTooLarge(src, maxLongestSide = 2600)
        return try {
            val w = downscaled.width.coerceAtLeast(1)
            val h = downscaled.height.coerceAtLeast(1)
            val needWidthScale = if (w < targetWidth) (targetWidth.toFloat() / w).coerceIn(1f, 3.5f) else 1f
            val needHeightScale = if (h < 72) (72f / h).coerceIn(1f, 3.5f) else 1f
            val scale = maxOf(needWidthScale, needHeightScale)
            if (scale <= 1.05f) {
                downscaled
            } else {
                val nw = (w * scale).roundToInt().coerceIn(w, 2800)
                val nh = (h * scale).roundToInt().coerceAtLeast(h)
                Bitmap.createScaledBitmap(downscaled, nw, nh, true)
            }
        } catch (_: Exception) {
            downscaled
        }.also { result ->
            if (result !== downscaled && downscaled !== src) {
                downscaled.recycle()
            }
        }
    }

    private fun decodeQrRaw(bmp: Bitmap): String? {
        val w = bmp.width
        val h = bmp.height
        if (w <= 0 || h <= 0) return null
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        val source = RGBLuminanceSource(w, h, pixels)
        val reader = QRCodeReader()

        return try {
            reader.decode(BinaryBitmap(HybridBinarizer(source)), qrHints).text
        } catch (_: Exception) {
            reader.reset()
            try {
                reader.decode(BinaryBitmap(GlobalHistogramBinarizer(source)), qrHints).text
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun decodeBarcodeRaw(bmp: Bitmap): String? {
        val w = bmp.width
        val h = bmp.height
        if (w < 16 || h < 4) return null
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        val lum = BarcodeLuminanceDecoder.argbToLuminance(pixels)
        return BarcodeLuminanceDecoder.decodeCode128(w, h, lum, addQuietZone = true)
    }

    private fun extractNormalizedCrop(src: Bitmap, region: CropRegion): Bitmap? {
        val minW = minOf(24, src.width)
        val minH = minOf(12, src.height)
        val x = (region.left * src.width).roundToInt().coerceIn(0, (src.width - minW).coerceAtLeast(0))
        val y = (region.top * src.height).roundToInt().coerceIn(0, (src.height - minH).coerceAtLeast(0))
        val w = ((region.right - region.left) * src.width).roundToInt().coerceIn(minW, src.width - x)
        val h = ((region.bottom - region.top) * src.height).roundToInt().coerceIn(minH, src.height - y)
        if (w < 16 || h < 8) return null
        return runCatching { Bitmap.createBitmap(src, x, y, w, h) }.getOrNull()
    }

    private fun rotateBitmapWhiteBg(src: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        val dstRect = RectF(0f, 0f, src.width.toFloat(), src.height.toFloat())
        matrix.mapRect(dstRect)
        val outW = dstRect.width().roundToInt().coerceAtLeast(1)
        val outH = dstRect.height().roundToInt().coerceAtLeast(1)
        matrix.postTranslate(-dstRect.left, -dstRect.top)
        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(src, matrix, bitmapFilterPaint)
        return out
    }

    private fun upscaleIfSmall(src: Bitmap, minSideTarget: Int): Bitmap {
        val minSide = minOf(src.width, src.height)
        if (minSide >= minSideTarget || minSide <= 0) return src
        val factor = (minSideTarget.toFloat() / minSide).coerceIn(1f, 3f)
        val nw = (src.width * factor).roundToInt().coerceAtLeast(src.width)
        val nh = (src.height * factor).roundToInt().coerceAtLeast(src.height)
        return Bitmap.createScaledBitmap(src, nw, nh, true)
    }

    private fun upscaleWidthIfSmall(src: Bitmap, minWidthTarget: Int): Bitmap {
        if (src.width >= minWidthTarget || src.width <= 0) return src
        val factor = (minWidthTarget.toFloat() / src.width).coerceIn(1f, 3.5f)
        val nw = (src.width * factor).roundToInt().coerceIn(src.width, 2800)
        val nh = (src.height * factor).roundToInt().coerceAtLeast(src.height)
        return Bitmap.createScaledBitmap(src, nw, nh, true)
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

