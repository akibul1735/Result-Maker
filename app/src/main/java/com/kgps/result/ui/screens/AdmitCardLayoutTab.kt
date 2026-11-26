@file:OptIn(ExperimentalMaterial3Api::class)

package com.kgps.result.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.AdmitCardConfig
import com.kgps.result.BENGALI_PDF_FONT_LIST
import com.kgps.result.ENGLISH_PDF_FONT_LIST
import com.kgps.result.PdfGen
import com.kgps.result.SeatPlanBorderStyle
import com.kgps.result.SeatPlanBorderWeight
import com.kgps.result.SeatPlanNumeralMode
import com.kgps.result.bn
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import kotlin.math.abs

@Composable
internal fun AdmitLayoutAndStyleTab(
    ctx: Context,
    config: AdmitCardConfig,
    activeBnFontName: String,
    activeEnFontName: String,
    onUpdateConfig: (AdmitCardConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // 1. Page, Spacing & Fonts
        AdmitSectionGroupCard(title = "১. পৃষ্ঠা, ফাঁকা স্থান ও ফন্ট পরিবার") {
            AdmitSegmentedOptionBar(
                items = listOf(2, 3, 4, 5).map { it to "${bn(it.toString())}টি/পৃষ্ঠা" },
                selected = config.safeCardsPerPage,
                onSelect = { onUpdateConfig(config.copy(cardsPerPage = it)) },
                tagPrefix = "admit_card_per_page"
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AdmitCompactPickerBox(
                    label = "মার্জিন",
                    valueText = formatAdmitInchesBn(config.safeMarginInches),
                    options = ADMIT_MARGIN_INCH_OPTIONS,
                    selectedValue = config.safeMarginInches,
                    optionLabel = { formatAdmitInchesBn(it) + if (abs(it - 0.28f) < 0.01f) " (ডিফল্ট)" else "" },
                    onSelect = { onUpdateConfig(config.copy(marginInches = it)) },
                    modifier = Modifier.weight(1f),
                    testTag = "admit_card_margin_picker"
                )
                AdmitCompactPickerBox(
                    label = "কার্ড ফাঁক",
                    valueText = formatAdmitPtBn(config.safeVerticalGapPt),
                    options = ADMIT_GAP_PT_OPTIONS,
                    selectedValue = config.safeVerticalGapPt,
                    optionLabel = { formatAdmitPtBn(it) },
                    onSelect = { onUpdateConfig(config.copy(verticalGapPt = it)) },
                    modifier = Modifier.weight(1f),
                    testTag = "admit_card_gap_picker"
                )
                AdmitCompactPickerBox(
                    label = "হেড-তথ্য ফাঁক",
                    valueText = formatAdmitPtBn(config.safeHeaderToInfoGapPt),
                    options = ADMIT_HEADER_INFO_GAP_OPTIONS,
                    selectedValue = config.safeHeaderToInfoGapPt,
                    optionLabel = { g -> formatAdmitPtBn(g) + if (abs(g - 15.5f) < 0.2f) " (ডিফল্ট)" else "" },
                    onSelect = { onUpdateConfig(config.copy(headerToInfoGapPt = it)) },
                    modifier = Modifier.weight(1.15f),
                    testTag = "admit_card_header_info_gap_picker"
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AdmitCompactPickerBox(
                    label = "বাংলা ফন্ট",
                    valueText = activeBnFontName,
                    options = BENGALI_PDF_FONT_LIST.map { it.id },
                    selectedValue = config.bnFontId,
                    optionLabel = { id ->
                        BENGALI_PDF_FONT_LIST.find { it.id == id }?.labelBn?.substringBefore(" (") ?: id
                    },
                    onSelect = { onUpdateConfig(config.copy(bnFontId = it)) },
                    isFontDropdown = true,
                    modifier = Modifier.weight(1.1f),
                    testTag = "admit_card_bn_font_btn",
                    optionFontFamily = { id ->
                        FontFamily(androidx.compose.ui.text.font.Typeface(PdfGen.resolveTypeface(ctx, id, false)))
                    }
                )
                AdmitCompactPickerBox(
                    label = "ইংরেজি ফন্ট",
                    valueText = activeEnFontName,
                    options = ENGLISH_PDF_FONT_LIST.map { it.id },
                    selectedValue = config.enFontId,
                    optionLabel = { id ->
                        ENGLISH_PDF_FONT_LIST.find { it.id == id }?.labelBn?.substringBefore(" (") ?: id
                    },
                    onSelect = { onUpdateConfig(config.copy(enFontId = it)) },
                    isFontDropdown = true,
                    modifier = Modifier.weight(0.95f),
                    testTag = "admit_card_en_font_btn",
                    optionFontFamily = { id ->
                        FontFamily(androidx.compose.ui.text.font.Typeface(PdfGen.resolveTypeface(ctx, id, false)))
                    }
                )
            }
        }

        // 2. Header & Headline Badge Typography
        AdmitSectionGroupCard(title = "২. উপরের হেডার ও প্রবেশপত্র শিরোনাম") {
            Text(
                "বিদ্যালয়, ঠিকানা ও পরীক্ষা",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                color = SlateMuted
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AdmitCompactPickerBox(
                    label = "হেডার সাইজ",
                    valueText = formatAdmitScalePctBn(config.safeHeaderFontScale),
                    options = ADMIT_FONT_SCALE_OPTIONS,
                    selectedValue = config.safeHeaderFontScale,
                    optionLabel = { formatAdmitScalePctBn(it) },
                    onSelect = { onUpdateConfig(config.copy(headerFontScale = it)) },
                    modifier = Modifier.weight(1f),
                    testTag = "admit_header_size_picker"
                )
                AdmitCompactTogglePill(
                    label = "বোল্ড",
                    checked = config.headerBold,
                    onCheckedChange = { onUpdateConfig(config.copy(headerBold = it)) },
                    modifier = Modifier.weight(0.85f),
                    testTag = "admit_header_bold_toggle"
                )
            }

            HorizontalDivider(color = SlateOutline.copy(alpha = 0.35f))
            Text(
                "প্রবেশপত্র শিরোনাম (হেড লাইন)",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                color = SlateMuted
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AdmitCompactPickerBox(
                    label = "শিরোনাম সাইজ",
                    valueText = formatAdmitScalePctBn(config.safeBadgeFontScale),
                    options = ADMIT_FONT_SCALE_OPTIONS,
                    selectedValue = config.safeBadgeFontScale,
                    optionLabel = { formatAdmitScalePctBn(it) },
                    onSelect = { onUpdateConfig(config.copy(badgeFontScale = it)) },
                    modifier = Modifier.weight(1f),
                    testTag = "admit_badge_size_picker"
                )
                AdmitCompactTogglePill(
                    label = "বোল্ড",
                    checked = config.badgeBold,
                    onCheckedChange = { onUpdateConfig(config.copy(badgeBold = it)) },
                    modifier = Modifier.weight(0.75f),
                    testTag = "admit_badge_bold_toggle"
                )
                AdmitCompactTogglePill(
                    label = "নিচে দাগ",
                    checked = config.badgeUnderline,
                    onCheckedChange = { onUpdateConfig(config.copy(badgeUnderline = it)) },
                    modifier = Modifier.weight(0.85f),
                    testTag = "admit_badge_underline_toggle"
                )
            }
        }

        // 3. Student Info Typography (নাম, শ্রেণি, রোল)
        AdmitSectionGroupCard(title = "৩. শিক্ষার্থীর তথ্য (নাম, শ্রেণি, রোল)") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AdmitCompactPickerBox(
                    label = "তথ্য সাইজ",
                    valueText = formatAdmitScalePctBn(config.safeStudentInfoFontScale),
                    options = ADMIT_FONT_SCALE_OPTIONS,
                    selectedValue = config.safeStudentInfoFontScale,
                    optionLabel = { formatAdmitScalePctBn(it) },
                    onSelect = { onUpdateConfig(config.copy(studentInfoFontScale = it)) },
                    modifier = Modifier.weight(1f),
                    testTag = "admit_info_size_picker"
                )
                AdmitCompactTogglePill(
                    label = "লেবেল বোল্ড",
                    checked = config.studentLabelBold,
                    onCheckedChange = { onUpdateConfig(config.copy(studentLabelBold = it)) },
                    modifier = Modifier.weight(0.9f),
                    testTag = "admit_info_label_bold_toggle"
                )
                AdmitCompactTogglePill(
                    label = "নাম/রোল বোল্ড",
                    checked = config.studentValueBold,
                    onCheckedChange = { onUpdateConfig(config.copy(studentValueBold = it)) },
                    modifier = Modifier.weight(1f),
                    testTag = "admit_info_value_bold_toggle"
                )
            }
        }

        // 4. Routine Table & Signature Title Typography
        AdmitSectionGroupCard(title = "৪. রুটিন টেবিল ও স্বাক্ষর পদবি") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AdmitCompactPickerBox(
                    label = "রুটিন সাইজ",
                    valueText = formatAdmitScalePctBn(config.safeRoutineFontScale),
                    options = ADMIT_FONT_SCALE_OPTIONS,
                    selectedValue = config.safeRoutineFontScale,
                    optionLabel = { formatAdmitScalePctBn(it) },
                    onSelect = { onUpdateConfig(config.copy(routineFontScale = it)) },
                    modifier = Modifier.weight(1f),
                    testTag = "admit_routine_size_picker"
                )
                AdmitCompactTogglePill(
                    label = "হেডার বোল্ড",
                    checked = config.routineHeaderBold,
                    onCheckedChange = { onUpdateConfig(config.copy(routineHeaderBold = it)) },
                    modifier = Modifier.weight(0.95f),
                    testTag = "admit_routine_hdr_bold_toggle"
                )
                AdmitCompactTogglePill(
                    label = "বিষয় বোল্ড",
                    checked = config.routineBodyBold,
                    onCheckedChange = { onUpdateConfig(config.copy(routineBodyBold = it)) },
                    modifier = Modifier.weight(0.95f),
                    testTag = "admit_routine_body_bold_toggle"
                )
            }
            HorizontalDivider(color = SlateOutline.copy(alpha = 0.35f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AdmitCompactPickerBox(
                    label = "স্বাক্ষর পদবি সাইজ",
                    valueText = formatAdmitScalePctBn(config.safeSignatureTitleFontScale),
                    options = ADMIT_FONT_SCALE_OPTIONS,
                    selectedValue = config.safeSignatureTitleFontScale,
                    optionLabel = { formatAdmitScalePctBn(it) },
                    onSelect = { onUpdateConfig(config.copy(signatureTitleFontScale = it)) },
                    modifier = Modifier.weight(1f),
                    testTag = "admit_sig_title_size_picker"
                )
                AdmitCompactTogglePill(
                    label = "স্বাক্ষর পদবি বোল্ড",
                    checked = config.signatureTitleBold,
                    onCheckedChange = { onUpdateConfig(config.copy(signatureTitleBold = it)) },
                    modifier = Modifier.weight(1f),
                    testTag = "admit_sig_title_bold_toggle"
                )
            }
        }

        // 5. Border & General Switches
        AdmitSectionGroupCard(title = "৫. বর্ডার ও অন্যান্য অপশন") {
            AdmitSegmentedOptionBar(
                items = SeatPlanBorderStyle.entries.map { it to it.labelBn },
                selected = config.borderStyle,
                onSelect = { onUpdateConfig(config.copy(borderStyle = it)) },
                tagPrefix = "admit_card_border"
            )
            if (config.borderStyle != SeatPlanBorderStyle.NONE) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AdmitSegmentedOptionBar(
                        items = SeatPlanBorderWeight.entries.map { it to it.labelBn },
                        selected = config.borderWeight,
                        onSelect = { onUpdateConfig(config.copy(borderWeight = it)) },
                        modifier = Modifier.weight(1.35f),
                        tagPrefix = "admit_card_border_weight"
                    )
                    AdmitCompactPickerBox(
                        label = "কোনার বাঁক",
                        valueText = if (config.safeCornerRadiusPt < 0.5f) "০ pt" else formatAdmitPtBn(config.safeCornerRadiusPt),
                        options = ADMIT_CORNER_RADIUS_OPTIONS,
                        selectedValue = config.safeCornerRadiusPt,
                        optionLabel = { r -> if (r < 0.5f) "০ pt (সোজা)" else formatAdmitPtBn(r) },
                        onSelect = { onUpdateConfig(config.copy(cornerRadiusPt = it)) },
                        modifier = Modifier.weight(0.75f),
                        testTag = "admit_card_corner_picker"
                    )
                }
            }
            AdmitCompactSwitchRow(
                label = "সংখ্যা বাংলা (০১/১২/২০২৬)",
                checked = config.numeralMode == SeatPlanNumeralMode.BENGALI,
                onCheckedChange = { bnOn ->
                    onUpdateConfig(
                        config.copy(numeralMode = if (bnOn) SeatPlanNumeralMode.BENGALI else SeatPlanNumeralMode.ENGLISH)
                    )
                },
                testTag = "admit_card_opt_bn_num"
            )
            HorizontalDivider(color = SlateOutline.copy(alpha = 0.35f))
            AdmitCompactSwitchRow(
                label = "কার্ডের মাঝে কাটার দাগ",
                checked = config.showCutLines,
                onCheckedChange = { onUpdateConfig(config.copy(showCutLines = it)) },
                testTag = "admit_card_opt_cut_lines"
            )
            HorizontalDivider(color = SlateOutline.copy(alpha = 0.35f))
            AdmitCompactSwitchRow(
                label = "মাঝে লম্বা ড্যাশড বিভাজক",
                checked = config.showCenterDivider,
                onCheckedChange = { onUpdateConfig(config.copy(showCenterDivider = it)) },
                testTag = "admit_card_opt_center_divider"
            )
            HorizontalDivider(color = SlateOutline.copy(alpha = 0.35f))
            AdmitCompactSwitchRow(
                label = "০১ রোল (২ ডিজিট)",
                checked = config.padRollTwoDigits,
                onCheckedChange = { onUpdateConfig(config.copy(padRollTwoDigits = it)) },
                testTag = "admit_card_opt_pad_roll"
            )
            HorizontalDivider(color = SlateOutline.copy(alpha = 0.35f))
            AdmitCompactSwitchRow(
                label = "ডান পাশে পরীক্ষার রুটিন টেবিল দেখাও",
                checked = config.showRoutineTable,
                onCheckedChange = { onUpdateConfig(config.copy(showRoutineTable = it)) },
                testTag = "admit_card_opt_show_routine"
            )
        }
    }
}
