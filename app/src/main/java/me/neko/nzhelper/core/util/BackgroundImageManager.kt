package me.neko.nzhelper.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import java.io.File

object BackgroundImageManager {

    private const val TAG = "BackgroundImageManager"
    private const val DIR_NAME = "backgrounds"
    private const val FILE_PREFIX = "background_"
    private const val MAX_DIMENSION = 2560

    private fun targetFile(context: Context): File {
        val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
        return File(dir, "${FILE_PREFIX}${System.currentTimeMillis()}.jpg")
    }

    fun saveImage(context: Context, uri: Uri): String? {
        val bitmap = try {
            decodeSampled(context, uri, MAX_DIMENSION)
        } catch (e: Exception) {
            Log.e(TAG, "解码背景图片失败", e)
            null
        } ?: return null

        val file = targetFile(context)
        return try {
            val success = file.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
            if (success) {
                file.parentFile?.listFiles()
                    ?.filter { it.absolutePath != file.absolutePath }
                    ?.forEach { it.delete() }
                file.absolutePath
            } else {
                file.delete()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "写入背景图片失败", e)
            null
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    fun removeImage(context: Context) {
        File(context.filesDir, DIR_NAME).listFiles()?.forEach { it.delete() }
    }

    fun loadImageBitmap(path: String, maxDimension: Int = 0): Bitmap? {
        if (!File(path).exists()) return null
        return try {
            if (maxDimension > 0) {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                var sampleSize = 1
                while (bounds.outWidth / sampleSize > maxDimension ||
                    bounds.outHeight / sampleSize > maxDimension
                ) {
                    sampleSize *= 2
                }
                BitmapFactory.decodeFile(
                    path,
                    BitmapFactory.Options().apply { inSampleSize = sampleSize }
                )
            } else {
                BitmapFactory.decodeFile(path)
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载背景图片失败", e)
            null
        }
    }

    private fun decodeSampled(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxDimension ||
            bounds.outHeight / sampleSize > maxDimension
        ) {
            sampleSize *= 2
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.setTargetSampleSize(sampleSize)
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(
                    input,
                    null,
                    BitmapFactory.Options().apply { inSampleSize = sampleSize }
                )
            }
        }
    }
}
