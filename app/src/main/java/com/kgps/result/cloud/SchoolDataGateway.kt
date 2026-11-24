package com.kgps.result.cloud

import com.kgps.result.BlankSheetQrMeta
import com.kgps.result.ResultEvaluationRules
import com.kgps.result.Settings
import com.kgps.result.Student
import kotlinx.coroutines.flow.Flow

interface SchoolDataGateway {
    suspend fun ensureUserProfile(): Result<UserCloudProfile>
    fun observeUserProfile(): Flow<UserCloudProfile?>
    suspend fun setActiveSchoolId(schoolId: String): Result<Unit>
    suspend fun cancelPendingJoinRequest(schoolId: String): Result<Unit>

    fun observeMySchools(): Flow<List<SchoolWorkspace>>
    fun observeJoinRequests(schoolId: String, adminUid: String): Flow<List<JoinRequestItem>>

    suspend fun createSchool(
        schoolName: String,
        schoolAddress: String,
        year: String,
        baseSettings: Settings,
        initialStudents: List<Student>
    ): Result<SchoolWorkspace>

    suspend fun lookupSchoolCode(rawCode: String): Result<SchoolCodePreview?>
    suspend fun joinSchoolWithCode(preview: SchoolCodePreview): Result<Boolean>

    suspend fun approveJoinRequest(
        schoolId: String,
        req: JoinRequestItem,
        permissions: TeacherPermissions = TeacherPermissions.fullAccess()
    ): Result<Unit>

    suspend fun rejectJoinRequest(schoolId: String, requestUid: String): Result<Unit>
    suspend fun regenerateJoinCode(school: SchoolWorkspace): Result<String>
    suspend fun setApprovalRequired(school: SchoolWorkspace, required: Boolean): Result<Unit>

    suspend fun updateMemberPermissions(
        school: SchoolWorkspace,
        targetUid: String,
        permissions: TeacherPermissions
    ): Result<Unit>

    suspend fun updateMemberRole(school: SchoolWorkspace, targetUid: String, role: MemberRole): Result<Unit>
    suspend fun removeMember(school: SchoolWorkspace, targetUid: String): Result<Unit>
    suspend fun leaveSchool(school: SchoolWorkspace): Result<Unit>

    suspend fun updateSchoolSettings(school: SchoolWorkspace, nextSettings: Settings): Result<Unit>
    suspend fun updateSchoolResultRules(
        school: SchoolWorkspace,
        nextRules: ResultEvaluationRules
    ): Result<Unit>

    fun observeSchoolStudents(schoolId: String): Flow<Pair<List<Student>, Map<String, StudentSyncMeta>>>

    suspend fun upsertStudent(
        schoolId: String,
        student: Student,
        existing: Student?
    ): Result<Unit>

    suspend fun patchStudentField(
        schoolId: String,
        studentId: String,
        fieldPath: String,
        value: Any?
    ): Result<Unit>

    suspend fun patchBatchXFields(
        schoolId: String,
        studentIds: Collection<String>,
        xUpdates: Map<String, String?>
    ): Result<Unit>

    suspend fun patchBatchPerStudentXField(
        schoolId: String,
        xKey: String,
        valuesByStudentId: Map<String, String?>
    ): Result<Unit>

    suspend fun deleteStudent(schoolId: String, studentId: String): Result<Unit>
    suspend fun deleteAllSchoolStudents(schoolId: String, studentIds: List<String>): Result<Unit>

    suspend fun syncBatchStudents(
        schoolId: String,
        previousList: List<Student>,
        nextList: List<Student>,
        deleteRemoved: Boolean = false
    ): Result<Unit>

    suspend fun saveSheetTemplate(
        schoolId: String,
        meta: BlankSheetQrMeta
    ): Result<String>

    suspend fun fetchSheetTemplate(
        schoolId: String,
        templateId: String
    ): Result<BlankSheetQrMeta?>

    suspend fun fetchAllSheetTemplates(
        schoolId: String,
        limitCount: Long = 200L
    ): Result<List<BlankSheetQrMeta>>
}
