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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EventSeat
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.UnfoldMore
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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

private fun formatPtBn(value: Float): String {
    val roundedHalf = (value * 2f).roundToInt() / 2f
    val raw = if (abs(roundedHalf - roundedHalf.toInt()) < 0.05f) {
        roundedHalf.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", roundedHalf)
    }
    return "${bn(raw)} pt"
}

private val PART_FONT_SIZES = listOf(8.0f, 8.5f, 9.0f, 9.5f, 10.0f, 10.5f, 11.0f, 11.5f, 12.0f, 12.5f, 13.0f, 14.0f, 15.0f, 16.0f)
private val MARGIN_INCH_OPTIONS = listOf(0.15f, 0.20f, 0.25f, 0.30f, 0.35f, 0.40f, 0.45f, 0.50f, 0.60f, 0.75f)
private val GAP_PT_OPTIONS = listOf(0f, 4f, 6f, 8f, 10f, 12f, 14f, 16f, 20f, 24f, 28f, 32f)
private val CORNER_RADIUS_OPTIONS = listOf(0f, 4f, 8f, 12f, 15f, 18f, 20f, 22f, 25f, 28f)

@Composable
private fun SectionGroupCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = SlateBg.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, SlateOutline.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(11.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                title,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TealDark
            )
            content()
        }
    }
}

