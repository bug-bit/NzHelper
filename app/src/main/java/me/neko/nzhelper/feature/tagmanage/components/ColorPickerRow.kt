package me.neko.nzhelper.feature.tagmanage.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.ui.theme.TagColors

private val SwatchSize = 36.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerRow(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TagColors.names.forEach { name ->
            val color = TagColors.colorFor(name)
            val isSelected = name == selected
            val contrastColor = if (color.luminance() > 0.5f) Color.Black else Color.White
            Box(
                modifier = Modifier
                    .size(SwatchSize)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        },
                        shape = CircleShape
                    )
                    .clickable { onSelect(name) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = contrastColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
