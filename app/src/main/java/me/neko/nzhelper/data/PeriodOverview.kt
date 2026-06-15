package me.neko.nzhelper.data

data class PeriodOverview(
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
    val climaxCount: Int
)