package me.neko.nzhelper.core.webdav

import android.content.Context
import kotlinx.coroutines.runBlocking
import me.neko.nzhelper.core.database.AppDatabase
import me.neko.nzhelper.core.database.entity.WebDavConfigEntity

object WebDavSettings {

    private const val DEFAULT_REMOTE_PATH = "/NzHelper"

    @Volatile
    private var cache: WebDavConfigEntity? = null

    private fun dao(context: Context) = AppDatabase.get(context).webDavConfigDao()

    private fun current(context: Context): WebDavConfigEntity {
        cache?.let { return it }
        return runBlocking {
            val c = dao(context).get() ?: WebDavConfigEntity()
            cache = c
            c
        }
    }

    private fun persist(context: Context, config: WebDavConfigEntity) {
        cache = config
        runBlocking { dao(context).upsert(config) }
    }

    fun getUrl(context: Context): String = current(context).url

    fun getUsername(context: Context): String = current(context).username

    fun getPassword(context: Context): String = current(context).password

    fun getRemotePath(context: Context): String =
        current(context).remotePath.ifBlank { DEFAULT_REMOTE_PATH }

    fun isAutoBackupEnabled(context: Context): Boolean = current(context).autoBackup

    fun isConfigured(context: Context): Boolean =
        getUrl(context).isNotBlank() && getUsername(context).isNotBlank()

    fun getLastBackupTime(context: Context): Long = current(context).lastBackupTime

    fun setLastBackupTime(context: Context, time: Long) {
        persist(context, current(context).copy(lastBackupTime = time))
    }

    fun save(
        context: Context,
        url: String,
        username: String,
        password: String,
        remotePath: String
    ) {
        val normalized = remotePath.trim().trimEnd('/').ifBlank { DEFAULT_REMOTE_PATH }
        persist(
            context,
            current(context).copy(
                url = url.trimEnd('/'),
                username = username,
                password = password,
                remotePath = normalized
            )
        )
    }

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        persist(context, current(context).copy(autoBackup = enabled))
    }

    fun clear(context: Context) {
        persist(context, WebDavConfigEntity())
    }
}