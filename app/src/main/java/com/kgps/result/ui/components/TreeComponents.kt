package com.kgps.result.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealContainer
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft
import kotlin.math.abs

/**
 * Expandable Material 3 Tree Group Card with animated chevron, optional badge,
 * and indented vertical tree branch rail for children.
 */
@Composable
fun TreeGroupCard(
    title: String,
    subtitle: String? = null,
    badgeText: String? = null,
    badgeColor: Color = TealPrimary,
    badgeContainerColor: Color = TealSoft,
    icon: ImageVector? = null,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    headerTestTag: String? = null,
    headerTrailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "tree_chevron_rotation"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (expanded) TealPrimary.copy(alpha = 0.45f) else SlateOutline
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            val headerMod = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .then(if (headerTestTag != null) Modifier.testTag(headerTestTag) else Modifier)
                .clickable(onClick = onToggle)
                .background(if (expanded) TealSoft.copy(alpha = 0.45f) else Color.Transparent)
                .padding(horizontal = 10.dp, vertical = 7.dp)

            Row(
                modifier = headerMod,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    Surface(
                        color = if (expanded) TealContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (expanded) TealDark else SlateMuted,
                            modifier = Modifier.padding(6.dp).size(16.dp)
                        )
                    }
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        color = SlateText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = SlateMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (!badgeText.isNullOrBlank()) {
                    Surface(
                        color = badgeContainerColor,
                        shape = RoundedCornerShape(7.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            maxLines = 1
                        )
                    }
                }

                if (headerTrailing != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        content = headerTrailing
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (expanded) "গুটিয়ে রাখুন" else "বিস্তারিত খুলুন",
                    tint = SlateMuted,
                    modifier = Modifier.size(20.dp).rotate(rotation)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(220)) + fadeIn(tween(180)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(140))
            ) {
                Column(Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = SlateOutline.copy(alpha = 0.7f))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 5.dp, end = 7.dp, top = 6.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        content = content
                    )
                }
            }
        }
    }
}

/**
 * Floating sticky group header bar that appears only when an expanded group's real header
 * scrolls above the viewport top edge.
 */
@Composable
fun FloatingGroupStickyBar(
    visible: Boolean,
    title: String,
    badgeText: String? = null,
    icon: ImageVector? = null,
    actionLabel: String = "বন্ধ করুন",
    testTag: String? = null,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(180)) { -it } + fadeIn(tween(150)),
        exit = slideOutVertically(tween(150)) { -it } + fadeOut(tween(130)),
        modifier = modifier
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 6.dp,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.2.dp, TealPrimary.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .clickable(onClick = onAction)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TealSoft.copy(alpha = 0.55f))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    Surface(
                        color = TealContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = TealDark,
                            modifier = Modifier.padding(6.dp).size(16.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = SlateText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!badgeText.isNullOrBlank()) {
                    Surface(
                        color = Color.White,
                        border = BorderStroke(0.8.dp, TealPrimary.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(7.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TealDark,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
                Surface(
                    color = TealPrimary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = actionLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowUp,
                            contentDescription = actionLabel,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

data class TrackedGroupInfo(
    val key: String,
    val title: String,
    val badgeText: String?,
    val icon: ImageVector,
    val expanded: Boolean,
    val topInContent: Float,
    val heightPx: Float,
    val onCollapse: () -> Unit
)

@Composable
fun TrackedGroupBox(
    key: String,
    title: String,
    badgeText: String?,
    icon: ImageVector,
    expanded: Boolean,
    contentRootY: () -> Float,
    trackedGroups: MutableMap<String, TrackedGroupInfo>?,
    onCollapse: () -> Unit,
    content: @Composable () -> Unit
) {
    if (trackedGroups == null) {
        content()
        return
    }
    var lastTop by remember(key) { mutableFloatStateOf(0f) }
    var lastHeight by remember(key) { mutableFloatStateOf(0f) }

    fun syncEntry(top: Float, height: Float) {
        val next = TrackedGroupInfo(
            key = key,
            title = title,
            badgeText = badgeText,
            icon = icon,
            expanded = expanded,
            topInContent = top,
            heightPx = height,
            onCollapse = onCollapse
        )
        val prev = trackedGroups[key]
        if (prev == null ||
            prev.expanded != next.expanded ||
            prev.title != next.title ||
            prev.badgeText != next.badgeText ||
            abs(prev.topInContent - next.topInContent) > 1f ||
            abs(prev.heightPx - next.heightPx) > 1f
        ) {
            trackedGroups[key] = next
        }
    }

    SideEffect {
        if (lastHeight > 0f) {
            syncEntry(lastTop, lastHeight)
        }
    }

    DisposableEffect(key) {
        onDispose {
            trackedGroups.remove(key)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                val top = coords.positionInRoot().y - contentRootY()
                val height = coords.size.height.toFloat()
                lastTop = top
                lastHeight = height
                syncEntry(top, height)
            }
    ) {
        content()
    }
}

/**
 * Tree branch wrapper drawing a clean left vertical guide + horizontal elbow connector
 * beside each child node in a grouped tree.
 */
@Composable
fun TreeBranchItem(
    isLast: Boolean = false,
    lineColor: Color = SlateOutline,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val railW = 8.dp.toPx()
                val x = railW * 0.4f
                val midY = size.height * 0.5f
                val endY = if (isLast) midY else size.height
                drawLine(
                    color = lineColor,
                    start = Offset(x, 0f),
                    end = Offset(x, endY),
                    strokeWidth = 2f
                )
                drawLine(
                    color = lineColor,
                    start = Offset(x, midY),
                    end = Offset(railW - 1f, midY),
                    strokeWidth = 2f
                )
            }
            .padding(start = 8.dp)
    ) {
        content()
    }
}
