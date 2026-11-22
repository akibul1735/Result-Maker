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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.TableRows
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.CsvDelimiterFormat
import com.kgps.result.CsvDigitMode
import com.kgps.result.CsvImportAssistant
import com.kgps.result.CsvImportModule
import com.kgps.result.CsvPromptConfig
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.activeSchoolClasses
import com.kgps.result.bn
import com.kgps.result.defaultSelectedColumnKeys
import com.kgps.result.effectiveTermsForConfig
import com.kgps.result.mk
import com.kgps.result.subjectsFor
import com.kgps.result.termOrdinalBn
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CsvStepOptions(
    config: CsvPromptConfig,
    settings: Settings,
    onConfigChange: (CsvPromptConfig) -> Unit
) {
    val customFields = settings.customFields
    val schoolClasses = remember(settings.customClasses) { activeSchoolClasses(settings) }
    val effectiveCls = if (config.targetCls in 1..20) config.targetCls else 3
    val availableCols = remember(
        config.module,
        config.targetCls,
        config.targetTerm,
        config.selectedSubjectKeys,
        config.allTermsSelection,
        config.multiTermSelection,
        config.conciseMarks,
        config.includeCustomFields,
        customFields
    ) {
        CsvImportAssistant.availableColumnsForConfig(config, customFields)
    }
    val groupedCols = remember(availableCols) { availableCols.groupBy { it.group } }
    val selectedCount = availableCols.count { it.required || config.selectedColumnKeys.contains(it.key) }
    val headerPreview = remember(config, settings) {
        CsvImportAssistant.buildHeaderLine(config, customFields, settings)
    }
    var columnsExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Module selector cards: Featured 3-Term Subject Sheet FIRST + 2x2 grid
        val gridModules = listOf(
            CsvImportModule.STUDENT_PROFILE,
            CsvImportModule.TERM_MARKS,
            CsvImportModule.ATTENDANCE_TRAITS,
            CsvImportModule.FULL_RECORD
        )
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            // Top featured card for Subject-Wise 3-Term Sheet (উপরে শ্রেণি ও বিষয়; ১ম-২য়-৩য় প্রান্তিক ধারাবাহিক/সামষ্টিক)
            val mod3T = CsvImportModule.SUBJECT_ALL_TERMS
            val sel3T = config.module == mod3T
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .testTag("csv_module_${mod3T.id}")
                    .clickable {
                        val safeCls = if (config.targetCls in 1..20) config.targetCls else 1
                        val clsSubKeys = subjectsFor(safeCls, "ইসলাম").map { it.key }
                        val firstSubKey = config.selectedSubjectKeys.firstOrNull { it in clsSubKeys } ?: clsSubKeys.first()
                        val nextSubs = setOf(firstSubKey)
                        val nextTerms = config.allTermsSelection.filter { it in 1..3 }.toSet().ifEmpty { setOf(1, 2, 3) }
                        val nextCols = defaultSelectedColumnKeys(
                            module = mod3T,
                            targetCls = safeCls,
                            targetTerm = config.targetTerm,
                            customFields = customFields,
                            selectedSubjects = nextSubs,
                            allTermsSelection = nextTerms,
                            multiTermSelection = config.multiTermSelection,
                            conciseMarks = config.conciseMarks
                        )
                        onConfigChange(
                            config.copy(
                                module = mod3T,
                                targetCls = safeCls,
                                selectedSubjectKeys = nextSubs,
                                allTermsSelection = nextTerms,
                                selectedColumnKeys = nextCols
                            )
                        )
                    },
                shape = RoundedCornerShape(10.dp),
                color = if (sel3T) TealContainer.copy(alpha = 0.85f) else TealSoft.copy(alpha = 0.55f),
                border = BorderStroke(
                    width = if (sel3T) 1.7.dp else 1.dp,
                    color = if (sel3T) TealPrimary else TealContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.TableChart,
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                mod3T.titleBn,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = TealDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                color = if (sel3T) TealPrimary else TealContainer,
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                Text(
                                    "৩ প্রান্তিক",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sel3T) Color.White else TealDark,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            mod3T.subtitleBn,
                            fontSize = 10.5.sp,
                            color = SlateMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            gridModules.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    pair.forEach { mod ->
                        val selected = config.module == mod
                        val icon = when (mod) {
                            CsvImportModule.STUDENT_PROFILE -> Icons.Rounded.Person
                            CsvImportModule.TERM_MARKS -> Icons.Rounded.GridOn
                            CsvImportModule.ATTENDANCE_TRAITS -> Icons.Rounded.FactCheck
                            CsvImportModule.FULL_RECORD -> Icons.Rounded.Description
                            CsvImportModule.SUBJECT_ALL_TERMS -> Icons.Rounded.TableChart
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .testTag("csv_module_${mod.id}")
                                .clickable {
                                    val nextMultiTerms = config.multiTermSelection.ifEmpty { setOf(config.targetTerm) }
                                    val nextCols = defaultSelectedColumnKeys(
                                        module = mod,
                                        targetCls = config.targetCls,
                                        targetTerm = config.targetTerm,
                                        customFields = customFields,
                                        selectedSubjects = config.selectedSubjectKeys,
                                        allTermsSelection = config.allTermsSelection,
                                        multiTermSelection = nextMultiTerms,
                                        conciseMarks = config.conciseMarks
                                    )
                                    onConfigChange(
                                        config.copy(
                                            module = mod,
                                            multiTermSelection = nextMultiTerms,
                                            selectedColumnKeys = nextCols
                                        )
                                    )
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) TealContainer.copy(alpha = 0.75f) else Color.White,
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) TealPrimary else SlateOutline
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (selected) TealPrimary else SlateMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    mod.titleBn,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = if (selected) TealDark else SlateText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Compact Class, Term & Subjects card
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateBg),
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("শ্রেণি:", fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, color = SlateMuted)
                    schoolClasses.forEach { cDef ->
                        val c = cDef.id
                        val isClsSelected = config.targetCls == c
                        CompactOptionPill(
                            label = cDef.shortBn,
                            selected = isClsSelected,
                            testTag = "csv_opt_cls_$c",
                            onClick = {
                                val nextSubs = if (config.module == CsvImportModule.SUBJECT_ALL_TERMS) {
                                    val subsForCls = subjectsFor(c, "ইসলাম").map { it.key }
                                    val kept = config.selectedSubjectKeys.firstOrNull { it in subsForCls } ?: subsForCls.first()
                                    setOf(kept)
                                } else config.selectedSubjectKeys
                                val nextCols = defaultSelectedColumnKeys(
                                    module = config.module,
                                    targetCls = c,
                                    targetTerm = config.targetTerm,
                                    customFields = customFields,
                                    selectedSubjects = nextSubs,
                                    allTermsSelection = config.allTermsSelection,
                                    multiTermSelection = config.multiTermSelection,
                                    conciseMarks = config.conciseMarks
                                )
                                onConfigChange(
                                    config.copy(
                                        targetCls = c,
                                        selectedSubjectKeys = nextSubs,
                                        selectedColumnKeys = nextCols
                                    )
                                )
                            }
                        )
                    }
                    if (config.module != CsvImportModule.SUBJECT_ALL_TERMS) {
                        CompactOptionPill(
                            label = "একাধিক",
                            selected = config.targetCls == 0,
                            testTag = "csv_opt_cls_multi",
                            onClick = {
                                val nextCols = defaultSelectedColumnKeys(
                                    module = config.module,
                                    targetCls = 0,
                                    targetTerm = config.targetTerm,
                                    customFields = customFields,
                                    selectedSubjects = config.selectedSubjectKeys,
                                    allTermsSelection = config.allTermsSelection,
                                    multiTermSelection = config.multiTermSelection,
                                    conciseMarks = config.conciseMarks
                                )
                                onConfigChange(config.copy(targetCls = 0, selectedColumnKeys = nextCols))
                            }
                        )
                    }
                }

                if (config.module == CsvImportModule.SUBJECT_ALL_TERMS) {
                    val clsSubjects = remember(effectiveCls, settings.customClasses) { subjectsFor(effectiveCls, "ইসলাম") }
                    val activeSub = CsvImportAssistant.resolveSubjectForAllTerms(config)
                    val activeTerms = effectiveTermsForConfig(config).toSet()
                    val all3Sel = activeTerms == setOf(1, 2, 3)
                    val hasTotalSelected = activeTerms.any { t ->
                        config.selectedColumnKeys.contains(mk(activeSub.key, t, "TOTAL"))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("বিষয়:", fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, color = SlateMuted)
                        clsSubjects.forEach { s ->
                            val isSelected = activeSub.key == s.key
                            CompactOptionPill(
                                label = s.bn.substringBefore(" ("),
                                selected = isSelected,
                                testTag = "csv_opt_sub_${s.key}",
                                onClick = {
                                    val nextSubs = setOf(s.key)
                                    val nextCols = defaultSelectedColumnKeys(
                                        module = config.module,
                                        targetCls = effectiveCls,
                                        targetTerm = config.targetTerm,
                                        customFields = customFields,
                                        selectedSubjects = nextSubs,
                                        allTermsSelection = activeTerms,
                                        multiTermSelection = config.multiTermSelection,
                                        conciseMarks = config.conciseMarks
                                    ).let { base ->
                                        if (hasTotalSelected) base + activeTerms.map { t -> mk(s.key, t, "TOTAL") } else base
                                    }
                                    onConfigChange(config.copy(selectedSubjectKeys = nextSubs, selectedColumnKeys = nextCols))
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("প্রান্তিক:", fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, color = SlateMuted)
                        CompactOptionPill(
                            label = "সব ৩টি",
                            selected = all3Sel,
                            testTag = "csv_opt_multi_term_all",
                            onClick = {
                                val nextTerms = setOf(1, 2, 3)
                                val nextCols = defaultSelectedColumnKeys(
                                    module = config.module,
                                    targetCls = effectiveCls,
                                    targetTerm = config.targetTerm,
                                    customFields = customFields,
                                    selectedSubjects = setOf(activeSub.key),
                                    allTermsSelection = nextTerms,
                                    multiTermSelection = config.multiTermSelection,
                                    conciseMarks = config.conciseMarks
                                ).let { base ->
                                    if (hasTotalSelected) base + nextTerms.map { t -> mk(activeSub.key, t, "TOTAL") } else base
                                }
                                onConfigChange(config.copy(allTermsSelection = nextTerms, selectedColumnKeys = nextCols))
                            }
                        )
                        (1..3).forEach { t ->
                            val isTermSel = activeTerms.contains(t)
                            CompactOptionPill(
                                label = termOrdinalBn(t),
                                selected = isTermSel,
                                testTag = "csv_opt_multi_term_$t",
                                onClick = {
                                    val nextTerms = if (isTermSel && activeTerms.size > 1) {
                                        activeTerms - t
                                    } else {
                                        activeTerms + t
                                    }
                                    val nextCols = defaultSelectedColumnKeys(
                                        module = config.module,
                                        targetCls = effectiveCls,
                                        targetTerm = config.targetTerm,
                                        customFields = customFields,
                                        selectedSubjects = setOf(activeSub.key),
                                        allTermsSelection = nextTerms,
                                        multiTermSelection = config.multiTermSelection,
                                        conciseMarks = config.conciseMarks
                                    ).let { base ->
                                        if (hasTotalSelected) base + nextTerms.map { tr -> mk(activeSub.key, tr, "TOTAL") } else base
                                    }
                                    onConfigChange(config.copy(allTermsSelection = nextTerms, selectedColumnKeys = nextCols))
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("কাঠামো:", fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, color = SlateMuted)
                        CompactOptionPill(
                            label = if (config.useTwoRowGroupedHeader) "২-সারি হেডার ✓" else "১-সারি হেডার",
                            selected = config.useTwoRowGroupedHeader,
                            testTag = "csv_opt_2row_header",
                            onClick = {
                                onConfigChange(config.copy(useTwoRowGroupedHeader = !config.useTwoRowGroupedHeader))
                            }
                        )
                        CompactOptionPill(
                            label = if (hasTotalSelected) "মোট কলাম সহ ✓" else "+ মোট কলাম",
                            selected = hasTotalSelected,
                            testTag = "csv_opt_toggle_total_col",
                            onClick = {
                                val nextCols = if (hasTotalSelected) {
                                    config.selectedColumnKeys - activeTerms.map { t -> mk(activeSub.key, t, "TOTAL") }.toSet()
                                } else {
                                    config.selectedColumnKeys + activeTerms.map { t -> mk(activeSub.key, t, "TOTAL") }
                                }
                                onConfigChange(config.copy(selectedColumnKeys = nextCols))
                            }
                        )
                    }

                    if (activeSub.written == 0) {
                        Text(
                            "নোট: ${activeSub.bn.substringBefore(" (")} বিষয়ে সামষ্টিক পরীক্ষা নেই (শুধু ধারাবাহিক ৫০)",
                            fontSize = 10.5.sp,
                            color = TealDark
                        )
                    }
                } else if (config.module != CsvImportModule.STUDENT_PROFILE) {
                    val activeMultiTerms = effectiveTermsForConfig(config).toSet()
                    val isAll3Terms = activeMultiTerms == setOf(1, 2, 3)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("প্রান্তিক:", fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, color = SlateMuted)
                        CompactOptionPill(
                            label = "সব ৩টি",
                            selected = isAll3Terms,
                            testTag = "csv_opt_term_all",
                            onClick = {
                                val nextTerms = setOf(1, 2, 3)
                                val nextCols = defaultSelectedColumnKeys(
                                    module = config.module,
                                    targetCls = config.targetCls,
                                    targetTerm = 1,
                                    customFields = customFields,
                                    selectedSubjects = config.selectedSubjectKeys,
                                    allTermsSelection = config.allTermsSelection,
                                    multiTermSelection = nextTerms,
                                    conciseMarks = config.conciseMarks
                                )
                                onConfigChange(
                                    config.copy(
                                        targetTerm = 1,
                                        multiTermSelection = nextTerms,
                                        selectedColumnKeys = nextCols
                                    )
                                )
                            }
                        )
                        (1..3).forEach { t ->
                            val isTermSel = activeMultiTerms.contains(t)
                            CompactOptionPill(
                                label = termOrdinalBn(t),
                                selected = isTermSel,
                                testTag = "csv_opt_term_$t",
                                onClick = {
                                    val nextTerms = if (isTermSel && activeMultiTerms.size > 1) {
                                        activeMultiTerms - t
                                    } else {
                                        activeMultiTerms + t
                                    }
                                    val nextPrimary = nextTerms.minOrNull() ?: 1
                                    val nextCols = defaultSelectedColumnKeys(
                                        module = config.module,
                                        targetCls = config.targetCls,
                                        targetTerm = nextPrimary,
                                        customFields = customFields,
                                        selectedSubjects = config.selectedSubjectKeys,
                                        allTermsSelection = config.allTermsSelection,
                                        multiTermSelection = nextTerms,
                                        conciseMarks = config.conciseMarks
                                    )
                                    onConfigChange(
                                        config.copy(
                                            targetTerm = nextPrimary,
                                            multiTermSelection = nextTerms,
                                            selectedColumnKeys = nextCols
                                        )
                                    )
                                }
                            )
                        }
                    }
                }

                if (config.module == CsvImportModule.TERM_MARKS || config.module == CsvImportModule.FULL_RECORD) {
                    val clsSubjects = remember(effectiveCls, settings.customClasses) { subjectsFor(effectiveCls, "ইসলাম") }
                    val activeMultiTerms = effectiveTermsForConfig(config).toSet()
                    val isConciseNow = config.conciseMarks || activeMultiTerms.size > 1
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("নম্বর:", fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, color = SlateMuted)
                        CompactOptionPill(
                            label = "ধারাবাহিক + সামষ্টিক",
                            selected = isConciseNow,
                            testTag = "csv_opt_marks_concise",
                            onClick = {
                                val nextCols = defaultSelectedColumnKeys(
                                    module = config.module,
                                    targetCls = config.targetCls,
                                    targetTerm = config.targetTerm,
                                    customFields = customFields,
                                    selectedSubjects = config.selectedSubjectKeys,
                                    allTermsSelection = config.allTermsSelection,
                                    multiTermSelection = activeMultiTerms,
                                    conciseMarks = true
                                )
                                onConfigChange(config.copy(conciseMarks = true, multiTermSelection = activeMultiTerms, selectedColumnKeys = nextCols))
                            }
                        )
                        CompactOptionPill(
                            label = "বিস্তারিত (১.১–১.৪)",
                            selected = !config.conciseMarks && activeMultiTerms.size == 1,
                            testTag = "csv_opt_marks_detailed",
                            onClick = {
                                val singleTermSet = setOf(activeMultiTerms.minOrNull() ?: config.targetTerm)
                                val nextCols = defaultSelectedColumnKeys(
                                    module = config.module,
                                    targetCls = config.targetCls,
                                    targetTerm = singleTermSet.first(),
                                    customFields = customFields,
                                    selectedSubjects = config.selectedSubjectKeys,
                                    allTermsSelection = config.allTermsSelection,
                                    multiTermSelection = singleTermSet,
                                    conciseMarks = false
                                )
                                onConfigChange(config.copy(conciseMarks = false, multiTermSelection = singleTermSet, selectedColumnKeys = nextCols))
                            }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("বিষয়:", fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, color = SlateMuted)
                        CompactOptionPill(
                            label = "সব",
                            selected = config.selectedSubjectKeys.isEmpty(),
                            testTag = "csv_opt_sub_all",
                            onClick = {
                                val nextCols = defaultSelectedColumnKeys(
                                    module = config.module,
                                    targetCls = config.targetCls,
                                    targetTerm = config.targetTerm,
                                    customFields = customFields,
                                    selectedSubjects = emptySet(),
                                    allTermsSelection = config.allTermsSelection,
                                    multiTermSelection = activeMultiTerms,
                                    conciseMarks = config.conciseMarks
                                )
                                onConfigChange(config.copy(selectedSubjectKeys = emptySet(), selectedColumnKeys = nextCols))
                            }
                        )
                        clsSubjects.forEach { s ->
                            val isSelected = config.selectedSubjectKeys.contains(s.key)
                            CompactOptionPill(
                                label = s.bn.substringBefore(" ("),
                                selected = isSelected,
                                testTag = "csv_opt_sub_${s.key}",
                                onClick = {
                                    val nextSubs = if (isSelected) {
                                        config.selectedSubjectKeys - s.key
                                    } else {
                                        config.selectedSubjectKeys + s.key
                                    }
                                    val nextCols = defaultSelectedColumnKeys(
                                        module = config.module,
                                        targetCls = config.targetCls,
                                        targetTerm = config.targetTerm,
                                        customFields = customFields,
                                        selectedSubjects = nextSubs,
                                        allTermsSelection = config.allTermsSelection,
                                        multiTermSelection = activeMultiTerms,
                                        conciseMarks = config.conciseMarks
                                    )
                                    onConfigChange(config.copy(selectedSubjectKeys = nextSubs, selectedColumnKeys = nextCols))
                                }
                            )
                        }
                    }
                }
            }
        }

        // Live Header Structure Preview inside Step 1 so the 3-term & metadata structure is immediately visible
        Surface(
            color = TealSoft.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, TealContainer),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    "শিট হেডার প্রিভিউ (${bn(selectedCount.toString())}টি কলাম):",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TealDark
                )
                Text(
                    headerPreview,
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    color = SlateText,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 3. Collapsible Columns Card (collapsed by default)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { columnsExpanded = !columnsExpanded }
                        .padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp)
                        .testTag("csv_cols_expand_toggle"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "কলাম (${bn(selectedCount.toString())}/${bn(availableCols.size.toString())}টি)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = SlateText
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        TextButton(
                            onClick = {
                                onConfigChange(config.copy(selectedColumnKeys = availableCols.map { it.key }.toSet()))
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp).testTag("csv_cols_select_all_btn")
                        ) {
                            Text("সব", fontSize = 11.sp)
                        }
                        TextButton(
                            onClick = {
                                onConfigChange(
                                    config.copy(
                                        selectedColumnKeys = availableCols.filter { it.required }.map { it.key }.toSet()
                                    )
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp).testTag("csv_cols_required_only_btn")
                        ) {
                            Text("জরুরি", fontSize = 11.sp)
                        }
                        Icon(
                            if (columnsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (columnsExpanded) "সংকুচিত করুন" else "কলাম দেখুন",
                            tint = SlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = columnsExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HorizontalDivider(color = SlateOutline.copy(alpha = 0.6f))
                        groupedCols.forEach { (groupName, groupCols) ->
                            Text(groupName, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = TealDark)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                groupCols.forEach { col ->
                                    val isChecked = col.required || config.selectedColumnKeys.contains(col.key)
                                    FilterChip(
                                        selected = isChecked,
                                        onClick = {
                                            if (!col.required) {
                                                val next = if (isChecked) {
                                                    config.selectedColumnKeys - col.key
                                                } else {
                                                    config.selectedColumnKeys + col.key
                                                }
                                                onConfigChange(config.copy(selectedColumnKeys = next))
                                            }
                                        },
                                        leadingIcon = {
                                            if (col.required) {
                                                Icon(Icons.Rounded.Lock, contentDescription = "আবশ্যক", modifier = Modifier.size(13.dp))
                                            } else if (isChecked) {
                                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                                            }
                                        },
                                        colors = if (col.required) {
                                            FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = TealContainer,
                                                selectedLabelColor = TealDark,
                                                selectedLeadingIconColor = TealDark
                                            )
                                        } else {
                                            appFilterChipColors()
                                        },
                                        border = appFilterChipBorder(isChecked),
                                        label = {
                                            Text(
                                                col.labelBn,
                                                fontSize = 11.sp,
                                                fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        },
                                        modifier = Modifier.testTag("csv_col_toggle_${col.key}")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Compact Format, Digit Mode & Note card
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateBg),
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Format segmented control
                    Surface(
                        color = Color.White,
                        border = BorderStroke(1.dp, SlateOutline),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Row(Modifier.padding(2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            CsvDelimiterFormat.entries.forEach { fmtOpt ->
                                val isFmtSelected = config.format == fmtOpt
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isFmtSelected) TealPrimary else Color.Transparent)
                                        .clickable { onConfigChange(config.copy(format = fmtOpt)) }
                                        .padding(horizontal = 4.dp, vertical = 6.dp)
                                        .testTag("csv_fmt_${fmtOpt.id}")
                                ) {
                                    Text(
                                        text = when (fmtOpt) {
                                            CsvDelimiterFormat.COMMA -> "CSV"
                                            CsvDelimiterFormat.TAB -> "ট্যাব"
                                            CsvDelimiterFormat.SEMICOLON -> "; CSV"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = if (isFmtSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isFmtSelected) Color.White else SlateText,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Digit segmented control
                    Surface(
                        color = Color.White,
                        border = BorderStroke(1.dp, SlateOutline),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(Modifier.padding(2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            CsvDigitMode.entries.forEach { digitOpt ->
                                val isDigitSelected = config.digitMode == digitOpt
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isDigitSelected) TealPrimary else Color.Transparent)
                                        .clickable { onConfigChange(config.copy(digitMode = digitOpt)) }
                                        .padding(horizontal = 4.dp, vertical = 6.dp)
                                        .testTag("csv_digit_${digitOpt.id}")
                                ) {
                                    Text(
                                        text = if (digitOpt == CsvDigitMode.ENGLISH) "0-9" else "০-৯",
                                        fontSize = 11.sp,
                                        fontWeight = if (isDigitSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isDigitSelected) Color.White else SlateText,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = config.extraInstruction,
                    onValueChange = { onConfigChange(config.copy(extraInstruction = it)) },
                    placeholder = { Text("অতিরিক্ত নির্দেশনা (ঐচ্ছিক, যেমন: রোল ১-৩০)", fontSize = 11.sp) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 12.sp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("csv_extra_instruction_input")
                )
            }
        }
    }
}

@Composable
private fun CompactOptionPill(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) TealPrimary else Color.White,
        border = BorderStroke(1.dp, if (selected) TealPrimary else SlateOutline),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else SlateText,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun CsvStepPrompt(
    config: CsvPromptConfig,
    students: List<Student>,
    settings: Settings,
    promptCopiedRecently: Boolean,
    onCopyPrompt: (String) -> Unit,
    onCopyHeaderOnly: (String) -> Unit,
    onCopyRosterTemplate: (String, Boolean) -> Unit,
    onSaveTemplateFile: (String) -> Unit = {},
    onGoToStep3: () -> Unit
) {
    val promptText = remember(config, settings) {
        CsvImportAssistant.buildAiPrompt(config, settings)
    }
    val headerOnly = remember(config, settings) {
        CsvImportAssistant.buildHeaderLine(config, settings.customFields, settings)
    }
    val selectedSpecs = remember(config, settings.customFields) {
        CsvImportAssistant.selectedColumnSpecs(config, settings.customFields)
    }
    val prefilledTemplate = remember(config, students, settings) {
        CsvImportAssistant.buildBlankTemplateCsv(config, students, settings, prefillRoster = true)
    }
    val blankTemplate = remember(config, students, settings) {
        CsvImportAssistant.buildBlankTemplateCsv(config, students, settings, prefillRoster = false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = TealSoft,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, TealContainer)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "ধাপ ২: AI প্রম্পট ও রেডি এক্সেল/CSV শিট",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = TealDark
                    )
                    Text(
                        "AI-তে ছবি/PDF থেকে ডেটা আনতে 'প্রম্পট কপি করুন', অথবা এক্সেল/গুগল শিটে নিজের হাতে নম্বর বসাতে 'রোল-নামসহ শিট কপি' ব্যবহার করুন।",
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        color = SlateText
                    )
                }
            }
        }

        // Metadata badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(color = SlateSurfaceVariant, shape = RoundedCornerShape(8.dp)) {
                Text(
                    "কলাম: ${bn(selectedSpecs.size.toString())}টি",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Surface(color = SlateSurfaceVariant, shape = RoundedCornerShape(8.dp)) {
                Text(
                    config.format.labelBn,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onCopyPrompt(promptText) },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("csv_copy_prompt_btn")
            ) {
                Icon(
                    if (promptCopiedRecently) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(if (promptCopiedRecently) "প্রম্পট কপি হয়েছে" else "প্রম্পট কপি করুন", fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = { onCopyHeaderOnly(headerOnly) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("csv_copy_header_btn")
            ) {
                Text("শুধু হেডার", fontSize = 12.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onCopyRosterTemplate(prefilledTemplate, true) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("csv_copy_roster_sheet_btn")
            ) {
                Icon(Icons.Rounded.TableRows, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("রোল-নামসহ শিট কপি", fontSize = 11.sp, maxLines = 1)
            }

            OutlinedButton(
                onClick = { onCopyRosterTemplate(blankTemplate, false) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(0.78f)
                    .testTag("csv_copy_blank_sheet_btn")
            ) {
                Text("ফাঁকা টেমপ্লেট", fontSize = 11.sp, maxLines = 1)
            }
        }

        OutlinedButton(
            onClick = { onSaveTemplateFile(prefilledTemplate) },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("csv_save_template_file_btn")
        ) {
            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("এক্সেল/CSV টেমপ্লেট ফাইল সেভ করুন (.csv)", fontSize = 12.sp, maxLines = 1)
        }

        Surface(
            color = SlateBg,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SlateOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("প্রম্পট প্রিভিউ", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = SlateMuted)
                Text(
                    text = promptText,
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp,
                    fontFamily = FontFamily.Monospace,
                    color = SlateText
                )
            }
        }

        OutlinedButton(
            onClick = onGoToStep3,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("csv_prompt_to_paste_btn")
        ) {
            Text("AI বা এক্সেল থেকে CSV পেয়েছেন? পরের ধাপে পেস্ট করুন →", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
    }
}
