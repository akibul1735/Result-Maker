@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.kgps.result.ui.screens

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kgps.result.BENGALI_PDF_FONT_LIST
import com.kgps.result.ENGLISH_PDF_FONT_LIST
import com.kgps.result.PdfGen
import com.kgps.result.Settings
import com.kgps.result.TabulationConfig
import com.kgps.result.TabulationFinalDisplay
import com.kgps.result.TabulationHeaderStyle
import com.kgps.result.TabulationNumeralMode
import com.kgps.result.TabulationPageSize
import com.kgps.result.TabulationPageSpec
import com.kgps.result.TabulationPdfGen
import com.kgps.result.TabulationSortOrder
import com.kgps.result.TabulationSubjectDisplay
import com.kgps.result.TabulationTermScope
import com.kgps.result.bn
import com.kgps.result.classShortBn
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealContainer
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs

private val TAB_MARGIN_OPTIONS = listOf(0.20f, 0.25f, 0.30f, 0.35f, 0.40f, 0.50f, 0.60f)
private val TAB_ROWS_OPTIONS = listOf(12, 15, 18, 20, 22, 25, 28, 30, 35)

private fun formatTabInchesBn(inches: Float): String =
    bn(String.format(Locale.US, "%.2f", inches)) + "\""

private fun clampTabFullscreenPan(offset: Offset, scale: Float, size: IntSize): Offset {
    if (scale <= 1.02f || size.width <= 0 || size.height <= 0) return Offset.Zero
    val maxX = (size.width * (scale - 1f)) / 2f
    val maxY = (size.height * (scale - 1f)) / 2f + size.height * 0.22f
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY)
    )
}

