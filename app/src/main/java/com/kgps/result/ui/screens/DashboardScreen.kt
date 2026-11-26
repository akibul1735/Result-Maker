package com.kgps.result.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EventSeat
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.TableView
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.ReportSummaryColumn
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.activeClassIds
import com.kgps.result.backup.AutoBackupStatus
import com.kgps.result.bn
import com.kgps.result.classNameBn
import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.fmt
import com.kgps.result.ui.components.BatchReportDialog
import com.kgps.result.ui.theme.AmberBg
import com.kgps.result.ui.theme.AmberBorder
import com.kgps.result.ui.theme.AmberNotice
import com.kgps.result.ui.theme.EmeraldGrade
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
fun DashboardScreen(
    students: List<Student>,
    settings: Settings,
    selectedClass: Int,
    permissions: TeacherPermissions,
    isAdmin: Boolean,
    pendingRequestCount: Int,
    backupStatus: AutoBackupStatus,
    includeClassHighest: Boolean = false,
    onToggleClassHighest: (Boolean) -> Unit = {},
    includeSummaryTable: Boolean = false,
    summaryColumns: List<ReportSummaryColumn> = ReportSummaryColumn.DEFAULT_SELECTED,
    onToggleSummaryTable: (Boolean) -> Unit = {},
    onUpdateSummaryColumns: (List<ReportSummaryColumn>) -> Unit = {},
    onOpenApprovals: () -> Unit,
    onOpenSettings: () -> Unit,
    onCsvImport: () -> Unit,
    onBasicStudentImport: () -> Unit = {},
    onBatchEdit: () -> Unit,
    onAiScan: () -> Unit,
    onAllPdf: () -> Unit,
    onOpenBlankSheetPdf: () -> Unit = {},
    onOpenSeatPlanPdf: () -> Unit = {},
    onOpenTabulationPdf: () -> Unit = {},
    onOpenAdmitCardPdf: () -> Unit = {},
    onPrintBatch: (List<Student>, String) -> Unit,
    onExportBatchPdf: (List<Student>, String) -> Unit,
    onBackupNowAll: () -> Unit,
    onOpenStudent: (Student) -> Unit,
    onPreviewStudent: (Student) -> Unit,
    onOpenSheetInClass: (Int) -> Unit
) {
    val availableClassIds = remember(settings.customClasses) { activeClassIds(settings) }
    val visibleClasses = remember(permissions, availableClassIds) { permissions.effectiveClasses(availableClassIds) }
    var classFilter by remember(visibleClasses) { mutableStateOf<Int?>(null) } // null = সব অনুমোদিত শ্রেণি
    var termFilter by remember { mutableIntStateOf(0) } // 0 = বার্ষিক, 1..3 = নির্দিষ্ট প্রান্তিক
    var showBatchDialog by remember { mutableStateOf(false) }
    var showTieDialog by remember { mutableStateOf(false) }

    val sortedAllowedStudents = remember(students, visibleClasses) {
        val allowed = visibleClasses.toSet()
        students.filter { it.cls in allowed }.sortedWith(compareBy({ it.cls }, { it.roll }))
    }

    val summary = remember(students, visibleClasses, classFilter, termFilter, permissions, settings.resultRules) {
        buildDashboardSummary(
            allStudents = students,
            visibleClasses = visibleClasses,
            classFilter = classFilter,
            termFilter = termFilter,
            permissions = permissions,
            rules = settings.resultRules
        )
    }

    val isRestricted = !isAdmin && (
        !permissions.allClasses ||
            !permissions.allSubjects ||
            !permissions.canManageStudents ||
            !permissions.canEditAttendance ||
            !permissions.canExportPdf
        )
    val canScanSheet = permissions.canManageStudents ||
        permissions.allSubjects ||
        permissions.effectiveSubjects().isNotEmpty()
    val canOpenSheet = permissions.canEditAttendance ||
        permissions.canManageStudents ||
        permissions.allSubjects ||
        permissions.effectiveSubjects().isNotEmpty() ||
        permissions.canViewOtherSubjects
    val showSubjectRestrictionNote = !permissions.allSubjects && !permissions.canViewOtherSubjects

    val termLabel = when (termFilter) {
        1 -> "১ম প্রান্তিক"
        2 -> "২য় প্রান্তিক"
        3 -> "৩য় প্রান্তিক"
        else -> "বার্ষিক গড়"
    }

    val latestBackupMs = maxOf(
        backupStatus.lastLocalBackupMs,
        backupStatus.lastCsvBackupMs,
        backupStatus.lastDriveBackupMs
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 920.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 18.dp)
        ) {
            // 1. Admin Join Requests Banner
            if (isAdmin && pendingRequestCount > 0) {
                item(key = "admin_requests") {
                    Surface(
                        color = AmberBg,
                        border = BorderStroke(1.dp, AmberBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenApprovals() }
                            .testTag("dash_pending_approvals_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.HourglassTop, contentDescription = null, tint = AmberNotice, modifier = Modifier.size(18.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${bn(pendingRequestCount.toString())} জন নতুন শিক্ষক অনুমোদনের অপেক্ষায়",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = AmberNotice
                                )
                                Text(
                                    "অনুমতি নির্ধারণ করে অনুমোদন করতে ট্যাপ করুন",
                                    fontSize = 10.5.sp,
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
            }

            // 2. School Hero Overview Card
            item(key = "hero_summary") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, TealContainer),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(TealSoft, Color.White)
                                )
                            )
                            .padding(11.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(color = TealContainer, shape = RoundedCornerShape(9.dp)) {
                                Icon(
                                    Icons.Rounded.School,
                                    contentDescription = "বিদ্যালয়",
                                    tint = TealDark,
                                    modifier = Modifier.padding(7.dp).size(20.dp)
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    settings.school.ifBlank { "বিদ্যালয়ের নাম নির্ধারিত নয়" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.5.sp,
                                    color = SlateText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    buildString {
                                        append("শিক্ষাবর্ষ ${bn(settings.year)}")
                                        if (settings.schoolAddress.isNotBlank()) append(" · ${settings.schoolAddress}")
                                    },
                                    fontSize = 11.5.sp,
                                    color = SlateMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (isRestricted) {
                            Surface(
                                color = Color.White.copy(alpha = 0.85f),
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HeroMetricBox(
                                value = bn(sortedAllowedStudents.size.toString()),
                                label = "মোট শিক্ষার্থী",
                                accent = TealPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            HeroMetricBox(
                                value = bn(summary.evaluatedInScope.toString()),
                                label = "মূল্যায়িত",
                                accent = EmeraldGrade,
                                modifier = Modifier.weight(1f)
                            )
                            HeroMetricBox(
                                value = bn(visibleClasses.size.toString()),
                                label = "শ্রেণি",
                                accent = Color(0xFF0369A1),
                                modifier = Modifier.weight(1f)
                            )
                            HeroMetricBox(
                                value = summary.avgAttendancePct?.let { "${fmt(it)}%" } ?: "—",
                                label = "গড় উপস্থিতি",
                                accent = AmberNotice,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (visibleClasses.isEmpty()) {
                item(key = "no_class_access") {
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
                                "বিদ্যালয়ের অ্যাডমিন সেটিংস থেকে আপনার শ্রেণি ও বিষয়ের অনুমতি চালু করলে ড্যাশবোর্ডে ফলাফল ও টুলস দেখতে পাবেন।",
                                fontSize = 12.sp,
                                color = SlateMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                return@LazyColumn
            }

            if (summary.tieGroups.isNotEmpty()) {
                item(key = "tie_alert_button") {
                    TieSummaryActionButton(
                        tieGroups = summary.tieGroups,
                        termLabel = termLabel,
                        settings = settings,
                        onClick = { showTieDialog = true }
                    )
                }
            }

            // 3. Quick Tools & Export / Backup Shortcuts Card
            item(key = "quick_tools") {
                val dataEntryTools = buildList {
                    if (permissions.canManageStudents) {
                        add(
                            QuickToolSpec(
                                icon = Icons.Rounded.Badge,
                                title = "বেসিক ইমপোর্ট",
                                subtitle = "পূর্ণ প্রোফাইল",
                                tint = TealDark,
                                testTag = "dash_basic_import_btn",
                                onClick = onBasicStudentImport
                            )
                        )
                        add(
                            QuickToolSpec(
                                icon = Icons.Rounded.UploadFile,
                                title = "CSV ইমপোর্ট",
                                subtitle = "ফাইল / টেক্সট",
                                tint = TealPrimary,
                                testTag = "dash_csv_import_btn",
                                onClick = onCsvImport
                            )
                        )
                        add(
                            QuickToolSpec(
                                icon = Icons.Rounded.EditNote,
                                title = "ব্যাচ আপডেট",
                                subtitle = "একসাথে তথ্য",
                                tint = Color(0xFF0369A1),
                                testTag = "dash_batch_edit_btn",
                                onClick = onBatchEdit
                            )
                        )
                    }
                    if (canScanSheet) {
                        add(
                            QuickToolSpec(
                                icon = Icons.Rounded.DocumentScanner,
                                title = "AI ছবি স্ক্যান",
                                subtitle = "নম্বরপত্র স্ক্যান",
                                tint = EmeraldGrade,
                                testTag = "dash_ai_scan_btn",
                                onClick = onAiScan
                            )
                        )
                    }
                }

                val reportBackupTools = buildList {
                    if (permissions.canExportPdf) {
                        add(
                            QuickToolSpec(
                                icon = Icons.Rounded.Description,
                                title = "সব শ্রেণির PDF",
                                subtitle = "এক ফাইলে সব",
                                tint = TealPrimary,
                                testTag = "dash_all_pdf_btn",
                                onClick = onAllPdf
                            )
                        )
                        add(
                            QuickToolSpec(
                                icon = Icons.Rounded.LibraryBooks,
                                title = "ব্যাচ প্রিন্ট / PDF",
                                subtitle = "শ্রেণি বা কাস্টম",
                                tint = AmberNotice,
                                testTag = "dash_batch_print_btn",
                                onClick = { showBatchDialog = true }
                            )
                        )
                        add(
                            QuickToolSpec(
                                icon = Icons.Rounded.TableChart,
                                title = "ফাঁকা ফর্ম PDF",
                                subtitle = "হাতে লেখার শিট",
                                tint = TealDark,
                                testTag = "dash_blank_sheet_pdf_btn",
                                onClick = onOpenBlankSheetPdf
                            )
                        )
                        add(
                            QuickToolSpec(
                                icon = Icons.Rounded.EventSeat,
                                title = "আসন বিন্যাস",
                                subtitle = "সিট প্ল্যান PDF",
                                tint = TealPrimary,
                                testTag = "dash_seat_plan_pdf_btn",
                                onClick = onOpenSeatPlanPdf
                            )
                        )
                        add(
                            QuickToolSpec(
                                icon = Icons.Rounded.TableView,
                                title = "ফলাফল টেবিল",
                                subtitle = "ট্যাবুলেশন PDF",
                                tint = TealDark,
                                testTag = "dash_tabulation_pdf_btn",
                                onClick = onOpenTabulationPdf
                            )
                        )
                        add(
                            QuickToolSpec(
                                icon = Icons.Rounded.Badge,
                                title = "প্রবেশপত্র",
                                subtitle = "রুটিনসহ Admit",
                                tint = TealPrimary,
                                testTag = "dash_admit_card_pdf_btn",
                                onClick = onOpenAdmitCardPdf
                            )
                        )
                    }
                    add(
                        QuickToolSpec(
                            icon = if (latestBackupMs > 0L) Icons.Rounded.CloudDone else Icons.Rounded.Backup,
                            title = "দ্রুত ব্যাকআপ",
                            subtitle = formatBackupShortTime(latestBackupMs),
                            tint = EmeraldGrade,
                            testTag = "dash_backup_now_btn",
                            onClick = onBackupNowAll
                        )
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val columns = if (maxWidth >= 520.dp) 4 else 2
                        Column(
                            modifier = Modifier.padding(11.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "দ্রুত টুলস, প্রিন্ট ও ব্যাকআপ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = SlateText
                                )
                                Surface(
                                    onClick = onOpenSettings,
                                    color = SlateBg,
                                    border = BorderStroke(1.dp, SlateOutline),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 36.dp)
                                        .testTag("dash_backup_settings_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Rounded.Settings, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(14.dp))
                                        Text("সেটিংস", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = SlateText)
                                    }
                                }
                            }

                            if (dataEntryTools.isNotEmpty()) {
                                QuickToolGroupSection(
                                    header = "তথ্য এন্ট্রি ও স্ক্যান",
                                    items = dataEntryTools,
                                    columns = columns
                                )
                            }

                            if (dataEntryTools.isNotEmpty() && reportBackupTools.isNotEmpty()) {
                                HorizontalDivider(color = SlateOutline.copy(alpha = 0.45f))
                            }

                            if (reportBackupTools.isNotEmpty()) {
                                QuickToolGroupSection(
                                    header = "প্রিন্ট, PDF ও ব্যাকআপ",
                                    items = reportBackupTools,
                                    columns = columns
                                )
                            }
                        }
                    }
                }
            }

            // 4. Filter Strip (Class & Term for Analytics/Progress/Gaps)
            item(key = "analytics_filters") {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = classFilter == null,
                                onClick = { classFilter = null },
                                colors = appFilterChipColors(),
                                border = appFilterChipBorder(classFilter == null),
                                modifier = Modifier.height(32.dp),
                                label = { Text("সব শ্রেণি", fontSize = 11.sp) }
                            )
                            visibleClasses.forEach { c ->
                                val sel = classFilter == c
                                FilterChip(
                                    selected = sel,
                                    onClick = { classFilter = c },
                                    colors = appFilterChipColors(),
                                    border = appFilterChipBorder(sel),
                                    modifier = Modifier.height(32.dp),
                                    label = { Text("${classNameBn(c, settings)} শ্রেণি", fontSize = 11.sp) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                0 to "বার্ষিক",
                                1 to "১ম প্রান্তিক",
                                2 to "২য় প্রান্তিক",
                                3 to "৩য় প্রান্তিক"
                            ).forEach { (t, lbl) ->
                                val sel = termFilter == t
                                FilterChip(
                                    selected = sel,
                                    onClick = { termFilter = t },
                                    colors = appFilterChipColors(),
                                    border = appFilterChipBorder(sel),
                                    modifier = Modifier.height(32.dp),
                                    label = { Text(lbl, fontSize = 10.5.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // 5. Evaluation & Attendance Progress Tracker
            item(key = "progress_tracker") {
                ProgressTrackerCard(
                    summary = summary,
                    showClassBreakdown = classFilter == null,
                    onSelectClassFilter = { c -> classFilter = c }
                )
            }

            // 6. Class-wise Result Summary & Merit List
            item(key = "merit_results") {
                MeritAndResultsCard(
                    summary = summary,
                    termLabel = termLabel,
                    canPreviewReport = permissions.canExportPdf,
                    showSubjectRestrictionNote = showSubjectRestrictionNote,
                    onOpenStudent = onOpenStudent,
                    onPreviewStudent = onPreviewStudent,
                    onOpenTieDetails = { showTieDialog = true }
                )
            }

            // 7. Incomplete Marks / Missing Info Student List
            item(key = "incomplete_gaps") {
                IncompleteStudentsCard(
                    gapItems = summary.gapItems,
                    canOpenSheet = canOpenSheet,
                    onOpenStudent = onOpenStudent,
                    onOpenBatchSheet = onOpenSheetInClass
                )
            }
        }
    }

    if (showTieDialog) {
        TotalMarksTieDetailsDialog(
            tieGroups = summary.tieGroups,
            termLabel = termLabel,
            settings = settings,
            canPreviewReport = permissions.canExportPdf,
            onOpenStudent = onOpenStudent,
            onPreviewStudent = onPreviewStudent,
            onDismiss = { showTieDialog = false }
        )
    }

    if (showBatchDialog) {
        BatchReportDialog(
            sortedStudents = sortedAllowedStudents,
            initialClass = classFilter ?: selectedClass,
            includeClassHighest = includeClassHighest,
            onToggleClassHighest = onToggleClassHighest,
            includeSummaryTable = includeSummaryTable,
            summaryColumns = summaryColumns,
            onToggleSummaryTable = onToggleSummaryTable,
            onUpdateSummaryColumns = onUpdateSummaryColumns,
            onPrintBatch = onPrintBatch,
            onExportBatchPdf = onExportBatchPdf,
            onDismiss = { showBatchDialog = false }
        )
    }
}

@Composable
private fun HeroMetricBox(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        border = BorderStroke(1.dp, SlateOutline),
        shape = RoundedCornerShape(9.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                value,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = accent,
                maxLines = 1
            )
            Text(
                label,
                fontSize = 9.5.sp,
                color = SlateMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class QuickToolSpec(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val tint: Color,
    val testTag: String,
    val onClick: () -> Unit
)

@Composable
private fun QuickToolGroupSection(
    header: String,
    items: List<QuickToolSpec>,
    columns: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = header,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = SlateMuted,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        items.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowItems.forEach { spec ->
                    QuickShortcutTile(
                        icon = spec.icon,
                        title = spec.title,
                        subtitle = spec.subtitle,
                        tint = spec.tint,
                        testTag = spec.testTag,
                        onClick = spec.onClick,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size in 2 until columns) {
                    repeat(columns - rowItems.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickShortcutTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = SlateBg,
        border = BorderStroke(1.dp, SlateOutline.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .heightIn(min = 52.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = tint.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    modifier = Modifier.padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp,
                    color = SlateText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    fontSize = 9.5.sp,
                    color = SlateMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
