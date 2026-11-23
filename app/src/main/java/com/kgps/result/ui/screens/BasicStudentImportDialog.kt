package com.kgps.result.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kgps.result.ClassResolver
import com.kgps.result.CsvImportAssistant
import com.kgps.result.CsvRowStatus
import com.kgps.result.Repo
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.activeSchoolClasses
import com.kgps.result.backup.XlsxImporter
import com.kgps.result.bn
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

private enum class BasicImportStep(val num: Int, val titleBn: String) {
    INPUT(1, "ফাইল / পেস্ট"),
    MAPPING(2, "ফিল্ড ও শ্রেণি"),
    PREVIEW(3, "প্রিভিউ ও সেভ")
}

private fun buildBasicStudentTemplateCsv(settings: Settings, sampleCls: Int): String {
    val stdHeaders = listOf("শ্রেণি", "রোল", "নাম", "পিতার নাম", "মোবাইল", "গ্রাম", "ইউনিয়ন", "ডাকঘর", "উপজেলা", "জেলা", "ধর্ম")
    val customHeaders = settings.customFields.map { it.label }
    val allHeaders = stdHeaders + customHeaders
    val sampleRow = listOf(
        sampleCls.toString(),
        "১",
        "নুসরাত জাহান মিম",
        "মোঃ শফিকুল ইসলাম",
        "01712345678",
        "বানা",
        "বানা",
        "বানা",
        "আলফাডাঙ্গা",
        "ফরিদপুর",
        "ইসলাম"
    ) + List(customHeaders.size) { "" }

    fun quoteCell(v: String): String {
        val needs = v.contains(',') || v.contains('"') || v.contains('\n')
        return if (needs) "\"${v.replace("\"", "\"\"")}\"" else v
    }
    return listOf(allHeaders, sampleRow).joinToString("\n") { row ->
        row.joinToString(",") { quoteCell(it) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BasicStudentImportDialog(
    students: List<Student>,
    settings: Settings,
    defaultCls: Int,
    initialText: String,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onImported: (Repo.CsvImportSummary) -> Unit,
    onError: (String) -> Unit
) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var rawText by remember(initialText) { mutableStateOf(initialText) }
    var fallbackCls by remember(defaultCls) { mutableStateOf(defaultCls.coerceIn(1, 20)) }

    val parsedSheet = remember(rawText) {
        if (rawText.isBlank()) Repo.CsvParsedSheet(emptyList(), null, null, null, false, 0)
        else Repo.parseSmartCsvSheet(rawText)
    }
    val parsedRows = parsedSheet.rows

    var currentStep by remember(initialText) {
        mutableStateOf(
            if (initialText.isNotBlank() && Repo.parseSmartCsvSheet(initialText).rows.size >= 2) {
                BasicImportStep.MAPPING
            } else {
                BasicImportStep.INPUT
            }
        )
    }

    LaunchedEffect(parsedSheet.detectedCls) {
        parsedSheet.detectedCls?.let { fallbackCls = it }
    }

    var columns by remember(parsedRows, settings.customFields) {
        mutableStateOf(
            Repo.detectCsvColumns(
                rows = parsedRows,
                existingCustomFields = settings.customFields,
                profileOnly = true
            )
        )
    }

    var classOverrides by remember(rawText, settings.customClasses) {
        mutableStateOf<Map<String, ClassResolver.ClassOverride>>(emptyMap())
    }

    val clsColIndices = remember(columns) {
        columns.filter { it.targetType == "standard" && it.targetKey == "cls" }.map { it.colIndex }
    }
    val rawClsValues = remember(parsedRows, clsColIndices) {
        if (parsedRows.size < 2 || clsColIndices.isEmpty()) emptyList()
        else parsedRows.drop(1).flatMap { r ->
            clsColIndices.mapNotNull { idx -> r.getOrNull(idx)?.trim()?.takeIf { it.isNotBlank() } }
        }
    }
    val detectedUnregisteredClasses = remember(rawClsValues, settings.customClasses) {
        ClassResolver.detectUnregisteredClasses(rawClsValues, settings.customClasses)
    }
    val excludedNonClasses = remember(rawClsValues, settings.customClasses, classOverrides) {
        ClassResolver.detectExcludedNonClasses(rawClsValues, settings.customClasses, classOverrides)
    }

    val validationReport = remember(
        parsedRows,
        columns,
        students,
        fallbackCls,
        settings.customClasses,
        settings.customFields,
        classOverrides
    ) {
        CsvImportAssistant.validateCsvData(
            parsedRows = parsedRows,
            columns = columns,
            existingStudents = students,
            fallbackCls = fallbackCls,
            sharedWorkingDaysByTerm = emptyMap(),
            customClasses = settings.customClasses,
            classOverrides = classOverrides,
            existingCustomFieldsCount = settings.customFields.size
        )
    }

    var selectedRowIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    LaunchedEffect(validationReport) {
        selectedRowIndices = validationReport.rows
            .filter { it.status != CsvRowStatus.ERROR }
            .map { it.dataRowIndex }
            .toSet()
    }
    var previewFilter by remember { mutableStateOf(CsvPreviewFilter.ALL) }

    val finalSummary = remember(
        parsedRows,
        columns,
        selectedRowIndices,
        students,
        settings,
        fallbackCls,
        classOverrides
    ) {
        CsvImportAssistant.buildFilteredImportSummary(
            parsedRows = parsedRows,
            columns = columns,
            selectedDataRowIndices = selectedRowIndices,
            existingStudents = students,
            existingSettings = settings,
            fallbackCls = fallbackCls,
            sharedWorkingDaysByTerm = emptyMap(),
            classOverrides = classOverrides
        )
    }

    var pendingTemplateContent by remember { mutableStateOf("") }
    val saveTemplateLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(("\uFEFF" + pendingTemplateContent).toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(ctx, "বেসিক তথ্য টেমপ্লেট সেভ হয়েছে", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                onError("ফাইল সেভ করা যায়নি: ${e.message ?: "ত্রুটি"}")
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            XlsxImporter.readSpreadsheetAsText(ctx.contentResolver, uri)
                .onSuccess { loaded ->
                    rawText = loaded
                    if (Repo.parseSmartCsvSheet(loaded).rows.size >= 2) {
                        currentStep = BasicImportStep.MAPPING
                    } else {
                        Toast.makeText(ctx, "ফাইল লোড হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                }
                .onFailure { e ->
                    onError("ফাইল পড়া যায়নি: ${e.message ?: "ত্রুটি"}")
                }
        }
    }

    BackHandler {
        when (currentStep) {
            BasicImportStep.INPUT -> onDismiss()
            BasicImportStep.MAPPING -> currentStep = BasicImportStep.INPUT
            BasicImportStep.PREVIEW -> currentStep = BasicImportStep.MAPPING
        }
    }

    val pendingNewFields = remember(columns, settings.customFields) {
        columns.filter { it.targetType == "create_new" }
    }
    val pendingSchemaAdditions = pendingNewFields.isNotEmpty() || validationReport.autoCreatedClasses.isNotEmpty()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 880.dp)
                .heightIn(min = 520.dp, max = 780.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header + 3-step Stepper
                Surface(color = TealSoft) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Rounded.Badge,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "পূর্ণাঙ্গ বেসিক তথ্য ইমপোর্ট",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.5.sp,
                                    color = TealDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("basic_import_close_btn")
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "বন্ধ করুন",
                                    tint = SlateText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BasicImportStep.entries.forEach { step ->
                                val active = currentStep == step
                                val done = step.num < currentStep.num ||
                                    (step == BasicImportStep.INPUT && parsedRows.size >= 2)
                                val bg = when {
                                    active -> TealPrimary
                                    done -> TealContainer
                                    else -> Color.White
                                }
                                val fg = when {
                                    active -> Color.White
                                    done -> TealDark
                                    else -> SlateMuted
                                }
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("basic_import_step_${step.num}")
                                        .clickable { currentStep = step },
                                    shape = RoundedCornerShape(10.dp),
                                    color = bg,
                                    border = BorderStroke(1.dp, if (active || done) TealPrimary.copy(alpha = 0.4f) else SlateOutline)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${bn(step.num.toString())}. ${step.titleBn}",
                                            fontSize = 11.5.sp,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                                            color = fg,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = SlateOutline)
                    }
                }

                // Body
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (currentStep) {
                        BasicImportStep.INPUT -> BasicImportStepInput(
                            rawText = rawText,
                            onRawTextChange = { rawText = it },
                            parsedRowCount = (parsedRows.size - 1).coerceAtLeast(0),
                            colCount = columns.size,
                            newFieldCount = pendingNewFields.size,
                            onPickFile = { filePicker.launch(arrayOf("*/*")) },
                            onPasteClipboard = {
                                val pasted = clipboard.getText()?.text.orEmpty()
                                if (pasted.isBlank()) {
                                    Toast.makeText(ctx, "ক্লিপবোর্ড ফাঁকা আছে", Toast.LENGTH_SHORT).show()
                                } else {
                                    rawText = pasted
                                    Toast.makeText(ctx, "ক্লিপবোর্ড থেকে পেস্ট হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onCopyTemplate = {
                                val tpl = buildBasicStudentTemplateCsv(settings, fallbackCls)
                                clipboard.setText(AnnotatedString(tpl))
                                Toast.makeText(ctx, "বেসিক তথ্য টেমপ্লেট কপি হয়েছে", Toast.LENGTH_SHORT).show()
                            },
                            onDownloadTemplate = {
                                pendingTemplateContent = buildBasicStudentTemplateCsv(settings, fallbackCls)
                                saveTemplateLauncher.launch("student_basic_template.csv")
                            }
                        )

                        BasicImportStep.MAPPING -> BasicImportStepMapping(
                            columns = columns,
                            onColumnsChange = { columns = it },
                            settings = settings,
                            fallbackCls = fallbackCls,
                            onFallbackClsChange = { fallbackCls = it },
                            detectedUnregisteredClasses = detectedUnregisteredClasses,
                            excludedNonClasses = excludedNonClasses,
                            classOverrides = classOverrides,
                            onClassOverridesChange = { classOverrides = it },
                            isAdmin = isAdmin
                        )

                        BasicImportStep.PREVIEW -> Column(modifier = Modifier.fillMaxSize()) {
                            if (pendingSchemaAdditions || !isAdmin) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, top = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!isAdmin && pendingSchemaAdditions) {
                                        Surface(
                                            color = RoseBg,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, RoseDanger.copy(alpha = 0.35f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = RoseDanger, modifier = Modifier.size(18.dp))
                                                Text(
                                                    "নতুন শ্রেণি বা কাস্টম ফিল্ড যোগ করতে বিদ্যালয়ের অ্যাডমিন অনুমতি প্রয়োজন। ধাপ ২ থেকে এগুলো বাদ দিন বা বিদ্যমান ফিল্ডে ম্যাপ করুন।",
                                                    fontSize = 11.5.sp,
                                                    color = RoseDanger,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                    if (pendingNewFields.isNotEmpty()) {
                                        Surface(
                                            color = AmberBg,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, AmberBorder)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    "অ্যাপের সাথে না মেলা ${bn(pendingNewFields.size.toString())}টি নতুন কাস্টম ফিল্ড তৈরি হয়ে ডেটা জমা হবে:",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AmberNotice
                                                )
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    pendingNewFields.forEach { cf ->
                                                        Surface(
                                                            color = Color.White,
                                                            shape = RoundedCornerShape(6.dp),
                                                            border = BorderStroke(1.dp, AmberBorder)
                                                        ) {
                                                            Text(
                                                                "+ ${cf.newLabel.ifBlank { cf.header }}",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = SlateText,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                CsvStepPreviewAndSave(
                                    report = validationReport,
                                    selectedRowIndices = selectedRowIndices,
                                    onToggleRow = { idx ->
                                        selectedRowIndices = if (selectedRowIndices.contains(idx)) {
                                            selectedRowIndices - idx
                                        } else {
                                            selectedRowIndices + idx
                                        }
                                    },
                                    onSelectAllValid = {
                                        selectedRowIndices = validationReport.rows
                                            .filter { it.status != CsvRowStatus.ERROR }
                                            .map { it.dataRowIndex }
                                            .toSet()
                                    },
                                    onClearSelection = { selectedRowIndices = emptySet() },
                                    filter = previewFilter,
                                    onFilterChange = { previewFilter = it },
                                    onJumpToInput = { currentStep = BasicImportStep.INPUT }
                                )
                            }
                        }
                    }
                }

                // Bottom action bar
                HorizontalDivider(color = SlateOutline)
                Surface(color = SlateBg) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStep == BasicImportStep.INPUT) {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("basic_import_cancel_btn")
                            ) {
                                Text("বাতিল", fontSize = 12.5.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    currentStep = when (currentStep) {
                                        BasicImportStep.MAPPING -> BasicImportStep.INPUT
                                        BasicImportStep.PREVIEW -> BasicImportStep.MAPPING
                                        BasicImportStep.INPUT -> BasicImportStep.INPUT
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("basic_import_prev_btn")
                            ) {
                                Text("← পূর্ববর্তী", fontSize = 12.5.sp)
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        when (currentStep) {
                            BasicImportStep.INPUT -> Button(
                                onClick = { currentStep = BasicImportStep.MAPPING },
                                enabled = parsedRows.size >= 2,
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("basic_import_to_mapping_btn")
                            ) {
                                Text("ফিল্ড ও শ্রেণি মিলান →", fontSize = 12.5.sp)
                            }

                            BasicImportStep.MAPPING -> Button(
                                onClick = { currentStep = BasicImportStep.PREVIEW },
                                enabled = parsedRows.size >= 2,
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("basic_import_to_preview_btn")
                            ) {
                                Text("প্রিভিউ ও যাচাই →", fontSize = 12.5.sp)
                            }

                            BasicImportStep.PREVIEW -> {
                                val selectedValidCount = finalSummary?.let { it.addedCount + it.updatedCount } ?: 0
                                val blockedByNonAdmin = !isAdmin && (
                                    (finalSummary?.createdFields?.isNotEmpty() == true) ||
                                        (finalSummary?.createdClasses?.isNotEmpty() == true)
                                    )
                                val canSave = finalSummary != null &&
                                    !validationReport.hasMissingRollOrNameMapping &&
                                    selectedValidCount > 0 &&
                                    !blockedByNonAdmin

                                Button(
                                    onClick = { if (finalSummary != null) onImported(finalSummary) },
                                    enabled = canSave,
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("basic_import_save_btn")
                                ) {
                                    Text(
                                        if (selectedValidCount > 0) {
                                            "চূড়ান্ত সংরক্ষণ (${bn(selectedValidCount.toString())} জন)"
                                        } else {
                                            "চূড়ান্ত সংরক্ষণ"
                                        },
                                        fontSize = 12.5.sp
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

@Composable
private fun BasicImportStepInput(
    rawText: String,
    onRawTextChange: (String) -> Unit,
    parsedRowCount: Int,
    colCount: Int,
    newFieldCount: Int,
    onPickFile: () -> Unit,
    onPasteClipboard: () -> Unit,
    onCopyTemplate: () -> Unit,
    onDownloadTemplate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = TealSoft,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = TealDark, modifier = Modifier.size(18.dp))
                Text(
                    "শিক্ষার্থীর পূর্ণাঙ্গ বেসিক তথ্য (.xlsx / .csv / এক্সেল বা টেবিল পেস্ট) ইমপোর্ট করুন। বিদ্যমান ফিল্ডের সাথে না মিললে প্রতিটি নতুন কলাম স্বয়ংক্রিয়ভাবে কাস্টম ফিল্ড হিসেবে সংরক্ষিত হবে এবং নতুন শ্রেণির নাম চিনে যোগ করা যাবে।",
                    fontSize = 11.5.sp,
                    color = TealDark
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onPickFile,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("basic_import_pick_file_btn")
            ) {
                Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Excel / CSV ফাইল", fontSize = 12.sp, maxLines = 1)
            }
            OutlinedButton(
                onClick = onPasteClipboard,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("basic_import_paste_btn")
            ) {
                Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("ক্লিপবোর্ড পেস্ট", fontSize = 12.sp, maxLines = 1)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCopyTemplate,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("basic_import_copy_tpl_btn")
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("বেসিক টেমপ্লেট কপি", fontSize = 11.5.sp, maxLines = 1)
            }
            OutlinedButton(
                onClick = onDownloadTemplate,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("basic_import_save_tpl_btn")
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("টেমপ্লেট .csv", fontSize = 11.5.sp, maxLines = 1)
            }
        }

        OutlinedTextField(
            value = rawText,
            onValueChange = onRawTextChange,
            label = { Text("শিক্ষার্থীর তথ্যের CSV / এক্সেল টেবিল পেস্ট করুন") },
            placeholder = {
                Text(
                    "শ্রেণি,রোল,নাম,আইডি,মাতার নাম,জন্মতারিখ,ডাকঘর,শিক্ষাবর্ষ\nপ্রাক-প্রাথমিক,২৬,মো. আলিফ মোল্যা,০২৬,মোসাঃ সেলিনা বেগম,০১/০১/২০১৯,বানা,২০২৬",
                    fontSize = 11.5.sp,
                    color = SlateMuted
                )
            },
            minLines = 7,
            maxLines = 12,
            textStyle = TextStyle(fontSize = 12.sp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("basic_import_text_input")
        )

        if (parsedRowCount > 0) {
            Surface(
                color = SlateBg,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SlateOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "শনাক্ত: ${bn(parsedRowCount.toString())} জন শিক্ষার্থী · ${bn(colCount.toString())}টি কলাম",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealDark
                    )
                    if (newFieldCount > 0) {
                        Surface(
                            color = AmberBg,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, AmberBorder)
                        ) {
                            Text(
                                "+${bn(newFieldCount.toString())} নতুন ফিল্ড",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberNotice,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BasicImportStepMapping(
    columns: List<Repo.CsvColMapping>,
    onColumnsChange: (List<Repo.CsvColMapping>) -> Unit,
    settings: Settings,
    fallbackCls: Int,
    onFallbackClsChange: (Int) -> Unit,
    detectedUnregisteredClasses: List<ClassResolver.DetectedUnregisteredClass>,
    excludedNonClasses: List<ClassResolver.ExcludedNonClassGroup>,
    classOverrides: Map<String, ClassResolver.ClassOverride>,
    onClassOverridesChange: (Map<String, ClassResolver.ClassOverride>) -> Unit,
    isAdmin: Boolean
) {
    val activeClasses = remember(settings.customClasses) { activeSchoolClasses(settings) }
    val newColsCount = columns.count { it.targetType == "create_new" }
    val totalProjectedFields = settings.customFields.size + newColsCount
    val matchedColsCount = columns.count { it.targetType != "create_new" && it.targetType != "skip" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Fallback Class Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateBg),
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "ডিফল্ট শ্রেণি (কোনো সারিতে শ্রেণি না থাকলে ব্যবহৃত হবে)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateText
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    activeClasses.forEach { clsDef ->
                        val sel = fallbackCls == clsDef.id
                        FilterChip(
                            selected = sel,
                            onClick = { onFallbackClsChange(clsDef.id) },
                            colors = appFilterChipColors(),
                            border = appFilterChipBorder(sel),
                            label = {
                                Text(
                                    clsDef.shortBn,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("basic_fallback_cls_${clsDef.id}")
                        )
                    }
                }
            }
        }

        if (excludedNonClasses.isNotEmpty()) {
            ExcludedNonClassSummaryBanner(excludedGroups = excludedNonClasses)
        }

        if (detectedUnregisteredClasses.isNotEmpty()) {
            UnregisteredClassesResolutionCard(
                detectedUnregisteredClasses = detectedUnregisteredClasses,
                settings = settings,
                classOverrides = classOverrides,
                onClassOverridesChange = onClassOverridesChange,
                tagPrefix = "basic"
            )
        }

        // Field quota & admin status banner
        Surface(
            color = if (totalProjectedFields > 30) AmberBg else SlateSurfaceVariant,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, if (totalProjectedFields > 30) AmberBorder else SlateOutline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "মিলেছে: ${bn(matchedColsCount.toString())}টি · নতুন ফিল্ড: ${bn(newColsCount.toString())}টি",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateText
                    )
                    Text(
                        "কাস্টম ফিল্ড সীমা: ${bn(totalProjectedFields.toString())}/৩০টি",
                        fontSize = 11.sp,
                        color = if (totalProjectedFields > 30) AmberNotice else SlateMuted
                    )
                }
                if (columns.any { it.isNew }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                onColumnsChange(
                                    columns.map { col ->
                                        if (col.isNew) col.copy(targetType = "create_new", newLabel = col.newLabel.ifBlank { col.header })
                                        else col
                                    }
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("basic_enable_all_new_fields_btn")
                        ) {
                            Text("সব নতুন রাখুন", fontSize = 11.sp)
                        }
                        TextButton(
                            onClick = {
                                onColumnsChange(
                                    columns.map { col ->
                                        if (col.targetType == "create_new") col.copy(targetType = "skip")
                                        else col
                                    }
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("basic_skip_new_fields_btn")
                        ) {
                            Text("নতুন বাদ", fontSize = 11.sp, color = SlateMuted)
                        }
                    }
                }
            }
        }

        val hasPendingNewClasses = detectedUnregisteredClasses.any { det ->
            val ov = classOverrides[det.canonicalKey]
            ov !is ClassResolver.ClassOverride.Reject && ov !is ClassResolver.ClassOverride.MapToExisting
        }
        if (!isAdmin && (newColsCount > 0 || hasPendingNewClasses)) {
            Surface(
                color = AmberBg,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, AmberBorder)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = AmberNotice, modifier = Modifier.size(18.dp))
                    Text(
                        "আপনি অ্যাডমিন নন — নতুন ফিল্ড বা নতুন শ্রেণি ডাটাবেসে যোগ করতে বিদ্যালয়ের অ্যাডমিনের মাধ্যমে ইমপোর্ট করুন, অথবা বিদ্যমান ফিল্ড/শ্রেণিতে ম্যাপ করুন।",
                        fontSize = 11.sp,
                        color = AmberNotice,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Column mapping cards
        columns.forEachIndexed { idx, col ->
            BasicColumnMappingCard(
                index = idx,
                col = col,
                settings = settings,
                onUpdateCol = { updated ->
                    onColumnsChange(columns.map { if (it.colIndex == col.colIndex) updated else it })
                }
            )
        }
    }
}

@Composable
private fun BasicColumnMappingCard(
    index: Int,
    col: Repo.CsvColMapping,
    settings: Settings,
    onUpdateCol: (Repo.CsvColMapping) -> Unit
) {
    var dropdownOpen by remember(col.colIndex) { mutableStateOf(false) }
    val targetDesc = when (col.targetType) {
        "create_new" -> "+ নতুন কাস্টম ফিল্ড: ${col.newLabel.ifBlank { col.header }}"
        "skip" -> "✕ বাদ দিন"
        "standard" -> Repo.STANDARD_FIELDS.find { it.first == col.targetKey }?.second ?: col.targetKey
        "custom" -> "কাস্টম: ${settings.customFields.find { it.key == col.targetKey }?.label ?: col.targetKey}"
        else -> col.targetKey
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (col.targetType == "create_new") AmberBg else Color.White
        ),
        border = BorderStroke(1.dp, if (col.targetType == "create_new") AmberBorder else SlateOutline),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${bn((index + 1).toString())}. ${col.header}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = SlateText,
                    modifier = Modifier.weight(1f)
                )
                val statusBadge = when (col.targetType) {
                    "skip" -> "বাদ" to SlateMuted
                    "create_new" -> "নতুন ফিল্ড" to AmberNotice
                    else -> "মিলেছে" to EmeraldGrade
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (col.targetType == "create_new") {
                        Icon(Icons.Rounded.AddCircleOutline, contentDescription = null, tint = AmberNotice, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        statusBadge.first,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusBadge.second
                    )
                }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("basic_col_map_btn_${col.colIndex}"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("$targetDesc ▾", fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                DropdownMenu(expanded = dropdownOpen, onDismissRequest = { dropdownOpen = false }) {
                    val isCreateNewSel = col.targetType == "create_new"
                    DropdownMenuItem(
                        modifier = Modifier.background(if (isCreateNewSel) TealContainer else Color.Transparent),
                        text = {
                            Text(
                                "+ নতুন ফিল্ড হিসেবে জমা রাখুন",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCreateNewSel) TealDark else AmberNotice
                            )
                        },
                        trailingIcon = if (isCreateNewSel) {
                            { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealDark, modifier = Modifier.size(16.dp)) }
                        } else null,
                        onClick = {
                            onUpdateCol(col.copy(targetType = "create_new", newLabel = col.newLabel.ifBlank { col.header }))
                            dropdownOpen = false
                        }
                    )

                    val isSkipSel = col.targetType == "skip"
                    DropdownMenuItem(
                        modifier = Modifier.background(if (isSkipSel) TealContainer else Color.Transparent),
                        text = {
                            Text(
                                "✕ বাদ দিন (ইমপোর্ট করবেন না)",
                                fontSize = 12.sp,
                                fontWeight = if (isSkipSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSkipSel) TealDark else SlateMuted
                            )
                        },
                        trailingIcon = if (isSkipSel) {
                            { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealDark, modifier = Modifier.size(16.dp)) }
                        } else null,
                        onClick = {
                            onUpdateCol(col.copy(targetType = "skip"))
                            dropdownOpen = false
                        }
                    )

                    HorizontalDivider()
                    Repo.STANDARD_FIELDS.forEach { (fKey, fLabel) ->
                        val isStdSel = col.targetType == "standard" && col.targetKey == fKey
                        DropdownMenuItem(
                            modifier = Modifier.background(if (isStdSel) TealContainer else Color.Transparent),
                            text = {
                                Text(
                                    fLabel,
                                    fontSize = 12.sp,
                                    fontWeight = if (isStdSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isStdSel) TealDark else SlateText
                                )
                            },
                            trailingIcon = if (isStdSel) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealDark, modifier = Modifier.size(16.dp)) }
                            } else null,
                            onClick = {
                                onUpdateCol(col.copy(targetType = "standard", targetKey = fKey))
                                dropdownOpen = false
                            }
                        )
                    }

                    if (settings.customFields.isNotEmpty()) {
                        HorizontalDivider()
                        settings.customFields.forEach { cf ->
                            val isCustomSel = col.targetType == "custom" && col.targetKey == cf.key
                            DropdownMenuItem(
                                modifier = Modifier.background(if (isCustomSel) TealContainer else Color.Transparent),
                                text = {
                                    Text(
                                        "কাস্টম: ${cf.label}",
                                        fontSize = 12.sp,
                                        fontWeight = if (isCustomSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCustomSel) TealDark else SlateText
                                    )
                                },
                                trailingIcon = if (isCustomSel) {
                                    { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealDark, modifier = Modifier.size(16.dp)) }
                                } else null,
                                onClick = {
                                    onUpdateCol(col.copy(targetType = "custom", targetKey = cf.key))
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
                    onValueChange = { lbl -> onUpdateCol(col.copy(newLabel = lbl.take(80))) },
                    label = { Text("নতুন কাস্টম ফিল্ডের নাম", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("basic_new_field_label_${col.colIndex}")
                )
            }
        }
    }
}
