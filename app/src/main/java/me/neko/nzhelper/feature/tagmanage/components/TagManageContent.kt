package me.neko.nzhelper.feature.tagmanage.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.model.CategoryDef
import me.neko.nzhelper.core.model.SessionMode
import me.neko.nzhelper.core.model.TagDef
import me.neko.nzhelper.core.model.TagGroupDef
import me.neko.nzhelper.ui.component.AppSearchField
import me.neko.nzhelper.ui.component.ReorderableColumn
import me.neko.nzhelper.ui.component.setting.SettingsItem
import me.neko.nzhelper.ui.component.setting.SettingsItemHost
import me.neko.nzhelper.ui.theme.TagColors
import me.neko.nzhelper.ui.theme.TagIcons

private val RowGap = 8.dp

@Composable
fun TaxonomyBadge(
    color: String,
    icon: String,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    iconSize: Dp = 16.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(TagColors.containerColor(color)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = TagIcons.iconFor(icon),
            contentDescription = null,
            tint = TagColors.contentColor(color),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun TaxonomyItemRow(
    name: String,
    color: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    dragHandle: Modifier? = null
) {
    val trailing: (@Composable () -> Unit)? = if (dragHandle != null) {
        { DragHandleBox(dragHandle) }
    } else {
        null
    }

    SettingsItem(
        icon = TagIcons.iconFor(icon),
        title = name,
        subtitle = subtitle,
        onClick = onClick,
        modifier = modifier,
        iconTint = TagColors.contentColor(color),
        iconSize = 16.dp,
        leadingModifier = Modifier
            .clip(CircleShape)
            .background(TagColors.containerColor(color)),
        trailingContent = trailing
    )
}

@Composable
fun TaxonomyStaticRow(
    name: String,
    color: String,
    icon: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    SettingsItemHost(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TaxonomyBadge(color = color, icon = icon)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DragHandleBox(@SuppressLint("ModifierParameter") handle: Modifier) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .then(handle),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.DragHandle,
            contentDescription = "拖动排序",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
fun TaxonomyEmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    hint: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceBright),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun HintCard(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModeFilterRow(
    selected: SessionMode?,
    onSelect: (SessionMode?) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("全部模式") }
        )
        SessionMode.entries.forEach { mode ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                label = { Text(mode.label) }
            )
        }
    }
}

@Composable
fun GroupFilterRow(
    groups: List<TagGroupDef>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        items(groups, key = { it.id }) { group ->
            val selected = group.id == selectedId
            FilterChip(
                selected = selected,
                onClick = { onSelect(group.id) },
                label = {
                    Text(
                        text = group.name,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                TagColors.colorFor(group.color).copy(
                                    alpha = if (selected) 1f else 0.35f
                                )
                            )
                    )
                }
            )
        }
    }
}

@Composable
fun CategoryTabContent(
    categories: List<CategoryDef>,
    defaultModeLabelOf: (CategoryDef) -> String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(RowGap)
    ) {
        HintCard("分类用于记录的归类与统计，为应用内置内容，不支持增删改。")
        categories.forEach { category ->
            TaxonomyStaticRow(
                name = category.name,
                color = category.color,
                icon = category.icon,
                subtitle = defaultModeLabelOf(category)
            )
        }
        if (categories.isEmpty()) {
            TaxonomyEmptyState(icon = Icons.Outlined.Category, title = "暂无分类")
        }
    }
}

