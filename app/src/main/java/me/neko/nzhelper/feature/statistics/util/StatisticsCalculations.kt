package me.neko.nzhelper.feature.statistics.util

import android.annotation.SuppressLint
import android.content.Context
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.core.model.SessionMode
import me.neko.nzhelper.core.model.sessionMode
import me.neko.nzhelper.feature.statistics.model.ActivityTimeData
import me.neko.nzhelper.feature.statistics.model.HeatmapData
import me.neko.nzhelper.feature.statistics.model.HeatmapDay
import me.neko.nzhelper.feature.statistics.model.HeatmapWeek
import me.neko.nzhelper.feature.statistics.model.LatestSessionInfo
import me.neko.nzhelper.feature.statistics.model.MonthlyTrendData
import me.neko.nzhelper.feature.statistics.model.MonthlyTrendItem
import me.neko.nzhelper.feature.statistics.model.PeriodDashboard
import me.neko.nzhelper.feature.statistics.model.PeriodData
import me.neko.nzhelper.feature.statistics.model.PeriodOverview
import me.neko.nzhelper.feature.statistics.model.PeriodType
import me.neko.nzhelper.feature.statistics.model.TagStat
import me.neko.nzhelper.feature.statistics.model.TagTrendData
import me.neko.nzhelper.feature.statistics.model.TagTrendDirection
import me.neko.nzhelper.feature.statistics.model.TagTrendItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// ── 热力图数据 ──
fun calculateHeatmapData(
    sessions: List<Session>,
    now: LocalDateTime
): HeatmapData {
    val today = now.toLocalDate()
    val weeksToShow = 14
    val dayOfWeek = today.dayOfWeek.value
    val mondayThisWeek = today.minusDays((dayOfWeek - 1).toLong())
    val startMonday = mondayThisWeek.minusWeeks((weeksToShow - 1).toLong())

    val sessionMap = sessions
        .groupBy { it.timestamp.toLocalDate() }
        .mapValues { e -> e.value.size to e.value.sumOf { it.duration } }

    val weeks = mutableListOf<HeatmapWeek>()
    val monthLabels = mutableListOf<Pair<Int, String>>()
    var lastMonth = -1
    var activeDays = 0
    var totalDays = 0

    for (weekIndex in 0 until weeksToShow) {
        val weekStart = startMonday.plusWeeks(weekIndex.toLong())

        if (weekStart.monthValue != lastMonth) {
            monthLabels.add(weekIndex to "${weekStart.monthValue}月")
            lastMonth = weekStart.monthValue
        }

        val days = (0 until 7).map { dayOffset ->
            val date = weekStart.plusDays(dayOffset.toLong())
            val isFuture = date > today
            val (count, duration) = sessionMap[date] ?: (0 to 0)
            if (!isFuture) {
                totalDays++
                if (count > 0) activeDays++
            }
            HeatmapDay(date, count, duration, isFuture)
        }
        weeks.add(HeatmapWeek(days))
    }

    val maxCount = sessionMap.values.maxOfOrNull { it.first } ?: 0

    return HeatmapData(
        weeks = weeks,
        monthLabels = monthLabels,
        weekCount = weeksToShow,
        maxCount = maxCount,
        activeDays = activeDays,
        totalDays = totalDays
    )
}

// ── 趋势图数据 ──
fun calculateTrendData(
    sessions: List<Session>,
    now: LocalDateTime
): List<Pair<String, Float>> {
    val today = now.toLocalDate()
    val dayOfWeek = today.dayOfWeek.value
    val mondayThisWeek = today.minusDays((dayOfWeek - 1).toLong())
    val weeksToShow = 12
    val result = mutableListOf<Pair<String, Float>>()

    for (i in weeksToShow - 1 downTo 0) {
        val weekStart = mondayThisWeek.minusWeeks(i.toLong())
        val weekEnd = weekStart.plusDays(6)
        val totalMinutes = sessions
            .filter { it.timestamp.toLocalDate() in weekStart..weekEnd }
            .sumOf { it.duration } / 60f
        val label = if (i == 0) "本周" else "${weekStart.monthValue}/${weekStart.dayOfMonth}"
        result.add(label to totalMinutes)
    }
    return result
}

