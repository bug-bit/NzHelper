package me.neko.nzhelper.core.datastore

import android.content.Context
import androidx.core.content.edit

object OnboardingSettings {

    const val PREFS_NAME = "onboarding_prefs"
    const val KEY_COMPLETED = "completed_v1"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isCompleted(context: Context): Boolean =
        prefs(context).getBoolean(KEY_COMPLETED, false)

    fun markCompleted(context: Context) {
        prefs(context).edit { putBoolean(KEY_COMPLETED, true) }
    }
}
