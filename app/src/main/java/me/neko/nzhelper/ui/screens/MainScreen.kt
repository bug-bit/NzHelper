package me.neko.nzhelper.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import me.neko.nzhelper.BuildConfig
import me.neko.nzhelper.ui.BottomNavItem
import me.neko.nzhelper.ui.screens.statistics.StatisticsScreen
import me.neko.nzhelper.ui.util.UpdateChecker

@Composable
fun BottomNavigationBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    NavigationBar {
        BottomNavItem.Companion.items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                label = { Text(text = item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // 检查通知权限
    val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    var showNotifyDialog by remember { mutableStateOf(!notificationsEnabled) }

    // 打开应用通知设置
    fun openNotificationSettings(context: Context) {
        val intent = Intent().apply {
            action =
                Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra("app_uid", context.applicationInfo.uid)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

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

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) { HomeScreen() }
            composable(BottomNavItem.History.route) { HistoryScreen() }
            composable(BottomNavItem.Settings.route) { SettingsScreen(navController) }
            composable(BottomNavItem.Statistics.route) { StatisticsScreen() }
            composable("about") { AboutScreen(navController) }
            composable("open_source") { OpenSourceScreen(navController) }
        }

        if (showUpdateDialog && latestTag != null) {
            AlertDialog(
                onDismissRequest = { showUpdateDialog = false },
                title = { Text("检测到新版本") },
                text = {
                    Text(
                        "当前版本：${BuildConfig.VERSION_NAME}\n" +
                                "最新版本：$latestTag\n\n" +
                                "针对你的牛牛进行了一些优化，是否前往 GitHub 下载？"
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showUpdateDialog = false
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            "https://github.com/$owner/$repo/releases/latest".toUri()
                        )
                        context.startActivity(intent)
                    }) { Text("去下载") }
                },
                dismissButton = {
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text("稍后再说")
                    }
                }
            )
        }
        if (showNotifyDialog) {
            AlertDialog(
                onDismissRequest = { showNotifyDialog = false },
                title = {
                    Text(text = "还未开启通知权限")
                },
                text = {
                    Text("为确保应用能在后台继续计时，请授予通知权限！")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            openNotificationSettings(context)
                            showNotifyDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("去开启")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNotifyDialog = false }) {
                        Text("以后再说")
                    }
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
