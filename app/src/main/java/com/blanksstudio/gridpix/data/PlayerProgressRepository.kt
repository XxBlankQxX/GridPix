package com.blanksstudio.gridpix.data

import com.blanksstudio.gridpix.data.local.PuzzleProgressDao
import com.blanksstudio.gridpix.data.local.SolvedRow
import com.blanksstudio.gridpix.data.packs.PackRepository
import com.blanksstudio.gridpix.data.rules.ProgressRules
import com.blanksstudio.gridpix.data.rules.ProgressSnapshot
import com.blanksstudio.gridpix.data.rules.SolvedPuzzle
import com.blanksstudio.gridpix.util.LocalDates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Level, XP, achievements, streak calendar and collection, all derived from solved puzzles. */
@Singleton
class PlayerProgressRepository @Inject constructor(
    private val dao: PuzzleProgressDao,
    private val packs: PackRepository,
) {
    val progress: Flow<ProgressSnapshot> = dao.observeSolved().map { rows -> compute(rows) }

    suspend fun current(): ProgressSnapshot = compute(dao.getSolved())

    private suspend fun compute(rows: List<SolvedRow>): ProgressSnapshot {
        val totals = packs.packs().associate { it.id to it.puzzles.size }
        val solved = rows.map { SolvedPuzzle(it.puzzleId, it.size, it.hintsUsed, it.elapsedMs) }
        return ProgressRules.snapshot(solved, totals, LocalDate.parse(LocalDates.today()))
    }
}
