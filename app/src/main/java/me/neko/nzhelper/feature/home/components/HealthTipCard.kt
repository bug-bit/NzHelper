package me.neko.nzhelper.feature.home.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.ai.AiUsage
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.core.model.SessionMode
import me.neko.nzhelper.core.model.sessionMode
import java.time.LocalDateTime

enum class TipType { PRAISE, REMINDER, INFO }

data class HealthTip(
    val message: String,
    val type: TipType
)

@Composable
fun HealthTipCard(
    modifier: Modifier = Modifier,
    tip: HealthTip? = null,
    aiTip: String? = null,
    aiLoading: Boolean = false,
    errorText: String? = null,
    usage: AiUsage? = null,
    onRefreshAi: (() -> Unit)? = null
) {
    val isAi = onRefreshAi != null
    val context = LocalContext.current
    val message = when {
        aiTip != null -> aiTip
        isAi -> "点击刷新获取 AI 建议"
        else -> tip?.message ?: ""
    }
    val color = if (isAi) {
        MaterialTheme.colorScheme.tertiary
    } else when (tip?.type) {
        TipType.PRAISE -> MaterialTheme.colorScheme.primary
        TipType.REMINDER -> MaterialTheme.colorScheme.tertiary
        TipType.INFO -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isAi) Icons.Outlined.AutoAwesome else Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (isAi) "AI 健康建议" else "健康小贴士",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = color,
                    modifier = Modifier.weight(1f)
                )
                if (isAi) {
                    Box(
                        modifier = Modifier.size(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (aiLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = color
                            )
                        } else {
                            IconButton(
                                onClick = onRefreshAi,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Refresh,
                                    "刷新 AI 分析",
                                    tint = color,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    if (errorText != null) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("AI Error", errorText))
                                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Outlined.ContentCopy,
                                "复制错误信息",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.size(8.dp))
            if (isAi && aiLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = color
                    )
                    Text(
                        "AI 分析中...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isAi && aiTip == null) MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = 0.6f
                    )
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isAi && usage != null && aiTip != null) {
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = "↑${usage.inputTokens ?: "?"} ↓${usage.outputTokens ?: "?"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
            }
        }
    }
}

fun analyzeHealthTip(sessions: List<Session>): HealthTip? {
    val now = LocalDateTime.now()
    val weekAgo = now.minusDays(7)
    val recent = sessions.filter { !it.timestamp.isBefore(weekAgo) && !it.timestamp.isAfter(now) }

    if (sessions.isEmpty()) return null
    if (recent.isEmpty()) {
        return HealthTip(
            message = "最近一周还没有记录，偶尔放松一下也很正常，别给自己太大压力～",
            type = TipType.INFO
        )
    }

    val count = recent.size
    val daysWithRecords = recent.map { it.timestamp.toLocalDate() }.distinct().size

    val lateNightCount = recent.count { it.timestamp.hour >= 23 }
    val lateNightRatio = lateNightCount.toFloat() / count

    val sorted = recent.sortedBy { it.timestamp }
    var maxGapDays = 0L
    for (i in 1 until sorted.size) {
        val gap = sorted[i].timestamp.toLocalDate().toEpochDay() -
                sorted[i - 1].timestamp.toLocalDate().toEpochDay()
        if (gap > maxGapDays) maxGapDays = gap
    }

    // 按最近一条记录的模式选择文案（混合历史取最新倾向）
    val mode = sorted.lastOrNull()?.sessionMode() ?: SessionMode.SOLO_MALE

    return when (mode) {
        SessionMode.SOLO_FEMALE -> femaleTip(count, daysWithRecords, lateNightRatio, maxGapDays)
        SessionMode.PAIR -> pairTip(recent, count, daysWithRecords, lateNightRatio, maxGapDays)
        SessionMode.SOLO_MALE -> maleTip(count, daysWithRecords, lateNightRatio, maxGapDays)
    }
}

