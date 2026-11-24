package com.kgps.result.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.BENGALI_APP_FONT_LIST
import com.kgps.result.BENGALI_PDF_FONT_LIST
import com.kgps.result.CsvHeaderStyle
import com.kgps.result.CustomClassDef
import com.kgps.result.CustomFieldDef
import com.kgps.result.DPE_LOGO_LIST
import com.kgps.result.ENGLISH_APP_FONT_LIST
import com.kgps.result.ENGLISH_PDF_FONT_LIST
import com.kgps.result.PdfGen
import com.kgps.result.Repo
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.activeClassIds
import com.kgps.result.activeSchoolClasses
import com.kgps.result.ai.DiscoveredGeminiModel
import com.kgps.result.backup.AutoBackupStatus
import com.kgps.result.backup.BackupScheduleConfig
import com.kgps.result.backup.BackupSystemKind
import com.kgps.result.backup.CsvBackupFormat
import com.kgps.result.backup.DriveBackupFile
import com.kgps.result.backup.LocalBackupEntry
import com.kgps.result.backup.XlsxExporter
import com.kgps.result.backup.XlsxImporter
import com.kgps.result.bn
import com.kgps.result.classNameBn
import com.kgps.result.DataResetCategory
import com.kgps.result.DataResetRequest
import com.kgps.result.cloud.JoinRequestItem
import com.kgps.result.cloud.MemberRole
import com.kgps.result.cloud.SchoolMemberProfile
import com.kgps.result.cloud.SchoolWorkspace
import com.kgps.result.cloud.SheetTemplateCache
import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.ui.components.AddCustomClassDialog
import com.kgps.result.ui.components.DataResetDialog
import com.kgps.result.ui.components.TeacherPermissionsEditorDialog
import com.kgps.result.ui.components.TreeBranchItem
import com.kgps.result.ui.components.TreeGroupCard
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
import com.kgps.result.ui.theme.appComposeFontFamily
import com.kgps.result.ui.theme.appFilterChipBorder
import com.kgps.result.ui.theme.appFilterChipColors
import com.kgps.result.ui.theme.buildMixedScriptAnnotatedString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    students: List<Student>,
    settings: Settings,
    currentUserId: String,
    activeSchool: SchoolWorkspace?,
    permissions: TeacherPermissions,
    joinRequests: List<JoinRequestItem>,
    backupStatus: AutoBackupStatus,
    driveFiles: List<DriveBackupFile>,
    driveFilesLoading: Boolean,
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
    appFontId: String = "system",
    appEnglishFontId: String = "system",
    onSelectAppFont: (String) -> Unit = {},
    onSelectAppEnglishFont: (String) -> Unit = {},
    onUpdateSettings: (Settings) -> Unit,
    onAddCustomField: (String) -> Boolean,
    onRemoveCustomField: (String) -> Unit,
    onAddCustomClass: (String, String, Int) -> Boolean = { _, _, _ -> false },
    onRemoveCustomClass: (Int) -> Unit = {},
    onOpenCsvImport: (String) -> Unit,
    onOpenBasicImport: () -> Unit = {},
    onRestoreBackupJson: (String) -> Unit,
    onBuildBackupJson: (() -> String)? = null,
    onExportAllPdf: () -> Unit,
    onResetSampleData: () -> Unit,
    onClearAllStudents: () -> Unit,
    onExecuteDataReset: (DataResetRequest) -> Unit,
    onRegenerateJoinCode: () -> Unit,
    onApproveJoinRequest: (JoinRequestItem, TeacherPermissions) -> Unit,
    onRejectJoinRequest: (String) -> Unit,
    onUpdateMemberPermissions: (String, TeacherPermissions) -> Unit,
    onUpdateMemberRole: (String, MemberRole) -> Unit,
    onRemoveMember: (String) -> Unit,
    onLeaveSchool: () -> Unit,
    onToggleLocalAutoBackup: (Boolean) -> Unit,
    onSetLocalBackupFolder: (Uri?) -> Unit,
    onToggleDriveAutoBackup: (Boolean) -> Unit,
    onToggleCsvAutoBackup: (Boolean) -> Unit,
    onSetCsvBackupFolder: (Uri?) -> Unit,
    onSelectCsvHeaderStyle: (CsvHeaderStyle) -> Unit,
    onSelectCsvBackupFormat: (CsvBackupFormat) -> Unit,
    onUpdateBackupSchedule: (BackupSystemKind, BackupScheduleConfig) -> Unit,
    onBackupNowAll: () -> Unit,
    onBackupLocalNow: () -> Unit,
    onBackupDriveNow: () -> Unit,
    onBackupCsvNow: (CsvBackupFormat?) -> Unit,
    onRestoreLocalSnapshot: (String) -> Unit,
    onLoadDriveBackups: () -> Unit,
    onRestoreFromDriveFile: (String) -> Unit,
    onSaveGeminiConfig: (String, String) -> Unit,
    onTestGeminiConnection: (String, String) -> Unit,
    onRefreshGeminiModels: (String) -> Unit,
    onRemoveCustomGeminiModel: (String) -> Unit,
    onClearGeminiApiKey: () -> Unit,
    onSignOut: () -> Unit,
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    BackHandler { onBack() }

    val isAdmin = activeSchool?.isAdmin(currentUserId) ?: true
    val isOwner = activeSchool?.ownerUid == currentUserId

    var s by remember(settings) { mutableStateOf(settings) }
    var selectedSettingsTab by remember { mutableIntStateOf(0) }
    // All settings groups start collapsed by default
    var expandedGroup by remember { mutableStateOf("") }
    var newFieldLabel by remember { mutableStateOf("") }
    var apiKeyInput by remember(geminiApiKey) { mutableStateOf(geminiApiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var modelInput by remember(geminiModel) { mutableStateOf(geminiModel) }
    var modelSearchQuery by remember { mutableStateOf("") }
    var showAllLiveModels by remember { mutableStateOf(false) }

    LaunchedEffect(expandedGroup, geminiApiKey) {
        if (expandedGroup == "aiproxy" && geminiApiKey.isNotBlank()) {
            onRefreshGeminiModels(geminiApiKey)
        }
    }

    var showDataResetWizard by remember { mutableStateOf(false) }
    var dataResetInitialCategory by remember { mutableStateOf<DataResetCategory?>(null) }
    var askRegenerateCode by remember { mutableStateOf(false) }
    var askLeaveSchool by remember { mutableStateOf(false) }
    var askSignOut by remember { mutableStateOf(false) }
    var pendingRemoveField by remember { mutableStateOf<CustomFieldDef?>(null) }
    var showAddCustomClassDialog by remember { mutableStateOf(false) }
    var pendingRemoveCustomClass by remember { mutableStateOf<CustomClassDef?>(null) }
    var pendingRemoveMember by remember { mutableStateOf<SchoolMemberProfile?>(null) }
    var pendingApproveRequest by remember { mutableStateOf<JoinRequestItem?>(null) }
    var pendingEditMember by remember { mutableStateOf<SchoolMemberProfile?>(null) }

    var showLocalSnapshotsDialog by remember { mutableStateOf(false) }
    var showCsvSnapshotsDialog by remember { mutableStateOf(false) }
    var showInstantExportDialog by remember { mutableStateOf(false) }
    var showDriveBackupsDialog by remember { mutableStateOf(false) }
    var confirmLocalRestore by remember { mutableStateOf<LocalBackupEntry?>(null) }
    var confirmDriveRestore by remember { mutableStateOf<DriveBackupFile?>(null) }
    var pendingManualJsonRestore by remember { mutableStateOf<String?>(null) }

    var pendingCsvExportCls by remember { mutableStateOf<Int?>(null) }
    var pendingCsvTemplateCls by remember { mutableStateOf(1) }

    val csvExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            try {
                val availableIds = activeClassIds(settings)
                val allowed = permissions.effectiveClasses(availableIds).ifEmpty { availableIds }
                val target = pendingCsvExportCls
                val listToExport = if (target == null) students.filter { it.cls in allowed } else students
                ctx.contentResolver.openOutputStream(uri)?.use {
                    it.write(
                        Repo.toCsv(
                            list = listToExport,
                            customFields = settings.customFields,
                            style = backupStatus.csvHeaderStyle,
                            targetCls = target
                        ).toByteArray(Charsets.UTF_8)
                    )
                }
                val msg = if (target == null) {
                    "১টি CSV ফাইলে সকল শ্রেণির ডাটা সংরক্ষিত হয়েছে"
                } else {
                    "${classNameBn(target, settings)} শ্রেণির CSV ফাইল সংরক্ষিত হয়েছে"
                }
                onShowMessage(msg)
            } catch (e: Exception) {
                onShowMessage("CSV তৈরি হয়নি: ${e.message ?: "ত্রুটি"}")
            }
        }
    }

    val csvTemplateLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            try {
                val hdr = Repo.toCsvTemplate(
                    customFields = settings.customFields,
                    style = backupStatus.csvHeaderStyle,
                    targetCls = pendingCsvTemplateCls
                )
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(hdr.toByteArray(Charsets.UTF_8)) }
                onShowMessage("${classNameBn(pendingCsvTemplateCls, settings)} শ্রেণির খালি CSV টেমপ্লেট তৈরি হয়েছে")
            } catch (e: Exception) {
                onShowMessage("টেমপ্লেট তৈরি হয়নি: ${e.message ?: "ত্রুটি"}")
            }
        }
    }

    val xlsxExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri != null) {
            try {
                val availableIds = (activeClassIds(settings) + students.map { it.cls }.filter { it in 1..20 }).distinct().sorted()
                val allowed = permissions.effectiveClasses(availableIds).ifEmpty { availableIds }
                val bytes = XlsxExporter.exportAllClassesWorkbook(
                    students = students.filter { it.cls in allowed },
                    customFields = settings.customFields,
                    style = backupStatus.csvHeaderStyle,
                    classes = allowed
                )
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                onShowMessage("প্রতিটি শ্রেণি আলাদা ট্যাবে Excel (.xlsx) সংরক্ষিত হয়েছে")
            } catch (e: Exception) {
                onShowMessage("Excel ফাইল তৈরি হয়নি: ${e.message ?: "ত্রুটি"}")
            }
        }
    }

    val csvZipExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            try {
                val availableIds = (activeClassIds(settings) + students.map { it.cls }.filter { it in 1..20 }).distinct().sorted()
                val allowed = permissions.effectiveClasses(availableIds).ifEmpty { availableIds }
                val bytes = Repo.toPerClassCsvZip(
                    list = students.filter { it.cls in allowed },
                    customFields = settings.customFields,
                    style = backupStatus.csvHeaderStyle,
                    classes = allowed
                )
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                onShowMessage("প্রতিটি শ্রেণির আলাদা CSV (ZIP) সংরক্ষিত হয়েছে")
            } catch (e: Exception) {
                onShowMessage("ZIP ফাইল তৈরি হয়নি: ${e.message ?: "ত্রুটি"}")
            }
        }
    }

    val csvFilePickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            XlsxImporter.readSpreadsheetAsText(ctx.contentResolver, uri)
                .onSuccess { txt -> onOpenCsvImport(txt) }
                .onFailure { e -> onShowMessage("ফাইল পড়া যায়নি: ${e.message ?: "ত্রুটি"}") }
        }
    }

    val jsonExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            try {
                val fullJson = onBuildBackupJson?.invoke() ?: Repo.toJson(
                    list = students,
                    st = settings,
                    reportPrefs = Repo.loadReportBackupPrefs(ctx),
                    sheetTemplates = SheetTemplateCache(ctx).getAll()
                )
                ctx.contentResolver.openOutputStream(uri)?.use {
                    it.write(fullJson.toByteArray(Charsets.UTF_8))
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
                val txt = ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?.removePrefix("\uFEFF") ?: ""
                if (txt.isNotBlank()) pendingManualJsonRestore = txt
            } catch (e: Exception) {
                onShowMessage("ফাইল পড়া যায়নি: ${e.message ?: "ত্রুটি"}")
            }
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            onSetLocalBackupFolder(uri)
        }
    }

    val csvFolderPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            onSetCsvBackupFolder(uri)
        }
    }

    val activeAppBnFontLabel = BENGALI_APP_FONT_LIST.find { it.id == appFontId }?.labelBn?.substringBefore(" (") ?: appFontId
    val activeAppEnFontLabel = ENGLISH_APP_FONT_LIST.find { it.id == appEnglishFontId }?.labelBn?.substringBefore(" (") ?: appEnglishFontId
    val activeAppFontLabel = "বাংলা: $activeAppBnFontLabel · ইংরেজি: $activeAppEnFontLabel"
    val activePdfBnFontLabel = BENGALI_PDF_FONT_LIST.find { it.id == s.pdfFont }?.labelBn?.substringBefore(" (") ?: s.pdfFont
    val activePdfEnFontLabel = ENGLISH_PDF_FONT_LIST.find { it.id == s.pdfEnglishFont }?.labelBn?.substringBefore(" (") ?: s.pdfEnglishFont
    val activeFontLabel = "বাংলা: $activePdfBnFontLabel · ইংরেজি: $activePdfEnFontLabel"
    val activeLogoLabel = DPE_LOGO_LIST.find { it.id == s.dpeLogo }?.labelBn?.substringBefore(" (") ?: s.dpeLogo

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            Modifier
                .widthIn(max = 820.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsOverviewCard(
                schoolName = s.school,
                year = s.year,
                studentCount = students.size,
                activeSchool = activeSchool,
                currentUserId = currentUserId,
                isGeminiConfigured = geminiApiKey.isNotBlank(),
                geminiModel = geminiModel
            )

            if (!isAdmin) {
                Surface(
                    color = AmberBg,
                    border = BorderStroke(1.dp, AmberBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "আপনি এই বিদ্যালয়ে \"শিক্ষক\" ভূমিকায় যুক্ত আছেন। আপনার ডেটা অ্যাক্সেস ও সেটিংস বিদ্যালয়ের অ্যাডমিন নির্ধারণ করেন।",
                            fontSize = 12.sp,
                            color = AmberNotice,
                            lineHeight = 17.sp
                        )
                        Text(
                            text = "বর্তমান অনুমতি: ${permissions.summaryBn()}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SlateText
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateOutline.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TabRow(
                    selectedTabIndex = selectedSettingsTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedSettingsTab == 0,
                        onClick = { selectedSettingsTab = 0 },
                        icon = { Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        text = { Text("সাধারণ সেটিংস", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, maxLines = 1) },
                        modifier = Modifier.testTag("settings_tab_general")
                    )
                    Tab(
                        selected = selectedSettingsTab == 1,
                        onClick = { selectedSettingsTab = 1 },
                        icon = { Icon(Icons.Rounded.Gavel, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        text = { Text("শর্তাবলী", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, maxLines = 1) },
                        modifier = Modifier.testTag("settings_tab_rules")
                    )
                }
            }

            if (selectedSettingsTab == 1) {
                SettingsRulesTab(
                    rules = s.resultRules,
                    isAdmin = isAdmin,
                    onUpdateRules = { nextRules ->
                        val updated = s.copy(resultRules = nextRules)
                        s = updated
                        onUpdateSettings(updated)
                    },
                    onShowMessage = onShowMessage
                )
            } else {

            // Category 1: School & Team
            SettingsCategoryHeader(title = "বিদ্যালয় ও শিক্ষক দল", icon = Icons.Rounded.School)

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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = s.school,
                            onValueChange = { if (isAdmin) s = s.copy(school = it) },
                            enabled = isAdmin,
                            label = { Text("বিদ্যালয়ের নাম") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("settings_school_input")
                        )
                        OutlinedTextField(
                            value = s.schoolAddress,
                            onValueChange = { if (isAdmin) s = s.copy(schoolAddress = it) },
                            enabled = isAdmin,
                            label = { Text("বিদ্যালয়ের ঠিকানা (ফ্রন্ট ও ব্যাক কভারে মুদ্রিত হবে)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("settings_address_input")
                        )
                        OutlinedTextField(
                            value = s.year,
                            onValueChange = { if (isAdmin) s = s.copy(year = it) },
                            enabled = isAdmin,
                            label = { Text("শিক্ষাবর্ষ") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("settings_year_input")
                        )
                        if (isAdmin && (s.school != settings.school || s.schoolAddress != settings.schoolAddress || s.year != settings.year)) {
                            Button(
                                onClick = {
                                    onUpdateSettings(s)
                                    onShowMessage("বিদ্যালয়ের তথ্য সংরক্ষিত হয়েছে")
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("settings_save_school_btn")
                            ) {
                                Text("বিদ্যালয়ের তথ্য সংরক্ষণ করুন")
                            }
                        }
                    }
                }
            }

            if (activeSchool != null) {
                TreeGroupCard(
                    title = "আমন্ত্রণ কোড ও যোগদান অনুরোধ",
                    subtitle = if (joinRequests.isNotEmpty() && isAdmin) {
                        "কোড: ${activeSchool.joinCode} · নতুন অনুরোধ অপেক্ষমাণ"
                    } else {
                        "কোড: ${activeSchool.joinCode}"
                    },
                    badgeText = if (joinRequests.isNotEmpty() && isAdmin) "${bn(joinRequests.size.toString())} অনুরোধ" else "কোড",
                    badgeColor = if (joinRequests.isNotEmpty() && isAdmin) AmberNotice else TealPrimary,
                    badgeContainerColor = if (joinRequests.isNotEmpty() && isAdmin) AmberBg else TealSoft,
                    icon = Icons.Rounded.QrCode2,
                    expanded = expandedGroup == "collab",
                    headerTestTag = "settings_group_collab",
                    onToggle = { expandedGroup = if (expandedGroup == "collab") "" else "collab" }
                ) {
                    TreeBranchItem(isLast = !(isAdmin && joinRequests.isNotEmpty())) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = TealSoft,
                                border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("আমন্ত্রণ কোড (অন্য শিক্ষককে দিন)", fontSize = 11.5.sp, color = SlateMuted)
                                        Text(
                                            activeSchool.joinCode,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TealDark,
                                            letterSpacing = 2.sp,
                                            modifier = Modifier.testTag("settings_join_code_text")
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(activeSchool.joinCode))
                                            onShowMessage("স্কুল কোড কপি হয়েছে: ${activeSchool.joinCode}")
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("settings_copy_code_btn")
                                    ) {
                                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.size(4.dp))
                                        Text("কপি", fontSize = 12.sp)
                                    }
                                    if (isAdmin) {
                                        IconButton(
                                            onClick = { askRegenerateCode = true },
                                            modifier = Modifier.size(44.dp).testTag("settings_regen_code_btn")
                                        ) {
                                            Icon(Icons.Rounded.Refresh, contentDescription = "নতুন কোড তৈরি করুন", tint = TealPrimary)
                                        }
                                    }
                                }
                            }

                            Surface(
                                color = SlateBg,
                                border = BorderStroke(1.dp, SlateOutline),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text("অ্যাডমিন অনুমোদন: সর্বদা চালু", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = TealDark)
                                    Text(
                                        "নতুন শিক্ষক কোড পাঠানোর পর অ্যাডমিন তার শ্রেণি ও বিষয়ের অনুমতি নির্ধারণ করে অনুমোদন করবেন। একজন ব্যবহারকারী একটিমাত্র বিদ্যালয়ে যুক্ত থাকতে পারবেন।",
                                        fontSize = 11.sp,
                                        color = SlateMuted,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    if (isAdmin && joinRequests.isNotEmpty()) {
                        TreeBranchItem(isLast = true) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "অপেক্ষমান যোগদানের অনুরোধ (${bn(joinRequests.size.toString())})",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = AmberNotice
                                )
                                joinRequests.forEach { req ->
                                    Surface(
                                        color = AmberBg,
                                        border = BorderStroke(1.dp, AmberBorder),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Column {
                                                Text(req.displayName.ifBlank { "শিক্ষক" }, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                if (req.email.isNotBlank()) {
                                                    Text(req.email, fontSize = 11.sp, color = SlateMuted)
                                                }
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = { onRejectJoinRequest(req.uid) },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("বাতিল", fontSize = 11.5.sp, color = RoseDanger) }
                                                Button(
                                                    onClick = { pendingApproveRequest = req },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1.4f)
                                                ) { Text("অনুমোদন ও অ্যাক্সেস দিন", fontSize = 11.5.sp) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                TreeGroupCard(
                    title = "সদস্য তালিকা ও অনুমতি",
                    subtitle = "${bn(activeSchool.members.size.toString())} জন শিক্ষক যুক্ত আছেন",
                    badgeText = if (isOwner) "প্রতিষ্ঠাতা" else activeSchool.roleOf(currentUserId).labelBn,
                    icon = Icons.Rounded.AdminPanelSettings,
                    expanded = expandedGroup == "members",
                    headerTestTag = "settings_group_members",
                    onToggle = { expandedGroup = if (expandedGroup == "members") "" else "members" }
                ) {
                    TreeBranchItem(isLast = true) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            activeSchool.members.forEach { member ->
                                val memberIsOwner = member.uid == activeSchool.ownerUid
                                val isSelf = member.uid == currentUserId
                                Surface(
                                    color = SlateBg,
                                    border = BorderStroke(1.dp, SlateOutline),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    text = member.name.ifBlank { "শিক্ষক" } + if (isSelf) " (আপনি)" else "",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (member.email.isNotBlank()) {
                                                    Text(member.email, fontSize = 11.sp, color = SlateMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                            Surface(
                                                color = if (member.role == MemberRole.ADMIN) TealContainer else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = if (memberIsOwner) "প্রতিষ্ঠাতা" else member.role.labelBn,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (member.role == MemberRole.ADMIN) TealDark else SlateMuted,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = if (member.role == MemberRole.ADMIN) "অ্যাক্সেস: পূর্ণ অ্যাডমিন অ্যাক্সেস" else "অ্যাক্সেস: ${member.permissions.summaryBn()}",
                                            fontSize = 11.sp,
                                            color = if (member.role == MemberRole.ADMIN) TealDark else SlateMuted
                                        )

                                        if (isAdmin && !memberIsOwner && !isSelf) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (member.role == MemberRole.TEACHER) {
                                                    OutlinedButton(
                                                        onClick = { pendingEditMember = member },
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(Modifier.size(4.dp))
                                                        Text("অ্যাক্সেস / অনুমতি", fontSize = 11.sp)
                                                    }
                                                }
                                                val nextRole = if (member.role == MemberRole.ADMIN) MemberRole.TEACHER else MemberRole.ADMIN
                                                TextButton(
                                                    onClick = { onUpdateMemberRole(member.uid, nextRole) },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        if (nextRole == MemberRole.ADMIN) "অ্যাডমিন বানান" else "শিক্ষক বানান",
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { pendingRemoveMember = member },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Rounded.PersonRemove, contentDescription = "সদস্য অপসারণ", tint = RoseDanger, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val allClassesList = remember(s.customClasses) { activeSchoolClasses(s) }
            TreeGroupCard(
                title = "শ্রেণি ব্যবস্থাপনা ও নতুন শ্রেণি",
                subtitle = if (s.customClasses.isEmpty()) {
                    "মূল ১ম–৫ম শ্রেণি চালু আছে · নতুন শ্রেণি যোগ করুন"
                } else {
                    "মোট ${bn(allClassesList.size.toString())}টি শ্রেণি (নতুন ${bn(s.customClasses.size.toString())}টি)"
                },
                badgeText = "${bn(allClassesList.size.toString())}টি",
                icon = Icons.Rounded.School,
                expanded = expandedGroup == "classes",
                headerTestTag = "settings_group_classes",
                onToggle = { expandedGroup = if (expandedGroup == "classes") "" else "classes" }
            ) {
                TreeBranchItem(isLast = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        allClassesList.forEach { cDef ->
                            val stuCnt = students.count { it.cls == cDef.id }
                            Surface(
                                color = SlateBg,
                                border = BorderStroke(1.dp, SlateOutline),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = if (cDef.isCustom) TealSoft else Color.White,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, if (cDef.isCustom) TealPrimary.copy(alpha = 0.4f) else SlateOutline)
                                    ) {
                                        Text(
                                            cDef.shortBn,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (cDef.isCustom) TealDark else SlateText,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "${cDef.nameBn} শ্রেণি",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SlateText
                                        )
                                        val tierText = if (cDef.syllabusTier <= 2) "১ম–২য় ধরন (৫০/৫০)" else "৩য়–৫ম ধরন (৩০/৭০)"
                                        Text(
                                            "${bn(stuCnt.toString())} জন শিক্ষার্থী · $tierText",
                                            fontSize = 10.5.sp,
                                            color = SlateMuted
                                        )
                                    }
                                    if (cDef.isCustom && isAdmin) {
                                        IconButton(
                                            onClick = {
                                                pendingRemoveCustomClass = s.customClasses.find { it.id == cDef.id }
                                            },
                                            modifier = Modifier.size(36.dp).testTag("settings_remove_class_${cDef.id}")
                                        ) {
                                            Icon(
                                                Icons.Rounded.DeleteOutline,
                                                contentDescription = "${cDef.nameBn} মুছুন",
                                                tint = RoseDanger,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (isAdmin) {
                            OutlinedButton(
                                onClick = { showAddCustomClassDialog = true },
                                enabled = s.customClasses.size < 15,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("settings_add_custom_class_btn")
                            ) {
                                Icon(Icons.Rounded.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("নতুন শ্রেণি যোগ করুন (যেমন: প্রাক-প্রাথমিক, ষষ্ঠ)", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            TreeGroupCard(
                title = "শ্রেণি শিক্ষক ও মোবাইল",
                subtitle = "${allClassesList.size.let { bn(it.toString()) }}টি শ্রেণির দায়িত্বপ্রাপ্ত শিক্ষক",
                badgeText = "${bn(allClassesList.size.toString())} শ্রেণি",
                icon = Icons.Rounded.Badge,
                expanded = expandedGroup == "teachers",
                headerTestTag = "settings_group_teachers",
                onToggle = { expandedGroup = if (expandedGroup == "teachers") "" else "teachers" }
            ) {
                TreeBranchItem(isLast = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        allClassesList.forEach { cDef ->
                            val c = cDef.id
                            Text("${cDef.nameBn} শ্রেণি", fontWeight = FontWeight.Medium, fontSize = 12.sp, color = SlateMuted)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = s.teachers[c] ?: "",
                                    onValueChange = { v -> if (isAdmin) s = s.copy(teachers = s.teachers + (c to v)) },
                                    enabled = isAdmin,
                                    label = { Text("শিক্ষকের নাম", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = s.teacherMobiles[c] ?: "",
                                    onValueChange = { v -> if (isAdmin) s = s.copy(teacherMobiles = s.teacherMobiles + (c to v)) },
                                    enabled = isAdmin,
                                    label = { Text("মোবাইল", fontSize = 11.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier.weight(0.85f)
                                )
                            }
                        }
                        if (isAdmin && (s.teachers != settings.teachers || s.teacherMobiles != settings.teacherMobiles)) {
                            Button(
                                onClick = {
                                    onUpdateSettings(s)
                                    onShowMessage("শ্রেণি শিক্ষকের তথ্য সংরক্ষিত হয়েছে")
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("settings_save_teachers_btn")
                            ) {
                                Text("শিক্ষকের তথ্য সংরক্ষণ করুন")
                            }
                        }
                    }
                }
            }

            // Category 2: Font Management (Separate Bengali & English choices for App UI and Documents)
            SettingsCategoryHeader(title = "ফন্ট ব্যবস্থাপনা (বাংলা ও ইংরেজি — আলাদা নির্বাচন)", icon = Icons.Rounded.FontDownload)

            var appFontScriptTab by remember { mutableStateOf("bn") }
            val activeAppBnFamily = remember(appFontId) { appComposeFontFamily(appFontId) }
            val activeAppEnFamily = remember(appEnglishFontId) { appComposeFontFamily(appEnglishFontId) }

            TreeGroupCard(
                title = "ভাগ ১: অ্যাপসের নিজস্ব ফন্ট",
                subtitle = "অ্যাপের লেখা ও মেনু · $activeAppFontLabel",
                badgeText = "অ্যাপ UI",
                icon = Icons.Rounded.TextFields,
                expanded = expandedGroup == "app_font",
                headerTestTag = "settings_group_app_font",
                onToggle = { expandedGroup = if (expandedGroup == "app_font") "" else "app_font" }
            ) {
                TreeBranchItem(isLast = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "বাংলার জন্য আলাদা বাংলা ফন্ট এবং ইংরেজি/সংখ্যা/কোডের জন্য আলাদা ইংরেজি ফন্ট নির্বাচন করুন — একই স্ক্রিনে উভয় ফন্ট একসাথে প্রযোজ্য হবে।",
                            fontSize = 11.5.sp,
                            color = SlateMuted
                        )
                        Surface(
                            color = TealSoft,
                            border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    "লাইভ মিশ্র প্রিভিউ (অ্যাপ UI): বাংলা = $activeAppBnFontLabel · ইংরেজি = $activeAppEnFontLabel",
                                    fontSize = 10.5.sp,
                                    color = TealDark,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = buildMixedScriptAnnotatedString(
                                        "শ্রেণি: Class 3 · রোল 01 — মিতু (GPA A+ 2026)",
                                        bnFamily = activeAppBnFamily,
                                        enFamily = activeAppEnFamily
                                    ),
                                    fontSize = 13.sp,
                                    color = SlateText
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = appFontScriptTab == "bn",
                                onClick = { appFontScriptTab = "bn" },
                                colors = appFilterChipColors(),
                                border = appFilterChipBorder(appFontScriptTab == "bn"),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_app_font_tab_bn"),
                                label = {
                                    Text(
                                        "বাংলা ফন্ট",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                            FilterChip(
                                selected = appFontScriptTab == "en",
                                onClick = { appFontScriptTab = "en" },
                                colors = appFilterChipColors(),
                                border = appFilterChipBorder(appFontScriptTab == "en"),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_app_font_tab_en"),
                                label = {
                                    Text(
                                        "ইংরেজি ফন্ট",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }

                        val activeAppList = if (appFontScriptTab == "bn") BENGALI_APP_FONT_LIST else ENGLISH_APP_FONT_LIST
                        val selectedAppFontForTab = if (appFontScriptTab == "bn") appFontId else appEnglishFontId
                        activeAppList.forEach { font ->
                            val selected = selectedAppFontForTab == font.id
                            val sampleFamily = remember(font.id) {
                                appComposeFontFamily(font.id)
                            }
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_app_font_${font.id}")
                                    .clickable {
                                        if (appFontScriptTab == "bn") {
                                            onSelectAppFont(font.id)
                                        } else {
                                            onSelectAppEnglishFont(font.id)
                                        }
                                    },
                                color = if (selected) TealContainer.copy(alpha = 0.65f) else SlateBg,
                                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) TealPrimary else SlateOutline),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = {
                                            if (appFontScriptTab == "bn") {
                                                onSelectAppFont(font.id)
                                            } else {
                                                onSelectAppEnglishFont(font.id)
                                            }
                                        }
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(font.labelBn, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                                        Text(font.descriptionBn, fontSize = 11.sp, color = SlateMuted)
                                        Text(
                                            if (appFontScriptTab == "en") {
                                                "Result Maker Dashboard · Class 1–5 · Roll 01 (2026)"
                                            } else {
                                                "ড্যাশবোর্ড, শ্রেণি ও শিক্ষার্থী তালিকা — ২০২৬"
                                            },
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
            }

            var pdfFontScriptTab by remember { mutableStateOf("bn") }
            val activePdfBnFamily = remember(s.pdfFont) {
                FontFamily(androidx.compose.ui.text.font.Typeface(PdfGen.resolveTypeface(ctx, s.pdfFont, false)))
            }
            val activePdfEnFamily = remember(s.pdfEnglishFont) {
                FontFamily(androidx.compose.ui.text.font.Typeface(PdfGen.resolveTypeface(ctx, s.pdfEnglishFont, false)))
            }

            TreeGroupCard(
                title = "ভাগ ২: রিপোর্ট ও অন্যান্য PDF ফন্ট",
                subtitle = "প্রগতি প্রতিবেদন ও ফাঁকা শিট · $activeFontLabel",
                badgeText = "PDF ফন্ট",
                icon = Icons.Rounded.FontDownload,
                expanded = expandedGroup == "font",
                headerTestTag = "settings_group_font",
                onToggle = { expandedGroup = if (expandedGroup == "font") "" else "font" }
            ) {
                TreeBranchItem(isLast = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            if (isAdmin) {
                                "একই ডকুমেন্ট বা ফাঁকা শিটে বাংলা অংশ নির্বাচিত বাংলা ফন্টে এবং ইংরেজি অক্ষর/সংখ্যা/রেফারেন্স কোড নির্বাচিত ইংরেজি ফন্টে মুদ্রিত হবে।"
                            } else {
                                "একই ডকুমেন্ট বা ফাঁকা শিটে বাংলা ও ইংরেজি উভয় ফন্ট একসাথে কাজ করবে (নিজের ডিভাইসের PDF ফন্ট পরিবর্তন করতে পারবেন)।"
                            },
                            fontSize = 11.5.sp,
                            color = SlateMuted
                        )
                        Surface(
                            color = TealSoft,
                            border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    "লাইভ মিশ্র প্রিভিউ (PDF): বাংলা = $activePdfBnFontLabel · ইংরেজি = $activePdfEnFontLabel",
                                    fontSize = 10.5.sp,
                                    color = TealDark,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = buildMixedScriptAnnotatedString(
                                        "ফাঁকা শিট / Progress Report · Roll 05 · REF:C3-BAN · Grade A+",
                                        bnFamily = activePdfBnFamily,
                                        enFamily = activePdfEnFamily
                                    ),
                                    fontSize = 13.sp,
                                    color = SlateText
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = pdfFontScriptTab == "bn",
                                onClick = { pdfFontScriptTab = "bn" },
                                colors = appFilterChipColors(),
                                border = appFilterChipBorder(pdfFontScriptTab == "bn"),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_pdf_font_tab_bn"),
                                label = {
                                    Text(
                                        "বাংলা ফন্ট (PDF)",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                            FilterChip(
                                selected = pdfFontScriptTab == "en",
                                onClick = { pdfFontScriptTab = "en" },
                                colors = appFilterChipColors(),
                                border = appFilterChipBorder(pdfFontScriptTab == "en"),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_pdf_font_tab_en"),
                                label = {
                                    Text(
                                        "ইংরেজি ফন্ট (PDF)",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }

                        val activePdfList = if (pdfFontScriptTab == "bn") BENGALI_PDF_FONT_LIST else ENGLISH_PDF_FONT_LIST
                        val selectedPdfFontForTab = if (pdfFontScriptTab == "bn") s.pdfFont else s.pdfEnglishFont
                        activePdfList.forEach { font ->
                            val selected = selectedPdfFontForTab == font.id
                            val sampleFamily = remember(font.id) {
                                FontFamily(androidx.compose.ui.text.font.Typeface(PdfGen.resolveTypeface(ctx, font.id, false)))
                            }
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_font_${font.id}")
                                    .clickable {
                                        val next = if (pdfFontScriptTab == "bn") {
                                            s.copy(pdfFont = font.id)
                                        } else {
                                            s.copy(pdfEnglishFont = font.id)
                                        }
                                        s = next
                                        onUpdateSettings(next)
                                    },
                                color = if (selected) TealContainer.copy(alpha = 0.65f) else SlateBg,
                                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) TealPrimary else SlateOutline),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = {
                                            val next = if (pdfFontScriptTab == "bn") {
                                                s.copy(pdfFont = font.id)
                                            } else {
                                                s.copy(pdfEnglishFont = font.id)
                                            }
                                            s = next
                                            onUpdateSettings(next)
                                        }
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(font.labelBn, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                                        Text(font.descriptionBn, fontSize = 11.sp, color = SlateMuted)
                                        Text(
                                            if (pdfFontScriptTab == "en") {
                                                "Progress Report Card & Blank Sheet · Class V (2026)"
                                            } else {
                                                "শিখন অগ্রগতি প্রতিবেদন ও নম্বরপত্র — ২০২৬"
                                            },
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
            }

            // Category 3: Reports, Print & Form Structure
            SettingsCategoryHeader(title = "রিপোর্ট, প্রিন্ট ও ফর্ম", icon = Icons.Rounded.Description)

            TreeGroupCard(
                title = "প্রাথমিক শিক্ষা অধিদপ্তর লোগো",
                subtitle = "সক্রিয়: $activeLogoLabel",
                badgeText = "কভার",
                icon = Icons.Rounded.Image,
                expanded = expandedGroup == "logo",
                headerTestTag = "settings_group_logo",
                onToggle = { expandedGroup = if (expandedGroup == "logo") "" else "logo" }
            ) {
                TreeBranchItem(isLast = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DPE_LOGO_LIST.forEach { logo ->
                            val selected = s.dpeLogo == logo.id
                            val thumbBmp = remember(logo.id) { PdfGen.resolveLogoBitmap(ctx, logo.id) }
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_logo_${logo.id}")
                                    .clickable(enabled = isAdmin) {
                                        val next = s.copy(dpeLogo = logo.id)
                                        s = next
                                        onUpdateSettings(next)
                                    },
                                color = if (selected) TealContainer.copy(alpha = 0.65f) else SlateBg,
                                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) TealPrimary else SlateOutline),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        enabled = isAdmin,
                                        onClick = {
                                            val next = s.copy(dpeLogo = logo.id)
                                            s = next
                                            onUpdateSettings(next)
                                        }
                                    )
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

            TreeGroupCard(
                title = "কাস্টম ডাটাবেস ফিল্ড",
                subtitle = if (s.customFields.isEmpty()) "কোনো অতিরিক্ত ফিল্ড নেই" else "বর্তমান ${bn(s.customFields.size.toString())}টি অতিরিক্ত ফিল্ড",
                badgeText = "${bn(s.customFields.size.toString())}টি",
                icon = Icons.Rounded.Tune,
                expanded = expandedGroup == "custom_fields",
                headerTestTag = "settings_group_custom_fields",
                onToggle = { expandedGroup = if (expandedGroup == "custom_fields") "" else "custom_fields" }
            ) {
                TreeBranchItem(isLast = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isAdmin) {
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
                        }
                        if (s.customFields.isEmpty()) {
                            Text("এখনো কোনো কাস্টম ফিল্ড নেই।", fontSize = 12.sp, color = SlateMuted)
                        } else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                s.customFields.forEach { cf: CustomFieldDef ->
                                    AssistChip(
                                        onClick = { if (isAdmin) pendingRemoveField = cf },
                                        label = { Text(cf.label, fontSize = 12.sp) },
                                        trailingIcon = if (isAdmin) {
                                            {
                                                Icon(
                                                    Icons.Rounded.Close,
                                                    contentDescription = "${cf.label} মুছুন",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null,
                                        modifier = Modifier.testTag("settings_remove_field_${cf.key}")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Category 3: Data, Backup & Import/Export
            SettingsCategoryHeader(title = "ব্যাকআপ, ইমপোর্ট ও ডাটা", icon = Icons.Rounded.Backup)

            val autoBackupActive = backupStatus.localEnabled || backupStatus.driveEnabled || backupStatus.csvEnabled
            val thirdFmt = if (backupStatus.csvBackupFormat == CsvBackupFormat.CSV) "CSV" else "Excel"
            TreeGroupCard(
                title = "অটো ব্যাকআপ (লোকাল · Drive · $thirdFmt)",
                subtitle = "লোকাল: ${if (backupStatus.localEnabled) "চালু" else "বন্ধ"} · Drive: ${if (backupStatus.driveEnabled) "চালু" else "বন্ধ"} · $thirdFmt: ${if (backupStatus.csvEnabled) "চালু" else "বন্ধ"}",
                badgeText = if (autoBackupActive) "চালু" else "বন্ধ",
                badgeColor = if (autoBackupActive) EmeraldGrade else SlateMuted,
                badgeContainerColor = if (autoBackupActive) TealSoft else MaterialTheme.colorScheme.surfaceVariant,
                icon = Icons.Rounded.Backup,
                expanded = expandedGroup == "backup",
                headerTestTag = "settings_group_backup",
                onToggle = { expandedGroup = if (expandedGroup == "backup") "" else "backup" }
            ) {
                SettingsAutoBackupContent(
                    backupStatus = backupStatus,
                    onBackupNowAll = onBackupNowAll,
                    onBackupLocalNow = onBackupLocalNow,
                    onBackupDriveNow = onBackupDriveNow,
                    onBackupCsvNow = { showInstantExportDialog = true },
                    onToggleLocalAutoBackup = onToggleLocalAutoBackup,
                    onPickLocalFolder = { folderPickerLauncher.launch(null) },
                    onShowLocalSnapshots = { showLocalSnapshotsDialog = true },
                    onToggleDriveAutoBackup = onToggleDriveAutoBackup,
                    onShowDriveBackups = {
                        onLoadDriveBackups()
                        showDriveBackupsDialog = true
                    },
                    onToggleCsvAutoBackup = onToggleCsvAutoBackup,
                    onPickCsvFolder = { csvFolderPickerLauncher.launch(null) },
                    onShowCsvSnapshots = { showCsvSnapshotsDialog = true },
                    onSelectCsvHeaderStyle = onSelectCsvHeaderStyle,
                    onSelectCsvBackupFormat = onSelectCsvBackupFormat,
                    onUpdateBackupSchedule = onUpdateBackupSchedule,
                    formatTime = ::formatTimeBn
                )
            }

            TreeGroupCard(
                title = "PDF, CSV ও JSON ডাটা",
                subtitle = "মোট ${bn(students.size.toString())} জন শিক্ষার্থীর ফাইল",
                badgeText = "ফাইল",
                icon = Icons.Rounded.UploadFile,
                expanded = expandedGroup == "csv_json",
                headerTestTag = "settings_group_csv_json",
                onToggle = { expandedGroup = if (expandedGroup == "csv_json") "" else "csv_json" }
            ) {
                val allClsIds = remember(s.customClasses) { activeClassIds(s) }
                val countByClass = remember(students, allClsIds) {
                    allClsIds.associateWith { c -> students.count { it.cls == c } }
                }
                SettingsCsvJsonContent(
                    permissions = permissions,
                    isAdmin = isAdmin,
                    availableClassIds = allClsIds,
                    studentCountByClass = countByClass,
                    csvHeaderStyle = backupStatus.csvHeaderStyle,
                    onSelectCsvHeaderStyle = onSelectCsvHeaderStyle,
                    onInstantCsvExport = { showInstantExportDialog = true },
                    onExportAllPdf = onExportAllPdf,
                    onExportAllCsvSingleFile = {
                        pendingCsvExportCls = null
                        csvExportLauncher.launch("ResultMaker_AllClasses.csv")
                    },
                    onExportExcelMultiTab = { xlsxExportLauncher.launch("ResultMaker_AllClasses.xlsx") },
                    onExportCsvForClass = { c ->
                        pendingCsvExportCls = c
                        csvExportLauncher.launch("students_class_$c.csv")
                    },
                    onExportCsvTemplateForClass = { c ->
                        pendingCsvTemplateCls = c
                        csvTemplateLauncher.launch("template_class_$c.csv")
                    },
                    onExportPerClassCsvZip = { csvZipExportLauncher.launch("students_per_class_csv.zip") },
                    onOpenSmartCsvImport = { onOpenCsvImport("") },
                    onOpenBasicStudentImport = onOpenBasicImport,
                    onPickCsvFile = { csvFilePickLauncher.launch(arrayOf("*/*")) },
                    onExportJson = { jsonExportLauncher.launch("result_maker_backup.json") },
                    onRestoreJsonFile = { jsonRestoreLauncher.launch(arrayOf("application/json", "*/*")) }
                )
            }

            if (isAdmin) {
                TreeGroupCard(
                    title = "ডাটা রিসেট ও মুছে ফেলা",
                    subtitle = "শ্রেণি, বিষয়, বেসিক তথ্য বা সম্পূর্ণ ডাটা রিসেট (ফিঙ্গারপ্রিন্ট সুরক্ষিত)",
                    badgeText = "সতর্কতা",
                    badgeColor = RoseDanger,
                    badgeContainerColor = RoseBg,
                    icon = Icons.Rounded.WarningAmber,
                    expanded = expandedGroup == "danger",
                    headerTestTag = "settings_group_danger",
                    onToggle = { expandedGroup = if (expandedGroup == "danger") "" else "danger" }
                ) {
                    TreeBranchItem(isLast = true) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "সতর্কতা: যেকোনো রিসেট বা মুছে ফেলার আগে সতর্কবার্তা দেখানো হবে এবং ডিভাইসের ফিঙ্গারপ্রিন্ট সেন্সর যাচাই করা বাধ্যতামূলক।",
                                fontSize = 11.5.sp,
                                color = RoseDanger,
                                lineHeight = 16.sp
                            )
                            Button(
                                onClick = {
                                    dataResetInitialCategory = null
                                    showDataResetWizard = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_step_reset_btn")
                            ) {
                                Icon(Icons.Rounded.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("ধাপে ধাপে ডাটা রিসেট (শ্রেণি, বিষয়, বেসিক তথ্য)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = {
                                        dataResetInitialCategory = DataResetCategory.SAMPLE_DATA
                                        showDataResetWizard = true
                                    },
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("settings_reset_sample_btn"),
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text("নমুনা রিসেট", fontSize = 12.sp) }
                                Button(
                                    onClick = {
                                        dataResetInitialCategory = DataResetCategory.ALL_STUDENTS
                                        showDataResetWizard = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("settings_clear_all_btn"),
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text("সব মুছুন", fontSize = 12.sp) }
                            }
                        }
                    }
                }
            }

            // Category 4: AI & Account
            SettingsCategoryHeader(title = "AI সহায়ক ও অ্যাকাউন্ট", icon = Icons.Rounded.AutoAwesome)

            TreeGroupCard(
                title = "Gemini AI ও মডেল",
                subtitle = if (geminiApiKey.isNotBlank()) {
                    "সক্রিয়: $geminiModel · API Key সংরক্ষিত"
                } else {
                    "Google AI Studio থেকে নিজস্ব API Key যুক্ত করুন"
                },
                badgeText = if (geminiApiKey.isNotBlank()) "সংযুক্ত" else "সেটআপ বাকি",
                badgeColor = if (geminiApiKey.isNotBlank()) EmeraldGrade else AmberNotice,
                badgeContainerColor = if (geminiApiKey.isNotBlank()) TealSoft else AmberBg,
                icon = Icons.Rounded.AutoAwesome,
                expanded = expandedGroup == "aiproxy",
                headerTestTag = "settings_group_aiproxy",
                onToggle = { expandedGroup = if (expandedGroup == "aiproxy") "" else "aiproxy" }
            ) {
                SettingsGeminiAiContent(
                    apiKeyInput = apiKeyInput,
                    onApiKeyChange = { apiKeyInput = it },
                    showApiKey = showApiKey,
                    onToggleShowApiKey = { showApiKey = !showApiKey },
                    modelInput = modelInput,
                    onModelInputChange = { modelInput = it },
                    modelSearchQuery = modelSearchQuery,
                    onModelSearchChange = { modelSearchQuery = it },
                    showAllLiveModels = showAllLiveModels,
                    onToggleShowAllLiveModels = { showAllLiveModels = !showAllLiveModels },
                    geminiApiKey = geminiApiKey,
                    geminiModel = geminiModel,
                    geminiAvailableModels = geminiAvailableModels,
                    geminiCustomModels = geminiCustomModels,
                    geminiNewModelIds = geminiNewModelIds,
                    geminiRefreshingModels = geminiRefreshingModels,
                    geminiModelsError = geminiModelsError,
                    geminiTesting = geminiTesting,
                    geminiTestStatus = geminiTestStatus,
                    geminiTestSuccess = geminiTestSuccess,
                    onSaveGeminiConfig = onSaveGeminiConfig,
                    onTestGeminiConnection = onTestGeminiConnection,
                    onRefreshGeminiModels = onRefreshGeminiModels,
                    onRemoveCustomGeminiModel = onRemoveCustomGeminiModel,
                    onClearGeminiApiKey = {
                        apiKeyInput = ""
                        onClearGeminiApiKey()
                    }
                )
            }

            TreeGroupCard(
                title = "অ্যাকাউন্ট ও অ্যাপ তথ্য",
                subtitle = "সাইন আউট, বিদ্যালয় ত্যাগ ও সংস্করণ",
                badgeText = "১.০.০",
                icon = Icons.Rounded.AccountCircle,
                expanded = expandedGroup == "account",
                headerTestTag = "settings_group_account",
                onToggle = { expandedGroup = if (expandedGroup == "account") "" else "account" }
            ) {
                TreeBranchItem(isLast = false) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (activeSchool != null && !isOwner) {
                            OutlinedButton(
                                onClick = { askLeaveSchool = true },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, RoseDanger.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().testTag("settings_leave_school_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp), tint = RoseDanger)
                                Spacer(Modifier.size(6.dp))
                                Text("এই বিদ্যালয় ত্যাগ করুন (অন্য বিদ্যালয়ে যুক্ত হতে)", fontSize = 12.sp, color = RoseDanger)
                            }
                        }
                        OutlinedButton(
                            onClick = { askSignOut = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("settings_sign_out_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(16.dp), tint = RoseDanger)
                            Spacer(Modifier.size(4.dp))
                            Text("সাইন আউট", fontSize = 12.sp, color = RoseDanger)
                        }
                    }
                }

                TreeBranchItem(isLast = true) {
                    Surface(
                        color = SlateBg,
                        border = BorderStroke(1.dp, SlateOutline),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("অ্যাপ্লিকেশন তথ্য", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = SlateText)
                            Text("অ্যাপ আইডি: com.kgps.result · ভার্সন: 1.0.0", fontSize = 11.5.sp, color = SlateMuted)
                            Text("ফরম্যাট: NCTB প্রাথমিক বিদ্যালয় (১ম–৫ম ও কাস্টম শ্রেণি) A4 Landscape ভাঁজযোগ্য বুকলেট", fontSize = 11.5.sp, color = SlateMuted)
                        }
                    }
                }
            }
            }

            Spacer(Modifier.height(12.dp))
        }
    }

    // Dialog: Approve Join Request + Assign Granular Permissions
    pendingApproveRequest?.let { req ->
        TeacherPermissionsEditorDialog(
            title = "শিক্ষক অনুমোদন ও অনুমতি নির্ধারণ",
            teacherLabel = "${req.displayName.ifBlank { "শিক্ষক" }} (${req.email})",
            initialPermissions = TeacherPermissions.fullAccess(),
            confirmButtonText = "অনুমোদন করুন",
            availableClasses = activeSchoolClasses(s),
            onDismiss = { pendingApproveRequest = null },
            onConfirm = { chosenPerm ->
                onApproveJoinRequest(req, chosenPerm)
                pendingApproveRequest = null
            }
        )
    }

    // Dialog: Edit Existing Teacher's Granular Permissions
    pendingEditMember?.let { member ->
        TeacherPermissionsEditorDialog(
            title = "শিক্ষকের ডেটা অ্যাক্সেস পরিবর্তন",
            teacherLabel = "${member.name.ifBlank { "শিক্ষক" }} (${member.email})",
            initialPermissions = member.permissions,
            confirmButtonText = "সংরক্ষণ করুন",
            availableClasses = activeSchoolClasses(s),
            onDismiss = { pendingEditMember = null },
            onConfirm = { chosenPerm ->
                onUpdateMemberPermissions(member.uid, chosenPerm)
                pendingEditMember = null
            }
        )
    }

    // Dialog: Local Backup Snapshots List
    if (showLocalSnapshotsDialog) {
        AlertDialog(
            onDismissRequest = { showLocalSnapshotsDialog = false },
            title = { Text("লোকাল অটো ব্যাকআপ স্ন্যাপশট") },
            text = {
                if (backupStatus.localSnapshots.isEmpty()) {
                    Text("এখনো কোনো লোকাল ব্যাকআপ ফাইল তৈরি হয়নি।", fontSize = 13.sp, color = SlateMuted)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        backupStatus.localSnapshots.forEach { entry ->
                            Surface(
                                color = SlateBg,
                                border = BorderStroke(1.dp, SlateOutline),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = isAdmin) {
                                        showLocalSnapshotsDialog = false
                                        confirmLocalRestore = entry
                                    }
                            ) {
                                Row(
                                    Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(entry.fileName, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                                        Text(
                                            "${formatTimeBn(entry.modifiedMs)} · ${bn((entry.sizeBytes / 1024).coerceAtLeast(1).toString())} KB",
                                            fontSize = 11.sp,
                                            color = SlateMuted
                                        )
                                    }
                                    if (isAdmin) {
                                        Text("রিস্টোর", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TealPrimary)
                                    }
                                }
                            }
                        }
                        if (!isAdmin) {
                            Text("নোট: ব্যাকআপ রিস্টোর শুধুমাত্র অ্যাডমিন করতে পারবেন।", fontSize = 11.sp, color = AmberNotice)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLocalSnapshotsDialog = false }) { Text("বন্ধ করুন") }
            }
        )
    }

    // Dialog: Ask CSV vs Excel on Instant Export
    if (showInstantExportDialog) {
        AlertDialog(
            onDismissRequest = { showInstantExportDialog = false },
            title = { Text("তাৎক্ষণিক এক্সপোর্ট — ফরম্যাট বেছে নিন", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "আপনি কোন ধরনের ১টি ফাইল হিসেবে সকল শ্রেণির তথ্য এক্সপোর্ট করতে চান?",
                        fontSize = 12.sp,
                        color = SlateMuted
                    )
                    CsvBackupFormat.entries.forEach { fmt ->
                        val isPreferred = backupStatus.csvBackupFormat == fmt
                        Surface(
                            color = if (isPreferred) TealSoft else SlateBg,
                            border = BorderStroke(if (isPreferred) 1.5.dp else 1.dp, if (isPreferred) TealPrimary else SlateOutline),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("instant_export_choice_${fmt.id}")
                                .clickable {
                                    showInstantExportDialog = false
                                    onBackupCsvNow(fmt)
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = fmt.labelBn,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TealDark
                                )
                                Text(
                                    text = fmt.descBn,
                                    fontSize = 11.sp,
                                    color = SlateMuted,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstantExportDialog = false }) { Text("বাতিল") }
            },
            confirmButton = {}
        )
    }

    // Dialog: CSV / Excel Auto Backup Files List (Share / Open in Excel/Sheets)
    if (showCsvSnapshotsDialog) {
        AlertDialog(
            onDismissRequest = { showCsvSnapshotsDialog = false },
            title = { Text("CSV ও Excel অটো ব্যাকআপ ফাইল") },
            text = {
                if (backupStatus.csvSnapshots.isEmpty()) {
                    Text("এখনো কোনো CSV বা Excel অটো ব্যাকআপ ফাইল তৈরি হয়নি।", fontSize = 13.sp, color = SlateMuted)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        backupStatus.csvSnapshots.forEach { entry ->
                            Surface(
                                color = SlateBg,
                                border = BorderStroke(1.dp, SlateOutline),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        runCatching {
                                            val fileUri = FileProvider.getUriForFile(
                                                ctx,
                                                "${ctx.packageName}.fileprovider",
                                                File(entry.path)
                                            )
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = if (entry.fileName.endsWith(".xlsx", ignoreCase = true)) {
                                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                                } else {
                                                    "text/csv"
                                                }
                                                putExtra(Intent.EXTRA_STREAM, fileUri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            ctx.startActivity(Intent.createChooser(shareIntent, entry.fileName))
                                        }.onFailure { err ->
                                            onShowMessage(err.localizedMessage ?: "ফাইল খোলা যায়নি")
                                        }
                                    }
                            ) {
                                Row(
                                    Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(entry.fileName, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                                        Text(
                                            "${formatTimeBn(entry.modifiedMs)} · ${bn((entry.sizeBytes / 1024).coerceAtLeast(1).toString())} KB",
                                            fontSize = 11.sp,
                                            color = SlateMuted
                                        )
                                    }
                                    Text("শেয়ার / খুলুন", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = TealPrimary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCsvSnapshotsDialog = false }) { Text("বন্ধ করুন") }
            }
        )
    }

    // Dialog: Google Drive Backups List
    if (showDriveBackupsDialog) {
        AlertDialog(
            onDismissRequest = { showDriveBackupsDialog = false },
            title = { Text("Google Drive ব্যাকআপ ফাইল") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (driveFilesLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("Google Drive থেকে তালিকা আনা হচ্ছে...", fontSize = 12.5.sp, color = SlateMuted)
                        }
                    } else if (driveFiles.isEmpty()) {
                        Text("\"Result Maker Backups\" ফোল্ডারে এখনো কোনো ব্যাকআপ ফাইল পাওয়া যায়নি।", fontSize = 12.5.sp, color = SlateMuted)
                    } else {
                        driveFiles.forEach { file ->
                            Surface(
                                color = TealSoft,
                                border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = isAdmin) {
                                        showDriveBackupsDialog = false
                                        confirmDriveRestore = file
                                    }
                            ) {
                                Row(
                                    Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(file.name, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                                        Text(
                                            "${file.modifiedTime.take(16).replace('T', ' ')} · ${bn((file.sizeBytes / 1024).coerceAtLeast(1).toString())} KB",
                                            fontSize = 11.sp,
                                            color = SlateMuted
                                        )
                                    }
                                    if (isAdmin) {
                                        Text("রিস্টোর", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TealPrimary)
                                    }
                                }
                            }
                        }
                        if (!isAdmin) {
                            Text("নোট: Google Drive থেকে রিস্টোর শুধুমাত্র অ্যাডমিন করতে পারবেন।", fontSize = 11.sp, color = AmberNotice)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDriveBackupsDialog = false }) { Text("বন্ধ করুন") }
            }
        )
    }

    // Confirm Dialogs before Restoring (Mutating Firestore)
    confirmLocalRestore?.let { entry ->
        AlertDialog(
            onDismissRequest = { confirmLocalRestore = null },
            title = { Text("লোকাল ব্যাকআপ রিস্টোর করবেন?") },
            text = { Text("\"${entry.fileName}\" থেকে ডাটা রিস্টোর করলে বিদ্যালয়ের বর্তমান সকল শিক্ষার্থী ও সেটিংস প্রতিস্থাপিত হবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreLocalSnapshot(entry.path)
                        confirmLocalRestore = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger)
                ) { Text("হ্যাঁ, রিস্টোর করুন") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLocalRestore = null }) { Text("বাতিল") }
            }
        )
    }

    confirmDriveRestore?.let { file ->
        AlertDialog(
            onDismissRequest = { confirmDriveRestore = null },
            title = { Text("Google Drive থেকে রিস্টোর করবেন?") },
            text = { Text("Drive-এর \"${file.name}\" ফাইল থেকে ডাটা আনলে বিদ্যালয়ের বর্তমান সকল শিক্ষার্থী ও সেটিংস প্রতিস্থাপিত হবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreFromDriveFile(file.id)
                        confirmDriveRestore = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger)
                ) { Text("হ্যাঁ, রিস্টোর করুন") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDriveRestore = null }) { Text("বাতিল") }
            }
        )
    }

    pendingManualJsonRestore?.let { jsonText ->
        AlertDialog(
            onDismissRequest = { pendingManualJsonRestore = null },
            title = { Text("ফাইল থেকে ব্যাকআপ রিস্টোর করবেন?") },
            text = { Text("নির্বাচিত JSON ফাইল থেকে রিস্টোর করলে বিদ্যালয়ের বর্তমান সকল শিক্ষার্থী ও সেটিংস প্রতিস্থাপিত হবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreBackupJson(jsonText)
                        pendingManualJsonRestore = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger)
                ) { Text("হ্যাঁ, রিস্টোর করুন") }
            },
            dismissButton = {
                TextButton(onClick = { pendingManualJsonRestore = null }) { Text("বাতিল") }
            }
        )
    }

    if (showDataResetWizard) {
        DataResetDialog(
            students = students,
            settings = settings,
            initialCategory = dataResetInitialCategory,
            onDismiss = { showDataResetWizard = false },
            onConfirmReset = { req ->
                when (req.category) {
                    DataResetCategory.SAMPLE_DATA -> onResetSampleData()
                    DataResetCategory.ALL_STUDENTS -> onClearAllStudents()
                    else -> onExecuteDataReset(req)
                }
            }
        )
    }

    if (askRegenerateCode) {
        AlertDialog(
            onDismissRequest = { askRegenerateCode = false },
            title = { Text("নতুন স্কুল কোড তৈরি করবেন?") },
            text = { Text("নতুন কোড তৈরি করলে পুরোনো কোড (${activeSchool?.joinCode.orEmpty()}) সাথে সাথে বাতিল হয়ে যাবে।") },
            confirmButton = {
                Button(
                    onClick = { askRegenerateCode = false; onRegenerateJoinCode() },
                    modifier = Modifier.testTag("settings_confirm_regen_code_btn")
                ) { Text("নতুন কোড তৈরি করুন") }
            },
            dismissButton = {
                TextButton(onClick = { askRegenerateCode = false }) { Text("বাতিল") }
            }
        )
    }

    if (askLeaveSchool) {
        AlertDialog(
            onDismissRequest = { askLeaveSchool = false },
            title = { Text("বিদ্যালয় ত্যাগ করবেন?") },
            text = { Text("আপনি \"${activeSchool?.settings?.school.orEmpty()}\" থেকে বের হয়ে যাবেন। এরপর নতুন বিদ্যালয় তৈরি করতে বা অন্য বিদ্যালয়ে কোড দিয়ে যোগদানের অনুরোধ পাঠাতে পারবেন।") },
            confirmButton = {
                Button(
                    onClick = { askLeaveSchool = false; onLeaveSchool() },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                    modifier = Modifier.testTag("settings_confirm_leave_school_btn")
                ) { Text("হ্যাঁ, ত্যাগ করুন") }
            },
            dismissButton = {
                TextButton(onClick = { askLeaveSchool = false }) { Text("বাতিল") }
            }
        )
    }

    if (askSignOut) {
        AlertDialog(
            onDismissRequest = { askSignOut = false },
            title = { Text("সাইন আউট করবেন?") },
            text = { Text("আপনার সকল ডেটা ক্লাউডে নিরাপদ থাকবে। পুনরায় Google দিয়ে সাইন-ইন করলে সব ফিরে পাবেন।") },
            confirmButton = {
                Button(
                    onClick = { askSignOut = false; onSignOut() },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                    modifier = Modifier.testTag("settings_confirm_signout_btn")
                ) { Text("হ্যাঁ, সাইন আউট") }
            },
            dismissButton = {
                TextButton(onClick = { askSignOut = false }) { Text("বাতিল") }
            }
        )
    }

    pendingRemoveField?.let { field ->
        AlertDialog(
            onDismissRequest = { pendingRemoveField = null },
            title = { Text("কাস্টম ফিল্ড মুছবেন?") },
            text = { Text("\"${field.label}\" কাস্টম ফিল্ডটি তালিকা থেকে মুছে ফেলা হবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveCustomField(field.key)
                        pendingRemoveField = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                    modifier = Modifier.testTag("settings_confirm_remove_field_btn")
                ) { Text("হ্যাঁ, মুছুন") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoveField = null }) { Text("বাতিল") }
            }
        )
    }

    pendingRemoveMember?.let { member ->
        AlertDialog(
            onDismissRequest = { pendingRemoveMember = null },
            title = { Text("সদস্য অপসারণ করবেন?") },
            text = { Text("\"${member.name.ifBlank { member.email }}\"-কে এই বিদ্যালয় থেকে অপসারণ করলে তিনি আর তথ্য দেখতে বা সম্পাদনা করতে পারবেন না।") },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveMember(member.uid)
                        pendingRemoveMember = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                    modifier = Modifier.testTag("settings_confirm_remove_member_btn")
                ) { Text("অপসারণ করুন") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoveMember = null }) { Text("বাতিল") }
            }
        )
    }

    if (showAddCustomClassDialog) {
        AddCustomClassDialog(
            settings = s,
            onDismiss = { showAddCustomClassDialog = false },
            onConfirm = { nameBn, shortBn, tier ->
                onAddCustomClass(nameBn, shortBn, tier)
            }
        )
    }

    pendingRemoveCustomClass?.let { cc ->
        val stuCount = students.count { it.cls == cc.id }
        AlertDialog(
            onDismissRequest = { pendingRemoveCustomClass = null },
            title = { Text("\"${cc.nameBn}\" শ্রেণি মুছবেন?") },
            text = {
                Text(
                    if (stuCount > 0) {
                        "এই শ্রেণির নামের তালিকা সরানো হবে। বর্তমানে থাকা ${bn(stuCount.toString())} জন শিক্ষার্থীর ডাটা মুছে যাবে না।"
                    } else {
                        "\"${cc.nameBn}\" শ্রেণিটি শ্রেণির তালিকা থেকে সরানো হবে।"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveCustomClass(cc.id)
                        pendingRemoveCustomClass = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                    modifier = Modifier.testTag("settings_confirm_remove_class_btn")
                ) { Text("হ্যাঁ, সরান") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoveCustomClass = null }) { Text("বাতিল") }
            }
        )
    }
}

private fun formatTimeBn(millis: Long): String {
    if (millis <= 0L) return ""
    val formatted = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(millis))
    return bn(formatted)
}
