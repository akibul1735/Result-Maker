package com.kgps.result.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.ai.DiscoveredGeminiModel
import com.kgps.result.ai.GeminiApiConfig
import com.kgps.result.bn
import com.kgps.result.ui.theme.AmberBg
import com.kgps.result.ui.theme.AmberBorder
import com.kgps.result.ui.theme.AmberNotice
import com.kgps.result.ui.theme.RoseDanger
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealContainer
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft

/**
 * ব্যবহারকারীর নির্বাচিত সর্বোচ্চ ৩টি AI মডেলের ক্রম (১ → ২ → ৩) সাজানো ও পরিচালনার কার্ড।
 * কোনো ডিফল্ট/প্রস্তাবিত মডেল থাকে না; সব মডেল লাইভ API বা কাস্টম তালিকা থেকে আসে।
 */
@Composable
fun OrderedThreeModelsCard(
    orderedModels: List<String>,
    availableModels: List<DiscoveredGeminiModel>,
    customModels: List<String>,
    onSetModelSlot: (slotIndex: Int, modelId: String?) -> Unit,
    onMoveModelSlot: (index: Int, delta: Int) -> Unit,
    onRequestFetchLiveModels: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var editingSlotIndex by remember { mutableStateOf<Int?>(null) }
    var pendingRemoveSlot by remember { mutableStateOf<Pair<Int, String>?>(null) }

    val slotRoles = remember {
        listOf("মূল (১ম)", "বিকল্প ১ (২য়)", "বিকল্প ২ (৩য়)")
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SwapVert,
                contentDescription = null,
                tint = TealPrimary,
                modifier = Modifier.size(18.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = "নির্বাচিত মডেলের ক্রম",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = SlateText
                )
                Text(
                    text = "১ম মডেল ব্যস্ত বা ত্রুটি দিলে ২য় ও ৩য় মডেলে চেষ্টা করবে",
                    fontSize = 10.5.sp,
                    color = SlateMuted,
                    lineHeight = 14.sp
                )
            }
            Surface(
                color = if (orderedModels.isEmpty()) AmberBg else TealSoft,
                border = BorderStroke(
                    1.dp,
                    if (orderedModels.isEmpty()) AmberBorder else TealPrimary.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "${bn(orderedModels.size.toString())}/৩ নির্বাচিত",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (orderedModels.isEmpty()) AmberNotice else TealDark,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (slotIdx in 0 until GeminiApiConfig.MAX_ORDERED_MODELS) {
                val modelId = orderedModels.getOrNull(slotIdx)
                val isFilled = !modelId.isNullOrBlank()
                val slotNumBn = bn((slotIdx + 1).toString())
                val roleLabel = slotRoles.getOrElse(slotIdx) { "স্লট $slotNumBn" }

                Surface(
                    color = when {
                        isFilled && slotIdx == 0 -> TealContainer.copy(alpha = 0.45f)
                        isFilled -> SlateBg
                        else -> MaterialTheme.colorScheme.surface
                    },
                    border = BorderStroke(
                        width = if (isFilled && slotIdx == 0) 1.5.dp else 1.dp,
                        color = when {
                            isFilled && slotIdx == 0 -> TealPrimary
                            isFilled -> SlateOutline
                            else -> SlateOutline.copy(alpha = 0.55f)
                        }
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = when {
                                isFilled && slotIdx == 0 -> TealPrimary
                                isFilled -> TealContainer
                                else -> SlateBg
                            },
                            shape = RoundedCornerShape(7.dp)
                        ) {
                            Text(
                                text = slotNumBn,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isFilled && slotIdx == 0 -> Color.White
                                    isFilled -> TealDark
                                    else -> SlateMuted
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .clickable { editingSlotIndex = slotIdx }
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                .testTag("ordered_model_slot_select_$slotIdx"),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = roleLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isFilled && slotIdx == 0) TealDark else SlateMuted
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = modelId ?: "+ লাইভ তালিকা থেকে বাছুন",
                                    fontSize = 12.sp,
                                    fontWeight = if (isFilled) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isFilled) SlateText else SlateMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDropDown,
                                    contentDescription = "ক্রম $slotNumBn মডেল নির্বাচন",
                                    tint = SlateMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (isFilled) {
                            IconButton(
                                onClick = { onMoveModelSlot(slotIdx, -1) },
                                enabled = slotIdx > 0,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("ordered_model_slot_up_$slotIdx")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = "মডেল $slotNumBn উপরে নিন",
                                    tint = if (slotIdx > 0) TealPrimary else SlateOutline,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { onMoveModelSlot(slotIdx, +1) },
                                enabled = slotIdx < orderedModels.lastIndex,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("ordered_model_slot_down_$slotIdx")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "মডেল $slotNumBn নিচে নিন",
                                    tint = if (slotIdx < orderedModels.lastIndex) TealPrimary else SlateOutline,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { pendingRemoveSlot = slotIdx to modelId!! },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("ordered_model_slot_remove_$slotIdx")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "মডেল $slotNumBn সরান",
                                    tint = RoseDanger,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    editingSlotIndex?.let { slotIdx ->
        LiveModelSlotPickerDialog(
            slotIndex = slotIdx,
            currentSlotModel = orderedModels.getOrNull(slotIdx),
            orderedModels = orderedModels,
            availableModels = availableModels,
            customModels = customModels,
            onRequestFetchLiveModels = onRequestFetchLiveModels,
            onPick = { chosen ->
                onSetModelSlot(slotIdx, chosen)
                editingSlotIndex = null
            },
            onDismiss = { editingSlotIndex = null }
        )
    }

    pendingRemoveSlot?.let { (slotIdx, modelId) ->
        val slotNumBn = bn((slotIdx + 1).toString())
        BiometricDeleteConfirmDialog(
            title = "ক্রম $slotNumBn থেকে মডেল সরাবেন?",
            description = "\"$modelId\" মডেলটি নির্বাচিত তালিকা থেকে সরানো হবে।",
            warningDescription = "নির্বাচিত মডেল সরাতে আপনার ফিঙ্গারপ্রিন্ট অনুমতি দিন।",
            confirmActionLabel = "ফিঙ্গারপ্রিন্ট অনুমতি দিয়ে সরান",
            confirmButtonTestTag = "ordered_model_confirm_remove_btn",
            onDismiss = { pendingRemoveSlot = null },
            onConfirmedDelete = { onSetModelSlot(slotIdx, null) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LiveModelSlotPickerDialog(
    slotIndex: Int,
    currentSlotModel: String?,
    orderedModels: List<String>,
    availableModels: List<DiscoveredGeminiModel>,
    customModels: List<String>,
    onRequestFetchLiveModels: (() -> Unit)?,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var customInput by remember(currentSlotModel) { mutableStateOf(currentSlotModel.orEmpty()) }
    var askClearSlot by remember { mutableStateOf(false) }
    val slotNumBn = bn((slotIndex + 1).toString())

    val liveAndCustom = remember(availableModels, customModels) {
        val out = linkedMapOf<String, String>()
        availableModels.forEach { m ->
            out[m.id] = m.displayName.takeIf { it.isNotBlank() && !it.equals(m.id, ignoreCase = true) }.orEmpty()
        }
        customModels.forEach { cid ->
            if (!out.containsKey(cid)) out[cid] = "কাস্টম"
        }
        out.entries.map { it.key to it.value }
    }

    val filteredModels = remember(liveAndCustom, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) liveAndCustom
        else liveAndCustom.filter { (id, label) ->
            id.lowercase().contains(q) || label.lowercase().contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "ক্রম $slotNumBn-এর জন্য মডেল বাছুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp
                )
                if (!currentSlotModel.isNullOrBlank()) {
                    Text(
                        text = "বর্তমান: $currentSlotModel",
                        fontSize = 11.sp,
                        color = TealPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (liveAndCustom.isEmpty()) {
                    Surface(
                        color = AmberBg,
                        border = BorderStroke(1.dp, AmberBorder),
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "এখনো API থেকে কোনো লাইভ মডেল আনা হয়নি।",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AmberNotice
                            )
                            Text(
                                text = "প্রথমে লাইভ মডেল আনুন অথবা নিচে পছন্দের মডেল আইডি লিখুন।",
                                fontSize = 11.sp,
                                color = SlateMuted
                            )
                            if (onRequestFetchLiveModels != null) {
                                OutlinedButton(
                                    onClick = {
                                        onRequestFetchLiveModels()
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text("API থেকে লাইভ মডেল আনুন", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    if (liveAndCustom.size > 5) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("মডেল খুঁজুন...", fontSize = 11.5.sp) },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "খোঁজ মুছুন", modifier = Modifier.size(15.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 12.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (filteredModels.isEmpty()) {
                        Text(
                            text = "\"$searchQuery\"-এর সাথে কোনো লাইভ মডেল মেলেনি।",
                            fontSize = 11.5.sp,
                            color = SlateMuted
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            filteredModels.take(30).forEach { (modelId, subLabel) ->
                                val isCurrentSlot = currentSlotModel == modelId
                                val otherIdx = orderedModels.indexOf(modelId).takeIf { it >= 0 && it != slotIndex }
                                Surface(
                                    color = if (isCurrentSlot) TealContainer.copy(alpha = 0.7f) else SlateBg,
                                    border = BorderStroke(
                                        if (isCurrentSlot) 1.5.dp else 1.dp,
                                        if (isCurrentSlot) TealPrimary else SlateOutline
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPick(modelId) }
                                        .testTag("slot_pick_$modelId")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                text = modelId,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                color = SlateText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val note = when {
                                                otherIdx != null && subLabel.isNotBlank() ->
                                                    "$subLabel · ক্রম ${bn((otherIdx + 1).toString())} থেকে অদলবদল"
                                                otherIdx != null ->
                                                    "ক্রম ${bn((otherIdx + 1).toString())} থেকে অদলবদল হবে"
                                                else -> subLabel
                                            }
                                            if (note.isNotBlank()) {
                                                Text(
                                                    text = note,
                                                    fontSize = 10.sp,
                                                    color = if (otherIdx != null) TealDark else SlateMuted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        if (otherIdx != null) {
                                            Surface(color = TealSoft, shape = RoundedCornerShape(5.dp)) {
                                                Text(
                                                    text = "#${bn((otherIdx + 1).toString())}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TealDark,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        if (isCurrentSlot) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = "নির্বাচিত",
                                                tint = TealPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = SlateOutline.copy(alpha = 0.7f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customInput,
                        onValueChange = { customInput = it },
                        label = { Text("কাস্টম মডেল আইডি (ঐচ্ছিক)", fontSize = 11.sp) },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 11.5.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val clean = customInput.trim().removePrefix("models/").trim()
                            if (clean.isNotBlank() && !GeminiApiConfig.isExcludedModelId(clean)) {
                                onPick(clean)
                            }
                        },
                        enabled = customInput.trim().removePrefix("models/").trim().let {
                            it.isNotBlank() && !GeminiApiConfig.isExcludedModelId(it)
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text("যুক্ত", fontSize = 11.5.sp)
                    }
                }
            }
        },
        dismissButton = {
            if (!currentSlotModel.isNullOrBlank()) {
                TextButton(
                    onClick = { askClearSlot = true },
                    modifier = Modifier.testTag("ordered_slot_clear_btn")
                ) {
                    Text("স্লট খালি রাখুন", fontSize = 12.sp, color = RoseDanger)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন", fontSize = 12.sp)
            }
        }
    )

    if (askClearSlot && !currentSlotModel.isNullOrBlank()) {
        BiometricDeleteConfirmDialog(
            title = "ক্রম $slotNumBn স্লট খালি করবেন?",
            description = "\"$currentSlotModel\" মডেলটি ক্রম $slotNumBn থেকে সরানো হবে।",
            warningDescription = "মডেল স্লটটি খালি করতে আপনার ফিঙ্গারপ্রিন্ট অনুমতি দিন।",
            confirmActionLabel = "ফিঙ্গারপ্রিন্ট অনুমতি দিয়ে সরান",
            confirmButtonTestTag = "ordered_slot_confirm_clear_btn",
            onDismiss = { askClearSlot = false },
            onConfirmedDelete = { onPick(null) }
        )
    }
}
