package com.kgps.result.backup

import com.kgps.result.CsvHeaderStyle
import com.kgps.result.CustomFieldDef
import com.kgps.result.Repo
import com.kgps.result.Student
import com.kgps.result.activeClassIds
import com.kgps.result.classNameBn
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object XlsxExporter {

    data class SheetData(
        val name: String,
        val headers: List<String>,
        val rows: List<List<String>>
    )

    fun exportAllClassesWorkbook(
        students: List<Student>,
        customFields: List<CustomFieldDef>,
        style: CsvHeaderStyle,
        classes: List<Int> = activeClassIds()
    ): ByteArray {
        val sheets = classes.map { cls ->
            val tabName = when (style) {
                CsvHeaderStyle.FRIENDLY_BN -> "${classNameBn(cls)} শ্রেণি"
                CsvHeaderStyle.FRIENDLY_EN -> "Class $cls"
                CsvHeaderStyle.SYSTEM_KEYS -> "Class_$cls"
            }
            val (headers, rows) = Repo.toCsvTable(
                list = students,
                customFields = customFields,
                style = style,
                targetCls = cls
            )
            SheetData(name = sanitizeSheetName(tabName), headers = headers, rows = rows)
        }
        return buildWorkbook(sheets)
    }

    private fun buildWorkbook(sheets: List<SheetData>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            writeEntry(zos, "[Content_Types].xml", contentTypesXml(sheets.size))
            writeEntry(zos, "_rels/.rels", rootRelsXml())
            writeEntry(zos, "xl/workbook.xml", workbookXml(sheets))
            writeEntry(zos, "xl/_rels/workbook.xml.rels", workbookRelsXml(sheets.size))
            writeEntry(zos, "xl/styles.xml", stylesXml())
            sheets.forEachIndexed { idx, sheet ->
                writeEntry(zos, "xl/worksheets/sheet${idx + 1}.xml", sheetXml(sheet))
            }
        }
        return baos.toByteArray()
    }

    private fun writeEntry(zos: ZipOutputStream, path: String, content: String) {
        zos.putNextEntry(ZipEntry(path))
        zos.write(content.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
    }

    private fun sanitizeSheetName(raw: String): String {
        val cleaned = raw.replace(Regex("[\\\\/?*\\[\\]:]"), " ").trim()
        return cleaned.take(31).ifBlank { "Sheet" }
    }

    private fun escapeXml(raw: String): String {
        val sb = StringBuilder(raw.length + 8)
        for (ch in raw) {
            val code = ch.code
            if (code < 0x20 && ch != '\t' && ch != '\n' && ch != '\r') continue
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun colName(zeroBasedIndex: Int): String {
        var n = zeroBasedIndex + 1
        val sb = StringBuilder()
        while (n > 0) {
            val rem = (n - 1) % 26
            sb.append(('A'.code + rem).toChar())
            n = (n - 1) / 26
        }
        return sb.reverse().toString()
    }

    private fun isPlainNumber(value: String): Boolean {
        if (value.isEmpty() || value.length > 12) return false
        if (value.length > 1 && value.startsWith("0") && !value.startsWith("0.")) return false
        return value.toDoubleOrNull()?.isFinite() == true
    }

    private fun contentTypesXml(sheetCount: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
        for (i in 1..sheetCount) {
            append("""<Override PartName="/xl/worksheets/sheet$i.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
        }
        append("""</Types>""")
    }

    private fun rootRelsXml(): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        append("""<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>""")
        append("""</Relationships>""")
    }

    private fun workbookXml(sheets: List<SheetData>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
        append("""<sheets>""")
        sheets.forEachIndexed { idx, s ->
            append("""<sheet name="${escapeXml(s.name)}" sheetId="${idx + 1}" r:id="rId${idx + 1}"/>""")
        }
        append("""</sheets>""")
        append("""</workbook>""")
    }

    private fun workbookRelsXml(sheetCount: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        for (i in 1..sheetCount) {
            append("""<Relationship Id="rId$i" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$i.xml"/>""")
        }
        append("""<Relationship Id="rId${sheetCount + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
        append("""</Relationships>""")
    }

    private fun stylesXml(): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        append("""<fonts count="2">""")
        append("""<font><sz val="11"/><name val="Calibri"/></font>""")
        append("""<font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>""")
        append("""</fonts>""")
        append("""<fills count="3">""")
        append("""<fill><patternFill patternType="none"/></fill>""")
        append("""<fill><patternFill patternType="gray125"/></fill>""")
        append("""<fill><patternFill patternType="solid"><fgColor rgb="FF0F766E"/><bgColor indexed="64"/></patternFill></fill>""")
        append("""</fills>""")
        append("""<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>""")
        append("""<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""")
        append("""<cellXfs count="2">""")
        append("""<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>""")
        append("""<xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>""")
        append("""</cellXfs>""")
        append("""</styleSheet>""")
    }

    private fun sheetXml(sheet: SheetData): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        append("""<sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>""")
        append("""<cols>""")
        sheet.headers.forEachIndexed { colIdx, h ->
            val w = (h.length + 4).coerceIn(10, 34)
            append("""<col min="${colIdx + 1}" max="${colIdx + 1}" width="$w" customWidth="1"/>""")
        }
        append("""</cols>""")
        append("""<sheetData>""")

        // Header Row (row 1)
        append("""<row r="1" ht="24" customHeight="1">""")
        sheet.headers.forEachIndexed { colIdx, h ->
            val ref = "${colName(colIdx)}1"
            append("""<c r="$ref" t="inlineStr" s="1"><is><t xml:space="preserve">${escapeXml(h)}</t></is></c>""")
        }
        append("""</row>""")

        // Data Rows
        sheet.rows.forEachIndexed { rIdx, row ->
            val rowNum = rIdx + 2
            append("""<row r="$rowNum">""")
            row.forEachIndexed { colIdx, cell ->
                if (cell.isNotEmpty()) {
                    val ref = "${colName(colIdx)}$rowNum"
                    if (isPlainNumber(cell)) {
                        append("""<c r="$ref" t="n"><v>$cell</v></c>""")
                    } else {
                        append("""<c r="$ref" t="inlineStr"><is><t xml:space="preserve">${escapeXml(cell)}</t></is></c>""")
                    }
                }
            }
            append("""</row>""")
        }

        append("""</sheetData>""")
        append("""</worksheet>""")
    }
}
