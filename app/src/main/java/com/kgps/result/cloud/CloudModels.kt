package com.kgps.result.cloud

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.kgps.result.Settings

enum class MemberRole(val code: String, val labelBn: String) {
    ADMIN("ADMIN", "অ্যাডমিন"),
    TEACHER("TEACHER", "শিক্ষক");

    companion object {
        fun fromCode(raw: String?): MemberRole =
            entries.firstOrNull { it.code == raw } ?: TEACHER
    }
}

@Keep
@IgnoreExtraProperties
data class SchoolMemberProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: MemberRole = MemberRole.TEACHER
)

@Keep
@IgnoreExtraProperties
data class UserCloudProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val activeSchoolId: String = ""
)

@Keep
@IgnoreExtraProperties
data class SchoolWorkspace(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val joinCode: String = "",
    val approvalRequired: Boolean = false,
    val memberUids: List<String> = emptyList(),
    val adminUids: List<String> = emptyList(),
    val members: List<SchoolMemberProfile> = emptyList(),
    val settings: Settings = Settings(),
    val updatedAt: Timestamp? = null
) {
    fun isAdmin(uid: String): Boolean = uid.isNotBlank() && (uid == ownerUid || uid in adminUids)
    fun roleOf(uid: String): MemberRole =
        if (isAdmin(uid)) MemberRole.ADMIN else MemberRole.TEACHER
}

@Keep
@IgnoreExtraProperties
data class SchoolCodePreview(
    val code: String = "",
    val schoolId: String = "",
    val schoolName: String = "",
    val ownerUid: String = "",
    val adminUids: List<String> = emptyList(),
    val approvalRequired: Boolean = false,
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
