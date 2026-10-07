package com.blanksstudio.gridpix.game

/**
 * Optional assist (setting "Auto-cross completed lines"): when the filled cells of a row or column
 * already match its clue, the remaining empty cells of that line are marked with X.
 *
 * It only looks at the clue, never at the solution, so it gives away nothing a player could not see
 * for themselves. A line filled wrongly but still matching its clue is crossed too; the mistake shows
 * up in the crossing lines, as it would on paper.
 */
object AutoCross {

    fun apply(clues: Clues, board: GridState): GridState {
        val n = board.size
        var result = board
        for (r in 0 until n) {
            val filled = BooleanArray(n) { c -> board[r, c] == CellState.FILLED }
            if (ClueComputer.lineClue(filled) == clues.rows[r]) {
                for (c in 0 until n) if (result[r, c] == CellState.EMPTY) result = result.with(r, c, CellState.MARKED)
            }
        }
        for (c in 0 until n) {
            val filled = BooleanArray(n) { r -> board[r, c] == CellState.FILLED }
            if (ClueComputer.lineClue(filled) == clues.cols[c]) {
                for (r in 0 until n) if (result[r, c] == CellState.EMPTY) result = result.with(r, c, CellState.MARKED)
            }
        }
        return result
    }
}
