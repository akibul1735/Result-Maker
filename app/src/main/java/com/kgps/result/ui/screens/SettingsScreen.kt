package com.kgps.result.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.CLSN
import com.kgps.result.CustomFieldDef
import com.kgps.result.DPE_LOGO_LIST
import com.kgps.result.PDF_FONT_LIST
import com.kgps.result.PdfGen
import com.kgps.result.Repo
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.bn
import com.kgps.result.ui.components.TreeBranchItem
import com.kgps.result.ui.components.TreeGroupCard
import com.kgps.result.ui.theme.RoseDanger
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    students: List<Student>,
    settings: Settings,
    onUpdateSettings: (Settings) -> Unit,
    onAddCustomField: (String) -> Boolean,
    onRemoveCustomField: (String) -> Unit,
    onOpenCsvImport: (String) -> Unit,
    onRestoreBackupJson: (String) -> Unit,
    onExportAllPdf: () -> Unit,
    onResetSampleData: () -> Unit,
    onClearAllStudents: () -> Unit,
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    BackHandler { onBack() }

    var s by remember(settings) { mutableStateOf(settings) }
    var expandedGroup by remember { mutableStateOf("school") } // school, font, teachers, backup
    var newFieldLabel by remember { mutableStateOf("") }
    var askReset by remember { mutableStateOf(false) }
    var askClear by remember { mutableStateOf(false) }

    val csvExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.openOutputStream(uri)?.use {
                    it.write(Repo.toCsv(students, settings.customFields).toByteArray(Charsets.UTF_8))
                }
                onShowMessage("CSV ফাইল সংরক্ষিত হয়েছে")
            } catch (e: Exception) {
                onShowMessage("CSV তৈরি হয়নি: ${e.message ?: "ত্রুটি"}")
            }
        }
    }

    val csvTemplateLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            try {
                val hdr = "\uFEFF" + Repo.header(settings.customFields).joinToString(",") + "\n"
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(hdr.toByteArray(Charsets.UTF_8)) }
                onShowMessage("খালি CSV টেমপ্লেট তৈরি হয়েছে")
            } catch (e: Exception) {
                onShowMessage("টেমপ্লেট তৈরি হয়নি: ${e.message ?: "ত্রুটি"}")
            }
        }
    }

    val csvFilePickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val txt = ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                onOpenCsvImport(txt)
            } catch (e: Exception) {
                onShowMessage("CSV পড়া যায়নি: ${e.message ?: "ত্রুটি"}")
            }
        }
    }

    val jsonExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.openOutputStream(uri)?.use {
                    it.write(Repo.toJson(students, settings).toByteArray(Charsets.UTF_8))
                }
                onShowMessage("JSON ব্যাকআপ সংরক্ষিত হয়েছে")
            } catch (e: Exception) {
                onShowMessage("ব্যাকআপ তৈরি হয়নি: ${e.message ?: "ত্রুটি"}")
            }
        }
    }

    val jsonRestoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val txt = ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                onRestoreBackupJson(txt)
            } catch (e: Exception) {
                onShowMessage("ফাইল পড়া যায়নি: ${e.message ?: "ত্রুটি"}")
            }
        }
    }

    fun updateAndSave(next: Settings) {
        s = next
        onUpdateSettings(next)
    }

    val activeFontLabel = PDF_FONT_LIST.find { it.id == s.pdfFont }?.labelBn?.substringBefore(" (") ?: s.pdfFont
    val activeLogoLabel = DPE_LOGO_LIST.find { it.id == s.dpeLogo }?.labelBn?.substringBefore(" (") ?: s.dpeLogo

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            Modifier.widthIn(max = 820.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Group 1: School Information
            TreeGroupCard(
                title = "বিদ্যালয় ও শিক্ষাবর্ষ",
                subtitle = s.school.ifBlank { "বিদ্যালয়ের নাম ও ঠিকানা" },
                badgeText = bn(s.year),
                icon = Icons.Rounded.School,
                expanded = expandedGroup == "school",
                headerTestTag = "settings_group_school",
                onToggle = { expandedGroup = if (expandedGroup == "school") "" else "school" }
            ) {
                TreeBranchItem(isLast = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = s.school,
                            onValueChange = { updateAndSave(s.copy(school = it)) },
                            label = { Text("বিদ্যালয়ের নাম") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("settings_school_input")
                        )
                        OutlinedTextField(
                            value = s.schoolAddress,
                            onValueChange = { updateAndSave(s.copy(schoolAddress = it)) },
                            label = { Text("বিদ্যালয়ের ঠিকানা (ব্যাক কভারে মুদ্রিত হবে)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("settings_address_input")
                        )
                        OutlinedTextField(
                            value = s.year,
                            onValueChange = { updateAndSave(s.copy(year = it)) },
                            label = { Text("শিক্ষাবর্ষ") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("settings_year_input")
                        )
                    }
                }
            }

            // Group 2: PDF Fonts & DPE Cover Logo
            TreeGroupCard(
                title = "PDF ফন্ট ও কভার লোগো",
                subtitle = "$activeFontLabel · $activeLogoLabel",
                badgeText = "প্রিন্ট",
                icon = Icons.Rounded.FontDownload,
                expanded = expandedGroup == "font",
                headerTestTag = "settings_group_font",
                onToggle = { expandedGroup = if (expandedGroup == "font") "" else "font" }
            ) {
                TreeBranchItem(isLast = false) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("অফিসিয়াল বাংলা ফন্ট", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TealPrimary)
                        PDF_FONT_LIST.forEach { font ->
                            val selected = s.pdfFont == font.id
                            val sampleFamily = remember(font.id) {
                                FontFamily(androidx.compose.ui.text.font.Typeface(PdfGen.resolveTypeface(ctx, font.id, false)))
                            }
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_font_${font.id}")
                                    .clickable { updateAndSave(s.copy(pdfFont = font.id)) },
                                color = if (selected) TealSoft else SlateBg,
                                border = BorderStroke(1.dp, if (selected) TealPrimary else SlateOutline),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(selected = selected, onClick = { updateAndSave(s.copy(pdfFont = font.id)) })
                                    Column(Modifier.weight(1f)) {
                                        Text(font.labelBn, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                                        Text(font.descriptionBn, fontSize = 11.sp, color = SlateMuted)
                                        Text(
                                            "শিখন অগ্রগতি প্রতিবেদন — ২০২৬",
                                            fontSize = 12.5.sp,
                                            fontFamily = sampleFamily,
                                            color = SlateText,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                TreeBranchItem(isLast = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("প্রাথমিক শিক্ষা অধিদপ্তর লোগো", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TealPrimary)
                        DPE_LOGO_LIST.forEach { logo ->
                            val selected = s.dpeLogo == logo.id
                            val thumbBmp = remember(logo.id) { PdfGen.resolveLogoBitmap(ctx, logo.id) }
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_logo_${logo.id}")
                                    .clickable { updateAndSave(s.copy(dpeLogo = logo.id)) },
                                color = if (selected) TealSoft else SlateBg,
                                border = BorderStroke(1.dp, if (selected) TealPrimary else SlateOutline),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(selected = selected, onClick = { updateAndSave(s.copy(dpeLogo = logo.id)) })
                                    if (thumbBmp != null) {
                                        Image(
                                            bitmap = thumbBmp.asImageBitmap(),
                                            contentDescription = logo.labelBn,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(logo.labelBn, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                                        Text(logo.descriptionBn, fontSize = 11.sp, color = SlateMuted)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Group 3: Teachers & Custom Fields
            TreeGroupCard(
                title = "শ্রেণি শিক্ষক ও কাস্টম ফিল্ড",
                subtitle = "১ম–৫ম শ্রেণি · ${bn(s.customFields.size.toString())}টি কাস্টম ফিল্ড",
                icon = Icons.Rounded.Group,
                expanded = expandedGroup == "teachers",
                headerTestTag = "settings_group_teachers",
                onToggle = { expandedGroup = if (expandedGroup == "teachers") "" else "teachers" }
            ) {
                TreeBranchItem(isLast = false) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("শ্রেণি শিক্ষক ও মোবাইল", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TealPrimary)
                        (1..5).forEach { c ->
                            Text("${CLSN[c - 1]} শ্রেণি", fontWeight = FontWeight.Medium, fontSize = 12.sp, color = SlateMuted)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = s.teachers[c] ?: "",
                                    onValueChange = { v -> updateAndSave(s.copy(teachers = s.teachers + (c to v))) },
                                    label = { Text("শিক্ষকের নাম", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = s.teacherMobiles[c] ?: "",
                                    onValueChange = { v -> updateAndSave(s.copy(teacherMobiles = s.teacherMobiles + (c to v))) },
                                    label = { Text("মোবাইল", fontSize = 11.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier.weight(0.85f)
                                )
                            }
                        }
                    }
                }

                TreeBranchItem(isLast = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("কাস্টম ডাটাবেস ফিল্ড", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TealPrimary)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = newFieldLabel,
                                onValueChange = { newFieldLabel = it },
                                placeholder = { Text("যেমন: রক্তের গ্রুপ", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("settings_new_custom_field_input")
                            )
                            Button(
                                onClick = {
                                    if (onAddCustomField(newFieldLabel)) newFieldLabel = ""
                                },
                                enabled = newFieldLabel.isNotBlank(),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("settings_add_custom_field_btn")
                            ) { Text("+ যোগ", fontSize = 12.sp) }
                        }
                        if (s.customFields.isEmpty()) {
                            Text("এখনো কোনো কাস্টম ফিল্ড নেই।", fontSize = 12.sp, color = SlateMuted)
                        } else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                s.customFields.forEach { cf: CustomFieldDef ->
                                    AssistChip(
                                        onClick = { onRemoveCustomField(cf.key) },
                                        label = { Text(cf.label, fontSize = 12.sp) },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Rounded.Close,
                                                contentDescription = "${cf.label} মুছুন",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        modifier = Modifier.testTag("settings_remove_field_${cf.key}")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Group 4: Database, CSV & Backup
            TreeGroupCard(
                title = "ডাটাবেস, CSV ও ব্যাকআপ",
                subtitle = "মোট ${bn(students.size.toString())} জন শিক্ষার্থী সংরক্ষিত",
                badgeText = "ব্যাকআপ",
                icon = Icons.Rounded.Backup,
                expanded = expandedGroup == "backup",
                headerTestTag = "settings_group_backup",
                onToggle = { expandedGroup = if (expandedGroup == "backup") "" else "backup" }
            ) {
                TreeBranchItem(isLast = false) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = onExportAllPdf,
                            modifier = Modifier.fillMaxWidth().testTag("settings_all_pdf_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("সব শ্রেণির সব শিক্ষার্থীর PDF ডাউনলোড") }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { csvExportLauncher.launch("students.csv") },
                                modifier = Modifier.weight(1f).testTag("settings_csv_export_btn"),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("CSV এক্সপোর্ট", fontSize = 12.sp) }
                            OutlinedButton(
                                onClick = { csvTemplateLauncher.launch("students_template.csv") },
                                modifier = Modifier.weight(1f).testTag("settings_csv_template_btn"),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("খালি টেমপ্লেট", fontSize = 12.sp) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { onOpenCsvImport("") },
                                modifier = Modifier.weight(1f).testTag("settings_smart_csv_btn"),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("স্মার্ট CSV ইমপোর্ট", fontSize = 12.sp) }
                            OutlinedButton(
                                onClick = { csvFilePickLauncher.launch(arrayOf("*/*")) },
                                modifier = Modifier.weight(1f).testTag("settings_pick_csv_btn"),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("CSV ফাইল খুলুন", fontSize = 12.sp) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { jsonExportLauncher.launch("result_maker_backup.json") },
                                modifier = Modifier.weight(1f).testTag("settings_json_export_btn"),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("JSON ব্যাকআপ", fontSize = 12.sp) }
                            OutlinedButton(
                                onClick = { jsonRestoreLauncher.launch(arrayOf("application/json", "*/*")) },
                                modifier = Modifier.weight(1f).testTag("settings_json_restore_btn"),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("JSON রিস্টোর", fontSize = 12.sp) }
                        }
                    }
                }

                TreeBranchItem(isLast = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        HorizontalDivider(color = SlateOutline)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { askReset = true },
                                modifier = Modifier.weight(1f).testTag("settings_reset_sample_btn"),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("নমুনা রিসেট", fontSize = 12.sp) }
                            Button(
                                onClick = { askClear = true },
                                colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                                modifier = Modifier.weight(1f).testTag("settings_clear_all_btn"),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("সব মুছুন", fontSize = 12.sp) }
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = SlateBg),
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("অ্যাপ্লিকেশন তথ্য", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    Text("অ্যাপ আইডি: com.kgps.result · ভার্সন: 1.0.0", fontSize = 12.sp, color = SlateMuted)
                    Text("ফরম্যাট: NCTB প্রাথমিক বিদ্যালয় (১ম–৫ম শ্রেণি) A4 Landscape ভাঁজযোগ্য বুকলেট", fontSize = 12.sp, color = SlateMuted)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (askReset) {
        AlertDialog(
            onDismissRequest = { askReset = false },
            title = { Text("নমুনা ডাটা রিসেট করবেন?") },
            text = { Text("বর্তমান সকল তথ্য প্রতিস্থাপিত হয়ে প্রাথমিক নমুনা শিক্ষার্থী ও সেটিংস লোড হবে।") },
            confirmButton = {
                Button(onClick = { askReset = false; onResetSampleData() }) { Text("হ্যাঁ, রিসেট") }
            },
            dismissButton = {
                TextButton(onClick = { askReset = false }) { Text("বাতিল") }
            }
        )
    }

    if (askClear) {
        AlertDialog(
            onDismissRequest = { askClear = false },
            title = { Text("সব শিক্ষার্থী মুছে ফেলবেন?") },
            text = { Text("সকল শ্রেণির সকল শিক্ষার্থীর তথ্য স্থায়ীভাবে মুছে যাবে। ব্যাকআপ না থাকলে পুনরুদ্ধার করা যাবে না।") },
            confirmButton = {
                Button(
                    onClick = { askClear = false; onClearAllStudents() },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger)
                ) { Text("সব মুছুন") }
            },
            dismissButton = {
                TextButton(onClick = { askClear = false }) { Text("বাতিল") }
            }
        )
    }
}
