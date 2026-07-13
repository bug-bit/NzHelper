package me.neko.nzhelper.feature.lock

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.feature.lock.components.GestureLockView

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GestureLockSetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var stage by remember { mutableIntStateOf(1) }
    var firstPattern by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("设置手势密码") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Gesture,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when {
                    errorMsg != null -> errorMsg!!
                    stage == 1 -> "请绘制手势密码（至少连接4个点）"
                    else -> "请再次绘制以确认"
                },
                color = if (errorMsg != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(48.dp))

            GestureLockView(
                onGestureComplete = { pattern ->
                    if (pattern.split("-").size < 4) {
                        isError = true
                        errorMsg = "至少连接4个点，请重试"
                        return@GestureLockView
                    }

                    if (stage == 1) {
                        firstPattern = pattern
                        stage = 2
                        errorMsg = null
                        isError = false
                    } else {
                        if (pattern == firstPattern) {
                            GestureLockManager.setGesturePassword(context, pattern)
                            Toast.makeText(context, "手势密码设置成功", Toast.LENGTH_SHORT).show()
                            onBack()
                        } else {
                            isError = true
                            errorMsg = "两次绘制不一致，请重新设置"
                            firstPattern = ""
                            stage = 1
                        }
                    }
                },
                isError = isError,
                modifier = Modifier.fillMaxWidth(1f)
            )

            Spacer(modifier = Modifier.weight(1f))

            if (GestureLockManager.hasGesturePassword(context)) {
                TextButton(
                    onClick = {
                        GestureLockManager.clearGesturePassword(context)
                        Toast.makeText(context, "已清除手势密码", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("清除手势密码")
                }
            }
        }
    }
}