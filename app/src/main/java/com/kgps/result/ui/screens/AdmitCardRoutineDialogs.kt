@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.kgps.result.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.AdmitCardConfig
import com.kgps.result.AdmitCardPageSpec
import com.kgps.result.AdmitCardPlanner
import com.kgps.result.ExamRoutineEntry
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.bn
import com.kgps.result.classNameBn
import com.kgps.result.classShortBn
import com.kgps.result.syllabusTierForClass
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft
import com.kgps.result.ui.theme.appFilterChipBorder
import com.kgps.result.ui.theme.appFilterChipColors

@Composable
internal fun AdmitRoutineDatePickerDialog(
    initialDateText: String,
    onDismiss: () -> Unit,
    onDatePicked: (dateBn: String, weekdayBn: String) -> Unit
) {
    val initialMillis = remember(initialDateText) {
        AdmitCardPlanner.parseDateToUtcMillis(initialDateText) ?: System.currentTimeMillis()
    }
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val millis = pickerState.selectedDateMillis ?: initialMillis
                    val (dateBn, dayBn) = AdmitCardPlanner.formatDateAndDayFromUtcMillis(millis)
                    onDatePicked(dateBn, dayBn)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.heightIn(min = 48.dp).testTag("admit_date_picker_confirm")
            ) {
                Text("নির্বাচন করুন")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("বাতিল")
            }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

