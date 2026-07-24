package me.neko.nzhelper

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import me.neko.nzhelper.navigation.MainScreen
import me.neko.nzhelper.ui.theme.NzHelperTheme

class MainActivity : AppCompatActivity() {
    private val stopRequest = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            NzHelperTheme {
                MainScreen(stopRequest = stopRequest)
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
