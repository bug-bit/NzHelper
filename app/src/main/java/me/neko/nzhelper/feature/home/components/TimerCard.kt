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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.neko.nzhelper.core.util.formatTime

@Composable
fun TimerCard(
    elapsedSeconds: Int,
    isRunning: Boolean,
    onToggleRun: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
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
        initialValue = 1f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    val dotScale by animateFloatAsState(
        targetValue = if (isRunning) pulseScale else 1f,
        animationSpec = tween(300), label = "dotScale"
    )
    val dotAlpha by animateFloatAsState(
        targetValue = if (isRunning) pulseAlpha else if (isPaused) 0.8f else 0.4f,
        animationSpec = tween(300), label = "dotAlpha"
    )

    val statusText = when {
        isRunning -> "进行中…"
        isPaused -> "已暂停"
        else -> "准备开始"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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

        Spacer(Modifier.height(8.dp))

        FlipClockText(
            text = formatTime(elapsedSeconds),
            color = timeColor
        )

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = onReset,
                shape = CircleShape,
                modifier = Modifier.size(52.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Rounded.Replay, "重置", modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(20.dp))

            LargeFloatingActionButton(
                onClick = onToggleRun,
                shape = CircleShape,
                containerColor = if (isRunning) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    primary
                },
                contentColor = if (isRunning) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    onPrimary
                }
            ) {
                Crossfade(
                    targetState = isRunning,
                    animationSpec = tween(200),
                    label = "fabIcon"
                ) { running ->
                    Icon(
                        if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (running) "暂停" else "开始",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(Modifier.width(20.dp))

            FilledIconButton(
                onClick = onStop,
                shape = CircleShape,
                modifier = Modifier.size(52.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Rounded.Stop, "结束", modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun FlipClockText(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val charStyle = MaterialTheme.typography.displayLarge.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
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