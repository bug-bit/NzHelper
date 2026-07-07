package me.neko.nzhelper.ui.component.tag

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.neko.nzhelper.core.model.TagDef
import me.neko.nzhelper.core.model.TagGroupDef
import me.neko.nzhelper.ui.theme.TagColors
import me.neko.nzhelper.ui.theme.TagIcons

/**
 * 分组多标签选择器 —— 标签系统的核心交互组件。
 *
 * 按分组渲染：每个分组一个标题（图标 + 名称 + 分组色条），其下是该分组的标签 FlowRow，
 * 支持跨分组多选。选中态使用标签自身颜色高亮。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagPicker(
    groups: List<Pair<TagGroupDef, List<TagDef>>>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    autoTagIds: Set<String> = emptySet()
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        groups.forEach { (group, tags) ->
            if (tags.isEmpty()) return@forEach
            GroupHeader(group)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tags.forEach { tag ->
                    val selected = tag.id in selectedIds
                    val isAuto = tag.id in autoTagIds
                    val groupColor = TagColors.contentColor(group.color)
                    FilterChip(
                        selected = selected,
                        onClick = { onToggle(tag.id) },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(tag.name)
                                if (isAuto && selected) {
                                    AutoBadge()
                                }
                            }
                        },
                        leadingIcon = {
                            if (selected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            } else {
                                Icon(
                                    TagIcons.iconFor(tag.icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = groupColor
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TagColors.containerColor(tag.color),
                            selectedLabelColor = TagColors.contentColor(tag.color),
                            selectedLeadingIconColor = TagColors.contentColor(tag.color)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoBadge() {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = "自动标签",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(9.dp)
            )
            Text(
                text = "自动",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun GroupHeader(group: TagGroupDef) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(TagColors.containerColor(group.color)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = TagIcons.iconFor(group.icon),
                contentDescription = null,
                tint = TagColors.contentColor(group.color),
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = group.name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(0.dp))
    }
}
