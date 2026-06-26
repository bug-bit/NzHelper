package me.neko.nzhelper.ui.screens.setting

import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.neko.nzhelper.data.RecycleBinItem
import me.neko.nzhelper.data.SessionRepository
import me.neko.nzhelper.ui.screens.history.ConfirmDialog
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RecycleBinScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val recycleBinItems = remember { mutableStateListOf<RecycleBinItem>() }
    val autoCleanEnabled = RecycleBinSettings.isAutoCleanEnabled(context)

    var showEmptyDialog by remember { mutableStateOf(false) }
    var showDeleteItemDialog by remember { mutableStateOf<RecycleBinItem?>(null) }

    LaunchedEffect(Unit) {
        SessionRepository.cleanExpiredRecycleBinItems(context)
        val loaded = SessionRepository.loadRecycleBin(context)
        recycleBinItems.clear()
        recycleBinItems.addAll(loaded)
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("回收站") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    if (recycleBinItems.isNotEmpty()) {
                        TextButton(onClick = {
                            scope.launch {
                                SessionRepository.restoreFromRecycleBin(
                                    context,
                                    recycleBinItems.toList()
                                )
                                val count = recycleBinItems.size
                                recycleBinItems.clear()
                                Toast.makeText(context, "已恢复 $count 条记录", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }) { Text("全部恢复") }
                        IconButton(onClick = { showEmptyDialog = true }) {
                            Icon(
                                Icons.Outlined.DeleteForever,
                                "清空回收站",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        if (recycleBinItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "回收站为空",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    16.dp,
                    innerPadding.calculateTopPadding() + 8.dp,
                    16.dp,
                    innerPadding.calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recycleBinItems.toList(), key = { it.session.timestamp.toString() }) { item ->
                    RecycleBinSessionCard(
                        item = item,
                        autoCleanEnabled = autoCleanEnabled,
                        onRestore = {
                            scope.launch {
                                SessionRepository.restoreFromRecycleBin(context, listOf(item))
                                recycleBinItems.remove(item)
                                Toast.makeText(context, "已恢复", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDelete = { showDeleteItemDialog = item }
                    )
                }
            }
        }
    }

    if (showEmptyDialog) {
        ConfirmDialog(
            icon = Icons.Default.Warning, title = "清空回收站",
            message = "此操作不可撤销，确定要永久删除回收站中的所有记录吗？",
            confirmText = "永久删除",
            onConfirm = {
                scope.launch { SessionRepository.clearRecycleBin(context); recycleBinItems.clear() }
                showEmptyDialog = false
            },
            onDismiss = { showEmptyDialog = false }
        )
    }

    showDeleteItemDialog?.let { item ->
        ConfirmDialog(
            icon = Icons.Default.Warning, title = "永久删除",
            message = "此操作不可撤销，确定要永久删除这条记录吗？",
            confirmText = "永久删除",
            onConfirm = {
                scope.launch {
                    SessionRepository.deleteFromRecycleBin(context, listOf(item))
                    recycleBinItems.remove(item)
                    Toast.makeText(context, "已永久删除", Toast.LENGTH_SHORT).show()
                }
                showDeleteItemDialog = null
            },
            onDismiss = { showDeleteItemDialog = null }
        )
    }
}

@Composable
private fun RecycleBinSessionCard(
    item: RecycleBinItem,
    autoCleanEnabled: Boolean,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm") }

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L.milliseconds)
            currentTime = System.currentTimeMillis()
        }
    }

    val countdownInfo = remember(item.deletedTimestamp, autoCleanEnabled, currentTime) {
        if (!autoCleanEnabled) {
            "自动清理已关闭" to false
        } else {
            val retentionMillis = RecycleBinSettings.RETENTION_DAYS * 24 * 60 * 60 * 1000L
            val endTime = item.deletedTimestamp + retentionMillis
            val timeLeft = endTime - currentTime

            if (timeLeft <= 0) {
                "等待自动清理中..." to true
            } else {
                val days = TimeUnit.MILLISECONDS.toDays(timeLeft)
                val hours = TimeUnit.MILLISECONDS.toHours(timeLeft) % 24
                val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeft) % 60

                val timeStr = when {
                    days > 0 -> "${days}天${hours}小时${minutes}分钟"
                    hours > 0 -> "${hours}小时${minutes}分钟"
                    minutes > 0 -> "${minutes}分钟"
                    else -> "不到1分钟"
                }

                val isUrgent = timeLeft <= 3 * 24 * 60 * 60 * 1000L
                "${timeStr}后自动删除" to isUrgent
            }
        }
    }

    val (countdownText, isUrgent) = countdownInfo

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.session.timestamp.format(dateFormatter),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    buildString {
                        val durationSecs = item.session.duration
                        val dMin = durationSecs / 60
                        val dSec = durationSecs % 60
                        val durationText = when {
                            dMin > 0 && dSec > 0 -> "${dMin}分${dSec}秒"
                            dMin > 0 -> "${dMin}分钟"
                            else -> "${dSec}秒"
                        }
                        append("$durationText · ${item.session.mood} · 评分${item.session.rating}")
                        if (item.session.location.isNotEmpty()) append(" · ${item.session.location}")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.session.remark.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        item.session.remark,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = countdownText,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isUrgent && autoCleanEnabled -> MaterialTheme.colorScheme.error
                        !autoCleanEnabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    }
                )
            }

            IconButton(onClick = onRestore) {
                Icon(
                    Icons.Outlined.Restore,
                    contentDescription = "恢复",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.DeleteForever,
                    contentDescription = "永久删除",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}