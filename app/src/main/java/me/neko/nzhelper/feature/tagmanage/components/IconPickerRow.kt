package me.neko.nzhelper.feature.tagmanage.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

private val IconCellSize = 44.dp
private val IconCellShape = RoundedCornerShape(12.dp)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPickerRow(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: String = "slate"
) {
    val accent = TagColors.colorFor(accentColor)
    val selectedContainer = TagColors.containerColor(accentColor)
    val idleContainer = MaterialTheme.colorScheme.surfaceContainerHighest
    val idleContent = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 164.dp)
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
                        .size(IconCellSize)
                        .clip(IconCellShape)
                        .background(if (isSelected) selectedContainer else idleContainer)
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) accent else idleContainer,
                            shape = IconCellShape
                        )
                        .clickable { onSelect(name) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = TagIcons.iconFor(name),
                        contentDescription = name,
                        tint = if (isSelected) accent else idleContent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
