package me.neko.nzhelper.feature.statistics.model

import java.time.LocalDate

enum class PeriodType { WEEK, MONTH, YEAR }

data class PeriodData(
    val totalDurationSeconds: Int,
    val avgDurationMinutes: Float,
    val chartData: List<Pair<String, Float>>
)

data class TotalStats(
    val totalCount: Int,
    val totalSeconds: Int,
    val avgMinutes: Float,
    val weekCount: Int,
    val monthCount: Int,
    val yearCount: Int
)

data class LatestSessionInfo(
    val displayDate: String,
    val time: String,
    val durationText: String,
    val daysAgo: Long,
    val detailText: String,
    val isErrorState: Boolean
)

data class TagStat(
    val id: String,
    val name: String,
    val color: String,
    val icon: String,
    val count: Int
)

data class PeriodOverview(
    val periodLabel: String,
    val count: Int,
    val totalDurationSeconds: Int,
    val longestDurationSeconds: Int,
    val longestSessionDisplayDate: String,
    val longestSessionEndTime: String,
    val avgDurationSeconds: Int,
    val avgRating: Float,
    val climaxCount: Int,
    val topTags: List<TagStat>,
    val countComparison: String = "",
    val durationComparison: String = "",
    val avgDurationComparison: String = "",
    val avgRatingComparison: String = "",
    val climaxComparison: String = "",
    val topTagsComparison: String = ""
)

data class HeatmapData(
    val weeks: List<HeatmapWeek>,
    val monthLabels: List<Pair<Int, String>>,
    val weekCount: Int,
    val maxCount: Int,
    val activeDays: Int,
    val totalDays: Int
)

data class HeatmapWeek(
    val days: List<HeatmapDay>
)

data class HeatmapDay(
    val date: LocalDate,
    val count: Int,
    val totalDurationSeconds: Int,
    val isFuture: Boolean
)

data class ActivityTimeData(
    val weekdayCounts: List<Int>,
    val weekdayMax: Int,
    val mostActiveWeekdayIndex: Int,
    val hourCounts: List<Int>,
    val hourMax: Int,
    val mostActiveHour: Int
)

data class MonthlyTrendItem(
    val label: String,
    val year: Int,
    val month: Int,
    val count: Int,
    val isCurrent: Boolean
)

data class MonthlyTrendData(
    val items: List<MonthlyTrendItem>,
    val predictedCount: Int,
    val currentActual: Int,
    val elapsedDays: Int,
    val totalDaysInMonth: Int,
    val last3MonthsAvg: Float
)

data class TagTrendItem(
    val id: String,
    val name: String,
    val color: String,
    val icon: String,
    val recentCount: Int,
    val previousCount: Int,
    val changePercent: Int,
    val trend: TagTrendDirection
)

enum class TagTrendDirection { UP, DOWN, FLAT }

data class TagTrendData(
    val items: List<TagTrendItem>,
    val windowDays: Int = 30
)