private fun maleTip(
    count: Int,
    daysWithRecords: Int,
    lateNightRatio: Float,
    maxGapDays: Long
): HealthTip = when {
    count >= 8 -> HealthTip(
        "最近一周频率偏高（$count 次），频繁可能会影响精力和专注力，试试延长间隔、多休息～",
        TipType.REMINDER
    )

    count >= 6 -> HealthTip(
        "这周频率偏密，记得多补水、适当休息，身体是革命的本钱 💪",
        TipType.REMINDER
    )

    lateNightRatio > 0.5f -> HealthTip(
        "最近深夜时段偏多，睡眠不足会影响第二天的状态，试着早点休息吧 🌙",
        TipType.REMINDER
    )

    maxGapDays >= 5 -> HealthTip(
        "距离上次记录已经好几天了，别忘了偶尔释放一下压力，放松心情～",
        TipType.INFO
    )

    count >= 3 -> HealthTip(
        "频率适中（$daysWithRecords 天 $count 次），生活工作两不误，继续保持～",
        TipType.PRAISE
    )

    count >= 2 -> HealthTip(
        "节奏舒缓，松弛有度最自在，想记录的时候就来吧～",
        TipType.PRAISE
    )

    else -> HealthTip(
        "这周只记录了 $count 次，频率偏低很正常，别给自己压力，顺其自然就好～",
        TipType.INFO
    )
}

private fun femaleTip(
    count: Int,
    daysWithRecords: Int,
    lateNightRatio: Float,
    maxGapDays: Long
): HealthTip = when {
    count >= 8 -> HealthTip(
        "最近一周频率偏高（$count 次），多关注身体的真实感受，注意休息和卫生护理～",
        TipType.REMINDER
    )

    count >= 6 -> HealthTip(
        "这周频率偏密，记得多补水、适当休息，别透支身体～",
        TipType.REMINDER
    )

    lateNightRatio > 0.5f -> HealthTip(
        "最近深夜时段偏多，睡眠不足会影响第二天的状态，试着早点休息吧 🌙",
        TipType.REMINDER
    )

    maxGapDays >= 5 -> HealthTip(
        "距离上次记录已经好几天了，顺其自然，想放松的时候再来～",
        TipType.INFO
    )

    count >= 3 -> HealthTip(
        "频率适中（$daysWithRecords 天 $count 次），张弛有度，状态很不错～",
        TipType.PRAISE
    )

    count >= 2 -> HealthTip(
        "节奏舒缓，取悦自己最自在～",
        TipType.PRAISE
    )

    else -> HealthTip(
        "这周只记录了 $count 次，频率高低都正常，跟随自己的感受就好～",
        TipType.INFO
    )
}

private fun pairTip(
    recent: List<Session>,
    count: Int,
    daysWithRecords: Int,
    lateNightRatio: Float,
    maxGapDays: Long
): HealthTip {
    val pairSessions = recent.filter { it.sessionMode() == SessionMode.PAIR }
    val partnerCareNeeded = pairSessions.isNotEmpty() &&
            pairSessions.count { it.partnerClimaxCount > 0 } < pairSessions.size / 2

    return when {
        count >= 8 -> HealthTip(
            "最近一周亲密频率偏高（$count 次），注意休息，双方舒适最重要～",
            TipType.REMINDER
        )

        count >= 6 -> HealthTip(
            "这周甜蜜偏密，记得多休息，为彼此保存体力 💞",
            TipType.REMINDER
        )

        lateNightRatio > 0.5f -> HealthTip(
            "最近深夜时段偏多，睡眠不足会影响状态，试着早点休息吧 🌙",
            TipType.REMINDER
        )

        partnerCareNeeded -> HealthTip(
            "最近的亲密时光里，对方的体验值得更多关注，多些前戏和沟通会让彼此更尽兴～",
            TipType.REMINDER
        )

        maxGapDays >= 5 -> HealthTip(
            "距离上次亲密已经好几天了，来点小浪漫也不错～",
            TipType.INFO
        )

        count >= 3 -> HealthTip(
            "频率适中（$daysWithRecords 天 $count 次），感情与生活两不误，继续保持～",
            TipType.PRAISE
        )

        count >= 2 -> HealthTip(
            "节奏舒缓，彼此舒适最重要，享受过程就好～",
            TipType.PRAISE
        )

        else -> HealthTip(
            "这周只亲密了 $count 次，频率高低都正常，感情比数字更重要～",
            TipType.INFO
        )
    }
}