fun calculatePeriodData(
    sessions: List<Session>,
    now: LocalDateTime,
    type: PeriodType
): PeriodData {
    val filteredSessions = sessions.filter { isWithinPeriod(it.timestamp, now, type) }

    if (filteredSessions.isEmpty()) return PeriodData(0, 0f, emptyList())

    val totalSeconds = filteredSessions.sumOf { it.duration }
    val avgMinutes = totalSeconds.toFloat() / (60 * filteredSessions.size)

    val chartData = when (type) {
        PeriodType.WEEK -> calculateWeeklyChartData(filteredSessions, now)
        PeriodType.MONTH -> calculateMonthlyChartData(filteredSessions, now)
        PeriodType.YEAR -> calculateYearlyChartData(filteredSessions, now)
    }

    return PeriodData(totalSeconds, avgMinutes, chartData)
}

fun isWithinPeriod(
    timestamp: LocalDateTime,
    now: LocalDateTime,
    type: PeriodType
): Boolean {
    val start = when (type) {
        PeriodType.WEEK -> now.minusDays(now.dayOfWeek.value.toLong() - 1).toLocalDate()
            .atStartOfDay()

        PeriodType.MONTH -> now.withDayOfMonth(1).toLocalDate().atStartOfDay()
        PeriodType.YEAR -> now.withDayOfYear(1).toLocalDate().atStartOfDay()
    }
    return timestamp >= start
}

private fun calculateWeeklyChartData(
    sessions: List<Session>,
    now: LocalDateTime
): List<Pair<String, Float>> {
    val monday = now.minusDays(now.dayOfWeek.value.toLong() - 1).toLocalDate()
    val weekDays = (0..6).map { monday.plusDays(it.toLong()) }

    val dailyMap = sessions
        .groupBy { it.timestamp.toLocalDate() }
        .mapValues { it.value.sumOf { s -> s.duration } / 60f }

    return weekDays.map { date ->
        val label = when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> "一"
            DayOfWeek.TUESDAY -> "二"
            DayOfWeek.WEDNESDAY -> "三"
            DayOfWeek.THURSDAY -> "四"
            DayOfWeek.FRIDAY -> "五"
            DayOfWeek.SATURDAY -> "六"
            DayOfWeek.SUNDAY -> "日"
            else -> ""
        }
        label to (dailyMap[date] ?: 0f)
    }
}

private fun calculateMonthlyChartData(
    sessions: List<Session>,
    now: LocalDateTime
): List<Pair<String, Float>> {
    val firstDay = now.withDayOfMonth(1).toLocalDate()

    return sessions
        .filter { it.timestamp.toLocalDate() >= firstDay }
        .groupBy { it.timestamp.toLocalDate() }
        .mapValues { it.value.sumOf { s -> s.duration } / 60f }
        .filter { it.value > 0f }
        .entries
        .sortedBy { it.key }
        .map { entry ->
            entry.key.format(DateTimeFormatter.ofPattern("dd")) to entry.value
        }
}

private fun calculateYearlyChartData(
    sessions: List<Session>,
    now: LocalDateTime
): List<Pair<String, Float>> {
    return sessions
        .filter { it.timestamp.year == now.year }
        .groupBy { YearMonth.from(it.timestamp) }
        .mapValues { it.value.sumOf { s -> s.duration } / 60f }
        .filter { it.value > 0f }
        .entries
        .sortedBy { it.key }
        .map { entry ->
            "${entry.key.monthValue}月" to entry.value
        }
}

