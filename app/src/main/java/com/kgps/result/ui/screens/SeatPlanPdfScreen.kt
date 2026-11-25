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
import androidx.compose.material.icons.rounded.RestartAlt
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kgps.result.BENGALI_PDF_FONT_LIST
import com.kgps.result.ENGLISH_PDF_FONT_LIST
import com.kgps.result.PdfGen
import com.kgps.result.SeatPlanBorderStyle
import com.kgps.result.SeatPlanBorderWeight
import com.kgps.result.SeatPlanConfig
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
import java.util.Locale
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

private fun formatInchesBn(inches: Float): String =
    bn(String.format(Locale.US, "%.2f", inches)) + "\""

@Composable
private fun CompactStepperBox(
    label: String,
    valueText: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseTag: String,
    increaseTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SlateBg,
        border = BorderStroke(1.dp, SlateOutline),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
        ) {
            IconButton(
                onClick = onDecrease,
                enabled = canDecrease,
                modifier = Modifier.size(48.dp).testTag(decreaseTag)
            ) {
                Icon(Icons.Rounded.Remove, contentDescription = "$label কমান", modifier = Modifier.size(16.dp))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    label,
                    fontSize = 9.5.sp,
                    color = SlateMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    valueText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateText,
                    maxLines = 1
                )
            }
            IconButton(
                onClick = onIncrease,
                enabled = canIncrease,
                modifier = Modifier.size(48.dp).testTag(increaseTag)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "$label বাড়ান", modifier = Modifier.size(16.dp))
            }
        }
    }
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

    val defaultConfig = remember(initialClass, allowedClasses) {
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
            rollOrder = SeatPlanRollOrder.DESCENDING
        )
    }

    var config by remember(defaultConfig) { mutableStateOf(defaultConfig) }
    var setupExpanded by remember { mutableStateOf(true) }
    var activeSetupTab by remember { mutableIntStateOf(0) } // 0=শ্রেণি ও রোল, 1=লেআউট ও ফন্ট, 2=শিরোনাম
    var rollFromText by remember { mutableStateOf("") }
    var rollToText by remember { mutableStateOf("") }
    var leftClsMenuOpen by remember { mutableStateOf(false) }
    var rightClsMenuOpen by remember { mutableStateOf(false) }
    var bnFontMenuOpen by remember { mutableStateOf(false) }
    var enFontMenuOpen by remember { mutableStateOf(false) }
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
        append(" · মার্জিন ${formatInchesBn(config.safeMarginInches)}")
        append(" · $activeBnFontName")
        append(" · ${bn(totalSlipCount.toString())} কার্ড")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Compact Organized Settings Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            "আসন বিন্যাস সেটিংস",
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
                            config = defaultConfig
                            rollFromText = ""
                            rollToText = ""
                        },
                        modifier = Modifier.size(48.dp).testTag("seat_plan_reset_btn")
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
                        // Compact 3-Tab Pill Bar
                        val tabs = listOf("শ্রেণি ও রোল", "লেআউট ও ফন্ট", "শিরোনাম ও কক্ষ")
                        Surface(
                            color = SlateBg,
                            border = BorderStroke(1.dp, SlateOutline),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                tabs.forEachIndexed { idx, title ->
                                    val selected = activeSetupTab == idx
                                    Surface(
                                        onClick = { activeSetupTab = idx },
                                        color = if (selected) TealContainer else Color.Transparent,
                                        border = if (selected) BorderStroke(1.dp, TealPrimary.copy(alpha = 0.45f)) else null,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 42.dp)
                                            .testTag("seat_plan_tab_$idx")
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
                                            Text(
                                                title,
                                                fontSize = 11.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) TealDark else SlateMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        when (activeSetupTab) {
                            // TAB 0: শ্রেণি ও রোল
                            0 -> Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
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
                                            label = { Text(mode.labelBn, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
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
                                            label = { Text("সব শ্রেণি", fontSize = 11.sp) },
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
                                                    Text("${classShortBn(c, settings)} (${bn(cnt.toString())})", fontSize = 11.sp)
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
                                            val leftLabel = config.leftClass?.let { "${classNameBn(it, settings)} শ্রেণি" } ?: "নির্বাচন"
                                            OutlinedButton(
                                                onClick = { leftClsMenuOpen = true },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                            ) {
                                                Text("বাম: $leftLabel", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
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
                                            val rightLabel = config.rightClass?.let { "${classNameBn(it, settings)} শ্রেণি" } ?: "নির্বাচন"
                                            OutlinedButton(
                                                onClick = { rightClsMenuOpen = true },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                            ) {
                                                Text("ডান: $rightLabel", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
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

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                        label = { Text("রোল থেকে", fontSize = 10.5.sp) },
                                        placeholder = { Text("সব", fontSize = 10.5.sp) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(0.9f)
                                    )
                                    OutlinedTextField(
                                        value = rollToText,
                                        onValueChange = { raw ->
                                            val digits = fromBn(raw).filter { it.isDigit() }.take(4)
                                            rollToText = digits
                                            config = config.copy(rollTo = digits.toIntOrNull())
                                        },
                                        label = { Text("রোল পর্যন্ত", fontSize = 10.5.sp) },
                                        placeholder = { Text("সব", fontSize = 10.5.sp) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(0.9f)
                                    )
                                    CompactStepperBox(
                                        label = "ফাঁকা কার্ড",
                                        valueText = bn(config.extraBlankSlips.toString()),
                                        canDecrease = config.extraBlankSlips > 0,
                                        canIncrease = config.extraBlankSlips < 48,
                                        onDecrease = { config = config.copy(extraBlankSlips = config.extraBlankSlips - 1) },
                                        onIncrease = { config = config.copy(extraBlankSlips = config.extraBlankSlips + 1) },
                                        decreaseTag = "seat_plan_blank_minus",
                                        increaseTag = "seat_plan_blank_plus",
                                        modifier = Modifier.weight(1.35f)
                                    )
                                }
                            }

                            // TAB 1: লেআউট, মার্জিন, গ্যাপ ও ফন্ট
                            1 -> Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                // Grid Columns & Rows + Orientation
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
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
                                            label = { Text(bn(colCnt.toString()), fontSize = 11.sp) },
                                            colors = appFilterChipColors(),
                                            border = appFilterChipBorder(selected),
                                            modifier = Modifier.testTag("seat_plan_cols_$colCnt")
                                        )
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Text("রো:", fontSize = 11.sp, color = SlateMuted, fontWeight = FontWeight.SemiBold)
                                    listOf(4, 5, 6, 7, 8).forEach { rowCnt ->
                                        val selected = config.rows == rowCnt
                                        FilterChip(
                                            selected = selected,
                                            onClick = { config = config.copy(rows = rowCnt) },
                                            label = { Text(bn(rowCnt.toString()), fontSize = 11.sp) },
                                            colors = appFilterChipColors(),
                                            border = appFilterChipBorder(selected),
                                            modifier = Modifier.testTag("seat_plan_rows_$rowCnt")
                                        )
                                    }
                                }

                                // Margin (default 0.40") + Orientation compact row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CompactStepperBox(
                                        label = "পৃষ্ঠা মার্জিন (ডিফল্ট ০.৪০\")",
                                        valueText = "${formatInchesBn(config.safeMarginInches)} ইঞ্চি",
                                        canDecrease = config.safeMarginInches > 0.105f,
                                        canIncrease = config.safeMarginInches < 0.995f,
                                        onDecrease = {
                                            val next = ((config.safeMarginInches - 0.05f) * 20f).roundToInt() / 20f
                                            config = config.copy(marginInches = next.coerceIn(0.10f, 1.00f))
                                        },
                                        onIncrease = {
                                            val next = ((config.safeMarginInches + 0.05f) * 20f).roundToInt() / 20f
                                            config = config.copy(marginInches = next.coerceIn(0.10f, 1.00f))
                                        },
                                        decreaseTag = "seat_plan_margin_minus",
                                        increaseTag = "seat_plan_margin_plus",
                                        modifier = Modifier.weight(1.4f)
                                    )
                                    FilterChip(
                                        selected = config.orientation == SeatPlanOrientation.LANDSCAPE,
                                        onClick = {
                                            val next = if (config.orientation == SeatPlanOrientation.PORTRAIT) {
                                                SeatPlanOrientation.LANDSCAPE
                                            } else {
                                                SeatPlanOrientation.PORTRAIT
                                            }
                                            config = config.copy(orientation = next)
                                        },
                                        label = {
                                            Text(
                                                if (config.orientation == SeatPlanOrientation.PORTRAIT) "পোর্ট্রেট" else "ল্যান্ডস্কেপ",
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                        },
                                        colors = appFilterChipColors(),
                                        border = appFilterChipBorder(config.orientation == SeatPlanOrientation.LANDSCAPE),
                                        modifier = Modifier.weight(0.75f).testTag("seat_plan_orientation_toggle")
                                    )
                                }

                                // Horizontal & Vertical Gap controls
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CompactStepperBox(
                                        label = "অনুভূমিক গ্যাপ (H)",
                                        valueText = "${bn(config.safeHorizontalGapPt.roundToInt().toString())} pt",
                                        canDecrease = config.safeHorizontalGapPt > 0.5f,
                                        canIncrease = config.safeHorizontalGapPt < 35.5f,
                                        onDecrease = {
                                            config = config.copy(horizontalGapPt = (config.safeHorizontalGapPt - 2f).coerceIn(0f, 36f))
                                        },
                                        onIncrease = {
                                            config = config.copy(horizontalGapPt = (config.safeHorizontalGapPt + 2f).coerceIn(0f, 36f))
                                        },
                                        decreaseTag = "seat_plan_hgap_minus",
                                        increaseTag = "seat_plan_hgap_plus",
                                        modifier = Modifier.weight(1f)
                                    )
                                    CompactStepperBox(
                                        label = "উল্লম্ব গ্যাপ (V)",
                                        valueText = "${bn(config.safeVerticalGapPt.roundToInt().toString())} pt",
                                        canDecrease = config.safeVerticalGapPt > 0.5f,
                                        canIncrease = config.safeVerticalGapPt < 35.5f,
                                        onDecrease = {
                                            config = config.copy(verticalGapPt = (config.safeVerticalGapPt - 2f).coerceIn(0f, 36f))
                                        },
                                        onIncrease = {
                                            config = config.copy(verticalGapPt = (config.safeVerticalGapPt + 2f).coerceIn(0f, 36f))
                                        },
                                        decreaseTag = "seat_plan_vgap_minus",
                                        increaseTag = "seat_plan_vgap_plus",
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Font Selector (Bangla + English)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(Modifier.weight(1.15f)) {
                                        OutlinedButton(
                                            onClick = { bnFontMenuOpen = true },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("seat_plan_bn_font_btn")
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text("বাংলা ফন্ট", fontSize = 9.sp, color = SlateMuted)
                                                Text(
                                                    activeBnFontName,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = SlateText,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                                        }
                                        DropdownMenu(expanded = bnFontMenuOpen, onDismissRequest = { bnFontMenuOpen = false }) {
                                            BENGALI_PDF_FONT_LIST.forEach { font ->
                                                val previewFamily = remember(font.id) {
                                                    FontFamily(androidx.compose.ui.text.font.Typeface(PdfGen.resolveTypeface(ctx, font.id, false)))
                                                }
                                                val isSel = config.bnFontId == font.id
                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text(
                                                                font.labelBn.substringBefore(" ("),
                                                                fontFamily = previewFamily,
                                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                                color = if (isSel) TealPrimary else SlateText
                                                            )
                                                            Text(
                                                                "৩৮ নং প্রাথমিক বিদ্যালয় · আসন বিন্যাস",
                                                                fontFamily = previewFamily,
                                                                fontSize = 10.sp,
                                                                color = SlateMuted
                                                            )
                                                        }
                                                    },
                                                    onClick = {
                                                        config = config.copy(bnFontId = font.id)
                                                        bnFontMenuOpen = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Box(Modifier.weight(1f)) {
                                        OutlinedButton(
                                            onClick = { enFontMenuOpen = true },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("seat_plan_en_font_btn")
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text("ইংরেজি ফন্ট", fontSize = 9.sp, color = SlateMuted)
                                                Text(
                                                    activeEnFontName,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = SlateText,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                                        }
                                        DropdownMenu(expanded = enFontMenuOpen, onDismissRequest = { enFontMenuOpen = false }) {
                                            ENGLISH_PDF_FONT_LIST.forEach { font ->
                                                val previewFamily = remember(font.id) {
                                                    FontFamily(androidx.compose.ui.text.font.Typeface(PdfGen.resolveTypeface(ctx, font.id, false)))
                                                }
                                                val isSel = config.enFontId == font.id
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            font.labelBn.substringBefore(" ("),
                                                            fontFamily = previewFamily,
                                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSel) TealPrimary else SlateText
                                                        )
                                                    },
                                                    onClick = {
                                                        config = config.copy(enFontId = font.id)
                                                        enFontMenuOpen = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Font Size (%) + Corner Radius (pt)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CompactStepperBox(
                                        label = "ফন্ট সাইজ",
                                        valueText = "${bn(config.safeFontSizePercent.toString())}%",
                                        canDecrease = config.safeFontSizePercent > 70,
                                        canIncrease = config.safeFontSizePercent < 140,
                                        onDecrease = {
                                            config = config.copy(fontSizePercent = (config.safeFontSizePercent - 5).coerceIn(70, 140))
                                        },
                                        onIncrease = {
                                            config = config.copy(fontSizePercent = (config.safeFontSizePercent + 5).coerceIn(70, 140))
                                        },
                                        decreaseTag = "seat_plan_font_size_minus",
                                        increaseTag = "seat_plan_font_size_plus",
                                        modifier = Modifier.weight(1f)
                                    )
                                    val borderEnabled = config.borderStyle != SeatPlanBorderStyle.NONE
                                    val cornerLabel = if (config.safeCornerRadiusPt < 0.5f) {
                                        "০ (সোজা)"
                                    } else {
                                        "${bn(config.safeCornerRadiusPt.roundToInt().toString())} pt"
                                    }
                                    CompactStepperBox(
                                        label = "কোনার গোলাকার",
                                        valueText = if (borderEnabled) cornerLabel else "—",
                                        canDecrease = borderEnabled && config.safeCornerRadiusPt > 0.5f,
                                        canIncrease = borderEnabled && config.safeCornerRadiusPt < 27.5f,
                                        onDecrease = {
                                            config = config.copy(cornerRadiusPt = (config.safeCornerRadiusPt - 2f).coerceIn(0f, 28f))
                                        },
                                        onIncrease = {
                                            config = config.copy(cornerRadiusPt = (config.safeCornerRadiusPt + 2f).coerceIn(0f, 28f))
                                        },
                                        decreaseTag = "seat_plan_corner_minus",
                                        increaseTag = "seat_plan_corner_plus",
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Border Style Selector
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("বর্ডার:", fontSize = 11.sp, color = SlateMuted, fontWeight = FontWeight.SemiBold)
                                    SeatPlanBorderStyle.entries.forEach { bs ->
                                        val selected = config.borderStyle == bs
                                        FilterChip(
                                            selected = selected,
                                            onClick = { config = config.copy(borderStyle = bs) },
                                            label = { Text(bs.labelBn, fontSize = 10.5.sp) },
                                            colors = appFilterChipColors(),
                                            border = appFilterChipBorder(selected),
                                            modifier = Modifier.testTag("seat_plan_border_${bs.name.lowercase()}")
                                        )
                                    }
                                }

                                // Border Weight + Numerals + Card Toggles
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (config.borderStyle != SeatPlanBorderStyle.NONE) {
                                        SeatPlanBorderWeight.entries.forEach { bw ->
                                            val selected = config.borderWeight == bw
                                            FilterChip(
                                                selected = selected,
                                                onClick = { config = config.copy(borderWeight = bw) },
                                                label = { Text("লাইন: ${bw.labelBn}", fontSize = 10.5.sp) },
                                                colors = appFilterChipColors(),
                                                border = appFilterChipBorder(selected)
                                            )
                                        }
                                    }
                                    SeatPlanNumeralMode.entries.forEach { numMode ->
                                        val selected = config.numeralMode == numMode
                                        FilterChip(
                                            selected = selected,
                                            onClick = { config = config.copy(numeralMode = numMode) },
                                            label = {
                                                Text(
                                                    if (numMode == SeatPlanNumeralMode.BENGALI) "সংখ্যা: বাংলা" else "Number: Eng",
                                                    fontSize = 10.5.sp
                                                )
                                            },
                                            colors = appFilterChipColors(),
                                            border = appFilterChipBorder(selected)
                                        )
                                    }
                                    FilterChip(
                                        selected = config.showCutLines,
                                        onClick = { config = config.copy(showCutLines = !config.showCutLines) },
                                        label = { Text("কাটার দাগ", fontSize = 10.5.sp) },
                                        colors = appFilterChipColors(),
                                        border = appFilterChipBorder(config.showCutLines)
                                    )
                                    FilterChip(
                                        selected = config.boldStudentName,
                                        onClick = { config = config.copy(boldStudentName = !config.boldStudentName) },
                                        label = { Text("বোল্ড", fontSize = 10.5.sp) },
                                        colors = appFilterChipColors(),
                                        border = appFilterChipBorder(config.boldStudentName)
                                    )
                                    FilterChip(
                                        selected = config.padRollTwoDigits,
                                        onClick = { config = config.copy(padRollTwoDigits = !config.padRollTwoDigits) },
                                        label = { Text("০১ রোল", fontSize = 10.5.sp) },
                                        colors = appFilterChipColors(),
                                        border = appFilterChipBorder(config.padRollTwoDigits)
                                    )
                                }
                            }

                            // TAB 2: শিরোনাম, প্রান্তিক ও কক্ষ
                            else -> Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    (1..3).forEach { t ->
                                        val selected = config.term == t
                                        FilterChip(
                                            selected = selected,
                                            onClick = { config = config.copy(term = t) },
                                            label = { Text(termNameBn(t), fontSize = 11.sp) },
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
                                        label = { Text("উপ-শিরোনাম", fontSize = 10.5.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = config.roomLabel,
                                        onValueChange = { config = config.copy(roomLabel = it) },
                                        label = { Text("কক্ষ নং (ঐচ্ছিক)", fontSize = 10.5.sp) },
                                        placeholder = { Text("কক্ষ ০১", fontSize = 10.5.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                OutlinedTextField(
                                    value = config.customExamTitle,
                                    onValueChange = { config = config.copy(customExamTitle = it) },
                                    label = { Text("কাস্টম পরীক্ষার শিরোনাম", fontSize = 10.5.sp) },
                                    placeholder = { Text(SeatPlanPlanner.resolvedExamTitle(config.copy(customExamTitle = ""), settings), fontSize = 10.5.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = config.customSchoolName,
                                    onValueChange = { config = config.copy(customSchoolName = it) },
                                    label = { Text("কাস্টম বিদ্যালয়ের নাম", fontSize = 10.5.sp) },
                                    placeholder = { Text(settings.school.ifBlank { "বিদ্যালয়ের নাম" }, fontSize = 10.5.sp) },
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
                                        label = { Text("কাস্টম ঠিকানা", fontSize = 10.5.sp) },
                                        placeholder = { Text(settings.schoolAddress.ifBlank { "ঠিকানা" }, fontSize = 10.5.sp) },
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
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
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
