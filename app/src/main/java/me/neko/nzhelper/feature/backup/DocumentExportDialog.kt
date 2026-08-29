package me.neko.nzhelper.feature.backup

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.export.DocumentExporter
import me.neko.nzhelper.core.model.BackupModules

@Composable
fun DocumentExportDialog(
    sessionCount: Int,
    recycleCount: Int,
    taxonomyCount: Int,
    onConfirm: (DocumentExporter.Format, BackupModules) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var format by remember { mutableStateOf(DocumentExporter.Format.PDF) }
    var sessions by remember { mutableStateOf(true) }
    var recycleBin by remember { mutableStateOf(false) }
    var taxonomy by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        icon = {
            Icon(Icons.Outlined.Description, contentDescription = null)
        },
        title = { Text("导出文档") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FormatRow(
                    icon = Icons.Outlined.PictureAsPdf,
                    label = "PDF 文档",
                    desc = "排版固定，适合查看与打印",
                    selected = format == DocumentExporter.Format.PDF,
                    onClick = { format = DocumentExporter.Format.PDF }
                )
                FormatRow(
                    icon = Icons.Outlined.Description,
                    label = "Word 文档",
                    desc = "内容可继续编辑",
                    selected = format == DocumentExporter.Format.DOCX,
                    onClick = { format = DocumentExporter.Format.DOCX }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                ModuleRow(
                    icon = Icons.Outlined.FileOpen,
                    label = "记录",
                    count = sessionCount,
                    checked = sessions,
                    onCheckedChange = { sessions = it }
                )
                ModuleRow(
                    icon = Icons.Outlined.Recycling,
                    label = "回收站",
                    count = recycleCount,
                    checked = recycleBin,
                    onCheckedChange = { recycleBin = it }
                )
                ModuleRow(
                    icon = Icons.Outlined.Sell,
                    label = "标签体系",
                    count = taxonomyCount,
                    checked = taxonomy,
                    onCheckedChange = { taxonomy = it }
                )
                Text(
                    "文档为明文导出（不加密、不含 AI 配置），请妥善保管。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!sessions && !recycleBin && !taxonomy) {
                        Toast.makeText(context, "请至少选择一项", Toast.LENGTH_SHORT).show()
                    } else {
                        onConfirm(
                            format,
                            BackupModules(sessions, recycleBin, taxonomy, aiConfig = false)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) { Text("导出") }
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
private fun FormatRow(
    icon: ImageVector,
    label: String,
    desc: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ModuleRow(
    icon: ImageVector,
    label: String,
    count: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
