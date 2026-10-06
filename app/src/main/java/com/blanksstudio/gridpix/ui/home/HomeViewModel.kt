package com.blanksstudio.gridpix.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blanksstudio.gridpix.billing.Entitlements
import com.blanksstudio.gridpix.data.ProgressRepository
import com.blanksstudio.gridpix.data.rules.HintRules
import com.blanksstudio.gridpix.data.rules.StreakRules
import com.blanksstudio.gridpix.data.settings.SettingsRepository
import com.blanksstudio.gridpix.game.PuzzleGenerator
import com.blanksstudio.gridpix.game.PuzzleIds
import com.blanksstudio.gridpix.util.LocalDates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

enum class DailyState { NEW, IN_PROGRESS, SOLVED }

data class HomeUiState(
    val loading: Boolean = true,
    val sizes: List<Int> = PuzzleGenerator.SIZES,
    val selectedSize: Int = 10,
    val unlockedSizes: Set<Int> = setOf(5, 10),
    val continueSize: Int? = null,
    val continueSeed: Long? = null,
    val today: String = LocalDates.today(),
    val dailyState: DailyState = DailyState.NEW,
    val streak: Int = 0,
    val hintsAvailable: Int = HintRules.FREE_PER_DAY,
    val showTutorialCard: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val progress: ProgressRepository,
) : ViewModel() {

    private val today = LocalDates.today()
    private val yesterday = LocalDates.yesterday()

    private val dailyState = progress.observe(PuzzleIds.daily(today)).map { row ->
        when {
            row == null -> DailyState.NEW
            row.solved -> DailyState.SOLVED
            else -> DailyState.IN_PROGRESS
        }
    }

    private val continueEndless = progress.observeLatestUnsolvedEndless().map { row ->
        row?.puzzleId?.removePrefix("endless-")?.split("-")?.takeIf { it.size == 2 }?.let { (size, seed) ->
            size.toIntOrNull()?.let { s -> seed.toLongOrNull()?.let { s to it } }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        settings.lastSize,
        settings.ownedProducts,
        settings.hintWallet,
        settings.streak,
        settings.tutorialDone,
    ) { lastSize, owned, wallet, streak, tutorialDone ->
        HomeUiState(
            loading = false,
            selectedSize = lastSize,
            unlockedSizes = PuzzleGenerator.SIZES.filter { Entitlements.sizeUnlocked(owned, it) }.toSet(),
            today = today,
            streak = StreakRules.current(streak, today, yesterday),
            hintsAvailable = HintRules.available(wallet, today),
            showTutorialCard = !tutorialDone,
        )
    }.flatMapLatest { base ->
        combine(dailyState, continueEndless) { daily, cont ->
            base.copy(dailyState = daily, continueSize = cont?.first, continueSeed = cont?.second)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun selectSize(size: Int) {
        viewModelScope.launch { settings.setLastSize(size) }
    }

    /** New endless puzzle: a fresh random seed; the puzzle itself is reproducible from it. */
    fun newSeed(): Long = Random.nextLong(1, Long.MAX_VALUE)
}
