package com.kgps.result.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.WifiOff
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import com.kgps.result.ai.offline.ALL_CONFIGURABLE_COLUMN_ITEMS
import com.kgps.result.ai.offline.ColumnRole
import com.kgps.result.ai.offline.OFFLINE_COLUMN_SUGGESTIONS
import com.kgps.result.ai.offline.OfflineColumnTemplate
import com.kgps.result.ai.offline.OfflineDigitScript
import com.kgps.result.ai.offline.ScanEngineMode
import com.kgps.result.bn
import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.subjectsFor
import com.kgps.result.ui.theme.AmberBg
import com.kgps.result.ui.theme.AmberBorder
import com.kgps.result.ui.theme.AmberNotice
import com.kgps.result.ui.theme.RoseBg
import com.kgps.result.ui.theme.RoseDanger
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateSurfaceVariant
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealContainer
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft

@Composable
private fun StepSectionHeader(
    stepNumberBn: String,
    title: String,
    trailingBadge: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            color = TealContainer,
            shape = CircleShape,
            modifier = Modifier.size(20.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stepNumberBn,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TealDark
                )
            }
        }
        Text(
            text = title,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = SlateText,
            modifier = Modifier.weight(1f)
        )
        trailingBadge?.invoke()
    }
}

@Composable
internal fun ScannerModeHeaderBar(
    scanMode: String,
    canManageStudents: Boolean,
    onChangeScanMode: (String) -> Unit,
    onOpenCsvImport: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = SlateSurfaceVariant,
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                Modifier.padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val isClassMode = scanMode == "class"
                Surface(
                    color = if (isClassMode) TealPrimary else Color.Transparent,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .clickable { onChangeScanMode("class") }
                        .testTag("scan_mode_class_chip")
                ) {
                    Text(
                        "শ্রেণি তালিকা",
                        fontSize = 11.sp,
                        fontWeight = if (isClassMode) FontWeight.Bold else FontWeight.Medium,
                        color = if (isClassMode) Color.White else SlateText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                val isSingleMode = scanMode == "single"
                Surface(
                    color = if (isSingleMode) TealPrimary else Color.Transparent,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .clickable { onChangeScanMode("single") }
                        .testTag("scan_mode_single_chip")
                ) {
                    Text(
                        "একক (১ জন)",
                        fontSize = 11.sp,
                        fontWeight = if (isSingleMode) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSingleMode) Color.White else SlateText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        if (canManageStudents) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .clickable { onOpenCsvImport() }
                    .testTag("scanner_open_csv_btn")
            ) {
                Row(
                    Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Rounded.UploadFile,
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "CSV",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateText
                    )
                }
            }
        }
    }
}

