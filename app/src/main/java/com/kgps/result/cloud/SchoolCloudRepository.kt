package com.kgps.result.cloud

import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import com.kgps.result.BlankNumeralStyle
import com.kgps.result.BlankSheetQrMeta
import com.kgps.result.CustomFieldDef
import com.kgps.result.Settings
import com.kgps.result.SheetQrCodec
import com.kgps.result.Student
import com.kgps.result.ai.ClassSheetImportScope
import com.kgps.result.ai.ClassSheetKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.UUID

class SchoolCloudRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private val random = SecureRandom()

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Firestore ব্যবহারের আগে Google দিয়ে সাইন-ইন করতে হবে।")

    private fun currentUserName(): String {
        val user = auth.currentUser
        return user?.displayName?.trim()?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore('@')?.takeIf { it.isNotBlank() }
            ?: "শিক্ষক"
    }

    private fun currentUserEmail(): String =
        auth.currentUser?.email?.trim().orEmpty()

    private fun generateCodeCandidate(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString(6) {
            repeat(6) { append(alphabet[random.nextInt(alphabet.length)]) }
        }
    }

    private suspend fun allocateUniqueJoinCode(): String {
        repeat(8) {
            val code = generateCodeCandidate()
            val snap = db.collection("schoolCodes").document(code).get().await()
            if (!snap.exists()) return code
        }
        return "K" + UUID.randomUUID().toString().replace("-", "").uppercase().take(7)
    }

    // ---------- User Profile ----------

    suspend fun ensureUserProfile(): Result<UserCloudProfile> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val ref = db.collection("users").document(uid)
            val existing = ref.get().await()
            val name = currentUserName().take(100)
            val email = currentUserEmail().take(150)
            if (!existing.exists()) {
                val payload = mapOf(
                    "uid" to uid,
                    "displayName" to name,
                    "email" to email,
                    "activeSchoolId" to "",
                    "pendingSchoolId" to "",
                    "pendingSchoolName" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
                Result.success(UserCloudProfile(id = uid, uid = uid, displayName = name, email = email, activeSchoolId = ""))
            } else {
                ref.update(
                    mapOf(
                        "displayName" to name,
                        "email" to email,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(
                    UserCloudProfile(
                        id = uid,
                        uid = uid,
                        displayName = name,
                        email = email,
                        activeSchoolId = existing.getString("activeSchoolId").orEmpty(),
                        pendingSchoolId = existing.getString("pendingSchoolId").orEmpty(),
                        pendingSchoolName = existing.getString("pendingSchoolName").orEmpty()
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(): Flow<UserCloudProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { doc ->
                    if (!doc.exists()) null
                    else UserCloudProfile(
                        id = doc.id,
                        uid = doc.getString("uid").orEmpty(),
                        displayName = doc.getString("displayName").orEmpty(),
                        email = doc.getString("email").orEmpty(),
                        activeSchoolId = doc.getString("activeSchoolId").orEmpty(),
                        pendingSchoolId = doc.getString("pendingSchoolId").orEmpty(),
                        pendingSchoolName = doc.getString("pendingSchoolName").orEmpty()
                    )
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreException(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun setActiveSchoolId(schoolId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "activeSchoolId" to schoolId.take(128),
                        "pendingSchoolId" to "",
                        "pendingSchoolName" to "",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun cancelPendingJoinRequest(schoolId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            if (schoolId.isNotBlank()) {
                runCatching {
                    db.collection("schools").document(schoolId)
                        .collection("joinRequests").document(uid)
                        .delete()
                        .await()
                }
            }
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "pendingSchoolId" to "",
                        "pendingSchoolName" to "",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // ---------- Schools & Membership ----------

    fun observeMySchools(): Flow<List<SchoolWorkspace>> = flow {
        val uid = requireUserId()
        val path = "schools"
        emitAll(
            db.collection(path)
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap -> snap.documents.mapNotNull { parseSchoolDoc(it) } }
                .catch { error ->
                    val fsErr = unwrapFirestoreException(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeJoinRequests(schoolId: String, adminUid: String): Flow<List<JoinRequestItem>> = flow {
        val uid = requireUserId()
        require(schoolId.isNotBlank()) { "School ID required" }
        val path = "schools/$schoolId/joinRequests"
        emitAll(
            db.collection("schools").document(schoolId).collection("joinRequests")
                .whereArrayContains("schoolAdminUids", adminUid)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull { doc ->
                        JoinRequestItem(
                            id = doc.id,
                            uid = doc.getString("uid").orEmpty(),
                            displayName = doc.getString("displayName").orEmpty(),
                            email = doc.getString("email").orEmpty(),
                            schoolId = doc.getString("schoolId").orEmpty(),
                            schoolName = doc.getString("schoolName").orEmpty(),
                            schoolAdminUids = (doc.get("schoolAdminUids") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                            status = doc.getString("status") ?: "PENDING",
                            createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        )
                    }
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreException(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createSchool(
        schoolName: String,
        schoolAddress: String,
        year: String,
        baseSettings: Settings,
        initialStudents: List<Student>
    ): Result<SchoolWorkspace> {
        var path = "schools"
        return try {
            val uid = requireUserId()
            val cleanName = schoolName.trim().take(160)
            require(cleanName.isNotEmpty()) { "বিদ্যালয়ের নাম লিখুন" }

            // Enforce 1 school per user
            val existingMemberships = db.collection("schools")
                .whereArrayContains("memberUids", uid)
                .limit(1)
                .get()
                .await()
            if (!existingMemberships.isEmpty) {
                return Result.failure(IllegalStateException("একজন ব্যবহারকারী শুধুমাত্র একটি বিদ্যালয়ে যুক্ত থাকতে পারবেন।"))
            }

            val cleanAddr = schoolAddress.trim().take(240)
            val cleanYear = year.trim().ifBlank { "২০২৬" }.take(40)

            val schoolId = "sch_" + UUID.randomUUID().toString().replace("-", "").take(20)
            path = "schools/$schoolId"
            val joinCode = allocateUniqueJoinCode()

            val creatorName = currentUserName().take(100)
            val creatorEmail = currentUserEmail().take(150)

            val settings = baseSettings.copy(
                school = cleanName,
                schoolAddress = cleanAddr,
                year = cleanYear
            )

            val adminPerms = TeacherPermissions.fullAccess()
            val memberProfileMap = mapOf(
                "uid" to uid,
                "name" to creatorName,
                "email" to creatorEmail,
                "role" to MemberRole.ADMIN.code
            ) + adminPerms.toFirestoreMap()

            val schoolPayload = mapOf(
                "ownerUid" to uid,
                "joinCode" to joinCode,
                "approvalRequired" to true,
                "memberUids" to listOf(uid),
                "adminUids" to listOf(uid),
                "memberProfiles" to mapOf(uid to memberProfileMap),
                "school" to settings.school,
                "schoolAddress" to settings.schoolAddress,
                "year" to settings.year,
                "teachers" to settings.teachers.entries.take(25).associate { it.key.toString() to it.value.trim().take(120) },
                "teacherMobiles" to settings.teacherMobiles.entries.take(25).associate { it.key.toString() to it.value.trim().take(30) },
                "pdfFont" to com.kgps.result.encodePdfFontPair(settings.pdfFont, settings.pdfEnglishFont).take(40),
                "dpeLogo" to if (settings.dpeLogo == "color") "color" else "simple",
                "customFields" to settings.customFields.take(30).map { mapOf("key" to it.key.take(60), "label" to it.label.take(120)) },
                "customClasses" to settings.customClasses.take(15).map {
                    mapOf(
                        "id" to it.id.coerceIn(6, 20),
                        "nameBn" to it.nameBn.trim().take(40),
                        "shortBn" to it.shortBn.trim().take(12),
                        "syllabusTier" to if (it.syllabusTier <= 2) 1 else 3
                    )
                },
                "resultRules" to settings.resultRules.toFirestoreMap(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val codePayload = mapOf(
                "schoolId" to schoolId,
                "schoolName" to cleanName,
                "ownerUid" to uid,
                "adminUids" to listOf(uid),
                "approvalRequired" to true,
                "active" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val userRef = db.collection("users").document(uid)
            val userSnap = userRef.get().await()

            val batch = db.batch()
            batch.set(db.collection("schools").document(schoolId), schoolPayload)
            batch.set(db.collection("schoolCodes").document(joinCode), codePayload)
            if (!userSnap.exists()) {
                batch.set(
                    userRef,
                    mapOf(
                        "uid" to uid,
                        "displayName" to creatorName,
                        "email" to creatorEmail,
                        "activeSchoolId" to schoolId,
                        "pendingSchoolId" to "",
                        "pendingSchoolName" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.update(
                    userRef,
                    mapOf(
                        "activeSchoolId" to schoolId,
                        "pendingSchoolId" to "",
                        "pendingSchoolName" to "",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()

            if (initialStudents.isNotEmpty()) {
                writeStudentsInChunks(schoolId, initialStudents, uid, creatorName)
            }

            Result.success(
                SchoolWorkspace(
                    id = schoolId,
                    ownerUid = uid,
                    joinCode = joinCode,
                    approvalRequired = true,
                    memberUids = listOf(uid),
                    adminUids = listOf(uid),
                    members = listOf(SchoolMemberProfile(uid, creatorName, creatorEmail, MemberRole.ADMIN, adminPerms)),
                    settings = settings
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun lookupSchoolCode(rawCode: String): Result<SchoolCodePreview?> {
        val code = rawCode.trim().uppercase().replace(Regex("[^A-Z0-9]"), "")
        if (code.length < 6 || code.length > 12) {
            return Result.failure(IllegalArgumentException("৬ থেকে ১২ অক্ষরের সঠিক স্কুল কোড লিখুন"))
        }
        val path = "schoolCodes/$code"
        return try {
            requireUserId()
            val snap = db.collection("schoolCodes").document(code).get().await()
            if (!snap.exists() || snap.getBoolean("active") == false) {
                Result.success(null)
            } else {
                Result.success(
                    SchoolCodePreview(
                        code = code,
                        schoolId = snap.getString("schoolId").orEmpty(),
                        schoolName = snap.getString("schoolName").orEmpty(),
                        ownerUid = snap.getString("ownerUid").orEmpty(),
                        adminUids = (snap.get("adminUids") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        approvalRequired = snap.getBoolean("approvalRequired") ?: false,
                        active = snap.getBoolean("active") ?: true
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun joinSchoolWithCode(preview: SchoolCodePreview): Result<Boolean> {
        // Always requires Admin approval; returns false indicating pending request sent.
        var path = "schools/${preview.schoolId}/joinRequests"
        return try {
            val uid = requireUserId()
            val name = currentUserName().take(100)
            val email = currentUserEmail().take(150)

            // Enforce 1 school per user
            val existingMemberships = db.collection("schools")
                .whereArrayContains("memberUids", uid)
                .limit(1)
                .get()
                .await()
            if (!existingMemberships.isEmpty) {
                return Result.failure(IllegalStateException("একজন ব্যবহারকারী শুধুমাত্র একটি বিদ্যালয়ে যুক্ত থাকতে পারবেন।"))
            }

            val reqPath = "schools/${preview.schoolId}/joinRequests/$uid"
            path = reqPath
            val reqRef = db.collection("schools").document(preview.schoolId)
                .collection("joinRequests").document(uid)
            val userRef = db.collection("users").document(uid)
            val existingReq = runCatching { reqRef.get().await() }.getOrNull()
            val userSnap = userRef.get().await()

            val batch = db.batch()
            if (existingReq == null || !existingReq.exists()) {
                val reqPayload = mapOf(
                    "uid" to uid,
                    "displayName" to name,
                    "email" to email,
                    "schoolId" to preview.schoolId,
                    "schoolName" to preview.schoolName.take(160),
                    "schoolAdminUids" to preview.adminUids,
                    "status" to "PENDING",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(reqRef, reqPayload)
            }
            if (!userSnap.exists()) {
                batch.set(
                    userRef,
                    mapOf(
                        "uid" to uid,
                        "displayName" to name,
                        "email" to email,
                        "activeSchoolId" to "",
                        "pendingSchoolId" to preview.schoolId,
                        "pendingSchoolName" to preview.schoolName.take(160),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.update(
                    userRef,
                    mapOf(
                        "pendingSchoolId" to preview.schoolId,
                        "pendingSchoolName" to preview.schoolName.take(160),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun approveJoinRequest(
        schoolId: String,
        req: JoinRequestItem,
        permissions: TeacherPermissions = TeacherPermissions.fullAccess()
    ): Result<Unit> {
        val path = "schools/$schoolId"
        return try {
            requireUserId()
            val memberProfileMap = mapOf(
                "uid" to req.uid,
                "name" to req.displayName.take(100),
                "email" to req.email.take(150),
                "role" to MemberRole.TEACHER.code
            ) + permissions.toFirestoreMap()

            val batch = db.batch()
            batch.update(
                db.collection("schools").document(schoolId),
                mapOf(
                    "memberUids" to FieldValue.arrayUnion(req.uid),
                    "memberProfiles.${req.uid}" to memberProfileMap,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.delete(
                db.collection("schools").document(schoolId)
                    .collection("joinRequests").document(req.uid)
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun rejectJoinRequest(schoolId: String, requestUid: String): Result<Unit> {
        val path = "schools/$schoolId/joinRequests/$requestUid"
        return try {
            requireUserId()
            db.collection("schools").document(schoolId)
                .collection("joinRequests").document(requestUid)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun regenerateJoinCode(school: SchoolWorkspace): Result<String> {
        val path = "schools/${school.id}"
        return try {
            requireUserId()
            val newCode = allocateUniqueJoinCode()
            val batch = db.batch()
            if (school.joinCode.isNotBlank()) {
                batch.delete(db.collection("schoolCodes").document(school.joinCode))
            }
            batch.set(
                db.collection("schoolCodes").document(newCode),
                mapOf(
                    "schoolId" to school.id,
                    "schoolName" to school.settings.school.ifBlank { "বিদ্যালয়" }.take(160),
                    "ownerUid" to school.ownerUid,
                    "adminUids" to school.adminUids,
                    "approvalRequired" to school.approvalRequired,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("schools").document(school.id),
                mapOf(
                    "joinCode" to newCode,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(newCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun setApprovalRequired(school: SchoolWorkspace, required: Boolean): Result<Unit> {
        val path = "schools/${school.id}"
        return try {
            requireUserId()
            val batch = db.batch()
            batch.update(
                db.collection("schools").document(school.id),
                mapOf("approvalRequired" to required, "updatedAt" to FieldValue.serverTimestamp())
            )
            if (school.joinCode.isNotBlank()) {
                batch.update(
                    db.collection("schoolCodes").document(school.joinCode),
                    mapOf("approvalRequired" to required, "updatedAt" to FieldValue.serverTimestamp())
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateMemberPermissions(
        school: SchoolWorkspace,
        targetUid: String,
        permissions: TeacherPermissions
    ): Result<Unit> {
        val path = "schools/${school.id}"
        return try {
            requireUserId()
            val existingMember = school.members.find { it.uid == targetUid }
                ?: return Result.failure(IllegalArgumentException("সদস্য পাওয়া যায়নি"))
            val updatedProfile = mapOf(
                "uid" to existingMember.uid,
                "name" to existingMember.name,
                "email" to existingMember.email,
                "role" to existingMember.role.code
            ) + permissions.toFirestoreMap()
            db.collection("schools").document(school.id)
                .update(
                    mapOf(
                        "memberProfiles.$targetUid" to updatedProfile,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateMemberRole(school: SchoolWorkspace, targetUid: String, role: MemberRole): Result<Unit> {
        val path = "schools/${school.id}"
        return try {
            requireUserId()
            require(targetUid != school.ownerUid) { "প্রতিষ্ঠাতার রোল পরিবর্তন করা যাবে না" }
            val nextAdmins = if (role == MemberRole.ADMIN) {
                (school.adminUids + targetUid).distinct().take(20)
            } else {
                school.adminUids.filter { it != targetUid }
            }
            val updates = mutableMapOf<String, Any>(
                "adminUids" to nextAdmins,
                "memberProfiles.$targetUid.role" to role.code,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (role == MemberRole.ADMIN) {
                val existingMember = school.members.find { it.uid == targetUid }
                if (existingMember != null) {
                    updates["memberProfiles.$targetUid"] = mapOf(
                        "uid" to existingMember.uid,
                        "name" to existingMember.name,
                        "email" to existingMember.email,
                        "role" to role.code
                    ) + TeacherPermissions.fullAccess().toFirestoreMap()
                }
            }
            val batch = db.batch()
            batch.update(db.collection("schools").document(school.id), updates)
            if (school.joinCode.isNotBlank()) {
                batch.update(
                    db.collection("schoolCodes").document(school.joinCode),
                    mapOf("adminUids" to nextAdmins, "updatedAt" to FieldValue.serverTimestamp())
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeMember(school: SchoolWorkspace, targetUid: String): Result<Unit> {
        val path = "schools/${school.id}"
        return try {
            requireUserId()
            require(targetUid != school.ownerUid) { "প্রতিষ্ঠাতাকে অপসারণ করা যাবে না" }
            val nextAdmins = school.adminUids.filter { it != targetUid }
            val batch = db.batch()
            batch.update(
                db.collection("schools").document(school.id),
                mapOf(
                    "memberUids" to FieldValue.arrayRemove(targetUid),
                    "adminUids" to nextAdmins,
                    "memberProfiles.$targetUid" to FieldValue.delete(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (school.joinCode.isNotBlank()) {
                batch.update(
                    db.collection("schoolCodes").document(school.joinCode),
                    mapOf("adminUids" to nextAdmins, "updatedAt" to FieldValue.serverTimestamp())
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun leaveSchool(school: SchoolWorkspace): Result<Unit> {
        val path = "schools/${school.id}"
        return try {
            val uid = requireUserId()
            require(uid != school.ownerUid) { "প্রতিষ্ঠাতা বিদ্যালয় ত্যাগ করতে পারবেন না" }
            val batch = db.batch()
            batch.update(
                db.collection("schools").document(school.id),
                mapOf(
                    "memberUids" to FieldValue.arrayRemove(uid),
                    "adminUids" to FieldValue.arrayRemove(uid),
                    "memberProfiles.$uid" to FieldValue.delete(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "activeSchoolId" to "",
                    "pendingSchoolId" to "",
                    "pendingSchoolName" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSchoolSettings(school: SchoolWorkspace, nextSettings: Settings): Result<Unit> {
        val path = "schools/${school.id}"
        return try {
            requireUserId()
            val prev = school.settings
            val cleanSchoolName = nextSettings.school.trim().ifBlank { "প্রাথমিক বিদ্যালয়" }.take(160)
            val cleanPrevSchoolName = prev.school.trim().ifBlank { "প্রাথমিক বিদ্যালয়" }.take(160)
            val cleanAddress = nextSettings.schoolAddress.trim().take(240)
            val cleanPrevAddress = prev.schoolAddress.trim().take(240)
            val cleanYear = nextSettings.year.trim().ifBlank { "২০২৬" }.take(40)
            val cleanPrevYear = prev.year.trim().ifBlank { "২০২৬" }.take(40)
            val cleanPdfFont = com.kgps.result.encodePdfFontPair(nextSettings.pdfFont, nextSettings.pdfEnglishFont).take(40)
            val cleanPrevPdfFont = com.kgps.result.encodePdfFontPair(prev.pdfFont, prev.pdfEnglishFont).take(40)
            val cleanLogo = if (nextSettings.dpeLogo == "color") "color" else "simple"
            val cleanPrevLogo = if (prev.dpeLogo == "color") "color" else "simple"

            val nextTeachers = nextSettings.teachers.entries.take(25).associate { it.key.toString() to it.value.trim().take(120) }
            val prevTeachers = prev.teachers.entries.take(25).associate { it.key.toString() to it.value.trim().take(120) }
            val nextMobiles = nextSettings.teacherMobiles.entries.take(25).associate { it.key.toString() to it.value.trim().take(30) }
            val prevMobiles = prev.teacherMobiles.entries.take(25).associate { it.key.toString() to it.value.trim().take(30) }
            val nextFields = nextSettings.customFields.take(30).map { mapOf("key" to it.key.take(60), "label" to it.label.take(120)) }
            val prevFields = prev.customFields.take(30).map { mapOf("key" to it.key.take(60), "label" to it.label.take(120)) }
            val nextClasses = nextSettings.customClasses.take(15).map {
                mapOf(
                    "id" to it.id.coerceIn(6, 20),
                    "nameBn" to it.nameBn.trim().take(40),
                    "shortBn" to it.shortBn.trim().take(12),
                    "syllabusTier" to if (it.syllabusTier <= 2) 1 else 3
                )
            }
            val prevClasses = prev.customClasses.take(15).map {
                mapOf(
                    "id" to it.id.coerceIn(6, 20),
                    "nameBn" to it.nameBn.trim().take(40),
                    "shortBn" to it.shortBn.trim().take(12),
                    "syllabusTier" to if (it.syllabusTier <= 2) 1 else 3
                )
            }

            val nextRules = nextSettings.resultRules.toFirestoreMap()
            val prevRules = prev.resultRules.toFirestoreMap()

            val updates = mutableMapOf<String, Any>()
            if (cleanSchoolName != cleanPrevSchoolName) updates["school"] = cleanSchoolName
            if (cleanAddress != cleanPrevAddress) updates["schoolAddress"] = cleanAddress
            if (cleanYear != cleanPrevYear) updates["year"] = cleanYear
            if (nextTeachers != prevTeachers) updates["teachers"] = nextTeachers
            if (nextMobiles != prevMobiles) updates["teacherMobiles"] = nextMobiles
            if (cleanPdfFont != cleanPrevPdfFont) updates["pdfFont"] = cleanPdfFont
            if (cleanLogo != cleanPrevLogo) updates["dpeLogo"] = cleanLogo
            if (nextFields != prevFields) updates["customFields"] = nextFields
            if (nextClasses != prevClasses) updates["customClasses"] = nextClasses
            if (nextRules != prevRules) updates["resultRules"] = nextRules
            if (updates.isEmpty()) return Result.success(Unit)
            updates["updatedAt"] = FieldValue.serverTimestamp()

            val batch = db.batch()
            batch.update(db.collection("schools").document(school.id), updates)
            if (school.joinCode.isNotBlank() && cleanSchoolName != cleanPrevSchoolName) {
                batch.update(
                    db.collection("schoolCodes").document(school.joinCode),
                    mapOf("schoolName" to cleanSchoolName, "updatedAt" to FieldValue.serverTimestamp())
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSchoolResultRules(
        school: SchoolWorkspace,
        nextRules: com.kgps.result.ResultEvaluationRules
    ): Result<Unit> {
        val path = "schools/${school.id}"
        return try {
            requireUserId()
            db.collection("schools").document(school.id)
                .update(
                    mapOf(
                        "resultRules" to nextRules.toFirestoreMap(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // ---------- School Students (Realtime + Concurrent Field-Level Sync) ----------

    fun observeSchoolStudents(schoolId: String): Flow<Pair<List<Student>, Map<String, StudentSyncMeta>>> = flow {
        val uid = requireUserId()
        require(schoolId.isNotBlank()) { "School ID required" }
        val path = "schools/$schoolId/students"
        emitAll(
            db.collection("schools").document(schoolId).collection("students")
                .snapshots()
                .map { snap ->
                    val list = ArrayList<Student>(snap.documents.size)
                    val meta = HashMap<String, StudentSyncMeta>(snap.documents.size)
                    for (doc in snap.documents) {
                        val (st, m) = parseStudentDoc(doc)
                        list.add(st)
                        meta[st.id] = m
                    }
                    list.sortedWith(compareBy({ it.cls }, { it.roll })) to meta
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreException(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun upsertStudent(
        schoolId: String,
        student: Student,
        existing: Student?
    ): Result<Unit> {
        val docId = sanitizeDocId(student.id)
        val path = "schools/$schoolId/students/$docId"
        return try {
            val uid = requireUserId()
            val name = currentUserName().take(100)
            val docRef = db.collection("schools").document(schoolId).collection("students").document(docId)

            if (existing == null) {
                docRef.set(buildStudentCreatePayload(student, uid, name)).await()
            } else {
                val updates = StudentSyncDiffHelper.buildUpdateDiff(
                    prev = existing,
                    next = student,
                    uid = uid,
                    userName = name,
                    deleteSentinel = FieldValue.delete(),
                    updatedAtValue = FieldValue.serverTimestamp()
                )
                if (updates != null) {
                    docRef.update(updates).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun patchStudentField(
        schoolId: String,
        studentId: String,
        fieldPath: String,
        value: Any?
    ): Result<Unit> {
        val docId = sanitizeDocId(studentId)
        val path = "schools/$schoolId/students/$docId"
        return try {
            val uid = requireUserId()
            val name = currentUserName().take(100)
            val actualFieldPath = if (fieldPath == "union") "x.UNION" else fieldPath
            val safeValue: Any = when {
                fieldPath == "union" && (value as? String).isNullOrBlank() -> FieldValue.delete()
                value == null -> FieldValue.delete()
                fieldPath == "mobile" && value is String -> value.take(30)
                fieldPath == "religion" && value is String -> value.ifBlank { "ইসলাম" }.take(40)
                fieldPath == "upazila" && value is String -> value.ifBlank { com.kgps.result.DEFAULT_UPAZILA }.take(120)
                fieldPath == "district" && value is String -> value.ifBlank { com.kgps.result.DEFAULT_DISTRICT }.take(120)
                value is String -> value.take(240)
                else -> value
            }
            val updates = mapOf<String, Any>(
                actualFieldPath to safeValue,
                "updatedByUid" to uid,
                "updatedByName" to name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("schools").document(schoolId)
                .collection("students").document(docId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun patchBatchXFields(
        schoolId: String,
        studentIds: Collection<String>,
        xUpdates: Map<String, String?>
    ): Result<Unit> {
        val path = "schools/$schoolId/students"
        if (studentIds.isEmpty() || xUpdates.isEmpty()) return Result.success(Unit)
        return try {
            val uid = requireUserId()
            val name = currentUserName().take(100)
            val col = db.collection("schools").document(schoolId).collection("students")
            val payload = mutableMapOf<String, Any>(
                "updatedByUid" to uid,
                "updatedByName" to name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            xUpdates.forEach { (k, v) ->
                val cleanKey = k.trim()
                if (cleanKey.isNotEmpty()) {
                    payload["x.$cleanKey"] = if (v.isNullOrBlank()) {
                        FieldValue.delete()
                    } else {
                        v.trim().take(240)
                    }
                }
            }
            studentIds.map { sanitizeDocId(it) }.distinct().chunked(380).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { docId ->
                    batch.update(col.document(docId), payload)
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun patchBatchPerStudentXField(
        schoolId: String,
        xKey: String,
        valuesByStudentId: Map<String, String?>
    ): Result<Unit> {
        val cleanKey = xKey.trim()
        val path = "schools/$schoolId/students"
        if (cleanKey.isEmpty() || valuesByStudentId.isEmpty()) return Result.success(Unit)
        return try {
            val uid = requireUserId()
            val name = currentUserName().take(100)
            val col = db.collection("schools").document(schoolId).collection("students")
            valuesByStudentId.entries.chunked(380).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { (studentId, rawVal) ->
                    val docId = sanitizeDocId(studentId)
                    val fieldValue: Any = if (rawVal.isNullOrBlank()) {
                        FieldValue.delete()
                    } else {
                        rawVal.trim().take(240)
                    }
                    val payload = mapOf<String, Any>(
                        "x.$cleanKey" to fieldValue,
                        "updatedByUid" to uid,
                        "updatedByName" to name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.update(col.document(docId), payload)
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteStudent(schoolId: String, studentId: String): Result<Unit> {
        val docId = sanitizeDocId(studentId)
        val path = "schools/$schoolId/students/$docId"
        return try {
            requireUserId()
            db.collection("schools").document(schoolId)
                .collection("students").document(docId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAllSchoolStudents(schoolId: String, studentIds: List<String>): Result<Unit> {
        val path = "schools/$schoolId/students"
        return try {
            requireUserId()
            val col = db.collection("schools").document(schoolId).collection("students")
            studentIds.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { rawId -> batch.delete(col.document(sanitizeDocId(rawId))) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun syncBatchStudents(
        schoolId: String,
        previousList: List<Student>,
        nextList: List<Student>,
        deleteRemoved: Boolean = false
    ): Result<Unit> {
        val path = "schools/$schoolId/students"
        return try {
            val uid = requireUserId()
            val name = currentUserName().take(100)
            val prevMap = previousList.associateBy { sanitizeDocId(it.id) }
            val nextMap = nextList.associateBy { sanitizeDocId(it.id) }
            val col = db.collection("schools").document(schoolId).collection("students")

            val ops = mutableListOf<Pair<String, Map<String, Any>?>>() // null payload = delete
            for ((docId, st) in nextMap) {
                val prev = prevMap[docId]
                if (prev == null) {
                    ops.add(docId to buildStudentCreatePayload(st, uid, name))
                } else {
                    val diff = StudentSyncDiffHelper.buildUpdateDiff(
                        prev = prev,
                        next = st,
                        uid = uid,
                        userName = name,
                        deleteSentinel = FieldValue.delete(),
                        updatedAtValue = FieldValue.serverTimestamp()
                    )
                    if (diff != null) {
                        ops.add(docId to diff)
                    }
                }
            }
            if (deleteRemoved) {
                for (oldId in prevMap.keys) {
                    if (!nextMap.containsKey(oldId)) {
                        ops.add(oldId to null)
                    }
                }
            }

            ops.chunked(380).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { (docId, payload) ->
                    val ref = col.document(docId)
                    if (payload == null) batch.delete(ref)
                    else if (!prevMap.containsKey(docId)) batch.set(ref, payload)
                    else batch.update(ref, payload)
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // ---------- Helpers ----------

    private suspend fun writeStudentsInChunks(
        schoolId: String,
        students: List<Student>,
        uid: String,
        userName: String
    ) {
        val col = db.collection("schools").document(schoolId).collection("students")
        students.chunked(380).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { st ->
                val docId = sanitizeDocId(st.id)
                batch.set(col.document(docId), buildStudentCreatePayload(st, uid, userName))
            }
            batch.commit().await()
        }
    }

    private fun sanitizeDocId(rawId: String): String {
        val clean = rawId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        return clean.ifBlank { "st_" + UUID.randomUUID().toString().replace("-", "").take(16) }
    }

    private fun buildStudentCreatePayload(s: Student, uid: String, userName: String): Map<String, Any> =
        StudentSyncDiffHelper.buildCreatePayload(
            s = s,
            uid = uid,
            userName = userName,
            createdAtValue = FieldValue.serverTimestamp(),
            updatedAtValue = FieldValue.serverTimestamp()
        )

    private fun parseSchoolDoc(doc: DocumentSnapshot): SchoolWorkspace? {
        if (!doc.exists()) return null
        val ownerUid = doc.getString("ownerUid").orEmpty()
        val memberUids = (doc.get("memberUids") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val adminUids = (doc.get("adminUids") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

        val profilesMap = doc.get("memberProfiles") as? Map<*, *> ?: emptyMap<Any, Any>()
        val members = profilesMap.entries.mapNotNull { (k, v) ->
            val mUid = k as? String ?: return@mapNotNull null
            val m = v as? Map<*, *> ?: return@mapNotNull null
            val role = MemberRole.fromCode(m["role"] as? String)
            val perms = if (role == MemberRole.ADMIN) {
                TeacherPermissions.fullAccess()
            } else {
                TeacherPermissions.fromMap(m)
            }
            SchoolMemberProfile(
                uid = (m["uid"] as? String) ?: mUid,
                name = (m["name"] as? String).orEmpty(),
                email = (m["email"] as? String).orEmpty(),
                role = role,
                permissions = perms
            )
        }.sortedWith(compareBy({ it.role != MemberRole.ADMIN }, { it.name }))

        val teachersRaw = doc.get("teachers") as? Map<*, *> ?: emptyMap<Any, Any>()
        val teachers = teachersRaw.entries.mapNotNull { (k, v) ->
            val cls = (k as? String)?.toIntOrNull() ?: return@mapNotNull null
            val name = v as? String ?: return@mapNotNull null
            cls to name
        }.toMap()

        val mobilesRaw = doc.get("teacherMobiles") as? Map<*, *> ?: emptyMap<Any, Any>()
        val teacherMobiles = mobilesRaw.entries.mapNotNull { (k, v) ->
            val cls = (k as? String)?.toIntOrNull() ?: return@mapNotNull null
            val num = v as? String ?: return@mapNotNull null
            cls to num
        }.toMap()

        val cfRaw = doc.get("customFields") as? List<*> ?: emptyList<Any>()
        val customFields = cfRaw.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            val key = (m["key"] as? String)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val label = (m["label"] as? String)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            CustomFieldDef(key, label)
        }

        val ccRaw = doc.get("customClasses") as? List<*> ?: emptyList<Any>()
        val customClasses = ccRaw.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            val id = (m["id"] as? Number)?.toInt()?.takeIf { it in 6..20 } ?: return@mapNotNull null
            val nameBn = (m["nameBn"] as? String)?.trim()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val shortBn = (m["shortBn"] as? String)?.trim().orEmpty()
                .ifBlank { com.kgps.result.defaultShortClassLabel(nameBn, id) }
            val tier = ((m["syllabusTier"] as? Number)?.toInt() ?: 3).let { if (it <= 2) 1 else 3 }
            com.kgps.result.CustomClassDef(id = id, nameBn = nameBn, shortBn = shortBn, syllabusTier = tier)
        }.distinctBy { it.id }.sortedBy { it.id }

        val dpeLogo = if (doc.getString("dpeLogo") == "color") "color" else "simple"
        val decodedFonts = com.kgps.result.decodePdfFontPair(doc.getString("pdfFont"))
        val resultRules = com.kgps.result.ResultEvaluationRules.fromMap(doc.get("resultRules") as? Map<*, *>)
        val settings = Settings(
            school = doc.getString("school").orEmpty(),
            schoolAddress = doc.getString("schoolAddress").orEmpty(),
            year = doc.getString("year") ?: "২০২৬",
            teachers = teachers,
            teacherMobiles = teacherMobiles,
            pdfFont = decodedFonts.bengaliFontId,
            pdfEnglishFont = decodedFonts.englishFontId,
            dpeLogo = dpeLogo,
            customFields = customFields,
            customClasses = customClasses,
            resultRules = resultRules
        )

        return SchoolWorkspace(
            id = doc.id,
            ownerUid = ownerUid,
            joinCode = doc.getString("joinCode").orEmpty(),
            approvalRequired = doc.getBoolean("approvalRequired") ?: false,
            memberUids = memberUids,
            adminUids = adminUids,
            members = members,
            settings = settings,
            updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    private fun parseStudentDoc(doc: DocumentSnapshot): Pair<Student, StudentSyncMeta> {
        val mRaw = doc.get("m") as? Map<*, *> ?: emptyMap<Any, Any>()
        val m = mRaw.entries.mapNotNull { (k, v) ->
            val key = k as? String ?: return@mapNotNull null
            val num = (v as? Number)?.toDouble() ?: return@mapNotNull null
            key to num
        }.toMap()

        val xRaw = doc.get("x") as? Map<*, *> ?: emptyMap<Any, Any>()
        val xAll = xRaw.entries.mapNotNull { (k, v) ->
            val key = k as? String ?: return@mapNotNull null
            val valStr = v as? String ?: return@mapNotNull null
            key to valStr
        }.toMap()
        val unionVal = xAll["UNION"].orEmpty().trim()
        val x = xAll - "UNION"

        val st = Student(
            id = doc.id,
            cls = ((doc.get("cls") as? Number)?.toInt() ?: 1).coerceIn(1, 20),
            roll = ((doc.get("roll") as? Number)?.toInt() ?: 1).coerceAtLeast(1),
            name = doc.getString("name").orEmpty(),
            guardian = doc.getString("guardian").orEmpty(),
            village = doc.getString("village").orEmpty(),
            union = unionVal,
            post = doc.getString("post").orEmpty(),
            upazila = doc.getString("upazila").orEmpty().ifBlank { com.kgps.result.DEFAULT_UPAZILA },
            district = doc.getString("district").orEmpty().ifBlank { com.kgps.result.DEFAULT_DISTRICT },
            mobile = doc.getString("mobile").orEmpty(),
            religion = doc.getString("religion")?.ifBlank { "ইসলাম" } ?: "ইসলাম",
            m = m,
            x = x
        )
        val meta = StudentSyncMeta(
            updatedByUid = doc.getString("updatedByUid").orEmpty(),
            updatedByName = doc.getString("updatedByName").orEmpty(),
            createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
            updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
        return st to meta
    }

    // ---------- Smart Blank Sheet Templates ----------

    suspend fun saveSheetTemplate(
        schoolId: String,
        meta: BlankSheetQrMeta
    ): Result<String> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("টেমপ্লেট সংরক্ষণের আগে Google দিয়ে সাইন-ইন করতে হবে।"))
        val cleanSchoolId = schoolId.trim()
        if (cleanSchoolId.isBlank()) {
            return Result.failure(IllegalArgumentException("School ID পাওয়া যায়নি"))
        }
        val templateId = meta.templateId.trim().uppercase().ifBlank {
            SheetQrCodec.computeTemplateId(meta)
        }.take(24)
        val path = "schools/$cleanSchoolId/sheetTemplates/$templateId"
        return try {
            val docRef = db.collection("schools").document(cleanSchoolId)
                .collection("sheetTemplates").document(templateId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                return Result.success(templateId)
            }
            val payload = mapOf(
                "templateId" to templateId,
                "cls" to meta.cls.coerceIn(1, 20),
                "subjectKey" to (meta.subjectKey?.trim()?.take(24).orEmpty()),
                "term" to meta.term.coerceIn(-1, 3),
                "sheetKind" to meta.sheetKind.name,
                "importScope" to meta.importScope.name,
                "columnIds" to meta.columnIds.take(30),
                "customColumnTitles" to meta.customColumnTitles.map { it.trim().take(40) }.take(15),
                "numeralStyle" to meta.numeralStyle.name,
                "showSubjectMaxInHeader" to meta.showSubjectMaxInHeader,
                "workingDaysByTerm" to meta.workingDaysByTerm.entries.take(3).associate { (k, v) -> k.toString() to v.take(10) },
                "pageIndex" to meta.pageIndex.coerceIn(1, 100),
                "totalPages" to meta.totalPages.coerceIn(1, 100),
                "refCode" to meta.refCode.trim().take(64),
                "createdByUid" to uid,
                "updatedByUid" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(templateId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun fetchSheetTemplate(
        schoolId: String,
        templateId: String
    ): Result<BlankSheetQrMeta?> {
        val cleanSchoolId = schoolId.trim()
        val cleanTid = templateId.trim().uppercase()
        if (cleanSchoolId.isBlank() || cleanTid.isBlank()) return Result.success(null)
        val path = "schools/$cleanSchoolId/sheetTemplates/$cleanTid"
        return try {
            val snap = db.collection("schools").document(cleanSchoolId)
                .collection("sheetTemplates").document(cleanTid)
                .get()
                .await()
            if (!snap.exists()) {
                Result.success(null)
            } else {
                Result.success(parseSheetTemplateDoc(snap))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun fetchAllSheetTemplates(
        schoolId: String,
        limitCount: Long = 200L
    ): Result<List<BlankSheetQrMeta>> {
        if (auth.currentUser == null) return Result.success(emptyList())
        val cleanSchoolId = schoolId.trim()
        if (cleanSchoolId.isBlank()) return Result.success(emptyList())
        val path = "schools/$cleanSchoolId/sheetTemplates"
        return try {
            val snap = db.collection("schools").document(cleanSchoolId)
                .collection("sheetTemplates")
                .limit(limitCount.coerceIn(1L, 250L))
                .get()
                .await()
            Result.success(snap.documents.mapNotNull { parseSheetTemplateDoc(it) })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    private fun parseSheetTemplateDoc(doc: DocumentSnapshot): BlankSheetQrMeta? {
        val cls = (doc.get("cls") as? Number)?.toInt()?.takeIf { it in 1..20 } ?: return null
        val sub = doc.getString("subjectKey")?.trim()?.takeIf { it.isNotEmpty() }
        val term = ((doc.get("term") as? Number)?.toInt() ?: 1).coerceIn(-1, 3)
        val kind = runCatching {
            ClassSheetKind.valueOf(doc.getString("sheetKind").orEmpty())
        }.getOrDefault(ClassSheetKind.SINGLE_TERM_MARKS)
        val scope = runCatching {
            ClassSheetImportScope.valueOf(doc.getString("importScope").orEmpty())
        }.getOrDefault(ClassSheetImportScope.AUTO)
        val numStyle = runCatching {
            BlankNumeralStyle.valueOf(doc.getString("numeralStyle").orEmpty())
        }.getOrDefault(BlankNumeralStyle.BENGALI)

        val colIds = (doc.get("columnIds") as? List<*>)
            ?.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotBlank) }
            .orEmpty()
        if (colIds.isEmpty()) return null

        val customTitles = (doc.get("customColumnTitles") as? List<*>)
            ?.mapNotNull { it?.toString()?.trim() }
            .orEmpty()

        val wdRaw = doc.get("workingDaysByTerm") as? Map<*, *> ?: emptyMap<Any, Any>()
        val wdMap = linkedMapOf<Int, String>()
        wdRaw.forEach { (k, v) ->
            val t = k?.toString()?.trim()?.toIntOrNull()
            val d = v?.toString()?.trim()?.filter { it.isDigit() }
            if (t in 1..3 && !d.isNullOrEmpty()) wdMap[t!!] = d
        }

        return BlankSheetQrMeta(
            version = 2,
            templateId = doc.getString("templateId")?.trim()?.uppercase().orEmpty().ifBlank { doc.id },
            cls = cls,
            subjectKey = sub,
            term = term,
            sheetKind = kind,
            importScope = scope,
            columnIds = colIds,
            customColumnTitles = customTitles,
            numeralStyle = numStyle,
            showSubjectMaxInHeader = doc.getBoolean("showSubjectMaxInHeader") ?: true,
            workingDaysByTerm = wdMap,
            pageIndex = ((doc.get("pageIndex") as? Number)?.toInt() ?: 1).coerceAtLeast(1),
            totalPages = ((doc.get("totalPages") as? Number)?.toInt() ?: 1).coerceAtLeast(1),
            refCode = doc.getString("refCode").orEmpty()
        )
    }

    private fun unwrapFirestoreException(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
}

object StudentSyncDiffHelper {
    private const val MAX_MARKS_ENTRIES = 200
    private const val MAX_X_ENTRIES = 120

    fun normalizeMarksForCloud(marks: Map<String, Double>): Map<String, Double> =
        marks.entries
            .asSequence()
            .filter { (k, v) -> k.isNotBlank() && !k.contains('.') && v.isFinite() }
            .take(MAX_MARKS_ENTRIES)
            .associate { it.key to it.value }

    fun normalizeXForCloud(x: Map<String, String>, union: String): Map<String, String> {
        val cleanUnion = union.trim().take(120)
        val nonUnionLimit = if (cleanUnion.isNotEmpty()) MAX_X_ENTRIES - 1 else MAX_X_ENTRIES
        val out = LinkedHashMap<String, String>()
        x.entries
            .asSequence()
            .filter { (k, _) -> k.isNotBlank() && !k.contains('.') && k != "UNION" }
            .take(nonUnionLimit)
            .forEach { (k, v) -> out[k] = v.take(240) }
        if (cleanUnion.isNotEmpty()) {
            out["UNION"] = cleanUnion
        }
        return out
    }

    fun buildCreatePayload(
        s: Student,
        uid: String,
        userName: String,
        createdAtValue: Any,
        updatedAtValue: Any
    ): Map<String, Any> = mapOf(
        "cls" to s.cls.coerceIn(1, 20),
        "roll" to s.roll.coerceIn(1, 9999),
        "name" to s.name.take(120),
        "guardian" to s.guardian.take(120),
        "village" to s.village.take(120),
        "post" to s.post.take(120),
        "upazila" to s.upazila.ifBlank { com.kgps.result.DEFAULT_UPAZILA }.take(120),
        "district" to s.district.ifBlank { com.kgps.result.DEFAULT_DISTRICT }.take(120),
        "mobile" to s.mobile.take(30),
        "religion" to s.religion.ifBlank { "ইসলাম" }.take(40),
        "m" to normalizeMarksForCloud(s.m),
        "x" to normalizeXForCloud(s.x, s.union),
        "updatedByUid" to uid,
        "updatedByName" to userName.take(100),
        "createdAt" to createdAtValue,
        "updatedAt" to updatedAtValue
    )

    fun buildUpdateDiff(
        prev: Student,
        next: Student,
        uid: String,
        userName: String,
        deleteSentinel: Any,
        updatedAtValue: Any
    ): Map<String, Any>? {
        val updates = LinkedHashMap<String, Any>()

        val prevCls = prev.cls.coerceIn(1, 20)
        val nextCls = next.cls.coerceIn(1, 20)
        if (prevCls != nextCls) updates["cls"] = nextCls

        val prevRoll = prev.roll.coerceIn(1, 9999)
        val nextRoll = next.roll.coerceIn(1, 9999)
        if (prevRoll != nextRoll) updates["roll"] = nextRoll

        if (prev.name.take(120) != next.name.take(120)) updates["name"] = next.name.take(120)
        if (prev.guardian.take(120) != next.guardian.take(120)) updates["guardian"] = next.guardian.take(120)
        if (prev.village.take(120) != next.village.take(120)) updates["village"] = next.village.take(120)
        if (prev.post.take(120) != next.post.take(120)) updates["post"] = next.post.take(120)

        val prevUpazila = prev.upazila.ifBlank { com.kgps.result.DEFAULT_UPAZILA }.take(120)
        val nextUpazila = next.upazila.ifBlank { com.kgps.result.DEFAULT_UPAZILA }.take(120)
        if (prevUpazila != nextUpazila) updates["upazila"] = nextUpazila

        val prevDistrict = prev.district.ifBlank { com.kgps.result.DEFAULT_DISTRICT }.take(120)
        val nextDistrict = next.district.ifBlank { com.kgps.result.DEFAULT_DISTRICT }.take(120)
        if (prevDistrict != nextDistrict) updates["district"] = nextDistrict

        if (prev.mobile.take(30) != next.mobile.take(30)) updates["mobile"] = next.mobile.take(30)

        val prevReligion = prev.religion.ifBlank { "ইসলাম" }.take(40)
        val nextReligion = next.religion.ifBlank { "ইসলাম" }.take(40)
        if (prevReligion != nextReligion) updates["religion"] = nextReligion

        val prevM = normalizeMarksForCloud(prev.m)
        val nextM = normalizeMarksForCloud(next.m)
        for ((k, v) in nextM) {
            if (prevM[k] != v) updates["m.$k"] = v
        }
        for (k in prevM.keys) {
            if (!nextM.containsKey(k)) updates["m.$k"] = deleteSentinel
        }

        val prevX = normalizeXForCloud(prev.x, prev.union)
        val nextX = normalizeXForCloud(next.x, next.union)
        for ((k, v) in nextX) {
            if (prevX[k] != v) updates["x.$k"] = v
        }
        for (k in prevX.keys) {
            if (!nextX.containsKey(k)) updates["x.$k"] = deleteSentinel
        }

        if (updates.isEmpty()) return null

        updates["updatedByUid"] = uid
        updates["updatedByName"] = userName.take(100)
        updates["updatedAt"] = updatedAtValue
        return updates
    }
}
