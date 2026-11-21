package com.kgps.result

enum class DataResetCategory(
    val titleBn: String,
    val subtitleBn: String,
    val badgeBn: String,
    val isHighRiskAll: Boolean
) {
    RESULTS(
        titleBn = "শ্রেণি ও বিষয়ভিত্তিক রেজাল্ট রিসেট",
        subtitleBn = "নির্দিষ্ট শ্রেণি, প্রান্তিক বা নির্দিষ্ট বিষয়ের নম্বর মুছুন (শিক্ষার্থীর নাম ও তথ্য ঠিক থাকবে)",
        badgeBn = "নম্বর রিসেট",
        isHighRiskAll = false
    ),
    ATTENDANCE_TRAITS(
        titleBn = "উপস্থিতি, গুণাবলী ও মন্তব্য রিসেট",
        subtitleBn = "কার্যদিবস, উপস্থিতি, ৩টি গুণাবলী বা শ্রেণি শিক্ষকের মন্তব্য মুছুন",
        badgeBn = "উপস্থিতি ও মন্তব্য",
        isHighRiskAll = false
    ),
    BASIC_INFO(
        titleBn = "শিক্ষার্থীর বেসিক তথ্য রিসেট",
        subtitleBn = "অভিভাবক, মোবাইল, ঠিকানা, ধর্ম বা কাস্টম ফিল্ড পরিষ্কার করুন (পরীক্ষার নম্বর অক্ষুণ্ণ থাকবে)",
        badgeBn = "বেসিক তথ্য",
        isHighRiskAll = false
    ),
    CLASS_STUDENTS(
        titleBn = "নির্দিষ্ট শ্রেণির সকল শিক্ষার্থী মুছুন",
        subtitleBn = "বেছে নেওয়া শ্রেণির সকল শিক্ষার্থী ও তাদের সমস্ত ডাটা স্থায়ীভাবে মুছে ফেলুন",
        badgeBn = "শ্রেণি মুছুন",
        isHighRiskAll = true
    ),
    ALL_STUDENTS(
        titleBn = "সকল শ্রেণির সব শিক্ষার্থী মুছুন",
        subtitleBn = "বিদ্যালয়ের ১ম–৫ম শ্রেণির সকল শিক্ষার্থীর তথ্য স্থায়ীভাবে মুছে ফেলুন",
        badgeBn = "সম্পূর্ণ মুছুন",
        isHighRiskAll = true
    ),
    SAMPLE_DATA(
        titleBn = "নমুনা ডাটা রিসেট",
        subtitleBn = "বর্তমান সব শিক্ষার্থী প্রতিস্থাপন করে প্রাথমিক নমুনা শিক্ষার্থী ও সেটিংস লোড করুন",
        badgeBn = "নমুনা রিসেট",
        isHighRiskAll = true
    )
}

data class ResetSubjectOption(
    val key: String,
    val labelBn: String
)

data class DataResetRequest(
    val category: DataResetCategory,
    val selectedClasses: Set<Int> = setOf(1),
    val selectedTerms: Set<Int> = setOf(1, 2, 3),
    val allSubjects: Boolean = true,
    val selectedSubjectKeys: Set<String> = emptySet(),
    val resetAttendance: Boolean = true,
    val resetTraits: Boolean = true,
    val resetRemarks: Boolean = true,
    val resetGuardian: Boolean = true,
    val resetMobile: Boolean = true,
    val resetAddress: Boolean = false,
    val resetReligionToDefault: Boolean = false,
    val resetCustomFields: Boolean = false
)

data class DataResetImpact(
    val matchedStudentCount: Int,
    val affectedStudentCount: Int,
    val affectedEntryCount: Int,
    val scopeSummaryBn: String,
    val preservedSummaryBn: String,
    val warningLines: List<String>
)

object DataResetEngine {

    private val SUBJECT_CATALOG = listOf(
        ResetSubjectOption("BAN", "বাংলা"),
        ResetSubjectOption("ENG", "ইংরেজি"),
        ResetSubjectOption("MATH", "গণিত"),
        ResetSubjectOption("SOC", "সামাজিক বিজ্ঞান ও বিজ্ঞান (১ম–২য়)"),
        ResetSubjectOption("SCI", "বিজ্ঞান (৩য়–৫ম)"),
        ResetSubjectOption("BGW", "বাংলাদেশ ও বিশ্বপরিচয় (৩য়–৫ম)"),
        ResetSubjectOption("REL", "ধর্মশিক্ষা"),
        ResetSubjectOption("PE", "শারীরিক ও মানসিক স্বাস্থ্য শিক্ষা"),
        ResetSubjectOption("ART", "শিল্পকলা")
    )

