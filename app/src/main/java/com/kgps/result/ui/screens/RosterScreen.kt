package com.kgps.result.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.CLSN
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.bn
import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.fmt
import com.kgps.result.gradeLabelBn
import com.kgps.result.gradeOf
import com.kgps.result.hasTerm
import com.kgps.result.overallPct
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
import com.kgps.result.ui.theme.SlateSurfaceVariant
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealContainer
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft

@Composable
fun RosterScreen(
    students: List<Student>,
    settings: Settings,
    cls: Int,
    permissions: TeacherPermissions,
    isAdmin: Boolean,
    pendingRequestCount: Int,
    onOpenApprovals: () -> Unit,
    onSelectClass: (Int) -> Unit,
    onOpenStudent: (Student) -> Unit,
    onPreviewStudent: (Student) -> Unit,
    onNewStudentInClass: (Int) -> Unit,
    onOpenSheetInClass: (Int) -> Unit,
    onCsvImport: () -> Unit,
    onBatchEdit: () -> Unit = {},
    onAiScan: () -> Unit,
    onClassPdf: (Int) -> Unit,
    onAllPdf: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var expandedClass by remember(cls) { mutableStateOf<Int?>(cls) }

    val visibleClasses = remember(permissions) { permissions.effectiveClasses() }
    val q = query.trim().lowercase()
    val visibleStudents = remember(students, visibleClasses) {
        students.filter { it.cls in visibleClasses }
    }
    val totalStudents = visibleStudents.size
    val totalEvaluated = remember(visibleStudents) { visibleStudents.count { s -> (1..3).any { s.hasTerm(it) } } }
    val canOpenSheet = permissions.canEditAttendance ||
        permissions.canManageStudents ||
        permissions.allSubjects ||
        permissions.effectiveSubjects().isNotEmpty() ||
        permissions.canViewOtherSubjects
    val isRestricted = !isAdmin && (
        !permissions.allClasses ||
            !permissions.allSubjects ||
            !permissions.canManageStudents ||
            !permissions.canEditAttendance ||
            !permissions.canExportPdf
        )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.widthIn(max = 880.dp).fillMaxSize()) {
            // Admin Pending Join Request Banner
            if (isAdmin && pendingRequestCount > 0) {
                Surface(
                    color = AmberBg,
                    border = BorderStroke(1.dp, AmberBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { onOpenApprovals() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.HourglassTop, contentDescription = null, tint = AmberNotice, modifier = Modifier.size(20.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${bn(pendingRequestCount.toString())} জন নতুন শিক্ষক অনুমোদনের অপেক্ষায়",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = AmberNotice
                            )
                            Text(
                                "অনুমতি নির্ধারণ করে অনুমোদন করতে এখানে ট্যাপ করুন",
                                fontSize = 11.sp,
                                color = SlateText
                            )
                        }
                        Button(
                            onClick = onOpenApprovals,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("দেখুন", fontSize = 11.5.sp)
                        }
                    }
                }
            }

            // Root School Node Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(color = TealContainer, shape = RoundedCornerShape(10.dp)) {
                            Icon(
                                Icons.Rounded.School,
                                contentDescription = "বিদ্যালয়",
                                tint = TealDark,
                                modifier = Modifier.padding(8.dp).size(20.dp)
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                settings.school.ifBlank { "বিদ্যালয়ের নাম নির্ধারিত নয়" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "শিক্ষাবর্ষ ${bn(settings.year)} · ${bn(totalStudents.toString())} জন (মূল্যায়িত ${bn(totalEvaluated.toString())} জন)",
                                fontSize = 11.5.sp,
                                color = SlateMuted
                            )
                        }
                    }

                    if (isRestricted) {
                        Surface(
                            color = TealSoft,
                            border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "আপনার নির্ধারিত অ্যাক্সেস: ${permissions.summaryBn()}",
                                fontSize = 11.sp,
                                color = TealDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    val canScanSheet = permissions.canManageStudents || permissions.allSubjects || permissions.effectiveSubjects().isNotEmpty()
                    if (permissions.canManageStudents || canScanSheet || permissions.canExportPdf) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (permissions.canManageStudents) {
                                AssistChip(
                                    onClick = onCsvImport,
                                    modifier = Modifier.testTag("roster_csv_import_btn"),
                                    leadingIcon = { Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    label = { Text("CSV ইমপোর্ট", fontSize = 11.5.sp) }
                                )
                                AssistChip(
                                    onClick = onBatchEdit,
                                    modifier = Modifier.testTag("roster_batch_update_btn"),
                                    leadingIcon = { Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    label = { Text("ব্যাচ আপডেট", fontSize = 11.5.sp) }
                                )
                            }
                            if (canScanSheet) {
                                AssistChip(
                                    onClick = onAiScan,
                                    modifier = Modifier.testTag("roster_ai_scan_btn"),
                                    leadingIcon = { Icon(Icons.Rounded.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    label = { Text("AI ছবি স্ক্যান", fontSize = 11.5.sp) }
                                )
                            }
                            if (permissions.canExportPdf) {
                                AssistChip(
                                    onClick = onAllPdf,
                                    modifier = Modifier.testTag("roster_all_pdf_btn"),
                                    leadingIcon = { Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    label = { Text("সব শ্রেণির PDF", fontSize = 11.5.sp) }
                                )
                            }
                        }
                    }
                }
            }

            if (visibleClasses.isEmpty()) {
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = AmberNotice, modifier = Modifier.size(32.dp))
                        Text(
                            "অ্যাডমিন এখনো আপনাকে কোনো শ্রেণি নির্ধারণ করেননি",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = SlateText
                        )
                        Text(
                            "বিদ্যালয়ের অ্যাডমিন সেটিংস থেকে আপনার শ্রেণি ও বিষয়ের অনুমতি চালু করলে এখানে শিক্ষার্থীর তালিকা দেখাবে।",
                            fontSize = 12.sp,
                            color = SlateMuted
                        )
                    }
                }
                return@Column
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("নির্ধারিত শ্রেণিতে নাম, রোল, অভিভাবক বা মোবাইল খুঁজুন...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "অনুসন্ধান") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("roster_search_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(8.dp))

            // Expandable School -> Allowed Classes -> Student Tree
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 84.dp)
            ) {
                items(visibleClasses, key = { it }) { c ->
                    val allInClass = remember(students, c) {
                        students.filter { it.cls == c }.sortedBy { it.roll }
                    }
                    val matching = remember(allInClass, q) {
                        if (q.isEmpty()) allInClass
                        else allInClass.filter {
                            it.name.lowercase().contains(q) ||
                                it.roll.toString().contains(q) ||
                                bn(it.roll.toString()).contains(q) ||
                                it.guardian.lowercase().contains(q) ||
                                it.mobile.lowercase().contains(q) ||
                                it.x.values.any { v -> v.lowercase().contains(q) }
                        }
                    }

                    if (q.isNotEmpty() && matching.isEmpty()) return@items

                    val evaluated = allInClass.filter { s -> (1..3).any { s.hasTerm(it) } }
                    val avgPct = if (evaluated.isNotEmpty()) evaluated.sumOf { it.overallPct() } / evaluated.size else 0.0
                    val avgGrade = gradeOf(avgPct)
                    val teacherName = settings.teachers[c].orEmpty().ifBlank { "অনির্ধারিত" }
                    val isExpanded = if (q.isNotEmpty()) matching.isNotEmpty() else (expandedClass == c)

                    val badgeText = when {
                        allInClass.isEmpty() -> "০ জন"
                        evaluated.isNotEmpty() && (permissions.allSubjects || permissions.canViewOtherSubjects) ->
                            "${bn(allInClass.size.toString())} জন · ${fmt(avgPct)}% ($avgGrade)"
                        else -> "${bn(allInClass.size.toString())} জন"
                    }

                    TreeGroupCard(
                        title = "${CLSN[c - 1]} শ্রেণি",
                        subtitle = "শ্রেণি শিক্ষক: $teacherName",
                        badgeText = badgeText,
                        badgeColor = if (evaluated.isNotEmpty() && avgGrade == "A" && (permissions.allSubjects || permissions.canViewOtherSubjects)) EmeraldGrade else TealPrimary,
                        badgeContainerColor = TealSoft,
                        icon = Icons.Rounded.FolderOpen,
                        expanded = isExpanded,
                        headerTestTag = "roster_class_node_$c",
                        onToggle = {
                            onSelectClass(c)
                            expandedClass = if (expandedClass == c) null else c
                        }
                    ) {
                        // Class-level Quick Tree Actions gated by permissions
                        if (permissions.canManageStudents || canOpenSheet || permissions.canExportPdf) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (permissions.canManageStudents) {
                                    OutlinedButton(
                                        onClick = { onNewStudentInClass(c) },
                                        modifier = Modifier.weight(1f).testTag("roster_class_add_$c"),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                        border = BorderStroke(1.dp, SlateOutline)
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Text(" যোগ", fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                                if (canOpenSheet) {
                                    OutlinedButton(
                                        onClick = { onOpenSheetInClass(c) },
                                        modifier = Modifier.weight(1.25f).testTag("roster_class_sheet_$c"),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                        border = BorderStroke(1.dp, SlateOutline)
                                    ) {
                                        Icon(Icons.Rounded.GridOn, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Text(" ব্যাচ এন্ট্রি", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                if (permissions.canExportPdf) {
                                    OutlinedButton(
                                        onClick = { onClassPdf(c) },
                                        modifier = Modifier.weight(0.95f).testTag("roster_class_pdf_$c"),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                        border = BorderStroke(1.dp, SlateOutline)
                                    ) {
                                        Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Text(" PDF", fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                            }
                        }

                        if (matching.isEmpty()) {
                            TreeBranchItem(isLast = true) {
                                Surface(
                                    color = SlateBg,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, SlateOutline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.PersonOutline,
                                                contentDescription = null,
                                                tint = SlateMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                "এই শ্রেণিতে কোনো শিক্ষার্থী নেই",
                                                fontSize = 12.sp,
                                                color = SlateMuted
                                            )
                                        }
                                        if (permissions.canManageStudents) {
                                            TextButton(onClick = { onNewStudentInClass(c) }) {
                                                Text("+ যোগ করুন", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            matching.forEachIndexed { sIdx, s ->
                                val isLast = sIdx == matching.lastIndex
                                val hasEval = (1..3).any { s.hasTerm(it) }
                                val pct = s.overallPct()
                                val grade = gradeOf(pct)
                                val gradeColor = when (grade) {
                                    "A" -> EmeraldGrade
                                    "B" -> TealPrimary
                                    "C" -> AmberNotice
                                    else -> RoseDanger
                                }
                                val showFullResult = permissions.allSubjects || permissions.canViewOtherSubjects

                                TreeBranchItem(isLast = isLast) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("student_card_${s.id}")
                                            .clickable { onOpenStudent(s) },
                                        colors = CardDefaults.cardColors(containerColor = SlateBg),
                                        border = BorderStroke(1.dp, SlateOutline),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Surface(
                                                    color = SlateSurfaceVariant,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        bn(s.roll.toString().padStart(2, '0')),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.5.sp,
                                                        color = SlateText,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                                Column(Modifier.weight(1f)) {
                                                    Text(
                                                        s.name.ifBlank { "(নাম নেই)" },
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 14.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    val subLine = listOfNotNull(
                                                        s.guardian.takeIf { it.isNotBlank() },
                                                        s.religion.takeIf { it.isNotBlank() }
                                                    ).joinToString(" · ")
                                                    if (subLine.isNotBlank()) {
                                                        Text(
                                                            subLine,
                                                            fontSize = 11.sp,
                                                            color = SlateMuted,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                                if (hasEval && showFullResult) {
                                                    Surface(
                                                        color = gradeColor.copy(alpha = 0.12f),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            "$grade · ${fmt(pct)}%",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = gradeColor,
                                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    when {
                                                        hasEval && showFullResult -> "মূল্যায়ন: ${gradeLabelBn(grade)}"
                                                        !showFullResult -> "নির্ধারিত বিষয় দেখতে/লিখতে ট্যাপ করুন"
                                                        else -> "নম্বর দেওয়া হয়নি"
                                                    },
                                                    fontSize = 11.sp,
                                                    color = if (hasEval && showFullResult) gradeColor else SlateMuted
                                                )
                                                if (permissions.canExportPdf) {
                                                    IconButton(
                                                        onClick = { onPreviewStudent(s) },
                                                        modifier = Modifier.size(48.dp).testTag("preview_student_${s.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Visibility,
                                                            contentDescription = "রিপোর্ট প্রিভিউ",
                                                            tint = TealPrimary,
                                                            modifier = Modifier.size(18.dp)
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
                }
            }
        }
    }
}
