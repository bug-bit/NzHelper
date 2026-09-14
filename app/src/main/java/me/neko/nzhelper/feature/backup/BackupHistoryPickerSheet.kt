package me.neko.nzhelper.feature.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.database.BackupRepository
import me.neko.nzhelper.ui.component.setting.LocalSettingsItemCorners
import me.neko.nzhelper.ui.component.setting.SettingsConnectionRadius
import me.neko.nzhelper.ui.component.setting.SettingsCornerRadius
import me.neko.nzhelper.ui.component.setting.SettingsItem
import me.neko.nzhelper.ui.component.setting.SettingsItemCorners
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupHistoryPickerSheet(
    files: List<BackupRepository.WebDavBackupFile>,
    onConfirm: (BackupRepository.WebDavBackupFile) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(files.first()) }

    @Suppress("DEPRECATION")
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "选择要恢复的备份",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(files, key = { it.fileName }) { file ->
                    val isSelected = selected.fileName == file.fileName
                    val corners = SettingsItemCorners(
                        topRadius = if (file === files.first()) SettingsCornerRadius
                        else SettingsConnectionRadius,
                        bottomRadius = if (file === files.last()) SettingsCornerRadius
                        else SettingsConnectionRadius
                    )
                    CompositionLocalProvider(LocalSettingsItemCorners provides corners) {
                        SettingsItem(
                            icon = when {
                                file.legacy -> Icons.Outlined.Cloud
                                file.type == BackupRepository.BackupType.AUTO -> Icons.Outlined.Update
                                else -> Icons.Outlined.History
                            },
                            title = when {
                                file.legacy -> "旧版备份"
                                file.type == BackupRepository.BackupType.AUTO -> "自动备份"
                                else -> "手动备份"
                            },
                            subtitle = buildString {
                                append(formatBackupTime(file.timestamp))
                                if (file.size >= 0) append(" · ").append(formatFileSize(file.size))
                            },
                            selected = isSelected,
                            onClick = { selected = file },
                            trailingContent = {
                                RadioButton(selected = isSelected, onClick = null)
                            }
                        )
                    }
                    if (file !== files.last()) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("取消")
                }
                Button(
                    onClick = { onConfirm(selected) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("恢复")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun formatBackupTime(timestamp: Long): String {
    if (timestamp <= 0) return "时间未知"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}

private fun formatFileSize(size: Long): String {
    if (size < 1024) return "$size B"
    if (size < 1024 * 1024) return "%.1f KB".format(Locale.US, size / 1024.0)
    return "%.1f MB".format(Locale.US, size / 1024.0 / 1024.0)
}
