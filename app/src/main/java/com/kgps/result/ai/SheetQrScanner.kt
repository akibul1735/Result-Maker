package com.kgps.result.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import com.kgps.result.BlankSheetQrMeta
import com.kgps.result.SheetQrCodec
import kotlin.math.roundToInt

data class SheetScanDetection(
    val templateId: String? = null,
    val thinQrRawText: String? = null,
    val offlineQrRawText: String? = null,
    val offlineQrMeta: BlankSheetQrMeta? = null,
    val bottomQrRawText: String? = null,
    val barcodeRawText: String? = null,
    val barcodeMeta: BlankSheetQrMeta? = null,
    val detectedFromThinQr: Boolean = false,
    val detectedFromOfflineQr: Boolean = false,
    val detectedFromBottomQr: Boolean = false,
    val detectedFromBarcode: Boolean = false
) {
    val bestLocalMeta: BlankSheetQrMeta?
        get() = offlineQrMeta ?: barcodeMeta

    val detectedCount: Int
        get() = (if (detectedFromOfflineQr) 1 else 0) +
            (if (detectedFromThinQr) 1 else 0) +
            (if (detectedFromBottomQr) 1 else 0) +
            (if (detectedFromBarcode) 1 else 0)

    val sourceLabelBn: String
        get() = when {
            detectedFromOfflineQr -> "ডান পাশের অফলাইন QR"
            detectedFromThinQr -> "বাম পাশের QR"
            detectedFromBottomQr -> "নিচের QR"
            detectedFromBarcode -> "নিচের বারকোড"
            else -> ""
        }
}

/**
 * নির্বাচিত/তোলা শীটের ছবি থেকে সম্পূর্ণ অফলাইনে ZXing দিয়ে অগ্রাধিকার ক্রমে ৩টি QR চ্যানেল স্ক্যান করে:
 * ১) প্রথমে শীর্ষ-ডানের পূর্ণাঙ্গ অফলাইন QR (`K2/...` বা পুরনো JSON)
 * ২) ডানের QR না পেলে শীর্ষ-বামের স্মার্ট ID QR (`KT1:...`)
 * ৩) বামের QR-ও না পেলে নিচের ব্যাকআপ QR (`KT1:...`)
 * (বর্তমানে Code-128 বারকোড ব্যবহার ও স্ক্যান করা হয় না)
 */
object SheetQrScanner {

    private data class CropRegion(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    )

    private enum class QrSlot {
        TOP_RIGHT,
        TOP_LEFT,
        BOTTOM,
        GENERIC
    }

    // ১) শীর্ষ-ডানের পূর্ণাঙ্গ অফলাইন QR অঞ্চল (টাইট -> মাঝারি -> প্রশস্ত -> ইনসেট)
    private val qrTopRightRegions = listOf(
        CropRegion(0.72f, 0.00f, 1.00f, 0.24f),
        CropRegion(0.62f, 0.00f, 1.00f, 0.28f),
        CropRegion(0.56f, 0.00f, 1.00f, 0.34f),
        CropRegion(0.76f, 0.00f, 0.99f, 0.16f),
        CropRegion(0.58f, 0.05f, 0.96f, 0.38f)
    )

    // ২) শীর্ষ-বামের স্মার্ট ID QR অঞ্চল
    private val qrTopLeftRegions = listOf(
        CropRegion(0.00f, 0.00f, 0.28f, 0.24f),
        CropRegion(0.00f, 0.00f, 0.44f, 0.34f),
        CropRegion(0.04f, 0.05f, 0.42f, 0.38f)
    )

    // ৩) নিচের ব্যাকআপ QR অঞ্চল (৩১pt নিচে-বাম কোণা + বাড়তি মার্জিন + ঘোরানো কোণা)
    private val qrBottomRegions = listOf(
        CropRegion(0.00f, 0.78f, 0.28f, 1.00f),
        CropRegion(0.00f, 0.68f, 0.40f, 1.00f),
        CropRegion(0.00f, 0.68f, 1.00f, 1.00f),
        CropRegion(0.66f, 0.74f, 1.00f, 1.00f),
        CropRegion(0.54f, 0.62f, 1.00f, 1.00f)
    )

    private val qrWideFallbackRegions = listOf(
        CropRegion(0.46f, 0.00f, 1.00f, 0.48f),
        CropRegion(0.00f, 0.00f, 0.54f, 0.48f)
    )

