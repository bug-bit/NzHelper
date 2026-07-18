package me.neko.nzhelper.feature.recyclebin

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import me.neko.nzhelper.core.database.RecycleRepository
import me.neko.nzhelper.core.datastore.RecycleBinSettings
import me.neko.nzhelper.ui.component.dialog.ConfirmDialog
import me.neko.nzhelper.ui.component.setting.SettingsCard
import me.neko.nzhelper.ui.component.setting.SettingsDivider
import me.neko.nzhelper.ui.component.setting.SettingsItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RecycleBinSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var showClearDialog by remember { mutableStateOf(false) }
    var recycleBinCount by remember { mutableIntStateOf(0) }
    var autoCleanEnabled by remember { mutableStateOf(RecycleBinSettings.isAutoCleanEnabled(context)) }

    LaunchedEffect(Unit) {
        recycleBinCount = RecycleRepository.loadRecycleBin(context).size
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    recycleBinCount = RecycleRepository.loadRecycleBin(context).size
                }
                autoCleanEnabled = RecycleBinSettings.isAutoCleanEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("回收站") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                    SettingsItem(
                        icon = Icons.Outlined.Delete,
                        title = "回收站记录",
                        subtitle = if (recycleBinCount > 0) "共 $recycleBinCount 条记录，点击管理"
                        else "暂无已删除的记录",
                        onClick = {
                            context.startActivity(Intent(context, RecycleBinActivity::class.java))
                        },
                        badgeText = if (recycleBinCount > 0) "$recycleBinCount" else null
                    )
                }
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.AutoDelete,
                        title = "自动清理回收站",
                        subtitle = if (autoCleanEnabled) "已开启，记录将在 30 天后自动永久删除"
                        else "已关闭，记录将一直保留在回收站中",
                        onClick = {
                            autoCleanEnabled = !autoCleanEnabled
                            RecycleBinSettings.setAutoCleanEnabled(context, autoCleanEnabled)
                        },
                        trailingContent = {
                            Switch(
                                checked = autoCleanEnabled,
                                onCheckedChange = { enabled ->
                                    autoCleanEnabled = enabled
                                    RecycleBinSettings.setAutoCleanEnabled(context, enabled)
                                }
                            )
                        }
                    )
                }
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.DeleteSweep,
                        title = "移入回收站",
                        subtitle = "将所有记录移入回收站，可从回收站恢复",
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = { showClearDialog = true }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        ConfirmDialog(
            icon = Icons.Default.Warning,
            title = "移入回收站",
            message = "确定要将所有记录移入回收站吗？可从回收站恢复。",
            confirmText = "移入回收站",
            onConfirm = {
                scope.launch {
                    RecycleRepository.moveAllToRecycleBin(context)
                    recycleBinCount = RecycleRepository.loadRecycleBin(context).size
                    Toast.makeText(context, "已移入回收站", Toast.LENGTH_SHORT).show()
                }
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false }
        )
    }
}
