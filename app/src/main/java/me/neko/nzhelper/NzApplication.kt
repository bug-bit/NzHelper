package me.neko.nzhelper

import android.app.Application
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import me.neko.nzhelper.ui.util.NotificationUtil
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class NzApplication : Application() {

    companion object {

        lateinit var gson: Gson
            private set

        lateinit var instance: NzApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()

        instance = this

        // 创建通知渠道
        NotificationUtil.createChannel(this)

        // 初始化带 LocalDateTime 支持的 Gson
        gson = GsonBuilder()
            .registerTypeAdapter(
                LocalDateTime::class.java,
                JsonSerializer<LocalDateTime> { src, _, _ ->
                    JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                }
            )
            .registerTypeAdapter(
                LocalDateTime::class.java,
                JsonDeserializer { json, _, _ ->
                    LocalDateTime.parse(
                        json.asJsonPrimitive.asString,
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    )
                }
            )
            .create()
    }
}