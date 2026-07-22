package me.neko.nzhelper.feature.tagmanage

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.CategoryDef
import me.neko.nzhelper.core.model.TagDef
import me.neko.nzhelper.core.model.TagGroupDef
import me.neko.nzhelper.feature.tagmanage.components.ColorPickerRow
import me.neko.nzhelper.feature.tagmanage.components.IconPickerRow
import me.neko.nzhelper.ui.component.dialog.ConfirmDialog
import me.neko.nzhelper.ui.component.tag.TagChip
import me.neko.nzhelper.ui.theme.TagColors
import me.neko.nzhelper.ui.theme.TagIcons
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TagManageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val pagerState = rememberPagerState(pageCount = { 3 })

    var categories by remember { mutableStateOf(TagSettings.getCategories(context)) }
    var groups by remember { mutableStateOf(TagSettings.getGroups(context)) }
    var tags by remember { mutableStateOf(TagSettings.getTags(context)) }

    var editingCategory by remember { mutableStateOf<CategoryDef?>(null) }
    var addingCategory by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<TagGroupDef?>(null) }
    var addingGroup by remember { mutableStateOf(false) }
    var pendingDeleteGroupId by remember { mutableStateOf<String?>(null) }
    var editingTag by remember { mutableStateOf<TagDef?>(null) }
    var addingTag by remember { mutableStateOf(false) }

    fun refresh() {
        categories = TagSettings.getCategories(context)
        groups = TagSettings.getGroups(context)
        tags = TagSettings.getTags(context)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("标签管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (pagerState.currentPage) {
                        0 -> addingCategory = true
                        1 -> addingGroup = true
                        2 -> addingTag = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Filled.Add, contentDescription = "新增")
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                listOf("分类", "分组", "标签").forEachIndexed { i, title ->
                    Tab(
                        selected = pagerState.currentPage == i,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(i)
                            }
                        },
                        text = { Text(title) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (page) {
                        0 -> CategoryTabContent(
                            categories = categories,
                            onEdit = { editingCategory = it },
                            onDelete = { cat ->
                                if (categories.size > 1) {
                                    TagSettings.deleteCategory(context, cat.id)
                                    refresh()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "至少保留一个分类",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )

                        1 -> GroupTabContent(
                            groups = groups,
                            tags = tags,
                            onEdit = { editingGroup = it },
                            onDelete = { pendingDeleteGroupId = it.id },
                            onReorder = { newGroups -> groups = newGroups },
                            onCommit = {
                                TagSettings.reorderGroups(context, groups.map { it.id })
                            }
                        )

                        2 -> TagTabContent(
                            groups = groups,
                            tags = tags,
                            onEdit = { editingTag = it },
                            onDelete = { tag ->
                                TagSettings.deleteTag(context, tag.id)
                                refresh()
                            },
                            onReorderTags = { groupId, reordered ->
                                tags = tags.filterNot { it.groupId == groupId } + reordered
                            },
                            onCommitTags = { groupId ->
                                TagSettings.reorderTags(
                                    context,
                                    tags.filter { it.groupId == groupId }.map { it.id }
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    if (addingCategory || editingCategory != null) {
        val target = editingCategory
        TaxonomyEditorDialog(
            title = if (target == null) "新增分类" else "编辑分类",
            initialName = target?.name ?: "",
            initialColor = target?.color ?: "rose",
            initialIcon = target?.icon ?: "tag",
            onConfirm = { name, color, icon, _ ->
                if (target == null) {
                    TagSettings.addCategory(context, name, icon, color)
                } else {
                    TagSettings.updateCategory(context, target.id, name, icon, color)
                }
                addingCategory = false
                editingCategory = null
                refresh()
            },
            onDismiss = {
                addingCategory = false
                editingCategory = null
            }
        )
    }

    if (addingGroup || editingGroup != null) {
        val target = editingGroup
        TaxonomyEditorDialog(
            title = if (target == null) "新增分组" else "编辑分组",
            initialName = target?.name ?: "",
            initialColor = target?.color ?: "slate",
            initialIcon = target?.icon ?: "folder",
            onConfirm = { name, color, icon, _ ->
                if (target == null) {
                    TagSettings.addGroup(context, name, icon, color)
                } else {
                    TagSettings.updateGroup(context, target.id, name, icon, color)
                }
                addingGroup = false
                editingGroup = null
                refresh()
            },
            onDismiss = {
                addingGroup = false
                editingGroup = null
            }
        )
    }

    if (addingTag || editingTag != null) {
        val target = editingTag
        TagEditorDialog(
            title = if (target == null) "新增标签" else "编辑标签",
            initialName = target?.name ?: "",
            initialColor = target?.color ?: "slate",
            initialIcon = target?.icon ?: "hash",
            initialGroupId = target?.groupId ?: groups.firstOrNull()?.id ?: "",
            groups = groups,
            existingNames = tags.filter { it.id != target?.id }.map { it.name }.toSet(),
            onConfirm = { name, color, icon, groupId ->
                if (target == null) {
                    val created = TagSettings.addTag(context, name, groupId, icon, color)
                    if (created == null) {
                        Toast.makeText(
                            context,
                            "标签名「$name」已存在",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@TagEditorDialog
                    }
                } else {
                    TagSettings.updateTag(context, target.id, name, icon, color, groupId)
                }
                addingTag = false
                editingTag = null
                refresh()
            },
            onDismiss = {
                addingTag = false
                editingTag = null
            }
        )
    }

    val pendingGroup = groups.firstOrNull { it.id == pendingDeleteGroupId }
    if (pendingGroup != null) {
        val tagCount = tags.count { it.groupId == pendingGroup.id }
        ConfirmDialog(
            title = "删除分组",
            message = "确定删除「${pendingGroup.name}」？将同时删除该组下所有 $tagCount 个标签。",
            confirmText = "删除",
            onConfirm = {
                TagSettings.deleteGroup(context, pendingGroup.id)
                pendingDeleteGroupId = null
                refresh()
            },
            onDismiss = { pendingDeleteGroupId = null }
        )
    }
}

@Composable
private fun CategoryTabContent(
    categories: List<CategoryDef>,
    onEdit: (CategoryDef) -> Unit,
    onDelete: (CategoryDef) -> Unit
) {
    TabHeader(title = "分类", count = categories.size)
    if (categories.isEmpty()) {
        EmptyState(icon = Icons.Outlined.Sell, text = "暂无分类，点击右下角新增")
        return
    }
    categories.forEach { cat ->
        TaxonomyRow(
            name = cat.name,
            color = cat.color,
            icon = cat.icon,
            canDelete = categories.size > 1,
            onEdit = { onEdit(cat) },
            onDelete = { onDelete(cat) }
        )
    }
}

@Composable
private fun GroupTabContent(
    groups: List<TagGroupDef>,
    tags: List<TagDef>,
    onEdit: (TagGroupDef) -> Unit,
    onDelete: (TagGroupDef) -> Unit,
    onReorder: (List<TagGroupDef>) -> Unit,
    onCommit: () -> Unit
) {
    TabHeader(title = "分组", count = groups.size)
    if (groups.isEmpty()) {
        EmptyState(icon = Icons.Outlined.Sell, text = "暂无分组，点击右下角新增")
        return
    }
    ReorderableColumn(
        items = groups,
        keyOf = { it.id },
        onReorder = onReorder,
        onCommit = onCommit
    ) { item, dragHandle, _ ->
        TaxonomyRow(
            name = item.name,
            color = item.color,
            icon = item.icon,
            trailingText = "${tags.count { it.groupId == item.id }} 个标签",
            dragHandle = dragHandle,
            onEdit = { onEdit(item) },
            onDelete = { onDelete(item) }
        )
    }
}

@Composable
private fun TagTabContent(
    groups: List<TagGroupDef>,
    tags: List<TagDef>,
    onEdit: (TagDef) -> Unit,
    onDelete: (TagDef) -> Unit,
    onReorderTags: (groupId: String, reordered: List<TagDef>) -> Unit,
    onCommitTags: (groupId: String) -> Unit
) {
    TabHeader(title = "标签", count = tags.size)

    var query by remember { mutableStateOf("") }

    val visibleGroups = remember(groups, tags) {
        groups.mapNotNull { g ->
            val groupTags = tags.filter { it.groupId == g.id }
            if (groupTags.isEmpty()) null else g to groupTags
        }
    }
    var selectedGroupId by remember { mutableStateOf(visibleGroups.firstOrNull()?.first?.id) }

    LaunchedEffect(visibleGroups, selectedGroupId) {
        if (selectedGroupId == null || visibleGroups.none { it.first.id == selectedGroupId }) {
            selectedGroupId = visibleGroups.firstOrNull()?.first?.id
        }
    }
    val current =
        visibleGroups.firstOrNull { it.first.id == selectedGroupId } ?: visibleGroups.firstOrNull()

    SearchField(query = query, onQueryChange = { query = it })

    if (tags.isEmpty()) {
        EmptyState(icon = Icons.Outlined.Sell, text = "暂无标签，点击右下角新增")
        return
    }

    if (query.isNotBlank()) {
        val filtered = tags.filter { it.name.contains(query, ignoreCase = true) }
        if (filtered.isEmpty()) {
            EmptyState(icon = Icons.Outlined.SearchOff, text = "未找到匹配的标签")
        } else {
            filtered.forEach { tag ->
                val group = groups.firstOrNull { it.id == tag.groupId }
                TaxonomyRow(
                    name = tag.name,
                    color = tag.color,
                    icon = tag.icon,
                    trailingText = group?.name,
                    onEdit = { onEdit(tag) },
                    onDelete = { onDelete(tag) }
                )
            }
        }
    } else {
        if (visibleGroups.size > 1) {
            GroupSelectorBar(
                groups = visibleGroups.map { it.first },
                selectedId = current?.first?.id,
                onSelect = { selectedGroupId = it }
            )
        }

        if (current != null) {
            val (g, groupTags) = current
            ReorderableColumn(
                items = groupTags,
                keyOf = { it.id },
                onReorder = { reordered -> onReorderTags(g.id, reordered) },
                onCommit = { onCommitTags(g.id) }
            ) { item, dragHandle, _ ->
                TaxonomyRow(
                    name = item.name,
                    color = item.color,
                    icon = item.icon,
                    dragHandle = dragHandle,
                    onEdit = { onEdit(item) },
                    onDelete = { onDelete(item) }
                )
            }
        }
    }
}

/**
 * 顶部分组选择器：单选切换当前展示的分组。
 *
 * 与下方标签行视觉区分：使用描边样式（[FilterChip] + [BorderStroke]）+ 圆形分组色徽标
 * 作为前导图标，选中时填充分组色。标签则是实心浅底行，二者一眼可辨。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupSelectorBar(
    groups: List<TagGroupDef>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        groups.forEach { group ->
            val selected = group.id == selectedId
            val groupColor = TagColors.colorFor(group.color)
            FilterChip(
                selected = selected,
                onClick = { onSelect(group.id) },
                label = {
                    Text(
                        group.name,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (selected) Color.Transparent else groupColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            TagIcons.iconFor(group.icon),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = groupColor
                        )
                    }
                },
                shape = CircleShape,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selected) groupColor else groupColor.copy(alpha = 0.4f)
                ),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconColor = groupColor,
                    selectedContainerColor = groupColor.copy(alpha = 0.14f),
                    selectedLabelColor = groupColor,
                    selectedLeadingIconColor = groupColor
                )
            )
        }
    }
}

@Composable
private fun TabHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun EmptyState(icon: ImageVector, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("搜索标签") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "清除",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large
    )
}

@Composable
private fun TaxonomyRow(
    name: String,
    color: String,
    icon: String,
    trailingText: String? = null,
    canDelete: Boolean = true,
    @SuppressLint("ModifierParameter") dragHandle: Modifier? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceBright)
            .clickable { onEdit() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(TagColors.containerColor(color)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = TagIcons.iconFor(icon),
                contentDescription = null,
                tint = TagColors.contentColor(color),
                modifier = Modifier.size(15.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onDelete,
                enabled = canDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "删除",
                    tint = if (canDelete) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
            if (dragHandle != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .then(dragHandle),
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
        }
    }
}

@Composable
private fun TaxonomyEditorDialog(
    title: String,
    initialName: String,
    initialColor: String,
    initialIcon: String,
    onConfirm: (name: String, color: String, icon: String, groupId: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var color by remember { mutableStateOf(initialColor) }
    var icon by remember { mutableStateOf(initialIcon) }
    var nameError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
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
                    onValueChange = { name = it; nameError = false },
                    label = { Text("名称") },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("名称不能为空") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("颜色", style = MaterialTheme.typography.labelLarge)
                ColorPickerRow(selected = color, onSelect = { color = it })

                Text("图标", style = MaterialTheme.typography.labelLarge)
                IconPickerRow(selected = icon, onSelect = { icon = it }, accentColor = color)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        val trimmed = name.trim()
                        if (trimmed.isEmpty()) {
                            nameError = true
                        } else {
                            onConfirm(trimmed, color, icon, null)
                        }
                    }) { Text("保存") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagEditorDialog(
    title: String,
    initialName: String,
    initialColor: String,
    initialIcon: String,
    initialGroupId: String,
    groups: List<TagGroupDef>,
    existingNames: Set<String>,
    onConfirm: (name: String, color: String, icon: String, groupId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var color by remember { mutableStateOf(initialColor) }
    var icon by remember { mutableStateOf(initialIcon) }
    var groupId by remember { mutableStateOf(initialGroupId) }
    var nameError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
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
                    onValueChange = { name = it; nameError = null },
                    label = { Text("名称") },
                    isError = nameError != null,
                    supportingText = nameError?.let { err ->
                        { Text(err) }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("所属分组", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groups.forEach { g ->
                        FilterChip(
                            selected = g.id == groupId,
                            onClick = {
                                groupId = g.id
                                nameError = null
                            },
                            label = { Text(g.name) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(TagColors.containerColor(g.color)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = TagIcons.iconFor(g.icon),
                                        contentDescription = null,
                                        tint = TagColors.contentColor(g.color),
                                        modifier = Modifier.size(9.dp)
                                    )
                                }
                            }
                        )
                    }
                }

                Text("颜色", style = MaterialTheme.typography.labelLarge)
                ColorPickerRow(selected = color, onSelect = { color = it })

                Text("图标", style = MaterialTheme.typography.labelLarge)
                IconPickerRow(selected = icon, onSelect = { icon = it }, accentColor = color)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        val trimmed = name.trim()
                        when {
                            trimmed.isEmpty() -> nameError = "名称不能为空"
                            trimmed in existingNames -> nameError = "标签名「$trimmed」已存在"
                            groupId.isBlank() -> nameError = "请选择分组"
                            else -> onConfirm(trimmed, color, icon, groupId)
                        }
                    }) { Text("保存") }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T> ReorderableColumn(
    items: List<T>,
    keyOf: (T) -> String,
    onReorder: (List<T>) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
    gap: Dp = 10.dp,
    itemContent: @Composable (item: T, dragHandle: Modifier, isDragging: Boolean) -> Unit
) {
    val gapPx = with(LocalDensity.current) { gap.toPx() }
    val latestItems = rememberUpdatedState(items)
    val latestOnReorder = rememberUpdatedState(onReorder)
    val latestOnCommit = rememberUpdatedState(onCommit)
    val itemHeights = remember { hashMapOf<String, Int>() }
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val hapticFeedback = LocalHapticFeedback.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        items.forEach { item ->
            val itemKey = keyOf(item)
            val isDragging = draggingKey == itemKey
            val dragHandle = Modifier.pointerInput(itemKey) {
                detectDragGestures(
                    onDragStart = {
                        draggingKey = itemKey
                        dragOffset = 0f
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragEnd = {
                        latestOnCommit.value()
                        draggingKey = null
                        dragOffset = 0f
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragCancel = {
                        draggingKey = null
                        dragOffset = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount.y
                        val cur = latestItems.value
                        val curIndex = cur.indexOfFirst { keyOf(it) == itemKey }
                        if (curIndex < 0) return@detectDragGestures
                        val dH = (itemHeights[itemKey] ?: 0).toFloat()
                        if (dH <= 0f) return@detectDragGestures
                        var newIndex = curIndex
                        while (newIndex < cur.lastIndex) {
                            val nH = (itemHeights[keyOf(cur[newIndex + 1])] ?: 0).toFloat()
                            if (nH <= 0f) break
                            if (dragOffset > (dH + nH) / 2f + gapPx) {
                                newIndex++
                                dragOffset -= (nH + gapPx)
                            } else break
                        }
                        while (newIndex > 0) {
                            val pH = (itemHeights[keyOf(cur[newIndex - 1])] ?: 0).toFloat()
                            if (pH <= 0f) break
                            if (dragOffset < -((dH + pH) / 2f + gapPx)) {
                                newIndex--
                                dragOffset += (pH + gapPx)
                            } else break
                        }
                        if (newIndex != curIndex) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val mutable = cur.toMutableList()
                            val moved = mutable.removeAt(curIndex)
                            mutable.add(newIndex, moved)
                            latestOnReorder.value(mutable)
                        }
                    }
                )
            }
            key(itemKey) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { itemHeights[itemKey] = it.size.height }
                        .then(
                            if (isDragging) Modifier
                                .offset { IntOffset(0, dragOffset.roundToInt()) }
                                .zIndex(1f)
                                .shadow(12.dp, MaterialTheme.shapes.medium)
                                .graphicsLayer {
                                    scaleX = 1.03f
                                    scaleY = 1.03f
                                }
                            else Modifier
                        )
                ) {
                    itemContent(item, dragHandle, isDragging)
                }
            }
        }
    }
}