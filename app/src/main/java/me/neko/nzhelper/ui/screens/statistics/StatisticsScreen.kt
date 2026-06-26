package me.neko.nzhelper.ui.screens.statistics

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import me.neko.nzhelper.data.Session
import me.neko.nzhelper.data.SessionRepository
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatisticsScreen(isActive: Boolean = false) {
    val context = LocalContext.current
    val sessions = remember { mutableStateListOf<Session>() }

    LaunchedEffect(isActive) {
        if (isActive) {
            val loaded = SessionRepository.loadSessions(context)
            sessions.clear()
            sessions.addAll(loaded)
        }
    }

    val currentTime = LocalDateTime.now()

    val weekData by remember(sessions, currentTime) {
        derivedStateOf { calculatePeriodData(sessions, currentTime, PeriodType.WEEK) }
    }
    val monthData by remember(sessions, currentTime) {
        derivedStateOf { calculatePeriodData(sessions, currentTime, PeriodType.MONTH) }
    }
    val yearData by remember(sessions, currentTime) {
        derivedStateOf { calculatePeriodData(sessions, currentTime, PeriodType.YEAR) }
    }

    // 总体统计数据
    val totalStats by remember(sessions) {
        derivedStateOf {
            if (sessions.isEmpty()) {
                TotalStats(0, 0, 0f, 0, 0, 0)
            } else {
                val totalCount = sessions.size
                val totalSeconds = sessions.sumOf { it.duration }
                val avgMinutes =
                    if (totalCount > 0) totalSeconds.toFloat() / (60 * totalCount) else 0f

                TotalStats(
                    totalCount = totalCount,
                    totalSeconds = totalSeconds,
                    avgMinutes = avgMinutes,
                    weekCount = sessions.count {
                        isWithinPeriod(it.timestamp, currentTime, PeriodType.WEEK)
                    },
                    monthCount = sessions.count {
                        isWithinPeriod(it.timestamp, currentTime, PeriodType.MONTH)
                    },
                    yearCount = sessions.count {
                        isWithinPeriod(it.timestamp, currentTime, PeriodType.YEAR)
                    }
                )
            }
        }
    }

    val latestInfo by remember(sessions) {
        derivedStateOf { calculateLatestInfo(sessions) }
    }

    var selectedOverview by remember { mutableStateOf<PeriodOverview?>(null) }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("统计") },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            if (sessions.isEmpty()) {
                EmptyStateView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // 顶部卡片区域
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            LatestSessionCard(
                                latestInfo = latestInfo,
                                modifier = Modifier.fillMaxWidth()
                            )
                            TotalStatCard(
                                stats = totalStats,
                                sessions = sessions,
                                onPeriodClick = { type, label ->
                                    selectedOverview = calculatePeriodOverview(
                                        sessions, currentTime, type, label
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                PeriodSection(title = "本周", data = weekData)
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                PeriodSection(title = "本月", data = monthData)
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                PeriodSection(title = "今年", data = yearData)
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }

    selectedOverview?.let { overview ->
        PeriodOverviewDialog(
            overview = overview,
            onDismiss = { selectedOverview = null }
        )
    }
}

// --- 数据类与计算逻辑 ---
private enum class PeriodType { WEEK, MONTH, YEAR }

private data class PeriodData(
    val totalDurationSeconds: Int,
    val avgDurationMinutes: Float,
    val chartData: List<Pair<String, Float>>
)

private data class TotalStats(
    val totalCount: Int,
    val totalSeconds: Int,
    val avgMinutes: Float,
    val weekCount: Int,
    val monthCount: Int,
    val yearCount: Int
)

private data class LatestSessionInfo(
    val displayDate: String,
    val time: String,
    val durationText: String,
    val daysAgo: Long,
    val detailText: String,
    val isErrorState: Boolean
)

private data class PeriodOverview(
    val periodLabel: String,
    val count: Int,
    val totalDurationSeconds: Int,
    val longestDurationSeconds: Int,
    val longestSessionDisplayDate: String,
    val longestSessionEndTime: String,
    val mostUsedProps: String,
    val mostUsedPropsCount: Int,
    val mostCommonMood: String,
    val mostCommonMoodCount: Int,
    val mostCommonLocation: String,
    val mostCommonLocationCount: Int,
    val avgRating: Float,
    val movieCount: Int,
    val climaxCount: Int,
    val countComparison: String = "",
    val durationComparison: String = "",
    val propsComparison: String = "",
    val moodComparison: String = "",
    val locationComparison: String = "",
    val avgRatingComparison: String = "",
    val movieComparison: String = "",
    val climaxComparison: String = ""
)

/**
 * 统一的周期数据计算
 */
private fun calculatePeriodData(
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

private fun isWithinPeriod(
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

private fun calculateLatestInfo(sessions: List<Session>): LatestSessionInfo? {
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

    val time = latest.timestamp.format(DateTimeFormatter.ofPattern("a h:mm", Locale.CHINA))
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

    val detailText = getRandomComment(daysAgo, isErrorState)

    return LatestSessionInfo(displayDate, time, durationText, daysAgo, detailText, isErrorState)
}

private fun getRandomComment(days: Long, isError: Boolean): String {
    return if (isError) {
        when (days) {
            0L -> listOf(
                "今日不宜贪多，请注意节制",
                "频率过高，身体需要休息",
                "透支精力，明天暂停吧",
                "为了健康，请适当克制"
            ).random()

            1L -> listOf(
                "连续高强度，身体吃不消的",
                "昨日频率较高，今日需休养",
                "注意节奏，不要贪多",
                "过于频繁，容易疲劳"
            ).random()

            else -> listOf(
                "近期频率偏高，建议克制",
                "注意身体健康，不要过度"
            ).random()
        }
    } else {
        when (days) {
            0L -> listOf(
                "适度释放，身心舒畅",
                "保持规律，劳逸结合",
                "状态不错，继续保持",
                "记得多喝水，补充水分"
            ).random()

            1L -> listOf(
                "昨日适度，状态不错",
                "间隔合理，精力充沛",
                "节奏很好，享受生活"
            ).random()

            2L -> listOf(
                "休息了两天，精力恢复",
                "身体状态满分，蓄势待发",
                "休养生息，很有活力"
            ).random()

            else -> listOf(
                "很久没活动了，身体状态极佳",
                "保持健康的生活方式",
                "精力充沛，充满活力"
            ).random()
        }
    }
}

@SuppressLint("DefaultLocale")
private fun calculatePeriodOverview(
    sessions: List<Session>,
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
            periodLabel = label, count = 0, totalDurationSeconds = 0,
            longestDurationSeconds = 0, longestSessionDisplayDate = "",
            longestSessionEndTime = "", mostUsedProps = "", mostUsedPropsCount = 0,
            mostCommonMood = "", mostCommonMoodCount = 0,
            mostCommonLocation = "", mostCommonLocationCount = 0,
            avgRating = 0f, movieCount = 0, climaxCount = 0,
            countComparison = "", durationComparison = "",
            propsComparison = "", moodComparison = "", locationComparison = "",
            avgRatingComparison = "", movieComparison = "", climaxComparison = ""
        )
    }

    // 获取上一周期时间范围
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

    // 次数对比
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

    // 总时长对比
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

    val mostUsedProps = filtered.groupingBy { it.props }.eachCount().maxByOrNull { it.value }
    val prevMostUsedProps =
        prevSessions.groupingBy { it.props }.eachCount().maxByOrNull { it.value }

    val propsComparison = if (prevMostUsedProps == null) {
        "${prevLabel}无记录"
    } else {
        val prevPropsName = prevMostUsedProps.key.ifEmpty { "无" }
        if (prevMostUsedProps.key == mostUsedProps?.key) {
            "${prevLabel}也为：$prevPropsName (${prevMostUsedProps.value}次)"
        } else {
            "${prevLabel}为：$prevPropsName (${prevMostUsedProps.value}次)"
        }
    }

    val mostCommonMood = filtered.groupingBy { it.mood }.eachCount().maxByOrNull { it.value }
    val prevMostCommonMood =
        prevSessions.groupingBy { it.mood }.eachCount().maxByOrNull { it.value }

    val moodComparison = if (prevMostCommonMood == null) {
        "${prevLabel}无记录"
    } else {
        val prevMoodName = prevMostCommonMood.key.ifEmpty { "无" }
        if (prevMostCommonMood.key == mostCommonMood?.key) {
            "${prevLabel}也为：$prevMoodName (${prevMostCommonMood.value}次)"
        } else {
            "${prevLabel}为：$prevMoodName (${prevMostCommonMood.value}次)"
        }
    }

    val mostCommonLocation = filtered
        .mapNotNull { it.location.takeIf { it.isNotEmpty() } }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
    val prevMostCommonLocation = prevSessions
        .mapNotNull { it.location.takeIf { it.isNotEmpty() } }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }

    val locationComparison = if (prevMostCommonLocation == null) {
        "${prevLabel}无记录"
    } else {
        if (prevMostCommonLocation.key == mostCommonLocation?.key) {
            "${prevLabel}也为：${prevMostCommonLocation.key} (${prevMostCommonLocation.value}次)"
        } else {
            "${prevLabel}为：${prevMostCommonLocation.key} (${prevMostCommonLocation.value}次)"
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

    val movieCount = filtered.count { it.watchedMovie }
    val movieComparison = if (prevCount == 0) {
        "${prevLabel}无记录"
    } else {
        val prevMovieCount = prevSessions.count { it.watchedMovie }
        val diff = movieCount - prevMovieCount
        when {
            diff > 0 -> "较${prevLabel}多 ${diff}次"
            diff < 0 -> "较${prevLabel}少 ${-diff}次"
            else -> "与${prevLabel}相同"
        }
    }

    val climaxCount = filtered.count { it.climax }
    val climaxComparison = if (prevCount == 0) {
        "${prevLabel}无记录"
    } else {
        val prevClimaxCount = prevSessions.count { it.climax }
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
        mostUsedProps = mostUsedProps?.key ?: "",
        mostUsedPropsCount = mostUsedProps?.value ?: 0,
        mostCommonMood = mostCommonMood?.key ?: "",
        mostCommonMoodCount = mostCommonMood?.value ?: 0,
        mostCommonLocation = mostCommonLocation?.key ?: "未记录",
        mostCommonLocationCount = mostCommonLocation?.value ?: 0,
        avgRating = avgRating,
        movieCount = movieCount,
        climaxCount = climaxCount,
        countComparison = countComparison,
        durationComparison = durationComparison,
        propsComparison = propsComparison,
        moodComparison = moodComparison,
        locationComparison = locationComparison,
        avgRatingComparison = avgRatingComparison,
        movieComparison = movieComparison,
        climaxComparison = climaxComparison
    )
}

// --- UI 组件 ---
@Composable
private fun PeriodSection(
    title: String,
    data: PeriodData
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatDuration(data.totalDurationSeconds),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "总时长",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (data.totalDurationSeconds > 0) "%.1f分".format(data.avgDurationMinutes) else "0分",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (data.totalDurationSeconds > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "平均",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // 图表
        BarChart(data = data.chartData)
    }
}

@Composable
private fun EmptyStateView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "(。・ω・。)",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "暂无统计数据\n快去完成第一次记录吧！",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TotalStatCard(
    stats: TotalStats,
    sessions: List<Session>,
    onPeriodClick: (PeriodType, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val statusText = buildTotalStatStatus(sessions)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "总体统计",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = formatDuration(stats.totalSeconds),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                val avgText =
                    if (stats.totalCount > 0) "%.1f 分钟".format(stats.avgMinutes) else "0 分钟"
                Text(
                    text = "平均每次 $avgText · 共 ${stats.totalCount} 次",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (statusText.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PeriodStatCard(
                    label = "本周",
                    count = stats.weekCount,
                    modifier = Modifier.weight(1f),
                    onClick = { onPeriodClick(PeriodType.WEEK, "本周") }
                )
                PeriodStatCard(
                    label = "本月",
                    count = stats.monthCount,
                    modifier = Modifier.weight(1f),
                    onClick = { onPeriodClick(PeriodType.MONTH, "本月") }
                )
                PeriodStatCard(
                    label = "今年",
                    count = stats.yearCount,
                    modifier = Modifier.weight(1f),
                    onClick = { onPeriodClick(PeriodType.YEAR, "今年") }
                )
            }
        }
    }
}

@Composable
private fun PeriodStatCard(
    label: String,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun LatestSessionCard(
    latestInfo: LatestSessionInfo?,
    modifier: Modifier = Modifier
) {
    val isError = latestInfo?.isErrorState == true

    val gradientBrush = Brush.verticalGradient(
        colors = if (isError) {
            listOf(
                MaterialTheme.colorScheme.error,
                MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
            )
        } else {
            listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
            )
        }
    )

    val contentColor =
        if (isError) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
    val contentColorVariant = contentColor.copy(alpha = 0.8f)

    val overlayColor = contentColor.copy(alpha = 0.15f)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .fillMaxWidth()
        ) {
            if (latestInfo == null) {
                Box(
                    modifier = Modifier
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "还没有开始记录哦～",
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColorVariant
                    )
                }
            } else {
                Column {
                    Row(
                        modifier = Modifier
                            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(overlayColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = contentColor
                            )
                        }

                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "最近一次 · ${latestInfo.displayDate}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = contentColor
                            )
                            Text(
                                text = "${latestInfo.time} · 坚持了 ${latestInfo.durationText}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = contentColorVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(overlayColor)
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = latestInfo.detailText,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodOverviewDialog(
    overview: PeriodOverview,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current

    val screenHeight = with(density) {
        LocalWindowInfo.current.containerSize.height.toDp()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.heightIn(max = screenHeight * 0.95f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "${overview.periodLabel}总览",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 总览与周期对比
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "共 ${overview.count} 次 · 总时长 ${formatDuration(overview.totalDurationSeconds)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (overview.count > 0 && overview.countComparison.isNotEmpty()) {
                            Text(
                                text = "次数：${overview.countComparison}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (overview.count > 0 && overview.durationComparison.isNotEmpty()) {
                            Text(
                                text = "总时长：${overview.durationComparison}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (overview.count > 0) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                                alpha = 0.12f
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "最长记录",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                            alpha = 0.7f
                                        ),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "${overview.longestSessionDisplayDate} · ${
                                            formatDuration(
                                                overview.longestDurationSeconds
                                            )
                                        }",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${overview.longestSessionEndTime} 结束",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                            alpha = 0.65f
                                        )
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                alpha = 0.3f
                            )
                        )

                        OverviewDetailRow(
                            label = "最多次数的道具",
                            value = "${overview.mostUsedProps.ifEmpty { "无" }} (${overview.mostUsedPropsCount}次)",
                            comparison = overview.propsComparison
                        )
                        OverviewDetailRow(
                            label = "最多次数的心情",
                            value = "${overview.mostCommonMood.ifEmpty { "无" }} (${overview.mostCommonMoodCount}次)",
                            comparison = overview.moodComparison
                        )
                        OverviewDetailRow(
                            label = "最多次数的地点",
                            value = if (overview.mostCommonLocation == "未记录") "未记录"
                            else "${overview.mostCommonLocation} (${overview.mostCommonLocationCount}次)",
                            comparison = overview.locationComparison
                        )
                        OverviewDetailRow(
                            label = "平均评分",
                            value = "%.1f / 5.0".format(overview.avgRating),
                            comparison = overview.avgRatingComparison
                        )
                        OverviewDetailRow(
                            label = "小电影",
                            value = "${overview.movieCount} / ${overview.count} 次",
                            comparison = overview.movieComparison
                        )
                        OverviewDetailRow(
                            label = "高潮",
                            value = "${overview.climaxCount} / ${overview.count} 次",
                            comparison = overview.climaxComparison,
                            showDivider = false
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "暂无记录",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("关闭")
                }
            }
        }
    }
}

@Composable
private fun OverviewDetailRow(
    label: String,
    value: String,
    comparison: String = "",
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                if (comparison.isNotEmpty()) {
                    Text(
                        text = comparison,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        }
    }
}

// --- 通用图表组件 ---
@Composable
private fun BarChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 160.dp,
    minBarWidth: Dp = 16.dp,
    maxBarWidth: Dp = 40.dp,
    spacing: Dp = 12.dp
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier.height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val maxValue = data.maxOf { it.second }.coerceAtLeast(1f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight + 40.dp) // 给 X 轴留空间
    ) {
        YAxis(
            maxValue = maxValue,
            modifier = Modifier
                .wrapContentWidth()
                .fillMaxHeight()
        )

        @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // 动态计算柱子宽度
            val totalSpacing = spacing * (data.size - 1)
            val availableWidth = maxWidth - totalSpacing
            val idealBarWidth = availableWidth / data.size
            val barWidth = idealBarWidth.coerceIn(minBarWidth, maxBarWidth)

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                items(data) { (date, value) ->
                    BarItem(
                        value = value,
                        maxValue = maxValue,
                        date = date,
                        barWidth = barWidth,
                        chartHeight = chartHeight
                    )
                }
            }
        }
    }
}

