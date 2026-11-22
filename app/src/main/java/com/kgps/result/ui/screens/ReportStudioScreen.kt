@file:OptIn(ExperimentalMaterial3Api::class)

package com.kgps.result.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.kgps.result.DPE_LOGO_LIST
import com.kgps.result.PDF_FONT_LIST
import com.kgps.result.PdfGen
import com.kgps.result.ReportSummaryColumn
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.bn
import com.kgps.result.classNameBn
import com.kgps.result.fromBn
import com.kgps.result.ui.components.BatchReportDialog
import com.kgps.result.ui.components.SummaryTableConfigCard
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealContainer
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft
import com.kgps.result.ui.theme.appFilterChipBorder
import com.kgps.result.ui.theme.appFilterChipColors
import kotlin.math.abs
import kotlin.math.roundToInt

private fun clampPanOffset(offset: Offset, scale: Float, size: IntSize): Offset {
    if (scale <= 1.02f || size.width <= 0 || size.height <= 0) return Offset.Zero
    val maxX = (size.width * (scale - 1f)) / 2f
    val maxY = (size.height * (scale - 1f)) / 2f + size.height * 0.25f
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY)
    )
}

@Composable
fun ReportStudioScreen(
    students: List<Student>,
    allSchoolStudents: List<Student> = students,
    settings: Settings,
    selectedId: String?,
    includeClassHighest: Boolean = false,
    onToggleClassHighest: (Boolean) -> Unit = {},
    includeSummaryTable: Boolean = false,
    summaryColumns: List<ReportSummaryColumn> = ReportSummaryColumn.DEFAULT_SELECTED,
    onToggleSummaryTable: (Boolean) -> Unit = {},
    onUpdateSummaryColumns: (List<ReportSummaryColumn>) -> Unit = {},
    onSelectId: (String) -> Unit,
    onUpdateSettings: (Settings) -> Unit,
    showCoverAddress: Boolean = true,
    onToggleCoverAddress: (Boolean) -> Unit = {},
    onSaveCoverAddress: (String) -> Unit = { addr -> onUpdateSettings(settings.copy(schoolAddress = addr)) },
    onPrintStudent: (Student) -> Unit,
    onExportPdf: (Student) -> Unit,
    onPrintBatch: (List<Student>, String) -> Unit = { list, _ -> list.firstOrNull()?.let(onPrintStudent) },
    onExportBatchPdf: (List<Student>, String) -> Unit = { list, _ -> list.firstOrNull()?.let(onExportPdf) },
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

    var pageView by remember { mutableStateOf("p1") }
    var lastSinglePage by remember { mutableStateOf("p1") }
    val selectPage: (String) -> Unit = { target ->
        if (target != "both") lastSinglePage = target
        pageView = target
    }
    var showFoldGuide by remember { mutableStateOf(true) }
    var classMenuOpen by remember { mutableStateOf(false) }
    var rollMenuOpen by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showCustomizeSheet by remember { mutableStateOf(false) }
    var showFullscreenPreview by remember { mutableStateOf(false) }
    var showBatchDialog by remember { mutableStateOf(false) }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(pageView, active?.id) {
        zoomScale = 1f
        panOffset = Offset.Zero
    }

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

    val availableClasses = remember(sorted) {
        sorted.map { it.cls }.distinct().sorted()
    }
    val classStudents = remember(sorted, active.cls) {
        sorted.filter { it.cls == active.cls }
    }
    val classActiveIdx = remember(classStudents, active.id) {
        classStudents.indexOfFirst { it.id == active.id }.coerceAtLeast(0)
    }

    val coverBmp = remember(active, settings, showFoldGuide, showCoverAddress) {
        PdfGen.renderCoverBitmap(
            ctx = ctx,
            s = active,
            st = settings,
            showFoldGuide = showFoldGuide,
            scale = 2.4f,
            showCoverAddress = showCoverAddress
        )
    }
    val marksBmp = remember(
        active,
        settings,
        showFoldGuide,
        allSchoolStudents,
        includeClassHighest,
        includeSummaryTable,
        summaryColumns
    ) {
        PdfGen.renderMarksBitmap(
            ctx,
            active,
            settings,
            showFoldGuide = showFoldGuide,
            scale = 2.4f,
            allStudents = allSchoolStudents,
            showClassHighest = includeClassHighest,
            showSummaryTable = includeSummaryTable,
            summaryColumns = summaryColumns
        )
    }

    // Crop the two landscape A4 sheets into the 4 individual A5 pages
    val page4BackBmp = remember(coverBmp) {
        val half = coverBmp.width / 2
        Bitmap.createBitmap(coverBmp, 0, 0, half, coverBmp.height)
    }
    val page1FrontBmp = remember(coverBmp) {
        val half = coverBmp.width / 2
        Bitmap.createBitmap(coverBmp, half, 0, coverBmp.width - half, coverBmp.height)
    }
    val page2MarksBmp = remember(marksBmp) {
        val half = marksBmp.width / 2
        Bitmap.createBitmap(marksBmp, 0, 0, half, marksBmp.height)
    }
    val page3SummaryBmp = remember(marksBmp) {
        val half = marksBmp.width / 2
        Bitmap.createBitmap(marksBmp, half, 0, marksBmp.width - half, marksBmp.height)
    }

    val singlePageRatio = (PdfGen.W / 2f) / PdfGen.H.toFloat()
    val fullSheetRatio = PdfGen.W.toFloat() / PdfGen.H.toFloat()

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            Modifier.widthIn(max = 920.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Top Page Selector Chips Row (swapped to top above preview)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(9.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp, vertical = 5.dp)
                ) {
                    listOf(
                        "p1" to "১. কভার",
                        "p2" to "২. ১ম-২য় ও গুণাবলি",
                        "p3" to "৩. ৩য় ও চূড়ান্ত",
                        "p4" to "৪. ব্যাক কভার",
                        "both" to "পুরো শিট (২ পাতা)"
                    ).forEach { (id, label) ->
                        val selected = pageView == id
                        Surface(
                            onClick = { selectPage(id) },
                            shape = RoundedCornerShape(7.dp),
                            color = if (selected) TealPrimary else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (selected) TealPrimary else SlateOutline),
                            modifier = Modifier.height(30.dp).testTag("studio_page_chip_$id")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) Color.White else SlateText,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // Central Zoomable & Pannable Preview Area
            Card(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateBg),
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.fillMaxSize()) {
                    // Slim Zoom Control Strip
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val pageCaption = when (pageView) {
                                "p1" -> "পৃষ্ঠা ১/৪"
                                "p2" -> "পৃষ্ঠা ২/৪"
                                "p3" -> "পৃষ্ঠা ৩/৪"
                                "p4" -> "পৃষ্ঠা ৪/৪"
                                else -> "পুরো শিট"
                            }
                            Text(
                                pageCaption,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = SlateMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Surface(
                                onClick = { onToggleClassHighest(!includeClassHighest) },
                                shape = RoundedCornerShape(6.dp),
                                color = if (includeClassHighest) TealPrimary else SlateBg,
                                border = BorderStroke(1.dp, if (includeClassHighest) TealPrimary else SlateOutline),
                                modifier = Modifier.height(28.dp).testTag("studio_toggle_class_highest_chip")
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (includeClassHighest) "✓ সর্বোচ্চ" else "+ সর্বোচ্চ",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (includeClassHighest) Color.White else SlateText,
                                        maxLines = 1
                                    )
                                }
                            }

                            Surface(
                                onClick = { onToggleSummaryTable(!includeSummaryTable) },
                                shape = RoundedCornerShape(6.dp),
                                color = if (includeSummaryTable) TealPrimary else SlateBg,
                                border = BorderStroke(1.dp, if (includeSummaryTable) TealPrimary else SlateOutline),
                                modifier = Modifier.height(28.dp).testTag("studio_toggle_summary_chip")
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (includeSummaryTable) "✓ সারাংশ" else "+ সারাংশ",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (includeSummaryTable) Color.White else SlateText,
                                        maxLines = 1
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    val next = (zoomScale - 0.5f).coerceAtLeast(1f)
                                    zoomScale = next
                                    panOffset = clampPanOffset(panOffset, next, previewSize)
                                },
                                enabled = zoomScale > 1.02f,
                                shape = RoundedCornerShape(6.dp),
                                color = SlateBg,
                                border = BorderStroke(1.dp, SlateOutline),
                                modifier = Modifier.size(28.dp).testTag("studio_zoom_out_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.Remove,
                                        contentDescription = "জুম আউট",
                                        tint = if (zoomScale > 1.02f) SlateText else SlateOutline,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = if (zoomScale > 1.02f) TealSoft else SlateBg,
                                border = BorderStroke(1.dp, if (zoomScale > 1.02f) TealContainer else SlateOutline),
                                modifier = Modifier.height(28.dp).testTag("studio_reset_zoom_btn")
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${bn((zoomScale * 100).roundToInt().toString())}%",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (zoomScale > 1.02f) TealPrimary else SlateText
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    val next = (zoomScale + 0.5f).coerceAtMost(4f)
                                    zoomScale = next
                                    panOffset = clampPanOffset(panOffset, next, previewSize)
                                },
                                enabled = zoomScale < 3.98f,
                                shape = RoundedCornerShape(6.dp),
                                color = SlateBg,
                                border = BorderStroke(1.dp, SlateOutline),
                                modifier = Modifier.size(28.dp).testTag("studio_zoom_in_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.Add,
                                        contentDescription = "জুম ইন",
                                        tint = if (zoomScale < 3.98f) SlateText else SlateOutline,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Surface(
                                onClick = { showFullscreenPreview = true },
                                shape = RoundedCornerShape(6.dp),
                                color = TealSoft,
                                border = BorderStroke(1.dp, TealContainer),
                                modifier = Modifier.size(28.dp).testTag("studio_fullscreen_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.Fullscreen,
                                        contentDescription = "ফুলস্ক্রিন বড় করে দেখুন",
                                        tint = TealPrimary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = SlateOutline)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clipToBounds()
                            .onSizeChanged { previewSize = it }
                            .pointerInput(pageView, active.id, previewSize) {
                                val pageOrder = listOf("p1", "p2", "p3", "p4")
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    var swipeDx = 0f
                                    var swipeDy = 0f
                                    var swipeTriggered = false
                                    do {
                                        val event = awaitPointerEvent()
                                        val pressedCount = event.changes.count { it.pressed }
                                        if (pressedCount >= 2) {
                                            swipeTriggered = true // suppress swipe on multi-touch
                                            val zoomChange = event.calculateZoom()
                                            val panChange = event.calculatePan()
                                            if (zoomChange != 1f || panChange != Offset.Zero) {
                                                val newScale = (zoomScale * zoomChange).coerceIn(1f, 4f)
                                                zoomScale = newScale
                                                panOffset = clampPanOffset(panOffset + panChange, newScale, previewSize)
                                                event.changes.forEach { it.consume() }
                                            }
                                        } else if (zoomScale > 1.02f && pressedCount == 1) {
                                            val panChange = event.calculatePan()
                                            if (panChange != Offset.Zero) {
                                                panOffset = clampPanOffset(panOffset + panChange, zoomScale, previewSize)
                                                event.changes.forEach { it.consume() }
                                            }
                                        } else if (!swipeTriggered && pressedCount == 1) {
                                            val panChange = event.calculatePan()
                                            swipeDx += panChange.x
                                            swipeDy += panChange.y
                                            if (abs(swipeDx) > 75f && abs(swipeDx) > abs(swipeDy) * 1.4f) {
                                                swipeTriggered = true
                                                event.changes.forEach { it.consume() }
                                                if (swipeDx < 0f) {
                                                    // Swipe left -> Next page
                                                    val nextPage = if (pageView == "both") "p1" else {
                                                        val idx = pageOrder.indexOf(pageView).coerceAtLeast(0)
                                                        pageOrder[(idx + 1).coerceAtMost(pageOrder.lastIndex)]
                                                    }
                                                    selectPage(nextPage)
                                                } else {
                                                    // Swipe right -> Previous page
                                                    val prevPage = if (pageView == "both") "p4" else {
                                                        val idx = pageOrder.indexOf(pageView).coerceAtLeast(0)
                                                        pageOrder[(idx - 1).coerceAtLeast(0)]
                                                    }
                                                    selectPage(prevPage)
                                                }
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            }
                            .pointerInput(pageView, active.id) {
                                detectTapGestures(
                                    onTap = {
                                        showFullscreenPreview = true
                                    },
                                    onDoubleTap = {
                                        if (pageView == "both") {
                                            selectPage(lastSinglePage)
                                        } else {
                                            lastSinglePage = pageView
                                            pageView = "both"
                                        }
                                    }
                                )
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(6.dp)
                                .graphicsLayer {
                                    scaleX = zoomScale
                                    scaleY = zoomScale
                                    translationX = panOffset.x
                                    translationY = panOffset.y
                                },
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            when (pageView) {
                                "p1" -> SingleReportPageCard(
                                    bitmap = page1FrontBmp,
                                    ratio = singlePageRatio,
                                    description = "পৃষ্ঠা ১: ফ্রন্ট কভার প্রিভিউ"
                                )
                                "p2" -> SingleReportPageCard(
                                    bitmap = page2MarksBmp,
                                    ratio = singlePageRatio,
                                    description = "পৃষ্ঠা ২: ১ম ও ২য় প্রান্তিক এবং গুণাবলি প্রিভিউ"
                                )
                                "p3" -> SingleReportPageCard(
                                    bitmap = page3SummaryBmp,
                                    ratio = singlePageRatio,
                                    description = "পৃষ্ঠা ৩: ৩য় প্রান্তিক, চূড়ান্ত ফলাফল ও উপস্থিতি প্রিভিউ"
                                )
                                "p4" -> SingleReportPageCard(
                                    bitmap = page4BackBmp,
                                    ratio = singlePageRatio,
                                    description = "পৃষ্ঠা ৪: ব্যাক কভার প্রিভিউ"
                                )
                                else -> {
                                    SheetOverviewCard(
                                        title = "শিট-১ (বাম: পাতা-৪ ব্যাক কভার · ডান: পাতা-১ ফ্রন্ট কভার)",
                                        bitmap = coverBmp,
                                        ratio = fullSheetRatio,
                                        onOpenLeft = { selectPage("p4") },
                                        leftLabel = "পাতা-৪ বড় করুন",
                                        onOpenRight = { selectPage("p1") },
                                        rightLabel = "পাতা-১ বড় করুন"
                                    )
                                    SheetOverviewCard(
                                        title = "শিট-২ (বাম: পাতা-২ নম্বর ও গুণাবলি · ডান: পাতা-৩ চূড়ান্ত ও উপস্থিতি)",
                                        bitmap = marksBmp,
                                        ratio = fullSheetRatio,
                                        onOpenLeft = { selectPage("p2") },
                                        leftLabel = "পাতা-২ বড় করুন",
                                        onOpenRight = { selectPage("p3") },
                                        rightLabel = "পাতা-৩ বড় করুন"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Compact Single-Row Selection Bar (swapped below preview, right above Print/PDF bar): Class + Roll + Name(Search) + Prev/Next
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Class Selector (no prefix/suffix, e.g. ৩য়)
                    Box {
                        Surface(
                            onClick = { classMenuOpen = true },
                            shape = RoundedCornerShape(7.dp),
                            color = TealSoft,
                            border = BorderStroke(1.dp, TealContainer),
                            modifier = Modifier.height(34.dp).testTag("studio_class_dropdown")
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 7.dp, end = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    classNameBn(active.cls, settings),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealPrimary,
                                    maxLines = 1
                                )
                                Icon(
                                    Icons.Rounded.ArrowDropDown,
                                    contentDescription = "শ্রেণি পরিবর্তন",
                                    tint = TealPrimary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                        DropdownMenu(expanded = classMenuOpen, onDismissRequest = { classMenuOpen = false }) {
                            availableClasses.forEach { c ->
                                val count = sorted.count { it.cls == c }
                                val isSel = c == active.cls
                                DropdownMenuItem(
                                    modifier = Modifier.background(if (isSel) TealContainer else Color.Transparent),
                                    text = {
                                        Text(
                                            "${classNameBn(c, settings)} শ্রেণি (${bn(count.toString())} জন)",
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) TealDark else SlateText
                                        )
                                    },
                                    trailingIcon = if (isSel) {
                                        { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    onClick = {
                                        classMenuOpen = false
                                        sorted.firstOrNull { it.cls == c }?.let { onSelectId(it.id) }
                                    }
                                )
                            }
                        }
                    }

                    // Roll Selector (no prefix, e.g. ০১)
                    Box {
                        Surface(
                            onClick = { rollMenuOpen = true },
                            shape = RoundedCornerShape(7.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, SlateOutline),
                            modifier = Modifier.height(34.dp).testTag("studio_roll_dropdown")
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 7.dp, end = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    bn(active.roll.toString().padStart(2, '0')),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateText,
                                    maxLines = 1
                                )
                                Icon(
                                    Icons.Rounded.ArrowDropDown,
                                    contentDescription = "রোল পরিবর্তন",
                                    tint = SlateMuted,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = rollMenuOpen,
                            onDismissRequest = { rollMenuOpen = false },
                            modifier = Modifier.heightIn(max = 360.dp)
                        ) {
                            classStudents.forEach { s ->
                                val isSel = s.id == active.id
                                DropdownMenuItem(
                                    modifier = Modifier.background(if (isSel) TealContainer else Color.Transparent),
                                    text = {
                                        Text(
                                            "${bn(s.roll.toString().padStart(2, '0'))} — ${s.name.ifBlank { "(নাম নেই)" }}",
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) TealDark else SlateText
                                        )
                                    },
                                    trailingIcon = if (isSel) {
                                        { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    onClick = {
                                        onSelectId(s.id)
                                        rollMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    // Student Name + Search Trigger right beside Class & Roll
                    Surface(
                        onClick = { showSearchDialog = true },
                        shape = RoundedCornerShape(7.dp),
                        color = SlateBg,
                        border = BorderStroke(1.dp, SlateOutline),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .testTag("studio_student_selector")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                active.name.ifBlank { "(নাম নেই)" },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SlateText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = "নাম বা রোল দিয়ে খুঁজুন",
                                tint = TealPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            if (classActiveIdx > 0) onSelectId(classStudents[classActiveIdx - 1].id)
                        },
                        enabled = classActiveIdx > 0,
                        shape = RoundedCornerShape(7.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, SlateOutline),
                        modifier = Modifier.size(34.dp).testTag("studio_prev_student_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "আগের শিক্ষার্থী",
                                tint = if (classActiveIdx > 0) SlateText else SlateOutline
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            if (classActiveIdx < classStudents.lastIndex) onSelectId(classStudents[classActiveIdx + 1].id)
                        },
                        enabled = classActiveIdx < classStudents.lastIndex,
                        shape = RoundedCornerShape(7.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, SlateOutline),
                        modifier = Modifier.size(34.dp).testTag("studio_next_student_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "পরের শিক্ষার্থী",
                                tint = if (classActiveIdx < classStudents.lastIndex) SlateText else SlateOutline
                            )
                        }
                    }
                }
            }

            // Bottom Compact Action Bar: Customize + Batch/Class/All + Single Print + Single PDF
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedIconButton(
                        onClick = { showCustomizeSheet = true },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SlateOutline),
                        modifier = Modifier.size(44.dp).testTag("studio_customize_btn")
                    ) {
                        Icon(Icons.Rounded.Tune, contentDescription = "ফন্ট ও লোগো সেটিংস", modifier = Modifier.size(18.dp))
                    }
                    OutlinedButton(
                        onClick = { showBatchDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, TealPrimary),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1.1f).testTag("studio_batch_btn")
                    ) {
                        Icon(Icons.Rounded.LibraryBooks, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(15.dp))
                        Text(" ব্যাচ/সব", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = TealPrimary, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { onPrintStudent(active) },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SlateOutline),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier.weight(0.9f).testTag("studio_print_btn")
                    ) {
                        Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                        Text(" প্রিন্ট", fontSize = 11.5.sp, maxLines = 1)
                    }
                    Button(
                        onClick = { onExportPdf(active) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier.weight(0.95f).testTag("studio_download_pdf_btn")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                        Text(" PDF", fontSize = 11.5.sp, maxLines = 1)
                    }
                }
            }
        }
    }

    if (showBatchDialog) {
        BatchReportDialog(
            sortedStudents = sorted,
            initialClass = active.cls,
            includeClassHighest = includeClassHighest,
            onToggleClassHighest = onToggleClassHighest,
            includeSummaryTable = includeSummaryTable,
            summaryColumns = summaryColumns,
            onToggleSummaryTable = onToggleSummaryTable,
            onUpdateSummaryColumns = onUpdateSummaryColumns,
            onPrintBatch = onPrintBatch,
            onExportBatchPdf = onExportBatchPdf,
            onDismiss = { showBatchDialog = false }
        )
    }

    // Student Quick Search Dialog (by Class, Roll, or Name)
    if (showSearchDialog) {
        var query by remember { mutableStateOf("") }
        var filterCls by remember { mutableStateOf<Int?>(active.cls) }
        val filteredList = remember(sorted, query, filterCls) {
            val q = query.trim()
            val qDigits = fromBn(q)
            sorted.filter { s ->
                val clsMatch = filterCls == null || s.cls == filterCls
                if (!clsMatch) return@filter false
                if (q.isEmpty()) return@filter true
                val rollStr = s.roll.toString()
                val rollBn = bn(rollStr)
                s.name.contains(q, ignoreCase = true) ||
                    rollStr == qDigits ||
                    rollStr.contains(qDigits) ||
                    rollBn.contains(q)
            }
        }

        Dialog(
            onDismissRequest = { showSearchDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateOutline),
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(max = 560.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "শিক্ষার্থী খুঁজুন (শ্রেণি, রোল বা নাম)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = SlateText
                        )
                        IconButton(
                            onClick = { showSearchDialog = false },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "বন্ধ করুন")
                        }
                    }

                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("নাম বা রোল নম্বর লিখুন...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = TealPrimary) },
                        trailingIcon = if (query.isNotEmpty()) {
                            {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "মুছুন")
                                }
                            }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("studio_search_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = filterCls == null,
                            onClick = { filterCls = null },
                            colors = appFilterChipColors(),
                            border = appFilterChipBorder(filterCls == null),
                            label = { Text("সব শ্রেণি", fontSize = 11.5.sp) }
                        )
                        availableClasses.forEach { c ->
                            val isSel = filterCls == c
                            FilterChip(
                                selected = isSel,
                                onClick = { filterCls = c },
                                colors = appFilterChipColors(),
                                border = appFilterChipBorder(isSel),
                                label = { Text("${classNameBn(c, settings)} শ্রেণি", fontSize = 11.5.sp) }
                            )
                        }
                    }

                    HorizontalDivider(color = SlateOutline)

                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (filteredList.isEmpty()) {
                            item {
                                Text(
                                    "কোনো শিক্ষার্থী পাওয়া যায়নি",
                                    color = SlateMuted,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(vertical = 20.dp)
                                )
                            }
                        } else {
                            items(filteredList, key = { it.id }) { s ->
                                val isCurr = s.id == active.id
                                Surface(
                                    onClick = {
                                        onSelectId(s.id)
                                        showSearchDialog = false
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCurr) TealContainer.copy(alpha = 0.7f) else SlateBg,
                                    border = BorderStroke(if (isCurr) 2.dp else 1.dp, if (isCurr) TealPrimary else SlateOutline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isCurr) TealPrimary else MaterialTheme.colorScheme.surface
                                        ) {
                                            Text(
                                                bn(s.roll.toString().padStart(2, '0')),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isCurr) Color.White else TealPrimary,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                s.name.ifBlank { "(নাম নেই)" },
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SlateText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val meta = buildString {
                                                append("${classNameBn(s.cls, settings)} শ্রেণি")
                                                if (s.guardian.isNotBlank()) append(" · ${s.guardian}")
                                            }
                                            Text(meta, fontSize = 11.sp, color = SlateMuted, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Full-Screen Large Zoom Viewer (supports 1..4 pages AND full sheet, plus left/right swipe)
    if (showFullscreenPreview) {
        val pageOrder = listOf("p1", "p2", "p3", "p4")
        val fsIdx = pageOrder.indexOf(pageView).coerceAtLeast(0)
        val fsTitle = when (pageView) {
            "p1" -> "পৃষ্ঠা ১/৪ · ফ্রন্ট কভার"
            "p2" -> "পৃষ্ঠা ২/৪ · ১ম-২য় প্রান্তিক ও গুণাবলি"
            "p3" -> "পৃষ্ঠা ৩/৪ · ৩য়, চূড়ান্ত ও উপস্থিতি"
            "p4" -> "পৃষ্ঠা ৪/৪ · ব্যাক কভার"
            else -> "পুরো শিট (২ পাতা)"
        }
        val fsBmp = when (pageView) {
            "p1" -> page1FrontBmp
            "p2" -> page2MarksBmp
            "p3" -> page3SummaryBmp
            else -> page4BackBmp
        }
        var fsScale by remember(pageView) { mutableFloatStateOf(1f) }
        var fsPan by remember(pageView) { mutableStateOf(Offset.Zero) }
        var fsBoxSize by remember { mutableStateOf(IntSize.Zero) }

        Dialog(
            onDismissRequest = { showFullscreenPreview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(Modifier.fillMaxSize()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, SlateOutline)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "${active.name} · $fsTitle",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val next = (fsScale - 0.5f).coerceAtLeast(1f)
                                    fsScale = next
                                    fsPan = clampPanOffset(fsPan, next, fsBoxSize)
                                },
                                enabled = fsScale > 1.02f,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "জুম আউট")
                            }
                            Text(
                                "${bn((fsScale * 100).roundToInt().toString())}%",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary,
                                modifier = Modifier.clickable {
                                    fsScale = 1f
                                    fsPan = Offset.Zero
                                }
                            )
                            IconButton(
                                onClick = {
                                    val next = (fsScale + 0.5f).coerceAtMost(4.5f)
                                    fsScale = next
                                    fsPan = clampPanOffset(fsPan, next, fsBoxSize)
                                },
                                enabled = fsScale < 4.45f,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "জুম ইন")
                            }
                            IconButton(
                                onClick = { showFullscreenPreview = false },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "বন্ধ করুন")
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(SlateBg)
                            .clipToBounds()
                            .onSizeChanged { fsBoxSize = it }
                            .pointerInput(pageView, fsBoxSize) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    var swipeDx = 0f
                                    var swipeDy = 0f
                                    var swipeHandled = false
                                    do {
                                        val event = awaitPointerEvent()
                                        val pressed = event.changes.count { it.pressed }
                                        if (pressed >= 2) {
                                            swipeHandled = true
                                            val z = event.calculateZoom()
                                            val p = event.calculatePan()
                                            if (z != 1f || p != Offset.Zero) {
                                                val ns = (fsScale * z).coerceIn(1f, 4.5f)
                                                fsScale = ns
                                                fsPan = clampPanOffset(fsPan + p, ns, fsBoxSize)
                                                event.changes.forEach { it.consume() }
                                            }
                                        } else if (fsScale > 1.02f && pressed == 1) {
                                            val p = event.calculatePan()
                                            if (p != Offset.Zero) {
                                                fsPan = clampPanOffset(fsPan + p, fsScale, fsBoxSize)
                                                event.changes.forEach { it.consume() }
                                            }
                                        } else if (!swipeHandled && pressed == 1) {
                                            val p = event.calculatePan()
                                            swipeDx += p.x
                                            swipeDy += p.y
                                            if (abs(swipeDx) > 75f && abs(swipeDx) > abs(swipeDy) * 1.4f) {
                                                swipeHandled = true
                                                event.changes.forEach { it.consume() }
                                                if (swipeDx < 0f) {
                                                    val next = if (pageView == "both") "p1" else pageOrder[(fsIdx + 1).coerceAtMost(pageOrder.lastIndex)]
                                                    selectPage(next)
                                                } else {
                                                    val prev = if (pageView == "both") "p4" else pageOrder[(fsIdx - 1).coerceAtLeast(0)]
                                                    selectPage(prev)
                                                }
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            }
                            .pointerInput(pageView) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (fsScale > 1.05f) {
                                            fsScale = 1f
                                            fsPan = Offset.Zero
                                        } else {
                                            fsScale = 2.4f
                                            fsPan = Offset.Zero
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (pageView == "both") {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(8.dp)
                                    .graphicsLayer {
                                        scaleX = fsScale
                                        scaleY = fsScale
                                        translationX = fsPan.x
                                        translationY = fsPan.y
                                    },
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                SingleReportPageCard(
                                    bitmap = coverBmp,
                                    ratio = fullSheetRatio,
                                    description = "শিট-১: ব্যাক ও ফ্রন্ট কভার"
                                )
                                SingleReportPageCard(
                                    bitmap = marksBmp,
                                    ratio = fullSheetRatio,
                                    description = "শিট-২: নম্বর ও চূড়ান্ত ফলাফল"
                                )
                            }
                        } else {
                            Image(
                                bitmap = fsBmp.asImageBitmap(),
                                contentDescription = fsTitle,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp)
                                    .graphicsLayer {
                                        scaleX = fsScale
                                        scaleY = fsScale
                                        translationX = fsPan.x
                                        translationY = fsPan.y
                                    }
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, SlateOutline)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (pageView == "both") selectPage("p4")
                                    else if (fsIdx > 0) selectPage(pageOrder[fsIdx - 1])
                                },
                                enabled = pageView == "both" || fsIdx > 0
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = null)
                                Text("আগের পাতা")
                            }
                            Text(
                                if (pageView == "both") "পুরো শিট" else "পৃষ্ঠা ${bn((fsIdx + 1).toString())} / ৪",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = TealPrimary,
                                modifier = Modifier.clickable {
                                    if (pageView == "both") selectPage(lastSinglePage) else pageView = "both"
                                }
                            )
                            OutlinedButton(
                                onClick = {
                                    if (pageView == "both") selectPage("p1")
                                    else if (fsIdx < pageOrder.lastIndex) selectPage(pageOrder[fsIdx + 1])
                                },
                                enabled = pageView == "both" || fsIdx < pageOrder.lastIndex
                            ) {
                                Text("পরের পাতা")
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomizeSheet) {
        val customizeScrollState = rememberScrollState()
        Dialog(
            onDismissRequest = { showCustomizeSheet = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showCustomizeSheet = false }
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                val sheetMaxHeight = maxHeight * 0.88f
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    tonalElevation = 4.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .heightIn(max = sheetMaxHeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        // Fixed header bar so panel stays anchored and always has a 1-tap Done button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "রিপোর্ট কার্ড ফন্ট, লোগো ও টেবিল সেটিংস",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = SlateText,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { showCustomizeSheet = false },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("studio_customize_done_btn")
                            ) {
                                Text("সম্পন্ন", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TealPrimary)
                            }
                        }
                        HorizontalDivider(color = SlateOutline.copy(alpha = 0.7f))

                        Column(
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(customizeScrollState)
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .padding(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("অফিসিয়াল বাংলা ফন্ট (PDF)", fontSize = 12.sp, color = SlateMuted)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                PDF_FONT_LIST.forEach { font ->
                                    val isSel = settings.pdfFont == font.id
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { onUpdateSettings(settings.copy(pdfFont = font.id)) },
                                        colors = appFilterChipColors(),
                                        border = appFilterChipBorder(isSel),
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
                                    val isSel = settings.dpeLogo == logo.id
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { onUpdateSettings(settings.copy(dpeLogo = logo.id)) },
                                        colors = appFilterChipColors(),
                                        border = appFilterChipBorder(isSel),
                                        modifier = Modifier.testTag("studio_logo_${logo.id}"),
                                        label = { Text(logo.labelBn.substringBefore(" (")) }
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { showFoldGuide = !showFoldGuide }
                            ) {
                                Checkbox(
                                    checked = showFoldGuide,
                                    onCheckedChange = { showFoldGuide = it },
                                    modifier = Modifier.testTag("studio_fold_guide_checkbox")
                                )
                                Text("মাঝখানের ভাঁজ-রেখা দেখান (A4 → A5)", fontSize = 13.sp)
                            }

                            var addressDraft by remember(showCustomizeSheet, settings.schoolAddress) {
                                mutableStateOf(settings.schoolAddress)
                            }
                            Surface(
                                color = SlateBg,
                                border = BorderStroke(1.dp, SlateOutline),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onToggleCoverAddress(!showCoverAddress) }
                                    ) {
                                        Checkbox(
                                            checked = showCoverAddress,
                                            onCheckedChange = onToggleCoverAddress,
                                            modifier = Modifier.testTag("studio_cover_address_checkbox")
                                        )
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                "পাতা-১ প্রচ্ছদে বিদ্যালয়ের ঠিকানা দেখান",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = SlateText
                                            )
                                            Text(
                                                "বিদ্যালয়ের নামের নিচে ২ সাইজ ছোট ফন্টে দেখাবে",
                                                fontSize = 11.sp,
                                                color = SlateMuted
                                            )
                                        }
                                    }
                                    OutlinedTextField(
                                        value = addressDraft,
                                        onValueChange = { addressDraft = it },
                                        label = { Text("বিদ্যালয়ের ঠিকানা (প্রচ্ছদ ও ব্যাক কভার)", fontSize = 12.sp) },
                                        placeholder = { Text("যেমন: আলফাডাঙ্গা, ফরিদপুর", fontSize = 12.sp) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("studio_address_input")
                                    )
                                    val trimmedDraft = addressDraft.trim()
                                    if (trimmedDraft != settings.schoolAddress.trim()) {
                                        Button(
                                            onClick = { onSaveCoverAddress(trimmedDraft) },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("studio_save_address_btn")
                                        ) {
                                            Text("ঠিকানা সংরক্ষণ করুন")
                                        }
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { onToggleClassHighest(!includeClassHighest) }
                            ) {
                                Checkbox(
                                    checked = includeClassHighest,
                                    onCheckedChange = onToggleClassHighest,
                                    modifier = Modifier.testTag("studio_class_highest_checkbox")
                                )
                                Column(Modifier.weight(1f)) {
                                    Text("৩য় পৃষ্ঠায় 'শ্রেণিতে সর্বোচ্চ নম্বর' কলাম যোগ করুন", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        "চূড়ান্ত ফলাফলের অধীনে শ্রেণির সর্বোচ্চ নম্বর দেখাবে",
                                        fontSize = 11.sp,
                                        color = SlateMuted
                                    )
                                }
                            }

                            SummaryTableConfigCard(
                                includeSummaryTable = includeSummaryTable,
                                selectedColumns = summaryColumns,
                                onToggleInclude = onToggleSummaryTable,
                                onUpdateColumns = onUpdateSummaryColumns,
                                initiallyExpanded = true,
                                collapsibleWhenEnabled = false,
                                testTagPrefix = "studio_summary"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleReportPageCard(
    bitmap: Bitmap,
    ratio: Float,
    description: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SlateOutline),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = description,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .padding(4.dp)
        )
    }
}

@Composable
private fun SheetOverviewCard(
    title: String,
    bitmap: Bitmap,
    ratio: Float,
    onOpenLeft: () -> Unit,
    leftLabel: String,
    onOpenRight: () -> Unit,
    rightLabel: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SlateOutline),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontSize = 11.sp, color = SlateMuted)
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = title,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenLeft,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.ZoomOutMap, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(leftLabel, fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = onOpenRight,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.ZoomOutMap, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(rightLabel, fontSize = 11.sp)
                }
            }
        }
    }
}
