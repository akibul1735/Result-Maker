@file:OptIn(ExperimentalMaterial3Api::class)

package com.kgps.result

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.kgps.result.backup.AutoBackupManager
import com.kgps.result.cloud.CloudSessionState
import com.kgps.result.cloud.SchoolCloudRepository
import com.kgps.result.cloud.SchoolWorkspace
import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.cloud.attemptAutoSignIn
import com.kgps.result.cloud.authStateFlow
import com.kgps.result.cloud.onGoogleSignInClicked
import com.kgps.result.cloud.signOutFromGoogle
import com.kgps.result.ui.components.SummaryTableConfigCard
import com.kgps.result.ui.screens.AiAssistantScreen
import com.kgps.result.ui.screens.AuthScreen
import com.kgps.result.ui.screens.BatchStudentEditDialog
import com.kgps.result.ui.screens.BulkSheetScreen
import com.kgps.result.ui.screens.DashboardScreen
import com.kgps.result.ui.screens.ReportStudioScreen
import com.kgps.result.ui.screens.RosterScreen
import com.kgps.result.ui.screens.SchoolHubScreen
import com.kgps.result.ui.screens.SettingsScreen
import com.kgps.result.ui.screens.SmartCsvImportDialog
import com.kgps.result.ui.screens.StudentEditorScreen
import com.kgps.result.ui.theme.AmberNotice
import com.kgps.result.ui.theme.EmeraldGrade
import com.kgps.result.ui.theme.ResultMakerTheme
import com.kgps.result.ui.theme.RoseDanger
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            )
        )
        setContent {
            ResultMakerTheme {
                AppRoot()
            }
        }
    }
}

private data class NavItem(
    val section: NavSection,
    val shortLabel: String,
    val icon: ImageVector
)

