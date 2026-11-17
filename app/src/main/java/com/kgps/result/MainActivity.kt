@file:OptIn(ExperimentalMaterial3Api::class)

package com.kgps.result

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

private val TealPrimary = Color(0xFF0F766E)
private val TealContainer = Color(0xFFCCFBF1)
private val TealOnContainer = Color(0xFF115E59)
private val SlateBg = Color(0xFFF8FAFC)
private val SlateSurfaceVariant = Color(0xFFF1F5F9)
private val SlateOutline = Color(0xFFE2E8F0)
private val SlateText = Color(0xFF0F172A)
private val SlateMuted = Color(0xFF64748B)

private val ResultMakerColors = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = TealOnContainer,
    secondary = Color(0xFF334155),
    onSecondary = Color.White,
    background = SlateBg,
    onBackground = SlateText,
    surface = Color.White,
    onSurface = SlateText,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = SlateMuted,
    outline = SlateOutline
)

class AppState(val ctx: Context) {
    var students by mutableStateOf(listOf<Student>())
    var settings by mutableStateOf(DEFAULT_SETTINGS)

    init {
        val (a, b) = Repo.load(ctx)
        students = a
        settings = b
    }

    fun persist() = Repo.save(ctx, students, settings)
    fun upsert(s: Student) {
        students = if (students.any { it.id == s.id }) students.map { if (it.id == s.id) s else it } else students + s
        persist()
    }
    fun delete(id: String) { students = students.filter { it.id != id }; persist() }
    fun updateSettings(st: Settings) { settings = st; persist() }
    fun replaceAll(list: List<Student>, st: Settings) { students = list; settings = st; persist() }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = AppState(applicationContext)
        setContent {
            MaterialTheme(colorScheme = ResultMakerColors) {
                Surface(Modifier.fillMaxSize(), color = SlateBg) { App(state) }
            }
        }
    }
}

private fun toast(ctx: Context, m: String) = Toast.makeText(ctx, m, Toast.LENGTH_SHORT).show()
private fun readText(ctx: Context, uri: Uri) =
    ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
private fun writeText(ctx: Context, uri: Uri, t: String) {
    ctx.contentResolver.openOutputStream(uri)?.use { it.write(t.toByteArray(Charsets.UTF_8)) }
}

@Composable
fun App(st: AppState) {
    val ctx = LocalContext.current
    var screen by remember { mutableStateOf("list") }
    var editReturnScreen by remember { mutableStateOf("list") }
    var editing by remember { mutableStateOf<Student?>(null) }
    var studioStudentId by remember { mutableStateOf<String?>(null) }
    var cls by remember { mutableStateOf(1) }
    var pending by remember { mutableStateOf<List<Student>>(emptyList()) }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.openOutputStream(uri)?.use { PdfGen.write(ctx, pending, st.settings, it) }
                toast(ctx, "PDF সংরক্ষিত হয়েছে")
            } catch (e: Exception) { toast(ctx, "PDF তৈরি করা যায়নি: ${e.message}") }
        }
    }
    val makePdf: (List<Student>, String) -> Unit = { list, name ->
        if (list.isEmpty()) toast(ctx, "কোনো শিক্ষার্থী নেই") else { pending = list; pdfLauncher.launch(name) }
    }

    val openNew: () -> Unit = {
        val next = (st.students.filter { it.cls == cls }.maxOfOrNull { it.roll } ?: 0) + 1
        editing = Student(UUID.randomUUID().toString(), cls, next, "")
        editReturnScreen = if (screen == "sheet") "sheet" else "list"
        screen = "edit"
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        if (screen != "edit") {
            AppHeaderBar(
                st = st,
                currentScreen = screen,
                onNavigate = { screen = it },
                onNewStudent = openNew,
                onAllPdf = {
                    makePdf(st.students.sortedWith(compareBy({ it.cls }, { it.roll })), "সব-শ্রেণির-প্রতিবেদন.pdf")
                }
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (screen) {
                "list" -> ListScreen(
                    st = st,
                    cls = cls,
                    onCls = { cls = it },
                    onOpen = { editing = it; editReturnScreen = "list"; screen = "edit" },
                    onPreview = { studioStudentId = it.id; screen = "studio" },
                    onNew = openNew,
                    onClassPdf = {
                        makePdf(st.students.filter { it.cls == cls }.sortedBy { it.roll }, "শ্রেণি-${cls}-প্রতিবেদন.pdf")
                    }
                )
                "sheet" -> BulkSheetScreen(
                    st = st,
                    cls = cls,
                    onCls = { cls = it },
                    onOpenStudent = { editing = it; editReturnScreen = "sheet"; screen = "edit" },
                    onClassPdf = {
                        makePdf(st.students.filter { it.cls == cls }.sortedBy { it.roll }, "শ্রেণি-${cls}-প্রতিবেদন.pdf")
                    }
                )
                "studio" -> StudioScreen(
                    st = st,
                    selectedId = studioStudentId,
                    onSelectId = { studioStudentId = it },
                    onBack = { screen = "list" },
                    onExportPdf = { s -> makePdf(listOf(s), "${s.name.ifBlank { "student" }}-report.pdf") }
                )
                "edit" -> editing?.let { e ->
                    EditScreen(
                        st = st,
                        init = e,
                        onBack = { screen = editReturnScreen },
                        onPreview = { saved ->
                            if (saved.name.isNotBlank()) {
                                st.upsert(saved)
                                studioStudentId = saved.id
                                screen = "studio"
                            } else {
                                toast(ctx, "প্রথমে শিক্ষার্থীর নাম লিখুন")
                            }
                        },
                        onPdf = { makePdf(listOf(it), "${it.name.ifBlank { "student" }}.pdf") }
                    )
                }
                else -> SettingsScreen(st = st, onBack = { screen = "list" }, makePdf = makePdf)
            }
        }
    }
}

