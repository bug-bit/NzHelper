package me.neko.nzhelper.ui.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.neko.nzhelper.data.SessionFormState
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 详情填写弹窗
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailsDialog(
    show: Boolean,
    formState: SessionFormState,
    onFormStateChange: (SessionFormState) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    locationList: List<String> = listOf("卧室", "沙发", "厕所"),
    propsList: List<String> = listOf("手", "飞机杯", "小胶妻"),
    moodList: List<String> = listOf("平静", "愉悦", "兴奋", "疲惫"),
    showDurationField: Boolean = false,
    title: String = "本次详情"
) {
    if (!show) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // 手动添加时显示时长输入
                if (showDurationField) {
                    DurationInputSection(
                        formState = formState,
                        onFormStateChange = onFormStateChange
                    )
                }

                // 地点
                SelectionSection(
                    title = "地点",
                    items = locationList,
                    selected = formState.location,
                    onSelected = { onFormStateChange(formState.copy(location = it)) }
                )

                // 开关组
                SwitchSection(
                    watchedMovie = formState.watchedMovie,
                    onWatchedMovieChange = { onFormStateChange(formState.copy(watchedMovie = it)) },
                    climax = formState.climax,
                    onClimaxChange = { onFormStateChange(formState.copy(climax = it)) }
                )

                // 评分
                RatingSection(
                    rating = formState.rating,
                    onRatingChange = { onFormStateChange(formState.copy(rating = it)) }
                )

                // 道具选择
                SelectionSection(
                    title = "道具",
                    items = propsList,
                    selected = formState.props,
                    onSelected = { onFormStateChange(formState.copy(props = it)) }
                )

                // 心情选择
                SelectionSection(
                    title = "心情",
                    items = moodList,
                    selected = formState.mood,
                    onSelected = { onFormStateChange(formState.copy(mood = it)) }
                )

                // 备注
                InputSection(title = "备注") {
                    OutlinedTextField(
                        value = formState.remark,
                        onValueChange = { onFormStateChange(formState.copy(remark = it)) },
                        placeholder = { Text("有什么想说的...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = Int.MAX_VALUE
                    )
                }

                // 按钮组
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("保存记录")
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchSection(
    watchedMovie: Boolean,
    onWatchedMovieChange: (Boolean) -> Unit,
    climax: Boolean,
    onClimaxChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "状态",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedFilterChip(
                selected = watchedMovie,
                onClick = { onWatchedMovieChange(!watchedMovie) },
                label = { Text("小电影") },
                leadingIcon = if (watchedMovie) {
                    {
                        Icon(
                            Icons.Default.Check,
                            null,
                            Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null,
                modifier = Modifier.weight(1f)
            )
            ElevatedFilterChip(
                selected = climax,
                onClick = { onClimaxChange(!climax) },
                label = { Text("高潮") },
                leadingIcon = if (climax) {
                    {
                        Icon(
                            Icons.Default.Check,
                            null,
                            Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun InputSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}

@Composable
private fun SelectionSection(
    title: String,
    items: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                AssistChip(
                    onClick = { onSelected(item) },
                    label = { Text(item) },
                    border = if (selected == item) {
                        null
                    } else {
                        BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (selected == item)
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    ),
                    leadingIcon = if (selected == item) {
                        {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun RatingSection(
    rating: Float,
    onRatingChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "评分",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "%.1f".format(rating),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = rating,
            onValueChange = {
                val rounded = (it * 10).roundToInt() / 10f
                onRatingChange(rounded.coerceIn(0f, 5f))
            },
            valueRange = 0f..5f,
            steps = 49
        )
    }
}

@Composable
private fun DurationInputSection(
    formState: SessionFormState,
    onFormStateChange: (SessionFormState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "时长",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = formState.durationHour,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { it.isDigit() }.take(2)
                    onFormStateChange(formState.copy(durationHour = filtered))
                },
                label = { Text("时") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(textAlign = TextAlign.Center)
            )
            OutlinedTextField(
                value = formState.durationMinute,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { it.isDigit() }.take(2)
                    onFormStateChange(formState.copy(durationMinute = filtered))
                },
                label = { Text("分") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(textAlign = TextAlign.Center)
            )
            OutlinedTextField(
                value = formState.durationSecond,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { it.isDigit() }.take(2)
                    onFormStateChange(formState.copy(durationSecond = filtered))
                },
                label = { Text("秒") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(textAlign = TextAlign.Center)
            )
        }
        Text(
            text = "合计：${formatTime(formState.manualDurationSeconds)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

// 通用工具函数
fun formatTime(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return buildString {
        if (hours > 0) append(String.format(Locale.getDefault(), "%02d:", hours))
        append(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds))
    }
}