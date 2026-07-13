package me.neko.nzhelper.ui.component.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.neko.nzhelper.feature.statistics.model.PeriodData
import me.neko.nzhelper.feature.statistics.util.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodChartCard(
    weekData: PeriodData,
    monthData: PeriodData,
    yearData: PeriodData,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val periodDataList = listOf(weekData, monthData, yearData)
    val tabLabels = listOf("本周", "本月", "今年")
    val currentData = periodDataList[selectedTabIndex]

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
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Insights,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "周期统计",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "时长分布对比",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                tabLabels.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = tabLabels.size
                        ),
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selectedTabIndex == index) FontWeight.SemiBold
                                else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        "总时长",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        formatDuration(currentData.totalDurationSeconds),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "平均时长",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (currentData.totalDurationSeconds > 0)
                            "%.1f 分钟".format(currentData.avgDurationMinutes)
                        else "—",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (currentData.totalDurationSeconds > 0)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            BarChart(data = currentData.chartData)
        }
    }
}

@Composable
private fun BarChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 160.dp
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

    val rawMax = data.maxOf { it.second }
    val maxValue = if (rawMax <= 0f) 1f else rawMax * 1.2f

    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall

    val animationProgress = remember(data) { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(chartHeight)
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

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            ) {
                val barCount = data.size
                val spacing = when {
                    barCount > 20 -> 2.dp
                    barCount > 10 -> 4.dp
                    else -> 6.dp
                }
                val totalSpacing = spacing * (barCount - 1)
                val availableWidth = maxWidth - totalSpacing
                val barWidth = (availableWidth / barCount).coerceIn(2.dp, 32.dp)
                val showValueLabels = barCount <= 12

                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeight)
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
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(
                                spacing,
                                Alignment.CenterHorizontally
                            ),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            data.forEach { (_, value) ->
                                val ratio = (value / maxValue).coerceIn(0f, 1f)
                                val animatedRatio = ratio * animationProgress.value
                                val barHeight = chartHeight * animatedRatio
                                val isMax = value == rawMax && value > 0f
                                val hasValue = value > 0f

                                Column(
                                    modifier = Modifier.width(barWidth),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    if (showValueLabels && hasValue) {
                                        Text(
                                            text = value.toInt().toString(),
                                            style = labelStyle,
                                            fontSize = 9.sp,
                                            color = if (isMax) primary
                                            else onSurfaceVariant.copy(alpha = 0.6f),
                                            fontWeight = if (isMax) FontWeight.SemiBold
                                            else FontWeight.Normal,
                                            maxLines = 1
                                        )
                                        Spacer(Modifier.height(2.dp))
                                    } else if (showValueLabels) {
                                        Spacer(Modifier.height(13.dp))
                                    }

                                    val cornerRadius = minOf(6.dp, barWidth / 2)
                                    Box(
                                        modifier = Modifier
                                            .width(barWidth)
                                            .height(barHeight.coerceAtLeast(if (hasValue) 2.dp else 0.dp))
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = cornerRadius,
                                                    topEnd = cornerRadius
                                                )
                                            )
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = if (isMax) {
                                                        listOf(primary, primary.copy(alpha = 0.85f))
                                                    } else {
                                                        listOf(
                                                            primary.copy(alpha = 0.6f),
                                                            primary.copy(alpha = 0.4f)
                                                        )
                                                    }
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(
                            spacing,
                            Alignment.CenterHorizontally
                        )
                    ) {
                        data.forEach { (label, value) ->
                            val isMax = value == rawMax && value > 0f
                            Text(
                                text = label,
                                style = labelStyle,
                                fontSize = if (barCount > 10) 9.sp else 10.sp,
                                color = if (isMax) primary
                                else onSurfaceVariant.copy(alpha = 0.6f),
                                fontWeight = if (isMax) FontWeight.SemiBold
                                else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.width(barWidth)
                            )
                        }
                    }
                }
            }
        }
    }
}