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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.feature.statistics.model.PeriodType
import me.neko.nzhelper.feature.statistics.util.isWithinPeriod
import me.neko.nzhelper.ui.theme.TagColors
import java.time.LocalDateTime

private data class DonutSlice(val label: String, val count: Int, val colorName: String)

private const val OTHER_COLOR = "__other__"

/**
 * 「分布统计」卡片按 标签 / 分组 维度统计本年度 Session 的标签用量分布。
 * - 标签 tab：跨分组展开 session.tagIds，按标签计数 Top 6 + 其他。
 * - 分组 tab：把标签用量按 group 汇总，Top 6 + 其他。
 *
 * 每块的颜色取自标签/分组自身的颜色，而非派生调色板 —— 让分布图与标签管理配色一致。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonutChartCard(
    sessions: List<Session>,
    currentTime: LocalDateTime,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("标签", "分组")

    val distribution by remember(sessions, currentTime, selectedTabIndex) {
        derivedStateOf {
            val filtered = sessions.filter {
                isWithinPeriod(it.timestamp, currentTime, PeriodType.YEAR)
            }
            when (selectedTabIndex) {
                0 -> {
                    val counts = mutableMapOf<String, Int>()
                    for (s in filtered) {
                        for (id in s.tagIds) {
                            counts[id] = (counts[id] ?: 0) + 1
                        }
                    }
                    val resolved = counts.entries.mapNotNull { (id, c) ->
                        TagSettings.getTag(context, id)?.let { tag ->
                            DonutSlice(tag.name, c, tag.color)
                        }
                    }
                    buildTopSlices(resolved)
                }

                1 -> {
                    val groupCounts = mutableMapOf<String, Int>()
                    for (s in filtered) {
                        for (id in s.tagIds) {
                            val tag = TagSettings.getTag(context, id) ?: continue
                            groupCounts[tag.groupId] = (groupCounts[tag.groupId] ?: 0) + 1
                        }
                    }
                    val resolved = groupCounts.entries.mapNotNull { (gid, c) ->
                        TagSettings.getGroup(context, gid)?.let { g ->
                            DonutSlice(g.name, c, g.color)
                        }
                    }
                    buildTopSlices(resolved)
                }

                else -> emptyList()
            }
        }
    }

    val total = distribution.sumOf { it.count }

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
                        imageVector = Icons.Outlined.DonutLarge,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "分布统计",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(16.dp))

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = tabs.size
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

            if (total == 0) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DonutChart(
                        data = distribution,
                        total = total,
                        modifier = Modifier.size(140.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        distribution.forEach { slice ->
                            val color = sliceColor(slice.colorName)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Text(
                                    text = slice.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = slice.count.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "%.0f%%".format(slice.count.toFloat() / total * 100),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 取 Top 6 + 其他（不足 6 项时不出现其他）。 */
private fun buildTopSlices(resolved: List<DonutSlice>): List<DonutSlice> {
    val sorted = resolved.sortedByDescending { it.count }
    val top = sorted.take(6)
    val othersCount = sorted.drop(6).sumOf { it.count }
    return if (othersCount > 0) top + DonutSlice("其他", othersCount, OTHER_COLOR) else top
}

/** 把 colorName 解析为 Color；其他类目退回到主题 outlineVariant。 */
@Composable
private fun sliceColor(colorName: String): Color =
    if (colorName == OTHER_COLOR) MaterialTheme.colorScheme.outlineVariant
    else TagColors.colorFor(colorName)

@Composable
private fun DonutChart(
    data: List<DonutSlice>,
    total: Int,
    modifier: Modifier = Modifier
) {
    val bgRingColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val resolvedColors = data.map { sliceColor(it.colorName) }

    val animatedProgress = remember(data) { Animatable(0f) }
    LaunchedEffect(data) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = minOf(size.width, size.height)
            val strokePx = 18.dp.toPx()
            val radius = (diameter - strokePx) / 2
            val center = Offset(size.width / 2, size.height / 2)

            drawCircle(
                color = bgRingColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokePx)
            )

            var startAngle = -90f
            data.forEachIndexed { index, (_, count) ->
                val sweep = (count.toFloat() / total) * 360f * animatedProgress.value
                if (sweep > 0.5f) {
                    drawArc(
                        color = resolvedColors[index],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2)
                    )
                }
                startAngle += sweep
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                total.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                "总计",
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceVariant
            )
        }
    }
}