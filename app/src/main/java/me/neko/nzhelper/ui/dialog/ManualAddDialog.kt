package me.neko.nzhelper.ui.dialog

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// 扩展函数：复制 LocalDateTime 的日期部分
private fun LocalDateTime.withDate(newDate: LocalDate): LocalDateTime {
    return this.withYear(newDate.year)
        .withMonth(newDate.monthValue)
        .withDayOfMonth(newDate.dayOfMonth)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualAddDialog(
    show: Boolean,
    selectedDateTime: LocalDateTime,
    onSelectedDateTimeChange: (LocalDateTime) -> Unit,
    durationSeconds: Int,
    onDurationSecondsChange: (Int) -> Unit,
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
    customMoods: List<String> = listOf("平静", "愉悦", "兴奋", "疲惫", "这是最后一次！"),
    customProps: List<String> = listOf("手", "斐济杯", "小胶妻"),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "手动添加记录",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // 日期时间选择
                InputSection(title = "发生时间") {
                    var showDatePicker by remember { mutableStateOf(false) }
                    var showTimePicker by remember { mutableStateOf(false) }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // 显示当前选中的日期时间
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .clickable { showDatePicker = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "日期",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = selectedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "时间",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = selectedDateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                    
                    // 日期选择器弹窗
                    if (showDatePicker) {
                        val datePickerState = rememberDatePickerState(
                            initialSelectedDateMillis = selectedDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        )
                        DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        datePickerState.selectedDateMillis?.let { millis ->
                                            val newDate = Instant.ofEpochMilli(millis)
                                                .atZone(ZoneId.systemDefault())
                                                .toLocalDate()
                                            // 检查是否选择了未来的日期
                                            val now = LocalDateTime.now()
                                            val selectedDateTimeWithNewDate = newDate.atTime(now.hour, now.minute)
                                            if (selectedDateTimeWithNewDate.isAfter(now)) {
                                                // 如果选择了未来日期，重置到今天
                                                onSelectedDateTimeChange(now)
                                            } else {
                                                val newDateTime = selectedDateTime.withDate(newDate)
                                                onSelectedDateTimeChange(newDateTime)
                                            }
                                        }
                                        showDatePicker = false
                                        // 显示时间选择器
                                        showTimePicker = true
                                    }
                                ) {
                                    Text("确定")
                                }
                            },
                            dismissButton = {
                                Button(
                                    onClick = { showDatePicker = false }
                                ) {
                                    Text("取消")
                                }
                            }
                        ) {
                            DatePicker(state = datePickerState)
                        }
                    }
                    
                    // 时间选择器弹窗
                    if (showTimePicker) {
                        val timePickerState = rememberTimePickerState(
                            initialHour = selectedDateTime.hour,
                            initialMinute = selectedDateTime.minute,
                            is24Hour = true
                        )
                        DatePickerDialog(
                            onDismissRequest = { showTimePicker = false },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val newDateTime = selectedDateTime
                                            .withHour(timePickerState.hour)
                                            .withMinute(timePickerState.minute)
                                        onSelectedDateTimeChange(newDateTime)
                                        showTimePicker = false
                                    }
                                ) {
                                    Text("确定")
                                }
                            },
                            dismissButton = {
                                Button(
                                    onClick = { showTimePicker = false }
                                ) {
                                    Text("取消")
                                }
                            }
                        ) {
                            Surface(
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                TimePicker(state = timePickerState)
                            }
                        }
                    }
                }

                // 时长输入
                InputSection(title = "持续时长") {
                    val minutes = durationSeconds / 60
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // 显示当前时长
                        Text(
                            text = "已设置：${minutes}分钟",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        // 快速选择按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(-30, -15, -10, -5, +5, +10, +15, +30).forEach { mins ->
                                Button(
                                    onClick = { 
                                        val newMinutes = (minutes + mins).coerceAtLeast(0)
                                        onDurationSecondsChange(newMinutes * 60)
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(4.dp)
                                ) {
                                    Text(
                                        text = if (mins > 0) "+$mins" else "$mins",
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp
                )

                InputSection(title = "起飞地点（可选）") {
                    OutlinedTextField(
                        value = location,
                        onValueChange = onLocationChange,
                        placeholder = { Text("例如：卧室") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }

                CheckboxGroup(
                    watchedMovie = watchedMovie,
                    onWatchedMovieChange = onWatchedMovieChange,
                    climax = climax,
                    onClimaxChange = onClimaxChange
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp
                )

                SelectionSection(
                    title = "道具",
                    items = customProps,
                    selected = props,
                    onSelected = onPropsChange
                )

                RatingSection(rating = rating, onRatingChange = onRatingChange)

                SelectionSection(
                    title = "心情",
                    items = customMoods,
                    selected = mood,
                    onSelected = onMoodChange
                )

                InputSection(title = "备注（可选）") {
                    OutlinedTextField(
                        value = remark,
                        onValueChange = onRemarkChange,
                        placeholder = { Text("有什么想说的？") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("取消", style = MaterialTheme.typography.bodyMedium)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("确认保存", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun InputSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun CheckboxGroup(
    watchedMovie: Boolean,
    onWatchedMovieChange: (Boolean) -> Unit,
    climax: Boolean,
    onClimaxChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.medium
                )
                .clickable { onWatchedMovieChange(!watchedMovie) }
                .padding(horizontal = 6.dp, vertical = 8.dp)
        ) {
            Checkbox(checked = watchedMovie, onCheckedChange = null)
            Spacer(Modifier.width(10.dp))
            Text("配菜", style = MaterialTheme.typography.bodyMedium)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.medium
                )
                .clickable { onClimaxChange(!climax) }
                .padding(horizontal = 6.dp, vertical = 8.dp)
        ) {
            Checkbox(checked = climax, onCheckedChange = null)
            Spacer(Modifier.width(10.dp))
            Text("是否发射", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SelectionSection(
    title: String,
    items: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)

        // 使用横向滚动布局，避免多行堆叠
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                FilterChip(
                    onClick = { onSelected(item) },
                    label = { Text(item) },
                    selected = selected == item,
                    leadingIcon = if (selected == item) {
                        {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("评分", style = MaterialTheme.typography.titleMedium)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "%.1f".format(rating),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.width(48.dp)
            )

            Slider(
                value = rating,
                onValueChange = { newValue ->
                    val rounded = (newValue * 2).roundToInt() / 2f
                    onRatingChange(rounded.coerceIn(0f, 5f))
                },
                valueRange = 0f..5f,
                steps = 9,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "/ 5.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(48.dp)
            )
        }
    }
}
