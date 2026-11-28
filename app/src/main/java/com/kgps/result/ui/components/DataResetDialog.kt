package com.kgps.result.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoDelete
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Grade
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kgps.result.DataResetCategory
import com.kgps.result.DataResetEngine
import com.kgps.result.DataResetRequest
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.activeClassIds
import com.kgps.result.bn
import com.kgps.result.classShortBn
import com.kgps.result.security.BiometricAvailability
import com.kgps.result.security.BiometricResetAuthenticator
import com.kgps.result.security.BiometricVerificationResult
import com.kgps.result.ui.theme.AmberBg
import com.kgps.result.ui.theme.AmberBorder
import com.kgps.result.ui.theme.AmberNotice
import com.kgps.result.ui.theme.EmeraldGrade
import com.kgps.result.ui.theme.RoseBg
import com.kgps.result.ui.theme.RoseDanger
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
fun DataResetDialog(
    students: List<Student>,
    settings: Settings,
    initialCategory: DataResetCategory? = null,
    onDismiss: () -> Unit,
    onConfirmReset: (DataResetRequest) -> Unit
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val startStep = when (initialCategory) {
        DataResetCategory.ALL_STUDENTS, DataResetCategory.SAMPLE_DATA -> 3
        null -> 1
        else -> 2
    }

    var currentStep by remember(initialCategory) { mutableIntStateOf(startStep) }
    var request by remember(initialCategory) {
        mutableStateOf(
            DataResetRequest(
                category = initialCategory ?: DataResetCategory.RESULTS,
                selectedClasses = setOf(1),
                selectedTerms = setOf(1, 2, 3),
                allSubjects = true,
                selectedSubjectKeys = setOf("BAN")
            )
        )
    }

    var biometricAvailability by remember {
        mutableStateOf(BiometricResetAuthenticator.checkFingerprintAvailability(ctx))
    }
    var hasDeviceLockFallback by remember {
        mutableStateOf(BiometricResetAuthenticator.canUseDeviceCredentialFallback(ctx))
    }
    var authStatusMsg by remember { mutableStateOf<String?>(null) }
    var authStatusIsError by remember { mutableStateOf(false) }
    var fallbackTypedConfirm by remember { mutableStateOf("") }

    DisposableEffect(lifecycleOwner, ctx) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                biometricAvailability = BiometricResetAuthenticator.checkFingerprintAvailability(ctx)
                hasDeviceLockFallback = BiometricResetAuthenticator.canUseDeviceCredentialFallback(ctx)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val activeClassList = remember(settings.customClasses) { activeClassIds(settings) }
    val countByClass = remember(students, activeClassList) {
        activeClassList.associateWith { c -> students.count { it.cls == c } }
    }
    val customFieldKeys = remember(settings.customFields) {
        settings.customFields.map { it.key }.toSet()
    }
    val subjectOptions = remember(request.selectedClasses) {
        DataResetEngine.subjectOptionsForClasses(request.selectedClasses)
    }
    val isStep2Valid = remember(request) {
        DataResetEngine.isRequestValid(request)
    }
    val impact = remember(students, request, customFieldKeys) {
        DataResetEngine.computeImpact(students, request, customFieldKeys)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .widthIn(max = 560.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Surface(color = RoseBg, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = RoseDanger.copy(alpha = 0.14f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.WarningAmber,
                                        contentDescription = "সতর্কতা",
                                        tint = RoseDanger,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ধাপে ধাপে ডাটা রিসেট",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = SlateText
                                )
                                Text(
                                    text = "সতর্কতা ও ফিঙ্গারপ্রিন্ট যাচাই দ্বারা সুরক্ষিত",
                                    fontSize = 11.5.sp,
                                    color = RoseDanger
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(48.dp).testTag("data_reset_close_btn")
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "বন্ধ করুন", tint = SlateMuted)
                            }
                        }

                        // Step Indicator Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StepPill(
                                stepNum = "১",
                                label = "ধরন",
                                active = currentStep == 1,
                                done = currentStep > 1,
                                modifier = Modifier.weight(1f)
                            )
                            StepPill(
                                stepNum = "২",
                                label = "শ্রেণি ও অংশ",
                                active = currentStep == 2,
                                done = currentStep > 2,
                                modifier = Modifier.weight(1f)
                            )
                            StepPill(
                                stepNum = "৩",
                                label = "সতর্কতা ও ফিঙ্গারপ্রিন্ট",
                                active = currentStep == 3,
                                done = false,
                                modifier = Modifier.weight(1.25f)
                            )
                        }
                    }
                }

                HorizontalDivider(color = SlateOutline)

                // Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (currentStep) {
                        1 -> {
                            Text(
                                text = "ধাপ ১: কোন ধরনের ডাটা রিসেট বা মুছে ফেলতে চান?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = SlateText
                            )
                            Text(
                                text = "প্রথমে রিসেটের ধরণ বেছে নিন। পরবর্তী ধাপে নির্দিষ্ট শ্রেণি ও বিষয় নির্বাচন করতে পারবেন।",
                                fontSize = 12.sp,
                                color = SlateMuted
                            )
                            DataResetCategory.entries.forEach { cat ->
                                val selected = request.category == cat
                                val icon = categoryIcon(cat)
                                val accent = if (cat.isHighRiskAll) RoseDanger else TealPrimary
                                val bg = when {
                                    selected && cat.isHighRiskAll -> RoseBg
                                    selected -> TealSoft
                                    else -> SlateBg
                                }
                                Surface(
                                    color = bg,
                                    border = BorderStroke(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) accent else SlateOutline
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reset_cat_${cat.name}")
                                        .clickable {
                                            authStatusMsg = null
                                            fallbackTypedConfirm = ""
                                            request = request.copy(category = cat)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        RadioButton(
                                            selected = selected,
                                            onClick = {
                                                authStatusMsg = null
                                                fallbackTypedConfirm = ""
                                                request = request.copy(category = cat)
                                            }
                                        )
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = accent,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = cat.titleBn,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = SlateText,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                            }
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = cat.subtitleBn,
                                                fontSize = 11.5.sp,
                                                color = SlateMuted,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            Text(
                                text = "ধাপ ২: ${request.category.titleBn}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = SlateText
                            )

                            // Class Picker
                            Surface(
                                color = SlateBg,
                                border = BorderStroke(1.dp, SlateOutline),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "১) শ্রেণি নির্বাচন করুন",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = SlateText
                                        )
                                        val allSelected = request.selectedClasses.size == activeClassList.size
                                        TextButton(
                                            onClick = {
                                                request = request.copy(
                                                    selectedClasses = if (allSelected) setOf(1) else activeClassList.toSet()
                                                )
                                            },
                                            modifier = Modifier.testTag("reset_toggle_all_classes")
                                        ) {
                                            Text(
                                                if (allSelected) "শুধু ১ম শ্রেণি" else "সব শ্রেণি নিন",
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        activeClassList.forEach { c ->
                                            val sel = c in request.selectedClasses
                                            val cnt = countByClass[c] ?: 0
                                            FilterChip(
                                                selected = sel,
                                                onClick = {
                                                    val next = if (sel) request.selectedClasses - c else request.selectedClasses + c
                                                    val validKeys = DataResetEngine.subjectOptionsForClasses(next)
                                                        .map { it.key }.toSet()
                                                    request = request.copy(
                                                        selectedClasses = next,
                                                        selectedSubjectKeys = request.selectedSubjectKeys.intersect(validKeys)
                                                            .ifEmpty { setOf("BAN") }
                                                    )
                                                },
                                                colors = appFilterChipColors(),
                                                border = appFilterChipBorder(sel),
                                                modifier = Modifier.testTag("reset_class_$c"),
                                                label = {
                                                    Text(
                                                        "${classShortBn(c, settings)} (${bn(cnt.toString())})",
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            )
                                        }
                                    }
                                    if (request.selectedClasses.isEmpty()) {
                                        Text(
                                            "অন্তত একটি শ্রেণি বেছে নিন",
                                            fontSize = 11.5.sp,
                                            color = RoseDanger
                                        )
                                    }
                                }
                            }

                            // Term Picker (for RESULTS & ATTENDANCE_TRAITS)
                            if (request.category == DataResetCategory.RESULTS ||
                                request.category == DataResetCategory.ATTENDANCE_TRAITS
                            ) {
                                Surface(
                                    color = SlateBg,
                                    border = BorderStroke(1.dp, SlateOutline),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "২) কোন প্রান্তিকের তথ্য রিসেট করবেন?",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = SlateText
                                        )
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(
                                                1 to "১ম প্রান্তিক",
                                                2 to "২য় প্রান্তিক",
                                                3 to "৩য় প্রান্তিক"
                                            ).forEach { (term, label) ->
                                                val sel = term in request.selectedTerms
                                                FilterChip(
                                                    selected = sel,
                                                    onClick = {
                                                        val next = if (sel) request.selectedTerms - term else request.selectedTerms + term
                                                        request = request.copy(selectedTerms = next)
                                                    },
                                                    colors = appFilterChipColors(),
                                                    border = appFilterChipBorder(sel),
                                                    modifier = Modifier.testTag("reset_term_$term"),
                                                    label = { Text(label, fontSize = 12.sp) }
                                                )
                                            }
                                        }
                                        if (request.selectedTerms.isEmpty()) {
                                            Text(
                                                "অন্তত একটি প্রান্তিক নির্বাচন করুন",
                                                fontSize = 11.5.sp,
                                                color = RoseDanger
                                            )
                                        }
                                    }
                                }
                            }

                            // Category-specific options
                            when (request.category) {
                                DataResetCategory.RESULTS -> {
                                    Surface(
                                        color = SlateBg,
                                        border = BorderStroke(1.dp, SlateOutline),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "৩) বিষয় নির্বাচন করুন",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = SlateText
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                FilterChip(
                                                    selected = request.allSubjects,
                                                    onClick = { request = request.copy(allSubjects = true) },
                                                    colors = appFilterChipColors(),
                                                    border = appFilterChipBorder(request.allSubjects),
                                                    modifier = Modifier.testTag("reset_all_subjects_chip"),
                                                    label = { Text("সকল বিষয়", fontSize = 12.sp) }
                                                )
                                                FilterChip(
                                                    selected = !request.allSubjects,
                                                    onClick = {
                                                        val fallback = request.selectedSubjectKeys.ifEmpty { setOf("BAN") }
                                                        request = request.copy(
                                                            allSubjects = false,
                                                            selectedSubjectKeys = fallback
                                                        )
                                                    },
                                                    colors = appFilterChipColors(),
                                                    border = appFilterChipBorder(!request.allSubjects),
                                                    modifier = Modifier.testTag("reset_specific_subjects_chip"),
                                                    label = { Text("নির্দিষ্ট বিষয়", fontSize = 12.sp) }
                                                )
                                            }
                                            if (!request.allSubjects) {
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    subjectOptions.forEach { opt ->
                                                        val sel = opt.key in request.selectedSubjectKeys
                                                        FilterChip(
                                                            selected = sel,
                                                            onClick = {
                                                                val next = if (sel) {
                                                                    request.selectedSubjectKeys - opt.key
                                                                } else {
                                                                    request.selectedSubjectKeys + opt.key
                                                                }
                                                                request = request.copy(selectedSubjectKeys = next)
                                                            },
                                                            colors = appFilterChipColors(),
                                                            border = appFilterChipBorder(sel),
                                                            modifier = Modifier.testTag("reset_subject_${opt.key}"),
                                                            label = { Text(opt.labelBn, fontSize = 11.5.sp) }
                                                        )
                                                    }
                                                }
                                                if (request.selectedSubjectKeys.isEmpty()) {
                                                    Text(
                                                        "অন্তত একটি বিষয় বেছে নিন",
                                                        fontSize = 11.5.sp,
                                                        color = RoseDanger
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                DataResetCategory.ATTENDANCE_TRAITS -> {
                                    Surface(
                                        color = SlateBg,
                                        border = BorderStroke(1.dp, SlateOutline),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "৩) কোন কোন তথ্য মুছবেন?",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = SlateText
                                            )
                                            ResetCheckboxRow(
                                                checked = request.resetAttendance,
                                                label = "মোট কার্যদিবস ও উপস্থিতি",
                                                onCheckedChange = { request = request.copy(resetAttendance = it) }
                                            )
                                            ResetCheckboxRow(
                                                checked = request.resetTraits,
                                                label = "৩টি গুণাবলী (শ্রেণিকাজে সক্রিয়তা, শৃঙ্খলা, আচরণ)",
                                                onCheckedChange = { request = request.copy(resetTraits = it) }
                                            )
                                            ResetCheckboxRow(
                                                checked = request.resetRemarks,
                                                label = "শ্রেণি শিক্ষকের সার্বিক মন্তব্য",
                                                onCheckedChange = { request = request.copy(resetRemarks = it) }
                                            )
                                        }
                                    }
                                }

                                DataResetCategory.BASIC_INFO -> {
                                    Surface(
                                        color = SlateBg,
                                        border = BorderStroke(1.dp, SlateOutline),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "২) শিক্ষার্থীর কোন বেসিক তথ্য রিসেট করবেন?",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = SlateText
                                            )
                                            Text(
                                                text = "শিক্ষার্থীর নাম ও রোল অক্ষুণ্ণ থাকবে।",
                                                fontSize = 11.5.sp,
                                                color = SlateMuted
                                            )
                                            ResetCheckboxRow(
                                                checked = request.resetGuardian,
                                                label = "পিতা/মাতা/অভিভাবকের নাম মুছুন",
                                                onCheckedChange = { request = request.copy(resetGuardian = it) }
                                            )
                                            ResetCheckboxRow(
                                                checked = request.resetMobile,
                                                label = "মোবাইল নম্বর মুছুন",
                                                onCheckedChange = { request = request.copy(resetMobile = it) }
                                            )
                                            ResetCheckboxRow(
                                                checked = request.resetAddress,
                                                label = "ঠিকানা (গ্রাম, ইউনিয়ন, ডাকঘর মুছুন ও উপজেলা/জেলা ডিফল্ট করুন)",
                                                onCheckedChange = { request = request.copy(resetAddress = it) }
                                            )
                                            ResetCheckboxRow(
                                                checked = request.resetReligionToDefault,
                                                label = "ধর্ম ডিফল্ট (\"ইসলাম\") এ রিসেট করুন",
                                                onCheckedChange = { request = request.copy(resetReligionToDefault = it) }
                                            )
                                            if (settings.customFields.isNotEmpty()) {
                                                ResetCheckboxRow(
                                                    checked = request.resetCustomFields,
                                                    label = "সকল কাস্টম ফিল্ড (${bn(settings.customFields.size.toString())}টি) মুছুন",
                                                    onCheckedChange = { request = request.copy(resetCustomFields = it) }
                                                )
                                            }
                                        }
                                    }
                                }

                                DataResetCategory.CLASS_STUDENTS -> {
                                    Surface(
                                        color = RoseBg,
                                        border = BorderStroke(1.dp, RoseDanger.copy(alpha = 0.45f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                Icons.Rounded.WarningAmber,
                                                contentDescription = null,
                                                tint = RoseDanger,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "নির্বাচিত শ্রেণির মোট ${bn(impact.matchedStudentCount.toString())} জন শিক্ষার্থী সম্পূর্ণভাবে ডিলিট হয়ে যাবে। পরবর্তী ধাপে সতর্কতা দেখে ফিঙ্গারপ্রিন্ট দিয়ে নিশ্চিত করতে হবে।",
                                                fontSize = 12.sp,
                                                color = RoseDanger,
                                                lineHeight = 17.sp
                                            )
                                        }
                                    }
                                }

                                else -> Unit
                            }
                        }

                        3 -> {
                            // Destructive Warning Card
                            Surface(
                                color = RoseBg,
                                border = BorderStroke(1.5.dp, RoseDanger.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.WarningAmber,
                                            contentDescription = "গুরুত্বপূর্ণ সতর্কতা",
                                            tint = RoseDanger,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = "গুরুত্বপূর্ণ সতর্কতা (Warning)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp,
                                            color = RoseDanger
                                        )
                                    }

                                    Surface(
                                        color = Color.White,
                                        border = BorderStroke(1.dp, RoseDanger.copy(alpha = 0.25f)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "লক্ষ্য: ${impact.scopeSummaryBn}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp,
                                                color = SlateText
                                            )
                                            Text(
                                                text = "প্রভাবিত শিক্ষার্থী: ${bn(impact.affectedStudentCount.toString())} জন · মোট রেকর্ড: ${bn(impact.affectedEntryCount.toString())}টি",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = RoseDanger
                                            )
                                        }
                                    }

                                    impact.warningLines.forEach { line ->
                                        Text(
                                            text = "• $line",
                                            fontSize = 12.sp,
                                            color = SlateText,
                                            lineHeight = 17.sp
                                        )
                                    }

                                    Surface(
                                        color = TealSoft,
                                        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = EmeraldGrade,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "সুরক্ষিত থাকবে: ${impact.preservedSummaryBn}",
                                                fontSize = 11.5.sp,
                                                color = TealDark,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Fingerprint / Biometric Permission & Verification Card
                            Surface(
                                color = SlateBg,
                                border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = TealContainer,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Rounded.Fingerprint,
                                                    contentDescription = "ফিঙ্গারপ্রিন্ট সেন্সর",
                                                    tint = TealDark,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "ফিঙ্গারপ্রিন্ট সেন্সর অনুমতি ও যাচাই",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = SlateText
                                            )
                                            Text(
                                                text = "যেকোনো ডাটা রিসেটের আগে আপনার ফিঙ্গারপ্রিন্ট সেন্সর যাচাই করা বাধ্যতামূলক।",
                                                fontSize = 11.5.sp,
                                                color = SlateMuted,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }

                                    Text(
                                        text = BiometricResetAuthenticator.availabilityHintBn(biometricAvailability),
                                        fontSize = 12.sp,
                                        color = if (biometricAvailability == BiometricAvailability.READY) TealDark else AmberNotice,
                                        lineHeight = 17.sp
                                    )

                                    authStatusMsg?.let { msg ->
                                        Surface(
                                            color = if (authStatusIsError) RoseBg else AmberBg,
                                            border = BorderStroke(
                                                1.dp,
                                                if (authStatusIsError) RoseDanger.copy(alpha = 0.5f) else AmberBorder
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = msg,
                                                fontSize = 11.5.sp,
                                                color = if (authStatusIsError) RoseDanger else AmberNotice,
                                                modifier = Modifier.padding(10.dp),
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }

                                    if (biometricAvailability == BiometricAvailability.READY) {
                                        Button(
                                            onClick = {
                                                authStatusMsg = null
                                                BiometricResetAuthenticator.authenticateWithFingerprint(
                                                    context = ctx,
                                                    titleBn = "ফিঙ্গারপ্রিন্ট সেন্সর যাচাই",
                                                    subtitleBn = request.category.titleBn,
                                                    warningDescriptionBn = "সতর্কতা: ${impact.scopeSummaryBn} রিসেট করলে ${bn(impact.affectedStudentCount.toString())} জন শিক্ষার্থীর তথ্য পরিবর্তিত/মুছে যাবে। চালিয়ে যেতে ফিঙ্গারপ্রিন্ট দিন।",
                                                    useDeviceCredentialFallback = false
                                                ) { res ->
                                                    when (res) {
                                                        BiometricVerificationResult.Verified -> {
                                                            onConfirmReset(request)
                                                            onDismiss()
                                                        }
                                                        BiometricVerificationResult.Cancelled -> {
                                                            authStatusIsError = false
                                                            authStatusMsg = "ফিঙ্গারপ্রিন্ট যাচাই বাতিল করা হয়েছে। কোনো ডাটা রিসেট হয়নি।"
                                                        }
                                                        is BiometricVerificationResult.FailedAttempt -> {
                                                            authStatusIsError = true
                                                            authStatusMsg = res.messageBn
                                                        }
                                                        is BiometricVerificationResult.Error -> {
                                                            authStatusIsError = true
                                                            authStatusMsg = res.messageBn
                                                        }
                                                        is BiometricVerificationResult.Unavailable -> {
                                                            biometricAvailability = res.availability
                                                            authStatusIsError = true
                                                            authStatusMsg = res.messageBn
                                                        }
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .testTag("data_reset_fingerprint_btn")
                                        ) {
                                            Icon(
                                                Icons.Rounded.Fingerprint,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.size(8.dp))
                                            Text(
                                                "ফিঙ্গারপ্রিন্ট যাচাই করে রিসেট করুন",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    } else {
                                        // Biometric unavailable / not enrolled
                                        if (biometricAvailability == BiometricAvailability.NONE_ENROLLED) {
                                            OutlinedButton(
                                                onClick = {
                                                    BiometricResetAuthenticator.openBiometricEnrollSettings(ctx)
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 48.dp)
                                                    .testTag("data_reset_open_security_btn")
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Settings,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.size(6.dp))
                                                Text(
                                                    "ডিভাইস সেটিংসে ফিঙ্গারপ্রিন্ট যোগ করুন",
                                                    fontSize = 12.5.sp
                                                )
                                            }
                                        }

                                        if (hasDeviceLockFallback) {
                                            Button(
                                                onClick = {
                                                    authStatusMsg = null
                                                    BiometricResetAuthenticator.authenticateWithFingerprint(
                                                        context = ctx,
                                                        titleBn = "ডিভাইস নিরাপত্তা যাচাই",
                                                        subtitleBn = request.category.titleBn,
                                                        warningDescriptionBn = "সতর্কতা: ${impact.scopeSummaryBn} রিসেট নিশ্চিত করতে ডিভাইস PIN/Pattern/Password দিন।",
                                                        useDeviceCredentialFallback = true
                                                    ) { res ->
                                                        when (res) {
                                                            BiometricVerificationResult.Verified -> {
                                                                onConfirmReset(request)
                                                                onDismiss()
                                                            }
                                                            BiometricVerificationResult.Cancelled -> {
                                                                authStatusIsError = false
                                                                authStatusMsg = "নিরাপত্তা যাচাই বাতিল করা হয়েছে।"
                                                            }
                                                            is BiometricVerificationResult.FailedAttempt -> {
                                                                authStatusIsError = true
                                                                authStatusMsg = res.messageBn
                                                            }
                                                            is BiometricVerificationResult.Error -> {
                                                                authStatusIsError = true
                                                                authStatusMsg = res.messageBn
                                                            }
                                                            is BiometricVerificationResult.Unavailable -> {
                                                                authStatusIsError = true
                                                                authStatusMsg = res.messageBn
                                                            }
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 48.dp)
                                                    .testTag("data_reset_device_lock_btn")
                                            ) {
                                                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.size(8.dp))
                                                Text("ডিভাইস লক দিয়ে যাচাই ও রিসেট করুন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        } else {
                                            // No fingerprint AND no device lock screen configured (e.g. clean test device)
                                            Surface(
                                                color = AmberBg,
                                                border = BorderStroke(1.dp, AmberBorder),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Rounded.Shield,
                                                            contentDescription = null,
                                                            tint = AmberNotice,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Text(
                                                            text = "জরুরি নিরাপত্তা কোড যাচাই",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = AmberNotice
                                                        )
                                                    }
                                                    Text(
                                                        text = "এই ডিভাইসে ফিঙ্গারপ্রিন্ট বা স্ক্রিন-লক সেট করা না থাকায় নিচের ঘরে ইংরেজিতে RESET লিখে নিশ্চিত করুন:",
                                                        fontSize = 11.5.sp,
                                                        color = SlateText,
                                                        lineHeight = 16.sp
                                                    )
                                                    OutlinedTextField(
                                                        value = fallbackTypedConfirm,
                                                        onValueChange = { fallbackTypedConfirm = it.trim().uppercase() },
                                                        label = { Text("RESET লিখুন") },
                                                        singleLine = true,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .testTag("data_reset_type_confirm_input")
                                                    )
                                                    Button(
                                                        onClick = {
                                                            onConfirmReset(request)
                                                            onDismiss()
                                                        },
                                                        enabled = fallbackTypedConfirm == "RESET",
                                                        colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                                                        shape = RoundedCornerShape(10.dp),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .heightIn(min = 48.dp)
                                                            .testTag("data_reset_fallback_confirm_btn")
                                                    ) {
                                                        Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                                                        Spacer(Modifier.size(6.dp))
                                                        Text("নিশ্চিত করে রিসেট করুন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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

                HorizontalDivider(color = SlateOutline)

                // Footer Navigation Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > startStep) {
                        OutlinedButton(
                            onClick = {
                                authStatusMsg = null
                                currentStep = when (currentStep) {
                                    3 -> if (request.category == DataResetCategory.ALL_STUDENTS ||
                                        request.category == DataResetCategory.SAMPLE_DATA
                                    ) 1 else 2
                                    else -> 1
                                }
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("data_reset_prev_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("পূর্ববর্তী ধাপ", fontSize = 12.5.sp)
                        }
                    } else {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("data_reset_cancel_btn")
                        ) {
                            Text("বাতিল", fontSize = 13.sp, color = SlateMuted)
                        }
                    }

                    if (currentStep < 3) {
                        val nextEnabled = when (currentStep) {
                            1 -> true
                            2 -> isStep2Valid
                            else -> false
                        }
                        Button(
                            onClick = {
                                authStatusMsg = null
                                fallbackTypedConfirm = ""
                                currentStep = when (currentStep) {
                                    1 -> if (request.category == DataResetCategory.ALL_STUDENTS ||
                                        request.category == DataResetCategory.SAMPLE_DATA
                                    ) 3 else 2
                                    else -> 3
                                }
                            },
                            enabled = nextEnabled,
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("data_reset_next_btn")
                        ) {
                            Text(
                                text = if (currentStep == 2 ||
                                    request.category == DataResetCategory.ALL_STUDENTS ||
                                    request.category == DataResetCategory.SAMPLE_DATA
                                ) "সতর্কতা ও যাচাই দেখুন" else "পরবর্তী ধাপ",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.size(4.dp))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("বন্ধ করুন", fontSize = 12.5.sp, color = SlateMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepPill(
    stepNum: String,
    label: String,
    active: Boolean,
    done: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = when {
        active -> TealPrimary
        done -> TealContainer
        else -> Color.White
    }
    val textCol = when {
        active -> Color.White
        done -> TealDark
        else -> SlateMuted
    }
    Surface(
        color = bg,
        border = BorderStroke(1.dp, if (active || done) TealPrimary else SlateOutline),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$stepNum.",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textCol
            )
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                color = textCol,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ResetCheckboxRow(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { onCheckedChange(!checked) }
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = label,
            fontSize = 12.5.sp,
            color = SlateText,
            lineHeight = 17.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun categoryIcon(cat: DataResetCategory): ImageVector = when (cat) {
    DataResetCategory.RESULTS -> Icons.Rounded.Grade
    DataResetCategory.ATTENDANCE_TRAITS -> Icons.Rounded.EventNote
    DataResetCategory.BASIC_INFO -> Icons.Rounded.PersonOutline
    DataResetCategory.CLASS_STUDENTS -> Icons.Rounded.AutoDelete
    DataResetCategory.ALL_STUDENTS -> Icons.Rounded.DeleteForever
    DataResetCategory.SAMPLE_DATA -> Icons.Rounded.RestartAlt
}
