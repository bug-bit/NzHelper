package me.neko.nzhelper.ui.screens.statistics

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.data.Session
import me.neko.nzhelper.data.SessionRepository
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.random.Random

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
    val weekStats by remember {
        derivedStateOf {
            calculatePeriodStats(sessions, PeriodType.WEEK, currentTime)
        }
    }
    val monthStats by remember {
        derivedStateOf {
            calculatePeriodStats(sessions, PeriodType.MONTH, currentTime)
        }
    }
    val yearStats by remember {
        derivedStateOf {
            calculatePeriodStats(sessions, PeriodType.YEAR, currentTime)
        }
    }

    val weekCount by remember {
        derivedStateOf {
            sessions.count {
                it.timestamp >= currentTime.minusDays(currentTime.dayOfWeek.value.toLong() - 1)
                    .withHour(0).withMinute(0).withSecond(0).withNano(0)
            }
        }
    }
    val monthCount by remember {
        derivedStateOf {
            sessions.count {
                it.timestamp >= currentTime.withDayOfMonth(1)
                    .withHour(0).withMinute(0).withSecond(0).withNano(0)
            }
        }
    }
    val yearCount by remember {
        derivedStateOf {
            sessions.count {
                it.timestamp >= currentTime.withDayOfYear(1)
                    .withHour(0).withMinute(0).withSecond(0).withNano(0)
            }
        }
    }

    // 计算总体统计
    val totalStats by remember {
        derivedStateOf {
            if (sessions.isEmpty()) {
                Triple(0, 0, 0f)
            } else {
                val totalCount = sessions.size
                val totalSeconds = sessions.sumOf { it.duration }
                val avgMinutes = totalSeconds.toFloat() / (60 * totalCount)
                Triple(totalCount, totalSeconds, avgMinutes)
            }
        }
    }

    val weekDailyStats by remember {
        derivedStateOf {
            if (sessions.isEmpty()) emptyList()
            else {
                val now = LocalDateTime.now()
                // 计算本周一的日期
                val monday = now.minusDays(now.dayOfWeek.value.toLong() - 1)
                    .withHour(0).withMinute(0).withSecond(0).withNano(0)
                    .toLocalDate()

                // 生成本周7天的日期列表（周一到周日）
                val weekDays = (0..6).map { monday.plusDays(it.toLong()) }

                // 按日期分组统计
                val statsMap = sessions
                    .filter { it.timestamp.toLocalDate() >= monday }
                    .groupBy { it.timestamp.toLocalDate() }
                    .mapValues { entry ->
                        DailyStat(
                            count = entry.value.size,
                            totalDuration = entry.value.sumOf { it.duration }
                        )
                    }

                // 按周一到周日顺序构建完整7天数据（缺失天补0）
                weekDays.map { date ->
                    val dayOfWeekName = when (date.dayOfWeek) {
                        DayOfWeek.MONDAY -> "一"
                        DayOfWeek.TUESDAY -> "二"
                        DayOfWeek.WEDNESDAY -> "三"
                        DayOfWeek.THURSDAY -> "四"
                        DayOfWeek.FRIDAY -> "五"
                        DayOfWeek.SATURDAY -> "六"
                        DayOfWeek.SUNDAY -> "日"
                        else -> ""
                    }
                    val stat = statsMap[date] ?: DailyStat(count = 0, totalDuration = 0)
                    dayOfWeekName to (stat.totalDuration / 60f)  // 直接转为分钟
                }
            }
        }
    }

    val monthDailyStats by remember {
        derivedStateOf {
            if (sessions.isEmpty()) emptyList()
            else {
                val now = LocalDateTime.now()
                val firstDayOfMonth = now.withDayOfMonth(1).toLocalDate()

                sessions
                    .filter {
                        val date = it.timestamp.toLocalDate()
                        date >= firstDayOfMonth
                    }
                    .groupBy { it.timestamp.toLocalDate() }
                    .mapValues { entry ->
                        entry.value.sumOf { it.duration } / 60f
                    }
                    .filter { it.value > 0f }  // 过滤掉空白日期
                    .entries
                    .sortedBy { it.key }
                    .map { entry ->
                        entry.key.format(DateTimeFormatter.ofPattern("dd")) to entry.value
                    }
            }
        }
    }

    val yearMonthlyStats by remember {
        derivedStateOf {
            if (sessions.isEmpty()) emptyList()
            else {
                val now = LocalDateTime.now()
                val currentYear = now.year

                // 按年月分组统计时长（秒 → 分钟）
                val statsMap = sessions
                    .filter { it.timestamp.year == currentYear }
                    .groupBy { YearMonth.from(it.timestamp) }
                    .mapValues { entry ->
                        entry.value.sumOf { it.duration } / 60f
                    }

                // 只保留有时长的月份，并按月份顺序排序
                statsMap
                    .filter { it.value > 0f }  // 过滤掉空白月份
                    .entries
                    .sortedBy { it.key }       // 从1月到12月自然排序
                    .map { entry ->
                        val monthName = when (entry.key.monthValue) {
                            1 -> "1月"
                            2 -> "2月"
                            3 -> "3月"
                            4 -> "4月"
                            5 -> "5月"
                            6 -> "6月"
                            7 -> "7月"
                            8 -> "8月"
                            9 -> "9月"
                            10 -> "10月"
                            11 -> "11月"
                            12 -> "12月"
                            else -> ""
                        }
                        monthName to entry.value
                    }
            }
        }
    }

    val latestSessionInfo by remember {
        derivedStateOf {
            if (sessions.isEmpty()) {
                null
            } else {
                val latest = sessions.maxByOrNull { it.timestamp }!!
                val lastDate = latest.timestamp.toLocalDate()
                val daysAgo = ChronoUnit.DAYS.between(lastDate, LocalDateTime.now().toLocalDate())

                // 主日期显示
                val displayDate = when (daysAgo) {
                    0L -> "今天"
                    1L -> "昨天"
                    else -> {
                        val dateFmt = lastDate.format(DateTimeFormatter.ofPattern("M月d日"))
                        val dayOfWeek = when (lastDate.dayOfWeek) {
                            DayOfWeek.MONDAY -> "星期一"
                            DayOfWeek.TUESDAY -> "星期二"
                            DayOfWeek.WEDNESDAY -> "星期三"
                            DayOfWeek.THURSDAY -> "星期四"
                            DayOfWeek.FRIDAY -> "星期五"
                            DayOfWeek.SATURDAY -> "星期六"
                            DayOfWeek.SUNDAY -> "星期日"
                            else -> ""
                        }
                        "$dateFmt $dayOfWeek"
                    }
                }

                // 时间
                val time = latest.timestamp.format(
                    DateTimeFormatter.ofPattern("a h:mm").withLocale(Locale.CHINA)
                )

                fun randomOf(vararg list: String): String =
                    list[Random.nextInt(list.size)]

                val breakDetail = when (daysAgo) {
                    0L -> randomOf(
                        "今日已交作业",
                        "今天完成了释放指标",
                        "已完成今日份输出",
                        "今天没忍住，已记录"
                    )

                    1L -> randomOf(
                        "昨天完成了一次",
                        "昨日成功部署",
                        "昨天交过作业了",
                        "昨天有过记录"
                    )

                    2L -> randomOf(
                        "已经鸽了 2 天",
                        "空窗 2 天中",
                        "连续摆烂 2 天"
                    )

                    else -> {
                        val startFmt =
                            lastDate.plusDays(1).format(DateTimeFormatter.ofPattern("M月d日"))

                        randomOf(
                            "已经鸽了 $daysAgo 天（自 $startFmt 起）",
                            "空窗 $daysAgo 天了（$startFmt 开始）",
                            "持续摆烂 $daysAgo 天（从 $startFmt 算）"
                        )
                    }
                }

                LatestSessionInfo(
                    daysAgo = daysAgo,
                    displayDate = displayDate,
                    time = time,
                    durationSeconds = latest.duration,
                    breakDetail = breakDetail
                )
            }
        }
    }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("统计") },
                scrollBehavior = scrollBehavior
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
                // 空状态 整个屏幕居中显示提示
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "(。・ω・。)",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无统计数据哦！",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // 有数据时正常使用 LazyColumn
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {

                    item {
                        LatestSessionCard(
                            latestInfo = latestSessionInfo,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    // 总体统计卡片
                    item {
                        TotalStatCard(
                            totalCount = totalStats.first,
                            totalSeconds = totalStats.second,
                            avgMinutes = totalStats.third,
                            weekCount = weekCount,
                            monthCount = monthCount,
                            yearCount = yearCount,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Column {
                            Text(
                                "本周",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 32.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        formatDuration(weekStats.first),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "本周总时长",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (weekStats.first > 0) {
                                        Text(
                                            "%.1f 分钟".format(weekStats.second),
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "平均每次",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            "0 分钟",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "平均每次",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            BarChart(
                                data = weekDailyStats
                            )
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "本月",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 32.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        formatDuration(monthStats.first),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "本月总时长",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (monthStats.first > 0) {
                                        Text(
                                            "%.1f 分钟".format(monthStats.second),
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "平均每次",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            "0 分钟",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "平均每次",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            BarChart(
                                data = monthDailyStats
                            )
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "今年",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 32.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        formatDuration(yearStats.first),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "今年总时长",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (yearStats.first > 0) {
                                        Text(
                                            "%.1f 分钟".format(yearStats.second),
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "平均每次",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            "0 分钟",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "平均每次",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            BarChart(
                                data = yearMonthlyStats
                            )
                        }
                    }
                }
            }
        }
    }
}

// 枚举周期类型
private enum class PeriodType { WEEK, MONTH, YEAR }

// 计算某个周期的时长统计
private fun calculatePeriodStats(
    sessions: List<Session>,
    type: PeriodType,
    now: LocalDateTime
): Pair<Int, Float> { // Pair<总时长秒数, 平均单次时长分钟>
    val filtered = sessions.filter { session ->
        when (type) {
            PeriodType.WEEK -> {
                val weekStart = now.minusDays(now.dayOfWeek.value.toLong() - 1) // 本周一 00:00
                    .withHour(0).withMinute(0).withSecond(0).withNano(0)
                session.timestamp >= weekStart
            }

            PeriodType.MONTH -> {
                val monthStart = now.withDayOfMonth(1)
                    .withHour(0).withMinute(0).withSecond(0).withNano(0)
                session.timestamp >= monthStart
            }

            PeriodType.YEAR -> {
                val yearStart = now.withDayOfYear(1)
                    .withHour(0).withMinute(0).withSecond(0).withNano(0)
                session.timestamp >= yearStart
            }
        }
    }

    if (filtered.isEmpty()) return 0 to 0f

    val totalSeconds = filtered.sumOf { it.duration }
    val avgMinutes = totalSeconds.toFloat() / (60 * filtered.size)

    return totalSeconds to avgMinutes
}

// 格式化秒数为 “X小时 Y分钟”
private fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) {
        "${hours}小时 ${minutes}分钟"
    } else {
        "${minutes}分钟"
    }
}

@Composable
private fun TotalStatCard(
    totalCount: Int,
    totalSeconds: Int,
    avgMinutes: Float,
    weekCount: Int,
    monthCount: Int,
    yearCount: Int,
    modifier: Modifier = Modifier
) {

    val statusText by remember(totalCount) {
        mutableStateOf(buildTotalStatStatus(totalCount))
    }

    ElevatedCard(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                text = "总体统计",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = formatDuration(totalSeconds),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )

            val avgText =
                if (totalCount > 0) "%.1f 分钟".format(avgMinutes) else "0 分钟"

            Text(
                text = "平均每次 $avgText · 共 $totalCount 次",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatChip(label = "本周", value = weekCount)
                StatChip(label = "本月", value = monthCount)
                StatChip(label = "今年", value = yearCount)
            }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: Int
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = "$label $value 次",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun LatestSessionCard(
    latestInfo: LatestSessionInfo?,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "最近一次",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (latestInfo == null) {
                Text(
                    text = "还没有开始记录哦～\n快去完成第一次吧！",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            } else {
                val durationText = formatDuration(latestInfo.durationSeconds)

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = latestInfo.displayDate,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${latestInfo.time} · 坚持了 $durationText",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    val (textColor, backgroundColor) = when (latestInfo.daysAgo) {
                        0L -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer
                        1L -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.errorContainer
                    }

                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = backgroundColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = latestInfo.breakDetail,
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

// 每日统计数据类
private data class DailyStat(
    val count: Int,
    val totalDuration: Int // 秒
)

private data class LatestSessionInfo(
    val daysAgo: Long,
    val displayDate: String,
    val time: String,
    val durationSeconds: Int,
    val breakDetail: String
)

// 条形图
@Composable
fun BarChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 240.dp,
    minBarWidth: Dp = 16.dp,
    maxBarWidth: Dp = 54.dp,
    spacing: Dp = 16.dp
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier.height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val maxValue = data.maxOf { it.second }.coerceAtLeast(1f)
    val barColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight + 60.dp) // 给 X 轴留空间
    ) {
        // ================= Y Axis =================
        YAxis(
            maxValue = maxValue,
            modifier = Modifier
                .wrapContentWidth()
                .fillMaxHeight()
        )

        // ================= Chart =================
        @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            val totalSpacing = spacing * (data.size - 1)
            val availableWidth = maxWidth - totalSpacing
            val idealBarWidth = availableWidth / data.size
            val barWidth = idealBarWidth.coerceIn(minBarWidth, maxBarWidth)

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                items(data) { (date, value) ->
                    BarItem(
                        value = value,
                        maxValue = maxValue,
                        date = date,
                        barWidth = barWidth,
                        chartHeight = chartHeight,
                        color = barColor
                    )
                }
            }
        }
    }
}

@Composable
private fun YAxis(
    maxValue: Float,
    modifier: Modifier = Modifier,
    tickCount: Int = 5
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in tickCount downTo 0) {
                val value = maxValue * i / tickCount
                Text(
                    text = "${value.toInt()} 分钟",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BarItem(
    value: Float,
    maxValue: Float,
    date: String,
    barWidth: Dp,
    chartHeight: Dp,
    color: Color
) {
    val ratio = value / maxValue
    val barHeight = chartHeight * ratio

    Column(
        modifier = Modifier.width(barWidth),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ======= 图表区域 =======
        Box(
            modifier = Modifier
                .height(chartHeight)
                .fillMaxWidth()
        ) {

            // 柱子（贴底）
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .height(barHeight)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
            )

            // 数值
            val showInside = barHeight < 48.dp

            Text(
                text = value.toInt().toString(),
                style = MaterialTheme.typography.bodySmall,
                color = if (showInside) Color.White else Color.Black,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(
                        y = if (showInside)
                            -barHeight / 2
                        else
                            -barHeight - 8.dp
                    )
            )
        }

        Spacer(Modifier.height(8.dp))

        // 日期（X 轴）
        Text(
            text = date,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun buildTotalStatStatus(totalCount: Int): String {
    fun randomOf(vararg list: String) =
        list.random()

    return when {
        totalCount <= 0 -> "还没有留下任何记录"

        totalCount < 3 -> randomOf(
            "已经不是没开始，是在路上了",
            "第一次不重要，继续就好",
            "不是手滑，是开始了"
        )

        totalCount < 10 -> randomOf(
            "已经不是手滑，是习惯",
            "这已经算得上稳定输出了"
        )

        totalCount < 30 -> randomOf(
            "数据在这，不用自证",
            "已经不是一时兴起",
            "这事你是真的在做"
        )

        totalCount < 100 -> randomOf(
            "这是生活的一部分了",
            "不用提醒，你自己会来",
            "已经很难说是“记录”了"
        )

        else -> randomOf(
            "这不是习惯，这是你的一部分",
            "已经不需要证明任何东西",
            "数据只是顺手留下的痕迹"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StatisticsScreenPreview() {
    StatisticsScreen()
}