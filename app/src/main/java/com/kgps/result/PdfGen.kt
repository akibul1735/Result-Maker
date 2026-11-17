package com.kgps.result

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * প্রতি শিক্ষার্থীর জন্য ২টি A4 Landscape পাতা (মাঝখানে x=421 বরাবর ভাঁজ করলে ৪-পাতার A5 বুকলেট):
 *  পাতা-১ (বাইরের পাশ):
 *    - বাম অর্ধেক (পৃষ্ঠা-৪ / ব্যাক কভার): মতামত ও স্বাক্ষর, গ্রেড স্কেল, শ্রেণি শিক্ষক ও NCTB মনোগ্রাম
 *    - ডান অর্ধেক (পৃষ্ঠা-১ / ফ্রন্ট কভার): শিখন অগ্রগতি প্রতিবেদন ব্যানার, প্রাথমিক শিক্ষা অধিদপ্তর সিল, বিদ্যালয় ও শিক্ষার্থীর তথ্য (পোস্টসহ)
 *  পাতা-২ (ভেতরের পাশ):
 *    - বাম অর্ধেক (পৃষ্ঠা-২): বিষয় + ১ম ও ২য় প্রান্তিকের নম্বর এবং নিচে ব্যক্তিগত ও সামাজিক গুণাবলি
 *    - ডান অর্ধেক (পৃষ্ঠা-৩): ৩য় প্রান্তিক + চূড়ান্ত ফলাফল এবং নিচে উপস্থিতি সারণি
 */
