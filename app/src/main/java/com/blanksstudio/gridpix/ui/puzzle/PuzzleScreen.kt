package com.blanksstudio.gridpix.ui.puzzle

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blanksstudio.gridpix.R
import com.blanksstudio.gridpix.ui.common.PicturePreview
import com.blanksstudio.gridpix.ui.common.rememberFeedback
import com.blanksstudio.gridpix.ui.theme.BoardColors
import com.blanksstudio.gridpix.ui.theme.isDarkTheme
import com.blanksstudio.gridpix.util.formatElapsed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleScreen(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onShop: () -> Unit,
    onOpenPuzzle: (route: String) -> Unit,
    viewModel: PuzzleViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val feedback = rememberFeedback(state.settings.haptics, state.settings.sound)
    var showHintDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }

    // Timer follows the screen lifecycle (SPEC section 2: pauses when the app backgrounds).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onScreenVisible()
                Lifecycle.Event.ON_PAUSE -> viewModel.onScreenHidden()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onScreenHidden()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                PuzzleEvent.OutOfHints -> showHintDialog = true
                PuzzleEvent.NothingToReveal -> snackbar.showSnackbar(context.getString(R.string.puzzle_hint_nothing))
                is PuzzleEvent.CheckResult -> snackbar.showSnackbar(
                    when {
                        event.mistakes > 0 -> context.getString(R.string.puzzle_check_mistakes, event.mistakes)
                        event.complete -> context.getString(R.string.puzzle_check_ok)
                        else -> context.getString(R.string.puzzle_check_incomplete)
                    },
                )
            }
        }
    }

    LaunchedEffect(state.solved) { if (state.solved && !state.loading) feedback.win() }

    val title = when (val k = state.kind) {
        is PuzzleKind.Endless -> stringResource(R.string.puzzle_title_endless, k.size)
        is PuzzleKind.Daily -> stringResource(R.string.puzzle_title_daily, k.date)
        is PuzzleKind.Pack -> stringResource(R.string.puzzle_title_pack, k.packName, k.index, k.count)
        is PuzzleKind.Tutorial -> stringResource(R.string.puzzle_title_tutorial, k.step.step, TutorialSteps.COUNT)
        null -> stringResource(R.string.puzzle_loading)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (!state.solved && !state.loading) {
                        Text(elapsedText(state.elapsedMs), style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { showRestartDialog = true }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.puzzle_restart))
                        }
                        IconButton(onClick = viewModel::togglePause) {
                            Icon(
                                painterResource(if (state.paused) R.drawable.ic_play else R.drawable.ic_pause),
                                contentDescription = stringResource(if (state.paused) R.string.puzzle_resume else R.string.puzzle_pause),
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val puzzle = state.puzzle
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.error || puzzle == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Button(onClick = onBack) { Text(stringResource(R.string.action_back)) }
            }
            state.solved -> SolvedContent(
                state = state,
                modifier = Modifier.fillMaxSize().padding(padding),
                onNext = { viewModel.nextRoute()?.let(onOpenPuzzle) ?: onHome() },
                onHome = onHome,
            )
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp),
            ) {
                val tutorial = state.kind as? PuzzleKind.Tutorial
                if (tutorial != null) {
                    Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(tutorial.step.title), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(tutorial.step.body), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (state.paused) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.puzzle_paused), style = MaterialTheme.typography.headlineSmall)
                            Button(onClick = viewModel::togglePause) { Text(stringResource(R.string.puzzle_resume)) }
                        }
                    } else {
                        NonogramBoard(
                            clues = puzzle.clues,
                            board = state.board,
                            mistakes = state.mistakes,
                            lastHint = state.lastHint,
                            darkTheme = isDarkTheme(state.settings.theme),
                            enabled = !state.solved,
                            zoomEnabled = puzzle.size >= 15,
                            targetFor = viewModel::targetFor,
                            onStrokeStart = viewModel::beginStroke,
                            onPaint = viewModel::paint,
                            onStrokeEnd = viewModel::endStroke,
                            onTapFeedback = feedback::tap,
                            onLongPressFeedback = feedback::longPress,
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                        )
                    }
                }
                Controls(state, viewModel, modifier = Modifier.padding(vertical = 12.dp))
            }
        }
    }

    if (showHintDialog) {
        AlertDialog(
            onDismissRequest = { showHintDialog = false },
            title = { Text(stringResource(R.string.puzzle_hint_none_title)) },
            text = { Text(stringResource(R.string.puzzle_hint_none_body)) },
            confirmButton = {
                TextButton(onClick = { showHintDialog = false; onShop() }) { Text(stringResource(R.string.puzzle_hint_none_shop)) }
            },
            dismissButton = {
                TextButton(onClick = { showHintDialog = false }) { Text(stringResource(R.string.puzzle_hint_none_later)) }
            },
        )
    }
    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text(stringResource(R.string.puzzle_restart)) },
            text = { Text(stringResource(R.string.puzzle_restart_confirm)) },
            confirmButton = {
                TextButton(onClick = { showRestartDialog = false; viewModel.restart() }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showRestartDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Controls(state: PuzzleUiState, viewModel: PuzzleViewModel, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = state.mode == PaintMode.FILL,
                onClick = { viewModel.setMode(PaintMode.FILL) },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                icon = { Icon(painterResource(R.drawable.ic_fill), contentDescription = null) },
            ) { Text(stringResource(R.string.puzzle_mode_fill)) }
            SegmentedButton(
                selected = state.mode == PaintMode.MARK,
                onClick = { viewModel.setMode(PaintMode.MARK) },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                icon = { Icon(painterResource(R.drawable.ic_cross), contentDescription = null) },
            ) { Text(stringResource(R.string.puzzle_mode_mark)) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = viewModel::undo, enabled = state.canUndo && !state.paused) {
                Icon(painterResource(R.drawable.ic_undo), contentDescription = stringResource(R.string.puzzle_undo))
            }
            IconButton(onClick = viewModel::redo, enabled = state.canRedo && !state.paused) {
                Icon(painterResource(R.drawable.ic_redo), contentDescription = stringResource(R.string.puzzle_redo))
            }
            IconButton(onClick = viewModel::useHint, enabled = !state.paused) {
                BadgedBox(badge = {
                    if (state.kind !is PuzzleKind.Tutorial) Badge { Text(state.hintsAvailable.toString()) }
                }) {
                    Icon(painterResource(R.drawable.ic_hint), contentDescription = stringResource(R.string.puzzle_hint))
                }
            }
            if (!state.settings.highlightMistakes) {
                TextButton(onClick = viewModel::check, enabled = !state.paused) { Text(stringResource(R.string.puzzle_check)) }
            }
        }
    }
}

