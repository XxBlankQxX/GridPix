package com.blanksstudio.gridpix.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.blanksstudio.gridpix.R
import com.blanksstudio.gridpix.data.PlayerProgressRepository
import com.blanksstudio.gridpix.data.rules.Achievement
import com.blanksstudio.gridpix.data.rules.CalendarRules
import com.blanksstudio.gridpix.data.rules.ProgressSnapshot
import com.blanksstudio.gridpix.data.rules.Trophy
import com.blanksstudio.gridpix.game.PuzzleGenerator
import com.blanksstudio.gridpix.ui.common.ScreenScaffold
import com.blanksstudio.gridpix.ui.common.emoji
import com.blanksstudio.gridpix.ui.common.ui
import com.blanksstudio.gridpix.ui.puzzle.elapsedText
import com.blanksstudio.gridpix.ui.theme.Accents
import com.blanksstudio.gridpix.util.LocalDates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(playerProgress: PlayerProgressRepository) : ViewModel() {
    val progress: StateFlow<ProgressSnapshot> =
        playerProgress.progress.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressSnapshot.EMPTY)
}

/** Progress screen (SPEC S8, extended): level, streaks, daily calendar with monthly trophies, badges, stats by size. */
@Composable
fun StatsScreen(onBack: () -> Unit, viewModel: StatsViewModel = hiltViewModel()) {
    val p by viewModel.progress.collectAsStateWithLifecycle()
    val today = LocalDate.parse(LocalDates.today())
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    val month = YearMonth.parse(monthText)

    ScreenScaffold(title = stringResource(R.string.progress_title), onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Level
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Accents.heroGradient).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.level_label, p.level.level), style = MaterialTheme.typography.headlineMedium, color = Color.White)
                LinearProgressIndicator(
                    progress = { p.level.fraction },
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.28f),
                    drawStopIndicator = {},
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
                )
                Text(
                    stringResource(R.string.level_xp_progress, p.level.xpIntoLevel, p.level.xpForNextLevel) + " · " +
                        stringResource(R.string.progress_total_xp, p.level.totalXp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("🧩", p.solvedCount.toString(), stringResource(R.string.stats_total), Modifier.weight(1f))
                StatTile("🔥", stringResource(R.string.progress_days, p.currentStreak), stringResource(R.string.progress_current_streak), Modifier.weight(1f))
                StatTile("🏆", stringResource(R.string.progress_days, p.longestStreak), stringResource(R.string.progress_best_streak), Modifier.weight(1f))
            }

            // Calendar
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.progress_calendar), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { monthText = month.minusMonths(1).toString() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.progress_prev_month))
                    }
                    Text(
                        month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + " " + month.year,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    IconButton(onClick = { monthText = month.plusMonths(1).toString() }, enabled = month < YearMonth.from(today)) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.progress_next_month))
                    }
                }
                MonthGrid(month, p.dailyDays, today)
                val solvedInMonth = p.dailyDays.count { YearMonth.from(it) == month }
                val trophy = CalendarRules.trophy(month, p.dailyDays)
                Text(
                    stringResource(R.string.progress_month_solved, solvedInMonth, month.lengthOfMonth()),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    trophy.emoji + (if (trophy == Trophy.NONE) "" else " ") + stringResource(
                        when (trophy) {
                            Trophy.NONE -> R.string.progress_trophy_none
                            Trophy.BRONZE -> R.string.progress_trophy_bronze
                            Trophy.SILVER -> R.string.progress_trophy_silver
                            Trophy.GOLD -> R.string.progress_trophy_gold
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Badges
            Text(
                stringResource(R.string.progress_badges) + "  " + stringResource(R.string.progress_badge_progress, p.unlocked.size, Achievement.entries.size),
                style = MaterialTheme.typography.titleLarge,
            )
            Achievement.entries.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { a -> BadgeCard(a, p.achievementProgress[a] ?: 0, Modifier.weight(1f)) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            // By size
            SectionCard {
                Text(stringResource(R.string.progress_by_size), style = MaterialTheme.typography.titleMedium)
                PuzzleGenerator.SIZES.forEach { size ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.padding(end = 10.dp).clip(CircleShape).background(Accents.forEndless(size)).padding(6.dp))
                        Text(stringResource(R.string.size_label, size), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.stats_solved_count, p.solvedBySize[size] ?: 0), style = MaterialTheme.typography.bodyMedium)
                            p.bestTimeBySize[size]?.let {
                                Text(stringResource(R.string.stats_best_time, elapsedText(it)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun StatTile(icon: String, value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, style = MaterialTheme.typography.titleLarge)
            Text(value, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun MonthGrid(month: YearMonth, solved: Set<LocalDate>, today: LocalDate) {
    val firstDow = DayOfWeek.MONDAY
    Row(Modifier.fillMaxWidth()) {
        (0 until 7).forEach { i ->
            Text(
                firstDow.plus(i.toLong()).getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
    val lead = (month.atDay(1).dayOfWeek.value - firstDow.value + 7) % 7
    val cells = lead + month.lengthOfMonth()
    (0 until (cells + 6) / 7).forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            (0 until 7).forEach { col ->
                val dayNum = week * 7 + col - lead + 1
                Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                    if (dayNum in 1..month.lengthOfMonth()) {
                        val date = month.atDay(dayNum)
                        val isSolved = date in solved
                        val bg = if (isSolved) Accents.daily else MaterialTheme.colorScheme.surfaceContainerHigh
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(bg)
                                .then(if (date == today) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                                .alpha(if (date.isAfter(today)) 0.4f else 1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                dayNum.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSolved) Color.White else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeCard(a: Achievement, progress: Int, modifier: Modifier = Modifier) {
    val ui = a.ui
    val unlocked = progress >= a.target
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(ui.emoji, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.alpha(if (unlocked) 1f else 0.35f))
            Text(stringResource(ui.title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(ui.description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!unlocked && a.target > 1) {
                LinearProgressIndicator(
                    progress = { progress.toFloat() / a.target },
                    drawStopIndicator = {},
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(50)),
                )
                Text(stringResource(R.string.progress_badge_progress, progress, a.target), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
