package me.neko.nzhelper.feature.tagmanage

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.SessionMode
import me.neko.nzhelper.core.model.TagDef
import me.neko.nzhelper.core.model.TagGroupDef
import me.neko.nzhelper.feature.tagmanage.components.CategoryTabContent
import me.neko.nzhelper.feature.tagmanage.components.GroupOption
import me.neko.nzhelper.feature.tagmanage.components.GroupTabContent
import me.neko.nzhelper.feature.tagmanage.components.TagTabContent
import me.neko.nzhelper.feature.tagmanage.components.TaxonomyEditorSheet
import me.neko.nzhelper.ui.component.SlidingTab
import me.neko.nzhelper.ui.component.SlidingTabRow
import me.neko.nzhelper.ui.component.dialog.ConfirmDialog
import me.neko.nzhelper.ui.component.dialog.TaxonomyUpdateDialog

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

    var editingGroup by remember { mutableStateOf<TagGroupDef?>(null) }
    var addingGroup by remember { mutableStateOf(false) }
    var pendingDeleteGroupId by remember { mutableStateOf<String?>(null) }

    var editingTag by remember { mutableStateOf<TagDef?>(null) }
    var addingTag by remember { mutableStateOf(false) }
    var pendingDeleteTagId by remember { mutableStateOf<String?>(null) }

    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showTaxonomyDialog by remember {
        mutableStateOf(TagSettings.shouldPromptTaxonomyUpdate(context))
    }

    var groupModeFilter by remember { mutableStateOf<SessionMode?>(null) }
    var tagModeFilter by remember { mutableStateOf<SessionMode?>(null) }
    var tagQuery by remember { mutableStateOf("") }

    fun refresh() {
        categories = TagSettings.getCategories(context)
        groups = TagSettings.getGroups(context)
        tags = TagSettings.getTags(context)
    }

    val categoryModeLabels = remember(categories) {
        SessionMode.entries
            .groupBy { TagSettings.defaultCategoryFor(context, it).id }
            .mapValues { (_, modes) -> modes.joinToString(" · ") { it.label } + " 默认" }
    }

    val groupOptions = remember(groups) {
        groups.map { GroupOption(id = it.id, name = it.name, color = it.color, icon = it.icon) }
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
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "更多操作")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("恢复默认标签") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Restore, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    showRestoreConfirm = true
                                }
                            )
                        }
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
            if (pagerState.currentPage != 0) {
                FloatingActionButton(
                    onClick = {
                        when (pagerState.currentPage) {
                            1 -> addingGroup = true
                            else -> addingTag = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "新增")
                }
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
            val tabs = listOf(
                SlidingTab("分类", Icons.Outlined.Category, categories.size.toString()),
                SlidingTab("分组", Icons.Outlined.Workspaces, groups.size.toString()),
                SlidingTab("标签", Icons.Outlined.Sell, tags.size.toString())
            )
            SlidingTabRow(
                tabs = tabs,
                selectedIndex = pagerState.currentPage,
                selectionFraction = {
                    pagerState.currentPage + pagerState.currentPageOffsetFraction
                },
                onSelect = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.padding(top = 4.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (page) {
                        0 -> CategoryTabContent(
                            categories = categories,
                            defaultModeLabelOf = { categoryModeLabels[it.id] }
                        )

                        1 -> GroupTabContent(
                            groups = groups,
                            tags = tags,
                            modeFilter = groupModeFilter,
                            onModeFilterChange = { groupModeFilter = it },
                            onEdit = { editingGroup = it },
                            onDelete = { pendingDeleteGroupId = it.id },
                            onReorder = { newGroups -> groups = newGroups },
                            onCommit = {
                                TagSettings.reorderGroups(context, groups.map { it.id })
                            }
                        )

                        else -> TagTabContent(
                            groups = groups,
                            tags = tags,
                            query = tagQuery,
                            onQueryChange = { tagQuery = it },
                            modeFilter = tagModeFilter,
                            onModeFilterChange = { tagModeFilter = it },
                            onEdit = { editingTag = it },
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

    if (addingGroup || editingGroup != null) {
        val target = editingGroup
        TaxonomyEditorSheet(
            title = if (target == null) "新增分组" else "编辑分组",
            initialName = target?.name.orEmpty(),
            initialColor = target?.color ?: "slate",
            initialIcon = target?.icon ?: "folder",
            initialModeKeys = target?.modeKeys.orEmpty(),
            onConfirm = { name, color, icon, _, modeKeys ->
                if (target == null) {
                    TagSettings.addGroup(context, name, icon, color, modeKeys.orEmpty())
                } else {
                    TagSettings.updateGroup(context, target.id, name, icon, color, modeKeys.orEmpty())
                }
                addingGroup = false
                editingGroup = null
                refresh()
            },
            onDelete = target?.let {
                {
                    editingGroup = null
                    pendingDeleteGroupId = it.id
                }
            },
            onDismiss = {
                addingGroup = false
                editingGroup = null
            }
        )
    }

    if (addingTag || editingTag != null) {
        val target = editingTag
        TaxonomyEditorSheet(
            title = if (target == null) "新增标签" else "编辑标签",
            initialName = target?.name.orEmpty(),
            initialColor = target?.color ?: "slate",
            initialIcon = target?.icon ?: "hash",
            groupOptions = groupOptions,
            initialGroupId = target?.groupId ?: groups.firstOrNull()?.id,
            initialModeKeys = target?.modeKeys.orEmpty(),
            existingNames = remember(tags, target) {
                tags.filter { it.id != target?.id }.map { it.name }.toSet()
            },
            onConfirm = { name, color, icon, groupId, modeKeys ->
                if (target == null) {
                    val created = TagSettings.addTag(
                        context, name, groupId.orEmpty(), icon, color, modeKeys.orEmpty()
                    )
                    if (created == null) {
                        Toast.makeText(context, "标签名「$name」已存在", Toast.LENGTH_SHORT).show()
                        return@TaxonomyEditorSheet
                    }
                } else {
                    TagSettings.updateTag(
                        context, target.id, name, icon, color, groupId.orEmpty(), modeKeys.orEmpty()
                    )
                }
                addingTag = false
                editingTag = null
                refresh()
            },
            onDelete = target?.let {
                {
                    editingTag = null
                    pendingDeleteTagId = it.id
                }
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

    val pendingTag = tags.firstOrNull { it.id == pendingDeleteTagId }
    if (pendingTag != null) {
        ConfirmDialog(
            title = "删除标签",
            message = "确定删除「${pendingTag.name}」？历史记录中已使用该标签的记录不受影响。",
            confirmText = "删除",
            onConfirm = {
                TagSettings.deleteTag(context, pendingTag.id)
                pendingDeleteTagId = null
                refresh()
            },
            onDismiss = { pendingDeleteTagId = null }
        )
    }

    if (showRestoreConfirm) {
        ConfirmDialog(
            title = "恢复默认标签",
            message = "将把分类、分组和标签全部重置为默认内容，" +
                    "你自定义的分类、分组、标签及对默认标签的修改都将被移除，" +
                    "历史记录数据不受影响。确定恢复？",
            confirmText = "恢复",
            onConfirm = {
                TagSettings.restoreDefaultTaxonomy(context)
                refresh()
                showRestoreConfirm = false
            },
            onDismiss = { showRestoreConfirm = false }
        )
    }

    if (showTaxonomyDialog) {
        TaxonomyUpdateDialog(
            onConfirm = {
                TagSettings.applyTaxonomyUpdate(context)
                refresh()
                showTaxonomyDialog = false
            },
            onLater = { showTaxonomyDialog = false }
        )
    }
}
