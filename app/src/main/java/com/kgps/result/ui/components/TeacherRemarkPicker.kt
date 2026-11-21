package com.kgps.result.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kgps.result.AttendanceBand
import com.kgps.result.RemarkCategory
import com.kgps.result.ResultBand
import com.kgps.result.Student
import com.kgps.result.StudentTermProfile
import com.kgps.result.TeacherRemarkEngine
import com.kgps.result.TeacherRemarkOption
import com.kgps.result.TraitsBand
import com.kgps.result.bn
import com.kgps.result.classShortBn
import com.kgps.result.fmt
import com.kgps.result.termNameBn
import com.kgps.result.ui.theme.AmberBg
import com.kgps.result.ui.theme.AmberBorder
import com.kgps.result.ui.theme.AmberNotice
import com.kgps.result.ui.theme.EmeraldGrade
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
import com.kgps.result.ui.theme.appFilterChipBorder
import com.kgps.result.ui.theme.appFilterChipColors
import kotlin.math.abs

@Composable
fun StudentRemarkContextSummary(
    profile: StudentTermProfile,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (resText, resBg, resFg) = when (profile.resultBand) {
            ResultBand.EXCELLENT -> Triple(
                "ফলাফল: অতি উত্তম (${fmt(profile.overallPct ?: 0.0)}%)",
                TealSoft,
                EmeraldGrade
            )
            ResultBand.GOOD -> Triple(
                "ফলাফল: উত্তম (${fmt(profile.overallPct ?: 0.0)}%)",
                TealSoft,
                TealDark
            )
            ResultBand.AVERAGE -> Triple(
                "ফলাফল: সন্তোষজনক (${fmt(profile.overallPct ?: 0.0)}%)",
                AmberBg,
                AmberNotice
            )
            ResultBand.NEEDS_SUPPORT -> Triple(
                "ফলাফল: দুর্বল (${fmt(profile.overallPct ?: 0.0)}%)",
                RoseBg,
                RoseDanger
            )
            ResultBand.UNKNOWN -> Triple("ফলাফল: নম্বর নেই", SlateSurfaceVariant, SlateMuted)
        }
        ContextPillChip(resText, resBg, resFg)

        val (attText, attBg, attFg) = when (profile.attendanceBand) {
            AttendanceBand.REGULAR -> Triple("উপস্থিতি: ${fmt(profile.attPct ?: 0.0)}%", TealSoft, EmeraldGrade)
            AttendanceBand.MODERATE -> Triple("উপস্থিতি: ${fmt(profile.attPct ?: 0.0)}%", AmberBg, AmberNotice)
            AttendanceBand.LOW -> Triple("উপস্থিতি কম: ${fmt(profile.attPct ?: 0.0)}%", RoseBg, RoseDanger)
            AttendanceBand.UNKNOWN -> Triple("উপস্থিতি নেই", SlateSurfaceVariant, SlateMuted)
        }
        ContextPillChip(attText, attBg, attFg)

        val traitSummary = listOfNotNull(profile.act, profile.disc, profile.cond).joinToString("/")
        val (trText, trBg, trFg) = when (profile.traitsBand) {
            TraitsBand.EXEMPLARY -> Triple("গুণাবলি: $traitSummary", TealSoft, EmeraldGrade)
            TraitsBand.GOOD -> Triple("গুণাবলি: $traitSummary", TealSoft, TealDark)
            TraitsBand.NEEDS_GUIDANCE -> Triple("গুণাবলি: $traitSummary", AmberBg, AmberNotice)
            TraitsBand.UNKNOWN -> Triple("গুণাবলি নেই", SlateSurfaceVariant, SlateMuted)
        }
        ContextPillChip(trText, trBg, trFg)

        profile.prevDelta?.let { delta ->
            if (abs(delta) >= 2.0) {
                val up = delta > 0
                val deltaText = if (up) "+${fmt(delta)}% উন্নতি" else "-${fmt(abs(delta))}% কমেছে"
                ContextPillChip(
                    deltaText,
                    if (up) TealSoft else AmberBg,
                    if (up) EmeraldGrade else AmberNotice
                )
            }
        }
    }
}

