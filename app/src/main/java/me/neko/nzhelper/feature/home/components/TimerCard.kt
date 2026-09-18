package me.neko.nzhelper.feature.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.neko.nzhelper.core.util.formatTime
import me.neko.nzhelper.feature.statistics.model.LatestSessionInfo
import me.neko.nzhelper.ui.component.setting.TrailingArrowIcon

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun TimerCard(
    elapsedSeconds: Int,
    isRunning: Boolean,
    onToggleRun: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    latestInfo: LatestSessionInfo? = null,
    isLoading: Boolean = false,
    floatingEnabled: Boolean = false,
    onToggleFloating: () -> Unit = {},
    onOpenHistory: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val isPaused = !isRunning && elapsedSeconds > 0

    val timeColor by animateColorAsState(
        targetValue = when {
            isRunning -> colorScheme.primary
            isPaused -> colorScheme.onSurface
            else -> colorScheme.onSurfaceVariant
        },
        animationSpec = tween(800), label = "timerTimeColor"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceBright
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimerStatusPill(isRunning = isRunning, isPaused = isPaused)
                Spacer(Modifier.weight(1f))
                FloatingWindowChip(
                    enabled = floatingEnabled,
                    onClick = onToggleFloating
                )
            }

            Spacer(Modifier.height(12.dp))

            FlipClockText(
                text = formatTime(elapsedSeconds),
                color = timeColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            Spacer(Modifier.height(12.dp))

            if (isLoading) {
                LastSessionPlaceholder()
            } else if (latestInfo != null) {
                LastSessionRow(
                    info = latestInfo,
                    onClick = onOpenHistory
                )
            }

            Spacer(Modifier.height(16.dp))

            TimerActionGroup(
                isRunning = isRunning,
                onToggleRun = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleRun()
                },
                onStop = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onStop()
                },
                onReset = onReset
            )
        }
    }
}

@Composable
private fun TimerStatusPill(
    isRunning: Boolean,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    val containerColor by animateColorAsState(
        targetValue = when {
            isRunning -> colorScheme.primaryContainer
            isPaused -> colorScheme.tertiaryContainer
            else -> colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(500), label = "statusContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            isRunning -> colorScheme.onPrimaryContainer
            isPaused -> colorScheme.onTertiaryContainer
            else -> colorScheme.onSurfaceVariant
        },
        animationSpec = tween(500), label = "statusContent"
    )

    val pulseScale: Float
    val pulseAlpha: Float
    if (isRunning) {
        val transition = rememberInfiniteTransition(label = "timerPulse")
        pulseScale = transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
            label = "timerPulseScale"
        ).value
        pulseAlpha = transition.animateFloat(
            initialValue = 0.8f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
            label = "timerPulseAlpha"
        ).value
    } else {
        pulseScale = 1f
        pulseAlpha = if (isPaused) 0.8f else 0.5f
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(contentColor.copy(alpha = pulseAlpha))
        )
        Crossfade(
            targetState = when {
                isRunning -> "进行中"
                isPaused -> "已暂停"
                else -> "准备开始"
            },
            animationSpec = tween(300),
            label = "statusText"
        ) { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun FloatingWindowChip(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = if (enabled) {
            colorScheme.primaryContainer
        } else {
            colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(400), label = "floatingChipContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (enabled) {
            colorScheme.onPrimaryContainer
        } else {
            colorScheme.onSurfaceVariant
        },
        animationSpec = tween(400), label = "floatingChipContent"
    )

    Box(
        modifier = modifier.minimumInteractiveComponentSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(containerColor)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.PictureInPictureAlt,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "悬浮窗",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun LastSessionPlaceholder(modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(colorScheme.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            strokeWidth = 2.dp,
            color = colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "加载中…",
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LastSessionRow(
    info: LatestSessionInfo,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val daysAgoText = when (info.daysAgo) {
        0L -> "今天"
        1L -> "昨天"
        else -> "${info.daysAgo} 天前"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(colorScheme.surfaceContainerHigh)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "上次记录",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = daysAgoText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.primary
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${info.displayDate} ${info.time} · ${info.durationText}",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onClick != null) {
            TrailingArrowIcon()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
private fun TimerActionGroup(
    isRunning: Boolean,
    onToggleRun: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    val actionHeight = 52.dp

    val resetColors = ToggleButtonDefaults.colors(
        containerColor = colorScheme.surfaceContainerHigh,
        contentColor = colorScheme.onSurfaceVariant,
        checkedContainerColor = colorScheme.surfaceContainerHigh,
        checkedContentColor = colorScheme.onSurfaceVariant
    )
    val stopColors = ToggleButtonDefaults.colors(
        containerColor = colorScheme.errorContainer,
        contentColor = colorScheme.onErrorContainer,
        checkedContainerColor = colorScheme.errorContainer,
        checkedContentColor = colorScheme.onErrorContainer
    )
    val runColors = ToggleButtonDefaults.colors(
        containerColor = if (isRunning) colorScheme.tertiary else colorScheme.primary,
        contentColor = if (isRunning) colorScheme.onTertiary else colorScheme.onPrimary,
        checkedContainerColor = if (isRunning) colorScheme.tertiary else colorScheme.primary,
        checkedContentColor = if (isRunning) colorScheme.onTertiary else colorScheme.onPrimary
    )

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ToggleButton(
            checked = false,
            onCheckedChange = { onReset() },
            modifier = Modifier.height(actionHeight),
            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(
                checkedShape = ButtonGroupDefaults.connectedLeadingButtonShape
            ),
            colors = resetColors,
        ) {
            Icon(Icons.Rounded.Replay, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
            Text("重置")
        }
        ToggleButton(
            checked = false,
            onCheckedChange = { onToggleRun() },
            modifier = Modifier
                .weight(1f)
                .height(actionHeight),
            shapes = ButtonGroupDefaults.connectedMiddleButtonShapes(
                shape = ButtonGroupDefaults.connectedButtonCheckedShape
            ),
            colors = runColors,
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
            Text(if (isRunning) "暂停" else "开始")
        }
        ToggleButton(
            checked = false,
            onCheckedChange = { onStop() },
            modifier = Modifier.height(actionHeight),
            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(
                checkedShape = ButtonGroupDefaults.connectedTrailingButtonShape
            ),
            colors = stopColors,
        ) {
            Icon(Icons.Rounded.Stop, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
            Text("结束")
        }
    }
}

@Composable
private fun FlipClockText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val charStyle = MaterialTheme.typography.displayMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        fontFeatureSettings = "tnum"
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        text.forEach { ch ->
            if (ch.isDigit()) {
                AnimatedContent(
                    targetState = ch,
                    transitionSpec = {
                        (slideInVertically { it } + fadeIn()) togetherWith
                                (slideOutVertically { -it } + fadeOut())
                    },
                    label = "flipDigit"
                ) { digit ->
                    Text(
                        text = digit.toString(),
                        style = charStyle,
                        color = color,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = ch.toString(),
                    style = charStyle,
                    color = color.copy(alpha = color.alpha * 0.55f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}