    private val qrRightTiltRegions = listOf(
        CropRegion(0.68f, 0.00f, 1.00f, 0.28f),
        CropRegion(0.56f, 0.00f, 1.00f, 0.36f)
    )
    private val qrLeftTiltRegions = listOf(
        CropRegion(0.00f, 0.00f, 0.34f, 0.28f)
    )
    private val qrBottomTiltRegions = listOf(
        CropRegion(0.00f, 0.72f, 0.36f, 1.00f)
    )
    private val qrTiltAnglesDeg = floatArrayOf(-4f, 4f, -8.5f, 8.5f, -13f, 13f)

    private val bitmapFilterPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    fun decodeFromBitmap(bitmap: Bitmap): BlankSheetQrMeta? =
        decodeAllFromBitmap(bitmap).bestLocalMeta

    fun decodeAllFromBitmap(bitmap: Bitmap): SheetScanDetection {
        if (bitmap.width < 32 || bitmap.height < 12) return SheetScanDetection()

        var templateId: String? = null
        var thinQrRaw: String? = null
        var offlineQrRaw: String? = null
        var offlineMeta: BlankSheetQrMeta? = null
        var bottomQrRaw: String? = null
        var fromThinQr = false
        var fromOfflineQr = false
        var fromBottomQr = false

        fun buildResult() = SheetScanDetection(
            templateId = templateId,
            thinQrRawText = thinQrRaw,
            offlineQrRawText = offlineQrRaw,
            offlineQrMeta = offlineMeta,
            bottomQrRawText = bottomQrRaw,
            detectedFromThinQr = fromThinQr,
            detectedFromOfflineQr = fromOfflineQr,
            detectedFromBottomQr = fromBottomQr,
            detectedFromBarcode = false
        )

        fun handleDecodedQrText(raw: String, slot: QrSlot) {
            val ch = SheetQrCodec.decodeChannel(raw)
            if (ch.meta != null && offlineMeta == null) {
                offlineMeta = ch.meta
                fromOfflineQr = true
                offlineQrRaw = raw.trim()
                templateId = ch.templateId ?: ch.meta.templateId.takeIf { it.isNotBlank() } ?: templateId
                return
            }
            val tid = ch.templateId ?: return
            if (templateId == null) {
                templateId = tid
            }
            when (slot) {
                QrSlot.BOTTOM -> {
                    if (!fromBottomQr) {
                        fromBottomQr = true
                        bottomQrRaw = raw.trim()
                    }
                }
                QrSlot.TOP_LEFT -> {
                    if (!fromThinQr) {
                        fromThinQr = true
                        thinQrRaw = raw.trim()
                    }
                }
                QrSlot.TOP_RIGHT, QrSlot.GENERIC -> {
                    if (!fromThinQr) {
                        fromThinQr = true
                        thinQrRaw = raw.trim()
                    }
                }
            }
        }

        // ১) প্রথমেই ডান পাশের পূর্ণাঙ্গ অফলাইন QR স্ক্যান (পেলে সঙ্গে সঙ্গে সম্পন্ন)
        for (region in qrTopRightRegions) {
            if (fromOfflineQr) break
            scanQrRegionMulti(bitmap, region, preferOfflineQr = true).forEach {
                handleDecodedQrText(it, QrSlot.TOP_RIGHT)
            }
        }
        if (!fromOfflineQr) {
            tryTiltRegions(bitmap, qrRightTiltRegions, stopOnOfflineQr = true) { text ->
                handleDecodedQrText(text, QrSlot.TOP_RIGHT)
                fromOfflineQr
            }
        }
        if (fromOfflineQr) return buildResult()

        // ২) ডান পাশের QR না পেলে বাম পাশের স্মার্ট ID QR স্ক্যান
        for (region in qrTopLeftRegions) {
            if (fromOfflineQr || fromThinQr) break
            scanQrRegionMulti(bitmap, region, preferOfflineQr = false).forEach {
                handleDecodedQrText(it, QrSlot.TOP_LEFT)
            }
        }
        if (!fromOfflineQr && !fromThinQr) {
            tryTiltRegions(bitmap, qrLeftTiltRegions, stopOnOfflineQr = false) { text ->
                handleDecodedQrText(text, QrSlot.TOP_LEFT)
                fromOfflineQr || fromThinQr
            }
        }
        if (fromOfflineQr || fromThinQr) return buildResult()

        // ৩) ডান ও বাম কোনোটিই না পেলে নিচের QR কোড স্ক্যান
        for (region in qrBottomRegions) {
            if (fromOfflineQr || fromBottomQr) break
            scanQrRegionMulti(bitmap, region, preferOfflineQr = false).forEach {
                handleDecodedQrText(it, QrSlot.BOTTOM)
            }
        }
        if (!fromOfflineQr && !fromBottomQr) {
            tryTiltRegions(bitmap, qrBottomTiltRegions, stopOnOfflineQr = false) { text ->
                handleDecodedQrText(text, QrSlot.BOTTOM)
                fromOfflineQr || fromBottomQr
            }
        }
        if (fromOfflineQr || fromBottomQr) return buildResult()

        // ৪) শেষ চেষ্টা: অপ্রচলিত মার্জিন/ক্রপের জন্য প্রশস্ত অঞ্চল ও পূর্ণ ছবিতে বহু-QR অনুসন্ধান
        for (region in qrWideFallbackRegions) {
            if (fromOfflineQr || fromThinQr) break
            scanQrRegionMulti(bitmap, region, preferOfflineQr = true).forEach {
                handleDecodedQrText(it, QrSlot.GENERIC)
            }
        }
        if (!fromOfflineQr && !fromThinQr && !fromBottomQr) {
            val fullScaled = downscaleIfTooLarge(bitmap, maxLongestSide = 1800)
            try {
                val working = upscaleIfSmall(fullScaled, minSideTarget = 560, maxFactor = 3.5f, maxOutSide = 1800)
                try {
                    decodeAllQrsWithLuminance(working, stopOnOfflineQr = true).forEach {
                        handleDecodedQrText(it, QrSlot.GENERIC)
                    }
                } finally {
                    if (working !== fullScaled) working.recycle()
                }
            } finally {
                if (fullScaled !== bitmap) fullScaled.recycle()
            }
        }

        return buildResult()
    }

