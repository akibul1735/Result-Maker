package com.kgps.result

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.kgps.result.ai.ClassSheetImportScope
import com.kgps.result.ai.ClassSheetKind

/**
 * ফাঁকা শীটে এম্বেড করা QR কোডের মেটাডেটা ও কাঠামো।
 * এটি পিওর-কটলিন (android.* বা org.json.* ছাড়া) যাতে অ্যান্ড্রয়েড এবং JVM ইউনিট টেস্ট উভয় স্থানেই নির্বিঘ্নে চলে।
 */
data class BlankSheetQrMeta(
    val version: Int = 1,
    val cls: Int,
    val subjectKey: String?,
    val term: Int,
    val sheetKind: ClassSheetKind,
    val importScope: ClassSheetImportScope = ClassSheetImportScope.AUTO,
    val columnIds: List<String>,
    val customColumnTitles: List<String> = emptyList(),
    val numeralStyle: BlankNumeralStyle = BlankNumeralStyle.BENGALI,
    val showSubjectMaxInHeader: Boolean = true,
    val workingDaysByTerm: Map<Int, String> = emptyMap(),
    val pageIndex: Int = 1,
    val totalPages: Int = 1,
    val refCode: String = ""
)

object SheetQrCodec {

    private val customIdPrefixRegex = Regex("^CUS:(\\d+)$")

    fun inferSheetKindAndScope(
        columns: List<BlankSheetColumn>
    ): Pair<ClassSheetKind, ClassSheetImportScope> {
        val is3TMarks = BlankSheetCatalog.isThreeTermMarksActive(columns)
        val is3TTraits = BlankSheetCatalog.isThreeTermAttendanceTraitsActive(columns)
        val hasMarks = columns.any { it.compKey != null }
        val hasAttSingle = columns.any { it.id == "ATT" } && !is3TTraits && !hasMarks

        if (is3TTraits && !is3TMarks && !hasMarks) {
            return ClassSheetKind.ATTENDANCE_TRAITS to ClassSheetImportScope.BOTH
        }
        if (hasAttSingle) {
            return ClassSheetKind.ATTENDANCE_TRAITS to ClassSheetImportScope.BOTH
        }
        if (is3TMarks) {
            return ClassSheetKind.THREE_TERM_MARKS to ClassSheetImportScope.BOTH
        }

        val hasCont = columns.any { it.compKey in setOf("TB", "ACT", "LANG", "CT", "CONT") }
        val hasWr = columns.any { it.compKey == "WR" }
        val hasTotal = columns.any { it.compKey == "TOTAL" }

        val scope = when {
            hasCont && !hasWr && !hasTotal -> ClassSheetImportScope.CONT_ONLY
            hasWr && !hasCont && !hasTotal -> ClassSheetImportScope.WR_ONLY
            else -> ClassSheetImportScope.BOTH
        }
        return ClassSheetKind.SINGLE_TERM_MARKS to scope
    }

    fun fromPageConfig(
        page: BlankSheetPageSpec,
        term: Int
    ): BlankSheetQrMeta {
        val (kind, scope) = inferSheetKindAndScope(page.columns)
        val customTitles = mutableListOf<String>()
        val encodedColIds = page.columns.map { col ->
            if (col.isCustom || col.id.startsWith("CUSTOM_")) {
                val idx = customTitles.size
                customTitles.add(col.titleBn.trim())
                "CUS:$idx"
            } else {
                col.id
            }
        }
        val cleanSub = page.subjectKey?.takeIf {
            it.isNotBlank() && it != BlankSheetCatalog.ALL_SUBJECTS_KEY
        }
        val cleanWd = page.workingDaysByTerm
            .mapValues { (_, v) -> fromBn(v).filter { it.isDigit() } }
            .filterValues { it.isNotEmpty() }

        return BlankSheetQrMeta(
            version = 1,
            cls = page.cls,
            subjectKey = cleanSub,
            term = term,
            sheetKind = kind,
            importScope = scope,
            columnIds = encodedColIds,
            customColumnTitles = customTitles,
            numeralStyle = page.numeralStyle,
            showSubjectMaxInHeader = page.showSubjectMaxInHeader,
            workingDaysByTerm = cleanWd,
            pageIndex = page.classPageIndex,
            totalPages = page.classTotalPages,
            refCode = page.sheetRefCode
        )
    }

