@file:OptIn(ExperimentalMaterial3Api::class)

package com.kgps.result

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kgps.result.ui.screens.AiAssistantScreen
import com.kgps.result.ui.screens.BulkSheetScreen
import com.kgps.result.ui.screens.ReportStudioScreen
import com.kgps.result.ui.screens.RosterScreen
import com.kgps.result.ui.screens.SettingsScreen
import com.kgps.result.ui.screens.SmartCsvImportDialog
import com.kgps.result.ui.screens.StudentEditorScreen
import com.kgps.result.ui.theme.ResultMakerTheme
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealContainer
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ResultMakerTheme {
                ResultMakerApp()
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
    NavItem(NavSection.ROSTER, "শিক্ষার্থী", Icons.Rounded.People),
    NavItem(NavSection.SHEET, "শিট এন্ট্রি", Icons.Rounded.GridOn),
    NavItem(NavSection.STUDIO, "রিপোর্ট", Icons.Rounded.Description),
    NavItem(NavSection.ASSISTANT, "AI সহায়ক", Icons.Rounded.AutoAwesome)
)

@Composable
fun ResultMakerApp(vm: ResultMakerViewModel = viewModel()) {
    val ctx = LocalContext.current
    val students by vm.students.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val cls by vm.selectedClass.collectAsStateWithLifecycle()
    val section by vm.currentSection.collectAsStateWithLifecycle()
    val editingStudent by vm.editingStudent.collectAsStateWithLifecycle()
    val studioStudentId by vm.studioStudentId.collectAsStateWithLifecycle()
    val csvImportState by vm.csvImportState.collectAsStateWithLifecycle()
    val snackbarMessage by vm.snackbarMessage.collectAsStateWithLifecycle()
    val speakingId by vm.speakingId.collectAsStateWithLifecycle()
    val aiState by vm.aiState.collectAsStateWithLifecycle()
    val draftingTerm by vm.draftingRemarkTerm.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            vm.clearMessage()
        }
    }

    var pendingPdfStudents by remember { mutableStateOf<List<Student>>(emptyList()) }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.openOutputStream(uri)?.use { out ->
                    PdfGen.write(ctx, pendingPdfStudents, settings, out)
                }
                vm.showMessage("PDF সংরক্ষিত হয়েছে")
            } catch (e: Exception) {
                vm.showMessage("PDF তৈরি করা যায়নি: ${e.message ?: "ত্রুটি"}")
            }
        }
    }

    val makePdf: (List<Student>, String) -> Unit = { list, fileName ->
        if (list.isEmpty()) {
            vm.showMessage("PDF তৈরির জন্য কোনো শিক্ষার্থী নেই")
        } else {
            pendingPdfStudents = list
            pdfLauncher.launch(fileName)
        }
    }

    val isEditing = editingStudent != null
    val isSettings = section == NavSection.SETTINGS
    val showPrimaryChrome = !isEditing && !isSettings

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = SlateBg,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (!isEditing) {
                    ResultMakerTopBar(
                        settings = settings,
                        isSettings = isSettings,
                        onBackFromSettings = { vm.navigate(NavSection.ROSTER) },
                        onOpenSettings = { vm.navigate(NavSection.SETTINGS) },
                        onCsvImport = { vm.openCsvImport("") },
                        onAllPdf = {
                            makePdf(
                                students.sortedWith(compareBy({ it.cls }, { it.roll })),
                                "সব-শ্রেণির-প্রতিবেদন.pdf"
                            )
                        }
                    )
                }
            },
            floatingActionButton = {
                if (!isEditing && section == NavSection.ROSTER) {
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
                                        fontSize = 11.5.sp,
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
                    if (speakingId != null) {
                        Surface(
                            color = TealSoft,
                            border = BorderStroke(1.dp, TealContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                                    Text("অডিও পাঠ চলছে...", fontSize = 12.sp, color = TealDark, fontWeight = FontWeight.Medium)
                                }
                                TextButton(
                                    onClick = vm::stopSpeaking,
                                    modifier = Modifier.testTag("tts_stop_banner_btn")
                                ) {
                                    Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(" থামান", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        val activeEdit = editingStudent
                        if (activeEdit != null) {
                            StudentEditorScreen(
                                initialStudent = activeEdit,
                                settings = settings,
                                speakingId = speakingId,
                                draftingRemarkTerm = draftingTerm,
                                isPlaceholderAi = vm.isPlaceholderAi,
                                onAddCustomField = vm::addCustomField,
                                onUpsertStudent = vm::upsertStudent,
                                onDeleteStudent = vm::deleteStudent,
                                onBack = vm::closeEditor,
                                onPreview = { saved -> vm.openStudio(saved.id) },
                                onPdf = { s -> makePdf(listOf(s), "${s.name.ifBlank { "student" }}.pdf") },
                                onSpeakText = vm::speakText,
                                onDraftRemark = vm::draftRemark,
                                onShowMessage = vm::showMessage
                            )
                        } else {
                            when (section) {
                                NavSection.ROSTER -> RosterScreen(
                                    students = students,
                                    settings = settings,
                                    cls = cls,
                                    speakingId = speakingId,
                                    onSelectClass = vm::selectClass,
                                    onOpenStudent = { vm.openEditor(it, NavSection.ROSTER) },
                                    onPreviewStudent = { vm.openStudio(it.id) },
                                    onSpeakStudent = vm::speakStudentSummary,
                                    onNewStudentInClass = { targetCls ->
                                        vm.selectClass(targetCls)
                                        vm.openNewStudentEditor(NavSection.ROSTER)
                                    },
                                    onOpenSheetInClass = vm::openSheetForClass,
                                    onCsvImport = { vm.openCsvImport("") },
                                    onClassPdf = { targetCls ->
                                        makePdf(
                                            students.filter { it.cls == targetCls }.sortedBy { it.roll },
                                            "শ্রেণি-$targetCls-প্রতিবেদন.pdf"
                                        )
                                    },
                                    onAllPdf = {
                                        makePdf(
                                            students.sortedWith(compareBy({ it.cls }, { it.roll })),
                                            "সব-শ্রেণির-প্রতিবেদন.pdf"
                                        )
                                    }
                                )

                                NavSection.SHEET -> BulkSheetScreen(
                                    students = students,
                                    settings = settings,
                                    cls = cls,
                                    onSelectClass = vm::selectClass,
                                    onUpsertStudent = vm::upsertStudent,
                                    onOpenStudent = { vm.openEditor(it, NavSection.SHEET) },
                                    onClassPdf = {
                                        makePdf(
                                            students.filter { it.cls == cls }.sortedBy { it.roll },
                                            "শ্রেণি-$cls-প্রতিবেদন.pdf"
                                        )
                                    },
                                    onShowMessage = vm::showMessage
                                )

                                NavSection.STUDIO -> ReportStudioScreen(
                                    students = students,
                                    settings = settings,
                                    selectedId = studioStudentId,
                                    speakingId = speakingId,
                                    onSelectId = vm::selectStudioStudent,
                                    onUpdateSettings = vm::updateSettings,
                                    onSpeakStudent = vm::speakStudentSummary,
                                    onPrintStudent = { s ->
                                        PdfGen.printStudents(
                                            ctx,
                                            listOf(s),
                                            settings,
                                            "${s.name.ifBlank { "Report" }}_${s.roll}"
                                        )
                                    },
                                    onExportPdf = { s -> makePdf(listOf(s), "${s.name.ifBlank { "student" }}-report.pdf") },
                                    onBack = { vm.navigate(NavSection.ROSTER) }
                                )

                                NavSection.ASSISTANT -> AiAssistantScreen(
                                    state = aiState,
                                    students = students,
                                    speakingId = speakingId,
                                    isPlaceholderAi = vm.isPlaceholderAi,
                                    onSetModelMode = vm::setAiModelMode,
                                    onSetContextStudent = vm::setAiContextStudent,
                                    onClearChat = vm::clearAiChat,
                                    onSendChat = vm::sendChatMessage,
                                    onPickMarksheetUri = vm::loadMarksheetImage,
                                    onClearImage = vm::clearMarksheetImage,
                                    onAnalyzeImage = vm::analyzeSelectedMarksheet,
                                    onApplyExtracted = { ext -> vm.applyExtractedMarksheet(ext) },
                                    onSpeakText = vm::speakText
                                )

                                NavSection.SETTINGS -> SettingsScreen(
                                    students = students,
                                    settings = settings,
                                    onUpdateSettings = vm::updateSettings,
                                    onAddCustomField = vm::addCustomField,
                                    onRemoveCustomField = vm::removeCustomField,
                                    onOpenCsvImport = { txt -> vm.openCsvImport(txt) },
                                    onRestoreBackupJson = vm::restoreBackupJson,
                                    onExportAllPdf = {
                                        makePdf(
                                            students.sortedWith(compareBy({ it.cls }, { it.roll })),
                                            "সব-শ্রেণির-প্রতিবেদন.pdf"
                                        )
                                    },
                                    onResetSampleData = vm::resetSampleData,
                                    onClearAllStudents = vm::clearAllStudents,
                                    onShowMessage = vm::showMessage,
                                    onBack = { vm.navigate(NavSection.ROSTER) }
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
    }
}

@Composable
private fun ResultMakerTopBar(
    settings: Settings,
    isSettings: Boolean,
    onBackFromSettings: () -> Unit,
    onOpenSettings: () -> Unit,
    onCsvImport: () -> Unit,
    onAllPdf: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

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
                Text(
                    if (isSettings) "সেটিংস ও ব্যাকআপ" else "Result Maker",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    modifier = Modifier.testTag("top_bar_title")
                )
                if (!isSettings) {
                    Text(
                        settings.school.ifBlank { "প্রাথমিক বিদ্যালয় প্রতিবেদন" },
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
                    Icon(Icons.Rounded.Settings, contentDescription = "সেটিংস ও ব্যাকআপ")
                }

                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.size(48.dp).testTag("top_bar_menu_btn")
                    ) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "অতিরিক্ত অপশন")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("স্মার্ট CSV ইমপোর্ট") },
                            onClick = { menuOpen = false; onCsvImport() }
                        )
                        DropdownMenuItem(
                            text = { Text("সব শ্রেণির সব শিক্ষার্থীর PDF") },
                            onClick = { menuOpen = false; onAllPdf() }
                        )
                    }
                }
            }
        }
    )
}
