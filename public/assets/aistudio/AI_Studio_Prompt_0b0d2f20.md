# AI Studio-তে পেস্ট করার প্রম্পট

আমার Android অ্যাপে (Kotlin + Jetpack Compose + Material3) নিচের ৪টি ফাইল যোগ করো। প্রতিটি ফাইল নির্দিষ্ট পাথে ঠিক যেমন দেওয়া আছে তেমন বসাও (কোড নিজে থেকে বদলাবে না)।

নিয়ম:
1. আমার প্রজেক্টের বর্তমান package নাম যদি `com.akibul.shikhon` না হয়, তাহলে ৪টি ফাইলের প্রথম লাইনের `package` এবং ফোল্ডারের পাথ বর্তমান package অনুযায়ী মিলিয়ে নাও।
2. আগের MainActivity.kt থাকলে সেটি এই নতুন MainActivity.kt দিয়ে বদলে দাও। AndroidManifest-এর launcher activity যেন `.MainActivity` থাকে।
3. app/build.gradle.kts-এ নিশ্চিত করো: Compose চালু (buildFeatures { compose = true }), এবং dependencies-এ `androidx.activity:activity-compose`, Compose BOM, `androidx.compose.material3:material3`, `androidx.compose.ui:ui` আছে। minSdk 24+ রাখো। অতিরিক্ত কোনো লাইব্রেরি লাগবে না (JSON = org.json, PDF = android.graphics.pdf.PdfDocument, ফাইল সেভ = Storage Access Framework)।
4. কোনো permission যোগ করার দরকার নেই।
5. বিল্ড এরর হলে শুধু প্রয়োজনীয় ন্যূনতম ঠিক করো, ফিচার বদলাবে না।

অ্যাপের কাজ: ১ম–৫ম শ্রেণির শিক্ষার্থীর তথ্য ও নম্বর ইনপুট নিয়ে প্রতি শিক্ষার্থীর "শিখন অগ্রগতি প্রতিবেদন" PDF তৈরি (A4 Landscape, প্রতি শিক্ষার্থীর ২ পাতা: বাইরের পাশে বাম=ব্যাক কভার, ডান=ফ্রন্ট কভার; ভেতরের পাশে মূল নম্বর পাতা। মাঝখানে ভাঁজ করলে বই হবে)। CSV ইমপোর্ট/এক্সপোর্ট এবং JSON ব্যাকআপ/রিস্টোর আছে।

## ফাইল পাথ: app/src/main/java/com/akibul/shikhon/Models.kt
```kotlin
package com.akibul.shikhon

data class Student(
    val id: String,
    val cls: Int,
    val roll: Int,
    val name: String,
    val guardian: String = "",
    val village: String = "",
    val upazila: String = "",
    val district: String = "",
    val mobile: String = "",
    val religion: String = "ইসলাম",
    val m: Map<String, Double> = emptyMap(),   // নম্বর: BAN_1_TB, BAN_1_WR, ART_2_TOTAL ...
    val x: Map<String, String> = emptyMap()    // DAYS_1, PRES_1, ACT_1, DISC_1, COND_1, REM_1 ...
)

data class Settings(
    val school: String = "",
    val year: String = "২০২৬",
    val teachers: Map<Int, String> = emptyMap(),
    val teacherMobiles: Map<Int, String> = emptyMap()
)

data class Comp(val key: String, val bn: String, val max: Int)
data class Sub(val key: String, val bn: String, val max: Int, val comps: List<Comp>, val written: Int)

val RELIGIONS = listOf("ইসলাম", "হিন্দু", "বৌদ্ধ", "খ্রীষ্ট")
val CLSN = listOf("প্রথম", "দ্বিতীয়", "তৃতীয়", "চতুর্থ", "পঞ্চম")

private fun comps(tb: Int, ct: Int) = listOf(
    Comp("TB", "পাঠ্যপুস্তক/শিক্ষক সহায়িকার কাজ", tb),
    Comp("ACT", "শ্রেণিকাজে সক্রিয়তা", 5),
    Comp("LANG", "ভাষাদক্ষতা/বোধগম্যতা", 5),
    Comp("CT", "ক্লাস টেস্ট", ct)
)

fun subjectsFor(cls: Int, religion: String): List<Sub> {
    val low = cls <= 2
    fun full(k: String, n: String) =
        if (low) Sub(k, n, 100, comps(20, 20), 50) else Sub(k, n, 100, comps(10, 10), 70)
    fun half(k: String, n: String) = Sub(k, n, 50, comps(20, 20), 0)
    val art = Sub("ART", "শিল্পকলা", 50,
        listOf(Comp("TOTAL", "মোট নম্বর (চারু ও কারুকলা, সংগীত, নৃত্য, নাট্যকলা)", 50)), 0)
    val rel = "ধর্মশিক্ষা ($religion)"
    return if (low) listOf(
        full("BAN", "বাংলা"), full("ENG", "ইংরেজি"), full("MATH", "গণিত"),
        half("SOC", "সামাজিক বিজ্ঞান ও বিজ্ঞান (সমন্বিত)"), half("REL", rel),
        half("PE", "শারীরিক ও মানসিক স্বাস্থ্য শিক্ষা"), art
    ) else listOf(
        full("BAN", "বাংলা"), full("ENG", "ইংরেজি"), full("MATH", "গণিত"),
        full("SCI", "বিজ্ঞান"), full("BGW", "বাংলাদেশ ও বিশ্বপরিচয়"), full("REL", rel),
        half("PE", "শারীরিক ও মানসিক স্বাস্থ্য শিক্ষা"), art
    )
}

// ---------- হিসাব ----------
fun mk(sub: String, t: Int, c: String) = "${sub}_${t}_$c"

fun bn(s: String): String = s.map { if (it in '0'..'9') '০' + (it - '0') else it }.joinToString("")
fun fromBn(s: String): String = s.map { if (it in '০'..'৯') '0' + (it - '০') else it }.joinToString("")
fun fmtEn(d: Double): String { val r = Math.round(d * 10) / 10.0; return if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString() }
fun fmt(d: Double): String = bn(fmtEn(d))

fun Student.subs() = subjectsFor(cls, religion)
fun Student.hasTerm(t: Int) = m.keys.any { it.contains("_${t}_") }
fun Student.hasSub(s: Sub, t: Int) = m.keys.any { it.startsWith("${s.key}_${t}_") }
fun Student.cont(s: Sub, t: Int) = s.comps.sumOf { m[mk(s.key, t, it.key)] ?: 0.0 }
fun Student.wr(s: Sub, t: Int) = if (s.written > 0) (m[mk(s.key, t, "WR")] ?: 0.0) else 0.0
fun Student.total(s: Sub, t: Int) = cont(s, t) + wr(s, t)
fun Student.avg(s: Sub) = (1..3).sumOf { total(s, it) } / 3.0
fun gradeOf(p: Double) = when { p >= 80 -> "A"; p >= 60 -> "B"; p >= 40 -> "C"; else -> "D" }
fun Student.overallPct(): Double {
    val ss = subs()
    return ss.sumOf { avg(it) } / ss.sumOf { it.max } * 100
}
```

