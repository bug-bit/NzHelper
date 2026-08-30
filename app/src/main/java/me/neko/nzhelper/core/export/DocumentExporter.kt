package me.neko.nzhelper.core.export

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.neko.nzhelper.core.database.RecycleRepository
import me.neko.nzhelper.core.database.SessionRepository
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.BackupModules
import me.neko.nzhelper.core.model.RecycleBinItem
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.core.model.SessionMode
import me.neko.nzhelper.core.model.TagDef
import me.neko.nzhelper.core.model.sessionMode
import me.neko.nzhelper.feature.statistics.util.formatDuration
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

sealed class ReportBlock {
    data class Heading(val text: String, val level: Int = 1) : ReportBlock()
    data class Paragraph(val text: String, val muted: Boolean = false) : ReportBlock()
    data class Table(
        val headers: List<String>,
        val rows: List<List<String>>,
        val weights: List<Float>
    ) : ReportBlock()
}

data class ReportDocument(
    val title: String,
    val subtitle: String,
    val blocks: List<ReportBlock>
)

object DocumentExporter {

    enum class Format(val extension: String, val mimeType: String) {
        PDF("pdf", "application/pdf"),
        DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        fun fileName(timeMillis: Long = System.currentTimeMillis()): String =
            "NzHelper_Report_" +
                    SimpleDateFormat(
                        "yyyyMMdd_HHmm",
                        Locale.getDefault()
                    ).format(Date(timeMillis)) +
                    "." + extension
    }

