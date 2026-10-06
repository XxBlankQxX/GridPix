package com.blanksstudio.gridpix.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.blanksstudio.gridpix.game.Solution
import kotlin.math.min

/**
 * Draws a finished picture as solid pixels, no grid lines. Used for pack thumbnails and the
 * solved reveal. [revealRows] limits drawing to the top rows for the reveal animation (0..size).
 */
@Composable
fun PicturePreview(
    solution: Solution,
    color: Color,
    modifier: Modifier = Modifier,
    background: Color = Color.Transparent,
    revealRows: Float = solution.size.toFloat(),
) {
    Canvas(modifier) {
        val n = solution.size
        val cell = min(size.width, size.height) / n
        val originX = (size.width - cell * n) / 2
        val originY = (size.height - cell * n) / 2
        if (background != Color.Transparent) {
            drawRect(background, Offset(originX, originY), Size(cell * n, cell * n))
        }
        for (r in 0 until n) {
            val rowAlpha = (revealRows - r).coerceIn(0f, 1f)
            if (rowAlpha <= 0f) break
            for (c in 0 until n) {
                if (solution[r, c]) {
                    drawRect(
                        color = color.copy(alpha = color.alpha * rowAlpha),
                        topLeft = Offset(originX + c * cell, originY + r * cell),
                        size = Size(cell + 0.5f, cell + 0.5f),
                    )
                }
            }
        }
    }
}
