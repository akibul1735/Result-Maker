@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.kgps.result.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.AdmitCardLocalStore
import com.kgps.result.BENGALI_PDF_FONT_LIST
import com.kgps.result.ENGLISH_PDF_FONT_LIST
import com.kgps.result.SeatPlanRollOrder
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.TestimonialConfig
import com.kgps.result.TestimonialLocalStore
import com.kgps.result.TestimonialPageSpec
import com.kgps.result.TestimonialPlanner
import com.kgps.result.TestimonialStudentOverride
import com.kgps.result.activeClassIds
import com.kgps.result.bn
import com.kgps.result.classShortBn
import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class TestimonialBottomTabItem(
    val index: Int,
    val label: String,
    val panelTitle: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
)

private val TESTIMONIAL_BOTTOM_TABS = listOf(
    TestimonialBottomTabItem(0, "শ্রেণি ও রোল", "শ্রেণি, রোল ও শিক্ষার্থীর তথ্য", Icons.Rounded.Groups, Icons.Outlined.Groups),
    TestimonialBottomTabItem(1, "তথ্য ও বডি", "ধরন, সিরিয়াল ও বক্তব্য", Icons.Rounded.Description, Icons.Outlined.Description),
    TestimonialBottomTabItem(2, "প্রতিষ্ঠান ও স্বাক্ষর", "প্রতিষ্ঠান, সিল ও স্বাক্ষর", Icons.Rounded.School, Icons.Outlined.School),
    TestimonialBottomTabItem(3, "ডিজাইন ও ফন্ট", "মুড়ি, বর্ডার ও ফন্ট", Icons.Rounded.Tune, Icons.Outlined.Tune)
)

private enum class TestimonialExportAction { PDF, PRINT }

