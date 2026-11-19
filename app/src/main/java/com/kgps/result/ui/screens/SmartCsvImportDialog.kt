package com.kgps.result.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kgps.result.CLS_SHORT
import com.kgps.result.RELIGIONS
import com.kgps.result.Repo
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.bn
import com.kgps.result.subjectsFor
import com.kgps.result.ui.theme.AmberBg
import com.kgps.result.ui.theme.AmberBorder
import com.kgps.result.ui.theme.AmberNotice
import com.kgps.result.ui.theme.EmeraldGrade
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateSurfaceVariant
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealContainer
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartCsvImportDialog(
    students: List<Student>,
    settings: Settings,
    defaultCls: Int,
    initialText: String,
    onDismiss: () -> Unit,
    onImported: (Repo.CsvImportSummary) -> Unit,
    onError: (String) -> Unit
) {
    val ctx = LocalContext.current
    var activeTab by remember(initialText) { mutableIntStateOf(0) } // 0 = Import CSV, 1 = AI Prompt Builder
    var rawText by remember(initialText) { mutableStateOf(initialText) }
    var fallbackCls by remember(defaultCls) { mutableIntStateOf(defaultCls) }

    // AI Prompt Builder Options
    var promptOptions by remember(defaultCls) {
        mutableStateOf(
            Repo.CsvPromptOptions(
                targetCls = defaultCls,
                includeGuardianMobile = true,
                includeAddress = false,
                includeExistingCustomFields = settings.customFields.isNotEmpty(),
                includeMarks = false,
                selectedTerms = setOf(1),
                selectedSubjects = emptySet(),
                includeAttendanceTraits = false,
                sourceKind = Repo.CsvSourceKind.IMAGE,
                extraInstruction = ""
            )
        )
    }
    var promptCopied by remember { mutableStateOf(false) }

    LaunchedEffect(promptCopied) {
        if (promptCopied) {
            delay(2_400L)
            promptCopied = false
        }
    }

    val generatedPrompt = remember(promptOptions, settings.customFields) {
        Repo.buildAiCsvPrompt(promptOptions, settings.customFields)
    }

    val parsedRows = remember(rawText) { if (rawText.isBlank()) emptyList() else Repo.parseCsv(rawText) }
    var columns by remember(parsedRows, settings.customFields) {
        mutableStateOf(Repo.detectCsvColumns(parsedRows, settings.customFields))
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                rawText = ctx.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    ?: ""
            } catch (e: Exception) {
                onError("ফাইল পড়া যায়নি: ${e.message ?: "ত্রুটি"}")
            }
        }
    }

    val summary = remember(parsedRows, columns, students, settings, fallbackCls) {
        if (parsedRows.size >= 2 && columns.isNotEmpty()) {
            Repo.applyMappedCsvImport(parsedRows, columns, students, settings, fallbackCls)
        } else null
    }
    val hasRollOrName = columns.any {
        it.targetType == "standard" && (it.targetKey == "roll" || it.targetKey == "name")
    }

    fun pasteFromClipboard() {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipText = cm?.primaryClip?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(ctx)
            ?.toString()
            .orEmpty()
        if (clipText.isBlank()) {
            onError("ক্লিপবোর্ডে কোনো টেক্সট পাওয়া যায়নি।")
        } else {
            rawText = Repo.cleanCsvText(clipText)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 20.dp)
                .widthIn(max = 580.dp)
                .heightIn(max = 720.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 10.dp
        ) {
            Column(Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TealSoft)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "স্মার্ট CSV / Excel ইমপোর্ট",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TealDark
                        )
                        Text(
                            "AI প্রম্পট তৈরি করুন বা ফরম্যাট করা CSV ইমপোর্ট করুন",
                            fontSize = 11.5.sp,
                            color = SlateMuted
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("csv_close_icon_btn")
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "বন্ধ করুন",
                            tint = SlateText
                        )
                    }
                }

                // 2-Tab Selector
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = TealPrimary
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        modifier = Modifier.testTag("csv_tab_import"),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(17.dp))
                                Text("CSV ইমপোর্ট", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        modifier = Modifier.testTag("csv_tab_prompt"),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(17.dp))
                                Text("AI প্রম্পট তৈরি", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }

                // Body
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (activeTab == 0) {
                        // Quick banner to AI Prompt Builder
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { activeTab = 1 }
                                .testTag("csv_go_prompt_banner"),
                            colors = CardDefaults.cardColors(containerColor = TealSoft),
                            border = BorderStroke(1.dp, TealContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "বাইরের AI দিয়ে CSV বানাতে চান?",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = TealDark
                                    )
                                    Text(
                                        "এখানে ক্লিক করে চাহিদামতো অপশন দিয়ে প্রম্পট তৈরি করুন ও কপি করুন",
                                        fontSize = 11.sp,
                                        color = SlateMuted
                                    )
                                }
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = "প্রম্পট তৈরি করুন",
                                    tint = TealPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // File Pick + Paste buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { filePicker.launch(arrayOf("*/*")) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("csv_pick_file_btn"),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("ফাইল নিন", fontSize = 12.sp, maxLines = 1)
                            }
                            FilledTonalButton(
                                onClick = { pasteFromClipboard() },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("csv_paste_clipboard_btn"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = TealContainer,
                                    contentColor = TealDark
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("ক্লিপবোর্ড পেস্ট", fontSize = 12.sp, maxLines = 1)
                            }
                        }

                        OutlinedTextField(
                            value = rawText,
                            onValueChange = { rawText = it },
                            label = { Text("অথবা AI/Excel থেকে পাওয়া CSV ডাটা এখানে পেস্ট করুন", fontSize = 11.5.sp) },
                            minLines = if (parsedRows.isEmpty()) 4 else 2,
                            maxLines = 5,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("csv_raw_input")
                        )

                        if (parsedRows.isNotEmpty()) {
                            val newCols = columns.count { it.isNew }
                            val matchedCols = columns.size - newCols
                            Text(
                                "মোট কলাম: ${bn(columns.size.toString())}টি · শনাক্ত: ${bn(matchedCols.toString())}টি · নতুন: ${bn(newCols.toString())}টি",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (newCols > 0) AmberNotice else TealPrimary
                            )

                            if (newCols > 0) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = {
                                            columns = columns.map {
                                                if (it.isNew) it.copy(targetType = "create_new", newLabel = it.header) else it
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("csv_create_all_new_btn"),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("নতুন সব তৈরি করুন", fontSize = 11.sp, maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            columns = columns.map {
                                                if (it.isNew) it.copy(targetType = "skip") else it
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("csv_skip_all_new_btn"),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("নতুনগুলো বাদ দিন", fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                            }

                            Text("ডিফল্ট শ্রেণি (CSV-তে না থাকলে):", fontSize = 11.5.sp, color = SlateMuted)
                            Row(
                                Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                (1..5).forEach { c ->
                                    FilterChip(
                                        selected = fallbackCls == c,
                                        onClick = { fallbackCls = c },
                                        label = { Text(CLS_SHORT[c - 1], fontSize = 11.5.sp) }
                                    )
                                }
                            }

                            columns.forEachIndexed { idx, col ->
                                var dropdownOpen by remember(col.colIndex) { mutableStateOf(false) }
                                val targetDesc = when (col.targetType) {
                                    "create_new" -> "+ নতুন ফিল্ড: ${col.newLabel}"
                                    "skip" -> "✕ বাদ দিন"
                                    "standard" -> Repo.STANDARD_FIELDS.find { it.first == col.targetKey }?.second ?: col.targetKey
                                    "mark" -> "নম্বর কলাম: ${col.targetKey}"
                                    "text" -> "উপস্থিতি/আচরণ: ${col.targetKey}"
                                    "custom" -> "কাস্টম: ${settings.customFields.find { it.key == col.targetKey }?.label ?: col.targetKey}"
                                    else -> col.targetKey
                                }

                                Card(
                                    Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = if (col.isNew) AmberBg else SlateBg),
                                    border = BorderStroke(1.dp, if (col.isNew) AmberBorder else SlateOutline),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "${bn((idx + 1).toString())}. ${col.header}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                if (col.isNew) "নতুন কলাম" else "মিলেছে",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (col.isNew) AmberNotice else EmeraldGrade
                                            )
                                        }
                                        if (col.samples.isNotEmpty()) {
                                            Text(
                                                "নমুনা: ${col.samples.joinToString(" · ")}",
                                                fontSize = 11.sp,
                                                color = SlateMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Box {
                                            OutlinedButton(
                                                onClick = { dropdownOpen = true },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text("$targetDesc ▾", fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            DropdownMenu(expanded = dropdownOpen, onDismissRequest = { dropdownOpen = false }) {
                                                DropdownMenuItem(
                                                    text = { Text("+ নতুন ফিল্ড তৈরি করুন (ডাটাবেসে যোগ হবে)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AmberNotice) },
                                                    onClick = {
                                                        columns = columns.map { if (it.colIndex == col.colIndex) it.copy(targetType = "create_new", newLabel = it.header) else it }
                                                        dropdownOpen = false
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("✕ বাদ দিন (ইমপোর্ট করবেন না)", fontSize = 12.sp, color = SlateMuted) },
                                                    onClick = {
                                                        columns = columns.map { if (it.colIndex == col.colIndex) it.copy(targetType = "skip") else it }
                                                        dropdownOpen = false
                                                    }
                                                )
                                                HorizontalDivider()
                                                Repo.STANDARD_FIELDS.forEach { (fKey, fLabel) ->
                                                    DropdownMenuItem(
                                                        text = { Text(fLabel, fontSize = 12.sp) },
                                                        onClick = {
                                                            columns = columns.map { if (it.colIndex == col.colIndex) it.copy(targetType = "standard", targetKey = fKey) else it }
                                                            dropdownOpen = false
                                                        }
                                                    )
                                                }
                                                if (settings.customFields.isNotEmpty()) {
                                                    HorizontalDivider()
                                                    settings.customFields.forEach { cf ->
                                                        DropdownMenuItem(
                                                            text = { Text("কাস্টম: ${cf.label}", fontSize = 12.sp) },
                                                            onClick = {
                                                                columns = columns.map { if (it.colIndex == col.colIndex) it.copy(targetType = "custom", targetKey = cf.key) else it }
                                                                dropdownOpen = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        if (col.targetType == "create_new") {
                                            OutlinedTextField(
                                                value = col.newLabel,
                                                onValueChange = { lbl ->
                                                    columns = columns.map { if (it.colIndex == col.colIndex) it.copy(newLabel = lbl) else it }
                                                },
                                                label = { Text("নতুন ডাটাবেস ফিল্ডের নাম", fontSize = 10.sp) },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }

                            if (summary != null) {
                                Text(
                                    "নতুন ${bn(summary.addedCount.toString())} জন · হালনাগাদ ${bn(summary.updatedCount.toString())} জন · নতুন ফিল্ড ${bn(summary.createdFields.size.toString())}টি",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealPrimary
                                )
                            }
                        }
                    } else {
                        // TAB 1: AI PROMPT GENERATOR FOR EXTERNAL AI CSV CREATION
                        Text(
                            "যেকোনো AI (ChatGPT, Gemini, Claude)-কে আপনার ফাইল বা ছবি দিয়ে নিখুঁত ফরম্যাটের CSV বানিয়ে নিতে নিচের অপশনগুলো বাছাই করুন:",
                            fontSize = 12.sp,
                            color = SlateMuted
                        )

                        // 1. Source Kind
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateBg),
                            border = BorderStroke(1.dp, SlateOutline),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "১. আপনি AI-কে কোন ধরনের ডেটা দেবেন?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = SlateText
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Repo.CsvSourceKind.entries.forEach { kind ->
                                        FilterChip(
                                            selected = promptOptions.sourceKind == kind,
                                            onClick = { promptOptions = promptOptions.copy(sourceKind = kind) },
                                            label = { Text(kind.labelBn, fontSize = 11.5.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = TealContainer,
                                                selectedLabelColor = TealDark
                                            )
                                        )
                                    }
                                }

                                HorizontalDivider(color = SlateOutline)

                                Text(
                                    "২. কোন শ্রেণির CSV তৈরি করবেন?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = SlateText
                                )
                                Row(
                                    Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = promptOptions.targetCls == null,
                                        onClick = { promptOptions = promptOptions.copy(targetCls = null) },
                                        label = { Text("সব / একাধিক", fontSize = 11.5.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = TealContainer,
                                            selectedLabelColor = TealDark
                                        )
                                    )
                                    (1..5).forEach { c ->
                                        FilterChip(
                                            selected = promptOptions.targetCls == c,
                                            onClick = {
                                                promptOptions = promptOptions.copy(
                                                    targetCls = c,
                                                    selectedSubjects = emptySet()
                                                )
                                            },
                                            label = { Text(CLS_SHORT[c - 1], fontSize = 11.5.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = TealContainer,
                                                selectedLabelColor = TealDark
                                            )
                                        )
                                    }
                                }

                                HorizontalDivider(color = SlateOutline)

                                Text(
                                    "৩. CSV-তে কোন কোন তথ্য থাকবে?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = SlateText
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    FilterChip(
                                        selected = promptOptions.includeGuardianMobile,
                                        onClick = {
                                            promptOptions = promptOptions.copy(
                                                includeGuardianMobile = !promptOptions.includeGuardianMobile
                                            )
                                        },
                                        label = { Text("অভিভাবক, মোবাইল ও ধর্ম", fontSize = 11.5.sp) }
                                    )
                                    FilterChip(
                                        selected = promptOptions.includeAddress,
                                        onClick = {
                                            promptOptions = promptOptions.copy(
                                                includeAddress = !promptOptions.includeAddress
                                            )
                                        },
                                        label = { Text("ঠিকানা (গ্রাম–জেলা)", fontSize = 11.5.sp) }
                                    )
                                    if (settings.customFields.isNotEmpty()) {
                                        FilterChip(
                                            selected = promptOptions.includeExistingCustomFields,
                                            onClick = {
                                                promptOptions = promptOptions.copy(
                                                    includeExistingCustomFields = !promptOptions.includeExistingCustomFields
                                                )
                                            },
                                            label = {
                                                Text(
                                                    "কাস্টম ফিল্ড (${bn(settings.customFields.size.toString())})",
                                                    fontSize = 11.5.sp
                                                )
                                            }
                                        )
                                    }
                                    FilterChip(
                                        selected = promptOptions.includeMarks,
                                        onClick = {
                                            promptOptions = promptOptions.copy(
                                                includeMarks = !promptOptions.includeMarks
                                            )
                                        },
                                        label = { Text("বিষয়ের নম্বর", fontSize = 11.5.sp) }
                                    )
                                    FilterChip(
                                        selected = promptOptions.includeAttendanceTraits,
                                        onClick = {
                                            promptOptions = promptOptions.copy(
                                                includeAttendanceTraits = !promptOptions.includeAttendanceTraits
                                            )
                                        },
                                        label = { Text("উপস্থিতি ও আচরণ", fontSize = 11.5.sp) }
                                    )
                                }

                                // Term & Subject sub-options when marks or attendance are enabled
                                AnimatedVisibility(visible = promptOptions.includeMarks || promptOptions.includeAttendanceTraits) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White, RoundedCornerShape(10.dp))
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "প্রান্তিক নির্বাচন করুন:",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.5.sp,
                                            color = TealDark
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            (1..3).forEach { t ->
                                                val selected = t in promptOptions.selectedTerms
                                                FilterChip(
                                                    selected = selected,
                                                    onClick = {
                                                        val next = if (selected && promptOptions.selectedTerms.size > 1) {
                                                            promptOptions.selectedTerms - t
                                                        } else {
                                                            promptOptions.selectedTerms + t
                                                        }
                                                        promptOptions = promptOptions.copy(selectedTerms = next)
                                                    },
                                                    label = { Text("${bn(t.toString())}ম প্রান্তিক", fontSize = 11.sp) }
                                                )
                                            }
                                        }

                                        if (promptOptions.includeMarks) {
                                            val availSubs = remember(promptOptions.targetCls) {
                                                if (promptOptions.targetCls == null) {
                                                    (subjectsFor(1, RELIGIONS.first()) + subjectsFor(3, RELIGIONS.first()))
                                                        .distinctBy { it.key }
                                                } else {
                                                    subjectsFor(promptOptions.targetCls!!, RELIGIONS.first())
                                                }
                                            }
                                            Text(
                                                "কোন কোন বিষয়ের নম্বর নেবেন? (কিছু না বাছলে সব বিষয় থাকবে):",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.5.sp,
                                                color = TealDark
                                            )
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                FilterChip(
                                                    selected = promptOptions.selectedSubjects.isEmpty(),
                                                    onClick = {
                                                        promptOptions = promptOptions.copy(selectedSubjects = emptySet())
                                                    },
                                                    label = { Text("সব বিষয়", fontSize = 11.sp) }
                                                )
                                                availSubs.forEach { sub ->
                                                    val shortName = sub.bn.substringBefore(" (")
                                                    val isSel = sub.key in promptOptions.selectedSubjects
                                                    FilterChip(
                                                        selected = isSel,
                                                        onClick = {
                                                            val next = if (isSel) {
                                                                promptOptions.selectedSubjects - sub.key
                                                            } else {
                                                                promptOptions.selectedSubjects + sub.key
                                                            }
                                                            promptOptions = promptOptions.copy(selectedSubjects = next)
                                                        },
                                                        label = { Text(shortName, fontSize = 11.sp) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = promptOptions.extraCustomColumnsRaw,
                                    onValueChange = {
                                        promptOptions = promptOptions.copy(extraCustomColumnsRaw = it)
                                    },
                                    label = { Text("অতিরিক্ত নতুন কলাম (ঐচ্ছিক, কমা দিয়ে লিখুন)", fontSize = 11.sp) },
                                    placeholder = { Text("যেমন: জন্ম নিবন্ধন, রক্তের গ্রুপ, উপবৃত্তি", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("csv_prompt_extra_cols_input")
                                )

                                OutlinedTextField(
                                    value = promptOptions.extraInstruction,
                                    onValueChange = {
                                        promptOptions = promptOptions.copy(extraInstruction = it)
                                    },
                                    label = { Text("AI-কে বাড়তি কোনো নির্দেশনা (ঐচ্ছিক)", fontSize = 11.sp) },
                                    placeholder = { Text("যেমন: রোল না থাকলে ১ থেকে বসাবে / শুধু ৫ম শ্রেণি নেবে", fontSize = 11.sp) },
                                    minLines = 1,
                                    maxLines = 3,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("csv_prompt_extra_note_input")
                                )
                            }
                        }

                        // 2. Live Generated Prompt Box
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
                            border = BorderStroke(1.dp, TealContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "তৈরি হওয়া প্রম্পট",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TealDark
                                    )
                                    Surface(
                                        color = TealContainer,
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text(
                                            "মোট ${bn(generatedPrompt.columns.size.toString())}টি কলাম",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TealDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 120.dp, max = 210.dp)
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = generatedPrompt.prompt,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = SlateText,
                                        modifier = Modifier.testTag("csv_generated_prompt_text")
                                    )
                                }

                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            cm?.setPrimaryClip(
                                                ClipData.newPlainText("Result Maker CSV Prompt", generatedPrompt.prompt)
                                            )
                                            promptCopied = true
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("csv_copy_prompt_btn"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            if (promptCopied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            if (promptCopied) "কপি হয়েছে ✓" else "প্রম্পট কপি করুন",
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            runCatching {
                                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_SUBJECT, "Result Maker CSV Prompt")
                                                    putExtra(Intent.EXTRA_TEXT, generatedPrompt.prompt)
                                                }
                                                ctx.startActivity(Intent.createChooser(sendIntent, "AI প্রম্পট শেয়ার করুন"))
                                            }.onFailure {
                                                onError("শেয়ার করার অ্যাপ পাওয়া যায়নি। প্রম্পট কপি করে ব্যবহার করুন।")
                                            }
                                        },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("csv_share_prompt_btn"),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Rounded.Share, contentDescription = "শেয়ার করুন", modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("শেয়ার", fontSize = 12.sp)
                                    }
                                }

                                FilledTonalButton(
                                    onClick = { activeTab = 0 },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("csv_prompt_to_import_btn"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = AmberBg,
                                        contentColor = AmberNotice
                                    )
                                ) {
                                    Text(
                                        "AI থেকে আউটপুট CSV পেয়ে গেলে এখানে পেস্ট/ইমপোর্ট করুন",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = SlateOutline)

                // Footer Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("csv_cancel_btn")
                    ) {
                        Text("বাতিল")
                    }
                    if (activeTab == 0) {
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { if (summary != null) onImported(summary) },
                            enabled = summary != null && hasRollOrName && (summary.addedCount + summary.updatedCount > 0),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("csv_confirm_import_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("ইমপোর্ট করুন")
                        }
                    }
                }
            }
        }
    }
}
