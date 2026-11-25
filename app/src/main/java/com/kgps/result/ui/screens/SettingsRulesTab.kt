package com.kgps.result.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material.icons.rounded.VerifiedUser
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
import com.kgps.result.ResultRulesEngine
import com.kgps.result.bn
import com.kgps.result.fmt
import com.kgps.result.fmtEn
import com.kgps.result.fromBn
import com.kgps.result.ui.components.TrackedGroupBox
import com.kgps.result.ui.components.TrackedGroupInfo
import com.kgps.result.ui.theme.AmberBg
import com.kgps.result.ui.theme.AmberBorder
import com.kgps.result.ui.theme.AmberNotice
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

private const val GROUP_PASS = "pass"
private const val GROUP_MERIT = "merit"
private const val GROUP_ATTENDANCE_TERM = "attendance_term"
private const val GROUP_GUIDELINES = "guidelines"

private val ALL_RULE_GROUPS = setOf(GROUP_PASS, GROUP_MERIT, GROUP_ATTENDANCE_TERM, GROUP_GUIDELINES)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsRulesTab(
    rules: ResultEvaluationRules,
    isAdmin: Boolean,
    onUpdateRules: (ResultEvaluationRules) -> Unit,
    onShowMessage: (String) -> Unit,
    contentRootY: () -> Float = { 0f },
    trackedGroups: MutableMap<String, TrackedGroupInfo>? = null
) {
    var expandedGroups by remember { mutableStateOf(setOf(GROUP_PASS)) }
    var expandedGuidelineId by remember { mutableStateOf<String?>(null) }
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

    fun toggleGroup(groupId: String) {
        expandedGroups = if (groupId in expandedGroups) expandedGroups - groupId else expandedGroups + groupId
    }

    val allExpanded = expandedGroups.containsAll(ALL_RULE_GROUPS)

    val passActiveCount = listOf(
        rules.subjectPassRuleEnabled,
        rules.specialNeedsRuleEnabled,
        rules.absentCountsAsFail
    ).count { it }

    val meritTierLabel = when {
        rules.meritTierAllSubPassFirst && rules.meritTierAvgPassEnabled -> "৩ স্তর"
        rules.meritTierAllSubPassFirst || rules.meritTierAvgPassEnabled -> "২ স্তর"
        else -> "শুধু নম্বর"
    }

    val attendanceOn = rules.attendanceRuleEnabled && rules.promotionCheckAttendance
    val ignoredCount = OFFICIAL_EVAL_GUIDELINES.count { item ->
        when (item.id) {
            "GOVT_3_1_ATTENDANCE" -> item.id in rules.ignoredGuidelineIds || !rules.attendanceRuleEnabled
            "GOVT_3_2_PASS" -> item.id in rules.ignoredGuidelineIds ||
                (!rules.subjectPassRuleEnabled && !rules.promotionCheckAvgPass)
            "GOVT_3_4_ABSENT" -> item.id in rules.ignoredGuidelineIds || !rules.absentCountsAsFail
            "GOVT_4_2_PROGRESS" -> item.id in rules.ignoredGuidelineIds || !rules.dynamicTermAveraging
            else -> item.id in rules.ignoredGuidelineIds
        }
    }
    val activeGuidelineCount = OFFICIAL_EVAL_GUIDELINES.size - ignoredCount

    val isDefaultPreset = rules == ResultEvaluationRules()
    val isStrictPreset = rules.attendanceRuleEnabled &&
        rules.promotionCheckAttendance &&
        rules.promotionCheckAllSubjects &&
        rules.promotionCheckAvgPass &&
        rules.subjectPassRuleEnabled &&
        rules.absentCountsAsFail &&
        rules.meritTierAllSubPassFirst &&
        rules.meritTierAvgPassEnabled
    val isFlexiblePreset = !rules.attendanceRuleEnabled &&
        !rules.promotionCheckAttendance &&
        !rules.promotionCheckAllSubjects &&
        rules.promotionCheckAvgPass &&
        !rules.meritTierAllSubPassFirst &&
        rules.meritTierAvgPassEnabled

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!isAdmin) {
            Surface(
                color = AmberBg,
                border = BorderStroke(1.dp, AmberBorder),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "আপনি 'শিক্ষক' ভূমিকায় আছেন — শর্তাবলী দেখতে পারবেন, তবে পরিবর্তন শুধুমাত্র অ্যাডমিন করতে পারবেন।",
                    fontSize = 11.sp,
                    color = AmberNotice,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }
        }

        // ১. এক নজরে সক্রিয় শর্তসার ও দ্রুত প্রিসেট
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, TealContainer),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "সক্রিয় শর্তসার",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SlateText
                        )
                    }
                    Surface(
                        color = SlateBg,
                        border = BorderStroke(1.dp, SlateOutline.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 32.dp)
                            .clickable {
                                expandedGroups = if (allExpanded) emptySet() else ALL_RULE_GROUPS
                            }
                            .testTag("rules_toggle_all_groups_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                if (allExpanded) Icons.Rounded.UnfoldLess else Icons.Rounded.UnfoldMore,
                                contentDescription = null,
                                tint = TealDark,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                if (allExpanded) "সব বন্ধ" else "সব খুলুন",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TealDark
                            )
                        }
                    }
                }

                // লাইভ স্ট্যাটাস চিপস
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    SummaryChip(
                        text = if (rules.subjectPassRuleEnabled) {
                            "পাশ ${fmt(rules.regularPassPct)}%${if (rules.showFailIndicator) " (${rules.failIndicatorStyle.sampleBn})" else ""}"
                        } else {
                            "বিষয় পাশ অফ"
                        },
                        active = rules.subjectPassRuleEnabled
                    )
                    SummaryChip(
                        text = if (rules.specialNeedsRuleEnabled) "বিশেষ ${fmt(rules.specialNeedsPassPct)}%" else "বিশেষ অফ",
                        active = rules.specialNeedsRuleEnabled
                    )
                    SummaryChip(
                        text = if (attendanceOn) "উপস্থিতি ${fmt(rules.minAttendancePct)}%" else "উপস্থিতি অফ",
                        active = attendanceOn
                    )
                    SummaryChip(
                        text = "মেধাক্রম: $meritTierLabel${if (rules.jointMeritEnabled) " · যৌথ" else ""}",
                        active = rules.meritTierAllSubPassFirst || rules.meritTierAvgPassEnabled
                    )
                    SummaryChip(
                        text = if (rules.dynamicTermAveraging) "১/২/৩ প্রান্তিক গড়" else "স্থির ÷৩",
                        active = rules.dynamicTermAveraging
                    )
                    SummaryChip(
                        text = if (rules.absentCountsAsFail) "A = ফেল" else "A = শুধু নম্বর",
                        active = rules.absentCountsAsFail
                    )
                }

                // দ্রুত প্রিসেট
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "প্রিসেট:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateMuted
                    )
                    FilterChip(
                        selected = isDefaultPreset,
                        enabled = isAdmin,
                        onClick = {
                            applyRulesUpdate(
                                ResultEvaluationRules(),
                                "সরকারি ডিফল্ট প্রিসেট চালু হয়েছে"
                            )
                        },
                        label = { Text("সরকারি ডিফল্ট", fontSize = 11.sp) },
                        colors = appFilterChipColors(),
                        border = appFilterChipBorder(isDefaultPreset),
                        modifier = Modifier.testTag("rules_preset_default")
                    )
                    FilterChip(
                        selected = isStrictPreset,
                        enabled = isAdmin,
                        onClick = {
                            applyRulesUpdate(
                                rules.copy(
                                    attendanceRuleEnabled = true,
                                    promotionCheckAttendance = true,
                                    promotionCheckAvgPass = true,
                                    promotionCheckAllSubjects = true,
                                    subjectPassRuleEnabled = true,
                                    absentCountsAsFail = true,
                                    meritTierAllSubPassFirst = true,
                                    meritTierAvgPassEnabled = true,
                                    ignoredGuidelineIds = rules.ignoredGuidelineIds - setOf("GOVT_3_1_ATTENDANCE", "GOVT_3_2_PASS", "GOVT_3_4_ABSENT")
                                ),
                                "কঠোর নীতি (উপস্থিতি + সব বিষয় পাশ) চালু হয়েছে"
                            )
                        },
                        label = { Text("কঠোর (উপস্থিতি + সব বিষয়)", fontSize = 11.sp) },
                        colors = appFilterChipColors(),
                        border = appFilterChipBorder(isStrictPreset),
                        modifier = Modifier.testTag("rules_preset_strict")
                    )
                    FilterChip(
                        selected = isFlexiblePreset,
                        enabled = isAdmin,
                        onClick = {
                            applyRulesUpdate(
                                rules.copy(
                                    attendanceRuleEnabled = false,
                                    promotionCheckAttendance = false,
                                    promotionCheckAvgPass = true,
                                    promotionCheckAllSubjects = false,
                                    meritTierAllSubPassFirst = false,
                                    meritTierAvgPassEnabled = true,
                                    ignoredGuidelineIds = rules.ignoredGuidelineIds + "GOVT_3_1_ATTENDANCE"
                                ),
                                "নমনীয় (গড় নম্বর ভিত্তিক) প্রিসেট চালু হয়েছে"
                            )
                        },
                        label = { Text("নমনীয় (শুধু গড় ৪০%)", fontSize = 11.sp) },
                        colors = appFilterChipColors(),
                        border = appFilterChipBorder(isFlexiblePreset),
                        modifier = Modifier.testTag("rules_preset_flexible")
                    )
                }
            }
        }

        // গ্রুপ ১: পাশ নম্বর, নির্দেশক ও অনুপস্থিতি (শর্ত ২, ৩, ৭)
        TrackedGroupBox(
            key = "rules_$GROUP_PASS",
            title = "পাশ নম্বর ও অনুপস্থিতি",
            badgeText = "${bn(passActiveCount.toString())}/৩ অন",
            icon = Icons.Rounded.VerifiedUser,
            expanded = GROUP_PASS in expandedGroups,
            contentRootY = contentRootY,
            trackedGroups = trackedGroups,
            onCollapse = { expandedGroups = expandedGroups - GROUP_PASS }
        ) {
        RuleGroupCard(
            title = "পাশ নম্বর ও অনুপস্থিতি",
            subtitle = "শর্ত ২ (বিষয় ${fmt(rules.regularPassPct)}%) · শর্ত ৩ (বিশেষ ৩৩%) · শর্ত ৭ (অনুপস্থিত A)",
            icon = Icons.Rounded.VerifiedUser,
            badgeText = "${bn(passActiveCount.toString())}/৩ অন",
            badgeActive = passActiveCount > 0,
            expanded = GROUP_PASS in expandedGroups,
            headerTestTag = "rules_group_pass",
            onToggle = { toggleGroup(GROUP_PASS) }
        ) {
            CompactRuleHeader(numberBn = "২", title = "বিষয়ভিত্তিক ${fmt(rules.regularPassPct)}% পাশ ও ফেইল নির্দেশক", icon = Icons.Rounded.Warning)
            RuleSwitchRow(
                label = "বিষয়ভিত্তিক পাশ নম্বর যাচাই (${fmt(rules.regularPassPct)}%)",
                checked = rules.subjectPassRuleEnabled,
                enabled = isAdmin,
                testTag = "rule_2_subject_pass_switch",
                onCheckedChange = { checked ->
                    applyRulesUpdate(rules.copy(subjectPassRuleEnabled = checked))
                }
            )
            AnimatedVisibility(visible = rules.subjectPassRuleEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    RuleSwitchRow(
                        label = "নম্বর সেলে ফেইল নির্দেশক দেখান",
                        checked = rules.showFailIndicator,
                        enabled = isAdmin,
                        testTag = "rule_2_fail_indicator_switch",
                        onCheckedChange = { checked ->
                            applyRulesUpdate(rules.copy(showFailIndicator = checked))
                        }
                    )
                    if (rules.showFailIndicator) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
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
                                    label = { Text(style.labelBn, fontSize = 10.5.sp) },
                                    colors = appFilterChipColors(),
                                    border = appFilterChipBorder(selected),
                                    modifier = Modifier.testTag("rule_2_style_${style.id}")
                                )
                            }
                        }
                        Surface(
                            color = RoseBg,
                            border = BorderStroke(1.dp, RoseDanger.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(7.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("নমুনা সেল (১০০-তে ৩৪):", fontSize = 10.5.sp, color = SlateText)
                                Surface(color = Color.White, shape = RoundedCornerShape(5.dp)) {
                                    Text(
                                        ResultRulesEngine.formatTermTotalCell("৩৪", true, rules),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RoseDanger,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            GroupDivider()

            CompactRuleHeader(numberBn = "৩", title = "বিশেষ চাহিদাসম্পন্ন শিক্ষার্থীর ৩৩%", icon = Icons.Rounded.AccessibilityNew)
            RuleSwitchRow(
                label = "বিশেষ শিক্ষার্থীর ৩৩% পাশ নীতি",
                sub = "৩৩% পেয়ে পাশ করলে স্বাভাবিকের আগেও রোল/মেধাক্রমে আসতে পারবে",
                checked = rules.specialNeedsRuleEnabled,
                enabled = isAdmin,
                testTag = "rule_3_special_needs_switch",
                onCheckedChange = { checked ->
                    applyRulesUpdate(rules.copy(specialNeedsRuleEnabled = checked))
                }
            )

            GroupDivider()

            CompactRuleHeader(numberBn = "৭", title = "অনুপস্থিত (A) এবং ০ নম্বরের পার্থক্য", icon = Icons.Rounded.FactCheck)
            RuleSwitchRow(
                label = "অনুপস্থিত 'A' থাকলে বিষয় অকৃতকার্য ধরুন",
                sub = "নম্বর ঘরে 'A' এবং প্রাপ্ত '0' আলাদাভাবে সংরক্ষিত থাকে",
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
        }
        }

        // গ্রুপ ২: উত্তীর্ণ ও মেধাক্রম বিন্যাস (শর্ত ৪, ৫, ৮)
        TrackedGroupBox(
            key = "rules_$GROUP_MERIT",
            title = "উত্তীর্ণ ও মেধাক্রম বিন্যাস",
            badgeText = "$meritTierLabel${if (rules.jointMeritEnabled) " · যৌথ" else ""}",
            icon = Icons.Rounded.Leaderboard,
            expanded = GROUP_MERIT in expandedGroups,
            contentRootY = contentRootY,
            trackedGroups = trackedGroups,
            onCollapse = { expandedGroups = expandedGroups - GROUP_MERIT }
        ) {
        RuleGroupCard(
            title = "উত্তীর্ণ ও মেধাক্রম বিন্যাস",
            subtitle = "শর্ত ৪ (উত্তীর্ণ) · শর্ত ৫ (অনুত্তীর্ণ ক্রম: $meritTierLabel) · শর্ত ৮ (যৌথ ও টাই-ব্রেকার)",
            icon = Icons.Rounded.Leaderboard,
            badgeText = "$meritTierLabel${if (rules.jointMeritEnabled) " · যৌথ" else ""}",
            badgeActive = rules.promotionCheckAvgPass || rules.meritTierAllSubPassFirst || rules.meritTierAvgPassEnabled,
            expanded = GROUP_MERIT in expandedGroups,
            headerTestTag = "rules_group_merit",
            onToggle = { toggleGroup(GROUP_MERIT) }
        ) {
            CompactRuleHeader(numberBn = "৪", title = "পরবর্তী শ্রেণিতে উত্তীর্ণ হওয়ার শর্ত", icon = Icons.Rounded.School)
            RuleCheckboxRow(
                label = "গড়ে ৪০% পাস (বিশেষ চাহিদাসম্পন্ন: ৩৩%)",
                sub = "ডিফল্ট অন — গড় ৪০%-এর নিচে থাকলে অনুত্তীর্ণ",
                checked = rules.promotionCheckAvgPass,
                enabled = isAdmin,
                testTag = "rule_4_avg_pass_check",
                onCheckedChange = { checked ->
                    applyRulesUpdate(rules.copy(promotionCheckAvgPass = checked))
                }
            )
            RuleCheckboxRow(
                label = "উপস্থিতি বিবেচনা (ন্যূনতম ${fmt(rules.minAttendancePct)}%)",
                sub = if (attendanceOn) "সক্রিয় — উপস্থিতি কম হলে অনুত্তীর্ণ" else "অন করলে শর্ত ১-এর উপস্থিতির শর্তও অন হবে",
                checked = attendanceOn,
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
                sub = "বন্ধ থাকলে ১–২ বিষয়ে ফেল করলেও গড় ৪০% হলে উত্তীর্ণ",
                checked = rules.promotionCheckAllSubjects,
                enabled = isAdmin,
                testTag = "rule_4_all_subjects_check",
                onCheckedChange = { checked ->
                    applyRulesUpdate(rules.copy(promotionCheckAllSubjects = checked))
                }
            )

            GroupDivider()

            CompactRuleHeader(numberBn = "৫", title = "অনুত্তীর্ণ শিক্ষার্থী ও মেধাক্রম স্তর", icon = Icons.Rounded.Leaderboard)
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
                label = "২) গড়ে ৪০% পেয়ে পাশ (৪০%-এর নিচের আগে)",
                checked = rules.meritTierAvgPassEnabled,
                enabled = isAdmin,
                testTag = "rule_5_avg_pass_switch",
                onCheckedChange = { checked ->
                    applyRulesUpdate(rules.copy(meritTierAvgPassEnabled = checked))
                }
            )
            val flowPreview = when {
                rules.meritTierAllSubPassFirst && rules.meritTierAvgPassEnabled ->
                    "ক্রম: ① সব বিষয়ে ৪০%+ → ② গড়ে ৪০%+ → ③ গড়ে ৪০%-এর নিচে"
                rules.meritTierAllSubPassFirst && !rules.meritTierAvgPassEnabled ->
                    "ক্রম: ① সব বিষয়ে ৪০%+ → ② অবশিষ্ট সবাই (নম্বর অনুসারে)"
                !rules.meritTierAllSubPassFirst && rules.meritTierAvgPassEnabled ->
                    "ক্রম: ① গড়ে ৪০%+ → ② ৪০%-এর নিচে"
                else ->
                    "ক্রম: শুধু মোট প্রাপ্ত নম্বরের সিরিয়াল"
            }
            Surface(
                color = TealSoft,
                border = BorderStroke(1.dp, TealContainer),
                shape = RoundedCornerShape(7.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    flowPreview,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TealDark,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }

            GroupDivider()

            CompactRuleHeader(numberBn = "৮", title = "যৌথ মেধাক্রম ও সমান নম্বরে টাই-ব্রেকার", icon = Icons.Rounded.EmojiEvents)
            RuleSwitchRow(
                label = "মেধাক্রম যৌথ হতে পারে (সমান অবস্থানে একই স্থান)",
                checked = rules.jointMeritEnabled,
                enabled = isAdmin,
                testTag = "rule_8_joint_merit_switch",
                onCheckedChange = { checked ->
                    applyRulesUpdate(rules.copy(jointMeritEnabled = checked))
                }
            )
            RuleCheckboxRow(
                label = "উপস্থিতি বিবেচ্য (বেশি উপস্থিতি এগিয়ে)",
                sub = "নম্বর সমান হলে শ্রেণি উপস্থিতির হার যাচাই হবে",
                checked = rules.tieBreakAttendance,
                enabled = isAdmin,
                testTag = "rule_8_tie_attendance_check",
                onCheckedChange = { checked ->
                    applyRulesUpdate(rules.copy(tieBreakAttendance = checked))
                }
            )
            RuleCheckboxRow(
                label = "গুণাবলি বিবেচ্য (ক > খ > গ)",
                sub = "সক্রিয়তা, শৃঙ্খলা ও আচরণে 'ক' অর্জনকারী এগিয়ে",
                checked = rules.tieBreakTraits,
                enabled = isAdmin,
                testTag = "rule_8_tie_traits_check",
                onCheckedChange = { checked ->
                    applyRulesUpdate(rules.copy(tieBreakTraits = checked))
                }
            )
            RuleCheckboxRow(
                label = "পূর্বের রোল বিবেচ্য (ডিফল্ট অন)",
                sub = "পূর্বের ছোট রোল এগিয়ে থাকবে (অফ করলে সতর্কতা দেখাবে)",
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
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "টিপস: সত্যিকারের যৌথ মেধাক্রম (১, ১, ৩) পেতে 'পূর্বের রোল বিবেচ্য' অফ করুন।",
                        fontSize = 10.5.sp,
                        color = AmberNotice,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
        }
        }

        // গ্রুপ ৩: উপস্থিতি ও প্রান্তিক গড় হিসাব (শর্ত ১ ও ৬)
        TrackedGroupBox(
            key = "rules_$GROUP_ATTENDANCE_TERM",
            title = "উপস্থিতি ও প্রান্তিক গড়",
            badgeText = "${bn(listOf(rules.attendanceRuleEnabled, rules.dynamicTermAveraging).count { it }.toString())}/২ অন",
            icon = Icons.Rounded.DateRange,
            expanded = GROUP_ATTENDANCE_TERM in expandedGroups,
            contentRootY = contentRootY,
            trackedGroups = trackedGroups,
            onCollapse = { expandedGroups = expandedGroups - GROUP_ATTENDANCE_TERM }
        ) {
        RuleGroupCard(
            title = "উপস্থিতি ও প্রান্তিক গড়",
            subtitle = "শর্ত ১ (${if (rules.attendanceRuleEnabled) "${fmt(rules.minAttendancePct)}% অন" else "অফ"}) · শর্ত ৬ (${if (rules.dynamicTermAveraging) "১/২/৩ প্রান্তিক গড়" else "স্থির ÷৩"})",
            icon = Icons.Rounded.DateRange,
            badgeText = "${bn(listOf(rules.attendanceRuleEnabled, rules.dynamicTermAveraging).count { it }.toString())}/২ অন",
            badgeActive = rules.attendanceRuleEnabled || rules.dynamicTermAveraging,
            expanded = GROUP_ATTENDANCE_TERM in expandedGroups,
            headerTestTag = "rules_group_attendance_terms",
            onToggle = { toggleGroup(GROUP_ATTENDANCE_TERM) }
        ) {
            CompactRuleHeader(numberBn = "১", title = "ন্যূনতম উপস্থিতির শর্ত (${fmt(rules.minAttendancePct)}%)", icon = Icons.Rounded.FactCheck)
            RuleSwitchRow(
                label = "ন্যূনতম উপস্থিতির হার যাচাই",
                sub = "ডিফল্ট অফ; অন করলে শর্ত ৪-এও উপস্থিতি যাচাই চালু হয়",
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
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = attendanceInputText,
                    onValueChange = { raw ->
                        attendanceInputText = fromBn(raw).filter { it.isDigit() || it == '.' }
                    },
                    enabled = isAdmin,
                    label = { Text("উপস্থিতির হার (%)", fontSize = 11.sp) },
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
                    shape = RoundedCornerShape(9.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("rule_1_attendance_save_btn")
                ) {
                    Text("নির্ধারণ", fontSize = 12.sp)
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf(75.0, 80.0, 85.0, 90.0).forEach { preset ->
                    val selected = kotlin.math.abs(rules.minAttendancePct - preset) < 0.01
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (isAdmin) {
                                attendanceInputText = fmtEn(preset)
                                applyRulesUpdate(rules.copy(minAttendancePct = preset))
                            }
                        },
                        enabled = isAdmin,
                        label = { Text("${fmt(preset)}%", fontSize = 11.sp) },
                        colors = appFilterChipColors(),
                        border = appFilterChipBorder(selected)
                    )
                }
            }

            GroupDivider()

            CompactRuleHeader(numberBn = "৬", title = "প্রান্তিকভিত্তিক ফলাফল ও গড় হিসাব", icon = Icons.Rounded.DateRange)
            RuleSwitchRow(
                label = "১, ২ বা ৩ প্রান্তিকের ভিত্তিতে গড় হিসাব",
                sub = "১ বা ২ প্রান্তিক দিলে সে অনুযায়ী গড়; ৩ প্রান্তিক পূর্ণ হলে ফাইনাল",
                checked = rules.dynamicTermAveraging,
                enabled = isAdmin,
                testTag = "rule_6_dynamic_term_switch",
                onCheckedChange = { checked ->
                    val nextIgnored = if (checked) {
                        rules.ignoredGuidelineIds - "GOVT_4_2_PROGRESS"
                    } else {
                        rules.ignoredGuidelineIds + "GOVT_4_2_PROGRESS"
                    }
                    applyRulesUpdate(
                        rules.copy(
                            dynamicTermAveraging = checked,
                            ignoredGuidelineIds = nextIgnored
                        )
                    )
                }
            )
        }
        }

        // গ্রুপ ৪: সরকারি মূল্যায়ন নির্দেশিকা (৭টি ধারা)
        TrackedGroupBox(
            key = "rules_$GROUP_GUIDELINES",
            title = "সরকারি মূল্যায়ন নির্দেশিকা",
            badgeText = "${bn(activeGuidelineCount.toString())}/${bn(OFFICIAL_EVAL_GUIDELINES.size.toString())} সচল",
            icon = Icons.Rounded.Gavel,
            expanded = GROUP_GUIDELINES in expandedGroups,
            contentRootY = contentRootY,
            trackedGroups = trackedGroups,
            onCollapse = { expandedGroups = expandedGroups - GROUP_GUIDELINES }
        ) {
        RuleGroupCard(
            title = "সরকারি মূল্যায়ন নির্দেশিকা",
            subtitle = "শিরোনামে ট্যাপে বিস্তারিত · 'এই শর্ত এপ্লাই হবে না' টিক দেওয়া যাবে",
            icon = Icons.Rounded.Gavel,
            badgeText = "${bn(activeGuidelineCount.toString())}/${bn(OFFICIAL_EVAL_GUIDELINES.size.toString())} সচল",
            badgeActive = ignoredCount == 0,
            badgeWarning = ignoredCount > 0,
            expanded = GROUP_GUIDELINES in expandedGroups,
            headerTestTag = "rules_group_guidelines",
            onToggle = { toggleGroup(GROUP_GUIDELINES) }
        ) {
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
                        if (isIgnored) AmberBorder else SlateOutline.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("guideline_card_${item.id}")
                ) {
                    Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 40.dp)
                                .clickable {
                                    expandedGuidelineId = if (isExpanded) null else item.id
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = if (isIgnored) AmberBorder.copy(alpha = 0.35f) else TealContainer,
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                Text(
                                    item.sectionLabelBn,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIgnored) AmberNotice else TealDark,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.shortTitleBn,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isIgnored) SlateMuted else SlateText,
                                    textDecoration = if (isIgnored) TextDecoration.LineThrough else TextDecoration.None,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    item.summaryBn,
                                    fontSize = 10.5.sp,
                                    color = SlateMuted,
                                    maxLines = if (isExpanded) 4 else 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            StatusPill(active = !isIgnored)
                            Icon(
                                imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = "বিস্তারিত",
                                tint = SlateMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                Modifier.padding(top = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                HorizontalDivider(color = SlateOutline.copy(alpha = 0.35f))
                                Text(
                                    item.fullDetailsBn,
                                    fontSize = 11.sp,
                                    color = SlateText,
                                    lineHeight = 16.sp
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        item.linkedRuleHintBn,
                                        fontSize = 10.sp,
                                        color = TealDark,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = isAdmin) { toggleGuidelineIgnored(!isIgnored) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isIgnored,
                                enabled = isAdmin,
                                onCheckedChange = { checked -> toggleGuidelineIgnored(checked) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("guideline_ignore_${item.id}")
                            )
                            Text(
                                "এই শর্ত এপ্লাই হবে না",
                                fontSize = 11.sp,
                                fontWeight = if (isIgnored) FontWeight.Bold else FontWeight.Medium,
                                color = if (isIgnored) AmberNotice else SlateMuted
                            )
                        }
                    }
                }
            }
        }
        }

        if (isAdmin) {
            OutlinedButton(
                onClick = { confirmResetDefaults = true },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("rules_reset_defaults_btn")
            ) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("ডিফল্ট শর্তাবলীতে ফিরে যান", fontSize = 12.sp)
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
private fun SummaryChip(text: String, active: Boolean) {
    Surface(
        color = if (active) TealSoft else SlateBg,
        border = BorderStroke(
            1.dp,
            if (active) TealContainer else SlateOutline.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (active) TealDark else SlateMuted,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun RuleGroupCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeText: String,
    badgeActive: Boolean,
    badgeWarning: Boolean = false,
    expanded: Boolean,
    headerTestTag: String,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            when {
                badgeWarning -> AmberBorder
                expanded || badgeActive -> TealContainer
                else -> SlateOutline.copy(alpha = 0.45f)
            }
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag(headerTestTag),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = when {
                        badgeWarning -> AmberBg
                        badgeActive -> TealSoft
                        else -> SlateBg
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = when {
                                badgeWarning -> AmberNotice
                                badgeActive -> TealPrimary
                                else -> SlateMuted
                            },
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SlateText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        subtitle,
                        fontSize = 10.5.sp,
                        color = SlateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = when {
                        badgeWarning -> AmberBg
                        badgeActive -> TealContainer
                        else -> SlateBg
                    },
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        badgeText,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            badgeWarning -> AmberNotice
                            badgeActive -> TealDark
                            else -> SlateMuted
                        },
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "সংকুচিত করুন" else "বিস্তারিত খুলুন",
                    tint = SlateMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    HorizontalDivider(color = SlateOutline.copy(alpha = 0.35f))
                    content()
                }
            }
        }
    }
}

@Composable
private fun CompactRuleHeader(
    numberBn: String,
    title: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.padding(top = 2.dp)
    ) {
        Icon(icon, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(14.dp))
        Text(
            "$numberBn। $title",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = TealDark
        )
    }
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(
        color = SlateOutline.copy(alpha = 0.28f),
        modifier = Modifier.padding(vertical = 3.dp)
    )
}

