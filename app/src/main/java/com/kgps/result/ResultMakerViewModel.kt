package com.kgps.result

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kgps.result.ai.AiChatMessage
import com.kgps.result.ai.AiModelMode
import com.kgps.result.ai.AiRepository
import com.kgps.result.ai.ExtractedMarksheet
import com.kgps.result.ai.PlaceholderAiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
            text = "আসসালামু আলাইকুম! আমি আপনার শ্রেণিকক্ষ সহায়ক। নম্বর বিভাজন, অভিভাবক সভার সারসংক্ষেপ বা প্রান্তিক মতামতের খসড়া নিয়ে প্রশ্ন করতে পারেন।\n\nনোট: লাইভ Gemini AI ব্যবহারের জন্য Firebase প্রয়োজন — বর্তমানে নমুনা (Placeholder) উত্তর প্রদর্শিত হচ্ছে।",
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

class ResultMakerViewModel(application: Application) : AndroidViewModel(application) {
    private val aiRepository: AiRepository = PlaceholderAiRepository()

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students.asStateFlow()

    private val _settings = MutableStateFlow(DEFAULT_SETTINGS)
    val settings: StateFlow<Settings> = _settings.asStateFlow()

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
        val (loadedStudents, loadedSettings) = Repo.load(application)
        _students.value = loadedStudents
        _settings.value = loadedSettings
    }

    private fun persist(nextStudents: List<Student> = _students.value, nextSettings: Settings = _settings.value) {
        viewModelScope.launch(Dispatchers.IO) {
            Repo.save(getApplication(), nextStudents, nextSettings)
        }
    }

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
        val nextRoll = (_students.value.filter { it.cls == cls }.maxOfOrNull { it.roll } ?: 0) + 1
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
        val cur = _students.value
        val next = if (cur.any { it.id == student.id }) {
            cur.map { if (it.id == student.id) student else it }
        } else {
            cur + student
        }
        _students.value = next
        persist(nextStudents = next)
    }

    fun deleteStudent(id: String) {
        val target = _students.value.find { it.id == id }
        val next = _students.value.filter { it.id != id }
        _students.value = next
        if (_studioStudentId.value == id) _studioStudentId.value = null
        persist(nextStudents = next)
        val label = target?.name?.ifBlank { "রোল ${bn(target.roll.toString())}" } ?: "শিক্ষার্থী"
        showMessage("$label মুছে ফেলা হয়েছে")
    }

    fun updateSettings(newSettings: Settings) {
        _settings.value = newSettings
        persist(nextSettings = newSettings)
    }

    fun addCustomField(label: String): Boolean {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return false
        val existing = _settings.value.customFields
        if (existing.any { it.label.equals(trimmed, ignoreCase = true) }) {
            showMessage("এই নামে একটি ফিল্ড ইতিমধ্যে আছে")
            return false
        }
        val key = Repo.makeCustomFieldKey(trimmed, existing.map { it.key }.toSet())
        val next = _settings.value.copy(customFields = existing + CustomFieldDef(key, trimmed))
        updateSettings(next)
        return true
    }

    fun removeCustomField(key: String) {
        val removed = _settings.value.customFields.find { it.key == key }
        val next = _settings.value.copy(
            customFields = _settings.value.customFields.filter { it.key != key }
        )
        updateSettings(next)
        if (removed != null) {
            showMessage("\"${removed.label}\" ফিল্ড মুছে ফেলা হয়েছে")
        }
    }

    fun applyCsvImport(summary: Repo.CsvImportSummary) {
        _students.value = summary.students
        _settings.value = summary.settings
        persist(summary.students, summary.settings)
        closeCsvImport()
        val msg = buildString {
            append("ইমপোর্ট সম্পন্ন: নতুন ${bn(summary.addedCount.toString())} জন, হালনাগাদ ${bn(summary.updatedCount.toString())} জন")
            if (summary.createdFields.isNotEmpty()) {
                append(", নতুন ফিল্ড ${bn(summary.createdFields.size.toString())}টি")
            }
        }
        showMessage(msg)
    }

    fun restoreBackupJson(rawText: String) {
        try {
            val (restoredStudents, restoredSettings) = Repo.fromJson(rawText)
            _students.value = restoredStudents
            _settings.value = restoredSettings
            persist(restoredStudents, restoredSettings)
            showMessage("ব্যাকআপ রিস্টোর হয়েছে (${bn(restoredStudents.size.toString())} জন শিক্ষার্থী)")
        } catch (e: Exception) {
            showMessage("রিস্টোর ব্যর্থ হয়েছে: ${e.message ?: "অবৈধ ফাইল"}")
        }
    }

    fun resetSampleData() {
        _students.value = SAMPLE_STUDENTS
        _settings.value = DEFAULT_SETTINGS
        persist(SAMPLE_STUDENTS, DEFAULT_SETTINGS)
        showMessage("নমুনা ডাটা রিসেট করা হয়েছে")
    }

    fun clearAllStudents() {
        _students.value = emptyList()
        _studioStudentId.value = null
        persist(nextStudents = emptyList())
        showMessage("সকল শিক্ষার্থীর তথ্য মুছে ফেলা হয়েছে")
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
                        text = "নতুন কথোপকথন শুরু হয়েছে। শ্রেণিকক্ষ বা ফলাফল সংক্রান্ত যেকোনো প্রশ্ন করুন।\n\nনোট: লাইভ Gemini AI-এর জন্য Firebase প্রয়োজন।",
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
            _students.value.find { it.id == id }
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
            ?: ((_students.value.filter { it.cls == cls }.maxOfOrNull { it.roll } ?: 0) + 1)

        val existing = _students.value.find { it.cls == cls && it.roll == roll }
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
                    showMessage("নমুনা মতামত খসড়া যুক্ত হয়েছে (লাইভ AI-এর জন্য Firebase প্রয়োজন)")
                }
            } catch (e: Exception) {
                showMessage("খসড়া তৈরি করা যায়নি: ${e.message ?: "ত্রুটি"}")
            } finally {
                _draftingRemarkTerm.value = null
            }
        }
    }

    private fun decodeAndScaleBitmap(uri: Uri, maxDim: Int): Bitmap? {
        val resolver = getApplication<Application>().contentResolver
        return try {
            val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(resolver, uri)
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
                resolver.openInputStream(uri)?.use { stream ->
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