@Composable
fun AppHeaderBar(
    st: AppState,
    currentScreen: String,
    onNavigate: (String) -> Unit,
    onNewStudent: () -> Unit,
    onAllPdf: () -> Unit
) {
    val ctx = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }

    Surface(
        color = Color.White,
        border = BorderStroke(1.dp, SlateOutline)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Result Maker",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = SlateText,
                    modifier = Modifier.weight(1f).clickable { onNavigate("list") }
                )

                Box {
                    OutlinedButton(
                        onClick = { menuOpen = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SlateOutline)
                    ) {
                        Text("মেনু ও সেটিংস ▾", fontSize = 12.sp, color = SlateText)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        Text(
                            "মেনু নেভিগেশন",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateMuted,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                        listOf(
                            "list" to "শিক্ষার্থী তালিকা",
                            "sheet" to "দ্রুত শিট এন্ট্রি",
                            "studio" to "রিপোর্ট স্টুডিও (PDF প্রিভিউ)",
                            "settings" to "সেটিংস, শিক্ষক ও ব্যাকআপ"
                        ).forEach { (key, label) ->
                            val active = currentScreen == key
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (active) "✓ $label" else "   $label",
                                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (active) TealPrimary else SlateText
                                    )
                                },
                                onClick = { menuOpen = false; onNavigate(key) }
                            )
                        }
                        HorizontalDivider()
                        Text(
                            "অফিসিয়াল বাংলা ফন্ট (PDF)",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateMuted,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        PDF_FONT_LIST.forEach { font ->
                            val selected = st.settings.pdfFont == font.id
                            val sampleFamily = remember(font.id) {
                                FontFamily(androidx.compose.ui.text.font.Typeface(PdfGen.resolveTypeface(ctx, font.id, false)))
                            }
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            if (selected) "✓ ${font.labelBn}" else "   ${font.labelBn}",
                                            fontSize = 13.sp,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (selected) TealPrimary else SlateText
                                        )
                                        Text(
                                            "   শিখন অগ্রগতি প্রতিবেদন — ২০২৬",
                                            fontSize = 12.sp,
                                            fontFamily = sampleFamily,
                                            color = SlateMuted
                                        )
                                    }
                                },
                                onClick = {
                                    st.updateSettings(st.settings.copy(pdfFont = font.id))
                                    toast(ctx, "ফন্ট: ${font.labelBn}")
                                    menuOpen = false
                                }
                            )
                        }
                        HorizontalDivider()
                        Text(
                            "কভার লোগো (প্রাথমিক শিক্ষা অধিদপ্তর)",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateMuted,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        DPE_LOGO_LIST.forEach { logo ->
                            val selected = st.settings.dpeLogo == logo.id
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (selected) "✓ ${logo.labelBn}" else "   ${logo.labelBn}",
                                        fontSize = 13.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (selected) TealPrimary else SlateText
                                    )
                                },
                                onClick = {
                                    st.updateSettings(st.settings.copy(dpeLogo = logo.id))
                                    toast(ctx, "লোগো: ${logo.labelBn}")
                                    menuOpen = false
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("সব শ্রেণির সব শিক্ষার্থীর PDF") },
                            onClick = { menuOpen = false; onAllPdf() }
                        )
                    }
                }

                Button(
                    onClick = onNewStudent,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ নতুন", fontSize = 12.sp)
                }
            }

            // Secondary compact nav tabs matching Web Preview
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                listOf(
                    "list" to "শিক্ষার্থী তালিকা",
                    "sheet" to "শিট এন্ট্রি",
                    "studio" to "রিপোর্ট স্টুডিও",
                    "settings" to "সেটিংস"
                ).forEach { (key, label) ->
                    val active = currentScreen == key
                    Column(
                        Modifier.clickable { onNavigate(key) }.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            label,
                            fontSize = 13.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (active) TealPrimary else SlateMuted
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier.height(2.dp).width(56.dp)
                                .background(if (active) TealPrimary else Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ListScreen(
    st: AppState,
    cls: Int,
    onCls: (Int) -> Unit,
    onOpen: (Student) -> Unit,
    onPreview: (Student) -> Unit,
    onNew: () -> Unit,
    onClassPdf: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val allInClass = remember(st.students, cls) {
        st.students.filter { it.cls == cls }.sortedBy { it.roll }
    }
    val filtered = remember(allInClass, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) allInClass
        else allInClass.filter {
            it.name.lowercase().contains(q) ||
                it.roll.toString().contains(q) ||
                bn(it.roll.toString()).contains(q) ||
                it.guardian.lowercase().contains(q)
        }
    }
    val evaluated = allInClass.filter { s -> (1..3).any { s.hasTerm(it) } }
    val avgPct = if (evaluated.isNotEmpty()) evaluated.sumOf { it.overallPct() } / evaluated.size else 0.0
    val gradeACount = evaluated.count { gradeOf(it.overallPct()) == "A" }
    val teacherName = st.settings.teachers[cls].orEmpty().ifBlank { "অনির্ধারিত" }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        // Class selector chips with live student counts
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            (1..5).forEach { c ->
                val count = st.students.count { it.cls == c }
                FilterChip(
                    selected = cls == c,
                    onClick = { onCls(c) },
                    label = { Text("${CLS_SHORT[c - 1]} (${bn(count.toString())})") }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Summary Strip Card matching Web Preview
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1.2f)) {
                    Text("শ্রেণি ও শিক্ষক", fontSize = 11.sp, color = SlateMuted)
                    Text("${CLSN[cls - 1]} শ্রেণি", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(teacherName, fontSize = 11.sp, color = SlateMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(Modifier.weight(0.8f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("মোট", fontSize = 11.sp, color = SlateMuted)
                    Text("${bn(allInClass.size.toString())} জন", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("মূল্যায়িত ${bn(evaluated.size.toString())}", fontSize = 11.sp, color = SlateMuted)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("গড় · গ্রেড A", fontSize = 11.sp, color = SlateMuted)
                    Text(
                        if (evaluated.isNotEmpty()) "${fmt(avgPct)}%" else "—",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TealPrimary
                    )
                    Text("অতি উত্তম ${bn(gradeACount.toString())} জন", fontSize = 11.sp, color = SlateMuted)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("নাম বা রোল খুঁজুন...", fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        LazyColumn(
            Modifier.weight(1f).padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, SlateOutline)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                if (query.isNotBlank()) "অনুসন্ধানে কোনো শিক্ষার্থী পাওয়া যায়নি"
                                else "${CLSN[cls - 1]} শ্রেণিতে এখনো কোনো শিক্ষার্থী নেই",
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("নতুন শিক্ষার্থী যোগ করুন বা সেটিংস থেকে ইমপোর্ট করুন।", fontSize = 12.sp, color = SlateMuted)
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { s ->
                    val hasEval = (1..3).any { s.hasTerm(it) }
                    val pct = s.overallPct()
                    val grade = gradeOf(pct)
                    val gradeColor = when (grade) {
                        "A" -> Color(0xFF047857)
                        "B" -> TealPrimary
                        "C" -> Color(0xFFB45309)
                        else -> Color(0xFFBE123C)
                    }
                    Card(
                        Modifier.fillMaxWidth().clickable { onOpen(s) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, SlateOutline),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = SlateSurfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    bn(s.roll.toString().padStart(2, '0')),
                                    fontWeight = FontWeight.Bold,
                                    color = SlateText,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    s.name.ifBlank { "(নাম নেই)" },
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val subLine = listOfNotNull(
                                    s.guardian.takeIf { it.isNotBlank() }?.let { "অভিভাবক: $it" },
                                    s.religion.takeIf { it.isNotBlank() }
                                ).joinToString(" · ")
                                if (subLine.isNotBlank()) {
                                    Text(subLine, fontSize = 11.sp, color = SlateMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                if (hasEval) {
                                    Text(
                                        "গড় ${fmt(pct)}% · গ্রেড $grade (${gradeLabelBn(grade)})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = gradeColor
                                    )
                                } else {
                                    Text("নম্বর দেওয়া হয়নি", fontSize = 11.sp, color = SlateMuted)
                                }
                            }
                            OutlinedButton(
                                onClick = { onPreview(s) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, SlateOutline)
                            ) {
                                Text("প্রিভিউ", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onNew, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                Text("+ শিক্ষার্থী")
            }
            OutlinedButton(
                onClick = onClassPdf,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SlateOutline)
            ) {
                Text("শ্রেণির সব PDF")
            }
        }
    }
}

@Composable
fun StudioScreen(
    st: AppState,
    selectedId: String?,
    onSelectId: (String) -> Unit,
    onBack: () -> Unit,
    onExportPdf: (Student) -> Unit
) {
    val ctx = LocalContext.current
    BackHandler { onBack() }

    val sorted = remember(st.students) {
        st.students.sortedWith(compareBy({ it.cls }, { it.roll }))
    }
    val active = sorted.find { it.id == selectedId } ?: sorted.firstOrNull()
    var pageView by remember { mutableStateOf("both") }
    var showFoldGuide by remember { mutableStateOf(true) }
    var studentMenuOpen by remember { mutableStateOf(false) }

    if (active == null) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("প্রিভিউ দেখানোর মতো কোনো শিক্ষার্থী নেই", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onBack) { Text("তালিকায় ফিরে যান") }
        }
        return
    }

    val coverBmp = remember(active, st.settings, showFoldGuide) {
        PdfGen.renderCoverBitmap(ctx, active, st.settings, showFoldGuide = showFoldGuide, scale = 1.8f)
    }
    val marksBmp = remember(active, st.settings, showFoldGuide) {
        PdfGen.renderMarksBitmap(ctx, active, st.settings, showFoldGuide = showFoldGuide, scale = 1.8f)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Student & Font controls
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedButton(
                        onClick = { studentMenuOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SlateOutline)
                    ) {
                        Text(
                            "${CLSN[active.cls - 1]} · রোল ${bn(active.roll.toString())} — ${active.name.ifBlank { "(নাম নেই)" }} ▾",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    DropdownMenu(expanded = studentMenuOpen, onDismissRequest = { studentMenuOpen = false }) {
                        sorted.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${CLSN[s.cls - 1]} · রোল ${bn(s.roll.toString())} — ${s.name}") },
                                onClick = { onSelectId(s.id); studentMenuOpen = false }
                            )
                        }
                    }
                }

                Text("পাতা বিন্যাস (A4 Landscape)", fontSize = 12.sp, color = SlateMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    listOf("both" to "দুই পাতা", "cover" to "পাতা-১ (কভার)", "marks" to "পাতা-২ (নম্বর)").forEach { (id, label) ->
                        FilterChip(selected = pageView == id, onClick = { pageView = id }, label = { Text(label) })
                    }
                }

                Text("অফিসিয়াল বাংলা ফন্ট (PDF)", fontSize = 12.sp, color = SlateMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    PDF_FONT_LIST.forEach { font ->
                        FilterChip(
                            selected = st.settings.pdfFont == font.id,
                            onClick = { st.updateSettings(st.settings.copy(pdfFont = font.id)) },
                            label = { Text(font.labelBn.substringBefore(" (")) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = showFoldGuide, onCheckedChange = { showFoldGuide = it })
                        Text("মাঝখানের ভাঁজ-রেখা", fontSize = 12.sp)
                    }
                    Button(onClick = { onExportPdf(active) }, shape = RoundedCornerShape(8.dp)) {
                        Text("PDF ডাউনলোড")
                    }
                }
            }
        }

        if (pageView == "both" || pageView == "cover") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text("পাতা-১ (বাইরের পাশ): বাম = ব্যাক কভার · ডান = ফ্রন্ট কভার", fontSize = 11.sp, color = SlateMuted)
                    Spacer(Modifier.height(6.dp))
                    Image(
                        bitmap = coverBmp.asImageBitmap(),
                        contentDescription = "Cover Page Preview",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth().aspectRatio(PdfGen.W.toFloat() / PdfGen.H.toFloat())
                    )
                }
            }
        }

        if (pageView == "both" || pageView == "marks") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text("পাতা-২ (ভেতরের পাশ): ৩ প্রান্তিকের নম্বর, চূড়ান্ত গ্রেড, গুণাবলি ও উপস্থিতি", fontSize = 11.sp, color = SlateMuted)
                    Spacer(Modifier.height(6.dp))
                    Image(
                        bitmap = marksBmp.asImageBitmap(),
                        contentDescription = "Marks Page Preview",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth().aspectRatio(PdfGen.W.toFloat() / PdfGen.H.toFloat())
                    )
                }
            }
        }
    }
}

