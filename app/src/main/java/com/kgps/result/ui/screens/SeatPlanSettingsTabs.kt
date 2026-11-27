@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.kgps.result.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.BorderAll
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.BENGALI_PDF_FONT_LIST
import com.kgps.result.ENGLISH_PDF_FONT_LIST
import com.kgps.result.PdfGen
import com.kgps.result.SeatPlanBorderStyle
import com.kgps.result.SeatPlanBorderWeight
import com.kgps.result.SeatPlanConfig
import com.kgps.result.SeatPlanNumeralMode
import com.kgps.result.SeatPlanOrientation
import com.kgps.result.SeatPlanPairMode
import com.kgps.result.SeatPlanPlanner
import com.kgps.result.SeatPlanRollOrder
import com.kgps.result.Settings
import com.kgps.result.bn
import com.kgps.result.classNameBn
import com.kgps.result.classShortBn
import com.kgps.result.fromBn
import com.kgps.result.termNameBn
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealPrimary
import java.util.Locale
import kotlin.math.abs

private val SEAT_PART_FONT_SIZES = listOf(
    7.5f, 8.0f, 8.5f, 9.0f, 9.5f, 10.0f, 10.5f, 11.0f, 11.5f, 12.0f, 12.5f, 13.0f, 14.0f, 15.0f, 16.0f, 17.0f, 18.0f
)
private val SEAT_MARGIN_INCH_OPTIONS = listOf(0.10f, 0.15f, 0.20f, 0.25f, 0.30f, 0.35f, 0.40f, 0.45f, 0.50f, 0.60f, 0.75f, 1.00f)
private val SEAT_GAP_PT_OPTIONS = listOf(0f, 4f, 6f, 8f, 10f, 12f, 14f, 16f, 20f, 24f, 28f, 32f, 36f)
private val SEAT_CORNER_RADIUS_OPTIONS = listOf(0f, 4f, 8f, 12f, 15f, 18f, 20f, 22f, 25f, 28f)

private fun formatSeatInchesBn(inches: Float): String =
    bn(String.format(Locale.US, "%.2f", inches)) + "\""

