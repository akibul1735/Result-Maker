package com.kgps.result

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.roundToLong

enum class FailIndicatorStyle(
    val id: String,
    val symbol: String,
    val labelBn: String,
    val sampleBn: String
) {
    ASTERISK("ASTERISK", "*", "তারকা চিহ্ন (*)", "৩৪*"),
    CROSS("CROSS", "×", "ক্রস চিহ্ন (×)", "৩৪×"),
    LETTER_F("LETTER_F", "F", "F চিহ্ন (F)", "৩৪ F"),
    EXCLAMATION("EXCLAMATION", "!", "সতর্ক চিহ্ন (!)", "৩৪!"),
    FAIL_BN("FAIL_BN", "ফেল", "বাংলা লেখা (ফেল)", "৩৪ ফেল"),
    RED_ONLY("RED_ONLY", "", "শুধু লাল রঙ", "৩৪ (লাল)");

    companion object {
        fun fromId(raw: String?): FailIndicatorStyle =
            entries.firstOrNull { it.id == raw } ?: ASTERISK
    }
}

data class OfficialGuidelineItem(
    val id: String,
    val sectionLabelBn: String,
    val shortTitleBn: String,
    val summaryBn: String,
    val fullDetailsBn: String,
    val linkedRuleHintBn: String
)

