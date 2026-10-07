package com.blanksstudio.gridpix.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.blanksstudio.gridpix.data.packs.PixelColors
import com.blanksstudio.gridpix.game.Solution
import kotlin.math.min

/**
 * Draws a finished picture as pixels. With [colors] each pixel gets its own colour (the reveal,
 * collection and thumbnails); otherwise every filled pixel uses [color].
 *
 * [reveal] animates the solved reveal from 0 to 1: pixels pop in along a diagonal wave from the
 * top-left corner. Leave it at 1 for a static picture.
 */
@Composable
fun PicturePreview(
    solution: Solution,
    color: Color,
    modifier: Modifier = Modifier,
    colors: PixelColors? = null,
    background: Color = Color.Transparent,
    reveal: Float = 1f,
    roundedPixels: Boolean = false,
) {
    Canvas(modifier) {
        val n = solution.size
        val cell = min(size.width, size.height) / n
        val originX = (size.width - cell * n) / 2
        val originY = (size.height - cell * n) / 2
        if (background != Color.Transparent) {
            drawRoundRect(background, Offset(originX, originY), Size(cell * n, cell * n), CornerRadius(cell * 0.6f))
        }
        val wave = reveal * (2 * n + 2)
        val corner = if (roundedPixels) CornerRadius(cell * 0.18f) else CornerRadius.Zero
        for (r in 0 until n) {
            for (c in 0 until n) {
                if (!solution[r, c]) continue
                val t = (wave - (r + c)).coerceIn(0f, 1f)
                if (t <= 0f) continue
                val px = colors?.get(r, c)?.takeIf { it != 0 }?.let { Color(it) } ?: color
                // Pop in: scale from 0 to slightly over 1 and settle.
                val scale = if (t >= 1f) 1f else (t * 1.15f).coerceAtMost(1.15f)
                val inset = if (roundedPixels) cell * 0.06f else 0f
                val s = (cell - 2 * inset) * scale
                val cx = originX + c * cell + cell / 2
                val cy = originY + r * cell + cell / 2
                // +0.5 avoids hairline gaps between square pixels.
                val extra = if (roundedPixels) 0f else 0.5f
                drawRoundRect(px, Offset(cx - s / 2, cy - s / 2), Size(s + extra, s + extra), corner)
            }
        }
    }
}
