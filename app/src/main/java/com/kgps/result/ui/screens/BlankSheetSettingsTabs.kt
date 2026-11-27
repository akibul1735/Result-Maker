@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.kgps.result.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.BlankBottomCodeStyle
import com.kgps.result.BlankNumeralStyle
import com.kgps.result.BlankSheetCatalog
import com.kgps.result.BlankSheetColumn
import com.kgps.result.BlankSheetConfig
import com.kgps.result.BlankSheetOrientation
import com.kgps.result.BlankSheetPlanner
import com.kgps.result.BlankSheetPreset
import com.kgps.result.BlankSheetRow
import com.kgps.result.BlankSheetRowFitMode
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.bn
import com.kgps.result.classShortBn
import com.kgps.result.fromBn
import com.kgps.result.termNameBn
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealContainer
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft

@Composable
internal fun BlankSheetClassSubjectTab(
    config: BlankSheetConfig,
    settings: Settings,
    allowedClasses: List<Int>,
    countByClass: Map<Int, Int>,
    isThreeTermMarks: Boolean,
    isThreeTermTraits: Boolean,
    isClassLevelTraitsOnly: Boolean,
    selectedSubjectLabel: String,
    selectedTermLabel: String,
    onUpdateConfig: (BlankSheetConfig) -> Unit,
    onResetPageIndex: () -> Unit
) {
    val allCount = countByClass.values.sum()
    val classSummary = config.selectedClass?.let { "${classShortBn(it, settings)} (${bn((countByClass[it] ?: 0).toString())})" }
        ?: "সব শ্রেণি (${bn(allCount.toString())})"

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AdmitCollapsibleGroupCard(
            title = "১. শ্রেণি নির্বাচন",
            summary = classSummary,
            icon = Icons.Rounded.MenuBook,
            initiallyExpanded = true,
            testTag = "blank_grp_class"
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                val allSel = config.selectedClass == null
                Surface(
                    onClick = {
                        onUpdateConfig(config.copy(selectedClass = null))
                        onResetPageIndex()
                    },
                    color = if (allSel) TealPrimary else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (allSel) TealPrimary else SlateOutline),
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier.heightIn(min = 32.dp).testTag("blank_class_all_chip")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "সব শ্রেণি (${bn(allCount.toString())})",
                            fontSize = 11.sp,
                            fontWeight = if (allSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (allSel) Color.White else SlateText
                        )
                    }
                }
                allowedClasses.forEach { c ->
                    val isSel = config.selectedClass == c
                    val cnt = countByClass[c] ?: 0
                    Surface(
                        onClick = {
                            onUpdateConfig(config.copy(selectedClass = c))
                            onResetPageIndex()
                        },
                        color = if (isSel) TealPrimary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (isSel) TealPrimary else SlateOutline),
                        shape = RoundedCornerShape(7.dp),
                        modifier = Modifier.heightIn(min = 32.dp).testTag("blank_class_chip_$c")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text(
                                "${classShortBn(c, settings)} (${bn(cnt.toString())})",
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else SlateText,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        AdmitCollapsibleGroupCard(
            title = "২. বিষয়, মূল্যায়ন ও সংখ্যা",
            summary = "$selectedSubjectLabel · $selectedTermLabel · ${config.numeralStyle.shortLabelBn}",
            icon = Icons.Rounded.Tune,
            initiallyExpanded = true,
            testTag = "blank_grp_subject_term"
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isClassLevelTraitsOnly) {
                    Surface(
                        color = TealSoft,
                        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.1f).heightIn(min = 42.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("ধরন", fontSize = 8.5.sp, color = SlateMuted)
                            Text(
                                "উপস্থিতি ও গুণাবলি",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TealDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    AdmitCompactPickerBox(
                        label = "বিষয়",
                        valueText = selectedSubjectLabel,
                        options = BlankSheetCatalog.SUBJECT_OPTIONS,
                        selectedValue = BlankSheetCatalog.SUBJECT_OPTIONS.find { it.key == config.subjectKey }
                            ?: BlankSheetCatalog.SUBJECT_OPTIONS.last(),
                        optionLabel = { it.labelBn },
                        onSelect = { opt ->
                            onUpdateConfig(config.copy(subjectKey = opt.key))
                            onResetPageIndex()
                        },
                        modifier = Modifier.weight(1.1f),
                        testTag = "blank_subject_dropdown"
                    )
                }

                val termOptions = listOf(
                    1 to termNameBn(1),
                    2 to termNameBn(2),
                    3 to termNameBn(3),
                    0 to "বার্ষিক মূল্যায়ন",
                    -1 to "কাস্টম / শিরোনামহীন"
                )
                AdmitCompactPickerBox(
                    label = "প্রান্তিক / পরীক্ষা",
                    valueText = selectedTermLabel,
                    options = termOptions.map { it.first },
                    selectedValue = config.term,
                    optionLabel = { tVal -> termOptions.find { it.first == tVal }?.second ?: "" },
                    onSelect = { tVal ->
                        onUpdateConfig(config.copy(term = tVal))
                    },
                    enabled = !isThreeTermMarks && !isThreeTermTraits,
                    modifier = Modifier.weight(0.95f),
                    testTag = "blank_term_dropdown"
                )
            }

            if ((!isClassLevelTraitsOnly && config.subjectKey == null) ||
                (!isThreeTermMarks && !isThreeTermTraits && config.term == -1)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (!isClassLevelTraitsOnly && config.subjectKey == null) {
                        AdmitCompactTextInput(
                            label = "কাস্টম বিষয়",
                            value = config.customSubjectTitle,
                            onValueChange = { onUpdateConfig(config.copy(customSubjectTitle = it)) },
                            placeholder = "ফাঁকা = শূন্যস্থান",
                            modifier = Modifier.weight(1f).testTag("blank_custom_subject_input")
                        )
                    }
                    if (!isThreeTermMarks && !isThreeTermTraits && config.term == -1) {
                        AdmitCompactTextInput(
                            label = "পরীক্ষার শিরোনাম",
                            value = config.customTermTitle,
                            onValueChange = { onUpdateConfig(config.copy(customTermTitle = it)) },
                            placeholder = "যেমন: মাসিক",
                            modifier = Modifier.weight(1f).testTag("blank_custom_term_input")
                        )
                    }
                }
            }

            Text(
                "নম্বর ও রোল অঙ্কের ধরন",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                color = SlateMuted
            )
            AdmitSegmentedOptionBar(
                items = listOf(
                    BlankNumeralStyle.BENGALI to "বাংলা (১২৩)",
                    BlankNumeralStyle.ENGLISH to "English (123)"
                ),
                selected = config.numeralStyle,
                onSelect = { onUpdateConfig(config.copy(numeralStyle = it)) },
                tagPrefix = "blank_num"
            )
        }
    }
}

