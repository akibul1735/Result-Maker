package com.kgps.result.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.RELIGIONS
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.TeacherRemarkEngine
import com.kgps.result.activeClassIds
import com.kgps.result.avg
import com.kgps.result.bn
import com.kgps.result.classNameBn
import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.cont
import com.kgps.result.fmt
import com.kgps.result.fmtEn
import com.kgps.result.fromBn
import com.kgps.result.gradeOf
import com.kgps.result.hasSub
import com.kgps.result.mk
import com.kgps.result.subjectsFor
import com.kgps.result.subs
import com.kgps.result.termNameBn
import com.kgps.result.total
import com.kgps.result.ui.components.StudentRemarkContextSummary
import com.kgps.result.ui.components.TeacherRemarkPickerDialog
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
import com.kgps.result.util.CommentGenerator

private data class SheetColDef(
    val key: String,
    val label: String,
    val subLabel: String = "",
    val max: Int = 100,
    val width: Dp = 74.dp,
    val isMark: Boolean = false,
    val markSubKey: String? = null,
    val markCompKey: String? = null,
    val xKey: String? = null,
    val infoFieldKey: String? = null,
    val isNumeric: Boolean = true,
    val isTraitCycle: Boolean = false
)

private val SHEET_MODES = listOf(
    "subject" to "বিষয়ভিত্তিক নম্বর",
    "all" to "সকল বিষয় মোট",
    "attendance" to "উপস্থিতি এন্ট্রি",
    "traits" to "গুণাবলি এন্ট্রি",
    "remarks" to "মন্তব্য অটো-ফিল",
    "info" to "তথ্য ও কাস্টম"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BulkSheetScreen(
    students: List<Student>,
    settings: Settings,
    cls: Int,
    permissions: TeacherPermissions,
    onSelectClass: (Int) -> Unit,
    onUpsertStudent: (Student) -> Unit,
    onPatchStudentField: (studentId: String, fieldPath: String, value: Any?) -> Unit,
    onOpenStudent: (Student) -> Unit,
    onClassPdf: () -> Unit,
    onBatchEdit: (Int) -> Unit = {},
    onBatchPatchTraits: (studentIds: Set<String>, xKeys: Set<String>, value: String?) -> Unit = { _, _, _ -> },
    onBatchPatchAttendance: (studentIds: Set<String>, term: Int, workingDays: Int?, presentDays: Int?) -> Unit = { _, _, _, _ -> },
    onBatchPatchRemarks: (term: Int, remarksByStudentId: Map<String, String?>) -> Unit = { _, _ -> },
    onShowMessage: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val availableClassIds = remember(settings.customClasses) { activeClassIds(settings) }
    val allowedClasses = remember(permissions, availableClassIds) {
        permissions.effectiveClasses(availableClassIds)
    }
    val activeCls = if (cls in allowedClasses) cls else (allowedClasses.firstOrNull() ?: cls)

    val availableModes = remember(permissions) {
        SHEET_MODES.filter { (id, _) ->
            when (id) {
                "subject" -> permissions.allSubjects || permissions.effectiveSubjects().isNotEmpty() || permissions.canViewOtherSubjects
                "all" -> permissions.allSubjects || permissions.effectiveSubjects().isNotEmpty() || permissions.canViewOtherSubjects
                "attendance", "traits", "remarks" -> permissions.canEditAttendance
                "info" -> permissions.canManageStudents
                else -> true
            }
        }
    }

    var term by remember { mutableStateOf(1) }
    var mode by remember { mutableStateOf(availableModes.firstOrNull()?.first ?: "subject") }
    var navDir by remember { mutableStateOf("down") }
    var isEditMode by remember { mutableStateOf(false) }
    var useBnDigits by remember { mutableStateOf(true) }
    var traitBatchSelectMode by remember { mutableStateOf(false) }
    var selectedTraitStudentIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedTraitCols by remember(term) {
        mutableStateOf(setOf("ACT_$term", "DISC_$term", "COND_$term"))
    }
    var selectedTraitValue by remember { mutableStateOf<String?>("ক") }
    var activeTraitMenuCellKey by remember { mutableStateOf<String?>(null) }

    var attBatchFillMode by remember { mutableStateOf(false) }
    var selectedAttStudentIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var batchWorkingDaysInput by remember { mutableStateOf("") }
    var batchPresentDaysInput by remember { mutableStateOf("") }
    var remarkPickerStudent by remember { mutableStateOf<Student?>(null) }

    LaunchedEffect(activeCls, mode) {
        traitBatchSelectMode = false
        selectedTraitStudentIds = emptySet()
        activeTraitMenuCellKey = null
        attBatchFillMode = false
        selectedAttStudentIds = emptySet()
        batchWorkingDaysInput = ""
        batchPresentDaysInput = ""
    }
    LaunchedEffect(useBnDigits) {
        if (batchWorkingDaysInput.isNotEmpty()) {
            batchWorkingDaysInput = if (useBnDigits) bn(fromBn(batchWorkingDaysInput)) else fromBn(batchWorkingDaysInput)
        }
        if (batchPresentDaysInput.isNotEmpty()) {
            batchPresentDaysInput = if (useBnDigits) bn(fromBn(batchPresentDaysInput)) else fromBn(batchPresentDaysInput)
        }
    }
    LaunchedEffect(isEditMode) {
        if (!isEditMode) activeTraitMenuCellKey = null
    }

    fun fmtSheetNum(v: Double): String = if (useBnDigits) fmt(v) else fmtEn(v)
    fun fmtSheetDigits(raw: String): String = if (useBnDigits) bn(fromBn(raw)) else fromBn(raw)

    LaunchedEffect(availableModes) {
        if (availableModes.isNotEmpty() && availableModes.none { it.first == mode }) {
            mode = availableModes.first().first
        }
    }

    var classMenuOpen by remember { mutableStateOf(false) }
    var termMenuOpen by remember { mutableStateOf(false) }
    var modeMenuOpen by remember { mutableStateOf(false) }
    var subjectMenuOpen by remember { mutableStateOf(false) }

    val allSubsForClass = remember(activeCls) { subjectsFor(activeCls, RELIGIONS.first()) }
    val visibleSubs = remember(allSubsForClass, permissions) {
        allSubsForClass.filter { permissions.canViewSubject(it.key) }
    }
    var subKey by remember(activeCls, visibleSubs) {
        mutableStateOf(
            visibleSubs.firstOrNull { permissions.canEditSubject(it.key) }?.key
                ?: visibleSubs.firstOrNull()?.key
                ?: allSubsForClass.first().key
        )
    }
    val activeSub = visibleSubs.find { it.key == subKey } ?: visibleSubs.firstOrNull()

    fun isColumnEditable(col: SheetColDef): Boolean {
        return when {
            col.isMark && col.markSubKey != null -> permissions.canEditSubject(col.markSubKey)
            col.xKey != null && (mode == "attendance" || mode == "traits") -> permissions.canEditAttendance
            col.xKey != null || col.infoFieldKey != null -> permissions.canManageStudents
            else -> false
        }
    }

    val classStudents = remember(students, activeCls) {
        students.filter { it.cls == activeCls }.sortedBy { it.roll }
    }

    val cols: List<SheetColDef> = remember(mode, activeSub, term, visibleSubs, settings.customFields, useBnDigits) {
        when (mode) {
            "subject" -> if (activeSub == null) emptyList() else buildList {
                activeSub.comps.forEach { c ->
                    val short = when (c.key) {
                        "TB" -> "পাঠ্যবই"
                        "ACT" -> "সক্রিয়তা"
                        "LANG" -> "ভাষা"
                        "CT" -> "ক্লাস টেস্ট"
                        else -> "মোট"
                    }
                    add(
                        SheetColDef(
                            key = "${activeSub.key}_${c.key}",
                            label = short,
                            subLabel = "সর্বোচ্চ ${fmtSheetDigits(c.max.toString())}",
                            max = c.max,
                            width = 76.dp,
                            isMark = true,
                            markSubKey = activeSub.key,
                            markCompKey = c.key
                        )
                    )
                }
                if (activeSub.written > 0) {
                    add(
                        SheetColDef(
                            key = "${activeSub.key}_WR",
                            label = "সামষ্টিক",
                            subLabel = "সর্বোচ্চ ${fmtSheetDigits(activeSub.written.toString())}",
                            max = activeSub.written,
                            width = 76.dp,
                            isMark = true,
                            markSubKey = activeSub.key,
                            markCompKey = "WR"
                        )
                    )
                }
            }
            "all" -> visibleSubs.map { sub ->
                val shortName = when (sub.key) {
                    "SOC" -> "সামাজিক/বিজ্ঞান"
                    "BGW" -> "বাওবি"
                    "PE" -> "শারীরিক শিক্ষা"
                    "REL" -> "ধর্ম"
                    else -> sub.bn
                }
                val hasWritten = sub.written > 0
                SheetColDef(
                    key = "all_${sub.key}",
                    label = shortName,
                    subLabel = "${if (hasWritten) "সামষ্টিক" else "ধারাবাহিক"} (${fmtSheetDigits((if (hasWritten) sub.written else sub.max).toString())})",
                    max = if (hasWritten) sub.written else sub.max,
                    width = 86.dp,
                    isMark = true,
                    markSubKey = sub.key,
                    markCompKey = if (hasWritten) "WR" else "CONT"
                )
            }
            "attendance" -> listOf(
                SheetColDef("DAYS_$term", "কার্যদিবস", "সর্বোচ্চ ${fmtSheetDigits("365")}", max = 365, width = 96.dp, xKey = "DAYS_$term"),
                SheetColDef("PRES_$term", "উপস্থিতি", "সর্বোচ্চ ${fmtSheetDigits("365")}", max = 365, width = 96.dp, xKey = "PRES_$term")
            )
            "traits" -> listOf(
                SheetColDef("ACT_$term", "সক্রিয়তা", "হালকা চেপে ধরুন", width = 94.dp, xKey = "ACT_$term", isNumeric = false, isTraitCycle = true),
                SheetColDef("DISC_$term", "শৃঙ্খলা", "হালকা চেপে ধরুন", width = 94.dp, xKey = "DISC_$term", isNumeric = false, isTraitCycle = true),
                SheetColDef("COND_$term", "আচরণ", "হালকা চেপে ধরুন", width = 94.dp, xKey = "COND_$term", isNumeric = false, isTraitCycle = true)
            )
            "remarks" -> emptyList()
            else -> buildList {
                add(SheetColDef("info_guardian", "অভিভাবকের নাম", "তথ্য", width = 132.dp, infoFieldKey = "guardian", isNumeric = false))
                add(SheetColDef("info_mobile", "মোবাইল", "নম্বর", width = 108.dp, infoFieldKey = "mobile", isNumeric = true))
                add(SheetColDef("info_village", "গ্রাম", "ঠিকানা", width = 104.dp, infoFieldKey = "village", isNumeric = false))
                add(SheetColDef("info_union", "ইউনিয়ন", "ঐচ্ছিক", width = 100.dp, infoFieldKey = "union", isNumeric = false))
                add(SheetColDef("info_upazila", "উপজেলা", "ডিফল্ট আলফাডাঙ্গা", width = 104.dp, infoFieldKey = "upazila", isNumeric = false))
                add(SheetColDef("info_district", "জেলা", "ডিফল্ট ফরিদপুর", width = 96.dp, infoFieldKey = "district", isNumeric = false))
                add(SheetColDef("info_religion", "ধর্ম", "ইসলাম/হিন্দু...", width = 84.dp, infoFieldKey = "religion", isNumeric = false))
                settings.customFields.forEach { cf ->
                    add(SheetColDef("cf_${cf.key}", cf.label, "কাস্টম", width = 110.dp, xKey = cf.key, isNumeric = false))
                }
            }
        }
    }

    val focusMatrix = remember(classStudents.size, cols.size) {
        List(classStudents.size) { List(cols.size) { FocusRequester() } }
    }

    fun moveFocusFrom(r: Int, c: Int) {
        if (classStudents.isEmpty() || cols.isEmpty()) return
        val nextR: Int
        val nextC: Int
        if (navDir == "down") {
            if (r + 1 < classStudents.size) {
                nextR = r + 1; nextC = c
            } else if (c + 1 < cols.size) {
                nextR = 0; nextC = c + 1
            } else return
        } else {
            if (c + 1 < cols.size) {
                nextR = r; nextC = c + 1
            } else if (r + 1 < classStudents.size) {
                nextR = r + 1; nextC = 0
            } else return
        }
        runCatching { focusMatrix[nextR][nextC].requestFocus() }
    }

    fun applyClipboardPaste() {
        val raw = clipboard.getText()?.text?.trim().orEmpty()
        if (raw.isEmpty()) {
            onShowMessage("ক্লিপবোর্ডে কোনো ডাটা পাওয়া যায়নি (Excel থেকে কপি করে পেস্ট করুন)")
            return
        }
        val lines = raw.split(Regex("\\r?\\n")).filter { it.isNotBlank() }
        if (lines.isEmpty() || classStudents.isEmpty() || cols.isEmpty()) return

        var updatedCells = 0
        lines.forEachIndexed { rIdx, line ->
            val targetStudent = classStudents.getOrNull(rIdx) ?: return@forEachIndexed
            val parts = if (line.contains('\t')) line.split('\t') else line.split(',')
            var nextStudent = targetStudent
            val nextM = nextStudent.m.toMutableMap()
            val nextX = nextStudent.x.toMutableMap()

            parts.forEachIndexed { cIdx, rawVal ->
                val col = cols.getOrNull(cIdx) ?: return@forEachIndexed
                if (!isColumnEditable(col)) return@forEachIndexed
                val trimmed = rawVal.trim()
                if (col.isMark && col.markSubKey != null && col.markCompKey != null) {
                    val d = fromBn(trimmed).toDoubleOrNull()
                    if (d != null && d in 0.0..col.max.toDouble()) {
                        nextM[mk(col.markSubKey, term, col.markCompKey)] = d
                        if (col.markCompKey == "CONT") {
                            listOf("TB", "ACT", "LANG", "CT", "TOTAL").forEach { comp ->
                                nextM.remove(mk(col.markSubKey, term, comp))
                            }
                        } else if (col.markCompKey in listOf("TB", "ACT", "LANG", "CT")) {
                            nextM.remove(mk(col.markSubKey, term, "CONT"))
                            nextM.remove(mk(col.markSubKey, term, "TOTAL"))
                        }
                        updatedCells++
                    }
                } else if (col.xKey != null) {
                    if (col.isNumeric) {
                        val iv = fromBn(trimmed).toIntOrNull()
                        if (iv != null && iv in 0..col.max) {
                            nextX[col.xKey] = iv.toString()
                            updatedCells++
                        }
                    } else if (trimmed.isNotEmpty()) {
                        nextX[col.xKey] = trimmed
                        updatedCells++
                    }
                } else if (col.infoFieldKey != null && trimmed.isNotEmpty()) {
                    nextStudent = when (col.infoFieldKey) {
                        "guardian" -> nextStudent.copy(guardian = trimmed)
                        "mobile" -> nextStudent.copy(mobile = trimmed)
                        "village" -> nextStudent.copy(village = trimmed)
                        "union" -> nextStudent.copy(union = trimmed)
                        "upazila" -> nextStudent.copy(upazila = trimmed)
                        "district" -> nextStudent.copy(district = trimmed)
                        "religion" -> nextStudent.copy(religion = trimmed)
                        else -> nextStudent
                    }
                    updatedCells++
                }
            }
            onUpsertStudent(nextStudent.copy(m = nextM, x = nextX))
        }
        onShowMessage(
            if (updatedCells > 0) "ক্লিপবোর্ড থেকে ${bn(updatedCells.toString())}টি অনুমোদিত ঘর আপডেট হয়েছে"
            else "সম্পাদনার অনুমতিপ্রাপ্ত কোনো ঘর আপডেট হয়নি"
        )
    }

    if (allowedClasses.isEmpty() || availableModes.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = AmberNotice, modifier = Modifier.size(34.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                "ব্যাচ এন্ট্রির জন্য কোনো শ্রেণি বা বিষয় নির্ধারিত নেই",
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp,
                color = SlateText,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "বিদ্যালয়ের অ্যাডমিন আপনার শ্রেণি ও বিষয়ের অ্যাক্সেস চালু করলে এখানে ব্যাচ এন্ট্রি করতে পারবেন।",
                fontSize = 12.sp,
                color = SlateMuted,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val hScroll = rememberScrollState()
    val canPasteCurrentSheet = cols.any { isColumnEditable(it) }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Ultra-Compact 2-Row Toolbar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Row 1: Class + Term + Digit Switch + Edit + PDF
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box {
                        Surface(
                            onClick = { classMenuOpen = true },
                            shape = RoundedCornerShape(7.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, SlateOutline),
                            modifier = Modifier.height(32.dp).testTag("sheet_class_dropdown")
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 8.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${classNameBn(activeCls, settings)} শ্রেণি",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SlateText,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = SlateMuted)
                            }
                        }
                        DropdownMenu(expanded = classMenuOpen, onDismissRequest = { classMenuOpen = false }) {
                            allowedClasses.forEach { c ->
                                val count = students.count { it.cls == c }
                                val isSelected = c == activeCls
                                DropdownMenuItem(
                                    modifier = Modifier.background(if (isSelected) TealContainer else Color.Transparent),
                                    text = {
                                        Text(
                                            "${classNameBn(c, settings)} শ্রেণি (${bn(count.toString())} জন)",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) TealDark else SlateText
                                        )
                                    },
                                    trailingIcon = if (isSelected) {
                                        { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    onClick = { onSelectClass(c); classMenuOpen = false }
                                )
                            }
                        }
                    }

                    if (mode != "info") {
                        Box {
                            Surface(
                                onClick = { termMenuOpen = true },
                                shape = RoundedCornerShape(7.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, SlateOutline),
                                modifier = Modifier.height(32.dp).testTag("sheet_term_dropdown")
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 8.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        termNameBn(term),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SlateText,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = SlateMuted)
                                }
                            }
                            DropdownMenu(expanded = termMenuOpen, onDismissRequest = { termMenuOpen = false }) {
                                (1..3).forEach { t ->
                                    val isSelected = t == term
                                    DropdownMenuItem(
                                        modifier = Modifier.background(if (isSelected) TealContainer else Color.Transparent),
                                        text = {
                                            Text(
                                                termNameBn(t),
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) TealDark else SlateText
                                            )
                                        },
                                        trailingIcon = if (isSelected) {
                                            { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        onClick = { term = t; termMenuOpen = false }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // Compact Digit Toggle (০-৯ | 0-9)
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = SlateBg,
                        border = BorderStroke(1.dp, SlateOutline),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .background(if (useBnDigits) TealPrimary else Color.Transparent)
                                    .clickable { useBnDigits = true }
                                    .padding(horizontal = 7.dp)
                                    .testTag("sheet_digit_bn_chip"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "০-৯",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (useBnDigits) Color.White else SlateMuted
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .background(if (!useBnDigits) TealPrimary else Color.Transparent)
                                    .clickable { useBnDigits = false }
                                    .padding(horizontal = 7.dp)
                                    .testTag("sheet_digit_en_chip"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "0-9",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!useBnDigits) Color.White else SlateMuted
                                )
                            }
                        }
                    }

                    if (canPasteCurrentSheet) {
                        Surface(
                            onClick = {
                                if (isEditMode) {
                                    focusManager.clearFocus(force = true)
                                    isEditMode = false
                                    onShowMessage("এডিট মোড বন্ধ করা হয়েছে")
                                } else {
                                    isEditMode = true
                                }
                            },
                            color = if (isEditMode) TealPrimary else TealSoft,
                            border = BorderStroke(1.dp, if (isEditMode) TealPrimary else TealContainer),
                            shape = RoundedCornerShape(7.dp),
                            modifier = Modifier.height(32.dp).testTag("sheet_edit_toggle_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    if (isEditMode) Icons.Rounded.Check else Icons.Rounded.Edit,
                                    contentDescription = if (isEditMode) "এডিট সম্পন্ন" else "এডিট চালু",
                                    tint = if (isEditMode) Color.White else TealPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    if (isEditMode) "সম্পন্ন" else "এডিট",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEditMode) Color.White else TealPrimary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    if (permissions.canExportPdf) {
                        Surface(
                            onClick = onClassPdf,
                            shape = RoundedCornerShape(7.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, SlateOutline),
                            modifier = Modifier.size(32.dp).testTag("sheet_pdf_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Description,
                                    contentDescription = "শ্রেণির PDF",
                                    tint = TealPrimary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }

                // Row 2: Mode Dropdown + Subject Dropdown + Context Quick Action (Batch / Nav Dir)
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(Modifier.weight(1f)) {
                        val activeModeLabel = availableModes.find { it.first == mode }?.second ?: mode
                        Surface(
                            onClick = { modeMenuOpen = true },
                            shape = RoundedCornerShape(7.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, SlateOutline),
                            modifier = Modifier.fillMaxWidth().height(32.dp).testTag("sheet_mode_dropdown")
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 8.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    activeModeLabel,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SlateText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = SlateMuted)
                            }
                        }
                        DropdownMenu(expanded = modeMenuOpen, onDismissRequest = { modeMenuOpen = false }) {
                            availableModes.forEach { (id, label) ->
                                val isSelected = mode == id
                                DropdownMenuItem(
                                    modifier = Modifier.background(if (isSelected) TealContainer else Color.Transparent),
                                    text = {
                                        Text(
                                            label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) TealDark else SlateText
                                        )
                                    },
                                    trailingIcon = if (isSelected) {
                                        { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    onClick = { mode = id; modeMenuOpen = false }
                                )
                            }
                        }
                    }

                    if (mode == "subject" && activeSub != null) {
                        Box(Modifier.weight(1f)) {
                            Surface(
                                onClick = { subjectMenuOpen = true },
                                shape = RoundedCornerShape(7.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, SlateOutline),
                                modifier = Modifier.fillMaxWidth().height(32.dp).testTag("sheet_subject_dropdown")
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 8.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        activeSub.bn + if (!permissions.canEditSubject(activeSub.key)) " (দেখা)" else "",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SlateText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = SlateMuted)
                                }
                            }
                            DropdownMenu(expanded = subjectMenuOpen, onDismissRequest = { subjectMenuOpen = false }) {
                                visibleSubs.forEach { s ->
                                    val editableSub = permissions.canEditSubject(s.key)
                                    val isSelected = subKey == s.key
                                    DropdownMenuItem(
                                        modifier = Modifier.background(if (isSelected) TealContainer else Color.Transparent),
                                        text = {
                                            Text(
                                                "${s.bn} (${fmtSheetDigits(s.max.toString())})" + if (!editableSub) " · শুধু দেখা" else "",
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) TealDark else if (editableSub) SlateText else SlateMuted
                                            )
                                        },
                                        trailingIcon = if (isSelected) {
                                            { Icon(Icons.Rounded.Check, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        onClick = { subKey = s.key; subjectMenuOpen = false }
                                    )
                                }
                            }
                        }
                    }

                    if (mode == "info" && permissions.canManageStudents) {
                        Surface(
                            onClick = { onBatchEdit(activeCls) },
                            shape = RoundedCornerShape(7.dp),
                            color = TealSoft,
                            border = BorderStroke(1.dp, TealContainer),
                            modifier = Modifier.height(32.dp).testTag("sheet_batch_update_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Rounded.EditNote, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(15.dp))
                                Text("ব্যাচ আপডেট", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TealPrimary, maxLines = 1, softWrap = false)
                            }
                        }
                    } else if (mode == "traits" && permissions.canEditAttendance) {
                        Surface(
                            onClick = {
                                traitBatchSelectMode = !traitBatchSelectMode
                                if (!traitBatchSelectMode) selectedTraitStudentIds = emptySet()
                            },
                            shape = RoundedCornerShape(7.dp),
                            color = if (traitBatchSelectMode) TealPrimary else TealSoft,
                            border = BorderStroke(1.dp, if (traitBatchSelectMode) TealPrimary else TealContainer),
                            modifier = Modifier.height(32.dp).testTag("sheet_trait_batch_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    if (traitBatchSelectMode) Icons.Rounded.Close else Icons.Rounded.DoneAll,
                                    contentDescription = null,
                                    tint = if (traitBatchSelectMode) Color.White else TealPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    if (traitBatchSelectMode) "সিলেক্ট শেষ" else "ব্যাচ সিলেক্ট",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (traitBatchSelectMode) Color.White else TealPrimary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    } else if (mode == "attendance" && permissions.canEditAttendance) {
                        Surface(
                            onClick = {
                                val next = !attBatchFillMode
                                attBatchFillMode = next
                                selectedAttStudentIds = if (next) {
                                    classStudents.map { it.id }.toSet()
                                } else {
                                    emptySet()
                                }
                            },
                            shape = RoundedCornerShape(7.dp),
                            color = if (attBatchFillMode) TealPrimary else TealSoft,
                            border = BorderStroke(1.dp, if (attBatchFillMode) TealPrimary else TealContainer),
                            modifier = Modifier.height(32.dp).testTag("sheet_att_batch_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    if (attBatchFillMode) Icons.Rounded.Close else Icons.Rounded.DoneAll,
                                    contentDescription = null,
                                    tint = if (attBatchFillMode) Color.White else TealPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    if (attBatchFillMode) "ফিল শেষ" else "কার্যদিবস/ব্যাচ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (attBatchFillMode) Color.White else TealPrimary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    } else if (mode == "remarks" && permissions.canEditAttendance && classStudents.isNotEmpty()) {
                        Surface(
                            onClick = {
                                val updates = classStudents.mapNotNull { st ->
                                    val existingRem = st.x["REM_$term"]?.trim().orEmpty()
                                    if (existingRem.isNotEmpty()) return@mapNotNull null
                                    val autoRem = TeacherRemarkEngine.autoPrefillIfApplicable(st, term)
                                        ?: return@mapNotNull null
                                    st.id to autoRem
                                }.toMap()
                                if (updates.isEmpty()) {
                                    onShowMessage("ফাঁকা অবস্থায় থাকা কোনো শিক্ষার্থীর নম্বর, উপস্থিতি বা গুণাবলি পাওয়া যায়নি")
                                } else {
                                    onBatchPatchRemarks(term, updates)
                                }
                            },
                            shape = RoundedCornerShape(7.dp),
                            color = TealPrimary,
                            modifier = Modifier.height(32.dp).testTag("sheet_remarks_autofill_blank_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.AutoFixHigh,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    "ফাঁকা অটো পূরণ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    if (mode != "traits" && mode != "remarks") {
                        Surface(
                            onClick = { navDir = if (navDir == "down") "right" else "down" },
                            shape = RoundedCornerShape(7.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, SlateOutline),
                            modifier = Modifier.height(32.dp).testTag("sheet_nav_dir_chip")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (navDir == "down") "↓ নিচে" else "→ ডানে",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SlateText,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                // Compact single-line status strip (hidden when batch panel is active to save vertical space)
                if (!traitBatchSelectMode && !attBatchFillMode) {
                    if (mode == "subject" && activeSub != null && !permissions.canEditSubject(activeSub.key)) {
                        Surface(
                            color = AmberBg,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "\"${activeSub.bn}\" বিষয়ে শুধুমাত্র দেখার অনুমতি আছে",
                                fontSize = 10.5.sp,
                                color = AmberNotice,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else if (canPasteCurrentSheet && !isEditMode) {
                        Surface(
                            color = SlateBg,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isEditMode = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "নিরাপদ ভিউ চালু — লিখতে ডানের 'এডিট' চাপুন",
                                    fontSize = 10.5.sp,
                                    color = SlateMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "এডিট চালু",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealPrimary
                                )
                            }
                        }
                    } else if (isEditMode && mode == "traits") {
                        Surface(
                            color = TealSoft,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "ঘরে ০.৫ সে. চেপে ধরে ক/খ/গ বাছাই করুন অথবা 'ব্যাচ সিলেক্ট' চাপুন",
                                fontSize = 10.5.sp,
                                color = TealPrimary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else if (isEditMode && canPasteCurrentSheet) {
                        Surface(
                            color = TealSoft,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 8.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "এডিট চালু — ঘরে ট্যাপ করে লিখুন (শেষে 'সম্পন্ন' চাপুন)",
                                    fontSize = 10.5.sp,
                                    color = TealPrimary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(5.dp))
                                        .clickable { applyClipboardPaste() }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .testTag("sheet_paste_btn"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.ContentPaste,
                                        contentDescription = "ক্লিপবোর্ড পেস্ট",
                                        tint = TealPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text("পেস্ট", fontSize = 10.5.sp, color = TealPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (mode == "attendance" && !attBatchFillMode && permissions.canEditAttendance && classStudents.isNotEmpty()) {
            val commonDays = remember(classStudents, term) {
                classStudents.mapNotNull { st ->
                    fromBn(st.x["DAYS_$term"].orEmpty()).filter { it.isDigit() }.takeIf { it.isNotEmpty() }
                }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            }
            Surface(
                color = TealSoft,
                border = BorderStroke(1.dp, TealContainer),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (batchWorkingDaysInput.isBlank() && !commonDays.isNullOrBlank()) {
                            batchWorkingDaysInput = fmtSheetDigits(commonDays)
                        }
                        selectedAttStudentIds = classStudents.map { it.id }.toSet()
                        attBatchFillMode = true
                    }
                    .testTag("sheet_att_quick_days_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (commonDays != null) {
                            "${termNameBn(term)}র কার্যদিবস: ${fmtSheetDigits(commonDays)} দিন · সবার জন্য একসাথে বদলাতে ট্যাপ করুন"
                        } else {
                            "সবার মোট কার্যদিবস বারবার লিখতে হবে না — এখানে একবার বসালেই সবার হবে"
                        },
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TealDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = TealPrimary
                    ) {
                        Text(
                            "একসাথে বসান",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        if (mode == "traits" && traitBatchSelectMode && permissions.canEditAttendance && classStudents.isNotEmpty()) {
            val allSelected = selectedTraitStudentIds.size == classStudents.size && classStudents.isNotEmpty()
            val traitColOptions = listOf(
                "ACT_$term" to "সক্রিয়তা",
                "DISC_$term" to "শৃঙ্খলা",
                "COND_$term" to "আচরণ"
            )
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, TealPrimary.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "নির্বাচিত: ${fmtSheetDigits(selectedTraitStudentIds.size.toString())}/${fmtSheetDigits(classStudents.size.toString())} জন (নামে ট্যাপ করুন)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            onClick = {
                                selectedTraitStudentIds = if (allSelected) {
                                    emptySet()
                                } else {
                                    classStudents.map { it.id }.toSet()
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = if (allSelected) TealPrimary else TealSoft,
                            border = BorderStroke(1.dp, if (allSelected) TealPrimary else TealContainer),
                            modifier = Modifier.height(28.dp).testTag("sheet_trait_select_all_btn")
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    if (allSelected) "সব বাদ" else "সবাই",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (allSelected) Color.White else TealPrimary
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        traitColOptions.forEach { (colKey, colLabel) ->
                            val picked = colKey in selectedTraitCols
                            Surface(
                                onClick = {
                                    selectedTraitCols = if (picked && selectedTraitCols.size > 1) {
                                        selectedTraitCols - colKey
                                    } else {
                                        selectedTraitCols + colKey
                                    }
                                },
                                shape = RoundedCornerShape(7.dp),
                                color = if (picked) TealPrimary else SlateBg,
                                border = BorderStroke(1.dp, if (picked) TealPrimary else SlateOutline),
                                modifier = Modifier.weight(1f).height(30.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (picked) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(Modifier.width(3.dp))
                                    }
                                    Text(
                                        colLabel,
                                        fontSize = 11.sp,
                                        fontWeight = if (picked) FontWeight.Bold else FontWeight.Medium,
                                        color = if (picked) Color.White else SlateText,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        listOf("ক" to "ক", "খ" to "খ", "গ" to "গ", null to "ফাঁকা").forEach { (v, lbl) ->
                            val isSelectedVal = selectedTraitValue == v
                            val accentColor = when (v) {
                                "ক" -> EmeraldGrade
                                "খ" -> AmberNotice
                                "গ" -> RoseDanger
                                else -> SlateText
                            }
                            Surface(
                                onClick = { selectedTraitValue = v },
                                shape = RoundedCornerShape(7.dp),
                                color = if (isSelectedVal) accentColor else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    if (isSelectedVal) 1.5.dp else 1.dp,
                                    if (isSelectedVal) accentColor else accentColor.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier.height(32.dp).testTag("sheet_trait_val_${lbl}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    if (isSelectedVal) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Text(
                                        lbl,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelectedVal) Color.White else accentColor
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        Button(
                            onClick = {
                                onBatchPatchTraits(
                                    selectedTraitStudentIds,
                                    selectedTraitCols,
                                    selectedTraitValue
                                )
                                selectedTraitStudentIds = emptySet()
                            },
                            enabled = selectedTraitStudentIds.isNotEmpty() && selectedTraitCols.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TealPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(7.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp).testTag("sheet_trait_batch_apply_btn")
                        ) {
                            Text(
                                "প্রয়োগ করুন",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        if (mode == "attendance" && attBatchFillMode && permissions.canEditAttendance && classStudents.isNotEmpty()) {
            val allAttSelected = selectedAttStudentIds.size == classStudents.size && classStudents.isNotEmpty()
            val cleanDaysStr = fromBn(batchWorkingDaysInput.trim())
            val cleanPresStr = fromBn(batchPresentDaysInput.trim())
            val parsedDays = if (cleanDaysStr.isEmpty()) null else cleanDaysStr.toIntOrNull()
            val parsedPres = if (cleanPresStr.isEmpty()) null else cleanPresStr.toIntOrNull()
            val daysInvalid = cleanDaysStr.isNotEmpty() && (parsedDays == null || parsedDays !in 0..365)
            val presInvalid = cleanPresStr.isNotEmpty() && (parsedPres == null || parsedPres !in 0..365)
            val crossInvalid = parsedDays != null && parsedPres != null && parsedPres > parsedDays
            val hasAnyAttVal = parsedDays != null || parsedPres != null
            val canApplyAtt = selectedAttStudentIds.isNotEmpty() && hasAnyAttVal && !daysInvalid && !presInvalid && !crossInvalid

            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, TealPrimary.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "নির্বাচিত: ${fmtSheetDigits(selectedAttStudentIds.size.toString())}/${fmtSheetDigits(classStudents.size.toString())} জন (নামে ট্যাপ করুন)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            onClick = {
                                selectedAttStudentIds = if (allAttSelected) {
                                    emptySet()
                                } else {
                                    classStudents.map { it.id }.toSet()
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = if (allAttSelected) TealPrimary else TealSoft,
                            border = BorderStroke(1.dp, if (allAttSelected) TealPrimary else TealContainer),
                            modifier = Modifier.height(28.dp).testTag("sheet_att_select_all_btn")
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    if (allAttSelected) "সব বাদ" else "সবাই",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (allAttSelected) Color.White else TealPrimary
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = batchWorkingDaysInput,
                            onValueChange = { raw ->
                                val filtered = fromBn(raw).filter { it.isDigit() }.take(3)
                                batchWorkingDaysInput = if (useBnDigits) bn(filtered) else filtered
                            },
                            label = { Text("কার্যদিবস", fontSize = 11.sp) },
                            placeholder = { Text(fmtSheetDigits("65"), fontSize = 12.sp) },
                            singleLine = true,
                            isError = daysInvalid,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("sheet_att_batch_days_input")
                        )
                        OutlinedTextField(
                            value = batchPresentDaysInput,
                            onValueChange = { raw ->
                                val filtered = fromBn(raw).filter { it.isDigit() }.take(3)
                                batchPresentDaysInput = if (useBnDigits) bn(filtered) else filtered
                            },
                            label = { Text("উপস্থিতি", fontSize = 11.sp) },
                            placeholder = { Text(fmtSheetDigits("60"), fontSize = 12.sp) },
                            singleLine = true,
                            isError = presInvalid || crossInvalid,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("sheet_att_batch_pres_input")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val hintText = when {
                            daysInvalid || presInvalid -> "সংখ্যা ০ থেকে ${fmtSheetDigits("365")} এর মধ্যে হতে হবে"
                            crossInvalid -> "উপস্থিতি কার্যদিবসের চেয়ে বেশি হতে পারবে না"
                            else -> "১টি বা উভয় ঘরে সংখ্যা লিখে প্রয়োগ করুন"
                        }
                        Text(
                            hintText,
                            fontSize = 10.5.sp,
                            color = if (daysInvalid || presInvalid || crossInvalid) RoseDanger else SlateMuted,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                focusManager.clearFocus(force = true)
                                onBatchPatchAttendance(
                                    selectedAttStudentIds,
                                    term,
                                    parsedDays,
                                    parsedPres
                                )
                            },
                            enabled = canApplyAtt,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TealPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(7.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp).testTag("sheet_att_batch_apply_btn")
                        ) {
                            Text(
                                "প্রয়োগ করুন",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // Frozen Roll+Name Column + Horizontally Scrollable Grid
        Card(
            Modifier.weight(1f).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (classStudents.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "${classNameBn(activeCls, settings)} শ্রেণিতে এখনো কোনো শিক্ষার্থী নেই",
                        color = SlateMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (permissions.canManageStudents) "শিক্ষার্থী ট্যাব থেকে নতুন শিক্ষার্থী যোগ করুন অথবা CSV ইমপোর্ট করুন"
                        else "অ্যাডমিন বা অনুমোদিত শিক্ষক শিক্ষার্থী যোগ করলে এখানে তালিকা দেখাবে",
                        color = SlateMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (mode == "remarks") {
                BulkRemarksPanel(
                    classStudents = classStudents,
                    term = term,
                    canEdit = permissions.canEditAttendance,
                    onApplySingle = { studentId, text ->
                        onPatchStudentField(studentId, "x.REM_$term", text)
                    },
                    onOpenPicker = { student -> remarkPickerStudent = student },
                    onBatchFillAllWithData = {
                        val updates = classStudents.mapNotNull { st ->
                            val existingRem = st.x["REM_$term"]?.trim().orEmpty()
                            if (existingRem.isNotEmpty()) return@mapNotNull null // Never overwrite manually edited or existing comments
                            val autoRem = TeacherRemarkEngine.autoPrefillIfApplicable(st, term)
                                ?: return@mapNotNull null
                            st.id to autoRem
                        }.toMap()
                        if (updates.isEmpty()) {
                            onShowMessage("ফাঁকা মন্তব্যযুক্ত ও তথ্যসম্পন্ন কোনো শিক্ষার্থী নেই")
                        } else {
                            onBatchPatchRemarks(term, updates)
                        }
                    },
                    onOpenStudent = onOpenStudent
                )
            } else {
                Column(Modifier.fillMaxSize()) {
                    // Header row
                    Row(Modifier.fillMaxWidth().background(SlateSurfaceVariant)) {
                        Box(
                            Modifier.width(136.dp).height(46.dp).padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text("রোল · শিক্ষার্থী", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SlateText)
                        }
                        Box(Modifier.width(1.dp).height(46.dp).background(SlateOutline))
                        Row(Modifier.weight(1f).horizontalScroll(hScroll)) {
                            var renderedContHeader = false
                            val contMaxForSub = activeSub?.comps?.sumOf { it.max } ?: 0
                            cols.forEach { col ->
                                if (mode == "subject" && col.markCompKey == "WR" && !renderedContHeader) {
                                    renderedContHeader = true
                                    SheetComputedHeader(
                                        label = "ধারাবাহিক",
                                        subLabel = if (contMaxForSub > 0) "সর্বোচ্চ ${fmtSheetDigits(contMaxForSub.toString())}" else ""
                                    )
                                }
                                val colEditable = isColumnEditable(col)
                                Column(
                                    Modifier.width(col.width).height(46.dp).padding(horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        col.label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (colEditable) SlateText else SlateMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val subLbl = if (!colEditable && col.subLabel.isNotBlank()) "দেখা" else col.subLabel
                                    if (subLbl.isNotBlank()) {
                                        Text(subLbl, fontSize = 10.sp, color = SlateMuted, maxLines = 1)
                                    }
                                }
                                Box(Modifier.width(1.dp).height(46.dp).background(SlateOutline))
                            }
                            when (mode) {
                                "subject" -> {
                                    if (!renderedContHeader) {
                                        SheetComputedHeader(
                                            label = "ধারাবাহিক",
                                            subLabel = if (contMaxForSub > 0) "সর্বোচ্চ ${fmtSheetDigits(contMaxForSub.toString())}" else ""
                                        )
                                    }
                                    val totalMax = activeSub?.max ?: 0
                                    SheetComputedHeader(
                                        label = "মোট",
                                        subLabel = if (totalMax > 0) "সর্বোচ্চ ${fmtSheetDigits(totalMax.toString())}" else ""
                                    )
                                    SheetComputedHeader("গ্রেড")
                                }
                                "all" -> if (permissions.allSubjects || permissions.canViewOtherSubjects) {
                                    SheetComputedHeader("গড় %")
                                }
                                "attendance" -> SheetComputedHeader("হার %")
                            }
                        }
                    }
                    HorizontalDivider(color = SlateOutline)

                    LazyColumn(Modifier.fillMaxSize()) {
                        itemsIndexed(classStudents, key = { _, s -> s.id }) { rIdx, s ->
                            val rowHeight = 48.dp
                            val isTraitRowSelected = mode == "traits" && traitBatchSelectMode && (s.id in selectedTraitStudentIds)
                            val isAttRowSelected = mode == "attendance" && attBatchFillMode && (s.id in selectedAttStudentIds)
                            val isRowSelected = isTraitRowSelected || isAttRowSelected
                            val isAnyBatchSelect = (mode == "traits" && traitBatchSelectMode && permissions.canEditAttendance) ||
                                (mode == "attendance" && attBatchFillMode && permissions.canEditAttendance)
                            Column {
                                Row(Modifier.fillMaxWidth().height(rowHeight), verticalAlignment = Alignment.CenterVertically) {
                                    Row(
                                        Modifier
                                            .width(136.dp)
                                            .fillMaxHeight()
                                            .background(if (isRowSelected) TealContainer else Color.Transparent)
                                            .clickable {
                                                when {
                                                    mode == "traits" && traitBatchSelectMode && permissions.canEditAttendance -> {
                                                        selectedTraitStudentIds = if (isTraitRowSelected) {
                                                            selectedTraitStudentIds - s.id
                                                        } else {
                                                            selectedTraitStudentIds + s.id
                                                        }
                                                    }
                                                    mode == "attendance" && attBatchFillMode && permissions.canEditAttendance -> {
                                                        selectedAttStudentIds = if (isAttRowSelected) {
                                                            selectedAttStudentIds - s.id
                                                        } else {
                                                            selectedAttStudentIds + s.id
                                                        }
                                                    }
                                                    else -> onOpenStudent(s)
                                                }
                                            }
                                            .padding(horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (isAnyBatchSelect) {
                                            Checkbox(
                                                checked = isRowSelected,
                                                onCheckedChange = null,
                                                colors = CheckboxDefaults.colors(checkedColor = TealPrimary),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Text(
                                            fmtSheetDigits(s.roll.toString().padStart(2, '0')),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isRowSelected) TealDark else TealPrimary
                                        )
                                        Text(
                                            s.name.ifBlank { "(নাম নেই)" },
                                            fontSize = 12.sp,
                                            fontWeight = if (isRowSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isRowSelected) TealDark else SlateText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Box(Modifier.width(1.dp).fillMaxHeight().background(SlateOutline))

                                    Row(Modifier.weight(1f).fillMaxHeight().horizontalScroll(hScroll)) {
                                        val subForStudent = if (mode == "subject" && activeSub != null) {
                                            s.subs().find { it.key == activeSub.key } ?: activeSub
                                        } else null
                                        var renderedContCell = false
                                        cols.forEachIndexed { cIdx, col ->
                                            if (mode == "subject" && col.markCompKey == "WR" && !renderedContCell && subForStudent != null) {
                                                renderedContCell = true
                                                val cScore = s.cont(subForStudent, term)
                                                SheetComputedCell(fmtSheetNum(cScore), SlateBg, SlateMuted)
                                            }
                                            val hasPerm = isColumnEditable(col)
                                            val colEditable = isEditMode && hasPerm

                                            if (col.isTraitCycle && col.xKey != null) {
                                                val currentVal = s.x[col.xKey] ?: ""
                                                val badgeBg = when {
                                                    isTraitRowSelected -> TealContainer
                                                    currentVal == "ক" -> TealSoft
                                                    currentVal == "খ" -> AmberBg
                                                    currentVal == "গ" -> RoseDanger.copy(alpha = 0.1f)
                                                    else -> MaterialTheme.colorScheme.surface
                                                }
                                                val badgeFg = when (currentVal) {
                                                    "ক" -> EmeraldGrade
                                                    "খ" -> AmberNotice
                                                    "গ" -> RoseDanger
                                                    else -> SlateMuted
                                                }

                                                val cellMenuKey = "${s.id}__${col.xKey}"
                                                var holdProgress by remember(s.id, col.xKey) { mutableFloatStateOf(0f) }
                                                var isHolding by remember(s.id, col.xKey) { mutableStateOf(false) }
                                                val latestCanEdit by rememberUpdatedState(colEditable)
                                                val latestHasPerm by rememberUpdatedState(hasPerm)
                                                val latestBatchSelect by rememberUpdatedState(traitBatchSelectMode)
                                                val latestToggleBatchRow by rememberUpdatedState {
                                                    selectedTraitStudentIds = if (s.id in selectedTraitStudentIds) {
                                                        selectedTraitStudentIds - s.id
                                                    } else {
                                                        selectedTraitStudentIds + s.id
                                                    }
                                                }
                                                val latestOpenMenu by rememberUpdatedState {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    activeTraitMenuCellKey = cellMenuKey
                                                }

                                                Box(
                                                    Modifier
                                                        .width(col.width)
                                                        .fillMaxHeight()
                                                        .background(if (hasPerm) badgeBg else SlateBg)
                                                        .pointerInput(s.id, col.xKey) {
                                                            coroutineScope {
                                                                val holdScope = this
                                                                awaitEachGesture {
                                                                    val down = awaitFirstDown(requireUnconsumed = false)
                                                                    if (latestBatchSelect && latestHasPerm) {
                                                                        val up = waitForUpOrCancellation()
                                                                        if (up != null) {
                                                                            latestToggleBatchRow()
                                                                        }
                                                                        return@awaitEachGesture
                                                                    }
                                                                    if (!latestCanEdit) {
                                                                        val up = waitForUpOrCancellation()
                                                                        if (up != null) {
                                                                            onShowMessage(
                                                                                if (!latestHasPerm) "গুণাবলি সম্পাদনার অনুমতি নেই"
                                                                                else "পরিবর্তন করতে উপরের 'এডিট' বা 'ব্যাচ সিলেক্ট' চাপুন"
                                                                            )
                                                                        }
                                                                        return@awaitEachGesture
                                                                    }

                                                                    val startPos = down.position
                                                                    val startTime = System.currentTimeMillis()
                                                                    var completed = false
                                                                    var movedAway = false
                                                                    isHolding = true
                                                                    holdProgress = 0f

                                                                    val holdDurationMs = 450L
                                                                    val timerJob = holdScope.launch {
                                                                        while (isActive) {
                                                                            val elapsed = System.currentTimeMillis() - startTime
                                                                            holdProgress = (elapsed.toFloat() / holdDurationMs).coerceIn(0f, 1f)
                                                                            if (elapsed >= holdDurationMs) {
                                                                                completed = true
                                                                                isHolding = false
                                                                                holdProgress = 0f
                                                                                latestOpenMenu()
                                                                                break
                                                                            }
                                                                            delay(16L)
                                                                        }
                                                                    }

                                                                    try {
                                                                        while (!completed) {
                                                                            val event = awaitPointerEvent()
                                                                            val change = event.changes.firstOrNull { it.id == down.id }
                                                                            if (change == null || !change.pressed || change.isConsumed) {
                                                                                break
                                                                            }
                                                                            if ((change.position - startPos).getDistance() > viewConfiguration.touchSlop) {
                                                                                movedAway = true
                                                                                break
                                                                            }
                                                                        }
                                                                    } finally {
                                                                        timerJob.cancel()
                                                                        val heldDuration = System.currentTimeMillis() - startTime
                                                                        isHolding = false
                                                                        holdProgress = 0f
                                                                        if (!completed && !movedAway && heldDuration < holdDurationMs) {
                                                                            onShowMessage("বিকল্প খুলতে ০.৫ সেকেন্ড চেপে ধরুন")
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isHolding && holdProgress > 0f) {
                                                        Box(
                                                            Modifier
                                                                .align(Alignment.CenterStart)
                                                                .fillMaxHeight()
                                                                .fillMaxWidth(holdProgress)
                                                                .background(TealPrimary.copy(alpha = 0.22f))
                                                        )
                                                        Box(
                                                            Modifier
                                                                .align(Alignment.BottomStart)
                                                                .height(3.dp)
                                                                .fillMaxWidth(holdProgress)
                                                                .background(TealPrimary)
                                                        )
                                                    }
                                                    if (isHolding) {
                                                        Text(
                                                            "ধরুন...",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TealPrimary
                                                        )
                                                    } else {
                                                        Text(
                                                            currentVal.ifEmpty { "—" },
                                                            fontSize = 13.sp,
                                                            fontWeight = if (currentVal.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                                                            color = badgeFg
                                                        )
                                                    }

                                                    DropdownMenu(
                                                        expanded = activeTraitMenuCellKey == cellMenuKey,
                                                        onDismissRequest = { activeTraitMenuCellKey = null }
                                                    ) {
                                                        listOf(
                                                            Triple("ক", "ক · সর্বোত্তম", EmeraldGrade),
                                                            Triple("খ", "খ · উত্তম", AmberNotice),
                                                            Triple("গ", "গ · অগ্রগতি প্রয়োজন", RoseDanger)
                                                        ).forEach { (optVal, optLabel, optColor) ->
                                                            DropdownMenuItem(
                                                                modifier = Modifier.background(if (currentVal == optVal) TealContainer.copy(alpha = 0.7f) else Color.Transparent),
                                                                text = {
                                                                    Text(
                                                                        optLabel,
                                                                        fontSize = 13.sp,
                                                                        fontWeight = if (currentVal == optVal) FontWeight.Bold else FontWeight.Medium,
                                                                        color = optColor
                                                                    )
                                                                },
                                                                trailingIcon = if (currentVal == optVal) {
                                                                    {
                                                                        Icon(
                                                                            Icons.Rounded.Check,
                                                                            contentDescription = null,
                                                                            tint = optColor,
                                                                            modifier = Modifier.size(16.dp)
                                                                        )
                                                                    }
                                                                } else null,
                                                                onClick = {
                                                                    activeTraitMenuCellKey = null
                                                                    onPatchStudentField(s.id, "x.${col.xKey}", optVal)
                                                                }
                                                            )
                                                        }
                                                        HorizontalDivider()
                                                        DropdownMenuItem(
                                                            text = {
                                                                Text(
                                                                    "ফাঁকা (মুছুন)",
                                                                    fontSize = 12.5.sp,
                                                                    color = SlateMuted
                                                                )
                                                            },
                                                            leadingIcon = {
                                                                Icon(
                                                                    Icons.Rounded.DeleteOutline,
                                                                    contentDescription = null,
                                                                    tint = SlateMuted,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            },
                                                            onClick = {
                                                                activeTraitMenuCellKey = null
                                                                onPatchStudentField(s.id, "x.${col.xKey}", null)
                                                            }
                                                        )
                                                    }
                                                }
                                                Box(Modifier.width(1.dp).fillMaxHeight().background(SlateOutline))
                                                return@forEachIndexed
                                            }

                                            val initialVal = remember(s.id, s.m, s.x, s.guardian, s.mobile, s.village, s.union, s.upazila, s.district, s.religion, col.key, term, useBnDigits) {
                                                when {
                                                    col.isMark && col.markSubKey != null && col.markCompKey != null -> {
                                                        val explicit = s.m[mk(col.markSubKey, term, col.markCompKey)]
                                                        if (explicit != null) {
                                                            fmtSheetNum(explicit)
                                                        } else if (col.markCompKey == "CONT") {
                                                            val subDef = allSubsForClass.find { it.key == col.markSubKey }
                                                            if (subDef != null && s.hasSub(subDef, term)) fmtSheetNum(s.cont(subDef, term)) else ""
                                                        } else {
                                                            ""
                                                        }
                                                    }
                                                    col.xKey != null -> {
                                                        val rawX = s.x[col.xKey].orEmpty()
                                                        if (col.isNumeric && rawX.isNotBlank()) fmtSheetDigits(rawX) else rawX
                                                    }
                                                    col.infoFieldKey != null -> when (col.infoFieldKey) {
                                                        "guardian" -> s.guardian
                                                        "mobile" -> s.mobile
                                                        "village" -> s.village
                                                        "union" -> s.union
                                                        "upazila" -> s.upazila.ifBlank { com.kgps.result.DEFAULT_UPAZILA }
                                                        "district" -> s.district.ifBlank { com.kgps.result.DEFAULT_DISTRICT }
                                                        "religion" -> s.religion
                                                        else -> ""
                                                    }
                                                    else -> ""
                                                }
                                            }
                                            var draft by remember(s.id, col.key, term) { mutableStateOf(initialVal) }
                                            var isFocused by remember { mutableStateOf(false) }

                                            LaunchedEffect(initialVal, isFocused) {
                                                if (!isFocused) {
                                                    draft = initialVal
                                                }
                                            }

                                            val parsedNum = if (col.isNumeric && col.infoFieldKey != "mobile") fromBn(draft.trim()).toDoubleOrNull() else null
                                            val invalid = col.isNumeric && col.infoFieldKey != "mobile" && draft.isNotBlank() &&
                                                (parsedNum == null || parsedNum < 0 || parsedNum > col.max)

                                            fun commitDraft(text: String) {
                                                if (!hasPerm) return
                                                val clean = fromBn(text.trim())
                                                if (col.isMark && col.markSubKey != null && col.markCompKey != null) {
                                                    val k = mk(col.markSubKey, term, col.markCompKey)
                                                    val subDef = allSubsForClass.find { it.key == col.markSubKey }
                                                    if (clean.isEmpty()) {
                                                        if (s.m.containsKey(k)) {
                                                            onPatchStudentField(s.id, "m.$k", null)
                                                        }
                                                        if (col.markCompKey == "CONT") {
                                                            listOf("TB", "ACT", "LANG", "CT", "TOTAL").forEach { comp ->
                                                                val ck = mk(col.markSubKey, term, comp)
                                                                if (s.m.containsKey(ck)) onPatchStudentField(s.id, "m.$ck", null)
                                                            }
                                                        }
                                                    } else {
                                                        clean.toDoubleOrNull()?.let { d ->
                                                            if (d in 0.0..col.max.toDouble()) {
                                                                val currentShown = s.m[k] ?: if (col.markCompKey == "CONT" && subDef != null && s.hasSub(subDef, term)) s.cont(subDef, term) else null
                                                                if (currentShown != d) {
                                                                    onPatchStudentField(s.id, "m.$k", d)
                                                                    if (col.markCompKey == "CONT") {
                                                                        listOf("TB", "ACT", "LANG", "CT", "TOTAL").forEach { comp ->
                                                                            val ck = mk(col.markSubKey, term, comp)
                                                                            if (s.m.containsKey(ck)) onPatchStudentField(s.id, "m.$ck", null)
                                                                        }
                                                                    } else if (col.markCompKey in listOf("TB", "ACT", "LANG", "CT")) {
                                                                        val contK = mk(col.markSubKey, term, "CONT")
                                                                        if (s.m.containsKey(contK)) onPatchStudentField(s.id, "m.$contK", null)
                                                                        val totK = mk(col.markSubKey, term, "TOTAL")
                                                                        if (s.m.containsKey(totK)) onPatchStudentField(s.id, "m.$totK", null)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else if (col.xKey != null) {
                                                    val rawTrim = text.trim()
                                                    if (rawTrim.isEmpty()) {
                                                        if (s.x.containsKey(col.xKey)) {
                                                            onPatchStudentField(s.id, "x.${col.xKey}", null)
                                                        }
                                                    } else if (col.isNumeric) {
                                                        clean.toIntOrNull()?.let { iv ->
                                                            if (iv in 0..col.max && s.x[col.xKey] != iv.toString()) {
                                                                onPatchStudentField(s.id, "x.${col.xKey}", iv.toString())
                                                            }
                                                        }
                                                    } else if (s.x[col.xKey] != rawTrim) {
                                                        onPatchStudentField(s.id, "x.${col.xKey}", rawTrim)
                                                    }
                                                } else if (col.infoFieldKey != null) {
                                                    val v = text.trim()
                                                    when (col.infoFieldKey) {
                                                        "guardian" -> if (v != s.guardian) onPatchStudentField(s.id, "guardian", v)
                                                        "mobile" -> if (v != s.mobile) onPatchStudentField(s.id, "mobile", v)
                                                        "village" -> if (v != s.village) onPatchStudentField(s.id, "village", v)
                                                        "union" -> if (v != s.union) onPatchStudentField(s.id, "union", v)
                                                        "upazila" -> {
                                                            val safeUp = v.ifBlank { com.kgps.result.DEFAULT_UPAZILA }
                                                            if (safeUp != s.upazila) onPatchStudentField(s.id, "upazila", safeUp)
                                                        }
                                                        "district" -> {
                                                            val safeDist = v.ifBlank { com.kgps.result.DEFAULT_DISTRICT }
                                                            if (safeDist != s.district) onPatchStudentField(s.id, "district", safeDist)
                                                        }
                                                        "religion" -> {
                                                            val safeRel = v.ifBlank { s.religion }
                                                            if (safeRel != s.religion) onPatchStudentField(s.id, "religion", safeRel)
                                                        }
                                                    }
                                                }
                                            }

                                            Box(
                                                Modifier
                                                    .width(col.width)
                                                    .fillMaxHeight()
                                                    .background(
                                                        when {
                                                            invalid -> RoseDanger.copy(alpha = 0.08f)
                                                            isRowSelected -> TealContainer.copy(alpha = 0.7f)
                                                            !colEditable -> SlateBg
                                                            isFocused -> TealContainer.copy(alpha = 0.35f)
                                                            else -> MaterialTheme.colorScheme.surface
                                                        }
                                                    )
                                                    .padding(horizontal = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                BasicTextField(
                                                    value = draft,
                                                    onValueChange = { v ->
                                                        if (colEditable) {
                                                            draft = if (col.isNumeric && col.infoFieldKey != "mobile") {
                                                                if (useBnDigits) bn(v) else fromBn(v)
                                                            } else {
                                                                v
                                                            }
                                                        }
                                                    },
                                                    enabled = colEditable,
                                                    readOnly = !colEditable,
                                                    singleLine = true,
                                                    textStyle = TextStyle(
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = when {
                                                            !colEditable -> SlateMuted
                                                            invalid -> RoseDanger
                                                            else -> SlateText
                                                        },
                                                        textAlign = if (col.isNumeric) TextAlign.Center else TextAlign.Start
                                                    ),
                                                    cursorBrush = SolidColor(TealPrimary),
                                                    keyboardOptions = KeyboardOptions(
                                                        keyboardType = if (col.isNumeric) KeyboardType.Decimal else KeyboardType.Text,
                                                        imeAction = ImeAction.Next
                                                    ),
                                                    keyboardActions = KeyboardActions(
                                                        onNext = {
                                                            commitDraft(draft)
                                                            moveFocusFrom(rIdx, cIdx)
                                                        }
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .focusRequester(focusMatrix[rIdx][cIdx])
                                                        .onFocusChanged { fs ->
                                                            if (isFocused && !fs.isFocused) commitDraft(draft)
                                                            isFocused = fs.isFocused
                                                        }
                                                )
                                                if (draft.isEmpty() && !isFocused) {
                                                    Text("—", fontSize = 12.sp, color = SlateOutline)
                                                }
                                            }
                                            Box(Modifier.width(1.dp).fillMaxHeight().background(SlateOutline))
                                        }

                                        if (mode == "subject" && subForStudent != null) {
                                            if (!renderedContCell) {
                                                val cScore = s.cont(subForStudent, term)
                                                SheetComputedCell(fmtSheetNum(cScore), SlateBg, SlateMuted)
                                            }
                                            val tScore = s.total(subForStudent, term)
                                            val grade = gradeOf((tScore / subForStudent.max.toDouble()) * 100.0)
                                            SheetComputedCell(fmtSheetNum(tScore), TealContainer.copy(alpha = 0.25f), TealPrimary, bold = true)
                                            SheetComputedCell(grade, SlateBg, TealPrimary, bold = true)
                                        } else if (mode == "all" && (permissions.allSubjects || permissions.canViewOtherSubjects)) {
                                            val maxSum = allSubsForClass.sumOf { it.max }.coerceAtLeast(1)
                                            val avgSum = allSubsForClass.sumOf { s.avg(it) }
                                            val pct = avgSum / maxSum * 100.0
                                            SheetComputedCell("${fmtSheetNum(pct)}%", TealContainer.copy(alpha = 0.25f), TealPrimary, bold = true)
                                        } else if (mode == "attendance") {
                                            val dVal = s.x["DAYS_$term"]?.let { fromBn(it).toDoubleOrNull() }
                                            val pVal = s.x["PRES_$term"]?.let { fromBn(it).toDoubleOrNull() }
                                            val pctStr = if (dVal != null && pVal != null && dVal > 0) "${fmtSheetNum((pVal / dVal) * 100)}%" else "—"
                                            SheetComputedCell(
                                                pctStr,
                                                if (isRowSelected) TealContainer else TealContainer.copy(alpha = 0.25f),
                                                if (isRowSelected) TealDark else TealPrimary,
                                                bold = true
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(color = SlateOutline)
                            }
                        }
                    }
                }
            }
        }
    }

    remarkPickerStudent?.let { targetStudent ->
        TeacherRemarkPickerDialog(
            student = targetStudent,
            term = term,
            currentRemark = targetStudent.x["REM_$term"].orEmpty(),
            onSelect = { chosen ->
                onPatchStudentField(targetStudent.id, "x.REM_$term", chosen)
                remarkPickerStudent = null
            },
            onDismiss = { remarkPickerStudent = null }
        )
    }
}

@Composable
private fun BulkRemarksPanel(
    classStudents: List<Student>,
    term: Int,
    canEdit: Boolean,
    onApplySingle: (studentId: String, remark: String) -> Unit,
    onOpenPicker: (Student) -> Unit,
    onBatchFillAllWithData: () -> Unit,
    onOpenStudent: (Student) -> Unit
) {
    val filledCount = remember(classStudents, term) {
        classStudents.count { !it.x["REM_$term"].isNullOrBlank() }
    }
    val emptyReadyCount = remember(classStudents, term) {
        classStudents.count { st ->
            st.x["REM_$term"].isNullOrBlank() &&
                TeacherRemarkEngine.autoPrefillIfApplicable(st, term) != null
        }
    }
    var askBatchAutoGenerate by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Surface(color = SlateSurfaceVariant) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${termNameBn(term)} · মন্তব্য সম্পন্ন: ${bn(filledCount.toString())}/${bn(classStudents.size.toString())} জন",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateText,
                        modifier = Modifier.weight(1f)
                    )
                    if (canEdit) {
                        Surface(
                            onClick = { askBatchAutoGenerate = true },
                            shape = RoundedCornerShape(7.dp),
                            color = TealSoft,
                            border = BorderStroke(1.dp, TealContainer),
                            modifier = Modifier.testTag("sheet_remarks_reset_all_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.AutoFixHigh,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    "সব শিক্ষার্থীর জন্য অটো মন্তব্য তৈরি করুন",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = SlateOutline)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(classStudents, key = { _, s -> s.id }) { _, student ->
                val currentRem = student.x["REM_$term"]?.trim().orEmpty()
                val profile = remember(student, term) {
                    TeacherRemarkEngine.analyzeProfile(student, term)
                }
                val rankedOptions = remember(student, term) {
                    TeacherRemarkEngine.getRankedAlternatives(student, term)
                }
                val topOpt = rankedOptions.firstOrNull()
                val isUsingTop = topOpt != null && currentRem == topOpt.text.trim()

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (currentRem.isNotEmpty()) TealContainer else SlateOutline
                    ),
                    shape = RoundedCornerShape(11.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onOpenStudent(student) }
                                    .padding(vertical = 2.dp)
                            ) {
                                Surface(
                                    color = TealSoft,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        bn(student.roll.toString()),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TealPrimary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    student.name.ifBlank { "নামহীন শিক্ষার্থী" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                color = when {
                                    isUsingTop -> TealSoft
                                    currentRem.isNotEmpty() -> SlateSurfaceVariant
                                    else -> AmberBg
                                },
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = when {
                                        isUsingTop -> "সেরা অটো ম্যাচ"
                                        currentRem.isNotEmpty() -> "নির্বাচিত"
                                        else -> "অপেক্ষমান"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when {
                                        isUsingTop -> TealDark
                                        currentRem.isNotEmpty() -> SlateText
                                        else -> AmberNotice
                                    },
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        StudentRemarkContextSummary(profile = profile)

                        if (currentRem.isNotEmpty()) {
                            val curWc = CommentGenerator.countWords(currentRem)
                            val curOver = curWc > CommentGenerator.MAX_WORDS
                            Surface(
                                color = SlateBg,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (curOver) RoseDanger else SlateOutline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = currentRem,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp,
                                        color = SlateText
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(
                                            text = "${bn(curWc.toString())}/${bn(CommentGenerator.MAX_WORDS.toString())} শব্দ",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (curOver) RoseDanger else SlateMuted
                                        )
                                    }
                                }
                            }
                        } else if (topOpt != null) {
                            Surface(
                                color = TealSoft.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, TealContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        if (profile.hasAnyData) "অটো প্রস্তাবনা (সেরা ম্যাচ):" else "সাধারণ প্রস্তাবনা (তথ্য দিলে আরও নির্ভুল হবে):",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TealPrimary
                                    )
                                    Text(
                                        text = topOpt.text,
                                        fontSize = 11.5.sp,
                                        lineHeight = 16.sp,
                                        color = SlateText
                                    )
                                }
                            }
                        }

                        if (canEdit) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (topOpt != null && currentRem != topOpt.text.trim()) {
                                    Surface(
                                        onClick = { onApplySingle(student.id, topOpt.text) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = TealPrimary,
                                        modifier = Modifier.testTag("sheet_remark_apply_top_${student.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                if (currentRem.isEmpty()) "ব্যবহার করুন" else "সেরা অটো",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                if (rankedOptions.size > 1) {
                                    Surface(
                                        onClick = {
                                            val curIdx = rankedOptions.indexOfFirst { it.text.trim() == currentRem }
                                            val pool = rankedOptions.size.coerceAtMost(8)
                                            val nextIdx = if (curIdx < 0) 0 else (curIdx + 1) % pool
                                            onApplySingle(student.id, rankedOptions[nextIdx].text)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = SlateSurfaceVariant,
                                        border = BorderStroke(1.dp, SlateOutline),
                                        modifier = Modifier.testTag("sheet_remark_next_${student.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.SwapHoriz,
                                                contentDescription = null,
                                                tint = SlateText,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                "অন্য বিকল্প",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SlateText
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    onClick = { onOpenPicker(student) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = TealSoft,
                                    border = BorderStroke(1.dp, TealContainer),
                                    modifier = Modifier.testTag("sheet_remark_all_${student.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Tune,
                                            contentDescription = null,
                                            tint = TealPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            "সব বিকল্প (${bn(rankedOptions.size.toString())})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TealPrimary
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

    if (askBatchAutoGenerate) {
        AlertDialog(
            onDismissRequest = { askBatchAutoGenerate = false },
            title = { Text("অটো মন্তব্য তৈরি করবেন?") },
            text = {
                Text(
                    buildString {
                        append("${termNameBn(term)}-এ যে ${bn(emptyReadyCount.toString())} জন শিক্ষার্থীর মন্তব্য এখনো খালি আছে ও নম্বর/উপস্থিতি দেওয়া আছে, তাদের জন্য অটো মন্তব্য তৈরি হবে।\n\n")
                        append("আগে থেকে থাকা বা ম্যানুয়ালি এডিট করা ${bn(filledCount.toString())}টি মন্তব্য অপরিবর্তিত থাকবে।")
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        askBatchAutoGenerate = false
                        onBatchFillAllWithData()
                    },
                    enabled = emptyReadyCount > 0,
                    modifier = Modifier.testTag("sheet_remarks_confirm_batch_btn")
                ) {
                    Text("তৈরি করুন (${bn(emptyReadyCount.toString())} জন)")
                }
            },
            dismissButton = {
                TextButton(onClick = { askBatchAutoGenerate = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
private fun SheetComputedHeader(label: String, subLabel: String = "") {
    Column(
        Modifier.width(74.dp).height(46.dp).padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            label,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = SlateText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (subLabel.isNotBlank()) {
            Text(
                subLabel,
                fontSize = 10.sp,
                color = SlateMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    Box(Modifier.width(1.dp).height(46.dp).background(SlateOutline))
}

@Composable
private fun SheetComputedCell(
    text: String,
    bg: Color,
    fg: Color,
    bold: Boolean = false
) {
    Box(Modifier.width(74.dp).fillMaxHeight().background(bg), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 12.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = fg)
    }
    Box(Modifier.width(1.dp).fillMaxHeight().background(SlateOutline))
}
