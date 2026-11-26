package com.kgps.result

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
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
import kotlin.math.min

object AdmitCardPdfGen {
    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842

    private val tableLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF000000.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 0.65f
    }

    private fun makeDashPaint(dashStyle: AdmitDashStyle, strokeWidth: Float, colorInt: Int): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorInt
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            pathEffect = DashPathEffect(dashStyle.intervals, 0f)
        }

    private fun makeBorderPaint(
        style: AdmitBorderStyle,
        dashStyle: AdmitDashStyle,
        strokeWidth: Float
    ): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF000000.toInt()
            this.style = Paint.Style.STROKE
            this.strokeWidth = if (style == AdmitBorderStyle.THICK_THIN) {
                (strokeWidth * 1.45f).coerceAtLeast(1.1f)
            } else {
                strokeWidth
            }
            if (style == AdmitBorderStyle.DASHED) {
                pathEffect = DashPathEffect(dashStyle.intervals, 0f)
            }
        }

    fun renderPageBitmap(
        ctx: Context,
        page: AdmitCardPageSpec,
        settings: Settings,
        signatureBitmap: Bitmap?,
        scale: Float = 2.0f
    ): Bitmap {
        val bmp = Bitmap.createBitmap((A4_WIDTH * scale).toInt(), (A4_HEIGHT * scale).toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        c.scale(scale, scale)
        drawPage(ctx, c, page, settings, signatureBitmap)
        return bmp
    }

    fun write(
        ctx: Context,
        pages: List<AdmitCardPageSpec>,
        settings: Settings,
        signatureBitmap: Bitmap?,
        out: OutputStream
    ) {
        if (pages.isEmpty()) return
        val doc = PdfDocument()
        try {
            pages.forEachIndexed { idx, page ->
                val pdfPage = doc.startPage(PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, idx + 1).create())
                pdfPage.canvas.drawColor(Color.WHITE)
                drawPage(ctx, pdfPage.canvas, page, settings, signatureBitmap)
                doc.finishPage(pdfPage)
            }
            doc.writeTo(out)
        } finally {
            doc.close()
        }
    }

    fun print(
        ctx: Context,
        pages: List<AdmitCardPageSpec>,
        settings: Settings,
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
                        write(ctx, pages, settings, signatureBitmap, out)
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
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asPortrait())
                .build()
        )
    }

    internal fun drawPage(
        ctx: Context?,
        c: Canvas,
        page: AdmitCardPageSpec,
        settings: Settings,
        signatureBitmap: Bitmap?
    ) {
        val cfg = page.config
        val marginPt = cfg.safeMarginPt
        val gridLeft = marginPt
        val gridTop = marginPt
        val gridRight = (A4_WIDTH - marginPt).coerceAtLeast(gridLeft + 180f)
        val gridBottom = (A4_HEIGHT - marginPt).coerceAtLeast(gridTop + 240f)
        val gridH = gridBottom - gridTop

        val rows = cfg.safeCardsPerPage
        val cellH = gridH / rows.toFloat()

        val centerDividerPaint = makeDashPaint(cfg.dashStyle, 0.7f, 0xFF1A1A1A.toInt())
        val cutGuidePaint = makeDashPaint(cfg.dashStyle, 0.85f, 0xFF000000.toInt())

        // Full-width horizontal cut lines between stacked cards
        if (cfg.showCutLines) {
            val cutLeft = (gridLeft - 12f).coerceAtLeast(6f)
            val cutRight = (gridRight + 12f).coerceAtMost(A4_WIDTH - 6f)
            for (r in 1 until rows) {
                val y = gridTop + r * cellH
                c.drawLine(cutLeft, y, cutRight, y, cutGuidePaint)
            }
        }

        val schoolName = AdmitCardPlanner.resolvedSchoolName(cfg, settings)
        val schoolAddress = AdmitCardPlanner.resolvedSchoolAddress(cfg, settings)
        val examTitle = AdmitCardPlanner.resolvedExamTitle(cfg, settings)
        val routineTitle = AdmitCardPlanner.resolvedRoutineTitle(cfg, settings)
        val badgeText = cfg.badgeLabel.trim().ifBlank { "প্রবেশপত্র" }

        val bnFont = cfg.bnFontId.ifBlank { settings.pdfFont }
        val enFont = cfg.enFontId.ifBlank { settings.pdfEnglishFont }

        val insetY = (cfg.safeVerticalGapPt / 2f).coerceAtMost(cellH * 0.24f)
        val borderPaint = makeBorderPaint(cfg.borderStyle, cfg.dashStyle, cfg.borderWeight.strokePt)
        val innerSecondaryPaint = when (cfg.borderStyle) {
            AdmitBorderStyle.DOUBLE -> Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF000000.toInt()
                style = Paint.Style.STROKE
                strokeWidth = (cfg.borderWeight.strokePt * 0.7f).coerceAtLeast(0.5f)
            }
            AdmitBorderStyle.THICK_THIN -> Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF000000.toInt()
                style = Paint.Style.STROKE
                strokeWidth = (cfg.borderWeight.strokePt * 0.55f).coerceAtLeast(0.45f)
            }
            else -> null
        }
        val cornerAccentPaint = if (cfg.borderStyle == AdmitBorderStyle.CORNER_ACCENT) {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF000000.toInt()
                style = Paint.Style.STROKE
                strokeWidth = (cfg.borderWeight.strokePt * 2.0f).coerceAtLeast(1.4f)
                strokeCap = Paint.Cap.SQUARE
            }
        } else null

        for (r in 0 until rows) {
            val item = page.items.getOrNull(r) ?: continue
            val cellTop = gridTop + r * cellH
            val cardRect = RectF(
                gridLeft,
                cellTop + insetY,
                gridRight,
                cellTop + cellH - insetY
            )
            val cardW = cardRect.width().coerceAtLeast(140f)
            val cardH = cardRect.height().coerceAtLeast(80f)
            val heightScale = (cardH / 184f).coerceIn(0.68f, 1.38f)
            val densityScale = (cardH / 184f).coerceIn(0.82f, 1.18f)

            val maxRadius = min(cardW, cardH) * 0.25f
            val radius = if (cfg.borderStyle == AdmitBorderStyle.CORNER_ACCENT) {
                0f
            } else {
                (cfg.safeCornerRadiusPt * heightScale).coerceIn(0f, maxRadius)
            }

            if (cfg.borderStyle != AdmitBorderStyle.NONE) {
                c.drawRoundRect(cardRect, radius, radius, borderPaint)
                if (innerSecondaryPaint != null) {
                    val gap = (cfg.borderWeight.strokePt + 2.1f).coerceAtMost(min(cardW, cardH) * 0.08f)
                    val innerRect = RectF(
                        cardRect.left + gap,
                        cardRect.top + gap,
                        cardRect.right - gap,
                        cardRect.bottom - gap
                    )
                    val innerRadius = (radius - gap * 0.7f).coerceAtLeast(0f)
                    c.drawRoundRect(innerRect, innerRadius, innerRadius, innerSecondaryPaint)
                }
                if (cornerAccentPaint != null) {
                    val arm = min(14f, min(cardW, cardH) * 0.14f)
                    c.drawLine(cardRect.left, cardRect.top, cardRect.left + arm, cardRect.top, cornerAccentPaint)
                    c.drawLine(cardRect.left, cardRect.top, cardRect.left, cardRect.top + arm, cornerAccentPaint)
                    c.drawLine(cardRect.right - arm, cardRect.top, cardRect.right, cardRect.top, cornerAccentPaint)
                    c.drawLine(cardRect.right, cardRect.top, cardRect.right, cardRect.top + arm, cornerAccentPaint)
                    c.drawLine(cardRect.left, cardRect.bottom, cardRect.left + arm, cardRect.bottom, cornerAccentPaint)
                    c.drawLine(cardRect.left, cardRect.bottom - arm, cardRect.left, cardRect.bottom, cornerAccentPaint)
                    c.drawLine(cardRect.right - arm, cardRect.bottom, cardRect.right, cardRect.bottom, cornerAccentPaint)
                    c.drawLine(cardRect.right, cardRect.bottom - arm, cardRect.right, cardRect.bottom, cornerAccentPaint)
                }
            }

            val padX = 9.5f
            val padY = (7.5f * heightScale).coerceIn(5f, 13f)
            val contentLeft = cardRect.left + padX
            val contentRight = cardRect.right - padX
            val contentTop = cardRect.top + padY
            val contentBottom = cardRect.bottom - padY
            val contentW = (contentRight - contentLeft).coerceAtLeast(100f)

            val showRoutine = cfg.showRoutineTable
            val leftW = if (showRoutine) contentW * 0.445f else contentW
            val gapW = if (showRoutine) contentW * 0.024f else 0f
            val leftEndX = contentLeft + leftW
            val leftCenterX = contentLeft + leftW / 2f

            // Vertical dashed divider separating left student card & right routine table
            if (showRoutine && cfg.showCenterDivider) {
                val dividerX = leftEndX + gapW / 2f
                c.drawLine(
                    dividerX,
                    cardRect.top + 3f,
                    dividerX,
                    cardRect.bottom - 3f,
                    centerDividerPaint
                )
            }

            // --- LEFT PANEL: School Header, প্রবেশপত্র Badge, Student Info, Head Teacher Signature ---
            val schoolSize = cfg.safeSchoolFontPt * densityScale
            val metaSize = cfg.safeHeaderMetaFontPt * densityScale
            var curY = contentTop + (schoolSize * 0.95f).coerceIn(6.5f, 16f)
            val schoolStep = (schoolSize * 1.18f).coerceIn(8.0f, 20f)
            val metaStep = (metaSize * 1.24f).coerceIn(7.8f, 19f)

            if (schoolName.isNotBlank()) {
                val dp = PdfGen.dualPaint(ctx, schoolSize, cfg.headerBold, bnFont, enFont, 0xFF000000.toInt())
                PdfGen.shrinkMixedToFit(dp, schoolName, leftW - 4f, step = 0.25f, minSize = 5.5f)
                PdfGen.drawMixedText(c, schoolName, leftCenterX, curY, dp, Paint.Align.CENTER)
                curY += schoolStep
            }
            if (schoolAddress.isNotBlank()) {
                val dp = PdfGen.dualPaint(ctx, metaSize, cfg.headerBold, bnFont, enFont, 0xFF000000.toInt())
                PdfGen.shrinkMixedToFit(dp, schoolAddress, leftW - 4f, step = 0.25f, minSize = 5.2f)
                PdfGen.drawMixedText(c, schoolAddress, leftCenterX, curY, dp, Paint.Align.CENTER)
                curY += metaStep + (1.2f * heightScale)
            }
            if (examTitle.isNotBlank()) {
                val dp = PdfGen.dualPaint(ctx, metaSize, cfg.headerBold, bnFont, enFont, 0xFF000000.toInt())
                PdfGen.shrinkMixedToFit(dp, examTitle, leftW - 4f, step = 0.25f, minSize = 5.2f)
                PdfGen.drawMixedText(c, examTitle, leftCenterX, curY, dp, Paint.Align.CENTER)
                curY += metaStep + (2.4f * heightScale)
            }

            // "প্রবেশপত্র" title badge
            val badgeDp = PdfGen.dualPaint(ctx, cfg.safeBadgeFontPt * densityScale, cfg.badgeBold, bnFont, enFont, 0xFF000000.toInt()).apply {
                bn.isUnderlineText = cfg.badgeUnderline
                en.isUnderlineText = cfg.badgeUnderline
            }
            PdfGen.shrinkMixedToFit(badgeDp, badgeText, leftW - 8f, step = 0.2f, minSize = 5.8f)
            PdfGen.drawMixedText(c, badgeText, leftCenterX, curY, badgeDp, Paint.Align.CENTER)

            // Reserve signature area at bottom-right of Left Panel (exact text-width line + clean non-overlapping gaps)
            val sigLabelText = cfg.headTeacherTitle.trim().ifBlank { "প্রধান শিক্ষকের স্বাক্ষর" }
            val sigLabelDp = PdfGen.dualPaint(
                ctx,
                cfg.safeSignatureTitleFontPt * densityScale,
                cfg.signatureTitleBold,
                bnFont,
                enFont,
                0xFF000000.toInt()
            )
            PdfGen.shrinkMixedToFit(sigLabelDp, sigLabelText, leftW * 0.54f, step = 0.2f, minSize = 5.0f)
            val sigTextW = PdfGen.measureMixedText(sigLabelText, sigLabelDp)
            val sigHalfTextW = sigTextW / 2f
            val sigAnchorX = (leftEndX - (leftW * 0.26f)).coerceIn(
                contentLeft + 2f + sigHalfTextW,
                (leftEndX - 2f - sigHalfTextW).coerceAtLeast(contentLeft + 2f + sigHalfTextW)
            )
            val sigLabelY = contentBottom - (1.5f * heightScale)
            PdfGen.drawMixedText(c, sigLabelText, sigAnchorX, sigLabelY, sigLabelDp, Paint.Align.CENTER)

            val sigFm = sigLabelDp.bn.fontMetrics
            val sigTextTopY = sigLabelY + sigFm.ascent // ascent is negative; actual top edge of Bengali glyphs/matra

            val sigAreaBottom = if (cfg.showSignatureLine) {
                val labelToLineGap = (4.2f * heightScale).coerceIn(3.2f, 6.5f)
                val sigLineY = sigTextTopY - labelToLineGap
                val lineHalfW = sigHalfTextW.coerceAtLeast(10f)
                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF000000.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = 0.65f
                }
                c.drawLine(sigAnchorX - lineHalfW, sigLineY, sigAnchorX + lineHalfW, sigLineY, linePaint)
                val lineToSignGap = (4.8f * heightScale).coerceIn(3.6f, 7.5f)
                sigLineY - lineToSignGap
            } else {
                val labelToSignGap = (4.5f * heightScale).coerceIn(3.4f, 7.0f)
                sigTextTopY - labelToSignGap
            }

            val preferredSigTop = (contentBottom - (46f * heightScale)).coerceAtLeast(curY + 8f)
            val sigAreaTop = min(preferredSigTop, (sigAreaBottom - 14f * heightScale).coerceAtLeast(curY + 6f))
            if (cfg.showSignatureImage && signatureBitmap != null && sigAreaBottom > sigAreaTop + 6f) {
                val maxAllowedW = (leftW * 0.62f).coerceAtLeast(20f)
                val maxAllowedH = (sigAreaBottom - (curY + 6f)).coerceAtLeast(8f)
                val maxSigW = min(maxAllowedW, (leftW * 0.44f) * cfg.safeSignatureScale)
                val maxSigH = min(maxAllowedH, (sigAreaBottom - sigAreaTop) * cfg.safeSignatureScale)
                val bmpScale = min(maxSigW / signatureBitmap.width, maxSigH / signatureBitmap.height)
                val drawW = (signatureBitmap.width * bmpScale).coerceAtLeast(10f)
                val drawH = (signatureBitmap.height * bmpScale).coerceAtLeast(6f)
                val halfW = drawW / 2f
                val clampedCenterX = sigAnchorX.coerceIn(
                    contentLeft + 2f + halfW,
                    (leftEndX - 2f - halfW).coerceAtLeast(contentLeft + 2f + halfW)
                )
                val dstRect = RectF(
                    clampedCenterX - halfW,
                    sigAreaBottom - drawH,
                    clampedCenterX + halfW,
                    sigAreaBottom
                )
                val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                c.drawBitmap(signatureBitmap, null, dstRect, imgPaint)
            }

            // Student details on left side: configurable gap below headline, horizontal slide offset, Word pt font size, and boldness
            val sigReserveTop = contentBottom - (28f * heightScale)
            val rawInfoStartY = curY + (cfg.safeHeaderToInfoGapPt * heightScale)
            val infoStartY = rawInfoStartY.coerceAtMost((sigReserveTop - 22f).coerceAtLeast(curY + 6f))
            val infoEndY = (contentBottom - (32f * heightScale)).coerceAtLeast(infoStartY + 22f)
            val infoStep = ((infoEndY - infoStartY) / 2f).coerceIn(10.5f, 24f)
            val baseInfoLeftX = contentLeft + (if (showRoutine) 6f else 18f)
            val maxSlideRight = if (showRoutine) (leftW * 0.48f) else (leftW * 0.65f)
            val infoLeftX = (baseInfoLeftX + (cfg.safeStudentInfoOffsetXPt * heightScale)).coerceIn(
                contentLeft + 2f,
                (leftEndX - 48f).coerceAtLeast(contentLeft + 2f).coerceAtMost(baseInfoLeftX + maxSlideRight)
            )
            val infoMaxW = (leftEndX - infoLeftX - 4f).coerceAtLeast(36f)

            val nameVal = if (item.isBlank) "............................" else item.studentName
            val clsVal = classShortBn(item.cls, settings)
            val rollVal = AdmitCardPlanner.formatRoll(item.roll, cfg)
            val labelSize = cfg.safeStudentInfoFontPt * densityScale

            drawLabelValueLine(
                ctx, c, "নাম : ", nameVal, infoLeftX, infoStartY, infoMaxW, labelSize,
                cfg.studentLabelBold, cfg.studentValueBold, bnFont, enFont
            )
            drawLabelValueLine(
                ctx, c, "শ্রেণি : ", clsVal, infoLeftX, infoStartY + infoStep, infoMaxW, labelSize,
                cfg.studentLabelBold, cfg.studentValueBold, bnFont, enFont
            )
            drawLabelValueLine(
                ctx, c, "রোল : ", rollVal, infoLeftX, infoStartY + infoStep * 2f, infoMaxW, labelSize,
                cfg.studentLabelBold, cfg.studentValueBold, bnFont, enFont
            )

            // --- RIGHT PANEL: Clean White Exam Routine Table + Optional Auto-Fit Note ---
            if (showRoutine) {
                val rLeft = leftEndX + gapW
                val rRight = contentRight
                val rW = (rRight - rLeft).coerceAtLeast(80f)
                val rTop = contentTop + (1.5f * heightScale)
                val rBottom = contentBottom - (1.5f * heightScale)
                val rH = (rBottom - rTop).coerceAtLeast(50f)

                val dataRows = page.routineRows
                val blankExtra = if (cfg.includeBlankRoutineRow) 1 else 0
                val totalDataRows = (dataRows.size + blankExtra).coerceAtLeast(1)
                val noteText = cfg.routineNote.trim()
                val noteRowH = if (noteText.isNotEmpty()) (rH * 0.145f).coerceIn(11f, 22f) else 0f

                // Allocate heights: top title + 2 sub-header rows + data rows + optional note row
                val titleRowH = (rH * 0.135f).coerceIn(11f, 20f)
                val subHdrRowH = (rH * 0.115f).coerceIn(9.5f, 16.5f)
                val subHdrTotalH = subHdrRowH * 2f
                val bodyH = (rH - titleRowH - subHdrTotalH - noteRowH).coerceAtLeast(22f)
                val rowH = bodyH / totalDataRows.toFloat()

                val colDateW = rW * 0.225f
                val colDayW = rW * 0.220f
                val colSubjW = (rW - colDateW - colDayW).coerceAtLeast(40f)

                val col1End = rLeft + colDateW
                val col2End = col1End + colDayW

                val rtHdrPt = cfg.safeRoutineHeaderFontPt * densityScale
                val rtBodyPt = cfg.safeRoutineBodyFontPt * densityScale

                // Row 1: Routine Title
                drawCenteredTableCell(
                    ctx, c, routineTitle, rLeft, rTop, rW, titleRowH,
                    rtHdrPt, cfg.routineHeaderBold, bnFont, enFont
                )

                // Row 2-3: তারিখ (2-row span), বার (2-row span), Time / বিষয়
                val hdrTopY = rTop + titleRowH
                drawCenteredTableCell(
                    ctx, c, "তারিখ", rLeft, hdrTopY, colDateW, subHdrTotalH,
                    (rtHdrPt * 0.96f), cfg.routineHeaderBold, bnFont, enFont
                )
                drawCenteredTableCell(
                    ctx, c, "বার", col1End, hdrTopY, colDayW, subHdrTotalH,
                    (rtHdrPt * 0.96f), cfg.routineHeaderBold, bnFont, enFont
                )
                val timeStr = AdmitCardPlanner.formatDigits(
                    cfg.examTimeLabel.trim().ifBlank { "দুপুর ০১:০০-০৩:৩০ টা" },
                    cfg.numeralMode
                )
                drawCenteredTableCell(
                    ctx, c, timeStr, col2End, hdrTopY, colSubjW, subHdrRowH,
                    (rtHdrPt * 0.94f), cfg.routineHeaderBold, bnFont, enFont
                )
                drawCenteredTableCell(
                    ctx, c, "বিষয়", col2End, hdrTopY + subHdrRowH, colSubjW, subHdrRowH,
                    (rtHdrPt * 0.96f), cfg.routineHeaderBold, bnFont, enFont
                )

                // Data rows
                val cellTextPt = min(rtBodyPt, (rowH * 0.72f)).coerceIn(4.5f, 18.0f)
                val bodyStartY = hdrTopY + subHdrTotalH
                for (i in 0 until totalDataRows) {
                    val ry = bodyStartY + i * rowH
                    val entry = dataRows.getOrNull(i)
                    val dateTxt = entry?.date?.let { AdmitCardPlanner.formatDigits(it, cfg.numeralMode) }.orEmpty()
                    val dayTxt = entry?.day.orEmpty()

                    drawCenteredTableCell(ctx, c, dateTxt, rLeft, ry, colDateW, rowH, cellTextPt, cfg.routineBodyBold, bnFont, enFont)
                    drawCenteredTableCell(ctx, c, dayTxt, col1End, ry, colDayW, rowH, cellTextPt, cfg.routineBodyBold, bnFont, enFont)
                    drawRoutineSubjectCell(
                        ctx, c, entry?.nonEmptySubjects ?: emptyList(),
                        col2End, ry, colSubjW, rowH, cellTextPt, cfg.routineBodyBold, bnFont, enFont
                    )
                }

                val tableDataBottom = bodyStartY + totalDataRows * rowH
                if (noteText.isNotEmpty() && noteRowH > 0f) {
                    drawAutoFitNoteCell(
                        ctx = ctx,
                        c = c,
                        text = noteText,
                        x = rLeft,
                        y = tableDataBottom,
                        w = rW,
                        h = noteRowH,
                        baseSizePt = min(rtBodyPt * 0.92f, noteRowH * 0.62f).coerceIn(5.0f, 9.5f),
                        bnFont = bnFont,
                        enFont = enFont
                    )
                }

                // Crisp outer border of routine table
                c.drawRect(rLeft, rTop, rRight, tableDataBottom + noteRowH, tableLinePaint)
            }
        }
    }

    private fun drawLabelValueLine(
        ctx: Context?,
        c: Canvas,
        label: String,
        value: String,
        x: Float,
        y: Float,
        maxW: Float,
        sizePt: Float,
        labelBold: Boolean,
        valueBold: Boolean,
        bnFont: String,
        enFont: String
    ) {
        val labelDp = PdfGen.dualPaint(ctx, sizePt, labelBold, bnFont, enFont, 0xFF000000.toInt())
        val valueDp = PdfGen.dualPaint(ctx, sizePt, valueBold, bnFont, enFont, 0xFF000000.toInt())
        var curSize = sizePt
        val minSize = 5.4f
        while (
            (PdfGen.measureMixedText(label, labelDp) + PdfGen.measureMixedText(value, valueDp)) > maxW &&
            curSize > minSize
        ) {
            curSize = (curSize - 0.2f).coerceAtLeast(minSize)
            labelDp.textSize = curSize
            valueDp.textSize = curSize
        }
        PdfGen.drawMixedText(c, label, x, y, labelDp, Paint.Align.LEFT)
        val labelW = PdfGen.measureMixedText(label, labelDp)
        PdfGen.drawMixedText(c, value, x + labelW, y, valueDp, Paint.Align.LEFT)
    }

    private fun drawRoutineSubjectCell(
        ctx: Context?,
        c: Canvas,
        subjects: List<String>,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        size: Float,
        bold: Boolean,
        bnFont: String,
        enFont: String
    ) {
        val joined = subjects.joinToString(" / ")
        if (subjects.size <= 1) {
            drawCenteredTableCell(ctx, c, joined, x, y, w, h, size, bold, bnFont, enFont)
            return
        }
        val availW = (w - 4f).coerceAtLeast(8f)
        val probeDp = PdfGen.dualPaint(ctx, size, bold, bnFont, enFont, 0xFF000000.toInt())
        PdfGen.shrinkMixedToFit(probeDp, joined, availW, step = 0.2f, minSize = 4.6f)
        if (probeDp.textSize >= 6.4f || h < 14f) {
            c.drawRect(x, y, x + w, y + h, tableLinePaint)
            val fm = probeDp.bn.fontMetrics
            val baselineY = y + h / 2f - (fm.ascent + fm.descent) / 2f
            PdfGen.drawMixedText(c, joined, x + w / 2f, baselineY, probeDp, Paint.Align.CENTER)
        } else {
            c.drawRect(x, y, x + w, y + h, tableLinePaint)
            val line1 = subjects.first()
            val line2 = subjects.drop(1).joinToString(" / ")
            val lineSize = min(size * 0.85f, (h * 0.36f)).coerceIn(4.6f, 8.2f)
            val dp1 = PdfGen.dualPaint(ctx, lineSize, bold, bnFont, enFont, 0xFF000000.toInt())
            PdfGen.shrinkMixedToFit(dp1, line1, availW, step = 0.2f, minSize = 4.4f)
            val dp2 = PdfGen.dualPaint(ctx, lineSize, bold, bnFont, enFont, 0xFF000000.toInt())
            PdfGen.shrinkMixedToFit(dp2, line2, availW, step = 0.2f, minSize = 4.4f)
            val fm1 = dp1.bn.fontMetrics
            val fm2 = dp2.bn.fontMetrics
            val y1 = y + h * 0.32f - (fm1.ascent + fm1.descent) / 2f
            val y2 = y + h * 0.72f - (fm2.ascent + fm2.descent) / 2f
            PdfGen.drawMixedText(c, line1, x + w / 2f, y1, dp1, Paint.Align.CENTER)
            PdfGen.drawMixedText(c, line2, x + w / 2f, y2, dp2, Paint.Align.CENTER)
        }
    }

    private fun drawCenteredTableCell(
        ctx: Context?,
        c: Canvas,
        text: String,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        size: Float,
        bold: Boolean,
        bnFont: String,
        enFont: String
    ) {
        c.drawRect(x, y, x + w, y + h, tableLinePaint)
        if (text.isBlank()) return
        val dp = PdfGen.dualPaint(ctx, size, bold, bnFont, enFont, 0xFF000000.toInt())
        PdfGen.shrinkMixedToFit(dp, text, (w - 4f).coerceAtLeast(8f), step = 0.2f, minSize = 4.6f)
        val fm = dp.bn.fontMetrics
        val baselineY = y + h / 2f - (fm.ascent + fm.descent) / 2f
        PdfGen.drawMixedText(c, text, x + w / 2f, baselineY, dp, Paint.Align.CENTER)
    }

    private fun drawAutoFitNoteCell(
        ctx: Context?,
        c: Canvas,
        text: String,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        baseSizePt: Float,
        bnFont: String,
        enFont: String
    ) {
        c.drawRect(x, y, x + w, y + h, tableLinePaint)
        val availW = (w - 8f).coerceAtLeast(12f)
        val probe = PdfGen.dualPaint(ctx, baseSizePt, false, bnFont, enFont, 0xFF000000.toInt())
        PdfGen.shrinkMixedToFit(probe, text, availW, step = 0.2f, minSize = 4.2f)
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (probe.textSize >= 6.0f || words.size <= 2 || h < 13f) {
            val fm = probe.bn.fontMetrics
            val baselineY = y + h / 2f - (fm.ascent + fm.descent) / 2f
            PdfGen.drawMixedText(c, text, x + w / 2f, baselineY, probe, Paint.Align.CENTER)
        } else {
            // Split cleanly near midpoint into 2 auto-fitted lines
            val totalChars = text.length
            var splitIdx = words.size / 2
            var acc = 0
            for (idx in words.indices) {
                acc += words[idx].length + 1
                if (acc >= totalChars / 2) {
                    splitIdx = (idx + 1).coerceIn(1, words.lastIndex)
                    break
                }
            }
            val line1 = words.take(splitIdx).joinToString(" ")
            val line2 = words.drop(splitIdx).joinToString(" ")
            val linePt = min(baseSizePt * 0.86f, h * 0.36f).coerceIn(4.2f, 7.8f)
            val dp1 = PdfGen.dualPaint(ctx, linePt, false, bnFont, enFont, 0xFF000000.toInt())
            PdfGen.shrinkMixedToFit(dp1, line1, availW, step = 0.2f, minSize = 4.0f)
            val dp2 = PdfGen.dualPaint(ctx, linePt, false, bnFont, enFont, 0xFF000000.toInt())
            PdfGen.shrinkMixedToFit(dp2, line2, availW, step = 0.2f, minSize = 4.0f)
            val fm1 = dp1.bn.fontMetrics
            val fm2 = dp2.bn.fontMetrics
            val y1 = y + h * 0.31f - (fm1.ascent + fm1.descent) / 2f
            val y2 = y + h * 0.72f - (fm2.ascent + fm2.descent) / 2f
            PdfGen.drawMixedText(c, line1, x + w / 2f, y1, dp1, Paint.Align.CENTER)
            PdfGen.drawMixedText(c, line2, x + w / 2f, y2, dp2, Paint.Align.CENTER)
        }
    }
}
