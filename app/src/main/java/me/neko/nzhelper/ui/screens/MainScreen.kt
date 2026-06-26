package me.neko.nzhelper.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import me.neko.nzhelper.BuildConfig
import me.neko.nzhelper.ui.BottomNavItem
import me.neko.nzhelper.ui.dialog.CustomAppAlertDialog
import me.neko.nzhelper.ui.screens.history.HistoryScreen
import me.neko.nzhelper.ui.screens.home.HomeScreen
import me.neko.nzhelper.ui.screens.lock.AppLockManager
import me.neko.nzhelper.ui.screens.lock.LockScreen
import me.neko.nzhelper.ui.screens.setting.SettingsScreen
import me.neko.nzhelper.ui.screens.statistics.StatisticsScreen
import me.neko.nzhelper.ui.util.UpdateChecker

@Composable
fun BottomNavigationBar(
    pagerState: PagerState,
    onPageSelected: (Int) -> Unit
) {
    ShortNavigationBar(
        containerColor = MaterialTheme.colorScheme.background
    ) {
        BottomNavItem.items.forEachIndexed { index, item ->
            ShortNavigationBarItem(
                icon = {
                    Icon(
                        painter = item.icon(),
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title) },
                selected = pagerState.currentPage == index,
                onClick = { onPageSelected(index) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ── 应用锁状态 ──
    var isLocked by remember {
        mutableStateOf(AppLockManager.isLockEnabled(context))
    }
    var wentToBackground by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    wentToBackground = true
                    if (AppLockManager.isLockEnabled(context)) {
                        AppLockManager.resetAuthentication()
                    }
                }

                Lifecycle.Event.ON_RESUME -> {
                    if (wentToBackground &&
                        AppLockManager.isLockEnabled(context) &&
                        !AppLockManager.isAuthenticated
                    ) {
                        isLocked = true
                    }
                    wentToBackground = false
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // ── 通知权限 ──
    val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    var showNotifyDialog by remember { mutableStateOf(!notificationsEnabled) }

    fun openNotificationSettings(context: Context) {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra("app_uid", context.applicationInfo.uid)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    // ── 更新检查 ──
    val owner = "bug-bit"
    val repo = "NzHelper"
    var showUpdateDialog by remember { mutableStateOf(false) }
    var latestTag by remember { mutableStateOf<String?>(null) }

    fun stripSuffix(version: String): String =
        version.trimStart('v', 'V').substringBefore('-')

    fun parseNumbers(version: String): List<Int> =
        stripSuffix(version)
            .split('.')
            .map { it.toIntOrNull() ?: 0 }
            .let {
                when {
                    it.size >= 3 -> it.take(3)
                    it.size == 2 -> it + listOf(0)
                    it.size == 1 -> it + listOf(0, 0)
                    else -> listOf(0, 0, 0)
                }
            }

    fun isRemoteGreater(local: String, remote: String): Boolean {
        val localNums = parseNumbers(local)
        val remoteNums = parseNumbers(remote)
        for (i in 0..2) {
            if (remoteNums[i] > localNums[i]) return true
            if (remoteNums[i] < localNums[i]) return false
        }
        return false
    }

    LaunchedEffect(Unit) {
        UpdateChecker.fetchLatestVersion(owner, repo)?.let { remoteVer ->
            latestTag = remoteVer
            if (isRemoteGreater(BuildConfig.VERSION_NAME, remoteVer)) {
                showUpdateDialog = true
            }
        }
    }

    // ── Pager 状态 ──
    val pagerState = rememberPagerState(pageCount = { BottomNavItem.items.size })
    val scope = rememberCoroutineScope()

    // ── UI ──
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                BottomNavigationBar(
                    pagerState = pagerState,
                    onPageSelected = { targetPage ->
                        scope.launch {
                            pagerState.animateScrollToPage(targetPage)
                        }
                    }
                )
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                beyondViewportPageCount = BottomNavItem.items.size - 1
            ) { page ->
                val isCurrentPage = pagerState.currentPage == page

                when (BottomNavItem.items[page].route) {
                    BottomNavItem.Home.route -> HomeScreen(isActive = isCurrentPage)
                    BottomNavItem.Statistics.route -> StatisticsScreen(isActive = isCurrentPage)
                    BottomNavItem.History.route -> HistoryScreen(isActive = isCurrentPage)
                    BottomNavItem.Settings.route -> SettingsScreen()
                }
            }
        }

        // 锁屏覆盖层
        if (isLocked) {
            LockScreen(onUnlock = { isLocked = false })
        }
    }

    // 弹窗仅在解锁后显示
    if (!isLocked) {
        if (showUpdateDialog && latestTag != null) {
            CustomAppAlertDialog(
                onDismissRequest = { showUpdateDialog = false },
                iconVector = Icons.Default.Update,
                title = "检测到新版本",
                message = "当前版本：${BuildConfig.VERSION_NAME}\n" +
                        "最新版本：$latestTag\n\n" +
                        "有新版本发布啦，是否前往 GitHub 下载？",
                confirmText = "去下载",
                confirmIcon = Icons.Default.Download,
                dismissText = "稍后再说",
                onConfirm = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        "https://github.com/$owner/$repo/releases/latest".toUri()
                    )
                    context.startActivity(intent)
                }
            )
        }

        if (showNotifyDialog) {
            CustomAppAlertDialog(
                onDismissRequest = { showNotifyDialog = false },
                iconVector = Icons.Default.Notifications,
                title = "还未开启通知权限",
                message = "为确保应用能在后台继续计时，请授予通知权限！",
                confirmText = "开启通知",
                confirmIcon = Icons.Default.Settings,
                dismissText = "暂不开启",
                onConfirm = {
                    openNotificationSettings(context)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen()
}