    fun encodePayload(meta: BlankSheetQrMeta): String {
        val kindCode = when (meta.sheetKind) {
            ClassSheetKind.SINGLE_TERM_MARKS -> "1M"
            ClassSheetKind.THREE_TERM_MARKS -> "3M"
            ClassSheetKind.ATTENDANCE_TRAITS -> "AT"
        }
        val scopeCode = when (meta.importScope) {
            ClassSheetImportScope.AUTO -> "A"
            ClassSheetImportScope.BOTH -> "B"
            ClassSheetImportScope.CONT_ONLY -> "C"
            ClassSheetImportScope.WR_ONLY -> "W"
        }
        val sb = StringBuilder(160)
        sb.append("{\"v\":").append(meta.version)
        sb.append(",\"c\":").append(meta.cls)
        if (!meta.subjectKey.isNullOrBlank() && meta.subjectKey != BlankSheetCatalog.ALL_SUBJECTS_KEY) {
            sb.append(",\"s\":").append(jsonQuote(meta.subjectKey))
        }
        sb.append(",\"t\":").append(meta.term)
        sb.append(",\"k\":").append(jsonQuote(kindCode))
        sb.append(",\"sc\":").append(jsonQuote(scopeCode))
        if (meta.numeralStyle != BlankNumeralStyle.BENGALI) {
            sb.append(",\"n\":\"E\"")
        }
        if (!meta.showSubjectMaxInHeader) {
            sb.append(",\"mx\":0")
        }
        if (meta.pageIndex != 1 || meta.totalPages != 1) {
            sb.append(",\"p\":").append(jsonQuote("${meta.pageIndex}/${meta.totalPages}"))
        }
        sb.append(",\"cols\":[")
        meta.columnIds.forEachIndexed { idx, id ->
            if (idx > 0) sb.append(',')
            sb.append(jsonQuote(id))
        }
        sb.append(']')
        if (meta.customColumnTitles.isNotEmpty()) {
            sb.append(",\"cu\":[")
            meta.customColumnTitles.forEachIndexed { idx, title ->
                if (idx > 0) sb.append(',')
                sb.append(jsonQuote(title.take(28)))
            }
            sb.append(']')
        }
        if (meta.workingDaysByTerm.isNotEmpty()) {
            sb.append(",\"wd\":{")
            meta.workingDaysByTerm.entries.forEachIndexed { idx, (t, days) ->
                if (idx > 0) sb.append(',')
                sb.append(jsonQuote(t.toString())).append(':').append(jsonQuote(days))
            }
            sb.append('}')
        }
        sb.append('}')
        return sb.toString()
    }

