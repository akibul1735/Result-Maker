package com.kgps.result

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import com.kgps.result.ai.AiChatMessage
import com.kgps.result.ai.AiModelMode
import com.kgps.result.ai.AiRepository
import com.kgps.result.ai.AutoScanResult
import com.kgps.result.ai.ClassSheetImportScope
import com.kgps.result.ai.ClassSheetKind
import com.kgps.result.ai.DirectGeminiAiRepository
import com.kgps.result.ai.DiscoveredGeminiModel
import com.kgps.result.ai.ExtractedClassSheet
import com.kgps.result.ai.ExtractedMarksheet
import com.kgps.result.ai.GeminiApiConfig
import com.kgps.result.ai.PlaceholderAiRepository
import com.kgps.result.ai.PreparedImageStats
import com.kgps.result.ai.ScanConstraintSolver
import com.kgps.result.ai.ScannedStudentRow
import com.kgps.result.ai.SheetQrScanner
import com.kgps.result.ai.SheetScanDetection
import com.kgps.result.ai.VisionImageOptimizer
import com.kgps.result.ai.validateScannedStudentRow
import com.kgps.result.backup.AutoBackupManager
import com.kgps.result.backup.AutoBackupStatus
import com.kgps.result.backup.BackupFileRetentionMode
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
import com.kgps.result.cloud.SchoolCodePreview
import com.kgps.result.cloud.SchoolDataGateway
import com.kgps.result.cloud.SchoolWorkspace
import com.kgps.result.cloud.SheetTemplateCache
import com.kgps.result.cloud.StudentSyncMeta
import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.cloud.UserCloudProfile
import com.kgps.result.cloud.friendlyFirestoreErrorMessage
import com.kgps.result.ui.screens.BatchStudentUpdatePatch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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
    DASHBOARD("dashboard", "ড্যাশবোর্ড"),
    ROSTER("roster", "শিক্ষার্থী তালিকা"),
    SHEET("sheet", "ব্যাচ এন্ট্রি"),
    STUDIO("studio", "রিপোর্ট স্টুডিও"),
    BLANK_SHEET("blank_sheet", "ফাঁকা ফর্ম / শিট PDF"),
    SEAT_PLAN("seat_plan", "আসন বিন্যাস (সিট প্ল্যান)"),
    TABULATION("tabulation", "ফলাফল টেবিল PDF"),
    ADMIT_CARD("admit_card", "প্রবেশপত্র PDF"),
    TESTIMONIAL("testimonial", "প্রশংসাপত্র মেকার"),
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
    val documentEnhance: Boolean = true,
    val preparedImageStats: PreparedImageStats? = null,
    val hasOriginalImageBackup: Boolean = false,
    val detectedSheetQr: BlankSheetQrMeta? = null,
    val sheetDetectionSource: String? = null,
    val sheetDetection: SheetScanDetection? = null,
    val resolvedTemplateMeta: BlankSheetQrMeta? = null,
    val resolvedTemplateSource: String? = null,
    val resolvingSheetTemplate: Boolean = false,
    val loadingImage: Boolean = false,
    val analyzingImage: Boolean = false,
    val analysisError: String? = null,
    val analysisResult: ExtractedMarksheet? = null,
    val classSheetResult: ExtractedClassSheet? = null,
    val savingClassSheet: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
