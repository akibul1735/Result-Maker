package com.kgps.result.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContactPhone
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.DEFAULT_DISTRICT
import com.kgps.result.DEFAULT_UPAZILA
import com.kgps.result.RELIGIONS
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.TeacherRemarkEngine
import com.kgps.result.activeClassIds
import com.kgps.result.bn
import com.kgps.result.classShortBn
import com.kgps.result.cloud.StudentSyncMeta
import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.fmt
import com.kgps.result.fmtEn
import com.kgps.result.fromBn
import com.kgps.result.gradeLabelBn
import com.kgps.result.gradeOf
import com.kgps.result.hasSub
import com.kgps.result.hasTerm
import com.kgps.result.mk
import com.kgps.result.overallPct
import com.kgps.result.subs
import com.kgps.result.termNameBn
import com.kgps.result.total
import com.kgps.result.ui.components.InlineTeacherRemarkSection
import com.kgps.result.ui.components.TreeBranchItem
import com.kgps.result.ui.components.TreeGroupCard
import com.kgps.result.ui.theme.AmberBg
import com.kgps.result.ui.theme.AmberBorder
import com.kgps.result.ui.theme.AmberNotice
import com.kgps.result.ui.theme.EmeraldGrade
import com.kgps.result.ui.theme.RoseDanger
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.TealContainer
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft
import com.kgps.result.ui.theme.appFilterChipBorder
import com.kgps.result.ui.theme.appFilterChipColors

