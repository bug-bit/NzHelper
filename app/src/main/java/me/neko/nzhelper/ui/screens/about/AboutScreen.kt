package me.neko.nzhelper.ui.screens.about

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
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
import me.neko.nzhelper.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutScreen(
    navController: NavHostController
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
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
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
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
                            .clip(RoundedCornerShape(22.dp))
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
                }
            }

            item {
                SettingsCard {
                    SettingsItem(
                        painter = painterResource(id = R.drawable.code_24px),
                        title = "GitHub 仓库",
                        subtitle = "我要好多好多小星星✨~",
                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
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

            item {
                SettingsCard {
                    SettingsItem(
                        painter = painterResource(id = R.drawable.ic_telegram),
                        title = "Telegram 群组",
                        subtitle = "@NzHelper",
                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
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

            item {
                SettingsCard {
                    SettingsItem(
                        painter = painterResource(id = R.drawable.source_code_24px),
                        title = "开放源代码",
                        subtitle = "查看第三方开源声明",
                        iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = { navController.navigate("open_source") }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrailingArrowIcon() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.size(20.dp)
    )
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsItem(
    painter: Painter,
    title: String,
    iconContainerColor: Color,
    iconContentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enabled: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val contentAlpha = if (enabled) 1f else 0.5f
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconContainerColor.copy(alpha = contentAlpha)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = iconContentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        headlineContent = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = titleColor.copy(alpha = contentAlpha)
                )
                subtitle?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = subtitleColor.copy(alpha = contentAlpha)
                    )
                }
            }
        },
        trailingContent = {
            if (trailingContent != null) {
                trailingContent()
            } else {
                TrailingArrowIcon()
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Preview(showBackground = true)
@Composable
fun AboutScreenPreview() {
    AboutScreen(
        navController = rememberNavController()
    )
}