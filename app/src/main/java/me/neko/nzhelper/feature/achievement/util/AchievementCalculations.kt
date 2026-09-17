package me.neko.nzhelper.feature.achievement.util

import me.neko.nzhelper.core.model.Achievement
import me.neko.nzhelper.core.model.AchievementProgress
import me.neko.nzhelper.core.model.AchievementStats
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.core.model.SessionMode
import me.neko.nzhelper.core.model.allTagIds
import me.neko.nzhelper.core.model.sessionMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

fun calculateAchievementStats(sessions: List<Session>): AchievementStats {
    val accumulator = AchievementStatsAccumulator()
    sessions.sortedBy { it.timestamp }.forEach(accumulator::add)
    return accumulator.toStats()
}

/**
 * 计算各成就的首次达成时间
 *
 * 按时间顺序回放记录，指标一旦达到目标即记下当时的记录时间
 */
fun calculateUnlockTimeline(sessions: List<Session>): Map<Achievement, Long> {
    val accumulator = AchievementStatsAccumulator()
    val unlocked = LinkedHashMap<Achievement, Long>()
    val pending = Achievement.entries.toMutableList()

    for (session in sessions.sortedBy { it.timestamp }) {
        accumulator.add(session)
        if (pending.isEmpty()) break
        val stats = accumulator.toStats()
        val iterator = pending.iterator()
        while (iterator.hasNext()) {
            val achievement = iterator.next()
            if (achievement.isUnlocked(stats)) {
                unlocked[achievement] = session.timestamp
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                iterator.remove()
            }
        }
    }
    return unlocked
}

fun evaluateAchievements(
    sessions: List<Session>,
    unlockedAt: Map<String, Long>
): List<AchievementProgress> {
    val stats = calculateAchievementStats(sessions)
    return Achievement.entries.map { achievement ->
        AchievementProgress(
            achievement = achievement,
            current = achievement.progress(stats),
            unlockedAt = unlockedAt[achievement.key]
        )
    }
}

private class AchievementStatsAccumulator {

    private val activeDays = HashSet<LocalDate>()
    private val dayCounts = HashMap<LocalDate, Int>()
    private val monthCounts = HashMap<YearMonth, Int>()
    private val tagIds = HashSet<String>()
    private val partnerNames = HashSet<String>()
    private val sessionModes = HashSet<String>()

    private var totalCount = 0
    private var totalSeconds = 0
    private var longestSeconds = 0
    private var maxStreakDays = 0
    private var maxDailyCount = 0
    private var remarkCount = 0
    private var nightCount = 0
    private var earlyBirdCount = 0
    private var weekendCount = 0
    private var pairCount = 0
    private var toyCount = 0
    private var locationCount = 0
    private var climaxCount = 0
    private var partnerClimaxCount = 0
    private var maxMonthlyCount = 0
    private var spanDays = 0
    private var longestRemarkLength = 0
    private var maxToysPerSession = 0
    private var fullRatingCount = 0

    private var currentStreak = 0
    private var lastActiveDay: LocalDate? = null
    private var firstActiveDay: LocalDate? = null

    fun add(raw: Session) {
        val session = raw.normalized()
        val duration = session.duration.coerceAtLeast(0)
        totalCount++
        totalSeconds += duration
        if (duration > longestSeconds) longestSeconds = duration
        climaxCount += session.climaxCount
        partnerClimaxCount += session.partnerClimaxCount
        if (session.remark.isNotBlank()) remarkCount++
        if (session.remark.length > longestRemarkLength) longestRemarkLength = session.remark.length
        if (session.toys.isNotEmpty()) toyCount++
        if (session.toys.size > maxToysPerSession) maxToysPerSession = session.toys.size
        if (session.locations.isNotEmpty()) locationCount++
        if (session.rating >= FULL_RATING) fullRatingCount++
        if (session.sessionMode() == SessionMode.PAIR) pairCount++
        session.partners.forEach { if (it.isNotBlank()) partnerNames += it }
        if (session.partnerName.isNotBlank()) partnerNames += session.partnerName
        sessionModes += session.sessionMode().key
        tagIds += session.allTagIds()

        val timestamp = session.timestamp
        when (timestamp.hour) {
            in 0..4 -> nightCount++
            in 5..7 -> earlyBirdCount++
        }
        if (timestamp.dayOfWeek == DayOfWeek.SATURDAY ||
            timestamp.dayOfWeek == DayOfWeek.SUNDAY
        ) {
            weekendCount++
        }

        val day = timestamp.toLocalDate()
        activeDays += day
        val dayCount = (dayCounts[day] ?: 0) + 1
        dayCounts[day] = dayCount
        if (dayCount > maxDailyCount) maxDailyCount = dayCount

        val month = YearMonth.from(day)
        val monthCount = (monthCounts[month] ?: 0) + 1
        monthCounts[month] = monthCount
        if (monthCount > maxMonthlyCount) maxMonthlyCount = monthCount

        val first = firstActiveDay
        if (first == null) {
            firstActiveDay = day
        } else {
            val span = ChronoUnit.DAYS.between(first, day).toInt()
            if (span > spanDays) spanDays = span
        }

        val previous = lastActiveDay
        currentStreak = when {
            previous == null -> 1
            day == previous -> currentStreak
            day == previous.plusDays(1) -> currentStreak + 1
            day.isAfter(previous) -> 1
            else -> currentStreak
        }
        if (currentStreak > maxStreakDays) maxStreakDays = currentStreak
        if (previous == null || day.isAfter(previous)) lastActiveDay = day
    }

    fun toStats(): AchievementStats = AchievementStats(
        totalCount = totalCount,
        totalSeconds = totalSeconds,
        longestSeconds = longestSeconds,
        maxStreakDays = maxStreakDays,
        activeDays = activeDays.size,
        maxDailyCount = maxDailyCount,
        distinctTagCount = tagIds.size,
        remarkCount = remarkCount,
        nightCount = nightCount,
        earlyBirdCount = earlyBirdCount,
        weekendCount = weekendCount,
        pairCount = pairCount,
        distinctPartnerCount = partnerNames.size,
        toyCount = toyCount,
        locationCount = locationCount,
        climaxCount = climaxCount,
        partnerClimaxCount = partnerClimaxCount,
        maxMonthlyCount = maxMonthlyCount,
        spanDays = spanDays,
        distinctModeCount = sessionModes.size,
        longestRemarkLength = longestRemarkLength,
        maxToysPerSession = maxToysPerSession,
        fullRatingCount = fullRatingCount
    )
}

private const val FULL_RATING = 5f
