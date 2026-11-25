package com.kgps.result

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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

object TabulationPdfGen {

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF334155.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 0.65f
    }

    private val strongBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0F172A.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 0.95f
    }

    private val headerBgPaint = Paint().apply {
        color = 0xFFE2E8F0.toInt()
        style = Paint.Style.FILL
    }

    private val subHeaderBgPaint = Paint().apply {
        color = 0xFFF1F5F9.toInt()
        style = Paint.Style.FILL
    }

    private val zebraBgPaint = Paint().apply {
        color = 0xFFF8FAFC.toInt()
        style = Paint.Style.FILL
    }

    private val summaryBgPaint = Paint().apply {
        color = 0xFFCCFBF1.toInt()
        style = Paint.Style.FILL
    }

    private const val INK_DARK = 0xFF0F172A.toInt()
    private const val INK_MUTED = 0xFF475569.toInt()
    private const val INK_FAIL = 0xFFDC2626.toInt()

    fun renderPageBitmap(
        ctx: Context,
        page: TabulationPageSpec,
        settings: Settings,
        scale: Float = 2.0f
    ): Bitmap {
        val w = page.config.pageSize.widthPt
        val h = page.config.pageSize.heightPt
        val bmp = Bitmap.createBitmap((w * scale).toInt(), (h * scale).toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        c.scale(scale, scale)
        drawPage(ctx, c, page, settings)
        return bmp
    }

    fun write(
        ctx: Context,
        pages: List<TabulationPageSpec>,
        settings: Settings,
        out: OutputStream
    ) {
        if (pages.isEmpty()) return
        val doc = PdfDocument()
        try {
            pages.forEachIndexed { idx, page ->
                val w = page.config.pageSize.widthPt
                val h = page.config.pageSize.heightPt
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
        pages: List<TabulationPageSpec>,
        settings: Settings,
        jobName: String
    ) {
        if (pages.isEmpty()) return
        val printManager = ctx.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val firstSize = pages.first().config.pageSize
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
        val mediaSize = when (firstSize) {
            TabulationPageSize.A4_LANDSCAPE -> PrintAttributes.MediaSize.ISO_A4.asLandscape()
            TabulationPageSize.LEGAL_LANDSCAPE -> PrintAttributes.MediaSize.NA_LEGAL.asLandscape()
            TabulationPageSize.A4_PORTRAIT -> PrintAttributes.MediaSize.ISO_A4.asPortrait()
        }
        printManager.print(
            jobName,
            adapter,
            PrintAttributes.Builder().setMediaSize(mediaSize).build()
        )
    }

    private data class SummaryColSpec(
        val id: String,
        val headerBn: String,
        val width: Float
    )

    internal fun drawPage(
        ctx: Context?,
        c: Canvas,
        page: TabulationPageSpec,
        settings: Settings
    ) {
        val cfg = page.config
        val w = cfg.pageSize.widthPt.toFloat()
        val h = cfg.pageSize.heightPt.toFloat()
        val margin = cfg.safeMarginPt
        val left = margin
        val right = (w - margin).coerceAtLeast(left + 200f)
        val top = margin
        val bottom = (h - margin).coerceAtLeast(top + 200f)
        val tableW = right - left

        val bnFont = cfg.bnFontId.ifBlank { settings.pdfFont }
        val enFont = cfg.enFontId.ifBlank { settings.pdfEnglishFont }

        // 1. Page Header
        val schoolTitle = settings.school.ifBlank { "প্রাথমিক বিদ্যালয়" } +
            settings.schoolAddress.trim().takeIf { it.isNotEmpty() }?.let { " — $it" }.orEmpty()
        val schoolDp = PdfGen.dualPaint(ctx, 11.2f, true, bnFont, enFont, INK_DARK)
        PdfGen.shrinkMixedToFit(schoolDp, schoolTitle, tableW, step = 0.25f, minSize = 7.5f)
        PdfGen.drawMixedText(c, schoolTitle, left + tableW / 2f, top + 12f, schoolDp, Paint.Align.CENTER)

        val pageLabel = "পৃষ্ঠা ${TabulationPlanner.formatDigits(page.pageNumber.toString(), cfg.numeralMode)}/${TabulationPlanner.formatDigits(page.totalPages.toString(), cfg.numeralMode)}"
        val subHeaderLine = "${page.classNameBn} · ${page.scopeTitleBn}   ($pageLabel)"
        val subDp = PdfGen.dualPaint(ctx, 9.2f, false, bnFont, enFont, INK_MUTED)
        PdfGen.shrinkMixedToFit(subDp, subHeaderLine, tableW, step = 0.2f, minSize = 6.5f)
        PdfGen.drawMixedText(c, subHeaderLine, left + tableW / 2f, top + 25f, subDp, Paint.Align.CENTER)

        val tableTop = top + 33f
        val isLastPage = page.pageNumber == page.totalPages
        val footerReserve = if (cfg.showSummaryFooter && isLastPage) 24f else 4f
        val tableBottom = (bottom - footerReserve).coerceAtLeast(tableTop + 100f)

        // 2. Column Width Distribution
        val summaryCols = buildList {
            if (cfg.showTotal) add(SummaryColSpec("TOTAL", "মোট", if (cfg.pageSize == TabulationPageSize.LEGAL_LANDSCAPE) 34f else 28f))
            if (cfg.showHighest) add(SummaryColSpec("HIGH", "সর্বোচ্চ", if (cfg.pageSize == TabulationPageSize.LEGAL_LANDSCAPE) 34f else 28f))
            if (cfg.showPercentage) add(SummaryColSpec("PCT", "শতকরা\n(%)", if (cfg.pageSize == TabulationPageSize.LEGAL_LANDSCAPE) 34f else 28f))
            if (cfg.showGrade) add(SummaryColSpec("GRADE", "গ্রেড", if (cfg.pageSize == TabulationPageSize.LEGAL_LANDSCAPE) 28f else 23f))
            if (cfg.showMeritRank) add(SummaryColSpec("RANK", "মেধা\nক্রম", if (cfg.pageSize == TabulationPageSize.LEGAL_LANDSCAPE) 30f else 25f))
        }

        val totalSubCols = page.subjectGroups.sumOf { it.subCols.size }.coerceAtLeast(1)
        val rollW = when (cfg.pageSize) {
            TabulationPageSize.LEGAL_LANDSCAPE -> 26f
            TabulationPageSize.A4_LANDSCAPE -> 22f
            TabulationPageSize.A4_PORTRAIT -> 20f
        }
        val nameW = when {
            cfg.pageSize == TabulationPageSize.LEGAL_LANDSCAPE -> 108f
            cfg.pageSize == TabulationPageSize.A4_PORTRAIT && totalSubCols <= 10 -> 105f
            totalSubCols <= 10 -> 125f
            else -> 82f
        }
        val summaryTotalW = summaryCols.sumOf { it.width.toDouble() }.toFloat()
        val rawSubjAreaW = (tableW - rollW - nameW - summaryTotalW).coerceAtLeast(100f)
        val subColW = rawSubjAreaW / totalSubCols.toFloat()

        val isTwoTierHeader = page.subjectGroups.any { it.subCols.size > 1 }
        val hdr1H = 18f
        val hdr2H = if (!isTwoTierHeader) 0f else if (cfg.headerStyle == TabulationHeaderStyle.VERTICAL_SUB) 36f else 17f
        val totalHdrH = hdr1H + hdr2H

        val rowsAvailH = (tableBottom - tableTop - totalHdrH).coerceAtLeast(40f)
        val rowCount = page.rows.size.coerceAtLeast(1)
        val rowH = (rowsAvailH / rowCount.toFloat()).coerceIn(13.5f, 22f)

        // 3. Draw Table Headers
        c.drawRect(left, tableTop, right, tableTop + hdr1H, headerBgPaint)
        if (hdr2H > 0f) {
            c.drawRect(left, tableTop + hdr1H, right, tableTop + totalHdrH, subHeaderBgPaint)
        } else {
            c.drawRect(left, tableTop, right, tableTop + totalHdrH, headerBgPaint)
        }

        var curX = left
        // রোল
        drawCenteredCell(ctx, c, "রোল", curX, tableTop, rollW, totalHdrH, 7.5f, true, bnFont, enFont, INK_DARK)
        curX += rollW

        // নাম
        drawCenteredCell(ctx, c, "শিক্ষার্থীর নাম", curX, tableTop, nameW, totalHdrH, 8.0f, true, bnFont, enFont, INK_DARK)
        curX += nameW

        // Subject groups + sub-columns
        page.subjectGroups.forEach { grp ->
            val grpW = subColW * grp.subCols.size
            val maxStr = TabulationPlanner.formatDigits(grp.maxMarks.toString(), cfg.numeralMode)
            if (isTwoTierHeader) {
                val subjHeaderText = if (grp.subCols.size == 1 && subColW < 34f) {
                    grp.shortTitleBn
                } else {
                    "${grp.shortTitleBn} ($maxStr)"
                }
                drawCenteredCell(ctx, c, subjHeaderText, curX, tableTop, grpW, hdr1H, 7.2f, true, bnFont, enFont, INK_DARK)
                var subX = curX
                grp.subCols.forEach { subCol ->
                    if (cfg.headerStyle == TabulationHeaderStyle.VERTICAL_SUB) {
                        drawVerticalHeaderCell(
                            ctx, c, subCol.fullBn, subX, tableTop + hdr1H, subColW, hdr2H, 6.8f, bnFont, enFont
                        )
                    } else {
                        drawCenteredCell(
                            ctx, c, subCol.shortBn, subX, tableTop + hdr1H, subColW, hdr2H, 6.6f, true, bnFont, enFont, INK_DARK
                        )
                    }
                    subX += subColW
                }
            } else {
                val multilineSubj = "${grp.shortTitleBn}\n($maxStr)"
                drawMultilineCenteredCell(ctx, c, multilineSubj, curX, tableTop, grpW, totalHdrH, 7.2f, true, bnFont, enFont, INK_DARK)
            }
            curX += grpW
        }

        // Right Summary Headers
        summaryCols.forEach { sc ->
            drawMultilineCenteredCell(ctx, c, sc.headerBn, curX, tableTop, sc.width, totalHdrH, 7.0f, true, bnFont, enFont, INK_DARK)
            curX += sc.width
        }

        c.drawRect(left, tableTop, right, tableTop + totalHdrH, strongBorderPaint)

        // 4. Draw Student Rows
        val dataFontBase = when {
            subColW >= 30f -> 8.0f
            subColW >= 24f -> 7.3f
            else -> 6.7f
        }

        page.rows.forEachIndexed { rIdx, row ->
            val ry = tableTop + totalHdrH + rIdx * rowH
            if (rIdx % 2 == 1) {
                c.drawRect(left, ry, right, ry + rowH, zebraBgPaint)
            }

            var x = left
            // Roll
            drawCenteredCell(ctx, c, row.rollText, x, ry, rollW, rowH, dataFontBase, true, bnFont, enFont, INK_DARK)
            x += rollW

            // Name (left aligned with gentle shrink)
            drawLeftCell(ctx, c, row.student.name, x, ry, nameW, rowH, dataFontBase + 0.3f, false, bnFont, enFont, INK_DARK)
            x += nameW

            // Subject marks
            row.subjectCells.forEach { cells ->
                cells.forEach { cell ->
                    val ink = when {
                        cell.isAbsent || cell.isFailed -> INK_FAIL
                        else -> INK_DARK
                    }
                    drawCenteredCell(
                        ctx, c, cell.text, x, ry, subColW, rowH,
                        dataFontBase, cell.isBold, bnFont, enFont, ink
                    )
                    x += subColW
                }
            }

            // Summary cells
            summaryCols.forEach { sc ->
                val txt = when (sc.id) {
                    "TOTAL" -> row.totalText
                    "HIGH" -> row.highestText
                    "PCT" -> row.pctText
                    "GRADE" -> row.gradeText
                    "RANK" -> row.meritRankText
                    else -> ""
                }
                val isBold = sc.id == "TOTAL" || sc.id == "GRADE" || sc.id == "RANK"
                val ink = if (!row.isPromoted && row.hasAnyData && (sc.id == "GRADE" || sc.id == "TOTAL")) {
                    INK_FAIL
                } else {
                    INK_DARK
                }
                drawCenteredCell(ctx, c, txt, x, ry, sc.width, rowH, dataFontBase, isBold, bnFont, enFont, ink)
                x += sc.width
            }

            c.drawLine(left, ry + rowH, right, ry + rowH, borderPaint)
        }

        // Re-draw outer box & strong subject separators cleanly on top
        val tableEndY = tableTop + totalHdrH + rowCount * rowH
        c.drawRect(left, tableTop, right, tableEndY, strongBorderPaint)

        // Re-stroke subject separators cleanly
        var sepX = left + rollW + nameW
        page.subjectGroups.forEach { grp ->
            c.drawLine(sepX, tableTop, sepX, tableEndY, strongBorderPaint)
            sepX += grp.subCols.size * subColW
        }
        c.drawLine(sepX, tableTop, sepX, tableEndY, strongBorderPaint)

        // 5. Summary Footer on last page
        if (cfg.showSummaryFooter && isLastPage) {
            val fTop = (tableEndY + 5f).coerceAtMost(bottom - 18f)
            val fH = 18f
            c.drawRect(left, fTop, right, fTop + fH, summaryBgPaint)
            c.drawRect(left, fTop, right, fTop + fH, borderPaint)

            val st = page.classStats
            val footerText = buildString {
                append("মোট শিক্ষার্থী: ${TabulationPlanner.formatDigits(st.totalStudents.toString(), cfg.numeralMode)}")
                append("   |   মূল্যায়িত: ${TabulationPlanner.formatDigits(st.evaluatedStudents.toString(), cfg.numeralMode)}")
                append("   |   উত্তীর্ণ: ${TabulationPlanner.formatDigits(st.promotedStudents.toString(), cfg.numeralMode)}")
                append("   |   অনুত্তীর্ণ: ${TabulationPlanner.formatDigits(st.notPromotedStudents.toString(), cfg.numeralMode)}")
                append("   |   পাসের হার: ${st.passRateText}")
                append("   |   সর্বোচ্চ গড়: ${st.highestTotalText}")
            }
            val fDp = PdfGen.dualPaint(ctx, 8.0f, true, bnFont, enFont, INK_DARK)
            PdfGen.shrinkMixedToFit(fDp, footerText, tableW - 12f, step = 0.2f, minSize = 6.0f)
            val fm = fDp.bn.fontMetrics
            val ty = fTop + fH / 2f - (fm.ascent + fm.descent) / 2f
            PdfGen.drawMixedText(c, footerText, left + tableW / 2f, ty, fDp, Paint.Align.CENTER)
        }
    }

    private fun drawCenteredCell(
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
        enFont: String,
        color: Int
    ) {
        c.drawRect(x, y, x + w, y + h, borderPaint)
        if (text.isBlank()) return
        val dp = PdfGen.dualPaint(ctx, size, bold, bnFont, enFont, color)
        PdfGen.shrinkMixedToFit(dp, text, (w - 3.2f).coerceAtLeast(6f), step = 0.2f, minSize = 4.6f)
        val fm = dp.bn.fontMetrics
        val ty = y + h / 2f - (fm.ascent + fm.descent) / 2f
        PdfGen.drawMixedText(c, text, x + w / 2f, ty, dp, Paint.Align.CENTER)
    }

    private fun drawMultilineCenteredCell(
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
        enFont: String,
        color: Int
    ) {
        c.drawRect(x, y, x + w, y + h, borderPaint)
        if (text.isBlank()) return
        val lines = text.split('\n').filter { it.isNotBlank() }
        if (lines.size <= 1) {
            drawCenteredCell(ctx, c, text, x, y, w, h, size, bold, bnFont, enFont, color)
            return
        }
        val dp = PdfGen.dualPaint(ctx, size, bold, bnFont, enFont, color)
        val maxLineW = (w - 3.2f).coerceAtLeast(6f)
        lines.forEach { ln ->
            PdfGen.shrinkMixedToFit(dp, ln, maxLineW, step = 0.2f, minSize = 4.6f)
        }
        val step = min(dp.bn.textSize * 1.15f, h / lines.size.toFloat())
        val fm = dp.bn.fontMetrics
        val totalBlockH = step * (lines.size - 1) + (fm.descent - fm.ascent)
        var baseY = y + (h - totalBlockH) / 2f - fm.ascent
        lines.forEach { ln ->
            PdfGen.drawMixedText(c, ln, x + w / 2f, baseY, dp, Paint.Align.CENTER)
            baseY += step
        }
    }

    private fun drawLeftCell(
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
        enFont: String,
        color: Int
    ) {
        c.drawRect(x, y, x + w, y + h, borderPaint)
        if (text.isBlank()) return
        val dp = PdfGen.dualPaint(ctx, size, bold, bnFont, enFont, color)
        PdfGen.shrinkMixedToFit(dp, text, (w - 5f).coerceAtLeast(10f), step = 0.2f, minSize = 4.8f)
        val fm = dp.bn.fontMetrics
        val ty = y + h / 2f - (fm.ascent + fm.descent) / 2f
        PdfGen.drawMixedText(c, text, x + 3f, ty, dp, Paint.Align.LEFT)
    }

    private fun drawVerticalHeaderCell(
        ctx: Context?,
        c: Canvas,
        text: String,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        size: Float,
        bnFont: String,
        enFont: String
    ) {
        c.drawRect(x, y, x + w, y + h, borderPaint)
        if (text.isBlank()) return
        val dp = PdfGen.dualPaint(ctx, size, true, bnFont, enFont, INK_DARK)
        PdfGen.shrinkMixedToFit(dp, text, (h - 4f).coerceAtLeast(12f), step = 0.2f, minSize = 4.6f)
        val fm = dp.bn.fontMetrics
        c.save()
        c.rotate(-90f, x + w / 2f, y + h / 2f)
        val baselineY = y + h / 2f - (fm.ascent + fm.descent) / 2f
        PdfGen.drawMixedText(c, text, x + w / 2f, baselineY, dp, Paint.Align.CENTER)
        c.restore()
    }
}