val OFFICIAL_EVAL_GUIDELINES: List<OfficialGuidelineItem> = listOf(
    OfficialGuidelineItem(
        id = "GOVT_2_1_PERCENT",
        sectionLabelBn = "ধারা ২.১",
        shortTitleBn = "শ্রেণিভিত্তিক মূল্যায়নের শতকরা হার",
        summaryBn = "১ম–২য় শ্রেণি: ৫০% ধারাবাহিক + ৫০% সামষ্টিক; ৩য়–৫ম: ৩০% ধারাবাহিক + ৭০% সামষ্টিক",
        fullDetailsBn = "• প্রথম ও দ্বিতীয় শ্রেণি (পাঠ্যপুস্তকযুক্ত বিষয়): ধারাবাহিক মূল্যায়ন ৫০% এবং সামষ্টিক মূল্যায়ন ৫০%।\n" +
            "• তৃতীয়, চতুর্থ ও পঞ্চম শ্রেণি (পাঠ্যপুস্তকযুক্ত বিষয়): ধারাবাহিক মূল্যায়ন ৩০% এবং সামষ্টিক মূল্যায়ন ৭০%।\n" +
            "• পাঠ্যপুস্তকবিহীন বিষয় (১ম–৫ম): শুধুমাত্র শিক্ষক সহায়িকার ভিত্তিতে ১০০% ধারাবাহিক মূল্যায়ন (প্রতি প্রান্তিকে ৫০ নম্বর)।",
        linkedRuleHintBn = "অ্যাপের ডিফল্ট বিষয়-কাঠামো এই হার অনুসারেই সেট করা আছে; চাইলে এই নীতি প্রযোজ্য নয় চিহ্নিত করতে পারেন।"
    ),
    OfficialGuidelineItem(
        id = "GOVT_2_2_MARKS",
        sectionLabelBn = "ধারা ২.২",
        shortTitleBn = "বিষয়ভিত্তিক নম্বর বণ্টন",
        summaryBn = "১ম–২য় শ্রেণি সর্বমোট ৫০০ নম্বর; ৩য়–৫ম শ্রেণি সর্বমোট ৭০০ নম্বর (প্রতি প্রান্তিক)",
        fullDetailsBn = "• প্রথম ও দ্বিতীয় শ্রেণি (সর্বমোট ৫০০): বাংলা ১০০ (৫০+৫০), ইংরেজি ১০০ (৫০+৫০), গণিত ১০০ (৫০+৫০), সামাজিক বিজ্ঞান ও বিজ্ঞান (সমন্বিত) ৫০, ধর্মশিক্ষা ৫০, শিল্পকলা ৫০, শারীরিক ও মানসিক স্বাস্থ্য শিক্ষা ৫০।\n" +
            "• তৃতীয়, চতুর্থ ও পঞ্চম শ্রেণি (সর্বমোট ৭০০): বাংলা ১০০ (৩০+৭০), ইংরেজি ১০০ (৩০+৭০), গণিত ১০০ (৩০+৭০), বাংলাদেশ ও বিশ্বপরিচয় ১০০ (৩০+৭০), বিজ্ঞান ১০০ (৩০+৭০), ধর্মশিক্ষা ১০০ (৩০+৭০), শিল্পকলা ৫০, শারীরিক ও মানসিক স্বাস্থ্য শিক্ষা ৫০।",
        linkedRuleHintBn = "প্রতিটি শ্রেণির পূর্ণমান ও নম্বর বণ্টন এই সরকারি ছক অনুসরণ করে।"
    ),
    OfficialGuidelineItem(
        id = "GOVT_2_3_CONTINUOUS",
        sectionLabelBn = "ধারা ২.৩",
        shortTitleBn = "ধারাবাহিক মূল্যায়নের ৪টি ক্ষেত্র",
        summaryBn = "পাঠ্যপুস্তক/সহায়িকার কাজ, শ্রেণিকাজে সক্রিয়তা, ভাষা/বোধগম্যতা ও ক্লাস টেস্ট",
        fullDetailsBn = "প্রতি প্রান্তিকে ধারাবাহিক মূল্যায়নের ৪টি ক্ষেত্র:\n" +
            "• ১.১ পাঠ্যপুস্তক/শিক্ষক সহায়িকার কাজ সম্পন্ন করা (১ম–২য়: ২০, ৩য়–৫ম মূল: ১০)\n" +
            "• ১.২ শ্রেণিকাজে সক্রিয়তা (৫ নম্বর)\n" +
            "• ১.৩ ভাষা দক্ষতা/বিষয়বস্তুর বোধগম্যতা (৫ নম্বর)\n" +
            "• ১.৪ ক্লাস টেস্ট / শ্রেণি অভীক্ষা (১ম–২য়: ২০, ৩য়–৫ম মূল: ১০, ৫০ নম্বরের বিষয়: ২০)",
        linkedRuleHintBn = "নম্বর এডিটর ও বাল্ক শিটে এই ৪টি ক্ষেত্র স্বয়ংক্রিয়ভাবে যোগ হয়ে ধারাবাহিক নম্বর হিসাব হয়।"
    ),
    OfficialGuidelineItem(
        id = "GOVT_3_1_ATTENDANCE",
        sectionLabelBn = "ধারা ৩ (উপস্থিতি)",
        shortTitleBn = "ন্যূনতম ৮৫% শ্রেণি উপস্থিতি",
        summaryBn = "পরবর্তী শ্রেণিতে উত্তীর্ণ হতে প্রতি প্রান্তিকে মোট কার্যদিবসের ন্যূনতম ৮৫% উপস্থিতি",
        fullDetailsBn = "সরকারি নির্দেশনা ধারা ৩ অনুযায়ী পরবর্তী শ্রেণিতে উত্তীর্ণ হতে হলে শিক্ষার্থীকে প্রতি প্রান্তিকে মোট কার্যদিবসের ন্যূনতম ৮৫% শ্রেণিতে উপস্থিত থাকতে হবে। বিদ্যালয়ের প্রয়োজনে নিচে এই হার পরিবর্তন বা শর্তটি বন্ধ রাখা যাবে।",
        linkedRuleHintBn = "শর্ত ১ ও ৪-এর সাথে যুক্ত (ডিফল্ট অফ; প্রয়োজনে অন ও হার পরিবর্তনযোগ্য)।"
    ),
    OfficialGuidelineItem(
        id = "GOVT_3_2_PASS",
        sectionLabelBn = "ধারা ৩ (পাশ নম্বর)",
        shortTitleBn = "বিষয়ভিত্তিক ৪০% ও বিশেষ শিক্ষার্থীর ৩৩% পাশ",
        summaryBn = "সাধারণ শিক্ষার্থী ৪০% ও বিশেষ চাহিদাসম্পন্ন শিক্ষার্থী ৩৩% পেলে পাশ",
        fullDetailsBn = "• সাধারণ শিক্ষার্থীর বিষয়ভিত্তিক পাশ নম্বর ৪০%।\n" +
            "• বিশেষ চাহিদাসম্পন্ন শিক্ষার্থীর ক্ষেত্রে পাশ নম্বর ৩৩%।\n" +
            "• গড় পাস: পরবর্তী শ্রেণিতে উত্তীর্ণ হতে তিন প্রান্তিকে মোট নম্বরের গড় ৪০% (বা বিশেষ ক্ষেত্রে ৩৩%) নম্বর পেতে হবে।",
        linkedRuleHintBn = "শর্ত ২, ৩, ৪ ও ৫-এর মাধ্যমে নিয়ন্ত্রিত।"
    ),
    OfficialGuidelineItem(
        id = "GOVT_3_4_ABSENT",
        sectionLabelBn = "ধারা ৩ (অনুপস্থিতি ও বিকল্প পরীক্ষা)",
        shortTitleBn = "সামষ্টিক পরীক্ষায় উপস্থিতি ও অনুপস্থিত 'A'",
        summaryBn = "অনিবার্য কারণে অনুপস্থিত থাকলে ১০ কর্মদিবসের মধ্যে বিকল্প পরীক্ষার বিধান",
        fullDetailsBn = "প্রতি প্রান্তিকের সামষ্টিক পরীক্ষায় শিক্ষার্থীকে উপস্থিত থাকতে হবে। অসুস্থতাজনিত বা অনিবার্য কারণে কোনো পরীক্ষায় অংশগ্রহণ করতে না পারলে শিক্ষার্থীর আবেদনের পরিপ্রেক্ষিতে বিদ্যালয় কর্তৃপক্ষ পরবর্তী ১০ কর্মদিবসের মধ্যে নতুন প্রশ্নপত্রে বিকল্প পরীক্ষা গ্রহণ করবেন। বিকল্প পরীক্ষার আগে পর্যন্ত অনুপস্থিত ঘরে 'A' ব্যবহার করা যাবে।",
        linkedRuleHintBn = "শর্ত ৭ অনুযায়ী অনুপস্থিত 'A' এবং প্রাপ্ত '0' আলাদাভাবে সংরক্ষিত হয়; শর্ত ৭ থেকে অনুপস্থিতিকে অকৃতকার্য ধরা অন/অফ করা যাবে।"
    ),
    OfficialGuidelineItem(
        id = "GOVT_4_2_PROGRESS",
        sectionLabelBn = "ধারা ৪.২",
        shortTitleBn = "গ্রেডিং, প্রান্তিক গড় ও গুণাবলি",
        summaryBn = "A (৮০–১০০%), B (৬০–৭৯%), C (৪০–৫৯%), D (০–৩৯%) ও গুণাবলি 'ক'/'খ'/'গ'",
        fullDetailsBn = "• প্রতি প্রান্তিকে প্রতি বিষয়ে ধারাবাহিক ও সামষ্টিক নম্বরের সমষ্টি করে মোট নম্বর হিসাব হবে।\n" +
            "• ৩য় প্রান্তিক শেষে ৩ প্রান্তিকের গড় নম্বর (এবং চলমান ১ বা ২ প্রান্তিক থাকলে প্রাপ্ত প্রান্তিক অনুযায়ী গড়) ও মোট নম্বরের শতকরা হার অনুসারে সমন্বিত গ্রেড প্রদান করতে হবে:\n" +
            "  A: ৮০%-১০০% (অতি উত্তম), B: ৬০%-৭৯% (উত্তম), C: ৪০%-৫৯% (সন্তোষজনক), D: ০%-৩৯% (সহায়তা প্রয়োজন)।\n" +
            "• ব্যক্তিগত ও সামাজিক গুণাবলি ('সক্রিয়তা', 'শৃঙ্খলা' ও 'আচরণ'): ক / খ / গ।",
        linkedRuleHintBn = "শর্ত ৬ ও ৮-এ প্রান্তিক গড় ও গুণাবলি-ভিত্তিক টাই-ব্রেকার সংযুক্ত।"
    )
)

