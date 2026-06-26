package me.neko.nzhelper.ui.screens.history

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import me.neko.nzhelper.data.Session
import me.neko.nzhelper.data.SessionFormState
import me.neko.nzhelper.data.SessionRepository
import me.neko.nzhelper.ui.dialog.DetailsDialog
import me.neko.nzhelper.ui.dialog.formatTime
import me.neko.nzhelper.ui.screens.setting.CategorySettings
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryScreen(isActive: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val sessions = remember { mutableStateListOf<Session>() }

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
                EmptyStateView()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
                        ) {
                            Column {
                                sessions.forEachIndexed { index, session ->
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
                                                location = session.location,
                                                watchedMovie = session.watchedMovie,
                                                climax = session.climax,
                                                rating = session.rating,
                                                mood = session.mood,
                                                props = session.props
                                            )
                                        },
                                        onDelete = {
                                            sessionToDelete = session
                                            showDeleteConfirmDialog = true
                                        }
                                    )
                                    if (index < sessions.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 72.dp),
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                alpha = 0.5f
                                            )
                                        )
                                    }
                                }
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
                isViewingDetails = false
                isEditing = true
                editFormState = SessionFormState(
                    remark = selectedSession!!.remark,
                    location = selectedSession!!.location,
                    watchedMovie = selectedSession!!.watchedMovie,
                    climax = selectedSession!!.climax,
                    rating = selectedSession!!.rating,
                    mood = selectedSession!!.mood,
                    props = selectedSession!!.props
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
                    location = editFormState.location,
                    watchedMovie = editFormState.watchedMovie,
                    climax = editFormState.climax,
                    rating = editFormState.rating,
                    mood = editFormState.mood,
                    props = editFormState.props
                )
                sessions[index] = updated
                scope.launch { SessionRepository.saveSessions(context, sessions) }
            }
            isEditing = false; selectedSession = null
        },
        onDismiss = { isEditing = false; selectedSession = null },
        locationList = CategorySettings.getLocations(context),
        propsList = CategorySettings.getProps(context),
        moodList = CategorySettings.getMoods(context)
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
                    SessionRepository.moveSessionsToRecycleBin(context, listOf(session))
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

// --- 辅助组件 ---

@Composable
private fun EmptyStateView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "(。・ω・。)",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "暂无历史记录",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SessionHistoryItem(
    session: Session,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧彩色图标容器
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                session.timestamp.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatTime(session.duration),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal
                )
                if (session.remark.isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "· ${session.remark}",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 右侧操作区
        Row(modifier = Modifier.padding(start = 8.dp)) {
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SessionDetailDialog(
    session: Session,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "记录详情",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column {
                        DetailRow(
                            "时间",
                            session.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            showDivider = true
                        )
                        DetailRow("时长", formatTime(session.duration), showDivider = true)
                        DetailRow("地点", session.location.ifEmpty { "未记录" }, showDivider = true)
                        DetailRow("备注", session.remark.ifEmpty { "无" }, showDivider = true)
                        DetailRow("道具", session.props, showDivider = true)
                        DetailRow("心情", session.mood, showDivider = true)
                        DetailRow("评分", "%.1f".format(session.rating), showDivider = true)
                        DetailRow(
                            "小电影",
                            if (session.watchedMovie) "是" else "否",
                            showDivider = true
                        )
                        DetailRow("高潮", if (session.climax) "是" else "否", showDivider = false)

                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("关闭") }

                    Button(
                        onClick = onEditClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("编辑")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, showDivider: Boolean) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun ConfirmDialog(
    icon: ImageVector,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        icon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text(confirmText) }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) { Text("取消") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    HistoryScreen()
}
