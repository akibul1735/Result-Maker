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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.UUID

class AppState(val ctx: Context) {
    var students by mutableStateOf(listOf<Student>())
    var settings by mutableStateOf(Settings())

    init { val (a, b) = Repo.load(ctx); students = a; settings = b }

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
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { App(state) } } }
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
    var editing by remember { mutableStateOf<Student?>(null) }
    var cls by remember { mutableStateOf(1) }
    var pending by remember { mutableStateOf<List<Student>>(emptyList()) }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.openOutputStream(uri)?.use { PdfGen.write(pending, st.settings, it) }
                toast(ctx, "PDF সংরক্ষিত হয়েছে")
            } catch (e: Exception) { toast(ctx, "PDF তৈরি করা যায়নি: ${e.message}") }
        }
    }
    val makePdf: (List<Student>, String) -> Unit = { list, name ->
        if (list.isEmpty()) toast(ctx, "কোনো শিক্ষার্থী নেই") else { pending = list; pdfLauncher.launch(name) }
    }

    when (screen) {
        "list" -> ListScreen(st, cls, { cls = it },
            onOpen = { editing = it; screen = "edit" },
            onNew = {
                val next = (st.students.filter { it.cls == cls }.maxOfOrNull { it.roll } ?: 0) + 1
                editing = Student(UUID.randomUUID().toString(), cls, next, ""); screen = "edit"
            },
            onSettings = { screen = "settings" },
            onClassPdf = { makePdf(st.students.filter { it.cls == cls }.sortedBy { it.roll }, "শ্রেণি-${cls}-প্রতিবেদন.pdf") })
        "edit" -> editing?.let { e ->
            EditScreen(st, e, onBack = { screen = "list" }, onPdf = { makePdf(listOf(it), "${it.name.ifBlank { "student" }}.pdf") })
        }
        else -> SettingsScreen(st, onBack = { screen = "list" }, makePdf = makePdf)
    }
}

