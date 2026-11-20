package com.kgps.result

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import com.kgps.result.ai.AiChatMessage
import com.kgps.result.ai.AiModelMode
import com.kgps.result.ai.AiRepository
import com.kgps.result.ai.DirectGeminiAiRepository
import com.kgps.result.ai.DiscoveredGeminiModel
import com.kgps.result.ai.ExtractedClassSheet
import com.kgps.result.ai.ExtractedMarksheet
import com.kgps.result.ai.GeminiApiConfig
import com.kgps.result.ai.PlaceholderAiRepository
import com.kgps.result.ai.ScanConstraintSolver
import com.kgps.result.ai.ScannedStudentRow
import com.kgps.result.ai.validateScannedStudentRow
import com.kgps.result.backup.AutoBackupManager
import com.kgps.result.backup.AutoBackupStatus
import com.kgps.result.backup.BackupScheduleConfig
import com.kgps.result.backup.BackupScheduleMode
import com.kgps.result.backup.BackupSystemKind
import com.kgps.result.backup.BackupWorkScheduler
import com.kgps.result.backup.CsvBackupFormat
import com.kgps.result.backup.DriveAuthOutcome
import com.kgps.result.backup.DriveBackupFile
import com.kgps.result.backup.GoogleDriveBackupClient
import com.kgps.result.cloud.CloudSessionState
import com.kgps.result.cloud.JoinRequestItem
import com.kgps.result.cloud.MemberRole
import com.kgps.result.cloud.SchoolCloudRepository
import com.kgps.result.cloud.SchoolCodePreview
import com.kgps.result.cloud.SchoolWorkspace
import com.kgps.result.cloud.StudentSyncMeta
import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.cloud.UserCloudProfile
import com.kgps.result.cloud.friendlyFirestoreErrorMessage
import com.kgps.result.ui.screens.BatchStudentUpdatePatch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class NavSection(val route: String, val labelBn: String) {
    ROSTER("roster", "শিক্ষার্থী তালিকা"),
    SHEET("sheet", "ব্যাচ এন্ট্রি"),
    STUDIO("studio", "রিপোর্ট স্টুডিও"),
    ASSISTANT("assistant", "AI সহায়ক"),
    SETTINGS("settings", "সেটিংস")
}

data class CsvImportModalState(
    val isOpen: Boolean = false,
    val initialText: String = ""
)

