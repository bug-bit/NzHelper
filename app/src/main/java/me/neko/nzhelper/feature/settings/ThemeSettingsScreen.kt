package me.neko.nzhelper.feature.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image as BitmapImage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Window
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.neko.nzhelper.core.datastore.ThemeSettings
import me.neko.nzhelper.core.util.BackgroundImageManager
import me.neko.nzhelper.ui.component.setting.SettingsCard
import me.neko.nzhelper.ui.component.setting.SettingsDivider
import me.neko.nzhelper.ui.component.setting.SettingsItem
import me.neko.nzhelper.ui.theme.LocalThemeState
import me.neko.nzhelper.ui.theme.ThemeColorOptions

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

    val scope = rememberCoroutineScope()
    val hasBackground = themeState.backgroundImagePath != null

    val pickBackgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val savedPath = BackgroundImageManager.saveImage(context, uri)
                withContext(Dispatchers.Main) {
                    if (savedPath != null) {
                        themeState.backgroundImagePath = savedPath
                        ThemeSettings.setBackgroundImagePath(context, savedPath)
                    } else {
                        Toast.makeText(context, "设置背景图片失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val removeBackground: () -> Unit = {
        themeState.backgroundImagePath = null
        ThemeSettings.setBackgroundImagePath(context, null)
        scope.launch(Dispatchers.IO) { BackgroundImageManager.removeImage(context) }
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
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(16.dp),
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
                    if (!themeState.dynamicColor) {
                        SettingsDivider()
                        ThemeColorPickerItem(
                            selectedIndex = themeState.themeColorIndex,
                            onSelect = { index ->
                                themeState.themeColorIndex = index
                                ThemeSettings.setThemeColorIndex(context, index)
                            }
                        )
                    }
                }
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.Image,
                        title = "自定义背景图片",
                        subtitle = if (hasBackground) "点击更换图片" else "从相册选择图片作为全局背景",
                        onClick = {
                            pickBackgroundLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        trailingContent = {
                            BackgroundThumbnail(imagePath = themeState.backgroundImagePath)
                        }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Outlined.Opacity,
                        title = "背景不透明度",
                        subtitle = "${(themeState.backgroundOpacity * 100).roundToInt()}%",
                        enabled = hasBackground,
                        onClick = {},
                        trailingContent = {
                            Slider(
                                value = themeState.backgroundOpacity,
                                onValueChange = { opacity ->
                                    themeState.backgroundOpacity = opacity
                                    ThemeSettings.setBackgroundOpacity(context, opacity)
                                },
                                modifier = Modifier.width(160.dp),
                                enabled = hasBackground
                            )
                        }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Outlined.BlurOn,
                        title = "背景模糊度",
                        subtitle = if (themeState.backgroundBlur < 0.5f) {
                            "无"
                        } else {
                            "${themeState.backgroundBlur.roundToInt()} dp"
                        },
                        enabled = hasBackground,
                        onClick = {},
                        trailingContent = {
                            Slider(
                                value = themeState.backgroundBlur,
                                onValueChange = { blur ->
                                    themeState.backgroundBlur = blur
                                    ThemeSettings.setBackgroundBlur(context, blur)
                                },
                                valueRange = 0f..25f,
                                modifier = Modifier.width(160.dp),
                                enabled = hasBackground
                            )
                        }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Outlined.Dashboard,
                        title = "卡片不透明度",
                        subtitle = "${(themeState.cardOpacity * 100).roundToInt()}%",
                        enabled = hasBackground,
                        onClick = {},
                        trailingContent = {
                            Slider(
                                value = themeState.cardOpacity,
                                onValueChange = { opacity ->
                                    themeState.cardOpacity = opacity
                                    ThemeSettings.setCardOpacity(context, opacity)
                                },
                                modifier = Modifier.width(160.dp),
                                enabled = hasBackground
                            )
                        }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Outlined.Window,
                        title = "弹窗不透明度",
                        subtitle = "${(themeState.dialogOpacity * 100).roundToInt()}%",
                        enabled = hasBackground,
                        onClick = {},
                        trailingContent = {
                            Slider(
                                value = themeState.dialogOpacity,
                                onValueChange = { opacity ->
                                    themeState.dialogOpacity = opacity
                                    ThemeSettings.setDialogOpacity(context, opacity)
                                },
                                modifier = Modifier.width(160.dp),
                                enabled = hasBackground
                            )
                        }
                    )
                    if (hasBackground) {
                        SettingsDivider()
                        SettingsItem(
                            icon = Icons.Outlined.DeleteOutline,
                            title = "移除背景图片",
                            subtitle = "恢复纯色背景",
                            onClick = removeBackground
                        )
                    }
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

@Composable
private fun BackgroundThumbnail(imagePath: String?) {
    val thumbnail by produceState<ImageBitmap?>(initialValue = null, imagePath) {
        if (imagePath != null) {
            value = withContext(Dispatchers.IO) {
                BackgroundImageManager.loadImageBitmap(imagePath, maxDimension = 256)
                    ?.asImageBitmap()
            }
        }
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        thumbnail?.let { bitmap ->
            BitmapImage(
                bitmap = bitmap,
                contentDescription = "背景预览",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } ?: Icon(
            imageVector = Icons.Outlined.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ThemeColorPickerItem(
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ColorLens,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "主题色",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "关闭动态取色后使用所选颜色",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeColorOptions.forEachIndexed { index, option ->
                ColorDot(
                    color = option.seed,
                    name = option.name,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) }
                )
            }
        }
    }
}

@Composable
private fun ColorDot(
    color: Color,
    name: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .semantics { contentDescription = name }
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(color)
        )
    }
}
