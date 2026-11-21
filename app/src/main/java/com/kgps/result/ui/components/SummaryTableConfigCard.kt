package com.kgps.result.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kgps.result.ReportSummaryColumn
import com.kgps.result.bn
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateSurfaceVariant
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealContainer
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft

@Composable
fun SummaryTableConfigCard(
    includeSummaryTable: Boolean,
    selectedColumns: List<ReportSummaryColumn>,
    onToggleInclude: (Boolean) -> Unit,
    onUpdateColumns: (List<ReportSummaryColumn>) -> Unit,
    initiallyExpanded: Boolean = true,
    collapsibleWhenEnabled: Boolean = false,
    testTagPrefix: String = "summary"
) {
    val activeCols = remember(selectedColumns) {
        selectedColumns.distinct().ifEmpty { ReportSummaryColumn.DEFAULT_SELECTED }
    }
    var editorExpanded by remember(initiallyExpanded) { mutableStateOf(initiallyExpanded) }
    var keepOneHint by remember { mutableStateOf(false) }

    Surface(
        color = if (includeSummaryTable) TealSoft else SlateBg,
        border = BorderStroke(
            1.dp,
            if (includeSummaryTable) TealPrimary.copy(alpha = 0.45f) else SlateOutline
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${testTagPrefix}_table_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Primary Toggle: থাকবে কি না
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleInclude(!includeSummaryTable) }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("${testTagPrefix}_table_toggle_row"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = includeSummaryTable,
                    onCheckedChange = onToggleInclude,
                    modifier = Modifier.testTag("${testTagPrefix}_table_checkbox")
                )
                Column(Modifier.weight(1f).padding(end = 6.dp)) {
                    Text(
                        "৩য় পৃষ্ঠায় 'ফলাফল সারাংশ' টেবিল যোগ করুন",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateText
                    )
                    Text(
                        if (includeSummaryTable) {
                            "${bn(activeCols.size.toString())}টি কলাম: ${activeCols.joinToString(" · ") { it.shortLabelBn }}"
                        } else {
                            "শতকরা হার, অবস্থানগত মান, মেধাক্রম ইত্যাদি পছন্দমতো যোগ করুন"
                        },
                        fontSize = 10.5.sp,
                        color = if (includeSummaryTable) TealDark else SlateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (includeSummaryTable && collapsibleWhenEnabled) {
                    Surface(
                        onClick = { editorExpanded = !editorExpanded },
                        shape = RoundedCornerShape(7.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, TealContainer),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("${testTagPrefix}_expand_config_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                if (editorExpanded) "বন্ধ" else "সাজান",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TealPrimary
                            )
                            Icon(
                                imageVector = if (editorExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = if (editorExpanded) "কলাম এডিটর লুকান" else "কলাম বাছাই ও ড্র্যাগ করুন",
                                tint = TealPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 2. Column Selection + Horizontal Drag Reorder Panel
            AnimatedVisibility(visible = includeSummaryTable && (!collapsibleWhenEnabled || editorExpanded)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.86f))
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = TealContainer)

                    // Hint Header + Default Reset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Rounded.SwapHoriz,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "বামে ↔ ডানে ড্রাগ করে কলামের ক্রম সাজান",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TealDark
                            )
                        }
                        if (activeCols != ReportSummaryColumn.DEFAULT_SELECTED) {
                            TextButton(
                                onClick = {
                                    keepOneHint = false
                                    onUpdateColumns(ReportSummaryColumn.DEFAULT_SELECTED)
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                modifier = Modifier.height(26.dp).testTag("${testTagPrefix}_reset_default_btn")
                            ) {
                                Text("ডিফল্ট", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TealPrimary)
                            }
                        }
                    }

                    // Horizontal Draggable Table Preview Strip
                    DraggableSummaryColumnStrip(
                        columns = activeCols,
                        onReorder = onUpdateColumns,
                        testTagPrefix = testTagPrefix
                    )

                    HorizontalDivider(color = SlateOutline.copy(alpha = 0.7f))

                    Text(
                        "টেবিলে কোন কোন তথ্য থাকবে (ট্যাপ করে চালু/বন্ধ করুন)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateText
                    )

                    // 2-Column Grid of Available Columns
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        ReportSummaryColumn.entries.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                pair.forEach { col ->
                                    val selectedIdx = activeCols.indexOf(col)
                                    val isSelected = selectedIdx >= 0
                                    Surface(
                                        onClick = {
                                            if (isSelected) {
                                                if (activeCols.size <= 1) {
                                                    keepOneHint = true
                                                } else {
                                                    keepOneHint = false
                                                    onUpdateColumns(activeCols - col)
                                                }
                                            } else {
                                                keepOneHint = false
                                                onUpdateColumns(activeCols + col)
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) TealSoft else SlateBg,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) TealPrimary else SlateOutline
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("${testTagPrefix}_col_chip_${col.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Rounded.Check else Icons.Rounded.Add,
                                                contentDescription = null,
                                                tint = if (isSelected) TealPrimary else SlateMuted,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = if (isSelected) {
                                                    "${bn((selectedIdx + 1).toString())}. ${col.shortLabelBn}"
                                                } else {
                                                    col.shortLabelBn
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) TealDark else SlateText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    if (keepOneHint) {
                        Text(
                            "টেবিল চালু রাখলে কমপক্ষে ১টি কলাম নির্বাচিত থাকতে হবে",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DraggableSummaryColumnStrip(
    columns: List<ReportSummaryColumn>,
    onReorder: (List<ReportSummaryColumn>) -> Unit,
    testTagPrefix: String
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val spacingDp = 4.dp
    val spacingPx = with(density) { spacingDp.toPx() }

    var draggingCol by remember { mutableStateOf<ReportSummaryColumn?>(null) }
    var dragDeltaX by remember { mutableFloatStateOf(0f) }
    val slotWidths = remember { mutableStateMapOf<ReportSummaryColumn, Float>() }

    val latestCols by rememberUpdatedState(columns)
    val latestOnReorder by rememberUpdatedState(onReorder)

    Surface(
        color = SlateSurfaceVariant,
        border = BorderStroke(1.dp, SlateOutline),
        shape = RoundedCornerShape(9.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(spacingDp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            columns.forEachIndexed { idx, col ->
                key(col) {
                    val isDragging = draggingCol == col
                    Surface(
                        color = if (isDragging) TealContainer else Color.White,
                        border = BorderStroke(
                            width = if (isDragging) 1.5.dp else 1.dp,
                            color = if (isDragging) TealPrimary else SlateOutline
                        ),
                        shadowElevation = if (isDragging) 6.dp else 0.dp,
                        shape = RoundedCornerShape(7.dp),
                        modifier = Modifier
                            .weight(col.tableWeight.coerceAtLeast(0.8f))
                            .zIndex(if (isDragging) 10f else 0f)
                            .graphicsLayer {
                                if (isDragging) {
                                    translationX = dragDeltaX
                                    scaleX = 1.03f
                                    scaleY = 1.03f
                                }
                            }
                            .onSizeChanged { size ->
                                slotWidths[col] = size.width.toFloat()
                            }
                            .pointerInput(col) {
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        draggingCol = col
                                        dragDeltaX = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                    onDragEnd = {
                                        draggingCol = null
                                        dragDeltaX = 0f
                                    },
                                    onDragCancel = {
                                        draggingCol = null
                                        dragDeltaX = 0f
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        if (latestCols.size <= 1) return@detectHorizontalDragGestures
                                        dragDeltaX += dragAmount

                                        val curList = latestCols.toMutableList()
                                        var curIdx = curList.indexOf(col)
                                        if (curIdx < 0) return@detectHorizontalDragGestures

                                        // ডানদিকে ড্রাগ করে পরের কলামের সাথে অদলবদল
                                        while (curIdx < curList.lastIndex) {
                                            val curW = slotWidths[curList[curIdx]] ?: 80f
                                            val nextCol = curList[curIdx + 1]
                                            val nextW = slotWidths[nextCol] ?: 80f
                                            val threshold = ((curW + nextW) * 0.5f + spacingPx) * 0.52f
                                            if (dragDeltaX > threshold) {
                                                curList[curIdx] = nextCol
                                                curList[curIdx + 1] = col
                                                curIdx++
                                                dragDeltaX -= (nextW + spacingPx)
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                latestOnReorder(curList.toList())
                                            } else {
                                                break
                                            }
                                        }

                                        // বামদিকে ড্রাগ করে আগের কলামের সাথে অদলবদল
                                        while (curIdx > 0) {
                                            val curW = slotWidths[curList[curIdx]] ?: 80f
                                            val prevCol = curList[curIdx - 1]
                                            val prevW = slotWidths[prevCol] ?: 80f
                                            val threshold = ((curW + prevW) * 0.5f + spacingPx) * 0.52f
                                            if (dragDeltaX < -threshold) {
                                                curList[curIdx] = prevCol
                                                curList[curIdx - 1] = col
                                                curIdx--
                                                dragDeltaX += (prevW + spacingPx)
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                latestOnReorder(curList.toList())
                                            } else {
                                                break
                                            }
                                        }
                                    }
                                )
                            }
                            .testTag("${testTagPrefix}_drag_col_${col.id}")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top simulated header row
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isDragging) TealPrimary.copy(alpha = 0.14f) else SlateBg)
                                    .padding(horizontal = 3.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.DragIndicator,
                                        contentDescription = "ড্রাগ করুন",
                                        tint = if (isDragging) TealPrimary else SlateMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(1.dp))
                                    Text(
                                        bn((idx + 1).toString()),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TealPrimary
                                    )
                                }
                                Text(
                                    col.shortLabelBn,
                                    fontSize = if (columns.size >= 5) 9.sp else 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateText,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    lineHeight = 11.sp,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            HorizontalDivider(color = SlateOutline.copy(alpha = 0.6f))
                            // Simulated sample value row
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 3.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    col.sampleValueBn,
                                    fontSize = if (columns.size >= 5) 9.sp else 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SlateMuted,
                                    textAlign = TextAlign.Center,
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
}