data class AiAssistantState(
    val activeTab: Int = 0,
    val modelMode: AiModelMode = AiModelMode.GENERAL,
    val contextStudentId: String? = null,
    val chatMessages: List<AiChatMessage> = listOf(
        AiChatMessage(
            id = "welcome-msg",
            isUser = false,
            text = "আসসালামু আলাইকুম! আমি আপনার শ্রেণিকক্ষ সহায়ক। নম্বর বিভাজন, অভিভাবক সভার সারসংক্ষেপ বা প্রান্তিক মতামতের খসড়া নিয়ে প্রশ্ন করতে পারেন।\n\nএছাড়া 'নম্বরপত্র ছবি স্ক্যান' ট্যাব থেকে পুরো শ্রেণির নম্বরপত্র ছবি তুলে সরাসরি প্রিভিউ দেখে ইমপোর্ট করতে পারবেন।",
            modelLabel = "Gemini AI",
            isPlaceholder = false
        )
    ),
    val chatLoading: Boolean = false,
    val chatError: String? = null,
    val selectedImageBitmap: Bitmap? = null,
    val analyzingImage: Boolean = false,
    val analysisError: String? = null,
    val analysisResult: ExtractedMarksheet? = null,
    val classSheetResult: ExtractedClassSheet? = null,
    val savingClassSheet: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
class ResultMakerViewModel(
    private val appContext: Context,
    private val cloudRepo: SchoolCloudRepository,
    private val autoBackupManager: AutoBackupManager,
    private val contentResolver: ContentResolver,
    val currentUserId: String,
    val currentUserDisplayName: String,
    val currentUserEmail: String,
    private val initialLocalSnapshot: Pair<List<Student>, Settings>? = null
) : ViewModel() {

    private val directAiRepository = DirectGeminiAiRepository(appContext)
    private val aiRepository: AiRepository get() = directAiRepository
    private val sampleAiRepository = PlaceholderAiRepository()

    val localSeedCount: Int get() = initialLocalSnapshot?.first?.size ?: 0
    val localSeedSettings: Settings? get() = initialLocalSnapshot?.second

    private val _isSigningOut = MutableStateFlow(false)
    private val _suppressedSchoolId = MutableStateFlow<String?>(null)
    private val _selectedSchoolOverrideId = MutableStateFlow<String?>(null)
    private val _schoolsLoaded = MutableStateFlow(false)
    private val _cloudInitError = MutableStateFlow<String?>(null)
    private val _cloudRetryTrigger = MutableStateFlow(0)

    private val _inSchoolHub = MutableStateFlow(false)
    val inSchoolHub: StateFlow<Boolean> = _inSchoolHub.asStateFlow()

    private val _hubBusy = MutableStateFlow(false)
    val hubBusy: StateFlow<Boolean> = _hubBusy.asStateFlow()

    private val _lookupBusy = MutableStateFlow(false)
    val lookupBusy: StateFlow<Boolean> = _lookupBusy.asStateFlow()

    private val _codePreview = MutableStateFlow<SchoolCodePreview?>(null)
    val codePreview: StateFlow<SchoolCodePreview?> = _codePreview.asStateFlow()

    private val _lookupError = MutableStateFlow<String?>(null)
    val lookupError: StateFlow<String?> = _lookupError.asStateFlow()

    private val _pendingJoinSchoolName = MutableStateFlow<String?>(null)
    private val _pendingJoinSchoolId = MutableStateFlow<String?>(null)

    val backupStatus: StateFlow<AutoBackupStatus> = autoBackupManager.status

    private val _pendingDriveConsent = MutableStateFlow<IntentSender?>(null)
    val pendingDriveConsent: StateFlow<IntentSender?> = _pendingDriveConsent.asStateFlow()

    private val _driveFiles = MutableStateFlow<List<DriveBackupFile>>(emptyList())
    val driveFiles: StateFlow<List<DriveBackupFile>> = _driveFiles.asStateFlow()

    private val _driveFilesLoading = MutableStateFlow(false)
    val driveFilesLoading: StateFlow<Boolean> = _driveFilesLoading.asStateFlow()

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    val userProfile: StateFlow<UserCloudProfile?> = _cloudRetryTrigger
        .flatMapLatest {
            cloudRepo.observeUserProfile()
                .catch { e ->
                    val fsErr = unwrapFirestoreError(e)
                    if (fsErr == null && e is CancellationException) throw e
                    if (!_isSigningOut.value) {
                        _snackbarMessage.value = friendlyFirestoreErrorMessage(fsErr ?: e)
                    }
                    emit(null)
                }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val pendingJoinSchoolName: StateFlow<String?> = combine(
        userProfile,
        _pendingJoinSchoolName
    ) { profile, localPending ->
        localPending?.takeIf { it.isNotBlank() }
            ?: profile?.pendingSchoolName?.takeIf { it.isNotBlank() }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val pendingJoinSchoolId: StateFlow<String?> = combine(
        userProfile,
        _pendingJoinSchoolId
    ) { profile, localId ->
        localId?.takeIf { it.isNotBlank() }
            ?: profile?.pendingSchoolId?.takeIf { it.isNotBlank() }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val mySchools: StateFlow<List<SchoolWorkspace>> = _cloudRetryTrigger
        .flatMapLatest {
            cloudRepo.observeMySchools()
                .map { list ->
                    _schoolsLoaded.value = true
                    _cloudInitError.value = null
                    list.sortedBy { it.settings.school }
                }
                .catch { e ->
                    val fsErr = unwrapFirestoreError(e)
                    if (fsErr == null && e is CancellationException) throw e
                    _schoolsLoaded.value = true
                    if (!_isSigningOut.value) {
                        _cloudInitError.value = friendlyFirestoreErrorMessage(fsErr ?: e)
                    }
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeSchool: StateFlow<SchoolWorkspace?> = combine(
        mySchools,
        userProfile,
        _selectedSchoolOverrideId,
        _suppressedSchoolId
    ) { schools, profile, overrideId, suppressedId ->
        val filtered = if (suppressedId != null) schools.filter { it.id != suppressedId } else schools
        val preferredId = overrideId ?: profile?.activeSchoolId
        filtered.find { it.id == preferredId } ?: filtered.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val sessionState: StateFlow<CloudSessionState> = combine(
        _schoolsLoaded,
        mySchools,
        activeSchool,
        _cloudInitError
    ) { loaded, schools, active, err ->
        when {
            !loaded -> CloudSessionState.Loading
            err != null && schools.isEmpty() -> CloudSessionState.Error(err)
            else -> CloudSessionState.Ready(schools, active)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CloudSessionState.Loading)

    private val studentsAndMetaFlow: StateFlow<Pair<List<Student>, Map<String, StudentSyncMeta>>> =
        combine(
            activeSchool.map { it?.id }.distinctUntilChanged(),
            _cloudRetryTrigger
        ) { schoolId, _ -> schoolId }
            .flatMapLatest { schoolId ->
                if (schoolId.isNullOrBlank()) {
                    flowOf(emptyList<Student>() to emptyMap())
                } else {
                    cloudRepo.observeSchoolStudents(schoolId)
                        .catch { e ->
                            val fsErr = unwrapFirestoreError(e)
                            if (fsErr == null && e is CancellationException) throw e
                            if (!_isSigningOut.value && _suppressedSchoolId.value != schoolId) {
                                _snackbarMessage.value = friendlyFirestoreErrorMessage(fsErr ?: e)
                            }
                            emit(emptyList<Student>() to emptyMap())
                        }
                }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList<Student>() to emptyMap())

    val students: StateFlow<List<Student>> = studentsAndMetaFlow
        .map { it.first }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val studentSyncMeta: StateFlow<Map<String, StudentSyncMeta>> = studentsAndMetaFlow
        .map { it.second }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val settings: StateFlow<Settings> = activeSchool
        .map { it?.settings ?: DEFAULT_SETTINGS }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_SETTINGS)

    val joinRequests: StateFlow<List<JoinRequestItem>> =
        activeSchool
            .map { school ->
                if (school != null && school.isAdmin(currentUserId)) school.id else null
            }
            .distinctUntilChanged()
            .flatMapLatest { adminSchoolId ->
                if (adminSchoolId.isNullOrBlank()) {
                    flowOf(emptyList())
                } else {
                    cloudRepo.observeJoinRequests(adminSchoolId, currentUserId)
                        .catch { e ->
                            val fsErr = unwrapFirestoreError(e)
                            if (fsErr == null && e is CancellationException) throw e
                            emit(emptyList())
                        }
                }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedClass = MutableStateFlow(1)
    val selectedClass: StateFlow<Int> = _selectedClass.asStateFlow()

    private val _currentSection = MutableStateFlow(NavSection.ROSTER)
    val currentSection: StateFlow<NavSection> = _currentSection.asStateFlow()

    private val _editingStudent = MutableStateFlow<Student?>(null)
    val editingStudent: StateFlow<Student?> = _editingStudent.asStateFlow()

    private val _editReturnSection = MutableStateFlow(NavSection.ROSTER)
    val editReturnSection: StateFlow<NavSection> = _editReturnSection.asStateFlow()

    private val _studioStudentId = MutableStateFlow<String?>(null)
    val studioStudentId: StateFlow<String?> = _studioStudentId.asStateFlow()

    private val _csvImportState = MutableStateFlow(CsvImportModalState())
    val csvImportState: StateFlow<CsvImportModalState> = _csvImportState.asStateFlow()

    private val _batchEditOpen = MutableStateFlow(false)
    val batchEditOpen: StateFlow<Boolean> = _batchEditOpen.asStateFlow()

    private val _batchEditInitialClass = MutableStateFlow<Int?>(null)
    val batchEditInitialClass: StateFlow<Int?> = _batchEditInitialClass.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _aiState = MutableStateFlow(AiAssistantState())
    val aiState: StateFlow<AiAssistantState> = _aiState.asStateFlow()

    private val _draftingRemarkTerm = MutableStateFlow<Int?>(null)
    val draftingRemarkTerm: StateFlow<Int?> = _draftingRemarkTerm.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(GeminiApiConfig.getApiKey(appContext))
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _geminiModel = MutableStateFlow(GeminiApiConfig.getSelectedModel(appContext))
    val geminiModel: StateFlow<String> = _geminiModel.asStateFlow()

    private val _geminiAvailableModels = MutableStateFlow(GeminiApiConfig.getCachedModels(appContext))
    val geminiAvailableModels: StateFlow<List<DiscoveredGeminiModel>> = _geminiAvailableModels.asStateFlow()

    private val _geminiCustomModels = MutableStateFlow(GeminiApiConfig.getCustomModels(appContext))
    val geminiCustomModels: StateFlow<List<String>> = _geminiCustomModels.asStateFlow()

    private val _geminiNewModelIds = MutableStateFlow<Set<String>>(emptySet())
    val geminiNewModelIds: StateFlow<Set<String>> = _geminiNewModelIds.asStateFlow()

    private val _geminiRefreshingModels = MutableStateFlow(false)
    val geminiRefreshingModels: StateFlow<Boolean> = _geminiRefreshingModels.asStateFlow()

    private val _geminiModelsError = MutableStateFlow<String?>(null)
    val geminiModelsError: StateFlow<String?> = _geminiModelsError.asStateFlow()

    private val _geminiTesting = MutableStateFlow(false)
    val geminiTesting: StateFlow<Boolean> = _geminiTesting.asStateFlow()

    private val _geminiTestStatus = MutableStateFlow<String?>(null)
    val geminiTestStatus: StateFlow<String?> = _geminiTestStatus.asStateFlow()

    private val _geminiTestSuccess = MutableStateFlow<Boolean?>(null)
    val geminiTestSuccess: StateFlow<Boolean?> = _geminiTestSuccess.asStateFlow()

    val isGeminiConfigured: StateFlow<Boolean> = _geminiApiKey
        .map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, GeminiApiConfig.isConfigured(appContext))

    val activePermissions: StateFlow<TeacherPermissions> = activeSchool
        .map { school ->
            school?.permissionsFor(currentUserId) ?: TeacherPermissions.fullAccess()
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, TeacherPermissions.fullAccess())

    init {
        viewModelScope.launch {
            cloudRepo.ensureUserProfile().onFailure { e ->
                if (!_isSigningOut.value) {
                    _snackbarMessage.value = friendlyFirestoreErrorMessage(e)
                }
            }
        }

        // Sync activeSchoolId into user profile whenever approved or joined (supports 1-school-per-user enforcement)
        viewModelScope.launch {
            combine(activeSchool, userProfile) { school, profile -> school to profile }
                .collectLatest { (school, profile) ->
                    if (school != null && profile != null && profile.activeSchoolId != school.id) {
                        _pendingJoinSchoolName.value = null
                        _pendingJoinSchoolId.value = null
                        cloudRepo.setActiveSchoolId(school.id)
                    }
                    if (school != null) {
                        autoBackupManager.refreshLocalSnapshots(school.id)
                        autoBackupManager.refreshCsvSnapshots(school.id)
                    }
                }
        }

        // Ensure _selectedClass always lands on an allowed class for restricted teachers
        viewModelScope.launch {
            activePermissions.collectLatest { perm ->
                val allowed = perm.effectiveClasses()
                if (allowed.isNotEmpty() && _selectedClass.value !in allowed) {
                    _selectedClass.value = allowed.first()
                }
            }
        }

        // Sync WorkManager schedules whenever active school or backup configuration changes
        viewModelScope.launch {
            combine(activeSchool, backupStatus) { school, status -> school to status }
                .collectLatest { (school, status) ->
                    if (school != null) {
                        BackupWorkScheduler.syncAllSchedules(
                            context = appContext,
                            userId = currentUserId,
                            schoolId = school.id,
                            schoolCode = school.joinCode,
                            status = status
                        )
                    }
                }
        }

        // Three-way Auto Backup (Local JSON, Google Drive, CSV) when Firestore data updates
        viewModelScope.launch {
            combine(activeSchool, students, settings) { school, studentList, st ->
                Triple(school, studentList, st)
            }
                .debounce(3_500L)
                .collectLatest { (school, studentList, st) ->
                    if (school == null) return@collectLatest
                    if (studentList.isEmpty() && st == DEFAULT_SETTINGS) return@collectLatest

                    Repo.save(appContext, studentList, st)
                    BackupWorkScheduler.saveActiveMeta(appContext, currentUserId, school.id, school.joinCode)

                    autoBackupManager.runLocalBackupIfNeeded(
                        schoolId = school.id,
                        schoolCode = school.joinCode,
                        students = studentList,
                        settings = st,
                        force = false
                    )

                    autoBackupManager.runCsvBackupIfNeeded(
                        schoolId = school.id,
                        schoolCode = school.joinCode,
                        students = studentList,
                        settings = st,
                        force = false
                    )

                    if (autoBackupManager.status.value.driveEnabled) {
                        delay(4_000L)
                        autoBackupManager.runDriveBackupIfNeeded(
                            schoolCode = school.joinCode,
                            students = studentList,
                            settings = st,
                            force = false
                        )
                    }
                }
        }

        // Foreground minute ticker so scheduled backups also fire promptly while the app stays open
        viewModelScope.launch {
            while (isActive) {
                delay(60_000L)
                val school = activeSchool.value ?: continue
                val studentList = students.value
                val st = settings.value
                if (studentList.isEmpty() && st == DEFAULT_SETTINGS) continue

                autoBackupManager.runLocalBackupIfNeeded(
                    schoolId = school.id,
                    schoolCode = school.joinCode,
                    students = studentList,
                    settings = st,
                    force = false
                )
                autoBackupManager.runCsvBackupIfNeeded(
                    schoolId = school.id,
                    schoolCode = school.joinCode,
                    students = studentList,
                    settings = st,
                    force = false
                )
                if (autoBackupManager.status.value.driveEnabled) {
                    autoBackupManager.runDriveBackupIfNeeded(
                        schoolCode = school.joinCode,
                        students = studentList,
                        settings = st,
                        force = false
                    )
                }
            }
        }

        if (GeminiApiConfig.isConfigured(appContext)) {
            refreshGeminiModels(silent = true)
        }
    }

    fun prepareForSignOut() {
        _isSigningOut.value = true
        _suppressedSchoolId.value = activeSchool.value?.id
    }

    fun retryCloudConnection() {
        _cloudInitError.value = null
        _schoolsLoaded.value = false
        _cloudRetryTrigger.update { it + 1 }
        viewModelScope.launch {
            cloudRepo.ensureUserProfile().onFailure { e ->
                if (!_isSigningOut.value) {
                    _snackbarMessage.value = friendlyFirestoreErrorMessage(e)
                }
            }
        }
    }

    // ---------- School Hub & Collaborative Admin ----------

    fun openSchoolHub() {
        _editingStudent.value = null
        _lookupError.value = null
        _codePreview.value = null
        _inSchoolHub.value = true
    }

    fun closeSchoolHub() {
        if (activeSchool.value != null) {
            _inSchoolHub.value = false
        }
    }

    fun selectSchool(schoolId: String) {
        _selectedSchoolOverrideId.value = schoolId
        _editingStudent.value = null
        _studioStudentId.value = null
        _currentSection.value = NavSection.ROSTER
        _inSchoolHub.value = false
        viewModelScope.launch {
            cloudRepo.setActiveSchoolId(schoolId).onFailure { e ->
                showMessage(friendlyFirestoreErrorMessage(e))
            }
        }
    }

    fun createSchool(
        name: String,
        address: String,
        year: String,
        importLocalData: Boolean
    ) {
        if (_hubBusy.value) return
        if (mySchools.value.isNotEmpty()) {
            showMessage("একজন ব্যবহারকারী শুধুমাত্র একটি বিদ্যালয়ে যুক্ত থাকতে পারবেন।")
            return
        }
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            showMessage("বিদ্যালয়ের নাম লিখুন")
            return
        }
        val seedStudents = if (importLocalData) initialLocalSnapshot?.first.orEmpty() else emptyList()
        val seedSettings = if (importLocalData) (initialLocalSnapshot?.second ?: DEFAULT_SETTINGS) else DEFAULT_SETTINGS

        _hubBusy.value = true
        viewModelScope.launch {
            cloudRepo.createSchool(
                schoolName = cleanName,
                schoolAddress = address,
                year = year,
                baseSettings = seedSettings,
                initialStudents = seedStudents
            ).onSuccess { created ->
                _pendingJoinSchoolName.value = null
                _pendingJoinSchoolId.value = null
                _selectedSchoolOverrideId.value = created.id
                _inSchoolHub.value = false
                _currentSection.value = NavSection.ROSTER
                showMessage("বিদ্যালয় তৈরি হয়েছে! স্কুল কোড: ${created.joinCode}")
            }.onFailure { e ->
                showMessage(friendlyFirestoreErrorMessage(e))
            }
            _hubBusy.value = false
        }
    }

    fun lookupSchoolCode(rawCode: String) {
        if (_lookupBusy.value) return
        _lookupError.value = null
        _codePreview.value = null
        _lookupBusy.value = true
        viewModelScope.launch {
            cloudRepo.lookupSchoolCode(rawCode)
                .onSuccess { preview ->
                    if (preview == null) {
                        _lookupError.value = "এই কোডে কোনো সক্রিয় বিদ্যালয় পাওয়া যায়নি"
                    } else {
                        _codePreview.value = preview
                    }
                }
                .onFailure { e ->
                    _lookupError.value = friendlyFirestoreErrorMessage(e)
                }
            _lookupBusy.value = false
        }
    }

    fun clearCodePreview() {
        _codePreview.value = null
        _lookupError.value = null
    }

    fun joinWithCodePreview(preview: SchoolCodePreview) {
        if (_hubBusy.value) return
        if (mySchools.value.isNotEmpty()) {
            showMessage("একজন ব্যবহারকারী শুধুমাত্র একটি বিদ্যালয়ে যুক্ত থাকতে পারবেন।")
            return
        }
        _hubBusy.value = true
        viewModelScope.launch {
            cloudRepo.joinSchoolWithCode(preview)
                .onSuccess {
                    _codePreview.value = null
                    _lookupError.value = null
                    _pendingJoinSchoolId.value = preview.schoolId
                    _pendingJoinSchoolName.value = preview.schoolName
                    showMessage("যোগদানের অনুরোধ পাঠানো হয়েছে — অ্যাডমিন অনুমোদনের অপেক্ষায়")
                }
                .onFailure { e ->
                    showMessage(friendlyFirestoreErrorMessage(e))
                }
            _hubBusy.value = false
        }
    }

    fun cancelPendingJoinRequest() {
        val targetSchoolId = pendingJoinSchoolId.value.orEmpty()
        viewModelScope.launch {
            cloudRepo.cancelPendingJoinRequest(targetSchoolId)
                .onSuccess {
                    _pendingJoinSchoolId.value = null
                    _pendingJoinSchoolName.value = null
                    showMessage("যোগদানের অনুরোধ বাতিল করা হয়েছে")
                }
                .onFailure { e -> showMessage(friendlyFirestoreErrorMessage(e)) }
        }
    }

    fun approveJoinRequest(
        req: JoinRequestItem,
        permissions: TeacherPermissions = TeacherPermissions.fullAccess()
    ) {
        val school = activeSchool.value ?: return
        if (!school.isAdmin(currentUserId)) {
            showMessage("শুধুমাত্র অ্যাডমিন অনুমোদন করতে পারবেন")
            return
        }
        viewModelScope.launch {
            cloudRepo.approveJoinRequest(school.id, req, permissions)
                .onSuccess { showMessage("${req.displayName.ifBlank { "শিক্ষক" }}-কে বিদ্যালয়ে যুক্ত করা হয়েছে") }
                .onFailure { e -> showMessage(friendlyFirestoreErrorMessage(e)) }
        }
    }

    fun rejectJoinRequest(requestUid: String) {
        val school = activeSchool.value ?: return
        if (!school.isAdmin(currentUserId)) return
        viewModelScope.launch {
            cloudRepo.rejectJoinRequest(school.id, requestUid)
                .onSuccess { showMessage("অনুরোধ বাতিল করা হয়েছে") }
                .onFailure { e -> showMessage(friendlyFirestoreErrorMessage(e)) }
        }
    }

    fun regenerateJoinCode() {
        val school = activeSchool.value ?: return
        if (!school.isAdmin(currentUserId)) {
            showMessage("শুধুমাত্র অ্যাডমিন কোড পরিবর্তন করতে পারবেন")
            return
        }
        viewModelScope.launch {
            cloudRepo.regenerateJoinCode(school)
                .onSuccess { newCode -> showMessage("নতুন স্কুল কোড তৈরি হয়েছে: $newCode") }
                .onFailure { e -> showMessage(friendlyFirestoreErrorMessage(e)) }
        }
    }

    fun setApprovalRequired(required: Boolean) {
        val school = activeSchool.value ?: return
        if (!school.isAdmin(currentUserId)) return
        viewModelScope.launch {
            cloudRepo.setApprovalRequired(school, true)
                .onSuccess {
                    showMessage("যোগদানের আগে অ্যাডমিন অনুমোদন সর্বদা চালু থাকে")
                }
                .onFailure { e -> showMessage(friendlyFirestoreErrorMessage(e)) }
        }
    }

    fun updateMemberPermissions(targetUid: String, permissions: TeacherPermissions) {
        val school = activeSchool.value ?: return
        if (!school.isAdmin(currentUserId)) {
            showMessage("শুধুমাত্র অ্যাডমিন শিক্ষকের অ্যাক্সেস পরিবর্তন করতে পারবেন")
            return
        }
        viewModelScope.launch {
            cloudRepo.updateMemberPermissions(school, targetUid, permissions)
                .onSuccess { showMessage("শিক্ষকের ডেটা অ্যাক্সেস অনুমতি হালনাগাদ হয়েছে") }
                .onFailure { e -> showMessage(friendlyFirestoreErrorMessage(e)) }
        }
    }

    fun updateMemberRole(targetUid: String, role: MemberRole) {
        val school = activeSchool.value ?: return
        if (!school.isAdmin(currentUserId)) return
        viewModelScope.launch {
            cloudRepo.updateMemberRole(school, targetUid, role)
                .onSuccess { showMessage("সদস্যের ভূমিকা হালনাগাদ হয়েছে") }
                .onFailure { e -> showMessage(friendlyFirestoreErrorMessage(e)) }
        }
    }

    fun removeMember(targetUid: String) {
        val school = activeSchool.value ?: return
        if (!school.isAdmin(currentUserId)) return
        viewModelScope.launch {
            cloudRepo.removeMember(school, targetUid)
                .onSuccess { showMessage("সদস্যকে বিদ্যালয় থেকে অপসারণ করা হয়েছে") }
                .onFailure { e -> showMessage(friendlyFirestoreErrorMessage(e)) }
        }
    }

    fun leaveActiveSchool() {
        val school = activeSchool.value ?: return
        if (school.ownerUid == currentUserId) {
            showMessage("বিদ্যালয়ের প্রতিষ্ঠাতা নিজে বিদ্যালয় ত্যাগ করতে পারবেন না")
            return
        }
        viewModelScope.launch {
            _suppressedSchoolId.value = school.id
            cloudRepo.leaveSchool(school)
                .onSuccess {
                    if (_selectedSchoolOverrideId.value == school.id) {
                        _selectedSchoolOverrideId.value = null
                    }
                    _suppressedSchoolId.value = null
                    _editingStudent.value = null
                    _studioStudentId.value = null
                    _inSchoolHub.value = true
                    showMessage("\"${school.settings.school}\" ত্যাগ করা হয়েছে")
                }
                .onFailure { e ->
                    _suppressedSchoolId.value = null
                    showMessage(friendlyFirestoreErrorMessage(e))
                }
        }
    }

    // ---------- Navigation & Student CRUD ----------

    fun selectClass(cls: Int) {
        if (cls in 1..5) _selectedClass.value = cls
    }

    fun openSheetForClass(cls: Int) {
        if (cls in 1..5) _selectedClass.value = cls
        _editingStudent.value = null
        _currentSection.value = NavSection.SHEET
    }

    fun navigate(section: NavSection) {
        _editingStudent.value = null
        _currentSection.value = section
    }

    fun openEditor(student: Student, returnTo: NavSection = _currentSection.value) {
        _editReturnSection.value = returnTo
        _editingStudent.value = student
    }

    fun openNewStudentEditor(returnTo: NavSection = _currentSection.value) {
        val perm = activePermissions.value
        if (!perm.canManageStudents) {
            showMessage("নতুন শিক্ষার্থী যোগ করার অনুমতি আপনার নেই")
            return
        }
        val cls = _selectedClass.value
        if (!perm.canAccessClass(cls)) {
            showMessage("এই শ্রেণিতে শিক্ষার্থী যোগ করার অনুমতি নেই")
            return
        }
        val nextRoll = (students.value.filter { it.cls == cls }.maxOfOrNull { it.roll } ?: 0) + 1
        _editReturnSection.value = returnTo
        _editingStudent.value = createEmptyStudent(cls, nextRoll)
    }

    fun closeEditor() {
        val target = _editReturnSection.value
        _editingStudent.value = null
        _currentSection.value = target
    }

    fun openStudio(studentId: String?) {
        if (!activePermissions.value.canExportPdf) {
            showMessage("রিপোর্ট কার্ড প্রিভিউ/PDF দেখার অনুমতি আপনার নেই")
            return
        }
        _editingStudent.value = null
        _studioStudentId.value = studentId
        _currentSection.value = NavSection.STUDIO
    }

    fun selectStudioStudent(studentId: String) {
        _studioStudentId.value = studentId
    }

    fun openCsvImport(initialText: String = "") {
        if (!activePermissions.value.canManageStudents) {
            showMessage("CSV ইমপোর্টের অনুমতি শুধুমাত্র অনুমোদিত শিক্ষক বা অ্যাডমিনের")
            return
        }
        _csvImportState.value = CsvImportModalState(isOpen = true, initialText = initialText)
    }

    fun closeCsvImport() {
        _csvImportState.value = CsvImportModalState(isOpen = false, initialText = "")
    }

    fun openBatchEdit(initialCls: Int? = null) {
        if (!activePermissions.value.canManageStudents) {
            showMessage("শিক্ষার্থীদের তথ্য ব্যাচ পরিবর্তনের অনুমতি আপনার নেই")
            return
        }
        _batchEditInitialClass.value = initialCls
        _batchEditOpen.value = true
    }

    fun closeBatchEdit() {
        _batchEditOpen.value = false
    }

    fun batchUpdateStudents(studentIds: Set<String>, patch: BatchStudentUpdatePatch) {
        val school = activeSchool.value ?: return
        val perm = school.permissionsFor(currentUserId)
        if (!perm.canManageStudents) {
            showMessage("শিক্ষার্থীদের তথ্য পরিবর্তনের অনুমতি আপনার নেই")
            return
        }
        if (studentIds.isEmpty() || !patch.hasAnyField) {
            showMessage("অন্তত ১ জন শিক্ষার্থী ও ১টি পরিবর্তনের তথ্য নির্বাচন করুন")
            return
        }
        val prevList = students.value
        var touchedCount = 0
        val nextList = prevList.map { st ->
            if (st.id !in studentIds || !perm.canAccessClass(st.cls)) {
                st
            } else {
                touchedCount++
                val nextX = if (patch.customFieldUpdates.isEmpty()) {
                    st.x
                } else {
                    st.x.toMutableMap().apply {
                        patch.customFieldUpdates.forEach { (cfKey, cfVal) ->
                            val clean = cfVal.trim()
                            if (clean.isEmpty()) remove(cfKey) else put(cfKey, clean)
                        }
                    }
                }
                st.copy(
                    village = if (patch.updateVillage) patch.village.trim() else st.village,
                    union = if (patch.updateUnion) patch.union.trim() else st.union,
                    upazila = if (patch.updateUpazila) patch.upazila.trim().ifBlank { DEFAULT_UPAZILA } else st.upazila,
                    district = if (patch.updateDistrict) patch.district.trim().ifBlank { DEFAULT_DISTRICT } else st.district,
                    religion = if (patch.updateReligion) patch.religion.trim().ifBlank { st.religion } else st.religion,
                    x = nextX
                )
            }
        }
        if (touchedCount == 0) {
            showMessage("অনুমোদিত কোনো শিক্ষার্থী নির্বাচিত হয়নি")
            return
        }
        closeBatchEdit()
        viewModelScope.launch {
            cloudRepo.syncBatchStudents(school.id, prevList, nextList, deleteRemoved = false)
                .onSuccess {
                    showMessage("${bn(touchedCount.toString())} জন শিক্ষার্থীর তথ্য ব্যাচ আপডেট সম্পন্ন হয়েছে")
                }
                .onFailure { e ->
                    showMessage(friendlyFirestoreErrorMessage(e))
                }
        }
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearMessage() {
        _snackbarMessage.value = null
    }

    private fun sanitizeStudentForPermissions(
        student: Student,
        existing: Student?,
        perm: TeacherPermissions
    ): Student? {
        if (existing == null) {
            if (!perm.canManageStudents || !perm.canAccessClass(student.cls)) return null
            val filteredM = if (perm.allSubjects) student.m else student.m.filterKeys { k ->
                val sub = k.substringBefore('_')
                perm.canEditSubject(sub)
            }
            val filteredX = if (perm.canEditAttendance) student.x else emptyMap()
            return student.copy(m = filteredM, x = filteredX)
        }

        if (!perm.canAccessClass(existing.cls)) return null

        val nextM = if (perm.allSubjects) {
            student.m
        } else {
            existing.m.toMutableMap().apply {
                // Keep untouched keys from existing; only add/update/remove allowed subject keys
                keys.toList().forEach { k ->
                    val sub = k.substringBefore('_')
                    if (perm.canEditSubject(sub) && !student.m.containsKey(k)) remove(k)
                }
                student.m.forEach { (k, v) ->
                    val sub = k.substringBefore('_')
                    if (perm.canEditSubject(sub)) put(k, v)
                }
            }
        }

        val attendancePrefixKeys = setOf("DAYS_", "PRES_", "ACT_", "DISC_", "COND_", "REM_")
        fun isAttendanceKey(k: String): Boolean = attendancePrefixKeys.any { k.startsWith(it) }

        val nextX = existing.x.toMutableMap().apply {
            if (perm.canEditAttendance) {
                keys.toList().forEach { k -> if (isAttendanceKey(k) && !student.x.containsKey(k)) remove(k) }
                student.x.forEach { (k, v) -> if (isAttendanceKey(k)) put(k, v) }
            }
            if (perm.canManageStudents) {
                keys.toList().forEach { k -> if (!isAttendanceKey(k) && !student.x.containsKey(k)) remove(k) }
                student.x.forEach { (k, v) -> if (!isAttendanceKey(k)) put(k, v) }
            }
        }

        return if (perm.canManageStudents && perm.canAccessClass(student.cls)) {
            student.copy(m = nextM, x = nextX)
        } else {
            existing.copy(m = nextM, x = nextX)
        }
    }

    fun upsertStudent(student: Student) {
        val school = activeSchool.value ?: return
        val existing = students.value.find { it.id == student.id }
        val perm = school.permissionsFor(currentUserId)
        val safeStudent = sanitizeStudentForPermissions(student, existing, perm)
        if (safeStudent == null) {
            showMessage("এই শিক্ষার্থীর তথ্য পরিবর্তন করার অনুমতি আপনার নেই")
            return
        }
        viewModelScope.launch {
            cloudRepo.upsertStudent(school.id, safeStudent, existing).onFailure { e ->
                showMessage(friendlyFirestoreErrorMessage(e))
            }
        }
    }

    fun patchStudentField(studentId: String, fieldPath: String, value: Any?) {
        val school = activeSchool.value ?: return
        val perm = school.permissionsFor(currentUserId)
        val target = students.value.find { it.id == studentId } ?: return
        if (!perm.canAccessClass(target.cls)) {
            showMessage("এই শ্রেণির তথ্য পরিবর্তনের অনুমতি নেই")
            return
        }
        val allowed = when {
            fieldPath.startsWith("m.") -> {
                val subKey = fieldPath.removePrefix("m.").substringBefore('_')
                perm.canEditSubject(subKey)
            }
            fieldPath.startsWith("x.") -> {
                val xKey = fieldPath.removePrefix("x.")
                val isAtt = listOf("DAYS_", "PRES_", "ACT_", "DISC_", "COND_", "REM_").any { xKey.startsWith(it) }
                if (isAtt) perm.canEditAttendance else perm.canManageStudents
            }
            else -> perm.canManageStudents
        }
        if (!allowed) {
            showMessage("এই ক্ষেত্রটি পরিবর্তনের অনুমতি আপনার নেই")
            return
        }
        viewModelScope.launch {
            cloudRepo.patchStudentField(school.id, studentId, fieldPath, value).onFailure { e ->
                showMessage(friendlyFirestoreErrorMessage(e))
            }
        }
    }

    fun batchUpdateTraits(
        studentIds: Set<String>,
        xKeys: Set<String>,
        value: String?
    ) {
        val school = activeSchool.value ?: return
        val perm = school.permissionsFor(currentUserId)
        if (!perm.canEditAttendance) {
            showMessage("গুণাবলি সম্পাদনার অনুমতি আপনার নেই")
            return
        }
        val validTraitKeys = xKeys.filter { k ->
            k.startsWith("ACT_") || k.startsWith("DISC_") || k.startsWith("COND_")
        }
        if (studentIds.isEmpty() || validTraitKeys.isEmpty()) {
            showMessage("অন্তত ১ জন শিক্ষার্থী ও ১টি গুণাবলি নির্বাচন করুন")
            return
        }
        val cleanVal = value?.trim()?.takeIf { it in setOf("ক", "খ", "গ") }
        val allowedIds = students.value
            .filter { it.id in studentIds && perm.canAccessClass(it.cls) }
            .map { it.id }
        if (allowedIds.isEmpty()) {
            showMessage("অনুমোদিত কোনো শিক্ষার্থী নির্বাচিত হয়নি")
            return
        }
        val xUpdates = validTraitKeys.associateWith { cleanVal }
        viewModelScope.launch {
            cloudRepo.patchBatchXFields(school.id, allowedIds, xUpdates)
                .onSuccess {
                    val valLabel = if (cleanVal == null) "ফাঁকা (মুছে ফেলা)" else "'$cleanVal'"
                    showMessage("${bn(allowedIds.size.toString())} জন শিক্ষার্থীর গুণাবলি $valLabel হিসেবে আপডেট হয়েছে")
                }
                .onFailure { e ->
                    showMessage(friendlyFirestoreErrorMessage(e))
                }
        }
    }

    fun batchUpdateAttendance(
        studentIds: Set<String>,
        term: Int,
        workingDays: Int?,
        presentDays: Int?
    ) {
        val school = activeSchool.value ?: return
        val perm = school.permissionsFor(currentUserId)
        if (!perm.canEditAttendance) {
            showMessage("উপস্থিতি ও কার্যদিবস সম্পাদনার অনুমতি আপনার নেই")
            return
        }
        val safeTerm = term.coerceIn(1, 3)
        if (studentIds.isEmpty()) {
            showMessage("অন্তত ১ জন শিক্ষার্থী নির্বাচন করুন")
            return
        }
        if (workingDays == null && presentDays == null) {
            showMessage("কার্যদিবস বা উপস্থিতির সংখ্যা লিখুন")
            return
        }
        if ((workingDays != null && workingDays !in 0..365) || (presentDays != null && presentDays !in 0..365)) {
            showMessage("সংখ্যা ০ থেকে ৩৬৫ এর মধ্যে হতে হবে")
            return
        }
        if (workingDays != null && presentDays != null && presentDays > workingDays) {
            showMessage("উপস্থিতি কার্যদিবসের চেয়ে বেশি হতে পারবে না")
            return
        }
        val allowedIds = students.value
            .filter { it.id in studentIds && perm.canAccessClass(it.cls) }
            .map { it.id }
        if (allowedIds.isEmpty()) {
            showMessage("অনুমোদিত কোনো শিক্ষার্থী নির্বাচিত হয়নি")
            return
        }
        val xUpdates = buildMap<String, String?> {
            workingDays?.let { put("DAYS_$safeTerm", it.toString()) }
            presentDays?.let { put("PRES_$safeTerm", it.toString()) }
        }
        viewModelScope.launch {
            cloudRepo.patchBatchXFields(school.id, allowedIds, xUpdates)
                .onSuccess {
                    val what = when {
                        workingDays != null && presentDays != null -> "কার্যদিবস ও উপস্থিতি"
                        workingDays != null -> "কার্যদিবস"
                        else -> "উপস্থিতি"
                    }
                    showMessage("${bn(allowedIds.size.toString())} জন শিক্ষার্থীর $what আপডেট হয়েছে")
                }
                .onFailure { e ->
                    showMessage(friendlyFirestoreErrorMessage(e))
                }
        }
    }

    fun deleteStudent(id: String) {
        val school = activeSchool.value ?: return
        val target = students.value.find { it.id == id } ?: return
        val perm = school.permissionsFor(currentUserId)
        if (!perm.canManageStudents || !perm.canAccessClass(target.cls)) {
            showMessage("শিক্ষার্থী মুছে ফেলার অনুমতি আপনার নেই")
            return
        }
        if (_studioStudentId.value == id) _studioStudentId.value = null
        viewModelScope.launch {
            cloudRepo.deleteStudent(school.id, id)
                .onSuccess {
                    val label = target.name.ifBlank { "রোল ${bn(target.roll.toString())}" }
                    showMessage("$label মুছে ফেলা হয়েছে")
                }
                .onFailure { e ->
                    showMessage(friendlyFirestoreErrorMessage(e))
                }
        }
    }

    fun updateSettings(newSettings: Settings) {
        val school = activeSchool.value ?: return
        if (!school.isAdmin(currentUserId)) {
            showMessage("বিদ্যালয়ের সেটিংস পরিবর্তনের অনুমতি শুধুমাত্র অ্যাডমিনের")
            return
        }
        viewModelScope.launch {
            cloudRepo.updateSchoolSettings(school, newSettings).onFailure { e ->
                showMessage(friendlyFirestoreErrorMessage(e))
            }
        }
    }

    fun addCustomField(label: String): Boolean {
        val school = activeSchool.value ?: return false
        if (!school.isAdmin(currentUserId)) {
            showMessage("নতুন কাস্টম ফিল্ড যোগ করার অনুমতি শুধুমাত্র অ্যাডমিনের")
            return false
        }
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return false
        val existing = settings.value.customFields
        if (existing.any { it.label.equals(trimmed, ignoreCase = true) }) {
            showMessage("এই নামে একটি ফিল্ড ইতিমধ্যে আছে")
            return false
        }
        val key = Repo.makeCustomFieldKey(trimmed, existing.map { it.key }.toSet())
        val next = settings.value.copy(customFields = existing + CustomFieldDef(key, trimmed))
        updateSettings(next)
        return true
    }

    fun removeCustomField(key: String) {
        val school = activeSchool.value ?: return
        if (!school.isAdmin(currentUserId)) {
            showMessage("কাস্টম ফিল্ড মুছার অনুমতি শুধুমাত্র অ্যাডমিনের")
            return
        }
        val removed = settings.value.customFields.find { it.key == key }
        val next = settings.value.copy(
            customFields = settings.value.customFields.filter { it.key != key }
        )
        updateSettings(next)
        if (removed != null) {
            showMessage("\"${removed.label}\" ফিল্ড মুছে ফেলা হয়েছে")
        }
    }

    fun applyCsvImport(summary: Repo.CsvImportSummary) {
        val school = activeSchool.value ?: return
        val perm = school.permissionsFor(currentUserId)
        if (!perm.canManageStudents) {
            showMessage("CSV ইমপোর্টের অনুমতি আপনার নেই")
            closeCsvImport()
            return
        }
        val prevList = students.value
        val existingById = prevList.associateBy { it.id }
        val safeStudents = if (school.isAdmin(currentUserId)) {
            summary.students
        } else {
            summary.students.mapNotNull { candidate ->
                sanitizeStudentForPermissions(candidate, existingById[candidate.id], perm)
            }
        }
        closeCsvImport()
        viewModelScope.launch {
            if (summary.createdFields.isNotEmpty() && school.isAdmin(currentUserId)) {
                cloudRepo.updateSchoolSettings(school, summary.settings)
            }
            cloudRepo.syncBatchStudents(school.id, prevList, safeStudents, deleteRemoved = false)
                .onSuccess {
                    val msg = buildString {
                        append("ইমপোর্ট সম্পন্ন: নতুন ${bn(summary.addedCount.toString())} জন, হালনাগাদ ${bn(summary.updatedCount.toString())} জন")
                        if (summary.createdFields.isNotEmpty() && school.isAdmin(currentUserId)) {
                            append(", নতুন ফিল্ড ${bn(summary.createdFields.size.toString())}টি")
                        }
                    }
                    showMessage(msg)
                }
                .onFailure { e ->
                    showMessage(friendlyFirestoreErrorMessage(e))
                }
        }
    }

    fun restoreBackupJson(rawText: String) {
        val school = activeSchool.value ?: return
        if (!school.isAdmin(currentUserId)) {
            showMessage("JSON রিস্টোর শুধুমাত্র বিদ্যালয়ের অ্যাডমিন করতে পারবেন")
            return
        }
        try {
            val (restoredStudents, restoredSettings) = Repo.fromJson(rawText)
            val prevList = students.value
            viewModelScope.launch {
                cloudRepo.updateSchoolSettings(school, restoredSettings)
                cloudRepo.syncBatchStudents(school.id, prevList, restoredStudents, deleteRemoved = true)
                    .onSuccess {
                        showMessage("ব্যাকআপ রিস্টোর হয়েছে (${bn(restoredStudents.size.toString())} জন শিক্ষার্থী)")
                    }
                    .onFailure { e ->
                        showMessage(friendlyFirestoreErrorMessage(e))
                    }
            }
        } catch (e: Exception) {
            showMessage("রিস্টোর ব্যর্থ হয়েছে: ${e.message ?: "অবৈধ ফাইল"}")
        }
    }

    fun resetSampleData() {
        val school = activeSchool.value ?: return
        if (!school.isAdmin(currentUserId)) {
            showMessage("নমুনা ডাটা রিসেট শুধুমাত্র অ্যাডমিন করতে পারবেন")
            return
        }
        val prevList = students.value
        viewModelScope.launch {
            val keepSchoolSettings = DEFAULT_SETTINGS.copy(
                school = school.settings.school.ifBlank { DEFAULT_SETTINGS.school },
                schoolAddress = school.settings.schoolAddress,
                year = school.settings.year
            )
            cloudRepo.updateSchoolSettings(school, keepSchoolSettings)
            cloudRepo.syncBatchStudents(school.id, prevList, SAMPLE_STUDENTS, deleteRemoved = true)
                .onSuccess { showMessage("নমুনা ডাটা রিসেট করা হয়েছে") }
                .onFailure { e -> showMessage(friendlyFirestoreErrorMessage(e)) }
        }
    }

    fun clearAllStudents() {
        val school = activeSchool.value ?: return
        if (!school.isAdmin(currentUserId)) {
            showMessage("সকল শিক্ষার্থী মুছে ফেলার অনুমতি শুধুমাত্র অ্যাডমিনের")
            return
        }
        val ids = students.value.map { it.id }
        _studioStudentId.value = null
        viewModelScope.launch {
            cloudRepo.deleteAllSchoolStudents(school.id, ids)
                .onSuccess { showMessage("সকল শিক্ষার্থীর তথ্য মুছে ফেলা হয়েছে") }
                .onFailure { e -> showMessage(friendlyFirestoreErrorMessage(e)) }
        }
    }

    // ---------- 3-System Auto Backup (Local JSON + Google Drive + CSV) ----------

    fun toggleLocalAutoBackup(enabled: Boolean) {
        autoBackupManager.setLocalAutoBackupEnabled(enabled)
        if (enabled) {
            val school = activeSchool.value ?: return
            viewModelScope.launch {
                autoBackupManager.runLocalBackupIfNeeded(
                    schoolId = school.id,
                    schoolCode = school.joinCode,
                    students = students.value,
                    settings = settings.value,
                    force = true
                )
            }
            showMessage("লোকাল অটো ব্যাকআপ চালু হয়েছে")
        } else {
            showMessage("লোকাল অটো ব্যাকআপ বন্ধ করা হয়েছে")
        }
    }

    fun setLocalBackupFolder(uri: Uri?) {
        autoBackupManager.setLocalFolderUri(uri)
        val school = activeSchool.value ?: return
        viewModelScope.launch {
            autoBackupManager.runLocalBackupIfNeeded(
                schoolId = school.id,
                schoolCode = school.joinCode,
                students = students.value,
                settings = settings.value,
                force = true
            ).onSuccess {
                showMessage("নির্বাচিত ফোল্ডারে লোকাল ব্যাকআপ সংরক্ষিত হয়েছে")
            }.onFailure { e ->
                showMessage(e.localizedMessage ?: "ফোল্ডারে ব্যাকআপ লেখা যায়নি")
            }
        }
    }

    fun toggleDriveAutoBackup(enabled: Boolean) {
        if (!enabled) {
            autoBackupManager.setDriveAutoBackupEnabled(false)
            showMessage("Google Drive অটো ব্যাকআপ বন্ধ হয়েছে")
            return
        }
        val school = activeSchool.value ?: return
        viewModelScope.launch {
            when (val outcome = autoBackupManager.runDriveBackupIfNeeded(
                schoolCode = school.joinCode,
                students = students.value,
                settings = settings.value,
                force = true
            )) {
                is DriveAuthOutcome.Authorized -> {
                    autoBackupManager.setDriveAutoBackupEnabled(true)
                    showMessage("Google Drive অটো ব্যাকআপ চালু ও সম্পন্ন হয়েছে")
                }
                is DriveAuthOutcome.NeedsConsent -> {
                    _pendingDriveConsent.value = outcome.intentSender
                }
                is DriveAuthOutcome.Failed -> {
                    showMessage(outcome.message)
                }
            }
        }
    }

    fun toggleCsvAutoBackup(enabled: Boolean) {
        autoBackupManager.setCsvAutoBackupEnabled(enabled)
        if (enabled) {
            val school = activeSchool.value ?: return
            viewModelScope.launch {
                autoBackupManager.runCsvBackupIfNeeded(
                    schoolId = school.id,
                    schoolCode = school.joinCode,
                    students = students.value,
                    settings = settings.value,
                    force = true
                )
            }
            showMessage("CSV অটো ব্যাকআপ চালু হয়েছে")
        } else {
            showMessage("CSV অটো ব্যাকআপ বন্ধ করা হয়েছে")
        }
    }

    fun setCsvBackupFolder(uri: Uri?) {
        autoBackupManager.setCsvFolderUri(uri)
        val school = activeSchool.value ?: return
        viewModelScope.launch {
            autoBackupManager.runCsvBackupIfNeeded(
                schoolId = school.id,
                schoolCode = school.joinCode,
                students = students.value,
                settings = settings.value,
                force = true
            ).onSuccess {
                showMessage("নির্বাচিত ফোল্ডারে CSV ব্যাকআপ সংরক্ষিত হয়েছে")
            }.onFailure { e ->
                showMessage(e.localizedMessage ?: "ফোল্ডারে CSV ব্যাকআপ লেখা যায়নি")
            }
        }
    }

    fun setCsvHeaderStyle(style: CsvHeaderStyle) {
        autoBackupManager.setCsvHeaderStyle(style)
        showMessage("CSV হেডার ফরম্যাট: ${style.labelBn}")
        val school = activeSchool.value ?: return
        if (autoBackupManager.status.value.csvEnabled) {
            viewModelScope.launch {
                autoBackupManager.runCsvBackupIfNeeded(
                    schoolId = school.id,
                    schoolCode = school.joinCode,
                    students = students.value,
                    settings = settings.value,
                    force = true
                )
            }
        }
    }

    fun setCsvBackupFormat(format: CsvBackupFormat) {
        autoBackupManager.setCsvBackupFormat(format)
        showMessage("অটো ব্যাকআপ ফরম্যাট: ${format.labelBn}")
    }

    fun updateBackupSchedule(kind: BackupSystemKind, config: BackupScheduleConfig) {
        when (kind) {
            BackupSystemKind.LOCAL -> autoBackupManager.setLocalSchedule(config)
            BackupSystemKind.DRIVE -> autoBackupManager.setDriveSchedule(config)
            BackupSystemKind.CSV -> autoBackupManager.setCsvSchedule(config)
        }
        val school = activeSchool.value
        if (school != null) {
            BackupWorkScheduler.syncAllSchedules(
                context = appContext,
                userId = currentUserId,
                schoolId = school.id,
                schoolCode = school.joinCode,
                status = autoBackupManager.status.value
            )
        }
        val kindLabel = when (kind) {
            BackupSystemKind.LOCAL -> "লোকাল JSON"
            BackupSystemKind.DRIVE -> "Google Drive"
            BackupSystemKind.CSV -> "CSV"
        }
        val scheduleDesc = if (config.mode == BackupScheduleMode.ON_CHANGE) {
            config.mode.labelBn
        } else {
            "${config.mode.shortBn} · ${com.kgps.result.backup.BackupScheduleCalculator.formatTime12hBn(config.hour, config.minute)}"
        }
        showMessage("$kindLabel সময়সূচি: $scheduleDesc")
    }

    fun backupLocalNow() {
        val school = activeSchool.value ?: return
        viewModelScope.launch {
            autoBackupManager.runLocalBackupIfNeeded(
                schoolId = school.id,
                schoolCode = school.joinCode,
                students = students.value,
                settings = settings.value,
                force = true
            ).onSuccess {
                showMessage("লোকাল JSON ব্যাকআপ সম্পন্ন হয়েছে")
            }.onFailure { e ->
                showMessage(e.localizedMessage ?: "লোকাল ব্যাকআপ ব্যর্থ")
            }
        }
    }

    fun backupDriveNow() {
        val school = activeSchool.value ?: return
        viewModelScope.launch {
            when (val driveRes = autoBackupManager.runDriveBackupIfNeeded(
                schoolCode = school.joinCode,
                students = students.value,
                settings = settings.value,
                force = true
            )) {
                is DriveAuthOutcome.Authorized -> showMessage("Google Drive ব্যাকআপ সম্পন্ন হয়েছে")
                is DriveAuthOutcome.NeedsConsent -> _pendingDriveConsent.value = driveRes.intentSender
                is DriveAuthOutcome.Failed -> showMessage(driveRes.message)
            }
        }
    }

    fun backupCsvNow(formatOverride: CsvBackupFormat? = null) {
        val school = activeSchool.value ?: return
        val usedFormat = formatOverride ?: autoBackupManager.status.value.csvBackupFormat
        viewModelScope.launch {
            autoBackupManager.runCsvBackupIfNeeded(
                schoolId = school.id,
                schoolCode = school.joinCode,
                students = students.value,
                settings = settings.value,
                force = true,
                formatOverride = usedFormat
            ).onSuccess {
                val msg = when (usedFormat) {
                    CsvBackupFormat.CSV -> "১টি CSV ফাইলে সকল শ্রেণির এক্সপোর্ট সম্পন্ন হয়েছে (Downloads/ResultMaker)"
                    CsvBackupFormat.EXCEL -> "১টি Excel (.xlsx) ফাইলে আলাদা আলাদা শিটে এক্সপোর্ট সম্পন্ন হয়েছে"
                }
                showMessage(msg)
            }.onFailure { e ->
                showMessage(e.localizedMessage ?: "এক্সপোর্ট ব্যর্থ হয়েছে")
            }
        }
    }

    fun backupNowAll() {
        val school = activeSchool.value ?: return
        viewModelScope.launch {
            val localRes = autoBackupManager.runLocalBackupIfNeeded(
                schoolId = school.id,
                schoolCode = school.joinCode,
                students = students.value,
                settings = settings.value,
                force = true
            )
            val csvRes = autoBackupManager.runCsvBackupIfNeeded(
                schoolId = school.id,
                schoolCode = school.joinCode,
                students = students.value,
                settings = settings.value,
                force = true
            )
            when (val driveRes = autoBackupManager.runDriveBackupIfNeeded(
                schoolCode = school.joinCode,
                students = students.value,
                settings = settings.value,
                force = true
            )) {
                is DriveAuthOutcome.Authorized -> {
                    val localOk = localRes.isSuccess
                    val csvOk = csvRes.isSuccess
                    val msg = when {
                        localOk && csvOk -> "লোকাল, Google Drive ও CSV — ৩টি ব্যাকআপ সম্পন্ন হয়েছে"
                        else -> "Drive সম্পন্ন (লোকাল: ${if (localOk) "সফল" else "ত্রুটি"}, CSV: ${if (csvOk) "সফল" else "ত্রুটি"})"
                    }
                    showMessage(msg)
                }
                is DriveAuthOutcome.NeedsConsent -> {
                    _pendingDriveConsent.value = driveRes.intentSender
                }
                is DriveAuthOutcome.Failed -> {
                    val done = buildList {
                        if (localRes.isSuccess) add("লোকাল")
                        if (csvRes.isSuccess) add("CSV")
                    }
                    if (done.isNotEmpty()) {
                        showMessage("${done.joinToString(" ও ")} ব্যাকআপ হয়েছে। Drive ত্রুটি: ${driveRes.message}")
                    } else {
                        showMessage(driveRes.message)
                    }
                }
            }
        }
    }

    fun clearPendingDriveConsent() {
        _pendingDriveConsent.value = null
    }

    fun handleDriveConsentIntentResult(data: Intent?) {
        _pendingDriveConsent.value = null
        val school = activeSchool.value ?: return
        viewModelScope.launch {
            when (val outcome = GoogleDriveBackupClient.handleConsentResult(appContext, data)) {
                is DriveAuthOutcome.Authorized -> {
                    val json = Repo.toJson(students.value, settings.value)
                    autoBackupManager.uploadWithToken(outcome.accessToken, school.joinCode, json)
                        .onSuccess { uploaded ->
                            showMessage("Google Drive-এ ব্যাকআপ হয়েছে: ${uploaded.name}")
                        }
                        .onFailure { err ->
                            showMessage(err.localizedMessage ?: "Google Drive আপলোড ব্যর্থ")
                        }
                }
                is DriveAuthOutcome.Failed -> showMessage(outcome.message)
                is DriveAuthOutcome.NeedsConsent -> showMessage("Google Drive অনুমতি সম্পন্ন হয়নি")
            }
        }
    }

    fun restoreLocalSnapshot(path: String) {
        viewModelScope.launch {
            autoBackupManager.readLocalSnapshot(path)
                .onSuccess { json -> restoreBackupJson(json) }
                .onFailure { e -> showMessage("লোকাল ব্যাকআপ পড়া যায়নি: ${e.localizedMessage ?: "ত্রুটি"}") }
        }
    }

    fun loadDriveBackupList() {
        viewModelScope.launch {
            _driveFilesLoading.value = true
            when (val auth = GoogleDriveBackupClient.authorizeDrive(appContext)) {
                is DriveAuthOutcome.Authorized -> {
                    GoogleDriveBackupClient.listBackupFiles(auth.accessToken)
                        .onSuccess { files -> _driveFiles.value = files }
                        .onFailure { err -> showMessage(err.localizedMessage ?: "Drive তালিকা লোড হয়নি") }
                }
                is DriveAuthOutcome.NeedsConsent -> {
                    _pendingDriveConsent.value = auth.intentSender
                }
                is DriveAuthOutcome.Failed -> {
                    showMessage(auth.message)
                }
            }
            _driveFilesLoading.value = false
        }
    }

    fun restoreFromDriveFile(fileId: String) {
        viewModelScope.launch {
            _driveFilesLoading.value = true
            when (val auth = GoogleDriveBackupClient.authorizeDrive(appContext)) {
                is DriveAuthOutcome.Authorized -> {
                    GoogleDriveBackupClient.downloadBackupJson(auth.accessToken, fileId)
                        .onSuccess { json -> restoreBackupJson(json) }
                        .onFailure { err -> showMessage(err.localizedMessage ?: "Drive ফাইল ডাউনলোড হয়নি") }
                }
                is DriveAuthOutcome.NeedsConsent -> {
                    _pendingDriveConsent.value = auth.intentSender
                }
                is DriveAuthOutcome.Failed -> showMessage(auth.message)
            }
            _driveFilesLoading.value = false
        }
    }

    // ---------- AI Assistant & Drafting ----------

    fun setAiAssistantTab(tab: Int) {
        _aiState.update { it.copy(activeTab = tab.coerceIn(0, 1)) }
    }

    fun openAiScanner() {
        _editingStudent.value = null
        _aiState.update { it.copy(activeTab = 1) }
        _currentSection.value = NavSection.ASSISTANT
    }

    fun setAiModelMode(mode: AiModelMode) {
        _aiState.update { it.copy(modelMode = mode) }
    }

    fun setAiContextStudent(studentId: String?) {
        _aiState.update { it.copy(contextStudentId = studentId) }
    }

    fun clearAiChat() {
        _aiState.update {
            it.copy(
                chatMessages = listOf(
                    AiChatMessage(
                        id = UUID.randomUUID().toString(),
                        isUser = false,
                        text = "নতুন কথোপকথন শুরু হয়েছে। শ্রেণিকক্ষ বা ফলাফল সংক্রান্ত যেকোনো প্রশ্ন করুন।",
                        modelLabel = "Gemini AI",
                        isPlaceholder = false
                    )
                ),
                chatError = null
            )
        }
    }

    private fun buildStudentAiContext(s: Student): String {
        val pct = s.overallPct()
        val grade = gradeOf(pct)
        val hasEval = (1..3).any { s.hasTerm(it) }
        return buildString {
            append("নাম: ${s.name}, শ্রেণি: ${CLSN[s.cls - 1]}, রোল: ${s.roll}, ধর্ম: ${s.religion}")
            if (s.guardian.isNotBlank()) append(", অভিভাবক: ${s.guardian}")
            if (hasEval) append(", গড়: ${fmtEn(pct)}% ($grade - ${gradeLabelBn(grade)})")
        }
    }

    fun sendChatMessage(promptText: String) {
        val clean = promptText.trim()
        if (clean.isEmpty() || _aiState.value.chatLoading) return
        val userMsg = AiChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = clean
        )
        val selectedStudent = _aiState.value.contextStudentId?.let { id ->
            students.value.find { it.id == id }
        }
        val contextText = selectedStudent?.let { buildStudentAiContext(it) }
        val prevHistory = _aiState.value.chatMessages
        val mode = _aiState.value.modelMode

        _aiState.update {
            it.copy(
                chatMessages = it.chatMessages + userMsg,
                chatLoading = true,
                chatError = null
            )
        }

        viewModelScope.launch {
            try {
                val reply = aiRepository.chat(prevHistory, clean, mode, contextText)
                val botMsg = AiChatMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = reply.text,
                    modelLabel = reply.modelLabel,
                    isPlaceholder = reply.isPlaceholder
                )
                _aiState.update {
                    it.copy(
                        chatMessages = it.chatMessages + botMsg,
                        chatLoading = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiState.update {
                    it.copy(
                        chatLoading = false,
                        chatError = e.message ?: "AI উত্তর তৈরি করা যায়নি"
                    )
                }
            }
        }
    }

    fun loadMarksheetImage(uri: Uri) {
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                decodeAndScaleBitmap(uri, 2200)
            }
            if (bmp == null) {
                showMessage("ছবি পড়া যায়নি")
            } else {
                _aiState.update {
                    it.copy(
                        activeTab = 1,
                        selectedImageBitmap = bmp,
                        analysisResult = null,
                        classSheetResult = null,
                        analysisError = null
                    )
                }
            }
        }
    }

    fun clearMarksheetImage() {
        _aiState.update {
            it.copy(
                selectedImageBitmap = null,
                analysisResult = null,
                classSheetResult = null,
                analysisError = null
            )
        }
    }

    fun analyzeSelectedMarksheet(teacherNote: String) {
        val bmp = _aiState.value.selectedImageBitmap ?: return
        if (_aiState.value.analyzingImage) return
        _aiState.update { it.copy(analyzingImage = true, analysisError = null, classSheetResult = null) }
        viewModelScope.launch {
            try {
                val result = aiRepository.analyzeMarksheetImage(bmp, teacherNote)
                _aiState.update { it.copy(analyzingImage = false, analysisResult = result) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiState.update {
                    it.copy(
                        analyzingImage = false,
                        analysisError = e.message ?: "ছবি বিশ্লেষণ করা যায়নি"
                    )
                }
            }
        }
    }

    fun analyzeClassSheet(
        clsHint: Int?,
        termHint: Int?,
        subKeyHint: String?,
        teacherNote: String
    ) {
        val bmp = _aiState.value.selectedImageBitmap ?: return
        if (_aiState.value.analyzingImage) return
        _aiState.update { it.copy(analyzingImage = true, analysisError = null, analysisResult = null) }
        viewModelScope.launch {
            try {
                val result = aiRepository.analyzeClassSheetImage(bmp, clsHint, termHint, subKeyHint, teacherNote)
                _aiState.update { it.copy(analyzingImage = false, classSheetResult = result) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiState.update {
                    it.copy(
                        analyzingImage = false,
                        analysisError = e.message ?: "নম্বরপত্র স্ক্যান করা যায়নি"
                    )
                }
            }
        }
    }

    fun loadSampleClassSheetPreview(clsHint: Int?, termHint: Int?, subKeyHint: String?) {
        if (_aiState.value.analyzingImage) return
        _aiState.update { it.copy(analyzingImage = true, analysisError = null, analysisResult = null) }
        viewModelScope.launch {
            try {
                val bmp = _aiState.value.selectedImageBitmap ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                val result = sampleAiRepository.analyzeClassSheetImage(bmp, clsHint, termHint, subKeyHint, "")
                _aiState.update { it.copy(analyzingImage = false, classSheetResult = result) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiState.update {
                    it.copy(analyzingImage = false, analysisError = e.message ?: "নমুনা প্রিভিউ লোড হয়নি")
                }
            }
        }
    }

    fun updateClassSheetMeta(cls: Int, term: Int, subKey: String) {
        _aiState.update { st ->
            val current = st.classSheetResult ?: return@update st
            st.copy(classSheetResult = current.copy(cls = cls.coerceIn(1, 5), term = term.coerceIn(1, 3), subKey = subKey))
        }
    }

    fun toggleScannedRowSelected(rowId: String) {
        _aiState.update { st ->
            val current = st.classSheetResult ?: return@update st
            st.copy(
                classSheetResult = current.copy(
                    rows = current.rows.map { r -> if (r.id == rowId) r.copy(selected = !r.selected) else r }
                )
            )
        }
    }

    fun setAllScannedRowsSelected(selected: Boolean) {
        _aiState.update { st ->
            val current = st.classSheetResult ?: return@update st
            st.copy(
                classSheetResult = current.copy(
                    rows = current.rows.map { r -> r.copy(selected = selected) }
                )
            )
        }
    }

    fun updateScannedRowField(
        rowId: String,
        rollText: String? = null,
        name: String? = null,
        markEntry: Pair<String, String>? = null,
        verifyField: String? = null
    ) {
        _aiState.update { st ->
            val current = st.classSheetResult ?: return@update st
            val nextRows = current.rows.map { r ->
                if (r.id != rowId) r
                else {
                    val editedKey = markEntry?.first
                    val editedVal = markEntry?.second

                    var nextMarks = r.marksText
                    var nextCont = r.sheetContinuousTotal
                    var nextGrand = r.sheetGrandTotal
                    val nextDeduced = r.deducedFields.toMutableMap()
                    val nextResolved = r.resolvedFields.toMutableSet()

                    if (rollText != null) nextResolved.add("roll")
                    if (name != null) nextResolved.add("name")

                    if (editedKey != null && editedVal != null) {
                        when (editedKey) {
                            "CONT" -> nextCont = editedVal.ifBlank { null }
                            "GRAND" -> nextGrand = editedVal.ifBlank { null }
                            else -> {
                                nextMarks = r.marksText.toMutableMap().apply {
                                    put(editedKey, editedVal)
                                }
                            }
                        }
                        // ব্যবহারকারী নিজে এডিট করলে আগের অনুমান মুছে নতুন হিসাব চালু হবে
                        nextDeduced.remove(editedKey)
                        nextResolved.add(editedKey)
                        nextResolved.remove("arithmetic")

                        // আগে থেকে স্বয়ংক্রিয় হিসাবকৃত CONT/GRAND থাকলে সেগুলো পুনরায় হিসাবের সুযোগ দিন
                        if (editedKey != "CONT" && "CONT" in nextDeduced) {
                            nextCont = null
                            nextDeduced.remove("CONT")
                            nextResolved.remove("CONT")
                        }
                        if (editedKey != "GRAND" && "GRAND" in nextDeduced) {
                            nextGrand = null
                            nextDeduced.remove("GRAND")
                            nextResolved.remove("GRAND")
                        }
                        // যদি user CONT বা GRAND এডিট করেন এবং WR স্বয়ংক্রিয় অনুমান ছিল, তবে পুনরায় অনুমান করুন
                        if ((editedKey == "CONT" || editedKey == "GRAND") && "WR" in nextDeduced) {
                            nextMarks = nextMarks.toMutableMap().apply { remove("WR") }
                            nextDeduced.remove("WR")
                            nextResolved.remove("WR")
                        }
                    }

                    var nextUseScannedName = r.useScannedNameForExisting
                    if (verifyField == "toggle_use_scanned_name") {
                        nextUseScannedName = !nextUseScannedName
                    } else if (verifyField != null) {
                        nextResolved.add(verifyField)
                    }

                    val updatedRow = r.copy(
                        rollText = rollText ?: r.rollText,
                        name = name ?: r.name,
                        marksText = nextMarks,
                        sheetContinuousTotal = nextCont,
                        sheetGrandTotal = nextGrand,
                        resolvedFields = nextResolved,
                        deducedFields = nextDeduced,
                        useScannedNameForExisting = nextUseScannedName
                    )
                    if (editedKey != null) {
                        ScanConstraintSolver.reconcileInterdependencies(updatedRow, current.cls, current.subKey)
                    } else {
                        updatedRow
                    }
                }
            }
            st.copy(classSheetResult = current.copy(rows = nextRows))
        }
    }

    fun addBlankScannedRow() {
        _aiState.update { st ->
            val current = st.classSheetResult ?: return@update st
            val maxRoll = current.rows.mapNotNull { fromBn(it.rollText.trim()).toIntOrNull() }.maxOrNull() ?: 0
            val newRow = ScannedStudentRow(
                rollText = (maxRoll + 1).toString(),
                name = "",
                marksText = emptyMap()
            )
            st.copy(classSheetResult = current.copy(rows = current.rows + newRow))
        }
    }

    fun removeScannedRow(rowId: String) {
        _aiState.update { st ->
            val current = st.classSheetResult ?: return@update st
            st.copy(classSheetResult = current.copy(rows = current.rows.filterNot { it.id == rowId }))
        }
    }

    fun markAllScannedIssuesVerified() {
        _aiState.update { st ->
            val current = st.classSheetResult ?: return@update st
            val nextRows = current.rows.map { r ->
                val allKeys = r.aiIssues.keys + setOf(
                    "roll", "name", "TB", "ACT", "LANG", "CT", "WR", "TOTAL", "CONT", "GRAND", "arithmetic"
                )
                r.copy(resolvedFields = r.resolvedFields + allKeys)
            }
            st.copy(classSheetResult = current.copy(rows = nextRows))
        }
    }

    fun dismissClassSheetPreview() {
        _aiState.update { it.copy(classSheetResult = null) }
    }

    fun commitScannedClassSheet() {
        val sheet = _aiState.value.classSheetResult ?: return
        val school = activeSchool.value ?: return
        if (_aiState.value.savingClassSheet) return

        val perm = school.permissionsFor(currentUserId)
        val cls = sheet.cls.coerceIn(1, 5)
        val term = sheet.term.coerceIn(1, 3)
        val subKey = sheet.subKey

        if (!perm.canAccessClass(cls)) {
            showMessage("এই শ্রেণিতে (${CLSN[cls - 1]}) ডেটা সংরক্ষণের অনুমতি আপনার নেই")
            return
        }
        if (!perm.canEditSubject(subKey)) {
            showMessage("এই বিষয়ে নম্বর পরিবর্তনের অনুমতি আপনার নেই")
            return
        }

        val selectedRows = sheet.rows.filter { it.selected }
        if (selectedRows.isEmpty()) {
            showMessage("সংরক্ষণের জন্য অন্তত একজন শিক্ষার্থী নির্বাচন করুন")
            return
        }

        val prevList = students.value
        val rollCounts = selectedRows.mapNotNull { fromBn(it.rollText.trim()).toIntOrNull() }.groupingBy { it }.eachCount()
        val duplicates = rollCounts.filterValues { it > 1 }.keys
        val hasHardError = selectedRows.any { row ->
            val rInt = fromBn(row.rollText.trim()).toIntOrNull()
            val hasDbName = rInt != null && prevList.any { it.cls == cls && it.roll == rInt && it.name.isNotBlank() }
            validateScannedStudentRow(row, cls, subKey, duplicates, hasDbName).hasHardError
        }
        if (hasHardError) {
            showMessage("লাল চিহ্নিত ত্রুটিপূর্ণ ঘরগুলো ঠিক করে আবার সংরক্ষণ করুন")
            return
        }

        val out = prevList.toMutableList()
        var addedCount = 0
        var updatedCount = 0
        var skippedNoPerm = 0

        selectedRows.forEach { row ->
            val roll = fromBn(row.rollText.trim()).toIntOrNull() ?: return@forEach
            val pos = out.indexOfFirst { it.cls == cls && it.roll == roll }
            if (pos < 0 && !perm.canManageStudents) {
                skippedNoPerm++
                return@forEach
            }
            val base = if (pos >= 0) out[pos] else createEmptyStudent(cls, roll)
            val nextM = base.m.toMutableMap()
            val subDef = subjectsFor(cls, RELIGIONS.first()).find { it.key == subKey }
            val compKeys = subDef?.comps?.map { it.key } ?: emptyList()
            val hasWritten = (subDef?.written ?: 0) > 0
            val isArt = compKeys == listOf("TOTAL") && !hasWritten
            val hasBreakdown = compKeys.any { !row.marksText[it]?.trim().isNullOrEmpty() }

            if (isArt) {
                val totalRaw = row.marksText["TOTAL"]?.trim().takeIf { !it.isNullOrEmpty() }
                    ?: row.sheetGrandTotal?.trim().orEmpty()
                val fullKey = mk(subKey, term, "TOTAL")
                if (totalRaw.isEmpty()) nextM.remove(fullKey)
                else fromBn(totalRaw).toDoubleOrNull()?.let { nextM[fullKey] = it }
            } else if (hasBreakdown) {
                nextM.remove(mk(subKey, term, "CONT"))
                compKeys.forEach { compKey ->
                    val raw = row.marksText[compKey]?.trim().orEmpty()
                    val fullKey = mk(subKey, term, compKey)
                    if (raw.isEmpty()) {
                        nextM.remove(fullKey)
                    } else {
                        fromBn(raw).toDoubleOrNull()?.let { d -> nextM[fullKey] = d }
                    }
                }
            } else {
                // রেজাল্ট কার্ড মোড (কেবল ধারাবাহিক মোট, সামষ্টিক ও মোট দিয়ে সংরক্ষণ)
                compKeys.forEach { nextM.remove(mk(subKey, term, it)) }
                val rawCont = row.sheetContinuousTotal?.trim().takeIf { !it.isNullOrEmpty() }
                    ?: if (!hasWritten) row.sheetGrandTotal?.trim().orEmpty() else ""
                val contKey = mk(subKey, term, "CONT")
                if (rawCont.isEmpty()) {
                    nextM.remove(contKey)
                } else {
                    fromBn(rawCont).toDoubleOrNull()?.let { d -> nextM[contKey] = d }
                }
            }

            if (hasWritten) {
                val rawWr = row.marksText["WR"]?.trim().orEmpty()
                val wrKey = mk(subKey, term, "WR")
                if (rawWr.isEmpty()) {
                    nextM.remove(wrKey)
                } else {
                    fromBn(rawWr).toDoubleOrNull()?.let { d -> nextM[wrKey] = d }
                }
            }

            val cleanScannedName = row.name.trim()
            val shouldKeepDbName = pos >= 0 && base.name.isNotBlank() && !row.useScannedNameForExisting
            val candidate = if (!shouldKeepDbName && perm.canManageStudents && cleanScannedName.isNotBlank()) {
                base.copy(name = cleanScannedName, m = nextM)
            } else {
                base.copy(m = nextM)
            }
            val safe = if (school.isAdmin(currentUserId)) {
                candidate
            } else {
                sanitizeStudentForPermissions(candidate, if (pos >= 0) base else null, perm)
            }
            if (safe != null) {
                if (pos >= 0) {
                    out[pos] = safe
                    updatedCount++
                } else {
                    out.add(safe)
                    addedCount++
                }
            }
        }

        val existingById = prevList.associateBy { it.id }
        val changedList = out.filter { s -> existingById[s.id] != s }
        if (changedList.isEmpty()) {
            showMessage(
                if (skippedNoPerm > 0) "নতুন শিক্ষার্থী যোগ করার অনুমতি নেই (শুধু তালিকার শিক্ষার্থীদের নম্বর আপডেট হবে)"
                else "কোনো পরিবর্তিত ডেটা পাওয়া যায়নি"
            )
            return
        }

        _aiState.update { it.copy(savingClassSheet = true) }
        viewModelScope.launch {
            cloudRepo.syncBatchStudents(school.id, prevList, out, deleteRemoved = false)
                .onSuccess {
                    _selectedClass.value = cls
                    _aiState.update { it.copy(savingClassSheet = false, classSheetResult = null) }
                    val msg = buildString {
                        append("স্ক্যান ডেটা সংরক্ষিত: ")
                        if (addedCount > 0) append("নতুন ${bn(addedCount.toString())} জন")
                        if (addedCount > 0 && updatedCount > 0) append(", ")
                        if (updatedCount > 0) append("হালনাগাদ ${bn(updatedCount.toString())} জন")
                        if (skippedNoPerm > 0) append(" (${bn(skippedNoPerm.toString())} জন নতুন বাদ পড়েছে)")
                    }
                    showMessage(msg)
                }
                .onFailure { e ->
                    _aiState.update { it.copy(savingClassSheet = false) }
                    showMessage(friendlyFirestoreErrorMessage(e))
                }
        }
    }

    fun applyExtractedMarksheet(ext: ExtractedMarksheet): Student {
        val perm = activePermissions.value
        val cls = (ext.student.cls ?: _selectedClass.value).coerceIn(1, 5)
        val roll = ext.student.roll?.takeIf { it > 0 }
            ?: ((students.value.filter { it.cls == cls }.maxOfOrNull { it.roll } ?: 0) + 1)

        val existing = students.value.find { it.cls == cls && it.roll == roll }
        if (!perm.canAccessClass(cls)) {
            showMessage("এই শ্রেণিতে (${CLSN[cls - 1]}) নম্বর বা তথ্য সংরক্ষণের অনুমতি আপনার নেই")
            return existing ?: createEmptyStudent(cls, roll)
        }
        if (existing == null && !perm.canManageStudents) {
            showMessage("নতুন শিক্ষার্থী যোগ করার অনুমতি আপনার নেই")
            return createEmptyStudent(cls, roll)
        }
        val base = existing ?: createEmptyStudent(cls, roll)

        val nextM = base.m.toMutableMap()
        ext.marks.forEach { item ->
            val sub = item.key.substringBefore('_')
            if (perm.canEditSubject(sub)) {
                nextM[item.key] = item.value
            }
        }

        val t = ext.term.coerceIn(1, 3)
        val nextX = base.x.toMutableMap()
        if (perm.canEditAttendance) {
            ext.attendanceAndTraits.days?.takeIf { it.isNotBlank() }?.let { nextX["DAYS_$t"] = it }
            ext.attendanceAndTraits.present?.takeIf { it.isNotBlank() }?.let { nextX["PRES_$t"] = it }
            ext.attendanceAndTraits.act?.takeIf { it.isNotBlank() }?.let { nextX["ACT_$t"] = it }
            ext.attendanceAndTraits.disc?.takeIf { it.isNotBlank() }?.let { nextX["DISC_$t"] = it }
            ext.attendanceAndTraits.cond?.takeIf { it.isNotBlank() }?.let { nextX["COND_$t"] = it }
            ext.attendanceAndTraits.remark?.takeIf { it.isNotBlank() }?.let { nextX["REM_$t"] = it }
        }

        val updated = if (perm.canManageStudents) {
            base.copy(
                name = ext.student.name?.takeIf { it.isNotBlank() } ?: base.name,
                guardian = ext.student.guardian?.takeIf { it.isNotBlank() } ?: base.guardian,
                village = ext.student.village?.takeIf { it.isNotBlank() } ?: base.village,
                union = ext.student.union?.takeIf { it.isNotBlank() } ?: base.union,
                upazila = (ext.student.upazila?.takeIf { it.isNotBlank() } ?: base.upazila).ifBlank { DEFAULT_UPAZILA },
                district = (ext.student.district?.takeIf { it.isNotBlank() } ?: base.district).ifBlank { DEFAULT_DISTRICT },
                mobile = ext.student.mobile?.takeIf { it.isNotBlank() } ?: base.mobile,
                religion = ext.student.religion?.takeIf { it.isNotBlank() } ?: base.religion,
                m = nextM,
                x = nextX
            )
        } else {
            base.copy(m = nextM, x = nextX)
        }
        upsertStudent(updated)
        _selectedClass.value = cls
        showMessage("${updated.name.ifBlank { "রোল ${bn(roll.toString())}" }} শিক্ষার্থীর অনুমোদিত তথ্য সংরক্ষিত হয়েছে")
        return updated
    }

    private fun applyDiscoveredModels(fetched: List<DiscoveredGeminiModel>) {
        if (fetched.isEmpty()) return
        val prevIds = _geminiAvailableModels.value.map { it.id }.toSet()
        val newlyDetected = if (prevIds.isNotEmpty()) {
            fetched.map { it.id }.filter { it !in prevIds && it !in GeminiApiConfig.SUGGESTED_MODEL_IDS }.toSet()
        } else {
            emptySet()
        }
        if (newlyDetected.isNotEmpty()) {
            _geminiNewModelIds.value = _geminiNewModelIds.value + newlyDetected
        }
        _geminiAvailableModels.value = fetched
        GeminiApiConfig.saveCachedModels(appContext, fetched)
    }

    fun refreshGeminiModels(rawApiKey: String? = null, silent: Boolean = false) {
        if (_geminiRefreshingModels.value) return
        val keyToUse = GeminiApiConfig.normalizeApiKey(rawApiKey ?: _geminiApiKey.value)
        if (keyToUse.isBlank()) {
            if (!silent) {
                _geminiModelsError.value = "নতুন মডেল লোড করতে আগে Gemini API Key লিখুন।"
            }
            return
        }
        _geminiRefreshingModels.value = true
        if (!silent) _geminiModelsError.value = null
        viewModelScope.launch {
            DirectGeminiAiRepository.fetchModelsOnly(keyToUse)
                .onSuccess { fetched ->
                    _geminiModelsError.value = null
                    applyDiscoveredModels(fetched)
                }
                .onFailure { e ->
                    if (!silent) {
                        _geminiModelsError.value = e.message ?: "লাইভ মডেল তালিকা লোড করা যায়নি।"
                    }
                }
            _geminiRefreshingModels.value = false
        }
    }

    fun removeCustomGeminiModel(modelId: String) {
        _geminiCustomModels.value = GeminiApiConfig.removeCustomModel(appContext, modelId)
    }

    fun selectGeminiModel(rawModel: String) {
        val cleanModel = GeminiApiConfig.normalizeModelId(rawModel)
        GeminiApiConfig.saveConfig(appContext, _geminiApiKey.value, cleanModel)
        val savedModel = GeminiApiConfig.getSelectedModel(appContext)
        _geminiModel.value = savedModel
        _geminiCustomModels.value = GeminiApiConfig.getCustomModels(appContext)
        showMessage("Gemini মডেল পরিবর্তন হয়েছে: $savedModel")
    }

    fun saveGeminiConfig(rawApiKey: String, rawModel: String) {
        GeminiApiConfig.saveConfig(appContext, rawApiKey, rawModel)
        val savedKey = GeminiApiConfig.getApiKey(appContext)
        val savedModel = GeminiApiConfig.getSelectedModel(appContext)
        _geminiApiKey.value = savedKey
        _geminiModel.value = savedModel
        _geminiCustomModels.value = GeminiApiConfig.getCustomModels(appContext)
        _geminiTestStatus.value = null
        _geminiTestSuccess.value = null
        if (savedKey.isNotBlank()) {
            refreshGeminiModels(savedKey, silent = true)
        }
        showMessage(
            if (savedKey.isNotBlank()) "Gemini API Key এবং মডেল ($savedModel) সংরক্ষিত হয়েছে"
            else "মডেল ($savedModel) সংরক্ষিত হয়েছে; AI চালাতে API Key দিন"
        )
    }

    fun clearGeminiApiKey() {
        GeminiApiConfig.clearApiKey(appContext)
        _geminiApiKey.value = ""
        _geminiAvailableModels.value = emptyList()
        _geminiNewModelIds.value = emptySet()
        _geminiModelsError.value = null
        _geminiTestStatus.value = null
        _geminiTestSuccess.value = null
        showMessage("Gemini API Key মুছে ফেলা হয়েছে")
    }

    fun testGeminiConnection(rawApiKey: String, rawModel: String) {
        if (_geminiTesting.value) return
        val cleanKey = GeminiApiConfig.normalizeApiKey(rawApiKey)
        val cleanModel = GeminiApiConfig.normalizeModelId(rawModel)
        if (cleanKey.isBlank()) {
            _geminiTestSuccess.value = false
            _geminiTestStatus.value = "পরীক্ষার জন্য আপনার Gemini API Key লিখুন।"
            return
        }
        _geminiTesting.value = true
        _geminiTestStatus.value = null
        _geminiTestSuccess.value = null
        _geminiModelsError.value = null
        viewModelScope.launch {
            val result = DirectGeminiAiRepository.testConnectionAndListModels(cleanKey, cleanModel)
            _geminiTestSuccess.value = result.ok
            _geminiTestStatus.value = result.message
            if (result.availableModels.isNotEmpty()) {
                applyDiscoveredModels(result.availableModels)
            }
            if (result.ok) {
                GeminiApiConfig.saveConfig(appContext, cleanKey, cleanModel)
                _geminiApiKey.value = cleanKey
                _geminiModel.value = cleanModel
                _geminiCustomModels.value = GeminiApiConfig.getCustomModels(appContext)
            }
            _geminiTesting.value = false
        }
    }

    fun draftRemark(student: Student, term: Int, onResult: (String) -> Unit) {
        if (_draftingRemarkTerm.value != null) return
        _draftingRemarkTerm.value = term
        viewModelScope.launch {
            try {
                val res = aiRepository.draftRemark(buildStudentAiContext(student), term)
                onResult(res.remark)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showMessage("খসড়া তৈরি করা যায়নি: ${e.message ?: "ত্রুটি"}")
            } finally {
                _draftingRemarkTerm.value = null
            }
        }
    }

    private fun decodeAndScaleBitmap(uri: Uri, maxDim: Int): Bitmap? {
        return try {
            val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val w = info.size.width
                    val h = info.size.height
                    val longest = maxOf(w, h)
                    if (longest > maxDim) {
                        val ratio = maxDim.toFloat() / longest
                        decoder.setTargetSize((w * ratio).toInt().coerceAtLeast(1), (h * ratio).toInt().coerceAtLeast(1))
                    }
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } ?: return null

            val longest = maxOf(raw.width, raw.height)
            if (longest <= maxDim) raw
            else {
                val ratio = maxDim.toFloat() / longest
                Bitmap.createScaledBitmap(raw, (raw.width * ratio).toInt().coerceAtLeast(1), (raw.height * ratio).toInt().coerceAtLeast(1), true)
            }
        } catch (_: Exception) {
            null
        }
    }
}
