@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class
)

package com.kgps.result.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.TableView
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.kgps.result.BENGALI_PDF_FONT_LIST
import com.kgps.result.ENGLISH_PDF_FONT_LIST
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.TabulationConfig
import com.kgps.result.TabulationHeaderStyle
import com.kgps.result.TabulationNumeralMode
import com.kgps.result.TabulationPageSize
import com.kgps.result.TabulationPageSpec
import com.kgps.result.TabulationPlanner
import com.kgps.result.TabulationSortOrder
import com.kgps.result.TabulationSubjectDisplay
import com.kgps.result.TabulationTermScope
import com.kgps.result.activeClassIds
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
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

@Composable
fun TabulationPdfScreen(
    students: List<Student>,
    settings: Settings,
    permissions: TeacherPermissions,
    initialClass: Int,
    onExportPdf: (pages: List<TabulationPageSpec>, fileName: String) -> Unit,
    onPrint: (pages: List<TabulationPageSpec>, jobName: String) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val availableClassIds = remember(settings.customClasses) { activeClassIds(settings) }
    val allowedClasses = remember(permissions, availableClassIds) {
        permissions.effectiveClasses(availableClassIds)
    }

    val defaultConfig = remember(initialClass, allowedClasses, settings.pdfFont, settings.pdfEnglishFont) {
        val defaultCls = when {
            initialClass in allowedClasses -> initialClass
            5 in allowedClasses -> 5
            else -> allowedClasses.firstOrNull() ?: 1
        }
        TabulationConfig(
            selectedClass = defaultCls,
            termScope = TabulationTermScope.TERM_3,
            subjectDisplay = TabulationSubjectDisplay.CONT_WR_TOT,
            headerStyle = TabulationHeaderStyle.SHORT_SUB,
            pageSize = TabulationPageSize.A4_LANDSCAPE,
            sortOrder = TabulationSortOrder.ROLL_ASC,
            numeralMode = TabulationNumeralMode.BENGALI,
            bnFontId = settings.pdfFont.ifBlank { "kalpurush" },
            enFontId = settings.pdfEnglishFont.ifBlank { "times_new_roman" },
            marginInches = 0.30f,
            rowsPerPage = 25,
            skipWrittenOnHalfSubjects = true,
            showTotal = true,
            showHighest = false,
            showPercentage = true,
            showGrade = true,
            showMeritRank = true,
            showSummaryFooter = true
        )
    }

    var config by remember(defaultConfig) { mutableStateOf(defaultConfig) }
    var settingsExpanded by remember { mutableStateOf(false) }
    var showFullscreen by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var panelHeightFraction by remember { mutableFloatStateOf(0.50f) }
    var isCustomPanelHeight by remember { mutableStateOf(false) }
    var previewViewportHeightPx by remember { mutableIntStateOf(0) }
    var previewZoomScale by remember { mutableFloatStateOf(1f) }
    var previewPanOffset by remember { mutableStateOf(Offset.Zero) }
    var previewPageBoxSize by remember { mutableStateOf(IntSize.Zero) }

    // Back priority: zoomed preview resets first -> open panel closes -> exit screen
    if (onBack != null) {
        BackHandler(enabled = !showFullscreen && !settingsExpanded && previewZoomScale <= 1.02f) { onBack() }
    }
    BackHandler(enabled = !showFullscreen && settingsExpanded && previewZoomScale <= 1.02f) {
        settingsExpanded = false
    }
    BackHandler(enabled = !showFullscreen && previewZoomScale > 1.02f) {
        previewZoomScale = 1f
        previewPanOffset = Offset.Zero
    }

    val pages = remember(students, settings, config, allowedClasses) {
        TabulationPlanner.buildPages(
            students = students,
            settings = settings,
            config = config,
            allowedClassIds = allowedClasses
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })
    LaunchedEffect(pages.size) {
        if (pages.isNotEmpty() && pagerState.currentPage >= pages.size) {
            pagerState.scrollToPage((pages.size - 1).coerceAtLeast(0))
        }
    }
    LaunchedEffect(pagerState.currentPage, pages.size, config.pageSize) {
        previewZoomScale = 1f
        previewPanOffset = Offset.Zero
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

    val totalStudentCount = countByClass[config.selectedClass] ?: 0
    val summaryLine = buildString {
        append("${classShortBn(config.selectedClass, settings)} (${bn(totalStudentCount.toString())})")
        append(" · ${config.termScope.shortLabelBn}")
        append(" · ${config.pageSize.labelBn}")
        append(" · ${bn(pages.size.toString())} পৃষ্ঠা")
    }
    val exportName = "ফলাফল-টেবিল-${classShortBn(config.selectedClass, settings)}-${config.termScope.shortLabelBn}.pdf"

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
                shape = RoundedCornerShape(9.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Surface(color = TealSoft, shape = RoundedCornerShape(7.dp)) {
                        Icon(
                            Icons.Rounded.TableView,
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
                    IconButton(
                        onClick = { showResetConfirm = true },
                        modifier = Modifier.size(38.dp).testTag("tabulation_reset_btn")
                    ) {
                        Icon(
                            Icons.Rounded.RestartAlt,
                            contentDescription = "ডিফল্ট সেটিংস রিসেট",
                            tint = SlateMuted,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = { onPrint(pages, "ফলাফল টেবিল (${classShortBn(config.selectedClass, settings)})") },
                        enabled = pages.isNotEmpty(),
                        modifier = Modifier.size(38.dp).testTag("tabulation_print_btn")
                    ) {
                        Icon(
                            Icons.Rounded.Print,
                            contentDescription = "প্রিন্ট করুন",
                            tint = if (pages.isNotEmpty()) TealPrimary else SlateMuted.copy(alpha = 0.45f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Button(
                        onClick = { onExportPdf(pages, exportName) },
                        enabled = pages.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp).testTag("tabulation_export_btn")
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
                        .padding(top = 1.dp, bottom = 2.dp),
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
                                Text(
                                    "নির্বাচিত শ্রেণিতে কোনো শিক্ষার্থী পাওয়া যায়নি।",
                                    fontSize = 12.5.sp,
                                    color = SlateMuted,
                                    modifier = Modifier.padding(18.dp)
                                )
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
                                .testTag("tabulation_preview_pager")
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
                                contentAlignment = Alignment.TopCenter
                            ) {
                                TabulationRenderedPage(
                                    ctx = ctx,
                                    page = pageSpec,
                                    settings = settings,
                                    pageSize = config.pageSize,
                                    renderScale = 2.2f,
                                    pageAlignment = Alignment.TopCenter,
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
                                            .testTag("tabulation_zoom_reset_pill")
                                    ) {
                                        Text(
                                            text = "${bn(String.format(Locale.US, "%.1f", previewZoomScale))}× · রিসেট",
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
                                    modifier = Modifier.size(38.dp).testTag("tabulation_prev_page_btn")
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
                                    modifier = Modifier.size(38.dp).testTag("tabulation_next_page_btn")
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "পরের পৃষ্ঠা", modifier = Modifier.size(20.dp))
                                }
                            }
                            IconButton(
                                onClick = { showFullscreen = true },
                                modifier = Modifier.size(38.dp).testTag("tabulation_fullscreen_btn")
                            ) {
                                Icon(Icons.Rounded.Fullscreen, contentDescription = "পূর্ণ স্ক্রিন প্রিভিউ", tint = TealPrimary, modifier = Modifier.size(19.dp))
                            }
                        }
                    }
                }

                // Overlay Slide-Up Compact Options Panel (floats over bottom portion of preview)
                androidx.compose.animation.AnimatedVisibility(
                    visible = settingsExpanded,
                    enter = slideInVertically(tween(220)) { it } + fadeIn(tween(180)),
                    exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(140)),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
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
                            .testTag("tabulation_slide_panel")
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            // Draggable top handle & title row
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
                                                    settingsExpanded = false
                                                    panelHeightFraction = 0.50f
                                                    isCustomPanelHeight = false
                                                }
                                            },
                                            onDragCancel = {
                                                if (panelHeightFraction < 0.24f) {
                                                    settingsExpanded = false
                                                    panelHeightFraction = 0.50f
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
                                            text = "ফলাফল টেবিল সেটিংস",
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
                                        onClick = { showResetConfirm = true },
                                        modifier = Modifier.size(48.dp).testTag("tabulation_panel_reset_btn")
                                    ) {
                                        Icon(
                                            Icons.Rounded.RestartAlt,
                                            contentDescription = "ডিফল্ট সেটিংস রিসেট",
                                            tint = SlateMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { settingsExpanded = false },
                                        modifier = Modifier.size(48.dp).testTag("tabulation_panel_close_btn")
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

                            // Unified Compact Scrollable Settings Body
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                TabulationUnifiedSettingsPanel(
                                    config = config,
                                    settings = settings,
                                    allowedClasses = allowedClasses,
                                    countByClass = countByClass,
                                    activeBnFontName = activeBnFontName,
                                    activeEnFontName = activeEnFontName,
                                    onUpdateConfig = { config = it },
                                    onResetPageIndex = {
                                        scope.launch { pagerState.scrollToPage(0) }
                                    },
                                    onRequestReset = { showResetConfirm = true }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Single Compact Bottom Settings Toggle Tab/Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (settingsExpanded) TealPrimary.copy(alpha = 0.45f) else SlateOutline
                ),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Surface(
                    onClick = { settingsExpanded = !settingsExpanded },
                    color = if (settingsExpanded) TealSoft.copy(alpha = 0.65f) else Color.Transparent,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("tabulation_setup_toggle")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = if (settingsExpanded) TealPrimary else TealSoft,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = if (settingsExpanded) Color.White else TealDark,
                                modifier = Modifier.padding(6.dp).size(17.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ফলাফল টেবিল সেটিংস",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (settingsExpanded) TealDark else SlateText,
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
                        Surface(
                            color = if (settingsExpanded) Color.White else TealSoft,
                            border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (settingsExpanded) "বন্ধ" else "সেটিংস",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealDark
                                )
                                Icon(
                                    imageVector = if (settingsExpanded) {
                                        Icons.Rounded.KeyboardArrowDown
                                    } else {
                                        Icons.Rounded.KeyboardArrowUp
                                    },
                                    contentDescription = if (settingsExpanded) "সেটিংস বন্ধ করুন" else "সেটিংস খুলুন",
                                    tint = TealDark,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFullscreen && pages.isNotEmpty()) {
        TabulationFullscreenDialog(
            ctx = ctx,
            pages = pages,
            initialPageIndex = pagerState.currentPage,
            settings = settings,
            pageSize = config.pageSize,
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

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("ডিফল্ট সেটিংস রিসেট করবেন?", fontWeight = FontWeight.Bold, fontSize = 15.5.sp) },
            text = {
                Text(
                    "ফলাফল টেবিলের কলাম, পৃষ্ঠা, মার্জিন ও ফন্ট সেটিংস ডিফল্ট অবস্থায় ফিরে যাবে।",
                    fontSize = 13.sp,
                    color = SlateText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        config = defaultConfig
                        previewZoomScale = 1f
                        previewPanOffset = Offset.Zero
                        scope.launch { pagerState.scrollToPage(0) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("tabulation_reset_confirm_btn")
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
