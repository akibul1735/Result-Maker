package com.kgps.result.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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

enum class DashboardAnalyticsTab(val labelBn: String, val testTag: String) {
    MERIT("মেধাতালিকা", "dash_tab_merit"),
    PROGRESS("অগ্রগতি", "dash_tab_progress"),
    GAPS("ঘাটতি", "dash_tab_gaps")
}

@Composable
fun DashboardAnalyticsCard(
    summary: DashboardSummary,
    termLabel: String,
    showClassBreakdown: Boolean,
    canPreviewReport: Boolean,
    canOpenSheet: Boolean,
    showSubjectRestrictionNote: Boolean,
    activeTab: DashboardAnalyticsTab,
    onSelectTab: (DashboardAnalyticsTab) -> Unit,
    onSelectClassFilter: (Int) -> Unit,
    onOpenStudent: (Student) -> Unit,
    onPreviewStudent: (Student) -> Unit,
    onOpenBatchSheet: (Int) -> Unit,
    onOpenTieDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SlateOutline.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnalyticsTabBar(
                activeTab = activeTab,
                gapCount = summary.gapItems.size,
                meritCount = summary.meritList.size,
                onSelectTab = onSelectTab
            )

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                },
                label = "dashboard_analytics_tab"
            ) { tab ->
                when (tab) {
                    DashboardAnalyticsTab.MERIT -> MeritTabBody(
                        summary = summary,
                        termLabel = termLabel,
                        canPreviewReport = canPreviewReport,
                        showSubjectRestrictionNote = showSubjectRestrictionNote,
                        onOpenStudent = onOpenStudent,
                        onPreviewStudent = onPreviewStudent,
                        onOpenTieDetails = onOpenTieDetails
                    )

                    DashboardAnalyticsTab.PROGRESS -> ProgressTabBody(
                        summary = summary,
                        showClassBreakdown = showClassBreakdown,
                        onSelectClassFilter = onSelectClassFilter
                    )

                    DashboardAnalyticsTab.GAPS -> GapsTabBody(
                        gapItems = summary.gapItems,
                        canOpenSheet = canOpenSheet,
                        onOpenStudent = onOpenStudent,
                        onOpenBatchSheet = onOpenBatchSheet
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsTabBar(
    activeTab: DashboardAnalyticsTab,
    gapCount: Int,
    meritCount: Int,
    onSelectTab: (DashboardAnalyticsTab) -> Unit
) {
    Surface(
        color = SlateSurfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DashboardAnalyticsTab.entries.forEach { tab ->
                val selected = activeTab == tab
                val badgeText = when (tab) {
                    DashboardAnalyticsTab.MERIT -> if (meritCount > 0) bn(meritCount.toString()) else null
                    DashboardAnalyticsTab.PROGRESS -> null
                    DashboardAnalyticsTab.GAPS -> bn(gapCount.toString())
                }
                val isGapAlert = tab == DashboardAnalyticsTab.GAPS && gapCount > 0

                Surface(
                    onClick = { onSelectTab(tab) },
                    color = if (selected) Color.White else Color.Transparent,
                    shadowElevation = if (selected) 1.dp else 0.dp,
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 40.dp)
                        .testTag(tab.testTag)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tab.labelBn,
                            fontSize = 11.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) TealDark else SlateMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (badgeText != null) {
                            Spacer(Modifier.size(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isGapAlert && selected -> AmberBg
                                            isGapAlert -> AmberNotice.copy(alpha = 0.14f)
                                            selected -> TealSoft
                                            else -> SlateOutline.copy(alpha = 0.7f)
                                        }
                                    )
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isGapAlert -> AmberNotice
                                        selected -> TealPrimary
                                        else -> SlateMuted
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

@Composable
private fun MeritTabBody(
    summary: DashboardSummary,
    termLabel: String,
    canPreviewReport: Boolean,
    showSubjectRestrictionNote: Boolean,
    onOpenStudent: (Student) -> Unit,
    onPreviewStudent: (Student) -> Unit,
    onOpenTieDetails: () -> Unit
) {
    var showAllMerit by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "ফলাফল সারসংক্ষেপ · $termLabel",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = SlateText
                )
                Text(
                    text = if (summary.evaluatedInScope > 0) {
                        "মূল্যায়িত ${bn(summary.evaluatedInScope.toString())} জন · গড় ${fmt(summary.avgScorePct)}%"
                    } else {
                        "এই পরিসরে এখনো নম্বর এন্ট্রি হয়নি"
                    },
                    fontSize = 11.sp,
                    color = SlateMuted
                )
            }

            if (summary.evaluatedInScope > 0) {
                val hasTies = summary.tieGroups.isNotEmpty()
                Surface(
                    onClick = onOpenTieDetails,
                    color = if (hasTies) AmberBg else SlateBg,
                    border = BorderStroke(1.dp, if (hasTies) AmberBorder else SlateOutline),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("dash_merit_tie_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.CompareArrows,
                            contentDescription = "সমতা বিস্তারিত",
                            tint = if (hasTies) AmberNotice else SlateMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (hasTies) "Tie · ${bn(summary.tieGroups.size.toString())}" else "Tie নেই",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasTies) AmberNotice else SlateMuted
                        )
                    }
                }
            }
        }

        if (showSubjectRestrictionNote) {
            Surface(
                color = TealSoft,
                shape = RoundedCornerShape(9.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "শুধুমাত্র আপনার অনুমোদিত বিষয়গুলোর ভিত্তিতে হিসাব দেখানো হচ্ছে।",
                    fontSize = 10.5.sp,
                    color = TealDark,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        if (summary.evaluatedInScope == 0) {
            AnalyticsEmptyState(
                icon = Icons.Rounded.EmojiEvents,
                title = "মেধাতালিকা এখনো তৈরি হয়নি",
                subtitle = "শিক্ষার্থীদের নম্বর এন্ট্রি সম্পন্ন হলে এখানে গ্রেড ও মেধাক্রম দেখা যাবে।"
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                GradeStatPill("A", "অতি উত্তম", summary.gradeCounts["A"] ?: 0, EmeraldGrade, Modifier.weight(1f))
                GradeStatPill("B", "উত্তম", summary.gradeCounts["B"] ?: 0, TealPrimary, Modifier.weight(1f))
                GradeStatPill("C", "সন্তোষজনক", summary.gradeCounts["C"] ?: 0, AmberNotice, Modifier.weight(1f))
                GradeStatPill("D", "সহায়তা", summary.gradeCounts["D"] ?: 0, RoseDanger, Modifier.weight(1f))
            }

            HorizontalDivider(color = SlateOutline.copy(alpha = 0.65f))

            val visibleMerit = if (showAllMerit) summary.meritList else summary.meritList.take(5)
            if (visibleMerit.isEmpty()) {
                Text(
                    "মেধাক্রম প্রদর্শনের মতো উত্তীর্ণ শিক্ষার্থী পাওয়া যায়নি।",
                    fontSize = 11.5.sp,
                    color = SlateMuted,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    visibleMerit.forEach { entry ->
                        MeritStudentRow(
                            entry = entry,
                            canPreviewReport = canPreviewReport,
                            onOpenStudent = onOpenStudent,
                            onPreviewStudent = onPreviewStudent
                        )
                    }
                }
            }

            if (summary.meritList.size > 5) {
                TextButton(
                    onClick = { showAllMerit = !showAllMerit },
                    modifier = Modifier
                        .align(Alignment.End)
                        .defaultMinSize(minHeight = 40.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (showAllMerit) "সংক্ষিপ্ত করুন" else "সব দেখুন (${bn(summary.meritList.size.toString())} জন)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TealPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun MeritStudentRow(
    entry: MeritEntry,
    canPreviewReport: Boolean,
    onOpenStudent: (Student) -> Unit,
    onPreviewStudent: (Student) -> Unit
) {
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
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .testTag("dash_merit_row_${entry.student.id}")
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = if (canPreviewReport) 2.dp else 8.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = rankBg,
                shape = RoundedCornerShape(7.dp)
            ) {
                Text(
                    text = if (entry.isJointRank) "${bn(entry.rank.toString())} (যৌথ)" else "#${bn(entry.rank.toString())}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = rankFg,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
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
                val subMeta = buildString {
                    append("${classShortLabel(entry.student.cls)} · রোল ${bn(entry.student.roll.toString().padStart(2, '0'))} · ${fmt(entry.obtained)}/${fmt(entry.maxMarks)}")
                    if (entry.isSpecialNeeds) append(" · বিশেষ")
                    if (!entry.isPromoted) append(" · অনুত্তীর্ণ")
                }
                Text(
                    subMeta,
                    fontSize = 10.sp,
                    color = if (!entry.isPromoted) RoseDanger else SlateMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                color = gradeColor.copy(alpha = 0.11f),
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

@Composable
private fun GradeStatPill(
    gradeLetter: String,
    caption: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                "$gradeLetter · ${bn(count.toString())}",
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                color = color,
                maxLines = 1
            )
            Text(
                caption,
                fontSize = 9.5.sp,
                color = SlateMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProgressTabBody(
    summary: DashboardSummary,
    showClassBreakdown: Boolean,
    onSelectClassFilter: (Int) -> Unit
) {
    val total = summary.totalStudents.coerceAtLeast(1)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column {
            Text(
                "মূল্যায়ন ও তথ্য পূরণের হার",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = SlateText
            )
            Text(
                "প্রান্তিক নম্বর, উপস্থিতি ও গুণাবলি অগ্রগতি",
                fontSize = 11.sp,
                color = SlateMuted
            )
        }

        if (summary.totalStudents == 0) {
            AnalyticsEmptyState(
                icon = Icons.Rounded.Assessment,
                title = "কোনো শিক্ষার্থী পাওয়া যায়নি",
                subtitle = "শিক্ষার্থী যোগ করলে প্রতিটি প্রান্তিকের অগ্রগতি এখানে দেখা যাবে।"
            )
        } else {
            Surface(
                color = SlateBg,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    ProgressRowItem("১ম প্রান্তিক মূল্যায়ন", summary.term1Count, summary.totalStudents, TealPrimary)
                    ProgressRowItem("২য় প্রান্তিক মূল্যায়ন", summary.term2Count, summary.totalStudents, TealPrimary)
                    ProgressRowItem("৩য় প্রান্তিক মূল্যায়ন", summary.term3Count, summary.totalStudents, EmeraldGrade)
                    ProgressRowItem(
                        label = buildString {
                            append("উপস্থিতি এন্ট্রি")
                            summary.avgAttendancePct?.let { append(" (গড় ${fmt(it)}%)") }
                        },
                        done = summary.attendanceFilledCount,
                        total = summary.totalStudents,
                        barColor = AmberNotice
                    )
                    ProgressRowItem("গুণাবলি (সক্রিয়তা, শৃঙ্খলা, আচরণ)", summary.traitsFilledCount, total, Color(0xFF0284C7))
                }
            }
        }

        if (showClassBreakdown && summary.classBreakdown.size > 1) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "শ্রেণিভিত্তিক সংক্ষেপ (ট্যাপ করে ফিল্টার করুন)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = SlateMuted
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    summary.classBreakdown.forEach { item ->
                        val pct = if (item.totalStudents > 0) (item.evaluatedCount * 100.0 / item.totalStudents) else 0.0
                        Surface(
                            onClick = { onSelectClassFilter(item.cls) },
                            color = SlateBg,
                            border = BorderStroke(1.dp, SlateOutline),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("dash_cls_breakdown_${item.cls}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
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
    val pct = ratio * 100f
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
private fun GapsTabBody(
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

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "অপূর্ণ তথ্য ও ঘাটতি",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = SlateText
                )
                Text(
                    text = if (gapItems.isEmpty()) "নির্বাচিত পরিসরে কোনো ঘাটতি নেই"
                    else "${bn(gapItems.size.toString())} জন শিক্ষার্থীর তথ্য অসম্পূর্ণ",
                    fontSize = 11.sp,
                    color = if (gapItems.isEmpty()) EmeraldGrade else AmberNotice
                )
            }
            if (gapItems.isNotEmpty() && canOpenSheet) {
                Surface(
                    onClick = { onOpenBatchSheet(gapItems.first().student.cls) },
                    color = TealSoft,
                    border = BorderStroke(1.dp, TealContainer),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("dash_gaps_open_sheet_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.GridOn, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(14.dp))
                        Text("ব্যাচ এন্ট্রি", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TealPrimary)
                    }
                }
            }
        }

        if (gapItems.isEmpty()) {
            AnalyticsEmptyState(
                icon = Icons.Rounded.CheckCircle,
                title = "সব তথ্য সম্পূর্ণ আছে!",
                subtitle = "নির্বাচিত শ্রেণি ও প্রান্তিকে কোনো নম্বর, উপস্থিতি বা গুণাবলি বাকি নেই।",
                iconTint = EmeraldGrade
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
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
                    Surface(
                        onClick = { activeFilter = f },
                        color = if (sel) TealPrimary else SlateBg,
                        border = BorderStroke(1.dp, if (sel) TealPrimary else SlateOutline),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                    ) {
                        Text(
                            text = "${f.labelBn} (${bn(count.toString())})",
                            fontSize = 10.5.sp,
                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (sel) Color.White else SlateText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                Text(
                    "এই ক্যাটাগরিতে কোনো ঘাটতি নেই।",
                    fontSize = 11.5.sp,
                    color = SlateMuted,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                val shown = if (showAll) filtered else filtered.take(6)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    shown.forEach { item ->
                        Surface(
                            onClick = { onOpenStudent(item.student) },
                            color = SlateBg,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("dash_gap_row_${item.student.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Surface(
                                    color = if (item.missingMarks) RoseBg else AmberBg,
                                    shape = RoundedCornerShape(7.dp)
                                ) {
                                    Text(
                                        "${classShortLabel(item.student.cls)} · ${bn(item.student.roll.toString().padStart(2, '0'))}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp,
                                        color = if (item.missingMarks) RoseDanger else AmberNotice,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
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
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (filtered.size > 6) {
                    TextButton(
                        onClick = { showAll = !showAll },
                        modifier = Modifier
                            .align(Alignment.End)
                            .defaultMinSize(minHeight = 40.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(
                            if (showAll) "কম দেখুন" else "আরও ${bn((filtered.size - 6).toString())} জন দেখুন",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TealPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = TealPrimary
) {
    Surface(
        color = SlateBg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Surface(
                color = iconTint.copy(alpha = 0.12f),
                shape = CircleShape
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.padding(9.dp).size(22.dp)
                )
            }
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = SlateText,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = SlateMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
