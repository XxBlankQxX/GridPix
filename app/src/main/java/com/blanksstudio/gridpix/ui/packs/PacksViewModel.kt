package com.blanksstudio.gridpix.ui.packs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blanksstudio.gridpix.billing.BillingManager
import com.blanksstudio.gridpix.billing.Entitlements
import com.blanksstudio.gridpix.data.ProgressRepository
import com.blanksstudio.gridpix.data.packs.Pack
import com.blanksstudio.gridpix.data.packs.PackRepository
import com.blanksstudio.gridpix.data.settings.SettingsRepository
import com.blanksstudio.gridpix.game.Solution
import com.blanksstudio.gridpix.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PackCard(
    val id: String,
    val name: String,
    val productId: String?,
    val total: Int,
    val solved: Int,
    val unlocked: Boolean,
    val price: String?,
    val cover: Solution?,
)

@HiltViewModel
class PacksViewModel @Inject constructor(
    packs: PackRepository,
    progress: ProgressRepository,
    settings: SettingsRepository,
    billing: BillingManager,
) : ViewModel() {

    val cards: StateFlow<List<PackCard>?> = flow { emit(packs.packs()) }
        .flatMapLatest { list ->
            val solvedFlows = list.map { pack -> progress.observePack(pack.idPrefix) }
            combine(combine(solvedFlows) { it.toList() }, settings.ownedProducts, billing.prices) { rows, owned, prices ->
                list.mapIndexed { i, pack ->
                    val solvedIds = rows[i].filter { it.solved }.map { it.puzzleId }.toSet()
                    PackCard(
                        id = pack.id,
                        name = pack.name,
                        productId = pack.productId,
                        total = pack.puzzles.size,
                        solved = solvedIds.size,
                        unlocked = Entitlements.packUnlocked(owned, pack.productId),
                        price = pack.productId?.let { prices[it] },
                        cover = pack.puzzles.firstOrNull { it.toPuzzle(pack.id).id in solvedIds }?.solution,
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

data class PuzzleTile(
    val index: Int,
    val name: String,
    val solution: Solution,
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
                        PuzzleTile(p.index, p.name, p.solution, solved = row?.solved == true, started = row != null)
                    },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PackPuzzlesUiState())
}