fun calculateLatestInfo(sessions: List<Session>): LatestSessionInfo? {
    if (sessions.isEmpty()) return null

    val latest = sessions.maxByOrNull { it.timestamp }!!
    val lastDate = latest.timestamp.toLocalDate()
    val today = LocalDateTime.now().toLocalDate()
    val daysAgo = ChronoUnit.DAYS.between(lastDate, today)

    val displayDate = when (daysAgo) {
        0L -> "今天"
        1L -> "昨天"
        else -> lastDate.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA))
    }

    val time = buildString {
        append(formatPeriod(latest.timestamp))
        append(" · ")
        append(latest.timestamp.format(DateTimeFormatter.ofPattern("HH:mm")))
    }
    val durationText = formatDuration(latest.duration)

    val isErrorState = if (daysAgo <= 1) {
        val countOnLastDate = sessions.count { it.timestamp.toLocalDate() == lastDate }
        if (countOnLastDate >= 2) {
            true
        } else {
            val dayBeforeLast = lastDate.minusDays(1)
            val hasSessionDayBefore = sessions.any { it.timestamp.toLocalDate() == dayBeforeLast }
            hasSessionDayBefore
        }
    } else {
        false
    }

    val detailText = getRandomComment(daysAgo, isErrorState, latest.sessionMode())

    return LatestSessionInfo(displayDate, time, durationText, daysAgo, detailText, isErrorState)
}

private fun formatPeriod(dateTime: LocalDateTime): String {
    val hour = dateTime.hour
    return when (hour) {
        in 5..7 -> "清晨"
        in 8..10 -> "上午"
        in 11..13 -> "中午"
        in 14..17 -> "下午"
        in 18..20 -> "傍晚"
        in 21..23 -> "深夜"
        else -> "凌晨"
    }
}

private fun getRandomComment(days: Long, isError: Boolean, mode: SessionMode): String {
    return when (mode) {
        SessionMode.SOLO_FEMALE -> getFemaleComment(days, isError)
        SessionMode.PAIR -> getPairComment(days, isError)
        SessionMode.SOLO_MALE -> getMaleComment(days, isError)
    }
}

private fun getFemaleComment(days: Long, isError: Boolean): String {
    return if (isError) {
        when (days) {
            0L -> listOf(
                "今天已经尽兴了，记得多喝水、早点休息 🌙",
                "开心最重要，身体感受放在第一位～",
                "连续作战辛苦了，给身体留点恢复时间吧",
                "尽兴之余别忘了卫生护理哦"
            ).random()

            1L -> listOf(
                "这两天状态很好呀，也别太累着自己",
                "尽兴很重要，休息同样重要～",
                "身体是最诚实的，听听它的节奏"
            ).random()

            else -> listOf(
                "最近频率不低，注意身体感受和休息",
                "适度放松，别忘了好好照顾自己",
                "给身体放个假，恢复一下状态吧"
            ).random()
        }
    } else {
        when (days) {
            0L -> listOf(
                "适度放松，取悦自己～",
                "身心舒畅，继续保持好状态",
                "今天也要好好爱自己 💕",
                "状态在线，享受当下"
            ).random()

            1L -> listOf(
                "隔天一次，节奏刚刚好",
                "张弛有度，身体状态很棒",
                "保持这个节奏就很舒服"
            ).random()

            2L, 3L -> listOf(
                "休息了几天，状态满格 ⚡",
                "身体充电完毕，随时出发",
                "休整得不错，精神饱满"
            ).random()

            in 4L..6L -> listOf(
                "好几天没记录了，顺其自然就好 🧘",
                "清心寡欲也是种修行",
                "跟随自己的节奏，不必强求"
            ).random()

            else -> listOf(
                "久违了！想放松的时候随时来 🔋",
                "尘封已久，记得定期关爱自己",
                "一切随缘，开心最重要"
            ).random()
        }
    }
}