@Composable
internal fun SeatPlanClassAndRollTab(
    config: SeatPlanConfig,
    settings: Settings,
    allowedClasses: List<Int>,
    countByClass: Map<Int, Int>,
    rollFromText: String,
    rollToText: String,
    onUpdateConfig: (SeatPlanConfig) -> Unit,
    onRollFromChange: (String) -> Unit,
    onRollToChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AdmitSectionGroupCard(title = "আসন মোড ও শ্রেণি নির্বাচন") {
            AdmitSegmentedOptionBar(
                items = SeatPlanPairMode.entries.map { it to it.labelBn },
                selected = config.pairMode,
                onSelect = { mode ->
                    val nextCols = if (mode == SeatPlanPairMode.SIDE_BY_SIDE && config.columns < 2) 2 else config.columns
                    onUpdateConfig(config.copy(pairMode = mode, columns = nextCols))
                },
                tagPrefix = "seat_plan_mode"
            )

            if (config.pairMode == SeatPlanPairMode.SEQUENTIAL) {
                val allSelected = allowedClasses.isNotEmpty() &&
                    allowedClasses.all { it in config.selectedClasses }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Surface(
                        onClick = {
                            onUpdateConfig(
                                if (allSelected) {
                                    config.copy(selectedClasses = listOfNotNull(allowedClasses.firstOrNull()))
                                } else {
                                    config.copy(selectedClasses = allowedClasses)
                                }
                            )
                        },
                        color = if (allSelected) TealPrimary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (allSelected) TealPrimary else SlateOutline),
                        shape = RoundedCornerShape(7.dp),
                        modifier = Modifier.heightIn(min = 30.dp).testTag("seat_plan_cls_all")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text(
                                "সব",
                                fontSize = 11.sp,
                                fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (allSelected) Color.White else SlateText
                            )
                        }
                    }

                    allowedClasses.forEach { c ->
                        val isSel = c in config.selectedClasses
                        val cnt = countByClass[c] ?: 0
                        Surface(
                            onClick = {
                                val next = if (isSel) {
                                    val reduced = config.selectedClasses - c
                                    if (reduced.isEmpty()) listOf(c) else reduced
                                } else {
                                    (config.selectedClasses + c).distinct().sortedBy { allowedClasses.indexOf(it) }
                                }
                                onUpdateConfig(config.copy(selectedClasses = next))
                            },
                            color = if (isSel) TealPrimary else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isSel) TealPrimary else SlateOutline),
                            shape = RoundedCornerShape(7.dp),
                            modifier = Modifier.heightIn(min = 30.dp).testTag("seat_plan_cls_$c")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    "${classShortBn(c, settings)} (${bn(cnt.toString())})",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else SlateText,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AdmitCompactPickerBox(
                        label = "বাম কলাম",
                        valueText = config.leftClass?.let { "${classNameBn(it, settings)} শ্রেণি" } ?: "নির্বাচন",
                        options = allowedClasses,
                        selectedValue = config.leftClass ?: (allowedClasses.firstOrNull() ?: 1),
                        optionLabel = { c -> "${classNameBn(c, settings)} (${bn((countByClass[c] ?: 0).toString())} জন)" },
                        onSelect = { onUpdateConfig(config.copy(leftClass = it)) },
                        modifier = Modifier.weight(1f),
                        testTag = "seat_plan_left_cls_btn"
                    )
                    AdmitCompactPickerBox(
                        label = "ডান কলাম",
                        valueText = config.rightClass?.let { "${classNameBn(it, settings)} শ্রেণি" } ?: "নির্বাচন",
                        options = allowedClasses,
                        selectedValue = config.rightClass ?: (allowedClasses.firstOrNull() ?: 1),
                        optionLabel = { c -> "${classNameBn(c, settings)} (${bn((countByClass[c] ?: 0).toString())} জন)" },
                        onSelect = { onUpdateConfig(config.copy(rightClass = it)) },
                        modifier = Modifier.weight(1f),
                        testTag = "seat_plan_right_cls_btn"
                    )
                }
            }
        }

        AdmitSectionGroupCard(title = "রোল ক্রম, পরিসর ও ফাঁকা কার্ড") {
            AdmitSegmentedOptionBar(
                items = SeatPlanRollOrder.entries.map { it to it.labelBn },
                selected = config.rollOrder,
                onSelect = { onUpdateConfig(config.copy(rollOrder = it)) },
                tagPrefix = "seat_plan_order"
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AdmitCompactTextInput(
                    label = "রোল থেকে",
                    value = rollFromText,
                    onValueChange = { raw ->
                        onRollFromChange(fromBn(raw).filter { it.isDigit() }.take(4))
                    },
                    placeholder = "সব",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                AdmitCompactTextInput(
                    label = "রোল পর্যন্ত",
                    value = rollToText,
                    onValueChange = { raw ->
                        onRollToChange(fromBn(raw).filter { it.isDigit() }.take(4))
                    },
                    placeholder = "সব",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        "অতিরিক্ত ফাঁকা সিট কার্ড",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SlateText,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (config.extraBlankSlips > 0) {
                                onUpdateConfig(config.copy(extraBlankSlips = config.extraBlankSlips - 1))
                            }
                        },
                        enabled = config.extraBlankSlips > 0,
                        modifier = Modifier.size(34.dp).testTag("seat_plan_blank_minus")
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "ফাঁকা কার্ড কমান", modifier = Modifier.size(15.dp))
                    }
                    Text(
                        bn(config.extraBlankSlips.toString()),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateText,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    IconButton(
                        onClick = {
                            if (config.extraBlankSlips < 48) {
                                onUpdateConfig(config.copy(extraBlankSlips = config.extraBlankSlips + 1))
                            }
                        },
                        enabled = config.extraBlankSlips < 48,
                        modifier = Modifier.size(34.dp).testTag("seat_plan_blank_plus")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "ফাঁকা কার্ড বাড়ান", modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun SeatPlanTitleAndRoomTab(
    config: SeatPlanConfig,
    settings: Settings,
    onUpdateConfig: (SeatPlanConfig) -> Unit
) {
    val titleSummary = "${termNameBn(config.term)} · ${SeatPlanPlanner.resolvedSubtitle(config)}"
    val schoolSummary = SeatPlanPlanner.resolvedSchoolName(config, settings)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AdmitCollapsibleGroupCard(
            title = "মূল্যায়ন, শিরোনাম ও কক্ষ",
            summary = titleSummary,
            icon = Icons.Rounded.Badge,
            initiallyExpanded = true,
            testTag = "seat_plan_grp_titles"
        ) {
            Text(
                "প্রান্তিক মূল্যায়ন",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                color = SlateMuted
            )
            AdmitSegmentedOptionBar(
                items = (1..3).map { it to termNameBn(it) },
                selected = config.term,
                onSelect = { onUpdateConfig(config.copy(term = it)) },
                tagPrefix = "seat_plan_term"
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AdmitCompactTextInput(
                    label = "উপ-শিরোনাম",
                    value = config.subtitle,
                    onValueChange = { onUpdateConfig(config.copy(subtitle = it)) },
                    placeholder = "আসন বিন্যাস",
                    modifier = Modifier.weight(1f)
                )
                AdmitCompactTextInput(
                    label = "কক্ষ নং (ঐচ্ছিক)",
                    value = config.roomLabel,
                    onValueChange = { onUpdateConfig(config.copy(roomLabel = it)) },
                    placeholder = "কক্ষ ০১",
                    modifier = Modifier.weight(1f)
                )
            }
            AdmitCompactTextInput(
                label = "কাস্টম পরীক্ষার শিরোনাম",
                value = config.customExamTitle,
                onValueChange = { onUpdateConfig(config.copy(customExamTitle = it)) },
                placeholder = SeatPlanPlanner.resolvedExamTitle(config.copy(customExamTitle = ""), settings)
            )
        }

        AdmitCollapsibleGroupCard(
            title = "বিদ্যালয়ের নাম ও ঠিকানা",
            summary = schoolSummary,
            icon = Icons.Rounded.School,
            initiallyExpanded = false,
            testTag = "seat_plan_grp_school"
        ) {
            AdmitCompactSwitchRow(
                label = "বিদ্যালয়ের ঠিকানা দেখাও",
                checked = config.showSchoolAddress,
                onCheckedChange = { onUpdateConfig(config.copy(showSchoolAddress = it)) },
                testTag = "seat_plan_show_address_switch"
            )
            HorizontalDivider(color = SlateOutline.copy(alpha = 0.4f))
            AdmitCompactTextInput(
                label = "কাস্টম বিদ্যালয়ের নাম",
                value = config.customSchoolName,
                onValueChange = { onUpdateConfig(config.copy(customSchoolName = it)) },
                placeholder = settings.school.ifBlank { "বিদ্যালয়ের নাম" }
            )
            AdmitCompactTextInput(
                label = "কাস্টম ঠিকানা",
                value = config.customSchoolAddress,
                onValueChange = { onUpdateConfig(config.copy(customSchoolAddress = it)) },
                enabled = config.showSchoolAddress,
                placeholder = settings.schoolAddress.ifBlank { "ঠিকানা" }
            )
        }
    }
}

@Composable
internal fun SeatPlanLayoutAndBorderTab(
    config: SeatPlanConfig,
    onUpdateConfig: (SeatPlanConfig) -> Unit
) {
    val gridSummary = "${bn(config.safeColumns.toString())}×${bn(config.safeRows.toString())} · ${config.orientation.labelBn.substringBefore(" ")}"
    val borderSummary = if (config.borderStyle == SeatPlanBorderStyle.NONE) {
        "বর্ডার নেই"
    } else {
        "${config.borderStyle.labelBn} · ${config.borderWeight.labelBn}"
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AdmitCollapsibleGroupCard(
            title = "১. পৃষ্ঠা, গ্রিড ও ফাঁক",
            summary = gridSummary,
            icon = Icons.Rounded.GridView,
            initiallyExpanded = true,
            testTag = "seat_plan_grp_grid"
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val colOptions = if (config.pairMode == SeatPlanPairMode.SIDE_BY_SIDE) listOf(2, 3) else listOf(1, 2, 3)
                AdmitCompactPickerBox(
                    label = "কলাম",
                    valueText = "${bn(config.safeColumns.toString())}টি",
                    options = colOptions,
                    selectedValue = config.safeColumns,
                    optionLabel = { "${bn(it.toString())} কলাম" },
                    onSelect = { onUpdateConfig(config.copy(columns = it)) },
                    modifier = Modifier.weight(1f),
                    testTag = "seat_plan_cols_picker"
                )
                AdmitCompactPickerBox(
                    label = "সারি",
                    valueText = "${bn(config.safeRows.toString())}টি",
                    options = (3..10).toList(),
                    selectedValue = config.safeRows,
                    optionLabel = { "${bn(it.toString())} সারি" },
                    onSelect = { onUpdateConfig(config.copy(rows = it)) },
                    modifier = Modifier.weight(1f),
                    testTag = "seat_plan_rows_picker"
                )
                AdmitCompactPickerBox(
                    label = "মার্জিন",
                    valueText = formatSeatInchesBn(config.safeMarginInches),
                    options = SEAT_MARGIN_INCH_OPTIONS,
                    selectedValue = config.safeMarginInches,
                    optionLabel = { formatSeatInchesBn(it) + if (abs(it - 0.40f) < 0.01f) " (ডিফল্ট)" else "" },
                    onSelect = { onUpdateConfig(config.copy(marginInches = it)) },
                    modifier = Modifier.weight(1.05f),
                    testTag = "seat_plan_margin_picker"
                )
            }
            AdmitSegmentedOptionBar(
                items = listOf(
                    SeatPlanOrientation.PORTRAIT to "পোর্ট্রেট (লম্বা)",
                    SeatPlanOrientation.LANDSCAPE to "ল্যান্ডস্কেপ (আড়াআড়ি)"
                ),
                selected = config.orientation,
                onSelect = { onUpdateConfig(config.copy(orientation = it)) },
                tagPrefix = "seat_plan_orientation"
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AdmitCompactPickerBox(
                    label = "অনুভূমিক ফাঁক",
                    valueText = formatAdmitPtBn(config.safeHorizontalGapPt),
                    options = SEAT_GAP_PT_OPTIONS,
                    selectedValue = config.safeHorizontalGapPt,
                    optionLabel = { formatAdmitPtBn(it) },
                    onSelect = { onUpdateConfig(config.copy(horizontalGapPt = it)) },
                    modifier = Modifier.weight(1f),
                    testTag = "seat_plan_hgap_picker"
                )
                AdmitCompactPickerBox(
                    label = "উল্লম্ব ফাঁক",
                    valueText = formatAdmitPtBn(config.safeVerticalGapPt),
                    options = SEAT_GAP_PT_OPTIONS,
                    selectedValue = config.safeVerticalGapPt,
                    optionLabel = { formatAdmitPtBn(it) },
                    onSelect = { onUpdateConfig(config.copy(verticalGapPt = it)) },
                    modifier = Modifier.weight(1f),
                    testTag = "seat_plan_vgap_picker"
                )
            }
        }

        AdmitCollapsibleGroupCard(
            title = "২. বর্ডার, কাটার দাগ ও সংখ্যা",
            summary = borderSummary,
            icon = Icons.Rounded.BorderAll,
            initiallyExpanded = false,
            testTag = "seat_plan_grp_border_opts"
        ) {
            AdmitSegmentedOptionBar(
                items = SeatPlanBorderStyle.entries.map { it to it.labelBn },
                selected = config.borderStyle,
                onSelect = { onUpdateConfig(config.copy(borderStyle = it)) },
                tagPrefix = "seat_plan_border"
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
                        tagPrefix = "seat_plan_border_weight"
                    )
                    AdmitCompactPickerBox(
                        label = "কোনার বাঁক",
                        valueText = if (config.safeCornerRadiusPt < 0.5f) "০ pt" else formatAdmitPtBn(config.safeCornerRadiusPt),
                        options = SEAT_CORNER_RADIUS_OPTIONS,
                        selectedValue = config.safeCornerRadiusPt,
                        optionLabel = { r ->
                            when {
                                r < 0.5f -> "০ pt (সোজা)"
                                abs(r - 18f) < 0.5f -> "১৮ pt (বেশি গোলাকার)"
                                else -> formatAdmitPtBn(r)
                            }
                        },
                        onSelect = { onUpdateConfig(config.copy(cornerRadiusPt = it)) },
                        modifier = Modifier.weight(0.85f),
                        testTag = "seat_plan_corner_picker"
                    )
                }
            }
            HorizontalDivider(color = SlateOutline.copy(alpha = 0.35f))
            AdmitCompactSwitchRow(
                label = "সংখ্যা বাংলা (০১, ৫ম)",
                checked = config.numeralMode == SeatPlanNumeralMode.BENGALI,
                onCheckedChange = { bnOn ->
                    onUpdateConfig(
                        config.copy(numeralMode = if (bnOn) SeatPlanNumeralMode.BENGALI else SeatPlanNumeralMode.ENGLISH)
                    )
                },
                testTag = "seat_plan_opt_bn_num"
            )
            HorizontalDivider(color = SlateOutline.copy(alpha = 0.35f))
            AdmitCompactSwitchRow(
                label = "কার্ডের মাঝে কাটার দাগ",
                checked = config.showCutLines,
                onCheckedChange = { onUpdateConfig(config.copy(showCutLines = it)) },
                testTag = "seat_plan_opt_cut_lines"
            )
            HorizontalDivider(color = SlateOutline.copy(alpha = 0.35f))
            AdmitCompactSwitchRow(
                label = "০১ রোল (২ ডিজিট)",
                checked = config.padRollTwoDigits,
                onCheckedChange = { onUpdateConfig(config.copy(padRollTwoDigits = it)) },
                testTag = "seat_plan_opt_pad_roll"
            )
        }
    }
}

