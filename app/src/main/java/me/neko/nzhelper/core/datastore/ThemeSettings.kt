package me.neko.nzhelper.core.datastore

import android.content.Context
import androidx.core.content.edit

object ThemeSettings {

    private const val PREFS = "theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_AMOLED_DARK = "amoled_dark"
    private const val KEY_DYNAMIC_COLOR = "dynamic_color"

    enum class ThemeMode(val label: String) {
        SYSTEM("跟随系统"),
        LIGHT("浅色模式"),
        DARK("深色模式");

        companion object {
            fun fromName(name: String?): ThemeMode =
                entries.firstOrNull { it.name == name } ?: SYSTEM
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getThemeMode(context: Context): ThemeMode =
        ThemeMode.fromName(prefs(context).getString(KEY_THEME_MODE, null))

    fun setThemeMode(context: Context, mode: ThemeMode) {
        prefs(context).edit { putString(KEY_THEME_MODE, mode.name) }
    }

    fun isAmoledDark(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AMOLED_DARK, false)

    fun setAmoledDark(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_AMOLED_DARK, enabled) }
    }

    fun isDynamicColor(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DYNAMIC_COLOR, true)

    fun setDynamicColor(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_DYNAMIC_COLOR, enabled) }
    }
}
