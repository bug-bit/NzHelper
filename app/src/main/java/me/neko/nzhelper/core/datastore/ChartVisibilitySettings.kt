package me.neko.nzhelper.core.datastore

import android.content.Context
import androidx.core.content.edit

/**
 * 统计页卡片可见性与排序设置。
 */
object ChartVisibilitySettings {

    private const val PREFS = "chart_visibility_prefs"
    private const val KEY_PREFIX = "chart_visible_"
    private const val KEY_ORDER = "chart_order"

    enum class Chart(val key: String, val label: String, val description: String) {
        TOTAL_STAT("total_stat", "总计概览", "总次数、总时长、平均时长等基础指标"),
        PERIOD_DASHBOARD("period_dashboard", "周期面板", "本周/本月/本年的快速对比卡片"),
        HEATMAP("heatmap", "活动热力图", "日历热力图展示活跃天数"),
        TREND("trend", "趋势分析", "最近 12 周时长变化折线"),
        DONUT("donut", "分布统计", "标签/分组用量环形占比"),
        PERIOD_CHART("period_chart", "周期统计", "时长分布对比柱状图"),
        ACTIVITY_TIME_HEATMAP("activity_time_heatmap", "活跃时间热力图", "工作日与时段活跃度"),
        MONTHLY_TREND("monthly_trend", "月度趋势", "最近 14 个月趋势与本月预测"),
        TAG_BAR_CHART("tag_bar_chart", "标签 Top 10", "最常用标签排行"),
        TAG_COMBO("tag_combo", "高频组合", "标签之间的关联组合"),
        TAG_TREND("tag_trend", "标签趋势", "近 30 天标签变化对比"),
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun keyOf(chart: Chart): String = KEY_PREFIX + chart.key

    fun isVisible(context: Context, chart: Chart): Boolean =
        prefs(context).getBoolean(keyOf(chart), true)

    fun setVisible(context: Context, chart: Chart, visible: Boolean) {
        prefs(context).edit { putBoolean(keyOf(chart), visible) }
    }

    /** 获取按用户自定义顺序排列的图表列表。如果从未保存过，返回默认 enum 顺序。 */
    fun getOrderedCharts(context: Context): List<Chart> {
        val saved = prefs(context).getString(KEY_ORDER, null) ?: return Chart.entries.toList()
        val keyList = saved.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val lookup = Chart.entries.associateBy { it.key }
        val ordered = keyList.mapNotNull { lookup[it] }
        val missing = Chart.entries.filter { it.key !in keyList }
        return ordered + missing
    }

    /** 持久化图表顺序。 */
    fun saveOrder(context: Context, charts: List<Chart>) {
        val csv = charts.joinToString(",") { it.key }
        prefs(context).edit { putString(KEY_ORDER, csv) }
    }
}
