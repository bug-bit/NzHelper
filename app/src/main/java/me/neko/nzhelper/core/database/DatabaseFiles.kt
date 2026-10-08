package me.neko.nzhelper.core.database

import android.content.Context
import java.io.File

object DatabaseFiles {

    private val SUFFIXES = listOf("", "-wal", "-shm", "-journal")

    private const val ARCHIVE_SUFFIX = ".unreadable-"

    fun main(context: Context): File = context.getDatabasePath(AppDatabase.DB_NAME)

    fun existing(context: Context): List<File> {
        val main = main(context)
        return SUFFIXES.map { File(main.path + it) }.filter { it.exists() }
    }

    fun exists(context: Context): Boolean = existing(context).isNotEmpty()

    fun archive(context: Context, stamp: String): List<String> {
        val archived = mutableListOf<String>()
        for (file in existing(context)) {
            val target = File(file.parentFile, file.name + ARCHIVE_SUFFIX + stamp)
            if (target.exists()) continue
            if (file.renameTo(target)) archived += target.name
        }
        return archived
    }
}
