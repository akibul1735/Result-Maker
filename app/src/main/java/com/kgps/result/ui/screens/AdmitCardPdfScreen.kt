@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.kgps.result.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kgps.result.AdmitCardConfig
import com.kgps.result.AdmitCardLocalStore
import com.kgps.result.AdmitCardPageSpec
import com.kgps.result.AdmitCardPdfGen
import com.kgps.result.AdmitCardPlanner
import com.kgps.result.BENGALI_PDF_FONT_LIST
import com.kgps.result.ENGLISH_PDF_FONT_LIST
import com.kgps.result.SeatPlanRollOrder
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.activeClassIds
import com.kgps.result.bn
import com.kgps.result.classShortBn
import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateSurfaceVariant
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

private fun clampAdmitCardPanOffset(offset: Offset, scale: Float, size: IntSize): Offset {
    if (scale <= 1.02f || size.width <= 0 || size.height <= 0) return Offset.Zero
    val maxX = (size.width * (scale - 1f)) / 2f
    val maxY = (size.height * (scale - 1f)) / 2f + size.height * 0.25f
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY)
    )
}

@Composable
fun AdmitCardPdfScreen(
    students: List<Student>,
    settings: Settings,
    permissions: TeacherPermissions,
    initialClass: Int,
    onExportPdf: (pages: List<AdmitCardPageSpec>, signatureBitmap: Bitmap?, fileName: String) -> Unit,
    onPrint: (pages: List<AdmitCardPageSpec>, signatureBitmap: Bitmap?, jobName: String) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (onBack != null) {
        BackHandler { onBack() }
    }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val availableClassIds = remember(settings.customClasses) { activeClassIds(settings) }
    val allowedClasses = remember(permissions, availableClassIds) {
        permissions.effectiveClasses(availableClassIds)
    }

    val defaultConfig = remember(initialClass, allowedClasses, settings.pdfEnglishFont) {
        val defaultCls = when {
            initialClass in allowedClasses -> initialClass
            5 in allowedClasses -> 5
            else -> allowedClasses.firstOrNull() ?: 1
        }
        AdmitCardConfig(
            selectedClass = defaultCls,
            rollOrder = SeatPlanRollOrder.DESCENDING,
            term = 3,
            cardsPerPage = 4,
            marginInches = 0.28f,
            verticalGapPt = 10f,
            bnFontId = "kalpurush",
            enFontId = settings.pdfEnglishFont.ifBlank { "times_new_roman" },
            examTimeLabel = AdmitCardLocalStore.loadExamTime(ctx)
        )
    }

    var config by remember(defaultConfig) { mutableStateOf(defaultConfig) }
    var routineRows by remember(defaultConfig.selectedClass) {
        mutableStateOf(AdmitCardLocalStore.loadRoutine(ctx, defaultConfig.selectedClass, settings))
    }
    var signatureBitmap by remember { mutableStateOf(AdmitCardLocalStore.loadSignatureBitmap(ctx)) }
    var setupExpanded by remember { mutableStateOf(true) }
    var activeSetupTab by remember { mutableIntStateOf(0) } // 0=শ্রেণি ও রোল, 1=রুটিন ও স্বাক্ষর, 2=লেআউট ও ফন্ট
    var rollFromText by remember { mutableStateOf("") }
    var rollToText by remember { mutableStateOf("") }
    var showFullscreen by remember { mutableStateOf(false) }

    val signaturePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val loaded = withContext(Dispatchers.IO) {
                    AdmitCardLocalStore.saveSignatureFromUri(ctx, uri)
                }
                if (loaded != null) {
                    signatureBitmap = loaded
                    config = config.copy(showSignatureImage = true)
                }
            }
        }
    }

    val pages = remember(students, settings, config, routineRows, allowedClasses) {
        AdmitCardPlanner.buildPages(
            students = students,
            settings = settings,
            config = config,
            routineRows = routineRows,
            allowedClassIds = allowedClasses
        )
    }

    val totalCardCount = remember(pages) { pages.sumOf { it.items.size } }

    var previewPageIdx by remember { mutableIntStateOf(0) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(pages.size) {
        if (previewPageIdx >= pages.size) {
            previewPageIdx = (pages.size - 1).coerceAtLeast(0)
        }
    }
    LaunchedEffect(previewPageIdx, pages.size) {
        zoomScale = 1f
        panOffset = Offset.Zero
    }

    val activePage = pages.getOrNull(previewPageIdx)
    val previewBitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = activePage,
        key2 = settings,
        key3 = signatureBitmap
    ) {
        value = if (activePage == null) {
            null
        } else {
            withContext(Dispatchers.Default) {
                AdmitCardPdfGen.renderPageBitmap(
                    ctx = ctx,
                    page = activePage,
                    settings = settings,
                    signatureBitmap = signatureBitmap,
                    scale = 2.0f
                )
            }
        }
    }

    val countByClass = remember(students, allowedClasses) {
        allowedClasses.associateWith { c -> students.count { it.cls == c } }
    }

    val activeBnFontName = remember(config.bnFontId) {
        BENGALI_PDF_FONT_LIST.find { it.id == config.bnFontId }?.labelBn?.substringBefore(" (")
            ?: "কালপুরুষ"
    }
    val activeEnFontName = remember(config.enFontId) {
        ENGLISH_PDF_FONT_LIST.find { it.id == config.enFontId }?.labelBn?.substringBefore(" (")
            ?: "Times New Roman"
    }

    val summaryLine = buildString {
        append("${classShortBn(config.selectedClass, settings)} শ্রেণি")
        append(" · ${bn(config.safeCardsPerPage.toString())} কার্ড/পৃষ্ঠা")
        append(" · ${formatAdmitInchesBn(config.safeMarginInches)}")
        append(" · ${bn(totalCardCount.toString())} জন")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Settings Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { setupExpanded = !setupExpanded }
                        .testTag("admit_card_setup_toggle"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(color = TealSoft, shape = RoundedCornerShape(8.dp)) {
                        Icon(
                            Icons.Rounded.Badge,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.padding(6.dp).size(18.dp)
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "প্রবেশপত্র সেটিংস ও রুটিন",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = SlateText
                        )
                        Text(
                            summaryLine,
                            fontSize = 10.5.sp,
                            color = SlateMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = {
                            config = defaultConfig.copy(examTimeLabel = "দুপুর ০১:০০-০৩:৩০ টা")
                            AdmitCardLocalStore.saveExamTime(ctx, config.examTimeLabel)
                            val resetRoutine = AdmitCardPlanner.defaultRoutineForClass(defaultConfig.selectedClass, settings)
                            routineRows = resetRoutine
                            AdmitCardLocalStore.saveRoutine(ctx, defaultConfig.selectedClass, resetRoutine)
                            rollFromText = ""
                            rollToText = ""
                        },
                        modifier = Modifier.size(48.dp).testTag("admit_card_reset_btn")
                    ) {
                        Icon(
                            Icons.Rounded.RestartAlt,
                            contentDescription = "ডিফল্ট সেটিংস রিসেট",
                            tint = SlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Icon(
                        imageVector = if (setupExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (setupExpanded) "সেটিংস লুকান" else "সেটিংস খুলুন",
                        tint = SlateMuted
                    )
                }

                AnimatedVisibility(visible = setupExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdmitSegmentedOptionBar(
                            items = listOf(
                                0 to "শ্রেণি ও রোল",
                                1 to "রুটিন ও স্বাক্ষর",
                                2 to "লেআউট ও ফন্ট"
                            ),
                            selected = activeSetupTab,
                            onSelect = { activeSetupTab = it },
                            useLightActivePill = true,
                            tagPrefix = "admit_card_tab"
                        )

                        when (activeSetupTab) {
                            0 -> AdmitClassAndRollTab(
                                config = config,
                                settings = settings,
                                allowedClasses = allowedClasses,
                                countByClass = countByClass,
                                rollFromText = rollFromText,
                                rollToText = rollToText,
                                onSelectClass = { newCls ->
                                    config = config.copy(selectedClass = newCls)
                                    routineRows = AdmitCardLocalStore.loadRoutine(ctx, newCls, settings)
                                },
                                onUpdateConfig = { config = it },
                                onRollFromChange = { digits ->
                                    rollFromText = digits
                                    config = config.copy(rollFrom = digits.toIntOrNull())
                                },
                                onRollToChange = { digits ->
                                    rollToText = digits
                                    config = config.copy(rollTo = digits.toIntOrNull())
                                }
                            )

                            1 -> AdmitRoutineAndSignatureTab(
                                config = config,
                                settings = settings,
                                routineRows = routineRows,
                                signatureBitmap = signatureBitmap,
                                onUpdateConfig = { config = it },
                                onExamTimeChange = { newTime ->
                                    config = config.copy(examTimeLabel = newTime)
                                    AdmitCardLocalStore.saveExamTime(ctx, newTime)
                                },
                                onUpdateRoutineRows = { updated ->
                                    routineRows = updated
                                    AdmitCardLocalStore.saveRoutine(ctx, config.selectedClass, updated)
                                },
                                onPickSignatureImage = {
                                    signaturePickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onClearSignatureImage = {
                                    AdmitCardLocalStore.clearSignature(ctx)
                                    signatureBitmap = null
                                }
                            )

                            else -> AdmitLayoutAndStyleTab(
                                ctx = ctx,
                                config = config,
                                settings = settings,
                                activeBnFontName = activeBnFontName,
                                activeEnFontName = activeEnFontName,
                                onUpdateConfig = { config = it }
                            )
                        }
                    }
                }
            }
        }

        // 2. Live Preview Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (pages.isEmpty()) "প্রিভিউ" else "লাইভ প্রিভিউ · পৃষ্ঠা ${bn((previewPageIdx + 1).toString())}/${bn(pages.size.toString())}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SlateText
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (pages.size > 1) {
                            IconButton(
                                onClick = { if (previewPageIdx > 0) previewPageIdx-- },
                                enabled = previewPageIdx > 0,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "আগের পৃষ্ঠা")
                            }
                            IconButton(
                                onClick = { if (previewPageIdx < pages.lastIndex) previewPageIdx++ },
                                enabled = previewPageIdx < pages.lastIndex,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "পরের পৃষ্ঠা")
                            }
                        }
                        if (previewBitmap != null) {
                            IconButton(
                                onClick = { showFullscreen = true },
                                modifier = Modifier.size(48.dp).testTag("admit_card_fullscreen_btn")
                            ) {
                                Icon(Icons.Rounded.Fullscreen, contentDescription = "পূর্ণ স্ক্রিন প্রিভিউ", tint = TealPrimary)
                            }
                        }
                    }
                }

                if (pages.isEmpty()) {
                    Surface(
                        color = SlateBg,
                        border = BorderStroke(1.dp, SlateOutline),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "নির্বাচিত শ্রেণি বা রোল রেঞ্জে কোনো শিক্ষার্থী নেই।",
                                fontSize = 12.5.sp,
                                color = SlateMuted
                            )
                            OutlinedButton(
                                onClick = { config = config.copy(extraBlankCards = config.safeCardsPerPage) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("১ পৃষ্ঠা ফাঁকা প্রবেশপত্র তৈরি করুন", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(595f / 842f)
                            .background(SlateSurfaceVariant, RoundedCornerShape(10.dp))
                            .clipToBounds()
                            .onSizeChanged { previewSize = it }
                            .pointerInput(previewPageIdx, pages.size) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (zoomScale > 1.05f) {
                                            zoomScale = 1f
                                            panOffset = Offset.Zero
                                        } else {
                                            zoomScale = 2.1f
                                        }
                                    }
                                )
                            }
                            .pointerInput(previewPageIdx, pages.size) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    do {
                                        val event = awaitPointerEvent()
                                        val pointerCount = event.changes.count { it.pressed }
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        if (pointerCount >= 2 || zoomScale > 1.02f) {
                                            val nextScale = (zoomScale * zoomChange).coerceIn(1f, 3.5f)
                                            zoomScale = nextScale
                                            panOffset = clampAdmitCardPanOffset(
                                                panOffset + panChange,
                                                nextScale,
                                                previewSize
                                            )
                                            if (pointerCount >= 2 || abs(zoomChange - 1f) > 0.01f) {
                                                event.changes.forEach { it.consume() }
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val bmp = previewBitmap
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "প্রবেশপত্র প্রিভিউ",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = zoomScale,
                                        scaleY = zoomScale,
                                        translationX = panOffset.x,
                                        translationY = panOffset.y
                                    )
                            )
                        } else {
                            CircularProgressIndicator(color = TealPrimary, modifier = Modifier.size(28.dp))
                        }
                    }
                    Text(
                        "জুম করতে দুই আঙুলে টানুন বা ডাবল ট্যাপ করুন",
                        fontSize = 10.5.sp,
                        color = SlateMuted
                    )
                }
            }
        }

        // 3. Print & Export PDF Action Buttons
        val exportName = "প্রবেশপত্র-${classShortBn(config.selectedClass, settings)}-শ্রেণি.pdf"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onPrint(pages, signatureBitmap, "প্রবেশপত্র") },
                enabled = pages.isNotEmpty(),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.6f)),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("admit_card_print_btn")
            ) {
                Icon(Icons.Rounded.Print, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("প্রিন্ট করুন", fontWeight = FontWeight.SemiBold, color = TealPrimary)
            }
            Button(
                onClick = { onExportPdf(pages, signatureBitmap, exportName) },
                enabled = pages.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("admit_card_export_btn")
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("PDF সংরক্ষণ", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showFullscreen && previewBitmap != null) {
        Dialog(
            onDismissRequest = { showFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            var fsScale by remember { mutableFloatStateOf(1f) }
            var fsPan by remember { mutableStateOf(Offset.Zero) }
            var fsSize by remember { mutableStateOf(IntSize.Zero) }
            Surface(
                color = Color.Black.copy(alpha = 0.94f),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .onSizeChanged { fsSize = it }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (fsScale > 1.05f) {
                                            fsScale = 1f
                                            fsPan = Offset.Zero
                                        } else {
                                            fsScale = 2.2f
                                        }
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    do {
                                        val event = awaitPointerEvent()
                                        val pointerCount = event.changes.count { it.pressed }
                                        val z = event.calculateZoom()
                                        val p = event.calculatePan()
                                        if (pointerCount >= 2 || fsScale > 1.02f) {
                                            val ns = (fsScale * z).coerceIn(1f, 4f)
                                            fsScale = ns
                                            fsPan = clampAdmitCardPanOffset(fsPan + p, ns, fsSize)
                                            if (pointerCount >= 2 || abs(z - 1f) > 0.01f) {
                                                event.changes.forEach { it.consume() }
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        previewBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "পূর্ণ স্ক্রিন প্রবেশপত্র",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = fsScale,
                                        scaleY = fsScale,
                                        translationX = fsPan.x,
                                        translationY = fsPan.y
                                    )
                            )
                        }
                    }
                    IconButton(
                        onClick = { showFullscreen = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "বন্ধ করুন", tint = Color.White)
                    }
                }
            }
        }
    }
}
