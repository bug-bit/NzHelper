package me.neko.nzhelper.feature.mine.model

import androidx.compose.runtime.Immutable

@Immutable
data class ProfileSummary(
    val totalCount: Int = 0,
    val totalSeconds: Int = 0,
    val companionDays: Int = 0
)
