package com.kgps.result.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.ExamRoutineEntry
import com.kgps.result.Settings
import com.kgps.result.ai.ExamRoutineNoticeExtractor
import com.kgps.result.ai.ExtractedClassRoutineGroup
import com.kgps.result.ai.ExtractedExamRoutineNotice
import com.kgps.result.bn
import com.kgps.result.classShortBn
import com.kgps.result.termNameBn
import com.kgps.result.ui.theme.AmberBg
import com.kgps.result.ui.theme.AmberBorder
import com.kgps.result.ui.theme.AmberNotice
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft
import java.util.UUID

data class AiRoutineApplySelection(
    val classPlans: Map<Int, Pair<String, List<ExamRoutineEntry>>>,
    val selectedGroupsForBase: List<ExtractedClassRoutineGroup>,
    val applyTerm: Int?,
    val applyFootnote: String?,
    val saveAsBaseRoutines: Boolean
)

@Composable
internal fun AdmitAiRoutinePreviewDialog(
    notice: ExtractedExamRoutineNotice,
    settings: Settings,
    onDismiss: () -> Unit,
    onApply: (AiRoutineApplySelection) -> Unit
) {
    val allDetectedClasses = remember(notice) {
        notice.groups.flatMap { it.classes }.distinct().sorted()
    }
    var selectedClasses by remember(notice) { mutableStateOf(allDetectedClasses.toSet()) }
    var canonicalizeSubjects by remember(notice) { mutableStateOf(true) }
    var applyTerm by remember(notice) { mutableStateOf(notice.detectedTerm != null) }
    var applyFootnote by remember(notice) { mutableStateOf(notice.footnote.isNotBlank()) }
    var footnoteText by remember(notice) { mutableStateOf(notice.footnote) }
    var saveAsBaseRoutines by remember(notice) { mutableStateOf(false) }

    val resolvedByClass = remember(notice, canonicalizeSubjects, settings) {
        buildMap<Int, Pair<String, List<ExamRoutineEntry>>> {
            notice.groups.forEach { grp ->
                grp.classes.forEach { cls ->
                    val rows = grp.rows.map { row ->
                        val nextSubjects = if (canonicalizeSubjects) {
                            row.nonEmptySubjects.map { sub ->
                                ExamRoutineNoticeExtractor.canonicalizeSubjectForClass(sub, cls, settings)
                            }
                        } else {
                            row.nonEmptySubjects
                        }
                        row.copy(
                            id = UUID.randomUUID().toString(),
                            subjects = nextSubjects.ifEmpty { row.subjects }
                        )
                    }
                    put(cls, grp.examTime to rows)
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(19.dp)
                )
                Column {
                    Text(
                        "AI নোটিশ রুটিন প্রিভিউ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateText
                    )
                    if (notice.usedModel.isNotBlank()) {
                        Text(
                            "মডেল: ${notice.usedModel} · ফরম্যাট: ${notice.layoutType}",
                            fontSize = 9.5.sp,
                            color = SlateMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                if (notice.examTitle.isNotBlank()) {
                    Surface(
                        color = TealSoft,
                        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = notice.examTitle,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TealDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }

                if (notice.warnings.isNotEmpty()) {
                    Surface(
                        color = AmberBg,
                        border = BorderStroke(1.dp, AmberBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(7.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            notice.warnings.forEach { w ->
                                Text("• $w", fontSize = 9.5.sp, color = AmberNotice)
                            }
                        }
                    }
                }

                // Toggles
                AdmitCompactSwitchRow(
                    label = "বিষয়ের নাম অ্যাপের মূল নামের সাথে মিলাও",
                    checked = canonicalizeSubjects,
                    onCheckedChange = { canonicalizeSubjects = it },
                    testTag = "admit_ai_canonical_toggle"
                )
                if (notice.detectedTerm != null) {
                    AdmitCompactSwitchRow(
                        label = "প্রান্তিক আপডেট করো (${termNameBn(notice.detectedTerm)})",
                        checked = applyTerm,
                        onCheckedChange = { applyTerm = it },
                        testTag = "admit_ai_term_toggle"
                    )
                }
                AdmitCompactSwitchRow(
                    label = "বেস রুটিন টেমপ্লেট হিসেবেও সেভ করো",
                    checked = saveAsBaseRoutines,
                    onCheckedChange = { saveAsBaseRoutines = it },
                    testTag = "admit_ai_save_base_toggle"
                )

                if (notice.footnote.isNotBlank() || footnoteText.isNotBlank()) {
                    Surface(
                        color = SlateBg,
                        border = BorderStroke(1.dp, SlateOutline),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(7.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AdmitCompactSwitchRow(
                                label = "রুটিনের নিচে বিঃদ্রঃ নোটস বসাও",
                                checked = applyFootnote,
                                onCheckedChange = { applyFootnote = it },
                                testTag = "admit_ai_footnote_toggle"
                            )
                            if (applyFootnote) {
                                AdmitCompactTextInput(
                                    label = "নোটস টেক্সট",
                                    value = footnoteText,
                                    onValueChange = { footnoteText = it },
                                    placeholder = "মৌখিক/ধারাবাহিক মূল্যায়ন নোটস"
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = SlateOutline.copy(alpha = 0.5f))

                Text(
                    "প্রাপ্ত শ্রেণিভিত্তিক রুটিন (${bn(selectedClasses.size.toString())}/${bn(allDetectedClasses.size.toString())} নির্বাচিত):",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateText
                )

                allDetectedClasses.forEach { cls ->
                    val pair = resolvedByClass[cls] ?: return@forEach
                    val (examTime, rows) = pair
                    val checked = cls in selectedClasses

                    Surface(
                        color = if (checked) MaterialTheme.colorScheme.surface else SlateBg,
                        border = BorderStroke(1.dp, if (checked) TealPrimary.copy(alpha = 0.5f) else SlateOutline),
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedClasses = if (checked) selectedClasses - cls else selectedClasses + cls
                            }
                            .testTag("admit_ai_cls_card_$cls")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { on ->
                                        selectedClasses = if (on) selectedClasses + cls else selectedClasses - cls
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = TealPrimary),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "${classShortBn(cls, settings)} শ্রেণি",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (checked) TealDark else SlateMuted,
                                    modifier = Modifier.weight(1f)
                                )
                                if (examTime.isNotBlank()) {
                                    Surface(
                                        color = TealSoft,
                                        shape = RoundedCornerShape(5.dp)
                                    ) {
                                        Text(
                                            text = examTime,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TealDark,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            rows.forEach { r ->
                                Text(
                                    text = "${r.date.ifBlank { "—" }} (${r.day.ifBlank { "—" }}) → ${r.subject}",
                                    fontSize = 9.8.sp,
                                    color = SlateText,
                                    modifier = Modifier.padding(start = 26.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val filteredPlans = resolvedByClass.filterKeys { it in selectedClasses }
                    val selectedGroups = notice.groups.mapNotNull { grp ->
                        val keptClasses = grp.classes.filter { it in selectedClasses }
                        if (keptClasses.isEmpty()) {
                            null
                        } else {
                            val sampleCls = keptClasses.first()
                            val sampleRows = filteredPlans[sampleCls]?.second ?: grp.rows
                            grp.copy(classes = keptClasses, rows = sampleRows)
                        }
                    }
                    onApply(
                        AiRoutineApplySelection(
                            classPlans = filteredPlans,
                            selectedGroupsForBase = selectedGroups,
                            applyTerm = if (applyTerm) notice.detectedTerm else null,
                            applyFootnote = if (applyFootnote && footnoteText.isNotBlank()) footnoteText.trim() else null,
                            saveAsBaseRoutines = saveAsBaseRoutines
                        )
                    )
                },
                enabled = selectedClasses.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.heightIn(min = 36.dp).testTag("admit_ai_apply_btn")
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("রুটিনে বসান", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", fontSize = 11.5.sp, color = SlateMuted)
            }
        }
    )
}
