package com.kgps.result.cloud

import android.content.Context
import com.kgps.result.BlankSheetQrMeta
import com.kgps.result.DEFAULT_DISTRICT
import com.kgps.result.DEFAULT_SETTINGS
import com.kgps.result.DEFAULT_UPAZILA
import com.kgps.result.Repo
import com.kgps.result.ResultEvaluationRules
import com.kgps.result.SAMPLE_STUDENTS
import com.kgps.result.Settings
import com.kgps.result.SheetQrCodec
import com.kgps.result.Student
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Local-only offline implementation of [SchoolDataGateway] used in preview emulators
 * so the app boots and runs every feature locally via [Repo] without touching Firebase.
 */
class LocalPreviewSchoolGateway(
    private val appContext: Context
) : SchoolDataGateway {

    companion object {
        const val PREVIEW_UID = "preview-local-teacher"
        const val PREVIEW_NAME = "প্রিভিউ শিক্ষক"
        const val PREVIEW_EMAIL = "preview@local"
        const val PREVIEW_SCHOOL_ID = "preview-local-school"
    }

    private val mutex = Mutex()
    private val templateCache = SheetTemplateCache(appContext)

    private val initialPair: Pair<List<Student>, Settings> = Repo.load(appContext)?.let { (loadedStudents, loadedSettings) ->
        if (loadedStudents.isEmpty() && loadedSettings.school.isBlank()) {
            SAMPLE_STUDENTS to DEFAULT_SETTINGS
        } else {
            loadedStudents to loadedSettings
        }
    } ?: (SAMPLE_STUDENTS to DEFAULT_SETTINGS)

    private val initialSchool = SchoolWorkspace(
        id = PREVIEW_SCHOOL_ID,
        ownerUid = PREVIEW_UID,
        joinCode = "LOCAL",
        approvalRequired = false,
        memberUids = listOf(PREVIEW_UID),
        adminUids = listOf(PREVIEW_UID),
        members = listOf(
            SchoolMemberProfile(
                uid = PREVIEW_UID,
                name = PREVIEW_NAME,
                email = PREVIEW_EMAIL,
                role = MemberRole.ADMIN,
                permissions = TeacherPermissions.fullAccess()
            )
        ),
        settings = initialPair.second
    )

    private val profileFlow = MutableStateFlow<UserCloudProfile?>(
        UserCloudProfile(
            id = PREVIEW_UID,
            uid = PREVIEW_UID,
            displayName = PREVIEW_NAME,
            email = PREVIEW_EMAIL,
            activeSchoolId = PREVIEW_SCHOOL_ID
        )
    )

    private val schoolsFlow = MutableStateFlow(listOf(initialSchool))
    private val studentsBySchoolFlow = MutableStateFlow(
        mapOf(PREVIEW_SCHOOL_ID to initialPair.first.sortedWith(compareBy({ it.cls }, { it.roll })))
    )

    init {
        Repo.save(appContext, initialPair.first, initialPair.second)
    }

    private fun persistActiveLocked(schoolId: String) {
        val activeSettings = schoolsFlow.value.find { it.id == schoolId }?.settings ?: DEFAULT_SETTINGS
        val activeStudents = studentsBySchoolFlow.value[schoolId].orEmpty()
        Repo.save(appContext, activeStudents, activeSettings)
    }

    override suspend fun ensureUserProfile(): Result<UserCloudProfile> =
        Result.success(profileFlow.value!!)

    override fun observeUserProfile(): Flow<UserCloudProfile?> = profileFlow

    override suspend fun setActiveSchoolId(schoolId: String): Result<Unit> = mutex.withLock {
        val current = profileFlow.value ?: return@withLock Result.success(Unit)
        profileFlow.value = current.copy(
            activeSchoolId = schoolId,
            pendingSchoolId = "",
            pendingSchoolName = ""
        )
        Result.success(Unit)
    }

    override suspend fun cancelPendingJoinRequest(schoolId: String): Result<Unit> = mutex.withLock {
        val current = profileFlow.value ?: return@withLock Result.success(Unit)
        profileFlow.value = current.copy(pendingSchoolId = "", pendingSchoolName = "")
        Result.success(Unit)
    }

    override fun observeMySchools(): Flow<List<SchoolWorkspace>> = schoolsFlow

    override fun observeJoinRequests(schoolId: String, adminUid: String): Flow<List<JoinRequestItem>> =
        flowOf(emptyList())

    override suspend fun createSchool(
        schoolName: String,
        schoolAddress: String,
        year: String,
        baseSettings: Settings,
        initialStudents: List<Student>
    ): Result<SchoolWorkspace> = mutex.withLock {
        val cleanName = schoolName.trim().ifBlank { "প্রাথমিক বিদ্যালয়" }.take(160)
        val cleanAddr = schoolAddress.trim().take(240)
        val cleanYear = year.trim().ifBlank { "২০২৬" }.take(40)
        val schoolId = "sch_" + UUID.randomUUID().toString().replace("-", "").take(12)
        val nextSettings = baseSettings.copy(
            school = cleanName,
            schoolAddress = cleanAddr,
            year = cleanYear
        )
        val workspace = SchoolWorkspace(
            id = schoolId,
            ownerUid = PREVIEW_UID,
            joinCode = "LOCAL",
            approvalRequired = false,
            memberUids = listOf(PREVIEW_UID),
            adminUids = listOf(PREVIEW_UID),
            members = listOf(
                SchoolMemberProfile(
                    uid = PREVIEW_UID,
                    name = PREVIEW_NAME,
                    email = PREVIEW_EMAIL,
                    role = MemberRole.ADMIN,
                    permissions = TeacherPermissions.fullAccess()
                )
            ),
            settings = nextSettings
        )
        schoolsFlow.value = listOf(workspace)
        val sortedStudents = initialStudents.sortedWith(compareBy({ it.cls }, { it.roll }))
        studentsBySchoolFlow.value = mapOf(schoolId to sortedStudents)
        profileFlow.value = profileFlow.value?.copy(activeSchoolId = schoolId)
        persistActiveLocked(schoolId)
        Result.success(workspace)
    }

    override suspend fun lookupSchoolCode(rawCode: String): Result<SchoolCodePreview?> =
        Result.failure(IllegalStateException("প্রিভিউ এমুলেটরে Firebase বন্ধ রয়েছে (অফলাইন মোড)।"))

    override suspend fun joinSchoolWithCode(preview: SchoolCodePreview): Result<Boolean> =
        Result.failure(IllegalStateException("প্রিভিউ এমুলেটরে Firebase বন্ধ রয়েছে (অফলাইন মোড)।"))

    override suspend fun approveJoinRequest(
        schoolId: String,
        req: JoinRequestItem,
        permissions: TeacherPermissions
    ): Result<Unit> = Result.success(Unit)

    override suspend fun rejectJoinRequest(schoolId: String, requestUid: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun regenerateJoinCode(school: SchoolWorkspace): Result<String> =
        Result.success(school.joinCode.ifBlank { "LOCAL" })

    override suspend fun setApprovalRequired(school: SchoolWorkspace, required: Boolean): Result<Unit> =
        mutex.withLock {
            schoolsFlow.value = schoolsFlow.value.map {
                if (it.id == school.id) it.copy(approvalRequired = required) else it
            }
            Result.success(Unit)
        }

    override suspend fun updateMemberPermissions(
        school: SchoolWorkspace,
        targetUid: String,
        permissions: TeacherPermissions
    ): Result<Unit> = Result.success(Unit)

    override suspend fun updateMemberRole(
        school: SchoolWorkspace,
        targetUid: String,
        role: MemberRole
    ): Result<Unit> = Result.success(Unit)

    override suspend fun removeMember(school: SchoolWorkspace, targetUid: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun leaveSchool(school: SchoolWorkspace): Result<Unit> =
        Result.failure(IllegalStateException("অফলাইন প্রিভিউ মোডে বিদ্যালয় ত্যাগ করা যাবে না।"))

    override suspend fun updateSchoolSettings(
        school: SchoolWorkspace,
        nextSettings: Settings
    ): Result<Unit> = mutex.withLock {
        schoolsFlow.value = schoolsFlow.value.map {
            if (it.id == school.id) it.copy(settings = nextSettings) else it
        }
        persistActiveLocked(school.id)
        Result.success(Unit)
    }

    override suspend fun updateSchoolResultRules(
        school: SchoolWorkspace,
        nextRules: ResultEvaluationRules
    ): Result<Unit> = mutex.withLock {
        schoolsFlow.value = schoolsFlow.value.map {
            if (it.id == school.id) it.copy(settings = it.settings.copy(resultRules = nextRules)) else it
        }
        persistActiveLocked(school.id)
        Result.success(Unit)
    }

    override fun observeSchoolStudents(
        schoolId: String
    ): Flow<Pair<List<Student>, Map<String, StudentSyncMeta>>> =
        studentsBySchoolFlow.map { bySchool ->
            val list = bySchool[schoolId].orEmpty()
            val meta = list.associate { st ->
                st.id to StudentSyncMeta(updatedByUid = PREVIEW_UID, updatedByName = PREVIEW_NAME)
            }
            list to meta
        }

    override suspend fun upsertStudent(
        schoolId: String,
        student: Student,
        existing: Student?
    ): Result<Unit> = mutex.withLock {
        val current = studentsBySchoolFlow.value[schoolId].orEmpty()
        val updated = (current.filterNot { it.id == student.id } + student)
            .sortedWith(compareBy({ it.cls }, { it.roll }))
        studentsBySchoolFlow.value = studentsBySchoolFlow.value + (schoolId to updated)
        persistActiveLocked(schoolId)
        Result.success(Unit)
    }

    override suspend fun patchStudentField(
        schoolId: String,
        studentId: String,
        fieldPath: String,
        value: Any?
    ): Result<Unit> = mutex.withLock {
        val current = studentsBySchoolFlow.value[schoolId].orEmpty()
        val updated = current.map { st ->
            if (st.id != studentId) st else applyFieldPatch(st, fieldPath, value)
        }
        studentsBySchoolFlow.value = studentsBySchoolFlow.value + (schoolId to updated)
        persistActiveLocked(schoolId)
        Result.success(Unit)
    }

    private fun applyFieldPatch(st: Student, fieldPath: String, value: Any?): Student {
        if (fieldPath.startsWith("m.")) {
            val key = fieldPath.removePrefix("m.")
            val nextM = st.m.toMutableMap()
            val num = (value as? Number)?.toDouble()
            if (num == null) nextM.remove(key) else nextM[key] = num
            return st.copy(m = nextM)
        }
        if (fieldPath.startsWith("x.")) {
            val key = fieldPath.removePrefix("x.")
            val text = (value as? String)?.trim()
            if (key == "UNION") {
                return st.copy(union = text.orEmpty())
            }
            val nextX = st.x.toMutableMap()
            if (text.isNullOrBlank()) nextX.remove(key) else nextX[key] = text
            return st.copy(x = nextX)
        }
        val text = (value as? String).orEmpty()
        return when (fieldPath) {
            "cls" -> st.copy(cls = (value as? Number)?.toInt()?.coerceIn(1, 20) ?: st.cls)
            "roll" -> st.copy(roll = (value as? Number)?.toInt()?.coerceAtLeast(1) ?: st.roll)
            "name" -> st.copy(name = text)
            "guardian" -> st.copy(guardian = text)
            "village" -> st.copy(village = text)
            "union" -> st.copy(union = text)
            "post" -> st.copy(post = text)
            "upazila" -> st.copy(upazila = text.ifBlank { DEFAULT_UPAZILA })
            "district" -> st.copy(district = text.ifBlank { DEFAULT_DISTRICT })
            "mobile" -> st.copy(mobile = text)
            "religion" -> st.copy(religion = text.ifBlank { "ইসলাম" })
            else -> st
        }
    }

    override suspend fun patchBatchXFields(
        schoolId: String,
        studentIds: Collection<String>,
        xUpdates: Map<String, String?>
    ): Result<Unit> = mutex.withLock {
        if (studentIds.isEmpty() || xUpdates.isEmpty()) return@withLock Result.success(Unit)
        val idSet = studentIds.toSet()
        val current = studentsBySchoolFlow.value[schoolId].orEmpty()
        val updated = current.map { st ->
            if (st.id !in idSet) st
            else {
                val nextX = st.x.toMutableMap()
                xUpdates.forEach { (k, v) ->
                    val cleanKey = k.trim()
                    if (cleanKey.isNotEmpty()) {
                        if (v.isNullOrBlank()) nextX.remove(cleanKey) else nextX[cleanKey] = v.trim().take(240)
                    }
                }
                st.copy(x = nextX)
            }
        }
        studentsBySchoolFlow.value = studentsBySchoolFlow.value + (schoolId to updated)
        persistActiveLocked(schoolId)
        Result.success(Unit)
    }

    override suspend fun patchBatchPerStudentXField(
        schoolId: String,
        xKey: String,
        valuesByStudentId: Map<String, String?>
    ): Result<Unit> = mutex.withLock {
        val cleanKey = xKey.trim()
        if (cleanKey.isEmpty() || valuesByStudentId.isEmpty()) return@withLock Result.success(Unit)
        val current = studentsBySchoolFlow.value[schoolId].orEmpty()
        val updated = current.map { st ->
            if (!valuesByStudentId.containsKey(st.id)) st
            else {
                val nextX = st.x.toMutableMap()
                val rawVal = valuesByStudentId[st.id]
                if (rawVal.isNullOrBlank()) nextX.remove(cleanKey) else nextX[cleanKey] = rawVal.trim().take(240)
                st.copy(x = nextX)
            }
        }
        studentsBySchoolFlow.value = studentsBySchoolFlow.value + (schoolId to updated)
        persistActiveLocked(schoolId)
        Result.success(Unit)
    }

    override suspend fun deleteStudent(schoolId: String, studentId: String): Result<Unit> = mutex.withLock {
        val current = studentsBySchoolFlow.value[schoolId].orEmpty()
        val updated = current.filterNot { it.id == studentId }
        studentsBySchoolFlow.value = studentsBySchoolFlow.value + (schoolId to updated)
        persistActiveLocked(schoolId)
        Result.success(Unit)
    }

    override suspend fun deleteAllSchoolStudents(
        schoolId: String,
        studentIds: List<String>
    ): Result<Unit> = mutex.withLock {
        val removeSet = studentIds.toSet()
        val current = studentsBySchoolFlow.value[schoolId].orEmpty()
        val updated = if (removeSet.isEmpty()) emptyList() else current.filterNot { it.id in removeSet }
        studentsBySchoolFlow.value = studentsBySchoolFlow.value + (schoolId to updated)
        persistActiveLocked(schoolId)
        Result.success(Unit)
    }

    override suspend fun syncBatchStudents(
        schoolId: String,
        previousList: List<Student>,
        nextList: List<Student>,
        deleteRemoved: Boolean
    ): Result<Unit> = mutex.withLock {
        val merged = if (deleteRemoved) {
            nextList
        } else {
            val nextById = nextList.associateBy { it.id }
            val current = studentsBySchoolFlow.value[schoolId].orEmpty()
            val kept = current.filterNot { nextById.containsKey(it.id) }
            kept + nextList
        }
        val sorted = merged.sortedWith(compareBy({ it.cls }, { it.roll }))
        studentsBySchoolFlow.value = studentsBySchoolFlow.value + (schoolId to sorted)
        persistActiveLocked(schoolId)
        Result.success(Unit)
    }

    override suspend fun saveSheetTemplate(schoolId: String, meta: BlankSheetQrMeta): Result<String> {
        val tid = meta.templateId.trim().uppercase().ifBlank { SheetQrCodec.computeTemplateId(meta) }.take(24)
        templateCache.put(meta.copy(templateId = tid))
        return Result.success(tid)
    }

    override suspend fun fetchSheetTemplate(schoolId: String, templateId: String): Result<BlankSheetQrMeta?> =
        Result.success(templateCache.get(templateId))

    override suspend fun fetchAllSheetTemplates(
        schoolId: String,
        limitCount: Long
    ): Result<List<BlankSheetQrMeta>> =
        Result.success(templateCache.getAll())
}