@Composable
fun TestimonialPdfScreen(
    students: List<Student>,
    settings: Settings,
    permissions: TeacherPermissions,
    initialClass: Int,
    onExportPdf: (pages: List<TestimonialPageSpec>, signatureBitmap: Bitmap?, fileName: String) -> Unit,
    onPrint: (pages: List<TestimonialPageSpec>, signatureBitmap: Bitmap?, jobName: String) -> Unit,
    onSaveStudentProfiles: (updatedStudents: List<Student>, nextSettings: Settings) -> Unit = { _, _ -> },
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val availableClassIds = remember(settings.customClasses) { activeClassIds(settings) }
    val allowedClasses = remember(permissions, availableClassIds) {
        permissions.effectiveClasses(availableClassIds)
    }

    val defaultConfig = remember(initialClass, allowedClasses, settings.pdfFont, settings.pdfEnglishFont, settings.year) {
        val defaultCls = when {
            initialClass in allowedClasses -> initialClass
            5 in allowedClasses -> 5
            else -> allowedClasses.firstOrNull() ?: 1
        }
        TestimonialConfig(
            selectedClass = defaultCls,
            selectedClasses = listOf(defaultCls),
            rollOrder = SeatPlanRollOrder.DESCENDING,
            bnFontId = settings.pdfFont.ifBlank { "noto_serif" },
            enFontId = settings.pdfEnglishFont.ifBlank { "times_new_roman" },
            sessionYear = settings.year.ifBlank { "২০২৬" },
            issueDate = TestimonialPlanner.todayBanglaDateString()
        )
    }

    var config by remember(defaultConfig) {
        val loaded = TestimonialLocalStore.loadConfig(ctx, defaultConfig)
        mutableStateOf(
            loaded.copy(
                selectedClass = loaded.selectedClass.takeIf { it in allowedClasses } ?: defaultConfig.selectedClass,
                selectedClasses = loaded.resolvedSelectedClasses.filter { it in allowedClasses }.ifEmpty { defaultConfig.selectedClasses }
            )
        )
    }
    var overrides by remember { mutableStateOf(TestimonialLocalStore.loadOverrides(ctx)) }
    var signatureBitmap by remember { mutableStateOf(AdmitCardLocalStore.loadSignatureBitmap(ctx)) }

    var rollFromText by remember(config.rollFrom) { mutableStateOf(config.rollFrom?.toString().orEmpty()) }
    var rollToText by remember(config.rollTo) { mutableStateOf(config.rollTo?.toString().orEmpty()) }

    var activeTab by remember { mutableIntStateOf(0) }
    var panelOpen by remember { mutableStateOf(false) }
    var previewFullscreen by remember { mutableStateOf(false) }
    var showStudentInfoDialog by remember { mutableStateOf(false) }
    var showBnFontPicker by remember { mutableStateOf(false) }
    var showEnFontPicker by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var pendingExportAction by remember { mutableStateOf<TestimonialExportAction?>(null) }

    val handleSignaturePicked: (Uri?) -> Unit = { uri ->
        if (uri != null) {
            scope.launch {
                val loaded = withContext(Dispatchers.IO) {
                    AdmitCardLocalStore.saveSignatureFromUri(ctx, uri)
                }
                if (loaded != null) {
                    signatureBitmap = loaded
                    Toast.makeText(ctx, "স্বাক্ষরের ছবি যুক্ত হয়েছে", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(ctx, "ছবিটি পড়া যায়নি", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == android.app.Activity.RESULT_OK) {
            handleSignaturePicked(res.data?.data)
        }
    }

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        handleSignaturePicked(uri)
    }

    BackHandler(enabled = panelOpen || previewFullscreen) {
        when {
            previewFullscreen -> previewFullscreen = false
            panelOpen -> panelOpen = false
        }
    }

    LaunchedEffect(config) {
        TestimonialLocalStore.saveConfig(ctx, config)
    }

    LaunchedEffect(allowedClasses) {
        if (allowedClasses.isNotEmpty()) {
            val validSelected = config.resolvedSelectedClasses.filter { it in allowedClasses }
                .ifEmpty { listOf(allowedClasses.first()) }
            val nextPrimary = config.selectedClass.takeIf { it in validSelected } ?: validSelected.first()
            if (validSelected != config.resolvedSelectedClasses || nextPrimary != config.selectedClass) {
                config = config.copy(selectedClass = nextPrimary, selectedClasses = validSelected)
            }
        }
    }

    val countByClass = remember(students, allowedClasses) {
        allowedClasses.associateWith { c -> students.count { it.cls == c } }
    }

    val previewPages = remember(students, settings, config, overrides, allowedClasses) {
        TestimonialPlanner.buildMultiClassPages(
            students = students,
            settings = settings,
            baseConfig = config,
            selectedClasses = config.resolvedSelectedClasses,
            overrides = overrides,
            allowedClassIds = allowedClasses
        )
    }

    val selectedStudentsForEditor = remember(students, config, allowedClasses) {
        val selectedSet = config.resolvedSelectedClasses.filter { it in allowedClasses }.toSet()
        val minRoll = config.rollFrom?.coerceAtLeast(1)
        val maxRoll = config.rollTo?.coerceAtLeast(1)
        students.filter { st ->
            st.cls in selectedSet &&
                (minRoll == null || st.roll >= minRoll) &&
                (maxRoll == null || st.roll <= maxRoll)
        }.sortedWith(compareBy({ it.cls }, { it.roll }))
    }

    val missingInfoCount = remember(selectedStudentsForEditor, settings, config, overrides) {
        selectedStudentsForEditor.count { st ->
            val ov = overrides[st.id]
            val f = TestimonialPlanner.resolveFatherName(st, settings, ov)
            val m = TestimonialPlanner.resolveMotherName(st, settings, config.motherFieldKeyHint, ov)
            val d = TestimonialPlanner.resolveDob(st, settings, config.dobFieldKeyHint, config.numeralMode, ov)
            f.isBlank() || m.isBlank() || d.isBlank()
        }
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { previewPages.size.coerceAtLeast(1) }
    )
    LaunchedEffect(previewPages.size) {
        if (previewPages.isNotEmpty() && pagerState.currentPage >= previewPages.size) {
            pagerState.scrollToPage(previewPages.lastIndex)
        }
    }

    var viewportHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Compact Top Action Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateOutline.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (previewPages.isNotEmpty()) {
                        Surface(
                            color = TealSoft,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        if (pagerState.currentPage > 0) {
                                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                        }
                                    },
                                    enabled = pagerState.currentPage > 0,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                        contentDescription = "আগের পাতা",
                                        tint = TealDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                val curPage = previewPages.getOrNull(pagerState.currentPage)
                                val pageBadge = buildString {
                                    append("পৃষ্ঠা ${bn((pagerState.currentPage + 1).toString())}/${bn(previewPages.size.toString())}")
                                    if (curPage != null) {
                                        append(" · ${classShortBn(curPage.item.cls, settings)}")
                                    }
                                }
                                Text(
                                    text = pageBadge,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TealDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                IconButton(
                                    onClick = {
                                        if (pagerState.currentPage < previewPages.lastIndex) {
                                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                        }
                                    },
                                    enabled = pagerState.currentPage < previewPages.lastIndex,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = "পরের পাতা",
                                        tint = TealDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "প্রশংসাপত্র মেকার",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TealDark
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    IconButton(
                        onClick = { showResetConfirm = true },
                        modifier = Modifier.size(38.dp).testTag("testimonial_reset_btn")
                    ) {
                        Icon(
                            Icons.Rounded.RestartAlt,
                            contentDescription = "ডিফল্ট রিসেট",
                            tint = SlateMuted,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = { pendingExportAction = TestimonialExportAction.PRINT },
                        enabled = previewPages.isNotEmpty(),
                        modifier = Modifier.size(38.dp).testTag("testimonial_print_btn")
                    ) {
                        Icon(
                            Icons.Rounded.Print,
                            contentDescription = "প্রিন্ট করুন",
                            tint = if (previewPages.isNotEmpty()) TealPrimary else SlateMuted.copy(alpha = 0.45f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Button(
                        onClick = { pendingExportAction = TestimonialExportAction.PDF },
                        enabled = previewPages.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp).testTag("testimonial_export_btn")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("PDF", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                    }
                }
            }

            // 2. Preview Viewport + Floating Overlay Sheet
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onSizeChanged { viewportHeightPx = it.height }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (previewPages.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, SlateOutline),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "নির্বাচিত শ্রেণি বা রোল রেঞ্জে কোনো শিক্ষার্থী নেই।",
                                        fontSize = 12.5.sp,
                                        color = SlateMuted
                                    )
                                    OutlinedButton(
                                        onClick = { config = config.copy(extraBlankCount = 1) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.heightIn(min = 38.dp)
                                    ) {
                                        Text("১টি ফাঁকা প্রশংসাপত্র যোগ করুন", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            HorizontalPager(
                                state = pagerState,
                                userScrollEnabled = true,
                                modifier = Modifier.fillMaxSize().testTag("testimonial_preview_pager")
                            ) { pageIdx ->
                                TestimonialRenderedPage(
                                    ctx = ctx,
                                    page = previewPages[pageIdx],
                                    settings = settings,
                                    students = students,
                                    signatureBitmap = signatureBitmap,
                                    renderScale = 1.9f,
                                    modifier = Modifier.fillMaxSize().padding(vertical = 4.dp)
                                )
                            }

                            Surface(
                                color = Color(0xFF0F172A).copy(alpha = 0.72f),
                                shape = RoundedCornerShape(9.dp),
                                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                            ) {
                                IconButton(
                                    onClick = { previewFullscreen = true },
                                    modifier = Modifier.size(40.dp).testTag("testimonial_fullscreen_btn")
                                ) {
                                    Icon(
                                        Icons.Rounded.Fullscreen,
                                        contentDescription = "ফুলস্ক্রিন প্রিভিউ",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                val maxOverlayHeightDp = if (viewportHeightPx > 0) {
                    with(density) { (viewportHeightPx * 0.68f).toDp() }.coerceIn(220.dp, 460.dp)
                } else {
                    340.dp
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = panelOpen,
                    enter = fadeIn(tween(150)) + slideInVertically(tween(220)) { it / 2 },
                    exit = fadeOut(tween(150)) + slideOutVertically(tween(180)) { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                        shadowElevation = 14.dp,
                        border = BorderStroke(1.dp, SlateOutline.copy(alpha = 0.8f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxOverlayHeightDp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            var dragDeltaY = 0f
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectVerticalDragGestures(
                                            onVerticalDrag = { _, dragAmount ->
                                                dragDeltaY += dragAmount
                                                if (dragDeltaY > 65f) {
                                                    panelOpen = false
                                                }
                                            },
                                            onDragEnd = { dragDeltaY = 0f },
                                            onDragCancel = { dragDeltaY = 0f }
                                        )
                                    }
                                    .padding(top = 6.dp, start = 12.dp, end = 8.dp, bottom = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(4.dp)
                                        .background(SlateOutline, RoundedCornerShape(2.dp))
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = TESTIMONIAL_BOTTOM_TABS[activeTab].panelTitle,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TealDark
                                    )
                                    IconButton(
                                        onClick = { panelOpen = false },
                                        modifier = Modifier.size(32.dp).testTag("testimonial_panel_close_btn")
                                    ) {
                                        Icon(
                                            Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = "সেটিংস লুকান",
                                            tint = SlateMuted
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = SlateOutline.copy(alpha = 0.55f))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                when (activeTab) {
                                    0 -> TestimonialClassAndRollTab(
                                        config = config,
                                        settings = settings,
                                        allowedClasses = allowedClasses,
                                        countByClass = countByClass,
                                        rollFromText = rollFromText,
                                        rollToText = rollToText,
                                        missingInfoCount = missingInfoCount,
                                        selectedStudentsCount = selectedStudentsForEditor.size,
                                        onSelectClasses = { selectedList, primary ->
                                            config = config.copy(
                                                selectedClasses = selectedList,
                                                selectedClass = primary
                                            )
                                        },
                                        onUpdateConfig = { config = it },
                                        onRollFromChange = { txt ->
                                            rollFromText = txt
                                            config = config.copy(rollFrom = txt.trim().toIntOrNull())
                                        },
                                        onRollToChange = { txt ->
                                            rollToText = txt
                                            config = config.copy(rollTo = txt.trim().toIntOrNull())
                                        },
                                        onOpenStudentInfoEditor = { showStudentInfoDialog = true }
                                    )
                                    1 -> TestimonialContentAndBodyTab(
                                        config = config,
                                        settings = settings,
                                        onUpdateConfig = { config = it }
                                    )
                                    2 -> TestimonialSchoolAndSignTab(
                                        config = config,
                                        settings = settings,
                                        signatureBitmap = signatureBitmap,
                                        onUpdateConfig = { config = it },
                                        onPickSignatureFromGallery = {
                                            val pickIntent = Intent(
                                                Intent.ACTION_PICK,
                                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                            ).apply { type = "image/*" }
                                            galleryPickerLauncher.launch(pickIntent)
                                        },
                                        onPickSignatureFromFiles = {
                                            docPickerLauncher.launch(arrayOf("image/*"))
                                        },
                                        onClearSignature = {
                                            AdmitCardLocalStore.clearSignature(ctx)
                                            signatureBitmap = null
                                            Toast.makeText(ctx, "স্বাক্ষরের ছবি মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    3 -> TestimonialDesignAndFontTab(
                                        config = config,
                                        onUpdateConfig = { config = it },
                                        onOpenBnFontPicker = { showBnFontPicker = true },
                                        onOpenEnFontPicker = { showEnFontPicker = true }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Bottom Tab Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateOutline.copy(alpha = 0.7f)),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    TESTIMONIAL_BOTTOM_TABS.forEach { tab ->
                        val isSel = panelOpen && activeTab == tab.index
                        Surface(
                            onClick = {
                                if (panelOpen && activeTab == tab.index) {
                                    panelOpen = false
                                } else {
                                    activeTab = tab.index
                                    panelOpen = true
                                }
                            },
                            color = if (isSel) TealSoft else Color.Transparent,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("testimonial_tab_${tab.index}")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSel) tab.activeIcon else tab.inactiveIcon,
                                    contentDescription = tab.label,
                                    tint = if (isSel) TealPrimary else SlateMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = tab.label,
                                    fontSize = 9.8.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) TealDark else SlateMuted,
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

    if (previewFullscreen && previewPages.isNotEmpty()) {
        TestimonialFullscreenDialog(
            ctx = ctx,
            pages = previewPages,
            initialPageIndex = pagerState.currentPage,
            settings = settings,
            students = students,
            signatureBitmap = signatureBitmap,
            onPageChanged = { idx ->
                scope.launch { pagerState.scrollToPage(idx) }
            },
            onDismiss = { previewFullscreen = false }
        )
    }

    if (showStudentInfoDialog) {
        TestimonialStudentInfoEditorDialog(
            students = selectedStudentsForEditor,
            settings = settings,
            config = config,
            overrides = overrides,
            canSaveToProfile = permissions.canManageStudents,
            onSaveAll = { updatedMap, saveToProfiles ->
                val merged = overrides.toMutableMap().apply { putAll(updatedMap) }
                overrides = merged
                TestimonialLocalStore.saveOverrides(ctx, merged)
                if (saveToProfiles) {
                    val (updatedStudents, nextSettings) = applyStudentProfileFieldUpdates(
                        students = selectedStudentsForEditor,
                        settings = settings,
                        config = config,
                        updatedMap = updatedMap
                    )
                    if (updatedStudents.isNotEmpty() || nextSettings != settings) {
                        onSaveStudentProfiles(updatedStudents, nextSettings)
                    }
                }
                showStudentInfoDialog = false
                Toast.makeText(ctx, "শিক্ষার্থীদের তথ্য সংরক্ষিত হয়েছে", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showStudentInfoDialog = false }
        )
    }

    if (showBnFontPicker) {
        TestimonialFontPickerDialog(
            title = "বাংলা PDF ফন্ট নির্বাচন",
            fonts = BENGALI_PDF_FONT_LIST,
            selectedId = config.bnFontId,
            onSelect = {
                config = config.copy(bnFontId = it)
                showBnFontPicker = false
            },
            onDismiss = { showBnFontPicker = false }
        )
    }

    if (showEnFontPicker) {
        TestimonialFontPickerDialog(
            title = "ইংরেজি PDF ফন্ট নির্বাচন",
            fonts = ENGLISH_PDF_FONT_LIST,
            selectedId = config.enFontId,
            onSelect = {
                config = config.copy(enFontId = it)
                showEnFontPicker = false
            },
            onDismiss = { showEnFontPicker = false }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("সেটিংস রিসেট করবেন?", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = { Text("প্রশংসাপত্র মেকারের সকল কাস্টম সেটিংস ডিফল্ট অবস্থায় ফিরে যাবে।", fontSize = 12.5.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        TestimonialLocalStore.clearConfig(ctx)
                        config = defaultConfig
                        rollFromText = ""
                        rollToText = ""
                        showResetConfirm = false
                        Toast.makeText(ctx, "ডিফল্ট সেটিংস রিসেট হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) { Text("রিসেট করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("বাতিল") }
            }
        )
    }

    pendingExportAction?.let { action ->
        val actionTitle = if (action == TestimonialExportAction.PDF) "PDF সংরক্ষণ" else "প্রিন্ট করুন"
        val classSummary = config.resolvedSelectedClasses.joinToString(", ") { classShortBn(it, settings) }
        AlertDialog(
            onDismissRequest = { pendingExportAction = null },
            title = { Text("$actionTitle — ${TestimonialPlanner.resolvedBannerTitle(config)}", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "শ্রেণি: $classSummary\nমোট পৃষ্ঠা: ${bn(previewPages.size.toString())}টি (প্রতি পাতায় ১টি করে প্রশংসাপত্র ও মুড়ি অংশ)",
                    fontSize = 12.5.sp,
                    color = SlateText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingExportAction = null
                        val titleSlug = TestimonialPlanner.resolvedBannerTitle(config)
                        val fileName = "${titleSlug}_${classSummary.replace(", ", "_")}.pdf"
                        if (action == TestimonialExportAction.PDF) {
                            onExportPdf(previewPages, signatureBitmap, fileName)
                        } else {
                            onPrint(previewPages, signatureBitmap, titleSlug)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text(actionTitle)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingExportAction = null }) { Text("বাতিল") }
            }
        )
    }
}