data class ResultEvaluationRules(
    // Rule 1: ন্যূনতম উপস্থিতির শর্ত (Default OFF)
    val attendanceRuleEnabled: Boolean = false,
    val minAttendancePct: Double = 85.0,

    // Rule 2: বিষয়ভিত্তিক পাশ নম্বর ও ফেইল নির্দেশক
    val subjectPassRuleEnabled: Boolean = true,
    val regularPassPct: Double = 40.0,
    val showFailIndicator: Boolean = true,
    val failIndicatorStyle: FailIndicatorStyle = FailIndicatorStyle.ASTERISK,

    // Rule 3: বিশেষ চাহিদাসম্পন্ন শিক্ষার্থীর ৩৩%
    val specialNeedsRuleEnabled: Boolean = true,
    val specialNeedsPassPct: Double = 33.0,

    // Rule 4: পরবর্তী শ্রেণিতে উত্তীর্ণ হওয়ার শর্ত (Default গড়ে ৪০% পাস ON)
    val promotionCheckAvgPass: Boolean = true,
    val promotionCheckAttendance: Boolean = false,
    val promotionCheckAllSubjects: Boolean = false,

    // Rule 5: অনুত্তীর্ণ শিক্ষার্থীর মেধাক্রম শ্রেণিবিন্যাস
    val meritTierAllSubPassFirst: Boolean = true,
    val meritTierAvgPassEnabled: Boolean = true,

    // Rule 6: প্রান্তিকভিত্তিক গড় (১/২/৩ প্রান্তিক অনুযায়ী গড়)
    val dynamicTermAveraging: Boolean = true,

    // Rule 7: অনুপস্থিত 'A' থাকলে সেই বিষয় অকৃতকার্য গণ্য হবে কি না
    val absentCountsAsFail: Boolean = true,

    // Rule 8: মেধাক্রম যৌথ ও টাই-ব্রেকার
    val jointMeritEnabled: Boolean = false,
    val tieBreakAttendance: Boolean = false,
    val tieBreakTraits: Boolean = false,
    val tieBreakPrevRoll: Boolean = true,

    // কোন সরকারি নির্দেশনা কার্ডে "এই শর্ত এপ্লাই হবে না" টিক দেওয়া আছে
    val ignoredGuidelineIds: Set<String> = emptySet()
) {
    fun isGuidelineIgnored(id: String): Boolean = id in ignoredGuidelineIds

    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "attendanceRuleEnabled" to attendanceRuleEnabled,
        "minAttendancePct" to minAttendancePct.coerceIn(1.0, 100.0),
        "subjectPassRuleEnabled" to subjectPassRuleEnabled,
        "regularPassPct" to regularPassPct.coerceIn(1.0, 100.0),
        "showFailIndicator" to showFailIndicator,
        "failIndicatorStyle" to failIndicatorStyle.id,
        "specialNeedsRuleEnabled" to specialNeedsRuleEnabled,
        "specialNeedsPassPct" to specialNeedsPassPct.coerceIn(1.0, 100.0),
        "promotionCheckAvgPass" to promotionCheckAvgPass,
        "promotionCheckAttendance" to promotionCheckAttendance,
        "promotionCheckAllSubjects" to promotionCheckAllSubjects,
        "meritTierAllSubPassFirst" to meritTierAllSubPassFirst,
        "meritTierAvgPassEnabled" to meritTierAvgPassEnabled,
        "dynamicTermAveraging" to dynamicTermAveraging,
        "absentCountsAsFail" to absentCountsAsFail,
        "jointMeritEnabled" to jointMeritEnabled,
        "tieBreakAttendance" to tieBreakAttendance,
        "tieBreakTraits" to tieBreakTraits,
        "tieBreakPrevRoll" to tieBreakPrevRoll,
        "ignoredGuidelineIds" to ignoredGuidelineIds.sorted()
    )

    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("attendanceRuleEnabled", attendanceRuleEnabled)
        put("minAttendancePct", minAttendancePct)
        put("subjectPassRuleEnabled", subjectPassRuleEnabled)
        put("regularPassPct", regularPassPct)
        put("showFailIndicator", showFailIndicator)
        put("failIndicatorStyle", failIndicatorStyle.id)
        put("specialNeedsRuleEnabled", specialNeedsRuleEnabled)
        put("specialNeedsPassPct", specialNeedsPassPct)
        put("promotionCheckAvgPass", promotionCheckAvgPass)
        put("promotionCheckAttendance", promotionCheckAttendance)
        put("promotionCheckAllSubjects", promotionCheckAllSubjects)
        put("meritTierAllSubPassFirst", meritTierAllSubPassFirst)
        put("meritTierAvgPassEnabled", meritTierAvgPassEnabled)
        put("dynamicTermAveraging", dynamicTermAveraging)
        put("absentCountsAsFail", absentCountsAsFail)
        put("jointMeritEnabled", jointMeritEnabled)
        put("tieBreakAttendance", tieBreakAttendance)
        put("tieBreakTraits", tieBreakTraits)
        put("tieBreakPrevRoll", tieBreakPrevRoll)
        put("ignoredGuidelineIds", JSONArray(ignoredGuidelineIds.sorted()))
    }

    companion object {
        fun fromMap(map: Map<*, *>?): ResultEvaluationRules {
            if (map == null) return ResultEvaluationRules()
            fun bool(key: String, def: Boolean): Boolean = (map[key] as? Boolean) ?: def
            fun dbl(key: String, def: Double): Double =
                (map[key] as? Number)?.toDouble()?.takeIf { it.isFinite() }?.coerceIn(1.0, 100.0) ?: def

            val ignored = (map["ignoredGuidelineIds"] as? List<*>)
                ?.mapNotNull { (it as? String)?.trim()?.takeIf { s -> s.isNotEmpty() } }
                ?.toSet()
                ?: emptySet()

            return ResultEvaluationRules(
                attendanceRuleEnabled = bool("attendanceRuleEnabled", false),
                minAttendancePct = dbl("minAttendancePct", 85.0),
                subjectPassRuleEnabled = bool("subjectPassRuleEnabled", true),
                regularPassPct = dbl("regularPassPct", 40.0),
                showFailIndicator = bool("showFailIndicator", true),
                failIndicatorStyle = FailIndicatorStyle.fromId(map["failIndicatorStyle"] as? String),
                specialNeedsRuleEnabled = bool("specialNeedsRuleEnabled", true),
                specialNeedsPassPct = dbl("specialNeedsPassPct", 33.0),
                promotionCheckAvgPass = bool("promotionCheckAvgPass", true),
                promotionCheckAttendance = bool("promotionCheckAttendance", false),
                promotionCheckAllSubjects = bool("promotionCheckAllSubjects", false),
                meritTierAllSubPassFirst = bool("meritTierAllSubPassFirst", true),
                meritTierAvgPassEnabled = bool("meritTierAvgPassEnabled", true),
                dynamicTermAveraging = bool("dynamicTermAveraging", true),
                absentCountsAsFail = bool("absentCountsAsFail", true),
                jointMeritEnabled = bool("jointMeritEnabled", false),
                tieBreakAttendance = bool("tieBreakAttendance", false),
                tieBreakTraits = bool("tieBreakTraits", false),
                tieBreakPrevRoll = bool("tieBreakPrevRoll", true),
                ignoredGuidelineIds = ignored
            )
        }

        fun fromJsonObject(obj: JSONObject?): ResultEvaluationRules {
            if (obj == null) return ResultEvaluationRules()
            val arr = obj.optJSONArray("ignoredGuidelineIds")
            val ignored = buildSet {
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        arr.optString(i).trim().takeIf { it.isNotEmpty() }?.let(::add)
                    }
                }
            }
            return ResultEvaluationRules(
                attendanceRuleEnabled = obj.optBoolean("attendanceRuleEnabled", false),
                minAttendancePct = obj.optDouble("minAttendancePct", 85.0).takeIf { it.isFinite() }?.coerceIn(1.0, 100.0) ?: 85.0,
                subjectPassRuleEnabled = obj.optBoolean("subjectPassRuleEnabled", true),
                regularPassPct = obj.optDouble("regularPassPct", 40.0).takeIf { it.isFinite() }?.coerceIn(1.0, 100.0) ?: 40.0,
                showFailIndicator = obj.optBoolean("showFailIndicator", true),
                failIndicatorStyle = FailIndicatorStyle.fromId(obj.optString("failIndicatorStyle", FailIndicatorStyle.ASTERISK.id)),
                specialNeedsRuleEnabled = obj.optBoolean("specialNeedsRuleEnabled", true),
                specialNeedsPassPct = obj.optDouble("specialNeedsPassPct", 33.0).takeIf { it.isFinite() }?.coerceIn(1.0, 100.0) ?: 33.0,
                promotionCheckAvgPass = obj.optBoolean("promotionCheckAvgPass", true),
                promotionCheckAttendance = obj.optBoolean("promotionCheckAttendance", false),
                promotionCheckAllSubjects = obj.optBoolean("promotionCheckAllSubjects", false),
                meritTierAllSubPassFirst = obj.optBoolean("meritTierAllSubPassFirst", true),
                meritTierAvgPassEnabled = obj.optBoolean("meritTierAvgPassEnabled", true),
                dynamicTermAveraging = obj.optBoolean("dynamicTermAveraging", true),
                absentCountsAsFail = obj.optBoolean("absentCountsAsFail", true),
                jointMeritEnabled = obj.optBoolean("jointMeritEnabled", false),
                tieBreakAttendance = obj.optBoolean("tieBreakAttendance", false),
                tieBreakTraits = obj.optBoolean("tieBreakTraits", false),
                tieBreakPrevRoll = obj.optBoolean("tieBreakPrevRoll", true),
                ignoredGuidelineIds = ignored
            )
        }
    }
}

