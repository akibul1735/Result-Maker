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
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.QrCode2
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.BlankSheetQrMeta
import com.kgps.result.RELIGIONS
import com.kgps.result.SheetQrCodec
import com.kgps.result.ai.ClassSheetImportScope
import com.kgps.result.ai.ClassSheetKind
import com.kgps.result.ai.SheetScanDetection
import com.kgps.result.bn
import com.kgps.result.classNameBn
import com.kgps.result.classShortBn
import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.subjectsFor
import com.kgps.result.termNameBn
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateSurfaceVariant
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealContainer
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft

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
    sheetKindHint: ClassSheetKind? = null,
    clsHint: Int?,
    termHint: Int?,
    subKeyHint: String?,
    teacherNote: String,
    permissions: TeacherPermissions,
    autoDetectedActive: Boolean = false,
    onResetToAuto: () -> Unit = {},
    onChangeScanMode: (String) -> Unit,
    onOpenCsvImport: () -> Unit,
    onChangeScope: (ClassSheetImportScope) -> Unit,
    onChangeSheetKind: (ClassSheetKind?) -> Unit = {},
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
    val modeSubtitle = when (scanMode) {
        "auto" -> "অটো শনাক্ত"
        "class" -> when (sheetKindHint) {
            ClassSheetKind.SINGLE_TERM_MARKS -> "শ্রেণি · ১ প্রা."
            ClassSheetKind.THREE_TERM_MARKS -> "শ্রেণি · ৩ প্রা."
            ClassSheetKind.ATTENDANCE_TRAITS -> "শ্রেণি · হাজিরা"
            null -> "শ্রেণি তালিকা"
        }
        else -> "একক (১ জন)"
    }
    val hasAnyTargetHint = clsHint != null || termHint != null || subKeyHint != null || sheetKindHint != null
    val targetSubtitle = when {
        scanMode == "auto" && !hasAnyTargetHint ->
            if (teacherNote.isNotBlank()) "অটো · নির্দেশনা" else "অটো শনাক্ত"
        scanMode == "single" && !hasAnyTargetHint ->
            if (teacherNote.isNotBlank()) "নির্দেশনা যুক্ত" else "অটো / নির্দেশনা"
        else -> {
            val cText = clsHint?.let { classShortBn(it) } ?: "অটো"
            when (sheetKindHint) {
                ClassSheetKind.ATTENDANCE_TRAITS -> "$cText · উপস্থিতি"
                ClassSheetKind.THREE_TERM_MARKS -> "$cText · ৩ প্রা. · ${subLabel ?: "অটো"}"
                else -> {
                    val tText = termHint?.let { "${bn(it.toString())} প্রা." } ?: "অটো"
                    if (subLabel != null) "$cText · $tText · $subLabel" else "$cText · $tText · অটো"
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // 1. Single top row menu: ধরন | লক্ষ্য | CSV
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigTopMenuChip(
                title = "ধরন",
                subtitle = modeSubtitle,
                icon = Icons.Rounded.SwapHoriz,
                expanded = activeMenu == ScannerConfigMenu.MODE,
                isAutoBadge = autoDetectedActive,
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
                isAutoBadge = autoDetectedActive && hasAnyTargetHint,
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
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { onOpenCsvImport() }
                        .testTag("scanner_open_csv_btn")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
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
                            fontSize = 9.5.sp,
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
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = section,
                        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(110)) },
                        label = "scanner_config_panel"
                    ) { current ->
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                                .padding(2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            listOf(
                                                Triple("auto", "অটো", "scan_mode_auto_chip"),
                                                Triple("class", "শ্রেণি তালিকা", "scan_mode_class_chip"),
                                                Triple("single", "একক (১ জন)", "scan_mode_single_chip")
                                            ).forEach { (modeKey, modeLabel, tag) ->
                                                val isSel = scanMode == modeKey
                                                Surface(
                                                    color = if (isSel) TealPrimary else Color.Transparent,
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier
                                                        .weight(if (modeKey == "auto") 0.85f else 1f)
                                                        .clickable {
                                                            onChangeScanMode(modeKey)
                                                            onToggleMenu(null)
                                                        }
                                                        .testTag(tag)
                                                ) {
                                                    Text(
                                                        modeLabel,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSel) Color.White else SlateText,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 7.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                ScannerConfigMenu.TARGET -> {
                                    val canResetToAuto = scanMode != "auto" || hasAnyTargetHint || importScope != ClassSheetImportScope.AUTO
                                    if (canResetToAuto) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                if (autoDetectedActive) "স্ক্যান থেকে অটো-ফিল হয়েছে" else "ম্যানুয়াল লক্ষ্য সেট করা",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (autoDetectedActive) TealDark else SlateMuted
                                            )
                                            Surface(
                                                color = TealSoft,
                                                border = BorderStroke(1.dp, TealContainer),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier
                                                    .clickable { onResetToAuto() }
                                                    .testTag("scan_target_reset_auto_btn")
                                            ) {
                                                Text(
                                                    "অটো রিসেট",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TealDark,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (scanMode != "single") {
                                        // শীটের ধরন (অটো / ১ প্রান্তিক / ৩ প্রান্তিক / উপস্থিতি)
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
                                                    null to "অটো",
                                                    ClassSheetKind.SINGLE_TERM_MARKS to "১ প্রান্তিক",
                                                    ClassSheetKind.THREE_TERM_MARKS to "৩ প্রান্তিক",
                                                    ClassSheetKind.ATTENDANCE_TRAITS to "উপস্থিতি"
                                                ).forEach { (kindOpt, label) ->
                                                    val isSel = sheetKindHint == kindOpt
                                                    val tagKey = kindOpt?.name?.lowercase() ?: "auto"
                                                    Surface(
                                                        color = if (isSel) TealPrimary else Color.Transparent,
                                                        shape = RoundedCornerShape(6.dp),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable { onChangeSheetKind(kindOpt) }
                                                            .testTag("scan_kind_${tagKey}_btn")
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

                                        ClassSheetHintSelectors(
                                            clsHint = clsHint,
                                            termHint = termHint,
                                            subKeyHint = subKeyHint,
                                            permissions = permissions,
                                            showTerm = sheetKindHint == null || sheetKindHint == ClassSheetKind.SINGLE_TERM_MARKS,
                                            showSubject = sheetKindHint != ClassSheetKind.ATTENDANCE_TRAITS,
                                            onChangeCls = onChangeCls,
                                            onChangeTerm = onChangeTerm,
                                            onChangeSub = onChangeSub
                                        )

                                        if (sheetKindHint == null || sheetKindHint == ClassSheetKind.SINGLE_TERM_MARKS) {
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
                                                        ClassSheetImportScope.AUTO to "অটো",
                                                        ClassSheetImportScope.BOTH to "সব অংশ",
                                                        ClassSheetImportScope.CONT_ONLY to "ধারাবাহিক",
                                                        ClassSheetImportScope.WR_ONLY to "সামষ্টিক"
                                                    ).forEach { (scopeOpt, label) ->
                                                        val isSel = importScope == scopeOpt
                                                        Surface(
                                                            color = if (isSel) TealPrimary else Color.Transparent,
                                                            shape = RoundedCornerShape(6.dp),
                                                            modifier = Modifier
                                                                .weight(1f)
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
    isAutoBadge: Boolean = false,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (expanded) TealPrimary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (expanded) TealPrimary else SlateOutline),
        shape = RoundedCornerShape(9.dp),
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (expanded) Color.White else TealPrimary,
                modifier = Modifier.size(14.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (expanded) Color.White.copy(alpha = 0.85f) else SlateMuted,
                        maxLines = 1
                    )
                    if (isAutoBadge) {
                        Surface(
                            color = if (expanded) Color.White.copy(alpha = 0.22f) else TealSoft,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "অটো",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (expanded) Color.White else TealDark,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp)
                            )
                        }
                    }
                }
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
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
internal fun ScannerPhotoAndActionCard(
    scanMode: String,
    selectedImageBitmap: Bitmap?,
    detectedSheetQr: BlankSheetQrMeta? = null,
    sheetDetectionSource: String? = null,
    sheetDetection: SheetScanDetection? = null,
    resolvedTemplateMeta: BlankSheetQrMeta? = null,
    resolvedTemplateSource: String? = null,
    resolvingSheetTemplate: Boolean = false,
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
        shape = RoundedCornerShape(11.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                        .height(36.dp)
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
                        .height(36.dp)
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
                            .size(36.dp)
                            .testTag("ai_clear_photo_btn")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "ছবি সরান", modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Photo Preview & Edit Tools
            selectedImageBitmap?.let { bmp ->
                var expandedCodeChannel by remember(bmp) { mutableStateOf<SheetCodeChannel?>(null) }
                val rightQrDetected = sheetDetection?.detectedFromOfflineQr == true
                val leftQrDetected = sheetDetection?.detectedFromThinQr == true
                val bottomQrDetected = sheetDetection?.detectedFromBottomQr == true

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
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
                                .heightIn(max = 124.dp)
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
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
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
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
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
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
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
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
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

                        // অগ্রাধিকার ক্রমে ৩টি QR কোড (ডান অফলাইন -> বাম স্মার্ট ID -> নিচের ব্যাকআপ QR)
                        SheetCodeIconPill(
                            channel = SheetCodeChannel.RIGHT_OFFLINE_QR,
                            detected = rightQrDetected,
                            selected = expandedCodeChannel == SheetCodeChannel.RIGHT_OFFLINE_QR,
                            loading = false,
                            testTag = "ai_detected_icon_qr2",
                            onClick = {
                                expandedCodeChannel = if (expandedCodeChannel == SheetCodeChannel.RIGHT_OFFLINE_QR) null else SheetCodeChannel.RIGHT_OFFLINE_QR
                            }
                        )
                        SheetCodeIconPill(
                            channel = SheetCodeChannel.LEFT_THIN_QR,
                            detected = leftQrDetected,
                            selected = expandedCodeChannel == SheetCodeChannel.LEFT_THIN_QR,
                            loading = leftQrDetected && resolvingSheetTemplate,
                            testTag = "ai_detected_icon_qr1",
                            onClick = {
                                expandedCodeChannel = if (expandedCodeChannel == SheetCodeChannel.LEFT_THIN_QR) null else SheetCodeChannel.LEFT_THIN_QR
                            }
                        )
                        SheetCodeIconPill(
                            channel = SheetCodeChannel.BOTTOM_THIN_QR,
                            detected = bottomQrDetected,
                            selected = expandedCodeChannel == SheetCodeChannel.BOTTOM_THIN_QR,
                            loading = bottomQrDetected && resolvingSheetTemplate,
                            testTag = "ai_detected_icon_bottom_qr",
                            onClick = {
                                expandedCodeChannel = if (expandedCodeChannel == SheetCodeChannel.BOTTOM_THIN_QR) null else SheetCodeChannel.BOTTOM_THIN_QR
                            }
                        )
                    }

                    preparedStatsLabel?.let { label ->
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            color = if (preparedStatsGood) TealDark else SlateMuted
                        )
                    }

                    AnimatedVisibility(
                        visible = expandedCodeChannel != null,
                        enter = expandVertically(tween(180)) + fadeIn(tween(180)),
                        exit = shrinkVertically(tween(150)) + fadeOut(tween(150))
                    ) {
                        expandedCodeChannel?.let { channel ->
                            SheetDetectedChannelDetailCard(
                                channel = channel,
                                detection = sheetDetection,
                                fallbackQrMeta = detectedSheetQr,
                                fallbackSource = sheetDetectionSource,
                                resolvedTemplateMeta = resolvedTemplateMeta,
                                resolvedTemplateSource = resolvedTemplateSource,
                                resolvingSheetTemplate = resolvingSheetTemplate,
                                onDismiss = { expandedCodeChannel = null }
                            )
                        }
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

                if (scanMode != "single") {
                    OutlinedButton(
                        onClick = onLoadSample,
                        enabled = !analyzingImage,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .testTag("ai_sample_preview_btn")
                    ) {
                        Text("নমুনা", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
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
    showTerm: Boolean = true,
    showSubject: Boolean = true,
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
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Class selector
        Box(Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { clsMenu = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    clsHint?.let { "${classShortBn(it)} শ্রেণি" } ?: "শ্রেণি: অটো",
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
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
                                "${classNameBn(c)} শ্রেণি",
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
        if (showTerm) {
            Box(Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { termMenu = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    termHint?.let { termNameBn(it) } ?: "প্রান্তিক: অটো",
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
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
                                termNameBn(t),
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
        }

        // Subject selector
        if (showSubject) {
        Box(Modifier.weight(1.2f)) {
            val activeSubName = subKeyHint?.let { k -> subs.find { it.key == k }?.bn ?: k }
            OutlinedButton(
                onClick = { subMenu = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    activeSubName ?: "বিষয়: অটো",
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
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
}

private enum class SheetCodeChannel(
    val shortTag: String,
    val titleBn: String
) {
    RIGHT_OFFLINE_QR(shortTag = "ডা", titleBn = "১. ডান QR · পূর্ণাঙ্গ অফলাইন কাঠামো"),
    LEFT_THIN_QR(shortTag = "বা", titleBn = "২. বাম QR · স্মার্ট ID (ক্যাশ/ক্লাউড)"),
    BOTTOM_THIN_QR(shortTag = "নি", titleBn = "৩. নিচের QR · ব্যাকআপ স্মার্ট ID")
}

@Composable
private fun SheetCodeIconPill(
    channel: SheetCodeChannel,
    detected: Boolean,
    selected: Boolean,
    loading: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val bgColor = when {
        selected -> TealPrimary
        detected -> TealSoft
        else -> SlateBg
    }
    val borderColor = when {
        selected -> TealDark
        detected -> TealPrimary.copy(alpha = 0.72f)
        else -> SlateOutline
    }
    val contentColor = when {
        selected -> Color.White
        detected -> TealDark
        else -> SlateMuted.copy(alpha = 0.78f)
    }
    val statusText = when {
        loading -> "খোঁজা হচ্ছে"
        detected -> "শনাক্ত"
        else -> "পাওয়া যায়নি"
    }

    Surface(
        color = bgColor,
        border = BorderStroke(if (selected) 1.4.dp else 1.dp, borderColor),
        shape = RoundedCornerShape(7.dp),
        modifier = Modifier
            .semantics { contentDescription = "${channel.titleBn} — $statusText" }
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = if (channel == SheetCodeChannel.RIGHT_OFFLINE_QR) Icons.Rounded.QrCode2 else Icons.Rounded.QrCode,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = channel.shortTag,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }

            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(7.dp),
                    strokeWidth = 1.4.dp,
                    color = if (selected) Color.White else TealPrimary
                )
            } else if (detected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(5.dp)
                        .background(
                            color = if (selected) Color.White else TealPrimary,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun SheetDetectedChannelDetailCard(
    channel: SheetCodeChannel,
    detection: SheetScanDetection?,
    fallbackQrMeta: BlankSheetQrMeta?,
    fallbackSource: String?,
    resolvedTemplateMeta: BlankSheetQrMeta?,
    resolvedTemplateSource: String?,
    resolvingSheetTemplate: Boolean,
    onDismiss: () -> Unit
) {
    val isDetected = when (channel) {
        SheetCodeChannel.RIGHT_OFFLINE_QR -> detection?.detectedFromOfflineQr == true
        SheetCodeChannel.LEFT_THIN_QR -> detection?.detectedFromThinQr == true
        SheetCodeChannel.BOTTOM_THIN_QR -> detection?.detectedFromBottomQr == true
    }
    val skippedByHigherPriority = !isDetected && when (channel) {
        SheetCodeChannel.RIGHT_OFFLINE_QR -> false
        SheetCodeChannel.LEFT_THIN_QR -> detection?.detectedFromOfflineQr == true
        SheetCodeChannel.BOTTOM_THIN_QR -> detection?.detectedFromOfflineQr == true || detection?.detectedFromThinQr == true
    }

    val statusText = when {
        (channel == SheetCodeChannel.LEFT_THIN_QR || channel == SheetCodeChannel.BOTTOM_THIN_QR) && isDetected && resolvingSheetTemplate -> "লোড হচ্ছে..."
        isDetected -> "শনাক্ত"
        skippedByHigherPriority -> "প্রয়োজন হয়নি"
        else -> "পাওয়া যায়নি"
    }

    val cardBg = if (isDetected) TealSoft else SlateBg
    val cardBorder = if (isDetected) TealContainer else SlateOutline

    Surface(
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_qr_detected_badge")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = channel.titleBn,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDetected) TealDark else SlateText,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    color = if (isDetected) TealContainer else Color.White,
                    shape = RoundedCornerShape(5.dp),
                    border = BorderStroke(1.dp, if (isDetected) TealPrimary.copy(alpha = 0.5f) else SlateOutline)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDetected) TealDark else SlateMuted,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "বিস্তারিত বন্ধ করুন",
                    tint = SlateMuted,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(onClick = onDismiss)
                )
            }

            when (channel) {
                SheetCodeChannel.RIGHT_OFFLINE_QR -> {
                    val offlineMeta = detection?.offlineQrMeta
                    if (isDetected && offlineMeta != null) {
                        Text(
                            text = SheetQrCodec.buildSummaryLabelBn(offlineMeta),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TealDark
                        )
                        val extraBits = buildList {
                            if (offlineMeta.totalPages > 1) {
                                add("পৃষ্ঠা: ${bn(offlineMeta.pageIndex.toString())}/${bn(offlineMeta.totalPages.toString())}")
                            }
                            if (offlineMeta.workingDaysByTerm.isNotEmpty()) {
                                add("কার্যদিবস: ${offlineMeta.workingDaysByTerm.entries.joinToString(", ") { "${termNameBn(it.key)} ${bn(it.value)}" }}")
                            }
                        }
                        if (extraBits.isNotEmpty()) {
                            Text(
                                text = extraBits.joinToString(" · "),
                                fontSize = 9.5.sp,
                                color = TealPrimary
                            )
                        }
                        val cols = SheetQrCodec.shortColumnsPreviewBn(offlineMeta, maxItems = 12)
                        if (cols.isNotBlank()) {
                            Text(
                                text = "কলাম: $cols",
                                fontSize = 9.5.sp,
                                color = SlateText
                            )
                        }
                        detection?.offlineQrRawText?.takeIf { it.isNotBlank() }?.let { raw ->
                            Text(
                                text = "পেলোড: ${raw.take(54)}${if (raw.length > 54) "…" else ""}",
                                fontSize = 9.sp,
                                color = SlateMuted
                            )
                        }
                    } else {
                        val otherHint = buildOtherDetectedFallbackHint(detection, fallbackSource)
                        Text(
                            text = "ছবির উপরের ডান কোণায় অফলাইন QR পড়া যায়নি।$otherHint",
                            fontSize = 9.5.sp,
                            color = SlateMuted
                        )
                    }
                }

                SheetCodeChannel.LEFT_THIN_QR, SheetCodeChannel.BOTTOM_THIN_QR -> {
                    if (isDetected) {
                        val tid = detection?.templateId ?: fallbackQrMeta?.templateId.orEmpty()
                        val raw = if (channel == SheetCodeChannel.BOTTOM_THIN_QR) {
                            detection?.bottomQrRawText
                        } else {
                            detection?.thinQrRawText
                        } ?: tid.takeIf { it.isNotBlank() }?.let { "KT1:$it" }.orEmpty()
                        if (raw.isNotBlank()) {
                            Text(
                                text = "কোড: $raw",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = TealPrimary
                            )
                        }
                        when {
                            resolvingSheetTemplate -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(11.dp),
                                        strokeWidth = 1.6.dp,
                                        color = TealPrimary
                                    )
                                    Text(
                                        text = "Firestore/ক্যাশ থেকে ফর্মের পূর্ণ কাঠামো মিলিয়ে নেওয়া হচ্ছে...",
                                        fontSize = 9.5.sp,
                                        color = TealDark
                                    )
                                }
                            }
                            resolvedTemplateMeta != null -> {
                                val src = resolvedTemplateSource ?: "ক্যাশ/Firestore"
                                Text(
                                    text = "উৎস: $src · ${SheetQrCodec.buildSummaryLabelBn(resolvedTemplateMeta)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TealDark
                                )
                                val cols = SheetQrCodec.shortColumnsPreviewBn(resolvedTemplateMeta, maxItems = 12)
                                if (cols.isNotBlank()) {
                                    Text(
                                        text = "কলাম: $cols",
                                        fontSize = 9.5.sp,
                                        color = SlateText
                                    )
                                }
                            }
                            else -> {
                                Text(
                                    text = "স্মার্ট ID পাওয়া গেছে, তবে এই ডিভাইসের ক্যাশ বা অনলাইনে মিল পাওয়া যায়নি।",
                                    fontSize = 9.5.sp,
                                    color = SlateMuted
                                )
                            }
                        }
                    } else if (skippedByHigherPriority) {
                        val higherName = if (detection?.detectedFromOfflineQr == true) {
                            "ডান পাশের অফলাইন QR"
                        } else {
                            "বাম পাশের QR"
                        }
                        Text(
                            text = "$higherName থেকে কাঠামো শনাক্ত হয়ে যাওয়ায় এই ব্যাকআপ QR আর স্ক্যান করতে হয়নি।",
                            fontSize = 9.5.sp,
                            color = SlateMuted
                        )
                    } else {
                        val posText = if (channel == SheetCodeChannel.BOTTOM_THIN_QR) {
                            "ছবির নিচের অংশে ব্যাকআপ QR পড়া যায়নি।"
                        } else {
                            "ছবির উপরের বাম কোণায় স্মার্ট ID QR পড়া যায়নি।"
                        }
                        val otherHint = buildOtherDetectedFallbackHint(detection, fallbackSource)
                        Text(
                            text = "$posText$otherHint",
                            fontSize = 9.5.sp,
                            color = SlateMuted
                        )
                    }
                }
            }
        }
    }
}

private fun buildOtherDetectedFallbackHint(
    detection: SheetScanDetection?,
    fallbackSource: String?
): String {
    if (detection == null || detection.detectedCount == 0) {
        return " ডান, বাম বা নিচের QR কোড স্পষ্টভাবে ফ্রেমে রেখে চেষ্টা করুন।"
    }
    val activeSource = fallbackSource?.takeIf { it.isNotBlank() } ?: detection.sourceLabelBn
    return if (activeSource.isNotBlank()) {
        " তবে '$activeSource' থেকে কাঠামো শনাক্ত হয়ে গেছে।"
    } else ""
}
