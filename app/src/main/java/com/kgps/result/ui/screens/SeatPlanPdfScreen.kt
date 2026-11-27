@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.kgps.result.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EventSeat
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.RestartAlt
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.kgps.result.BENGALI_PDF_FONT_LIST
import com.kgps.result.ENGLISH_PDF_FONT_LIST
import com.kgps.result.SeatPlanConfig
import com.kgps.result.SeatPlanLocalStore
import com.kgps.result.SeatPlanPageSpec
import com.kgps.result.SeatPlanPlanner
import com.kgps.result.SeatPlanRollOrder
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.activeClassIds
import com.kgps.result.bn
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

private data class SeatPlanBottomTabItem(
    val index: Int,
    val label: String,
    val panelTitle: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
)

private val SEAT_PLAN_BOTTOM_TABS = listOf(
    SeatPlanBottomTabItem(0, "শ্রেণি ও রোল", "আসন মোড, শ্রেণি ও রোল", Icons.Rounded.Groups, Icons.Outlined.Groups),
    SeatPlanBottomTabItem(1, "শিরোনাম ও কক্ষ", "মূল্যায়ন, শিরোনাম ও কক্ষ", Icons.Rounded.Badge, Icons.Outlined.Badge),
    SeatPlanBottomTabItem(2, "পেজ ও বর্ডার", "পৃষ্ঠা, গ্রিড ও বর্ডার", Icons.Rounded.GridView, Icons.Outlined.GridView),
    SeatPlanBottomTabItem(3, "ফন্ট ও সাইজ", "বাংলা/ইংরেজি ফন্ট ও সাইজ", Icons.Rounded.FormatSize, Icons.Outlined.FormatSize)
)