object PdfGen {
    const val W = 842
    const val H = 595
    private const val FOLD_X = W / 2f

    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0F172A.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 0.75f
    }
    private val thickFrame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0F172A.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2.4f
    }
    private val gray = Paint().apply {
        color = 0xFFE2E8F0.toInt()
        style = Paint.Style.FILL
    }
    private val darkFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0F172A.toInt()
        style = Paint.Style.FILL
    }
    private val foldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF94A3B8.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1f
        pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
    }
    private val dottedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF334155.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 0.85f
        pathEffect = DashPathEffect(floatArrayOf(1.6f, 2.4f), 0f)
    }

    private val fontCache = ConcurrentHashMap<String, Typeface>()
    private val logoCache = ConcurrentHashMap<String, Bitmap>()

    fun resolveLogoBitmap(ctx: Context?, logoId: String): Bitmap? {
        if (ctx == null) return null
        val path = if (logoId == "color") "logos/dpe_logo_color.png" else "logos/dpe_logo_simple.png"
        logoCache[path]?.let { return it }
        return try {
            ctx.assets.open(path).use { stream ->
                BitmapFactory.decodeStream(stream)
            }?.also { bmp ->
                logoCache[path] = bmp
            }
        } catch (_: Exception) {
            null
        }
    }

    fun resolveTypeface(ctx: Context?, fontId: String, bold: Boolean): Typeface {
        val assetPath = when (fontId) {
            "noto_serif", "tiro_bangla" ->
                if (bold) "fonts/NotoSerifBengali-Bold.ttf" else "fonts/NotoSerifBengali-Regular.ttf"
            "noto_sans", "hind_siliguri" ->
                if (bold) "fonts/NotoSansBengali-Bold.ttf" else "fonts/NotoSansBengali-Regular.ttf"
            else -> null
        }
        if (ctx != null && assetPath != null) {
            return fontCache.getOrPut(assetPath) {
                try {
                    Typeface.createFromAsset(ctx.assets, assetPath)
                } catch (_: Exception) {
                    Typeface.create(
                        if (fontId == "noto_serif" || fontId == "tiro_bangla") Typeface.SERIF else Typeface.SANS_SERIF,
                        if (bold) Typeface.BOLD else Typeface.NORMAL
                    )
                }
            }
        }
        val base = when (fontId) {
            "noto_serif", "tiro_bangla" -> Typeface.SERIF
            "noto_sans", "hind_siliguri" -> Typeface.SANS_SERIF
            else -> Typeface.DEFAULT
        }
        return Typeface.create(base, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun tp(
        ctx: Context?,
        size: Float,
        bold: Boolean = false,
        fontId: String = "noto_serif",
        textColor: Int = 0xFF0F172A.toInt()
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        color = textColor
        typeface = resolveTypeface(ctx, fontId, bold)
    }

    private fun wrap(text: String, p: Paint, w: Float): List<String> {
        val out = mutableListOf<String>()
        text.split("\n").forEach { para ->
            var cur = ""
            para.split(" ").forEach { word ->
                val t = if (cur.isEmpty()) word else "$cur $word"
                if (p.measureText(t) <= w || cur.isEmpty()) cur = t else {
                    out.add(cur)
                    cur = word
                }
            }
            out.add(cur)
        }
        return out
    }

    private fun drawPanelFrames(c: Canvas, showFoldGuide: Boolean) {
        if (showFoldGuide) {
            c.drawLine(FOLD_X, 6f, FOLD_X, H - 6f, foldPaint)
        }
        // বাম A5 ফ্রেম (18..409)
        c.drawRect(18f, 18f, 409f, 577f, thickFrame)
        c.drawRect(24f, 24f, 403f, 571f, stroke)

        // ডান A5 ফ্রেম (433..824)
        c.drawRect(433f, 18f, 824f, 577f, thickFrame)
        c.drawRect(439f, 24f, 818f, 571f, stroke)
    }

    private fun cell(
        ctx: Context?,
        c: Canvas,
        t: String,
        l: Float,
        top: Float,
        w: Float,
        h: Float,
        size: Float = 9f,
        bold: Boolean = false,
        fill: Boolean = false,
        fontId: String = "noto_serif"
    ) {
        if (fill) c.drawRect(l, top, l + w, top + h, gray)
        c.drawRect(l, top, l + w, top + h, stroke)
        if (t.isEmpty()) return
        val p = tp(ctx, size, bold, fontId)
        p.textAlign = Paint.Align.CENTER
        val lines = t.split("\n")
        while (lines.maxOf { p.measureText(it) } > w - 4 && p.textSize > 4.5f) {
            p.textSize -= 0.4f
        }
        val lh = p.textSize * 1.24f
        var y = top + (h - lh * lines.size) / 2 + p.textSize * 0.92f
        lines.forEach {
            c.drawText(it, l + w / 2, y, p)
            y += lh
        }
    }

    private fun box(
        ctx: Context?,
        c: Canvas,
        t: String,
        l: Float,
        top: Float,
        w: Float,
        h: Float,
        size: Float = 7.2f,
        fontId: String = "noto_serif"
    ) {
        c.drawRect(l, top, l + w, top + h, stroke)
        if (t.isEmpty()) return
        val p = tp(ctx, size, false, fontId)
        var lines = wrap(t, p, w - 7f)
        while (lines.size * (p.textSize * 1.25f) > h - 4f && p.textSize > 5.2f) {
            p.textSize -= 0.3f
            lines = wrap(t, p, w - 7f)
        }
        val lh = p.textSize * 1.25f
        val totalH = lines.size * lh
        var y = top + max(p.textSize + 2f, (h - totalH) / 2 + p.textSize * 0.9f)
        lines.forEach {
            if (y < top + h - 2f) c.drawText(it, l + 3.5f, y, p)
            y += lh
        }
    }

    private fun center(
        ctx: Context?,
        c: Canvas,
        t: String,
        cx: Float,
        y: Float,
        size: Float,
        bold: Boolean = false,
        maxW: Float = 340f,
        fontId: String = "noto_serif",
        textColor: Int = 0xFF0F172A.toInt()
    ) {
        val p = tp(ctx, size, bold, fontId, textColor)
        p.textAlign = Paint.Align.CENTER
        while (p.measureText(t) > maxW && p.textSize > 5.5f) {
            p.textSize -= 0.5f
        }
        c.drawText(t, cx, y, p)
    }

    private fun dottedField(
        ctx: Context?,
        c: Canvas,
        label: String,
        value: String,
        x: Float,
        y: Float,
        maxX: Float,
        fontSize: Float = 10.5f,
        fontId: String = "noto_serif"
    ) {
        val lp = tp(ctx, fontSize, false, fontId)
        c.drawText(label, x, y, lp)
        val lineStart = x + lp.measureText(label) + 5f
        c.drawLine(lineStart, y + 3f, maxX, y + 3f, dottedPaint)

        val v = value.trim()
        if (v.isNotEmpty()) {
            val vp = tp(ctx, fontSize, true, fontId)
            val avail = max(20f, maxX - lineStart - 4f)
            while (vp.measureText(v) > avail && vp.textSize > 6.5f) {
                vp.textSize -= 0.4f
            }
            c.drawText(v, lineStart + 3f, y - 0.5f, vp)
        }
    }

    private fun drawDpeLogo(
        ctx: Context?,
        c: Canvas,
        cx: Float,
        topY: Float,
        targetH: Float,
        logoId: String,
        fontId: String
    ) {
        val bmp = resolveLogoBitmap(ctx, logoId)
        if (bmp != null && bmp.width > 0 && bmp.height > 0) {
            val w = (bmp.width.toFloat() / bmp.height.toFloat()) * targetH
            val dst = RectF(cx - w / 2f, topY, cx + w / 2f, topY + targetH)
            c.drawBitmap(bmp, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
            return
        }
        val cy = topY + targetH / 2f - 4f
        val r = 34f
        val sealStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0F172A.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
        }
        c.drawCircle(cx, cy, r, sealStroke)
        center(ctx, c, "প্রাথমিক শিক্ষা অধিদপ্তর", cx, cy + r + 12f, 8.5f, true, 160f, fontId)
    }

    private fun drawNctbEmblem(ctx: Context?, c: Canvas, cx: Float, cy: Float, r: Float, fontId: String) {
        val sealStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0F172A.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1.4f
        }
        c.drawCircle(cx, cy, r, sealStroke)
        sealStroke.strokeWidth = 0.7f
        c.drawCircle(cx, cy, r - 3.5f, sealStroke)

        sealStroke.strokeWidth = 1.1f
        c.drawLine(cx, cy - 10f, cx, cy + 8f, sealStroke)

        val wings = Path().apply {
            moveTo(cx - 8f, cy + 4f)
            quadTo(cx - 4f, cy + 1f, cx, cy + 4f)
            quadTo(cx + 4f, cy + 1f, cx + 8f, cy + 4f)
        }
        c.drawPath(wings, sealStroke)
        c.drawCircle(cx, cy - 6f, 3.2f, sealStroke)

        val p = tp(ctx, 6.5f, true, fontId)
        p.textAlign = Paint.Align.CENTER
        c.drawText("NCTB", cx, cy + 12.5f, p)
    }

    fun renderCoverBitmap(
        ctx: Context,
        s: Student,
        st: Settings,
        showFoldGuide: Boolean = true,
        scale: Float = 2f
    ): Bitmap {
        val bmp = Bitmap.createBitmap((W * scale).toInt(), (H * scale).toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        c.scale(scale, scale)
        cover(ctx, c, s, st, showFoldGuide)
        return bmp
    }

    fun renderMarksBitmap(
        ctx: Context,
        s: Student,
        st: Settings,
        showFoldGuide: Boolean = true,
        scale: Float = 2f
    ): Bitmap {
        val bmp = Bitmap.createBitmap((W * scale).toInt(), (H * scale).toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        c.scale(scale, scale)
        marks(ctx, c, s, st.pdfFont, showFoldGuide)
        return bmp
    }

    fun write(ctx: Context, students: List<Student>, st: Settings, out: OutputStream) {
        val doc = PdfDocument()
        var n = 1
        students.forEach { s ->
            val p1 = doc.startPage(PdfDocument.PageInfo.Builder(W, H, n++).create())
            cover(ctx, p1.canvas, s, st, showFoldGuide = false)
            doc.finishPage(p1)

            val p2 = doc.startPage(PdfDocument.PageInfo.Builder(W, H, n++).create())
            marks(ctx, p2.canvas, s, st.pdfFont, showFoldGuide = false)
            doc.finishPage(p2)
        }
        doc.writeTo(out)
        doc.close()
    }

    // ================= পাতা-১: বাইরের পাশ (বাম = ব্যাক কভার, ডান = ফ্রন্ট কভার) =================
    private fun cover(ctx: Context?, c: Canvas, s: Student, st: Settings, showFoldGuide: Boolean) {
        val f = st.pdfFont
        drawPanelFrames(c, showFoldGuide)

        // ---- বাম অর্ধেক (x: 18..409): ব্যাক কভার ----
        val bx0 = 32f
        val bCw = floatArrayOf(34f, 134f, 65f, 65f, 65f) // 363
        val bHd = listOf(
            "প্রান্তিক",
            "শ্রেণি শিক্ষকের\nসার্বিক মতামত",
            "শ্রেণি শিক্ষকের\nস্বাক্ষর",
            "প্রধান শিক্ষকের\nস্বাক্ষর",
            "অভিভাবকের\nস্বাক্ষর"
        )
        var x = bx0
        bHd.forEachIndexed { i, h ->
            cell(ctx, c, h, x, 36f, bCw[i], 30f, 8f, true, true, f)
            x += bCw[i]
        }
        val tl = listOf("১ম\nপ্রান্তিক", "২য়\nপ্রান্তিক", "৩য়\nপ্রান্তিক")
        for (t in 1..3) {
            val y = 66f + (t - 1) * 66f
            x = bx0
            cell(ctx, c, tl[t - 1], x, y, bCw[0], 66f, 8f, true, false, f); x += bCw[0]
            box(ctx, c, s.x["REM_$t"] ?: "", x, y, bCw[1], 66f, 7.5f, f); x += bCw[1]
            for (i in 2..4) {
                cell(ctx, c, "", x, y, bCw[i], 66f, 9f, false, false, f)
                x += bCw[i]
            }
        }

        val gy = 282f
        val gw = floatArrayOf(102f, 54f, 140f) // 296
        val gx = bx0 + (363f - 296f) / 2f
        x = gx
        listOf("প্রাপ্ত নম্বর", "গ্রেড", "শিখন অর্জন").forEachIndexed { i, h ->
            cell(ctx, c, h, x, gy, gw[i], 22f, 9f, true, true, f)
            x += gw[i]
        }
        val scale = listOf(
            Triple("৮০%-১০০%", "A", "অতি উত্তম"),
            Triple("৬০%-৭৯%", "B", "উত্তম"),
            Triple("৪০%-৫৯%", "C", "সন্তোষজনক"),
            Triple("০%-৩৯%", "D", "সহায়তা প্রয়োজন")
        )
        scale.forEachIndexed { r, (a, g, d) ->
            x = gx
            val y = gy + 22f + r * 23f
            drawCellRow(ctx, c, a, g, d, x, y, gw, f)
        }

        val tn = st.teachers[s.cls] ?: ""
        val tm = st.teacherMobiles[s.cls] ?: ""
        dottedField(ctx, c, "শ্রেণি শিক্ষকের নাম:", tn, 32f, 438f, 240f, 9.5f, f)
        dottedField(ctx, c, "মোবাইল নম্বর:", bn(tm), 246f, 438f, 395f, 9.5f, f)

        val backCx = 18f + 391f / 2f
        drawNctbEmblem(ctx, c, backCx, 488f, 19f, f)
        center(ctx, c, "জাতীয় শিক্ষাক্রম ও পাঠ্যপুস্তক বোর্ড, বাংলাদেশ", backCx, 532f, 10.5f, true, 355f, f)
        center(ctx, c, "প্রাথমিক শিক্ষা স্তর (১ম–৫ম শ্রেণি)", backCx, 549f, 8f, false, 355f, f, 0xFF475569.toInt())

        // ---- ডান অর্ধেক (x: 433..824): ফ্রন্ট কভার ----
        val frontCx = 433f + 391f / 2f // 628.5
        val pillW = 312f
        val pillH = 44f
        val pillX = frontCx - pillW / 2f
        val pillY = 46f
        c.drawRoundRect(RectF(pillX, pillY, pillX + pillW, pillY + pillH), 22f, 22f, darkFill)
        center(ctx, c, "শিখন অগ্রগতি প্রতিবেদন", frontCx, 75f, 20f, true, 290f, f, Color.WHITE)

        drawDpeLogo(ctx, c, frontCx, 102f, 104f, st.dpeLogo, f)

        dottedField(
            ctx,
            c,
            "বিদ্যালয়ের নাম:",
            st.school.ifBlank { "বিদ্যালয়ের নাম" },
            456f,
            236f,
            801f,
            11f,
            f
        )
        dottedField(ctx, c, "শিক্ষাবর্ষ:", bn(st.year), 556f, 266f, 701f, 11f, f)

        val formY0 = 360f
        val formDy = 32f
        dottedField(ctx, c, "শিক্ষার্থীর নাম:", s.name, 456f, formY0, 801f, 10.5f, f)

        dottedField(ctx, c, "শ্রেণি:", CLSN[s.cls - 1], 456f, formY0 + formDy, 622f, 10.5f, f)
        dottedField(ctx, c, "রোল/আইডি:", bn(s.roll.toString()), 632f, formY0 + formDy, 801f, 10.5f, f)

        dottedField(ctx, c, "গ্রাম:", s.village, 456f, formY0 + formDy * 2, 622f, 10.5f, f)
        dottedField(ctx, c, "পোস্ট:", s.post, 632f, formY0 + formDy * 2, 801f, 10.5f, f)

        dottedField(ctx, c, "উপজেলা/থানা:", s.upazila, 456f, formY0 + formDy * 3, 636f, 10.5f, f)
        dottedField(ctx, c, "জেলা:", s.district, 646f, formY0 + formDy * 3, 801f, 10.5f, f)

        dottedField(ctx, c, "অভিভাবকের নাম:", s.guardian, 456f, formY0 + formDy * 4, 656f, 10.5f, f)
        dottedField(ctx, c, "মোবাইল:", bn(s.mobile), 664f, formY0 + formDy * 4, 801f, 10.5f, f)

        center(
            ctx,
            c,
            "(${CLSN[s.cls - 1]} শ্রেণি — শিখন অগ্রগতি প্রতিবেদন)",
            frontCx,
            536f,
            9.5f,
            true,
            340f,
            f,
            0xFF334155.toInt()
        )
    }

    private fun drawCellRow(
        ctx: Context?,
        c: Canvas,
        a: String,
        g: String,
        d: String,
        x: Float,
        y: Float,
        gw: FloatArray,
        f: String
    ) {
        cell(ctx, c, a, x, y, gw[0], 23f, 9f, false, false, f)
        cell(ctx, c, g, x + gw[0], y, gw[1], 23f, 10f, true, false, f)
        cell(ctx, c, d, x + gw[0] + gw[1], y, gw[2], 23f, 9f, false, false, f)
    }

    // ================= পাতা-২: ভেতরের পাশ (বাম = ১ম+২য় ও গুণাবলি, ডান = ৩য়+চূড়ান্ত ও উপস্থিতি) =================
    private fun marks(ctx: Context?, c: Canvas, s: Student, f: String, showFoldGuide: Boolean) {
        drawPanelFrames(c, showFoldGuide)

        val subs = s.subs()
        val anyData = (1..3).any { s.hasTerm(it) }
        val totMax = subs.sumOf { it.max }

        // উভয় ভেতরের পাতার শীর্ষে সংক্ষিপ্ত পরিচিতি
        val topLeftP = tp(ctx, 8.5f, true, f, 0xFF334155.toInt())
        val topRightP = tp(ctx, 8.5f, true, f, 0xFF334155.toInt()).apply { textAlign = Paint.Align.RIGHT }
        c.drawText("শিক্ষার্থীর নাম: ${s.name}", 28f, 38f, topLeftP)
        c.drawText("শ্রেণি: ${CLSN[s.cls - 1]}", 399f, 38f, topRightP)

        c.drawText("রোল/আইডি: ${bn(s.roll.toString())}", 443f, 38f, topLeftP)
        c.drawText("ধর্ম: ${s.religion}", 814f, 38f, topRightP)

        // ---- বাম ভেতরের অর্ধেক (x: 28..399) ----
        val lx = 28f
        val sw = 125f
        val lcw = 41f // 125 + 6*41 = 371
        val ty0 = 46f
        val hdr1H = 18f
        val hdr2H = 26f
        val hdrH = hdr1H + hdr2H // 44
        val rh = 27.5f

        cell(ctx, c, "বিষয় (পূর্ণমান)", lx, ty0, sw, hdrH, 8.5f, true, true, f)
        cell(ctx, c, "১ম প্রান্তিক", lx + sw, ty0, 3 * lcw, hdr1H, 8.5f, true, true, f)
        cell(ctx, c, "২য় প্রান্তিক", lx + sw + 3 * lcw, ty0, 3 * lcw, hdr1H, 8.5f, true, true, f)

        val termSubHdr = listOf("ধারাবাহিক\nনম্বর", "সামষ্টিক\nনম্বর", "মোট\nনম্বর")
        for (t in 0..1) {
            for (i in 0..2) {
                cell(ctx, c, termSubHdr[i], lx + sw + (t * 3 + i) * lcw, ty0 + hdr1H, lcw, hdr2H, 7f, true, true, f)
            }
        }

        val rowsStartY = ty0 + hdrH
        subs.forEachIndexed { i, sub ->
            val ry = rowsStartY + i * rh
            cell(ctx, c, "${sub.bn} (${bn(sub.max.toString())})", lx, ry, sw, rh, 8f, false, false, f)
            for (t in 1..2) {
                val has = s.hasSub(sub, t)
                val b = lx + sw + (t - 1) * 3 * lcw
                val contTxt = if (sub.key == "ART") "......" else if (has) fmt(s.cont(sub, t)) else ""
                val wrTxt = if (sub.written == 0) "......" else if (has) fmt(s.wr(sub, t)) else ""
                val totTxt = if (has) fmt(s.total(sub, t)) else ""

                cell(ctx, c, contTxt, b, ry, lcw, rh, 8f, false, false, f)
                cell(ctx, c, wrTxt, b + lcw, ry, lcw, rh, 8f, false, false, f)
                cell(ctx, c, totTxt, b + 2 * lcw, ry, lcw, rh, 8.5f, true, false, f)
            }
        }

        // বাম মোট সারি
        val ty = rowsStartY + subs.size * rh
        val totalH = 26f
        cell(ctx, c, "সর্বমোট নম্বর (${bn(totMax.toString())})", lx, ty, sw, totalH, 8f, true, true, f)
        for (t in 1..2) {
            val b = lx + sw + (t - 1) * 3 * lcw
            val has = s.hasTerm(t)
            val label = if (t == 1) "১ম প্রান্তিকে মোট" else "২য় প্রান্তিকে মোট"
            cell(ctx, c, label, b, ty, 2 * lcw, totalH, 7.5f, true, true, f)
            cell(
                ctx,
                c,
                if (has) fmt(subs.sumOf { s.total(it, t) }) else "",
                b + 2 * lcw,
                ty,
                lcw,
                totalH,
                8.5f,
                true,
                true,
                f
            )
        }

        // ---- নিচে বামে: ব্যক্তিগত ও সামাজিক গুণাবলি (width = 371) ----
        val by = ty + totalH + 14f
        val qw = floatArrayOf(52f, 187f, 44f, 44f, 44f)
        val qTopH = 16f
        val qSub1H = 13f
        val qSub2H = 19f
        val qHdrH = qTopH + qSub1H + qSub2H // 48
        val qRowH = 42f

        cell(ctx, c, "ব্যক্তিগত ও সামাজিক গুণাবলি", lx, by, 177f, qTopH, 7.5f, true, true, f)
        cell(ctx, c, "অবস্থান", lx + 177f, by, 62f, qTopH, 7.5f, true, true, f)
        cell(ctx, c, "উত্তম", lx + 239f, by, qw[2], qTopH, 7.2f, true, true, f)
        cell(ctx, c, "সন্তোষজনক", lx + 239f + qw[2], by, qw[3], qTopH, 6.8f, true, true, f)
        cell(ctx, c, "উন্নতি প্রয়োজন", lx + 239f + qw[2] + qw[3], by, qw[4], qTopH, 6.4f, true, true, f)

        cell(ctx, c, "গুণাবলি", lx, by + qTopH, qw[0], qSub1H + qSub2H, 8f, true, true, f)
        cell(ctx, c, "বর্ণনা", lx + qw[0], by + qTopH, qw[1], qSub1H + qSub2H, 8f, true, true, f)
        val kkg = listOf("ক", "খ", "গ")
        val termShort = listOf("১ম প্রান্তিক", "২য় প্রান্তিক", "৩য় প্রান্তিক")
        for (i in 0..2) {
            val cx = lx + qw[0] + qw[1] + i * qw[2 + i]
            cell(ctx, c, kkg[i], cx, by + qTopH, qw[2 + i], qSub1H, 7.5f, true, true, f)
            cell(ctx, c, termShort[i], cx, by + qTopH + qSub1H, qw[2 + i], qSub2H, 6.8f, true, true, f)
        }

        val traits = listOf(
            Triple("সক্রিয়তা", "ACT", "শিক্ষকের নির্দেশনামতো পাঠ্যপুস্তকের কাজ সম্পন্ন করা; বাড়ির কাজ সময়মতো করা; জোড়ায় বা দলগত কাজে অংশ নেওয়া; প্রশ্নোত্তরে অংশগ্রহণ ইত্যাদি"),
            Triple("শৃঙ্খলা", "DISC", "বিদ্যালয়ে নিয়মিত উপস্থিত থাকা; বিদ্যালয়ের নিয়ম-শৃঙ্খলা মেনে চলা; পরিষ্কার-পরিচ্ছন্ন থাকা ইত্যাদি"),
            Triple("আচরণ", "COND", "শিক্ষক ও বড়দের সম্মান করা; ছোটদের স্নেহ করা; সহযোগিতা করা; ভুল হলে তা স্বীকার করা; অন্যকে কষ্ট না দেওয়া ইত্যাদি")
        )
        traits.forEachIndexed { r, (lab, k, d) ->
            val yy = by + qHdrH + r * qRowH
            var cx = lx
            cell(ctx, c, lab, cx, yy, qw[0], qRowH, 8.5f, true, false, f); cx += qw[0]
            box(ctx, c, d, cx, yy, qw[1], qRowH, 6.8f, f); cx += qw[1]
            for (t in 1..3) {
                cell(ctx, c, s.x["${k}_$t"] ?: "", cx, yy, qw[1 + t], qRowH, 10f, true, false, f)
                cx += qw[1 + t]
            }
        }
        val lg = tp(ctx, 7.5f, false, f, 0xFF334155.toInt())
        c.drawText("ক = উত্তম,  খ = সন্তোষজনক,  গ = উন্নতি প্রয়োজন", lx, by + qHdrH + 3 * qRowH + 11f, lg)

        // ---- ডান ভেতরের অর্ধেক (x: 443..814) ----
        val rx = 443f
        val rW = 371f
        val rcw = rW / 6f

        cell(ctx, c, "৩য় প্রান্তিক", rx, ty0, 3 * rcw, hdr1H, 8.5f, true, true, f)
        cell(ctx, c, "চূড়ান্ত ফলাফল", rx + 3 * rcw, ty0, 3 * rcw, hdr1H, 8.5f, true, true, f)

        val finalSubHdr = listOf("প্রান্তিকের\nগড় নম্বর", "বিষয়\nগ্রেড", "সমন্বিত\nগ্রেড")
        for (i in 0..2) {
            cell(ctx, c, termSubHdr[i], rx + i * rcw, ty0 + hdr1H, rcw, hdr2H, 7f, true, true, f)
            cell(ctx, c, finalSubHdr[i], rx + (3 + i) * rcw, ty0 + hdr1H, rcw, hdr2H, 7f, true, true, f)
        }

        subs.forEachIndexed { i, sub ->
            val ry = rowsStartY + i * rh
            val has3 = s.hasSub(sub, 3)
            val cont3 = if (sub.key == "ART") "......" else if (has3) fmt(s.cont(sub, 3)) else ""
            val wr3 = if (sub.written == 0) "......" else if (has3) fmt(s.wr(sub, 3)) else ""
            val tot3 = if (has3) fmt(s.total(sub, 3)) else ""

            cell(ctx, c, cont3, rx, ry, rcw, rh, 8f, false, false, f)
            cell(ctx, c, wr3, rx + rcw, ry, rcw, rh, 8f, false, false, f)
            cell(ctx, c, tot3, rx + 2 * rcw, ry, rcw, rh, 8.5f, true, false, f)

            val a = s.avg(sub)
            cell(ctx, c, if (anyData) fmt(a) else "", rx + 3 * rcw, ry, rcw, rh, 8.5f, true, false, f)
            cell(ctx, c, if (anyData) gradeOf(a / sub.max * 100) else "", rx + 4 * rcw, ry, rcw, rh, 9.5f, true, false, f)
        }

        val pct = s.overallPct()
        cell(
            ctx,
            c,
            if (anyData) "${gradeOf(pct)}\n${fmt(pct)}%" else "",
            rx + 5 * rcw,
            rowsStartY,
            rcw,
            rh * subs.size,
            11.5f,
            true,
            false,
            f
        )

        val has3Term = s.hasTerm(3)
        cell(ctx, c, "৩য় প্রান্তিকে মোট", rx, ty, 2 * rcw, totalH, 7.5f, true, true, f)
        cell(
            ctx,
            c,
            if (has3Term) fmt(subs.sumOf { s.total(it, 3) }) else "",
            rx + 2 * rcw,
            ty,
            rcw,
            totalH,
            8.5f,
            true,
            true,
            f
        )
        cell(ctx, c, "প্রান্তিকের গড় মোট", rx + 3 * rcw, ty, rcw, totalH, 7f, true, true, f)
        cell(
            ctx,
            c,
            if (anyData) fmt(subs.sumOf { s.avg(it) }) else "",
            rx + 4 * rcw,
            ty,
            rcw,
            totalH,
            8.5f,
            true,
            true,
            f
        )
        cell(ctx, c, "", rx + 5 * rcw, ty, rcw, totalH, 8f, false, true, f)

        // ---- নিচে ডানে: উপস্থিতি (গুণাবলির সাথে সারিবদ্ধ) ----
        val aw = floatArrayOf(125f, 82f, 82f, 82f)
        val attTopH = 20f
        val attSubH = 28f
        cell(ctx, c, "উপস্থিতি", rx, by, rW, attTopH, 9f, true, true, f)
        var ax = rx
        listOf("বিবরণ", "১ম প্রান্তিক", "২য় প্রান্তিক", "৩য় প্রান্তিক").forEachIndexed { i, h ->
            cell(ctx, c, h, ax, by + attTopH, aw[i], attSubH, 8f, true, true, f)
            ax += aw[i]
        }

        val attLabels = listOf("মোট কার্যদিবস", "শিক্ষার্থীর উপস্থিতি", "উপস্থিতির হার")
        attLabels.forEachIndexed { r, lab ->
            val yy = by + qHdrH + r * qRowH
            ax = rx
            cell(ctx, c, lab, ax, yy, aw[0], qRowH, 8.5f, true, false, f)
            ax += aw[0]
            for (t in 1..3) {
                val d = s.x["DAYS_$t"]?.let { fromBn(it).toDoubleOrNull() }
                val p = s.x["PRES_$t"]?.let { fromBn(it).toDoubleOrNull() }
                val txt = when (r) {
                    0 -> d?.let { fmt(it) } ?: ""
                    1 -> p?.let { fmt(it) } ?: ""
                    else -> if (d != null && p != null && d > 0) "${fmt(p / d * 100)}%" else ""
                }
                cell(ctx, c, txt, ax, yy, aw[t], qRowH, 9.5f, r == 2, false, f)
                ax += aw[t]
            }
        }
        c.drawText("উপস্থিতির হার = (শিক্ষার্থীর উপস্থিতি ÷ মোট কার্যদিবস) × ১০০%", rx, by + qHdrH + 3 * qRowH + 11f, lg)
    }
}
