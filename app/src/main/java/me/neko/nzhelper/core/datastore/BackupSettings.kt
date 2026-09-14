package me.neko.nzhelper.core.datastore

import android.content.Context
import androidx.core.content.edit

object BackupSettings {
    private const val PREFS_NAME = "backup_prefs"
    private const val KEY_AUTO_KEEP_COUNT = "auto_keep_count"
    private const val KEY_MANUAL_KEEP_COUNT = "manual_keep_count"

    const val MIN_KEEP = 1
    const val MAX_KEEP = 30
    const val DEFAULT_AUTO_KEEP = 5
    const val DEFAULT_MANUAL_KEEP = 5

    fun getAutoKeepCount(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_AUTO_KEEP_COUNT, DEFAULT_AUTO_KEEP)
            .coerceIn(MIN_KEEP, MAX_KEEP)
    }

    fun setAutoKeepCount(context: Context, count: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putInt(KEY_AUTO_KEEP_COUNT, count.coerceIn(MIN_KEEP, MAX_KEEP)) }
    }

    fun getManualKeepCount(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_MANUAL_KEEP_COUNT, DEFAULT_MANUAL_KEEP)
            .coerceIn(MIN_KEEP, MAX_KEEP)
    }

    fun setManualKeepCount(context: Context, count: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putInt(KEY_MANUAL_KEEP_COUNT, count.coerceIn(MIN_KEEP, MAX_KEEP)) }
    }
}
