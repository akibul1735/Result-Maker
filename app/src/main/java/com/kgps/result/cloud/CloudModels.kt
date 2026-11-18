package com.kgps.result.cloud

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.kgps.result.CLS_SHORT
import com.kgps.result.Settings

enum class MemberRole(val code: String, val labelBn: String) {
    ADMIN("ADMIN", "অ্যাডমিন"),
    TEACHER("TEACHER", "শিক্ষক");

    companion object {
        fun fromCode(raw: String?): MemberRole =
            entries.firstOrNull { it.code == raw } ?: TEACHER
    }
}

data class SubjectAccessDef(
    val key: String,
    val labelBn: String,
    val shortBn: String
)

val SUBJECT_ACCESS_DEFS = listOf(
    SubjectAccessDef("BAN", "বাংলা", "বাংলা"),
    SubjectAccessDef("ENG", "ইংরেজি", "ইংরেজি"),
    SubjectAccessDef("MATH", "গণিত", "গণিত"),
    SubjectAccessDef("SOC", "সামাজিক বিজ্ঞান ও বিজ্ঞান (১ম–২য়)", "সামাজিক/বিজ্ঞান"),
    SubjectAccessDef("SCI", "বিজ্ঞান (৩য়–৫ম)", "বিজ্ঞান"),
    SubjectAccessDef("BGW", "বাংলাদেশ ও বিশ্বপরিচয় (৩য়–৫ম)", "বাওবি"),
    SubjectAccessDef("REL", "ধর্মশিক্ষা", "ধর্ম"),
    SubjectAccessDef("PE", "শারীরিক ও মানসিক স্বাস্থ্য", "শারীরিক শিক্ষা"),
    SubjectAccessDef("ART", "শিল্পকলা", "শিল্পকলা")
)

val ALL_SUBJECT_KEYS: List<String> = SUBJECT_ACCESS_DEFS.map { it.key }
val ALL_CLASS_NUMS: List<Int> = listOf(1, 2, 3, 4, 5)

@Keep
@IgnoreExtraProperties
data class TeacherPermissions(
    val allClasses: Boolean = true,
    val allowedClasses: List<Int> = ALL_CLASS_NUMS,
    val allSubjects: Boolean = true,
    val allowedSubjects: List<String> = ALL_SUBJECT_KEYS,
    val canViewOtherSubjects: Boolean = true,
    val canEditAttendance: Boolean = true,
    val canManageStudents: Boolean = true,
    val canExportPdf: Boolean = true
) {
    fun canAccessClass(cls: Int): Boolean = allClasses || cls in allowedClasses

    fun canEditSubject(subKey: String): Boolean = allSubjects || subKey in allowedSubjects

    fun canViewSubject(subKey: String): Boolean =
        allSubjects || subKey in allowedSubjects || canViewOtherSubjects

    fun effectiveClasses(): List<Int> =
        if (allClasses) ALL_CLASS_NUMS else allowedClasses.filter { it in 1..5 }.distinct().sorted()

    fun effectiveSubjects(): List<String> =
        if (allSubjects) ALL_SUBJECT_KEYS else allowedSubjects.filter { it in ALL_SUBJECT_KEYS }.distinct()

    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "allClasses" to allClasses,
        "allowedClasses" to effectiveClasses(),
        "allSubjects" to allSubjects,
        "allowedSubjects" to effectiveSubjects(),
        "canViewOtherSubjects" to canViewOtherSubjects,
        "canEditAttendance" to canEditAttendance,
        "canManageStudents" to canManageStudents,
        "canExportPdf" to canExportPdf
    )

    fun summaryBn(): String {
        val clsPart = when {
            allClasses -> "সব শ্রেণি"
            allowedClasses.isEmpty() -> "কোনো শ্রেণি নয়"
            else -> allowedClasses.sorted().joinToString(", ") { CLS_SHORT.getOrElse(it - 1) { it.toString() } }
        }
        val subPart = when {
            allSubjects -> "সব বিষয়"
            allowedSubjects.isEmpty() -> "কোনো বিষয় নয়"
            else -> allowedSubjects.mapNotNull { k -> SUBJECT_ACCESS_DEFS.find { it.key == k }?.shortBn }.joinToString(", ")
        }
        val extras = buildList {
            if (canManageStudents) add("শিক্ষার্থী প্রোফাইল")
            if (canEditAttendance) add("উপস্থিতি/মতামত")
            if (canExportPdf) add("PDF")
        }.joinToString(" · ")
        return if (extras.isNotBlank()) "$clsPart · $subPart · $extras" else "$clsPart · $subPart"
    }

    companion object {
        fun fullAccess(): TeacherPermissions = TeacherPermissions()

        fun subjectOnlyPreset(
            classes: List<Int> = ALL_CLASS_NUMS,
            subjects: List<String> = listOf("BAN")
        ): TeacherPermissions = TeacherPermissions(
            allClasses = classes.size == 5,
            allowedClasses = classes,
            allSubjects = false,
            allowedSubjects = subjects,
            canViewOtherSubjects = false,
            canEditAttendance = false,
            canManageStudents = false,
            canExportPdf = false
        )

        fun fromMap(map: Map<*, *>?): TeacherPermissions {
            if (map == null) return fullAccess()
            val hasExplicitKeys = map.containsKey("allClasses") ||
                map.containsKey("allSubjects") ||
                map.containsKey("canManageStudents")
            if (!hasExplicitKeys) return fullAccess()

            val allClasses = (map["allClasses"] as? Boolean) ?: true
            val allowedClasses = (map["allowedClasses"] as? List<*>)
                ?.mapNotNull { (it as? Number)?.toInt() }
                ?.filter { it in 1..5 }
                ?.distinct()
                ?.sorted()
                ?: if (allClasses) ALL_CLASS_NUMS else emptyList()

            val allSubjects = (map["allSubjects"] as? Boolean) ?: true
            val allowedSubjects = (map["allowedSubjects"] as? List<*>)
                ?.mapNotNull { it as? String }
                ?.filter { it in ALL_SUBJECT_KEYS }
                ?.distinct()
                ?: if (allSubjects) ALL_SUBJECT_KEYS else emptyList()

            return TeacherPermissions(
                allClasses = allClasses,
                allowedClasses = allowedClasses,
                allSubjects = allSubjects,
                allowedSubjects = allowedSubjects,
                canViewOtherSubjects = (map["canViewOtherSubjects"] as? Boolean) ?: false,
                canEditAttendance = (map["canEditAttendance"] as? Boolean) ?: false,
                canManageStudents = (map["canManageStudents"] as? Boolean) ?: false,
                canExportPdf = (map["canExportPdf"] as? Boolean) ?: true
            )
        }
    }
}

