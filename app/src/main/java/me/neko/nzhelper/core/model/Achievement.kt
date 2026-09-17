package me.neko.nzhelper.core.model

import androidx.compose.runtime.Immutable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class AchievementCategory(val label: String) {
    MILESTONE("里程碑"),
    DURATION("时长"),
    STREAK("坚持"),
    TIME("时段"),
    VARIETY("多样性"),
    PAIR("双人")
}

enum class AchievementTier(val label: String) {
    BRONZE("铜"),
    SILVER("银"),
    GOLD("金"),
    DIAMOND("钻")
}

/**
 * 由记录数据聚合出的成就指标。
 *
 * 每个字段都是「截至当前前缀」的单调递增统计量，因此既可以用于展示进度，
 * 也可以在按时间回放记录时判定成就的首次达成时刻。
 */
@Immutable
data class AchievementStats(
    val totalCount: Int = 0,
    val totalSeconds: Int = 0,
    val longestSeconds: Int = 0,
    val maxStreakDays: Int = 0,
    val activeDays: Int = 0,
    val maxDailyCount: Int = 0,
    val distinctTagCount: Int = 0,
    val remarkCount: Int = 0,
    val nightCount: Int = 0,
    val earlyBirdCount: Int = 0,
    val weekendCount: Int = 0,
    val pairCount: Int = 0,
    val distinctPartnerCount: Int = 0,
    val toyCount: Int = 0,
    val locationCount: Int = 0,
    val climaxCount: Int = 0,
    val partnerClimaxCount: Int = 0,
    val maxMonthlyCount: Int = 0,
    val spanDays: Int = 0,
    val distinctModeCount: Int = 0,
    val longestRemarkLength: Int = 0,
    val maxToysPerSession: Int = 0,
    val fullRatingCount: Int = 0
)

@Immutable
data class AchievementProgress(
    val achievement: Achievement,
    val current: Int,
    val unlockedAt: Long? = null
) {
    val target: Int get() = achievement.target

    val isUnlocked: Boolean get() = unlockedAt != null || current >= target

    /** 隐藏成就在解锁前不出现在列表与统计中。 */
    val isRevealed: Boolean get() = !achievement.hidden || isUnlocked

    val fraction: Float
        get() = if (target <= 0) 1f else (current.toFloat() / target).coerceIn(0f, 1f)

    val progressText: String
        get() = if (isUnlocked) {
            "已解锁"
        } else {
            "${achievement.formatValue(current)} / ${achievement.formatValue(target)} ${achievement.unit}"
        }

    val unlockedDate: String?
        get() = unlockedAt?.let { millis ->
            Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(UnlockDateFormatter)
        }

    val remainingText: String?
        get() = if (isUnlocked) {
            null
        } else {
            "还差 ${achievement.formatValue((target - current).coerceAtLeast(0))} ${achievement.unit}"
        }
}

private val UnlockDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

