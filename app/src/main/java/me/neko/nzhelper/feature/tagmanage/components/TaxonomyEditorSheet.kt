package me.neko.nzhelper.feature.tagmanage.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.model.SessionMode
import me.neko.nzhelper.ui.component.tag.TagChip
import me.neko.nzhelper.ui.theme.TagColors
import me.neko.nzhelper.ui.theme.TagIcons

data class GroupOption(
    val id: String,
    val name: String,
    val color: String,
    val icon: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaxonomyEditorSheet(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        color: String,
        icon: String,
        groupId: String?,
        modeKeys: List<String>?
    ) -> Unit,
    initialName: String = "",
    initialColor: String = "slate",
    initialIcon: String = "hash",
    groupOptions: List<GroupOption>? = null,
    initialGroupId: String? = null,
    initialModeKeys: List<String>? = null,
    existingNames: Set<String> = emptySet(),
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var color by remember { mutableStateOf(initialColor) }
    var icon by remember { mutableStateOf(initialIcon) }
    var groupId by remember { mutableStateOf(initialGroupId ?: groupOptions?.firstOrNull()?.id) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var selectedModes by remember(initialModeKeys) {
        mutableStateOf(
            when {
                initialModeKeys.isNullOrEmpty() -> SessionMode.entries.toSet()
                else -> SessionMode.entries.filter { it.key in initialModeKeys }.toSet()
            }
        )
    }

    @Suppress("DEPRECATION")
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TagChip(
                        name = name.trim().ifBlank { "预览" },
                        color = color,
                        icon = icon
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorText = null
                    },
                    label = { Text("名称") },
                    isError = errorText != null,
                    supportingText = errorText?.let { err -> { Text(err) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (groupOptions != null) {
                    SectionLabel("所属分组")
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groupOptions.forEach { option ->
                            FilterChip(
                                selected = option.id == groupId,
                                onClick = {
                                    groupId = option.id
                                    errorText = null
                                },
                                label = { Text(option.name) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(TagColors.containerColor(option.color)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = TagIcons.iconFor(option.icon),
                                            contentDescription = null,
                                            tint = TagColors.contentColor(option.color),
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                SectionLabel("颜色")
                ColorPickerRow(selected = color, onSelect = { color = it })

                SectionLabel("图标")
                IconPickerRow(selected = icon, onSelect = { icon = it }, accentColor = color)

                if (initialModeKeys != null) {
                    SectionLabel("适用模式")
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SessionMode.entries.forEach { mode ->
                            val isSelected = mode in selectedModes
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedModes = when {
                                        isSelected && selectedModes.size > 1 -> selectedModes - mode
                                        isSelected -> selectedModes
                                        else -> selectedModes + mode
                                    }
                                },
                                label = { Text(mode.label) }
                            )
                        }
                    }
                    Text(
                        text = "全部勾选表示适用于所有模式",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                    onClick = {
                        val trimmed = name.trim()
                        when {
                            trimmed.isEmpty() -> errorText = "名称不能为空"
                            trimmed in existingNames -> errorText = "「$trimmed」已存在"
                            groupOptions != null && groupId.isNullOrBlank() -> errorText = "请选择分组"
                            else -> onConfirm(
                                trimmed,
                                color,
                                icon,
                                groupId,
                                if (initialModeKeys == null) {
                                    null
                                } else if (selectedModes.size == SessionMode.entries.size) {
                                    emptyList()
                                } else {
                                    selectedModes.map { it.key }
                                }
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("保存")
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