## ফাইল পাথ: app/src/main/java/com/akibul/shikhon/Repo.kt
```kotlin
package com.akibul.shikhon

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

object Repo {
    private val BASE = listOf("cls", "roll", "name", "guardian", "village", "upazila", "district", "mobile", "religion")
    val MARK_KEYS: List<String> = listOf("BAN", "ENG", "MATH", "SOC", "SCI", "BGW", "REL", "PE", "ART").flatMap { s ->
        (1..3).flatMap { t -> listOf("TB", "ACT", "LANG", "CT", "WR", "TOTAL").map { c -> mk(s, t, c) } }
    }
    val TEXT_KEYS: List<String> = (1..3).flatMap { t -> listOf("DAYS", "PRES", "ACT", "DISC", "COND", "REM").map { "${it}_$t" } }

    // ---------- JSON (ব্যাকআপ/রিস্টোর) ----------
    fun toJson(list: List<Student>, st: Settings): String {
        val o = JSONObject()
        o.put("version", 1)
        o.put("school", st.school)
        o.put("year", st.year)
        o.put("teachers", JSONObject().also { j -> st.teachers.forEach { (k, v) -> j.put(k.toString(), v) } })
        o.put("teacherMobiles", JSONObject().also { j -> st.teacherMobiles.forEach { (k, v) -> j.put(k.toString(), v) } })
        val a = JSONArray()
        list.forEach { s ->
            a.put(JSONObject().apply {
                put("id", s.id); put("cls", s.cls); put("roll", s.roll); put("name", s.name)
                put("guardian", s.guardian); put("village", s.village); put("upazila", s.upazila)
                put("district", s.district); put("mobile", s.mobile); put("religion", s.religion)
                put("m", JSONObject().also { j -> s.m.forEach { (k, v) -> j.put(k, v) } })
                put("x", JSONObject().also { j -> s.x.forEach { (k, v) -> j.put(k, v) } })
            })
        }
        o.put("students", a)
        return o.toString()
    }

    private fun intMap(j: JSONObject?): Map<Int, String> =
        j?.keys()?.asSequence()?.mapNotNull { k -> k.toIntOrNull()?.let { it to j.getString(k) } }?.toMap() ?: emptyMap()

    fun fromJson(txt: String): Pair<List<Student>, Settings> {
        val o = JSONObject(txt)
        val st = Settings(
            school = o.optString("school"), year = o.optString("year", "২০২৬"),
            teachers = intMap(o.optJSONObject("teachers")), teacherMobiles = intMap(o.optJSONObject("teacherMobiles"))
        )
        val a = o.optJSONArray("students") ?: JSONArray()
        val list = (0 until a.length()).map { i ->
            val j = a.getJSONObject(i)
            val mj = j.optJSONObject("m"); val xj = j.optJSONObject("x")
            Student(
                id = j.optString("id", UUID.randomUUID().toString()), cls = j.optInt("cls", 1), roll = j.optInt("roll"),
                name = j.optString("name"), guardian = j.optString("guardian"), village = j.optString("village"),
                upazila = j.optString("upazila"), district = j.optString("district"), mobile = j.optString("mobile"),
                religion = j.optString("religion", "ইসলাম"),
                m = mj?.keys()?.asSequence()?.associateWith { mj.getDouble(it) } ?: emptyMap(),
                x = xj?.keys()?.asSequence()?.associateWith { xj.getString(it) } ?: emptyMap()
            )
        }
        return list to st
    }

    private fun file(ctx: Context) = File(ctx.filesDir, "data.json")
    fun load(ctx: Context): Pair<List<Student>, Settings> =
        try { if (file(ctx).exists()) fromJson(file(ctx).readText()) else emptyList<Student>() to Settings() }
        catch (e: Exception) { emptyList<Student>() to Settings() }
    fun save(ctx: Context, list: List<Student>, st: Settings) { file(ctx).writeText(toJson(list, st)) }

    // ---------- CSV ----------
    private fun q(s: String) = if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s

    fun header(): List<String> = BASE + MARK_KEYS + TEXT_KEYS

    fun toCsv(list: List<Student>): String {
        val sb = StringBuilder("\uFEFF")
        sb.append(header().joinToString(",")).append("\n")
        list.sortedWith(compareBy({ it.cls }, { it.roll })).forEach { s ->
            val row = listOf(s.cls.toString(), s.roll.toString(), s.name, s.guardian, s.village, s.upazila, s.district, s.mobile, s.religion) +
                MARK_KEYS.map { k -> s.m[k]?.let { fmtEn(it) } ?: "" } +
                TEXT_KEYS.map { k -> s.x[k] ?: "" }
            sb.append(row.joinToString(",") { q(it) }).append("\n")
        }
        return sb.toString()
    }

    fun parseCsv(t0: String): List<List<String>> {
        val t = t0.removePrefix("\uFEFF")
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val sb = StringBuilder(); var inQ = false; var i = 0
        while (i < t.length) {
            val c = t[i]
            if (inQ) {
                if (c == '"') { if (i + 1 < t.length && t[i + 1] == '"') { sb.append('"'); i++ } else inQ = false } else sb.append(c)
            } else when (c) {
                '"' -> inQ = true
                ',' -> { row.add(sb.toString()); sb.clear() }
                '\n' -> { row.add(sb.toString()); sb.clear(); rows.add(row); row = mutableListOf() }
                '\r' -> {}
                else -> sb.append(c)
            }
            i++
        }
        if (sb.isNotEmpty() || row.isNotEmpty()) { row.add(sb.toString()); rows.add(row) }
        return rows.filter { r -> r.any { it.isNotBlank() } }
    }

    /** একই শ্রেণি+রোল থাকলে হালনাগাদ, না থাকলে নতুন শিক্ষার্থী যোগ */
    fun importCsv(txt: String, existing: List<Student>): List<Student> {
        val rows = parseCsv(txt)
        if (rows.size < 2) return existing
        val idx = rows[0].mapIndexed { i, h -> h.trim() to i }.toMap()
        val out = existing.toMutableList()
        for (r in rows.drop(1)) {
            fun g(k: String) = idx[k]?.let { r.getOrNull(it) }?.trim() ?: ""
            val cls = fromBn(g("cls")).toIntOrNull()?.takeIf { it in 1..5 } ?: continue
            val roll = fromBn(g("roll")).toIntOrNull() ?: continue
            val m = mutableMapOf<String, Double>(); val x = mutableMapOf<String, String>()
            MARK_KEYS.forEach { k -> g(k).takeIf { it.isNotBlank() }?.let { v -> fromBn(v).toDoubleOrNull()?.let { m[k] = it } } }
            TEXT_KEYS.forEach { k -> g(k).takeIf { it.isNotBlank() }?.let { x[k] = it } }
            val pos = out.indexOfFirst { it.cls == cls && it.roll == roll }
            val base = if (pos >= 0) out[pos] else Student(UUID.randomUUID().toString(), cls, roll, "")
            val upd = base.copy(
                name = g("name").ifBlank { base.name }, guardian = g("guardian").ifBlank { base.guardian },
                village = g("village").ifBlank { base.village }, upazila = g("upazila").ifBlank { base.upazila },
                district = g("district").ifBlank { base.district }, mobile = g("mobile").ifBlank { base.mobile },
                religion = g("religion").ifBlank { base.religion },
                m = base.m + m, x = base.x + x
            )
            if (pos >= 0) out[pos] = upd else out.add(upd)
        }
        return out
    }
}
```

