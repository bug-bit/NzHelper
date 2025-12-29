package me.neko.nzhelper.ui.dialog

import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

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
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "填写本次信息",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                InputSection(title = "起飞地点（可选）") {
                    OutlinedTextField(
                        value = location,
                        onValueChange = onLocationChange,
                        placeholder = { Text("例如：卧室") },
                        modifier = Modifier.fillMaxWidth()
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
                    items = listOf("手", "斐济杯", "小胶妻"),
                    selected = props,
                    onSelected = onPropsChange
                )

                RatingSection(rating = rating, onRatingChange = onRatingChange)

                SelectionSection(
                    title = "心情",
                    items = listOf("平静", "愉悦", "兴奋", "疲惫", "这是最后一次！"),
                    selected = mood,
                    onSelected = onMoodChange
                )

                InputSection(title = "备注（可选）") {
                    OutlinedTextField(
                        value = remark,
                        onValueChange = onRemarkChange,
                        placeholder = { Text("有什么想说的？") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .height(44.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("取消")
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("确认")
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline, // 推荐使用主题色，更适应深浅模式
                    shape = MaterialTheme.shapes.medium
                )
                .clickable { onWatchedMovieChange(!watchedMovie) }
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Checkbox(checked = watchedMovie, onCheckedChange = null)
            Spacer(Modifier.width(12.dp))
            Text("是否观看小电影", style = MaterialTheme.typography.bodyLarge)
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
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Checkbox(checked = climax, onCheckedChange = null)
            Spacer(Modifier.width(12.dp))
            Text("是否发射", style = MaterialTheme.typography.bodyLarge)
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    // 四舍五入到最近的 0.5 倍数
                    val rounded = (newValue * 2).roundToInt() / 2f
                    onRatingChange(rounded.coerceIn(0f, 5f))
                },
                valueRange = 0f..5f,
                steps = 9, // 0.5 步长：从 0 到 5 共 10 个间隔，需要 steps = 9
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