@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.kgps.result.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.AdmitCardConfig
import com.kgps.result.AdmitCardPlanner
import com.kgps.result.SeatPlanRollOrder
import com.kgps.result.Settings
import com.kgps.result.bn
import com.kgps.result.classShortBn
import com.kgps.result.fromBn
import com.kgps.result.termNameBn
import com.kgps.result.ui.theme.RoseDanger
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateSurfaceVariant
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.appFilterChipBorder
import com.kgps.result.ui.theme.appFilterChipColors
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

internal val ADMIT_MARGIN_INCH_OPTIONS = listOf(0.15f, 0.20f, 0.25f, 0.28f, 0.30f, 0.35f, 0.40f, 0.45f, 0.50f, 0.60f)
internal val ADMIT_GAP_PT_OPTIONS = listOf(0f, 2f, 4f, 6f, 8f, 10f, 12f, 16f, 20f)
internal val ADMIT_CORNER_RADIUS_OPTIONS = listOf(0f, 2f, 4f, 5f, 6f, 8f, 10f, 12f, 16f, 20f)
internal val ADMIT_SIG_SCALE_OPTIONS = listOf(0.7f, 0.85f, 1.0f, 1.2f, 1.4f)

internal fun formatAdmitInchesBn(inches: Float): String =
    bn(String.format(Locale.US, "%.2f", inches)) + "\""

internal fun formatAdmitPtBn(value: Float): String {
    val roundedHalf = (value * 2f).roundToInt() / 2f
    val raw = if (abs(roundedHalf - roundedHalf.toInt()) < 0.05f) {
        roundedHalf.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", roundedHalf)
    }
    return "${bn(raw)} pt"
}

@Composable
internal fun AdmitSectionGroupCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = SlateBg.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, SlateOutline.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(11.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                title,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TealDark
            )
            content()
        }
    }
}

