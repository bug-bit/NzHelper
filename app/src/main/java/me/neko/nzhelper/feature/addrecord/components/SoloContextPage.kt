package me.neko.nzhelper.feature.addrecord.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.auto.AutoTagRules
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.SessionFormState
import java.time.LocalDateTime

@Composable
internal fun SoloContextPage(
    formState: SessionFormState,
    onFormStateChange: (SessionFormState) -> Unit
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
