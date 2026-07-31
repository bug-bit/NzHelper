package me.neko.nzhelper.feature.ai.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
fun ModelPickerDialog(
    provider: AiProvider,
    onDismiss: () -> Unit,
    onConfirmed: (model: String, cachedModels: List<String>, manualModels: List<String>) -> Unit,
    onChanged: ((List<String>, List<String>) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    var models by remember { mutableStateOf(provider.cachedModels) }
    var manualModels by remember { mutableStateOf(provider.manualModels.toSet()) }
    var loading by remember { mutableStateOf(false) }
    var selectedModel by remember {
        val defaults = setOf("gpt-4o-mini", "claude-3-5-haiku-latest")
        mutableStateOf(if (provider.model in defaults) "" else provider.model)
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testOk by remember { mutableStateOf(false) }

    fun doTest(modelName: String) {
        testing = true
        loading = true
        testResult = null
        testOk = false
        scope.launch {
            val result = AiAnalyzer.fetchModels(
                provider.baseUrl, provider.apiKey, provider.mode,
                fallbackModel = modelName
            )
            testing = false
            loading = false
            result.fold(
                onSuccess = {
                    if (it.isNotEmpty()) models = (models + it).distinct()
                    testOk = true
                    testResult = if (it.isNotEmpty()) "${it.size} 个模型可用" else "连接成功"
                    if (selectedModel.isBlank()) selectedModel = it.firstOrNull() ?: selectedModel
                },
                onFailure = { e -> testResult = e.message ?: "未知错误" }
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择模型") },
        text = {
            Column {
                if (selectedModel.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "当前：",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            selectedModel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        doTest(selectedModel.ifBlank { provider.model }.ifBlank { "test" })
                    },
                    enabled = !testing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (testing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("测试中...")
                    } else {
                        Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("获取模型列表并测试连接")
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

                if (loading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("获取模型列表...", style = MaterialTheme.typography.bodyMedium)
                    }
                } else if (models.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.height(250.dp)) {
                        items(models) { m ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (m in manualModels)
                                            Modifier.background(
                                                MaterialTheme.colorScheme.secondaryContainer.copy(
                                                    alpha = 0.25f
                                                ),
                                                shape = MaterialTheme.shapes.small
                                            )
                                        else Modifier
                                    )
                                    .clickable { selectedModel = m }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedModel == m,
                                    onClick = { selectedModel = m },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    m,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (m in manualModels) {
                                    Text(
                                        "手动",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.secondaryContainer,
                                                shape = MaterialTheme.shapes.extraSmall
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    IconButton(
                                        onClick = {
                                            val newModels = models - m
                                            val newManual = manualModels - m
                                            models = newModels
                                            manualModels = newManual
                                            if (selectedModel == m) selectedModel = ""
                                            onChanged?.invoke(newModels, newManual.toList())
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Close, "删除",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.5f
                                            ),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "暂无模型，请手动添加或点击测试获取",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Outlined.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("添加模型")
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(
                    onClick = {
                        onConfirmed(
                            selectedModel.ifBlank { provider.model },
                            models,
                            manualModels.toList()
                        )
                    },
                    enabled = selectedModel.isNotBlank()
                ) { Text("确定") }
            }
        }
    )

    if (showAddDialog) {
        AddModelDialog(
            provider = provider,
            existingModels = models,
            onDismiss = { showAddDialog = false },
            onAdded = { name ->
                val newModels = models + name
                val newManual = manualModels + name
                models = newModels
                manualModels = newManual
                selectedModel = name
                onChanged?.invoke(newModels, newManual.toList())
                showAddDialog = false
            }
        )
    }
}