// ---------- বিশেষ চাহিদা ও অনুপস্থিত (A) হেল্পার ----------

const val X_KEY_SPECIAL_NEEDS = "SPECIAL_NEEDS"

fun absKey(subKey: String, term: Int, comp: String): String =
    "ABS_${subKey}_${term}_$comp"

fun isAbsentXKey(xKey: String): Boolean {
    if (!xKey.startsWith("ABS_")) return false
    val parts = xKey.split('_')
    return parts.size == 4 &&
        parts[2].toIntOrNull() in 1..3 &&
        (parts[3] == "WR" || parts[3] == "CONT")
}

fun absentSubjectFromXKey(xKey: String): String? =
    if (isAbsentXKey(xKey)) xKey.split('_')[1] else null

fun isAbsentToken(raw: String): Boolean {
    val clean = raw.trim().lowercase()
    return clean in setOf("a", "ab", "abs", "absent", "অনু", "অনুপস্থিত")
}

val Student.isSpecialNeeds: Boolean
    get() {
        val raw = x[X_KEY_SPECIAL_NEEDS]?.trim()?.lowercase().orEmpty()
        return raw == "1" || raw == "true" || raw == "yes" || raw == "হ্যাঁ"
    }

fun Student.withSpecialNeeds(enabled: Boolean): Student {
    val nextX = x.toMutableMap()
    if (enabled) nextX[X_KEY_SPECIAL_NEEDS] = "1" else nextX.remove(X_KEY_SPECIAL_NEEDS)
    return copy(x = nextX)
}

