package com.kgps.result.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import com.kgps.result.BlankSheetQrMeta
import com.kgps.result.SheetQrCodec
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
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
        if (!fromOfflineQr) {
            scanAutoZoomZone(
                bitmap = bitmap,
                zone = CropRegion(0.42f, 0.00f, 1.00f, 0.56f),
                preferOfflineQr = true,
                maxCandidates = 4,
                onTextFound = { text ->
                    handleDecodedQrText(text, QrSlot.TOP_RIGHT)
                    fromOfflineQr
                }
            )
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
        if (!fromOfflineQr && !fromThinQr) {
            scanAutoZoomZone(
                bitmap = bitmap,
                zone = CropRegion(0.00f, 0.00f, 0.58f, 0.56f),
                preferOfflineQr = false,
                maxCandidates = 4,
                onTextFound = { text ->
                    handleDecodedQrText(text, QrSlot.TOP_LEFT)
                    fromOfflineQr || fromThinQr
                }
            )
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
        if (!fromOfflineQr && !fromBottomQr) {
            scanAutoZoomZone(
                bitmap = bitmap,
                zone = CropRegion(0.00f, 0.45f, 1.00f, 1.00f),
                preferOfflineQr = false,
                maxCandidates = 4,
                onTextFound = { text ->
                    handleDecodedQrText(text, QrSlot.BOTTOM)
                    fromOfflineQr || fromBottomQr
                }
            )
        }
        if (fromOfflineQr || fromBottomQr) return buildResult()

        // ৪) শেষ চেষ্টা: অপ্রচলিত মার্জিন/ক্রপের জন্য প্রশস্ত অঞ্চল, পূর্ণ ছবি ও পূর্ণ-ফ্রেম অটো-জুম অনুসন্ধান
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
        if (!fromOfflineQr && !fromThinQr && !fromBottomQr) {
            scanAutoZoomZone(
                bitmap = bitmap,
                zone = CropRegion(0.00f, 0.00f, 1.00f, 1.00f),
                preferOfflineQr = true,
                maxCandidates = 6,
                minScaleFrac = 0.055f,
                onTextFound = { text ->
                    handleDecodedQrText(text, QrSlot.GENERIC)
                    fromOfflineQr
                }
            )
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

    internal data class AutoZoomBox(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val score: Float
    )

    private inline fun scanAutoZoomZone(
        bitmap: Bitmap,
        zone: CropRegion,
        preferOfflineQr: Boolean,
        maxCandidates: Int = 4,
        minScaleFrac: Float = 0.075f,
        onTextFound: (String) -> Boolean
    ) {
        val zoneCrop = if (zone.left <= 0.001f && zone.top <= 0.001f && zone.right >= 0.999f && zone.bottom >= 0.999f) {
            bitmap
        } else {
            extractNormalizedCrop(bitmap, zone) ?: return
        }
        try {
            val analysisBmp = downscaleIfTooLarge(zoneCrop, maxLongestSide = 440)
            val candidates = try {
                val aw = analysisBmp.width
                val ah = analysisBmp.height
                if (aw < 28 || ah < 28) {
                    emptyList()
                } else {
                    val pixels = IntArray(aw * ah)
                    analysisBmp.getPixels(pixels, 0, aw, 0, 0, aw, ah)
                    val luma = BarcodeLuminanceDecoder.argbToLuminance(pixels)
                    findAutoZoomCandidateBoxesLuma(
                        luma = luma,
                        width = aw,
                        height = ah,
                        minScaleFrac = minScaleFrac,
                        maxScaleFrac = 0.34f,
                        maxCandidates = maxCandidates
                    )
                }
            } finally {
                if (analysisBmp !== zoneCrop) analysisBmp.recycle()
            }

            val zoneW = (zone.right - zone.left).coerceAtLeast(0.01f)
            val zoneH = (zone.bottom - zone.top).coerceAtLeast(0.01f)
            for ((idx, box) in candidates.withIndex()) {
                val padX = (box.right - box.left) * 0.34f
                val padY = (box.bottom - box.top) * 0.34f
                val fullRegion = CropRegion(
                    left = (zone.left + (box.left - padX) * zoneW).coerceIn(0f, 0.95f),
                    top = (zone.top + (box.top - padY) * zoneH).coerceIn(0f, 0.95f),
                    right = (zone.left + (box.right + padX) * zoneW).coerceIn(0.05f, 1f),
                    bottom = (zone.top + (box.bottom + padY) * zoneH).coerceIn(0.05f, 1f)
                )
                val candCrop = extractNormalizedCrop(bitmap, fullRegion) ?: continue
                try {
                    val zoomed = upscaleIfSmall(candCrop, minSideTarget = 720, maxFactor = 6.5f, maxOutSide = 1280)
                    try {
                        var hit = false
                        val texts = decodeAllQrsWithLuminance(zoomed, stopOnOfflineQr = preferOfflineQr)
                        for (t in texts) {
                            hit = onTextFound(t)
                            if (hit) return
                        }
                        if (!hit && idx < 2) {
                            for (angle in floatArrayOf(-5.5f, 5.5f)) {
                                val tilted = rotateBitmapWhiteBg(zoomed, angle)
                                try {
                                    val tiltTexts = decodeAllQrsWithLuminance(tilted, stopOnOfflineQr = preferOfflineQr)
                                    for (t in tiltTexts) {
                                        if (onTextFound(t)) return
                                    }
                                } finally {
                                    tilted.recycle()
                                }
                            }
                        }
                    } finally {
                        if (zoomed !== candCrop) zoomed.recycle()
                    }
                } finally {
                    if (candCrop !== bitmap) candCrop.recycle()
                }
            }
        } finally {
            if (zoneCrop !== bitmap) zoneCrop.recycle()
        }
    }

    internal fun findAutoZoomCandidateBoxesLuma(
        luma: ByteArray,
        width: Int,
        height: Int,
        minScaleFrac: Float = 0.075f,
        maxScaleFrac: Float = 0.34f,
        maxCandidates: Int = 4
    ): List<AutoZoomBox> {
        if (width < 28 || height < 28 || luma.size < width * height) return emptyList()

        // Adaptive dark threshold from [p15..p85] luminance percentiles
        val hist = IntArray(256)
        var sampled = 0
        var idx = 0
        val stepSample = if (width * height > 60_000) 2 else 1
        while (idx < width * height) {
            hist[luma[idx].toInt() and 0xFF]++
            sampled++
            idx += stepSample
        }
        var p15 = 40
        var p85 = 215
        if (sampled > 0) {
            val t15 = (sampled * 0.15f).toInt()
            val t85 = (sampled * 0.85f).toInt().coerceAtLeast(t15 + 1)
            var acc = 0
            var got15 = false
            for (v in 0..255) {
                acc += hist[v]
                if (!got15 && acc >= t15) {
                    p15 = v
                    got15 = true
                }
                if (acc >= t85) {
                    p85 = v
                    break
                }
            }
        }
        val darkThresh = (p15 + ((p85 - p15) * 0.48f).toInt()).coerceIn(60, 195)

        val iw = width + 1
        val intGx = LongArray(iw * (height + 1))
        val intGy = LongArray(iw * (height + 1))
        val intDark = IntArray(iw * (height + 1))

        for (y in 0 until height) {
            var rowGx = 0L
            var rowGy = 0L
            var rowDark = 0
            val curOff = (y + 1) * iw
            val prevOff = y * iw
            val rowPix = y * width
            val topPix = max(0, y - 1) * width
            val botPix = min(height - 1, y + 1) * width
            for (x in 0 until width) {
                val lum = luma[rowPix + x].toInt() and 0xFF
                if (lum < darkThresh) rowDark++
                val xl = max(0, x - 1)
                val xr = min(width - 1, x + 1)
                val gx = abs((luma[rowPix + xr].toInt() and 0xFF) - (luma[rowPix + xl].toInt() and 0xFF))
                val gy = abs((luma[botPix + x].toInt() and 0xFF) - (luma[topPix + x].toInt() and 0xFF))
                if (gx >= 20) rowGx += gx
                if (gy >= 20) rowGy += gy
                intGx[curOff + x + 1] = intGx[prevOff + x + 1] + rowGx
                intGy[curOff + x + 1] = intGy[prevOff + x + 1] + rowGy
                intDark[curOff + x + 1] = intDark[prevOff + x + 1] + rowDark
            }
        }

        fun sumLong(arr: LongArray, x0: Int, y0: Int, x1: Int, y1: Int): Long =
            arr[y1 * iw + x1] - arr[y0 * iw + x1] - arr[y1 * iw + x0] + arr[y0 * iw + x0]

        fun sumInt(arr: IntArray, x0: Int, y0: Int, x1: Int, y1: Int): Int =
            arr[y1 * iw + x1] - arr[y0 * iw + x1] - arr[y1 * iw + x0] + arr[y0 * iw + x0]

        val minDim = min(width, height)
        val rawFracs = floatArrayOf(0.06f, 0.09f, 0.13f, 0.18f, 0.24f, 0.31f)
        val windowSizes = rawFracs
            .filter { it in (minScaleFrac - 0.015f)..(maxScaleFrac + 0.02f) }
            .map { (minDim * it).roundToInt().coerceIn(18, minDim) }
            .distinct()

        val scored = ArrayList<AutoZoomBox>(64)
        for (s in windowSizes) {
            val step = max(5, (s * 0.38f).roundToInt())
            val half = (s / 2).coerceAtLeast(4)
            val area = (s * s).toFloat()
            var y = 0
            while (y <= height - s) {
                var x = 0
                while (x <= width - s) {
                    val sumGx = sumLong(intGx, x, y, x + s, y + s)
                    val sumGy = sumLong(intGy, x, y, x + s, y + s)
                    val meanGx = sumGx.toFloat() / area
                    val meanGy = sumGy.toFloat() / area
                    if (meanGx >= 7f && meanGy >= 7f) {
                        val darkRatio = sumInt(intDark, x, y, x + s, y + s).toFloat() / area
                        if (darkRatio in 0.14f..0.76f) {
                            val tl = min(
                                sumLong(intGx, x, y, x + half, y + half),
                                sumLong(intGy, x, y, x + half, y + half)
                            )
                            val tr = min(
                                sumLong(intGx, x + half, y, x + s, y + half),
                                sumLong(intGy, x + half, y, x + s, y + half)
                            )
                            val bl = min(
                                sumLong(intGx, x, y + half, x + half, y + s),
                                sumLong(intGy, x, y + half, x + half, y + s)
                            )
                            val br = min(
                                sumLong(intGx, x + half, y + half, x + s, y + s),
                                sumLong(intGy, x + half, y + half, x + s, y + s)
                            )
                            val qList = longArrayOf(tl, tr, bl, br).apply { sort() }
                            val qThird = qList[1] // 2nd smallest == weakest of the top 3 finder quadrants
                            val qMax = qList[3]
                            if (qMax > 0L) {
                                val quadUniformity = qThird.toFloat() / qMax.toFloat()
                                if (quadUniformity >= 0.22f) {
                                    val xyMin = min(meanGx, meanGy)
                                    val xyMax = max(meanGx, meanGy)
                                    val xyBalance = xyMin / xyMax
                                    val darkBonus = 1f - abs(darkRatio - 0.44f).coerceAtMost(0.32f)
                                    val score = xyMin * xyBalance * (0.45f + 0.55f * quadUniformity) * darkBonus
                                    scored.add(
                                        AutoZoomBox(
                                            left = x.toFloat() / width,
                                            top = y.toFloat() / height,
                                            right = (x + s).toFloat() / width,
                                            bottom = (y + s).toFloat() / height,
                                            score = score
                                        )
                                    )
                                }
                            }
                        }
                    }
                    x += step
                }
                y += step
            }
        }

        if (scored.isEmpty()) return emptyList()
        scored.sortByDescending { it.score }

        val picked = ArrayList<AutoZoomBox>(maxCandidates)
        for (cand in scored) {
            val overlaps = picked.any { existing ->
                val ix0 = max(existing.left, cand.left)
                val iy0 = max(existing.top, cand.top)
                val ix1 = min(existing.right, cand.right)
                val iy1 = min(existing.bottom, cand.bottom)
                val inter = max(0f, ix1 - ix0) * max(0f, iy1 - iy0)
                if (inter <= 0f) return@any false
                val a1 = (existing.right - existing.left) * (existing.bottom - existing.top)
                val a2 = (cand.right - cand.left) * (cand.bottom - cand.top)
                val union = (a1 + a2 - inter).coerceAtLeast(1e-6f)
                inter / union > 0.28f || inter / min(a1, a2).coerceAtLeast(1e-6f) > 0.50f
            }
            if (!overlaps) {
                picked.add(cand)
                if (picked.size >= maxCandidates) break
            }
        }
        return picked
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

