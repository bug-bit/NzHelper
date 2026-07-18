package me.neko.nzhelper.feature.home

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import me.neko.nzhelper.core.auto.AutoTagRules
import me.neko.nzhelper.core.database.SessionRepository
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.core.model.SessionFormState
import me.neko.nzhelper.core.service.TimerService
import me.neko.nzhelper.feature.home.components.ConfirmResetDialog
import me.neko.nzhelper.feature.home.components.ConfirmStopDialog
import me.neko.nzhelper.feature.home.components.SinceLastCard
import me.neko.nzhelper.feature.home.components.TimelineItem
import me.neko.nzhelper.feature.home.components.TimerCard
import me.neko.nzhelper.ui.component.dialog.DetailsDialog
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun HomeScreen(isActive: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    // 绑定 Service
    val serviceIntent = remember { Intent(context, TimerService::class.java) }
    var timerService by remember { mutableStateOf<TimerService?>(null) }
    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                timerService = (binder as TimerService.LocalBinder).getService()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                timerService = null
            }
        }
    }

    // 启动并绑定服务
    LaunchedEffect(Unit) {
        val autoStart = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
            .getBoolean("auto_start_timer", false)

        if (autoStart) {
            ContextCompat.startForegroundService(
                context,
                serviceIntent.apply { action = TimerService.ACTION_START }
            )
        }
        context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }
    DisposableEffect(Unit) {
        onDispose { context.unbindService(connection) }
    }

    val elapsedSeconds by timerService?.elapsedSec?.collectAsState(initial = 0)
        ?: remember { mutableIntStateOf(0) }
    val isServiceRunning by timerService?.isRunning?.collectAsState(initial = false)
        ?: remember { mutableStateOf(false) }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showManualAddDialog by remember { mutableStateOf(false) }
    var formState by remember { mutableStateOf(SessionFormState()) }
    val sessions = remember { mutableStateListOf<Session>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val loaded = SessionRepository.loadSessions(context)
            .sortedByDescending { it.timestamp }
        sessions.clear()
        sessions.addAll(loaded)
        isLoading = false
    }
    LaunchedEffect(isActive) {
        if (isActive) {
            val loaded = SessionRepository.loadSessions(context)
                .sortedByDescending { it.timestamp }
            sessions.clear()
            sessions.addAll(loaded)
            isLoading = false
        }
    }

    var nowTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000L.milliseconds)
            nowTick = System.currentTimeMillis()
        }
    }
    val sinceLastText = remember(sessions.size, nowTick) {
        val latest = sessions.firstOrNull()?.timestamp ?: return@remember "还没有记录哦"
        val mins = java.time.Duration.between(latest, LocalDateTime.now()).toMinutes()
        when {
            mins < 1 -> "刚刚"
            mins < 60 -> "${mins}分钟前"
            mins < 1440 -> "${mins / 60}小时前"
            mins < 10080 -> "${mins / 1440}天前"
            else -> "${mins / 10080}周前"
        }
    }
    val hasRecord = sessions.isNotEmpty()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(text = "牛子小助手") },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        )
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // 计时器主区域
                item {
                    TimerCard(
                        elapsedSeconds = elapsedSeconds,
                        isRunning = isServiceRunning,
                        onToggleRun = {
                            if (isServiceRunning) {
                                context.startService(serviceIntent.apply {
                                    action = TimerService.ACTION_PAUSE
                                })
                            } else {
                                ContextCompat.startForegroundService(
                                    context,
                                    serviceIntent.apply { action = TimerService.ACTION_START }
                                )
                            }
                        },
                        onStop = {
                            if (elapsedSeconds > 0) showConfirmDialog = true
                            else Toast.makeText(context, "计时尚未开始", Toast.LENGTH_SHORT).show()
                        },
                        onReset = {
                            if (elapsedSeconds > 0) {
                                showResetConfirmDialog = true
                            } else {
                                Toast.makeText(context, "计时尚未开始", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                // 距上次记录
                item {
                    SinceLastCard(
                        sinceLastText = sinceLastText,
                        hasRecord = hasRecord
                    )
                }

                // 手动添加记录
                item {
                    FilledTonalButton(
                        onClick = {
                            val nowTime = LocalDateTime.now()
                            val suggested = AutoTagRules.suggest(context, nowTime)
                            formState = SessionFormState(
                                categoryId = TagSettings.defaultCategory(context).id,
                                tagIds = suggested,
                                autoTagIds = suggested,
                                manualYear = nowTime.year,
                                manualMonth = nowTime.monthValue,
                                manualDay = nowTime.dayOfMonth,
                                manualHour = nowTime.hour,
                                manualMinute = nowTime.minute
                            )
                            showManualAddDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Outlined.AddCircle,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "手动添加记录",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (sessions.isNotEmpty()) {
                    item {
                        Card(
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceBright
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                val recentSessions = sessions.take(8)

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(MaterialTheme.shapes.medium)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Timeline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "活动时间轴",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "共 ${sessions.size} 次记录",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                recentSessions.forEachIndexed { index, session ->
                                    TimelineItem(
                                        session = session,
                                        isLast = index == recentSessions.lastIndex
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item { EmptyStateCard() }
                }
            }
        }
    }

    if (showConfirmDialog) {
        ConfirmStopDialog(
            onDismiss = { showConfirmDialog = false },
            onConfirm = {
                showConfirmDialog = false
                val nowTime = LocalDateTime.now()
                val suggested = AutoTagRules.suggest(context, nowTime)
                formState = formState.copy(
                    tagIds = suggested,
                    autoTagIds = suggested
                )
                showDetailsDialog = true
                context.startService(serviceIntent.apply {
                    action = TimerService.ACTION_PAUSE
                })
            }
        )
    }

    if (showResetConfirmDialog) {
        ConfirmResetDialog(
            onDismiss = { showResetConfirmDialog = false },
            onConfirm = {
                showResetConfirmDialog = false
                context.startService(serviceIntent.apply {
                    action = TimerService.ACTION_RESET
                })
            }
        )
    }

    DetailsDialog(
        show = showDetailsDialog,
        formState = formState,
        onFormStateChange = { formState = it },
        onConfirm = {
            val nowTime = LocalDateTime.now()
            val finalTags = run {
                val suggested = AutoTagRules.suggest(context, nowTime)
                val (merged, added) = AutoTagRules.merge(formState.tagIds, suggested)
                merged to (formState.autoTagIds + added)
            }
            val session = Session(
                timestamp = nowTime,
                duration = elapsedSeconds,
                remark = formState.remark,
                rating = formState.rating,
                climax = formState.climax,
                categoryId = formState.categoryId.ifBlank { TagSettings.defaultCategory(context).id },
                tagIds = finalTags.first.toList()
            )
            sessions.add(0, session)
            scope.launch { SessionRepository.saveSessions(context, sessions) }

            formState = SessionFormState()
            showDetailsDialog = false
            context.startService(serviceIntent.apply { action = TimerService.ACTION_STOP })
        },
        onDismiss = {
            showDetailsDialog = false
            context.startService(serviceIntent.apply { action = TimerService.ACTION_START })
        }
    )

    DetailsDialog(
        show = showManualAddDialog,
        formState = formState,
        onFormStateChange = { formState = it },
        showDurationField = true,
        title = "手动添加记录",
        onConfirm = {
            val duration = formState.manualDurationSeconds
            if (duration <= 0) {
                Toast.makeText(context, "请输入时长", Toast.LENGTH_SHORT).show()
                return@DetailsDialog
            }
            val timestamp = try {
                formState.toLocalDateTime()
            } catch (_: Exception) {
                Toast.makeText(context, "日期时间无效，请重新选择", Toast.LENGTH_SHORT)
                    .show()
                return@DetailsDialog
            }
            val session = Session(
                timestamp = timestamp,
                duration = duration,
                remark = formState.remark,
                rating = formState.rating,
                climax = formState.climax,
                categoryId = formState.categoryId.ifBlank { TagSettings.defaultCategory(context).id },
                tagIds = formState.tagIds.toList()
            )
            sessions.add(0, session)
            scope.launch { SessionRepository.saveSessions(context, sessions) }

            formState = SessionFormState()
            showManualAddDialog = false
        },
        onDismiss = { showManualAddDialog = false }
    )
}

@Composable
private fun EmptyStateCard() {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "(。・ω・。)",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            Text(
                text = "暂无记录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = "点击上方「开始」计时，或手动添加第一条记录",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}
