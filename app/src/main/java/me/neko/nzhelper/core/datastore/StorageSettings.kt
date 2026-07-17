package me.neko.nzhelper.core.datastore

import android.content.Context
import androidx.core.content.edit

object RecycleBinSettings {
    private const val PREFS_NAME = "settings_prefs"
    private const val KEY_AUTO_CLEAN_ENABLED = "recycle_bin_auto_clean_enabled"

    const val RETENTION_DAYS = 30

    fun isAutoCleanEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_CLEAN_ENABLED, false)
    }

    fun setAutoCleanEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_AUTO_CLEAN_ENABLED, enabled) }
    }
}