@Composable
internal fun <T> AdmitSegmentedOptionBar(
    items: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    useLightActivePill: Boolean = false,
    tagPrefix: String = "admit_seg"
) {
    Surface(
        color = SlateBg,
        border = BorderStroke(1.dp, SlateOutline),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items.forEachIndexed { idx, (value, label) ->
                val isSel = value == selected
                val bg = when {
                    isSel && useLightActivePill -> MaterialTheme.colorScheme.surface
                    isSel -> TealPrimary
                    else -> Color.Transparent
                }
                val fg = when {
                    isSel && useLightActivePill -> TealDark
                    isSel -> Color.White
                    else -> SlateMuted
                }
                val border = if (isSel && useLightActivePill) {
                    BorderStroke(1.dp, TealPrimary.copy(alpha = 0.4f))
                } else null
                Surface(
                    onClick = { onSelect(value) },
                    color = bg,
                    border = border,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("${tagPrefix}_$idx")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text(
                            label,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = fg,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun <T> AdmitCompactPickerBox(
    label: String,
    valueText: String,
    options: List<T>,
    selectedValue: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isFontDropdown: Boolean = false,
    testTag: String = "",
    optionFontFamily: ((T) -> FontFamily?)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            onClick = { if (enabled) expanded = true },
            enabled = enabled,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, if (enabled) SlateOutline else SlateOutline.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        label,
                        fontSize = 9.5.sp,
                        color = SlateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        valueText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) SlateText else SlateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = if (isFontDropdown) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.UnfoldMore,
                    contentDescription = label,
                    tint = SlateMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { item ->
                val isSel = item == selectedValue
                val family = optionFontFamily?.invoke(item)
                DropdownMenuItem(
                    text = {
                        Text(
                            optionLabel(item),
                            fontFamily = family,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) TealPrimary else SlateText
                        )
                    },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
internal fun AdmitCompactSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = SlateText,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = TealPrimary,
                uncheckedTrackColor = SlateSurfaceVariant
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
internal fun AdmitClassAndRollTab(
    config: AdmitCardConfig,
    settings: Settings,
    allowedClasses: List<Int>,
    countByClass: Map<Int, Int>,
    rollFromText: String,
    rollToText: String,
    onSelectClass: (Int) -> Unit,
    onUpdateConfig: (AdmitCardConfig) -> Unit,
    onRollFromChange: (String) -> Unit,
    onRollToChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdmitSectionGroupCard(title = "শ্রেণি নির্বাচন") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                allowedClasses.forEach { c ->
                    val isSel = c == config.selectedClass
                    val cnt = countByClass[c] ?: 0
                    FilterChip(
                        selected = isSel,
                        onClick = { onSelectClass(c) },
                        label = {
                            Text("${classShortBn(c, settings)} (${bn(cnt.toString())})", fontSize = 11.5.sp)
                        },
                        colors = appFilterChipColors(),
                        border = appFilterChipBorder(isSel),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("admit_card_cls_$c")
                    )
                }
            }
        }

        AdmitSectionGroupCard(title = "প্রান্তিক মূল্যায়ন") {
            AdmitSegmentedOptionBar(
                items = (1..3).map { it to termNameBn(it) },
                selected = config.term,
                onSelect = { onUpdateConfig(config.copy(term = it)) },
                tagPrefix = "admit_card_term"
            )
        }

        AdmitSectionGroupCard(title = "রোল ক্রম ও পরিসর") {
            AdmitSegmentedOptionBar(
                items = SeatPlanRollOrder.entries.map { it to it.labelBn },
                selected = config.rollOrder,
                onSelect = { onUpdateConfig(config.copy(rollOrder = it)) },
                tagPrefix = "admit_card_order"
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = rollFromText,
                    onValueChange = { raw ->
                        val digits = fromBn(raw).filter { it.isDigit() }.take(4)
                        onRollFromChange(digits)
                    },
                    label = { Text("রোল থেকে", fontSize = 11.sp) },
                    placeholder = { Text("সব", fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag("admit_card_roll_from")
                )
                OutlinedTextField(
                    value = rollToText,
                    onValueChange = { raw ->
                        val digits = fromBn(raw).filter { it.isDigit() }.take(4)
                        onRollToChange(digits)
                    },
                    label = { Text("রোল পর্যন্ত", fontSize = 11.sp) },
                    placeholder = { Text("সব", fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag("admit_card_roll_to")
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text(
                        "অতিরিক্ত ফাঁকা প্রবেশপত্র",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SlateText,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (config.extraBlankCards > 0) {
                                onUpdateConfig(config.copy(extraBlankCards = config.extraBlankCards - 1))
                            }
                        },
                        enabled = config.extraBlankCards > 0,
                        modifier = Modifier.size(48.dp).testTag("admit_card_blank_minus")
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "ফাঁকা প্রবেশপত্র কমান", modifier = Modifier.size(16.dp))
                    }
                    Text(
                        bn(config.extraBlankCards.toString()),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateText,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(
                        onClick = {
                            if (config.extraBlankCards < 40) {
                                onUpdateConfig(config.copy(extraBlankCards = config.extraBlankCards + 1))
                            }
                        },
                        enabled = config.extraBlankCards < 40,
                        modifier = Modifier.size(48.dp).testTag("admit_card_blank_plus")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "ফাঁকা প্রবেশপত্র বাড়ান", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun AdmitSignatureAndInfoTab(
    config: AdmitCardConfig,
    settings: Settings,
    signatureBitmap: Bitmap?,
    onUpdateConfig: (AdmitCardConfig) -> Unit,
    onPickSignatureImage: () -> Unit,
    onClearSignatureImage: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdmitSectionGroupCard(title = "প্রধান শিক্ষকের স্বাক্ষর") {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (signatureBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = signatureBitmap.asImageBitmap(),
                                contentDescription = "প্রধান শিক্ষকের স্বাক্ষরের ছবি",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            )
                        }
                    } else {
                        Text(
                            "স্বাক্ষরের ছবি যুক্ত করা নেই (কার্ডে পরিষ্কার ফাঁকা স্বাক্ষর ঘর থাকবে)",
                            fontSize = 11.sp,
                            color = SlateMuted
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onPickSignatureImage,
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("admit_card_pick_sig_btn")
                        ) {
                            Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(if (signatureBitmap == null) "গ্যালারি থেকে স্বাক্ষর" else "ছবি বদলান", fontSize = 11.5.sp)
                        }
                        if (signatureBitmap != null) {
                            OutlinedButton(
                                onClick = onClearSignatureImage,
                                shape = RoundedCornerShape(9.dp),
                                border = BorderStroke(1.dp, RoseDanger.copy(alpha = 0.5f)),
                                modifier = Modifier.heightIn(min = 48.dp).testTag("admit_card_clear_sig_btn")
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "স্বাক্ষর মুছুন", tint = RoseDanger, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("মুছুন", fontSize = 11.5.sp, color = RoseDanger)
                            }
                        }
                    }
                }
            }

            if (signatureBitmap != null) {
                AdmitCompactSwitchRow(
                    label = "স্বাক্ষরের ছবি দেখাও (বন্ধ করলে ফাঁকা ঘর)",
                    checked = config.showSignatureImage,
                    onCheckedChange = { onUpdateConfig(config.copy(showSignatureImage = it)) },
                    testTag = "admit_card_show_sig_img_switch"
                )
                AdmitCompactPickerBox(
                    label = "স্বাক্ষরের ছবির সাইজ",
                    valueText = "${bn(String.format(Locale.US, "%.2g", config.safeSignatureScale))}×",
                    options = ADMIT_SIG_SCALE_OPTIONS,
                    selectedValue = config.safeSignatureScale,
                    optionLabel = { "${bn(String.format(Locale.US, "%.2g", it))}×" },
                    onSelect = { onUpdateConfig(config.copy(signatureScale = it)) },
                    testTag = "admit_card_sig_scale_picker"
                )
            }

            OutlinedTextField(
                value = config.headTeacherTitle,
                onValueChange = { onUpdateConfig(config.copy(headTeacherTitle = it)) },
                label = { Text("স্বাক্ষর পদের নাম", fontSize = 11.sp) },
                placeholder = { Text("প্রধান শিক্ষকের স্বাক্ষর", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("admit_card_sig_title_input")
            )
        }

        AdmitSectionGroupCard(title = "বিদ্যালয় ও কার্ড শিরোনাম") {
            AdmitCompactSwitchRow(
                label = "বিদ্যালয়ের ঠিকানা দেখাও",
                checked = config.showSchoolAddress,
                onCheckedChange = { onUpdateConfig(config.copy(showSchoolAddress = it)) },
                testTag = "admit_card_opt_show_address"
            )
            HorizontalDivider(color = SlateOutline.copy(alpha = 0.4f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = config.badgeLabel,
                    onValueChange = { onUpdateConfig(config.copy(badgeLabel = it)) },
                    label = { Text("কার্ড শিরোনাম", fontSize = 10.5.sp) },
                    placeholder = { Text("প্রবেশপত্র", fontSize = 10.5.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(0.85f).testTag("admit_card_badge_label_input")
                )
                OutlinedTextField(
                    value = config.customExamTitle,
                    onValueChange = { onUpdateConfig(config.copy(customExamTitle = it)) },
                    label = { Text("পরীক্ষার শিরোনাম", fontSize = 10.5.sp) },
                    placeholder = {
                        Text(AdmitCardPlanner.resolvedExamTitle(config.copy(customExamTitle = ""), settings), fontSize = 10.5.sp)
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1.15f).testTag("admit_card_exam_title_input")
                )
            }
            OutlinedTextField(
                value = config.customSchoolName,
                onValueChange = { onUpdateConfig(config.copy(customSchoolName = it)) },
                label = { Text("বিদ্যালয়ের নাম (ঐচ্ছিক)", fontSize = 11.sp) },
                placeholder = { Text(settings.school.ifBlank { "বিদ্যালয়ের নাম" }, fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("admit_card_school_name_input")
            )
            OutlinedTextField(
                value = config.customSchoolAddress,
                onValueChange = { onUpdateConfig(config.copy(customSchoolAddress = it)) },
                enabled = config.showSchoolAddress,
                label = { Text("বিদ্যালয়ের ঠিকানা (ঐচ্ছিক)", fontSize = 11.sp) },
                placeholder = { Text(settings.schoolAddress.ifBlank { "ঠিকানা" }, fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("admit_card_school_addr_input")
            )
        }
    }
}
