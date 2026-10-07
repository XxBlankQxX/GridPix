package com.blanksstudio.gridpix.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.blanksstudio.gridpix.R
import com.blanksstudio.gridpix.ui.packs.PackCard
import com.blanksstudio.gridpix.ui.theme.Accents
import kotlin.random.Random

/** Colourful pack card: gradient in the pack colour, a solved picture (or mosaic) as cover, progress and price. */
@Composable
fun PackCardView(card: PackCard, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = Accents.forPack(card.id)
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Accents.gradient(accent))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center,
        ) {
            val cover = card.cover
            if (cover != null) {
                PicturePreview(cover.solution, accent, Modifier.fillMaxSize().padding(10.dp), colors = cover.colors, roundedPixels = true)
            } else {
                MosaicPlaceholder(accent, card.id.hashCode().toLong(), Modifier.fillMaxSize().padding(10.dp))
            }
            if (!card.unlocked) {
                Surface(shape = RoundedCornerShape(50), color = Color(0xCC1C1A27), modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.locked), tint = Color.White, modifier = Modifier.padding(6.dp).size(16.dp))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                card.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                when {
                    card.productId == null -> stringResource(R.string.free)
                    card.unlocked -> stringResource(R.string.home_pack_progress, card.solved, card.total)
                    else -> card.price ?: stringResource(R.string.locked)
                },
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
        }
        LinearProgressIndicator(
            progress = { if (card.total == 0) 0f else card.solved.toFloat() / card.total },
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.3f),
            drawStopIndicator = {},
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
        )
    }
}

/** A soft random mosaic in the pack colour, shown before any picture of the pack is solved. */
@Composable
fun MosaicPlaceholder(accent: Color, seed: Long, modifier: Modifier = Modifier, cells: Int = 6) {
    Canvas(modifier) {
        val rnd = Random(seed)
        val cell = size.minDimension / cells
        val ox = (size.width - cell * cells) / 2
        val oy = (size.height - cell * cells) / 2
        for (r in 0 until cells) for (c in 0 until cells) {
            val a = 0.12f + rnd.nextFloat() * 0.5f
            drawRoundRect(
                accent.copy(alpha = a),
                Offset(ox + c * cell + cell * 0.08f, oy + r * cell + cell * 0.08f),
                Size(cell * 0.84f, cell * 0.84f),
                CornerRadius(cell * 0.2f),
            )
        }
    }
}
