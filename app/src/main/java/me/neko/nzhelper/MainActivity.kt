package me.neko.nzhelper

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import me.neko.nzhelper.core.crash.CrashLogManager
import me.neko.nzhelper.feature.crash.CrashLogActivity
import me.neko.nzhelper.navigation.MainScreen
import me.neko.nzhelper.ui.component.dialog.CustomAppAlertDialog
import me.neko.nzhelper.ui.theme.NzHelperTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NzHelperTheme {
                // 启动时检测是否存在未读崩溃日志，若有则提示用户查看。
                var showCrashPrompt by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    showCrashPrompt = CrashLogManager.hasUnread(this@MainActivity)
                }

                MainScreen()

                if (showCrashPrompt) {
                    CustomAppAlertDialog(
                        onDismissRequest = { showCrashPrompt = false },
                        iconVector = Icons.Default.BugReport,
                        title = "应用上次意外退出",
                        message = "检测到新的崩溃记录，是否查看崩溃日志以便排查问题？",
                        confirmText = "查看日志",
                        confirmIcon = Icons.Default.BugReport,
                        dismissText = "稍后再说",
                        onConfirm = {
                            startActivity(
                                Intent(this@MainActivity, CrashLogActivity::class.java)
                            )
                        }
                    )
                }
            }
        }
    }
}