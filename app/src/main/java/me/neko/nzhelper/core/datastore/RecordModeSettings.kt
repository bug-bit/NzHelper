package me.neko.nzhelper.core.datastore

import android.content.Context
import androidx.core.content.edit
import me.neko.nzhelper.core.model.SessionMode

object RecordModeSettings {

    const val PREFS_NAME = "record_mode_prefs"
    const val KEY_DEFAULT_MODE = "default_mode"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDefaultMode(context: Context): SessionMode =
        SessionMode.fromKey(prefs(context).getString(KEY_DEFAULT_MODE, null))

    fun setDefaultMode(context: Context, mode: SessionMode) {
        prefs(context).edit { putString(KEY_DEFAULT_MODE, mode.key) }
    }
}
