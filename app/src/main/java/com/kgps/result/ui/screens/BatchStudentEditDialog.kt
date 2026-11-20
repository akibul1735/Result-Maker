package com.kgps.result.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kgps.result.CLSN
import com.kgps.result.DEFAULT_DISTRICT
import com.kgps.result.DEFAULT_UPAZILA
import com.kgps.result.RELIGIONS
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.bn
import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.fromBn
import com.kgps.result.ui.theme.EmeraldGrade
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

data class BatchStudentUpdatePatch(
    val updateUpazila: Boolean = false,
    val upazila: String = DEFAULT_UPAZILA,
    val updateDistrict: Boolean = false,
    val district: String = DEFAULT_DISTRICT,
    val updateReligion: Boolean = false,
    val religion: String = RELIGIONS.first(),
    val updateUnion: Boolean = false,
    val union: String = "",
    val updateVillage: Boolean = false,
    val village: String = "",
    val customFieldUpdates: Map<String, String> = emptyMap()
) {
    val activeFieldCount: Int
        get() = listOf(updateUpazila, updateDistrict, updateReligion, updateUnion, updateVillage).count { it } +
            customFieldUpdates.size

    val hasAnyField: Boolean
        get() = activeFieldCount > 0
}

@Composable
fun BatchStudentEditDialog(
    students: List<Student>,
    settings: Settings,
    permissions: TeacherPermissions,
    initialClassFilter: Int? = null,
    onDismiss: () -> Unit,
    onApply: (studentIds: Set<String>, patch: BatchStudentUpdatePatch) -> Unit
) {
    val allowedClasses = remember(permissions) { permissions.effectiveClasses() }
    val accessibleStudents = remember(students, allowedClasses) {
        students
            .filter { it.cls in allowedClasses }
            .sortedWith(compareBy({ it.cls }, { it.roll }))
    }

    var step by remember { mutableStateOf(0) } // 0 = Select students, 1 = Configure fields
    var classFilter by remember(initialClassFilter, allowedClasses) {
        mutableStateOf(if (initialClassFilter != null && initialClassFilter in allowedClasses) initialClassFilter else 0)
    }
    var searchQuery by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    var updateUpazila by remember { mutableStateOf(false) }
    var upazilaText by remember { mutableStateOf(DEFAULT_UPAZILA) }
    var updateDistrict by remember { mutableStateOf(false) }
    var districtText by remember { mutableStateOf(DEFAULT_DISTRICT) }
    var updateReligion by remember { mutableStateOf(false) }
    var religionChoice by remember { mutableStateOf(RELIGIONS.first()) }
    var updateUnion by remember { mutableStateOf(false) }
    var unionText by remember { mutableStateOf("") }
    var updateVillage by remember { mutableStateOf(false) }
    var villageText by remember { mutableStateOf("") }
    var enabledCustomKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var customValues by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val filteredStudents = remember(accessibleStudents, classFilter, searchQuery) {
        val q = searchQuery.trim().lowercase()
        val qEn = fromBn(q)
        accessibleStudents.filter { s ->
            val clsOk = classFilter == 0 || s.cls == classFilter
            if (!clsOk) return@filter false
            if (q.isEmpty()) return@filter true
            val rollStr = s.roll.toString()
            val rollBn = bn(rollStr)
            listOf(
                s.name,
                rollStr,
                rollBn,
                s.village,
                s.union,
                s.upazila,
                s.district,
                s.religion,
                s.guardian
            ).any { field ->
                val lower = field.lowercase()
                lower.contains(q) || lower.contains(qEn)
            }
        }
    }

    val currentPatch = BatchStudentUpdatePatch(
        updateUpazila = updateUpazila,
        upazila = upazilaText,
        updateDistrict = updateDistrict,
        district = districtText,
        updateReligion = updateReligion,
        religion = religionChoice,
        updateUnion = updateUnion,
        union = unionText,
        updateVillage = updateVillage,
        village = villageText,
        customFieldUpdates = enabledCustomKeys.associateWith { k -> customValues[k].orEmpty() }
    )

    BackHandler {
        if (step == 1) step = 0 else onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 860.dp)
                .heightIn(min = 500.dp, max = 760.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            tonalElevation = 4.dp
        ) {
            Column(Modifier.fillMaxSize()) {
                // Header
                Surface(color = TealSoft) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Rounded.EditNote,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        "শিক্ষার্থীদের তথ্য ব্যাচ পরিবর্তন",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TealDark
                                    )
                                    Text(
                                        "একাধিক শিক্ষার্থী সিলেক্ট করে উপজেলা, জেলা, ধর্ম, ইউনিয়ন বা গ্রাম বদলান",
                                        fontSize = 11.sp,
                                        color = SlateMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(48.dp).testTag("batch_edit_close_btn")
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "বন্ধ করুন", tint = SlateText)
                            }
                        }

                        // 2-step switcher
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = if (step == 0) TealPrimary else Color.White,
                                border = BorderStroke(1.dp, if (step == 0) TealPrimary else TealContainer),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { step = 0 }
                                    .testTag("batch_edit_step_select")
                            ) {
                                Text(
                                    "১. শিক্ষার্থী নির্বাচন (${bn(selectedIds.size.toString())} জন)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (step == 0) Color.White else TealDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                            Surface(
                                color = if (step == 1) TealPrimary else Color.White,
                                border = BorderStroke(1.dp, if (step == 1) TealPrimary else TealContainer),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        if (selectedIds.isNotEmpty()) step = 1
                                    }
                                    .testTag("batch_edit_step_fields")
                            ) {
                                Text(
                                    "২. তথ্য পরিবর্তন (${bn(currentPatch.activeFieldCount.toString())}টি)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (step == 1) Color.White else if (selectedIds.isNotEmpty()) TealDark else SlateMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = SlateOutline)

                if (step == 0) {
                    // Step 1: Filter + Select existing students
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    "নাম, রোল, গ্রাম, উপজেলা বা ধর্ম লিখে খুঁজুন...",
                                    fontSize = 12.5.sp
                                )
                            },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "খোঁজা মুছুন")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("batch_edit_search_input")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = classFilter == 0,
                                onClick = { classFilter = 0 },
                                colors = appFilterChipColors(),
                                border = appFilterChipBorder(classFilter == 0),
                                label = { Text("সব শ্রেণি (${bn(accessibleStudents.size.toString())})", fontSize = 11.5.sp) }
                            )
                            allowedClasses.forEach { c ->
                                val cnt = accessibleStudents.count { it.cls == c }
                                val isSel = classFilter == c
                                FilterChip(
                                    selected = isSel,
                                    onClick = { classFilter = c },
                                    colors = appFilterChipColors(),
                                    border = appFilterChipBorder(isSel),
                                    label = { Text("${CLSN[c - 1]} (${bn(cnt.toString())})", fontSize = 11.5.sp) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "দৃশ্যমান: ${bn(filteredStudents.size.toString())} জন · নির্বাচিত: ${bn(selectedIds.size.toString())} জন",
                                fontSize = 11.5.sp,
                                color = SlateMuted,
                                fontWeight = FontWeight.Medium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        selectedIds = selectedIds + filteredStudents.map { it.id }
                                    },
                                    enabled = filteredStudents.isNotEmpty(),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("batch_edit_select_all_btn")
                                ) {
                                    Icon(Icons.Rounded.DoneAll, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("সব বেছে নিন", fontSize = 11.sp)
                                }
                                if (selectedIds.isNotEmpty()) {
                                    TextButton(
                                        onClick = { selectedIds = emptySet() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("batch_edit_clear_btn")
                                    ) {
                                        Text("মুছুন", fontSize = 11.sp, color = SlateMuted)
                                    }
                                }
                            }
                        }

                        if (filteredStudents.isEmpty()) {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "এই ফিল্টারে কোনো শিক্ষার্থী পাওয়া যায়নি",
                                    fontSize = 13.sp,
                                    color = SlateMuted
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredStudents, key = { it.id }) { s ->
                                    val checked = s.id in selectedIds
                                    Surface(
                                        color = if (checked) TealContainer.copy(alpha = 0.7f) else SlateBg,
                                        border = BorderStroke(if (checked) 2.dp else 1.dp, if (checked) TealPrimary else SlateOutline),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedIds = if (checked) selectedIds - s.id else selectedIds + s.id
                                            }
                                            .testTag("batch_edit_student_${s.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Checkbox(
                                                checked = checked,
                                                onCheckedChange = { isChk ->
                                                    selectedIds = if (isChk) selectedIds + s.id else selectedIds - s.id
                                                }
                                            )
                                            Surface(
                                                color = SlateSurfaceVariant,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    "${CLSN[s.cls - 1].take(2)}·${bn(s.roll.toString().padStart(2, '0'))}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TealDark,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    s.name.ifBlank { "(নাম নেই)" },
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    color = SlateText,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val infoLine = listOfNotNull(
                                                    s.village.takeIf { it.isNotBlank() }?.let { "গ্রাম: $it" },
                                                    s.union.takeIf { it.isNotBlank() }?.let { "ইউনিয়ন: $it" },
                                                    "উপজেলা: ${s.upazila.ifBlank { DEFAULT_UPAZILA }}",
                                                    "জেলা: ${s.district.ifBlank { DEFAULT_DISTRICT }}",
                                                    "ধর্ম: ${s.religion}"
                                                ).joinToString(" · ")
                                                Text(
                                                    infoLine,
                                                    fontSize = 10.5.sp,
                                                    color = SlateMuted,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Step 2: Choose fields & new values
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val previewNames = remember(accessibleStudents, selectedIds) {
                            accessibleStudents
                                .filter { it.id in selectedIds }
                                .take(4)
                                .joinToString(", ") { it.name.ifBlank { "রোল ${bn(it.roll.toString())}" } }
                        }
                        Surface(
                            color = TealSoft,
                            border = BorderStroke(1.dp, TealContainer),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "নির্বাচিত শিক্ষার্থী: ${bn(selectedIds.size.toString())} জন",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TealDark
                                )
                                if (previewNames.isNotBlank()) {
                                    Text(
                                        previewNames + if (selectedIds.size > 4) " এবং আরও ${bn((selectedIds.size - 4).toString())} জন" else "",
                                        fontSize = 11.sp,
                                        color = SlateMuted
                                    )
                                }
                            }
                        }

                        Text(
                            "যে তথ্যগুলো পরিবর্তন করতে চান শুধু সেগুলোর সুইচ চালু করে নতুন মান লিখুন:",
                            fontSize = 12.sp,
                            color = SlateMuted
                        )

                        BatchToggleFieldCard(
                            title = "উপজেলা / থানা",
                            enabled = updateUpazila,
                            onToggle = { updateUpazila = it },
                            testTag = "batch_toggle_upazila"
                        ) {
                            OutlinedTextField(
                                value = upazilaText,
                                onValueChange = { upazilaText = it },
                                label = { Text("নতুন উপজেলার নাম") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("batch_input_upazila")
                            )
                        }

                        BatchToggleFieldCard(
                            title = "জেলা",
                            enabled = updateDistrict,
                            onToggle = { updateDistrict = it },
                            testTag = "batch_toggle_district"
                        ) {
                            OutlinedTextField(
                                value = districtText,
                                onValueChange = { districtText = it },
                                label = { Text("নতুন জেলার নাম") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("batch_input_district")
                            )
                        }

                        BatchToggleFieldCard(
                            title = "ধর্ম",
                            enabled = updateReligion,
                            onToggle = { updateReligion = it },
                            testTag = "batch_toggle_religion"
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                RELIGIONS.forEach { rel ->
                                    val isSel = religionChoice == rel
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { religionChoice = rel },
                                        colors = appFilterChipColors(),
                                        border = appFilterChipBorder(isSel),
                                        label = { Text(rel, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }

                        BatchToggleFieldCard(
                            title = "ইউনিয়ন (ঐচ্ছিক)",
                            enabled = updateUnion,
                            onToggle = { updateUnion = it },
                            testTag = "batch_toggle_union"
                        ) {
                            OutlinedTextField(
                                value = unionText,
                                onValueChange = { unionText = it },
                                label = { Text("নতুন ইউনিয়নের নাম (ফাঁকা রাখলে মুছে যাবে)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("batch_input_union")
                            )
                        }

                        BatchToggleFieldCard(
                            title = "গ্রাম / মহল্লা",
                            enabled = updateVillage,
                            onToggle = { updateVillage = it },
                            testTag = "batch_toggle_village"
                        ) {
                            OutlinedTextField(
                                value = villageText,
                                onValueChange = { villageText = it },
                                label = { Text("নতুন গ্রামের নাম") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("batch_input_village")
                            )
                        }

                        if (settings.customFields.isNotEmpty()) {
                            Text(
                                "কাস্টম ফিল্ডসমূহ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateText,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            settings.customFields.forEach { cf ->
                                val isCfOn = cf.key in enabledCustomKeys
                                BatchToggleFieldCard(
                                    title = cf.label,
                                    enabled = isCfOn,
                                    onToggle = { on ->
                                        enabledCustomKeys = if (on) enabledCustomKeys + cf.key else enabledCustomKeys - cf.key
                                    },
                                    testTag = "batch_toggle_cf_${cf.key}"
                                ) {
                                    OutlinedTextField(
                                        value = customValues[cf.key].orEmpty(),
                                        onValueChange = { v -> customValues = customValues + (cf.key to v) },
                                        label = { Text("${cf.label} (নতুন মান)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = SlateOutline)

                // Footer Navigation / Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (step == 0) {
                        TextButton(onClick = onDismiss) {
                            Text("বাতিল")
                        }
                        Button(
                            onClick = { step = 1 },
                            enabled = selectedIds.isNotEmpty(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("batch_edit_next_btn")
                        ) {
                            Text("পরবর্তী (${bn(selectedIds.size.toString())} জন)")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { step = 0 },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("পূর্ববর্তী")
                        }
                        Button(
                            onClick = { onApply(selectedIds, currentPatch) },
                            enabled = selectedIds.isNotEmpty() && currentPatch.hasAnyField,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrade),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("batch_edit_apply_btn")
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "${bn(selectedIds.size.toString())} জনে আপডেট করুন",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchToggleFieldCard(
    title: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    Surface(
        color = if (enabled) TealContainer.copy(alpha = 0.55f) else SlateBg,
        border = BorderStroke(if (enabled) 1.8.dp else 1.dp, if (enabled) TealPrimary else SlateOutline),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(!enabled) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    color = if (enabled) TealDark else SlateText
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag(testTag)
                )
            }
            if (enabled) {
                content()
            }
        }
    }
}