@Composable
private fun ContextPillChip(text: String, bg: Color, fg: Color) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.25f))
    ) {
        Text(
            text = text,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun InlineTeacherRemarkSection(
    student: Student,
    term: Int,
    remarkText: String,
    enabled: Boolean,
    isAutoSynced: Boolean,
    isGeminiConfigured: Boolean,
    draftingRemarkTerm: Int?,
    onSelectRemark: (String) -> Unit,
    onManualEditRemark: (String) -> Unit,
    onResetAutoRemark: () -> Unit,
    onAiDraftRemark: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = remember(student, term) { TeacherRemarkEngine.analyzeProfile(student, term) }
    val rankedOptions = remember(student, term) { TeacherRemarkEngine.getRankedAlternatives(student, term) }
    val topOption = rankedOptions.firstOrNull()
    val nearbyOptions = remember(rankedOptions) { rankedOptions.take(5) }

    var showPickerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header Row: Title + Auto Status Badge + Auto Reset / Next Alt
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Rounded.AutoFixHigh,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    "শ্রেণি শিক্ষকের সার্বিক মতামত",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateText
                )
            }

            val isUsingTop = topOption != null && remarkText.trim() == topOption.text.trim()
            Surface(
                color = if (isUsingTop || isAutoSynced) TealSoft else SlateSurfaceVariant,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(
                    1.dp,
                    if (isUsingTop || isAutoSynced) TealContainer else SlateOutline
                )
            ) {
                Text(
                    text = when {
                        remarkText.isBlank() -> "খালি"
                        isUsingTop -> "অটো প্রিফিল"
                        rankedOptions.any { it.text.trim() == remarkText.trim() } -> "কাছাকাছি বিকল্প"
                        else -> "কাস্টম মন্তব্য"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isUsingTop || isAutoSynced) TealDark else SlateMuted,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        // Detected Signals Strip (Result + Attendance + Traits)
        StudentRemarkContextSummary(profile = profile)

        // Editable Text Box (Prefilled automatically — no typing needed)
        OutlinedTextField(
            value = remarkText,
            onValueChange = { if (enabled) onManualEditRemark(it) },
            enabled = enabled,
            label = { Text("মতামত (প্রিফিলকৃত — প্রয়োজনে বদলান)", fontSize = 11.5.sp) },
            placeholder = { Text("উপরের তথ্য দিলে এখানে অটো মন্তব্য চলে আসবে", fontSize = 12.sp) },
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("editor_remark_input_$term")
        )

        if (enabled && nearbyOptions.isNotEmpty()) {
            // Quick-swap nearby alternatives bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "কাছাকাছি অল্টারনেটিভ (১ ট্যাপে বাছুন):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateMuted
                    )
                    if (topOption != null && remarkText.trim() != topOption.text.trim()) {
                        Surface(
                            color = TealSoft,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, TealContainer),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onResetAutoRemark() }
                                .testTag("editor_remark_auto_reset_$term")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Refresh,
                                    contentDescription = "অটো প্রিফিলে ফিরুন",
                                    tint = TealPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    "সেরা অটো বাছাই",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealPrimary
                                )
                            }
                        }
                    }
                }

                // Horizontally scrollable nearby alternative cards for instant comparison
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    nearbyOptions.forEachIndexed { idx, opt ->
                        val isSelected = remarkText.trim() == opt.text.trim()
                        Surface(
                            color = if (isSelected) TealSoft else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) TealPrimary else SlateOutline
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .width(228.dp)
                                .heightIn(min = 84.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectRemark(opt.text) }
                                .testTag("editor_remark_alt_${term}_$idx")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = if (opt.isTopPick) TealPrimary else SlateSurfaceVariant,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (opt.isTopPick) "সেরা ম্যাচ" else opt.toneLabel,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (opt.isTopPick) Color.White else SlateText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            "${bn(opt.matchScore.toString())}% মিল",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) TealPrimary else SlateMuted
                                        )
                                        if (isSelected) {
                                            Icon(
                                                Icons.Rounded.CheckCircle,
                                                contentDescription = "নির্বাচিত",
                                                tint = TealPrimary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = opt.text,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = SlateText,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Bottom action row: Browse all alternatives (+ optional AI draft if configured)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showPickerDialog = true },
                        shape = RoundedCornerShape(9.dp),
                        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.55f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = TealSoft,
                            contentColor = TealPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("editor_all_remarks_btn_$term")
                    ) {
                        Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "সব বিকল্প দেখুন (${bn(rankedOptions.size.toString())}টি)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    // Next quick cycle button
                    OutlinedButton(
                        onClick = {
                            val currentIdx = rankedOptions.indexOfFirst { it.text.trim() == remarkText.trim() }
                            val poolSize = rankedOptions.size.coerceAtMost(8).coerceAtLeast(1)
                            val nextIdx = if (currentIdx < 0) 0 else (currentIdx + 1) % poolSize
                            onSelectRemark(rankedOptions[nextIdx].text)
                        },
                        shape = RoundedCornerShape(9.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("editor_cycle_remark_btn_$term")
                    ) {
                        Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("বদলান", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (isGeminiConfigured) {
                        OutlinedButton(
                            onClick = onAiDraftRemark,
                            enabled = draftingRemarkTerm == null,
                            shape = RoundedCornerShape(9.dp),
                            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("editor_ai_draft_$term")
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = "AI বিকল্প", modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(
                                if (draftingRemarkTerm == term) "..." else "AI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPickerDialog) {
        TeacherRemarkPickerDialog(
            student = student,
            term = term,
            currentRemark = remarkText,
            onSelect = { chosen ->
                onSelectRemark(chosen)
                showPickerDialog = false
            },
            onDismiss = { showPickerDialog = false }
        )
    }
}

@Composable
fun TeacherRemarkPickerDialog(
    student: Student,
    term: Int,
    currentRemark: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val profile = remember(student, term) { TeacherRemarkEngine.analyzeProfile(student, term) }
    val allOptions = remember(student, term) { TeacherRemarkEngine.getRankedAlternatives(student, term) }

    var selectedCategory by remember { mutableStateOf(RemarkCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredOptions = remember(allOptions, selectedCategory, searchQuery) {
        val q = searchQuery.trim().lowercase()
        allOptions.filter { opt ->
            val catOk = selectedCategory == RemarkCategory.ALL || opt.category == selectedCategory
            if (!catOk) return@filter false
            if (q.isEmpty()) return@filter true
            opt.text.lowercase().contains(q) ||
                opt.toneLabel.lowercase().contains(q) ||
                opt.category.labelBn.lowercase().contains(q)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 720.dp)
                .heightIn(min = 440.dp, max = 760.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(Modifier.fillMaxSize()) {
                // Header
                Surface(color = TealSoft) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "শ্রেণি শিক্ষকের মন্তব্য বাছাই",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TealDark
                                )
                                Text(
                                    "${student.name.ifBlank { "শিক্ষার্থী" }} · ${classShortBn(student.cls)} · রোল ${bn(student.roll.toString())} · ${termNameBn(term)}",
                                    fontSize = 11.5.sp,
                                    color = SlateMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(48.dp).testTag("remark_picker_close_btn")
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "বন্ধ করুন", tint = SlateText)
                            }
                        }

                        StudentRemarkContextSummary(profile = profile)
                    }
                }

                // Search + Category Filter Chips
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "খোঁজ মুছুন", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        placeholder = { Text("বিষয় বা মন্তব্যের ধরন খুঁজুন (যেমন: গণিত, নিয়মিত)...", fontSize = 12.sp) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("remark_picker_search")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RemarkCategory.entries.forEach { cat ->
                            val selected = selectedCategory == cat
                            val count = if (cat == RemarkCategory.ALL) allOptions.size else allOptions.count { it.category == cat }
                            FilterChip(
                                selected = selected,
                                onClick = { selectedCategory = cat },
                                colors = appFilterChipColors(),
                                border = appFilterChipBorder(selected),
                                modifier = Modifier.testTag("remark_picker_cat_${cat.name.lowercase()}"),
                                label = {
                                    Text(
                                        "${cat.labelBn} (${bn(count.toString())})",
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = SlateOutline)

                // Options List
                if (filteredOptions.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "এই ক্যাটাগরি বা খোঁজে কোনো মন্তব্য পাওয়া যায়নি।",
                            color = SlateMuted,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredOptions, key = { it.id }) { opt ->
                            val isSelected = currentRemark.trim() == opt.text.trim()
                            val proximityBadge = when {
                                opt.isTopPick -> "সেরা অটো ম্যাচ"
                                opt.matchScore >= 82 -> "অত্যন্ত কাছাকাছি"
                                opt.matchScore >= 68 -> "কাছাকাছি বিকল্প"
                                else -> "অন্যান্য বিকল্প"
                            }
                            Surface(
                                color = if (isSelected) TealSoft else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    if (isSelected) 1.6.dp else 1.dp,
                                    if (isSelected) TealPrimary else SlateOutline
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelect(opt.text) }
                                    .testTag("remark_picker_option_${opt.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                color = when {
                                                    opt.isTopPick -> TealPrimary
                                                    opt.matchScore >= 82 -> TealContainer
                                                    else -> SlateSurfaceVariant
                                                },
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = proximityBadge,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when {
                                                        opt.isTopPick -> Color.White
                                                        opt.matchScore >= 82 -> TealDark
                                                        else -> SlateText
                                                    },
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = opt.toneLabel,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = SlateMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Text(
                                                text = "${bn(opt.matchScore.toString())}% মিল",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (opt.matchScore >= 80) EmeraldGrade else SlateMuted
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Rounded.Check,
                                                    contentDescription = "নির্বাচিত",
                                                    tint = TealPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = opt.text,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp,
                                        color = SlateText
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = SlateOutline)

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "মোট ${bn(filteredOptions.size.toString())}টি বিকল্প",
                        fontSize = 11.5.sp,
                        color = SlateMuted
                    )
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("remark_picker_done_btn")
                    ) {
                        Text("বন্ধ করুন", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
