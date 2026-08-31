package me.neko.nzhelper.ui.component.dialog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

/**
 * 标签体系更新确认弹窗：
 * 同意后强制重置为全新的默认分组与标签；暂不更新则下次进入标签管理页再问。
 */
@Composable
fun TaxonomyUpdateDialog(
    onConfirm: () -> Unit,
    onLater: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onLater,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Sell,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "全新标签系统",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = "分组已重新设计为：地点、时间、情绪、身体、行为、道具等，" +
                        "并为双人模式新增「伴侣」「体位」「射精方式」等分组。\n\n" +
                        "更新后将以全新默认分组与标签替换现有内容，" +
                        "您自定义的分组、标签及对默认标签的修改将被移除，历史记录数据不受影响。",
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Text("立即更新")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onLater,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Text("暂不更新")
            }
        }
    )
}
