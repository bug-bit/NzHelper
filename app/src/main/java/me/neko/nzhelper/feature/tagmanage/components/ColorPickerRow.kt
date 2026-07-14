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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.ui.theme.LocalDarkMode
import me.neko.nzhelper.ui.theme.TagColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerRow(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkMode.current
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TagColors.names.forEach { name ->
            val color = TagColors.colorFor(name)
            val isSelected = name == selected
            val contrastColor = if (isDark) Color.Black else Color.White
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                width = 2.dp,
                                color = contrastColor,
                                shape = CircleShape
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSelect(name) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = contrastColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
