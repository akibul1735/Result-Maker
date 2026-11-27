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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.TableView
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kgps.result.BENGALI_PDF_FONT_LIST
import com.kgps.result.ENGLISH_PDF_FONT_LIST
import com.kgps.result.PdfGen
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.TabulationConfig
import com.kgps.result.TabulationHeaderStyle
import com.kgps.result.TabulationNumeralMode
import com.kgps.result.TabulationPageSize
import com.kgps.result.TabulationPageSpec
import com.kgps.result.TabulationPdfGen
import com.kgps.result.TabulationPlanner
import com.kgps.result.TabulationSortOrder
import com.kgps.result.TabulationSubjectDisplay
import com.kgps.result.TabulationTermScope
import com.kgps.result.activeClassIds
import com.kgps.result.bn
import com.kgps.result.classShortBn
import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.ui.theme.RoseDanger
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateSurfaceVariant
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft
import com.kgps.result.ui.theme.appFilterChipBorder
import com.kgps.result.ui.theme.appFilterChipColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs

private val TAB_MARGIN_OPTIONS = listOf(0.20f, 0.25f, 0.30f, 0.35f, 0.40f, 0.50f, 0.60f)
private val TAB_ROWS_OPTIONS = listOf(15, 18, 20, 22, 25, 28, 30, 35)

private fun clampTabulationPanOffset(offset: Offset, scale: Float, size: IntSize): Offset {
    if (scale <= 1.02f || size.width <= 0 || size.height <= 0) return Offset.Zero
    val maxX = (size.width * (scale - 1f)) / 2f
    val maxY = (size.height * (scale - 1f)) / 2f + size.height * 0.25f
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY)
    )
}

private fun formatTabInchesBn(inches: Float): String =
    bn(String.format(Locale.US, "%.2f", inches)) + "\""

