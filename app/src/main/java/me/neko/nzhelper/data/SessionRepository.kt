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
import me.neko.nzhelper.ui.screens.setting.RecycleBinSettings
import me.neko.nzhelper.ui.screens.setting.StorageSettings
import me.neko.nzhelper.ui.screens.setting.WebDavSettings
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object SessionRepository {

    private const val PREFS_NAME = "sessions_prefs"
    private const val KEY_SESSIONS = "sessions"
    private const val KEY_RECYCLE_BIN = "recycle_bin"
    private const val EXTERNAL_FILENAME = "nzHelper_data.json"
    private const val EXTERNAL_RECYCLE_FILENAME = "nzHelper_recycle.json"
    private const val WEBDAV_BACKUP_FILENAME = "nzHelper_backup.json"
    private const val CONNECT_TIMEOUT = 15_000L
    private const val READ_TIMEOUT = 30_000L

    private val gson = NzApplication.gson
    private val sessionsTypeToken = object : TypeToken<List<Session>>() {}.type
    private val recycleBinTypeToken = object : TypeToken<List<RecycleBinItem>>() {}.type

    private fun readJson(context: Context): String? {
        return if (StorageSettings.getMode(context) == StorageSettings.MODE_EXTERNAL) {
            val dir = File(StorageSettings.getExternalPath(context))
            val file = File(dir, EXTERNAL_FILENAME)
            if (file.exists()) file.readText() else null
        } else {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SESSIONS, null)
        }
    }

    /**
     * 从指定模式/路径读取数据（不受全局设置影响，用于迁移前预读目标位置数据）
     */
    private fun readJsonFromTarget(mode: String, path: String, context: Context): String? {
        return if (mode == StorageSettings.MODE_EXTERNAL) {
            val dir = File(path)
            val file = File(dir, EXTERNAL_FILENAME)
            if (file.exists()) file.readText() else null
        } else {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SESSIONS, null)
        }
    }

    private fun writeJson(context: Context, json: String) {
        if (StorageSettings.getMode(context) == StorageSettings.MODE_EXTERNAL) {
            val dir = File(StorageSettings.getExternalPath(context))
            if (!dir.exists()) dir.mkdirs()
            File(dir, EXTERNAL_FILENAME).writeText(json)
        } else {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit { putString(KEY_SESSIONS, json) }
        }
    }

    private fun parseSessionsJson(context: Context, json: String): List<Session> {
        return try {
            val root = JsonParser.parseString(json)
            if (root.isJsonArray && root.asJsonArray.size() > 0) {
                val firstElem = root.asJsonArray[0]
                if (firstElem.isJsonObject) {
                    val obj = firstElem.asJsonObject
                    if (!obj.has("timestamp")) {
                        val migrated = migrateObfuscatedData(root.asJsonArray)
                        val correctedJson = gson.toJson(migrated)
                        writeJson(context, correctedJson)
                        return migrated
                    }
                }
            }
            gson.fromJson(json, sessionsTypeToken) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun readRecycleBinJson(context: Context): String? {
        return if (StorageSettings.getMode(context) == StorageSettings.MODE_EXTERNAL) {
            val dir = File(StorageSettings.getExternalPath(context))
            val file = File(dir, EXTERNAL_RECYCLE_FILENAME)
            if (file.exists()) file.readText() else null
        } else {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_RECYCLE_BIN, null)
        }
    }

    private fun readRecycleBinJsonFromTarget(
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

    private fun writeRecycleBinJson(context: Context, json: String) {
        if (StorageSettings.getMode(context) == StorageSettings.MODE_EXTERNAL) {
            val dir = File(StorageSettings.getExternalPath(context))
            if (!dir.exists()) dir.mkdirs()
            File(dir, EXTERNAL_RECYCLE_FILENAME).writeText(json)
        } else {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit { putString(KEY_RECYCLE_BIN, json) }
        }
    }

    private fun parseRecycleBinJson(json: String): List<RecycleBinItem> {
        return try {
            val items =
                gson.fromJson<List<RecycleBinItem>>(json, recycleBinTypeToken) ?: emptyList()
            items
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

    suspend fun loadSessions(context: Context): List<Session> = withContext(Dispatchers.IO) {
        val json = readJson(context)
        if (json.isNullOrEmpty()) {
            emptyList()
        } else {
            parseSessionsJson(context, json).sortedByDescending { it.timestamp }
        }
    }

    suspend fun saveSessions(context: Context, sessions: List<Session>) =
        withContext(Dispatchers.IO) {
            val json = gson.toJson(sessions)
            writeJson(context, json)
            autoBackupIfNeeded(context)
        }

    suspend fun loadRecycleBin(context: Context): List<RecycleBinItem> =
        withContext(Dispatchers.IO) {
            val json = readRecycleBinJson(context)
            if (json.isNullOrEmpty()) emptyList() else parseRecycleBinJson(json)
        }

    suspend fun moveAllToRecycleBin(context: Context) = withContext(Dispatchers.IO) {
        val currentJson = readJson(context)
        val currentSessions = if (!currentJson.isNullOrEmpty()) {
            parseSessionsJson(context, currentJson)
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
        writeJson(context, gson.toJson(emptyList<Session>()))
    }

    /**
     * 将指定记录移入回收站（用于单条/多条删除）
     */
    suspend fun moveSessionsToRecycleBin(context: Context, sessionsToMove: List<Session>) =
        withContext(Dispatchers.IO) {
            if (sessionsToMove.isEmpty()) return@withContext

            val currentJson = readJson(context)
            val currentSessions = if (!currentJson.isNullOrEmpty()) {
                parseSessionsJson(context, currentJson)
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
            writeJson(context, gson.toJson(remaining))
        }

    /**
     * 从回收站恢复记录
     */
    suspend fun restoreFromRecycleBin(context: Context, itemsToRestore: List<RecycleBinItem>) =
        withContext(Dispatchers.IO) {
            if (itemsToRestore.isEmpty()) return@withContext

            val currentJson = readJson(context)
            val currentSessions = if (!currentJson.isNullOrEmpty()) {
                parseSessionsJson(context, currentJson)
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

            writeJson(context, gson.toJson(mergedSessions))
            writeRecycleBinJson(context, gson.toJson(remainingRecycleBin))
        }

    /**
     * 从回收站永久删除指定记录
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
     * 清空回收站（永久删除所有回收站记录）
     */
    suspend fun clearRecycleBin(context: Context) = withContext(Dispatchers.IO) {
        writeRecycleBinJson(context, gson.toJson(emptyList<RecycleBinItem>()))
    }

    /**
     * 清理过期的回收站记录
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

    /**
     * 切换存储模式并合并迁移数据
     * @return true 切换成功，false 切换失败（路径不可写等）
     */
    suspend fun switchStorageMode(
        context: Context,
        newMode: String,
        newPath: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentMode = StorageSettings.getMode(context)
            val currentPath = StorageSettings.getExternalPath(context)

            if (currentMode == newMode &&
                (newMode != StorageSettings.MODE_EXTERNAL || currentPath == newPath)
            ) {
                return@withContext true
            }

            if (newMode == StorageSettings.MODE_EXTERNAL) {
                val dir = File(newPath)
                if (!dir.exists() && !dir.mkdirs()) return@withContext false
                val testFile = File(dir, ".nzhelper_write_test")
                try {
                    testFile.writeText("test")
                    testFile.delete()
                } catch (_: Exception) {
                    return@withContext false
                }
            }

            val currentJson = readJson(context) ?: ""
            val currentSessions = if (currentJson.isNotEmpty()) {
                parseSessionsJson(context, currentJson)
            } else {
                emptyList()
            }

            val targetJson = readJsonFromTarget(newMode, newPath, context) ?: ""
            val targetSessions = if (targetJson.isNotEmpty()) {
                parseSessionsJson(context, targetJson)
            } else {
                emptyList()
            }

            val mergedSessions = (currentSessions + targetSessions)
                .distinctBy { it.timestamp }

            // ===== 合并回收站数据 =====
            val currentRecycleBinJson = readRecycleBinJson(context) ?: ""
            val currentRecycleBin = if (currentRecycleBinJson.isNotEmpty()) {
                parseRecycleBinJson(currentRecycleBinJson)
            } else {
                emptyList()
            }

            val targetRecycleBinJson = readRecycleBinJsonFromTarget(newMode, newPath, context) ?: ""
            val targetRecycleBin = if (targetRecycleBinJson.isNotEmpty()) {
                parseRecycleBinJson(targetRecycleBinJson)
            } else {
                emptyList()
            }

            val mergedRecycleBin = (currentRecycleBin + targetRecycleBin)
                .distinctBy { it.session.timestamp }

            StorageSettings.setMode(context, newMode)
            if (newMode == StorageSettings.MODE_EXTERNAL) {
                StorageSettings.setExternalPath(context, newPath)
            }

            saveSessions(context, mergedSessions)
            writeRecycleBinJson(context, gson.toJson(mergedRecycleBin))

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
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
                try {
                    val list: List<Session> = gson.fromJson(jsonStr, listType)
                    result.addAll(list)
                    return@withContext result
                } catch (_: Exception) {
                    try {
                        val root = JsonParser.parseString(jsonStr).asJsonArray
                        for (elem in root) {
                            if (elem.isJsonArray) {
                                val arr = elem.asJsonArray
                                val timeStr = arr[0].asString
                                val timestamp = LocalDateTime.parse(
                                    timeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME
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

    // ===================== WebDAV 备份相关 =====================

    private fun buildAuthHeader(context: Context): String? {
        val user = WebDavSettings.getUsername(context)
        val pass = WebDavSettings.getPassword(context)
        if (user.isBlank()) return null
        val credentials = "$user:$pass"
        return "Basic " + android.util.Base64.encodeToString(
            credentials.toByteArray(),
            android.util.Base64.NO_WRAP
        )
    }

    /**
     * URL 编码 WebDAV 路径的每一段，保留 "/"
     */
    private fun encodeWebDavPath(path: String): String {
        if (path.isBlank()) return ""
        val normalized = if (path.startsWith("/")) path else "/$path"
        return normalized.split("/").joinToString("/") { segment ->
            java.net.URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
        }
    }

    private fun buildFullUrl(context: Context, fileName: String): String {
        val baseUrl = WebDavSettings.getUrl(context).trimEnd('/')
        val remotePath = WebDavSettings.getRemotePath(context)
        val encodedPath = encodeWebDavPath(remotePath)
        return "$baseUrl$encodedPath/$fileName"
    }

    private fun buildParentUrl(context: Context): String {
        val baseUrl = WebDavSettings.getUrl(context).trimEnd('/')
        val remotePath = WebDavSettings.getRemotePath(context)
        val encodedPath = encodeWebDavPath(remotePath)
        return "$baseUrl$encodedPath"
    }

    private fun newOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
            .build()
    }

    private fun ensureRemoteDirectory(context: Context): Boolean {
        val parentUrl = buildParentUrl(context)
        val auth = buildAuthHeader(context) ?: return false

        val mkcolRequest = Request.Builder()
            .url(parentUrl)
            .method("MKCOL", null)
            .header("Authorization", auth)
            .build()

        newOkHttpClient().newCall(mkcolRequest).execute().use { resp ->
            // 201 成功 / 405 已存在 / 301 重定向都视为可继续
            return resp.code == 201 || resp.code == 405 || resp.code == 301
        }
    }

    /**
     * 备份数据到 WebDAV（sessions + recycleBin 合并为一个 JSON 文件）
     * @return Pair(成功?, 消息)
     */
    suspend fun backupToWebDav(context: Context): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            if (!WebDavSettings.isConfigured(context)) {
                return@withContext false to "未配置 WebDAV 服务器"
            }
            try {
                if (!ensureRemoteDirectory(context)) {
                    return@withContext false to "无法创建远程目录"
                }

                val sessions = loadSessions(context)
                val recycleBin = loadRecycleBin(context)

                val backupPayload = WebDavBackupPayload(
                    version = 1,
                    exportedAt = System.currentTimeMillis(),
                    sessions = sessions,
                    recycleBin = recycleBin
                )

                val json = gson.toJson(backupPayload)
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = json.toRequestBody(mediaType)

                val url = buildFullUrl(context, WEBDAV_BACKUP_FILENAME)
                val auth = buildAuthHeader(context)
                    ?: return@withContext false to "未配置 WebDAV 服务器"

                val request = Request.Builder()
                    .url(url)
                    .put(body)
                    .header("Authorization", auth)
                    .header("Content-Type", "application/json")
                    .build()

                newOkHttpClient().newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val currentTime = System.currentTimeMillis()
                        WebDavSettings.setLastBackupTime(context, currentTime)

                        val timeStr = java.text.SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss", Locale.getDefault()
                        ).format(Date(currentTime))
                        true to "备份成功 ($timeStr)"
                    } else {
                        false to "备份失败: HTTP ${resp.code}"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false to "备份失败: ${e.message ?: "未知错误"}"
            }
        }

    /**
     * 从 WebDAV 恢复数据（与本地数据合并去重）
     * @return Pair(成功?, 消息)
     */
    suspend fun restoreFromWebDav(context: Context): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            if (!WebDavSettings.isConfigured(context)) {
                return@withContext false to "未配置 WebDAV 服务器"
            }
            try {
                val url = buildFullUrl(context, WEBDAV_BACKUP_FILENAME)
                val auth = buildAuthHeader(context)
                    ?: return@withContext false to "未配置 WebDAV 服务器"

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("Authorization", auth)
                    .build()

                newOkHttpClient().newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        return@withContext false to "恢复失败: HTTP ${resp.code}"
                    }
                    val body = resp.body.string()

                    val payload = try {
                        gson.fromJson(body, WebDavBackupPayload::class.java)
                    } catch (_: Exception) {
                        null
                    }

                    if (payload == null) {
                        val remoteSessions = parseSessionsJson(context, body)
                        val currentSessions = loadSessions(context)
                        val merged = (currentSessions + remoteSessions)
                            .distinctBy { it.timestamp }
                        saveSessions(context, merged)
                        return@withContext true to "恢复成功（仅记录）：新增 ${merged.size - currentSessions.size} 条"
                    }

                    val currentSessions = loadSessions(context)
                    val mergedSessions = (currentSessions + payload.sessions)
                        .distinctBy { it.timestamp }

                    val currentRecycle = loadRecycleBin(context)
                    val mergedRecycle = (currentRecycle + payload.recycleBin)
                        .distinctBy { it.session.timestamp }

                    saveSessions(context, mergedSessions)
                    writeRecycleBinJson(context, gson.toJson(mergedRecycle))

                    val addedSession = mergedSessions.size - currentSessions.size
                    val addedRecycle = mergedRecycle.size - currentRecycle.size
                    true to "恢复成功：记录 +$addedSession，回收站 +$addedRecycle"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false to "恢复失败: ${e.message ?: "未知错误"}"
            }
        }

    /**
     * 测试 WebDAV 连接
     */
    suspend fun testWebDavConnection(
        url: String,
        username: String,
        password: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = url.trimEnd('/')
            val credentials = "$username:$password"
            val auth = "Basic " + android.util.Base64.encodeToString(
                credentials.toByteArray(), android.util.Base64.NO_WRAP
            )

            val request = Request.Builder()
                .url("$cleanUrl/")
                .method("PROPFIND", "".toRequestBody("application/xml".toMediaTypeOrNull()))
                .header("Authorization", auth)
                .header("Depth", "0")
                .build()

            newOkHttpClient().newCall(request).execute().use { resp ->
                when {
                    resp.isSuccessful || resp.code == 207 -> true to "连接成功"
                    resp.code == 401 -> false to "用户名或密码错误"
                    resp.code == 403 -> false to "无访问权限"
                    else -> false to "连接失败: HTTP ${resp.code}"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false to "连接失败: ${e.message ?: "未知错误"}"
        }
    }

    /**
     * 自动备份（如果开启自动备份，则在保存记录后调用）
     */
    suspend fun autoBackupIfNeeded(context: Context) {
        if (!WebDavSettings.isAutoBackupEnabled(context)) return
        if (!WebDavSettings.isConfigured(context)) return
        backupToWebDav(context)
    }
}