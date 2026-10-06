package com.blanksstudio.gridpix.game

/** SPEC section 2: the puzzle is solved when the filled cells equal the picture. X marks are ignored. */
object WinChecker {

    fun isSolved(solution: Solution, state: GridState): Boolean {
        if (solution.size != state.size) return false
        val n = solution.size
        for (r in 0 until n) {
            for (c in 0 until n) {
                if ((state[r, c] == CellState.FILLED) != solution[r, c]) return false
            }
        }
        return true
    }

    /** Cells the player has filled that are empty in the picture (for "highlight mistakes"). */
    fun mistakes(solution: Solution, state: GridState): List<Pair<Int, Int>> {
        val n = solution.size
        return buildList {
            for (r in 0 until n) for (c in 0 until n) {
                if (state[r, c] == CellState.FILLED && !solution[r, c]) add(r to c)
            }
        }
    }
}
