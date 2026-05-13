package me.neko.nzhelper.ui.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.neko.nzhelper.R
import me.neko.nzhelper.ui.util.NotificationUtil

/**
 * 前台计时服务
 */
class TimerService : Service() {
    private val binder = LocalBinder()
    private val _elapsedSec = MutableStateFlow(0)
    val elapsedSec: StateFlow<Int> = _elapsedSec.asStateFlow()
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var startTimeMs: Long = 0L
    private var accumulatedSec: Int = 0

    private val handler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            val nowMs = System.currentTimeMillis()
            _elapsedSec.value = accumulatedSec + ((nowMs - startTimeMs) / 1000).toInt()
            updateNotification(_elapsedSec.value)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTimer()
            ACTION_PAUSE -> pauseTimer()
            ACTION_STOP  -> stopTimer()
        }
        return START_STICKY
    }

    /** 启动计时并进入前台 */
    private fun startTimer() {
        if (_isRunning.value) return
        if (startTimeMs == 0L) {
            startTimeMs = System.currentTimeMillis()
        }
        _isRunning.value = true
        handler.removeCallbacks(tickRunnable)
        handler.post(tickRunnable)
        val notif = buildNotification(_elapsedSec.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIF_ID, notif)
        }
    }

    /** 暂停计时（仍可保留通知） */
    private fun pauseTimer() {
        if (!_isRunning.value) return
        handler.removeCallbacks(tickRunnable)
        accumulatedSec = _elapsedSec.value
        startTimeMs = 0L
        _isRunning.value = false
    }

    /** 停止并重置计时 */
    @Suppress("DEPRECATION")
    private fun stopTimer() {
        handler.removeCallbacks(tickRunnable)
        // 重置状态
        accumulatedSec = 0
        startTimeMs = 0L
        _elapsedSec.value = 0
        _isRunning.value = false
        // 取消前台状态并移除通知
        stopForeground(true)
        stopSelf()
    }

    /** 构建通知 */
    private fun buildNotification(elapsed: Int): Notification {
        val contentText = formatTime(elapsed)
        return NotificationCompat.Builder(this, NotificationUtil.CHANNEL_ID)
            .setContentTitle("计时进行中")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.baseline_access_alarm_24)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(elapsed: Int) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notif = buildNotification(elapsed)
        NotificationManagerCompat.from(this).notify(NOTIF_ID, notif)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tickRunnable)
        _isRunning.value = false
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "me.neko.nzhelper.ACTION_START"
        const val ACTION_PAUSE = "me.neko.nzhelper.ACTION_PAUSE"
        const val ACTION_STOP  = "me.neko.nzhelper.ACTION_STOP"
        const val NOTIF_ID = 1001
    }

    @SuppressLint("DefaultLocale")
    private fun formatTime(totalSeconds: Int): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return buildString {
            if (h > 0) append(String.format("%02d:", h))
            append(String.format("%02d:%02d", m, s))
        }
    }
}
