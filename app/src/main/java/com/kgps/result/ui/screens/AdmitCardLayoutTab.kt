@file:OptIn(ExperimentalMaterial3Api::class)

package com.kgps.result.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.kgps.result.AdmitCardConfig
import com.kgps.result.BENGALI_PDF_FONT_LIST
import com.kgps.result.ENGLISH_PDF_FONT_LIST
import com.kgps.result.PdfGen
import com.kgps.result.SeatPlanBorderStyle
import com.kgps.result.SeatPlanBorderWeight
import com.kgps.result.SeatPlanNumeralMode
import com.kgps.result.bn
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdmitSectionGroupCard(title = "পৃষ্ঠায় কার্ড সংখ্যা ও ফাঁক (A4)") {
            AdmitSegmentedOptionBar(
                items = listOf(2, 3, 4, 5).map { it to "${bn(it.toString())}টি" },
                selected = config.safeCardsPerPage,
                onSelect = { onUpdateConfig(config.copy(cardsPerPage = it)) },
                tagPrefix = "admit_card_per_page"
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AdmitCompactPickerBox(
                    label = "পৃষ্ঠা মার্জিন",
                    valueText = formatAdmitInchesBn(config.safeMarginInches),
                    options = ADMIT_MARGIN_INCH_OPTIONS,
                    selectedValue = config.safeMarginInches,
                    optionLabel = { formatAdmitInchesBn(it) + if (abs(it - 0.28f) < 0.01f) " (ডিফল্ট)" else "" },
                    onSelect = { onUpdateConfig(config.copy(marginInches = it)) },
                    modifier = Modifier.weight(1f),
                    testTag = "admit_card_margin_picker"
                )
                AdmitCompactPickerBox(
                    label = "কার্ডের মাঝের ফাঁক",
                    valueText = formatAdmitPtBn(config.safeVerticalGapPt),
                    options = ADMIT_GAP_PT_OPTIONS,
                    selectedValue = config.safeVerticalGapPt,
                    optionLabel = { formatAdmitPtBn(it) },
                    onSelect = { onUpdateConfig(config.copy(verticalGapPt = it)) },
                    modifier = Modifier.weight(1f),
                    testTag = "admit_card_gap_picker"
                )
            }
        }

        AdmitSectionGroupCard(title = "ফন্ট ও বর্ডার") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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

            AdmitSegmentedOptionBar(
                items = SeatPlanBorderStyle.entries.map { it to it.labelBn },
                selected = config.borderStyle,
                onSelect = { onUpdateConfig(config.copy(borderStyle = it)) },
                tagPrefix = "admit_card_border"
            )
            if (config.borderStyle != SeatPlanBorderStyle.NONE) {
                AdmitSegmentedOptionBar(
                    items = SeatPlanBorderWeight.entries.map { it to it.labelBn },
                    selected = config.borderWeight,
                    onSelect = { onUpdateConfig(config.copy(borderWeight = it)) },
                    tagPrefix = "admit_card_border_weight"
                )
                AdmitCompactPickerBox(
                    label = "কোনার গোলাকার",
                    valueText = if (config.safeCornerRadiusPt < 0.5f) "০ pt (সোজা)" else formatAdmitPtBn(config.safeCornerRadiusPt),
                    options = ADMIT_CORNER_RADIUS_OPTIONS,
                    selectedValue = config.safeCornerRadiusPt,
                    optionLabel = { r -> if (r < 0.5f) "০ pt (সোজা)" else formatAdmitPtBn(r) },
                    onSelect = { onUpdateConfig(config.copy(cornerRadiusPt = it)) },
                    testTag = "admit_card_corner_picker"
                )
            }
        }

        AdmitSectionGroupCard(title = "কার্ড বিন্যাস ও সংখ্যা") {
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
            HorizontalDivider(color = SlateOutline.copy(alpha = 0.4f))
            AdmitCompactSwitchRow(
                label = "কার্ডের মাঝে কাটার দাগ",
                checked = config.showCutLines,
                onCheckedChange = { onUpdateConfig(config.copy(showCutLines = it)) },
                testTag = "admit_card_opt_cut_lines"
            )
            HorizontalDivider(color = SlateOutline.copy(alpha = 0.4f))
            AdmitCompactSwitchRow(
                label = "মাঝে লম্বা ড্যাশড বিভাজক",
                checked = config.showCenterDivider,
                onCheckedChange = { onUpdateConfig(config.copy(showCenterDivider = it)) },
                testTag = "admit_card_opt_center_divider"
            )
            HorizontalDivider(color = SlateOutline.copy(alpha = 0.4f))
            AdmitCompactSwitchRow(
                label = "০১ রোল (২ ডিজিট)",
                checked = config.padRollTwoDigits,
                onCheckedChange = { onUpdateConfig(config.copy(padRollTwoDigits = it)) },
                testTag = "admit_card_opt_pad_roll"
            )
            HorizontalDivider(color = SlateOutline.copy(alpha = 0.4f))
            AdmitCompactSwitchRow(
                label = "ডান পাশে পরীক্ষার রুটিন টেবিল দেখাও",
                checked = config.showRoutineTable,
                onCheckedChange = { onUpdateConfig(config.copy(showRoutineTable = it)) },
                testTag = "admit_card_opt_show_routine"
            )
        }
    }
}
