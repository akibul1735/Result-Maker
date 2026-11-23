package com.kgps.result.backup

import android.content.ContentResolver
import android.net.Uri
import com.kgps.result.ClassResolver
import com.kgps.result.Repo
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.util.zip.ZipInputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

object XlsxImporter {

    private const val MAX_ENTRY_BYTES = 20 * 1024 * 1024 // 20 MB per XML entry

    data class ImportedSheet(
        val name: String,
        val rows: List<List<String>>
    )

    fun isXlsxBytes(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        val b0 = bytes[0].toInt() and 0xFF
        val b1 = bytes[1].toInt() and 0xFF
        val b2 = bytes[2].toInt() and 0xFF
        val b3 = bytes[3].toInt() and 0xFF
        return b0 == 0x50 && b1 == 0x4B && (b2 == 0x03 || b2 == 0x05) && (b3 == 0x04 || b3 == 0x06)
    }

    fun isLegacyXlsBytes(bytes: ByteArray): Boolean {
        if (bytes.size < 8) return false
        val magic = intArrayOf(0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1)
        return magic.indices.all { (bytes[it].toInt() and 0xFF) == magic[it] }
    }

    fun readSpreadsheetAsText(contentResolver: ContentResolver, uri: Uri): Result<String> = runCatching {
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("ফাইলটি খোলা যায়নি")
        if (bytes.isEmpty()) {
            throw IllegalStateException("নির্বাচিত ফাইলটি খালি")
        }
        when {
            isLegacyXlsBytes(bytes) -> {
                throw IllegalArgumentException(
                    "পুরোনো .xls ফরম্যাট সরাসরি পড়া যায় না — অনুগ্রহ করে Excel বা Google Sheets থেকে .xlsx বা .csv ফরম্যাটে সেভ করে নিন।"
                )
            }
            isXlsxBytes(bytes) -> {
                val csv = importWorkbookAsCsvText(bytes)
                if (csv.isBlank()) {
                    throw IllegalStateException("Excel ফাইলে আমদানির মতো কোনো তথ্য পাওয়া যায়নি")
                }
                csv
            }
            else -> {
                val text = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
                if (text.isBlank()) {
                    throw IllegalStateException("নির্বাচিত ফাইলটি খালি")
                }
                text
            }
        }
    }

    private fun inferSheetClassText(
        sheetName: String,
        detectedCls: Int?,
        defaultCls: Int?
    ): String? {
        detectedCls?.let { return it.toString() }
        Repo.parseClassNum(sheetName)?.let { return it.toString() }
        if (ClassResolver.looksValidAsNewCustomClass(sheetName, isSheetTabName = true)) {
            return sheetName.trim()
        }
        return defaultCls?.toString()
    }

