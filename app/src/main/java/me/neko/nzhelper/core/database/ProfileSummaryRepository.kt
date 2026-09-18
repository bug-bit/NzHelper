package me.neko.nzhelper.core.database

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import me.neko.nzhelper.feature.mine.model.ProfileSummary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

object ProfileSummaryRepository {

    private val _summary = MutableStateFlow(ProfileSummary())
    val summary: StateFlow<ProfileSummary> = _summary.asStateFlow()

    suspend fun refresh(context: Context) = withContext(Dispatchers.IO) {
        val row = AppDatabase.get(context).sessionDao().summary()
        val firstDate = row.firstTimestampIso?.let { iso ->
            runCatching { LocalDateTime.parse(iso).toLocalDate() }.getOrNull()
        }
        _summary.value = ProfileSummary(
            totalCount = row.recordCount,
            totalSeconds = row.totalSeconds,
            companionDays = firstDate
                ?.let { ChronoUnit.DAYS.between(it, LocalDate.now()).toInt().coerceAtLeast(0) }
                ?: 0
        )
    }
}
