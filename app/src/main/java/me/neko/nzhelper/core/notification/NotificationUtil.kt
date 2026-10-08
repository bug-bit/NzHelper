package me.neko.nzhelper.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import me.neko.nzhelper.R
import me.neko.nzhelper.core.crash.CrashHandler

object NotificationUtil {
    const val CHANNEL_ID = "timer_channel"
    const val CHANNEL_NAME = "计时服务"

    const val CRASH_CHANNEL_ID = "crash_channel"
    const val CRASH_CHANNEL_NAME = "崩溃提醒"

    private const val NOTIFICATION_ID_CRASH = 1001
    private const val REQUEST_CODE_CRASH = 1001

    fun createChannel(context: Context) {
        val chan = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(chan)
    }

    fun createCrashChannel(context: Context) {
        val chan = NotificationChannel(
            CRASH_CHANNEL_ID,
            CRASH_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "应用异常退出后保留崩溃日志的提醒"
            setShowBadge(true)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(chan)
    }

    fun notifyCrash(context: Context, crashFileName: String, summary: String) {
        val intent = Intent().apply {
            setClassName(context, CrashHandler.CRASH_LOG_ACTIVITY)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(CrashHandler.EXTRA_CRASH_FILE_NAME, crashFileName)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_CRASH,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CRASH_CHANNEL_ID)
            .setSmallIcon(R.drawable.history_24px)
            .setContentTitle("NzHelper 上次异常退出")
            .setContentText(summary.take(120))
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_CRASH, notification)
    }
}
