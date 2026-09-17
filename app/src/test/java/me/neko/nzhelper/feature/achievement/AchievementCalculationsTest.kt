package me.neko.nzhelper.feature.achievement

import me.neko.nzhelper.core.model.Achievement
import me.neko.nzhelper.core.model.AchievementProgress
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.core.model.SessionMode
import me.neko.nzhelper.feature.achievement.util.calculateAchievementStats
import me.neko.nzhelper.feature.achievement.util.calculateUnlockTimeline
import me.neko.nzhelper.feature.achievement.util.evaluateAchievements
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class AchievementCalculationsTest {

    private fun session(
        dateTime: LocalDateTime,
        duration: Int = 600,
        remark: String = "",
        tagIds: List<String> = emptyList(),
        mode: String = SessionMode.SOLO_MALE.key,
        climaxCount: Int = 0,
        toys: List<String> = emptyList(),
        locations: List<String> = emptyList(),
        rating: Float = 3f
    ) = Session(
        timestamp = dateTime,
        duration = duration,
        remark = remark,
        rating = rating,
        climax = false,
        tagIds = tagIds,
        mode = mode,
        climaxCount = climaxCount,
        toys = toys,
        locations = locations
    )

    private fun epochMillis(dateTime: LocalDateTime): Long =
        dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun statsAggregateRecordedFields() {
        val sessions = listOf(
            session(
                LocalDateTime.of(2026, 1, 1, 1, 30),
                duration = 900,
                remark = "备注",
                tagIds = listOf("t1"),
                climaxCount = 2
            ),
            session(
                LocalDateTime.of(2026, 1, 2, 6, 0),
                duration = 1800,
                tagIds = listOf("t1", "t2"),
                mode = SessionMode.PAIR.key,
                toys = listOf("toy"),
                locations = listOf("loc")
            ),
            session(
                LocalDateTime.of(2026, 1, 3, 12, 0),
                duration = 300,
                tagIds = listOf("t3"),
                mode = SessionMode.PAIR.key
            )
        )

        val stats = calculateAchievementStats(sessions)

        assertEquals(3, stats.totalCount)
        assertEquals(3000, stats.totalSeconds)
        assertEquals(1800, stats.longestSeconds)
        assertEquals(3, stats.maxStreakDays)
        assertEquals(3, stats.activeDays)
        assertEquals(1, stats.maxDailyCount)
        assertEquals(5, stats.distinctTagCount)
        assertEquals(1, stats.remarkCount)
        assertEquals(1, stats.nightCount)
        assertEquals(1, stats.earlyBirdCount)
        assertEquals(1, stats.weekendCount)
        assertEquals(2, stats.pairCount)
        assertEquals(2, stats.climaxCount)
        assertEquals(1, stats.toyCount)
        assertEquals(1, stats.locationCount)
    }

    @Test
    fun statsBreakStreakOnMissingDay() {
        val sessions = listOf(
            session(LocalDateTime.of(2026, 2, 1, 12, 0)),
            session(LocalDateTime.of(2026, 2, 2, 12, 0)),
            session(LocalDateTime.of(2026, 2, 5, 12, 0)),
            session(LocalDateTime.of(2026, 2, 6, 12, 0)),
            session(LocalDateTime.of(2026, 2, 7, 12, 0))
        )

        val stats = calculateAchievementStats(sessions)

        assertEquals(5, stats.activeDays)
        assertEquals(3, stats.maxStreakDays)
    }

    @Test
    fun statsCountMultipleSessionsPerDay() {
        val day = LocalDateTime.of(2026, 2, 10, 12, 0)
        val sessions = (0..3).map { session(day.plusHours(it.toLong())) }

        val stats = calculateAchievementStats(sessions)

        assertEquals(4, stats.totalCount)
        assertEquals(1, stats.activeDays)
        assertEquals(4, stats.maxDailyCount)
        assertEquals(1, stats.maxStreakDays)
    }

    @Test
    fun statsTrackHiddenAchievementMetrics() {
        val sessions = listOf(
            session(LocalDateTime.of(2025, 12, 30, 12, 0), remark = "短备注"),
            session(LocalDateTime.of(2025, 12, 31, 12, 0), mode = SessionMode.SOLO_FEMALE.key),
            session(
                LocalDateTime.of(2026, 12, 30, 12, 0),
                mode = SessionMode.PAIR.key,
                remark = "长".repeat(120),
                toys = listOf("a", "b", "c"),
                rating = 5f
            )
        )

        val stats = calculateAchievementStats(sessions)

        assertEquals(2, stats.maxMonthlyCount)
        assertEquals(365, stats.spanDays)
        assertEquals(3, stats.distinctModeCount)
        assertEquals(120, stats.longestRemarkLength)
        assertEquals(3, stats.maxToysPerSession)
        assertEquals(1, stats.fullRatingCount)
    }

    @Test
    fun hiddenAchievementsStayHiddenUntilUnlocked() {
        val locked = AchievementProgress(achievement = Achievement.HIDDEN_TOTAL_1000, current = 5)
        assertTrue(locked.achievement.hidden)
        assertFalse(locked.isRevealed)

        val unlocked = AchievementProgress(
            achievement = Achievement.HIDDEN_TOTAL_1000,
            current = 5,
            unlockedAt = 1L
        )
        assertTrue(unlocked.isRevealed)

        assertFalse(Achievement.RECORD_10.hidden)
        assertTrue(AchievementProgress(achievement = Achievement.RECORD_10, current = 0).isRevealed)
    }

    @Test
    fun hiddenSpanAchievementUnlocksOnCrossingSession() {
        val first = LocalDateTime.of(2025, 1, 1, 20, 0)
        val crossing = LocalDateTime.of(2026, 1, 1, 20, 0)
        val sessions = listOf(session(first), session(crossing))

        val timeline = calculateUnlockTimeline(sessions)

        assertEquals(epochMillis(crossing), timeline[Achievement.HIDDEN_SPAN_365])
    }

    @Test
    fun unlockTimelineUsesEarliestReachingSession() {
        val first = LocalDateTime.of(2026, 3, 10, 10, 0)
        val second = LocalDateTime.of(2026, 3, 11, 10, 0)
        val sessions = listOf(
            session(first),
            session(second),
            session(LocalDateTime.of(2026, 3, 12, 10, 0))
        )

        val timeline = calculateUnlockTimeline(sessions)

        assertEquals(epochMillis(first), timeline[Achievement.FIRST_RECORD])
        assertEquals(epochMillis(second), timeline[Achievement.STREAK_2])
        assertNull(timeline[Achievement.STREAK_7])
        assertNull(timeline[Achievement.RECORD_10])
    }

    @Test
    fun unlockTimelineIgnoresSessionOrder() {
        val first = LocalDateTime.of(2026, 4, 20, 9, 0)
        val second = LocalDateTime.of(2026, 4, 21, 9, 0)
        val sessions = listOf(
            session(LocalDateTime.of(2026, 4, 22, 9, 0)),
            session(second),
            session(first)
        )

        val timeline = calculateUnlockTimeline(sessions)

        assertEquals(epochMillis(first), timeline[Achievement.FIRST_RECORD])
        assertEquals(epochMillis(second), timeline[Achievement.STREAK_2])
    }

    @Test
    fun evaluateCapsProgressAndKeepsStoredUnlocks() {
        val sessions = (0..2).map {
            session(LocalDateTime.of(2026, 5, 1, 20, 0).plusDays(it.toLong()), duration = 600)
        }

        val progress = evaluateAchievements(
            sessions,
            mapOf(Achievement.RECORD_100.key to 1234L)
        )

        val hundred = progress.first { it.achievement == Achievement.RECORD_100 }
        assertTrue(hundred.isUnlocked)
        assertEquals(3, hundred.current)
        assertEquals(1234L, hundred.unlockedAt)

        val firstRecord = progress.first { it.achievement == Achievement.FIRST_RECORD }
        assertTrue(firstRecord.isUnlocked)
        assertEquals(1, firstRecord.current)

        val record365 = progress.first { it.achievement == Achievement.RECORD_365 }
        assertFalse(record365.isUnlocked)
        assertEquals(3, record365.current)

        assertEquals(Achievement.entries.size, progress.size)
    }

    @Test
    fun progressDerivedTextsDescribeLockedAndUnlockedState() {
        val locked = AchievementProgress(achievement = Achievement.RECORD_10, current = 7)
        assertEquals("7 / 10 次", locked.progressText)
        assertEquals("还差 3 次", locked.remainingText)
        assertNull(locked.unlockedDate)
        assertFalse(locked.isUnlocked)

        val unlocked = AchievementProgress(
            achievement = Achievement.RECORD_10,
            current = 10,
            unlockedAt = epochMillis(LocalDateTime.of(2026, 3, 11, 10, 0))
        )
        assertEquals("已解锁", unlocked.progressText)
        assertNull(unlocked.remainingText)
        assertEquals("2026-03-11", unlocked.unlockedDate)
        assertTrue(unlocked.isUnlocked)

        val hours = AchievementProgress(achievement = Achievement.TOTAL_HOUR_10, current = 5400)
        assertEquals("1.5 / 10 小时", hours.progressText)
        assertEquals("还差 8.5 小时", hours.remainingText)
    }

    @Test
    fun progressTextAndFormattingUseDisplayUnits() {
        val sessions = (0..2).map { session(LocalDateTime.of(2026, 6, 1, 20, 0)) }
        val progress = evaluateAchievements(
            sessions,
            mapOf(Achievement.FIRST_RECORD.key to 999L)
        )

        val firstRecord = progress.first { it.achievement == Achievement.FIRST_RECORD }
        assertEquals("已解锁", firstRecord.progressText)

        val hour1 = progress.first { it.achievement == Achievement.TOTAL_HOUR_1 }
        assertEquals("0.5 / 1 小时", hour1.progressText)

        assertEquals("1", Achievement.TOTAL_HOUR_1.formatValue(3600))
        assertEquals("1.5", Achievement.TOTAL_HOUR_10.formatValue(5400))
        assertEquals("15", Achievement.SINGLE_15_MIN.formatValue(900))
        assertEquals("100", Achievement.RECORD_100.formatValue(100))
    }
}
