package me.neko.nzhelper.feature.addrecord.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.auto.AutoTagRules
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.SessionFormState
import me.neko.nzhelper.core.model.SessionMode
import me.neko.nzhelper.ui.component.form.SectionCard
import me.neko.nzhelper.ui.component.form.SectionLabel
import java.time.LocalDateTime

@Composable
internal fun PartnerLocationPage(
    formState: SessionFormState,
    onFormStateChange: (SessionFormState) -> Unit
) {
    val context = LocalContext.current
    val pairMode = SessionMode.PAIR

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TagSelectCard(
            title = "伴侣",
            loadTags = {
                TagSettings.getTags(context).filter {
                    it.groupId == TagSettings.GROUP_PARTNER && it.appliesTo(pairMode)
                }
            },
            selectedIds = formState.partners,
            onSelectionChange = {
                onFormStateChange(formState.copy(partners = it))
            },
            addLabel = "新增伴侣",
            onAddNew = { name ->
                TagSettings.addTag(
                    context,
                    name,
                    TagSettings.GROUP_PARTNER,
                    icon = "smile",
                    color = "pink",
                    modeKeys = listOf(SessionMode.PAIR.key)
                )
            }
        )

        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("发起者")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("我", "对方").forEach { option ->
                        val selected = formState.initiator == option
                        FilterChip(
                            selected = selected,
                            onClick = {
                                onFormStateChange(
                                    formState.copy(
                                        initiator = if (selected) "" else option
                                    )
                                )
                            },
                            label = { Text(option) }
                        )
                    }
                }
            }
        }

        TagSelectCard(
            title = "地点",
            loadTags = {
                TagSettings.getTags(context).filter {
                    it.groupId == TagSettings.LEGACY_GROUP_ENV
                }
            },
            selectedIds = formState.locations,
            onSelectionChange = {
                onFormStateChange(formState.copy(locations = it))
            }
        )

        val timeTags = TagSettings.getTags(context).filter {
            it.groupId == TagSettings.GROUP_TIME
        }
        val timeIds = timeTags.map { it.id }.toSet()
        TagSelectCard(
            title = "时间",
            loadTags = { timeTags },
            selectedIds = formState.tagIds.intersect(timeIds),
            onSelectionChange = { selected ->
                onFormStateChange(
                    formState.copy(
                        tagIds = formState.tagIds - timeIds + selected
                    )
                )
            },
            autoMatchLabel = "自动匹配",
            onAutoMatch = { current ->
                val ts = try {
                    formState.toLocalDateTime()
                } catch (_: Exception) {
                    LocalDateTime.now()
                }
                current + AutoTagRules.suggest(context, ts).intersect(timeIds)
            }
        )
    }
}