    fun subjectOptionsForClasses(classes: Set<Int>): List<ResetSubjectOption> {
        val targetClasses = classes.ifEmpty { (1..5).toSet() }
        val hasLow = targetClasses.any { it <= 2 }
        val hasHigh = targetClasses.any { it >= 3 }
        return SUBJECT_CATALOG.filter { opt ->
            when (opt.key) {
                "SOC" -> hasLow
                "SCI", "BGW" -> hasHigh
                else -> true
            }
        }
    }

    fun isRequestValid(request: DataResetRequest): Boolean {
        return when (request.category) {
            DataResetCategory.RESULTS ->
                request.selectedClasses.isNotEmpty() &&
                    request.selectedTerms.isNotEmpty() &&
                    (request.allSubjects || request.selectedSubjectKeys.isNotEmpty())
            DataResetCategory.ATTENDANCE_TRAITS ->
                request.selectedClasses.isNotEmpty() &&
                    request.selectedTerms.isNotEmpty() &&
                    (request.resetAttendance || request.resetTraits || request.resetRemarks)
            DataResetCategory.BASIC_INFO ->
                request.selectedClasses.isNotEmpty() &&
                    (request.resetGuardian || request.resetMobile || request.resetAddress ||
                        request.resetReligionToDefault || request.resetCustomFields)
            DataResetCategory.CLASS_STUDENTS ->
                request.selectedClasses.isNotEmpty()
            DataResetCategory.ALL_STUDENTS,
            DataResetCategory.SAMPLE_DATA -> true
        }
    }

    fun applyReset(
        students: List<Student>,
        request: DataResetRequest,
        customFieldKeys: Set<String> = emptySet()
    ): Pair<List<Student>, Int> {
        return when (request.category) {
            DataResetCategory.ALL_STUDENTS -> emptyList<Student>() to students.size
            DataResetCategory.SAMPLE_DATA -> SAMPLE_STUDENTS to students.size
            DataResetCategory.CLASS_STUDENTS -> {
                val kept = students.filterNot { it.cls in request.selectedClasses }
                kept to (students.size - kept.size)
            }
            DataResetCategory.RESULTS -> {
                var modifiedCount = 0
                val updated = students.map { st ->
                    if (st.cls !in request.selectedClasses) {
                        st
                    } else {
                        val filteredMarks = st.m.filterKeys { key ->
                            !matchesResultKey(key, request)
                        }
                        if (filteredMarks.size != st.m.size) {
                            modifiedCount++
                            st.copy(m = filteredMarks)
                        } else {
                            st
                        }
                    }
                }
                updated to modifiedCount
            }
            DataResetCategory.ATTENDANCE_TRAITS -> {
                var modifiedCount = 0
                val keysToDrop = buildAttendanceTraitKeys(request)
                val updated = students.map { st ->
                    if (st.cls !in request.selectedClasses) {
                        st
                    } else {
                        val filteredX = st.x.filterKeys { it !in keysToDrop }
                        if (filteredX.size != st.x.size) {
                            modifiedCount++
                            st.copy(x = filteredX)
                        } else {
                            st
                        }
                    }
                }
                updated to modifiedCount
            }
            DataResetCategory.BASIC_INFO -> {
                var modifiedCount = 0
                val updated = students.map { st ->
                    if (st.cls !in request.selectedClasses) {
                        st
                    } else {
                        val nextGuardian = if (request.resetGuardian) "" else st.guardian
                        val nextMobile = if (request.resetMobile) "" else st.mobile
                        val nextVillage = if (request.resetAddress) "" else st.village
                        val nextUnion = if (request.resetAddress) "" else st.union
                        val nextPost = if (request.resetAddress) "" else st.post
                        val nextUpazila = if (request.resetAddress) DEFAULT_UPAZILA else st.upazila
                        val nextDistrict = if (request.resetAddress) DEFAULT_DISTRICT else st.district
                        val nextReligion = if (request.resetReligionToDefault) "ইসলাম" else st.religion.ifBlank { "ইসলাম" }
                        val nextX = st.x.filterKeys { key ->
                            val dropCustom = request.resetCustomFields &&
                                (key in customFieldKeys || key.startsWith("CF_"))
                            val dropUnion = request.resetAddress && key == "UNION"
                            !dropCustom && !dropUnion
                        }
                        val nextStudent = st.copy(
                            guardian = nextGuardian,
                            mobile = nextMobile,
                            village = nextVillage,
                            union = nextUnion,
                            post = nextPost,
                            upazila = nextUpazila,
                            district = nextDistrict,
                            religion = nextReligion,
                            x = nextX
                        )
                        if (nextStudent != st) {
                            modifiedCount++
                        }
                        nextStudent
                    }
                }
                updated to modifiedCount
            }
        }
    }

