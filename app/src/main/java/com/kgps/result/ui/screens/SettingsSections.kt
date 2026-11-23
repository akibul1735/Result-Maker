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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.CsvHeaderStyle
import com.kgps.result.activeClassIds
import com.kgps.result.ai.DiscoveredGeminiModel
import com.kgps.result.ai.GeminiApiConfig
import com.kgps.result.backup.AutoBackupStatus
import com.kgps.result.backup.BackupScheduleCalculator
import com.kgps.result.backup.BackupScheduleConfig
import com.kgps.result.backup.BackupScheduleMode
import com.kgps.result.backup.BackupSystemKind
import com.kgps.result.backup.CsvBackupFormat
import com.kgps.result.bn
import com.kgps.result.classShortBn
import com.kgps.result.cloud.SchoolWorkspace
import com.kgps.result.cloud.TeacherPermissions
import java.util.Calendar
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun BackupScheduleControl(
    kind: BackupSystemKind,
    titleBn: String,
    schedule: BackupScheduleConfig,
    enabled: Boolean,
    segmentBackgroundColor: Color,
    onUpdateSchedule: (BackupScheduleConfig) -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }

    val activeBadgeText = when (schedule.mode) {
        BackupScheduleMode.ON_CHANGE -> "তাৎক্ষণিক"
        BackupScheduleMode.EVERY_6_HOURS -> "৬ ঘণ্টা পরপর"
        BackupScheduleMode.EVERY_12_HOURS -> "১২ ঘণ্টা পরপর"
        BackupScheduleMode.DAILY -> "প্রতিদিন · ${BackupScheduleCalculator.formatTime12hBn(schedule.hour, schedule.minute)}"
        BackupScheduleMode.WEEKLY -> "${BackupScheduleCalculator.formatWeekdayShortBn(schedule.dayOfWeek)} · ${BackupScheduleCalculator.formatTime12hBn(schedule.hour, schedule.minute)}"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        HorizontalDivider(color = SlateOutline.copy(alpha = 0.65f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "সময়সূচি:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateText
                )
            }
            Surface(
                color = if (enabled) TealSoft else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = activeBadgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) TealDark else SlateMuted,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }

        // Single-row compact segmented selector — all 5 modes always stay on one line
        Surface(
            color = segmentBackgroundColor,
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(9.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackupScheduleMode.entries.forEach { mode ->
                    val selected = schedule.mode == mode
                    val segWeight = when (mode) {
                        BackupScheduleMode.ON_CHANGE -> 1.24f
                        BackupScheduleMode.EVERY_6_HOURS -> 0.90f
                        BackupScheduleMode.EVERY_12_HOURS -> 0.94f
                        BackupScheduleMode.DAILY -> 0.94f
                        BackupScheduleMode.WEEKLY -> 1.04f
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(segWeight)
                            .heightIn(min = 38.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (selected) TealPrimary else Color.Transparent)
                            .clickable { onUpdateSchedule(schedule.copy(mode = mode)) }
                            .testTag("settings_sched_${kind.name.lowercase()}_${mode.id}")
                            .padding(horizontal = 2.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = mode.shortBn,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color.White else SlateText,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Weekday 1-row bar (shown only for Weekly)
        AnimatedVisibility(visible = schedule.mode == BackupScheduleMode.WEEKLY) {
            val weekDays = listOf(
                Calendar.SATURDAY,
                Calendar.SUNDAY,
                Calendar.MONDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY
            )
            Surface(
                color = segmentBackgroundColor,
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    weekDays.forEach { dow ->
                        val selected = schedule.dayOfWeek == dow
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 34.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) TealPrimary else Color.Transparent)
                                .clickable { onUpdateSchedule(schedule.copy(dayOfWeek = dow)) }
                                .padding(horizontal = 1.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = BackupScheduleCalculator.formatWeekdayShortBn(dow),
                                fontSize = 9.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color.White else SlateText,
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Clock time row (strictly hidden for Instant / ON_CHANGE)
        AnimatedVisibility(visible = schedule.mode != BackupScheduleMode.ON_CHANGE) {
            val timeFieldLabel = when (schedule.mode) {
                BackupScheduleMode.EVERY_6_HOURS -> "শুরুর সময় (প্রতি ৬ ঘণ্টা):"
                BackupScheduleMode.EVERY_12_HOURS -> "শুরুর সময় (প্রতি ১২ ঘণ্টা):"
                BackupScheduleMode.DAILY -> "প্রতিদিন ব্যাকআপের সময়:"
                BackupScheduleMode.WEEKLY -> "প্রতি ${BackupScheduleCalculator.formatWeekdayFullBn(schedule.dayOfWeek)} সময়:"
                BackupScheduleMode.ON_CHANGE -> ""
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = timeFieldLabel,
                    fontSize = 10.5.sp,
                    color = SlateText,
                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                )
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier.testTag("settings_backup_time_btn_${kind.name.lowercase()}")
                ) {
                    Icon(Icons.Rounded.AccessTime, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = BackupScheduleCalculator.formatTime12hBn(schedule.hour, schedule.minute),
                        fontSize = 10.5.sp
                    )
                }
            }
        }

        val scheduleHint = when {
            !enabled -> "সুইচ বন্ধ আছে · চালু করলে \"$activeBadgeText\" নিয়মে চলবে"
            schedule.mode == BackupScheduleMode.ON_CHANGE ->
                "ডাটা বদলালেই সাথে সাথে ব্যাকআপ হবে (আলাদা সময়ের দরকার নেই)"
            else -> "পরবর্তী ব্যাকআপ: ${BackupScheduleCalculator.formatNextRunBn(schedule)}"
        }
        Text(
            text = scheduleHint,
            fontSize = 10.sp,
            lineHeight = 13.5.sp,
            color = if (enabled) TealDark else SlateMuted
        )
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = schedule.hour,
            initialMinute = schedule.minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Text(
                    "$titleBn · ${schedule.mode.shortBn} সময়",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TimePicker(state = pickerState)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            8 to 0 to "সকাল ৮:০০",
                            14 to 0 to "দুপুর ২:০০",
                            20 to 0 to "রাত ৮:০০",
                            22 to 0 to "রাত ১০:০০"
                        ).forEach { (hm, label) ->
                            OutlinedButton(
                                onClick = {
                                    val nextMode = if (schedule.mode == BackupScheduleMode.ON_CHANGE) {
                                        BackupScheduleMode.DAILY
                                    } else {
                                        schedule.mode
                                    }
                                    onUpdateSchedule(
                                        schedule.copy(
                                            mode = nextMode,
                                            hour = hm.first,
                                            minute = hm.second
                                        )
                                    )
                                    showTimePicker = false
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(label, fontSize = 10.5.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nextMode = if (schedule.mode == BackupScheduleMode.ON_CHANGE) {
                            BackupScheduleMode.DAILY
                        } else {
                            schedule.mode
                        }
                        onUpdateSchedule(
                            schedule.copy(
                                mode = nextMode,
                                hour = pickerState.hour,
                                minute = pickerState.minute
                            )
                        )
                        showTimePicker = false
                    },
                    modifier = Modifier.testTag("settings_backup_time_confirm_btn")
                ) { Text("সেট করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("বাতিল") }
            }
        )
    }
}

@Composable
internal fun CsvHeaderStyleSelector(
    selectedStyle: CsvHeaderStyle,
    onSelectStyle: (CsvHeaderStyle) -> Unit,
    segmentBackgroundColor: Color = Color.White
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "CSV কলাম হেডার ফরম্যাট (সহজ পাঠযোগ্য)",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = SlateText
        )
        // Single-row 3-option segmented bar — never wraps "সিস্টেম কোড" onto a second line
        Surface(
            color = segmentBackgroundColor,
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(9.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CsvHeaderStyle.entries.forEach { style ->
                    val selected = style == selectedStyle
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 38.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (selected) TealPrimary else Color.Transparent)
                            .clickable { onSelectStyle(style) }
                            .testTag("settings_csv_header_${style.id}")
                            .padding(horizontal = 3.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = style.shortBn,
                            fontSize = 10.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color.White else SlateText,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        Text(
            text = "উদাহরণ: ${selectedStyle.previewBn}",
            fontSize = 9.5.sp,
            color = SlateMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun SettingsAutoBackupContent(
    backupStatus: AutoBackupStatus,
    onBackupNowAll: () -> Unit,
    onBackupLocalNow: () -> Unit,
    onBackupDriveNow: () -> Unit,
    onBackupCsvNow: () -> Unit,
    onToggleLocalAutoBackup: (Boolean) -> Unit,
    onPickLocalFolder: () -> Unit,
    onShowLocalSnapshots: () -> Unit,
    onToggleDriveAutoBackup: (Boolean) -> Unit,
    onShowDriveBackups: () -> Unit,
    onToggleCsvAutoBackup: (Boolean) -> Unit,
    onPickCsvFolder: () -> Unit,
    onShowCsvSnapshots: () -> Unit,
    onSelectCsvHeaderStyle: (CsvHeaderStyle) -> Unit,
    onSelectCsvBackupFormat: (CsvBackupFormat) -> Unit,
    onUpdateBackupSchedule: (BackupSystemKind, BackupScheduleConfig) -> Unit,
    formatTime: (Long) -> String
) {
    TreeBranchItem(isLast = false) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(
                onClick = onBackupNowAll,
                enabled = !backupStatus.driveBusy,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
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
                    Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(6.dp))
                }
                Text(
                    text = "এখনই ৩টি সিস্টেমে ব্যাকআপ (লোকাল · Drive · ${backupStatus.csvBackupFormat.shortBn})",
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "প্রতিটি সিস্টেমে ১টি নিয়ম বেছে নিন: \"তাৎক্ষণিক\" (ডাটা বদলালেই) অথবা নির্ধারিত সময়।",
                fontSize = 10.sp,
                lineHeight = 13.5.sp,
                color = SlateMuted
            )
        }
    }

    // 1. Local JSON Auto Backup
    TreeBranchItem(isLast = false) {
        Surface(
            color = SlateBg,
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.SdStorage, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(19.dp))
                    Column(Modifier.weight(1f)) {
                        Text("১. লোকাল ডিভাইস (JSON)", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                        Text(backupStatus.localFolderLabel, fontSize = 10.sp, color = SlateMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Switch(
                        checked = backupStatus.localEnabled,
                        onCheckedChange = onToggleLocalAutoBackup,
                        modifier = Modifier.testTag("settings_local_backup_switch")
                    )
                }

                if (backupStatus.lastLocalBackupMs > 0L) {
                    Text(
                        "সর্বশেষ: ${formatTime(backupStatus.lastLocalBackupMs)} (${backupStatus.lastLocalFileName})",
                        fontSize = 10.sp,
                        color = EmeraldGrade,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!backupStatus.lastLocalError.isNullOrBlank()) {
                    Text("ত্রুটি: ${backupStatus.lastLocalError}", fontSize = 10.sp, color = RoseDanger)
                }

                BackupScheduleControl(
                    kind = BackupSystemKind.LOCAL,
                    titleBn = "লোকাল JSON ব্যাকআপ",
                    schedule = backupStatus.localSchedule,
                    enabled = backupStatus.localEnabled,
                    segmentBackgroundColor = Color.White,
                    onUpdateSchedule = { cfg -> onUpdateBackupSchedule(BackupSystemKind.LOCAL, cfg) }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onPickLocalFolder,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.weight(0.95f)
                    ) {
                        Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.size(3.dp))
                        Text("ফোল্ডার", fontSize = 10.5.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onShowLocalSnapshots,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1.15f)
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.size(3.dp))
                        Text("স্ন্যাপশট (${bn(backupStatus.localSnapshots.size.toString())})", fontSize = 10.5.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onBackupLocalNow,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.weight(0.9f).testTag("settings_backup_local_now_btn")
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.size(2.dp))
                        Text("এখনই", fontSize = 10.5.sp, maxLines = 1)
                    }
                }
            }
        }
    }

    // 2. Google Drive Auto Backup
    TreeBranchItem(isLast = false) {
        Surface(
            color = TealSoft,
            border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(19.dp))
                    Column(Modifier.weight(1f)) {
                        Text("২. Google Drive অটো ব্যাকআপ", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = TealDark)
                        Text("Google Drive → \"Result Maker Backups\" ফোল্ডার", fontSize = 10.sp, color = SlateMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Switch(
                        checked = backupStatus.driveEnabled,
                        enabled = !backupStatus.driveBusy,
                        onCheckedChange = onToggleDriveAutoBackup,
                        modifier = Modifier.testTag("settings_drive_backup_switch")
                    )
                }

                if (backupStatus.lastDriveBackupMs > 0L) {
                    Text(
                        "সর্বশেষ: ${formatTime(backupStatus.lastDriveBackupMs)} (${backupStatus.lastDriveFileName})",
                        fontSize = 10.sp,
                        color = EmeraldGrade,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                            fontSize = 10.sp,
                            color = RoseDanger,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }

                BackupScheduleControl(
                    kind = BackupSystemKind.DRIVE,
                    titleBn = "Google Drive ব্যাকআপ",
                    schedule = backupStatus.driveSchedule,
                    enabled = backupStatus.driveEnabled,
                    segmentBackgroundColor = Color.White,
                    onUpdateSchedule = { cfg -> onUpdateBackupSchedule(BackupSystemKind.DRIVE, cfg) }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onShowDriveBackups,
                        enabled = !backupStatus.driveBusy,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.35f)
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Drive তালিকা ও রিস্টোর", fontSize = 10.5.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onBackupDriveNow,
                        enabled = !backupStatus.driveBusy,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(0.85f).testTag("settings_backup_drive_now_btn")
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.size(2.dp))
                        Text("এখনই", fontSize = 10.5.sp, maxLines = 1)
                    }
                }
            }
        }
    }

    // 3. CSV or Multi-Sheet Excel Auto Backup (1 single file per backup)
    TreeBranchItem(isLast = true) {
        Surface(
            color = SlateBg,
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.TableChart, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(19.dp))
                    Column(Modifier.weight(1f)) {
                        Text("৩. CSV / Excel অটো ব্যাকআপ", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                        Text(backupStatus.csvFolderLabel, fontSize = 10.sp, color = SlateMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Switch(
                        checked = backupStatus.csvEnabled,
                        onCheckedChange = onToggleCsvAutoBackup,
                        modifier = Modifier.testTag("settings_csv_backup_switch")
                    )
                }

                if (backupStatus.lastCsvBackupMs > 0L) {
                    Text(
                        "সর্বশেষ: ${formatTime(backupStatus.lastCsvBackupMs)} (${backupStatus.lastCsvFileName})",
                        fontSize = 10.sp,
                        color = EmeraldGrade,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!backupStatus.lastCsvError.isNullOrBlank()) {
                    Text("ত্রুটি: ${backupStatus.lastCsvError}", fontSize = 10.sp, color = RoseDanger)
                }

                // Auto backup output format choice: single CSV (all classes) vs single Excel (per-class sheets)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HorizontalDivider(color = SlateOutline.copy(alpha = 0.65f))
                    Text(
                        text = "অটো ব্যাকআপ ফরম্যাট (যেকোনো ১টি):",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateText
                    )
                    Surface(
                        color = Color.White,
                        border = BorderStroke(1.dp, SlateOutline),
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CsvBackupFormat.entries.forEach { fmt ->
                                val selected = backupStatus.csvBackupFormat == fmt
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 38.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(if (selected) TealPrimary else Color.Transparent)
                                        .clickable { onSelectCsvBackupFormat(fmt) }
                                        .testTag("settings_csv_auto_fmt_${fmt.id}")
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = fmt.shortBn,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) Color.White else SlateText,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = backupStatus.csvBackupFormat.descBn,
                        fontSize = 9.5.sp,
                        color = TealDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                BackupScheduleControl(
                    kind = BackupSystemKind.CSV,
                    titleBn = "CSV / Excel অটো ব্যাকআপ",
                    schedule = backupStatus.csvSchedule,
                    enabled = backupStatus.csvEnabled,
                    segmentBackgroundColor = Color.White,
                    onUpdateSchedule = { cfg -> onUpdateBackupSchedule(BackupSystemKind.CSV, cfg) }
                )

                HorizontalDivider(color = SlateOutline.copy(alpha = 0.65f))

                CsvHeaderStyleSelector(
                    selectedStyle = backupStatus.csvHeaderStyle,
                    onSelectStyle = onSelectCsvHeaderStyle,
                    segmentBackgroundColor = Color.White
                )

                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onPickCsvFolder,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.size(3.dp))
                        Text("ফোল্ডার", fontSize = 10.5.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onShowCsvSnapshots,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1.05f).testTag("settings_csv_snapshots_btn")
                    ) {
                        Icon(Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.size(3.dp))
                        Text("ফাইল (${bn(backupStatus.csvSnapshots.size.toString())})", fontSize = 10.5.sp, maxLines = 1)
                    }
                    Button(
                        onClick = onBackupCsvNow,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1.15f).testTag("settings_backup_csv_now_btn")
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.size(2.dp))
                        Text("তাৎক্ষণিক এক্সপোর্ট", fontSize = 10.5.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsCsvJsonContent(
    permissions: TeacherPermissions,
    isAdmin: Boolean,
    availableClassIds: List<Int> = activeClassIds(),
    studentCountByClass: Map<Int, Int>,
    csvHeaderStyle: CsvHeaderStyle,
    onSelectCsvHeaderStyle: (CsvHeaderStyle) -> Unit,
    onInstantCsvExport: () -> Unit,
    onExportAllPdf: () -> Unit,
    onExportAllCsvSingleFile: () -> Unit,
    onExportExcelMultiTab: () -> Unit,
    onExportCsvForClass: (Int) -> Unit,
    onExportCsvTemplateForClass: (Int) -> Unit,
    onExportPerClassCsvZip: () -> Unit,
    onOpenSmartCsvImport: () -> Unit,
    onOpenBasicStudentImport: () -> Unit = {},
    onPickCsvFile: () -> Unit,
    onExportJson: () -> Unit,
    onRestoreJsonFile: () -> Unit
) {
    val allowedClasses = remember(permissions, availableClassIds) {
        permissions.effectiveClasses(availableClassIds).ifEmpty { availableClassIds }
    }
    var selectedExportCls by remember(allowedClasses) {
        mutableStateOf(allowedClasses.first())
    }

    TreeBranchItem(isLast = true) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (permissions.canExportPdf) {
                Button(
                    onClick = onExportAllPdf,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_all_pdf_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("সব শ্রেণির সব শিক্ষার্থীর PDF ডাউনলোড", fontSize = 12.sp) }
            }

            Surface(
                color = SlateBg,
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CsvHeaderStyleSelector(
                        selectedStyle = csvHeaderStyle,
                        onSelectStyle = onSelectCsvHeaderStyle,
                        segmentBackgroundColor = Color.White
                    )

                    HorizontalDivider(color = SlateOutline.copy(alpha = 0.65f))

                    Text(
                        text = "CSV ও Excel এক্সপোর্ট (১টি ফাইলে সব শ্রেণি)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TealDark
                    )

                    Button(
                        onClick = onInstantCsvExport,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_instant_csv_export_btn")
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(5.dp))
                        Text("তাৎক্ষণিক এক্সপোর্ট (CSV / Excel বেছে নিন)", fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onExportAllCsvSingleFile,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 5.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_csv_all_single_btn")
                        ) {
                            Text("সব শ্রেণি ১টি CSV", fontSize = 11.sp, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onExportExcelMultiTab,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 5.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("settings_xlsx_multitab_export_btn")
                        ) {
                            Text("Excel (আলাদা শিট)", fontSize = 11.sp, maxLines = 1)
                        }
                    }

                    // Single-row horizontally scrollable class selector pills
                    Surface(
                        color = Color.White,
                        border = BorderStroke(1.dp, SlateOutline),
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            allowedClasses.forEach { c ->
                                val selected = selectedExportCls == c
                                val count = studentCountByClass[c] ?: 0
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .heightIn(min = 38.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(if (selected) TealPrimary else Color.Transparent)
                                        .clickable { selectedExportCls = c }
                                        .testTag("settings_csv_class_chip_$c")
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${classShortBn(c)} (${bn(count.toString())})",
                                        fontSize = 10.5.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) Color.White else SlateText,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    val clsLabel = classShortBn(selectedExportCls)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { onExportCsvForClass(selectedExportCls) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_csv_export_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("$clsLabel শ্রেণি CSV", fontSize = 11.sp, maxLines = 1) }
                        OutlinedButton(
                            onClick = { onExportCsvTemplateForClass(selectedExportCls) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_csv_template_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("$clsLabel টেমপ্লেট", fontSize = 11.sp, maxLines = 1) }
                    }

                    OutlinedButton(
                        onClick = onExportPerClassCsvZip,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_csv_zip_export_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("সব শ্রেণির আলাদা CSV ফাইল (ZIP আর্কাইভ)", fontSize = 11.sp, maxLines = 1) }
                }
            }
            if (permissions.canManageStudents) {
                Button(
                    onClick = onOpenBasicStudentImport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("settings_basic_import_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("শিক্ষার্থীর পূর্ণাঙ্গ বেসিক ডেটা ইমপোর্ট (.xlsx / CSV)", fontSize = 12.sp)
                }
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
                    ) { Text("CSV / Excel খুলুন", fontSize = 12.sp) }
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
    var editingApiKey by remember(geminiApiKey) { mutableStateOf(geminiApiKey.isBlank()) }
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

    // 1. Compact API Key Section (auto-hides after saving, with a "Change" button)
    TreeBranchItem(isLast = false) {
        if (geminiApiKey.isNotBlank() && !editingApiKey) {
            Surface(
                color = TealSoft,
                border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldGrade,
                        modifier = Modifier.size(16.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Gemini API Key সংরক্ষিত",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TealDark
                        )
                        val maskedSuffix = if (geminiApiKey.length >= 4) "••••${geminiApiKey.takeLast(4)}" else "••••"
                        Text(
                            text = maskedSuffix,
                            fontSize = 10.5.sp,
                            color = SlateMuted
                        )
                    }
                    OutlinedButton(
                        onClick = { editingApiKey = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("settings_gemini_change_key_btn")
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("পরিবর্তন", fontSize = 11.sp)
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "aistudio.google.com/apikey থেকে কী দিন (শুধু এই ডিভাইসে থাকে)",
                    fontSize = 11.sp,
                    color = SlateMuted
                )
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = onApiKeyChange,
                    label = { Text("Gemini API Key", fontSize = 12.sp) },
                    placeholder = { Text("AIza...", fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 12.5.sp),
                    leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        IconButton(
                            onClick = onToggleShowApiKey,
                            modifier = Modifier.testTag("settings_gemini_key_toggle_btn")
                        ) {
                            Icon(
                                imageVector = if (showApiKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (showApiKey) "কী লুকান" else "কী দেখুন",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_gemini_api_key_input")
                )
                if (geminiApiKey.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                onApiKeyChange(geminiApiKey)
                                editingApiKey = false
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("বাতিল", fontSize = 11.sp, color = SlateMuted)
                        }
                        TextButton(
                            onClick = onClearGeminiApiKey,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("settings_gemini_clear_btn")
                        ) {
                            Text("API Key মুছুন", fontSize = 11.sp, color = RoseDanger)
                        }
                    }
                }
            }
        }
    }

    // 2. Ultra-Compact Model Selector Section
    TreeBranchItem(isLast = false) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "প্রস্তাবিত মডেল",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = SlateText
                )
                Text(
                    text = "সক্রিয়: $geminiModel",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = TealPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Compact 2x2 Grid for Suggested Models
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                GeminiApiConfig.SUGGESTED_MODELS.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        pair.forEach { preset ->
                            val isSelected = GeminiApiConfig.normalizeModelId(modelInput) == preset.id
                            val shortTitle = preset.title.removePrefix("Gemini ")
                            val shortBadge = when {
                                preset.badge.contains("ডিফল্ট") -> "ডিফল্ট"
                                preset.badge.contains("দ্রুত") -> "দ্রুত"
                                preset.badge.contains("উন্নত") -> "উন্নত"
                                else -> "নতুন"
                            }
                            Surface(
                                color = if (isSelected) TealContainer.copy(alpha = 0.65f) else SlateBg,
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) TealPrimary else SlateOutline
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onModelInputChange(preset.id) }
                                    .testTag("settings_model_preset_${preset.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onModelInputChange(preset.id) },
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = shortTitle,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.5.sp,
                                                color = SlateText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Surface(
                                                color = if (isSelected) TealPrimary else MaterialTheme.colorScheme.surface,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = shortBadge,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isSelected) Color.White else SlateMuted,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = preset.id,
                                            fontSize = 9.5.sp,
                                            color = SlateMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            HorizontalDivider(color = SlateOutline.copy(alpha = 0.7f))

            // Compact Live API-Discovered Models Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "লাইভ মডেল",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp,
                    color = SlateText
                )
                if (extraLiveModels.isNotEmpty()) {
                    Surface(color = TealSoft, shape = RoundedCornerShape(5.dp)) {
                        Text(
                            text = "${bn(extraLiveModels.size.toString())}টি",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TealDark,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                if (extraLiveModels.size > 4) {
                    Text(
                        text = if (showAllLiveModels) "সংক্ষিপ্ত" else "সব ও খুঁজুন",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TealPrimary,
                        modifier = Modifier
                            .clickable { onToggleShowAllLiveModels() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .testTag("settings_toggle_all_live_models_btn")
                    )
                }
                Surface(
                    color = SlateBg,
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .clickable(enabled = !geminiRefreshingModels) {
                            onRefreshGeminiModels(apiKeyInput.ifBlank { geminiApiKey })
                        }
                        .testTag("settings_refresh_models_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (geminiRefreshingModels) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.8.dp)
                        } else {
                            Icon(Icons.Rounded.Refresh, contentDescription = "নতুন মডেল খুঁজুন", tint = TealPrimary, modifier = Modifier.size(13.dp))
                        }
                        Text("রিফ্রেশ", fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = TealDark)
                    }
                }
            }

            if (!geminiModelsError.isNullOrBlank()) {
                Text(text = geminiModelsError, fontSize = 10.5.sp, color = RoseDanger)
            }

            if (geminiNewModelIds.isNotEmpty()) {
                Text(
                    text = "নতুন: ${geminiNewModelIds.joinToString(", ")}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = TealDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (extraLiveModels.isNotEmpty()) {
                if (showAllLiveModels) {
                    if (extraLiveModels.size > 6) {
                        OutlinedTextField(
                            value = modelSearchQuery,
                            onValueChange = onModelSearchChange,
                            placeholder = { Text("মডেল খুঁজুন (যেমন: flash, pro)...", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            trailingIcon = {
                                if (modelSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onModelSearchChange("") }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "খোঁজ মুছুন", modifier = Modifier.size(15.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 11.5.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_live_model_search_input")
                        )
                    }

                    if (filteredLiveModels.isEmpty()) {
                        Text(
                            text = "\"$modelSearchQuery\"-এর সাথে মিল পাওয়া যায়নি।",
                            fontSize = 10.5.sp,
                            color = SlateMuted
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            filteredLiveModels.forEach { item ->
                                val selected = GeminiApiConfig.normalizeModelId(modelInput) == item.id
                                val isNew = item.id in geminiNewModelIds
                                Surface(
                                    color = if (selected) TealPrimary else SlateBg,
                                    border = BorderStroke(1.dp, if (selected) TealPrimary else SlateOutline),
                                    shape = RoundedCornerShape(7.dp),
                                    modifier = Modifier.clickable { onModelInputChange(item.id) }
                                ) {
                                    Text(
                                        text = if (isNew) "নতুন · ${item.id}" else item.id,
                                        fontSize = 10.sp,
                                        fontWeight = if (isNew || selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.White else SlateText,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Single compact horizontal strip by default
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        extraLiveModels.take(8).forEach { item ->
                            val selected = GeminiApiConfig.normalizeModelId(modelInput) == item.id
                            val isNew = item.id in geminiNewModelIds
                            Surface(
                                color = if (selected) TealPrimary else SlateBg,
                                border = BorderStroke(1.dp, if (selected) TealPrimary else SlateOutline),
                                shape = RoundedCornerShape(7.dp),
                                modifier = Modifier.clickable { onModelInputChange(item.id) }
                            ) {
                                Text(
                                    text = if (isNew) "নতুন · ${item.id}" else item.id,
                                    fontSize = 10.sp,
                                    fontWeight = if (isNew || selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Color.White else SlateText,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (geminiCustomModels.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("কাস্টম:", fontSize = 10.sp, color = SlateMuted, fontWeight = FontWeight.Medium)
                    geminiCustomModels.forEach { customId ->
                        val selected = GeminiApiConfig.normalizeModelId(modelInput) == customId
                        Surface(
                            color = if (selected) TealContainer else SlateBg,
                            border = BorderStroke(1.dp, if (selected) TealPrimary else SlateOutline),
                            shape = RoundedCornerShape(7.dp),
                            modifier = Modifier.clickable { onModelInputChange(customId) }
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 7.dp, end = 5.dp, top = 3.dp, bottom = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    customId,
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) TealDark else SlateText
                                )
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "$customId সরান",
                                    tint = SlateMuted,
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clickable { onRemoveCustomGeminiModel(customId) }
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = modelInput,
                onValueChange = onModelInputChange,
                label = { Text("কাস্টম মডেল আইডি (ঐচ্ছিক)", fontSize = 11.sp) },
                placeholder = { Text(GeminiApiConfig.DEFAULT_MODEL, fontSize = 11.sp) },
                trailingIcon = {
                    if (modelInput.isNotBlank() && normalizedTypedModel != GeminiApiConfig.DEFAULT_MODEL) {
                        IconButton(
                            onClick = { onModelInputChange(GeminiApiConfig.DEFAULT_MODEL) },
                            modifier = Modifier.testTag("settings_reset_default_model_btn")
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "ডিফল্ট মডেলে ফিরুন",
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                textStyle = TextStyle(fontSize = 12.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_gemini_model_input")
            )
        }
    }

    // 3. Compact Save & Test Action Row
    TreeBranchItem(isLast = true) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        if (apiKeyInput.isNotBlank()) editingApiKey = false
                        onSaveGeminiConfig(apiKeyInput, modelInput)
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                    modifier = Modifier
                        .weight(0.85f)
                        .testTag("settings_gemini_save_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("সংরক্ষণ", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { onTestGeminiConnection(apiKeyInput, modelInput) },
                    enabled = !geminiTesting,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                    modifier = Modifier
                        .weight(1.15f)
                        .testTag("settings_gemini_test_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (geminiTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(13.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(5.dp))
                        Text("পরীক্ষা হচ্ছে...", fontSize = 11.sp)
                    } else {
                        Text("পরীক্ষা ও মডেল লোড", fontSize = 11.5.sp)
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
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isSuccess) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = TealDark,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = geminiTestStatus,
                            fontSize = 11.5.sp,
                            color = if (isSuccess) TealDark else RoseDanger,
                            lineHeight = 15.5.sp
                        )
                    }
                }
            }
        }
    }
}
