@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class
)

package com.kgps.result.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.BlankBottomCodeStyle
import com.kgps.result.BlankNumeralStyle
import com.kgps.result.BlankSheetCatalog
import com.kgps.result.BlankSheetConfig
import com.kgps.result.BlankSheetLocalStore
import com.kgps.result.BlankSheetOrientation
import com.kgps.result.BlankSheetPageSpec
import com.kgps.result.BlankSheetPlanner
import com.kgps.result.BlankSheetRowFitMode
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.activeClassIds
import com.kgps.result.bn
import com.kgps.result.classNameBn
import com.kgps.result.classShortBn
import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.termNameBn
import com.kgps.result.ui.theme.RoseDanger
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

private data class BlankSheetBottomTabItem(
    val index: Int,
    val label: String,
    val panelTitle: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
)

private val BLANK_SHEET_BOTTOM_TABS = listOf(
    BlankSheetBottomTabItem(0, "শ্রেণি ও বিষয়", "শ্রেণি, বিষয় ও মূল্যায়ন", Icons.Rounded.MenuBook, Icons.Outlined.MenuBook),
    BlankSheetBottomTabItem(1, "কলাম ও ফর্ম", "টেমপ্লেট, কলাম প্রস্থ ও সাব-কলাম", Icons.Rounded.ViewColumn, Icons.Outlined.ViewColumn),
    BlankSheetBottomTabItem(2, "সারি ও অপশন", "সারি, অটোফিট, QR ও অপশন", Icons.Rounded.ViewAgenda, Icons.Outlined.ViewAgenda)
)

