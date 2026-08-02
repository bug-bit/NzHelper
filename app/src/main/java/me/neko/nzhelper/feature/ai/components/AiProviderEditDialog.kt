package me.neko.nzhelper.feature.ai.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.ai.AiProvider
import me.neko.nzhelper.core.ai.AiSettings
import me.neko.nzhelper.core.ai.PresetProvider

@Composable
fun AiProviderEditDialog(
    provider: AiProvider,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isNew = provider.id.isEmpty()

    val presetKeys = PresetProvider.ALL.map { it.key }
    val initialPresetIdx = if (isNew) 0
    else presetKeys.indexOf(provider.modeKey).coerceAtLeast(0)
    var selectedPresetIdx by remember { mutableIntStateOf(initialPresetIdx) }

    val currentPreset = PresetProvider.ALL[selectedPresetIdx]
    var name by remember { mutableStateOf(if (isNew) currentPreset.label else provider.name) }
    var baseUrl by remember { mutableStateOf(if (isNew) currentPreset.baseUrl else provider.baseUrl) }
    var apiKey by remember { mutableStateOf(provider.apiKey) }
    var keyVisible by remember { mutableStateOf(false) }

    val saveEnabled = baseUrl.isNotBlank() && apiKey.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "添加供应商" else "编辑供应商") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    PresetProvider.ALL.forEachIndexed { idx, preset ->
                        SegmentedButton(
                            selected = selectedPresetIdx == idx,
                            onClick = { selectedPresetIdx = idx },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = idx,
                                count = PresetProvider.ALL.size
                            )
                        ) { Text(preset.label) }
                    }
                }
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    placeholder = { Text("如 OpenAI、DeepSeek、通义千问") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text("sk-...") },
                    singleLine = true,
                    visualTransformation = if (keyVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { keyVisible = !keyVisible }) {
                            Icon(
                                imageVector = if (keyVisible) Icons.Outlined.VisibilityOff
                                else Icons.Outlined.Visibility,
                                contentDescription = if (keyVisible) "隐藏" else "显示"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val p = AiProvider(
                        id = provider.id.ifBlank { java.util.UUID.randomUUID().toString().take(8) },
                        name = name.ifBlank { currentPreset.label },
                        baseUrl = baseUrl,
                        apiKey = apiKey,
                        modeKey = currentPreset.mode.key,
                        model = provider.model,
                        isActive = provider.isActive,
                        cachedModels = provider.cachedModels,
                        manualModels = provider.manualModels
                    )
                    scope.launch {
                        AiSettings.saveProvider(context, p)
                        onSaved()
                    }
                },
                enabled = saveEnabled
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
