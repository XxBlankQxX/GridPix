package com.blanksstudio.gridpix.game

/** One revealed cell and the state it should be set to. */
data class Hint(val row: Int, val col: Int, val state: CellState)

/**
 * Hint rule from SPEC section 2: reveal one correct cell from the row or column with the
 * most unresolved cells.
 *
 * A cell is unresolved when the board disagrees with the picture: a filled picture cell
 * that is not FILLED on the board, or an empty picture cell the player has FILLED.
 * Untouched cells that are empty in the picture count as resolved, since leaving them
 * blank is correct; the hint therefore never spends itself on an X mark unless the
 * player filled that cell by mistake.
 *
 * Ties go to the first row, then the first column. Within the chosen line the first
 * unresolved cell is revealed. Deterministic on purpose so it is testable.
 */
object HintSelector {

    fun select(solution: Solution, state: GridState): Hint? {
        require(solution.size == state.size) { "solution and board sizes differ" }
        val n = solution.size

        var bestCount = 0
        var bestIsRow = true
        var bestIndex = -1

        for (r in 0 until n) {
            val count = (0 until n).count { c -> isUnresolved(solution, state, r, c) }
            if (count > bestCount) { bestCount = count; bestIsRow = true; bestIndex = r }
        }
        for (c in 0 until n) {
            val count = (0 until n).count { r -> isUnresolved(solution, state, r, c) }
            if (count > bestCount) { bestCount = count; bestIsRow = false; bestIndex = c }
        }
        if (bestIndex < 0) return null

        for (i in 0 until n) {
            val (r, c) = if (bestIsRow) bestIndex to i else i to bestIndex
            if (isUnresolved(solution, state, r, c)) {
                return Hint(r, c, if (solution[r, c]) CellState.FILLED else CellState.MARKED)
            }
        }
        return null
    }

    fun isUnresolved(solution: Solution, state: GridState, row: Int, col: Int): Boolean {
        val filled = state[row, col] == CellState.FILLED
        return solution[row, col] != filled
    }
}
