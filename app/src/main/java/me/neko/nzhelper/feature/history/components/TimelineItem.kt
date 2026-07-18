package me.neko.nzhelper.feature.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarRate
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.core.util.formatTime
import me.neko.nzhelper.ui.component.tag.TagChip
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimelineItem(
    modifier: Modifier = Modifier,
    session: Session,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val outline = MaterialTheme.colorScheme.outlineVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val today = remember { LocalDate.now() }
    val sessionDate = remember(session.timestamp) { session.timestamp.toLocalDate() }
    val dayDiff = remember(sessionDate) { ChronoUnit.DAYS.between(sessionDate, today).toInt() }
    val isToday = dayDiff == 0

    val relativeDate = remember(dayDiff, sessionDate) {
        when (dayDiff) {
            0 -> "今天"
            1 -> "昨天"
            2 -> "前天"
            else -> sessionDate.format(DateTimeFormatter.ofPattern("M月d日 EEE", Locale.CHINA))
        }
    }
    val timeText = remember(session.timestamp) {
        session.timestamp.format(DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA))
    }

    val context = LocalContext.current
    val resolvedTags = remember(session.tagIds) {
        session.tagIds.mapNotNull { TagSettings.getTag(context, it) }.take(4)
    }

    val showActions = onDelete != null
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val largeRadius = MaterialTheme.shapes.large.topStart
    val smallRadius = MaterialTheme.shapes.extraSmall.topStart

    val shape = when {
        isPressed -> RoundedCornerShape(
            topStart = largeRadius, topEnd = largeRadius,
            bottomStart = largeRadius, bottomEnd = largeRadius
        )

        isFirst && isLast -> RoundedCornerShape(
            topStart = largeRadius, topEnd = largeRadius,
            bottomStart = largeRadius, bottomEnd = largeRadius
        )

        isFirst -> RoundedCornerShape(
            topStart = largeRadius, topEnd = largeRadius,
            bottomStart = smallRadius, bottomEnd = smallRadius
        )

        isLast -> RoundedCornerShape(
            topStart = smallRadius, topEnd = smallRadius,
            bottomStart = largeRadius, bottomEnd = largeRadius
        )

        else -> RoundedCornerShape(
            topStart = smallRadius, topEnd = smallRadius,
            bottomStart = smallRadius, bottomEnd = smallRadius
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceBright)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isToday) {
                Box(
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(primary),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(primary.copy(alpha = 0.6f))
                    )
                }
            }
            if (!isLast) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(outline.copy(alpha = 0.6f))
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = relativeDate,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isToday) primary else onSurface
                    )
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.titleSmall,
                        color = onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (showActions) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(5.dp))

            val durationColor = if (isToday) primary else onSurfaceVariant
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = durationColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = formatTime(session.duration),
                        style = MaterialTheme.typography.labelLarge,
                        color = durationColor
                    )
                }
                if (session.rating > 0f) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.StarRate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "%.1f".format(session.rating),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                if (session.climax) {
                    TagChip(
                        name = "高潮",
                        color = "rose",
                        icon = null,
                        small = true
                    )
                }
            }

            if (resolvedTags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    resolvedTags.forEach { tag ->
                        TagChip(
                            name = tag.name,
                            color = tag.color,
                            icon = tag.icon,
                            small = true
                        )
                    }
                }
            }

            if (session.remark.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(14.dp)
                            .clip(CircleShape)
                            .background(onSurfaceVariant.copy(alpha = 0.3f))
                    )
                    Text(
                        text = session.remark,
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}