    fun importWorkbookAsCsvText(bytes: ByteArray, defaultCls: Int? = null): String {
        val sheets = parseWorkbookSheets(bytes).filter { s ->
            s.rows.any { row -> row.any { it.isNotBlank() } }
        }
        if (sheets.isEmpty()) return ""

        if (sheets.size == 1) {
            val only = sheets.first()
            val sheetCsv = rowsToCsv(only.rows)
            val parsed = Repo.parseSmartCsvSheet(sheetCsv)
            if (parsed.rows.size >= 2) {
                val headers = parsed.rows.first()
                val cols = Repo.detectCsvColumns(
                    rows = listOf(headers),
                    existingCustomFields = emptyList(),
                    contextSubjectKey = parsed.detectedSubjectKey,
                    contextTerm = parsed.detectedTerm
                )
                val hasClsCol = cols.any { it.targetType == "standard" && it.targetKey == "cls" }
                val tabClsText = inferSheetClassText(only.name, parsed.detectedCls, defaultCls)
                if (hasClsCol || tabClsText == null) {
                    return sheetCsv
                }
            } else {
                return sheetCsv
            }
        }

        // Unified merge for multi-sheet workbooks (or single sheet needing tab-name class/working-days enrichment)
        data class NormalizedSheet(
            val headers: List<String>,
            val dataRows: List<List<String>>
        )

        val normalizedSheets = mutableListOf<NormalizedSheet>()
        for (sheet in sheets) {
            val sheetCsv = rowsToCsv(sheet.rows)
            val parsed = Repo.parseSmartCsvSheet(sheetCsv)
            if (parsed.rows.size < 2) continue

            val origHeaders = parsed.rows.first()
            val origData = parsed.rows.drop(1).filter { row -> row.any { it.isNotBlank() } }
            if (origData.isEmpty()) continue

            val detectedCols = Repo.detectCsvColumns(
                rows = listOf(origHeaders),
                existingCustomFields = emptyList(),
                contextSubjectKey = parsed.detectedSubjectKey,
                contextTerm = parsed.detectedTerm
            )
            val clsColIdx = detectedCols.indexOfFirst { it.targetType == "standard" && it.targetKey == "cls" }
            val inferredClsText = inferSheetClassText(sheet.name, parsed.detectedCls, defaultCls)

            val headersOut = origHeaders.toMutableList()
            val dataOut = origData.map { it.toMutableList() }

            if (clsColIdx >= 0) {
                if (inferredClsText != null) {
                    dataOut.forEach { r ->
                        while (r.size <= clsColIdx) r.add("")
                        if (r[clsColIdx].isBlank()) r[clsColIdx] = inferredClsText
                    }
                }
            } else if (inferredClsText != null) {
                headersOut.add(0, "cls")
                dataOut.forEach { r -> r.add(0, inferredClsText) }
            }

            // Preserve sheet-level working days banner (if sheet itself didn't include a DAYS_t column)
            parsed.detectedWorkingDaysByTerm.forEach { (term, days) ->
                val dKey = "DAYS_$term"
                val hasDaysCol = detectedCols.any { it.targetType == "text" && it.targetKey == dKey }
                if (!hasDaysCol && days.isNotBlank()) {
                    headersOut.add(dKey)
                    dataOut.forEach { r ->
                        while (r.size < headersOut.size - 1) r.add("")
                        r.add(days)
                    }
                }
            }

            normalizedSheets.add(NormalizedSheet(headersOut, dataOut))
        }

        if (normalizedSheets.isEmpty()) return ""

        val unifiedHeaders = LinkedHashSet<String>()
        normalizedSheets.forEach { ns ->
            ns.headers.forEachIndexed { idx, h ->
                val clean = h.trim().ifBlank { "col_${idx + 1}" }
                unifiedHeaders.add(clean)
            }
        }
        val finalHeaders = unifiedHeaders.toList()
        val headerIndex = finalHeaders.withIndex().associate { it.value to it.index }

        val mergedRows = mutableListOf<List<String>>()
        mergedRows.add(finalHeaders)
        normalizedSheets.forEach { ns ->
            ns.dataRows.forEach { row ->
                val outRow = MutableList(finalHeaders.size) { "" }
                ns.headers.forEachIndexed { colIdx, rawH ->
                    val clean = rawH.trim().ifBlank { "col_${colIdx + 1}" }
                    val targetIdx = headerIndex[clean] ?: return@forEachIndexed
                    val v = row.getOrNull(colIdx).orEmpty()
                    if (v.isNotBlank() && outRow[targetIdx].isBlank()) {
                        outRow[targetIdx] = v
                    }
                }
                if (outRow.any { it.isNotBlank() }) {
                    mergedRows.add(outRow)
                }
            }
        }
        return rowsToCsv(mergedRows)
    }

    fun parseWorkbookSheets(bytes: ByteArray): List<ImportedSheet> {
        val entries = unzipXmlEntries(bytes)
        if (entries.isEmpty()) return emptyList()

        val sharedStrings = entries["xl/sharedStrings.xml"]
            ?.let(::parseSharedStrings)
            ?: emptyList()

        val relTargets = entries["xl/_rels/workbook.xml.rels"]
            ?.let(::parseRelationships)
            ?: emptyMap()

        val workbookSheetRefs = entries["xl/workbook.xml"]
            ?.let { parseWorkbookSheetList(it, relTargets) }
            ?: emptyList()

        val orderedSheets: List<Pair<String, ByteArray>> = if (workbookSheetRefs.isNotEmpty()) {
            workbookSheetRefs.mapNotNull { (name, targetPath) ->
                val normalized = normalizeZipPath(targetPath)
                val xmlBytes = entries[normalized]
                    ?: entries["xl/$normalized"]
                    ?: entries.entries.firstOrNull { it.key.equals(normalized, ignoreCase = true) }?.value
                if (xmlBytes != null) name to xmlBytes else null
            }
        } else {
            entries.entries
                .filter { (path, _) -> path.startsWith("xl/worksheets/sheet") && path.endsWith(".xml") }
                .sortedBy { it.key }
                .mapIndexed { index, entry -> "Sheet ${index + 1}" to entry.value }
        }

        return orderedSheets.map { (name, xmlBytes) ->
            ImportedSheet(name = name, rows = parseWorksheetXml(xmlBytes, sharedStrings))
        }
    }

