package me.neko.nzhelper

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import me.neko.nzhelper.feature.about.AboutScreen
import me.neko.nzhelper.feature.about.OpenSourceScreen
import me.neko.nzhelper.feature.ai.AiConfigScreen
import me.neko.nzhelper.feature.ai.AiProviderListScreen
import me.neko.nzhelper.feature.backup.BackupScreen
import me.neko.nzhelper.feature.crash.CrashLogScreen
import me.neko.nzhelper.feature.lock.GestureLockSetupScreen
import me.neko.nzhelper.feature.recyclebin.RecycleBinScreen
import me.neko.nzhelper.feature.recyclebin.RecycleBinSettingsScreen
import me.neko.nzhelper.feature.settings.ChartManageScreen
import me.neko.nzhelper.feature.settings.ThemeSettingsScreen
import me.neko.nzhelper.feature.tagmanage.TagManageScreen
import me.neko.nzhelper.navigation.MainScreen
import me.neko.nzhelper.navigation.screenEnter
import me.neko.nzhelper.navigation.screenExit
import me.neko.nzhelper.navigation.screenPopEnter
import me.neko.nzhelper.navigation.screenPopExit
import me.neko.nzhelper.ui.theme.NzHelperTheme

class MainActivity : AppCompatActivity() {
    private val stopRequest = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            NzHelperTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "main",
                    enterTransition = { screenEnter() },
                    exitTransition = { screenExit() },
                    popEnterTransition = { screenPopEnter() },
                    popExitTransition = { screenPopExit() }
                ) {
                        composable("main") {
                            MainScreen(
                                rootNavController = navController,
                                stopRequest = stopRequest
                            )
                        }
                        composable("about") {
                            val aboutNav = rememberNavController()
                            NavHost(
                                navController = aboutNav,
                                startDestination = "about",
                                enterTransition = { screenEnter() },
                                exitTransition = { screenExit() },
                                popEnterTransition = { screenPopEnter() },
                                popExitTransition = { screenPopExit() }
                            ) {
                                composable("about") { AboutScreen(aboutNav) }
                                composable("open_source") { OpenSourceScreen(aboutNav) }
                            }
                        }
                        composable("ai_config") {
                            val aiNav = rememberNavController()
                            NavHost(
                                navController = aiNav,
                                startDestination = "config",
                                enterTransition = { screenEnter() },
                                exitTransition = { screenExit() },
                                popEnterTransition = { screenPopEnter() },
                                popExitTransition = { screenPopExit() }
                            ) {
                                composable("config") {
                                    AiConfigScreen(
                                        onBack = { navController.popBackStack() },
                                        onProviders = { aiNav.navigate("providers") }
                                    )
                                }
                                composable("providers") {
                                    AiProviderListScreen(onBack = { aiNav.popBackStack() })
                                }
                            }
                        }
                        composable("backup") {
                            BackupScreen(onBack = { navController.popBackStack() })
                        }
                        composable("recycle_bin") {
                            RecycleBinScreen(onBack = { navController.popBackStack() })
                        }
                        composable("recycle_bin_settings") {
                            RecycleBinSettingsScreen(
                                onBack = { navController.popBackStack() },
                                onNavigateToRecycleBin = { navController.navigate("recycle_bin") }
                            )
                        }
                        composable("gesture_lock") {
                            GestureLockSetupScreen(onBack = { navController.popBackStack() })
                        }
                        composable("tag_manage") {
                            TagManageScreen(onBack = { navController.popBackStack() })
                        }
                        composable("crash_logs") {
                            CrashLogScreen(
                                onClose = { navController.popBackStack() },
                                onRestart = {
                                    navController.navigate("main") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("theme_settings") {
                            ThemeSettingsScreen(onBack = { navController.popBackStack() })
                        }
                        composable("chart_manage") {
                            ChartManageScreen(onBack = { navController.popBackStack() })
                        }
                    }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_OPEN_STOP_CONFIRM) {
            stopRequest.update { it + 1 }
            intent.action = null
        }
    }

    companion object {
        const val ACTION_OPEN_STOP_CONFIRM = "me.neko.nzhelper.ACTION_OPEN_STOP_CONFIRM"
    }
}
