package me.neko.nzhelper.core.ai

import android.content.Context
import me.neko.nzhelper.core.datastore.AgeGroupSettings
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.Session

object AiPromptBuilder {

    private const val SYSTEM_PROMPT = "你是健康生活顾问，用户记录的是手淫数据。你的建议需简短、自然、不评判。"

    suspend fun build(
        context: Context,
        sessions: List<Session>,
        rangeDays: Int
    ): Pair<String, String> {
        val opts = AiSettings.getDataOptions(context)
        return SYSTEM_PROMPT to buildUserPrompt(context, sessions, opts, rangeDays)
    }

    private suspend fun buildUserPrompt(
        context: Context,
        sessions: List<Session>,
        opts: AiSettings.DataOptions,
        rangeDays: Int
    ): String {
        val parts = mutableListOf<String>()

        if (opts.isEnabled(AiSettings.DataField.COUNT.key)) {
            parts += "共${sessions.size}次"
        }
        if (opts.isEnabled(AiSettings.DataField.DAYS.key)) {
            val days = sessions.map { it.timestamp.toLocalDate() }.distinct().size
            parts += "分${days}天"
        }
        if (opts.isEnabled(AiSettings.DataField.TIME_PERIOD.key)) {
            val lateNight = sessions.count { it.timestamp.hour >= 23 }
            val morning = sessions.count { it.timestamp.hour in 6..11 }
            val afternoon = sessions.count { it.timestamp.hour in 12..17 }
            val evening = sessions.count { it.timestamp.hour in 18..22 }
            val periods = mutableListOf<String>()
            if (morning > 0) periods += "上午${morning}次"
            if (afternoon > 0) periods += "下午${afternoon}次"
            if (evening > 0) periods += "晚上${evening}次"
            if (lateNight > 0) periods += "深夜${lateNight}次"
            if (periods.isNotEmpty()) parts += periods.joinToString("，")
        }
        if (opts.isEnabled(AiSettings.DataField.MAX_GAP.key) && sessions.size >= 2) {
            val sorted = sessions.sortedBy { it.timestamp }
            var maxGap = 0
            for (i in 1 until sorted.size) {
                val gap = sorted[i].timestamp.toLocalDate().toEpochDay() -
                        sorted[i - 1].timestamp.toLocalDate().toEpochDay()
                if (gap > maxGap) maxGap = gap.toInt()
            }
            if (maxGap >= 2) parts += "最长间隔${maxGap}天"
        }
        if (opts.isEnabled(AiSettings.DataField.AVG_DURATION.key)) {
            val avgSec = if (sessions.isNotEmpty()) sessions.map { it.duration }.average().toInt() else 0
            if (avgSec > 0) parts += "平均时长${avgSec / 60}分钟"
        }
        if (opts.isEnabled(AiSettings.DataField.RATING.key)) {
            val withRating = sessions.filter { it.rating > 0 }
            if (withRating.isNotEmpty()) {
                val avg = withRating.map { it.rating }.average()
                parts += "平均评分${"%.1f".format(avg)}"
            }
        }
        if (opts.isEnabled(AiSettings.DataField.CLIMAX.key)) {
            val climaxCount = sessions.count { it.climax }
            parts += "高潮${climaxCount}次"
        }
        if (opts.isEnabled(AiSettings.DataField.TAGS.key)) {
            val allTags = TagSettings.getTags(context)
            val tagCounts = sessions.flatMap { it.tagIds }
                .groupingBy { it }.eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(5)
            if (tagCounts.isNotEmpty()) {
                val tagLines = tagCounts.joinToString("，") { (id, c) ->
                    val name = allTags.firstOrNull { it.id == id }?.name ?: id
                    "${name}${c}次"
                }
                parts += "常用标签：$tagLines"
            }
        }
        if (opts.isEnabled(AiSettings.DataField.AGE.key)) {
            val age = AgeGroupSettings.getAge(context)
            if (age > 0) parts += "年龄${age}岁"
        }

        val rangeLabel = when {
            rangeDays <= 0 -> "全部记录"
            rangeDays >= 365 -> "最近1年"
            else -> "最近${rangeDays}天"
        }
        val dataSection = if (parts.isNotEmpty()) {
            "${rangeLabel}${parts.joinToString("，")}。"
        } else {
            "${rangeLabel}有记录。"
        }

        val tone = when (AiSettings.getPromptTone(context)) {
            "warm" -> "语气温暖亲切，像朋友聊天一样自然"
            "caring" -> "语气温柔体贴，多给情感支持和理解"
            "encouraging" -> "语气积极阳光，多鼓励、多肯定、提气"
            "professional" -> "语气专业理性，从健康角度分析利弊"
            "humorous" -> "语气轻松有趣，可以带点幽默和调侃"
            "concise" -> "极其简洁，像发短信一样一两句话说完"
            else -> "语气自然平和"
        }
        val fieldCount = parts.size
        val len = when (AiSettings.getPromptLength(context)) {
            "unlimited" -> null
            "short" -> "请控制在${(20 + fieldCount * 5).coerceAtMost(60)}字以内"
            "detailed" -> "可以展开写${(60 + fieldCount * 15).coerceAtMost(200)}字左右的详细建议"
            else -> "请控制在${(40 + fieldCount * 8).coerceAtMost(120)}字以内"
        }
        val extra = AiSettings.getPromptCustom(context).trim()
            .takeIf { it.isNotBlank() }?.let { "。额外要求：$it" } ?: ""
        val lenPart = if (len != null) "。$len" else ""
        val toneText = "。${tone}${lenPart}${extra}。只输出建议不要推理。"
        return "这是手淫记录：${dataSection}${toneText}"
    }
}
