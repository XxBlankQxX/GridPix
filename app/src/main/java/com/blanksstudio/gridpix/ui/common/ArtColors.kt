package com.blanksstudio.gridpix.ui.common

import com.blanksstudio.gridpix.data.packs.PixelColors
import com.blanksstudio.gridpix.game.Solution
import kotlin.random.Random

/**
 * Colours for pictures that have no hand-made colour map: generated endless and daily puzzles and the
 * tutorial. A seeded two-colour diagonal gradient with a soft checker shade, so every random picture
 * still gets a colourful, reproducible reveal.
 */
object ArtColors {

    private val gradients = listOf(
        0xFF6C4DF6 to 0xFFFF4F8B,
        0xFF2F7FED to 0xFF5DE0CB,
        0xFFF08A24 to 0xFFFF4F8B,
        0xFF27A35A to 0xFFF5D547,
        0xFFE5484D to 0xFFF5A524,
        0xFF12A594 to 0xFF6C4DF6,
        0xFFFF4F9A to 0xFFFFB86B,
        0xFF3A5BD9 to 0xFFB04DF0,
    ).map { (a, b) -> a.toInt() to b.toInt() }

    fun generated(solution: Solution, seed: Long): PixelColors {
        val (from, to) = gradients[Random(seed).nextInt(gradients.size)]
        val n = solution.size
        val argb = IntArray(n * n)
        val span = (2 * n - 2).coerceAtLeast(1)
        for (r in 0 until n) for (c in 0 until n) {
            if (!solution[r, c]) continue
            val t = (r + c).toFloat() / span
            val base = lerp(from, to, t)
            argb[r * n + c] = if ((r + c) % 2 == 0) base else shade(base, 0.92f)
        }
        return PixelColors(n, argb)
    }

    private fun lerp(a: Int, b: Int, t: Float): Int {
        fun ch(x: Int, shift: Int) = (x shr shift) and 0xFF
        fun mix(shift: Int) = (ch(a, shift) + (ch(b, shift) - ch(a, shift)) * t).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }

    private fun shade(c: Int, f: Float): Int {
        fun ch(shift: Int) = (((c shr shift) and 0xFF) * f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }
}