private fun getPairComment(days: Long, isError: Boolean): String {
    return if (isError) {
        when (days) {
            0L -> listOf(
                "今天已经甜蜜满格，给彼此一点休息时间吧 💞",
                "热情似火也要注意休息，来日方长",
                "亲密无间，也别透支体力哦"
            ).random()

            1L -> listOf(
                "连续两天甜蜜加倍，注意保存体力～",
                "感情升温很快嘛，休息也不能落下",
                "甜度超标，适当缓缓"
            ).random()

            else -> listOf(
                "最近亲密频繁，记得互相照顾、适当休息",
                "爱意满满，身体也要跟得上",
                "适度亲密，细水长流"
            ).random()
        }
    } else {
        when (days) {
            0L -> listOf(
                "亲密愉快，感情又升温啦 💕",
                "享受彼此，默契满分",
                "今天的甜蜜值已达标",
                "双向奔赴的感觉真好"
            ).random()

            1L -> listOf(
                "隔天一约，节奏舒适",
                "亲密有度，感情更长久",
                "保持这个频率刚刚好"
            ).random()

            2L, 3L -> listOf(
                "小别胜新婚，蓄势待发 ⚡",
                "休息几天，期待值拉满",
                "养精蓄锐，下次更尽兴"
            ).random()

            in 4L..6L -> listOf(
                "好几天没亲密了，来点小浪漫？🧘",
                "平淡日子里也别忘了亲密感",
                "顺其自然，感情照样升温"
            ).random()

            else -> listOf(
                "久违的二人世界，安排一下？🔋",
                "尘封已久的亲密时光，值得重启",
                "感情需要经营，亲密也需要"
            ).random()
        }
    }
}

private fun getMaleComment(days: Long, isError: Boolean): String {
    return if (isError) {
        when (days) {
            0L -> listOf(
                "今天已经够拼了，给牛牛放个假吧 🌱",
                "如此勤奋，牛牛都要申请加班费了",
                "频率超标！建议立刻放下手机出门走走",
                "再这样下去要进化成无性繁殖了",
                "身体是革命的本钱，别透支了兄弟",
                "今日 KPI 已超额完成，可以下班了"
            ).random()

            1L -> listOf(
                "连续两天高强度，你是永动机吗？",
                "昨天刚交过卷，今天又补考？",
                "牛牛建议你加入工会维权",
                "这个频率，建议买份意外险",
                "身体在抗议，请及时停火"
            ).random()

            else -> listOf(
                "近期频率偏高，悠着点",
                "连续作战容易翻车，注意休整",
                "牛牛的耐久度不是无限的",
                "建议适当断网，回归现实生活"
            ).random()
        }
    } else {
        when (days) {
            0L -> listOf(
                "适度释放，身心舒畅～",
                "保持节奏，劳逸结合",
                "状态在线，继续发扬",
                "科学释放，有益身心健康",
                "记得多喝水，补充水分"
            ).random()

            1L -> listOf(
                "隔天一次，节奏拿捏得死死的",
                "间隔合理，精力充沛",
                "昨天刚打卡，今天养精蓄锐",
                "这个频率很科学，继续保持"
            ).random()

            2L, 3L -> listOf(
                "休息了几天，蓄势待发 ⚡",
                "身体状态回满，随时可以出发",
                "休养生息完毕，战力恢复",
                "养精蓄锐，状态满分"
            ).random()

            in 4L..6L -> listOf(
                "好几天没活动了，清心寡欲 🧘",
                "这是要修仙的节奏吗",
                "克制力惊人，佩服佩服",
                "精力储备充足，随时待命"
            ).random()

            else -> listOf(
                "久违了！是时候重启一下了 🔋",
                "尘封已久，建议定期维护",
                "再不用就要生锈了",
                "克制达人，向你学习",
                "心如止水，境界很高"
            ).random()
        }
    }
}

