package me.neko.nzhelper.feature.addrecord.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private const val MIN_MINUTES = 1
private const val MAX_MINUTES = 240
private val ITEM_HEIGHT = 40.dp
private val PICKER_HEIGHT = 200.dp
private val CENTER_OFFSET = (PICKER_HEIGHT - ITEM_HEIGHT) / 2
private val EDGE_SCRIM_HEIGHT = 60.dp

internal fun formatDurationMinutes(minutes: Int): String = when {
    minutes < 60 -> "$minutes 分钟"
    minutes % 60 == 0 -> "${minutes / 60} 小时"
    else -> "${minutes / 60} 小时 ${minutes % 60} 分钟"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DurationPickerSheet(
    initialSeconds: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMinute = (initialSeconds / 60).coerceIn(MIN_MINUTES, MAX_MINUTES)
    val density = LocalDensity.current
    val itemHeightPx = with(density) { ITEM_HEIGHT.toPx() }
    val centerOffsetPx = with(density) { CENTER_OFFSET.toPx() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(
        lazyListState = listState,
        snapPosition = SnapPosition.Center
    )

    LaunchedEffect(Unit) {
        listState.scrollToItem(0)
        listState.scroll {
            scrollBy(itemHeightPx * (initialMinute - MIN_MINUTES))
        }
    }

    val selectedMinute by remember {
        derivedStateOf {
            val centerPx =
                listState.layoutInfo.viewportSize.height / 2f - centerOffsetPx
            val index = (
                    listState.firstVisibleItemIndex * itemHeightPx +
                            listState.firstVisibleItemScrollOffset +
                            centerPx
                    ) / itemHeightPx
            (index.toInt().coerceIn(0, MAX_MINUTES - 1)) + MIN_MINUTES
        }
    }

    @Suppress("DEPRECATION")
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "选择时长",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PICKER_HEIGHT)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .align(Alignment.Center)
                        .height(ITEM_HEIGHT)
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                )
                LazyColumn(
                    state = listState,
                    flingBehavior = flingBehavior,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = CENTER_OFFSET)
                ) {
                    items(MAX_MINUTES) { index ->
                        val minute = index + MIN_MINUTES
                        val selected = minute == selectedMinute
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ITEM_HEIGHT)
                                .clickable {
                                    val targetPx = itemHeightPx * (minute - MIN_MINUTES)
                                    scope.launch {
                                        listState.scroll {
                                            val currentPx =
                                                listState.firstVisibleItemIndex * itemHeightPx +
                                                        listState.firstVisibleItemScrollOffset
                                            scrollBy(targetPx - currentPx)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formatDurationMinutes(minute),
                                style = if (selected) {
                                    MaterialTheme.typography.titleMedium
                                } else {
                                    MaterialTheme.typography.bodyLarge
                                },
                                fontWeight = if (selected) {
                                    FontWeight.SemiBold
                                } else {
                                    FontWeight.Normal
                                },
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .height(EDGE_SCRIM_HEIGHT)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    Color.Transparent
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(EDGE_SCRIM_HEIGHT)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            )
                        )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                    onClick = { onConfirm(selectedMinute * 60) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("确定")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