## ফাইল পাথ: app/src/main/java/com/akibul/shikhon/PdfGen.kt
```kotlin
package com.akibul.shikhon

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

/**
 * প্রতি শিক্ষার্থীর জন্য ২টি A4 Landscape পাতা:
 *  পাতা-১ (বাইরের পাশ): বাম অর্ধেক = ব্যাক কভার (মতামত/স্বাক্ষর/গ্রেড স্কেল), ডান অর্ধেক = ফ্রন্ট কভার (শিক্ষার্থীর তথ্য)
 *  পাতা-২ (ভেতরের পাশ): মূল নম্বর পাতা (নম্বর, গুণাবলি, উপস্থিতি)
 * মাঝখানে ভাঁজ করলে A5 বইয়ের মতো হবে।
 */
object PdfGen {
    private const val W = 842
    private const val H = 595
    private val stroke = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 0.8f }
    private val gray = Paint().apply { color = 0xFFEAEAEA.toInt(); style = Paint.Style.FILL }

    private fun tp(size: Float, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size; color = Color.BLACK
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
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

    private fun cell(c: Canvas, t: String, l: Float, top: Float, w: Float, h: Float,
                     size: Float = 9f, bold: Boolean = false, fill: Boolean = false) {
        if (fill) c.drawRect(l, top, l + w, top + h, gray)
        c.drawRect(l, top, l + w, top + h, stroke)
        if (t.isEmpty()) return
        val p = tp(size, bold); p.textAlign = Paint.Align.CENTER
        val lines = t.split("\n")
        while (lines.maxOf { p.measureText(it) } > w - 4 && p.textSize > 5f) p.textSize -= 0.5f
        val lh = p.textSize * 1.25f
        var y = top + (h - lh * lines.size) / 2 + p.textSize * 0.95f
        lines.forEach { c.drawText(it, l + w / 2, y, p); y += lh }
    }

    private fun box(c: Canvas, t: String, l: Float, top: Float, w: Float, h: Float, size: Float = 8f) {
        c.drawRect(l, top, l + w, top + h, stroke)
        val p = tp(size)
        var y = top + size + 3
        wrap(t, p, w - 8).forEach { if (y < top + h) c.drawText(it, l + 4, y, p); y += size * 1.3f }
    }

    private fun center(c: Canvas, t: String, cx: Float, y: Float, size: Float, bold: Boolean = false, maxW: Float = 340f) {
        val p = tp(size, bold); p.textAlign = Paint.Align.CENTER
        while (p.measureText(t) > maxW && p.textSize > 6f) p.textSize -= 0.5f
        c.drawText(t, cx, y, p)
    }

    fun write(students: List<Student>, st: Settings, out: OutputStream) {
        val doc = PdfDocument()
        var n = 1
        students.forEach { s ->
            val p1 = doc.startPage(PdfDocument.PageInfo.Builder(W, H, n++).create())
            cover(p1.canvas, s, st); doc.finishPage(p1)
            val p2 = doc.startPage(PdfDocument.PageInfo.Builder(W, H, n++).create())
            marks(p2.canvas, s); doc.finishPage(p2)
        }
        doc.writeTo(out); doc.close()
    }

    // ================= বাইরের পাশ =================
    private fun cover(c: Canvas, s: Student, st: Settings) {
        // ---- বাম অর্ধেক: ব্যাক কভার ----
        val x0 = 22f
        center(c, "শ্রেণি শিক্ষকের সার্বিক মতামত ও স্বাক্ষর", x0 + 377f / 2, 32f, 12f, true, 370f)
        val cw = floatArrayOf(44f, 125f, 70f, 70f, 68f)
        val hd = listOf("প্রান্তিক", "শ্রেণি শিক্ষকের\nসার্বিক মতামত", "শ্রেণি শিক্ষকের\nস্বাক্ষর", "প্রধান শিক্ষকের\nস্বাক্ষর", "অভিভাবকের\nস্বাক্ষর")
        var x = x0
        hd.forEachIndexed { i, h -> cell(c, h, x, 42f, cw[i], 30f, 8.5f, true, true); x += cw[i] }
        val tl = listOf("১ম\nপ্রান্তিক", "২য়\nপ্রান্তিক", "৩য়\nপ্রান্তিক")
        for (t in 1..3) {
            val y = 72f + (t - 1) * 78f
            x = x0
            cell(c, tl[t - 1], x, y, cw[0], 78f, 8.5f, true); x += cw[0]
            box(c, s.x["REM_$t"] ?: "", x, y, cw[1], 78f, 8f); x += cw[1]
            for (i in 2..4) { cell(c, "", x, y, cw[i], 78f); x += cw[i] }
        }
        val gy = 322f
        val gw = floatArrayOf(120f, 70f, 187f)
        x = x0
        listOf("প্রাপ্ত নম্বর", "গ্রেড", "শিখন অর্জন").forEachIndexed { i, h -> cell(c, h, x, gy, gw[i], 24f, 9.5f, true, true); x += gw[i] }
        val scale = listOf(
            Triple("৮০%-১০০%", "A", "অতি উত্তম"), Triple("৬০%-৭৯%", "B", "উত্তম"),
            Triple("৪০%-৫৯%", "C", "সন্তোষজনক"), Triple("০%-৩৯%", "D", "সহায়তা প্রয়োজন")
        )
        scale.forEachIndexed { r, (a, g, d) ->
            x = x0; val y = gy + 24f + r * 26f
            cell(c, a, x, y, gw[0], 26f, 10f); x += gw[0]
            cell(c, g, x, y, gw[1], 26f, 11f, true); x += gw[1]
            cell(c, d, x, y, gw[2], 26f, 10f)
        }
        val tn = st.teachers[s.cls] ?: ""; val tm = st.teacherMobiles[s.cls] ?: ""
        val lp = tp(11f)
        c.drawText("শ্রেণি শিক্ষকের নাম: $tn", x0, 480f, lp)
        c.drawText("মোবাইল নম্বর: ${bn(tm)}", x0, 504f, lp)
        center(c, "জাতীয় শিক্ষাক্রম ও পাঠ্যপুস্তক বোর্ড, বাংলাদেশ", x0 + 377f / 2, 560f, 11f, true, 370f)

        // ---- ডান অর্ধেক: ফ্রন্ট কভার ----
        val l = 441f; val w = 379f
        val thick = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 3f }
        c.drawRect(l, 22f, l + w, 573f, thick)
        c.drawRect(l + 7, 29f, l + w - 7, 566f, stroke)
        val cx = l + w / 2
        center(c, "শিখন অগ্রগতি প্রতিবেদন", cx, 105f, 26f, true)
        center(c, "শিক্ষাবর্ষ: ${bn(st.year)}", cx, 135f, 13f)
        center(c, st.school.ifBlank { "বিদ্যালয়ের নাম" }, cx, 175f, 17f, true, 345f)
        center(c, "(${CLSN[s.cls - 1]} শ্রেণি)", cx, 202f, 13f)
        val rows = listOf(
            "শিক্ষার্থীর নাম" to s.name, "শ্রেণি" to CLSN[s.cls - 1], "রোল" to bn(s.roll.toString()),
            "অভিভাবকের নাম" to s.guardian, "গ্রাম" to s.village, "উপজেলা" to s.upazila,
            "জেলা" to s.district, "মোবাইল নম্বর" to bn(s.mobile)
        )
        var y = 262f
        rows.forEach { (lab, v) ->
            val lpnt = tp(12f); val vp = tp(12f, true)
            val tx = l + 22
            c.drawText("$lab :", tx, y, lpnt)
            val vx = tx + 112f
            c.drawText(v, vx, y - 1, vp)
            c.drawLine(vx, y + 4, l + w - 22, y + 4, stroke)
            y += 36f
        }
    }

    // ================= ভেতরের পাশ =================
    private fun marks(c: Canvas, s: Student) {
        val subs = s.subs(); val L = 24f
        center(c, "শিখন অগ্রগতি প্রতিবেদন — ${s.name}   |   শ্রেণি: ${CLSN[s.cls - 1]}   |   রোল: ${bn(s.roll.toString())}", W / 2f, 28f, 13f, true, 790f)
        var y = 40f
        val sw = 170f; val cw = (W - 2 * L - sw) / 12f
        cell(c, "বিষয় (পূর্ণমান)", L, y, sw, 50f, 9.5f, true, true)
        val ord = listOf("১ম", "২য়", "৩য়")
        for (t in 0..2) cell(c, "${ord[t]} প্রান্তিক", L + sw + t * 3 * cw, y, 3 * cw, 22f, 9f, true, true)
        cell(c, "চূড়ান্ত ফলাফল", L + sw + 9 * cw, y, 3 * cw, 22f, 9f, true, true)
        val sub1 = listOf("ধারাবাহিক\nনম্বর", "সামষ্টিক\nনম্বর", "মোট\nনম্বর")
        val sub2 = listOf("প্রান্তিকের\nগড় নম্বর", "বিষয়\nগ্রেড", "সমন্বিত\nগ্রেড")
        for (t in 0..2) for (i in 0..2) cell(c, sub1[i], L + sw + (t * 3 + i) * cw, y + 22f, cw, 28f, 7.5f, true, true)
        for (i in 0..2) cell(c, sub2[i], L + sw + (9 + i) * cw, y + 22f, cw, 28f, 7.5f, true, true)
        y += 50f
        val rh = 26f
        val anyData = (1..3).any { s.hasTerm(it) }
        subs.forEachIndexed { i, sub ->
            val ry = y + i * rh
            cell(c, "${sub.bn} (${bn(sub.max.toString())})", L, ry, sw, rh, 9f)
            for (t in 1..3) {
                val has = s.hasSub(sub, t); val b = L + sw + (t - 1) * 3 * cw
                cell(c, if (has) fmt(s.cont(sub, t)) else "", b, ry, cw, rh)
                cell(c, if (sub.written == 0) "--" else if (has) fmt(s.wr(sub, t)) else "", b + cw, ry, cw, rh)
                cell(c, if (has) fmt(s.total(sub, t)) else "", b + 2 * cw, ry, cw, rh, 9f, true)
            }
            val a = s.avg(sub)
            cell(c, if (anyData) fmt(a) else "", L + sw + 9 * cw, ry, cw, rh, 9f, true)
            cell(c, if (anyData) gradeOf(a / sub.max * 100) else "", L + sw + 10 * cw, ry, cw, rh, 10f, true)
        }
        // সমন্বিত গ্রেড (মার্জড ঘর)
        val pct = s.overallPct()
        cell(c, if (anyData) "${gradeOf(pct)}\n${fmt(pct)}%" else "", L + sw + 11 * cw, y, cw, rh * subs.size, 12f, true)
        // মোট সারি
        val ty = y + subs.size * rh
        val totMax = subs.sumOf { it.max }
        cell(c, "সর্বমোট নম্বর (${bn(totMax.toString())})", L, ty, sw, rh, 9f, true, true)
        for (t in 1..3) {
            val b = L + sw + (t - 1) * 3 * cw; val has = s.hasTerm(t)
            cell(c, if (has) fmt(subs.sumOf { s.cont(it, t) }) else "", b, ty, cw, rh, 9f, false, true)
            cell(c, if (has) fmt(subs.sumOf { s.wr(it, t) }) else "", b + cw, ty, cw, rh, 9f, false, true)
            cell(c, if (has) fmt(subs.sumOf { s.total(it, t) }) else "", b + 2 * cw, ty, cw, rh, 9f, true, true)
        }
        cell(c, if (anyData) fmt(subs.sumOf { s.avg(it) }) else "", L + sw + 9 * cw, ty, cw, rh, 9f, true, true)
        cell(c, "", L + sw + 10 * cw, ty, cw, rh, 9f, false, true)
        cell(c, "", L + sw + 11 * cw, ty, cw, rh, 9f, false, true)

        // ---- ব্যক্তিগত ও সামাজিক গুণাবলি ----
        var by = ty + rh + 18f
        val qw = floatArrayOf(70f, 270f, 46f, 46f, 46f)  // মোট 478
        var x = L
        listOf("গুণাবলি", "বর্ণনা", "১ম প্রান্তিক", "২য় প্রান্তিক", "৩য় প্রান্তিক").forEachIndexed { i, h ->
            cell(c, h, x, by, qw[i], 24f, 8.5f, true, true); x += qw[i]
        }
        val q = listOf(
            Triple("সক্রিয়তা", "ACT", "শিক্ষকের নির্দেশনামতো পাঠ্যপুস্তকের কাজ সম্পন্ন করা; বাড়ির কাজ সময়মতো করা; জোড়ায়/দলগত কাজে অংশগ্রহণ; প্রশ্নোত্তরে অংশ নেওয়া ইত্যাদি"),
            Triple("শৃঙ্খলা", "DISC", "বিদ্যালয়ে নিয়মিত উপস্থিত থাকা; বিদ্যালয়ের নিয়ম-শৃঙ্খলা মেনে চলা; পরিষ্কার-পরিচ্ছন্ন থাকা ইত্যাদি"),
            Triple("আচরণ", "COND", "শিক্ষক/বড়দের সম্মান করা; ছোটদের স্নেহ করা; সহযোগিতা করা; ভুল হলে স্বীকার করা; অন্যকে কষ্ট না দেওয়া ইত্যাদি")
        )
        q.forEachIndexed { r, (lab, k, d) ->
            val yy = by + 24f + r * 34f; x = L
            cell(c, lab, x, yy, qw[0], 34f, 9f, true); x += qw[0]
            box(c, d, x, yy, qw[1], 34f, 7f); x += qw[1]
            for (t in 1..3) { cell(c, s.x["${k}_$t"] ?: "", x, yy, qw[1 + t], 34f, 11f, true); x += qw[1 + t] }
        }
        val lg = tp(8f)
        c.drawText("ক = উত্তম,  খ = সন্তোষজনক,  গ = উন্নতি প্রয়োজন", L, by + 24f + 3 * 34f + 12f, lg)

        // ---- উপস্থিতি ----
        val ax = L + 478f + 16f
        val aw = floatArrayOf(108f, 64f, 64f, 64f)
        x = ax
        listOf("উপস্থিতি", "১ম প্রান্তিক", "২য় প্রান্তিক", "৩য় প্রান্তিক").forEachIndexed { i, h ->
            cell(c, h, x, by, aw[i], 24f, 8.5f, true, true); x += aw[i]
        }
        val labels = listOf("মোট কার্যদিবস", "শিক্ষার্থীর উপস্থিতি", "উপস্থিতির হার")
        labels.forEachIndexed { r, lab ->
            val yy = by + 24f + r * 34f; x = ax
            cell(c, lab, x, yy, aw[0], 34f, 9f, true); x += aw[0]
            for (t in 1..3) {
                val d = s.x["DAYS_$t"]?.let { fromBn(it).toDoubleOrNull() }
                val p = s.x["PRES_$t"]?.let { fromBn(it).toDoubleOrNull() }
                val txt = when (r) {
                    0 -> d?.let { fmt(it) } ?: ""
                    1 -> p?.let { fmt(it) } ?: ""
                    else -> if (d != null && p != null && d > 0) "${fmt(p / d * 100)}%" else ""
                }
                cell(c, txt, x, yy, aw[t], 34f, 10f); x += aw[t]
            }
        }
    }
}
```

