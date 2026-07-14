package me.neko.nzhelper.feature.tagmanage.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.ui.theme.TagColors
import me.neko.nzhelper.ui.theme.TagIcons

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPickerRow(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: String = "slate"
) {
    val selectedContainer = TagColors.containerColor(accentColor)
    val selectedContent = TagColors.contentColor(accentColor)
    val idleContainer = MaterialTheme.colorScheme.surfaceContainerHigh
    val idleContent = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 120.dp)
            .verticalScroll(rememberScrollState())
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TagIcons.candidates.forEach { name ->
                val isSelected = name == selected
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) selectedContainer else idleContainer)
                        .clickable { onSelect(name) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = TagIcons.iconFor(name),
                        contentDescription = name,
                        tint = if (isSelected) selectedContent else idleContent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
