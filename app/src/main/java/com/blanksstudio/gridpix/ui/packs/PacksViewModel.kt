package com.blanksstudio.gridpix.ui.packs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blanksstudio.gridpix.billing.BillingManager
import com.blanksstudio.gridpix.billing.Entitlements
import com.blanksstudio.gridpix.data.ProgressRepository
import com.blanksstudio.gridpix.data.packs.Pack
import com.blanksstudio.gridpix.data.packs.PackRepository
import com.blanksstudio.gridpix.data.packs.PixelColors
import com.blanksstudio.gridpix.data.settings.SettingsRepository
import com.blanksstudio.gridpix.game.Solution
import com.blanksstudio.gridpix.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PackCover(val solution: Solution, val colors: PixelColors?)

data class PackCard(
    val id: String,
    val name: String,
    val productId: String?,
    val total: Int,
    val solved: Int,
    val unlocked: Boolean,
    val price: String?,
    /** Most recently listed solved picture, shown in colour on the card; null until one is solved. */
    val cover: PackCover?,
    val seasonal: Boolean = false,
)

/** Pack cards with progress, ownership and Play prices. Shared by Home (carousel) and the Packs screen. */
fun packCardsFlow(
    packs: PackRepository,
    progress: ProgressRepository,
    settings: SettingsRepository,
    billing: BillingManager,
): Flow<List<PackCard>> = flow { emit(packs.visiblePacks()) }
    .flatMapLatest { list ->
        if (list.isEmpty()) return@flatMapLatest flowOf(emptyList())
        val solvedFlows = list.map { pack -> progress.observePack(pack.idPrefix) }
        combine(combine(solvedFlows) { it.toList() }, settings.ownedProducts, billing.prices) { rows, owned, prices ->
            list.mapIndexed { i, pack ->
                val solvedIds = rows[i].filter { it.solved }.map { it.puzzleId }.toSet()
                val coverPuzzle = pack.puzzles.lastOrNull { it.toPuzzle(pack.id).id in solvedIds }
                PackCard(
                    id = pack.id,
                    name = pack.name,
                    productId = pack.productId,
                    total = pack.puzzles.size,
                    solved = solvedIds.size,
                    unlocked = Entitlements.packUnlocked(owned, pack.productId),
                    price = pack.productId?.let { prices[it] },
                    cover = coverPuzzle?.let { PackCover(it.solution, it.colors) },
                    seasonal = pack.isSeasonal,
                )
            }
        }
    }

@HiltViewModel
class PacksViewModel @Inject constructor(
    packs: PackRepository,
    progress: ProgressRepository,
    settings: SettingsRepository,
    billing: BillingManager,
) : ViewModel() {

    init {
        billing.refreshProducts() // pack prices on the cards come from Play
    }

    val cards: StateFlow<List<PackCard>?> = packCardsFlow(packs, progress, settings, billing)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

data class PuzzleTile(
    val index: Int,
    val name: String,
    val solution: Solution,
    val colors: PixelColors?,
    val solved: Boolean,
    val started: Boolean,
)

data class PackPuzzlesUiState(
    val pack: Pack? = null,
    val unlocked: Boolean = false,
    val tiles: List<PuzzleTile> = emptyList(),
)

@HiltViewModel
class PackPuzzlesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    packs: PackRepository,
    progress: ProgressRepository,
    settings: SettingsRepository,
) : ViewModel() {

    private val packId: String = checkNotNull(savedStateHandle[Routes.ARG_PACK_ID])

    val uiState: StateFlow<PackPuzzlesUiState> = flow { emit(packs.pack(packId)) }
        .flatMapLatest { pack ->
            if (pack == null) return@flatMapLatest flowOf(PackPuzzlesUiState())
            combine(progress.observePack(pack.idPrefix), settings.ownedProducts) { rows, owned ->
                val byId = rows.associateBy { it.puzzleId }
                PackPuzzlesUiState(
                    pack = pack,
                    unlocked = Entitlements.packUnlocked(owned, pack.productId),
                    tiles = pack.puzzles.map { p ->
                        val row = byId[p.toPuzzle(pack.id).id]
                        PuzzleTile(p.index, p.name, p.solution, p.colors, solved = row?.solved == true, started = row != null)
                    },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PackPuzzlesUiState())
}
