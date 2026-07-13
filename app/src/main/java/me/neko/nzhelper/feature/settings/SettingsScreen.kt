package me.neko.nzhelper.feature.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import me.neko.nzhelper.NzApplication
import me.neko.nzhelper.core.auto.AutoTagRules
import me.neko.nzhelper.core.crash.CrashLogManager
import me.neko.nzhelper.core.database.BackupRepository
import me.neko.nzhelper.core.database.RecycleRepository
import me.neko.nzhelper.core.database.SessionRepository
import me.neko.nzhelper.core.datastore.RecycleBinSettings
import me.neko.nzhelper.core.datastore.StorageSettings
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.core.webdav.WebDavSettings
import me.neko.nzhelper.feature.about.AboutActivity
import me.neko.nzhelper.feature.backup.WebDavSettingsDialog
import me.neko.nzhelper.feature.crash.CrashLogActivity
import me.neko.nzhelper.feature.lock.AppLockManager
import me.neko.nzhelper.feature.lock.GestureLockManager
import me.neko.nzhelper.feature.lock.GestureLockSetupActivity
import me.neko.nzhelper.feature.recyclebin.RecycleBinActivity
import me.neko.nzhelper.feature.settings.components.StorageLocationDialog
import me.neko.nzhelper.ui.component.dialog.ConfirmDialog
import me.neko.nzhelper.ui.component.setting.SettingsCard
import me.neko.nzhelper.ui.component.setting.SettingsDivider
import me.neko.nzhelper.ui.component.setting.SettingsItem
import me.neko.nzhelper.ui.component.setting.TrailingArrowIcon
import java.io.OutputStreamWriter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen() {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current

    val sessions = remember { mutableStateListOf<Session>() }
    val gson = NzApplication.gson
    val listType = object : TypeToken<List<Session>>() {}.type

    var showClearDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }

    var recycleBinCount by remember { mutableIntStateOf(0) }
    var crashLogCount by remember { mutableIntStateOf(CrashLogManager.listCrashLogs(context).size) }
    var unreadCrashCount by remember { mutableIntStateOf(CrashLogManager.unreadCount(context)) }
    var tagCount by remember { mutableIntStateOf(TagSettings.getTags(context).size) }
    var autoCleanEnabled by remember { mutableStateOf(RecycleBinSettings.isAutoCleanEnabled(context)) }
    var age by remember {
        mutableIntStateOf(me.neko.nzhelper.core.datastore.AgeGroupSettings.getAge(context))
    }
    var showAgeDialog by remember { mutableStateOf(false) }

    var lockEnabled by remember { mutableStateOf(AppLockManager.isLockEnabled(context)) }
    var hasGesturePassword by remember {
        mutableStateOf(
            GestureLockManager.hasGesturePassword(
                context
            )
        )
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

    var storageMode by remember { mutableStateOf(StorageSettings.getMode(context)) }
    var pendingStorageSwitch by remember { mutableStateOf<Pair<String, String>?>(null) }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingStorageSwitch?.let { (mode, path) ->
            if (granted) {
                scope.launch {
                    val success = BackupRepository.switchStorageMode(context, mode, path)
                    if (success) {
                        storageMode = mode
                        val loaded = SessionRepository.loadSessions(context)
                        sessions.clear()
                        sessions.addAll(loaded)
                        Toast.makeText(context, "存储位置已切换，记录已合并去重", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(context, "切换失败，请检查路径是否可写", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } else {
                Toast.makeText(context, "需要存储权限才能切换到外部存储", Toast.LENGTH_SHORT).show()
            }
            pendingStorageSwitch = null
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    val loaded = SessionRepository.loadSessions(context)
                    sessions.clear()
                    sessions.addAll(loaded)
                    recycleBinCount = RecycleRepository.loadRecycleBin(context).size
                    tagCount = TagSettings.getTags(context).size
                    crashLogCount = CrashLogManager.listCrashLogs(context).size
                    unreadCrashCount = CrashLogManager.unreadCount(context)
                }
                hasGesturePassword = GestureLockManager.hasGesturePassword(context)
                pendingStorageSwitch?.let { (mode, path) ->
                    if (StorageSettings.hasExternalStoragePermission(context)) {
                        scope.launch {
                            val success = BackupRepository.switchStorageMode(context, mode, path)
                            if (success) {
                                storageMode = mode
                                val loaded = SessionRepository.loadSessions(context)
                                sessions.clear()
                                sessions.addAll(loaded)
                                recycleBinCount = RecycleRepository.loadRecycleBin(context).size
                                Toast.makeText(
                                    context,
                                    "存储位置已切换，记录已合并去重",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "切换失败，请检查路径是否可写",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        pendingStorageSwitch = null
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        val loaded = SessionRepository.loadSessions(context)
        sessions.clear()
        sessions.addAll(loaded)
        recycleBinCount = RecycleRepository.loadRecycleBin(context).size
        crashLogCount = CrashLogManager.listCrashLogs(context).size
        unreadCrashCount = CrashLogManager.unreadCount(context)
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val imported = SessionRepository.parseImportFile(context, it, gson, listType)
                if (imported.isNotEmpty()) {
                    val existingTimestamps = sessions.map { it.timestamp }.toSet()
                    val newSessions = imported.filter { it.timestamp !in existingTimestamps }
                    sessions.addAll(newSessions)
                    SessionRepository.saveSessions(context, sessions)
                    val mergedCount = newSessions.size
                    val skippedCount = imported.size - mergedCount
                    val msg = if (skippedCount > 0) {
                        "成功导入 $mergedCount 条新记录，跳过 $skippedCount 条重复记录"
                    } else {
                        "成功导入 $mergedCount 条记录"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "导入失败：文件格式不正确或为空", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        OutputStreamWriter(os).use { writer -> writer.write(gson.toJson(sessions)) }
                    }
                    Toast.makeText(context, "导出成功", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val performStorageSwitch: (String, String) -> Unit = { mode, path ->
        if (mode == StorageSettings.MODE_EXTERNAL && !StorageSettings.hasExternalStoragePermission(
                context
            )
        ) {
            pendingStorageSwitch = mode to path
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        "package:${context.packageName}".toUri()
                    )
                    context.startActivity(intent)
                } catch (_: Exception) {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            } else {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else {
            scope.launch {
                val success = BackupRepository.switchStorageMode(context, mode, path)
                if (success) {
                    storageMode = mode
                    val loaded = SessionRepository.loadSessions(context)
                    sessions.clear()
                    sessions.addAll(loaded)
                    Toast.makeText(context, "存储位置已切换，记录已合并去重", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(context, "切换失败，请检查路径是否可写", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    var showWebDavDialog by remember { mutableStateOf(false) }
    var webDavConfigured by remember { mutableStateOf(WebDavSettings.isConfigured(context)) }
    var webDavBackingUp by remember { mutableStateOf(false) }
    var webDavRestoring by remember { mutableStateOf(false) }
    var webDavLastBackup by remember { mutableLongStateOf(WebDavSettings.getLastBackupTime(context)) }

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

    val webDavBackupDateStr = remember(webDavLastBackup, configuration) {
        if (webDavLastBackup > 0) {
            val locale = configuration.locales[0] ?: Locale.getDefault()
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", locale)
                .format(java.util.Date(webDavLastBackup))
        } else null
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
                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
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
                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = {
                            if (!hasGesturePassword) {
                                context.startActivity(
                                    Intent(
                                        context,
                                        GestureLockSetupActivity::class.java
                                    )
                                )
                            } else {
                                context.startActivity(
                                    Intent(
                                        context,
                                        GestureLockSetupActivity::class.java
                                    )
                                )
                            }
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
                        iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = { showAgeDialog = true }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Outlined.Timer,
                        title = "自动计时",
                        subtitle = "进入首页时自动开始计时",
                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
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
                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
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
                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = {
                            context.startActivity(
                                Intent(
                                    context,
                                    TagManageActivity::class.java
                                )
                            )
                        }
                    )
                }
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.FolderOpen,
                        title = "数据存储位置",
                        subtitle = if (storageMode == StorageSettings.MODE_INTERNAL) "当前：应用内部存储"
                        else "当前：${StorageSettings.getExternalPath(context)}",
                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = { showStorageDialog = true }
                    )
                }
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.Delete,
                        title = "回收站",
                        subtitle = if (recycleBinCount > 0) "共 $recycleBinCount 条记录，点击管理"
                        else "暂无已删除的记录",
                        iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = {
                            context.startActivity(Intent(context, RecycleBinActivity::class.java))
                        },
                        badgeText = if (recycleBinCount > 0) "$recycleBinCount" else null
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Outlined.AutoDelete,
                        title = "自动清理回收站",
                        subtitle = if (autoCleanEnabled) "已开启，记录将在 30 天后自动永久删除"
                        else "已关闭，记录将一直保留在回收站中",
                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
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
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Outlined.DeleteSweep,
                        title = "移入回收站",
                        subtitle = "将所有记录移入回收站，可从回收站恢复",
                        titleColor = MaterialTheme.colorScheme.error,
                        iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                        iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
                        onClick = { showClearDialog = true }
                    )
                }
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.Upload,
                        title = "导出数据",
                        subtitle = "将记录导出为 JSON 文件",
                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = { exportLauncher.launch("NzHelper_Export_${System.currentTimeMillis()}.json") }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Outlined.Download,
                        title = "导入数据",
                        subtitle = "从 JSON 文件恢复记录",
                        iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = {
                            importLauncher.launch(
                                arrayOf(
                                    "application/json",
                                    "text/plain"
                                )
                            )
                        }
                    )
                }
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.Cloud,
                        title = "WebDAV 备份",
                        subtitle = if (webDavConfigured) "已配置，点击修改" else "未配置，点击设置",
                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = { showWebDavDialog = true }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Outlined.CloudUpload,
                        title = "云端备份",
                        subtitle = webDavBackupDateStr?.let { "上次备份：$it" }
                            ?: "上传记录到 WebDAV 服务器",
                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = {
                            if (webDavConfigured && !webDavBackingUp) {
                                webDavBackingUp = true
                                scope.launch {
                                    val (_, msg) = BackupRepository.backupToWebDav(context)
                                    webDavBackingUp = false
                                    webDavLastBackup = WebDavSettings.getLastBackupTime(context)
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = webDavConfigured && !webDavBackingUp,
                        trailingContent = {
                            if (webDavBackingUp) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                TrailingArrowIcon()
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Outlined.CloudDownload,
                        title = "云端恢复",
                        subtitle = "从 WebDAV 恢复并合并到本地",
                        iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = {
                            if (webDavConfigured && !webDavRestoring) {
                                webDavRestoring = true
                                scope.launch {
                                    val (ok, msg) = BackupRepository.restoreFromWebDav(context)
                                    webDavRestoring = false
                                    if (ok) {
                                        val loaded = SessionRepository.loadSessions(context)
                                        sessions.clear()
                                        sessions.addAll(loaded)
                                        recycleBinCount =
                                            RecycleRepository.loadRecycleBin(context).size
                                    }
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = webDavConfigured && !webDavRestoring,
                        trailingContent = {
                            if (webDavRestoring) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                TrailingArrowIcon()
                            }
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
                        iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                        iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
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
                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = {
                            context.startActivity(Intent(context, AboutActivity::class.java))
                        }
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
                    sessions.clear()
                    recycleBinCount = RecycleRepository.loadRecycleBin(context).size
                }
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false }
        )
    }

    if (showAgeDialog) {
        me.neko.nzhelper.feature.settings.components.AgeSliderDialog(
            currentAge = age,
            onConfirm = { selectedAge ->
                me.neko.nzhelper.core.datastore.AgeGroupSettings.setAge(context, selectedAge)
                age = selectedAge
                showAgeDialog = false
            },
            onDismiss = { showAgeDialog = false }
        )
    }

    if (showStorageDialog) {
        StorageLocationDialog(
            currentMode = storageMode,
            currentPath = StorageSettings.getExternalPath(context),
            onConfirm = { mode, path ->
                performStorageSwitch(mode, path)
                showStorageDialog = false
            },
            onDismiss = { showStorageDialog = false }
        )
    }

    if (showWebDavDialog) {
        WebDavSettingsDialog(onDismiss = {
            showWebDavDialog = false
            webDavConfigured = WebDavSettings.isConfigured(context)
            webDavLastBackup = WebDavSettings.getLastBackupTime(context)
        })
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(
    )
}