fun Student.isCompAbsent(subKey: String, term: Int, comp: String): Boolean {
    val key = absKey(subKey, term, comp)
    val raw = x[key]?.trim().orEmpty()
    return raw == "1" || raw.equals("true", ignoreCase = true) || raw.equals("A", ignoreCase = true)
}

fun Student.isSubTermAbsent(sub: Sub, term: Int): Boolean {
    val wrAbs = sub.written > 0 && isCompAbsent(sub.key, term, "WR")
    val contAbs = isCompAbsent(sub.key, term, "CONT")
    return wrAbs || contAbs
}

fun Student.isSubFullyAbsent(sub: Sub, term: Int): Boolean {
    val contAbs = isCompAbsent(sub.key, term, "CONT")
    return if (sub.written > 0) {
        val wrAbs = isCompAbsent(sub.key, term, "WR")
        val hasAnyNumericMark = m.keys.any { k ->
            k.startsWith("${sub.key}_${term}_")
        }
        (wrAbs && contAbs) || ((wrAbs || contAbs) && !hasAnyNumericMark)
    } else {
        contAbs
    }
}

fun Student.hasSubAbsent(sub: Sub, term: Int): Boolean =
    isCompAbsent(sub.key, term, "WR") || isCompAbsent(sub.key, term, "CONT")

fun Student.hasTermAbsent(term: Int): Boolean =
    x.entries.any { (k, v) ->
        v.isNotBlank() && isAbsentXKey(k) && k.split('_').getOrNull(2)?.toIntOrNull() == term
    }

