package me.neko.nzhelper.core.database

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.neko.nzhelper.NzApplication
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.BackupModules
import me.neko.nzhelper.core.security.BackupCipher
import me.neko.nzhelper.core.security.DbKeyProvider
import me.neko.nzhelper.core.webdav.WebDavSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DbBootstrap {

    data class Issue(
        val message: String,
        val detail: String
    )

    private const val TAG = "DbBootstrap"

    private val _issue = MutableStateFlow<Issue?>(null)

    val issue: StateFlow<Issue?> = _issue.asStateFlow()

    fun start(context: Context) {
        val app = context.applicationContext
        val issue = diagnostic(app)
        if (issue != null) {
            report(issue)
            return
        }
        try {
            TagSettings.preload(app)
            WebDavSettings.preload(app)
        } catch (t: Throwable) {
            report(openFailure(t))
            return
        }
        _issue.value = null
        NzApplication.appScope.launch {
            try {
                AppDatabase.get(app)
                LegacyMigrator.migrateIfNeeded(app)
            } catch (t: Throwable) {
                AppDatabase.releaseInstance()
                report(openFailure(t))
            }
        }
    }

    suspend fun retry(context: Context): Boolean = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val issue = diagnostic(app)
        if (issue != null) {
            report(issue)
            return@withContext false
        }
        return@withContext try {
            TagSettings.preload(app)
            WebDavSettings.preload(app)
            AppDatabase.get(app)
            _issue.value = null
            true
        } catch (t: Throwable) {
            report(openFailure(t))
            false
        }
    }

    suspend fun resetLocalData(context: Context): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            val app = context.applicationContext
            try {
                val archived = rebuildCore(app)
                TagSettings.preload(app)
                WebDavSettings.preload(app)
                AppDatabase.get(app)
                _issue.value = null
                true to archivedMessage(archived)
            } catch (t: Throwable) {
                Log.e(TAG, "重建数据库失败", t)
                false to (t.message ?: "重建数据库失败")
            }
        }

    suspend fun restoreFromBackup(
        context: Context,
        preview: BackupRepository.BackupPreview
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        try {
            val archived = rebuildCore(app)
            TagSettings.preload(app)
            WebDavSettings.preload(app)
            AppDatabase.get(app)
            val (ok, message) = BackupRepository.applyPreview(
                app,
                preview,
                BackupModules.ALL
            )
            if (!ok) return@withContext false to message
            TagSettings.preload(app)
            _issue.value = null
            true to "$message（${archivedMessage(archived)}）"
        } catch (t: Throwable) {
            Log.e(TAG, "从备份恢复失败", t)
            false to (t.message ?: "从备份恢复失败")
        }
    }

    private fun rebuildCore(app: Context): List<String> {
        AppDatabase.releaseInstance()
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val archived = DatabaseFiles.archive(app, stamp)
        DbKeyProvider.clear(app)
        BackupCipher.resetPasswordIfUnreadable(app)
        return archived
    }

    private fun archivedMessage(archived: List<String>): String =
        if (archived.isEmpty()) "数据库已重新初始化"
        else "旧数据文件已保留：${archived.joinToString("、")}"

    private fun diagnostic(app: Context): Issue? {
        val keyState = try {
            DbKeyProvider.state(app)
        } catch (t: Throwable) {
            return Issue("无法读取数据库密钥", t.toString())
        }
        return when (keyState) {
            DbKeyProvider.State.Ready -> null

            DbKeyProvider.State.NotInitialized -> {
                if (!DatabaseFiles.exists(app)) {
                    null
                } else {
                    Issue(
                        "本机没有数据库密钥记录，但检测到已有数据库文件",
                        "口令记录缺失（可能是备份只恢复了 databases/ 目录），磁盘上的 ${
                            DatabaseFiles.main(
                                app
                            ).name
                        } 无法打开"
                    )
                }
            }

            is DbKeyProvider.State.Unusable -> {
                if (!DatabaseFiles.exists(app)) {
                    Log.w(TAG, "口令记录不可用但不存在数据库文件，按首次启动处理")
                    DbKeyProvider.clear(app)
                    null
                } else {
                    Issue(
                        "数据库密钥已丢失，历史数据无法解密",
                        DbKeyProvider.description(keyState.failure)
                    )
                }
            }
        }
    }

    private fun openFailure(t: Throwable): Issue = Issue(
        "数据库无法打开",
        t.toString()
    )

    private fun report(issue: Issue) {
        Log.e(TAG, "数据库不可用: ${issue.message} / ${issue.detail}")
        _issue.value = issue
    }
}
