package com.kgps.result.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.CLSN
import com.kgps.result.CLS_SHORT
import com.kgps.result.RELIGIONS
import com.kgps.result.ai.ClassSheetImportScope
import com.kgps.result.bn
import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.subjectsFor
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateSurfaceVariant
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealContainer
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft

internal enum class ScannerMenuSection {
    MODE,
    PHOTO,
    TARGET,
    SCAN
}

@Composable
internal fun ScannerCompactMenuContainer(
    activeMenu: ScannerMenuSection?,
    onSelectMenu: (ScannerMenuSection?) -> Unit,
    scanMode: String,
    canManageStudents: Boolean,
    onChangeScanMode: (String) -> Unit,
    onOpenCsvImport: () -> Unit,
    selectedImageBitmap: Bitmap?,
    documentEnhance: Boolean,
    preparedStatsLabel: String?,
    preparedStatsGood: Boolean,
    onLaunchCamera: () -> Unit,
    onLaunchGallery: () -> Unit,
    onAskClearImage: () -> Unit,
    onOpenZoom: () -> Unit,
    onOpenCrop: () -> Unit,
    onRotateImage: () -> Unit,
    onToggleDocumentEnhance: (Boolean) -> Unit,
    importScope: ClassSheetImportScope,
    clsHint: Int?,
    termHint: Int?,
    subKeyHint: String?,
    teacherNote: String,
    permissions: TeacherPermissions,
    onChangeScope: (ClassSheetImportScope) -> Unit,
    onChangeCls: (Int?) -> Unit,
    onChangeTerm: (Int?) -> Unit,
    onChangeSub: (String?) -> Unit,
    onChangeTeacherNote: (String) -> Unit,
    analyzingImage: Boolean,
    hasPreviewOrError: Boolean,
    onScan: () -> Unit,
    onLoadSample: () -> Unit
) {
    val hasImage = selectedImageBitmap != null
    val targetBadge = if (scanMode == "class") {
        val cText = clsHint?.let { CLS_SHORT[it - 1] } ?: "অটো"
        val tText = termHint?.let { bn(it.toString()) } ?: "অটো"
        "$cText·$tText"
    } else {
        if (teacherNote.isNotBlank()) "যুক্ত ✓" else "নির্দেশনা"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Single Compact Top Menu Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScannerTopMenuPill(
                title = "ধরন",
                subtitle = if (scanMode == "class") "শ্রেণি" else "একক",
                icon = Icons.Rounded.SwapHoriz,
                selected = activeMenu == ScannerMenuSection.MODE,
                accentReady = false,
                testTag = "scanner_menu_mode",
                modifier = Modifier.weight(1f),
                onClick = {
                    onSelectMenu(if (activeMenu == ScannerMenuSection.MODE) null else ScannerMenuSection.MODE)
                }
            )
            ScannerTopMenuPill(
                title = "ছবি",
                subtitle = if (hasImage) "প্রস্তুত ✓" else "বাছুন",
                icon = Icons.Rounded.PhotoLibrary,
                selected = activeMenu == ScannerMenuSection.PHOTO,
                accentReady = hasImage,
                testTag = "scanner_menu_photo",
                modifier = Modifier.weight(1f),
                onClick = {
                    onSelectMenu(if (activeMenu == ScannerMenuSection.PHOTO) null else ScannerMenuSection.PHOTO)
                }
            )
            ScannerTopMenuPill(
                title = "লক্ষ্য",
                subtitle = targetBadge,
                icon = Icons.Rounded.Tune,
                selected = activeMenu == ScannerMenuSection.TARGET,
                accentReady = false,
                testTag = "scanner_menu_target",
                modifier = Modifier.weight(1f),
                onClick = {
                    onSelectMenu(if (activeMenu == ScannerMenuSection.TARGET) null else ScannerMenuSection.TARGET)
                }
            )
            ScannerTopMenuPill(
                title = "স্ক্যান",
                subtitle = when {
                    analyzingImage -> "চলছে..."
                    hasImage -> "শুরু ✓"
                    else -> "অ্যাকশন"
                },
                icon = Icons.Rounded.DocumentScanner,
                selected = activeMenu == ScannerMenuSection.SCAN,
                accentReady = hasImage,
                testTag = "scanner_menu_scan",
                modifier = Modifier.weight(1f),
                onClick = {
                    onSelectMenu(if (activeMenu == ScannerMenuSection.SCAN) null else ScannerMenuSection.SCAN)
                }
            )
        }

        // Only the tapped menu's options open here
        AnimatedVisibility(
            visible = activeMenu != null,
            enter = fadeIn(tween(180)) + expandVertically(tween(220)),
            exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
        ) {
            activeMenu?.let { section ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(11.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = section,
                        transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
                        label = "scanner_menu_panel"
                    ) { currentSection ->
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ScannerOpenPanelHeader(
                                title = when (currentSection) {
                                    ScannerMenuSection.MODE -> "স্ক্যান ধরন ও ইমপোর্ট"
                                    ScannerMenuSection.PHOTO -> if (scanMode == "class") "নম্বরপত্রের ছবি" else "একক শিক্ষার্থীর নম্বরপত্র"
                                    ScannerMenuSection.TARGET -> if (scanMode == "class") "লক্ষ্য শ্রেণি, প্রান্তিক ও বিষয়" else "বিশ্লেষণ নির্দেশনা"
                                    ScannerMenuSection.SCAN -> "স্ক্যান ও প্রিভিউ"
                                },
                                onClose = { onSelectMenu(null) }
                            )

                            when (currentSection) {
                                ScannerMenuSection.MODE -> {
                                    ScannerModeOptionsPanel(
                                        scanMode = scanMode,
                                        canManageStudents = canManageStudents,
                                        onChangeScanMode = onChangeScanMode,
                                        onOpenCsvImport = onOpenCsvImport
                                    )
                                }

                                ScannerMenuSection.PHOTO -> {
                                    ScannerPhotoOptionsPanel(
                                        selectedImageBitmap = selectedImageBitmap,
                                        documentEnhance = documentEnhance,
                                        preparedStatsLabel = preparedStatsLabel,
                                        preparedStatsGood = preparedStatsGood,
                                        analyzingImage = analyzingImage,
                                        onLaunchCamera = onLaunchCamera,
                                        onLaunchGallery = onLaunchGallery,
                                        onAskClearImage = onAskClearImage,
                                        onOpenZoom = onOpenZoom,
                                        onOpenCrop = onOpenCrop,
                                        onRotateImage = onRotateImage,
                                        onToggleDocumentEnhance = onToggleDocumentEnhance,
                                        onQuickScan = onScan
                                    )
                                }

                                ScannerMenuSection.TARGET -> {
                                    ScannerTargetOptionsPanel(
                                        scanMode = scanMode,
                                        importScope = importScope,
                                        clsHint = clsHint,
                                        termHint = termHint,
                                        subKeyHint = subKeyHint,
                                        teacherNote = teacherNote,
                                        permissions = permissions,
                                        onChangeScope = onChangeScope,
                                        onChangeCls = onChangeCls,
                                        onChangeTerm = onChangeTerm,
                                        onChangeSub = onChangeSub,
                                        onChangeTeacherNote = onChangeTeacherNote
                                    )
                                }

                                ScannerMenuSection.SCAN -> {
                                    ScannerActionOptionsPanel(
                                        scanMode = scanMode,
                                        clsHint = clsHint,
                                        termHint = termHint,
                                        subKeyHint = subKeyHint,
                                        permissions = permissions,
                                        hasImage = hasImage,
                                        analyzingImage = analyzingImage,
                                        onScan = onScan,
                                        onLoadSample = onLoadSample,
                                        onOpenPhotoMenu = { onSelectMenu(ScannerMenuSection.PHOTO) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Ultra-compact 1-line status bar when all menus are collapsed & no preview/error is open
        if (activeMenu == null && !hasPreviewOrError) {
            if (hasImage) {
                Surface(
                    color = TealSoft,
                    border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "ছবি প্রস্তুত · ${if (scanMode == "class") "শ্রেণি তালিকা" else "একক"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TealDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = onScan,
                            enabled = !analyzingImage,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("ai_analyze_image_btn")
                        ) {
                            if (analyzingImage) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(5.dp))
                                Text("চলছে...", fontSize = 11.sp)
                            } else {
                                Text("স্ক্যান ও প্রিভিউ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Surface(
                    color = SlateSurfaceVariant,
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectMenu(ScannerMenuSection.PHOTO) }
                        .testTag("scanner_collapsed_hint")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.PhotoLibrary,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "উপরের মেনু থেকে ‘ছবি’ ট্যাপ করে নম্বরপত্র তুলুন বা ‘স্ক্যান’ থেকে নমুনা দেখুন",
                            fontSize = 11.sp,
                            color = SlateMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = SlateMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannerTopMenuPill(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    accentReady: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = when {
        selected -> TealPrimary
        accentReady -> TealSoft
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        selected -> TealPrimary
        accentReady -> TealPrimary.copy(alpha = 0.5f)
        else -> SlateOutline
    }
    val titleColor = when {
        selected -> Color.White
        accentReady -> TealDark
        else -> SlateText
    }
    val subColor = when {
        selected -> Color.White.copy(alpha = 0.9f)
        accentReady -> TealDark
        else -> SlateMuted
    }
    val iconTint = when {
        selected -> Color.White
        accentReady -> TealPrimary
        else -> TealPrimary
    }

    Surface(
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = subtitle,
                fontSize = 9.sp,
                fontWeight = if (accentReady || selected) FontWeight.SemiBold else FontWeight.Normal,
                color = subColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ScannerOpenPanelHeader(
    title: String,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TealDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Surface(
            color = SlateSurfaceVariant,
            shape = CircleShape,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .clickable(onClick = onClose)
                .testTag("scanner_panel_collapse_btn")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "বন্ধ করুন",
                    tint = SlateMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ScannerModeOptionsPanel(
    scanMode: String,
    canManageStudents: Boolean,
    onChangeScanMode: (String) -> Unit,
    onOpenCsvImport: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Surface(
            color = SlateSurfaceVariant,
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val isClassMode = scanMode == "class"
                Surface(
                    color = if (isClassMode) TealPrimary else Color.Transparent,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onChangeScanMode("class") }
                        .testTag("scan_mode_class_chip")
                ) {
                    Text(
                        "শ্রেণি তালিকা",
                        fontSize = 11.5.sp,
                        fontWeight = if (isClassMode) FontWeight.Bold else FontWeight.Medium,
                        color = if (isClassMode) Color.White else SlateText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                    )
                }
                val isSingleMode = scanMode == "single"
                Surface(
                    color = if (isSingleMode) TealPrimary else Color.Transparent,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onChangeScanMode("single") }
                        .testTag("scan_mode_single_chip")
                ) {
                    Text(
                        "একক (১ জন)",
                        fontSize = 11.5.sp,
                        fontWeight = if (isSingleMode) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSingleMode) Color.White else SlateText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                    )
                }
            }
        }

        if (canManageStudents) {
            OutlinedButton(
                onClick = onOpenCsvImport,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .testTag("scanner_open_csv_btn")
            ) {
                Icon(
                    Icons.Rounded.UploadFile,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "CSV থেকে নম্বর ইমপোর্ট",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateText
                )
            }
        }
    }
}

@Composable
private fun ScannerPhotoOptionsPanel(
    selectedImageBitmap: Bitmap?,
    documentEnhance: Boolean,
    preparedStatsLabel: String?,
    preparedStatsGood: Boolean,
    analyzingImage: Boolean,
    onLaunchCamera: () -> Unit,
    onLaunchGallery: () -> Unit,
    onAskClearImage: () -> Unit,
    onOpenZoom: () -> Unit,
    onOpenCrop: () -> Unit,
    onRotateImage: () -> Unit,
    onToggleDocumentEnhance: (Boolean) -> Unit,
    onQuickScan: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onLaunchCamera,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("ai_camera_photo_btn"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text("ক্যামেরা", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onLaunchGallery,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("ai_pick_photo_btn"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text("গ্যালারি", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
            }
            if (selectedImageBitmap != null) {
                OutlinedButton(
                    onClick = onAskClearImage,
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("ai_clear_photo_btn")
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "ছবি সরান", modifier = Modifier.size(16.dp))
                }
            }
        }

        selectedImageBitmap?.let { bmp ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenZoom() }
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "নির্বাচিত নম্বরপত্র ছবি",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 118.dp)
                        .background(SlateSurfaceVariant)
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.62f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Rounded.ZoomIn, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Text("জুম", color = Color.White, fontSize = 9.5.sp)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = SlateBg,
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier
                        .clickable { onOpenCrop() }
                        .testTag("ai_crop_photo_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.Crop, contentDescription = "ক্রপ", tint = TealPrimary, modifier = Modifier.size(14.dp))
                        Text("ক্রপ", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = SlateText)
                    }
                }

                Surface(
                    color = SlateBg,
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier
                        .clickable { onRotateImage() }
                        .testTag("ai_rotate_photo_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.RotateRight, contentDescription = "ঘোরান", tint = TealPrimary, modifier = Modifier.size(14.dp))
                        Text("৯০°", fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = SlateText)
                    }
                }

                Surface(
                    color = if (documentEnhance) TealContainer else SlateBg,
                    border = BorderStroke(1.dp, if (documentEnhance) TealPrimary else SlateOutline),
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier
                        .clickable { onToggleDocumentEnhance(!documentEnhance) }
                        .testTag("ai_doc_enhance_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (documentEnhance) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = TealDark, modifier = Modifier.size(13.dp))
                        }
                        Text(
                            if (documentEnhance) "ডকুমেন্ট স্পষ্ট" else "মূল রঙিন",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (documentEnhance) TealDark else SlateText
                        )
                    }
                }

                Surface(
                    color = TealPrimary,
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier
                        .clickable(enabled = !analyzingImage) { onQuickScan() }
                        .testTag("ai_quick_scan_from_photo_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (analyzingImage) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.8.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(Icons.Rounded.DocumentScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        }
                        Text(
                            if (analyzingImage) "চলছে..." else "স্ক্যান করুন",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            preparedStatsLabel?.let { label ->
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = if (preparedStatsGood) TealDark else SlateMuted
                )
            }
        }
    }
}

@Composable
private fun ScannerTargetOptionsPanel(
    scanMode: String,
    importScope: ClassSheetImportScope,
    clsHint: Int?,
    termHint: Int?,
    subKeyHint: String?,
    teacherNote: String,
    permissions: TeacherPermissions,
    onChangeScope: (ClassSheetImportScope) -> Unit,
    onChangeCls: (Int?) -> Unit,
    onChangeTerm: (Int?) -> Unit,
    onChangeSub: (String?) -> Unit,
    onChangeTeacherNote: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        if (scanMode == "class") {
            ClassSheetHintSelectors(
                clsHint = clsHint,
                termHint = termHint,
                subKeyHint = subKeyHint,
                permissions = permissions,
                onChangeCls = onChangeCls,
                onChangeTerm = onChangeTerm,
                onChangeSub = onChangeSub
            )

            Surface(
                color = SlateSurfaceVariant,
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf(
                        ClassSheetImportScope.AUTO to "স্বয়ংক্রিয়",
                        ClassSheetImportScope.BOTH to "ধারাবাহিক+সামষ্টিক",
                        ClassSheetImportScope.CONT_ONLY to "ধারাবাহিক",
                        ClassSheetImportScope.WR_ONLY to "সামষ্টিক"
                    ).forEach { (scopeOpt, label) ->
                        val isSel = importScope == scopeOpt
                        Surface(
                            color = if (isSel) TealPrimary else Color.Transparent,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(if (scopeOpt == ClassSheetImportScope.BOTH) 1.35f else 1f)
                                .clickable { onChangeScope(scopeOpt) }
                                .testTag("scan_scope_${scopeOpt.name.lowercase()}_btn")
                        ) {
                            Text(
                                label,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else SlateText,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = teacherNote,
            onValueChange = onChangeTeacherNote,
            label = {
                Text(
                    "নির্দেশনা (ঐচ্ছিক, যেমন: রোল ১-৩৬ শুধু ধারাবাহিক)",
                    fontSize = 10.5.sp
                )
            },
            singleLine = true,
            textStyle = TextStyle(fontSize = 12.sp),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_scanner_note_input")
        )
    }
}

@Composable
private fun ScannerActionOptionsPanel(
    scanMode: String,
    clsHint: Int?,
    termHint: Int?,
    subKeyHint: String?,
    permissions: TeacherPermissions,
    hasImage: Boolean,
    analyzingImage: Boolean,
    onScan: () -> Unit,
    onLoadSample: () -> Unit,
    onOpenPhotoMenu: () -> Unit
) {
    val allowedClasses = remember(permissions) { permissions.effectiveClasses() }
    val previewCls = clsHint ?: allowedClasses.firstOrNull() ?: 3
    val subs = remember(previewCls, permissions) {
        subjectsFor(previewCls, RELIGIONS.first()).filter { permissions.canEditSubject(it.key) }
    }
    val subLabel = subKeyHint?.let { k -> subs.find { it.key == k }?.bn ?: k } ?: "অটো বিষয়"
    val summaryText = if (scanMode == "class") {
        val cText = clsHint?.let { "${CLS_SHORT[it - 1]} শ্রেণি" } ?: "অটো শ্রেণি"
        val tText = termHint?.let { "${bn(it.toString())} প্রান্তিক" } ?: "অটো প্রান্তিক"
        "$cText · $tText · $subLabel · ${if (hasImage) "ছবি প্রস্তুত" else "ছবি নেই"}"
    } else {
        "একক শিক্ষার্থী · ${if (hasImage) "ছবি প্রস্তুত" else "ছবি নেই"}"
    }

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Surface(
            color = SlateSurfaceVariant,
            shape = RoundedCornerShape(7.dp),
            modifier = Modifier
                .fillMaxWidth()
                .let { if (!hasImage) it.clickable { onOpenPhotoMenu() } else it }
        ) {
            Text(
                text = summaryText,
                fontSize = 10.5.sp,
                color = if (hasImage) TealDark else SlateMuted,
                fontWeight = if (hasImage) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onScan,
                enabled = hasImage && !analyzingImage,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("ai_analyze_image_btn"),
                shape = RoundedCornerShape(9.dp)
            ) {
                if (analyzingImage) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(15.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "স্ক্যান হচ্ছে...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Icon(
                        Icons.Rounded.DocumentScanner,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (scanMode != "class") "নম্বরপত্র বিশ্লেষণ" else "স্ক্যান ও প্রিভিউ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (scanMode == "class") {
                OutlinedButton(
                    onClick = onLoadSample,
                    enabled = !analyzingImage,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("ai_sample_preview_btn")
                ) {
                    Text("নমুনা", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
internal fun ClassSheetHintSelectors(
    clsHint: Int?,
    termHint: Int?,
    subKeyHint: String?,
    permissions: TeacherPermissions,
    onChangeCls: (Int?) -> Unit,
    onChangeTerm: (Int?) -> Unit,
    onChangeSub: (String?) -> Unit
) {
    var clsMenu by remember { mutableStateOf(false) }
    var termMenu by remember { mutableStateOf(false) }
    var subMenu by remember { mutableStateOf(false) }

    val allowedClasses = remember(permissions) { permissions.effectiveClasses() }
    val previewCls = clsHint ?: allowedClasses.firstOrNull() ?: 3
    val subs = remember(previewCls, permissions) {
        subjectsFor(previewCls, RELIGIONS.first()).filter { permissions.canEditSubject(it.key) }
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Class selector
        Box(Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { clsMenu = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 4.dp)
            ) {
                Text(
                    clsHint?.let { "${CLS_SHORT[it - 1]} শ্রেণি" } ?: "শ্রেণি: অটো",
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(15.dp))
            }
            DropdownMenu(expanded = clsMenu, onDismissRequest = { clsMenu = false }) {
                val isClsAuto = clsHint == null
                DropdownMenuItem(
                    modifier = Modifier.background(if (isClsAuto) TealContainer else Color.Transparent),
                    text = {
                        Text(
                            "স্বয়ংক্রিয় শনাক্ত",
                            fontWeight = if (isClsAuto) FontWeight.Bold else FontWeight.Normal,
                            color = if (isClsAuto) TealDark else SlateText
                        )
                    },
                    trailingIcon = if (isClsAuto) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealDark, modifier = Modifier.size(16.dp)) }
                    } else null,
                    onClick = { onChangeCls(null); clsMenu = false }
                )
                allowedClasses.forEach { c ->
                    val isClsSel = clsHint == c
                    DropdownMenuItem(
                        modifier = Modifier.background(if (isClsSel) TealContainer else Color.Transparent),
                        text = {
                            Text(
                                "${CLSN[c - 1]} শ্রেণি",
                                fontWeight = if (isClsSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isClsSel) TealDark else SlateText
                            )
                        },
                        trailingIcon = if (isClsSel) {
                            { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealDark, modifier = Modifier.size(16.dp)) }
                        } else null,
                        onClick = { onChangeCls(c); clsMenu = false }
                    )
                }
            }
        }

        // Term selector
        Box(Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { termMenu = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 4.dp)
            ) {
                Text(
                    termHint?.let { "${bn(it.toString())} প্রান্তিক" } ?: "প্রান্তিক: অটো",
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(15.dp))
            }
            DropdownMenu(expanded = termMenu, onDismissRequest = { termMenu = false }) {
                val isTermAuto = termHint == null
                DropdownMenuItem(
                    modifier = Modifier.background(if (isTermAuto) TealContainer else Color.Transparent),
                    text = {
                        Text(
                            "স্বয়ংক্রিয় শনাক্ত",
                            fontWeight = if (isTermAuto) FontWeight.Bold else FontWeight.Normal,
                            color = if (isTermAuto) TealDark else SlateText
                        )
                    },
                    trailingIcon = if (isTermAuto) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealDark, modifier = Modifier.size(16.dp)) }
                    } else null,
                    onClick = { onChangeTerm(null); termMenu = false }
                )
                (1..3).forEach { t ->
                    val isTermSel = termHint == t
                    DropdownMenuItem(
                        modifier = Modifier.background(if (isTermSel) TealContainer else Color.Transparent),
                        text = {
                            Text(
                                "${bn(t.toString())} প্রান্তিক",
                                fontWeight = if (isTermSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isTermSel) TealDark else SlateText
                            )
                        },
                        trailingIcon = if (isTermSel) {
                            { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealDark, modifier = Modifier.size(16.dp)) }
                        } else null,
                        onClick = { onChangeTerm(t); termMenu = false }
                    )
                }
            }
        }

        // Subject selector
        Box(Modifier.weight(1.2f)) {
            val activeSubName = subKeyHint?.let { k -> subs.find { it.key == k }?.bn ?: k }
            OutlinedButton(
                onClick = { subMenu = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 4.dp)
            ) {
                Text(
                    activeSubName ?: "বিষয়: অটো",
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(15.dp))
            }
            DropdownMenu(expanded = subMenu, onDismissRequest = { subMenu = false }) {
                val isSubAuto = subKeyHint == null
                DropdownMenuItem(
                    modifier = Modifier.background(if (isSubAuto) TealContainer else Color.Transparent),
                    text = {
                        Text(
                            "স্বয়ংক্রিয় শনাক্ত",
                            fontWeight = if (isSubAuto) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSubAuto) TealDark else SlateText
                        )
                    },
                    trailingIcon = if (isSubAuto) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealDark, modifier = Modifier.size(16.dp)) }
                    } else null,
                    onClick = { onChangeSub(null); subMenu = false }
                )
                subs.forEach { s ->
                    val isSubSel = subKeyHint == s.key
                    DropdownMenuItem(
                        modifier = Modifier.background(if (isSubSel) TealContainer else Color.Transparent),
                        text = {
                            Text(
                                s.bn,
                                fontWeight = if (isSubSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSubSel) TealDark else SlateText
                            )
                        },
                        trailingIcon = if (isSubSel) {
                            { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealDark, modifier = Modifier.size(16.dp)) }
                        } else null,
                        onClick = { onChangeSub(s.key); subMenu = false }
                    )
                }
            }
        }
    }
}