fun Student.withCompAbsent(sub: Sub, term: Int, comp: String, absent: Boolean): Student {
    val nextX = x.toMutableMap()
    val nextM = m.toMutableMap()
    val key = absKey(sub.key, term, comp)
    if (absent) {
        nextX[key] = "1"
        if (comp == "WR") {
            nextM.remove(mk(sub.key, term, "WR"))
        } else if (comp == "CONT") {
            listOf("TB", "ACT", "LANG", "CT", "CONT", "TOTAL").forEach { c ->
                nextM.remove(mk(sub.key, term, c))
            }
        }
    } else {
        nextX.remove(key)
    }
    return copy(m = nextM, x = nextX)
}

// ---------- মূল্যায়ন ও মেধাক্রম ইঞ্জিন ----------

data class StudentEvalSummary(
    val hasAnyData: Boolean,
    val activeTerms: List<Int>,
    val obtainedAvgTotal: Double,
    val maxTotal: Double,
    val pct: Double,
    val grade: String,
    val passPctUsed: Double,
    val allSubjectsPassed: Boolean,
    val failedSubjectKeys: Set<String>,
    val absentSubjectKeys: Set<String>,
    val attendancePct: Double?,
    val attendancePassed: Boolean,
    val traitScore: Double,
    val traitCount: Int,
    val isPromoted: Boolean,
    val promotionFailureReasons: List<String>,
    val meritTier: Int
)

data class RankedStudentResult(
    val student: Student,
    val eval: StudentEvalSummary,
    val rank: Int,
    val isJointRank: Boolean
)

object ResultRulesEngine {

    fun activeTermsForClass(
        classStudents: List<Student>,
        dynamicTermAveraging: Boolean = true
    ): List<Int> {
        if (!dynamicTermAveraging) return listOf(1, 2, 3)
        val active = (1..3).filter { t ->
            classStudents.any { it.hasTerm(t) }
        }
        return active
    }

    fun activeTermsForStudent(
        student: Student,
        dynamicTermAveraging: Boolean = true
    ): List<Int> {
        if (!dynamicTermAveraging) return listOf(1, 2, 3)
        return (1..3).filter { student.hasTerm(it) }
    }

    fun effectivePassPct(student: Student, rules: ResultEvaluationRules): Double =
        if (rules.specialNeedsRuleEnabled && student.isSpecialNeeds) {
            rules.specialNeedsPassPct
        } else {
            rules.regularPassPct
        }

    fun subjectAvg(student: Student, sub: Sub, activeTerms: List<Int>): Double {
        if (activeTerms.isEmpty()) return 0.0
        return activeTerms.sumOf { student.total(sub, it) } / activeTerms.size.toDouble()
    }

    fun isSubjectTermFailed(
        student: Student,
        sub: Sub,
        term: Int,
        rules: ResultEvaluationRules
    ): Boolean {
        if (!rules.subjectPassRuleEnabled) return false
        if (!student.hasSub(sub, term)) return false
        if (rules.absentCountsAsFail && student.isSubTermAbsent(sub, term)) return true
        val passPct = effectivePassPct(student, rules)
        val threshold = sub.max * (passPct / 100.0)
        return student.total(sub, term) + 1e-6 < threshold
    }

    fun isSubjectAnnualFailed(
        student: Student,
        sub: Sub,
        activeTerms: List<Int>,
        rules: ResultEvaluationRules
    ): Boolean {
        if (!rules.subjectPassRuleEnabled || activeTerms.isEmpty()) return false
        val enteredTerms = activeTerms.filter { student.hasSub(sub, it) }
        if (enteredTerms.isEmpty()) return false
        if (rules.absentCountsAsFail && enteredTerms.any { student.isSubTermAbsent(sub, it) }) return true
        val avgVal = subjectAvg(student, sub, activeTerms)
        val passPct = effectivePassPct(student, rules)
        val threshold = sub.max * (passPct / 100.0)
        return avgVal + 1e-6 < threshold
    }

    fun formatTermTotalCell(
        rawValueText: String,
        failed: Boolean,
        rules: ResultEvaluationRules
    ): String {
        if (!failed || !rules.showFailIndicator) return rawValueText
        val sym = rules.failIndicatorStyle.symbol
        if (sym.isEmpty()) return rawValueText
        return if (rules.failIndicatorStyle == FailIndicatorStyle.LETTER_F ||
            rules.failIndicatorStyle == FailIndicatorStyle.FAIL_BN
        ) {
            "$rawValueText $sym"
        } else {
            "$rawValueText$sym"
        }
    }

    fun attendancePctFor(student: Student, terms: List<Int>): Double? {
        val checkTerms = if (terms.isEmpty()) listOf(1, 2, 3) else terms
        var totalDays = 0.0
        var totalPresent = 0.0
        checkTerms.forEach { t ->
            val days = fromBn(student.x["DAYS_$t"].orEmpty()).toDoubleOrNull()?.takeIf { it > 0.0 }
            val pres = fromBn(student.x["PRES_$t"].orEmpty()).toDoubleOrNull()?.coerceAtLeast(0.0)
            if (days != null && pres != null) {
                totalDays += days
                totalPresent += pres.coerceAtMost(days)
            }
        }
        if (totalDays <= 0.0) return null
        return (totalPresent / totalDays) * 100.0
    }

