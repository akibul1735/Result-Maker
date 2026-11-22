package com.kgps.result

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class CsvHeaderStyle(
    val id: String,
    val labelBn: String,
    val shortBn: String,
    val previewBn: String
) {
    FRIENDLY_BN(
        id = "friendly_bn",
        labelBn = "সহজ বাংলা হেডার",
        shortBn = "সহজ বাংলা",
        previewBn = "শ্রেণি, রোল নম্বর, শিক্ষার্থীর নাম, বাংলা (১ম প্রান্তিক) - লিখিত"
    ),
    FRIENDLY_EN(
        id = "friendly_en",
        labelBn = "সহজ ইংরেজি হেডার",
        shortBn = "সহজ ইংরেজি",
        previewBn = "Class, Roll No, Student Name, Bangla (Term 1) - Written"
    ),
    SYSTEM_KEYS(
        id = "system_keys",
        labelBn = "সিস্টেম কোড (Raw)",
        shortBn = "সিস্টেম কোড",
        previewBn = "cls, roll, name, BAN_1_WR"
    );

    companion object {
        fun fromId(id: String?): CsvHeaderStyle =
            entries.firstOrNull { it.id == id } ?: FRIENDLY_BN
    }
}

object Repo {
    private val BASE = listOf("cls", "roll", "name", "guardian", "village", "union", "upazila", "district", "mobile", "religion")
    val MARK_KEYS: List<String> = listOf("BAN", "ENG", "MATH", "SOC", "SCI", "BGW", "REL", "PE", "ART").flatMap { s ->
        (1..3).flatMap { t -> listOf("TB", "ACT", "LANG", "CT", "WR", "CONT", "TOTAL").map { c -> mk(s, t, c) } }
    }
    val EXPORT_MARK_KEYS: List<String> = exportMarkKeysForClass(null)
    val TEXT_KEYS: List<String> = (1..3).flatMap { t -> listOf("DAYS", "PRES", "ACT", "DISC", "COND", "REM").map { "${it}_$t" } }