    private inline fun tryTiltRegions(
        bitmap: Bitmap,
        regions: List<CropRegion>,
        stopOnOfflineQr: Boolean,
        onTextFound: (String) -> Boolean
    ) {
        for (region in regions) {
            val crop = extractNormalizedCrop(bitmap, region) ?: continue
            try {
                val prepared = upscaleIfSmall(crop, minSideTarget = 560, maxFactor = 3.5f, maxOutSide = 1400)
                try {
                    for (angle in qrTiltAnglesDeg) {
                        val tilted = rotateBitmapWhiteBg(prepared, angle)
                        try {
                            val texts = decodeAllQrsWithLuminance(tilted, stopOnOfflineQr = stopOnOfflineQr)
                            for (t in texts) {
                                if (onTextFound(t)) return
                            }
                        } finally {
                            tilted.recycle()
                        }
                    }
                } finally {
                    if (prepared !== crop) prepared.recycle()
                }
            } finally {
                if (crop !== bitmap) crop.recycle()
            }
        }
    }

    private fun scanQrRegionMulti(
        bitmap: Bitmap,
        region: CropRegion,
        preferOfflineQr: Boolean
    ): List<String> {
        val crop = extractNormalizedCrop(bitmap, region) ?: return emptyList()
        return try {
            val isTightCorner = (region.right - region.left) <= 0.36f && (region.bottom - region.top) <= 0.30f
            val minTarget = if (isTightCorner) 680 else 760
            val prepared = upscaleIfSmall(crop, minSideTarget = minTarget, maxFactor = 4.0f, maxOutSide = 1400)
            try {
                decodeAllQrsWithLuminance(prepared, stopOnOfflineQr = preferOfflineQr)
            } finally {
                if (prepared !== crop) prepared.recycle()
            }
        } finally {
            if (crop !== bitmap) crop.recycle()
        }
    }

    private fun decodeAllQrsWithLuminance(
        bmp: Bitmap,
        stopOnOfflineQr: Boolean
    ): List<String> {
        val w = bmp.width
        val h = bmp.height
        if (w < 20 || h < 20) return emptyList()
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        val luma = BarcodeLuminanceDecoder.argbToLuminance(pixels)
        return QrLuminanceDecoder.decodeAllQrs(
            width = w,
            height = h,
            luma = luma,
            addQuietZone = true,
            stopWhen = { found ->
                if (stopOnOfflineQr) {
                    found.any { SheetQrCodec.decodePayload(it) != null }
                } else {
                    found.any { SheetQrCodec.decodeChannel(it).let { ch -> ch.templateId != null || ch.meta != null } }
                }
            }
        )
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

    private fun upscaleIfSmall(
        src: Bitmap,
        minSideTarget: Int,
        maxFactor: Float = 3f,
        maxOutSide: Int = 2000
    ): Bitmap {
        val minSide = minOf(src.width, src.height)
        val maxSide = maxOf(src.width, src.height)
        if (minSide >= minSideTarget || minSide <= 0 || maxSide >= maxOutSide) return src
        val needed = (minSideTarget.toFloat() / minSide).coerceIn(1f, maxFactor)
        val maxAllowed = maxOutSide.toFloat() / maxSide
        val factor = minOf(needed, maxAllowed)
        if (factor <= 1.05f) return src
        val nw = (src.width * factor).roundToInt().coerceAtLeast(src.width)
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