@Composable
fun StudentEditorScreen(
    initialStudent: Student,
    settings: Settings,
    permissions: TeacherPermissions,
    syncMeta: StudentSyncMeta? = null,
    draftingRemarkTerm: Int?,
    isGeminiConfigured: Boolean,
    onAddCustomField: (String) -> Boolean,
    onUpsertStudent: (Student) -> Unit,
    onDeleteStudent: (String) -> Unit,
    onBack: () -> Unit,
    onPreview: (Student) -> Unit,
    onPdf: (Student) -> Unit,
    onDraftRemark: (Student, Int, (String) -> Unit) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val prefillInit = remember(initialStudent.id, permissions.canEditAttendance) {
        val base = initialStudent.copy(
            upazila = initialStudent.upazila.ifBlank { DEFAULT_UPAZILA },
            district = initialStudent.district.ifBlank { DEFAULT_DISTRICT }
        )
        if (!permissions.canEditAttendance) {
            Triple(base, emptyMap<Int, String>(), setOf(1, 2, 3))
        } else {
            val nextX = base.x.toMutableMap()
            val autoMap = mutableMapOf<Int, String>()
            val locked = mutableSetOf<Int>()
            (1..3).forEach { t ->
                val remKey = "REM_$t"
                val curRem = nextX[remKey]?.trim().orEmpty()
                val autoRem = TeacherRemarkEngine.autoPrefillIfApplicable(base, t)
                if (curRem.isEmpty() && autoRem != null) {
                    nextX[remKey] = autoRem
                    autoMap[t] = autoRem
                } else if (autoRem != null && curRem == autoRem) {
                    autoMap[t] = autoRem
                } else if (curRem.isNotEmpty()) {
                    locked.add(t)
                }
            }
            Triple(base.copy(x = nextX), autoMap.toMap(), locked.toSet())
        }
    }
    var s by remember(initialStudent.id) { mutableStateOf(prefillInit.first) }
    var autoManagedRemarks by remember(initialStudent.id) { mutableStateOf(prefillInit.second) }
    var manuallyLockedTerms by remember(initialStudent.id) { mutableStateOf(prefillInit.third) }
    val cur by rememberUpdatedState(s)
    val deleted = remember { booleanArrayOf(false) }
    val defaultTab = remember(permissions) {
        when {
            permissions.canManageStudents -> 0
            permissions.allSubjects || permissions.effectiveSubjects().isNotEmpty() || permissions.canViewOtherSubjects -> 1
            permissions.canEditAttendance -> 2
            else -> 0
        }
    }
    var tab by remember(initialStudent.id) { mutableStateOf(defaultTab) }
    var askDelete by remember { mutableStateOf(false) }

    val canEditAnything = permissions.canManageStudents ||
        permissions.canEditAttendance ||
        permissions.allSubjects ||
        permissions.effectiveSubjects().isNotEmpty()

    // Tree expansion states
    var infoExpandedGroup by remember { mutableStateOf("identity") }
    var expandedTerm by remember { mutableStateOf<Int?>(1) }
    var expandedSubKey by remember { mutableStateOf<String?>(null) }
    var expandedTraitsTerm by remember { mutableStateOf<Int?>(1) }

    LaunchedEffect(s.m, s.x, permissions.canEditAttendance, manuallyLockedTerms) {
        if (!permissions.canEditAttendance) return@LaunchedEffect
        var updatedX: MutableMap<String, String>? = null
        var updatedAuto: MutableMap<Int, String>? = null
        (1..3).forEach { t ->
            if (t !in manuallyLockedTerms) {
                val remKey = "REM_$t"
                val curRem = (updatedX ?: s.x)[remKey]?.trim().orEmpty()
                val lastAuto = (updatedAuto ?: autoManagedRemarks)[t]
                if (curRem.isEmpty() || (lastAuto != null && curRem == lastAuto)) {
                    val nextAuto = TeacherRemarkEngine.autoPrefillIfApplicable(s, t)
                    if (nextAuto != null && nextAuto != curRem) {
                        val xm = updatedX ?: s.x.toMutableMap().also { updatedX = it }
                        xm[remKey] = nextAuto
                        val am = updatedAuto ?: autoManagedRemarks.toMutableMap().also { updatedAuto = it }
                        am[t] = nextAuto
                    }
                }
            }
        }
        updatedX?.let { s = s.copy(x = it) }
        updatedAuto?.let { autoManagedRemarks = it }
    }

    DisposableEffect(initialStudent.id) {
        onDispose {
            if (!deleted[0] && canEditAnything && cur.name.isNotBlank() && cur != initialStudent) {
                onUpsertStudent(cur)
            }
        }
    }
    BackHandler { onBack() }

    fun setX(k: String, v: String) {
        s = s.copy(x = if (v.isBlank()) s.x - k else s.x + (k to v))
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        // Editor Header Bar with explicit Save CTA
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, SlateOutline)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp).testTag("editor_back_btn")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "ফিরে যান")
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        s.name.ifBlank { "নতুন শিক্ষার্থী" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val editorSub = buildString {
                        append("শ্রেণি ${bn(s.cls.toString())} · রোল ${bn(s.roll.toString())}")
                        val by = syncMeta?.updatedByName?.trim()
                        if (!by.isNullOrBlank()) append(" · সর্বশেষ: $by")
                    }
                    Text(
                        editorSub,
                        fontSize = 11.sp,
                        color = SlateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (canEditAnything) {
                    IconButton(
                        onClick = {
                            if (s.name.isNotBlank()) {
                                onUpsertStudent(s)
                                onShowMessage("${s.name} সংরক্ষিত হয়েছে")
                                onBack()
                            } else {
                                onShowMessage("প্রথমে শিক্ষার্থীর নাম লিখুন")
                            }
                        },
                        modifier = Modifier.size(48.dp).testTag("editor_save_btn")
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = "সংরক্ষণ করুন", tint = EmeraldGrade)
                    }
                }
                if (permissions.canExportPdf) {
                    IconButton(
                        onClick = {
                            if (s.name.isNotBlank()) {
                                if (canEditAnything && s != initialStudent) onUpsertStudent(s)
                                onPreview(s)
                            } else {
                                onShowMessage("প্রথমে শিক্ষার্থীর নাম লিখুন")
                            }
                        },
                        modifier = Modifier.size(48.dp).testTag("editor_preview_btn")
                    ) {
                        Icon(Icons.Rounded.Visibility, contentDescription = "প্রিভিউ দেখুন", tint = TealPrimary)
                    }
                    IconButton(
                        onClick = { onPdf(s) },
                        modifier = Modifier.size(48.dp).testTag("editor_pdf_btn")
                    ) {
                        Icon(Icons.Rounded.Description, contentDescription = "PDF তৈরি করুন", tint = TealPrimary)
                    }
                }
                if (permissions.canManageStudents) {
                    IconButton(
                        onClick = { askDelete = true },
                        modifier = Modifier.size(48.dp).testTag("editor_delete_btn")
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = "শিক্ষার্থী মুছুন", tint = RoseDanger)
                    }
                }
            }
        }

        TabRow(selectedTabIndex = tab) {
            listOf("তথ্য", "নম্বর", "উপস্থিতি ও গুণাবলি").forEachIndexed { i, title ->
                Tab(
                    selected = tab == i,
                    onClick = { tab = i },
                    modifier = Modifier.testTag("editor_tab_$i"),
                    text = { Text(title, maxLines = 1, fontSize = 13.sp) }
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (tab) {
                    0 -> {
                        val canEditProfile = permissions.canManageStudents
                        if (!canEditProfile) {
                            ReadOnlyPermissionBanner("শিক্ষার্থীর ব্যক্তিগত তথ্য ও ঠিকানা সম্পাদনার অনুমতি আপনার নেই (শুধুমাত্র দেখার জন্য)।")
                        }
                        // Tab 0: Tree-Grouped Personal Info
                        TreeGroupCard(
                            title = "পরিচিতি ও শ্রেণি",
                            subtitle = "${s.name.ifBlank { "নাম লিখুন" }} · রোল ${bn(s.roll.toString())}",
                            badgeText = s.religion,
                            icon = Icons.Rounded.Badge,
                            expanded = infoExpandedGroup == "identity",
                            headerTestTag = "editor_group_identity",
                            onToggle = { infoExpandedGroup = if (infoExpandedGroup == "identity") "" else "identity" }
                        ) {
                            TreeBranchItem(isLast = true) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(Modifier.weight(1.7f)) {
                                            EditorTextField("শিক্ষার্থীর নাম", s.name, enabled = canEditProfile) {
                                                s = s.copy(name = it)
                                            }
                                        }
                                        Box(Modifier.weight(0.8f)) {
                                            EditorTextField(
                                                "রোল",
                                                if (s.roll == 0) "" else s.roll.toString(),
                                                number = true,
                                                enabled = canEditProfile
                                            ) { s = s.copy(roll = fromBn(it).toIntOrNull() ?: 0) }
                                        }
                                    }
                                    Text("শ্রেণি", style = MaterialTheme.typography.labelMedium, color = SlateMuted)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.horizontalScroll(rememberScrollState())
                                    ) {
                                        permissions.effectiveClasses(activeClassIds(settings)).forEach { c ->
                                            val isSel = s.cls == c
                                            FilterChip(
                                                selected = isSel,
                                                enabled = canEditProfile,
                                                onClick = { if (canEditProfile) s = s.copy(cls = c) },
                                                colors = appFilterChipColors(),
                                                border = appFilterChipBorder(isSel, enabled = canEditProfile),
                                                modifier = Modifier.height(32.dp),
                                                label = { Text(classShortBn(c, settings), fontSize = 11.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) }
                                            )
                                        }
                                    }
                                    Text("ধর্ম", style = MaterialTheme.typography.labelMedium, color = SlateMuted)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        modifier = Modifier.horizontalScroll(rememberScrollState())
                                    ) {
                                        RELIGIONS.forEach { r ->
                                            val isSel = s.religion == r
                                            FilterChip(
                                                selected = isSel,
                                                enabled = canEditProfile,
                                                onClick = { if (canEditProfile) s = s.copy(religion = r) },
                                                colors = appFilterChipColors(),
                                                border = appFilterChipBorder(isSel, enabled = canEditProfile),
                                                modifier = Modifier.height(32.dp),
                                                label = { Text(r, fontSize = 11.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        TreeGroupCard(
                            title = "ঠিকানা",
                            subtitle = listOf(
                                s.village,
                                s.union,
                                s.post,
                                s.upazila.ifBlank { DEFAULT_UPAZILA },
                                s.district.ifBlank { DEFAULT_DISTRICT }
                            ).filter { it.isNotBlank() }.joinToString(", "),
                            icon = Icons.Rounded.Home,
                            expanded = infoExpandedGroup == "address",
                            headerTestTag = "editor_group_address",
                            onToggle = { infoExpandedGroup = if (infoExpandedGroup == "address") "" else "address" }
                        ) {
                            TreeBranchItem(isLast = true) {
                                Column {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(Modifier.weight(1f)) { EditorTextField("গ্রাম / মহল্লা", s.village, enabled = canEditProfile) { s = s.copy(village = it) } }
                                        Box(Modifier.weight(1f)) { EditorTextField("ইউনিয়ন (ঐচ্ছিক)", s.union, enabled = canEditProfile) { s = s.copy(union = it) } }
                                    }
                                    EditorTextField("ডাকঘর / পোস্ট অফিস (ঐচ্ছিক)", s.post, enabled = canEditProfile) { s = s.copy(post = it) }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(Modifier.weight(1f)) { EditorTextField("উপজেলা / থানা", s.upazila, enabled = canEditProfile) { s = s.copy(upazila = it) } }
                                        Box(Modifier.weight(1f)) { EditorTextField("জেলা", s.district, enabled = canEditProfile) { s = s.copy(district = it) } }
                                    }
                                }
                            }
                        }

                        TreeGroupCard(
                            title = "অভিভাবক ও যোগাযোগ",
                            subtitle = s.guardian.ifBlank { "অভিভাবকের নাম ও মোবাইল নম্বর" },
                            badgeText = s.mobile.takeIf { it.isNotBlank() },
                            icon = Icons.Rounded.ContactPhone,
                            expanded = infoExpandedGroup == "guardian",
                            headerTestTag = "editor_group_guardian",
                            onToggle = { infoExpandedGroup = if (infoExpandedGroup == "guardian") "" else "guardian" }
                        ) {
                            TreeBranchItem(isLast = true) {
                                Column {
                                    EditorTextField("পিতা / অভিভাবকের নাম", s.guardian, enabled = canEditProfile) { s = s.copy(guardian = it) }
                                    EditorTextField("মোবাইল নম্বর", s.mobile, number = true, enabled = canEditProfile) { s = s.copy(mobile = it) }
                                }
                            }
                        }

                        val registeredCustomKeys = settings.customFields.map { it.key }.toSet()
                        val textKeySet = com.kgps.result.Repo.TEXT_KEYS.toSet()
                        val orphanCustomEntries = s.x.entries
                            .filter { (k, v) -> v.isNotBlank() && k !in registeredCustomKeys && k !in textKeySet }
                            .sortedBy { it.key }

                        TreeGroupCard(
                            title = "অতিরিক্ত ও কাস্টম ফিল্ড",
                            subtitle = "${bn((settings.customFields.size + orphanCustomEntries.size).toString())}টি কাস্টম ডাটাবেস ফিল্ড",
                            icon = Icons.Rounded.Tune,
                            expanded = infoExpandedGroup == "custom",
                            headerTestTag = "editor_group_custom",
                            onToggle = { infoExpandedGroup = if (infoExpandedGroup == "custom") "" else "custom" }
                        ) {
                            TreeBranchItem(isLast = true) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (canEditProfile) {
                                        var newFieldLabel by remember { mutableStateOf("") }
                                        Row(
                                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = newFieldLabel,
                                                onValueChange = { newFieldLabel = it },
                                                placeholder = { Text("নতুন ফিল্ড (যেমন: মাতার নাম)", fontSize = 12.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f).testTag("editor_new_custom_field_input")
                                            )
                                            Button(
                                                onClick = {
                                                    if (onAddCustomField(newFieldLabel)) newFieldLabel = ""
                                                },
                                                enabled = newFieldLabel.isNotBlank(),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("editor_add_custom_field_btn")
                                            ) {
                                                Text("+ ফিল্ড", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                    settings.customFields.forEach { cf ->
                                        EditorTextField(cf.label, s.x[cf.key] ?: "", enabled = canEditProfile) { setX(cf.key, it) }
                                    }
                                    orphanCustomEntries.forEach { (k, v) ->
                                        EditorTextField(k, v, enabled = canEditProfile) { setX(k, it) }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        val visibleSubs = s.subs().filter { permissions.canViewSubject(it.key) }
                        if (visibleSubs.isEmpty()) {
                            ReadOnlyPermissionBanner("কোনো বিষয়ের নম্বর দেখা বা সম্পাদনার অনুমতি আপনার নেই।")
                            return@Column
                        }

                        // Show full overall grade card only when all subjects are viewable
                        if (permissions.allSubjects || permissions.canViewOtherSubjects) {
                            val overall = s.overallPct()
                            val overallGrade = gradeOf(overall)
                            Card(
                                Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = TealSoft),
                                border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("সমন্বিত চূড়ান্ত ফলাফল", fontSize = 11.5.sp, color = SlateMuted)
                                        Text(
                                            "${fmt(overall)}% — ${gradeLabelBn(overallGrade)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = TealPrimary
                                        )
                                    }
                                    Surface(color = TealPrimary, shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            "গ্রেড $overallGrade",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            ReadOnlyPermissionBanner("শুধুমাত্র আপনার নির্ধারিত বিষয়গুলো (${visibleSubs.joinToString(", ") { it.bn }}) প্রদর্শিত হচ্ছে।")
                        }

                        val maxAll = visibleSubs.sumOf { it.max }.coerceAtLeast(1)

                        (1..3).forEach { t ->
                            val hasT = visibleSubs.any { s.hasSub(it, t) }
                            val termSum = visibleSubs.sumOf { s.total(it, t) }
                            val termPct = termSum / maxAll * 100.0
                            val termGrade = gradeOf(termPct)

                            TreeGroupCard(
                                title = "${termNameBn(t)} মূল্যায়ন",
                                subtitle = if (hasT) "নির্ধারিত বিষয়ে মোট ${fmt(termSum)} / ${bn(maxAll.toString())}" else "এখনো নম্বর এন্ট্রি হয়নি",
                                badgeText = if (hasT) "$termGrade · ${fmt(termPct)}%" else "খালি",
                                badgeColor = if (hasT) EmeraldGrade else SlateMuted,
                                icon = Icons.Rounded.FolderOpen,
                                expanded = expandedTerm == t,
                                headerTestTag = "editor_marks_term_$t",
                                onToggle = {
                                    expandedTerm = if (expandedTerm == t) null else t
                                }
                            ) {
                                visibleSubs.forEachIndexed { sIdx, sub ->
                                    val isLastSub = sIdx == visibleSubs.lastIndex
                                    val subKeyForTerm = "${t}_${sub.key}"
                                    val subTot = s.total(sub, t)
                                    val subHas = s.hasSub(sub, t)
                                    val subGrade = gradeOf(subTot / sub.max.toDouble() * 100.0)
                                    val canEditSub = permissions.canEditSubject(sub.key)

                                    TreeBranchItem(isLast = isLastSub) {
                                        TreeGroupCard(
                                            title = sub.bn + if (!canEditSub) " (শুধু দেখা)" else "",
                                            subtitle = "পূর্ণমান ${bn(sub.max.toString())}",
                                            badgeText = if (subHas) "${fmt(subTot)} ($subGrade)" else "—",
                                            badgeColor = if (subHas) TealPrimary else SlateMuted,
                                            icon = Icons.AutoMirrored.Rounded.MenuBook,
                                            expanded = expandedSubKey == subKeyForTerm,
                                            headerTestTag = "editor_sub_${t}_${sub.key}",
                                            onToggle = {
                                                expandedSubKey = if (expandedSubKey == subKeyForTerm) null else subKeyForTerm
                                            }
                                        ) {
                                            val chunked = sub.comps.chunked(2)
                                            chunked.forEach { pair ->
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    pair.forEach { c ->
                                                        val k = mk(sub.key, t, c.key)
                                                        val shortLabel = when (c.key) {
                                                            "TB" -> "পাঠ্যবই (${bn(c.max.toString())})"
                                                            "ACT" -> "সক্রিয়তা (${bn(c.max.toString())})"
                                                            "LANG" -> "ভাষা (${bn(c.max.toString())})"
                                                            "CT" -> "ক্লাস টেস্ট (${bn(c.max.toString())})"
                                                            else -> "${c.bn} (${bn(c.max.toString())})"
                                                        }
                                                        Box(Modifier.weight(1f)) {
                                                            val ck = mk(sub.key, t, "CONT")
                                                            val tk = mk(sub.key, t, "TOTAL")
                                                            MarkField(
                                                                label = shortLabel,
                                                                max = c.max.toDouble(),
                                                                value = s.m[k],
                                                                k = k,
                                                                enabled = canEditSub
                                                            ) { v ->
                                                                s = s.copy(m = if (v == null) s.m - k else (s.m - ck - tk) + (k to v))
                                                            }
                                                        }
                                                    }
                                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                                }
                                            }
                                            val ck = mk(sub.key, t, "CONT")
                                            val tk = mk(sub.key, t, "TOTAL")
                                            val hasBreakdown = sub.comps.any { s.m.containsKey(mk(sub.key, t, it.key)) }
                                            val legacyKey = when {
                                                s.m.containsKey(ck) -> ck
                                                sub.written == 0 && s.m.containsKey(tk) -> tk
                                                else -> null
                                            }
                                            if (!hasBreakdown && legacyKey != null) {
                                                val contMax = sub.comps.sumOf { it.max }
                                                MarkField(
                                                    label = "ধারাবাহিক মোট (সরাসরি · সর্বোচ্চ ${bn(contMax.toString())})",
                                                    max = contMax.toDouble(),
                                                    value = s.m[legacyKey],
                                                    k = legacyKey,
                                                    enabled = canEditSub
                                                ) { v ->
                                                    s = s.copy(m = if (v == null) s.m - legacyKey else s.m + (legacyKey to v))
                                                }
                                            }
                                            if (sub.written > 0) {
                                                val wk = mk(sub.key, t, "WR")
                                                MarkField(
                                                    label = "সামষ্টিক / লিখিত (সর্বোচ্চ ${bn(sub.written.toString())})",
                                                    max = sub.written.toDouble(),
                                                    value = s.m[wk],
                                                    k = wk,
                                                    enabled = canEditSub
                                                ) { v ->
                                                    s = s.copy(m = if (v == null) s.m - wk else s.m + (wk to v))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        val canEditAtt = permissions.canEditAttendance
                        if (!canEditAtt) {
                            ReadOnlyPermissionBanner("উপস্থিতি, গুণাবলি ও মন্তব্য সম্পাদনার অনুমতি আপনার নেই (শুধুমাত্র দেখার জন্য)।")
                        }

                        (1..3).forEach { t ->
                            val remKey = "REM_$t"
                            val remarkText = s.x[remKey] ?: ""
                            val dVal = s.x["DAYS_$t"]?.let { fromBn(it).toDoubleOrNull() }
                            val pVal = s.x["PRES_$t"]?.let { fromBn(it).toDoubleOrNull() }
                            val attPct = if (dVal != null && pVal != null && dVal > 0) "${fmt((pVal / dVal) * 100.0)}%" else null

                            TreeGroupCard(
                                title = "${termNameBn(t)} — উপস্থিতি ও গুণাবলি",
                                subtitle = listOfNotNull(
                                    s.x["ACT_$t"]?.let { "সক্রিয়তা: $it" },
                                    s.x["DISC_$t"]?.let { "শৃঙ্খলা: $it" },
                                    s.x["COND_$t"]?.let { "আচরণ: $it" }
                                ).joinToString(" · ").ifBlank { "উপস্থিতি, গুণাবলি ও স্বয়ংক্রিয় মন্তব্য" },
                                badgeText = attPct?.let { "উপস্থিতি $it" } ?: if (remarkText.isNotBlank()) "মন্তব্য প্রস্তুত" else "তথ্য দিন",
                                badgeColor = if (attPct != null || remarkText.isNotBlank()) EmeraldGrade else SlateMuted,
                                icon = Icons.Rounded.EventAvailable,
                                expanded = expandedTraitsTerm == t,
                                headerTestTag = "editor_traits_term_$t",
                                onToggle = { expandedTraitsTerm = if (expandedTraitsTerm == t) null else t }
                            ) {
                                TreeBranchItem(isLast = true) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(Modifier.weight(1f)) {
                                                EditorTextField("মোট কার্যদিবস", s.x["DAYS_$t"] ?: "", number = true, enabled = canEditAtt) {
                                                    setX("DAYS_$t", it)
                                                }
                                            }
                                            Box(Modifier.weight(1f)) {
                                                EditorTextField("উপস্থিতি", s.x["PRES_$t"] ?: "", number = true, enabled = canEditAtt) {
                                                    setX("PRES_$t", it)
                                                }
                                            }
                                        }

                                        listOf(
                                            "ACT_$t" to "সক্রিয়তা",
                                            "DISC_$t" to "শৃঙ্খলা",
                                            "COND_$t" to "আচরণ"
                                        ).forEach { (k, label) ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                                listOf("ক", "খ", "গ").forEach { opt ->
                                                    val isSel = s.x[k] == opt
                                                    FilterChip(
                                                        selected = isSel,
                                                        enabled = canEditAtt,
                                                        onClick = { if (canEditAtt) setX(k, if (isSel) "" else opt) },
                                                        colors = appFilterChipColors(),
                                                        border = appFilterChipBorder(isSel, enabled = canEditAtt),
                                                        modifier = Modifier.height(32.dp),
                                                        label = { Text(opt, fontSize = 11.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) }
                                                    )
                                                }
                                            }
                                        }

                                        InlineTeacherRemarkSection(
                                            student = s,
                                            term = t,
                                            remarkText = remarkText,
                                            enabled = canEditAtt,
                                            isAutoSynced = t !in manuallyLockedTerms && remarkText.isNotBlank(),
                                            isGeminiConfigured = isGeminiConfigured,
                                            draftingRemarkTerm = draftingRemarkTerm,
                                            onSelectRemark = { chosen ->
                                                manuallyLockedTerms = manuallyLockedTerms + t
                                                setX(remKey, chosen)
                                            },
                                            onManualEditRemark = { edited ->
                                                manuallyLockedTerms = manuallyLockedTerms + t
                                                setX(remKey, edited)
                                            },
                                            onResetAutoRemark = {
                                                val top = TeacherRemarkEngine.bestRemark(s, t).text
                                                manuallyLockedTerms = manuallyLockedTerms - t
                                                autoManagedRemarks = autoManagedRemarks + (t to top)
                                                setX(remKey, top)
                                            },
                                            onAiDraftRemark = {
                                                onDraftRemark(s, t) { drafted ->
                                                    manuallyLockedTerms = manuallyLockedTerms + t
                                                    setX(remKey, drafted)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (askDelete) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            title = { Text("শিক্ষার্থী মুছুন?") },
            text = { Text("${s.name.ifBlank { "এই শিক্ষার্থী" }}-এর সমস্ত তথ্য স্থায়ীভাবে মুছে যাবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        deleted[0] = true
                        onDeleteStudent(s.id)
                        askDelete = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                    modifier = Modifier.testTag("editor_confirm_delete_btn")
                ) { Text("হ্যাঁ, মুছুন") }
            },
            dismissButton = {
                TextButton(onClick = { askDelete = false }) { Text("না") }
            }
        )
    }
}

@Composable
private fun ReadOnlyPermissionBanner(text: String) {
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
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = AmberNotice, modifier = Modifier.size(18.dp))
            Text(text, fontSize = 11.5.sp, color = AmberNotice, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun EditorTextField(
    label: String,
    value: String,
    number: Boolean = false,
    lines: Int = 1,
    enabled: Boolean = true,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (enabled) onChange(it) },
        enabled = enabled,
        label = { Text(label, fontSize = 12.sp) },
        singleLine = lines == 1,
        minLines = lines,
        keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Number else KeyboardType.Text),
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
    )
}

@Composable
private fun MarkField(
    label: String,
    max: Double,
    value: Double?,
    k: String,
    enabled: Boolean = true,
    onValue: (Double?) -> Unit
) {
    var t by remember(k, value) { mutableStateOf(value?.let { fmtEn(it) } ?: "") }
    val v = fromBn(t).toDoubleOrNull()
    val err = t.isNotBlank() && (v == null || v > max || v < 0)
    OutlinedTextField(
        value = t,
        onValueChange = {
            if (!enabled) return@OutlinedTextField
            t = it
            if (it.isBlank()) onValue(null)
            else fromBn(it).toDoubleOrNull()?.let { d -> if (d in 0.0..max) onValue(d) }
        },
        enabled = enabled,
        label = { Text(label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        isError = err,
        singleLine = true,
        supportingText = if (err) ({ Text("সর্বোচ্চ ${fmt(max)}") }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
    )
}
