package me.neko.nzhelper.feature.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.feature.statistics.model.PeriodSelection
import me.neko.nzhelper.feature.statistics.model.PeriodType
import java.time.LocalDateTime
import java.time.YearMonth

@Composable
fun PeriodPickerDialog(
    selection: PeriodSelection,
    earliestSessionTime: LocalDateTime,
    currentTime: LocalDateTime,
    onDismiss: () -> Unit,
    onConfirm: (PeriodSelection) -> Unit
) {
    val earliestMonth = YearMonth.from(earliestSessionTime)
    val currentMonth = YearMonth.from(currentTime)
    val initialMonth = if (selection.type == PeriodType.MONTH) {
        YearMonth.from(selection.anchor)
    } else {
        currentMonth
    }
    val initialYear = if (selection.type == PeriodType.WEEK) {
        currentMonth.year
    } else {
        selection.anchor.year
    }
    val years = (currentMonth.year downTo earliestMonth.year).toList()

    fun monthsIn(year: Int): List<Int> = (1..12).filter { month ->
        !YearMonth.of(year, month).isBefore(earliestMonth) &&
                !YearMonth.of(year, month).isAfter(currentMonth)
    }

    fun validMonth(year: Int, preferred: Int): Int {
        val months = monthsIn(year)
        return preferred.coerceIn(months.first(), months.last())
    }

    var selectedType by remember {
        mutableStateOf(if (selection.type == PeriodType.YEAR) PeriodType.YEAR else PeriodType.MONTH)
    }
    var selectedYear by remember { mutableIntStateOf(initialYear) }
    var selectedMonth by remember {
        mutableIntStateOf(validMonth(initialYear, initialMonth.monthValue))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("查看历史统计") },
        text = {
            Column {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(PeriodType.MONTH, PeriodType.YEAR).forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                if (type == PeriodType.MONTH) {
                                    selectedMonth = validMonth(selectedYear, selectedMonth)
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 2)
                        ) {
                            Text(if (type == PeriodType.MONTH) "月份" else "年份")
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                PeriodDropdown(
                    label = "年份",
                    value = "${selectedYear}年",
                    options = years.map { "${it}年" },
                    onSelect = { index ->
                        selectedYear = years[index]
                        selectedMonth = validMonth(selectedYear, selectedMonth)
                    }
                )

                if (selectedType == PeriodType.MONTH) {
                    Spacer(Modifier.height(12.dp))
                    val months = monthsIn(selectedYear)
                    PeriodDropdown(
                        label = "月份",
                        value = "${selectedMonth}月",
                        options = months.map { "${it}月" },
                        onSelect = { index -> selectedMonth = months[index] }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val anchor = when (selectedType) {
                        PeriodType.MONTH -> LocalDateTime.of(selectedYear, selectedMonth, 1, 0, 0)
                        else -> LocalDateTime.of(selectedYear, 1, 1, 0, 0)
                    }
                    onConfirm(PeriodSelection(selectedType, anchor))
                }
            ) {
                Text("查看")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun PeriodDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    BoxWithConstraints {
        Surface(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceBright
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.width(12.dp))
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ArrowDropUp else Icons.Outlined.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(maxWidth)
                .heightIn(max = 320.dp),
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            options.forEachIndexed { index, option ->
                val selected = option == value
                val contentColor = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
                DropdownMenuItem(
                    text = { Text(option, color = contentColor) },
                    trailingIcon = {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            if (selected) MaterialTheme.colorScheme.secondaryContainer
                            else Color.Transparent
                        ),
                    onClick = {
                        onSelect(index)
                        expanded = false
                    }
                )
            }
        }
    }
}
