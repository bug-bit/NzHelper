package me.neko.nzhelper.feature.about

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import me.neko.nzhelper.R
import me.neko.nzhelper.core.datastore.UpdateSettings
import me.neko.nzhelper.core.util.UpdateCheckManager
import me.neko.nzhelper.core.util.UpdateCheckResult
import me.neko.nzhelper.core.util.UpdateInfo
import me.neko.nzhelper.ui.component.dialog.UpdateAvailableDialog
import me.neko.nzhelper.ui.component.setting.LocalSettingsItemCorners
import me.neko.nzhelper.ui.component.setting.SettingsCard
import me.neko.nzhelper.ui.component.setting.SettingsCornerRadius
import me.neko.nzhelper.ui.component.setting.TrailingArrowIcon
import me.neko.nzhelper.ui.theme.LocalDarkMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutScreen(
    navController: NavHostController
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val scope = rememberCoroutineScope()
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var manualUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    val versionName = remember(context, isInPreview) {
        if (isInPreview) {
            "预览版"
        } else {
            try {
                context.packageManager
                    .getPackageInfo(context.packageName, 0)
                    .versionName
                    ?: "未知版本"
            } catch (_: PackageManager.NameNotFoundException) {
                "未知版本"
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("关于") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!navController.popBackStack()) {
                            (context as? Activity)?.finish()
                        }
                    }) {
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
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
                ) {
                    val icon = remember(context) {
                        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)!!
                        BitmapPainter(drawable.toBitmap().asImageBitmap())
                    }
                    Image(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(MaterialTheme.shapes.extraLarge)
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = versionName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            if (!isCheckingUpdate) {
                                isCheckingUpdate = true
                                scope.launch {
                                    try {
                                        when (
                                            val result = UpdateCheckManager.checkManually(context)
                                        ) {
                                            is UpdateCheckResult.Available -> {
                                                manualUpdate = result.info
                                            }

                                            UpdateCheckResult.UpToDate -> Toast.makeText(
                                                context,
                                                "已是最新版本",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                            UpdateCheckResult.Failed -> Toast.makeText(
                                                context,
                                                "检查更新失败",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                            UpdateCheckResult.UnsupportedBetaBuild -> Toast.makeText(
                                                context,
                                                "当前版本不是 CI 构建，无法检测测试版更新",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } finally {
                                        isCheckingUpdate = false
                                    }
                                }
                            }
                        },
                        enabled = !isCheckingUpdate
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Update,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isCheckingUpdate) "正在检查…" else "检查更新"
                        )
                    }
                }
            }

            item {
                SettingsCard(title = "项目") {
                    item {
                        SettingsItem(
                            painter = painterResource(id = R.drawable.code_24px),
                            title = "GitHub 仓库",
                            subtitle = "我要好多好多小星星✨~",
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    "https://github.com/bug-bit/NzHelper".toUri()
                                )
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }

            item {
                SettingsCard(title = "社区") {
                    item {
                        SettingsItem(
                            painter = painterResource(id = R.drawable.ic_telegram),
                            title = "Telegram CI 构建频道",
                            subtitle = "获取最新测试版",
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    "https://t.me/NzzHelper".toUri()
                                )
                                context.startActivity(intent)
                            }
                        )
                    }
                    item {
                        SettingsItem(
                            painter = painterResource(id = R.drawable.ic_telegram),
                            title = "Telegram 群组",
                            subtitle = "@NzHelper",
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    "https://t.me/NzHelper".toUri()
                                )
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }

            item {
                SettingsCard(title = "许可证") {
                    item {
                        SettingsItem(
                            painter = painterResource(id = R.drawable.source_code_24px),
                            title = "开放源代码",
                            subtitle = "查看第三方开源声明",
                            onClick = { navController.navigate("open_source") }
                        )
                    }
                }
            }
        }
    }

    manualUpdate?.let { info ->
        UpdateAvailableDialog(
            info = info,
            onDismissRequest = { manualUpdate = null },
            onIgnore = { UpdateSettings.setIgnoredVersion(context, info.tag) }
        )
    }
}

@Composable
private fun SettingsItem(
    painter: Painter,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enabled: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val contentAlpha = if (enabled) 1f else 0.38f
    val dynamicInternalPadding = (6 * LocalDensity.current.fontScale).dp
    val corners = LocalSettingsItemCorners.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val topRadius by animateDpAsState(
        targetValue = if (pressed) SettingsCornerRadius else corners.topRadius,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "settingsItemTopRadius"
    )
    val bottomRadius by animateDpAsState(
        targetValue = if (pressed) SettingsCornerRadius else corners.bottomRadius,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "settingsItemBottomRadius"
    )
    val shape = RoundedCornerShape(
        topStart = topRadius,
        topEnd = topRadius,
        bottomEnd = bottomRadius,
        bottomStart = bottomRadius
    )
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceBright)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    onClick()
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = {
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = if (LocalDarkMode.current) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        trailingContent = {
            if (trailingContent != null) {
                trailingContent()
            } else {
                TrailingArrowIcon()
            }
        },
        overlineContent = null,
        supportingContent = null,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        elevation = ListItemDefaults.elevation(),
        content = {
            Column {
                Box(
                    modifier = Modifier.padding(
                        top = dynamicInternalPadding,
                        bottom = if (subtitle == null) dynamicInternalPadding else 0.dp
                    )
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal
                        ),
                        color = titleColor.copy(alpha = contentAlpha)
                    )
                }
                subtitle?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = subtitleColor.copy(alpha = contentAlpha),
                        modifier = Modifier.padding(bottom = dynamicInternalPadding)
                    )
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
fun AboutScreenPreview() {
    AboutScreen(
        navController = rememberNavController()
    )
}