package com.kgps.result.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kgps.result.CLSN
import com.kgps.result.Student
import com.kgps.result.bn
import com.kgps.result.fromBn
import com.kgps.result.ui.theme.AmberBg
import com.kgps.result.ui.theme.AmberBorder
import com.kgps.result.ui.theme.AmberNotice
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

enum class BatchReportScope(val labelBn: String) {
    CLASS("শ্রেণি ভিত্তিক"),
    CUSTOM("ব্যাচ বাছাই"),
    ALL("সকল শ্রেণি")
}

@Composable
fun BatchReportDialog(
    sortedStudents: List<Student>,
    initialClass: Int,
    includeClassHighest: Boolean = false,
    onToggleClassHighest: (Boolean) -> Unit = {},
    onPrintBatch: (students: List<Student>, jobName: String) -> Unit,
    onExportBatchPdf: (students: List<Student>, fileName: String) -> Unit,
    onDismiss: () -> Unit
) {
    val availableClasses = remember(sortedStudents) {
        sortedStudents.map { it.cls }.distinct().sorted()
    }
    val classCounts = remember(sortedStudents) {
        sortedStudents.groupingBy { it.cls }.eachCount()
    }

    var scope by remember { mutableStateOf(BatchReportScope.CLASS) }
    var selectedClass by remember(initialClass, availableClasses) {
        mutableStateOf(
            if (initialClass in availableClasses) initialClass
            else availableClasses.firstOrNull() ?: 1
        )
    }

    // Custom batch selection state (persists across filter changes inside the dialog)
    var customFilterCls by remember(selectedClass) { mutableStateOf<Int?>(selectedClass) }
    var searchQuery by remember { mutableStateOf("") }
    var rollFromText by remember { mutableStateOf("") }
    var rollToText by remember { mutableStateOf("") }
    var selectedIds by remember(sortedStudents, selectedClass) {
        mutableStateOf(sortedStudents.filter { it.cls == selectedClass }.map { it.id }.toSet())
    }

    val filteredCustomStudents = remember(sortedStudents, customFilterCls, searchQuery) {
        val q = searchQuery.trim()
        val qDigits = fromBn(q)
        sortedStudents.filter { s ->
            val clsOk = customFilterCls == null || s.cls == customFilterCls
            if (!clsOk) return@filter false
            if (q.isEmpty()) return@filter true
            val rStr = s.roll.toString()
            s.name.contains(q, ignoreCase = true) ||
                rStr == qDigits ||
                rStr.contains(qDigits) ||
                bn(rStr).contains(q)
        }
    }

    val targetStudents = remember(scope, sortedStudents, selectedClass, selectedIds) {
        when (scope) {
            BatchReportScope.CLASS -> sortedStudents.filter { it.cls == selectedClass }
            BatchReportScope.CUSTOM -> sortedStudents.filter { it.id in selectedIds }
            BatchReportScope.ALL -> sortedStudents
        }
    }

    val pdfFileName = remember(scope, selectedClass, targetStudents.size) {
        when (scope) {
            BatchReportScope.CLASS -> "শ্রেণি-${selectedClass}-প্রতিবেদন.pdf"
            BatchReportScope.CUSTOM -> "ব্যাচ-${targetStudents.size}জন-প্রতিবেদন.pdf"
            BatchReportScope.ALL -> "সব-শ্রেণির-প্রতিবেদন.pdf"
        }
    }

    val printJobName = remember(scope, selectedClass, targetStudents.size) {
        when (scope) {
            BatchReportScope.CLASS -> "শ্রেণি_${selectedClass}_প্রতিবেদন_${targetStudents.size}জন"
            BatchReportScope.CUSTOM -> "ব্যাচ_প্রতিবেদন_${targetStudents.size}জন"
            BatchReportScope.ALL -> "সব_শ্রেণির_প্রতিবেদন_${targetStudents.size}জন"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, SlateOutline),
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxWidth(0.95f)
                .heightIn(max = 640.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = TealSoft,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.LibraryBooks, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(19.dp))
                            }
                        }
                        Column {
                            Text(
                                "ব্যাচ প্রিন্ট ও PDF এক্সপোর্ট",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = SlateText
                            )
                            Text(
                                "শ্রেণি ভিত্তিক, কাস্টম ব্যাচ বা সকল শ্রেণি একসাথে",
                                fontSize = 11.sp,
                                color = SlateMuted
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp).testTag("batch_report_close_btn")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "বন্ধ করুন", tint = SlateMuted)
                    }
                }

                // 3 Mode Segmented Tabs
                Surface(
                    color = SlateSurfaceVariant,
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        BatchReportScope.entries.forEach { mode ->
                            val isSel = scope == mode
                            Surface(
                                color = if (isSel) TealPrimary else Color.Transparent,
                                shape = RoundedCornerShape(7.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { scope = mode }
                                    .testTag("batch_scope_${mode.name.lowercase()}")
                            ) {
                                Text(
                                    text = mode.labelBn,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else SlateText,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 9.dp)
                                )
                            }
                        }
                    }
                }

                // Body per mode
                Box(modifier = Modifier.weight(1f, fill = false).fillMaxWidth()) {
                    when (scope) {
                        BatchReportScope.CLASS -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("কোন শ্রেণির প্রতিবেদন তৈরি করবেন?", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SlateText)
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    availableClasses.forEach { c ->
                                        val isSel = selectedClass == c
                                        val count = classCounts[c] ?: 0
                                        FilterChip(
                                            selected = isSel,
                                            onClick = { selectedClass = c },
                                            colors = appFilterChipColors(),
                                            border = appFilterChipBorder(isSel),
                                            modifier = Modifier.testTag("batch_class_chip_$c"),
                                            label = {
                                                Text(
                                                    "${CLSN[c - 1]} (${bn(count.toString())})",
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                        )
                                    }
                                }

                                val clsCount = classCounts[selectedClass] ?: 0
                                Surface(
                                    color = TealSoft.copy(alpha = 0.7f),
                                    border = BorderStroke(1.dp, TealContainer),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Rounded.School, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                                            Text(
                                                "${CLSN[selectedClass - 1]} শ্রেণির পূর্ণাঙ্গ ব্যাচ",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = TealDark
                                            )
                                        }
                                        Text(
                                            "মোট ${bn(clsCount.toString())} জন শিক্ষার্থী · রোল ক্রমানুসারে মোট ${bn((clsCount * 2).toString())} পৃষ্ঠা (A4 ডাবল-পেজ বুকলেট)",
                                            fontSize = 12.sp,
                                            color = SlateText,
                                            lineHeight = 17.sp
                                        )
                                        TextButton(
                                            onClick = {
                                                customFilterCls = selectedClass
                                                selectedIds = sortedStudents.filter { it.cls == selectedClass }.map { it.id }.toSet()
                                                scope = BatchReportScope.CUSTOM
                                            },
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                            modifier = Modifier.testTag("batch_customize_this_class_btn")
                                        ) {
                                            Text("এই শ্রেণি থেকে নির্দিষ্ট রোল বাছাই করুন →", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = TealPrimary)
                                        }
                                    }
                                }
                            }
                        }

                        BatchReportScope.CUSTOM -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Class Filter Chips
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    FilterChip(
                                        selected = customFilterCls == null,
                                        onClick = { customFilterCls = null },
                                        colors = appFilterChipColors(),
                                        border = appFilterChipBorder(customFilterCls == null),
                                        label = { Text("সব শ্রেণি", fontSize = 11.sp) }
                                    )
                                    availableClasses.forEach { c ->
                                        val isSel = customFilterCls == c
                                        FilterChip(
                                            selected = isSel,
                                            onClick = { customFilterCls = c },
                                            colors = appFilterChipColors(),
                                            border = appFilterChipBorder(isSel),
                                            label = { Text("${CLSN[c - 1]} (${bn((classCounts[c] ?: 0).toString())})", fontSize = 11.sp) }
                                        )
                                    }
                                }

                                // Quick Search + Roll Range Picker
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("নাম বা রোল দিয়ে খুঁজুন...", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp)) },
                                    trailingIcon = if (searchQuery.isNotEmpty()) {
                                        {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Rounded.Close, contentDescription = "মুছুন", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    } else null,
                                    singleLine = true,
                                    textStyle = TextStyle(fontSize = 12.5.sp),
                                    modifier = Modifier.fillMaxWidth().testTag("batch_custom_search_input")
                                )

                                // Quick Roll Range Row + Select/Clear helpers
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text("রোল:", fontSize = 11.sp, color = SlateMuted, fontWeight = FontWeight.Medium)
                                    OutlinedTextField(
                                        value = rollFromText,
                                        onValueChange = { rollFromText = it.filter { ch -> ch.isDigit() || ch in '০'..'৯' }.take(4) },
                                        placeholder = { Text("১", fontSize = 11.sp) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        textStyle = TextStyle(fontSize = 11.5.sp, textAlign = TextAlign.Center),
                                        modifier = Modifier.width(58.dp).testTag("batch_roll_from_input")
                                    )
                                    Text("–", fontSize = 12.sp, color = SlateMuted)
                                    OutlinedTextField(
                                        value = rollToText,
                                        onValueChange = { rollToText = it.filter { ch -> ch.isDigit() || ch in '০'..'৯' }.take(4) },
                                        placeholder = { Text("১৫", fontSize = 11.sp) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        textStyle = TextStyle(fontSize = 11.5.sp, textAlign = TextAlign.Center),
                                        modifier = Modifier.width(58.dp).testTag("batch_roll_to_input")
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            val fromRoll = fromBn(rollFromText.trim()).toIntOrNull() ?: 1
                                            val toRoll = fromBn(rollToText.trim()).toIntOrNull() ?: Int.MAX_VALUE
                                            val lo = minOf(fromRoll, toRoll)
                                            val hi = maxOf(fromRoll, toRoll)
                                            val matched = filteredCustomStudents.filter { it.roll in lo..hi }.map { it.id }
                                            selectedIds = selectedIds + matched
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(7.dp),
                                        modifier = Modifier.testTag("batch_apply_roll_range_btn")
                                    ) {
                                        Text("যোগ", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Spacer(Modifier.weight(1f))
                                    TextButton(
                                        onClick = {
                                            val visibleIds = filteredCustomStudents.map { it.id }.toSet()
                                            val allVisibleSelected = visibleIds.isNotEmpty() && visibleIds.all { it in selectedIds }
                                            selectedIds = if (allVisibleSelected) selectedIds - visibleIds else selectedIds + visibleIds
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("batch_toggle_visible_btn")
                                    ) {
                                        val allSel = filteredCustomStudents.isNotEmpty() && filteredCustomStudents.all { it.id in selectedIds }
                                        Text(if (allSel) "বাদ দিন" else "সবগুলো", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    if (selectedIds.isNotEmpty()) {
                                        TextButton(
                                            onClick = { selectedIds = emptySet() },
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                            modifier = Modifier.testTag("batch_clear_all_btn")
                                        ) {
                                            Text("মুছুন", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }

                                HorizontalDivider(color = SlateOutline)

                                LazyColumn(
                                    modifier = Modifier.heightIn(min = 140.dp, max = 260.dp).fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (filteredCustomStudents.isEmpty()) {
                                        item {
                                            Text(
                                                "এই ফিল্টারে কোনো শিক্ষার্থী পাওয়া যায়নি",
                                                color = SlateMuted,
                                                fontSize = 12.5.sp,
                                                modifier = Modifier.padding(vertical = 20.dp)
                                            )
                                        }
                                    } else {
                                        items(filteredCustomStudents, key = { it.id }) { s ->
                                            val checked = s.id in selectedIds
                                            Surface(
                                                color = if (checked) TealSoft else SlateBg,
                                                border = BorderStroke(1.dp, if (checked) TealPrimary else SlateOutline),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedIds = if (checked) selectedIds - s.id else selectedIds + s.id
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Checkbox(
                                                        checked = checked,
                                                        onCheckedChange = { isChecked ->
                                                            selectedIds = if (isChecked) selectedIds + s.id else selectedIds - s.id
                                                        }
                                                    )
                                                    Surface(
                                                        color = if (checked) TealPrimary else MaterialTheme.colorScheme.surface,
                                                        shape = RoundedCornerShape(5.dp)
                                                    ) {
                                                        Text(
                                                            bn(s.roll.toString().padStart(2, '0')),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.5.sp,
                                                            color = if (checked) Color.White else TealPrimary,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Text(
                                                        s.name.ifBlank { "(নাম নেই)" },
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = SlateText,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Text(
                                                        "${CLSN[s.cls - 1]} শ্রেণি",
                                                        fontSize = 10.5.sp,
                                                        color = SlateMuted
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        BatchReportScope.ALL -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    color = TealSoft.copy(alpha = 0.7f),
                                    border = BorderStroke(1.dp, TealContainer),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Rounded.Groups, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(21.dp))
                                            Text(
                                                "সকল শ্রেণি একইসাথে (${bn(availableClasses.size.toString())}টি শ্রেণি)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = TealDark
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            availableClasses.forEach { c ->
                                                val count = classCounts[c] ?: 0
                                                Surface(
                                                    color = MaterialTheme.colorScheme.surface,
                                                    border = BorderStroke(1.dp, TealContainer),
                                                    shape = RoundedCornerShape(7.dp)
                                                ) {
                                                    Text(
                                                        "${CLSN[c - 1]}: ${bn(count.toString())} জন",
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = TealDark,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            "সব শ্রেণির মোট ${bn(sortedStudents.size.toString())} জন শিক্ষার্থীর রিপোর্ট কার্ড (১ম→৫ম ও রোল ক্রমে) মোট ${bn((sortedStudents.size * 2).toString())}টি A4 পৃষ্ঠায় তৈরি হবে।",
                                            fontSize = 12.sp,
                                            color = SlateText,
                                            lineHeight = 17.sp
                                        )
                                    }
                                }

                                if (sortedStudents.size > 60) {
                                    Surface(
                                        color = AmberBg,
                                        border = BorderStroke(1.dp, AmberBorder),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "শিক্ষার্থী সংখ্যা বেশি (${bn(sortedStudents.size.toString())} জন) হওয়ায় PDF বা প্রিন্ট প্রিভিউ তৈরি হতে কয়েক সেকেন্ড সময় লাগতে পারে।",
                                            fontSize = 11.sp,
                                            color = AmberNotice,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = SlateOutline)

                // Selection Summary Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = if (targetStudents.isNotEmpty()) TealPrimary else SlateMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (targetStudents.isEmpty()) "কোনো শিক্ষার্থী নির্বাচিত নেই"
                            else "নির্বাচিত: ${bn(targetStudents.size.toString())} জন (${bn((targetStudents.size * 2).toString())} পৃষ্ঠা)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (targetStudents.isNotEmpty()) TealDark else SlateMuted
                        )
                    }
                }

                // Optional Class Highest Column Toggle
                Surface(
                    color = if (includeClassHighest) TealSoft else SlateBg,
                    border = BorderStroke(1.dp, if (includeClassHighest) TealPrimary.copy(alpha = 0.45f) else SlateOutline),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleClassHighest(!includeClassHighest) }
                        .testTag("batch_class_highest_row")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = includeClassHighest,
                            onCheckedChange = onToggleClassHighest,
                            modifier = Modifier.testTag("batch_class_highest_checkbox")
                        )
                        Column(Modifier.weight(1f).padding(end = 6.dp)) {
                            Text(
                                "৩য় পৃষ্ঠায় 'শ্রেণিতে সর্বোচ্চ নম্বর' কলাম যোগ করুন",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SlateText
                            )
                            Text(
                                "চূড়ান্ত ফলাফলের অধীনে বিষয়ভিত্তিক শ্রেণির সর্বোচ্চ নম্বর দেখাবে",
                                fontSize = 10.5.sp,
                                color = SlateMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Print + PDF Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onPrintBatch(targetStudents, printJobName)
                        },
                        enabled = targetStudents.isNotEmpty(),
                        shape = RoundedCornerShape(9.dp),
                        border = BorderStroke(1.dp, if (targetStudents.isNotEmpty()) TealPrimary else SlateOutline),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("batch_print_confirm_btn")
                    ) {
                        Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("প্রিন্ট করুন", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                    Button(
                        onClick = {
                            onDismiss()
                            onExportBatchPdf(targetStudents, pdfFileName)
                        },
                        enabled = targetStudents.isNotEmpty(),
                        shape = RoundedCornerShape(9.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1.1f).heightIn(min = 48.dp).testTag("batch_pdf_confirm_btn")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("PDF তৈরি", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }
        }
    }
}
