package com.kgps.result.ai

import android.graphics.Bitmap
import com.kgps.result.CLSN
import com.kgps.result.bn
import kotlinx.coroutines.delay

enum class AiModelMode {
    GENERAL,
    FAST
}

data class AiChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val modelLabel: String? = null,
    val isPlaceholder: Boolean = false
)

data class ExtractedMarkItem(
    val key: String,
    val value: Double,
    val subjectLabel: String
)

data class ExtractedStudentInfo(
    val name: String? = null,
    val cls: Int? = null,
    val roll: Int? = null,
    val guardian: String? = null,
    val village: String? = null,
    val upazila: String? = null,
    val district: String? = null,
    val mobile: String? = null,
    val religion: String? = null
)

data class ExtractedAttendanceAndTraits(
    val days: String? = null,
    val present: String? = null,
    val act: String? = null,
    val disc: String? = null,
    val cond: String? = null,
    val remark: String? = null
)

data class ExtractedMarksheet(
    val summary: String,
    val student: ExtractedStudentInfo,
    val term: Int,
    val marks: List<ExtractedMarkItem>,
    val attendanceAndTraits: ExtractedAttendanceAndTraits,
    val isPlaceholder: Boolean = true
)

data class AiChatReply(
    val text: String,
    val modelLabel: String,
    val isPlaceholder: Boolean = true
)

data class AiDraftRemarkResult(
    val remark: String,
    val isPlaceholder: Boolean = true
)

interface AiRepository {
    val isPlaceholderBackend: Boolean
    suspend fun chat(
        history: List<AiChatMessage>,
        latestUserMessage: String,
        modelMode: AiModelMode,
        studentContext: String?
    ): AiChatReply

    suspend fun analyzeMarksheetImage(
        bitmap: Bitmap,
        teacherNote: String
    ): ExtractedMarksheet

    suspend fun draftRemark(
        studentSummary: String,
        term: Int
    ): AiDraftRemarkResult
}

/**
 * Placeholder implementation used when Firebase AI Logic is not enabled.
 * All responses are clearly labeled sample outputs until Firebase is enabled.
 */
class PlaceholderAiRepository : AiRepository {
    override val isPlaceholderBackend: Boolean = true

    override suspend fun chat(
        history: List<AiChatMessage>,
        latestUserMessage: String,
        modelMode: AiModelMode,
        studentContext: String?
    ): AiChatReply {
        delay(450)
        val q = latestUserMessage.lowercase()
        val modeTag = if (modelMode == AiModelMode.FAST) "Sample · Fast Mode" else "Sample · Detailed Mode"
        val body = when {
            q.contains("মতামত") || q.contains("remark") -> {
                "শিক্ষার্থী শ্রেণিকক্ষে নিয়মিত উপস্থিত থাকে এবং পাঠ্যবিষয়ে মনোযোগী। ধারাবাহিক ও সামষ্টিক মূল্যায়নে ভালো অগ্রগতি দেখা গেছে; ইংরেজি ও গণিতে নিয়মিত অনুশীলন করলে আরও উৎকর্ষ অর্জন করবে।"
            }
            q.contains("বিভাজন") || q.contains("নম্বর") || q.contains("grading") -> {
                """
                NCTB প্রাথমিক মূল্যায়ন নম্বর বিভাজন:
                • ১ম–২য় শ্রেণি: বাংলা, ইংরেজি, গণিত (১০০ নম্বর) = ধারাবাহিক ৫০ (পাঠ্যবই ২০ + সক্রিয়তা ৫ + ভাষা ৫ + ক্লাস টেস্ট ২০) + সামষ্টিক ৫০।
                • ১ম–২য় শ্রেণি: সামাজিক/বিজ্ঞান, ধর্ম, শারীরিক শিক্ষা (৫০ নম্বর) = ধারাবাহিক ৫০। শিল্পকলা = ৫০।
                • ৩য়–৫ম শ্রেণি: বাংলা, ইংরেজি, গণিত, বিজ্ঞান, বাওবি, ধর্ম (১০০ নম্বর) = ধারাবাহিক ৩০ (পাঠ্যবই ১০ + সক্রিয়তা ৫ + ভাষা ৫ + ক্লাস টেস্ট ১০) + সামষ্টিক ৭০।
                • গ্রেড স্কেল: ৮০%+=A (অতি উত্তম), ৬০-৭৯%=B (উত্তম), ৪০-৫৯%=C (সন্তোষজনক), ০-৩৯%=D (সহায়তা প্রয়োজন)।
                """.trimIndent()
            }
            q.contains("অভিভাবক") || q.contains("সভা") || q.contains("summary") -> {
                "অভিভাবক সভার সারসংক্ষেপ (নমুনা): শিক্ষার্থীর উপস্থিতি ও শ্রেণি সক্রিয়তা সন্তোষজনক। বাসায় প্রতিদিন ৩০ মিনিট বাংলা ও গণিত অনুশীলন এবং সময়মতো বিদ্যালয়ে উপস্থিতি নিশ্চিত করার জন্য অনুরোধ করা হলো।"
            }
            else -> {
                val ctxNote = if (!studentContext.isNullOrBlank()) {
                    "\n\nপ্রাসঙ্গিক তথ্য: $studentContext"
                } else ""
                "নমুনা উত্তর (Firebase AI Logic সংযুক্ত নয়): আপনার প্রশ্ন \"${latestUserMessage.take(80)}\" গৃহীত হয়েছে। লাইভ Gemini উত্তর পেতে Firebase সক্রিয় করুন।$ctxNote"
            }
        }
        return AiChatReply(
            text = "[নমুনা উত্তর — লাইভ AI এর জন্য Firebase প্রয়োজন]\n\n$body",
            modelLabel = modeTag,
            isPlaceholder = true
        )
    }

