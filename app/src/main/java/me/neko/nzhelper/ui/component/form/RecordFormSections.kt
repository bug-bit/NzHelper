package me.neko.nzhelper.ui.component.form

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.model.Contraception
import me.neko.nzhelper.core.model.PartnerGender
import me.neko.nzhelper.core.model.SessionFormState
import me.neko.nzhelper.core.model.SessionMode
import me.neko.nzhelper.ui.component.setting.SettingsCard
import me.neko.nzhelper.ui.component.setting.SettingsItemHost
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.math.roundToInt

@Composable
fun SettingsSection(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    SettingsItemHost(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun SectionCard(
    title: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    SettingsCard(title = title) {
        item {
            SettingsSection(onClick = onClick, content = content)
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun ClimaxCountSection(
    formState: SessionFormState,
    onFormStateChange: (SessionFormState) -> Unit
) {
    val isPair = SessionMode.fromKey(formState.mode).isPair
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (isPair) {
            ClimaxStepperRow(
                label = "我的高潮",
                value = formState.climaxCount,
                onChange = { onFormStateChange(formState.copy(climaxCount = it)) }
            )
            ClimaxStepperRow(
                label = "对方高潮",
                value = formState.partnerClimaxCount,
                onChange = { onFormStateChange(formState.copy(partnerClimaxCount = it)) }
            )
        } else {
            ClimaxStepperRow(
                label = "高潮次数",
                value = formState.climaxCount,
                onChange = { onFormStateChange(formState.copy(climaxCount = it)) }
            )
        }
    }
}

@Composable
fun ClimaxStepperRow(
    label: String,
    value: Int,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onChange((value - 1).coerceAtLeast(0)) },
                enabled = value > 0,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Outlined.Remove,
                    contentDescription = "减少",
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = "$value 次",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(56.dp)
            )
            IconButton(
                onClick = { onChange((value + 1).coerceAtMost(20)) },
                enabled = value < 20,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "增加",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun PartnerGenderSection(
    formState: SessionFormState,
    onFormStateChange: (SessionFormState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("对方性别")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PartnerGender.entries.forEach { gender ->
                val selected = formState.partnerGender == gender.key
                FilterChip(
                    selected = selected,
                    onClick = {
                        onFormStateChange(
                            formState.copy(
                                partnerGender = if (selected) "" else gender.key
                            )
                        )
                    },
                    label = { Text(gender.label) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContraceptionSection(
    formState: SessionFormState,
    onFormStateChange: (SessionFormState) -> Unit
) {
    OptionSelectCard(
        title = "避孕措施",
        options = Contraception.entries.map { it.key to it.label },
        selectedKey = formState.contraception,
        onSelectionChange = {
            onFormStateChange(formState.copy(contraception = it))
        }
    )
}

@Composable
fun RatingSection(
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
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "%.1f".format(rating),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        val sliderState = rememberSliderState(
            value = rating,
            steps = 49,
            trackRange = 0f..5f
        )
        Slider(
            state = sliderState,
            onValueChange = {
                val rounded = (it * 10).roundToInt() / 10f
                onRatingChange(rounded.coerceIn(0f, 5f))
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeInputSection(
    formState: SessionFormState,
    onFormStateChange: (SessionFormState) -> Unit
) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("日期时间")

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.fillMaxWidth(),
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
        val today = LocalDate.now()
        val selectableDates = remember {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date =
                        Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    return !date.isAfter(today)
                }

                override fun isSelectableYear(year: Int): Boolean = year <= today.year
            }
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate
                .of(formState.manualYear, formState.manualMonth, formState.manualDay)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
            selectableDates = selectableDates
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate =
                                Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            if (selectedDate.isAfter(today)) {
                                Toast.makeText(context, "不能选择未来的日期", Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                onFormStateChange(
                                    formState.copy(
                                        manualYear = selectedDate.year,
                                        manualMonth = selectedDate.monthValue,
                                        manualDay = selectedDate.dayOfMonth
                                    )
                                )
                            }
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = formState.manualHour,
            initialMinute = formState.manualMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("选择时间") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDate =
                            LocalDate.of(
                                formState.manualYear,
                                formState.manualMonth,
                                formState.manualDay
                            )
                        val selectedTime =
                            LocalTime.of(timePickerState.hour, timePickerState.minute)

                        if (selectedDate.isEqual(LocalDate.now()) && selectedTime.isAfter(LocalTime.now())) {
                            Toast.makeText(context, "不能选择未来的时间", Toast.LENGTH_SHORT).show()
                        } else {
                            onFormStateChange(
                                formState.copy(
                                    manualHour = timePickerState.hour,
                                    manualMinute = timePickerState.minute
                                )
                            )
                        }
                        showTimePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("取消")
                }
            }
        )
    }
}