@Composable
fun SeatPlanPdfScreen(
    students: List<Student>,
    settings: Settings,
    permissions: TeacherPermissions,
    initialClass: Int,
    onExportPdf: (pages: List<SeatPlanPageSpec>, fileName: String) -> Unit,
    onPrint: (pages: List<SeatPlanPageSpec>, jobName: String) -> Unit,
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
        val leftDefault = allowedClasses.firstOrNull { it != defaultCls } ?: defaultCls
        SeatPlanConfig(
            selectedClasses = listOf(defaultCls),
            leftClass = leftDefault,
            rightClass = defaultCls,
            term = 3,
            columns = 2,
            rows = 6,
            marginInches = 0.40f,
            horizontalGapPt = 12f,
            verticalGapPt = 10f,
            bnFontId = "kalpurush",
            enFontId = settings.pdfEnglishFont.ifBlank { "times_new_roman" },
            schoolFontPt = 12.0f,
            headerMetaFontPt = 10.5f,
            studentNameFontPt = 11.5f,
            classRollFontPt = 11.0f,
            cornerRadiusPt = 18f,
            rollOrder = SeatPlanRollOrder.DESCENDING
        )
    }

    var config by remember(defaultConfig) {
        val loaded = SeatPlanLocalStore.loadConfig(ctx, defaultConfig)
        mutableStateOf(
            loaded.copy(
                selectedClasses = defaultConfig.selectedClasses,
                leftClass = defaultConfig.leftClass,
                rightClass = defaultConfig.rightClass
            )
        )
    }
    LaunchedEffect(config) {
        SeatPlanLocalStore.saveConfig(ctx, config)
    }

    var openTab by remember { mutableStateOf<Int?>(null) }
    var rollFromText by remember { mutableStateOf("") }
    var rollToText by remember { mutableStateOf("") }
    var showFullscreen by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var pendingExportAction by remember { mutableStateOf<AdmitExportAction?>(null) }
    var panelHeightFraction by remember { mutableFloatStateOf(0.48f) }
    var isCustomPanelHeight by remember { mutableStateOf(false) }
    var previewViewportHeightPx by remember { mutableIntStateOf(0) }
    var previewZoomScale by remember { mutableFloatStateOf(1f) }
    var previewPanOffset by remember { mutableStateOf(Offset.Zero) }
    var previewPageBoxSize by remember { mutableStateOf(IntSize.Zero) }

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

    val pages = remember(students, settings, config, allowedClasses) {
        SeatPlanPlanner.buildPages(
            students = students,
            settings = settings,
            config = config,
            allowedClassIds = allowedClasses
        )
    }

    val totalSlipCount = remember(pages) {
        pages.sumOf { p -> p.slips.count { it != null } }
    }

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
        append("${bn(config.safeColumns.toString())}×${bn(config.safeRows.toString())}")
        append(" · $activeBnFontName")
        append(" · ${bn(totalSlipCount.toString())} কার্ড")
    }

    val exportName = remember(config.roomLabel) {
        buildString {
            append("আসন-বিন্যাস")
            if (config.roomLabel.isNotBlank()) {
                append("-").append(config.roomLabel.trim().replace(" ", "-"))
            }
            append(".pdf")
        }
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
                            Icons.Rounded.EventSeat,
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
                        modifier = Modifier.size(38.dp).testTag("seat_plan_reset_btn")
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
                        modifier = Modifier.size(38.dp).testTag("seat_plan_print_btn")
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
                        modifier = Modifier.height(34.dp).testTag("seat_plan_export_btn")
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
                                        onClick = { config = config.copy(extraBlankSlips = config.slipsPerPage.coerceAtMost(48)) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.heightIn(min = 38.dp)
                                    ) {
                                        Text("১ পৃষ্ঠা ফাঁকা সিট কার্ড তৈরি করুন", fontSize = 11.5.sp)
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
                                .testTag("seat_plan_preview_pager")
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
                                SeatPlanRenderedPage(
                                    ctx = ctx,
                                    page = pageSpec,
                                    settings = settings,
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
                                            .testTag("seat_plan_zoom_reset_pill")
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
                                    modifier = Modifier.size(38.dp).testTag("seat_plan_prev_page_btn")
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
                                    modifier = Modifier.size(38.dp).testTag("seat_plan_next_page_btn")
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "পরের পৃষ্ঠা", modifier = Modifier.size(20.dp))
                                }
                            }
                            IconButton(
                                onClick = { showFullscreen = true },
                                modifier = Modifier.size(38.dp).testTag("seat_plan_fullscreen_btn")
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
                    val tabMeta = SEAT_PLAN_BOTTOM_TABS[selectedTabIdx]
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
                            .testTag("seat_plan_slide_panel")
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
                                        modifier = Modifier.size(36.dp).testTag("seat_plan_panel_close_btn")
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
                                        0 -> SeatPlanClassAndRollTab(
                                            config = config,
                                            settings = settings,
                                            allowedClasses = allowedClasses,
                                            countByClass = countByClass,
                                            rollFromText = rollFromText,
                                            rollToText = rollToText,
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

                                        1 -> SeatPlanTitleAndRoomTab(
                                            config = config,
                                            settings = settings,
                                            onUpdateConfig = { config = it }
                                        )

                                        2 -> SeatPlanLayoutAndBorderTab(
                                            config = config,
                                            onUpdateConfig = { config = it }
                                        )

                                        else -> SeatPlanFontAndStyleTab(
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
                    SEAT_PLAN_BOTTOM_TABS.forEach { tab ->
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
                                .testTag("seat_plan_bottom_tab_${tab.index}")
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
        SeatPlanFullscreenDialog(
            ctx = ctx,
            pages = pages,
            initialPageIndex = pagerState.currentPage,
            settings = settings,
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

    pendingExportAction?.let { exportAction ->
        SeatPlanMultiClassExportDialog(
            action = exportAction,
            allowedClasses = allowedClasses,
            countByClass = countByClass,
            settings = settings,
            students = students,
            config = config,
            onDismiss = { pendingExportAction = null },
            onConfirm = { updatedCfg, exportPages, fileNameOrJob ->
                config = updatedCfg
                pendingExportAction = null
                if (exportAction == AdmitExportAction.PDF) {
                    onExportPdf(exportPages, fileNameOrJob)
                } else {
                    onPrint(exportPages, fileNameOrJob)
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
                    "আসন বিন্যাসের ফন্ট, সাইজ, বর্ডার, কলাম-সারি ও অবস্থান সেটিংস ডিফল্ট অবস্থায় ফিরে যাবে।",
                    fontSize = 13.sp,
                    color = SlateText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        config = defaultConfig
                        SeatPlanLocalStore.saveConfig(ctx, defaultConfig)
                        rollFromText = ""
                        rollToText = ""
                        previewZoomScale = 1f
                        previewPanOffset = Offset.Zero
                        Toast.makeText(ctx, "আসন বিন্যাসের সেটিংস রিসেট হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("seat_plan_reset_confirm_btn")
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
