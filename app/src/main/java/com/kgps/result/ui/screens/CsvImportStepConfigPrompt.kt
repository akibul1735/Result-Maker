package com.kgps.result.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.CLS_SHORT
import com.kgps.result.CsvDelimiterFormat
import com.kgps.result.CsvDigitMode
import com.kgps.result.CsvImportAssistant
import com.kgps.result.CsvImportModule
import com.kgps.result.CsvPromptConfig
import com.kgps.result.CustomFieldDef
import com.kgps.result.Settings
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CsvStepOptions(
    config: CsvPromptConfig,
    customFields: List<CustomFieldDef>,
    onConfigChange: (CsvPromptConfig) -> Unit
) {
    val effectiveCls = if (config.targetCls in 1..5) config.targetCls else 3
    val availableCols = remember(config.module, config.targetCls, config.targetTerm, config.selectedSubjectKeys, config.includeCustomFields, customFields) {
        CsvImportAssistant.availableColumnsForConfig(config, customFields)
    }
    val groupedCols = remember(availableCols) { availableCols.groupBy { it.group } }
    val selectedCount = availableCols.count { it.required || config.selectedColumnKeys.contains(it.key) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "১. কী ধরনের ডেটা ইমপোর্ট করতে চান তা বেছে নিন",
            style = MaterialTheme.typography.titleMedium,
            color = SlateText
        )

        // Module cards (2x2 or stacked gracefully)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CsvImportModule.entries.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) TealSoft else Color.White,
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) TealPrimary else SlateOutline
                            ),
                            tonalElevation = if (selected) 2.dp else 0.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = if (selected) TealPrimary else SlateMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        mod.titleBn,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (selected) TealDark else SlateText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    mod.subtitleBn,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = SlateMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // Target Class & Term
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateBg),
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("শ্রেণি ও প্রান্তিক নির্বাচন", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SlateText)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..5).forEach { c ->
                        FilterChip(
                            selected = config.targetCls == c,
                            onClick = {
                                val nextCols = defaultSelectedColumnKeys(
                                    module = config.module,
                                    targetCls = c,
                                    targetTerm = config.targetTerm,
                                    customFields = customFields,
                                    selectedSubjects = config.selectedSubjectKeys
                                )
                                onConfigChange(config.copy(targetCls = c, selectedColumnKeys = nextCols))
                            },
                            label = { Text("${CLS_SHORT[c - 1]} শ্রেণি", fontSize = 12.sp) },
                            modifier = Modifier.testTag("csv_opt_cls_$c")
                        )
                    }
                    FilterChip(
                        selected = config.targetCls == 0,
                        onClick = {
                            val nextCols = defaultSelectedColumnKeys(
                                module = config.module,
                                targetCls = 0,
                                targetTerm = config.targetTerm,
                                customFields = customFields,
                                selectedSubjects = config.selectedSubjectKeys
                            )
                            onConfigChange(config.copy(targetCls = 0, selectedColumnKeys = nextCols))
                        },
                        label = { Text("একাধিক শ্রেণি", fontSize = 12.sp) },
                        modifier = Modifier.testTag("csv_opt_cls_multi")
                    )
                }

                if (config.module != CsvImportModule.STUDENT_PROFILE) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..3).forEach { t ->
                            FilterChip(
                                selected = config.targetTerm == t,
                                onClick = {
                                    val nextCols = defaultSelectedColumnKeys(
                                        module = config.module,
                                        targetCls = config.targetCls,
                                        targetTerm = t,
                                        customFields = customFields,
                                        selectedSubjects = config.selectedSubjectKeys
                                    )
                                    onConfigChange(config.copy(targetTerm = t, selectedColumnKeys = nextCols))
                                },
                                label = { Text("${bn(t.toString())}ম প্রান্তিক", fontSize = 12.sp) },
                                modifier = Modifier.testTag("csv_opt_term_$t")
                            )
                        }
                    }
                }

                if (config.module == CsvImportModule.TERM_MARKS || config.module == CsvImportModule.FULL_RECORD) {
                    val clsSubjects = remember(effectiveCls) { subjectsFor(effectiveCls, "ইসলাম") }
                    Text("বিষয়সমূহ (ফাঁকা বা সবগুলো একসাথে বেছে নিন):", fontSize = 11.5.sp, color = SlateMuted)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = config.selectedSubjectKeys.isEmpty(),
                            onClick = {
                                val nextCols = defaultSelectedColumnKeys(
                                    module = config.module,
                                    targetCls = config.targetCls,
                                    targetTerm = config.targetTerm,
                                    customFields = customFields,
                                    selectedSubjects = emptySet()
                                )
                                onConfigChange(config.copy(selectedSubjectKeys = emptySet(), selectedColumnKeys = nextCols))
                            },
                            label = { Text("সব বিষয়", fontSize = 11.5.sp) },
                            modifier = Modifier.testTag("csv_opt_sub_all")
                        )
                        clsSubjects.forEach { s ->
                            val isSelected = config.selectedSubjectKeys.contains(s.key)
                            FilterChip(
                                selected = isSelected,
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
                                },
                                label = { Text(s.bn.substringBefore(" ("), fontSize = 11.5.sp) },
                                modifier = Modifier.testTag("csv_opt_sub_${s.key}")
                            )
                        }
                    }
                }
            }
        }

        // Columns toggle card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "প্রয়োজনীয় কলাম (${bn(selectedCount.toString())}/${bn(availableCols.size.toString())}টি)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = SlateText
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                onConfigChange(config.copy(selectedColumnKeys = availableCols.map { it.key }.toSet()))
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("csv_cols_select_all_btn")
                        ) {
                            Text("সব দিন", fontSize = 11.5.sp)
                        }
                        TextButton(
                            onClick = {
                                onConfigChange(
                                    config.copy(
                                        selectedColumnKeys = availableCols.filter { it.required }.map { it.key }.toSet()
                                    )
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("csv_cols_required_only_btn")
                        ) {
                            Text("শুধু জরুরি", fontSize = 11.5.sp)
                        }
                    }
                }

                groupedCols.forEach { (groupName, groupCols) ->
                    Text(groupName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TealDark)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                        Icon(Icons.Rounded.Lock, contentDescription = "আবশ্যক", modifier = Modifier.size(14.dp))
                                    } else if (isChecked) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (col.required) TealContainer else TealSoft,
                                    selectedLabelColor = TealDark
                                ),
                                label = { Text(col.labelBn, fontSize = 11.5.sp) },
                                modifier = Modifier.testTag("csv_col_toggle_${col.key}")
                            )
                        }
                    }
                }
            }
        }

        // Format & Digit Style
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateBg),
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("আউটপুট ফরম্যাট ও সংখ্যার ধরন", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SlateText)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CsvDelimiterFormat.entries.forEach { fmtOpt ->
                        FilterChip(
                            selected = config.format == fmtOpt,
                            onClick = { onConfigChange(config.copy(format = fmtOpt)) },
                            label = { Text(fmtOpt.labelBn, fontSize = 12.sp) },
                            modifier = Modifier.testTag("csv_fmt_${fmtOpt.id}")
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CsvDigitMode.entries.forEach { digitOpt ->
                        FilterChip(
                            selected = config.digitMode == digitOpt,
                            onClick = { onConfigChange(config.copy(digitMode = digitOpt)) },
                            label = { Text(digitOpt.labelBn, fontSize = 12.sp) },
                            modifier = Modifier.testTag("csv_digit_${digitOpt.id}")
                        )
                    }
                }
                OutlinedTextField(
                    value = config.extraInstruction,
                    onValueChange = { onConfigChange(config.copy(extraInstruction = it)) },
                    label = { Text("AI-কে অতিরিক্ত নির্দেশনা (ঐচ্ছিক, যেমন: রোল ১-৩০)", fontSize = 11.5.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("csv_extra_instruction_input")
                )
            }
        }
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
