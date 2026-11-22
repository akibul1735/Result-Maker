package com.kgps.result

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.oned.Code128Writer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.kgps.result.ai.ClassSheetImportScope
import com.kgps.result.ai.ClassSheetKind
import java.security.MessageDigest

/**
 * ফাঁকা শীটে এম্বেড করা QR কোড ও বারকোডের মেটাডেটা এবং কাঠামো।
 * এটি পিওর-কটলিন (android.* বা org.json.* ছাড়া) যাতে অ্যান্ড্রয়েড এবং JVM ইউনিট টেস্ট উভয় স্থানেই নির্বিঘ্নে চলে।
 */
data class BlankSheetQrMeta(
    val version: Int = 2,
    val templateId: String = "",
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

data class DecodedSheetChannel(
    val templateId: String? = null,
    val meta: BlankSheetQrMeta? = null
)

object SheetQrCodec {

    private const val BASE32_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val customIdPrefixRegex = Regex("^CUS:(\\d+)$")
    private val thinQrRegex = Regex("^KT1:([A-Z0-9]{6,24})$", RegexOption.IGNORE_CASE)
    private val barcodeRegex = Regex(
        "^(?:REF\\s*:\\s*)?C(\\d{1,2})-([A-Z]+)-([A-Z0-9]+)(?:-([A-Z0-9]+))?(?:-([A-Z0-9]{6,24}))?$",
        RegexOption.IGNORE_CASE
    )
    private val refInlineRegex = Regex(
        "REF\\s*:\\s*C(\\d{1,2})-([A-Z]+)-([A-Z0-9]+)(?:-([A-Z0-9]+))?",
        RegexOption.IGNORE_CASE
    )

    // ১–২ অক্ষরের QR Alphanumeric কলাম কোড (JSON-এর তুলনায় ~৭০% ছোট এবং পাতলা QR তৈরি করে)
    private val colToShort = mapOf(
        "ROLL" to "R",
        "NAME" to "N",
        "TB" to "B",
        "ACT" to "A",
        "LANG" to "L",
        "CT" to "C",
        "CONT" to "K",
        "WR" to "W",
        "TOTAL" to "T",
        "T1_CONT" to "1K",
        "T1_WR" to "1W",
        "T1_TOTAL" to "1T",
        "T2_CONT" to "2K",
        "T2_WR" to "2W",
        "T2_TOTAL" to "2T",
        "T3_CONT" to "3K",
        "T3_WR" to "3W",
        "T3_TOTAL" to "3T",
        "T1_DAYS" to "1D",
        "T1_PRES" to "1P",
        "T1_ACT" to "1A",
        "T1_DISC" to "1S",
        "T1_COND" to "1O",
        "T2_DAYS" to "2D",
        "T2_PRES" to "2P",
        "T2_ACT" to "2A",
        "T2_DISC" to "2S",
        "T2_COND" to "2O",
        "T3_DAYS" to "3D",
        "T3_PRES" to "3P",
        "T3_ACT" to "3A",
        "T3_DISC" to "3S",
        "T3_COND" to "3O",
        "GRADE" to "G",
        "GP" to "P",
        "MERIT" to "M",
        "ATT" to "U",
        "REMARK" to "E",
        "SIGN" to "Z"
    )
    private val shortToCol = colToShort.entries.associate { (k, v) -> v to k }

    private val presetShortcuts: List<Pair<String, List<String>>> = listOf(
        "\$F" to BlankSheetPreset.FULL_MARKS.columnIds,
        "\$C" to BlankSheetPreset.COMPACT_MARKS.columnIds,
        "\$T" to BlankSheetPreset.ROLL_NAME_TOTAL.columnIds,
        "\$A" to BlankSheetPreset.ATTENDANCE_SIGN.columnIds,
        "\$3" to BlankSheetPreset.THREE_TERM_MARKS.columnIds,
        "\$4" to BlankSheetCatalog.threeTermMarksColumns(includeTotal = true).map { it.id },
        "\$R" to BlankSheetPreset.THREE_TERM_ATTENDANCE_TRAITS.columnIds
    )
    private val presetCodeToCols = presetShortcuts.toMap()

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

    /**
     * একই শীট-কাঠামোর জন্য সব সময় একই Deterministic ১০-অক্ষরের [A-Z0-9] Template ID তৈরি করে
     * (যাতে অপ্রয়োজনীয় ডুপ্লিকেট Firestore ডকুমেন্ট তৈরি না হয়)।
     */
    fun computeTemplateId(meta: BlankSheetQrMeta): String {
        val canonical = buildString {
            append(meta.cls).append('|')
            append(meta.subjectKey.orEmpty().uppercase()).append('|')
            append(meta.term).append('|')
            append(meta.sheetKind.name).append('|')
            append(meta.importScope.name).append('|')
            append(meta.numeralStyle.name).append('|')
            append(if (meta.showSubjectMaxInHeader) '1' else '0').append('|')
            append(meta.columnIds.joinToString(",")).append('|')
            append(meta.customColumnTitles.joinToString(",")).append('|')
            meta.workingDaysByTerm.toSortedMap().entries.forEach { (k, v) ->
                append(k).append(':').append(v).append(';')
            }
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
        return buildString(10) {
            var acc = 0
            var bits = 0
            for (b in digest) {
                acc = (acc shl 8) or (b.toInt() and 0xFF)
                bits += 8
                while (bits >= 5 && length < 10) {
                    bits -= 5
                    append(BASE32_ALPHABET[(acc ushr bits) and 0x1F])
                }
                if (length >= 10) break
            }
        }
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
                customTitles.add(col.titleBn.trim().take(28))
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

        val draft = BlankSheetQrMeta(
            version = 2,
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
        val tid = page.templateId.takeIf { it.isNotBlank() } ?: computeTemplateId(draft)
        return draft.copy(templateId = tid)
    }

    /**
     * সর্ব-বামের অতি পাতলা (Version 1, 21×21 মডিউল) Firestore/Cache মেটাডেটা QR পেলোড।
     */
    fun encodeThinTemplateQr(templateId: String): String {
        val clean = templateId.trim().uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }
        return if (clean.length >= 6) "KT1:$clean" else ""
    }

    fun decodeTemplateId(rawText: String): String? {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return null
        thinQrRegex.matchEntire(trimmed)?.let { return it.groupValues[1].uppercase() }
        barcodeRegex.matchEntire(trimmed)?.let { m ->
            val tid = m.groupValues.getOrNull(5)?.trim().orEmpty()
            if (tid.length >= 6) return tid.uppercase()
        }
        if (trimmed.startsWith("K2/")) {
            val parts = trimmed.split('/')
            val tid = parts.getOrNull(11)?.trim().orEmpty()
            if (tid.length >= 6 && tid != "." && tid.all { it in 'A'..'Z' || it in '0'..'9' }) {
                return tid
            }
        }
        return null
    }

    /**
     * সর্ব-ডানের পাতলা অফলাইন QR পেলোড (K2/... আলফানিউমেরিক ফরম্যাট)।
     */
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
        val numCode = if (meta.numeralStyle == BlankNumeralStyle.ENGLISH) "E" else "B"
        val mxCode = if (meta.showSubjectMaxInHeader) "1" else "0"
        val subCode = meta.subjectKey
            ?.trim()
            ?.uppercase()
            ?.takeIf { it.isNotBlank() && it != BlankSheetCatalog.ALL_SUBJECTS_KEY }
            ?: "."
        val pageCode = if (meta.pageIndex == 1 && meta.totalPages == 1) "." else "${meta.pageIndex}:${meta.totalPages}"

        val colsCode = presetShortcuts.firstOrNull { (_, presetIds) -> presetIds == meta.columnIds }?.first
            ?: meta.columnIds.joinToString("+") { id ->
                colToShort[id] ?: run {
                    val m = customIdPrefixRegex.matchEntire(id)
                    if (m != null) "*${m.groupValues[1]}" else id.replace('/', '.')
                }
            }

        val wdCode = if (meta.workingDaysByTerm.isEmpty()) {
            "."
        } else {
            meta.workingDaysByTerm.toSortedMap().entries.joinToString("+") { (t, d) -> "$t:$d" }
        }

        val base = listOf(
            "K2",
            meta.cls.toString(),
            subCode,
            meta.term.toString(),
            kindCode,
            scopeCode,
            numCode,
            mxCode,
            pageCode,
            colsCode,
            wdCode
        ).joinToString("/")

        return if (meta.customColumnTitles.isNotEmpty()) {
            val encodedTitles = meta.customColumnTitles.joinToString("~") { t ->
                t.trim().take(28).replace("%", "%25").replace("/", "%2F").replace("~", "%7E")
            }
            "$base/$encodedTitles"
        } else {
            base
        }
    }

    /**
     * নিচের ফুটারে Code-128 বারকোডের কমপ্যাক্ট ASCII স্ট্রিং।
     */
    fun encodeBarcodeText(
        cls: Int,
        subjectKey: String?,
        term: Int,
        columns: List<BlankSheetColumn>,
        templateId: String = ""
    ): String {
        val baseRef = BlankSheetCatalog.buildSheetRefCode(cls, subjectKey, term, columns)
            .removePrefix("REF:")
            .uppercase()
        val cleanTid = templateId.trim().uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }
        return if (cleanTid.length >= 6) "$baseRef-$cleanTid" else baseRef
    }

    fun decodeChannel(rawText: String): DecodedSheetChannel {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return DecodedSheetChannel()
        val tid = decodeTemplateId(trimmed)
        val meta = decodePayload(trimmed)?.let { m ->
            if (tid != null && m.templateId.isBlank()) m.copy(templateId = tid) else m
        }
        return DecodedSheetChannel(templateId = tid, meta = meta)
    }

    fun decodePayload(rawText: String): BlankSheetQrMeta? {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return null

        // ১. কমপ্যাক্ট K2/... অফলাইন ফরম্যাট
        if (trimmed.startsWith("K2/")) {
            decodeCompactK2(trimmed)?.let { return it }
        }

        // ২. পুরনো v1 JSON পেলোড (ব্যাকওয়ার্ড কম্প্যাটিবিলিটি)
        if (trimmed.startsWith("{")) {
            decodeLegacyJson(trimmed)?.let { return it }
        }

        // ৩. নিচের বারকোড (C3-BAN-T1-FULL-TEMPLATEID) অথবা REF:C3-BAN-T1-FULL টেক্সট
        barcodeRegex.matchEntire(trimmed)?.let { m ->
            return buildMetaFromRefGroups(
                clsRaw = m.groupValues[1],
                subRaw = m.groupValues[2],
                termRaw = m.groupValues[3],
                layoutRaw = m.groupValues.getOrNull(4).orEmpty(),
                templateIdRaw = m.groupValues.getOrNull(5).orEmpty(),
                refText = "REF:C${m.groupValues[1]}-${m.groupValues[2].uppercase()}-${m.groupValues[3].uppercase()}" +
                    (m.groupValues.getOrNull(4)?.takeIf { it.isNotBlank() }?.let { "-${it.uppercase()}" }.orEmpty())
            )
        }

        val refMatch = refInlineRegex.find(trimmed) ?: return null
        return buildMetaFromRefGroups(
            clsRaw = refMatch.groupValues[1],
            subRaw = refMatch.groupValues[2],
            termRaw = refMatch.groupValues[3],
            layoutRaw = refMatch.groupValues.getOrNull(4).orEmpty(),
            templateIdRaw = "",
            refText = refMatch.value.uppercase()
        )
    }

    private fun decodeCompactK2(raw: String): BlankSheetQrMeta? {
        val parts = raw.split('/')
        if (parts.size < 11 || parts[0] != "K2") return null
        val cls = parts[1].toIntOrNull()?.takeIf { it in 1..20 } ?: return null
        val sub = parts[2].trim().uppercase().takeIf { it.isNotBlank() && it != "." }
        val term = parts[3].toIntOrNull()?.takeIf { it in -1..3 } ?: 1
        val kind = when (parts[4].trim().uppercase()) {
            "3M" -> ClassSheetKind.THREE_TERM_MARKS
            "AT" -> ClassSheetKind.ATTENDANCE_TRAITS
            else -> ClassSheetKind.SINGLE_TERM_MARKS
        }
        val scope = when (parts[5].trim().uppercase()) {
            "B" -> ClassSheetImportScope.BOTH
            "C" -> ClassSheetImportScope.CONT_ONLY
            "W" -> ClassSheetImportScope.WR_ONLY
            else -> ClassSheetImportScope.AUTO
        }
        val numStyle = if (parts[6].trim().equals("E", ignoreCase = true)) {
            BlankNumeralStyle.ENGLISH
        } else {
            BlankNumeralStyle.BENGALI
        }
        val showMax = parts[7].trim() != "0"

        val pagePart = parts[8].trim()
        val (pageIdx, totalPages) = if (pagePart == "." || pagePart.isEmpty()) {
            1 to 1
        } else {
            val ps = pagePart.split(':')
            val pi = ps.getOrNull(0)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val tp = ps.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(pi) ?: pi
            pi to tp
        }

        val colsRaw = parts[9].trim()
        val colIds = presetCodeToCols[colsRaw] ?: colsRaw.split('+').mapNotNull { token ->
            val clean = token.trim()
            if (clean.isEmpty()) null
            else shortToCol[clean] ?: if (clean.startsWith("*")) {
                val idx = clean.drop(1).toIntOrNull()
                if (idx != null) "CUS:$idx" else null
            } else {
                clean
            }
        }.ifEmpty { BlankSheetCatalog.DEFAULT_COLUMNS.map { it.id } }

        val wdMap = linkedMapOf<Int, String>()
        val wdRaw = parts[10].trim()
        if (wdRaw.isNotEmpty() && wdRaw != ".") {
            wdRaw.split('+').forEach { pair ->
                val kv = pair.split(':')
                val t = kv.getOrNull(0)?.toIntOrNull()
                val d = kv.getOrNull(1)?.filter { it.isDigit() }
                if (t in 1..3 && !d.isNullOrEmpty()) {
                    wdMap[t!!] = d
                }
            }
        }

        val customTitles = if (parts.size > 11) {
            val merged = parts.subList(11, parts.size).joinToString("/")
            if (merged.isBlank()) emptyList()
            else merged.split('~').map { enc ->
                enc.replace("%7E", "~").replace("%2F", "/").replace("%25", "%").trim()
            }
        } else {
            emptyList()
        }

        val colsForRef = resolveColumnsForRef(colIds, customTitles)
        val draft = BlankSheetQrMeta(
            version = 2,
            cls = cls,
            subjectKey = sub,
            term = term,
            sheetKind = kind,
            importScope = scope,
            columnIds = colIds,
            customColumnTitles = customTitles,
            numeralStyle = numStyle,
            showSubjectMaxInHeader = showMax,
            workingDaysByTerm = wdMap,
            pageIndex = pageIdx,
            totalPages = totalPages,
            refCode = BlankSheetCatalog.buildSheetRefCode(cls, sub, term, colsForRef)
        )
        return draft.copy(templateId = computeTemplateId(draft))
    }

    private fun decodeLegacyJson(trimmed: String): BlankSheetQrMeta? {
        val root = runCatching { MiniJsonParser(trimmed).parseObject() }.getOrNull() ?: return null
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

        val colsForRef = resolveColumnsForRef(rawCols, customTitles)
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

    private fun buildMetaFromRefGroups(
        clsRaw: String,
        subRaw: String,
        termRaw: String,
        layoutRaw: String,
        templateIdRaw: String,
        refText: String
    ): BlankSheetQrMeta? {
        val cls = clsRaw.toIntOrNull()?.takeIf { it in 1..20 } ?: return null
        val subUpper = subRaw.uppercase()
        val sub = subUpper.takeIf { it != "ATT" && it != "GEN" }
        val term = when (termRaw.uppercase()) {
            "T1" -> 1
            "T2" -> 2
            "T3" -> 3
            "ANN" -> 0
            "TC" -> -1
            else -> 1
        }
        val (kind, presetCols) = when (layoutRaw.uppercase()) {
            "TRAITS", "ATT" -> ClassSheetKind.ATTENDANCE_TRAITS to BlankSheetCatalog.presetColumns(BlankSheetPreset.THREE_TERM_ATTENDANCE_TRAITS)
            "3MARKS" -> ClassSheetKind.THREE_TERM_MARKS to BlankSheetCatalog.presetColumns(BlankSheetPreset.THREE_TERM_MARKS)
            "FULL" -> ClassSheetKind.SINGLE_TERM_MARKS to BlankSheetCatalog.presetColumns(BlankSheetPreset.FULL_MARKS)
            "TOTAL" -> ClassSheetKind.SINGLE_TERM_MARKS to BlankSheetCatalog.presetColumns(BlankSheetPreset.ROLL_NAME_TOTAL)
            else -> ClassSheetKind.SINGLE_TERM_MARKS to BlankSheetCatalog.presetColumns(BlankSheetPreset.COMPACT_MARKS)
        }
        return BlankSheetQrMeta(
            version = 2,
            templateId = templateIdRaw.uppercase(),
            cls = cls,
            subjectKey = sub,
            term = term,
            sheetKind = kind,
            importScope = ClassSheetImportScope.BOTH,
            columnIds = presetCols.map { it.id },
            refCode = refText
        )
    }

    private fun resolveColumnsForRef(
        colIds: List<String>,
        customTitles: List<String>
    ): List<BlankSheetColumn> = colIds.mapNotNull { colToken ->
        val customMatch = customIdPrefixRegex.matchEntire(colToken)
        if (customMatch != null) {
            val cIdx = customMatch.groupValues[1].toIntOrNull() ?: 0
            val cTitle = customTitles.getOrNull(cIdx).orEmpty().ifBlank { "কাস্টম" }
            BlankSheetColumn(id = colToken, titleBn = cTitle, isCustom = true)
        } else {
            BlankSheetCatalog.byId(colToken)
        }
    }.ifEmpty { BlankSheetCatalog.DEFAULT_COLUMNS }

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

    fun encodeBarcodeMatrix(text: String): BitMatrix? {
        val clean = text.trim()
        if (clean.isEmpty()) return null
        return runCatching {
            val hints = mapOf(EncodeHintType.MARGIN to 0)
            Code128Writer().encode(clean, BarcodeFormat.CODE_128, 0, 1, hints)
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

    fun shortColumnsPreviewBn(meta: BlankSheetQrMeta, maxItems: Int = 8): String {
        val labels = meta.columnIds.take(maxItems).map { colToken ->
            val customMatch = customIdPrefixRegex.matchEntire(colToken)
            if (customMatch != null) {
                val cIdx = customMatch.groupValues[1].toIntOrNull() ?: 0
                meta.customColumnTitles.getOrNull(cIdx).orEmpty().ifBlank { "কাস্টম" }
            } else {
                BlankSheetCatalog.byId(colToken)?.pickerLabelBn ?: colToken
            }
        }
        val suffix = if (meta.columnIds.size > maxItems) "…" else ""
        return labels.joinToString(", ") + suffix
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
            appendLine("[নিশ্চিত শিট কাঠামো — QR/বারকোড ও টেমপ্লেট থেকে প্রাপ্ত]:")
            appendLine("- নিশ্চিত তথ্য: cls=${meta.cls}, subKey=${meta.subjectKey ?: ""}, term=${meta.term.coerceIn(1, 3)}, sheetKind=${meta.sheetKind.name}, বিষয়=$subStr, প্রান্তিক=$termLabel, পৃষ্ঠা=${meta.pageIndex}/${meta.totalPages}।")
            appendLine("- টেবিলের বাম থেকে ডান কলাম ক্রম: $colListStr।")
            appendLine("- মুদ্রিত সংখ্যা ধরন: ${meta.numeralStyle.labelBn}; তবে আউটপুট JSON-এ সব সংখ্যা ইংরেজি অঙ্কে (0-9) দিন।")
            if (meta.workingDaysByTerm.isNotEmpty()) {
                val wdStr = meta.workingDaysByTerm.entries.joinToString(", ") { (t, d) -> "${termNameBn(t)}=$d দিন" }
                appendLine("- প্রাপ্ত মোট কার্যদিবস: $wdStr (sharedWorkingDays-এ বসান)।")
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

    /**
     * ন্যূনতম pure-Kotlin JSON পার্সার (পুরনো QR পেলোড ডিকোড করার জন্য)।
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
            pos++
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
