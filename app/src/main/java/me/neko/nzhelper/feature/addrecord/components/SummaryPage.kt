package me.neko.nzhelper.feature.addrecord.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.Contraception
import me.neko.nzhelper.core.model.PartnerGender
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.core.model.SessionFormState
import me.neko.nzhelper.core.model.SessionMode
import me.neko.nzhelper.core.util.formatTime
import me.neko.nzhelper.feature.addrecord.AddRecordFlow
import me.neko.nzhelper.ui.component.wizard.SummaryRow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
internal fun SummaryPage(
    flow: AddRecordFlow,
    formState: SessionFormState,
    elapsedSeconds: Int,
    editSession: Session?
) {
    val context = LocalContext.current
    val isPair = SessionMode.fromKey(formState.mode).isPair

    val timestamp = when (flow) {
        AddRecordFlow.EDIT -> try {
            formState.toLocalDateTime()
        } catch (_: Exception) {
            editSession?.timestamp
        }

        AddRecordFlow.MANUAL -> try {
            formState.toLocalDateTime()
        } catch (_: Exception) {
            null
        }

        AddRecordFlow.TIMER -> LocalDateTime.now()
    }
    val duration = when (flow) {
        AddRecordFlow.EDIT -> formState.manualDurationSeconds
        AddRecordFlow.MANUAL -> formState.manualDurationSeconds
        AddRecordFlow.TIMER -> elapsedSeconds
    }

    val categories = remember { TagSettings.getCategories(context) }
    val categoryName = categories.firstOrNull { it.id == formState.categoryId }?.name
        ?: formState.categoryId
    val tags = remember { TagSettings.getTags(context) }
    fun tagName(id: String): String = tags.firstOrNull { it.id == id }?.name ?: id
    val tagText = formState.tagIds
        .mapNotNull { id -> tags.firstOrNull { it.id == id }?.name }
        .joinToString("、")
        .ifBlank { "无" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryRow("模式", SessionMode.fromKey(formState.mode).label)
            if (timestamp != null) {
                SummaryRow(
                    "日期时间",
                    timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                )
            }
            SummaryRow("时长", formatTime(duration))
            SummaryRow("分类", categoryName)
            SummaryRow("标签", tagText)
            SummaryRow("评分", "%.1f".format(formState.rating))
            SummaryRow(
                "高潮",
                if (isPair) {
                    "我 ${formState.climaxCount} 次 · 对方 ${formState.partnerClimaxCount} 次"
                } else {
                    "${formState.climaxCount} 次"
                }
            )
            if (formState.locations.isNotEmpty()) {
                SummaryRow("地点", formState.locations.joinToString("、") { tagName(it) })
            }
            if (formState.moods.isNotEmpty()) {
                SummaryRow("情绪", formState.moods.joinToString("、") { tagName(it) })
            }
            if (isPair) {
                if (formState.partners.isNotEmpty()) {
                    SummaryRow("伴侣", formState.partners.joinToString("、") { tagName(it) })
                }
                if (formState.initiator.isNotBlank()) {
                    SummaryRow("发起者", formState.initiator)
                }
                SummaryRow(
                    "对方性别",
                    PartnerGender.fromKey(formState.partnerGender)?.label ?: "未设置"
                )
                if (formState.partnerName.isNotBlank()) {
                    SummaryRow("对方昵称", formState.partnerName)
                }
                if (formState.positions.isNotEmpty()) {
                    SummaryRow("体位", formState.positions.joinToString("、") { tagName(it) })
                }
                if (formState.toys.isNotEmpty()) {
                    SummaryRow("情趣玩具", formState.toys.joinToString("、") { tagName(it) })
                }
                SummaryRow("避孕措施", Contraception.fromKey(formState.contraception).label)
                if (formState.ejaculation.isNotBlank()) {
                    SummaryRow("射精方式", tagName(formState.ejaculation))
                }
            }
            if (formState.remark.isNotBlank()) {
                SummaryRow("备注", formState.remark)
            }
        }
    }
}