@SuppressLint("DefaultLocale")
fun calculatePeriodOverview(
    sessions: List<Session>,
    context: Context,
    now: LocalDateTime,
    type: PeriodType,
    label: String
): PeriodOverview {
    val filtered = sessions.filter { isWithinPeriod(it.timestamp, now, type) }

    val prevLabel = when (type) {
        PeriodType.WEEK -> "上周"
        PeriodType.MONTH -> "上月"
        PeriodType.YEAR -> "去年"
    }

    if (filtered.isEmpty()) {
        return PeriodOverview(
            periodLabel = label,
            count = 0,
            totalDurationSeconds = 0,
            longestDurationSeconds = 0,
            longestSessionDisplayDate = "",
            longestSessionEndTime = "",
            avgDurationSeconds = 0,
            avgRating = 0f,
            climaxCount = 0,
            topTags = emptyList()
        )
    }

    val startCurrent = when (type) {
        PeriodType.WEEK -> now.minusDays(now.dayOfWeek.value.toLong() - 1).toLocalDate()
            .atStartOfDay()

        PeriodType.MONTH -> now.withDayOfMonth(1).toLocalDate().atStartOfDay()
        PeriodType.YEAR -> now.withDayOfYear(1).toLocalDate().atStartOfDay()
    }
    val startPrev = when (type) {
        PeriodType.WEEK -> startCurrent.minusWeeks(1)
        PeriodType.MONTH -> startCurrent.minusMonths(1)
        PeriodType.YEAR -> startCurrent.minusYears(1)
    }
    val prevSessions = sessions.filter { it.timestamp in startPrev..<startCurrent }
    val prevCount = prevSessions.size

    val countComparison = if (prevCount == 0) {
        "${prevLabel}无记录"
    } else {
        val diff = filtered.size - prevCount
        when {
            diff > 0 -> "较${prevLabel}多${diff}次"
            diff < 0 -> "较${prevLabel}少${-diff}次"
            else -> "与${prevLabel}相同"
        }
    }

    val totalDuration = filtered.sumOf { it.duration }
    val prevTotalDuration = prevSessions.sumOf { it.duration }
    val durationComparison = if (prevCount == 0) {
        "${prevLabel}无记录"
    } else {
        val diff = totalDuration - prevTotalDuration
        when {
            diff > 0 -> "较${prevLabel}多${formatDuration(diff)}"
            diff < 0 -> "较${prevLabel}少${formatDuration(-diff)}"
            else -> "与${prevLabel}相同"
        }
    }

    val longest = filtered.maxByOrNull { it.duration }!!
    val endDateTime = longest.timestamp.plusSeconds(longest.duration.toLong())

    val tagCountsCurrent = mutableMapOf<String, Int>()
    for (s in filtered) {
        for (id in s.tagIds) {
            tagCountsCurrent[id] = (tagCountsCurrent[id] ?: 0) + 1
        }
    }
    val topTags: List<TagStat> = tagCountsCurrent.entries
        .sortedByDescending { it.value }
        .take(5)
        .mapNotNull { (id, count) ->
            TagSettings.getTag(context, id)?.let { tag ->
                TagStat(
                    id = tag.id,
                    name = tag.name,
                    color = tag.color,
                    icon = tag.icon,
                    count = count
                )
            }
        }

    val topTagsComparison = if (prevCount == 0) {
        "${prevLabel}无记录"
    } else {
        val prevTagCounts = mutableMapOf<String, Int>()
        for (s in prevSessions) {
            for (id in s.tagIds) {
                prevTagCounts[id] = (prevTagCounts[id] ?: 0) + 1
            }
        }
        val prevTopEntry = prevTagCounts.entries.maxByOrNull { it.value }
        if (prevTopEntry == null) {
            "${prevLabel}无记录"
        } else {
            val prevTagName = TagSettings.getTag(context, prevTopEntry.key)?.name ?: "已删除"
            "${prevLabel}最常：$prevTagName (${prevTopEntry.value}次)"
        }
    }

    val avgDurationSeconds = if (filtered.isNotEmpty()) totalDuration / filtered.size else 0
    val avgDurationComparison = if (prevCount == 0) {
        "${prevLabel}无记录"
    } else {
        val prevAvgDuration = if (prevSessions.isNotEmpty()) {
            prevTotalDuration / prevSessions.size
        } else 0
        val diff = avgDurationSeconds - prevAvgDuration
        when {
            diff > 0 -> "较${prevLabel}长 ${formatDuration(diff)}"
            diff < 0 -> "较${prevLabel}短 ${formatDuration(-diff)}"
            else -> "与${prevLabel}相同"
        }
    }

    val avgRating = filtered.map { it.rating }.average().toFloat()
    val avgRatingComparison = if (prevCount == 0) {
        "${prevLabel}无记录"
    } else {
        val prevAvgRating = prevSessions.map { it.rating }.average().toFloat()
        val diff = avgRating - prevAvgRating
        when {
            diff > 0.05f -> "较${prevLabel}高 ${String.format("%.1f", diff)}"
            diff < -0.05f -> "较${prevLabel}低 ${String.format("%.1f", -diff)}"
            else -> "与${prevLabel}持平"
        }
    }

    val climaxCount = filtered.sumOf { it.climaxCount }
    val climaxSessionCount = filtered.count { it.climaxCount > 0 }
    val partnerClimaxCount = filtered.sumOf { it.partnerClimaxCount }
    val pairCount = filtered.count { it.sessionMode() == SessionMode.PAIR }
    val climaxComparison = if (prevCount == 0) {
        "${prevLabel}无记录"
    } else {
        val prevClimaxCount = prevSessions.sumOf { it.climaxCount }
        val diff = climaxCount - prevClimaxCount
        when {
            diff > 0 -> "较${prevLabel}多 ${diff}次"
            diff < 0 -> "较${prevLabel}少 ${-diff}次"
            else -> "与${prevLabel}相同"
        }
    }

    return PeriodOverview(
        periodLabel = label,
        count = filtered.size,
        totalDurationSeconds = totalDuration,
        longestDurationSeconds = longest.duration,
        longestSessionDisplayDate = longest.timestamp.format(
            DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
        ),
        longestSessionEndTime = endDateTime.format(
            DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
        ),
        avgDurationSeconds = avgDurationSeconds,
        avgRating = avgRating,
        climaxCount = climaxCount,
        topTags = topTags,
        countComparison = countComparison,
        durationComparison = durationComparison,
        avgDurationComparison = avgDurationComparison,
        avgRatingComparison = avgRatingComparison,
        climaxComparison = climaxComparison,
        topTagsComparison = topTagsComparison,
        climaxSessionCount = climaxSessionCount,
        partnerClimaxCount = partnerClimaxCount,
        pairCount = pairCount
    )
}

fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) {
        "${hours}小时${minutes}分"
    } else {
        "${minutes}分钟"
    }
}

fun calculateActivityTimeData(
    sessions: List<Session>,
    now: LocalDateTime
): ActivityTimeData {
    if (sessions.isEmpty()) {
        return ActivityTimeData(
            weekdayCounts = List(7) { 0 },
            weekdayMax = 0,
            mostActiveWeekdayIndex = -1,
            hourCounts = List(24) { 0 },
            hourMax = 0,
            mostActiveHour = -1
        )
    }

    val weekdayCounts = IntArray(7)
    val hourCounts = IntArray(24)

    for (s in sessions) {
        val wd = s.timestamp.dayOfWeek.value - 1
        if (wd in 0..6) weekdayCounts[wd]++
        val h = s.timestamp.hour
        if (h in 0..23) hourCounts[h]++
    }

    val weekdayMax = weekdayCounts.maxOrNull() ?: 0
    val hourMax = hourCounts.maxOrNull() ?: 0

    var maxWd = -1
    var maxHr = -1
    for (i in 0 until 7) {
        if (weekdayCounts[i] > 0 && (maxWd == -1 || weekdayCounts[i] > weekdayCounts[maxWd])) {
            maxWd = i
        }
    }
    for (i in 0 until 24) {
        if (hourCounts[i] > 0 && (maxHr == -1 || hourCounts[i] > hourCounts[maxHr])) {
            maxHr = i
        }
    }

    return ActivityTimeData(
        weekdayCounts = weekdayCounts.toList(),
        weekdayMax = weekdayMax,
        mostActiveWeekdayIndex = maxWd,
        hourCounts = hourCounts.toList(),
        hourMax = hourMax,
        mostActiveHour = maxHr
    )
}

