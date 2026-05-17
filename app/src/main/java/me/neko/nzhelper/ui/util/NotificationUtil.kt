package me.neko.nzhelper.ui.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationUtil {
    const val CHANNEL_ID = "timer_live_activity_channel"
    const val CHANNEL_NAME = "计时服务"

    fun createChannel(context: Context) {
        val chan = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            setSound(null, null)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(chan)
    }
}
