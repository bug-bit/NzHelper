package me.neko.nzhelper.core.datastore

import android.content.Context
import androidx.core.content.edit

object AgeGroupSettings {

    const val PREFS_NAME = "age_prefs"
    const val KEY_AGE = "age"
    const val DEFAULT_AGE = 22
    const val MIN_AGE = 18
    const val MAX_AGE = 99

    enum class AgeGroup(
        val label: String,
        val range: String,
        val moderateMax: Int,
        val highMax: Int,
        val dailyLimit: Int
    ) {
        AGE_18_25("18-25 岁", "18-25", 4, 7, 3),
        AGE_26_30("26-30 岁", "26-30", 3, 6, 3),
        AGE_31_40("31-40 岁", "31-40", 2, 4, 3),
        AGE_41_50("41-50 岁", "41-50", 2, 3, 2),
        AGE_51_60("51-60 岁", "51-60", 1, 2, 2),
        AGE_61_99("61 岁以上", "61-99", 1, 2, 2)
    }

    fun getAge(context: Context): Int {
        val age = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_AGE, DEFAULT_AGE)
        return age.coerceIn(MIN_AGE, MAX_AGE)
    }

    fun setAge(context: Context, age: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putInt(KEY_AGE, age.coerceIn(MIN_AGE, MAX_AGE)) }
    }

    fun ageToGroup(age: Int): AgeGroup {
        val a = age.coerceIn(MIN_AGE, MAX_AGE)
        return when (a) {
            in 18..25 -> AgeGroup.AGE_18_25
            in 26..30 -> AgeGroup.AGE_26_30
            in 31..40 -> AgeGroup.AGE_31_40
            in 41..50 -> AgeGroup.AGE_41_50
            in 51..60 -> AgeGroup.AGE_51_60
            else -> AgeGroup.AGE_61_99
        }
    }

    fun getAgeGroup(context: Context): AgeGroup = ageToGroup(getAge(context))
}
