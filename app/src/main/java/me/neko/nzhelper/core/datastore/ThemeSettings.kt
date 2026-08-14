package me.neko.nzhelper.core.datastore

import android.content.Context
import androidx.core.content.edit

object ThemeSettings {

    private const val PREFS = "theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_AMOLED_DARK = "amoled_dark"
    private const val KEY_DYNAMIC_COLOR = "dynamic_color"
    private const val KEY_BACKGROUND_IMAGE = "background_image"
    private const val KEY_BACKGROUND_OPACITY = "background_opacity"
    private const val KEY_BACKGROUND_BLUR = "background_blur"
    private const val KEY_CARD_OPACITY = "card_opacity"
    private const val KEY_DIALOG_OPACITY = "dialog_opacity"

    const val DEFAULT_BACKGROUND_OPACITY = 0.45f
    const val DEFAULT_BACKGROUND_BLUR = 0f
    const val DEFAULT_CARD_OPACITY = 1f
    const val DEFAULT_DIALOG_OPACITY = 1f

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

    fun getBackgroundImagePath(context: Context): String? =
        prefs(context).getString(KEY_BACKGROUND_IMAGE, null)

    fun setBackgroundImagePath(context: Context, path: String?) {
        prefs(context).edit {
            if (path != null) {
                putString(KEY_BACKGROUND_IMAGE, path)
            } else {
                remove(KEY_BACKGROUND_IMAGE)
            }
        }
    }

    fun getBackgroundOpacity(context: Context): Float =
        prefs(context).getFloat(KEY_BACKGROUND_OPACITY, DEFAULT_BACKGROUND_OPACITY)

    fun setBackgroundOpacity(context: Context, opacity: Float) {
        prefs(context).edit { putFloat(KEY_BACKGROUND_OPACITY, opacity) }
    }

    fun getBackgroundBlur(context: Context): Float =
        prefs(context).getFloat(KEY_BACKGROUND_BLUR, DEFAULT_BACKGROUND_BLUR)

    fun setBackgroundBlur(context: Context, blur: Float) {
        prefs(context).edit { putFloat(KEY_BACKGROUND_BLUR, blur) }
    }

    fun getCardOpacity(context: Context): Float =
        prefs(context).getFloat(KEY_CARD_OPACITY, DEFAULT_CARD_OPACITY)

    fun setCardOpacity(context: Context, opacity: Float) {
        prefs(context).edit { putFloat(KEY_CARD_OPACITY, opacity) }
    }

    fun getDialogOpacity(context: Context): Float =
        prefs(context).getFloat(KEY_DIALOG_OPACITY, DEFAULT_DIALOG_OPACITY)

    fun setDialogOpacity(context: Context, opacity: Float) {
        prefs(context).edit { putFloat(KEY_DIALOG_OPACITY, opacity) }
    }
}
