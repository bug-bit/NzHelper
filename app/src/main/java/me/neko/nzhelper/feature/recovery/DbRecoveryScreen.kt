package me.neko.nzhelper.feature.recovery

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.neko.nzhelper.core.database.BackupRepository
import me.neko.nzhelper.core.database.DbBootstrap
import me.neko.nzhelper.ui.component.dialog.ConfirmDialog

@Composable
fun DbRecoveryScreen(
    issue: DbBootstrap.Issue,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPreview by remember { mutableStateOf<BackupRepository.BackupPreview?>(null) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        pendingUri = uri
        status = null
        busy = true
        scope.launch {
            val (preview, message) = BackupRepository.previewFromUri(context, uri)
            busy = false
            if (preview != null) {
                pendingPreview = preview
            } else {
                status = message
                password = ""
                passwordError = null
                showPasswordDialog = true
            }
        }
    }

    val preview = pendingPreview
    if (preview != null) {
        ConfirmDialog(
            icon = Icons.Filled.Warning,
            title = "确认恢复",
            message = "将归档当前数据库文件，用新密钥重建数据库，然后导入备份中的 " +
                    "${preview.sessionCount} 条记录、${preview.recycleCount} 项回收站内容。",
            confirmText = "恢复",
            onConfirm = {
                pendingPreview = null
                busy = true
                status = null
                scope.launch {
                    val (ok, message) = DbBootstrap.restoreFromBackup(context, preview)
                    busy = false
                    if (!ok) status = message
                }
            },
            onDismiss = { pendingPreview = null }
        )
    }

    if (showPasswordDialog) {
        val uri = pendingUri
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("输入备份密码") },
            text = {
                Column {
                    Text(
                        text = "旧备份文件是用它导出时的密码加密的，本机已无法自动读取该密码。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null
                        },
                        label = { Text("备份密码") },
                        singleLine = true,
                        isError = passwordError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val error = passwordError
                    if (error != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (uri == null) {
                            showPasswordDialog = false
                            return@Button
                        }
                        if (password.isEmpty()) {
                            passwordError = "请输入备份密码"
                            return@Button
                        }
                        busy = true
                        scope.launch {
                            val (result, message) =
                                BackupRepository.previewFromUriWithPassword(context, uri, password)
                            busy = false
                            if (result == null) {
                                passwordError = message
                            } else {
                                showPasswordDialog = false
                                pendingPreview = result
                            }
                        }
                    },
                    enabled = !busy,
                    shape = MaterialTheme.shapes.large
                ) { Text("读取备份") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showPasswordDialog = false },
                    shape = MaterialTheme.shapes.large
                ) { Text("取消") }
            }
        )
    }

    if (showResetConfirm) {
        ConfirmDialog(
            icon = Icons.Filled.Warning,
            title = "放弃旧数据并重新开始？",
            message = "将用新密钥建立空数据库。旧数据库文件不会被删除，会改名保留在应用目录中，" +
                    "但在本机无法再解密，其中的记录将不再出现在应用里。",
            confirmText = "重新开始",
            onConfirm = {
                showResetConfirm = false
                busy = true
                status = null
                scope.launch {
                    val (ok, message) = DbBootstrap.resetLocalData(context)
                    busy = false
                    if (!ok) status = message
                }
            },
            onDismiss = { showResetConfirm = false }
        )
    }

    DbRecoveryContent(
        issue = issue,
        busy = busy,
        status = status,
        onPickBackupFile = {
            picker.launch(arrayOf("application/octet-stream", "application/json", "*/*"))
        },
        onRetry = {
            busy = true
            status = null
            scope.launch {
                val ok = DbBootstrap.retry(context)
                busy = false
                if (!ok) status = "仍然无法打开数据库"
            }
        },
        onResetRequest = { showResetConfirm = true },
        modifier = modifier
    )
}

@Composable
private fun DbRecoveryContent(
    issue: DbBootstrap.Issue,
    busy: Boolean,
    status: String?,
    onPickBackupFile: () -> Unit,
    onRetry: () -> Unit,
    onResetRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(24.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = issue.message,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "数据库密钥由系统 Keystore 保管，换机或恢复出厂后无法随应用数据一起迁移，" +
                        "因此这台设备解不开原有的记录数据库。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "要恢复历史记录，如果你还保留着旧手机：",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "① 在旧手机上打开 App →「我的」→「备份与恢复」→「备份密码」，" +
                                "设置一个你能记住的密码；\n" +
                                "② 在同一页面导出数据，得到 .nz 备份文件；\n" +
                                "③ 回到本机，用下面的「从备份文件恢复」选择该文件并输入同一个密码。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "旧手机已经不在、也没有备份文件时，旧数据无法再解密。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onPickBackupFile,
                enabled = !busy,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) { Text("从备份文件恢复") }

            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onRetry,
                enabled = !busy,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) { Text("重试") }

            Spacer(Modifier.height(10.dp))
            TextButton(
                onClick = onResetRequest,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("放弃旧数据并重新开始", color = MaterialTheme.colorScheme.error)
            }

            if (busy) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(10.dp))
                    Text("处理中…", style = MaterialTheme.typography.bodySmall)
                }
            }

            status?.let {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = "诊断信息\n${issue.detail}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DbRecoveryScreenPreview() {
    DbRecoveryContent(
        issue = DbBootstrap.Issue(
            message = "数据库密钥已丢失，历史数据无法解密",
            detail = "系统 Keystore 中没有本机主密钥，无法解开数据库密钥"
        ),
        busy = false,
        status = null,
        onPickBackupFile = {},
        onRetry = {},
        onResetRequest = {}
    )
}
