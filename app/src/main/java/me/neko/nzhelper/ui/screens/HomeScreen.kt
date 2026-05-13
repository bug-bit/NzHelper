package me.neko.nzhelper.ui.screens

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import me.neko.nzhelper.data.Session
import me.neko.nzhelper.data.SessionRepository
import me.neko.nzhelper.ui.dialog.DetailsDialog
import me.neko.nzhelper.ui.service.TimerService
import java.time.LocalDateTime


@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class
)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val scope = rememberCoroutineScope()
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    // 绑定 Service
    val serviceIntent = remember(appContext) { Intent(appContext, TimerService::class.java) }
    var timerService by remember { mutableStateOf<TimerService?>(null) }
    var isServiceBound by remember { mutableStateOf(false) }
    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                timerService = (binder as TimerService.LocalBinder).getService()
                isServiceBound = true
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                timerService = null
                isServiceBound = false
            }
        }
    }

    // 仅绑定服务，避免首次进入页面就自动开始计时
    LaunchedEffect(Unit) {
        isServiceBound = appContext.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }
    DisposableEffect(appContext, connection, isServiceBound) {
        onDispose {
            if (isServiceBound) {
                appContext.unbindService(connection)
                isServiceBound = false
            }
        }
    }

    val elapsedSeconds = timerService?.elapsedSec?.collectAsState(initial = 0)?.value ?: 0
    val isRunning = timerService?.isRunning?.collectAsState(initial = false)?.value ?: false

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }

    var remarkInput by remember { mutableStateOf("") }
    var locationInput by remember { mutableStateOf("") }
    var watchedMovie by remember { mutableStateOf(false) }
    var climax by remember { mutableStateOf(false) }
    var rating by remember { mutableFloatStateOf(3f) }
    var mood by remember { mutableStateOf("平静") }
    var props by remember { mutableStateOf("手") }

    val sessions = remember { mutableStateListOf<Session>() }

    // 加载自定义选项（带默认值）
    var customOptions by remember { mutableStateOf(me.neko.nzhelper.data.CustomOptions()) }
    LaunchedEffect(Unit) {
        customOptions = SessionRepository.loadCustomOptions(context)
    }

    // 加载历史（仍然需要加载，用于后续保存新记录时合并）
    LaunchedEffect(Unit) {
        val loaded = SessionRepository.loadSessions(context)
        sessions.clear()
        sessions.addAll(loaded)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "牛牛小助手") },
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
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 32.dp, horizontal = 16.dp)
            ) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        Text(
                            text = "记录",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = if (isRunning) "开始啦~" else "准备开始……",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text = formatTime(elapsedSeconds),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(48.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val action = if (isRunning) {
                                        TimerService.ACTION_PAUSE
                                    } else {
                                        TimerService.ACTION_START
                                    }
                                    ContextCompat.startForegroundService(
                                        appContext,
                                        Intent(appContext, TimerService::class.java).apply {
                                            this.action = action
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isRunning) "暂停" else "开始",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (elapsedSeconds > 0) showConfirmDialog = true
                                    else Toast.makeText(context, "计时尚未开始", Toast.LENGTH_SHORT)
                                        .show()
                                },
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(
                                        MaterialTheme.colorScheme.errorContainer,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Stop,
                                    contentDescription = "结束",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (showConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmDialog = false },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    title = {
                        Text(
                            text = "结束了吗？",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    text = {
                        Text(
                            text = "真的要结束了吗？",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    confirmButton = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showConfirmDialog = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text("还不可以…")
                            }

                            Button(
                                onClick = {
                                    showConfirmDialog = false
                                    showDetailsDialog = true
                                    ContextCompat.startForegroundService(
                                        appContext,
                                        Intent(appContext, TimerService::class.java).apply {
                                            action = TimerService.ACTION_PAUSE
                                        }
                                    )
                                },
                                modifier = Modifier.height(44.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("燃尽了……")
                            }
                        }
                    },

                    dismissButton = {}
                )
            }

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
                customMoods = customOptions.moods,
                customProps = customOptions.props,
                onConfirm = {
                    val now = LocalDateTime.now()
                    val session = Session(
                        timestamp = now,
                        duration = elapsedSeconds,
                        remark = remarkInput,
                        location = locationInput,
                        watchedMovie = watchedMovie,
                        climax = climax,
                        rating = rating,
                        mood = mood,
                        props = props
                    )
                    sessions.add(session)
                    scope.launch {
                        SessionRepository.saveSessions(context, sessions.toList())
                    }

                    // 重置所有输入状态
                    remarkInput = ""
                    locationInput = ""
                    watchedMovie = false
                    climax = false
                    rating = 3f
                    mood = "平静"
                    props = "手"
                    showDetailsDialog = false

                    // 停止计时服务
                    appContext.startService(
                        Intent(appContext, TimerService::class.java).apply {
                            action = TimerService.ACTION_STOP
                        }
                    )
                },
                onDismiss = {
                    showDetailsDialog = false
                }
            )
        }
    }
}

@SuppressLint("DefaultLocale")
private fun formatTime(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return buildString {
        if (h > 0) append(String.format("%02d:", h))
        append(String.format("%02d:%02d", m, s))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}
