package me.neko.nzhelper.core.auto

import android.content.Context
import androidx.core.content.edit
import me.neko.nzhelper.core.datastore.TagSettings
import java.time.DayOfWeek
import java.time.LocalDateTime

/**
 * 自动标签规则引擎。
 *
 * 根据 Session 的时间戳，自动推断应该附带哪些「时间分组」标签。
 * 规则只建议 id，真正是否加入由调用方决定（且只建议当前 TagSettings 中存在的标签
 */
object AutoTagRules {

    const val PREF_KEY_ENABLED = "auto_tag_enabled"

    private data class HourRangeRule(
        val startHour: Int,
        val endHour: Int,
        val tagId: String,
        val tagName: String
    )

    private val HOUR_RULES = listOf(
        HourRangeRule(0, 6, "tag_time_dawn", "凌晨"),
        HourRangeRule(6, 12, "tag_time_morning", "上午"),
        HourRangeRule(12, 18, "tag_time_afternoon", "下午"),
        HourRangeRule(18, 22, "tag_time_evening", "晚上"),
        HourRangeRule(22, 24, "tag_time_latenight", "深夜")
    )

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
            .getBoolean(PREF_KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
            .edit { putBoolean(PREF_KEY_ENABLED, enabled) }
    }

    fun suggest(context: Context, timestamp: LocalDateTime): Set<String> {
        if (!isEnabled(context)) return emptySet()

        val result = LinkedHashSet<String>()

        val hour = timestamp.hour
        HOUR_RULES.firstOrNull { hour in it.startHour until it.endHour }
            ?.let { rule ->
                if (TagSettings.getTag(context, rule.tagId) != null) {
                    result += rule.tagId
                }
            }

        val weekendId = "tag_time_weekend"
        val weekdayId = "tag_time_weekday"
        val isWeekend = timestamp.dayOfWeek == DayOfWeek.SATURDAY ||
                timestamp.dayOfWeek == DayOfWeek.SUNDAY
        val dayTagId = if (isWeekend) weekendId else weekdayId
        if (TagSettings.getTag(context, dayTagId) != null) {
            result += dayTagId
        }

        return result
    }

    fun merge(
        currentTagIds: Set<String>,
        suggested: Set<String>
    ): Pair<Set<String>, Set<String>> {
        val toAdd = suggested - currentTagIds
        if (toAdd.isEmpty()) return currentTagIds to emptySet()
        val merged = currentTagIds + toAdd
        return merged to toAdd
    }
}
