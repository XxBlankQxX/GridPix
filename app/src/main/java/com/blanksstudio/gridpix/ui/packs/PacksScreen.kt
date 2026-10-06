package com.blanksstudio.gridpix.ui.packs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blanksstudio.gridpix.R
import com.blanksstudio.gridpix.ui.common.PicturePreview
import com.blanksstudio.gridpix.ui.common.ScreenScaffold

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
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(list, key = { it.id }) { card ->
                Card(onClick = { onOpenPack(card.id) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(
                            Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            val cover = card.cover
                            if (cover != null) {
                                PicturePreview(cover, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.fillMaxSize().padding(6.dp))
                            } else if (!card.unlocked) {
                                Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.locked))
                            } else {
                                Text(card.name.take(1), style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(card.name, style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.packs_progress, card.solved, card.total), style = MaterialTheme.typography.bodySmall)
                            LinearProgressIndicator(progress = { if (card.total == 0) 0f else card.solved.toFloat() / card.total }, modifier = Modifier.fillMaxWidth())
                        }
                        Text(
                            when {
                                card.productId == null -> stringResource(R.string.free)
                                card.unlocked -> stringResource(R.string.owned)
                                else -> card.price ?: stringResource(R.string.locked)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
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
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!state.unlocked) {
                Card(onClick = onShop, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Text(stringResource(R.string.pack_locked_hint))
                    }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(96.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.tiles, key = { it.index }) { tile ->
                    val description = if (tile.solved) stringResource(R.string.pack_thumbnail_solved, tile.name)
                    else stringResource(R.string.pack_thumbnail_unsolved, tile.index)
                    Card(
                        onClick = { if (state.unlocked) onOpenPuzzle(pack.id, tile.index) else onShop() },
                        modifier = Modifier
                            .aspectRatio(1f)
                            .semantics { contentDescription = description },
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (tile.solved) {
                                PicturePreview(tile.solution, MaterialTheme.colorScheme.onSurface, Modifier.fillMaxSize().padding(8.dp))
                            } else {
                                Text(
                                    stringResource(R.string.pack_puzzle_number, tile.index),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = if (tile.started) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (!state.unlocked) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp))
                                }
                            }
                            if (tile.solved) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
