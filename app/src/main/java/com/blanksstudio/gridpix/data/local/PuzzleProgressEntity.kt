package com.blanksstudio.gridpix.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * SPEC section 5 `puzzle_progress`. One row per puzzle the player has opened.
 * [state] is the N*N board string from `GridState.encode()` (`.` empty, `#` filled, `x` marked).
 */
@Entity(tableName = "puzzle_progress")
data class PuzzleProgressEntity(
    @PrimaryKey @ColumnInfo(name = "puzzle_id") val puzzleId: String,
    @ColumnInfo(name = "size") val size: Int,
    @ColumnInfo(name = "state") val state: String,
    @ColumnInfo(name = "solved") val solved: Boolean,
    @ColumnInfo(name = "elapsed_ms") val elapsedMs: Long,
    @ColumnInfo(name = "hints_used") val hintsUsed: Int,
    @ColumnInfo(name = "solved_at") val solvedAt: Long?,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
