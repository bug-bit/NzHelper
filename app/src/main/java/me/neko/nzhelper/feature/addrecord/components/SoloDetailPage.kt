package me.neko.nzhelper.feature.addrecord.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.SessionFormState
import me.neko.nzhelper.core.model.SessionMode

@Composable
internal fun SoloDetailPage(
    formState: SessionFormState,
    onFormStateChange: (SessionFormState) -> Unit
) {
    val mode = SessionMode.fromKey(formState.mode)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SoloGroupTagCard("身体", TagSettings.GROUP_BODY, mode, formState, onFormStateChange)
        SoloGroupTagCard("行为", TagSettings.LEGACY_GROUP_ACT, mode, formState, onFormStateChange)
        SoloGroupTagCard("道具", TagSettings.LEGACY_GROUP_TOOL, mode, formState, onFormStateChange)
    }
}

@Composable
private fun SoloGroupTagCard(
    title: String,
    groupId: String,
    mode: SessionMode,
    formState: SessionFormState,
    onFormStateChange: (SessionFormState) -> Unit
) {
    val context = LocalContext.current
    val tags = TagSettings.getTags(context).filter {
        it.groupId == groupId && it.appliesTo(mode)
    }
    val ids = tags.map { it.id }.toSet()

    TagSelectCard(
        title = title,
        loadTags = { tags },
        selectedIds = formState.tagIds.intersect(ids),
        onSelectionChange = { selected ->
            onFormStateChange(
                formState.copy(
                    tagIds = formState.tagIds - ids + selected
                )
            )
        }
    )
}
