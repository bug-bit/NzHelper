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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Leaderboard
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.feature.statistics.model.PeriodType
import me.neko.nzhelper.feature.statistics.model.TagStat
import me.neko.nzhelper.feature.statistics.util.isWithinPeriod
import me.neko.nzhelper.ui.theme.TagColors
import java.time.LocalDateTime

/**
 * 「标签 Top 10」横向条形图卡片。
 *
 * 跨分组展开本年度 Session 的 tagIds，按标签计数取前 10。
 * 每条条形按标签自身颜色渲染，宽度按比例 + 动画进度铺开。
 * 无 Canvas 之外的图表库依赖。
 */
@Composable
fun TagBarChartCard(
    sessions: List<Session>,
    currentTime: LocalDateTime,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val topTags by remember(sessions, currentTime) {
        derivedStateOf {
            val filtered = sessions.filter {
                isWithinPeriod(it.timestamp, currentTime, PeriodType.YEAR)
            }
            val counts = mutableMapOf<String, Int>()
            for (s in filtered) {
                for (id in s.tagIds) {
                    counts[id] = (counts[id] ?: 0) + 1
                }
            }
            counts.entries
                .sortedByDescending { it.value }
                .take(10)
                .mapNotNull { (id, c) ->
                    TagSettings.getTag(context, id)?.let { tag ->
                        TagStat(tag.id, tag.name, tag.color, tag.icon, c)
                    }
                }
        }
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(topTags) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val maxCount = topTags.maxOfOrNull { it.count } ?: 0

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
                        imageVector = Icons.Outlined.Leaderboard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "标签 Top 10",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(20.dp))

            if (topTags.isEmpty()) {
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    topTags.forEach { tag ->
                        TagBarRow(
                            name = tag.name,
                            count = tag.count,
                            colorName = tag.color,
                            maxCount = maxCount,
                            progress = progress.value
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagBarRow(
    name: String,
    count: Int,
    colorName: String,
    maxCount: Int,
    progress: Float
) {
    val color = TagColors.colorFor(colorName)
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val safeMax = maxCount.coerceAtLeast(1)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.width(56.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(16.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(trackColor)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width * (count.toFloat() / safeMax) * progress
                if (w > 0f) {
                    drawRoundRect(
                        color = color,
                        size = Size(w, size.height),
                        cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                    )
                }
            }
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.width(28.dp)
        )
    }
}
