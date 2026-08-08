package me.neko.nzhelper.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    keyOf: (T) -> String,
    onReorder: (List<T>) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
    gap: Dp = 10.dp,
    itemContent: @Composable (item: T, dragHandle: Modifier, isDragging: Boolean) -> Unit
) {
    val gapPx = with(LocalDensity.current) { gap.toPx() }
    val latestItems = rememberUpdatedState(items)
    val latestOnReorder = rememberUpdatedState(onReorder)
    val latestOnCommit = rememberUpdatedState(onCommit)
    val itemHeights = remember { hashMapOf<String, Int>() }
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        items.forEach { item ->
            val itemKey = keyOf(item)
            val isDragging = draggingKey == itemKey

            val dragHandle = Modifier.pointerInput(itemKey) {
                detectDragGestures(
                    onDragStart = {
                        draggingKey = itemKey
                        dragOffset = 0f
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragEnd = {
                        latestOnCommit.value()
                        draggingKey = null
                        dragOffset = 0f
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragCancel = {
                        draggingKey = null
                        dragOffset = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount.y
                        val cur = latestItems.value
                        val curIndex = cur.indexOfFirst { keyOf(it) == itemKey }
                        if (curIndex < 0) return@detectDragGestures
                        val selfH = (itemHeights[itemKey] ?: 0).toFloat()
                        if (selfH <= 0f) return@detectDragGestures

                        // 向下移动
                        var newIndex = curIndex
                        while (newIndex < cur.lastIndex) {
                            val nextH =
                                (itemHeights[keyOf(cur[newIndex + 1])] ?: 0).toFloat()
                            if (nextH <= 0f) break
                            if (dragOffset > (selfH + nextH) / 2f + gapPx) {
                                newIndex++
                                dragOffset -= (nextH + gapPx)
                            } else break
                        }
                        // 向上移动
                        while (newIndex > 0) {
                            val prevH =
                                (itemHeights[keyOf(cur[newIndex - 1])] ?: 0).toFloat()
                            if (prevH <= 0f) break
                            if (dragOffset < -((selfH + prevH) / 2f + gapPx)) {
                                newIndex--
                                dragOffset += (prevH + gapPx)
                            } else break
                        }

                        if (newIndex != curIndex) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val mutable = cur.toMutableList()
                            val moved = mutable.removeAt(curIndex)
                            mutable.add(newIndex, moved)
                            latestOnReorder.value(mutable)
                        }
                    }
                )
            }

            key(itemKey) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            itemHeights[itemKey] = coords.size.height
                        }
                        .then(
                            if (isDragging) {
                                Modifier
                                    .offset { IntOffset(0, dragOffset.roundToInt()) }
                                    .zIndex(1f)
                                    .shadow(12.dp, shape = RoundedCornerShape(12.dp))
                                    .graphicsLayer {
                                        scaleX = 1.03f
                                        scaleY = 1.03f
                                    }
                            } else {
                                Modifier
                            }
                        )
                ) {
                    itemContent(item, dragHandle, isDragging)
                }
            }
        }
    }
}
