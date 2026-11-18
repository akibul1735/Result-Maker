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
import com.kgps.result.CustomFieldDef
import com.kgps.result.Settings
import com.kgps.result.Student
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
                "teachers" to settings.teachers.mapKeys { it.key.toString() },
                "teacherMobiles" to settings.teacherMobiles.mapKeys { it.key.toString() },
                "pdfFont" to settings.pdfFont.ifBlank { "noto_serif" }.take(40),
                "dpeLogo" to if (settings.dpeLogo == "color") "color" else "simple",
                "customFields" to settings.customFields.take(30).map { mapOf("key" to it.key, "label" to it.label) },
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
            val cleanSchoolName = nextSettings.school.trim().ifBlank { "প্রাথমিক বিদ্যালয়" }.take(160)
            val batch = db.batch()
            batch.update(
                db.collection("schools").document(school.id),
                mapOf(
                    "school" to cleanSchoolName,
                    "schoolAddress" to nextSettings.schoolAddress.trim().take(240),
                    "year" to nextSettings.year.trim().ifBlank { "২০২৬" }.take(40),
                    "teachers" to nextSettings.teachers.mapKeys { it.key.toString() },
                    "teacherMobiles" to nextSettings.teacherMobiles.mapKeys { it.key.toString() },
                    "pdfFont" to nextSettings.pdfFont.ifBlank { "noto_serif" }.take(40),
                    "dpeLogo" to if (nextSettings.dpeLogo == "color") "color" else "simple",
                    "customFields" to nextSettings.customFields.take(30).map { mapOf("key" to it.key, "label" to it.label) },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (school.joinCode.isNotBlank() && cleanSchoolName != school.settings.school) {
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
                val updates = mutableMapOf<String, Any>(
                    "updatedByUid" to uid,
                    "updatedByName" to name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (existing.cls != student.cls) updates["cls"] = student.cls.coerceIn(1, 5)
                if (existing.roll != student.roll) updates["roll"] = student.roll.coerceIn(1, 9999)
                if (existing.name != student.name) updates["name"] = student.name.take(120)
                if (existing.guardian != student.guardian) updates["guardian"] = student.guardian.take(120)
                if (existing.village != student.village) updates["village"] = student.village.take(120)
                if (existing.post != student.post) updates["post"] = student.post.take(120)
                if (existing.upazila != student.upazila) updates["upazila"] = student.upazila.take(120)
                if (existing.district != student.district) updates["district"] = student.district.take(120)
                if (existing.mobile != student.mobile) updates["mobile"] = student.mobile.take(30)
                if (existing.religion != student.religion) updates["religion"] = student.religion.ifBlank { "ইসলাম" }.take(40)

                // Field-level diff on marks `m` so concurrent edits by other teachers on other subjects stay intact
                for ((k, v) in student.m) {
                    if (existing.m[k] != v) updates["m.$k"] = v
                }
                for (k in existing.m.keys) {
                    if (!student.m.containsKey(k)) updates["m.$k"] = FieldValue.delete()
                }
                // Field-level diff on attributes/remarks `x`
                for ((k, v) in student.x) {
                    if (existing.x[k] != v) updates["x.$k"] = v
                }
                for (k in existing.x.keys) {
                    if (!student.x.containsKey(k)) updates["x.$k"] = FieldValue.delete()
                }

                if (updates.size > 3) {
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
            val safeValue: Any = when {
                value == null -> FieldValue.delete()
                fieldPath == "mobile" && value is String -> value.take(30)
                fieldPath == "religion" && value is String -> value.ifBlank { "ইসলাম" }.take(40)
                value is String -> value.take(240)
                else -> value
            }
            val updates = mapOf<String, Any>(
                fieldPath to safeValue,
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
                } else if (prev != st) {
                    ops.add(
                        docId to mapOf(
                            "cls" to st.cls.coerceIn(1, 5),
                            "roll" to st.roll.coerceIn(1, 9999),
                            "name" to st.name.take(120),
                            "guardian" to st.guardian.take(120),
                            "village" to st.village.take(120),
                            "post" to st.post.take(120),
                            "upazila" to st.upazila.take(120),
                            "district" to st.district.take(120),
                            "mobile" to st.mobile.take(30),
                            "religion" to st.religion.ifBlank { "ইসলাম" }.take(40),
                            "m" to st.m,
                            "x" to st.x,
                            "updatedByUid" to uid,
                            "updatedByName" to name,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
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
        mapOf(
            "cls" to s.cls.coerceIn(1, 5),
            "roll" to s.roll.coerceIn(1, 9999),
            "name" to s.name.take(120),
            "guardian" to s.guardian.take(120),
            "village" to s.village.take(120),
            "post" to s.post.take(120),
            "upazila" to s.upazila.take(120),
            "district" to s.district.take(120),
            "mobile" to s.mobile.take(30),
            "religion" to s.religion.ifBlank { "ইসলাম" }.take(40),
            "m" to s.m,
            "x" to s.x,
            "updatedByUid" to uid,
            "updatedByName" to userName.take(100),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
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

        val dpeLogo = if (doc.getString("dpeLogo") == "color") "color" else "simple"
        val settings = Settings(
            school = doc.getString("school").orEmpty(),
            schoolAddress = doc.getString("schoolAddress").orEmpty(),
            year = doc.getString("year") ?: "২০২৬",
            teachers = teachers,
            teacherMobiles = teacherMobiles,
            pdfFont = doc.getString("pdfFont")?.ifBlank { "noto_serif" } ?: "noto_serif",
            dpeLogo = dpeLogo,
            customFields = customFields
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
        val x = xRaw.entries.mapNotNull { (k, v) ->
            val key = k as? String ?: return@mapNotNull null
            val valStr = v as? String ?: return@mapNotNull null
            key to valStr
        }.toMap()

        val st = Student(
            id = doc.id,
            cls = ((doc.get("cls") as? Number)?.toInt() ?: 1).coerceIn(1, 5),
            roll = ((doc.get("roll") as? Number)?.toInt() ?: 1).coerceAtLeast(1),
            name = doc.getString("name").orEmpty(),
            guardian = doc.getString("guardian").orEmpty(),
            village = doc.getString("village").orEmpty(),
            post = doc.getString("post").orEmpty(),
            upazila = doc.getString("upazila").orEmpty(),
            district = doc.getString("district").orEmpty(),
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

    private fun unwrapFirestoreException(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
}
