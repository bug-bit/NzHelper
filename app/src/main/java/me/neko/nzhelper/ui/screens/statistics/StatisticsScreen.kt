package me.neko.nzhelper.ui.screens.statistics

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import me.neko.nzhelper.data.PeriodOverview
import me.neko.nzhelper.data.Session
import me.neko.nzhelper.data.SessionRepository
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatisticsScreen() {
    val context = LocalContext.current
    val sessions = remember { mutableStateListOf<Session>() }

    LaunchedEffect(Unit) {
        val loaded = SessionRepository.loadSessions(context)
        sessions.clear()
        sessions.addAll(loaded)
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
    val daysAgo = java.time.temporal.ChronoUnit.DAYS.between(lastDate, today)

    val displayDate = when (daysAgo) {
        0L -> "今天"
        1L -> "昨天"
        else -> lastDate.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA))
    }

    val time = latest.timestamp.format(DateTimeFormatter.ofPattern("a h:mm", Locale.CHINA))
    val durationText = formatDuration(latest.duration)

    val isErrorState = daysAgo > 1

    val detailText = when (daysAgo) {
        0L -> getRandomComment(0)
        1L -> getRandomComment(1)
        else -> getRandomComment(daysAgo.toInt())
    }

    return LatestSessionInfo(displayDate, time, durationText, daysAgo, detailText, isErrorState)
}

private fun getRandomComment(days: Int): String {
    return when (days) {
        0 -> listOf(
            "适度释放，有益身心",
            "记得多喝水，补充水分",
            "注意休息，不要过度劳累",
            "保持良好的生活习惯"
        ).random()

        1 -> listOf(
            "昨日适度，保持规律",
            "劳逸结合最重要",
            "注意频率，呵护身体"
        ).random()

        2 -> listOf(
            "坚持了两天，自律真棒",
            "养精蓄锐，状态回升",
            "身体正在自我修复中"
        ).random()

        else -> listOf(
            "坚持了 $days 天，非常有毅力",
            "身体健康，精力充沛",
            "继续保持健康的生活方式",
            "身体状态正在恢复"
        ).random()
    }
}

private fun calculatePeriodOverview(
    sessions: List<Session>,
    now: LocalDateTime,
    type: PeriodType,
    label: String
): PeriodOverview {
    val filtered = sessions.filter { isWithinPeriod(it.timestamp, now, type) }

    if (filtered.isEmpty()) {
        return PeriodOverview(
            periodLabel = label, count = 0, totalDurationSeconds = 0,
            longestDurationSeconds = 0, longestSessionDisplayDate = "",
            longestSessionEndTime = "", mostUsedProps = "", mostUsedPropsCount = 0,
            mostCommonMood = "", mostCommonMoodCount = 0,
            mostCommonLocation = "", mostCommonLocationCount = 0,
            avgRating = 0f, movieCount = 0, climaxCount = 0
        )
    }

    val totalDuration = filtered.sumOf { it.duration }
    val longest = filtered.maxByOrNull { it.duration }!!
    val endDateTime = longest.timestamp.plusSeconds(longest.duration.toLong())

    val mostUsedProps = filtered.groupingBy { it.props }.eachCount()
        .maxByOrNull { it.value }

    val mostCommonMood = filtered.groupingBy { it.mood }.eachCount()
        .maxByOrNull { it.value }

    val mostCommonLocation = filtered
        .mapNotNull { it.location.takeIf { it.isNotEmpty() } }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }

    val avgRating = filtered.map { it.rating }.average().toFloat()

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
        movieCount = filtered.count { it.watchedMovie },
        climaxCount = filtered.count { it.climax }
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
            fontWeight = FontWeight.Bold,
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
    Dialog(onDismissRequest = onDismiss) {
        Surface(
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
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "共 ${overview.count} 次 · 总时长 ${formatDuration(overview.totalDurationSeconds)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

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
                            label = "道具",
                            value = "${overview.mostUsedProps} (${overview.mostUsedPropsCount}次)"
                        )
                        OverviewDetailRow(
                            label = "心情",
                            value = "${overview.mostCommonMood} (${overview.mostCommonMoodCount}次)"
                        )
                        OverviewDetailRow(
                            label = "地点",
                            value = if (overview.mostCommonLocation == "未记录") "未记录"
                            else "${overview.mostCommonLocation} (${overview.mostCommonLocationCount}次)"
                        )
                        OverviewDetailRow(
                            label = "平均评分",
                            value = "%.1f / 5.0".format(overview.avgRating)
                        )
                        OverviewDetailRow(
                            label = "小电影",
                            value = "${overview.movieCount} / ${overview.count} 次"
                        )
                        OverviewDetailRow(
                            label = "高潮",
                            value = "${overview.climaxCount} / ${overview.count} 次",
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
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
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
                    fontWeight = FontWeight.Bold,
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
    // 数据少于2条，无法计算间隔
    if (sessions.size < 2) return "刚开始记录，保持适度"

    val firstDate = sessions.first().timestamp.toLocalDate()
    val lastDate = sessions.last().timestamp.toLocalDate()
    // 计算首尾记录的时间跨度（天），至少为1天
    val daysSpan = java.time.temporal.ChronoUnit.DAYS.between(firstDate, lastDate).coerceAtLeast(1)

    // 计算平均每天的频率 (次/天)
    val frequency = sessions.size.toDouble() / daysSpan

    return when {
        // 每天一次甚至更多 -> 过度
        frequency >= 1.0 -> "平均每天一次以上，频率偏高，建议适度节制"

        // 两三天一次 -> 频繁
        frequency >= 0.3 -> "平均两三天一次，较为频繁，注意休息"

        // 一周一次左右 -> 适度
        frequency >= 0.14 -> "平均一周左右一次，频率适中，身心健康"

        // 更少 -> 节制
        else -> "频率较低，注意保持良好心态"
    }
}

@Preview(showBackground = true)
@Composable
fun StatisticsScreenPreview() {
    StatisticsScreen()
}