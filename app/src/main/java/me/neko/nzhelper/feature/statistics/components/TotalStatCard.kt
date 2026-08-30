package me.neko.nzhelper.feature.statistics.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.neko.nzhelper.feature.statistics.model.TotalStats

@Composable
fun TotalStatCard(
    stats: TotalStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
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
                        imageVector = Icons.Outlined.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "总体统计",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = buildDurationText(stats.totalSeconds, stats.totalCount),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = buildAvgText(stats.totalCount, stats.avgMinutes),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            if (stats.totalCount > 0) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModeCountText("男单", stats.soloMaleCount)
                    ModeCountText("女单", stats.soloFemaleCount)
                    ModeCountText("双人", stats.pairCount)
                }
            }
        }
    }
}

@Composable
private fun ModeCountText(label: String, count: Int) {
    Text(
        text = "$label $count 次",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun buildDurationText(totalSeconds: Int, totalCount: Int): AnnotatedString {
    val unitColor = MaterialTheme.colorScheme.onSurfaceVariant
    val durationNumberSize = MaterialTheme.typography.headlineMedium.fontSize
    return buildAnnotatedString {
        fun appendUnit(unit: String) {
            withStyle(
                SpanStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = unitColor
                )
            ) {
                append(unit)
            }
        }

        fun appendDurationNumber(number: Int) {
            withStyle(SpanStyle(fontSize = durationNumberSize)) {
                append("$number")
            }
        }

        appendUnit("共 ")
        append("$totalCount")
        appendUnit(" 次 · ")

        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        if (hours > 0) {
            appendDurationNumber(hours)
            appendUnit(" 小时 ")
        }
        appendDurationNumber(minutes)
        appendUnit(if (hours > 0) " 分" else " 分钟")
    }
}

@Composable
private fun buildAvgText(totalCount: Int, avgMinutes: Float): AnnotatedString {
    val unitColor = MaterialTheme.colorScheme.onSurfaceVariant
    return buildAnnotatedString {
        fun appendSmall(text: String) {
            withStyle(
                SpanStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = unitColor
                )
            ) {
                append(text)
            }
        }

        if (totalCount == 0) {
            appendSmall("暂无平均")
        } else {
            appendSmall("平均 ")
            append("%.1f".format(avgMinutes))
            appendSmall(" 分钟/次")
        }
    }
}