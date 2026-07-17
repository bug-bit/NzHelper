package me.neko.nzhelper.core.database

import android.content.Context
import androidx.core.content.edit
import me.neko.nzhelper.NzApplication
import me.neko.nzhelper.core.database.entity.TaxonomyEntity
import me.neko.nzhelper.core.database.entity.WebDavConfigEntity
import me.neko.nzhelper.core.model.RecycleBinItem
import me.neko.nzhelper.core.model.Session
import java.io.File

object LegacyMigrator {

    private const val SESSIONS_PREFS = "sessions_prefs"
    private const val KEY_SESSIONS = "sessions"
    private const val KEY_RECYCLE_BIN = "recycle_bin"
    private const val EXTERNAL_SESSIONS = "nzHelper_data.json"
    private const val EXTERNAL_RECYCLE = "nzHelper_recycle.json"
    private const val DEFAULT_EXTERNAL_PATH = "/storage/emulated/0/NzHelper"

    private const val WEBDAV_PREFS = "webdav_prefs"
    private const val TAG_PREFS = "tag_taxonomy_prefs"

    private const val MIGRATED_KEY = "legacy_prefs_migrated_to_room_v2"

    private val gson = NzApplication.gson

    suspend fun migrateIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean(MIGRATED_KEY, false)) return

        migrateSessions(context)
        migrateWebDav(context)
        migrateTaxonomy(context)

        prefs.edit { putBoolean(MIGRATED_KEY, true) }
    }

    private suspend fun migrateSessions(context: Context) {
        val sessions = readLegacySessions(context)
        val recycleBin = readLegacyRecycleBin(context)

        if (sessions.isNotEmpty()) {
            AppDatabase.get(context).sessionDao()
                .upsertAll(sessions.map { Mappers.sessionToEntity(it, gson) })
        }
        if (recycleBin.isNotEmpty()) {
            AppDatabase.get(context).recycleBinDao()
                .upsertAll(recycleBin.map { Mappers.itemToEntity(it, gson) })
        }

        context.getSharedPreferences(SESSIONS_PREFS, Context.MODE_PRIVATE).edit {
            remove(KEY_SESSIONS)
            remove(KEY_RECYCLE_BIN)
        }
        val path = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
            .getString("external_storage_path", DEFAULT_EXTERNAL_PATH) ?: DEFAULT_EXTERNAL_PATH
        listOf(EXTERNAL_SESSIONS, EXTERNAL_RECYCLE).forEach { name ->
            File(path, name).takeIf { it.exists() }?.delete()
        }
    }

    private suspend fun migrateWebDav(context: Context) {
        val wp = context.getSharedPreferences(WEBDAV_PREFS, Context.MODE_PRIVATE)
        if (wp.all.isEmpty()) return
        val dao = AppDatabase.get(context).webDavConfigDao()
        val existing = dao.get() ?: WebDavConfigEntity()
        val remotePath = wp.getString("remote_path", "/NzHelper") ?: "/NzHelper"
        dao.upsert(
            existing.copy(
                url = wp.getString("url", "") ?: "",
                username = wp.getString("username", "") ?: "",
                password = wp.getString("password", "") ?: "",
                remotePath = remotePath,
                autoBackup = wp.getBoolean("auto_backup", false),
                lastBackupTime = wp.getLong("last_backup_time", 0L)
            )
        )
        wp.edit { clear() }
    }

    private suspend fun migrateTaxonomy(context: Context) {
        val tp = context.getSharedPreferences(TAG_PREFS, Context.MODE_PRIVATE)
        if (tp.all.isEmpty()) return
        val dao = AppDatabase.get(context).taxonomyDao()

        tp.getString("categories", null)?.let {
            dao.upsert(TaxonomyEntity("categories", it))
        }
        tp.getString("groups", null)?.let {
            dao.upsert(TaxonomyEntity("groups", it))
        }
        tp.getString("tags", null)?.let {
            dao.upsert(TaxonomyEntity("tags", it))
        }
        if (tp.getBoolean("defaults_seeded_v1", false)) {
            dao.upsert(TaxonomyEntity("defaults_seeded_v1", "true"))
        }
        tp.edit { clear() }
    }

    private fun readLegacySessions(context: Context): List<Session> {
        val externalMode = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
            .getString("storage_mode", "internal") == "external"
        val json = if (externalMode) {
            val path = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
                .getString("external_storage_path", DEFAULT_EXTERNAL_PATH) ?: DEFAULT_EXTERNAL_PATH
            File(path, EXTERNAL_SESSIONS).takeIf { it.exists() }?.readText()
        } else {
            context.getSharedPreferences(SESSIONS_PREFS, Context.MODE_PRIVATE)
                .getString(KEY_SESSIONS, null)
        }
        if (json.isNullOrBlank()) return emptyList()
        return SessionRepository.parseSessionsJson(context, json)
    }

    private fun readLegacyRecycleBin(context: Context): List<RecycleBinItem> {
        val externalMode = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
            .getString("storage_mode", "internal") == "external"
        val json = if (externalMode) {
            val path = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
                .getString("external_storage_path", DEFAULT_EXTERNAL_PATH) ?: DEFAULT_EXTERNAL_PATH
            File(path, EXTERNAL_RECYCLE).takeIf { it.exists() }?.readText()
        } else {
            context.getSharedPreferences(SESSIONS_PREFS, Context.MODE_PRIVATE)
                .getString(KEY_RECYCLE_BIN, null)
        }
        if (json.isNullOrBlank()) return emptyList()
        return RecycleRepository.parseRecycleBinJson(json)
    }
}