fun weekdayLabel(index: Int): String = when (index) {
    0 -> "周一"
    1 -> "周二"
    2 -> "周三"
    3 -> "周四"
    4 -> "周五"
    5 -> "周六"
    6 -> "周日"
    else -> "-"
}

fun calculateMonthlyTrendData(
    sessions: List<Session>,
    now: LocalDateTime
): MonthlyTrendData {
    val items = mutableListOf<MonthlyTrendItem>()
    val monthCountMap = HashMap<Int, Int>()
    for (s in sessions) {
        val key = s.timestamp.year * 12 + (s.timestamp.monthValue - 1)
        monthCountMap[key] = (monthCountMap[key] ?: 0) + 1
    }

    for (i in 13 downTo 0) {
        val d = now.minusMonths(i.toLong())
        val key = d.year * 12 + (d.monthValue - 1)
        val count = monthCountMap[key] ?: 0
        items += MonthlyTrendItem(
            label = "${d.monthValue}月",
            year = d.year,
            month = d.monthValue,
            count = count,
            isCurrent = i == 0
        )
    }

    // 预测
    val dayOfMonth = now.dayOfMonth
    val daysInMonth = YearMonth.of(now.year, now.monthValue).lengthOfMonth()
    val elapsedRatio = if (daysInMonth > 0) dayOfMonth.toFloat() / daysInMonth else 0f
    val currentActual = items.last().count
    val last3 = items.takeIf { it.size >= 4 }
        ?.let { it.subList(it.size - 4, it.size - 1) }
        ?: emptyList()
    val last3Avg = if (last3.isNotEmpty()) last3.map { it.count }.average().toFloat() else 0f

    val predictedCount = if (elapsedRatio > 0f) {
        val prorated = currentActual.toFloat() / elapsedRatio
        (prorated * 0.6f + last3Avg * 0.4f).toInt().coerceAtLeast(currentActual)
    } else {
        last3Avg.toInt()
    }

    return MonthlyTrendData(
        items = items,
        predictedCount = predictedCount,
        currentActual = currentActual,
        elapsedDays = dayOfMonth,
        totalDaysInMonth = daysInMonth,
        last3MonthsAvg = (last3Avg * 10f).toInt() / 10f
    )
}