@Composable
internal fun BlankSheetColumnsTab(
    config: BlankSheetConfig,
    isThreeTermMarks: Boolean,
    hasThreeTermTotal: Boolean,
    isThreeTermTraits: Boolean,
    onUpdateConfig: (BlankSheetConfig) -> Unit,
    onResetPageIndex: () -> Unit
) {
    var showCustomColumnInput by remember { mutableStateOf(false) }
    var customColText by remember { mutableStateOf("") }
    var focusedColId by remember { mutableStateOf<String?>(null) }

    val activePresetLabel = remember(config.columns, isThreeTermMarks, isThreeTermTraits) {
        BlankSheetPreset.entries.find { preset ->
            when (preset) {
                BlankSheetPreset.THREE_TERM_MARKS -> isThreeTermMarks
                BlankSheetPreset.THREE_TERM_ATTENDANCE_TRAITS -> isThreeTermTraits && !isThreeTermMarks
                else -> config.columns.map { it.id } == preset.columnIds
            }
        }?.labelBn ?: "কাস্টম (${bn(config.columns.size.toString())} কলাম)"
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AdmitCollapsibleGroupCard(
            title = "১. ফর্ম টেমপ্লেট (প্রিসেট)",
            summary = activePresetLabel,
            icon = Icons.Rounded.TableChart,
            initiallyExpanded = true,
            testTag = "blank_grp_presets"
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                BlankSheetPreset.entries.forEach { preset ->
                    val isExact = when (preset) {
                        BlankSheetPreset.THREE_TERM_MARKS -> isThreeTermMarks
                        BlankSheetPreset.THREE_TERM_ATTENDANCE_TRAITS -> isThreeTermTraits && !isThreeTermMarks
                        else -> config.columns.map { it.id } == preset.columnIds
                    }
                    Surface(
                        onClick = {
                            val cols = BlankSheetCatalog.presetColumns(preset)
                            val suggestedOri = if (cols.size >= 9) {
                                BlankSheetOrientation.LANDSCAPE
                            } else {
                                BlankSheetOrientation.PORTRAIT
                            }
                            onUpdateConfig(config.copy(columns = cols, orientation = suggestedOri))
                            focusedColId = null
                            onResetPageIndex()
                        },
                        shape = RoundedCornerShape(7.dp),
                        color = if (isExact) TealPrimary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (isExact) TealPrimary else SlateOutline),
                        modifier = Modifier.heightIn(min = 32.dp).testTag("blank_preset_${preset.name.lowercase()}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                preset.labelBn,
                                fontSize = 10.5.sp,
                                fontWeight = if (isExact) FontWeight.Bold else FontWeight.Medium,
                                color = if (isExact) Color.White else SlateText
                            )
                        }
                    }
                }
            }

            if (isThreeTermMarks) {
                Surface(
                    onClick = {
                        val nextCols = BlankSheetCatalog.toggleThreeTermTotals(config.columns, !hasThreeTermTotal)
                        onUpdateConfig(config.copy(columns = nextCols))
                    },
                    shape = RoundedCornerShape(7.dp),
                    color = if (hasThreeTermTotal) TealSoft else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (hasThreeTermTotal) TealPrimary else SlateOutline),
                    modifier = Modifier.fillMaxWidth().testTag("blank_3term_total_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Checkbox(
                            checked = hasThreeTermTotal,
                            onCheckedChange = { checked ->
                                val nextCols = BlankSheetCatalog.toggleThreeTermTotals(config.columns, checked)
                                onUpdateConfig(config.copy(columns = nextCols))
                            },
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            "প্রতি প্রান্তিকে ঐচ্ছিক 'মোট' কলাম যোগ করুন",
                            fontSize = 10.5.sp,
                            fontWeight = if (hasThreeTermTotal) FontWeight.Bold else FontWeight.Medium,
                            color = if (hasThreeTermTotal) TealDark else SlateText
                        )
                    }
                }
            }
        }

        AdmitCollapsibleGroupCard(
            title = "২. কলাম বাছাই, যোগ ও ক্রম",
            summary = "${bn(config.columns.size.toString())}টি কলাম সক্রিয়",
            icon = Icons.Rounded.ViewColumn,
            initiallyExpanded = true,
            testTag = "blank_grp_columns"
        ) {
            val pickerColumns = remember(config.columns) {
                BlankSheetCatalog.pickerColumnsFor(config.columns)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                pickerColumns.forEach { catalogCol ->
                    val isAdded = config.columns.any { it.id == catalogCol.id }
                    Surface(
                        onClick = {
                            val updated = if (isAdded) {
                                if (config.columns.size > 1) config.columns.filterNot { it.id == catalogCol.id }
                                else config.columns
                            } else {
                                insertBlankColumnInOrder(config.columns, catalogCol)
                            }
                            onUpdateConfig(config.copy(columns = updated))
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
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                            }
                            Text(
                                catalogCol.pickerLabelBn,
                                fontSize = 10.5.sp,
                                fontWeight = if (isAdded) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAdded) Color.White else SlateText,
                                maxLines = 1
                            )
                        }
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

            AnimatedVisibility(visible = showCustomColumnInput) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AdmitCompactTextInput(
                        label = "নতুন কলামের নাম",
                        value = customColText,
                        onValueChange = { customColText = it },
                        placeholder = "যেমন: মৌখিক / স্বাক্ষর",
                        modifier = Modifier.weight(1f).testTag("blank_custom_col_input")
                    )
                    Button(
                        onClick = {
                            val clean = customColText.trim()
                            if (clean.isNotEmpty()) {
                                val newCol = BlankSheetCatalog.createCustomColumn(clean)
                                onUpdateConfig(config.copy(columns = config.columns + newCol))
                                customColText = ""
                                showCustomColumnInput = false
                            }
                        },
                        enabled = customColText.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(38.dp).testTag("blank_add_custom_col_btn")
                    ) {
                        Text("যোগ", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = SlateOutline.copy(alpha = 0.35f))
            Text(
                "কলামের ক্রম (বামে/ডানে সরাতে ট্যাপ করুন):",
                fontSize = 9.5.sp,
                color = SlateMuted
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                config.columns.forEachIndexed { idx, col ->
                    val isFocused = focusedColId == col.id
                    Surface(
                        onClick = { focusedColId = if (isFocused) null else col.id },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isFocused) TealContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (isFocused) TealPrimary else SlateOutline)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
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
                                            .size(18.dp)
                                            .clickable {
                                                val list = config.columns.toMutableList()
                                                val item = list.removeAt(idx)
                                                list.add(idx - 1, item)
                                                onUpdateConfig(config.copy(columns = list))
                                            }
                                    )
                                }
                                if (idx < config.columns.lastIndex) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = "ডানে সরান",
                                        tint = TealPrimary,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable {
                                                val list = config.columns.toMutableList()
                                                val item = list.removeAt(idx)
                                                list.add(idx + 1, item)
                                                onUpdateConfig(config.copy(columns = list))
                                            }
                                    )
                                }
                                if (config.columns.size > 1) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "বাদ দিন",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                onUpdateConfig(
                                                    config.copy(columns = config.columns.filterNot { it.id == col.id })
                                                )
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

