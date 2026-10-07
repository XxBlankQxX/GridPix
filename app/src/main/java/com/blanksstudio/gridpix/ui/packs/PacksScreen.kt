package com.blanksstudio.gridpix.ui.packs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blanksstudio.gridpix.R
import com.blanksstudio.gridpix.ui.common.PackCardView
import com.blanksstudio.gridpix.ui.common.PicturePreview
import com.blanksstudio.gridpix.ui.common.ScreenScaffold
import com.blanksstudio.gridpix.ui.theme.Accents

@Composable
fun PacksScreen(
    onBack: () -> Unit,
    onOpenPack: (packId: String) -> Unit,
    onShop: () -> Unit,
    viewModel: PacksViewModel = hiltViewModel(),
) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    ScreenScaffold(title = stringResource(R.string.packs_title), onBack = onBack) { padding ->
        val list = cards
        if (list == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@ScreenScaffold
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(list, key = { it.id }) { card ->
                PackCardView(card, onClick = { onOpenPack(card.id) })
            }
        }
    }
}

@Composable
fun PackPuzzlesScreen(
    onBack: () -> Unit,
    onOpenPuzzle: (packId: String, index: Int) -> Unit,
    onShop: () -> Unit,
    viewModel: PackPuzzlesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pack = state.pack
    ScreenScaffold(title = pack?.name ?: "", onBack = onBack) { padding ->
        if (pack == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@ScreenScaffold
        }
        val accent = Accents.forPack(pack.id)
        val solvedCount = state.tiles.count { it.solved }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(96.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Accents.gradient(accent))
                        .then(if (!state.unlocked) Modifier.clickable(onClick = onShop) else Modifier)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.packs_progress, solvedCount, pack.puzzles.size), style = MaterialTheme.typography.titleMedium, color = Color.White)
                    LinearProgressIndicator(
                        progress = { solvedCount.toFloat() / pack.puzzles.size },
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f),
                        drawStopIndicator = {},
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                    )
                    if (!state.unlocked) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.pack_locked_hint), color = Color.White, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            items(state.tiles, key = { it.index }) { tile ->
                val description = if (tile.solved) stringResource(R.string.pack_thumbnail_solved, tile.name)
                else stringResource(R.string.pack_thumbnail_unsolved, tile.index)
                Box(
                    Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (tile.solved) MaterialTheme.colorScheme.surfaceContainerLowest else accent.copy(alpha = if (tile.started) 0.28f else 0.14f))
                        .clickable { if (state.unlocked) onOpenPuzzle(pack.id, tile.index) else onShop() }
                        .semantics { contentDescription = description },
                    contentAlignment = Alignment.Center,
                ) {
                    if (tile.solved) {
                        PicturePreview(tile.solution, accent, Modifier.fillMaxSize().padding(10.dp), colors = tile.colors)
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp),
                        )
                    } else {
                        Text(tile.index.toString(), style = MaterialTheme.typography.headlineSmall, color = accent)
                        if (!state.unlocked) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = accent, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
