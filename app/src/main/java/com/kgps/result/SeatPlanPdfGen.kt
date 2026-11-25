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

    private val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF111111.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 0.85f
    }

    private val cutGuidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF333333.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 0.65f
        pathEffect = DashPathEffect(floatArrayOf(3.2f, 2.6f), 0f)
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
        val marginX = 18f
        val marginY = 18f
        val gridLeft = marginX
        val gridTop = marginY
        val gridRight = w - marginX
        val gridBottom = h - marginY
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
                c.drawLine(x, gridTop - 4f, x, gridBottom + 4f, cutGuidePaint)
            }
            for (row in 1 until rows) {
                val y = gridTop + row * cellH
                c.drawLine(gridLeft - 4f, y, gridRight + 4f, y, cutGuidePaint)
            }
        }

        val schoolName = SeatPlanPlanner.resolvedSchoolName(cfg, settings)
        val schoolAddress = SeatPlanPlanner.resolvedSchoolAddress(cfg, settings)
        val examTitle = SeatPlanPlanner.resolvedExamTitle(cfg, settings)
        val subtitle = SeatPlanPlanner.resolvedSubtitle(cfg)

        val bnFont = settings.pdfFont
        val enFont = settings.pdfEnglishFont

        val insetX = if (cols >= 3) 4.5f else 5.5f
        val insetY = if (rows >= 7) 4.0f else 5.0f

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
                val radius = if (cfg.roundedCorners) 9f else 0f
                c.drawRoundRect(cardRect, radius, radius, cardBorderPaint)

                val cardH = cardRect.height()
                val heightScale = (cardH / 124f).coerceIn(0.68f, 1.35f)
                val widthScale = (cardRect.width() / 268f).coerceIn(0.72f, 1.3f)
                val fontFactor = min(heightScale, widthScale).coerceIn(0.72f, 1.18f) * cfg.fontScale.factor

                val padX = (14f * widthScale).coerceIn(8f, 22f)
                val innerLeft = cardRect.left + padX
                val innerRight = cardRect.right - padX
                val innerW = (innerRight - innerLeft).coerceAtLeast(40f)
                val centerX = cardRect.centerX()

                // Header block
                val headerLines = buildList {
                    if (schoolName.isNotBlank()) add(schoolName to 10.2f)
                    if (schoolAddress.isNotBlank()) add(schoolAddress to 9.4f)
                    if (examTitle.isNotBlank()) add(examTitle to 9.4f)
                    if (subtitle.isNotBlank()) add(subtitle to 9.4f)
                }
                val lineStep = (13.0f * heightScale).coerceIn(9.5f, 18f)
                var curY = cardRect.top + (19f * heightScale).coerceIn(13f, 28f)
                headerLines.forEachIndexed { lineIdx, (text, baseSize) ->
                    val dp = PdfGen.dualPaint(
                        ctx = ctx,
                        size = baseSize * fontFactor,
                        bold = (lineIdx == 0 && cfg.boldStudentName),
                        bnFontId = bnFont,
                        enFontId = enFont,
                        textColor = 0xFF000000.toInt()
                    )
                    PdfGen.shrinkMixedToFit(dp, text, innerW, step = 0.25f, minSize = 5.5f)
                    PdfGen.drawMixedText(c, text, centerX, curY, dp, Paint.Align.CENTER)
                    curY += lineStep
                }

                // Footer student lines
                val bottomY = cardRect.bottom - (10.5f * heightScale).coerceIn(7f, 16f)
                val nameGap = (14.5f * heightScale).coerceIn(10.5f, 22f)
                val nameY = bottomY - nameGap

                val classLabel = "শ্রেণি: ${classShortBn(slip.cls, settings)}"
                val rollLabel = "রোল: ${SeatPlanPlanner.formatRoll(slip.roll, cfg)}"

                val metaPaintLeft = PdfGen.dualPaint(
                    ctx = ctx,
                    size = 9.5f * fontFactor,
                    bold = false,
                    bnFontId = bnFont,
                    enFontId = enFont,
                    textColor = 0xFF000000.toInt()
                )
                val metaPaintRight = PdfGen.dualPaint(
                    ctx = ctx,
                    size = 9.5f * fontFactor,
                    bold = false,
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
                    size = 10.0f * fontFactor,
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
