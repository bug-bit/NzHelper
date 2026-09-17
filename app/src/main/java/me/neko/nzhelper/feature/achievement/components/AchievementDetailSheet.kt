package me.neko.nzhelper.feature.achievement.components

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.neko.nzhelper.core.achievement.AchievementShare
import me.neko.nzhelper.core.achievement.AchievementShareRenderer
import me.neko.nzhelper.core.model.Achievement
import me.neko.nzhelper.core.model.AchievementProgress
import me.neko.nzhelper.core.model.AchievementStats

private val DragHandlePadding = 22.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementDetailSheet(
    progress: AchievementProgress,
    onDismiss: () -> Unit
) {
    @Suppress("DEPRECATION")
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
        dragHandle = {}
    ) {
        AchievementDetailContent(progress = progress)
    }
}

@Composable
private fun AchievementDetailContent(
    progress: AchievementProgress,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        AchievementHeader(progress = progress)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            ProgressCard(progress = progress)

            if (progress.isUnlocked) {
                Spacer(Modifier.height(16.dp))
                ShareButton(progress = progress)
            }
        }
    }
}

@Composable
private fun AchievementHeader(
    progress: AchievementProgress,
    modifier: Modifier = Modifier
) {
    val achievement = progress.achievement
    val unlocked = progress.isUnlocked
    val accent = achievement.tier.accent
    val topAlpha = if (unlocked) 0.30f else 0.14f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accent.copy(alpha = topAlpha),
                        accent.copy(alpha = topAlpha / 3f),
                        accent.copy(alpha = 0f)
                    )
                )
            )
            .padding(horizontal = 24.dp)
            .padding(top = DragHandlePadding, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(width = 32.dp, height = 4.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                .clearAndSetSemantics {}
        )

        Spacer(Modifier.height(DragHandlePadding))

        Box(
            modifier = Modifier
                .size(88.dp)
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
                tint = if (unlocked) {
                    accent
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                },
                modifier = Modifier.size(42.dp)
            )
            if (!unlocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = achievement.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TierBadge(label = achievement.tier.label, accent = accent)
            StatusBadge(unlocked = unlocked, accent = accent)
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = achievement.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProgressCard(
    progress: AchievementProgress,
    modifier: Modifier = Modifier
) {
    val unlocked = progress.isUnlocked
    val accent = progress.achievement.tier.accent

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "当前进度",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = progress.progressText,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (unlocked) accent else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (unlocked) accent else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )

            val hint = progress.unlockedDate?.let { "解锁于 $it" } ?: progress.remainingText
            if (hint != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ShareButton(
    progress: AchievementProgress,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    var isRendering by remember { mutableStateOf(false) }

    Button(
        onClick = {
            if (isRendering) return@Button
            isRendering = true
            scope.launch {
                val palette = AchievementShareRenderer.Palette(
                    background = scheme.surface.toArgb(),
                    card = scheme.surfaceBright.toArgb(),
                    title = scheme.onSurface.toArgb(),
                    body = scheme.onSurfaceVariant.toArgb(),
                    accent = progress.achievement.tier.accent.toArgb()
                )
                val uri = AchievementShare.createImageUri(context, progress, palette)
                if (uri != null) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(
                            Intent.EXTRA_SUBJECT,
                            "NzHelper 成就 - ${progress.achievement.title}"
                        )
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(intent, "分享成就"))
                    }.onFailure {
                        Toast.makeText(context, "没有可用的分享应用", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "生成分享图片失败", Toast.LENGTH_SHORT).show()
                }
                isRendering = false
            }
        },
        enabled = !isRendering,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        if (isRendering) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(if (isRendering) "生成中…" else "分享成就")
    }
}

@Preview(showBackground = true)
@Composable
private fun AchievementDetailContentPreview() {
    val stats = AchievementStats(totalCount = 7, totalSeconds = 5400)
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        AchievementDetailContent(
            progress = AchievementProgress(
                achievement = Achievement.RECORD_10,
                current = Achievement.RECORD_10.progress(stats)
            )
        )
        AchievementDetailContent(
            progress = AchievementProgress(
                achievement = Achievement.TOTAL_HOUR_1,
                current = Achievement.TOTAL_HOUR_1.progress(stats),
                unlockedAt = System.currentTimeMillis()
            )
        )
    }
}
