package com.kgps.result

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object TestimonialPdfGen {
    const val A4_LANDSCAPE_W = 842
    const val A4_LANDSCAPE_H = 595

    private const val INK_PRIMARY = 0xFF0F172A.toInt()
    private const val INK_SECONDARY = 0xFF334155.toInt()
    private const val FRAME_DARK = 0xFF1E293B.toInt()
    private const val FRAME_MUTED = 0xFF64748B.toInt()

    fun renderPageBitmap(
        ctx: Context,
        page: TestimonialPageSpec,
        settings: Settings,
        students: List<Student>,
        signatureBitmap: Bitmap?,
        scale: Float = 2.0f
    ): Bitmap {
        val bmp = Bitmap.createBitmap(
            (A4_LANDSCAPE_W * scale).toInt(),
            (A4_LANDSCAPE_H * scale).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        c.scale(scale, scale)
        drawPage(ctx, c, page, settings, students, signatureBitmap)
        return bmp
    }

    fun write(
        ctx: Context,
        pages: List<TestimonialPageSpec>,
        settings: Settings,
        students: List<Student>,
        signatureBitmap: Bitmap?,
        out: OutputStream
    ) {
        if (pages.isEmpty()) return
        val doc = PdfDocument()
        try {
            pages.forEachIndexed { idx, page ->
                val pdfPage = doc.startPage(
                    PdfDocument.PageInfo.Builder(A4_LANDSCAPE_W, A4_LANDSCAPE_H, idx + 1).create()
                )
                pdfPage.canvas.drawColor(Color.WHITE)
                drawPage(ctx, pdfPage.canvas, page, settings, students, signatureBitmap)
                doc.finishPage(pdfPage)
            }
            doc.writeTo(out)
        } finally {
            doc.close()
        }
    }

    fun print(
        ctx: Context,
        pages: List<TestimonialPageSpec>,
        settings: Settings,
        students: List<Student>,
        signatureBitmap: Bitmap?,
        jobName: String
    ) {
        if (pages.isEmpty()) return
        val printManager = ctx.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(pages.size)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pageRanges: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                try {
                    FileOutputStream(destination.fileDescriptor).use { out ->
                        write(ctx, pages, settings, students, signatureBitmap, out)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }
        printManager.print(
            jobName,
            adapter,
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape())
                .build()
        )
    }

    internal fun drawPage(
        ctx: Context?,
        c: Canvas,
        page: TestimonialPageSpec,
        settings: Settings,
        students: List<Student>,
        signatureBitmap: Bitmap?
    ) {
        val cfg = page.config
        val margin = cfg.safeMarginPt
        val pageRect = RectF(
            margin,
            margin,
            (A4_LANDSCAPE_W - margin).coerceAtLeast(margin + 360f),
            (A4_LANDSCAPE_H - margin).coerceAtLeast(margin + 240f)
        )

        val bnFont = cfg.bnFontId.ifBlank { settings.pdfFont }
        val enFont = cfg.enFontId.ifBlank { settings.pdfEnglishFont }

        if (cfg.showCounterfoil) {
            val counterfoilW = min(178f, pageRect.width() * 0.23f)
            val gapW = 14f
            val counterfoilRect = RectF(
                pageRect.left,
                pageRect.top,
                pageRect.left + counterfoilW,
                pageRect.bottom
            )
            val cutX = counterfoilRect.right + gapW / 2f
            val cutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF475569.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 0.85f
                pathEffect = DashPathEffect(floatArrayOf(3.5f, 3.0f), 0f)
            }
            c.drawLine(cutX, pageRect.top - 4f, cutX, pageRect.bottom + 4f, cutPaint)

            drawCounterfoil(
                ctx = ctx,
                c = c,
                rect = counterfoilRect,
                page = page,
                settings = settings,
                students = students,
                bnFont = bnFont,
                enFont = enFont
            )

            val mainRect = RectF(
                counterfoilRect.right + gapW,
                pageRect.top,
                pageRect.right,
                pageRect.bottom
            )
            drawMainCertificate(
                ctx = ctx,
                c = c,
                rect = mainRect,
                page = page,
                settings = settings,
                students = students,
                signatureBitmap = signatureBitmap,
                bnFont = bnFont,
                enFont = enFont
            )
        } else {
            drawMainCertificate(
                ctx = ctx,
                c = c,
                rect = pageRect,
                page = page,
                settings = settings,
                students = students,
                signatureBitmap = signatureBitmap,
                bnFont = bnFont,
                enFont = enFont
            )
        }
    }

    private fun drawCounterfoil(
        ctx: Context?,
        c: Canvas,
        rect: RectF,
        page: TestimonialPageSpec,
        settings: Settings,
        students: List<Student>,
        bnFont: String,
        enFont: String
    ) {
        val cfg = page.config
        val item = page.item
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FRAME_DARK
            style = Paint.Style.STROKE
            strokeWidth = 0.9f
        }
        c.drawRect(rect, borderPaint)

        val padX = 8f
        val innerLeft = rect.left + padX
        val innerRight = rect.right - padX
        val innerW = innerRight - innerLeft
        val cx = rect.centerX()
        var y = rect.top + 16f

        val schoolName = TestimonialPlanner.resolvedSchoolName(cfg, settings)
        val schoolPaint = PdfGen.dualPaint(ctx, 9.6f, true, bnFont, enFont, INK_PRIMARY)
        val schoolLines = wrapText(schoolName, schoolPaint, innerW).take(2)
        schoolLines.forEach { line ->
            val linePaint = PdfGen.dualPaint(ctx, 9.6f, true, bnFont, enFont, INK_PRIMARY)
            PdfGen.shrinkMixedToFit(linePaint, line, innerW, minSize = 6.5f)
            PdfGen.drawMixedText(c, line, cx, y, linePaint, Paint.Align.CENTER)
            y += 12f
        }

        val address = TestimonialPlanner.resolvedSchoolAddress(cfg, settings, students)
        if (address.isNotBlank()) {
            val addrPaint = PdfGen.dualPaint(ctx, 7.6f, false, bnFont, enFont, INK_SECONDARY)
            val addrLines = wrapText(address, addrPaint, innerW).take(2)
            addrLines.forEach { line ->
                PdfGen.shrinkMixedToFit(addrPaint, line, innerW, minSize = 6f)
                PdfGen.drawMixedText(c, line, cx, y, addrPaint, Paint.Align.CENTER)
                y += 10f
            }
        }

        val estd = cfg.establishedYear.trim()
        if (estd.isNotBlank()) {
            val estdText = "স্থাপিত: ${TestimonialPlanner.formatDigits(estd, cfg.numeralMode)}"
            val estdPaint = PdfGen.dualPaint(ctx, 7.6f, false, bnFont, enFont, INK_SECONDARY)
            PdfGen.drawMixedText(c, estdText, cx, y, estdPaint, Paint.Align.CENTER)
            y += 11f
        }

        // Compact scroll ribbon in counterfoil
        val bannerW = (innerW - 6f).coerceAtLeast(90f)
        val bannerH = 22f
        drawScrollBanner(
            ctx = ctx,
            c = c,
            cx = cx,
            topY = y,
            w = bannerW,
            h = bannerH,
            title = TestimonialPlanner.resolvedBannerTitle(cfg),
            fontSize = 10.5f,
            bnFont = bnFont,
            enFont = enFont
        )
        y += bannerH + 22f

        val metaPaint = PdfGen.dualPaint(ctx, 8.2f, false, bnFont, enFont, INK_PRIMARY)
        val serialVal = if (item.serialText.isBlank()) "...................." else item.serialText
        PdfGen.drawMixedText(c, "সিরিয়াল নম্বর: $serialVal", innerLeft, y, metaPaint, Paint.Align.LEFT)
        y += 14f
        PdfGen.drawMixedText(
            c,
            "তারিখ: ${TestimonialPlanner.resolvedIssueDate(cfg)}",
            innerLeft,
            y,
            metaPaint,
            Paint.Align.LEFT
        )
        y += 24f

        val infoPairs = listOf(
            "নাম:" to item.studentName,
            "পিতার নাম:" to item.fatherName,
            "মাতার নাম:" to item.motherName,
            "জন্মতারিখ:" to item.dateOfBirth
        )
        infoPairs.forEach { (label, value) ->
            y = drawCounterfoilLabeledLine(
                ctx = ctx,
                c = c,
                x = innerLeft,
                y = y,
                maxW = innerW,
                label = label,
                value = value,
                bnFont = bnFont,
                enFont = enFont
            )
            y += 3f
        }

        y += 12f
        val classLabel = classShortBn(item.cls, settings).ifBlank { classNameBn(item.cls, settings) }
        val rollLabel = if (item.isBlank) "" else TestimonialPlanner.formatRoll(item.roll, cfg)
        val academicPairs = listOf(
            "শ্রেণি:" to classLabel,
            "রোল নম্বর:" to rollLabel,
            "শিক্ষাবর্ষ:" to if (item.isBlank) "" else item.sessionYear
        )
        academicPairs.forEach { (label, value) ->
            y = drawCounterfoilLabeledLine(
                ctx = ctx,
                c = c,
                x = innerLeft,
                y = y,
                maxW = innerW,
                label = label,
                value = value,
                bnFont = bnFont,
                enFont = enFont
            )
            y += 3f
        }

        // Bottom two rounded signature boxes
        val boxH = 54f
        val boxGap = 14f
        val bottomPad = 14f
        val box2Bottom = rect.bottom - bottomPad
        val box2Top = (box2Bottom - boxH).coerceAtLeast(y + 8f)
        val box1Bottom = box2Top - boxGap
        val box1Top = box1Bottom - boxH

        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FRAME_MUTED
            style = Paint.Style.STROKE
            strokeWidth = 0.7f
        }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK_SECONDARY
            style = Paint.Style.STROKE
            strokeWidth = 0.7f
            pathEffect = DashPathEffect(floatArrayOf(1.8f, 2.0f), 0f)
        }
        val boxTextPaint = PdfGen.dualPaint(ctx, 7.6f, false, bnFont, enFont, INK_PRIMARY)

        if (box1Top >= y + 4f) {
            val r1 = RectF(innerLeft + 2f, box1Top, innerRight - 2f, box1Bottom)
            c.drawRoundRect(r1, 8f, 8f, boxPaint)
            val l1 = "ছাত্র/ছাত্রীর স্বাক্ষর:"
            PdfGen.drawMixedText(c, l1, r1.left + 8f, r1.top + 25f, boxTextPaint, Paint.Align.LEFT)
            val l1End = r1.left + 8f + PdfGen.measureMixedText(l1, boxTextPaint) + 4f
            if (l1End < r1.right - 8f) {
                c.drawLine(l1End, r1.top + 26f, r1.right - 8f, r1.top + 26f, dotPaint)
            }
            PdfGen.drawMixedText(c, "তারিখ:", r1.left + 8f, r1.top + 41f, boxTextPaint, Paint.Align.LEFT)

            val r2 = RectF(innerLeft + 2f, box2Top, innerRight - 2f, box2Bottom)
            c.drawRoundRect(r2, 8f, 8f, boxPaint)
            val l2 = "প্রদানকারী শিক্ষকের স্বাক্ষর:"
            PdfGen.shrinkMixedToFit(boxTextPaint, l2, r2.width() - 42f, minSize = 6.2f)
            PdfGen.drawMixedText(c, l2, r2.left + 8f, r2.top + 25f, boxTextPaint, Paint.Align.LEFT)
            val l2End = r2.left + 8f + PdfGen.measureMixedText(l2, boxTextPaint) + 4f
            if (l2End < r2.right - 8f) {
                c.drawLine(l2End, r2.top + 26f, r2.right - 8f, r2.top + 26f, dotPaint)
            }
            val dateResetPaint = PdfGen.dualPaint(ctx, 7.6f, false, bnFont, enFont, INK_PRIMARY)
            PdfGen.drawMixedText(c, "তারিখ:", r2.left + 8f, r2.top + 41f, dateResetPaint, Paint.Align.LEFT)
        }
    }

    private fun drawCounterfoilLabeledLine(
        ctx: Context?,
        c: Canvas,
        x: Float,
        y: Float,
        maxW: Float,
        label: String,
        value: String,
        bnFont: String,
        enFont: String
    ): Float {
        val text = if (value.isBlank()) "$label ................" else "$label $value"
        val p = PdfGen.dualPaint(ctx, 8.1f, false, bnFont, enFont, INK_PRIMARY)
        val lines = wrapText(text, p, maxW).take(2)
        var curY = y
        lines.forEach { line ->
            PdfGen.shrinkMixedToFit(p, line, maxW, minSize = 6.3f)
            PdfGen.drawMixedText(c, line, x, curY, p, Paint.Align.LEFT)
            curY += 11.5f
        }
        return curY
    }

    private fun drawMainCertificate(
        ctx: Context?,
        c: Canvas,
        rect: RectF,
        page: TestimonialPageSpec,
        settings: Settings,
        students: List<Student>,
        signatureBitmap: Bitmap?,
        bnFont: String,
        enFont: String
    ) {
        val cfg = page.config
        val item = page.item

        drawCertificateBorder(c, rect, cfg.borderStyle)

        val contentLeft = rect.left + 28f
        val contentRight = rect.right - 28f
        val contentW = (contentRight - contentLeft).coerceAtLeast(200f)
        val cx = rect.centerX()

        // Faint centered watermark behind body text
        if (cfg.showWatermark) {
            val wmBmp = PdfGen.resolveLogoBitmap(ctx, "color")
                ?: PdfGen.resolveLogoBitmap(ctx, settings.dpeLogo)
            if (wmBmp != null && wmBmp.width > 0 && wmBmp.height > 0) {
                val wmSize = min(rect.width() * 0.56f, rect.height() * 0.68f)
                val wmAspect = wmBmp.width.toFloat() / wmBmp.height.toFloat()
                val wmW = if (wmAspect >= 1f) wmSize else wmSize * wmAspect
                val wmH = if (wmAspect >= 1f) wmSize / wmAspect else wmSize
                val wmCx = cx
                val wmCy = rect.top + rect.height() * 0.54f
                val wmRect = RectF(wmCx - wmW / 2f, wmCy - wmH / 2f, wmCx + wmW / 2f, wmCy + wmH / 2f)
                val wmPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = 28 }
                c.drawBitmap(wmBmp, null, wmRect, wmPaint)
            }
        }

        // Top Header with Left Govt Seal + Center Titles + Right DPE Logo
        val sealSize = 52f
        val headerTop = rect.top + 26f
        val leftSealCx = contentLeft + sealSize / 2f + 10f
        val rightLogoCx = contentRight - sealSize / 2f - 10f

        if (cfg.showGovtSeal) {
            drawBangladeshGovtSeal(ctx, c, leftSealCx, headerTop + sealSize / 2f + 6f, sealSize / 2f, bnFont, enFont)
        }
        if (cfg.showDpeLogo) {
            val dpeBmp = PdfGen.resolveLogoBitmap(ctx, "color")
                ?: PdfGen.resolveLogoBitmap(ctx, settings.dpeLogo)
            if (dpeBmp != null && dpeBmp.width > 0 && dpeBmp.height > 0) {
                val aspect = dpeBmp.width.toFloat() / dpeBmp.height.toFloat()
                val drawH = sealSize
                val drawW = drawH * aspect
                val dst = RectF(
                    rightLogoCx - drawW / 2f,
                    headerTop + 4f,
                    rightLogoCx + drawW / 2f,
                    headerTop + 4f + drawH
                )
                c.drawBitmap(dpeBmp, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
            }
        }

        val centerMaxW = (contentW - (if (cfg.showGovtSeal) 80f else 0f) - (if (cfg.showDpeLogo) 80f else 0f)).coerceAtLeast(180f)
        var y = headerTop + 10f

        if (cfg.showGovtHeader) {
            val govtLines = listOf(cfg.govtLine1, cfg.govtLine2, cfg.govtLine3)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            govtLines.forEach { line ->
                val gp = PdfGen.dualPaint(ctx, 9.8f, false, bnFont, enFont, INK_PRIMARY)
                PdfGen.shrinkMixedToFit(gp, line, centerMaxW, minSize = 7f)
                PdfGen.drawMixedText(c, line, cx, y, gp, Paint.Align.CENTER)
                y += 13f
            }
            y += 4f
        } else {
            y += 10f
        }

        val schoolName = TestimonialPlanner.resolvedSchoolName(cfg, settings)
        val schoolPaint = PdfGen.dualPaint(ctx, 18.5f, true, bnFont, enFont, INK_PRIMARY)
        PdfGen.shrinkMixedToFit(schoolPaint, schoolName, centerMaxW + 28f, step = 0.4f, minSize = 11.5f)
        PdfGen.drawMixedText(c, schoolName, cx, y + 4f, schoolPaint, Paint.Align.CENTER)
        y += 22f

        val schoolAddress = TestimonialPlanner.resolvedSchoolAddress(cfg, settings, students)
        if (schoolAddress.isNotBlank()) {
            val addrPaint = PdfGen.dualPaint(ctx, 10.2f, false, bnFont, enFont, INK_PRIMARY)
            PdfGen.shrinkMixedToFit(addrPaint, schoolAddress, centerMaxW, minSize = 7.5f)
            PdfGen.drawMixedText(c, schoolAddress, cx, y, addrPaint, Paint.Align.CENTER)
            y += 13.5f
        }

        val estd = cfg.establishedYear.trim()
        if (estd.isNotBlank()) {
            val estdText = "স্থাপিত: ${TestimonialPlanner.formatDigits(estd, cfg.numeralMode)}"
            val estdPaint = PdfGen.dualPaint(ctx, 9.6f, false, bnFont, enFont, INK_PRIMARY)
            PdfGen.drawMixedText(c, estdText, cx, y, estdPaint, Paint.Align.CENTER)
            y += 13f
        }

        y += 6f

        // Centered Scroll / Ribbon Banner
        val bannerW = 248f
        val bannerH = 38f
        drawScrollBanner(
            ctx = ctx,
            c = c,
            cx = cx,
            topY = y,
            w = bannerW,
            h = bannerH,
            title = TestimonialPlanner.resolvedBannerTitle(cfg),
            fontSize = 18.5f,
            bnFont = bnFont,
            enFont = enFont
        )
        y += bannerH + 22f

        // Serial Number (left) & Date (right)
        val metaLabelPaint = PdfGen.dualPaint(ctx, 10.6f, true, bnFont, enFont, INK_PRIMARY)
        val metaValPaint = PdfGen.dualPaint(ctx, 10.6f, false, bnFont, enFont, INK_PRIMARY)
        val serialPrefix = "সিরিয়াল নম্বর: "
        PdfGen.drawMixedText(c, serialPrefix, contentLeft, y, metaLabelPaint, Paint.Align.LEFT)
        val prefixW = PdfGen.measureMixedText(serialPrefix, metaLabelPaint)
        if (item.serialText.isBlank()) {
            val dotP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = INK_SECONDARY
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
                pathEffect = DashPathEffect(floatArrayOf(2.2f, 2.2f), 0f)
            }
            c.drawLine(contentLeft + prefixW + 2f, y + 2f, contentLeft + prefixW + 86f, y + 2f, dotP)
        } else {
            PdfGen.drawMixedText(c, item.serialText, contentLeft + prefixW, y, metaValPaint, Paint.Align.LEFT)
        }

        val dateText = TestimonialPlanner.resolvedIssueDate(cfg)
        val datePrefix = "তারিখ: "
        val dateValueW = PdfGen.measureMixedText(dateText, metaValPaint)
        PdfGen.drawMixedText(c, dateText, contentRight, y, metaValPaint, Paint.Align.RIGHT)
        PdfGen.drawMixedText(c, datePrefix, contentRight - dateValueW, y, metaLabelPaint, Paint.Align.RIGHT)

        y += 38f

        // Certificate Body with Inline Dashed Underlines
        val tokens = TestimonialPlanner.parseBodyTokens(item, cfg, settings)
        val bodyBottomLimit = rect.bottom - 102f
        val afterBodyY = drawInlineTokenParagraph(
            ctx = ctx,
            c = c,
            tokens = tokens,
            left = contentLeft + 2f,
            right = contentRight - 2f,
            startY = y,
            maxBottomY = bodyBottomLimit,
            firstLineIndent = 38f,
            baseFontSize = 11.4f,
            lineHeight = 25.5f,
            bnFont = bnFont,
            enFont = enFont
        )

        val closingLine = TestimonialPlanner.resolvedClosingLine(cfg)
        if (closingLine.isNotBlank() && afterBodyY + 14f < rect.bottom - 72f) {
            val closePaint = PdfGen.dualPaint(ctx, 11.4f, false, bnFont, enFont, INK_PRIMARY)
            val closeLines = wrapText(closingLine, closePaint, contentW - 44f)
            var closeY = afterBodyY + 12f
            closeLines.forEachIndexed { idx, line ->
                val xOff = if (idx == 0) contentLeft + 38f else contentLeft + 2f
                PdfGen.drawMixedText(c, line, xOff, closeY, closePaint, Paint.Align.LEFT)
                closeY += 20f
            }
        }

        // Bottom-Right Signature Block
        val signPlace = TestimonialPlanner.resolvedSignPlace(cfg, settings, students)
        val sigTitle = cfg.headTeacherTitle.trim().ifBlank { "(প্রধান শিক্ষক)" }
        val sigLines = buildList {
            add(sigTitle)
            add(schoolName)
            if (signPlace.isNotBlank()) add(signPlace)
        }

        val sigCenterX = contentRight - 96f
        val sigTextStartY = rect.bottom - 24f - (sigLines.size - 1) * 14f

        if (cfg.showSignatureImage && signatureBitmap != null && signatureBitmap.width > 0 && signatureBitmap.height > 0) {
            val maxW = 92f * cfg.safeSignatureScale
            val maxH = 34f * cfg.safeSignatureScale
            val scaleRatio = min(maxW / signatureBitmap.width, maxH / signatureBitmap.height)
            val drawW = (signatureBitmap.width * scaleRatio).coerceAtLeast(14f)
            val drawH = (signatureBitmap.height * scaleRatio).coerceAtLeast(8f)
            val dst = RectF(
                sigCenterX - drawW / 2f,
                sigTextStartY - 16f - drawH,
                sigCenterX + drawW / 2f,
                sigTextStartY - 16f
            )
            c.drawBitmap(signatureBitmap, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
        }

        sigLines.forEachIndexed { idx, line ->
            val p = PdfGen.dualPaint(
                ctx = ctx,
                size = if (idx == 0) 10.2f else 9.8f,
                bold = false,
                bnFontId = bnFont,
                enFontId = enFont,
                textColor = INK_PRIMARY
            )
            PdfGen.shrinkMixedToFit(p, line, 200f, minSize = 7.5f)
            PdfGen.drawMixedText(c, line, sigCenterX, sigTextStartY + idx * 14f, p, Paint.Align.CENTER)
        }
    }

    private sealed interface LayoutAtom {
        data class Word(val text: String, val width: Float, val addSpaceBefore: Boolean) : LayoutAtom
        data class BlankField(val value: String, val width: Float, val addSpaceBefore: Boolean) : LayoutAtom
    }

    private fun drawInlineTokenParagraph(
        ctx: Context?,
        c: Canvas,
        tokens: List<TestimonialBodyToken>,
        left: Float,
        right: Float,
        startY: Float,
        maxBottomY: Float,
        firstLineIndent: Float,
        baseFontSize: Float,
        lineHeight: Float,
        bnFont: String,
        enFont: String
    ): Float {
        val textPaint = PdfGen.dualPaint(ctx, baseFontSize, false, bnFont, enFont, INK_PRIMARY)
        val fieldPaint = PdfGen.dualPaint(ctx, baseFontSize, false, bnFont, enFont, INK_PRIMARY)
        val spaceW = PdfGen.measureMixedText(" ", textPaint).coerceAtLeast(3.2f)
        val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK_SECONDARY
            style = Paint.Style.STROKE
            strokeWidth = 0.75f
            pathEffect = DashPathEffect(floatArrayOf(2.2f, 2.2f), 0f)
        }

        // Flatten tokens into wrap-safe atoms
        val atoms = mutableListOf<LayoutAtom>()
        var pendingSpace = false
        tokens.forEach { tok ->
            when (tok) {
                is TestimonialBodyToken.Plain -> {
                    val parts = tok.text.split(Regex(" +"))
                    parts.forEachIndexed { idx, part ->
                        if (part.isEmpty()) {
                            pendingSpace = true
                        } else {
                            val attachPunctuation = part in setOf(",", "।", ".", ";", ":")
                            val spaceBefore = (pendingSpace || idx > 0) && !attachPunctuation
                            atoms.add(
                                LayoutAtom.Word(
                                    text = part,
                                    width = PdfGen.measureMixedText(part, textPaint),
                                    addSpaceBefore = spaceBefore
                                )
                            )
                            pendingSpace = false
                        }
                    }
                    if (tok.text.endsWith(" ")) pendingSpace = true
                }
                is TestimonialBodyToken.Field -> {
                    val v = tok.value.trim()
                    val measured = if (v.isEmpty()) 0f else PdfGen.measureMixedText(v, fieldPaint) + 28f
                    val boxW = max(tok.minWidthPt, measured).coerceAtMost(right - left - 18f)
                    atoms.add(
                        LayoutAtom.BlankField(
                            value = v,
                            width = boxW,
                            addSpaceBefore = pendingSpace
                        )
                    )
                    pendingSpace = false
                }
            }
        }

        // Greedy wrap into lines
        val lines = mutableListOf<List<LayoutAtom>>()
        var currentLine = mutableListOf<LayoutAtom>()
        var lineIndex = 0
        var curW = 0f

        fun currentMaxW(): Float = if (lineIndex == 0) (right - left - firstLineIndent) else (right - left)

        atoms.forEach { atom ->
            val sp = if (currentLine.isNotEmpty() &&
                ((atom is LayoutAtom.Word && atom.addSpaceBefore) ||
                    (atom is LayoutAtom.BlankField && atom.addSpaceBefore))
            ) spaceW else 0f
            val atomW = when (atom) {
                is LayoutAtom.Word -> atom.width
                is LayoutAtom.BlankField -> atom.width
            }
            if (currentLine.isNotEmpty() && curW + sp + atomW > currentMaxW()) {
                lines.add(currentLine)
                currentLine = mutableListOf()
                lineIndex++
                curW = 0f
            }
            val actualSpace = if (currentLine.isNotEmpty()) sp else 0f
            currentLine.add(atom)
            curW += actualSpace + atomW
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)

        val actualLineHeight = if (lines.size > 1 && startY + lines.size * lineHeight > maxBottomY) {
            ((maxBottomY - startY) / lines.size.toFloat()).coerceIn(17f, lineHeight)
        } else {
            lineHeight
        }

        var y = startY
        lines.forEachIndexed { idx, lineAtoms ->
            var x = if (idx == 0) left + firstLineIndent else left
            lineAtoms.forEachIndexed { atomIdx, atom ->
                val needsSpace = atomIdx > 0 && when (atom) {
                    is LayoutAtom.Word -> atom.addSpaceBefore
                    is LayoutAtom.BlankField -> atom.addSpaceBefore
                }
                if (needsSpace) x += spaceW
                when (atom) {
                    is LayoutAtom.Word -> {
                        PdfGen.drawMixedText(c, atom.text, x, y, textPaint, Paint.Align.LEFT)
                        x += atom.width
                    }
                    is LayoutAtom.BlankField -> {
                        c.drawLine(x, y + 3.5f, x + atom.width, y + 3.5f, dashPaint)
                        if (atom.value.isNotBlank()) {
                            val vp = PdfGen.dualPaint(ctx, baseFontSize, false, bnFont, enFont, INK_PRIMARY)
                            PdfGen.shrinkMixedToFit(vp, atom.value, atom.width - 8f, minSize = 7.5f)
                            PdfGen.drawMixedText(c, atom.value, x + atom.width / 2f, y - 0.5f, vp, Paint.Align.CENTER)
                        }
                        x += atom.width
                    }
                }
            }
            y += actualLineHeight
        }
        return y
    }

    private fun drawScrollBanner(
        ctx: Context?,
        c: Canvas,
        cx: Float,
        topY: Float,
        w: Float,
        h: Float,
        title: String,
        fontSize: Float,
        bnFont: String,
        enFont: String
    ) {
        val left = cx - w / 2f
        val right = cx + w / 2f
        val bottom = topY + h
        val curlW = (h * 0.36f).coerceIn(7f, 14f)
        val curlDrop = (h * 0.16f).coerceIn(3f, 6f)

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x220F172A
            style = Paint.Style.FILL
        }
        val bodyFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                left, topY, left, bottom,
                intArrayOf(0xFFE2E8F0.toInt(), 0xFFCBD5E1.toInt(), 0xFFE2E8F0.toInt()),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        val foldFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF94A3B8.toInt()
            style = Paint.Style.FILL
        }
        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF334155.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 0.85f
        }

        // Subtle drop shadow
        val bodyRect = RectF(left + curlW, topY, right - curlW, bottom)
        c.drawRoundRect(
            RectF(bodyRect.left + 2f, bodyRect.top + 2.5f, bodyRect.right + 2f, bodyRect.bottom + 2.5f),
            4f,
            4f,
            shadowPaint
        )

        // Left & right curled scroll ends
        val leftCurl = RectF(left, topY + curlDrop, left + curlW * 1.4f, bottom + curlDrop)
        val rightCurl = RectF(right - curlW * 1.4f, topY - curlDrop, right, bottom - curlDrop)
        c.drawRoundRect(leftCurl, curlW * 0.65f, curlW * 0.65f, bodyFill)
        c.drawRoundRect(leftCurl, curlW * 0.65f, curlW * 0.65f, outline)
        c.drawRoundRect(rightCurl, curlW * 0.65f, curlW * 0.65f, bodyFill)
        c.drawRoundRect(rightCurl, curlW * 0.65f, curlW * 0.65f, outline)

        // Back-curl shadow accents
        val foldL = RectF(left + curlW * 0.6f, topY, left + curlW * 1.3f, topY + curlDrop * 1.3f)
        c.drawRect(foldL, foldFill)
        val foldR = RectF(right - curlW * 1.3f, bottom - curlDrop * 1.3f, right - curlW * 0.6f, bottom)
        c.drawRect(foldR, foldFill)

        // Main banner ribbon face
        c.drawRoundRect(bodyRect, 5f, 5f, bodyFill)
        c.drawRoundRect(bodyRect, 5f, 5f, outline)

        val tp = PdfGen.dualPaint(ctx, fontSize, true, bnFont, enFont, INK_PRIMARY)
        PdfGen.shrinkMixedToFit(tp, title, bodyRect.width() - 16f, minSize = 7.5f)
        val textY = bodyRect.centerY() - (tp.bn.ascent() + tp.bn.descent()) / 2f
        PdfGen.drawMixedText(c, title, cx, textY, tp, Paint.Align.CENTER)
    }

    private fun drawCertificateBorder(
        c: Canvas,
        rect: RectF,
        style: TestimonialBorderStyle
    ) {
        if (style == TestimonialBorderStyle.NONE) return

        val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FRAME_DARK
            this.style = Paint.Style.STROKE
            strokeWidth = if (style == TestimonialBorderStyle.SIMPLE_CLEAN) 1.35f else 2.2f
        }
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FRAME_DARK
            this.style = Paint.Style.STROKE
            strokeWidth = 0.85f
        }

        c.drawRect(rect, outerPaint)
        val inset = if (style == TestimonialBorderStyle.SIMPLE_CLEAN) 3.5f else 5.2f
        val innerRect = RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset)
        if (style != TestimonialBorderStyle.SIMPLE_CLEAN) {
            c.drawRect(innerRect, innerPaint)
        } else {
            val hairline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = FRAME_MUTED
                this.style = Paint.Style.STROKE
                strokeWidth = 0.45f
            }
            c.drawRect(innerRect, hairline)
            return
        }

        if (style != TestimonialBorderStyle.ORNAMENTAL_CLASSIC) return

        // Ornamental baroque-style vector flourishes at each of the 4 corners
        val ornamentStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FRAME_DARK
            this.style = Paint.Style.STROKE
            strokeWidth = 1.45f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val ornamentLight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FRAME_MUTED
            this.style = Paint.Style.STROKE
            strokeWidth = 0.85f
            strokeCap = Paint.Cap.ROUND
        }
        val ornamentFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FRAME_DARK
            this.style = Paint.Style.FILL
        }

        val corners = listOf(
            Triple(innerRect.left, innerRect.top, 1f to 1f),
            Triple(innerRect.right, innerRect.top, -1f to 1f),
            Triple(innerRect.left, innerRect.bottom, 1f to -1f),
            Triple(innerRect.right, innerRect.bottom, -1f to -1f)
        )
        corners.forEach { (ox, oy, dir) ->
            val sx = dir.first
            val sy = dir.second
            c.save()
            c.translate(ox, oy)
            c.scale(sx, sy)

            // Corner background backing white + flourish
            val scrollPath = Path().apply {
                moveTo(3f, 54f)
                cubicTo(3f, 34f, 18f, 30f, 16f, 20f)
                cubicTo(14f, 11f, 28f, 8f, 34f, 18f)
                cubicTo(38f, 24f, 30f, 32f, 24f, 28f)
                moveTo(54f, 3f)
                cubicTo(34f, 3f, 30f, 18f, 20f, 16f)
                cubicTo(11f, 14f, 8f, 28f, 18f, 34f)
                cubicTo(24f, 38f, 32f, 30f, 28f, 24f)
            }
            c.drawPath(scrollPath, ornamentStroke)

            val outerCurl = Path().apply {
                moveTo(4f, 70f)
                cubicTo(12f, 56f, 6f, 44f, 14f, 38f)
                moveTo(70f, 4f)
                cubicTo(56f, 12f, 44f, 6f, 38f, 14f)
                moveTo(10f, 10f)
                cubicTo(22f, 10f, 24f, 22f, 14f, 24f)
                cubicTo(8f, 24f, 8f, 14f, 14f, 14f)
            }
            c.drawPath(outerCurl, ornamentLight)
            c.drawCircle(11f, 11f, 2.3f, ornamentFill)
            c.drawCircle(36f, 10f, 1.4f, ornamentFill)
            c.drawCircle(10f, 36f, 1.4f, ornamentFill)
            c.restore()
        }
    }

    private fun drawBangladeshGovtSeal(
        ctx: Context?,
        c: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        bnFont: String,
        enFont: String
    ) {
        val outerGreen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF006A4E.toInt()
            style = Paint.Style.FILL
        }
        val whiteRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val innerRed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF42A41.toInt()
            style = Paint.Style.FILL
        }
        val goldBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD97706.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        val mapGold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFACC15.toInt()
            style = Paint.Style.FILL
        }

        // Outer green ring, white ring track, inner red circle
        c.drawCircle(cx, cy, radius, outerGreen)
        c.drawCircle(cx, cy, radius * 0.92f, goldBorder)
        c.drawCircle(cx, cy, radius * 0.65f, whiteRing)
        c.drawCircle(cx, cy, radius * 0.58f, innerRed)

        // Stylized Bangladesh map silhouette inside red disc
        val s = radius * 0.36f
        val mapPath = Path().apply {
            moveTo(cx - 0.35f * s, cy - 0.85f * s)
            lineTo(cx + 0.10f * s, cy - 0.72f * s)
            lineTo(cx + 0.55f * s, cy - 0.42f * s)
            lineTo(cx + 0.68f * s, cy - 0.10f * s)
            lineTo(cx + 0.36f * s, cy + 0.12f * s)
            lineTo(cx + 0.58f * s, cy + 0.55f * s)
            lineTo(cx + 0.40f * s, cy + 0.85f * s)
            lineTo(cx + 0.18f * s, cy + 0.35f * s)
            lineTo(cx - 0.25f * s, cy + 0.52f * s)
            lineTo(cx - 0.52f * s, cy + 0.20f * s)
            lineTo(cx - 0.45f * s, cy - 0.25f * s)
            lineTo(cx - 0.62f * s, cy - 0.55f * s)
            close()
        }
        c.drawPath(mapPath, mapGold)

        // 4 small stars on outer ring
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFACC15.toInt()
            style = Paint.Style.FILL
        }
        listOf(-22f, 22f, 158f, 202f).forEach { deg ->
            val rad = deg * PI.toFloat() / 180f
            val sx = cx + radius * 0.79f * cos(rad)
            val sy = cy + radius * 0.79f * sin(rad)
            c.drawCircle(sx, sy, 1.5f, starPaint)
        }

        // Arc text on green ring
        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (radius * 0.19f).coerceAtLeast(4.2f)
            textAlign = Paint.Align.CENTER
            typeface = PdfGen.resolveTypeface(ctx, bnFont, true)
        }
        val textR = radius * 0.76f
        val topArc = Path().apply {
            addArc(
                RectF(cx - textR, cy - textR, cx + textR, cy + textR),
                -168f,
                156f
            )
        }
        c.drawTextOnPath("গণপ্রজাতন্ত্রী বাংলাদেশ", topArc, 0f, 2f, arcPaint)

        val bottomR = radius * 0.84f
        val bottomArc = Path().apply {
            addArc(
                RectF(cx - bottomR, cy - bottomR, cx + bottomR, cy + bottomR),
                148f,
                -116f
            )
        }
        c.drawTextOnPath("সরকার", bottomArc, 0f, -1f, arcPaint)
    }

    private fun wrapText(text: String, dp: PdfGen.DualPaint, maxW: Float): List<String> {
        val out = mutableListOf<String>()
        text.split("\n").forEach { para ->
            var cur = ""
            para.trim().split(Regex("\\s+")).forEach { word ->
                val cand = if (cur.isEmpty()) word else "$cur $word"
                if (PdfGen.measureMixedText(cand, dp) <= maxW || cur.isEmpty()) {
                    cur = cand
                } else {
                    out.add(cur)
                    cur = word
                }
            }
            if (cur.isNotEmpty()) out.add(cur)
        }
        return out
    }
}
