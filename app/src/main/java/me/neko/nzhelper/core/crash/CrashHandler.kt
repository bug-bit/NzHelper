package me.neko.nzhelper.core.crash

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import me.neko.nzhelper.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashHandler private constructor(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread, e: Throwable) {
        val file = runCatching { saveCrashLog(t, e) }.onFailure {
            Log.e(TAG, "保存崩溃日志失败", it)
        }.getOrNull()

        runCatching {
            if (file != null) launchCrashLogActivity(file.name)
            Process.killProcess(Process.myPid())
        }.onFailure {
            Log.e(TAG, "跳转崩溃页失败，回退默认处理器", it)
            defaultHandler?.uncaughtException(t, e)
        }
    }

    private fun saveCrashLog(thread: Thread, throwable: Throwable): File {
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
        @Suppress("DEPRECATION")
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
        return file
    }

    private fun launchCrashLogActivity(crashFileName: String) {
        val intent = Intent().apply {
            setClassName(context, CRASH_LOG_ACTIVITY)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(EXTRA_CRASH_FILE_NAME, crashFileName)
        }
        context.startActivity(intent)
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
        internal const val CRASH_LOG_ACTIVITY = "me.neko.nzhelper.feature.crash.CrashLogActivity"
        const val EXTRA_CRASH_FILE_NAME = "crash_file_name"

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
