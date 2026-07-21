package me.neko.nzhelper.feature.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import me.neko.nzhelper.core.auto.AutoTagRules
import me.neko.nzhelper.core.crash.CrashLogManager
import me.neko.nzhelper.core.datastore.AgeGroupSettings
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.feature.about.AboutActivity
import me.neko.nzhelper.feature.backup.BackupActivity
import me.neko.nzhelper.feature.crash.CrashLogActivity
import me.neko.nzhelper.feature.lock.AppLockManager
import me.neko.nzhelper.feature.lock.GestureLockManager
import me.neko.nzhelper.feature.lock.GestureLockSetupActivity
import me.neko.nzhelper.feature.recyclebin.RecycleBinSettingsActivity
import me.neko.nzhelper.feature.settings.components.AgePickerBottomSheet
import me.neko.nzhelper.feature.tagmanage.TagManageActivity
import me.neko.nzhelper.ui.component.setting.SettingsCard
import me.neko.nzhelper.ui.component.setting.SettingsDivider
import me.neko.nzhelper.ui.component.setting.SettingsItem
import java.time.LocalDate
import java.time.Period

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen() {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var crashLogCount by remember { mutableIntStateOf(CrashLogManager.listCrashLogs(context).size) }
    var unreadCrashCount by remember { mutableIntStateOf(CrashLogManager.unreadCount(context)) }
    var tagCount by remember { mutableIntStateOf(TagSettings.getTags(context).size) }
    var birthDate by remember {
        mutableStateOf(AgeGroupSettings.getBirthDate(context))
    }
    val age = remember(birthDate) {
        Period.between(birthDate, LocalDate.now()).years
            .coerceIn(AgeGroupSettings.MIN_AGE, AgeGroupSettings.MAX_AGE)
    }
    var showAgeDialog by remember { mutableStateOf(false) }

    var lockEnabled by remember { mutableStateOf(AppLockManager.isLockEnabled(context)) }
    var hasGesturePassword by remember {
        mutableStateOf(GestureLockManager.hasGesturePassword(context))
    }

    var autoStartEnabled by remember {
        mutableStateOf(
            context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
                .getBoolean("auto_start_timer", false)
        )
    }

    var autoTagEnabled by remember {
        mutableStateOf(AutoTagRules.isEnabled(context))
    }
    val toggleAutoTag: (Boolean) -> Unit = { enabled ->
        autoTagEnabled = enabled
        AutoTagRules.setEnabled(context, enabled)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    tagCount = TagSettings.getTags(context).size
                    crashLogCount = CrashLogManager.listCrashLogs(context).size
                    unreadCrashCount = CrashLogManager.unreadCount(context)
                }
                hasGesturePassword = GestureLockManager.hasGesturePassword(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        crashLogCount = CrashLogManager.listCrashLogs(context).size
        unreadCrashCount = CrashLogManager.unreadCount(context)
    }

    val requestToggleLock: (Boolean) -> Unit = { targetState ->
        val activity = context as? FragmentActivity
        if (activity == null) {
            Toast.makeText(context, "无法启动验证", Toast.LENGTH_SHORT).show()
        } else if (targetState) {
            when (AppLockManager.canAuthenticate(context)) {
                BiometricManager.BIOMETRIC_SUCCESS -> {
                    AppLockManager.authenticate(activity, onSuccess = {
                        AppLockManager.setLockEnabled(context, true)
                        lockEnabled = true
                    })
                }

                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    Toast.makeText(
                        context,
                        "未设置锁屏密码或生物识别，请先在系统设置中配置",
                        Toast.LENGTH_LONG
                    ).show()
                }

                else -> {
                    Toast.makeText(context, "设备不支持生物识别或锁屏验证", Toast.LENGTH_LONG)
                        .show()
                }
            }
        } else {
            AppLockManager.authenticate(activity, onSuccess = {
                AppLockManager.setLockEnabled(context, false)
                lockEnabled = false
            })
        }
    }

    val toggleAutoStart: (Boolean) -> Unit = { enabled ->
        autoStartEnabled = enabled
        context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
            .edit { putBoolean("auto_start_timer", enabled) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("设置") },
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
                        icon = Icons.Outlined.Lock,
                        title = "应用锁",
                        subtitle = "使用生物识别或锁屏密码解锁",
                        onClick = { requestToggleLock(!lockEnabled) },
                        trailingContent = {
                            Switch(
                                checked = lockEnabled,
                                onCheckedChange = requestToggleLock
                            )
                        }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Outlined.Gesture,
                        title = "手势密码",
                        subtitle = if (hasGesturePassword) "已开启，点击可设置" else "关闭，点击开启并设置",
                        onClick = {
                            context.startActivity(
                                Intent(
                                    context,
                                    GestureLockSetupActivity::class.java
                                )
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = hasGesturePassword,
                                onCheckedChange = { targetState ->
                                    if (targetState) {
                                        context.startActivity(
                                            Intent(
                                                context,
                                                GestureLockSetupActivity::class.java
                                            )
                                        )
                                    } else {
                                        GestureLockManager.clearGesturePassword(context)
                                        hasGesturePassword = false
                                        Toast.makeText(
                                            context,
                                            "已关闭并清除手势密码",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }
                    )
                }
            }
            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.Cake,
                        title = "年龄",
                        subtitle = "当前：$age 岁",
                        onClick = { showAgeDialog = true }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Outlined.Timer,
                        title = "自动计时",
                        subtitle = "进入首页时自动开始计时",
                        onClick = { toggleAutoStart(!autoStartEnabled) },
                        trailingContent = {
                            Switch(
                                checked = autoStartEnabled,
                                onCheckedChange = toggleAutoStart
                            )
                        }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Outlined.AutoAwesome,
                        title = "自动标签",
                        subtitle = "按时间/星期自动打标签",
                        onClick = { toggleAutoTag(!autoTagEnabled) },
                        trailingContent = {
                            Switch(
                                checked = autoTagEnabled,
                                onCheckedChange = toggleAutoTag
                            )
                        }
                    )
                }
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.Sell,
                        title = "标签管理",
                        subtitle = "分类 · 分组 · 标签（共 $tagCount 个标签）",
                        onClick = {
                            context.startActivity(
                                Intent(
                                    context,
                                    TagManageActivity::class.java
                                )
                            )
                        }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Outlined.CloudSync,
                        title = "备份与恢复",
                        subtitle = "导出 / 导入 / WebDAV 云备份",
                        onClick = {
                            context.startActivity(Intent(context, BackupActivity::class.java))
                        }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Outlined.DeleteOutline,
                        title = "回收站",
                        subtitle = "管理已删除记录",
                        onClick = {
                            context.startActivity(
                                Intent(
                                    context,
                                    RecycleBinSettingsActivity::class.java
                                )
                            )
                        }
                    )
                }
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.BugReport,
                        title = "崩溃日志",
                        subtitle = when {
                            crashLogCount == 0 -> "暂无崩溃记录"
                            unreadCrashCount > 0 -> "共 $crashLogCount 条，$unreadCrashCount 条未读"
                            else -> "共 $crashLogCount 条记录"
                        },
                        onClick = {
                            context.startActivity(Intent(context, CrashLogActivity::class.java))
                        },
                        badgeText = if (unreadCrashCount > 0) "$unreadCrashCount" else null
                    )
                }
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.Info,
                        title = "关于",
                        onClick = {
                            context.startActivity(Intent(context, AboutActivity::class.java))
                        }
                    )
                }
            }
        }
    }

    if (showAgeDialog) {
        AgePickerBottomSheet(
            currentAge = age,
            onConfirm = { selectedBirth ->
                AgeGroupSettings.setBirthDate(context, selectedBirth)
                birthDate = selectedBirth
                showAgeDialog = false
            },
            onDismiss = { showAgeDialog = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen()
}
