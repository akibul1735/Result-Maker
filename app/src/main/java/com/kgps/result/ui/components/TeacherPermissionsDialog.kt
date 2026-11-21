package com.kgps.result.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.SchoolClassDef
import com.kgps.result.activeSchoolClasses
import com.kgps.result.bn
import com.kgps.result.classNameBn
import com.kgps.result.cloud.ALL_SUBJECT_KEYS
import com.kgps.result.cloud.SUBJECT_ACCESS_DEFS
import com.kgps.result.cloud.TeacherPermissions
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeacherPermissionsEditorDialog(
    title: String,
    teacherLabel: String,
    initialPermissions: TeacherPermissions,
    availableClasses: List<SchoolClassDef> = activeSchoolClasses(),
    confirmButtonText: String,
    onDismiss: () -> Unit,
    onConfirm: (TeacherPermissions) -> Unit
) {
    val allClassIds = remember(availableClasses) { availableClasses.map { it.id } }
    var allClasses by remember(initialPermissions) { mutableStateOf(initialPermissions.allClasses) }
    var selectedClasses by remember(initialPermissions, allClassIds) {
        mutableStateOf(
            if (initialPermissions.allClasses) allClassIds.toSet()
            else initialPermissions.allowedClasses.toSet()
        )
    }
    var allSubjects by remember(initialPermissions) { mutableStateOf(initialPermissions.allSubjects) }
    var selectedSubjects by remember(initialPermissions) {
        mutableStateOf(
            if (initialPermissions.allSubjects) ALL_SUBJECT_KEYS.toSet()
            else initialPermissions.allowedSubjects.toSet()
        )
    }
    var canViewOtherSubjects by remember(initialPermissions) { mutableStateOf(initialPermissions.canViewOtherSubjects) }
    var canEditAttendance by remember(initialPermissions) { mutableStateOf(initialPermissions.canEditAttendance) }
    var canManageStudents by remember(initialPermissions) { mutableStateOf(initialPermissions.canManageStudents) }
    var canExportPdf by remember(initialPermissions) { mutableStateOf(initialPermissions.canExportPdf) }

    fun buildResult(): TeacherPermissions = TeacherPermissions(
        allClasses = allClasses,
        allowedClasses = if (allClasses) allClassIds else selectedClasses.sorted(),
        allSubjects = allSubjects,
        allowedSubjects = if (allSubjects) ALL_SUBJECT_KEYS else ALL_SUBJECT_KEYS.filter { it in selectedSubjects },
        canViewOtherSubjects = canViewOtherSubjects,
        canEditAttendance = canEditAttendance,
        canManageStudents = canManageStudents,
        canExportPdf = canExportPdf
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (teacherLabel.isNotBlank()) {
                    Text(teacherLabel, fontSize = 12.sp, color = SlateMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quick Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            allClasses = true
                            selectedClasses = allClassIds.toSet()
                            allSubjects = true
                            selectedSubjects = ALL_SUBJECT_KEYS.toSet()
                            canViewOtherSubjects = true
                            canEditAttendance = true
                            canManageStudents = true
                            canExportPdf = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("সব অ্যাক্সেস", fontSize = 11.5.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            allClasses = true
                            selectedClasses = allClassIds.toSet()
                            allSubjects = false
                            if (selectedSubjects.size == ALL_SUBJECT_KEYS.size || selectedSubjects.isEmpty()) {
                                selectedSubjects = setOf("BAN")
                            }
                            canViewOtherSubjects = false
                            canEditAttendance = false
                            canManageStudents = false
                            canExportPdf = false
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("শুধু নির্ধারিত বিষয়", fontSize = 11.5.sp)
                    }
                }

                // Class Permissions
                Surface(
                    color = SlateBg,
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("কোন শ্রেণির ডেটা অ্যাক্সেস করবেন", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = SlateText)
                                Text(if (allClasses) "সকল শ্রেণি (${bn(availableClasses.size.toString())}টি)" else "শুধুমাত্র চিহ্নিত শ্রেণি", fontSize = 11.sp, color = SlateMuted)
                            }
                            Switch(
                                checked = allClasses,
                                onCheckedChange = { checked ->
                                    allClasses = checked
                                    if (checked) selectedClasses = allClassIds.toSet()
                                }
                            )
                        }
                        if (!allClasses) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                availableClasses.forEach { cDef ->
                                    val c = cDef.id
                                    val isSel = c in selectedClasses
                                    FilterChip(
                                        selected = isSel,
                                        onClick = {
                                            selectedClasses = if (isSel) selectedClasses - c else selectedClasses + c
                                        },
                                        colors = appFilterChipColors(),
                                        border = appFilterChipBorder(isSel),
                                        label = { Text(cDef.nameBn, fontSize = 11.5.sp) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Subject Permissions
                Surface(
                    color = SlateBg,
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("কোন বিষয়ের নম্বর সম্পাদনা করবেন", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = SlateText)
                                Text(if (allSubjects) "সকল বিষয়" else "শুধু চিহ্নিত বিষয় সম্পাদনা করতে পারবেন", fontSize = 11.sp, color = SlateMuted)
                            }
                            Switch(
                                checked = allSubjects,
                                onCheckedChange = { checked ->
                                    allSubjects = checked
                                    if (checked) selectedSubjects = ALL_SUBJECT_KEYS.toSet()
                                }
                            )
                        }
                        if (!allSubjects) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                SUBJECT_ACCESS_DEFS.forEach { def ->
                                    val isSel = def.key in selectedSubjects
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) TealContainer.copy(alpha = 0.6f) else androidx.compose.ui.graphics.Color.Transparent)
                                            .clickable {
                                                selectedSubjects = if (isSel) selectedSubjects - def.key else selectedSubjects + def.key
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isSel,
                                            onCheckedChange = { checked ->
                                                selectedSubjects = if (checked) selectedSubjects + def.key else selectedSubjects - def.key
                                            }
                                        )
                                        Text(
                                            def.labelBn,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSel) TealDark else SlateText
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = SlateOutline)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { canViewOtherSubjects = !canViewOtherSubjects }
                                    .padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("অন্যান্য বিষয় শুধু দেখতে পারবে", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                    Text("চালু থাকলে বাকি বিষয়ের নম্বর দেখা যাবে কিন্তু বদলানো যাবে না", fontSize = 10.5.sp, color = SlateMuted)
                                }
                                Switch(
                                    checked = canViewOtherSubjects,
                                    onCheckedChange = { canViewOtherSubjects = it }
                                )
                            }
                        }
                    }
                }

                // Operational Permissions
                Surface(
                    color = SlateBg,
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("অন্যান্য কার্যক্রমের অনুমতি", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = TealPrimary)

                        PermissionSwitchRow(
                            title = "উপস্থিতি, গুণাবলি ও মতামত",
                            subtitle = "হাজিরা, সক্রিয়তা, শৃঙ্খলা, আচরণ ও শ্রেণি শিক্ষকের মতামত সম্পাদনা",
                            checked = canEditAttendance,
                            onCheckedChange = { canEditAttendance = it }
                        )
                        HorizontalDivider(color = SlateOutline)
                        PermissionSwitchRow(
                            title = "শিক্ষার্থী যোগ, প্রোফাইল সম্পাদনা ও মুছা",
                            subtitle = "নাম/রোল/অভিভাবক/ঠিকানা বদলানো, CSV ইমপোর্ট ও শিক্ষার্থী মুছে ফেলা",
                            checked = canManageStudents,
                            onCheckedChange = { canManageStudents = it }
                        )
                        HorizontalDivider(color = SlateOutline)
                        PermissionSwitchRow(
                            title = "রিপোর্ট কার্ড ও PDF তৈরি",
                            subtitle = "প্রিভিউ দেখা, প্রিন্ট করা ও PDF এক্সপোর্ট",
                            checked = canExportPdf,
                            onCheckedChange = { canExportPdf = it }
                        )
                    }
                }

                Surface(
                    color = TealSoft,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "সারসংক্ষেপ: ${buildResult().summaryBn()}",
                        fontSize = 11.sp,
                        color = SlateText,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(buildResult()) },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun PermissionSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 12.5.sp, color = SlateText)
            Text(subtitle, fontSize = 10.5.sp, color = SlateMuted, lineHeight = 14.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
