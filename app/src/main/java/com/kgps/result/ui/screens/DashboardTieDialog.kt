package com.kgps.result.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.bn
import com.kgps.result.classNameBn
import com.kgps.result.fmt
import com.kgps.result.ui.theme.AmberBg
import com.kgps.result.ui.theme.AmberBorder
import com.kgps.result.ui.theme.AmberNotice
import com.kgps.result.ui.theme.RoseDanger
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealContainer
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft

@Composable
fun TotalMarksTieDetailsDialog(
    tieGroups: List<ClassTotalTieGroup>,
    termLabel: String,
    settings: Settings,
    canPreviewReport: Boolean,
    onOpenStudent: (Student) -> Unit,
    onPreviewStudent: (Student) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, SlateOutline),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .heightIn(max = 620.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(color = AmberBg, shape = RoundedCornerShape(9.dp)) {
                        Icon(
                            Icons.Rounded.CompareArrows,
                            contentDescription = null,
                            tint = AmberNotice,
                            modifier = Modifier.padding(6.dp).size(18.dp)
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "মোট নম্বরে সমতা (Tie) বিস্তারিত",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = SlateText
                        )
                        val totalStudents = tieGroups.sumOf { it.students.size }
                        Text(
                            if (tieGroups.isEmpty()) "$termLabel · কোনো Tie নেই"
                            else "$termLabel · ${bn(tieGroups.size.toString())}টি ক্ষেত্রে ${bn(totalStudents.toString())} জন শিক্ষার্থী",
                            fontSize = 11.sp,
                            color = SlateMuted
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp).testTag("tie_dialog_close_btn")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "বন্ধ করুন", tint = SlateMuted)
                    }
                }

                HorizontalDivider(color = SlateOutline)

                if (tieGroups.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "নির্বাচিত শ্রেণি ও প্রান্তিকে কোনো শিক্ষার্থীর মোট নম্বর এক হয়নি।",
                            fontSize = 12.5.sp,
                            color = SlateMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        tieGroups.forEach { group ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateBg),
                                border = BorderStroke(1.dp, AmberBorder),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = TealSoft,
                                            border = BorderStroke(1.dp, TealContainer),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                "${classNameBn(group.cls, settings)} শ্রেণি",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TealDark,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                        }
                                        Surface(
                                            color = AmberBg,
                                            border = BorderStroke(1.dp, AmberBorder),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                "মোট ${fmt(group.obtained)}/${fmt(group.maxMarks)} (${fmt(group.pct)}%)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AmberNotice,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                        }
                                        Spacer(Modifier.weight(1f))
                                        Text(
                                            "${bn(group.students.size.toString())} জন",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SlateText
                                        )
                                    }

                                    group.students.forEach { detail ->
                                        val rankText = detail.classRank?.let { r ->
                                            if (detail.isJointRank) "মেধা ${bn(r.toString())} (যৌথ)" else "মেধা ${bn(r.toString())}"
                                        } ?: "রোল ${bn(detail.student.roll.toString().padStart(2, '0'))}"
                                        Surface(
                                            onClick = {
                                                onDismiss()
                                                onOpenStudent(detail.student)
                                            },
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, SlateOutline),
                                            shape = RoundedCornerShape(9.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .defaultMinSize(minHeight = 48.dp)
                                                .testTag("tie_student_row_${detail.student.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                                            ) {
                                                Surface(
                                                    color = if (detail.isJointRank) AmberBg else TealSoft,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        rankText,
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (detail.isJointRank) AmberNotice else TealDark,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                                    )
                                                }
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                                ) {
                                                    Text(
                                                        detail.student.name.ifBlank { "(নাম নেই)" },
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 12.5.sp,
                                                        color = SlateText,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    val meta = buildString {
                                                        append("রোল ${bn(detail.student.roll.toString().padStart(2, '0'))} · গ্রেড ${detail.grade}")
                                                        detail.attendancePct?.let { append(" · উপস্থিতি ${fmt(it)}%") }
                                                        if (detail.traitScore > 0.0) append(" · গুণাবলি ${fmt(detail.traitScore)}")
                                                        if (!detail.isPromoted) append(" · অনুত্তীর্ণ")
                                                    }
                                                    Text(
                                                        meta,
                                                        fontSize = 10.sp,
                                                        color = if (!detail.isPromoted) RoseDanger else SlateMuted,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                if (canPreviewReport) {
                                                    IconButton(
                                                        onClick = {
                                                            onDismiss()
                                                            onPreviewStudent(detail.student)
                                                        },
                                                        modifier = Modifier.size(48.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Rounded.Visibility,
                                                            contentDescription = "রিপোর্ট দেখুন",
                                                            tint = TealPrimary,
                                                            modifier = Modifier.size(18.dp)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text("বন্ধ করুন", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
