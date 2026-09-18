package me.neko.nzhelper.feature.mine.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.neko.nzhelper.core.util.AvatarManager
import me.neko.nzhelper.feature.statistics.util.formatDuration
import me.neko.nzhelper.ui.component.setting.TrailingArrowIcon

private val ProgressBarInset = 8.dp

@Composable
fun ProfileSummaryCard(
    totalCount: Int,
    totalSeconds: Int,
    companionDays: Int,
    unlockedAchievements: Int,
    totalAchievements: Int,
    unseenAchievements: Int,
    avatarPath: String?,
    onAvatarClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val avatar by produceState(
        initialValue = AvatarManager.peekBitmap(avatarPath)?.asImageBitmap(),
        avatarPath
    ) {
        value = withContext(Dispatchers.IO) {
            AvatarManager.loadBitmap(avatarPath)?.asImageBitmap()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(onClick = onAvatarClick),
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = avatar
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "头像",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = "头像",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceBright),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (totalCount > 0) "已记录 $totalCount 次" else "还没有记录",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (totalCount > 0) {
                            "累计 ${formatDuration(totalSeconds)} · 陪伴 $companionDays 天"
                        } else {
                            "完成第一次记录后，这里会显示你的数据"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(onClick = onAchievementsClick)
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = ProgressBarInset)
                        .size(20.dp)
                )
                Text(
                    text = "成就",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$unlockedAchievements / $totalAchievements",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (unseenAchievements > 0) {
                    Badge { Text("$unseenAchievements") }
                }
                Spacer(Modifier.width(4.dp))
                TrailingArrowIcon()
            }

            LinearProgressIndicator(
                progress = {
                    if (totalAchievements == 0) {
                        0f
                    } else {
                        unlockedAchievements.toFloat() / totalAchievements
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ProgressBarInset)
                    .height(6.dp),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileSummaryCardPreview() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProfileSummaryCard(
            totalCount = 128,
            totalSeconds = 153_000,
            companionDays = 96,
            unlockedAchievements = 12,
            totalAchievements = 33,
            unseenAchievements = 2,
            avatarPath = null,
            onAvatarClick = {},
            onAchievementsClick = {}
        )
        ProfileSummaryCard(
            totalCount = 0,
            totalSeconds = 0,
            companionDays = 0,
            unlockedAchievements = 0,
            totalAchievements = 33,
            unseenAchievements = 0,
            avatarPath = null,
            onAvatarClick = {},
            onAchievementsClick = {}
        )
    }
}