@Keep
@IgnoreExtraProperties
data class SchoolMemberProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: MemberRole = MemberRole.TEACHER,
    val permissions: TeacherPermissions = TeacherPermissions.fullAccess()
)

@Keep
@IgnoreExtraProperties
data class UserCloudProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val activeSchoolId: String = "",
    val pendingSchoolId: String = "",
    val pendingSchoolName: String = ""
)

@Keep
@IgnoreExtraProperties
data class SchoolWorkspace(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val joinCode: String = "",
    val approvalRequired: Boolean = true,
    val memberUids: List<String> = emptyList(),
    val adminUids: List<String> = emptyList(),
    val members: List<SchoolMemberProfile> = emptyList(),
    val settings: Settings = Settings(),
    val updatedAt: Timestamp? = null
) {
    fun isAdmin(uid: String): Boolean = uid.isNotBlank() && (uid == ownerUid || uid in adminUids)

    fun roleOf(uid: String): MemberRole =
        if (isAdmin(uid)) MemberRole.ADMIN else MemberRole.TEACHER

    fun permissionsFor(uid: String): TeacherPermissions {
        if (isAdmin(uid)) return TeacherPermissions.fullAccess()
        return members.find { it.uid == uid }?.permissions ?: TeacherPermissions(
            allClasses = false,
            allowedClasses = emptyList(),
            allSubjects = false,
            allowedSubjects = emptyList(),
            canViewOtherSubjects = false,
            canEditAttendance = false,
            canManageStudents = false,
            canExportPdf = false
        )
    }
}

@Keep
@IgnoreExtraProperties
data class SchoolCodePreview(
    val code: String = "",
    val schoolId: String = "",
    val schoolName: String = "",
    val ownerUid: String = "",
    val adminUids: List<String> = emptyList(),
    val approvalRequired: Boolean = true,
    val active: Boolean = true
)

@Keep
@IgnoreExtraProperties
data class JoinRequestItem(
    @DocumentId val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val schoolId: String = "",
    val schoolName: String = "",
    val schoolAdminUids: List<String> = emptyList(),
    val status: String = "PENDING",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudentSyncMeta(
    val updatedByUid: String = "",
    val updatedByName: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

sealed interface CloudSessionState {
    data object Loading : CloudSessionState
    data class Ready(
        val schools: List<SchoolWorkspace>,
        val activeSchool: SchoolWorkspace?
    ) : CloudSessionState
    data class Error(val message: String) : CloudSessionState
}
