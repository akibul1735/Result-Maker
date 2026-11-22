package com.kgps.result

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
import kotlin.math.max
import kotlin.math.min

object BlankSheetPdfGen {
    private const val A4_SHORT = 595
    private const val A4_LONG = 842

    fun pageDimensions(orientation: BlankSheetOrientation): Pair<Int, Int> = when (orientation) {
        BlankSheetOrientation.PORTRAIT -> A4_SHORT to A4_LONG
        BlankSheetOrientation.LANDSCAPE -> A4_LONG to A4_SHORT
    }

    // Pure Black, White, and Ultra-Light Gray Shades Only
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF000000.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 0.7f
    }

    private val headerStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF000000.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1.0f
    }

    private val headerFillPaint = Paint().apply {
        color = 0xFFF2F2F2.toInt() // ~5% very light black shade
        style = Paint.Style.FILL
    }

    private val metaFillPaint = Paint().apply {
        color = 0xFFF7F7F7.toInt() // ~3% very light black shade
        style = Paint.Style.FILL
    }

    private val metaStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF000000.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 0.75f
    }

    private val altRowFillPaint = Paint().apply {
        color = 0xFFFAFAFA.toInt() // ~2% ultra-light black shade
        style = Paint.Style.FILL
    }

    private fun textPaint(
        ctx: Context?,
        size: Float,
        bold: Boolean,
        fontId: String,
        color: Int = 0xFF000000.toInt()
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = PdfGen.resolveTypeface(ctx, fontId, bold)
    }

    fun renderPageBitmap(
        ctx: Context,
        page: BlankSheetPageSpec,
        settings: Settings,
        orientation: BlankSheetOrientation,
        scale: Float = 2.0f
    ): Bitmap {
        val (w, h) = pageDimensions(orientation)
        val bmp = Bitmap.createBitmap((w * scale).toInt(), (h * scale).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        canvas.scale(scale, scale)
        drawPage(ctx, canvas, page, settings, orientation)
        return bmp
    }

    fun write(
        ctx: Context,
        pages: List<BlankSheetPageSpec>,
        settings: Settings,
        orientation: BlankSheetOrientation,
        out: OutputStream
    ) {
        if (pages.isEmpty()) return
        val (w, h) = pageDimensions(orientation)
        val doc = PdfDocument()
        try {
            pages.forEachIndexed { idx, page ->
                val pdfPage = doc.startPage(PdfDocument.PageInfo.Builder(w, h, idx + 1).create())
                pdfPage.canvas.drawColor(Color.WHITE)
                drawPage(ctx, pdfPage.canvas, page, settings, orientation)
                doc.finishPage(pdfPage)
            }
            doc.writeTo(out)
        } finally {
            doc.close()
        }
    }

    fun print(
        ctx: Context,
        pages: List<BlankSheetPageSpec>,
        settings: Settings,
        orientation: BlankSheetOrientation,
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
                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled()
                    return
                }
                try {
                    FileOutputStream(destination.fileDescriptor).use { fos ->
                        write(ctx, pages, settings, orientation, fos)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }
        val media = if (orientation == BlankSheetOrientation.LANDSCAPE) {
            PrintAttributes.MediaSize.ISO_A4.asLandscape()
        } else {
            PrintAttributes.MediaSize.ISO_A4.asPortrait()
        }
        val attrs = PrintAttributes.Builder()
            .setMediaSize(media)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
        printManager.print(jobName, adapter, attrs)
    }

    private fun drawPage(
        ctx: Context?,
        c: Canvas,
        page: BlankSheetPageSpec,
        st: Settings,
        orientation: BlankSheetOrientation
    ) {
        val (pageWInt, pageHInt) = pageDimensions(orientation)
        val pageW = pageWInt.toFloat()
        val pageH = pageHInt.toFloat()

        val marginX = 24f
        val marginTop = 22f
        val marginBottom = 20f
        val contentW = pageW - marginX * 2f
        val fontId = st.pdfFont
        val numStyle = page.numeralStyle

        // 1. Monochrome Black-and-White DPE Logo on top-left + School Title & Subtitle + Top-Right QR Code
        val logoSize = 27f
        val logoBmp = PdfGen.resolveLogoBitmap(ctx, "simple")
        if (logoBmp != null) {
            c.drawBitmap(
                logoBmp,
                null,
                RectF(marginX, marginTop, marginX + logoSize, marginTop + logoSize),
                null
            )
        }

        val qrMatrix = if (page.qrPayload.isNotBlank()) SheetQrCodec.encodeQrMatrix(page.qrPayload) else null
        val qrBoxSize = 40f
        if (qrMatrix != null) {
            val qrLeft = marginX + contentW - qrBoxSize
            val qrTop = marginTop - 3f
            val qrBox = RectF(qrLeft, qrTop, qrLeft + qrBoxSize, qrTop + qrBoxSize)
            val whitePaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            val qrFramePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = 0.55f
            }
            val qrModulePaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.FILL
            }
            c.drawRoundRect(qrBox, 2f, 2f, whitePaint)
            c.drawRoundRect(qrBox, 2f, 2f, qrFramePaint)

            val quietPad = 2.6f
            val matrixW = qrMatrix.width.coerceAtLeast(1)
            val matrixH = qrMatrix.height.coerceAtLeast(1)
            val drawArea = qrBoxSize - quietPad * 2f
            val modW = drawArea / matrixW.toFloat()
            val modH = drawArea / matrixH.toFloat()
            val originX = qrLeft + quietPad
            val originY = qrTop + quietPad
            for (my in 0 until matrixH) {
                val topY = originY + my * modH
                val bottomY = originY + (my + 1) * modH
                for (mx in 0 until matrixW) {
                    if (qrMatrix.get(mx, my)) {
                        val leftX = originX + mx * modW
                        val rightX = originX + (mx + 1) * modW
                        c.drawRect(leftX, topY, rightX, bottomY, qrModulePaint)
                    }
                }
            }
        }

        val leftClearance = if (logoBmp != null) logoSize + 8f else 0f
        val rightClearance = if (qrMatrix != null) qrBoxSize + 8f else 0f
        val sideReserve = max(leftClearance, rightClearance)

        val schoolName = st.school.trim().ifBlank { "প্রাথমিক বিদ্যালয়" }
        val schoolPaint = textPaint(ctx, 13.5f, bold = true, fontId = fontId, color = 0xFF000000.toInt()).apply {
            textAlign = Paint.Align.CENTER
        }
        val titleMaxW = max(120f, contentW - sideReserve * 2f)
        while (schoolPaint.measureText(schoolName) > titleMaxW && schoolPaint.textSize > 8.5f) {
            schoolPaint.textSize -= 0.5f
        }
        c.drawText(schoolName, pageW / 2f, marginTop + 13.5f, schoolPaint)

        val yearFormatted = BlankSheetCatalog.formatDigits(st.year.ifBlank { "২০২৬" }, numStyle)
        val addressAndYear = buildString {
            val addr = st.schoolAddress.trim()
            if (addr.isNotEmpty()) {
                append(addr)
                append(" · ")
            }
            append("শিক্ষাবর্ষ: ")
            append(yearFormatted)
        }
        val subPaint = textPaint(ctx, 8.8f, bold = false, fontId = fontId, color = 0xFF262626.toInt()).apply {
            textAlign = Paint.Align.CENTER
        }
        while (subPaint.measureText(addressAndYear) > titleMaxW && subPaint.textSize > 6.8f) {
            subPaint.textSize -= 0.3f
        }
        c.drawText(addressAndYear, pageW / 2f, marginTop + 26.5f, subPaint)

        // 2. Concise Bengali Class + Subject + Term Title Banner (White + Very Light Gray Shade + Black Border)
        val metaTop = if (qrMatrix != null) marginTop + 40f else marginTop + 34f
        val metaH = 24f
        val metaRect = RectF(marginX, metaTop, marginX + contentW, metaTop + metaH)
        c.drawRoundRect(metaRect, 3f, 3f, metaFillPaint)
        c.drawRoundRect(metaRect, 3f, 3f, metaStrokePaint)

        val metaY = metaTop + 15.8f
        val classBannerText = "শ্রেণি: ${page.classTitleBn}"
        val subjectBannerText = when {
            page.isClassLevelForm -> page.subjectTitleBn
            page.subjectTitleBn.isNotBlank() -> "বিষয়: ${page.subjectTitleBn}"
            else -> "বিষয়: ......................"
        }
        val rightBannerText = buildString {
            if (page.termTitleBn.isNotBlank()) {
                append(page.termTitleBn)
                append(" · ")
            }
            append("পৃষ্ঠা ")
            append(BlankSheetCatalog.formatInt(page.classPageIndex, numStyle))
            append("/")
            append(BlankSheetCatalog.formatInt(page.classTotalPages, numStyle))
        }

        val leftBannerPaint = textPaint(ctx, 9.6f, bold = true, fontId = fontId, color = 0xFF000000.toInt()).apply {
            textAlign = Paint.Align.LEFT
        }
        val centerBannerPaint = textPaint(ctx, 10.2f, bold = true, fontId = fontId, color = 0xFF000000.toInt()).apply {
            textAlign = Paint.Align.CENTER
        }
        val rightBannerPaint = textPaint(ctx, 8.8f, bold = false, fontId = fontId, color = 0xFF1A1A1A.toInt()).apply {
            textAlign = Paint.Align.RIGHT
        }

        val maxSideW = contentW * 0.28f
        while (leftBannerPaint.measureText(classBannerText) > maxSideW && leftBannerPaint.textSize > 7f) {
            leftBannerPaint.textSize -= 0.3f
        }
        while (rightBannerPaint.measureText(rightBannerText) > maxSideW && rightBannerPaint.textSize > 6.5f) {
            rightBannerPaint.textSize -= 0.3f
        }
        val maxCenterW = contentW * 0.40f
        while (centerBannerPaint.measureText(subjectBannerText) > maxCenterW && centerBannerPaint.textSize > 7f) {
            centerBannerPaint.textSize -= 0.3f
        }

        c.drawText(classBannerText, marginX + 8f, metaY, leftBannerPaint)
        c.drawText(subjectBannerText, pageW / 2f, metaY, centerBannerPaint)
        c.drawText(rightBannerText, marginX + contentW - 8f, metaY, rightBannerPaint)

        val cols = page.columns.ifEmpty { BlankSheetCatalog.DEFAULT_COLUMNS }
        val hasGroupedHeader = cols.any { !it.groupBn.isNullOrBlank() }

        // Optional shared single-place মোট কার্যদিবস strip (replaces repeating কার্যদিবস on every row)
        val tableTop = if (page.showSharedWorkingDaysStrip) {
            val wdTop = metaTop + metaH + 5f
            val wdH = 19f
            val wdRect = RectF(marginX, wdTop, marginX + contentW, wdTop + wdH)
            c.drawRoundRect(wdRect, 2.5f, 2.5f, metaFillPaint)
            c.drawRoundRect(wdRect, 2.5f, 2.5f, borderPaint)

            val wdText = if (page.sharedWorkingDaysTerms.size == 1) {
                val t = page.sharedWorkingDaysTerms.first()
                val raw = page.workingDaysByTerm[t]?.let { fromBn(it).trim() }.orEmpty()
                val valStr = if (raw.isNotEmpty()) {
                    "${BlankSheetCatalog.formatDigits(raw, numStyle)} দিন"
                } else {
                    ".............. দিন"
                }
                "মোট কার্যদিবস (${termNameBn(t)}): $valStr"
            } else {
                val termParts = page.sharedWorkingDaysTerms.joinToString("    |    ") { t ->
                    val raw = page.workingDaysByTerm[t]?.let { fromBn(it).trim() }.orEmpty()
                    val valStr = if (raw.isNotEmpty()) {
                        "${BlankSheetCatalog.formatDigits(raw, numStyle)} দিন"
                    } else {
                        "......... দিন"
                    }
                    "${termNameBn(t)}: $valStr"
                }
                "মোট কার্যদিবস —   $termParts"
            }

            val wdPaint = textPaint(ctx, 8.8f, bold = true, fontId = fontId, color = 0xFF000000.toInt()).apply {
                textAlign = Paint.Align.CENTER
            }
            val maxWdTextW = max(40f, contentW - 14f)
            while (wdPaint.measureText(wdText) > maxWdTextW && wdPaint.textSize > 6.4f) {
                wdPaint.textSize -= 0.25f
            }
            val wdBaseline = wdTop + (wdH + wdPaint.textSize * 0.72f) / 2f
            c.drawText(wdText, pageW / 2f, wdBaseline, wdPaint)
            wdTop + wdH + 5f
        } else {
            metaTop + metaH + 7f
        }

        // 3. Table Geometry
        val footerReservedH = 22f
        val tableBottomLimit = pageH - marginBottom - footerReservedH
        val hasMaxSubHeader = page.showSubjectMaxInHeader && cols.any { col ->
            BlankSheetCatalog.resolveMaxHint(page.cls, page.subjectKey, col.compKey) != null
        }
        val groupRowH = 17f
        val subRowH = if (hasMaxSubHeader) 27f else 21f
        val tableHeaderH = if (hasGroupedHeader) {
            groupRowH + subRowH
        } else if (hasMaxSubHeader) {
            30f
        } else {
            24f
        }
        val bodyAvailH = max(80f, tableBottomLimit - tableTop - tableHeaderH)

        val capacity = page.rowsPerPageCapacity.coerceIn(5, 50)
        val rowCount = page.rows.size.coerceAtLeast(1)
        val rowH = min(
            bodyAvailH / rowCount.toFloat(),
            max(16f, min(38f, bodyAvailH / capacity.toFloat()))
        )

        val totalWeight = cols.sumOf { it.widthWeight.toDouble() }.toFloat().coerceAtLeast(1f)
        val colWidths = cols.map { (it.widthWeight / totalWeight) * contentW }

        fun drawHeaderLines(
            lines: List<String>,
            cellX: Float,
            cellY: Float,
            cellW: Float,
            cellH: Float,
            baseTextSize: Float
        ) {
            val hp = textPaint(
                ctx = ctx,
                size = baseTextSize,
                bold = true,
                fontId = fontId,
                color = 0xFF000000.toInt()
            ).apply {
                textAlign = Paint.Align.CENTER
            }
            val maxLineW = max(6f, cellW - 3.5f)
            while (lines.maxOf { hp.measureText(it) } > maxLineW && hp.textSize > 4.6f) {
                hp.textSize -= 0.25f
            }
            val lineH = hp.textSize * 1.16f
            var ty = cellY + (cellH - lineH * lines.size) / 2f + hp.textSize * 0.88f
            lines.forEach { line ->
                c.drawText(line, cellX + cellW / 2f, ty, hp)
                ty += lineH
            }
        }

        // 4. Draw Table Header Row(s) (Very Light Gray Shade + Black Borders + Concise Bengali Text)
        c.drawRect(marginX, tableTop, marginX + contentW, tableTop + tableHeaderH, headerFillPaint)
        val colXPositions = FloatArray(cols.size)
        run {
            var accX = marginX
            cols.indices.forEach { i ->
                colXPositions[i] = accX
                accX += colWidths[i]
            }
        }

        if (!hasGroupedHeader) {
            cols.forEachIndexed { idx, col ->
                val cx = colXPositions[idx]
                val w = colWidths[idx]
                c.drawRect(cx, tableTop, cx + w, tableTop + tableHeaderH, headerStrokePaint)

                val maxHint = if (page.showSubjectMaxInHeader) {
                    BlankSheetCatalog.resolveMaxHint(page.cls, page.subjectKey, col.compKey)
                } else {
                    null
                }
                val lines = if (maxHint != null) {
                    col.titleBn.split("\n") + "(${BlankSheetCatalog.formatInt(maxHint, numStyle)})"
                } else {
                    col.titleBn.split("\n")
                }
                val baseSize = if (cols.size >= 9) 8.2f else 9.0f
                drawHeaderLines(lines, cx, tableTop, w, tableHeaderH, baseSize)
            }
        } else {
            val baseSubSize = when {
                cols.size >= 15 -> 7.6f
                cols.size >= 10 -> 8.2f
                else -> 8.8f
            }
            var idx = 0
            while (idx < cols.size) {
                val col = cols[idx]
                val grp = col.groupBn?.trim().orEmpty()
                if (grp.isEmpty()) {
                    val cx = colXPositions[idx]
                    val w = colWidths[idx]
                    c.drawRect(cx, tableTop, cx + w, tableTop + tableHeaderH, headerStrokePaint)
                    val maxHint = if (page.showSubjectMaxInHeader) {
                        BlankSheetCatalog.resolveMaxHint(page.cls, page.subjectKey, col.compKey)
                    } else {
                        null
                    }
                    val lines = if (maxHint != null) {
                        col.titleBn.split("\n") + "(${BlankSheetCatalog.formatInt(maxHint, numStyle)})"
                    } else {
                        col.titleBn.split("\n")
                    }
                    drawHeaderLines(lines, cx, tableTop, w, tableHeaderH, 9.0f)
                    idx++
                } else {
                    var endIdx = idx
                    while (endIdx + 1 < cols.size && cols[endIdx + 1].groupBn?.trim() == grp) {
                        endIdx++
                    }
                    val groupX = colXPositions[idx]
                    var groupW = 0f
                    for (k in idx..endIdx) groupW += colWidths[k]
                    c.drawRect(groupX, tableTop, groupX + groupW, tableTop + groupRowH, headerStrokePaint)
                    drawHeaderLines(listOf(grp), groupX, tableTop, groupW, groupRowH, 9.0f)

                    for (k in idx..endIdx) {
                        val subCol = cols[k]
                        val subX = colXPositions[k]
                        val subW = colWidths[k]
                        c.drawRect(subX, tableTop + groupRowH, subX + subW, tableTop + tableHeaderH, headerStrokePaint)
                        val maxHint = if (page.showSubjectMaxInHeader) {
                            BlankSheetCatalog.resolveMaxHint(page.cls, page.subjectKey, subCol.compKey)
                        } else {
                            null
                        }
                        val lines = if (maxHint != null) {
                            subCol.titleBn.split("\n") + "(${BlankSheetCatalog.formatInt(maxHint, numStyle)})"
                        } else {
                            subCol.titleBn.split("\n")
                        }
                        drawHeaderLines(lines, subX, tableTop + groupRowH, subW, subRowH, baseSubSize)
                    }
                    idx = endIdx + 1
                }
            }
        }

        // 5. Draw Table Body Rows (Only Roll & Name filled; all other cells blank)
        var curY = tableTop + tableHeaderH
        val baseCellFont = when {
            rowH >= 28f -> 9.8f
            rowH >= 22f -> 9.0f
            rowH >= 18f -> 8.2f
            else -> 7.4f
        }

        page.rows.forEachIndexed { rIdx, row ->
            if (rIdx % 2 == 1) {
                c.drawRect(marginX, curY, marginX + contentW, curY + rowH, altRowFillPaint)
            }
            var cx = marginX
            cols.forEachIndexed { cIdx, col ->
                val w = colWidths[cIdx]
                c.drawRect(cx, curY, cx + w, curY + rowH, borderPaint)

                val cellValue = when (col.fillWith) {
                    BlankColFill.ROLL -> row.rollText
                    BlankColFill.NAME -> row.nameText
                    BlankColFill.NONE -> ""
                }
                if (cellValue.isNotBlank()) {
                    val isRoll = col.fillWith == BlankColFill.ROLL
                    val cp = textPaint(
                        ctx = ctx,
                        size = baseCellFont,
                        bold = isRoll,
                        fontId = fontId,
                        color = 0xFF000000.toInt()
                    )
                    val padX = if (col.align == BlankColAlign.LEFT) 5f else 3f
                    val availW = max(8f, w - padX * 2f)
                    while (cp.measureText(cellValue) > availW && cp.textSize > 5.2f) {
                        cp.textSize -= 0.3f
                    }
                    val textBaseline = curY + (rowH + cp.textSize * 0.72f) / 2f
                    if (col.align == BlankColAlign.LEFT) {
                        cp.textAlign = Paint.Align.LEFT
                        c.drawText(cellValue, cx + padX, textBaseline, cp)
                    } else {
                        cp.textAlign = Paint.Align.CENTER
                        c.drawText(cellValue, cx + w / 2f, textBaseline, cp)
                    }
                }
                cx += w
            }
            curY += rowH
        }

        c.drawRect(marginX, tableTop, marginX + contentW, curY, headerStrokePaint)

        // 6. Monochrome Footer Line
        val footerY = pageH - marginBottom + 2f
        val footerLeftPaint = textPaint(ctx, 7.8f, bold = false, fontId = fontId, color = 0xFF262626.toInt()).apply {
            textAlign = Paint.Align.LEFT
        }
        val footerRightPaint = textPaint(ctx, 8.2f, bold = false, fontId = fontId, color = 0xFF000000.toInt()).apply {
            textAlign = Paint.Align.RIGHT
        }
        val refSuffix = if (page.sheetRefCode.isNotBlank()) " · ${page.sheetRefCode}" else ""
        val leftFooter = "মোট: ${BlankSheetCatalog.formatInt(page.totalStudentsInClass, numStyle)} জন · মোট পৃষ্ঠা ${BlankSheetCatalog.formatInt(page.globalPageNumber, numStyle)}/${BlankSheetCatalog.formatInt(page.totalPages, numStyle)}$refSuffix"
        val rightFooter = "তারিখ: ......................   শিক্ষকের স্বাক্ষর: ..........................."
        c.drawText(leftFooter, marginX, footerY, footerLeftPaint)
        c.drawText(rightFooter, marginX + contentW, footerY, footerRightPaint)

        if (BlankSheetCatalog.isThreeTermAttendanceTraitsActive(cols)) {
            val legendText = "গুণাবলি: ক = উত্তম · খ = সন্তোষজনক · গ = উন্নতি প্রয়োজন"
            val legendPaint = textPaint(ctx, 7.6f, bold = false, fontId = fontId, color = 0xFF333333.toInt()).apply {
                textAlign = Paint.Align.CENTER
            }
            val leftW = footerLeftPaint.measureText(leftFooter)
            val rightW = footerRightPaint.measureText(rightFooter)
            val freeCenterW = contentW - leftW - rightW - 20f
            if (freeCenterW > 80f) {
                while (legendPaint.measureText(legendText) > freeCenterW && legendPaint.textSize > 5.8f) {
                    legendPaint.textSize -= 0.2f
                }
                val centerX = marginX + leftW + 10f + freeCenterW / 2f
                c.drawText(legendText, centerX, footerY, legendPaint)
            }
        }
    }
}
