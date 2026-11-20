package com.kgps.result.ui.screens

import android.net.Uri
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
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
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
import com.kgps.result.ai.DiscoveredGeminiModel
import com.kgps.result.ai.GeminiApiConfig
import com.kgps.result.backup.AutoBackupStatus
import com.kgps.result.backup.DriveBackupFile
import com.kgps.result.backup.LocalBackupEntry
import com.kgps.result.bn
import com.kgps.result.cloud.JoinRequestItem
import com.kgps.result.cloud.MemberRole
import com.kgps.result.cloud.SchoolMemberProfile
import com.kgps.result.cloud.SchoolWorkspace
import com.kgps.result.cloud.TeacherPermissions
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
import com.kgps.result.ui.theme.appFilterChipBorder
import com.kgps.result.ui.theme.appFilterChipColors
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
    onUpdateSettings: (Settings) -> Unit,
    onAddCustomField: (String) -> Boolean,
    onRemoveCustomField: (String) -> Unit,
    onOpenCsvImport: (String) -> Unit,
    onRestoreBackupJson: (String) -> Unit,
    onExportAllPdf: () -> Unit,
    onResetSampleData: () -> Unit,
    onClearAllStudents: () -> Unit,
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
    onBackupNowBoth: () -> Unit,
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
    var expandedGroup by remember { mutableStateOf("collab") } // collab, school, font, teachers, backup, aiproxy
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

    var askReset by remember { mutableStateOf(false) }
    var askClear by remember { mutableStateOf(false) }
    var askRegenerateCode by remember { mutableStateOf(false) }
    var askLeaveSchool by remember { mutableStateOf(false) }
    var askSignOut by remember { mutableStateOf(false) }
    var pendingRemoveField by remember { mutableStateOf<CustomFieldDef?>(null) }
    var pendingRemoveMember by remember { mutableStateOf<SchoolMemberProfile?>(null) }
    var pendingApproveRequest by remember { mutableStateOf<JoinRequestItem?>(null) }
    var pendingEditMember by remember { mutableStateOf<SchoolMemberProfile?>(null) }

    var showLocalSnapshotsDialog by remember { mutableStateOf(false) }
    var showDriveBackupsDialog by remember { mutableStateOf(false) }
    var confirmLocalRestore by remember { mutableStateOf<LocalBackupEntry?>(null) }
    var confirmDriveRestore by remember { mutableStateOf<DriveBackupFile?>(null) }
    var pendingManualJsonRestore by remember { mutableStateOf<String?>(null) }

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

            // Group 1: Collaborative School Workspace, Join Code, Members & Approvals
            if (activeSchool != null) {
                TreeGroupCard(
                    title = "বিদ্যালয় কোড, সদস্য ও অনুমতি",
                    subtitle = "কোড: ${activeSchool.joinCode} · ${bn(activeSchool.members.size.toString())} জন শিক্ষক",
                    badgeText = if (joinRequests.isNotEmpty() && isAdmin) "${bn(joinRequests.size.toString())} অনুরোধ" else activeSchool.roleOf(currentUserId).labelBn,
                    badgeColor = if (joinRequests.isNotEmpty() && isAdmin) AmberNotice else TealPrimary,
                    badgeContainerColor = if (joinRequests.isNotEmpty() && isAdmin) AmberBg else TealSoft,
                    icon = Icons.Rounded.AdminPanelSettings,
                    expanded = expandedGroup == "collab",
                    headerTestTag = "settings_group_collab",
                    onToggle = { expandedGroup = if (expandedGroup == "collab") "" else "collab" }
                ) {
                    TreeBranchItem(isLast = false) {
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
                        TreeBranchItem(isLast = false) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("অপেক্ষমান যোগদানের অনুরোধ (${bn(joinRequests.size.toString())})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = AmberNotice)
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

                    TreeBranchItem(isLast = false) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("সদস্য তালিকা (${bn(activeSchool.members.size.toString())} জন)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TealPrimary)
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

                    TreeBranchItem(isLast = true) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!isOwner) {
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
                }
            }

            // Group 2: School Information
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
                            label = { Text("বিদ্যালয়ের ঠিকানা (ব্যাক কভারে মুদ্রিত হবে)") },
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

            // Group 3: PDF Fonts & DPE Cover Logo
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
                                    .clickable(enabled = isAdmin) {
                                        val next = s.copy(pdfFont = font.id)
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
                                            val next = s.copy(pdfFont = font.id)
                                            s = next
                                            onUpdateSettings(next)
                                        }
                                    )
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

            // Group 4: Teachers & Custom Fields
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

                TreeBranchItem(isLast = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("কাস্টম ডাটাবেস ফিল্ড", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TealPrimary)
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

            // Group 5: Side-by-Side Auto Backup (Local + Google Drive), CSV & Database
            TreeGroupCard(
                title = "অটো ব্যাকআপ (লোকাল + Drive) ও CSV",
                subtitle = "মোট ${bn(students.size.toString())} জন · Firestore + লোকাল + Drive",
                badgeText = "ব্যাকআপ",
                icon = Icons.Rounded.Backup,
                expanded = expandedGroup == "backup",
                headerTestTag = "settings_group_backup",
                onToggle = { expandedGroup = if (expandedGroup == "backup") "" else "backup" }
            ) {
                // Auto Backup Controls (Local + Google Drive side by side with Firestore)
                TreeBranchItem(isLast = false) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onBackupNowBoth,
                            enabled = !backupStatus.driveBusy,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("settings_backup_both_now_btn")
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

                        // Local Auto Backup Card
                        Surface(
                            color = SlateBg,
                            border = BorderStroke(1.dp, SlateOutline),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                        "সর্বশেষ লোকাল ব্যাকআপ: ${formatTimeBn(backupStatus.lastLocalBackupMs)} (${backupStatus.lastLocalFileName})",
                                        fontSize = 11.sp,
                                        color = EmeraldGrade
                                    )
                                }
                                if (!backupStatus.lastLocalError.isNullOrBlank()) {
                                    Text("ত্রুটি: ${backupStatus.lastLocalError}", fontSize = 11.sp, color = RoseDanger)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { folderPickerLauncher.launch(null) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.size(4.dp))
                                        Text("ফোল্ডার বাছাই", fontSize = 11.5.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { showLocalSnapshotsDialog = true },
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

                        // Google Drive Auto Backup Card
                        Surface(
                            color = TealSoft,
                            border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                        "সর্বশেষ Drive ব্যাকআপ: ${formatTimeBn(backupStatus.lastDriveBackupMs)} (${backupStatus.lastDriveFileName})",
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
                                    onClick = {
                                        onLoadDriveBackups()
                                        showDriveBackupsDialog = true
                                    },
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

                // Manual Export / Import / PDF
                TreeBranchItem(isLast = !isAdmin) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (permissions.canExportPdf) {
                            Button(
                                onClick = onExportAllPdf,
                                modifier = Modifier.fillMaxWidth().testTag("settings_all_pdf_btn"),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("সব শ্রেণির সব শিক্ষার্থীর PDF ডাউনলোড") }
                        }

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
                        if (permissions.canManageStudents) {
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
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { jsonExportLauncher.launch("result_maker_backup.json") },
                                modifier = Modifier.weight(1f).testTag("settings_json_export_btn"),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("JSON এক্সপোর্ট", fontSize = 12.sp) }
                            if (isAdmin) {
                                OutlinedButton(
                                    onClick = { jsonRestoreLauncher.launch(arrayOf("application/json", "*/*")) },
                                    modifier = Modifier.weight(1f).testTag("settings_json_restore_btn"),
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text("ফাইল থেকে রিস্টোর", fontSize = 12.sp) }
                            }
                        }
                    }
                }

                if (isAdmin) {
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
            }

            // Group 6: Manual Gemini API Key & Latest Model Suggestions
            TreeGroupCard(
                title = "Gemini AI ও মডেল",
                subtitle = if (geminiApiKey.isNotBlank()) {
                    "সক্রিয় মডেল: $geminiModel · API Key সংরক্ষিত"
                } else {
                    "Google AI Studio থেকে নিজস্ব Gemini API Key যুক্ত করুন"
                },
                badgeText = if (geminiApiKey.isNotBlank()) "সংযুক্ত" else "সেটআপ বাকি",
                badgeColor = if (geminiApiKey.isNotBlank()) EmeraldGrade else AmberNotice,
                badgeContainerColor = if (geminiApiKey.isNotBlank()) TealSoft else AmberBg,
                icon = Icons.Rounded.AutoAwesome,
                expanded = expandedGroup == "aiproxy",
                headerTestTag = "settings_group_aiproxy",
                onToggle = { expandedGroup = if (expandedGroup == "aiproxy") "" else "aiproxy" }
            ) {
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
                            onValueChange = { apiKeyInput = it },
                            label = { Text("Gemini API Key") },
                            placeholder = { Text("AIza...") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { showApiKey = !showApiKey },
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
                                    .clickable { modelInput = preset.id }
                                    .testTag("settings_model_preset_${preset.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { modelInput = preset.id }
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

                        val suggestedIds = remember { GeminiApiConfig.SUGGESTED_MODEL_IDS }
                        val normalizedTypedModel = remember(modelInput) {
                            modelInput.trim().removePrefix("models/").trim()
                        }
                        val extraLiveModels = remember(geminiAvailableModels, geminiNewModelIds) {
                            geminiAvailableModels
                                .filter { it.id !in suggestedIds }
                                .sortedWith(
                                    compareByDescending<DiscoveredGeminiModel> { it.id in geminiNewModelIds }
                                )
                        }
                        val filteredLiveModels = remember(extraLiveModels, modelSearchQuery) {
                            val q = modelSearchQuery.trim().lowercase()
                            if (q.isBlank()) extraLiveModels
                            else extraLiveModels.filter {
                                it.id.lowercase().contains(q) || it.displayName.lowercase().contains(q)
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
                                        Surface(
                                            color = TealSoft,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
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
                                onClick = {
                                    onRefreshGeminiModels(apiKeyInput.ifBlank { geminiApiKey })
                                },
                                enabled = !geminiRefreshingModels,
                                modifier = Modifier.testTag("settings_refresh_models_btn"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                if (geminiRefreshingModels) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Rounded.Refresh,
                                        contentDescription = "নতুন মডেল খুঁজুন",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.size(4.dp))
                                    Text("রিফ্রেশ", fontSize = 11.5.sp)
                                }
                            }
                        }

                        if (!geminiModelsError.isNullOrBlank()) {
                            Text(
                                text = geminiModelsError,
                                fontSize = 11.sp,
                                color = RoseDanger,
                                lineHeight = 15.sp
                            )
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
                                    onValueChange = { modelSearchQuery = it },
                                    placeholder = { Text("নতুন মডেল খুঁজুন (যেমন: 4.5, pro, flash)...", fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                    },
                                    trailingIcon = {
                                        if (modelSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { modelSearchQuery = "" }) {
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
                                            onClick = { modelInput = item.id },
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
                                        onClick = { showAllLiveModels = !showAllLiveModels },
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
                                        onClick = { modelInput = customId },
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
                            onValueChange = { modelInput = it },
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
                                        onClick = { modelInput = GeminiApiConfig.DEFAULT_MODEL },
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
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
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
                                onClick = {
                                    apiKeyInput = ""
                                    onClearGeminiApiKey()
                                },
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

    // Dialog: Approve Join Request + Assign Granular Permissions
    pendingApproveRequest?.let { req ->
        TeacherPermissionsEditorDialog(
            title = "শিক্ষক অনুমোদন ও অনুমতি নির্ধারণ",
            teacherLabel = "${req.displayName.ifBlank { "শিক্ষক" }} (${req.email})",
            initialPermissions = TeacherPermissions.fullAccess(),
            confirmButtonText = "অনুমোদন করুন",
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
}

private fun formatTimeBn(millis: Long): String {
    if (millis <= 0L) return ""
    val formatted = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(millis))
    return bn(formatted)
}
