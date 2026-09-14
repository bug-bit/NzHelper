package me.neko.nzhelper.core.datastore

import android.content.Context
import androidx.core.content.edit

object UpdateSettings {
    private const val PREFS_NAME = "settings_prefs"
    private const val KEY_CHECK_UPDATE_ENABLED = "check_update_enabled"
    private const val KEY_BETA_UPDATE_ENABLED = "beta_update_enabled"

    fun isCheckUpdateEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_CHECK_UPDATE_ENABLED, true)
    }

    fun setCheckUpdateEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_CHECK_UPDATE_ENABLED, enabled) }
    }

    fun isBetaUpdateEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_BETA_UPDATE_ENABLED, false)
    }

    fun setBetaUpdateEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_BETA_UPDATE_ENABLED, enabled) }
    }
}
