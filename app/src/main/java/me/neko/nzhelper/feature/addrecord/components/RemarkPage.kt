package me.neko.nzhelper.feature.addrecord.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.neko.nzhelper.core.model.SessionFormState
import me.neko.nzhelper.ui.component.form.SectionCard
import me.neko.nzhelper.ui.component.form.SectionLabel

@Composable
internal fun RemarkPage(
    formState: SessionFormState,
    onFormStateChange: (SessionFormState) -> Unit
) {
    SectionCard {
        SectionLabel("备注")
        OutlinedTextField(
            value = formState.remark,
            onValueChange = {
                onFormStateChange(formState.copy(remark = it))
            },
            placeholder = { Text("说点什么吧...") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = Int.MAX_VALUE
        )
    }
}