    fun decodePayload(rawText: String): BlankSheetQrMeta? {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return null

        // 1. Structured JSON payload
        if (trimmed.startsWith("{")) {
            val root = runCatching { MiniJsonParser(trimmed).parseObject() }.getOrNull()
            if (root != null) {
                val cls = (root["c"] as? Number)?.toInt()?.takeIf { it in 1..20 } ?: return null
                val rawSub = (root["s"] as? String)?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
                val term = (root["t"] as? Number)?.toInt() ?: 1
                val kind = when ((root["k"] as? String)?.trim()?.uppercase()) {
                    "3M", "THREE_TERM_MARKS" -> ClassSheetKind.THREE_TERM_MARKS
                    "AT", "ATTENDANCE_TRAITS" -> ClassSheetKind.ATTENDANCE_TRAITS
                    else -> ClassSheetKind.SINGLE_TERM_MARKS
                }
                val scope = when ((root["sc"] as? String)?.trim()?.uppercase()) {
                    "B", "BOTH" -> ClassSheetImportScope.BOTH
                    "C", "CONT_ONLY" -> ClassSheetImportScope.CONT_ONLY
                    "W", "WR_ONLY" -> ClassSheetImportScope.WR_ONLY
                    else -> ClassSheetImportScope.AUTO
                }
                val numStyle = when ((root["n"] as? String)?.trim()?.uppercase()) {
                    "E", "ENGLISH" -> BlankNumeralStyle.ENGLISH
                    else -> BlankNumeralStyle.BENGALI
                }
                val showMax = when (val mx = root["mx"]) {
                    is Boolean -> mx
                    is Number -> mx.toInt() != 0
                    else -> true
                }
                val pageParts = (root["p"] as? String)?.split('/')
                val pageIdx = pageParts?.getOrNull(0)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                val totalPages = pageParts?.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(pageIdx) ?: 1

                val rawCols = (root["cols"] as? List<*>)
                    ?.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
                    .orEmpty()
                val customTitles = (root["cu"] as? List<*>)
                    ?.mapNotNull { it?.toString()?.trim() }
                    .orEmpty()

                val wdMap = linkedMapOf<Int, String>()
                val wdRaw = root["wd"] as? Map<*, *>
                wdRaw?.forEach { (k, v) ->
                    val t = k?.toString()?.trim()?.toIntOrNull()
                    val d = v?.toString()?.trim()?.filter { it.isDigit() }
                    if (t in 1..3 && !d.isNullOrEmpty()) {
                        wdMap[t!!] = d
                    }
                }

                val colsForRef = rawCols.mapNotNull { colToken ->
                    val customMatch = customIdPrefixRegex.matchEntire(colToken)
                    if (customMatch != null) {
                        val cIdx = customMatch.groupValues[1].toIntOrNull() ?: 0
                        val cTitle = customTitles.getOrNull(cIdx).orEmpty().ifBlank { "কাস্টম" }
                        BlankSheetColumn(id = colToken, titleBn = cTitle, isCustom = true)
                    } else {
                        BlankSheetCatalog.byId(colToken)
                    }
                }.ifEmpty { BlankSheetCatalog.DEFAULT_COLUMNS }

                val ref = (root["ref"] as? String)?.takeIf { it.isNotBlank() }
                    ?: BlankSheetCatalog.buildSheetRefCode(cls, rawSub, term, colsForRef)

                return BlankSheetQrMeta(
                    version = (root["v"] as? Number)?.toInt() ?: 1,
                    cls = cls,
                    subjectKey = rawSub,
                    term = term,
                    sheetKind = kind,
                    importScope = scope,
                    columnIds = rawCols.ifEmpty { colsForRef.map { it.id } },
                    customColumnTitles = customTitles,
                    numeralStyle = numStyle,
                    showSubjectMaxInHeader = showMax,
                    workingDaysByTerm = wdMap,
                    pageIndex = pageIdx,
                    totalPages = totalPages,
                    refCode = ref
                )
            }
        }

        // 2. Fallback: REF:C3-BAN-T1-FULL string
        val refMatch = Regex("REF\\s*:\\s*C(\\d{1,2})-([A-Z]+)-([A-Z0-9]+)(?:-([A-Z0-9]+))?", RegexOption.IGNORE_CASE)
            .find(trimmed) ?: return null
        val cls = refMatch.groupValues[1].toIntOrNull()?.takeIf { it in 1..20 } ?: return null
        val subRaw = refMatch.groupValues[2].uppercase()
        val sub = subRaw.takeIf { it != "ATT" && it != "GEN" }
        val termPart = refMatch.groupValues[3].uppercase()
        val term = when (termPart) {
            "T1" -> 1
            "T2" -> 2
            "T3" -> 3
            "ANN" -> 0
            else -> 1
        }
        val layout = refMatch.groupValues.getOrNull(4)?.uppercase().orEmpty()
        val (kind, presetCols) = when (layout) {
            "TRAITS", "ATT" -> ClassSheetKind.ATTENDANCE_TRAITS to BlankSheetCatalog.presetColumns(BlankSheetPreset.THREE_TERM_ATTENDANCE_TRAITS)
            "3MARKS" -> ClassSheetKind.THREE_TERM_MARKS to BlankSheetCatalog.presetColumns(BlankSheetPreset.THREE_TERM_MARKS)
            "FULL" -> ClassSheetKind.SINGLE_TERM_MARKS to BlankSheetCatalog.presetColumns(BlankSheetPreset.FULL_MARKS)
            "TOTAL" -> ClassSheetKind.SINGLE_TERM_MARKS to BlankSheetCatalog.presetColumns(BlankSheetPreset.ROLL_NAME_TOTAL)
            else -> ClassSheetKind.SINGLE_TERM_MARKS to BlankSheetCatalog.presetColumns(BlankSheetPreset.COMPACT_MARKS)
        }
        return BlankSheetQrMeta(
            version = 1,
            cls = cls,
            subjectKey = sub,
            term = term,
            sheetKind = kind,
            importScope = ClassSheetImportScope.BOTH,
            columnIds = presetCols.map { it.id },
            refCode = refMatch.value.uppercase()
        )
    }

