package com.kgps.result.ai

import android.graphics.Bitmap
import android.graphics.Matrix
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.oned.Code128Reader
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
 * ৩) নিচের-বাম ফুটার Code-128 বারকোড (`C3-BAN-T1-FULL-TID`)
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

    private val barcodeHorizontalRegions = listOf(
        CropRegion(0.00f, 0.78f, 0.72f, 1.00f), // নিচের বাম দিকে সরানো বারকোড
        CropRegion(0.00f, 0.70f, 1.00f, 1.00f), // পুরো নিচের স্ট্রিপ (যদি মাঝখানে বা ডানে থাকে)
        CropRegion(0.00f, 0.00f, 1.00f, 0.24f)  // উল্টানো ছবির জন্য
    )

    private val barcodeSideRegions = listOf(
        CropRegion(0.00f, 0.08f, 0.24f, 0.88f), // ৯০° বাঁকা ছবির বাম পাশ
        CropRegion(0.76f, 0.08f, 1.00f, 0.88f)  // ৯০° বাঁকা ছবির ডান পাশ
    )

    private val qrHints = mapOf(
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.CHARACTER_SET to "UTF-8"
    )

    private val barcodeHints = mapOf(
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.CODE_128)
    )

    fun decodeFromBitmap(bitmap: Bitmap): BlankSheetQrMeta? =
        decodeAllFromBitmap(bitmap).bestLocalMeta

    fun decodeAllFromBitmap(bitmap: Bitmap): SheetScanDetection {
        if (bitmap.width < 32 || bitmap.height < 32) return SheetScanDetection()

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

        // ৩) নিচের ফুটার Code-128 বারকোড (সব সময় যাচাই করা হয় যাতে Bar 1 আইকন সঠিকভাবে জ্বলে)
        val regionsToScan = if (fromThinQr || fromOfflineQr) {
            barcodeHorizontalRegions.take(2)
        } else {
            barcodeHorizontalRegions
        }
        for (region in regionsToScan) {
            val rawBar = scanBarcodeInRegion(bitmap, region, rotate90 = false) ?: continue
            val ch = SheetQrCodec.decodeChannel(rawBar)
            if (ch.templateId != null && templateId == null) {
                templateId = ch.templateId
            }
            if (ch.meta != null || ch.templateId != null) {
                barcodeMeta = ch.meta
                barcodeRaw = rawBar.trim()
                fromBarcode = true
                break
            }
        }
        // ৯০° বাঁকা ছবির ক্ষেত্রে পাশের স্ট্রিপ ঘুরে চেষ্টা
        if (!fromBarcode && (!fromOfflineQr || !fromThinQr)) {
            for (region in barcodeSideRegions) {
                val rawBar = scanBarcodeInRegion(bitmap, region, rotate90 = true) ?: continue
                val ch = SheetQrCodec.decodeChannel(rawBar)
                if (ch.templateId != null && templateId == null) templateId = ch.templateId
                if (ch.meta != null || ch.templateId != null) {
                    barcodeMeta = ch.meta
                    barcodeRaw = rawBar.trim()
                    fromBarcode = true
                    break
                }
            }
        }

        // ৪) শেষ চেষ্টা: পূর্ণ ছবিতে QR/Barcode (যেমন কেউ শুধু QR বা বারকোড অংশ ক্রপ করলে)
        if (!fromThinQr && !fromOfflineQr && !fromBarcode) {
            val fullScaled = downscaleIfTooLarge(bitmap, maxLongestSide = 1600)
            try {
                val working = upscaleIfSmall(fullScaled, minSideTarget = 420)
                try {
                    decodeQrRaw(working)?.let(::handleDecodedQrText)
                    if (!fromOfflineQr && !fromThinQr) {
                        decodeBarcodeRaw(working)?.let { rawBar ->
                            val ch = SheetQrCodec.decodeChannel(rawBar)
                            if (ch.templateId != null) templateId = ch.templateId
                            if (ch.meta != null || ch.templateId != null) {
                                barcodeMeta = ch.meta
                                barcodeRaw = rawBar.trim()
                                fromBarcode = true
                            }
                        }
                    }
                } finally {
                    if (working !== fullScaled) working.recycle()
                }
            } finally {
                if (fullScaled !== bitmap) fullScaled.recycle()
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

    private fun scanBarcodeInRegion(bitmap: Bitmap, region: CropRegion, rotate90: Boolean): String? {
        val crop = extractNormalizedCrop(bitmap, region) ?: return null
        return try {
            val oriented = if (rotate90) rotateBitmap90(crop) else crop
            try {
                val upscaled = upscaleWidthIfSmall(oriented, minWidthTarget = 900)
                try {
                    decodeBarcodeRaw(upscaled)
                } finally {
                    if (upscaled !== oriented) upscaled.recycle()
                }
            } finally {
                if (oriented !== crop) oriented.recycle()
            }
        } finally {
            if (crop !== bitmap) crop.recycle()
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
        if (w <= 0 || h <= 0) return null
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        val source = RGBLuminanceSource(w, h, pixels)
        val reader = Code128Reader()

        return try {
            reader.decode(BinaryBitmap(GlobalHistogramBinarizer(source)), barcodeHints).text
        } catch (_: Exception) {
            reader.reset()
            try {
                reader.decode(BinaryBitmap(HybridBinarizer(source)), barcodeHints).text
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun extractNormalizedCrop(src: Bitmap, region: CropRegion): Bitmap? {
        val x = (region.left * src.width).roundToInt().coerceIn(0, (src.width - 16).coerceAtLeast(0))
        val y = (region.top * src.height).roundToInt().coerceIn(0, (src.height - 16).coerceAtLeast(0))
        val w = ((region.right - region.left) * src.width).roundToInt().coerceIn(16, src.width - x)
        val h = ((region.bottom - region.top) * src.height).roundToInt().coerceIn(16, src.height - y)
        if (w < 24 || h < 24) return null
        return runCatching { Bitmap.createBitmap(src, x, y, w, h) }.getOrNull()
    }

    private fun rotateBitmap90(src: Bitmap): Bitmap {
        val m = Matrix().apply { postRotate(90f) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, false)
    }

    private fun upscaleIfSmall(src: Bitmap, minSideTarget: Int): Bitmap {
        val minSide = minOf(src.width, src.height)
        if (minSide >= minSideTarget || minSide <= 0) return src
        val factor = (minSideTarget.toFloat() / minSide).coerceIn(1f, 3f)
        val nw = (src.width * factor).roundToInt().coerceAtLeast(src.width)
        val nh = (src.height * factor).roundToInt().coerceAtLeast(src.height)
        return Bitmap.createScaledBitmap(src, nw, nh, false)
    }

    private fun upscaleWidthIfSmall(src: Bitmap, minWidthTarget: Int): Bitmap {
        if (src.width >= minWidthTarget || src.width <= 0) return src
        val factor = (minWidthTarget.toFloat() / src.width).coerceIn(1f, 3f)
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
