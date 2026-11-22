package com.kgps.result.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.Student
import com.kgps.result.bn
import com.kgps.result.classNameBn
import com.kgps.result.fmt
import com.kgps.result.ui.theme.AmberBg
import com.kgps.result.ui.theme.AmberBorder
import com.kgps.result.ui.theme.AmberNotice
import com.kgps.result.ui.theme.EmeraldGrade
import com.kgps.result.ui.theme.RoseBg
import com.kgps.result.ui.theme.RoseDanger
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

@Composable
fun ProgressTrackerCard(
    summary: DashboardSummary,
    showClassBreakdown: Boolean,
    onSelectClassFilter: (Int) -> Unit
) {
    val total = summary.totalStudents.coerceAtLeast(1)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SlateOutline),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(color = TealSoft, shape = RoundedCornerShape(8.dp)) {
                    Icon(
                        Icons.Rounded.Assessment,
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.padding(5.dp).size(16.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "মূল্যায়ন ও উপস্থিতির অগ্রগতি",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = SlateText
                    )
                    Text(
                        "প্রান্তিক নম্বর, উপস্থিতি ও গুণাবলি পূরণের হার",
                        fontSize = 10.5.sp,
                        color = SlateMuted
                    )
                }
            }

            if (summary.totalStudents == 0) {
                Text(
                    "এখনো কোনো শিক্ষার্থী যোগ করা হয়নি।",
                    fontSize = 12.sp,
                    color = SlateMuted
                )
            } else {
                ProgressRowItem(
                    label = "১ম প্রান্তিক মূল্যায়ন",
                    done = summary.term1Count,
                    total = summary.totalStudents,
                    barColor = TealPrimary
                )
                ProgressRowItem(
                    label = "২য় প্রান্তিক মূল্যায়ন",
                    done = summary.term2Count,
                    total = summary.totalStudents,
                    barColor = TealPrimary
                )
                ProgressRowItem(
                    label = "৩য় প্রান্তিক মূল্যায়ন",
                    done = summary.term3Count,
                    total = summary.totalStudents,
                    barColor = EmeraldGrade
                )
                ProgressRowItem(
                    label = buildString {
                        append("উপস্থিতি এন্ট্রি")
                        summary.avgAttendancePct?.let { append(" (গড় উপস্থিতি ${fmt(it)}%)") }
                    },
                    done = summary.attendanceFilledCount,
                    total = summary.totalStudents,
                    barColor = AmberNotice
                )
                ProgressRowItem(
                    label = "গুণাবলি (সক্রিয়তা, শৃঙ্খলা, আচরণ)",
                    done = summary.traitsFilledCount,
                    total = total,
                    barColor = Color(0xFF0284C7)
                )
            }

            if (showClassBreakdown && summary.classBreakdown.size > 1) {
                HorizontalDivider(color = SlateOutline)
                Text(
                    "শ্রেণিভিত্তিক সংক্ষেপ (ট্যাপ করে ফিল্টার করুন)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = SlateMuted
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    summary.classBreakdown.forEach { item ->
                        val pct = if (item.totalStudents > 0) {
                            (item.evaluatedCount * 100.0 / item.totalStudents)
                        } else 0.0
                        Surface(
                            onClick = { onSelectClassFilter(item.cls) },
                            color = SlateBg,
                            border = BorderStroke(1.dp, SlateOutline),
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier.testTag("dash_cls_breakdown_${item.cls}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Text(
                                    "${classNameBn(item.cls)} (${bn(item.totalStudents.toString())})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = SlateText
                                )
                                Text(
                                    "মূল্যায়িত ${bn(item.evaluatedCount.toString())} (${fmt(pct)}%)",
                                    fontSize = 10.sp,
                                    color = TealPrimary
                                )
                                if (item.evaluatedCount > 0) {
                                    Text(
                                        "গড় ${fmt(item.avgPct)}%",
                                        fontSize = 10.sp,
                                        color = SlateMuted
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
private fun ProgressRowItem(
    label: String,
    done: Int,
    total: Int,
    barColor: Color
) {
    val ratio = if (total > 0) (done.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    val pct = (ratio * 100f)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = SlateText,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${bn(done.toString())}/${bn(total.toString())} (${fmt(pct.toDouble())}%)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = barColor
            )
        }
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = barColor,
            trackColor = SlateSurfaceVariant,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun MeritAndResultsCard(
    summary: DashboardSummary,
    termLabel: String,
    canPreviewReport: Boolean,
    showSubjectRestrictionNote: Boolean,
    onOpenStudent: (Student) -> Unit,
    onPreviewStudent: (Student) -> Unit
) {
    var showAllMerit by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SlateOutline),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(color = AmberBg, shape = RoundedCornerShape(8.dp)) {
                    Icon(
                        Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = AmberNotice,
                        modifier = Modifier.padding(5.dp).size(16.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "ফলাফল ও মেধাতালিকা ($termLabel)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = SlateText
                    )
                    Text(
                        if (summary.evaluatedInScope > 0) {
                            "মূল্যায়িত ${bn(summary.evaluatedInScope.toString())} জন · গড় নম্বর ${fmt(summary.avgScorePct)}%"
                        } else {
                            "এই পরিসরে এখনো নম্বর এন্ট্রি হয়নি"
                        },
                        fontSize = 10.5.sp,
                        color = SlateMuted
                    )
                }
            }

            if (showSubjectRestrictionNote) {
                Surface(
                    color = TealSoft,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, TealContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "শুধুমাত্র আপনার অনুমোদিত বিষয়গুলোর ভিত্তিতে হিসাব দেখানো হচ্ছে।",
                        fontSize = 10.5.sp,
                        color = TealDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            if (summary.evaluatedInScope > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    GradeStatPill("A", "অতি উত্তম", summary.gradeCounts["A"] ?: 0, EmeraldGrade, Modifier.weight(1f))
                    GradeStatPill("B", "উত্তম", summary.gradeCounts["B"] ?: 0, TealPrimary, Modifier.weight(1f))
                    GradeStatPill("C", "সন্তোষজনক", summary.gradeCounts["C"] ?: 0, AmberNotice, Modifier.weight(1f))
                    GradeStatPill("D", "সহায়তা", summary.gradeCounts["D"] ?: 0, RoseDanger, Modifier.weight(1f))
                }

                HorizontalDivider(color = SlateOutline)

                val visibleMerit = if (showAllMerit) summary.meritList else summary.meritList.take(5)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    visibleMerit.forEach { entry ->
                        val rankBg = when (entry.rank) {
                            1 -> AmberBg
                            2 -> TealSoft
                            3 -> Color(0xFFE0F2FE)
                            else -> SlateSurfaceVariant
                        }
                        val rankFg = when (entry.rank) {
                            1 -> AmberNotice
                            2 -> TealPrimary
                            3 -> Color(0xFF0369A1)
                            else -> SlateText
                        }
                        val gradeColor = when (entry.grade) {
                            "A" -> EmeraldGrade
                            "B" -> TealPrimary
                            "C" -> AmberNotice
                            else -> RoseDanger
                        }
                        Surface(
                            onClick = { onOpenStudent(entry.student) },
                            color = SlateBg,
                            border = BorderStroke(1.dp, SlateOutline),
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier.fillMaxWidth().testTag("dash_merit_row_${entry.student.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = rankBg,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, rankFg.copy(alpha = 0.25f))
                                ) {
                                    Text(
                                        bn(entry.rank.toString()),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        color = rankFg,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        entry.student.name.ifBlank { "(নাম নেই)" },
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.5.sp,
                                        color = SlateText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${classShortLabel(entry.student.cls)} · রোল ${bn(entry.student.roll.toString().padStart(2, '0'))} · ${fmt(entry.obtained)}/${fmt(entry.maxMarks)}",
                                        fontSize = 10.5.sp,
                                        color = SlateMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    color = gradeColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "${entry.grade} · ${fmt(entry.pct)}%",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = gradeColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                                if (canPreviewReport) {
                                    IconButton(
                                        onClick = { onPreviewStudent(entry.student) },
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

                if (summary.meritList.size > 5) {
                    TextButton(
                        onClick = { showAllMerit = !showAllMerit },
                        modifier = Modifier.align(Alignment.End),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            if (showAllMerit) "সংক্ষিপ্ত করুন" else "সম্পূর্ণ মেধাতালিকা (${bn(summary.meritList.size.toString())} জন)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GradeStatPill(
    gradeLetter: String,
    caption: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f)),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                "$gradeLetter · ${bn(count.toString())}",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = color,
                maxLines = 1
            )
            Text(
                caption,
                fontSize = 9.5.sp,
                color = SlateText,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun IncompleteStudentsCard(
    gapItems: List<GapStudentItem>,
    canOpenSheet: Boolean,
    onOpenStudent: (Student) -> Unit,
    onOpenBatchSheet: (Int) -> Unit
) {
    var activeFilter by remember { mutableStateOf(GapFilter.ALL) }
    var showAll by remember { mutableStateOf(false) }

    val filtered = remember(gapItems, activeFilter) {
        when (activeFilter) {
            GapFilter.ALL -> gapItems
            GapFilter.MARKS -> gapItems.filter { it.missingMarks }
            GapFilter.ATTENDANCE -> gapItems.filter { it.missingAttendance }
            GapFilter.INFO -> gapItems.filter { it.missingInfo }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (gapItems.isNotEmpty()) AmberBorder else SlateOutline),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = if (gapItems.isNotEmpty()) AmberBg else TealSoft,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        if (gapItems.isNotEmpty()) Icons.Rounded.WarningAmber else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = if (gapItems.isNotEmpty()) AmberNotice else EmeraldGrade,
                        modifier = Modifier.padding(5.dp).size(16.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "অপূর্ণ নম্বর ও ঘাটতি তালিকা",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = SlateText
                    )
                    Text(
                        if (gapItems.isEmpty()) "নির্বাচিত পরিসরে কোনো ঘাটতি নেই"
                        else "${bn(gapItems.size.toString())} জন শিক্ষার্থীর তথ্য বা নম্বর অসম্পূর্ণ",
                        fontSize = 10.5.sp,
                        color = if (gapItems.isEmpty()) EmeraldGrade else AmberNotice
                    )
                }
                if (gapItems.isNotEmpty() && canOpenSheet) {
                    Surface(
                        onClick = { onOpenBatchSheet(gapItems.first().student.cls) },
                        color = TealSoft,
                        border = BorderStroke(1.dp, TealContainer),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("dash_gaps_open_sheet_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.GridOn, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(13.dp))
                            Text("ব্যাচ এন্ট্রি", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = TealPrimary)
                        }
                    }
                }
            }

            if (gapItems.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    GapFilter.entries.forEach { f ->
                        val count = when (f) {
                            GapFilter.ALL -> gapItems.size
                            GapFilter.MARKS -> gapItems.count { it.missingMarks }
                            GapFilter.ATTENDANCE -> gapItems.count { it.missingAttendance }
                            GapFilter.INFO -> gapItems.count { it.missingInfo }
                        }
                        val sel = activeFilter == f
                        FilterChip(
                            selected = sel,
                            onClick = { activeFilter = f },
                            colors = appFilterChipColors(),
                            border = appFilterChipBorder(sel),
                            modifier = Modifier.height(32.dp),
                            label = { Text("${f.labelBn} (${bn(count.toString())})", fontSize = 10.5.sp) }
                        )
                    }
                }

                if (filtered.isEmpty()) {
                    Text(
                        "এই ক্যাটাগরিতে কোনো ঘাটতি নেই।",
                        fontSize = 11.5.sp,
                        color = SlateMuted
                    )
                } else {
                    val shown = if (showAll) filtered else filtered.take(6)
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        shown.forEach { item ->
                            Surface(
                                onClick = { onOpenStudent(item.student) },
                                color = SlateBg,
                                border = BorderStroke(1.dp, SlateOutline),
                                shape = RoundedCornerShape(9.dp),
                                modifier = Modifier.fillMaxWidth().testTag("dash_gap_row_${item.student.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = if (item.missingMarks) RoseBg else AmberBg,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            "${classShortLabel(item.student.cls)} · ${bn(item.student.roll.toString().padStart(2, '0'))}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp,
                                            color = if (item.missingMarks) RoseDanger else AmberNotice,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            item.student.name.ifBlank { "(নাম লেখা হয়নি)" },
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.5.sp,
                                            color = SlateText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            item.tags.forEach { tag ->
                                                val isUrgent = tag.contains("নম্বর") || tag.contains("নাম")
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            if (isUrgent) RoseBg else AmberBg,
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        tag,
                                                        fontSize = 9.5.sp,
                                                        color = if (isUrgent) RoseDanger else AmberNotice,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Icon(
                                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = "সম্পাদনা করুন",
                                        tint = SlateMuted,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (filtered.size > 6) {
                        TextButton(
                            onClick = { showAll = !showAll },
                            modifier = Modifier.align(Alignment.End),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                if (showAll) "কম দেখুন" else "আরও ${bn((filtered.size - 6).toString())} জন দেখুন",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
