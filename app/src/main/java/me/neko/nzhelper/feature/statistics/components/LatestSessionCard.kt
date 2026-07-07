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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.feature.statistics.model.LatestSessionInfo

@Composable
fun LatestSessionCard(
    latestInfo: LatestSessionInfo?,
    modifier: Modifier = Modifier
) {
    val isError = latestInfo?.isErrorState == true

    val accentBase = if (isError) MaterialTheme.colorScheme.tertiary
    else MaterialTheme.colorScheme.primary
    val onAccentBase = if (isError) MaterialTheme.colorScheme.onTertiary
    else MaterialTheme.colorScheme.onPrimary

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(accentBase, accentBase.copy(alpha = 0.78f))
    )

    val contentColorVariant = onAccentBase.copy(alpha = 0.82f)
    val overlayColor = onAccentBase.copy(alpha = 0.14f)

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .fillMaxWidth()
        ) {
            if (latestInfo == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(overlayColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = onAccentBase,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "还没有记录哦",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = onAccentBase
                    )
                    Text(
                        text = "去首页开始第一次记录吧",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColorVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(overlayColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isError) Icons.Outlined.Error
                                else Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = onAccentBase,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "最近一次 · ${latestInfo.displayDate}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = onAccentBase
                            )
                            Spacer(Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = latestInfo.time,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = contentColorVariant
                                )
                                Box(
                                    modifier = Modifier
                                        .size(3.dp)
                                        .clip(CircleShape)
                                        .background(contentColorVariant.copy(alpha = 0.6f))
                                )
                                Text(
                                    text = "时长 ${latestInfo.durationText}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = contentColorVariant
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val (bigText, smallText) = when (latestInfo.daysAgo) {
                                0L -> "今天" to ""
                                1L -> "昨天" to ""
                                else -> latestInfo.daysAgo.toString() to "天前"
                            }
                            Text(
                                text = bigText,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = onAccentBase
                            )
                            if (smallText.isNotEmpty()) {
                                Text(
                                    text = smallText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = contentColorVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(overlayColor)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(28.dp)
                                .clip(CircleShape)
                                .background(onAccentBase.copy(alpha = 0.5f))
                        )
                        Text(
                            text = latestInfo.detailText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = onAccentBase,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}