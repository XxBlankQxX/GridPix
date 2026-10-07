package com.blanksstudio.gridpix.ui.puzzle

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blanksstudio.gridpix.billing.Entitlements
import com.blanksstudio.gridpix.data.PlayerProgressRepository
import com.blanksstudio.gridpix.data.ProgressRepository
import com.blanksstudio.gridpix.data.packs.PixelColors
import com.blanksstudio.gridpix.data.rules.Achievement
import com.blanksstudio.gridpix.data.rules.LevelInfo
import com.blanksstudio.gridpix.data.rules.ProgressSnapshot
import com.blanksstudio.gridpix.ui.common.ArtColors
import com.blanksstudio.gridpix.data.packs.PackRepository
import com.blanksstudio.gridpix.data.rules.HintRules
import com.blanksstudio.gridpix.data.settings.GameSettings
import com.blanksstudio.gridpix.data.settings.SettingsRepository
import com.blanksstudio.gridpix.data.settings.ThemeMode
import com.blanksstudio.gridpix.game.AutoCross
import com.blanksstudio.gridpix.game.CellState
import com.blanksstudio.gridpix.game.DailyPuzzle
import com.blanksstudio.gridpix.game.GridState
import com.blanksstudio.gridpix.game.HintSelector
import com.blanksstudio.gridpix.game.Puzzle
import com.blanksstudio.gridpix.game.PuzzleGenerator
import com.blanksstudio.gridpix.game.WinChecker
import com.blanksstudio.gridpix.ui.navigation.Routes
import com.blanksstudio.gridpix.util.LocalDates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.random.Random

enum class PaintMode { FILL, MARK }

/** What kind of puzzle the screen is showing; drives the title and the "Next" action. */
sealed interface PuzzleKind {
    data class Endless(val size: Int, val seed: Long) : PuzzleKind
    data class Daily(val date: String) : PuzzleKind
    data class Pack(val packId: String, val packName: String, val index: Int, val count: Int, val nextUnlocked: Boolean) : PuzzleKind
    data class Tutorial(val step: TutorialStep) : PuzzleKind
}

data class PuzzleUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val kind: PuzzleKind? = null,
    val puzzle: Puzzle? = null,
    val board: GridState = GridState.empty(5),
    val mode: PaintMode = PaintMode.FILL,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val elapsedMs: Long = 0,
    val hintsUsed: Int = 0,
    val hintsAvailable: Int = 0,
    val solved: Boolean = false,
    val paused: Boolean = false,
    val mistakes: Set<Pair<Int, Int>> = emptySet(),
    val lastHint: Pair<Int, Int>? = null,
    val settings: GameSettings = GameSettings(false, true, true, ThemeMode.SYSTEM),
    val streakAfterSolve: Int? = null,
    /** Per-pixel colours for the solved reveal (hand-made for packs, generated otherwise). */
    val colors: PixelColors? = null,
    /** Filled in when this solve just happened (not when reopening an already-solved puzzle). */
    val reward: SolveReward? = null,
)

/** What a fresh solve earned: shown on the solved screen. */
data class SolveReward(
    val xpGained: Int,
    val level: LevelInfo,
    val leveledUp: Boolean,
    val newAchievements: List<Achievement>,
    /** Free hints credited for reaching new levels (HintRules.levelRewards). */
    val hintsEarned: Int = 0,
    /** Ask Google Play for a rating now (5th solve or later, asked once). */
    val askForReview: Boolean = false,
)

sealed interface PuzzleEvent {
    data object OutOfHints : PuzzleEvent
    data object NothingToReveal : PuzzleEvent
    data class CheckResult(val mistakes: Int, val complete: Boolean) : PuzzleEvent
}

