package com.kgps.result.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AddBusiness
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.Settings
import com.kgps.result.bn
import com.kgps.result.cloud.SchoolCodePreview
import com.kgps.result.cloud.SchoolWorkspace
import com.kgps.result.ui.components.TreeBranchItem
import com.kgps.result.ui.components.TreeGroupCard
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

@Composable
fun SchoolHubScreen(
    userDisplayName: String,
    userEmail: String,
    currentUserId: String,
    schools: List<SchoolWorkspace>,
    activeSchoolId: String,
    pendingJoinSchoolName: String?,
    localSeedCount: Int,
    localSeedSettings: Settings?,
    isBusy: Boolean,
    lookupBusy: Boolean,
    codePreview: SchoolCodePreview?,
    lookupError: String?,
    onSelectSchool: (String) -> Unit,
    onCreateSchool: (name: String, address: String, year: String, approvalRequired: Boolean, importLocalData: Boolean) -> Unit,
    onLookupCode: (String) -> Unit,
    onClearCodePreview: () -> Unit,
    onJoinWithPreview: (SchoolCodePreview) -> Unit,
    onBackToApp: (() -> Unit)?,
    onSignOut: () -> Unit
) {
    if (onBackToApp != null) {
        BackHandler { onBackToApp() }
    }

    var expandedSection by remember(schools.isEmpty()) {
        mutableStateOf(if (schools.isEmpty()) "join" else "create")
    }
    var schoolName by remember(localSeedSettings?.school) {
        mutableStateOf(
            if (schools.isEmpty()) localSeedSettings?.school?.takeIf { it.isNotBlank() } ?: "" else ""
        )
    }
    var schoolAddress by remember(localSeedSettings?.schoolAddress) {
        mutableStateOf(
            if (schools.isEmpty()) localSeedSettings?.schoolAddress.orEmpty() else ""
        )
    }
    var schoolYear by remember(localSeedSettings?.year) {
        mutableStateOf(localSeedSettings?.year?.ifBlank { "২০২৬" } ?: "২০২৬")
    }
    var approvalRequired by remember { mutableStateOf(false) }
    var importLocalData by remember(localSeedCount, schools.isEmpty()) {
        mutableStateOf(localSeedCount > 0 && schools.isEmpty())
    }

    var joinCodeInput by remember { mutableStateOf("") }
    var askSignOut by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SlateBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header / User Identity Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SlateOutline),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onBackToApp != null) {
                            IconButton(
                                onClick = onBackToApp,
                                modifier = Modifier.size(48.dp).testTag("hub_back_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "অ্যাপে ফিরে যান")
                            }
                        } else {
                            Surface(
                                color = TealContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Person, contentDescription = null, tint = TealDark)
                                }
                            }
                        }

                        Column(Modifier.weight(1f)) {
                            Text(
                                text = userDisplayName.ifBlank { "শিক্ষক" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = SlateText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (userEmail.isNotBlank()) {
                                Text(
                                    text = userEmail,
                                    fontSize = 11.5.sp,
                                    color = SlateMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { askSignOut = true },
                            enabled = !isBusy,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("hub_sign_out_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(16.dp), tint = RoseDanger)
                            Spacer(Modifier.size(6.dp))
                            Text("সাইন আউট", fontSize = 12.sp, color = RoseDanger)
                        }
                    }
                }

                if (!pendingJoinSchoolName.isNullOrBlank()) {
                    Surface(
                        color = AmberBg,
                        border = BorderStroke(1.dp, AmberBorder),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("hub_pending_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.HourglassTop, contentDescription = null, tint = AmberNotice)
                            Column(Modifier.weight(1f)) {
                                Text("অনুমোদনের অপেক্ষায়", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AmberNotice)
                                Text(
                                    "\"$pendingJoinSchoolName\" বিদ্যালয়ে যোগদানের অনুরোধ পাঠানো হয়েছে। অ্যাডমিন অনুমোদন করলেই তালিকা আপডেট হবে।",
                                    fontSize = 12.sp,
                                    color = SlateText,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }

                // User's Joined Schools
                if (schools.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, SlateOutline),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.School, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                                Text(
                                    "আপনার বিদ্যালয়সমূহ (${bn(schools.size.toString())})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = SlateText
                                )
                            }

                            schools.forEach { school ->
                                val isCurrent = school.id == activeSchoolId
                                val role = school.roleOf(currentUserId)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("hub_school_card_${school.id}")
                                        .clickable(enabled = !isBusy) { onSelectSchool(school.id) },
                                    color = if (isCurrent) TealSoft else SlateBg,
                                    border = BorderStroke(1.dp, if (isCurrent) TealPrimary else SlateOutline),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(
                                                text = school.settings.school.ifBlank { "বিদ্যালয়" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.5.sp,
                                                color = SlateText
                                            )
                                            Text(
                                                text = "শিক্ষাবর্ষ ${bn(school.settings.year)} · ${bn(school.memberUids.size.toString())} জন সদস্য · কোড: ${school.joinCode}",
                                                fontSize = 11.5.sp,
                                                color = SlateMuted
                                            )
                                        }
                                        Surface(
                                            color = if (role.code == "ADMIN") TealContainer else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = role.labelBn,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (role.code == "ADMIN") TealDark else SlateMuted,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        if (isCurrent) {
                                            Icon(
                                                Icons.Rounded.CheckCircle,
                                                contentDescription = "সক্রিয় বিদ্যালয়",
                                                tint = EmeraldGrade,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Create New School Card
                TreeGroupCard(
                    title = "নতুন বিদ্যালয় তৈরি করুন",
                    subtitle = "নিজে অ্যাডমিন হয়ে স্কুল কোড তৈরি ও শেয়ার করুন",
                    badgeText = "নতুন",
                    icon = Icons.Rounded.AddBusiness,
                    expanded = expandedSection == "create",
                    headerTestTag = "hub_create_group_header",
                    onToggle = { expandedSection = if (expandedSection == "create") "" else "create" }
                ) {
                    TreeBranchItem(isLast = true) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = schoolName,
                                onValueChange = { schoolName = it },
                                label = { Text("বিদ্যালয়ের নাম *") },
                                placeholder = { Text("যেমন: কামারখালী সরকারি প্রাথমিক বিদ্যালয়") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("hub_create_school_name")
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = schoolAddress,
                                    onValueChange = { schoolAddress = it },
                                    label = { Text("ঠিকানা (ঐচ্ছিক)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.4f).testTag("hub_create_school_address")
                                )
                                OutlinedTextField(
                                    value = schoolYear,
                                    onValueChange = { schoolYear = it },
                                    label = { Text("শিক্ষাবর্ষ") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.7f).testTag("hub_create_school_year")
                                )
                            }

                            Surface(
                                color = SlateBg,
                                border = BorderStroke(1.dp, SlateOutline),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("যোগদানের আগে অ্যাডমিন অনুমোদন", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        Text(
                                            "চালু রাখলে কোড দেওয়ার পর অ্যাডমিন অনুমোদন করলে শিক্ষক যুক্ত হতে পারবেন।",
                                            fontSize = 11.sp,
                                            color = SlateMuted
                                        )
                                    }
                                    Switch(
                                        checked = approvalRequired,
                                        onCheckedChange = { approvalRequired = it },
                                        modifier = Modifier.testTag("hub_create_approval_switch")
                                    )
                                }
                            }

                            if (localSeedCount > 0) {
                                Surface(
                                    color = TealSoft,
                                    border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.25f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                "ডিভাইসের ${bn(localSeedCount.toString())} জন শিক্ষার্থী যুক্ত করুন",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.5.sp,
                                                color = TealDark
                                            )
                                            Text(
                                                "পূর্বের সংরক্ষিত শিক্ষার্থী ও কাস্টম ফিল্ড ক্লাউডে আপলোড হবে।",
                                                fontSize = 11.sp,
                                                color = SlateMuted
                                            )
                                        }
                                        Switch(
                                            checked = importLocalData,
                                            onCheckedChange = { importLocalData = it },
                                            modifier = Modifier.testTag("hub_create_import_local_switch")
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    onCreateSchool(schoolName, schoolAddress, schoolYear, approvalRequired, importLocalData)
                                },
                                enabled = !isBusy && schoolName.isNotBlank(),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("hub_create_submit_btn")
                            ) {
                                if (isBusy) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.size(8.dp))
                                }
                                Text("বিদ্যালয় তৈরি করুন ও কোড পান", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Join Existing School Card
                TreeGroupCard(
                    title = "কোড দিয়ে বিদ্যমান বিদ্যালয়ে যুক্ত হোন",
                    subtitle = "অ্যাডমিনের দেওয়া ৬ অক্ষরের স্কুল কোড দিন",
                    badgeText = "যুক্ত হোন",
                    icon = Icons.Rounded.Key,
                    expanded = expandedSection == "join",
                    headerTestTag = "hub_join_group_header",
                    onToggle = { expandedSection = if (expandedSection == "join") "" else "join" }
                ) {
                    TreeBranchItem(isLast = true) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = joinCodeInput,
                                    onValueChange = { raw ->
                                        val clean = raw.uppercase().replace(Regex("[^A-Z0-9]"), "").take(12)
                                        joinCodeInput = clean
                                        if (codePreview != null || lookupError != null) onClearCodePreview()
                                    },
                                    label = { Text("স্কুল কোড") },
                                    placeholder = { Text("যেমন: KGP482") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("hub_join_code_input")
                                )
                                Button(
                                    onClick = { onLookupCode(joinCodeInput) },
                                    enabled = !lookupBusy && !isBusy && joinCodeInput.length >= 6,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(52.dp).testTag("hub_lookup_code_btn")
                                ) {
                                    if (lookupBusy) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.size(4.dp))
                                        Text("খুঁজুন")
                                    }
                                }
                            }

                            if (!lookupError.isNullOrBlank()) {
                                Surface(
                                    color = RoseBg,
                                    border = BorderStroke(1.dp, RoseDanger.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = lookupError,
                                        color = RoseDanger,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            if (codePreview != null) {
                                val alreadyMember = schools.any { it.id == codePreview.schoolId }
                                Surface(
                                    color = TealSoft,
                                    border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("hub_code_preview_card")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = codePreview.schoolName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp,
                                            color = SlateText
                                        )
                                        Text(
                                            text = if (codePreview.approvalRequired)
                                                "অনুমোদন প্রয়োজন: অনুরোধ পাঠালে অ্যাডমিন অনুমোদন করবেন।"
                                            else
                                                "সরাসরি যোগদান চালু আছে: সাথে সাথেই বিদ্যালয়ে যুক্ত হবেন।",
                                            fontSize = 12.sp,
                                            color = SlateMuted
                                        )
                                        if (alreadyMember) {
                                            OutlinedButton(
                                                onClick = { onSelectSchool(codePreview.schoolId) },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("আপনি ইতিমধ্যে সদস্য — প্রবেশ করুন")
                                            }
                                        } else {
                                            Button(
                                                onClick = { onJoinWithPreview(codePreview) },
                                                enabled = !isBusy,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("hub_join_confirm_btn")
                                            ) {
                                                Text(
                                                    if (codePreview.approvalRequired) "যোগদানের অনুরোধ পাঠান" else "বিদ্যালয়ে যুক্ত হোন",
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (askSignOut) {
        AlertDialog(
            onDismissRequest = { askSignOut = false },
            title = { Text("সাইন আউট করবেন?") },
            text = { Text("আপনার সকল ডেটা ক্লাউডে নিরাপদ থাকবে। পরবর্তীতে Google দিয়ে সাইন-ইন করলে পুনরায় সব দেখতে পারবেন।") },
            confirmButton = {
                Button(
                    onClick = {
                        askSignOut = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                    modifier = Modifier.testTag("hub_confirm_signout_btn")
                ) { Text("হ্যাঁ, সাইন আউট") }
            },
            dismissButton = {
                TextButton(onClick = { askSignOut = false }) { Text("বাতিল") }
            }
        )
    }
}