enum class Achievement(
    val key: String,
    val title: String,
    val description: String,
    val category: AchievementCategory,
    val tier: AchievementTier,
    val target: Int,
    val unit: String,
    val displayDivisor: Int,
    private val metric: (AchievementStats) -> Int,
    val hidden: Boolean = false
) {
    // ── 里程碑 ──
    FIRST_RECORD(
        key = "first_record",
        title = "初次记录",
        description = "完成第一次记录",
        category = AchievementCategory.MILESTONE,
        tier = AchievementTier.BRONZE,
        target = 1,
        unit = "次",
        displayDivisor = 1,
        metric = { it.totalCount }
    ),
    RECORD_10(
        key = "record_10",
        title = "小有积累",
        description = "累计记录 10 次",
        category = AchievementCategory.MILESTONE,
        tier = AchievementTier.BRONZE,
        target = 10,
        unit = "次",
        displayDivisor = 1,
        metric = { it.totalCount }
    ),
    RECORD_50(
        key = "record_50",
        title = "渐入佳境",
        description = "累计记录 50 次",
        category = AchievementCategory.MILESTONE,
        tier = AchievementTier.SILVER,
        target = 50,
        unit = "次",
        displayDivisor = 1,
        metric = { it.totalCount }
    ),
    RECORD_100(
        key = "record_100",
        title = "百次纪念",
        description = "累计记录 100 次",
        category = AchievementCategory.MILESTONE,
        tier = AchievementTier.GOLD,
        target = 100,
        unit = "次",
        displayDivisor = 1,
        metric = { it.totalCount }
    ),
    RECORD_365(
        key = "record_365",
        title = "一年之约",
        description = "累计记录 365 次",
        category = AchievementCategory.MILESTONE,
        tier = AchievementTier.DIAMOND,
        target = 365,
        unit = "次",
        displayDivisor = 1,
        metric = { it.totalCount }
    ),

    // ── 时长 ──
    TOTAL_HOUR_1(
        key = "total_hour_1",
        title = "初露锋芒",
        description = "累计时长达到 1 小时",
        category = AchievementCategory.DURATION,
        tier = AchievementTier.BRONZE,
        target = 3600,
        unit = "小时",
        displayDivisor = 3600,
        metric = { it.totalSeconds }
    ),
    TOTAL_HOUR_10(
        key = "total_hour_10",
        title = "时光沉淀",
        description = "累计时长达到 10 小时",
        category = AchievementCategory.DURATION,
        tier = AchievementTier.SILVER,
        target = 36000,
        unit = "小时",
        displayDivisor = 3600,
        metric = { it.totalSeconds }
    ),
    TOTAL_HOUR_50(
        key = "total_hour_50",
        title = "漫漫长路",
        description = "累计时长达到 50 小时",
        category = AchievementCategory.DURATION,
        tier = AchievementTier.GOLD,
        target = 180000,
        unit = "小时",
        displayDivisor = 3600,
        metric = { it.totalSeconds }
    ),
    TOTAL_HOUR_100(
        key = "total_hour_100",
        title = "百时之旅",
        description = "累计时长达到 100 小时",
        category = AchievementCategory.DURATION,
        tier = AchievementTier.DIAMOND,
        target = 360000,
        unit = "小时",
        displayDivisor = 3600,
        metric = { it.totalSeconds }
    ),
    SINGLE_15_MIN(
        key = "single_15_min",
        title = "小试身手",
        description = "单次时长达到 15 分钟",
        category = AchievementCategory.DURATION,
        tier = AchievementTier.BRONZE,
        target = 900,
        unit = "分钟",
        displayDivisor = 60,
        metric = { it.longestSeconds }
    ),
    SINGLE_30_MIN(
        key = "single_30_min",
        title = "游刃有余",
        description = "单次时长达到 30 分钟",
        category = AchievementCategory.DURATION,
        tier = AchievementTier.SILVER,
        target = 1800,
        unit = "分钟",
        displayDivisor = 60,
        metric = { it.longestSeconds }
    ),
    SINGLE_60_MIN(
        key = "single_60_min",
        title = "耐力马拉松",
        description = "单次时长达到 60 分钟",
        category = AchievementCategory.DURATION,
        tier = AchievementTier.GOLD,
        target = 3600,
        unit = "分钟",
        displayDivisor = 60,
        metric = { it.longestSeconds }
    ),

    // ── 坚持 ──
    STREAK_2(
        key = "streak_2",
        title = "连续两天",
        description = "连续 2 天有记录",
        category = AchievementCategory.STREAK,
        tier = AchievementTier.BRONZE,
        target = 2,
        unit = "天",
        displayDivisor = 1,
        metric = { it.maxStreakDays }
    ),
    STREAK_7(
        key = "streak_7",
        title = "一周坚持",
        description = "连续 7 天有记录",
        category = AchievementCategory.STREAK,
        tier = AchievementTier.SILVER,
        target = 7,
        unit = "天",
        displayDivisor = 1,
        metric = { it.maxStreakDays }
    ),
    STREAK_30(
        key = "streak_30",
        title = "月度全勤",
        description = "连续 30 天有记录",
        category = AchievementCategory.STREAK,
        tier = AchievementTier.GOLD,
        target = 30,
        unit = "天",
        displayDivisor = 1,
        metric = { it.maxStreakDays }
    ),
    ACTIVE_10(
        key = "active_10",
        title = "活跃十天",
        description = "累计 10 天有记录",
        category = AchievementCategory.STREAK,
        tier = AchievementTier.BRONZE,
        target = 10,
        unit = "天",
        displayDivisor = 1,
        metric = { it.activeDays }
    ),
    ACTIVE_100(
        key = "active_100",
        title = "百日陪伴",
        description = "累计 100 天有记录",
        category = AchievementCategory.STREAK,
        tier = AchievementTier.GOLD,
        target = 100,
        unit = "天",
        displayDivisor = 1,
        metric = { it.activeDays }
    ),
    DAILY_3(
        key = "daily_3",
        title = "一日三记",
        description = "同一天记录 3 次",
        category = AchievementCategory.STREAK,
        tier = AchievementTier.SILVER,
        target = 3,
        unit = "次",
        displayDivisor = 1,
        metric = { it.maxDailyCount }
    ),

    // ── 时段 ──
    NIGHT_OWL_10(
        key = "night_owl_10",
        title = "夜猫子",
        description = "在凌晨 0-5 点记录 10 次",
        category = AchievementCategory.TIME,
        tier = AchievementTier.SILVER,
        target = 10,
        unit = "次",
        displayDivisor = 1,
        metric = { it.nightCount }
    ),
    EARLY_BIRD_10(
        key = "early_bird_10",
        title = "早起鸟",
        description = "在清晨 5-8 点记录 10 次",
        category = AchievementCategory.TIME,
        tier = AchievementTier.SILVER,
        target = 10,
        unit = "次",
        displayDivisor = 1,
        metric = { it.earlyBirdCount }
    ),
    WEEKEND_30(
        key = "weekend_30",
        title = "周末时光",
        description = "在周末记录 30 次",
        category = AchievementCategory.TIME,
        tier = AchievementTier.BRONZE,
        target = 30,
        unit = "次",
        displayDivisor = 1,
        metric = { it.weekendCount }
    ),

    // ── 多样性 ──
    TAG_5(
        key = "tag_5",
        title = "标签新手",
        description = "使用过 5 个不同标签",
        category = AchievementCategory.VARIETY,
        tier = AchievementTier.BRONZE,
        target = 5,
        unit = "个",
        displayDivisor = 1,
        metric = { it.distinctTagCount }
    ),
    TAG_15(
        key = "tag_15",
        title = "标签达人",
        description = "使用过 15 个不同标签",
        category = AchievementCategory.VARIETY,
        tier = AchievementTier.SILVER,
        target = 15,
        unit = "个",
        displayDivisor = 1,
        metric = { it.distinctTagCount }
    ),
    TAG_30(
        key = "tag_30",
        title = "标签收藏家",
        description = "使用过 30 个不同标签",
        category = AchievementCategory.VARIETY,
        tier = AchievementTier.GOLD,
        target = 30,
        unit = "个",
        displayDivisor = 1,
        metric = { it.distinctTagCount }
    ),
    REMARK_20(
        key = "remark_20",
        title = "记录习惯",
        description = "写下 20 条备注",
        category = AchievementCategory.VARIETY,
        tier = AchievementTier.BRONZE,
        target = 20,
        unit = "条",
        displayDivisor = 1,
        metric = { it.remarkCount }
    ),
    REMARK_100(
        key = "remark_100",
        title = "笔耕不辍",
        description = "写下 100 条备注",
        category = AchievementCategory.VARIETY,
        tier = AchievementTier.GOLD,
        target = 100,
        unit = "条",
        displayDivisor = 1,
        metric = { it.remarkCount }
    ),
    CLIMAX_50(
        key = "climax_50",
        title = "巅峰时刻",
        description = "累计高潮 50 次",
        category = AchievementCategory.VARIETY,
        tier = AchievementTier.SILVER,
        target = 50,
        unit = "次",
        displayDivisor = 1,
        metric = { it.climaxCount }
    ),

    // ── 双人 ──
    PAIR_10(
        key = "pair_10",
        title = "双人世界",
        description = "双人记录 10 次",
        category = AchievementCategory.PAIR,
        tier = AchievementTier.SILVER,
        target = 10,
        unit = "次",
        displayDivisor = 1,
        metric = { it.pairCount }
    ),
    PAIR_50(
        key = "pair_50",
        title = "默契搭档",
        description = "双人记录 50 次",
        category = AchievementCategory.PAIR,
        tier = AchievementTier.GOLD,
        target = 50,
        unit = "次",
        displayDivisor = 1,
        metric = { it.pairCount }
    ),
    PARTNER_3(
        key = "partner_3",
        title = "同伴名录",
        description = "记录中出现过 3 位伴侣",
        category = AchievementCategory.PAIR,
        tier = AchievementTier.BRONZE,
        target = 3,
        unit = "位",
        displayDivisor = 1,
        metric = { it.distinctPartnerCount }
    ),
    TOY_10(
        key = "toy_10",
        title = "情趣探索",
        description = "使用道具记录 10 次",
        category = AchievementCategory.PAIR,
        tier = AchievementTier.SILVER,
        target = 10,
        unit = "次",
        displayDivisor = 1,
        metric = { it.toyCount }
    ),
    LOCATION_10(
        key = "location_10",
        title = "场景探索",
        description = "记录地点 10 次",
        category = AchievementCategory.PAIR,
        tier = AchievementTier.BRONZE,
        target = 10,
        unit = "次",
        displayDivisor = 1,
        metric = { it.locationCount }
    ),
    PARTNER_CLIMAX_50(
        key = "partner_climax_50",
        title = "体贴入微",
        description = "伴侣高潮累计 50 次",
        category = AchievementCategory.PAIR,
        tier = AchievementTier.GOLD,
        target = 50,
        unit = "次",
        displayDivisor = 1,
        metric = { it.partnerClimaxCount }
    ),

    // ── 隐藏成就（解锁前不出现在列表与统计中）──
    // 归属分类仍由 category 决定，达成后会出现在对应分组里。
    HIDDEN_ALL_MODES(
        key = "hidden_all_modes",
        title = "三栖玩家",
        description = "三种记录模式都留下过记录",
        category = AchievementCategory.VARIETY,
        tier = AchievementTier.SILVER,
        target = 3,
        unit = "种",
        displayDivisor = 1,
        metric = { it.distinctModeCount },
        hidden = true
    ),
    HIDDEN_LONG_REMARK(
        key = "hidden_long_remark",
        title = "长篇大论",
        description = "写下一条 100 字以上的备注",
        category = AchievementCategory.VARIETY,
        tier = AchievementTier.BRONZE,
        target = 100,
        unit = "字",
        displayDivisor = 1,
        metric = { it.longestRemarkLength },
        hidden = true
    ),
    HIDDEN_FULL_RATING(
        key = "hidden_full_rating",
        title = "满分体验",
        description = "10 次记录打满 5 分",
        category = AchievementCategory.VARIETY,
        tier = AchievementTier.SILVER,
        target = 10,
        unit = "次",
        displayDivisor = 1,
        metric = { it.fullRatingCount },
        hidden = true
    ),
    HIDDEN_DAILY_5(
        key = "hidden_daily_5",
        title = "一日五连",
        description = "同一天记录 5 次",
        category = AchievementCategory.STREAK,
        tier = AchievementTier.SILVER,
        target = 5,
        unit = "次",
        displayDivisor = 1,
        metric = { it.maxDailyCount },
        hidden = true
    ),
    HIDDEN_MONTH_30(
        key = "hidden_month_30",
        title = "月度狂欢",
        description = "单月记录 30 次",
        category = AchievementCategory.STREAK,
        tier = AchievementTier.GOLD,
        target = 30,
        unit = "次",
        displayDivisor = 1,
        metric = { it.maxMonthlyCount },
        hidden = true
    ),
    HIDDEN_STREAK_100(
        key = "hidden_streak_100",
        title = "百日之约",
        description = "连续 100 天有记录",
        category = AchievementCategory.STREAK,
        tier = AchievementTier.DIAMOND,
        target = 100,
        unit = "天",
        displayDivisor = 1,
        metric = { it.maxStreakDays },
        hidden = true
    ),
    HIDDEN_SPAN_365(
        key = "hidden_span_365",
        title = "跨越四季",
        description = "记录的时间跨度达到 365 天",
        category = AchievementCategory.MILESTONE,
        tier = AchievementTier.GOLD,
        target = 365,
        unit = "天",
        displayDivisor = 1,
        metric = { it.spanDays },
        hidden = true
    ),
    HIDDEN_TOTAL_1000(
        key = "hidden_total_1000",
        title = "千次纪念",
        description = "累计记录 1000 次",
        category = AchievementCategory.MILESTONE,
        tier = AchievementTier.DIAMOND,
        target = 1000,
        unit = "次",
        displayDivisor = 1,
        metric = { it.totalCount },
        hidden = true
    ),
    HIDDEN_MARATHON_120(
        key = "hidden_marathon_120",
        title = "超长待机",
        description = "单次时长达到 120 分钟",
        category = AchievementCategory.DURATION,
        tier = AchievementTier.GOLD,
        target = 7200,
        unit = "分钟",
        displayDivisor = 60,
        metric = { it.longestSeconds },
        hidden = true
    ),
    HIDDEN_TOYS_3(
        key = "hidden_toys_3",
        title = "装备齐全",
        description = "单次记录使用 3 个以上道具",
        category = AchievementCategory.PAIR,
        tier = AchievementTier.SILVER,
        target = 3,
        unit = "个",
        displayDivisor = 1,
        metric = { it.maxToysPerSession },
        hidden = true
    );

    fun currentProgress(stats: AchievementStats): Int = metric(stats).coerceAtLeast(0)

    fun isUnlocked(stats: AchievementStats): Boolean = currentProgress(stats) >= target

    fun progress(stats: AchievementStats): Int = currentProgress(stats).coerceAtMost(target)

    fun formatValue(raw: Int): String {
        if (displayDivisor <= 1) return raw.toString()
        val scaled = raw.toFloat() / displayDivisor
        if (scaled >= 10f || scaled % 1f == 0f) return scaled.toInt().toString()
        return String.format(Locale.US, "%.1f", scaled)
    }

    companion object {
        fun fromKey(key: String?): Achievement? = entries.firstOrNull { it.key == key }
    }
}
