package me.neko.nzhelper.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Addchart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem(
        route = "home",
        title = "首页",
        icon = Icons.Default.Home
    )

    object Statistics : BottomNavItem(
        route = "statistics",
        title = "统计",
        icon = Icons.Default.Addchart
    )

    object History : BottomNavItem(
        route = "history",
        title = "历史",
        icon = Icons.AutoMirrored.Filled.List
    )

    object Settings : BottomNavItem(
        route = "settings",
        title = "设置",
        icon = Icons.Default.Settings
    )

    companion object {
        val items = listOf(Home, Statistics, History, Settings)
    }
}