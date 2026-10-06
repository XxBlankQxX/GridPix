package com.blanksstudio.gridpix.data

import com.blanksstudio.gridpix.data.local.PuzzleProgressDao
import com.blanksstudio.gridpix.data.local.PuzzleProgressEntity
import com.blanksstudio.gridpix.data.local.SizeStats
import com.blanksstudio.gridpix.game.GridState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Thin wrapper over the Room DAO so ViewModels never see Room types. */
@Singleton
class ProgressRepository @Inject constructor(
    private val dao: PuzzleProgressDao,
) {
    suspend fun get(puzzleId: String): PuzzleProgressEntity? = dao.get(puzzleId)

    fun observe(puzzleId: String): Flow<PuzzleProgressEntity?> = dao.observe(puzzleId)

    /** SPEC section 2: called on every move. */
    suspend fun save(
        puzzleId: String,
        state: GridState,
        elapsedMs: Long,
        hintsUsed: Int,
        solved: Boolean,
        now: Long = System.currentTimeMillis(),
    ) {
        val existing = dao.get(puzzleId)
        dao.upsert(
            PuzzleProgressEntity(
                puzzleId = puzzleId,
                size = state.size,
                state = state.encode(),
                solved = solved,
                elapsedMs = elapsedMs,
                hintsUsed = hintsUsed,
                solvedAt = if (solved) existing?.solvedAt ?: now else null,
                updatedAt = now,
            ),
        )
    }

    fun observePack(prefix: String): Flow<List<PuzzleProgressEntity>> = dao.observeByPrefix(prefix)

    fun observeSolvedCount(prefix: String): Flow<Int> = dao.observeSolvedCount(prefix)

    fun observeStatsBySize(): Flow<List<SizeStats>> = dao.observeStatsBySize()

    fun observeTotalSolved(): Flow<Int> = dao.observeTotalSolved()

    fun observeLatestUnsolvedEndless(): Flow<PuzzleProgressEntity?> = dao.observeLatestUnsolvedEndless()

    suspend fun reset(puzzleId: String) = dao.delete(puzzleId)
}
