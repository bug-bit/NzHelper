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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen() {
    val context = LocalContext.current
    val sessions = remember { mutableStateListOf<Session>() }
    LaunchedEffect(Unit) {
        try {
            val loaded = SessionRepository.loadSessions(context)
            sessions.clear()
            sessions.addAll(loaded)
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果加载失败，保持空列表
            sessions.clear()
        }
    }

    val currentTime = LocalDateTime.now()
    
    // 使用 derivedStateOf 来确保只在 sessions 变化时才重新计算
    val validSessions by remember {
        derivedStateOf {
            sessions.filter { it.timestamp != null }
        }
    }
    
    val weekStats by remember {
        derivedStateOf {
            calculatePeriodStats(validSessions, PeriodType.WEEK, currentTime)
        }
    }
    val monthStats by remember {
        derivedStateOf {
            calculatePeriodStats(validSessions, PeriodType.MONTH, currentTime)
        }
    }
    val yearStats by remember {
        derivedStateOf {
            calculatePeriodStats(validSessions, PeriodType.YEAR, currentTime)
        }
    }

    val weekCount by remember {
        derivedStateOf {
            validSessions.count { session ->
                session.timestamp != null && 
                session.timestamp >= currentTime.minusDays(currentTime.dayOfWeek.value.toLong() - 1)
                    .withHour(0).withMinute(0).withSecond(0).withNano(0)
            }
        }
    }
    val monthCount by remember {
        derivedStateOf {
            validSessions.count { session ->
                session.timestamp != null &&
                session.timestamp >= currentTime.withDayOfMonth(1)
                    .withHour(0).withMinute(0).withSecond(0).withNano(0)
            }
        }
    }
    val yearCount by remember {
        derivedStateOf {
            validSessions.count { session ->
                session.timestamp != null &&
                session.timestamp >= currentTime.withDayOfYear(1)
                    .withHour(0).withMinute(0).withSecond(0).withNano(0)
            }
        }
    }

    // 计算总体统计
    val totalStats by remember {
        derivedStateOf {
            if (validSessions.isEmpty()) {
                Triple(0, 0, 0f)
            } else {
                val totalCount = validSessions.size
                val totalSeconds = validSessions.sumOf { it.duration }
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
                val statsMap = validSessions
                    .filter { it.timestamp!!.toLocalDate() >= monday }
                    .groupBy { it.timestamp!!.toLocalDate() }
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

                validSessions
                    .filter {
                        val date = it.timestamp!!.toLocalDate()
                        date >= firstDayOfMonth
                    }
                    .groupBy { it.timestamp!!.toLocalDate() }
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
                val statsMap = validSessions
                    .filter { it.timestamp!!.year == currentYear }
                    .groupBy { YearMonth.from(it.timestamp!!) }
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
            if (validSessions.isEmpty()) {
                null
            } else {
                val latest = validSessions.maxByOrNull { it.timestamp!! }
                val lastDate = latest!!.timestamp!!.toLocalDate()
                val daysAgo = ChronoUnit.DAYS.between(lastDate, LocalDateTime.now().toLocalDate())
                    
                // 计算今天（最后一次记录日期）的记录次数
                val todayCount = validSessions.count { 
                    it.timestamp?.toLocalDate() == lastDate 
                }
    
                // 主日期显示
                val displayDate = when (daysAgo) {
                    0L -> "今天"
                    1L -> "昨天"
                    else -> {
                        val dateFmt = lastDate.format(DateTimeFormatter.ofPattern("M 月 d 日"))
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
    
                // 判断是否频率过高（今天 >= 3 次）
                val isTooFrequent = daysAgo == 0L && todayCount >= 3
    
                val breakDetail = when {
                    // 频率过高时的专门提醒
                    isTooFrequent -> randomOf(
                        "今天已经很多次了，要注意休息哦～",
                        "释放太多会伤身体的，科学适度哦~",
                        "今天有点频繁了呢，早点休息吧",
                        "今天的活动就到此为止吧~",
                        "警告: 行动点耗尽!"
                    )
                    
                    // 正常今天的消息
                    daysAgo == 0L -> randomOf(
                        "今日已交作业",
                        "今天完成了释放指标",
                        "已完成今日份输出",
                        "今天没忍住，已记录~"
                    )
    
                    daysAgo == 1L -> randomOf(
                        "昨天完成了一次",
                        "昨日成功部署",
                        "昨天交过作业了",
                        "昨天有过记录"
                    )
    
                    daysAgo == 2L -> randomOf(
                        "已经鸽了 2 天",
                        "空窗 2 天中",
                        "连续摆烂 2 天"
                    )
    
                    else -> {
                        val startFmt =
                            lastDate.plusDays(1).format(DateTimeFormatter.ofPattern("M 月 d 日"))
    
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
                    breakDetail = breakDetail,
                    todayCount = todayCount
                )
            }
        }
    }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
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

                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LatestSessionCard(
                                latestInfo = latestSessionInfo,
                                modifier = Modifier.fillMaxWidth()
                            )

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
                    }

                    // 贡献热力图
                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                ContributionHeatmap(
                                    sessions = validSessions,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
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
): Pair<Int, Float> { // Pair<总时长秒数，平均单次时长分钟>
    val filtered = sessions.filter { session ->
        // 确保 timestamp 不为 null
        val timestamp = session.timestamp ?: return@filter false
        
        when (type) {
            PeriodType.WEEK -> {
                val weekStart = now.minusDays(now.dayOfWeek.value.toLong() - 1) // 本周一 00:00
                    .withHour(0).withMinute(0).withSecond(0).withNano(0)
                timestamp >= weekStart
            }

            PeriodType.MONTH -> {
                val monthStart = now.withDayOfMonth(1)
                    .withHour(0).withMinute(0).withSecond(0).withNano(0)
                timestamp >= monthStart
            }

            PeriodType.YEAR -> {
                val yearStart = now.withDayOfYear(1)
                    .withHour(0).withMinute(0).withSecond(0).withNano(0)
                timestamp >= yearStart
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

                    // 根据空窗天数和频率决定背景颜色
                    val backgroundColor = when {
                        // 今天记录次数 >= 3 次，红色背景警告
                        latestInfo.daysAgo == 0L && latestInfo.todayCount >= 3 -> 
                            MaterialTheme.colorScheme.errorContainer
                        // 今天记录次数 < 3 次，粉色背景
                        latestInfo.daysAgo == 0L -> 
                            MaterialTheme.colorScheme.primaryContainer
                        // 昨天，柔和粉色背景
                        latestInfo.daysAgo == 1L -> 
                            MaterialTheme.colorScheme.secondaryContainer
                        // 超过 1 天，粉色背景
                        else -> 
                            MaterialTheme.colorScheme.tertiaryContainer
                    }
                    
                    // 文字颜色根据背景自动适配
                    val textColor = when {
                        latestInfo.daysAgo == 0L && latestInfo.todayCount >= 3 -> 
                            MaterialTheme.colorScheme.onErrorContainer
                        latestInfo.daysAgo == 0L -> 
                            MaterialTheme.colorScheme.onPrimaryContainer
                        latestInfo.daysAgo == 1L -> 
                            MaterialTheme.colorScheme.onSecondaryContainer
                        else -> 
                            MaterialTheme.colorScheme.onTertiaryContainer
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
    val breakDetail: String,
    val todayCount: Int = 0
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(bottom = 40.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End
    ) {
        Text(
            "${maxValue.toInt()} 分钟",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "${(maxValue / 2).toInt()} 分钟",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            "0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
    }
}

// GitHub 风格的贡献热力图
@Composable
private fun ContributionHeatmap(
    sessions: List<Session>,
    modifier: Modifier = Modifier
) {
    if (sessions.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "暂无数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val now = LocalDateTime.now()
    
    // 计算固定 17 周的日期范围
    val weeksCount = 17
    val endDate = now.toLocalDate()
    val startDate = endDate.minusWeeks((weeksCount - 1).toLong()).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY))
    
    // 按日期分组统计次数
    val sessionsByDate = sessions
        .filter { it.timestamp != null }
        .groupBy { it.timestamp!!.toLocalDate() }
        .mapValues { it.value.size }
    
    // 找到最大次数用于颜色分级
    val maxCount = sessionsByDate.values.maxOrNull() ?: 0
    
    Column(
        modifier = modifier
    ) {
        // 热力图网格（7 行 x 17 列）
        val weeks = mutableListOf<List<LocalDate>>()
        var currentWeek = mutableListOf<LocalDate>()
        
        // 生成固定 17 周的日期
        var date = startDate
        repeat(weeksCount) {
            currentWeek.clear()
            for (day in 0 until 7) {
                currentWeek.add(date.plusDays(day.toLong()))
            }
            weeks.add(currentWeek.toList())
            date = date.plusWeeks(1)
        }
        
        // 使用 BoxWithConstraints 获取可用宽度，动态计算方块大小
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val availableWidth = maxWidth
            val spacing = 3.dp
            val totalSpacing = spacing * (weeksCount - 1) // 16 个间隔
            val cellWidth = (availableWidth - totalSpacing) / weeksCount
            
            // 按周显示（横向排列周，每列 7 个方块）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    weeks.forEach { week ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(spacing)
                        ) {
                            for (row in 0 until 7) {
                                if (row < week.size) {
                                    val currentDate = week[row]
                                    val count = sessionsByDate[currentDate] ?: 0
                                    val isFuture = currentDate > endDate
                                    
                                    HeatmapCell(
                                        count = count,
                                        maxCount = maxCount,
                                        isFuture = isFuture,
                                        modifier = Modifier
                                            .width(cellWidth)
                                            .height(cellWidth)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 月份标签（底部，热力图外面）
        Spacer(modifier = Modifier.height(4.dp))
        MonthLabels(weeks = weeks, weeksCount = weeksCount)
    }
}

@Composable
private fun MonthLabels(
    weeks: List<List<LocalDate>>,
    weeksCount: Int
) {
    val monthStartLabels = buildMap<Int, String> {
        weeks.forEachIndexed { index, week ->
            val firstDayOfMonth = week.firstOrNull { it.dayOfMonth == 1 } ?: return@forEachIndexed
            put(index, "${firstDayOfMonth.monthValue}月")
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val spacing = 3.dp
        val totalSpacing = spacing * (weeksCount - 1)
        val cellWidth = (maxWidth - totalSpacing) / weeksCount

        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            repeat(weeksCount) { index ->
                Box(
                    modifier = Modifier.width(cellWidth)
                ) {
                    monthStartLabels[index]?.let { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapCell(
    count: Int,
    maxCount: Int,
    isFuture: Boolean,
    modifier: Modifier = Modifier
) {
    val color = when {
        isFuture -> MaterialTheme.colorScheme.surface
        count == 0 -> MaterialTheme.colorScheme.surfaceContainerHighest
        maxCount == 0 -> MaterialTheme.colorScheme.primaryContainer
        else -> {
            val ratio = count.toFloat() / maxCount
            when {
                ratio < 0.25 -> MaterialTheme.colorScheme.primaryContainer
                ratio < 0.5 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                ratio < 0.75 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                else -> MaterialTheme.colorScheme.primary
            }
        }
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    )
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

        Box(
            modifier = Modifier
                .height(chartHeight)
                .fillMaxWidth()
        ) {

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .height(barHeight)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
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
