package com.kgps.result.ui.screens

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.TestimonialCustomAssets
import com.kgps.result.TestimonialPageSpec
import com.kgps.result.TestimonialPdfGen
import com.kgps.result.bn
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.TealPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

private const val A4_LANDSCAPE_ASPECT_RATIO = 842f / 595f

@Composable
internal fun TestimonialRenderedPage(
    ctx: Context,
    page: TestimonialPageSpec,
    settings: Settings,
    students: List<Student>,
    signatureBitmap: Bitmap?,
    customAssets: TestimonialCustomAssets = TestimonialCustomAssets(),
    renderScale: Float,
    modifier: Modifier = Modifier,
    pageAlignment: Alignment = Alignment.Center
) {
    val bmp by produceState<Bitmap?>(
        initialValue = null,
        key1 = Triple(page, signatureBitmap, customAssets),
        key2 = settings,
        key3 = students
    ) {
        value = withContext(Dispatchers.Default) {
            TestimonialPdfGen.renderPageBitmap(
                ctx = ctx,
                page = page,
                settings = settings,
                students = students,
                signatureBitmap = signatureBitmap,
                customAssets = customAssets,
                scale = renderScale
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = pageAlignment
    ) {
        val availableRatio = if (maxHeight.value > 0f) maxWidth.value / maxHeight.value else A4_LANDSCAPE_ASPECT_RATIO
        val paperModifier = if (availableRatio > A4_LANDSCAPE_ASPECT_RATIO) {
            Modifier.fillMaxHeight().aspectRatio(A4_LANDSCAPE_ASPECT_RATIO)
        } else {
            Modifier.fillMaxWidth().aspectRatio(A4_LANDSCAPE_ASPECT_RATIO)
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
                    contentDescription = "প্রশংসাপত্র প্রিভিউ পৃষ্ঠা ${bn(page.pageNumber.toString())}",
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
internal fun TestimonialFullscreenDialog(
    ctx: Context,
    pages: List<TestimonialPageSpec>,
    initialPageIndex: Int,
    settings: Settings,
    students: List<Student>,
    signatureBitmap: Bitmap?,
    customAssets: TestimonialCustomAssets = TestimonialCustomAssets(),
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
                        .testTag("testimonial_fullscreen_pager")
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
                                            val nextScale = (fsScale * zoomChange).coerceIn(1f, 4f)
                                            fsScale = nextScale
                                            fsPan = clampFullscreenPanOffset(fsPan + panChange, nextScale, fsSize)
                                            if (pointerCount >= 2 || abs(zoomChange - 1f) > 0.01f || fsScale > 1.02f) {
                                                event.changes.forEach { it.consume() }
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        TestimonialRenderedPage(
                            ctx = ctx,
                            page = pageSpec,
                            settings = settings,
                            students = students,
                            signatureBitmap = signatureBitmap,
                            customAssets = customAssets,
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "পৃষ্ঠা ${bn((fsPagerState.currentPage + 1).toString())} / ${bn(pages.size.toString())} · জুম করতে পিন্চ বা ডাবল-ট্যাপ",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(48.dp).testTag("testimonial_fullscreen_close_btn")
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "প্রিভিউ বন্ধ করুন",
                                tint = Color.White
                            )
                        }
                    }
                }

                if (pages.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (fsPagerState.currentPage > 0) {
                                        scope.launch { fsPagerState.animateScrollToPage(fsPagerState.currentPage - 1) }
                                    }
                                },
                                enabled = fsPagerState.currentPage > 0,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                    contentDescription = "আগের পৃষ্ঠা",
                                    tint = Color.White
                                )
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        Surface(
                            color = Color.White.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (fsPagerState.currentPage < pages.lastIndex) {
                                        scope.launch { fsPagerState.animateScrollToPage(fsPagerState.currentPage + 1) }
                                    }
                                },
                                enabled = fsPagerState.currentPage < pages.lastIndex,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = "পরের পৃষ্ঠা",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
