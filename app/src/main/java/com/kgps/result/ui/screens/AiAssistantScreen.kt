package com.kgps.result.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.AiAssistantState
import com.kgps.result.CLSN
import com.kgps.result.Student
import com.kgps.result.ai.AiModelMode
import com.kgps.result.ai.ExtractedMarksheet
import com.kgps.result.bn
import com.kgps.result.fmt
import com.kgps.result.ui.theme.AmberBg
import com.kgps.result.ui.theme.AmberBorder
import com.kgps.result.ui.theme.AmberNotice
import com.kgps.result.ui.theme.RoseBg
import com.kgps.result.ui.theme.RoseDanger
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.TealContainer
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft

@Composable
fun AiAssistantScreen(
    state: AiAssistantState,
    students: List<Student>,
    speakingId: String?,
    isPlaceholderAi: Boolean,
    onSetModelMode: (AiModelMode) -> Unit,
    onSetContextStudent: (String?) -> Unit,
    onClearChat: () -> Unit,
    onSendChat: (String) -> Unit,
    onPickMarksheetUri: (android.net.Uri) -> Unit,
    onClearImage: () -> Unit,
    onAnalyzeImage: (String) -> Unit,
    onApplyExtracted: (ExtractedMarksheet) -> Unit,
    onSpeakText: (String, String) -> Unit
) {
    var activeTab by remember { mutableStateOf(0) }
    var chatInput by remember { mutableStateOf("") }
    var teacherNote by remember { mutableStateOf("") }
    var contextDropdownOpen by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onPickMarksheetUri(uri)
    }

    val sortedStudents = remember(students) {
        students.sortedWith(compareBy({ it.cls }, { it.roll }))
    }
    val selectedStudent = sortedStudents.find { it.id == state.contextStudentId }

    Column(
        modifier = Modifier.fillMaxSize().imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.widthIn(max = 880.dp).fillMaxSize()) {
            if (isPlaceholderAi) {
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = "সতর্কীকরণ",
                            tint = AmberNotice,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Sample Mode · লাইভ Gemini AI-এর জন্য Firebase প্রয়োজন",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = AmberNotice,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            TabRow(selectedTabIndex = activeTab) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    modifier = Modifier.testTag("ai_tab_chat"),
                    text = { Text("শিক্ষক সহায়ক চ্যাট", fontSize = 13.sp) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    modifier = Modifier.testTag("ai_tab_scanner"),
                    text = { Text("নম্বরপত্র ছবি স্ক্যান", fontSize = 13.sp) }
                )
            }

            if (activeTab == 0) {
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
                            onClick = onClearChat,
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
                            val speechKey = "chat-${msg.id}"
                            val isSpeaking = speakingId == speechKey
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
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    msg.modelLabel,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TealPrimary
                                                )
                                                IconButton(
                                                    onClick = { onSpeakText(msg.text, speechKey) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isSpeaking) Icons.Rounded.Stop else Icons.Rounded.VolumeUp,
                                                        contentDescription = if (isSpeaking) "পাঠ থামান" else "উত্তর শুনুন",
                                                        tint = if (isSpeaking) RoseDanger else TealPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
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
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(err, color = RoseDanger, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
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
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, SlateOutline),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "নম্বরপত্র বা খাতার ছবি নির্বাচন করে ফলাফল তথ্য শনাক্ত করুন",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                "অ্যান্ড্রয়েড ফটো পিকারের মাধ্যমে কোনো স্টোরেজ পারমিশন ছাড়াই ছবি নির্বাচন করুন।",
                                fontSize = 12.sp,
                                color = SlateMuted
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
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
                                    Text(" গ্যালারি থেকে ছবি নিন", fontSize = 12.sp)
                                }
                                if (state.selectedImageBitmap != null) {
                                    OutlinedButton(
                                        onClick = onClearImage,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("ai_clear_photo_btn")
                                    ) {
                                        Icon(Icons.Rounded.Close, contentDescription = "ছবি সরান", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            state.selectedImageBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "নির্বাচিত নম্বরপত্র ছবি",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 220.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )

                                OutlinedTextField(
                                    value = teacherNote,
                                    onValueChange = { teacherNote = it },
                                    label = { Text("অতিরিক্ত নির্দেশনা (যেমন: শ্রেণি ৩, ২য় প্রান্তিক)", fontSize = 11.5.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("ai_scanner_note_input")
                                )

                                Button(
                                    onClick = { onAnalyzeImage(teacherNote) },
                                    enabled = !state.analyzingImage,
                                    modifier = Modifier.fillMaxWidth().testTag("ai_analyze_image_btn"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (state.analyzingImage) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Text(" বিশ্লেষণ হচ্ছে...", fontSize = 12.sp)
                                    } else {
                                        Icon(Icons.Rounded.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text(" নম্বরপত্র বিশ্লেষণ করুন", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    state.analysisError?.let { err ->
                        Surface(
                            color = RoseBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(err, color = RoseDanger, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                        }
                    }

                    state.analysisResult?.let { ext ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = TealSoft),
                            border = BorderStroke(1.dp, TealContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                                    Text("শনাক্তকৃত তথ্য", fontWeight = FontWeight.Bold, color = TealDark)
                                }
                                Text(ext.summary, fontSize = 12.5.sp)

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
}
