package me.neko.nzhelper.feature.backup

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import me.neko.nzhelper.core.database.RecycleRepository
import me.neko.nzhelper.core.database.SessionRepository
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.BackupModules
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

    var pendingExportModules by remember { mutableStateOf<BackupModules?>(null) }
    var exportCounts by remember { mutableStateOf<ModuleCounts?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        val modules = pendingExportModules
        pendingExportModules = null
        uri?.let {
            scope.launch {
                try {
                    val data = BackupRepository.exportNzBytes(context, modules ?: BackupModules.ALL)
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

    suspend fun loadLocalCounts(): ModuleCounts {
        val sessions = SessionRepository.loadSessions(context).size
        val recycle = RecycleRepository.loadRecycleBin(context).size
        val taxonomy = TagSettings.getCategories(context).size +
                TagSettings.getGroups(context).size +
                TagSettings.getTags(context).size
        return ModuleCounts(sessions, recycle, taxonomy)
    }

    var importing by remember { mutableStateOf(false) }
    var pendingImportPreview by remember { mutableStateOf<BackupRepository.BackupPreview?>(null) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            importing = true
            scope.launch {
                val (preview, msg) = BackupRepository.previewFromUri(context, it)
                importing = false
                if (preview == null) {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                } else {
                    pendingImportPreview = preview
                }
            }
        }
    }

    var showWebDavDialog by remember { mutableStateOf(false) }
    var webDavConfigured by remember { mutableStateOf(WebDavSettings.isConfigured(context)) }
    var webDavBackingUp by remember { mutableStateOf(false) }
    var webDavRestoring by remember { mutableStateOf(false) }
    var webDavLastBackup by remember { mutableLongStateOf(WebDavSettings.getLastBackupTime(context)) }

    var pendingWebDavBackupModules by remember { mutableStateOf<BackupModules?>(null) }
    var pendingWebDavRestorePreview by remember {
        mutableStateOf<BackupRepository.BackupPreview?>(
            null
        )
    }

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
                        onClick = { showBackupPasswordDialog = true }
                    )
                }
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.Upload,
                        title = "导出数据",
                        subtitle = "将所选内容加密导出为备份文件，恢复时需输入密码验证",
                        onClick = {
                            scope.launch {
                                exportCounts = loadLocalCounts()
                                pendingExportModules = BackupModules.ALL
                            }
                        },
                        trailingContent = { TrailingArrowIcon() }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Outlined.Download,
                        title = "导入数据",
                        subtitle = "从备份文件选择要恢复的内容",
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
                        onClick = { showWebDavDialog = true }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Outlined.CloudUpload,
                        title = "云端备份",
                        subtitle = webDavBackupDateStr?.let { "上次备份：$it" }
                            ?: "选择要备份的内容，加密导出并上传至 WebDAV 服务器，恢复时需凭密码解锁",
                        onClick = {
                            if (webDavConfigured && !webDavBackingUp) {
                                scope.launch {
                                    exportCounts = loadLocalCounts()
                                    pendingWebDavBackupModules = BackupModules.ALL
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
                        subtitle = "从 WebDAV 选择内容恢复并合并到本地",
                        onClick = {
                            if (webDavConfigured && !webDavRestoring) {
                                webDavRestoring = true
                                scope.launch {
                                    val (preview, msg) = BackupRepository.previewFromWebDav(context)
                                    webDavRestoring = false
                                    if (preview == null) {
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    } else {
                                        pendingWebDavRestorePreview = preview
                                    }
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

    pendingExportModules?.let { modules ->
        val counts = exportCounts
        if (counts != null) {
            BackupModuleDialog(
                title = "选择备份内容",
                confirmText = "导出",
                icon = Icons.Outlined.Upload,
                modules = modules,
                counts = counts,
                onConfirm = { selected ->
                    if (selected.noneSelected) {
                        Toast.makeText(context, "请至少选择一项", Toast.LENGTH_SHORT).show()
                    } else {
                        pendingExportModules = selected
                        exportCounts = null
                        exportLauncher.launch("NzHelper_Backup_${System.currentTimeMillis()}.nz")
                    }
                },
                onDismiss = {
                    pendingExportModules = null
                    exportCounts = null
                }
            )
        }
    }

    pendingImportPreview?.let { preview ->
        BackupModuleDialog(
            title = "选择恢复内容",
            confirmText = "恢复",
            icon = Icons.Outlined.Download,
            modules = BackupModules.ALL,
            counts = ModuleCounts(
                sessions = preview.sessionCount,
                recycleBin = preview.recycleCount,
                taxonomy = preview.taxonomyCount
            ),
            legacySessionsOnly = preview.legacySessionsOnly,
            onConfirm = { selected ->
                val p = preview
                pendingImportPreview = null
                importing = true
                scope.launch {
                    val (_, msg) = BackupRepository.applyPreview(context, p, selected)
                    importing = false
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { pendingImportPreview = null }
        )
    }

    pendingWebDavBackupModules?.let { modules ->
        val counts = exportCounts
        if (counts != null) {
            BackupModuleDialog(
                title = "选择备份内容",
                confirmText = "上传",
                icon = Icons.Outlined.CloudUpload,
                modules = modules,
                counts = counts,
                onConfirm = { selected ->
                    pendingWebDavBackupModules = null
                    exportCounts = null
                    if (selected.noneSelected) {
                        Toast.makeText(context, "请至少选择一项", Toast.LENGTH_SHORT).show()
                    } else {
                        webDavBackingUp = true
                        scope.launch {
                            val (_, msg) = BackupRepository.backupToWebDav(context, selected)
                            webDavBackingUp = false
                            webDavLastBackup = WebDavSettings.getLastBackupTime(context)
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onDismiss = {
                    pendingWebDavBackupModules = null
                    exportCounts = null
                }
            )
        }
    }

    pendingWebDavRestorePreview?.let { preview ->
        BackupModuleDialog(
            title = "选择恢复内容",
            confirmText = "恢复",
            icon = Icons.Outlined.CloudDownload,
            modules = BackupModules.ALL,
            counts = ModuleCounts(
                sessions = preview.sessionCount,
                recycleBin = preview.recycleCount,
                taxonomy = preview.taxonomyCount
            ),
            legacySessionsOnly = preview.legacySessionsOnly,
            onConfirm = { selected ->
                val p = preview
                pendingWebDavRestorePreview = null
                webDavRestoring = true
                scope.launch {
                    val (_, msg) = BackupRepository.applyPreview(context, p, selected)
                    webDavRestoring = false
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { pendingWebDavRestorePreview = null }
        )
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
                    if (pw.isNotEmpty()) "备份密码已设置，请务必牢记，丢失后数据无法恢复！" else "已改用自动密码，换手机后将无法恢复备份",
                    Toast.LENGTH_LONG
                ).show()
                showBackupPasswordDialog = false
            },
            onDismiss = { showBackupPasswordDialog = false }
        )
    }
}

private data class ModuleCounts(
    val sessions: Int,
    val recycleBin: Int,
    val taxonomy: Int
)

@Composable
private fun BackupModuleDialog(
    title: String,
    confirmText: String,
    icon: ImageVector,
    modules: BackupModules,
    counts: ModuleCounts,
    legacySessionsOnly: Boolean = false,
    onConfirm: (BackupModules) -> Unit,
    onDismiss: () -> Unit
) {
    var sessions by remember { mutableStateOf(modules.sessions) }
    var recycleBin by remember { mutableStateOf(modules.recycleBin && !legacySessionsOnly) }
    var taxonomy by remember { mutableStateOf(modules.taxonomy && !legacySessionsOnly) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        icon = {
            Icon(icon, contentDescription = null)
        },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (legacySessionsOnly) {
                    Text(
                        "此备份为旧格式，仅包含记录，只能恢复记录。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ModuleRow(
                    icon = Icons.Outlined.FileOpen,
                    label = "记录",
                    count = counts.sessions,
                    checked = sessions,
                    onCheckedChange = { sessions = it }
                )
                ModuleRow(
                    icon = Icons.Outlined.Recycling,
                    label = "回收站",
                    count = counts.recycleBin,
                    checked = recycleBin,
                    enabled = !legacySessionsOnly,
                    onCheckedChange = { recycleBin = it }
                )
                ModuleRow(
                    icon = Icons.Outlined.Sell,
                    label = "标签体系",
                    count = counts.taxonomy,
                    checked = taxonomy,
                    enabled = !legacySessionsOnly,
                    onCheckedChange = { taxonomy = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(BackupModules(sessions, recycleBin, taxonomy))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) { Text(confirmText) }
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

@Composable
private fun ModuleRow(
    icon: ImageVector,
    label: String,
    count: Int,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
