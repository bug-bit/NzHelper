package me.neko.nzhelper.feature.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.datastore.ThemeSettings
import me.neko.nzhelper.ui.component.setting.SettingsCard
import me.neko.nzhelper.ui.component.setting.SettingsDivider
import me.neko.nzhelper.ui.component.setting.SettingsItem
import me.neko.nzhelper.ui.theme.LocalThemeState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val systemIsDark = isSystemInDarkTheme()
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val themeState = LocalThemeState.current

    val effectiveDark = when (themeState.themeMode) {
        ThemeSettings.ThemeMode.LIGHT -> false
        ThemeSettings.ThemeMode.DARK -> true
        ThemeSettings.ThemeMode.SYSTEM -> systemIsDark
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("主题设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        )
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsCard {
                    ModeOption(
                        icon = Icons.Outlined.PhoneAndroid,
                        title = "跟随系统",
                        subtitle = "自动在浅色与深色外观间切换",
                        selected = themeState.themeMode == ThemeSettings.ThemeMode.SYSTEM,
                        onClick = {
                            themeState.themeMode = ThemeSettings.ThemeMode.SYSTEM
                            ThemeSettings.setThemeMode(context, ThemeSettings.ThemeMode.SYSTEM)
                        }
                    )
                    SettingsDivider()
                    ModeOption(
                        icon = Icons.Outlined.LightMode,
                        title = "浅色模式",
                        subtitle = "始终使用浅色外观",
                        selected = themeState.themeMode == ThemeSettings.ThemeMode.LIGHT,
                        onClick = {
                            themeState.themeMode = ThemeSettings.ThemeMode.LIGHT
                            ThemeSettings.setThemeMode(context, ThemeSettings.ThemeMode.LIGHT)
                        }
                    )
                    SettingsDivider()
                    ModeOption(
                        icon = Icons.Outlined.DarkMode,
                        title = "深色模式",
                        subtitle = "始终使用深色外观",
                        selected = themeState.themeMode == ThemeSettings.ThemeMode.DARK,
                        onClick = {
                            themeState.themeMode = ThemeSettings.ThemeMode.DARK
                            ThemeSettings.setThemeMode(context, ThemeSettings.ThemeMode.DARK)
                        }
                    )
                }
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.DarkMode,
                        title = "AMOLED 纯黑",
                        subtitle = "深色模式下使用纯黑背景",
                        enabled = effectiveDark,
                        onClick = {
                            if (effectiveDark) {
                                themeState.amoledDark = !themeState.amoledDark
                                ThemeSettings.setAmoledDark(context, themeState.amoledDark)
                            }
                        },
                        trailingContent = {
                            Switch(
                                checked = themeState.amoledDark,
                                enabled = effectiveDark,
                                onCheckedChange = { enabled ->
                                    themeState.amoledDark = enabled
                                    ThemeSettings.setAmoledDark(context, enabled)
                                }
                            )
                        }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Outlined.Palette,
                        title = "动态取色",
                        subtitle = "使用壁纸颜色生成 Material You 主题（Android 12+）",
                        onClick = {
                            themeState.dynamicColor = !themeState.dynamicColor
                            ThemeSettings.setDynamicColor(context, themeState.dynamicColor)
                        },
                        trailingContent = {
                            Switch(
                                checked = themeState.dynamicColor,
                                onCheckedChange = { enabled ->
                                    themeState.dynamicColor = enabled
                                    ThemeSettings.setDynamicColor(context, enabled)
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    SettingsItem(
        icon = icon,
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        trailingContent = {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    )
}
