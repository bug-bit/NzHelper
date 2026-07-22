package me.neko.nzhelper.ui.component.dialog

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.neko.nzhelper.core.auto.AutoTagRules
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.CategoryDef
import me.neko.nzhelper.core.model.SessionFormState
import me.neko.nzhelper.core.util.formatTime
import me.neko.nzhelper.ui.component.tag.TagPicker
import me.neko.nzhelper.ui.theme.TagColors
import me.neko.nzhelper.ui.theme.TagIcons
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailsDialog(
    show: Boolean,
    formState: SessionFormState,
    onFormStateChange: (SessionFormState) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    showDurationField: Boolean = false,
    title: String = "本次详情"
) {
    if (!show) return

    val context = LocalContext.current
    val windowInfo = LocalWindowInfo.current
    val categories = remember { TagSettings.getCategories(context) }
    val groupedTags = remember { TagSettings.groupedTags(context) }
    val effectiveCategoryId = remember(formState.categoryId) {
        formState.categoryId.ifBlank {
            TagSettings.defaultCategory(context).id
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)
                )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(
                        top = WindowInsets.statusBars
                            .asPaddingValues()
                            .calculateTopPadding()
                    )
                    .heightIn(
                        max = windowInfo.containerSize.height.dp
                    ),
                shape = RoundedCornerShape(
                    topStart = 32.dp,
                    topEnd = 32.dp
                ),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.outlineVariant)
                            .align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        textAlign = TextAlign.Center
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (showDurationField) {
                            SectionCard {
                                DurationInputSection(
                                    formState = formState,
                                    onFormStateChange = onFormStateChange
                                )

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )

                                DateTimeInputSection(
                                    formState = formState,
                                    onFormStateChange = { newForm ->
                                        val ts = try {
                                            newForm.toLocalDateTime()
                                        } catch (_: Exception) {
                                            onFormStateChange(newForm)
                                            return@DateTimeInputSection
                                        }

                                        val suggested = AutoTagRules.suggest(context, ts)
                                        val (merged, added) = AutoTagRules.merge(
                                            newForm.tagIds,
                                            suggested
                                        )

                                        onFormStateChange(
                                            newForm.copy(
                                                tagIds = merged,
                                                autoTagIds = newForm.autoTagIds + added
                                            )
                                        )
                                    }
                                )
                            }
                        }

                        SectionCard {
                            CategorySection(
                                categories = categories,
                                selectedId = effectiveCategoryId,
                                onSelect = {
                                    onFormStateChange(formState.copy(categoryId = it))
                                }
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            TagSectionHeader(
                                selectedCount = formState.tagIds.size,
                                onAutoMatch = {
                                    val ts = try {
                                        formState.toLocalDateTime()
                                    } catch (_: Exception) {
                                        LocalDateTime.now()
                                    }
                                    val suggested = AutoTagRules.suggest(context, ts)
                                    val (merged, added) = AutoTagRules.merge(
                                        formState.tagIds,
                                        suggested
                                    )
                                    onFormStateChange(
                                        formState.copy(
                                            tagIds = merged,
                                            autoTagIds = formState.autoTagIds + added
                                        )
                                    )
                                }
                            )

                            TagPicker(
                                groups = groupedTags,
                                selectedIds = formState.tagIds,
                                autoTagIds = formState.autoTagIds,
                                onToggle = { id ->
                                    val newSet = formState.tagIds.toMutableSet()
                                    val newAuto = formState.autoTagIds.toMutableSet()

                                    if (id in newSet) {
                                        newSet.remove(id)
                                        newAuto.remove(id)
                                    } else {
                                        newSet.add(id)
                                        newAuto.remove(id)
                                    }

                                    onFormStateChange(
                                        formState.copy(
                                            tagIds = newSet,
                                            autoTagIds = newAuto
                                        )
                                    )
                                }
                            )
                        }

                        SectionCard {
                            RatingSection(
                                rating = formState.rating,
                                onRatingChange = {
                                    onFormStateChange(formState.copy(rating = it))
                                }
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            ClimaxRow(
                                climax = formState.climax,
                                onClimaxChange = {
                                    onFormStateChange(formState.copy(climax = it))
                                }
                            )
                        }

                        SectionCard {
                            SectionLabel("备注")
                            OutlinedTextField(
                                value = formState.remark,
                                onValueChange = {
                                    onFormStateChange(formState.copy(remark = it))
                                },
                                placeholder = { Text("说点什么吧...") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = Int.MAX_VALUE
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Text("取消")
                        }

                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Text("保存记录")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySection(
    categories: List<CategoryDef>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "分类",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            categories.forEach { category ->
                val selected = category.id == selectedId
                FilterChip(
                    selected = selected,
                    onClick = { onSelect(category.id) },
                    label = { Text(category.name) },
                    leadingIcon = {
                        Icon(
                            imageVector = TagIcons.iconFor(category.icon),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (selected) TagColors.contentColor(category.color)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TagColors.containerColor(category.color),
                        selectedLabelColor = TagColors.contentColor(category.color),
                        selectedLeadingIconColor = TagColors.contentColor(category.color)
                    )
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun TagSectionHeader(
    selectedCount: Int,
    onAutoMatch: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "标签",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "已选 $selectedCount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = onAutoMatch,
                contentPadding = PaddingValues(
                    horizontal = 8.dp,
                    vertical = 0.dp
                )
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "自动匹配",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ClimaxRow(
    climax: Boolean,
    onClimaxChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClimaxChange(!climax) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "高潮",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Switch(
            checked = climax,
            onCheckedChange = onClimaxChange
        )
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center)
            )
            Text(
                ":",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center)
            )
            Text(
                ":",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center)
            )
        }
        Text(
            text = "合计：${formatTime(formState.manualDurationSeconds)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
private fun DateTimeInputSection(
    formState: SessionFormState,
    onFormStateChange: (SessionFormState) -> Unit
) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "日期时间",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "%02d-%02d".format(
                        formState.manualMonth,
                        formState.manualDay
                    )
                )
            }

            OutlinedButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "%02d:%02d".format(formState.manualHour, formState.manualMinute)
                )
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                if (selectedDate.isAfter(LocalDate.now())) {
                    Toast.makeText(context, "不能选择未来的日期", Toast.LENGTH_SHORT).show()
                } else {
                    onFormStateChange(
                        formState.copy(
                            manualYear = year,
                            manualMonth = month + 1,
                            manualDay = dayOfMonth
                        )
                    )
                }
                showDatePicker = false
            },
            formState.manualYear,
            formState.manualMonth - 1,
            formState.manualDay
        ).apply {
            setOnCancelListener { showDatePicker = false }
        }.show()
    }

    if (showTimePicker) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val selectedDate =
                    LocalDate.of(formState.manualYear, formState.manualMonth, formState.manualDay)
                val selectedTime = LocalTime.of(hourOfDay, minute)

                if (selectedDate.isEqual(LocalDate.now()) && selectedTime.isAfter(LocalTime.now())) {
                    Toast.makeText(context, "不能选择未来的时间", Toast.LENGTH_SHORT).show()
                } else {
                    onFormStateChange(
                        formState.copy(
                            manualHour = hourOfDay,
                            manualMinute = minute
                        )
                    )
                }
                showTimePicker = false
            },
            formState.manualHour,
            formState.manualMinute,
            true
        ).apply {
            setOnCancelListener { showTimePicker = false }
        }.show()
    }
}