    private val dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    private fun formatMillis(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))

    suspend fun export(
        context: Context,
        format: Format,
        modules: BackupModules
    ): ByteArray = withContext(Dispatchers.IO) {
        val report = buildReport(context, modules)
        when (format) {
            Format.PDF -> PdfReportRenderer.render(report)
            Format.DOCX -> DocxReportRenderer.render(report)
        }
    }

    internal suspend fun buildReport(
        context: Context,
        modules: BackupModules
    ): ReportDocument {
        // 记录始终加载（概览与标签使用次数统计需要），但仅在勾选时输出对应章节
        val sessions = SessionRepository.loadSessions(context)
        val recycleBin =
            if (modules.recycleBin) RecycleRepository.loadRecycleBin(context) else emptyList()
        val categories = TagSettings.getCategories(context)
        val groups = TagSettings.getGroups(context)
        val tags = TagSettings.getTags(context)

        val categoryNames = categories.associate { it.id to it.name }
        val tagNames = tags.associate { it.id to it.name }

        val blocks = mutableListOf<ReportBlock>()

        if (modules.sessions) {
            blocks += ReportBlock.Heading("概览")
            if (sessions.isEmpty()) {
                blocks += ReportBlock.Paragraph("暂无记录。", muted = true)
            } else {
                val totalSeconds = sessions.sumOf { it.duration }
                val avgMinutes = totalSeconds.toFloat() / sessions.size / 60f
                val avgRating = sessions.map { it.rating }.average()
                val oldest = sessions.minByOrNull { it.timestamp }!!.timestamp
                val newest = sessions.maxByOrNull { it.timestamp }!!.timestamp
                val maleCount = sessions.count { it.sessionMode() == SessionMode.SOLO_MALE }
                val femaleCount = sessions.count { it.sessionMode() == SessionMode.SOLO_FEMALE }
                val pairCount = sessions.count { it.sessionMode() == SessionMode.PAIR }
                blocks += ReportBlock.Table(
                    headers = listOf("指标", "数值"),
                    rows = listOf(
                        listOf("记录总数", "${sessions.size} 次"),
                        listOf(
                            "时间范围",
                            "${oldest.format(dateTimeFormat)} ~ ${newest.format(dateTimeFormat)}"
                        ),
                        listOf("总时长", formatDuration(totalSeconds)),
                        listOf("平均时长", "%.0f 分钟".format(avgMinutes)),
                        listOf("平均评分", "%.1f".format(avgRating)),
                        listOf(
                            "模式分布",
                            "男性单人 $maleCount · 女性单人 $femaleCount · 双人 $pairCount"
                        )
                    ),
                    weights = listOf(1f, 2.2f)
                )
                blocks += ReportBlock.Heading("分类统计", 2)
                blocks += categoryStatsTable(sessions, categoryNames)
            }

            blocks += ReportBlock.Heading("记录明细")
            blocks += sessionsTable(sessions, categoryNames, tagNames)
        }

        if (modules.recycleBin) {
            blocks += ReportBlock.Heading("回收站")
            if (recycleBin.isEmpty()) {
                blocks += ReportBlock.Paragraph("回收站为空。", muted = true)
            } else {
                blocks += ReportBlock.Paragraph(
                    "共 ${recycleBin.size} 条已删除记录。",
                    muted = true
                )
                blocks += recycleTable(recycleBin, categoryNames, tagNames)
            }
        }

        if (modules.taxonomy) {
            blocks += ReportBlock.Heading("标签体系")

            blocks += ReportBlock.Heading("分类", 2)
            if (categories.isEmpty()) {
                blocks += ReportBlock.Paragraph("暂无分类。", muted = true)
            } else {
                val knownIds = categories.map { it.id }.toSet()
                val usage = sessions.groupingBy { it.categoryId }.eachCount()
                val rows = buildList {
                    categories.forEach { c -> add(listOf(c.name, "${usage[c.id] ?: 0} 次")) }
                    val unknownCount = sessions.count { it.categoryId !in knownIds }
                    if (unknownCount > 0) add(listOf("（未分类）", "$unknownCount 次"))
                }
                blocks += ReportBlock.Table(
                    headers = listOf("分类", "记录数"),
                    rows = rows,
                    weights = listOf(2f, 1f)
                )
            }

            blocks += ReportBlock.Heading("标签分组", 2)
            if (groups.isEmpty() && tags.isEmpty()) {
                blocks += ReportBlock.Paragraph("暂无标签。", muted = true)
            } else {
                val tagsByGroup: Map<String, List<TagDef>> = tags.groupBy { it.groupId }
                val knownGroupIds = groups.map { it.id }.toSet()
                val rows = buildList {
                    groups.forEach { g ->
                        add(
                            listOf(
                                g.name,
                                tagsByGroup[g.id]?.joinToString("、") { it.name } ?: "—"
                            )
                        )
                    }
                    val orphanTags = tags.filter { it.groupId !in knownGroupIds }
                    if (orphanTags.isNotEmpty()) {
                        add(listOf("（未分组）", orphanTags.joinToString("、") { it.name }))
                    }
                }
                blocks += ReportBlock.Table(
                    headers = listOf("分组", "标签"),
                    rows = rows,
                    weights = listOf(1f, 3f)
                )
            }
        }

        return ReportDocument(
            title = "NzHelper 数据报告",
            subtitle = "导出时间：${
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            } · 由 NzHelper 生成",
            blocks = blocks
        )
    }

    private fun categoryStatsTable(
        sessions: List<Session>,
        categoryNames: Map<String, String>
    ): ReportBlock {
        val nameOf = { id: String -> categoryNames[id] ?: "未分类" }
        val counts = sessions.groupingBy { nameOf(it.categoryId) }.eachCount()
        val durations = sessions
            .groupBy { nameOf(it.categoryId) }
            .mapValues { e -> e.value.sumOf { it.duration } }
        val total = sessions.size
        val rows = counts.entries
            .sortedByDescending { it.value }
            .map { (name, count) ->
                listOf(
                    name,
                    "$count 次",
                    formatDuration(durations[name] ?: 0),
                    "%.1f%%".format(count * 100f / total)
                )
            }
        return ReportBlock.Table(
            headers = listOf("分类", "次数", "总时长", "占比"),
            rows = rows,
            weights = listOf(2f, 1f, 1.4f, 0.9f)
        )
    }

    private fun sessionsTable(
        sessions: List<Session>,
        categoryNames: Map<String, String>,
        tagNames: Map<String, String>
    ): ReportBlock {
        if (sessions.isEmpty()) {
            return ReportBlock.Paragraph("暂无记录。", muted = true)
        }
        val rows = sessions
            .sortedWith(compareByDescending { it.timestamp })
            .map { s ->
                sessionRow(s, categoryNames, tagNames)
            }
        return ReportBlock.Table(
            headers = listOf("日期", "模式", "时长", "评分", "高潮", "分类", "标签", "备注"),
            rows = rows,
            weights = listOf(80f, 42f, 40f, 30f, 40f, 42f, 86f, 96f)
        )
    }

    private fun recycleTable(
        recycleBin: List<RecycleBinItem>,
        categoryNames: Map<String, String>,
        tagNames: Map<String, String>
    ): ReportBlock {
        val rows = recycleBin
            .sortedByDescending { it.deletedTimestamp }
            .map { item ->
                listOf(formatMillis(item.deletedTimestamp)) +
                        sessionRow(item.session, categoryNames, tagNames)
            }
        return ReportBlock.Table(
            headers = listOf("删除时间", "日期", "模式", "时长", "评分", "高潮", "分类", "标签", "备注"),
            rows = rows,
            weights = listOf(84f, 84f, 38f, 36f, 30f, 34f, 36f, 74f, 84f)
        )
    }

    private fun sessionRow(
        s: Session,
        categoryNames: Map<String, String>,
        tagNames: Map<String, String>
    ): List<String> = listOf(
        s.timestamp.format(dateTimeFormat),
        s.sessionMode().label,
        formatDuration(s.duration),
        "%.1f".format(s.rating),
        if (s.sessionMode() == SessionMode.PAIR) {
            "${s.climaxCount}/${s.partnerClimaxCount}"
        } else {
            "${s.climaxCount}"
        },
        categoryNames[s.categoryId]?.takeIf { it.isNotBlank() } ?: "未分类",
        s.tagIds.mapNotNull { tagNames[it] }.joinToString("、").ifEmpty { "—" },
        s.remark.trim().ifEmpty { "—" }
    )
}
