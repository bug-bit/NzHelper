package me.neko.nzhelper.ui.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Binder
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.app.Notification
import android.annotation.SuppressLint
import android.content.pm.ServiceInfo
import android.os.Build

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import me.neko.nzhelper.MainActivity

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.neko.nzhelper.R
import me.neko.nzhelper.ui.util.NotificationUtil
import me.neko.nzhelper.ui.util.OplusLiveAlertUtil

/**
 * 前台计时服务
 */
class TimerService : Service() {
    private val binder = LocalBinder()
    private val _elapsedSec = MutableStateFlow(0)
    val elapsedSec: StateFlow<Int> = _elapsedSec.asStateFlow()
    private val _isRunning = MutableStateFlow(false)
    val isRunningState: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var startElapsedRealtimeMs: Long = 0L
    private var accumulatedSec: Int = 0
    private var isRunning: Boolean = false

    private val handler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            publishElapsed()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    @SuppressLint("ForegroundServiceType")
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
        if (isRunning) return
        startElapsedRealtimeMs = SystemClock.elapsedRealtime()
        isRunning = true
        _isRunning.value = true
        startUiTicker()
        val notif = buildNotification()
        // 以 dataSync 类型运行前台服务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    /** 暂停计时（仍可保留通知） */
    private fun pauseTimer() {
        if (!isRunning) return
        accumulatedSec = currentElapsedSec()
        handler.removeCallbacks(tickRunnable)
        startElapsedRealtimeMs = 0L
        isRunning = false
        _isRunning.value = false
        publishElapsed()
        updateNotification()
    }

    /** 停止并重置计时 */
    @Suppress("DEPRECATION")
    private fun stopTimer() {
        handler.removeCallbacks(tickRunnable)
        // 重置状态
        accumulatedSec = 0
        startElapsedRealtimeMs = 0L
        isRunning = false
        _isRunning.value = false
        _elapsedSec.value = 0
        // 取消前台状态并移除通知
        stopForeground(true)
        stopSelf()
    }

    /** 构建通知 */
    private fun buildNotification(): Notification {
        val elapsed = currentElapsedSec()
        val contentText = formatTime(elapsed)
        val stateText = if (isRunning) "计时进行中" else "计时已暂停"
        val chronometerBase = System.currentTimeMillis() - elapsed * 1000L

        return NotificationCompat.Builder(this, NotificationUtil.CHANNEL_ID)
            .setContentTitle(stateText)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.baseline_access_alarm_24)
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setWhen(chronometerBase)
            .setUsesChronometer(isRunning)
            .setChronometerCountDown(false)
            .setShortCriticalText(contentText)
            .setRequestPromotedOngoing(isRunning)
            .addAction(controlAction())
            .addAction(stopAction())
            .build()
            .apply {
                extras.putBoolean("android.requestPromotedOngoing", isRunning)
                extras.putString("oplusLiveAlertAppConfig", OplusLiveAlertUtil.appConfig(this@TimerService))
                extras.putString(
                    "oplus.livealert.capsule",
                    OplusLiveAlertUtil.capsuleData(chronometerBase, isRunning, contentText)
                )
                extras.putString(
                    "oplus.livealert.card",
                    OplusLiveAlertUtil.cardData(
                        chronometerBase,
                        isRunning,
                        contentText,
                        stateText
                    )
                )
                extras.putString("op_fluid_serviceId", FLUID_SERVICE_ID)
                extras.putParcelable(OplusLiveAlertUtil.ACTION_ENTER_APP, openAppIntent())
                extras.putParcelable(OplusLiveAlertUtil.ACTION_PAUSE, servicePendingIntent(ACTION_PAUSE, 3))
                extras.putParcelable(OplusLiveAlertUtil.ACTION_RESUME, servicePendingIntent(ACTION_START, 4))
                extras.putParcelable(OplusLiveAlertUtil.ACTION_STOP, servicePendingIntent(ACTION_STOP, 5))
            }
    }

    private fun updateNotification() {
        val notif = buildNotification()
        NotificationManagerCompat.from(this).notify(NOTIF_ID, notif)
    }

    private fun startUiTicker() {
        handler.removeCallbacks(tickRunnable)
        publishElapsed()
        handler.postDelayed(tickRunnable, 1000)
    }

    private fun publishElapsed() {
        _elapsedSec.value = currentElapsedSec()
    }

    private fun currentElapsedSec(): Int {
        if (!isRunning || startElapsedRealtimeMs == 0L) return accumulatedSec
        val runningSec = ((SystemClock.elapsedRealtime() - startElapsedRealtimeMs) / 1000).toInt()
        return accumulatedSec + runningSec
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun controlAction(): NotificationCompat.Action {
        val action = if (isRunning) ACTION_PAUSE else ACTION_START
        val title = if (isRunning) "暂停" else "继续"
        return NotificationCompat.Action.Builder(
            if (isRunning) R.drawable.timer_24px else R.drawable.baseline_access_alarm_24,
            title,
            servicePendingIntent(action, 1)
        ).build()
    }

    private fun stopAction(): NotificationCompat.Action =
        NotificationCompat.Action.Builder(
            R.drawable.timer_24px,
            "结束",
            servicePendingIntent(ACTION_STOP, 2)
        ).build()

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, TimerService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getForegroundService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onDestroy() {
        handler.removeCallbacks(tickRunnable)
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "me.neko.nzhelper.ACTION_START"
        const val ACTION_PAUSE = "me.neko.nzhelper.ACTION_PAUSE"
        const val ACTION_STOP  = "me.neko.nzhelper.ACTION_STOP"
        const val NOTIF_ID = 1001
        const val FLUID_SERVICE_ID = "nzhelper_timer"
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
