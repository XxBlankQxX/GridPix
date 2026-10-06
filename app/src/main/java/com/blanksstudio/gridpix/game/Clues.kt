package com.blanksstudio.gridpix.game

/**
 * Row and column clues: the lengths of consecutive filled runs, in order (SPEC section 2).
 * A line with no filled cells has an empty list; the UI shows it as "0".
 */
data class Clues(
    val rows: List<List<Int>>,
    val cols: List<List<Int>>,
) {
    init {
        require(rows.size == cols.size) { "rows (${rows.size}) and cols (${cols.size}) differ" }
    }

    val size: Int get() = rows.size
}

object ClueComputer {

    fun lineClue(line: BooleanArray): List<Int> {
        val clue = ArrayList<Int>()
        var run = 0
        for (filled in line) {
            if (filled) {
                run++
            } else if (run > 0) {
                clue.add(run)
                run = 0
            }
        }
        if (run > 0) clue.add(run)
        return clue
    }

    fun compute(solution: Solution): Clues = Clues(
        rows = List(solution.size) { lineClue(solution.row(it)) },
        cols = List(solution.size) { lineClue(solution.col(it)) },
    )
}
