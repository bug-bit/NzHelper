package me.neko.nzhelper.ui.component.setting

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.ui.theme.LocalDarkMode

val SettingsCornerRadius = 16.dp
val SettingsConnectionRadius = 5.dp
private val GroupGap = 2.dp

data class SettingsItemCorners(val topRadius: Dp, val bottomRadius: Dp)

val LocalSettingsItemCorners = compositionLocalOf {
    SettingsItemCorners(SettingsCornerRadius, SettingsCornerRadius)
}

@DslMarker
annotation class SettingsCardDsl

@SettingsCardDsl
class SettingsCardScope {
    internal val items = mutableListOf<@Composable () -> Unit>()

    fun item(content: @Composable () -> Unit) {
        items.add(content)
    }
}

@Composable
fun TrailingArrowIcon() {
    val tint = if (LocalDarkMode.current) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
    }
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(24.dp)
    )
}

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: SettingsCardScope.() -> Unit
) {
    val scope = SettingsCardScope().apply(content)
    if (scope.items.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
        }
        scope.items.forEachIndexed { index, itemContent ->
            val isFirst = index == 0
            val isLast = index == scope.items.lastIndex
            val corners = SettingsItemCorners(
                topRadius = if (isFirst) SettingsCornerRadius else SettingsConnectionRadius,
                bottomRadius = if (isLast) SettingsCornerRadius else SettingsConnectionRadius
            )
            CompositionLocalProvider(LocalSettingsItemCorners provides corners) {
                itemContent()
            }
            if (!isLast) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(GroupGap)
                )
            }
        }
    }
}

@Composable
fun SettingsItemSurface(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceBright,
    content: @Composable ColumnScope.() -> Unit
) {
    val corners = LocalSettingsItemCorners.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = corners.topRadius,
                    topEnd = corners.topRadius,
                    bottomEnd = corners.bottomRadius,
                    bottomStart = corners.bottomRadius
                )
            )
            .background(containerColor),
        content = content
    )
}

@Composable
fun SettingsItemHost(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val corners = LocalSettingsItemCorners.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val topRadius by animateDpAsState(
        targetValue = if (pressed) SettingsCornerRadius else corners.topRadius,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "settingsItemHostTopRadius"
    )
    val bottomRadius by animateDpAsState(
        targetValue = if (pressed) SettingsCornerRadius else corners.bottomRadius,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "settingsItemHostBottomRadius"
    )
    val shape = RoundedCornerShape(
        topStart = topRadius,
        topEnd = topRadius,
        bottomEnd = bottomRadius,
        bottomStart = bottomRadius
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceBright)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        enabled = enabled,
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            onClick()
                        }
                    )
                } else {
                    Modifier
                }
            ),
        content = content
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enabled: Boolean = true,
    selected: Boolean = false,
    badgeText: String? = null,
    iconTint: Color? = null,
    iconSize: Dp = 24.dp,
    leadingModifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val contentAlpha = if (enabled) 1f else 0.38f
    val dynamicInternalPadding = (6 * LocalDensity.current.fontScale).dp
    val corners = LocalSettingsItemCorners.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val topRadius by animateDpAsState(
        targetValue = if (pressed) SettingsCornerRadius else corners.topRadius,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "settingsItemTopRadius"
    )
    val bottomRadius by animateDpAsState(
        targetValue = if (pressed) SettingsCornerRadius else corners.bottomRadius,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "settingsItemBottomRadius"
    )
    val shape = RoundedCornerShape(
        topStart = topRadius,
        topEnd = topRadius,
        bottomEnd = bottomRadius,
        bottomStart = bottomRadius
    )

    val baseContentColor = if (selected) {
        MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.primaryContainer)
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceBright
    }
    val defaultIconColor = if (LocalDarkMode.current) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
    }
    val resolvedIconColor = if (selected) {
        baseContentColor
    } else {
        defaultIconColor
    }
    val effectiveIconColor = iconTint ?: resolvedIconColor
    val effectiveTitleColor = if (selected) baseContentColor else titleColor
    val effectiveSubtitleColor =
        if (selected) baseContentColor.copy(alpha = 0.7f) else subtitleColor

    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    onClick()
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .then(leadingModifier),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = effectiveIconColor,
                    modifier = Modifier.size(iconSize)
                )
            }
        },
        trailingContent = {
            if (trailingContent != null) {
                trailingContent()
            } else {
                TrailingArrowIcon()
            }
        },
        overlineContent = null,
        supportingContent = null,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        elevation = ListItemDefaults.elevation(),
        content = {
            Column {
                Box(
                    modifier = Modifier.padding(
                        top = dynamicInternalPadding,
                        bottom = if (subtitle == null) dynamicInternalPadding else 0.dp
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Normal
                            ),
                            color = effectiveTitleColor.copy(alpha = contentAlpha)
                        )
                        badgeText?.let {
                            Badge { Text(it) }
                        }
                    }
                }
                subtitle?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = effectiveSubtitleColor.copy(alpha = contentAlpha),
                        modifier = Modifier.padding(bottom = dynamicInternalPadding)
                    )
                }
            }
        },
    )
}