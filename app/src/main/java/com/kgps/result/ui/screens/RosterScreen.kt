@file:OptIn(ExperimentalLayoutApi::class)

package com.kgps.result.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.RankedStudentResult
import com.kgps.result.ResultRulesEngine
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.activeClassIds
import com.kgps.result.bn
import com.kgps.result.classNameBn
import com.kgps.result.classShortBn
import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.fmt
import com.kgps.result.fromBn
import com.kgps.result.isSpecialNeeds
import com.kgps.result.isSubTermAbsent
import com.kgps.result.ordinalBn
import com.kgps.result.subjectsFor
import com.kgps.result.termNameBn
import com.kgps.result.total
import com.kgps.result.ui.components.BiometricDeleteConfirmDialog
import com.kgps.result.ui.components.FloatingGroupStickyBar
import com.kgps.result.ui.components.TreeBranchItem
import com.kgps.result.ui.components.TreeGroupCard
import com.kgps.result.ui.theme.AmberBg
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
import kotlinx.coroutines.launch

@Composable
fun RosterScreen(
    students: List<Student>,
    settings: Settings,
    cls: Int,
    permissions: TeacherPermissions,
    onSelectClass: (Int) -> Unit,
    onOpenStudent: (Student) -> Unit,
    onNewStudentInClass: (Int) -> Unit,
    onDeleteStudent: ((String) -> Unit)? = null,
    onPreviewStudent: ((Student) -> Unit)? = null
) {
    val availableClassIds = remember(settings.customClasses) { activeClassIds(settings) }
    val visibleClasses = remember(permissions, availableClassIds) {
        permissions.effectiveClasses(availableClassIds)
    }

    var query by remember { mutableStateOf("") }
    var expandedClass by remember(visibleClasses) {
        mutableStateOf<Int?>(if (cls in visibleClasses) cls else visibleClasses.firstOrNull())
    }
    var expandedStudentIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingDeleteStudent by remember { mutableStateOf<Student?>(null) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val q = query.trim().lowercase()
    val qDigits = fromBn(q)

    val studentsByClass = remember(students, visibleClasses) {
        visibleClasses.associateWith { c ->
            students.filter { it.cls == c }.sortedBy { it.roll }
        }
    }
    val matchingByClass = remember(studentsByClass, q, qDigits) {
        studentsByClass.mapValues { (_, list) ->
            if (q.isEmpty()) list
            else list.filter {
                val rollStr = it.roll.toString()
                it.name.lowercase().contains(q) ||
                    (qDigits.isNotEmpty() && rollStr.contains(qDigits)) ||
                    bn(rollStr).contains(q) ||
                    it.guardian.lowercase().contains(q) ||
                    it.mobile.lowercase().contains(q) ||
                    it.village.lowercase().contains(q)
            }
        }
    }
    val displayedClasses = remember(visibleClasses, matchingByClass, q) {
        if (q.isEmpty()) visibleClasses
        else visibleClasses.filter { matchingByClass[it].orEmpty().isNotEmpty() }
    }

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

            // Option খ: One-tap class switcher chips (no scrolling back up needed)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("roster_class_switcher_row"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(visibleClasses, key = { "chip_$it" }) { c ->
                    val cnt = studentsByClass[c]?.size ?: 0
                    val isSelected = q.isEmpty() && expandedClass == c
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (q.isNotEmpty()) {
                                query = ""
                                expandedClass = c
                                onSelectClass(c)
                                val idx = visibleClasses.indexOf(c)
                                if (idx >= 0) {
                                    coroutineScope.launch { listState.animateScrollToItem(idx) }
                                }
                            } else if (expandedClass == c) {
                                expandedClass = null
                            } else {
                                expandedClass = c
                                onSelectClass(c)
                                val idx = visibleClasses.indexOf(c)
                                if (idx >= 0) {
                                    coroutineScope.launch { listState.animateScrollToItem(idx) }
                                }
                            }
                        },
                        label = {
                            Text(
                                text = "${classShortBn(c, settings)} (${bn(cnt.toString())})",
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        },
                        trailingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = "গুটিয়ে নিন",
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        } else null,
                        colors = appFilterChipColors(),
                        border = appFilterChipBorder(isSelected),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("roster_chip_cls_$c")
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            val density = LocalDensity.current
            val headerHideThresholdPx = with(density) { 48.dp.roundToPx() }
            val minRemainingPx = with(density) { 64.dp.roundToPx() }
            val floatingStickyClass by remember(q, expandedClass, headerHideThresholdPx, minRemainingPx) {
                derivedStateOf {
                    val visible = listState.layoutInfo.visibleItemsInfo
                    visible.firstOrNull { info ->
                        val clsId = info.key as? Int ?: return@firstOrNull false
                        val isOpen = if (q.isNotEmpty()) true else (expandedClass == clsId)
                        isOpen &&
                            info.offset < -headerHideThresholdPx &&
                            (info.offset + info.size) > minRemainingPx
                    }?.key as? Int
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 84.dp)
                ) {
                    items(displayedClasses, key = { it }) { c ->
                        val allInClass = studentsByClass[c].orEmpty()
                        val matching = matchingByClass[c].orEmpty()
                        val rankMap = remember(allInClass, settings.resultRules) {
                            ResultRulesEngine.rankMapForClass(allInClass, settings.resultRules)
                        }

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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("roster_class_add_$c"),
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
                                val isDeckExpanded = s.id in expandedStudentIds
                                val ranked = rankMap[s.id]

                                TreeBranchItem(isLast = isLast) {
                                    StudentDeckCard(
                                        student = s,
                                        settings = settings,
                                        ranked = ranked,
                                        expanded = isDeckExpanded,
                                        canManage = permissions.canManageStudents && permissions.canAccessClass(s.cls),
                                        canExportPdf = permissions.canExportPdf,
                                        onToggleExpand = {
                                            expandedStudentIds = if (isDeckExpanded) {
                                                expandedStudentIds - s.id
                                            } else {
                                                expandedStudentIds + s.id
                                            }
                                        },
                                        onOpenEditor = { onOpenStudent(s) },
                                        onPreview = onPreviewStudent?.let { cb -> { cb(s) } },
                                        onRequestDelete = onDeleteStudent?.let {
                                            { pendingDeleteStudent = s }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                }

                // Option ক: Smart floating header only when expanded group's real header scrolls off top
                val stickyCls = floatingStickyClass
                val stickyCount = stickyCls?.let { studentsByClass[it]?.size ?: 0 } ?: 0
                FloatingGroupStickyBar(
                    visible = stickyCls != null,
                    title = if (stickyCls != null) "${classNameBn(stickyCls, settings)} শ্রেণি" else "",
                    badgeText = if (stickyCls != null) "${bn(stickyCount.toString())} জন" else null,
                    icon = Icons.Rounded.FolderOpen,
                    actionLabel = if (q.isNotEmpty()) "শ্রেণির শুরুতে" else "বন্ধ করুন",
                    testTag = "roster_floating_group_bar",
                    onAction = {
                        val target = stickyCls ?: return@FloatingGroupStickyBar
                        val targetIdx = displayedClasses.indexOf(target)
                        if (q.isEmpty()) {
                            expandedClass = null
                        }
                        if (targetIdx >= 0) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(targetIdx)
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp)
                )
            }
        }
    }

    pendingDeleteStudent?.let { target ->
        val displayName = target.name.ifBlank { "রোল ${bn(target.roll.toString())}" }
        val clsName = classNameBn(target.cls, settings)
        BiometricDeleteConfirmDialog(
            title = "শিক্ষার্থী মুছবেন?",
            description = "$clsName শ্রেণির \"$displayName\" (রোল ${bn(target.roll.toString())})-এর সমস্ত নম্বর, উপস্থিতি ও তথ্য স্থায়ীভাবে মুছে যাবে।",
            warningDescription = "$clsName শ্রেণির $displayName-কে স্থায়ীভাবে মুছতে আপনার ফিঙ্গারপ্রিন্ট অনুমতি দিন।",
            confirmActionLabel = "ফিঙ্গারপ্রিন্ট অনুমতি দিয়ে মুছুন",
            confirmButtonTestTag = "roster_confirm_delete_btn",
            onDismiss = { pendingDeleteStudent = null },
            onConfirmedDelete = {
                onDeleteStudent?.invoke(target.id)
                expandedStudentIds = expandedStudentIds - target.id
            }
        )
    }
}

@Composable
private fun StudentDeckCard(
    student: Student,
    settings: Settings,
    ranked: RankedStudentResult?,
    expanded: Boolean,
    canManage: Boolean,
    canExportPdf: Boolean,
    onToggleExpand: () -> Unit,
    onOpenEditor: () -> Unit,
    onPreview: (() -> Unit)?,
    onRequestDelete: (() -> Unit)?
) {
    val eval = ranked?.eval
    val hasData = eval?.hasAnyData == true
    val hasFail = eval != null && eval.hasAnyData && !eval.allSubjectsPassed
    val accentColor = when {
        !hasData -> SlateOutline
        !eval!!.isPromoted || hasFail -> RoseDanger
        eval.grade == "A" || eval.grade == "B" -> EmeraldGrade
        else -> TealPrimary
    }
    val gradeBg = when {
        !hasData -> SlateSurfaceVariant
        hasFail || eval?.grade == "D" -> RoseBg
        eval?.grade == "A" || eval?.grade == "B" -> TealSoft
        else -> AmberBg
    }
    val gradeFg = when {
        !hasData -> SlateMuted
        hasFail || eval?.grade == "D" -> RoseDanger
        eval?.grade == "A" || eval?.grade == "B" -> EmeraldGrade
        else -> AmberNotice
    }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(200),
        label = "deck_chevron"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("student_card_${student.id}")
            .clickable(onClick = onToggleExpand),
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) Color.White else SlateBg
        ),
        border = BorderStroke(
            width = if (expanded) 1.3.dp else 1.dp,
            color = if (expanded) TealPrimary.copy(alpha = 0.55f) else SlateOutline
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left vertical status accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                // 1. Upfront Header: Roll badge + Name + Religion + Grade/Merit pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = if (hasData) TealContainer else SlateSurfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "রোল",
                                fontSize = 8.5.sp,
                                color = if (hasData) TealDark else SlateMuted,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = bn(student.roll.toString().padStart(2, '0')),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = if (hasData) TealDark else SlateText
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = student.name.ifBlank { "(নাম নেই)" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = SlateText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (student.religion.isNotBlank()) {
                                Surface(
                                    color = Color.White,
                                    border = BorderStroke(0.8.dp, SlateOutline),
                                    shape = RoundedCornerShape(5.dp)
                                ) {
                                    Text(
                                        text = student.religion,
                                        fontSize = 9.5.sp,
                                        color = SlateMuted,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            if (student.isSpecialNeeds && settings.resultRules.specialNeedsRuleEnabled) {
                                Surface(
                                    color = AmberBg,
                                    shape = RoundedCornerShape(5.dp)
                                ) {
                                    Text(
                                        text = "বিশেষ",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberNotice,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            if (ranked != null && ranked.rank > 0) {
                                val rankText = if (ranked.isJointRank) {
                                    "যৌথ ${ordinalBn(ranked.rank)}"
                                } else {
                                    "মেধা ${ordinalBn(ranked.rank)}"
                                }
                                Surface(
                                    color = TealSoft,
                                    shape = RoundedCornerShape(5.dp)
                                ) {
                                    Text(
                                        text = rankText,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TealDark,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Grade + Percentage badge upfront
                    Surface(
                        color = gradeBg,
                        border = BorderStroke(0.8.dp, gradeFg.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (hasData) {
                                "${eval!!.grade} · ${fmt(eval.pct)}%"
                            } else {
                                "নম্বর নেই"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = gradeFg,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            maxLines = 1
                        )
                    }
                }

                // 2. Upfront Essential Deck Strip (Result, Attendance, Guardian/Mobile)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DeckMetricChip(
                        label = "গড় ও ফলাফল",
                        value = if (hasData) {
                            "${fmt(eval!!.obtainedAvgTotal)}/${fmt(eval.maxTotal)}"
                        } else {
                            "—"
                        },
                        status = when {
                            !hasData -> "তথ্য বাকি"
                            eval!!.failedSubjectKeys.isNotEmpty() -> "${bn(eval.failedSubjectKeys.size.toString())}টি ফেল"
                            eval.isPromoted -> "উত্তীর্ণ"
                            else -> "পর্যালোচনা"
                        },
                        statusColor = when {
                            !hasData -> SlateMuted
                            eval!!.failedSubjectKeys.isNotEmpty() -> RoseDanger
                            eval.isPromoted -> EmeraldGrade
                            else -> AmberNotice
                        },
                        modifier = Modifier.weight(1f)
                    )

                    val attPct = eval?.attendancePct
                    DeckMetricChip(
                        label = "উপস্থিতি",
                        value = if (attPct != null) "${fmt(attPct)}%" else "—",
                        status = if (eval != null && eval.activeTerms.isNotEmpty()) {
                            "${bn(eval.activeTerms.size.toString())} প্রান্তিক"
                        } else {
                            "কোনো প্রান্তিক নেই"
                        },
                        statusColor = if (attPct != null && attPct < settings.resultRules.minAttendancePct && settings.resultRules.attendanceRuleEnabled) {
                            AmberNotice
                        } else {
                            SlateMuted
                        },
                        modifier = Modifier.weight(0.85f)
                    )
                }

                // 3. Upfront Guardian, Mobile & Location summary line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        val guardianLabel = student.guardian.ifBlank { "অভিভাবকের নাম নেই" }
                        val mobileLabel = student.mobile.takeIf { it.isNotBlank() }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "অভি: $guardianLabel",
                                fontSize = 11.sp,
                                color = SlateText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (mobileLabel != null) {
                                Icon(
                                    imageVector = Icons.Rounded.Call,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = bn(mobileLabel),
                                    fontSize = 11.sp,
                                    color = TealDark,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                        val addr = listOf(student.village, student.union, student.upazila)
                            .filter { it.isNotBlank() }
                            .joinToString(", ")
                        if (addr.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = SlateMuted,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = addr,
                                    fontSize = 10.5.sp,
                                    color = SlateMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (expanded) "সংক্ষিপ্ত" else "বিস্তারিত",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TealPrimary
                        )
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = if (expanded) "বিস্তারিত বন্ধ করুন" else "বিস্তারিত দেখুন",
                            tint = TealPrimary,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(chevronRotation)
                        )
                    }
                }

                // 4. Expanded Detail View (Revealed on Tap)
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(tween(220)) + fadeIn(tween(180)),
                    exit = shrinkVertically(tween(180)) + fadeOut(tween(140))
                ) {
                    StudentDeckExpandedDetail(
                        student = student,
                        settings = settings,
                        eval = eval,
                        canManage = canManage,
                        canExportPdf = canExportPdf,
                        onOpenEditor = onOpenEditor,
                        onPreview = onPreview,
                        onRequestDelete = onRequestDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun DeckMetricChip(
    label: String,
    value: String,
    status: String,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        border = BorderStroke(0.8.dp, SlateOutline),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = label,
                fontSize = 9.5.sp,
                color = SlateMuted,
                maxLines = 1
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateText,
                    maxLines = 1
                )
                Text(
                    text = status,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StudentDeckExpandedDetail(
    student: Student,
    settings: Settings,
    eval: com.kgps.result.StudentEvalSummary?,
    canManage: Boolean,
    canExportPdf: Boolean,
    onOpenEditor: () -> Unit,
    onPreview: (() -> Unit)?,
    onRequestDelete: (() -> Unit)?
) {
    val subjects = remember(student.cls, student.religion, settings.customClasses) {
        subjectsFor(student.cls, student.religion, settings)
    }
    val maxTotal = remember(subjects) { subjects.sumOf { it.max } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider(color = SlateOutline)

        // Term-by-Term Snapshot (1st, 2nd, 3rd)
        Text(
            text = "প্রান্তিকভিত্তিক ফলাফল ও উপস্থিতি",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = SlateText
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            (1..3).forEach { term ->
                val hasMarks = subjects.any { sub ->
                    student.m.keys.any { it.startsWith("${sub.key}_${term}_") } ||
                        student.isSubTermAbsent(sub, term)
                }
                val termTotal = if (hasMarks) subjects.sumOf { student.total(it, term) } else 0.0
                val days = student.x["DAYS_$term"].orEmpty().trim()
                val pres = student.x["PRES_$term"].orEmpty().trim()
                val traits = listOf("ACT_$term", "DISC_$term", "COND_$term")
                    .mapNotNull { k -> student.x[k]?.trim()?.takeIf { it.isNotEmpty() } }

                Surface(
                    color = SlateBg,
                    border = BorderStroke(0.8.dp, SlateOutline),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = termNameBn(term),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TealDark
                        )
                        Text(
                            text = if (hasMarks) "${fmt(termTotal)}/${bn(maxTotal.toString())}" else "নম্বর নেই",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasMarks) SlateText else SlateMuted
                        )
                        if (days.isNotBlank() && pres.isNotBlank()) {
                            Text(
                                text = "উপস্থিতি: ${bn(pres)}/${bn(days)}",
                                fontSize = 9.5.sp,
                                color = SlateMuted
                            )
                        }
                        if (traits.isNotEmpty()) {
                            Text(
                                text = "গুণ: ${traits.joinToString("/")}",
                                fontSize = 9.5.sp,
                                color = TealPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Subject-wise Breakdown
        if (eval?.hasAnyData == true) {
            Text(
                text = "বিষয়ভিত্তিক গড় নম্বর",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = SlateText
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                subjects.forEach { sub ->
                    val shortTitle = sub.bn.substringBefore(" (").let {
                        when {
                            it.startsWith("ধর্মশিক্ষা") -> "ধর্ম"
                            it.startsWith("সামাজিক বিজ্ঞান") -> "স.বিজ্ঞান"
                            it.startsWith("বাংলাদেশ ও বিশ্বপরিচয়") -> "বাওবি"
                            it.startsWith("শারীরিক") -> "শারীরিক"
                            else -> it
                        }
                    }
                    val activeTerms = eval.activeTerms
                    val avgScore = if (activeTerms.isNotEmpty()) {
                        ResultRulesEngine.subjectAvg(student, sub, activeTerms)
                    } else 0.0
                    val isFailed = sub.key in eval.failedSubjectKeys
                    val isAbsent = sub.key in eval.absentSubjectKeys
                    Surface(
                        color = if (isFailed || isAbsent) RoseBg else SlateBg,
                        border = BorderStroke(
                            0.8.dp,
                            if (isFailed || isAbsent) RoseDanger.copy(alpha = 0.5f) else SlateOutline
                        ),
                        shape = RoundedCornerShape(7.dp)
                    ) {
                        Text(
                            text = "$shortTitle: ${fmt(avgScore)}/${bn(sub.max.toString())}" +
                                if (isAbsent) " (অনু)" else if (isFailed) "*" else "",
                            fontSize = 10.5.sp,
                            fontWeight = if (isFailed || isAbsent) FontWeight.Bold else FontWeight.Medium,
                            color = if (isFailed || isAbsent) RoseDanger else SlateText,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Address, Custom Fields & Latest Remark
        val fullAddress = listOfNotNull(
            student.village.takeIf { it.isNotBlank() }?.let { "গ্রাম: $it" },
            student.union.takeIf { it.isNotBlank() }?.let { "ইউনিয়ন: $it" },
            student.post.takeIf { it.isNotBlank() }?.let { "ডাকঘর: $it" },
            student.upazila.takeIf { it.isNotBlank() }?.let { "উপজেলা: $it" },
            student.district.takeIf { it.isNotBlank() }?.let { "জেলা: $it" }
        ).joinToString(" · ")

        val customLines = settings.customFields.mapNotNull { cf ->
            val v = student.x[cf.key]?.trim().orEmpty()
            if (v.isNotEmpty()) "${cf.label}: $v" else null
        }

        val latestRemark = listOf(3, 2, 1)
            .firstNotNullOfOrNull { t ->
                student.x["REM_$t"]?.trim()?.takeIf { it.isNotEmpty() }?.let { t to it }
            }

        if (fullAddress.isNotBlank() || customLines.isNotEmpty() || latestRemark != null) {
            Surface(
                color = SlateBg,
                border = BorderStroke(0.8.dp, SlateOutline),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (fullAddress.isNotBlank()) {
                        Text(text = fullAddress, fontSize = 11.sp, color = SlateText, lineHeight = 15.sp)
                    }
                    if (customLines.isNotEmpty()) {
                        Text(
                            text = customLines.joinToString(" · "),
                            fontSize = 11.sp,
                            color = SlateMuted,
                            lineHeight = 15.sp
                        )
                    }
                    if (latestRemark != null) {
                        Text(
                            text = "${termNameBn(latestRemark.first)} মন্তব্য: ${latestRemark.second}",
                            fontSize = 11.sp,
                            color = TealDark,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onOpenEditor,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("roster_deck_edit_${student.id}")
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("সম্পাদনা / পূর্ণ তথ্য", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }

            if (canExportPdf && onPreview != null) {
                OutlinedButton(
                    onClick = onPreview,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("roster_deck_preview_${student.id}")
                ) {
                    Icon(Icons.Rounded.Visibility, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("প্রিভিউ", fontSize = 11.5.sp, color = TealPrimary, fontWeight = FontWeight.SemiBold)
                }
            }

            if (canManage && onRequestDelete != null) {
                OutlinedButton(
                    onClick = onRequestDelete,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, RoseDanger.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("roster_deck_delete_${student.id}")
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = RoseDanger, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("মুছুন", fontSize = 11.5.sp, color = RoseDanger, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
