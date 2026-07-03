package me.neko.nzhelper.ui.screens.lock

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import me.neko.nzhelper.ui.screens.lock.components.GestureLockView
import me.neko.nzhelper.ui.screens.lock.util.AppLockManager
import me.neko.nzhelper.ui.screens.lock.util.GestureLockManager

@Composable
fun LockScreen(onUnlock: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    BackHandler(enabled = true) { /* 拦截返回 */ }

    val needBioLock = remember { AppLockManager.isLockEnabled(context) }
    val hasGesture = remember { GestureLockManager.hasGesturePassword(context) }

    var showGestureLock by remember { mutableStateOf(!needBioLock && hasGesture) }
    var gestureError by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    if (!needBioLock && !hasGesture) {
        LaunchedEffect(Unit) {
            AppLockManager.setLockEnabled(context, false)
            onUnlock()
        }
        return
    }

    LaunchedEffect(Unit) {
        if (needBioLock) {
            activity?.let {
                AppLockManager.authenticate(
                    activity = it,
                    onSuccess = onUnlock,
                    onError = {
                        if (hasGesture) showGestureLock = true
                    }
                )
            }
        }
    }

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
                imageVector = if (showGestureLock) Icons.Outlined.Gesture else Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )

            if (showGestureLock) {
                Text(
                    "应用已锁定",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (gestureError) "手势密码错误，请重试" else "绘制手势密码验证身份以继续使用",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (gestureError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))

                GestureLockView(
                    onGestureComplete = { pattern ->
                        if (GestureLockManager.verifyPassword(context, pattern)) {
                            isError = false
                            AppLockManager.setAuthenticated(true)
                            onUnlock()
                        } else {
                            isError = true
                            val vibrator = context.getSystemService(Vibrator::class.java)
                            vibrator?.vibrate(
                                VibrationEffect.createOneShot(
                                    100,
                                    VibrationEffect.DEFAULT_AMPLITUDE
                                )
                            )
                        }
                    },
                    isError = isError,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )

                if (needBioLock) {
                    Spacer(modifier = Modifier.height(24.dp))
                    TextButton(onClick = {
                        showGestureLock = false
                        activity?.let {
                            AppLockManager.authenticate(
                                activity = it,
                                onSuccess = onUnlock,
                                onError = {
                                    if (hasGesture) showGestureLock = true
                                }
                            )
                        }
                    }) {
                        Text("使用生物识别解锁")
                    }
                }
            } else {
                Text(
                    "应用已锁定",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "验证身份以继续使用",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))

                FilledTonalButton(
                    onClick = {
                        if (needBioLock) {
                            activity?.let {
                                AppLockManager.authenticate(
                                    activity = it,
                                    onSuccess = onUnlock,
                                    onError = { if (hasGesture) showGestureLock = true }
                                )
                            }
                        } else {
                            showGestureLock = true
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (needBioLock) Icons.Outlined.Fingerprint else Icons.Outlined.Gesture,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (needBioLock) "解锁" else "手势解锁")
                }

                if (hasGesture) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showGestureLock = true }) {
                        Text("使用手势密码解锁")
                    }
                }
            }
        }
    }
}