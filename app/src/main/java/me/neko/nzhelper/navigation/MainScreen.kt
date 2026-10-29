package me.neko.nzhelper.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.NavController
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.neko.nzhelper.NzApplication
import me.neko.nzhelper.core.achievement.AchievementRepository
import me.neko.nzhelper.core.database.ProfileSummaryRepository
import me.neko.nzhelper.core.datastore.UpdateSettings
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.core.util.UpdateCheckManager
import me.neko.nzhelper.feature.achievement.components.AchievementUnlockDialog
import me.neko.nzhelper.feature.addrecord.AddRecordFlow
import me.neko.nzhelper.feature.history.HistoryScreen
import me.neko.nzhelper.feature.home.HomeScreen
import me.neko.nzhelper.feature.lock.AppLockManager
import me.neko.nzhelper.feature.lock.GestureLockManager
import me.neko.nzhelper.feature.lock.LockScreen
import me.neko.nzhelper.feature.mine.MineScreen
import me.neko.nzhelper.feature.statistics.StatisticsScreen
import me.neko.nzhelper.ui.component.dialog.CustomAppAlertDialog
import me.neko.nzhelper.ui.component.dialog.UpdateAvailableDialog
import me.neko.nzhelper.ui.theme.LocalHazeState
import me.neko.nzhelper.ui.theme.LocalThemeState

@Composable
fun BottomNavigationBar(
    pagerState: PagerState,
    onPageSelected: (Int) -> Unit
) {
    ShortNavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
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
fun MainScreen(
    rootNavController: NavController,
    stopRequest: StateFlow<Int>? = null
) {
    val context = LocalContext.current

    // ── 应用锁状态 ──
    var isLocked by remember {
        mutableStateOf(
            (AppLockManager.isLockEnabled(context) || GestureLockManager.hasGesturePassword(context))
                    && !AppLockManager.isAuthenticated
        )
    }
    var wentToBackground by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    wentToBackground = true
                    if (AppLockManager.isLockEnabled(context) || GestureLockManager.hasGesturePassword(
                            context
                        )
                    ) {
                        AppLockManager.resetAuthentication()
                    }
                }

                Lifecycle.Event.ON_START -> {
                    if (wentToBackground &&
                        (AppLockManager.isLockEnabled(context) || GestureLockManager.hasGesturePassword(
                            context
                        )) &&
                        !AppLockManager.isAuthenticated
                    ) {
                        isLocked = true
                    }
                    wentToBackground = false
                }

                else -> {}
            }
        }

        val processLifecycleOwner = ProcessLifecycleOwner.get()
        processLifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            processLifecycleOwner.lifecycle.removeObserver(observer)
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
    val pendingAutoUpdate by UpdateCheckManager.pendingAutoUpdate.collectAsState()

    LaunchedEffect(Unit) {
        UpdateCheckManager.startAutoCheck(context)
    }

    // ── 成就 ──
    LaunchedEffect(Unit) {
        AchievementRepository.sync(context)
        ProfileSummaryRepository.refresh(context)
    }
    val pendingAchievementUnlocks by AchievementRepository.newUnlocks.collectAsState()

    // ── Pager 状态 ──
    val pagerState = rememberPagerState(pageCount = { BottomNavItem.items.size })
    val scope = rememberCoroutineScope()
    val stopRequestId by stopRequest?.collectAsState(initial = 0)
        ?: remember { mutableIntStateOf(0) }

    LaunchedEffect(stopRequestId) {
        if (stopRequestId > 0) {
            pagerState.scrollToPage(BottomNavItem.items.indexOf(BottomNavItem.Home))
        }
    }

    fun openAddRecord(flow: AddRecordFlow, elapsedSeconds: Int, session: Session?) {
        val editArg = session?.let {
            "&editSession=${Uri.encode(NzApplication.gson.toJson(it))}"
        } ?: ""
        rootNavController.navigate(
            "add_record?flow=${flow.key}&elapsed=$elapsedSeconds$editArg"
        )
    }

    // ── UI ──
    val floatingBottomBar = LocalThemeState.current.floatingBottomBar
    val blurEffect = LocalThemeState.current.blurEffect
    val hazeState = LocalHazeState.current?.takeIf { floatingBottomBar && blurEffect }
    val onPageSelected: (Int) -> Unit = { targetPage ->
        scope.launch { pagerState.animateScrollToPage(targetPage) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            bottomBar = {
                if (!floatingBottomBar) {
                    BottomNavigationBar(
                        pagerState = pagerState,
                        onPageSelected = onPageSelected
                    )
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .then(
                        if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier
                    ),
                beyondViewportPageCount = 1
            ) { page ->
                val isCurrentPage = pagerState.currentPage == page

                when (BottomNavItem.items[page].route) {
                    BottomNavItem.Home.route -> HomeScreen(
                        isActive = isCurrentPage,
                        stopRequestId = if (isLocked) 0 else stopRequestId,
                        onOpenAddRecord = { flow, elapsed ->
                            openAddRecord(flow, elapsed, null)
                        },
                        onOpenHistory = {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    BottomNavItem.items.indexOf(BottomNavItem.History)
                                )
                            }
                        }
                    )

                    BottomNavItem.Statistics.route -> StatisticsScreen(isActive = isCurrentPage)
                    BottomNavItem.History.route -> HistoryScreen(
                        isActive = isCurrentPage,
                        onEditRecord = { session ->
                            openAddRecord(AddRecordFlow.EDIT, 0, session)
                        }
                    )

                    BottomNavItem.Mine.route -> MineScreen(
                        rootNavController = rootNavController
                    )
                }
            }
        }

        if (floatingBottomBar) {
            FloatingBottomBar(
                items = BottomNavItem.items,
                pagerState = pagerState,
                onItemClick = onPageSelected,
                modifier = Modifier.align(Alignment.BottomCenter),
                hazeState = hazeState
            )
        }

        // 锁屏覆盖层
        if (isLocked) {
            LockScreen(onUnlock = { isLocked = false })
        }
    }

    // 弹窗仅在解锁后显示
    if (!isLocked) {
        pendingAutoUpdate?.let { info ->
            UpdateAvailableDialog(
                info = info,
                onDismissRequest = { UpdateCheckManager.pendingAutoUpdate.value = null },
                onIgnore = { UpdateSettings.setIgnoredVersion(context, info.tag) }
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

        if (pendingAchievementUnlocks.isNotEmpty()) {
            AchievementUnlockDialog(
                achievements = pendingAchievementUnlocks,
                onDismiss = { AchievementRepository.consumeNewUnlocks() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen(rootNavController = NavController(LocalContext.current))
}