    override suspend fun analyzeMarksheetImage(
        bitmap: Bitmap,
        teacherNote: String
    ): ExtractedMarksheet {
        delay(650)
        val detectedTerm = when {
            teacherNote.contains("৩") || teacherNote.contains("3") -> 3
            teacherNote.contains("২") || teacherNote.contains("2") -> 2
            else -> 1
        }
        val detectedCls = when {
            teacherNote.contains("৫") || teacherNote.contains("5") -> 5
            teacherNote.contains("৪") || teacherNote.contains("4") -> 4
            teacherNote.contains("৩") || teacherNote.contains("3") -> 3
            teacherNote.contains("২") || teacherNote.contains("2") -> 2
            else -> 1
        }
        val t = detectedTerm
        val isUpper = detectedCls >= 3
        val sampleMarks = if (isUpper) {
            listOf(
                ExtractedMarkItem("BAN_${t}_TB", 9.0, "বাংলা (পাঠ্যবই)"),
                ExtractedMarkItem("BAN_${t}_WR", 61.0, "বাংলা (সামষ্টিক)"),
                ExtractedMarkItem("ENG_${t}_TB", 8.0, "ইংরেজি (পাঠ্যবই)"),
                ExtractedMarkItem("ENG_${t}_WR", 56.0, "ইংরেজি (সামষ্টিক)"),
                ExtractedMarkItem("MATH_${t}_TB", 10.0, "গণিত (পাঠ্যবই)"),
                ExtractedMarkItem("MATH_${t}_WR", 64.0, "গণিত (সামষ্টিক)")
            )
        } else {
            listOf(
                ExtractedMarkItem("BAN_${t}_TB", 18.0, "বাংলা (পাঠ্যবই)"),
                ExtractedMarkItem("BAN_${t}_WR", 44.0, "বাংলা (সামষ্টিক)"),
                ExtractedMarkItem("ENG_${t}_TB", 17.0, "ইংরেজি (পাঠ্যবই)"),
                ExtractedMarkItem("ENG_${t}_WR", 41.0, "ইংরেজি (সামষ্টিক)"),
                ExtractedMarkItem("MATH_${t}_TB", 19.0, "গণিত (পাঠ্যবই)"),
                ExtractedMarkItem("MATH_${t}_WR", 46.0, "গণিত (সামষ্টিক)")
            )
        }
        return ExtractedMarksheet(
            summary = "[নমুনা স্ক্যান ফলাফল — লাইভ ছবি বিশ্লেষণের জন্য Firebase AI Logic প্রয়োজন]\n${CLSN[detectedCls - 1]} শ্রেণির ${bn(t.toString())}ম প্রান্তিকের নমুনা নম্বরপত্র শনাক্ত দেখানো হয়েছে (${bitmap.width}×${bitmap.height}px)।",
            student = ExtractedStudentInfo(
                name = "রাফিউজ্জামান রাফি (নমুনা)",
                cls = detectedCls,
                roll = 10,
                guardian = "মোঃ আবুল কালাম",
                religion = "ইসলাম"
            ),
            term = t,
            marks = sampleMarks,
            attendanceAndTraits = ExtractedAttendanceAndTraits(
                days = "64",
                present = "60",
                act = "ক",
                disc = "ক",
                cond = "ক",
                remark = "পাঠ্যবিষয়ে মনোযোগী ও নিয়মিত (নমুনা খসড়া)।"
            ),
            isPlaceholder = true
        )
    }

    override suspend fun draftRemark(
        studentSummary: String,
        term: Int
    ): AiDraftRemarkResult {
        delay(450)
        return AiDraftRemarkResult(
            remark = "${bn(term.toString())} প্রান্তিকের মূল্যায়নে শিক্ষার্থীর অংশগ্রহণ ও অগ্রগতি প্রশংসনীয়। নিয়মিত পাঠ অনুশীলন ও উপস্থিতি বজায় রাখলে আগামীতে আরও ভালো ফলাফল অর্জন করবে। [নমুনা খসড়া — Firebase প্রয়োজন]",
            isPlaceholder = true
        )
    }
}
