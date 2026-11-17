package com.kgps.result

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kgps.result.ai.AiChatMessage
import com.kgps.result.ai.AiModelMode
import com.kgps.result.ai.AiRepository
import com.kgps.result.ai.ExtractedMarksheet
import com.kgps.result.ai.PlaceholderAiRepository
import com.kgps.result.cloud.CloudSessionState
import com.kgps.result.cloud.JoinRequestItem
import com.kgps.result.cloud.MemberRole
import com.kgps.result.cloud.SchoolCloudRepository
import com.kgps.result.cloud.SchoolCodePreview
import com.kgps.result.cloud.SchoolWorkspace
import com.kgps.result.cloud.StudentSyncMeta
import com.kgps.result.cloud.UserCloudProfile
import com.kgps.result.cloud.friendlyFirestoreErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
    val modelMode: AiModelMode = AiModelMode.GENERAL,
    val contextStudentId: String? = null,
    val chatMessages: List<AiChatMessage> = listOf(
        AiChatMessage(
            id = "welcome-msg",
            isUser = false,
            text = "আসসালামু আলাইকুম! আমি আপনার শ্রেণিকক্ষ সহায়ক। নম্বর বিভাজন, অভিভাবক সভার সারসংক্ষেপ বা প্রান্তিক মতামতের খসড়া নিয়ে প্রশ্ন করতে পারেন।\n\nনোট: লাইভ Gemini AI ব্যবহারের জন্য Firebase AI Logic প্রয়োজন — বর্তমানে নমুনা (Placeholder) উত্তর প্রদর্শিত হচ্ছে।",
            modelLabel = "Sample Mode",
            isPlaceholder = true
        )
    ),
    val chatLoading: Boolean = false,
    val chatError: String? = null,
    val selectedImageBitmap: Bitmap? = null,
    val analyzingImage: Boolean = false,
    val analysisError: String? = null,
    val analysisResult: ExtractedMarksheet? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class ResultMakerViewModel(
    private val cloudRepo: SchoolCloudRepository,
    private val contentResolver: ContentResolver,
    val currentUserId: String,
    val currentUserDisplayName: String,
    val currentUserEmail: String,
    private val initialLocalSnapshot: Pair<List<Student>, Settings>? = null
) : ViewModel() {

    private val aiRepository: AiRepository = PlaceholderAiRepository()

    val localSeedCount: Int get() = initialLocalSnapshot?.first?.size ?: 0
    val localSeedSettings: Settings? get() = initialLocalSnapshot?.second

    private val _isSigningOut = MutableStateFlow(false)
    private val _suppressedSchoolId = MutableStateFlow<String?>(null)
    private val _selectedSchoolOverrideId = MutableStateFlow<String?>(null)
    private val _schoolsLoaded = MutableStateFlow(false)
    private val _cloudInitError = MutableStateFlow<String?>(null)

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
    val pendingJoinSchoolName: StateFlow<String?> = _pendingJoinSchoolName.asStateFlow()

    val userProfile: StateFlow<UserCloudProfile?> = cloudRepo.observeUserProfile()
        .catch { e ->
            if (e is CancellationException) throw e
            if (!_isSigningOut.value) {
                _snackbarMessage.value = friendlyFirestoreErrorMessage(e)
            }
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val mySchools: StateFlow<List<SchoolWorkspace>> = cloudRepo.observeMySchools()
        .map { list ->
            _schoolsLoaded.value = true
            _cloudInitError.value = null
            list.sortedBy { it.settings.school }
        }
        .catch { e ->
            if (e is CancellationException) throw e
            _schoolsLoaded.value = true
            if (!_isSigningOut.value) {
                _cloudInitError.value = friendlyFirestoreErrorMessage(e)
            }
            emit(emptyList())
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
        activeSchool
            .map { it?.id }
            .distinctUntilChanged()
            .flatMapLatest { schoolId ->
                if (schoolId.isNullOrBlank()) {
                    flowOf(emptyList<Student>() to emptyMap())
                } else {
                    cloudRepo.observeSchoolStudents(schoolId)
                        .catch { e ->
                            if (e is CancellationException) throw e
                            if (!_isSigningOut.value && _suppressedSchoolId.value != schoolId) {
                                _snackbarMessage.value = friendlyFirestoreErrorMessage(e)
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
                            if (e is CancellationException) throw e
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

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _aiState = MutableStateFlow(AiAssistantState())
    val aiState: StateFlow<AiAssistantState> = _aiState.asStateFlow()

    private val _draftingRemarkTerm = MutableStateFlow<Int?>(null)
    val draftingRemarkTerm: StateFlow<Int?> = _draftingRemarkTerm.asStateFlow()

    val isPlaceholderAi: Boolean get() = aiRepository.isPlaceholderBackend

    init {
        viewModelScope.launch {
            cloudRepo.ensureUserProfile().onFailure { e ->
                if (!_isSigningOut.value) {
                    _snackbarMessage.value = friendlyFirestoreErrorMessage(e)
                }
            }
        }
    }

    fun prepareForSignOut() {
        _isSigningOut.value = true
        _suppressedSchoolId.value = activeSchool.value?.id
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
        approvalRequired: Boolean,
        importLocalData: Boolean
    ) {
        if (_hubBusy.value) return
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
                approvalRequired = approvalRequired,
                baseSettings = seedSettings,
                initialStudents = seedStudents
            ).onSuccess { created ->
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
        _hubBusy.value = true
        viewModelScope.launch {
            cloudRepo.joinSchoolWithCode(preview)
                .onSuccess { joinedDirectly ->
                    _codePreview.value = null
                    _lookupError.value = null
                    if (joinedDirectly) {
                        _pendingJoinSchoolName.value = null
                        _selectedSchoolOverrideId.value = preview.schoolId
                        _inSchoolHub.value = false
                        _currentSection.value = NavSection.ROSTER
                        showMessage("\"${preview.schoolName}\"-এ যুক্ত হয়েছেন!")
                    } else {
                        _pendingJoinSchoolName.value = preview.schoolName
                        showMessage("যোগদানের অনুরোধ পাঠানো হয়েছে — অ্যাডমিন অনুমোদনের অপেক্ষায়")
                    }
                }
                .onFailure { e ->
                    showMessage(friendlyFirestoreErrorMessage(e))
                }
            _hubBusy.value = false
        }
    }

    fun approveJoinRequest(req: JoinRequestItem) {
        val school = activeSchool.value ?: return
        if (!school.isAdmin(currentUserId)) {
            showMessage("শুধুমাত্র অ্যাডমিন অনুমোদন করতে পারবেন")
            return
        }
        viewModelScope.launch {
            cloudRepo.approveJoinRequest(school.id, req)
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
            cloudRepo.setApprovalRequired(school, required)
                .onSuccess {
                    showMessage(if (required) "অ্যাডমিন অনুমোদন চালু হয়েছে" else "সরাসরি যোগদান চালু হয়েছে")
                }
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
        val cls = _selectedClass.value
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
        _editingStudent.value = null
        _studioStudentId.value = studentId
        _currentSection.value = NavSection.STUDIO
    }

    fun selectStudioStudent(studentId: String) {
        _studioStudentId.value = studentId
    }

    fun openCsvImport(initialText: String = "") {
        _csvImportState.value = CsvImportModalState(isOpen = true, initialText = initialText)
    }

    fun closeCsvImport() {
        _csvImportState.value = CsvImportModalState(isOpen = false, initialText = "")
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearMessage() {
        _snackbarMessage.value = null
    }

    fun upsertStudent(student: Student) {
        val schoolId = activeSchool.value?.id ?: return
        val existing = students.value.find { it.id == student.id }
        viewModelScope.launch {
            cloudRepo.upsertStudent(schoolId, student, existing).onFailure { e ->
                showMessage(friendlyFirestoreErrorMessage(e))
            }
        }
    }

    fun patchStudentField(studentId: String, fieldPath: String, value: Any?) {
        val schoolId = activeSchool.value?.id ?: return
        viewModelScope.launch {
            cloudRepo.patchStudentField(schoolId, studentId, fieldPath, value).onFailure { e ->
                showMessage(friendlyFirestoreErrorMessage(e))
            }
        }
    }

    fun deleteStudent(id: String) {
        val schoolId = activeSchool.value?.id ?: return
        val target = students.value.find { it.id == id }
        if (_studioStudentId.value == id) _studioStudentId.value = null
        viewModelScope.launch {
            cloudRepo.deleteStudent(schoolId, id)
                .onSuccess {
                    val label = target?.name?.ifBlank { "রোল ${bn(target.roll.toString())}" } ?: "শিক্ষার্থী"
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
        val prevList = students.value
        closeCsvImport()
        viewModelScope.launch {
            if (summary.createdFields.isNotEmpty() && school.isAdmin(currentUserId)) {
                cloudRepo.updateSchoolSettings(school, summary.settings)
            }
            cloudRepo.syncBatchStudents(school.id, prevList, summary.students, deleteRemoved = false)
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

    // ---------- AI Assistant & Drafting ----------

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
                        text = "নতুন কথোপকথন শুরু হয়েছে। শ্রেণিকক্ষ বা ফলাফল সংক্রান্ত যেকোনো প্রশ্ন করুন।\n\nনোট: লাইভ Gemini AI-এর জন্য Firebase AI Logic প্রয়োজন।",
                        modelLabel = "Sample Mode",
                        isPlaceholder = true
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
                decodeAndScaleBitmap(uri, 1600)
            }
            if (bmp == null) {
                showMessage("ছবি পড়া যায়নি")
            } else {
                _aiState.update {
                    it.copy(
                        selectedImageBitmap = bmp,
                        analysisResult = null,
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
                analysisError = null
            )
        }
    }

    fun analyzeSelectedMarksheet(teacherNote: String) {
        val bmp = _aiState.value.selectedImageBitmap ?: return
        if (_aiState.value.analyzingImage) return
        _aiState.update { it.copy(analyzingImage = true, analysisError = null) }
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

    fun applyExtractedMarksheet(ext: ExtractedMarksheet): Student {
        val cls = (ext.student.cls ?: _selectedClass.value).coerceIn(1, 5)
        val roll = ext.student.roll?.takeIf { it > 0 }
            ?: ((students.value.filter { it.cls == cls }.maxOfOrNull { it.roll } ?: 0) + 1)

        val existing = students.value.find { it.cls == cls && it.roll == roll }
        val base = existing ?: createEmptyStudent(cls, roll)

        val nextM = base.m.toMutableMap()
        ext.marks.forEach { item ->
            nextM[item.key] = item.value
        }

        val t = ext.term.coerceIn(1, 3)
        val nextX = base.x.toMutableMap()
        ext.attendanceAndTraits.days?.takeIf { it.isNotBlank() }?.let { nextX["DAYS_$t"] = it }
        ext.attendanceAndTraits.present?.takeIf { it.isNotBlank() }?.let { nextX["PRES_$t"] = it }
        ext.attendanceAndTraits.act?.takeIf { it.isNotBlank() }?.let { nextX["ACT_$t"] = it }
        ext.attendanceAndTraits.disc?.takeIf { it.isNotBlank() }?.let { nextX["DISC_$t"] = it }
        ext.attendanceAndTraits.cond?.takeIf { it.isNotBlank() }?.let { nextX["COND_$t"] = it }
        ext.attendanceAndTraits.remark?.takeIf { it.isNotBlank() }?.let { nextX["REM_$t"] = it }

        val updated = base.copy(
            name = ext.student.name?.takeIf { it.isNotBlank() } ?: base.name,
            guardian = ext.student.guardian?.takeIf { it.isNotBlank() } ?: base.guardian,
            village = ext.student.village?.takeIf { it.isNotBlank() } ?: base.village,
            upazila = ext.student.upazila?.takeIf { it.isNotBlank() } ?: base.upazila,
            district = ext.student.district?.takeIf { it.isNotBlank() } ?: base.district,
            mobile = ext.student.mobile?.takeIf { it.isNotBlank() } ?: base.mobile,
            religion = ext.student.religion?.takeIf { it.isNotBlank() } ?: base.religion,
            m = nextM,
            x = nextX
        )
        upsertStudent(updated)
        _selectedClass.value = cls
        showMessage("${updated.name.ifBlank { "রোল ${bn(roll.toString())}" }} শিক্ষার্থীর তথ্য সংরক্ষিত হয়েছে")
        return updated
    }

    fun draftRemark(student: Student, term: Int, onResult: (String) -> Unit) {
        if (_draftingRemarkTerm.value != null) return
        _draftingRemarkTerm.value = term
        viewModelScope.launch {
            try {
                val res = aiRepository.draftRemark(buildStudentAiContext(student), term)
                onResult(res.remark)
                if (res.isPlaceholder) {
                    showMessage("নমুনা মতামত খসড়া যুক্ত হয়েছে (লাইভ AI-এর জন্য Firebase AI Logic প্রয়োজন)")
                }
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
