package me.neko.nzhelper.feature.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import me.neko.nzhelper.core.database.StatisticsRepository
import me.neko.nzhelper.core.datastore.ChartVisibilitySettings
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.feature.statistics.components.EmptyStateView
import me.neko.nzhelper.feature.statistics.components.PeriodDashboardCard
import me.neko.nzhelper.feature.statistics.components.PeriodOverviewDialog
import me.neko.nzhelper.feature.statistics.components.TotalStatCard
import me.neko.nzhelper.feature.statistics.model.PeriodData
import me.neko.nzhelper.feature.statistics.model.PeriodSelection
import me.neko.nzhelper.feature.statistics.model.PeriodType
import me.neko.nzhelper.feature.statistics.model.TotalStats
import me.neko.nzhelper.navigation.floatingBarContentBottomInset
import me.neko.nzhelper.ui.component.chart.ActivityTimeHeatmapCard
import me.neko.nzhelper.ui.component.chart.DonutChartCard
import me.neko.nzhelper.ui.component.chart.HeatMapCard
import me.neko.nzhelper.ui.component.chart.MonthlyTrendCard
import me.neko.nzhelper.ui.component.chart.PeriodChartCard
import me.neko.nzhelper.ui.component.chart.TagBarChartCard
import me.neko.nzhelper.ui.component.chart.TagComboCard
import me.neko.nzhelper.ui.component.chart.TagTrendCard
import me.neko.nzhelper.ui.component.chart.TrendChartCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatisticsScreen(
    isActive: Boolean = false,
    onOpenHistory: (PeriodSelection) -> Unit = {}
) {
    val context = LocalContext.current
    val sessions = remember { mutableStateListOf<Session>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(isActive) {
        if (isActive) {
            val loaded = StatisticsRepository.loadSessions(context)
            sessions.clear()
            sessions.addAll(loaded)
            isLoading = false
        }
    }

    val currentTime = remember { LocalDateTime.now() }

    val weekData by remember(sessions) {
        derivedStateOf {
            StatisticsRepository.calculatePeriodData(sessions, currentTime, PeriodType.WEEK)
        }
    }
    val monthData by remember(sessions) {
        derivedStateOf {
            StatisticsRepository.calculatePeriodData(sessions, currentTime, PeriodType.MONTH)
        }
    }
    val yearData by remember(sessions) {
        derivedStateOf {
            StatisticsRepository.calculatePeriodData(sessions, currentTime, PeriodType.YEAR)
        }
    }

    val totalStats by remember(sessions) {
        derivedStateOf { StatisticsRepository.calculateTotalStats(sessions, currentTime) }
    }

    var selectedPeriod by remember { mutableStateOf<PeriodSelection?>(null) }

    // 读取可见性与排序
    val orderedCharts = remember {
        ChartVisibilitySettings.getOrderedCharts(context)
    }
    val visibility by remember {
        derivedStateOf {
            ChartVisibilitySettings.Chart.entries.associateWith { chart ->
                ChartVisibilitySettings.isVisible(context, chart)
            }
        }
    }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("统计") },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
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
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (sessions.isEmpty()) {
                EmptyStateView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 16.dp + floatingBarContentBottomInset()
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    orderedCharts.forEach { chart ->
                        if (visibility[chart] != true) return@forEach
                        item(key = chart.key) {
                            ChartContent(
                                chart = chart,
                                sessions = sessions,
                                currentTime = currentTime,
                                totalStats = totalStats,
                                weekData = weekData,
                                monthData = monthData,
                                yearData = yearData,
                                onPeriodClick = { type, _ ->
                                    selectedPeriod = PeriodSelection(type, currentTime)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedPeriod?.let { selection ->
        val overview = remember(selection) {
            StatisticsRepository.calculatePeriodOverview(
                sessions = sessions,
                context = context,
                now = selection.anchor,
                type = selection.type,
                label = periodLabel(selection, currentTime)
            )
        }
        PeriodOverviewDialog(
            overview = overview,
            selection = selection,
            earliestSessionTime = sessions.minOfOrNull { it.timestamp } ?: currentTime,
            currentTime = currentTime,
            onPeriodSelected = { selectedPeriod = it },
            onShowRecords = {
                selectedPeriod = null
                onOpenHistory(selection)
            },
            onDismiss = { selectedPeriod = null }
        )
    }
}

private fun periodLabel(selection: PeriodSelection, currentTime: LocalDateTime): String = when (selection.type) {
    PeriodType.WEEK -> "本周"
    PeriodType.MONTH -> {
        if (selection.anchor.year == currentTime.year &&
            selection.anchor.monthValue == currentTime.monthValue
        ) {
            "本月"
        } else {
            "${selection.anchor.year}年${selection.anchor.monthValue}月"
        }
    }
    PeriodType.YEAR -> if (selection.anchor.year == currentTime.year) {
        "今年"
    } else {
        "${selection.anchor.year}年"
    }
}

/**
 * 根据图表枚举分发到具体卡片组件。
 */
@Composable
private fun ChartContent(
    chart: ChartVisibilitySettings.Chart,
    sessions: List<Session>,
    currentTime: LocalDateTime,
    totalStats: TotalStats,
    weekData: PeriodData,
    monthData: PeriodData,
    yearData: PeriodData,
    onPeriodClick: (PeriodType, String) -> Unit
) {
    when (chart) {
        ChartVisibilitySettings.Chart.TOTAL_STAT -> TotalStatCard(
            stats = totalStats, modifier = Modifier.fillMaxWidth()
        )

        ChartVisibilitySettings.Chart.PERIOD_DASHBOARD -> PeriodDashboardCard(
            sessions = sessions, currentTime = currentTime,
            onPeriodClick = onPeriodClick, modifier = Modifier.fillMaxWidth()
        )

        ChartVisibilitySettings.Chart.HEATMAP -> HeatMapCard(
            sessions = sessions, currentTime = currentTime, modifier = Modifier.fillMaxWidth()
        )

        ChartVisibilitySettings.Chart.TREND -> TrendChartCard(
            sessions = sessions, currentTime = currentTime, modifier = Modifier.fillMaxWidth()
        )

        ChartVisibilitySettings.Chart.DONUT -> DonutChartCard(
            sessions = sessions, currentTime = currentTime, modifier = Modifier.fillMaxWidth()
        )

        ChartVisibilitySettings.Chart.PERIOD_CHART -> PeriodChartCard(
            weekData = weekData, monthData = monthData, yearData = yearData,
            modifier = Modifier.fillMaxWidth()
        )

        ChartVisibilitySettings.Chart.ACTIVITY_TIME_HEATMAP -> ActivityTimeHeatmapCard(
            sessions = sessions, currentTime = currentTime, modifier = Modifier.fillMaxWidth()
        )

        ChartVisibilitySettings.Chart.MONTHLY_TREND -> MonthlyTrendCard(
            sessions = sessions, currentTime = currentTime, modifier = Modifier.fillMaxWidth()
        )

        ChartVisibilitySettings.Chart.TAG_BAR_CHART -> TagBarChartCard(
            sessions = sessions, currentTime = currentTime, modifier = Modifier.fillMaxWidth()
        )

        ChartVisibilitySettings.Chart.TAG_COMBO -> TagComboCard(
            sessions = sessions, currentTime = currentTime, modifier = Modifier.fillMaxWidth()
        )

        ChartVisibilitySettings.Chart.TAG_TREND -> TagTrendCard(
            sessions = sessions, currentTime = currentTime, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StatisticsScreenPreview() {
    StatisticsScreen()
}