@file:OptIn(ExperimentalMaterial3Api::class)

package com.kgps.result.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kgps.result.BlankColFill
import com.kgps.result.BlankNumeralStyle
import com.kgps.result.BlankSheetCatalog
import com.kgps.result.BlankSheetColumn
import com.kgps.result.BlankSheetConfig
import com.kgps.result.BlankSheetOrientation
import com.kgps.result.BlankSheetPageSpec
import com.kgps.result.BlankSheetPdfGen
import com.kgps.result.BlankSheetPlanner
import com.kgps.result.BlankSheetPreset
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.activeClassIds
import com.kgps.result.bn
import com.kgps.result.classNameBn
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

private fun clampBlankPanOffset(offset: Offset, scale: Float, size: IntSize): Offset {
    if (scale <= 1.02f || size.width <= 0 || size.height <= 0) return Offset.Zero
    val maxX = (size.width * (scale - 1f)) / 2f
    val maxY = (size.height * (scale - 1f)) / 2f + size.height * 0.25f
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY)
    )
}

@Composable
fun BlankSheetPdfScreen(
    students: List<Student>,
    settings: Settings,
    permissions: TeacherPermissions,
    initialClass: Int,
    onExportPdf: (pages: List<BlankSheetPageSpec>, orientation: BlankSheetOrientation, fileName: String) -> Unit,
    onPrint: (pages: List<BlankSheetPageSpec>, orientation: BlankSheetOrientation, jobName: String) -> Unit,
    onCacheTemplates: (List<BlankSheetPageSpec>) -> Unit = {},
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (onBack != null) {
        BackHandler { onBack() }
    }
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val availableClassIds = remember(settings.customClasses) { activeClassIds(settings) }
    val allowedClasses = remember(permissions, availableClassIds) {
        permissions.effectiveClasses(availableClassIds)
    }

    var config by remember(initialClass, allowedClasses) {
        val initCls = if (initialClass in allowedClasses) initialClass else allowedClasses.firstOrNull()
        mutableStateOf(
            BlankSheetConfig(
                selectedClass = initCls,
                subjectKey = "BAN",
                term = 1,
                columns = BlankSheetCatalog.DEFAULT_COLUMNS,
                rowsPerPage = 20,
                orientation = BlankSheetOrientation.PORTRAIT
            )
        )
    }

    var setupExpanded by remember { mutableStateOf(true) }
    var showMoreOptions by remember { mutableStateOf(false) }
    var showCustomColumnInput by remember { mutableStateOf(false) }
    var customColText by remember { mutableStateOf("") }
    var focusedColId by remember { mutableStateOf<String?>(null) }

    var classMenuOpen by remember { mutableStateOf(false) }
    var subjectMenuOpen by remember { mutableStateOf(false) }
    var termMenuOpen by remember { mutableStateOf(false) }
    var rowsMenuOpen by remember { mutableStateOf(false) }
    var showFullscreen by remember { mutableStateOf(false) }

    val pages = remember(students, settings, config, allowedClasses) {
        BlankSheetPlanner.buildPages(
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
    LaunchedEffect(pages) {
        if (pages.isNotEmpty()) {
            onCacheTemplates(pages)
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
        key2 = settings,
        key3 = config.orientation
    ) {
        value = if (activePage == null) {
            null
        } else {
            withContext(Dispatchers.Default) {
                BlankSheetPdfGen.renderPageBitmap(
                    ctx = ctx,
                    page = activePage,
                    settings = settings,
                    orientation = config.orientation,
                    scale = 2.0f
                )
            }
        }
    }

    val selectedClassLabel = remember(config.selectedClass, settings, students, allowedClasses) {
        val c = config.selectedClass
        if (c == null) {
            "সব শ্রেণি"
        } else {
            "${classNameBn(c, settings)} শ্রেণি"
        }
    }

    val isThreeTermMarks = remember(config.columns) {
        BlankSheetCatalog.isThreeTermMarksActive(config.columns)
    }
    val hasThreeTermTotal = remember(config.columns) {
        BlankSheetCatalog.hasThreeTermTotals(config.columns)
    }
    val isThreeTermTraits = remember(config.columns) {
        BlankSheetCatalog.isThreeTermAttendanceTraitsActive(config.columns)
    }
    val isClassLevelTraitsOnly = remember(config.columns) {
        isThreeTermTraits && !isThreeTermMarks && config.columns.none { it.compKey != null }
    }
    val hasPerRowDaysCol = remember(config.columns) {
        config.columns.any { it.id in setOf("T1_DAYS", "T2_DAYS", "T3_DAYS") }
    }
    val hasSingleAttCol = remember(config.columns) {
        config.columns.any { it.id == "ATT" }
    }
    val sharedWorkingDaysTerms = remember(isThreeTermTraits, hasPerRowDaysCol, hasSingleAttCol, config.term) {
        when {
            isThreeTermTraits && !hasPerRowDaysCol -> listOf(1, 2, 3)
            !isThreeTermTraits && hasSingleAttCol && config.term in 1..3 -> listOf(config.term)
            else -> emptyList()
        }
    }
    val commonWorkingDays = remember(students, config.selectedClass) {
        BlankSheetCatalog.resolveCommonWorkingDaysByTerm(students, config.selectedClass)
    }

    val selectedSubjectLabel = remember(config.subjectKey, config.customSubjectTitle, isClassLevelTraitsOnly) {
        if (isClassLevelTraitsOnly) {
            "উপস্থিতি ও গুণাবলি"
        } else {
            BlankSheetCatalog.subjectTitleFor(config.subjectKey, config.customSubjectTitle)
                .ifBlank { "সাধারণ" }
        }
    }

    val selectedTermLabel = remember(config.term, config.customTermTitle, isThreeTermMarks, isThreeTermTraits) {
        if (isThreeTermMarks || isThreeTermTraits) {
            "৩ প্রান্তিক"
        } else {
            when (config.term) {
                1, 2, 3 -> termNameBn(config.term)
                0 -> "বার্ষিক"
                -1 -> config.customTermTitle.ifBlank { "শিরোনামহীন" }
                else -> "শিরোনামহীন"
            }
        }
    }

    val exportFileName = remember(config, selectedClassLabel, selectedSubjectLabel, isClassLevelTraitsOnly) {
        val clsPart = if (config.selectedClass == null) "সব-শ্রেণি" else "শ্রেণি-${config.selectedClass}"
        val subPart = when {
            isClassLevelTraitsOnly -> "৩-প্রান্তিক-উপস্থিতি-গুণাবলি"
            config.subjectKey == BlankSheetCatalog.ALL_SUBJECTS_KEY -> "সব-বিষয়"
            else -> selectedSubjectLabel.replace(" ", "-")
        }
        "ফাঁকা-শিট-$clsPart-$subPart.pdf"
    }

    val printJobName = remember(config, selectedClassLabel, selectedSubjectLabel) {
        "BlankSheet_${selectedClassLabel}_$selectedSubjectLabel"
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. COMPACT COLLAPSIBLE SETUP CARD
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(11.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Header / Collapse Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { setupExpanded = !setupExpanded }
                        .testTag("blank_setup_toggle_row"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (setupExpanded) {
                            "ফাঁকা ফর্ম / নম্বর শিট সেটাপ"
                        } else {
                            "$selectedClassLabel · $selectedSubjectLabel · ${bn(config.rowsPerPage.toString())} জন/পেজ · ${config.numeralStyle.shortLabelBn}"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            if (setupExpanded) "সংকুচিত" else "পরিবর্তন",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TealPrimary
                        )
                        Icon(
                            imageVector = if (setupExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (setupExpanded) "সেটাপ লুকান" else "সেটাপ দেখুন",
                            tint = TealPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = setupExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Row 1: Class + Subject + Term Dropdown Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            // Class Selector
                            Box(Modifier.weight(1f)) {
                                CompactDropdownPill(
                                    label = selectedClassLabel,
                                    highlighted = config.selectedClass == null,
                                    testTag = "blank_class_dropdown",
                                    onClick = { classMenuOpen = true }
                                )
                                DropdownMenu(
                                    expanded = classMenuOpen,
                                    onDismissRequest = { classMenuOpen = false }
                                ) {
                                    val allCount = students.count { it.cls in allowedClasses }
                                    DropdownMenuItem(
                                        modifier = Modifier.background(if (config.selectedClass == null) TealContainer else Color.Transparent),
                                        text = {
                                            Text(
                                                "সব শ্রেণি (${bn(allCount.toString())} জন)",
                                                fontWeight = if (config.selectedClass == null) FontWeight.Bold else FontWeight.Normal,
                                                color = if (config.selectedClass == null) TealDark else SlateText
                                            )
                                        },
                                        trailingIcon = if (config.selectedClass == null) {
                                            { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        onClick = {
                                            classMenuOpen = false
                                            config = config.copy(selectedClass = null)
                                            previewPageIdx = 0
                                        }
                                    )
                                    allowedClasses.forEach { c ->
                                        val isSel = config.selectedClass == c
                                        val cnt = students.count { it.cls == c }
                                        DropdownMenuItem(
                                            modifier = Modifier.background(if (isSel) TealContainer else Color.Transparent),
                                            text = {
                                                Text(
                                                    "${classNameBn(c, settings)} শ্রেণি (${bn(cnt.toString())} জন)",
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSel) TealDark else SlateText
                                                )
                                            },
                                            trailingIcon = if (isSel) {
                                                { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp)) }
                                            } else null,
                                            onClick = {
                                                classMenuOpen = false
                                                config = config.copy(selectedClass = c)
                                                previewPageIdx = 0
                                            }
                                        )
                                    }
                                }
                            }

                            // Subject Selector
                            Box(Modifier.weight(1.1f)) {
                                if (isClassLevelTraitsOnly) {
                                    CompactDropdownPill(
                                        label = "ধরন: উপস্থিতি ও গুণাবলি",
                                        highlighted = true,
                                        testTag = "blank_subject_dropdown",
                                        onClick = { /* Class-level form applies to all subjects */ }
                                    )
                                } else {
                                    CompactDropdownPill(
                                        label = "বিষয়: $selectedSubjectLabel",
                                        highlighted = config.subjectKey == BlankSheetCatalog.ALL_SUBJECTS_KEY,
                                        testTag = "blank_subject_dropdown",
                                        onClick = { subjectMenuOpen = true }
                                    )
                                    DropdownMenu(
                                        expanded = subjectMenuOpen,
                                        onDismissRequest = { subjectMenuOpen = false },
                                        modifier = Modifier.heightIn(max = 320.dp)
                                    ) {
                                        BlankSheetCatalog.SUBJECT_OPTIONS.forEach { opt ->
                                            val isSel = config.subjectKey == opt.key
                                            DropdownMenuItem(
                                                modifier = Modifier.background(if (isSel) TealContainer else Color.Transparent),
                                                text = {
                                                    Text(
                                                        opt.labelBn,
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSel) TealDark else SlateText
                                                    )
                                                },
                                                trailingIcon = if (isSel) {
                                                    { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp)) }
                                                } else null,
                                                onClick = {
                                                    subjectMenuOpen = false
                                                    config = config.copy(subjectKey = opt.key)
                                                    previewPageIdx = 0
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Term Selector
                            Box(Modifier.weight(0.95f)) {
                                CompactDropdownPill(
                                    label = selectedTermLabel,
                                    highlighted = isThreeTermMarks || isThreeTermTraits,
                                    testTag = "blank_term_dropdown",
                                    onClick = {
                                        if (!isThreeTermMarks && !isThreeTermTraits) {
                                            termMenuOpen = true
                                        }
                                    }
                                )
                                DropdownMenu(
                                    expanded = termMenuOpen,
                                    onDismissRequest = { termMenuOpen = false }
                                ) {
                                    listOf(
                                        1 to termNameBn(1),
                                        2 to termNameBn(2),
                                        3 to termNameBn(3),
                                        0 to "বার্ষিক মূল্যায়ন",
                                        -1 to "কাস্টম / শিরোনামহীন"
                                    ).forEach { (tVal, lbl) ->
                                        val isSel = config.term == tVal
                                        DropdownMenuItem(
                                            modifier = Modifier.background(if (isSel) TealContainer else Color.Transparent),
                                            text = {
                                                Text(
                                                    lbl,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSel) TealDark else SlateText
                                                )
                                            },
                                            trailingIcon = if (isSel) {
                                                { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp)) }
                                            } else null,
                                            onClick = {
                                                termMenuOpen = false
                                                config = config.copy(term = tVal)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Inline Custom Subject / Custom Term Input (only when selected)
                        if ((!isClassLevelTraitsOnly && config.subjectKey == null) || (!isThreeTermMarks && !isThreeTermTraits && config.term == -1)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (!isClassLevelTraitsOnly && config.subjectKey == null) {
                                    OutlinedTextField(
                                        value = config.customSubjectTitle,
                                        onValueChange = { config = config.copy(customSubjectTitle = it) },
                                        placeholder = { Text("কাস্টম বিষয়ের নাম লিখুন (ফাঁকা রাখলে শূন্যস্থান)", fontSize = 11.sp) },
                                        singleLine = true,
                                        textStyle = TextStyle(fontSize = 12.sp),
                                        modifier = Modifier.weight(1f).testTag("blank_custom_subject_input")
                                    )
                                }
                                if (!isThreeTermMarks && !isThreeTermTraits && config.term == -1) {
                                    OutlinedTextField(
                                        value = config.customTermTitle,
                                        onValueChange = { config = config.copy(customTermTitle = it) },
                                        placeholder = { Text("পরীক্ষার শিরোনাম (যেমন: মাসিক)", fontSize = 11.sp) },
                                        singleLine = true,
                                        textStyle = TextStyle(fontSize = 12.sp),
                                        modifier = Modifier.weight(1f).testTag("blank_custom_term_input")
                                    )
                                }
                            }
                        }

                        // Row 2: Rows Per Page + Orientation + Numeral Style (বাংলা/English) + More Options
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            // Rows per page stepper + preset dropdown
                            Surface(
                                color = SlateBg,
                                border = BorderStroke(1.dp, SlateOutline),
                                shape = RoundedCornerShape(7.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        "প্রতি পেজ:",
                                        fontSize = 10.sp,
                                        color = SlateMuted,
                                        modifier = Modifier.padding(start = 2.dp, end = 3.dp)
                                    )
                                    Surface(
                                        onClick = {
                                            config = config.copy(rowsPerPage = (config.rowsPerPage - 1).coerceAtLeast(5))
                                            previewPageIdx = 0
                                        },
                                        shape = RoundedCornerShape(5.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.size(24.dp).testTag("blank_rows_dec_btn")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.Remove, contentDescription = "কমান", modifier = Modifier.size(14.dp), tint = SlateText)
                                        }
                                    }
                                    Box {
                                        Surface(
                                            onClick = { rowsMenuOpen = true },
                                            color = TealSoft,
                                            shape = RoundedCornerShape(5.dp),
                                            modifier = Modifier
                                                .padding(horizontal = 3.dp)
                                                .height(24.dp)
                                                .testTag("blank_rows_value_pill")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 5.dp)
                                            ) {
                                                Text(
                                                    bn(config.rowsPerPage.toString()),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TealDark
                                                )
                                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                        DropdownMenu(
                                            expanded = rowsMenuOpen,
                                            onDismissRequest = { rowsMenuOpen = false }
                                        ) {
                                            listOf(10, 15, 20, 25, 30, 35, 40, 45, 50).forEach { count ->
                                                val isSel = config.rowsPerPage == count
                                                DropdownMenuItem(
                                                    modifier = Modifier.background(if (isSel) TealContainer else Color.Transparent),
                                                    text = {
                                                        Text(
                                                            "প্রতি পেজে ${bn(count.toString())} জন",
                                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSel) TealDark else SlateText
                                                        )
                                                    },
                                                    onClick = {
                                                        rowsMenuOpen = false
                                                        config = config.copy(rowsPerPage = count)
                                                        previewPageIdx = 0
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    Surface(
                                        onClick = {
                                            config = config.copy(rowsPerPage = (config.rowsPerPage + 1).coerceAtMost(50))
                                            previewPageIdx = 0
                                        },
                                        shape = RoundedCornerShape(5.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.size(24.dp).testTag("blank_rows_inc_btn")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.Add, contentDescription = "বাড়ান", modifier = Modifier.size(14.dp), tint = SlateText)
                                        }
                                    }
                                }
                            }

                            // Orientation Switch (Portrait / Landscape)
                            Surface(
                                color = SlateSurfaceVariant,
                                border = BorderStroke(1.dp, SlateOutline),
                                shape = RoundedCornerShape(7.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxHeight().padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    listOf(
                                        BlankSheetOrientation.PORTRAIT to "খাড়া",
                                        BlankSheetOrientation.LANDSCAPE to "আড়াআড়ি"
                                    ).forEach { (ori, lbl) ->
                                        val isSel = config.orientation == ori
                                        Surface(
                                            onClick = { config = config.copy(orientation = ori) },
                                            color = if (isSel) TealPrimary else Color.Transparent,
                                            shape = RoundedCornerShape(5.dp),
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .testTag("blank_ori_${ori.name.lowercase()}")
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(horizontal = 7.dp)
                                            ) {
                                                Text(
                                                    lbl,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSel) Color.White else SlateText,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Numeral Style Switch (বাংলা ১২৩ / English 123)
                            Surface(
                                color = SlateSurfaceVariant,
                                border = BorderStroke(1.dp, SlateOutline),
                                shape = RoundedCornerShape(7.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxHeight().padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    listOf(
                                        BlankNumeralStyle.BENGALI to "১২৩",
                                        BlankNumeralStyle.ENGLISH to "123"
                                    ).forEach { (style, lbl) ->
                                        val isSel = config.numeralStyle == style
                                        Surface(
                                            onClick = { config = config.copy(numeralStyle = style) },
                                            color = if (isSel) TealPrimary else Color.Transparent,
                                            shape = RoundedCornerShape(5.dp),
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .testTag("blank_num_${style.name.lowercase()}")
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(horizontal = 7.dp)
                                            ) {
                                                Text(
                                                    lbl,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSel) Color.White else SlateText,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Extra Options Drawer Toggle
                            Surface(
                                onClick = { showMoreOptions = !showMoreOptions },
                                color = if (showMoreOptions) TealSoft else SlateBg,
                                border = BorderStroke(1.dp, if (showMoreOptions) TealPrimary else SlateOutline),
                                shape = RoundedCornerShape(7.dp),
                                modifier = Modifier.height(32.dp).testTag("blank_more_options_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(horizontal = 7.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Tune,
                                        contentDescription = "আরও অপশন",
                                        tint = if (showMoreOptions) TealPrimary else SlateMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        "আরও",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (showMoreOptions) TealDark else SlateText
                                    )
                                }
                            }
                        }

                        // Collapsible More Options Micro-Drawer
                        AnimatedVisibility(visible = showMoreOptions) {
                            Surface(
                                color = SlateBg,
                                border = BorderStroke(1.dp, SlateOutline),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    config = config.copy(autoFillRollAndName = !config.autoFillRollAndName)
                                                }
                                        ) {
                                            Checkbox(
                                                checked = config.autoFillRollAndName,
                                                onCheckedChange = { config = config.copy(autoFillRollAndName = it) },
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Text("রোল ও নাম অটো-ফিল", fontSize = 11.sp, color = SlateText)
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    config = config.copy(showSubjectMaxInHeader = !config.showSubjectMaxInHeader)
                                                }
                                        ) {
                                            Checkbox(
                                                checked = config.showSubjectMaxInHeader,
                                                onCheckedChange = { config = config.copy(showSubjectMaxInHeader = it) },
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Text("হেডারে সর্বোচ্চ নম্বর", fontSize = 11.sp, color = SlateText)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text("অতিরিক্ত ফাঁকা সারি:", fontSize = 10.5.sp, color = SlateMuted)
                                        listOf(0, 2, 5, 10).forEach { extra ->
                                            val sel = config.extraBlankRowsPerClass == extra
                                            FilterChip(
                                                selected = sel,
                                                onClick = { config = config.copy(extraBlankRowsPerClass = extra) },
                                                colors = appFilterChipColors(),
                                                border = appFilterChipBorder(sel),
                                                label = {
                                                    Text(
                                                        if (extra == 0) "নেই" else "+${bn(extra.toString())}",
                                                        fontSize = 10.5.sp
                                                    )
                                                }
                                            )
                                        }
                                        FilterChip(
                                            selected = config.includeQrCode,
                                            onClick = { config = config.copy(includeQrCode = !config.includeQrCode) },
                                            colors = appFilterChipColors(),
                                            border = appFilterChipBorder(config.includeQrCode),
                                            label = { Text("স্মার্ট QR কোড", fontSize = 10.5.sp) },
                                            modifier = Modifier.testTag("blank_sheet_qr_toggle")
                                        )
                                        FilterChip(
                                            selected = config.includeBarcode,
                                            onClick = { config = config.copy(includeBarcode = !config.includeBarcode) },
                                            colors = appFilterChipColors(),
                                            border = appFilterChipBorder(config.includeBarcode),
                                            label = { Text("নিচে বারকোড", fontSize = 10.5.sp) },
                                            modifier = Modifier.testTag("blank_sheet_barcode_toggle")
                                        )
                                        FilterChip(
                                            selected = config.fillRemainingPageRows,
                                            onClick = { config = config.copy(fillRemainingPageRows = !config.fillRemainingPageRows) },
                                            colors = appFilterChipColors(),
                                            border = appFilterChipBorder(config.fillRemainingPageRows),
                                            label = { Text("পেজ পূর্ণ সারি", fontSize = 10.5.sp) }
                                        )
                                        if (sharedWorkingDaysTerms.isNotEmpty()) {
                                            FilterChip(
                                                selected = config.showSharedWorkingDaysHeader,
                                                onClick = {
                                                    config = config.copy(showSharedWorkingDaysHeader = !config.showSharedWorkingDaysHeader)
                                                },
                                                colors = appFilterChipColors(),
                                                border = appFilterChipBorder(config.showSharedWorkingDaysHeader),
                                                label = { Text("উপরে মোট কার্যদিবস", fontSize = 10.5.sp) },
                                                modifier = Modifier.testTag("blank_shared_wd_toggle_chip")
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = SlateOutline.copy(alpha = 0.7f))

                        // Row 3: Quick Presets + Add Custom Column Trigger
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text("টেমপ্লেট:", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = SlateMuted)
                            BlankSheetPreset.entries.forEach { preset ->
                                val isExact = when (preset) {
                                    BlankSheetPreset.THREE_TERM_MARKS -> isThreeTermMarks
                                    BlankSheetPreset.THREE_TERM_ATTENDANCE_TRAITS -> isThreeTermTraits && !isThreeTermMarks
                                    else -> config.columns.map { it.id } == preset.columnIds
                                }
                                Surface(
                                    onClick = {
                                        val cols = BlankSheetCatalog.presetColumns(preset)
                                        val suggestedOri = if (cols.size >= 8) BlankSheetOrientation.LANDSCAPE else BlankSheetOrientation.PORTRAIT
                                        config = config.copy(columns = cols, orientation = suggestedOri)
                                        focusedColId = null
                                        previewPageIdx = 0
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isExact) TealSoft else SlateBg,
                                    border = BorderStroke(1.dp, if (isExact) TealPrimary else SlateOutline),
                                    modifier = Modifier.testTag("blank_preset_${preset.name.lowercase()}")
                                ) {
                                    Text(
                                        preset.labelBn,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isExact) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isExact) TealDark else SlateText,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Surface(
                                onClick = { showCustomColumnInput = !showCustomColumnInput },
                                shape = RoundedCornerShape(6.dp),
                                color = if (showCustomColumnInput) TealContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, TealPrimary),
                                modifier = Modifier.testTag("blank_toggle_custom_col_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(13.dp))
                                    Text("কাস্টম কলাম", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                                }
                            }
                        }

                        // Optional Total Column Toggle for 3-Term Marks Sheet
                        if (isThreeTermMarks) {
                            Surface(
                                onClick = {
                                    val nextCols = BlankSheetCatalog.toggleThreeTermTotals(
                                        config.columns,
                                        !hasThreeTermTotal
                                    )
                                    config = config.copy(columns = nextCols)
                                },
                                shape = RoundedCornerShape(7.dp),
                                color = if (hasThreeTermTotal) TealSoft else SlateBg,
                                border = BorderStroke(1.dp, if (hasThreeTermTotal) TealPrimary else SlateOutline),
                                modifier = Modifier.fillMaxWidth().testTag("blank_3term_total_toggle")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Checkbox(
                                        checked = hasThreeTermTotal,
                                        onCheckedChange = { checked ->
                                            val nextCols = BlankSheetCatalog.toggleThreeTermTotals(
                                                config.columns,
                                                checked
                                            )
                                            config = config.copy(columns = nextCols)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        "প্রতি প্রান্তিকে ঐচ্ছিক 'মোট' কলাম যোগ করুন (ধারাবাহিক + সামষ্টিক + মোট)",
                                        fontSize = 11.sp,
                                        fontWeight = if (hasThreeTermTotal) FontWeight.Bold else FontWeight.Medium,
                                        color = if (hasThreeTermTotal) TealDark else SlateText
                                    )
                                }
                            }
                        }

                        // Shared Working Days strip setup (when 3-term traits or single-term ATT without per-row DAYS column)
                        if (sharedWorkingDaysTerms.isNotEmpty() && config.showSharedWorkingDaysHeader) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SlateBg,
                                border = BorderStroke(1.dp, SlateOutline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            "প্রান্তিকের মোট কার্যদিবস (উপরে এক জায়গায় ছাপাবে):",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TealDark,
                                            modifier = Modifier.weight(1f)
                                        )
                                        val hasAnyFromData = sharedWorkingDaysTerms.any { !commonWorkingDays[it].isNullOrBlank() }
                                        if (hasAnyFromData) {
                                            Surface(
                                                onClick = {
                                                    val next = config.workingDaysByTerm.toMutableMap()
                                                    sharedWorkingDaysTerms.forEach { t ->
                                                        commonWorkingDays[t]?.let { next[t] = it }
                                                    }
                                                    config = config.copy(workingDaysByTerm = next)
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                color = TealSoft,
                                                border = BorderStroke(1.dp, TealContainer),
                                                modifier = Modifier.testTag("blank_wd_autofill_btn")
                                            ) {
                                                Text(
                                                    "ডাটা থেকে নিন",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TealPrimary,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        if (sharedWorkingDaysTerms.any { !config.workingDaysByTerm[it].isNullOrBlank() }) {
                                            Surface(
                                                onClick = {
                                                    val next = config.workingDaysByTerm.toMutableMap()
                                                    sharedWorkingDaysTerms.forEach { t -> next.remove(t) }
                                                    config = config.copy(workingDaysByTerm = next)
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, SlateOutline),
                                                modifier = Modifier.testTag("blank_wd_clear_btn")
                                            ) {
                                                Text(
                                                    "লাইন রাখুন",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = SlateMuted,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        sharedWorkingDaysTerms.forEach { t ->
                                            val rawVal = config.workingDaysByTerm[t].orEmpty()
                                            val displayVal = if (rawVal.isEmpty()) "" else BlankSheetCatalog.formatDigits(rawVal, config.numeralStyle)
                                            OutlinedTextField(
                                                value = displayVal,
                                                onValueChange = { txt ->
                                                    val clean = fromBn(txt).filter { it.isDigit() }.take(3)
                                                    val next = config.workingDaysByTerm.toMutableMap()
                                                    if (clean.isEmpty()) next.remove(t) else next[t] = clean
                                                    config = config.copy(workingDaysByTerm = next)
                                                },
                                                label = { Text(termNameBn(t), fontSize = 10.sp) },
                                                placeholder = { Text("ফাঁকা=লাইন", fontSize = 10.sp) },
                                                singleLine = true,
                                                textStyle = TextStyle(fontSize = 11.5.sp),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("blank_wd_input_$t")
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Inline Custom Column Adder
                        AnimatedVisibility(visible = showCustomColumnInput) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = customColText,
                                    onValueChange = { customColText = it },
                                    placeholder = { Text("নতুন কলামের নাম (যেমন: মৌখিক / স্বাক্ষর)", fontSize = 11.5.sp) },
                                    singleLine = true,
                                    textStyle = TextStyle(fontSize = 12.sp),
                                    modifier = Modifier.weight(1f).testTag("blank_custom_col_input")
                                )
                                Button(
                                    onClick = {
                                        val clean = customColText.trim()
                                        if (clean.isNotEmpty()) {
                                            val newCol = BlankSheetCatalog.createCustomColumn(clean)
                                            config = config.copy(columns = config.columns + newCol)
                                            customColText = ""
                                            showCustomColumnInput = false
                                        }
                                    },
                                    enabled = customColText.isNotBlank(),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("blank_add_custom_col_btn")
                                ) {
                                    Text("যোগ", fontSize = 12.sp)
                                }
                            }
                        }

                        // Column Picker Strip (context-aware: standard, 3-term marks, or 3-term traits)
                        val pickerColumns = remember(config.columns) {
                            BlankSheetCatalog.pickerColumnsFor(config.columns)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            pickerColumns.forEach { catalogCol ->
                                val isAdded = config.columns.any { it.id == catalogCol.id }
                                Surface(
                                    onClick = {
                                        val updated = if (isAdded) {
                                            if (config.columns.size > 1) config.columns.filterNot { it.id == catalogCol.id }
                                            else config.columns
                                        } else {
                                            insertCatalogColumnInOrder(config.columns, catalogCol)
                                        }
                                        config = config.copy(columns = updated)
                                        if (!isAdded) focusedColId = null
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isAdded) TealPrimary else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, if (isAdded) TealPrimary else SlateOutline),
                                    modifier = Modifier.testTag("blank_col_toggle_${catalogCol.id}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                    ) {
                                        if (isAdded) {
                                            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        }
                                        Text(
                                            catalogCol.pickerLabelBn,
                                            fontSize = 11.sp,
                                            fontWeight = if (isAdded) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isAdded) Color.White else SlateText,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // Compact Active Column Order & Quick Reorder Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "ক্রম (${bn(config.columns.size.toString())}):",
                                fontSize = 10.sp,
                                color = SlateMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                            config.columns.forEachIndexed { idx, col ->
                                val isFocused = focusedColId == col.id
                                Surface(
                                    onClick = { focusedColId = if (isFocused) null else col.id },
                                    shape = RoundedCornerShape(5.dp),
                                    color = if (isFocused) TealContainer else SlateBg,
                                    border = BorderStroke(1.dp, if (isFocused) TealPrimary else SlateOutline)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "${bn((idx + 1).toString())}.${col.pickerLabelBn}",
                                            fontSize = 10.sp,
                                            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isFocused) TealDark else SlateText
                                        )
                                        if (isFocused) {
                                            if (idx > 0) {
                                                Icon(
                                                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                                    contentDescription = "বামে সরান",
                                                    tint = TealPrimary,
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clickable {
                                                            val list = config.columns.toMutableList()
                                                            val item = list.removeAt(idx)
                                                            list.add(idx - 1, item)
                                                            config = config.copy(columns = list)
                                                        }
                                                )
                                            }
                                            if (idx < config.columns.lastIndex) {
                                                Icon(
                                                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                                    contentDescription = "ডানে সরান",
                                                    tint = TealPrimary,
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clickable {
                                                            val list = config.columns.toMutableList()
                                                            val item = list.removeAt(idx)
                                                            list.add(idx + 1, item)
                                                            config = config.copy(columns = list)
                                                        }
                                                )
                                            }
                                            if (config.columns.size > 1) {
                                                Icon(
                                                    Icons.Rounded.Close,
                                                    contentDescription = "বাদ দিন",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable {
                                                            config = config.copy(columns = config.columns.filterNot { it.id == col.id })
                                                            focusedColId = null
                                                        }
                                                )
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

        // 2. LIVE PDF PAGE PREVIEW AREA
        Card(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateBg),
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(11.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                // Slim Page Navigation & Status Header
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (activePage != null) {
                                val subSuffix = if (activePage.subjectTitleBn.isNotBlank()) " · ${activePage.subjectTitleBn}" else ""
                                "পৃষ্ঠা ${bn(activePage.globalPageNumber.toString())}/${bn(activePage.totalPages.toString())} · শ্রেণি: ${activePage.classTitleBn}$subSuffix"
                            } else {
                                "কোনো পৃষ্ঠা নেই"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SlateText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            onClick = { if (previewPageIdx > 0) previewPageIdx-- },
                            enabled = previewPageIdx > 0,
                            shape = RoundedCornerShape(6.dp),
                            color = SlateBg,
                            border = BorderStroke(1.dp, SlateOutline),
                            modifier = Modifier.size(28.dp).testTag("blank_prev_page_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                    contentDescription = "আগের পৃষ্ঠা",
                                    tint = if (previewPageIdx > 0) SlateText else SlateOutline,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Surface(
                            onClick = { if (previewPageIdx < pages.lastIndex) previewPageIdx++ },
                            enabled = previewPageIdx < pages.lastIndex,
                            shape = RoundedCornerShape(6.dp),
                            color = SlateBg,
                            border = BorderStroke(1.dp, SlateOutline),
                            modifier = Modifier.size(28.dp).testTag("blank_next_page_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = "পরের পৃষ্ঠা",
                                    tint = if (previewPageIdx < pages.lastIndex) SlateText else SlateOutline,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Surface(
                            onClick = {
                                val next = (zoomScale - 0.5f).coerceAtLeast(1f)
                                zoomScale = next
                                panOffset = clampBlankPanOffset(panOffset, next, previewSize)
                            },
                            enabled = zoomScale > 1.02f,
                            shape = RoundedCornerShape(6.dp),
                            color = SlateBg,
                            border = BorderStroke(1.dp, SlateOutline),
                            modifier = Modifier.size(28.dp).testTag("blank_zoom_out_btn")
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
                            modifier = Modifier.height(28.dp).testTag("blank_reset_zoom_btn")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${bn((zoomScale * 100).roundToInt().toString())}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (zoomScale > 1.02f) TealPrimary else SlateText
                                )
                            }
                        }
                        Surface(
                            onClick = {
                                val next = (zoomScale + 0.5f).coerceAtMost(4f)
                                zoomScale = next
                                panOffset = clampBlankPanOffset(panOffset, next, previewSize)
                            },
                            enabled = zoomScale < 3.98f,
                            shape = RoundedCornerShape(6.dp),
                            color = SlateBg,
                            border = BorderStroke(1.dp, SlateOutline),
                            modifier = Modifier.size(28.dp).testTag("blank_zoom_in_btn")
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
                            onClick = { if (previewBitmap != null) showFullscreen = true },
                            enabled = previewBitmap != null,
                            shape = RoundedCornerShape(6.dp),
                            color = TealSoft,
                            border = BorderStroke(1.dp, TealContainer),
                            modifier = Modifier.size(28.dp).testTag("blank_fullscreen_preview_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Fullscreen,
                                    contentDescription = "বড় করে দেখুন",
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
                        .pointerInput(previewPageIdx, pages.size, previewSize) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                var swipeDx = 0f
                                var swipeDy = 0f
                                var swipeTriggered = false
                                do {
                                    val event = awaitPointerEvent()
                                    val pressedCount = event.changes.count { it.pressed }
                                    if (pressedCount >= 2) {
                                        swipeTriggered = true
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        if (zoomChange != 1f || panChange != Offset.Zero) {
                                            val newScale = (zoomScale * zoomChange).coerceIn(1f, 4f)
                                            zoomScale = newScale
                                            panOffset = clampBlankPanOffset(panOffset + panChange, newScale, previewSize)
                                            event.changes.forEach { it.consume() }
                                        }
                                    } else if (zoomScale > 1.02f && pressedCount == 1) {
                                        val panChange = event.calculatePan()
                                        if (panChange != Offset.Zero) {
                                            panOffset = clampBlankPanOffset(panOffset + panChange, zoomScale, previewSize)
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
                                                if (previewPageIdx < pages.lastIndex) previewPageIdx++
                                            } else {
                                                if (previewPageIdx > 0) previewPageIdx--
                                            }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                        .pointerInput(previewPageIdx, previewBitmap != null) {
                            detectTapGestures(
                                onTap = {
                                    if (previewBitmap != null) showFullscreen = true
                                },
                                onDoubleTap = {
                                    if (zoomScale > 1.05f) {
                                        zoomScale = 1f
                                        panOffset = Offset.Zero
                                    } else {
                                        zoomScale = 2.2f
                                        panOffset = Offset.Zero
                                    }
                                }
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState(), enabled = zoomScale <= 1.02f)
                            .padding(8.dp)
                            .graphicsLayer {
                                scaleX = zoomScale
                                scaleY = zoomScale
                                translationX = panOffset.x
                                translationY = panOffset.y
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val bmp = previewBitmap
                        if (bmp != null) {
                            val ratio = if (config.orientation == BlankSheetOrientation.PORTRAIT) {
                                595f / 842f
                            } else {
                                842f / 595f
                            }
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(6.dp),
                                shadowElevation = 3.dp,
                                border = BorderStroke(1.dp, SlateOutline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 560.dp)
                                    .aspectRatio(ratio)
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "ফাঁকা শিট PDF প্রিভিউ",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = TealPrimary, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }
        }

        // 3. BOTTOM EXPORT / PRINT BAR
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val totalStu = remember(pages, config.selectedClass, students, allowedClasses) {
                    if (config.selectedClass != null) {
                        students.count { it.cls == config.selectedClass }
                    } else {
                        students.count { it.cls in allowedClasses }
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "${bn(totalStu.toString())} জন · ${bn(pages.size.toString())} পৃষ্ঠা",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateText,
                        maxLines = 1
                    )
                    Text(
                        config.orientation.labelBn,
                        fontSize = 9.5.sp,
                        color = SlateMuted,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = {
                        val promptText = BlankSheetCatalog.buildExternalAiCsvPrompt(config, settings, allowedClasses)
                        clipboard.setText(AnnotatedString(promptText))
                        Toast.makeText(
                            ctx,
                            "এই ফর্মের AI প্রম্পট কপি হয়েছে — বাইরের AI-তে ছবি/PDF-সহ দিন",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    enabled = pages.isNotEmpty(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, TealContainer),
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 10.dp),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("blank_sheet_copy_ai_prompt_btn")
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = "AI প্রম্পট কপি", tint = TealPrimary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("প্রম্পট", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TealDark)
                }

                OutlinedButton(
                    onClick = { onPrint(pages, config.orientation, printJobName) },
                    enabled = pages.isNotEmpty(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, TealPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("blank_sheet_print_btn")
                ) {
                    Icon(Icons.Rounded.Print, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("প্রিন্ট", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = TealPrimary)
                }

                Button(
                    onClick = { onExportPdf(pages, config.orientation, exportFileName) },
                    enabled = pages.isNotEmpty(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("blank_sheet_export_pdf_btn")
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PDF", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showFullscreen) {
        var fsScale by remember(previewPageIdx) { mutableFloatStateOf(1f) }
        var fsPan by remember(previewPageIdx) { mutableStateOf(Offset.Zero) }
        var fsBoxSize by remember { mutableStateOf(IntSize.Zero) }

        Dialog(
            onDismissRequest = { showFullscreen = false },
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
                                activePage?.let {
                                    val subSuffix = if (it.subjectTitleBn.isNotBlank()) " · ${it.subjectTitleBn}" else ""
                                    "${it.classTitleBn}$subSuffix · পৃষ্ঠা ${bn(it.globalPageNumber.toString())}/${bn(it.totalPages.toString())}"
                                }.orEmpty(),
                                color = SlateText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val next = (fsScale - 0.5f).coerceAtLeast(1f)
                                    fsScale = next
                                    fsPan = clampBlankPanOffset(fsPan, next, fsBoxSize)
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
                                    fsPan = clampBlankPanOffset(fsPan, next, fsBoxSize)
                                },
                                enabled = fsScale < 4.45f,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "জুম ইন")
                            }
                            IconButton(
                                onClick = { showFullscreen = false },
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
                            .pointerInput(previewPageIdx, pages.size, fsBoxSize) {
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
                                                fsPan = clampBlankPanOffset(fsPan + p, ns, fsBoxSize)
                                                event.changes.forEach { it.consume() }
                                            }
                                        } else if (fsScale > 1.02f && pressed == 1) {
                                            val p = event.calculatePan()
                                            if (p != Offset.Zero) {
                                                fsPan = clampBlankPanOffset(fsPan + p, fsScale, fsBoxSize)
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
                                                    if (previewPageIdx < pages.lastIndex) previewPageIdx++
                                                } else {
                                                    if (previewPageIdx > 0) previewPageIdx--
                                                }
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            }
                            .pointerInput(previewPageIdx) {
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
                        val bmp = previewBitmap
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "পূর্ণ পৃষ্ঠা প্রিভিউ",
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
                        } else {
                            CircularProgressIndicator(color = TealPrimary, modifier = Modifier.size(32.dp))
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
                                onClick = { if (previewPageIdx > 0) previewPageIdx-- },
                                enabled = previewPageIdx > 0
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = null)
                                Text("আগের পাতা")
                            }
                            Text(
                                if (pages.isEmpty()) "০ / ০"
                                else "পৃষ্ঠা ${bn((previewPageIdx + 1).toString())} / ${bn(pages.size.toString())}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = TealPrimary
                            )
                            OutlinedButton(
                                onClick = { if (previewPageIdx < pages.lastIndex) previewPageIdx++ },
                                enabled = previewPageIdx < pages.lastIndex
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
}

@Composable
private fun CompactDropdownPill(
    label: String,
    highlighted: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(7.dp),
        color = if (highlighted) TealSoft else SlateBg,
        border = BorderStroke(1.dp, if (highlighted) TealPrimary else SlateOutline),
        modifier = Modifier.fillMaxWidth().height(34.dp).testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 7.dp, end = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (highlighted) TealDark else SlateText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Rounded.ArrowDropDown,
                contentDescription = null,
                tint = if (highlighted) TealPrimary else SlateMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun insertCatalogColumnInOrder(
    current: List<BlankSheetColumn>,
    toAdd: BlankSheetColumn
): List<BlankSheetColumn> {
    val catalogOrder = BlankSheetCatalog.ALL_COLUMNS.mapIndexed { idx, c -> c.id to idx }.toMap()
    val targetIdx = catalogOrder[toAdd.id] ?: return current + toAdd
    val insertPos = current.indexOfFirst { (catalogOrder[it.id] ?: Int.MAX_VALUE) > targetIdx }
    return if (insertPos < 0) current + toAdd
    else current.toMutableList().apply { add(insertPos, toAdd) }
}
