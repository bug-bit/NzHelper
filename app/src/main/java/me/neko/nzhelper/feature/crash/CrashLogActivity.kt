package me.neko.nzhelper.feature.crash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import me.neko.nzhelper.core.crash.CrashHandler
import me.neko.nzhelper.ui.theme.NzHelperTheme

class CrashLogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialCrashFileName = intent.getStringExtra(CrashHandler.EXTRA_CRASH_FILE_NAME)
        setContent {
            NzHelperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CrashLogScreen(
                        onClose = { finish() },
                        initialCrashFileName = initialCrashFileName
                    )
                }
            }
        }
    }
}
