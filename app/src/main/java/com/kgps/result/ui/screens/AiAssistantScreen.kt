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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhotoLibrary
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.kgps.result.AiAssistantState
import com.kgps.result.CLSN
import com.kgps.result.CLS_SHORT
import com.kgps.result.RELIGIONS
import com.kgps.result.Student
import com.kgps.result.ai.AiModelMode
import com.kgps.result.ai.ExtractedClassSheet
import com.kgps.result.ai.ExtractedMarksheet
import com.kgps.result.ai.RowValidationReport
import com.kgps.result.ai.ScannedStudentRow
import com.kgps.result.ai.validateScannedStudentRow
import com.kgps.result.bn
import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.fmt
import com.kgps.result.fmtEn
import com.kgps.result.fromBn
import com.kgps.result.subjectsFor
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
    onOpenAiSettings: () -> Unit,
    onSetActiveTab: (Int) -> Unit,
    onSetModelMode: (AiModelMode) -> Unit,
    onSetContextStudent: (String?) -> Unit,
    onClearChat: () -> Unit,
    onSendChat: (String) -> Unit,
    onOpenCsvImport: () -> Unit,
    onPickMarksheetUri: (Uri) -> Unit,
    onClearImage: () -> Unit,
    onAnalyzeImage: (String) -> Unit,
    onApplyExtracted: (ExtractedMarksheet) -> Unit,
    onAnalyzeClassSheet: (clsHint: Int?, termHint: Int?, subKeyHint: String?, teacherNote: String) -> Unit,
    onLoadSampleClassPreview: (clsHint: Int?, termHint: Int?, subKeyHint: String?) -> Unit,
    onUpdateClassSheetMeta: (cls: Int, term: Int, subKey: String) -> Unit,
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

    // Scanner mode & selectors
    var scanMode by remember { mutableStateOf("class") } // "class" | "single"
    var clsHint by remember(selectedClass) { mutableStateOf<Int?>(selectedClass) }
    var termHint by remember { mutableStateOf<Int?>(1) }
    var subKeyHint by remember { mutableStateOf<String?>(null) } // null = auto-detect

    var zoomDialogOpen by remember { mutableStateOf(false) }
    var showOnlyIssues by remember { mutableStateOf(false) }
    var askUnreviewedConfirm by remember { mutableStateOf(false) }
    var unreviewedPendingCount by remember { mutableStateOf(0) }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

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
            if (!isGeminiConfigured) {
                Surface(
                    color = AmberBg,
                    border = BorderStroke(1.dp, AmberBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = "সতর্কীকরণ",
                            tint = AmberNotice,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Gemini API Key সেট করা নেই · সেটিংস থেকে আপনার কী যুক্ত করুন",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = AmberNotice,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onOpenAiSettings,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("ai_open_settings_btn")
                        ) {
                            Text("সেটিংস", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
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
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            "সক্রিয় Gemini মডেল: $activeModelName",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TealDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onOpenAiSettings,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.testTag("ai_change_model_btn")
                        ) {
                            Text("পরিবর্তন", fontSize = 11.sp)
                        }
                    }
                }
            }

            TabRow(selectedTabIndex = state.activeTab) {
                Tab(
                    selected = state.activeTab == 0,
                    onClick = { onSetActiveTab(0) },
                    modifier = Modifier.testTag("ai_tab_chat"),
                    text = { Text("শিক্ষক সহায়ক চ্যাট", fontSize = 13.sp) }
                )
                Tab(
                    selected = state.activeTab == 1,
                    onClick = { onSetActiveTab(1) },
                    modifier = Modifier.testTag("ai_tab_scanner"),
                    text = { Text("নম্বরপত্র ছবি স্ক্যান", fontSize = 13.sp) }
                )
            }

            if (state.activeTab == 0) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = state.modelMode == AiModelMode.GENERAL,
                            onClick = { onSetModelMode(AiModelMode.GENERAL) },
                            modifier = Modifier.testTag("ai_mode_general"),
                            label = { Text("বিস্তারিত", fontSize = 11.5.sp) }
                        )
                        FilterChip(
                            selected = state.modelMode == AiModelMode.FAST,
                            onClick = { onSetModelMode(AiModelMode.FAST) },
                            modifier = Modifier.testTag("ai_mode_fast"),
                            label = { Text("দ্রুত", fontSize = 11.5.sp) }
                        )
                        Spacer(Modifier.weight(1f))
                        OutlinedButton(
                            onClick = {
                                if (state.chatMessages.size > 1) {
                                    askClearChat = true
                                } else {
                                    onClearChat()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SlateOutline),
                            modifier = Modifier.testTag("ai_clear_chat_btn")
                        ) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(15.dp))
                            Text(" নতুন", fontSize = 11.sp)
                        }
                    }

                    Box {
                        OutlinedButton(
                            onClick = { contextDropdownOpen = true },
                            modifier = Modifier.fillMaxWidth().testTag("ai_context_student_btn"),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SlateOutline)
                        ) {
                            Text(
                                selectedStudent?.let {
                                    "প্রসঙ্গ: ${CLSN[it.cls - 1]} · রোল ${bn(it.roll.toString())} — ${it.name}"
                                } ?: "শিক্ষার্থী প্রসঙ্গ: সাধারণ (ঐচ্ছিক) ▾",
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(expanded = contextDropdownOpen, onDismissRequest = { contextDropdownOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("সাধারণ / কোনো নির্দিষ্ট শিক্ষার্থী নয়") },
                                onClick = { onSetContextStudent(null); contextDropdownOpen = false }
                            )
                            sortedStudents.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text("${CLSN[s.cls - 1]} · রোল ${bn(s.roll.toString())} — ${s.name.ifBlank { "(নাম নেই)" }}") },
                                    onClick = { onSetContextStudent(s.id); contextDropdownOpen = false }
                                )
                            }
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "প্রান্তিক মতামত খসড়া লিখুন" to "মতামত খসড়া",
                            "NCTB প্রাথমিক মূল্যায়ন নম্বর বিভাজন ব্যাখ্যা করুন" to "নম্বর বিভাজন",
                            "অভিভাবক সভার জন্য সংক্ষিপ্ত সারসংক্ষেপ তৈরি করুন" to "অভিভাবক সভা"
                        ).forEachIndexed { idx, (prompt, shortLabel) ->
                            AssistChip(
                                onClick = { onSendChat(prompt) },
                                modifier = Modifier.testTag("ai_quick_prompt_$idx"),
                                label = { Text(shortLabel, fontSize = 11.5.sp) }
                            )
                        }
                    }

                    LazyColumn(
                        Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.chatMessages, key = { it.id }) { msg ->
                            val isBot = !msg.isUser
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    color = if (msg.isUser) TealPrimary else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(12.dp),
                                    border = if (isBot) BorderStroke(1.dp, SlateOutline) else null,
                                    modifier = Modifier.widthIn(max = 560.dp)
                                ) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (isBot && !msg.modelLabel.isNullOrBlank()) {
                                            Text(
                                                msg.modelLabel,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TealPrimary
                                            )
                                        }
                                        Text(
                                            msg.text,
                                            fontSize = 13.sp,
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
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text("উত্তর তৈরি হচ্ছে...", fontSize = 12.sp, color = SlateMuted)
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
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(err, color = RoseDanger, fontSize = 12.sp, lineHeight = 16.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(
                                        onClick = onOpenAiSettings,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("ai_chat_err_settings_btn")
                                    ) {
                                        Text("মডেল পরিবর্তন করুন", fontSize = 11.5.sp)
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            placeholder = { Text("আপনার প্রশ্ন লিখুন...", fontSize = 13.sp) },
                            maxLines = 3,
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
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                            modifier = Modifier.testTag("ai_chat_send_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "পাঠান")
                        }
                    }
                }
            } else {
                // Scanner Tab (Class Marksheet Batch Scan + Review Preview + Single Student Scan)
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top Mode & CSV Shortcut Bar
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = scanMode == "class",
                            onClick = { scanMode = "class" },
                            label = { Text("শ্রেণি তালিকা (সব শিক্ষার্থী)", fontSize = 11.5.sp) },
                            modifier = Modifier.testTag("scan_mode_class_chip")
                        )
                        FilterChip(
                            selected = scanMode == "single",
                            onClick = { scanMode = "single" },
                            label = { Text("একক (১ জন)", fontSize = 11.5.sp) },
                            modifier = Modifier.testTag("scan_mode_single_chip")
                        )
                        Spacer(Modifier.weight(1f))
                        if (permissions.canManageStudents) {
                            OutlinedButton(
                                onClick = onOpenCsvImport,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, SlateOutline),
                                modifier = Modifier.testTag("scanner_open_csv_btn")
                            ) {
                                Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(15.dp))
                                Text(" CSV", fontSize = 11.5.sp)
                            }
                        }
                    }

                    // Capture / Pick Photo Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, SlateOutline),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                if (scanMode == "class") "খাতা বা নম্বরপত্রের ছবি তুলে পুরো শ্রেণির নম্বর প্রিভিউ করুন"
                                else "একজন শিক্ষার্থীর নম্বরপত্রের ছবি নির্বাচন করে তথ্য শনাক্ত করুন",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                "ডেটা সংরক্ষণের আগে প্রিভিউতে প্রতিটি ঘর যাচাই করতে পারবেন; অস্পষ্ট বা সন্দেহজনক লেখা স্বয়ংক্রিয়ভাবে আলাদা রঙে দেখাবে।",
                                fontSize = 11.5.sp,
                                color = SlateMuted
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = {
                                        val uri = createTempScanImageUri(context)
                                        pendingCameraUri = uri
                                        cameraLauncher.launch(uri)
                                    },
                                    modifier = Modifier.weight(1f).testTag("ai_camera_photo_btn"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(" ক্যামেরা", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {
                                        photoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.weight(1f).testTag("ai_pick_photo_btn"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(" গ্যালারি", fontSize = 12.sp)
                                }
                                if (state.selectedImageBitmap != null) {
                                    OutlinedButton(
                                        onClick = { askClearImage = true },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("ai_clear_photo_btn")
                                    ) {
                                        Icon(Icons.Rounded.Close, contentDescription = "ছবি সরান", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            state.selectedImageBitmap?.let { bmp ->
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { zoomDialogOpen = true }
                                ) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "নির্বাচিত নম্বরপত্র ছবি",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 210.dp)
                                            .background(SlateSurfaceVariant)
                                    )
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.62f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(6.dp)
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Rounded.ZoomIn, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            Text("জুম করে দেখুন", color = Color.White, fontSize = 10.5.sp)
                                        }
                                    }
                                }
                            }

                            if (scanMode == "class") {
                                ClassSheetHintSelectors(
                                    clsHint = clsHint,
                                    termHint = termHint,
                                    subKeyHint = subKeyHint,
                                    permissions = permissions,
                                    onChangeCls = { newCls ->
                                        clsHint = newCls
                                        if (newCls != null && subKeyHint != null) {
                                            val validKeys = subjectsFor(newCls, RELIGIONS.first()).map { it.key }
                                            if (subKeyHint !in validKeys) subKeyHint = null
                                        }
                                    },
                                    onChangeTerm = { termHint = it },
                                    onChangeSub = { subKeyHint = it }
                                )
                            }

                            OutlinedTextField(
                                value = teacherNote,
                                onValueChange = { teacherNote = it },
                                label = { Text("অতিরিক্ত নির্দেশনা (ঐচ্ছিক, যেমন: রোল ১-৩৬ গণিত)", fontSize = 11.5.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("ai_scanner_note_input")
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        showOnlyIssues = false
                                        if (scanMode == "class") {
                                            onAnalyzeClassSheet(clsHint, termHint, subKeyHint, teacherNote)
                                        } else {
                                            onAnalyzeImage(teacherNote)
                                        }
                                    },
                                    enabled = state.selectedImageBitmap != null && !state.analyzingImage,
                                    modifier = Modifier.weight(1f).testTag("ai_analyze_image_btn"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (state.analyzingImage) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Text(" স্ক্যান হচ্ছে...", fontSize = 12.sp)
                                    } else {
                                        Icon(Icons.Rounded.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text(
                                            if (scanMode == "class") " স্ক্যান ও প্রিভিউ দেখুন" else " নম্বরপত্র বিশ্লেষণ করুন",
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                if (scanMode == "class") {
                                    OutlinedButton(
                                        onClick = {
                                            showOnlyIssues = false
                                            onLoadSampleClassPreview(clsHint, termHint, subKeyHint)
                                        },
                                        enabled = !state.analyzingImage,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("ai_sample_preview_btn")
                                    ) {
                                        Text("নমুনা", fontSize = 11.5.sp)
                                    }
                                }
                            }
                        }
                    }

                    state.analysisError?.let { err ->
                        Surface(
                            color = RoseBg,
                            border = BorderStroke(1.dp, RoseDanger.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = RoseDanger, modifier = Modifier.size(18.dp))
                                    Text(err, color = RoseDanger, fontSize = 12.sp, lineHeight = 17.sp)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (state.selectedImageBitmap != null && !state.analyzingImage) {
                                        OutlinedButton(
                                            onClick = {
                                                showOnlyIssues = false
                                                if (scanMode == "class") {
                                                    onAnalyzeClassSheet(clsHint, termHint, subKeyHint, teacherNote)
                                                } else {
                                                    onAnalyzeImage(teacherNote)
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("ai_scan_retry_btn")
                                        ) {
                                            Text("আবার চেষ্টা করুন", fontSize = 11.5.sp)
                                        }
                                    }
                                    TextButton(
                                        onClick = onOpenAiSettings,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("ai_scan_err_settings_btn")
                                    ) {
                                        Text("মডেল পরিবর্তন করুন", fontSize = 11.5.sp)
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
                            onToggleRowSelected = onToggleScannedRowSelected,
                            onSetAllSelected = onSetAllScannedRowsSelected,
                            onUpdateRowField = onUpdateScannedRowField,
                            onAddBlankRow = onAddBlankScannedRow,
                            onRemoveRow = onRemoveScannedRow,
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
                                            "শিক্ষার্থী: ${ext.student.name ?: "অজানা"} · শ্রেণি: ${ext.student.cls?.let { CLSN[it - 1] } ?: "—"} · রোল: ${ext.student.roll?.let { bn(it.toString()) } ?: "—"}",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            "প্রান্তিক: ${bn(ext.term.toString())}ম · নম্বর পাওয়া গেছে: ${bn(ext.marks.size.toString())}টি",
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
        AlertDialog(
            onDismissRequest = { askClearChat = false },
            title = { Text("চ্যাট কথোপকথন মুছবেন?") },
            text = { Text("বর্তমান চ্যাট ইতিহাস মুছে নতুন কথোপকথন শুরু করা হবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        askClearChat = false
                        onClearChat()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                    modifier = Modifier.testTag("ai_confirm_clear_chat_btn")
                ) { Text("হ্যাঁ, মুছুন") }
            },
            dismissButton = {
                TextButton(onClick = { askClearChat = false }) { Text("বাতিল") }
            }
        )
    }

    if (askClearImage) {
        AlertDialog(
            onDismissRequest = { askClearImage = false },
            title = { Text("নির্বাচিত ছবি মুছবেন?") },
            text = { Text("নম্বরপত্রের ছবি ও বিশ্লেষিত প্রিভিউ মুছে ফেলা হবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        askClearImage = false
                        onClearImage()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                    modifier = Modifier.testTag("ai_confirm_clear_photo_btn")
                ) { Text("হ্যাঁ, মুছুন") }
            },
            dismissButton = {
                TextButton(onClick = { askClearImage = false }) { Text("বাতিল") }
            }
        )
    }
}

@Composable
private fun ClassSheetHintSelectors(
    clsHint: Int?,
    termHint: Int?,
    subKeyHint: String?,
    permissions: TeacherPermissions,
    onChangeCls: (Int?) -> Unit,
    onChangeTerm: (Int?) -> Unit,
    onChangeSub: (String?) -> Unit
) {
    var clsMenu by remember { mutableStateOf(false) }
    var termMenu by remember { mutableStateOf(false) }
    var subMenu by remember { mutableStateOf(false) }

    val allowedClasses = remember(permissions) { permissions.effectiveClasses() }
    val previewCls = clsHint ?: allowedClasses.firstOrNull() ?: 3
    val subs = remember(previewCls, permissions) {
        subjectsFor(previewCls, RELIGIONS.first()).filter { permissions.canEditSubject(it.key) }
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Class selector
        Box(Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { clsMenu = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    clsHint?.let { "${CLS_SHORT[it - 1]} শ্রেণি" } ?: "শ্রেণি: স্বয়ংক্রিয়",
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(expanded = clsMenu, onDismissRequest = { clsMenu = false }) {
                DropdownMenuItem(
                    text = { Text("স্বয়ংক্রিয় শনাক্ত") },
                    onClick = { onChangeCls(null); clsMenu = false }
                )
                allowedClasses.forEach { c ->
                    DropdownMenuItem(
                        text = { Text("${CLSN[c - 1]} শ্রেণি") },
                        onClick = { onChangeCls(c); clsMenu = false }
                    )
                }
            }
        }

        // Term selector
        Box(Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { termMenu = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    termHint?.let { "${bn(it.toString())} প্রান্তিক" } ?: "প্রান্তিক: স্বয়ংক্রিয়",
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(expanded = termMenu, onDismissRequest = { termMenu = false }) {
                DropdownMenuItem(
                    text = { Text("স্বয়ংক্রিয় শনাক্ত") },
                    onClick = { onChangeTerm(null); termMenu = false }
                )
                (1..3).forEach { t ->
                    DropdownMenuItem(
                        text = { Text("${bn(t.toString())} প্রান্তিক") },
                        onClick = { onChangeTerm(t); termMenu = false }
                    )
                }
            }
        }

        // Subject selector
        Box(Modifier.weight(1.2f)) {
            val activeSubName = subKeyHint?.let { k -> subs.find { it.key == k }?.bn ?: k }
            OutlinedButton(
                onClick = { subMenu = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    activeSubName ?: "বিষয়: স্বয়ংক্রিয়",
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(expanded = subMenu, onDismissRequest = { subMenu = false }) {
                DropdownMenuItem(
                    text = { Text("স্বয়ংক্রিয় শনাক্ত") },
                    onClick = { onChangeSub(null); subMenu = false }
                )
                subs.forEach { s ->
                    DropdownMenuItem(
                        text = { Text(s.bn) },
                        onClick = { onChangeSub(s.key); subMenu = false }
                    )
                }
            }
        }
    }
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

    val reportsByRowId = remember(sheet.rows, sheet.cls, sheet.subKey, rollDuplicates) {
        sheet.rows.associate { r ->
            r.id to validateScannedStudentRow(r, sheet.cls, sheet.subKey, rollDuplicates)
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
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (sheet.isSample) "নমুনা প্রিভিউ (সংরক্ষণের আগে যাচাই)" else "স্ক্যান প্রিভিউ (সংরক্ষণের আগে যাচাই ও সংশোধন)",
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

            // Target Class / Term / Subject Override Row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { clsDrop = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("${CLSN[sheet.cls - 1]} শ্রেণি", fontSize = 11.5.sp, maxLines = 1, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = clsDrop, onDismissRequest = { clsDrop = false }) {
                        permissions.effectiveClasses().forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${CLSN[c - 1]} শ্রেণি") },
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
                Box(Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { termDrop = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("${bn(sheet.term.toString())} প্রান্তিক", fontSize = 11.5.sp, maxLines = 1, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = termDrop, onDismissRequest = { termDrop = false }) {
                        (1..3).forEach { t ->
                            DropdownMenuItem(
                                text = { Text("${bn(t.toString())} প্রান্তিক") },
                                onClick = { onUpdateMeta(sheet.cls, t, sheet.subKey); termDrop = false }
                            )
                        }
                    }
                }
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
                            DropdownMenuItem(
                                text = { Text(s.bn) },
                                onClick = { onUpdateMeta(sheet.cls, sheet.term, s.key); subDrop = false }
                            )
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
                color = if (issueRows.isNotEmpty()) AmberBg else TealSoft,
                border = BorderStroke(1.dp, if (issueRows.isNotEmpty()) AmberBorder else TealContainer),
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
                            tint = if (issueRows.isNotEmpty()) AmberNotice else EmeraldGrade,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            if (issueRows.isNotEmpty()) {
                                "মোট ${bn(sheet.rows.size.toString())} জনের মধ্যে ${bn(issueRows.size.toString())}টি সারিতে অস্পষ্ট বা যাচাইযোগ্য লেখা রয়েছে — নিচে ঠিক করুন"
                            } else {
                                "সব ${bn(sheet.rows.size.toString())}টি সারি পরিষ্কার ও যাচাইকৃত"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (issueRows.isNotEmpty()) AmberNotice else TealDark
                        )
                    }

                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = !showOnlyIssues,
                            onClick = { if (showOnlyIssues) onToggleShowOnlyIssues() },
                            label = { Text("সবগুলো (${bn(sheet.rows.size.toString())})", fontSize = 11.sp) }
                        )
                        if (issueRows.isNotEmpty()) {
                            FilterChip(
                                selected = showOnlyIssues,
                                onClick = { if (!showOnlyIssues) onToggleShowOnlyIssues() },
                                modifier = Modifier.testTag("preview_filter_issues_chip"),
                                label = { Text("শুধু অস্পষ্ট/সমস্যা (${bn(issueRows.size.toString())})", fontSize = 11.sp) }
                            )
                        }
                        AssistChip(
                            onClick = { onSetAllSelected(selectedRows.size < sheet.rows.size) },
                            label = {
                                Text(
                                    if (selectedRows.size < sheet.rows.size) "সব নির্বাচন" else "সব বাতিল",
                                    fontSize = 11.sp
                                )
                            }
                        )
                        AssistChip(
                            onClick = onAddBlankRow,
                            leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text("সারি যোগ", fontSize = 11.sp) }
                        )
                        if (unreviewedIssueRows > 0) {
                            AssistChip(
                                onClick = onMarkAllVerified,
                                leadingIcon = { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                label = { Text("সব ঠিক আছে", fontSize = 11.sp) }
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
                visibleRows.forEach { row ->
                    val report = reportsByRowId[row.id] ?: RowValidationReport(emptyMap(), emptyMap())
                    val rollInt = fromBn(row.rollText.trim()).toIntOrNull()
                    val matchedExisting = rollInt?.let { r -> existingStudents.find { it.cls == sheet.cls && it.roll == r } }
                    val isNewUnauthorized = matchedExisting == null && !permissions.canManageStudents

                    ScannedStudentPreviewCard(
                        row = row,
                        activeSub = activeSub,
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

@Composable
private fun ScannedStudentPreviewCard(
    row: ScannedStudentRow,
    activeSub: com.kgps.result.Sub,
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
    val borderColor = when {
        !row.selected -> SlateOutline
        report.hasHardError -> RoseDanger
        report.hasAnyIssue -> AmberBorder
        else -> SlateOutline
    }
    val cardBg = when {
        !row.selected -> SlateBg
        report.hasHardError -> RoseBg
        report.hasAnyIssue -> AmberBg
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(if (report.hasAnyIssue && row.selected) 1.5.dp else 1.dp, borderColor),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header row: Checkbox + Badge + Delete
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Checkbox(
                    checked = row.selected,
                    onCheckedChange = { onToggleSelected() },
                    modifier = Modifier.size(22.dp)
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
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        statusLabel,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                if (report.hasHardError) {
                    Text("ত্রুটি", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = RoseDanger)
                } else if (report.hasAnyIssue) {
                    Text("অস্পষ্ট/যাচাই", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = AmberNotice)
                }
                IconButton(onClick = onRemoveRow, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "সারি মুছুন", tint = SlateMuted, modifier = Modifier.size(16.dp))
                }
            }

            // Roll + Name editable row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EditableCellField(
                    value = row.rollText,
                    onValueChange = onUpdateRoll,
                    label = "রোল",
                    hardError = report.hardErrors["roll"],
                    uncertainWarning = report.uncertainWarnings["roll"],
                    onVerify = { onVerifyField("roll") },
                    isNumeric = true,
                    modifier = Modifier.width(84.dp)
                )
                EditableCellField(
                    value = row.name,
                    onValueChange = onUpdateName,
                    label = "শিক্ষার্থীর নাম",
                    hardError = report.hardErrors["name"],
                    uncertainWarning = report.uncertainWarnings["name"],
                    onVerify = { onVerifyField("name") },
                    isNumeric = false,
                    modifier = Modifier.weight(1f)
                )
            }

            // Subject Components Grid (wrapped nicely in rows of 3)
            val compFields = remember(activeSub) {
                buildList {
                    activeSub.comps.forEach { c ->
                        val short = when (c.key) {
                            "TB" -> "পাঠ্যবই"
                            "ACT" -> "সক্রিয়তা"
                            "LANG" -> "ভাষা"
                            "CT" -> "ক্লাস টেস্ট"
                            else -> "মোট"
                        }
                        add(Triple(c.key, short, c.max))
                    }
                    if (activeSub.written > 0) {
                        add(Triple("WR", "সামষ্টিক", activeSub.written))
                    }
                }
            }

            compFields.chunked(3).forEach { chunk ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    chunk.forEach { (cKey, shortLabel, maxVal) ->
                        EditableCellField(
                            value = row.marksText[cKey] ?: "",
                            onValueChange = { onUpdateMark(cKey, it) },
                            label = "$shortLabel (${bn(maxVal.toString())})",
                            hardError = report.hardErrors[cKey],
                            uncertainWarning = report.uncertainWarnings[cKey],
                            onVerify = { onVerifyField(cKey) },
                            isNumeric = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - chunk.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            // Calculated Totals & Raw Notes Footer
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val contStr = report.calculatedContinuous?.let { bn(fmtEn(it)) } ?: "—"
                val grandStr = report.calculatedGrandTotal?.let { bn(fmtEn(it)) } ?: "—"
                Text(
                    if (activeSub.written > 0) "হিসাব: ধারাবাহিক $contStr · মোট $grandStr"
                    else "হিসাব: মোট $contStr",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = SlateText
                )
                if (row.rawNotes.isNotBlank()) {
                    Text(
                        row.rawNotes,
                        fontSize = 10.5.sp,
                        color = SlateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            report.arithmeticWarning?.let { arithMsg ->
                Surface(
                    color = Color.White,
                    border = BorderStroke(1.dp, AmberBorder),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = AmberNotice, modifier = Modifier.size(15.dp))
                        Text(arithMsg, fontSize = 11.sp, color = AmberNotice, modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = { onVerifyField("arithmetic") },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("ঠিক আছে", fontSize = 10.5.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableCellField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    hardError: String?,
    uncertainWarning: String?,
    onVerify: () -> Unit,
    isNumeric: Boolean,
    modifier: Modifier = Modifier
) {
    val hasError = hardError != null
    val hasWarn = !hasError && uncertainWarning != null
    val msg = hardError ?: uncertainWarning

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            singleLine = true,
            isError = hasError,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isNumeric) KeyboardType.Decimal else KeyboardType.Text
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = when {
                    hasError -> RoseBg
                    hasWarn -> Color(0xFFFFFBEB)
                    else -> Color.White
                },
                focusedBorderColor = when {
                    hasError -> RoseDanger
                    hasWarn -> AmberNotice
                    else -> TealPrimary
                },
                unfocusedBorderColor = when {
                    hasError -> RoseDanger
                    hasWarn -> AmberNotice
                    else -> SlateOutline
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (msg != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "⚠ $msg",
                    fontSize = 10.sp,
                    color = if (hasError) RoseDanger else AmberNotice,
                    modifier = Modifier.weight(1f),
                    lineHeight = 13.sp
                )
                if (hasWarn) {
                    Surface(
                        color = AmberNotice.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.clickable { onVerify() }
                    ) {
                        Text(
                            "✓ ঠিক",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberNotice,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }
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
