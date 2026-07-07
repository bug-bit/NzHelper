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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.feature.statistics.model.MonthlyTrendData
import me.neko.nzhelper.feature.statistics.util.calculateMonthlyTrendData
import java.time.LocalDateTime

@Composable
fun MonthlyTrendCard(
    sessions: List<Session>,
    currentTime: LocalDateTime,
    modifier: Modifier = Modifier
) {
    val data by remember(sessions, currentTime) {
        derivedStateOf { calculateMonthlyTrendData(sessions, currentTime) }
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
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
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ShowChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "月度趋势",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "最近 14 个月 · 本月预测",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "预计本月 ${data.predictedCount} 次",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            if (data.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "暂无数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                MonthlyBars(data = data, progress = progress.value)
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "已过 ${data.elapsedDays}/${data.totalDaysInMonth} 天 · 当前 ${data.currentActual} 次",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "近 3 月均 ${data.last3MonthsAvg} 次",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MonthlyBars(
    data: MonthlyTrendData,
    progress: Float
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val density = LocalDensity.current
    val maxCount = (data.items.maxOfOrNull { it.count } ?: 0)
        .coerceAtLeast(data.predictedCount)
        .coerceAtLeast(1)

    val dashEffect = PathEffect.dashPathEffect(
        intervals = floatArrayOf(
            with(density) { 3.dp.toPx() },
            with(density) { 3.dp.toPx() }
        )
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            data.items.forEach { item ->
                val ratio = item.count.toFloat() / maxCount
                val predRatio = if (item.isCurrent && data.predictedCount > item.count) {
                    data.predictedCount.toFloat() / maxCount
                } else 0f

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = if (item.count > 0) item.count.toString() else "",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = if (item.isCurrent) primary else onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = if (item.isCurrent) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Spacer(Modifier.height(2.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val barW = size.width
                            if (predRatio > 0f) {
                                val predH = size.height * predRatio * progress
                                drawRoundRect(
                                    color = primary.copy(alpha = 0.12f),
                                    size = Size(barW, predH),
                                    topLeft = Offset(0f, size.height - predH),
                                    cornerRadius = CornerRadius(
                                        with(density) { 3.dp.toPx() },
                                        with(density) { 3.dp.toPx() }
                                    )
                                )
                                drawRoundRect(
                                    color = primary.copy(alpha = 0.7f),
                                    topLeft = Offset(0f, size.height - predH),
                                    size = Size(barW, predH),
                                    cornerRadius = CornerRadius(
                                        with(density) { 3.dp.toPx() },
                                        with(density) { 3.dp.toPx() }
                                    ),
                                    style = Stroke(
                                        width = with(density) { 1.dp.toPx() },
                                        pathEffect = dashEffect
                                    )
                                )
                            }
                            if (item.count > 0) {
                                val h = size.height * ratio * progress
                                drawRoundRect(
                                    color = if (item.isCurrent) primary else primary.copy(alpha = 0.55f),
                                    size = Size(barW, h),
                                    topLeft = Offset(0f, size.height - h),
                                    cornerRadius = CornerRadius(
                                        with(density) { 3.dp.toPx() },
                                        with(density) { 3.dp.toPx() }
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            data.items.forEachIndexed { i, item ->
                Text(
                    text = if (i % 2 == 0 || i == data.items.lastIndex) item.label else "",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = if (item.isCurrent) MaterialTheme.colorScheme.primary
                    else onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    fontWeight = if (item.isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(color = primary.copy(alpha = 0.55f), label = "历史月")
            LegendDot(color = primary, label = "本月实际")
            LegendDashedBox(color = primary, label = "预测部分")
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LegendDashedBox(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color.copy(alpha = 0.12f))
        )
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