private val PRIMARY_NAV_ITEMS = listOf(
    NavItem(NavSection.DASHBOARD, "ড্যাশবোর্ড", Icons.Rounded.Dashboard),
    NavItem(NavSection.ROSTER, "শিক্ষার্থী", Icons.Rounded.People),
    NavItem(NavSection.SHEET, "ব্যাচ এন্ট্রি", Icons.Rounded.GridOn),
    NavItem(NavSection.STUDIO, "রিপোর্ট", Icons.Rounded.Description),
    NavItem(NavSection.ASSISTANT, "AI সহায়ক", Icons.Rounded.AutoAwesome)
)

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(
        initialValue = Firebase.auth.currentUser
    )
    var checkingAutoSignIn by remember { mutableStateOf(Firebase.auth.currentUser == null) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    checkingAutoSignIn = false
                    authError = null
                },
                onUnauthenticated = {
                    checkingAutoSignIn = false
                },
                scope = scope
            )
        } else {
            checkingAutoSignIn = false
        }
    }

    val user = currentUser
    if (checkingAutoSignIn && user == null) {
        Surface(Modifier.fillMaxSize(), color = SlateBg) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = TealPrimary)
                    Text("অ্যাকাউন্ট যাচাই করা হচ্ছে...", color = SlateMuted, fontSize = 13.sp)
                }
            }
        }
        return
    }

    if (user == null) {
        AuthScreen(
            isSigningIn = isSigningIn,
            errorMessage = authError,
            onGoogleSignIn = {
                isSigningIn = true
                authError = null
                onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = {
                        isSigningIn = false
                        authError = null
                    },
                    onAuthError = { msg ->
                        isSigningIn = false
                        authError = msg
                    },
                    onAuthCancelled = {
                        isSigningIn = false
                    },
                    scope = scope
                )
            }
        )
        return
    }

    key(user.uid) {
        val appContext = context.applicationContext
        val vm: ResultMakerViewModel = viewModel(
            key = user.uid,
            factory = viewModelFactory {
                initializer {
                    val dbId = appContext.getString(R.string.firestore_database_id).trim()
                    val db = FirebaseFirestore.getInstance(dbId)
                    val cloudRepo = SchoolCloudRepository(db)
                    val autoBackupManager = AutoBackupManager(appContext, user.uid)
                    val localSeed = Repo.load(appContext)
                    ResultMakerViewModel(
                        appContext = appContext,
                        cloudRepo = cloudRepo,
                        autoBackupManager = autoBackupManager,
                        contentResolver = appContext.contentResolver,
                        currentUserId = user.uid,
                        currentUserDisplayName = user.displayName.orEmpty(),
                        currentUserEmail = user.email.orEmpty(),
                        initialLocalSnapshot = localSeed
                    )
                }
            }
        )

        val snackbarMessage by vm.snackbarMessage.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(snackbarMessage) {
            val msg = snackbarMessage
            if (!msg.isNullOrBlank()) {
                snackbarHostState.showSnackbar(msg)
                vm.clearMessage()
            }
        }

        val sessionState by vm.sessionState.collectAsStateWithLifecycle()
        val inSchoolHub by vm.inSchoolHub.collectAsStateWithLifecycle()
        val schools by vm.mySchools.collectAsStateWithLifecycle()
        val activeSchool by vm.activeSchool.collectAsStateWithLifecycle()
        val hubBusy by vm.hubBusy.collectAsStateWithLifecycle()
        val lookupBusy by vm.lookupBusy.collectAsStateWithLifecycle()
        val codePreview by vm.codePreview.collectAsStateWithLifecycle()
        val lookupError by vm.lookupError.collectAsStateWithLifecycle()
        val pendingJoinSchoolName by vm.pendingJoinSchoolName.collectAsStateWithLifecycle()

        val pendingDriveConsent by vm.pendingDriveConsent.collectAsStateWithLifecycle()
        val driveConsentLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            vm.handleDriveConsentIntentResult(result.data)
        }
        LaunchedEffect(pendingDriveConsent) {
            val sender = pendingDriveConsent ?: return@LaunchedEffect
            driveConsentLauncher.launch(IntentSenderRequest.Builder(sender).build())
        }

        val performSignOut: () -> Unit = {
            signOutFromGoogle(
                credentialManager = credentialManager,
                scope = scope,
                onBeforeSignOut = { vm.prepareForSignOut() }
            )
        }

        Box(Modifier.fillMaxSize()) {
            when (val state = sessionState) {
                is CloudSessionState.Loading -> {
                    Surface(Modifier.fillMaxSize(), color = SlateBg) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = TealPrimary)
                                Text("বিদ্যালয়ের তথ্য লোড হচ্ছে...", color = SlateMuted, fontSize = 13.sp)
                            }
                        }
                    }
                }

                is CloudSessionState.Error -> {
                    SchoolHubScreen(
                        userDisplayName = vm.currentUserDisplayName,
                        userEmail = vm.currentUserEmail,
                        currentUserId = vm.currentUserId,
                        schools = schools,
                        activeSchoolId = activeSchool?.id.orEmpty(),
                        pendingJoinSchoolName = pendingJoinSchoolName,
                        localSeedCount = vm.localSeedCount,
                        localSeedSettings = vm.localSeedSettings,
                        isBusy = hubBusy,
                        lookupBusy = lookupBusy,
                        codePreview = codePreview,
                        lookupError = lookupError,
                        cloudError = state.message,
                        onRetryCloud = vm::retryCloudConnection,
                        onSelectSchool = vm::selectSchool,
                        onCreateSchool = vm::createSchool,
                        onLookupCode = vm::lookupSchoolCode,
                        onClearCodePreview = vm::clearCodePreview,
                        onJoinWithPreview = vm::joinWithCodePreview,
                        onCancelPendingRequest = vm::cancelPendingJoinRequest,
                        onBackToApp = null,
                        onSignOut = performSignOut
                    )
                }

                is CloudSessionState.Ready -> {
                    val currentActive = activeSchool
                    if (inSchoolHub || currentActive == null) {
                        SchoolHubScreen(
                            userDisplayName = vm.currentUserDisplayName,
                            userEmail = vm.currentUserEmail,
                            currentUserId = vm.currentUserId,
                            schools = schools,
                            activeSchoolId = currentActive?.id.orEmpty(),
                            pendingJoinSchoolName = pendingJoinSchoolName,
                            localSeedCount = vm.localSeedCount,
                            localSeedSettings = vm.localSeedSettings,
                            isBusy = hubBusy,
                            lookupBusy = lookupBusy,
                            codePreview = codePreview,
                            lookupError = lookupError,
                            onSelectSchool = vm::selectSchool,
                            onCreateSchool = vm::createSchool,
                            onLookupCode = vm::lookupSchoolCode,
                            onClearCodePreview = vm::clearCodePreview,
                            onJoinWithPreview = vm::joinWithCodePreview,
                            onCancelPendingRequest = vm::cancelPendingJoinRequest,
                            onBackToApp = if (currentActive != null) ({ vm.closeSchoolHub() }) else null,
                            onSignOut = performSignOut
                        )
                    } else {
                        ResultMakerWorkspace(
                            vm = vm,
                            activeSchool = currentActive,
                            snackbarHostState = snackbarHostState,
                            onOpenSchoolHub = vm::openSchoolHub,
                            onSignOut = performSignOut
                        )
                    }
                }
            }

            // Global snackbar overlay for Auth/SchoolHub when workspace Scaffold isn't mounted
            if (inSchoolHub || activeSchool == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(bottom = 12.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    ResultMakerSnackbarHost(snackbarHostState)
                }
            }
        }
    }
}

@Composable
private fun ResultMakerSnackbarHost(snackbarHostState: SnackbarHostState) {
    SnackbarHost(hostState = snackbarHostState) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = TealSoft,
            contentColor = SlateText,
            actionColor = TealPrimary,
            dismissActionContentColor = SlateMuted,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .border(
                    width = 1.dp,
                    color = TealPrimary.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp)
                )
        )
    }
}

