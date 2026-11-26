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

    private val centerDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1A1A1A.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 0.7f
        pathEffect = DashPathEffect(floatArrayOf(3.0f, 2.6f), 0f)
    }

    private val cutGuidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF000000.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 0.9f
        pathEffect = DashPathEffect(floatArrayOf(4.2f, 3.2f), 0f)
    }

    private fun makeBorderPaint(style: SeatPlanBorderStyle, strokeWidth: Float): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF000000.toInt()
            this.style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            if (style == SeatPlanBorderStyle.DASHED) {
                pathEffect = DashPathEffect(floatArrayOf(4.0f, 3.0f), 0f)
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

        // Full-width horizontal dashed cut lines between stacked cards
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

        val insetY = (cfg.safeVerticalGapPt / 2f).coerceAtMost(cellH * 0.22f)
        val borderPaint = makeBorderPaint(cfg.borderStyle, cfg.borderWeight.strokePt)
        val innerDoublePaint = if (cfg.borderStyle == SeatPlanBorderStyle.DOUBLE) {
            makeBorderPaint(SeatPlanBorderStyle.SOLID, (cfg.borderWeight.strokePt * 0.7f).coerceAtLeast(0.5f))
        } else {
            null
        }

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

            val maxRadius = min(cardW, cardH) * 0.25f
            val radius = (cfg.safeCornerRadiusPt * heightScale).coerceIn(0f, maxRadius)

            if (cfg.borderStyle != SeatPlanBorderStyle.NONE) {
                c.drawRoundRect(cardRect, radius, radius, borderPaint)
                if (innerDoublePaint != null) {
                    val gap = (cfg.borderWeight.strokePt + 2.0f).coerceAtMost(min(cardW, cardH) * 0.08f)
                    val innerRect = RectF(
                        cardRect.left + gap,
                        cardRect.top + gap,
                        cardRect.right - gap,
                        cardRect.bottom - gap
                    )
                    val innerRadius = (radius - gap * 0.7f).coerceAtLeast(0f)
                    c.drawRoundRect(innerRect, innerRadius, innerRadius, innerDoublePaint)
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

            // --- LEFT PANEL: Regular School Header, Bold-Underlined প্রবেশপত্র, Student Info, Head Teacher Signature ---
            var curY = contentTop + (9.5f * heightScale).coerceIn(6.5f, 14f)
            val headerStep = (11.6f * heightScale).coerceIn(8.2f, 16.5f)

            if (schoolName.isNotBlank()) {
                val dp = PdfGen.dualPaint(ctx, 9.8f * heightScale, false, bnFont, enFont, 0xFF000000.toInt())
                PdfGen.shrinkMixedToFit(dp, schoolName, leftW - 4f, step = 0.25f, minSize = 5.8f)
                PdfGen.drawMixedText(c, schoolName, leftCenterX, curY, dp, Paint.Align.CENTER)
                curY += headerStep
            }
            if (schoolAddress.isNotBlank()) {
                val dp = PdfGen.dualPaint(ctx, 9.2f * heightScale, false, bnFont, enFont, 0xFF000000.toInt())
                PdfGen.shrinkMixedToFit(dp, schoolAddress, leftW - 4f, step = 0.25f, minSize = 5.5f)
                PdfGen.drawMixedText(c, schoolAddress, leftCenterX, curY, dp, Paint.Align.CENTER)
                curY += headerStep + (1.2f * heightScale)
            }
            if (examTitle.isNotBlank()) {
                val dp = PdfGen.dualPaint(ctx, 9.2f * heightScale, false, bnFont, enFont, 0xFF000000.toInt())
                PdfGen.shrinkMixedToFit(dp, examTitle, leftW - 4f, step = 0.25f, minSize = 5.5f)
                PdfGen.drawMixedText(c, examTitle, leftCenterX, curY, dp, Paint.Align.CENTER)
                curY += headerStep + (2.5f * heightScale)
            }

            // Bold underlined "প্রবেশপত্র"
            val badgeDp = PdfGen.dualPaint(ctx, 9.8f * heightScale, true, bnFont, enFont, 0xFF000000.toInt()).apply {
                bn.isUnderlineText = true
                en.isUnderlineText = true
            }
            PdfGen.shrinkMixedToFit(badgeDp, badgeText, leftW - 8f, step = 0.2f, minSize = 6.2f)
            PdfGen.drawMixedText(c, badgeText, leftCenterX, curY, badgeDp, Paint.Align.CENTER)

            // Reserve signature area at bottom-right of Left Panel (clean blank space when no image)
            val sigLabelText = cfg.headTeacherTitle.trim().ifBlank { "প্রধান শিক্ষকের স্বাক্ষর" }
            val sigLabelDp = PdfGen.dualPaint(ctx, 8.6f * heightScale, false, bnFont, enFont, 0xFF000000.toInt())
            PdfGen.shrinkMixedToFit(sigLabelDp, sigLabelText, leftW * 0.56f, step = 0.2f, minSize = 5.5f)
            val sigLabelY = contentBottom - (1.5f * heightScale)
            val sigAnchorX = leftEndX - (leftW * 0.26f)
            PdfGen.drawMixedText(c, sigLabelText, sigAnchorX, sigLabelY, sigLabelDp, Paint.Align.CENTER)

            val sigAreaTop = (contentBottom - (38f * heightScale)).coerceAtLeast(curY + 8f)
            val sigAreaBottom = sigLabelY - (6.5f * heightScale)
            if (cfg.showSignatureImage && signatureBitmap != null && sigAreaBottom > sigAreaTop + 6f) {
                val maxSigW = (leftW * 0.44f) * cfg.safeSignatureScale
                val maxSigH = (sigAreaBottom - sigAreaTop) * cfg.safeSignatureScale
                val bmpScale = min(maxSigW / signatureBitmap.width, maxSigH / signatureBitmap.height)
                val drawW = (signatureBitmap.width * bmpScale).coerceAtLeast(10f)
                val drawH = (signatureBitmap.height * bmpScale).coerceAtLeast(6f)
                val dstRect = RectF(
                    sigAnchorX - drawW / 2f,
                    sigAreaBottom - drawH,
                    sigAnchorX + drawW / 2f,
                    sigAreaBottom
                )
                val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                c.drawBitmap(signatureBitmap, null, dstRect, imgPaint)
            }

            // Student details on left side: bold label prefix + regular value, comfortably spaced above bottom
            val infoStartY = curY + (15.5f * heightScale).coerceIn(10f, 22f)
            val infoEndY = (contentBottom - (34f * heightScale)).coerceAtLeast(infoStartY + 26f)
            val infoStep = ((infoEndY - infoStartY) / 2f).coerceIn(12f, 20f)
            val infoLeftX = contentLeft + (if (showRoutine) 6f else 18f)
            val infoMaxW = if (showRoutine) (leftW - 10f) else (leftW - 36f)

            val nameVal = if (item.isBlank) "............................" else item.studentName
            val clsVal = classShortBn(item.cls, settings)
            val rollVal = AdmitCardPlanner.formatRoll(item.roll, cfg)
            val labelSize = 9.5f * heightScale

            drawLabelValueLine(
                ctx, c, "নাম : ", nameVal, infoLeftX, infoStartY, infoMaxW, labelSize, bnFont, enFont
            )
            drawLabelValueLine(
                ctx, c, "শ্রেণি : ", clsVal, infoLeftX, infoStartY + infoStep, infoMaxW * 0.58f, labelSize, bnFont, enFont
            )
            drawLabelValueLine(
                ctx, c, "রোল : ", rollVal, infoLeftX, infoStartY + infoStep * 2f, infoMaxW * 0.58f, labelSize, bnFont, enFont
            )

            // --- RIGHT PANEL: Clean White Exam Routine Table ---
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

                // Allocate heights: top title + 2 sub-header rows + data rows
                val titleRowH = (rH * 0.138f).coerceIn(11f, 21f)
                val subHdrRowH = (rH * 0.118f).coerceIn(9.5f, 17f)
                val subHdrTotalH = subHdrRowH * 2f
                val bodyH = (rH - titleRowH - subHdrTotalH).coerceAtLeast(24f)
                val rowH = bodyH / totalDataRows.toFloat()

                val colDateW = rW * 0.225f
                val colDayW = rW * 0.220f
                val colSubjW = (rW - colDateW - colDayW).coerceAtLeast(40f)

                val col1End = rLeft + colDateW
                val col2End = col1End + colDayW

                // Row 1: Routine Title
                drawCenteredTableCell(
                    ctx, c, routineTitle, rLeft, rTop, rW, titleRowH,
                    8.5f * heightScale, false, bnFont, enFont
                )

                // Row 2-3: তারিখ (2-row span), বার (2-row span), Time / বিষয়
                val hdrTopY = rTop + titleRowH
                drawCenteredTableCell(
                    ctx, c, "তারিখ", rLeft, hdrTopY, colDateW, subHdrTotalH,
                    8.3f * heightScale, false, bnFont, enFont
                )
                drawCenteredTableCell(
                    ctx, c, "বার", col1End, hdrTopY, colDayW, subHdrTotalH,
                    8.3f * heightScale, false, bnFont, enFont
                )
                val timeStr = AdmitCardPlanner.formatDigits(
                    cfg.examTimeLabel.trim().ifBlank { "দুপুর ০১:০০-০৩:৩০ টা" },
                    cfg.numeralMode
                )
                drawCenteredTableCell(
                    ctx, c, timeStr, col2End, hdrTopY, colSubjW, subHdrRowH,
                    8.0f * heightScale, false, bnFont, enFont
                )
                drawCenteredTableCell(
                    ctx, c, "বিষয়", col2End, hdrTopY + subHdrRowH, colSubjW, subHdrRowH,
                    8.2f * heightScale, false, bnFont, enFont
                )

                // Data rows
                val cellTextPt = min(8.3f * heightScale, (rowH * 0.65f)).coerceIn(5.4f, 10.5f)
                val bodyStartY = hdrTopY + subHdrTotalH
                for (i in 0 until totalDataRows) {
                    val ry = bodyStartY + i * rowH
                    val entry = dataRows.getOrNull(i)
                    val dateTxt = entry?.date?.let { AdmitCardPlanner.formatDigits(it, cfg.numeralMode) }.orEmpty()
                    val dayTxt = entry?.day.orEmpty()
                    val subjTxt = entry?.subject.orEmpty()

                    drawCenteredTableCell(ctx, c, dateTxt, rLeft, ry, colDateW, rowH, cellTextPt, false, bnFont, enFont)
                    drawCenteredTableCell(ctx, c, dayTxt, col1End, ry, colDayW, rowH, cellTextPt, false, bnFont, enFont)
                    drawCenteredTableCell(ctx, c, subjTxt, col2End, ry, colSubjW, rowH, cellTextPt, false, bnFont, enFont)
                }

                // Crisp outer border of routine table
                c.drawRect(rLeft, rTop, rRight, bodyStartY + totalDataRows * rowH, tableLinePaint)
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
        bnFont: String,
        enFont: String
    ) {
        val labelDp = PdfGen.dualPaint(ctx, sizePt, true, bnFont, enFont, 0xFF000000.toInt())
        val valueDp = PdfGen.dualPaint(ctx, sizePt, false, bnFont, enFont, 0xFF000000.toInt())
        var curSize = sizePt
        val minSize = 5.6f
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
}
