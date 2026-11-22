package com.kgps.result

data class FontPair(
    val bengaliFontId: String,
    val englishFontId: String
)

data class MixedScriptRun(
    val text: String,
    val isBengali: Boolean
)

/**
 * Encodes the selected Bengali and English document fonts into a single <= 40-char token
 * compatible with existing Firestore security rules (`schools.pdfFont <= 40 chars`).
 */
fun encodePdfFontPair(bengaliFontId: String, englishFontId: String): String {
    val validBn = bengaliFontId.trim().takeIf { id -> BENGALI_PDF_FONT_LIST.any { it.id == id } }
        ?: "noto_serif"
    val validEn = englishFontId.trim().takeIf { id -> ENGLISH_PDF_FONT_LIST.any { it.id == id } }
        ?: "times_new_roman"
    return "$validBn|$validEn"
}

/**
 * Decodes either a packed `"bn|en"` token or a legacy single font ID (plus an optional
 * explicit English font ID) into a valid [FontPair].
 */
fun decodePdfFontPair(
    rawPdfFont: String?,
    explicitEnglishFont: String? = null
): FontPair {
    val cleanRaw = rawPdfFont?.trim().orEmpty()
    val explicitEn = explicitEnglishFont?.trim()?.takeIf { id ->
        ENGLISH_PDF_FONT_LIST.any { it.id == id }
    }
    if ("|" in cleanRaw) {
        val parts = cleanRaw.split("|", limit = 2)
        val bn = parts.getOrNull(0)?.trim()?.takeIf { id ->
            BENGALI_PDF_FONT_LIST.any { it.id == id }
        } ?: "noto_serif"
        val en = explicitEn ?: parts.getOrNull(1)?.trim()?.takeIf { id ->
            ENGLISH_PDF_FONT_LIST.any { it.id == id }
        } ?: "times_new_roman"
        return FontPair(bengaliFontId = bn, englishFontId = en)
    }

    val isLegacyBn = BENGALI_PDF_FONT_LIST.any { it.id == cleanRaw }
    val isLegacyEn = ENGLISH_PDF_FONT_LIST.any { it.id == cleanRaw }
    val bn = if (isLegacyBn) cleanRaw else "noto_serif"
    val en = explicitEn ?: if (isLegacyEn) cleanRaw else "times_new_roman"
    return FontPair(bengaliFontId = bn, englishFontId = en)
}

private fun isBengaliStrongChar(c: Char): Boolean = c in '\u0980'..'\u09FF'

private fun isEnglishStrongChar(c: Char): Boolean =
    (c in 'A'..'Z') || (c in 'a'..'z') || (c in '0'..'9')

private fun isOpeningBracketOrQuote(c: Char): Boolean = c == '(' || c == '[' || c == '{'

/**
 * Splits text into contiguous Bengali vs English/Latin+digits runs while keeping Bengali
 * conjuncts/joiners (`\u200C`, `\u200D`) intact and snapping opening brackets (`(`, `[`, `{`)
 * to the script of the enclosed text.
 */
fun splitMixedScriptRuns(text: String): List<MixedScriptRun> {
    if (text.isEmpty()) return emptyList()

    fun peekNextStrongIsBengali(startIndex: Int, fallback: Boolean): Boolean {
        for (i in startIndex until text.length) {
            val ch = text[i]
            if (isBengaliStrongChar(ch)) return true
            if (isEnglishStrongChar(ch)) return false
        }
        return fallback
    }

    var currentIsBn = peekNextStrongIsBengali(0, fallback = true)
    val sb = StringBuilder()
    val runs = mutableListOf<MixedScriptRun>()

    for (i in text.indices) {
        val ch = text[i]
        val nextIsBn: Boolean = when {
            isBengaliStrongChar(ch) -> true
            isEnglishStrongChar(ch) -> false
            ch == '\u200C' || ch == '\u200D' -> currentIsBn
            isOpeningBracketOrQuote(ch) -> peekNextStrongIsBengali(i + 1, currentIsBn)
            else -> currentIsBn
        }
        if (nextIsBn != currentIsBn && sb.isNotEmpty()) {
            runs.add(MixedScriptRun(sb.toString(), currentIsBn))
            sb.setLength(0)
        }
        currentIsBn = nextIsBn
        sb.append(ch)
    }
    if (sb.isNotEmpty()) {
        runs.add(MixedScriptRun(sb.toString(), currentIsBn))
    }
    return runs
}
