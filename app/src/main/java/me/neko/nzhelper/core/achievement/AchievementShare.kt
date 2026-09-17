package me.neko.nzhelper.core.achievement

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.neko.nzhelper.core.model.AchievementProgress
import java.io.File

/**
 * 把成就图片写入缓存目录，并返回可对外分享的 content:// URI。
 */
object AchievementShare {

    private const val DIR_NAME = "shared_images"
    private const val FILE_PROVIDER_SUFFIX = ".fileprovider"
    private const val MAX_FILE_AGE_MILLIS = 24 * 60 * 60 * 1000L

    /**
     * 生成成就分享图片；渲染或生成链接失败时返回 null。
     */
    suspend fun createImageUri(
        context: Context,
        progress: AchievementProgress,
        palette: AchievementShareRenderer.Palette
    ): Uri? = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = AchievementShareRenderer.render(progress, palette)
            val dir = File(context.cacheDir, DIR_NAME).apply { mkdirs() }
            pruneExpiredFiles(dir)
            val file = File(dir, "achievement_${progress.achievement.key}.png")
            file.writeBytes(bytes)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}$FILE_PROVIDER_SUFFIX",
                file
            )
        }.getOrNull()
    }

    private fun pruneExpiredFiles(dir: File) {
        val cutoff = System.currentTimeMillis() - MAX_FILE_AGE_MILLIS
        dir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) file.delete()
        }
    }
}
