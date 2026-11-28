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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.BENGALI_PDF_FONT_LIST
import com.kgps.result.ENGLISH_PDF_FONT_LIST
import com.kgps.result.SeatPlanNumeralMode
import com.kgps.result.SeatPlanRollOrder
import com.kgps.result.Settings
import com.kgps.result.TestimonialBodyPreset
import com.kgps.result.TestimonialBorderStyle
import com.kgps.result.TestimonialConfig
import com.kgps.result.TestimonialPlanner
import com.kgps.result.TestimonialSerialMode
import com.kgps.result.TestimonialType
import com.kgps.result.bn
import com.kgps.result.classShortBn
import com.kgps.result.fromBn
import com.kgps.result.ui.theme.AmberBg
import com.kgps.result.ui.theme.AmberBorder
import com.kgps.result.ui.theme.AmberNotice
import com.kgps.result.ui.theme.RoseDanger
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft

@Composable
internal fun TestimonialClassAndRollTab(
    config: TestimonialConfig,
    settings: Settings,
    allowedClasses: List<Int>,
    countByClass: Map<Int, Int>,
    rollFromText: String,
    rollToText: String,
    missingInfoCount: Int,
    selectedStudentsCount: Int,
    onSelectClasses: (selectedClasses: List<Int>, primaryClass: Int) -> Unit,
    onUpdateConfig: (TestimonialConfig) -> Unit,
    onRollFromChange: (String) -> Unit,
    onRollToChange: (String) -> Unit,
    onOpenStudentInfoEditor: () -> Unit
) {
    val selectedSet = config.resolvedSelectedClasses.toSet()
    val allSelected = allowedClasses.isNotEmpty() && allowedClasses.all { it in selectedSet }

    AdmitSectionGroupCard(title = "শ্রেণি নির্বাচন (এক বা একাধিক)") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (allowedClasses.size > 1) {
                Surface(
                    onClick = {
                        val next = if (allSelected) {
                            listOf(config.selectedClass.takeIf { it in allowedClasses } ?: allowedClasses.first())
                        } else {
                            allowedClasses
                        }
                        onSelectClasses(next, next.first())
                    },
                    color = if (allSelected) TealPrimary else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (allSelected) TealPrimary else SlateOutline),
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier.heightIn(min = 32.dp).testTag("testimonial_cls_all")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (allSelected) "সব ✓" else "সব শ্রেণি",
                            fontSize = 10.5.sp,
                            fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (allSelected) Color.White else SlateText
                        )
                    }
                }
            }
            allowedClasses.forEach { c ->
                val isSel = c in selectedSet
                val count = countByClass[c] ?: 0
                Surface(
                    onClick = {
                        val next = if (isSel) {
                            if (selectedSet.size > 1) (selectedSet - c).toList() else selectedSet.toList()
                        } else {
                            (selectedSet + c).toList()
                        }
                        val ordered = allowedClasses.filter { it in next }.ifEmpty { next }
                        val nextPrimary = if (config.selectedClass in ordered) config.selectedClass else ordered.first()
                        onSelectClasses(ordered, nextPrimary)
                    },
                    color = if (isSel) TealPrimary else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (isSel) TealPrimary else SlateOutline),
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier.heightIn(min = 32.dp).testTag("testimonial_cls_$c")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${classShortBn(c, settings)} (${bn(count.toString())})",
                            fontSize = 10.5.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) Color.White else SlateText
                        )
                    }
                }
            }
        }
    }

    AdmitSectionGroupCard(title = "রোল ক্রম ও পরিসর") {
        AdmitSegmentedOptionBar(
            items = listOf(
                SeatPlanRollOrder.ASCENDING to "রোল ছোট → বড়",
                SeatPlanRollOrder.DESCENDING to "রোল বড় → ছোট"
            ),
            selected = config.rollOrder,
            onSelect = { onUpdateConfig(config.copy(rollOrder = it)) },
            tagPrefix = "testimonial_order"
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AdmitCompactTextInput(
                label = "শুরু রোল",
                value = rollFromText,
                onValueChange = onRollFromChange,
                placeholder = "যেমন: ১",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
                testTag = "testimonial_roll_from"
            )
            AdmitCompactTextInput(
                label = "শেষ রোল",
                value = rollToText,
                onValueChange = onRollToChange,
                placeholder = "সব",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
                testTag = "testimonial_roll_to"
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text("অতিরিক্ত ফাঁকা ফর্ম", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SlateText)
                Text("হাতে লেখার জন্য খালি প্রশংসাপত্র যোগ", fontSize = 9.5.sp, color = SlateMuted)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = SlateBg,
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = RoundedCornerShape(7.dp)
                ) {
                    IconButton(
                        onClick = { onUpdateConfig(config.copy(extraBlankCount = (config.extraBlankCount - 1).coerceAtLeast(0))) },
                        enabled = config.extraBlankCount > 0,
                        modifier = Modifier.size(34.dp).testTag("testimonial_blank_minus")
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "কমান", tint = SlateText, modifier = Modifier.size(16.dp))
                    }
                }
                Text(
                    bn(config.extraBlankCount.toString()),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TealDark
                )
                Surface(
                    color = SlateBg,
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = RoundedCornerShape(7.dp)
                ) {
                    IconButton(
                        onClick = { onUpdateConfig(config.copy(extraBlankCount = (config.extraBlankCount + 1).coerceAtMost(100))) },
                        modifier = Modifier.size(34.dp).testTag("testimonial_blank_plus")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "বাড়ান", tint = SlateText, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    Surface(
        color = if (missingInfoCount > 0) AmberBg else TealSoft,
        border = BorderStroke(1.dp, if (missingInfoCount > 0) AmberBorder else TealPrimary.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (missingInfoCount > 0) {
                        "${bn(missingInfoCount.toString())} জন শিক্ষার্থীর পিতা/মাতা বা জন্মতারিখ বাকি আছে"
                    } else {
                        "নির্বাচিত ${bn(selectedStudentsCount.toString())} জন শিক্ষার্থীর তথ্য প্রস্তুত"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (missingInfoCount > 0) AmberNotice else TealDark
                )
                Text(
                    text = "সরাসরি এখান থেকেই শিক্ষার্থীর পিতার নাম, মাতার নাম ও জন্মতারিখ পূরণ বা সংশোধন করুন",
                    fontSize = 9.5.sp,
                    color = SlateMuted
                )
            }
            OutlinedButton(
                onClick = onOpenStudentInfoEditor,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
                modifier = Modifier.heightIn(min = 38.dp).testTag("testimonial_edit_students_info_btn")
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("তথ্য পূরণ", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
internal fun TestimonialContentAndBodyTab(
    config: TestimonialConfig,
    settings: Settings,
    onUpdateConfig: (TestimonialConfig) -> Unit
) {
    AdmitSectionGroupCard(title = "ধরন ও শিরোনাম") {
        AdmitSegmentedOptionBar(
            items = TestimonialType.entries.map { it to it.labelBn },
            selected = config.testimonialType,
            onSelect = { nextType ->
                val shouldSwitchPreset = config.bodyPreset != TestimonialBodyPreset.CUSTOM &&
                    config.bodyPreset == config.testimonialType.defaultPreset
                onUpdateConfig(
                    config.copy(
                        testimonialType = nextType,
                        bodyPreset = if (shouldSwitchPreset) nextType.defaultPreset else config.bodyPreset
                    )
                )
            },
            tagPrefix = "testimonial_type"
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AdmitCompactTextInput(
                label = "ব্যানার শিরোনাম (ঐচ্ছিক)",
                value = config.customBannerTitle,
                onValueChange = { onUpdateConfig(config.copy(customBannerTitle = it)) },
                placeholder = config.testimonialType.defaultBannerTitle,
                modifier = Modifier.weight(1.1f),
                testTag = "testimonial_banner_title_input"
            )
            AdmitCompactTextInput(
                label = "শিক্ষাবর্ষ",
                value = config.sessionYear,
                onValueChange = { onUpdateConfig(config.copy(sessionYear = it)) },
                placeholder = settings.year.ifBlank { "২০২৬" },
                modifier = Modifier.weight(0.9f),
                testTag = "testimonial_session_year_input"
            )
        }
    }

    AdmitSectionGroupCard(title = "সিরিয়াল নম্বর ও তারিখ") {
        AdmitSegmentedOptionBar(
            items = TestimonialSerialMode.entries.map { it to it.labelBn },
            selected = config.serialMode,
            onSelect = { onUpdateConfig(config.copy(serialMode = it)) },
            tagPrefix = "testimonial_serial_mode"
        )
        if (config.serialMode != TestimonialSerialMode.BLANK) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AdmitCompactTextInput(
                    label = if (config.serialMode == TestimonialSerialMode.BASE_PLUS_ROLL) "সিরিয়াল Prefix (যার পরে রোল বসবে)" else "শুরুর সিরিয়াল নম্বর",
                    value = config.serialStartOrPrefix,
                    onValueChange = { onUpdateConfig(config.copy(serialStartOrPrefix = it)) },
                    placeholder = if (config.serialMode == TestimonialSerialMode.BASE_PLUS_ROLL) "২০২৬৫" else "২০২৬০০১",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                    testTag = "testimonial_serial_base_input"
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AdmitCompactTextInput(
                label = "প্রদানের তারিখ",
                value = config.issueDate,
                onValueChange = { onUpdateConfig(config.copy(issueDate = it)) },
                placeholder = TestimonialPlanner.todayBanglaDateString(config.numeralMode),
                modifier = Modifier.weight(1f),
                testTag = "testimonial_issue_date_input"
            )
            OutlinedButton(
                onClick = {
                    onUpdateConfig(
                        config.copy(issueDate = TestimonialPlanner.todayBanglaDateString(config.numeralMode))
                    )
                },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier.heightIn(min = 38.dp).testTag("testimonial_today_date_btn")
            ) {
                Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("আজকের", fontSize = 10.5.sp)
            }
        }
    }

    AdmitCollapsibleGroupCard(
        title = "মূল বক্তব্য ও সমাপনী লাইন",
        summary = config.bodyPreset.labelBn,
        icon = Icons.Rounded.Description,
        initiallyExpanded = false,
        testTag = "testimonial_grp_body"
    ) {
        AdmitCompactPickerBox(
            label = "বক্তব্যের ধরন",
            valueText = config.bodyPreset.labelBn,
            options = TestimonialBodyPreset.entries,
            selectedValue = config.bodyPreset,
            optionLabel = { it.labelBn },
            onSelect = { preset ->
                onUpdateConfig(
                    config.copy(
                        bodyPreset = preset,
                        customBodyTemplate = if (preset == TestimonialBodyPreset.CUSTOM && config.customBodyTemplate.isBlank()) {
                            preset.bodyTemplate
                        } else {
                            config.customBodyTemplate
                        }
                    )
                )
            },
            testTag = "testimonial_body_preset_picker"
        )
        if (config.bodyPreset == TestimonialBodyPreset.CUSTOM) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "কাস্টম বক্তব্য (ট্যাগ: {{NAME}}, {{FATHER}}, {{MOTHER}}, {{DOB}}, {{ROLL}}, {{YEAR}}, {{CLASS}})",
                        fontSize = 9.sp,
                        color = SlateMuted
                    )
                    BasicTextField(
                        value = config.customBodyTemplate,
                        onValueChange = { onUpdateConfig(config.copy(customBodyTemplate = it)) },
                        textStyle = TextStyle(fontSize = 11.5.sp, color = SlateText, lineHeight = 16.sp),
                        cursorBrush = SolidColor(TealPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 66.dp)
                            .testTag("testimonial_custom_body_input")
                    )
                }
            }
        }
        AdmitCompactTextInput(
            label = "সমাপনী লাইন (ঐচ্ছিক)",
            value = config.customClosingLine,
            onValueChange = { onUpdateConfig(config.copy(customClosingLine = it)) },
            placeholder = config.bodyPreset.defaultClosing,
            testTag = "testimonial_closing_line_input"
        )
    }
}

@Composable
internal fun TestimonialSchoolAndSignTab(
    config: TestimonialConfig,
    settings: Settings,
    signatureBitmap: Bitmap?,
    onUpdateConfig: (TestimonialConfig) -> Unit,
    onPickSignatureFromGallery: () -> Unit,
    onPickSignatureFromFiles: () -> Unit,
    onClearSignature: () -> Unit
) {
    AdmitCollapsibleGroupCard(
        title = "বিদ্যালয় ও সরকারি শিরোনাম",
        summary = TestimonialPlanner.resolvedSchoolName(config, settings),
        icon = Icons.Rounded.School,
        initiallyExpanded = true,
        testTag = "testimonial_grp_school"
    ) {
        AdmitCompactTextInput(
            label = "বিদ্যালয়ের নাম (ফাঁকা রাখলে সেটিংস থেকে নেবে)",
            value = config.customSchoolName,
            onValueChange = { onUpdateConfig(config.copy(customSchoolName = it)) },
            placeholder = settings.school.ifBlank { "বিদ্যালয়ের নাম" },
            testTag = "testimonial_school_name_input"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            AdmitCompactTextInput(
                label = "ঠিকানা (উপজেলা ও জেলা)",
                value = config.customSchoolAddress,
                onValueChange = { onUpdateConfig(config.copy(customSchoolAddress = it)) },
                placeholder = settings.schoolAddress.ifBlank { "উপজেলা: আলফাডাঙ্গা, জেলা: ফরিদপুর।" },
                modifier = Modifier.weight(1.3f),
                testTag = "testimonial_school_addr_input"
            )
            AdmitCompactTextInput(
                label = "স্থাপিত (ঐচ্ছিক)",
                value = config.establishedYear,
                onValueChange = { onUpdateConfig(config.copy(establishedYear = it)) },
                placeholder = "১৯৭৩",
                modifier = Modifier.weight(0.7f),
                testTag = "testimonial_estd_input"
            )
        }
        AdmitCompactTextInput(
            label = "স্বাক্ষরের নিচে স্থান (ঐচ্ছিক)",
            value = config.customSignPlace,
            onValueChange = { onUpdateConfig(config.copy(customSignPlace = it)) },
            placeholder = "উপজেলা, জেলা।",
            testTag = "testimonial_sign_place_input"
        )
        AdmitCompactSwitchRow(
            label = "সরকারি শিরোনাম (গণপ্রজাতন্ত্রী বাংলাদেশ সরকার...)",
            checked = config.showGovtHeader,
            onCheckedChange = { onUpdateConfig(config.copy(showGovtHeader = it)) },
            testTag = "testimonial_opt_govt_header"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            AdmitCompactTogglePill(
                label = "সরকারি সিল",
                checked = config.showGovtSeal,
                onCheckedChange = { onUpdateConfig(config.copy(showGovtSeal = it)) },
                modifier = Modifier.weight(1f),
                testTag = "testimonial_opt_govt_seal"
            )
            AdmitCompactTogglePill(
                label = "DPE লোগো",
                checked = config.showDpeLogo,
                onCheckedChange = { onUpdateConfig(config.copy(showDpeLogo = it)) },
                modifier = Modifier.weight(1f),
                testTag = "testimonial_opt_dpe_logo"
            )
            AdmitCompactTogglePill(
                label = "জলছাপ",
                checked = config.showWatermark,
                onCheckedChange = { onUpdateConfig(config.copy(showWatermark = it)) },
                modifier = Modifier.weight(1f),
                testTag = "testimonial_opt_watermark"
            )
        }
    }

    AdmitCollapsibleGroupCard(
        title = "প্রধান শিক্ষকের স্বাক্ষর",
        summary = if (signatureBitmap != null) "স্বাক্ষর যুক্ত" else "ফাঁকা স্বাক্ষর",
        icon = Icons.Rounded.Draw,
        initiallyExpanded = false,
        testTag = "testimonial_grp_signature"
    ) {
        AdmitCompactTextInput(
            label = "পদবি টেক্সট",
            value = config.headTeacherTitle,
            onValueChange = { onUpdateConfig(config.copy(headTeacherTitle = it)) },
            placeholder = "(প্রধান শিক্ষক)",
            testTag = "testimonial_sig_title_input"
        )
        if (signatureBitmap != null) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().height(48.dp).padding(4.dp)
                ) {
                    Image(
                        bitmap = signatureBitmap.asImageBitmap(),
                        contentDescription = "প্রধান শিক্ষকের স্বাক্ষর",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onPickSignatureFromGallery,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(34.dp).testTag("testimonial_pick_gallery_btn")
            ) {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("গ্যালারি", fontSize = 10.5.sp)
            }
            OutlinedButton(
                onClick = onPickSignatureFromFiles,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1.1f).height(34.dp).testTag("testimonial_pick_file_btn")
            ) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("ফাইল/ড্রাইভ", fontSize = 10.5.sp)
            }
            if (signatureBitmap != null) {
                OutlinedButton(
                    onClick = onClearSignature,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseDanger),
                    border = BorderStroke(1.dp, RoseDanger.copy(alpha = 0.45f)),
                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp).testTag("testimonial_clear_sig_btn")
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "স্বাক্ষর মুছুন", modifier = Modifier.size(14.dp))
                }
            }
        }
        if (signatureBitmap != null) {
            AdmitCompactSwitchRow(
                label = "ডিজিটাল স্বাক্ষর ছবি দেখান",
                checked = config.showSignatureImage,
                onCheckedChange = { onUpdateConfig(config.copy(showSignatureImage = it)) },
                testTag = "testimonial_show_sig_img_switch"
            )
            AdmitCompactPickerBox(
                label = "স্বাক্ষরের আকার",
                valueText = "${bn(String.format(java.util.Locale.US, "%.0f", config.safeSignatureScale * 100))}%",
                options = ADMIT_SIG_SCALE_OPTIONS,
                selectedValue = ADMIT_SIG_SCALE_OPTIONS.minByOrNull { kotlin.math.abs(it - config.safeSignatureScale) } ?: 1.0f,
                optionLabel = { "${bn(String.format(java.util.Locale.US, "%.0f", it * 100))}%" },
                onSelect = { onUpdateConfig(config.copy(signatureScale = it)) },
                testTag = "testimonial_sig_scale_picker"
            )
        }
    }
}

