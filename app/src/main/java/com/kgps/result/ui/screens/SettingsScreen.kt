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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.kgps.result.bn
import com.kgps.result.cloud.JoinRequestItem
import com.kgps.result.cloud.MemberRole
import com.kgps.result.cloud.SchoolMemberProfile
import com.kgps.result.cloud.SchoolWorkspace
import com.kgps.result.ui.components.TreeBranchItem
import com.kgps.result.ui.components.TreeGroupCard
import com.kgps.result.ui.theme.AmberBg
import com.kgps.result.ui.theme.AmberBorder
import com.kgps.result.ui.theme.AmberNotice
import com.kgps.result.ui.theme.EmeraldGrade
import com.kgps.result.ui.theme.RoseDanger
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealContainer
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    students: List<Student>,
    settings: Settings,
    currentUserId: String,
    activeSchool: SchoolWorkspace?,
    joinRequests: List<JoinRequestItem>,
    onUpdateSettings: (Settings) -> Unit,
    onAddCustomField: (String) -> Boolean,
    onRemoveCustomField: (String) -> Unit,
    onOpenCsvImport: (String) -> Unit,
    onRestoreBackupJson: (String) -> Unit,
    onExportAllPdf: () -> Unit,
    onResetSampleData: () -> Unit,
    onClearAllStudents: () -> Unit,
    onRegenerateJoinCode: () -> Unit,
    onSetApprovalRequired: (Boolean) -> Unit,
    onApproveJoinRequest: (JoinRequestItem) -> Unit,
    onRejectJoinRequest: (String) -> Unit,
    onUpdateMemberRole: (String, MemberRole) -> Unit,
    onRemoveMember: (String) -> Unit,
    onLeaveSchool: () -> Unit,
    onOpenSchoolHub: () -> Unit,
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
    var expandedGroup by remember { mutableStateOf("collab") } // collab, school, font, teachers, backup
    var newFieldLabel by remember { mutableStateOf("") }

    var askReset by remember { mutableStateOf(false) }
    var askClear by remember { mutableStateOf(false) }
    var askRegenerateCode by remember { mutableStateOf(false) }
    var askLeaveSchool by remember { mutableStateOf(false) }
    var askSignOut by remember { mutableStateOf(false) }
    var pendingRemoveField by remember { mutableStateOf<CustomFieldDef?>(null) }
    var pendingRemoveMember by remember { mutableStateOf<SchoolMemberProfile?>(null) }

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
                    Text(
                        text = "আপনি এই বিদ্যালয়ে \"শিক্ষক\" ভূমিকায় যুক্ত আছেন। বিদ্যালয়ের নাম, কাস্টম ফিল্ড, সদস্য ব্যবস্থাপনা ও মুছে ফেলা সংক্রান্ত সেটিংস শুধুমাত্র অ্যাডমিন পরিবর্তন করতে পারবেন।",
                        fontSize = 12.sp,
                        color = AmberNotice,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Group 1: Collaborative School Workspace, Join Code, Members & Approvals
            if (activeSchool != null) {
                TreeGroupCard(
                    title = "বিদ্যালয় কোড ও সদস্যবৃন্দ",
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

                            if (isAdmin) {
                                Surface(
                                    color = SlateBg,
                                    border = BorderStroke(1.dp, SlateOutline),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text("যোগদানে অ্যাডমিন অনুমোদন", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                            Text(
                                                if (activeSchool.approvalRequired) "চালু: নতুন শিক্ষককে আপনি অনুমোদন করবেন" else "বন্ধ: কোড দিলেই সরাসরি যুক্ত হতে পারবে",
                                                fontSize = 11.sp,
                                                color = SlateMuted
                                            )
                                        }
                                        Switch(
                                            checked = activeSchool.approvalRequired,
                                            onCheckedChange = onSetApprovalRequired,
                                            modifier = Modifier.testTag("settings_approval_switch")
                                        )
                                    }
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
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text(req.displayName.ifBlank { "শিক্ষক" }, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                if (req.email.isNotBlank()) {
                                                    Text(req.email, fontSize = 11.sp, color = SlateMuted)
                                                }
                                            }
                                            OutlinedButton(
                                                onClick = { onRejectJoinRequest(req.uid) },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) { Text("বাতিল", fontSize = 11.5.sp, color = RoseDanger) }
                                            Button(
                                                onClick = { onApproveJoinRequest(req) },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) { Text("অনুমোদন", fontSize = 11.5.sp) }
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
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
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
                                        if (isAdmin && !memberIsOwner && !isSelf) {
                                            val nextRole = if (member.role == MemberRole.ADMIN) MemberRole.TEACHER else MemberRole.ADMIN
                                            TextButton(
                                                onClick = { onUpdateMemberRole(member.uid, nextRole) },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    if (nextRole == MemberRole.ADMIN) "অ্যাডমিন" else "শিক্ষক",
                                                    fontSize = 11.sp
                                                )
                                            }
                                            IconButton(
                                                onClick = { pendingRemoveMember = member },
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Icon(Icons.Rounded.PersonRemove, contentDescription = "সদস্য অপসারণ", tint = RoseDanger, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    TreeBranchItem(isLast = true) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = onOpenSchoolHub,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("settings_switch_school_btn")
                                ) {
                                    Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(4.dp))
                                    Text("বিদ্যালয় বদল/যোগ", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { askSignOut = true },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("settings_sign_out_btn")
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(16.dp), tint = RoseDanger)
                                    Spacer(Modifier.size(4.dp))
                                    Text("সাইন আউট", fontSize = 12.sp, color = RoseDanger)
                                }
                            }
                            if (!isOwner) {
                                OutlinedButton(
                                    onClick = { askLeaveSchool = true },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, RoseDanger.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth().testTag("settings_leave_school_btn")
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp), tint = RoseDanger)
                                    Spacer(Modifier.size(6.dp))
                                    Text("এই বিদ্যালয় ত্যাগ করুন", fontSize = 12.sp, color = RoseDanger)
                                }
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
                                color = if (selected) TealSoft else SlateBg,
                                border = BorderStroke(1.dp, if (selected) TealPrimary else SlateOutline),
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
                                color = if (selected) TealSoft else SlateBg,
                                border = BorderStroke(1.dp, if (selected) TealPrimary else SlateOutline),
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

            // Group 5: Database, CSV & Backup
            TreeGroupCard(
                title = "ডাটাবেস, CSV ও ব্যাকআপ",
                subtitle = "মোট ${bn(students.size.toString())} জন শিক্ষার্থী সংরক্ষিত",
                badgeText = "ব্যাকআপ",
                icon = Icons.Rounded.Backup,
                expanded = expandedGroup == "backup",
                headerTestTag = "settings_group_backup",
                onToggle = { expandedGroup = if (expandedGroup == "backup") "" else "backup" }
            ) {
                TreeBranchItem(isLast = !isAdmin) {
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
                            if (isAdmin) {
                                OutlinedButton(
                                    onClick = { jsonRestoreLauncher.launch(arrayOf("application/json", "*/*")) },
                                    modifier = Modifier.weight(1f).testTag("settings_json_restore_btn"),
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text("JSON রিস্টোর", fontSize = 12.sp) }
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
            text = { Text("আপনি \"${activeSchool?.settings?.school.orEmpty()}\" থেকে বের হয়ে যাবেন। পুনরায় যুক্ত হতে হলে আবার স্কুল কোড ব্যবহার করতে হবে।") },
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
