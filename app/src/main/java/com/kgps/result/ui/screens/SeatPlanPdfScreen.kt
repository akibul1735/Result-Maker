@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.kgps.result.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EventSeat
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kgps.result.SeatPlanConfig
import com.kgps.result.SeatPlanFontScale
import com.kgps.result.SeatPlanNumeralMode
import com.kgps.result.SeatPlanOrientation
import com.kgps.result.SeatPlanPageSpec
import com.kgps.result.SeatPlanPairMode
import com.kgps.result.SeatPlanPdfGen
import com.kgps.result.SeatPlanPlanner
import com.kgps.result.SeatPlanRollOrder
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.activeClassIds
import com.kgps.result.bn
import com.kgps.result.classNameBn
import com.kgps.result.classShortBn
import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.fromBn
import com.kgps.result.termNameBn
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

private fun clampSeatPlanPanOffset(offset: Offset, scale: Float, size: IntSize): Offset {
    if (scale <= 1.02f || size.width <= 0 || size.height <= 0) return Offset.Zero
    val maxX = (size.width * (scale - 1f)) / 2f
    val maxY = (size.height * (scale - 1f)) / 2f + size.height * 0.25f
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY)
    )
}

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
    if (onBack != null) {
        BackHandler { onBack() }
    }
    val ctx = LocalContext.current
    val availableClassIds = remember(settings.customClasses) { activeClassIds(settings) }
    val allowedClasses = remember(permissions, availableClassIds) {
        permissions.effectiveClasses(availableClassIds)
    }

    var config by remember(initialClass, allowedClasses) {
        val defaultCls = when {
            initialClass in allowedClasses -> initialClass
            5 in allowedClasses -> 5
            else -> allowedClasses.firstOrNull() ?: 1
        }
        val leftDefault = allowedClasses.firstOrNull { it != defaultCls } ?: defaultCls
        mutableStateOf(
            SeatPlanConfig(
                selectedClasses = listOf(defaultCls),
                leftClass = leftDefault,
                rightClass = defaultCls,
                term = 3,
                columns = 2,
                rows = 6,
                rollOrder = SeatPlanRollOrder.DESCENDING
            )
        )
    }

    var setupExpanded by remember { mutableStateOf(true) }
    var showAdvancedHeader by remember { mutableStateOf(false) }
    var rollFromText by remember { mutableStateOf("") }
    var rollToText by remember { mutableStateOf("") }
    var leftClsMenuOpen by remember { mutableStateOf(false) }
    var rightClsMenuOpen by remember { mutableStateOf(false) }
    var showFullscreen by remember { mutableStateOf(false) }

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

    var previewPageIdx by remember { mutableIntStateOf(0) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(pages.size) {
        if (previewPageIdx >= pages.size) {
            previewPageIdx = (pages.size - 1).coerceAtLeast(0)
        }
    }
    LaunchedEffect(previewPageIdx, pages.size, config.orientation) {
        zoomScale = 1f
        panOffset = Offset.Zero
    }

    val activePage = pages.getOrNull(previewPageIdx)
    val previewBitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = activePage,
        key2 = settings
    ) {
        value = if (activePage == null) {
            null
        } else {
            withContext(Dispatchers.Default) {
                SeatPlanPdfGen.renderPageBitmap(
                    ctx = ctx,
                    page = activePage,
                    settings = settings,
                    scale = 2.0f
                )
            }
        }
    }

    val countByClass = remember(students, allowedClasses) {
        allowedClasses.associateWith { c -> students.count { it.cls == c } }
    }

    val summaryLine = buildString {
        append("${bn(config.safeColumns.toString())}×${bn(config.safeRows.toString())} ")
        append("(${bn(config.slipsPerPage.toString())}/পৃষ্ঠা)")
        append(" · ${termNameBn(config.term)}")
        append(" · মোট ${bn(totalSlipCount.toString())}টি কার্ড")
        if (pages.isNotEmpty()) {
            append(" (${bn(pages.size.toString())} পৃষ্ঠা)")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Setup & Customization Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { setupExpanded = !setupExpanded }
                        .testTag("seat_plan_setup_toggle"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = TealSoft,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.EventSeat,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.padding(6.dp).size(18.dp)
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "আসন বিন্যাস (সিট প্ল্যান) সেটাপ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = SlateText
                        )
                        Text(
                            summaryLine,
                            fontSize = 11.sp,
                            color = SlateMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = if (setupExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (setupExpanded) "সেটাপ লুকান" else "সেটাপ খুলুন",
                        tint = SlateMuted
                    )
                }

                AnimatedVisibility(visible = setupExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        HorizontalDivider(color = SlateOutline.copy(alpha = 0.6f))

                        // A. Mode & Class Selection
                        Text(
                            "১. শ্রেণি ও রোল বিন্যাস",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TealDark
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SeatPlanPairMode.entries.forEach { mode ->
                                val selected = config.pairMode == mode
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        val nextCols = if (mode == SeatPlanPairMode.SIDE_BY_SIDE && config.columns < 2) 2 else config.columns
                                        config = config.copy(pairMode = mode, columns = nextCols)
                                    },
                                    label = { Text(mode.labelBn, fontSize = 11.5.sp) },
                                    colors = appFilterChipColors(),
                                    border = appFilterChipBorder(selected),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("seat_plan_mode_${mode.name.lowercase()}")
                                )
                            }
                        }

                        if (config.pairMode == SeatPlanPairMode.SEQUENTIAL) {
                            val allSelected = allowedClasses.isNotEmpty() &&
                                allowedClasses.all { it in config.selectedClasses }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FilterChip(
                                    selected = allSelected,
                                    onClick = {
                                        config = if (allSelected) {
                                            config.copy(selectedClasses = listOfNotNull(allowedClasses.firstOrNull()))
                                        } else {
                                            config.copy(selectedClasses = allowedClasses)
                                        }
                                    },
                                    label = { Text("সব শ্রেণি", fontSize = 11.5.sp) },
                                    colors = appFilterChipColors(),
                                    border = appFilterChipBorder(allSelected),
                                    modifier = Modifier.testTag("seat_plan_cls_all")
                                )
                                allowedClasses.forEach { c ->
                                    val isSel = c in config.selectedClasses
                                    val cnt = countByClass[c] ?: 0
                                    FilterChip(
                                        selected = isSel,
                                        onClick = {
                                            val next = if (isSel) {
                                                val reduced = config.selectedClasses - c
                                                if (reduced.isEmpty()) listOf(c) else reduced
                                            } else {
                                                (config.selectedClasses + c).distinct().sortedBy { allowedClasses.indexOf(it) }
                                            }
                                            config = config.copy(selectedClasses = next)
                                        },
                                        label = {
                                            Text(
                                                "${classShortBn(c, settings)} (${bn(cnt.toString())})",
                                                fontSize = 11.5.sp
                                            )
                                        },
                                        colors = appFilterChipColors(),
                                        border = appFilterChipBorder(isSel),
                                        modifier = Modifier.testTag("seat_plan_cls_$c")
                                    )
                                }
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(Modifier.weight(1f)) {
                                    val leftLabel = config.leftClass?.let { "${classNameBn(it, settings)} শ্রেণি" } ?: "নির্বাচন করুন"
                                    OutlinedButton(
                                        onClick = { leftClsMenuOpen = true },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    ) {
                                        Text("বাম কলাম: $leftLabel", fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                                    }
                                    DropdownMenu(expanded = leftClsMenuOpen, onDismissRequest = { leftClsMenuOpen = false }) {
                                        allowedClasses.forEach { c ->
                                            DropdownMenuItem(
                                                text = { Text("${classNameBn(c, settings)} শ্রেণি (${bn((countByClass[c] ?: 0).toString())} জন)") },
                                                onClick = {
                                                    config = config.copy(leftClass = c)
                                                    leftClsMenuOpen = false
                                                }
                                            )
                                        }
                                    }
                                }
                                Box(Modifier.weight(1f)) {
                                    val rightLabel = config.rightClass?.let { "${classNameBn(it, settings)} শ্রেণি" } ?: "নির্বাচন করুন"
                                    OutlinedButton(
                                        onClick = { rightClsMenuOpen = true },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    ) {
                                        Text("ডান কলাম: $rightLabel", fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                                    }
                                    DropdownMenu(expanded = rightClsMenuOpen, onDismissRequest = { rightClsMenuOpen = false }) {
                                        allowedClasses.forEach { c ->
                                            DropdownMenuItem(
                                                text = { Text("${classNameBn(c, settings)} শ্রেণি (${bn((countByClass[c] ?: 0).toString())} জন)") },
                                                onClick = {
                                                    config = config.copy(rightClass = c)
                                                    rightClsMenuOpen = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Roll order + Roll range
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SeatPlanRollOrder.entries.forEach { ord ->
                                val selected = config.rollOrder == ord
                                FilterChip(
                                    selected = selected,
                                    onClick = { config = config.copy(rollOrder = ord) },
                                    label = { Text("রোল: ${ord.labelBn}", fontSize = 11.sp) },
                                    colors = appFilterChipColors(),
                                    border = appFilterChipBorder(selected),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("seat_plan_order_${ord.name.lowercase()}")
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = rollFromText,
                                onValueChange = { raw ->
                                    val digits = fromBn(raw).filter { it.isDigit() }.take(4)
                                    rollFromText = digits
                                    config = config.copy(rollFrom = digits.toIntOrNull())
                                },
                                label = { Text("রোল থেকে", fontSize = 11.sp) },
                                placeholder = { Text("সব", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = rollToText,
                                onValueChange = { raw ->
                                    val digits = fromBn(raw).filter { it.isDigit() }.take(4)
                                    rollToText = digits
                                    config = config.copy(rollTo = digits.toIntOrNull())
                                },
                                label = { Text("রোল পর্যন্ত", fontSize = 11.sp) },
                                placeholder = { Text("সব", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = SlateBg,
                                border = BorderStroke(1.dp, SlateOutline),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (config.extraBlankSlips > 0) {
                                                config = config.copy(extraBlankSlips = config.extraBlankSlips - 1)
                                            }
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Rounded.Remove, contentDescription = "ফাঁকা কমান", modifier = Modifier.size(16.dp))
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("ফাঁকা", fontSize = 9.sp, color = SlateMuted)
                                        Text(
                                            bn(config.extraBlankSlips.toString()),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SlateText
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (config.extraBlankSlips < 48) {
                                                config = config.copy(extraBlankSlips = config.extraBlankSlips + 1)
                                            }
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = "ফাঁকা বাড়ান", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = SlateOutline.copy(alpha = 0.5f))

                        // B. Grid & Page Layout
                        Text(
                            "২. গ্রিড, পেজ ও স্টাইল",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TealDark
                        )

                        // Columns + Rows
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("কলাম:", fontSize = 11.sp, color = SlateMuted, fontWeight = FontWeight.SemiBold)
                            listOf(1, 2, 3).forEach { colCnt ->
                                val selected = config.columns == colCnt
                                val enabled = !(config.pairMode == SeatPlanPairMode.SIDE_BY_SIDE && colCnt == 1)
                                FilterChip(
                                    selected = selected,
                                    enabled = enabled,
                                    onClick = { config = config.copy(columns = colCnt) },
                                    label = { Text(bn(colCnt.toString()), fontSize = 11.5.sp) },
                                    colors = appFilterChipColors(),
                                    border = appFilterChipBorder(selected),
                                    modifier = Modifier.testTag("seat_plan_cols_$colCnt")
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("রো:", fontSize = 11.sp, color = SlateMuted, fontWeight = FontWeight.SemiBold)
                            listOf(4, 5, 6, 7, 8).forEach { rowCnt ->
                                val selected = config.rows == rowCnt
                                FilterChip(
                                    selected = selected,
                                    onClick = { config = config.copy(rows = rowCnt) },
                                    label = { Text(bn(rowCnt.toString()), fontSize = 11.5.sp) },
                                    colors = appFilterChipColors(),
                                    border = appFilterChipBorder(selected),
                                    modifier = Modifier.testTag("seat_plan_rows_$rowCnt")
                                )
                            }
                        }

                        // Orientation + Font scale + Numerals
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SeatPlanOrientation.entries.forEach { ori ->
                                val selected = config.orientation == ori
                                FilterChip(
                                    selected = selected,
                                    onClick = { config = config.copy(orientation = ori) },
                                    label = { Text(ori.labelBn, fontSize = 11.sp) },
                                    colors = appFilterChipColors(),
                                    border = appFilterChipBorder(selected)
                                )
                            }
                            SeatPlanNumeralMode.entries.forEach { numMode ->
                                val selected = config.numeralMode == numMode
                                FilterChip(
                                    selected = selected,
                                    onClick = { config = config.copy(numeralMode = numMode) },
                                    label = { Text(numMode.labelBn, fontSize = 11.sp) },
                                    colors = appFilterChipColors(),
                                    border = appFilterChipBorder(selected)
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SeatPlanFontScale.entries.forEach { fs ->
                                val selected = config.fontScale == fs
                                FilterChip(
                                    selected = selected,
                                    onClick = { config = config.copy(fontScale = fs) },
                                    label = { Text("ফন্ট: ${fs.labelBn}", fontSize = 11.sp) },
                                    colors = appFilterChipColors(),
                                    border = appFilterChipBorder(selected)
                                )
                            }
                            FilterChip(
                                selected = config.showCutLines,
                                onClick = { config = config.copy(showCutLines = !config.showCutLines) },
                                label = { Text("কাটার দাগ", fontSize = 11.sp) },
                                colors = appFilterChipColors(),
                                border = appFilterChipBorder(config.showCutLines)
                            )
                            FilterChip(
                                selected = config.roundedCorners,
                                onClick = { config = config.copy(roundedCorners = !config.roundedCorners) },
                                label = { Text("গোলাকার বর্ডার", fontSize = 11.sp) },
                                colors = appFilterChipColors(),
                                border = appFilterChipBorder(config.roundedCorners)
                            )
                            FilterChip(
                                selected = config.boldStudentName,
                                onClick = { config = config.copy(boldStudentName = !config.boldStudentName) },
                                label = { Text("নাম বোল্ড", fontSize = 11.sp) },
                                colors = appFilterChipColors(),
                                border = appFilterChipBorder(config.boldStudentName)
                            )
                            FilterChip(
                                selected = config.padRollTwoDigits,
                                onClick = { config = config.copy(padRollTwoDigits = !config.padRollTwoDigits) },
                                label = { Text("২ ডিজিট রোল (০১)", fontSize = 11.sp) },
                                colors = appFilterChipColors(),
                                border = appFilterChipBorder(config.padRollTwoDigits)
                            )
                        }

                        HorizontalDivider(color = SlateOutline.copy(alpha = 0.5f))

                        // C. Header & Room Details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "৩. শিরোনাম ও কক্ষ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TealDark
                            )
                            Surface(
                                onClick = { showAdvancedHeader = !showAdvancedHeader },
                                color = TealSoft,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Rounded.Tune, contentDescription = null, tint = TealDark, modifier = Modifier.size(13.dp))
                                    Text(
                                        if (showAdvancedHeader) "কাস্টম তথ্য লুকান" else "কাস্টম বিদ্যালয়/শিরোনাম",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TealDark
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            (1..3).forEach { t ->
                                val selected = config.term == t
                                FilterChip(
                                    selected = selected,
                                    onClick = { config = config.copy(term = t) },
                                    label = { Text(termNameBn(t), fontSize = 11.5.sp) },
                                    colors = appFilterChipColors(),
                                    border = appFilterChipBorder(selected),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = config.subtitle,
                                onValueChange = { config = config.copy(subtitle = it) },
                                label = { Text("উপ-শিরোনাম", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = config.roomLabel,
                                onValueChange = { config = config.copy(roomLabel = it) },
                                label = { Text("কক্ষ নং (ঐচ্ছিক)", fontSize = 11.sp) },
                                placeholder = { Text("যেমন: কক্ষ ০১", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        AnimatedVisibility(visible = showAdvancedHeader) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = config.customExamTitle,
                                    onValueChange = { config = config.copy(customExamTitle = it) },
                                    label = { Text("কাস্টম পরীক্ষার শিরোনাম", fontSize = 11.sp) },
                                    placeholder = { Text(SeatPlanPlanner.resolvedExamTitle(config.copy(customExamTitle = ""), settings), fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = config.customSchoolName,
                                    onValueChange = { config = config.copy(customSchoolName = it) },
                                    label = { Text("কাস্টম বিদ্যালয়ের নাম", fontSize = 11.sp) },
                                    placeholder = { Text(settings.school.ifBlank { "বিদ্যালয়ের নাম" }, fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = config.customSchoolAddress,
                                        onValueChange = { config = config.copy(customSchoolAddress = it) },
                                        enabled = config.showSchoolAddress,
                                        label = { Text("কাস্টম ঠিকানা", fontSize = 11.sp) },
                                        placeholder = { Text(settings.schoolAddress.ifBlank { "ঠিকানা" }, fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = config.showSchoolAddress,
                                        onClick = { config = config.copy(showSchoolAddress = !config.showSchoolAddress) },
                                        label = { Text("ঠিকানা", fontSize = 11.sp) },
                                        colors = appFilterChipColors(),
                                        border = appFilterChipBorder(config.showSchoolAddress)
                                    )
                                }
                            }
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
                                modifier = Modifier.size(48.dp).testTag("seat_plan_fullscreen_btn")
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
                                onClick = { config = config.copy(extraBlankSlips = config.slipsPerPage) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("১ পৃষ্ঠা ফাঁকা সিট কার্ড তৈরি করুন", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    val ratio = if (config.orientation == SeatPlanOrientation.LANDSCAPE) 842f / 595f else 595f / 842f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(ratio)
                            .background(SlateSurfaceVariant, RoundedCornerShape(10.dp))
                            .clipToBounds()
                            .onSizeChanged { previewSize = it }
                            .pointerInput(previewPageIdx, pages.size, config.orientation) {
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
                            .pointerInput(previewPageIdx, pages.size, config.orientation) {
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
                                            panOffset = clampSeatPlanPanOffset(
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
                                contentDescription = "আসন বিন্যাস প্রিভিউ",
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

        // 3. Action Buttons
        val exportName = buildString {
            append("আসন-বিন্যাস")
            if (config.roomLabel.isNotBlank()) {
                append("-").append(config.roomLabel.trim().replace(" ", "-"))
            }
            append(".pdf")
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onPrint(pages, "আসন বিন্যাস") },
                enabled = pages.isNotEmpty(),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.6f)),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("seat_plan_print_btn")
            ) {
                Icon(Icons.Rounded.Print, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("প্রিন্ট করুন", fontWeight = FontWeight.SemiBold, color = TealPrimary)
            }
            Button(
                onClick = { onExportPdf(pages, exportName) },
                enabled = pages.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("seat_plan_export_btn")
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
                                            fsPan = clampSeatPlanPanOffset(fsPan + p, ns, fsSize)
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
                                contentDescription = "পূর্ণ স্ক্রিন আসন বিন্যাস",
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
