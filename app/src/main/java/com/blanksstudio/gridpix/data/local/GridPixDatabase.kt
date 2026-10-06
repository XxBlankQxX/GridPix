package com.blanksstudio.gridpix.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * SPEC section 5: database `gridpix.db`, version 1. Schema JSON is exported to app/schemas/.
 * Any schema change: bump version, add a Migration in DatabaseModule, commit the new JSON.
 * Never fallbackToDestructiveMigration in release (CLAUDE.md).
 */
@Database(
    entities = [PuzzleProgressEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class GridPixDatabase : RoomDatabase() {
    abstract fun puzzleProgressDao(): PuzzleProgressDao

    companion object {
        const val NAME = "gridpix.db"
    }
}
