package com.kgps.result.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.kgps.result.AiAssistantState
import com.kgps.result.BlankSheetCatalog
import com.kgps.result.RELIGIONS
import com.kgps.result.Student
import com.kgps.result.ui.components.BiometricDeleteConfirmDialog
import com.kgps.result.ai.AiModelMode
import com.kgps.result.ai.ClassSheetImportScope
import com.kgps.result.ai.ClassSheetKind
import com.kgps.result.ai.DiscoveredGeminiModel
import com.kgps.result.ai.ExtractedClassSheet
import com.kgps.result.ai.ExtractedMarksheet
import com.kgps.result.ai.FieldConfidence
import com.kgps.result.ai.GeminiApiConfig
import com.kgps.result.ai.RowValidationReport
import com.kgps.result.ai.ScannedStudentRow
import com.kgps.result.ai.validateScannedStudentRow
import com.kgps.result.bn
import com.kgps.result.classNameBn
import com.kgps.result.classShortBn
import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.fmt
import com.kgps.result.fmtEn
import com.kgps.result.fromBn
import com.kgps.result.subjectsFor
import com.kgps.result.termNameBn
import com.kgps.result.termOrdinalBn
import com.kgps.result.ui.components.ImageCropDialog
import com.kgps.result.ui.theme.AmberBg
import com.kgps.result.ui.theme.AmberBorder
import com.kgps.result.ui.theme.AmberNotice
import com.kgps.result.ui.theme.EmeraldGrade
import com.kgps.result.ui.theme.RoseBg
import com.kgps.result.ui.theme.RoseDanger
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateSurfaceVariant
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealContainer
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft
import com.kgps.result.ui.theme.appFilterChipBorder
import com.kgps.result.ui.theme.appFilterChipColors
import java.io.File

private fun createTempScanImageUri(context: Context): Uri {
    val imgFile = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imgFile)
}

