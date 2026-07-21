package me.neko.nzhelper.core.datastore

import android.content.Context
import androidx.core.content.edit
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

object AgeGroupSettings {

    const val PREFS_NAME = "age_prefs"
    const val KEY_AGE = "age"
    const val KEY_BIRTH_DATE = "birth_date"
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

    fun getBirthDate(context: Context): LocalDate {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        when (val stored = prefs.all[KEY_BIRTH_DATE]) {
            is String -> runCatching { LocalDate.parse(stored) }
                .getOrNull()?.let { return it }
            is Long -> runCatching {
                Instant.ofEpochMilli(stored)
                    .atZone(ZoneId.of("UTC"))
                    .toLocalDate()
            }.getOrNull()?.let { return it }
            is Int -> runCatching {
                Instant.ofEpochMilli(stored.toLong())
                    .atZone(ZoneId.of("UTC"))
                    .toLocalDate()
            }.getOrNull()?.let { return it }
            else -> Unit
        }
        val legacyAge = (prefs.all[KEY_AGE] as? Int ?: DEFAULT_AGE)
            .coerceIn(MIN_AGE, MAX_AGE)
        return LocalDate.now().minusYears(legacyAge.toLong())
    }

    fun setBirthDate(context: Context, date: LocalDate) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_BIRTH_DATE, date.toString())
                remove(KEY_AGE)
            }
    }

    fun getAge(context: Context): Int {
        val birth = getBirthDate(context)
        val age = Period.between(birth, LocalDate.now()).years
        return age.coerceIn(MIN_AGE, MAX_AGE)
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