    fun traitScoreFor(student: Student, terms: List<Int>): Pair<Double, Int> {
        val checkTerms = if (terms.isEmpty()) listOf(1, 2, 3) else terms
        var totalPts = 0.0
        var count = 0
        checkTerms.forEach { t ->
            listOf("ACT_$t", "DISC_$t", "COND_$t").forEach { key ->
                val v = student.x[key]?.trim()
                val pts = when (v) {
                    "ক" -> 3.0
                    "খ" -> 2.0
                    "গ" -> 1.0
                    else -> null
                }
                if (pts != null) {
                    totalPts += pts
                    count++
                }
            }
        }
        val avgScore = if (count > 0) totalPts / count.toDouble() else 0.0
        return avgScore to count
    }

    fun evaluateStudent(
        student: Student,
        rules: ResultEvaluationRules,
        classActiveTerms: List<Int>,
        scopedTerm: Int = 0
    ): StudentEvalSummary {
        val terms = when {
            scopedTerm in 1..3 -> listOf(scopedTerm)
            classActiveTerms.isNotEmpty() -> classActiveTerms
            else -> activeTermsForStudent(student, rules.dynamicTermAveraging)
        }
        val subs = student.subs()
        val maxTotal = subs.sumOf { it.max }.toDouble()
        val hasAnyData = terms.isNotEmpty() && terms.any { student.hasTerm(it) }

        val obtainedAvgTotal = if (!hasAnyData || terms.isEmpty()) {
            0.0
        } else {
            subs.sumOf { sub -> subjectAvg(student, sub, terms) }
        }
        val pct = if (maxTotal > 0.0 && hasAnyData) (obtainedAvgTotal / maxTotal) * 100.0 else 0.0
        val grade = gradeOf(pct)
        val passPct = effectivePassPct(student, rules)

        val failedSubKeys = linkedSetOf<String>()
        val absentSubKeys = linkedSetOf<String>()
        if (hasAnyData) {
            subs.forEach { sub ->
                val enteredTerms = terms.filter { student.hasSub(sub, it) }
                if (enteredTerms.any { student.isSubTermAbsent(sub, it) }) {
                    absentSubKeys.add(sub.key)
                }
                val failed = if (scopedTerm in 1..3) {
                    isSubjectTermFailed(student, sub, scopedTerm, rules)
                } else {
                    isSubjectAnnualFailed(student, sub, terms, rules)
                }
                if (failed) {
                    failedSubKeys.add(sub.key)
                }
            }
        }
        val allSubjectsPassed = hasAnyData &&
            failedSubKeys.isEmpty() &&
            (!rules.subjectPassRuleEnabled || !rules.absentCountsAsFail || absentSubKeys.isEmpty())

        val attPct = attendancePctFor(student, terms)
        val checkAttendanceNow = rules.attendanceRuleEnabled && rules.promotionCheckAttendance
        val attendancePassed = if (!checkAttendanceNow) {
            true
        } else {
            attPct != null && attPct + 1e-6 >= rules.minAttendancePct
        }

        val avgPassed = pct + 1e-6 >= passPct

        val failureReasons = buildList {
            if (!hasAnyData) {
                add("মূল্যায়ন নম্বর নেই")
            } else {
                if (rules.promotionCheckAvgPass && !avgPassed) {
                    add("গড় নম্বর ${fmt(passPct)}%-এর নিচে (${fmt(pct)}%)")
                }
                if (rules.promotionCheckAllSubjects && !allSubjectsPassed) {
                    add("${bn(failedSubKeys.size.toString())}টি বিষয়ে অকৃতকার্য")
                }
                if (checkAttendanceNow) {
                    if (attPct == null) {
                        add("উপস্থিতির তথ্য নেই (ন্যূনতম ${fmt(rules.minAttendancePct)}%)")
                    } else if (attPct + 1e-6 < rules.minAttendancePct) {
                        add("উপস্থিতি ${fmt(rules.minAttendancePct)}%-এর নিচে (${fmt(attPct)}%)")
                    }
                }
            }
        }

        val isPromoted = hasAnyData &&
            (!rules.promotionCheckAvgPass || avgPassed) &&
            (!rules.promotionCheckAllSubjects || allSubjectsPassed) &&
            attendancePassed

        val meritTier = if (!hasAnyData) {
            99
        } else {
            when {
                rules.meritTierAllSubPassFirst && rules.meritTierAvgPassEnabled -> when {
                    allSubjectsPassed && avgPassed -> 0
                    avgPassed -> 1
                    else -> 2
                }
                rules.meritTierAllSubPassFirst && !rules.meritTierAvgPassEnabled -> when {
                    allSubjectsPassed -> 0
                    else -> 1
                }
                !rules.meritTierAllSubPassFirst && rules.meritTierAvgPassEnabled -> when {
                    avgPassed -> 0
                    else -> 1
                }
                else -> 0
            }
        }

        val (traitScore, traitCount) = traitScoreFor(student, terms)

        return StudentEvalSummary(
            hasAnyData = hasAnyData,
            activeTerms = terms,
            obtainedAvgTotal = obtainedAvgTotal,
            maxTotal = maxTotal,
            pct = pct,
            grade = grade,
            passPctUsed = passPct,
            allSubjectsPassed = allSubjectsPassed,
            failedSubjectKeys = failedSubKeys,
            absentSubjectKeys = absentSubKeys,
            attendancePct = attPct,
            attendancePassed = attendancePassed,
            traitScore = traitScore,
            traitCount = traitCount,
            isPromoted = isPromoted,
            promotionFailureReasons = failureReasons,
            meritTier = meritTier
        )
    }

