package com.blanksstudio.gridpix.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Piece(
    val x: Float, val vx: Float, val vy: Float, val spin: Float, val color: Color, val w: Float, val h: Float, val delay: Float,
)

private val ConfettiColors = listOf(
    Color(0xFF6C4DF6), Color(0xFFFF4F8B), Color(0xFFF5A524), Color(0xFF12A594),
    Color(0xFF2F7FED), Color(0xFFF08A24), Color(0xFF27A35A), Color(0xFFFFD447),
)

/** A one-shot confetti burst from the top of the screen. Purely decorative; draws nothing once finished. */
@Composable
fun ConfettiBurst(modifier: Modifier = Modifier, pieces: Int = 90, durationMs: Int = 2600) {
    val progress = remember { Animatable(0f) }
    val confetti = remember {
        val rnd = Random(System.nanoTime())
        List(pieces) {
            Piece(
                x = rnd.nextFloat(),
                vx = (rnd.nextFloat() - 0.5f) * 0.5f,
                vy = 0.35f + rnd.nextFloat() * 0.5f,
                spin = (rnd.nextFloat() - 0.5f) * 1440f,
                color = ConfettiColors[rnd.nextInt(ConfettiColors.size)],
                w = 6f + rnd.nextFloat() * 8f,
                h = 10f + rnd.nextFloat() * 10f,
                delay = rnd.nextFloat() * 0.25f,
            )
        }
    }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(durationMs, easing = LinearEasing)) }
    if (progress.value >= 1f) return
    Canvas(modifier) {
        val p = progress.value
        for (piece in confetti) {
            val t = ((p - piece.delay) / (1f - piece.delay)).coerceIn(0f, 1f)
            if (t <= 0f) continue
            val x = (piece.x + piece.vx * t + sin(t * 12f + piece.x * 20f) * 0.02f) * size.width
            val y = (-0.05f + piece.vy * t + 0.9f * t * t) * size.height
            val alpha = if (t > 0.8f) (1f - t) / 0.2f else 1f
            rotate(piece.spin * t, Offset(x, y)) {
                drawRect(
                    piece.color.copy(alpha = alpha),
                    Offset(x - piece.w / 2, y - piece.h / 2),
                    Size(piece.w * (0.6f + 0.4f * cos(t * 9f).let { if (it < 0) -it else it }), piece.h),
                )
            }
        }
    }
}
