package me.neko.nzhelper

import android.app.Application
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import me.neko.nzhelper.core.crash.CrashHandler
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.notification.NotificationUtil
import me.neko.nzhelper.core.worker.RecycleBinWorker
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
        CrashHandler.install(this)
        super.onCreate()

        instance = this

        NotificationUtil.createChannel(this)
        RecycleBinWorker.schedulePeriodicCleanup(this)

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

        TagSettings.ensureDefaults(this)
    }
}