@Composable
internal fun SeatPlanFontAndStyleTab(
    ctx: Context,
    config: SeatPlanConfig,
    activeBnFontName: String,
    activeEnFontName: String,
    onUpdateConfig: (SeatPlanConfig) -> Unit
) {
    val sizeSummary = "স্কুল ${formatAdmitPtBn(config.safeSchoolFontPt)} · নাম ${formatAdmitPtBn(config.safeStudentNameFontPt)} · রোল ${formatAdmitPtBn(config.safeClassRollFontPt)}"

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AdmitCollapsibleGroupCard(
            title = "১. বাংলা ও ইংরেজি ফন্ট",
            summary = "$activeBnFontName · $activeEnFontName",
            icon = Icons.Rounded.TextFields,
            initiallyExpanded = true,
            testTag = "seat_plan_grp_fonts"
        ) {
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
                    testTag = "seat_plan_bn_font_btn",
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
                    testTag = "seat_plan_en_font_btn",
                    optionFontFamily = { id ->
                        FontFamily(androidx.compose.ui.text.font.Typeface(PdfGen.resolveTypeface(ctx, id, false)))
                    }
                )
            }
        }

        AdmitCollapsibleGroupCard(
            title = "২. অংশভেদে ফন্ট সাইজ ও বোল্ড (pt)",
            summary = sizeSummary,
            icon = Icons.Rounded.FormatSize,
            initiallyExpanded = true,
            testTag = "seat_plan_grp_sizes"
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AdmitCompactPickerBox(
                    label = "বিদ্যালয়ের নাম",
                    valueText = formatAdmitPtBn(config.safeSchoolFontPt),
                    options = SEAT_PART_FONT_SIZES,
                    selectedValue = config.safeSchoolFontPt,
                    optionLabel = { formatAdmitPtBn(it) },
                    onSelect = { onUpdateConfig(config.copy(schoolFontPt = it)) },
                    modifier = Modifier.weight(1f),
                    testTag = "seat_plan_size_school"
                )
                AdmitCompactPickerBox(
                    label = "ঠিকানা ও শিরোনাম",
                    valueText = formatAdmitPtBn(config.safeHeaderMetaFontPt),
                    options = SEAT_PART_FONT_SIZES,
                    selectedValue = config.safeHeaderMetaFontPt,
                    optionLabel = { formatAdmitPtBn(it) },
                    onSelect = { onUpdateConfig(config.copy(headerMetaFontPt = it)) },
                    modifier = Modifier.weight(1f),
                    testTag = "seat_plan_size_header"
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AdmitCompactPickerBox(
                    label = "শিক্ষার্থীর নাম",
                    valueText = formatAdmitPtBn(config.safeStudentNameFontPt),
                    options = SEAT_PART_FONT_SIZES,
                    selectedValue = config.safeStudentNameFontPt,
                    optionLabel = { formatAdmitPtBn(it) },
                    onSelect = { onUpdateConfig(config.copy(studentNameFontPt = it)) },
                    modifier = Modifier.weight(1f),
                    testTag = "seat_plan_size_name"
                )
                AdmitCompactPickerBox(
                    label = "শ্রেণি ও রোল",
                    valueText = formatAdmitPtBn(config.safeClassRollFontPt),
                    options = SEAT_PART_FONT_SIZES,
                    selectedValue = config.safeClassRollFontPt,
                    optionLabel = { formatAdmitPtBn(it) },
                    onSelect = { onUpdateConfig(config.copy(classRollFontPt = it)) },
                    modifier = Modifier.weight(1f),
                    testTag = "seat_plan_size_meta"
                )
            }
            HorizontalDivider(color = SlateOutline.copy(alpha = 0.35f))
            Text(
                "বোল্ড করবেন কোন অংশ:",
                fontSize = 9.5.sp,
                color = SlateMuted
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                AdmitCompactTogglePill(
                    label = "বিদ্যালয়",
                    checked = config.boldSchoolName,
                    onCheckedChange = { onUpdateConfig(config.copy(boldSchoolName = it)) },
                    testTag = "seat_plan_bold_school"
                )
                AdmitCompactTogglePill(
                    label = "ঠিকানা",
                    checked = config.boldAddress,
                    onCheckedChange = { onUpdateConfig(config.copy(boldAddress = it)) },
                    testTag = "seat_plan_bold_address"
                )
                AdmitCompactTogglePill(
                    label = "পরীক্ষা ও বিন্যাস",
                    checked = config.boldExamTitle,
                    onCheckedChange = { onUpdateConfig(config.copy(boldExamTitle = it)) },
                    testTag = "seat_plan_bold_exam"
                )
                AdmitCompactTogglePill(
                    label = "নাম",
                    checked = config.boldStudentName,
                    onCheckedChange = { onUpdateConfig(config.copy(boldStudentName = it)) },
                    testTag = "seat_plan_bold_name"
                )
                AdmitCompactTogglePill(
                    label = "শ্রেণি ও রোল",
                    checked = config.boldClassRoll,
                    onCheckedChange = { onUpdateConfig(config.copy(boldClassRoll = it)) },
                    testTag = "seat_plan_bold_meta"
                )
            }
        }
    }
}