@Composable
internal fun TestimonialDesignAndFontTab(
    config: TestimonialConfig,
    onUpdateConfig: (TestimonialConfig) -> Unit,
    onOpenBnFontPicker: () -> Unit,
    onOpenEnFontPicker: () -> Unit
) {
    val bnFontName = BENGALI_PDF_FONT_LIST.find { it.id == config.bnFontId }?.labelBn ?: "নোতো সেরিফ বাংলা"
    val enFontName = ENGLISH_PDF_FONT_LIST.find { it.id == config.enFontId }?.labelBn ?: "টাইমস নিউ রোমান"

    AdmitSectionGroupCard(title = "কাগজ ও বর্ডার কাঠামো") {
        AdmitCompactSwitchRow(
            label = "বাম পাশের মুড়ি অংশ (অফিস কপি) দেখান",
            checked = config.showCounterfoil,
            onCheckedChange = { onUpdateConfig(config.copy(showCounterfoil = it)) },
            testTag = "testimonial_opt_counterfoil"
        )
        AdmitCompactSwitchRow(
            label = "২-অঙ্কের রোল নম্বর (০১, ০২...)",
            checked = config.padRollTwoDigits,
            onCheckedChange = { onUpdateConfig(config.copy(padRollTwoDigits = it)) },
            testTag = "testimonial_opt_pad_roll"
        )
        AdmitCompactSwitchRow(
            label = "বাংলা সংখ্যায় প্রিন্ট (১২৩)",
            checked = config.numeralMode == SeatPlanNumeralMode.BENGALI,
            onCheckedChange = { checked ->
                onUpdateConfig(
                    config.copy(
                        numeralMode = if (checked) SeatPlanNumeralMode.BENGALI else SeatPlanNumeralMode.ENGLISH
                    )
                )
            },
            testTag = "testimonial_opt_bn_num"
        )
        AdmitCompactPickerBox(
            label = "বর্ডার স্টাইল",
            valueText = config.borderStyle.labelBn,
            options = TestimonialBorderStyle.entries,
            selectedValue = config.borderStyle,
            optionLabel = { it.labelBn },
            onSelect = { onUpdateConfig(config.copy(borderStyle = it)) },
            testTag = "testimonial_border_style_picker"
        )
        val marginOptions = ADMIT_MARGIN_INCH_OPTIONS.filter { it in 0.14f..0.75f }
        AdmitCompactPickerBox(
            label = "পেজ মার্জিন",
            valueText = formatAdmitInchesBn(config.safeMarginInches),
            options = marginOptions,
            selectedValue = marginOptions.minByOrNull { kotlin.math.abs(it - config.safeMarginInches) } ?: 0.28f,
            optionLabel = { formatAdmitInchesBn(it) },
            onSelect = { onUpdateConfig(config.copy(marginInches = it)) },
            testTag = "testimonial_margin_picker"
        )
    }

    AdmitSectionGroupCard(title = "ফন্ট") {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            Surface(
                onClick = onOpenBnFontPicker,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).heightIn(min = 38.dp).testTag("testimonial_bn_font_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("বাংলা ফন্ট", fontSize = 9.sp, color = SlateMuted)
                        Text(bnFontName, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(14.dp))
                }
            }
            Surface(
                onClick = onOpenEnFontPicker,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).heightIn(min = 38.dp).testTag("testimonial_en_font_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("ইংরেজি ফন্ট", fontSize = 9.sp, color = SlateMuted)
                        Text(enFontName, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
