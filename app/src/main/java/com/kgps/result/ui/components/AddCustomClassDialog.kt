package com.kgps.result.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.CustomClassDef
import com.kgps.result.Settings
import com.kgps.result.activeSchoolClasses
import com.kgps.result.bn
import com.kgps.result.defaultShortClassLabel
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealContainer
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft

private data class CustomClassPreset(
    val nameBn: String,
    val shortBn: String,
    val syllabusTier: Int
)

private val CUSTOM_CLASS_PRESETS = listOf(
    CustomClassPreset("প্রাক-প্রাথমিক", "প্রাক", 1),
    CustomClassPreset("ষষ্ঠ", "৬ষ্ঠ", 3),
    CustomClassPreset("সপ্তম", "৭ম", 3),
    CustomClassPreset("অষ্টম", "৮ম", 3)
)

@Composable
fun AddCustomClassDialog(
    settings: Settings,
    onDismiss: () -> Unit,
    onConfirm: (nameBn: String, shortBn: String, syllabusTier: Int) -> Boolean
) {
    var nameBn by remember { mutableStateOf("") }
    var shortBn by remember { mutableStateOf("") }
    var syllabusTier by remember { mutableIntStateOf(3) }

    val existingNames = remember(settings.customClasses) {
        activeSchoolClasses(settings).map { it.nameBn.trim().lowercase() }.toSet()
    }
    val remainingSlots = (15 - settings.customClasses.size).coerceAtLeast(0)
    val cleanName = nameBn.trim()
    val isDuplicate = cleanName.isNotEmpty() && existingNames.contains(cleanName.lowercase())
    val effectiveShort = shortBn.trim().ifBlank {
        if (cleanName.isNotEmpty()) defaultShortClassLabel(cleanName, 6) else ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TealSoft,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Rounded.School,
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("নতুন শ্রেণি যোগ করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SlateText)
                    Text(
                        "আরও ${bn(remainingSlots.toString())}টি নতুন শ্রেণি যোগ করা যাবে",
                        fontSize = 11.sp,
                        color = SlateMuted
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp).testTag("add_custom_class_close_btn")
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "বন্ধ করুন", tint = SlateMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("দ্রুত পছন্দ (ট্যাপ করে বসান)", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = SlateMuted)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CUSTOM_CLASS_PRESETS.forEach { preset ->
                        val alreadyAdded = existingNames.contains(preset.nameBn.lowercase())
                        val isSel = cleanName.equals(preset.nameBn, ignoreCase = true)
                        Surface(
                            color = when {
                                alreadyAdded -> SlateBg
                                isSel -> TealContainer
                                else -> TealSoft.copy(alpha = 0.7f)
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    alreadyAdded -> SlateOutline
                                    isSel -> TealPrimary
                                    else -> TealPrimary.copy(alpha = 0.35f)
                                }
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .clickable(enabled = !alreadyAdded) {
                                    nameBn = preset.nameBn
                                    shortBn = preset.shortBn
                                    syllabusTier = preset.syllabusTier
                                }
                                .testTag("preset_class_${preset.shortBn}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (alreadyAdded) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(13.dp))
                                }
                                Text(
                                    text = if (alreadyAdded) "${preset.nameBn} (আছে)" else "${preset.nameBn} (${preset.shortBn})",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (alreadyAdded) SlateMuted else TealDark
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = nameBn,
                    onValueChange = { nameBn = it },
                    label = { Text("শ্রেণির নাম (যেমন: প্রাক-প্রাথমিক, ষষ্ঠ)", fontSize = 12.sp) },
                    singleLine = true,
                    isError = isDuplicate,
                    supportingText = if (isDuplicate) {
                        { Text("এই নামের শ্রেণি ইতিমধ্যে চালু আছে", fontSize = 11.sp) }
                    } else null,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_custom_class_name_input")
                )

                OutlinedTextField(
                    value = shortBn,
                    onValueChange = { shortBn = it.take(8) },
                    label = { Text("সংক্ষিপ্ত নাম (ঐচ্ছিক, যেমন: প্রাক, ৬ষ্ঠ)", fontSize = 12.sp) },
                    placeholder = {
                        if (effectiveShort.isNotBlank()) Text("স্বয়ংক্রিয়: $effectiveShort", fontSize = 12.sp)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_custom_class_short_input")
                )

                Text("বিষয় ও নম্বর বিভাজনের ধরন", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SlateText)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SyllabusTierOptionCard(
                        selected = syllabusTier == 1,
                        title = "১ম–২য় শ্রেণির ধরন (সমন্বিত ৫০/৫০)",
                        subtitle = "বাংলা, ইংরেজি, গণিত (৫০ ধারাবাহিক + ৫০ সামষ্টিক) ও সমন্বিত বিজ্ঞান/সামাজিক",
                        testTag = "add_class_tier_1",
                        onClick = { syllabusTier = 1 }
                    )
                    SyllabusTierOptionCard(
                        selected = syllabusTier == 3,
                        title = "৩য়–৫ম শ্রেণির ধরন (বিজ্ঞান + বাওবি ৩০/৭০)",
                        subtitle = "বাংলা, ইংরেজি, গণিত, বিজ্ঞান ও বাওবি (৩০ ধারাবাহিক + ৭০ সামষ্টিক)",
                        testTag = "add_class_tier_3",
                        onClick = { syllabusTier = 3 }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (onConfirm(cleanName, shortBn.trim(), syllabusTier)) {
                        onDismiss()
                    }
                },
                enabled = cleanName.isNotEmpty() && !isDuplicate && remainingSlots > 0,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .testTag("add_custom_class_confirm_btn")
            ) {
                Icon(Icons.Rounded.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("শ্রেণি যোগ করুন", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("add_custom_class_cancel_btn")
            ) { Text("বাতিল", fontSize = 12.5.sp) }
        }
    )
}

@Composable
private fun SyllabusTierOptionCard(
    selected: Boolean,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) TealSoft else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) TealPrimary else SlateOutline),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RadioButton(selected = selected, onClick = onClick, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    title,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selected) TealDark else SlateText
                )
                Text(subtitle, fontSize = 10.5.sp, color = SlateMuted, lineHeight = 14.sp)
            }
        }
    }
}