@Composable
internal fun TabulationRenderedPage(
    ctx: Context,
    page: TabulationPageSpec,
    settings: Settings,
    pageSize: TabulationPageSize,
    renderScale: Float,
    modifier: Modifier = Modifier,
    pageAlignment: Alignment = Alignment.Center
) {
    val bmp by produceState<Bitmap?>(
        initialValue = null,
        key1 = page,
        key2 = settings
    ) {
        value = withContext(Dispatchers.Default) {
            TabulationPdfGen.renderPageBitmap(
                ctx = ctx,
                page = page,
                settings = settings,
                scale = renderScale
            )
        }
    }

    val targetRatio = pageSize.widthPt.toFloat() / pageSize.heightPt.toFloat()

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = pageAlignment
    ) {
        val availableRatio = if (maxHeight.value > 0f) maxWidth.value / maxHeight.value else targetRatio
        val paperModifier = if (availableRatio > targetRatio) {
            Modifier.fillMaxHeight().aspectRatio(targetRatio)
        } else {
            Modifier.fillMaxWidth().aspectRatio(targetRatio)
        }

        Surface(
            color = Color.White,
            border = BorderStroke(1.dp, SlateOutline.copy(alpha = 0.9f)),
            shadowElevation = 4.dp,
            shape = RoundedCornerShape(6.dp),
            modifier = paperModifier
        ) {
            val loaded = bmp
            if (loaded != null) {
                Image(
                    bitmap = loaded.asImageBitmap(),
                    contentDescription = "ফলাফল টেবিল প্রিভিউ পৃষ্ঠা ${bn(page.pageNumber.toString())}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TealPrimary, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
internal fun TabulationFullscreenDialog(
    ctx: Context,
    pages: List<TabulationPageSpec>,
    initialPageIndex: Int,
    settings: Settings,
    pageSize: TabulationPageSize,
    onPageChanged: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    if (pages.isEmpty()) return
    val scope = rememberCoroutineScope()
    val fsPagerState = rememberPagerState(
        initialPage = initialPageIndex.coerceIn(0, pages.lastIndex),
        pageCount = { pages.size }
    )
    var fsScale by remember { mutableFloatStateOf(1f) }
    var fsPan by remember { mutableStateOf(Offset.Zero) }
    var fsSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(fsPagerState.currentPage) {
        fsScale = 1f
        fsPan = Offset.Zero
        onPageChanged(fsPagerState.currentPage)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color(0xFF0F172A),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                HorizontalPager(
                    state = fsPagerState,
                    userScrollEnabled = fsScale <= 1.05f,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 60.dp, bottom = 52.dp, start = 10.dp, end = 10.dp)
                        .testTag("tabulation_fullscreen_pager")
                ) { pageIdx ->
                    val pageSpec = pages[pageIdx]
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .onSizeChanged { fsSize = it }
                            .pointerInput(pageIdx) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (fsScale > 1.05f) {
                                            fsScale = 1f
                                            fsPan = Offset.Zero
                                        } else {
                                            fsScale = 2.2f
                                        }
                                    }
                                )
                            }
                            .pointerInput(pageIdx) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    do {
                                        val event = awaitPointerEvent()
                                        val pointerCount = event.changes.count { it.pressed }
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        if (pointerCount >= 2 || fsScale > 1.02f) {
                                            val nextScale = (fsScale * zoomChange).coerceIn(1f, 4.5f)
                                            fsScale = nextScale
                                            fsPan = clampTabFullscreenPan(fsPan + panChange, nextScale, fsSize)
                                            if (pointerCount >= 2 || abs(zoomChange - 1f) > 0.01f) {
                                                event.changes.forEach { it.consume() }
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        TabulationRenderedPage(
                            ctx = ctx,
                            page = pageSpec,
                            settings = settings,
                            pageSize = pageSize,
                            renderScale = 2.6f,
                            modifier = Modifier.graphicsLayer(
                                scaleX = fsScale,
                                scaleY = fsScale,
                                translationX = fsPan.x,
                                translationY = fsPan.y
                            )
                        )
                    }
                }

                // Top info + close bar
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "পৃষ্ঠা ${bn((fsPagerState.currentPage + 1).toString())}/${bn(pages.size.toString())} · ডাবল ট্যাপ বা ২ আঙুলে জুম",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                            .testTag("tabulation_fullscreen_close_btn")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "বন্ধ করুন", tint = Color.White)
                    }
                }

                if (pages.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    fsPagerState.animateScrollToPage((fsPagerState.currentPage - 1).coerceAtLeast(0))
                                }
                            },
                            enabled = fsPagerState.currentPage > 0,
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(999.dp))
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "আগের পৃষ্ঠা", tint = Color.White)
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    fsPagerState.animateScrollToPage((fsPagerState.currentPage + 1).coerceAtMost(pages.lastIndex))
                                }
                            },
                            enabled = fsPagerState.currentPage < pages.lastIndex,
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(999.dp))
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "পরের পৃষ্ঠা", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TabulationUnifiedSettingsPanel(
    config: TabulationConfig,
    settings: Settings,
    allowedClasses: List<Int>,
    countByClass: Map<Int, Int>,
    activeBnFontName: String,
    activeEnFontName: String,
    onUpdateConfig: (TabulationConfig) -> Unit,
    onResetPageIndex: () -> Unit,
    onRequestReset: () -> Unit
) {
    val ctx = LocalContext.current
    var showMoreOptions by remember { mutableStateOf(false) }
    var rowsMenuOpen by remember { mutableStateOf(false) }

    val isSingleTermContWr = config.termScope != TabulationTermScope.FINAL_AVG &&
        config.subjectDisplay == TabulationSubjectDisplay.CONT_WR_TOT
    val hasSubHeaders = isSingleTermContWr ||
        (config.termScope == TabulationTermScope.FINAL_AVG && config.finalDisplay == TabulationFinalDisplay.TERMS_AND_AVG)

    val moreSummary = buildString {
        append(activeBnFontName)
        append(" · ")
        append(config.numeralMode.labelBn)
        append(" · মার্জিন ")
        append(formatTabInchesBn(config.safeMarginInches))
    }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        // 1. Single-row horizontal swipeable Class chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "শ্রেণি:",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = SlateMuted,
                modifier = Modifier.padding(end = 2.dp)
            )
            allowedClasses.forEach { cId ->
                val isSel = config.selectedClass == cId
                val cnt = countByClass[cId] ?: 0
                Surface(
                    onClick = {
                        onUpdateConfig(config.copy(selectedClass = cId))
                        onResetPageIndex()
                    },
                    color = if (isSel) TealPrimary else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (isSel) TealPrimary else SlateOutline),
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier.heightIn(min = 28.dp).testTag("tabulation_cls_$cId")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "${classShortBn(cId, settings)} (${bn(cnt.toString())})",
                            fontSize = 10.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) Color.White else SlateText,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 2. Term Scope segmented bar (১ম / ২য় / ৩য় / চূড়ান্ত)
        AdmitSegmentedOptionBar(
            items = TabulationTermScope.entries.map { it to it.shortLabelBn },
            selected = config.termScope,
            onSelect = { scope ->
                val nextPage = if (scope == TabulationTermScope.FINAL_AVG &&
                    config.finalDisplay == TabulationFinalDisplay.TERMS_AND_AVG &&
                    config.pageSize == TabulationPageSize.A4_PORTRAIT
                ) {
                    TabulationPageSize.LEGAL_LANDSCAPE
                } else {
                    config.pageSize
                }
                onUpdateConfig(config.copy(termScope = scope, pageSize = nextPage))
                onResetPageIndex()
            },
            tagPrefix = "tabulation_term"
        )

        // 3. Subject Display + Sort Order side-by-side (or Final Avg hint + Sort Order)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (config.termScope != TabulationTermScope.FINAL_AVG) {
                AdmitSegmentedOptionBar(
                    items = TabulationSubjectDisplay.entries.map { it to it.labelBn },
                    selected = config.subjectDisplay,
                    onSelect = { disp ->
                        onUpdateConfig(config.copy(subjectDisplay = disp))
                    },
                    modifier = Modifier.weight(1.05f),
                    tagPrefix = "tabulation_subj_disp"
                )
            } else {
                AdmitSegmentedOptionBar(
                    items = TabulationFinalDisplay.entries.map { it to it.labelBn },
                    selected = config.finalDisplay,
                    onSelect = { fd ->
                        val nextPage = if (fd == TabulationFinalDisplay.TERMS_AND_AVG &&
                            config.pageSize == TabulationPageSize.A4_PORTRAIT
                        ) {
                            TabulationPageSize.LEGAL_LANDSCAPE
                        } else {
                            config.pageSize
                        }
                        onUpdateConfig(config.copy(finalDisplay = fd, pageSize = nextPage))
                    },
                    modifier = Modifier.weight(1.1f),
                    tagPrefix = "tabulation_final_disp"
                )
            }
            AdmitSegmentedOptionBar(
                items = listOf(
                    TabulationSortOrder.ROLL_ASC to "রোল ক্রম",
                    TabulationSortOrder.MERIT_ASC to "মেধা ক্রম"
                ),
                selected = config.sortOrder,
                onSelect = { order ->
                    onUpdateConfig(config.copy(sortOrder = order))
                    onResetPageIndex()
                },
                modifier = Modifier.weight(0.95f),
                tagPrefix = "tabulation_sort"
            )
        }

        // 4. Page Size segmented bar + Rows per page compact stepper
        AdmitSegmentedOptionBar(
            items = TabulationPageSize.entries.map { it to it.labelBn },
            selected = config.pageSize,
            onSelect = { ps ->
                onUpdateConfig(config.copy(pageSize = ps))
                onResetPageIndex()
            },
            tagPrefix = "tabulation_pagesize"
        )

        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    "প্রতি পৃষ্ঠায় শিক্ষার্থী",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = SlateText,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    onClick = {
                        onUpdateConfig(config.copy(rowsPerPage = (config.safeRowsPerPage - 1).coerceAtLeast(12)))
                        onResetPageIndex()
                    },
                    shape = RoundedCornerShape(6.dp),
                    color = SlateBg,
                    border = BorderStroke(1.dp, SlateOutline),
                    modifier = Modifier.size(30.dp).testTag("tabulation_rows_dec_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Remove, contentDescription = "কমান", modifier = Modifier.size(15.dp), tint = SlateText)
                    }
                }
                Box {
                    Surface(
                        onClick = { rowsMenuOpen = true },
                        color = TealSoft,
                        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .padding(horizontal = 5.dp)
                            .height(30.dp)
                            .testTag("tabulation_rows_picker")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                "${bn(config.safeRowsPerPage.toString())} জন",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealDark
                            )
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(15.dp))
                        }
                    }
                    DropdownMenu(
                        expanded = rowsMenuOpen,
                        onDismissRequest = { rowsMenuOpen = false }
                    ) {
                        TAB_ROWS_OPTIONS.forEach { count ->
                            val isSel = config.safeRowsPerPage == count
                            DropdownMenuItem(
                                modifier = Modifier.background(if (isSel) TealContainer else Color.Transparent),
                                text = {
                                    Text(
                                        "প্রতি পৃষ্ঠায় ${bn(count.toString())} জন",
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) TealDark else SlateText
                                    )
                                },
                                onClick = {
                                    rowsMenuOpen = false
                                    onUpdateConfig(config.copy(rowsPerPage = count))
                                    onResetPageIndex()
                                }
                            )
                        }
                    }
                }
                Surface(
                    onClick = {
                        onUpdateConfig(config.copy(rowsPerPage = (config.safeRowsPerPage + 1).coerceAtMost(36)))
                        onResetPageIndex()
                    },
                    shape = RoundedCornerShape(6.dp),
                    color = SlateBg,
                    border = BorderStroke(1.dp, SlateOutline),
                    modifier = Modifier.size(30.dp).testTag("tabulation_rows_inc_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Add, contentDescription = "বাড়ান", modifier = Modifier.size(15.dp), tint = SlateText)
                    }
                }
            }
        }

        // 5. Single-row horizontal swipeable Right-side Result Columns
        val resultColToggles = listOf(
            Triple("মোট নম্বর", config.showTotal, "tab_col_total") to { v: Boolean -> config.copy(showTotal = v) },
            Triple("সর্বোচ্চ", config.showHighest, "tab_col_highest") to { v: Boolean -> config.copy(showHighest = v) },
            Triple("শতকরা (%)", config.showPercentage, "tab_col_pct") to { v: Boolean -> config.copy(showPercentage = v) },
            Triple("গ্রেড", config.showGrade, "tab_col_grade") to { v: Boolean -> config.copy(showGrade = v) },
            Triple("মেধাক্রম", config.showMeritRank, "tab_col_merit") to { v: Boolean -> config.copy(showMeritRank = v) }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "কলাম:",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = SlateMuted,
                modifier = Modifier.padding(end = 2.dp)
            )
            resultColToggles.forEach { (meta, updater) ->
                val (label, checked, tag) = meta
                Surface(
                    onClick = { onUpdateConfig(updater(!checked)) },
                    shape = RoundedCornerShape(6.dp),
                    color = if (checked) TealPrimary else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (checked) TealPrimary else SlateOutline),
                    modifier = Modifier.heightIn(min = 27.dp).testTag(tag)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        if (checked) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                        }
                        Text(
                            label,
                            fontSize = 9.8.sp,
                            fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
                            color = if (checked) Color.White else SlateText,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 6. "More" toggle button for less-frequently used settings
        Surface(
            onClick = { showMoreOptions = !showMoreOptions },
            color = if (showMoreOptions) TealSoft else SlateBg.copy(alpha = 0.75f),
            border = BorderStroke(
                1.dp,
                if (showMoreOptions) TealPrimary.copy(alpha = 0.45f) else SlateOutline
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 38.dp)
                .testTag("tabulation_more_options_btn")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = TealDark,
                    modifier = Modifier.size(15.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "আরও অপশন (More)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealDark
                    )
                    if (!showMoreOptions) {
                        Text(
                            text = moreSummary,
                            fontSize = 9.sp,
                            color = SlateMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = if (showMoreOptions) "কম" else "খুলুন",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TealDark
                )
                Icon(
                    imageVector = if (showMoreOptions) {
                        Icons.Rounded.KeyboardArrowUp
                    } else {
                        Icons.Rounded.KeyboardArrowDown
                    },
                    contentDescription = if (showMoreOptions) "কম দেখুন" else "আরও অপশন দেখুন",
                    tint = TealDark,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 7. Collapsible Secondary / Less-Used Options
        AnimatedVisibility(visible = showMoreOptions) {
            Surface(
                color = SlateBg.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, SlateOutline.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(9.dp),
                modifier = Modifier.fillMaxWidth().testTag("tabulation_more_options_container")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (hasSubHeaders) {
                        val hdrItems = if (config.termScope == TabulationTermScope.FINAL_AVG) {
                            listOf(
                                TabulationHeaderStyle.SHORT_SUB to "সংক্ষিপ্ত (১ম/২য়/৩য়/গড়)",
                                TabulationHeaderStyle.VERTICAL_SUB to "লম্বা ৯০° (প্রান্তিক)"
                            )
                        } else {
                            TabulationHeaderStyle.entries.map { it to it.labelBn }
                        }
                        AdmitSegmentedOptionBar(
                            items = hdrItems,
                            selected = config.headerStyle,
                            onSelect = { onUpdateConfig(config.copy(headerStyle = it)) },
                            tagPrefix = "tabulation_hdr_style"
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AdmitCompactPickerBox(
                            label = "মার্জিন",
                            valueText = formatTabInchesBn(config.safeMarginInches),
                            options = TAB_MARGIN_OPTIONS,
                            selectedValue = config.safeMarginInches,
                            optionLabel = { formatTabInchesBn(it) },
                            onSelect = { onUpdateConfig(config.copy(marginInches = it)) },
                            modifier = Modifier.weight(0.8f),
                            testTag = "tabulation_margin_picker"
                        )
                        AdmitCompactPickerBox(
                            label = "বাংলা ফন্ট",
                            valueText = activeBnFontName,
                            options = BENGALI_PDF_FONT_LIST.map { it.id },
                            selectedValue = config.bnFontId,
                            optionLabel = { id ->
                                BENGALI_PDF_FONT_LIST.find { it.id == id }?.labelBn?.substringBefore(" (") ?: id
                            },
                            onSelect = { onUpdateConfig(config.copy(bnFontId = it)) },
                            isFontDropdown = true,
                            modifier = Modifier.weight(1.1f),
                            testTag = "tabulation_bn_font",
                            optionFontFamily = { id ->
                                FontFamily(androidx.compose.ui.text.font.Typeface(PdfGen.resolveTypeface(ctx, id, false)))
                            }
                        )
                        AdmitCompactPickerBox(
                            label = "ইংরেজি ফন্ট",
                            valueText = activeEnFontName,
                            options = ENGLISH_PDF_FONT_LIST.map { it.id },
                            selectedValue = config.enFontId,
                            optionLabel = { id ->
                                ENGLISH_PDF_FONT_LIST.find { it.id == id }?.labelBn?.substringBefore(" (") ?: id
                            },
                            onSelect = { onUpdateConfig(config.copy(enFontId = it)) },
                            isFontDropdown = true,
                            modifier = Modifier.weight(1.1f),
                            testTag = "tabulation_en_font",
                            optionFontFamily = { id ->
                                FontFamily(androidx.compose.ui.text.font.Typeface(PdfGen.resolveTypeface(ctx, id, false)))
                            }
                        )
                    }

                    AdmitSegmentedOptionBar(
                        items = TabulationNumeralMode.entries.map { it to it.labelBn },
                        selected = config.numeralMode,
                        onSelect = { onUpdateConfig(config.copy(numeralMode = it)) },
                        tagPrefix = "tabulation_numeral"
                    )

                    if (isSingleTermContWr) {
                        AdmitCompactSwitchRow(
                            label = "স্বাস্থ্য ও শিল্প বিষয়ে শুধু মোট দেখাও",
                            checked = config.skipWrittenOnHalfSubjects,
                            onCheckedChange = { onUpdateConfig(config.copy(skipWrittenOnHalfSubjects = it)) },
                            testTag = "tab_opt_half_subj"
                        )
                    }
                    AdmitCompactSwitchRow(
                        label = "শেষ পৃষ্ঠায় পাসের হার ও সারাংশ দেখাও",
                        checked = config.showSummaryFooter,
                        onCheckedChange = { onUpdateConfig(config.copy(showSummaryFooter = it)) },
                        testTag = "tab_opt_footer"
                    )

                    AdmitCompactTextInput(
                        label = "কাস্টম শিরোনাম (ঐচ্ছিক)",
                        value = config.customTitle,
                        onValueChange = { onUpdateConfig(config.copy(customTitle = it)) },
                        placeholder = "যেমন: ৩য় প্রান্তিক মূল্যায়ন ফলাফল - ২০২৬",
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "tabulation_custom_title"
                    )

                    HorizontalDivider(color = SlateOutline.copy(alpha = 0.35f))
                    OutlinedButton(
                        onClick = onRequestReset,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SlateOutline),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 38.dp).testTag("tabulation_more_reset_btn")
                    ) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("ডিফল্ট সেটিংস রিসেট করুন", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SlateText)
                    }
                }
            }
        }
    }
}
