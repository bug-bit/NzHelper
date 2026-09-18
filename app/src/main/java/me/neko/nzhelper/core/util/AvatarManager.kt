package me.neko.nzhelper.core.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import java.io.File

object AvatarManager {

    private const val TAG = "AvatarManager"
    private const val DIR_NAME = "avatars"
    private const val FILE_PREFIX = "avatar_"
    private const val MAX_DIMENSION = 512

    private val lock = Any()
    private var cachedPath: String? = null
    private var cachedBitmap: Bitmap? = null

    fun peekBitmap(path: String?): Bitmap? = synchronized(lock) {
        if (path != null && path == cachedPath) cachedBitmap else null
    }

    fun loadBitmap(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        synchronized(lock) {
            if (path == cachedPath) return cachedBitmap
            val bitmap = if (File(path).exists()) {
                BackgroundImageManager.loadImageBitmap(path, MAX_DIMENSION)
            } else {
                null
            }
            cachedPath = path
            cachedBitmap = bitmap
            return bitmap
        }
    }

    fun saveImage(context: Context, uri: Uri): String? {
        val bitmap = try {
            BackgroundImageManager.decodeSampled(context, uri, MAX_DIMENSION)
        } catch (e: Exception) {
            Log.e(TAG, "解码头像失败", e)
            null
        } ?: return null

        val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
        val file = File(dir, "$FILE_PREFIX${System.currentTimeMillis()}.png")
        return try {
            val success = file.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            if (success) {
                dir.listFiles()
                    ?.filter { it.absolutePath != file.absolutePath }
                    ?.forEach { it.delete() }
                clearCache()
                file.absolutePath
            } else {
                file.delete()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "写入头像失败", e)
            file.delete()
            null
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    fun removeImage(context: Context) {
        File(context.filesDir, DIR_NAME).listFiles()?.forEach { it.delete() }
        clearCache()
    }

    private fun clearCache() = synchronized(lock) {
        cachedPath = null
        cachedBitmap = null
    }
}
