package me.neko.nzhelper.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailsDialog(
    show: Boolean,
    remark: String,
    onRemarkChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    watchedMovie: Boolean,
    onWatchedMovieChange: (Boolean) -> Unit,
    climax: Boolean,
    onClimaxChange: (Boolean) -> Unit,
    props: String,
    onPropsChange: (String) -> Unit,
    rating: Float,
    onRatingChange: (Float) -> Unit,
    mood: String,
    onMoodChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("填写本次信息") },
        text = {
            val scrollState = rememberScrollState()
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // 备注
                Text("备注（可选）")
                Spacer(Modifier.height(2.dp))
                OutlinedTextField(
                    value = remark,
                    onValueChange = onRemarkChange,
                    placeholder = { Text("有什么想说的？") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                // 起飞地点
                Text("起飞地点（可选）")
                Spacer(Modifier.height(2.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = onLocationChange,
                    placeholder = { Text("例如：卧室") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                // 选项：是否观看小电影 / 是否发射（高潮）
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onWatchedMovieChange(!watchedMovie) }
                            .padding(vertical = 8.dp)
                    ) {
                        Checkbox(checked = watchedMovie, onCheckedChange = null)
                        Spacer(Modifier.width(4.dp))
                        Text("是否观看小电影")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClimaxChange(!climax) }
                            .padding(vertical = 8.dp)
                    ) {
                        Checkbox(checked = climax, onCheckedChange = null)
                        Spacer(Modifier.width(4.dp))
                        Text("是否发射")
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 道具
                Text(
                    text = "道具：",
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                val propList = listOf("手", "斐济杯", "小胶妻")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    propList.forEach { p ->
                        val isSelected = props == p

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
                                )
                                .clickable { onPropsChange(p) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null, // 交由 Row 处理点击
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = p,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                // 评分
                Text("评分：${"%.1f".format(rating)}")
                Slider(
                    value = rating,
                    onValueChange = onRatingChange,
                    valueRange = 0f..5f,
                    steps = 25,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0")
                    Text("5.0")
                }
                Spacer(Modifier.height(12.dp))

                // 心情
                Text(
                    text = "心情：",
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                val moods = listOf("平静", "愉悦", "兴奋", "疲惫", "这是最后一次！")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    moods.forEach { m ->
                        val isSelected = mood == m

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
                                )
                                .clickable { onMoodChange(m) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = m,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}