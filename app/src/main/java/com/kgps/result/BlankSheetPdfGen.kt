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

    private fun dualTextPaint(
        ctx: Context?,
        size: Float,
        bold: Boolean,
        bnFontId: String,
        enFontId: String,
        color: Int = 0xFF000000.toInt()
    ): PdfGen.DualPaint = PdfGen.dualPaint(
        ctx = ctx,
        size = size,
        bold = bold,
        bnFontId = bnFontId,
        enFontId = enFontId,
        textColor = color
    )

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
        val enFontId = st.pdfEnglishFont
        val numStyle = page.numeralStyle

        // 1. Far-Left Thin Firestore/Cache QR + Centered [DPE Logo + School Title & Subtitle] + Far-Right Compact Offline QR
        val leftQrMatrix = if (page.thinQrPayload.isNotBlank()) SheetQrCodec.encodeQrMatrix(page.thinQrPayload) else null
        val rightQrMatrix = if (page.qrPayload.isNotBlank()) SheetQrCodec.encodeQrMatrix(page.qrPayload) else null
        val qrBoxSize = 42f
        val qrTop = marginTop - 3f

        fun drawQrBox(matrix: com.google.zxing.common.BitMatrix, qrLeft: Float) {
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

            val quietPad = 2.8f
            val matrixW = matrix.width.coerceAtLeast(1)
            val matrixH = matrix.height.coerceAtLeast(1)
            val drawArea = qrBoxSize - quietPad * 2f
            val modW = drawArea / matrixW.toFloat()
            val modH = drawArea / matrixH.toFloat()
            val originX = qrLeft + quietPad
            val originY = qrTop + quietPad
            for (my in 0 until matrixH) {
                val topY = originY + my * modH
                val bottomY = originY + (my + 1) * modH
                for (mx in 0 until matrixW) {
                    if (matrix.get(mx, my)) {
                        val leftX = originX + mx * modW
                        val rightX = originX + (mx + 1) * modW
                        c.drawRect(leftX, topY, rightX, bottomY, qrModulePaint)
                    }
                }
            }
        }

        if (leftQrMatrix != null) {
            drawQrBox(leftQrMatrix, marginX)
        }
        if (rightQrMatrix != null) {
            drawQrBox(rightQrMatrix, marginX + contentW - qrBoxSize)
        }

        val hasAnyQr = leftQrMatrix != null || rightQrMatrix != null
        val qrSideClearance = if (hasAnyQr) qrBoxSize + 10f else 0f
        val centerAvailW = max(130f, contentW - qrSideClearance * 2f)

        val logoSize = 25f
        val logoGap = 7f
        val logoBmp = PdfGen.resolveLogoBitmap(ctx, "simple")
        val logoReserve = if (logoBmp != null) logoSize + logoGap else 0f
        val titleMaxW = max(95f, centerAvailW - logoReserve)

        val schoolName = st.school.trim().ifBlank { "প্রাথমিক বিদ্যালয়" }
        val schoolPaint = dualTextPaint(ctx, 13.5f, bold = true, bnFontId = fontId, enFontId = enFontId, color = 0xFF000000.toInt())
        PdfGen.shrinkMixedToFit(schoolPaint, schoolName, titleMaxW, step = 0.5f, minSize = 8.5f)

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
        val subPaint = dualTextPaint(ctx, 8.8f, bold = false, bnFontId = fontId, enFontId = enFontId, color = 0xFF262626.toInt())
        PdfGen.shrinkMixedToFit(subPaint, addressAndYear, titleMaxW, step = 0.3f, minSize = 6.6f)

        val textBlockW = max(
            PdfGen.measureMixedText(schoolName, schoolPaint),
            PdfGen.measureMixedText(addressAndYear, subPaint)
        )
        val groupW = min(centerAvailW, logoReserve + textBlockW)
        val groupStartX = pageW / 2f - groupW / 2f
        val textCenterX = if (logoBmp != null) {
            val textAreaStart = groupStartX + logoReserve
            val actualTextW = max(20f, groupW - logoReserve)
            c.drawBitmap(
                logoBmp,
                null,
                RectF(groupStartX, marginTop + 3f, groupStartX + logoSize, marginTop + 3f + logoSize),
                null
            )
            textAreaStart + actualTextW / 2f
        } else {
            pageW / 2f
        }

        PdfGen.drawMixedText(c, schoolName, textCenterX, marginTop + 14.5f, schoolPaint, Paint.Align.CENTER)
        PdfGen.drawMixedText(c, addressAndYear, textCenterX, marginTop + 27.5f, subPaint, Paint.Align.CENTER)

        // 2. Concise Bengali Class + Subject + Term Title Banner (White + Very Light Gray Shade + Black Border)
        val metaTop = if (hasAnyQr) marginTop + 42f else marginTop + 34f
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

        val leftBannerPaint = dualTextPaint(ctx, 9.6f, bold = true, bnFontId = fontId, enFontId = enFontId, color = 0xFF000000.toInt())
        val centerBannerPaint = dualTextPaint(ctx, 10.2f, bold = true, bnFontId = fontId, enFontId = enFontId, color = 0xFF000000.toInt())
        val rightBannerPaint = dualTextPaint(ctx, 8.8f, bold = false, bnFontId = fontId, enFontId = enFontId, color = 0xFF1A1A1A.toInt())

        val maxSideW = contentW * 0.28f
        PdfGen.shrinkMixedToFit(leftBannerPaint, classBannerText, maxSideW, step = 0.3f, minSize = 7f)
        PdfGen.shrinkMixedToFit(rightBannerPaint, rightBannerText, maxSideW, step = 0.3f, minSize = 6.5f)
        val maxCenterW = contentW * 0.40f
        PdfGen.shrinkMixedToFit(centerBannerPaint, subjectBannerText, maxCenterW, step = 0.3f, minSize = 7f)

        PdfGen.drawMixedText(c, classBannerText, marginX + 8f, metaY, leftBannerPaint, Paint.Align.LEFT)
        PdfGen.drawMixedText(c, subjectBannerText, pageW / 2f, metaY, centerBannerPaint, Paint.Align.CENTER)
        PdfGen.drawMixedText(c, rightBannerText, marginX + contentW - 8f, metaY, rightBannerPaint, Paint.Align.RIGHT)

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

            val wdPaint = dualTextPaint(ctx, 8.8f, bold = true, bnFontId = fontId, enFontId = enFontId, color = 0xFF000000.toInt())
            val maxWdTextW = max(40f, contentW - 14f)
            PdfGen.shrinkMixedToFit(wdPaint, wdText, maxWdTextW, step = 0.25f, minSize = 6.4f)
            val wdBaseline = wdTop + (wdH + wdPaint.textSize * 0.72f) / 2f
            PdfGen.drawMixedText(c, wdText, pageW / 2f, wdBaseline, wdPaint, Paint.Align.CENTER)
            wdTop + wdH + 5f
        } else {
            metaTop + metaH + 7f
        }

        // 3. Table Geometry
        val barcodeMatrix = if (page.barcodeText.isNotBlank()) SheetQrCodec.encodeBarcodeMatrix(page.barcodeText) else null
        val footerReservedH = if (barcodeMatrix != null) 48f else 22f
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
            val hp = dualTextPaint(
                ctx = ctx,
                size = baseTextSize,
                bold = true,
                bnFontId = fontId,
                enFontId = enFontId,
                color = 0xFF000000.toInt()
            )
            val maxLineW = max(6f, cellW - 3.5f)
            while (lines.maxOf { PdfGen.measureMixedText(it, hp) } > maxLineW && hp.textSize > 4.6f) {
                hp.textSize -= 0.25f
            }
            val lineH = hp.textSize * 1.16f
            var ty = cellY + (cellH - lineH * lines.size) / 2f + hp.textSize * 0.88f
            lines.forEach { line ->
                PdfGen.drawMixedText(c, line, cellX + cellW / 2f, ty, hp, Paint.Align.CENTER)
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
                    val cp = dualTextPaint(
                        ctx = ctx,
                        size = baseCellFont,
                        bold = isRoll,
                        bnFontId = fontId,
                        enFontId = enFontId,
                        color = 0xFF000000.toInt()
                    )
                    val padX = if (col.align == BlankColAlign.LEFT) 5f else 3f
                    val availW = max(8f, w - padX * 2f)
                    PdfGen.shrinkMixedToFit(cp, cellValue, availW, step = 0.3f, minSize = 5.2f)
                    val textBaseline = curY + (rowH + cp.textSize * 0.72f) / 2f
                    if (col.align == BlankColAlign.LEFT) {
                        PdfGen.drawMixedText(c, cellValue, cx + padX, textBaseline, cp, Paint.Align.LEFT)
                    } else {
                        PdfGen.drawMixedText(c, cellValue, cx + w / 2f, textBaseline, cp, Paint.Align.CENTER)
                    }
                }
                cx += w
            }
            curY += rowH
        }

        c.drawRect(marginX, tableTop, marginX + contentW, curY, headerStrokePaint)

        // 6. Optional Code-128 Barcode + Monochrome Footer Line
        val footerY = pageH - marginBottom + 2f

        if (barcodeMatrix != null) {
            val barH = 16f
            val barW = min(contentW * 0.40f, max(132f, barcodeMatrix.width * 1.02f))
            val barLeft = marginX + 4f
            val barTop = footerY - 34f
            val barQuietX = 4f
            val barBox = RectF(
                barLeft - barQuietX,
                barTop - 2f,
                barLeft + barW + barQuietX,
                barTop + barH + 8.5f
            )
            val whitePaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            val barPaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.FILL
            }
            c.drawRect(barBox, whitePaint)
            val mw = barcodeMatrix.width.coerceAtLeast(1)
            val unitW = barW / mw.toFloat()
            var bx = 0
            while (bx < mw) {
                if (barcodeMatrix.get(bx, 0)) {
                    var runEnd = bx + 1
                    while (runEnd < mw && barcodeMatrix.get(runEnd, 0)) runEnd++
                    val rx1 = barLeft + bx * unitW
                    val rx2 = barLeft + runEnd * unitW
                    c.drawRect(rx1, barTop, rx2, barTop + barH, barPaint)
                    bx = runEnd
                } else {
                    bx++
                }
            }
            val barLabelPaint = dualTextPaint(ctx, 5.8f, bold = false, bnFontId = fontId, enFontId = enFontId, color = 0xFF1A1A1A.toInt())
            PdfGen.drawMixedText(c, page.barcodeText, barLeft + barW / 2f, barTop + barH + 6.5f, barLabelPaint, Paint.Align.CENTER)
        }

        val footerLeftPaint = dualTextPaint(ctx, 7.8f, bold = false, bnFontId = fontId, enFontId = enFontId, color = 0xFF262626.toInt())
        val footerRightPaint = dualTextPaint(ctx, 8.2f, bold = false, bnFontId = fontId, enFontId = enFontId, color = 0xFF000000.toInt())
        val refSuffix = if (page.sheetRefCode.isNotBlank()) " · ${page.sheetRefCode}" else ""
        val leftFooter = "মোট: ${BlankSheetCatalog.formatInt(page.totalStudentsInClass, numStyle)} জন · মোট পৃষ্ঠা ${BlankSheetCatalog.formatInt(page.globalPageNumber, numStyle)}/${BlankSheetCatalog.formatInt(page.totalPages, numStyle)}$refSuffix"
        val rightFooter = "তারিখ: ......................   শিক্ষকের স্বাক্ষর: ..........................."
        PdfGen.drawMixedText(c, leftFooter, marginX, footerY, footerLeftPaint, Paint.Align.LEFT)
        PdfGen.drawMixedText(c, rightFooter, marginX + contentW, footerY, footerRightPaint, Paint.Align.RIGHT)

        if (BlankSheetCatalog.isThreeTermAttendanceTraitsActive(cols)) {
            val legendText = "গুণাবলি: ক = উত্তম · খ = সন্তোষজনক · গ = উন্নতি প্রয়োজন"
            val legendPaint = dualTextPaint(ctx, 7.6f, bold = false, bnFontId = fontId, enFontId = enFontId, color = 0xFF333333.toInt())
            val leftW = PdfGen.measureMixedText(leftFooter, footerLeftPaint)
            val rightW = PdfGen.measureMixedText(rightFooter, footerRightPaint)
            val freeCenterW = contentW - leftW - rightW - 20f
            if (freeCenterW > 80f) {
                PdfGen.shrinkMixedToFit(legendPaint, legendText, freeCenterW, step = 0.2f, minSize = 5.8f)
                val centerX = marginX + leftW + 10f + freeCenterW / 2f
                PdfGen.drawMixedText(c, legendText, centerX, footerY, legendPaint, Paint.Align.CENTER)
            }
        }
    }
}
