package com.blanksstudio.gridpix.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Aggregate for the Stats screen (SPEC S8). */
data class SizeStats(
    @androidx.room.ColumnInfo(name = "size") val size: Int,
    @androidx.room.ColumnInfo(name = "solved_count") val solvedCount: Int,
    @androidx.room.ColumnInfo(name = "best_ms") val bestMs: Long?,
)

data class SolvedRow(
    @androidx.room.ColumnInfo(name = "puzzle_id") val puzzleId: String,
    @androidx.room.ColumnInfo(name = "size") val size: Int,
    @androidx.room.ColumnInfo(name = "hints_used") val hintsUsed: Int,
    @androidx.room.ColumnInfo(name = "elapsed_ms") val elapsedMs: Long,
)

@Dao
interface PuzzleProgressDao {

    @Query("SELECT * FROM puzzle_progress WHERE puzzle_id = :puzzleId")
    suspend fun get(puzzleId: String): PuzzleProgressEntity?

    @Query("SELECT * FROM puzzle_progress WHERE puzzle_id = :puzzleId")
    fun observe(puzzleId: String): Flow<PuzzleProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PuzzleProgressEntity)

    /** Progress rows for one pack, e.g. prefix `pack-animals-`. Used for thumbnails and pack progress. */
    @Query("SELECT * FROM puzzle_progress WHERE puzzle_id LIKE :prefix || '%'")
    fun observeByPrefix(prefix: String): Flow<List<PuzzleProgressEntity>>

    @Query("SELECT COUNT(*) FROM puzzle_progress WHERE puzzle_id LIKE :prefix || '%' AND solved = 1")
    fun observeSolvedCount(prefix: String): Flow<Int>

    @Query(
        """
        SELECT size, COUNT(*) AS solved_count, MIN(elapsed_ms) AS best_ms
        FROM puzzle_progress WHERE solved = 1 GROUP BY size ORDER BY size
        """,
    )
    fun observeStatsBySize(): Flow<List<SizeStats>>

    @Query("SELECT COUNT(*) FROM puzzle_progress WHERE solved = 1")
    fun observeTotalSolved(): Flow<Int>

    /** Most recently played unsolved endless puzzle, for the Home "continue" button. */
    @Query(
        """
        SELECT * FROM puzzle_progress WHERE puzzle_id LIKE 'endless-%' AND solved = 0
        ORDER BY updated_at DESC LIMIT 1
        """,
    )
    fun observeLatestUnsolvedEndless(): Flow<PuzzleProgressEntity?>

    /** Every solved puzzle, for XP, achievements, the streak calendar and the collection. */
    @Query("SELECT puzzle_id, size, hints_used, elapsed_ms FROM puzzle_progress WHERE solved = 1")
    fun observeSolved(): Flow<List<SolvedRow>>

    @Query("SELECT puzzle_id, size, hints_used, elapsed_ms FROM puzzle_progress WHERE solved = 1")
    suspend fun getSolved(): List<SolvedRow>

    @Query("DELETE FROM puzzle_progress WHERE puzzle_id = :puzzleId")
    suspend fun delete(puzzleId: String)
}
