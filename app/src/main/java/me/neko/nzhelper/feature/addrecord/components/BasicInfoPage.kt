package me.neko.nzhelper.feature.addrecord.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Female
import androidx.compose.material.icons.outlined.Male
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.auto.AutoTagRules
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.core.model.SessionFormState
import me.neko.nzhelper.core.model.SessionMode
import me.neko.nzhelper.core.util.formatTime
import me.neko.nzhelper.feature.addrecord.AddRecordFlow
import me.neko.nzhelper.ui.component.form.DateTimeInputSection
import me.neko.nzhelper.ui.component.form.SectionCard
import me.neko.nzhelper.ui.component.wizard.SummaryRow

@Composable
internal fun BasicInfoPage(
    flow: AddRecordFlow,
    formState: SessionFormState,
    elapsedSeconds: Int,
    editSession: Session?,
    onFormStateChange: (SessionFormState) -> Unit
) {
    val context = LocalContext.current
    var showDurationPicker by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SessionMode.entries.forEach { mode ->
                ModeTile(
                    icon = when (mode) {
                        SessionMode.SOLO_MALE -> Icons.Outlined.Male
                        SessionMode.SOLO_FEMALE -> Icons.Outlined.Female
                        SessionMode.PAIR -> Icons.Outlined.FavoriteBorder
                    },
                    title = mode.label,
                    subtitle = when (mode) {
                        SessionMode.SOLO_MALE -> "个人记录"
                        SessionMode.SOLO_FEMALE -> "个人记录"
                        SessionMode.PAIR -> "与伴侣"
                    },
                    selected = formState.mode == mode.key,
                    onClick = {
                        if (formState.mode == mode.key) return@ModeTile
                        onFormStateChange(
                            formState.copy(
                                mode = mode.key,
                                categoryId = TagSettings.defaultCategoryFor(context, mode).id
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        when (flow) {
            AddRecordFlow.MANUAL -> {
                SectionCard(onClick = { showDurationPicker = true }) {
                    DurationSummaryRow(seconds = formState.manualDurationSeconds)
                }

                SectionCard {
                    DateTimeInputSection(
                        formState = formState,
                        onFormStateChange = { newForm ->
                            val ts = try {
                                newForm.toLocalDateTime()
                            } catch (_: Exception) {
                                onFormStateChange(newForm)
                                return@DateTimeInputSection
                            }

                            val suggested = AutoTagRules.suggest(context, ts)
                            val (merged, added) = AutoTagRules.merge(newForm.tagIds, suggested)

                            onFormStateChange(
                                newForm.copy(
                                    tagIds = merged,
                                    autoTagIds = newForm.autoTagIds + added
                                )
                            )
                        }
                    )
                }
            }

            AddRecordFlow.EDIT if editSession != null -> {
                SectionCard(onClick = { showDurationPicker = true }) {
                    DurationSummaryRow(seconds = formState.manualDurationSeconds)
                }

                SectionCard {
                    DateTimeInputSection(
                        formState = formState,
                        onFormStateChange = onFormStateChange
                    )
                }
            }

            else -> {
                SectionCard {
                    SummaryRow("时长", formatTime(elapsedSeconds))
                }
            }
        }
    }

    if (showDurationPicker) {
        DurationPickerSheet(
            initialSeconds = formState.manualDurationSeconds,
            onConfirm = { seconds ->
                onFormStateChange(
                    formState.copy(
                        durationHour = (seconds / 3600).toString(),
                        durationMinute = ((seconds % 3600) / 60).toString(),
                        durationSecond = ""
                    )
                )
                showDurationPicker = false
            },
            onDismiss = { showDurationPicker = false }
        )
    }
}

@Composable
private fun DurationSummaryRow(seconds: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "时长",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatDurationMinutes(seconds / 60),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ModeTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceBright
            }
        ),
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
