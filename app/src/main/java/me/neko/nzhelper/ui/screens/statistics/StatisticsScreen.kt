package me.neko.nzhelper.ui.screens.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import me.neko.nzhelper.data.Session
import me.neko.nzhelper.data.SessionRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(navController: NavController) {
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

    val dailyStats by remember {
        derivedStateOf {
            if (sessions.isEmpty()) emptyList()
            else sessions
                .groupBy { it.timestamp.toLocalDate() }
                .entries
                .sortedBy { it.key }
                .takeLast(30)
                .map { entry ->
                    entry.key.format(DateTimeFormatter.ofPattern("MM-dd")) to DailyStat(
                        count = entry.value.size,
                        totalDuration = entry.value.sumOf { it.duration }
                    )
                }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("统计") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (sessions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("暂无数据", style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                // 本周 + 本月
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PeriodStatCard(
                            title = "本周时长",
                            stats = weekStats,
                            modifier = Modifier.weight(1f)
                        )
                        PeriodStatCard(
                            title = "本月时长",
                            stats = monthStats,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

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

                // 【优化后】最近30天时长图（分钟）
                item {
                    Column {
                        Text(
                            "最近 30 天总时长（分钟）",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        BarChart(
                            data = dailyStats.map { it.first to (it.second.totalDuration / 60f) },
                            yAxisLabel = "分钟"
                        )
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
private fun BarChart(
    data: List<Pair<String, Float>>, // Pair<日期 "MM-dd", 值（分钟）>
    yAxisLabel: String
) {
    if (data.isEmpty()) {
        Box(modifier = Modifier.height(300.dp), contentAlignment = Alignment.Center) {
            Text("无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val maxValue = data.maxOf { it.second }.coerceAtLeast(1f)
    val barColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(bottom = 40.dp) // 底部留空间给 X 轴日期标签
    ) {
        // 左侧：Y 轴标签（顶部） + 刻度数值
        Column(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight()
        ) {
            // Y 轴标签放在顶部（水平显示）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "↑",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = yAxisLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 刻度数值（从上到下：0 → max） → 视觉上底部为0，顶部为max
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                val tickCount = 5
                // 反转顺序：i=0 时为最大值 → 改为从最大到0 反向循环
                repeat(tickCount + 1) { reverseI ->
                    val i = tickCount - reverseI  // 让 i 从 tickCount 到 0
                    val ratio = i / tickCount.toFloat()
                    val value = ratio * maxValue

                    Text(
                        text = if (value % 1 == 0f) value.toInt().toString() else "%.0f".format(
                            value
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }

        // 右侧：图表 Canvas
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 8.dp, end = 16.dp, top = 32.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val chartHeight = canvasHeight
            val chartWidth = canvasWidth

            val dataCount = data.size.coerceAtLeast(1)

            // 新增：定义柱子宽度的最小和最大值（dp 转 px）
            val minBarWidthPx = 32.dp.toPx()   // 最小宽度：避免数据多时柱子太窄
            val maxBarWidthPx = 80.dp.toPx()   // 最大宽度：避免数据少时柱子太宽

            // 计算理想宽度（原有逻辑）
            val idealBarWidth = chartWidth / dataCount * 0.7f
            val idealSpacing = chartWidth / dataCount * 0.3f

            // 限制柱子宽度在 min ~ max 之间
            val actualBarWidth = idealBarWidth.coerceIn(minBarWidthPx, maxBarWidthPx)

            // 根据实际柱子宽度重新计算间距（保证总宽度不超过 chartWidth）
            val totalBarsWidth = actualBarWidth * dataCount
            val remainingSpace = chartWidth - totalBarsWidth
            val spacing =
                if (dataCount == 1) 0f else remainingSpace / (dataCount - 1).coerceAtLeast(1)

            // 计算第一个柱子的起始 left（整体居中）
            val totalContentWidth = totalBarsWidth + spacing * (dataCount - 1)
            val startOffset = (chartWidth - totalContentWidth) / 2

            // 绘制网格线（保持不变）
            val tickCount = 5
            repeat(tickCount + 1) { i ->
                val y = chartHeight * (1 - i / tickCount.toFloat())
                drawLine(
                    color = Color.Gray.copy(alpha = 0.2f),
                    start = Offset(0f, y),
                    end = Offset(chartWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 绘制柱子和标签
            data.forEachIndexed { index, (dateLabel, value) ->
                val left = startOffset + index * (actualBarWidth + spacing)

                val barHeight = (value / maxValue) * chartHeight
                val top = chartHeight - barHeight.coerceAtLeast(8.dp.toPx())

                // 柱子（圆角）
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, top),
                    size = Size(actualBarWidth, barHeight),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )

                // 柱子上方数值
                val valueText =
                    if (value % 1 == 0f) value.toInt().toString() else "%.1f".format(value)
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = Color.Black.copy(alpha = 0.87f).toArgb()
                        textSize = 36f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    // 如果柱子太矮，数值显示在柱子内中间
                    val textY = if (barHeight < 48.dp.toPx()) {
                        top + barHeight / 2 + 12f
                    } else {
                        top - 8.dp.toPx()
                    }
                    drawText(
                        valueText,
                        left + actualBarWidth / 2,
                        textY,
                        paint
                    )
                }

                // 底部日期标签
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = Color.Black.copy(alpha = 0.6f).toArgb()
                        textSize = 32f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    drawText(
                        dateLabel,
                        left + actualBarWidth / 2,
                        chartHeight + 32.dp.toPx(),
                        paint
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StatisticsScreenPreview() {
    StatisticsScreen(navController = rememberNavController())
}