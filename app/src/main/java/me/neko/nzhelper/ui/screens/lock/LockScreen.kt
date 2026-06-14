package me.neko.nzhelper.ui.screens.lock

import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity

@Composable
fun LockScreen(onUnlock: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    // 禁止返回键
    BackHandler(enabled = true) { /* 拦截返回，不做任何操作 */ }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "应用已锁定",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "验证身份以继续使用",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            FilledTonalButton(
                onClick = {
                    activity?.let {
                        AppLockManager.authenticate(it, onSuccess = onUnlock)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("解锁")
            }
        }
    }

    // 进入锁屏时自动弹出一次验证
    LaunchedEffect(Unit) {
        val canAuth = AppLockManager.canAuthenticate(context)
        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            activity?.let { AppLockManager.authenticate(it, onSuccess = onUnlock) }
        } else {
            // 设备已无可用凭据（如用户删除了锁屏密码），自动关闭应用锁
            AppLockManager.setLockEnabled(context, false)
            onUnlock()
        }
    }
}