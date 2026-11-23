package com.kgps.result.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.kgps.result.BlankSheetCatalog
import com.kgps.result.BlankSheetQrMeta
import com.kgps.result.CsvImportAssistant
import com.kgps.result.CsvImportModule
import com.kgps.result.CsvPromptConfig
import com.kgps.result.CsvRowStatus
import com.kgps.result.Repo
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.ai.SheetQrScanner
import com.kgps.result.bn
import com.kgps.result.cloud.SheetTemplateCache
import com.kgps.result.defaultSelectedColumnKeys
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateSurfaceVariant
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealContainer
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private fun createCsvScanTempUri(context: Context): Uri {
    val imgFile = File(context.cacheDir, "csv_sheet_scan_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imgFile)
}

private fun decodeScaledSheetBitmap(context: Context, uri: Uri, maxDim: Int = 2800): Bitmap? {
    return try {
        val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val w = info.size.width
                val h = info.size.height
                val longest = maxOf(w, h)
                if (longest > maxDim) {
                    val ratio = maxDim.toFloat() / longest
                    decoder.setTargetSize(
                        (w * ratio).toInt().coerceAtLeast(1),
                        (h * ratio).toInt().coerceAtLeast(1)
                    )
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } ?: return null

        val longest = maxOf(raw.width, raw.height)
        if (longest <= maxDim) {
            raw
        } else {
            val ratio = maxDim.toFloat() / longest
            val scaled = Bitmap.createScaledBitmap(
                raw,
                (raw.width * ratio).toInt().coerceAtLeast(1),
                (raw.height * ratio).toInt().coerceAtLeast(1),
                true
            )
            if (scaled !== raw) raw.recycle()
            scaled
        }
    } catch (_: Exception) {
        null
    }
}

private enum class CsvWizardStep(val num: Int, val shortTitle: String) {
    OPTIONS(1, "অপশন"),
    PROMPT(2, "প্রম্পট"),
    INPUT(3, "আউটপুট"),
    PREVIEW(4, "প্রিভিউ")
}

@Composable
fun SmartCsvImportDialog(
    students: List<Student>,
    settings: Settings,
    defaultCls: Int,
    initialText: String,
    onResolveRemoteTemplate: suspend (String) -> BlankSheetQrMeta? = { null },
    onDismiss: () -> Unit,
    onImported: (Repo.CsvImportSummary) -> Unit,
    onError: (String) -> Unit
) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var currentStep by remember(initialText) {
        mutableStateOf(
            if (initialText.isNotBlank()) CsvWizardStep.INPUT else CsvWizardStep.OPTIONS
        )
    }

    var promptConfig by remember(defaultCls, settings.customFields) {
        val safeCls = defaultCls.coerceIn(1, 20)
        mutableStateOf(
            CsvPromptConfig(
                module = CsvImportModule.STUDENT_PROFILE,
                targetCls = safeCls,
                targetTerm = 1,
                selectedColumnKeys = defaultSelectedColumnKeys(
                    module = CsvImportModule.STUDENT_PROFILE,
                    targetCls = safeCls,
                    targetTerm = 1,
                    customFields = settings.customFields
                )
            )
        )
    }
    var promptCopiedRecently by remember { mutableStateOf(false) }

    var isScanningSheet by remember { mutableStateOf(false) }
    var scannedBanner by remember { mutableStateOf<CsvScannedSheetBannerInfo?>(null) }
    var scannedWorkingDaysByTerm by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    var rawText by remember(initialText) { mutableStateOf(initialText) }
    var fallbackCls by remember(defaultCls) { mutableStateOf(defaultCls.coerceIn(1, 20)) }
    val parsedSheet = remember(rawText) {
        if (rawText.isBlank()) Repo.CsvParsedSheet(emptyList(), null, null, null, false, 0)
        else Repo.parseSmartCsvSheet(rawText)
    }
    val parsedRows = parsedSheet.rows
    var contextSubjectKey by remember { mutableStateOf<String?>(null) }
    var contextTerm by remember { mutableStateOf<Int?>(null) }
    var sharedWorkingDaysByTerm by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }

    fun scanSheetUri(uri: Uri) {
        if (isScanningSheet) return
        scope.launch {
            isScanningSheet = true
            try {
                val (bmpFound, detection, cachedMeta) = withContext(Dispatchers.IO) {
                    val bmp = decodeScaledSheetBitmap(ctx, uri, 2800)
                    if (bmp == null) {
                        Triple(false, com.kgps.result.ai.SheetScanDetection(), null)
                    } else {
                        try {
                            val det = SheetQrScanner.decodeAllFromBitmap(bmp)
                            val tid = det.templateId?.trim()?.uppercase()?.takeIf { it.length >= 6 }
                            val cached = tid?.let { SheetTemplateCache(ctx).get(it) }
                            Triple(true, det, cached)
                        } finally {
                            bmp.recycle()
                        }
                    }
                }
                if (!bmpFound) {
                    onError("ছবিটি পড়া যায়নি")
                    return@launch
                }
                val tid = detection.templateId?.trim()?.uppercase()?.takeIf { it.length >= 6 }
                val remoteMeta = if (cachedMeta == null && tid != null) {
                    runCatching { onResolveRemoteTemplate(tid) }.getOrNull()
                } else null

                val resolvedMeta = cachedMeta ?: remoteMeta ?: detection.bestLocalMeta
                if (resolvedMeta != null) {
                    val nextConfig = CsvImportAssistant.configFromScannedSheetMeta(resolvedMeta, settings, promptConfig)
                    promptConfig = nextConfig
                    fallbackCls = resolvedMeta.cls.coerceIn(1, 20)
                    contextSubjectKey = resolvedMeta.subjectKey?.takeIf {
                        it.isNotBlank() && it != BlankSheetCatalog.ALL_SUBJECTS_KEY
                    }
                    contextTerm = resolvedMeta.term.takeIf { it in 1..3 }
                    scannedWorkingDaysByTerm = resolvedMeta.workingDaysByTerm
                    sharedWorkingDaysByTerm = scannedWorkingDaysByTerm + parsedSheet.detectedWorkingDaysByTerm

                    val srcLabel = when {
                        cachedMeta != null && !detection.detectedFromOfflineQr -> "ক্যাশ টেমপ্লেট"
                        remoteMeta != null -> "ক্লাউড টেমপ্লেট"
                        detection.sourceLabelBn.isNotBlank() -> detection.sourceLabelBn
                        else -> "শীট কোড"
                    }
                    scannedBanner = CsvScannedSheetBannerInfo(
                        summaryBn = CsvImportAssistant.buildScannedSheetSummaryBn(resolvedMeta, settings),
                        detection = detection,
                        sourceLabelBn = srcLabel
                    )
                    promptCopiedRecently = false
                    currentStep = CsvWizardStep.PROMPT
                    Toast.makeText(ctx, "শীট শনাক্ত হয়েছে — AI প্রম্পট প্রস্তুত!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        ctx,
                        "ছবিতে QR বা বারকোড শনাক্ত করা যায়নি — কোণার QR ও নিচের বারকোড স্পষ্ট রেখে আবার তুলুন",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                isScanningSheet = false
            }
        }
    }

    val sheetGalleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) scanSheetUri(uri)
    }

    val sheetCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            scanSheetUri(uri)
        }
    }

    val launchSheetCamera: () -> Unit = {
        runCatching {
            val uri = createCsvScanTempUri(ctx)
            pendingCameraUri = uri
            sheetCameraLauncher.launch(uri)
        }.onFailure { e ->
            onError("ক্যামেরা চালু করা যায়নি: ${e.message ?: "ত্রুটি"}")
        }
    }
    val launchSheetGallery: () -> Unit = {
        sheetGalleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    LaunchedEffect(promptConfig.targetCls) {
        if (promptConfig.targetCls > 0 && parsedSheet.detectedCls == null) {
            fallbackCls = promptConfig.targetCls
        }
    }
    LaunchedEffect(parsedSheet, promptConfig.module, promptConfig.selectedSubjectKeys, scannedWorkingDaysByTerm) {
        parsedSheet.detectedCls?.let { fallbackCls = it }
        if (parsedSheet.detectedTerm != null) {
            contextTerm = parsedSheet.detectedTerm
        }
        if (parsedSheet.detectedSubjectKey != null) {
            contextSubjectKey = parsedSheet.detectedSubjectKey
        } else if (promptConfig.module == CsvImportModule.SUBJECT_ALL_TERMS) {
            contextSubjectKey = CsvImportAssistant.resolveSubjectForAllTerms(promptConfig).key
        } else if (promptConfig.module == CsvImportModule.TERM_MARKS && promptConfig.selectedSubjectKeys.size == 1) {
            contextSubjectKey = promptConfig.selectedSubjectKeys.first()
        }
        sharedWorkingDaysByTerm = scannedWorkingDaysByTerm + parsedSheet.detectedWorkingDaysByTerm
    }
    val promptFallbackTerm = if (promptConfig.module != CsvImportModule.STUDENT_PROFILE) {
        promptConfig.targetTerm.takeIf { it in 1..3 }
    } else null
    var columns by remember(parsedRows, contextSubjectKey, contextTerm, parsedSheet.detectedTerm, promptFallbackTerm, settings.customFields) {
        mutableStateOf(
            Repo.detectCsvColumns(
                rows = parsedRows,
                existingCustomFields = settings.customFields,
                contextSubjectKey = contextSubjectKey,
                contextTerm = parsedSheet.detectedTerm ?: contextTerm ?: promptFallbackTerm
            )
        )
    }

    val validationReport = remember(parsedRows, columns, students, fallbackCls, sharedWorkingDaysByTerm) {
        CsvImportAssistant.validateCsvData(
            parsedRows = parsedRows,
            columns = columns,
            existingStudents = students,
            fallbackCls = fallbackCls,
            sharedWorkingDaysByTerm = sharedWorkingDaysByTerm
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

    var pendingTemplateContent by remember { mutableStateOf("") }
    val saveTemplateLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(("\uFEFF" + pendingTemplateContent).toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(ctx, "CSV টেমপ্লেট ফাইল সংরক্ষিত হয়েছে", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                onError("ফাইল সেভ করা যায়নি: ${e.message ?: "ত্রুটি"}")
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val loaded = ctx.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    ?: ""
                rawText = loaded
                if (Repo.parseSmartCsvSheet(loaded).rows.size >= 2) {
                    currentStep = CsvWizardStep.PREVIEW
                } else {
                    Toast.makeText(ctx, "ফাইল লোড হয়েছে", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                onError("ফাইল পড়া যায়নি: ${e.message ?: "ত্রুটি"}")
            }
        }
    }

    val finalSummary = remember(parsedRows, columns, selectedRowIndices, students, settings, fallbackCls, sharedWorkingDaysByTerm) {
        CsvImportAssistant.buildFilteredImportSummary(
            parsedRows = parsedRows,
            columns = columns,
            selectedDataRowIndices = selectedRowIndices,
            existingStudents = students,
            existingSettings = settings,
            fallbackCls = fallbackCls,
            sharedWorkingDaysByTerm = sharedWorkingDaysByTerm
        )
    }

    BackHandler {
        when (currentStep) {
            CsvWizardStep.OPTIONS -> onDismiss()
            CsvWizardStep.PROMPT -> currentStep = CsvWizardStep.OPTIONS
            CsvWizardStep.INPUT -> currentStep = CsvWizardStep.PROMPT
            CsvWizardStep.PREVIEW -> currentStep = CsvWizardStep.INPUT
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 880.dp)
                .heightIn(min = 520.dp, max = 760.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Compact Header + Stepper
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
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "স্মার্ট AI ও CSV ইমপোর্ট",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TealDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(40.dp).testTag("csv_dialog_close_btn")
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "বন্ধ করুন", tint = SlateText, modifier = Modifier.size(20.dp))
                            }
                        }

                        // Compact 4-step interactive stepper
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            CsvWizardStep.entries.forEach { step ->
                                val active = currentStep == step
                                val done = step.num < currentStep.num ||
                                    (step == CsvWizardStep.INPUT && parsedRows.size >= 2)
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
                                        .testTag("csv_step_tab_${step.num}")
                                        .clickable { currentStep = step },
                                    shape = RoundedCornerShape(8.dp),
                                    color = bg,
                                    border = BorderStroke(1.dp, if (active || done) TealPrimary.copy(alpha = 0.4f) else SlateOutline)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "${bn(step.num.toString())}. ${step.shortTitle}",
                                            fontSize = 11.sp,
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
                        CsvWizardStep.OPTIONS -> CsvStepOptions(
                            config = promptConfig,
                            settings = settings,
                            isScanningSheet = isScanningSheet,
                            scannedBanner = scannedBanner,
                            onScanCamera = launchSheetCamera,
                            onScanGallery = launchSheetGallery,
                            onConfigChange = {
                                promptConfig = it
                                promptCopiedRecently = false
                            }
                        )

                        CsvWizardStep.PROMPT -> CsvStepPrompt(
                            config = promptConfig,
                            students = students,
                            settings = settings,
                            promptCopiedRecently = promptCopiedRecently,
                            isScanningSheet = isScanningSheet,
                            scannedBanner = scannedBanner,
                            onScanCamera = launchSheetCamera,
                            onScanGallery = launchSheetGallery,
                            onConfigChange = {
                                promptConfig = it
                                promptCopiedRecently = false
                            },
                            onCopyPrompt = { txt ->
                                clipboard.setText(AnnotatedString(txt))
                                promptCopiedRecently = true
                                Toast.makeText(ctx, "AI প্রম্পট ক্লিপবোর্ডে কপি হয়েছে", Toast.LENGTH_SHORT).show()
                            },
                            onCopyHeaderOnly = { hdr ->
                                clipboard.setText(AnnotatedString(hdr))
                                Toast.makeText(ctx, "শুধু হেডার লাইন কপি হয়েছে", Toast.LENGTH_SHORT).show()
                            },
                            onCopyRosterTemplate = { csvTxt, isRoster ->
                                clipboard.setText(AnnotatedString(csvTxt))
                                Toast.makeText(
                                    ctx,
                                    if (isRoster) "রোল-নামসহ শিট কপি হয়েছে" else "ফাঁকা টেমপ্লেট কপি হয়েছে",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onSaveTemplateFile = { csvTxt ->
                                pendingTemplateContent = csvTxt
                                val safeName = if (promptConfig.module == CsvImportModule.SUBJECT_ALL_TERMS) {
                                    val subKey = CsvImportAssistant.resolveSubjectForAllTerms(promptConfig).key
                                    "class_${promptConfig.targetCls}_${subKey}_3terms.csv"
                                } else {
                                    "kgps_import_template.csv"
                                }
                                saveTemplateLauncher.launch(safeName)
                            },
                            onGoToStep3 = { currentStep = CsvWizardStep.INPUT }
                        )

                        CsvWizardStep.INPUT -> CsvStepInputAndMapping(
                            rawText = rawText,
                            onRawTextChange = { rawText = it },
                            parsedSheet = parsedSheet,
                            parsedRows = parsedRows,
                            columns = columns,
                            onColumnsChange = { columns = it },
                            fallbackCls = fallbackCls,
                            onFallbackClsChange = { fallbackCls = it },
                            contextTerm = contextTerm,
                            onContextTermChange = { contextTerm = it },
                            contextSubjectKey = contextSubjectKey,
                            onContextSubjectKeyChange = { contextSubjectKey = it },
                            sharedWorkingDaysByTerm = sharedWorkingDaysByTerm,
                            onSharedWorkingDaysChange = { sharedWorkingDaysByTerm = it },
                            settings = settings,
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
                            onGoToPreview = { currentStep = CsvWizardStep.PREVIEW }
                        )

                        CsvWizardStep.PREVIEW -> CsvStepPreviewAndSave(
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
                            onJumpToInput = { currentStep = CsvWizardStep.INPUT }
                        )
                    }
                }

                // Bottom action bar
                HorizontalDivider(color = SlateOutline)
                Surface(color = SlateBg) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStep == CsvWizardStep.OPTIONS) {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("csv_cancel_btn")
                            ) {
                                Text("বাতিল", fontSize = 12.5.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    currentStep = when (currentStep) {
                                        CsvWizardStep.PROMPT -> CsvWizardStep.OPTIONS
                                        CsvWizardStep.INPUT -> CsvWizardStep.PROMPT
                                        CsvWizardStep.PREVIEW -> CsvWizardStep.INPUT
                                        CsvWizardStep.OPTIONS -> CsvWizardStep.OPTIONS
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("csv_prev_step_btn")
                            ) {
                                Text("← পূর্ববর্তী", fontSize = 12.5.sp)
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        when (currentStep) {
                            CsvWizardStep.OPTIONS -> Button(
                                onClick = { currentStep = CsvWizardStep.PROMPT },
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("csv_next_to_prompt_btn")
                            ) {
                                Text("AI প্রম্পট তৈরি করুন →", fontSize = 12.5.sp)
                            }

                            CsvWizardStep.PROMPT -> Button(
                                onClick = {
                                    val promptText = CsvImportAssistant.buildAiPrompt(promptConfig, settings, students)
                                    clipboard.setText(AnnotatedString(promptText))
                                    promptCopiedRecently = true
                                    Toast.makeText(ctx, "প্রম্পট কপি হয়েছে — এবার AI-তে ব্যবহার করুন", Toast.LENGTH_SHORT).show()
                                    currentStep = CsvWizardStep.INPUT
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("csv_copy_and_next_btn")
                            ) {
                                Text("প্রম্পট কপি ও পরবর্তী ধাপ →", fontSize = 12.5.sp)
                            }

                            CsvWizardStep.INPUT -> Button(
                                onClick = { currentStep = CsvWizardStep.PREVIEW },
                                enabled = parsedRows.size >= 2,
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("csv_next_to_preview_btn")
                            ) {
                                Text("প্রিভিউ ও ভ্যালিডেশন →", fontSize = 12.5.sp)
                            }

                            CsvWizardStep.PREVIEW -> {
                                val selectedValidCount = finalSummary?.let { it.addedCount + it.updatedCount } ?: 0
                                val canSave = finalSummary != null &&
                                    !validationReport.hasMissingRollOrNameMapping &&
                                    selectedValidCount > 0
                                Button(
                                    onClick = { if (finalSummary != null) onImported(finalSummary) },
                                    enabled = canSave,
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("csv_confirm_import_btn")
                                ) {
                                    Text(
                                        if (selectedValidCount > 0) {
                                            "চূড়ান্ত সেভ (${bn(selectedValidCount.toString())} জন)"
                                        } else {
                                            "চূড়ান্ত সেভ"
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
