package com.blanksstudio.gridpix.ui.puzzle

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.blanksstudio.gridpix.game.CellState
import com.blanksstudio.gridpix.game.Clues
import com.blanksstudio.gridpix.game.GridState
import com.blanksstudio.gridpix.ui.theme.BoardColors
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Grid cell under a touch point, or null when the touch is outside the grid. */
private data class Cell(val row: Int, val col: Int)

/**
 * The board (SPEC S4): column clues on top, row clues on the left, the grid, with tap, drag-paint,
 * long-press (opposite mode) and, when [zoomEnabled], pinch-zoom and two-finger pan.
 *
 * Gesture contract with the ViewModel: on the first paint of a gesture [onStrokeStart] is called,
 * then [onPaint] per cell, then [onStrokeEnd]. [targetFor] decides what state the stroke paints.
 */
@Composable
fun NonogramBoard(
    clues: Clues,
    board: GridState,
    mistakes: Set<Pair<Int, Int>>,
    lastHint: Pair<Int, Int>?,
    darkTheme: Boolean,
    enabled: Boolean,
    zoomEnabled: Boolean,
    targetFor: (row: Int, col: Int, alternate: Boolean) -> CellState,
    onStrokeStart: () -> Unit,
    onPaint: (row: Int, col: Int, target: CellState, from: CellState) -> Unit,
    onStrokeEnd: () -> Unit,
    onTapFeedback: () -> Unit,
    onLongPressFeedback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val n = clues.size
    val maxRowClues = max(1, clues.rows.maxOf { it.size })
    val maxColClues = max(1, clues.cols.maxOf { it.size })
    val clueRatio = 0.62f // clue cells are smaller than grid cells

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val currentBoard by rememberUpdatedState(board)
    val currentEnabled by rememberUpdatedState(enabled)

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val viewConfig = LocalViewConfiguration.current
        val boxSize = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        val cell = min(boxSize.width / (n + maxRowClues * clueRatio), boxSize.height / (n + maxColClues * clueRatio))
        val clueW = maxRowClues * clueRatio * cell
        val clueH = maxColClues * clueRatio * cell
        val totalW = clueW + n * cell
        val totalH = clueH + n * cell
        val totalSize = with(density) { androidx.compose.ui.unit.DpSize(totalW.toDp(), totalH.toDp()) }

        val textMeasurer = rememberTextMeasurer()
        val filledColor = if (darkTheme) BoardColors.filledDark else BoardColors.filledLight
        val lineColor = filledColor.copy(alpha = 0.35f)
        val thickColor = filledColor.copy(alpha = 0.9f)
        val clueColor = filledColor
        val clueBg = filledColor.copy(alpha = if (darkTheme) 0.10f else 0.06f)
        val clueDoneColor = filledColor.copy(alpha = 0.35f)
        val clueStyle = TextStyle(
            fontSize = with(density) { (cell * clueRatio * 0.62f).toSp() },
            fontWeight = FontWeight.Medium,
            color = clueColor,
        )

        fun cellAt(p: Offset): Cell? {
            // Undo the zoom/pan transform (graphicsLayer scales about the centre).
            val cx = totalW / 2
            val cy = totalH / 2
            val x = (p.x - offset.x - cx) / scale + cx
            val y = (p.y - offset.y - cy) / scale + cy
            val col = floor((x - clueW) / cell).toInt()
            val row = floor((y - clueH) / cell).toInt()
            return if (row in 0 until n && col in 0 until n) Cell(row, col) else null
        }

        Canvas(
            modifier = Modifier
                .size(totalSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .pointerInput(n, zoomEnabled) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downTime = down.uptimeMillis
                        val slop = viewConfig.touchSlop
                        val longPressMs = viewConfig.longPressTimeoutMillis
                        val startCell = cellAt(down.position)
                        var painting = false
                        var transforming = false
                        var axis: Char? = null // 'r' row-locked, 'c' column-locked
                        var target = CellState.EMPTY
                        var from = CellState.EMPTY
                        var lastCell: Cell? = startCell

                        fun startStroke(alternate: Boolean) {
                            val c = startCell ?: return
                            if (!currentEnabled) return
                            from = currentBoard[c.row, c.col]
                            target = targetFor(c.row, c.col, alternate)
                            onStrokeStart()
                            onPaint(c.row, c.col, target, from)
                            if (alternate) onLongPressFeedback() else onTapFeedback()
                            painting = true
                        }

                        var longPressFired = false
                        while (true) {
                            val event = if (!painting && !transforming && !longPressFired) {
                                val elapsed = SystemClock.uptimeMillis() - downTime
                                withTimeoutOrNull(max(1L, longPressMs - elapsed)) { awaitPointerEvent() }
                            } else {
                                awaitPointerEvent()
                            }
                            if (event == null) {
                                // Long press without movement: paint in the opposite mode.
                                longPressFired = true
                                if (startCell != null) startStroke(alternate = true)
                                continue
                            }
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.isEmpty()) break

                            if (zoomEnabled && pressed.size >= 2) {
                                if (painting) { onStrokeEnd(); painting = false }
                                transforming = true
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val newScale = (scale * zoom).coerceIn(1f, 4f)
                                scale = newScale
                                val maxDx = (totalW * (newScale - 1)) / 2
                                val maxDy = (totalH * (newScale - 1)) / 2
                                offset = Offset(
                                    (offset.x + pan.x).coerceIn(-maxDx, maxDx),
                                    (offset.y + pan.y).coerceIn(-maxDy, maxDy),
                                )
                                event.changes.forEach { it.consume() }
                                continue
                            }
                            if (transforming) continue

                            val change: PointerInputChange = pressed.first()
                            val moved = (change.position - down.position).getDistance() > slop
                            if (!painting) {
                                if (moved && startCell != null) startStroke(alternate = false)
                                else continue
                            }
                            change.consume()
                            if (!change.positionChanged()) continue
                            val here = cellAt(change.position) ?: continue
                            val start = startCell ?: continue
                            if (axis == null && here != start) {
                                axis = if (abs(here.col - start.col) >= abs(here.row - start.row)) 'r' else 'c'
                            }
                            val locked = when (axis) {
                                'r' -> Cell(start.row, here.col)
                                'c' -> Cell(here.row, start.col)
                                else -> here
                            }
                            if (locked != lastCell) {
                                // Paint every cell between the previous and current position so fast drags leave no gaps.
                                val prev = lastCell ?: start
                                val steps = max(abs(locked.row - prev.row), abs(locked.col - prev.col))
                                for (i in 1..steps) {
                                    val r = prev.row + (locked.row - prev.row) * i / steps
                                    val c = prev.col + (locked.col - prev.col) * i / steps
                                    onPaint(r, c, target, from)
                                }
                                onTapFeedback()
                                lastCell = locked
                            }
                        }
                        if (!painting && !transforming && startCell != null) {
                            // Plain tap: paint once on release.
                            startStroke(alternate = false)
                        }
                        if (painting) onStrokeEnd()
                    }
                },
        ) {
            // Clue backgrounds
            drawRect(clueBg, Offset(clueW, 0f), Size(n * cell, clueH))
            drawRect(clueBg, Offset(0f, clueH), Size(clueW, n * cell))

            // Cells
            for (r in 0 until n) {
                for (c in 0 until n) {
                    val x = clueW + c * cell
                    val y = clueH + r * cell
                    when (board[r, c]) {
                        CellState.FILLED -> {
                            val isMistake = (r to c) in mistakes
                            val isHint = lastHint == (r to c)
                            val color = when {
                                isMistake -> BoardColors.mistake
                                isHint -> BoardColors.hint
                                else -> filledColor
                            }
                            drawRect(color, Offset(x + 1f, y + 1f), Size(cell - 2f, cell - 2f))
                        }
                        CellState.MARKED -> {
                            val inset = cell * 0.3f
                            val color = if (lastHint == (r to c)) BoardColors.hint else lineColor
                            val stroke = Stroke(width = max(2f, cell * 0.08f), cap = StrokeCap.Round)
                            drawLine(color, Offset(x + inset, y + inset), Offset(x + cell - inset, y + cell - inset), stroke.width, stroke.cap)
                            drawLine(color, Offset(x + cell - inset, y + inset), Offset(x + inset, y + cell - inset), stroke.width, stroke.cap)
                        }
                        CellState.EMPTY -> Unit
                    }
                }
            }

            // Grid lines, thicker every 5
            for (i in 0..n) {
                val thick = i % 5 == 0
                val w = if (thick) max(2f, cell * 0.06f) else 1f
                val color = if (thick) thickColor else lineColor
                val x = clueW + i * cell
                drawLine(color, Offset(x, 0f), Offset(x, totalH), w)
                val y = clueH + i * cell
                drawLine(color, Offset(0f, y), Offset(totalW, y), w)
            }

            // Clues: row clues right-aligned, column clues bottom-aligned. Satisfied lines are dimmed.
            val clueCell = cell * clueRatio
            for (r in 0 until n) {
                val clue = clues.rows[r]
                val done = lineSatisfied(clue, (0 until n).map { board[r, it] == CellState.FILLED })
                val style = if (done) clueStyle.copy(color = clueDoneColor) else clueStyle
                val texts = if (clue.isEmpty()) listOf("0") else clue.map { it.toString() }
                texts.forEachIndexed { i, t ->
                    val layout = textMeasurer.measure(t, style)
                    val slot = maxRowClues - texts.size + i
                    val x = slot * clueCell + (clueCell - layout.size.width) / 2
                    val y = clueH + r * cell + (cell - layout.size.height) / 2
                    drawText(layout, topLeft = Offset(x, y))
                }
            }
            for (c in 0 until n) {
                val clue = clues.cols[c]
                val done = lineSatisfied(clue, (0 until n).map { board[it, c] == CellState.FILLED })
                val style = if (done) clueStyle.copy(color = clueDoneColor) else clueStyle
                val texts = if (clue.isEmpty()) listOf("0") else clue.map { it.toString() }
                texts.forEachIndexed { i, t ->
                    val layout = textMeasurer.measure(t, style)
                    val slot = maxColClues - texts.size + i
                    val x = clueW + c * cell + (cell - layout.size.width) / 2
                    val y = slot * clueCell + (clueCell - layout.size.height) / 2
                    drawText(layout, topLeft = Offset(x, y))
                }
            }
        }
    }
}

/** True when the filled cells of a line already match its clue exactly. */
internal fun lineSatisfied(clue: List<Int>, filled: List<Boolean>): Boolean {
    val runs = ArrayList<Int>()
    var run = 0
    for (f in filled) {
        if (f) run++ else if (run > 0) { runs.add(run); run = 0 }
    }
    if (run > 0) runs.add(run)
    return runs == clue
}
