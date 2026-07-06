package me.neko.nzhelper.core.database

import android.content.Context
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.feature.statistics.model.LatestSessionInfo
import me.neko.nzhelper.feature.statistics.model.PeriodData
import me.neko.nzhelper.feature.statistics.model.PeriodOverview
import me.neko.nzhelper.feature.statistics.model.PeriodType
import me.neko.nzhelper.feature.statistics.model.TotalStats
import java.time.LocalDateTime
import me.neko.nzhelper.feature.statistics.util.calculateLatestInfo as calcLatestInfo
import me.neko.nzhelper.feature.statistics.util.calculatePeriodData as calcPeriodData
import me.neko.nzhelper.feature.statistics.util.calculatePeriodOverview as calcPeriodOverview
import me.neko.nzhelper.feature.statistics.util.isWithinPeriod as calcIsWithinPeriod

object StatisticsRepository {

    /**
     * 加载全部 Session（按时间倒序），供统计页面使用。
     */
    suspend fun loadSessions(context: Context): List<Session> =
        SessionRepository.loadSessions(context)

    fun calculatePeriodData(
        sessions: List<Session>,
        now: LocalDateTime,
        type: PeriodType
    ): PeriodData = calcPeriodData(sessions, now, type)

    fun calculatePeriodOverview(
        sessions: List<Session>,
        now: LocalDateTime,
        type: PeriodType,
        label: String
    ): PeriodOverview = calcPeriodOverview(sessions, now, type, label)

    fun calculateLatestInfo(sessions: List<Session>): LatestSessionInfo? =
        calcLatestInfo(sessions)

    /**
     * 计算总计统计（总次数 / 总时长 / 平均时长 / 周-月-年次数）。
     */
    fun calculateTotalStats(
        sessions: List<Session>,
        now: LocalDateTime
    ): TotalStats {
        if (sessions.isEmpty()) return TotalStats(0, 0, 0f, 0, 0, 0)
        val totalCount = sessions.size
        val totalSeconds = sessions.sumOf { it.duration }
        val avgMinutes =
            totalSeconds.toFloat() / (60 * totalCount)

        return TotalStats(
            totalCount = totalCount,
            totalSeconds = totalSeconds,
            avgMinutes = avgMinutes,
            weekCount = sessions.count {
                calcIsWithinPeriod(it.timestamp, now, PeriodType.WEEK)
            },
            monthCount = sessions.count {
                calcIsWithinPeriod(it.timestamp, now, PeriodType.MONTH)
            },
            yearCount = sessions.count {
                calcIsWithinPeriod(it.timestamp, now, PeriodType.YEAR)
            }
        )
    }
}