@Composable
fun Field(label: String, value: String, number: Boolean = false, lines: Int = 1, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        singleLine = lines == 1, minLines = lines,
        keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Number else KeyboardType.Text),
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    )
}

@Composable
fun MarkField(label: String, max: Double, value: Double?, k: String, onValue: (Double?) -> Unit) {
    var t by remember(k) { mutableStateOf(value?.let { fmtEn(it) } ?: "") }
    val v = fromBn(t).toDoubleOrNull()
    val err = t.isNotBlank() && (v == null || v > max || v < 0)
    OutlinedTextField(
        value = t,
        onValueChange = {
            t = it
            if (it.isBlank()) onValue(null)
            else fromBn(it).toDoubleOrNull()?.let { d -> if (d in 0.0..max) onValue(d) }
        },
        label = { Text(label) }, isError = err, singleLine = true,
        supportingText = if (err) ({ Text("সর্বোচ্চ ${fmt(max)}") }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    )
}

@Composable
fun EditScreen(
    st: AppState,
    init: Student,
    onBack: () -> Unit,
    onPreview: (Student) -> Unit,
    onPdf: (Student) -> Unit
) {
    var s by remember { mutableStateOf(init) }
    val cur by rememberUpdatedState(s)
    val deleted = remember { booleanArrayOf(false) }
    var tab by remember { mutableStateOf(0) }
    var term by remember { mutableStateOf(1) }
    var askDelete by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { if (!deleted[0] && cur.name.isNotBlank()) st.upsert(cur) } }
    BackHandler { onBack() }

    fun setX(k: String, v: String) { s = s.copy(x = s.x.toMutableMap().also { if (v.isBlank()) it.remove(k) else it[k] = v }) }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            listOf("তথ্য", "নম্বর", "উপস্থিতি/গুণাবলি").forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp)) {
            when (tab) {
                0 -> {
                    Field("শিক্ষার্থীর নাম", s.name) { s = s.copy(name = it) }
                    Field("রোল", if (s.roll == 0) "" else s.roll.toString(), number = true) { s = s.copy(roll = fromBn(it).toIntOrNull() ?: 0) }
                    Text("শ্রেণি", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { c -> FilterChip(selected = s.cls == c, onClick = { s = s.copy(cls = c) }, label = { Text(bn(c.toString())) }) }
                    }
                    Text("ধর্ম", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RELIGIONS.forEach { r -> FilterChip(selected = s.religion == r, onClick = { s = s.copy(religion = r) }, label = { Text(r) }) }
                    }
                    Field("অভিভাবকের নাম", s.guardian) { s = s.copy(guardian = it) }
                    Field("গ্রাম", s.village) { s = s.copy(village = it) }
                    Field("পোস্ট অফিস", s.post) { s = s.copy(post = it) }
                    Field("উপজেলা", s.upazila) { s = s.copy(upazila = it) }
                    Field("জেলা", s.district) { s = s.copy(district = it) }
                    Field("মোবাইল নম্বর", s.mobile, number = true) { s = s.copy(mobile = it) }
                }
                1 -> {
                    Text("সমন্বিত ফলাফল: ${fmt(s.overallPct())}%  —  গ্রেড ${gradeOf(s.overallPct())} (${gradeLabelBn(gradeOf(s.overallPct()))})", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                        (1..3).forEach { t -> FilterChip(selected = term == t, onClick = { term = t }, label = { Text("${bn(t.toString())} প্রান্তিক") }) }
                    }
                    s.subs().forEach { sub ->
                        Card(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, SlateOutline)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("${sub.bn} (${bn(sub.max.toString())})", fontWeight = FontWeight.Bold)
                                sub.comps.forEach { c ->
                                    val k = mk(sub.key, term, c.key)
                                    MarkField("${c.bn} (${bn(c.max.toString())})", c.max.toDouble(), s.m[k], "${s.id}$k") { v ->
                                        s = s.copy(m = s.m.toMutableMap().also { mm -> if (v == null) mm.remove(k) else mm[k] = v })
                                    }
                                }
                                if (sub.written > 0) {
                                    val k = mk(sub.key, term, "WR")
                                    MarkField("সামষ্টিক (লিখিত) (${bn(sub.written.toString())})", sub.written.toDouble(), s.m[k], "${s.id}$k") { v ->
                                        s = s.copy(m = s.m.toMutableMap().also { mm -> if (v == null) mm.remove(k) else mm[k] = v })
                                    }
                                }
                                Text("প্রান্তিক মোট: ${fmt(s.total(sub, term))} / ${bn(sub.max.toString())}", style = MaterialTheme.typography.labelLarge, color = TealPrimary)
                            }
                        }
                    }
                }
                else -> {
                    (1..3).forEach { t ->
                        Card(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, SlateOutline)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("${bn(t.toString())} প্রান্তিক", fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(Modifier.weight(1f)) { Field("মোট কার্যদিবস", s.x["DAYS_$t"] ?: "", true) { setX("DAYS_$t", it) } }
                                    Box(Modifier.weight(1f)) { Field("উপস্থিত দিন", s.x["PRES_$t"] ?: "", true) { setX("PRES_$t", it) } }
                                }
                                listOf("সক্রিয়তা" to "ACT", "শৃঙ্খলা" to "DISC", "আচরণ" to "COND").forEach { (lab, key) ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(lab, modifier = Modifier.weight(1f))
                                        listOf("ক", "খ", "গ").forEach { g ->
                                            val sel = s.x["${key}_$t"] == g
                                            FilterChip(selected = sel, onClick = { setX("${key}_$t", if (sel) "" else g) },
                                                label = { Text(g) }, modifier = Modifier.padding(start = 4.dp))
                                        }
                                    }
                                }
                                Field("শ্রেণি শিক্ষকের সার্বিক মতামত", s.x["REM_$t"] ?: "", lines = 3) { setX("REM_$t", it) }
                            }
                        }
                    }
                }
            }
        }
        Surface(color = Color.White, border = BorderStroke(1.dp, SlateOutline)) {
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = onBack) { Text("পিছনে", fontSize = 12.sp) }
                OutlinedButton(onClick = { onPreview(s) }) { Text("প্রিভিউ", fontSize = 12.sp) }
                Button(
                    onClick = { if (s.name.isNotBlank()) { st.upsert(s); onPdf(s) } },
                    modifier = Modifier.weight(1f)
                ) { Text("PDF তৈরি", fontSize = 12.sp) }
                TextButton(onClick = { askDelete = true }) { Text("মুছুন", fontSize = 12.sp, color = Color(0xFFBE123C)) }
            }
        }
    }
    if (askDelete) AlertDialog(
        onDismissRequest = { askDelete = false },
        title = { Text("শিক্ষার্থী মুছবেন?") }, text = { Text("এই শিক্ষার্থীর সব তথ্য মুছে যাবে।") },
        confirmButton = { TextButton(onClick = { deleted[0] = true; st.delete(s.id); askDelete = false; onBack() }) { Text("মুছুন") } },
        dismissButton = { TextButton(onClick = { askDelete = false }) { Text("না") } }
    )
}

