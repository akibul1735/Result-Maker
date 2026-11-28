package com.kgps.result.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EventSeat
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.DashboardQuickToolId
import com.kgps.result.DashboardToolUsageStore
import com.kgps.result.ReportSummaryColumn
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.activeClassIds
import com.kgps.result.backup.AutoBackupStatus
import com.kgps.result.bn
import com.kgps.result.classNameBn
import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.fmt
import com.kgps.result.partitionQuickTools
import com.kgps.result.ui.components.BatchReportDialog
import com.kgps.result.ui.theme.AmberBg
import com.kgps.result.ui.theme.AmberBorder
import com.kgps.result.ui.theme.AmberNotice
import com.kgps.result.ui.theme.EmeraldGrade
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
    val context = LocalContext.current
    val availableClassIds = remember(settings.customClasses) { activeClassIds(settings) }
    val visibleClasses = remember(permissions, availableClassIds) { permissions.effectiveClasses(availableClassIds) }
    var classFilter by remember(visibleClasses) { mutableStateOf<Int?>(null) }
    var termFilter by remember { mutableIntStateOf(0) }
    var activeAnalyticsTab by remember { mutableStateOf(DashboardAnalyticsTab.MERIT) }
    var showMoreTools by remember { mutableStateOf(false) }
    var showBatchDialog by remember { mutableStateOf(false) }
    var showTieDialog by remember { mutableStateOf(false) }
    var toolUsageStats by remember(context) { mutableStateOf(DashboardToolUsageStore.loadStats(context)) }

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

    val allToolSpecs = remember(
        permissions,
        canScanSheet,
        latestBackupMs,
        onBasicStudentImport,
        onCsvImport,
        onBatchEdit,
        onAiScan,
        onAllPdf,
        onOpenBlankSheetPdf,
        onOpenSeatPlanPdf,
        onOpenTabulationPdf,
        onOpenAdmitCardPdf,
        onBackupNowAll
    ) {
        buildList {
            if (permissions.canExportPdf) {
                add(
                    QuickToolSpec(
                        id = DashboardQuickToolId.TABULATION_PDF,
                        icon = Icons.Rounded.TableView,
                        shortTitle = "ফলাফল টেবিল",
                        title = "ফলাফল টেবিল",
                        subtitle = "ট্যাবুলেশন PDF",
                        tint = TealDark,
                        testTag = "dash_tabulation_pdf_btn",
                        onClick = onOpenTabulationPdf
                    )
                )
                add(
                    QuickToolSpec(
                        id = DashboardQuickToolId.ADMIT_CARD_PDF,
                        icon = Icons.Rounded.Badge,
                        shortTitle = "প্রবেশপত্র",
                        title = "প্রবেশপত্র",
                        subtitle = "রুটিনসহ Admit",
                        tint = TealPrimary,
                        testTag = "dash_admit_card_pdf_btn",
                        onClick = onOpenAdmitCardPdf
                    )
                )
                add(
                    QuickToolSpec(
                        id = DashboardQuickToolId.BLANK_SHEET_PDF,
                        icon = Icons.Rounded.TableChart,
                        shortTitle = "ফাঁকা ফর্ম",
                        title = "ফাঁকা ফর্ম PDF",
                        subtitle = "হাতে লেখার শিট",
                        tint = TealDark,
                        testTag = "dash_blank_sheet_pdf_btn",
                        onClick = onOpenBlankSheetPdf
                    )
                )
            }
            if (canScanSheet) {
                add(
                    QuickToolSpec(
                        id = DashboardQuickToolId.AI_SCAN,
                        icon = Icons.Rounded.DocumentScanner,
                        shortTitle = "AI স্ক্যান",
                        title = "AI ছবি স্ক্যান",
                        subtitle = "নম্বরপত্র স্ক্যান",
                        tint = EmeraldGrade,
                        testTag = "dash_ai_scan_btn",
                        onClick = onAiScan
                    )
                )
            }
            if (permissions.canExportPdf) {
                add(
                    QuickToolSpec(
                        id = DashboardQuickToolId.ALL_PDF,
                        icon = Icons.Rounded.Description,
                        shortTitle = "সব PDF",
                        title = "সব শ্রেণির PDF",
                        subtitle = "এক ফাইলে সব",
                        tint = TealPrimary,
                        testTag = "dash_all_pdf_btn",
                        onClick = onAllPdf
                    )
                )
                add(
                    QuickToolSpec(
                        id = DashboardQuickToolId.BATCH_PRINT,
                        icon = Icons.Rounded.LibraryBooks,
                        shortTitle = "ব্যাচ প্রিন্ট",
                        title = "ব্যাচ প্রিন্ট / PDF",
                        subtitle = "শ্রেণি বা কাস্টম",
                        tint = AmberNotice,
                        testTag = "dash_batch_print_btn",
                        onClick = { showBatchDialog = true }
                    )
                )
                add(
                    QuickToolSpec(
                        id = DashboardQuickToolId.SEAT_PLAN_PDF,
                        icon = Icons.Rounded.EventSeat,
                        shortTitle = "আসন বিন্যাস",
                        title = "আসন বিন্যাস",
                        subtitle = "সিট প্ল্যান PDF",
                        tint = TealPrimary,
                        testTag = "dash_seat_plan_pdf_btn",
                        onClick = onOpenSeatPlanPdf
                    )
                )
            }
            if (permissions.canManageStudents) {
                add(
                    QuickToolSpec(
                        id = DashboardQuickToolId.BATCH_EDIT,
                        icon = Icons.Rounded.EditNote,
                        shortTitle = "ব্যাচ আপডেট",
                        title = "ব্যাচ আপডেট",
                        subtitle = "একসাথে তথ্য",
                        tint = Color(0xFF0369A1),
                        testTag = "dash_batch_edit_btn",
                        onClick = onBatchEdit
                    )
                )
                add(
                    QuickToolSpec(
                        id = DashboardQuickToolId.CSV_IMPORT,
                        icon = Icons.Rounded.UploadFile,
                        shortTitle = "CSV ইমপোর্ট",
                        title = "CSV ইমপোর্ট",
                        subtitle = "ফাইল / টেক্সট",
                        tint = TealPrimary,
                        testTag = "dash_csv_import_btn",
                        onClick = onCsvImport
                    )
                )
                add(
                    QuickToolSpec(
                        id = DashboardQuickToolId.BASIC_IMPORT,
                        icon = Icons.Rounded.Badge,
                        shortTitle = "বেসিক ইমপোর্ট",
                        title = "বেসিক ইমপোর্ট",
                        subtitle = "পূর্ণ প্রোফাইল",
                        tint = TealDark,
                        testTag = "dash_basic_import_btn",
                        onClick = onBasicStudentImport
                    )
                )
            }
            add(
                QuickToolSpec(
                    id = DashboardQuickToolId.BACKUP_NOW,
                    icon = if (latestBackupMs > 0L) Icons.Rounded.CloudDone else Icons.Rounded.Backup,
                    shortTitle = "ব্যাকআপ",
                    title = "দ্রুত ব্যাকআপ",
                    subtitle = formatBackupShortTime(latestBackupMs),
                    tint = EmeraldGrade,
                    testTag = "dash_backup_now_btn",
                    onClick = onBackupNowAll
                )
            )
        }
    }

    val (primaryTools, moreTools) = remember(allToolSpecs, toolUsageStats) {
        val specMap = allToolSpecs.associateBy { it.id }
        val (topIds, restIds) = partitionQuickTools(
            availableIds = allToolSpecs.map { it.id },
            stats = toolUsageStats,
            primaryLimit = 4
        )
        topIds.mapNotNull { specMap[it] } to restIds.mapNotNull { specMap[it] }
    }

    val handleToolClick: (QuickToolSpec) -> Unit = { spec ->
        toolUsageStats = DashboardToolUsageStore.recordUsage(context, spec.id)
        spec.onClick()
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val isWideLayout = maxWidth >= 760.dp

        if (isWideLayout && visibleClasses.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .widthIn(max = 1080.dp)
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.45f)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isAdmin && pendingRequestCount > 0) {
                        PendingApprovalsBanner(pendingRequestCount, onOpenApprovals)
                    }
                    DashboardHeroCard(
                        settings = settings,
                        totalStudents = sortedAllowedStudents.size,
                        evaluatedCount = summary.evaluatedInScope,
                        classCount = visibleClasses.size,
                        avgAttendancePct = summary.avgAttendancePct,
                        latestBackupMs = latestBackupMs,
                        isRestricted = isRestricted,
                        permissionsSummary = permissions.summaryBn(),
                        onBackupNow = onBackupNowAll,
                        onOpenSettings = onOpenSettings
                    )
                    ScopeFilterCard(
                        settings = settings,
                        visibleClasses = visibleClasses,
                        classFilter = classFilter,
                        termFilter = termFilter,
                        onSelectClass = { classFilter = it },
                        onSelectTerm = { termFilter = it }
                    )
                    QuickActionsCard(
                        primaryTools = primaryTools,
                        moreTools = moreTools,
                        showMoreTools = showMoreTools,
                        onToggleMore = { showMoreTools = !showMoreTools },
                        onToolClick = handleToolClick
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(0.55f)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 18.dp)
                ) {
                    DashboardAnalyticsCard(
                        summary = summary,
                        termLabel = termLabel,
                        showClassBreakdown = classFilter == null,
                        canPreviewReport = permissions.canExportPdf,
                        canOpenSheet = canOpenSheet,
                        showSubjectRestrictionNote = showSubjectRestrictionNote,
                        activeTab = activeAnalyticsTab,
                        onSelectTab = { activeAnalyticsTab = it },
                        onSelectClassFilter = { classFilter = it },
                        onOpenStudent = onOpenStudent,
                        onPreviewStudent = onPreviewStudent,
                        onOpenBatchSheet = onOpenSheetInClass,
                        onOpenTieDetails = { showTieDialog = true }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 20.dp)
            ) {
                if (isAdmin && pendingRequestCount > 0) {
                    item(key = "admin_requests") {
                        PendingApprovalsBanner(pendingRequestCount, onOpenApprovals)
                    }
                }

                item(key = "hero_summary") {
                    DashboardHeroCard(
                        settings = settings,
                        totalStudents = sortedAllowedStudents.size,
                        evaluatedCount = summary.evaluatedInScope,
                        classCount = visibleClasses.size,
                        avgAttendancePct = summary.avgAttendancePct,
                        latestBackupMs = latestBackupMs,
                        isRestricted = isRestricted,
                        permissionsSummary = permissions.summaryBn(),
                        onBackupNow = onBackupNowAll,
                        onOpenSettings = onOpenSettings
                    )
                }

                if (visibleClasses.isEmpty()) {
                    item(key = "no_class_access") {
                        NoClassAccessCard()
                    }
                    return@LazyColumn
                }

                item(key = "analytics_filters") {
                    ScopeFilterCard(
                        settings = settings,
                        visibleClasses = visibleClasses,
                        classFilter = classFilter,
                        termFilter = termFilter,
                        onSelectClass = { classFilter = it },
                        onSelectTerm = { termFilter = it }
                    )
                }

                item(key = "quick_tools") {
                    QuickActionsCard(
                        primaryTools = primaryTools,
                        moreTools = moreTools,
                        showMoreTools = showMoreTools,
                        onToggleMore = { showMoreTools = !showMoreTools },
                        onToolClick = handleToolClick
                    )
                }

                item(key = "analytics_tabs") {
                    DashboardAnalyticsCard(
                        summary = summary,
                        termLabel = termLabel,
                        showClassBreakdown = classFilter == null,
                        canPreviewReport = permissions.canExportPdf,
                        canOpenSheet = canOpenSheet,
                        showSubjectRestrictionNote = showSubjectRestrictionNote,
                        activeTab = activeAnalyticsTab,
                        onSelectTab = { activeAnalyticsTab = it },
                        onSelectClassFilter = { classFilter = it },
                        onOpenStudent = onOpenStudent,
                        onPreviewStudent = onPreviewStudent,
                        onOpenBatchSheet = onOpenSheetInClass,
                        onOpenTieDetails = { showTieDialog = true }
                    )
                }
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
private fun PendingApprovalsBanner(
    pendingRequestCount: Int,
    onOpenApprovals: () -> Unit
) {
    Surface(
        color = AmberBg,
        border = BorderStroke(1.dp, AmberBorder),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenApprovals() }
            .testTag("dash_pending_approvals_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
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
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(9.dp)
            ) {
                Text("দেখুন", fontSize = 11.5.sp)
            }
        }
    }
}