class ResultMakerViewModel(
    private val appContext: Context,
    private val cloudRepo: SchoolDataGateway,
    private val autoBackupManager: AutoBackupManager,
    private val contentResolver: ContentResolver,
    val currentUserId: String,
    val currentUserDisplayName: String,
    val currentUserEmail: String,
    private val initialLocalSnapshot: Pair<List<Student>, Settings>? = null,
    val isLocalPreviewMode: Boolean = false
) : ViewModel() {

    private val directAiRepository = DirectGeminiAiRepository(appContext)
    private val aiRepository: AiRepository get() = directAiRepository
    private val sampleAiRepository = PlaceholderAiRepository()
    private val sheetTemplateCache = SheetTemplateCache(appContext)
    private val syncedTemplateIdsInSession = mutableSetOf<String>()
    private val preloadedTemplateSchoolKeys = mutableSetOf<String>()
    private var originalMarksheetBitmap: Bitmap? = null
    private var loadImageJob: Job? = null

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

    private data class StudentsStreamState(
        val loadedSchoolId: String? = null,
        val hadError: Boolean = false,
        val students: List<Student> = emptyList(),
        val meta: Map<String, StudentSyncMeta> = emptyMap()
    )

    private sealed interface PendingDriveAction {
        data object UploadOnly : PendingDriveAction
        data object EnableAutoAndUpload : PendingDriveAction
        data object ListFiles : PendingDriveAction
        data class RestoreFile(val fileId: String) : PendingDriveAction
    }

    private val _pendingDriveConsent = MutableStateFlow<IntentSender?>(null)
    val pendingDriveConsent: StateFlow<IntentSender?> = _pendingDriveConsent.asStateFlow()
    private val _pendingDriveAction = MutableStateFlow<PendingDriveAction?>(null)
    private var driveAutoBackupJob: Job? = null

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

    private val studentsAndMetaFlow: StateFlow<StudentsStreamState> =
        combine(
            activeSchool.map { it?.id }.distinctUntilChanged(),
            _cloudRetryTrigger
        ) { schoolId, _ -> schoolId }
            .flatMapLatest { schoolId ->
                if (schoolId.isNullOrBlank()) {
                    flowOf(StudentsStreamState(loadedSchoolId = null, hadError = false))
                } else {
                    cloudRepo.observeSchoolStudents(schoolId)
                        .map { (list, meta) ->
                            StudentsStreamState(
                                loadedSchoolId = schoolId,
                                hadError = false,
                                students = list,
                                meta = meta
                            )
                        }
                        .catch { e ->
                            val fsErr = unwrapFirestoreError(e)
                            if (fsErr == null && e is CancellationException) throw e
                            if (!_isSigningOut.value && _suppressedSchoolId.value != schoolId) {
                                _snackbarMessage.value = friendlyFirestoreErrorMessage(fsErr ?: e)
                            }
                            emit(StudentsStreamState(loadedSchoolId = null, hadError = true))
                        }
                }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, StudentsStreamState())

    val students: StateFlow<List<Student>> = studentsAndMetaFlow
        .map { it.students }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val studentSyncMeta: StateFlow<Map<String, StudentSyncMeta>> = studentsAndMetaFlow
        .map { it.meta }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _localPdfFontOverride = MutableStateFlow(Repo.loadPdfFontOverride(appContext))
    private val _localPdfEnglishFontOverride = MutableStateFlow(Repo.loadPdfEnglishFontOverride(appContext))
    private val _localSchoolAddressOverride = MutableStateFlow(Repo.loadSchoolAddressOverride(appContext))
    private val _optimisticResultRules = MutableStateFlow<ResultEvaluationRules?>(null)

    val settings: StateFlow<Settings> = combine(
        activeSchool,
        _localPdfFontOverride,
        _localPdfEnglishFontOverride,
        _localSchoolAddressOverride,
        _optimisticResultRules
    ) { school, localFont, localEnFont, localAddress, optimisticRules ->
        val base = school?.settings ?: DEFAULT_SETTINGS
        val decodedBase = decodePdfFontPair(base.pdfFont, base.pdfEnglishFont)
        val effectiveFont = localFont?.takeIf { id -> BENGALI_PDF_FONT_LIST.any { it.id == id } }
            ?: decodedBase.bengaliFontId
        val effectiveEnFont = localEnFont?.takeIf { id -> ENGLISH_PDF_FONT_LIST.any { it.id == id } }
            ?: decodedBase.englishFontId
        val effectiveAddress = localAddress ?: base.schoolAddress
        val st = base.copy(
            pdfFont = effectiveFont,
            pdfEnglishFont = effectiveEnFont,
            schoolAddress = effectiveAddress,
            resultRules = optimisticRules ?: base.resultRules
        )
        SchoolClassRegistry.update(st.customClasses)
        st
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_SETTINGS)

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

    private val _currentSection = MutableStateFlow(NavSection.DASHBOARD)
    val currentSection: StateFlow<NavSection> = _currentSection.asStateFlow()

    private val _editingStudent = MutableStateFlow<Student?>(null)
    val editingStudent: StateFlow<Student?> = _editingStudent.asStateFlow()

    private val _editReturnSection = MutableStateFlow(NavSection.DASHBOARD)
    val editReturnSection: StateFlow<NavSection> = _editReturnSection.asStateFlow()

    private val _studioStudentId = MutableStateFlow<String?>(null)
    val studioStudentId: StateFlow<String?> = _studioStudentId.asStateFlow()

    private val _csvImportState = MutableStateFlow(CsvImportModalState())
    val csvImportState: StateFlow<CsvImportModalState> = _csvImportState.asStateFlow()

    private val _basicStudentImportState = MutableStateFlow(CsvImportModalState())
    val basicStudentImportState: StateFlow<CsvImportModalState> = _basicStudentImportState.asStateFlow()

    private val _batchEditOpen = MutableStateFlow(false)
    val batchEditOpen: StateFlow<Boolean> = _batchEditOpen.asStateFlow()

    private val _batchEditInitialClass = MutableStateFlow<Int?>(null)
    val batchEditInitialClass: StateFlow<Int?> = _batchEditInitialClass.asStateFlow()

    private val reportPrefs = appContext.getSharedPreferences("kgps_report_prefs", Context.MODE_PRIVATE)
    private val _includeClassHighestInReport = MutableStateFlow(
        reportPrefs.getBoolean("include_class_highest", false)
    )
    val includeClassHighestInReport: StateFlow<Boolean> = _includeClassHighestInReport.asStateFlow()

    private val _includeSummaryTableInReport = MutableStateFlow(
        reportPrefs.getBoolean("include_summary_table", false)
    )
    val includeSummaryTableInReport: StateFlow<Boolean> = _includeSummaryTableInReport.asStateFlow()

    private val _summaryTableColumns = MutableStateFlow(
        ReportSummaryColumn.deserializeList(reportPrefs.getString("summary_table_columns", null))
    )
    val summaryTableColumns: StateFlow<List<ReportSummaryColumn>> = _summaryTableColumns.asStateFlow()

    private val _showCoverAddressInReport = MutableStateFlow(
        reportPrefs.getBoolean("show_cover_address", true)
    )
    val showCoverAddressInReport: StateFlow<Boolean> = _showCoverAddressInReport.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _aiState = MutableStateFlow(AiAssistantState())
    val aiState: StateFlow<AiAssistantState> = _aiState.asStateFlow()

    private val _draftingRemarkTerm = MutableStateFlow<Int?>(null)
    val draftingRemarkTerm: StateFlow<Int?> = _draftingRemarkTerm.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(GeminiApiConfig.getApiKey(appContext))
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _geminiOrderedModels = MutableStateFlow(GeminiApiConfig.getOrderedModels(appContext))
    val geminiOrderedModels: StateFlow<List<String>> = _geminiOrderedModels.asStateFlow()

    val geminiModel: StateFlow<String> = _geminiOrderedModels
        .map { it.firstOrNull().orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _geminiOrderedModels.value.firstOrNull().orEmpty())

    val geminiFallbackModels: StateFlow<List<String>> = _geminiOrderedModels
        .map { it.drop(1) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _geminiOrderedModels.value.drop(1))

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

        // Pre-sync all school blank sheet templates from Firestore into local offline cache
        viewModelScope.launch {
            combine(
                activeSchool.map { it?.id }.distinctUntilChanged(),
                _cloudRetryTrigger
            ) { schoolId, retry -> schoolId to retry }
                .collectLatest { (schoolId, retry) ->
                    val cleanId = schoolId?.trim().orEmpty()
                    if (cleanId.isEmpty()) return@collectLatest
                    val sessionKey = "$cleanId|$retry"
                    val alreadyPreloaded = synchronized(preloadedTemplateSchoolKeys) {
                        preloadedTemplateSchoolKeys.contains(sessionKey)
                    }
                    if (alreadyPreloaded) return@collectLatest
                    withContext(Dispatchers.IO) {
                        val res = cloudRepo.fetchAllSheetTemplates(cleanId)
                        if (res.isSuccess) {
                            res.getOrNull()?.takeIf { it.isNotEmpty() }?.let { templates ->
                                sheetTemplateCache.putAll(templates)
                            }
                            synchronized(preloadedTemplateSchoolKeys) {
                                preloadedTemplateSchoolKeys.add(sessionKey)
                            }
                        }
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
            combine(activeSchool, studentsAndMetaFlow, settings) { school, streamState, st ->
                Triple(school, streamState, st)
            }
                .debounce(3_500L)
                .collectLatest { (school, streamState, st) ->
                    if (school == null) return@collectLatest
                    if (streamState.hadError || streamState.loadedSchoolId != school.id) return@collectLatest
                    val studentList = streamState.students
                    if (studentList.isEmpty() && st == DEFAULT_SETTINGS) return@collectLatest

                    Repo.save(appContext, studentList, st)
                    BackupWorkScheduler.saveActiveMeta(appContext, currentUserId, school.id, school.joinCode)

                    val reportBackupPrefs = Repo.loadReportBackupPrefs(appContext)
                    val cachedTemplates = sheetTemplateCache.getAll()

                    autoBackupManager.runLocalBackupIfNeeded(
                        schoolId = school.id,
                        schoolCode = school.joinCode,
                        students = studentList,
                        settings = st,
                        force = false,
                        reportPrefs = reportBackupPrefs,
                        sheetTemplates = cachedTemplates
                    )

                    autoBackupManager.runCsvBackupIfNeeded(
                        schoolId = school.id,
                        schoolCode = school.joinCode,
                        students = studentList,
                        settings = st,
                        force = false
                    )

                    if (autoBackupManager.status.value.driveEnabled && driveAutoBackupJob?.isActive != true) {
                        val targetSchoolCode = school.joinCode
                        driveAutoBackupJob = viewModelScope.launch {
                            delay(4_000L)
                            val latestSchool = activeSchool.value ?: return@launch
                            val latestStream = studentsAndMetaFlow.value
                            if (latestStream.hadError || latestStream.loadedSchoolId != latestSchool.id) return@launch
                            autoBackupManager.runDriveBackupIfNeeded(
                                schoolCode = latestSchool.joinCode.ifBlank { targetSchoolCode },
                                students = latestStream.students,
                                settings = settings.value,
                                force = false,
                                enableAutoOnSuccess = false,
                                reportPrefs = Repo.loadReportBackupPrefs(appContext),
                                sheetTemplates = sheetTemplateCache.getAll()
                            )
                        }
                    }
                }
        }

        // Foreground minute ticker so scheduled backups also fire promptly while the app stays open
        viewModelScope.launch {
            while (isActive) {
                delay(60_000L)
                val school = activeSchool.value ?: continue
                val streamState = studentsAndMetaFlow.value
                if (streamState.hadError || streamState.loadedSchoolId != school.id) continue
                val studentList = streamState.students
                val st = settings.value
                if (studentList.isEmpty() && st == DEFAULT_SETTINGS) continue

                val reportBackupPrefs = Repo.loadReportBackupPrefs(appContext)
                val cachedTemplates = sheetTemplateCache.getAll()

                autoBackupManager.runLocalBackupIfNeeded(
                    schoolId = school.id,
                    schoolCode = school.joinCode,
                    students = studentList,
                    settings = st,
                    force = false,
                    reportPrefs = reportBackupPrefs,
                    sheetTemplates = cachedTemplates
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
                        force = false,
                        enableAutoOnSuccess = false,
                        reportPrefs = reportBackupPrefs,
                        sheetTemplates = cachedTemplates
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
        _currentSection.value = NavSection.DASHBOARD
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
                _currentSection.value = NavSection.DASHBOARD
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
        if (cls in 1..20) _selectedClass.value = cls
    }

    fun openSheetForClass(cls: Int) {
        if (cls in 1..20) _selectedClass.value = cls
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

    fun openBlankSheetPdf() {
        if (!activePermissions.value.canExportPdf) {
            showMessage("PDF তৈরি করার অনুমতি আপনার নেই")
            return
        }
        _editingStudent.value = null
        _currentSection.value = NavSection.BLANK_SHEET
    }

    fun openSeatPlanPdf() {
        if (!activePermissions.value.canExportPdf) {
            showMessage("PDF তৈরি করার অনুমতি আপনার নেই")
            return
        }
        _editingStudent.value = null
        _currentSection.value = NavSection.SEAT_PLAN
    }

    fun openTabulationPdf() {
        if (!activePermissions.value.canExportPdf) {
            showMessage("PDF তৈরি করার অনুমতি আপনার নেই")
            return
        }
        _editingStudent.value = null
        _currentSection.value = NavSection.TABULATION
    }

    fun openAdmitCardPdf() {
        if (!activePermissions.value.canExportPdf) {
            showMessage("PDF তৈরি করার অনুমতি আপনার নেই")
            return
        }
        _editingStudent.value = null
        _currentSection.value = NavSection.ADMIT_CARD
    }

    fun openTestimonialPdf() {
        if (!activePermissions.value.canExportPdf) {
            showMessage("PDF তৈরি করার অনুমতি আপনার নেই")
            return
        }
        _editingStudent.value = null
        _currentSection.value = NavSection.TESTIMONIAL
    }

    fun saveTestimonialStudentProfiles(
        updatedStudents: List<Student>,
        nextSettings: Settings
    ) {
        val school = activeSchool.value ?: return
        val perm = school.permissionsFor(currentUserId)
        if (!perm.canManageStudents) {
            showMessage("শিক্ষার্থীর তথ্য পরিবর্তনের অনুমতি আপনার নেই")
            return
        }
        val prevList = students.value
        val existingById = prevList.associateBy { it.id }
        val safeUpdates = updatedStudents.mapNotNull { candidate ->
            sanitizeStudentForPermissions(candidate, existingById[candidate.id], perm)
        }
        val updateMap = safeUpdates.associateBy { it.id }
        val nextList = prevList.map { st -> updateMap[st.id] ?: st }
        val settingsChanged = nextSettings != school.settings

        viewModelScope.launch {
            if (settingsChanged && school.isAdmin(currentUserId)) {
                cloudRepo.updateSchoolSettings(school, nextSettings).onFailure { e ->
                    showMessage(friendlyFirestoreErrorMessage(e))
                    return@launch
                }
            }
            if (safeUpdates.isNotEmpty()) {
                cloudRepo.syncBatchStudents(school.id, prevList, nextList, deleteRemoved = false)
                    .onSuccess {
                        showMessage("${bn(safeUpdates.size.toString())} জন শিক্ষার্থীর প্রোফাইলে তথ্য সংরক্ষিত হয়েছে")
                    }
                    .onFailure { e ->
                        showMessage(friendlyFirestoreErrorMessage(e))
                    }
            } else if (settingsChanged) {
                showMessage("ফিল্ড সেটিংস সংরক্ষিত হয়েছে")
            }
        }
    }

    fun cacheBlankSheetTemplatesLocally(pages: List<BlankSheetPageSpec>) {
        if (pages.isEmpty()) return
        val distinctMetas = pages
            .map { SheetQrCodec.fromPageConfig(it, it.term) }
            .distinctBy { it.templateId }
        if (distinctMetas.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            sheetTemplateCache.putAll(distinctMetas)
        }
    }

    fun syncBlankSheetTemplates(pages: List<BlankSheetPageSpec>) {
        if (pages.isEmpty()) return
        val distinctMetas = pages
            .map { SheetQrCodec.fromPageConfig(it, it.term) }
            .distinctBy { it.templateId }
        if (distinctMetas.isEmpty()) return
        val schoolId = activeSchool.value?.id?.trim().orEmpty()
        viewModelScope.launch(Dispatchers.IO) {
            sheetTemplateCache.putAll(distinctMetas)
            if (schoolId.isNotEmpty()) {
                distinctMetas.forEach { meta ->
                    val sessionKey = "$schoolId|${meta.templateId}"
                    val alreadySynced = synchronized(syncedTemplateIdsInSession) {
                        syncedTemplateIdsInSession.contains(sessionKey)
                    }
                    if (!alreadySynced) {
                        val res = cloudRepo.saveSheetTemplate(schoolId, meta)
                        if (res.isSuccess) {
                            synchronized(syncedTemplateIdsInSession) {
                                syncedTemplateIdsInSession.add(sessionKey)
                            }
                        }
                    }
                }
            }
        }
    }

    fun selectStudioStudent(studentId: String) {
        _studioStudentId.value = studentId
    }

    fun setIncludeClassHighestInReport(enabled: Boolean) {
        _includeClassHighestInReport.value = enabled
        reportPrefs.edit().putBoolean("include_class_highest", enabled).apply()
    }

    fun setIncludeSummaryTableInReport(enabled: Boolean) {
        _includeSummaryTableInReport.value = enabled
        reportPrefs.edit().putBoolean("include_summary_table", enabled).apply()
    }

    fun setSummaryTableColumns(columns: List<ReportSummaryColumn>) {
        val cleaned = columns.distinct().ifEmpty { ReportSummaryColumn.DEFAULT_SELECTED }
        _summaryTableColumns.value = cleaned
        reportPrefs.edit()
            .putString("summary_table_columns", ReportSummaryColumn.serializeList(cleaned))
            .apply()
    }

    fun setShowCoverAddressInReport(enabled: Boolean) {
        _showCoverAddressInReport.value = enabled
        reportPrefs.edit().putBoolean("show_cover_address", enabled).apply()
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

    fun openBasicStudentImport(initialText: String = "") {
        if (!activePermissions.value.canManageStudents) {
            showMessage("শিক্ষার্থীর বেসিক ডেটা ইমপোর্টের অনুমতি শুধুমাত্র অনুমোদিত শিক্ষক বা অ্যাডমিনের")
            return
        }
        _basicStudentImportState.value = CsvImportModalState(isOpen = true, initialText = initialText)
    }

    fun closeBasicStudentImport() {
        _basicStudentImportState.value = CsvImportModalState(isOpen = false, initialText = "")
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
            val attendancePrefixes = listOf("DAYS_", "PRES_", "ACT_", "DISC_", "COND_", "REM_")
            val filteredX = student.x.filterKeys { k ->
                when {
                    attendancePrefixes.any { k.startsWith(it) } -> perm.canEditAttendance
                    isAbsentXKey(k) -> absentSubjectFromXKey(k)?.let { perm.canEditSubject(it) } == true
                    else -> true
                }
            }
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
        fun isPermittedAbsentKey(k: String): Boolean =
            isAbsentXKey(k) && absentSubjectFromXKey(k)?.let { perm.canEditSubject(it) } == true

        val nextX = existing.x.toMutableMap().apply {
            if (perm.canEditAttendance) {
                keys.toList().forEach { k -> if (isAttendanceKey(k) && !student.x.containsKey(k)) remove(k) }
                student.x.forEach { (k, v) -> if (isAttendanceKey(k)) put(k, v) }
            }
            keys.toList().forEach { k ->
                if (isPermittedAbsentKey(k) && !student.x.containsKey(k)) remove(k)
            }
            student.x.forEach { (k, v) ->
                if (isPermittedAbsentKey(k)) put(k, v)
            }
            if (perm.canManageStudents) {
                keys.toList().forEach { k ->
                    if (!isAttendanceKey(k) && !isAbsentXKey(k) && !student.x.containsKey(k)) remove(k)
                }
                student.x.forEach { (k, v) ->
                    if (!isAttendanceKey(k) && !isAbsentXKey(k)) put(k, v)
                }
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
                when {
                    isAtt -> perm.canEditAttendance
                    isAbsentXKey(xKey) -> absentSubjectFromXKey(xKey)?.let { perm.canEditSubject(it) } == true
                    else -> perm.canManageStudents
                }
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

    fun batchUpdateRemarks(
        term: Int,
        remarksByStudentId: Map<String, String?>
    ) {
        val school = activeSchool.value ?: return
        val perm = school.permissionsFor(currentUserId)
        if (!perm.canEditAttendance) {
            showMessage("মন্তব্য সম্পাদনার অনুমতি আপনার নেই")
            return
        }
        val safeTerm = term.coerceIn(1, 3)
        val allowedIds = students.value
            .filter { it.id in remarksByStudentId.keys && perm.canAccessClass(it.cls) }
            .map { it.id }
            .toSet()
        val filteredUpdates = remarksByStudentId.filterKeys { it in allowedIds }
        if (filteredUpdates.isEmpty()) {
            showMessage("আপডেট করার মতো কোনো শিক্ষার্থী পাওয়া যায়নি")
            return
        }
        viewModelScope.launch {
            cloudRepo.patchBatchPerStudentXField(school.id, "REM_$safeTerm", filteredUpdates)
                .onSuccess {
                    showMessage("${bn(filteredUpdates.size.toString())} জন শিক্ষার্থীর অটো মন্তব্য সংরক্ষিত হয়েছে")
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

    fun selectPdfFont(fontId: String) {
        val valid = if (BENGALI_PDF_FONT_LIST.any { it.id == fontId }) fontId else "noto_serif"
        _localPdfFontOverride.value = valid
        Repo.savePdfFontOverride(appContext, valid)

        val school = activeSchool.value ?: return
        if (!school.isAdmin(currentUserId)) return

        val next = school.settings.copy(pdfFont = valid)
        viewModelScope.launch {
            cloudRepo.updateSchoolSettings(school, next).onFailure { e ->
                val fsEx = generateSequence(e) { it.cause }
                    .filterIsInstance<com.google.firebase.firestore.FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsEx?.code != com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    showMessage(friendlyFirestoreErrorMessage(e))
                }
            }
        }
    }

    fun selectPdfEnglishFont(fontId: String) {
        val valid = if (ENGLISH_PDF_FONT_LIST.any { it.id == fontId }) fontId else "times_new_roman"
        _localPdfEnglishFontOverride.value = valid
        Repo.savePdfEnglishFontOverride(appContext, valid)

        val school = activeSchool.value ?: return
        if (!school.isAdmin(currentUserId)) return

        val next = school.settings.copy(pdfEnglishFont = valid)
        viewModelScope.launch {
            cloudRepo.updateSchoolSettings(school, next).onFailure { e ->
                val fsEx = generateSequence(e) { it.cause }
                    .filterIsInstance<com.google.firebase.firestore.FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsEx?.code != com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    showMessage(friendlyFirestoreErrorMessage(e))
                }
            }
        }
    }

    fun updateSettings(newSettings: Settings) {
        val currentSettings = settings.value
        val bnChanged = newSettings.pdfFont != currentSettings.pdfFont
        val enChanged = newSettings.pdfEnglishFont != currentSettings.pdfEnglishFont
        val onlyFontsChanged = (bnChanged || enChanged) &&
            newSettings == currentSettings.copy(
                pdfFont = newSettings.pdfFont,
                pdfEnglishFont = newSettings.pdfEnglishFont
            )
        if (onlyFontsChanged) {
            if (bnChanged) selectPdfFont(newSettings.pdfFont)
            if (enChanged) selectPdfEnglishFont(newSettings.pdfEnglishFont)
            return
        }

        val school = activeSchool.value
        if (school == null) {
            showMessage("আগে একটি বিদ্যালয় তৈরি বা নির্বাচন করুন")
            return
        }
        if (!school.isAdmin(currentUserId)) {
            showMessage("বিদ্যালয়ের সেটিংস পরিবর্তনের অনুমতি শুধুমাত্র অ্যাডমিনের")
            return
        }
        if (bnChanged) {
            val validBn = if (BENGALI_PDF_FONT_LIST.any { it.id == newSettings.pdfFont }) newSettings.pdfFont else "noto_serif"
            _localPdfFontOverride.value = validBn
            Repo.savePdfFontOverride(appContext, validBn)
        }
        if (enChanged) {
            val validEn = if (ENGLISH_PDF_FONT_LIST.any { it.id == newSettings.pdfEnglishFont }) newSettings.pdfEnglishFont else "times_new_roman"
            _localPdfEnglishFontOverride.value = validEn
            Repo.savePdfEnglishFontOverride(appContext, validEn)
        }
        if (newSettings.schoolAddress != currentSettings.schoolAddress) {
            _localSchoolAddressOverride.value = null
            Repo.saveSchoolAddressOverride(appContext, null)
        }
        viewModelScope.launch {
            cloudRepo.updateSchoolSettings(school, newSettings).onFailure { e ->
                showMessage(friendlyFirestoreErrorMessage(e))
            }
        }
    }

    fun updateResultRules(nextRules: ResultEvaluationRules) {
        val school = activeSchool.value
        if (school == null) {
            showMessage("আগে একটি বিদ্যালয় তৈরি বা নির্বাচন করুন")
            return
        }
        if (!school.isAdmin(currentUserId)) {
            showMessage("শর্তাবলী পরিবর্তনের অনুমতি শুধুমাত্র অ্যাডমিনের")
            return
        }
        _optimisticResultRules.value = nextRules
        viewModelScope.launch {
            cloudRepo.updateSchoolResultRules(school, nextRules)
                .onSuccess {
                    if (_optimisticResultRules.value == nextRules) {
                        _optimisticResultRules.value = null
                    }
                }
                .onFailure { e ->
                    if (_optimisticResultRules.value == nextRules) {
                        _optimisticResultRules.value = null
                    }
                    showMessage(friendlyFirestoreErrorMessage(e))
                }
        }
    }

    fun updateCoverAddress(address: String) {
        val clean = address.trim().take(240)
        val school = activeSchool.value
        if (clean.isNotEmpty() && !_showCoverAddressInReport.value) {
            setShowCoverAddressInReport(true)
        }
        if (school != null && school.isAdmin(currentUserId)) {
            _localSchoolAddressOverride.value = null
            Repo.saveSchoolAddressOverride(appContext, null)
            val base = school.settings
            viewModelScope.launch {
                cloudRepo.updateSchoolSettings(school, base.copy(schoolAddress = clean))
                    .onSuccess {
                        showMessage("প্রচ্ছদের ঠিকানা সংরক্ষিত হয়েছে")
                    }
                    .onFailure { e ->
                        showMessage(friendlyFirestoreErrorMessage(e))
                    }
            }
        } else {
            _localSchoolAddressOverride.value = clean
            Repo.saveSchoolAddressOverride(appContext, clean)
            showMessage("এই ডিভাইসের প্রতিবেদনে ঠিকানা সংরক্ষিত হয়েছে")
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

    fun addCustomClass(
        nameBn: String,
        shortBn: String = "",
        syllabusTier: Int = 3
    ): Boolean {
        val school = activeSchool.value ?: return false
        if (!school.isAdmin(currentUserId)) {
            showMessage("নতুন শ্রেণি যোগ করার অনুমতি শুধুমাত্র অ্যাডমিনের")
            return false
        }
        val trimmed = nameBn.trim()
        if (trimmed.isEmpty()) {
            showMessage("শ্রেণির নাম লিখুন")
            return false
        }
        val current = settings.value
        if (activeSchoolClasses(current).any { it.nameBn.equals(trimmed, ignoreCase = true) }) {
            showMessage("এই নামে একটি শ্রেণি ইতিমধ্যে আছে")
            return false
        }
        val nextId = nextAvailableCustomClassId(current.customClasses)
        if (nextId == null) {
            showMessage("সর্বোচ্চ ১৫টি নতুন শ্রেণি যোগ করা যায়")
            return false
        }
        val cleanShort = shortBn.trim().ifBlank { defaultShortClassLabel(trimmed, nextId) }.take(8)
        val tier = if (syllabusTier <= 2) 1 else 3
        val newDef = CustomClassDef(
            id = nextId,
            nameBn = trimmed,
            shortBn = cleanShort,
            syllabusTier = tier
        )
        val updatedCustom = (current.customClasses + newDef).sortedBy { it.id }
        SchoolClassRegistry.update(updatedCustom)
        val next = current.copy(customClasses = updatedCustom)
        updateSettings(next)
        _selectedClass.value = nextId
        showMessage("\"$trimmed\" শ্রেণি যোগ করা হয়েছে")
        return true
    }

    fun removeCustomClass(classId: Int) {
        val school = activeSchool.value ?: return
        if (!school.isAdmin(currentUserId)) {
            showMessage("শ্রেণি মুছে ফেলার অনুমতি শুধুমাত্র অ্যাডমিনের")
            return
        }
        if (classId in 1..5) {
            showMessage("মূল ১ম–৫ম শ্রেণি মুছে ফেলা যায় না")
            return
        }
        val current = settings.value
        val removed = current.customClasses.find { it.id == classId } ?: return
        val updatedCustom = current.customClasses.filter { it.id != classId }
        SchoolClassRegistry.update(updatedCustom)
        val next = current.copy(
            customClasses = updatedCustom,
            teachers = current.teachers - classId,
            teacherMobiles = current.teacherMobiles - classId
        )
        updateSettings(next)
        if (_selectedClass.value == classId) {
            _selectedClass.value = 1
        }
        showMessage("\"${removed.nameBn}\" শ্রেণি তালিকা থেকে সরানো হয়েছে")
    }

    fun applyCsvImport(summary: Repo.CsvImportSummary) {
        val school = activeSchool.value ?: return
        val perm = school.permissionsFor(currentUserId)
        if (!perm.canManageStudents) {
            showMessage("CSV ইমপোর্টের অনুমতি আপনার নেই")
            closeCsvImport()
            closeBasicStudentImport()
            return
        }
        val isUserAdmin = school.isAdmin(currentUserId)
        val hasSchemaAdditions = summary.createdFields.isNotEmpty() || summary.createdClasses.isNotEmpty()
        if (hasSchemaAdditions && !isUserAdmin) {
            showMessage("নতুন শ্রেণি বা কাস্টম ফিল্ড যোগ করতে বিদ্যালয়ের অ্যাডমিন অনুমতি প্রয়োজন")
            closeCsvImport()
            closeBasicStudentImport()
            return
        }
        val prevList = students.value
        val existingById = prevList.associateBy { it.id }
        val safeStudents = if (isUserAdmin) {
            summary.students
        } else {
            summary.students.mapNotNull { candidate ->
                sanitizeStudentForPermissions(candidate, existingById[candidate.id], perm)
            }
        }
        closeCsvImport()
        closeBasicStudentImport()
        viewModelScope.launch {
            if (hasSchemaAdditions && isUserAdmin) {
                if (summary.createdClasses.isNotEmpty()) {
                    SchoolClassRegistry.update(summary.settings.customClasses)
                }
                val settingsRes = cloudRepo.updateSchoolSettings(school, summary.settings)
                if (settingsRes.isFailure) {
                    showMessage(friendlyFirestoreErrorMessage(settingsRes.exceptionOrNull()!!))
                    return@launch
                }
            }
            cloudRepo.syncBatchStudents(school.id, prevList, safeStudents, deleteRemoved = false)
                .onSuccess {
                    val msg = buildString {
                        append("ইমপোর্ট সম্পন্ন: নতুন ${bn(summary.addedCount.toString())} জন, হালনাগাদ ${bn(summary.updatedCount.toString())} জন")
                        if (summary.createdClasses.isNotEmpty() && isUserAdmin) {
                            append(", নতুন শ্রেণি ${bn(summary.createdClasses.size.toString())}টি")
                        }
                        if (summary.createdFields.isNotEmpty() && isUserAdmin) {
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

    fun buildFullBackupJson(): String = Repo.toJson(
        list = students.value,
        st = settings.value,
        reportPrefs = Repo.loadReportBackupPrefs(appContext),
        sheetTemplates = sheetTemplateCache.getAll()
    )

    fun restoreBackupJson(rawText: String) {
        val school = activeSchool.value ?: return
        if (!school.isAdmin(currentUserId)) {
            showMessage("JSON রিস্টোর শুধুমাত্র বিদ্যালয়ের অ্যাডমিন করতে পারবেন")
            return
        }
        try {
            val bundle = Repo.fromBackupJson(rawText)
            val prevList = students.value
            viewModelScope.launch {
                val settingsRes = cloudRepo.updateSchoolSettings(school, bundle.settings)
                if (settingsRes.isFailure) {
                    showMessage(friendlyFirestoreErrorMessage(settingsRes.exceptionOrNull()!!))
                    return@launch
                }

                // Keep device font & cover address overrides aligned with the restored school backup
                Repo.saveSchoolAddressOverride(appContext, null)
                _localSchoolAddressOverride.value = null
                val decodedFonts = decodePdfFontPair(bundle.settings.pdfFont, bundle.settings.pdfEnglishFont)
                Repo.savePdfFontOverride(appContext, decodedFonts.bengaliFontId)
                _localPdfFontOverride.value = decodedFonts.bengaliFontId
                Repo.savePdfEnglishFontOverride(appContext, decodedFonts.englishFontId)
                _localPdfEnglishFontOverride.value = decodedFonts.englishFontId

                bundle.reportPrefs?.let { rp ->
                    setIncludeClassHighestInReport(rp.includeClassHighest)
                    setIncludeSummaryTableInReport(rp.includeSummaryTable)
                    setSummaryTableColumns(rp.summaryTableColumns)
                    setShowCoverAddressInReport(rp.showCoverAddress)
                }

                cloudRepo.syncBatchStudents(school.id, prevList, bundle.students, deleteRemoved = true)
                    .onSuccess {
                        val restoredTemplates = bundle.sheetTemplates
                        if (restoredTemplates.isNotEmpty()) {
                            sheetTemplateCache.putAll(restoredTemplates)
                            withContext(Dispatchers.IO) {
                                restoredTemplates.forEach { meta ->
                                    cloudRepo.saveSheetTemplate(school.id, meta)
                                }
                            }
                        }
                        val templateNote = if (restoredTemplates.isNotEmpty()) {
                            " ও ${bn(restoredTemplates.size.toString())}টি টেমপ্লেট"
                        } else ""
                        showMessage("ব্যাকআপ রিস্টোর হয়েছে (${bn(bundle.students.size.toString())} জন শিক্ষার্থী$templateNote)")
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

    fun executeDataReset(request: DataResetRequest) {
        val school = activeSchool.value ?: return
        if (!school.isAdmin(currentUserId)) {
            showMessage("ডাটা রিসেটের অনুমতি শুধুমাত্র বিদ্যালয়ের অ্যাডমিনের")
            return
        }
        if (!DataResetEngine.isRequestValid(request)) {
            showMessage("রিসেটের জন্য বৈধ শ্রেণি বা অংশ নির্বাচন করুন")
            return
        }

        when (request.category) {
            DataResetCategory.SAMPLE_DATA -> {
                resetSampleData()
                return
            }
            DataResetCategory.ALL_STUDENTS -> {
                clearAllStudents()
                return
            }
            else -> Unit
        }

        val prevList = students.value
        val customKeys = settings.value.customFields.map { it.key }.toSet()
        val (nextList, affectedCount) = DataResetEngine.applyReset(
            students = prevList,
            request = request,
            customFieldKeys = customKeys
        )

        if (affectedCount == 0 && prevList == nextList) {
            showMessage("নির্বাচিত অংশে রিসেট করার মতো কোনো ডাটা পাওয়া যায়নি")
            return
        }

        if (request.category == DataResetCategory.CLASS_STUDENTS) {
            val currentStudioId = _studioStudentId.value
            if (currentStudioId != null && nextList.none { it.id == currentStudioId }) {
                _studioStudentId.value = null
            }
        }

        val deleteRemoved = request.category == DataResetCategory.CLASS_STUDENTS
        viewModelScope.launch {
            cloudRepo.syncBatchStudents(
                schoolId = school.id,
                previousList = prevList,
                nextList = nextList,
                deleteRemoved = deleteRemoved
            )
                .onSuccess {
                    val label = when (request.category) {
                        DataResetCategory.RESULTS -> "নির্বাচিত শ্রেণি ও বিষয়ের ফলাফল রিসেট হয়েছে"
                        DataResetCategory.ATTENDANCE_TRAITS -> "উপস্থিতি, গুণাবলী ও মন্তব্য রিসেট হয়েছে"
                        DataResetCategory.BASIC_INFO -> "শিক্ষার্থীদের বেসিক তথ্য রিসেট হয়েছে"
                        DataResetCategory.CLASS_STUDENTS -> "নির্বাচিত শ্রেণির শিক্ষার্থী মুছে ফেলা হয়েছে"
                        else -> "ডাটা রিসেট সম্পন্ন হয়েছে"
                    }
                    showMessage("$label (${bn(affectedCount.toString())} জন)")
                }
                .onFailure { e ->
                    showMessage(friendlyFirestoreErrorMessage(e))
                }
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
                force = true,
                enableAutoOnSuccess = true,
                reportPrefs = Repo.loadReportBackupPrefs(appContext),
                sheetTemplates = sheetTemplateCache.getAll()
            )) {
                is DriveAuthOutcome.Authorized -> {
                    autoBackupManager.setDriveAutoBackupEnabled(true)
                    showMessage("Google Drive অটো ব্যাকআপ চালু ও সম্পন্ন হয়েছে")
                }
                is DriveAuthOutcome.NeedsConsent -> {
                    _pendingDriveAction.value = PendingDriveAction.EnableAutoAndUpload
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

    fun setBackupRetentionMode(kind: BackupSystemKind, mode: BackupFileRetentionMode) {
        when (kind) {
            BackupSystemKind.LOCAL -> autoBackupManager.setLocalRetentionMode(mode)
            BackupSystemKind.DRIVE -> autoBackupManager.setDriveRetentionMode(mode)
            BackupSystemKind.CSV -> autoBackupManager.setCsvRetentionMode(mode)
        }
        val kindLabel = when (kind) {
            BackupSystemKind.LOCAL -> "লোকাল JSON"
            BackupSystemKind.DRIVE -> "Google Drive"
            BackupSystemKind.CSV -> if (autoBackupManager.status.value.csvBackupFormat == CsvBackupFormat.EXCEL) "Excel" else "CSV"
        }
        showMessage("$kindLabel ফাইল মোড: ${mode.shortBn}")
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
                force = true,
                enableAutoOnSuccess = false,
                reportPrefs = Repo.loadReportBackupPrefs(appContext),
                sheetTemplates = sheetTemplateCache.getAll()
            )) {
                is DriveAuthOutcome.Authorized -> showMessage("Google Drive ব্যাকআপ সম্পন্ন হয়েছে")
                is DriveAuthOutcome.NeedsConsent -> {
                    _pendingDriveAction.value = PendingDriveAction.UploadOnly
                    _pendingDriveConsent.value = driveRes.intentSender
                }
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
            val reportBackupPrefs = Repo.loadReportBackupPrefs(appContext)
            val cachedTemplates = sheetTemplateCache.getAll()
            val localRes = autoBackupManager.runLocalBackupIfNeeded(
                schoolId = school.id,
                schoolCode = school.joinCode,
                students = students.value,
                settings = settings.value,
                force = true,
                reportPrefs = reportBackupPrefs,
                sheetTemplates = cachedTemplates
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
                force = true,
                enableAutoOnSuccess = false,
                reportPrefs = reportBackupPrefs,
                sheetTemplates = cachedTemplates
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
                    _pendingDriveAction.value = PendingDriveAction.UploadOnly
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
        _pendingDriveAction.value = null
        _driveFilesLoading.value = false
    }

    fun handleDriveConsentIntentResult(resultCode: Int, data: Intent?) {
        _pendingDriveConsent.value = null
        val pendingAction = _pendingDriveAction.value ?: PendingDriveAction.UploadOnly
        _pendingDriveAction.value = null

        if (resultCode != android.app.Activity.RESULT_OK) {
            _driveFilesLoading.value = false
            showMessage("Google Drive অনুমতি বাতিল করা হয়েছে")
            return
        }

        viewModelScope.launch {
            if (pendingAction is PendingDriveAction.ListFiles || pendingAction is PendingDriveAction.RestoreFile) {
                _driveFilesLoading.value = true
            }
            try {
                when (val outcome = GoogleDriveBackupClient.handleConsentResult(appContext, data)) {
                    is DriveAuthOutcome.Authorized -> {
                        when (pendingAction) {
                            is PendingDriveAction.ListFiles -> {
                                GoogleDriveBackupClient.listBackupFiles(outcome.accessToken)
                                    .onSuccess { files -> _driveFiles.value = files }
                                    .onFailure { err -> showMessage(err.localizedMessage ?: "Drive তালিকা লোড হয়নি") }
                            }
                            is PendingDriveAction.RestoreFile -> {
                                GoogleDriveBackupClient.downloadBackupJson(outcome.accessToken, pendingAction.fileId)
                                    .onSuccess { json -> restoreBackupJson(json) }
                                    .onFailure { err -> showMessage(err.localizedMessage ?: "Drive ফাইল ডাউনলোড হয়নি") }
                            }
                            is PendingDriveAction.EnableAutoAndUpload,
                            is PendingDriveAction.UploadOnly -> {
                                val school = activeSchool.value ?: return@launch
                                val enableAuto = pendingAction is PendingDriveAction.EnableAutoAndUpload
                                val json = buildFullBackupJson()
                                autoBackupManager.uploadWithToken(
                                    accessToken = outcome.accessToken,
                                    schoolCode = school.joinCode,
                                    jsonContent = json,
                                    enableAutoOnSuccess = enableAuto
                                )
                                    .onSuccess { uploaded ->
                                        val msg = if (enableAuto) {
                                            "Google Drive অটো ব্যাকআপ চালু ও সম্পন্ন হয়েছে: ${uploaded.name}"
                                        } else {
                                            "Google Drive-এ ব্যাকআপ হয়েছে: ${uploaded.name}"
                                        }
                                        showMessage(msg)
                                    }
                                    .onFailure { err ->
                                        showMessage(err.localizedMessage ?: "Google Drive আপলোড ব্যর্থ")
                                    }
                            }
                        }
                    }
                    is DriveAuthOutcome.Failed -> showMessage(outcome.message)
                    is DriveAuthOutcome.NeedsConsent -> showMessage("Google Drive অনুমতি সম্পন্ন হয়নি")
                }
            } finally {
                _driveFilesLoading.value = false
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
            try {
                when (val auth = GoogleDriveBackupClient.authorizeDrive(appContext)) {
                    is DriveAuthOutcome.Authorized -> {
                        GoogleDriveBackupClient.listBackupFiles(auth.accessToken)
                            .onSuccess { files -> _driveFiles.value = files }
                            .onFailure { err -> showMessage(err.localizedMessage ?: "Drive তালিকা লোড হয়নি") }
                    }
                    is DriveAuthOutcome.NeedsConsent -> {
                        _pendingDriveAction.value = PendingDriveAction.ListFiles
                        _pendingDriveConsent.value = auth.intentSender
                    }
                    is DriveAuthOutcome.Failed -> {
                        showMessage(auth.message)
                    }
                }
            } finally {
                _driveFilesLoading.value = false
            }
        }
    }

    fun restoreFromDriveFile(fileId: String) {
        viewModelScope.launch {
            _driveFilesLoading.value = true
            try {
                when (val auth = GoogleDriveBackupClient.authorizeDrive(appContext)) {
                    is DriveAuthOutcome.Authorized -> {
                        GoogleDriveBackupClient.downloadBackupJson(auth.accessToken, fileId)
                            .onSuccess { json -> restoreBackupJson(json) }
                            .onFailure { err -> showMessage(err.localizedMessage ?: "Drive ফাইল ডাউনলোড হয়নি") }
                    }
                    is DriveAuthOutcome.NeedsConsent -> {
                        _pendingDriveAction.value = PendingDriveAction.RestoreFile(fileId)
                        _pendingDriveConsent.value = auth.intentSender
                    }
                    is DriveAuthOutcome.Failed -> showMessage(auth.message)
                }
            } finally {
                _driveFilesLoading.value = false
            }
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
        val rules = settings.value.resultRules
        val peers = students.value.filter { it.cls == s.cls }
        val activeTerms = ResultRulesEngine.activeTermsForClass(peers, rules.dynamicTermAveraging)
        val eval = ResultRulesEngine.evaluateStudent(s, rules, activeTerms)
        return buildString {
            append("নাম: ${s.name}, শ্রেণি: ${classNameBn(s.cls, settings.value)}, রোল: ${s.roll}, ধর্ম: ${s.religion}")
            if (s.isSpecialNeeds) append(", বিশেষ চাহিদাসম্পন্ন (৩৩%)")
            if (s.guardian.isNotBlank()) append(", অভিভাবক: ${s.guardian}")
            if (eval.hasAnyData) {
                append(", গড়: ${fmtEn(eval.pct)}% (${eval.grade} - ${gradeLabelBn(eval.grade)})")
                append(", অবস্থা: ${if (eval.isPromoted) "উত্তীর্ণ" else "অনুত্তীর্ণ"}")
            }
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
        loadImageJob?.cancel()
        _aiState.update {
            it.copy(
                activeTab = 1,
                loadingImage = true,
                analysisError = null
            )
        }
        loadImageJob = viewModelScope.launch {
            try {
                val bmp = withContext(Dispatchers.IO) {
                    VisionImageOptimizer.decodeUriBitmap(appContext, uri, 2800)
                }
                if (bmp == null) {
                    _aiState.update {
                        it.copy(
                            loadingImage = false,
                            analysisError = "গ্যালারি থেকে ছবিটি পড়া যায়নি — অনুগ্রহ করে অন্য ছবি দিন অথবা স্ক্রিনশট/ক্রপ করে চেষ্টা করুন।"
                        )
                    }
                    showMessage("ছবি পড়া যায়নি")
                    return@launch
                }

                // ছবি ডিকোড হওয়ার সাথে সাথেই প্রিভিউ দেখান, যেন QR স্ক্যানের অপেক্ষায় ছবি খালি না থাকে
                originalMarksheetBitmap = bmp
                _aiState.update {
                    it.copy(
                        activeTab = 1,
                        loadingImage = false,
                        selectedImageBitmap = bmp,
                        preparedImageStats = null,
                        hasOriginalImageBackup = false,
                        detectedSheetQr = null,
                        sheetDetectionSource = null,
                        sheetDetection = null,
                        resolvedTemplateMeta = null,
                        resolvedTemplateSource = null,
                        resolvingSheetTemplate = false,
                        analysisResult = null,
                        classSheetResult = null,
                        analysisError = null
                    )
                }

                val enhance = _aiState.value.documentEnhance
                val (stats, detection) = withContext(Dispatchers.Default) {
                    val computedStats = runCatching {
                        VisionImageOptimizer.computeStatsOnly(bmp, enhance)
                    }.getOrElse {
                        PreparedImageStats(
                            width = bmp.width,
                            height = bmp.height,
                            byteSize = 0,
                            qualityUsed = 86,
                            documentEnhanced = enhance
                        )
                    }
                    val detected = runCatching {
                        SheetQrScanner.decodeAllFromBitmap(bmp)
                    }.getOrElse {
                        SheetScanDetection()
                    }
                    computedStats to detected
                }

                if (_aiState.value.selectedImageBitmap === bmp) {
                    applyDetectedSheetState(
                        targetBmp = bmp,
                        stats = stats,
                        detection = detection,
                        hasOriginalBackup = false,
                        switchToScannerTab = true,
                        keepExistingOnMiss = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                _aiState.update {
                    it.copy(
                        loadingImage = false,
                        analysisError = "গ্যালারি থেকে ছবি লোড করতে সমস্যা হয়েছে — আবার চেষ্টা করুন।"
                    )
                }
                showMessage("ছবি লোড করা যায়নি")
            }
        }
    }

    fun setDocumentEnhanceMode(enabled: Boolean) {
        _aiState.update { it.copy(documentEnhance = enabled) }
        val bmp = _aiState.value.selectedImageBitmap ?: return
        viewModelScope.launch {
            val stats = withContext(Dispatchers.Default) {
                VisionImageOptimizer.computeStatsOnly(bmp, enabled)
            }
            _aiState.update {
                if (it.selectedImageBitmap === bmp && it.documentEnhance == enabled) {
                    it.copy(preparedImageStats = stats)
                } else it
            }
        }
    }

    fun rotateMarksheetImage90() {
        val current = _aiState.value.selectedImageBitmap ?: return
        val enhance = _aiState.value.documentEnhance
        viewModelScope.launch {
            val (rotated, stats, detection) = withContext(Dispatchers.Default) {
                val r = VisionImageOptimizer.rotate90Clockwise(current)
                Triple(
                    r,
                    VisionImageOptimizer.computeStatsOnly(r, enhance),
                    SheetQrScanner.decodeAllFromBitmap(r)
                )
            }
            applyDetectedSheetState(
                targetBmp = rotated,
                stats = stats,
                detection = detection,
                hasOriginalBackup = true,
                switchToScannerTab = false,
                keepExistingOnMiss = true
            )
        }
    }

    fun cropMarksheetImage(leftFrac: Float, topFrac: Float, rightFrac: Float, bottomFrac: Float) {
        val current = _aiState.value.selectedImageBitmap ?: return
        val enhance = _aiState.value.documentEnhance
        viewModelScope.launch {
            val (cropped, stats, detection) = withContext(Dispatchers.Default) {
                val c = VisionImageOptimizer.cropNormalized(current, leftFrac, topFrac, rightFrac, bottomFrac)
                Triple(
                    c,
                    VisionImageOptimizer.computeStatsOnly(c, enhance),
                    SheetQrScanner.decodeAllFromBitmap(c)
                )
            }
            applyDetectedSheetState(
                targetBmp = cropped,
                stats = stats,
                detection = detection,
                hasOriginalBackup = true,
                switchToScannerTab = false,
                keepExistingOnMiss = true
            )
        }
    }

    fun restoreOriginalMarksheetImage() {
        val orig = originalMarksheetBitmap ?: return
        val enhance = _aiState.value.documentEnhance
        viewModelScope.launch {
            val (stats, detection) = withContext(Dispatchers.Default) {
                VisionImageOptimizer.computeStatsOnly(orig, enhance) to SheetQrScanner.decodeAllFromBitmap(orig)
            }
            applyDetectedSheetState(
                targetBmp = orig,
                stats = stats,
                detection = detection,
                hasOriginalBackup = false,
                switchToScannerTab = false,
                keepExistingOnMiss = false
            )
        }
    }

    fun clearMarksheetImage() {
        loadImageJob?.cancel()
        originalMarksheetBitmap = null
        _aiState.update {
            it.copy(
                loadingImage = false,
                selectedImageBitmap = null,
                preparedImageStats = null,
                hasOriginalImageBackup = false,
                detectedSheetQr = null,
                sheetDetectionSource = null,
                sheetDetection = null,
                resolvedTemplateMeta = null,
                resolvedTemplateSource = null,
                resolvingSheetTemplate = false,
                analysisResult = null,
                classSheetResult = null,
                analysisError = null
            )
        }
    }

    private suspend fun applyDetectedSheetState(
        targetBmp: Bitmap,
        stats: PreparedImageStats,
        detection: SheetScanDetection,
        hasOriginalBackup: Boolean,
        switchToScannerTab: Boolean,
        keepExistingOnMiss: Boolean
    ) {
        val offlineMeta = detection.offlineQrMeta
        val tid = detection.templateId?.trim()?.uppercase()?.takeIf { it.length >= 6 }
        val cachedMeta = if (offlineMeta == null && tid != null) {
            withContext(Dispatchers.IO) { sheetTemplateCache.get(tid) }
        } else null

        val immediateMeta = when {
            offlineMeta != null -> offlineMeta
            cachedMeta != null -> cachedMeta
            detection.bestLocalMeta != null -> detection.bestLocalMeta
            keepExistingOnMiss -> _aiState.value.detectedSheetQr
            else -> null
        }

        val immediateSource = when {
            offlineMeta != null -> detection.sourceLabelBn.ifBlank { "ডান পাশের অফলাইন QR" }
            cachedMeta != null -> "${detection.sourceLabelBn.ifBlank { "বাম/নিচের QR" }} · অফলাইন ক্যাশ"
            detection.bestLocalMeta != null -> detection.sourceLabelBn
            keepExistingOnMiss && immediateMeta != null -> _aiState.value.sheetDetectionSource
            else -> null
        }

        val preservePreviousDetection = keepExistingOnMiss && detection.detectedCount == 0
        val nextDetection = if (preservePreviousDetection) _aiState.value.sheetDetection else detection
        val nextResolvedMeta = when {
            offlineMeta != null -> offlineMeta
            cachedMeta != null -> cachedMeta
            preservePreviousDetection -> _aiState.value.resolvedTemplateMeta
            else -> null
        }
        val nextResolvedSource = when {
            offlineMeta != null -> "ডান পাশের অফলাইন QR"
            cachedMeta != null -> "অফলাইন ক্যাশ"
            preservePreviousDetection -> _aiState.value.resolvedTemplateSource
            else -> null
        }

        val schoolId = activeSchool.value?.id?.trim().orEmpty()
        val needsOnlineFetch = offlineMeta == null && tid != null && cachedMeta == null && schoolId.isNotEmpty()

        _aiState.update {
            it.copy(
                activeTab = if (switchToScannerTab) 1 else it.activeTab,
                selectedImageBitmap = targetBmp,
                preparedImageStats = stats,
                hasOriginalImageBackup = hasOriginalBackup,
                detectedSheetQr = immediateMeta,
                sheetDetectionSource = immediateSource,
                sheetDetection = nextDetection,
                resolvedTemplateMeta = nextResolvedMeta,
                resolvedTemplateSource = nextResolvedSource,
                resolvingSheetTemplate = needsOnlineFetch,
                analysisResult = null,
                classSheetResult = null,
                analysisError = null
            )
        }

        if (needsOnlineFetch && tid != null) {
            viewModelScope.launch {
                val cloudMeta = withContext(Dispatchers.IO) {
                    cloudRepo.fetchSheetTemplate(schoolId, tid).getOrNull()?.also { fetched ->
                        sheetTemplateCache.put(fetched)
                    }
                }
                _aiState.update { cur ->
                    if (cur.selectedImageBitmap !== targetBmp) {
                        cur
                    } else if (cloudMeta != null) {
                        val pIdx = detection.offlineQrMeta?.pageIndex ?: cloudMeta.pageIndex
                        val pTot = detection.offlineQrMeta?.totalPages ?: cloudMeta.totalPages
                        val resolvedPage = cloudMeta.copy(pageIndex = pIdx, totalPages = pTot)
                        cur.copy(
                            detectedSheetQr = resolvedPage,
                            sheetDetectionSource = "Firestore ক্লাউড টেমপ্লেট",
                            resolvedTemplateMeta = resolvedPage,
                            resolvedTemplateSource = "Firestore ক্লাউড",
                            resolvingSheetTemplate = false
                        )
                    } else {
                        cur.copy(resolvingSheetTemplate = false)
                    }
                }
            }
        }
    }

    fun analyzeSelectedMarksheet(teacherNote: String) {
        val bmp = _aiState.value.selectedImageBitmap ?: return
        if (_aiState.value.analyzingImage) return
        val enhance = _aiState.value.documentEnhance
        _aiState.update { it.copy(analyzingImage = true, analysisError = null, classSheetResult = null) }
        viewModelScope.launch {
            try {
                val result = aiRepository.analyzeMarksheetImage(bmp, teacherNote, enhance)
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

    private suspend fun resolveEffectiveSheetQrMeta(targetBmp: Bitmap): BlankSheetQrMeta? {
        val st = _aiState.value
        st.sheetDetection?.offlineQrMeta?.let { return it }

        val existingResolved = st.resolvedTemplateMeta
        if (existingResolved != null && existingResolved.columnIds.isNotEmpty()) {
            return existingResolved
        }

        val tid = st.sheetDetection?.templateId?.trim()?.uppercase()?.takeIf { it.length >= 6 }
        if (tid != null) {
            val resolved = kotlinx.coroutines.withTimeoutOrNull(3_500L) {
                val cached = withContext(Dispatchers.IO) { sheetTemplateCache.get(tid) }
                if (cached != null) {
                    cached to "অফলাইন ক্যাশ"
                } else if (st.resolvingSheetTemplate || activeSchool.value != null) {
                    val fetched = resolveSheetTemplateRemote(tid)
                    if (fetched != null) fetched to "Firestore ক্লাউড" else null
                } else {
                    null
                }
            }
            if (resolved != null) {
                val (meta, source) = resolved
                val offline = st.sheetDetection?.offlineQrMeta
                val merged = meta.copy(
                    pageIndex = offline?.pageIndex ?: meta.pageIndex,
                    totalPages = offline?.totalPages ?: meta.totalPages
                )
                _aiState.update { cur ->
                    if (cur.selectedImageBitmap !== targetBmp) cur
                    else cur.copy(
                        detectedSheetQr = merged,
                        sheetDetectionSource = "$source টেমপ্লেট",
                        resolvedTemplateMeta = merged,
                        resolvedTemplateSource = source,
                        resolvingSheetTemplate = false
                    )
                }
                return merged
            }
        }
        return _aiState.value.detectedSheetQr
    }

    fun analyzeClassSheet(
        clsHint: Int?,
        termHint: Int?,
        subKeyHint: String?,
        teacherNote: String,
        importScope: ClassSheetImportScope = ClassSheetImportScope.AUTO,
        sheetKindHint: ClassSheetKind? = null
    ) {
        val bmp = _aiState.value.selectedImageBitmap ?: return
        if (_aiState.value.analyzingImage) return
        val enhance = _aiState.value.documentEnhance

        _aiState.update {
            it.copy(
                analyzingImage = true,
                analysisError = null,
                analysisResult = null
            )
        }
        viewModelScope.launch {
            try {
                val qrMeta = resolveEffectiveSheetQrMeta(bmp)
                val result = aiRepository.analyzeClassSheetImage(
                    bmp, clsHint, termHint, subKeyHint, teacherNote, importScope, enhance, sheetKindHint, qrMeta
                )
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

    fun analyzeAutoSheet(
        clsHint: Int?,
        termHint: Int?,
        subKeyHint: String?,
        teacherNote: String,
        importScope: ClassSheetImportScope = ClassSheetImportScope.AUTO,
        sheetKindHint: ClassSheetKind? = null
    ) {
        val bmp = _aiState.value.selectedImageBitmap ?: return
        if (_aiState.value.analyzingImage) return
        val enhance = _aiState.value.documentEnhance

        _aiState.update {
            it.copy(
                analyzingImage = true,
                analysisError = null,
                analysisResult = null,
                classSheetResult = null
            )
        }
        viewModelScope.launch {
            try {
                val qrMeta = resolveEffectiveSheetQrMeta(bmp)
                when (
                    val res = aiRepository.analyzeAutoSheetImage(
                        bitmap = bmp,
                        clsHint = clsHint,
                        termHint = termHint,
                        subKeyHint = subKeyHint,
                        teacherNote = teacherNote,
                        importScope = importScope,
                        documentEnhance = enhance,
                        sheetKindHint = sheetKindHint,
                        qrMeta = qrMeta
                    )
                ) {
                    is AutoScanResult.ClassSheet -> {
                        _aiState.update { it.copy(analyzingImage = false, classSheetResult = res.sheet, analysisResult = null) }
                    }
                    is AutoScanResult.SingleStudent -> {
                        _aiState.update { it.copy(analyzingImage = false, analysisResult = res.marksheet, classSheetResult = null) }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiState.update {
                    it.copy(
                        analyzingImage = false,
                        analysisError = e.message ?: "ছবি অটো-স্ক্যান করা যায়নি"
                    )
                }
            }
        }
    }

    fun loadSampleClassSheetPreview(
        clsHint: Int?,
        termHint: Int?,
        subKeyHint: String?,
        importScope: ClassSheetImportScope = ClassSheetImportScope.AUTO,
        sheetKindHint: ClassSheetKind? = null
    ) {
        if (_aiState.value.analyzingImage) return
        _aiState.update { it.copy(analyzingImage = true, analysisError = null, analysisResult = null) }
        viewModelScope.launch {
            try {
                val bmp = _aiState.value.selectedImageBitmap ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                val result = sampleAiRepository.analyzeClassSheetImage(
                    bmp, clsHint, termHint, subKeyHint, "", importScope, true, sheetKindHint
                )
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
            val safeCls = cls.coerceIn(1, 20)
            val safeTerm = term.coerceIn(1, 3)
            val hasWritten = (subjectsFor(safeCls, RELIGIONS.first()).find { it.key == subKey }?.written ?: 0) > 0
            val nextScope = if (!hasWritten && current.scope == ClassSheetImportScope.WR_ONLY) {
                ClassSheetImportScope.BOTH
            } else {
                current.scope
            }
            val nextRows = if (safeCls != current.cls || subKey != current.subKey || nextScope != current.scope) {
                current.rows.map { r ->
                    ScanConstraintSolver.reconcileInterdependencies(r, safeCls, subKey, nextScope, current.sheetKind)
                }
            } else {
                current.rows
            }
            st.copy(
                classSheetResult = current.copy(
                    cls = safeCls,
                    term = safeTerm,
                    subKey = subKey,
                    scope = nextScope,
                    rows = nextRows
                )
            )
        }
    }

    fun updateClassSheetScope(newScope: ClassSheetImportScope) {
        _aiState.update { st ->
            val current = st.classSheetResult ?: return@update st
            val hasWritten = (subjectsFor(current.cls.coerceIn(1, 20), RELIGIONS.first())
                .find { it.key == current.subKey }?.written ?: 0) > 0
            val safeScope = when (newScope) {
                ClassSheetImportScope.WR_ONLY -> if (hasWritten) ClassSheetImportScope.WR_ONLY else ClassSheetImportScope.BOTH
                ClassSheetImportScope.AUTO -> ClassSheetImportScope.BOTH
                else -> newScope
            }
            val nextRows = current.rows.map { r ->
                ScanConstraintSolver.reconcileInterdependencies(r, current.cls, current.subKey, safeScope, current.sheetKind)
            }
            st.copy(classSheetResult = current.copy(scope = safeScope, rows = nextRows))
        }
    }

    fun updateClassSheetKind(newKind: ClassSheetKind) {
        _aiState.update { st ->
            val current = st.classSheetResult ?: return@update st
            if (current.sheetKind == newKind) return@update st
            val safeScope = if (newKind != ClassSheetKind.SINGLE_TERM_MARKS) ClassSheetImportScope.BOTH else current.scope
            val nextRows = current.rows.map { r ->
                ScanConstraintSolver.reconcileInterdependencies(r, current.cls, current.subKey, safeScope, newKind)
            }
            st.copy(
                classSheetResult = current.copy(
                    sheetKind = newKind,
                    scope = safeScope,
                    rows = nextRows
                )
            )
        }
    }

    fun updateClassSheetSharedWorkingDays(term: Int, rawDays: String) {
        _aiState.update { st ->
            val current = st.classSheetResult ?: return@update st
            val safeTerm = term.coerceIn(1, 3)
            val cleaned = fromBn(rawDays).filter { it.isDigit() }
            val nextMap = current.sharedWorkingDaysByTerm.toMutableMap().apply {
                if (cleaned.isEmpty()) remove(safeTerm) else put(safeTerm, cleaned)
            }
            st.copy(classSheetResult = current.copy(sharedWorkingDaysByTerm = nextMap))
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
                    var nextRemark = r.remark
                    var nextTraits = r.traitValues
                    val nextDeduced = r.deducedFields.toMutableMap()
                    val nextResolved = r.resolvedFields.toMutableSet()

                    if (rollText != null) nextResolved.add("roll")
                    if (name != null) nextResolved.add("name")

                    if (editedKey != null && editedVal != null) {
                        when {
                            editedKey == "REMARK" -> {
                                nextRemark = editedVal
                                nextResolved.add("remark")
                            }
                            editedKey.startsWith("TRAIT:") -> {
                                val tKey = editedKey.removePrefix("TRAIT:")
                                nextTraits = r.traitValues.toMutableMap().apply {
                                    if (editedVal.isEmpty()) remove(tKey) else put(tKey, editedVal)
                                }
                                nextResolved.add(tKey)
                            }
                            editedKey == "CONT" -> nextCont = editedVal.ifBlank { null }
                            editedKey == "GRAND" -> nextGrand = editedVal.ifBlank { null }
                            else -> {
                                nextMarks = r.marksText.toMutableMap().apply {
                                    if (editedVal.isEmpty()) remove(editedKey) else put(editedKey, editedVal)
                                }
                            }
                        }
                        if (editedKey != "REMARK" && !editedKey.startsWith("TRAIT:")) {
                            // ব্যবহারকারী নিজে এডিট করলে আগের অনুমান মুছে নতুন হিসাব চালু হবে
                            nextDeduced.remove(editedKey)
                            nextResolved.add(editedKey)
                            nextResolved.remove("arithmetic")

                            if (current.sheetKind == ClassSheetKind.THREE_TERM_MARKS &&
                                (editedKey.startsWith("T1_") || editedKey.startsWith("T2_") || editedKey.startsWith("T3_"))
                            ) {
                                val termPrefix = editedKey.substringBefore('_')
                                nextResolved.remove("arithmetic_${termPrefix.lowercase()}")
                                listOf("${termPrefix}_CONT", "${termPrefix}_WR", "${termPrefix}_TOTAL").forEach { sib ->
                                    if (sib != editedKey && sib in nextDeduced) {
                                        nextMarks = nextMarks.toMutableMap().apply { remove(sib) }
                                        nextDeduced.remove(sib)
                                        nextResolved.remove(sib)
                                    }
                                }
                            } else {
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
                        remark = nextRemark,
                        traitValues = nextTraits,
                        resolvedFields = nextResolved,
                        deducedFields = nextDeduced,
                        useScannedNameForExisting = nextUseScannedName
                    )
                    if (editedKey != null) {
                        ScanConstraintSolver.reconcileInterdependencies(
                            updatedRow,
                            current.cls,
                            current.subKey,
                            current.scope,
                            current.sheetKind
                        )
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
            val extraThreeTerm = (1..3).flatMap { t ->
                listOf("T${t}_CONT", "T${t}_WR", "T${t}_TOTAL", "arithmetic_t$t")
            }
            val extraTraits = (1..3).flatMap { t ->
                listOf("DAYS_$t", "PRES_$t", "ACT_$t", "DISC_$t", "COND_$t", "REM_$t")
            }
            val baseVerifyKeys = setOf(
                "roll", "name", "remark", "TB", "ACT", "LANG", "CT", "WR", "TOTAL", "CONT", "GRAND", "arithmetic"
            ) + extraThreeTerm + extraTraits
            val nextRows = current.rows.map { r ->
                val allKeys = r.aiIssues.keys + baseVerifyKeys
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
        val cls = sheet.cls.coerceIn(1, 20)
        val term = sheet.term.coerceIn(1, 3)
        val subKey = sheet.subKey

        if (!perm.canAccessClass(cls)) {
            showMessage("এই শ্রেণিতে (${classNameBn(cls, settings.value)}) ডেটা সংরক্ষণের অনুমতি আপনার নেই")
            return
        }
        if (sheet.sheetKind == ClassSheetKind.ATTENDANCE_TRAITS) {
            if (!perm.canEditAttendance) {
                showMessage("উপস্থিতি ও গুণাবলি পরিবর্তনের অনুমতি আপনার নেই")
                return
            }
        } else if (!perm.canEditSubject(subKey)) {
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
            validateScannedStudentRow(
                row = row,
                cls = cls,
                subKey = subKey,
                duplicateRolls = duplicates,
                hasExistingDbName = hasDbName,
                scope = sheet.scope,
                sheetKind = sheet.sheetKind,
                sharedWorkingDaysByTerm = sheet.sharedWorkingDaysByTerm
            ).hasHardError
        }
        if (hasHardError) {
            showMessage("লাল চিহ্নিত ত্রুটিপূর্ণ ঘরগুলো ঠিক করে আবার সংরক্ষণ করুন")
            return
        }

        val out = prevList.toMutableList()
        var addedCount = 0
        var updatedCount = 0
        var skippedNoPerm = 0

        val saveContinuous = sheet.scope != ClassSheetImportScope.WR_ONLY
        val saveWritten = sheet.scope != ClassSheetImportScope.CONT_ONLY

        selectedRows.forEach { row ->
            val roll = fromBn(row.rollText.trim()).toIntOrNull() ?: return@forEach
            val pos = out.indexOfFirst { it.cls == cls && it.roll == roll }
            if (pos < 0 && !perm.canManageStudents) {
                skippedNoPerm++
                return@forEach
            }
            val base = if (pos >= 0) out[pos] else createEmptyStudent(cls, roll)
            val nextM = base.m.toMutableMap()
            val nextX = base.x.toMutableMap()
            val subDef = subjectsFor(cls, RELIGIONS.first()).find { it.key == subKey }
            val compKeys = subDef?.comps?.map { it.key } ?: emptyList()
            val hasWritten = (subDef?.written ?: 0) > 0
            val isArt = compKeys == listOf("TOTAL") && !hasWritten

            when (sheet.sheetKind) {
                ClassSheetKind.ATTENDANCE_TRAITS -> {
                    (1..3).forEach { t ->
                        val rowDays = row.traitValues["DAYS_$t"]?.trim().orEmpty().let { fromBn(it).filter { c -> c.isDigit() } }
                        val rowPres = row.traitValues["PRES_$t"]?.trim().orEmpty().let { fromBn(it).filter { c -> c.isDigit() } }
                        val rowAct = row.traitValues["ACT_$t"]?.trim().orEmpty().let { ScanConstraintSolver.normalizeTraitCode(it) }
                        val rowDisc = row.traitValues["DISC_$t"]?.trim().orEmpty().let { ScanConstraintSolver.normalizeTraitCode(it) }
                        val rowCond = row.traitValues["COND_$t"]?.trim().orEmpty().let { ScanConstraintSolver.normalizeTraitCode(it) }
                        val rowRem = row.traitValues["REM_$t"]?.trim().orEmpty()
                        val sharedDays = sheet.sharedWorkingDaysByTerm[t]?.trim().orEmpty().let { fromBn(it).filter { c -> c.isDigit() } }

                        val hasTermActivity = rowDays.isNotEmpty() || rowPres.isNotEmpty() ||
                            rowAct.isNotEmpty() || rowDisc.isNotEmpty() || rowCond.isNotEmpty() || rowRem.isNotEmpty()

                        if (rowDays.isNotEmpty()) {
                            nextX["DAYS_$t"] = rowDays
                        } else if (hasTermActivity && sharedDays.isNotEmpty()) {
                            nextX["DAYS_$t"] = sharedDays
                        }
                        if (rowPres.isNotEmpty()) nextX["PRES_$t"] = rowPres
                        if (rowAct.isNotEmpty()) nextX["ACT_$t"] = rowAct
                        if (rowDisc.isNotEmpty()) nextX["DISC_$t"] = rowDisc
                        if (rowCond.isNotEmpty()) nextX["COND_$t"] = rowCond
                        if (rowRem.isNotEmpty()) nextX["REM_$t"] = rowRem
                    }
                    if (row.remark.isNotBlank() && row.traitValues["REM_$term"].isNullOrBlank()) {
                        nextX["REM_$term"] = row.remark.trim()
                    }
                }

                ClassSheetKind.THREE_TERM_MARKS -> {
                    (1..3).forEach { t ->
                        val rawCont = row.marksText["T${t}_CONT"]?.trim().orEmpty()
                        val rawWr = row.marksText["T${t}_WR"]?.trim().orEmpty()
                        val rawTot = row.marksText["T${t}_TOTAL"]?.trim().orEmpty()
                        if (rawCont.isEmpty() && rawWr.isEmpty() && rawTot.isEmpty()) return@forEach

                        if (isArt) {
                            fromBn(rawTot).toDoubleOrNull()?.let { d ->
                                nextM[mk(subKey, t, "TOTAL")] = d
                            }
                        } else if (!hasWritten) {
                            compKeys.forEach { nextM.remove(mk(subKey, t, it)) }
                            val chosen = rawCont.ifEmpty { rawTot }
                            fromBn(chosen).toDoubleOrNull()?.let { d ->
                                nextM[mk(subKey, t, "CONT")] = d
                                nextM.remove(mk(subKey, t, "TOTAL"))
                            }
                        } else if (rawCont.isNotEmpty() || rawWr.isNotEmpty()) {
                            if (rawCont.isNotEmpty()) {
                                compKeys.forEach { nextM.remove(mk(subKey, t, it)) }
                                fromBn(rawCont).toDoubleOrNull()?.let { d -> nextM[mk(subKey, t, "CONT")] = d }
                            }
                            if (rawWr.isNotEmpty()) {
                                fromBn(rawWr).toDoubleOrNull()?.let { d -> nextM[mk(subKey, t, "WR")] = d }
                            }
                            nextM.remove(mk(subKey, t, "TOTAL"))
                        } else if (rawTot.isNotEmpty()) {
                            compKeys.forEach { nextM.remove(mk(subKey, t, it)) }
                            nextM.remove(mk(subKey, t, "CONT"))
                            nextM.remove(mk(subKey, t, "WR"))
                            fromBn(rawTot).toDoubleOrNull()?.let { d -> nextM[mk(subKey, t, "TOTAL")] = d }
                        }
                    }
                    if (row.remark.isNotBlank() && perm.canEditAttendance) {
                        nextX["REM_$term"] = row.remark.trim()
                    }
                }

                ClassSheetKind.SINGLE_TERM_MARKS -> {
                    val hasBreakdown = compKeys.any { !row.marksText[it]?.trim().isNullOrEmpty() }
                    val isGrandTotalOnly = !isArt &&
                        !hasBreakdown &&
                        row.sheetContinuousTotal?.trim().isNullOrEmpty() &&
                        row.marksText["WR"]?.trim().isNullOrEmpty() &&
                        !row.sheetGrandTotal?.trim().isNullOrEmpty() &&
                        sheet.scope == ClassSheetImportScope.BOTH

                    if (isGrandTotalOnly) {
                        compKeys.forEach { nextM.remove(mk(subKey, term, it)) }
                        nextM.remove(mk(subKey, term, "CONT"))
                        nextM.remove(mk(subKey, term, "WR"))
                        fromBn(row.sheetGrandTotal!!.trim()).toDoubleOrNull()?.let { d ->
                            nextM[mk(subKey, term, "TOTAL")] = d
                        }
                    } else {
                        if (saveContinuous) {
                            if (isArt) {
                                val totalRaw = row.marksText["TOTAL"]?.trim().takeIf { !it.isNullOrEmpty() }
                                    ?: row.sheetGrandTotal?.trim().orEmpty()
                                val fullKey = mk(subKey, term, "TOTAL")
                                if (totalRaw.isEmpty()) nextM.remove(fullKey)
                                else fromBn(totalRaw).toDoubleOrNull()?.let { nextM[fullKey] = it }
                            } else if (hasBreakdown) {
                                nextM.remove(mk(subKey, term, "CONT"))
                                nextM.remove(mk(subKey, term, "TOTAL"))
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
                                // রেজাল্ট কার্ড / শুধু ধারাবাহিক মোট মোড
                                compKeys.forEach { nextM.remove(mk(subKey, term, it)) }
                                nextM.remove(mk(subKey, term, "TOTAL"))
                                val rawCont = row.sheetContinuousTotal?.trim().takeIf { !it.isNullOrEmpty() }
                                    ?: if (!hasWritten) row.sheetGrandTotal?.trim().orEmpty() else ""
                                val contKey = mk(subKey, term, "CONT")
                                if (rawCont.isEmpty()) {
                                    nextM.remove(contKey)
                                } else {
                                    fromBn(rawCont).toDoubleOrNull()?.let { d -> nextM[contKey] = d }
                                }
                            }
                        }

                        if (hasWritten && saveWritten) {
                            nextM.remove(mk(subKey, term, "TOTAL"))
                            val rawWr = row.marksText["WR"]?.trim().orEmpty()
                            val wrKey = mk(subKey, term, "WR")
                            if (rawWr.isEmpty()) {
                                nextM.remove(wrKey)
                            } else {
                                fromBn(rawWr).toDoubleOrNull()?.let { d -> nextM[wrKey] = d }
                            }
                        }
                    }

                    if (row.remark.isNotBlank() && perm.canEditAttendance) {
                        nextX["REM_$term"] = row.remark.trim()
                    }
                }
            }

            val cleanScannedName = row.name.trim()
            val shouldKeepDbName = pos >= 0 && base.name.isNotBlank() && !row.useScannedNameForExisting
            val candidate = if (!shouldKeepDbName && perm.canManageStudents && cleanScannedName.isNotBlank()) {
                base.copy(name = cleanScannedName, m = nextM, x = nextX)
            } else {
                base.copy(m = nextM, x = nextX)
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
                if (skippedNoPerm > 0) "নতুন শিক্ষার্থী যোগ করার অনুমতি নেই (শুধু তালিকার শিক্ষার্থীদের তথ্য আপডেট হবে)"
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
        val cls = (ext.student.cls ?: _selectedClass.value).coerceIn(1, 20)
        val roll = ext.student.roll?.takeIf { it > 0 }
            ?: ((students.value.filter { it.cls == cls }.maxOfOrNull { it.roll } ?: 0) + 1)

        val existing = students.value.find { it.cls == cls && it.roll == roll }
        if (!perm.canAccessClass(cls)) {
            showMessage("এই শ্রেণিতে (${classNameBn(cls, settings.value)}) নম্বর বা তথ্য সংরক্ষণের অনুমতি আপনার নেই")
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
            fetched.map { it.id }.filter { it !in prevIds }.toSet()
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
                _geminiModelsError.value = "লাইভ মডেল লোড করতে আগে Gemini API Key লিখুন।"
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
                    if (!silent) {
                        showMessage("${bn(fetched.size.toString())}টি লাইভ মডেল লোড হয়েছে — তালিকা থেকে পছন্দের ৩টি মডেল বাছুন")
                    }
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
        val clean = modelId.trim().removePrefix("models/").trim()
        _geminiCustomModels.value = GeminiApiConfig.removeCustomModel(appContext, clean)
        val remaining = _geminiOrderedModels.value.filter { it != clean }
        if (remaining != _geminiOrderedModels.value) {
            _geminiOrderedModels.value = GeminiApiConfig.saveOrderedModels(appContext, remaining)
            directAiRepository.clearCooldowns()
        }
    }

    fun toggleOrderedGeminiModel(rawModelId: String) {
        val cachedIds = _geminiAvailableModels.value.map { it.id }.toSet()
        val clean = rawModelId.trim().removePrefix("models/").trim()
        val (next, changed) = GeminiApiConfig.toggleOrderedModel(
            current = _geminiOrderedModels.value,
            rawModelId = clean,
            cachedIds = cachedIds + clean
        )
        if (!changed) {
            if (_geminiOrderedModels.value.size >= GeminiApiConfig.MAX_ORDERED_MODELS) {
                showMessage("সর্বোচ্চ ৩টি মডেল নির্বাচন করা যায় — আগে একটি সরান বা ক্রমের স্লটে পরিবর্তন করুন")
            }
            return
        }
        _geminiOrderedModels.value = GeminiApiConfig.saveOrderedModels(appContext, next)
        directAiRepository.clearCooldowns()
    }

    fun setOrderedGeminiModelSlot(slotIndex: Int, rawModelId: String?) {
        val cachedIds = _geminiAvailableModels.value.map { it.id }.toSet()
        val cleanNew = rawModelId?.trim()?.removePrefix("models/")?.trim().orEmpty()
        if (cleanNew.isNotBlank() && cleanNew !in cachedIds) {
            GeminiApiConfig.rememberCustomModel(appContext, cleanNew)
        }
        val updated = GeminiApiConfig.assignOrderedModelSlot(
            current = _geminiOrderedModels.value,
            slotIndex = slotIndex,
            rawModelId = cleanNew.ifBlank { null },
            cachedIds = if (cleanNew.isNotBlank()) cachedIds + cleanNew else cachedIds
        )
        _geminiOrderedModels.value = GeminiApiConfig.saveOrderedModels(appContext, updated)
        _geminiCustomModels.value = GeminiApiConfig.getCustomModels(appContext)
        directAiRepository.clearCooldowns()
    }

    fun moveOrderedGeminiModel(index: Int, delta: Int) {
        val reordered = GeminiApiConfig.moveOrderedModelSlot(_geminiOrderedModels.value, index, delta)
        if (reordered != _geminiOrderedModels.value) {
            _geminiOrderedModels.value = GeminiApiConfig.saveOrderedModels(appContext, reordered)
            directAiRepository.clearCooldowns()
        }
    }

    fun addCustomGeminiModel(rawModelId: String) {
        val clean = rawModelId.trim().removePrefix("models/").trim()
        if (clean.isBlank()) return
        if (GeminiApiConfig.isExcludedModelId(clean)) {
            showMessage("এই মডেলটি টেক্সট/ছবি বিশ্লেষণের জন্য উপযুক্ত নয়")
            return
        }
        _geminiCustomModels.value = GeminiApiConfig.rememberCustomModel(appContext, clean)
        val cur = _geminiOrderedModels.value
        if (clean !in cur) {
            if (cur.size < GeminiApiConfig.MAX_ORDERED_MODELS) {
                _geminiOrderedModels.value = GeminiApiConfig.saveOrderedModels(appContext, cur + clean)
                directAiRepository.clearCooldowns()
                showMessage("$clean মডেলটি ক্রম ${bn((cur.size + 1).toString())}-এ যুক্ত হয়েছে")
            } else {
                showMessage("$clean কাস্টম তালিকায় যুক্ত হয়েছে; ৩টি স্লট পূর্ণ থাকায় স্লটে ট্যাপ করে বসান")
            }
        }
    }

    fun saveGeminiApiKey(rawApiKey: String) {
        val savedKey = GeminiApiConfig.saveApiKey(appContext, rawApiKey)
        directAiRepository.clearCooldowns()
        _geminiApiKey.value = savedKey
        _geminiTestStatus.value = null
        _geminiTestSuccess.value = null
        showMessage(
            if (savedKey.isNotBlank()) "Gemini API Key সংরক্ষিত হয়েছে"
            else "API Key খালি আছে"
        )
    }

    fun saveGeminiConfig(rawApiKey: String, orderedModels: List<String> = _geminiOrderedModels.value) {
        GeminiApiConfig.saveConfig(appContext, rawApiKey, orderedModels)
        val savedKey = GeminiApiConfig.getApiKey(appContext)
        val savedOrdered = GeminiApiConfig.getOrderedModels(appContext)
        directAiRepository.clearCooldowns()
        _geminiApiKey.value = savedKey
        _geminiOrderedModels.value = savedOrdered
        _geminiCustomModels.value = GeminiApiConfig.getCustomModels(appContext)
        _geminiTestStatus.value = null
        _geminiTestSuccess.value = null
        showMessage(
            when {
                savedKey.isBlank() -> "API Key দিন এবং পছন্দের ৩টি মডেল নির্বাচন করুন"
                savedOrdered.isEmpty() -> "API Key সংরক্ষিত হয়েছে; এবার লাইভ মডেল থেকে পছন্দের ৩টি মডেল বাছুন"
                else -> "API Key ও ${bn(savedOrdered.size.toString())}টি মডেলের ক্রম সংরক্ষিত হয়েছে"
            }
        )
    }

    fun clearGeminiApiKey() {
        GeminiApiConfig.clearApiKey(appContext)
        directAiRepository.clearCooldowns()
        _geminiApiKey.value = ""
        _geminiAvailableModels.value = emptyList()
        _geminiNewModelIds.value = emptySet()
        _geminiModelsError.value = null
        _geminiTestStatus.value = null
        _geminiTestSuccess.value = null
        showMessage("Gemini API Key মুছে ফেলা হয়েছে")
    }

    fun testGeminiConnection(rawApiKey: String) {
        if (_geminiTesting.value) return
        val cleanKey = GeminiApiConfig.normalizeApiKey(rawApiKey)
        if (cleanKey.isBlank()) {
            _geminiTestSuccess.value = false
            _geminiTestStatus.value = "পরীক্ষার জন্য আপনার Gemini API Key লিখুন।"
            return
        }
        val firstOrdered = _geminiOrderedModels.value.firstOrNull().orEmpty()
        directAiRepository.clearCooldowns()
        _geminiTesting.value = true
        _geminiTestStatus.value = null
        _geminiTestSuccess.value = null
        _geminiModelsError.value = null
        viewModelScope.launch {
            val result = DirectGeminiAiRepository.testConnectionAndListModels(cleanKey, firstOrdered)
            _geminiTestSuccess.value = result.ok
            _geminiTestStatus.value = result.message
            if (result.availableModels.isNotEmpty()) {
                applyDiscoveredModels(result.availableModels)
            }
            if (result.ok || result.availableModels.isNotEmpty()) {
                GeminiApiConfig.saveApiKey(appContext, cleanKey)
                _geminiApiKey.value = cleanKey
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

    suspend fun resolveSheetTemplateRemote(templateId: String): BlankSheetQrMeta? {
        val schoolId = activeSchool.value?.id?.takeIf { it.isNotBlank() } ?: return null
        val tid = templateId.trim().uppercase().takeIf { it.length >= 6 } ?: return null
        return cloudRepo.fetchSheetTemplate(schoolId, tid).getOrNull()?.also { fetched ->
            withContext(Dispatchers.IO) { sheetTemplateCache.put(fetched) }
        }
    }
}
