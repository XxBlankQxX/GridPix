package com.blanksstudio.gridpix.game

/** A playable puzzle: stable id (SPEC section 5), the hidden picture, and its clues. */
data class Puzzle(
    val id: String,
    val solution: Solution,
    val clues: Clues,
    /** Picture name for pack puzzles, shown on the solved screen. Null for generated puzzles. */
    val name: String? = null,
) {
    val size: Int get() = solution.size

    companion object {
        fun from(id: String, solution: Solution, name: String? = null): Puzzle =
            Puzzle(id, solution, ClueComputer.compute(solution), name)
    }
}

/** Puzzle id formats from SPEC section 5. */
object PuzzleIds {
    fun endless(size: Int, seed: Long): String = "endless-$size-$seed"

    /** [isoDate] is the local date as `yyyy-MM-dd`. */
    fun daily(isoDate: String): String = "daily-$isoDate"

    fun pack(packId: String, index: Int): String = "pack-$packId-" + index.toString().padStart(2, '0')
}