@Composable
fun SettingsScreen(st: AppState, onBack: () -> Unit, makePdf: (List<Student>, String) -> Unit) {
    val ctx = LocalContext.current
    var payload by remember { mutableStateOf("") }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var askClearAll by remember { mutableStateOf(false) }
    BackHandler { onBack() }

    val csvSave = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { u ->
        if (u != null) { writeText(ctx, u, payload); toast(ctx, "CSV সংরক্ষিত হয়েছে") }
    }
    val jsonSave = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { u ->
        if (u != null) { writeText(ctx, u, payload); toast(ctx, "ব্যাকআপ সংরক্ষিত হয়েছে") }
    }
    val csvOpen = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) try {
            val before = st.students.size
            st.replaceAll(Repo.importCsv(readText(ctx, u), st.students), st.settings)
            toast(ctx, "ইমপোর্ট সম্পন্ন (নতুন ${bn((st.students.size - before).toString())} জন)")
        } catch (e: Exception) { toast(ctx, "ইমপোর্ট ব্যর্থ: ${e.message}") }
    }
    val jsonOpen = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> if (u != null) restoreUri = u }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("বিদ্যালয়, ফন্ট ও ব্যাকআপ সেটিংস", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Field("বিদ্যালয়ের নাম", st.settings.school) { st.updateSettings(st.settings.copy(school = it)) }
        Field("বিদ্যালয়ের ঠিকানা (ব্যাক কভারে প্রদর্শিত হবে)", st.settings.schoolAddress) { st.updateSettings(st.settings.copy(schoolAddress = it)) }
        Field("শিক্ষাবর্ষ", st.settings.year) { st.updateSettings(st.settings.copy(year = it)) }

        Text("প্রতিবেদনের বাংলা ফন্ট (প্রাতিষ্ঠানিক)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
        PDF_FONT_LIST.forEach { font ->
            val selected = st.settings.pdfFont == font.id
            val sampleFamily = remember(font.id) {
                FontFamily(androidx.compose.ui.text.font.Typeface(PdfGen.resolveTypeface(ctx, font.id, false)))
            }
            Card(
                modifier = Modifier.fillMaxWidth().clickable { st.updateSettings(st.settings.copy(pdfFont = font.id)) },
                colors = CardDefaults.cardColors(containerColor = if (selected) TealContainer.copy(alpha = 0.35f) else Color.White),
                border = BorderStroke(1.dp, if (selected) TealPrimary else SlateOutline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(font.labelBn, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SlateText)
                        if (selected) Text("✓ নির্বাচিত", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("শিখন অগ্রগতি প্রতিবেদন — ২০২৬", fontFamily = sampleFamily, fontSize = 15.sp, color = SlateText)
                    Text(font.descriptionBn, fontSize = 11.sp, color = SlateMuted)
                }
            }
        }

        Text("কভার লোগো (প্রাথমিক শিক্ষা অধিদপ্তর)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
        DPE_LOGO_LIST.forEach { logo ->
            val selected = st.settings.dpeLogo == logo.id
            val thumb = remember(logo.id) { PdfGen.resolveLogoBitmap(ctx, logo.id) }
            Card(
                modifier = Modifier.fillMaxWidth().clickable { st.updateSettings(st.settings.copy(dpeLogo = logo.id)) },
                colors = CardDefaults.cardColors(containerColor = if (selected) TealContainer.copy(alpha = 0.35f) else Color.White),
                border = BorderStroke(1.dp, if (selected) TealPrimary else SlateOutline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (thumb != null) {
                        Surface(
                            color = Color.White,
                            border = BorderStroke(1.dp, SlateOutline),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Image(
                                bitmap = thumb.asImageBitmap(),
                                contentDescription = logo.labelBn,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(width = 48.dp, height = 54.dp).padding(4.dp)
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(logo.labelBn, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SlateText)
                            if (selected) Text("✓ নির্বাচিত", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(logo.descriptionBn, fontSize = 11.sp, color = SlateMuted)
                    }
                }
            }
        }

        Text("শ্রেণি শিক্ষক", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        (1..5).forEach { c ->
            Text("${CLSN[c - 1]} শ্রেণি", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { Field("নাম", st.settings.teachers[c] ?: "") { v -> st.updateSettings(st.settings.copy(teachers = st.settings.teachers + (c to v))) } }
                Box(Modifier.weight(1f)) { Field("মোবাইল", st.settings.teacherMobiles[c] ?: "", true) { v -> st.updateSettings(st.settings.copy(teacherMobiles = st.settings.teacherMobiles + (c to v))) } }
            }
        }

        Text("PDF", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Button(onClick = { makePdf(st.students.sortedWith(compareBy({ it.cls }, { it.roll })), "সব-শ্রেণির-প্রতিবেদন.pdf") }, modifier = Modifier.fillMaxWidth()) { Text("সব শ্রেণির সব শিক্ষার্থীর PDF") }

        Text("CSV (Excel)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        OutlinedButton(onClick = { payload = Repo.toCsv(st.students); csvSave.launch("শিক্ষার্থী-তথ্য.csv") }, modifier = Modifier.fillMaxWidth()) { Text("CSV এক্সপোর্ট") }
        OutlinedButton(onClick = { payload = Repo.toCsv(emptyList()); csvSave.launch("খালি-টেমপ্লেট.csv") }, modifier = Modifier.fillMaxWidth()) { Text("খালি CSV টেমপ্লেট") }
        OutlinedButton(onClick = { csvOpen.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("CSV ইমপোর্ট (শ্রেণি+রোল মিললে হালনাগাদ)") }

        Text("ব্যাকআপ / রিস্টোর", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        OutlinedButton(onClick = { payload = Repo.toJson(st.students, st.settings); jsonSave.launch("ব্যাকআপ.json") }, modifier = Modifier.fillMaxWidth()) { Text("সম্পূর্ণ ব্যাকআপ এক্সপোর্ট") }
        OutlinedButton(onClick = { jsonOpen.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("ব্যাকআপ থেকে রিস্টোর") }
        OutlinedButton(onClick = { st.replaceAll(SAMPLE_STUDENTS, DEFAULT_SETTINGS); toast(ctx, "নমুনা তথ্য পুনরুদ্ধার হয়েছে") }, modifier = Modifier.fillMaxWidth()) { Text("নমুনা ডাটা রিসেট করুন") }
        OutlinedButton(onClick = { askClearAll = true }, modifier = Modifier.fillMaxWidth()) { Text("সব শিক্ষার্থীর তথ্য মুছুন", color = Color(0xFFBE123C)) }

        Spacer(Modifier.height(6.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("তালিকায় ফিরে যান") }
    }

    restoreUri?.let { u ->
        AlertDialog(
            onDismissRequest = { restoreUri = null },
            title = { Text("রিস্টোর করবেন?") }, text = { Text("বর্তমান সব তথ্য ব্যাকআপের তথ্য দিয়ে বদলে যাবে।") },
            confirmButton = {
                TextButton(onClick = {
                    try { val (l, s) = Repo.fromJson(readText(ctx, u)); st.replaceAll(l, s); toast(ctx, "রিস্টোর সম্পন্ন") }
                    catch (e: Exception) { toast(ctx, "ফাইল ঠিক নেই: ${e.message}") }
                    restoreUri = null
                }) { Text("হ্যাঁ, রিস্টোর") }
            },
            dismissButton = { TextButton(onClick = { restoreUri = null }) { Text("না") } }
        )
    }

    if (askClearAll) AlertDialog(
        onDismissRequest = { askClearAll = false },
        title = { Text("সব শিক্ষার্থী মুছবেন?") },
        text = { Text("সকল শ্রেণির সব শিক্ষার্থীর তালিকা খালি হয়ে যাবে।") },
        confirmButton = {
            TextButton(onClick = { st.replaceAll(emptyList(), st.settings); askClearAll = false; toast(ctx, "সব শিক্ষার্থীর তথ্য মুছে ফেলা হয়েছে") }) {
                Text("হ্যাঁ, মুছুন", color = Color(0xFFBE123C))
            }
        },
        dismissButton = { TextButton(onClick = { askClearAll = false }) { Text("না") } }
    )
}

private data class AndroidSheetCol(
    val id: String,
    val label: String,
    val subLabel: String,
    val max: Int,
    val kind: String, // "mark", "days", "trait"
    val subKey: String? = null,
    val compKey: String? = null,
    val xKey: String? = null
)

@Composable
fun BulkSheetScreen(
    st: AppState,
    cls: Int,
    onCls: (Int) -> Unit,
    onOpenStudent: (Student) -> Unit,
    onClassPdf: () -> Unit
) {
    val ctx = LocalContext.current
    var term by remember { mutableStateOf(1) }
    var mode by remember { mutableStateOf("subject") } // "subject", "all", "attendance"
    var allSubField by remember { mutableStateOf("PRIMARY") }
    var enterMovesDown by remember { mutableStateOf(true) }

    val classSubs = remember(cls) { subjectsFor(cls, "ইসলাম") }
    var selectedSubKey by remember(cls) { mutableStateOf(classSubs.firstOrNull()?.key ?: "BAN") }
    LaunchedEffect(classSubs) {
        if (classSubs.none { it.key == selectedSubKey }) {
            selectedSubKey = classSubs.firstOrNull()?.key ?: "BAN"
        }
    }
    val activeSub = remember(classSubs, selectedSubKey) {
        classSubs.find { it.key == selectedSubKey } ?: classSubs.firstOrNull()
    }

    val classStudents = remember(st.students, cls) {
        st.students.filter { it.cls == cls }.sortedWith(compareBy({ it.roll }, { it.name }))
    }

    val columns = remember(mode, activeSub, classSubs, allSubField, term) {
        when (mode) {
            "subject" -> {
                if (activeSub == null) emptyList()
                else buildList {
                    activeSub.comps.forEach { c ->
                        val shortLabel = when (c.key) {
                            "TB" -> "পাঠ্যবই"
                            "ACT" -> "সক্রিয়তা"
                            "LANG" -> "ভাষা"
                            "CT" -> "ক্লাস টেস্ট"
                            else -> "মোট"
                        }
                        add(
                            AndroidSheetCol(
                                id = "${activeSub.key}_${c.key}",
                                label = shortLabel,
                                subLabel = "(${bn(c.max.toString())})",
                                max = c.max,
                                kind = "mark",
                                subKey = activeSub.key,
                                compKey = c.key
                            )
                        )
                    }
                    if (activeSub.written > 0) {
                        add(
                            AndroidSheetCol(
                                id = "${activeSub.key}_WR",
                                label = "সামষ্টিক",
                                subLabel = "(${bn(activeSub.written.toString())})",
                                max = activeSub.written,
                                kind = "mark",
                                subKey = activeSub.key,
                                compKey = "WR"
                            )
                        )
                    }
                }
            }
            "all" -> {
                classSubs.map { sub ->
                    val targetCompKey: String
                    val targetMax: Int
                    val targetSubLabel: String
                    if (allSubField == "PRIMARY") {
                        when {
                            sub.written > 0 -> {
                                targetCompKey = "WR"
                                targetMax = sub.written
                                targetSubLabel = "লিখিত (${bn(targetMax.toString())})"
                            }
                            sub.key == "ART" -> {
                                targetCompKey = "TOTAL"
                                targetMax = sub.max
                                targetSubLabel = "মোট (${bn(targetMax.toString())})"
                            }
                            else -> {
                                targetCompKey = "TB"
                                targetMax = sub.comps.firstOrNull()?.max ?: 20
                                targetSubLabel = "পাঠ্য (${bn(targetMax.toString())})"
                            }
                        }
                    } else {
                        val comp = if (sub.key == "ART") sub.comps.first() else (sub.comps.find { it.key == allSubField } ?: sub.comps.first())
                        targetCompKey = comp.key
                        targetMax = comp.max
                        targetSubLabel = "${if (comp.key == "TOTAL") "মোট" else comp.key} (${bn(comp.max.toString())})"
                    }
                    val shortTitle = when (sub.key) {
                        "REL" -> "ধর্ম"
                        "SOC" -> "সমাজ/বিজ্ঞান"
                        "BGW" -> "বাওবি"
                        "PE" -> "শারীরিক"
                        else -> sub.bn
                    }
                    AndroidSheetCol(
                        id = "${sub.key}_$targetCompKey",
                        label = shortTitle,
                        subLabel = targetSubLabel,
                        max = targetMax,
                        kind = "mark",
                        subKey = sub.key,
                        compKey = targetCompKey
                    )
                }
            }
            else -> listOf(
                AndroidSheetCol("DAYS_$term", "কার্যদিবস", "দিন", 365, "days", xKey = "DAYS_$term"),
                AndroidSheetCol("PRES_$term", "উপস্থিতি", "দিন", 365, "days", xKey = "PRES_$term"),
                AndroidSheetCol("ACT_$term", "সক্রিয়তা", "ট্যাপ", 1, "trait", xKey = "ACT_$term"),
                AndroidSheetCol("DISC_$term", "শৃঙ্খলা", "ট্যাপ", 1, "trait", xKey = "DISC_$term"),
                AndroidSheetCol("COND_$term", "আচরণ", "ট্যাপ", 1, "trait", xKey = "COND_$term")
            )
        }
    }

    val focusRequesters = remember(cls, term, mode, selectedSubKey, allSubField, classStudents.size, columns.size) {
        mutableMapOf<Pair<Int, Int>, FocusRequester>()
    }

    fun moveFocusFrom(r: Int, c: Int) {
        var nr = if (enterMovesDown) r + 1 else r
        var nc = if (enterMovesDown) c else c + 1
        if (enterMovesDown && nr >= classStudents.size) {
            if (c + 1 < columns.size && columns[c + 1].kind != "trait") {
                nr = 0
                nc = c + 1
            } else return
        } else if (!enterMovesDown && nc >= columns.size) {
            if (r + 1 < classStudents.size) {
                nr = r + 1
                nc = 0
            } else return
        }
        focusRequesters[nr to nc]?.requestFocus()
    }

    // Quick add student inputs
    val suggestedRoll = remember(classStudents) { (classStudents.maxOfOrNull { it.roll } ?: 0) + 1 }
    var newRoll by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }

    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()
    val rowHeight = 46.dp
    val headerHeight = 48.dp
    val colWidth = 84.dp

    Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp)) {
        // Class & Term Controls Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("দ্রুত শিট এন্ট্রি", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SlateText)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { enterMovesDown = !enterMovesDown },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SlateOutline),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                if (enterMovesDown) "Enter: ↓ নিচে" else "Enter: → ডানে",
                                fontSize = 11.sp,
                                color = TealPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Button(
                            onClick = onClassPdf,
                            enabled = classStudents.isNotEmpty(),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("শ্রেণির PDF", fontSize = 11.sp)
                        }
                    }
                }

                // Class chips
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { c ->
                        val count = st.students.count { it.cls == c }
                        FilterChip(
                            selected = cls == c,
                            onClick = { onCls(c) },
                            label = { Text("${CLS_SHORT[c - 1]} (${bn(count.toString())})", fontSize = 12.sp) }
                        )
                    }
                }

                // Term + Mode chips
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..3).forEach { t ->
                        FilterChip(
                            selected = term == t,
                            onClick = { term = t },
                            label = { Text("${bn(t.toString())} প্রান্তিক", fontSize = 12.sp) }
                        )
                    }
                    VerticalDivider(Modifier.height(28.dp).align(Alignment.CenterVertically))
                    listOf(
                        "subject" to "বিষয়ভিত্তিক",
                        "all" to "সকল বিষয়",
                        "attendance" to "উপস্থিতি/গুণাবলি"
                    ).forEach { (mId, mLabel) ->
                        FilterChip(
                            selected = mode == mId,
                            onClick = { mode = mId },
                            label = { Text(mLabel, fontSize = 12.sp) }
                        )
                    }
                }

                // Sub-selector chips
                if (mode == "subject") {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        classSubs.forEach { sub ->
                            val shortName = when (sub.key) {
                                "REL" -> "ধর্মশিক্ষা"
                                "SOC" -> "সমাজ ও বিজ্ঞান"
                                "PE" -> "শারীরিক শিক্ষা"
                                else -> sub.bn
                            }
                            FilterChip(
                                selected = activeSub?.key == sub.key,
                                onClick = { selectedSubKey = sub.key },
                                label = { Text(shortName, fontSize = 11.sp) }
                            )
                        }
                    }
                } else if (mode == "all") {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "PRIMARY" to "সামষ্টিক/মূল",
                            "TB" to "পাঠ্যবই",
                            "CT" to "ক্লাস টেস্ট",
                            "ACT" to "সক্রিয়তা",
                            "LANG" to "ভাষাদক্ষতা"
                        ).forEach { (fId, fLabel) ->
                            FilterChip(
                                selected = allSubField == fId,
                                onClick = { allSubField = fId },
                                label = { Text(fLabel, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Frozen Left (Roll + Name) + Horizontally Scrollable Right Spreadsheet Grid
        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            color = Color.White,
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (classStudents.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("${CLSN[cls - 1]} শ্রেণিতে এখনো কোনো শিক্ষার্থী নেই", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("নিচের বক্স থেকে দ্রুত নতুন শিক্ষার্থী যোগ করুন।", fontSize = 12.sp, color = SlateMuted)
                }
            } else {
                Box(Modifier.fillMaxSize().verticalScroll(vScroll)) {
                    Row(Modifier.fillMaxWidth()) {
                        // Sticky Left Column: Roll + Name
                        Column(Modifier.width(146.dp).background(Color.White)) {
                            Row(
                                Modifier.fillMaxWidth().height(headerHeight).background(SlateSurfaceVariant).padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("রোল", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateText, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
                                Text("শিক্ষার্থীর নাম", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateText, modifier = Modifier.weight(1f).padding(start = 4.dp))
                            }
                            HorizontalDivider(color = SlateOutline)
                            classStudents.forEach { s ->
                                Row(
                                    Modifier.fillMaxWidth().height(rowHeight).clickable { onOpenStudent(s) }.padding(horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        bn(s.roll.toString()),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SlateMuted,
                                        modifier = Modifier.width(34.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        s.name.ifBlank { "(নাম নেই)" },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SlateText,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                                    )
                                }
                                HorizontalDivider(color = SlateOutline)
                            }
                        }

                        // Vertical divider separating frozen Roll+Name from scrollable marks
                        Box(Modifier.width(2.dp).height(headerHeight + (rowHeight + 1.dp) * classStudents.size).background(Color(0xFFCBD5E1)))

                        // Scrollable Right Columns
                        Column(Modifier.weight(1f).horizontalScroll(hScroll)) {
                            // Header Row
                            Row(Modifier.height(headerHeight).background(SlateSurfaceVariant), verticalAlignment = Alignment.CenterVertically) {
                                columns.forEach { col ->
                                    Column(
                                        Modifier.width(colWidth).fillMaxHeight().padding(horizontal = 4.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(col.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(col.subLabel, fontSize = 10.sp, color = SlateMuted, maxLines = 1)
                                    }
                                    Box(Modifier.width(1.dp).fillMaxHeight().background(SlateOutline))
                                }
                                if (mode == "subject" && activeSub != null) {
                                    Column(Modifier.width(68.dp).fillMaxHeight(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("ধারাবাহিক", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = SlateMuted)
                                        Text("(${bn((activeSub.max - activeSub.written).toString())})", fontSize = 10.sp, color = SlateMuted)
                                    }
                                    Box(Modifier.width(1.dp).fillMaxHeight().background(SlateOutline))
                                    Column(Modifier.width(68.dp).fillMaxHeight().background(TealContainer.copy(alpha = 0.45f)), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("মোট", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                                        Text("(${bn(activeSub.max.toString())})", fontSize = 10.sp, color = TealPrimary)
                                    }
                                } else if (mode == "attendance") {
                                    Column(Modifier.width(68.dp).fillMaxHeight().background(TealContainer.copy(alpha = 0.45f)), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("হার %", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                                    }
                                }
                            }
                            HorizontalDivider(color = SlateOutline)

                            // Data Rows
                            classStudents.forEachIndexed { rIdx, s ->
                                Row(Modifier.height(rowHeight), verticalAlignment = Alignment.CenterVertically) {
                                    columns.forEachIndexed { cIdx, col ->
                                        val fr = remember(rIdx, cIdx) { FocusRequester() }
                                        focusRequesters[rIdx to cIdx] = fr

                                        if (col.kind == "trait" && col.xKey != null) {
                                            val curTrait = s.x[col.xKey] ?: ""
                                            Box(
                                                Modifier
                                                    .width(colWidth)
                                                    .fillMaxHeight()
                                                    .clickable {
                                                        val nextTrait = when (curTrait) {
                                                            "" -> "ক"
                                                            "ক" -> "খ"
                                                            "খ" -> "গ"
                                                            else -> ""
                                                        }
                                                        val nextX = s.x.toMutableMap().apply {
                                                            if (nextTrait.isBlank()) remove(col.xKey) else put(col.xKey, nextTrait)
                                                        }
                                                        st.upsert(s.copy(x = nextX))
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    curTrait.ifBlank { "—" },
                                                    fontSize = 14.sp,
                                                    fontWeight = if (curTrait.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (curTrait.isNotBlank()) TealPrimary else SlateMuted
                                                )
                                            }
                                        } else {
                                            val storedStr = when (col.kind) {
                                                "mark" -> {
                                                    val k = mk(col.subKey!!, term, col.compKey!!)
                                                    s.m[k]?.let { fmtEn(it) } ?: ""
                                                }
                                                else -> s.x[col.xKey!!] ?: ""
                                            }
                                            var draft by remember(s.id, col.id, term, storedStr) { mutableStateOf(storedStr) }
                                            var isFocused by remember { mutableStateOf(false) }

                                            val parsedNum = fromBn(draft.trim()).toDoubleOrNull()
                                            val invalid = draft.isNotBlank() && (parsedNum == null || parsedNum < 0 || parsedNum > col.max)

                                            fun commitDraft(textVal: String) {
                                                val clean = fromBn(textVal.trim())
                                                if (col.kind == "mark") {
                                                    val k = mk(col.subKey!!, term, col.compKey!!)
                                                    val nextM = s.m.toMutableMap()
                                                    if (clean.isEmpty()) {
                                                        if (nextM.containsKey(k)) {
                                                            nextM.remove(k)
                                                            st.upsert(s.copy(m = nextM))
                                                        }
                                                    } else {
                                                        clean.toDoubleOrNull()?.let { d ->
                                                            val clamped = d.coerceIn(0.0, col.max.toDouble())
                                                            draft = fmtEn(clamped)
                                                            if (nextM[k] != clamped) {
                                                                nextM[k] = clamped
                                                                st.upsert(s.copy(m = nextM))
                                                            }
                                                        }
                                                    }
                                                } else if (col.xKey != null) {
                                                    val nextX = s.x.toMutableMap()
                                                    if (clean.isEmpty()) {
                                                        if (nextX.containsKey(col.xKey)) {
                                                            nextX.remove(col.xKey)
                                                            st.upsert(s.copy(x = nextX))
                                                        }
                                                    } else {
                                                        clean.toIntOrNull()?.let { iv ->
                                                            val clamped = iv.coerceIn(0, col.max)
                                                            draft = clamped.toString()
                                                            if (nextX[col.xKey] != draft) {
                                                                nextX[col.xKey] = draft
                                                                st.upsert(s.copy(x = nextX))
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            Box(
                                                Modifier
                                                    .width(colWidth)
                                                    .fillMaxHeight()
                                                    .background(
                                                        when {
                                                            invalid -> Color(0xFFFFF1F2)
                                                            isFocused -> TealContainer.copy(alpha = 0.45f)
                                                            else -> Color.Transparent
                                                        }
                                                    )
                                                    .padding(horizontal = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                BasicTextField(
                                                    value = draft,
                                                    onValueChange = { next ->
                                                        draft = next
                                                        val clean = fromBn(next.trim())
                                                        if (col.kind == "mark") {
                                                            val k = mk(col.subKey!!, term, col.compKey!!)
                                                            if (clean.isEmpty()) {
                                                                val nextM = s.m.toMutableMap().apply { remove(k) }
                                                                st.upsert(s.copy(m = nextM))
                                                            } else {
                                                                clean.toDoubleOrNull()?.let { d ->
                                                                    if (d in 0.0..col.max.toDouble()) {
                                                                        val nextM = s.m.toMutableMap().apply { put(k, d) }
                                                                        st.upsert(s.copy(m = nextM))
                                                                    }
                                                                }
                                                            }
                                                        } else if (col.xKey != null) {
                                                            if (clean.isEmpty()) {
                                                                val nextX = s.x.toMutableMap().apply { remove(col.xKey) }
                                                                st.upsert(s.copy(x = nextX))
                                                            } else {
                                                                clean.toIntOrNull()?.let { iv ->
                                                                    if (iv in 0..col.max) {
                                                                        val nextX = s.x.toMutableMap().apply { put(col.xKey, iv.toString()) }
                                                                        st.upsert(s.copy(x = nextX))
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    },
                                                    singleLine = true,
                                                    textStyle = TextStyle(
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = if (invalid) Color(0xFFBE123C) else SlateText,
                                                        textAlign = TextAlign.Center
                                                    ),
                                                    cursorBrush = SolidColor(TealPrimary),
                                                    keyboardOptions = KeyboardOptions(
                                                        keyboardType = KeyboardType.Decimal,
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
                                                        .focusRequester(fr)
                                                        .onFocusChanged { fs ->
                                                            if (isFocused && !fs.isFocused) commitDraft(draft)
                                                            isFocused = fs.isFocused
                                                        }
                                                )
                                                if (draft.isEmpty() && !isFocused) {
                                                    Text("—", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                                                }
                                            }
                                        }
                                        Box(Modifier.width(1.dp).fillMaxHeight().background(SlateOutline))
                                    }

                                    if (mode == "subject" && activeSub != null) {
                                        val subForStudent = s.subs().find { it.key == activeSub.key } ?: activeSub
                                        val cScore = s.cont(subForStudent, term)
                                        val tScore = s.total(subForStudent, term)
                                        Box(Modifier.width(68.dp).fillMaxHeight().background(SlateBg), contentAlignment = Alignment.Center) {
                                            Text(fmt(cScore), fontSize = 12.sp, color = SlateMuted)
                                        }
                                        Box(Modifier.width(1.dp).fillMaxHeight().background(SlateOutline))
                                        Box(Modifier.width(68.dp).fillMaxHeight().background(TealContainer.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                                            Text(fmt(tScore), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                                        }
                                    } else if (mode == "attendance") {
                                        val dVal = s.x["DAYS_$term"]?.let { fromBn(it).toDoubleOrNull() }
                                        val pVal = s.x["PRES_$term"]?.let { fromBn(it).toDoubleOrNull() }
                                        val pctStr = if (dVal != null && pVal != null && dVal > 0) "${fmt((pVal / dVal) * 100)}%" else "—"
                                        Box(Modifier.width(68.dp).fillMaxHeight().background(TealContainer.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                                            Text(pctStr, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
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

        Spacer(Modifier.height(6.dp))

        // Quick Add Student Footer Bar
        Surface(
            color = Color.White,
            border = BorderStroke(1.dp, SlateOutline),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = newRoll,
                    onValueChange = { newRoll = it },
                    placeholder = { Text(bn(suggestedRoll.toString()), fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(62.dp)
                )
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("দ্রুত নতুন শিক্ষার্থীর নাম...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val trimmed = newName.trim()
                        if (trimmed.isNotEmpty()) {
                            val rNum = fromBn(newRoll.trim()).toIntOrNull() ?: suggestedRoll
                            st.upsert(Student(UUID.randomUUID().toString(), cls, rNum, trimmed))
                            newName = ""
                            newRoll = ""
                            toast(ctx, "$trimmed (রোল ${bn(rNum.toString())}) যোগ হয়েছে")
                        }
                    },
                    enabled = newName.isNotBlank(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text("+ যোগ", fontSize = 12.sp)
                }
            }
        }
    }
}