@Composable
fun AiAssistantScreen(
    state: AiAssistantState,
    students: List<Student>,
    selectedClass: Int,
    permissions: TeacherPermissions,
    isGeminiConfigured: Boolean,
    activeModelName: String,
    availableModels: List<DiscoveredGeminiModel> = emptyList(),
    customModels: List<String> = emptyList(),
    newModelIds: Set<String> = emptySet(),
    refreshingModels: Boolean = false,
    modelsError: String? = null,
    onSelectModel: (String) -> Unit = {},
    onRefreshModels: () -> Unit = {},
    onRemoveCustomModel: (String) -> Unit = {},
    onOpenAiSettings: () -> Unit,
    onSetActiveTab: (Int) -> Unit,
    onSetModelMode: (AiModelMode) -> Unit,
    onSetContextStudent: (String?) -> Unit,
    onClearChat: () -> Unit,
    onSendChat: (String) -> Unit,
    onOpenCsvImport: () -> Unit,
    onPickMarksheetUri: (Uri) -> Unit,
    onClearImage: () -> Unit,
    onToggleDocumentEnhance: (Boolean) -> Unit = {},
    onRotateImage: () -> Unit = {},
    onCropImage: (Float, Float, Float, Float) -> Unit = { _, _, _, _ -> },
    onRestoreOriginalImage: () -> Unit = {},
    onAnalyzeImage: (String) -> Unit,
    onApplyExtracted: (ExtractedMarksheet) -> Unit,
    onAnalyzeClassSheet: (clsHint: Int?, termHint: Int?, subKeyHint: String?, teacherNote: String, importScope: ClassSheetImportScope, sheetKindHint: ClassSheetKind?) -> Unit,
    onAnalyzeAutoSheet: (clsHint: Int?, termHint: Int?, subKeyHint: String?, teacherNote: String, importScope: ClassSheetImportScope, sheetKindHint: ClassSheetKind?) -> Unit = { c, t, s, note, scope, kind ->
        onAnalyzeClassSheet(c, t, s, note, scope, kind)
    },
    onLoadSampleClassPreview: (clsHint: Int?, termHint: Int?, subKeyHint: String?, importScope: ClassSheetImportScope, sheetKindHint: ClassSheetKind?) -> Unit,
    onUpdateClassSheetMeta: (cls: Int, term: Int, subKey: String) -> Unit,
    onUpdateClassSheetScope: (ClassSheetImportScope) -> Unit = {},
    onUpdateClassSheetKind: (ClassSheetKind) -> Unit = {},
    onUpdateClassSheetSharedWorkingDays: (Int, String) -> Unit = { _, _ -> },
    onToggleScannedRowSelected: (rowId: String) -> Unit,
    onSetAllScannedRowsSelected: (Boolean) -> Unit,
    onUpdateScannedRowField: (rowId: String, rollText: String?, name: String?, markEntry: Pair<String, String>?, verifyField: String?) -> Unit,
    onAddBlankScannedRow: () -> Unit,
    onRemoveScannedRow: (rowId: String) -> Unit,
    onMarkAllScannedIssuesVerified: () -> Unit,
    onDismissClassSheetPreview: () -> Unit,
    onCommitClassSheet: () -> Unit
) {
    val context = LocalContext.current
    var chatInput by remember { mutableStateOf("") }
    var teacherNote by remember { mutableStateOf("") }
    var contextDropdownOpen by remember { mutableStateOf(false) }
    var askClearChat by remember { mutableStateOf(false) }
    var askClearImage by remember { mutableStateOf(false) }
    var pendingRemoveScannedRowId by remember { mutableStateOf<String?>(null) }
    var pendingRemoveModelId by remember { mutableStateOf<String?>(null) }
    var showModelPicker by remember { mutableStateOf(false) }

    // Scanner mode & selectors (auto-detect until scanned or manually overridden)
    var scanMode by remember { mutableStateOf("auto") } // "auto" | "class" | "single"
    var importScope by remember { mutableStateOf(ClassSheetImportScope.AUTO) }
    var sheetKindHint by remember { mutableStateOf<ClassSheetKind?>(null) } // null = অটো
    var clsHint by remember { mutableStateOf<Int?>(null) }
    var termHint by remember { mutableStateOf<Int?>(null) }
    var subKeyHint by remember { mutableStateOf<String?>(null) } // null = ছবির উপর থেকে স্বয়ংক্রিয়
    var autoFilledFromScan by remember { mutableStateOf(false) }

    val resetToAuto: () -> Unit = {
        scanMode = "auto"
        sheetKindHint = null
        clsHint = null
        termHint = null
        subKeyHint = null
        importScope = ClassSheetImportScope.AUTO
        autoFilledFromScan = false
    }

    var zoomDialogOpen by remember { mutableStateOf(false) }
    var cropDialogOpen by remember { mutableStateOf(false) }
    var showOnlyIssues by remember { mutableStateOf(false) }
    var askUnreviewedConfirm by remember { mutableStateOf(false) }
    var unreviewedPendingCount by remember { mutableStateOf(0) }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var activeConfigMenu by remember { mutableStateOf<ScannerConfigMenu?>(null) }

    LaunchedEffect(state.classSheetResult, state.analysisResult) {
        if (state.classSheetResult != null || state.analysisResult != null) {
            activeConfigMenu = null
        }
    }

    LaunchedEffect(state.selectedImageBitmap) {
        if (state.selectedImageBitmap != null &&
            !state.hasOriginalImageBackup &&
            state.detectedSheetQr == null &&
            state.classSheetResult == null &&
            state.analysisResult == null &&
            autoFilledFromScan
        ) {
            resetToAuto()
        }
    }

    LaunchedEffect(state.detectedSheetQr) {
        val qr = state.detectedSheetQr ?: return@LaunchedEffect
        scanMode = "class"
        clsHint = qr.cls.takeIf { it in 1..20 }
        termHint = qr.term.takeIf { it in 1..3 }
        subKeyHint = qr.subjectKey?.takeIf {
            it.isNotBlank() && it != BlankSheetCatalog.ALL_SUBJECTS_KEY
        }
        sheetKindHint = qr.sheetKind
        importScope = qr.importScope
        autoFilledFromScan = true
    }

    LaunchedEffect(state.classSheetResult) {
        val sheet = state.classSheetResult ?: return@LaunchedEffect
        scanMode = "class"
        sheetKindHint = sheet.sheetKind
        clsHint = sheet.cls.takeIf { it in 1..20 }
        termHint = if (sheet.sheetKind == ClassSheetKind.SINGLE_TERM_MARKS) sheet.term.takeIf { it in 1..3 } else null
        subKeyHint = if (sheet.sheetKind == ClassSheetKind.ATTENDANCE_TRAITS) null else sheet.subKey.takeIf { it.isNotBlank() }
        importScope = sheet.scope
        autoFilledFromScan = true
    }

    LaunchedEffect(state.analysisResult) {
        val single = state.analysisResult ?: return@LaunchedEffect
        scanMode = "single"
        clsHint = single.student.cls?.takeIf { it in 1..20 }
        termHint = single.term.takeIf { it in 1..3 }
        subKeyHint = single.marks
            .mapNotNull { it.key.substringBefore('_').takeIf { k -> k.isNotBlank() } }
            .distinct()
            .singleOrNull()
        sheetKindHint = null
        importScope = ClassSheetImportScope.AUTO
        autoFilledFromScan = true
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onPickMarksheetUri(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            onPickMarksheetUri(uri)
        }
    }

    val sortedStudents = remember(students) {
        students.sortedWith(compareBy({ it.cls }, { it.roll }))
    }
    val selectedStudent = sortedStudents.find { it.id == state.contextStudentId }

    Column(
        modifier = Modifier.fillMaxSize().imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.widthIn(max = 920.dp).fillMaxSize()) {
            // Compact Top Header: Model Selector Bar + Segmented Tabs
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!isGeminiConfigured) {
                    Surface(
                        color = AmberBg,
                        border = BorderStroke(1.dp, AmberBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Info,
                                contentDescription = "সতর্কীকরণ",
                                tint = AmberNotice,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "API Key নেই · মডেল: $activeModelName",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = AmberNotice,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = Color.White,
                                border = BorderStroke(1.dp, AmberBorder),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .clickable { showModelPicker = true }
                                    .testTag("ai_change_model_btn")
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("মডেল", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = AmberNotice)
                                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = AmberNotice, modifier = Modifier.size(14.dp))
                                }
                            }
                            Surface(
                                color = AmberNotice,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .clickable { onOpenAiSettings() }
                                    .testTag("ai_open_settings_btn")
                            ) {
                                Text(
                                    "কী দিন",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        color = TealSoft,
                        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showModelPicker = true }
                            .testTag("ai_change_model_btn")
                    ) {
                        Row(
                            Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "মডেল: $activeModelName",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TealDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = TealContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        "পরিবর্তন",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TealDark
                                    )
                                    Icon(
                                        Icons.Rounded.ArrowDropDown,
                                        contentDescription = "মডেল পরিবর্তন",
                                        tint = TealDark,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Compact Segmented Tab Bar
                Surface(
                    color = SlateSurfaceVariant,
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isChatTab = state.activeTab == 0
                        Surface(
                            color = if (isChatTab) TealPrimary else Color.Transparent,
                            shape = RoundedCornerShape(7.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSetActiveTab(0) }
                                .testTag("ai_tab_chat")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = if (isChatTab) Color.White else SlateMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    "শিক্ষক চ্যাট",
                                    fontSize = 12.sp,
                                    fontWeight = if (isChatTab) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isChatTab) Color.White else SlateText,
                                    maxLines = 1
                                )
                            }
                        }

                        val isScanTab = state.activeTab == 1
                        Surface(
                            color = if (isScanTab) TealPrimary else Color.Transparent,
                            shape = RoundedCornerShape(7.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSetActiveTab(1) }
                                .testTag("ai_tab_scanner")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.DocumentScanner,
                                    contentDescription = null,
                                    tint = if (isScanTab) Color.White else SlateMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    "নম্বরপত্র স্ক্যান",
                                    fontSize = 12.sp,
                                    fontWeight = if (isScanTab) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isScanTab) Color.White else SlateText,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            if (state.activeTab == 0) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Single Compact Controls Row: Context Student + Mode Toggle + Clear
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.weight(1f)) {
                            Surface(
                                color = if (selectedStudent != null) TealSoft else Color.White,
                                border = BorderStroke(1.dp, if (selectedStudent != null) TealPrimary.copy(alpha = 0.5f) else SlateOutline),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { contextDropdownOpen = true }
                                    .testTag("ai_context_student_btn")
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        selectedStudent?.let {
                                            "${classShortBn(it.cls)}·${bn(it.roll.toString())} ${it.name}"
                                        } ?: "প্রসঙ্গ: সাধারণ",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (selectedStudent != null) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (selectedStudent != null) TealDark else SlateText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        Icons.Rounded.ArrowDropDown,
                                        contentDescription = null,
                                        tint = SlateMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            DropdownMenu(expanded = contextDropdownOpen, onDismissRequest = { contextDropdownOpen = false }) {
                                val isNoneSel = state.contextStudentId == null
                                DropdownMenuItem(
                                    modifier = Modifier.background(if (isNoneSel) TealContainer else Color.Transparent),
                                    text = {
                                        Text(
                                            "সাধারণ / কোনো নির্দিষ্ট শিক্ষার্থী নয়",
                                            fontWeight = if (isNoneSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isNoneSel) TealDark else SlateText
                                        )
                                    },
                                    trailingIcon = if (isNoneSel) {
                                        { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealDark, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    onClick = { onSetContextStudent(null); contextDropdownOpen = false }
                                )
                                sortedStudents.forEach { s ->
                                    val isStuSel = state.contextStudentId == s.id
                                    DropdownMenuItem(
                                        modifier = Modifier.background(if (isStuSel) TealContainer else Color.Transparent),
                                        text = {
                                            Text(
                                                "${classNameBn(s.cls)} · রোল ${bn(s.roll.toString())} — ${s.name.ifBlank { "(নাম নেই)" }}",
                                                fontWeight = if (isStuSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isStuSel) TealDark else SlateText
                                            )
                                        },
                                        trailingIcon = if (isStuSel) {
                                            { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealDark, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        onClick = { onSetContextStudent(s.id); contextDropdownOpen = false }
                                    )
                                }
                            }
                        }

                        // Compact Mode Switcher
                        Surface(
                            color = SlateSurfaceVariant,
                            border = BorderStroke(1.dp, SlateOutline),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(Modifier.padding(2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                val isGeneralMode = state.modelMode == AiModelMode.GENERAL
                                Surface(
                                    color = if (isGeneralMode) TealPrimary else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .clickable { onSetModelMode(AiModelMode.GENERAL) }
                                        .testTag("ai_mode_general")
                                ) {
                                    Text(
                                        "বিস্তারিত",
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isGeneralMode) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isGeneralMode) Color.White else SlateText,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                                    )
                                }
                                val isFastMode = state.modelMode == AiModelMode.FAST
                                Surface(
                                    color = if (isFastMode) TealPrimary else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .clickable { onSetModelMode(AiModelMode.FAST) }
                                        .testTag("ai_mode_fast")
                                ) {
                                    Text(
                                        "দ্রুত",
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isFastMode) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isFastMode) Color.White else SlateText,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        // Compact Clear Chat Action
                        Surface(
                            color = Color.White,
                            border = BorderStroke(1.dp, SlateOutline),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .clickable {
                                    if (state.chatMessages.size > 1) {
                                        askClearChat = true
                                    } else {
                                        onClearChat()
                                    }
                                }
                                .testTag("ai_clear_chat_btn")
                        ) {
                            Row(
                                Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Rounded.DeleteSweep, contentDescription = "নতুন চ্যাট", tint = SlateMuted, modifier = Modifier.size(14.dp))
                                Text("নতুন", fontSize = 10.5.sp, color = SlateText, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // Slim Quick Prompts Row
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        listOf(
                            "প্রান্তিক মতামত খসড়া লিখুন" to "মতামত খসড়া",
                            "NCTB প্রাথমিক মূল্যায়ন নম্বর বিভাজন ব্যাখ্যা করুন" to "নম্বর বিভাজন",
                            "অভিভাবক সভার জন্য সংক্ষিপ্ত সারসংক্ষেপ তৈরি করুন" to "অভিভাবক সভা"
                        ).forEachIndexed { idx, (prompt, shortLabel) ->
                            Surface(
                                color = Color.White,
                                border = BorderStroke(1.dp, SlateOutline),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .clickable { onSendChat(prompt) }
                                    .testTag("ai_quick_prompt_$idx")
                            ) {
                                Text(
                                    shortLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TealDark,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    LazyColumn(
                        Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(state.chatMessages, key = { it.id }) { msg ->
                            val isBot = !msg.isUser
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    color = if (msg.isUser) TealPrimary else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(10.dp),
                                    border = if (isBot) BorderStroke(1.dp, SlateOutline) else null,
                                    modifier = Modifier.widthIn(max = 560.dp)
                                ) {
                                    Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        if (isBot && !msg.modelLabel.isNullOrBlank()) {
                                            Text(
                                                msg.modelLabel,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TealPrimary
                                            )
                                        }
                                        Text(
                                            msg.text,
                                            fontSize = 12.5.sp,
                                            lineHeight = 18.sp,
                                            color = if (msg.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        if (state.chatLoading) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(2.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Text("উত্তর তৈরি হচ্ছে...", fontSize = 11.5.sp, color = SlateMuted)
                                }
                            }
                        }
                    }

                    state.chatError?.let { err ->
                        Surface(
                            color = RoseBg,
                            border = BorderStroke(1.dp, RoseDanger.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(7.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(err, color = RoseDanger, fontSize = 11.5.sp, lineHeight = 15.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(
                                        onClick = { showModelPicker = true },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        modifier = Modifier.testTag("ai_chat_err_settings_btn")
                                    ) {
                                        Text("মডেল পরিবর্তন করুন", fontSize = 11.sp)
                                    }
                                    if (!isGeminiConfigured) {
                                        TextButton(
                                            onClick = onOpenAiSettings,
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                        ) {
                                            Text("API Key দিন", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            placeholder = { Text("আপনার প্রশ্ন লিখুন...", fontSize = 12.5.sp) },
                            maxLines = 3,
                            textStyle = TextStyle(fontSize = 13.sp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("ai_chat_input")
                        )
                        Button(
                            onClick = {
                                val text = chatInput
                                chatInput = ""
                                onSendChat(text)
                            },
                            enabled = chatInput.isNotBlank() && !state.chatLoading,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 13.dp, vertical = 12.dp),
                            modifier = Modifier.testTag("ai_chat_send_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "পাঠান", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            } else {
                // Compact Top-Menu Scanner Tab (Single menu row -> tap to open only that menu)
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val preparedStatsInfo = state.preparedImageStats?.let { st ->
                        val kb = ((st.byteSize + 512) / 1024).coerceAtLeast(1)
                        val label = "প্রস্তুত: ${st.width}×${st.height}px · ~${bn(kb.toString())} KB" +
                            if (st.documentEnhanced) " · তীক্ষ্ণ সাদাকালো" else " · রঙিন"
                        label to (kb <= 650)
                    }

                    val triggerScan: () -> Unit = {
                        showOnlyIssues = false
                        when (scanMode) {
                            "auto" -> onAnalyzeAutoSheet(clsHint, termHint, subKeyHint, teacherNote, importScope, sheetKindHint)
                            "class" -> onAnalyzeClassSheet(clsHint, termHint, subKeyHint, teacherNote, importScope, sheetKindHint)
                            else -> onAnalyzeImage(teacherNote)
                        }
                    }

                    ScannerConfigMenuBar(
                        activeMenu = activeConfigMenu,
                        onToggleMenu = { activeConfigMenu = it },
                        scanMode = scanMode,
                        canManageStudents = permissions.canManageStudents,
                        importScope = importScope,
                        sheetKindHint = sheetKindHint,
                        clsHint = clsHint,
                        termHint = termHint,
                        subKeyHint = subKeyHint,
                        teacherNote = teacherNote,
                        permissions = permissions,
                        autoDetectedActive = autoFilledFromScan,
                        onResetToAuto = resetToAuto,
                        onChangeScanMode = {
                            scanMode = it
                            autoFilledFromScan = false
                        },
                        onOpenCsvImport = onOpenCsvImport,
                        onChangeScope = {
                            importScope = it
                            autoFilledFromScan = false
                        },
                        onChangeSheetKind = {
                            sheetKindHint = it
                            autoFilledFromScan = false
                        },
                        onChangeCls = { newCls ->
                            clsHint = newCls
                            autoFilledFromScan = false
                            if (newCls != null && subKeyHint != null) {
                                val validKeys = subjectsFor(newCls, RELIGIONS.first()).map { it.key }
                                if (subKeyHint !in validKeys) subKeyHint = null
                            }
                        },
                        onChangeTerm = {
                            termHint = it
                            autoFilledFromScan = false
                        },
                        onChangeSub = {
                            subKeyHint = it
                            autoFilledFromScan = false
                        },
                        onChangeTeacherNote = { teacherNote = it }
                    )

                    ScannerPhotoAndActionCard(
                        scanMode = scanMode,
                        selectedImageBitmap = state.selectedImageBitmap,
                        detectedSheetQr = state.detectedSheetQr,
                        sheetDetectionSource = state.sheetDetectionSource,
                        sheetDetection = state.sheetDetection,
                        resolvedTemplateMeta = state.resolvedTemplateMeta,
                        resolvedTemplateSource = state.resolvedTemplateSource,
                        resolvingSheetTemplate = state.resolvingSheetTemplate,
                        documentEnhance = state.documentEnhance,
                        preparedStatsLabel = preparedStatsInfo?.first,
                        preparedStatsGood = preparedStatsInfo?.second == true,
                        analyzingImage = state.analyzingImage,
                        onLaunchCamera = {
                            activeConfigMenu = null
                            val uri = createTempScanImageUri(context)
                            pendingCameraUri = uri
                            cameraLauncher.launch(uri)
                        },
                        onLaunchGallery = {
                            activeConfigMenu = null
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onAskClearImage = { askClearImage = true },
                        onOpenZoom = { zoomDialogOpen = true },
                        onOpenCrop = { cropDialogOpen = true },
                        onRotateImage = onRotateImage,
                        onToggleDocumentEnhance = onToggleDocumentEnhance,
                        onScan = {
                            activeConfigMenu = null
                            triggerScan()
                        },
                        onLoadSample = {
                            activeConfigMenu = null
                            showOnlyIssues = false
                            onLoadSampleClassPreview(clsHint, termHint, subKeyHint, importScope, sheetKindHint)
                        }
                    )

                state.analysisError?.let { err ->
                    Surface(
                        color = RoseBg,
                        border = BorderStroke(1.dp, RoseDanger.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = RoseDanger, modifier = Modifier.size(16.dp))
                                Text(err, color = RoseDanger, fontSize = 11.5.sp, lineHeight = 16.sp)
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                if (state.selectedImageBitmap != null && !state.analyzingImage) {
                                    OutlinedButton(
                                        onClick = {
                                            showOnlyIssues = false
                                            triggerScan()
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("ai_scan_retry_btn")
                                    ) {
                                        Text("আবার চেষ্টা করুন", fontSize = 11.sp)
                                    }
                                }
                                TextButton(
                                    onClick = { showModelPicker = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("ai_scan_err_settings_btn")
                                ) {
                                    Text("মডেল পরিবর্তন করুন", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                    // Class Sheet Interactive Preview & Manual Correction
                    state.classSheetResult?.let { sheet ->
                        ClassSheetPreviewSection(
                            sheet = sheet,
                            existingStudents = students,
                            permissions = permissions,
                            hasImage = state.selectedImageBitmap != null,
                            saving = state.savingClassSheet,
                            showOnlyIssues = showOnlyIssues,
                            onToggleShowOnlyIssues = { showOnlyIssues = !showOnlyIssues },
                            onOpenZoomImage = { zoomDialogOpen = true },
                            onUpdateMeta = onUpdateClassSheetMeta,
                            onUpdateScope = onUpdateClassSheetScope,
                            onUpdateSheetKind = onUpdateClassSheetKind,
                            onUpdateSharedWorkingDays = onUpdateClassSheetSharedWorkingDays,
                            onToggleRowSelected = onToggleScannedRowSelected,
                            onSetAllSelected = onSetAllScannedRowsSelected,
                            onUpdateRowField = onUpdateScannedRowField,
                            onAddBlankRow = onAddBlankScannedRow,
                            onRemoveRow = { rowId -> pendingRemoveScannedRowId = rowId },
                            onMarkAllVerified = onMarkAllScannedIssuesVerified,
                            onDismissPreview = onDismissClassSheetPreview,
                            onRequestCommit = { unreviewed ->
                                if (unreviewed > 0) {
                                    unreviewedPendingCount = unreviewed
                                    askUnreviewedConfirm = true
                                } else {
                                    onCommitClassSheet()
                                }
                            }
                        )
                    }

                    // Single-Student Extracted Preview
                    state.analysisResult?.let { ext ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = TealSoft),
                            border = BorderStroke(1.dp, TealContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                                    Text("শনাক্তকৃত তথ্য (প্রিভিউ)", fontWeight = FontWeight.Bold, color = TealDark)
                                }
                                Text(ext.summary, fontSize = 12.5.sp)

                                if (ext.uncertainNotes.isNotEmpty()) {
                                    Surface(
                                        color = AmberBg,
                                        border = BorderStroke(1.dp, AmberBorder),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text("অস্পষ্ট / যাচাইযোগ্য নোট:", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = AmberNotice)
                                            ext.uncertainNotes.forEach { note ->
                                                Text("• $note", fontSize = 11.5.sp, color = SlateText)
                                            }
                                        }
                                    }
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, SlateOutline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            "শিক্ষার্থী: ${ext.student.name ?: "অজানা"} · শ্রেণি: ${ext.student.cls?.let { classNameBn(it) } ?: "—"} · রোল: ${ext.student.roll?.let { bn(it.toString()) } ?: "—"}",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            "প্রান্তিক: ${termOrdinalBn(ext.term)} · নম্বর পাওয়া গেছে: ${bn(ext.marks.size.toString())}টি",
                                            fontSize = 12.sp,
                                            color = SlateMuted
                                        )
                                        ext.marks.forEach { item ->
                                            Text("• ${item.subjectLabel}: ${fmt(item.value)}", fontSize = 12.sp)
                                        }
                                        ext.attendanceAndTraits.remark?.let { r ->
                                            Text("মতামত: $r", fontSize = 12.sp, color = SlateMuted)
                                        }
                                    }
                                }

                                Button(
                                    onClick = { onApplyExtracted(ext) },
                                    modifier = Modifier.fillMaxWidth().testTag("ai_apply_extracted_btn"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(" তালিকায় যুক্ত / হালনাগাদ করুন", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (zoomDialogOpen && state.selectedImageBitmap != null) {
        ZoomablePhotoDialog(
            bitmap = state.selectedImageBitmap,
            onDismiss = { zoomDialogOpen = false }
        )
    }

    if (cropDialogOpen && state.selectedImageBitmap != null) {
        ImageCropDialog(
            bitmap = state.selectedImageBitmap,
            canRestoreOriginal = state.hasOriginalImageBackup,
            onRotate90 = onRotateImage,
            onRestoreOriginal = onRestoreOriginalImage,
            onApplyCrop = { l, t, r, b ->
                cropDialogOpen = false
                onCropImage(l, t, r, b)
            },
            onDismiss = { cropDialogOpen = false }
        )
    }

    if (askUnreviewedConfirm) {
        AlertDialog(
            onDismissRequest = { askUnreviewedConfirm = false },
            title = { Text("অস্পষ্ট ঘর এখনো যাচাই করা হয়নি", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Text(
                    "এখনো ${bn(unreviewedPendingCount.toString())}টি অস্পষ্ট বা হিসাব-অমিল ঘর ম্যানুয়ালি যাচাই করা হয়নি। আগে দেখে ঠিক করবেন, নাকি বর্তমান মানসহ সংরক্ষণ করবেন?",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        askUnreviewedConfirm = false
                        onCommitClassSheet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) { Text("তবুও সংরক্ষণ করুন") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = {
                        askUnreviewedConfirm = false
                        showOnlyIssues = true
                    }) { Text("অস্পষ্টগুলো দেখুন") }
                    TextButton(onClick = { askUnreviewedConfirm = false }) { Text("বাতিল") }
                }
            }
        )
    }

    if (askClearChat) {
        BiometricDeleteConfirmDialog(
            title = "চ্যাট কথোপকথন মুছবেন?",
            description = "বর্তমান চ্যাট ইতিহাস মুছে নতুন কথোপকথন শুরু করা হবে।",
            warningDescription = "চ্যাট ইতিহাস মুছে ফেলতে আপনার ফিঙ্গারপ্রিন্ট অনুমতি দিন।",
            confirmActionLabel = "ফিঙ্গারপ্রিন্ট অনুমতি দিয়ে মুছুন",
            confirmButtonTestTag = "ai_confirm_clear_chat_btn",
            onDismiss = { askClearChat = false },
            onConfirmedDelete = onClearChat
        )
    }

    if (showModelPicker) {
        AiModelPickerDialog(
            activeModelName = activeModelName,
            isGeminiConfigured = isGeminiConfigured,
            availableModels = availableModels,
            customModels = customModels,
            newModelIds = newModelIds,
            refreshingModels = refreshingModels,
            modelsError = modelsError,
            onSelectModel = { chosen ->
                onSelectModel(chosen)
                showModelPicker = false
            },
            onRefreshModels = onRefreshModels,
            onRemoveCustomModel = { modelId -> pendingRemoveModelId = modelId },
            onOpenAiSettings = {
                showModelPicker = false
                onOpenAiSettings()
            },
            onDismiss = { showModelPicker = false }
        )
    }

    if (askClearImage) {
        BiometricDeleteConfirmDialog(
            title = "নির্বাচিত ছবি মুছবেন?",
            description = "নম্বরপত্রের ছবি ও বিশ্লেষিত প্রিভিউ মুছে ফেলা হবে।",
            warningDescription = "নির্বাচিত নম্বরপত্রের ছবি ও প্রিভিউ মুছতে আপনার ফিঙ্গারপ্রিন্ট অনুমতি দিন।",
            confirmActionLabel = "ফিঙ্গারপ্রিন্ট অনুমতি দিয়ে মুছুন",
            confirmButtonTestTag = "ai_confirm_clear_photo_btn",
            onDismiss = { askClearImage = false },
            onConfirmedDelete = {
                resetToAuto()
                onClearImage()
            }
        )
    }

    pendingRemoveScannedRowId?.let { rowId ->
        BiometricDeleteConfirmDialog(
            title = "স্ক্যান করা সারি মুছবেন?",
            description = "নম্বরপত্রের প্রিভিউ থেকে এই শিক্ষার্থীর সারিটি মুছে ফেলা হবে।",
            warningDescription = "স্ক্যান করা সারিটি মুছে ফেলতে আপনার ফিঙ্গারপ্রিন্ট অনুমতি দিন।",
            confirmActionLabel = "ফিঙ্গারপ্রিন্ট অনুমতি দিয়ে মুছুন",
            confirmButtonTestTag = "ai_confirm_remove_row_btn",
            onDismiss = { pendingRemoveScannedRowId = null },
            onConfirmedDelete = { onRemoveScannedRow(rowId) }
        )
    }

    pendingRemoveModelId?.let { modelId ->
        BiometricDeleteConfirmDialog(
            title = "কাস্টম মডেল মুছবেন?",
            description = "\"$modelId\" মডেলটি কাস্টম তালিকা থেকে মুছে ফেলা হবে।",
            warningDescription = "\"$modelId\" মডেলটি মুছতে আপনার ফিঙ্গারপ্রিন্ট অনুমতি দিন।",
            confirmActionLabel = "ফিঙ্গারপ্রিন্ট অনুমতি দিয়ে মুছুন",
            confirmButtonTestTag = "ai_confirm_remove_model_btn",
            onDismiss = { pendingRemoveModelId = null },
            onConfirmedDelete = { onRemoveCustomModel(modelId) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiModelPickerDialog(
    activeModelName: String,
    isGeminiConfigured: Boolean,
    availableModels: List<DiscoveredGeminiModel>,
    customModels: List<String>,
    newModelIds: Set<String>,
    refreshingModels: Boolean,
    modelsError: String?,
    onSelectModel: (String) -> Unit,
    onRefreshModels: () -> Unit,
    onRemoveCustomModel: (String) -> Unit,
    onOpenAiSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    var customInput by remember(activeModelName) { mutableStateOf(activeModelName) }
    var searchQuery by remember { mutableStateOf("") }
    val normalizedCurrent = GeminiApiConfig.normalizeModelId(activeModelName)
    val suggestedIds = remember { GeminiApiConfig.SUGGESTED_MODEL_IDS }

    val extraLiveModels = remember(availableModels, newModelIds) {
        availableModels
            .filter { it.id !in suggestedIds }
            .sortedWith(compareByDescending<DiscoveredGeminiModel> { it.id in newModelIds })
    }
    val filteredLiveModels = remember(extraLiveModels, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) extraLiveModels
        else extraLiveModels.filter {
            it.id.lowercase().contains(q) || it.displayName.lowercase().contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Gemini মডেল পরিবর্তন", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "সক্রিয়: $normalizedCurrent",
                        fontSize = 11.sp,
                        color = TealPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isGeminiConfigured) {
                    IconButton(
                        onClick = onRefreshModels,
                        enabled = !refreshingModels,
                        modifier = Modifier.size(40.dp).testTag("ai_picker_refresh_btn")
                    ) {
                        if (refreshingModels) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.Refresh, contentDescription = "মডেল তালিকা রিফ্রেশ", tint = TealPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!modelsError.isNullOrBlank()) {
                    Text(modelsError, fontSize = 11.sp, color = RoseDanger)
                }

                Text("প্রস্তাবিত মডেল", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SlateText)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    GeminiApiConfig.SUGGESTED_MODELS.forEach { preset ->
                        val selected = normalizedCurrent == preset.id
                        Surface(
                            color = if (selected) TealContainer.copy(alpha = 0.7f) else SlateBg,
                            border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) TealPrimary else SlateOutline),
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectModel(preset.id) }
                                .testTag("ai_picker_preset_${preset.id}")
                        ) {
                            Row(
                                Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = { onSelectModel(preset.id) },
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(preset.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SlateText)
                                        Surface(
                                            color = if (selected) TealPrimary else MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(5.dp)
                                        ) {
                                            Text(
                                                preset.badge,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (selected) Color.White else SlateMuted,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        "${preset.id} · ${preset.description}",
                                        fontSize = 10.5.sp,
                                        color = SlateMuted,
                                        lineHeight = 13.5.sp
                                    )
                                }
                            }
                        }
                    }
                }

                if (extraLiveModels.isNotEmpty()) {
                    HorizontalDivider(color = SlateOutline)
                    Text(
                        "API থেকে পাওয়া মডেল (${bn(extraLiveModels.size.toString())}টি)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = SlateText
                    )
                    if (extraLiveModels.size > 5) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("মডেল খুঁজুন...", fontSize = 11.5.sp) },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 12.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        filteredLiveModels.take(18).forEach { item ->
                            val selected = normalizedCurrent == item.id
                            val isNew = item.id in newModelIds
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectModel(item.id) },
                                colors = appFilterChipColors(),
                                border = appFilterChipBorder(selected),
                                label = {
                                    Text(
                                        text = if (isNew) "নতুন · ${item.id}" else item.id,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (selected || isNew) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                }

                if (customModels.isNotEmpty()) {
                    HorizontalDivider(color = SlateOutline)
                    Text("কাস্টম মডেল", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SlateText)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        customModels.forEach { customId ->
                            val selected = normalizedCurrent == customId
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectModel(customId) },
                                colors = appFilterChipColors(),
                                border = appFilterChipBorder(selected),
                                label = { Text(customId, fontSize = 10.5.sp) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "$customId সরান",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { onRemoveCustomModel(customId) }
                                    )
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = SlateOutline)
                Text("অন্য কোনো মডেল আইডি লিখুন", fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, color = SlateText)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customInput,
                        onValueChange = { customInput = it },
                        placeholder = { Text(GeminiApiConfig.DEFAULT_MODEL, fontSize = 11.5.sp) },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 12.sp),
                        modifier = Modifier.weight(1f).testTag("ai_picker_custom_input")
                    )
                    Button(
                        onClick = { onSelectModel(customInput) },
                        enabled = customInput.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.testTag("ai_picker_apply_custom_btn")
                    ) {
                        Text("চালু", fontSize = 11.5.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("বন্ধ করুন") }
        },
        dismissButton = {
            TextButton(onClick = onOpenAiSettings, modifier = Modifier.testTag("ai_picker_open_settings_btn")) {
                Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("API Key সেটিংস", fontSize = 12.sp)
            }
        }
    )
}


@Composable
private fun ClassSheetPreviewSection(
    sheet: ExtractedClassSheet,
    existingStudents: List<Student>,
    permissions: TeacherPermissions,
    hasImage: Boolean,
    saving: Boolean,
    showOnlyIssues: Boolean,
    onToggleShowOnlyIssues: () -> Unit,
    onOpenZoomImage: () -> Unit,
    onUpdateMeta: (cls: Int, term: Int, subKey: String) -> Unit,
    onUpdateScope: (ClassSheetImportScope) -> Unit,
    onUpdateSheetKind: (ClassSheetKind) -> Unit,
    onUpdateSharedWorkingDays: (Int, String) -> Unit,
    onToggleRowSelected: (rowId: String) -> Unit,
    onSetAllSelected: (Boolean) -> Unit,
    onUpdateRowField: (rowId: String, rollText: String?, name: String?, markEntry: Pair<String, String>?, verifyField: String?) -> Unit,
    onAddBlankRow: () -> Unit,
    onRemoveRow: (rowId: String) -> Unit,
    onMarkAllVerified: () -> Unit,
    onDismissPreview: () -> Unit,
    onRequestCommit: (unreviewedCount: Int) -> Unit
) {
    val subList = remember(sheet.cls) { subjectsFor(sheet.cls, RELIGIONS.first()) }
    val activeSub = subList.find { it.key == sheet.subKey } ?: subList.first()

    val rollDuplicates = remember(sheet.rows) {
        sheet.rows.filter { it.selected }
            .mapNotNull { fromBn(it.rollText.trim()).toIntOrNull() }
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys
    }

    val reportsByRowId = remember(
        sheet.rows,
        sheet.cls,
        sheet.subKey,
        sheet.scope,
        sheet.sheetKind,
        sheet.sharedWorkingDaysByTerm,
        rollDuplicates,
        existingStudents
    ) {
        sheet.rows.associate { r ->
            val rInt = fromBn(r.rollText.trim()).toIntOrNull()
            val hasDbName = rInt != null && existingStudents.any {
                it.cls == sheet.cls && it.roll == rInt && it.name.isNotBlank()
            }
            r.id to validateScannedStudentRow(
                row = r,
                cls = sheet.cls,
                subKey = sheet.subKey,
                duplicateRolls = rollDuplicates,
                hasExistingDbName = hasDbName,
                scope = sheet.scope,
                sheetKind = sheet.sheetKind,
                sharedWorkingDaysByTerm = sheet.sharedWorkingDaysByTerm
            )
        }
    }

    val selectedRows = sheet.rows.filter { it.selected }
    val hardErrorCount = selectedRows.count { reportsByRowId[it.id]?.hasHardError == true }
    val issueRows = sheet.rows.filter { reportsByRowId[it.id]?.hasAnyIssue == true }
    val unreviewedIssueRows = selectedRows.count { rep ->
        val r = reportsByRowId[rep.id]
        r != null && (r.uncertainWarnings.isNotEmpty() || r.arithmeticWarning != null)
    }

    val visibleRows = if (showOnlyIssues) issueRows else sheet.rows

    var clsDrop by remember { mutableStateOf(false) }
    var termDrop by remember { mutableStateOf(false) }
    var subDrop by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, TealPrimary.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        if (sheet.isSample) "নমুনা প্রিভিউ (সংরক্ষণের আগে যাচাই)"
                        else "স্ক্যান প্রিভিউ (সংরক্ষণের আগে যাচাই ও সংশোধন)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TealDark
                    )
                    Text(sheet.summary, fontSize = 11.5.sp, color = SlateMuted)
                }
                if (hasImage) {
                    OutlinedButton(
                        onClick = onOpenZoomImage,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Rounded.ZoomIn, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text(" মূল ছবি", fontSize = 11.sp)
                    }
                }
            }

            // Sheet Format Switcher (১ প্রান্তিক / ৩ প্রান্তিক / উপস্থিতি)
            Surface(
                color = SlateSurfaceVariant,
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    listOf(
                        ClassSheetKind.SINGLE_TERM_MARKS to "১ প্রান্তিক নম্বর",
                        ClassSheetKind.THREE_TERM_MARKS to "৩ প্রান্তিক নম্বর",
                        ClassSheetKind.ATTENDANCE_TRAITS to "উপস্থিতি ও গুণাবলি"
                    ).forEach { (kindOpt, label) ->
                        val selectedKind = sheet.sheetKind == kindOpt
                        Surface(
                            color = if (selectedKind) TealPrimary else Color.Transparent,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onUpdateSheetKind(kindOpt) }
                                .testTag("preview_kind_${kindOpt.name.lowercase()}_btn")
                        ) {
                            Text(
                                label,
                                fontSize = 10.sp,
                                fontWeight = if (selectedKind) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedKind) Color.White else SlateText,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Target Class / Term / Subject Override Row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { clsDrop = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("${classNameBn(sheet.cls)} শ্রেণি", fontSize = 11.5.sp, maxLines = 1, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = clsDrop, onDismissRequest = { clsDrop = false }) {
                        permissions.effectiveClasses().forEach { c ->
                            val isClsSel = sheet.cls == c
                            DropdownMenuItem(
                                modifier = Modifier.background(if (isClsSel) TealContainer else Color.Transparent),
                                text = {
                                    Text(
                                        "${classNameBn(c)} শ্রেণি",
                                        fontWeight = if (isClsSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isClsSel) TealDark else SlateText
                                    )
                                },
                                trailingIcon = if (isClsSel) {
                                    { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealDark, modifier = Modifier.size(16.dp)) }
                                } else null,
                                onClick = {
                                    val newSubs = subjectsFor(c, RELIGIONS.first())
                                    val nextSub = if (newSubs.any { it.key == sheet.subKey }) sheet.subKey else newSubs.first().key
                                    onUpdateMeta(c, sheet.term, nextSub)
                                    clsDrop = false
                                }
                            )
                        }
                    }
                }
                if (sheet.sheetKind == ClassSheetKind.SINGLE_TERM_MARKS) {
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { termDrop = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(termNameBn(sheet.term), fontSize = 11.5.sp, maxLines = 1, modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(expanded = termDrop, onDismissRequest = { termDrop = false }) {
                            (1..3).forEach { t ->
                                val isTermSel = sheet.term == t
                                DropdownMenuItem(
                                    modifier = Modifier.background(if (isTermSel) TealContainer else Color.Transparent),
                                    text = {
                                        Text(
                                            termNameBn(t),
                                            fontWeight = if (isTermSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isTermSel) TealDark else SlateText
                                        )
                                    },
                                    trailingIcon = if (isTermSel) {
                                        { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealDark, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    onClick = { onUpdateMeta(sheet.cls, t, sheet.subKey); termDrop = false }
                                )
                            }
                        }
                    }
                }
                if (sheet.sheetKind != ClassSheetKind.ATTENDANCE_TRAITS) {
                    Box(Modifier.weight(1.2f)) {
                        OutlinedButton(
                            onClick = { subDrop = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(activeSub.bn, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(expanded = subDrop, onDismissRequest = { subDrop = false }) {
                            subList.forEach { s ->
                                val isSubSel = sheet.subKey == s.key
                                DropdownMenuItem(
                                    modifier = Modifier.background(if (isSubSel) TealContainer else Color.Transparent),
                                    text = {
                                        Text(
                                            s.bn,
                                            fontWeight = if (isSubSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSubSel) TealDark else SlateText
                                        )
                                    },
                                    trailingIcon = if (isSubSel) {
                                        { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealDark, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    onClick = { onUpdateMeta(sheet.cls, sheet.term, s.key); subDrop = false }
                                )
                            }
                        }
                    }
                }
            }

            if (sheet.sheetKind == ClassSheetKind.ATTENDANCE_TRAITS) {
                Surface(
                    color = SlateSurfaceVariant,
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "মোট কার্যদিবস (সবার জন্য প্রযোজ্য হলে লিখুন)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TealDark
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..3).forEach { t ->
                                EditableCellField(
                                    value = sheet.sharedWorkingDaysByTerm[t] ?: "",
                                    onValueChange = { onUpdateSharedWorkingDays(t, it) },
                                    label = "${termOrdinalBn(t)} কার্যদিবস",
                                    confidence = if ((sheet.sharedWorkingDaysByTerm[t] ?: "").isNotBlank()) FieldConfidence.SURE else FieldConfidence.NEUTRAL,
                                    hardError = null,
                                    uncertainWarning = null,
                                    deducedHint = null,
                                    onVerify = {},
                                    isNumeric = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            if (sheet.sheetKind == ClassSheetKind.SINGLE_TERM_MARKS && activeSub.written > 0) {
                Surface(
                    color = SlateSurfaceVariant,
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        listOf(
                            ClassSheetImportScope.BOTH to "ধারাবাহিক + সামষ্টিক",
                            ClassSheetImportScope.CONT_ONLY to "শুধু ধারাবাহিক",
                            ClassSheetImportScope.WR_ONLY to "শুধু সামষ্টিক"
                        ).forEach { (scOpt, scLabel) ->
                            val selectedSc = sheet.scope == scOpt
                            Surface(
                                color = if (selectedSc) TealPrimary else Color.Transparent,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(if (scOpt == ClassSheetImportScope.BOTH) 1.25f else 1f)
                                    .clickable { onUpdateScope(scOpt) }
                            ) {
                                Text(
                                    scLabel,
                                    fontSize = 10.sp,
                                    fontWeight = if (selectedSc) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedSc) Color.White else SlateText,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (sheet.warnings.isNotEmpty()) {
                Surface(
                    color = AmberBg,
                    border = BorderStroke(1.dp, AmberBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        sheet.warnings.forEach { w ->
                            Text("• $w", fontSize = 11.sp, color = AmberNotice)
                        }
                    }
                }
            }

            // Issue & Filter Summary Bar
            Surface(
                color = if (issueRows.isNotEmpty()) RoseBg else Color(0xFFECFDF5),
                border = BorderStroke(1.dp, if (issueRows.isNotEmpty()) RoseDanger.copy(alpha = 0.4f) else EmeraldGrade.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            if (issueRows.isNotEmpty()) Icons.Rounded.WarningAmber else Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = if (issueRows.isNotEmpty()) RoseDanger else EmeraldGrade,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            if (issueRows.isNotEmpty()) {
                                "মোট ${bn(sheet.rows.size.toString())} জনের মধ্যে ${bn(issueRows.size.toString())}টি সারিতে সন্দেহযুক্ত/অমিল ঘর রয়েছে — লাল ঘরগুলো যাচাই করুন"
                            } else {
                                "সব ${bn(sheet.rows.size.toString())}টি সারির তথ্য ও হিসাব সম্পূর্ণ নিশ্চিত"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (issueRows.isNotEmpty()) RoseDanger else EmeraldGrade
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(EmeraldGrade))
                            Text("সবুজ = শিউর (নিশ্চিত)", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = EmeraldGrade)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(RoseDanger))
                            Text("লাল = সন্দেহযুক্ত / অমিল", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = RoseDanger)
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        FilterChip(
                            selected = !showOnlyIssues,
                            onClick = { if (showOnlyIssues) onToggleShowOnlyIssues() },
                            colors = appFilterChipColors(),
                            border = appFilterChipBorder(!showOnlyIssues),
                            modifier = Modifier.height(30.dp),
                            label = {
                                Text(
                                    "সবগুলো (${bn(sheet.rows.size.toString())})",
                                    fontSize = 10.5.sp,
                                    fontWeight = if (!showOnlyIssues) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                        if (issueRows.isNotEmpty()) {
                            FilterChip(
                                selected = showOnlyIssues,
                                onClick = { if (!showOnlyIssues) onToggleShowOnlyIssues() },
                                colors = appFilterChipColors(),
                                border = appFilterChipBorder(showOnlyIssues),
                                modifier = Modifier.height(30.dp).testTag("preview_filter_issues_chip"),
                                label = {
                                    Text(
                                        "অস্পষ্ট/সমস্যা (${bn(issueRows.size.toString())})",
                                        fontSize = 10.5.sp,
                                        fontWeight = if (showOnlyIssues) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                        AssistChip(
                            onClick = { onSetAllSelected(selectedRows.size < sheet.rows.size) },
                            modifier = Modifier.height(30.dp),
                            label = {
                                Text(
                                    if (selectedRows.size < sheet.rows.size) "সব নির্বাচন" else "সব বাতিল",
                                    fontSize = 10.5.sp
                                )
                            }
                        )
                        AssistChip(
                            onClick = onAddBlankRow,
                            modifier = Modifier.height(30.dp),
                            leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(13.dp)) },
                            label = { Text("সারি যোগ", fontSize = 10.5.sp) }
                        )
                        if (unreviewedIssueRows > 0) {
                            AssistChip(
                                onClick = onMarkAllVerified,
                                modifier = Modifier.height(30.dp),
                                leadingIcon = { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(13.dp)) },
                                label = { Text("সব ঠিক আছে", fontSize = 10.5.sp) }
                            )
                        }
                    }
                }
            }

            if (visibleRows.isEmpty()) {
                Text(
                    if (showOnlyIssues) "কোনো অস্পষ্ট বা সমস্যাযুক্ত সারি অবশিষ্ট নেই!" else "কোনো সারি পাওয়া যায়নি। '+ সারি যোগ' দিয়ে লিখতে পারেন।",
                    fontSize = 12.5.sp,
                    color = SlateMuted,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                val hasOtherTermTraitData = remember(sheet.rows, sheet.sharedWorkingDaysByTerm, sheet.term) {
                    sheet.term !in 1..3 ||
                        sheet.sharedWorkingDaysByTerm.any { (t, v) -> t != sheet.term && v.isNotBlank() } ||
                        sheet.rows.any { r ->
                            r.traitValues.any { (k, v) ->
                                v.isNotBlank() && (k.substringAfterLast('_', "").toIntOrNull() ?: sheet.term) != sheet.term
                            }
                        }
                }
                visibleRows.forEach { row ->
                    val report = reportsByRowId[row.id] ?: RowValidationReport(emptyMap(), emptyMap())
                    val rollInt = fromBn(row.rollText.trim()).toIntOrNull()
                    val matchedExisting = rollInt?.let { r -> existingStudents.find { it.cls == sheet.cls && it.roll == r } }
                    val isNewUnauthorized = matchedExisting == null && !permissions.canManageStudents

                    ScannedStudentPreviewCard(
                        row = row,
                        activeSub = activeSub,
                        activeTerm = sheet.term,
                        hasOtherTermTraitData = hasOtherTermTraitData,
                        scope = sheet.scope,
                        sheetKind = sheet.sheetKind,
                        report = report,
                        isExistingStudent = matchedExisting != null,
                        existingName = matchedExisting?.name,
                        isNewUnauthorized = isNewUnauthorized,
                        onToggleSelected = { onToggleRowSelected(row.id) },
                        onUpdateRoll = { onUpdateRowField(row.id, it, null, null, null) },
                        onUpdateName = { onUpdateRowField(row.id, null, it, null, null) },
                        onUpdateMark = { compKey, txt -> onUpdateRowField(row.id, null, null, compKey to txt, null) },
                        onVerifyField = { field -> onUpdateRowField(row.id, null, null, null, field) },
                        onRemoveRow = { onRemoveRow(row.id) }
                    )
                }
            }

            HorizontalDivider(color = SlateOutline)

            if (hardErrorCount > 0) {
                Text(
                    "লাল চিহ্নিত ${bn(hardErrorCount.toString())}টি সারির ত্রুটি ঠিক না করা পর্যন্ত সংরক্ষণ করা যাবে না।",
                    color = RoseDanger,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onDismissPreview,
                    enabled = !saving,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("বাতিল", fontSize = 12.sp)
                }
                Button(
                    onClick = { onRequestCommit(unreviewedIssueRows) },
                    enabled = selectedRows.isNotEmpty() && hardErrorCount == 0 && !saving,
                    modifier = Modifier.weight(1f).testTag("ai_commit_class_sheet_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(" সংরক্ষণ হচ্ছে...", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" নিশ্চিত করে সংরক্ষণ করুন (${bn(selectedRows.size.toString())} জন)", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScannedStudentPreviewCard(
    row: ScannedStudentRow,
    activeSub: com.kgps.result.Sub,
    activeTerm: Int,
    hasOtherTermTraitData: Boolean,
    scope: ClassSheetImportScope,
    sheetKind: ClassSheetKind,
    report: RowValidationReport,
    isExistingStudent: Boolean,
    existingName: String?,
    isNewUnauthorized: Boolean,
    onToggleSelected: () -> Unit,
    onUpdateRoll: (String) -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdateMark: (String, String) -> Unit,
    onVerifyField: (String) -> Unit,
    onRemoveRow: () -> Unit
) {
    val hasSuspect = report.hasAnyIssue || report.fieldConfidences.values.any { it == FieldConfidence.SUSPECT }
    val borderColor = when {
        !row.selected -> SlateOutline
        hasSuspect -> RoseDanger
        else -> EmeraldGrade.copy(alpha = 0.55f)
    }
    val cardBg = when {
        !row.selected -> SlateBg
        hasSuspect -> RoseBg.copy(alpha = 0.45f)
        else -> Color(0xFFF6FEF9)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(if (row.selected) 1.5.dp else 1.dp, borderColor),
        shape = RoundedCornerShape(9.dp)
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Header row: Checkbox + Badge + Delete
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Checkbox(
                    checked = row.selected,
                    onCheckedChange = { onToggleSelected() },
                    modifier = Modifier.size(20.dp)
                )
                val statusLabel = when {
                    isNewUnauthorized -> "নতুন (অনুমতি নেই)"
                    isExistingStudent -> "আপডেট: ${existingName?.take(18).orEmpty()}"
                    else -> "নতুন যোগ"
                }
                val statusColor = when {
                    isNewUnauthorized -> RoseDanger
                    isExistingStudent -> TealDark
                    else -> EmeraldGrade
                }
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Text(
                        statusLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                if (report.hasHardError) {
                    Text("ত্রুটি", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoseDanger)
                } else if (hasSuspect) {
                    Text("সন্দেহযুক্ত", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoseDanger)
                } else {
                    Text("সব নিশ্চিত", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldGrade)
                }
                IconButton(onClick = onRemoveRow, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "সারি মুছুন", tint = SlateMuted, modifier = Modifier.size(15.dp))
                }
            }

            // Roll + Name editable row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EditableCellField(
                    value = row.rollText,
                    onValueChange = onUpdateRoll,
                    label = "রোল",
                    confidence = report.fieldConfidences["roll"] ?: FieldConfidence.SURE,
                    hardError = report.hardErrors["roll"],
                    uncertainWarning = report.uncertainWarnings["roll"],
                    deducedHint = row.deducedFields["roll"],
                    onVerify = { onVerifyField("roll") },
                    isNumeric = true,
                    modifier = Modifier.width(64.dp)
                )
                val hasDbName = isExistingStudent && !existingName.isNullOrBlank()
                val useDbName = hasDbName && !row.useScannedNameForExisting
                val displayedName = if (useDbName) existingName!! else row.name
                val nameHint = when {
                    useDbName -> "অ্যাপ ডেটাবেসের নাম সুরক্ষিত"
                    else -> row.deducedFields["name"]
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    EditableCellField(
                        value = displayedName,
                        onValueChange = { if (!useDbName) onUpdateName(it) },
                        label = if (useDbName) "শিক্ষার্থীর নাম (ডেটাবেস)" else "শিক্ষার্থীর নাম",
                        confidence = if (useDbName) FieldConfidence.SURE else (report.fieldConfidences["name"] ?: FieldConfidence.SURE),
                        hardError = if (useDbName) null else report.hardErrors["name"],
                        uncertainWarning = if (useDbName) null else report.uncertainWarnings["name"],
                        deducedHint = nameHint,
                        onVerify = { onVerifyField("name") },
                        isNumeric = false,
                        centered = false,
                        readOnly = useDbName,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (hasDbName && row.name.isNotBlank() && row.name.trim() != existingName!!.trim()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                if (useDbName) "স্ক্যান পাঠ: ${row.name}" else "ডেটাবেস: $existingName",
                                fontSize = 9.5.sp,
                                color = SlateMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                if (useDbName) "স্ক্যান নাম নিন" else "ডেটাবেস নাম রাখুন",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TealPrimary,
                                modifier = Modifier.clickable { onVerifyField("toggle_use_scanned_name") }
                            )
                        }
                    }
                }
            }

            val isArt = activeSub.comps.size == 1 && activeSub.comps.first().key == "TOTAL" && activeSub.written == 0
            val maxCont = activeSub.comps.sumOf { it.max }
            var showRemarkField by remember(row.id) { mutableStateOf(false) }
            val hasRemarkContent = row.remark.isNotBlank() || report.uncertainWarnings["remark"] != null

            when (sheetKind) {
                ClassSheetKind.ATTENDANCE_TRAITS -> {
                    var showPerRowDaysAndRem by remember(row.id) { mutableStateOf(false) }
                    var showAllThreeTerms by remember(row.id, hasOtherTermTraitData, activeTerm) {
                        mutableStateOf(hasOtherTermTraitData || activeTerm !in 1..3)
                    }
                    val visibleTraitTerms = if (showAllThreeTerms) listOf(1, 2, 3) else listOf(activeTerm.coerceIn(1, 3))
                    val hasExtraTraitData = visibleTraitTerms.any { t ->
                        !row.traitValues["DAYS_$t"].isNullOrBlank() ||
                            !row.traitValues["REM_$t"].isNullOrBlank() ||
                            report.hardErrors["DAYS_$t"] != null ||
                            report.uncertainWarnings["DAYS_$t"] != null ||
                            report.uncertainWarnings["REM_$t"] != null
                    }
                    visibleTraitTerms.forEach { t ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Surface(
                                    color = TealContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        termOrdinalBn(t),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TealDark,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                                    )
                                }
                                val kPres = "PRES_$t"
                                val kAct = "ACT_$t"
                                val kDisc = "DISC_$t"
                                val kCond = "COND_$t"
                                EditableCellField(
                                    value = row.traitValues[kPres] ?: "",
                                    onValueChange = { onUpdateMark("TRAIT:$kPres", it) },
                                    label = "উপস্থিত",
                                    confidence = report.fieldConfidences[kPres] ?: FieldConfidence.NEUTRAL,
                                    hardError = report.hardErrors[kPres],
                                    uncertainWarning = report.uncertainWarnings[kPres],
                                    deducedHint = null,
                                    onVerify = { onVerifyField(kPres) },
                                    isNumeric = true,
                                    modifier = Modifier.weight(1.1f)
                                )
                                listOf(
                                    kAct to "সহপাঠ",
                                    kDisc to "শৃঙ্খলা",
                                    kCond to "আচরণ"
                                ).forEach { (k, lbl) ->
                                    EditableCellField(
                                        value = row.traitValues[k] ?: "",
                                        onValueChange = { onUpdateMark("TRAIT:$k", it) },
                                        label = lbl,
                                        confidence = report.fieldConfidences[k] ?: FieldConfidence.NEUTRAL,
                                        hardError = report.hardErrors[k],
                                        uncertainWarning = report.uncertainWarnings[k],
                                        deducedHint = null,
                                        onVerify = { onVerifyField(k) },
                                        isNumeric = false,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            if (showPerRowDaysAndRem || hasExtraTraitData) {
                                val kDays = "DAYS_$t"
                                val kRem = "REM_$t"
                                if (showPerRowDaysAndRem || !row.traitValues[kDays].isNullOrBlank() || !row.traitValues[kRem].isNullOrBlank() ||
                                    report.hardErrors[kDays] != null || report.uncertainWarnings[kDays] != null || report.uncertainWarnings[kRem] != null
                                ) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        EditableCellField(
                                            value = row.traitValues[kDays] ?: "",
                                            onValueChange = { onUpdateMark("TRAIT:$kDays", it) },
                                            label = "${termOrdinalBn(t)} কার্যদিবস",
                                            confidence = report.fieldConfidences[kDays] ?: FieldConfidence.NEUTRAL,
                                            hardError = report.hardErrors[kDays],
                                            uncertainWarning = report.uncertainWarnings[kDays],
                                            deducedHint = null,
                                            onVerify = { onVerifyField(kDays) },
                                            isNumeric = true,
                                            modifier = Modifier.weight(0.9f)
                                        )
                                        EditableCellField(
                                            value = row.traitValues[kRem] ?: "",
                                            onValueChange = { onUpdateMark("TRAIT:$kRem", it) },
                                            label = "${termOrdinalBn(t)} মন্তব্য",
                                            confidence = report.fieldConfidences[kRem] ?: FieldConfidence.NEUTRAL,
                                            hardError = report.hardErrors[kRem],
                                            uncertainWarning = report.uncertainWarnings[kRem],
                                            deducedHint = null,
                                            onVerify = { onVerifyField(kRem) },
                                            isNumeric = false,
                                            centered = false,
                                            modifier = Modifier.weight(1.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "গুণাবলি কোড: ক = উত্তম · খ = সন্তোষজনক · গ = উন্নতি প্রয়োজন",
                            fontSize = 10.sp,
                            color = SlateMuted,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (activeTerm in 1..3) {
                                Text(
                                    if (showAllThreeTerms) termNameBn(activeTerm) else "সব ৩ প্রান্তিক",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TealPrimary,
                                    modifier = Modifier.clickable { showAllThreeTerms = !showAllThreeTerms }
                                )
                            }
                            Text(
                                if (showPerRowDaysAndRem) "কম দেখুন" else "+ কার্যদিবস",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TealPrimary,
                                modifier = Modifier.clickable { showPerRowDaysAndRem = !showPerRowDaysAndRem }
                            )
                            Text(
                                if (showRemarkField || hasRemarkContent) "মন্তব্য" else "+ মন্তব্য",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TealPrimary,
                                modifier = Modifier.clickable { showRemarkField = !showRemarkField }
                            )
                        }
                    }
                }

                ClassSheetKind.THREE_TERM_MARKS -> {
                    (1..3).forEach { t ->
                        val kCont = "T${t}_CONT"
                        val kWr = "T${t}_WR"
                        val kTot = "T${t}_TOTAL"
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Surface(
                                color = TealContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    termOrdinalBn(t),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                                )
                            }
                            if (!isArt) {
                                EditableCellField(
                                    value = row.marksText[kCont] ?: "",
                                    onValueChange = { onUpdateMark(kCont, it) },
                                    label = "ধারাবাহিক (${bn(maxCont.toString())})",
                                    confidence = report.fieldConfidences[kCont] ?: FieldConfidence.NEUTRAL,
                                    hardError = report.hardErrors[kCont],
                                    uncertainWarning = report.uncertainWarnings[kCont],
                                    deducedHint = row.deducedFields[kCont],
                                    onVerify = { onVerifyField(kCont) },
                                    isNumeric = true,
                                    modifier = Modifier.weight(1f)
                                )
                                if (activeSub.written > 0) {
                                    EditableCellField(
                                        value = row.marksText[kWr] ?: "",
                                        onValueChange = { onUpdateMark(kWr, it) },
                                        label = "সামষ্টিক (${bn(activeSub.written.toString())})",
                                        confidence = report.fieldConfidences[kWr] ?: FieldConfidence.NEUTRAL,
                                        hardError = report.hardErrors[kWr],
                                        uncertainWarning = report.uncertainWarnings[kWr],
                                        deducedHint = row.deducedFields[kWr],
                                        onVerify = { onVerifyField(kWr) },
                                        isNumeric = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            EditableCellField(
                                value = row.marksText[kTot] ?: "",
                                onValueChange = { onUpdateMark(kTot, it) },
                                label = "মোট (${bn(activeSub.max.toString())})",
                                confidence = report.fieldConfidences[kTot] ?: FieldConfidence.NEUTRAL,
                                hardError = report.hardErrors[kTot],
                                uncertainWarning = report.uncertainWarnings[kTot],
                                deducedHint = row.deducedFields[kTot],
                                onVerify = { onVerifyField(kTot) },
                                isNumeric = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "খালি প্রান্তিকগুলোর আগের নম্বর অক্ষত থাকবে",
                            fontSize = 10.5.sp,
                            color = TealDark,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (!showRemarkField && !hasRemarkContent) {
                            Text(
                                "+ মন্তব্য",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TealPrimary,
                                modifier = Modifier.clickable { showRemarkField = true }
                            )
                        }
                    }
                }

                ClassSheetKind.SINGLE_TERM_MARKS -> {
                    val showContinuous = scope != ClassSheetImportScope.WR_ONLY
                    val showWritten = activeSub.written > 0 && scope != ClassSheetImportScope.CONT_ONLY
                    val showGrand = scope == ClassSheetImportScope.BOTH

                    var showBreakdownOverride by remember(row.id) { mutableStateOf(false) }
                    var showGrandPartsOverride by remember(row.id) { mutableStateOf(false) }
                    val hasBreakdownContent = showContinuous && activeSub.comps.any { c ->
                        !row.marksText[c.key].isNullOrBlank() ||
                            report.hardErrors[c.key] != null ||
                            report.uncertainWarnings[c.key] != null
                    }
                    val showPartsForGrandTotal = !report.isGrandTotalOnlyMode || showGrandPartsOverride
                    val showBreakdownRow = showContinuous && showPartsForGrandTotal && (!report.isSummaryOnlyMode || hasBreakdownContent || showBreakdownOverride)

                    if (isArt) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            EditableCellField(
                                value = row.marksText["TOTAL"] ?: "",
                                onValueChange = { onUpdateMark("TOTAL", it) },
                                label = "শিল্পকলা (${bn(activeSub.max.toString())})",
                                confidence = report.fieldConfidences["TOTAL"] ?: FieldConfidence.SURE,
                                hardError = report.hardErrors["TOTAL"],
                                uncertainWarning = report.uncertainWarnings["TOTAL"],
                                deducedHint = row.deducedFields["TOTAL"],
                                onVerify = { onVerifyField("TOTAL") },
                                isNumeric = true,
                                modifier = Modifier.weight(1f)
                            )
                            if (showGrand) {
                                EditableCellField(
                                    value = row.sheetGrandTotal ?: "",
                                    onValueChange = { onUpdateMark("GRAND", it) },
                                    label = "মোট (${bn(activeSub.max.toString())})",
                                    confidence = report.fieldConfidences["GRAND"] ?: FieldConfidence.SURE,
                                    hardError = report.hardErrors["GRAND"],
                                    uncertainWarning = report.uncertainWarnings["GRAND"],
                                    deducedHint = row.deducedFields["GRAND"],
                                    onVerify = { onVerifyField("GRAND") },
                                    isNumeric = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else if (report.isGrandTotalOnlyMode && !showGrandPartsOverride) {
                        // রোল-নাম-মোট (সহজ মোট) মোড: শুধু মোট ঘর দেখান
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            EditableCellField(
                                value = row.sheetGrandTotal ?: "",
                                onValueChange = { onUpdateMark("GRAND", it) },
                                label = "মোট নম্বর (${bn(activeSub.max.toString())})",
                                confidence = report.fieldConfidences["GRAND"] ?: FieldConfidence.SURE,
                                hardError = report.hardErrors["GRAND"],
                                uncertainWarning = report.uncertainWarnings["GRAND"],
                                deducedHint = row.deducedFields["GRAND"],
                                onVerify = { onVerifyField("GRAND") },
                                isNumeric = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        if (showBreakdownRow) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                activeSub.comps.forEach { c ->
                                    val shortLabel = when (c.key) {
                                        "TB" -> "পাঠ্য"
                                        "ACT" -> "সক্রিয়"
                                        "LANG" -> "ভাষা"
                                        "CT" -> "টেস্ট"
                                        else -> "মোট"
                                    }
                                    EditableCellField(
                                        value = row.marksText[c.key] ?: "",
                                        onValueChange = { onUpdateMark(c.key, it) },
                                        label = "$shortLabel (${bn(c.max.toString())})",
                                        confidence = report.fieldConfidences[c.key] ?: FieldConfidence.SURE,
                                        hardError = report.hardErrors[c.key],
                                        uncertainWarning = report.uncertainWarnings[c.key],
                                        deducedHint = row.deducedFields[c.key],
                                        onVerify = { onVerifyField(c.key) },
                                        isNumeric = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            if (showContinuous) {
                                EditableCellField(
                                    value = row.sheetContinuousTotal ?: "",
                                    onValueChange = { onUpdateMark("CONT", it) },
                                    label = "ধারা. (${bn(maxCont.toString())})",
                                    confidence = report.fieldConfidences["CONT"] ?: FieldConfidence.SURE,
                                    hardError = report.hardErrors["CONT"],
                                    uncertainWarning = report.uncertainWarnings["CONT"],
                                    deducedHint = row.deducedFields["CONT"],
                                    onVerify = { onVerifyField("CONT") },
                                    isNumeric = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (showWritten) {
                                EditableCellField(
                                    value = row.marksText["WR"] ?: "",
                                    onValueChange = { onUpdateMark("WR", it) },
                                    label = "সামষ্টিক (${bn(activeSub.written.toString())})",
                                    confidence = report.fieldConfidences["WR"] ?: FieldConfidence.SURE,
                                    hardError = report.hardErrors["WR"],
                                    uncertainWarning = report.uncertainWarnings["WR"],
                                    deducedHint = row.deducedFields["WR"],
                                    onVerify = { onVerifyField("WR") },
                                    isNumeric = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (showGrand) {
                                EditableCellField(
                                    value = row.sheetGrandTotal ?: "",
                                    onValueChange = { onUpdateMark("GRAND", it) },
                                    label = "মোট (${bn(activeSub.max.toString())})",
                                    confidence = report.fieldConfidences["GRAND"] ?: FieldConfidence.SURE,
                                    hardError = report.hardErrors["GRAND"],
                                    uncertainWarning = report.uncertainWarnings["GRAND"],
                                    deducedHint = row.deducedFields["GRAND"],
                                    onVerify = { onVerifyField("GRAND") },
                                    isNumeric = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val contStr = report.calculatedContinuous?.let { bn(fmtEn(it)) } ?: "—"
                        val grandStr = report.calculatedGrandTotal?.let { bn(fmtEn(it)) } ?: "—"
                        val wrStr = row.marksText["WR"]?.trim()?.takeIf { it.isNotEmpty() }?.let { bn(it) } ?: "—"
                        Text(
                            when {
                                report.isGrandTotalOnlyMode && !showGrandPartsOverride ->
                                    "সরাসরি মোট: $grandStr (সহজ মোট শিট)"
                                scope == ClassSheetImportScope.CONT_ONLY ->
                                    "শুধু ধারাবাহিক: $contStr (সামষ্টিক অক্ষত)"
                                scope == ClassSheetImportScope.WR_ONLY ->
                                    "শুধু সামষ্টিক: $wrStr (ধারাবাহিক অক্ষত)"
                                else -> buildString {
                                    if (activeSub.written > 0) append("হিসাব: ধারাবাহিক $contStr · মোট $grandStr")
                                    else append("হিসাব: মোট $contStr")
                                }
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (scope != ClassSheetImportScope.BOTH || report.isSummaryOnlyMode) TealDark else SlateText,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!isArt && report.isGrandTotalOnlyMode) {
                                Text(
                                    if (showGrandPartsOverride) "সহজ মোট" else "+ অংশ দিন",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TealPrimary,
                                    modifier = Modifier.clickable { showGrandPartsOverride = !showGrandPartsOverride }
                                )
                            } else if (!isArt && showContinuous && report.isSummaryOnlyMode && !hasBreakdownContent) {
                                Text(
                                    if (showBreakdownOverride) "সংক্ষিপ্ত করুন" else "+ ৪ অংশ",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TealPrimary,
                                    modifier = Modifier.clickable { showBreakdownOverride = !showBreakdownOverride }
                                )
                            }
                            if (!showRemarkField && !hasRemarkContent) {
                                Text(
                                    "+ মন্তব্য",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TealPrimary,
                                    modifier = Modifier.clickable { showRemarkField = true }
                                )
                            }
                        }
                    }
                }
            }

            if (showRemarkField || hasRemarkContent) {
                EditableCellField(
                    value = row.remark,
                    onValueChange = { onUpdateMark("REMARK", it) },
                    label = "মন্তব্য / টীকা",
                    confidence = report.fieldConfidences["remark"] ?: FieldConfidence.NEUTRAL,
                    hardError = report.hardErrors["remark"],
                    uncertainWarning = report.uncertainWarnings["remark"],
                    deducedHint = null,
                    onVerify = { onVerifyField("remark") },
                    isNumeric = false,
                    centered = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (row.rawNotes.isNotBlank()) {
                Text(
                    row.rawNotes,
                    fontSize = 10.5.sp,
                    color = SlateMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            report.arithmeticWarning?.let { arithMsg ->
                Surface(
                    color = RoseBg,
                    border = BorderStroke(1.dp, RoseDanger.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = RoseDanger, modifier = Modifier.size(15.dp))
                            Text(arithMsg, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = RoseDanger, modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = { onVerifyField("arithmetic") },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("ঠিক আছে", fontSize = 10.5.sp, color = RoseDanger)
                            }
                        }
                        if (report.quickFixes.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                report.quickFixes.forEach { fix ->
                                    Surface(
                                        color = Color.White,
                                        border = BorderStroke(1.dp, EmeraldGrade.copy(alpha = 0.6f)),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.clickable {
                                            onUpdateMark(fix.targetField, fix.newValue)
                                        }
                                    ) {
                                        Text(
                                            "✓ ${fix.labelBn}",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = EmeraldGrade,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditableCellField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    confidence: FieldConfidence,
    hardError: String?,
    uncertainWarning: String?,
    deducedHint: String?,
    onVerify: () -> Unit,
    isNumeric: Boolean,
    centered: Boolean = true,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isSuspect = hardError != null || uncertainWarning != null || confidence == FieldConfidence.SUSPECT
    val isSure = !isSuspect && confidence == FieldConfidence.SURE && value.isNotBlank()
    val msg = hardError ?: uncertainWarning
    val sureGreenBg = Color(0xFFECFDF5)
    val containerBg = when {
        isSuspect -> RoseBg
        isSure -> sureGreenBg
        else -> Color.White
    }
    val borderColor = when {
        isSuspect -> RoseDanger
        isSure -> EmeraldGrade
        else -> SlateOutline
    }
    val labelColor = when {
        isSuspect -> RoseDanger
        isSure -> EmeraldGrade
        else -> SlateMuted
    }
    val interactionSource = remember { MutableInteractionSource() }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = containerBg,
        unfocusedContainerColor = containerBg,
        focusedBorderColor = borderColor,
        unfocusedBorderColor = borderColor,
        errorContainerColor = RoseBg,
        errorBorderColor = RoseDanger,
        focusedLabelColor = labelColor,
        unfocusedLabelColor = labelColor,
        cursorColor = borderColor
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 13.sp,
                fontWeight = if (isSure || isSuspect) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSuspect) RoseDanger else SlateText,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isNumeric) KeyboardType.Decimal else KeyboardType.Text
            ),
            cursorBrush = SolidColor(borderColor),
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth()
        ) { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                isError = isSuspect,
                label = {
                    Text(
                        label,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = labelColor
                    )
                },
                colors = fieldColors,
                contentPadding = PaddingValues(horizontal = 5.dp, vertical = 6.dp),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
                        isError = isSuspect,
                        interactionSource = interactionSource,
                        colors = fieldColors,
                        shape = RoundedCornerShape(7.dp),
                        focusedBorderThickness = 1.5.dp,
                        unfocusedBorderThickness = if (isSuspect || isSure) 1.2.dp else 1.dp
                    )
                }
            )
        }
        if (msg != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "⚠ $msg",
                    fontSize = 9.5.sp,
                    color = RoseDanger,
                    modifier = Modifier.weight(1f),
                    lineHeight = 12.sp
                )
                if (hardError == null) {
                    Surface(
                        color = RoseDanger.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.clickable { onVerify() }
                    ) {
                        Text(
                            "✓ ঠিক",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoseDanger,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        } else if (!isSuspect && !deducedHint.isNullOrBlank()) {
            Text(
                "✓ $deducedHint",
                fontSize = 9.sp,
                color = EmeraldGrade,
                lineHeight = 11.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ZoomablePhotoDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color.Black,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize()) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "মূল নম্বরপত্র ছবি",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextScale = (scale * zoom).coerceIn(1f, 5f)
                                scale = nextScale
                                offset = if (nextScale == 1f) Offset.Zero else offset + pan
                            }
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "দুই আঙুলে জুম করে হাতের লেখা দেখুন",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (scale > 1.05f) {
                            TextButton(onClick = { scale = 1f; offset = Offset.Zero }) {
                                Text("রিসেট", color = Color.White)
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "বন্ধ করুন", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

