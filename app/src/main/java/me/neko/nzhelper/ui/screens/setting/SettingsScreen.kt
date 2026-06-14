package me.neko.nzhelper.ui.screens.setting

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
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
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Upload
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.fragment.app.FragmentActivity
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

    val sessions = remember { mutableStateListOf<Session>() }
    val gson = NzApplication.gson
    val listType = object : TypeToken<List<Session>>() {}.type

    var showClearDialog by remember { mutableStateOf(false) }

    var lockEnabled by remember { mutableStateOf(AppLockManager.isLockEnabled(context)) }

    // 加载数据用于导出
    LaunchedEffect(Unit) {
        val loaded = SessionRepository.loadSessions(context)
        sessions.clear()
        sessions.addAll(loaded)
    }

    // 导入 Launcher
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                scope.launch {
                    val imported = SessionRepository.parseImportFile(context, it, gson, listType)
                    if (imported.isNotEmpty()) {
                        sessions.clear()
                        sessions.addAll(imported)
                        SessionRepository.saveSessions(context, sessions)
                        Toast.makeText(
                            context,
                            "成功导入 ${imported.size} 条记录",
                            Toast.LENGTH_SHORT
                        ).show()
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
                                    text = "导出数据",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = "将记录导出为 JSON 文件",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
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
                                        imageVector = Icons.Outlined.Download,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            headlineContent = {
                                Text(
                                    text = "导入数据",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = "从 JSON 文件恢复记录",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
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
                                        imageVector = Icons.Outlined.DeleteForever,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            headlineContent = {
                                Text(
                                    text = "清除全部记录",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = "删除所有本地历史数据",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
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
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                val activity = context as? FragmentActivity
                                if (activity == null) {
                                    Toast.makeText(context, "无法启动验证", Toast.LENGTH_SHORT)
                                        .show()
                                    return@clickable
                                }

                                val targetState = !lockEnabled

                                if (targetState) {
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
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        headlineContent = {
                            Text(
                                text = "应用锁",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "使用生物识别或锁屏密码解锁",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = lockEnabled,
                                onCheckedChange = { targetState ->
                                    val activity = context as? FragmentActivity
                                    if (activity == null) {
                                        Toast.makeText(context, "无法启动验证", Toast.LENGTH_SHORT)
                                            .show()
                                        return@Switch
                                    }

                                    if (targetState) {
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
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        headlineContent = {
                            Text(
                                text = "关于",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
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
            title = "清除全部记录",
            message = "此操作不可撤销，确定要删除所有记录吗？",
            confirmText = "全部删除",
            onConfirm = {
                sessions.clear()
                scope.launch { SessionRepository.saveSessions(context, sessions) }
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false }
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
