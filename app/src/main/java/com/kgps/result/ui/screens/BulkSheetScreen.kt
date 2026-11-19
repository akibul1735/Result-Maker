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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Lock
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
import com.kgps.result.CLSN
import com.kgps.result.RELIGIONS
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.avg
import com.kgps.result.bn
import com.kgps.result.cloud.TeacherPermissions
import com.kgps.result.cont
import com.kgps.result.fmt
import com.kgps.result.fmtEn
import com.kgps.result.fromBn
import com.kgps.result.gradeOf
import com.kgps.result.mk
import com.kgps.result.subjectsFor
import com.kgps.result.subs
import com.kgps.result.total
import com.kgps.result.ui.theme.AmberBg
import com.kgps.result.ui.theme.AmberNotice
import com.kgps.result.ui.theme.EmeraldGrade
import com.kgps.result.ui.theme.RoseDanger
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateSurfaceVariant
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealContainer
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft

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
    onShowMessage: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val allowedClasses = remember(permissions) { permissions.effectiveClasses() }
    val activeCls = if (cls in allowedClasses) cls else (allowedClasses.firstOrNull() ?: cls)

    val availableModes = remember(permissions) {
        SHEET_MODES.filter { (id, _) ->
            when (id) {
                "subject" -> permissions.allSubjects || permissions.effectiveSubjects().isNotEmpty() || permissions.canViewOtherSubjects
                "all" -> permissions.allSubjects || permissions.effectiveSubjects().isNotEmpty() || permissions.canViewOtherSubjects
                "attendance", "traits" -> permissions.canEditAttendance
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
                val isArt = sub.key == "ART"
                SheetColDef(
                    key = "all_${sub.key}",
                    label = shortName,
                    subLabel = "${if (isArt) "মোট" else "সামষ্টিক"} (${fmtSheetDigits((if (sub.written > 0) sub.written else sub.max).toString())})",
                    max = if (sub.written > 0) sub.written else sub.max,
                    width = 86.dp,
                    isMark = true,
                    markSubKey = sub.key,
                    markCompKey = if (isArt) "TOTAL" else if (sub.written > 0) "WR" else "TB"
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
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Compact 2-Row Android Toolbar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Row 1: Class + Term + Quick Actions
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box {
                        OutlinedButton(
                            onClick = { classMenuOpen = true },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SlateOutline),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("sheet_class_dropdown")
                        ) {
                            Text("${CLSN[activeCls - 1]} শ্রেণি", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(expanded = classMenuOpen, onDismissRequest = { classMenuOpen = false }) {
                            allowedClasses.forEach { c ->
                                val count = students.count { it.cls == c }
                                DropdownMenuItem(
                                    text = { Text("${CLSN[c - 1]} শ্রেণি (${bn(count.toString())} জন)") },
                                    onClick = { onSelectClass(c); classMenuOpen = false }
                                )
                            }
                        }
                    }

                    if (mode != "info") {
                        Box {
                            OutlinedButton(
                                onClick = { termMenuOpen = true },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, SlateOutline),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("sheet_term_dropdown")
                            ) {
                                Text("${bn(term.toString())} প্রান্তিক", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(expanded = termMenuOpen, onDismissRequest = { termMenuOpen = false }) {
                                (1..3).forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text("${bn(t.toString())} প্রান্তিক") },
                                        onClick = { term = t; termMenuOpen = false }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    if (canPasteCurrentSheet) {
                        Button(
                            onClick = {
                                if (isEditMode) {
                                    focusManager.clearFocus(force = true)
                                    isEditMode = false
                                    onShowMessage("এডিট মোড বন্ধ করা হয়েছে")
                                } else {
                                    isEditMode = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEditMode) TealPrimary else TealSoft,
                                contentColor = if (isEditMode) Color.White else TealPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("sheet_edit_toggle_btn")
                        ) {
                            Icon(
                                if (isEditMode) Icons.Rounded.Check else Icons.Rounded.Edit,
                                contentDescription = if (isEditMode) "এডিট সম্পন্ন" else "এডিট চালু",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (isEditMode) "সম্পন্ন" else "এডিট",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    if (permissions.canExportPdf) {
                        IconButton(
                            onClick = onClassPdf,
                            modifier = Modifier.size(48.dp).testTag("sheet_pdf_btn")
                        ) {
                            Icon(Icons.Rounded.Description, contentDescription = "শ্রেণির PDF", tint = TealPrimary)
                        }
                    }
                }

                // Row 2: Mode Dropdown + Subject Dropdown + Enter Direction
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(Modifier.weight(1f)) {
                        val activeModeLabel = availableModes.find { it.first == mode }?.second ?: mode
                        OutlinedButton(
                            onClick = { modeMenuOpen = true },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SlateOutline),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.fillMaxWidth().testTag("sheet_mode_dropdown")
                        ) {
                            Text(
                                activeModeLabel,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(expanded = modeMenuOpen, onDismissRequest = { modeMenuOpen = false }) {
                            availableModes.forEach { (id, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            label,
                                            fontWeight = if (mode == id) FontWeight.Bold else FontWeight.Normal,
                                            color = if (mode == id) TealPrimary else SlateText
                                        )
                                    },
                                    onClick = { mode = id; modeMenuOpen = false }
                                )
                            }
                        }
                    }

                    if (mode == "subject" && activeSub != null) {
                        Box(Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { subjectMenuOpen = true },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, SlateOutline),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.fillMaxWidth().testTag("sheet_subject_dropdown")
                            ) {
                                Text(
                                    activeSub.bn + if (!permissions.canEditSubject(activeSub.key)) " (দেখা)" else "",
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(expanded = subjectMenuOpen, onDismissRequest = { subjectMenuOpen = false }) {
                                visibleSubs.forEach { s ->
                                    val editableSub = permissions.canEditSubject(s.key)
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "${s.bn} (${fmtSheetDigits(s.max.toString())})" + if (!editableSub) " · শুধু দেখা" else "",
                                                fontWeight = if (subKey == s.key) FontWeight.Bold else FontWeight.Normal,
                                                color = if (subKey == s.key) TealPrimary else if (editableSub) SlateText else SlateMuted
                                            )
                                        },
                                        onClick = { subKey = s.key; subjectMenuOpen = false }
                                    )
                                }
                            }
                        }
                    }
                }

                // Row 3: Consistent Digit Mode (বাংলা ০-৯ vs ইংরেজি 0-9) + Nav Direction / Batch Update
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = useBnDigits,
                            onClick = { useBnDigits = true },
                            modifier = Modifier.testTag("sheet_digit_bn_chip"),
                            label = { Text("সংখ্যা: ০-৯", fontSize = 11.sp, maxLines = 1, softWrap = false) }
                        )
                        FilterChip(
                            selected = !useBnDigits,
                            onClick = { useBnDigits = false },
                            modifier = Modifier.testTag("sheet_digit_en_chip"),
                            label = { Text("0-9", fontSize = 11.sp, maxLines = 1, softWrap = false) }
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    if (mode == "info" && permissions.canManageStudents) {
                        OutlinedButton(
                            onClick = { onBatchEdit(activeCls) },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, TealContainer),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("sheet_batch_update_btn")
                        ) {
                            Icon(
                                Icons.Rounded.EditNote,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "ব্যাচ আপডেট",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TealPrimary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    } else if (mode == "traits" && permissions.canEditAttendance) {
                        FilterChip(
                            selected = traitBatchSelectMode,
                            onClick = {
                                traitBatchSelectMode = !traitBatchSelectMode
                                if (!traitBatchSelectMode) selectedTraitStudentIds = emptySet()
                            },
                            modifier = Modifier.testTag("sheet_trait_batch_btn"),
                            leadingIcon = {
                                Icon(
                                    if (traitBatchSelectMode) Icons.Rounded.Close else Icons.Rounded.DoneAll,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            label = {
                                Text(
                                    if (traitBatchSelectMode) "সিলেক্ট শেষ" else "ব্যাচ সিলেক্ট",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        )
                    } else if (mode != "traits") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (mode == "attendance" && permissions.canEditAttendance) {
                                FilterChip(
                                    selected = attBatchFillMode,
                                    onClick = {
                                        val next = !attBatchFillMode
                                        attBatchFillMode = next
                                        selectedAttStudentIds = if (next) {
                                            classStudents.map { it.id }.toSet()
                                        } else {
                                            emptySet()
                                        }
                                    },
                                    modifier = Modifier.testTag("sheet_att_batch_btn"),
                                    leadingIcon = {
                                        Icon(
                                            if (attBatchFillMode) Icons.Rounded.Close else Icons.Rounded.DoneAll,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            if (attBatchFillMode) "ফিল শেষ" else "ব্যাচ ফিল",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                )
                            }
                            FilterChip(
                                selected = navDir == "down",
                                onClick = { navDir = if (navDir == "down") "right" else "down" },
                                modifier = Modifier.testTag("sheet_nav_dir_chip"),
                                label = { Text(if (navDir == "down") "↓ নিচে" else "→ ডানে", fontSize = 11.sp, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                }

                if (mode == "subject" && activeSub != null && !permissions.canEditSubject(activeSub.key)) {
                    Surface(
                        color = AmberBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "\"${activeSub.bn}\" বিষয়ে আপনার শুধুমাত্র দেখার অনুমতি রয়েছে — নম্বর পরিবর্তন করা যাবে না।",
                            fontSize = 11.sp,
                            color = AmberNotice,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                } else if (canPasteCurrentSheet && !isEditMode) {
                    Surface(
                        color = SlateBg,
                        border = BorderStroke(1.dp, SlateOutline),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isEditMode = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "নিরাপদ ভিউ মোড চালু — এডিট করতে ডানের 'এডিট' বাটনে ট্যাপ করুন",
                                fontSize = 11.sp,
                                color = SlateMuted,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "এডিট চালু",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary
                            )
                        }
                    }
                } else if (isEditMode && mode == "traits" && !traitBatchSelectMode) {
                    Surface(
                        color = TealSoft,
                        border = BorderStroke(1.dp, TealContainer),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "গুণাবলি এন্ট্রি: ঘরে হালকা (০.৫ সে.) চেপে ধরে ক / খ / গ / ফাঁকা মেনু খুলুন অথবা 'ব্যাচ সিলেক্ট' দিয়ে একসাথে মান দিন।",
                            fontSize = 11.sp,
                            color = TealPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                } else if (isEditMode && canPasteCurrentSheet) {
                    Surface(
                        color = TealSoft,
                        border = BorderStroke(1.dp, TealContainer),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "এডিট মোড চালু — ঘরে ট্যাপ করে লিখুন (শেষে 'সম্পন্ন' চাপুন)",
                                fontSize = 11.sp,
                                color = TealPrimary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = { applyClipboardPaste() },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, TealContainer),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("sheet_paste_btn")
                            ) {
                                Icon(
                                    Icons.Rounded.ContentPaste,
                                    contentDescription = "ক্লিপবোর্ড পেস্ট",
                                    tint = TealPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("পেস্ট", fontSize = 11.sp, color = TealPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
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
                color = TealSoft.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, TealContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "নির্বাচিত: ${fmtSheetDigits(selectedTraitStudentIds.size.toString())} / ${fmtSheetDigits(classStudents.size.toString())} জন (নামে ট্যাপ করে বাছাই করুন)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = {
                                selectedTraitStudentIds = if (allSelected) {
                                    emptySet()
                                } else {
                                    classStudents.map { it.id }.toSet()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, TealContainer),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("sheet_trait_select_all_btn")
                        ) {
                            Text(
                                if (allSelected) "সব বাদ" else "সবাই",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        traitColOptions.forEach { (colKey, colLabel) ->
                            val picked = colKey in selectedTraitCols
                            FilterChip(
                                selected = picked,
                                onClick = {
                                    selectedTraitCols = if (picked && selectedTraitCols.size > 1) {
                                        selectedTraitCols - colKey
                                    } else {
                                        selectedTraitCols + colKey
                                    }
                                },
                                label = { Text(colLabel, fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("ক" to "ক", "খ" to "খ", "গ" to "গ", null to "ফাঁকা").forEach { (v, lbl) ->
                            val isSelectedVal = selectedTraitValue == v
                            val chipBg = when (v) {
                                "ক" -> TealSoft
                                "খ" -> AmberBg
                                "গ" -> RoseDanger.copy(alpha = 0.12f)
                                else -> SlateBg
                            }
                            val chipFg = when (v) {
                                "ক" -> EmeraldGrade
                                "খ" -> AmberNotice
                                "গ" -> RoseDanger
                                else -> SlateMuted
                            }
                            FilterChip(
                                selected = isSelectedVal,
                                onClick = { selectedTraitValue = v },
                                modifier = Modifier.testTag("sheet_trait_val_${lbl}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipBg,
                                    selectedLabelColor = chipFg
                                ),
                                label = {
                                    Text(
                                        lbl,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            )
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
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("sheet_trait_batch_apply_btn")
                        ) {
                            Text(
                                "প্রয়োগ করুন",
                                fontSize = 11.5.sp,
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
                color = TealSoft.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, TealContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "নির্বাচিত: ${fmtSheetDigits(selectedAttStudentIds.size.toString())} / ${fmtSheetDigits(classStudents.size.toString())} জন (নামে ট্যাপ করে বাছাই করুন)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = {
                                selectedAttStudentIds = if (allAttSelected) {
                                    emptySet()
                                } else {
                                    classStudents.map { it.id }.toSet()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, TealContainer),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("sheet_att_select_all_btn")
                        ) {
                            Text(
                                if (allAttSelected) "সব বাদ" else "সবাই",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            else -> "যেকোনো ১টি বা উভয় ঘরে সংখ্যা লিখে প্রয়োগ করুন"
                        }
                        Text(
                            hintText,
                            fontSize = 11.sp,
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
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("sheet_att_batch_apply_btn")
                        ) {
                            Text(
                                "প্রয়োগ করুন",
                                fontSize = 11.5.sp,
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
                        "${CLSN[activeCls - 1]} শ্রেণিতে এখনো কোনো শিক্ষার্থী নেই",
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
                            cols.forEach { col ->
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
                                    SheetComputedHeader("ধারাবাহিক")
                                    SheetComputedHeader("মোট")
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
                                            .background(if (isRowSelected) TealSoft else Color.Transparent)
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
                                            color = TealPrimary
                                        )
                                        Text(
                                            s.name.ifBlank { "(নাম নেই)" },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Box(Modifier.width(1.dp).fillMaxHeight().background(SlateOutline))

                                    Row(Modifier.weight(1f).fillMaxHeight().horizontalScroll(hScroll)) {
                                        cols.forEachIndexed { cIdx, col ->
                                            val hasPerm = isColumnEditable(col)
                                            val colEditable = isEditMode && hasPerm

                                            if (col.isTraitCycle && col.xKey != null) {
                                                val currentVal = s.x[col.xKey] ?: ""
                                                val badgeBg = when {
                                                    isTraitRowSelected -> TealSoft.copy(alpha = 0.55f)
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
                                                        s.m[mk(col.markSubKey, term, col.markCompKey)]?.let { fmtSheetNum(it) } ?: ""
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
                                                    if (clean.isEmpty()) {
                                                        if (s.m.containsKey(k)) {
                                                            onPatchStudentField(s.id, "m.$k", null)
                                                        }
                                                    } else {
                                                        clean.toDoubleOrNull()?.let { d ->
                                                            if (d in 0.0..col.max.toDouble() && s.m[k] != d) {
                                                                onPatchStudentField(s.id, "m.$k", d)
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
                                                            !colEditable -> SlateBg
                                                            invalid -> RoseDanger.copy(alpha = 0.08f)
                                                            isFocused -> TealContainer.copy(alpha = 0.25f)
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

                                        if (mode == "subject" && activeSub != null) {
                                            val subForStudent = s.subs().find { it.key == activeSub.key } ?: activeSub
                                            val cScore = s.cont(subForStudent, term)
                                            val tScore = s.total(subForStudent, term)
                                            val grade = gradeOf((tScore / subForStudent.max.toDouble()) * 100.0)
                                            SheetComputedCell(fmtSheetNum(cScore), SlateBg, SlateMuted)
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
                                            SheetComputedCell(pctStr, TealContainer.copy(alpha = 0.25f), TealPrimary, bold = true)
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
}

@Composable
private fun SheetComputedHeader(label: String) {
    Box(Modifier.width(68.dp).height(46.dp), contentAlignment = Alignment.Center) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
    Box(Modifier.width(68.dp).fillMaxHeight().background(bg), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 12.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = fg)
    }
    Box(Modifier.width(1.dp).fillMaxHeight().background(SlateOutline))
}
