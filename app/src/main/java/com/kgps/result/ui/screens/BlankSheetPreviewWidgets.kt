package com.kgps.result.ui.screens

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kgps.result.BlankSheetOrientation
import com.kgps.result.BlankSheetPageSpec
import com.kgps.result.BlankSheetPdfGen
import com.kgps.result.Settings
import com.kgps.result.bn
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.TealPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

private fun clampBlankFullscreenPan(offset: Offset, scale: Float, size: IntSize): Offset {
    if (scale <= 1.02f || size.width <= 0 || size.height <= 0) return Offset.Zero
    val maxX = (size.width * (scale - 1f)) / 2f
    val maxY = (size.height * (scale - 1f)) / 2f + size.height * 0.22f
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY)
    )
}

@Composable
internal fun BlankSheetRenderedPage(
    ctx: Context,
    page: BlankSheetPageSpec,
    settings: Settings,
    orientation: BlankSheetOrientation,
    renderScale: Float,
    modifier: Modifier = Modifier
) {
    val bmp by produceState<Bitmap?>(
        initialValue = null,
        key1 = page,
        key2 = settings,
        key3 = orientation
    ) {
        value = withContext(Dispatchers.Default) {
            BlankSheetPdfGen.renderPageBitmap(
                ctx = ctx,
                page = page,
                settings = settings,
                orientation = orientation,
                scale = renderScale
            )
        }
    }

    val targetRatio = if (orientation == BlankSheetOrientation.LANDSCAPE) {
        842f / 595f
    } else {
        595f / 842f
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
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
                    contentDescription = "ফাঁকা শিট প্রিভিউ পৃষ্ঠা ${bn(page.globalPageNumber.toString())}",
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
internal fun BlankSheetFullscreenDialog(
    ctx: Context,
    pages: List<BlankSheetPageSpec>,
    initialPageIndex: Int,
    settings: Settings,
    orientation: BlankSheetOrientation,
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
                        .testTag("blank_fullscreen_pager")
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
                                            fsPan = clampBlankFullscreenPan(fsPan + panChange, nextScale, fsSize)
                                            if (pointerCount >= 2 || abs(zoomChange - 1f) > 0.01f || fsScale > 1.02f) {
                                                event.changes.forEach { it.consume() }
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        BlankSheetRenderedPage(
                            ctx = ctx,
                            page = pageSpec,
                            settings = settings,
                            orientation = orientation,
                            renderScale = 2.2f,
                            modifier = Modifier.graphicsLayer(
                                scaleX = fsScale,
                                scaleY = fsScale,
                                translationX = fsPan.x,
                                translationY = fsPan.y
                            )
                        )
                    }
                }

                val currentSpec = pages.getOrNull(fsPagerState.currentPage)
                val badgeText = buildString {
                    append("পৃষ্ঠা ${bn((fsPagerState.currentPage + 1).toString())}/${bn(pages.size.toString())}")
                    if (currentSpec != null) {
                        append(" · ${currentSpec.classTitleBn}")
                        if (currentSpec.subjectTitleBn.isNotBlank()) {
                            append(" (${currentSpec.subjectTitleBn})")
                        }
                    }
                    append(" · সোয়াইপ ও ডাবল ট্যাপে জুম")
                }

                // Top Header Bar (Page badge + Close button)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                            .testTag("blank_fullscreen_close_btn")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "ফুল স্ক্রিন বন্ধ করুন", tint = Color.White)
                    }
                }

                // Bottom Page Controls (when multiple pages)
                if (pages.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "আগের পৃষ্ঠা",
                                tint = if (fsPagerState.currentPage > 0) Color.White else Color.White.copy(alpha = 0.3f)
                            )
                        }
                        Text(
                            text = "${bn((fsPagerState.currentPage + 1).toString())} / ${bn(pages.size.toString())}",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(
                            onClick = {
                                scope.launch {
                                    fsPagerState.animateScrollToPage((fsPagerState.currentPage + 1).coerceAtMost(pages.lastIndex))
                                }
                            },
                            enabled = fsPagerState.currentPage < pages.lastIndex,
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "পরের পৃষ্ঠা",
                                tint = if (fsPagerState.currentPage < pages.lastIndex) Color.White else Color.White.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}