@Composable
private fun SolvedContent(state: PuzzleUiState, modifier: Modifier, onNext: () -> Unit, onHome: () -> Unit) {
    val puzzle = state.puzzle ?: return
    val reveal by animateFloatAsState(
        targetValue = puzzle.size.toFloat() + 1f,
        animationSpec = tween(durationMillis = 1400),
        label = "reveal",
    )
    val dark = isDarkTheme(state.settings.theme)
    Column(
        modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.solved_title), style = MaterialTheme.typography.headlineMedium)
        PicturePreview(
            solution = puzzle.solution,
            color = if (dark) BoardColors.filledDark else BoardColors.filledLight,
            background = MaterialTheme.colorScheme.surfaceVariant,
            revealRows = reveal,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .aspectRatio(1f),
        )
        puzzle.name?.let { Text(it, style = MaterialTheme.typography.headlineSmall) }
        Text(stringResource(R.string.solved_time, elapsedText(state.elapsedMs)), style = MaterialTheme.typography.titleMedium)
        Text(
            if (state.hintsUsed == 0) stringResource(R.string.solved_no_hints) else stringResource(R.string.solved_hints, state.hintsUsed),
            style = MaterialTheme.typography.bodyMedium,
        )
        when (val k = state.kind) {
            is PuzzleKind.Daily -> state.streakAfterSolve?.let { Text(stringResource(R.string.solved_daily_streak, it)) }
            is PuzzleKind.Tutorial -> if (k.step.step == TutorialSteps.COUNT) Text(stringResource(R.string.solved_tutorial_done))
            else -> Unit
        }
        Spacer(Modifier.weight(1f))
        val nextLabel = when (val k = state.kind) {
            is PuzzleKind.Endless -> stringResource(R.string.solved_next_endless)
            is PuzzleKind.Pack -> if (k.nextUnlocked) stringResource(R.string.solved_next_pack) else stringResource(R.string.action_home)
            is PuzzleKind.Tutorial -> if (k.step.step < TutorialSteps.COUNT) stringResource(R.string.action_next) else stringResource(R.string.action_home)
            is PuzzleKind.Daily, null -> stringResource(R.string.action_home)
        }
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(nextLabel) }
        if (state.kind !is PuzzleKind.Daily) {
            OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_home)) }
        }
    }
}

@Composable
fun elapsedText(ms: Long): String {
    val (h, m, s) = formatElapsed(ms)
    return if (h > 0) stringResource(R.string.time_format_hours, h, m, s) else stringResource(R.string.time_format, m, s)
}