@HiltViewModel
class PuzzleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val settings: SettingsRepository,
    private val progress: ProgressRepository,
    private val packs: PackRepository,
    private val playerProgress: PlayerProgressRepository,
) : ViewModel() {

    /** Progress just before this puzzle was solved, to work out XP and new achievements afterwards. */
    private var progressBefore: ProgressSnapshot? = null

    private val kindArg: String = checkNotNull(savedStateHandle[Routes.ARG_KIND])
    private val argA: String = checkNotNull(savedStateHandle[Routes.ARG_A])
    private val argB: String = checkNotNull(savedStateHandle[Routes.ARG_B])

    private val _uiState = MutableStateFlow(PuzzleUiState())
    val uiState: StateFlow<PuzzleUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PuzzleEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<PuzzleEvent> = _events.asSharedFlow()

    private val undoStack = ArrayDeque<GridState>()
    private val redoStack = ArrayDeque<GridState>()
    private var strokeStart: GridState? = null

    private var puzzleId: String = ""
    private var baseElapsedMs = 0L
    private var runningSince: Long? = null
    private var tickerJob: Job? = null
    private var screenVisible = false

    init {
        viewModelScope.launch { load() }
        viewModelScope.launch { settings.settings.collect { s -> _uiState.update { it.copy(settings = s) } } }
        viewModelScope.launch {
            settings.hintWallet.collect { w ->
                _uiState.update { it.copy(hintsAvailable = HintRules.available(w, LocalDates.today())) }
            }
        }
    }

    private suspend fun load() {
        val (kind, puzzle, colors) = try {
            withContext(Dispatchers.Default) { resolve() }
        } catch (e: Exception) {
            _uiState.update { it.copy(loading = false, error = true) }
            return
        } ?: run {
            _uiState.update { it.copy(loading = false, error = true) }
            return
        }
        puzzleId = puzzle.id
        val saved = progress.get(puzzleId)
        val board = saved?.takeIf { it.size == puzzle.size }?.let { runCatching { GridState.decode(it.size, it.state) }.getOrNull() }
            ?: GridState.empty(puzzle.size)
        baseElapsedMs = saved?.elapsedMs ?: 0L
        val solved = saved?.solved == true || WinChecker.isSolved(puzzle.solution, board)
        _uiState.update {
            it.copy(
                loading = false,
                kind = kind,
                puzzle = puzzle,
                colors = colors,
                board = board,
                elapsedMs = baseElapsedMs,
                hintsUsed = saved?.hintsUsed ?: 0,
                solved = solved,
                mistakes = mistakesFor(puzzle, board, it.settings),
            )
        }
        if (!solved) {
            if (kind !is PuzzleKind.Tutorial) progressBefore = playerProgress.current()
            startTimerIfVisible()
        }
    }

    private suspend fun resolve(): Triple<PuzzleKind, Puzzle, PixelColors>? = when (kindArg) {
        Routes.KIND_ENDLESS -> {
            val size = argA.toInt()
            val seed = argB.toLong()
            val puzzle = PuzzleGenerator.generate(size, seed)
            Triple(PuzzleKind.Endless(size, seed), puzzle, ArtColors.generated(puzzle.solution, seed))
        }
        Routes.KIND_DAILY -> {
            val puzzle = DailyPuzzle.generate(argA)
            Triple(PuzzleKind.Daily(argA), puzzle, ArtColors.generated(puzzle.solution, DailyPuzzle.seedFor(argA)))
        }
        Routes.KIND_PACK -> {
            val pack = packs.pack(argA) ?: return null
            val index = argB.toInt()
            val entry = pack.puzzles.getOrNull(index - 1) ?: return null
            val owned = settings.snapshotOwned()
            val unlocked = Entitlements.packUnlocked(owned, pack.productId)
            Triple(
                PuzzleKind.Pack(pack.id, pack.name, index, pack.puzzles.size, unlocked && index < pack.puzzles.size),
                entry.toPuzzle(pack.id),
                entry.colors ?: ArtColors.generated(entry.solution, index.toLong()),
            )
        }
        Routes.KIND_TUTORIAL -> {
            val step = TutorialSteps.step(argA.toInt()) ?: return null
            Triple(PuzzleKind.Tutorial(step), step.puzzle, ArtColors.generated(step.puzzle.solution, step.step.toLong()))
        }
        else -> null
    }

    // ---- Timer (SPEC section 2: pauses when the app backgrounds) ------------------------------

    fun onScreenVisible() {
        screenVisible = true
        startTimerIfVisible()
    }

    fun onScreenHidden() {
        screenVisible = false
        stopTimer()
        viewModelScope.launch { persist() }
    }

    fun togglePause() {
        val paused = !_uiState.value.paused
        _uiState.update { it.copy(paused = paused) }
        if (paused) stopTimer() else startTimerIfVisible()
    }

    private fun startTimerIfVisible() {
        val s = _uiState.value
        if (!screenVisible || s.solved || s.paused || s.loading || runningSince != null) return
        runningSince = SystemClock.elapsedRealtime()
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                _uiState.update { it.copy(elapsedMs = currentElapsed()) }
                delay(1_000)
            }
        }
    }

    private fun stopTimer() {
        baseElapsedMs = currentElapsed()
        runningSince = null
        tickerJob?.cancel()
        tickerJob = null
        _uiState.update { it.copy(elapsedMs = baseElapsedMs) }
    }

    private fun currentElapsed(): Long =
        baseElapsedMs + (runningSince?.let { SystemClock.elapsedRealtime() - it } ?: 0L)

    // ---- Moves --------------------------------------------------------------------------------

    fun setMode(mode: PaintMode) = _uiState.update { it.copy(mode = mode) }

    /** State a touch should paint, given the cell under the finger and whether the alternate mode is active. */
    fun targetFor(row: Int, col: Int, alternate: Boolean): CellState {
        val s = _uiState.value
        val mode = if (alternate) s.mode.other() else s.mode
        val want = if (mode == PaintMode.FILL) CellState.FILLED else CellState.MARKED
        return if (s.board[row, col] == want) CellState.EMPTY else want
    }

    fun beginStroke() {
        if (_uiState.value.solved || _uiState.value.paused) return
        strokeStart = _uiState.value.board
    }

    /** Paints one cell during a stroke. Only touches cells that were [from] when the stroke began. */
    fun paint(row: Int, col: Int, target: CellState, from: CellState) {
        val s = _uiState.value
        if (s.solved || s.paused || strokeStart == null) return
        val puzzle = s.puzzle ?: return
        if (row !in 0 until puzzle.size || col !in 0 until puzzle.size) return
        if (s.board[row, col] != from) return
        val board = s.board.with(row, col, target)
        if (board === s.board) return
        _uiState.update { it.copy(board = board, mistakes = mistakesFor(puzzle, board, it.settings), lastHint = null) }
    }

    fun endStroke() {
        val start = strokeStart ?: return
        strokeStart = null
        val s = _uiState.value
        if (s.board == start) return
        applyAutoCross()
        undoStack.addLast(start)
        if (undoStack.size > 500) undoStack.removeFirst()
        redoStack.clear()
        afterMove()
    }

    fun undo() {
        val s = _uiState.value
        if (s.solved || undoStack.isEmpty()) return
        redoStack.addLast(s.board)
        applyBoard(undoStack.removeLast())
    }

    fun redo() {
        val s = _uiState.value
        if (s.solved || redoStack.isEmpty()) return
        undoStack.addLast(s.board)
        applyBoard(redoStack.removeLast())
    }

    private fun applyBoard(board: GridState) {
        val puzzle = _uiState.value.puzzle ?: return
        _uiState.update { it.copy(board = board, mistakes = mistakesFor(puzzle, board, it.settings), lastHint = null) }
        afterMove()
    }

    fun useHint() {
        val s = _uiState.value
        val puzzle = s.puzzle ?: return
        if (s.solved || s.paused) return
        val hint = HintSelector.select(puzzle.solution, s.board)
        if (hint == null) {
            _events.tryEmit(PuzzleEvent.NothingToReveal)
            return
        }
        viewModelScope.launch {
            // Tutorial hints are free; everything else spends the wallet (SPEC section 2).
            val spent = s.kind is PuzzleKind.Tutorial || settings.spendHint(LocalDates.today())
            if (!spent) {
                _events.tryEmit(PuzzleEvent.OutOfHints)
                return@launch
            }
            undoStack.addLast(s.board)
            redoStack.clear()
            val board = s.board.with(hint.row, hint.col, hint.state)
            _uiState.update {
                it.copy(
                    board = board,
                    hintsUsed = it.hintsUsed + 1,
                    mistakes = mistakesFor(puzzle, board, it.settings),
                    lastHint = hint.row to hint.col,
                )
            }
            applyAutoCross()
            afterMove()
        }
    }

    /** Setting "Auto-cross finished lines": X the empty cells of lines whose fills match the clue. Same undo step as the move. */
    private fun applyAutoCross() {
        val s = _uiState.value
        val puzzle = s.puzzle ?: return
        if (!s.settings.autoCross) return
        val crossed = AutoCross.apply(puzzle.clues, s.board)
        if (crossed !== s.board) _uiState.update { it.copy(board = crossed) }
    }

    /** "Check on completion" default (SPEC section 2): shows mistakes once, without changing settings. */
    fun check() {
        val s = _uiState.value
        val puzzle = s.puzzle ?: return
        val wrong = WinChecker.mistakes(puzzle.solution, s.board).toSet()
        _uiState.update { it.copy(mistakes = wrong) }
        _events.tryEmit(PuzzleEvent.CheckResult(wrong.size, s.board.count(CellState.FILLED) == puzzle.solution.filledCount))
    }

    fun restart() {
        val puzzle = _uiState.value.puzzle ?: return
        undoStack.clear()
        redoStack.clear()
        baseElapsedMs = 0
        runningSince = null
        tickerJob?.cancel()
        tickerJob = null
        _uiState.update {
            it.copy(board = GridState.empty(puzzle.size), elapsedMs = 0, hintsUsed = 0, solved = false, mistakes = emptySet(), lastHint = null, paused = false)
        }
        viewModelScope.launch { persist() }
        startTimerIfVisible()
    }

    private fun afterMove() {
        _uiState.update { it.copy(canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty()) }
        val s = _uiState.value
        val puzzle = s.puzzle ?: return
        if (!s.solved && WinChecker.isSolved(puzzle.solution, s.board)) {
            stopTimer()
            _uiState.update { it.copy(solved = true, mistakes = emptySet()) }
            viewModelScope.launch {
                persist()
                if (s.kind is PuzzleKind.Daily) {
                    settings.onDailySolved(s.kind.date, LocalDates.yesterdayOf(s.kind.date))
                    _uiState.update { it.copy(streakAfterSolve = settings.streak.first().count) }
                }
                if (s.kind is PuzzleKind.Tutorial && s.kind.step.step == TutorialSteps.COUNT) settings.setTutorialDone()
                val before = progressBefore
                if (before != null) {
                    val after = playerProgress.current()
                    val hintsEarned = settings.claimLevelRewards(after.level.level)
                    val askForReview = after.solvedCount >= REVIEW_AFTER_SOLVES && !settings.reviewAsked()
                    if (askForReview) settings.setReviewAsked()
                    _uiState.update {
                        it.copy(
                            reward = SolveReward(
                                xpGained = after.level.totalXp - before.level.totalXp,
                                level = after.level,
                                leveledUp = after.level.level > before.level.level,
                                newAchievements = (after.unlocked - before.unlocked).sortedBy { a -> a.ordinal },
                                hintsEarned = hintsEarned,
                                askForReview = askForReview,
                            ),
                        )
                    }
                }
            }
        } else {
            viewModelScope.launch { persist() }
        }
    }

    /** SPEC section 2 / section 8: progress is saved on every move so process death loses nothing. */
    private suspend fun persist() {
        val s = _uiState.value
        if (s.puzzle == null || s.kind is PuzzleKind.Tutorial) return
        progress.save(puzzleId, s.board, currentElapsed(), s.hintsUsed, s.solved)
    }

    private fun mistakesFor(puzzle: Puzzle, board: GridState, gameSettings: GameSettings): Set<Pair<Int, Int>> =
        if (gameSettings.highlightMistakes) WinChecker.mistakes(puzzle.solution, board).toSet() else emptySet()

    /** Route for the "Next" button on the solved screen, or null to go home. */
    fun nextRoute(): String? = when (val k = _uiState.value.kind) {
        is PuzzleKind.Endless -> Routes.endless(k.size, Random.nextLong(1, Long.MAX_VALUE))
        is PuzzleKind.Pack -> if (k.nextUnlocked) Routes.packPuzzle(k.packId, k.index + 1) else null
        is PuzzleKind.Tutorial -> if (k.step.step < TutorialSteps.COUNT) Routes.tutorial(k.step.step + 1) else null
        is PuzzleKind.Daily, null -> null
    }

    override fun onCleared() {
        stopTimer()
        super.onCleared()
    }
}

/** Google's rating prompt is offered once, after this many solved puzzles. */
const val REVIEW_AFTER_SOLVES = 5

fun PaintMode.other(): PaintMode = if (this == PaintMode.FILL) PaintMode.MARK else PaintMode.FILL
