package me.neko.nzhelper.feature.achievement.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.model.Achievement
import me.neko.nzhelper.core.model.AchievementProgress
import me.neko.nzhelper.core.model.AchievementStats

@Composable
fun AchievementCard(
    progress: AchievementProgress,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val cardModifier = modifier.fillMaxWidth()
    val shape = MaterialTheme.shapes.large
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceBright
    )

    if (onClick == null) {
        Card(modifier = cardModifier, shape = shape, colors = colors) {
            AchievementCardContent(progress)
        }
    } else {
        Card(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                onClick()
            },
            modifier = cardModifier,
            shape = shape,
            colors = colors
        ) {
            AchievementCardContent(progress)
        }
    }
}

@Composable
private fun AchievementCardContent(progress: AchievementProgress) {
    val achievement = progress.achievement
    val unlocked = progress.isUnlocked
    val accent = achievement.tier.accent
    val iconTint = if (unlocked) {
        accent
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    if (unlocked) {
                        accent.copy(alpha = 0.18f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = achievement.category.icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            if (!unlocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (unlocked) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    },
                    fontWeight = if (unlocked) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (unlocked) {
                    Spacer(Modifier.width(8.dp))
                    TierBadge(label = achievement.tier.label, accent = accent)
                    if (achievement.hidden) {
                        Spacer(Modifier.width(4.dp))
                        HiddenBadge()
                    }
                }
            }

            Spacer(Modifier.height(3.dp))

            Text(
                text = achievement.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (unlocked) accent else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = progress.progressText,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (unlocked) accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
                progress.unlockedDate?.let { date ->
                    Text(
                        text = "解锁于 $date",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AchievementCardPreview() {
    val stats = AchievementStats(totalCount = 7)
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AchievementCard(
            progress = AchievementProgress(
                achievement = Achievement.RECORD_10,
                current = Achievement.RECORD_10.progress(stats),
                unlockedAt = null
            )
        )
        AchievementCard(
            progress = AchievementProgress(
                achievement = Achievement.FIRST_RECORD,
                current = 1,
                unlockedAt = System.currentTimeMillis()
            )
        )
    }
}
