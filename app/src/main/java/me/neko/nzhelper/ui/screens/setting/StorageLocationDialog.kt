package me.neko.nzhelper.ui.screens.setting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun StorageLocationDialog(
    currentMode: String,
    currentPath: String,
    onConfirm: (mode: String, path: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(currentMode) }
    var externalPath by remember { mutableStateOf(currentPath) }
    var pathError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        icon = {
            Icon(
                imageVector = Icons.Outlined.FolderOpen,
                contentDescription = null
            )
        },
        title = { Text("数据存储位置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // 内部存储选项
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selectedMode == StorageSettings.MODE_INTERNAL)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else Color.Transparent
                        )
                        .clickable { selectedMode = StorageSettings.MODE_INTERNAL }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedMode == StorageSettings.MODE_INTERNAL,
                        onClick = { selectedMode = StorageSettings.MODE_INTERNAL }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            "应用内部存储",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "数据保存在应用私有目录，卸载后清除",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 外部存储选项
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selectedMode == StorageSettings.MODE_EXTERNAL)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else Color.Transparent
                        )
                        .clickable { selectedMode = StorageSettings.MODE_EXTERNAL }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedMode == StorageSettings.MODE_EXTERNAL,
                        onClick = { selectedMode = StorageSettings.MODE_EXTERNAL }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            "外部存储目录",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "数据保存在指定路径，卸载后不会丢失",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AnimatedVisibility(
                    visible = selectedMode == StorageSettings.MODE_EXTERNAL,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(start = 12.dp, top = 4.dp)) {
                        OutlinedTextField(
                            value = externalPath,
                            onValueChange = {
                                externalPath = it
                                pathError = false
                            },
                            label = { Text("存储路径") },
                            placeholder = { Text(StorageSettings.DEFAULT_EXTERNAL_PATH) },
                            isError = pathError,
                            supportingText = if (pathError) {
                                { Text("路径不能为空") }
                            } else {
                                { Text("文件将保存在该路径下的 nzHelper_data.json") }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "需要授予「所有文件访问」权限",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalPath = externalPath.ifBlank { StorageSettings.DEFAULT_EXTERNAL_PATH }
                    if (selectedMode == StorageSettings.MODE_EXTERNAL && finalPath.isBlank()) {
                        pathError = true
                    } else {
                        onConfirm(selectedMode, finalPath)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("取消")
            }
        }
    )
}