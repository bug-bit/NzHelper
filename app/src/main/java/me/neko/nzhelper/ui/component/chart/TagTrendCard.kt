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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Tag
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.feature.statistics.model.TagTrendDirection
import me.neko.nzhelper.feature.statistics.model.TagTrendItem
import me.neko.nzhelper.feature.statistics.util.calculateTagTrendData
import me.neko.nzhelper.ui.theme.TagColors
import java.time.LocalDateTime

private val DownColor = Color(0xFFE11D48)

@Composable
fun TagTrendCard(
    sessions: List<Session>,
    currentTime: LocalDateTime,
    modifier: Modifier = Modifier,
    windowDays: Int = 30
) {
    val context = LocalContext.current

    val data by remember(sessions, currentTime, windowDays) {
        derivedStateOf { calculateTagTrendData(sessions, context, currentTime, windowDays) }
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
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "标签趋势",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "近 ${data.windowDays} 天 vs 前 ${data.windowDays} 天",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (data.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "暂无数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    data.items.forEach { item ->
                        TagTrendRow(item = item, progress = progress.value)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = "新出现标签按 +100% 显示 · 按 ${data.windowDays} 天内活跃度排序",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun TagTrendRow(
    item: TagTrendItem,
    progress: Float
) {
    val tagColor = TagColors.colorFor(item.color)
    val downColor = DownColor
    val flatColor = MaterialTheme.colorScheme.outline
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val barColor = when (item.trend) {
        TagTrendDirection.UP -> tagColor
        TagTrendDirection.DOWN -> downColor
        TagTrendDirection.FLAT -> flatColor
    }

    val safeMax = maxOf(item.recentCount, item.previousCount, 1)
    val ratio = item.recentCount.toFloat() / safeMax

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(52.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(trackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio * progress)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(barColor)
            )
        }

        Text(
            text = "${item.recentCount}/${item.previousCount}",
            style = MaterialTheme.typography.labelSmall,
            color = onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.width(38.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            modifier = Modifier.width(58.dp)
        ) {
            when (item.trend) {
                TagTrendDirection.UP -> Icon(
                    Icons.AutoMirrored.Outlined.TrendingUp,
                    contentDescription = null,
                    tint = tagColor,
                    modifier = Modifier.size(13.dp)
                )

                TagTrendDirection.DOWN -> Icon(
                    Icons.AutoMirrored.Outlined.TrendingDown,
                    contentDescription = null,
                    tint = downColor,
                    modifier = Modifier.size(13.dp)
                )

                TagTrendDirection.FLAT -> Icon(
                    Icons.AutoMirrored.Outlined.TrendingFlat,
                    contentDescription = null,
                    tint = flatColor,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = when (item.trend) {
                    TagTrendDirection.UP if item.previousCount == 0 -> "+100%"
                    TagTrendDirection.UP -> "+${item.changePercent}%"
                    TagTrendDirection.DOWN -> "${item.changePercent}%"
                    else -> "持平"
                },
                style = MaterialTheme.typography.labelSmall,
                color = barColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
