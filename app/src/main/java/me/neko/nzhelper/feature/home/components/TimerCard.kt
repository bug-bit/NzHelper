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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.neko.nzhelper.core.util.formatTime
import kotlin.time.Duration.Companion.milliseconds

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
    val surface = MaterialTheme.colorScheme.surfaceContainerLowest
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val tertiary = MaterialTheme.colorScheme.tertiary

    val bgTop by animateColorAsState(
        targetValue = if (isRunning) primary else surface,
        animationSpec = tween(800), label = "bgTop"
    )
    val bgBottom by animateColorAsState(
        targetValue = if (isRunning) primary.copy(alpha = 0.82f) else surface,
        animationSpec = tween(800), label = "bgBottom"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isRunning) onPrimary else onSurface,
        animationSpec = tween(800), label = "content"
    )
    val subtleColor by animateColorAsState(
        targetValue = if (isRunning) onPrimary.copy(alpha = 0.8f) else onSurfaceVariant,
        animationSpec = tween(800), label = "subtle"
    )

    val isPaused = !isRunning && elapsedSeconds > 0
    val dotColor by animateColorAsState(
        targetValue = when {
            isRunning -> onPrimary
            isPaused -> tertiary
            else -> onSurfaceVariant
        },
        animationSpec = tween(600), label = "dotColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseAlpha"
    )
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 2.4f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
        label = "rippleScale"
    )
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
        label = "rippleAlpha"
    )

    val dotScale by animateFloatAsState(
        targetValue = if (isRunning) pulseScale else 0.85f,
        animationSpec = tween(300), label = "dotScale"
    )
    val dotAlpha by animateFloatAsState(
        targetValue = if (isRunning) pulseAlpha else 0.5f,
        animationSpec = tween(300), label = "dotAlpha"
    )
    val rippleShownAlpha by animateFloatAsState(
        targetValue = if (isRunning) 1f else 0f,
        animationSpec = tween(300), label = "rippleShown"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isRunning) 1f else 0f,
        animationSpec = tween(500), label = "glow"
    )

    val sideBtnContainer by animateColorAsState(
        targetValue = if (isRunning) onPrimary.copy(alpha = 0.18f)
        else MaterialTheme.colorScheme.tertiaryContainer,
        animationSpec = tween(500), label = "sideBg"
    )
    val sideBtnContent by animateColorAsState(
        targetValue = if (isRunning) onPrimary
        else MaterialTheme.colorScheme.onTertiaryContainer,
        animationSpec = tween(500), label = "sideFg"
    )
    val stopBtnContainer by animateColorAsState(
        targetValue = if (isRunning) onPrimary.copy(alpha = 0.18f)
        else MaterialTheme.colorScheme.errorContainer,
        animationSpec = tween(500), label = "stopBg"
    )
    val stopBtnContent by animateColorAsState(
        targetValue = if (isRunning) onPrimary
        else MaterialTheme.colorScheme.onErrorContainer,
        animationSpec = tween(500), label = "stopFg"
    )

    val statusText = when {
        isRunning -> "进行中…"
        isPaused -> "已暂停"
        else -> "准备开始"
    }

    var pressed by remember { mutableStateOf(false) }
    val buttonScale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(120), label = "buttonScale"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (rippleShownAlpha > 0.01f) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .scale(rippleScale)
                                .alpha(rippleAlpha * rippleShownAlpha)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
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
                                color = subtleColor
                            )
                        }
                    }
                }

                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    if (glowAlpha > 0.01f) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .alpha(glowAlpha * 0.6f)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            contentColor.copy(alpha = 0.18f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                    FlipClockText(
                        text = formatTime(elapsedSeconds),
                        color = contentColor
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = onReset,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = sideBtnContainer,
                            contentColor = sideBtnContent
                        )
                    ) {
                        Icon(Icons.Rounded.Replay, "重置", modifier = Modifier.size(24.dp))
                    }

                    FilledTonalButton(
                        onClick = { pressed = true; onToggleRun() },
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .scale(buttonScale),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isRunning) onPrimary
                            else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (isRunning) primary
                            else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Crossfade(
                            targetState = isRunning,
                            animationSpec = tween(200),
                            label = "btnIcon"
                        ) { running ->
                            Icon(
                                if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                if (running) "暂停" else "开始",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Crossfade(
                            targetState = isRunning,
                            animationSpec = tween(200),
                            label = "btnText"
                        ) { running ->
                            Text(
                                if (running) "暂停" else "开始",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    FilledIconButton(
                        onClick = onStop,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = stopBtnContainer,
                            contentColor = stopBtnContent
                        )
                    ) {
                        Icon(Icons.Rounded.Stop, "结束", modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }

    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(120.milliseconds)
            pressed = false
        }
    }
}

@Composable
private fun FlipClockText(
    text: String,
    color: Color,
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