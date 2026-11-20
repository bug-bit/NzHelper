package me.neko.nzhelper

import me.neko.nzhelper.feature.statistics.model.PeriodType
import me.neko.nzhelper.feature.statistics.util.isWithinPeriod
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class StatisticsPeriodTest {

    @Test
    fun weekIncludesMondayAndExcludesNextMonday() {
        val anchor = LocalDateTime.of(2026, 10, 28, 12, 0)

        assertTrue(isWithinPeriod(LocalDateTime.of(2026, 10, 26, 0, 0), anchor, PeriodType.WEEK))
        assertTrue(isWithinPeriod(LocalDateTime.of(2026, 11, 1, 23, 59), anchor, PeriodType.WEEK))
        assertFalse(isWithinPeriod(LocalDateTime.of(2026, 10, 25, 23, 59), anchor, PeriodType.WEEK))
        assertFalse(isWithinPeriod(LocalDateTime.of(2026, 11, 2, 0, 0), anchor, PeriodType.WEEK))
    }

    @Test
    fun historicalLeapMonthHasExactBoundaries() {
        val anchor = LocalDateTime.of(2024, 2, 10, 12, 0)

        assertTrue(isWithinPeriod(LocalDateTime.of(2024, 2, 1, 0, 0), anchor, PeriodType.MONTH))
        assertTrue(isWithinPeriod(LocalDateTime.of(2024, 2, 29, 23, 59), anchor, PeriodType.MONTH))
        assertFalse(isWithinPeriod(LocalDateTime.of(2024, 1, 31, 23, 59), anchor, PeriodType.MONTH))
        assertFalse(isWithinPeriod(LocalDateTime.of(2024, 3, 1, 0, 0), anchor, PeriodType.MONTH))
    }

    @Test
    fun historicalYearHasExactBoundaries() {
        val anchor = LocalDateTime.of(2023, 6, 1, 0, 0)

        assertTrue(isWithinPeriod(LocalDateTime.of(2023, 1, 1, 0, 0), anchor, PeriodType.YEAR))
        assertTrue(isWithinPeriod(LocalDateTime.of(2023, 12, 31, 23, 59), anchor, PeriodType.YEAR))
        assertFalse(isWithinPeriod(LocalDateTime.of(2022, 12, 31, 23, 59), anchor, PeriodType.YEAR))
        assertFalse(isWithinPeriod(LocalDateTime.of(2024, 1, 1, 0, 0), anchor, PeriodType.YEAR))
    }
}
