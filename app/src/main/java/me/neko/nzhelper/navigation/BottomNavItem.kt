package me.neko.nzhelper.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource

import me.neko.nzhelper.R

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: @Composable () -> Painter
) {
    object Home : BottomNavItem(
        route = "home",
        title = "计时",
        icon = { painterResource(id = R.drawable.timer_24px) }
    )

    object Statistics : BottomNavItem(
        route = "statistics",
        title = "统计",
        icon = { painterResource(id = R.drawable.waterfall_chart_24px) }
    )

    object History : BottomNavItem(
        route = "history",
        title = "历史",
        icon = { painterResource(id = R.drawable.history_24px) }
    )

    object Mine : BottomNavItem(
        route = "mine",
        title = "我的",
        icon = { rememberVectorPainter(image = Icons.Outlined.Person) }
    )

    companion object {
        val items = listOf(Home, Statistics, History, Mine)
    }
}