@Composable
private fun StatusPill(active: Boolean) {
    Surface(
        color = if (active) TealSoft else RoseBg,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            if (active) "অন" else "অফ",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) TealDark else RoseDanger,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun RuleSwitchRow(
    label: String,
    sub: String? = null,
    checked: Boolean,
    enabled: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) SlateText else SlateMuted
            )
            if (!sub.isNullOrBlank()) {
                Text(
                    sub,
                    fontSize = 10.sp,
                    color = SlateMuted,
                    lineHeight = 13.5.sp
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                color = if (checked) TealSoft else SlateBg,
                border = BorderStroke(1.dp, if (checked) TealPrimary.copy(alpha = 0.45f) else SlateOutline.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = if (checked) "অন" else "অফ",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (checked) TealDark else SlateMuted,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                modifier = Modifier
                    .scale(0.88f)
                    .minimumInteractiveComponentSize()
                    .testTag(testTag)
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
            .defaultMinSize(minHeight = 48.dp)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag(testTag)
        )
        Column(Modifier.weight(1f).padding(start = 2.dp)) {
            Text(
                label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) SlateText else SlateMuted
            )
            Text(
                sub,
                fontSize = 10.sp,
                color = SlateMuted,
                lineHeight = 13.5.sp
            )
        }
    }
}
