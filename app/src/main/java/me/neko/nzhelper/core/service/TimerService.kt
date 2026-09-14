package me.neko.nzhelper.core.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.NotificationCompat
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.neko.nzhelper.MainActivity
import me.neko.nzhelper.R
import me.neko.nzhelper.core.datastore.TimerSettings
import me.neko.nzhelper.core.notification.NotificationUtil
import me.neko.nzhelper.core.util.formatTime
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

/**
 * 前台计时服务
 */
class TimerService : Service() {

    private val binder = LocalBinder()

    private val _elapsedSec = MutableStateFlow(0)
    val elapsedSec: StateFlow<Int> = _elapsedSec.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var accumulatedSec: Int = 0
    private var baseTimeMs: Long = 0L

    /** 绑定 Service 生命周期的协程作用域，onDestroy 时统一取消 */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** 当前计时 tick 协程，暂停/停止/重置时取消 */
    private var tickJob: Job? = null

    /** 计时悬浮窗视图，null 表示未显示 */
    private var floatingView: FloatingTimerView? = null

    override fun onBind(intent: Intent): IBinder = binder

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTimer()
            ACTION_PAUSE -> pauseTimer()
            ACTION_STOP -> stopTimer()
            ACTION_RESET -> resetTimer()
        }
        return START_STICKY
    }

    private fun startTimer() {
        if (_isRunning.value) return

        _isRunning.value = true
        baseTimeMs = SystemClock.elapsedRealtime()

        tickJob?.cancel()
        tickJob = serviceScope.launch {
            while (isActive) {
                val currentSec =
                    accumulatedSec + ((SystemClock.elapsedRealtime() - baseTimeMs) / 1000).toInt()
                if (_elapsedSec.value != currentSec) {
                    _elapsedSec.value = currentSec
                    updateFloatingWindow()
                }
                delay((1000L - (SystemClock.elapsedRealtime() % 1000)).milliseconds)
            }
        }

        startForeground(NOTIF_ID, buildNotification(_elapsedSec.value))
        showFloatingWindow()
    }

    private fun pauseTimer() {
        if (!_isRunning.value) return

        _isRunning.value = false
        tickJob?.cancel()

        accumulatedSec = _elapsedSec.value
        baseTimeMs = 0L

        updateNotification(accumulatedSec)
        updateFloatingWindow()
    }

    private fun stopTimer() {
        tickJob?.cancel()
        _isRunning.value = false
        accumulatedSec = 0
        _elapsedSec.value = 0
        baseTimeMs = 0L

        removeFloatingWindow()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun resetTimer() {
        tickJob?.cancel()
        _isRunning.value = false
        accumulatedSec = 0
        _elapsedSec.value = 0
        baseTimeMs = 0L

        removeFloatingWindow()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(elapsed: Int): Notification {
        val isRunning = _isRunning.value

        val builder = NotificationCompat.Builder(this, NotificationUtil.CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_access_alarm_24)
            .setContentIntent(getOpenAppPendingIntent())
            .setOngoing(isRunning)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setShowWhen(true)

        if (isRunning) {
            builder.setContentTitle("计时进行中")
            builder.setUsesChronometer(true)
            builder.setWhen(System.currentTimeMillis() - elapsed * 1000L)
            builder.setChronometerCountDown(false)
        } else {
            builder.setContentTitle("计时已暂停")
            builder.setUsesChronometer(false)
            builder.setContentText(formatTime(elapsed))
            builder.setWhen(System.currentTimeMillis())
        }

        val toggleIcon =
            if (isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val toggleTitle = if (isRunning) "暂停" else "继续"
        val toggleAction = if (isRunning) ACTION_PAUSE else ACTION_START

        builder.addAction(
            NotificationCompat.Action.Builder(
                toggleIcon,
                toggleTitle,
                getPendingIntent(toggleAction)
            ).build()
        )

        return builder.build()
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, TimerService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getOpenAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_OPEN_STOP_CONFIRM
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(
            this,
            MainActivity.ACTION_OPEN_STOP_CONFIRM.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun updateNotification(elapsed: Int) {
        val notif = buildNotification(elapsed)
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIF_ID, notif)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun showFloatingWindow() {
        if (!TimerSettings.isFloatingEnabled(this)) return
        if (!Settings.canDrawOverlays(this)) return
        if (floatingView != null) return
        val wm = getSystemService(WINDOW_SERVICE) as? WindowManager ?: return

        val view = FloatingTimerView(this).apply {
            text = formatTime(_elapsedSec.value)
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            fontFeatureSettings = "tnum"
            setPadding(dp(10), dp(4), dp(10), dp(4))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor("#B3202020".toColorInt())
            }
            onTap = {
                val intent = Intent(this@TimerService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                try {
                    this@TimerService.startActivity(intent)
                } catch (_: Exception) {
                }
            }
        }

        view.measure(
            View.MeasureSpec.makeMeasureSpec(
                0, View.MeasureSpec.UNSPECIFIED
            ),
            View.MeasureSpec.makeMeasureSpec(
                0, View.MeasureSpec.UNSPECIFIED
            )
        )

        (view.background as? GradientDrawable)?.cornerRadius = view.measuredHeight / 2f

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels - view.measuredWidth - dp(16)
            y = dp(96)
        }

        attachFloatingTouch(view, params, wm)

        try {
            wm.addView(view, params)
            floatingView = view
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun attachFloatingTouch(
        view: FloatingTimerView,
        params: WindowManager.LayoutParams,
        wm: WindowManager
    ) {
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        var downRawX = 0f
        var downRawY = 0f
        var downX = 0
        var downY = 0
        var moved = false

        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    downX = params.x
                    downY = params.y
                    moved = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (!moved && (abs(dx) > slop || abs(dy) > slop)) moved = true
                    if (moved) {
                        params.x = downX + dx.toInt()
                        params.y = downY + dy.toInt()
                        try {
                            wm.updateViewLayout(view, params)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        view.performClick()
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun updateFloatingWindow() {
        val view = floatingView ?: return
        val elapsed = _elapsedSec.value
        val running = _isRunning.value
        view.text = formatTime(elapsed)
        view.setTextColor(if (running) Color.WHITE else "#B3FFFFFF".toColorInt())
    }

    private fun removeFloatingWindow() {
        val view = floatingView ?: return
        floatingView = null
        try {
            (getSystemService(WINDOW_SERVICE) as? WindowManager)?.removeView(view)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        tickJob?.cancel()
        serviceScope.cancel()
        removeFloatingWindow()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "me.neko.nzhelper.ACTION_START"
        const val ACTION_PAUSE = "me.neko.nzhelper.ACTION_PAUSE"
        const val ACTION_STOP = "me.neko.nzhelper.ACTION_STOP"
        const val ACTION_RESET = "me.neko.nzhelper.ACTION_RESET"
        const val NOTIF_ID = 1001
    }
}

private class FloatingTimerView(context: Context) : AppCompatTextView(context) {
    var onTap: (() -> Unit)? = null

    override fun performClick(): Boolean {
        super.performClick()
        onTap?.invoke()
        return true
    }
}
