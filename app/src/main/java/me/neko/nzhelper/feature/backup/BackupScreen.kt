package me.neko.nzhelper.feature.backup

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import me.neko.nzhelper.core.database.BackupRepository
import me.neko.nzhelper.core.security.BackupCipher
import me.neko.nzhelper.core.webdav.WebDavSettings
import me.neko.nzhelper.ui.component.setting.SettingsCard
import me.neko.nzhelper.ui.component.setting.SettingsDivider
import me.neko.nzhelper.ui.component.setting.SettingsItem
import me.neko.nzhelper.ui.component.setting.TrailingArrowIcon
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var hasCustomBackupPassword by remember { mutableStateOf(BackupCipher.hasCustomPassword(context)) }
    var showBackupPasswordDialog by remember { mutableStateOf(false) }

    var importing by remember { mutableStateOf(false) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            importing = true
            scope.launch {
                val (_, msg) = BackupRepository.importFromUri(context, it)
                importing = false
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val data = BackupRepository.exportNzBytes(context)
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        os.write(data)
                    }
                    Toast.makeText(context, "导出成功", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var showWebDavDialog by remember { mutableStateOf(false) }
    var webDavConfigured by remember { mutableStateOf(WebDavSettings.isConfigured(context)) }
    var webDavBackingUp by remember { mutableStateOf(false) }
    var webDavRestoring by remember { mutableStateOf(false) }
    var webDavLastBackup by remember { mutableLongStateOf(WebDavSettings.getLastBackupTime(context)) }

    val webDavBackupDateStr = remember(webDavLastBackup, configuration) {
        if (webDavLastBackup > 0) {
            val locale = configuration.locales[0] ?: Locale.getDefault()
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", locale)
                .format(java.util.Date(webDavLastBackup))
        } else null
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCustomBackupPassword = BackupCipher.hasCustomPassword(context)
                webDavConfigured = WebDavSettings.isConfigured(context)
                webDavLastBackup = WebDavSettings.getLastBackupTime(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("备份与恢复") },
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
                        icon = Icons.Outlined.Key,
                        title = "备份密码",
                        subtitle = if (hasCustomBackupPassword) "已设置：换手机也能用相同密码恢复备份"
                        else "未设置：备份只能在本机恢复，换手机/重装后打不开",
                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = { showBackupPasswordDialog = true }
                    )
                }
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.Upload,
                        title = "导出数据",
                        subtitle = "导出加密备份，需用密码才能恢复",
                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = {
                            exportLauncher.launch("NzHelper_Backup_${System.currentTimeMillis()}.nz")
                        },
                        trailingContent = { TrailingArrowIcon() }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Outlined.Download,
                        title = "导入数据",
                        subtitle = "从备份文件恢复数据",
                        iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = {
                            importLauncher.launch(
                                arrayOf(
                                    "application/octet-stream",
                                    "application/json",
                                    "text/plain"
                                )
                            )
                        },
                        enabled = !importing,
                        trailingContent = {
                            if (importing) {
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
                            ?: "上传加密备份到 WebDAV 服务器",
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
                                    val (_, msg) = BackupRepository.restoreFromWebDav(context)
                                    webDavRestoring = false
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
        }
    }

    if (showWebDavDialog) {
        WebDavSettingsDialog(onDismiss = {
            showWebDavDialog = false
            webDavConfigured = WebDavSettings.isConfigured(context)
            webDavLastBackup = WebDavSettings.getLastBackupTime(context)
        })
    }

    if (showBackupPasswordDialog) {
        BackupPasswordDialog(
            initialPassword = BackupCipher.getCustomPassword(context) ?: "",
            onConfirm = { pw ->
                BackupCipher.setPassword(context, pw)
                hasCustomBackupPassword = pw.isNotEmpty()
                Toast.makeText(
                    context,
                    if (pw.isNotEmpty()) "备份密码已设置，请务必牢记" else "已改用自动密码，换手机后将无法恢复备份",
                    Toast.LENGTH_LONG
                ).show()
                showBackupPasswordDialog = false
            },
            onDismiss = { showBackupPasswordDialog = false }
        )
    }
}

@Composable
private fun BackupPasswordDialog(
    initialPassword: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf(initialPassword) }
    var passwordVisible by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        icon = {
            Icon(Icons.Outlined.Key, contentDescription = null)
        },
        title = { Text("备份密码") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "备份文件是加密的，必须输对密码才能恢复。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "· 自己设密码：换手机、重装 App 后，输入相同密码就能恢复。\n" +
                            "· 不设置：用自动生成的密码，只能在本机恢复，换手机后备份作废。\n" +
                            "\n输入框留空 = 使用自动密码。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("设置备份密码（留空用自动密码）") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.Visibility
                                else Icons.Outlined.VisibilityOff,
                                contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) { Text("保存") }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) { Text("取消") }
        }
    )
}
