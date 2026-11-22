package com.kgps.result.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.CsvRowStatus
import com.kgps.result.CsvValidatedRow
import com.kgps.result.CsvValidationReport
import com.kgps.result.Repo
import com.kgps.result.Settings
import com.kgps.result.activeSchoolClasses
import com.kgps.result.bn
import com.kgps.result.classShortBn
import com.kgps.result.fromBn
import com.kgps.result.subjectsFor
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
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealContainer
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft
import com.kgps.result.ui.theme.appFilterChipBorder
import com.kgps.result.ui.theme.appFilterChipColors

enum class CsvPreviewFilter(val id: String, val labelBn: String) {
    ALL("all", "সব"),
    ISSUES("issues", "সতর্কতা/ত্রুটি"),
    ADD("add", "নতুন"),
    UPDATE("update", "হালনাগাদ")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CsvStepInputAndMapping(
    rawText: String,
    onRawTextChange: (String) -> Unit,
    parsedSheet: Repo.CsvParsedSheet,
    parsedRows: List<List<String>>,
    columns: List<Repo.CsvColMapping>,
    onColumnsChange: (List<Repo.CsvColMapping>) -> Unit,
    fallbackCls: Int,
    onFallbackClsChange: (Int) -> Unit,
    contextTerm: Int? = null,
    onContextTermChange: (Int?) -> Unit = {},
    contextSubjectKey: String?,
    onContextSubjectKeyChange: (String?) -> Unit,
    sharedWorkingDaysByTerm: Map<Int, String> = emptyMap(),
    onSharedWorkingDaysChange: (Map<Int, String>) -> Unit = {},
    settings: Settings,
    onPickFile: () -> Unit,
    onPasteClipboard: () -> Unit,
    onGoToPreview: () -> Unit
) {
    var mappingExpanded by remember(columns.size) {
        mutableStateOf(columns.any { it.isNew })
    }
    val hasCodeFences = remember(rawText) { rawText.contains("```") }
    val newCols = columns.count { it.isNew }
    val matchedCols = columns.size - newCols
    val clsSubjects = remember(fallbackCls, settings.customClasses) { subjectsFor(fallbackCls.coerceIn(1, 20), "ইসলাম") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "৩. AI বা এক্সেল থেকে CSV/টেবিল পেস্ট করুন অথবা ফাইল দিন",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = SlateText
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onPasteClipboard,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("csv_paste_clipboard_btn")
            ) {
                Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("পেস্ট করুন", fontSize = 12.5.sp)
            }

            OutlinedButton(
                onClick = onPickFile,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("csv_pick_file_btn")
            ) {
                Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("ফাইল আপলোড", fontSize = 12.5.sp)
            }
        }

