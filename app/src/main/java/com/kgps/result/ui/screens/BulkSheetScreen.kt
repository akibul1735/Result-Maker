package com.kgps.result.ui.screens

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
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
import com.kgps.result.cont
import com.kgps.result.createEmptyStudent
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
    "attendance" to "উপস্থিতি ও গুণাবলি",
    "info" to "তথ্য ও কাস্টম"
)

@Composable
fun BulkSheetScreen(
    students: List<Student>,
    settings: Settings,
    cls: Int,
    onSelectClass: (Int) -> Unit,
    onUpsertStudent: (Student) -> Unit,
    onOpenStudent: (Student) -> Unit,
    onClassPdf: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var term by remember { mutableStateOf(1) }
    var mode by remember { mutableStateOf("subject") }
    var navDir by remember { mutableStateOf("down") }

    var classMenuOpen by remember { mutableStateOf(false) }
    var termMenuOpen by remember { mutableStateOf(false) }
    var modeMenuOpen by remember { mutableStateOf(false) }
    var subjectMenuOpen by remember { mutableStateOf(false) }
    var showQuickAddDialog by remember { mutableStateOf(false) }

    val subs = remember(cls) { subjectsFor(cls, RELIGIONS.first()) }
    var subKey by remember(cls) { mutableStateOf(subs.first().key) }
    val activeSub = subs.find { it.key == subKey } ?: subs.firstOrNull()

    val classStudents = remember(students, cls) {
        students.filter { it.cls == cls }.sortedBy { it.roll }
    }
    val suggestedRoll = (classStudents.maxOfOrNull { it.roll } ?: 0) + 1

    val cols: List<SheetColDef> = remember(mode, activeSub, term, subs, settings.customFields) {
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
                            subLabel = "সর্বোচ্চ ${bn(c.max.toString())}",
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
                            subLabel = "সর্বোচ্চ ${bn(activeSub.written.toString())}",
                            max = activeSub.written,
                            width = 76.dp,
                            isMark = true,
                            markSubKey = activeSub.key,
                            markCompKey = "WR"
                        )
                    )
                }
            }
            "all" -> subs.map { sub ->
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
                    subLabel = "${if (isArt) "মোট" else "সামষ্টিক"} (${bn((if (sub.written > 0) sub.written else sub.max).toString())})",
                    max = if (sub.written > 0) sub.written else sub.max,
                    width = 86.dp,
                    isMark = true,
                    markSubKey = sub.key,
                    markCompKey = if (isArt) "TOTAL" else if (sub.written > 0) "WR" else "TB"
                )
            }
            "attendance" -> listOf(
                SheetColDef("DAYS_$term", "কার্যদিবস", "সর্বোচ্চ ৩৬৫", max = 365, width = 82.dp, xKey = "DAYS_$term"),
                SheetColDef("PRES_$term", "উপস্থিতি", "সর্বোচ্চ ৩৬৫", max = 365, width = 82.dp, xKey = "PRES_$term"),
                SheetColDef("ACT_$term", "সক্রিয়তা", "ট্যাপ: ক/খ/গ", width = 76.dp, xKey = "ACT_$term", isNumeric = false, isTraitCycle = true),
                SheetColDef("DISC_$term", "শৃঙ্খলা", "ট্যাপ: ক/খ/গ", width = 76.dp, xKey = "DISC_$term", isNumeric = false, isTraitCycle = true),
                SheetColDef("COND_$term", "আচরণ", "ট্যাপ: ক/খ/গ", width = 76.dp, xKey = "COND_$term", isNumeric = false, isTraitCycle = true)
            )
            else -> buildList {
                add(SheetColDef("info_guardian", "অভিভাবকের নাম", "তথ্য", width = 132.dp, infoFieldKey = "guardian", isNumeric = false))
                add(SheetColDef("info_mobile", "মোবাইল", "নম্বর", width = 108.dp, infoFieldKey = "mobile", isNumeric = true))
                add(SheetColDef("info_village", "গ্রাম", "ঠিকানা", width = 104.dp, infoFieldKey = "village", isNumeric = false))
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
                        "religion" -> nextStudent.copy(religion = trimmed)
                        else -> nextStudent
                    }
                    updatedCells++
                }
            }
            onUpsertStudent(nextStudent.copy(m = nextM, x = nextX))
        }
        onShowMessage("ক্লিপবোর্ড থেকে ${bn(updatedCells.toString())}টি ঘর আপডেট হয়েছে")
    }

    val hScroll = rememberScrollState()

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
                            Text("${CLSN[cls - 1]} শ্রেণি", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(expanded = classMenuOpen, onDismissRequest = { classMenuOpen = false }) {
                            (1..5).forEach { c ->
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

                    IconButton(
                        onClick = { showQuickAddDialog = true },
                        modifier = Modifier.size(48.dp).testTag("sheet_open_quick_add_btn")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "নতুন শিক্ষার্থী যোগ", tint = TealPrimary)
                    }
                    IconButton(
                        onClick = { applyClipboardPaste() },
                        modifier = Modifier.size(48.dp).testTag("sheet_paste_btn")
                    ) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = "ক্লিপবোর্ড পেস্ট", tint = TealPrimary)
                    }
                    IconButton(
                        onClick = onClassPdf,
                        modifier = Modifier.size(48.dp).testTag("sheet_pdf_btn")
                    ) {
                        Icon(Icons.Rounded.Description, contentDescription = "শ্রেণির PDF", tint = TealPrimary)
                    }
                }

                // Row 2: Mode Dropdown + Subject Dropdown + Enter Direction
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(Modifier.weight(1f)) {
                        val activeModeLabel = SHEET_MODES.find { it.first == mode }?.second ?: mode
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
                            SHEET_MODES.forEach { (id, label) ->
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
                                    activeSub.bn,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(expanded = subjectMenuOpen, onDismissRequest = { subjectMenuOpen = false }) {
                                subs.forEach { s ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "${s.bn} (${bn(s.max.toString())})",
                                                fontWeight = if (subKey == s.key) FontWeight.Bold else FontWeight.Normal,
                                                color = if (subKey == s.key) TealPrimary else SlateText
                                            )
                                        },
                                        onClick = { subKey = s.key; subjectMenuOpen = false }
                                    )
                                }
                            }
                        }
                    }

                    FilterChip(
                        selected = navDir == "down",
                        onClick = { navDir = if (navDir == "down") "right" else "down" },
                        modifier = Modifier.testTag("sheet_nav_dir_chip"),
                        label = { Text(if (navDir == "down") "↓ নিচে" else "→ ডানে", fontSize = 11.5.sp) }
                    )
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
                        "${CLSN[cls - 1]} শ্রেণিতে এখনো কোনো শিক্ষার্থী নেই",
                        color = SlateMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { showQuickAddDialog = true },
                        modifier = Modifier.testTag("sheet_empty_add_btn")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" শিক্ষার্থী যোগ করুন")
                    }
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
                                Column(
                                    Modifier.width(col.width).height(46.dp).padding(horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(col.label, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (col.subLabel.isNotBlank()) {
                                        Text(col.subLabel, fontSize = 10.sp, color = SlateMuted, maxLines = 1)
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
                                "all" -> SheetComputedHeader("গড় %")
                                "attendance" -> SheetComputedHeader("হার %")
                            }
                        }
                    }
                    HorizontalDivider(color = SlateOutline)

                    LazyColumn(Modifier.fillMaxSize()) {
                        itemsIndexed(classStudents, key = { _, s -> s.id }) { rIdx, s ->
                            val rowHeight = 48.dp
                            Column {
                                Row(Modifier.fillMaxWidth().height(rowHeight), verticalAlignment = Alignment.CenterVertically) {
                                    Row(
                                        Modifier
                                            .width(136.dp)
                                            .fillMaxHeight()
                                            .clickable { onOpenStudent(s) }
                                            .padding(horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            bn(s.roll.toString().padStart(2, '0')),
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
                                            if (col.isTraitCycle && col.xKey != null) {
                                                val currentVal = s.x[col.xKey] ?: ""
                                                val nextVal = when (currentVal) {
                                                    "" -> "ক"
                                                    "ক" -> "খ"
                                                    "খ" -> "গ"
                                                    else -> ""
                                                }
                                                val badgeBg = when (currentVal) {
                                                    "ক" -> TealSoft
                                                    "খ" -> AmberBg
                                                    "গ" -> RoseDanger.copy(alpha = 0.1f)
                                                    else -> MaterialTheme.colorScheme.surface
                                                }
                                                val badgeFg = when (currentVal) {
                                                    "ক" -> EmeraldGrade
                                                    "খ" -> AmberNotice
                                                    "গ" -> RoseDanger
                                                    else -> SlateMuted
                                                }
                                                Box(
                                                    Modifier
                                                        .width(col.width)
                                                        .fillMaxHeight()
                                                        .background(badgeBg)
                                                        .clickable {
                                                            val nextX = if (nextVal.isEmpty()) s.x - col.xKey else s.x + (col.xKey to nextVal)
                                                            onUpsertStudent(s.copy(x = nextX))
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        currentVal.ifEmpty { "—" },
                                                        fontSize = 13.sp,
                                                        fontWeight = if (currentVal.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                                                        color = badgeFg
                                                    )
                                                }
                                                Box(Modifier.width(1.dp).fillMaxHeight().background(SlateOutline))
                                                return@forEachIndexed
                                            }

                                            val initialVal = remember(s.id, s.m, s.x, s.guardian, s.mobile, s.village, s.religion, col.key, term) {
                                                when {
                                                    col.isMark && col.markSubKey != null && col.markCompKey != null -> {
                                                        s.m[mk(col.markSubKey, term, col.markCompKey)]?.let { fmtEn(it) } ?: ""
                                                    }
                                                    col.xKey != null -> s.x[col.xKey] ?: ""
                                                    col.infoFieldKey != null -> when (col.infoFieldKey) {
                                                        "guardian" -> s.guardian
                                                        "mobile" -> s.mobile
                                                        "village" -> s.village
                                                        "religion" -> s.religion
                                                        else -> ""
                                                    }
                                                    else -> ""
                                                }
                                            }
                                            var draft by remember(initialVal) { mutableStateOf(initialVal) }
                                            var isFocused by remember { mutableStateOf(false) }

                                            val parsedNum = if (col.isNumeric && col.infoFieldKey != "mobile") fromBn(draft.trim()).toDoubleOrNull() else null
                                            val invalid = col.isNumeric && col.infoFieldKey != "mobile" && draft.isNotBlank() &&
                                                (parsedNum == null || parsedNum < 0 || parsedNum > col.max)

                                            fun commitDraft(text: String) {
                                                val clean = fromBn(text.trim())
                                                if (col.isMark && col.markSubKey != null && col.markCompKey != null) {
                                                    val k = mk(col.markSubKey, term, col.markCompKey)
                                                    if (clean.isEmpty()) {
                                                        if (s.m.containsKey(k)) onUpsertStudent(s.copy(m = s.m - k))
                                                    } else {
                                                        clean.toDoubleOrNull()?.let { d ->
                                                            if (d in 0.0..col.max.toDouble() && s.m[k] != d) {
                                                                onUpsertStudent(s.copy(m = s.m + (k to d)))
                                                            }
                                                        }
                                                    }
                                                } else if (col.xKey != null) {
                                                    val rawTrim = text.trim()
                                                    if (rawTrim.isEmpty()) {
                                                        if (s.x.containsKey(col.xKey)) onUpsertStudent(s.copy(x = s.x - col.xKey))
                                                    } else if (col.isNumeric) {
                                                        clean.toIntOrNull()?.let { iv ->
                                                            if (iv in 0..col.max && s.x[col.xKey] != iv.toString()) {
                                                                onUpsertStudent(s.copy(x = s.x + (col.xKey to iv.toString())))
                                                            }
                                                        }
                                                    } else if (s.x[col.xKey] != rawTrim) {
                                                        onUpsertStudent(s.copy(x = s.x + (col.xKey to rawTrim)))
                                                    }
                                                } else if (col.infoFieldKey != null) {
                                                    val v = text.trim()
                                                    val next = when (col.infoFieldKey) {
                                                        "guardian" -> s.copy(guardian = v)
                                                        "mobile" -> s.copy(mobile = v)
                                                        "village" -> s.copy(village = v)
                                                        "religion" -> s.copy(religion = v.ifBlank { s.religion })
                                                        else -> s
                                                    }
                                                    if (next != s) onUpsertStudent(next)
                                                }
                                            }

                                            Box(
                                                Modifier
                                                    .width(col.width)
                                                    .fillMaxHeight()
                                                    .background(
                                                        when {
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
                                                    onValueChange = { v -> draft = v },
                                                    singleLine = true,
                                                    textStyle = TextStyle(
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = if (invalid) RoseDanger else SlateText,
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
                                            SheetComputedCell(fmt(cScore), SlateBg, SlateMuted)
                                            SheetComputedCell(fmt(tScore), TealContainer.copy(alpha = 0.25f), TealPrimary, bold = true)
                                            SheetComputedCell(grade, SlateBg, TealPrimary, bold = true)
                                        } else if (mode == "all") {
                                            val maxSum = subs.sumOf { it.max }.coerceAtLeast(1)
                                            val avgSum = subs.sumOf { s.avg(it) }
                                            val pct = avgSum / maxSum * 100.0
                                            SheetComputedCell("${fmt(pct)}%", TealContainer.copy(alpha = 0.25f), TealPrimary, bold = true)
                                        } else if (mode == "attendance") {
                                            val dVal = s.x["DAYS_$term"]?.let { fromBn(it).toDoubleOrNull() }
                                            val pVal = s.x["PRES_$term"]?.let { fromBn(it).toDoubleOrNull() }
                                            val pctStr = if (dVal != null && pVal != null && dVal > 0) "${fmt((pVal / dVal) * 100)}%" else "—"
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

    if (showQuickAddDialog) {
        var rollInput by remember { mutableStateOf(suggestedRoll.toString()) }
        var nameInput by remember { mutableStateOf("") }
        var religionInput by remember { mutableStateOf(RELIGIONS.first()) }

        AlertDialog(
            onDismissRequest = { showQuickAddDialog = false },
            title = { Text("${CLSN[cls - 1]} শ্রেণিতে দ্রুত শিক্ষার্থী যোগ", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rollInput,
                        onValueChange = { rollInput = it },
                        label = { Text("রোল নম্বর") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("sheet_dialog_roll_input")
                    )
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("শিক্ষার্থীর নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("sheet_dialog_name_input")
                    )
                    Text("ধর্ম নির্বাচন করুন", fontSize = 12.sp, color = SlateMuted)
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RELIGIONS.forEach { r ->
                            FilterChip(
                                selected = religionInput == r,
                                onClick = { religionInput = r },
                                label = { Text(r, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = nameInput.trim()
                        if (trimmed.isNotEmpty()) {
                            val rNum = fromBn(rollInput.trim()).toIntOrNull() ?: suggestedRoll
                            onUpsertStudent(createEmptyStudent(cls, rNum, religionInput).copy(name = trimmed))
                            onShowMessage("$trimmed (রোল ${bn(rNum.toString())}) যোগ হয়েছে")
                            showQuickAddDialog = false
                        }
                    },
                    enabled = nameInput.isNotBlank(),
                    modifier = Modifier.testTag("sheet_dialog_confirm_add")
                ) {
                    Text("যোগ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickAddDialog = false }) { Text("বাতিল") }
            }
        )
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
