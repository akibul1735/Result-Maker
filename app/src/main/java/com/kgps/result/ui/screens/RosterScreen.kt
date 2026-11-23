package com.kgps.result.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.activeClassIds
import com.kgps.result.bn
import com.kgps.result.classNameBn
import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.fromBn
import com.kgps.result.ui.components.AddCustomClassDialog
import com.kgps.result.ui.components.TreeBranchItem
import com.kgps.result.ui.components.TreeGroupCard
import com.kgps.result.ui.theme.AmberNotice
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateSurfaceVariant
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft

@Composable
fun RosterScreen(
    students: List<Student>,
    settings: Settings,
    cls: Int,
    permissions: TeacherPermissions,
    isAdmin: Boolean = false,
    onAddCustomClass: ((nameBn: String, shortBn: String, syllabusTier: Int) -> Boolean)? = null,
    onBasicImport: (() -> Unit)? = null,
    onSelectClass: (Int) -> Unit,
    onOpenStudent: (Student) -> Unit,
    onNewStudentInClass: (Int) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var expandedClass by remember { mutableStateOf<Int?>(null) }
    var showAddClassDialog by remember { mutableStateOf(false) }

    val availableClassIds = remember(settings.customClasses) { activeClassIds(settings) }
    val visibleClasses = remember(permissions, availableClassIds) {
        permissions.effectiveClasses(availableClassIds)
    }
    val q = query.trim().lowercase()
    val qDigits = fromBn(q)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.widthIn(max = 880.dp).fillMaxSize()) {
            if (visibleClasses.isEmpty()) {
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = AmberNotice, modifier = Modifier.size(32.dp))
                        Text(
                            "অ্যাডমিন এখনো আপনাকে কোনো শ্রেণি নির্ধারণ করেননি",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = SlateText
                        )
                        Text(
                            "বিদ্যালয়ের অ্যাডমিন সেটিংস থেকে আপনার শ্রেণির অনুমতি চালু করলে এখানে শিক্ষার্থীর তালিকা দেখাবে।",
                            fontSize = 12.sp,
                            color = SlateMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                return@Column
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("নাম, রোল, অভিভাবক বা মোবাইল খুঁজুন...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "অনুসন্ধান") },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "মুছুন")
                        }
                    }
                } else null,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("roster_search_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "শ্রেণিতে ট্যাপ করে শিক্ষার্থী দেখুন (${bn(visibleClasses.size.toString())}টি শ্রেণি)",
                    fontSize = 11.5.sp,
                    color = SlateMuted,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (permissions.canManageStudents && onBasicImport != null) {
                        OutlinedButton(
                            onClick = onBasicImport,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("roster_basic_import_btn")
                        ) {
                            Icon(
                                Icons.Rounded.Badge,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.size(4.dp))
                            Text("বেসিক ইমপোর্ট", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = TealPrimary)
                        }
                    }
                    if (isAdmin && onAddCustomClass != null) {
                        OutlinedButton(
                            onClick = { showAddClassDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("roster_add_custom_class_btn")
                        ) {
                            Icon(
                                Icons.Rounded.AddCircleOutline,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.size(4.dp))
                            Text("নতুন শ্রেণি", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = TealPrimary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Pure Class -> Student List Tree (all collapsed by default until tapped)
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 76.dp)
            ) {
                items(visibleClasses, key = { it }) { c ->
                    val allInClass = remember(students, c) {
                        students.filter { it.cls == c }.sortedBy { it.roll }
                    }
                    val matching = remember(allInClass, q, qDigits) {
                        if (q.isEmpty()) allInClass
                        else allInClass.filter {
                            val rollStr = it.roll.toString()
                            it.name.lowercase().contains(q) ||
                                (qDigits.isNotEmpty() && rollStr.contains(qDigits)) ||
                                bn(rollStr).contains(q) ||
                                it.guardian.lowercase().contains(q) ||
                                it.mobile.lowercase().contains(q) ||
                                it.village.lowercase().contains(q)
                        }
                    }

                    if (q.isNotEmpty() && matching.isEmpty()) return@items

                    val teacherName = settings.teachers[c].orEmpty().ifBlank { "অনির্ধারিত" }
                    val isExpanded = if (q.isNotEmpty()) matching.isNotEmpty() else (expandedClass == c)

                    TreeGroupCard(
                        title = "${classNameBn(c, settings)} শ্রেণি",
                        subtitle = "শ্রেণি শিক্ষক: $teacherName",
                        badgeText = "${bn(allInClass.size.toString())} জন",
                        badgeColor = TealPrimary,
                        badgeContainerColor = TealSoft,
                        icon = Icons.Rounded.FolderOpen,
                        expanded = isExpanded,
                        headerTestTag = "roster_class_node_$c",
                        onToggle = {
                            onSelectClass(c)
                            expandedClass = if (expandedClass == c) null else c
                        }
                    ) {
                        if (permissions.canManageStudents) {
                            OutlinedButton(
                                onClick = { onNewStudentInClass(c) },
                                modifier = Modifier.fillMaxWidth().testTag("roster_class_add_$c"),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                                border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.45f))
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                                Text(
                                    " এই শ্রেণিতে নতুন শিক্ষার্থী যোগ করুন",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TealPrimary
                                )
                            }
                        }

                        if (matching.isEmpty()) {
                            TreeBranchItem(isLast = true) {
                                Surface(
                                    color = SlateBg,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, SlateOutline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.PersonOutline,
                                                contentDescription = null,
                                                tint = SlateMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                "এই শ্রেণিতে কোনো শিক্ষার্থী নেই",
                                                fontSize = 12.sp,
                                                color = SlateMuted
                                            )
                                        }
                                        if (permissions.canManageStudents) {
                                            TextButton(onClick = { onNewStudentInClass(c) }) {
                                                Text("+ যোগ করুন", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            matching.forEachIndexed { sIdx, s ->
                                val isLast = sIdx == matching.lastIndex
                                TreeBranchItem(isLast = isLast) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("student_card_${s.id}")
                                            .clickable { onOpenStudent(s) },
                                        colors = CardDefaults.cardColors(containerColor = SlateBg),
                                        border = BorderStroke(1.dp, SlateOutline),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 9.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                color = SlateSurfaceVariant,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    bn(s.roll.toString().padStart(2, '0')),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = SlateText,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                                )
                                            }
                                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                                Text(
                                                    s.name.ifBlank { "(নাম নেই)" },
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    color = SlateText,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val subLine = listOfNotNull(
                                                    s.guardian.takeIf { it.isNotBlank() },
                                                    s.mobile.takeIf { it.isNotBlank() },
                                                    s.religion.takeIf { it.isNotBlank() }
                                                ).joinToString(" · ")
                                                if (subLine.isNotBlank()) {
                                                    Text(
                                                        subLine,
                                                        fontSize = 10.5.sp,
                                                        color = SlateMuted,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                                contentDescription = "শিক্ষার্থীর তথ্য খুলুন",
                                                tint = SlateMuted,
                                                modifier = Modifier.size(18.dp)
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
    }

    if (showAddClassDialog && onAddCustomClass != null) {
        AddCustomClassDialog(
            settings = settings,
            onDismiss = { showAddClassDialog = false },
            onConfirm = { nameBn, shortBn, tier ->
                onAddCustomClass(nameBn, shortBn, tier)
            }
        )
    }
}
