package com.blanksstudio.gridpix.ui.common

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.blanksstudio.gridpix.R
import com.blanksstudio.gridpix.data.packs.PixelColors
import com.blanksstudio.gridpix.game.Solution
import java.io.File

/**
 * Renders a solved picture as a 1080x1080 card (pack-colour gradient, the picture in full colour,
 * its name and "Made in GridPix") and opens the Android share sheet. The image is written to the
 * app's cache and shared through the FileProvider; no permissions and no network involved.
 * Hidden entirely when the player turns sharing off in Settings.
 */
object SharePicture {

    private const val SIZE = 1080
    const val PLAY_URL = "https://play.google.com/store/apps/details?id=com.blanksstudio.gridpix"

    fun share(context: Context, solution: Solution, colors: PixelColors?, accentArgb: Int, title: String, message: String) {
        val bitmap = render(solution, colors, accentArgb, title, context.getString(R.string.share_made_in))
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "gridpix_picture.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, context.getString(R.string.share_chooser)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun render(solution: Solution, colors: PixelColors?, accent: Int, title: String, footer: String): Bitmap {
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, SIZE.toFloat(), SIZE.toFloat(), accent, lighten(accent), Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, SIZE.toFloat(), SIZE.toFloat(), bg)

        val card = RectF(120f, 90f, SIZE - 120f, SIZE - 210f)
        c.drawRoundRect(card, 64f, 64f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })

        val n = solution.size
        val area = card.width() - 120f
        val cell = area / n
        val ox = card.left + 60f
        val oy = card.top + (card.height() - cell * n) / 2
        val px = Paint(Paint.ANTI_ALIAS_FLAG)
        for (r in 0 until n) for (col in 0 until n) {
            if (!solution[r, col]) continue
            px.color = colors?.get(r, col)?.takeIf { it != 0 } ?: accent
            val inset = cell * 0.05f
            c.drawRoundRect(
                RectF(ox + col * cell + inset, oy + r * cell + inset, ox + (col + 1) * cell - inset, oy + (r + 1) * cell - inset),
                cell * 0.18f, cell * 0.18f, px,
            )
        }

        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 72f
        }
        c.drawText(title, SIZE / 2f, SIZE - 120f, text)
        text.typeface = Typeface.DEFAULT
        text.textSize = 40f
        text.alpha = 230
        c.drawText(footer, SIZE / 2f, SIZE - 55f, text)
        return bmp
    }

    private fun lighten(argb: Int): Int {
        fun ch(shift: Int, f: Float): Int { val v = (argb shr shift) and 0xFF; return (v + (255 - v) * f).toInt().coerceIn(0, 255) }
        return (0xFF shl 24) or (ch(16, 0.35f) shl 16) or (ch(8, 0.15f) shl 8) or ch(0, 0.45f)
    }
}