    fun encodeQrMatrix(
        payload: String,
        ecLevel: ErrorCorrectionLevel = ErrorCorrectionLevel.M
    ): BitMatrix? {
        if (payload.isBlank()) return null
        return runCatching {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ecLevel,
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 0
            )
            QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 0, 0, hints)
        }.getOrNull()
    }

    fun buildSummaryLabelBn(
        meta: BlankSheetQrMeta,
        settings: Settings? = null
    ): String {
        val clsLabel = if (settings != null) {
            "${classNameBn(meta.cls, settings)} শ্রেণি"
        } else {
            "${classNameBn(meta.cls)} শ্রেণি"
        }
        val parts = mutableListOf(clsLabel)
        if (meta.sheetKind != ClassSheetKind.ATTENDANCE_TRAITS && !meta.subjectKey.isNullOrBlank()) {
            val subLabel = BlankSheetCatalog.subjectTitleFor(meta.subjectKey, "").ifBlank { meta.subjectKey }
            parts.add(subLabel)
        }
        when (meta.sheetKind) {
            ClassSheetKind.THREE_TERM_MARKS -> parts.add("৩ প্রান্তিক নম্বর")
            ClassSheetKind.ATTENDANCE_TRAITS -> parts.add("উপস্থিতি ও গুণাবলি")
            ClassSheetKind.SINGLE_TERM_MARKS -> {
                if (meta.term in 1..3) parts.add(termNameBn(meta.term))
                when (meta.importScope) {
                    ClassSheetImportScope.CONT_ONLY -> parts.add("শুধু ধারাবাহিক")
                    ClassSheetImportScope.WR_ONLY -> parts.add("শুধু সামষ্টিক")
                    else -> Unit
                }
            }
        }
        if (meta.columnIds.isNotEmpty()) {
            parts.add("${bn(meta.columnIds.size.toString())} কলাম")
        }
        return parts.joinToString(" · ")
    }

    fun columnHeaderDescription(meta: BlankSheetQrMeta): List<String> {
        return meta.columnIds.mapIndexed { idx, colToken ->
            val customMatch = customIdPrefixRegex.matchEntire(colToken)
            val label = if (customMatch != null) {
                val cIdx = customMatch.groupValues[1].toIntOrNull() ?: 0
                val customTitle = meta.customColumnTitles.getOrNull(cIdx).orEmpty().ifBlank { "কাস্টম" }
                "$customTitle (কাস্টম কলাম -> প্রয়োজনে rawNotes-এ রাখুন)"
            } else {
                val col = BlankSheetCatalog.byId(colToken)
                val maxHint = if (meta.showSubjectMaxInHeader && col?.compKey != null) {
                    BlankSheetCatalog.resolveMaxHint(meta.cls, meta.subjectKey, col.compKey)
                } else {
                    null
                }
                val jsonTarget = when (colToken) {
                    "ROLL" -> "roll"
                    "NAME" -> "name"
                    "TB", "ACT", "LANG", "CT", "WR",
                    "T1_CONT", "T1_WR", "T1_TOTAL",
                    "T2_CONT", "T2_WR", "T2_TOTAL",
                    "T3_CONT", "T3_WR", "T3_TOTAL" -> "marks.$colToken"
                    "CONT" -> "sheetContinuousTotal"
                    "TOTAL" -> "sheetGrandTotal"
                    "REMARK" -> "remark"
                    "ATT" -> "traits.PRES_${meta.term.coerceIn(1, 3)}"
                    "T1_DAYS" -> "traits.DAYS_1"
                    "T2_DAYS" -> "traits.DAYS_2"
                    "T3_DAYS" -> "traits.DAYS_3"
                    "T1_PRES" -> "traits.PRES_1"
                    "T2_PRES" -> "traits.PRES_2"
                    "T3_PRES" -> "traits.PRES_3"
                    "T1_ACT" -> "traits.ACT_1"
                    "T2_ACT" -> "traits.ACT_2"
                    "T3_ACT" -> "traits.ACT_3"
                    "T1_DISC" -> "traits.DISC_1"
                    "T2_DISC" -> "traits.DISC_2"
                    "T3_DISC" -> "traits.DISC_3"
                    "T1_COND" -> "traits.COND_1"
                    "T2_COND" -> "traits.COND_2"
                    "T3_COND" -> "traits.COND_3"
                    "GRADE", "GP", "MERIT", "SIGN" -> "উপেক্ষা করুন"
                    else -> colToken
                }
                val title = col?.pickerLabelBn ?: colToken
                val maxTxt = maxHint?.let { " [সর্বোচ্চ $it]" }.orEmpty()
                "$title$maxTxt -> $jsonTarget"
            }
            "${idx + 1}) $label"
        }
    }

    fun buildAiPromptInstructions(meta: BlankSheetQrMeta): String {
        val subName = meta.subjectKey?.let { BlankSheetCatalog.subjectTitleFor(it, "") }.orEmpty()
        val subStr = if (!meta.subjectKey.isNullOrBlank()) {
            "$subName (${meta.subjectKey})"
        } else {
            "উপস্থিতি/সাধারণ"
        }
        val termLabel = when {
            meta.sheetKind == ClassSheetKind.THREE_TERM_MARKS ||
                meta.sheetKind == ClassSheetKind.ATTENDANCE_TRAITS -> "৩ প্রান্তিক"
            meta.term in 1..3 -> termNameBn(meta.term)
            else -> "বার্ষিক/সাধারণ"
        }
        val colListStr = columnHeaderDescription(meta).joinToString(" | ")
        val hasBreakdown = meta.columnIds.any { it in setOf("TB", "ACT", "LANG", "CT") }
        val hasCont = meta.columnIds.any { it == "CONT" }
        val hasWr = meta.columnIds.any { it == "WR" }
        val hasTotal = meta.columnIds.any { it == "TOTAL" }

        return buildString {
            appendLine("[QR-নিশ্চিত শিট কাঠামো — ছবির QR কোড থেকে অফলাইনে প্রাপ্ত]:")
            appendLine("- নিশ্চিত তথ্য: cls=${meta.cls}, subKey=${meta.subjectKey ?: ""}, term=${meta.term.coerceIn(1, 3)}, sheetKind=${meta.sheetKind.name}, বিষয়=$subStr, প্রান্তিক=$termLabel, পৃষ্ঠা=${meta.pageIndex}/${meta.totalPages}।")
            appendLine("- টেবিলের বাম থেকে ডান কলাম ক্রম: $colListStr।")
            appendLine("- মুদ্রিত সংখ্যা ধরন: ${meta.numeralStyle.labelBn}; তবে আউটপুট JSON-এ সব সংখ্যা ইংরেজি অঙ্কে (0-9) দিন।")
            if (meta.workingDaysByTerm.isNotEmpty()) {
                val wdStr = meta.workingDaysByTerm.entries.joinToString(", ") { (t, d) -> "${termNameBn(t)}=$d দিন" }
                appendLine("- QR-এ প্রাপ্ত মোট কার্যদিবস: $wdStr (sharedWorkingDays-এ বসান)।")
            }
            if (meta.sheetKind == ClassSheetKind.SINGLE_TERM_MARKS) {
                if (!hasBreakdown && !hasCont && !hasWr && hasTotal) {
                    appendLine("- এটি 'সহজ মোট' ফর্ম: কেবল `sheetGrandTotal` (ও থাকলে `remark`) দিন; `TB/ACT/LANG/CT/WR/sheetContinuousTotal` অনুমান করবেন না।")
                } else if (!hasBreakdown && hasCont && hasWr) {
                    appendLine("- এটি সংক্ষিপ্ত ফর্ম: আলাদা `TB/ACT/LANG/CT` নেই — কেবল `sheetContinuousTotal`, `WR`, ও `sheetGrandTotal` দিন।")
                } else if (hasCont && !hasWr && !hasTotal) {
                    appendLine("- এটি শুধু ধারাবাহিক নম্বর ফর্ম: `WR` বা `sheetGrandTotal` অনুমান করবেন না।")
                }
            }
            if (meta.customColumnTitles.isNotEmpty()) {
                append("- কাস্টম কলাম (${meta.customColumnTitles.joinToString(", ")}) ডাটা থাকলেও `marks` নষ্ট করবেন না, প্রয়োজনে `rawNotes`-এ উল্লেখ করুন।")
            }
        }.trim()
    }

    private fun jsonQuote(value: String): String {
        val sb = StringBuilder(value.length + 2)
        sb.append('"')
        for (ch in value) {
            when (ch) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(ch)
            }
        }
        sb.append('"')
        return sb.toString()
    }

    /**
     * ন্যূনতম pure-Kotlin JSON পার্সার (কোনো android/org.json নির্ভরতা নেই)।
     */
    private class MiniJsonParser(private val src: String) {
        private var pos = 0

        fun parseObject(): Map<String, Any?> {
            skipWhitespace()
            if (peek() != '{') throw IllegalArgumentException("Expected '{'")
            pos++
            val map = linkedMapOf<String, Any?>()
            skipWhitespace()
            if (peek() == '}') {
                pos++
                return map
            }
            while (pos < src.length) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                if (peek() != ':') throw IllegalArgumentException("Expected ':'")
                pos++
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    '}' -> {
                        pos++
                        return map
                    }
                    else -> throw IllegalArgumentException("Expected ',' or '}'")
                }
            }
            return map
        }

        private fun parseArray(): List<Any?> {
            pos++ // '['
            val list = mutableListOf<Any?>()
            skipWhitespace()
            if (peek() == ']') {
                pos++
                return list
            }
            while (pos < src.length) {
                list.add(parseValue())
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    ']' -> {
                        pos++
                        return list
                    }
                    else -> throw IllegalArgumentException("Expected ',' or ']'")
                }
            }
            return list
        }

        private fun parseValue(): Any? {
            skipWhitespace()
            return when (val ch = peek()) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't' -> parseLiteral("true", true)
                'f' -> parseLiteral("false", false)
                'n' -> parseLiteral("null", null)
                '-', in '0'..'9' -> parseNumber()
                else -> throw IllegalArgumentException("Unexpected char '$ch' at $pos")
            }
        }

        private fun parseString(): String {
            if (peek() != '"') throw IllegalArgumentException("Expected '\"'")
            pos++
            val sb = StringBuilder()
            while (pos < src.length) {
                val ch = src[pos++]
                when (ch) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (pos >= src.length) break
                        when (val esc = src[pos++]) {
                            '"', '\\', '/' -> sb.append(esc)
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 <= src.length) {
                                    val hex = src.substring(pos, pos + 4)
                                    hex.toIntOrNull(16)?.let { sb.append(it.toChar()) }
                                    pos += 4
                                }
                            }
                            else -> sb.append(esc)
                        }
                    }
                    else -> sb.append(ch)
                }
            }
            return sb.toString()
        }

        private fun parseLiteral(expected: String, result: Any?): Any? {
            if (!src.regionMatches(pos, expected, 0, expected.length)) {
                throw IllegalArgumentException("Invalid literal at $pos")
            }
            pos += expected.length
            return result
        }

        private fun parseNumber(): Number {
            val start = pos
            if (src[pos] == '-') pos++
            while (pos < src.length && (src[pos].isDigit() || src[pos] == '.' || src[pos] == 'e' || src[pos] == 'E' || src[pos] == '+' || src[pos] == '-')) {
                if ((src[pos] == '+' || src[pos] == '-') && pos > start && src[pos - 1] != 'e' && src[pos - 1] != 'E') {
                    break
                }
                pos++
            }
            val token = src.substring(start, pos)
            return if (token.contains('.') || token.contains('e', ignoreCase = true)) {
                token.toDouble()
            } else {
                token.toLong()
            }
        }

        private fun skipWhitespace() {
            while (pos < src.length && src[pos].isWhitespace()) pos++
        }

        private fun peek(): Char = if (pos < src.length) src[pos] else '\u0000'
    }
}
