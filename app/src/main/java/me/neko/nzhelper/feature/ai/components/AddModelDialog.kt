package me.neko.nzhelper.feature.ai.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.neko.nzhelper.core.ai.AiAnalyzer
import me.neko.nzhelper.core.ai.AiProvider

@Composable
fun AddModelDialog(
    provider: AiProvider,
    existingModels: List<String>,
    onDismiss: () -> Unit,
    onAdded: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var testing by remember { mutableStateOf(false) }
    var testOk by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }

    fun doTest() {
        val n = name.trim()
        if (n.isBlank()) return
        testing = true
        testOk = false
        testResult = null
        scope.launch {
            val result = AiAnalyzer.fetchModels(
                provider.baseUrl, provider.apiKey, provider.mode,
                fallbackModel = n
            )
            testing = false
            result.fold(
                onSuccess = {
                    testOk = true
                    testResult = "连接成功"
                },
                onFailure = { e ->
                    testResult = e.message ?: "未知错误"
                }
            )
        }
    }

    val canAdd = testOk && name.trim().isNotBlank() && name.trim() !in existingModels

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加模型") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        testOk = false
                        testResult = null
                    },
                    label = { Text("模型名称") },
                    placeholder = { Text("如 qwen-plus") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { doTest() },
                    enabled = !testing && name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (testing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("测试中...")
                    } else {
                        Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("测试连接")
                    }
                }
                if (testResult != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (testOk) {
                            Icon(
                                Icons.Outlined.CheckCircle, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            testResult!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (testOk) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdded(name.trim()) },
                enabled = canAdd
            ) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
