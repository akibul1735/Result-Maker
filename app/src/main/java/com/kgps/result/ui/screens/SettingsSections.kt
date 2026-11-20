package com.kgps.result.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.ai.DiscoveredGeminiModel
import com.kgps.result.ai.GeminiApiConfig
import com.kgps.result.backup.AutoBackupStatus
import com.kgps.result.bn
import com.kgps.result.cloud.SchoolWorkspace
import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.ui.components.TreeBranchItem
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

@Composable
internal fun SettingsCategoryHeader(
    title: String,
    icon: ImageVector,
    accentColor: Color = TealPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp, start = 2.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            letterSpacing = 0.2.sp
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = SlateOutline
        )
    }
}

@Composable
internal fun SettingsOverviewCard(
    schoolName: String,
    year: String,
    studentCount: Int,
    activeSchool: SchoolWorkspace?,
    currentUserId: String,
    isGeminiConfigured: Boolean,
    geminiModel: String
) {
    val isOwner = activeSchool?.ownerUid == currentUserId
    val roleText = when {
        activeSchool == null -> "লোকাল"
        isOwner -> "প্রতিষ্ঠাতা"
        else -> activeSchool.roleOf(currentUserId).labelBn
    }
    Surface(
        color = TealSoft,
        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.28f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = schoolName.ifBlank { "প্রাথমিক বিদ্যালয়" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = TealDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "শিক্ষাবর্ষ ${bn(year)} · ${bn(studentCount.toString())} জন শিক্ষার্থী",
                        fontSize = 11.5.sp,
                        color = SlateMuted
                    )
                }
                Surface(
                    color = TealPrimary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = roleText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (activeSchool != null) {
                    Surface(
                        color = Color.White,
                        border = BorderStroke(1.dp, SlateOutline),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                            Text("স্কুল কোড", fontSize = 10.sp, color = SlateMuted)
                            Text(
                                activeSchool.joinCode,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealDark,
                                maxLines = 1
                            )
                        }
                    }
                    Surface(
                        color = Color.White,
                        border = BorderStroke(1.dp, SlateOutline),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                            Text("সদস্য শিক্ষক", fontSize = 10.sp, color = SlateMuted)
                            Text(
                                "${bn(activeSchool.members.size.toString())} জন",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateText,
                                maxLines = 1
                            )
                        }
                    }
                }
                Surface(
                    color = Color.White,
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Column(Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                        Text("Gemini AI", fontSize = 10.sp, color = SlateMuted)
                        Text(
                            text = if (isGeminiConfigured) geminiModel else "সেটআপ বাকি",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isGeminiConfigured) EmeraldGrade else AmberNotice,
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
internal fun SettingsAutoBackupContent(
    backupStatus: AutoBackupStatus,
    onBackupNowBoth: () -> Unit,
    onToggleLocalAutoBackup: (Boolean) -> Unit,
    onPickLocalFolder: () -> Unit,
    onShowLocalSnapshots: () -> Unit,
    onToggleDriveAutoBackup: (Boolean) -> Unit,
    onShowDriveBackups: () -> Unit,
    formatTime: (Long) -> String
) {
    TreeBranchItem(isLast = false) {
        Button(
            onClick = onBackupNowBoth,
            enabled = !backupStatus.driveBusy,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_backup_both_now_btn")
        ) {
            if (backupStatus.driveBusy) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.size(8.dp))
            } else {
                Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
            }
            Text("এখনই লোকাল ও Google Drive-এ ব্যাকআপ নিন", fontSize = 12.5.sp)
        }
    }

    TreeBranchItem(isLast = false) {
        Surface(
            color = SlateBg,
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.SdStorage, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                    Column(Modifier.weight(1f)) {
                        Text("লোকাল ডিভাইস অটো ব্যাকআপ", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(backupStatus.localFolderLabel, fontSize = 11.sp, color = SlateMuted)
                    }
                    Switch(
                        checked = backupStatus.localEnabled,
                        onCheckedChange = onToggleLocalAutoBackup
                    )
                }

                if (backupStatus.lastLocalBackupMs > 0L) {
                    Text(
                        "সর্বশেষ লোকাল ব্যাকআপ: ${formatTime(backupStatus.lastLocalBackupMs)} (${backupStatus.lastLocalFileName})",
                        fontSize = 11.sp,
                        color = EmeraldGrade
                    )
                }
                if (!backupStatus.lastLocalError.isNullOrBlank()) {
                    Text("ত্রুটি: ${backupStatus.lastLocalError}", fontSize = 11.sp, color = RoseDanger)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onPickLocalFolder,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("ফোল্ডার বাছাই", fontSize = 11.5.sp)
                    }
                    OutlinedButton(
                        onClick = onShowLocalSnapshots,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("স্ন্যাপশট (${bn(backupStatus.localSnapshots.size.toString())})", fontSize = 11.5.sp)
                    }
                }
            }
        }
    }

    TreeBranchItem(isLast = true) {
        Surface(
            color = TealSoft,
            border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Google Drive অটো ব্যাকআপ", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TealDark)
                        Text("Google Drive → \"Result Maker Backups\" ফোল্ডারে স্বয়ংক্রিয় সিঙ্ক", fontSize = 11.sp, color = SlateMuted)
                    }
                    Switch(
                        checked = backupStatus.driveEnabled,
                        enabled = !backupStatus.driveBusy,
                        onCheckedChange = onToggleDriveAutoBackup
                    )
                }

                if (backupStatus.lastDriveBackupMs > 0L) {
                    Text(
                        "সর্বশেষ Drive ব্যাকআপ: ${formatTime(backupStatus.lastDriveBackupMs)} (${backupStatus.lastDriveFileName})",
                        fontSize = 11.sp,
                        color = EmeraldGrade
                    )
                }
                if (!backupStatus.lastDriveError.isNullOrBlank()) {
                    Surface(
                        color = RoseBg,
                        border = BorderStroke(1.dp, RoseDanger.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            backupStatus.lastDriveError,
                            fontSize = 11.sp,
                            color = RoseDanger,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                OutlinedButton(
                    onClick = onShowDriveBackups,
                    enabled = !backupStatus.driveBusy,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Google Drive ব্যাকআপ তালিকা ও রিস্টোর", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
internal fun SettingsCsvJsonContent(
    permissions: TeacherPermissions,
    isAdmin: Boolean,
    onExportAllPdf: () -> Unit,
    onExportCsv: () -> Unit,
    onExportCsvTemplate: () -> Unit,
    onOpenSmartCsvImport: () -> Unit,
    onPickCsvFile: () -> Unit,
    onExportJson: () -> Unit,
    onRestoreJsonFile: () -> Unit
) {
    TreeBranchItem(isLast = true) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (permissions.canExportPdf) {
                Button(
                    onClick = onExportAllPdf,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_all_pdf_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("সব শ্রেণির সব শিক্ষার্থীর PDF ডাউনলোড") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onExportCsv,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_csv_export_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("CSV এক্সপোর্ট", fontSize = 12.sp) }
                OutlinedButton(
                    onClick = onExportCsvTemplate,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_csv_template_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("খালি টেমপ্লেট", fontSize = 12.sp) }
            }
            if (permissions.canManageStudents) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onOpenSmartCsvImport,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_smart_csv_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("স্মার্ট CSV ইমপোর্ট", fontSize = 12.sp) }
                    OutlinedButton(
                        onClick = onPickCsvFile,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_pick_csv_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("CSV ফাইল খুলুন", fontSize = 12.sp) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onExportJson,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_json_export_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("JSON এক্সপোর্ট", fontSize = 12.sp) }
                if (isAdmin) {
                    OutlinedButton(
                        onClick = onRestoreJsonFile,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_json_restore_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("ফাইল থেকে রিস্টোর", fontSize = 12.sp) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsGeminiAiContent(
    apiKeyInput: String,
    onApiKeyChange: (String) -> Unit,
    showApiKey: Boolean,
    onToggleShowApiKey: () -> Unit,
    modelInput: String,
    onModelInputChange: (String) -> Unit,
    modelSearchQuery: String,
    onModelSearchChange: (String) -> Unit,
    showAllLiveModels: Boolean,
    onToggleShowAllLiveModels: () -> Unit,
    geminiApiKey: String,
    geminiModel: String,
    geminiAvailableModels: List<DiscoveredGeminiModel>,
    geminiCustomModels: List<String>,
    geminiNewModelIds: Set<String>,
    geminiRefreshingModels: Boolean,
    geminiModelsError: String?,
    geminiTesting: Boolean,
    geminiTestStatus: String?,
    geminiTestSuccess: Boolean?,
    onSaveGeminiConfig: (String, String) -> Unit,
    onTestGeminiConnection: (String, String) -> Unit,
    onRefreshGeminiModels: (String) -> Unit,
    onRemoveCustomGeminiModel: (String) -> Unit,
    onClearGeminiApiKey: () -> Unit
) {
    val suggestedIds = remember { GeminiApiConfig.SUGGESTED_MODEL_IDS }
    val normalizedTypedModel = remember(modelInput) {
        modelInput.trim().removePrefix("models/").trim()
    }
    val extraLiveModels = remember(geminiAvailableModels, geminiNewModelIds) {
        geminiAvailableModels
            .filter { it.id !in suggestedIds }
            .sortedWith(compareByDescending<DiscoveredGeminiModel> { it.id in geminiNewModelIds })
    }
    val filteredLiveModels = remember(extraLiveModels, modelSearchQuery) {
        val q = modelSearchQuery.trim().lowercase()
        if (q.isBlank()) extraLiveModels
        else extraLiveModels.filter {
            it.id.lowercase().contains(q) || it.displayName.lowercase().contains(q)
        }
    }

    TreeBranchItem(isLast = false) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Google AI Studio (aistudio.google.com/apikey) থেকে আপনার Gemini API Key এখানে দিন। কী শুধুমাত্র এই ডিভাইসে সংরক্ষিত থাকে এবং ছবি স্ক্যান, বাংলা OCR ও শিক্ষক চ্যাট সরাসরি Gemini API দিয়ে চলে।",
                fontSize = 12.sp,
                color = SlateMuted,
                lineHeight = 17.sp
            )
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = onApiKeyChange,
                label = { Text("Gemini API Key") },
                placeholder = { Text("AIza...") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                trailingIcon = {
                    IconButton(
                        onClick = onToggleShowApiKey,
                        modifier = Modifier.testTag("settings_gemini_key_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (showApiKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (showApiKey) "কী লুকান" else "কী দেখুন"
                        )
                    }
                },
                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_gemini_api_key_input")
            )
        }
    }

    TreeBranchItem(isLast = false) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "প্রস্তাবিত সর্বশেষ Gemini মডেল",
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp,
                color = SlateText
            )
            GeminiApiConfig.SUGGESTED_MODELS.forEach { preset ->
                val isSelected = GeminiApiConfig.normalizeModelId(modelInput) == preset.id
                Surface(
                    color = if (isSelected) TealContainer.copy(alpha = 0.65f) else SlateBg,
                    border = BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) TealPrimary else SlateOutline
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onModelInputChange(preset.id) }
                        .testTag("settings_model_preset_${preset.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onModelInputChange(preset.id) }
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = preset.title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                    color = SlateText
                                )
                                Surface(
                                    color = if (isSelected) TealContainer else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = preset.badge,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) TealDark else SlateMuted,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${preset.id} · ${preset.description}",
                                fontSize = 11.sp,
                                color = SlateMuted,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = SlateOutline, modifier = Modifier.padding(vertical = 2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "API থেকে পাওয়া নতুন ও অন্যান্য মডেল",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            color = SlateText
                        )
                        if (extraLiveModels.isNotEmpty()) {
                            Surface(color = TealSoft, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = "${bn(extraLiveModels.size.toString())}টি",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TealDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Google নতুন কোনো মডেল চালু করলে নির্বাচিত মডেলের বাইরেও এখানে স্বয়ংক্রিয়ভাবে দেখাবে",
                        fontSize = 11.sp,
                        color = SlateMuted,
                        lineHeight = 15.sp
                    )
                }
                TextButton(
                    onClick = { onRefreshGeminiModels(apiKeyInput.ifBlank { geminiApiKey }) },
                    enabled = !geminiRefreshingModels,
                    modifier = Modifier.testTag("settings_refresh_models_btn"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    if (geminiRefreshingModels) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = "নতুন মডেল খুঁজুন", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("রিফ্রেশ", fontSize = 11.5.sp)
                    }
                }
            }

            if (!geminiModelsError.isNullOrBlank()) {
                Text(text = geminiModelsError, fontSize = 11.sp, color = RoseDanger, lineHeight = 15.sp)
            }

            if (geminiNewModelIds.isNotEmpty()) {
                Surface(
                    color = TealSoft,
                    border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "নতুন মডেল শনাক্ত হয়েছে: ${geminiNewModelIds.joinToString(", ")}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TealDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            if (extraLiveModels.isNotEmpty()) {
                if (extraLiveModels.size > 6) {
                    OutlinedTextField(
                        value = modelSearchQuery,
                        onValueChange = onModelSearchChange,
                        placeholder = { Text("নতুন মডেল খুঁজুন (যেমন: 4.5, pro, flash)...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (modelSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { onModelSearchChange("") }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "খোঁজ মুছুন", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_live_model_search_input")
                    )
                }

                val visibleLiveModels = if (showAllLiveModels || modelSearchQuery.isNotBlank()) {
                    filteredLiveModels
                } else {
                    filteredLiveModels.take(10)
                }

                if (visibleLiveModels.isEmpty()) {
                    Text(
                        text = "\"$modelSearchQuery\"-এর সাথে মিল পাওয়া যায়নি। নিচে সরাসরি কাস্টম নাম লিখে ব্যবহার করতে পারেন।",
                        fontSize = 11.5.sp,
                        color = SlateMuted
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        visibleLiveModels.forEach { item ->
                            val selected = GeminiApiConfig.normalizeModelId(modelInput) == item.id
                            val isNew = item.id in geminiNewModelIds
                            FilterChip(
                                selected = selected,
                                onClick = { onModelInputChange(item.id) },
                                colors = appFilterChipColors(),
                                border = appFilterChipBorder(selected),
                                label = {
                                    Text(
                                        text = if (isNew) "নতুন · ${item.id}" else item.id,
                                        fontSize = 11.sp,
                                        fontWeight = if (isNew || selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }

                    val selectedDiscovered = extraLiveModels.find {
                        it.id == GeminiApiConfig.normalizeModelId(modelInput)
                    }
                    if (selectedDiscovered != null && (selectedDiscovered.displayName.isNotBlank() || selectedDiscovered.description.isNotBlank())) {
                        Surface(
                            color = TealSoft,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                if (selectedDiscovered.displayName.isNotBlank()) {
                                    Text(
                                        text = "${selectedDiscovered.displayName} (${selectedDiscovered.id})",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TealDark
                                    )
                                }
                                if (selectedDiscovered.description.isNotBlank()) {
                                    Text(
                                        text = selectedDiscovered.description,
                                        fontSize = 11.sp,
                                        color = SlateMuted,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    if (modelSearchQuery.isBlank() && filteredLiveModels.size > 10) {
                        TextButton(
                            onClick = onToggleShowAllLiveModels,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("settings_toggle_all_live_models_btn")
                        ) {
                            Text(
                                text = if (showAllLiveModels) "কম দেখুন" else "আরও ${bn((filteredLiveModels.size - 10).toString())}টি নতুন/অন্যান্য মডেল দেখুন",
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = if (apiKeyInput.isNotBlank() || geminiApiKey.isNotBlank()) {
                        "রিফ্রেশ বা 'পরীক্ষা ও মডেল লোড' চাপলে আপনার API Key-এ চালু থাকা সব নতুন ও অন্যান্য মডেল এখানে তালিকায় দেখাবে।"
                    } else {
                        "উপরে API Key দিলে Google-এ নতুন চালু হওয়া সব মডেল স্বয়ংক্রিয়ভাবে এখানে লোড হবে।"
                    },
                    fontSize = 11.sp,
                    color = SlateMuted,
                    lineHeight = 15.sp
                )
            }

            HorizontalDivider(color = SlateOutline, modifier = Modifier.padding(vertical = 2.dp))

            if (geminiCustomModels.isNotEmpty()) {
                Text(
                    text = "পূর্বে ব্যবহৃত কাস্টম মডেল:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = SlateText
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    geminiCustomModels.forEach { customId ->
                        val selected = GeminiApiConfig.normalizeModelId(modelInput) == customId
                        FilterChip(
                            selected = selected,
                            onClick = { onModelInputChange(customId) },
                            colors = appFilterChipColors(),
                            border = appFilterChipBorder(selected),
                            label = {
                                Text(
                                    customId,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "$customId সরান",
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onRemoveCustomGeminiModel(customId) }
                                )
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = modelInput,
                onValueChange = onModelInputChange,
                label = { Text("কাস্টম বা নতুন মডেলের নাম / আইডি লিখুন") },
                placeholder = { Text("যেমন: ${GeminiApiConfig.DEFAULT_MODEL} বা gemini-4.5-pro") },
                supportingText = {
                    Text(
                        text = "তালিকার বাইরে যেকোনো কাস্টম মডেলের নাম সরাসরি এখানে লিখে সংরক্ষণ ও ব্যবহার করতে পারবেন।",
                        fontSize = 11.sp
                    )
                },
                trailingIcon = {
                    if (modelInput.isNotBlank() && normalizedTypedModel != GeminiApiConfig.DEFAULT_MODEL) {
                        IconButton(
                            onClick = { onModelInputChange(GeminiApiConfig.DEFAULT_MODEL) },
                            modifier = Modifier.testTag("settings_reset_default_model_btn")
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "ডিফল্ট মডেলে ফিরুন",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_gemini_model_input")
            )

            if (normalizedTypedModel.isNotBlank() && normalizedTypedModel !in suggestedIds) {
                val isSavedActive = normalizedTypedModel == geminiModel
                Surface(
                    color = if (isSavedActive) TealSoft else AmberBg,
                    border = BorderStroke(
                        1.dp,
                        if (isSavedActive) TealPrimary.copy(alpha = 0.4f) else AmberBorder
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isSavedActive) {
                                "কাস্টম / নির্বাচিত মডেল সক্রিয়: $normalizedTypedModel"
                            } else {
                                "প্রস্তুত কাস্টম মডেল: $normalizedTypedModel (চালু করতে 'সংরক্ষণ' চাপুন)"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSavedActive) TealDark else AmberNotice,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    TreeBranchItem(isLast = true) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onSaveGeminiConfig(apiKeyInput, modelInput) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_gemini_save_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("সংরক্ষণ", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { onTestGeminiConnection(apiKeyInput, modelInput) },
                    enabled = !geminiTesting,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_gemini_test_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (geminiTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    } else {
                        Text("পরীক্ষা ও মডেল লোড", fontSize = 12.sp)
                    }
                }
            }

            if (!geminiTestStatus.isNullOrBlank()) {
                val isSuccess = geminiTestSuccess == true
                Surface(
                    color = if (isSuccess) TealSoft else RoseBg,
                    border = BorderStroke(
                        1.dp,
                        if (isSuccess) TealPrimary.copy(alpha = 0.4f) else RoseDanger.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isSuccess) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = TealDark,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = geminiTestStatus,
                            fontSize = 12.sp,
                            color = if (isSuccess) TealDark else RoseDanger,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            if (geminiApiKey.isNotBlank() || apiKeyInput.isNotBlank()) {
                OutlinedButton(
                    onClick = onClearGeminiApiKey,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_gemini_clear_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("API Key মুছে ফেলুন", fontSize = 12.sp, color = RoseDanger)
                }
            }
        }
    }
}
