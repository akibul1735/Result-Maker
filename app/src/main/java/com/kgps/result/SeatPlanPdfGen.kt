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

object SeatPlanPdfGen {
    private const val A4_SHORT = 595
    private const val A4_LONG = 842

    fun pageDimensions(orientation: SeatPlanOrientation): Pair<Int, Int> = when (orientation) {
        SeatPlanOrientation.PORTRAIT -> A4_SHORT to A4_LONG
        SeatPlanOrientation.LANDSCAPE -> A4_LONG to A4_SHORT
    }

    private fun makeBorderPaint(style: SeatPlanBorderStyle, strokeWidth: Float): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF000000.toInt()
            this.style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            if (style == SeatPlanBorderStyle.DASHED) {
                pathEffect = DashPathEffect(floatArrayOf(4.5f, 3.0f), 0f)
            }
        }

    private val cutGuidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1A1A1A.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 0.75f
        pathEffect = DashPathEffect(floatArrayOf(3.5f, 3.0f), 0f)
    }

    fun renderPageBitmap(
        ctx: Context,
        page: SeatPlanPageSpec,
        settings: Settings,
        scale: Float = 2.0f
    ): Bitmap {
        val (w, h) = pageDimensions(page.config.orientation)
        val bmp = Bitmap.createBitmap((w * scale).toInt(), (h * scale).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        canvas.scale(scale, scale)
        drawPage(ctx, canvas, page, settings)
        return bmp
    }

    fun write(
        ctx: Context,
        pages: List<SeatPlanPageSpec>,
        settings: Settings,
        out: OutputStream
    ) {
        if (pages.isEmpty()) return
        val doc = PdfDocument()
        try {
            pages.forEachIndexed { idx, page ->
                val (w, h) = pageDimensions(page.config.orientation)
                val pdfPage = doc.startPage(PdfDocument.PageInfo.Builder(w, h, idx + 1).create())
                pdfPage.canvas.drawColor(Color.WHITE)
                drawPage(ctx, pdfPage.canvas, page, settings)
                doc.finishPage(pdfPage)
            }
            doc.writeTo(out)
        } finally {
            doc.close()
        }
    }

    fun print(
        ctx: Context,
        pages: List<SeatPlanPageSpec>,
        settings: Settings,
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
                        write(ctx, pages, settings, out)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }
        val mediaSize = if (pages.first().config.orientation == SeatPlanOrientation.LANDSCAPE) {
            PrintAttributes.MediaSize.ISO_A4.asLandscape()
        } else {
            PrintAttributes.MediaSize.ISO_A4.asPortrait()
        }
        printManager.print(
            jobName,
            adapter,
            PrintAttributes.Builder().setMediaSize(mediaSize).build()
        )
    }

    internal fun drawPage(
        ctx: Context?,
        c: Canvas,
        page: SeatPlanPageSpec,
        settings: Settings
    ) {
        val cfg = page.config
        val (w, h) = pageDimensions(cfg.orientation)
        val marginPt = cfg.safeMarginPt
        val gridLeft = marginPt
        val gridTop = marginPt
        val gridRight = (w - marginPt).coerceAtLeast(gridLeft + 80f)
        val gridBottom = (h - marginPt).coerceAtLeast(gridTop + 80f)
        val gridW = gridRight - gridLeft
        val gridH = gridBottom - gridTop

        val cols = cfg.safeColumns
        val rows = cfg.safeRows
        val cellW = gridW / cols
        val cellH = gridH / rows

        // Dashed cutting lines cleanly centered between cards
        if (cfg.showCutLines) {
            for (col in 1 until cols) {
                val x = gridLeft + col * cellW
                c.drawLine(x, gridTop - 3f, x, gridBottom + 3f, cutGuidePaint)
            }
            for (row in 1 until rows) {
                val y = gridTop + row * cellH
                c.drawLine(gridLeft - 3f, y, gridRight + 3f, y, cutGuidePaint)
            }
        }

        val schoolName = SeatPlanPlanner.resolvedSchoolName(cfg, settings)
        val schoolAddress = SeatPlanPlanner.resolvedSchoolAddress(cfg, settings)
        val examTitle = SeatPlanPlanner.resolvedExamTitle(cfg, settings)
        val subtitle = SeatPlanPlanner.resolvedSubtitle(cfg)

        val bnFont = cfg.bnFontId.ifBlank { settings.pdfFont }
        val enFont = cfg.enFontId.ifBlank { settings.pdfEnglishFont }

        val insetX = (cfg.safeHorizontalGapPt / 2f).coerceAtMost(cellW * 0.28f)
        val insetY = (cfg.safeVerticalGapPt / 2f).coerceAtMost(cellH * 0.28f)

        val borderPaint = makeBorderPaint(cfg.borderStyle, cfg.borderWeight.strokePt)
        val innerDoublePaint = if (cfg.borderStyle == SeatPlanBorderStyle.DOUBLE) {
            makeBorderPaint(SeatPlanBorderStyle.SOLID, (cfg.borderWeight.strokePt * 0.7f).coerceAtLeast(0.5f))
        } else {
            null
        }

        for (r in 0 until rows) {
            for (col in 0 until cols) {
                val idx = r * cols + col
                val slip = page.slips.getOrNull(idx) ?: continue

                val cellLeft = gridLeft + col * cellW
                val cellTop = gridTop + r * cellH
                val cardRect = RectF(
                    cellLeft + insetX,
                    cellTop + insetY,
                    cellLeft + cellW - insetX,
                    cellTop + cellH - insetY
                )

                val cardH = cardRect.height().coerceAtLeast(36f)
                val cardW = cardRect.width().coerceAtLeast(60f)
                val heightScale = (cardH / 120f).coerceIn(0.62f, 1.35f)
                val widthScale = (cardW / 256f).coerceIn(0.65f, 1.35f)
                val cellScale = min(heightScale, widthScale).coerceIn(0.66f, 1.20f)

                val maxRadius = min(cardW, cardH) * 0.42f
                val radius = (cfg.safeCornerRadiusPt * min(heightScale, widthScale)).coerceIn(0f, maxRadius)

                if (cfg.borderStyle != SeatPlanBorderStyle.NONE) {
                    c.drawRoundRect(cardRect, radius, radius, borderPaint)
                    if (innerDoublePaint != null) {
                        val gap = (cfg.borderWeight.strokePt + 2.2f).coerceAtMost(min(cardW, cardH) * 0.12f)
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

                val padX = (14.5f * widthScale).coerceIn(7f, 22f)
                val innerLeft = cardRect.left + padX
                val innerRight = cardRect.right - padX
                val innerW = (innerRight - innerLeft).coerceAtLeast(30f)
                val centerX = cardRect.centerX()

                // Header block with per-part size & bold control
                data class HeaderEntry(val text: String, val sizePt: Float, val isBold: Boolean)
                val headerLines = buildList {
                    if (schoolName.isNotBlank()) add(HeaderEntry(schoolName, cfg.safeSchoolFontPt, cfg.boldSchoolName))
                    if (schoolAddress.isNotBlank()) add(HeaderEntry(schoolAddress, cfg.safeHeaderMetaFontPt, cfg.boldAddress))
                    if (examTitle.isNotBlank()) add(HeaderEntry(examTitle, cfg.safeHeaderMetaFontPt, cfg.boldExamTitle))
                    if (subtitle.isNotBlank()) add(HeaderEntry(subtitle, cfg.safeHeaderMetaFontPt, cfg.boldExamTitle))
                }
                val maxHeaderPt = headerLines.maxOfOrNull { it.sizePt } ?: 11f
                val lineStep = ((maxHeaderPt * 1.15f).coerceAtLeast(11.5f) * heightScale).coerceIn(9.0f, 22f)
                var curY = cardRect.top + ((cfg.safeSchoolFontPt * 1.62f).coerceAtLeast(14f) * heightScale).coerceIn(12f, 30f)
                headerLines.forEach { entry ->
                    val dp = PdfGen.dualPaint(
                        ctx = ctx,
                        size = entry.sizePt * cellScale,
                        bold = entry.isBold,
                        bnFontId = bnFont,
                        enFontId = enFont,
                        textColor = 0xFF000000.toInt()
                    )
                    PdfGen.shrinkMixedToFit(dp, entry.text, innerW, step = 0.25f, minSize = 5.5f)
                    PdfGen.drawMixedText(c, entry.text, centerX, curY, dp, Paint.Align.CENTER)
                    curY += lineStep
                }

                // Footer student lines with per-part size & bold control
                val bottomY = cardRect.bottom - (11.5f * heightScale).coerceIn(6.5f, 18f)
                val nameGap = ((maxOf(cfg.safeStudentNameFontPt, cfg.safeClassRollFontPt) * 1.38f).coerceAtLeast(13.5f) * heightScale).coerceIn(10.0f, 26f)
                val nameY = bottomY - nameGap

                val classLabel = "শ্রেণি: ${classShortBn(slip.cls, settings)}"
                val rollLabel = "রোল: ${SeatPlanPlanner.formatRoll(slip.roll, cfg)}"

                val metaPaintLeft = PdfGen.dualPaint(
                    ctx = ctx,
                    size = cfg.safeClassRollFontPt * cellScale,
                    bold = cfg.boldClassRoll,
                    bnFontId = bnFont,
                    enFontId = enFont,
                    textColor = 0xFF000000.toInt()
                )
                val metaPaintRight = PdfGen.dualPaint(
                    ctx = ctx,
                    size = cfg.safeClassRollFontPt * cellScale,
                    bold = cfg.boldClassRoll,
                    bnFontId = bnFont,
                    enFontId = enFont,
                    textColor = 0xFF000000.toInt()
                )
                PdfGen.shrinkMixedToFit(metaPaintLeft, classLabel, innerW * 0.55f, step = 0.2f, minSize = 5.5f)
                PdfGen.shrinkMixedToFit(metaPaintRight, rollLabel, innerW * 0.42f, step = 0.2f, minSize = 5.5f)

                PdfGen.drawMixedText(c, classLabel, innerLeft, bottomY, metaPaintLeft, Paint.Align.LEFT)
                PdfGen.drawMixedText(c, rollLabel, innerRight, bottomY, metaPaintRight, Paint.Align.RIGHT)

                val studentDisplay = if (slip.isBlank) "............................" else slip.studentName
                val nameText = "নাম: $studentDisplay"
                val namePaint = PdfGen.dualPaint(
                    ctx = ctx,
                    size = cfg.safeStudentNameFontPt * cellScale,
                    bold = cfg.boldStudentName,
                    bnFontId = bnFont,
                    enFontId = enFont,
                    textColor = 0xFF000000.toInt()
                )
                PdfGen.shrinkMixedToFit(namePaint, nameText, innerW, step = 0.25f, minSize = 5.5f)
                PdfGen.drawMixedText(c, nameText, innerLeft, nameY, namePaint, Paint.Align.LEFT)
            }
        }
    }
}
