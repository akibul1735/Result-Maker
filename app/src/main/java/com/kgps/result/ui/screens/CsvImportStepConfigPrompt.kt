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
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
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
import com.kgps.result.activeSchoolClasses
import com.kgps.result.bn
import com.kgps.result.defaultSelectedColumnKeys
import com.kgps.result.subjectsFor
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
    val availableCols = remember(config.module, config.targetCls, config.targetTerm, config.selectedSubjectKeys, config.includeCustomFields, customFields) {
        CsvImportAssistant.availableColumnsForConfig(config, customFields)
    }
    val groupedCols = remember(availableCols) { availableCols.groupBy { it.group } }
    val selectedCount = availableCols.count { it.required || config.selectedColumnKeys.contains(it.key) }
    var columnsExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Compact 2x2 single-row module pills
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            CsvImportModule.entries.chunked(2).forEach { pair ->
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
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .testTag("csv_module_${mod.id}")
                                .clickable {
                                    val nextCols = defaultSelectedColumnKeys(
                                        module = mod,
                                        targetCls = config.targetCls,
                                        targetTerm = config.targetTerm,
                                        customFields = customFields,
                                        selectedSubjects = config.selectedSubjectKeys
                                    )
                                    onConfigChange(config.copy(module = mod, selectedColumnKeys = nextCols))
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
                                val nextCols = defaultSelectedColumnKeys(
                                    module = config.module,
                                    targetCls = c,
                                    targetTerm = config.targetTerm,
                                    customFields = customFields,
                                    selectedSubjects = config.selectedSubjectKeys
                                )
                                onConfigChange(config.copy(targetCls = c, selectedColumnKeys = nextCols))
                            }
                        )
                    }
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
                                selectedSubjects = config.selectedSubjectKeys
                            )
                            onConfigChange(config.copy(targetCls = 0, selectedColumnKeys = nextCols))
                        }
                    )
                }

                if (config.module != CsvImportModule.STUDENT_PROFILE) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("প্রান্তিক:", fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, color = SlateMuted)
                        (1..3).forEach { t ->
                            CompactOptionPill(
                                label = "${bn(t.toString())}ম",
                                selected = config.targetTerm == t,
                                testTag = "csv_opt_term_$t",
                                onClick = {
                                    val nextCols = defaultSelectedColumnKeys(
                                        module = config.module,
                                        targetCls = config.targetCls,
                                        targetTerm = t,
                                        customFields = customFields,
                                        selectedSubjects = config.selectedSubjectKeys
                                    )
                                    onConfigChange(config.copy(targetTerm = t, selectedColumnKeys = nextCols))
                                }
                            )
                        }
                    }
                }

                if (config.module == CsvImportModule.TERM_MARKS || config.module == CsvImportModule.FULL_RECORD) {
                    val clsSubjects = remember(effectiveCls, settings.customClasses) { subjectsFor(effectiveCls, "ইসলাম") }
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
                                    selectedSubjects = emptySet()
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
                                        selectedSubjects = nextSubs
                                    )
                                    onConfigChange(config.copy(selectedSubjectKeys = nextSubs, selectedColumnKeys = nextCols))
                                }
                            )
                        }
                    }
                }
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
    settings: Settings,
    promptCopiedRecently: Boolean,
    onCopyPrompt: (String) -> Unit,
    onCopyHeaderOnly: (String) -> Unit,
    onGoToStep3: () -> Unit
) {
    val promptText = remember(config, settings) {
        CsvImportAssistant.buildAiPrompt(config, settings)
    }
    val headerOnly = remember(config, settings.customFields) {
        CsvImportAssistant.buildHeaderLine(config, settings.customFields)
    }
    val selectedSpecs = remember(config, settings.customFields) {
        CsvImportAssistant.selectedColumnSpecs(config, settings.customFields)
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
                        "ধাপ ২: রেডিমেড AI প্রম্পট তৈরি হয়েছে",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = TealDark
                    )
                    Text(
                        "এই প্রম্পটটি কপি করে ChatGPT, Gemini বা যেকোনো AI-তে খাতার ছবি, PDF বা লেখা সহ দিন। AI হুবহু এই অ্যাপের ফরম্যাটে CSV বানিয়ে দেবে।",
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
            Text("AI থেকে CSV পেয়েছেন? পরের ধাপে পেস্ট করুন →", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
    }
}
