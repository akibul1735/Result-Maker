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

enum class ColumnRole(
    val shortBn: String,
    val titleBn: String,
    val isSkipped: Boolean = false,
    val isRepeatable: Boolean = false
) {
    SERIAL_SKIP("ক্রমিক (বাদ)", "ক্রমিক নম্বর (পড়া হবে না)", isSkipped = true),
    ROLL("রোল", "শিক্ষার্থীর রোল নম্বর"),
    NAME_SKIP("নাম (বাদ)", "শিক্ষার্থীর নাম (বাদ দিয়ে ডেটাবেস থেকে নেবে)", isSkipped = true),
    FIELD_TB("পাঠ্যবই", "পাঠ্যবই মূল্যায়ন (TB)"),
    FIELD_ACT("সক্রিয়তা", "শ্রেণি সক্রিয়তা (ACT)"),
    FIELD_LANG("ভাষা", "ভাষা ব্যবহার (LANG)"),
    FIELD_CT("ক্লাস টেস্ট", "শ্রেণি পরীক্ষা (CT)"),
    FIELD_CONT("ধারাবাহিক", "মোট ধারাবাহিক (CONT)"),
    FIELD_WR("সামষ্টিক", "সামষ্টিক / লিখিত (WR)"),
    FIELD_TOTAL("শিল্পকলা মোট", "শিল্পকলা / একক মোট (TOTAL)"),
    FIELD_GRAND("সর্বমোট", "চূড়ান্ত মোট (GRAND)"),
    EXTRA_SKIP("অতিরিক্ত (বাদ)", "মন্তব্য / গ্রেড / স্বাক্ষর (বাদ দেবে)", isSkipped = true, isRepeatable = true)
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

val ALL_CONFIGURABLE_COLUMN_ITEMS: List<ColumnRole> = listOf(
    ColumnRole.SERIAL_SKIP,
    ColumnRole.ROLL,
    ColumnRole.NAME_SKIP,
    ColumnRole.FIELD_TB,
    ColumnRole.FIELD_ACT,
    ColumnRole.FIELD_LANG,
    ColumnRole.FIELD_CT,
    ColumnRole.FIELD_CONT,
    ColumnRole.FIELD_WR,
    ColumnRole.FIELD_TOTAL,
    ColumnRole.FIELD_GRAND,
    ColumnRole.EXTRA_SKIP
)

data class OfflineColumnTemplate(
    val id: String,
    val titleBn: String,
    val subtitleBn: String,
    val columns: List<ColumnRole>,
    val isPopular: Boolean = false,
    val badgeBn: String? = null
)

val OFFLINE_COLUMN_SUGGESTIONS: List<OfflineColumnTemplate> = listOf(
    // অধিক ব্যবহৃত কম্বিনেশন
    OfflineColumnTemplate(
        id = "pop_roll_cont_wr_grand",
        titleBn = "রোল · ধারাবাহিক · সামষ্টিক · মোট",
        subtitleBn = "৪ কলাম · নাম ছাড়া সবচেয়ে দ্রুত নম্বর এন্ট্রি",
        columns = listOf(ColumnRole.ROLL, ColumnRole.FIELD_CONT, ColumnRole.FIELD_WR, ColumnRole.FIELD_GRAND),
        isPopular = true,
        badgeBn = "সর্বাধিক ব্যবহৃত"
    ),
    OfflineColumnTemplate(
        id = "pop_roll_name_cont_wr_grand",
        titleBn = "রোল · নাম · ধারাবাহিক · সামষ্টিক · মোট",
        subtitleBn = "৫ কলাম · খাতার নাম বাদ দিয়ে রোল-ম্যাচিং",
        columns = listOf(ColumnRole.ROLL, ColumnRole.NAME_SKIP, ColumnRole.FIELD_CONT, ColumnRole.FIELD_WR, ColumnRole.FIELD_GRAND),
        isPopular = true,
        badgeBn = "জনপ্রিয়"
    ),
    OfflineColumnTemplate(
        id = "pop_roll_cont_wr",
        titleBn = "রোল · ধারাবাহিক · সামষ্টিক",
        subtitleBn = "৩ কলাম · মোট নিজে হিসাব করে নেবে",
        columns = listOf(ColumnRole.ROLL, ColumnRole.FIELD_CONT, ColumnRole.FIELD_WR),
        isPopular = true,
        badgeBn = "সংক্ষিপ্ত"
    ),
    OfflineColumnTemplate(
        id = "pop_roll_name_full",
        titleBn = "রোল · নাম · ৪ অংশ · ধারাবাহিক · সামষ্টিক · মোট",
        subtitleBn = "৯ কলাম · পাঠ্যবই, সক্রিয়তা, ভাষা ও ক্লাস টেস্টসহ পূর্ণাঙ্গ শীট",
        columns = listOf(
            ColumnRole.ROLL, ColumnRole.NAME_SKIP,
            ColumnRole.FIELD_TB, ColumnRole.FIELD_ACT, ColumnRole.FIELD_LANG, ColumnRole.FIELD_CT,
            ColumnRole.FIELD_CONT, ColumnRole.FIELD_WR, ColumnRole.FIELD_GRAND
        ),
        isPopular = true,
        badgeBn = "পূর্ণাঙ্গ"
    ),
    // আরও সাজেশন প্রিসেট
    OfflineColumnTemplate(
        id = "pre_roll_name_cont_wr",
        titleBn = "রোল · নাম · ধারাবাহিক · সামষ্টিক",
        subtitleBn = "৪ কলাম · নামসহ, মোট কলাম ছাড়া",
        columns = listOf(ColumnRole.ROLL, ColumnRole.NAME_SKIP, ColumnRole.FIELD_CONT, ColumnRole.FIELD_WR)
    ),
    OfflineColumnTemplate(
        id = "pre_roll_full_no_name",
        titleBn = "রোল · ৪ অংশ · ধারাবাহিক · সামষ্টিক · মোট",
        subtitleBn = "৮ কলাম · নাম ছাড়া পূর্ণাঙ্গ শীট",
        columns = listOf(
            ColumnRole.ROLL,
            ColumnRole.FIELD_TB, ColumnRole.FIELD_ACT, ColumnRole.FIELD_LANG, ColumnRole.FIELD_CT,
            ColumnRole.FIELD_CONT, ColumnRole.FIELD_WR, ColumnRole.FIELD_GRAND
        )
    ),
    OfflineColumnTemplate(
        id = "pre_roll_name_4parts_cont",
        titleBn = "রোল · নাম · ৪ অংশ · ধারাবাহিক",
        subtitleBn = "৭ কলাম · শুধু ধারাবাহিক মূল্যায়ন",
        columns = listOf(
            ColumnRole.ROLL, ColumnRole.NAME_SKIP,
            ColumnRole.FIELD_TB, ColumnRole.FIELD_ACT, ColumnRole.FIELD_LANG, ColumnRole.FIELD_CT,
            ColumnRole.FIELD_CONT
        )
    ),
    OfflineColumnTemplate(
        id = "pre_sl_roll_name_cwg",
        titleBn = "ক্রমিক · রোল · নাম · ধারাবাহিক · সামষ্টিক · মোট",
        subtitleBn = "৬ কলাম · বামে ক্রমিক নং কলামসহ রেজিস্টার",
        columns = listOf(
            ColumnRole.SERIAL_SKIP, ColumnRole.ROLL, ColumnRole.NAME_SKIP,
            ColumnRole.FIELD_CONT, ColumnRole.FIELD_WR, ColumnRole.FIELD_GRAND
        )
    ),
    OfflineColumnTemplate(
        id = "pre_roll_wr_only",
        titleBn = "রোল · সামষ্টিক",
        subtitleBn = "২ কলাম · শুধু লিখিত/সামষ্টিক নম্বর",
        columns = listOf(ColumnRole.ROLL, ColumnRole.FIELD_WR)
    ),
    OfflineColumnTemplate(
        id = "pre_roll_name_wr_only",
        titleBn = "রোল · নাম · সামষ্টিক",
        subtitleBn = "৩ কলাম · নামসহ শুধু সামষ্টিক",
        columns = listOf(ColumnRole.ROLL, ColumnRole.NAME_SKIP, ColumnRole.FIELD_WR)
    ),
    OfflineColumnTemplate(
        id = "pre_roll_cont_only",
        titleBn = "রোল · ধারাবাহিক",
        subtitleBn = "২ কলাম · শুধু ধারাবাহিক মোট",
        columns = listOf(ColumnRole.ROLL, ColumnRole.FIELD_CONT)
    ),
    OfflineColumnTemplate(
        id = "pre_roll_name_art",
        titleBn = "রোল · নাম · শিল্পকলা মোট",
        subtitleBn = "৩ কলাম · চারু ও কারুকলা / একক মোট",
        columns = listOf(ColumnRole.ROLL, ColumnRole.NAME_SKIP, ColumnRole.FIELD_TOTAL)
    )
)

data class OfflineScanDiagnostics(
    val detectedScript: OfflineDigitScript,
    val rowCount: Int,
    val headerRowsSkipped: Int,
    val columnCount: Int,
    val skippedNameColumnIndex: Int?,
    val columnRoles: List<ColumnRole>,
    val processingMs: Long,
    val usedDeskewDegrees: Float,
    val usedCustomColumns: Boolean = false
)
