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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
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

    // 计算总体统计（用于新卡片）
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

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        topBar = {
            TopAppBar(
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
                    // 总体统计卡片
                    item {
                        TotalStatCard(
                            totalCount = totalStats.first,
                            totalSeconds = totalStats.second,
                            avgMinutes = totalStats.third,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 今年时长
                    item {
                        PeriodStatCard(
                            title = "今年时长",
                            stats = yearStats,
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
                                    .padding(bottom = 24.dp),
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
                                    .padding(bottom = 24.dp),
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

// 周期统计卡片
@Composable
private fun PeriodStatCard(
    title: String,
    stats: Pair<Int, Float>,
    modifier: Modifier = Modifier
) {
    val (totalSeconds, avgMinutes) = stats

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                formatDuration(totalSeconds),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (totalSeconds > 0) {
                Text(
                    "平均 %.1f 分钟/次".format(avgMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// 新增：总体统计卡片
@Composable
private fun TotalStatCard(
    totalCount: Int,
    totalSeconds: Int,
    avgMinutes: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("总体统计", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                formatDuration(totalSeconds),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "总次数：$totalCount 次",
                style = MaterialTheme.typography.bodyLarge
            )
            if (totalCount > 0) {
                Text(
                    "平均 %.1f 分钟/次".format(avgMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// 每日统计数据类
private data class DailyStat(
    val count: Int,
    val totalDuration: Int // 秒
)

// 纯 Compose 实现的简单条形图
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

@Preview(showBackground = true)
@Composable
fun StatisticsScreenPreview() {
    StatisticsScreen()
}