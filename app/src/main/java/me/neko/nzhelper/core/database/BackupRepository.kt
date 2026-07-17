package me.neko.nzhelper.core.database

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.neko.nzhelper.NzApplication
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.WebDavBackupPayload
import me.neko.nzhelper.core.security.BackupCipher
import me.neko.nzhelper.core.webdav.WebDavSettings
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object BackupRepository {

    private const val WEBDAV_BACKUP_FILENAME = "nzHelper_backup.nz"
    private const val CONNECT_TIMEOUT = 15_000L
    private const val READ_TIMEOUT = 30_000L

    private val gson = NzApplication.gson

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
        bytes: ByteArray,
        mediaType: okhttp3.MediaType?
    ): Int {
        val body = bytes.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .put(body)
            .header("Authorization", auth)
            .header("Content-Type", "application/octet-stream")
            .build()

        return newOkHttpClient().newCall(request).execute().use { it.code }
    }

    suspend fun exportNzBytes(context: Context): ByteArray = withContext(Dispatchers.IO) {
        val payload = WebDavBackupPayload(
            version = 3,
            exportedAt = System.currentTimeMillis(),
            sessions = SessionRepository.loadSessions(context),
            recycleBin = RecycleRepository.loadRecycleBin(context),
            categories = TagSettings.getCategories(context),
            tagGroups = TagSettings.getGroups(context),
            tags = TagSettings.getTags(context)
        )
        val json = gson.toJson(payload)
        BackupCipher.encrypt(context, json.toByteArray(Charsets.UTF_8))
    }

    private suspend fun applyPayload(
        context: Context,
        payload: WebDavBackupPayload
    ): Pair<Int, Int> {
        val currentSessions = SessionRepository.loadSessions(context)
        val mergedSessions = (currentSessions + payload.sessions)
            .distinctBy { Mappers.sessionKey(it) }
            .map { TagSettings.migrateLegacySession(context, it) }

        val currentRecycle = RecycleRepository.loadRecycleBin(context)
        val mergedRecycle = (currentRecycle + payload.recycleBin)
            .distinctBy { Mappers.sessionKey(it.session) }
            .map { it.copy(session = TagSettings.migrateLegacySession(context, it.session)) }

        TagSettings.mergeTaxonomy(
            context,
            payload.categories,
            payload.tagGroups,
            payload.tags
        )

        SessionRepository.saveSessions(context, mergedSessions, triggerAutoBackup = false)
        RecycleRepository.saveRecycleBin(context, mergedRecycle)

        return (mergedSessions.size - currentSessions.size) to
                (mergedRecycle.size - currentRecycle.size)
    }

    suspend fun importNzBytes(context: Context, data: ByteArray): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            val plain = BackupCipher.decrypt(context, data)
                ?: return@withContext false to "备份密码不匹配或文件已损坏"
            val text = String(plain, Charsets.UTF_8)
            val payload = try {
                gson.fromJson(text, WebDavBackupPayload::class.java)
            } catch (_: Exception) {
                null
            } ?: return@withContext false to "备份内容格式无效"
            val (addedS, addedR) = applyPayload(context, payload)
            true to "恢复成功：记录 +$addedS，回收站 +$addedR"
        }

    suspend fun importFromUri(context: Context, uri: Uri): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            val bytes = try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } ?: return@withContext false to "无法读取文件"
            if (BackupCipher.isNzFile(bytes)) {
                return@withContext importNzBytes(context, bytes)
            }
            val text = String(bytes, Charsets.UTF_8)
            val payload = try {
                gson.fromJson(text, WebDavBackupPayload::class.java)
            } catch (_: Exception) {
                null
            }
            if (payload != null && payload.version > 0) {
                val (addedS, addedR) = applyPayload(context, payload)
                return@withContext true to "恢复成功：记录 +$addedS，回收站 +$addedR"
            }
            val listType = com.google.gson.reflect.TypeToken
                .getParameterized(
                    List::class.java,
                    me.neko.nzhelper.core.model.Session::class.java
                ).type
            val sessions = try {
                gson.fromJson<List<me.neko.nzhelper.core.model.Session>>(text, listType)
            } catch (_: Exception) {
                null
            } ?: return@withContext false to "无法识别的备份格式"
            val current = SessionRepository.loadSessions(context)
            val merged = (current + sessions).distinctBy { Mappers.sessionKey(it) }
                .map { TagSettings.migrateLegacySession(context, it) }
            SessionRepository.saveSessions(context, merged, triggerAutoBackup = false)
            true to "恢复成功（仅记录）：新增 ${merged.size - current.size} 条"
        }

    suspend fun backupToWebDav(context: Context): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            if (!WebDavSettings.isConfigured(context)) {
                return@withContext false to "未配置 WebDAV 服务器"
            }
            try {
                val data = exportNzBytes(context)
                val mediaType = "application/octet-stream".toMediaTypeOrNull()

                val url = buildFullUrl(context, WEBDAV_BACKUP_FILENAME)
                val auth = buildAuthHeader(context)
                    ?: return@withContext false to "未配置 WebDAV 服务器"

                var respCode = tryPut(url, auth, data, mediaType)

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

                        respCode = tryPut(url, auth, data, mediaType)
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
                    val body = resp.body.bytes()

                    if (!BackupCipher.isNzFile(body)) {
                        val text = String(body, Charsets.UTF_8)
                        val legacyPayload = try {
                            gson.fromJson(text, WebDavBackupPayload::class.java)
                        } catch (_: Exception) {
                            null
                        }
                        if (legacyPayload != null && legacyPayload.version > 0) {
                            val (addedS, addedR) = applyPayload(context, legacyPayload)
                            return@withContext true to "恢复成功：记录 +$addedS，回收站 +$addedR"
                        }
                        val remoteSessions = SessionRepository.parseSessionsJson(context, text)
                        val currentSessions = SessionRepository.loadSessions(context)
                        val merged = (currentSessions + remoteSessions)
                            .distinctBy { Mappers.sessionKey(it) }
                            .map { TagSettings.migrateLegacySession(context, it) }
                        SessionRepository.saveSessions(context, merged)
                        return@withContext true to "恢复成功（仅记录）：新增 ${merged.size - currentSessions.size} 条"
                    }

                    val plain = BackupCipher.decrypt(context, body)
                        ?: return@withContext false to "备份密码不匹配或文件已损坏"
                    val payload = try {
                        gson.fromJson(
                            String(plain, Charsets.UTF_8),
                            WebDavBackupPayload::class.java
                        )
                    } catch (_: Exception) {
                        null
                    } ?: return@withContext false to "备份内容格式无效"

                    val (addedS, addedR) = applyPayload(context, payload)
                    true to "恢复成功：记录 +$addedS，回收站 +$addedR"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false to "恢复失败: ${e.message ?: "未知错误"}"
            }
        }

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

    suspend fun autoBackupIfNeeded(context: Context) {
        if (!WebDavSettings.isAutoBackupEnabled(context)) return
        if (!WebDavSettings.isConfigured(context)) return
        backupToWebDav(context)
    }
}