    private fun unzipXmlEntries(bytes: ByteArray): Map<String, ByteArray> {
        val out = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name.replace('\\', '/').trimStart('/')
                if (!entry.isDirectory &&
                    (name.endsWith(".xml", ignoreCase = true) || name.endsWith(".rels", ignoreCase = true))
                ) {
                    val buf = readCappedBytes(zis, MAX_ENTRY_BYTES)
                    out[name] = buf
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return out
    }

    private fun readCappedBytes(zis: ZipInputStream, maxBytes: Int): ByteArray {
        val baos = java.io.ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0
        while (true) {
            val read = zis.read(buf)
            if (read <= 0) break
            total += read
            if (total > maxBytes) {
                throw IllegalStateException("Excel XML এন্ট্রি অত্যন্ত বড় (২০ MB-এর বেশি)")
            }
            baos.write(buf, 0, read)
        }
        return baos.toByteArray()
    }

    private fun secureDocumentBuilderFactory(): DocumentBuilderFactory =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            runCatching { setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            isExpandEntityReferences = false
        }

    private fun parseXml(bytes: ByteArray): Element? = runCatching {
        val doc = secureDocumentBuilderFactory().newDocumentBuilder().parse(ByteArrayInputStream(bytes))
        doc.documentElement
    }.getOrNull()

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val root = parseXml(bytes) ?: return emptyList()
        val list = mutableListOf<String>()
        val children = root.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node.nodeType != Node.ELEMENT_NODE) continue
            if (localName(node) != "si") continue
            list.add(extractSiText(node))
        }
        return list
    }

    private fun extractSiText(siNode: Node): String {
        val sb = StringBuilder()
        val children = siNode.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child.nodeType != Node.ELEMENT_NODE) continue
            when (localName(child)) {
                "t" -> sb.append(child.textContent.orEmpty())
                "r" -> {
                    val runChildren = child.childNodes
                    for (j in 0 until runChildren.length) {
                        val rc = runChildren.item(j)
                        if (rc.nodeType == Node.ELEMENT_NODE && localName(rc) == "t") {
                            sb.append(rc.textContent.orEmpty())
                        }
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun parseRelationships(bytes: ByteArray): Map<String, String> {
        val root = parseXml(bytes) ?: return emptyMap()
        val map = mutableMapOf<String, String>()
        val children = root.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node.nodeType != Node.ELEMENT_NODE || localName(node) != "Relationship") continue
            val el = node as Element
            val id = el.getAttribute("Id").trim()
            val target = el.getAttribute("Target").trim()
            if (id.isNotEmpty() && target.isNotEmpty()) {
                map[id] = target
            }
        }
        return map
    }

    private fun parseWorkbookSheetList(
        bytes: ByteArray,
        relTargets: Map<String, String>
    ): List<Pair<String, String>> {
        val root = parseXml(bytes) ?: return emptyList()
        val out = mutableListOf<Pair<String, String>>()
        val sheetNodes = root.getElementsByTagName("*")
        for (i in 0 until sheetNodes.length) {
            val node = sheetNodes.item(i)
            if (node.nodeType != Node.ELEMENT_NODE || localName(node) != "sheet") continue
            val el = node as Element
            val name = el.getAttribute("name").ifBlank { "Sheet ${out.size + 1}" }
            val rid = el.getAttribute("r:id").ifBlank { el.getAttribute("id") }
            val target = relTargets[rid] ?: "worksheets/sheet${out.size + 1}.xml"
            val fullTarget = if (target.startsWith("/") || target.startsWith("xl/")) {
                target.trimStart('/')
            } else {
                "xl/$target"
            }
            out.add(name to fullTarget)
        }
        return out
    }

    private fun normalizeZipPath(raw: String): String {
        val parts = raw.replace('\\', '/').trimStart('/').split('/')
        val stack = ArrayDeque<String>()
        for (p in parts) {
            when (p) {
                "", "." -> {}
                ".." -> if (stack.isNotEmpty()) stack.removeLast()
                else -> stack.addLast(p)
            }
        }
        return stack.joinToString("/")
    }

    private fun parseWorksheetXml(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val root = parseXml(bytes) ?: return emptyList()
        val sheetData = findFirstChildByName(root, "sheetData") ?: return emptyList()
        val rawRows = mutableListOf<MutableList<String>>()
        var maxWidth = 0

        val rowNodes = sheetData.childNodes
        for (i in 0 until rowNodes.length) {
            val rowNode = rowNodes.item(i)
            if (rowNode.nodeType != Node.ELEMENT_NODE || localName(rowNode) != "row") continue
            val rowCells = mutableMapOf<Int, String>()
            var fallbackColIdx = 0

            val cellNodes = rowNode.childNodes
            for (j in 0 until cellNodes.length) {
                val cellNode = cellNodes.item(j)
                if (cellNode.nodeType != Node.ELEMENT_NODE || localName(cellNode) != "c") continue
                val cEl = cellNode as Element
                val ref = cEl.getAttribute("r").trim()
                val colIdx = if (ref.isNotEmpty()) {
                    parseColumnIndexFromRef(ref) ?: fallbackColIdx
                } else {
                    fallbackColIdx
                }
                fallbackColIdx = colIdx + 1

                val value = extractCellValue(cEl, sharedStrings)
                if (value.isNotEmpty()) {
                    rowCells[colIdx] = value
                }
            }

            if (rowCells.isNotEmpty()) {
                val width = (rowCells.keys.maxOrNull() ?: 0) + 1
                if (width > maxWidth) maxWidth = width
                val list = MutableList(width) { idx -> rowCells[idx].orEmpty() }
                rawRows.add(list)
            }
        }

        if (rawRows.isEmpty()) return emptyList()
        return rawRows.map { r ->
            if (r.size >= maxWidth) r else r + List(maxWidth - r.size) { "" }
        }
    }

    private fun extractCellValue(cellEl: Element, sharedStrings: List<String>): String {
        val type = cellEl.getAttribute("t").trim()
        return when (type) {
            "s" -> {
                val idxStr = findFirstChildByName(cellEl, "v")?.textContent?.trim().orEmpty()
                val idx = idxStr.toIntOrNull()
                if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else ""
            }
            "inlineStr" -> {
                val isNode = findFirstChildByName(cellEl, "is")
                if (isNode != null) extractSiText(isNode) else ""
            }
            "str", "e" -> {
                findFirstChildByName(cellEl, "v")?.textContent.orEmpty()
            }
            "b" -> {
                val raw = findFirstChildByName(cellEl, "v")?.textContent?.trim().orEmpty()
                if (raw == "1") "TRUE" else if (raw == "0") "FALSE" else raw
            }
            else -> {
                val raw = findFirstChildByName(cellEl, "v")?.textContent?.trim().orEmpty()
                formatNumericCell(raw)
            }
        }
    }

    private fun formatNumericCell(raw: String): String {
        if (raw.isEmpty()) return ""
        val bd = runCatching { BigDecimal(raw) }.getOrNull() ?: return raw
        val normalized = bd.stripTrailingZeros()
        return if (normalized.scale() <= 0) {
            normalized.toPlainString()
        } else {
            normalized.toPlainString()
        }
    }

    internal fun parseColumnIndexFromRef(ref: String): Int? {
        var col = 0
        var seenLetter = false
        for (ch in ref) {
            if (ch in 'A'..'Z') {
                seenLetter = true
                col = col * 26 + (ch - 'A' + 1)
            } else if (ch in 'a'..'z') {
                seenLetter = true
                col = col * 26 + (ch - 'a' + 1)
            } else if (ch in '0'..'9') {
                break
            }
        }
        return if (seenLetter && col > 0) col - 1 else null
    }

    private fun findFirstChildByName(parent: Node, name: String): Node? {
        val children = parent.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child.nodeType == Node.ELEMENT_NODE && localName(child) == name) {
                return child
            }
        }
        return null
    }

    private fun localName(node: Node): String {
        val n = node.localName ?: node.nodeName
        return n.substringAfter(':')
    }

    private fun quoteCsv(cell: String): String =
        if (cell.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + cell.replace("\"", "\"\"") + "\""
        } else {
            cell
        }

    private fun rowsToCsv(rows: List<List<String>>): String = buildString {
        rows.forEach { row ->
            append(row.joinToString(",") { quoteCsv(it) }).append('\n')
        }
    }
}
