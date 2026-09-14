package me.neko.nzhelper.feature.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.neko.nzhelper.core.util.formatTime
import me.neko.nzhelper.feature.statistics.model.LatestSessionInfo

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun TimerCard(
    elapsedSeconds: Int,
    isRunning: Boolean,
    latestInfo: LatestSessionInfo? = null,
    isLoading: Boolean = false,
    floatingEnabled: Boolean = false,
    onToggleRun: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onToggleFloating: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val tertiary = MaterialTheme.colorScheme.tertiary

    val isPaused = !isRunning && elapsedSeconds > 0

    val dotColor by animateColorAsState(
        targetValue = when {
            isRunning -> primary
            isPaused -> tertiary
            else -> onSurfaceVariant
        },
        animationSpec = tween(600), label = "dotColor"
    )

    val statusColor by animateColorAsState(
        targetValue = when {
            isRunning -> primary
            isPaused -> tertiary
            else -> onSurfaceVariant
        },
        animationSpec = tween(600), label = "statusColor"
    )

    val timeColor by animateColorAsState(
        targetValue = when {
            isRunning -> primary
            isPaused -> onSurface
            else -> onSurface.copy(alpha = 0.7f)
        },
        animationSpec = tween(800), label = "timeColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    val dotScale by animateFloatAsState(
        targetValue = if (isRunning) pulseScale else 1f,
        animationSpec = tween(300), label = "dotScale"
    )
    val dotAlpha by animateFloatAsState(
        targetValue = if (isRunning) pulseAlpha else if (isPaused) 0.7f else 0.4f,
        animationSpec = tween(300), label = "dotAlpha"
    )

    val statusText = when {
        isRunning -> "进行中…"
        isPaused -> "已暂停"
        else -> "准备开始"
    }

    var selectedAction by remember { mutableIntStateOf(1) }

    val resetToggleColors = ToggleButtonDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        contentColor = onSurfaceVariant,
        checkedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        checkedContentColor = onSurfaceVariant
    )
    val stopToggleColors = ToggleButtonDefaults.colors(
        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        contentColor = MaterialTheme.colorScheme.error,
        checkedContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        checkedContentColor = MaterialTheme.colorScheme.error
    )
    val runToggleColors = ToggleButtonDefaults.colors(
        containerColor = if (isRunning) MaterialTheme.colorScheme.tertiary
        else MaterialTheme.colorScheme.primary,
        contentColor = if (isRunning) MaterialTheme.colorScheme.onTertiary
        else MaterialTheme.colorScheme.onPrimary,
        checkedContainerColor = if (isRunning) MaterialTheme.colorScheme.tertiary
        else MaterialTheme.colorScheme.primary,
        checkedContentColor = if (isRunning) MaterialTheme.colorScheme.onTertiary
        else MaterialTheme.colorScheme.onPrimary
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 22.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isRunning) 10.dp else 8.dp)
                            .scale(dotScale)
                            .clip(CircleShape)
                            .background(dotColor.copy(alpha = dotAlpha))
                    )
                    Crossfade(
                        targetState = statusText,
                        animationSpec = tween(300),
                        label = "statusText"
                    ) { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            FlipClockText(
                text = formatTime(elapsedSeconds),
                color = timeColor,
                modifier = Modifier.fillMaxWidth()
            )

            if (isLoading || latestInfo != null) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "加载中...",
                            style = MaterialTheme.typography.labelMedium,
                            color = onSurfaceVariant
                        )
                    } else if (latestInfo != null) {
                        Text(
                            "距上次 · ${latestInfo.displayDate}",
                            style = MaterialTheme.typography.labelMedium,
                            color = onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        val (daysText, daysUnit) = when (latestInfo.daysAgo) {
                            0L -> "今天" to ""
                            1L -> "昨天" to ""
                            else -> latestInfo.daysAgo.toString() to " 天前"
                        }
                        Text(
                            "$daysText$daysUnit",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                    .clickable { onToggleFloating() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "计时悬浮窗",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (floatingEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        onSurfaceVariant
                    },
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier.size(width = 38.dp, height = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Switch(
                        checked = floatingEnabled,
                        onCheckedChange = null,
                        modifier = Modifier.scale(0.6f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                ToggleButton(
                    checked = selectedAction == 0,
                    onCheckedChange = {
                        selectedAction = 0
                        onReset()
                    },
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(
                        checkedShape = ButtonGroupDefaults.connectedLeadingButtonShape
                    ),
                    colors = resetToggleColors,
                ) {
                    Icon(Icons.Rounded.Replay, "重置", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                    Text("重置")
                }
                ToggleButton(
                    checked = selectedAction == 1,
                    onCheckedChange = {
                        selectedAction = 1
                        onToggleRun()
                    },
                    shapes = ButtonGroupDefaults.connectedMiddleButtonShapes(
                        shape = ButtonGroupDefaults.connectedButtonCheckedShape
                    ),
                    modifier = Modifier.weight(1f),
                    colors = runToggleColors,
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Rounded.Pause
                        else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                    Text(if (isRunning) "暂停" else "开始")
                }
                ToggleButton(
                    checked = selectedAction == 2,
                    onCheckedChange = {
                        selectedAction = 2
                        onStop()
                    },
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(
                        checkedShape = ButtonGroupDefaults.connectedTrailingButtonShape
                    ),
                    colors = stopToggleColors,
                ) {
                    Icon(Icons.Rounded.Stop, "结束", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                    Text("结束")
                }
            }
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
                    label = "flip_$ch"
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
                    color = color,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}