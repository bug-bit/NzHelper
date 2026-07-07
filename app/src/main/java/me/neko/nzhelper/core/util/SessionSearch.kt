package me.neko.nzhelper.core.util

import android.content.Context
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.Session
import java.time.format.DateTimeFormatter
import java.util.Locale

object SessionSearch {

    /** 日期格式：覆盖「12月」「12-25」「2024」「周五」「星期五」等常见子串。 */
    private val DATE_FORMATS = listOf(
        "yyyy年MM月dd日",
        "yyyy-MM-dd",
        "yyyy/MM/dd",
        "MM月dd日",
        "MM-dd",
        "MM/dd",
        "yyyy",
        "MM月",
        "EEEE", // 星期五
        "E" // 周五
    )

    private val CN_LOCALE = Locale.CHINA

    fun filter(
        context: Context,
        sessions: List<Session>,
        query: String
    ): List<Session> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return sessions

        val keywords = trimmed.split(Regex("\\s+"))
            .map { it.lowercase(Locale.getDefault()) }
            .filter { it.isNotEmpty() }
        if (keywords.isEmpty()) return sessions

        return sessions.filter { session ->
            keywords.all { kw -> matchKeyword(context, session, kw) }
        }
    }

    private fun matchKeyword(context: Context, session: Session, kw: String): Boolean {
        // 备注
        if (session.remark.lowercase(Locale.getDefault()).contains(kw)) return true

        // 标签名
        val tagNames = session.tagIds.mapNotNull { TagSettings.getTag(context, it)?.name }
        if (tagNames.any { it.lowercase(Locale.getDefault()).contains(kw) }) return true

        // 分类名
        val categoryName = TagSettings.getCategory(context, session.categoryId)?.name ?: ""
        if (categoryName.lowercase(Locale.getDefault()).contains(kw)) return true

        // 日期文本
        if (matchDate(session, kw)) return true

        // 时长文本
        if (matchDuration(session.duration, kw)) return true

        // 高潮标记
        when (kw) {
            "高潮", "有高潮", "climax" -> if (session.climax) return true
            "无高潮", "没高潮", "未高潮" -> if (!session.climax) return true
        }

        return false
    }

    private fun matchDate(session: Session, kw: String): Boolean {
        for (pattern in DATE_FORMATS) {
            val formatted = try {
                session.timestamp.format(DateTimeFormatter.ofPattern(pattern, CN_LOCALE))
            } catch (_: Exception) {
                continue
            }
            if (formatted.lowercase(Locale.getDefault()).contains(kw)) return true
        }
        return false
    }

    private fun matchDuration(totalSeconds: Int, kw: String): Boolean {
        val candidates = buildList {
            add("${totalSeconds}秒")
            val totalMin = totalSeconds / 60
            add("${totalMin}分钟")
            add("${totalMin}分")
            val h = totalSeconds / 3600
            val m = (totalSeconds % 3600) / 60
            if (h > 0) {
                add("${h}小时")
                add("${h}时")
                if (m > 0) {
                    add("${h}小时${m}分")
                    add("${h}时${m}分")
                }
            }
        }
        return candidates.any { it.lowercase(Locale.getDefault()).contains(kw) }
    }
}