        OutlinedTextField(
            value = rawText,
            onValueChange = onRawTextChange,
            label = { Text("CSV / Excel Tab / Markdown টেবিল / ```csv কোড ব্লক এখানে দিন", fontSize = 11.5.sp) },
            minLines = if (parsedRows.isEmpty()) 6 else 4,
            maxLines = 8,
            trailingIcon = if (rawText.isNotBlank()) {
                {
                    TextButton(
                        onClick = { onRawTextChange("") },
                        modifier = Modifier.testTag("csv_clear_raw_btn")
                    ) {
                        Text("মুছুন", fontSize = 11.sp, color = RoseDanger)
                    }
                }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("csv_raw_input")
        )

        val hasSmartDetections = hasCodeFences ||
            parsedSheet.detectedCls != null ||
            parsedSheet.detectedSubjectKey != null ||
            parsedSheet.detectedWorkingDaysByTerm.isNotEmpty() ||
            parsedSheet.usedTwoRowHeader
        if (hasSmartDetections && parsedRows.isNotEmpty()) {
            val parts = buildList {
                parsedSheet.detectedCls?.let { add("শ্রেণি: ${classShortBn(it, settings)}") }
                parsedSheet.detectedSubjectKey?.let { sk ->
                    val sName = clsSubjects.find { it.key == sk }?.bn?.substringBefore(" (") ?: sk
                    add("বিষয়: $sName")
                }
                if (parsedSheet.detectedWorkingDaysByTerm.isNotEmpty()) {
                    val wdSummary = parsedSheet.detectedWorkingDaysByTerm.entries
                        .sortedBy { it.key }
                        .joinToString(", ") { (t, d) -> "${termNameBn(t)}: ${bn(d)}" }
                    add("কার্যদিবস ($wdSummary)")
                }
                if (parsedSheet.usedTwoRowHeader) add("২-সারি প্রান্তিক হেডার")
                if (hasCodeFences) add("AI কোড ব্লক পরিষ্কার")
            }
            Surface(
                color = TealSoft,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, TealContainer)
            ) {
                Text(
                    "✓ স্বয়ংক্রিয় শনাক্ত: ${parts.joinToString(" · ")}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TealDark,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        val hasAttendanceOrTraitCol = columns.any {
            it.targetType == "text" && (
                it.targetKey.startsWith("PRES_") ||
                    it.targetKey.startsWith("ACT_") ||
                    it.targetKey.startsWith("DISC_") ||
                    it.targetKey.startsWith("COND_") ||
                    it.targetKey.startsWith("DAYS_")
                )
        }
        if (parsedRows.isNotEmpty() && (hasAttendanceOrTraitCol || sharedWorkingDaysByTerm.isNotEmpty())) {
            Surface(
                color = SlateBg,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SlateOutline)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "সবার প্রান্তিক কার্যদিবস (CSV-তে কলাম না থাকলে একবার এখানে দিন):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TealDark
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 2, 3).forEach { t ->
                            val raw = sharedWorkingDaysByTerm[t].orEmpty()
                            OutlinedTextField(
                                value = if (raw.isEmpty()) "" else bn(raw),
                                onValueChange = { txt ->
                                    val clean = fromBn(txt).filter { it.isDigit() }.take(3)
                                    val next = sharedWorkingDaysByTerm.toMutableMap()
                                    if (clean.isEmpty()) next.remove(t) else next[t] = clean
                                    onSharedWorkingDaysChange(next)
                                },
                                label = { Text(termNameBn(t), fontSize = 10.sp) },
                                placeholder = { Text("ফাঁকা", fontSize = 10.sp) },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 11.5.sp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("csv_shared_wd_$t")
                            )
                        }
                    }
                }
            }
        }

        if (parsedRows.isNotEmpty()) {
            val dataCount = (parsedRows.size - 1).coerceAtLeast(0)
            Surface(
                color = SlateBg,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SlateOutline)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "শনাক্তকৃত ডেটা: ${bn(dataCount.toString())}টি সারি · ${bn(columns.size.toString())}টি কলাম (মিলেছে ${bn(matchedCols.toString())}, নতুন ${bn(newCols.toString())})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        color = if (newCols > 0) AmberNotice else TealPrimary
                    )

                    Text("শ্রেণি (CSV-তে না থাকলে ব্যবহৃত হবে):", fontSize = 11.5.sp, color = SlateMuted)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        activeSchoolClasses(settings).forEach { cDef ->
                            val c = cDef.id
                            val isFallbackSel = fallbackCls == c
                            FilterChip(
                                selected = isFallbackSel,
                                onClick = { onFallbackClsChange(c) },
                                colors = appFilterChipColors(),
                                border = appFilterChipBorder(isFallbackSel),
                                label = {
                                    Text(
                                        cDef.shortBn,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isFallbackSel) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.testTag("csv_fallback_cls_$c")
                            )
                        }
                    }

                    Text("প্রান্তিক (শিরোনামে না থাকলে একক প্রান্তিক কলামের জন্য):", fontSize = 11.5.sp, color = SlateMuted)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isAutoTerm = contextTerm == null
                        FilterChip(
                            selected = isAutoTerm,
                            onClick = { onContextTermChange(null) },
                            colors = appFilterChipColors(),
                            border = appFilterChipBorder(isAutoTerm),
                            label = { Text("স্বয়ংক্রিয়", fontSize = 11.sp) },
                            modifier = Modifier.testTag("csv_ctx_term_auto")
                        )
                        (1..3).forEach { t ->
                            val isTermSel = contextTerm == t
                            FilterChip(
                                selected = isTermSel,
                                onClick = { onContextTermChange(t) },
                                colors = appFilterChipColors(),
                                border = appFilterChipBorder(isTermSel),
                                label = {
                                    Text(
                                        termNameBn(t),
                                        fontSize = 11.sp,
                                        fontWeight = if (isTermSel) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.testTag("csv_ctx_term_$t")
                            )
                        }
                    }

                    Text("বিষয় (শিরোনামহীন 'ধারাবাহিক/সামষ্টিক' কলামের জন্য):", fontSize = 11.5.sp, color = SlateMuted)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isNoneSub = contextSubjectKey == null
                        FilterChip(
                            selected = isNoneSub,
                            onClick = { onContextSubjectKeyChange(null) },
                            colors = appFilterChipColors(),
                            border = appFilterChipBorder(isNoneSub),
                            label = {
                                Text("স্বয়ংক্রিয়", fontSize = 11.sp)
                            },
                            modifier = Modifier.testTag("csv_ctx_sub_auto")
                        )
                        clsSubjects.forEach { s ->
                            val isSubSel = contextSubjectKey == s.key
                            FilterChip(
                                selected = isSubSel,
                                onClick = { onContextSubjectKeyChange(s.key) },
                                colors = appFilterChipColors(),
                                border = appFilterChipBorder(isSubSel),
                                label = {
                                    Text(
                                        s.bn.substringBefore(" ("),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSubSel) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.testTag("csv_ctx_sub_${s.key}")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { mappingExpanded = !mappingExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "কলাম ম্যাপিং যাচাই ও কাস্টমাইজ করুন",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            color = SlateText
                        )
                        Icon(
                            if (mappingExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (mappingExpanded) "সংকুচিত করুন" else "বিস্তারিত দেখুন",
                            tint = SlateMuted
                        )
                    }

                    AnimatedVisibility(visible = mappingExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (newCols > 0) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            onColumnsChange(
                                                columns.map {
                                                    if (it.isNew) it.copy(targetType = "create_new", newLabel = it.header) else it
                                                }
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("csv_create_all_new_btn"),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("নতুন সব তৈরি করুন", fontSize = 11.sp, maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onColumnsChange(
                                                columns.map {
                                                    if (it.isNew) it.copy(targetType = "skip") else it
                                                }
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("csv_skip_all_new_btn"),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("নতুনগুলো বাদ দিন", fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                            }

                            columns.forEachIndexed { idx, col ->
                                CsvColumnMappingCard(
                                    index = idx,
                                    col = col,
                                    fallbackCls = fallbackCls,
                                    contextSubjectKey = contextSubjectKey,
                                    settings = settings,
                                    onUpdateCol = { updated ->
                                        onColumnsChange(
                                            columns.map { if (it.colIndex == col.colIndex) updated else it }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (dataCount > 0) {
                Button(
                    onClick = onGoToPreview,
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("csv_input_to_preview_btn")
                ) {
                    Text("ভ্যালিডেশন ও প্রিভিউ দেখুন (${bn(dataCount.toString())}টি সারি) →", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun CsvColumnMappingCard(
    index: Int,
    col: Repo.CsvColMapping,
    fallbackCls: Int,
    contextSubjectKey: String?,
    settings: Settings,
    onUpdateCol: (Repo.CsvColMapping) -> Unit
) {
    var dropdownOpen by remember(col.colIndex) { mutableStateOf(false) }
    val targetDesc = when (col.targetType) {
        "create_new" -> "+ নতুন ফিল্ড: ${col.newLabel}"
        "skip" -> "✕ বাদ দিন"
        "standard" -> Repo.STANDARD_FIELDS.find { it.first == col.targetKey }?.second ?: col.targetKey
        "custom" -> "কাস্টম: ${settings.customFields.find { it.key == col.targetKey }?.label ?: col.targetKey}"
        "mark" -> Repo.friendlyMarkHeader(col.targetKey, com.kgps.result.CsvHeaderStyle.FRIENDLY_BN)
        "text" -> Repo.friendlyTextHeader(col.targetKey, com.kgps.result.CsvHeaderStyle.FRIENDLY_BN)
        else -> col.targetKey
    }
    val clsSubs = remember(fallbackCls, settings.customClasses) { subjectsFor(fallbackCls.coerceIn(1, 20), "ইসলাম") }
    val activeSub = contextSubjectKey?.let { key -> clsSubs.find { it.key == key } } ?: clsSubs.first()
    val quickMarkTargets = remember(activeSub) {
        buildList {
            (1..3).forEach { t ->
                add(com.kgps.result.mk(activeSub.key, t, "CONT"))
                if (activeSub.written > 0) add(com.kgps.result.mk(activeSub.key, t, "WR"))
                add(com.kgps.result.mk(activeSub.key, t, "TOTAL"))
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (col.isNew && col.targetType == "create_new") AmberBg else Color.White),
        border = BorderStroke(1.dp, if (col.isNew && col.targetType == "create_new") AmberBorder else SlateOutline),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${bn((index + 1).toString())}. ${col.header}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    when {
                        col.targetType == "skip" -> "বাদ"
                        col.isNew && col.targetType == "create_new" -> "নতুন কলাম"
                        else -> "মিলেছে"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        col.targetType == "skip" -> SlateMuted
                        col.isNew && col.targetType == "create_new" -> AmberNotice
                        else -> EmeraldGrade
                    }
                )
            }
            if (col.samples.isNotEmpty()) {
                Text(
                    "নমুনা: ${col.samples.joinToString(" · ")}",
                    fontSize = 11.sp,
                    color = SlateMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box {
                OutlinedButton(
                    onClick = { dropdownOpen = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("$targetDesc ▾", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DropdownMenu(expanded = dropdownOpen, onDismissRequest = { dropdownOpen = false }) {
                    val isCreateNewSel = col.targetType == "create_new"
                    DropdownMenuItem(
                        modifier = Modifier.background(if (isCreateNewSel) TealContainer else Color.Transparent),
                        text = {
                            Text(
                                "+ নতুন ফিল্ড তৈরি করুন (ডাটাবেসে যোগ হবে)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCreateNewSel) TealDark else AmberNotice
                            )
                        },
                        trailingIcon = if (isCreateNewSel) {
                            { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealDark, modifier = Modifier.size(16.dp)) }
                        } else null,
                        onClick = {
                            onUpdateCol(col.copy(targetType = "create_new", newLabel = col.header))
                            dropdownOpen = false
                        }
                    )
                    val isSkipSel = col.targetType == "skip"
                    DropdownMenuItem(
                        modifier = Modifier.background(if (isSkipSel) TealContainer else Color.Transparent),
                        text = {
                            Text(
                                "✕ বাদ দিন (ইমপোর্ট করবেন না)",
                                fontSize = 12.sp,
                                fontWeight = if (isSkipSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSkipSel) TealDark else SlateMuted
                            )
                        },
                        trailingIcon = if (isSkipSel) {
                            { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealDark, modifier = Modifier.size(16.dp)) }
                        } else null,
                        onClick = {
                            onUpdateCol(col.copy(targetType = "skip"))
                            dropdownOpen = false
                        }
                    )
                    HorizontalDivider()
                    Repo.STANDARD_FIELDS.forEach { (fKey, fLabel) ->
                        val isStdSel = col.targetType == "standard" && col.targetKey == fKey
                        DropdownMenuItem(
                            modifier = Modifier.background(if (isStdSel) TealContainer else Color.Transparent),
                            text = {
                                Text(
                                    fLabel,
                                    fontSize = 12.sp,
                                    fontWeight = if (isStdSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isStdSel) TealDark else SlateText
                                )
                            },
                            trailingIcon = if (isStdSel) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealDark, modifier = Modifier.size(16.dp)) }
                            } else null,
                            onClick = {
                                onUpdateCol(col.copy(targetType = "standard", targetKey = fKey))
                                dropdownOpen = false
                            }
                        )
                    }
                    HorizontalDivider()
                    quickMarkTargets.forEach { mKey ->
                        val isMarkSel = col.targetType == "mark" && col.targetKey == mKey
                        DropdownMenuItem(
                            modifier = Modifier.background(if (isMarkSel) TealContainer else Color.Transparent),
                            text = {
                                Text(
                                    Repo.friendlyMarkHeader(mKey, com.kgps.result.CsvHeaderStyle.FRIENDLY_BN),
                                    fontSize = 12.sp,
                                    fontWeight = if (isMarkSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isMarkSel) TealDark else SlateText
                                )
                            },
                            trailingIcon = if (isMarkSel) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealDark, modifier = Modifier.size(16.dp)) }
                            } else null,
                            onClick = {
                                onUpdateCol(col.copy(isNew = false, targetType = "mark", targetKey = mKey))
                                dropdownOpen = false
                            }
                        )
                    }
                    HorizontalDivider()
                    Repo.TEXT_KEYS.forEach { tKey ->
                        val isTextSel = col.targetType == "text" && col.targetKey == tKey
                        DropdownMenuItem(
                            modifier = Modifier.background(if (isTextSel) TealContainer else Color.Transparent),
                            text = {
                                Text(
                                    Repo.friendlyTextHeader(tKey, com.kgps.result.CsvHeaderStyle.FRIENDLY_BN),
                                    fontSize = 12.sp,
                                    fontWeight = if (isTextSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isTextSel) TealDark else SlateText
                                )
                            },
                            trailingIcon = if (isTextSel) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealDark, modifier = Modifier.size(16.dp)) }
                            } else null,
                            onClick = {
                                onUpdateCol(col.copy(isNew = false, targetType = "text", targetKey = tKey))
                                dropdownOpen = false
                            }
                        )
                    }
                    if (settings.customFields.isNotEmpty()) {
                        HorizontalDivider()
                        settings.customFields.forEach { cf ->
                            val isCustomSel = col.targetType == "custom" && col.targetKey == cf.key
                            DropdownMenuItem(
                                modifier = Modifier.background(if (isCustomSel) TealContainer else Color.Transparent),
                                text = {
                                    Text(
                                        "কাস্টম: ${cf.label}",
                                        fontSize = 12.sp,
                                        fontWeight = if (isCustomSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCustomSel) TealDark else SlateText
                                    )
                                },
                                trailingIcon = if (isCustomSel) {
                                    { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealDark, modifier = Modifier.size(16.dp)) }
                                } else null,
                                onClick = {
                                    onUpdateCol(col.copy(targetType = "custom", targetKey = cf.key))
                                    dropdownOpen = false
                                }
                            )
                        }
                    }
                }
            }
            if (col.targetType == "create_new") {
                OutlinedTextField(
                    value = col.newLabel,
                    onValueChange = { lbl -> onUpdateCol(col.copy(newLabel = lbl)) },
                    label = { Text("নতুন ডাটাবেস ফিল্ডের নাম", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CsvStepPreviewAndSave(
    report: CsvValidationReport,
    selectedRowIndices: Set<Int>,
    onToggleRow: (Int) -> Unit,
    onSelectAllValid: () -> Unit,
    onClearSelection: () -> Unit,
    filter: CsvPreviewFilter,
    onFilterChange: (CsvPreviewFilter) -> Unit,
    onJumpToInput: () -> Unit
) {
    if (report.totalRows == 0) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Rounded.UploadFile, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(10.dp))
            Text("এখনো কোনো CSV ডেটা পাওয়া যায়নি", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SlateText)
            Spacer(Modifier.height(4.dp))
            Text(
                "আগের ধাপে AI-এর দেওয়া CSV ডেটা পেস্ট করুন অথবা .csv ফাইল আপলোড করুন।",
                fontSize = 12.sp,
                color = SlateMuted
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onJumpToInput,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.testTag("csv_empty_go_input_btn")
            ) {
                Text("CSV পেস্ট / আপলোড ধাপে যান")
            }
        }
        return
    }

    val filteredRows = remember(report.rows, filter) {
        when (filter) {
            CsvPreviewFilter.ALL -> report.rows
            CsvPreviewFilter.ISSUES -> report.rows.filter { it.status != CsvRowStatus.VALID }
            CsvPreviewFilter.ADD -> report.rows.filter { it.status != CsvRowStatus.ERROR && !it.isUpdate }
            CsvPreviewFilter.UPDATE -> report.rows.filter { it.status != CsvRowStatus.ERROR && it.isUpdate }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (report.hasMissingRollOrNameMapping) {
            Surface(
                color = RoseBg,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, RoseDanger.copy(alpha = 0.35f))
            ) {
                Text(
                    "কমপক্ষে 'রোল (roll)' অথবা 'শিক্ষার্থীর নাম (name)' কলাম ম্যাপ করতে হবে। ধাপ ৩ থেকে কলাম ম্যাপিং ঠিক করুন।",
                    fontSize = 11.5.sp,
                    color = RoseDanger,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Summary metrics strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MetricPill("নতুন", bn(report.validAddCount.toString()), EmeraldGrade, TealSoft, Modifier.weight(1f))
            MetricPill("হালনাগাদ", bn(report.validUpdateCount.toString()), TealDark, TealContainer.copy(alpha = 0.55f), Modifier.weight(1f))
            MetricPill("সতর্কতা", bn(report.warningCount.toString()), AmberNotice, AmberBg, Modifier.weight(1f))
            MetricPill("ত্রুটি/বাদ", bn(report.errorCount.toString()), RoseDanger, RoseBg, Modifier.weight(1f))
        }

        // Filters & bulk select
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CsvPreviewFilter.entries.forEach { f ->
                    val isFilterSel = filter == f
                    FilterChip(
                        selected = isFilterSel,
                        onClick = { onFilterChange(f) },
                        colors = appFilterChipColors(),
                        border = appFilterChipBorder(isFilterSel),
                        label = {
                            Text(
                                f.labelBn,
                                fontSize = 11.sp,
                                fontWeight = if (isFilterSel) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("csv_preview_filter_${f.id}")
                    )
                }
            }
            Row {
                TextButton(
                    onClick = onSelectAllValid,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("csv_select_all_valid_btn")
                ) {
                    Text("বৈধ সব", fontSize = 11.sp)
                }
                TextButton(
                    onClick = onClearSelection,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("csv_clear_sel_btn")
                ) {
                    Text("বাদ", fontSize = 11.sp, color = SlateMuted)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredRows, key = { it.dataRowIndex }) { row ->
                CsvPreviewRowCard(
                    row = row,
                    selected = selectedRowIndices.contains(row.dataRowIndex),
                    onToggle = { onToggleRow(row.dataRowIndex) }
                )
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    textColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = textColor)
            Text(label, fontSize = 10.5.sp, color = textColor, maxLines = 1)
        }
    }
}

@Composable
private fun CsvPreviewRowCard(
    row: CsvValidatedRow,
    selected: Boolean,
    onToggle: () -> Unit
) {
    val isError = row.status == CsvRowStatus.ERROR
    val bg = when (row.status) {
        CsvRowStatus.ERROR -> RoseBg
        CsvRowStatus.WARNING -> AmberBg
        CsvRowStatus.VALID -> if (selected) TealContainer.copy(alpha = 0.4f) else Color.White
    }
    val border = when (row.status) {
        CsvRowStatus.ERROR -> RoseDanger.copy(alpha = 0.4f)
        CsvRowStatus.WARNING -> if (selected) AmberNotice else AmberBorder
        CsvRowStatus.VALID -> if (selected) TealPrimary else SlateOutline
    }
    val badgeText = when {
        isError -> "ত্রুটি"
        row.isUpdate -> "হালনাগাদ"
        else -> "নতুন"
    }
    val badgeColor = when {
        isError -> RoseDanger
        row.isUpdate -> TealPrimary
        else -> EmeraldGrade
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!isError) Modifier.clickable(onClick = onToggle) else Modifier),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = BorderStroke(if (!isError && selected) 1.8.dp else 1.dp, border),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Checkbox(
                checked = !isError && selected,
                onCheckedChange = { if (!isError) onToggle() },
                enabled = !isError,
                modifier = Modifier.testTag("csv_row_check_${row.dataRowIndex}")
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val clsStr = row.resolvedCls?.let { classShortBn(it) } ?: "?"
                    val rollStr = row.resolvedRoll?.let { "রোল ${bn(it.toString())}" } ?: "স্বয়ংক্রিয় রোল"
                    val nameStr = row.resolvedName.ifBlank { "(নাম নেই)" }
                    Text(
                        "$clsStr · $rollStr · $nameStr",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SlateText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = badgeColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (row.parsedMarksCount > 0 || row.parsedFieldsPreview.isNotBlank()) {
                    val info = buildString {
                        if (row.parsedMarksCount > 0) append("${bn(row.parsedMarksCount.toString())}টি নম্বর · ")
                        append(row.parsedFieldsPreview)
                    }
                    Text(
                        info,
                        fontSize = 11.sp,
                        color = SlateMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (row.messages.isNotEmpty()) {
                    val msgIcon = if (isError) Icons.Rounded.ErrorOutline else Icons.Rounded.WarningAmber
                    val msgColor = if (isError) RoseDanger else AmberNotice
                    row.messages.forEach { msg ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(msgIcon, contentDescription = null, tint = msgColor, modifier = Modifier.size(14.dp))
                            Text(msg, fontSize = 11.sp, color = msgColor)
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldGrade, modifier = Modifier.size(13.dp))
                        Text("সারি ${bn(row.lineNumber.toString())} · সব তথ্য বৈধ", fontSize = 10.5.sp, color = EmeraldGrade)
                    }
                }
            }
        }
    }
}