@Composable
private fun YAxis(
    maxValue: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(bottom = 28.dp, end = 8.dp), // 底部留给 X 轴
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End
    ) {
        Text(
            "${maxValue.toInt()}m", // 缩写单位
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            "${(maxValue / 2).toInt()}m",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Text(
            "0",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun BarItem(
    value: Float,
    maxValue: Float,
    date: String,
    barWidth: Dp,
    chartHeight: Dp
) {
    val ratio = value / maxValue
    val barHeight = chartHeight * ratio

    val barColor = MaterialTheme.colorScheme.primary
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(barColor, barColor.copy(alpha = 0.6f))
    )

    Column(
        modifier = Modifier.width(barWidth),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .height(chartHeight)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .height(if (barHeight > 0.dp) barHeight else 0.dp) // 防止负值
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(gradientBrush)
            )

            // 数值显示逻辑：柱子太短时显示在上方
            val showInside = barHeight > 36.dp

            if (value > 0f) {
                Text(
                    text = value.toInt().toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (showInside) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(
                            y = if (showInside) -barHeight / 2 else -barHeight - 8.dp
                        )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 日期（X 轴）- 颜色减淡
        Text(
            text = date,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) {
        "${hours}小时${minutes}分"
    } else {
        "${minutes}分钟"
    }
}

private fun buildTotalStatStatus(sessions: List<Session>): String {
    if (sessions.isEmpty()) return ""
    if (sessions.size < 2) return "刚开始记录，保持适度"

    val dates = sessions.map { it.timestamp.toLocalDate() }
    val earliest = dates.min()
    val latest = dates.max()

    val daysSpan = ChronoUnit.DAYS.between(earliest, latest).coerceAtLeast(1)

    val frequency = sessions.size.toDouble() / daysSpan

    return when {
        frequency >= 1.0 -> "平均每天一次以上，频率偏高，建议适度节制"
        frequency >= 0.3 -> "平均两三天一次，较为频繁，注意休息"
        frequency >= 0.14 -> "平均一周左右一次，频率适中，身心健康"
        else -> "频率较低，精力充沛"
    }
}

@Preview(showBackground = true)
@Composable
fun StatisticsScreenPreview() {
    StatisticsScreen()
}