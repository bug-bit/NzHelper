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
import androidx.compose.material.icons.automirrored.outlined.ShowChart
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.feature.statistics.util.calculateTrendData
import me.neko.nzhelper.feature.statistics.util.formatDuration
import java.time.LocalDateTime

@Composable
fun TrendChartCard(
    sessions: List<Session>,
    currentTime: LocalDateTime,
    modifier: Modifier = Modifier
) {
    val trendData by remember(sessions, currentTime) {
        derivedStateOf { calculateTrendData(sessions, currentTime) }
    }

    val totalMinutes = trendData.sumOf { it.second.toDouble() }.toFloat()
    val avgMinutes = if (trendData.isNotEmpty()) totalMinutes / trendData.size else 0f
    val rawMax = trendData.maxOfOrNull { it.second } ?: 0f

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
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ShowChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "趋势分析",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "最近 12 周时长变化",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        "周均时长",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "%.1f 分钟".format(avgMinutes),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "12周总计",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        formatDuration((totalMinutes * 60).toInt()),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            LineChart(data = trendData, rawMax = rawMax)
        }
    }
}

@Composable
private fun LineChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 160.dp,
    rawMax: Float = 0f
) {
    if (data.isEmpty() || data.all { it.second <= 0f }) {
        Box(
            modifier = modifier
                .height(chartHeight)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "暂无数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxValue = (rawMax * 1.2f).coerceAtLeast(1f)
    val primary = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val labelStyle = MaterialTheme.typography.labelSmall

    val animatedProgress = remember(data) { Animatable(0f) }
    LaunchedEffect(data) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
        ) {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .fillMaxHeight()
            ) {
                Text(
                    "${maxValue.toInt()}",
                    style = labelStyle,
                    color = onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.TopEnd)
                )
                Text(
                    "${(maxValue / 2).toInt()}",
                    style = labelStyle,
                    color = onSurfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
                Text(
                    "0",
                    style = labelStyle,
                    color = onSurfaceVariant.copy(alpha = 0.25f),
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }

            Box(
                modifier = Modifier
                    .padding(start = 36.dp)
                    .fillMaxSize()
                    .drawBehind {
                        val gridCount = 4
                        for (i in 0..gridCount) {
                            val y = size.height * i / gridCount
                            drawLine(
                                color = outlineVariant.copy(alpha = 0.2f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val stepX = if (data.size > 1) w / (data.size - 1) else w

                    val points = data.mapIndexed { index, (_, value) ->
                        val x = index * stepX
                        val y = h - (value / maxValue) * h * animatedProgress.value
                        Offset(x, y)
                    }

                    if (points.size >= 2) {
                        val linePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                val prev = points[i - 1]
                                val curr = points[i]
                                val midX = (prev.x + curr.x) / 2
                                cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                            }
                        }

                        val fillPath = Path().apply {
                            addPath(linePath)
                            lineTo(points.last().x, h)
                            lineTo(points.first().x, h)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primary.copy(alpha = 0.35f),
                                    primary.copy(alpha = 0.0f)
                                )
                            )
                        )

                        drawPath(
                            path = linePath,
                            color = primary,
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    points.forEachIndexed { index, point ->
                        val value = data[index].second
                        val isMax = value == rawMax && value > 0f
                        drawCircle(
                            color = if (isMax) primary else primary.copy(alpha = 0.7f),
                            radius = (if (isMax) 5.dp else 3.dp).toPx(),
                            center = point
                        )
                        if (isMax) {
                            drawCircle(
                                color = onPrimary,
                                radius = 2.dp.toPx(),
                                center = point
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 36.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val maxLabels = 6
            val step = ((data.size + maxLabels - 1) / maxLabels).coerceAtLeast(1)
            data.forEachIndexed { index, (label, _) ->
                if (index % step == 0 || index == data.size - 1) {
                    Text(
                        text = label,
                        style = labelStyle,
                        color = onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}