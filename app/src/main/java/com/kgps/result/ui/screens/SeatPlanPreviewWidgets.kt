@file:OptIn(ExperimentalLayoutApi::class)

package com.kgps.result.ui.screens

import android.content.Context
import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kgps.result.SeatPlanConfig
import com.kgps.result.SeatPlanOrientation
import com.kgps.result.SeatPlanPageSpec
import com.kgps.result.SeatPlanPairMode
import com.kgps.result.SeatPlanPdfGen
import com.kgps.result.SeatPlanPlanner
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.bn
import com.kgps.result.classNameBn
import com.kgps.result.classShortBn
import com.kgps.result.syllabusTierForClass
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft
import com.kgps.result.ui.theme.appFilterChipBorder
import com.kgps.result.ui.theme.appFilterChipColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

private fun clampSeatPlanFullscreenPan(offset: Offset, scale: Float, size: IntSize): Offset {
    if (scale <= 1.02f || size.width <= 0 || size.height <= 0) return Offset.Zero
    val maxX = (size.width * (scale - 1f)) / 2f
    val maxY = (size.height * (scale - 1f)) / 2f + size.height * 0.22f
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY)
    )
}

@Composable
internal fun SeatPlanRenderedPage(
    ctx: Context,
    page: SeatPlanPageSpec,
    settings: Settings,
    renderScale: Float,
    modifier: Modifier = Modifier,
    pageAlignment: Alignment = Alignment.Center
) {
    val bmp by produceState<Bitmap?>(
        initialValue = null,
        key1 = page,
        key2 = settings
    ) {
        value = withContext(Dispatchers.Default) {
            SeatPlanPdfGen.renderPageBitmap(
                ctx = ctx,
                page = page,
                settings = settings,
                scale = renderScale
            )
        }
    }

    val targetRatio = if (page.config.orientation == SeatPlanOrientation.LANDSCAPE) {
        842f / 595f
    } else {
        595f / 842f
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = pageAlignment
    ) {
        val availableRatio = if (maxHeight.value > 0f) maxWidth.value / maxHeight.value else targetRatio
        val paperModifier = if (availableRatio > targetRatio) {
            Modifier.fillMaxHeight().aspectRatio(targetRatio)
        } else {
            Modifier.fillMaxWidth().aspectRatio(targetRatio)
        }

        Surface(
            color = Color.White,
            border = BorderStroke(1.dp, SlateOutline.copy(alpha = 0.9f)),
            shadowElevation = 4.dp,
            shape = RoundedCornerShape(6.dp),
            modifier = paperModifier
        ) {
            val loaded = bmp
            if (loaded != null) {
                Image(
                    bitmap = loaded.asImageBitmap(),
                    contentDescription = "আসন বিন্যাস প্রিভিউ পৃষ্ঠা ${bn(page.pageNumber.toString())}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TealPrimary, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
internal fun SeatPlanFullscreenDialog(
    ctx: Context,
    pages: List<SeatPlanPageSpec>,
    initialPageIndex: Int,
    settings: Settings,
    onPageChanged: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    if (pages.isEmpty()) return
    val scope = rememberCoroutineScope()
    val fsPagerState = rememberPagerState(
        initialPage = initialPageIndex.coerceIn(0, pages.lastIndex),
        pageCount = { pages.size }
    )
    var fsScale by remember { mutableFloatStateOf(1f) }
    var fsPan by remember { mutableStateOf(Offset.Zero) }
    var fsSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(fsPagerState.currentPage) {
        fsScale = 1f
        fsPan = Offset.Zero
        onPageChanged(fsPagerState.currentPage)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color(0xFF0F172A),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                HorizontalPager(
                    state = fsPagerState,
                    userScrollEnabled = fsScale <= 1.05f,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 60.dp, bottom = 52.dp, start = 10.dp, end = 10.dp)
                        .testTag("seat_plan_fullscreen_pager")
                ) { pageIdx ->
                    val pageSpec = pages[pageIdx]
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .onSizeChanged { fsSize = it }
                            .pointerInput(pageIdx) {
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
                            .pointerInput(pageIdx) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    do {
                                        val event = awaitPointerEvent()
                                        val pointerCount = event.changes.count { it.pressed }
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        if (pointerCount >= 2 || fsScale > 1.02f) {
                                            val nextScale = (fsScale * zoomChange).coerceIn(1f, 4f)
                                            fsScale = nextScale
                                            fsPan = clampSeatPlanFullscreenPan(fsPan + panChange, nextScale, fsSize)
                                            if (pointerCount >= 2 || abs(zoomChange - 1f) > 0.01f || fsScale > 1.02f) {
                                                event.changes.forEach { it.consume() }
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        SeatPlanRenderedPage(
                            ctx = ctx,
                            page = pageSpec,
                            settings = settings,
                            renderScale = 2.2f,
                            modifier = Modifier.graphicsLayer(
                                scaleX = fsScale,
                                scaleY = fsScale,
                                translationX = fsPan.x,
                                translationY = fsPan.y
                            )
                        )
                    }
                }

                // Top Header Bar (Page badge + Close button)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "পৃষ্ঠা ${bn((fsPagerState.currentPage + 1).toString())}/${bn(pages.size.toString())} · সোয়াইপ ও ডাবল ট্যাপে জুম",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                            .testTag("seat_plan_fullscreen_close_btn")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "ফুল স্ক্রিন বন্ধ করুন", tint = Color.White)
                    }
                }

                // Bottom Page Controls (when multiple pages)
                if (pages.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    fsPagerState.animateScrollToPage((fsPagerState.currentPage - 1).coerceAtLeast(0))
                                }
                            },
                            enabled = fsPagerState.currentPage > 0,
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "আগের পৃষ্ঠা",
                                tint = if (fsPagerState.currentPage > 0) Color.White else Color.White.copy(alpha = 0.3f)
                            )
                        }
                        Text(
                            text = "${bn((fsPagerState.currentPage + 1).toString())} / ${bn(pages.size.toString())}",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(
                            onClick = {
                                scope.launch {
                                    fsPagerState.animateScrollToPage((fsPagerState.currentPage + 1).coerceAtMost(pages.lastIndex))
                                }
                            },
                            enabled = fsPagerState.currentPage < pages.lastIndex,
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "পরের পৃষ্ঠা",
                                tint = if (fsPagerState.currentPage < pages.lastIndex) Color.White else Color.White.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SeatPlanMultiClassExportDialog(
    action: AdmitExportAction,
    allowedClasses: List<Int>,
    countByClass: Map<Int, Int>,
    settings: Settings,
    students: List<Student>,
    config: SeatPlanConfig,
    onDismiss: () -> Unit,
    onConfirm: (updatedConfig: SeatPlanConfig, exportPages: List<SeatPlanPageSpec>, fileNameOrJob: String) -> Unit
) {
    val nonEmptyClasses = remember(allowedClasses, countByClass, config.extraBlankSlips) {
        if (config.extraBlankSlips > 0) allowedClasses
        else allowedClasses.filter { (countByClass[it] ?: 0) > 0 }.ifEmpty { allowedClasses }
    }
    var selectedClasses by remember(config.selectedClasses, allowedClasses) {
        val valid = config.selectedClasses.filter { it in allowedClasses }.toSet()
        mutableStateOf(
            if (valid.isNotEmpty()) valid
            else allowedClasses.firstOrNull()?.let { setOf(it) }.orEmpty()
        )
    }
    var leftCls by remember(config.leftClass, allowedClasses) {
        mutableStateOf(config.leftClass?.takeIf { it in allowedClasses } ?: allowedClasses.firstOrNull())
    }
    var rightCls by remember(config.rightClass, allowedClasses) {
        mutableStateOf(config.rightClass?.takeIf { it in allowedClasses } ?: allowedClasses.lastOrNull())
    }

    val lowerClasses = remember(nonEmptyClasses, settings) {
        nonEmptyClasses.filter { syllabusTierForClass(it, settings) <= 2 }
    }
    val upperClasses = remember(nonEmptyClasses, settings) {
        nonEmptyClasses.filter { syllabusTierForClass(it, settings) >= 3 }
    }
    val orderedSelection = remember(selectedClasses, allowedClasses) {
        allowedClasses.filter { it in selectedClasses }
    }

    val effectiveConfig = remember(config, orderedSelection, leftCls, rightCls) {
        if (config.pairMode == SeatPlanPairMode.SIDE_BY_SIDE && config.safeColumns >= 2) {
            config.copy(leftClass = leftCls, rightClass = rightCls)
        } else {
            config.copy(selectedClasses = orderedSelection)
        }
    }

    val combinedPages = remember(effectiveConfig, students, settings, allowedClasses) {
        SeatPlanPlanner.buildPages(
            students = students,
            settings = settings,
            config = effectiveConfig,
            allowedClassIds = allowedClasses
        )
    }
    val totalSlips = remember(combinedPages) {
        combinedPages.sumOf { page -> page.slips.count { it != null } }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (action == AdmitExportAction.PDF) Icons.Rounded.PictureAsPdf else Icons.Rounded.Print,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (action == AdmitExportAction.PDF) "PDF-এর জন্য শ্রেণি বাছুন" else "প্রিন্টের জন্য শ্রেণি বাছুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (config.pairMode == SeatPlanPairMode.SIDE_BY_SIDE && config.safeColumns >= 2) {
                    Text(
                        text = "পাশাপাশি বেঞ্চ জোড়া মোড চালু আছে — বাম ও ডান কলামের শ্রেণি নির্বাচন করুন:",
                        fontSize = 11.5.sp,
                        color = SlateMuted
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AdmitCompactPickerBox(
                            label = "বাম কলাম শ্রেণি",
                            valueText = leftCls?.let { "${classShortBn(it, settings)} (${bn((countByClass[it] ?: 0).toString())})" } ?: "নেই",
                            options = allowedClasses,
                            selectedValue = leftCls ?: allowedClasses.firstOrNull() ?: 1,
                            optionLabel = { c -> "${classNameBn(c, settings)} (${bn((countByClass[c] ?: 0).toString())} জন)" },
                            onSelect = { leftCls = it },
                            modifier = Modifier.weight(1f),
                            testTag = "seat_export_left_cls"
                        )
                        AdmitCompactPickerBox(
                            label = "ডান কলাম শ্রেণি",
                            valueText = rightCls?.let { "${classShortBn(it, settings)} (${bn((countByClass[it] ?: 0).toString())})" } ?: "নেই",
                            options = allowedClasses,
                            selectedValue = rightCls ?: allowedClasses.lastOrNull() ?: 1,
                            optionLabel = { c -> "${classNameBn(c, settings)} (${bn((countByClass[c] ?: 0).toString())} জন)" },
                            onSelect = { rightCls = it },
                            modifier = Modifier.weight(1f),
                            testTag = "seat_export_right_cls"
                        )
                    }
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val allSelected = nonEmptyClasses.isNotEmpty() && nonEmptyClasses.all { it in selectedClasses }
                        FilterChip(
                            selected = allSelected,
                            onClick = { selectedClasses = nonEmptyClasses.toSet() },
                            label = { Text("সব শ্রেণি", fontSize = 11.sp) },
                            colors = appFilterChipColors(),
                            border = appFilterChipBorder(allSelected),
                            modifier = Modifier.heightIn(min = 38.dp).testTag("seat_export_all_cls")
                        )
                        if (lowerClasses.isNotEmpty() && lowerClasses.size < nonEmptyClasses.size) {
                            val isLower = selectedClasses == lowerClasses.toSet()
                            FilterChip(
                                selected = isLower,
                                onClick = { selectedClasses = lowerClasses.toSet() },
                                label = { Text("প্রাক–২য়", fontSize = 11.sp) },
                                colors = appFilterChipColors(),
                                border = appFilterChipBorder(isLower),
                                modifier = Modifier.heightIn(min = 38.dp)
                            )
                        }
                        if (upperClasses.isNotEmpty() && upperClasses.size < nonEmptyClasses.size) {
                            val isUpper = selectedClasses == upperClasses.toSet()
                            FilterChip(
                                selected = isUpper,
                                onClick = { selectedClasses = upperClasses.toSet() },
                                label = { Text("৩য়–৫ম", fontSize = 11.sp) },
                                colors = appFilterChipColors(),
                                border = appFilterChipBorder(isUpper),
                                modifier = Modifier.heightIn(min = 38.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        allowedClasses.forEach { c ->
                            val checked = c in selectedClasses
                            val cnt = countByClass[c] ?: 0
                            Surface(
                                color = if (checked) TealSoft.copy(alpha = 0.55f) else SlateBg.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (checked) TealPrimary.copy(alpha = 0.5f) else SlateOutline),
                                shape = RoundedCornerShape(9.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedClasses = if (checked) selectedClasses - c else selectedClasses + c
                                    }
                                    .testTag("seat_export_cls_$c")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { on ->
                                            selectedClasses = if (on) selectedClasses + c else selectedClasses - c
                                        }
                                    )
                                    Text(
                                        text = "${classNameBn(c, settings)} শ্রেণি",
                                        fontSize = 12.5.sp,
                                        fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
                                        color = SlateText,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${bn(cnt.toString())} জন",
                                        fontSize = 11.sp,
                                        color = SlateMuted,
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(
                    color = SlateBg,
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val clsInfo = if (config.pairMode == SeatPlanPairMode.SIDE_BY_SIDE && config.safeColumns >= 2) {
                            "${leftCls?.let { classShortBn(it, settings) } ?: "?"} × ${rightCls?.let { classShortBn(it, settings) } ?: "?"}"
                        } else {
                            "${bn(orderedSelection.size.toString())}টি শ্রেণি"
                        }
                        Text(
                            text = "মোট: $clsInfo · ${bn(totalSlips.toString())} স্লিপ · ${bn(combinedPages.size.toString())} পৃষ্ঠা",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (combinedPages.isNotEmpty()) TealPrimary else SlateMuted
                        )
                        Text(
                            text = "${config.orientation.labelBn} · ${bn(config.safeColumns.toString())}×${bn(config.safeRows.toString())} গ্রিড",
                            fontSize = 10.5.sp,
                            color = SlateMuted
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val label = if (config.pairMode == SeatPlanPairMode.SIDE_BY_SIDE && config.safeColumns >= 2) {
                        val l = leftCls?.let { classShortBn(it, settings) } ?: "বাম"
                        val r = rightCls?.let { classShortBn(it, settings) } ?: "ডান"
                        "আসন-বিন্যাস-$l-$r.pdf"
                    } else {
                        when {
                            orderedSelection.size == 1 -> "আসন-বিন্যাস-${classShortBn(orderedSelection.first(), settings)}.pdf"
                            orderedSelection.size == nonEmptyClasses.size && orderedSelection.size > 1 -> "আসন-বিন্যাস-সকল-শ্রেণি.pdf"
                            else -> "আসন-বিন্যাস-${bn(orderedSelection.size.toString())}-শ্রেণি.pdf"
                        }
                    }
                    val finalArg = if (action == AdmitExportAction.PDF) label else label.removeSuffix(".pdf")
                    onConfirm(effectiveConfig, combinedPages, finalArg)
                },
                enabled = combinedPages.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.heightIn(min = 48.dp).testTag("seat_export_confirm_btn")
            ) {
                Icon(
                    imageVector = if (action == AdmitExportAction.PDF) Icons.Rounded.PictureAsPdf else Icons.Rounded.Print,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    if (action == AdmitExportAction.PDF) {
                        "PDF তৈরি (${bn(combinedPages.size.toString())} পৃষ্ঠা)"
                    } else {
                        "প্রিন্ট (${bn(combinedPages.size.toString())} পৃষ্ঠা)"
                    }
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("বাতিল")
            }
        }
    )
}

