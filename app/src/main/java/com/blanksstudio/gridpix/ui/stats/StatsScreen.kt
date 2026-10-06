package com.blanksstudio.gridpix.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.blanksstudio.gridpix.R
import com.blanksstudio.gridpix.data.ProgressRepository
import com.blanksstudio.gridpix.data.local.SizeStats
import com.blanksstudio.gridpix.data.rules.StreakRules
import com.blanksstudio.gridpix.data.settings.SettingsRepository
import com.blanksstudio.gridpix.game.PuzzleGenerator
import com.blanksstudio.gridpix.ui.common.ScreenScaffold
import com.blanksstudio.gridpix.ui.puzzle.elapsedText
import com.blanksstudio.gridpix.util.LocalDates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatsUiState(
    val total: Int = 0,
    val streak: Int = 0,
    val bySize: List<SizeStats> = emptyList(),
)

@HiltViewModel
class StatsViewModel @Inject constructor(progress: ProgressRepository, settings: SettingsRepository) : ViewModel() {
    val uiState: StateFlow<StatsUiState> = combine(
        progress.observeTotalSolved(),
        settings.streak,
        progress.observeStatsBySize(),
    ) { total, streak, bySize ->
        StatsUiState(total, StreakRules.current(streak, LocalDates.today(), LocalDates.yesterday()), bySize)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())
}

@Composable
fun StatsScreen(onBack: () -> Unit, viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ScreenScaffold(title = stringResource(R.string.stats_title), onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(stringResource(R.string.stats_total), state.total.toString(), Modifier.weight(1f))
                StatCard(stringResource(R.string.stats_streak), state.streak.toString(), Modifier.weight(1f))
            }
            Text(stringResource(R.string.stats_by_size), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            if (state.total == 0) {
                Text(stringResource(R.string.stats_none_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val bySize = state.bySize.associateBy { it.size }
            (PuzzleGenerator.SIZES).forEach { size ->
                val row = bySize[size]
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.size_label, size), style = MaterialTheme.typography.titleMedium)
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Text(stringResource(R.string.stats_solved_count, row?.solvedCount ?: 0))
                            row?.bestMs?.let { Text(stringResource(R.string.stats_best_time, elapsedText(it)), style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}
