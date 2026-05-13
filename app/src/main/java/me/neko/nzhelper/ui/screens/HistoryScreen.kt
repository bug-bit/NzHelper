package me.neko.nzhelper.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.gson.JsonParser.parseString
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.neko.nzhelper.NzApplication
import me.neko.nzhelper.data.CustomOptions
import me.neko.nzhelper.data.Session
import me.neko.nzhelper.data.SessionRepository
import me.neko.nzhelper.ui.dialog.CustomAppAlertDialog
import me.neko.nzhelper.ui.dialog.ManualAddDialog
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
    val scope = rememberCoroutineScope()

    val sessions = remember { mutableStateListOf<Session>() }
    val sessionsTypeToken = object : TypeToken<List<Session>>() {}.type

    var editSession by remember { mutableStateOf<Session?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    
    // 加载自定义选项（带默认值）
    var customOptions by remember { mutableStateOf(me.neko.nzhelper.data.CustomOptions()) }
    LaunchedEffect(Unit) {
        customOptions = SessionRepository.loadCustomOptions(context)
    }
    
    // 手动添加记录的状态
    var showManualAddDialog by remember { mutableStateOf(false) }
    var manualDateTime by remember { mutableStateOf(LocalDateTime.now()) }
    var manualDurationSeconds by remember { mutableIntStateOf(0) }

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
    
    // 自定义选项导入导出
    val importOptionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { importUri ->
            scope.launch {
                try {
                    context.contentResolver.openInputStream(importUri)?.use { inputStream ->
                        val jsonStr = inputStream.bufferedReader().readText()
                        val importedOptions = NzApplication.gson.fromJson(
                            jsonStr,
                            object : TypeToken<CustomOptions>() {}.type
                        ) as? CustomOptions
                        
                        if (importedOptions != null) {
                            SessionRepository.saveCustomOptions(context, importedOptions)
                            customOptions = importedOptions
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "成功导入自定义选项",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(context, "导入失败：文件格式不正确", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "导入失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val exportOptionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { exportUri ->
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(exportUri)?.use { outputStream ->
                        val json = NzApplication.gson.toJson(customOptions)
                        outputStream.write(json.toByteArray())
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "成功导出自定义选项",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "导出失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { importUri ->
            scope.launch {
                try {
                    context.contentResolver.openInputStream(importUri)?.use { inputStream ->
                        val jsonStr = inputStream.bufferedReader().readText()
                        val importedSessions = mutableListOf<Session>()

                        var success = false
                        
                        // 1. 首先尝试新格式（完整字段名的 JSON 对象）
                        try {
                            val newList: List<Session> =
                                NzApplication.gson.fromJson(jsonStr, sessionsTypeToken)
                            // 过滤掉没有 timestamp 的数据
                            importedSessions.addAll(newList.filter { it.timestamp != null })
                            if (importedSessions.isNotEmpty()) {
                                success = true
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // 新格式失败，继续尝试旧格式
                        }

                        // 2. 如果新格式失败，尝试旧数组格式 [[timestamp, duration, ...]]
                        if (!success) {
                            try {
                                val root = parseString(jsonStr).asJsonArray
                                for (elem in root) {
                                    if (elem.isJsonArray) {
                                        val arr = elem.asJsonArray
                                        val timeStr = arr[0].asString
                                        val timestamp = LocalDateTime.parse(
                                            timeStr,
                                            DateTimeFormatter.ISO_LOCAL_DATE_TIME
                                        )

                                        val duration = if (arr.size() > 1) arr[1].asInt else 0
                                        val remark =
                                            if (arr.size() > 2 && !arr[2].isJsonNull) arr[2].asString else ""
                                        val location =
                                            if (arr.size() > 3 && !arr[3].isJsonNull) arr[3].asString else ""
                                        val watchedMovie =
                                            if (arr.size() > 4) arr[4].asBoolean else false
                                        val climax = if (arr.size() > 5) arr[5].asBoolean else false
                                        val rating = if (arr.size() > 6 && !arr[6].isJsonNull)
                                            arr[6].asFloat.coerceIn(0f, 5f) else 3f
                                        val mood =
                                            if (arr.size() > 7 && !arr[7].isJsonNull) arr[7].asString else "平静"
                                        val props =
                                            if (arr.size() > 8 && !arr[8].isJsonNull) arr[8].asString else "手"

                                        importedSessions.add(
                                            Session(
                                                timestamp = timestamp,
                                                duration = duration,
                                                remark = remark,
                                                location = location,
                                                watchedMovie = watchedMovie,
                                                climax = climax,
                                                rating = rating,
                                                mood = mood,
                                                props = props
                                            )
                                        )
                                    }
                                }
                                if (importedSessions.isNotEmpty()) {
                                    success = true
                                }
                            } catch (parseException: Exception) {
                                parseException.printStackTrace()
                            }
                        }
                        
                        // 3. 如果还是失败，尝试解析单字母字段名的 JSON 对象格式 {"a":..., "b":...}
                        if (!success) {
                            try {
                                val jsonArray = parseString(jsonStr).asJsonArray
                                for (elem in jsonArray) {
                                    if (elem.isJsonObject) {
                                        val obj = elem.asJsonObject
                                        val timeStr = obj.get("a")?.asString
                                        if (timeStr != null) {
                                            val timestamp = LocalDateTime.parse(
                                                timeStr,
                                                DateTimeFormatter.ISO_LOCAL_DATE_TIME
                                            )
                                            val duration = obj.get("b")?.asInt ?: 0
                                            val remark = obj.get("c")?.asString ?: ""
                                            val location = obj.get("d")?.asString ?: ""
                                            val watchedMovie = obj.get("e")?.asBoolean ?: false
                                            val climax = obj.get("f")?.asBoolean ?: false
                                            val rating = obj.get("g")?.asFloat?.coerceIn(0f, 5f) ?: 3f
                                            val mood = obj.get("h")?.asString ?: "平静"
                                            val props = obj.get("i")?.asString ?: "手"

                                            importedSessions.add(
                                                Session(
                                                    timestamp = timestamp,
                                                    duration = duration,
                                                    remark = remark,
                                                    location = location,
                                                    watchedMovie = watchedMovie,
                                                    climax = climax,
                                                    rating = rating,
                                                    mood = mood,
                                                    props = props
                                                )
                                            )
                                        }
                                    }
                                }
                                if (importedSessions.isNotEmpty()) {
                                    success = true
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        // 统一处理导入结果
                        if (importedSessions.isNotEmpty()) {
                            val sortedSessions = importedSessions.sortedByDescending { it.timestamp }
                            sessions.clear()
                            sessions.addAll(sortedSessions)
                            SessionRepository.saveSessions(context, sortedSessions)

                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "成功导入 ${importedSessions.size} 条记录",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(context, "导入失败：文件格式不正确", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { exportUri ->
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(exportUri)?.use { os ->
                        OutputStreamWriter(os).use { writer ->
                            writer.write(NzApplication.gson.toJson(sessions))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val loaded = SessionRepository.loadSessions(context)
            val validSessions = loaded.filter { it.timestamp != null }
                .sortedByDescending { it.timestamp }
            sessions.clear()
            sessions.addAll(validSessions)
        } catch (e: Exception) {
            e.printStackTrace()
            sessions.clear()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
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
                                exportLauncher.launch("NzHelper_export_${System.currentTimeMillis()}.json")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("导入数据（将覆盖当前）") },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // 打开手动添加记录弹窗
                    manualDateTime = LocalDateTime.now()
                    manualDurationSeconds = 0
                    remarkInput = ""
                    locationInput = ""
                    watchedMovie = false
                    climax = false
                    rating = 3f
                    mood = "平静"
                    props = "手"
                    showManualAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "手动添加记录"
                )
            }
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("(。・ω・。)", style = MaterialTheme.typography.titleLarge)
                        Text("暂无历史记录哦！", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(
                        items = sessions,
                        key = { session -> session.timestamp?.toString() ?: session.hashCode() }
                    ) { session ->
                        
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                                            session.timestamp!!.format(
                                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                            )
                                        )
                                        Text("持续: ${formatTime(session.duration)}")
                                        if (session.remark.isNotBlank()) {
                                            Text(
                                                "备注: ${session.remark}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(onClick = { sessionToDelete = session }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "删除")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 删除确认
            sessionToDelete?.let { session ->
                CustomAppAlertDialog(
                    onDismissRequest = { sessionToDelete = null },
                    iconVector = Icons.Rounded.Warning,
                    title = "删除记录",
                    message = "确认删除此记录吗？删除后不可恢复。",
                    confirmText = "删除",
                    confirmIcon = Icons.Rounded.Delete,
                    dismissText = "取消",
                    onConfirm = {
                        sessions.remove(session)
                        scope.launch {
                            SessionRepository.saveSessions(context, sessions.toList())
                        }
                    },
                    onDismiss = { sessionToDelete = null },
                    modifier = Modifier
                )
            }

            // 清除全部确认
            if (showClearDialog) {
                CustomAppAlertDialog(
                    onDismissRequest = { showClearDialog = false },
                    iconVector = Icons.Rounded.Warning,
                    title = "清除全部记录",
                    message = "此操作不可撤销，确定要删除所有记录吗？",
                    confirmText = "删除全部",
                    confirmIcon = Icons.Rounded.DeleteForever,
                    dismissText = "取消",
                    onConfirm = {
                        sessions.clear()
                        scope.launch {
                            SessionRepository.saveSessions(context, emptyList())
                        }
                    },
                    onDismiss = { showClearDialog = false },
                    modifier = Modifier
                )
            }

            // 查看详情
            sessionToView?.let { session ->
                Dialog(onDismissRequest = { sessionToView = null }) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        tonalElevation = 6.dp,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .wrapContentHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "详情",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            HorizontalDivider()

                            DetailRow(
                                "开始时间",
                                session.timestamp?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) ?: "未知"
                            )
                            DetailRow("持续时长", formatTime(session.duration))
                            DetailRow("地点", session.location.ifEmpty { "无" })
                            DetailRow("备注", session.remark.ifEmpty { "无" })
                            DetailRow("观看小电影", if (session.watchedMovie) "是" else "否")
                            DetailRow("发射", if (session.climax) "是" else "否")
                            DetailRow("道具", session.props)
                            DetailRow("评分", "%.1f / 5.0".format(session.rating))
                            DetailRow("心情", session.mood)

                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                            ) {
                                Button(
                                    onClick = { sessionToView = null },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Text("关闭")
                                }

                                Button(
                                    onClick = {
                                        // 使用 ManualAddDialog 进行编辑
                                        manualDateTime = session.timestamp ?: LocalDateTime.now()
                                        manualDurationSeconds = session.duration
                                        remarkInput = session.remark
                                        locationInput = session.location
                                        watchedMovie = session.watchedMovie
                                        climax = session.climax
                                        rating = session.rating
                                        mood = session.mood
                                        props = session.props
                                        editSession = session
                                        isEditing = true
                                        showManualAddDialog = true
                                        sessionToView = null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Icon(Icons.Rounded.Edit, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("编辑")
                                }
                            }
                        }
                    }
                }
            }

            // 手动添加记录弹窗
            ManualAddDialog(
                show = showManualAddDialog,
                selectedDateTime = manualDateTime,
                onSelectedDateTimeChange = { manualDateTime = it },
                durationSeconds = manualDurationSeconds,
                onDurationSecondsChange = { manualDurationSeconds = it },
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
                customMoods = customOptions.moods,
                customProps = customOptions.props,
                onConfirm = {
                    if (isEditing && editSession != null) {
                        val index = sessions.indexOf(editSession)
                        if (index != -1) {
                            sessions[index] = Session(
                                timestamp = manualDateTime,
                                duration = manualDurationSeconds,
                                remark = remarkInput,
                                location = locationInput,
                                watchedMovie = watchedMovie,
                                climax = climax,
                                rating = rating,
                                mood = mood,
                                props = props
                            )
                        }
                    } else {
                        // 新增模式：添加新记录
                        val newSession = Session(
                            timestamp = manualDateTime,
                            duration = manualDurationSeconds,
                            remark = remarkInput,
                            location = locationInput,
                            watchedMovie = watchedMovie,
                            climax = climax,
                            rating = rating,
                            mood = mood,
                            props = props
                        )
                        sessions.add(0, newSession)
                    }

                    val sortedSessions = sessions.sortedByDescending { it.timestamp }
                    sessions.clear()
                    sessions.addAll(sortedSessions)

                    scope.launch {
                        SessionRepository.saveSessions(context, sortedSessions)
                    }

                    showManualAddDialog = false
                    manualDateTime = LocalDateTime.now()
                    manualDurationSeconds = 0
                    isEditing = false
                    editSession = null
                },
                onDismiss = {
                    showManualAddDialog = false
                    manualDateTime = LocalDateTime.now()
                    manualDurationSeconds = 0
                    isEditing = false
                    editSession = null
                }
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row {
        Text(
            text = "$label：",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    HistoryScreen()
}
