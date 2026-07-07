package me.neko.nzhelper.feature.history

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.neko.nzhelper.core.database.RecycleRepository
import me.neko.nzhelper.core.database.SessionRepository
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.core.model.SessionFormState
import me.neko.nzhelper.core.util.SessionSearch
import me.neko.nzhelper.feature.history.components.HistoryEmptyState
import me.neko.nzhelper.feature.history.components.HistoryQuickFilter
import me.neko.nzhelper.feature.history.components.HistorySearchBar
import me.neko.nzhelper.feature.history.components.HistorySearchEmptyState
import me.neko.nzhelper.feature.history.components.SessionDetailDialog
import me.neko.nzhelper.feature.history.components.SessionHistoryItem
import me.neko.nzhelper.ui.component.dialog.ConfirmDialog
import me.neko.nzhelper.ui.component.dialog.DetailsDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryScreen(isActive: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val sessions = remember { mutableStateListOf<Session>() }

    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(HistoryQuickFilter.ALL) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<Session?>(null) }

    var selectedSession by remember { mutableStateOf<Session?>(null) }
    var isViewingDetails by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    var editFormState by remember { mutableStateOf(SessionFormState()) }

    LaunchedEffect(isActive) {
        if (isActive) {
            val loaded = SessionRepository.loadSessions(context)
                .sortedByDescending { it.timestamp }
            sessions.clear()
            sessions.addAll(loaded)
        }
    }

    val filteredSessions by remember(sessions, searchQuery, activeFilter) {
        derivedStateOf {
            val byText = SessionSearch.filter(context, sessions, searchQuery)
            when (activeFilter) {
                HistoryQuickFilter.ALL -> byText
                HistoryQuickFilter.CLIMAX -> byText.filter { it.climax }
                HistoryQuickFilter.NO_CLIMAX -> byText.filter { !it.climax }
            }
        }
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("历史记录") },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            if (sessions.isEmpty()) {
                HistoryEmptyState()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        HistorySearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            activeFilter = activeFilter,
                            onFilterChange = { activeFilter = it },
                            resultCount = filteredSessions.size,
                            totalCount = sessions.size
                        )
                    }

                    if (filteredSessions.isEmpty()) {
                        item {
                            HistorySearchEmptyState(
                                query = searchQuery,
                                onClearSearch = {
                                    searchQuery = ""
                                    activeFilter = HistoryQuickFilter.ALL
                                }
                            )
                        }
                    } else {
                        items(filteredSessions) { session ->
                            SessionHistoryItem(
                                session = session,
                                onClick = {
                                    selectedSession = session
                                    isViewingDetails = true
                                },
                                onEdit = {
                                    selectedSession = session
                                    isEditing = true
                                    editFormState = SessionFormState(
                                        remark = session.remark,
                                        categoryId = session.categoryId,
                                        tagIds = session.tagIds.toSet(),
                                        climax = session.climax,
                                        rating = session.rating
                                    )
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

    if (isViewingDetails && selectedSession != null) {
        SessionDetailDialog(
            session = selectedSession!!,
            onDismiss = { isViewingDetails = false; selectedSession = null },
            onEditClick = {
                isViewingDetails = false
                isEditing = true
                editFormState = SessionFormState(
                    remark = selectedSession!!.remark,
                    categoryId = selectedSession!!.categoryId,
                    tagIds = selectedSession!!.tagIds.toSet(),
                    climax = selectedSession!!.climax,
                    rating = selectedSession!!.rating
                )
            }
        )
    }

    DetailsDialog(
        show = isEditing,
        formState = editFormState,
        onFormStateChange = { editFormState = it },
        onConfirm = {
            val original = selectedSession ?: return@DetailsDialog
            val index = sessions.indexOf(original)
            if (index != -1) {
                val updated = original.copy(
                    remark = editFormState.remark,
                    climax = editFormState.climax,
                    rating = editFormState.rating,
                    categoryId = editFormState.categoryId.ifBlank {
                        TagSettings.defaultCategory(
                            context
                        ).id
                    },
                    tagIds = editFormState.tagIds.toList()
                )
                sessions[index] = updated
                scope.launch { SessionRepository.saveSessions(context, sessions) }
            }
            isEditing = false; selectedSession = null
        },
        onDismiss = { isEditing = false; selectedSession = null }
    )

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

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    HistoryScreen()
}
