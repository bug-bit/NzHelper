package me.neko.nzhelper.core.datastore

import android.content.Context
import androidx.core.content.edit

object AvatarSettings {

    private const val PREFS = "avatar_prefs"
    private const val KEY_AVATAR_PATH = "avatar_path"

    fun getAvatarPath(context: Context): String? =
        prefs(context).getString(KEY_AVATAR_PATH, null)?.takeIf { it.isNotBlank() }

    fun setAvatarPath(context: Context, path: String?) {
        prefs(context).edit {
            if (path == null) remove(KEY_AVATAR_PATH) else putString(KEY_AVATAR_PATH, path)
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
