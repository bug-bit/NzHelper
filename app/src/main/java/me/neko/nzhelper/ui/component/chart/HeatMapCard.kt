package me.neko.nzhelper.ui.component.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.feature.statistics.model.HeatmapData
import me.neko.nzhelper.feature.statistics.util.calculateHeatmapData
import java.time.LocalDateTime

@Composable
fun HeatMapCard(
    sessions: List<Session>,
    currentTime: LocalDateTime,
    modifier: Modifier = Modifier
) {
    val heatmapData by remember(sessions, currentTime) {
        derivedStateOf { calculateHeatmapData(sessions, currentTime) }
    }

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
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
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "活动热力图",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "最近 ${heatmapData.weekCount} 周",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            HeatMapGrid(data = heatmapData)

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "少",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                val primary = MaterialTheme.colorScheme.primary
                val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHighest
                listOf(
                    surfaceColor,
                    primary.copy(alpha = 0.25f),
                    primary.copy(alpha = 0.5f),
                    primary.copy(alpha = 0.75f),
                    primary
                ).forEach { color ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 1.dp)
                            .size(11.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(color)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    "多",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "活跃 ${heatmapData.activeDays} / ${heatmapData.totalDays} 天",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (heatmapData.maxCount > 0) {
                    Text(
                        "单日最高 ${heatmapData.maxCount} 次",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatMapGrid(
    data: HeatmapData,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val futureColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val levels = listOf(
        surfaceColor,
        primary.copy(alpha = 0.25f),
        primary.copy(alpha = 0.5f),
        primary.copy(alpha = 0.75f),
        primary
    )

    val animatedProgress = remember(data) { Animatable(0f) }
    LaunchedEffect(data) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
    }

    val cellSize = 13.dp
    val cellGap = 3.dp
    val labelWidth = 22.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.padding(start = labelWidth)) {
            data.monthLabels.forEach { (weekIndex, label) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(
                        start = (cellSize + cellGap) * weekIndex
                    )
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.width(labelWidth),
                verticalArrangement = Arrangement.spacedBy(cellGap)
            ) {
                listOf("一", "", "三", "", "五", "", "日").forEach { label ->
                    Box(modifier = Modifier.size(cellSize)) {
                        if (label.isNotEmpty()) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxSize(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(cellGap)
            ) {
                data.weeks.forEach { week ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(cellGap)
                    ) {
                        week.days.forEach { day ->
                            val level = when {
                                day.isFuture -> 5
                                day.count == 0 -> 0
                                data.maxCount == 0 -> 0
                                else -> {
                                    val ratio = day.count.toFloat() / data.maxCount
                                    when {
                                        ratio <= 0.25f -> 1
                                        ratio <= 0.5f -> 2
                                        ratio <= 0.75f -> 3
                                        else -> 4
                                    }
                                }
                            }

                            val cellColor = when (level) {
                                5 -> futureColor
                                0 -> levels[0]
                                else -> levels[level]
                            }

                            val alpha = when (level) {
                                0, 5 -> 1f
                                else -> animatedProgress.value
                            }

                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .background(cellColor.copy(alpha = alpha))
                            )
                        }
                    }
                }
            }
        }
    }
}