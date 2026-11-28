package com.kgps.result.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kgps.result.CustomFieldDef
import com.kgps.result.PdfFontInfo
import com.kgps.result.SeatPlanNumeralMode
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.TestimonialConfig
import com.kgps.result.TestimonialPlanner
import com.kgps.result.TestimonialStudentOverride
import com.kgps.result.bn
import com.kgps.result.classShortBn
import com.kgps.result.ui.theme.AmberNotice
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft

@Composable
internal fun TestimonialFontPickerDialog(
    title: String,
    fonts: List<PdfFontInfo>,
    selectedId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                fonts.forEach { f ->
                    val isSel = f.id == selectedId
                    Surface(
                        onClick = { onSelect(f.id) },
                        color = if (isSel) TealSoft else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (isSel) TealPrimary else SlateOutline),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    f.labelBn,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) TealDark else SlateText
                                )
                                Text(f.descriptionBn, fontSize = 10.sp, color = SlateMuted)
                            }
                            if (isSel) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("বন্ধ করুন") }
        }
    )
}

@Composable
internal fun TestimonialStudentInfoEditorDialog(
    students: List<Student>,
    settings: Settings,
    config: TestimonialConfig,
    overrides: Map<String, TestimonialStudentOverride>,
    canSaveToProfile: Boolean,
    onSaveAll: (updatedOverrides: Map<String, TestimonialStudentOverride>, saveToStudentProfiles: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val editMap = remember(students, overrides) {
        mutableStateMapOf<String, TestimonialStudentOverride>().apply {
            students.forEach { st ->
                val existingOv = overrides[st.id]
                val f = TestimonialPlanner.resolveFatherName(st, settings, existingOv)
                val m = TestimonialPlanner.resolveMotherName(st, settings, config.motherFieldKeyHint, existingOv)
                val rawDob = existingOv?.dob?.takeIf { it.isNotBlank() }
                    ?: TestimonialPlanner.resolveDob(st, settings, config.dobFieldKeyHint, SeatPlanNumeralMode.BENGALI, null)
                        .removeSuffix(" খ্রি.")
                        .trim()
                put(st.id, TestimonialStudentOverride(fatherName = f, motherName = m, dob = rawDob))
            }
        }
    }
    var alsoSaveProfile by remember { mutableStateOf(canSaveToProfile) }
    var filterOnlyMissing by remember { mutableStateOf(false) }

    val visibleStudents = remember(students, filterOnlyMissing, editMap.toMap()) {
        if (!filterOnlyMissing) {
            students
        } else {
            students.filter { st ->
                val cur = editMap[st.id] ?: TestimonialStudentOverride()
                cur.fatherName.isBlank() || cur.motherName.isBlank() || cur.dob.isBlank()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 620.dp)
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("শিক্ষার্থীর পিতা, মাতা ও জন্মতারিখ", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TealDark)
                        Text(
                            "মোট ${bn(students.size.toString())} জন শিক্ষার্থী (${bn(visibleStudents.size.toString())} জন প্রদর্শিত)",
                            fontSize = 11.sp,
                            color = SlateMuted
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "বন্ধ করুন")
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AdmitCompactTogglePill(
                        label = "শুধু অসম্পূর্ণগুলো দেখান",
                        checked = filterOnlyMissing,
                        onCheckedChange = { filterOnlyMissing = it },
                        testTag = "testimonial_filter_missing_toggle"
                    )
                    if (canSaveToProfile) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = alsoSaveProfile,
                                onCheckedChange = { alsoSaveProfile = it }
                            )
                            Text(
                                "শিক্ষার্থী প্রোফাইলেও সেভ করুন",
                                fontSize = 10.5.sp,
                                color = SlateText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                HorizontalDivider(color = SlateOutline.copy(alpha = 0.6f))

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false).heightIn(max = 390.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visibleStudents, key = { it.id }) { st ->
                        val cur = editMap[st.id] ?: TestimonialStudentOverride()
                        val hasMissing = cur.fatherName.isBlank() || cur.motherName.isBlank() || cur.dob.isBlank()
                        Surface(
                            color = SlateBg,
                            border = BorderStroke(
                                1.dp,
                                if (hasMissing) AmberNotice.copy(alpha = 0.45f) else SlateOutline
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(9.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "${st.name.ifBlank { "(নাম নেই)" }} · রোল ${bn(st.roll.toString())}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = TealDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        classShortBn(st.cls, settings),
                                        fontSize = 10.5.sp,
                                        color = SlateMuted
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = cur.fatherName,
                                        onValueChange = { v -> editMap[st.id] = cur.copy(fatherName = v) },
                                        label = { Text("পিতার নাম", fontSize = 10.sp) },
                                        singleLine = true,
                                        textStyle = TextStyle(fontSize = 12.sp),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = cur.motherName,
                                        onValueChange = { v -> editMap[st.id] = cur.copy(motherName = v) },
                                        label = { Text("মাতার নাম", fontSize = 10.sp) },
                                        singleLine = true,
                                        textStyle = TextStyle(fontSize = 12.sp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                OutlinedTextField(
                                    value = cur.dob,
                                    onValueChange = { v -> editMap[st.id] = cur.copy(dob = v) },
                                    label = { Text("জন্মতারিখ (যেমন: ১০/০১/২০১৫)", fontSize = 10.sp) },
                                    singleLine = true,
                                    textStyle = TextStyle(fontSize = 12.sp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Text("বাতিল", fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            onSaveAll(editMap.toMap(), alsoSaveProfile && canSaveToProfile)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1.3f).height(42.dp).testTag("testimonial_save_students_info_btn")
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("সংরক্ষণ করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

internal fun applyStudentProfileFieldUpdates(
    students: List<Student>,
    settings: Settings,
    config: TestimonialConfig,
    updatedMap: Map<String, TestimonialStudentOverride>
): Pair<List<Student>, Settings> {
    var nextSettings = settings
    var motherKey = TestimonialPlanner.findCustomFieldKey(
        settings = nextSettings,
        hintKey = config.motherFieldKeyHint,
        keywords = listOf("মাতা", "মায়ের", "মায়ের", "mother")
    )
    var dobKey = TestimonialPlanner.findCustomFieldKey(
        settings = nextSettings,
        hintKey = config.dobFieldKeyHint,
        keywords = listOf("জন্ম", "dob", "birth")
    )

    val anyMotherInput = updatedMap.values.any { it.motherName.isNotBlank() }
    val anyDobInput = updatedMap.values.any { it.dob.isNotBlank() }

    if (anyMotherInput && motherKey == null) {
        motherKey = "CF_MOTHER_NAME"
        nextSettings = nextSettings.copy(
            customFields = nextSettings.customFields + CustomFieldDef(key = motherKey, label = "মাতার নাম")
        )
    }
    if (anyDobInput && dobKey == null) {
        dobKey = "CF_DOB"
        nextSettings = nextSettings.copy(
            customFields = nextSettings.customFields + CustomFieldDef(key = dobKey, label = "জন্মতারিখ")
        )
    }

    val updatedStudents = students.mapNotNull { st ->
        val ov = updatedMap[st.id] ?: return@mapNotNull null
        val nextGuardian = ov.fatherName.trim().ifBlank { st.guardian }
        val nextX = st.x.toMutableMap()
        if (motherKey != null && ov.motherName.isNotBlank()) {
            nextX[motherKey] = ov.motherName.trim()
        }
        if (dobKey != null && ov.dob.isNotBlank()) {
            nextX[dobKey] = ov.dob.trim()
        }
        val changed = nextGuardian != st.guardian || nextX != st.x
        if (changed) st.copy(guardian = nextGuardian, x = nextX) else null
    }
    return updatedStudents to nextSettings
}
