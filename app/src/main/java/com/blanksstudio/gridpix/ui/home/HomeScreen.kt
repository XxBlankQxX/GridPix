package com.blanksstudio.gridpix.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blanksstudio.gridpix.R
import com.blanksstudio.gridpix.data.rules.Trophy
import com.blanksstudio.gridpix.ui.common.PackCardView
import com.blanksstudio.gridpix.ui.common.emoji
import com.blanksstudio.gridpix.ui.theme.Accents
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    onPlay: (size: Int, seed: Long) -> Unit,
    onDaily: (date: String) -> Unit,
    onTutorial: () -> Unit,
    onPacks: () -> Unit,
    onOpenPack: (packId: String) -> Unit,
    onProgress: () -> Unit,
    onCollection: () -> Unit,
    onSettings: () -> Unit,
    onShop: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val progress = state.progress

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Header
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.home_settings))
                }
            }

            // Level hero card
            Column(
                Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Accents.heroGradient)
                    .clickable(onClick = onProgress)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.level_label, progress.level.level), style = MaterialTheme.typography.headlineMedium, color = Color.White, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.progress_total_xp, progress.level.totalXp), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.9f))
                }
                LinearProgressIndicator(
                    progress = { progress.level.fraction },
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.28f),
                    drawStopIndicator = {},
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
                )
                Text(
                    stringResource(R.string.level_xp_progress, progress.level.xpIntoLevel, progress.level.xpForNextLevel),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroChip("🔥 " + stringResource(R.string.home_daily_streak, progress.currentStreak))
                    HeroChip("💡 " + stringResource(R.string.home_hints_chip, state.hintsAvailable))
                    HeroChip("🏅 " + stringResource(R.string.home_badges_count, progress.unlocked.size, viewModel.badgeTotal))
                }
            }

            if (state.showTutorialCard) {
                Card(
                    modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.home_tutorial_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.home_tutorial_body), style = MaterialTheme.typography.bodyMedium)
                        FilledTonalButton(onClick = onTutorial) { Text(stringResource(R.string.home_tutorial_start)) }
                    }
                }
            }

            // Endless
            Card(
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.home_play_title), style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.sizes.forEach { size ->
                            val unlocked = size in state.unlockedSizes
                            val sizeAccent = Accents.forEndless(size)
                            FilterChip(
                                selected = state.selectedSize == size,
                                onClick = { if (unlocked) viewModel.selectSize(size) else onShop() },
                                label = { Text(stringResource(R.string.size_label, size)) },
                                leadingIcon = if (unlocked) null else {
                                    { Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.locked), modifier = Modifier.size(16.dp)) }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = sizeAccent,
                                    selectedLabelColor = Color.White,
                                ),
                            )
                        }
                    }
                    val playAccent = Accents.forEndless(state.selectedSize)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Accents.gradient(playAccent))
                            .clickable(enabled = !state.loading) { onPlay(state.selectedSize, viewModel.newSeed()) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("▶  " + stringResource(R.string.home_play_size, state.selectedSize), style = MaterialTheme.typography.titleLarge, color = Color.White)
                    }
                    val contSize = state.continueSize
                    val contSeed = state.continueSeed
                    if (contSize != null && contSeed != null) {
                        OutlinedButton(onClick = { onPlay(contSize, contSeed) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.home_continue, contSize))
                        }
                    }
                }
            }

            // Daily
            Column(
                Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFFF5A524), Color(0xFFFF7A59))))
                    .clickable { onDaily(state.today) }
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("⭐ " + stringResource(R.string.home_daily), style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Text(
                            when (state.dailyState) {
                                DailyState.NEW -> stringResource(R.string.home_daily_new)
                                DailyState.IN_PROGRESS -> stringResource(R.string.home_daily_in_progress)
                                DailyState.SOLVED -> stringResource(R.string.home_daily_done)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                        )
                    }
                    if (state.monthTrophy != Trophy.NONE) Text(state.monthTrophy.emoji, style = MaterialTheme.typography.headlineMedium)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    state.week.forEach { (day, solved) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f),
                            )
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(if (solved) Color.White else Color.White.copy(alpha = 0.22f))
                                    .then(if (day.toString() == state.today) Modifier.border(2.dp, Color.White, CircleShape) else Modifier),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (solved) Text("✓", color = Color(0xFFF08A24), style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
            }

            // Packs carousel
            Row(Modifier.padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.home_packs), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onPacks) { Text(stringResource(R.string.home_see_all)) }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.packs, key = { it.id }) { card ->
                    PackCardView(card, onClick = { onOpenPack(card.id) }, modifier = Modifier.width(160.dp))
                }
            }

            // Bottom tiles
            Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeTile("🖼️", stringResource(R.string.home_collection), stringResource(R.string.home_collected_count, progress.collected, progress.collectible), onCollection, Modifier.weight(1f))
                HomeTile("🏆", stringResource(R.string.home_progress), stringResource(R.string.home_badges_count, progress.unlocked.size, viewModel.badgeTotal), onProgress, Modifier.weight(1f))
                HomeTile("🛒", stringResource(R.string.home_shop), stringResource(R.string.home_hints_chip, state.hintsAvailable), onShop, Modifier.weight(1f))
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun HeroChip(text: String) {
    Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.2f)) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

@Composable
private fun HomeTile(icon: String, title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(icon, style = MaterialTheme.typography.headlineSmall)
            Text(title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
