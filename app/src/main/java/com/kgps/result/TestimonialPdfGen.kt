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
    private const val FRAME_DARK = 0xFF111827.toInt()
    private const val FRAME_MUTED = 0xFF475569.toInt()

    fun renderPageBitmap(
        ctx: Context,
        page: TestimonialPageSpec,
        settings: Settings,
        students: List<Student>,
        signatureBitmap: Bitmap?,
        customAssets: TestimonialCustomAssets = TestimonialCustomAssets(),
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
        drawPage(ctx, c, page, settings, students, signatureBitmap, customAssets)
        return bmp
    }

    fun write(
        ctx: Context,
        pages: List<TestimonialPageSpec>,
        settings: Settings,
        students: List<Student>,
        signatureBitmap: Bitmap?,
        out: OutputStream,
        customAssets: TestimonialCustomAssets = TestimonialCustomAssets()
    ) {
        if (pages.isEmpty()) return
        val doc = PdfDocument()
        try {
            pages.forEachIndexed { idx, page ->
                val pdfPage = doc.startPage(
                    PdfDocument.PageInfo.Builder(A4_LANDSCAPE_W, A4_LANDSCAPE_H, idx + 1).create()
                )
                pdfPage.canvas.drawColor(Color.WHITE)
                drawPage(ctx, pdfPage.canvas, page, settings, students, signatureBitmap, customAssets)
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
        jobName: String,
        customAssets: TestimonialCustomAssets = TestimonialCustomAssets()
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
                        write(ctx, pages, settings, students, signatureBitmap, out, customAssets)
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
        signatureBitmap: Bitmap?,
        customAssets: TestimonialCustomAssets = TestimonialCustomAssets()
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
            val counterfoilW = min(180f, pageRect.width() * 0.232f)
            val gapW = 14f
            val counterfoilRect = RectF(
                pageRect.left,
                pageRect.top,
                pageRect.left + counterfoilW,
                pageRect.bottom
            )
            val cutX = counterfoilRect.right + gapW / 2f
            val cutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = FRAME_MUTED
                style = Paint.Style.STROKE
                strokeWidth = 0.85f
                pathEffect = DashPathEffect(floatArrayOf(3.0f, 2.8f), 0f)
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
                customAssets = customAssets,
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
                customAssets = customAssets,
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
            strokeWidth = 0.85f
        }
        c.drawRect(rect, borderPaint)

        val padX = 8f
        val innerLeft = rect.left + padX
        val innerRight = rect.right - padX
        val innerW = innerRight - innerLeft
        val cx = rect.centerX()
        var y = rect.top + 15f

        val schoolName = TestimonialPlanner.resolvedSchoolName(cfg, settings)
        val schoolPaint = PdfGen.dualPaint(ctx, 9.2f, true, bnFont, enFont, INK_PRIMARY)
        if (PdfGen.measureMixedText(schoolName, schoolPaint) <= innerW + 2f) {
            PdfGen.drawMixedText(c, schoolName, cx, y, schoolPaint, Paint.Align.CENTER)
            y += 11.5f
        } else {
            val singleLineTry = PdfGen.dualPaint(ctx, 9.2f, true, bnFont, enFont, INK_PRIMARY)
            PdfGen.shrinkMixedToFit(singleLineTry, schoolName, innerW, minSize = 7.4f)
            if (PdfGen.measureMixedText(schoolName, singleLineTry) <= innerW) {
                PdfGen.drawMixedText(c, schoolName, cx, y, singleLineTry, Paint.Align.CENTER)
                y += 11.5f
            } else {
                wrapText(schoolName, schoolPaint, innerW).take(2).forEach { line ->
                    val lp = PdfGen.dualPaint(ctx, 8.8f, true, bnFont, enFont, INK_PRIMARY)
                    PdfGen.shrinkMixedToFit(lp, line, innerW, minSize = 6.5f)
                    PdfGen.drawMixedText(c, line, cx, y, lp, Paint.Align.CENTER)
                    y += 11f
                }
            }
        }

        val address = TestimonialPlanner.resolvedSchoolAddress(cfg, settings, students)
        if (address.isNotBlank()) {
            val addrPaint = PdfGen.dualPaint(ctx, 7.5f, false, bnFont, enFont, INK_SECONDARY)
            wrapText(address, addrPaint, innerW).take(2).forEach { line ->
                val lp = PdfGen.dualPaint(ctx, 7.5f, false, bnFont, enFont, INK_SECONDARY)
                PdfGen.shrinkMixedToFit(lp, line, innerW, minSize = 6f)
                PdfGen.drawMixedText(c, line, cx, y, lp, Paint.Align.CENTER)
                y += 9.5f
            }
        }

        val estd = cfg.establishedYear.trim()
        if (estd.isNotBlank()) {
            val estdText = "স্থাপিত: ${TestimonialPlanner.formatDigits(estd, cfg.numeralMode)}"
            val estdPaint = PdfGen.dualPaint(ctx, 7.5f, false, bnFont, enFont, INK_SECONDARY)
            PdfGen.drawMixedText(c, estdText, cx, y, estdPaint, Paint.Align.CENTER)
            y += 10f
        } else {
            y += 2f
        }

        // Classic off-white scroll banner in counterfoil
        val bannerW = (innerW - 2f).coerceAtLeast(94f)
        val bannerH = 20.5f
        drawScrollBanner(
            ctx = ctx,
            c = c,
            cx = cx,
            topY = y,
            w = bannerW,
            h = bannerH,
            title = TestimonialPlanner.resolvedBannerTitle(cfg),
            fontSize = 10.2f,
            bnFont = bnFont,
            enFont = enFont
        )
        y += bannerH + 24f

        val metaPaint = PdfGen.dualPaint(ctx, 8.1f, false, bnFont, enFont, INK_PRIMARY)
        val serialVal = if (item.serialText.isBlank()) "...................." else item.serialText
        PdfGen.drawMixedText(c, "সিরিয়াল নম্বর: $serialVal", innerLeft, y, metaPaint, Paint.Align.LEFT)
        y += 13.5f
        PdfGen.drawMixedText(
            c,
            "তারিখ: ${TestimonialPlanner.resolvedIssueDate(cfg)}",
            innerLeft,
            y,
            metaPaint,
            Paint.Align.LEFT
        )
        y += 22f

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
            y += 2.2f
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
            y += 2.2f
        }

        // Bottom two rounded signature boxes
        val boxH = 56f
        val boxGap = 12f
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
            pathEffect = DashPathEffect(floatArrayOf(1.8f, 1.8f), 0f)
        }
        val boxTextPaint = PdfGen.dualPaint(ctx, 7.6f, false, bnFont, enFont, INK_PRIMARY)

        if (box1Top >= y + 4f) {
            val r1 = RectF(innerLeft + 1f, box1Top, innerRight - 1f, box1Bottom)
            c.drawRoundRect(r1, 10f, 10f, boxPaint)
            val l1 = "ছাত্র/ছাত্রীর স্বাক্ষর:"
            PdfGen.drawMixedText(c, l1, r1.left + 8f, r1.top + 24f, boxTextPaint, Paint.Align.LEFT)
            val l1End = r1.left + 8f + PdfGen.measureMixedText(l1, boxTextPaint) + 4f
            if (l1End < r1.right - 8f) {
                c.drawLine(l1End, r1.top + 25f, r1.right - 8f, r1.top + 25f, dotPaint)
            }
            PdfGen.drawMixedText(c, "তারিখ:", r1.left + 8f, r1.top + 42f, boxTextPaint, Paint.Align.LEFT)

            val r2 = RectF(innerLeft + 1f, box2Top, innerRight - 1f, box2Bottom)
            c.drawRoundRect(r2, 10f, 10f, boxPaint)
            val l2 = "প্রদানকারী শিক্ষকের স্বাক্ষর:"
            val p2 = PdfGen.dualPaint(ctx, 7.6f, false, bnFont, enFont, INK_PRIMARY)
            PdfGen.shrinkMixedToFit(p2, l2, r2.width() - 36f, minSize = 6.0f)
            PdfGen.drawMixedText(c, l2, r2.left + 8f, r2.top + 24f, p2, Paint.Align.LEFT)
            val l2End = r2.left + 8f + PdfGen.measureMixedText(l2, p2) + 4f
            if (l2End < r2.right - 8f) {
                c.drawLine(l2End, r2.top + 25f, r2.right - 8f, r2.top + 25f, dotPaint)
            }
            PdfGen.drawMixedText(c, "তারিখ:", r2.left + 8f, r2.top + 42f, boxTextPaint, Paint.Align.LEFT)
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
        val p = PdfGen.dualPaint(ctx, 8.0f, false, bnFont, enFont, INK_PRIMARY)
        val lines = wrapText(text, p, maxW).take(2)
        var curY = y
        lines.forEach { line ->
            val lp = PdfGen.dualPaint(ctx, 8.0f, false, bnFont, enFont, INK_PRIMARY)
            PdfGen.shrinkMixedToFit(lp, line, maxW, minSize = 6.2f)
            PdfGen.drawMixedText(c, line, x, curY, lp, Paint.Align.LEFT)
            curY += 11.2f
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
        customAssets: TestimonialCustomAssets,
        bnFont: String,
        enFont: String
    ) {
        val cfg = page.config
        val item = page.item

        drawCertificateBorder(
            c = c,
            rect = rect,
            style = cfg.borderStyle,
            customSideOrnament = customAssets.sideOrnament,
            sideDesignMode = cfg.sideDesignMode,
            sideDesignSizePt = cfg.safeSideDesignSizePt
        )

        val contentLeft = rect.left + 26f
        val contentRight = rect.right - 26f
        val contentW = (contentRight - contentLeft).coerceAtLeast(200f)
        val cx = rect.centerX()

        // Centered Watermark behind body text
        if (cfg.showWatermark) {
            val wmBmp = customAssets.watermarkLogo
                ?: (if (cfg.watermarkUseRightLogo) customAssets.rightLogo else null)
                ?: PdfGen.resolveLogoBitmap(ctx, "color")
                ?: PdfGen.resolveLogoBitmap(ctx, settings.dpeLogo)
            if (wmBmp != null && wmBmp.width > 0 && wmBmp.height > 0) {
                val wmSize = min(rect.width() * 0.56f, rect.height() * 0.68f)
                val wmAspect = wmBmp.width.toFloat() / wmBmp.height.toFloat()
                val wmW = if (wmAspect >= 1f) wmSize else wmSize * wmAspect
                val wmH = if (wmAspect >= 1f) wmSize / wmAspect else wmSize
                val wmCx = cx
                val wmCy = rect.top + rect.height() * 0.54f
                val wmRect = RectF(wmCx - wmW / 2f, wmCy - wmH / 2f, wmCx + wmW / 2f, wmCy + wmH / 2f)
                val wmAlphaInt = (cfg.safeWatermarkAlpha * 255f).toInt().coerceIn(8, 90)
                val wmPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = wmAlphaInt }
                c.drawBitmap(wmBmp, null, wmRect, wmPaint)
            }
        }

        // Top Header: Left Seal / Custom Left Logo + Center Titles + Right DPE / Custom Right Logo
        val logoSize = cfg.safeLogoSizePt
        val headerTop = rect.top + 24f
        val leftSealCx = contentLeft + logoSize / 2f + 12f
        val rightLogoCx = contentRight - logoSize / 2f - 12f
        val logoCy = headerTop + logoSize / 2f + 4f

        if (cfg.showGovtSeal) {
            val leftBmp = customAssets.leftLogo
            if (leftBmp != null && leftBmp.width > 0 && leftBmp.height > 0) {
                drawBitmapCenteredFit(c, leftBmp, leftSealCx, logoCy, logoSize, logoSize)
            } else {
                drawBangladeshGovtSeal(ctx, c, leftSealCx, logoCy, logoSize / 2f, bnFont)
            }
        }
        if (cfg.showDpeLogo) {
            val rightBmp = customAssets.rightLogo
                ?: PdfGen.resolveLogoBitmap(ctx, "color")
                ?: PdfGen.resolveLogoBitmap(ctx, settings.dpeLogo)
            if (rightBmp != null && rightBmp.width > 0 && rightBmp.height > 0) {
                drawBitmapCenteredFit(c, rightBmp, rightLogoCx, logoCy, logoSize, logoSize)
            }
        }

        val leftReserve = if (cfg.showGovtSeal) logoSize + 24f else 0f
        val rightReserve = if (cfg.showDpeLogo) logoSize + 24f else 0f
        val centerMaxW = (contentW - leftReserve - rightReserve).coerceAtLeast(180f)
        var y = headerTop + 10f

        if (cfg.showGovtHeader) {
            val govtLines = listOf(cfg.govtLine1, cfg.govtLine2, cfg.govtLine3)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            govtLines.forEach { line ->
                val gp = PdfGen.dualPaint(ctx, 9.7f, false, bnFont, enFont, INK_PRIMARY)
                PdfGen.shrinkMixedToFit(gp, line, centerMaxW, minSize = 7f)
                PdfGen.drawMixedText(c, line, cx, y, gp, Paint.Align.CENTER)
                y += 12.6f
            }
            y += 4.5f
        } else {
            y += 10f
        }

        val schoolName = TestimonialPlanner.resolvedSchoolName(cfg, settings)
        val schoolPaint = PdfGen.dualPaint(ctx, 18.8f, true, bnFont, enFont, INK_PRIMARY)
        PdfGen.shrinkMixedToFit(schoolPaint, schoolName, centerMaxW + 30f, step = 0.4f, minSize = 11.5f)
        PdfGen.drawMixedText(c, schoolName, cx, y + 4f, schoolPaint, Paint.Align.CENTER)
        y += 22.5f

        val schoolAddress = TestimonialPlanner.resolvedSchoolAddress(cfg, settings, students)
        if (schoolAddress.isNotBlank()) {
            val addrPaint = PdfGen.dualPaint(ctx, 10.1f, false, bnFont, enFont, INK_PRIMARY)
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

        // Centered Scroll Ribbon Banner (off-white classic scroll)
        val bannerW = 256f
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
        y += bannerH + 21f

        // Serial Number (left) & Date (right)
        val metaLabelPaint = PdfGen.dualPaint(ctx, 10.4f, true, bnFont, enFont, INK_PRIMARY)
        val metaValPaint = PdfGen.dualPaint(ctx, 10.4f, false, bnFont, enFont, INK_PRIMARY)
        val serialPrefix = "সিরিয়াল নম্বর: "
        PdfGen.drawMixedText(c, serialPrefix, contentLeft, y, metaLabelPaint, Paint.Align.LEFT)
        val prefixW = PdfGen.measureMixedText(serialPrefix, metaLabelPaint)
        if (item.serialText.isBlank()) {
            val dotP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = INK_SECONDARY
                style = Paint.Style.STROKE
                strokeWidth = 0.75f
                pathEffect = DashPathEffect(floatArrayOf(2.0f, 1.8f), 0f)
            }
            c.drawLine(contentLeft + prefixW + 2f, y + 2f, contentLeft + prefixW + 88f, y + 2f, dotP)
        } else {
            PdfGen.drawMixedText(c, item.serialText, contentLeft + prefixW, y, metaValPaint, Paint.Align.LEFT)
        }

        val dateText = TestimonialPlanner.resolvedIssueDate(cfg)
        val datePrefix = "তারিখ: "
        val dateValueW = PdfGen.measureMixedText(dateText, metaValPaint)
        PdfGen.drawMixedText(c, dateText, contentRight, y, metaValPaint, Paint.Align.RIGHT)
        PdfGen.drawMixedText(c, datePrefix, contentRight - dateValueW, y, metaLabelPaint, Paint.Align.RIGHT)

        y += 38f

        // Certificate Body with Exact Reference Inline Dashed Underlines & Stretch Justification
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
            firstLineIndent = 46f,
            baseFontSize = 11.4f,
            lineHeight = 25.5f,
            bnFont = bnFont,
            enFont = enFont
        )

        val closingLine = TestimonialPlanner.resolvedClosingLine(cfg)
        if (closingLine.isNotBlank() && afterBodyY + 12f < rect.bottom - 70f) {
            val closePaint = PdfGen.dualPaint(ctx, 11.4f, false, bnFont, enFont, INK_PRIMARY)
            val closeLines = wrapText(closingLine, closePaint, contentW - 48f)
            var closeY = afterBodyY + 11f
            closeLines.forEachIndexed { idx, line ->
                val xOff = if (idx == 0) contentLeft + 46f else contentLeft + 2f
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

        val sigCenterX = contentRight - 102f
        val sigTextStartY = rect.bottom - 24f - (sigLines.size - 1) * 14f

        if (cfg.showSignatureImage && signatureBitmap != null && signatureBitmap.width > 0 && signatureBitmap.height > 0) {
            val maxW = 92f * cfg.safeSignatureScale
            val maxH = 34f * cfg.safeSignatureScale
            val scaleRatio = min(maxW / signatureBitmap.width, maxH / signatureBitmap.height)
            val drawW = (signatureBitmap.width * scaleRatio).coerceAtLeast(14f)
            val drawH = (signatureBitmap.height * scaleRatio).coerceAtLeast(8f)
            val dst = RectF(
                sigCenterX - drawW / 2f,
                sigTextStartY - 15f - drawH,
                sigCenterX + drawW / 2f,
                sigTextStartY - 15f
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

    private fun drawBitmapCenteredFit(
        c: Canvas,
        bmp: Bitmap,
        cx: Float,
        cy: Float,
        boxW: Float,
        boxH: Float
    ) {
        val aspect = bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f)
        val drawW: Float
        val drawH: Float
        if (aspect >= boxW / boxH) {
            drawW = boxW
            drawH = boxW / aspect
        } else {
            drawH = boxH
            drawW = boxH * aspect
        }
        val dst = RectF(cx - drawW / 2f, cy - drawH / 2f, cx + drawW / 2f, cy + drawH / 2f)
        c.drawBitmap(bmp, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
    }

    private sealed interface LayoutAtom {
        data class Word(val text: String, val width: Float, val addSpaceBefore: Boolean) : LayoutAtom
        data class BlankField(val value: String, var width: Float, val addSpaceBefore: Boolean) : LayoutAtom
    }

    private data class AtomCluster(
        val atoms: List<LayoutAtom>,
        val totalWidth: Float,
        val leadingSpace: Boolean
    )

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
            strokeWidth = 0.7f
            pathEffect = DashPathEffect(floatArrayOf(2.0f, 1.8f), 0f)
        }

        // Flatten tokens into raw atoms
        val rawAtoms = mutableListOf<LayoutAtom>()
        var pendingSpace = false
        tokens.forEach { tok ->
            when (tok) {
                is TestimonialBodyToken.Plain -> {
                    val parts = tok.text.split(Regex(" +"))
                    parts.forEachIndexed { idx, part ->
                        if (part.isEmpty()) {
                            pendingSpace = true
                        } else {
                            val isPunct = part in setOf(",", "।", ".", ";", ":")
                            val spaceBefore = (pendingSpace || idx > 0) && !isPunct
                            rawAtoms.add(
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
                    val measured = if (v.isEmpty()) 0f else PdfGen.measureMixedText(v, fieldPaint) + 26f
                    val boxW = max(tok.minWidthPt, measured).coerceAtMost(right - left - 20f)
                    rawAtoms.add(
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

        // Group label words (ending with ":") with their immediately following BlankField,
        // and attach trailing punctuation (",", "।", etc.) so labels & commas never orphan across lines.
        val clusters = mutableListOf<AtomCluster>()
        var idx = 0
        while (idx < rawAtoms.size) {
            val group = mutableListOf<LayoutAtom>()
            val first = rawAtoms[idx]
            val leadingSpace = when (first) {
                is LayoutAtom.Word -> first.addSpaceBefore
                is LayoutAtom.BlankField -> first.addSpaceBefore
            }
            group.add(first)
            idx++

            // If word ends with ":" and next atom is a BlankField, bind them together
            if (first is LayoutAtom.Word && first.text.endsWith(":") && idx < rawAtoms.size && rawAtoms[idx] is LayoutAtom.BlankField) {
                group.add(rawAtoms[idx])
                idx++
            }

            // Attach trailing standalone punctuation (like "," or "।")
            while (idx < rawAtoms.size) {
                val peek = rawAtoms[idx]
                if (peek is LayoutAtom.Word && peek.text in setOf(",", "।", ".", ";") && !peek.addSpaceBefore) {
                    group.add(peek)
                    idx++
                } else {
                    break
                }
            }

            var groupW = 0f
            group.forEachIndexed { gIdx, a ->
                if (gIdx > 0) {
                    val sp = when (a) {
                        is LayoutAtom.Word -> if (a.addSpaceBefore) spaceW else 2.2f
                        is LayoutAtom.BlankField -> if (a.addSpaceBefore) spaceW else 0f
                    }
                    groupW += sp
                }
                groupW += when (a) {
                    is LayoutAtom.Word -> a.width
                    is LayoutAtom.BlankField -> a.width
                }
            }
            clusters.add(AtomCluster(group, groupW, leadingSpace))
        }

        // Greedy wrap clusters into lines
        val lines = mutableListOf<MutableList<LayoutAtom>>()
        var currentLine = mutableListOf<LayoutAtom>()
        var curW = 0f

        fun maxWForLine(lineIdx: Int): Float = if (lineIdx == 0) (right - left - firstLineIndent) else (right - left)

        clusters.forEach { cluster ->
            val sp = if (currentLine.isNotEmpty() && cluster.leadingSpace) spaceW else 0f
            val limit = maxWForLine(lines.size)
            if (currentLine.isNotEmpty() && curW + sp + cluster.totalWidth > limit) {
                lines.add(currentLine)
                currentLine = mutableListOf()
                curW = 0f
            }
            val actualSp = if (currentLine.isNotEmpty() && cluster.leadingSpace) spaceW else 0f
            cluster.atoms.forEachIndexed { aIdx, atom ->
                val adjusted = if (currentLine.isEmpty() && aIdx == 0) {
                    when (atom) {
                        is LayoutAtom.Word -> atom.copy(addSpaceBefore = false)
                        is LayoutAtom.BlankField -> atom.copy(addSpaceBefore = false)
                    }
                } else {
                    atom
                }
                currentLine.add(adjusted)
            }
            curW += actualSp + cluster.totalWidth
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)

        // Expand BlankField dashed widths gently across lines with leftover slack
        lines.forEachIndexed { lIdx, lineAtoms ->
            val fieldAtoms = lineAtoms.filterIsInstance<LayoutAtom.BlankField>()
            if (fieldAtoms.isNotEmpty() && (lIdx < lines.lastIndex || fieldAtoms.size >= 2)) {
                val limit = maxWForLine(lIdx)
                val used = measureLineWidth(lineAtoms, spaceW)
                val slack = (limit - used).coerceAtLeast(0f)
                if (slack > 8f) {
                    val extraTotal = (slack * 0.92f).coerceAtMost(fieldAtoms.size * 115f)
                    val perField = extraTotal / fieldAtoms.size.toFloat()
                    fieldAtoms.forEach { it.width += perField }
                }
            }
        }

        val actualLineHeight = if (lines.size > 1 && startY + lines.size * lineHeight > maxBottomY) {
            ((maxBottomY - startY) / lines.size.toFloat()).coerceIn(17f, lineHeight)
        } else {
            lineHeight
        }

        var y = startY
        lines.forEachIndexed { lIdx, lineAtoms ->
            var x = if (lIdx == 0) left + firstLineIndent else left
            lineAtoms.forEachIndexed { atomIdx, atom ->
                if (atomIdx > 0) {
                    val needsFullSpace = when (atom) {
                        is LayoutAtom.Word -> atom.addSpaceBefore
                        is LayoutAtom.BlankField -> atom.addSpaceBefore
                    }
                    x += if (needsFullSpace) {
                        spaceW
                    } else if (atom is LayoutAtom.Word && atom.text in setOf(",", "।", ".", ";")) {
                        2.5f
                    } else {
                        0f
                    }
                }
                when (atom) {
                    is LayoutAtom.Word -> {
                        PdfGen.drawMixedText(c, atom.text, x, y, textPaint, Paint.Align.LEFT)
                        x += atom.width
                    }
                    is LayoutAtom.BlankField -> {
                        c.drawLine(x, y + 2.8f, x + atom.width, y + 2.8f, dashPaint)
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

    private fun measureLineWidth(lineAtoms: List<LayoutAtom>, spaceW: Float): Float {
        var total = 0f
        lineAtoms.forEachIndexed { idx, atom ->
            if (idx > 0) {
                val sp = when (atom) {
                    is LayoutAtom.Word -> if (atom.addSpaceBefore) spaceW else if (atom.text in setOf(",", "।", ".", ";")) 2.5f else 0f
                    is LayoutAtom.BlankField -> if (atom.addSpaceBefore) spaceW else 0f
                }
                total += sp
            }
            total += when (atom) {
                is LayoutAtom.Word -> atom.width
                is LayoutAtom.BlankField -> atom.width
            }
        }
        return total
    }

    /**
     * Authentic crisp off-white classic MS Word / Certificate horizontal scroll banner:
     * Left curl hangs slightly down; right curl rises slightly up; clean paper-tint shading with zero heavy drop box.
     */
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
        val curlW = (h * 0.34f).coerceIn(6.5f, 13f)
        val curlOffset = (h * 0.14f).coerceIn(2.8f, 5.5f)

        val faceFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                left, topY, left, bottom,
                intArrayOf(0xFFFFFFFF.toInt(), 0xFFF3F6FA.toInt(), 0xFFE9EEF5.toInt()),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        val curlFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                left, topY, left, bottom,
                intArrayOf(0xFFF8FAFC.toInt(), 0xFFE2E8F0.toInt(), 0xFFCBD5E1.toInt()),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        val foldShadowFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFC5CED9.toInt()
            style = Paint.Style.FILL
        }
        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1E293B.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 0.85f
            strokeJoin = Paint.Join.ROUND
        }

        val bodyLeft = left + curlW * 0.9f
        val bodyRight = right - curlW * 0.9f

        // Left rolled scroll end (extends down slightly past bottom)
        val leftCurlRect = RectF(left, topY + curlOffset, left + curlW * 1.45f, bottom + curlOffset)
        c.drawRoundRect(leftCurlRect, curlW * 0.6f, curlW * 0.6f, curlFill)
        c.drawRoundRect(leftCurlRect, curlW * 0.6f, curlW * 0.6f, outline)

        // Right rolled scroll end (extends up slightly past top)
        val rightCurlRect = RectF(right - curlW * 1.45f, topY - curlOffset, right, bottom - curlOffset)
        c.drawRoundRect(rightCurlRect, curlW * 0.6f, curlW * 0.6f, curlFill)
        c.drawRoundRect(rightCurlRect, curlW * 0.6f, curlW * 0.6f, outline)

        // Main center banner face
        val bodyRect = RectF(bodyLeft, topY, bodyRight, bottom)
        c.drawRoundRect(bodyRect, 5f, 5f, faceFill)
        c.drawRoundRect(bodyRect, 5f, 5f, outline)

        // Inner small curled spiral folds at bottom-left & top-right of banner face
        val leftSpiral = RectF(bodyLeft - curlW * 0.45f, bottom - curlOffset * 0.2f, bodyLeft + curlW * 0.45f, bottom + curlOffset)
        c.drawRoundRect(leftSpiral, curlW * 0.35f, curlW * 0.35f, foldShadowFill)
        c.drawRoundRect(leftSpiral, curlW * 0.35f, curlW * 0.35f, outline)

        val rightSpiral = RectF(bodyRight - curlW * 0.45f, topY - curlOffset, bodyRight + curlW * 0.45f, topY + curlOffset * 0.2f)
        c.drawRoundRect(rightSpiral, curlW * 0.35f, curlW * 0.35f, foldShadowFill)
        c.drawRoundRect(rightSpiral, curlW * 0.35f, curlW * 0.35f, outline)

        val tp = PdfGen.dualPaint(ctx, fontSize, true, bnFont, enFont, INK_PRIMARY)
        PdfGen.shrinkMixedToFit(tp, title, bodyRect.width() - 16f, minSize = 7.5f)
        val textY = bodyRect.centerY() - (tp.bn.ascent() + tp.bn.descent()) / 2f
        PdfGen.drawMixedText(c, title, cx, textY, tp, Paint.Align.CENTER)
    }

    private fun drawCertificateBorder(
        c: Canvas,
        rect: RectF,
        style: TestimonialBorderStyle,
        customSideOrnament: Bitmap? = null,
        sideDesignMode: TestimonialSideDesignMode = TestimonialSideDesignMode.FOUR_CORNERS,
        sideDesignSizePt: Float = 88f
    ) {
        if (customSideOrnament != null && customSideOrnament.width > 0 && customSideOrnament.height > 0 &&
            sideDesignMode == TestimonialSideDesignMode.FULL_FRAME
        ) {
            c.drawBitmap(customSideOrnament, null, rect, Paint(Paint.FILTER_BITMAP_FLAG))
            return
        }

        if (style == TestimonialBorderStyle.NONE) {
            if (customSideOrnament != null && customSideOrnament.width > 0 && customSideOrnament.height > 0) {
                drawCustomCornerOrnaments(c, rect, customSideOrnament, sideDesignSizePt)
            }
            return
        }

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

        if (style == TestimonialBorderStyle.SIMPLE_CLEAN) {
            val hairline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = FRAME_MUTED
                this.style = Paint.Style.STROKE
                strokeWidth = 0.45f
            }
            c.drawRect(innerRect, hairline)
            if (customSideOrnament != null && customSideOrnament.width > 0 && customSideOrnament.height > 0) {
                drawCustomCornerOrnaments(c, innerRect, customSideOrnament, sideDesignSizePt)
            }
            return
        }

        // For DOUBLE_CLASSIC & ORNAMENTAL_CLASSIC, draw clean double frame with notched corner pockets if ornamental
        if (style == TestimonialBorderStyle.DOUBLE_CLASSIC) {
            c.drawRect(innerRect, innerPaint)
            if (customSideOrnament != null && customSideOrnament.width > 0 && customSideOrnament.height > 0) {
                drawCustomCornerOrnaments(c, innerRect, customSideOrnament, sideDesignSizePt)
            }
            return
        }

        // ORNAMENTAL_CLASSIC: Draw inner frame with classic subtle concave corner notches + rich baroque corner flourishes
        val notch = 16f
        val notchedFrame = Path().apply {
            moveTo(innerRect.left + notch, innerRect.top)
            lineTo(innerRect.right - notch, innerRect.top)
            quadTo(innerRect.right - notch * 0.45f, innerRect.top + notch * 0.45f, innerRect.right, innerRect.top + notch)
            lineTo(innerRect.right, innerRect.bottom - notch)
            quadTo(innerRect.right - notch * 0.45f, innerRect.bottom - notch * 0.45f, innerRect.right - notch, innerRect.bottom)
            lineTo(innerRect.left + notch, innerRect.bottom)
            quadTo(innerRect.left + notch * 0.45f, innerRect.bottom - notch * 0.45f, innerRect.left, innerRect.bottom - notch)
            lineTo(innerRect.left, innerRect.top + notch)
            quadTo(innerRect.left + notch * 0.45f, innerRect.top + notch * 0.45f, innerRect.left + notch, innerRect.top)
            close()
        }
        c.drawPath(notchedFrame, innerPaint)

        if (customSideOrnament != null && customSideOrnament.width > 0 && customSideOrnament.height > 0) {
            drawCustomCornerOrnaments(c, innerRect, customSideOrnament, sideDesignSizePt)
            return
        }

        // Rich symmetrical baroque acanthus & scroll corner flourishes matching the reference PDF
        val boldStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FRAME_DARK
            this.style = Paint.Style.STROKE
            strokeWidth = 1.6f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val midStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF334155.toInt()
            this.style = Paint.Style.STROKE
            strokeWidth = 0.95f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val fillDark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
            c.save()
            c.translate(ox, oy)
            c.scale(dir.first, dir.second)

            // Diagonal corner leaf/fleur-de-lis spear
            val cornerLeaf = Path().apply {
                moveTo(7f, 7f)
                cubicTo(14f, 6f, 24f, 14f, 29f, 29f)
                cubicTo(14f, 24f, 6f, 14f, 7f, 7f)
                close()
            }
            c.drawPath(cornerLeaf, fillDark)

            // Inner white highlight rib inside diagonal leaf
            val ribPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                this.style = Paint.Style.STROKE
                strokeWidth = 0.9f
                strokeCap = Paint.Cap.ROUND
            }
            c.drawLine(10f, 10f, 21f, 21f, ribPaint)

            // Primary sweeping S-scrolls along vertical & horizontal edges
            val primaryScrolls = Path().apply {
                // Vertical arm
                moveTo(4f, 78f)
                cubicTo(4f, 52f, 20f, 44f, 18f, 30f)
                cubicTo(16f, 20f, 6f, 22f, 8f, 32f)
                cubicTo(10f, 39f, 18f, 36f, 15f, 28f)
                // Horizontal arm (mirror)
                moveTo(78f, 4f)
                cubicTo(52f, 4f, 44f, 20f, 30f, 18f)
                cubicTo(20f, 16f, 22f, 6f, 32f, 8f)
                cubicTo(39f, 10f, 36f, 18f, 28f, 15f)
            }
            c.drawPath(primaryScrolls, boldStroke)

            // Secondary filigree tendrils & outer spiral curls
            val filigree = Path().apply {
                // Vertical outer tendril
                moveTo(3f, 88f)
                cubicTo(9f, 72f, 16f, 64f, 12f, 52f)
                cubicTo(10f, 45f, 22f, 44f, 24f, 52f)
                // Horizontal outer tendril
                moveTo(88f, 3f)
                cubicTo(72f, 9f, 64f, 16f, 52f, 12f)
                cubicTo(45f, 10f, 44f, 22f, 52f, 24f)
                // Inner corner embrace arc
                moveTo(22f, 34f)
                cubicTo(26f, 28f, 28f, 26f, 34f, 22f)
            }
            c.drawPath(filigree, midStroke)

            // Filled acanthus tear-buds
            val vBud = Path().apply {
                moveTo(8f, 58f)
                cubicTo(14f, 53f, 16f, 47f, 11f, 45f)
                cubicTo(7f, 48f, 6f, 54f, 8f, 58f)
                close()
            }
            c.drawPath(vBud, fillDark)

            val hBud = Path().apply {
                moveTo(58f, 8f)
                cubicTo(53f, 14f, 47f, 16f, 45f, 11f)
                cubicTo(48f, 7f, 54f, 6f, 58f, 8f)
                close()
            }
            c.drawPath(hBud, fillDark)

            c.drawCircle(24f, 52f, 1.5f, fillDark)
            c.drawCircle(52f, 24f, 1.5f, fillDark)
            c.drawCircle(11f, 70f, 1.2f, fillDark)
            c.drawCircle(70f, 11f, 1.2f, fillDark)

            c.restore()
        }
    }

    private fun drawCustomCornerOrnaments(
        c: Canvas,
        frameRect: RectF,
        ornamentBmp: Bitmap,
        sizePt: Float
    ) {
        val aspect = ornamentBmp.width.toFloat() / ornamentBmp.height.toFloat().coerceAtLeast(1f)
        val drawW = if (aspect >= 1f) sizePt else sizePt * aspect
        val drawH = if (aspect >= 1f) sizePt / aspect else sizePt
        val pad = 4f
        val dst = RectF(pad, pad, pad + drawW, pad + drawH)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        val corners = listOf(
            Triple(frameRect.left, frameRect.top, 1f to 1f),
            Triple(frameRect.right, frameRect.top, -1f to 1f),
            Triple(frameRect.left, frameRect.bottom, 1f to -1f),
            Triple(frameRect.right, frameRect.bottom, -1f to -1f)
        )
        corners.forEach { (ox, oy, dir) ->
            c.save()
            c.translate(ox, oy)
            c.scale(dir.first, dir.second)
            c.drawBitmap(ornamentBmp, null, dst, paint)
            c.restore()
        }
    }

    private fun drawBangladeshGovtSeal(
        ctx: Context?,
        c: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        bnFont: String
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

        c.drawCircle(cx, cy, radius, outerGreen)
        c.drawCircle(cx, cy, radius * 0.92f, goldBorder)
        c.drawCircle(cx, cy, radius * 0.65f, whiteRing)
        c.drawCircle(cx, cy, radius * 0.58f, innerRed)

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

        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFACC15.toInt()
            style = Paint.Style.FILL
        }
        listOf(-18f, 18f, 162f, 198f).forEach { deg ->
            val rad = deg * PI.toFloat() / 180f
            val sx = cx + radius * 0.79f * cos(rad)
            val sy = cy + radius * 0.79f * sin(rad)
            c.drawCircle(sx, sy, 1.4f, starPaint)
        }

        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (radius * 0.175f).coerceAtLeast(4.0f)
            textAlign = Paint.Align.CENTER
            typeface = PdfGen.resolveTypeface(ctx, bnFont, true)
        }
        val textR = radius * 0.76f
        val topArc = Path().apply {
            addArc(
                RectF(cx - textR, cy - textR, cx + textR, cy + textR),
                -165f,
                150f
            )
        }
        c.drawTextOnPath("গণপ্রজাতন্ত্রী বাংলাদেশ", topArc, 0f, 1.8f, arcPaint)

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
