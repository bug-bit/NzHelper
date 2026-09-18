package me.neko.nzhelper.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.material3.Material3
import me.neko.nzhelper.ui.theme.LocalThemeState

@Immutable
class FloatingBottomBarColors(
    val containerColor: Color,
    val indicatorColor: Color,
    val contentColor: Color,
    val activeContentColor: Color
)

object FloatingBottomBarDefaults {
    @Composable
    fun colors(
        containerColor: Color = MaterialTheme.colorScheme.surfaceBright,
        indicatorColor: Color = MaterialTheme.colorScheme.primary,
        contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        activeContentColor: Color = MaterialTheme.colorScheme.primary
    ): FloatingBottomBarColors = FloatingBottomBarColors(
        containerColor = containerColor,
        indicatorColor = indicatorColor,
        contentColor = contentColor,
        activeContentColor = activeContentColor
    )
}

@Composable
fun floatingBarContentBottomInset(): Dp =
    if (LocalThemeState.current.floatingBottomBar) floatingBarBottomOverlayHeight() else 0.dp

@Composable
fun floatingBarBottomOverlayHeight(): Dp = with(LocalDensity.current) {
    val navBarsPx = WindowInsets.navigationBars.getBottom(this)
    (navBarsPx + 76.dp.roundToPx()).toDp()
}

@Composable
fun FloatingBottomBar(
    items: List<BottomNavItem>,
    pagerState: PagerState,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    colors: FloatingBottomBarColors = FloatingBottomBarDefaults.colors(),
    hazeState: HazeState? = null
) {
    val tabsCount = items.size
    val currentIndex = pagerState.currentPage
    val pillShape = CircleShape
    val density = LocalDensity.current

    var totalWidthPx by remember { mutableIntStateOf(0) }
    val tabWidthPx = if (totalWidthPx > 0 && tabsCount > 0) {
        (totalWidthPx - with(density) { 8.dp.toPx() }) / tabsCount
    } else {
        0f
    }
    val tabWidthDp = with(density) { tabWidthPx.toDp() }

    val indicator = remember { Animatable(currentIndex.toFloat()) }
    LaunchedEffect(currentIndex) {
        indicator.animateTo(
            targetValue = currentIndex.toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords -> totalWidthPx = coords.size.width }
                .dropShadow(
                    shape = pillShape,
                    shadow = Shadow(radius = 10.dp, color = Color.Black, alpha = 0.15f)
                )
                .clip(pillShape)
                .then(
                    if (hazeState != null) {
                        Modifier.hazeBlur(
                            input = HazeInput.Sources(hazeState),
                            style = HazeBlurStyle.Material3(
                                containerColor = colors.containerColor.copy(alpha = 0.62f)
                            ) {
                                blurRadius(24.dp)
                            }
                        )
                    } else {
                        Modifier.background(colors.containerColor)
                    }
                )
        ) {
            if (tabWidthPx > 0f) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .offset {
                            IntOffset(
                                x = (indicator.value * tabWidthPx).roundToInt(),
                                y = with(density) { 4.dp.roundToPx() }
                            )
                        }
                        .size(width = tabWidthDp, height = 56.dp)
                        .clip(pillShape)
                        .background(colors.indicatorColor.copy(alpha = 0.15f), pillShape)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(pillShape)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val selected = index == currentIndex
                    val tint by animateColorAsState(
                        targetValue = if (selected) colors.activeContentColor else colors.contentColor,
                        animationSpec = tween(200),
                        label = "bottomBarItemTint"
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(pillShape)
                            .selectable(
                                selected = selected,
                                role = Role.Tab,
                                onClick = { onItemClick(index) }
                            ),
                        verticalArrangement = Arrangement.spacedBy(
                            1.dp,
                            Alignment.CenterVertically
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = item.icon(),
                            contentDescription = item.title,
                            tint = tint,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = item.title,
                            color = tint,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