fun calculateTagTrendData(
    sessions: List<Session>,
    context: Context,
    now: LocalDateTime,
    windowDays: Int = 30
): TagTrendData {
    if (sessions.isEmpty()) return TagTrendData(items = emptyList(), windowDays = windowDays)

    val recentCutoff = now.minusDays(windowDays.toLong())
    val previousCutoff = now.minusDays((windowDays * 2).toLong())

    val recent = HashMap<String, Int>()
    val previous = HashMap<String, Int>()

    for (s in sessions) {
        val ts = s.timestamp
        for (id in s.tagIds) {
            if (ts >= recentCutoff) {
                recent[id] = (recent[id] ?: 0) + 1
            } else if (ts >= previousCutoff) {
                previous[id] = (previous[id] ?: 0) + 1
            }
        }
    }

    val allIds = (recent.keys + previous.keys)
    val items = allIds.mapNotNull { id ->
        val tag = TagSettings.getTag(context, id) ?: return@mapNotNull null
        val r = recent[id] ?: 0
        val p = previous[id] ?: 0
        val (pct, dir) = when (p) {
            0 if r > 0 -> 100 to TagTrendDirection.UP
            0 if r == 0 -> 0 to TagTrendDirection.FLAT
            else -> {
                val v = ((r - p).toFloat() / p * 100f).toInt()
                v to when {
                    v > 0 -> TagTrendDirection.UP
                    v < 0 -> TagTrendDirection.DOWN
                    else -> TagTrendDirection.FLAT
                }
            }
        }
        TagTrendItem(
            id = tag.id,
            name = tag.name,
            color = tag.color,
            icon = tag.icon,
            recentCount = r,
            previousCount = p,
            changePercent = pct,
            trend = dir
        )
    }.sortedByDescending { it.recentCount }
        .take(10)

    return TagTrendData(items = items, windowDays = windowDays)
}

fun calculatePeriodDashboard(
    sessions: List<Session>,
    now: LocalDateTime,
    type: PeriodType
): PeriodDashboard {
    val label = when (type) {
        PeriodType.WEEK -> "本周"
        PeriodType.MONTH -> "本月"
        PeriodType.YEAR -> "今年"
    }

    val current = sessions.filter { isWithinPeriod(it.timestamp, now, type) }

    val startCurrent = when (type) {
        PeriodType.WEEK -> now.minusDays(now.dayOfWeek.value.toLong() - 1).toLocalDate()
            .atStartOfDay()

        PeriodType.MONTH -> now.withDayOfMonth(1).toLocalDate().atStartOfDay()
        PeriodType.YEAR -> now.withDayOfYear(1).toLocalDate().atStartOfDay()
    }
    val startPrev = when (type) {
        PeriodType.WEEK -> startCurrent.minusWeeks(1)
        PeriodType.MONTH -> startCurrent.minusMonths(1)
        PeriodType.YEAR -> startCurrent.minusYears(1)
    }
    val prev = sessions.filter { it.timestamp in startPrev..<startCurrent }

    val count = current.size
    val prevCount = prev.size
    val changePercent = when (prevCount) {
        0 if count > 0 -> 100
        0 -> 0
        else -> ((count - prevCount).toFloat() / prevCount * 100f).toInt()
    }

    val totalDuration = current.sumOf { it.duration }
    val avgDuration = if (count > 0) totalDuration / count else 0
    val climaxCount = current.sumOf { it.climaxCount }
    val partnerClimaxCount = current.sumOf { it.partnerClimaxCount }
    val pairCount = current.count { it.sessionMode() == SessionMode.PAIR }
    val climaxSessionCount = current.count {
        it.climaxCount > 0 || it.partnerClimaxCount > 0
    }
    val climaxRate = if (count > 0) (climaxSessionCount * 100 / count) else 0
    val streak = calculateStreakDays(sessions, now.toLocalDate())

    return PeriodDashboard(
        type = type,
        label = label,
        count = count,
        prevCount = prevCount,
        countChangePercent = changePercent,
        avgDurationSeconds = avgDuration,
        totalDurationSeconds = totalDuration,
        climaxRate = climaxRate,
        climaxCount = climaxCount,
        streakDays = streak,
        partnerClimaxCount = partnerClimaxCount,
        pairCount = pairCount
    )
}

fun calculateStreakDays(sessions: List<Session>, today: LocalDate): Int {
    if (sessions.isEmpty()) return 0
    val activeDays = sessions.map { it.timestamp.toLocalDate() }.toSet()
    val mostRecent = activeDays.maxOrNull() ?: return 0
    if (mostRecent.isBefore(today.minusDays(1))) return 0
    var streak = 0
    var cursor = mostRecent
    while (cursor in activeDays) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}