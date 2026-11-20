package com.kgps.result.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kgps.result.ui.theme.TealPrimary
import kotlin.math.hypot
import kotlin.math.min

private enum class DragHandle { NONE, TL, TR, BL, BR, MOVE }

@Composable
fun ImageCropDialog(
    bitmap: Bitmap,
    canRestoreOriginal: Boolean,
    onRotate90: () -> Unit,
    onRestoreOriginal: () -> Unit,
    onApplyCrop: (leftFrac: Float, topFrac: Float, rightFrac: Float, bottomFrac: Float) -> Unit,
    onDismiss: () -> Unit
) {
    var leftFrac by remember(bitmap) { mutableFloatStateOf(0.03f) }
    var topFrac by remember(bitmap) { mutableFloatStateOf(0.03f) }
    var rightFrac by remember(bitmap) { mutableFloatStateOf(0.97f) }
    var bottomFrac by remember(bitmap) { mutableFloatStateOf(0.97f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val density = LocalDensity.current
    val hitRadiusPx = with(density) { 38.dp.toPx() }
    val minSpan = 0.14f

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color(0xFF0E1418),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF151E24))
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "বাতিল", tint = Color.White)
                        }
                        Column {
                            Text(
                                "শুধু টেবিল অংশ ক্রপ করুন",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "বাড়তি মার্জিন বাদ দিলে লেখা দ্বিগুণ স্পষ্ট ও ফাইল ছোট হবে",
                                color = Color(0xFFB0BEC5),
                                fontSize = 10.5.sp
                            )
                        }
                    }
                    if (canRestoreOriginal) {
                        TextButton(onClick = onRestoreOriginal) {
                            Text("মূল ছবি", color = Color(0xFF80CBC4), fontSize = 11.5.sp)
                        }
                    }
                }

                // Crop Canvas Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(12.dp)
                        .onSizeChanged { containerSize = it }
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "ক্রপের জন্য নির্বাচিত ছবি",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (containerSize.width > 0 && containerSize.height > 0) {
                        val boxW = containerSize.width.toFloat()
                        val boxH = containerSize.height.toFloat()
                        val scale = min(boxW / bitmap.width.toFloat(), boxH / bitmap.height.toFloat())
                        val imgW = bitmap.width * scale
                        val imgH = bitmap.height * scale
                        val imgLeft = (boxW - imgW) / 2f
                        val imgTop = (boxH - imgH) / 2f

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(bitmap, containerSize) {
                                    var activeHandle = DragHandle.NONE
                                    detectDragGestures(
                                        onDragStart = { start ->
                                            val tl = Offset(imgLeft + leftFrac * imgW, imgTop + topFrac * imgH)
                                            val tr = Offset(imgLeft + rightFrac * imgW, imgTop + topFrac * imgH)
                                            val bl = Offset(imgLeft + leftFrac * imgW, imgTop + bottomFrac * imgH)
                                            val br = Offset(imgLeft + rightFrac * imgW, imgTop + bottomFrac * imgH)

                                            val distances = listOf(
                                                DragHandle.TL to hypot(start.x - tl.x, start.y - tl.y),
                                                DragHandle.TR to hypot(start.x - tr.x, start.y - tr.y),
                                                DragHandle.BL to hypot(start.x - bl.x, start.y - bl.y),
                                                DragHandle.BR to hypot(start.x - br.x, start.y - br.y)
                                            )
                                            val nearest = distances.minByOrNull { it.second }
                                            activeHandle = when {
                                                nearest != null && nearest.second <= hitRadiusPx -> nearest.first
                                                start.x in tl.x..br.x && start.y in tl.y..br.y -> DragHandle.MOVE
                                                else -> DragHandle.NONE
                                            }
                                        },
                                        onDragEnd = { activeHandle = DragHandle.NONE },
                                        onDragCancel = { activeHandle = DragHandle.NONE }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        if (imgW <= 1f || imgH <= 1f) return@detectDragGestures
                                        val dx = dragAmount.x / imgW
                                        val dy = dragAmount.y / imgH
                                        when (activeHandle) {
                                            DragHandle.TL -> {
                                                leftFrac = (leftFrac + dx).coerceIn(0f, rightFrac - minSpan)
                                                topFrac = (topFrac + dy).coerceIn(0f, bottomFrac - minSpan)
                                            }
                                            DragHandle.TR -> {
                                                rightFrac = (rightFrac + dx).coerceIn(leftFrac + minSpan, 1f)
                                                topFrac = (topFrac + dy).coerceIn(0f, bottomFrac - minSpan)
                                            }
                                            DragHandle.BL -> {
                                                leftFrac = (leftFrac + dx).coerceIn(0f, rightFrac - minSpan)
                                                bottomFrac = (bottomFrac + dy).coerceIn(topFrac + minSpan, 1f)
                                            }
                                            DragHandle.BR -> {
                                                rightFrac = (rightFrac + dx).coerceIn(leftFrac + minSpan, 1f)
                                                bottomFrac = (bottomFrac + dy).coerceIn(topFrac + minSpan, 1f)
                                            }
                                            DragHandle.MOVE -> {
                                                val w = rightFrac - leftFrac
                                                val h = bottomFrac - topFrac
                                                val newL = (leftFrac + dx).coerceIn(0f, 1f - w)
                                                val newT = (topFrac + dy).coerceIn(0f, 1f - h)
                                                leftFrac = newL
                                                rightFrac = newL + w
                                                topFrac = newT
                                                bottomFrac = newT + h
                                            }
                                            DragHandle.NONE -> Unit
                                        }
                                    }
                                }
                        ) {
                            val cropRect = Rect(
                                left = imgLeft + leftFrac * imgW,
                                top = imgTop + topFrac * imgH,
                                right = imgLeft + rightFrac * imgW,
                                bottom = imgTop + bottomFrac * imgH
                            )
                            val scrim = Color.Black.copy(alpha = 0.58f)
                            // Dark scrim around crop selection
                            drawRect(scrim, topLeft = Offset(imgLeft, imgTop), size = Size(imgW, (cropRect.top - imgTop).coerceAtLeast(0f)))
                            drawRect(scrim, topLeft = Offset(imgLeft, cropRect.bottom), size = Size(imgW, (imgTop + imgH - cropRect.bottom).coerceAtLeast(0f)))
                            drawRect(scrim, topLeft = Offset(imgLeft, cropRect.top), size = Size((cropRect.left - imgLeft).coerceAtLeast(0f), cropRect.height))
                            drawRect(scrim, topLeft = Offset(cropRect.right, cropRect.top), size = Size((imgLeft + imgW - cropRect.right).coerceAtLeast(0f), cropRect.height))

                            // Crop frame border
                            drawRect(
                                color = Color(0xFF4DB6AC),
                                topLeft = cropRect.topLeft,
                                size = cropRect.size,
                                style = Stroke(width = 2.5.dp.toPx())
                            )

                            // Subtle 3x3 guide lines
                            val thirdW = cropRect.width / 3f
                            val thirdH = cropRect.height / 3f
                            val guideColor = Color.White.copy(alpha = 0.28f)
                            for (i in 1..2) {
                                drawLine(
                                    guideColor,
                                    Offset(cropRect.left + thirdW * i, cropRect.top),
                                    Offset(cropRect.left + thirdW * i, cropRect.bottom),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    guideColor,
                                    Offset(cropRect.left, cropRect.top + thirdH * i),
                                    Offset(cropRect.right, cropRect.top + thirdH * i),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            // 4 corner handles
                            val handleR = 8.dp.toPx()
                            listOf(
                                cropRect.topLeft,
                                cropRect.topRight,
                                cropRect.bottomLeft,
                                cropRect.bottomRight
                            ).forEach { corner ->
                                drawCircle(color = TealPrimary, radius = handleR, center = corner)
                                drawCircle(color = Color.White, radius = handleR * 0.55f, center = corner)
                            }
                        }
                    }
                }

                // Bottom Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF151E24))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRotate90,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("crop_rotate_90_btn")
                    ) {
                        Icon(Icons.Rounded.RotateRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text(" ৯০°", color = Color.White, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            leftFrac = 0f
                            topFrac = 0f
                            rightFrac = 1f
                            bottomFrac = 1f
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("crop_reset_full_btn")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text(" পূর্ণ", color = Color.White, fontSize = 12.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = { onApplyCrop(leftFrac, topFrac, rightFrac, bottomFrac) },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("crop_apply_btn")
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" ক্রপ প্রয়োগ", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
