package com.blanksstudio.gridpix.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blanksstudio.gridpix.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPlay: (size: Int, seed: Long) -> Unit,
    onDaily: (date: String) -> Unit,
    onTutorial: () -> Unit,
    onPacks: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onShop: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.home_settings))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.showTutorialCard) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.home_tutorial_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.home_tutorial_body), style = MaterialTheme.typography.bodyMedium)
                        FilledTonalButton(onClick = onTutorial) { Text(stringResource(R.string.home_tutorial_start)) }
                    }
                }
            }

            // Endless
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.home_size_picker), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.sizes.forEach { size ->
                        val unlocked = size in state.unlockedSizes
                        FilterChip(
                            selected = state.selectedSize == size,
                            onClick = { if (unlocked) viewModel.selectSize(size) else onShop() },
                            label = { Text(stringResource(R.string.size_label, size)) },
                            leadingIcon = if (unlocked) null else {
                                { Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.locked)) }
                            },
                        )
                    }
                }
                if (state.unlockedSizes.size < state.sizes.size) {
                    Text(
                        stringResource(R.string.home_large_locked),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = { onPlay(state.selectedSize, viewModel.newSeed()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !state.loading,
                ) {
                    Text(stringResource(R.string.home_play_size, state.selectedSize), style = MaterialTheme.typography.titleMedium)
                }
                val contSize = state.continueSize
                val contSeed = state.continueSeed
                if (contSize != null && contSeed != null) {
                    OutlinedButton(onClick = { onPlay(contSize, contSeed) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.home_continue, contSize))
                    }
                }
            }

            // Daily
            Card(modifier = Modifier.fillMaxWidth(), onClick = { onDaily(state.today) }) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.home_daily), style = MaterialTheme.typography.titleMedium)
                        Text(
                            when (state.dailyState) {
                                DailyState.NEW -> stringResource(R.string.home_daily_new)
                                DailyState.IN_PROGRESS -> stringResource(R.string.home_daily_in_progress)
                                DailyState.SOLVED -> stringResource(R.string.home_daily_done)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        if (state.streak > 0) stringResource(R.string.home_streak, state.streak)
                        else stringResource(R.string.home_streak_none),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            FilledTonalButton(onClick = onPacks, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.home_packs))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onStats, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.home_stats)) }
                OutlinedButton(onClick = onShop, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.home_shop))
                }
            }

            Text(
                stringResource(R.string.home_hints_available, state.hintsAvailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}
