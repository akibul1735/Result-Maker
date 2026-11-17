@file:OptIn(ExperimentalMaterial3Api::class)

package com.kgps.result.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.CLSN
import com.kgps.result.DPE_LOGO_LIST
import com.kgps.result.PDF_FONT_LIST
import com.kgps.result.PdfGen
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.bn
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.TealPrimary

@Composable
fun ReportStudioScreen(
    students: List<Student>,
    settings: Settings,
    selectedId: String?,
    speakingId: String?,
    onSelectId: (String) -> Unit,
    onUpdateSettings: (Settings) -> Unit,
    onSpeakStudent: (Student) -> Unit,
    onPrintStudent: (Student) -> Unit,
    onExportPdf: (Student) -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    BackHandler { onBack() }

    val sorted = remember(students) {
        students.sortedWith(compareBy({ it.cls }, { it.roll }))
    }
    val activeIdx = remember(sorted, selectedId) {
        sorted.indexOfFirst { it.id == selectedId }.takeIf { it >= 0 } ?: 0
    }
    val active = sorted.getOrNull(activeIdx)

    var pageView by remember { mutableStateOf("both") }
    var showFoldGuide by remember { mutableStateOf(true) }
    var studentMenuOpen by remember { mutableStateOf(false) }
    var showCustomizeSheet by remember { mutableStateOf(false) }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    if (active == null) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("প্রিভিউ দেখানোর মতো কোনো শিক্ষার্থী নেই", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onBack, modifier = Modifier.testTag("studio_back_empty_btn")) {
                Text("তালিকায় ফিরে যান")
            }
        }
        return
    }

    val coverBmp = remember(active, settings, showFoldGuide) {
        PdfGen.renderCoverBitmap(ctx, active, settings, showFoldGuide = showFoldGuide, scale = 2.0f)
    }
    val marksBmp = remember(active, settings, showFoldGuide) {
        PdfGen.renderMarksBitmap(ctx, active, settings, showFoldGuide = showFoldGuide, scale = 2.0f)
    }
    val isSpeaking = speakingId == "student-${active.id}"

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            Modifier.widthIn(max = 920.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Compact Top Student Switcher & Page Mode Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (activeIdx > 0) onSelectId(sorted[activeIdx - 1].id)
                            },
                            enabled = activeIdx > 0,
                            modifier = Modifier.size(48.dp).testTag("studio_prev_student_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "আগের শিক্ষার্থী")
                        }

                        Box(Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { studentMenuOpen = true },
                                modifier = Modifier.fillMaxWidth().testTag("studio_student_selector"),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, SlateOutline)
                            ) {
                                Text(
                                    "${CLSN[active.cls - 1]} · রোল ${bn(active.roll.toString())} — ${active.name.ifBlank { "(নাম নেই)" }} ▾",
                                    fontSize = 12.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            DropdownMenu(expanded = studentMenuOpen, onDismissRequest = { studentMenuOpen = false }) {
                                sorted.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text("${CLSN[s.cls - 1]} · রোল ${bn(s.roll.toString())} — ${s.name.ifBlank { "(নাম নেই)" }}") },
                                        onClick = {
                                            onSelectId(s.id)
                                            studentMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                if (activeIdx < sorted.lastIndex) onSelectId(sorted[activeIdx + 1].id)
                            },
                            enabled = activeIdx < sorted.lastIndex,
                            modifier = Modifier.size(48.dp).testTag("studio_next_student_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "পরের শিক্ষার্থী")
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())
                        ) {
                            listOf(
                                "both" to "দুই পাতা",
                                "cover" to "পাতা-১ (কভার)",
                                "marks" to "পাতা-২ (নম্বর)"
                            ).forEach { (id, label) ->
                                FilterChip(
                                    selected = pageView == id,
                                    onClick = { pageView = id },
                                    modifier = Modifier.testTag("studio_page_chip_$id"),
                                    label = { Text(label, fontSize = 11.5.sp) }
                                )
                            }
                        }

                        if (zoomScale > 1.05f) {
                            IconButton(
                                onClick = {
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                },
                                modifier = Modifier.size(48.dp).testTag("studio_reset_zoom_btn")
                            ) {
                                Icon(Icons.Rounded.ZoomOutMap, contentDescription = "জুম রিসেট", tint = TealPrimary)
                            }
                        }
                    }
                }
            }

            // Central Pinch-to-Zoom & Pan Preview Surface
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .background(SlateBg, RoundedCornerShape(12.dp))
                    .pointerInput(pageView, active.id) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (zoomScale * zoom).coerceIn(1f, 3.8f)
                            zoomScale = newScale
                            panOffset = if (newScale <= 1.02f) {
                                Offset.Zero
                            } else {
                                Offset(panOffset.x + pan.x, panOffset.y + pan.y)
                            }
                        }
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .graphicsLayer {
                            scaleX = zoomScale
                            scaleY = zoomScale
                            translationX = panOffset.x
                            translationY = panOffset.y
                        },
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (pageView == "both" || pageView == "cover") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, SlateOutline),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(8.dp)) {
                                Text(
                                    "পাতা-১ (বাইরের পাশ): বাম = ব্যাক কভার · ডান = ফ্রন্ট কভার (দুই আঙুলে জুম করুন)",
                                    fontSize = 11.sp,
                                    color = SlateMuted
                                )
                                Spacer(Modifier.height(6.dp))
                                Image(
                                    bitmap = coverBmp.asImageBitmap(),
                                    contentDescription = "প্রতিবেদন কভার পাতা প্রিভিউ",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(PdfGen.W.toFloat() / PdfGen.H.toFloat())
                                )
                            }
                        }
                    }

                    if (pageView == "both" || pageView == "marks") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, SlateOutline),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(8.dp)) {
                                Text(
                                    "পাতা-২ (ভেতরের পাশ): ৩ প্রান্তিকের নম্বর, চূড়ান্ত গ্রেড, গুণাবলি ও উপস্থিতি",
                                    fontSize = 11.sp,
                                    color = SlateMuted
                                )
                                Spacer(Modifier.height(6.dp))
                                Image(
                                    bitmap = marksBmp.asImageBitmap(),
                                    contentDescription = "প্রতিবেদন নম্বর পাতা প্রিভিউ",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(PdfGen.W.toFloat() / PdfGen.H.toFloat())
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Compact Action Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showCustomizeSheet = true },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SlateOutline),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.testTag("studio_customize_btn")
                    ) {
                        Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" ফন্ট/লোগো", fontSize = 11.5.sp, maxLines = 1)
                    }
                    IconButton(
                        onClick = { onSpeakStudent(active) },
                        modifier = Modifier.size(48.dp).testTag("studio_speak_btn")
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Rounded.Stop else Icons.Rounded.VolumeUp,
                            contentDescription = if (isSpeaking) "পাঠ থামান" else "ফলাফল শুনুন",
                            tint = TealPrimary
                        )
                    }
                    OutlinedButton(
                        onClick = { onPrintStudent(active) },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SlateOutline),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f).testTag("studio_print_btn")
                    ) {
                        Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" প্রিন্ট", fontSize = 12.sp, maxLines = 1)
                    }
                    Button(
                        onClick = { onExportPdf(active) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1.1f).testTag("studio_download_pdf_btn")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" PDF", fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }
    }

    if (showCustomizeSheet) {
        ModalBottomSheet(onDismissRequest = { showCustomizeSheet = false }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("রিপোর্ট কার্ড ফন্ট ও লোগো সেটিংস", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                Text("অফিসিয়াল বাংলা ফন্ট (PDF)", fontSize = 12.sp, color = SlateMuted)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    PDF_FONT_LIST.forEach { font ->
                        FilterChip(
                            selected = settings.pdfFont == font.id,
                            onClick = { onUpdateSettings(settings.copy(pdfFont = font.id)) },
                            modifier = Modifier.testTag("studio_font_${font.id}"),
                            label = { Text(font.labelBn.substringBefore(" (")) }
                        )
                    }
                }

                Text("কভার লোগো (প্রাথমিক শিক্ষা অধিদপ্তর)", fontSize = 12.sp, color = SlateMuted)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    DPE_LOGO_LIST.forEach { logo ->
                        FilterChip(
                            selected = settings.dpeLogo == logo.id,
                            onClick = { onUpdateSettings(settings.copy(dpeLogo = logo.id)) },
                            modifier = Modifier.testTag("studio_logo_${logo.id}"),
                            label = { Text(logo.labelBn.substringBefore(" (")) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = showFoldGuide,
                        onCheckedChange = { showFoldGuide = it },
                        modifier = Modifier.testTag("studio_fold_guide_checkbox")
                    )
                    Text("মাঝখানের ভাঁজ-রেখা দেখান (A4 → A5)", fontSize = 13.sp)
                }
            }
        }
    }
}