@Composable
internal fun BaseRoutineNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("বেস রুটিনের নাম", fontSize = 12.sp) },
                placeholder = { Text("যেমন: মূল বেস ১ (৩য়–৫ম)", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("base_routine_name_input")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = name.trim()
                    if (clean.isNotEmpty()) onConfirm(clean)
                },
                enabled = name.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
internal fun CopyRoutineToClassesDialog(
    sourceLabel: String,
    allowedClasses: List<Int>,
    settings: Settings,
    onDismiss: () -> Unit,
    onConfirm: (targetClasses: List<Int>, matchClassSubjects: Boolean, copyExamTime: Boolean) -> Unit
) {
    var selectedClasses by remember(allowedClasses) { mutableStateOf(allowedClasses.toSet()) }
    var matchClassSubjects by remember { mutableStateOf(true) }
    var copyExamTime by remember { mutableStateOf(false) }

    val lowerClasses = remember(allowedClasses, settings) {
        allowedClasses.filter { syllabusTierForClass(it, settings) <= 2 }
    }
    val upperClasses = remember(allowedClasses, settings) {
        allowedClasses.filter { syllabusTierForClass(it, settings) >= 3 }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("শ্রেণিতে রুটিন কপি করুন", fontWeight = FontWeight.Bold, fontSize = 15.5.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "উৎস: $sourceLabel",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TealPrimary
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = selectedClasses.size == allowedClasses.size,
                        onClick = { selectedClasses = allowedClasses.toSet() },
                        label = { Text("সব শ্রেণি", fontSize = 11.sp) },
                        colors = appFilterChipColors(),
                        border = appFilterChipBorder(selectedClasses.size == allowedClasses.size),
                        modifier = Modifier.heightIn(min = 40.dp)
                    )
                    if (lowerClasses.isNotEmpty()) {
                        val isLower = selectedClasses == lowerClasses.toSet()
                        FilterChip(
                            selected = isLower,
                            onClick = { selectedClasses = lowerClasses.toSet() },
                            label = { Text("প্রাক–২য়", fontSize = 11.sp) },
                            colors = appFilterChipColors(),
                            border = appFilterChipBorder(isLower),
                            modifier = Modifier.heightIn(min = 40.dp)
                        )
                    }
                    if (upperClasses.isNotEmpty()) {
                        val isUpper = selectedClasses == upperClasses.toSet()
                        FilterChip(
                            selected = isUpper,
                            onClick = { selectedClasses = upperClasses.toSet() },
                            label = { Text("৩য়–৫ম", fontSize = 11.sp) },
                            colors = appFilterChipColors(),
                            border = appFilterChipBorder(isUpper),
                            modifier = Modifier.heightIn(min = 40.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    allowedClasses.forEach { c ->
                        val checked = c in selectedClasses
                        Surface(
                            color = if (checked) TealSoft.copy(alpha = 0.55f) else SlateBg.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (checked) TealPrimary.copy(alpha = 0.45f) else SlateOutline),
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedClasses = if (checked) selectedClasses - c else selectedClasses + c
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { on ->
                                        selectedClasses = if (on) selectedClasses + c else selectedClasses - c
                                    }
                                )
                                Text(
                                    "${classNameBn(c, settings)} শ্রেণি (${classShortBn(c, settings)})",
                                    fontSize = 12.5.sp,
                                    color = SlateText,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Surface(
                    color = SlateBg.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable { matchClassSubjects = !matchClassSubjects }
                        ) {
                            Column(Modifier.weight(1f).padding(end = 6.dp)) {
                                Text("শ্রেণি অনুযায়ী বিষয় অটো-ম্যাচ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SlateText)
                                Text(
                                    "বেস রুটিনে '/' বা একাধিক বিষয় থাকলে ওই শ্রেণির প্রযোজ্য বিষয় বেছে নেবে",
                                    fontSize = 10.5.sp,
                                    color = SlateMuted
                                )
                            }
                            Switch(checked = matchClassSubjects, onCheckedChange = { matchClassSubjects = it })
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable { copyExamTime = !copyExamTime }
                        ) {
                            Column(Modifier.weight(1f).padding(end = 6.dp)) {
                                Text("পরীক্ষার সময়ও কপি করুন", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SlateText)
                                Text(
                                    "বন্ধ রাখলে প্রতিটি শ্রেণির নিজস্ব শিফটের সময় অক্ষুণ্ণ থাকবে",
                                    fontSize = 10.5.sp,
                                    color = SlateMuted
                                )
                            }
                            Switch(checked = copyExamTime, onCheckedChange = { copyExamTime = it })
                        }
                    }
                }

                Text(
                    "নির্বাচিত: ${bn(selectedClasses.size.toString())}টি শ্রেণি",
                    fontSize = 11.sp,
                    color = SlateMuted
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val orderedTargets = allowedClasses.filter { it in selectedClasses }
                    if (orderedTargets.isNotEmpty()) {
                        onConfirm(orderedTargets, matchClassSubjects, copyExamTime)
                    }
                },
                enabled = selectedClasses.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.heightIn(min = 48.dp).testTag("copy_routine_confirm_btn")
            ) {
                Text("কপি করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("বাতিল")
            }
        }
    )
}

internal enum class AdmitExportAction { PDF, PRINT }

@Composable
internal fun AdmitMultiClassExportDialog(
    action: AdmitExportAction,
    allowedClasses: List<Int>,
    currentClass: Int,
    countByClass: Map<Int, Int>,
    initialSelection: Set<Int>,
    settings: Settings,
    students: List<Student>,
    config: AdmitCardConfig,
    routineProvider: (Int) -> List<ExamRoutineEntry>,
    examTimeProvider: (Int) -> String,
    onDismiss: () -> Unit,
    onConfirm: (selectedClasses: Set<Int>, pages: List<AdmitCardPageSpec>, fileNameOrJob: String) -> Unit
) {
    val nonEmptyClasses = remember(allowedClasses, countByClass, config.extraBlankCards) {
        if (config.extraBlankCards > 0) allowedClasses
        else allowedClasses.filter { (countByClass[it] ?: 0) > 0 }.ifEmpty { allowedClasses }
    }
    var selectedClasses by remember(initialSelection, allowedClasses, currentClass) {
        val valid = initialSelection.filter { it in allowedClasses }.toSet()
        mutableStateOf(if (valid.isNotEmpty()) valid else setOf(currentClass))
    }

    val lowerClasses = remember(nonEmptyClasses, settings) {
        nonEmptyClasses.filter { syllabusTierForClass(it, settings) <= 2 }
    }
    val upperClasses = remember(nonEmptyClasses, settings) {
        nonEmptyClasses.filter { syllabusTierForClass(it, settings) >= 3 }
    }

    val orderedSelection = remember(selectedClasses, allowedClasses) {
        allowedClasses.filter { it in selectedClasses }
    }
    val combinedPages = remember(orderedSelection, students, settings, config) {
        AdmitCardPlanner.buildMultiClassPages(
            students = students,
            settings = settings,
            baseConfig = config,
            selectedClasses = orderedSelection,
            routineProvider = routineProvider,
            examTimeProvider = examTimeProvider,
            allowedClassIds = allowedClasses
        )
    }
    val totalCards = remember(combinedPages) { combinedPages.sumOf { it.items.size } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (action == AdmitExportAction.PDF) Icons.Rounded.PictureAsPdf else Icons.Rounded.Print,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (action == AdmitExportAction.PDF) "PDF-এর জন্য শ্রেণি বাছুন" else "প্রিন্টের জন্য শ্রেণি বাছুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick select presets
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val allSelected = nonEmptyClasses.isNotEmpty() && nonEmptyClasses.all { it in selectedClasses }
                    FilterChip(
                        selected = allSelected,
                        onClick = { selectedClasses = nonEmptyClasses.toSet() },
                        label = { Text("সব শ্রেণি", fontSize = 11.sp) },
                        colors = appFilterChipColors(),
                        border = appFilterChipBorder(allSelected),
                        modifier = Modifier.heightIn(min = 38.dp).testTag("admit_export_all_cls")
                    )
                    val onlyCurrent = selectedClasses == setOf(currentClass)
                    FilterChip(
                        selected = onlyCurrent,
                        onClick = { selectedClasses = setOf(currentClass) },
                        label = { Text("শুধু ${classShortBn(currentClass, settings)}", fontSize = 11.sp) },
                        colors = appFilterChipColors(),
                        border = appFilterChipBorder(onlyCurrent),
                        modifier = Modifier.heightIn(min = 38.dp).testTag("admit_export_current_cls")
                    )
                    if (lowerClasses.isNotEmpty() && lowerClasses.size < nonEmptyClasses.size) {
                        val isLower = selectedClasses == lowerClasses.toSet()
                        FilterChip(
                            selected = isLower,
                            onClick = { selectedClasses = lowerClasses.toSet() },
                            label = { Text("প্রাক–২য়", fontSize = 11.sp) },
                            colors = appFilterChipColors(),
                            border = appFilterChipBorder(isLower),
                            modifier = Modifier.heightIn(min = 38.dp)
                        )
                    }
                    if (upperClasses.isNotEmpty() && upperClasses.size < nonEmptyClasses.size) {
                        val isUpper = selectedClasses == upperClasses.toSet()
                        FilterChip(
                            selected = isUpper,
                            onClick = { selectedClasses = upperClasses.toSet() },
                            label = { Text("৩য়–৫ম", fontSize = 11.sp) },
                            colors = appFilterChipColors(),
                            border = appFilterChipBorder(isUpper),
                            modifier = Modifier.heightIn(min = 38.dp)
                        )
                    }
                }

                // Individual class toggles with student count
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    allowedClasses.forEach { c ->
                        val checked = c in selectedClasses
                        val cnt = countByClass[c] ?: 0
                        Surface(
                            color = if (checked) TealSoft.copy(alpha = 0.55f) else SlateBg.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (checked) TealPrimary.copy(alpha = 0.5f) else SlateOutline),
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedClasses = if (checked) selectedClasses - c else selectedClasses + c
                                }
                                .testTag("admit_export_cls_$c")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { on ->
                                        selectedClasses = if (on) selectedClasses + c else selectedClasses - c
                                    }
                                )
                                Text(
                                    text = "${classNameBn(c, settings)} শ্রেণি",
                                    fontSize = 12.5.sp,
                                    fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
                                    color = SlateText,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${bn(cnt.toString())} জন",
                                    fontSize = 11.sp,
                                    color = SlateMuted,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }
                        }
                    }
                }

                Surface(
                    color = SlateBg,
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "মোট: ${bn(orderedSelection.size.toString())}টি শ্রেণি · ${bn(totalCards.toString())} কার্ড · ${bn(combinedPages.size.toString())} পৃষ্ঠা",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (combinedPages.isNotEmpty()) TealPrimary else SlateMuted
                        )
                        Text(
                            text = "প্রতিটি শ্রেণির নিজস্ব সংরক্ষিত রুটিন ও সময় ব্যবহার হবে",
                            fontSize = 10.5.sp,
                            color = SlateMuted
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val label = when {
                        orderedSelection.size == 1 -> "প্রবেশপত্র-${classShortBn(orderedSelection.first(), settings)}-শ্রেণি.pdf"
                        orderedSelection.size == nonEmptyClasses.size && orderedSelection.size > 1 -> "প্রবেশপত্র-সকল-শ্রেণি.pdf"
                        else -> "প্রবেশপত্র-${bn(orderedSelection.size.toString())}-শ্রেণি.pdf"
                    }
                    val finalArg = if (action == AdmitExportAction.PDF) label else label.removeSuffix(".pdf")
                    onConfirm(selectedClasses, combinedPages, finalArg)
                },
                enabled = combinedPages.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.heightIn(min = 48.dp).testTag("admit_export_confirm_btn")
            ) {
                Icon(
                    imageVector = if (action == AdmitExportAction.PDF) Icons.Rounded.PictureAsPdf else Icons.Rounded.Print,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    if (action == AdmitExportAction.PDF) {
                        "PDF তৈরি (${bn(combinedPages.size.toString())} পৃষ্ঠা)"
                    } else {
                        "প্রিন্ট (${bn(combinedPages.size.toString())} পৃষ্ঠা)"
                    }
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("বাতিল")
            }
        }
    )
}