@Composable
fun BlankSheetPdfScreen(
    students: List<Student>,
    settings: Settings,
    permissions: TeacherPermissions,
    initialClass: Int,
    onExportPdf: (pages: List<BlankSheetPageSpec>, orientation: BlankSheetOrientation, fileName: String) -> Unit,
    onPrint: (pages: List<BlankSheetPageSpec>, orientation: BlankSheetOrientation, jobName: String) -> Unit,
    onCacheTemplates: (List<BlankSheetPageSpec>) -> Unit = {},
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val availableClassIds = remember(settings.customClasses) { activeClassIds(settings) }
    val allowedClasses = remember(permissions, availableClassIds) {
        permissions.effectiveClasses(availableClassIds)
    }

    val defaultConfig = remember(initialClass, allowedClasses) {
        val initCls = if (initialClass in allowedClasses) initialClass else allowedClasses.firstOrNull()
        BlankSheetConfig(
            selectedClass = initCls,
            selectedClasses = if (initCls != null) listOf(initCls) else emptyList(),
            subjectKey = "BAN",
            term = 1,
            columns = BlankSheetCatalog.DEFAULT_COLUMNS,
            rowsPerPage = 20,
            orientation = BlankSheetOrientation.PORTRAIT,
            numeralStyle = BlankNumeralStyle.ENGLISH
        )
    }

    var config by remember(defaultConfig) {
        val loaded = BlankSheetLocalStore.loadConfig(ctx, defaultConfig)
        mutableStateOf(
            loaded.copy(
                selectedClass = defaultConfig.selectedClass,
                selectedClasses = defaultConfig.selectedClasses
            )
        )
    }
    LaunchedEffect(config) {
        BlankSheetLocalStore.saveConfig(ctx, config)
    }

    var lastEnabledTopQr by remember { mutableStateOf(config.includeQrCode) }
    var lastEnabledBottomCode by remember { mutableStateOf(config.includeBarcode) }
    LaunchedEffect(config.includeQrCode, config.includeBarcode) {
        if (config.includeQrCode || config.includeBarcode) {
            lastEnabledTopQr = config.includeQrCode
            lastEnabledBottomCode = config.includeBarcode
        }
    }

    var openTab by remember { mutableStateOf<Int?>(null) }
    var showFullscreen by remember { mutableStateOf(false) }
    var pendingExportAction by remember { mutableStateOf<AdmitExportAction?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var qrMenuExpanded by remember { mutableStateOf(false) }
    var panelHeightFraction by remember { mutableFloatStateOf(0.50f) }
    var isCustomPanelHeight by remember { mutableStateOf(false) }
    var previewViewportHeightPx by remember { mutableIntStateOf(0) }
    var previewZoomScale by remember { mutableFloatStateOf(1f) }
    var previewPanOffset by remember { mutableStateOf(Offset.Zero) }
    var previewPageBoxSize by remember { mutableStateOf(IntSize.Zero) }

    // Back priority: zoomed preview resets first -> open panel closes -> exit screen
    if (onBack != null) {
        BackHandler(enabled = !showFullscreen && openTab == null && previewZoomScale <= 1.02f) { onBack() }
    }
    BackHandler(enabled = !showFullscreen && openTab != null && previewZoomScale <= 1.02f) {
        openTab = null
    }
    BackHandler(enabled = !showFullscreen && previewZoomScale > 1.02f) {
        previewZoomScale = 1f
        previewPanOffset = Offset.Zero
    }

    val pages = remember(students, settings, config, allowedClasses) {
        BlankSheetPlanner.buildPages(
            students = students,
            settings = settings,
            config = config,
            allowedClassIds = allowedClasses
        )
    }

    LaunchedEffect(pages) {
        if (pages.isNotEmpty()) {
            onCacheTemplates(pages)
        }
    }

    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })
    LaunchedEffect(pages.size) {
        if (pages.isNotEmpty() && pagerState.currentPage >= pages.size) {
            pagerState.scrollToPage((pages.size - 1).coerceAtLeast(0))
        }
    }

    val countByClass = remember(students, allowedClasses) {
        allowedClasses.associateWith { c -> students.count { it.cls == c } }
    }

    val isThreeTermMarks = remember(config.columns) {
        BlankSheetCatalog.isThreeTermMarksActive(config.columns)
    }
    val hasThreeTermTotal = remember(config.columns) {
        BlankSheetCatalog.hasThreeTermTotals(config.columns)
    }
    val isThreeTermTraits = remember(config.columns) {
        BlankSheetCatalog.isThreeTermAttendanceTraitsActive(config.columns)
    }
    val isSingleTermTraits = remember(config.columns) {
        BlankSheetCatalog.isSingleTermAttendanceTraitsActive(config.columns)
    }
    val isClassLevelTraitsOnly = remember(isThreeTermTraits, isSingleTermTraits, isThreeTermMarks, config.columns) {
        (isThreeTermTraits || isSingleTermTraits) && !isThreeTermMarks && config.columns.none { it.compKey != null }
    }
    val hasPerRowDaysCol = remember(config.columns) {
        config.columns.any { it.id in setOf("T1_DAYS", "T2_DAYS", "T3_DAYS") }
    }
    val hasSingleAttCol = remember(config.columns) {
        config.columns.any { it.id == "ATT" } || BlankSheetCatalog.hasSingleTermTraitColumns(config.columns)
    }
    val sharedWorkingDaysTerms = remember(isThreeTermTraits, isSingleTermTraits, hasPerRowDaysCol, hasSingleAttCol, config.term) {
        when {
            isThreeTermTraits && !hasPerRowDaysCol -> listOf(1, 2, 3)
            !isThreeTermTraits && hasSingleAttCol && config.term in 1..3 -> listOf(config.term)
            else -> emptyList()
        }
    }
    val commonWorkingDays = remember(students, config.primarySelectedClass) {
        BlankSheetCatalog.resolveCommonWorkingDaysByTerm(students, config.primarySelectedClass)
    }

    val selectedClassLabel = remember(config.selectedClass, config.selectedClasses, settings) {
        when {
            config.selectedClasses.size > 1 -> "${bn(config.selectedClasses.size.toString())}টি শ্রেণি"
            config.primarySelectedClass != null -> "${classNameBn(config.primarySelectedClass!!, settings)} শ্রেণি"
            else -> "সব শ্রেণি"
        }
    }

    val selectedSubjectLabel = remember(config.subjectKey, config.customSubjectTitle, isClassLevelTraitsOnly) {
        if (isClassLevelTraitsOnly) {
            "উপস্থিতি ও গুণাবলি"
        } else {
            BlankSheetCatalog.subjectTitleFor(config.subjectKey, config.customSubjectTitle)
                .ifBlank { "সাধারণ" }
        }
    }

    val selectedTermLabel = remember(config.term, config.customTermTitle, isThreeTermMarks, isThreeTermTraits) {
        if (isThreeTermMarks || isThreeTermTraits) {
            "৩ প্রান্তিক"
        } else {
            when (config.term) {
                1, 2, 3 -> termNameBn(config.term)
                0 -> "বার্ষিক"
                -1 -> config.customTermTitle.ifBlank { "শিরোনামহীন" }
                else -> "শিরোনামহীন"
            }
        }
    }

    val activePage = pages.getOrNull(pagerState.currentPage)
    val summaryLine = buildString {
        val clsShort = when {
            config.selectedClasses.size > 1 -> "${bn(config.selectedClasses.size.toString())} শ্রেণি"
            config.primarySelectedClass != null -> classShortBn(config.primarySelectedClass!!, settings)
            else -> "সব শ্রেণি"
        }
        append(clsShort)
        append(" · ")
        append(activePage?.subjectTitleBn?.ifBlank { selectedSubjectLabel } ?: selectedSubjectLabel)
        append(" · ")
        if (config.rowFitMode == BlankSheetRowFitMode.AUTO_FIT_PAGES) {
            append("${bn(config.autoFitPageCount.coerceIn(1, 3).toString())} পেজ ফিট")
        } else {
            append("${bn(config.rowsPerPage.toString())} সারি/পেজ")
        }
        append(" · ")
        append(if (config.includeQrCode || config.includeBarcode) "QR চালু" else "QR বন্ধ")
    }

    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxPanelHeight = (maxHeight * 0.90f).coerceAtLeast(240.dp)
        val overlayTargetHeight = if (previewViewportHeightPx > 0) {
            with(density) { (previewViewportHeightPx * panelHeightFraction).toDp() }
                .coerceIn(170.dp, maxPanelHeight)
        } else {
            (maxHeight * panelHeightFraction).coerceIn(170.dp, maxPanelHeight)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Compact Top Status, Quick QR Toggle & Export Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Surface(color = TealSoft, shape = RoundedCornerShape(7.dp)) {
                        Icon(
                            Icons.Rounded.TableChart,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.padding(5.dp).size(15.dp)
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (pages.isEmpty()) "লাইভ প্রিভিউ" else "পৃষ্ঠা ${bn((pagerState.currentPage + 1).toString())}/${bn(pages.size.toString())}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = SlateText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = summaryLine,
                            fontSize = 9.5.sp,
                            color = SlateMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Quick QR On/Off Tap + Light Long-Press Options Menu
                    val haptic = LocalHapticFeedback.current
                    val baseViewConfig = LocalViewConfiguration.current
                    val lightLongPressConfig = remember(baseViewConfig) {
                        object : ViewConfiguration by baseViewConfig {
                            override val longPressTimeoutMillis: Long get() = 260L
                        }
                    }
                    val anyQrEnabled = config.includeQrCode || config.includeBarcode
                    Box {
                        CompositionLocalProvider(LocalViewConfiguration provides lightLongPressConfig) {
                            Surface(
                                color = if (anyQrEnabled) TealSoft else Color.Transparent,
                                border = if (anyQrEnabled) BorderStroke(1.dp, TealPrimary.copy(alpha = 0.4f)) else null,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .combinedClickable(
                                        onClick = {
                                            if (anyQrEnabled) {
                                                lastEnabledTopQr = config.includeQrCode
                                                lastEnabledBottomCode = config.includeBarcode
                                                config = config.copy(includeQrCode = false, includeBarcode = false)
                                                Toast.makeText(
                                                    ctx,
                                                    "চালু থাকা QR/কোড বন্ধ হয়েছে (চাপ দিয়ে রাখলে অপশন)",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                val restoreTop = if (lastEnabledTopQr || lastEnabledBottomCode) {
                                                    lastEnabledTopQr
                                                } else {
                                                    true
                                                }
                                                val restoreBottom = lastEnabledBottomCode
                                                config = config.copy(
                                                    includeQrCode = restoreTop,
                                                    includeBarcode = restoreBottom
                                                )
                                                Toast.makeText(
                                                    ctx,
                                                    "QR/কোড চালু করা হয়েছে",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            qrMenuExpanded = true
                                        }
                                    )
                                    .testTag("blank_quick_qr_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.QrCode2,
                                        contentDescription = if (anyQrEnabled) "QR কোড বন্ধ/অপশন" else "QR কোড চালু/অপশন",
                                        tint = if (anyQrEnabled) TealPrimary else SlateMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = qrMenuExpanded,
                            onDismissRequest = { qrMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "উপরের স্মার্ট QR",
                                        fontSize = 12.5.sp,
                                        fontWeight = if (config.includeQrCode) FontWeight.Bold else FontWeight.Normal,
                                        color = if (config.includeQrCode) TealDark else SlateText
                                    )
                                },
                                trailingIcon = {
                                    if (config.includeQrCode) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                                    }
                                },
                                onClick = {
                                    config = config.copy(includeQrCode = !config.includeQrCode)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "নিচের কোড (${config.bottomCodeStyle.labelBn})",
                                        fontSize = 12.5.sp,
                                        fontWeight = if (config.includeBarcode) FontWeight.Bold else FontWeight.Normal,
                                        color = if (config.includeBarcode) TealDark else SlateText
                                    )
                                },
                                trailingIcon = {
                                    if (config.includeBarcode) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                                    }
                                },
                                onClick = {
                                    config = config.copy(includeBarcode = !config.includeBarcode)
                                }
                            )
                            if (config.includeBarcode) {
                                HorizontalDivider(color = SlateOutline.copy(alpha = 0.4f))
                                DropdownMenuItem(
                                    text = {
                                        val nextStyle = if (config.bottomCodeStyle == BlankBottomCodeStyle.THIN_QR) {
                                            BlankBottomCodeStyle.CODE_128
                                        } else {
                                            BlankBottomCodeStyle.THIN_QR
                                        }
                                        Text(
                                            "স্টাইল বদলান: ${nextStyle.labelBn}",
                                            fontSize = 11.5.sp,
                                            color = TealPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    onClick = {
                                        val nextStyle = if (config.bottomCodeStyle == BlankBottomCodeStyle.THIN_QR) {
                                            BlankBottomCodeStyle.CODE_128
                                        } else {
                                            BlankBottomCodeStyle.THIN_QR
                                        }
                                        config = config.copy(bottomCodeStyle = nextStyle)
                                    }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            val promptText = BlankSheetCatalog.buildExternalAiCsvPrompt(config, settings, allowedClasses)
                            clipboard.setText(AnnotatedString(promptText))
                            Toast.makeText(
                                ctx,
                                "এই ফর্মের AI প্রম্পট কপি হয়েছে — বাইরের AI-তে ছবি/PDF-সহ দিন",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        enabled = pages.isNotEmpty(),
                        modifier = Modifier.size(38.dp).testTag("blank_sheet_copy_ai_prompt_btn")
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = "AI প্রম্পট কপি",
                            tint = if (pages.isNotEmpty()) TealPrimary else SlateMuted.copy(alpha = 0.45f),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = { pendingExportAction = AdmitExportAction.PRINT },
                        enabled = allowedClasses.isNotEmpty(),
                        modifier = Modifier.size(38.dp).testTag("blank_sheet_print_btn")
                    ) {
                        Icon(
                            Icons.Rounded.Print,
                            contentDescription = "প্রিন্ট করুন",
                            tint = if (allowedClasses.isNotEmpty()) TealPrimary else SlateMuted.copy(alpha = 0.45f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Button(
                        onClick = { pendingExportAction = AdmitExportAction.PDF },
                        enabled = allowedClasses.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp).testTag("blank_sheet_export_pdf_btn")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("PDF", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                    }
                }
            }

            // 2. Main Preview Viewport + Floating Bottom Overlay Settings Panel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onSizeChanged { previewViewportHeightPx = it.height }
            ) {
                // Full-Size Live Preview (never shrinks when overlay panel opens)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (pages.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, SlateOutline),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    "নির্বাচিত শ্রেণিতে কোনো পৃষ্ঠা তৈরি হয়নি।",
                                    fontSize = 12.5.sp,
                                    color = SlateMuted,
                                    modifier = Modifier.padding(18.dp)
                                )
                            }
                        }
                    } else {
                        HorizontalPager(
                            state = pagerState,
                            userScrollEnabled = previewZoomScale <= 1.02f,
                            pageSpacing = 12.dp,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("blank_preview_pager")
                        ) { pageIdx ->
                            val pageSpec = pages[pageIdx]
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clipToBounds()
                                    .onSizeChanged { previewPageBoxSize = it }
                                    .padding(horizontal = 4.dp)
                                    .pointerInput(pageIdx) {
                                        val touchSlop = viewConfiguration.touchSlop
                                        awaitEachGesture {
                                            val firstDown = awaitFirstDown(requireUnconsumed = false)
                                            var wasMultiTouch = false
                                            var movedBeyondSlop = false
                                            var upTimeMillis = firstDown.uptimeMillis
                                            var lastPointerWasUp = false
                                            do {
                                                val event = awaitPointerEvent()
                                                val pointerCount = event.changes.count { it.pressed }
                                                if (pointerCount >= 2) wasMultiTouch = true
                                                event.changes.firstOrNull { it.id == firstDown.id }?.let { ch ->
                                                    if ((ch.position - firstDown.position).getDistance() > touchSlop) {
                                                        movedBeyondSlop = true
                                                    }
                                                    if (!ch.pressed) {
                                                        upTimeMillis = ch.uptimeMillis
                                                        lastPointerWasUp = true
                                                    }
                                                }
                                                val zoomChange = event.calculateZoom()
                                                val panChange = event.calculatePan()
                                                if (pointerCount >= 2 || previewZoomScale > 1.02f) {
                                                    val nextScale = (previewZoomScale * zoomChange).coerceIn(1f, 4.5f)
                                                    if (nextScale <= 1.02f) {
                                                        previewZoomScale = 1f
                                                        previewPanOffset = Offset.Zero
                                                    } else {
                                                        previewZoomScale = nextScale
                                                        val w = previewPageBoxSize.width.toFloat().coerceAtLeast(1f)
                                                        val h = previewPageBoxSize.height.toFloat().coerceAtLeast(1f)
                                                        val maxX = (w * (nextScale - 1f)) / 2f + w * 0.15f
                                                        val maxY = (h * (nextScale - 1f)) / 2f + h * 0.48f
                                                        val nextOff = previewPanOffset + panChange
                                                        previewPanOffset = Offset(
                                                            x = nextOff.x.coerceIn(-maxX, maxX),
                                                            y = nextOff.y.coerceIn(-maxY, maxY)
                                                        )
                                                    }
                                                    if (pointerCount >= 2 || abs(zoomChange - 1f) > 0.01f || movedBeyondSlop) {
                                                        event.changes.forEach { it.consume() }
                                                    }
                                                }
                                            } while (event.changes.any { it.pressed })

                                            val tapDuration = upTimeMillis - firstDown.uptimeMillis
                                            if (!wasMultiTouch && !movedBeyondSlop && lastPointerWasUp && tapDuration < 450L) {
                                                showFullscreen = true
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                BlankSheetRenderedPage(
                                    ctx = ctx,
                                    page = pageSpec,
                                    settings = settings,
                                    orientation = config.orientation,
                                    renderScale = 2.2f,
                                    modifier = Modifier.graphicsLayer(
                                        scaleX = previewZoomScale,
                                        scaleY = previewZoomScale,
                                        translationX = previewPanOffset.x,
                                        translationY = previewPanOffset.y
                                    )
                                )
                                if (previewZoomScale > 1.02f) {
                                    Surface(
                                        onClick = {
                                            previewZoomScale = 1f
                                            previewPanOffset = Offset.Zero
                                        },
                                        color = Color(0xCC0F172A),
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .testTag("blank_zoom_reset_pill")
                                    ) {
                                        Text(
                                            text = "${bn(String.format(Locale.US, "%.1f", previewZoomScale))}× · রিসেট",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Compact Page Indicator & Gesture Hint Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (pages.size > 1) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                                        }
                                    },
                                    enabled = pagerState.currentPage > 0,
                                    modifier = Modifier.size(38.dp).testTag("blank_prev_page_btn")
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "আগের পৃষ্ঠা", modifier = Modifier.size(20.dp))
                                }
                            }
                            Text(
                                text = "ট্যাপে ফুল স্ক্রিন · ২ আঙুলে জুম · সোয়াইপে পৃষ্ঠা",
                                fontSize = 10.sp,
                                color = SlateMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            if (pages.size > 1) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(pages.lastIndex))
                                        }
                                    },
                                    enabled = pagerState.currentPage < pages.lastIndex,
                                    modifier = Modifier.size(38.dp).testTag("blank_next_page_btn")
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "পরের পৃষ্ঠা", modifier = Modifier.size(20.dp))
                                }
                            }
                            IconButton(
                                onClick = { showFullscreen = true },
                                modifier = Modifier.size(38.dp).testTag("blank_fullscreen_preview_btn")
                            ) {
                                Icon(Icons.Rounded.Fullscreen, contentDescription = "পূর্ণ স্ক্রিন প্রিভিউ", tint = TealPrimary, modifier = Modifier.size(19.dp))
                            }
                        }
                    }
                }

                // Overlay Slide-Up Compact Options Panel (floats over bottom portion of preview)
                androidx.compose.animation.AnimatedVisibility(
                    visible = openTab != null,
                    enter = slideInVertically(tween(220)) { it } + fadeIn(tween(180)),
                    exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(140)),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    val selectedTabIdx = openTab ?: 0
                    val tabMeta = BLANK_SHEET_BOTTOM_TABS[selectedTabIdx]
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                        border = BorderStroke(1.dp, SlateOutline),
                        tonalElevation = 3.dp,
                        shadowElevation = 12.dp,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .let { baseMod ->
                                if (isCustomPanelHeight) {
                                    baseMod.height(overlayTargetHeight)
                                } else {
                                    baseMod.heightIn(max = overlayTargetHeight)
                                }
                            }
                            .testTag("blank_slide_panel")
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            // Draggable top handle & title row
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(previewViewportHeightPx) {
                                        detectVerticalDragGestures(
                                            onDragStart = { isCustomPanelHeight = true },
                                            onVerticalDrag = { change, dragAmount ->
                                                change.consume()
                                                val denom = if (previewViewportHeightPx > 0) {
                                                    previewViewportHeightPx.toFloat()
                                                } else {
                                                    540f
                                                }
                                                panelHeightFraction = (panelHeightFraction - dragAmount / denom)
                                                    .coerceIn(0.18f, 0.94f)
                                            },
                                            onDragEnd = {
                                                if (panelHeightFraction < 0.24f) {
                                                    openTab = null
                                                    panelHeightFraction = 0.50f
                                                    isCustomPanelHeight = false
                                                }
                                            },
                                            onDragCancel = {
                                                if (panelHeightFraction < 0.24f) {
                                                    openTab = null
                                                    panelHeightFraction = 0.50f
                                                    isCustomPanelHeight = false
                                                }
                                            }
                                        )
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(38.dp)
                                            .height(4.dp)
                                            .background(SlateOutline, RoundedCornerShape(999.dp))
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 10.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = tabMeta.panelTitle,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = TealDark
                                        )
                                        Text(
                                            text = "উপরে টেনে বড় করুন · নিচে নামিয়ে ছোট করুন",
                                            fontSize = 8.5.sp,
                                            color = SlateMuted
                                        )
                                    }
                                    IconButton(
                                        onClick = { openTab = null },
                                        modifier = Modifier.size(36.dp).testTag("blank_panel_close_btn")
                                    ) {
                                        Icon(
                                            Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = "প্যানেল বন্ধ করুন",
                                            tint = SlateMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = SlateOutline.copy(alpha = 0.55f))

                            // Compact Scrollable Tab Body
                            androidx.compose.runtime.key(selectedTabIdx) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    when (selectedTabIdx) {
                                        0 -> BlankSheetClassSubjectTab(
                                            config = config,
                                            settings = settings,
                                            allowedClasses = allowedClasses,
                                            countByClass = countByClass,
                                            isThreeTermMarks = isThreeTermMarks,
                                            isThreeTermTraits = isThreeTermTraits,
                                            isClassLevelTraitsOnly = isClassLevelTraitsOnly,
                                            selectedSubjectLabel = selectedSubjectLabel,
                                            selectedTermLabel = selectedTermLabel,
                                            onUpdateConfig = { config = it },
                                            onResetPageIndex = {
                                                scope.launch { pagerState.scrollToPage(0) }
                                            }
                                        )

                                        1 -> BlankSheetColumnsTab(
                                            config = config,
                                            isThreeTermMarks = isThreeTermMarks,
                                            hasThreeTermTotal = hasThreeTermTotal,
                                            isThreeTermTraits = isThreeTermTraits,
                                            onUpdateConfig = { config = it },
                                            onResetPageIndex = {
                                                scope.launch { pagerState.scrollToPage(0) }
                                            }
                                        )

                                        else -> BlankSheetRowsAndOptionsTab(
                                            config = config,
                                            students = students,
                                            settings = settings,
                                            sharedWorkingDaysTerms = sharedWorkingDaysTerms,
                                            commonWorkingDays = commonWorkingDays,
                                            onUpdateConfig = { config = it },
                                            onResetPageIndex = {
                                                scope.launch { pagerState.scrollToPage(0) }
                                            },
                                            onRequestReset = { showResetConfirm = true }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Compact Fixed Bottom 3-Tab Dock Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BLANK_SHEET_BOTTOM_TABS.forEach { tab ->
                        val isSelected = openTab == tab.index
                        Surface(
                            onClick = {
                                openTab = if (isSelected) null else tab.index
                            },
                            color = if (isSelected) TealSoft else Color.Transparent,
                            border = if (isSelected) BorderStroke(1.dp, TealPrimary.copy(alpha = 0.45f)) else null,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 46.dp)
                                .testTag("blank_bottom_tab_${tab.index}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
                                    contentDescription = tab.label,
                                    tint = if (isSelected) TealDark else SlateMuted,
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = tab.label,
                                    fontSize = 9.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) TealDark else SlateMuted,
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

    if (showFullscreen && pages.isNotEmpty()) {
        BlankSheetFullscreenDialog(
            ctx = ctx,
            pages = pages,
            initialPageIndex = pagerState.currentPage,
            settings = settings,
            orientation = config.orientation,
            onPageChanged = { newPage ->
                scope.launch {
                    if (newPage in pages.indices && pagerState.currentPage != newPage) {
                        pagerState.scrollToPage(newPage)
                    }
                }
            },
            onDismiss = { showFullscreen = false }
        )
    }

    pendingExportAction?.let { exportAction ->
        BlankSheetMultiClassExportDialog(
            action = exportAction,
            allowedClasses = allowedClasses,
            countByClass = countByClass,
            settings = settings,
            students = students,
            config = config,
            subjectSummaryLabel = selectedSubjectLabel,
            onDismiss = { pendingExportAction = null },
            onConfirm = { chosenClasses, exportPages, fileNameOrJob ->
                pendingExportAction = null
                if (chosenClasses.isNotEmpty()) {
                    config = config.copy(
                        selectedClass = chosenClasses.first(),
                        selectedClasses = chosenClasses
                    )
                }
                if (exportAction == AdmitExportAction.PDF) {
                    onExportPdf(exportPages, config.orientation, fileNameOrJob)
                } else {
                    onPrint(exportPages, config.orientation, fileNameOrJob)
                }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("ডিফল্ট সেটিংস রিসেট করবেন?", fontWeight = FontWeight.Bold, fontSize = 15.5.sp) },
            text = {
                Text(
                    "ফাঁকা শিটের কলাম, সারি, অটোফিট ও QR/বারকোড সেটিংস ডিফল্ট অবস্থায় ফিরে যাবে।",
                    fontSize = 13.sp,
                    color = SlateText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        config = defaultConfig
                        BlankSheetLocalStore.saveConfig(ctx, defaultConfig)
                        previewZoomScale = 1f
                        previewPanOffset = Offset.Zero
                        scope.launch { pagerState.scrollToPage(0) }
                        Toast.makeText(ctx, "ফাঁকা সিট সেটিংস রিসেট হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("blank_sheet_reset_confirm_btn")
                ) {
                    Text("রিসেট করুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetConfirm = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("বাতিল")
                }
            }
        )
    }
}
