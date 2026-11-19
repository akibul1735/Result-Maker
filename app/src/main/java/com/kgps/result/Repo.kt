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
        o.put("customFields", JSONArray().also { arr ->
            st.customFields.forEach { cf ->
                arr.put(JSONObject().apply { put("key", cf.key); put("label", cf.label) })
            }
        })
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
        val cfArr = o.optJSONArray("customFields")
        val customFields = if (cfArr != null) {
            (0 until cfArr.length()).mapNotNull { idx ->
                val obj = cfArr.optJSONObject(idx) ?: return@mapNotNull null
                val k = obj.optString("key")
                val l = obj.optString("label")
                if (k.isNotBlank() && l.isNotBlank()) CustomFieldDef(k, l) else null
            }
        } else emptyList()
        val st = Settings(
            school = o.optString("school"),
            schoolAddress = o.optString("schoolAddress"),
            year = o.optString("year", "২০২৬"),
            teachers = intMap(o.optJSONObject("teachers")), teacherMobiles = intMap(o.optJSONObject("teacherMobiles")),
            pdfFont = o.optString("pdfFont", "noto_serif").ifBlank { "noto_serif" },
            dpeLogo = if (rawLogo == "color") "color" else "simple",
            customFields = customFields
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

    fun header(customFields: List<CustomFieldDef> = emptyList()): List<String> =
        BASE + customFields.map { it.label } + MARK_KEYS + TEXT_KEYS

    fun toCsv(list: List<Student>, customFields: List<CustomFieldDef> = emptyList()): String {
        val sb = StringBuilder("\uFEFF")
        sb.append(header(customFields).joinToString(",") { q(it) }).append("\n")
        list.sortedWith(compareBy({ it.cls }, { it.roll })).forEach { s ->
            val row = listOf(s.cls.toString(), s.roll.toString(), s.name, s.guardian, s.village, s.post, s.upazila, s.district, s.mobile, s.religion) +
                customFields.map { cf -> s.x[cf.key] ?: "" } +
                MARK_KEYS.map { k -> s.m[k]?.let { fmtEn(it) } ?: "" } +
                TEXT_KEYS.map { k -> s.x[k] ?: "" }
            sb.append(row.joinToString(",") { q(it) }).append("\n")
        }
        return sb.toString()
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

    fun cleanCsvText(raw: String): String {
        var t = raw.removePrefix("\uFEFF").trim()
        if (t.startsWith("```")) {
            val firstNewline = t.indexOf('\n')
            if (firstNewline >= 0) {
                t = t.substring(firstNewline + 1)
            } else {
                t = t.removePrefix("```csv").removePrefix("```CSV").removePrefix("```")
            }
            if (t.trimEnd().endsWith("```")) {
                t = t.trimEnd().removeSuffix("```")
            }
        }
        return t.trim()
    }

    fun parseCsv(t0: String): List<List<String>> {
        val t = cleanCsvText(t0)
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

    fun parseClassNum(raw: String): Int? {
        val s = fromBn(raw).trim().lowercase()
        if (s.isBlank()) return null
        s.toIntOrNull()?.takeIf { it in 1..5 }?.let { return it }
        if (s.contains("প্রথম") || s.contains("১ম") || s.contains("1st") || s.contains("class 1")) return 1
        if (s.contains("দ্বিতীয়") || s.contains("দ্বিতীয়") || s.contains("২য়") || s.contains("২য়") || s.contains("2nd") || s.contains("class 2")) return 2
        if (s.contains("তৃতীয়") || s.contains("তৃতীয়") || s.contains("৩য়") || s.contains("৩য়") || s.contains("3rd") || s.contains("class 3")) return 3
        if (s.contains("চতুর্থ") || s.contains("৪র্থ") || s.contains("4th") || s.contains("class 4")) return 4
        if (s.contains("পঞ্চম") || s.contains("৫ম") || s.contains("5th") || s.contains("class 5")) return 5
        return s.firstOrNull { it in '1'..'5' }?.digitToInt()
    }

    val STANDARD_FIELDS: List<Pair<String, String>> = listOf(
        "cls" to "শ্রেণি (cls)",
        "roll" to "রোল নম্বর (roll)",
        "name" to "শিক্ষার্থীর নাম (name)",
        "guardian" to "পিতা / অভিভাবকের নাম (guardian)",
        "mobile" to "মোবাইল নম্বর (mobile)",
        "village" to "গ্রাম (village)",
        "post" to "পোস্ট অফিস (post)",
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
        "guardian" to listOf("guardian", "guardian name", "father", "father name", "fathers name", "father's name", "অভিভাবক", "অভিভাবকের নাম", "পিতার নাম", "পিতা অভিভাবক"),
        "mobile" to listOf("mobile", "mobile no", "mobile number", "phone", "phone number", "contact", "মোবাইল", "মোবাইল নম্বর", "মোবাইল নং", "ফোন", "অভিভাবকের মোবাইল"),
        "village" to listOf("village", "গ্রাম", "গ্রাম মহল্লা", "মহল্লা"),
        "post" to listOf("post", "post office", "পোস্ট", "পোস্ট অফিস", "ডাকঘর"),
        "upazila" to listOf("upazila", "thana", "উপজেলা", "থানা"),
        "district" to listOf("district", "zilla", "জেলা"),
        "religion" to listOf("religion", "ধর্ম")
    )

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

    fun detectCsvColumns(rows: List<List<String>>, existingCustomFields: List<CustomFieldDef>): List<CsvColMapping> {
        if (rows.isEmpty()) return emptyList()
        val headers = rows[0]
        val dataRows = rows.drop(1)
        val markLookup = MARK_KEYS.associateBy { it.uppercase() }
        val textLookup = TEXT_KEYS.associateBy { it.uppercase() }

        return headers.mapIndexed { colIndex, rawHeader ->
            val header = rawHeader.trim().ifBlank { "কলাম_${colIndex + 1}" }
            val norm = normHeader(header)
            val upperKey = header.uppercase().replace(Regex("\\s+"), "_")
            val samples = dataRows.mapNotNull { r -> r.getOrNull(colIndex)?.trim()?.takeIf { it.isNotBlank() } }.take(2)

            val matchedStd = FIELD_ALIASES.entries.firstOrNull { (_, aliases) ->
                aliases.any { normHeader(it) == norm }
            }?.key
            val matchedMark = markLookup[upperKey]
            val matchedText = textLookup[upperKey]

            if (matchedStd != null) {
                CsvColMapping(colIndex, header, samples, isNew = false, targetType = "standard", targetKey = matchedStd, newLabel = header)
            } else if (matchedMark != null) {
                CsvColMapping(colIndex, header, samples, isNew = false, targetType = "mark", targetKey = matchedMark, newLabel = header)
            } else if (matchedText != null) {
                CsvColMapping(colIndex, header, samples, isNew = false, targetType = "text", targetKey = matchedText, newLabel = header)
            } else {
                val existingCf = existingCustomFields.find { it.key == header || normHeader(it.label) == norm }
                if (existingCf != null) {
                    CsvColMapping(colIndex, header, samples, isNew = false, targetType = "custom", targetKey = existingCf.key, newLabel = existingCf.label)
                } else {
                    CsvColMapping(colIndex, header, samples, isNew = true, targetType = "create_new", targetKey = "", newLabel = header)
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
            val m = mutableMapOf<String, Double>()
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
                    "mark" -> fromBn(raw).toDoubleOrNull()?.let { m[col.targetKey] = it }
                    "text", "custom" -> {
                        val prev = x[col.targetKey]
                        x[col.targetKey] = if (prev != null && prev != raw) "$prev, $raw" else raw
                    }
                }
            }

            val cls = clsVal ?: defaultCls
            if (cls !in 1..5) continue
            if (rollVal == null && stdMap["name"].isNullOrBlank()) continue
            val roll = rollVal ?: run {
                val nextAuto = (autoRollByCls[cls] ?: out.filter { it.cls == cls }.maxOfOrNull { it.roll } ?: 0) + 1
                autoRollByCls[cls] = nextAuto
                nextAuto
            }

            val pos = out.indexOfFirst { it.cls == cls && it.roll == roll }
            val base = if (pos >= 0) out[pos] else Student(UUID.randomUUID().toString(), cls, roll, "")
            val upd = base.copy(
                name = stdMap["name"]?.ifBlank { base.name } ?: base.name,
                guardian = stdMap["guardian"]?.ifBlank { base.guardian } ?: base.guardian,
                village = stdMap["village"]?.ifBlank { base.village } ?: base.village,
                post = stdMap["post"]?.ifBlank { base.post } ?: base.post,
                upazila = stdMap["upazila"]?.ifBlank { base.upazila } ?: base.upazila,
                district = stdMap["district"]?.ifBlank { base.district } ?: base.district,
                mobile = stdMap["mobile"]?.ifBlank { base.mobile } ?: base.mobile,
                religion = stdMap["religion"]?.ifBlank { base.religion } ?: base.religion,
                m = base.m + m,
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
        val rows = parseCsv(txt)
        if (rows.size < 2) return existing
        val cols = detectCsvColumns(rows, emptyList())
        return applyMappedCsvImport(rows, cols, existing, DEFAULT_SETTINGS, 1).students
    }

    // ---------- AI দিয়ে CSV তৈরির প্রম্পট জেনারেটর ----------
    enum class CsvSourceKind(val labelBn: String, val promptDescBn: String) {
        IMAGE("ছবি / হাতের লেখা খাতা", "আমার দেওয়া ছবি বা স্ক্যান করা খাতা/রেজিস্টার"),
        PDF("PDF / ডকুমেন্ট", "আমার দেওয়া PDF বা ডকুমেন্ট ফাইল"),
        EXCEL("Excel / Google Sheet", "আমার দেওয়া Excel, স্প্রেডশিট বা এলোমেলো টেবিল"),
        TEXT("মেসেজ / টেক্সট তালিকা", "আমার দেওয়া নিচের টেক্সট তালিকা বা মেসেজ")
    }

    data class CsvPromptOptions(
        val targetCls: Int? = null, // null = একাধিক বা সব শ্রেণি (১-৫)
        val includeGuardianMobile: Boolean = true,
        val includeAddress: Boolean = false,
        val includeExistingCustomFields: Boolean = true,
        val extraCustomColumnsRaw: String = "",
        val includeMarks: Boolean = false,
        val selectedTerms: Set<Int> = setOf(1),
        val selectedSubjects: Set<String> = emptySet(), // empty => classes' default subjects
        val includeAttendanceTraits: Boolean = false,
        val sourceKind: CsvSourceKind = CsvSourceKind.IMAGE,
        val extraInstruction: String = ""
    )

    data class GeneratedCsvPrompt(
        val prompt: String,
        val columns: List<String>
    )

    fun buildAiCsvPrompt(
        options: CsvPromptOptions,
        existingCustomFields: List<CustomFieldDef>
    ): GeneratedCsvPrompt {
        val cols = mutableListOf("cls", "roll", "name")
        if (options.includeGuardianMobile) {
            cols += listOf("guardian", "mobile", "religion")
        }
        if (options.includeAddress) {
            cols += listOf("village", "post", "upazila", "district")
        }
        if (options.includeExistingCustomFields && existingCustomFields.isNotEmpty()) {
            cols += existingCustomFields.map { it.label }
        }
        val extraCols = options.extraCustomColumnsRaw
            .split(',', '،', '\n')
            .map { it.trim() }
            .filter { it.isNotBlank() && it !in cols }
        cols += extraCols

        val activeTerms = options.selectedTerms.filter { it in 1..3 }.sorted().ifEmpty { listOf(1) }
        val refCls = options.targetCls ?: 3
        val classSubs = subjectsFor(refCls, RELIGIONS.first())
        val allPossibleSubs = if (options.targetCls == null) {
            (subjectsFor(1, RELIGIONS.first()) + subjectsFor(3, RELIGIONS.first())).distinctBy { it.key }
        } else {
            classSubs
        }
        val chosenSubs = if (options.selectedSubjects.isNotEmpty()) {
            allPossibleSubs.filter { it.key in options.selectedSubjects }.ifEmpty { allPossibleSubs }
        } else {
            classSubs
        }

        val markRulesDesc = mutableListOf<String>()
        if (options.includeMarks) {
            for (t in activeTerms) {
                for (sub in chosenSubs) {
                    sub.comps.forEach { c -> cols += mk(sub.key, t, c.key) }
                    if (sub.written > 0) cols += mk(sub.key, t, "WR")
                }
            }
            chosenSubs.forEach { sub ->
                val parts = sub.comps.joinToString(", ") { "${it.key} (সর্বোচ্চ ${it.max})" } +
                    if (sub.written > 0) ", WR (লিখিত সর্বোচ্চ ${sub.written})" else ""
                markRulesDesc += "• ${sub.key} (${sub.bn}): $parts"
            }
        }

        if (options.includeAttendanceTraits) {
            for (t in activeTerms) {
                cols += listOf("DAYS_$t", "PRES_$t", "ACT_$t", "DISC_$t", "COND_$t", "REM_$t")
            }
        }

        val distinctCols = cols.distinct()
        val headerLine = distinctCols.joinToString(",")
        val sampleCls = options.targetCls ?: 3
        val sampleRow = distinctCols.joinToString(",") { col ->
            when (col) {
                "cls" -> sampleCls.toString()
                "roll" -> "1"
                "name" -> "আরিয়ান ইসলাম"
                "guardian" -> "মোঃ রফিকুল ইসলাম"
                "mobile" -> "01712345678"
                "religion" -> "ইসলাম"
                "village" -> "কামারখালী"
                "post" -> "কামারখালী"
                "upazila" -> "মধুখালী"
                "district" -> "ফরিদপুর"
                else -> when {
                    col.startsWith("DAYS_") -> "64"
                    col.startsWith("PRES_") -> "60"
                    col.startsWith("ACT_") || col.startsWith("DISC_") || col.startsWith("COND_") -> "ক"
                    col.startsWith("REM_") -> "অগ্রগতি সন্তোষজনক"
                    col.endsWith("_TB") -> "9"
                    col.endsWith("_ACT") || col.endsWith("_LANG") -> "5"
                    col.endsWith("_CT") -> "8"
                    col.endsWith("_WR") -> if (sampleCls <= 2) "45" else "62"
                    col.endsWith("_TOTAL") -> "45"
                    else -> ""
                }
            }
        }

        val clsRule = if (options.targetCls != null) {
            "সব শিক্ষার্থীর `cls` কলামে `${options.targetCls}` (${CLSN[options.targetCls - 1]} শ্রেণি) বসাবে।"
        } else {
            "`cls` কলামে ১ থেকে ৫ এর মধ্যে শুধু ইংরেজি সংখ্যা (1, 2, 3, 4 বা 5) বসাবে।"
        }

        val prompt = buildString {
            appendLine("${options.sourceKind.promptDescBn} থেকে বাংলাদেশের সরকারি প্রাথমিক বিদ্যালয়ের শিক্ষার্থীর তথ্য নিখুঁতভাবে বের করে নিচের হুবহু CSV ফরম্যাটে রূপান্তর করে দাও।")
            appendLine()
            appendLine("## ১. আবশ্যিক CSV হেডার (ঠিক এই ক্রমে ও বানানে প্রথম সারিতে থাকবে):")
            appendLine(headerLine)
            appendLine()
            appendLine("## ২. কলাম ও ডেটা ফরম্যাট নিয়ম:")
            appendLine("- $clsRule")
            appendLine("- `roll` ও সব নম্বর অবশ্যই ইংরেজি অঙ্কে (0-9) লিখবে; শিক্ষার্থীর নাম (`name`) ও বাংলা তথ্য স্পষ্ট বাংলায় লিখবে।")
            if (options.includeGuardianMobile) {
                appendLine("- `mobile` কলামে ১১ ডিজিটের নম্বর (যেমন 01712345678) লিখবে; `religion` কলামে শুধু `ইসলাম`, `হিন্দু`, `বৌদ্ধ` বা `খ্রীষ্ট` লিখবে।")
            }
            if (options.includeMarks && markRulesDesc.isNotEmpty()) {
                val termLabel = activeTerms.joinToString(", ") { "${bn(it.toString())}ম প্রান্তিক (_${it}_)" }
                appendLine("- নম্বর কলাম নিয়ম (<বিষয়>_<প্রান্তিক>_<অংশ>), প্রান্তিক: $termLabel:")
                markRulesDesc.forEach { appendLine("  $it") }
                appendLine("  (খাতায় ক্লাস টেস্ট বা CT ঘরে `06+07=13/2=07` ধরনের গড় থাকলে `=` চিহ্নের পরের চূড়ান্ত মান `7` বসাবে; অনুপস্থিত `A`/`অনু` থাকলে `0` বসাবে।)")
            }
            if (options.includeAttendanceTraits) {
                appendLine("- উপস্থিতি ও আচরণ কলাম (_<প্রান্তিক>): `DAYS_t` (মোট কর্মদিবস), `PRES_t` (উপস্থিতি), `ACT_t` (পাঠ্য ও সহপাঠ্যক্রমিক কাজ: ক/খ/গ/ঘ), `DISC_t` (শৃঙ্খলা: ক/খ/গ/ঘ), `COND_t` (আচরণ: ক/খ/গ/ঘ), `REM_t` (মন্তব্য)।")
            }
            if (extraCols.isNotEmpty() || (options.includeExistingCustomFields && existingCustomFields.isNotEmpty())) {
                val cfList = (if (options.includeExistingCustomFields) existingCustomFields.map { it.label } else emptyList()) + extraCols
                if (cfList.isNotEmpty()) {
                    appendLine("- অতিরিক্ত কলামসমূহ (${cfList.joinToString(", ")}): পাওয়া গেলে মান বসাবে, না থাকলে ফাঁকা রাখবে।")
                }
            }
            appendLine("- কোনো তথ্য না থাকলে বা ঘর ফাঁকা থাকলে কিছু লিখবে না (শুধু কমা `,` দিয়ে ফাঁকা রাখবে; `N/A` বা `-` লিখবে না)।")
            appendLine("- কোনো মানের ভেতরে কমা (`,`) থাকলে মানটি ডাবল কোটেশন `\"...\"`-এর ভেতরে লিখবে।")
            if (options.extraInstruction.isNotBlank()) {
                appendLine("- অতিরিক্ত নির্দেশনা: ${options.extraInstruction.trim()}")
            }
            appendLine()
            appendLine("## ৩. নমুনা আউটপুট ফরম্যাট (শুধু উদাহরণ):")
            appendLine("```csv")
            appendLine(headerLine)
            appendLine(sampleRow)
            appendLine("```")
            appendLine()
            append("এখন কোনো বাড়তি ব্যাখ্যা বা ভূমিকা ছাড়া সম্পূর্ণ UTF-8 CSV ডেটা শুধু একটি ```csv কোড ব্লকের ভেতরে দাও যাতে আমি সরাসরি কপি করে Result Maker অ্যাপে ইমপোর্ট করতে পারি।")
        }

        return GeneratedCsvPrompt(prompt = prompt, columns = distinctCols)
    }
}