@Composable
private fun <T> SegmentedOptionBar(
    items: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    useLightActivePill: Boolean = false,
    tagPrefix: String = "seg"
) {
    Surface(
        color = SlateBg,
        border = BorderStroke(1.dp, SlateOutline),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items.forEachIndexed { idx, (value, label) ->
                val isSel = value == selected
                val bg = when {
                    isSel && useLightActivePill -> MaterialTheme.colorScheme.surface
                    isSel -> TealPrimary
                    else -> Color.Transparent
                }
                val fg = when {
                    isSel && useLightActivePill -> TealDark
                    isSel -> Color.White
                    else -> SlateMuted
                }
                val border = if (isSel && useLightActivePill) {
                    BorderStroke(1.dp, TealPrimary.copy(alpha = 0.4f))
                } else null
                Surface(
                    onClick = { onSelect(value) },
                    color = bg,
                    border = border,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
                        .testTag("${tagPrefix}_$idx")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text(
                            label,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = fg,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> CompactPickerBox(
    label: String,
    valueText: String,
    options: List<T>,
    selectedValue: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isFontDropdown: Boolean = false,
    testTag: String = "",
    optionFontFamily: ((T) -> FontFamily?)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            onClick = { if (enabled) expanded = true },
            enabled = enabled,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, if (enabled) SlateOutline else SlateOutline.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Column(Modifier.weight(1f)) {
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
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) SlateText else SlateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = if (isFontDropdown) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.UnfoldMore,
                    contentDescription = label,
                    tint = SlateMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { item ->
                val isSel = item == selectedValue
                val family = optionFontFamily?.invoke(item)
                DropdownMenuItem(
                    text = {
                        Text(
                            optionLabel(item),
                            fontFamily = family,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) TealPrimary else SlateText
                        )
                    },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CompactSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = SlateText,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = TealPrimary,
                uncheckedTrackColor = SlateSurfaceVariant
            ),
            modifier = Modifier.testTag(testTag)
        )
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
            schoolFontPt = 12.0f,
            headerMetaFontPt = 10.5f,
            studentNameFontPt = 11.5f,
            classRollFontPt = 11.0f,
            cornerRadiusPt = 18f,
            rollOrder = SeatPlanRollOrder.DESCENDING
        )
    }

    var config by remember(defaultConfig) { mutableStateOf(defaultConfig) }
    var setupExpanded by remember { mutableStateOf(true) }
    var activeSetupTab by remember { mutableIntStateOf(0) } // 0=শ্রেণি ও রোল, 1=লেআউট ও ফন্ট, 2=শিরোনাম ও কক্ষ
    var rollFromText by remember { mutableStateOf("") }
    var rollToText by remember { mutableStateOf("") }
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
        append(" · ${formatInchesBn(config.safeMarginInches)}")
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
            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { setupExpanded = !setupExpanded }
                        .testTag("seat_plan_setup_toggle"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(color = TealSoft, shape = RoundedCornerShape(8.dp)) {
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
                        // Top Segmented Tab Bar
                        SegmentedOptionBar(
                            items = listOf(
                                0 to "শ্রেণি ও রোল",
                                1 to "লেআউট ও ফন্ট",
                                2 to "শিরোনাম ও কক্ষ"
                            ),
                            selected = activeSetupTab,
                            onSelect = { activeSetupTab = it },
                            useLightActivePill = true,
                            tagPrefix = "seat_plan_tab"
                        )

                        when (activeSetupTab) {
                            // TAB 0: শ্রেণি ও রোল
                            0 -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SectionGroupCard(title = "মোড") {
                                    SegmentedOptionBar(
                                        items = SeatPlanPairMode.entries.map { it to it.labelBn },
                                        selected = config.pairMode,
                                        onSelect = { mode ->
                                            val nextCols = if (mode == SeatPlanPairMode.SIDE_BY_SIDE && config.columns < 2) 2 else config.columns
                                            config = config.copy(pairMode = mode, columns = nextCols)
                                        },
                                        tagPrefix = "seat_plan_mode"
                                    )
                                }

                                SectionGroupCard(title = "শ্রেণি") {
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
                                                label = { Text("সব", fontSize = 11.5.sp) },
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
                                                        Text("${classShortBn(c, settings)} (${bn(cnt.toString())})", fontSize = 11.5.sp)
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
                                            CompactPickerBox(
                                                label = "বাম কলাম",
                                                valueText = config.leftClass?.let { "${classNameBn(it, settings)} শ্রেণি" } ?: "নির্বাচন",
                                                options = allowedClasses,
                                                selectedValue = config.leftClass ?: (allowedClasses.firstOrNull() ?: 1),
                                                optionLabel = { c -> "${classNameBn(c, settings)} (${bn((countByClass[c] ?: 0).toString())} জন)" },
                                                onSelect = { config = config.copy(leftClass = it) },
                                                modifier = Modifier.weight(1f),
                                                testTag = "seat_plan_left_cls_btn"
                                            )
                                            CompactPickerBox(
                                                label = "ডান কলাম",
                                                valueText = config.rightClass?.let { "${classNameBn(it, settings)} শ্রেণি" } ?: "নির্বাচন",
                                                options = allowedClasses,
                                                selectedValue = config.rightClass ?: (allowedClasses.firstOrNull() ?: 1),
                                                optionLabel = { c -> "${classNameBn(c, settings)} (${bn((countByClass[c] ?: 0).toString())} জন)" },
                                                onSelect = { config = config.copy(rightClass = it) },
                                                modifier = Modifier.weight(1f),
                                                testTag = "seat_plan_right_cls_btn"
                                            )
                                        }
                                    }
                                }

                                SectionGroupCard(title = "রোল ক্রম") {
                                    SegmentedOptionBar(
                                        items = SeatPlanRollOrder.entries.map { it to it.labelBn },
                                        selected = config.rollOrder,
                                        onSelect = { config = config.copy(rollOrder = it) },
                                        tagPrefix = "seat_plan_order"
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, SlateOutline),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                "ফাঁকা কার্ড",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = SlateText,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = {
                                                    if (config.extraBlankSlips > 0) {
                                                        config = config.copy(extraBlankSlips = config.extraBlankSlips - 1)
                                                    }
                                                },
                                                enabled = config.extraBlankSlips > 0,
                                                modifier = Modifier.size(48.dp).testTag("seat_plan_blank_minus")
                                            ) {
                                                Icon(Icons.Rounded.Remove, contentDescription = "ফাঁকা কার্ড কমান", modifier = Modifier.size(16.dp))
                                            }
                                            Text(
                                                bn(config.extraBlankSlips.toString()),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SlateText,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )
                                            IconButton(
                                                onClick = {
                                                    if (config.extraBlankSlips < 48) {
                                                        config = config.copy(extraBlankSlips = config.extraBlankSlips + 1)
                                                    }
                                                },
                                                enabled = config.extraBlankSlips < 48,
                                                modifier = Modifier.size(48.dp).testTag("seat_plan_blank_plus")
                                            ) {
                                                Icon(Icons.Rounded.Add, contentDescription = "ফাঁকা কার্ড বাড়ান", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            // TAB 1: লেআউট ও ফন্ট
                            1 -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // পৃষ্ঠা
                                SectionGroupCard(title = "পৃষ্ঠা") {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val colOptions = if (config.pairMode == SeatPlanPairMode.SIDE_BY_SIDE) listOf(2, 3) else listOf(1, 2, 3)
                                        CompactPickerBox(
                                            label = "কলাম",
                                            valueText = bn(config.safeColumns.toString()),
                                            options = colOptions,
                                            selectedValue = config.safeColumns,
                                            optionLabel = { "${bn(it.toString())} কলাম" },
                                            onSelect = { config = config.copy(columns = it) },
                                            modifier = Modifier.weight(1f),
                                            testTag = "seat_plan_cols_picker"
                                        )
                                        CompactPickerBox(
                                            label = "সারি",
                                            valueText = bn(config.safeRows.toString()),
                                            options = (3..10).toList(),
                                            selectedValue = config.safeRows,
                                            optionLabel = { "${bn(it.toString())} সারি" },
                                            onSelect = { config = config.copy(rows = it) },
                                            modifier = Modifier.weight(1f),
                                            testTag = "seat_plan_rows_picker"
                                        )
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CompactPickerBox(
                                            label = "মার্জিন",
                                            valueText = formatInchesBn(config.safeMarginInches),
                                            options = MARGIN_INCH_OPTIONS,
                                            selectedValue = config.safeMarginInches,
                                            optionLabel = { formatInchesBn(it) + if (abs(it - 0.40f) < 0.01f) " (ডিফল্ট)" else "" },
                                            onSelect = { config = config.copy(marginInches = it) },
                                            modifier = Modifier.weight(0.9f),
                                            testTag = "seat_plan_margin_picker"
                                        )
                                        SegmentedOptionBar(
                                            items = listOf(
                                                SeatPlanOrientation.PORTRAIT to "পোর্ট্রেট",
                                                SeatPlanOrientation.LANDSCAPE to "ল্যান্ডস্কেপ"
                                            ),
                                            selected = config.orientation,
                                            onSelect = { config = config.copy(orientation = it) },
                                            modifier = Modifier.weight(1.25f),
                                            tagPrefix = "seat_plan_orientation"
                                        )
                                    }
                                }

                                // ফাঁক ও ফন্ট
                                SectionGroupCard(title = "ফাঁক ও ফন্ট") {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CompactPickerBox(
                                            label = "অনুভূমিক",
                                            valueText = formatPtBn(config.safeHorizontalGapPt),
                                            options = GAP_PT_OPTIONS,
                                            selectedValue = config.safeHorizontalGapPt,
                                            optionLabel = { formatPtBn(it) },
                                            onSelect = { config = config.copy(horizontalGapPt = it) },
                                            modifier = Modifier.weight(1f),
                                            testTag = "seat_plan_hgap_picker"
                                        )
                                        CompactPickerBox(
                                            label = "উল্লম্ব",
                                            valueText = formatPtBn(config.safeVerticalGapPt),
                                            options = GAP_PT_OPTIONS,
                                            selectedValue = config.safeVerticalGapPt,
                                            optionLabel = { formatPtBn(it) },
                                            onSelect = { config = config.copy(verticalGapPt = it) },
                                            modifier = Modifier.weight(1f),
                                            testTag = "seat_plan_vgap_picker"
                                        )
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CompactPickerBox(
                                            label = "বাংলা ফন্ট",
                                            valueText = activeBnFontName,
                                            options = BENGALI_PDF_FONT_LIST.map { it.id },
                                            selectedValue = config.bnFontId,
                                            optionLabel = { id ->
                                                BENGALI_PDF_FONT_LIST.find { it.id == id }?.labelBn?.substringBefore(" (") ?: id
                                            },
                                            onSelect = { config = config.copy(bnFontId = it) },
                                            isFontDropdown = true,
                                            modifier = Modifier.weight(1.1f),
                                            testTag = "seat_plan_bn_font_btn",
                                            optionFontFamily = { id ->
                                                FontFamily(androidx.compose.ui.text.font.Typeface(PdfGen.resolveTypeface(ctx, id, false)))
                                            }
                                        )
                                        CompactPickerBox(
                                            label = "ইংরেজি ফন্ট",
                                            valueText = activeEnFontName,
                                            options = ENGLISH_PDF_FONT_LIST.map { it.id },
                                            selectedValue = config.enFontId,
                                            optionLabel = { id ->
                                                ENGLISH_PDF_FONT_LIST.find { it.id == id }?.labelBn?.substringBefore(" (") ?: id
                                            },
                                            onSelect = { config = config.copy(enFontId = it) },
                                            isFontDropdown = true,
                                            modifier = Modifier.weight(1f),
                                            testTag = "seat_plan_en_font_btn",
                                            optionFontFamily = { id ->
                                                FontFamily(androidx.compose.ui.text.font.Typeface(PdfGen.resolveTypeface(ctx, id, false)))
                                            }
                                        )
                                    }
                                }

                                // অংশভেদে ফন্ট সাইজ ও বোল্ড
                                SectionGroupCard(title = "অংশভেদে ফন্ট সাইজ ও বোল্ড") {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CompactPickerBox(
                                            label = "বিদ্যালয়ের নাম",
                                            valueText = formatPtBn(config.safeSchoolFontPt),
                                            options = PART_FONT_SIZES,
                                            selectedValue = config.safeSchoolFontPt,
                                            optionLabel = { formatPtBn(it) },
                                            onSelect = { config = config.copy(schoolFontPt = it) },
                                            modifier = Modifier.weight(1f),
                                            testTag = "seat_plan_size_school"
                                        )
                                        CompactPickerBox(
                                            label = "ঠিকানা ও শিরোনাম",
                                            valueText = formatPtBn(config.safeHeaderMetaFontPt),
                                            options = PART_FONT_SIZES,
                                            selectedValue = config.safeHeaderMetaFontPt,
                                            optionLabel = { formatPtBn(it) },
                                            onSelect = { config = config.copy(headerMetaFontPt = it) },
                                            modifier = Modifier.weight(1f),
                                            testTag = "seat_plan_size_header"
                                        )
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CompactPickerBox(
                                            label = "শিক্ষার্থীর নাম",
                                            valueText = formatPtBn(config.safeStudentNameFontPt),
                                            options = PART_FONT_SIZES,
                                            selectedValue = config.safeStudentNameFontPt,
                                            optionLabel = { formatPtBn(it) },
                                            onSelect = { config = config.copy(studentNameFontPt = it) },
                                            modifier = Modifier.weight(1f),
                                            testTag = "seat_plan_size_name"
                                        )
                                        CompactPickerBox(
                                            label = "শ্রেণি ও রোল",
                                            valueText = formatPtBn(config.safeClassRollFontPt),
                                            options = PART_FONT_SIZES,
                                            selectedValue = config.safeClassRollFontPt,
                                            optionLabel = { formatPtBn(it) },
                                            onSelect = { config = config.copy(classRollFontPt = it) },
                                            modifier = Modifier.weight(1f),
                                            testTag = "seat_plan_size_meta"
                                        )
                                    }
                                    Text(
                                        "বোল্ড করবেন কোন অংশ:",
                                        fontSize = 10.sp,
                                        color = SlateMuted
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        FilterChip(
                                            selected = config.boldSchoolName,
                                            onClick = { config = config.copy(boldSchoolName = !config.boldSchoolName) },
                                            label = { Text("বিদ্যালয়", fontSize = 10.5.sp) },
                                            colors = appFilterChipColors(),
                                            border = appFilterChipBorder(config.boldSchoolName),
                                            modifier = Modifier.testTag("seat_plan_bold_school")
                                        )
                                        FilterChip(
                                            selected = config.boldAddress,
                                            onClick = { config = config.copy(boldAddress = !config.boldAddress) },
                                            label = { Text("ঠিকানা", fontSize = 10.5.sp) },
                                            colors = appFilterChipColors(),
                                            border = appFilterChipBorder(config.boldAddress),
                                            modifier = Modifier.testTag("seat_plan_bold_address")
                                        )
                                        FilterChip(
                                            selected = config.boldExamTitle,
                                            onClick = { config = config.copy(boldExamTitle = !config.boldExamTitle) },
                                            label = { Text("পরীক্ষা ও বিন্যাস", fontSize = 10.5.sp) },
                                            colors = appFilterChipColors(),
                                            border = appFilterChipBorder(config.boldExamTitle),
                                            modifier = Modifier.testTag("seat_plan_bold_exam")
                                        )
                                        FilterChip(
                                            selected = config.boldStudentName,
                                            onClick = { config = config.copy(boldStudentName = !config.boldStudentName) },
                                            label = { Text("নাম", fontSize = 10.5.sp) },
                                            colors = appFilterChipColors(),
                                            border = appFilterChipBorder(config.boldStudentName),
                                            modifier = Modifier.testTag("seat_plan_bold_name")
                                        )
                                        FilterChip(
                                            selected = config.boldClassRoll,
                                            onClick = { config = config.copy(boldClassRoll = !config.boldClassRoll) },
                                            label = { Text("শ্রেণি ও রোল", fontSize = 10.5.sp) },
                                            colors = appFilterChipColors(),
                                            border = appFilterChipBorder(config.boldClassRoll),
                                            modifier = Modifier.testTag("seat_plan_bold_meta")
                                        )
                                    }
                                }

                                // কার্ড স্টাইল
                                SectionGroupCard(title = "কার্ড স্টাইল") {
                                    SegmentedOptionBar(
                                        items = SeatPlanBorderStyle.entries.map { it to it.labelBn },
                                        selected = config.borderStyle,
                                        onSelect = { config = config.copy(borderStyle = it) },
                                        tagPrefix = "seat_plan_border"
                                    )
                                    if (config.borderStyle != SeatPlanBorderStyle.NONE) {
                                        SegmentedOptionBar(
                                            items = SeatPlanBorderWeight.entries.map { it to it.labelBn },
                                            selected = config.borderWeight,
                                            onSelect = { config = config.copy(borderWeight = it) },
                                            tagPrefix = "seat_plan_border_weight"
                                        )
                                        CompactPickerBox(
                                            label = "কোনার গোলাকার",
                                            valueText = if (config.safeCornerRadiusPt < 0.5f) "০ pt (সোজা)" else formatPtBn(config.safeCornerRadiusPt),
                                            options = CORNER_RADIUS_OPTIONS,
                                            selectedValue = config.safeCornerRadiusPt,
                                            optionLabel = { r ->
                                                when {
                                                    r < 0.5f -> "০ pt (সোজা)"
                                                    abs(r - 18f) < 0.5f -> "১৮ pt (বেশি গোলাকার)"
                                                    else -> formatPtBn(r)
                                                }
                                            },
                                            onSelect = { config = config.copy(cornerRadiusPt = it) },
                                            testTag = "seat_plan_corner_picker"
                                        )
                                    }
                                }

                                // অপশন
                                SectionGroupCard(title = "অপশন") {
                                    CompactSwitchRow(
                                        label = "সংখ্যা বাংলা",
                                        checked = config.numeralMode == SeatPlanNumeralMode.BENGALI,
                                        onCheckedChange = { bnOn ->
                                            config = config.copy(
                                                numeralMode = if (bnOn) SeatPlanNumeralMode.BENGALI else SeatPlanNumeralMode.ENGLISH
                                            )
                                        },
                                        testTag = "seat_plan_opt_bn_num"
                                    )
                                    HorizontalDivider(color = SlateOutline.copy(alpha = 0.4f))
                                    CompactSwitchRow(
                                        label = "কাটার দাগ",
                                        checked = config.showCutLines,
                                        onCheckedChange = { config = config.copy(showCutLines = it) },
                                        testTag = "seat_plan_opt_cut_lines"
                                    )
                                    HorizontalDivider(color = SlateOutline.copy(alpha = 0.4f))
                                    CompactSwitchRow(
                                        label = "০১ রোল (২ ডিজিট)",
                                        checked = config.padRollTwoDigits,
                                        onCheckedChange = { config = config.copy(padRollTwoDigits = it) },
                                        testTag = "seat_plan_opt_pad_roll"
                                    )
                                }
                            }

                            // TAB 2: শিরোনাম ও কক্ষ
                            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SectionGroupCard(title = "প্রান্তিক") {
                                    SegmentedOptionBar(
                                        items = (1..3).map { it to termNameBn(it) },
                                        selected = config.term,
                                        onSelect = { config = config.copy(term = it) },
                                        tagPrefix = "seat_plan_term"
                                    )
                                }

                                SectionGroupCard(title = "শিরোনাম ও কক্ষ") {
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
                                            placeholder = { Text("কক্ষ ০১", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
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
                                    OutlinedTextField(
                                        value = config.customSchoolAddress,
                                        onValueChange = { config = config.copy(customSchoolAddress = it) },
                                        enabled = config.showSchoolAddress,
                                        label = { Text("কাস্টম ঠিকানা", fontSize = 11.sp) },
                                        placeholder = { Text(settings.schoolAddress.ifBlank { "ঠিকানা" }, fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    CompactSwitchRow(
                                        label = "ঠিকানা দেখাও",
                                        checked = config.showSchoolAddress,
                                        onCheckedChange = { config = config.copy(showSchoolAddress = it) },
                                        testTag = "seat_plan_show_address_switch"
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
