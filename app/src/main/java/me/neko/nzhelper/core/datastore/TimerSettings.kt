package me.neko.nzhelper.core.datastore

import android.content.Context
import androidx.core.content.edit

object TimerSettings {
    private const val PREFS_NAME = "settings_prefs"
    private const val KEY_FLOATING_ENABLED = "floating_timer_enabled"

    fun isFloatingEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_FLOATING_ENABLED, false)
    }

    fun setFloatingEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_FLOATING_ENABLED, enabled) }
    }
}