    fun exportMarkKeysForClass(cls: Int?): List<String> {
        val low = cls != null && syllabusTierForClass(cls) <= 2
        val subs = when {
            cls == null -> listOf("BAN", "ENG", "MATH", "SOC", "SCI", "BGW", "REL", "PE", "ART")
            low -> listOf("BAN", "ENG", "MATH", "SOC", "REL", "PE", "ART")
            else -> listOf("BAN", "ENG", "MATH", "SCI", "BGW", "REL", "PE", "ART")
        }
        return subs.flatMap { s ->
            val comps = when {
                s == "SOC" || s == "PE" || s == "ART" -> listOf("TB", "ACT", "LANG", "CT", "CONT", "TOTAL")
                s == "REL" && low -> listOf("TB", "ACT", "LANG", "CT", "CONT", "TOTAL")
                else -> listOf("TB", "ACT", "LANG", "CT", "WR", "CONT", "TOTAL")
            }
            (1..3).flatMap { t -> comps.map { c -> mk(s, t, c) } }
        }
    }

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
        o.put("customFields", JSONArray().also { arr ->
            st.customFields.forEach { cf ->
                arr.put(JSONObject().apply { put("key", cf.key); put("label", cf.label) })
            }
        })
        o.put("customClasses", JSONArray().also { arr ->
            st.customClasses.forEach { cc ->
                arr.put(JSONObject().apply {
                    put("id", cc.id)
                    put("nameBn", cc.nameBn)
                    put("shortBn", cc.shortBn)
                    put("syllabusTier", cc.syllabusTier)
                })
            }
        })
        val a = JSONArray()
        list.forEach { s ->
            a.put(JSONObject().apply {
                put("id", s.id); put("cls", s.cls); put("roll", s.roll); put("name", s.name)
                put("guardian", s.guardian); put("village", s.village); put("union", s.union)
                put("upazila", s.upazila.ifBlank { DEFAULT_UPAZILA })
                put("district", s.district.ifBlank { DEFAULT_DISTRICT }); put("mobile", s.mobile); put("religion", s.religion)
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
        val cfArr = o.optJSONArray("customFields")
        val customFields = if (cfArr != null) {
            (0 until cfArr.length()).mapNotNull { idx ->
                val obj = cfArr.optJSONObject(idx) ?: return@mapNotNull null
                val k = obj.optString("key")
                val l = obj.optString("label")
                if (k.isNotBlank() && l.isNotBlank()) CustomFieldDef(k, l) else null
            }
        } else emptyList()
        val ccArr = o.optJSONArray("customClasses")
        val customClasses = if (ccArr != null) {
            (0 until ccArr.length()).mapNotNull { idx ->
                val obj = ccArr.optJSONObject(idx) ?: return@mapNotNull null
                val id = obj.optInt("id", -1).takeIf { it in 6..20 } ?: return@mapNotNull null
                val nameBn = obj.optString("nameBn").trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val shortBn = obj.optString("shortBn").trim().ifBlank { defaultShortClassLabel(nameBn, id) }
                val tier = obj.optInt("syllabusTier", 3).let { if (it <= 2) 1 else 3 }
                CustomClassDef(id, nameBn, shortBn, tier)
            }.distinctBy { it.id }.sortedBy { it.id }
        } else emptyList()
        val st = Settings(
            school = o.optString("school"),
            schoolAddress = o.optString("schoolAddress"),
            year = o.optString("year", "২০২৬"),
            teachers = intMap(o.optJSONObject("teachers")), teacherMobiles = intMap(o.optJSONObject("teacherMobiles")),
            pdfFont = o.optString("pdfFont", "noto_serif").ifBlank { "noto_serif" },
            dpeLogo = if (rawLogo == "color") "color" else "simple",
            customFields = customFields,
            customClasses = customClasses
        )
        val a = o.optJSONArray("students") ?: JSONArray()
        val list = (0 until a.length()).map { i ->
            val j = a.getJSONObject(i)
            val mj = j.optJSONObject("m"); val xj = j.optJSONObject("x")
            val xMap = xj?.keys()?.asSequence()?.associateWith { xj.getString(it) } ?: emptyMap()
            val unionVal = j.optString("union").ifBlank { xMap["UNION"].orEmpty() }
            Student(
                id = j.optString("id", UUID.randomUUID().toString()), cls = j.optInt("cls", 1), roll = j.optInt("roll"),
                name = j.optString("name"), guardian = j.optString("guardian"), village = j.optString("village"),
                union = unionVal,
                post = "",
                upazila = j.optString("upazila").ifBlank { DEFAULT_UPAZILA },
                district = j.optString("district").ifBlank { DEFAULT_DISTRICT },
                mobile = j.optString("mobile"),
                religion = j.optString("religion", "ইসলাম"),
                m = mj?.keys()?.asSequence()?.associateWith { mj.getDouble(it) } ?: emptyMap(),
                x = xMap - "UNION"
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

    private val BASE_BN = mapOf(
        "cls" to "শ্রেণি",
        "roll" to "রোল নম্বর",
        "name" to "শিক্ষার্থীর নাম",
        "guardian" to "পিতা/অভিভাবকের নাম",
        "village" to "গ্রাম",
        "union" to "ইউনিয়ন",
        "upazila" to "উপজেলা",
        "district" to "জেলা",
        "mobile" to "মোবাইল নম্বর",
        "religion" to "ধর্ম"
    )

    private val BASE_EN = mapOf(
        "cls" to "Class",
        "roll" to "Roll No",
        "name" to "Student Name",
        "guardian" to "Guardian Name",
        "village" to "Village",
        "union" to "Union",
        "upazila" to "Upazila",
        "district" to "District",
        "mobile" to "Mobile No",
        "religion" to "Religion"
    )

    private val SUB_BN = mapOf(
        "BAN" to "বাংলা",
        "ENG" to "ইংরেজি",
        "MATH" to "গণিত",
        "SOC" to "সামাজিক ও বিজ্ঞান",
        "SCI" to "বিজ্ঞান",
        "BGW" to "বাংলাদেশ ও বিশ্বপরিচয়",
        "REL" to "ধর্মশিক্ষা",
        "PE" to "শারীরিক ও স্বাস্থ্য",
        "ART" to "শিল্পকলা"
    )

    private val SUB_EN = mapOf(
        "BAN" to "Bangla",
        "ENG" to "English",
        "MATH" to "Math",
        "SOC" to "Social & Science",
        "SCI" to "Science",
        "BGW" to "Bangladesh & Global",
        "REL" to "Religion",
        "PE" to "Physical & Health",
        "ART" to "Art"
    )

    private val COMP_BN = mapOf(
        "TB" to "পাঠ্যপুস্তক",
        "ACT" to "সক্রিয়তা",
        "LANG" to "ভাষাদক্ষতা",
        "CT" to "ক্লাস টেস্ট",
        "WR" to "লিখিত",
        "CONT" to "ধারাবাহিক মোট",
        "TOTAL" to "বিষয় মোট"
    )

    private val COMP_EN = mapOf(
        "TB" to "Textbook",
        "ACT" to "Activity",
        "LANG" to "Language",
        "CT" to "Class Test",
        "WR" to "Written",
        "CONT" to "Continuous Total",
        "TOTAL" to "Subject Total"
    )

    private val TEXT_BN = mapOf(
        "DAYS" to "মোট কার্যদিবস",
        "PRES" to "মোট উপস্থিতি",
        "ACT" to "সহপাঠক্রমিক",
        "DISC" to "শৃঙ্খলাবোধ",
        "COND" to "আচরণ",
        "REM" to "শিক্ষকের মন্তব্য"
    )

    private val TEXT_EN = mapOf(
        "DAYS" to "Working Days",
        "PRES" to "Attendance",
        "ACT" to "Co-curricular",
        "DISC" to "Discipline",
        "COND" to "Conduct",
        "REM" to "Teacher Remark"
    )

    private fun termBn(t: Int) = when (t) {
        1 -> "১ম প্রান্তিক"
        2 -> "২য় প্রান্তিক"
        else -> "৩য় প্রান্তিক"
    }

    fun friendlyMarkHeader(key: String, style: CsvHeaderStyle): String {
        if (style == CsvHeaderStyle.SYSTEM_KEYS) return key
        val parts = key.split('_')
        if (parts.size != 3) return key
        val sub = parts[0]
        val term = parts[1].toIntOrNull() ?: 1
        val comp = parts[2]
        return when (style) {
            CsvHeaderStyle.FRIENDLY_BN -> {
                val subLabel = SUB_BN[sub] ?: sub
                val compLabel = if (sub == "ART" && comp == "TOTAL") "মোট নম্বর" else (COMP_BN[comp] ?: comp)
                "$subLabel (${termBn(term)}) - $compLabel"
            }
            CsvHeaderStyle.FRIENDLY_EN -> {
                val subLabel = SUB_EN[sub] ?: sub
                val compLabel = if (sub == "ART" && comp == "TOTAL") "Total" else (COMP_EN[comp] ?: comp)
                "$subLabel (Term $term) - $compLabel"
            }
            CsvHeaderStyle.SYSTEM_KEYS -> key
        }
    }

    fun friendlyTextHeader(key: String, style: CsvHeaderStyle): String {
        if (style == CsvHeaderStyle.SYSTEM_KEYS) return key
        val parts = key.split('_')
        if (parts.size != 2) return key
        val field = parts[0]
        val term = parts[1].toIntOrNull() ?: 1
        return when (style) {
            CsvHeaderStyle.FRIENDLY_BN -> "${termBn(term)} - ${TEXT_BN[field] ?: field}"
            CsvHeaderStyle.FRIENDLY_EN -> "Term $term - ${TEXT_EN[field] ?: field}"
            CsvHeaderStyle.SYSTEM_KEYS -> key
        }
    }

    private fun summaryHeaders(style: CsvHeaderStyle): List<String> = when (style) {
        CsvHeaderStyle.FRIENDLY_BN -> listOf("বার্ষিক গড় (%)", "সামগ্রিক গ্রেড")
        CsvHeaderStyle.FRIENDLY_EN -> listOf("Overall Avg (%)", "Overall Grade")
        CsvHeaderStyle.SYSTEM_KEYS -> emptyList()
    }

    fun header(
        customFields: List<CustomFieldDef> = emptyList(),
        style: CsvHeaderStyle = CsvHeaderStyle.FRIENDLY_BN,
        includeSummary: Boolean = false,
        targetCls: Int? = null
    ): List<String> {
        val baseHeaders = BASE.map { key ->
            when (style) {
                CsvHeaderStyle.FRIENDLY_BN -> BASE_BN[key] ?: key
                CsvHeaderStyle.FRIENDLY_EN -> BASE_EN[key] ?: key
                CsvHeaderStyle.SYSTEM_KEYS -> key
            }
        }
        val markKeys = if (style == CsvHeaderStyle.SYSTEM_KEYS && targetCls == null) {
            MARK_KEYS
        } else {
            exportMarkKeysForClass(targetCls)
        }
        val markHeaders = markKeys.map { friendlyMarkHeader(it, style) }
        val textHeaders = TEXT_KEYS.map { friendlyTextHeader(it, style) }
        val summary = if (includeSummary) summaryHeaders(style) else emptyList()
        return baseHeaders + customFields.map { it.label } + summary + markHeaders + textHeaders
    }

    fun toCsvTemplate(
        customFields: List<CustomFieldDef> = emptyList(),
        style: CsvHeaderStyle = CsvHeaderStyle.FRIENDLY_BN,
        targetCls: Int? = null
    ): String {
        return "\uFEFF" + header(customFields, style, includeSummary = false, targetCls = targetCls)
            .joinToString(",") { q(it) } + "\n"
    }

    fun toCsvTable(
        list: List<Student>,
        customFields: List<CustomFieldDef> = emptyList(),
        style: CsvHeaderStyle = CsvHeaderStyle.FRIENDLY_BN,
        targetCls: Int? = null
    ): Pair<List<String>, List<List<String>>> {
        val includeSummary = style != CsvHeaderStyle.SYSTEM_KEYS
        val headers = header(customFields, style, includeSummary = includeSummary, targetCls = targetCls)
        val markKeyList = if (style == CsvHeaderStyle.SYSTEM_KEYS && targetCls == null) {
            MARK_KEYS
        } else {
            exportMarkKeysForClass(targetCls)
        }
        val filtered = if (targetCls != null) list.filter { it.cls == targetCls } else list
        val rows = filtered.sortedWith(compareBy({ it.cls }, { it.roll })).map { s ->
            val clsCell = when (style) {
                CsvHeaderStyle.FRIENDLY_BN -> "${classShortBn(s.cls)} শ্রেণি"
                CsvHeaderStyle.FRIENDLY_EN -> "Class ${s.cls}"
                CsvHeaderStyle.SYSTEM_KEYS -> s.cls.toString()
            }
            val summaryCells = if (includeSummary) {
                val hasEval = (1..3).any { s.hasTerm(it) }
                if (hasEval) {
                    val pct = s.overallPct()
                    val g = gradeOf(pct)
                    val gText = if (style == CsvHeaderStyle.FRIENDLY_BN) "$g (${gradeLabelBn(g)})" else g
                    listOf(fmtEn(pct), gText)
                } else {
                    listOf("", "")
                }
            } else {
                emptyList()
            }
            val subs = s.subs()
            listOf(
                clsCell,
                s.roll.toString(),
                s.name,
                s.guardian,
                s.village,
                s.union,
                s.upazila.ifBlank { DEFAULT_UPAZILA },
                s.district.ifBlank { DEFAULT_DISTRICT },
                s.mobile,
                s.religion
            ) +
                customFields.map { cf -> s.x[cf.key] ?: "" } +
                summaryCells +
                markKeyList.map { k ->
                    val explicit = s.m[k]
                    if (explicit != null) {
                        fmtEn(explicit)
                    } else if (style != CsvHeaderStyle.SYSTEM_KEYS) {
                        val parts = k.split('_')
                        val subKey = parts.getOrNull(0)
                        val term = parts.getOrNull(1)?.toIntOrNull()
                        val comp = parts.getOrNull(2)
                        val subObj = if (subKey != null) subs.firstOrNull { it.key == subKey } else null
                        if (subObj != null && term != null && s.hasSub(subObj, term)) {
                            when (comp) {
                                "CONT" -> fmtEn(s.cont(subObj, term))
                                "TOTAL" -> fmtEn(s.total(subObj, term))
                                else -> ""
                            }
                        } else ""
                    } else ""
                } +
                TEXT_KEYS.map { k -> s.x[k] ?: "" }
        }
        return headers to rows
    }

    fun toCsv(
        list: List<Student>,
        customFields: List<CustomFieldDef> = emptyList(),
        style: CsvHeaderStyle = CsvHeaderStyle.FRIENDLY_BN,
        targetCls: Int? = null
    ): String {
        val (headers, rows) = toCsvTable(list, customFields, style, targetCls)
        val sb = StringBuilder("\uFEFF")
        sb.append(headers.joinToString(",") { q(it) }).append("\n")
        rows.forEach { row ->
            sb.append(row.joinToString(",") { q(it) }).append("\n")
        }
        return sb.toString()
    }

    fun toPerClassCsvZip(
        list: List<Student>,
        customFields: List<CustomFieldDef> = emptyList(),
        style: CsvHeaderStyle = CsvHeaderStyle.FRIENDLY_BN,
        prefix: String = "Class",
        classes: List<Int> = activeClassIds()
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            classes.forEach { c ->
                val csv = toCsv(list, customFields, style, targetCls = c)
                zos.putNextEntry(ZipEntry("${prefix}_$c.csv"))
                zos.write(csv.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    private fun detectDelimiter(t: String): Char {
        val firstLine = t.lineSequence().firstOrNull() ?: ""
        var inQ = false; var commas = 0; var tabs = 0; var semis = 0
        for (c in firstLine) {
            if (c == '"') inQ = !inQ
            else if (!inQ) when (c) {
                ',' -> commas++
                '\t' -> tabs++
                ';' -> semis++
            }
        }
        return when {
            tabs >= commas && tabs >= semis && tabs > 0 -> '\t'
            semis > commas && semis > 0 -> ';'
            else -> ','
        }
    }

    fun stripCsvCodeFences(raw: String): String {
        val cleaned = raw.removePrefix("\uFEFF").trim()
        val fenceRegex = Regex("```(?:csv|tsv|txt|text|excel|markdown|md|table)?\\s*\\n([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
        val match = fenceRegex.find(cleaned)
        if (match != null) {
            return match.groupValues[1].trim()
        }
        return cleaned
            .replace(Regex("^```(?:csv|tsv|txt|text|excel|markdown|md|table)?\\s*\\n?", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\n?```\\s*$"), "")
            .trim()
    }

    private fun tryParseMarkdownTable(t: String): List<List<String>>? {
        val lines = t.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size < 2) return null
        val firstPipeIdx = lines.indexOfFirst { it.startsWith("|") && it.endsWith("|") }
        if (firstPipeIdx < 0) return null
        val tailLines = lines.drop(firstPipeIdx)
        val pipeLines = tailLines.count { it.startsWith("|") && it.endsWith("|") }
        if (pipeLines < 2 || pipeLines < (tailLines.size * 2) / 3) return null
        val dividerRegex = Regex("^\\|?(?:\\s*:?-{2,}:?\\s*\\|)+\\s*$")
        val rows = lines
            .filter { !dividerRegex.matches(it) }
            .map { line ->
                if (line.startsWith("|") && line.endsWith("|")) {
                    line.removePrefix("|").removeSuffix("|").split("|").map { it.trim() }
                } else {
                    val splitSep = if (line.contains('\t')) '\t' else if (line.contains(';') && !line.contains(',')) ';' else ','
                    line.split(splitSep).map { it.trim().trim('"') }
                }
            }
            .filter { r -> r.any { it.isNotBlank() } }
        return rows.takeIf { it.size >= 2 }
    }

    fun parseCsv(t0: String): List<List<String>> {
        val t = stripCsvCodeFences(t0)
        if (t.isBlank()) return emptyList()
        tryParseMarkdownTable(t)?.let { return it }
        val sep = detectDelimiter(t)
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val sb = StringBuilder(); var inQ = false; var i = 0
        while (i < t.length) {
            val c = t[i]
            if (inQ) {
                if (c == '"') { if (i + 1 < t.length && t[i + 1] == '"') { sb.append('"'); i++ } else inQ = false } else sb.append(c)
            } else when (c) {
                '"' -> inQ = true
                sep -> { row.add(sb.toString()); sb.clear() }
                '\n' -> { row.add(sb.toString()); sb.clear(); rows.add(row); row = mutableListOf() }
                '\r' -> {}
                else -> sb.append(c)
            }
            i++
        }
        if (sb.isNotEmpty() || row.isNotEmpty()) { row.add(sb.toString()); rows.add(row) }
        return rows.filter { r -> r.any { it.isNotBlank() } }
    }

    fun isBlankOrDashPlaceholder(raw: String): Boolean {
        val v = raw.trim().lowercase()
        return v.isEmpty() || v in setOf("-", "—", "–", "--", "n/a", "na", "nil", "none", "নাই", "নেই", "প্রযোজ্য নয়", "প্রযোজ্য নহে")
    }

    fun parseClassNum(raw: String): Int? {
        val s = fromBn(raw).trim().lowercase()
        if (s.isBlank()) return null
        s.toIntOrNull()?.takeIf { it in 1..20 }?.let { return it }
        SchoolClassRegistry.currentCustom().forEach { cc ->
            val nameNorm = fromBn(cc.nameBn).trim().lowercase()
            val shortNorm = fromBn(cc.shortBn).trim().lowercase()
            if ((nameNorm.isNotBlank() && s.contains(nameNorm)) ||
                (shortNorm.isNotBlank() && s.contains(shortNorm))) {
                return cc.id
            }
        }
        if (s.contains("প্রথম") || s.contains("১ম") || s.contains("1st") || s.contains("class 1 ")) return 1
        if (s.contains("দ্বিতীয়") || s.contains("দ্বিতীয়") || s.contains("২য়") || s.contains("২য়") || s.contains("2nd") || s.contains("class 2")) return 2
        if (s.contains("তৃতীয়") || s.contains("তৃতীয়") || s.contains("৩য়") || s.contains("৩য়") || s.contains("3rd") || s.contains("class 3")) return 3
        if (s.contains("চতুর্থ") || s.contains("৪র্থ") || s.contains("4th") || s.contains("class 4")) return 4
        if (s.contains("পঞ্চম") || s.contains("৫ম") || s.contains("5th") || s.contains("class 5")) return 5
        if (s.contains("ষষ্ঠ") || s.contains("৬ষ্ঠ") || s.contains("6th") || s.contains("class 6")) {
            return SchoolClassRegistry.activeClasses().firstOrNull { it.nameBn.contains("৬ষ্ঠ") || it.id == 6 }?.id ?: 6
        }
        if (s.contains("সপ্তম") || s.contains("৭ম") || s.contains("7th") || s.contains("class 7")) {
            return SchoolClassRegistry.activeClasses().firstOrNull { it.nameBn.contains("৭ম") || it.id == 7 }?.id ?: 7
        }
        if (s.contains("অষ্টম") || s.contains("৮ম") || s.contains("8th") || s.contains("class 8")) {
            return SchoolClassRegistry.activeClasses().firstOrNull { it.nameBn.contains("৮ম") || it.id == 8 }?.id ?: 8
        }
        if (s.contains("প্রাক") || s.contains("শিশু") || s.contains("pre")) {
            return SchoolClassRegistry.currentCustom().firstOrNull {
                it.nameBn.contains("প্রাক") || it.nameBn.contains("শিশু")
            }?.id
        }
        val numToken = Regex("\\b([1-9]|1[0-9]|20)\\b").find(s)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (numToken != null && numToken in 1..20) return numToken
        return null
    }

    fun parseSubjectKey(raw: String): String? {
        val s = normHeader(fromBn(raw))
        if (s.isBlank()) return null
        return when {
            s.contains("বাংলাদেশ") || s.contains("বিশ্বপরিচ") || s.contains("বাওবি") ||
                s.contains("bgw") || s.contains("bgs") || s.contains("bangladesh") || s.contains("global") -> "BGW"
            s.contains("সামাজিক") || s.contains("সমন্বিত") || s.contains("social") || s == "soc" -> "SOC"
            s.contains("শিল্পকলা") || s.contains("চারু") || s.contains("কারুকলা") || s == "art" || s.contains("arts") -> "ART"
            s.contains("শারীরিক") || s.contains("মানসিক স্বাস্থ্য") || s.contains("স্বাস্থ্য শিক্ষা") ||
                s.contains("physical") || s == "pe" -> "PE"
            s.contains("ধর্ম") || s.contains("ইসলাম") || s.contains("হিন্দু") || s.contains("বৌদ্ধ") ||
                s.contains("খ্রীষ্ট") || s.contains("খ্রিষ্ট") || s.contains("religion") || s == "rel" -> "REL"
            s.contains("বিজ্ঞান") || s.contains("science") || s == "sci" -> "SCI"
            s.contains("গণিত") || s.contains("গনিত") || s.contains("অঙ্ক") || s.contains("অংক") ||
                s.contains("math") -> "MATH"
            s.contains("ইংরেজ") || s.contains("english") || s == "eng" -> "ENG"
            s.contains("বাংলা") || s.contains("bangla") || s.contains("bengali") || s == "ban" -> "BAN"
            else -> null
        }
    }

    val STANDARD_FIELDS: List<Pair<String, String>> = listOf(
        "cls" to "শ্রেণি (cls)",
        "roll" to "রোল নম্বর (roll)",
        "name" to "শিক্ষার্থীর নাম (name)",
        "guardian" to "পিতা / অভিভাবকের নাম (guardian)",
        "mobile" to "মোবাইল নম্বর (mobile)",
        "village" to "গ্রাম (village)",
        "union" to "ইউনিয়ন (union)",
        "upazila" to "উপজেলা (upazila)",
        "district" to "জেলা (district)",
        "religion" to "ধর্ম (religion)"
    )

    private fun normHeader(h: String): String = h.trim().lowercase()
        .replace(Regex("[?:*।.,()_\\-]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private val FIELD_ALIASES: Map<String, List<String>> = mapOf(
        "cls" to listOf("cls", "class", "class name", "শ্রেণি", "শ্রেণী", "শ্রেণি নাম"),
        "roll" to listOf("roll", "roll no", "roll number", "রোল", "রোল নম্বর", "রোল নং"),
        "name" to listOf("name", "student name", "শিক্ষার্থীর নাম", "নাম", "ছাত্র ছাত্রীর নাম"),
        "guardian" to listOf("guardian", "guardian name", "father", "father name", "fathers name", "father's name", "অভিভাবক", "অভিভাবকের নাম", "পিতার নাম", "পিতা অভিভাবক", "পিতা/অভিভাবকের নাম", "পিতা অভিভাবকের নাম"),
        "mobile" to listOf("mobile", "mobile no", "mobile number", "phone", "phone number", "contact", "মোবাইল", "মোবাইল নম্বর", "মোবাইল নং", "ফোন", "অভিভাবকের মোবাইল"),
        "village" to listOf("village", "গ্রাম", "গ্রাম মহল্লা", "মহল্লা"),
        "union" to listOf("union", "union parishad", "up", "ইউনিয়ন", "ইউনিয়ন", "ইউপি"),
        "upazila" to listOf("upazila", "thana", "উপজেলা", "থানা"),
        "district" to listOf("district", "zilla", "জেলা"),
        "religion" to listOf("religion", "ধর্ম")
    )

    private val FRIENDLY_HEADER_REVERSE: Map<String, Pair<String, String>> by lazy {
        val map = mutableMapOf<String, Pair<String, String>>()
        listOf(CsvHeaderStyle.FRIENDLY_BN, CsvHeaderStyle.FRIENDLY_EN).forEach { st ->
            MARK_KEYS.forEach { key ->
                map[normHeader(friendlyMarkHeader(key, st))] = "mark" to key
            }
            TEXT_KEYS.forEach { key ->
                map[normHeader(friendlyTextHeader(key, st))] = "text" to key
            }
            summaryHeaders(st).forEach { sumHdr ->
                map[normHeader(sumHdr)] = "skip" to ""
            }
        }
        map
    }

    data class CsvParsedSheet(
        val rows: List<List<String>>,
        val detectedCls: Int?,
        val detectedSubjectKey: String?,
        val detectedTerm: Int?,
        val usedTwoRowHeader: Boolean,
        val skippedMetadataLines: Int
    )

    private fun parseTermFromToken(raw: String): Int? {
        val s = normHeader(fromBn(raw))
        if (s.isBlank()) return null
        return when {
            s.contains("১ম") || s.contains("প্রথম") || s.contains("1st") ||
                Regex("\\b(t1|term 1|1)\\b").containsMatchIn(s) && (s.contains("প্রান্তিক") || s.contains("term") || s.contains("t1")) -> 1
            s.contains("২য়") || s.contains("২য়") || s.contains("দ্বিতীয়") || s.contains("দ্বিতীয়") || s.contains("2nd") ||
                Regex("\\b(t2|term 2|2)\\b").containsMatchIn(s) && (s.contains("প্রান্তিক") || s.contains("term") || s.contains("t2")) -> 2
            s.contains("৩য়") || s.contains("৩য়") || s.contains("তৃতীয়") || s.contains("তৃতীয়") || s.contains("3rd") ||
                Regex("\\b(t3|term 3|3)\\b").containsMatchIn(s) && (s.contains("প্রান্তিক") || s.contains("term") || s.contains("t3")) -> 3
            s.contains("১ম প্রান্তিক") || s.contains("প্রান্তিক ১") -> 1
            s.contains("২য় প্রান্তিক") || s.contains("২য় প্রান্তিক") || s.contains("প্রান্তিক ২") -> 2
            s.contains("৩য় প্রান্তিক") || s.contains("৩য় প্রান্তিক") || s.contains("প্রান্তিক ৩") -> 3
            else -> null
        }
    }

    private fun isSubHeaderKeyword(cell: String): Boolean {
        val n = normHeader(fromBn(cell))
        if (n.isBlank()) return false
        return n.contains("ধারাবাহিক") || n.contains("continuous") || n == "cont" || n == "ca" ||
            n.contains("সামষ্টিক") || n.contains("সামস্টিক") || n.contains("লিখিত") || n.contains("summative") || n.contains("written") || n == "wr" || n == "sa" ||
            n.contains("পাঠ্যপুস্তক") || n.contains("সহায়িকা") || n.contains("textbook") || n == "tb" ||
            n.contains("সক্রিয়তা") || n.contains("শ্রেণিকাজ") || n.contains("activity") || n == "act" ||
            n.contains("ভাষাদক্ষতা") || n.contains("বোধগম্যতা") || n.contains("language") || n == "lang" ||
            n.contains("ক্লাস টেস্ট") || n.contains("class test") || n == "ct" ||
            n == "মোট" || n == "সর্বমোট" || n == "total" || n.contains("মোট নম্বর") ||
            n.contains("কার্যদিবস") || n.contains("উপস্থিতি") || n.contains("মন্তব্য") ||
            n.contains("শৃঙ্খলা") || n.contains("আচরণ")
    }

    private fun isMetadataBannerRow(row: List<String>): Boolean {
        val nonBlank = row.map { it.trim() }.filter { it.isNotBlank() }
        if (nonBlank.isEmpty()) return false
        val joined = normHeader(fromBn(nonBlank.joinToString(" ")))
        val hasRollCol = nonBlank.any { normHeader(it) in FIELD_ALIASES["roll"].orEmpty().map(::normHeader) }
        val hasNameCol = nonBlank.any { normHeader(it) in FIELD_ALIASES["name"].orEmpty().map(::normHeader) }
        if (hasRollCol || hasNameCol) return false
        val hasKnownMarkOrTextKey = nonBlank.any { MARK_KEYS.contains(it) || TEXT_KEYS.contains(it) || FRIENDLY_HEADER_REVERSE.containsKey(normHeader(it)) }
        if (hasKnownMarkOrTextKey) return false
        // Top rows like "শ্রেণি: ৩য়, বিষয়: বাংলা" or "শ্রেণি, ৩য়, বিষয়, বাংলা" or school name title
        return joined.contains("শ্রেণি") || joined.contains("শ্রেণী") || joined.contains("class") ||
            joined.contains("বিষয়") || joined.contains("বিষয়") || joined.contains("subject") ||
            joined.contains("প্রান্তিক") || joined.contains("বিদ্যালয়") || joined.contains("বিদ্যালয়") ||
            joined.contains("স্কুল") || joined.contains("school") || nonBlank.size <= 2
    }

    private fun extractMetadataFromTopRows(metaRows: List<List<String>>): Triple<Int?, String?, Int?> {
        var clsFound: Int? = null
        var subFound: String? = null
        var termFound: Int? = null

        fun inspectPair(labelRaw: String, valueRaw: String?) {
            val lNorm = normHeader(fromBn(labelRaw))
            val combined = if (valueRaw != null) "$labelRaw $valueRaw" else labelRaw
            if (lNorm.contains("শ্রেণি") || lNorm.contains("শ্রেণী") || lNorm.contains("class")) {
                val cand = valueRaw?.takeIf { it.isNotBlank() } ?: labelRaw.substringAfter(":", "").ifBlank { labelRaw }
                parseClassNum(cand)?.let { if (clsFound == null) clsFound = it }
            }
            if (lNorm.contains("বিষয়") || lNorm.contains("বিষয়") || lNorm.contains("subject")) {
                val cand = valueRaw?.takeIf { it.isNotBlank() } ?: labelRaw.substringAfter(":", "").ifBlank { labelRaw }
                parseSubjectKey(cand)?.let { if (subFound == null) subFound = it }
            }
            if (lNorm.contains("প্রান্তিক") || lNorm.contains("term")) {
                val cand = valueRaw?.takeIf { it.isNotBlank() } ?: labelRaw.substringAfter(":", "").ifBlank { labelRaw }
                parseTermFromToken(cand)?.let { if (termFound == null) termFound = it }
            }
            // Also check single free-form cell e.g. "৩য় শ্রেণি - বাংলা"
            if (clsFound == null && (lNorm.contains("শ্রেণি") || lNorm.contains("শ্রেণী") || lNorm.contains("class"))) {
                parseClassNum(combined)?.let { clsFound = it }
            }
            if (subFound == null && !lNorm.contains("শ্রেণি") && !lNorm.contains("শ্রেণী") && !lNorm.contains("প্রান্তিক")) {
                parseSubjectKey(labelRaw)?.let { subFound = it }
            }
        }

        metaRows.forEach { row ->
            val trimmed = row.map { it.trim() }
            var i = 0
            while (i < trimmed.size) {
                val cell = trimmed[i]
                if (cell.isBlank()) {
                    i++; continue
                }
                // Check colon or equals key:value in one cell, or key|value in adjacent cells
                val splitColon = Regex("[:=：]").split(cell, limit = 2)
                if (splitColon.size == 2 && splitColon[1].isNotBlank()) {
                    inspectPair(splitColon[0], splitColon[1])
                } else {
                    val nextCell = trimmed.getOrNull(i + 1)?.takeIf { it.isNotBlank() }
                    val cNorm = normHeader(fromBn(cell))
                    val isKeyLabel = cNorm in setOf("শ্রেণি", "শ্রেণী", "class", "বিষয়", "বিষয়", "subject", "প্রান্তিক", "term")
                    if (isKeyLabel && nextCell != null) {
                        inspectPair(cell, nextCell)
                        i++
                    } else {
                        // Could still have multiple key:val chunks separated by | or -
                        cell.split("|", "•", "·").forEach { chunk ->
                            val subColon = Regex("[:=：]").split(chunk.trim(), limit = 2)
                            if (subColon.size == 2) inspectPair(subColon[0], subColon[1])
                            else inspectPair(chunk.trim(), null)
                        }
                    }
                }
                i++
            }
        }
        return Triple(clsFound, subFound, termFound)
    }

    fun parseSmartCsvSheet(rawText: String): CsvParsedSheet {
        val rawRows = parseCsv(rawText)
        if (rawRows.isEmpty()) {
            return CsvParsedSheet(emptyList(), null, null, null, usedTwoRowHeader = false, skippedMetadataLines = 0)
        }

        // Find the start of table headers within the first few rows
        val scanLimit = minOf(rawRows.size, 8)
        var headerIdx = 0
        while (headerIdx < scanLimit - 1 && isMetadataBannerRow(rawRows[headerIdx])) {
            val next = rawRows.getOrNull(headerIdx + 1) ?: break
            val nextJoined = normHeader(fromBn(next.joinToString(" ")))
            val nextHasHeaderSignal = next.any {
                val n = normHeader(it)
                n in FIELD_ALIASES["roll"].orEmpty().map(::normHeader) ||
                    n in FIELD_ALIASES["name"].orEmpty().map(::normHeader) ||
                    isSubHeaderKeyword(it) ||
                    parseTermFromToken(it) != null ||
                    MARK_KEYS.contains(it.trim()) ||
                    TEXT_KEYS.contains(it.trim()) ||
                    FRIENDLY_HEADER_REVERSE.containsKey(n)
            } || nextJoined.contains("প্রান্তিক")
            if (isMetadataBannerRow(rawRows[headerIdx]) && nextHasHeaderSignal) {
                headerIdx++
            } else if (isMetadataBannerRow(rawRows[headerIdx])) {
                // Keep scanning only if a later row in scanLimit explicitly has roll or name
                val laterHasTableHeader = ((headerIdx + 1) until scanLimit).any { idx ->
                    val r = rawRows[idx]
                    r.any {
                        val n = normHeader(it)
                        n in FIELD_ALIASES["roll"].orEmpty().map(::normHeader) ||
                            n in FIELD_ALIASES["name"].orEmpty().map(::normHeader)
                    }
                }
                if (laterHasTableHeader) headerIdx++ else break
            } else {
                break
            }
        }

        val (detectedCls, detectedSubjectKey, detectedTerm) = if (headerIdx > 0) {
            extractMetadataFromTopRows(rawRows.take(headerIdx))
        } else {
            Triple(null, null, null)
        }

        val hRow = rawRows[headerIdx]
        val subRow = rawRows.getOrNull(headerIdx + 1)
        val hasSecondHeaderRow = if (subRow != null && headerIdx + 2 <= rawRows.size) {
            val subKeywords = subRow.count { isSubHeaderKeyword(it) }
            val rollColInH = hRow.indexOfFirst { normHeader(it) in FIELD_ALIASES["roll"].orEmpty().map(::normHeader) }
            val rollCellInSub = if (rollColInH >= 0) subRow.getOrNull(rollColInH)?.trim().orEmpty() else ""
            val subLooksLikeStudentRow = fromBn(rollCellInSub).toIntOrNull()?.let { it > 0 } == true
            subKeywords >= 2 && !subLooksLikeStudentRow
        } else false

        if (!hasSecondHeaderRow) {
            return CsvParsedSheet(
                rows = rawRows.drop(headerIdx),
                detectedCls = detectedCls,
                detectedSubjectKey = detectedSubjectKey,
                detectedTerm = detectedTerm,
                usedTwoRowHeader = false,
                skippedMetadataLines = headerIdx
            )
        }

        val sRow = subRow!!
        val maxCols = maxOf(hRow.size, sRow.size)
        val combinedHeaders = MutableList(maxCols) { "" }
        var carriedParent = ""
        for (c in 0 until maxCols) {
            val p = hRow.getOrNull(c)?.trim().orEmpty()
            val sub = sRow.getOrNull(c)?.trim().orEmpty()
            val effectiveParent = if (p.isNotBlank()) {
                val pNorm = normHeader(p)
                val isIdentityHeader = pNorm in (FIELD_ALIASES["roll"].orEmpty() + FIELD_ALIASES["name"].orEmpty() + FIELD_ALIASES["cls"].orEmpty()).map(::normHeader)
                carriedParent = if (isIdentityHeader) "" else p
                p
            } else if (sub.isNotBlank()) {
                carriedParent
            } else {
                ""
            }

            combinedHeaders[c] = when {
                effectiveParent.isNotBlank() && sub.isNotBlank() && normHeader(effectiveParent) != normHeader(sub) -> {
                    val pNorm = normHeader(effectiveParent)
                    val sNorm = normHeader(sub)
                    if (sNorm in (FIELD_ALIASES["roll"].orEmpty() + FIELD_ALIASES["name"].orEmpty() + FIELD_ALIASES["cls"].orEmpty()).map(::normHeader)) {
                        sub
                    } else if (pNorm in (FIELD_ALIASES["roll"].orEmpty() + FIELD_ALIASES["name"].orEmpty() + FIELD_ALIASES["cls"].orEmpty()).map(::normHeader)) {
                        effectiveParent
                    } else {
                        "$effectiveParent - $sub"
                    }
                }
                sub.isNotBlank() -> sub
                else -> effectiveParent
            }
        }

        val dataRows = rawRows.drop(headerIdx + 2)
        return CsvParsedSheet(
            rows = listOf(combinedHeaders) + dataRows,
            detectedCls = detectedCls,
            detectedSubjectKey = detectedSubjectKey,
            detectedTerm = detectedTerm,
            usedTwoRowHeader = true,
            skippedMetadataLines = headerIdx
        )
    }

    fun makeCustomFieldKey(label: String, existingKeys: Set<String>): String {
        val ascii = label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        val base = if (ascii.isNotBlank()) "cf_$ascii" else "cf_${(label.hashCode().toUInt()).toString(36)}"
        var candidate = base
        var n = 2
        while (existingKeys.contains(candidate)) candidate = "${base}_${n++}"
        return candidate
    }

    data class CsvColMapping(
        val colIndex: Int,
        val header: String,
        val samples: List<String>,
        val isNew: Boolean,
        val targetType: String, // "standard", "mark", "text", "custom", "create_new", "skip"
        val targetKey: String,  // field key or mark/text/custom key
        val newLabel: String    // label when targetType == "create_new"
    )

    data class CsvImportSummary(
        val students: List<Student>,
        val settings: Settings,
        val addedCount: Int,
        val updatedCount: Int,
        val createdFields: List<CustomFieldDef>
    )

    private fun matchSemanticColumn(
        header: String,
        contextSubjectKey: String?,
        contextTerm: Int?
    ): Pair<String, String>? {
        val norm = normHeader(fromBn(header))
        if (norm.isBlank()) return null

        // Auto-skip common read-only summary columns when re-importing exported sheets
        if (norm in setOf("গড় নম্বর", "মোট প্রাপ্ত নম্বর", "শতকরা হার", "অবস্থানগত মান", "মেধাক্রম", "average marks", "total obtained", "percentage", "grade position", "merit rank")) {
            return "skip" to ""
        }

        val term = parseTermFromToken(header) ?: contextTerm

        // Attendance / Traits / Comments with optional term
        if (term != null) {
            when {
                norm.contains("কার্যদিবস") || norm.contains("working day") -> return "text" to "DAYS_$term"
                norm.contains("উপস্থিত") || norm.contains("attendance") || norm.contains("present") -> return "text" to "PRES_$term"
                norm.contains("সহপাঠক্রমিক") || norm.contains("সহপাঠ্য") || norm.contains("co curricular") -> return "text" to "ACT_$term"
                norm.contains("শৃঙ্খলা") || norm.contains("discipline") -> return "text" to "DISC_$term"
                norm.contains("আচরণ") || norm.contains("conduct") -> return "text" to "COND_$term"
                norm.contains("মন্তব্য") || norm.contains("মতামত") || norm.contains("remark") || norm.contains("comment") -> return "text" to "REM_$term"
            }
        }

        val subKey = parseSubjectKey(header) ?: contextSubjectKey
        val compKey = when {
            norm.contains("পাঠ্যপুস্তক") || norm.contains("সহায়িকা") || norm.contains("textbook") || Regex("\\btb\\b").containsMatchIn(norm) -> "TB"
            norm.contains("শ্রেণিকাজ") || norm.contains("সক্রিয়তা") || norm.contains("activity") || Regex("\\bact\\b").containsMatchIn(norm) -> "ACT"
            norm.contains("ভাষাদক্ষতা") || norm.contains("বোধগম্যতা") || norm.contains("language") || Regex("\\blang\\b").containsMatchIn(norm) -> "LANG"
            norm.contains("ক্লাস টেস্ট") || norm.contains("class test") || Regex("\\bct\\b").containsMatchIn(norm) -> "CT"
            norm.contains("ধারাবাহিক") || norm.contains("continuous") || Regex("\\b(cont|ca)\\b").containsMatchIn(norm) -> "CONT"
            norm.contains("সামষ্টিক") || norm.contains("সামস্টিক") || norm.contains("লিখিত") || norm.contains("summative") || norm.contains("written") || Regex("\\b(wr|sa)\\b").containsMatchIn(norm) -> "WR"
            norm == "মোট" || norm == "সর্বমোট" || norm == "total" || norm.endsWith(" মোট") || norm.contains("মোট নম্বর") || norm.contains("বিষয় মোট") -> "TOTAL"
            else -> null
        }

        if (subKey != null && term != null && term in 1..3 && compKey != null) {
            return "mark" to mk(subKey, term, compKey)
        }
        return null
    }

    fun detectCsvColumns(
        rows: List<List<String>>,
        existingCustomFields: List<CustomFieldDef>,
        contextSubjectKey: String? = null,
        contextTerm: Int? = null
    ): List<CsvColMapping> {
        if (rows.isEmpty()) return emptyList()
        val headers = rows[0]
        val dataRows = rows.drop(1)
        val markSet = MARK_KEYS.toSet()
        val textSet = TEXT_KEYS.toSet()

        return headers.mapIndexed { colIndex, rawHeader ->
            val header = rawHeader.trim().ifBlank { "কলাম_${colIndex + 1}" }
            val norm = normHeader(header)
            val samples = dataRows.mapNotNull { r -> r.getOrNull(colIndex)?.trim()?.takeIf { it.isNotBlank() } }.take(2)

            val matchedStd = FIELD_ALIASES.entries.firstOrNull { (_, aliases) ->
                aliases.any { normHeader(it) == norm }
            }?.key
            val friendlyMatch = FRIENDLY_HEADER_REVERSE[norm]
            val semanticMatch = if (matchedStd == null && friendlyMatch == null) {
                matchSemanticColumn(header, contextSubjectKey, contextTerm)
            } else null

            when {
                matchedStd != null -> {
                    CsvColMapping(colIndex, header, samples, isNew = false, targetType = "standard", targetKey = matchedStd, newLabel = header)
                }
                markSet.contains(header) -> {
                    CsvColMapping(colIndex, header, samples, isNew = false, targetType = "mark", targetKey = header, newLabel = header)
                }
                textSet.contains(header) -> {
                    CsvColMapping(colIndex, header, samples, isNew = false, targetType = "text", targetKey = header, newLabel = header)
                }
                friendlyMatch != null -> {
                    val (tType, tKey) = friendlyMatch
                    CsvColMapping(colIndex, header, samples, isNew = false, targetType = tType, targetKey = tKey, newLabel = header)
                }
                semanticMatch != null -> {
                    val (tType, tKey) = semanticMatch
                    CsvColMapping(colIndex, header, samples, isNew = false, targetType = tType, targetKey = tKey, newLabel = header)
                }
                else -> {
                    val existingCf = existingCustomFields.find { it.key == header || normHeader(it.label) == norm }
                    if (existingCf != null) {
                        CsvColMapping(colIndex, header, samples, isNew = false, targetType = "custom", targetKey = existingCf.key, newLabel = existingCf.label)
                    } else {
                        CsvColMapping(colIndex, header, samples, isNew = true, targetType = "create_new", targetKey = "", newLabel = header)
                    }
                }
            }
        }
    }

    fun applyMappedCsvImport(
        rows: List<List<String>>,
        columns: List<CsvColMapping>,
        existingStudents: List<Student>,
        existingSettings: Settings,
        defaultCls: Int = 1
    ): CsvImportSummary {
        if (rows.size < 2) return CsvImportSummary(existingStudents, existingSettings, 0, 0, emptyList())

        val nextCustomFields = existingSettings.customFields.toMutableList()
        val existingKeySet = (nextCustomFields.map { it.key } + TEXT_KEYS).toMutableSet()
        val createdFields = mutableListOf<CustomFieldDef>()

        val resolvedCols = columns.map { col ->
            if (col.targetType == "create_new") {
                val label = col.newLabel.trim().ifBlank { col.header }
                val already = nextCustomFields.find { normHeader(it.label) == normHeader(label) }
                if (already != null) {
                    col.copy(targetType = "custom", targetKey = already.key)
                } else {
                    val key = makeCustomFieldKey(label, existingKeySet)
                    existingKeySet.add(key)
                    val def = CustomFieldDef(key, label)
                    nextCustomFields.add(def)
                    createdFields.add(def)
                    col.copy(targetType = "custom", targetKey = key)
                }
            } else col
        }

        val out = existingStudents.toMutableList()
        var addedCount = 0
        var updatedCount = 0
        val autoRollByCls = mutableMapOf<Int, Int>()

        for (r in rows.drop(1)) {
            if (r.all { it.isBlank() }) continue
            var clsVal: Int? = null
            var rollVal: Int? = null
            val stdMap = mutableMapOf<String, String>()
            val importedMarks = mutableMapOf<String, Double>()
            val x = mutableMapOf<String, String>()

            for (col in resolvedCols) {
                if (col.targetType == "skip") continue
                val raw = r.getOrNull(col.colIndex)?.trim().orEmpty()
                if (raw.isBlank()) continue
                when (col.targetType) {
                    "standard" -> when (col.targetKey) {
                        "cls" -> clsVal = parseClassNum(raw)
                        "roll" -> rollVal = fromBn(raw).toIntOrNull()?.takeIf { it > 0 }
                        else -> {
                            val prev = stdMap[col.targetKey]
                            stdMap[col.targetKey] = if (prev != null && prev != raw) "$prev, $raw" else raw
                        }
                    }
                    "mark" -> if (!isBlankOrDashPlaceholder(raw)) {
                        fromBn(raw).toDoubleOrNull()?.let { importedMarks[col.targetKey] = it }
                    }
                    "text", "custom" -> {
                        val prev = x[col.targetKey]
                        x[col.targetKey] = if (prev != null && prev != raw) "$prev, $raw" else raw
                    }
                }
            }

            val cls = clsVal ?: defaultCls
            if (cls !in 1..20) continue
            if (rollVal == null && stdMap["name"].isNullOrBlank()) continue
            val roll = rollVal ?: run {
                val nextAuto = (autoRollByCls[cls] ?: out.filter { it.cls == cls }.maxOfOrNull { it.roll } ?: 0) + 1
                autoRollByCls[cls] = nextAuto
                nextAuto
            }

            val pos = out.indexOfFirst { it.cls == cls && it.roll == roll }
            val base = if (pos >= 0) out[pos] else Student(UUID.randomUUID().toString(), cls, roll, "")
            val clsSubs = subjectsFor(cls, stdMap["religion"]?.ifBlank { base.religion } ?: base.religion)

            // Reconcile CONT vs granular components (TB/ACT/LANG/CT) & ignore WR on written==0 subjects
            val mergedMarks = base.m.toMutableMap()
            val cleanedImported = importedMarks.filter { (key, num) ->
                val parts = key.split("_")
                if (parts.size == 3 && parts[2] == "WR") {
                    val sub = clsSubs.find { it.key == parts[0] }
                    sub == null || sub.written > 0 || num > 0.0
                } else true
            }
            cleanedImported.keys.forEach { key ->
                val parts = key.split("_")
                if (parts.size == 3) {
                    val subKey = parts[0]
                    val term = parts[1].toIntOrNull()
                    val compKey = parts[2]
                    if (term != null) {
                        when (compKey) {
                            "CONT" -> listOf("TB", "ACT", "LANG", "CT", "TOTAL").forEach { mergedMarks.remove(mk(subKey, term, it)) }
                            "TB", "ACT", "LANG", "CT" -> {
                                mergedMarks.remove(mk(subKey, term, "CONT"))
                                mergedMarks.remove(mk(subKey, term, "TOTAL"))
                            }
                            "TOTAL" -> {
                                val hasGranularOrContInSameRow = listOf("TB", "ACT", "LANG", "CT", "CONT", "WR")
                                    .any { cleanedImported.containsKey(mk(subKey, term, it)) }
                                if (!hasGranularOrContInSameRow) {
                                    listOf("TB", "ACT", "LANG", "CT", "CONT", "WR").forEach { mergedMarks.remove(mk(subKey, term, it)) }
                                }
                            }
                        }
                    }
                }
            }
            mergedMarks.putAll(cleanedImported)

            val upd = base.copy(
                name = stdMap["name"]?.ifBlank { base.name } ?: base.name,
                guardian = stdMap["guardian"]?.ifBlank { base.guardian } ?: base.guardian,
                village = stdMap["village"]?.ifBlank { base.village } ?: base.village,
                union = stdMap["union"]?.ifBlank { base.union } ?: base.union,
                post = "",
                upazila = (stdMap["upazila"]?.ifBlank { base.upazila } ?: base.upazila).ifBlank { DEFAULT_UPAZILA },
                district = (stdMap["district"]?.ifBlank { base.district } ?: base.district).ifBlank { DEFAULT_DISTRICT },
                mobile = stdMap["mobile"]?.ifBlank { base.mobile } ?: base.mobile,
                religion = stdMap["religion"]?.ifBlank { base.religion } ?: base.religion,
                m = mergedMarks,
                x = base.x + x
            )
            if (pos >= 0) {
                out[pos] = upd; updatedCount++
            } else {
                out.add(upd); addedCount++
            }
        }

        return CsvImportSummary(
            students = out,
            settings = existingSettings.copy(customFields = nextCustomFields),
            addedCount = addedCount,
            updatedCount = updatedCount,
            createdFields = createdFields
        )
    }

    /** একই শ্রেণি+রোল থাকলে হালনাগাদ, না থাকলে নতুন শিক্ষার্থী যোগ */
    fun importCsv(txt: String, existing: List<Student>): List<Student> {
        val sheet = parseSmartCsvSheet(txt)
        if (sheet.rows.size < 2) return existing
        val cols = detectCsvColumns(sheet.rows, emptyList(), sheet.detectedSubjectKey, sheet.detectedTerm)
        return applyMappedCsvImport(sheet.rows, cols, existing, DEFAULT_SETTINGS, sheet.detectedCls ?: 1).students
    }

    private const val APP_UI_PREFS = "kgps_app_ui_prefs"
    private const val KEY_APP_UI_FONT = "app_ui_font"
    private const val KEY_PDF_FONT_OVERRIDE = "pdf_font_override"

    fun loadAppFont(ctx: Context): String {
        val saved = ctx.getSharedPreferences(APP_UI_PREFS, Context.MODE_PRIVATE)
            .getString(KEY_APP_UI_FONT, "system")
            ?.trim()
            .orEmpty()
        return if (APP_FONT_LIST.any { it.id == saved }) saved else "system"
    }

    fun saveAppFont(ctx: Context, fontId: String) {
        val valid = if (APP_FONT_LIST.any { it.id == fontId }) fontId else "system"
        ctx.getSharedPreferences(APP_UI_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_APP_UI_FONT, valid)
            .apply()
    }

    fun loadPdfFontOverride(ctx: Context): String? {
        val saved = ctx.getSharedPreferences(APP_UI_PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PDF_FONT_OVERRIDE, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return saved.takeIf { id -> PDF_FONT_LIST.any { it.id == id } }
    }

    fun savePdfFontOverride(ctx: Context, fontId: String) {
        val valid = if (PDF_FONT_LIST.any { it.id == fontId }) fontId else "noto_serif"
        ctx.getSharedPreferences(APP_UI_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PDF_FONT_OVERRIDE, valid)
            .apply()
    }
}
