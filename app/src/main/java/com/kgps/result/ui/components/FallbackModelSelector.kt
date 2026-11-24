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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

/**
 * সর্বোচ্চ ৩টি Fallback (বিকল্প) AI মডেল নির্বাচন ও ক্রম সাজানোর সংক্ষিপ্ত কার্ড।
 */
@Composable
fun CompactFallbackModelsSection(
    primaryModel: String,
    fallbackModels: List<String>,
    availableModels: List<DiscoveredGeminiModel>,
    customModels: List<String>,
    onSetFallbackSlot: (slotIndex: Int, modelId: String?) -> Unit,
    onMoveFallbackSlot: (index: Int, delta: Int) -> Unit,
    onResetDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingSlotIndex by remember { mutableStateOf<Int?>(null) }
    val normalizedPrimary = GeminiApiConfig.normalizeModelId(
        primaryModel,
        availableModels.map { it.id }.toSet()
    )
    val defaultFallbacks = remember(normalizedPrimary) {
        GeminiApiConfig.defaultFallbacksFor(normalizedPrimary)
    }
    val isDefaultOrder = fallbackModels == defaultFallbacks

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SwapVert,
                contentDescription = null,
                tint = TealPrimary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = "বিকল্প (Fallback) মডেল",
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
                color = SlateText
            )
            Surface(color = TealSoft, shape = RoundedCornerShape(5.dp)) {
                Text(
                    text = "${bn(fallbackModels.size.toString())}/৩",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TealDark,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            if (!isDefaultOrder) {
                Text(
                    text = "ডিফল্ট সেট",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TealPrimary,
                    modifier = Modifier
                        .clickable { onResetDefaults() }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .testTag("fallback_reset_defaults_btn")
                )
            }
        }

        Text(
            text = "প্রধান মডেল ব্যস্ত বা ত্রুটি দিলে ১→২→৩ ক্রমে চেষ্টা করবে",
            fontSize = 10.sp,
            color = SlateMuted,
            lineHeight = 13.sp
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (slotIdx in 0 until GeminiApiConfig.MAX_FALLBACK_MODELS) {
                val modelId = fallbackModels.getOrNull(slotIdx)
                val isFilled = !modelId.isNullOrBlank()
                val slotNumBn = bn((slotIdx + 1).toString())

                Surface(
                    color = if (isFilled) SlateBg else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isFilled) SlateOutline else SlateOutline.copy(alpha = 0.55f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp, end = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = if (isFilled) TealContainer else SlateBg,
                            shape = RoundedCornerShape(5.dp)
                        ) {
                            Text(
                                text = slotNumBn,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFilled) TealDark else SlateMuted,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .clickable { editingSlotIndex = slotIdx }
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                                .testTag("fallback_slot_select_$slotIdx"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = modelId ?: "+ বিকল্প মডেল $slotNumBn বাছুন",
                                fontSize = 11.5.sp,
                                fontWeight = if (isFilled) FontWeight.Medium else FontWeight.Normal,
                                color = if (isFilled) SlateText else SlateMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Rounded.ArrowDropDown,
                                contentDescription = "বিকল্প মডেল $slotNumBn নির্বাচন করুন",
                                tint = SlateMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (isFilled) {
                            IconButton(
                                onClick = { onMoveFallbackSlot(slotIdx, -1) },
                                enabled = slotIdx > 0,
                                modifier = Modifier
                                    .size(34.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("fallback_slot_up_$slotIdx")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = "বিকল্প $slotNumBn উপরে নিন",
                                    tint = if (slotIdx > 0) TealPrimary else SlateOutline,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onMoveFallbackSlot(slotIdx, +1) },
                                enabled = slotIdx < fallbackModels.lastIndex,
                                modifier = Modifier
                                    .size(34.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("fallback_slot_down_$slotIdx")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "বিকল্প $slotNumBn নিচে নিন",
                                    tint = if (slotIdx < fallbackModels.lastIndex) TealPrimary else SlateOutline,
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
        FallbackSlotPickerDialog(
            slotIndex = slotIdx,
            currentSlotModel = fallbackModels.getOrNull(slotIdx),
            primaryModel = normalizedPrimary,
            fallbackModels = fallbackModels,
            availableModels = availableModels,
            customModels = customModels,
            onPick = { chosen ->
                onSetFallbackSlot(slotIdx, chosen)
                editingSlotIndex = null
            },
            onDismiss = { editingSlotIndex = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FallbackSlotPickerDialog(
    slotIndex: Int,
    currentSlotModel: String?,
    primaryModel: String,
    fallbackModels: List<String>,
    availableModels: List<DiscoveredGeminiModel>,
    customModels: List<String>,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var customInput by remember(currentSlotModel) { mutableStateOf(currentSlotModel.orEmpty()) }
    var askClearSlot by remember { mutableStateOf(false) }
    val slotNumBn = bn((slotIndex + 1).toString())

    val suggestedCandidates = remember(primaryModel) {
        GeminiApiConfig.SUGGESTED_MODELS.filter { it.id != primaryModel }
    }
    val extraModelIds = remember(availableModels, customModels, primaryModel) {
        val suggestedIds = GeminiApiConfig.SUGGESTED_MODEL_IDS
        val merged = linkedSetOf<String>()
        merged.add("gemini-2.0-flash-lite")
        availableModels.forEach { if (it.id != primaryModel && it.id !in suggestedIds) merged.add(it.id) }
        customModels.forEach { if (it != primaryModel && it !in suggestedIds) merged.add(it) }
        merged.toList()
    }
    val filteredExtra = remember(extraModelIds, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) extraModelIds else extraModelIds.filter { it.lowercase().contains(q) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "বিকল্প মডেল $slotNumBn নির্বাচন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp
                )
                Text(
                    text = "প্রধান: $primaryModel",
                    fontSize = 11.sp,
                    color = SlateMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("প্রস্তাবিত বিকল্প", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SlateText)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    suggestedCandidates.forEach { preset ->
                        val isCurrentSlot = currentSlotModel == preset.id
                        val otherIdx = fallbackModels.indexOf(preset.id).takeIf { it >= 0 && it != slotIndex }
                        Surface(
                            color = if (isCurrentSlot) TealContainer.copy(alpha = 0.7f) else SlateBg,
                            border = BorderStroke(
                                if (isCurrentSlot) 1.5.dp else 1.dp,
                                if (isCurrentSlot) TealPrimary else SlateOutline
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(preset.id) }
                                .testTag("fallback_pick_${preset.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = preset.title,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = SlateText
                                    )
                                    Text(
                                        text = if (otherIdx != null) {
                                            "${preset.id} · স্লট ${bn((otherIdx + 1).toString())} থেকে অদলবদল"
                                        } else {
                                            preset.id
                                        },
                                        fontSize = 10.sp,
                                        color = if (otherIdx != null) TealDark else SlateMuted
                                    )
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

                if (extraModelIds.isNotEmpty()) {
                    HorizontalDivider(color = SlateOutline.copy(alpha = 0.7f))
                    Text("অন্যান্য / লাইভ মডেল", fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, color = SlateText)
                    if (extraModelIds.size > 5) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("খুঁজুন...", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(15.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "খোঁজ মুছুন", modifier = Modifier.size(14.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 11.5.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        filteredExtra.take(18).forEach { modelId ->
                            val selected = currentSlotModel == modelId
                            val otherIdx = fallbackModels.indexOf(modelId).takeIf { it >= 0 && it != slotIndex }
                            FilterChip(
                                selected = selected,
                                onClick = { onPick(modelId) },
                                colors = appFilterChipColors(),
                                border = appFilterChipBorder(selected),
                                label = {
                                    Text(
                                        text = if (otherIdx != null) "$modelId (#${bn((otherIdx + 1).toString())})" else modelId,
                                        fontSize = 10.5.sp
                                    )
                                }
                            )
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
                        label = { Text("কাস্টম মডেল আইডি", fontSize = 11.sp) },
                        placeholder = { Text("gemini-2.0-flash-lite", fontSize = 11.sp) },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 11.5.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val clean = customInput.trim().removePrefix("models/").trim()
                            if (clean.isNotBlank() && clean != primaryModel) {
                                onPick(clean)
                            }
                        },
                        enabled = customInput.isNotBlank() &&
                            customInput.trim().removePrefix("models/").trim() != primaryModel,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
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
                    modifier = Modifier.testTag("fallback_slot_clear_btn")
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
            title = "বিকল্প মডেল স্লট $slotNumBn খালি করবেন?",
            description = "\"$currentSlotModel\" মডেলটি বিকল্প স্লট $slotNumBn থেকে সরানো হবে।",
            warningDescription = "বিকল্প মডেল স্লটটি খালি করতে আপনার ফিঙ্গারপ্রিন্ট অনুমতি দিন।",
            confirmActionLabel = "ফিঙ্গারপ্রিন্ট অনুমতি দিয়ে সরান",
            confirmButtonTestTag = "fallback_confirm_clear_btn",
            onDismiss = { askClearSlot = false },
            onConfirmedDelete = { onPick(null) }
        )
    }
}
