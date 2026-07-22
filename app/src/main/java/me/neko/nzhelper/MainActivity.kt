package me.neko.nzhelper

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import me.neko.nzhelper.navigation.MainScreen
import me.neko.nzhelper.ui.theme.NzHelperTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NzHelperTheme {
                MainScreen()
            }
        }
    }
}