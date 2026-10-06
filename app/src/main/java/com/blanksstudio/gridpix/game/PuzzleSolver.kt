package com.blanksstudio.gridpix.game

sealed interface SolveResult {
    /** Every cell was determined by line logic alone: the puzzle is unique and human-solvable. */
    data class Solved(val solution: Solution) : SolveResult

    /** Line logic ran out of deductions. The puzzle needs guessing or has several solutions. */
    data class Stuck(val partial: PartialGrid) : SolveResult

    /** No picture matches these clues. Cannot happen for clues computed from a real picture. */
    data object Contradiction : SolveResult
}

/** Solver working grid: row-major [LineSolver.UNKNOWN]/[LineSolver.FILLED]/[LineSolver.EMPTY]. */
class PartialGrid(val size: Int, private val cells: IntArray) {
    operator fun get(row: Int, col: Int): Int = cells[row * size + col]
    val unknownCount: Int get() = cells.count { it == LineSolver.UNKNOWN }
    fun unknownCells(): List<Pair<Int, Int>> =
        cells.indices.filter { cells[it] == LineSolver.UNKNOWN }.map { it / size to it % size }
}

/**
 * Whole-puzzle line-logic solver (SPEC section 3): repeatedly applies [LineSolver] to every
 * row and column that changed until nothing more can be deduced.
 */
object PuzzleSolver {

    fun solve(clues: Clues): SolveResult {
        val n = clues.size
        val grid = IntArray(n * n) // all UNKNOWN
        val rowDirty = BooleanArray(n) { true }
        val colDirty = BooleanArray(n) { true }
        val line = IntArray(n)

        var progress = true
        while (progress) {
            progress = false
            for (r in 0 until n) {
                if (!rowDirty[r]) continue
                rowDirty[r] = false
                for (c in 0 until n) line[c] = grid[r * n + c]
                val solved = LineSolver.solve(clues.rows[r], line) ?: return SolveResult.Contradiction
                for (c in 0 until n) {
                    if (solved[c] != grid[r * n + c]) {
                        grid[r * n + c] = solved[c]
                        colDirty[c] = true
                        progress = true
                    }
                }
            }
            for (c in 0 until n) {
                if (!colDirty[c]) continue
                colDirty[c] = false
                for (r in 0 until n) line[r] = grid[r * n + c]
                val solved = LineSolver.solve(clues.cols[c], line) ?: return SolveResult.Contradiction
                for (r in 0 until n) {
                    if (solved[r] != grid[r * n + c]) {
                        grid[r * n + c] = solved[r]
                        rowDirty[r] = true
                        progress = true
                    }
                }
            }
        }

        return if (grid.all { it != LineSolver.UNKNOWN }) {
            SolveResult.Solved(Solution.of(n, BooleanArray(n * n) { grid[it] == LineSolver.FILLED }))
        } else {
            SolveResult.Stuck(PartialGrid(n, grid))
        }
    }

    /** True when line logic alone reproduces [solution] from its clues. */
    fun isLineSolvable(solution: Solution): Boolean {
        val result = solve(ClueComputer.compute(solution))
        return result is SolveResult.Solved && result.solution == solution
    }
}
