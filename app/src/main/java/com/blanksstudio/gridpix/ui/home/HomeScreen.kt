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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blanksstudio.gridpix.R
import com.blanksstudio.gridpix.ads.BannerAdSlot
import com.blanksstudio.gridpix.data.rules.Trophy
import com.blanksstudio.gridpix.ui.common.PackCardView
import com.blanksstudio.gridpix.ui.common.emoji
import com.blanksstudio.gridpix.ui.common.rememberEnableReminder
import com.blanksstudio.gridpix.ui.theme.Accents
import java.time.format.TextStyle
import java.util.Locale

/** Difficulty words a child understands, mapped to the endless grid sizes. */
private fun difficultyLabel(size: Int): Int = when (size) {
    5 -> R.string.difficulty_easy
    10 -> R.string.difficulty_medium
    15 -> R.string.difficulty_hard
    else -> R.string.difficulty_expert
}

/**
 * Home (decision D28): the big PLAY card is the first thing on screen and works with one tap:
 * a new player starts the tutorial, a player with a puzzle in progress continues it, everyone else
 * gets a new puzzle at the chosen difficulty. Packs, Collection and Progress live in the bottom tabs.
 */
@Composable
fun HomeScreen(
    onPlay: (size: Int, seed: Long) -> Unit,
    onDaily: (date: String) -> Unit,
    onTutorial: () -> Unit,
    onPacks: () -> Unit,
    onOpenPack: (packId: String) -> Unit,
    onProgress: () -> Unit,
    onSettings: () -> Unit,
    onShop: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val progress = state.progress
    val showBanner by viewModel.showBanner.collectAsStateWithLifecycle()

    Scaffold(bottomBar = { if (showBanner) BannerAdSlot(onRemoveAds = onShop) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Keeps everything a comfortable width on tablets.
            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Header: name, hints (opens Shop), Shop, Settings
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    Surface(
                        onClick = onShop,
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        Text(
                            "💡 ${state.hintsAvailable}",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                    IconButton(onClick = onShop) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = stringResource(R.string.home_shop))
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.home_settings))
                    }
                }

                PlayCard(
                    state = state,
                    onPlayNew = { onPlay(state.selectedSize, viewModel.newSeed()) },
                    onContinue = { size, seed -> onPlay(size, seed) },
                    onTutorial = onTutorial,
                    onSkipTutorial = viewModel::skipTutorial,
                    onSelectSize = viewModel::selectSize,
                    onShop = onShop,
                )

                // Daily puzzle
                Column(
                    Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFF5A524), Color(0xFFFF7A59))))
                        .clickable(role = Role.Button) { onDaily(state.today) }
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

                // Level, streak, badges: one compact row (tap for the Progress tab)
                Card(
                    onClick = onProgress,
                    modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.level_label, progress.level.level), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                            Text("🔥 ${progress.currentStreak}   🏅 ${progress.unlocked.size}/${viewModel.badgeTotal}", style = MaterialTheme.typography.labelLarge)
                        }
                        LinearProgressIndicator(
                            progress = { progress.level.fraction },
                            drawStopIndicator = {},
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                        )
                        Text(
                            stringResource(R.string.level_xp_progress, progress.level.xpIntoLevel, progress.level.xpForNextLevel),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (state.showReminderCard) {
                    val enableReminder = rememberEnableReminder { granted -> viewModel.answerReminder(granted) }
                    Card(
                        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🔔 " + stringResource(R.string.reminder_card_title), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.reminder_card_body), style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = enableReminder,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary),
                                ) { Text(stringResource(R.string.reminder_card_enable)) }
                                TextButton(onClick = viewModel::dismissReminderCard) { Text(stringResource(R.string.reminder_card_dismiss)) }
                            }
                        }
                    }
                }

                // Picture packs
                Row(Modifier.padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.home_packs), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onPacks) { Text(stringResource(R.string.home_see_all)) }
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.packs, key = { it.id }) { card ->
                        PackCardView(card, onClick = { onOpenPack(card.id) }, modifier = Modifier.width(150.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

/** The big one-tap PLAY card at the top of Home. */
@Composable
private fun PlayCard(
    state: HomeUiState,
    onPlayNew: () -> Unit,
    onContinue: (Int, Long) -> Unit,
    onTutorial: () -> Unit,
    onSkipTutorial: () -> Unit,
    onSelectSize: (Int) -> Unit,
    onShop: () -> Unit,
) {
    val contSize = state.continueSize
    val contSeed = state.continueSeed
    val isNewPlayer = state.showTutorialCard
    val canContinue = !isNewPlayer && contSize != null && contSeed != null

    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Accents.heroGradient)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            when {
                isNewPlayer -> stringResource(R.string.home_ready_title)
                canContinue -> stringResource(R.string.home_continue_sub, contSize!!)
                else -> stringResource(difficultyLabel(state.selectedSize)) + " · " + stringResource(R.string.size_label, state.selectedSize)
            },
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        // The button: white, huge, impossible to miss.
        Box(
            Modifier
                .fillMaxWidth()
                .height(76.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .clickable(enabled = !state.loading, role = Role.Button) {
                    when {
                        isNewPlayer -> onTutorial()
                        canContinue -> onContinue(contSize!!, contSeed!!)
                        else -> onPlayNew()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "▶  " + stringResource(if (canContinue) R.string.home_continue_big else R.string.home_play_big),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = Accents.starter,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        when {
            isNewPlayer -> {
                Text(stringResource(R.string.home_tutorial_body), style = MaterialTheme.typography.bodyMedium, color = Color.White, textAlign = TextAlign.Center)
                TextButton(onClick = onSkipTutorial) {
                    Text(stringResource(R.string.home_skip_tutorial), color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
            else -> {
                if (canContinue) {
                    TextButton(onClick = onPlayNew) {
                        Text(stringResource(R.string.home_new_puzzle), color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.sizes.forEach { size ->
                        DifficultyPill(
                            size = size,
                            selected = state.selectedSize == size,
                            unlocked = size in state.unlockedSizes,
                            onClick = { if (size in state.unlockedSizes) onSelectSize(size) else onShop() },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** One difficulty choice: a word ("Easy") with the grid size under it. Locked sizes show a lock and open the Shop. */
@Composable
private fun DifficultyPill(size: Int, selected: Boolean, unlocked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Color.White else Color.White.copy(alpha = 0.18f))
            .clickable(role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
            val colour = if (selected) Accents.forEndless(size) else Color.White
            // Shrinks instead of cutting the word off on small phones or with a large system font.
            BasicText(
                stringResource(difficultyLabel(size)),
                style = MaterialTheme.typography.labelLarge.copy(color = colour),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = MaterialTheme.typography.labelLarge.fontSize),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!unlocked) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = stringResource(R.string.locked),
                        tint = colour,
                        modifier = Modifier.padding(end = 2.dp).size(11.dp),
                    )
                }
                Text(
                    stringResource(R.string.size_label_compact, size),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) colour else Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                )
            }
        }
    }
}
