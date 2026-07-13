package me.neko.nzhelper.core.crash

import android.content.Context
import android.os.Build
import android.util.Log
import me.neko.nzhelper.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全局未捕获异常处理器。
 *
 * 通过 [Thread.setDefaultUncaughtExceptionHandler] 捕获任意线程上抛出的未处理异常，
 * 将崩溃信息写入应用内部存储 `<filesDir>/crash_logs/` 目录下的文本文件中，
 * 之后交由系统默认处理器让进程按预期退出（系统弹出“应用已停止”或直接结束）。
 *
 * 设计要点：
 * 1. 捕获过程本身绝不能再抛出异常，否则可能与默认处理器形成死循环，因此全程用 runCatching 兜底。
 * 2. 保留对系统默认处理器的回调链，保证原生崩溃流程（如写入 tombstone、ANR 统计等）不受影响。
 * 3. 仅捕获 JVM 层未处理异常；native 崩溃与 ANR 不在本处理器范围内。
 *
 */
class CrashHandler private constructor(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread, e: Throwable) {
        // 写日志过程绝不能再抛异常，否则会与默认处理器互相触发。
        runCatching { saveCrashLog(t, e) }.onFailure {
            Log.e(TAG, "保存崩溃日志失败", it)
        }
        // 交回系统默认处理器，使进程按预期退出。
        defaultHandler?.uncaughtException(t, e)
    }

    private fun saveCrashLog(thread: Thread, throwable: Throwable) {
        val now = Date()
        val fileTime = SimpleDateFormat(FILE_DATE_FORMAT, Locale.getDefault()).format(now)
        val displayTime = SimpleDateFormat(DISPLAY_DATE_FORMAT, Locale.getDefault()).format(now)

        val dir = crashLogDir(context)
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "crash_$fileTime.txt")

        val sw = StringWriter()
        val pw = PrintWriter(sw)

        pw.write(buildHeader(displayTime))
        pw.write("\n===== Thread =====\n")
        pw.write("name=${thread.name}\n")
        pw.write("id=${thread.id}\n")
        pw.write("priority=${thread.priority}\n")
        pw.write("state=${thread.state}\n")
        pw.write("daemon=${thread.isDaemon}\n")
        pw.write("\n===== Stack Trace =====\n")
        throwable.printStackTrace(pw)

        var cause: Throwable? = throwable.cause
        var depth = 0
        while (cause != null && depth < MAX_CAUSE_DEPTH) {
            pw.write("\n===== Caused by =====\n")
            cause.printStackTrace(pw)
            if (cause === throwable) break
            cause = cause.cause
            depth++
        }

        pw.flush()
        file.writeText(sw.toString())

        Log.e(TAG, "崩溃日志已写入: ${file.absolutePath}")
    }

    private fun buildHeader(displayTime: String): String = buildString {
        append("===== NzHelper Crash Report =====\n")
        append("Time: $displayTime\n")
        append("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
        append("Build Type: ${BuildConfig.BUILD_TYPE}\n")
        append("Application Id: ${BuildConfig.APPLICATION_ID}\n")
        append("\n----- Device -----\n")
        append("Manufacturer: ${Build.MANUFACTURER}\n")
        append("Brand: ${Build.BRAND}\n")
        append("Model: ${Build.MODEL}\n")
        append("Device: ${Build.DEVICE}\n")
        append("Product: ${Build.PRODUCT}\n")
        append("Hardware: ${Build.HARDWARE}\n")
        append("\n----- OS -----\n")
        append("Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
        runCatching {
            val abis =
                Build.SUPPORTED_ABIS.joinToString(",")
            append("ABIs: $abis\n")
        }
    }

    companion object {
        private const val TAG = "CrashHandler"
        internal const val CRASH_DIR_NAME = "crash_logs"
        internal const val FILE_DATE_FORMAT = "yyyyMMdd_HHmmss_SSS"
        internal const val DISPLAY_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"

        /** cause 链最大展开深度，防止异常对象成环导致死循环。 */
        private const val MAX_CAUSE_DEPTH = 20

        /** 获取崩溃日志目录。 */
        internal fun crashLogDir(context: Context): File =
            File(context.filesDir, CRASH_DIR_NAME)

        fun install(context: Context) {
            val current = Thread.getDefaultUncaughtExceptionHandler()
            if (current is CrashHandler) return
            Thread.setDefaultUncaughtExceptionHandler(
                CrashHandler(context.applicationContext, current)
            )
            Log.d(TAG, "全局崩溃处理器已安装")
        }
    }
}