## ফাইল পাথ: app/src/main/java/com/akibul/shikhon/MainActivity.kt
```kotlin
@file:OptIn(ExperimentalMaterial3Api::class)

package com.akibul.shikhon

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.UUID

class AppState(val ctx: Context) {
    var students by mutableStateOf(listOf<Student>())
    var settings by mutableStateOf(Settings())

    init { val (a, b) = Repo.load(ctx); students = a; settings = b }

    fun persist() = Repo.save(ctx, students, settings)
    fun upsert(s: Student) {
        students = if (students.any { it.id == s.id }) students.map { if (it.id == s.id) s else it } else students + s
        persist()
    }
    fun delete(id: String) { students = students.filter { it.id != id }; persist() }
    fun updateSettings(st: Settings) { settings = st; persist() }
    fun replaceAll(list: List<Student>, st: Settings) { students = list; settings = st; persist() }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = AppState(applicationContext)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { App(state) } } }
    }
}

private fun toast(ctx: Context, m: String) = Toast.makeText(ctx, m, Toast.LENGTH_SHORT).show()
private fun readText(ctx: Context, uri: Uri) =
    ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
private fun writeText(ctx: Context, uri: Uri, t: String) {
    ctx.contentResolver.openOutputStream(uri)?.use { it.write(t.toByteArray(Charsets.UTF_8)) }
}

@Composable
fun App(st: AppState) {
    val ctx = LocalContext.current
    var screen by remember { mutableStateOf("list") }
    var editing by remember { mutableStateOf<Student?>(null) }
    var cls by remember { mutableStateOf(1) }
    var pending by remember { mutableStateOf<List<Student>>(emptyList()) }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.openOutputStream(uri)?.use { PdfGen.write(pending, st.settings, it) }
                toast(ctx, "PDF সংরক্ষিত হয়েছে")
            } catch (e: Exception) { toast(ctx, "PDF তৈরি করা যায়নি: ${e.message}") }
        }
    }
    val makePdf: (List<Student>, String) -> Unit = { list, name ->
        if (list.isEmpty()) toast(ctx, "কোনো শিক্ষার্থী নেই") else { pending = list; pdfLauncher.launch(name) }
    }

    when (screen) {
        "list" -> ListScreen(st, cls, { cls = it },
            onOpen = { editing = it; screen = "edit" },
            onNew = {
                val next = (st.students.filter { it.cls == cls }.maxOfOrNull { it.roll } ?: 0) + 1
                editing = Student(UUID.randomUUID().toString(), cls, next, ""); screen = "edit"
            },
            onSettings = { screen = "settings" },
            onClassPdf = { makePdf(st.students.filter { it.cls == cls }.sortedBy { it.roll }, "শ্রেণি-${cls}-প্রতিবেদন.pdf") })
        "edit" -> editing?.let { e ->
            EditScreen(st, e, onBack = { screen = "list" }, onPdf = { makePdf(listOf(it), "${it.name.ifBlank { "student" }}.pdf") })
        }
        else -> SettingsScreen(st, onBack = { screen = "list" }, makePdf = makePdf)
    }
}

@Composable
fun ListScreen(
    st: AppState, cls: Int, onCls: (Int) -> Unit, onOpen: (Student) -> Unit,
    onNew: () -> Unit, onSettings: () -> Unit, onClassPdf: () -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("শিখন অগ্রগতি প্রতিবেদন", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = onSettings) { Text("সেটিংস") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..5).forEach { c -> FilterChip(selected = cls == c, onClick = { onCls(c) }, label = { Text(listOf("১ম", "২য়", "৩য়", "৪র্থ", "৫ম")[c - 1]) }) }
        }
        Spacer(Modifier.height(8.dp))
        val list = st.students.filter { it.cls == cls }.sortedBy { it.roll }
        Text("${CLSN[cls - 1]} শ্রেণি — মোট ${bn(list.size.toString())} জন", style = MaterialTheme.typography.labelLarge)
        LazyColumn(Modifier.weight(1f).padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(list, key = { it.id }) { s ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(s) }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(bn(s.roll.toString()), fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                        Text(s.name.ifBlank { "(নাম নেই)" }, modifier = Modifier.weight(1f))
                        Text(s.religion, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onNew, modifier = Modifier.weight(1f)) { Text("+ শিক্ষার্থী") }
            OutlinedButton(onClick = onClassPdf, modifier = Modifier.weight(1f)) { Text("শ্রেণির সব PDF") }
        }
    }
}

@Composable
fun Field(label: String, value: String, number: Boolean = false, lines: Int = 1, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        singleLine = lines == 1, minLines = lines,
        keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Number else KeyboardType.Text),
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    )
}

@Composable
fun MarkField(label: String, max: Double, value: Double?, k: String, onValue: (Double?) -> Unit) {
    var t by remember(k) { mutableStateOf(value?.let { fmtEn(it) } ?: "") }
    val v = fromBn(t).toDoubleOrNull()
    val err = t.isNotBlank() && (v == null || v > max || v < 0)
    OutlinedTextField(
        value = t,
        onValueChange = {
            t = it
            if (it.isBlank()) onValue(null)
            else fromBn(it).toDoubleOrNull()?.let { d -> if (d in 0.0..max) onValue(d) }
        },
        label = { Text(label) }, isError = err, singleLine = true,
        supportingText = if (err) ({ Text("সর্বোচ্চ ${fmt(max)}") }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    )
}

@Composable
fun EditScreen(st: AppState, init: Student, onBack: () -> Unit, onPdf: (Student) -> Unit) {
    var s by remember { mutableStateOf(init) }
    val cur by rememberUpdatedState(s)
    val deleted = remember { booleanArrayOf(false) }
    var tab by remember { mutableStateOf(0) }
    var term by remember { mutableStateOf(1) }
    var askDelete by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { if (!deleted[0] && cur.name.isNotBlank()) st.upsert(cur) } }
    BackHandler { onBack() }

    fun setX(k: String, v: String) { s = s.copy(x = s.x.toMutableMap().also { if (v.isBlank()) it.remove(k) else it[k] = v }) }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        TabRow(selectedTabIndex = tab) {
            listOf("তথ্য", "নম্বর", "উপস্থিতি/গুণাবলি").forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp)) {
            when (tab) {
                0 -> {
                    Field("শিক্ষার্থীর নাম", s.name) { s = s.copy(name = it) }
                    Field("রোল", if (s.roll == 0) "" else s.roll.toString(), number = true) { s = s.copy(roll = fromBn(it).toIntOrNull() ?: 0) }
                    Text("শ্রেণি", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { c -> FilterChip(selected = s.cls == c, onClick = { s = s.copy(cls = c) }, label = { Text(bn(c.toString())) }) }
                    }
                    Text("ধর্ম", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RELIGIONS.forEach { r -> FilterChip(selected = s.religion == r, onClick = { s = s.copy(religion = r) }, label = { Text(r) }) }
                    }
                    Field("অভিভাবকের নাম", s.guardian) { s = s.copy(guardian = it) }
                    Field("গ্রাম", s.village) { s = s.copy(village = it) }
                    Field("উপজেলা", s.upazila) { s = s.copy(upazila = it) }
                    Field("জেলা", s.district) { s = s.copy(district = it) }
                    Field("মোবাইল নম্বর", s.mobile, number = true) { s = s.copy(mobile = it) }
                }
                1 -> {
                    Text("সমন্বিত ফলাফল: ${fmt(s.overallPct())}%  —  গ্রেড ${gradeOf(s.overallPct())}", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                        (1..3).forEach { t -> FilterChip(selected = term == t, onClick = { term = t }, label = { Text("${bn(t.toString())} প্রান্তিক") }) }
                    }
                    s.subs().forEach { sub ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("${sub.bn} (${bn(sub.max.toString())})", fontWeight = FontWeight.Bold)
                                sub.comps.forEach { c ->
                                    val k = mk(sub.key, term, c.key)
                                    MarkField("${c.bn} (${bn(c.max.toString())})", c.max.toDouble(), s.m[k], "${s.id}$k") { v ->
                                        s = s.copy(m = s.m.toMutableMap().also { mm -> if (v == null) mm.remove(k) else mm[k] = v })
                                    }
                                }
                                if (sub.written > 0) {
                                    val k = mk(sub.key, term, "WR")
                                    MarkField("সামষ্টিক (লিখিত) (${bn(sub.written.toString())})", sub.written.toDouble(), s.m[k], "${s.id}$k") { v ->
                                        s = s.copy(m = s.m.toMutableMap().also { mm -> if (v == null) mm.remove(k) else mm[k] = v })
                                    }
                                }
                                Text("প্রান্তিক মোট: ${fmt(s.total(sub, term))} / ${bn(sub.max.toString())}", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
                else -> {
                    (1..3).forEach { t ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("${bn(t.toString())} প্রান্তিক", fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(Modifier.weight(1f)) { Field("মোট কার্যদিবস", s.x["DAYS_$t"] ?: "", true) { setX("DAYS_$t", it) } }
                                    Box(Modifier.weight(1f)) { Field("উপস্থিত দিন", s.x["PRES_$t"] ?: "", true) { setX("PRES_$t", it) } }
                                }
                                listOf("সক্রিয়তা" to "ACT", "শৃঙ্খলা" to "DISC", "আচরণ" to "COND").forEach { (lab, key) ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(lab, modifier = Modifier.weight(1f))
                                        listOf("ক", "খ", "গ").forEach { g ->
                                            val sel = s.x["${key}_$t"] == g
                                            FilterChip(selected = sel, onClick = { setX("${key}_$t", if (sel) "" else g) },
                                                label = { Text(g) }, modifier = Modifier.padding(start = 4.dp))
                                        }
                                    }
                                }
                                Field("শ্রেণি শিক্ষকের সার্বিক মতামত", s.x["REM_$t"] ?: "", lines = 3) { setX("REM_$t", it) }
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("সংরক্ষণ ও পিছনে") }
            Button(onClick = { if (s.name.isNotBlank()) { st.upsert(s); onPdf(s) } }, modifier = Modifier.weight(1f)) { Text("PDF তৈরি") }
            TextButton(onClick = { askDelete = true }) { Text("মুছুন") }
        }
    }
    if (askDelete) AlertDialog(
        onDismissRequest = { askDelete = false },
        title = { Text("শিক্ষার্থী মুছবেন?") }, text = { Text("এই শিক্ষার্থীর সব তথ্য মুছে যাবে।") },
        confirmButton = { TextButton(onClick = { deleted[0] = true; st.delete(s.id); askDelete = false; onBack() }) { Text("মুছুন") } },
        dismissButton = { TextButton(onClick = { askDelete = false }) { Text("না") } }
    )
}

@Composable
fun SettingsScreen(st: AppState, onBack: () -> Unit, makePdf: (List<Student>, String) -> Unit) {
    val ctx = LocalContext.current
    var payload by remember { mutableStateOf("") }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    BackHandler { onBack() }

    val csvSave = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { u ->
        if (u != null) { writeText(ctx, u, payload); toast(ctx, "CSV সংরক্ষিত হয়েছে") }
    }
    val jsonSave = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { u ->
        if (u != null) { writeText(ctx, u, payload); toast(ctx, "ব্যাকআপ সংরক্ষিত হয়েছে") }
    }
    val csvOpen = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) try {
            val before = st.students.size
            st.replaceAll(Repo.importCsv(readText(ctx, u), st.students), st.settings)
            toast(ctx, "ইমপোর্ট সম্পন্ন (নতুন ${bn((st.students.size - before).toString())} জন)")
        } catch (e: Exception) { toast(ctx, "ইমপোর্ট ব্যর্থ: ${e.message}") }
    }
    val jsonOpen = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> if (u != null) restoreUri = u }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Text("সেটিংস", style = MaterialTheme.typography.titleLarge)
        Field("বিদ্যালয়ের নাম", st.settings.school) { st.updateSettings(st.settings.copy(school = it)) }
        Field("শিক্ষাবর্ষ", st.settings.year) { st.updateSettings(st.settings.copy(year = it)) }
        Text("শ্রেণি শিক্ষক", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        (1..5).forEach { c ->
            Text("${CLSN[c - 1]} শ্রেণি", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { Field("নাম", st.settings.teachers[c] ?: "") { v -> st.updateSettings(st.settings.copy(teachers = st.settings.teachers + (c to v))) } }
                Box(Modifier.weight(1f)) { Field("মোবাইল", st.settings.teacherMobiles[c] ?: "", true) { v -> st.updateSettings(st.settings.copy(teacherMobiles = st.settings.teacherMobiles + (c to v))) } }
            }
        }
        Text("PDF", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        Button(onClick = { makePdf(st.students.sortedWith(compareBy({ it.cls }, { it.roll })), "সব-শ্রেণির-প্রতিবেদন.pdf") }, modifier = Modifier.fillMaxWidth()) { Text("সব শ্রেণির সব শিক্ষার্থীর PDF") }
        Text("CSV (Excel)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        OutlinedButton(onClick = { payload = Repo.toCsv(st.students); csvSave.launch("শিক্ষার্থী-তথ্য.csv") }, modifier = Modifier.fillMaxWidth()) { Text("CSV এক্সপোর্ট") }
        OutlinedButton(onClick = { payload = Repo.toCsv(emptyList()); csvSave.launch("খালি-টেমপ্লেট.csv") }, modifier = Modifier.fillMaxWidth()) { Text("খালি CSV টেমপ্লেট") }
        OutlinedButton(onClick = { csvOpen.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("CSV ইমপোর্ট (শ্রেণি+রোল মিললে হালনাগাদ)") }
        Text("ব্যাকআপ / রিস্টোর", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        OutlinedButton(onClick = { payload = Repo.toJson(st.students, st.settings); jsonSave.launch("ব্যাকআপ.json") }, modifier = Modifier.fillMaxWidth()) { Text("সম্পূর্ণ ব্যাকআপ এক্সপোর্ট") }
        OutlinedButton(onClick = { jsonOpen.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("ব্যাকআপ থেকে রিস্টোর") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("পিছনে") }
    }
    restoreUri?.let { u ->
        AlertDialog(
            onDismissRequest = { restoreUri = null },
            title = { Text("রিস্টোর করবেন?") }, text = { Text("বর্তমান সব তথ্য ব্যাকআপের তথ্য দিয়ে বদলে যাবে।") },
            confirmButton = {
                TextButton(onClick = {
                    try { val (l, s) = Repo.fromJson(readText(ctx, u)); st.replaceAll(l, s); toast(ctx, "রিস্টোর সম্পন্ন") }
                    catch (e: Exception) { toast(ctx, "ফাইল ঠিক নেই: ${e.message}") }
                    restoreUri = null
                }) { Text("হ্যাঁ, রিস্টোর") }
            },
            dismissButton = { TextButton(onClick = { restoreUri = null }) { Text("না") } }
        )
    }
}
```

