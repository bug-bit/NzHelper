package me.neko.nzhelper.ui.screens.setting

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
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import me.neko.nzhelper.NzApplication
import me.neko.nzhelper.data.Session
import me.neko.nzhelper.data.SessionRepository
import me.neko.nzhelper.ui.screens.history.ConfirmDialog
import me.neko.nzhelper.ui.screens.lock.AppLockManager
import java.io.OutputStreamWriter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    navController: NavController
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val sessions = remember { mutableStateListOf<Session>() }
    val gson = NzApplication.gson
    val listType = object : TypeToken<List<Session>>() {}.type

    var showClearDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }

    var recycleBinCount by remember { mutableIntStateOf(0) }
    var autoCleanEnabled by remember { mutableStateOf(RecycleBinSettings.isAutoCleanEnabled(context)) }

    var lockEnabled by remember { mutableStateOf(AppLockManager.isLockEnabled(context)) }

    var autoStartEnabled by remember {
        mutableStateOf(
            context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
                .getBoolean("auto_start_timer", false)
        )
    }

    var storageMode by remember { mutableStateOf(StorageSettings.getMode(context)) }

    var pendingStorageSwitch by remember { mutableStateOf<Pair<String, String>?>(null) }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingStorageSwitch?.let { (mode, path) ->
            if (granted) {
                scope.launch {
                    val success = SessionRepository.switchStorageMode(context, mode, path)
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
                    recycleBinCount = SessionRepository.loadRecycleBin(context).size
                }
                pendingStorageSwitch?.let { (mode, path) ->
                    if (StorageSettings.hasExternalStoragePermission(context)) {
                        scope.launch {
                            val success = SessionRepository.switchStorageMode(context, mode, path)
                            if (success) {
                                storageMode = mode
                                val loaded = SessionRepository.loadSessions(context)
                                sessions.clear()
                                sessions.addAll(loaded)
                                recycleBinCount =
                                    SessionRepository.loadRecycleBin(context).size
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

    // 加载数据用于导出
    LaunchedEffect(Unit) {
        val loaded = SessionRepository.loadSessions(context)
        sessions.clear()
        sessions.addAll(loaded)
        recycleBinCount = SessionRepository.loadRecycleBin(context).size
    }

    // 导入 Launcher
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let { it ->
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

    // 导出 Launcher
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

    // 执行存储切换
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
                val success = SessionRepository.switchStorageMode(context, mode, path)
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

    var showPropsDialog by remember { mutableStateOf(false) }
    var showMoodDialog by remember { mutableStateOf(false) }
    var customProps by remember { mutableStateOf(CategorySettings.getProps(context)) }
    var customMoods by remember { mutableStateOf(CategorySettings.getMoods(context)) }
    var customLocations by remember { mutableStateOf(CategorySettings.getLocations(context)) }
    var showLocationDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("设置") },
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
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
                ) {
                    // 生物识别验证逻辑
                    val requestToggleLock: (Boolean) -> Unit = { targetState ->
                        val activity = context as? FragmentActivity
                        if (activity == null) {
                            Toast.makeText(context, "无法启动验证", Toast.LENGTH_SHORT).show()
                        } else if (targetState) {
                            when (AppLockManager.canAuthenticate(context)) {
                                BiometricManager.BIOMETRIC_SUCCESS -> {
                                    AppLockManager.authenticate(
                                        activity,
                                        onSuccess = {
                                            AppLockManager.setLockEnabled(context, true)
                                            lockEnabled = true
                                        }
                                    )
                                }

                                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                                    Toast.makeText(
                                        context,
                                        "未设置锁屏密码或生物识别，请先在系统设置中配置",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                                else -> {
                                    Toast.makeText(
                                        context,
                                        "设备不支持生物识别或锁屏验证",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        } else {
                            AppLockManager.authenticate(
                                activity,
                                onSuccess = {
                                    AppLockManager.setLockEnabled(context, false)
                                    lockEnabled = false
                                }
                            )
                        }
                    }
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = LocalIndication.current
                            ) {
                                requestToggleLock(!lockEnabled)
                            },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Lock,
                                    null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        headlineContent = {
                            Text(
                                "应用锁",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supportingContent = {
                            Text(
                                "使用生物识别或锁屏密码解锁",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = lockEnabled,
                                onCheckedChange = requestToggleLock
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    val toggleAutoStart: (Boolean) -> Unit = { enabled ->
                        autoStartEnabled = enabled
                        context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
                            .edit { putBoolean("auto_start_timer", enabled) }
                    }
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = LocalIndication.current
                            ) {
                                toggleAutoStart(!autoStartEnabled)
                            },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Timer,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        headlineContent = {
                            Text(
                                "自动计时",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supportingContent = {
                            Text(
                                "进入首页时自动开始计时",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = autoStartEnabled,
                                onCheckedChange = toggleAutoStart
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    )
                ) {
                    Column {
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLocationDialog = true },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Place, null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            headlineContent = {
                                Text(
                                    "自定义地点",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            supportingContent = {
                                Text(
                                    "共 ${customLocations.size} 项，点击管理",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            trailingContent = {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPropsDialog = true },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Category, null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            headlineContent = {
                                Text(
                                    "自定义道具",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            supportingContent = {
                                Text(
                                    "共 ${customProps.size} 项，点击管理",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            trailingContent = {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showMoodDialog = true },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Mood, null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            headlineContent = {
                                Text(
                                    "自定义心情",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            supportingContent = {
                                Text(
                                    "共 ${customMoods.size} 项，点击管理",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            trailingContent = {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
                ) {
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStorageDialog = true },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        headlineContent = {
                            Text(
                                "数据存储位置",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supportingContent = {
                            Text(
                                if (storageMode == StorageSettings.MODE_INTERNAL) "当前：应用内部存储"
                                else "当前：${StorageSettings.getExternalPath(context)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
                ) {
                    Column {
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate("recycle_bin") },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Delete, null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            headlineContent = {
                                Text(
                                    "回收站",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            supportingContent = {
                                Text(
                                    if (recycleBinCount > 0) "共 $recycleBinCount 条记录，点击管理"
                                    else "暂无已删除的记录",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            trailingContent = {
                                if (recycleBinCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge { Text("$recycleBinCount") }
                                        }
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.5f
                                            ),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newEnabled = !autoCleanEnabled
                                    autoCleanEnabled = newEnabled
                                    RecycleBinSettings.setAutoCleanEnabled(context, newEnabled)
                                },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.AutoDelete,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            headlineContent = {
                                Text(
                                    "自动清理回收站",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            supportingContent = {
                                Text(
                                    if (autoCleanEnabled) "已开启，记录将在 30 天后自动永久删除"
                                    else "已关闭，记录将一直保留在回收站中",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = autoCleanEnabled,
                                    onCheckedChange = { enabled ->
                                        autoCleanEnabled = enabled
                                        RecycleBinSettings.setAutoCleanEnabled(context, enabled)
                                    }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showClearDialog = true },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.DeleteSweep, null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            headlineContent = {
                                Text(
                                    "移入回收站",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            supportingContent = {
                                Text(
                                    "将所有记录移入回收站，可从回收站恢复",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
                ) {
                    Column {
                        // 导出数据
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    exportLauncher.launch("NzHelper_Export_${System.currentTimeMillis()}.json")
                                },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Upload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            headlineContent = {
                                Text(
                                    "导出数据",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            supportingContent = {
                                Text(
                                    "将记录导出为 JSON 文件",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            trailingContent = {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // 导入数据
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    importLauncher.launch(arrayOf("application/json", "text/plain"))
                                },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Download, null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            headlineContent = {
                                Text(
                                    "导入数据",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            supportingContent = {
                                Text(
                                    "从 JSON 文件恢复记录",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            trailingContent = {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
                ) {
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("about") },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Info,
                                    null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        headlineContent = {
                            Text(
                                "关于",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
                    SessionRepository.moveAllToRecycleBin(context)
                    sessions.clear()
                    recycleBinCount = SessionRepository.loadRecycleBin(context).size
                }
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false }
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

    if (showLocationDialog) {
        CategoryManageDialog(
            title = "管理地点",
            items = customLocations,
            onAdd = {
                CategorySettings.addLocation(context, it)
                customLocations = CategorySettings.getLocations(context)
            },
            onRemove = {
                CategorySettings.removeLocation(context, it)
                customLocations = CategorySettings.getLocations(context)
            },
            onReset = {
                CategorySettings.resetLocations(context)
                customLocations = CategorySettings.getLocations(context)
            },
            onDismiss = { showLocationDialog = false }
        )
    }

    if (showPropsDialog) {
        CategoryManageDialog(
            title = "管理道具",
            items = customProps,
            onAdd = {
                CategorySettings.addProp(context, it)
                customProps = CategorySettings.getProps(context)
            },
            onRemove = {
                CategorySettings.removeProp(context, it)
                customProps = CategorySettings.getProps(context)
            },
            onReset = {
                CategorySettings.resetProps(context)
                customProps = CategorySettings.getProps(context)
            },
            onDismiss = { showPropsDialog = false }
        )
    }

    if (showMoodDialog) {
        CategoryManageDialog(
            title = "管理心情",
            items = customMoods,
            onAdd = {
                CategorySettings.addMood(context, it)
                customMoods = CategorySettings.getMoods(context)
            },
            onRemove = {
                CategorySettings.removeMood(context, it)
                customMoods = CategorySettings.getMoods(context)
            },
            onReset = {
                CategorySettings.resetMoods(context)
                customMoods = CategorySettings.getMoods(context)
            },
            onDismiss = { showMoodDialog = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(
        navController = rememberNavController()
    )
}
