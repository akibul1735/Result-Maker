package com.kgps.result.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.CLS_SHORT
import com.kgps.result.Repo
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.bn
import com.kgps.result.ui.theme.AmberBg
import com.kgps.result.ui.theme.AmberBorder
import com.kgps.result.ui.theme.AmberNotice
import com.kgps.result.ui.theme.EmeraldGrade
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.TealPrimary

@Composable
fun SmartCsvImportDialog(
    students: List<Student>,
    settings: Settings,
    defaultCls: Int,
    initialText: String,
    onDismiss: () -> Unit,
    onImported: (Repo.CsvImportSummary) -> Unit,
    onError: (String) -> Unit
) {
    val ctx = LocalContext.current
    var rawText by remember(initialText) { mutableStateOf(initialText) }
    var fallbackCls by remember(defaultCls) { mutableStateOf(defaultCls) }
    val parsedRows = remember(rawText) { if (rawText.isBlank()) emptyList() else Repo.parseCsv(rawText) }
    var columns by remember(parsedRows, settings.customFields) {
        mutableStateOf(Repo.detectCsvColumns(parsedRows, settings.customFields))
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                rawText = ctx.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    ?: ""
            } catch (e: Exception) {
                onError("ফাইল পড়া যায়নি: ${e.message ?: "ত্রুটি"}")
            }
        }
    }

    val summary = remember(parsedRows, columns, students, settings, fallbackCls) {
        if (parsedRows.size >= 2 && columns.isNotEmpty()) {
            Repo.applyMappedCsvImport(parsedRows, columns, students, settings, fallbackCls)
        } else null
    }
    val hasRollOrName = columns.any {
        it.targetType == "standard" && (it.targetKey == "roll" || it.targetKey == "name")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("স্মার্ট CSV / Excel ইমপোর্ট", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { filePicker.launch(arrayOf("*/*")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("csv_pick_file_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("CSV / TSV ফাইল নির্বাচন করুন", fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    label = { Text("অথবা Excel/CSV ডাটা এখানে পেস্ট করুন", fontSize = 11.sp) },
                    minLines = if (parsedRows.isEmpty()) 4 else 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("csv_raw_input")
                )

                if (parsedRows.isNotEmpty()) {
                    val newCols = columns.count { it.isNew }
                    val matchedCols = columns.size - newCols
                    Text(
                        "মোট কলাম: ${bn(columns.size.toString())}টি · শনাক্ত: ${bn(matchedCols.toString())}টি · নতুন: ${bn(newCols.toString())}টি",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (newCols > 0) AmberNotice else TealPrimary
                    )

                    if (newCols > 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    columns = columns.map {
                                        if (it.isNew) it.copy(targetType = "create_new", newLabel = it.header) else it
                                    }
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
                                    columns = columns.map {
                                        if (it.isNew) it.copy(targetType = "skip") else it
                                    }
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

                    Text("ডিফল্ট শ্রেণি (CSV-তে না থাকলে):", fontSize = 11.sp, color = SlateMuted)
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        (1..5).forEach { c ->
                            FilterChip(
                                selected = fallbackCls == c,
                                onClick = { fallbackCls = c },
                                label = { Text(CLS_SHORT[c - 1], fontSize = 11.sp) }
                            )
                        }
                    }

                    columns.forEachIndexed { idx, col ->
                        var dropdownOpen by remember(col.colIndex) { mutableStateOf(false) }
                        val targetDesc = when (col.targetType) {
                            "create_new" -> "+ নতুন ফিল্ড: ${col.newLabel}"
                            "skip" -> "✕ বাদ দিন"
                            "standard" -> Repo.STANDARD_FIELDS.find { it.first == col.targetKey }?.second ?: col.targetKey
                            "custom" -> "কাস্টম: ${settings.customFields.find { it.key == col.targetKey }?.label ?: col.targetKey}"
                            else -> col.targetKey
                        }

                        Card(
                            Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = if (col.isNew) AmberBg else SlateBg),
                            border = BorderStroke(1.dp, if (col.isNew) AmberBorder else SlateOutline),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${bn((idx + 1).toString())}. ${col.header}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        if (col.isNew) "নতুন কলাম" else "মিলেছে",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (col.isNew) AmberNotice else EmeraldGrade
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
                                        DropdownMenuItem(
                                            text = { Text("+ নতুন ফিল্ড তৈরি করুন (ডাটাবেসে যোগ হবে)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AmberNotice) },
                                            onClick = {
                                                columns = columns.map { if (it.colIndex == col.colIndex) it.copy(targetType = "create_new", newLabel = it.header) else it }
                                                dropdownOpen = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("✕ বাদ দিন (ইমপোর্ট করবেন না)", fontSize = 12.sp, color = SlateMuted) },
                                            onClick = {
                                                columns = columns.map { if (it.colIndex == col.colIndex) it.copy(targetType = "skip") else it }
                                                dropdownOpen = false
                                            }
                                        )
                                        HorizontalDivider()
                                        Repo.STANDARD_FIELDS.forEach { (fKey, fLabel) ->
                                            DropdownMenuItem(
                                                text = { Text(fLabel, fontSize = 12.sp) },
                                                onClick = {
                                                    columns = columns.map { if (it.colIndex == col.colIndex) it.copy(targetType = "standard", targetKey = fKey) else it }
                                                    dropdownOpen = false
                                                }
                                            )
                                        }
                                        if (settings.customFields.isNotEmpty()) {
                                            HorizontalDivider()
                                            settings.customFields.forEach { cf ->
                                                DropdownMenuItem(
                                                    text = { Text("কাস্টম: ${cf.label}", fontSize = 12.sp) },
                                                    onClick = {
                                                        columns = columns.map { if (it.colIndex == col.colIndex) it.copy(targetType = "custom", targetKey = cf.key) else it }
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
                                        onValueChange = { lbl ->
                                            columns = columns.map { if (it.colIndex == col.colIndex) it.copy(newLabel = lbl) else it }
                                        },
                                        label = { Text("নতুন ডাটাবেস ফিল্ডের নাম", fontSize = 10.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    if (summary != null) {
                        Text(
                            "নতুন ${bn(summary.addedCount.toString())} জন · হালনাগাদ ${bn(summary.updatedCount.toString())} জন · নতুন ফিল্ড ${bn(summary.createdFields.size.toString())}টি",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (summary != null) onImported(summary) },
                enabled = summary != null && hasRollOrName && (summary.addedCount + summary.updatedCount > 0),
                modifier = Modifier.testTag("csv_confirm_import_btn")
            ) {
                Text("ইমপোর্ট করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("csv_cancel_btn")) {
                Text("বাতিল")
            }
        }
    )
}
