package me.neko.nzhelper.feature.history

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.neko.nzhelper.core.database.RecycleRepository
import me.neko.nzhelper.core.database.SessionRepository
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.core.util.SessionSearch
import me.neko.nzhelper.feature.history.components.HistoryEmptyState
import me.neko.nzhelper.feature.history.components.HistoryFilterSheet
import me.neko.nzhelper.feature.history.components.HistoryQuickFilter
import me.neko.nzhelper.feature.history.components.HistorySearchBar
import me.neko.nzhelper.feature.history.components.HistorySearchEmptyState
import me.neko.nzhelper.feature.history.components.matches
import me.neko.nzhelper.feature.history.components.SessionDetailDialog
import me.neko.nzhelper.feature.history.components.TimelineItem
import me.neko.nzhelper.feature.statistics.model.PeriodSelection
import me.neko.nzhelper.feature.statistics.model.PeriodType
import me.neko.nzhelper.feature.statistics.util.isWithinPeriod
import me.neko.nzhelper.feature.statistics.util.periodStart
import me.neko.nzhelper.navigation.floatingBarContentBottomInset
import me.neko.nzhelper.ui.component.dialog.ConfirmDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryScreen(
    isActive: Boolean = false,
    periodFilter: PeriodSelection? = null,
    onClearPeriodFilter: () -> Unit = {},
    onEditRecord: (Session) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val sessions = remember { mutableStateListOf<Session>() }
    var isLoading by remember { mutableStateOf(true) }

    var searchQuery by remember { mutableStateOf("") }
    var activeFilters by remember { mutableStateOf(setOf(HistoryQuickFilter.ALL)) }
    var showFilterSheet by remember { mutableStateOf(false) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<Session?>(null) }

    var selectedSession by remember { mutableStateOf<Session?>(null) }
    var isViewingDetails by remember { mutableStateOf(false) }

    LaunchedEffect(isActive) {
        if (isActive) {
            val loaded = SessionRepository.loadSessions(context)
                .sortedByDescending { it.timestamp }
            sessions.clear()
            sessions.addAll(loaded)
            isLoading = false
        }
    }

    LaunchedEffect(periodFilter) {
        if (periodFilter != null) {
            searchQuery = ""
            activeFilters = setOf(HistoryQuickFilter.ALL)
            listState.scrollToItem(0)
        }
    }

    val tagDefs = remember(sessions) {
        TagSettings.getTags(context).associateBy { it.id }
    }
    val categoryDefs = remember(sessions) {
        TagSettings.getCategories(context).associateBy { it.id }
    }

    val filteredSessions by remember(sessions, searchQuery, activeFilters, periodFilter) {
        derivedStateOf {
            val byPeriod = if (periodFilter == null) {
                sessions
            } else {
                sessions.filter {
                    isWithinPeriod(it.timestamp, periodFilter.anchor, periodFilter.type)
                }
            }
            val byText = SessionSearch.filter(tagDefs, categoryDefs, byPeriod, searchQuery)
            if (activeFilters.isEmpty() || HistoryQuickFilter.ALL in activeFilters) {
                byText
            } else {
                byText.filter { session ->
                    activeFilters.any { filter -> session.matches(filter) }
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("历史记录") },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            imageVector = Icons.Outlined.FilterList,
                            contentDescription = "筛选记录",
                            tint = if (HistoryQuickFilter.ALL !in activeFilters) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!isLoading && sessions.isNotEmpty()) {
                HistorySearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    activeFilters = activeFilters,
                    resultCount = filteredSessions.size,
                    totalCount = sessions.size,
                    periodLabel = periodFilter?.let(::periodFilterLabel),
                    onClearPeriod = onClearPeriodFilter,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 4.dp)
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (sessions.isEmpty()) {
                    HistoryEmptyState()
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 16.dp + floatingBarContentBottomInset()
                        ),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (filteredSessions.isEmpty()) {
                            item {
                                HistorySearchEmptyState(
                                    query = searchQuery,
                                    onClearSearch = {
                                        searchQuery = ""
                                        activeFilters = setOf(HistoryQuickFilter.ALL)
                                        onClearPeriodFilter()
                                    }
                                )
                            }
                        } else {
                            itemsIndexed(
                                filteredSessions,
                                key = { _, session -> session.timestamp.toString() }) { index, session ->
                                TimelineItem(
                                    session = session,
                                    tagDefs = tagDefs,
                                    isFirst = index == 0,
                                    isLast = index == filteredSessions.lastIndex,
                                    onClick = {
                                        selectedSession = session
                                        isViewingDetails = true
                                    },
                                    onDelete = {
                                        sessionToDelete = session
                                        showDeleteConfirmDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isViewingDetails && selectedSession != null) {
        SessionDetailDialog(
            session = selectedSession!!,
            onDismiss = { isViewingDetails = false; selectedSession = null },
            onEditClick = {
                val target = selectedSession
                isViewingDetails = false
                selectedSession = null
                if (target != null) onEditRecord(target)
            }
        )
    }

    if (showFilterSheet) {
        HistoryFilterSheet(
            activeFilters = activeFilters,
            onConfirm = { activeFilters = it },
            onDismiss = { showFilterSheet = false }
        )
    }

    if (showDeleteConfirmDialog && sessionToDelete != null) {
        ConfirmDialog(
            icon = Icons.Rounded.Delete,
            title = "移入回收站",
            message = "确定要将这条记录移入回收站吗？可从回收站恢复。",
            confirmText = "移入回收站",
            onConfirm = {
                val session = sessionToDelete!!
                sessions.remove(session)
                scope.launch {
                    RecycleRepository.moveSessionsToRecycleBin(context, listOf(session))
                    Toast.makeText(context, "已移入回收站", Toast.LENGTH_SHORT).show()
                }
                showDeleteConfirmDialog = false
                sessionToDelete = null
            },
            onDismiss = {
                showDeleteConfirmDialog = false
                sessionToDelete = null
            }
        )
    }
}

private fun periodFilterLabel(selection: PeriodSelection): String = when (selection.type) {
    PeriodType.WEEK -> {
        val start = periodStart(selection.anchor, selection.type).toLocalDate()
        "$start 至 ${start.plusDays(6)}"
    }
    PeriodType.MONTH -> "${selection.anchor.year}年${selection.anchor.monthValue}月"
    PeriodType.YEAR -> "${selection.anchor.year}年"
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    HistoryScreen()
}
