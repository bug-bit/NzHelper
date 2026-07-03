package me.neko.nzhelper.ui.screens.lock.util

import android.content.Context
import androidx.core.content.edit
import java.security.MessageDigest

object GestureLockManager {
    private const val PREFS_NAME = "app_lock_prefs"
    private const val KEY_GESTURE_PASSWORD = "gesture_password"

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hasGesturePassword(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_GESTURE_PASSWORD, null) != null
    }

    fun setGesturePassword(context: Context, password: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_GESTURE_PASSWORD, hashPassword(password)) }
    }

    fun clearGesturePassword(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { remove(KEY_GESTURE_PASSWORD) }
    }

    fun verifyPassword(context: Context, password: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_GESTURE_PASSWORD, null) ?: return false
        return saved == hashPassword(password)
    }
}