@Composable
fun ListScreen(
    st: AppState, cls: Int, onCls: (Int) -> Unit, onOpen: (Student) -> Unit,
    onNew: () -> Unit, onSettings: () -> Unit, onClassPdf: () -> Unit
) {
    val ctx = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("শিখন অগ্রগতি প্রতিবেদন", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = onSettings) { Text("সেটিংস") }
            Box {
                OutlinedButton(onClick = { menuOpen = true }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                    Text("মেনু ▾")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("সেটিংস (বিদ্যালয়, শিক্ষক ও ব্যাকআপ)") },
                        onClick = { menuOpen = false; onSettings() }
                    )
                    HorizontalDivider()
                    Text(
                        "অফিসিয়াল বাংলা ফন্ট (PDF)",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                    PDF_FONTS.forEach { (id, label) ->
                        val selected = st.settings.pdfFont == id
                        DropdownMenuItem(
                            text = { Text(if (selected) "✓ $label" else "   $label") },
                            onClick = {
                                st.updateSettings(st.settings.copy(pdfFont = id))
                                toast(ctx, "ফন্ট: $label")
                                menuOpen = false
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("${CLSN[cls - 1]} শ্রেণির সব PDF") },
                        onClick = { menuOpen = false; onClassPdf() }
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..5).forEach { c -> FilterChip(selected = cls == c, onClick = { onCls(c) }, label = { Text(listOf("১ম", "২য়", "৩য়", "৪র্থ", "৫ম")[c - 1]) }) }
        }
        Spacer(Modifier.height(8.dp))
        val list = st.students.filter { it.cls == cls }.sortedBy { it.roll }
        Text("${CLSN[cls - 1]} শ্রেণি — মোট ${bn(list.size.toString())} জন", style = MaterialTheme.typography.labelLarge)
        LazyColumn(Modifier.weight(1f).padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(list, key = { it.id }) { s ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(s) }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(bn(s.roll.toString()), fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                        Text(s.name.ifBlank { "(নাম নেই)" }, modifier = Modifier.weight(1f))
                        Text(s.religion, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onNew, modifier = Modifier.weight(1f)) { Text("+ শিক্ষার্থী") }
            OutlinedButton(onClick = onClassPdf, modifier = Modifier.weight(1f)) { Text("শ্রেণির সব PDF") }
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
fun EditScreen(st: AppState, init: Student, onBack: () -> Unit, onPdf: (Student) -> Unit) {
    var s by remember { mutableStateOf(init) }
    val cur by rememberUpdatedState(s)
    val deleted = remember { booleanArrayOf(false) }
    var tab by remember { mutableStateOf(0) }
    var term by remember { mutableStateOf(1) }
    var askDelete by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { if (!deleted[0] && cur.name.isNotBlank()) st.upsert(cur) } }
    BackHandler { onBack() }

    fun setX(k: String, v: String) { s = s.copy(x = s.x.toMutableMap().also { if (v.isBlank()) it.remove(k) else it[k] = v }) }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
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
                    Field("উপজেলা", s.upazila) { s = s.copy(upazila = it) }
                    Field("জেলা", s.district) { s = s.copy(district = it) }
                    Field("মোবাইল নম্বর", s.mobile, number = true) { s = s.copy(mobile = it) }
                }
                1 -> {
                    Text("সমন্বিত ফলাফল: ${fmt(s.overallPct())}%  —  গ্রেড ${gradeOf(s.overallPct())}", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                        (1..3).forEach { t -> FilterChip(selected = term == t, onClick = { term = t }, label = { Text("${bn(t.toString())} প্রান্তিক") }) }
                    }
                    s.subs().forEach { sub ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
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
                                Text("প্রান্তিক মোট: ${fmt(s.total(sub, term))} / ${bn(sub.max.toString())}", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
                else -> {
                    (1..3).forEach { t ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
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
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("সংরক্ষণ ও পিছনে") }
            Button(onClick = { if (s.name.isNotBlank()) { st.upsert(s); onPdf(s) } }, modifier = Modifier.weight(1f)) { Text("PDF তৈরি") }
            TextButton(onClick = { askDelete = true }) { Text("মুছুন") }
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

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Text("সেটিংস", style = MaterialTheme.typography.titleLarge)
        Field("বিদ্যালয়ের নাম", st.settings.school) { st.updateSettings(st.settings.copy(school = it)) }
        Field("শিক্ষাবর্ষ", st.settings.year) { st.updateSettings(st.settings.copy(year = it)) }
        Text("PDF ফন্ট (প্রাতিষ্ঠানিক)", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
            PDF_FONTS.forEach { (id, label) ->
                FilterChip(
                    selected = st.settings.pdfFont == id,
                    onClick = { st.updateSettings(st.settings.copy(pdfFont = id)) },
                    label = { Text(label) }
                )
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
        Text("PDF", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        Button(onClick = { makePdf(st.students.sortedWith(compareBy({ it.cls }, { it.roll })), "সব-শ্রেণির-প্রতিবেদন.pdf") }, modifier = Modifier.fillMaxWidth()) { Text("সব শ্রেণির সব শিক্ষার্থীর PDF") }
        Text("CSV (Excel)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        OutlinedButton(onClick = { payload = Repo.toCsv(st.students); csvSave.launch("শিক্ষার্থী-তথ্য.csv") }, modifier = Modifier.fillMaxWidth()) { Text("CSV এক্সপোর্ট") }
        OutlinedButton(onClick = { payload = Repo.toCsv(emptyList()); csvSave.launch("খালি-টেমপ্লেট.csv") }, modifier = Modifier.fillMaxWidth()) { Text("খালি CSV টেমপ্লেট") }
        OutlinedButton(onClick = { csvOpen.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("CSV ইমপোর্ট (শ্রেণি+রোল মিললে হালনাগাদ)") }
        Text("ব্যাকআপ / রিস্টোর", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        OutlinedButton(onClick = { payload = Repo.toJson(st.students, st.settings); jsonSave.launch("ব্যাকআপ.json") }, modifier = Modifier.fillMaxWidth()) { Text("সম্পূর্ণ ব্যাকআপ এক্সপোর্ট") }
        OutlinedButton(onClick = { jsonOpen.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("ব্যাকআপ থেকে রিস্টোর") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("পিছনে") }
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
}
