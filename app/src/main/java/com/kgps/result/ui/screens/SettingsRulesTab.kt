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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.FailIndicatorStyle
import com.kgps.result.OFFICIAL_EVAL_GUIDELINES
import com.kgps.result.ResultEvaluationRules
import com.kgps.result.bn
import com.kgps.result.fmt
import com.kgps.result.fmtEn
import com.kgps.result.fromBn
import com.kgps.result.ui.theme.AmberBg
import com.kgps.result.ui.theme.AmberBorder
import com.kgps.result.ui.theme.AmberNotice
import com.kgps.result.ui.theme.EmeraldGrade
import com.kgps.result.ui.theme.RoseBg
import com.kgps.result.ui.theme.RoseDanger
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealContainer
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft
import com.kgps.result.ui.theme.appFilterChipBorder
import com.kgps.result.ui.theme.appFilterChipColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsRulesTab(
    rules: ResultEvaluationRules,
    isAdmin: Boolean,
    onUpdateRules: (ResultEvaluationRules) -> Unit,
    onShowMessage: (String) -> Unit
) {
    var expandedGuidelineId by remember { mutableStateOf<String?>(null) }
    var guidelinesSectionExpanded by remember { mutableStateOf(true) }
    var attendanceInputText by remember(rules.minAttendancePct) {
        mutableStateOf(fmtEn(rules.minAttendancePct))
    }
    var confirmDisablePrevRoll by remember { mutableStateOf(false) }
    var confirmResetDefaults by remember { mutableStateOf(false) }

    LaunchedEffect(rules.minAttendancePct) {
        attendanceInputText = fmtEn(rules.minAttendancePct)
    }

    fun applyRulesUpdate(next: ResultEvaluationRules, toast: String? = null) {
        if (!isAdmin) {
            onShowMessage("শর্তাবলী পরিবর্তনের অনুমতি শুধুমাত্র অ্যাডমিনের")
            return
        }
        onUpdateRules(next)
        if (toast != null) onShowMessage(toast)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!isAdmin) {
            Surface(
                color = AmberBg,
                border = BorderStroke(1.dp, AmberBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "আপনি 'শিক্ষক' ভূমিকায় আছেন — শর্তাবলী দেখতে পারবেন, তবে অন/অফ বা পরিবর্তন শুধুমাত্র বিদ্যালয়ের অ্যাডমিন করতে পারবেন।",
                    fontSize = 11.5.sp,
                    color = AmberNotice,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                )
            }
        }

        // 1. সরকারি নির্দেশনা (সংক্ষিপ্ত শিরোনাম + ট্যাপে বিস্তারিত + এপ্লাই হবে না টিক)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, TealContainer),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { guidelinesSectionExpanded = !guidelinesSectionExpanded }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(color = TealSoft, shape = RoundedCornerShape(8.dp)) {
                        Icon(
                            Icons.Rounded.Gavel,
                            contentDescription = "সরকারি মূল্যায়ন নির্দেশনা",
                            tint = TealPrimary,
                            modifier = Modifier.padding(6.dp).size(18.dp)
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "সরকারি মূল্যায়ন নির্দেশিকা (প্রাথমিক)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = SlateText
                        )
                        Text(
                            "শিরোনামে ট্যাপ করলে বিস্তারিত দেখা যাবে · প্রয়োজনে 'প্রয়োগ হবে না' টিক দিন",
                            fontSize = 11.sp,
                            color = SlateMuted
                        )
                    }
                    Icon(
                        imageVector = if (guidelinesSectionExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (guidelinesSectionExpanded) "সংকুচিত করুন" else "বিস্তারিত দেখুন",
                        tint = SlateMuted
                    )
                }

                AnimatedVisibility(visible = guidelinesSectionExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OFFICIAL_EVAL_GUIDELINES.forEach { item ->
                            val isExpanded = expandedGuidelineId == item.id
                            val isIgnored = when (item.id) {
                                "GOVT_3_1_ATTENDANCE" -> item.id in rules.ignoredGuidelineIds || !rules.attendanceRuleEnabled
                                "GOVT_3_2_PASS" -> item.id in rules.ignoredGuidelineIds ||
                                    (!rules.subjectPassRuleEnabled && !rules.promotionCheckAvgPass)
                                "GOVT_3_4_ABSENT" -> item.id in rules.ignoredGuidelineIds || !rules.absentCountsAsFail
                                "GOVT_4_2_PROGRESS" -> item.id in rules.ignoredGuidelineIds || !rules.dynamicTermAveraging
                                else -> item.id in rules.ignoredGuidelineIds
                            }

                            fun toggleGuidelineIgnored(checked: Boolean) {
                                val nextIgnored = if (checked) {
                                    rules.ignoredGuidelineIds + item.id
                                } else {
                                    rules.ignoredGuidelineIds - item.id
                                }
                                val nextRules = when (item.id) {
                                    "GOVT_3_1_ATTENDANCE" -> rules.copy(
                                        ignoredGuidelineIds = nextIgnored,
                                        attendanceRuleEnabled = !checked,
                                        promotionCheckAttendance = !checked
                                    )
                                    "GOVT_3_2_PASS" -> rules.copy(
                                        ignoredGuidelineIds = nextIgnored,
                                        subjectPassRuleEnabled = !checked,
                                        promotionCheckAvgPass = !checked,
                                        specialNeedsRuleEnabled = !checked
                                    )
                                    "GOVT_3_4_ABSENT" -> rules.copy(
                                        ignoredGuidelineIds = nextIgnored,
                                        absentCountsAsFail = !checked
                                    )
                                    "GOVT_4_2_PROGRESS" -> rules.copy(
                                        ignoredGuidelineIds = nextIgnored,
                                        dynamicTermAveraging = !checked
                                    )
                                    else -> rules.copy(ignoredGuidelineIds = nextIgnored)
                                }
                                applyRulesUpdate(nextRules)
                            }

                            Surface(
                                color = if (isIgnored) AmberBg.copy(alpha = 0.65f) else SlateBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (isIgnored) AmberBorder else SlateOutline.copy(alpha = 0.45f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("guideline_card_${item.id}")
                            ) {
                                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                expandedGuidelineId = if (isExpanded) null else item.id
                                            },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            color = if (isIgnored) AmberBorder.copy(alpha = 0.35f) else TealContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                item.sectionLabelBn,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isIgnored) AmberNotice else TealDark,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                item.shortTitleBn,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isIgnored) SlateMuted else SlateText,
                                                textDecoration = if (isIgnored) TextDecoration.LineThrough else TextDecoration.None
                                            )
                                            Text(
                                                item.summaryBn,
                                                fontSize = 11.sp,
                                                color = SlateMuted,
                                                maxLines = if (isExpanded) 4 else 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                            contentDescription = "বিস্তারিত",
                                            tint = SlateMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    AnimatedVisibility(visible = isExpanded) {
                                        Column(
                                            Modifier.padding(top = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            HorizontalDivider(color = SlateOutline.copy(alpha = 0.35f))
                                            Text(
                                                item.fullDetailsBn,
                                                fontSize = 11.5.sp,
                                                color = SlateText,
                                                lineHeight = 17.sp
                                            )
                                            Surface(
                                                color = MaterialTheme.colorScheme.surface,
                                                shape = RoundedCornerShape(7.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    item.linkedRuleHintBn,
                                                    fontSize = 10.5.sp,
                                                    color = TealDark,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp)
                                            .clickable(enabled = isAdmin) { toggleGuidelineIgnored(!isIgnored) },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isIgnored,
                                            enabled = isAdmin,
                                            onCheckedChange = { checked -> toggleGuidelineIgnored(checked) },
                                            modifier = Modifier.testTag("guideline_ignore_${item.id}")
                                        )
                                        Text(
                                            "এই শর্ত এপ্লাই হবে না",
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isIgnored) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isIgnored) AmberNotice else SlateMuted
                                        )
                                        Spacer(Modifier.weight(1f))
                                        Surface(
                                            color = if (isIgnored) RoseBg else TealSoft,
                                            shape = RoundedCornerShape(999.dp)
                                        ) {
                                            Text(
                                                if (isIgnored) "অফ" else "অন",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isIgnored) RoseDanger else EmeraldGrade,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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

        // শর্ত ১: ন্যূনতম ৮৫% উপস্থিতির শর্ত
        RuleSectionCard(
            numberBn = "১",
            title = "ন্যূনতম উপস্থিতির শর্ত (${fmt(rules.minAttendancePct)}%)",
            subtitle = "ডিফল্ট অফ থাকে; অন করলে পরবর্তী শ্রেণিতে উত্তীর্ণ যাচাইয়ে ব্যবহার হবে",
            icon = Icons.Rounded.EventBusy,
            badgeText = if (rules.attendanceRuleEnabled) "অন · ${fmt(rules.minAttendancePct)}%" else "অফ",
            badgeActive = rules.attendanceRuleEnabled
        ) {
            RuleSwitchRow(
                label = "ন্যূনতম উপস্থিতির হার যাচাই (অন / অফ)",
                checked = rules.attendanceRuleEnabled,
                enabled = isAdmin,
                testTag = "rule_1_attendance_switch",
                onCheckedChange = { checked ->
                    val nextIgnored = if (checked) {
                        rules.ignoredGuidelineIds - "GOVT_3_1_ATTENDANCE"
                    } else {
                        rules.ignoredGuidelineIds + "GOVT_3_1_ATTENDANCE"
                    }
                    applyRulesUpdate(
                        rules.copy(
                            attendanceRuleEnabled = checked,
                            promotionCheckAttendance = checked,
                            ignoredGuidelineIds = nextIgnored
                        )
                    )
                }
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = attendanceInputText,
                    onValueChange = { raw ->
                        val clean = fromBn(raw).filter { it.isDigit() || it == '.' }
                        attendanceInputText = clean
                    },
                    enabled = isAdmin,
                    label = { Text("উপস্থিতির ন্যূনতম হার (%)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rule_1_attendance_pct_input")
                )
                Button(
                    onClick = {
                        val parsed = fromBn(attendanceInputText).toDoubleOrNull()
                        if (parsed == null || parsed < 1.0 || parsed > 100.0) {
                            onShowMessage("১ থেকে ১০০-এর মধ্যে সঠিক শতাংশ লিখুন")
                        } else {
                            applyRulesUpdate(
                                rules.copy(minAttendancePct = parsed),
                                "ন্যূনতম উপস্থিতির হার ${fmt(parsed)}% নির্ধারিত হয়েছে"
                            )
                        }
                    },
                    enabled = isAdmin,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("rule_1_attendance_save_btn")
                ) {
                    Text("নির্ধারণ")
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(75.0, 80.0, 85.0, 90.0).forEach { preset ->
                    FilterChip(
                        selected = kotlin.math.abs(rules.minAttendancePct - preset) < 0.01,
                        onClick = {
                            if (isAdmin) {
                                attendanceInputText = fmtEn(preset)
                                applyRulesUpdate(rules.copy(minAttendancePct = preset))
                            }
                        },
                        enabled = isAdmin,
                        label = { Text("${fmt(preset)}%", fontSize = 11.sp) },
                        colors = appFilterChipColors(),
                        border = appFilterChipBorder(kotlin.math.abs(rules.minAttendancePct - preset) < 0.01)
                    )
                }
            }
        }

        // শর্ত ২: বিষয়ভিত্তিক ৪০% পাশ নম্বর ও ফেইল নির্দেশক
        RuleSectionCard(
            numberBn = "২",
            title = "বিষয়ভিত্তিক ${fmt(rules.regularPassPct)}% পাশ ও ফেইল নির্দেশক",
            subtitle = "কোনো বিষয়ের মোট নম্বরে ${fmt(rules.regularPassPct)}%-এর নিচে পেলে সেই প্রান্তিকের মোট সেলে নির্দেশক দেখাবে",
            icon = Icons.Rounded.Warning,
            badgeText = if (rules.subjectPassRuleEnabled && rules.showFailIndicator) {
                rules.failIndicatorStyle.sampleBn
            } else if (rules.subjectPassRuleEnabled) {
                "নির্দেশক অফ"
            } else {
                "অফ"
            },
            badgeActive = rules.subjectPassRuleEnabled
        ) {
            RuleSwitchRow(
                label = "বিষয়ভিত্তিক পাশ নম্বর যাচাই (${fmt(rules.regularPassPct)}%)",
                checked = rules.subjectPassRuleEnabled,
                enabled = isAdmin,
                testTag = "rule_2_subject_pass_switch",
                onCheckedChange = { checked ->
                    applyRulesUpdate(rules.copy(subjectPassRuleEnabled = checked))
                }
            )
            RuleSwitchRow(
                label = "নম্বর সেলে ফেইল নির্দেশক দেখান",
                checked = rules.showFailIndicator,
                enabled = isAdmin && rules.subjectPassRuleEnabled,
                testTag = "rule_2_fail_indicator_switch",
                onCheckedChange = { checked ->
                    applyRulesUpdate(rules.copy(showFailIndicator = checked))
                }
            )
            if (rules.subjectPassRuleEnabled && rules.showFailIndicator) {
                Text(
                    "ফেইল নির্দেশক ধরন বেছে নিন:",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateText
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FailIndicatorStyle.entries.forEach { style ->
                        val selected = rules.failIndicatorStyle == style
                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (isAdmin) {
                                    applyRulesUpdate(rules.copy(failIndicatorStyle = style))
                                }
                            },
                            enabled = isAdmin,
                            label = { Text(style.labelBn, fontSize = 11.sp) },
                            colors = appFilterChipColors(),
                            border = appFilterChipBorder(selected),
                            modifier = Modifier.testTag("rule_2_style_${style.id}")
                        )
                    }
                }
                Surface(
                    color = RoseBg,
                    border = BorderStroke(1.dp, RoseDanger.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "নমুনা প্রান্তিক মোট সেল (১০০-তে ৩৪):",
                            fontSize = 11.sp,
                            color = SlateText
                        )
                        Surface(color = Color.White, shape = RoundedCornerShape(6.dp)) {
                            val formatted = com.kgps.result.ResultRulesEngine.formatTermTotalCell("৩৪", true, rules)
                            Text(
                                formatted,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoseDanger,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // শর্ত ৩: বিশেষ চাহিদাসম্পন্ন শিক্ষার্থীর ৩৩%
        RuleSectionCard(
            numberBn = "৩",
            title = "বিশেষ চাহিদাসম্পন্ন শিক্ষার্থীর ৩৩% সুবিধা",
            subtitle = "অন থাকলে ৩৩%-এ উত্তীর্ণ হয়ে স্বাভাবিকের আগেও রোল/মেধাক্রমে আসতে পারবে",
            icon = Icons.Rounded.AccessibilityNew,
            badgeText = if (rules.specialNeedsRuleEnabled) "অন · ৩৩%" else "অফ",
            badgeActive = rules.specialNeedsRuleEnabled
        ) {
            RuleSwitchRow(
                label = "বিশেষ চাহিদাসম্পন্ন শিক্ষার্থীর ৩৩% পাশ নীতি",
                checked = rules.specialNeedsRuleEnabled,
                enabled = isAdmin,
                testTag = "rule_3_special_needs_switch",
                onCheckedChange = { checked ->
                    applyRulesUpdate(rules.copy(specialNeedsRuleEnabled = checked))
                }
            )
            Text(
                "শিক্ষার্থী এডিটরের 'পরিচিতি ও শ্রেণি' ট্যাবে যেসব শিক্ষার্থীকে 'বিশেষ চাহিদাসম্পন্ন' চিহ্নিত করবেন, তাদের জন্য ৪০%-এর বদলে ৩৩% পাশ কার্যকর হবে।",
                fontSize = 11.sp,
                color = SlateMuted,
                lineHeight = 16.sp
            )
        }

        // শর্ত ৪: পরবর্তী শ্রেণিতে উত্তীর্ণ
        RuleSectionCard(
            numberBn = "৪",
            title = "পরবর্তী শ্রেণিতে উত্তীর্ণ হওয়ার শর্ত",
            subtitle = "ডিফল্টভাবে গড়ে ৪০% (বিশেষ ৩৩%) পাস; চাইলে উপস্থিতি বা সব বিষয়ও যুক্ত করুন",
            icon = Icons.Rounded.School,
            badgeText = buildString {
                val parts = mutableListOf<String>()
                if (rules.promotionCheckAvgPass) parts.add("গড় ৪০%")
                if (rules.attendanceRuleEnabled && rules.promotionCheckAttendance) parts.add("উপস্থিতি")
                if (rules.promotionCheckAllSubjects) parts.add("সব বিষয়")
                append(if (parts.isEmpty()) "শর্তহীন" else parts.joinToString(" + "))
            },
            badgeActive = rules.promotionCheckAvgPass || (rules.attendanceRuleEnabled && rules.promotionCheckAttendance)
        ) {
            RuleCheckboxRow(
                label = "গড়ে ৪০% পাস (বিশেষ চাহিদাসম্পন্ন: ৩৩%)",
                sub = "ডিফল্ট অন — গড় নম্বর ৪০%-এর নিচে থাকলে অনুত্তীর্ণ ধরা হবে",
                checked = rules.promotionCheckAvgPass,
                enabled = isAdmin,
                testTag = "rule_4_avg_pass_check",
                onCheckedChange = { checked ->
                    applyRulesUpdate(rules.copy(promotionCheckAvgPass = checked))
                }
            )
            RuleCheckboxRow(
                label = "উপস্থিতি বিবেচনা (ন্যূনতম ${fmt(rules.minAttendancePct)}%)",
                sub = if (rules.attendanceRuleEnabled && rules.promotionCheckAttendance) {
                    "শর্ত ১ অনুযায়ী ${fmt(rules.minAttendancePct)}% উপস্থিতি না থাকলে অনুত্তীর্ণ"
                } else {
                    "অন করলে শর্ত ১-এর ন্যূনতম উপস্থিতির শর্ত স্বয়ংক্রিয়ভাবে সক্রিয় হবে"
                },
                checked = rules.attendanceRuleEnabled && rules.promotionCheckAttendance,
                enabled = isAdmin,
                testTag = "rule_4_attendance_check",
                onCheckedChange = { checked ->
                    val nextIgnored = if (checked) {
                        rules.ignoredGuidelineIds - "GOVT_3_1_ATTENDANCE"
                    } else {
                        rules.ignoredGuidelineIds + "GOVT_3_1_ATTENDANCE"
                    }
                    applyRulesUpdate(
                        rules.copy(
                            attendanceRuleEnabled = checked,
                            promotionCheckAttendance = checked,
                            ignoredGuidelineIds = nextIgnored
                        )
                    )
                }
            )
            RuleCheckboxRow(
                label = "সব বিষয়ে আলাদাভাবে পাস (ঐচ্ছিক কঠোর নিয়ম)",
                sub = "বন্ধ থাকলে ১–২ বিষয়ে ফেল করলেও গড় ৪০% হলে উত্তীর্ণ থাকবে",
                checked = rules.promotionCheckAllSubjects,
                enabled = isAdmin,
                testTag = "rule_4_all_subjects_check",
                onCheckedChange = { checked ->
                    applyRulesUpdate(rules.copy(promotionCheckAllSubjects = checked))
                }
            )
        }

        // শর্ত ৫: অনুত্তীর্ণ শিক্ষার্থীর মেধাক্রম
        RuleSectionCard(
            numberBn = "৫",
            title = "অনুত্তীর্ণ শিক্ষার্থী ও মেধাক্রম বিন্যাস",
            subtitle = "তিন স্তরের অগ্রাধিকার (সব বিষয়ে পাস → গড়ে ৪০% পাস → ৪০%-এর নিচে)",
            icon = Icons.Rounded.Leaderboard,
            badgeText = when {
                rules.meritTierAllSubPassFirst && rules.meritTierAvgPassEnabled -> "৩ স্তর"
                rules.meritTierAllSubPassFirst || rules.meritTierAvgPassEnabled -> "২ স্তর"
                else -> "শুধু নম্বর"
            },
            badgeActive = rules.meritTierAllSubPassFirst || rules.meritTierAvgPassEnabled
        ) {
            RuleSwitchRow(
                label = "১) সব বিষয়ে ৪০% পেয়ে পাশ আগে থাকবে",
                checked = rules.meritTierAllSubPassFirst,
                enabled = isAdmin,
                testTag = "rule_5_all_sub_first_switch",
                onCheckedChange = { checked ->
                    applyRulesUpdate(rules.copy(meritTierAllSubPassFirst = checked))
                }
            )
            RuleSwitchRow(
                label = "২) গড়ে ৪০% পেয়ে পাশ (৪০%-এর নিচের আগে থাকবে)",
                checked = rules.meritTierAvgPassEnabled,
                enabled = isAdmin,
                testTag = "rule_5_avg_pass_switch",
                onCheckedChange = { checked ->
                    applyRulesUpdate(rules.copy(meritTierAvgPassEnabled = checked))
                }
            )
            val flowPreview = when {
                rules.meritTierAllSubPassFirst && rules.meritTierAvgPassEnabled ->
                    "বর্তমান ক্রম: ① সব বিষয়ে ৪০%+ → ② গড়ে ৪০%+ → ③ গড়ে ৪০%-এর নিচে"
                rules.meritTierAllSubPassFirst && !rules.meritTierAvgPassEnabled ->
                    "বর্তমান ক্রম: ① সব বিষয়ে ৪০%+ → ② অবশিষ্ট সবাই (নম্বর অনুসারে)"
                !rules.meritTierAllSubPassFirst && rules.meritTierAvgPassEnabled ->
                    "বর্তমান ক্রম: ① গড়ে ৪০%+ (সব পাস ও আংশিক ফেল একসাথে নম্বরে) → ② ৪০%-এর নিচে"
                else ->
                    "বর্তমান ক্রম: পাস-ফেল ভেদাভেদ ছাড়া শুধুমাত্র মোট প্রাপ্ত নম্বরের সিরিয়াল"
            }
            Surface(
                color = TealSoft,
                border = BorderStroke(1.dp, TealContainer),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    flowPreview,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TealDark,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }
        }

        // শর্ত ৬: ৩ প্রান্তিক বনাম ১/২ প্রান্তিক গড়
        RuleSectionCard(
            numberBn = "৬",
            title = "প্রান্তিকভিত্তিক ফলাফল ও গড় হিসাব",
            subtitle = "১ বা ২ প্রান্তিকের নম্বর দিলে সে অনুযায়ী গড় দেখাবে; ৩ প্রান্তিক পূর্ণ হলে ফাইনাল ফলাফল",
            icon = Icons.Rounded.DateRange,
            badgeText = if (rules.dynamicTermAveraging) "১/২/৩ সচল" else "স্থির ÷৩",
            badgeActive = rules.dynamicTermAveraging
        ) {
            RuleSwitchRow(
                label = "প্রদত্ত প্রান্তিক সংখ্যার ভিত্তিতে গড় হিসাব করুন",
                checked = rules.dynamicTermAveraging,
                enabled = isAdmin,
                testTag = "rule_6_dynamic_term_switch",
                onCheckedChange = { checked ->
                    applyRulesUpdate(rules.copy(dynamicTermAveraging = checked))
                }
            )
        }

        // শর্ত ৭: অনুপস্থিত 'A' বনাম প্রাপ্ত '0'
        RuleSectionCard(
            numberBn = "৭",
            title = "অনুপস্থিত (A) এবং ০ নম্বরের পার্থক্য",
            subtitle = "নম্বর ঘরে 'A' নির্বাচন করলে ০ ধরা হলেও তা অনুপস্থিত হিসেবে আলাদা থাকবে",
            icon = Icons.Rounded.FactCheck,
            badgeText = if (rules.absentCountsAsFail) "অন · A=অকৃতকার্য" else "অফ · শুধু নম্বর",
            badgeActive = rules.absentCountsAsFail
        ) {
            RuleSwitchRow(
                label = "অনুপস্থিত (A) থাকলে সেই বিষয় অকৃতকার্য ধরুন",
                checked = rules.absentCountsAsFail,
                enabled = isAdmin,
                testTag = "rule_7_absent_fail_switch",
                onCheckedChange = { checked ->
                    val nextIgnored = if (checked) {
                        rules.ignoredGuidelineIds - "GOVT_3_4_ABSENT"
                    } else {
                        rules.ignoredGuidelineIds + "GOVT_3_4_ABSENT"
                    }
                    applyRulesUpdate(
                        rules.copy(
                            absentCountsAsFail = checked,
                            ignoredGuidelineIds = nextIgnored
                        )
                    )
                }
            )
            Text(
                "• শিক্ষার্থী এডিটর ও বাল্ক শিটে লিখিত/ধারাবাহিক সেলে 'A' চাপলে 'A' হিসেবে সংরক্ষিত হয়, আর প্রাপ্ত শূন্যের জন্য '0' থাকে।\n" +
                    "• অন থাকলে কোনো অংশে 'A' থাকলে মোট নম্বরের হিসাব যাই হোক বিষয়টি অকৃতকার্য (বিকল্প পরীক্ষার আগে পর্যন্ত) হিসেবে চিহ্নিত হবে।",
                fontSize = 11.sp,
                color = SlateMuted,
                lineHeight = 16.sp
            )
        }

        // শর্ত ৮: মেধাক্রম যৌথ ও টাই-ব্রেকার
        RuleSectionCard(
            numberBn = "৮",
            title = "মেধাক্রম যৌথ ও সমান নম্বরে টাই-ব্রেকার",
            subtitle = "নম্বর সমান হলে উপস্থিতি, গুণাবলি ও পূর্বের রোল বিবেচনা করা যায়",
            icon = Icons.Rounded.EmojiEvents,
            badgeText = if (rules.jointMeritEnabled) "যৌথ অন" else "স্বতন্ত্র ক্রম",
            badgeActive = rules.jointMeritEnabled
        ) {
            RuleSwitchRow(
                label = "মেধাক্রম যৌথ হতে পারে (সমান অবস্থানে একই স্থান)",
                checked = rules.jointMeritEnabled,
                enabled = isAdmin,
                testTag = "rule_8_joint_merit_switch",
                onCheckedChange = { checked ->
                    applyRulesUpdate(rules.copy(jointMeritEnabled = checked))
                }
            )
            HorizontalDivider(color = SlateOutline.copy(alpha = 0.35f))
            Text(
                "নম্বর সমান হলে টাই-ব্রেকার বিবেচনা:",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = SlateText
            )
            RuleCheckboxRow(
                label = "১) উপস্থিতি বিবেচ্য",
                sub = "যার শ্রেণি উপস্থিতির হার বেশি সে এগিয়ে থাকবে",
                checked = rules.tieBreakAttendance,
                enabled = isAdmin,
                testTag = "rule_8_tie_attendance_check",
                onCheckedChange = { checked ->
                    applyRulesUpdate(rules.copy(tieBreakAttendance = checked))
                }
            )
            RuleCheckboxRow(
                label = "২) গুণাবলি বিবেচ্য (ক > খ > গ)",
                sub = "সহপাঠক্রমিক, শৃঙ্খলাবোধ ও আচরণে 'ক' অর্জনকারী এগিয়ে থাকবে",
                checked = rules.tieBreakTraits,
                enabled = isAdmin,
                testTag = "rule_8_tie_traits_check",
                onCheckedChange = { checked ->
                    applyRulesUpdate(rules.copy(tieBreakTraits = checked))
                }
            )
            RuleCheckboxRow(
                label = "৩) পূর্বের রোল বিবেচ্য (ডিফল্ট অন)",
                sub = "পূর্বের শ্রেণির ছোট রোল নম্বর এগিয়ে থাকবে (অফ করলে সতর্কতা দেখাবে)",
                checked = rules.tieBreakPrevRoll,
                enabled = isAdmin,
                testTag = "rule_8_tie_prev_roll_check",
                onCheckedChange = { checked ->
                    if (!checked && rules.tieBreakPrevRoll) {
                        confirmDisablePrevRoll = true
                    } else {
                        applyRulesUpdate(rules.copy(tieBreakPrevRoll = checked))
                    }
                }
            )
            if (rules.jointMeritEnabled && rules.tieBreakPrevRoll) {
                Surface(
                    color = AmberBg,
                    border = BorderStroke(1.dp, AmberBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "টিপস: 'পূর্বের রোল বিবেচ্য' অন থাকায় রোল ভিন্ন হলে মেধাক্রম রোল দিয়ে আলাদা হয়ে যাবে। সত্যিকারের যৌথ মেধাক্রম (যেমন ১, ১, ৩) দেখতে চাইলে 'পূর্বের রোল বিবেচ্য' অফ করুন।",
                        fontSize = 10.5.sp,
                        color = AmberNotice,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (isAdmin) {
            OutlinedButton(
                onClick = { confirmResetDefaults = true },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rules_reset_defaults_btn")
            ) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("ডিফল্ট শর্তাবলীতে ফিরে যান")
            }
        }
    }

    if (confirmDisablePrevRoll) {
        AlertDialog(
            onDismissRequest = { confirmDisablePrevRoll = false },
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = AmberNotice) },
            title = { Text("পূর্বের রোল বিবেচ্য বন্ধ করবেন?", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Text(
                    "এটি বন্ধ করলে দুইজন শিক্ষার্থীর নম্বর (এবং সক্রিয় থাকলে উপস্থিতি ও গুণাবলি) সমান হয়ে গেলে পূর্বের রোল অনুযায়ী সিরিয়াল ভাঙা হবে না।\n\n• 'মেধাক্রম যৌথ' চালু থাকলে দুজনেই একই যৌথ মেধাক্রম (যেমন: ১, ১, ৩) পাবে।\n• 'মেধাক্রম যৌথ' বন্ধ থাকলে তালিকা প্রদর্শনে রোল/আইডি ক্রমে সাজানো হবে।",
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDisablePrevRoll = false
                        applyRulesUpdate(
                            rules.copy(tieBreakPrevRoll = false),
                            "পূর্বের রোল বিবেচ্য বন্ধ করা হয়েছে"
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberNotice),
                    modifier = Modifier.testTag("confirm_disable_prev_roll_btn")
                ) { Text("হ্যাঁ, বন্ধ করুন") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDisablePrevRoll = false }) { Text("বাতিল") }
            }
        )
    }

    if (confirmResetDefaults) {
        AlertDialog(
            onDismissRequest = { confirmResetDefaults = false },
            title = { Text("ডিফল্ট শর্তে ফিরবেন?", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("শর্ত ১ থেকে ৮ পর্যন্ত সকল সেটিংস প্রাথমিক ডিফল্ট অবস্থায় ফিরে যাবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmResetDefaults = false
                        applyRulesUpdate(ResultEvaluationRules(), "শর্তাবলী ডিফল্টে রিসেট হয়েছে")
                    },
                    modifier = Modifier.testTag("confirm_reset_rules_btn")
                ) { Text("হ্যাঁ, রিসেট করুন") }
            },
            dismissButton = {
                TextButton(onClick = { confirmResetDefaults = false }) { Text("বাতিল") }
            }
        )
    }
}

@Composable
private fun RuleSectionCard(
    numberBn: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeText: String,
    badgeActive: Boolean,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (badgeActive) TealContainer else SlateOutline.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = if (badgeActive) TealSoft else SlateBg,
                    shape = RoundedCornerShape(9.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (badgeActive) TealPrimary else SlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "$numberBn। $title",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SlateText
                    )
                    Text(
                        subtitle,
                        fontSize = 11.sp,
                        color = SlateMuted,
                        lineHeight = 15.sp
                    )
                }
                Surface(
                    color = if (badgeActive) TealContainer else SlateBg,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (badgeActive) TealDark else SlateMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun RuleSwitchRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) SlateText else SlateMuted,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = if (checked) TealSoft else SlateBg,
                border = BorderStroke(1.dp, if (checked) TealPrimary.copy(alpha = 0.45f) else SlateOutline.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = if (checked) "অন" else "অফ",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (checked) TealDark else SlateMuted,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}

@Composable
private fun RuleCheckboxRow(
    label: String,
    sub: String,
    checked: Boolean,
    enabled: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.testTag(testTag)
        )
        Column(Modifier.weight(1f).padding(start = 2.dp)) {
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) SlateText else SlateMuted
            )
            Text(
                sub,
                fontSize = 10.5.sp,
                color = SlateMuted,
                lineHeight = 14.sp
            )
        }
    }
}
