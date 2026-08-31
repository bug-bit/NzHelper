package me.neko.nzhelper.feature.addrecord.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.SessionFormState
import me.neko.nzhelper.ui.component.form.ClimaxCountSection
import me.neko.nzhelper.ui.component.form.RatingSection
import me.neko.nzhelper.ui.component.form.SectionCard

@Composable
internal fun RatingPage(
    formState: SessionFormState,
    onFormStateChange: (SessionFormState) -> Unit
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard {
            RatingSection(
                rating = formState.rating,
                onRatingChange = {
                    onFormStateChange(formState.copy(rating = it))
                }
            )
        }

        SectionCard {
            ClimaxCountSection(formState, onFormStateChange)
        }

        TagSelectCard(
            title = "情绪",
            loadTags = {
                TagSettings.getTags(context).filter {
                    it.groupId == TagSettings.LEGACY_GROUP_STATE
                }
            },
            selectedIds = formState.moods,
            onSelectionChange = {
                onFormStateChange(formState.copy(moods = it))
            }
        )
    }
}
