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
import androidx.compose.material3.HorizontalDivider
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

internal enum class ScannerConfigMenu {
    MODE,
    TARGET
}

@Composable
internal fun ScannerConfigMenuBar(
    activeMenu: ScannerConfigMenu?,
    onToggleMenu: (ScannerConfigMenu?) -> Unit,
    scanMode: String,
    canManageStudents: Boolean,
    importScope: ClassSheetImportScope,
    clsHint: Int?,
    termHint: Int?,
    subKeyHint: String?,
    teacherNote: String,
    permissions: TeacherPermissions,
    onChangeScanMode: (String) -> Unit,
    onOpenCsvImport: () -> Unit,
    onChangeScope: (ClassSheetImportScope) -> Unit,
    onChangeCls: (Int?) -> Unit,
    onChangeTerm: (Int?) -> Unit,
    onChangeSub: (String?) -> Unit,
    onChangeTeacherNote: (String) -> Unit
) {
    val allowedClasses = remember(permissions) { permissions.effectiveClasses() }
    val previewCls = clsHint ?: allowedClasses.firstOrNull() ?: 3
    val subs = remember(previewCls, permissions) {
        subjectsFor(previewCls, RELIGIONS.first()).filter { permissions.canEditSubject(it.key) }
    }
    val subLabel = subKeyHint?.let { k -> subs.find { it.key == k }?.bn ?: k }
    val targetSubtitle = if (scanMode == "class") {
        val cText = clsHint?.let { CLS_SHORT[it - 1] } ?: "অটো"
        val tText = termHint?.let { bn(it.toString()) } ?: "অটো"
        if (subLabel != null) "$cText · $tText · $subLabel" else "$cText · $tText · বিষয় অটো"
    } else {
        if (teacherNote.isNotBlank()) "নির্দেশনা যুক্ত" else "নির্দেশনা (ঐচ্ছিক)"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. Single top row menu: ধরন | লক্ষ্য | CSV
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigTopMenuChip(
                title = "ধরন",
                subtitle = if (scanMode == "class") "শ্রেণি তালিকা" else "একক (১ জন)",
                icon = Icons.Rounded.SwapHoriz,
                expanded = activeMenu == ScannerConfigMenu.MODE,
                testTag = "scanner_menu_mode",
                modifier = Modifier.weight(0.95f),
                onClick = {
                    onToggleMenu(if (activeMenu == ScannerConfigMenu.MODE) null else ScannerConfigMenu.MODE)
                }
            )
            ConfigTopMenuChip(
                title = "লক্ষ্য",
                subtitle = targetSubtitle,
                icon = Icons.Rounded.Tune,
                expanded = activeMenu == ScannerConfigMenu.TARGET,
                testTag = "scanner_menu_target",
                modifier = Modifier.weight(1.35f),
                onClick = {
                    onToggleMenu(if (activeMenu == ScannerConfigMenu.TARGET) null else ScannerConfigMenu.TARGET)
                }
            )
            if (canManageStudents) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenCsvImport() }
                        .testTag("scanner_open_csv_btn")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Rounded.UploadFile,
                            contentDescription = "CSV ইমপোর্ট",
                            tint = TealPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            "CSV",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SlateText
                        )
                    }
                }
            }
        }

        // Only the tapped menu's options open below the menu bar
        AnimatedVisibility(
            visible = activeMenu != null,
            enter = fadeIn(tween(170)) + expandVertically(tween(210)),
            exit = fadeOut(tween(130)) + shrinkVertically(tween(170))
        ) {
            activeMenu?.let { section ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(11.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = section,
                        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(110)) },
                        label = "scanner_config_panel"
                    ) { current ->
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            when (current) {
                                ScannerConfigMenu.MODE -> {
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
                                                    .clickable {
                                                        onChangeScanMode("class")
                                                        onToggleMenu(null)
                                                    }
                                                    .testTag("scan_mode_class_chip")
                                            ) {
                                                Text(
                                                    "শ্রেণি তালিকা",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (isClassMode) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isClassMode) Color.White else SlateText,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                                                )
                                            }
                                            val isSingleMode = scanMode == "single"
                                            Surface(
                                                color = if (isSingleMode) TealPrimary else Color.Transparent,
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        onChangeScanMode("single")
                                                        onToggleMenu(null)
                                                    }
                                                    .testTag("scan_mode_single_chip")
                                            ) {
                                                Text(
                                                    "একক (১ জন)",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (isSingleMode) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSingleMode) Color.White else SlateText,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                ScannerConfigMenu.TARGET -> {
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
                                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 6.dp)
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
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigTopMenuChip(
    title: String,
    subtitle: String,
    icon: ImageVector,
    expanded: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (expanded) TealPrimary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (expanded) TealPrimary else SlateOutline),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (expanded) Color.White else TealPrimary,
                modifier = Modifier.size(15.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (expanded) Color.White.copy(alpha = 0.85f) else SlateMuted,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (expanded) Color.White else SlateText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = if (expanded) Color.White else SlateMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
internal fun ScannerPhotoAndActionCard(
    scanMode: String,
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
    onScan: () -> Unit,
    onLoadSample: () -> Unit
) {
    val hasImage = selectedImageBitmap != null

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SlateOutline),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Compact Photo Capture Row
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
                        .height(38.dp)
                        .testTag("ai_camera_photo_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("ক্যামেরা", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onLaunchGallery,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("ai_pick_photo_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("গ্যালারি", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                }
                if (hasImage) {
                    OutlinedButton(
                        onClick = onAskClearImage,
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("ai_clear_photo_btn")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "ছবি সরান", modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Photo Preview & Edit Tools
            selectedImageBitmap?.let { bmp ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                .heightIn(max = 136.dp)
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

            HorizontalDivider(color = SlateOutline.copy(alpha = 0.6f))

            // Primary Scan CTA directly underneath the photo
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
                        .heightIn(min = 44.dp)
                        .testTag("ai_analyze_image_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (analyzingImage) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            "স্ক্যান হচ্ছে...",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Icon(
                            Icons.Rounded.DocumentScanner,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "স্ক্যান করুন",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (scanMode == "class") {
                    OutlinedButton(
                        onClick = onLoadSample,
                        enabled = !analyzingImage,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .testTag("ai_sample_preview_btn")
                    ) {
                        Text("নমুনা", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
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