@Composable
private fun DashboardHeroCard(
    settings: Settings,
    totalStudents: Int,
    evaluatedCount: Int,
    classCount: Int,
    avgAttendancePct: Double?,
    latestBackupMs: Long,
    isRestricted: Boolean,
    permissionsSummary: String,
    onBackupNow: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = TealDark,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(colors = listOf(TealDark, TealPrimary)))
                .drawBehind {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        radius = size.height * 0.65f,
                        center = Offset(size.width * 0.92f, size.height * 0.12f)
                    )
                    drawCircle(
                        color = TealContainer.copy(alpha = 0.07f),
                        radius = size.height * 0.42f,
                        center = Offset(size.width * 0.08f, size.height * 0.95f)
                    )
                }
                .padding(horizontal = 13.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Rounded.School,
                        contentDescription = "বিদ্যালয়",
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp).size(20.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = settings.school.ifBlank { "বিদ্যালয়ের নাম নির্ধারিত নয়" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append("শিক্ষাবর্ষ ${bn(settings.year)}")
                            if (settings.schoolAddress.isNotBlank()) append(" · ${settings.schoolAddress}")
                        },
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    onClick = onBackupNow,
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("dash_hero_backup_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (latestBackupMs > 0L) Icons.Rounded.CloudDone else Icons.Rounded.Backup,
                            contentDescription = "দ্রুত ব্যাকআপ",
                            tint = TealContainer,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (latestBackupMs > 0L) formatBackupShortTime(latestBackupMs) else "ব্যাকআপ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("dash_backup_settings_btn")
                ) {
                    Icon(
                        Icons.Rounded.Settings,
                        contentDescription = "সেটিংস",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            if (isRestricted) {
                Surface(
                    color = Color.White.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "অ্যাক্সেস: $permissionsSummary",
                        fontSize = 10.5.sp,
                        color = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Surface(
                color = Color.White.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeroKpiCell(bn(totalStudents.toString()), "মোট শিক্ষার্থী", Modifier.weight(1f))
                    HeroKpiDivider()
                    HeroKpiCell(bn(evaluatedCount.toString()), "মূল্যায়িত", Modifier.weight(1f))
                    HeroKpiDivider()
                    HeroKpiCell(bn(classCount.toString()), "শ্রেণি", Modifier.weight(1f))
                    HeroKpiDivider()
                    HeroKpiCell(avgAttendancePct?.let { "${fmt(it)}%" } ?: "—", "গড় উপস্থিতি", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HeroKpiCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color.White,
            maxLines = 1
        )
        Text(
            text = label,
            fontSize = 9.5.sp,
            color = TealContainer.copy(alpha = 0.92f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HeroKpiDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(Color.White.copy(alpha = 0.2f))
    )
}

@Composable
private fun ScopeFilterCard(
    settings: Settings,
    visibleClasses: List<Int>,
    classFilter: Int?,
    termFilter: Int,
    onSelectClass: (Int?) -> Unit,
    onSelectTerm: (Int) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SlateOutline.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScopeClassPill(
                    label = "সব শ্রেণি",
                    selected = classFilter == null,
                    onClick = { onSelectClass(null) }
                )
                visibleClasses.forEach { c ->
                    ScopeClassPill(
                        label = "${classNameBn(c, settings)} শ্রেণি",
                        selected = classFilter == c,
                        onClick = { onSelectClass(c) }
                    )
                }
            }

            Surface(
                color = SlateSurfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    listOf(
                        0 to "বার্ষিক",
                        1 to "১ম প্রান্তিক",
                        2 to "২য় প্রান্তিক",
                        3 to "৩য় প্রান্তিক"
                    ).forEach { (t, lbl) ->
                        val sel = termFilter == t
                        Surface(
                            onClick = { onSelectTerm(t) },
                            color = if (sel) Color.White else Color.Transparent,
                            shadowElevation = if (sel) 1.dp else 0.dp,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 5.dp, horizontal = 3.dp)) {
                                Text(
                                    text = lbl,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (sel) TealDark else SlateMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScopeClassPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) TealPrimary else SlateBg,
        border = BorderStroke(1.dp, if (selected) TealPrimary else SlateOutline),
        shape = RoundedCornerShape(50),
        modifier = Modifier.defaultMinSize(minHeight = 30.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) Color.White else SlateText,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
        )
    }
}

private data class QuickToolSpec(
    val id: DashboardQuickToolId,
    val icon: ImageVector,
    val shortTitle: String,
    val title: String,
    val subtitle: String,
    val tint: Color,
    val testTag: String,
    val onClick: () -> Unit
)

@Composable
private fun QuickActionsCard(
    primaryTools: List<QuickToolSpec>,
    moreTools: List<QuickToolSpec>,
    showMoreTools: Boolean,
    onToggleMore: () -> Unit,
    onToolClick: (QuickToolSpec) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SlateOutline.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "কুইক অ্যাকশন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = SlateText
                    )
                    Surface(
                        color = TealSoft,
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "বেশি ব্যবহৃত",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TealDark
                            )
                        }
                    }
                }

                if (moreTools.isNotEmpty()) {
                    Surface(
                        onClick = onToggleMore,
                        color = if (showMoreTools) TealSoft else SlateBg,
                        border = BorderStroke(1.dp, if (showMoreTools) TealContainer else SlateOutline),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 32.dp)
                            .testTag("dash_more_tools_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = if (showMoreTools) "কম দেখুন" else "আরও (${bn(moreTools.size.toString())})",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (showMoreTools) TealDark else SlateText
                            )
                            Icon(
                                if (showMoreTools) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = if (showMoreTools) TealDark else SlateMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                primaryTools.forEach { spec ->
                    PrimaryToolGridItem(
                        spec = spec,
                        onClick = { onToolClick(spec) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (primaryTools.size in 1 until 4) {
                    repeat(4 - primaryTools.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            AnimatedVisibility(
                visible = showMoreTools && moreTools.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = SlateOutline.copy(alpha = 0.6f))
                    moreTools.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            rowItems.forEach { spec ->
                                SecondaryToolTile(
                                    spec = spec,
                                    onClick = { onToolClick(spec) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryToolGridItem(
    spec: QuickToolSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = SlateBg,
        border = BorderStroke(1.dp, SlateOutline.copy(alpha = 0.65f)),
        shape = RoundedCornerShape(13.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag(spec.testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = spec.tint.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Box(
                    modifier = Modifier.padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        spec.icon,
                        contentDescription = spec.title,
                        tint = spec.tint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = spec.shortTitle,
                fontSize = 10.5.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = SlateText,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SecondaryToolTile(
    spec: QuickToolSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = SlateBg,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .testTag(spec.testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = spec.tint.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    spec.icon,
                    contentDescription = null,
                    tint = spec.tint,
                    modifier = Modifier.padding(6.dp).size(16.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = spec.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = SlateText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = spec.subtitle,
                    fontSize = 9.5.sp,
                    color = SlateMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NoClassAccessCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SlateOutline),
        shape = RoundedCornerShape(16.dp)
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
                color = SlateText,
                textAlign = TextAlign.Center
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