@Composable
fun ResultMakerWorkspace(
    vm: ResultMakerViewModel,
    activeSchool: SchoolWorkspace,
    snackbarHostState: SnackbarHostState,
    onOpenSchoolHub: () -> Unit,
    onSignOut: () -> Unit
) {
    val ctx = LocalContext.current
    val students by vm.students.collectAsStateWithLifecycle()
    val syncMeta by vm.studentSyncMeta.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val joinRequests by vm.joinRequests.collectAsStateWithLifecycle()
    val permissions by vm.activePermissions.collectAsStateWithLifecycle()
    val backupStatus by vm.backupStatus.collectAsStateWithLifecycle()
    val driveFiles by vm.driveFiles.collectAsStateWithLifecycle()
    val driveFilesLoading by vm.driveFilesLoading.collectAsStateWithLifecycle()
    val cls by vm.selectedClass.collectAsStateWithLifecycle()
    val section by vm.currentSection.collectAsStateWithLifecycle()
    val editingStudent by vm.editingStudent.collectAsStateWithLifecycle()
    val studioStudentId by vm.studioStudentId.collectAsStateWithLifecycle()
    val csvImportState by vm.csvImportState.collectAsStateWithLifecycle()
    val batchEditOpen by vm.batchEditOpen.collectAsStateWithLifecycle()
    val batchEditInitialClass by vm.batchEditInitialClass.collectAsStateWithLifecycle()
    val aiState by vm.aiState.collectAsStateWithLifecycle()
    val draftingTerm by vm.draftingRemarkTerm.collectAsStateWithLifecycle()
    val geminiApiKey by vm.geminiApiKey.collectAsStateWithLifecycle()
    val geminiModel by vm.geminiModel.collectAsStateWithLifecycle()
    val geminiAvailableModels by vm.geminiAvailableModels.collectAsStateWithLifecycle()
    val geminiCustomModels by vm.geminiCustomModels.collectAsStateWithLifecycle()
    val geminiNewModelIds by vm.geminiNewModelIds.collectAsStateWithLifecycle()
    val geminiRefreshingModels by vm.geminiRefreshingModels.collectAsStateWithLifecycle()
    val geminiModelsError by vm.geminiModelsError.collectAsStateWithLifecycle()
    val geminiTesting by vm.geminiTesting.collectAsStateWithLifecycle()
    val geminiTestStatus by vm.geminiTestStatus.collectAsStateWithLifecycle()
    val geminiTestSuccess by vm.geminiTestSuccess.collectAsStateWithLifecycle()
    val isGeminiConfigured by vm.isGeminiConfigured.collectAsStateWithLifecycle()
    val includeClassHighest by vm.includeClassHighestInReport.collectAsStateWithLifecycle()
    val includeSummaryTable by vm.includeSummaryTableInReport.collectAsStateWithLifecycle()
    val summaryColumns by vm.summaryTableColumns.collectAsStateWithLifecycle()

    val workspaceScope = rememberCoroutineScope()
    var askSignOut by remember { mutableStateOf(false) }

    var pendingPdfStudents by remember { mutableStateOf<List<Student>>(emptyList()) }
    var pendingPdfOptionPrompt by remember { mutableStateOf<Pair<List<Student>, String>?>(null) }
    var pdfExportingCount by remember { mutableStateOf<Int?>(null) }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val listToWrite = pendingPdfStudents
        if (uri != null && listToWrite.isNotEmpty()) {
            pdfExportingCount = listToWrite.size
            workspaceScope.launch {
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        ctx.contentResolver.openOutputStream(uri)?.use { out ->
                            PdfGen.write(
                                ctx = ctx,
                                students = listToWrite,
                                st = settings,
                                out = out,
                                allStudents = students,
                                showClassHighest = includeClassHighest,
                                showSummaryTable = includeSummaryTable,
                                summaryColumns = summaryColumns
                            )
                        }
                    }
                }
                pdfExportingCount = null
                result.onSuccess {
                    vm.showMessage("PDF সংরক্ষিত হয়েছে (${bn(listToWrite.size.toString())} জন)")
                }.onFailure { e ->
                    vm.showMessage("PDF তৈরি করা যায়নি: ${e.message ?: "ত্রুটি"}")
                }
            }
        }
    }

    val makePdf: (List<Student>, String) -> Unit = { list, fileName ->
        if (!permissions.canExportPdf) {
            vm.showMessage("PDF তৈরি করার অনুমতি আপনার নেই")
        } else if (list.isEmpty()) {
            vm.showMessage("PDF তৈরির জন্য কোনো শিক্ষার্থী নেই")
        } else {
            pendingPdfStudents = list
            pdfLauncher.launch(fileName)
        }
    }

    val requestPdfWithOption: (List<Student>, String) -> Unit = { list, fileName ->
        if (!permissions.canExportPdf) {
            vm.showMessage("PDF তৈরি করার অনুমতি আপনার নেই")
        } else if (list.isEmpty()) {
            vm.showMessage("PDF তৈরির জন্য কোনো শিক্ষার্থী নেই")
        } else {
            pendingPdfOptionPrompt = list to fileName
        }
    }

    val isAdmin = activeSchool.isAdmin(vm.currentUserId)
    val isEditing = editingStudent != null
    val isSettings = section == NavSection.SETTINGS
    val showPrimaryChrome = !isEditing && !isSettings

    BackHandler(enabled = !isEditing && (section == NavSection.ROSTER || section == NavSection.SHEET || section == NavSection.ASSISTANT)) {
        vm.navigate(NavSection.DASHBOARD)
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = SlateBg,
            snackbarHost = { ResultMakerSnackbarHost(snackbarHostState) },
            topBar = {
                if (!isEditing) {
                    ResultMakerTopBar(
                        settings = settings,
                        activeSchool = activeSchool,
                        currentUserId = vm.currentUserId,
                        permissions = permissions,
                        pendingRequestCount = joinRequests.size,
                        isSettings = isSettings,
                        onBackFromSettings = { vm.navigate(NavSection.DASHBOARD) },
                        onOpenSettings = { vm.navigate(NavSection.SETTINGS) },
                        onCsvImport = { vm.openCsvImport("") },
                        onAiScan = vm::openAiScanner,
                        onAllPdf = {
                            val allowedCls = permissions.effectiveClasses().toSet()
                            requestPdfWithOption(
                                students.filter { it.cls in allowedCls }.sortedWith(compareBy({ it.cls }, { it.roll })),
                                "সব-শ্রেণির-প্রতিবেদন.pdf"
                            )
                        },
                        onOpenSchoolHub = onOpenSchoolHub,
                        onRequestSignOut = { askSignOut = true }
                    )
                }
            },
            floatingActionButton = {
                if (!isEditing && section == NavSection.ROSTER && permissions.canManageStudents && permissions.effectiveClasses().isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = { vm.openNewStudentEditor(NavSection.ROSTER) },
                        containerColor = TealPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        text = { Text("নতুন শিক্ষার্থী", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("roster_fab_add_student")
                    )
                }
            },
            bottomBar = {
                if (showPrimaryChrome && !useRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        PRIMARY_NAV_ITEMS.forEach { item ->
                            val selected = section == item.section
                            NavigationBarItem(
                                selected = selected,
                                onClick = { vm.navigate(item.section) },
                                icon = { Icon(item.icon, contentDescription = item.section.labelBn) },
                                label = {
                                    Text(
                                        item.shortLabel,
                                        fontSize = 10.5.sp,
                                        softWrap = false,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag("nav_bar_${item.section.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showPrimaryChrome && useRail) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        PRIMARY_NAV_ITEMS.forEach { item ->
                            NavigationRailItem(
                                selected = section == item.section,
                                onClick = { vm.navigate(item.section) },
                                icon = { Icon(item.icon, contentDescription = item.section.labelBn) },
                                label = { Text(item.shortLabel, fontSize = 11.sp, maxLines = 1) },
                                modifier = Modifier.testTag("nav_rail_${item.section.route}")
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        NavigationRailItem(
                            selected = isSettings,
                            onClick = { vm.navigate(NavSection.SETTINGS) },
                            icon = { Icon(Icons.Rounded.Settings, contentDescription = "সেটিংস") },
                            label = { Text("সেটিংস", fontSize = 11.sp) },
                            modifier = Modifier.testTag("nav_rail_settings")
                        )
                    }
                }

                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        val activeEdit = editingStudent
                        if (activeEdit != null) {
                            StudentEditorScreen(
                                initialStudent = activeEdit,
                                settings = settings,
                                permissions = permissions,
                                syncMeta = syncMeta[activeEdit.id],
                                draftingRemarkTerm = draftingTerm,
                                isGeminiConfigured = isGeminiConfigured,
                                onAddCustomField = vm::addCustomField,
                                onUpsertStudent = vm::upsertStudent,
                                onDeleteStudent = vm::deleteStudent,
                                onBack = vm::closeEditor,
                                onPreview = { saved -> vm.openStudio(saved.id) },
                                onPdf = { s -> requestPdfWithOption(listOf(s), "${s.name.ifBlank { "student" }}.pdf") },
                                onDraftRemark = vm::draftRemark,
                                onShowMessage = vm::showMessage
                            )
                        } else {
                            when (section) {
                                NavSection.DASHBOARD -> DashboardScreen(
                                    students = students,
                                    settings = settings,
                                    selectedClass = cls,
                                    permissions = permissions,
                                    isAdmin = isAdmin,
                                    pendingRequestCount = joinRequests.size,
                                    backupStatus = backupStatus,
                                    includeClassHighest = includeClassHighest,
                                    onToggleClassHighest = vm::setIncludeClassHighestInReport,
                                    includeSummaryTable = includeSummaryTable,
                                    summaryColumns = summaryColumns,
                                    onToggleSummaryTable = vm::setIncludeSummaryTableInReport,
                                    onUpdateSummaryColumns = vm::setSummaryTableColumns,
                                    onOpenApprovals = { vm.navigate(NavSection.SETTINGS) },
                                    onOpenSettings = { vm.navigate(NavSection.SETTINGS) },
                                    onCsvImport = { vm.openCsvImport("") },
                                    onBatchEdit = { vm.openBatchEdit(null) },
                                    onAiScan = vm::openAiScanner,
                                    onAllPdf = {
                                        val allowedCls = permissions.effectiveClasses().toSet()
                                        requestPdfWithOption(
                                            students.filter { it.cls in allowedCls }.sortedWith(compareBy({ it.cls }, { it.roll })),
                                            "সব-শ্রেণির-প্রতিবেদন.pdf"
                                        )
                                    },
                                    onPrintBatch = { batchList, jobName ->
                                        if (!permissions.canExportPdf) {
                                            vm.showMessage("প্রিন্ট করার অনুমতি আপনার নেই")
                                        } else if (batchList.isEmpty()) {
                                            vm.showMessage("প্রিন্ট করার জন্য কোনো শিক্ষার্থী নির্বাচিত নেই")
                                        } else {
                                            PdfGen.printStudents(
                                                ctx = ctx,
                                                students = batchList,
                                                st = settings,
                                                jobName = jobName,
                                                allStudents = students,
                                                showClassHighest = includeClassHighest,
                                                showSummaryTable = includeSummaryTable,
                                                summaryColumns = summaryColumns
                                            )
                                        }
                                    },
                                    onExportBatchPdf = { batchList, fileName ->
                                        makePdf(batchList, fileName)
                                    },
                                    onBackupNowAll = vm::backupNowAll,
                                    onOpenStudent = { vm.openEditor(it, NavSection.DASHBOARD) },
                                    onPreviewStudent = { vm.openStudio(it.id) },
                                    onOpenSheetInClass = vm::openSheetForClass
                                )

                                NavSection.ROSTER -> RosterScreen(
                                    students = students,
                                    settings = settings,
                                    cls = cls,
                                    permissions = permissions,
                                    onSelectClass = vm::selectClass,
                                    onOpenStudent = { vm.openEditor(it, NavSection.ROSTER) },
                                    onNewStudentInClass = { targetCls ->
                                        vm.selectClass(targetCls)
                                        vm.openNewStudentEditor(NavSection.ROSTER)
                                    }
                                )

                                NavSection.SHEET -> BulkSheetScreen(
                                    students = students,
                                    settings = settings,
                                    cls = cls,
                                    permissions = permissions,
                                    onSelectClass = vm::selectClass,
                                    onUpsertStudent = vm::upsertStudent,
                                    onPatchStudentField = vm::patchStudentField,
                                    onOpenStudent = { vm.openEditor(it, NavSection.SHEET) },
                                    onClassPdf = {
                                        requestPdfWithOption(
                                            students.filter { it.cls == cls }.sortedBy { it.roll },
                                            "শ্রেণি-$cls-প্রতিবেদন.pdf"
                                        )
                                    },
                                    onBatchEdit = { targetCls -> vm.openBatchEdit(targetCls) },
                                    onBatchPatchTraits = vm::batchUpdateTraits,
                                    onBatchPatchAttendance = vm::batchUpdateAttendance,
                                    onBatchPatchRemarks = vm::batchUpdateRemarks,
                                    onShowMessage = vm::showMessage
                                )

                                NavSection.STUDIO -> if (!permissions.canExportPdf) {
                                    Column(
                                        Modifier.fillMaxSize().padding(24.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = AmberNotice, modifier = Modifier.size(36.dp))
                                        Text(
                                            "রিপোর্ট কার্ড প্রিভিউ ও PDF তৈরির অনুমতি আপনার নেই",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = SlateText,
                                            modifier = Modifier.padding(top = 10.dp)
                                        )
                                        Text(
                                            "অ্যাডমিন আপনার জন্য PDF ও রিপোর্ট কার্ড অনুমতি চালু করলে এখানে প্রিভিউ ও প্রিন্ট করতে পারবেন।",
                                            fontSize = 12.sp,
                                            color = SlateMuted,
                                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                        )
                                        Button(onClick = { vm.navigate(NavSection.DASHBOARD) }) {
                                            Text("ড্যাশবোর্ডে ফিরুন")
                                        }
                                    }
                                } else {
                                    val allowedCls = permissions.effectiveClasses().toSet()
                                    val allowedStudents = students.filter { it.cls in allowedCls }
                                    ReportStudioScreen(
                                        students = allowedStudents,
                                        allSchoolStudents = students,
                                        settings = settings,
                                        selectedId = studioStudentId,
                                        includeClassHighest = includeClassHighest,
                                        onToggleClassHighest = vm::setIncludeClassHighestInReport,
                                        includeSummaryTable = includeSummaryTable,
                                        summaryColumns = summaryColumns,
                                        onToggleSummaryTable = vm::setIncludeSummaryTableInReport,
                                        onUpdateSummaryColumns = vm::setSummaryTableColumns,
                                        onSelectId = vm::selectStudioStudent,
                                        onUpdateSettings = vm::updateSettings,
                                        onPrintStudent = { s ->
                                            PdfGen.printStudents(
                                                ctx = ctx,
                                                students = listOf(s),
                                                st = settings,
                                                jobName = "${s.name.ifBlank { "Report" }}_${s.roll}",
                                                allStudents = students,
                                                showClassHighest = includeClassHighest,
                                                showSummaryTable = includeSummaryTable,
                                                summaryColumns = summaryColumns
                                            )
                                        },
                                        onExportPdf = { s -> makePdf(listOf(s), "${s.name.ifBlank { "student" }}-report.pdf") },
                                        onPrintBatch = { batchList, jobName ->
                                            if (batchList.isEmpty()) {
                                                vm.showMessage("প্রিন্ট করার জন্য কোনো শিক্ষার্থী নির্বাচিত নেই")
                                            } else {
                                                PdfGen.printStudents(
                                                    ctx = ctx,
                                                    students = batchList,
                                                    st = settings,
                                                    jobName = jobName,
                                                    allStudents = students,
                                                    showClassHighest = includeClassHighest,
                                                    showSummaryTable = includeSummaryTable,
                                                    summaryColumns = summaryColumns
                                                )
                                            }
                                        },
                                        onExportBatchPdf = { batchList, fileName ->
                                            makePdf(batchList, fileName)
                                        },
                                        onBack = { vm.navigate(NavSection.DASHBOARD) }
                                    )
                                }

                                NavSection.ASSISTANT -> AiAssistantScreen(
                                    state = aiState,
                                    students = students,
                                    selectedClass = cls,
                                    permissions = permissions,
                                    isGeminiConfigured = isGeminiConfigured,
                                    activeModelName = geminiModel,
                                    availableModels = geminiAvailableModels,
                                    customModels = geminiCustomModels,
                                    newModelIds = geminiNewModelIds,
                                    refreshingModels = geminiRefreshingModels,
                                    modelsError = geminiModelsError,
                                    onSelectModel = vm::selectGeminiModel,
                                    onRefreshModels = { vm.refreshGeminiModels(silent = false) },
                                    onRemoveCustomModel = vm::removeCustomGeminiModel,
                                    onOpenAiSettings = { vm.navigate(NavSection.SETTINGS) },
                                    onSetActiveTab = vm::setAiAssistantTab,
                                    onSetModelMode = vm::setAiModelMode,
                                    onSetContextStudent = vm::setAiContextStudent,
                                    onClearChat = vm::clearAiChat,
                                    onSendChat = vm::sendChatMessage,
                                    onOpenCsvImport = { vm.openCsvImport("") },
                                    onPickMarksheetUri = vm::loadMarksheetImage,
                                    onClearImage = vm::clearMarksheetImage,
                                    onToggleDocumentEnhance = vm::setDocumentEnhanceMode,
                                    onRotateImage = vm::rotateMarksheetImage90,
                                    onCropImage = vm::cropMarksheetImage,
                                    onRestoreOriginalImage = vm::restoreOriginalMarksheetImage,
                                    onAnalyzeImage = vm::analyzeSelectedMarksheet,
                                    onApplyExtracted = { ext -> vm.applyExtractedMarksheet(ext) },
                                    onAnalyzeClassSheet = { c, t, s, note, scope ->
                                        vm.analyzeClassSheet(c, t, s, note, scope)
                                    },
                                    onLoadSampleClassPreview = vm::loadSampleClassSheetPreview,
                                    onUpdateClassSheetMeta = vm::updateClassSheetMeta,
                                    onUpdateClassSheetScope = vm::updateClassSheetScope,
                                    onToggleScannedRowSelected = vm::toggleScannedRowSelected,
                                    onSetAllScannedRowsSelected = vm::setAllScannedRowsSelected,
                                    onUpdateScannedRowField = vm::updateScannedRowField,
                                    onAddBlankScannedRow = vm::addBlankScannedRow,
                                    onRemoveScannedRow = vm::removeScannedRow,
                                    onMarkAllScannedIssuesVerified = vm::markAllScannedIssuesVerified,
                                    onDismissClassSheetPreview = vm::dismissClassSheetPreview,
                                    onCommitClassSheet = vm::commitScannedClassSheet
                                )

                                NavSection.SETTINGS -> SettingsScreen(
                                    students = students,
                                    settings = settings,
                                    currentUserId = vm.currentUserId,
                                    activeSchool = activeSchool,
                                    permissions = permissions,
                                    joinRequests = joinRequests,
                                    backupStatus = backupStatus,
                                    driveFiles = driveFiles,
                                    driveFilesLoading = driveFilesLoading,
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
                                    onUpdateSettings = vm::updateSettings,
                                    onAddCustomField = vm::addCustomField,
                                    onRemoveCustomField = vm::removeCustomField,
                                    onOpenCsvImport = { txt -> vm.openCsvImport(txt) },
                                    onRestoreBackupJson = vm::restoreBackupJson,
                                    onExportAllPdf = {
                                        val allowedCls = permissions.effectiveClasses().toSet()
                                        requestPdfWithOption(
                                            students.filter { it.cls in allowedCls }.sortedWith(compareBy({ it.cls }, { it.roll })),
                                            "সব-শ্রেণির-প্রতিবেদন.pdf"
                                        )
                                    },
                                    onResetSampleData = vm::resetSampleData,
                                    onClearAllStudents = vm::clearAllStudents,
                                    onRegenerateJoinCode = vm::regenerateJoinCode,
                                    onApproveJoinRequest = vm::approveJoinRequest,
                                    onRejectJoinRequest = vm::rejectJoinRequest,
                                    onUpdateMemberPermissions = vm::updateMemberPermissions,
                                    onUpdateMemberRole = vm::updateMemberRole,
                                    onRemoveMember = vm::removeMember,
                                    onLeaveSchool = vm::leaveActiveSchool,
                                    onToggleLocalAutoBackup = vm::toggleLocalAutoBackup,
                                    onSetLocalBackupFolder = vm::setLocalBackupFolder,
                                    onToggleDriveAutoBackup = vm::toggleDriveAutoBackup,
                                    onToggleCsvAutoBackup = vm::toggleCsvAutoBackup,
                                    onSetCsvBackupFolder = vm::setCsvBackupFolder,
                                    onSelectCsvHeaderStyle = vm::setCsvHeaderStyle,
                                    onSelectCsvBackupFormat = vm::setCsvBackupFormat,
                                    onUpdateBackupSchedule = vm::updateBackupSchedule,
                                    onBackupNowAll = vm::backupNowAll,
                                    onBackupLocalNow = vm::backupLocalNow,
                                    onBackupDriveNow = vm::backupDriveNow,
                                    onBackupCsvNow = vm::backupCsvNow,
                                    onRestoreLocalSnapshot = vm::restoreLocalSnapshot,
                                    onLoadDriveBackups = vm::loadDriveBackupList,
                                    onRestoreFromDriveFile = vm::restoreFromDriveFile,
                                    onSaveGeminiConfig = vm::saveGeminiConfig,
                                    onTestGeminiConnection = vm::testGeminiConnection,
                                    onRefreshGeminiModels = { rawKey -> vm.refreshGeminiModels(rawKey, silent = false) },
                                    onRemoveCustomGeminiModel = vm::removeCustomGeminiModel,
                                    onClearGeminiApiKey = vm::clearGeminiApiKey,
                                    onSignOut = onSignOut,
                                    onShowMessage = vm::showMessage,
                                    onBack = { vm.navigate(NavSection.DASHBOARD) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (csvImportState.isOpen) {
            SmartCsvImportDialog(
                students = students,
                settings = settings,
                defaultCls = cls,
                initialText = csvImportState.initialText,
                onDismiss = vm::closeCsvImport,
                onImported = vm::applyCsvImport,
                onError = vm::showMessage
            )
        }

        if (batchEditOpen) {
            BatchStudentEditDialog(
                students = students,
                settings = settings,
                permissions = permissions,
                initialClassFilter = batchEditInitialClass,
                onDismiss = vm::closeBatchEdit,
                onApply = vm::batchUpdateStudents
            )
        }
    }

    if (askSignOut) {
        AlertDialog(
            onDismissRequest = { askSignOut = false },
            title = { Text("সাইন আউট করবেন?") },
            text = { Text("আপনার সকল ডেটা ক্লাউডে নিরাপদ থাকবে। পুনরায় Google দিয়ে সাইন-ইন করলে সব ফিরে পাবেন।") },
            confirmButton = {
                Button(
                    onClick = {
                        askSignOut = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                    modifier = Modifier.testTag("topbar_confirm_signout_btn")
                ) { Text("হ্যাঁ, সাইন আউট") }
            },
            dismissButton = {
                TextButton(onClick = { askSignOut = false }) { Text("বাতিল") }
            }
        )
    }

    pendingPdfOptionPrompt?.let { (targetList, targetFileName) ->
        AlertDialog(
            onDismissRequest = { pendingPdfOptionPrompt = null },
            title = { Text("রিপোর্ট PDF অপশন", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "${bn(targetList.size.toString())} জন শিক্ষার্থীর (${bn((targetList.size * 2).toString())} পৃষ্ঠা) PDF তৈরি হবে।",
                        fontSize = 13.sp,
                        color = SlateText
                    )
                    Surface(
                        color = if (includeClassHighest) TealSoft else SlateBg,
                        border = BorderStroke(1.dp, if (includeClassHighest) TealPrimary.copy(alpha = 0.45f) else SlateMuted.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.setIncludeClassHighestInReport(!includeClassHighest) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = includeClassHighest,
                                onCheckedChange = vm::setIncludeClassHighestInReport,
                                modifier = Modifier.testTag("pdf_prompt_class_highest_checkbox")
                            )
                            Column(Modifier.weight(1f).padding(end = 6.dp)) {
                                Text(
                                    "৩য় পৃষ্ঠায় 'শ্রেণিতে সর্বোচ্চ নম্বর' কলাম যোগ করুন",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SlateText
                                )
                                Text(
                                    "চূড়ান্ত ফলাফলের অধীনে শ্রেণির সর্বোচ্চ নম্বর দেখাবে",
                                    fontSize = 11.sp,
                                    color = SlateMuted
                                )
                            }
                        }
                    }

                    SummaryTableConfigCard(
                        includeSummaryTable = includeSummaryTable,
                        selectedColumns = summaryColumns,
                        onToggleInclude = vm::setIncludeSummaryTableInReport,
                        onUpdateColumns = vm::setSummaryTableColumns,
                        initiallyExpanded = true,
                        collapsibleWhenEnabled = true,
                        testTagPrefix = "pdf_prompt_summary"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingPdfOptionPrompt = null
                        makePdf(targetList, targetFileName)
                    },
                    modifier = Modifier.testTag("pdf_prompt_confirm_btn")
                ) {
                    Text("PDF তৈরি করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPdfOptionPrompt = null }) { Text("বাতিল") }
            }
        )
    }

    pdfExportingCount?.let { count ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("PDF তৈরি হচ্ছে...", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp, color = TealPrimary)
                    Text(
                        "${bn(count.toString())} জন শিক্ষার্থীর (${bn((count * 2).toString())} পৃষ্ঠা) প্রতিবেদন প্রস্তুত করা হচ্ছে...",
                        fontSize = 13.sp,
                        color = SlateText
                    )
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun ResultMakerTopBar(
    settings: Settings,
    activeSchool: SchoolWorkspace,
    currentUserId: String,
    permissions: TeacherPermissions,
    pendingRequestCount: Int,
    isSettings: Boolean,
    onBackFromSettings: () -> Unit,
    onOpenSettings: () -> Unit,
    onCsvImport: () -> Unit,
    onAiScan: () -> Unit,
    onAllPdf: () -> Unit,
    onOpenSchoolHub: () -> Unit,
    onRequestSignOut: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val isAdmin = activeSchool.isAdmin(currentUserId)

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = SlateText
        ),
        navigationIcon = {
            if (isSettings) {
                IconButton(
                    onClick = onBackFromSettings,
                    modifier = Modifier.size(48.dp).testTag("settings_back_btn")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "ফিরে যান")
                }
            }
        },
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        if (isSettings) "সেটিংস, সদস্য ও ব্যাকআপ" else "Result Maker",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        modifier = Modifier.testTag("top_bar_title")
                    )
                    if (!isSettings) {
                        Surface(
                            color = TealSoft,
                            border = BorderStroke(1.dp, EmeraldGrade.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "● লাইভ · ${bn(activeSchool.memberUids.size.toString())} জন",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldGrade,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (!isSettings) {
                    Text(
                        "${settings.school.ifBlank { "প্রাথমিক বিদ্যালয়" }} · ${if (isAdmin) "অ্যাডমিন" else "শিক্ষক"}",
                        fontSize = 11.sp,
                        color = SlateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            if (!isSettings) {
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(48.dp).testTag("top_bar_settings_btn")
                ) {
                    Icon(
                        Icons.Rounded.Settings,
                        contentDescription = if (pendingRequestCount > 0) "সেটিংস (${pendingRequestCount}টি নতুন অনুরোধ)" else "সেটিংস ও সদস্য"
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.size(48.dp).testTag("top_bar_menu_btn")
                    ) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "অতিরিক্ত অপশন")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (permissions.canManageStudents) {
                            DropdownMenuItem(
                                text = { Text("স্মার্ট CSV ইমপোর্ট") },
                                onClick = { menuOpen = false; onCsvImport() }
                            )
                        }
                        if (permissions.canManageStudents || permissions.allSubjects || permissions.effectiveSubjects().isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("AI নম্বরপত্র স্ক্যান") },
                                onClick = { menuOpen = false; onAiScan() }
                            )
                        }
                        if (permissions.canExportPdf) {
                            DropdownMenuItem(
                                text = { Text("সব শ্রেণির সব শিক্ষার্থীর PDF") },
                                onClick = { menuOpen = false; onAllPdf() }
                            )
                        }
                        if (permissions.canManageStudents || permissions.canExportPdf) {
                            HorizontalDivider()
                        }
                        DropdownMenuItem(
                            text = { Text("বিদ্যালয় পরিচিতি") },
                            onClick = { menuOpen = false; onOpenSchoolHub() },
                            modifier = Modifier.testTag("menu_switch_school")
                        )
                        DropdownMenuItem(
                            text = { Text("সাইন আউট", color = RoseDanger) },
                            onClick = { menuOpen = false; onRequestSignOut() },
                            modifier = Modifier.testTag("menu_sign_out")
                        )
                    }
                }
            }
        }
    )
}
