package me.neko.nzhelper.core.database

import android.content.Context
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.neko.nzhelper.NzApplication
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.Session
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object SessionRepository {

    private val gson = NzApplication.gson
    private val sessionsTypeToken = object : TypeToken<List<Session>>() {}.type

    private fun dao(context: Context) = AppDatabase.get(context).sessionDao()

    internal fun parseSessionsJson(context: Context, json: String): List<Session> {
        return try {
            val root = JsonParser.parseString(json)
            val raw: List<Session> = if (root.isJsonArray && root.asJsonArray.size() > 0) {
                val firstElem = root.asJsonArray[0]
                if (firstElem.isJsonObject && !firstElem.asJsonObject.has("timestamp")) {
                    migrateObfuscatedData(root.asJsonArray)
                } else {
                    gson.fromJson(json, sessionsTypeToken) ?: emptyList()
                }
            } else {
                gson.fromJson(json, sessionsTypeToken) ?: emptyList()
            }
            TagSettings.ensureDefaults(context)
            raw.map { TagSettings.migrateLegacySession(context, it) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun loadSessions(context: Context): List<Session> = withContext(Dispatchers.IO) {
        TagSettings.ensureDefaults(context)
        val entities = dao(context).getAll()
        entities.map { Mappers.entityToSession(it, gson) }
    }

    suspend fun saveSessions(context: Context, sessions: List<Session>) =
        saveSessions(context, sessions, triggerAutoBackup = true)

    suspend fun saveSessions(
        context: Context,
        sessions: List<Session>,
        triggerAutoBackup: Boolean
    ) = withContext(Dispatchers.IO) {
        val dao = dao(context)
        val incomingKeys = sessions.map { Mappers.sessionKey(it) }.toSet()
        val existingKeys = dao.getAll().map { it.timestampIso }.toSet()
        val toDelete = (existingKeys - incomingKeys)
        if (toDelete.isNotEmpty()) dao.deleteByKeys(toDelete.toList())
        dao.upsertAll(sessions.map { Mappers.sessionToEntity(it, gson) })
        if (triggerAutoBackup) {
            BackupRepository.autoBackupIfNeeded(context)
        }
    }

    private fun migrateObfuscatedData(array: com.google.gson.JsonArray): List<Session> {
        val result = mutableListOf<Session>()
        for (elem in array) {
            if (!elem.isJsonObject) continue
            val obj = elem.asJsonObject
            try {
                val aStr = obj.get("a")?.asString
                if (aStr.isNullOrEmpty()) continue
                val timestamp = LocalDateTime.parse(aStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)

                val duration = try {
                    obj.get("b")?.asInt ?: 0
                } catch (_: Exception) {
                    0
                }
                val remark = try {
                    if (obj.has("c") && !obj.get("c").isJsonNull) obj.get("c").asString else ""
                } catch (_: Exception) {
                    ""
                }
                val location = try {
                    if (obj.has("d") && !obj.get("d").isJsonNull) obj.get("d").asString else ""
                } catch (_: Exception) {
                    ""
                }
                val watchedMovie = try {
                    obj.get("e")?.asBoolean ?: false
                } catch (_: Exception) {
                    false
                }
                val climax = try {
                    obj.get("f")?.asBoolean ?: false
                } catch (_: Exception) {
                    false
                }
                val rating = try {
                    (obj.get("g")?.asFloat ?: 3f).coerceIn(0f, 5f)
                } catch (_: Exception) {
                    3f
                }
                val mood = try {
                    if (obj.has("h") && !obj.get("h").isJsonNull) obj.get("h").asString else "平静"
                } catch (_: Exception) {
                    "平静"
                }
                val props = try {
                    if (obj.has("i") && !obj.get("i").isJsonNull) obj.get("i").asString else "手"
                } catch (_: Exception) {
                    "手"
                }

                result.add(
                    Session(
                        timestamp = timestamp,
                        duration = duration,
                        remark = remark,
                        location = location,
                        watchedMovie = watchedMovie,
                        climax = climax,
                        rating = rating,
                        mood = mood,
                        props = props
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return result
    }
}