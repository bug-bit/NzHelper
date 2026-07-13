package me.neko.nzhelper.feature.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.neko.nzhelper.core.database.BackupRepository
import me.neko.nzhelper.ui.component.dialog.ConfirmDialog
import me.neko.nzhelper.core.webdav.WebDavSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDavSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf(WebDavSettings.getUrl(context)) }
    var username by remember { mutableStateOf(WebDavSettings.getUsername(context)) }
    var password by remember { mutableStateOf(WebDavSettings.getPassword(context)) }
    var remotePath by remember { mutableStateOf(WebDavSettings.getRemotePath(context)) }
    var autoBackup by remember { mutableStateOf(WebDavSettings.isAutoBackupEnabled(context)) }

    var showPassword by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var savedMsg by remember { mutableStateOf<String?>(null) }

    var showClearConfirmDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        icon = {
            Icon(
                imageVector = Icons.Outlined.CloudUpload,
                contentDescription = null
            )
        },
        title = { Text("WebDAV 备份设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("服务器地址") },
                    placeholder = { Text("https://dav.example.com/dav") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Outlined.VisibilityOff
                                else Icons.Outlined.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = remotePath,
                    onValueChange = { remotePath = it },
                    label = { Text("远程备份目录") },
                    placeholder = { Text("/NzHelper") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "自动备份",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(checked = autoBackup, onCheckedChange = { autoBackup = it })
                }
                Text(
                    "开启后每次保存记录都会自动上传到 WebDAV",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                testResult?.let { (success, msg) ->
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (success) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
                savedMsg?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (url.isBlank() || username.isBlank()) {
                            testResult = false to "请填写服务器地址和用户名"
                            return@OutlinedButton
                        }
                        testing = true
                        testResult = null
                        scope.launch {
                            val r = BackupRepository.testWebDavConnection(url, username, password)
                            testing = false
                            testResult = r
                        }
                    },
                    enabled = !testing,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(if (testing) "测试中…" else "测试连接")
                }

                Button(
                    onClick = {
                        WebDavSettings.save(context, url, username, password, remotePath)
                        WebDavSettings.setAutoBackupEnabled(context, autoBackup)
                        savedMsg = "已保存"
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("保存")
                }
            }
        },
        dismissButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (WebDavSettings.isConfigured(context)) {
                    OutlinedButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("清除配置")
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("取消")
                    }
                } else {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("取消")
                    }
                }
            }
        }
    )

    if (showClearConfirmDialog) {
        ConfirmDialog(
            icon = Icons.Outlined.Warning,
            title = "清除配置",
            message = "确定要清除 WebDAV 配置吗？这将清空已保存的服务器地址和凭据。",
            confirmText = "清除",
            onConfirm = {
                WebDavSettings.clear(context)
                url = ""
                username = ""
                password = ""
                remotePath = WebDavSettings.getRemotePath(context)
                autoBackup = false
                testResult = null
                savedMsg = "已清除配置"
                showClearConfirmDialog = false
            },
            onDismiss = { showClearConfirmDialog = false }
        )
    }
}