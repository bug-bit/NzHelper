package me.neko.nzhelper.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

data class SlidingTab(
    val label: String,
    val icon: ImageVector,
    val badge: String? = null
)

@Composable
fun SlidingTabRow(
    tabs: List<SlidingTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectionFraction: () -> Float = { selectedIndex.toFloat() },
    height: Dp = 48.dp
) {
    if (tabs.isEmpty()) return
    val colorScheme = MaterialTheme.colorScheme

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(colorScheme.surfaceBright)
    ) {
        val itemWidth = maxWidth / tabs.size
        val itemWidthPx = with(LocalDensity.current) { itemWidth.toPx() }
        val maxFraction = (tabs.size - 1).toFloat()

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (itemWidthPx * selectionFraction().coerceIn(0f, maxFraction))
                            .roundToInt(),
                        y = 0
                    )
                }
                .width(itemWidth)
                .fillMaxHeight()
                .padding(4.dp)
                .clip(CircleShape)
                .background(colorScheme.primaryContainer)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                val contentColor = if (selected) {
                    colorScheme.onPrimaryContainer
                } else {
                    colorScheme.onSurfaceVariant
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable { onSelect(index) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = contentColor,
                        maxLines = 1
                    )
                    if (tab.badge != null) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = tab.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
