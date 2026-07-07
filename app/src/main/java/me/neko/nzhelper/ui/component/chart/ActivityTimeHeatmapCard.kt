package me.neko.nzhelper.ui.component.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.feature.statistics.model.ActivityTimeData
import me.neko.nzhelper.feature.statistics.util.calculateActivityTimeData
import me.neko.nzhelper.feature.statistics.util.weekdayLabel
import java.time.LocalDateTime

@Composable
fun ActivityTimeHeatmapCard(
    sessions: List<Session>,
    currentTime: LocalDateTime,
    modifier: Modifier = Modifier
) {
    val data by remember(sessions, currentTime) {
        derivedStateOf { calculateActivityTimeData(sessions, currentTime) }
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "活跃时间热力图",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "看看自己最活跃的时间",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            WeekdayBars(
                data = data,
                progress = progress.value
            )

            Spacer(Modifier.height(20.dp))

            HourBars(
                data = data,
                progress = progress.value
            )

            Spacer(Modifier.height(14.dp))

            val wdText = if (data.mostActiveWeekdayIndex in 0..6) {
                "最活跃：${weekdayLabel(data.mostActiveWeekdayIndex)} · ${data.weekdayCounts[data.mostActiveWeekdayIndex]} 次"
            } else "暂无数据"
            val hrText = if (data.mostActiveHour in 0..23) {
                "最活跃时段：${data.mostActiveHour}:00 · ${data.hourCounts[data.mostActiveHour]} 次"
            } else null
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    wdText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (hrText != null) {
                    Text(
                        hrText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekdayBars(
    data: ActivityTimeData,
    progress: Float
) {
    val primary = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val safeMax = data.weekdayMax.coerceAtLeast(1)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "按星期",
            style = MaterialTheme.typography.labelMedium,
            color = onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        data.weekdayCounts.forEachIndexed { index, count ->
            val ratio = count.toFloat() / safeMax
            val isMax = index == data.mostActiveWeekdayIndex && count > 0
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = weekdayLabel(index),
                    style = MaterialTheme.typography.labelMedium,
                    color = onSurfaceVariant,
                    modifier = Modifier.width(36.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(trackColor)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width * ratio * progress
                        if (w > 0f) {
                            drawRoundRect(
                                color = if (isMax) primary else primary.copy(alpha = 0.6f),
                                size = Size(w, size.height),
                                cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                            )
                        }
                    }
                }
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isMax) primary else onSurfaceVariant,
                    fontWeight = if (isMax) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(28.dp)
                )
            }
        }
    }
}

@Composable
private fun HourBars(
    data: ActivityTimeData,
    progress: Float
) {
    val primary = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val safeMax = data.hourMax.coerceAtLeast(1)
    val density = LocalDensity.current
    val labelColor = onSurfaceVariant.copy(alpha = 0.6f)

    Column {
        Text(
            "按时段",
            style = MaterialTheme.typography.labelMedium,
            color = onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            for (h in 0 until 24) {
                val count = data.hourCounts[h]
                val ratio = count.toFloat() / safeMax
                val isMax = h == data.mostActiveHour && count > 0
                val isPeak = count > 0 && ratio >= 0.6f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(ratio.coerceAtLeast(if (count > 0) 0.04f else 0f) * progress)
                    ) {
                        if (count > 0) {
                            drawRoundRect(
                                color = when {
                                    isMax -> primary
                                    isPeak -> primary.copy(alpha = 0.7f)
                                    else -> primary.copy(alpha = 0.45f)
                                },
                                size = size,
                                cornerRadius = CornerRadius(
                                    with(density) { 2.dp.toPx() },
                                    with(density) { 2.dp.toPx() }
                                )
                            )
                        } else {
                            drawRoundRect(
                                color = trackColor,
                                size = Size(size.width, with(density) { 2.dp.toPx() }),
                                topLeft = Offset(0f, size.height - with(density) { 2.dp.toPx() }),
                                cornerRadius = CornerRadius(1f, 1f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (h in 0 until 24) {
                Text(
                    text = if (h % 3 == 0) h.toString() else "",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = labelColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
