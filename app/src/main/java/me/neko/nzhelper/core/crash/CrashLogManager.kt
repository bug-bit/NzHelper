package me.neko.nzhelper.core.crash

import android.content.Context
import androidx.core.content.edit
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CrashLog(
    val file: File,
    val timestamp: Long,
    val timeLabel: String,
    val sizeBytes: Long,
    val preview: String,
    val summary: String
) {
    val name: String get() = file.name
}

object CrashLogManager {

    private const val PREFS = "crash_prefs"
    private const val KEY_LAST_VIEWED_TS = "last_viewed_crash_ts"

    private fun crashDir(context: Context): File =
        CrashHandler.crashLogDir(context).apply { if (!exists()) mkdirs() }

    fun listCrashLogs(context: Context): List<CrashLog> {
        val dir = crashDir(context)
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".txt") }
            ?: return emptyList()

        val fileParser = SimpleDateFormat(CrashHandler.FILE_DATE_FORMAT, Locale.getDefault())
        val labelFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        return files.mapNotNull { f ->
            val core = f.name.removePrefix("crash_").removeSuffix(".txt")
            val date = runCatching { fileParser.parse(core) }.getOrNull() ?: return@mapNotNull null
            val ts = date.time
            val content = runCatching { f.readText() }.getOrDefault("")
            CrashLog(
                file = f,
                timestamp = ts,
                timeLabel = labelFormat.format(Date(ts)),
                sizeBytes = f.length(),
                preview = content.take(PREVIEW_LENGTH),
                summary = extractSummary(content)
            )
        }.sortedByDescending { it.timestamp }
    }

    private fun extractSummary(content: String): String {
        val causedBy = content.lineSequence().firstOrNull { it.contains("Caused by:") }
        if (causedBy != null) return causedBy.trim()
        val stackIdx = content.indexOf("===== Stack Trace =====")
        if (stackIdx >= 0) {
            return content.substring(stackIdx)
                .lineSequence()
                .drop(1) // 跳过标题行
                .firstOrNull { it.isNotBlank() }
                ?.trim()
                .orEmpty()
        }
        return content.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
    }

    fun readContent(file: File): String =
        runCatching { file.readText() }.getOrDefault("")

    fun delete(file: File): Boolean =
        runCatching { file.delete() }.getOrDefault(false)

    fun clearAll(context: Context): Int {
        val files = crashDir(context).listFiles() ?: return 0
        var count = 0
        for (f in files) {
            if (f.delete()) count++
        }
        return count
    }

    fun getLastViewedTimestamp(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_VIEWED_TS, 0L)

    fun markAllViewed(context: Context) {
        val latest = listCrashLogs(context).firstOrNull()?.timestamp ?: 0L
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putLong(KEY_LAST_VIEWED_TS, latest)
        }
    }

    fun unreadCount(context: Context): Int {
        val lastViewed = getLastViewedTimestamp(context)
        return listCrashLogs(context).count { it.timestamp > lastViewed }
    }

    fun hasUnread(context: Context): Boolean = unreadCount(context) > 0

    private const val PREVIEW_LENGTH = 600
}
