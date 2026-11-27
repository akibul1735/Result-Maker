@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.kgps.result.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import com.kgps.result.AdmitCardConfig
import com.kgps.result.AdmitCardLocalStore
import com.kgps.result.AdmitCardPageSpec
import com.kgps.result.AdmitCardPlanner
import com.kgps.result.BENGALI_PDF_FONT_LIST
import com.kgps.result.BaseRoutineTemplate
import com.kgps.result.ENGLISH_PDF_FONT_LIST
import com.kgps.result.SeatPlanRollOrder
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.activeClassIds
import com.kgps.result.ai.DirectGeminiAiRepository
import com.kgps.result.ai.ExamRoutineNoticeExtractor
import com.kgps.result.ai.ExtractedExamRoutineNotice
import com.kgps.result.ai.GeminiApiConfig
import com.kgps.result.bn
import com.kgps.result.classShortBn
import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.ui.theme.RoseDanger
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private data class AdmitBottomTabItem(
    val index: Int,
    val label: String,
    val panelTitle: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
)

private val ADMIT_BOTTOM_TABS = listOf(
    AdmitBottomTabItem(0, "শ্রেণি ও রোল", "শ্রেণি, মূল্যায়ন ও রোল", Icons.Rounded.Groups, Icons.Outlined.Groups),
    AdmitBottomTabItem(1, "রুটিন", "পরীক্ষার রুটিন ও সময়", Icons.Rounded.EventNote, Icons.Outlined.EventNote),
    AdmitBottomTabItem(2, "স্বাক্ষর ও তথ্য", "স্বাক্ষর ও শিরোনাম", Icons.Rounded.Draw, Icons.Outlined.Draw),
    AdmitBottomTabItem(3, "লেআউট ও ফন্ট", "পেজ লেআউট, ফন্ট ও বর্ডার", Icons.Rounded.Tune, Icons.Outlined.Tune)
)

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
            selectedClasses = listOf(defaultCls),
            rollOrder = SeatPlanRollOrder.DESCENDING,
            term = 3,
            cardsPerPage = 4,
            marginInches = 0.28f,
            verticalGapPt = 10f,
            bnFontId = "kalpurush",
            enFontId = settings.pdfEnglishFont.ifBlank { "times_new_roman" },
            examTimeLabel = AdmitCardLocalStore.loadExamTime(ctx, defaultCls, settings)
        )
    }

    var config by remember(defaultConfig) {
        val loaded = AdmitCardLocalStore.loadConfig(ctx, defaultConfig)
        mutableStateOf(
            loaded.copy(
                selectedClass = defaultConfig.selectedClass,
                selectedClasses = defaultConfig.selectedClasses,
                examTimeLabel = AdmitCardLocalStore.loadExamTime(ctx, defaultConfig.selectedClass, settings)
            )
        )
    }
    LaunchedEffect(config) {
        AdmitCardLocalStore.saveConfig(ctx, config)
    }
    var routineRows by remember(defaultConfig.selectedClass) {
        mutableStateOf(AdmitCardLocalStore.loadRoutine(ctx, defaultConfig.selectedClass, settings))
    }
    var baseRoutines by remember {
        mutableStateOf(AdmitCardLocalStore.loadBaseRoutines(ctx, settings))
    }
    var activeBaseId by remember { mutableStateOf<String?>(null) }
    var signatureBitmap by remember { mutableStateOf(AdmitCardLocalStore.loadSignatureBitmap(ctx)) }
    var openTab by remember { mutableStateOf<Int?>(null) } // null = collapsed; 0..3 = compact bottom sheet panel open
    var rollFromText by remember { mutableStateOf("") }
    var rollToText by remember { mutableStateOf("") }
    var showFullscreen by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var panelHeightFraction by remember { mutableFloatStateOf(0.48f) }
    var isCustomPanelHeight by remember { mutableStateOf(false) }
    var previewViewportHeightPx by remember { mutableIntStateOf(0) }
    var previewZoomScale by remember { mutableFloatStateOf(1f) }
    var previewPanOffset by remember { mutableStateOf(Offset.Zero) }
    var previewPageBoxSize by remember { mutableStateOf(IntSize.Zero) }
    var topAiMenuOpen by remember { mutableStateOf(false) }
    var pendingExportAction by remember { mutableStateOf<AdmitExportAction?>(null) }
    var selectedExportClasses by remember(defaultConfig.selectedClass) {
        mutableStateOf(setOf(defaultConfig.selectedClass))
    }

    val directAiRepository = remember(ctx) { DirectGeminiAiRepository(ctx.applicationContext) }
    var aiRoutineBusy by remember { mutableStateOf(false) }
    var aiRoutineError by remember { mutableStateOf<String?>(null) }
    var aiEnhanceNotice by remember { mutableStateOf(true) }
    var pendingAiNotice by remember { mutableStateOf<ExtractedExamRoutineNotice?>(null) }

    // Back priority: zoomed preview resets first -> open panel closes -> exit screen
    if (onBack != null) {
        BackHandler(enabled = !showFullscreen && openTab == null && previewZoomScale <= 1.02f) { onBack() }
    }
    BackHandler(enabled = !showFullscreen && openTab != null && previewZoomScale <= 1.02f) {
        openTab = null
    }
    BackHandler(enabled = !showFullscreen && previewZoomScale > 1.02f) {
        previewZoomScale = 1f
        previewPanOffset = Offset.Zero
    }

    fun handlePickedSignatureUri(uri: Uri?) {
        if (uri == null) return
        scope.launch {
            val loaded = withContext(Dispatchers.IO) {
                AdmitCardLocalStore.saveSignatureFromUri(ctx, uri)
            }
            if (loaded != null) {
                signatureBitmap = loaded
                config = config.copy(showSignatureImage = true)
            } else {
                Toast.makeText(ctx, "স্বাক্ষরের ছবি লোড করা যায়নি", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val signatureIntentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handlePickedSignatureUri(result.data?.data)
    }

    fun openGalleryPicker() {
        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }
        val chooser = Intent.createChooser(pickIntent, "গ্যালারি থেকে স্বাক্ষর বাছুন")
        runCatching {
            signatureIntentLauncher.launch(chooser)
        }.onFailure {
            val fallback = Intent.createChooser(
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                },
                "গ্যালারি থেকে স্বাক্ষর বাছুন"
            )
            runCatching { signatureIntentLauncher.launch(fallback) }
        }
    }

    fun openFileManagerPicker() {
        val getContentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_LOCAL_ONLY, false)
        }
        val chooser = Intent.createChooser(getContentIntent, "ফাইল ম্যানেজার থেকে স্বাক্ষর বাছুন")
        runCatching {
            signatureIntentLauncher.launch(chooser)
        }.onFailure {
            val openDocFallback = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            runCatching { signatureIntentLauncher.launch(openDocFallback) }
        }
    }

    fun processNoticeUriWithAi(uri: Uri?) {
        if (uri == null || aiRoutineBusy) return
        aiRoutineError = null
        aiRoutineBusy = true
        scope.launch {
            try {
                val bmp = ExamRoutineNoticeExtractor.decodeNoticeUriToBitmap(ctx, uri)
                    ?: throw IllegalStateException("নির্বাচিত ছবি বা PDF পড়া যায়নি। আরেকটি স্পষ্ট ফাইল বেছে নিন।")
                val extracted = ExamRoutineNoticeExtractor.extractFromBitmap(
                    repository = directAiRepository,
                    bitmap = bmp,
                    allowedClasses = allowedClasses,
                    settings = settings,
                    documentEnhance = aiEnhanceNotice
                )
                pendingAiNotice = extracted
            } catch (t: Throwable) {
                aiRoutineError = t.message ?: "AI দিয়ে নোটিশ পড়া যায়নি। আবার চেষ্টা করুন।"
            } finally {
                aiRoutineBusy = false
            }
        }
    }

    val noticeUriLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        processNoticeUriWithAi(result.data?.data)
    }

    fun openNoticeImagePicker() {
        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }
        val chooser = Intent.createChooser(pickIntent, "নোটিশের ছবি বাছুন")
        runCatching {
            noticeUriLauncher.launch(chooser)
        }.onFailure {
            val fallback = Intent.createChooser(
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                },
                "নোটিশের ছবি বাছুন"
            )
            runCatching { noticeUriLauncher.launch(fallback) }
        }
    }

    fun openNoticeFileOrPdfPicker() {
        val docIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "application/pdf"))
            putExtra(Intent.EXTRA_LOCAL_ONLY, false)
        }
        val chooser = Intent.createChooser(docIntent, "PDF বা নোটিশ ফাইল বাছুন")
        runCatching {
            noticeUriLauncher.launch(chooser)
        }.onFailure {
            val fallback = Intent.createChooser(
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "application/pdf"))
                },
                "PDF বা নোটিশ ফাইল বাছুন"
            )
            runCatching { noticeUriLauncher.launch(fallback) }
        }
    }

    val resolvedTabClasses = remember(config.selectedClass, config.selectedClasses, allowedClasses) {
        val valid = config.resolvedSelectedClasses.filter { it in allowedClasses }
        if (valid.isNotEmpty()) valid else listOfNotNull(allowedClasses.firstOrNull())
    }

    val pages = remember(students, settings, config, routineRows, resolvedTabClasses, allowedClasses) {
        if (resolvedTabClasses.size <= 1) {
            val singleCls = resolvedTabClasses.firstOrNull() ?: config.selectedClass
            val singleCfg = if (singleCls == config.selectedClass) config else config.copy(selectedClass = singleCls)
            AdmitCardPlanner.buildPages(
                students = students,
                settings = settings,
                config = singleCfg,
                routineRows = if (singleCls == config.selectedClass) routineRows else AdmitCardLocalStore.loadRoutine(ctx, singleCls, settings),
                allowedClassIds = allowedClasses
            )
        } else {
            AdmitCardPlanner.buildMultiClassPages(
                students = students,
                settings = settings,
                baseConfig = config,
                selectedClasses = resolvedTabClasses,
                routineProvider = { c ->
                    if (c == config.selectedClass) routineRows
                    else AdmitCardLocalStore.loadRoutine(ctx, c, settings)
                },
                examTimeProvider = { c ->
                    if (c == config.selectedClass) config.examTimeLabel
                    else AdmitCardLocalStore.loadExamTime(ctx, c, settings)
                },
                allowedClassIds = allowedClasses
            )
        }
    }

    val totalCardCount = remember(pages) { pages.sumOf { it.items.size } }
    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })

    LaunchedEffect(pages.size) {
        if (pages.isNotEmpty() && pagerState.currentPage >= pages.size) {
            pagerState.scrollToPage((pages.size - 1).coerceAtLeast(0))
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
        if (resolvedTabClasses.size > 1) {
            append("${bn(resolvedTabClasses.size.toString())}টি শ্রেণি")
        } else {
            append("${classShortBn(config.selectedClass, settings)} শ্রেণি")
        }
        append(" · ${bn(config.safeCardsPerPage.toString())} কার্ড/পৃষ্ঠা")
        append(" · ${bn(totalCardCount.toString())} জন")
    }

    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxPanelHeight = (maxHeight * 0.90f).coerceAtLeast(240.dp)
        val overlayTargetHeight = if (previewViewportHeightPx > 0) {
            with(density) { (previewViewportHeightPx * panelHeightFraction).toDp() }
                .coerceIn(170.dp, maxPanelHeight)
        } else {
            (maxHeight * panelHeightFraction).coerceIn(170.dp, maxPanelHeight)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Compact Top Status & Export Action Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Surface(color = TealSoft, shape = RoundedCornerShape(7.dp)) {
                        Icon(
                            Icons.Rounded.Badge,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.padding(5.dp).size(15.dp)
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (pages.isEmpty()) "লাইভ প্রিভিউ" else "পৃষ্ঠা ${bn((pagerState.currentPage + 1).toString())}/${bn(pages.size.toString())}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = SlateText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = summaryLine,
                            fontSize = 9.5.sp,
                            color = SlateMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { if (!aiRoutineBusy) topAiMenuOpen = true },
                            enabled = !aiRoutineBusy,
                            modifier = Modifier.size(38.dp).testTag("admit_top_ai_scan_btn")
                        ) {
                            if (aiRoutineBusy) {
                                CircularProgressIndicator(
                                    color = TealPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.AutoAwesome,
                                    contentDescription = "AI নোটিশ স্ক্যান",
                                    tint = TealPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = topAiMenuOpen,
                            onDismissRequest = { topAiMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(17.dp)) },
                                text = { Text("নোটিশের ছবি থেকে AI রুটিন", fontSize = 12.sp) },
                                onClick = {
                                    topAiMenuOpen = false
                                    openNoticeImagePicker()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(17.dp)) },
                                text = { Text("PDF / ফাইল থেকে AI রুটিন", fontSize = 12.sp) },
                                onClick = {
                                    topAiMenuOpen = false
                                    openNoticeFileOrPdfPicker()
                                }
                            )
                        }
                    }
                    IconButton(
                        onClick = { showResetConfirm = true },
                        modifier = Modifier.size(38.dp).testTag("admit_card_reset_btn")
                    ) {
                        Icon(
                            Icons.Rounded.RestartAlt,
                            contentDescription = "ডিফল্ট সেটিংস রিসেট",
                            tint = SlateMuted,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = { pendingExportAction = AdmitExportAction.PRINT },
                        enabled = allowedClasses.isNotEmpty(),
                        modifier = Modifier.size(38.dp).testTag("admit_card_print_btn")
                    ) {
                        Icon(
                            Icons.Rounded.Print,
                            contentDescription = "প্রিন্ট করুন",
                            tint = if (allowedClasses.isNotEmpty()) TealPrimary else SlateMuted.copy(alpha = 0.45f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Button(
                        onClick = { pendingExportAction = AdmitExportAction.PDF },
                        enabled = allowedClasses.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp).testTag("admit_card_export_btn")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("PDF", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                    }
                }
            }

            // 2. Main Preview Viewport + Floating Bottom Overlay Settings Panel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onSizeChanged { previewViewportHeightPx = it.height }
            ) {
                // Full-Size Live Preview (never shrinks when overlay panel opens)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (pages.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, SlateOutline),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
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
                                        modifier = Modifier.heightIn(min = 38.dp)
                                    ) {
                                        Text("১ পৃষ্ঠা ফাঁকা প্রবেশপত্র তৈরি করুন", fontSize = 11.5.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        HorizontalPager(
                            state = pagerState,
                            userScrollEnabled = previewZoomScale <= 1.02f,
                            pageSpacing = 12.dp,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("admit_preview_pager")
                        ) { pageIdx ->
                            val pageSpec = pages[pageIdx]
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clipToBounds()
                                    .onSizeChanged { previewPageBoxSize = it }
                                    .padding(horizontal = 4.dp)
                                    .pointerInput(pageIdx) {
                                        val touchSlop = viewConfiguration.touchSlop
                                        awaitEachGesture {
                                            val firstDown = awaitFirstDown(requireUnconsumed = false)
                                            var wasMultiTouch = false
                                            var movedBeyondSlop = false
                                            var upTimeMillis = firstDown.uptimeMillis
                                            var lastPointerWasUp = false
                                            do {
                                                val event = awaitPointerEvent()
                                                val pointerCount = event.changes.count { it.pressed }
                                                if (pointerCount >= 2) wasMultiTouch = true
                                                event.changes.firstOrNull { it.id == firstDown.id }?.let { ch ->
                                                    if ((ch.position - firstDown.position).getDistance() > touchSlop) {
                                                        movedBeyondSlop = true
                                                    }
                                                    if (!ch.pressed) {
                                                        upTimeMillis = ch.uptimeMillis
                                                        lastPointerWasUp = true
                                                    }
                                                }
                                                val zoomChange = event.calculateZoom()
                                                val panChange = event.calculatePan()
                                                if (pointerCount >= 2 || previewZoomScale > 1.02f) {
                                                    val nextScale = (previewZoomScale * zoomChange).coerceIn(1f, 4.5f)
                                                    if (nextScale <= 1.02f) {
                                                        previewZoomScale = 1f
                                                        previewPanOffset = Offset.Zero
                                                    } else {
                                                        previewZoomScale = nextScale
                                                        val w = previewPageBoxSize.width.toFloat().coerceAtLeast(1f)
                                                        val h = previewPageBoxSize.height.toFloat().coerceAtLeast(1f)
                                                        val maxX = (w * (nextScale - 1f)) / 2f + w * 0.15f
                                                        val maxY = (h * (nextScale - 1f)) / 2f + h * 0.48f
                                                        val nextOff = previewPanOffset + panChange
                                                        previewPanOffset = Offset(
                                                            x = nextOff.x.coerceIn(-maxX, maxX),
                                                            y = nextOff.y.coerceIn(-maxY, maxY)
                                                        )
                                                    }
                                                    if (pointerCount >= 2 || abs(zoomChange - 1f) > 0.01f || movedBeyondSlop) {
                                                        event.changes.forEach { it.consume() }
                                                    }
                                                }
                                            } while (event.changes.any { it.pressed })

                                            val tapDuration = upTimeMillis - firstDown.uptimeMillis
                                            if (!wasMultiTouch && !movedBeyondSlop && lastPointerWasUp && tapDuration < 450L) {
                                                showFullscreen = true
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                AdmitCardRenderedPage(
                                    ctx = ctx,
                                    page = pageSpec,
                                    settings = settings,
                                    signatureBitmap = signatureBitmap,
                                    renderScale = 2.4f,
                                    modifier = Modifier.graphicsLayer(
                                        scaleX = previewZoomScale,
                                        scaleY = previewZoomScale,
                                        translationX = previewPanOffset.x,
                                        translationY = previewPanOffset.y
                                    )
                                )
                                if (previewZoomScale > 1.02f) {
                                    Surface(
                                        onClick = {
                                            previewZoomScale = 1f
                                            previewPanOffset = Offset.Zero
                                        },
                                        color = Color(0xCC0F172A),
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .testTag("admit_zoom_reset_pill")
                                    ) {
                                        Text(
                                            text = "${bn(String.format(java.util.Locale.US, "%.1f", previewZoomScale))}× · রিসেট",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Compact Page Indicator & Gesture Hint Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (pages.size > 1) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                                        }
                                    },
                                    enabled = pagerState.currentPage > 0,
                                    modifier = Modifier.size(38.dp).testTag("admit_prev_page_btn")
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "আগের পৃষ্ঠা", modifier = Modifier.size(20.dp))
                                }
                            }
                            Text(
                                text = "ট্যাপে ফুল স্ক্রিন · ২ আঙুলে জুম · সোয়াইপে পৃষ্ঠা",
                                fontSize = 10.sp,
                                color = SlateMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            if (pages.size > 1) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(pages.lastIndex))
                                        }
                                    },
                                    enabled = pagerState.currentPage < pages.lastIndex,
                                    modifier = Modifier.size(38.dp).testTag("admit_next_page_btn")
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "পরের পৃষ্ঠা", modifier = Modifier.size(20.dp))
                                }
                            }
                            IconButton(
                                onClick = { showFullscreen = true },
                                modifier = Modifier.size(38.dp).testTag("admit_card_fullscreen_btn")
                            ) {
                                Icon(Icons.Rounded.Fullscreen, contentDescription = "পূর্ণ স্ক্রিন প্রিভিউ", tint = TealPrimary, modifier = Modifier.size(19.dp))
                            }
                        }
                    }
                }

                // Overlay Slide-Up Compact Options Panel (floats over bottom portion of preview)
                androidx.compose.animation.AnimatedVisibility(
                    visible = openTab != null,
                    enter = slideInVertically(tween(220)) { it } + fadeIn(tween(180)),
                    exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(140)),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    val selectedTabIdx = openTab ?: 0
                    val tabMeta = ADMIT_BOTTOM_TABS[selectedTabIdx]
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                        border = BorderStroke(1.dp, SlateOutline),
                        tonalElevation = 3.dp,
                        shadowElevation = 12.dp,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .let { baseMod ->
                                if (isCustomPanelHeight) {
                                    baseMod.height(overlayTargetHeight)
                                } else {
                                    baseMod.heightIn(max = overlayTargetHeight)
                                }
                            }
                            .testTag("admit_slide_panel")
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            // Draggable top handle & title row (drag up to expand, down to shrink/close)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(previewViewportHeightPx) {
                                        detectVerticalDragGestures(
                                            onDragStart = { isCustomPanelHeight = true },
                                            onVerticalDrag = { change, dragAmount ->
                                                change.consume()
                                                val denom = if (previewViewportHeightPx > 0) {
                                                    previewViewportHeightPx.toFloat()
                                                } else {
                                                    540f
                                                }
                                                panelHeightFraction = (panelHeightFraction - dragAmount / denom)
                                                    .coerceIn(0.18f, 0.94f)
                                            },
                                            onDragEnd = {
                                                if (panelHeightFraction < 0.24f) {
                                                    openTab = null
                                                    panelHeightFraction = 0.48f
                                                    isCustomPanelHeight = false
                                                }
                                            },
                                            onDragCancel = {
                                                if (panelHeightFraction < 0.24f) {
                                                    openTab = null
                                                    panelHeightFraction = 0.48f
                                                    isCustomPanelHeight = false
                                                }
                                            }
                                        )
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(38.dp)
                                            .height(4.dp)
                                            .background(SlateOutline, RoundedCornerShape(999.dp))
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 10.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = tabMeta.panelTitle,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = TealDark
                                        )
                                        Text(
                                            text = "উপরে টেনে বড় করুন · নিচে নামিয়ে ছোট করুন",
                                            fontSize = 8.5.sp,
                                            color = SlateMuted
                                        )
                                    }
                                    IconButton(
                                        onClick = { openTab = null },
                                        modifier = Modifier.size(36.dp).testTag("admit_panel_close_btn")
                                    ) {
                                        Icon(
                                            Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = "প্যানেল বন্ধ করুন",
                                            tint = SlateMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = SlateOutline.copy(alpha = 0.55f))

                            // Compact Scrollable Tab Body
                            androidx.compose.runtime.key(selectedTabIdx) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    when (selectedTabIdx) {
                                        0 -> AdmitClassAndRollTab(
                                            config = config,
                                            settings = settings,
                                            allowedClasses = allowedClasses,
                                            countByClass = countByClass,
                                            rollFromText = rollFromText,
                                            rollToText = rollToText,
                                            onSelectClasses = { chosenList, primaryCls ->
                                                config = config.copy(
                                                    selectedClass = primaryCls,
                                                    selectedClasses = chosenList,
                                                    examTimeLabel = AdmitCardLocalStore.loadExamTime(ctx, primaryCls, settings)
                                                )
                                                routineRows = AdmitCardLocalStore.loadRoutine(ctx, primaryCls, settings)
                                                activeBaseId = null
                                                selectedExportClasses = chosenList.toSet()
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

                                        1 -> AdmitCardRoutineEditor(
                                            config = config,
                                            settings = settings,
                                            allowedClasses = allowedClasses,
                                            baseRoutines = baseRoutines,
                                            activeBaseId = activeBaseId,
                                            classRoutineRows = routineRows,
                                            onSelectBase = { baseId -> activeBaseId = baseId },
                                            onSelectClass = { newCls ->
                                                activeBaseId = null
                                                val keptMulti = if (resolvedTabClasses.size > 1) {
                                                    (resolvedTabClasses + newCls).distinct().sortedBy { allowedClasses.indexOf(it) }
                                                } else {
                                                    listOf(newCls)
                                                }
                                                config = config.copy(
                                                    selectedClass = newCls,
                                                    selectedClasses = keptMulti,
                                                    examTimeLabel = AdmitCardLocalStore.loadExamTime(ctx, newCls, settings)
                                                )
                                                routineRows = AdmitCardLocalStore.loadRoutine(ctx, newCls, settings)
                                                selectedExportClasses = keptMulti.toSet()
                                            },
                                            onAddBaseRoutine = { newName ->
                                                val sourceBase = baseRoutines.find { it.id == activeBaseId }
                                                val seedRows = sourceBase?.rows ?: routineRows
                                                val seedTime = sourceBase?.examTimeLabel ?: config.examTimeLabel
                                                val created = BaseRoutineTemplate(
                                                    id = "base_${System.currentTimeMillis()}",
                                                    name = newName,
                                                    examTimeLabel = seedTime,
                                                    rows = seedRows.map { it.copy(id = UUID.randomUUID().toString()) }
                                                )
                                                val next = baseRoutines + created
                                                baseRoutines = next
                                                AdmitCardLocalStore.saveBaseRoutines(ctx, next)
                                                activeBaseId = created.id
                                            },
                                            onRenameBaseRoutine = { baseId, newName ->
                                                val next = baseRoutines.map { if (it.id == baseId) it.copy(name = newName) else it }
                                                baseRoutines = next
                                                AdmitCardLocalStore.saveBaseRoutines(ctx, next)
                                            },
                                            onDeleteBaseRoutine = { baseId ->
                                                if (baseRoutines.size > 1) {
                                                    val next = baseRoutines.filterNot { it.id == baseId }
                                                    baseRoutines = next
                                                    AdmitCardLocalStore.saveBaseRoutines(ctx, next)
                                                    if (activeBaseId == baseId) {
                                                        activeBaseId = next.firstOrNull()?.id
                                                    }
                                                }
                                            },
                                            onUpdateActiveExamTime = { newTime ->
                                                val currentBaseId = activeBaseId
                                                if (currentBaseId != null) {
                                                    val next = baseRoutines.map {
                                                        if (it.id == currentBaseId) it.copy(examTimeLabel = newTime) else it
                                                    }
                                                    baseRoutines = next
                                                    AdmitCardLocalStore.saveBaseRoutines(ctx, next)
                                                } else {
                                                    config = config.copy(examTimeLabel = newTime)
                                                    AdmitCardLocalStore.saveExamTime(ctx, config.selectedClass, newTime)
                                                }
                                            },
                                            onUpdateActiveRows = { updated ->
                                                val currentBaseId = activeBaseId
                                                if (currentBaseId != null) {
                                                    val next = baseRoutines.map {
                                                        if (it.id == currentBaseId) it.copy(rows = updated) else it
                                                    }
                                                    baseRoutines = next
                                                    AdmitCardLocalStore.saveBaseRoutines(ctx, next)
                                                } else {
                                                    routineRows = updated
                                                    AdmitCardLocalStore.saveRoutine(ctx, config.selectedClass, updated)
                                                }
                                            },
                                            onResetClassDefault = {
                                                val reset = AdmitCardPlanner.defaultRoutineForClass(config.selectedClass, settings)
                                                routineRows = reset
                                                AdmitCardLocalStore.saveRoutine(ctx, config.selectedClass, reset)
                                                Toast.makeText(ctx, "শ্রেণির মূল বিষয় লোড হয়েছে", Toast.LENGTH_SHORT).show()
                                            },
                                            onSaveActiveFeedback = {
                                                val currentBase = baseRoutines.find { it.id == activeBaseId }
                                                if (currentBase != null) {
                                                    AdmitCardLocalStore.saveBaseRoutines(ctx, baseRoutines)
                                                    Toast.makeText(ctx, "${currentBase.name} সংরক্ষিত হয়েছে", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    AdmitCardLocalStore.saveRoutine(ctx, config.selectedClass, routineRows)
                                                    AdmitCardLocalStore.saveExamTime(ctx, config.selectedClass, config.examTimeLabel)
                                                    Toast.makeText(
                                                        ctx,
                                                        "${classShortBn(config.selectedClass, settings)} শ্রেণির রুটিন সংরক্ষিত হয়েছে",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            onCopyToClasses = { targetClasses, matchClassSubjects, copyExamTime ->
                                                val currentBase = baseRoutines.find { it.id == activeBaseId }
                                                val sourceRows = currentBase?.rows ?: routineRows
                                                val sourceTime = currentBase?.examTimeLabel ?: config.examTimeLabel
                                                targetClasses.forEach { targetCls ->
                                                    val resolved = AdmitCardPlanner.resolveRoutineForClass(
                                                        baseRows = sourceRows,
                                                        targetCls = targetCls,
                                                        settings = settings,
                                                        matchClassSubjects = matchClassSubjects
                                                    )
                                                    AdmitCardLocalStore.saveRoutine(ctx, targetCls, resolved)
                                                    if (copyExamTime) {
                                                        AdmitCardLocalStore.saveExamTime(ctx, targetCls, sourceTime)
                                                    }
                                                    if (targetCls == config.selectedClass) {
                                                        routineRows = resolved
                                                        if (copyExamTime) {
                                                            config = config.copy(examTimeLabel = sourceTime)
                                                        }
                                                    }
                                                }
                                                Toast.makeText(
                                                    ctx,
                                                    "${bn(targetClasses.size.toString())}টি শ্রেণিতে রুটিন কপি হয়েছে",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            onUpdateConfig = { config = it },
                                            aiBusy = aiRoutineBusy,
                                            aiError = aiRoutineError,
                                            isAiConfigured = GeminiApiConfig.isConfigured(ctx),
                                            documentEnhance = aiEnhanceNotice,
                                            onToggleDocumentEnhance = { aiEnhanceNotice = it },
                                            onPickNoticeImageForAi = ::openNoticeImagePicker,
                                            onPickNoticeFileOrPdfForAi = ::openNoticeFileOrPdfPicker,
                                            onDismissAiError = { aiRoutineError = null }
                                        )

                                        2 -> AdmitSignatureAndInfoTab(
                                            config = config,
                                            settings = settings,
                                            signatureBitmap = signatureBitmap,
                                            onUpdateConfig = { config = it },
                                            onPickFromGallery = ::openGalleryPicker,
                                            onPickFromFileManager = ::openFileManagerPicker,
                                            onClearSignatureImage = {
                                                AdmitCardLocalStore.clearSignature(ctx)
                                                signatureBitmap = null
                                            }
                                        )

                                        else -> AdmitLayoutAndStyleTab(
                                            ctx = ctx,
                                            config = config,
                                            activeBnFontName = activeBnFontName,
                                            activeEnFontName = activeEnFontName,
                                            onUpdateConfig = { config = it }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Compact Fixed Bottom 4-Tab Dock Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ADMIT_BOTTOM_TABS.forEach { tab ->
                        val isSelected = openTab == tab.index
                        Surface(
                            onClick = {
                                openTab = if (isSelected) null else tab.index
                            },
                            color = if (isSelected) TealSoft else Color.Transparent,
                            border = if (isSelected) BorderStroke(1.dp, TealPrimary.copy(alpha = 0.45f)) else null,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 46.dp)
                                .testTag("admit_bottom_tab_${tab.index}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
                                    contentDescription = tab.label,
                                    tint = if (isSelected) TealDark else SlateMuted,
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = tab.label,
                                    fontSize = 9.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) TealDark else SlateMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFullscreen && pages.isNotEmpty()) {
        AdmitCardFullscreenDialog(
            ctx = ctx,
            pages = pages,
            initialPageIndex = pagerState.currentPage,
            settings = settings,
            signatureBitmap = signatureBitmap,
            onPageChanged = { newPage ->
                scope.launch {
                    if (newPage in pages.indices && pagerState.currentPage != newPage) {
                        pagerState.scrollToPage(newPage)
                    }
                }
            },
            onDismiss = { showFullscreen = false }
        )
    }

    pendingAiNotice?.let { extracted ->
        AdmitAiRoutinePreviewDialog(
            notice = extracted,
            settings = settings,
            onDismiss = { pendingAiNotice = null },
            onApply = { selection ->
                var nextConfig = config
                selection.applyTerm?.let { newTerm ->
                    nextConfig = nextConfig.copy(term = newTerm)
                }
                selection.applyFootnote?.let { note ->
                    nextConfig = nextConfig.copy(routineNote = note)
                }

                selection.classPlans.forEach { (cls, pair) ->
                    val (examTime, rows) = pair
                    AdmitCardLocalStore.saveRoutine(ctx, cls, rows)
                    if (examTime.isNotBlank()) {
                        AdmitCardLocalStore.saveExamTime(ctx, cls, examTime)
                    }
                    if (cls == nextConfig.selectedClass) {
                        routineRows = rows
                        if (examTime.isNotBlank()) {
                            nextConfig = nextConfig.copy(examTimeLabel = examTime)
                        }
                        activeBaseId = null
                    }
                }

                if (selection.saveAsBaseRoutines && selection.selectedGroupsForBase.isNotEmpty()) {
                    val existing = baseRoutines.toMutableList()
                    selection.selectedGroupsForBase.forEachIndexed { idx, grp ->
                        val label = grp.classGroupLabel.ifBlank { "AI বেস ${bn((idx + 1).toString())}" }
                        val created = BaseRoutineTemplate(
                            id = "ai_base_${System.currentTimeMillis()}_$idx",
                            name = "AI · $label",
                            examTimeLabel = grp.examTime.ifBlank { nextConfig.examTimeLabel },
                            rows = grp.rows.map { it.copy(id = UUID.randomUUID().toString()) }
                        )
                        existing.add(created)
                    }
                    baseRoutines = existing
                    AdmitCardLocalStore.saveBaseRoutines(ctx, existing)
                }

                config = nextConfig
                pendingAiNotice = null
                Toast.makeText(
                    ctx,
                    "${bn(selection.classPlans.size.toString())}টি শ্রেণির রুটিন AI থেকে বসানো হয়েছে",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    pendingExportAction?.let { exportAction ->
        AdmitMultiClassExportDialog(
            action = exportAction,
            allowedClasses = allowedClasses,
            currentClass = config.selectedClass,
            countByClass = countByClass,
            initialSelection = selectedExportClasses,
            settings = settings,
            students = students,
            config = config,
            routineProvider = { c ->
                if (c == config.selectedClass) routineRows
                else AdmitCardLocalStore.loadRoutine(ctx, c, settings)
            },
            examTimeProvider = { c ->
                if (c == config.selectedClass) config.examTimeLabel
                else AdmitCardLocalStore.loadExamTime(ctx, c, settings)
            },
            onDismiss = { pendingExportAction = null },
            onConfirm = { chosenClasses, combinedPages, fileNameOrJob ->
                selectedExportClasses = chosenClasses
                pendingExportAction = null
                if (exportAction == AdmitExportAction.PDF) {
                    onExportPdf(combinedPages, signatureBitmap, fileNameOrJob)
                } else {
                    onPrint(combinedPages, signatureBitmap, fileNameOrJob)
                }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("ডিফল্ট সেটিংস রিসেট করবেন?", fontWeight = FontWeight.Bold, fontSize = 15.5.sp) },
            text = {
                Text(
                    "প্রবেশপত্রের ফন্ট, সাইজ, বর্ডার, গ্যাপ এবং বর্তমান শ্রেণির রুটিন ডিফল্ট অবস্থায় ফিরে যাবে।",
                    fontSize = 13.sp,
                    color = SlateText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        val resetTime = AdmitCardPlanner.defaultExamTimeForClass(defaultConfig.selectedClass, settings)
                        val resetCfg = defaultConfig.copy(
                            examTimeLabel = resetTime,
                            selectedClasses = listOf(defaultConfig.selectedClass)
                        )
                        config = resetCfg
                        AdmitCardLocalStore.saveConfig(ctx, resetCfg)
                        AdmitCardLocalStore.saveExamTime(ctx, defaultConfig.selectedClass, resetTime)
                        val resetRoutine = AdmitCardPlanner.defaultRoutineForClass(defaultConfig.selectedClass, settings)
                        routineRows = resetRoutine
                        AdmitCardLocalStore.saveRoutine(ctx, defaultConfig.selectedClass, resetRoutine)
                        selectedExportClasses = setOf(defaultConfig.selectedClass)
                        activeBaseId = null
                        rollFromText = ""
                        rollToText = ""
                        previewZoomScale = 1f
                        previewPanOffset = Offset.Zero
                        Toast.makeText(ctx, "প্রবেশপত্রের সেটিংস রিসেট হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("admit_reset_confirm_btn")
                ) {
                    Text("রিসেট করুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetConfirm = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("বাতিল")
                }
            }
        )
    }
}
