package me.neko.nzhelper.ui.screens.lock.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity

object AppLockManager {

    private const val PREFS_NAME = "app_lock_prefs"
    private const val KEY_LOCK_ENABLED = "lock_enabled"

    /** 本次会话是否已通过验证（进程重启后自动重置） */
    @Volatile
    var isAuthenticated = false
        private set

    fun isLockEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_LOCK_ENABLED, false)
    }

    fun setLockEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_LOCK_ENABLED, enabled) }
    }

    fun resetAuthentication() {
        isAuthenticated = false
    }

    /** 返回 BiometricManager 的 canAuthenticate 结果 */
    fun canAuthenticate(context: Context): Int {
        return BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
    }

    /** 发起生物识别 / 锁屏密码验证 */
    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (() -> Unit)? = null
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    isAuthenticated = true
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    // 验证失败，用户可重试，不关闭页面
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // 用户主动取消不视为错误
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        onError?.invoke()
                    }
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("应用锁验证")
            .setSubtitle("请验证身份以继续")
            // 同时允许生物识别和锁屏密码（注意：设置 DEVICE_CREDENTIAL 时不能调用 setNegativeButtonText）
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    /** 暴露设置认证状态的方法，供手势解锁调用 */
    fun setAuthenticated(authenticated: Boolean) {
        isAuthenticated = authenticated
    }
}