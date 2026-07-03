package me.neko.nzhelper.ui.screens.lock.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun GestureLockView(
    onGestureComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    val points = remember { mutableStateListOf<Int>() }
    var currentPosition by remember { mutableStateOf(Offset.Zero) }
    var isTouching by remember { mutableStateOf(false) }
    var drawCompleted by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val outlineColor = MaterialTheme.colorScheme.outline
    val density = LocalDensity.current

    LaunchedEffect(drawCompleted, isError) {
        if (drawCompleted || isError) {
            delay(500.milliseconds)
            points.clear()
            currentPosition = Offset.Zero
            drawCompleted = false
        }
    }

    val lineColor = if (isError) errorColor else primaryColor

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val cellW = widthPx / 3f
        val cellH = heightPx / 3f

        val nodes = remember(widthPx, heightPx) {
            (0 until 9).map { i ->
                val row = i / 3
                val col = i % 3
                Offset(
                    x = cellW / 2f + col * cellW,
                    y = cellH / 2f + row * cellH
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    val hitRadius = cellW / 3f
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (!drawCompleted && !isError) {
                                points.clear()
                                isTouching = true
                                currentPosition = offset
                                checkNodeHit(offset, nodes, hitRadius, points)
                            }
                        },
                        onDrag = { change, _ ->
                            if (isTouching) {
                                currentPosition = change.position
                                checkNodeHit(change.position, nodes, hitRadius, points)
                            }
                        },
                        onDragEnd = {
                            isTouching = false
                            if (points.isNotEmpty()) {
                                onGestureComplete(points.joinToString("-"))
                                drawCompleted = true
                            }
                        },
                        onDragCancel = {
                            isTouching = false
                            points.clear()
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val nodeRadius = size.minDimension / 12f

                if (points.isNotEmpty()) {
                    for (i in 0 until points.size - 1) {
                        drawLine(
                            color = lineColor,
                            start = nodes[points[i]],
                            end = nodes[points[i + 1]],
                            strokeWidth = nodeRadius / 2,
                            cap = StrokeCap.Round
                        )
                    }

                    if (isTouching) {
                        drawLine(
                            color = lineColor.copy(alpha = 0.7f),
                            start = nodes[points.last()],
                            end = currentPosition,
                            strokeWidth = nodeRadius / 2,
                            cap = StrokeCap.Round
                        )
                    }
                }

                nodes.forEachIndexed { index, offset ->
                    drawCircle(
                        color = outlineColor.copy(alpha = 0.3f),
                        radius = nodeRadius,
                        center = offset
                    )

                    if (index in points) {
                        drawCircle(
                            color = lineColor.copy(alpha = 0.2f),
                            radius = nodeRadius,
                            center = offset
                        )
                        drawCircle(
                            color = lineColor,
                            radius = nodeRadius / 2,
                            center = offset
                        )
                    } else {
                        drawCircle(
                            color = outlineColor.copy(alpha = 0.8f),
                            radius = nodeRadius / 4,
                            center = offset
                        )
                    }
                }
            }
        }
    }
}

private fun checkNodeHit(
    position: Offset,
    nodes: List<Offset>,
    hitRadius: Float,
    hitPoints: MutableList<Int>
) {
    nodes.forEachIndexed { index, offset ->
        if ((position - offset).getDistance() < hitRadius && index !in hitPoints) {
            hitPoints.add(index)
        }
    }
}