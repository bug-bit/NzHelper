package me.neko.nzhelper.feature.achievement.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.model.AchievementCategory
import me.neko.nzhelper.core.model.AchievementTier

private val BronzeAccent = Color(0xFFB87333)
private val SilverAccent = Color(0xFF8A98A5)
private val GoldAccent = Color(0xFFCC9A16)
private val DiamondAccent = Color(0xFF3E9DBF)

val AchievementTier.accent: Color
    get() = when (this) {
        AchievementTier.BRONZE -> BronzeAccent
        AchievementTier.SILVER -> SilverAccent
        AchievementTier.GOLD -> GoldAccent
        AchievementTier.DIAMOND -> DiamondAccent
    }

val AchievementCategory.icon: ImageVector
    get() = when (this) {
        AchievementCategory.MILESTONE -> Icons.Outlined.EmojiEvents
        AchievementCategory.DURATION -> Icons.Outlined.Timer
        AchievementCategory.STREAK -> Icons.Outlined.LocalFireDepartment
        AchievementCategory.TIME -> Icons.Outlined.Nightlight
        AchievementCategory.VARIETY -> Icons.Outlined.Sell
        AchievementCategory.PAIR -> Icons.Outlined.Favorite
    }

@Composable
fun TierBadge(label: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = accent.copy(alpha = 0.16f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun HiddenBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
    ) {
        Text(
            text = "隐藏",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun StatusBadge(unlocked: Boolean, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = if (unlocked) {
            accent.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        }
    ) {
        Text(
            text = if (unlocked) "已解锁" else "未解锁",
            style = MaterialTheme.typography.labelSmall,
            color = if (unlocked) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
