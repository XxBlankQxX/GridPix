package com.blanksstudio.gridpix.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blanksstudio.gridpix.billing.BillingManager
import com.blanksstudio.gridpix.billing.Entitlements
import com.blanksstudio.gridpix.data.PlayerProgressRepository
import com.blanksstudio.gridpix.data.ProgressRepository
import com.blanksstudio.gridpix.data.packs.PackRepository
import com.blanksstudio.gridpix.data.rules.Achievement
import com.blanksstudio.gridpix.data.rules.CalendarRules
import com.blanksstudio.gridpix.data.rules.HintRules
import com.blanksstudio.gridpix.data.rules.ProgressSnapshot
import com.blanksstudio.gridpix.data.rules.Trophy
import com.blanksstudio.gridpix.data.settings.SettingsRepository
import com.blanksstudio.gridpix.game.PuzzleGenerator
import com.blanksstudio.gridpix.game.PuzzleIds
import com.blanksstudio.gridpix.ui.packs.PackCard
import com.blanksstudio.gridpix.ui.packs.packCardsFlow
import com.blanksstudio.gridpix.util.LocalDates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
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
    val hintsAvailable: Int = HintRules.FREE_PER_DAY,
    val showTutorialCard: Boolean = false,
    val progress: ProgressSnapshot = ProgressSnapshot.EMPTY,
    /** Last 7 days ending today, with whether the daily was solved. */
    val week: List<Pair<LocalDate, Boolean>> = emptyList(),
    val monthTrophy: Trophy = Trophy.NONE,
    val packs: List<PackCard> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settings: SettingsRepository,
    progressRepo: ProgressRepository,
    playerProgress: PlayerProgressRepository,
    packs: PackRepository,
    billing: BillingManager,
) : ViewModel() {

    private val today = LocalDates.today()

    private val dailyState = progressRepo.observe(PuzzleIds.daily(today)).map { row ->
        when {
            row == null -> DailyState.NEW
            row.solved -> DailyState.SOLVED
            else -> DailyState.IN_PROGRESS
        }
    }

    private val continueEndless = progressRepo.observeLatestUnsolvedEndless().map { row ->
        row?.puzzleId?.removePrefix("endless-")?.split("-")?.takeIf { it.size == 2 }?.let { (size, seed) ->
            size.toIntOrNull()?.let { s -> seed.toLongOrNull()?.let { s to it } }
        }
    }

    private val settingsPart = combine(
        settings.lastSize,
        settings.ownedProducts,
        settings.hintWallet,
        settings.tutorialDone,
    ) { lastSize, owned, wallet, tutorialDone ->
        HomeUiState(
            loading = false,
            selectedSize = lastSize,
            unlockedSizes = PuzzleGenerator.SIZES.filter { Entitlements.sizeUnlocked(owned, it) }.toSet(),
            today = today,
            hintsAvailable = HintRules.available(wallet, today),
            showTutorialCard = !tutorialDone,
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        settingsPart,
        dailyState,
        continueEndless,
        playerProgress.progress,
        packCardsFlow(packs, progressRepo, settings, billing),
    ) { base, daily, cont, progress, packCards ->
        val todayDate = LocalDate.parse(today)
        base.copy(
            dailyState = daily,
            continueSize = cont?.first,
            continueSeed = cont?.second,
            progress = progress,
            week = (6 downTo 0).map { todayDate.minusDays(it.toLong()) }.map { it to (it in progress.dailyDays) },
            monthTrophy = CalendarRules.trophy(YearMonth.from(todayDate), progress.dailyDays),
            packs = packCards,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    val badgeTotal: Int = Achievement.entries.size

    fun selectSize(size: Int) {
        viewModelScope.launch { settings.setLastSize(size) }
    }

    /** New endless puzzle: a fresh random seed; the puzzle itself is reproducible from it. */
    fun newSeed(): Long = Random.nextLong(1, Long.MAX_VALUE)
}
