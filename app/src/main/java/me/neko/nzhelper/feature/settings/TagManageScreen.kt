package me.neko.nzhelper.feature.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.CategoryDef
import me.neko.nzhelper.core.model.TagDef
import me.neko.nzhelper.core.model.TagGroupDef
import me.neko.nzhelper.feature.settings.components.ColorPickerRow
import me.neko.nzhelper.feature.settings.components.IconPickerRow
import me.neko.nzhelper.ui.component.dialog.ConfirmDialog
import me.neko.nzhelper.ui.theme.TagColors
import me.neko.nzhelper.ui.theme.TagIcons

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

    // 编辑器状态
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
                selectedTabIndex = pagerState.currentPage
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
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (page) {
                        0 -> CategoryTabContent(
                            categories = categories,
                            onAdd = { addingCategory = true },
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
                            onAdd = { addingGroup = true },
                            onEdit = { editingGroup = it },
                            onDelete = { pendingDeleteGroupId = it.id }
                        )

                        2 -> TagTabContent(
                            groups = groups,
                            tags = tags,
                            onAdd = { addingTag = true },
                            onEdit = { editingTag = it },
                            onDelete = { tag ->
                                TagSettings.deleteTag(context, tag.id)
                                refresh()
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
    onAdd: () -> Unit,
    onEdit: (CategoryDef) -> Unit,
    onDelete: (CategoryDef) -> Unit
) {
    TabHeader(title = "分类", count = categories.size, addLabel = "新增分类", onAdd = onAdd)
    if (categories.isEmpty()) {
        EmptyHint("暂无分类，点击右上角新增")
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
    onAdd: () -> Unit,
    onEdit: (TagGroupDef) -> Unit,
    onDelete: (TagGroupDef) -> Unit
) {
    TabHeader(title = "分组", count = groups.size, addLabel = "新增分组", onAdd = onAdd)
    if (groups.isEmpty()) {
        EmptyHint("暂无分组，点击右上角新增")
    }
    groups.forEach { g ->
        TaxonomyRow(
            name = g.name,
            color = g.color,
            icon = g.icon,
            trailingText = "${tags.count { it.groupId == g.id }} 个标签",
            onEdit = { onEdit(g) },
            onDelete = { onDelete(g) }
        )
    }
}

@Composable
private fun TagTabContent(
    groups: List<TagGroupDef>,
    tags: List<TagDef>,
    onAdd: () -> Unit,
    onEdit: (TagDef) -> Unit,
    onDelete: (TagDef) -> Unit
) {
    TabHeader(title = "标签", count = tags.size, addLabel = "新增标签", onAdd = onAdd)
    if (tags.isEmpty()) {
        EmptyHint("暂无标签，点击右上角新增")
        return
    }
    groups.forEach { g ->
        val groupTags = tags.filter { it.groupId == g.id }
        if (groupTags.isEmpty()) return@forEach
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(TagColors.containerColor(g.color)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = TagIcons.iconFor(g.icon),
                    contentDescription = null,
                    tint = TagColors.contentColor(g.color),
                    modifier = Modifier.size(10.dp)
                )
            }
            Text(
                text = "${g.name}（${groupTags.size}）",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        groupTags.forEach { tag ->
            TaxonomyRow(
                name = tag.name,
                color = tag.color,
                icon = tag.icon,
                onEdit = { onEdit(tag) },
                onDelete = { onDelete(tag) }
            )
        }
    }
}

@Composable
private fun TabHeader(
    title: String,
    count: Int,
    addLabel: String,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$title（$count）",
            style = MaterialTheme.typography.titleMedium
        )
        TextButton(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(addLabel)
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
    )
}

@Composable
private fun TaxonomyRow(
    name: String,
    color: String,
    icon: String,
    trailingText: String? = null,
    canDelete: Boolean = true,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceBright)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(TagColors.containerColor(color)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = TagIcons.iconFor(icon),
                contentDescription = null,
                tint = TagColors.contentColor(color),
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = "编辑",
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(
            onClick = onDelete,
            enabled = canDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "删除",
                tint = if (canDelete) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp)
            )
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
                    Button(onClick = {
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
    var groupExpanded by remember { mutableStateOf(false) }

    val selectedGroup = groups.firstOrNull { it.id == groupId }

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
                Box {
                    OutlinedButton(
                        onClick = { groupExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        if (selectedGroup != null) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(TagColors.containerColor(selectedGroup.color)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = TagIcons.iconFor(selectedGroup.icon),
                                    contentDescription = null,
                                    tint = TagColors.contentColor(selectedGroup.color),
                                    modifier = Modifier.size(9.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = selectedGroup?.name ?: "选择分组",
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = groupExpanded,
                        onDismissRequest = { groupExpanded = false }
                    ) {
                        groups.forEach { g ->
                            DropdownMenuItem(
                                text = { Text(g.name) },
                                onClick = {
                                    groupId = g.id
                                    groupExpanded = false
                                    nameError = null
                                }
                            )
                        }
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
                    Button(onClick = {
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