    fun rankClassStudents(
        classStudents: List<Student>,
        rules: ResultEvaluationRules,
        scopedTerm: Int = 0
    ): List<RankedStudentResult> {
        if (classStudents.isEmpty()) return emptyList()
        val classActiveTerms = if (scopedTerm in 1..3) {
            listOf(scopedTerm)
        } else {
            activeTermsForClass(classStudents, rules.dynamicTermAveraging)
        }

        val evaluated = classStudents.map { st ->
            st to evaluateStudent(st, rules, classActiveTerms, scopedTerm)
        }

        fun scoreKey(eval: StudentEvalSummary): Long =
            (eval.obtainedAvgTotal * 100.0).roundToLong()

        fun pctKey(eval: StudentEvalSummary): Long =
            (eval.pct * 100.0).roundToLong()

        fun attKey(eval: StudentEvalSummary): Long =
            ((eval.attendancePct ?: -1.0) * 100.0).roundToLong()

        fun traitKey(eval: StudentEvalSummary): Long =
            (eval.traitScore * 100.0).roundToLong()

        val sorted = evaluated.sortedWith { (sA, eA), (sB, eB) ->
            // ১. মেধাক্রম শ্রেণি/স্তর (Tier)
            val tCmp = eA.meritTier.compareTo(eB.meritTier)
            if (tCmp != 0) return@sortedWith tCmp

            // ২. শতাংশ এবং প্রাপ্ত মোট নম্বর (বেশি নম্বর আগে)
            val pCmp = pctKey(eB).compareTo(pctKey(eA))
            if (pCmp != 0) return@sortedWith pCmp
            val mCmp = scoreKey(eB).compareTo(scoreKey(eA))
            if (mCmp != 0) return@sortedWith mCmp

            // ৩. টাই-ব্রেকারসমূহ
            if (rules.tieBreakAttendance) {
                val aCmp = attKey(eB).compareTo(attKey(eA))
                if (aCmp != 0) return@sortedWith aCmp
            }
            if (rules.tieBreakTraits) {
                val trCmp = traitKey(eB).compareTo(traitKey(eA))
                if (trCmp != 0) return@sortedWith trCmp
            }
            if (rules.tieBreakPrevRoll) {
                val rCmp = sA.roll.compareTo(sB.roll)
                if (rCmp != 0) return@sortedWith rCmp
            }
            // স্থিতিশীল ফলব্যাক অর্ডার
            val rollFallback = sA.roll.compareTo(sB.roll)
            if (rollFallback != 0) rollFallback else sA.id.compareTo(sB.id)
        }

        fun isTied(a: Pair<Student, StudentEvalSummary>, b: Pair<Student, StudentEvalSummary>): Boolean {
            val (sA, eA) = a
            val (sB, eB) = b
            if (eA.meritTier != eB.meritTier) return false
            if (pctKey(eA) != pctKey(eB) || scoreKey(eA) != scoreKey(eB)) return false
            if (rules.tieBreakAttendance && attKey(eA) != attKey(eB)) return false
            if (rules.tieBreakTraits && traitKey(eA) != traitKey(eB)) return false
            if (rules.tieBreakPrevRoll && sA.roll != sB.roll) return false
            return true
        }

        val withData = sorted.filter { it.second.hasAnyData }
        val withoutData = sorted.filter { !it.second.hasAnyData }

        val rankedWithData = mutableListOf<RankedStudentResult>()
        val rawRanks = IntArray(withData.size)
        for (i in withData.indices) {
            rawRanks[i] = if (!rules.jointMeritEnabled) {
                i + 1
            } else {
                if (i == 0) 1
                else if (isTied(withData[i - 1], withData[i])) rawRanks[i - 1]
                else i + 1
            }
        }
        val rankCounts = rawRanks.toList().groupingBy { it }.eachCount()
        for (i in withData.indices) {
            val r = rawRanks[i]
            rankedWithData.add(
                RankedStudentResult(
                    student = withData[i].first,
                    eval = withData[i].second,
                    rank = r,
                    isJointRank = rules.jointMeritEnabled && (rankCounts[r] ?: 0) > 1
                )
            )
        }

        val unrankedTail = withoutData.map { (st, ev) ->
            RankedStudentResult(
                student = st,
                eval = ev,
                rank = 0,
                isJointRank = false
            )
        }
        return rankedWithData + unrankedTail
    }

    fun rankMapForClass(
        classStudents: List<Student>,
        rules: ResultEvaluationRules,
        scopedTerm: Int = 0
    ): Map<String, RankedStudentResult> =
        rankClassStudents(classStudents, rules, scopedTerm).associateBy { it.student.id }

    fun doubleEquals(a: Double, b: Double): Boolean = abs(a - b) < 1e-6
}