@Composable
internal fun ScannerPhotoCaptureCard(
    scanMode: String,
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
    onToggleDocumentEnhance: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SlateOutline),
        shape = RoundedCornerShape(11.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            StepSectionHeader(
                stepNumberBn = "১",
                title = if (scanMode == "class") "নম্বরপত্রের ছবি" else "একক শিক্ষার্থীর নম্বরপত্র",
                trailingBadge = {
                    val hasImage = selectedImageBitmap != null
                    Surface(
                        color = if (hasImage) TealContainer else SlateSurfaceVariant,
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text(
                            text = if (hasImage) "প্রস্তুত" else "ছবি বাছুন",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (hasImage) TealDark else SlateMuted,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            )

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
                                .heightIn(max = 128.dp)
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
        }
    }
}

@Composable
internal fun ScannerConfigCard(
    scanMode: String,
    scanEngineMode: ScanEngineMode,
    importScope: ClassSheetImportScope,
    clsHint: Int?,
    termHint: Int?,
    subKeyHint: String?,
    teacherNote: String,
    permissions: TeacherPermissions,
    onSetScanEngineMode: (ScanEngineMode) -> Unit,
    onChangeScope: (ClassSheetImportScope) -> Unit,
    onChangeCls: (Int?) -> Unit,
    onChangeTerm: (Int?) -> Unit,
    onChangeSub: (String?) -> Unit,
    onChangeTeacherNote: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SlateOutline),
        shape = RoundedCornerShape(11.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            StepSectionHeader(
                stepNumberBn = "২",
                title = if (scanMode == "class") "লক্ষ্য ও ইঞ্জিন" else "বিশ্লেষণ নির্দেশনা"
            )

            if (scanMode == "class") {
                // Engine Switcher
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
                        val isCloud = scanEngineMode == ScanEngineMode.CLOUD_GEMINI
                        Surface(
                            color = if (isCloud) TealPrimary else Color.Transparent,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSetScanEngineMode(ScanEngineMode.CLOUD_GEMINI) }
                                .testTag("scan_engine_cloud_btn")
                        ) {
                            Row(
                                Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (isCloud) Color.White else TealPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Gemini AI (অনলাইন)",
                                    fontSize = 11.sp,
                                    fontWeight = if (isCloud) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCloud) Color.White else SlateText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        val isOffline = scanEngineMode == ScanEngineMode.OFFLINE_TABLE
                        Surface(
                            color = if (isOffline) TealDark else Color.Transparent,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSetScanEngineMode(ScanEngineMode.OFFLINE_TABLE) }
                                .testTag("scan_engine_offline_btn")
                        ) {
                            Row(
                                Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.WifiOff,
                                    contentDescription = null,
                                    tint = if (isOffline) Color.White else TealDark,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "অফলাইন টেবিল",
                                    fontSize = 11.sp,
                                    fontWeight = if (isOffline) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isOffline) Color.White else SlateText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Target Class / Term / Subject
                ClassSheetHintSelectors(
                    clsHint = clsHint,
                    termHint = termHint,
                    subKeyHint = subKeyHint,
                    permissions = permissions,
                    onChangeCls = onChangeCls,
                    onChangeTerm = onChangeTerm,
                    onChangeSub = onChangeSub
                )

                // Import Scope Selector
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

            if (scanMode != "class" || scanEngineMode == ScanEngineMode.CLOUD_GEMINI) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OfflineTableConfiguratorCard(
    selectedScript: OfflineDigitScript,
    customColumns: List<ColumnRole>,
    onSetScript: (OfflineDigitScript) -> Unit,
    onApplyTemplate: (OfflineColumnTemplate) -> Unit,
    onResetToAuto: () -> Unit,
    onAddRole: (ColumnRole) -> Unit,
    onRemoveAt: (Int) -> Unit,
    onMoveColumn: (Int, Int) -> Unit
) {
    val popularTemplates = remember { OFFLINE_COLUMN_SUGGESTIONS.filter { it.isPopular } }
    val moreTemplates = remember { OFFLINE_COLUMN_SUGGESTIONS.filter { !it.isPopular } }
    var showMorePresets by remember { mutableStateOf(false) }
    var showAllItems by remember { mutableStateOf(false) }
    var selectedColIndex by remember(customColumns.size) {
        mutableIntStateOf((customColumns.size - 1).coerceAtLeast(0))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(11.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            StepSectionHeader(
                stepNumberBn = "৩",
                title = "অফলাইন টেবিল ও কলাম",
                trailingBadge = if (customColumns.isNotEmpty()) {
                    {
                        Surface(
                            color = TealSoft,
                            border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .clickable { onResetToAuto() }
                                .testTag("offline_reset_auto_btn")
                        ) {
                            Text(
                                "স্বয়ংক্রিয়",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TealDark,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                } else null
            )

            // 1. Compact Digit Script Segmented Bar
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
                    OfflineDigitScript.entries.forEach { scriptOpt ->
                        val sel = selectedScript == scriptOpt
                        Surface(
                            color = if (sel) TealPrimary else Color.Transparent,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSetScript(scriptOpt) }
                                .testTag("offline_script_${scriptOpt.name.lowercase()}")
                        ) {
                            Text(
                                scriptOpt.shortBn,
                                fontSize = 10.5.sp,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                color = if (sel) Color.White else SlateText,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // 2. Compact Popular Quick Presets Row
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                popularTemplates.forEach { tpl ->
                    val isSelected = customColumns == tpl.columns
                    Surface(
                        color = if (isSelected) TealPrimary else SlateBg,
                        border = BorderStroke(1.dp, if (isSelected) TealPrimary else SlateOutline),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .widthIn(min = 142.dp, max = 166.dp)
                            .clickable {
                                onApplyTemplate(tpl)
                                selectedColIndex = 0
                            }
                            .testTag("offline_popular_${tpl.id}")
                    ) {
                        Column(
                            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Star,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else AmberNotice,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        tpl.badgeBn ?: "জনপ্রিয়",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else AmberNotice
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = "নির্বাচিত",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            Text(
                                tpl.titleBn,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else SlateText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 3. Active Column Summary / Custom Order Strip
            if (customColumns.isEmpty()) {
                Surface(
                    color = SlateSurfaceVariant,
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "স্বয়ংক্রিয় কলাম · ছবি দেখে নিজেই কলাম সাজাবে",
                            fontSize = 10.5.sp,
                            color = SlateText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                Surface(
                    color = TealSoft,
                    border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FlowRow(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            customColumns.forEachIndexed { idx, role ->
                                val focused = idx == selectedColIndex
                                val bg = when {
                                    focused -> TealPrimary
                                    role.isSkipped -> AmberBg
                                    else -> Color.White
                                }
                                val fg = when {
                                    focused -> Color.White
                                    role.isSkipped -> AmberNotice
                                    else -> TealDark
                                }
                                val borderCol = when {
                                    focused -> TealDark
                                    role.isSkipped -> AmberBorder
                                    else -> TealPrimary.copy(alpha = 0.35f)
                                }
                                Surface(
                                    color = bg,
                                    border = BorderStroke(1.dp, borderCol),
                                    shape = RoundedCornerShape(7.dp),
                                    modifier = Modifier
                                        .clickable { selectedColIndex = idx }
                                        .testTag("offline_selected_col_$idx")
                                ) {
                                    Text(
                                        "${bn((idx + 1).toString())}. ${role.shortBn}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = fg,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        val safeIdx = selectedColIndex.coerceIn(0, customColumns.lastIndex)
                        val activeRole = customColumns[safeIdx]
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                "${bn((safeIdx + 1).toString())}: ${activeRole.titleBn}",
                                fontSize = 10.sp,
                                color = TealDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = if (safeIdx > 0) Color.White else Color.Transparent,
                                border = BorderStroke(1.dp, SlateOutline),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .clickable(enabled = safeIdx > 0) {
                                        onMoveColumn(safeIdx, -1)
                                        selectedColIndex = (safeIdx - 1).coerceAtLeast(0)
                                    }
                                    .testTag("offline_col_move_left")
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                        contentDescription = "বামে নিন",
                                        tint = if (safeIdx > 0) SlateText else SlateMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        "বামে",
                                        fontSize = 9.5.sp,
                                        color = if (safeIdx > 0) SlateText else SlateMuted
                                    )
                                }
                            }
                            Surface(
                                color = if (safeIdx < customColumns.lastIndex) Color.White else Color.Transparent,
                                border = BorderStroke(1.dp, SlateOutline),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .clickable(enabled = safeIdx < customColumns.lastIndex) {
                                        onMoveColumn(safeIdx, +1)
                                        selectedColIndex = (safeIdx + 1).coerceAtMost(customColumns.lastIndex)
                                    }
                                    .testTag("offline_col_move_right")
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "ডানে",
                                        fontSize = 9.5.sp,
                                        color = if (safeIdx < customColumns.lastIndex) SlateText else SlateMuted
                                    )
                                    Icon(
                                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = "ডানে নিন",
                                        tint = if (safeIdx < customColumns.lastIndex) SlateText else SlateMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            val canRemove = !(activeRole == ColumnRole.ROLL && customColumns.count { it == ColumnRole.ROLL } <= 1)
                            if (canRemove) {
                                Surface(
                                    color = RoseBg,
                                    border = BorderStroke(1.dp, RoseDanger.copy(alpha = 0.35f)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .clickable {
                                            onRemoveAt(safeIdx)
                                            selectedColIndex = (safeIdx - 1).coerceAtLeast(0)
                                        }
                                        .testTag("offline_col_remove_btn")
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = "বাদ দিন",
                                            tint = RoseDanger,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text("বাদ", fontSize = 9.5.sp, color = RoseDanger, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Compact Expandable Toggles (More Presets & All 12 Column Items)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = if (showMorePresets) TealSoft else SlateBg,
                    border = BorderStroke(1.dp, if (showMorePresets) TealPrimary else SlateOutline),
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showMorePresets = !showMorePresets }
                        .testTag("offline_toggle_more_presets")
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "আরও প্রিসেট (${bn(moreTemplates.size.toString())})",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (showMorePresets) TealDark else SlateText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            if (showMorePresets) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TealDark,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Surface(
                    color = if (showAllItems) TealSoft else SlateBg,
                    border = BorderStroke(1.dp, if (showAllItems) TealPrimary else SlateOutline),
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showAllItems = !showAllItems }
                        .testTag("offline_toggle_all_items")
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "কলাম আইটেম (${bn(ALL_CONFIGURABLE_COLUMN_ITEMS.size.toString())})",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (showAllItems) TealDark else SlateText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            if (showAllItems) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TealDark,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showMorePresets,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val autoSel = customColumns.isEmpty()
                    Surface(
                        color = if (autoSel) TealPrimary else SlateBg,
                        border = BorderStroke(1.dp, if (autoSel) TealPrimary else SlateOutline),
                        shape = RoundedCornerShape(7.dp),
                        modifier = Modifier
                            .clickable { onResetToAuto() }
                            .testTag("offline_preset_auto")
                    ) {
                        Text(
                            "স্বয়ংক্রিয় (ছবি দেখে)",
                            fontSize = 10.sp,
                            fontWeight = if (autoSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (autoSel) Color.White else SlateText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                    moreTemplates.forEach { tpl ->
                        val sel = customColumns == tpl.columns
                        Surface(
                            color = if (sel) TealPrimary else SlateBg,
                            border = BorderStroke(1.dp, if (sel) TealPrimary else SlateOutline),
                            shape = RoundedCornerShape(7.dp),
                            modifier = Modifier
                                .clickable {
                                    onApplyTemplate(tpl)
                                    selectedColIndex = 0
                                }
                                .testTag("offline_preset_${tpl.id}")
                        ) {
                            Text(
                                tpl.titleBn,
                                fontSize = 10.sp,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                color = if (sel) Color.White else SlateText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showAllItems,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    ALL_CONFIGURABLE_COLUMN_ITEMS.forEach { role ->
                        val activeCount = customColumns.count { it == role }
                        val isAdded = activeCount > 0
                        val canToggleOff = isAdded && !role.isRepeatable && role != ColumnRole.ROLL
                        Surface(
                            color = when {
                                isAdded && role.isSkipped -> AmberBg
                                isAdded -> TealPrimary
                                else -> SlateBg
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isAdded && role.isSkipped -> AmberBorder
                                    isAdded -> TealPrimary
                                    else -> SlateOutline
                                }
                            ),
                            shape = RoundedCornerShape(7.dp),
                            modifier = Modifier
                                .clickable {
                                    if (canToggleOff) {
                                        val idx = customColumns.indexOf(role)
                                        if (idx >= 0) onRemoveAt(idx)
                                    } else {
                                        onAddRole(role)
                                    }
                                }
                                .testTag("offline_item_${role.name.lowercase()}")
                        ) {
                            Row(
                                Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    if (isAdded && !role.isRepeatable) Icons.Rounded.Check else Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = when {
                                        isAdded && role.isSkipped -> AmberNotice
                                        isAdded -> Color.White
                                        else -> TealPrimary
                                    },
                                    modifier = Modifier.size(12.dp)
                                )
                                val badge = if (role.isRepeatable && activeCount > 0) " (${bn(activeCount.toString())})" else ""
                                Text(
                                    role.shortBn + badge,
                                    fontSize = 10.sp,
                                    fontWeight = if (isAdded) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isAdded && role.isSkipped -> AmberNotice
                                        isAdded -> Color.White
                                        else -> SlateText
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ScannerPrimaryActionBar(
    scanMode: String,
    scanEngineMode: ScanEngineMode,
    hasImage: Boolean,
    analyzingImage: Boolean,
    onScan: () -> Unit,
    onLoadSample: () -> Unit
) {
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
                .height(42.dp)
                .testTag("ai_analyze_image_btn"),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (analyzingImage) {
                CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (scanMode == "class" && scanEngineMode == ScanEngineMode.OFFLINE_TABLE) "অফলাইন স্ক্যান হচ্ছে..."
                    else "স্ক্যান হচ্ছে...",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Icon(
                    if (scanMode == "class" && scanEngineMode == ScanEngineMode.OFFLINE_TABLE) Icons.Rounded.WifiOff
                    else Icons.Rounded.DocumentScanner,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    when {
                        scanMode != "class" -> "নম্বরপত্র বিশ্লেষণ"
                        scanEngineMode == ScanEngineMode.OFFLINE_TABLE -> "অফলাইন স্ক্যান ও প্রিভিউ"
                        else -> "স্ক্যান ও প্রিভিউ"
                    },
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
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(42.dp)
                    .testTag("ai_sample_preview_btn")
            ) {
                Text("নমুনা", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
