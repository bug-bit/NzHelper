package me.neko.nzhelper.core.database

import android.content.Context
import androidx.core.content.edit
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.neko.nzhelper.NzApplication
import me.neko.nzhelper.core.datastore.RecycleBinSettings
import me.neko.nzhelper.core.datastore.StorageSettings
import me.neko.nzhelper.core.model.RecycleBinItem
import me.neko.nzhelper.core.model.Session
import java.io.File

object RecycleRepository {

    private const val PREFS_NAME = "sessions_prefs"
    private const val KEY_RECYCLE_BIN = "recycle_bin"
    private const val EXTERNAL_RECYCLE_FILENAME = "nzHelper_recycle.json"

    private val gson = NzApplication.gson
    private val recycleBinTypeToken = object : TypeToken<List<RecycleBinItem>>() {}.type
    private val sessionsTypeToken = object : TypeToken<List<Session>>() {}.type

    internal fun readRecycleBinJson(context: Context): String? {
        return if (StorageSettings.getMode(context) == StorageSettings.MODE_EXTERNAL) {
            val dir = File(StorageSettings.getExternalPath(context))
            val file = File(dir, EXTERNAL_RECYCLE_FILENAME)
            if (file.exists()) file.readText() else null
        } else {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_RECYCLE_BIN, null)
        }
    }

    internal fun readRecycleBinJsonFromTarget(
        mode: String,
        path: String,
        context: Context
    ): String? {
        return if (mode == StorageSettings.MODE_EXTERNAL) {
            val dir = File(path)
            val file = File(dir, EXTERNAL_RECYCLE_FILENAME)
            if (file.exists()) file.readText() else null
        } else {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_RECYCLE_BIN, null)
        }
    }

    internal fun writeRecycleBinJson(context: Context, json: String) {
        if (StorageSettings.getMode(context) == StorageSettings.MODE_EXTERNAL) {
            val dir = File(StorageSettings.getExternalPath(context))
            if (!dir.exists()) dir.mkdirs()
            File(dir, EXTERNAL_RECYCLE_FILENAME).writeText(json)
        } else {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit { putString(KEY_RECYCLE_BIN, json) }
        }
    }

    internal fun parseRecycleBinJson(json: String): List<RecycleBinItem> {
        return try {
            gson.fromJson<List<RecycleBinItem>>(json, recycleBinTypeToken) ?: emptyList()
        } catch (_: Exception) {
            // 兼容旧版回收站直接存 Session 列表的情况
            try {
                val oldSessions =
                    gson.fromJson<List<Session>>(json, sessionsTypeToken) ?: emptyList()
                oldSessions.map {
                    RecycleBinItem(
                        session = it,
                        deletedTimestamp = System.currentTimeMillis()
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    /**
     * 直接覆盖写入回收站列表
     * 供 BackupRepository 在存储切换 / WebDAV 恢复时使用。
     */
    internal fun saveRecycleBin(context: Context, items: List<RecycleBinItem>) {
        writeRecycleBinJson(context, gson.toJson(items))
    }

    // ===================== 公共 API =====================

    /**
     * 加载回收站全部记录。
     */
    suspend fun loadRecycleBin(context: Context): List<RecycleBinItem> =
        withContext(Dispatchers.IO) {
            val json = readRecycleBinJson(context)
            if (json.isNullOrEmpty()) emptyList() else parseRecycleBinJson(json)
        }

    /**
     * 将当前所有 Session 移入回收站，并清空 sessions 列表。
     */
    suspend fun moveAllToRecycleBin(context: Context) = withContext(Dispatchers.IO) {
        val currentJson = SessionRepository.readJson(context)
        val currentSessions = if (!currentJson.isNullOrEmpty()) {
            SessionRepository.parseSessionsJson(context, currentJson)
        } else {
            emptyList()
        }
        if (currentSessions.isEmpty()) return@withContext

        val recycleBinJson = readRecycleBinJson(context)
        val currentRecycleBin = if (!recycleBinJson.isNullOrEmpty()) {
            parseRecycleBinJson(recycleBinJson)
        } else {
            emptyList()
        }

        val now = System.currentTimeMillis()
        val newItems = currentSessions.map { RecycleBinItem(session = it, deletedTimestamp = now) }

        val newRecycleBin = (currentRecycleBin + newItems).distinctBy { it.session.timestamp }
        writeRecycleBinJson(context, gson.toJson(newRecycleBin))
        SessionRepository.writeJson(context, gson.toJson(emptyList<Session>()))
    }

    /**
     * 将指定记录移入回收站（用于单条/多条删除）。
     */
    suspend fun moveSessionsToRecycleBin(context: Context, sessionsToMove: List<Session>) =
        withContext(Dispatchers.IO) {
            if (sessionsToMove.isEmpty()) return@withContext

            val currentJson = SessionRepository.readJson(context)
            val currentSessions = if (!currentJson.isNullOrEmpty()) {
                SessionRepository.parseSessionsJson(context, currentJson)
            } else {
                emptyList()
            }

            val timestampsToMove = sessionsToMove.map { it.timestamp }.toSet()
            val remaining = currentSessions.filter { it.timestamp !in timestampsToMove }

            val recycleBinJson = readRecycleBinJson(context)
            val currentRecycleBin = if (!recycleBinJson.isNullOrEmpty()) {
                parseRecycleBinJson(recycleBinJson)
            } else {
                emptyList()
            }

            val now = System.currentTimeMillis()
            val newItems =
                sessionsToMove.map { RecycleBinItem(session = it, deletedTimestamp = now) }
            val newRecycleBin = (currentRecycleBin + newItems).distinctBy { it.session.timestamp }

            writeRecycleBinJson(context, gson.toJson(newRecycleBin))
            SessionRepository.writeJson(context, gson.toJson(remaining))
        }

    /**
     * 从回收站恢复记录（写回 sessions 列表，并从回收站移除）。
     */
    suspend fun restoreFromRecycleBin(context: Context, itemsToRestore: List<RecycleBinItem>) =
        withContext(Dispatchers.IO) {
            if (itemsToRestore.isEmpty()) return@withContext

            val currentJson = SessionRepository.readJson(context)
            val currentSessions = if (!currentJson.isNullOrEmpty()) {
                SessionRepository.parseSessionsJson(context, currentJson)
            } else {
                emptyList()
            }

            val recycleBinJson = readRecycleBinJson(context)
            val currentRecycleBin = if (!recycleBinJson.isNullOrEmpty()) {
                parseRecycleBinJson(recycleBinJson)
            } else {
                emptyList()
            }

            val sessionsToRestore = itemsToRestore.map { it.session }
            val timestampsToRestore = sessionsToRestore.map { it.timestamp }.toSet()

            val mergedSessions = (currentSessions + sessionsToRestore).distinctBy { it.timestamp }
            val remainingRecycleBin =
                currentRecycleBin.filter { it.session.timestamp !in timestampsToRestore }

            SessionRepository.writeJson(context, gson.toJson(mergedSessions))
            writeRecycleBinJson(context, gson.toJson(remainingRecycleBin))
        }

    /**
     * 从回收站永久删除指定记录。
     */
    suspend fun deleteFromRecycleBin(context: Context, itemsToDelete: List<RecycleBinItem>) =
        withContext(Dispatchers.IO) {
            if (itemsToDelete.isEmpty()) return@withContext

            val recycleBinJson = readRecycleBinJson(context)
            val currentRecycleBin = if (!recycleBinJson.isNullOrEmpty()) {
                parseRecycleBinJson(recycleBinJson)
            } else {
                emptyList()
            }

            val timestampsToDelete = itemsToDelete.map { it.session.timestamp }.toSet()
            val remaining = currentRecycleBin.filter { it.session.timestamp !in timestampsToDelete }

            writeRecycleBinJson(context, gson.toJson(remaining))
        }

    /**
     * 清空回收站（永久删除所有回收站记录）。
     */
    suspend fun clearRecycleBin(context: Context) = withContext(Dispatchers.IO) {
        writeRecycleBinJson(context, gson.toJson(emptyList<RecycleBinItem>()))
    }

    /**
     * 清理过期的回收站记录（按 [RecycleBinSettings.RETENTION_DAYS] 与自动清理开关）。
     */
    suspend fun cleanExpiredRecycleBinItems(context: Context) = withContext(Dispatchers.IO) {

        if (!RecycleBinSettings.isAutoCleanEnabled(context)) return@withContext

        val retentionDays = RecycleBinSettings.RETENTION_DAYS
        val retentionMillis = retentionDays * 24 * 60 * 60 * 1000L
        val currentTime = System.currentTimeMillis()

        val recycleBinJson = readRecycleBinJson(context)
        val currentRecycleBin = if (!recycleBinJson.isNullOrEmpty()) {
            parseRecycleBinJson(recycleBinJson)
        } else {
            emptyList()
        }

        val remaining = currentRecycleBin.filter { item ->
            (currentTime - item.deletedTimestamp) < retentionMillis
        }

        if (remaining.size != currentRecycleBin.size) {
            writeRecycleBinJson(context, gson.toJson(remaining))
        }
    }
}
