package com.kgps.result.ai.offline

enum class ScanEngineMode(val labelBn: String, val shortBn: String) {
    CLOUD_GEMINI("Gemini AI (অনলাইন)", "Gemini AI"),
    OFFLINE_TABLE("অফলাইন টেবিল স্ক্যান", "অফলাইন টেবিল")
}

enum class OfflineDigitScript(val labelBn: String, val shortBn: String) {
    BENGALI("বাংলা সংখ্যা (০–৯)", "বাংলা ০–৯"),
    ENGLISH("English Digits (0–9)", "English 0-9"),
    AUTO("স্বয়ংক্রিয় (বাংলা / English)", "স্বয়ংক্রিয়")
}

enum class OfflineTableLayoutPreset(val labelBn: String, val hintBn: String) {
    AUTO("স্বয়ংক্রিয় কলাম", "ছবির কলাম সংখ্যা ও ধরন দেখে নিজেই রোল, নাম (থাকলে বাদ) ও নম্বর বসাবে"),
    ROLL_NAME_SINGLE("রোল · (নাম) · ১ নম্বর", "শুধু ধারাবাহিক বা শুধু সামষ্টিক বা শিল্পকলার জন্য"),
    ROLL_NAME_CONT_WR_TOTAL("রোল · (নাম) · ধারাবাহিক · সামষ্টিক (· মোট)", "নাম থাকুক বা না থাকুক — সংক্ষিপ্ত নম্বরপত্র ফরম্যাটের জন্য"),
    ROLL_NAME_FULL_BREAKDOWN("রোল · (নাম) · ৪ অংশ · ধারাবাহিক · সামষ্টিক · মোট", "পাঠ্যবই, সক্রিয়তা, ভাষা ও ক্লাস টেস্টসহ পূর্ণাঙ্গ শীট")
}

data class DigitPrediction(
    val digit: Int,
    val confidence: Float,
    val secondDigit: Int,
    val secondConfidence: Float,
    val probabilities: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DigitPrediction) return false
        return digit == other.digit &&
            confidence == other.confidence &&
            secondDigit == other.secondDigit &&
            secondConfidence == other.secondConfidence &&
            probabilities.contentEquals(other.probabilities)
    }

    override fun hashCode(): Int {
        var result = digit
        result = 31 * result + confidence.hashCode()
        result = 31 * result + secondDigit
        result = 31 * result + secondConfidence.hashCode()
        result = 31 * result + probabilities.contentHashCode()
        return result
    }
}

data class CellNumberCandidate(
    val text: String,
    val score: Float
)

data class CellReadResult(
    val text: String,
    val confidence: Float,
    val digitCount: Int,
    val candidates: List<CellNumberCandidate>,
    val isBlank: Boolean,
    val hadAmbiguousSplit: Boolean = false
)

enum class ColumnRole {
    SERIAL_SKIP,
    ROLL,
    NAME_SKIP,
    FIELD_TB,
    FIELD_ACT,
    FIELD_LANG,
    FIELD_CT,
    FIELD_CONT,
    FIELD_WR,
    FIELD_TOTAL,
    FIELD_GRAND,
    EXTRA_SKIP
}

fun ColumnRole.toMarkFieldKey(): String? = when (this) {
    ColumnRole.FIELD_TB -> "TB"
    ColumnRole.FIELD_ACT -> "ACT"
    ColumnRole.FIELD_LANG -> "LANG"
    ColumnRole.FIELD_CT -> "CT"
    ColumnRole.FIELD_CONT -> "CONT"
    ColumnRole.FIELD_WR -> "WR"
    ColumnRole.FIELD_TOTAL -> "TOTAL"
    ColumnRole.FIELD_GRAND -> "GRAND"
    else -> null
}

data class OfflineScanDiagnostics(
    val detectedScript: OfflineDigitScript,
    val rowCount: Int,
    val headerRowsSkipped: Int,
    val columnCount: Int,
    val skippedNameColumnIndex: Int?,
    val columnRoles: List<ColumnRole>,
    val processingMs: Long,
    val usedDeskewDegrees: Float
)
