@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.kgps.result.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.AdmitCardConfig
import com.kgps.result.AdmitCardPlanner
import com.kgps.result.BaseRoutineTemplate
import com.kgps.result.ExamRoutineEntry
import com.kgps.result.Settings
import com.kgps.result.bn
import com.kgps.result.classShortBn
import com.kgps.result.ui.theme.AmberBg
import com.kgps.result.ui.theme.AmberBorder
import com.kgps.result.ui.theme.AmberNotice
import com.kgps.result.ui.theme.RoseDanger
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft

@Composable
internal fun AdmitCardRoutineEditor(
    config: AdmitCardConfig,
    settings: Settings,
    allowedClasses: List<Int>,
    baseRoutines: List<BaseRoutineTemplate>,
    activeBaseId: String?, // non-null => editing that base routine; null => editing config.selectedClass
    classRoutineRows: List<ExamRoutineEntry>,
    onSelectBase: (String) -> Unit,
    onSelectClass: (Int) -> Unit,
    onAddBaseRoutine: (String) -> Unit,
    onRenameBaseRoutine: (String, String) -> Unit,
    onDeleteBaseRoutine: (String) -> Unit,
    onUpdateActiveExamTime: (String) -> Unit,
    onUpdateActiveRows: (List<ExamRoutineEntry>) -> Unit,
    onResetClassDefault: () -> Unit,
    onSaveActiveFeedback: () -> Unit,
    onCopyToClasses: (targetClasses: List<Int>, matchClassSubjects: Boolean, copyExamTime: Boolean) -> Unit,
    onUpdateConfig: (AdmitCardConfig) -> Unit
) {
    val activeBase = remember(activeBaseId, baseRoutines) {
        if (activeBaseId == null) null else baseRoutines.find { it.id == activeBaseId }
    }
    val isEditingBase = activeBase != null
    val activeRows = activeBase?.rows ?: classRoutineRows
    val activeExamTime = activeBase?.examTimeLabel ?: config.examTimeLabel
    val activeTargetLabel = activeBase?.name ?: "${classShortBn(config.selectedClass, settings)} শ্রেণি"

    val subjectOptions = remember(isEditingBase, config.selectedClass, settings) {
        if (isEditingBase) {
            AdmitCardPlanner.allSchoolSubjectLabels(settings)
        } else {
            AdmitCardPlanner.classSubjectLabelsFor(config.selectedClass, settings)
        }
    }

    var datePickerRowIndex by remember { mutableIntStateOf(-1) }
    var showCreateBaseDialog by remember { mutableStateOf(false) }
    var renamingBase by remember { mutableStateOf<BaseRoutineTemplate?>(null) }
    var showCopyDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // 1. Top Horizontal Selector Strip: Base Routines + New Base + Class Chips
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    baseRoutines.forEach { base ->
                        val isSel = base.id == activeBase?.id
                        Surface(
                            onClick = { onSelectBase(base.id) },
                            color = if (isSel) AmberBg else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isSel) AmberBorder else SlateOutline),
                            shape = RoundedCornerShape(7.dp),
                            modifier = Modifier.heightIn(min = 32.dp).testTag("admit_base_chip_${base.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                if (isSel) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = AmberNotice, modifier = Modifier.size(12.dp))
                                }
                                Icon(Icons.Rounded.Star, contentDescription = null, tint = AmberNotice, modifier = Modifier.size(12.dp))
                                Text(
                                    base.name,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSel) Color(0xFF78350F) else SlateText,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = { showCreateBaseDialog = true },
                        color = SlateBg,
                        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(7.dp),
                        modifier = Modifier.heightIn(min = 32.dp).testTag("admit_add_base_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(13.dp))
                            Text("নতুন বেস", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = TealPrimary)
                        }
                    }

                    allowedClasses.forEach { c ->
                        val isSel = !isEditingBase && c == config.selectedClass
                        Surface(
                            onClick = { onSelectClass(c) },
                            color = if (isSel) TealSoft else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isSel) TealPrimary else SlateOutline),
                            shape = RoundedCornerShape(7.dp),
                            modifier = Modifier.heightIn(min = 32.dp).testTag("admit_routine_cls_$c")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                if (isSel) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = TealDark, modifier = Modifier.size(12.dp))
                                }
                                Text(
                                    "${classShortBn(c, settings)} শ্রেণি",
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) TealDark else SlateText
                                )
                            }
                        }
                    }
                }

                if (activeBase != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "বেস সম্পাদনা (প্রিভিউ নিচে ${classShortBn(config.selectedClass, settings)} শ্রেণির)",
                            fontSize = 10.5.sp,
                            color = AmberNotice,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                onClick = { renamingBase = activeBase },
                                color = SlateBg,
                                border = BorderStroke(1.dp, SlateOutline),
                                shape = RoundedCornerShape(7.dp),
                                modifier = Modifier.heightIn(min = 36.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "নাম বদলান", tint = SlateMuted, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("নাম", fontSize = 10.5.sp, color = SlateText)
                                }
                            }
                            if (baseRoutines.size > 1) {
                                Surface(
                                    onClick = { onDeleteBaseRoutine(activeBase.id) },
                                    color = SlateBg,
                                    border = BorderStroke(1.dp, RoseDanger.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(7.dp),
                                    modifier = Modifier.heightIn(min = 36.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "বেস মুছুন", tint = RoseDanger, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text("মুছুন", fontSize = 10.5.sp, color = RoseDanger)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Exam Time Box + Quick Presets (Matching screenshot)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(7.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                AdmitCompactTextInput(
                    label = if (isEditingBase) "পরীক্ষার সময় (BASE)" else "পরীক্ষার সময় (${classShortBn(config.selectedClass, settings)})",
                    value = activeExamTime,
                    onValueChange = onUpdateActiveExamTime,
                    placeholder = "দুপুর ০১:০০-০৩:৩০ টা",
                    testTag = "admit_card_exam_time_input"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    AdmitCardPlanner.EXAM_TIME_PRESETS.forEach { preset ->
                        val isSel = activeExamTime.trim() == preset
                        Surface(
                            onClick = { onUpdateActiveExamTime(preset) },
                            color = if (isSel) TealSoft else SlateBg,
                            border = BorderStroke(1.dp, if (isSel) TealPrimary else SlateOutline),
                            shape = RoundedCornerShape(7.dp),
                            modifier = Modifier.heightIn(min = 30.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    preset,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) TealDark else SlateText
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Days & Subjects List Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "${activeTargetLabel}-এর দিন ও বিষয়",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = TealDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${bn(activeRows.size.toString())} টি দিন",
                fontSize = 11.5.sp,
                color = SlateMuted
            )
        }

        // 4. Day Cards (Calendar pill left + Multi-subject stack middle + Add/Delete right)
        activeRows.forEachIndexed { idx, row ->
            RoutineDayCard(
                index = idx,
                entry = row,
                subjectOptions = subjectOptions,
                isBaseMode = isEditingBase,
                onPickDateClick = { datePickerRowIndex = idx },
                onUpdateEntry = { updated ->
                    onUpdateActiveRows(activeRows.mapIndexed { i, item -> if (i == idx) updated else item })
                },
                onDeleteDay = {
                    onUpdateActiveRows(activeRows.filterIndexed { i, _ -> i != idx })
                }
            )
        }

        // 5. Bottom 3-Button Action Bar (matching screenshot: + নতুন দিন | সেভ | শ্রেণিতে কপি)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onUpdateActiveRows(activeRows + ExamRoutineEntry(date = "", day = "", subjects = listOf("")))
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 36.dp).testTag("admit_card_add_row_btn")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("নতুন দিন", fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Button(
                    onClick = onSaveActiveFeedback,
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 36.dp).testTag("admit_card_save_routine_btn")
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(
                        if (isEditingBase) "বেস সেভ" else "শ্রেণি সেভ",
                        fontSize = 10.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = { showCopyDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.weight(1.1f).heightIn(min = 36.dp).testTag("admit_card_copy_classes_btn")
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("শ্রেণিতে কপি", fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // Extra helper controls (Load class curriculum default, Custom routine title, Bottom blank row switch)
        AdmitSectionGroupCard(title = "রুটিন অপশন") {
            if (!isEditingBase) {
                OutlinedButton(
                    onClick = onResetClassDefault,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 34.dp).testTag("admit_card_reset_routine_btn")
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${classShortBn(config.selectedClass, settings)} শ্রেণির মূল বিষয় লোড করুন", fontSize = 10.5.sp)
                }
            }
            AdmitCompactTextInput(
                label = "কাস্টম রুটিন শিরোনাম (ঐচ্ছিক)",
                value = config.customRoutineTitle,
                onValueChange = { onUpdateConfig(config.copy(customRoutineTitle = it)) },
                placeholder = AdmitCardPlanner.resolvedRoutineTitle(config.copy(customRoutineTitle = ""), settings)
            )
            AdmitCompactTextInput(
                label = "রুটিনের নিচে নোটস (ঐচ্ছিক - অটো ফিট)",
                value = config.routineNote,
                onValueChange = { onUpdateConfig(config.copy(routineNote = it)) },
                placeholder = "যেমন: পরীক্ষার হলে ১৫ মিনিট আগে উপস্থিত থাকতে হবে",
                testTag = "admit_card_routine_note_input"
            )
            AdmitCompactSwitchRow(
                label = "রুটিনের নিচে ১টি ফাঁকা সারি দেখাও",
                checked = config.includeBlankRoutineRow,
                onCheckedChange = { onUpdateConfig(config.copy(includeBlankRoutineRow = it)) },
                testTag = "admit_card_blank_routine_row"
            )
        }
    }

    if (datePickerRowIndex in activeRows.indices) {
        val targetRow = activeRows[datePickerRowIndex]
        AdmitRoutineDatePickerDialog(
            initialDateText = targetRow.date,
            onDismiss = { datePickerRowIndex = -1 },
            onDatePicked = { dateBn, weekdayBn ->
                val updated = activeRows.mapIndexed { idx, r ->
                    if (idx == datePickerRowIndex) r.copy(date = dateBn, day = weekdayBn) else r
                }
                onUpdateActiveRows(updated)
                datePickerRowIndex = -1
            }
        )
    }

    if (showCreateBaseDialog) {
        BaseRoutineNameDialog(
            title = "নতুন বেস রুটিন তৈরি",
            initialName = "বেস ${bn((baseRoutines.size + 1).toString())}",
            confirmLabel = "তৈরি করুন",
            onDismiss = { showCreateBaseDialog = false },
            onConfirm = { newName ->
                onAddBaseRoutine(newName)
                showCreateBaseDialog = false
            }
        )
    }

    renamingBase?.let { base ->
        BaseRoutineNameDialog(
            title = "বেস রুটিনের নাম বদলান",
            initialName = base.name,
            confirmLabel = "সংরক্ষণ",
            onDismiss = { renamingBase = null },
            onConfirm = { nextName ->
                onRenameBaseRoutine(base.id, nextName)
                renamingBase = null
            }
        )
    }

    if (showCopyDialog) {
        CopyRoutineToClassesDialog(
            sourceLabel = activeTargetLabel,
            allowedClasses = allowedClasses,
            settings = settings,
            onDismiss = { showCopyDialog = false },
            onConfirm = { targets, matchSubs, copyTime ->
                onCopyToClasses(targets, matchSubs, copyTime)
                showCopyDialog = false
            }
        )
    }
}

@Composable
private fun RoutineDayCard(
    index: Int,
    entry: ExamRoutineEntry,
    subjectOptions: List<String>,
    isBaseMode: Boolean,
    onPickDateClick: () -> Unit,
    onUpdateEntry: (ExamRoutineEntry) -> Unit,
    onDeleteDay: () -> Unit
) {
    val slots = entry.subjects.ifEmpty { listOf("") }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SlateOutline),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Left: Compact Clickable Calendar Date & Auto Weekday Pill
            Surface(
                onClick = onPickDateClick,
                color = Color(0xFFE6F4EC),
                border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.28f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .widthIn(min = 76.dp, max = 90.dp)
                    .heightIn(min = 42.dp)
                    .testTag("admit_day_date_btn_$index")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (entry.date.isBlank()) {
                        Icon(
                            Icons.Rounded.CalendarMonth,
                            contentDescription = "তারিখ বাছুন",
                            tint = TealDark,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            "তারিখ",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TealDark,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            entry.date,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealDark,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Text(
                            entry.day.ifBlank { AdmitCardPlanner.inferBanglaWeekday(entry.date).ifBlank { "বার" } },
                            fontSize = 9.5.sp,
                            color = TealPrimary,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }

            // Middle: Compact Subject Slots
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                slots.forEachIndexed { sIdx, subjText ->
                    var menuOpen by remember { mutableStateOf(false) }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = SlateBg,
                                border = BorderStroke(1.dp, SlateOutline),
                                shape = RoundedCornerShape(7.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 32.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 7.dp, end = 2.dp, top = 3.dp, bottom = 3.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.CenterStart,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (subjText.isEmpty()) {
                                            Text(
                                                text = "বিষয় ${bn((sIdx + 1).toString())}",
                                                fontSize = 10.5.sp,
                                                color = SlateMuted.copy(alpha = 0.65f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        BasicTextField(
                                            value = subjText,
                                            onValueChange = { nextVal ->
                                                val updatedSlots = slots.mapIndexed { i, v -> if (i == sIdx) nextVal else v }
                                                onUpdateEntry(entry.copy(subjects = updatedSlots))
                                            },
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                fontSize = 11.sp,
                                                color = SlateText
                                            ),
                                            cursorBrush = SolidColor(TealPrimary),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("admit_subj_input_${index}_$sIdx")
                                        )
                                    }
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clickable { menuOpen = true }
                                            .testTag("admit_subj_picker_${index}_$sIdx")
                                    ) {
                                        Icon(
                                            Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = "বিষয় তালিকা",
                                            tint = SlateMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false }
                            ) {
                                subjectOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt, fontSize = 12.sp) },
                                        onClick = {
                                            val updatedSlots = slots.mapIndexed { i, v -> if (i == sIdx) opt else v }
                                            onUpdateEntry(entry.copy(subjects = updatedSlots))
                                            menuOpen = false
                                        }
                                    )
                                }
                                if (isBaseMode && subjText.trim().isNotEmpty()) {
                                    HorizontalDivider()
                                    subjectOptions
                                        .filter { !subjText.contains(it, ignoreCase = true) }
                                        .take(4)
                                        .forEach { alt ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text("অথবা: ${subjText.trim()} / $alt", fontSize = 11.sp, color = TealDark)
                                                },
                                                onClick = {
                                                    val combined = "${subjText.trim()} / $alt"
                                                    val updatedSlots = slots.mapIndexed { i, v -> if (i == sIdx) combined else v }
                                                    onUpdateEntry(entry.copy(subjects = updatedSlots))
                                                    menuOpen = false
                                                }
                                            )
                                        }
                                }
                            }
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    val nextSlots = if (slots.size > 1) {
                                        slots.filterIndexed { i, _ -> i != sIdx }
                                    } else {
                                        listOf("")
                                    }
                                    onUpdateEntry(entry.copy(subjects = nextSlots))
                                }
                                .testTag("admit_subj_remove_${index}_$sIdx")
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "বিষয় বাদ দিন",
                                tint = RoseDanger,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Right: Compact + Add subject & Delete day icons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onUpdateEntry(entry.copy(subjects = slots + "")) }
                        .testTag("admit_day_add_subj_$index")
                ) {
                    Icon(
                        Icons.Rounded.AddCircleOutline,
                        contentDescription = "একই দিনে আরেকটি বিষয় যোগ",
                        tint = TealPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable(onClick = onDeleteDay)
                        .testTag("admit_day_del_$index")
                ) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "দিন মুছুন",
                        tint = RoseDanger,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}
