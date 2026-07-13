package me.neko.nzhelper.feature.statistics.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.feature.statistics.model.PeriodDashboard
import me.neko.nzhelper.feature.statistics.model.PeriodType
import me.neko.nzhelper.feature.statistics.util.calculatePeriodDashboard
import java.time.LocalDateTime

@Composable
fun PeriodDashboardCard(
    sessions: List<Session>,
    currentTime: LocalDateTime,
    onPeriodClick: (PeriodType, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedType by remember { mutableStateOf(PeriodType.WEEK) }

    val dashboard by remember(sessions, currentTime, selectedType) {
        mutableStateOf(calculatePeriodDashboard(sessions, currentTime, selectedType))
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
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BarChart,
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
                        text = "点任一指标查看详情",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PeriodType.entries.forEach { type ->
                    val label = when (type) {
                        PeriodType.WEEK -> "本周"
                        PeriodType.MONTH -> "本月"
                        PeriodType.YEAR -> "今年"
                    }
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            AnimatedContent(
                targetState = dashboard,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "dashboard"
            ) { db ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricTile(
                            icon = Icons.Outlined.BarChart,
                            label = "次数",
                            value = db.count.toString(),
                            sub = changeBadge(db),
                            accentContainer = MaterialTheme.colorScheme.primaryContainer,
                            accentContent = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { onPeriodClick(db.type, db.label) }
                        )
                        MetricTile(
                            icon = Icons.Outlined.Schedule,
                            label = "平均持续",
                            value = formatAvgDuration(db.avgDurationSeconds),
                            sub = "本周期内均值",
                            accentContainer = MaterialTheme.colorScheme.secondaryContainer,
                            accentContent = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { onPeriodClick(db.type, db.label) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricTile(
                            icon = Icons.Outlined.Bolt,
                            label = "高潮率",
                            value = "${db.climaxRate}%",
                            sub = if (db.count > 0) "${db.climaxCount}/${db.count} 次" else "暂无记录",
                            accentContainer = MaterialTheme.colorScheme.tertiaryContainer,
                            accentContent = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { onPeriodClick(db.type, db.label) }
                        )
                        MetricTile(
                            icon = Icons.Outlined.LocalFireDepartment,
                            label = "连续记录",
                            value = "${db.streakDays}",
                            sub = streakSub(db.streakDays),
                            accentContainer = MaterialTheme.colorScheme.errorContainer,
                            accentContent = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { onPeriodClick(db.type, db.label) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    sub: String,
    accentContainer: Color,
    accentContent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = accentContainer
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentContent.copy(alpha = 0.85f),
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentContent.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = accentContent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = accentContent.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun changeBadge(db: PeriodDashboard): String {
    val prevLabel = when (db.type) {
        PeriodType.WEEK -> "上周"
        PeriodType.MONTH -> "上月"
        PeriodType.YEAR -> "去年"
    }
    return when {
        db.prevCount == 0 && db.count > 0 -> "新增 · 首次记录"
        db.prevCount == 0 && db.count == 0 -> "暂无记录"
        db.countChangePercent > 0 -> "↑ ${db.countChangePercent}% · vs $prevLabel"
        db.countChangePercent < 0 -> "↓ ${-db.countChangePercent}% · vs $prevLabel"
        else -> "持平 · vs $prevLabel"
    }
}

private fun formatAvgDuration(seconds: Int): String {
    if (seconds <= 0) return "—"
    val m = seconds / 60
    val s = seconds % 60
    return if (m == 0) "${s}秒" else "${m}分钟"
}

private fun streakSub(streak: Int): String = when {
    streak == 0 -> "尚未开始"
    streak == 1 -> "今天 / 昨天"
    streak < 7 -> "坚持中"
    streak < 30 -> "一周以上 🔥"
    else -> "超长连击 🔥🔥"
}
