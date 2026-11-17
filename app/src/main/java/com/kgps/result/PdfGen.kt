package com.kgps.result

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * প্রতি শিক্ষার্থীর জন্য ২টি A4 Landscape পাতা:
 *  পাতা-১ (বাইরের পাশ): বাম অর্ধেক = ব্যাক কভার (মতামত/স্বাক্ষর/গ্রেড স্কেল), ডান অর্ধেক = ফ্রন্ট কভার (শিক্ষার্থীর তথ্য)
 *  পাতা-২ (ভেতরের পাশ): মূল নম্বর পাতা (নম্বর, গুণাবলি, উপস্থিতি)
 * মাঝখানে ভাঁজ করলে A5 বইয়ের মতো হবে।
 */
object PdfGen {
    const val W = 842
    const val H = 595
    private val stroke = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 0.8f }
    private val gray = Paint().apply { color = 0xFFEAEAEA.toInt(); style = Paint.Style.FILL }
    private val foldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF94A3B8.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1f
        pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
    }

    private val fontCache = ConcurrentHashMap<String, Typeface>()

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

    private fun tp(ctx: Context?, size: Float, bold: Boolean = false, fontId: String = "noto_serif") = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size; color = Color.BLACK
        typeface = resolveTypeface(ctx, fontId, bold)
    }

    private fun wrap(text: String, p: Paint, w: Float): List<String> {
        val out = mutableListOf<String>()
        text.split("\n").forEach { para ->
            var cur = ""
            para.split(" ").forEach { word ->
                val t = if (cur.isEmpty()) word else "$cur $word"
                if (p.measureText(t) <= w || cur.isEmpty()) cur = t else { out.add(cur); cur = word }
            }
            out.add(cur)
        }
        return out
    }

    private fun cell(ctx: Context?, c: Canvas, t: String, l: Float, top: Float, w: Float, h: Float,
                     size: Float = 9f, bold: Boolean = false, fill: Boolean = false, fontId: String = "noto_serif") {
        if (fill) c.drawRect(l, top, l + w, top + h, gray)
        c.drawRect(l, top, l + w, top + h, stroke)
        if (t.isEmpty()) return
        val p = tp(ctx, size, bold, fontId); p.textAlign = Paint.Align.CENTER
        val lines = t.split("\n")
        while (lines.maxOf { p.measureText(it) } > w - 4 && p.textSize > 5f) p.textSize -= 0.5f
        val lh = p.textSize * 1.25f
        var y = top + (h - lh * lines.size) / 2 + p.textSize * 0.95f
        lines.forEach { c.drawText(it, l + w / 2, y, p); y += lh }
    }

    private fun box(ctx: Context?, c: Canvas, t: String, l: Float, top: Float, w: Float, h: Float, size: Float = 8f, fontId: String = "noto_serif") {
        c.drawRect(l, top, l + w, top + h, stroke)
        val p = tp(ctx, size, false, fontId)
        var y = top + size + 3
        wrap(t, p, w - 8).forEach { if (y < top + h) c.drawText(it, l + 4, y, p); y += size * 1.3f }
    }

    private fun center(ctx: Context?, c: Canvas, t: String, cx: Float, y: Float, size: Float, bold: Boolean = false, maxW: Float = 340f, fontId: String = "noto_serif") {
        val p = tp(ctx, size, bold, fontId); p.textAlign = Paint.Align.CENTER
        while (p.measureText(t) > maxW && p.textSize > 6f) p.textSize -= 0.5f
        c.drawText(t, cx, y, p)
    }

    fun renderCoverBitmap(ctx: Context, s: Student, st: Settings, showFoldGuide: Boolean = true, scale: Float = 2f): Bitmap {
        val bmp = Bitmap.createBitmap((W * scale).toInt(), (H * scale).toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        c.scale(scale, scale)
        cover(ctx, c, s, st, showFoldGuide)
        return bmp
    }

    fun renderMarksBitmap(ctx: Context, s: Student, st: Settings, scale: Float = 2f): Bitmap {
        val bmp = Bitmap.createBitmap((W * scale).toInt(), (H * scale).toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        c.scale(scale, scale)
        marks(ctx, c, s, st.pdfFont)
        return bmp
    }

    fun write(ctx: Context, students: List<Student>, st: Settings, out: OutputStream) {
        val doc = PdfDocument()
        var n = 1
        students.forEach { s ->
            val p1 = doc.startPage(PdfDocument.PageInfo.Builder(W, H, n++).create())
            cover(ctx, p1.canvas, s, st, showFoldGuide = false); doc.finishPage(p1)
            val p2 = doc.startPage(PdfDocument.PageInfo.Builder(W, H, n++).create())
            marks(ctx, p2.canvas, s, st.pdfFont); doc.finishPage(p2)
        }
        doc.writeTo(out); doc.close()
    }

    // ================= বাইরের পাশ =================
    private fun cover(ctx: Context?, c: Canvas, s: Student, st: Settings, showFoldGuide: Boolean) {
        val f = st.pdfFont
        if (showFoldGuide) {
            c.drawLine(W / 2f, 10f, W / 2f, H - 10f, foldPaint)
        }

        // ---- বাম অর্ধেক: ব্যাক কভার ----
        val x0 = 22f
        center(ctx, c, "শ্রেণি শিক্ষকের সার্বিক মতামত ও স্বাক্ষর", x0 + 377f / 2, 32f, 12f, true, 370f, f)
        val cw = floatArrayOf(44f, 125f, 70f, 70f, 68f)
        val hd = listOf("প্রান্তিক", "শ্রেণি শিক্ষকের\nসার্বিক মতামত", "শ্রেণি শিক্ষকের\nস্বাক্ষর", "প্রধান শিক্ষকের\nস্বাক্ষর", "অভিভাবকের\nস্বাক্ষর")
        var x = x0
        hd.forEachIndexed { i, h -> cell(ctx, c, h, x, 42f, cw[i], 30f, 8.5f, true, true, f); x += cw[i] }
        val tl = listOf("১ম\nপ্রান্তিক", "২য়\nপ্রান্তিক", "৩য়\nপ্রান্তিক")
        for (t in 1..3) {
            val y = 72f + (t - 1) * 78f
            x = x0
            cell(ctx, c, tl[t - 1], x, y, cw[0], 78f, 8.5f, true, false, f); x += cw[0]
            box(ctx, c, s.x["REM_$t"] ?: "", x, y, cw[1], 78f, 8f, f); x += cw[1]
            for (i in 2..4) { cell(ctx, c, "", x, y, cw[i], 78f, 9f, false, false, f); x += cw[i] }
        }
        val gy = 322f
        val gw = floatArrayOf(120f, 70f, 187f)
        x = x0
        listOf("প্রাপ্ত নম্বর", "গ্রেড", "শিখন অর্জন").forEachIndexed { i, h -> cell(ctx, c, h, x, gy, gw[i], 24f, 9.5f, true, true, f); x += gw[i] }
        val scale = listOf(
            Triple("৮০%-১০০%", "A", "অতি উত্তম"), Triple("৬০%-৭৯%", "B", "উত্তম"),
            Triple("৪০%-৫৯%", "C", "সন্তোষজনক"), Triple("০%-৩৯%", "D", "সহায়তা প্রয়োজন")
        )
        scale.forEachIndexed { r, (a, g, d) ->
            x = x0; val y = gy + 24f + r * 26f
            cell(ctx, c, a, x, y, gw[0], 26f, 10f, false, false, f); x += gw[0]
            cell(ctx, c, g, x, y, gw[1], 26f, 11f, true, false, f); x += gw[1]
            cell(ctx, c, d, x, y, gw[2], 26f, 10f, false, false, f)
        }
        val tn = st.teachers[s.cls] ?: ""; val tm = st.teacherMobiles[s.cls] ?: ""
        val lp = tp(ctx, 11f, false, f)
        c.drawText("শ্রেণি শিক্ষকের নাম: $tn", x0, 480f, lp)
        c.drawText("মোবাইল নম্বর: ${bn(tm)}", x0, 504f, lp)
        center(ctx, c, "জাতীয় শিক্ষাক্রম ও পাঠ্যপুস্তক বোর্ড, বাংলাদেশ", x0 + 377f / 2, 560f, 11f, true, 370f, f)

        // ---- ডান অর্ধেক: ফ্রন্ট কভার ----
        val l = 441f; val w = 379f
        val thick = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 3f }
        c.drawRect(l, 22f, l + w, 573f, thick)
        c.drawRect(l + 7, 29f, l + w - 7, 566f, stroke)
        val cx = l + w / 2
        center(ctx, c, "শিখন অগ্রগতি প্রতিবেদন", cx, 105f, 26f, true, 340f, f)
        center(ctx, c, "শিক্ষাবর্ষ: ${bn(st.year)}", cx, 135f, 13f, false, 340f, f)
        center(ctx, c, st.school.ifBlank { "বিদ্যালয়ের নাম" }, cx, 175f, 17f, true, 345f, f)
        center(ctx, c, "(${CLSN[s.cls - 1]} শ্রেণি)", cx, 202f, 13f, false, 340f, f)
        val rows = listOf(
            "শিক্ষার্থীর নাম" to s.name, "শ্রেণি" to CLSN[s.cls - 1], "রোল" to bn(s.roll.toString()),
            "অভিভাবকের নাম" to s.guardian, "গ্রাম" to s.village, "উপজেলা" to s.upazila,
            "জেলা" to s.district, "মোবাইল নম্বর" to bn(s.mobile)
        )
        var y = 262f
        rows.forEach { (lab, v) ->
            val lpnt = tp(ctx, 12f, false, f); val vp = tp(ctx, 12f, true, f)
            val tx = l + 22
            c.drawText("$lab :", tx, y, lpnt)
            val vx = tx + 112f
            c.drawText(v, vx, y - 1, vp)
            c.drawLine(vx, y + 4, l + w - 22, y + 4, stroke)
            y += 36f
        }
    }

    // ================= ভেতরের পাশ =================
    private fun marks(ctx: Context?, c: Canvas, s: Student, f: String) {
        val subs = s.subs(); val L = 24f
        center(ctx, c, "শিখন অগ্রগতি প্রতিবেদন — ${s.name}   |   শ্রেণি: ${CLSN[s.cls - 1]}   |   রোল: ${bn(s.roll.toString())}", W / 2f, 28f, 13f, true, 790f, f)
        var y = 40f
        val sw = 170f; val cw = (W - 2 * L - sw) / 12f
        cell(ctx, c, "বিষয় (পূর্ণমান)", L, y, sw, 50f, 9.5f, true, true, f)
        val ord = listOf("১ম", "২য়", "৩য়")
        for (t in 0..2) cell(ctx, c, "${ord[t]} প্রান্তিক", L + sw + t * 3 * cw, y, 3 * cw, 22f, 9f, true, true, f)
        cell(ctx, c, "চূড়ান্ত ফলাফল", L + sw + 9 * cw, y, 3 * cw, 22f, 9f, true, true, f)
        val sub1 = listOf("ধারাবাহিক\nনম্বর", "সামষ্টিক\nনম্বর", "মোট\nনম্বর")
        val sub2 = listOf("প্রান্তিকের\nগড় নম্বর", "বিষয়\nগ্রেড", "সমন্বিত\nগ্রেড")
        for (t in 0..2) for (i in 0..2) cell(ctx, c, sub1[i], L + sw + (t * 3 + i) * cw, y + 22f, cw, 28f, 7.5f, true, true, f)
        for (i in 0..2) cell(ctx, c, sub2[i], L + sw + (9 + i) * cw, y + 22f, cw, 28f, 7.5f, true, true, f)
        y += 50f
        val rh = 32f
        val anyData = (1..3).any { s.hasTerm(it) }
        subs.forEachIndexed { i, sub ->
            val ry = y + i * rh
            cell(ctx, c, "${sub.bn} (${bn(sub.max.toString())})", L, ry, sw, rh, 9f, false, false, f)
            for (t in 1..3) {
                val has = s.hasSub(sub, t); val b = L + sw + (t - 1) * 3 * cw
                cell(ctx, c, if (has) fmt(s.cont(sub, t)) else "", b, ry, cw, rh, 9f, false, false, f)
                cell(ctx, c, if (sub.written == 0) "--" else if (has) fmt(s.wr(sub, t)) else "", b + cw, ry, cw, rh, 9f, false, false, f)
                cell(ctx, c, if (has) fmt(s.total(sub, t)) else "", b + 2 * cw, ry, cw, rh, 9f, true, false, f)
            }
            val a = s.avg(sub)
            cell(ctx, c, if (anyData) fmt(a) else "", L + sw + 9 * cw, ry, cw, rh, 9f, true, false, f)
            cell(ctx, c, if (anyData) gradeOf(a / sub.max * 100) else "", L + sw + 10 * cw, ry, cw, rh, 10f, true, false, f)
        }
        // সমন্বিত গ্রেড (মার্জড ঘর)
        val pct = s.overallPct()
        cell(ctx, c, if (anyData) "${gradeOf(pct)}\n${fmt(pct)}%" else "", L + sw + 11 * cw, y, cw, rh * subs.size, 12f, true, false, f)
        // মোট সারি
        val ty = y + subs.size * rh
        val totMax = subs.sumOf { it.max }
        cell(ctx, c, "সর্বমোট নম্বর (${bn(totMax.toString())})", L, ty, sw, rh, 9f, true, true, f)
        for (t in 1..3) {
            val b = L + sw + (t - 1) * 3 * cw; val has = s.hasTerm(t)
            cell(ctx, c, if (has) fmt(subs.sumOf { s.cont(it, t) }) else "", b, ty, cw, rh, 9f, false, true, f)
            cell(ctx, c, if (has) fmt(subs.sumOf { s.wr(it, t) }) else "", b + cw, ty, cw, rh, 9f, false, true, f)
            cell(ctx, c, if (has) fmt(subs.sumOf { s.total(it, t) }) else "", b + 2 * cw, ty, cw, rh, 9f, true, true, f)
        }
        cell(ctx, c, if (anyData) fmt(subs.sumOf { s.avg(it) }) else "", L + sw + 9 * cw, ty, cw, rh, 9f, true, true, f)
        cell(ctx, c, "", L + sw + 10 * cw, ty, cw, rh, 9f, false, true, f)
        cell(ctx, c, "", L + sw + 11 * cw, ty, cw, rh, 9f, false, true, f)

        // ---- ব্যক্তিগত ও সামাজিক গুণাবলি ----
        var by = ty + rh + 18f
        val qw = floatArrayOf(70f, 270f, 46f, 46f, 46f)  // মোট 478
        var x = L
        listOf("গুণাবলি", "বর্ণনা", "১ম প্রান্তিক", "২য় প্রান্তিক", "৩য় প্রান্তিক").forEachIndexed { i, h ->
            cell(ctx, c, h, x, by, qw[i], 24f, 8.5f, true, true, f); x += qw[i]
        }
        val q = listOf(
            Triple("সক্রিয়তা", "ACT", "শিক্ষকের নির্দেশনামতো পাঠ্যপুস্তকের কাজ সম্পন্ন করা; বাড়ির কাজ সময়মতো করা; জোড়ায়/দলগত কাজে অংশগ্রহণ; প্রশ্নোত্তরে অংশ নেওয়া ইত্যাদি"),
            Triple("শৃঙ্খলা", "DISC", "বিদ্যালয়ে নিয়মিত উপস্থিত থাকা; বিদ্যালয়ের নিয়ম-শৃঙ্খলা মেনে চলা; পরিষ্কার-পরিচ্ছন্ন থাকা ইত্যাদি"),
            Triple("আচরণ", "COND", "শিক্ষক/বড়দের সম্মান করা; ছোটদের স্নেহ করা; সহযোগিতা করা; ভুল হলে স্বীকার করা; অন্যকে কষ্ট না দেওয়া ইত্যাদি")
        )
        q.forEachIndexed { r, (lab, k, d) ->
            val yy = by + 24f + r * 38f; x = L
            cell(ctx, c, lab, x, yy, qw[0], 38f, 9f, true, false, f); x += qw[0]
            box(ctx, c, d, x, yy, qw[1], 38f, 7f, f); x += qw[1]
            for (t in 1..3) { cell(ctx, c, s.x["${k}_$t"] ?: "", x, yy, qw[1 + t], 38f, 11f, true, false, f); x += qw[1 + t] }
        }
        val lg = tp(ctx, 8f, false, f)
        c.drawText("ক = উত্তম,  খ = সন্তোষজনক,  গ = উন্নতি প্রয়োজন", L, by + 24f + 3 * 38f + 12f, lg)

        // ---- উপস্থিতি ----
        val ax = L + 478f + 16f
        val aw = floatArrayOf(108f, 64f, 64f, 64f)
        x = ax
        listOf("উপস্থিতি", "১ম প্রান্তিক", "২য় প্রান্তিক", "৩য় প্রান্তিক").forEachIndexed { i, h ->
            cell(ctx, c, h, x, by, aw[i], 24f, 8.5f, true, true, f); x += aw[i]
        }
        val labels = listOf("মোট কার্যদিবস", "শিক্ষার্থীর উপস্থিতি", "উপস্থিতির হার")
        labels.forEachIndexed { r, lab ->
            val yy = by + 24f + r * 38f; x = ax
            cell(ctx, c, lab, x, yy, aw[0], 38f, 9f, true, false, f); x += aw[0]
            for (t in 1..3) {
                val d = s.x["DAYS_$t"]?.let { fromBn(it).toDoubleOrNull() }
                val p = s.x["PRES_$t"]?.let { fromBn(it).toDoubleOrNull() }
                val txt = when (r) {
                    0 -> d?.let { fmt(it) } ?: ""
                    1 -> p?.let { fmt(it) } ?: ""
                    else -> if (d != null && p != null && d > 0) "${fmt(p / d * 100)}%" else ""
                }
                cell(ctx, c, txt, x, yy, aw[t], 38f, 10f, false, false, f); x += aw[t]
            }
        }
    }
}