    fun computeImpact(
        students: List<Student>,
        request: DataResetRequest,
        customFieldKeys: Set<String> = emptySet()
    ): DataResetImpact {
        val sortedClasses = request.selectedClasses.sorted()
        val classLabel = if (sortedClasses.size == 5) {
            "সকল শ্রেণি (১ম–৫ম)"
        } else if (sortedClasses.isEmpty()) {
            "কোনো শ্রেণি নির্বাচিত নয়"
        } else {
            sortedClasses.joinToString(", ") { "${CLSN[it - 1]} শ্রেণি" }
        }

        val termLabel = request.selectedTerms.sorted().joinToString(", ") { term ->
            when (term) {
                1 -> "১ম প্রান্তিক"
                2 -> "২য় প্রান্তিক"
                else -> "৩য় প্রান্তিক"
            }
        }

        return when (request.category) {
            DataResetCategory.RESULTS -> {
                val matched = students.filter { it.cls in request.selectedClasses }
                var affectedStudents = 0
                var removedEntries = 0
                matched.forEach { st ->
                    val hit = st.m.keys.count { matchesResultKey(it, request) }
                    if (hit > 0) {
                        affectedStudents++
                        removedEntries += hit
                    }
                }
                val subjectLabel = if (request.allSubjects) {
                    "সকল বিষয়"
                } else {
                    val names = SUBJECT_CATALOG
                        .filter { it.key in request.selectedSubjectKeys }
                        .map { it.labelBn }
                    names.joinToString(", ").ifBlank { "নির্বাচিত বিষয়" }
                }
                DataResetImpact(
                    matchedStudentCount = matched.size,
                    affectedStudentCount = affectedStudents,
                    affectedEntryCount = removedEntries,
                    scopeSummaryBn = "$classLabel · $termLabel · $subjectLabel",
                    preservedSummaryBn = "শিক্ষার্থীর নাম, রোল, বেসিক তথ্য, উপস্থিতি ও অন্যান্য প্রান্তিক/বিষয়ের নম্বর অক্ষুণ্ণ থাকবে।",
                    warningLines = listOf(
                        "নির্বাচিত শ্রেণি ও বিষয়ের সংরক্ষিত পরীক্ষার নম্বর স্থায়ীভাবে মুছে যাবে।",
                        "মোট ${bn(affectedStudents.toString())} জন শিক্ষার্থীর ${bn(removedEntries.toString())}টি নম্বর এন্ট্রি মুছে ফেলা হবে।"
                    )
                )
            }
            DataResetCategory.ATTENDANCE_TRAITS -> {
                val matched = students.filter { it.cls in request.selectedClasses }
                val dropKeys = buildAttendanceTraitKeys(request)
                var affectedStudents = 0
                var removedEntries = 0
                matched.forEach { st ->
                    val hit = st.x.keys.count { it in dropKeys }
                    if (hit > 0) {
                        affectedStudents++
                        removedEntries += hit
                    }
                }
                val parts = buildList {
                    if (request.resetAttendance) add("কার্যদিবস ও উপস্থিতি")
                    if (request.resetTraits) add("গুণাবলী")
                    if (request.resetRemarks) add("শ্রেণি শিক্ষকের মন্তব্য")
                }.joinToString(", ")
                DataResetImpact(
                    matchedStudentCount = matched.size,
                    affectedStudentCount = affectedStudents,
                    affectedEntryCount = removedEntries,
                    scopeSummaryBn = "$classLabel · $termLabel · $parts",
                    preservedSummaryBn = "শিক্ষার্থীর বেসিক তথ্য ও সকল বিষয়ের পরীক্ষার নম্বর অক্ষুণ্ণ থাকবে।",
                    warningLines = listOf(
                        "নির্বাচিত প্রান্তিকের $parts তথ্য মুছে যাবে।",
                        "মোট ${bn(affectedStudents.toString())} জন শিক্ষার্থীর ${bn(removedEntries.toString())}টি রেকর্ড পরিষ্কার হবে।"
                    )
                )
            }
            DataResetCategory.BASIC_INFO -> {
                val matched = students.filter { it.cls in request.selectedClasses }
                val (_, modifiedCount) = applyReset(matched, request, customFieldKeys)
                val fields = buildList {
                    if (request.resetGuardian) add("অভিভাবক")
                    if (request.resetMobile) add("মোবাইল")
                    if (request.resetAddress) add("ঠিকানা")
                    if (request.resetReligionToDefault) add("ধর্ম (ইসলাম)")
                    if (request.resetCustomFields) add("কাস্টম ফিল্ড")
                }.joinToString(", ")
                DataResetImpact(
                    matchedStudentCount = matched.size,
                    affectedStudentCount = modifiedCount,
                    affectedEntryCount = modifiedCount,
                    scopeSummaryBn = "$classLabel · $fields",
                    preservedSummaryBn = "শিক্ষার্থীর নাম, শ্রেণি, রোল, সকল বিষয়ের নম্বর ও উপস্থিতি/মন্তব্য অক্ষুণ্ণ থাকবে।",
                    warningLines = listOf(
                        "নির্বাচিত শ্রেণির শিক্ষার্থীদের বেসিক তথ্য ($fields) মুছে/ডিফল্টে রিসেট হবে।",
                        "মোট ${bn(modifiedCount.toString())} জন শিক্ষার্থীর তথ্যে পরিবর্তন আসবে।"
                    )
                )
            }
            DataResetCategory.CLASS_STUDENTS -> {
                val matched = students.filter { it.cls in request.selectedClasses }
                DataResetImpact(
                    matchedStudentCount = matched.size,
                    affectedStudentCount = matched.size,
                    affectedEntryCount = matched.size,
                    scopeSummaryBn = "$classLabel (সম্পূর্ণ শ্রেণি মুছুন)",
                    preservedSummaryBn = "অন্যান্য শ্রেণির শিক্ষার্থী ও বিদ্যালয়ের সেটিংস অক্ষুণ্ণ থাকবে।",
                    warningLines = listOf(
                        "সতর্কতা! $classLabel-এর সকল শিক্ষার্থী ও তাদের সকল নম্বর, উপস্থিতি ও তথ্য স্থায়ীভাবে মুছে যাবে।",
                        "মোট ${bn(matched.size.toString())} জন শিক্ষার্থী সম্পূর্ণভাবে ডিলিট হবে।"
                    )
                )
            }
            DataResetCategory.ALL_STUDENTS -> {
                DataResetImpact(
                    matchedStudentCount = students.size,
                    affectedStudentCount = students.size,
                    affectedEntryCount = students.size,
                    scopeSummaryBn = "সকল শ্রেণি (১ম–৫ম) — সম্পূর্ণ ডাটা মুছে ফেলা",
                    preservedSummaryBn = "বিদ্যালয়ের নাম, শিক্ষক তালিকা ও অ্যাপ সেটিংস অক্ষুণ্ণ থাকবে।",
                    warningLines = listOf(
                        "চরম সতর্কতা! বিদ্যালয়ের সকল শ্রেণির (${bn(students.size.toString())} জন) শিক্ষার্থী স্থায়ীভাবে মুছে যাবে।",
                        "আগে থেকে JSON/Excel/Drive ব্যাকআপ না থাকলে এই তথ্য আর ফিরিয়ে আনা যাবে না।"
                    )
                )
            }
            DataResetCategory.SAMPLE_DATA -> {
                DataResetImpact(
                    matchedStudentCount = students.size,
                    affectedStudentCount = students.size,
                    affectedEntryCount = students.size,
                    scopeSummaryBn = "নমুনা ডাটা রিসেট (প্রাথমিক নমুনা লোড)",
                    preservedSummaryBn = "বিদ্যালয়ের নাম, ঠিকানা ও শিক্ষাবর্ষ অক্ষুণ্ণ থাকবে।",
                    warningLines = listOf(
                        "সতর্কতা! বর্তমান ${bn(students.size.toString())} জন শিক্ষার্থীর তথ্য মুছে ৩ জন নমুনা শিক্ষার্থী ও ডিফল্ট কাস্টম সেটিংস লোড হবে।",
                        "প্রকৃত বিদ্যালয়ের বর্তমান রেজাল্ট ও তথ্য হারিয়ে যাবে যদি ব্যাকআপ না থাকে।"
                    )
                )
            }
        }
    }

    private fun matchesResultKey(key: String, request: DataResetRequest): Boolean {
        val parts = key.split("_", limit = 3)
        if (parts.size < 2) return false
        val subKey = parts[0]
        val term = parts[1].toIntOrNull() ?: return false
        if (term !in request.selectedTerms) return false
        return request.allSubjects || subKey in request.selectedSubjectKeys
    }

    private fun buildAttendanceTraitKeys(request: DataResetRequest): Set<String> {
        val keys = mutableSetOf<String>()
        for (t in request.selectedTerms) {
            if (request.resetAttendance) {
                keys += "DAYS_$t"
                keys += "PRES_$t"
            }
            if (request.resetTraits) {
                keys += "ACT_$t"
                keys += "DISC_$t"
                keys += "COND_$t"
            }
            if (request.resetRemarks) {
                keys += "REM_$t"
            }
        }
        return keys
    }
}