@Composable
internal fun BlankSheetRowsAndPageTab(
    config: BlankSheetConfig,
    students: List<Student>,
    settings: Settings,
    onUpdateConfig: (BlankSheetConfig) -> Unit,
    onResetPageIndex: () -> Unit
) {
    var rowsMenuOpen by remember { mutableStateOf(false) }

    val rowModeSummary = if (config.rowFitMode == BlankSheetRowFitMode.AUTO_FIT_PAGES) {
        "${config.orientation.labelBn} · অটোফিট (${bn(config.autoFitPageCount.coerceIn(1, 3).toString())} পেজ)"
    } else {
        "${config.orientation.labelBn} · ${bn(config.rowsPerPage.toString())} সারি/পেজ"
    }

    val previewSplitHint = remember(config, students, settings) {
        if (config.rowFitMode != BlankSheetRowFitMode.AUTO_FIT_PAGES) return@remember null
        val targetPages = config.autoFitPageCount.coerceIn(1, 3)
        val selCls = config.selectedClass
        if (selCls == null) {
            "সব শ্রেণির প্রতিটিকে সমানভাবে ${bn(targetPages.toString())} পেজে ভাগ করে সাজাবে"
        } else {
            val stuCount = students.count { it.cls == selCls }
            val totalRows = stuCount + config.extraBlankRowsPerClass.coerceIn(0, 20)
            if (totalRows <= 0) {
                "${classShortBn(selCls, settings)} শ্রেণি: ${bn(targetPages.toString())} পেজে ফাঁকা ফর্ম"
            } else {
                val dummyRows = List(totalRows) { BlankSheetRow("", "", true) }
                val (_, chunks) = BlankSheetPlanner.partitionBalancedPages(
                    rows = dummyRows,
                    targetPages = targetPages,
                    fillRemaining = false
                )
                val breakdown = chunks.joinToString(" + ") { bn(it.size.toString()) }
                val densityWarn = if (chunks.any { it.size > 45 }) " (ঘন সারি — লেখা অটো-ছোট হবে)" else ""
                "${classShortBn(selCls, settings)}: ${bn(totalRows.toString())} সারি → $breakdown সারি/পেজ$densityWarn"
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AdmitCollapsibleGroupCard(
            title = "১. কাগজের দিক, সারি ও অটোফিট পেজ",
            summary = rowModeSummary,
            icon = Icons.Rounded.ViewAgenda,
            initiallyExpanded = true,
            testTag = "blank_grp_rows_fit"
        ) {
            AdmitSegmentedOptionBar(
                items = listOf(
                    BlankSheetOrientation.PORTRAIT to "খাড়া A4",
                    BlankSheetOrientation.LANDSCAPE to "আড়াআড়ি A4"
                ),
                selected = config.orientation,
                onSelect = { onUpdateConfig(config.copy(orientation = it)) },
                tagPrefix = "blank_ori"
            )

            Text(
                "প্রতি পেজ সারি ও অটোফিট মোড",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                color = SlateMuted
            )
            AdmitSegmentedOptionBar(
                items = BlankSheetRowFitMode.entries.map { it to it.labelBn },
                selected = config.rowFitMode,
                onSelect = { mode ->
                    onUpdateConfig(config.copy(rowFitMode = mode))
                    onResetPageIndex()
                },
                tagPrefix = "blank_row_mode"
            )

            if (config.rowFitMode == BlankSheetRowFitMode.FIXED_ROWS) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "প্রতি পেজে সারি সংখ্যা",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = SlateText,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            onClick = {
                                onUpdateConfig(
                                    config.copy(rowsPerPage = (config.rowsPerPage - 1).coerceAtLeast(5))
                                )
                                onResetPageIndex()
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = SlateBg,
                            border = BorderStroke(1.dp, SlateOutline),
                            modifier = Modifier.size(32.dp).testTag("blank_rows_dec_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Remove, contentDescription = "কমান", modifier = Modifier.size(16.dp), tint = SlateText)
                            }
                        }
                        Box {
                            Surface(
                                onClick = { rowsMenuOpen = true },
                                color = TealSoft,
                                border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.45f)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .height(32.dp)
                                    .testTag("blank_rows_value_pill")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 9.dp)
                                ) {
                                    Text(
                                        "${bn(config.rowsPerPage.toString())} জন",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TealDark
                                    )
                                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
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
                                            onUpdateConfig(config.copy(rowsPerPage = count))
                                            onResetPageIndex()
                                        }
                                    )
                                }
                            }
                        }
                        Surface(
                            onClick = {
                                onUpdateConfig(
                                    config.copy(rowsPerPage = (config.rowsPerPage + 1).coerceAtMost(50))
                                )
                                onResetPageIndex()
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = SlateBg,
                            border = BorderStroke(1.dp, SlateOutline),
                            modifier = Modifier.size(32.dp).testTag("blank_rows_inc_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Add, contentDescription = "বাড়ান", modifier = Modifier.size(16.dp), tint = SlateText)
                            }
                        }
                    }
                }
            } else {
                AdmitSegmentedOptionBar(
                    items = listOf(
                        1 to "১ পেজে ফিট",
                        2 to "২ পেজে ফিট",
                        3 to "৩ পেজে ফিট"
                    ),
                    selected = config.autoFitPageCount.coerceIn(1, 3),
                    onSelect = { pagesTarget ->
                        onUpdateConfig(config.copy(autoFitPageCount = pagesTarget))
                        onResetPageIndex()
                    },
                    useLightActivePill = true,
                    tagPrefix = "blank_autofit_pages"
                )
                previewSplitHint?.let { hint ->
                    Surface(
                        color = TealSoft,
                        border = BorderStroke(1.dp, TealContainer),
                        shape = RoundedCornerShape(7.dp),
                        modifier = Modifier.fillMaxWidth().testTag("blank_autofit_summary_badge")
                    ) {
                        Text(
                            text = hint,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TealDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        AdmitCollapsibleGroupCard(
            title = "২. অতিরিক্ত ফাঁকা সারি ও পেজ পূর্ণ",
            summary = if (config.extraBlankRowsPerClass > 0) "+${bn(config.extraBlankRowsPerClass.toString())} সারি" else "অতিরিক্ত নেই",
            icon = Icons.Rounded.Tune,
            initiallyExpanded = false,
            testTag = "blank_grp_extra_rows"
        ) {
            Text(
                "শ্রেণি শেষে অতিরিক্ত ফাঁকা সারি",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                color = SlateMuted
            )
            AdmitSegmentedOptionBar(
                items = listOf(0 to "নেই", 2 to "+২", 5 to "+৫", 10 to "+১০"),
                selected = config.extraBlankRowsPerClass,
                onSelect = { extra ->
                    onUpdateConfig(config.copy(extraBlankRowsPerClass = extra))
                    onResetPageIndex()
                },
                tagPrefix = "blank_extra_rows"
            )
            HorizontalDivider(color = SlateOutline.copy(alpha = 0.35f))
            AdmitCompactSwitchRow(
                label = "পেজের বাকি অংশ ফাঁকা সারি দিয়ে পূর্ণ করো",
                checked = config.fillRemainingPageRows,
                onCheckedChange = { onUpdateConfig(config.copy(fillRemainingPageRows = it)) },
                testTag = "blank_fill_remaining_switch"
            )
        }
    }
}

@Composable
internal fun BlankSheetQrAndOptionsTab(
    config: BlankSheetConfig,
    sharedWorkingDaysTerms: List<Int>,
    commonWorkingDays: Map<Int, String>,
    onUpdateConfig: (BlankSheetConfig) -> Unit,
    onResetAll: () -> Unit
) {
    val qrSummary = buildString {
        append(if (config.includeQrCode) "মূল QR: চালু" else "মূল QR: বন্ধ")
        append(" · ")
        append(
            if (config.includeBarcode) config.bottomCodeStyle.labelBn else "নিচের কোড: বন্ধ"
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AdmitCollapsibleGroupCard(
            title = "১. QR ও বারকোড নিয়ন্ত্রণ",
            summary = qrSummary,
            icon = Icons.Rounded.QrCode2,
            initiallyExpanded = true,
            testTag = "blank_grp_qr"
        ) {
            AdmitCompactSwitchRow(
                label = "উপরের স্মার্ট QR কোড (বাম ও ডান কোণায়)",
                checked = config.includeQrCode,
                onCheckedChange = { onUpdateConfig(config.copy(includeQrCode = it)) },
                testTag = "blank_sheet_qr_toggle"
            )
            HorizontalDivider(color = SlateOutline.copy(alpha = 0.35f))
            AdmitCompactSwitchRow(
                label = "নিচের কোড (ছোট QR / বারকোড)",
                checked = config.includeBarcode,
                onCheckedChange = { onUpdateConfig(config.copy(includeBarcode = it)) },
                testTag = "blank_sheet_barcode_toggle"
            )
            if (config.includeBarcode) {
                AdmitSegmentedOptionBar(
                    items = listOf(
                        BlankBottomCodeStyle.THIN_QR to "নিচে ছোট QR",
                        BlankBottomCodeStyle.CODE_128 to "নিচে বারকোড"
                    ),
                    selected = config.bottomCodeStyle,
                    onSelect = { onUpdateConfig(config.copy(bottomCodeStyle = it)) },
                    useLightActivePill = true,
                    tagPrefix = "blank_sheet_bottom_style"
                )
            }
        }

        AdmitCollapsibleGroupCard(
            title = "২. ডাটা, হেডার ও কার্যদিবস",
            summary = if (config.autoFillRollAndName) "রোল-নাম অটো" else "সম্পূর্ণ ফাঁকা",
            icon = Icons.Rounded.Settings,
            initiallyExpanded = true,
            testTag = "blank_grp_data_opts"
        ) {
            AdmitCompactSwitchRow(
                label = "রোল ও শিক্ষার্থীর নাম অটো-ফিল",
                checked = config.autoFillRollAndName,
                onCheckedChange = { onUpdateConfig(config.copy(autoFillRollAndName = it)) },
                testTag = "blank_autofill_switch"
            )
            HorizontalDivider(color = SlateOutline.copy(alpha = 0.35f))
            AdmitCompactSwitchRow(
                label = "কলাম হেডারে সর্বোচ্চ নম্বর দেখাও",
                checked = config.showSubjectMaxInHeader,
                onCheckedChange = { onUpdateConfig(config.copy(showSubjectMaxInHeader = it)) },
                testTag = "blank_max_header_switch"
            )

            if (sharedWorkingDaysTerms.isNotEmpty()) {
                HorizontalDivider(color = SlateOutline.copy(alpha = 0.35f))
                AdmitCompactSwitchRow(
                    label = "উপরে মোট কার্যদিবস ব্যানার",
                    checked = config.showSharedWorkingDaysHeader,
                    onCheckedChange = { onUpdateConfig(config.copy(showSharedWorkingDaysHeader = it)) },
                    testTag = "blank_shared_wd_toggle_chip"
                )
                if (config.showSharedWorkingDaysHeader) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val hasAnyFromData = sharedWorkingDaysTerms.any { !commonWorkingDays[it].isNullOrBlank() }
                        if (hasAnyFromData) {
                            Surface(
                                onClick = {
                                    val next = config.workingDaysByTerm.toMutableMap()
                                    sharedWorkingDaysTerms.forEach { t ->
                                        commonWorkingDays[t]?.let { next[t] = it }
                                    }
                                    onUpdateConfig(config.copy(workingDaysByTerm = next))
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
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (sharedWorkingDaysTerms.any { !config.workingDaysByTerm[it].isNullOrBlank() }) {
                            Surface(
                                onClick = {
                                    val next = config.workingDaysByTerm.toMutableMap()
                                    sharedWorkingDaysTerms.forEach { t -> next.remove(t) }
                                    onUpdateConfig(config.copy(workingDaysByTerm = next))
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
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        sharedWorkingDaysTerms.forEach { t ->
                            val rawVal = config.workingDaysByTerm[t].orEmpty()
                            val displayVal = if (rawVal.isEmpty()) {
                                ""
                            } else {
                                BlankSheetCatalog.formatDigits(rawVal, config.numeralStyle)
                            }
                            AdmitCompactTextInput(
                                label = termNameBn(t),
                                value = displayVal,
                                onValueChange = { txt ->
                                    val clean = fromBn(txt).filter { it.isDigit() }.take(3)
                                    val next = config.workingDaysByTerm.toMutableMap()
                                    if (clean.isEmpty()) next.remove(t) else next[t] = clean
                                    onUpdateConfig(config.copy(workingDaysByTerm = next))
                                },
                                placeholder = "লাইন",
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f).testTag("blank_wd_input_$t")
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = SlateOutline.copy(alpha = 0.35f))
            OutlinedButton(
                onClick = onResetAll,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SlateOutline),
                modifier = Modifier.fillMaxWidth().heightIn(min = 38.dp).testTag("blank_sheet_reset_btn")
            ) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("ডিফল্ট সেটিংস রিসেট করুন", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SlateText)
            }
        }
    }
}

private fun insertBlankColumnInOrder(
    current: List<BlankSheetColumn>,
    toAdd: BlankSheetColumn
): List<BlankSheetColumn> {
    val catalogOrder = BlankSheetCatalog.ALL_COLUMNS.mapIndexed { idx, c -> c.id to idx }.toMap()
    val targetIdx = catalogOrder[toAdd.id] ?: return current + toAdd
    val insertPos = current.indexOfFirst { (catalogOrder[it.id] ?: Int.MAX_VALUE) > targetIdx }
    return if (insertPos < 0) current + toAdd
    else current.toMutableList().apply { add(insertPos, toAdd) }
}
