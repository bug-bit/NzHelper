package me.neko.nzhelper.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.launch
import me.neko.nzhelper.data.Session
import me.neko.nzhelper.data.SessionRepository
import me.neko.nzhelper.ui.details.DetailsDialog
import java.io.OutputStreamWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@SuppressLint("DefaultLocale")
private fun formatTime(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return buildString {
        if (hours > 0) append(String.format("%02d:", hours))
        append(String.format("%02d:%02d", minutes, seconds))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("sessions_prefs", Context.MODE_PRIVATE) }
    val gson = remember { Gson() }
    val formatter = remember { DateTimeFormatter.ISO_LOCAL_DATE_TIME }
    val sessions = remember { mutableStateListOf<Session>() }

    val scope = rememberCoroutineScope()
    var editSession by remember { mutableStateOf<Session?>(null) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    var remarkInput by remember { mutableStateOf("") }
    var locationInput by remember { mutableStateOf("") }
    var watchedMovie by remember { mutableStateOf(false) }
    var climax by remember { mutableStateOf(false) }
    var rating by remember { mutableFloatStateOf(3f) }
    var mood by remember { mutableStateOf("平静") }
    var props by remember { mutableStateOf("手") }

    var showMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<Session?>(null) }
    var sessionToView by remember { mutableStateOf<Session?>(null) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    // 导出 Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { os ->
                OutputStreamWriter(os).use { writer ->
                    // 序列化
                    val outList = sessions.map { s ->
                        listOf(
                            s.timestamp.format(formatter),
                            s.duration,
                            s.remark,
                            s.location,
                            s.watchedMovie,
                            s.climax,
                            s.rating,
                            s.mood,
                            s.props
                        )
                    }
                    writer.write(gson.toJson(outList))
                }
            }
        }
    }

    // 导入 Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)
                ?.bufferedReader()
                ?.use { reader ->
                    val jsonStr = reader.readText()
                    val root = JsonParser.parseString(jsonStr).asJsonArray

                    sessions.clear()

                    for (elem in root) {
                        if (elem.isJsonArray) {
                            val arr = elem.asJsonArray
                            val timeStr = arr[0].asString
                            val dur = if (arr.size() >= 2) arr[1].asInt else 0
                            val rem =
                                if (arr.size() >= 3 && !arr[2].isJsonNull) arr[2].asString else ""
                            val loc =
                                if (arr.size() >= 4 && !arr[3].isJsonNull) arr[3].asString else ""
                            val watched = if (arr.size() >= 5) arr[4].asBoolean else false
                            val climaxed = if (arr.size() >= 6) arr[5].asBoolean else false
                            val rate = if (arr.size() >= 7 && !arr[6].isJsonNull) {
                                arr[6].asFloat.coerceIn(0f, 5f) // 确保在范围内
                            } else 0f
                            val md =
                                if (arr.size() >= 8 && !arr[7].isJsonNull) arr[7].asString else ""
                            val prop =
                                if (arr.size() >= 9 && !arr[8].isJsonNull) arr[8].asString else ""
                            sessions.add(
                                Session(
                                    timestamp = LocalDateTime.parse(timeStr, formatter),
                                    duration = dur,
                                    remark = rem,
                                    location = loc,
                                    watchedMovie = watched,
                                    climax = climaxed,
                                    rating = rate,
                                    mood = md,
                                    props = prop
                                )
                            )
                        }
                    }

                    prefs.edit {
                        putString("sessions", jsonStr)
                    }
                }
        }
    }

    // 读取历史（兼容旧版）
    LaunchedEffect(Unit) {
        val loaded = SessionRepository.loadSessions(context)
        sessions.clear()
        sessions.addAll(loaded)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("历史记录") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("导出数据") },
                            onClick = {
                                showMenu = false
                                exportLauncher.launch("NzHelper_export.json")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("导入数据") },
                            onClick = {
                                showMenu = false
                                importLauncher.launch(arrayOf("application/json"))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("清除全部记录") },
                            onClick = {
                                showMenu = false
                                showClearDialog = true
                            }
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        ) {
            if (sessions.isEmpty()) {
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
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "暂无历史记录哦！",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    )
                ) {
                    items(sessions) { session ->
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            border = CardDefaults.outlinedCardBorder(),
                            onClick = { sessionToView = session }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text(
                                            "时间: ${
                                                session.timestamp.format(
                                                    DateTimeFormatter.ofPattern(
                                                        "yyyy-MM-dd HH:mm:ss"
                                                    )
                                                )
                                            }"
                                        )
                                        Text("持续: ${formatTime(session.duration)}")
                                        if (session.remark.isNotEmpty()) {
                                            Text(
                                                "备注: ${session.remark}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                    IconButton(onClick = { sessionToDelete = session }) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除记录")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (sessionToDelete != null) {
                AlertDialog(
                    onDismissRequest = { sessionToDelete = null },
                    title = { Text("删除记录") },
                    text = { Text("确认删除此记录？") },
                    confirmButton = {
                        TextButton(onClick = {
                            sessions.remove(sessionToDelete)
                            prefs.edit {
                                putString(
                                    "sessions",
                                    gson.toJson(sessions.map {
                                        listOf(
                                            it.timestamp.format(formatter),
                                            it.duration,
                                            it.remark
                                        )
                                    })
                                )
                            }
                            sessionToDelete = null
                        }) { Text("确认") }
                    },
                    dismissButton = {
                        TextButton(onClick = { sessionToDelete = null }) { Text("取消") }
                    }
                )
            }

            if (showClearDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDialog = false },
                    title = { Text("清除全部记录") },
                    text = { Text("确认要清除所有历史记录吗？此操作不可撤销。") },
                    confirmButton = {
                        TextButton(onClick = {
                            sessions.clear()
                            prefs.edit { remove("sessions") }
                            showClearDialog = false
                        }) { Text("删除") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearDialog = false }) { Text("取消") }
                    }
                )
            }

            // 查看详情对话框
            sessionToView?.let { s ->
                AlertDialog(
                    onDismissRequest = { sessionToView = null },
                    title = { Text("会话详情") },
                    text = {
                        Column {
                            val pat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            Text("开始时间：${s.timestamp.format(pat)}")
                            Text("持续时长：${formatTime(s.duration)}")
                            Text("备注：${s.remark.ifEmpty { "无" }}")
                            Text("地点：${s.location.ifEmpty { "无" }}")
                            Text("是否观看小电影：${if (s.watchedMovie) "是" else "否"}")
                            Text("发射：${if (s.climax) "是" else "否"}")
                            Text("道具：${s.props.ifEmpty { "无" }}")
                            Text("评分：${"%.1f".format(s.rating)} / 5.0")
                            Text("心情：${s.mood.ifEmpty { "无" }}")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { sessionToView = null }) { Text("关闭") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            // 进入编辑状态
                            editSession = s
                            isEditing = true
                            remarkInput = s.remark
                            locationInput = s.location
                            watchedMovie = s.watchedMovie
                            climax = s.climax
                            rating = s.rating
                            mood = s.mood
                            props = s.props
                            showDetailsDialog = true
                            sessionToView = null
                        }) { Text("编辑") }
                    }
                )
            }
            // 编辑对话框复用 DetailsDialog
            DetailsDialog(
                show = showDetailsDialog,
                remark = remarkInput,
                onRemarkChange = { remarkInput = it },
                location = locationInput,
                onLocationChange = { locationInput = it },
                watchedMovie = watchedMovie,
                onWatchedMovieChange = { watchedMovie = it },
                climax = climax,
                onClimaxChange = { climax = it },
                props = props,
                onPropsChange = { props = it },
                rating = rating,
                onRatingChange = { rating = it },
                mood = mood,
                onMoodChange = { mood = it },
                onConfirm = {
                    if (isEditing && editSession != null) {
                        // 更新列表中对应项
                        val idx = sessions.indexOf(editSession!!)
                        if (idx >= 0) {
                            sessions[idx] = editSession!!.copy(
                                remark = remarkInput,
                                location = locationInput,
                                watchedMovie = watchedMovie,
                                climax = climax,
                                rating = rating,
                                mood = mood,
                                props = props
                            )
                        }
                    }
                    // 保存并重置
                    scope.launch {
                        SessionRepository.saveSessions(context, sessions)
                    }
                    remarkInput = ""
                    locationInput = ""
                    watchedMovie = false
                    climax = false
                    rating = 3f
                    mood = "平静"
                    props = "手"
                    showDetailsDialog = false
                    isEditing = false
                    editSession = null
                },
                onDismiss = {
                    showDetailsDialog = false
                    isEditing = false
                    editSession = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    HistoryScreen()
}
