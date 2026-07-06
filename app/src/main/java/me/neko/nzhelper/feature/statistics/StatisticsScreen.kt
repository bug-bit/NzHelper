package me.neko.nzhelper.feature.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.core.database.StatisticsRepository
import me.neko.nzhelper.ui.component.chart.DonutChartCard
import me.neko.nzhelper.feature.statistics.components.EmptyStateView
import me.neko.nzhelper.ui.component.chart.HeatMapCard
import me.neko.nzhelper.feature.statistics.components.LatestSessionCard
import me.neko.nzhelper.ui.component.chart.PeriodChartCard
import me.neko.nzhelper.feature.statistics.components.PeriodOverviewDialog
import me.neko.nzhelper.feature.statistics.components.TotalStatCard
import me.neko.nzhelper.ui.component.chart.TrendChartCard
import me.neko.nzhelper.feature.statistics.model.PeriodOverview
import me.neko.nzhelper.feature.statistics.model.PeriodType
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatisticsScreen(isActive: Boolean = false) {
    val context = LocalContext.current
    val sessions = remember { mutableStateListOf<Session>() }

    LaunchedEffect(isActive) {
        if (isActive) {
            val loaded = StatisticsRepository.loadSessions(context)
            sessions.clear()
            sessions.addAll(loaded)
        }
    }

    val currentTime = LocalDateTime.now()

    val weekData by remember(sessions, currentTime) {
        derivedStateOf {
            StatisticsRepository.calculatePeriodData(sessions, currentTime, PeriodType.WEEK)
        }
    }
    val monthData by remember(sessions, currentTime) {
        derivedStateOf {
            StatisticsRepository.calculatePeriodData(sessions, currentTime, PeriodType.MONTH)
        }
    }
    val yearData by remember(sessions, currentTime) {
        derivedStateOf {
            StatisticsRepository.calculatePeriodData(sessions, currentTime, PeriodType.YEAR)
        }
    }

    val totalStats by remember(sessions, currentTime) {
        derivedStateOf { StatisticsRepository.calculateTotalStats(sessions, currentTime) }
    }

    val latestInfo by remember(sessions) {
        derivedStateOf { StatisticsRepository.calculateLatestInfo(sessions) }
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
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        LatestSessionCard(
                            latestInfo = latestInfo,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        TotalStatCard(
                            stats = totalStats,
                            sessions = sessions,
                            onPeriodClick = { type, label ->
                                selectedOverview = StatisticsRepository.calculatePeriodOverview(
                                    sessions, currentTime, type, label
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        PeriodChartCard(
                            weekData = weekData,
                            monthData = monthData,
                            yearData = yearData,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        HeatMapCard(
                            sessions = sessions,
                            currentTime = currentTime,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        TrendChartCard(
                            sessions = sessions,
                            currentTime = currentTime,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        DonutChartCard(
                            sessions = sessions,
                            currentTime = currentTime,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
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

@Preview(showBackground = true)
@Composable
fun StatisticsScreenPreview() {
    StatisticsScreen()
}