package me.neko.nzhelper.core.database

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.neko.nzhelper.NzApplication
import me.neko.nzhelper.core.datastore.StorageSettings
import me.neko.nzhelper.core.model.WebDavBackupPayload
import me.neko.nzhelper.core.webdav.WebDavSettings
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object BackupRepository {

    private const val WEBDAV_BACKUP_FILENAME = "nzHelper_backup.json"
    private const val CONNECT_TIMEOUT = 15_000L
    private const val READ_TIMEOUT = 30_000L

    private val gson = NzApplication.gson

    // ===================== WebDAV 内部工具 =====================

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
     * URL 编码 WebDAV 路径的每一段，保留 "/"。
     */
    private fun encodeWebDavPath(path: String): String {
        if (path.isBlank()) return ""
        val normalized = if (path.startsWith("/")) path else "/$path"
        return normalized.split("/").filter { it.isNotBlank() }.joinToString("/") { segment ->
            java.net.URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
        }.let { if (it.isBlank()) "" else "/$it" }
    }

    private fun buildFullUrl(context: Context, fileName: String): String {
        val baseUrl = WebDavSettings.getUrl(context).trimEnd('/')
        val remotePath = WebDavSettings.getRemotePath(context).trimEnd('/')
        val encodedPath = encodeWebDavPath(remotePath)
        val finalFileName = if (fileName.startsWith("/")) fileName else "/$fileName"
        return "$baseUrl$encodedPath$finalFileName"
    }

    private fun newOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
            .build()
    }

    /**
     * 支持递归创建多层目录。
     */
    private fun ensureRemoteDirectory(context: Context): Boolean {
        val baseUrl = WebDavSettings.getUrl(context).trimEnd('/')
        val remotePath = WebDavSettings.getRemotePath(context).trim().trimEnd('/')
        if (remotePath.isBlank() || remotePath == "/") return true

        val auth = buildAuthHeader(context) ?: return false
        val client = newOkHttpClient()

        val segments = remotePath.split("/").filter { it.isNotBlank() }
        var currentUrl = baseUrl

        for (segment in segments) {
            val encodedSegment = java.net.URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
            currentUrl = "$currentUrl/$encodedSegment"

            val mkcolRequest = Request.Builder()
                .url(currentUrl)
                .method("MKCOL", null)
                .header("Authorization", auth)
                .build()

            try {
                client.newCall(mkcolRequest).execute().use { resp ->
                    // 201 成功 / 405 已存在 / 301 重定向都视为可继续
                    if (!(resp.code == 201 || resp.code == 405 || resp.code == 301)) {
                        return false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }
        return true
    }

    private fun tryPut(
        url: String,
        auth: String,
        json: String,
        mediaType: okhttp3.MediaType?
    ): Int {
        val body = json.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .put(body)
            .header("Authorization", auth)
            .header("Content-Type", "application/json")
            .build()

        return newOkHttpClient().newCall(request).execute().use { it.code }
    }

    // ===================== 公共 API =====================

    /**
     * 备份数据到 WebDAV（sessions + recycleBin 合并为一个 JSON 文件）。
     * @return Pair(成功?, 消息)
     */
    suspend fun backupToWebDav(context: Context): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            if (!WebDavSettings.isConfigured(context)) {
                return@withContext false to "未配置 WebDAV 服务器"
            }
            try {
                val sessions = SessionRepository.loadSessions(context)
                val recycleBin = RecycleRepository.loadRecycleBin(context)

                val backupPayload = WebDavBackupPayload(
                    version = 1,
                    exportedAt = System.currentTimeMillis(),
                    sessions = sessions,
                    recycleBin = recycleBin
                )

                val json = gson.toJson(backupPayload)
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

                val url = buildFullUrl(context, WEBDAV_BACKUP_FILENAME)
                val auth = buildAuthHeader(context)
                    ?: return@withContext false to "未配置 WebDAV 服务器"

                var respCode = tryPut(url, auth, json, mediaType)

                if (respCode == 409) {
                    if (ensureRemoteDirectory(context)) {
                        val deleteRequest = Request.Builder()
                            .url(url)
                            .delete()
                            .header("Authorization", auth)
                            .build()
                        try {
                            newOkHttpClient().newCall(deleteRequest).execute().close()
                        } catch (_: Exception) {
                        }

                        respCode = tryPut(url, auth, json, mediaType)
                    } else {
                        return@withContext false to "无法创建远程目录"
                    }
                }

                if (respCode in 200..299) {
                    val currentTime = System.currentTimeMillis()
                    WebDavSettings.setLastBackupTime(context, currentTime)

                    val timeStr = java.text.SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss", Locale.getDefault()
                    ).format(Date(currentTime))
                    true to "备份成功 ($timeStr)"
                } else {
                    false to "备份失败: HTTP $respCode"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false to "备份失败: ${e.message ?: "未知错误"}"
            }
        }

    /**
     * 从 WebDAV 恢复数据（与本地数据合并去重）。
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
                        val msg = when (resp.code) {
                            404, 403, 405 -> "恢复失败: 服务器上未找到备份文件 (HTTP ${resp.code})"
                            401 -> "恢复失败: 认证失败 (HTTP 401)"
                            else -> "恢复失败: HTTP ${resp.code}"
                        }
                        return@withContext false to msg
                    }
                    val body = resp.body.string()

                    val payload = try {
                        gson.fromJson(body, WebDavBackupPayload::class.java)
                    } catch (_: Exception) {
                        null
                    }

                    if (payload == null) {
                        // 旧版备份格式：直接是 Session 列表
                        val remoteSessions = SessionRepository.parseSessionsJson(context, body)
                        val currentSessions = SessionRepository.loadSessions(context)
                        val merged = (currentSessions + remoteSessions)
                            .distinctBy { it.timestamp }
                        SessionRepository.saveSessions(context, merged)
                        return@withContext true to "恢复成功（仅记录）：新增 ${merged.size - currentSessions.size} 条"
                    }

                    val currentSessions = SessionRepository.loadSessions(context)
                    val mergedSessions = (currentSessions + payload.sessions)
                        .distinctBy { it.timestamp }

                    val currentRecycle = RecycleRepository.loadRecycleBin(context)
                    val mergedRecycle = (currentRecycle + payload.recycleBin)
                        .distinctBy { it.session.timestamp }

                    SessionRepository.saveSessions(context, mergedSessions)
                    RecycleRepository.saveRecycleBin(context, mergedRecycle)

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
     * 测试 WebDAV 连接（不依赖已保存的配置，使用传入的临时凭据）。
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
     * 自动备份（如果开启自动备份，则在保存记录后调用）。
     */
    suspend fun autoBackupIfNeeded(context: Context) {
        if (!WebDavSettings.isAutoBackupEnabled(context)) return
        if (!WebDavSettings.isConfigured(context)) return
        backupToWebDav(context)
    }

    /**
     * 切换存储模式并合并迁移数据（sessions + recycleBin）。
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

            // ===== 合并 sessions =====
            val currentJson = SessionRepository.readJson(context) ?: ""
            val currentSessions = if (currentJson.isNotEmpty()) {
                SessionRepository.parseSessionsJson(context, currentJson)
            } else {
                emptyList()
            }

            val targetJson = SessionRepository.readJsonFromTarget(newMode, newPath, context) ?: ""
            val targetSessions = if (targetJson.isNotEmpty()) {
                SessionRepository.parseSessionsJson(context, targetJson)
            } else {
                emptyList()
            }

            val mergedSessions = (currentSessions + targetSessions)
                .distinctBy { it.timestamp }

            // ===== 合并回收站数据 =====
            val currentRecycleBinJson = RecycleRepository.readRecycleBinJson(context) ?: ""
            val currentRecycleBin = if (currentRecycleBinJson.isNotEmpty()) {
                RecycleRepository.parseRecycleBinJson(currentRecycleBinJson)
            } else {
                emptyList()
            }

            val targetRecycleBinJson =
                RecycleRepository.readRecycleBinJsonFromTarget(newMode, newPath, context) ?: ""
            val targetRecycleBin = if (targetRecycleBinJson.isNotEmpty()) {
                RecycleRepository.parseRecycleBinJson(targetRecycleBinJson)
            } else {
                emptyList()
            }

            val mergedRecycleBin = (currentRecycleBin + targetRecycleBin)
                .distinctBy { it.session.timestamp }

            StorageSettings.setMode(context, newMode)
            if (newMode == StorageSettings.MODE_EXTERNAL) {
                StorageSettings.setExternalPath(context, newPath)
            }

            SessionRepository.saveSessions(context, mergedSessions)
            RecycleRepository.saveRecycleBin(context, mergedRecycleBin)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