@Composable
private fun TabSectionCard(
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
private fun <T> TabSegmentedBar(
    items: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    useLightActivePill: Boolean = false,
    tagPrefix: String = "tab_seg"
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
private fun <T> TabPickerBox(
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
private fun TabSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
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
    if (onBack != null) {
        BackHandler { onBack() }
    }
    val ctx = LocalContext.current
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
    var setupExpanded by remember { mutableStateOf(true) }
    var activeSetupTab by remember { mutableIntStateOf(0) } // 0=শ্রেণি ও প্রান্তিক, 1=পৃষ্ঠা ও কলাম, 2=ফন্ট ও স্টাইল
    var showFullscreen by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val pages = remember(students, settings, config, allowedClasses) {
        TabulationPlanner.buildPages(
            students = students,
            settings = settings,
            config = config,
            allowedClassIds = allowedClasses
        )
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
    LaunchedEffect(previewPageIdx, pages.size, config.pageSize) {
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
                TabulationPdfGen.renderPageBitmap(
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

    val totalStudentCount = countByClass[config.selectedClass] ?: 0
    val summaryLine = buildString {
        append("${classShortBn(config.selectedClass, settings)} (${bn(totalStudentCount.toString())})")
        append(" · ${config.termScope.shortLabelBn}")
        append(" · ${config.pageSize.labelBn}")
        append(" · ${bn(pages.size.toString())} পৃষ্ঠা")
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
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { setupExpanded = !setupExpanded }
                        .testTag("tabulation_setup_toggle"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(color = TealSoft, shape = RoundedCornerShape(8.dp)) {
                        Icon(
                            Icons.Rounded.TableView,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.padding(6.dp).size(18.dp)
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "ফলাফল টেবিল সেটিংস",
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
                        onClick = { showResetConfirm = true },
                        modifier = Modifier.size(48.dp).testTag("tabulation_reset_btn")
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
                        TabSegmentedBar(
                            items = listOf(
                                0 to "শ্রেণি ও প্রান্তিক",
                                1 to "পৃষ্ঠা ও কলাম",
                                2 to "ফন্ট ও স্টাইল"
                            ),
                            selected = activeSetupTab,
                            onSelect = { activeSetupTab = it },
                            useLightActivePill = true,
                            tagPrefix = "tabulation_tab"
                        )

                        when (activeSetupTab) {
                            // TAB 0: শ্রেণি ও প্রান্তিক
                            0 -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                TabSectionCard(title = "শ্রেণি নির্বাচন") {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        allowedClasses.forEach { cId ->
                                            val isSel = config.selectedClass == cId
                                            val count = countByClass[cId] ?: 0
                                            FilterChip(
                                                selected = isSel,
                                                onClick = { config = config.copy(selectedClass = cId) },
                                                label = {
                                                    Text(
                                                        "${classShortBn(cId, settings)} (${bn(count.toString())})",
                                                        fontSize = 11.5.sp
                                                    )
                                                },
                                                colors = appFilterChipColors(),
                                                border = appFilterChipBorder(isSel),
                                                modifier = Modifier
                                                    .heightIn(min = 48.dp)
                                                    .testTag("tabulation_cls_$cId")
                                            )
                                        }
                                    }
                                }

                                TabSectionCard(title = "প্রান্তিক ও বিষয়ের কলাম") {
                                    TabSegmentedBar(
                                        items = TabulationTermScope.entries.map { it to it.shortLabelBn },
                                        selected = config.termScope,
                                        onSelect = { scope -> config = config.copy(termScope = scope) },
                                        tagPrefix = "tabulation_term"
                                    )
                                    if (config.termScope != TabulationTermScope.FINAL_AVG) {
                                        TabSegmentedBar(
                                            items = TabulationSubjectDisplay.entries.map { it to it.labelBn },
                                            selected = config.subjectDisplay,
                                            onSelect = { disp -> config = config.copy(subjectDisplay = disp) },
                                            tagPrefix = "tabulation_subj_disp"
                                        )
                                    } else {
                                        Text(
                                            "চূড়ান্ত গড় মোডে প্রতিটি বিষয়ের বার্ষিক গড় মোট নম্বর দেখাবে।",
                                            fontSize = 11.sp,
                                            color = SlateMuted
                                        )
                                    }
                                }

                                TabSectionCard(title = "তালিকার ক্রম") {
                                    TabSegmentedBar(
                                        items = TabulationSortOrder.entries.map { it to it.labelBn },
                                        selected = config.sortOrder,
                                        onSelect = { config = config.copy(sortOrder = it) },
                                        tagPrefix = "tabulation_sort"
                                    )
                                }
                            }

                            // TAB 1: পৃষ্ঠা ও কলাম
                            1 -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                TabSectionCard(title = "কাগজের মাপ ও মার্জিন") {
                                    TabSegmentedBar(
                                        items = TabulationPageSize.entries.map { it to it.labelBn },
                                        selected = config.pageSize,
                                        onSelect = { config = config.copy(pageSize = it) },
                                        tagPrefix = "tabulation_pagesize"
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TabPickerBox(
                                            label = "মার্জিন",
                                            valueText = formatTabInchesBn(config.safeMarginInches),
                                            options = TAB_MARGIN_OPTIONS,
                                            selectedValue = config.safeMarginInches,
                                            optionLabel = { formatTabInchesBn(it) },
                                            onSelect = { config = config.copy(marginInches = it) },
                                            modifier = Modifier.weight(1f),
                                            testTag = "tabulation_margin_picker"
                                        )
                                        TabPickerBox(
                                            label = "প্রতি পৃষ্ঠায় শিক্ষার্থী",
                                            valueText = "${bn(config.safeRowsPerPage.toString())} জন",
                                            options = TAB_ROWS_OPTIONS,
                                            selectedValue = config.safeRowsPerPage,
                                            optionLabel = { "${bn(it.toString())} জন" },
                                            onSelect = { config = config.copy(rowsPerPage = it) },
                                            modifier = Modifier.weight(1f),
                                            testTag = "tabulation_rows_picker"
                                        )
                                    }
                                }

                                val hasSubHeaders = config.termScope != TabulationTermScope.FINAL_AVG &&
                                    config.subjectDisplay == TabulationSubjectDisplay.CONT_WR_TOT
                                if (hasSubHeaders) {
                                    TabSectionCard(title = "ধা / সা / মোট হেডার ধরন") {
                                        TabSegmentedBar(
                                            items = TabulationHeaderStyle.entries.map { it to it.labelBn },
                                            selected = config.headerStyle,
                                            onSelect = { config = config.copy(headerStyle = it) },
                                            tagPrefix = "tabulation_hdr_style"
                                        )
                                    }
                                }

                                TabSectionCard(title = "ডানদিকের ফলাফল কলাম") {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        FilterChip(
                                            selected = config.showTotal,
                                            onClick = { config = config.copy(showTotal = !config.showTotal) },
                                            label = { Text("মোট নম্বর", fontSize = 11.sp) },
                                            colors = appFilterChipColors(),
                                            border = appFilterChipBorder(config.showTotal),
                                            modifier = Modifier.heightIn(min = 48.dp).testTag("tab_col_total")
                                        )
                                        FilterChip(
                                            selected = config.showHighest,
                                            onClick = { config = config.copy(showHighest = !config.showHighest) },
                                            label = { Text("শ্রেণিতে সর্বোচ্চ", fontSize = 11.sp) },
                                            colors = appFilterChipColors(),
                                            border = appFilterChipBorder(config.showHighest),
                                            modifier = Modifier.heightIn(min = 48.dp).testTag("tab_col_highest")
                                        )
                                        FilterChip(
                                            selected = config.showPercentage,
                                            onClick = { config = config.copy(showPercentage = !config.showPercentage) },
                                            label = { Text("শতকরা (%)", fontSize = 11.sp) },
                                            colors = appFilterChipColors(),
                                            border = appFilterChipBorder(config.showPercentage),
                                            modifier = Modifier.heightIn(min = 48.dp).testTag("tab_col_pct")
                                        )
                                        FilterChip(
                                            selected = config.showGrade,
                                            onClick = { config = config.copy(showGrade = !config.showGrade) },
                                            label = { Text("গ্রেড", fontSize = 11.sp) },
                                            colors = appFilterChipColors(),
                                            border = appFilterChipBorder(config.showGrade),
                                            modifier = Modifier.heightIn(min = 48.dp).testTag("tab_col_grade")
                                        )
                                        FilterChip(
                                            selected = config.showMeritRank,
                                            onClick = { config = config.copy(showMeritRank = !config.showMeritRank) },
                                            label = { Text("মেধাক্রম", fontSize = 11.sp) },
                                            colors = appFilterChipColors(),
                                            border = appFilterChipBorder(config.showMeritRank),
                                            modifier = Modifier.heightIn(min = 48.dp).testTag("tab_col_merit")
                                        )
                                    }
                                }

                                TabSectionCard(title = "অতিরিক্ত অপশন") {
                                    if (hasSubHeaders) {
                                        TabSwitchRow(
                                            label = "স্বাস্থ্য ও শিল্প বিষয়ে শুধু মোট দেখাও",
                                            checked = config.skipWrittenOnHalfSubjects,
                                            onCheckedChange = { config = config.copy(skipWrittenOnHalfSubjects = it) },
                                            testTag = "tab_opt_half_subj"
                                        )
                                        HorizontalDivider(color = SlateOutline.copy(alpha = 0.4f))
                                    }
                                    TabSwitchRow(
                                        label = "শেষ পৃষ্ঠায় পাসের হার ও সারাংশ দেখাও",
                                        checked = config.showSummaryFooter,
                                        onCheckedChange = { config = config.copy(showSummaryFooter = it) },
                                        testTag = "tab_opt_footer"
                                    )
                                }
                            }

                            // TAB 2: ফন্ট ও স্টাইল
                            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                TabSectionCard(title = "ফন্ট ও সংখ্যা") {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TabPickerBox(
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
                                            testTag = "tabulation_bn_font",
                                            optionFontFamily = { id ->
                                                FontFamily(androidx.compose.ui.text.font.Typeface(PdfGen.resolveTypeface(ctx, id, false)))
                                            }
                                        )
                                        TabPickerBox(
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
                                            testTag = "tabulation_en_font",
                                            optionFontFamily = { id ->
                                                FontFamily(androidx.compose.ui.text.font.Typeface(PdfGen.resolveTypeface(ctx, id, false)))
                                            }
                                        )
                                    }
                                    TabSegmentedBar(
                                        items = TabulationNumeralMode.entries.map { it to it.labelBn },
                                        selected = config.numeralMode,
                                        onSelect = { config = config.copy(numeralMode = it) },
                                        tagPrefix = "tabulation_numeral"
                                    )
                                }

                                TabSectionCard(title = "কাস্টম শিরোনাম (ঐচ্ছিক)") {
                                    OutlinedTextField(
                                        value = config.customTitle,
                                        onValueChange = { config = config.copy(customTitle = it) },
                                        label = { Text("ফলাফল টেবিলের শিরোনাম", fontSize = 11.sp) },
                                        placeholder = { Text("যেমন: ৩য় প্রান্তিক মূল্যায়ন ফলাফল - ২০২৬", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("tabulation_custom_title")
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
                                modifier = Modifier.size(48.dp).testTag("tabulation_fullscreen_btn")
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
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "নির্বাচিত শ্রেণিতে কোনো শিক্ষার্থী পাওয়া যায়নি।",
                                fontSize = 12.5.sp,
                                color = SlateMuted
                            )
                        }
                    }
                } else {
                    val ratio = config.pageSize.widthPt.toFloat() / config.pageSize.heightPt.toFloat()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(ratio)
                            .background(SlateSurfaceVariant, RoundedCornerShape(10.dp))
                            .clipToBounds()
                            .onSizeChanged { previewSize = it }
                            .pointerInput(previewPageIdx, pages.size, config.pageSize) {
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
                            .pointerInput(previewPageIdx, pages.size, config.pageSize) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    do {
                                        val event = awaitPointerEvent()
                                        val pointerCount = event.changes.count { it.pressed }
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        if (pointerCount >= 2 || zoomScale > 1.02f) {
                                            val nextScale = (zoomScale * zoomChange).coerceIn(1f, 3.8f)
                                            zoomScale = nextScale
                                            panOffset = clampTabulationPanOffset(
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
                                contentDescription = "ফলাফল টেবিল প্রিভিউ",
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
        val exportName = "ফলাফল-টেবিল-${classShortBn(config.selectedClass, settings)}-${config.termScope.shortLabelBn}.pdf"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onPrint(pages, "ফলাফল টেবিল (${classShortBn(config.selectedClass, settings)})") },
                enabled = pages.isNotEmpty(),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.6f)),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("tabulation_print_btn")
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
                    .testTag("tabulation_export_btn")
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
                                            val ns = (fsScale * z).coerceIn(1f, 4.5f)
                                            fsScale = ns
                                            fsPan = clampTabulationPanOffset(fsPan + p, ns, fsSize)
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
                                contentDescription = "পূর্ণ স্ক্রিন ফলাফল টেবিল",
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
                        zoomScale = 1f
                        panOffset = Offset.Zero
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