@Composable
fun GroupTabContent(
    groups: List<TagGroupDef>,
    tags: List<TagDef>,
    modeFilter: SessionMode?,
    onModeFilterChange: (SessionMode?) -> Unit,
    onEdit: (TagGroupDef) -> Unit,
    onDelete: (TagGroupDef) -> Unit,
    onReorder: (List<TagGroupDef>) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleGroups = if (modeFilter == null) groups else groups.filter { it.appliesTo(modeFilter) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(RowGap)
    ) {
        ModeFilterRow(selected = modeFilter, onSelect = onModeFilterChange)

        when {
            groups.isEmpty() -> TaxonomyEmptyState(
                icon = Icons.Outlined.Workspaces,
                title = "还没有分组",
                hint = "点击右下角按钮新增第一个分组"
            )

            visibleGroups.isEmpty() -> TaxonomyEmptyState(
                icon = Icons.Outlined.Workspaces,
                title = "该模式下暂无分组"
            )

            else -> {
                SectionTitle("共 ${visibleGroups.size} 个分组")
                if (modeFilter != null) {
                    visibleGroups.forEach { group ->
                        TaxonomyItemRow(
                            name = group.name,
                            color = group.color,
                            icon = group.icon,
                            subtitle = "${tags.count { it.groupId == group.id }} 个标签 · ${group.modeBadge()}",
                            onClick = { onEdit(group) }
                        )
                    }
                } else {
                    ReorderableColumn(
                        items = visibleGroups,
                        keyOf = { it.id },
                        onReorder = onReorder,
                        onCommit = onCommit,
                        gap = RowGap
                    ) { group, dragHandle, _ ->
                        TaxonomyItemRow(
                            name = group.name,
                            color = group.color,
                            icon = group.icon,
                            subtitle = "${tags.count { it.groupId == group.id }} 个标签 · ${group.modeBadge()}",
                            onClick = { onEdit(group) },
                            dragHandle = dragHandle
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TagTabContent(
    groups: List<TagGroupDef>,
    tags: List<TagDef>,
    query: String,
    onQueryChange: (String) -> Unit,
    modeFilter: SessionMode?,
    onModeFilterChange: (SessionMode?) -> Unit,
    onEdit: (TagDef) -> Unit,
    onReorderTags: (groupId: String, reordered: List<TagDef>) -> Unit,
    onCommitTags: (groupId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredTags = if (modeFilter == null) tags else tags.filter { it.appliesTo(modeFilter) }
    val visibleGroups = groups.mapNotNull { group ->
        val groupTags = filteredTags.filter { it.groupId == group.id }
        if (groupTags.isEmpty()) null else group to groupTags
    }
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    val current = visibleGroups.firstOrNull { it.first.id == selectedGroupId }
        ?: visibleGroups.firstOrNull()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(RowGap)
    ) {
        AppSearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = "搜索标签名称"
        )

        ModeFilterRow(selected = modeFilter, onSelect = onModeFilterChange)

        if (query.isNotBlank()) {
            val matched = filteredTags.filter { it.name.contains(query, ignoreCase = true) }
            if (matched.isEmpty()) {
                TaxonomyEmptyState(icon = Icons.Outlined.SearchOff, title = "没有匹配「$query」的标签")
            } else {
                SectionTitle("找到 ${matched.size} 个标签")
                matched.forEach { tag ->
                    val group = groups.firstOrNull { it.id == tag.groupId }
                    TaxonomyItemRow(
                        name = tag.name,
                        color = tag.color,
                        icon = tag.icon,
                        subtitle = listOfNotNull(group?.name, tag.modeBadge()).joinToString(" · "),
                        onClick = { onEdit(tag) }
                    )
                }
            }
            return@Column
        }

        if (filteredTags.isEmpty()) {
            TaxonomyEmptyState(
                icon = Icons.Outlined.Sell,
                title = if (modeFilter == null) "还没有标签" else "该模式下暂无标签",
                hint = if (modeFilter == null) "点击右下角按钮新增第一个标签" else null
            )
            return@Column
        }

        if (visibleGroups.size > 1) {
            GroupFilterRow(
                groups = visibleGroups.map { it.first },
                selectedId = current?.first?.id.orEmpty(),
                onSelect = { selectedGroupId = it }
            )
        }

        val currentGroup = current ?: return@Column
        val (group, groupTags) = currentGroup
        SectionTitle("${group.name} · ${groupTags.size} 个标签")

        if (modeFilter != null) {
            groupTags.forEach { tag ->
                TaxonomyItemRow(
                    name = tag.name,
                    color = tag.color,
                    icon = tag.icon,
                    subtitle = tag.modeBadge(),
                    onClick = { onEdit(tag) }
                )
            }
        } else {
            ReorderableColumn(
                items = groupTags,
                keyOf = { it.id },
                onReorder = { reordered -> onReorderTags(group.id, reordered) },
                onCommit = { onCommitTags(group.id) },
                gap = RowGap
            ) { tag, dragHandle, _ ->
                TaxonomyItemRow(
                    name = tag.name,
                    color = tag.color,
                    icon = tag.icon,
                    subtitle = tag.modeBadge(),
                    onClick = { onEdit(tag) },
                    dragHandle = dragHandle
                )
            }
        }
    }
}

private fun TagGroupDef.modeBadge(): String =
    if (modeKeys.isEmpty()) "通用" else modeKeys.joinToString("·") { SessionMode.fromKey(it).label }

private fun TagDef.modeBadge(): String =
    if (modeKeys.isEmpty()) "通用" else modeKeys.joinToString("·") { SessionMode.fromKey(it).label }
