package me.neko.nzhelper.data

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.neko.nzhelper.NzApplication
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object SessionRepository {

    private const val PREFS_NAME = "sessions_prefs"
    private const val KEY_SESSIONS = "sessions"

    private val gson = NzApplication.gson
    private val sessionsTypeToken = object : TypeToken<List<Session>>() {}.type

    suspend fun loadSessions(context: Context): List<Session> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SESSIONS, null)
        if (json.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                val root = JsonParser.parseString(json)
                if (root.isJsonArray && root.asJsonArray.size() > 0) {
                    val firstElem = root.asJsonArray[0]
                    if (firstElem.isJsonObject) {
                        val obj = firstElem.asJsonObject
                        // 只要缺少 "timestamp" 字段，就认为是被混淆或损坏的旧数据
                        if (!obj.has("timestamp")) {
                            val migrated = migrateObfuscatedData(root.asJsonArray)
                            // 修复后立即回写正确格式
                            val correctedJson = gson.toJson(migrated)
                            prefs.edit { putString(KEY_SESSIONS, correctedJson) }
                            return@withContext migrated
                        }
                    }
                }
                gson.fromJson(json, sessionsTypeToken) ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * 将 R8 混淆后的数据映射回正确结构
     */
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

    suspend fun saveSessions(context: Context, sessions: List<Session>) =
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = gson.toJson(sessions)
            prefs.edit { putString(KEY_SESSIONS, json) }
        }

    /**
     * 从指定 Uri 解析导入的 JSON 文件
     */
    suspend fun parseImportFile(
        context: Context,
        uri: Uri,
        gson: Gson,
        listType: java.lang.reflect.Type
    ): List<Session> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Session>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val jsonStr = inputStream.bufferedReader().readText()
                // 尝试解析新格式
                try {
                    val list: List<Session> = gson.fromJson(jsonStr, listType)
                    result.addAll(list)
                    return@withContext result
                } catch (_: Exception) {
                    // 新格式失败，尝试旧格式解析
                    try {
                        val root = JsonParser.parseString(jsonStr).asJsonArray
                        for (elem in root) {
                            if (elem.isJsonArray) {
                                val arr = elem.asJsonArray
                                val timeStr = arr[0].asString
                                val timestamp = LocalDateTime.parse(
                                    timeStr,
                                    DateTimeFormatter.ISO_LOCAL_DATE_TIME
                                )
                                val duration = if (arr.size() > 1) arr[1].asInt else 0
                                val remark =
                                    if (arr.size() > 2 && !arr[2].isJsonNull) arr[2].asString else ""
                                val location =
                                    if (arr.size() > 3 && !arr[3].isJsonNull) arr[3].asString else ""
                                val watchedMovie = if (arr.size() > 4) arr[4].asBoolean else false
                                val climax = if (arr.size() > 5) arr[5].asBoolean else false
                                val rating =
                                    if (arr.size() > 6 && !arr[6].isJsonNull) arr[6].asFloat.coerceIn(
                                        0f,
                                        5f
                                    ) else 3f
                                val mood =
                                    if (arr.size() > 7 && !arr[7].isJsonNull) arr[7].asString else "平静"
                                val props =
                                    if (arr.size() > 8 && !arr[8].isJsonNull) arr[8].asString else "手"
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
                            }
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

}