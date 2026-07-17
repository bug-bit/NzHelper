package me.neko.nzhelper.core.database

import android.content.Context
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.neko.nzhelper.NzApplication
import me.neko.nzhelper.core.datastore.RecycleBinSettings
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.RecycleBinItem
import me.neko.nzhelper.core.model.Session

object RecycleRepository {

    private val gson = NzApplication.gson
    private val recycleBinTypeToken = object : TypeToken<List<RecycleBinItem>>() {}.type
    private val sessionsTypeToken = object : TypeToken<List<Session>>() {}.type

    private fun dao(context: Context) = AppDatabase.get(context).recycleBinDao()

    internal fun parseRecycleBinJson(json: String): List<RecycleBinItem> {
        return try {
            gson.fromJson<List<RecycleBinItem>>(json, recycleBinTypeToken) ?: emptyList()
        } catch (_: Exception) {
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

    internal suspend fun saveRecycleBin(context: Context, items: List<RecycleBinItem>) =
        withContext(Dispatchers.IO) {
            val dao = dao(context)
            dao.deleteAll()
            dao.upsertAll(items.map { Mappers.itemToEntity(it, gson) })
        }

    suspend fun loadRecycleBin(context: Context): List<RecycleBinItem> =
        withContext(Dispatchers.IO) {
            dao(context).getAll().map { e ->
                val item = Mappers.entityToItem(e, gson)
                item.copy(session = TagSettings.migrateLegacySession(context, item.session))
            }
        }

    suspend fun moveAllToRecycleBin(context: Context) = withContext(Dispatchers.IO) {
        val currentSessions = SessionRepository.loadSessions(context)
        if (currentSessions.isEmpty()) return@withContext
        val currentRecycleBin = dao(context).getAll().map { Mappers.entityToItem(it, gson) }
        val now = System.currentTimeMillis()
        val newItems = currentSessions.map { RecycleBinItem(session = it, deletedTimestamp = now) }
        val merged = (currentRecycleBin + newItems).distinctBy { Mappers.sessionKey(it.session) }
        val dao = dao(context)
        dao.deleteAll()
        dao.upsertAll(merged.map { Mappers.itemToEntity(it, gson) })
        SessionRepository.saveSessions(context, emptyList())
    }

    suspend fun moveSessionsToRecycleBin(context: Context, sessionsToMove: List<Session>) =
        withContext(Dispatchers.IO) {
            if (sessionsToMove.isEmpty()) return@withContext
            val currentSessions = SessionRepository.loadSessions(context)
            val timestampsToMove = sessionsToMove.map { Mappers.sessionKey(it) }.toSet()
            val remaining = currentSessions.filter { Mappers.sessionKey(it) !in timestampsToMove }

            val currentRecycleBin = dao(context).getAll().map { Mappers.entityToItem(it, gson) }
            val now = System.currentTimeMillis()
            val newItems =
                sessionsToMove.map { RecycleBinItem(session = it, deletedTimestamp = now) }
            val merged = (currentRecycleBin + newItems).distinctBy { Mappers.sessionKey(it.session) }

            val dao = dao(context)
            dao.deleteAll()
            dao.upsertAll(merged.map { Mappers.itemToEntity(it, gson) })
            SessionRepository.saveSessions(context, remaining)
        }

    suspend fun restoreFromRecycleBin(context: Context, itemsToRestore: List<RecycleBinItem>) =
        withContext(Dispatchers.IO) {
            if (itemsToRestore.isEmpty()) return@withContext
            val currentSessions = SessionRepository.loadSessions(context)
            val currentRecycleBin = dao(context).getAll().map { Mappers.entityToItem(it, gson) }

            val sessionsToRestore = itemsToRestore.map { it.session }
            val timestampsToRestore = sessionsToRestore.map { Mappers.sessionKey(it) }.toSet()

            val mergedSessions = (currentSessions + sessionsToRestore)
                .distinctBy { Mappers.sessionKey(it) }
            val remainingRecycleBin =
                currentRecycleBin.filter { Mappers.sessionKey(it.session) !in timestampsToRestore }

            SessionRepository.saveSessions(context, mergedSessions)
            val dao = dao(context)
            dao.deleteAll()
            dao.upsertAll(remainingRecycleBin.map { Mappers.itemToEntity(it, gson) })
        }

    suspend fun deleteFromRecycleBin(context: Context, itemsToDelete: List<RecycleBinItem>) =
        withContext(Dispatchers.IO) {
            if (itemsToDelete.isEmpty()) return@withContext
            val keys = itemsToDelete.map { Mappers.sessionKey(it.session) }
            dao(context).deleteBySessionKeys(keys)
        }

    suspend fun clearRecycleBin(context: Context) = withContext(Dispatchers.IO) {
        dao(context).deleteAll()
    }

    suspend fun cleanExpiredRecycleBinItems(context: Context) = withContext(Dispatchers.IO) {
        if (!RecycleBinSettings.isAutoCleanEnabled(context)) return@withContext
        val retentionMillis = RecycleBinSettings.RETENTION_DAYS * 24 * 60 * 60 * 1000L
        val cutoff = System.currentTimeMillis() - retentionMillis
        dao(context).deleteOlderThan(cutoff)
    }
}
