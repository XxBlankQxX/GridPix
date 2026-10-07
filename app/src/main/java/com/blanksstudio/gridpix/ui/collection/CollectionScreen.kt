package com.blanksstudio.gridpix.ui.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.blanksstudio.gridpix.R
import com.blanksstudio.gridpix.billing.Entitlements
import com.blanksstudio.gridpix.data.ProgressRepository
import com.blanksstudio.gridpix.data.packs.Pack
import com.blanksstudio.gridpix.data.packs.PackRepository
import com.blanksstudio.gridpix.data.settings.SettingsRepository
import com.blanksstudio.gridpix.ui.common.PicturePreview
import com.blanksstudio.gridpix.ui.common.ScreenScaffold
import com.blanksstudio.gridpix.ui.theme.Accents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CollectionSection(val pack: Pack, val unlocked: Boolean, val solved: Set<Int>)

@HiltViewModel
class CollectionViewModel @Inject constructor(
    packs: PackRepository,
    progress: ProgressRepository,
    settings: SettingsRepository,
) : ViewModel() {
    val sections: StateFlow<List<CollectionSection>> = flow { emit(packs.packs()) }
        .flatMapLatest { list ->
            combine(progress.observePack("pack-"), settings.ownedProducts) { rows, owned ->
                val solvedIds = rows.filter { it.solved }.map { it.puzzleId }.toSet()
                list.map { pack ->
                    CollectionSection(
                        pack = pack,
                        unlocked = Entitlements.packUnlocked(owned, pack.productId),
                        solved = pack.puzzles.filter { it.toPuzzle(pack.id).id in solvedIds }.map { it.index }.toSet(),
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/** Every pack picture in one gallery: solved ones in full colour, the rest as numbered blanks. */
@Composable
fun CollectionScreen(
    onBack: () -> Unit,
    onOpenPuzzle: (packId: String, index: Int) -> Unit,
    onShop: () -> Unit,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val collected = sections.sumOf { it.solved.size }
    val total = sections.sumOf { it.pack.puzzles.size }

    ScreenScaffold(title = stringResource(R.string.collection_title), onBack = onBack) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(72.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.collection_count, collected, total), style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(
                        progress = { if (total == 0) 0f else collected.toFloat() / total },
                        drawStopIndicator = {},
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)),
                    )
                }
            }
            sections.forEach { section ->
                val accent = Accents.forPack(section.pack.id)
                item(span = { GridItemSpan(maxLineSpan) }, key = "h-${section.pack.id}") {
                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(accent))
                        Text(section.pack.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp).weight(1f))
                        if (!section.unlocked) Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.locked), modifier = Modifier.size(18.dp))
                        Text(
                            stringResource(R.string.collection_pack_count, section.solved.size, section.pack.puzzles.size),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                items(section.pack.puzzles, key = { "${section.pack.id}-${it.index}" }) { p ->
                    val solved = p.index in section.solved
                    Box(
                        Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (solved) MaterialTheme.colorScheme.surfaceContainerLowest else accent.copy(alpha = 0.14f))
                            .clickable { if (section.unlocked) onOpenPuzzle(section.pack.id, p.index) else onShop() },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (solved) {
                            PicturePreview(p.solution, accent, Modifier.fillMaxSize().padding(6.dp), colors = p.colors)
                        } else {
                            Text(p.index.toString(), style = MaterialTheme.typography.titleMedium, color = accent)
                        }
                    }
                }
            }
        }
    }
}
