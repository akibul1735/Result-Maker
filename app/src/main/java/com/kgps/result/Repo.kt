package com.kgps.result

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

object Repo {
    private val BASE = listOf("cls", "roll", "name", "guardian", "village", "post", "upazila", "district", "mobile", "religion")
    val MARK_KEYS: List<String> = listOf("BAN", "ENG", "MATH", "SOC", "SCI", "BGW", "REL", "PE", "ART").flatMap { s ->
        (1..3).flatMap { t -> listOf("TB", "ACT", "LANG", "CT", "WR", "TOTAL").map { c -> mk(s, t, c) } }
    }
    val TEXT_KEYS: List<String> = (1..3).flatMap { t -> listOf("DAYS", "PRES", "ACT", "DISC", "COND", "REM").map { "${it}_$t" } }

    // ---------- JSON (ব্যাকআপ/রিস্টোর) ----------
    fun toJson(list: List<Student>, st: Settings): String {
        val o = JSONObject()
        o.put("version", 1)
        o.put("school", st.school)
        o.put("schoolAddress", st.schoolAddress)
        o.put("year", st.year)
        o.put("teachers", JSONObject().also { j -> st.teachers.forEach { (k, v) -> j.put(k.toString(), v) } })
        o.put("teacherMobiles", JSONObject().also { j -> st.teacherMobiles.forEach { (k, v) -> j.put(k.toString(), v) } })
        o.put("pdfFont", st.pdfFont)
        o.put("dpeLogo", st.dpeLogo)
        val a = JSONArray()
        list.forEach { s ->
            a.put(JSONObject().apply {
                put("id", s.id); put("cls", s.cls); put("roll", s.roll); put("name", s.name)
                put("guardian", s.guardian); put("village", s.village); put("post", s.post); put("upazila", s.upazila)
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
        val rawLogo = o.optString("dpeLogo", "simple")
        val st = Settings(
            school = o.optString("school"),
            schoolAddress = o.optString("schoolAddress"),
            year = o.optString("year", "২০২৬"),
            teachers = intMap(o.optJSONObject("teachers")), teacherMobiles = intMap(o.optJSONObject("teacherMobiles")),
            pdfFont = o.optString("pdfFont", "noto_serif").ifBlank { "noto_serif" },
            dpeLogo = if (rawLogo == "color") "color" else "simple"
        )
        val a = o.optJSONArray("students") ?: JSONArray()
        val list = (0 until a.length()).map { i ->
            val j = a.getJSONObject(i)
            val mj = j.optJSONObject("m"); val xj = j.optJSONObject("x")
            Student(
                id = j.optString("id", UUID.randomUUID().toString()), cls = j.optInt("cls", 1), roll = j.optInt("roll"),
                name = j.optString("name"), guardian = j.optString("guardian"), village = j.optString("village"),
                post = j.optString("post"), upazila = j.optString("upazila"), district = j.optString("district"), mobile = j.optString("mobile"),
                religion = j.optString("religion", "ইসলাম"),
                m = mj?.keys()?.asSequence()?.associateWith { mj.getDouble(it) } ?: emptyMap(),
                x = xj?.keys()?.asSequence()?.associateWith { xj.getString(it) } ?: emptyMap()
            )
        }
        return list to st
    }

    private fun file(ctx: Context) = File(ctx.filesDir, "data.json")
    fun load(ctx: Context): Pair<List<Student>, Settings> =
        try { if (file(ctx).exists()) fromJson(file(ctx).readText()) else SAMPLE_STUDENTS to DEFAULT_SETTINGS }
        catch (e: Exception) { SAMPLE_STUDENTS to DEFAULT_SETTINGS }
    fun save(ctx: Context, list: List<Student>, st: Settings) { file(ctx).writeText(toJson(list, st)) }

    // ---------- CSV ----------
    private fun q(s: String) = if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s

    fun header(): List<String> = BASE + MARK_KEYS + TEXT_KEYS

    fun toCsv(list: List<Student>): String {
        val sb = StringBuilder("\uFEFF")
        sb.append(header().joinToString(",")).append("\n")
        list.sortedWith(compareBy({ it.cls }, { it.roll })).forEach { s ->
            val row = listOf(s.cls.toString(), s.roll.toString(), s.name, s.guardian, s.village, s.post, s.upazila, s.district, s.mobile, s.religion) +
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
                village = g("village").ifBlank { base.village }, post = g("post").ifBlank { base.post }, upazila = g("upazila").ifBlank { base.upazila },
                district = g("district").ifBlank { base.district }, mobile = g("mobile").ifBlank { base.mobile },
                religion = g("religion").ifBlank { base.religion },
                m = base.m + m, x = base.x + x
            )
            if (pos >= 0) out[pos] = upd else out.add(upd)
        }
        return out
    }
}
