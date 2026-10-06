package com.blanksstudio.gridpix.game

/**
 * Deduces everything that can be known about a single line from its clue and the cells
 * already determined. This is the only reasoning step a human needs for a well-formed
 * nonogram, so a puzzle the line solver finishes is "solvable without guessing" (SPEC section 3).
 *
 * Method: enumerate every placement of the clue blocks that agrees with the known cells
 * and record, per cell, whether any placement fills it and whether any leaves it empty.
 * A cell with only one possibility is deduced. Grids are at most 20 wide, so the number
 * of placements is small (worst case a few thousand).
 */
object LineSolver {

    const val UNKNOWN = 0
    const val FILLED = 1
    const val EMPTY = 2

    /**
     * @return a new line with every deducible cell set, or null when no placement of
     *   [clue] fits the known cells (a contradiction).
     */
    fun solve(clue: List<Int>, line: IntArray): IntArray? {
        val n = line.size
        if (clue.isEmpty()) {
            return if (line.any { it == FILLED }) null else IntArray(n) { EMPTY }
        }
        require(clue.all { it > 0 }) { "clue blocks must be positive: $clue" }

        val canFill = BooleanArray(n)
        val canEmpty = BooleanArray(n)
        val ctx = Context(clue, line, canFill, canEmpty, IntArray(clue.size))
        ctx.place(0, 0)
        if (!ctx.found) return null

        return IntArray(n) { i ->
            when {
                canFill[i] && !canEmpty[i] -> FILLED
                canEmpty[i] && !canFill[i] -> EMPTY
                else -> line[i]
            }
        }
    }

    private class Context(
        val clue: List<Int>,
        val line: IntArray,
        val canFill: BooleanArray,
        val canEmpty: BooleanArray,
        val starts: IntArray,
    ) {
        val n = line.size
        var found = false
        // Unknown cells not yet seen both filled and empty; 0 = nothing more to learn, stop enumerating.
        var undecided = line.count { it == UNKNOWN }

        /** Minimum cells needed for blocks [from, end) including the gaps between them. */
        private fun minSpan(from: Int): Int {
            var span = 0
            for (b in from until clue.size) span += clue[b] + 1
            return span - 1
        }

        /** Places block [b] at or after [from]; cells before the block in [from, start) are gaps. */
        fun place(b: Int, from: Int) {
            if (found && undecided == 0) return
            if (b == clue.size) {
                // Tail after the last block must contain no filled cell.
                for (i in from until n) if (line[i] == FILLED) return
                record()
                return
            }
            val len = clue[b]
            val last = n - minSpan(b)
            var start = from
            while (start <= last) {
                // Gap cells between the previous block and this one cannot be filled.
                if (start > from && line[start - 1] == FILLED) return
                if (fits(start, len)) {
                    starts[b] = start
                    place(b + 1, start + len + 1)
                    if (found && undecided == 0) return
                }
                start++
            }
        }

        private fun fits(start: Int, len: Int): Boolean {
            for (i in start until start + len) if (line[i] == EMPTY) return false
            val after = start + len
            return after >= n || line[after] != FILLED
        }

        private fun record() {
            found = true
            var b = 0
            var i = 0
            while (i < n) {
                if (b < clue.size && i == starts[b]) {
                    val end = i + clue[b]
                    while (i < end) mark(canFill, i++)
                    b++
                } else {
                    mark(canEmpty, i++)
                }
            }
        }

        private fun mark(flags: BooleanArray, i: Int) {
            if (!flags[i]) {
                flags[i] = true
                if (line[i] == UNKNOWN && canFill[i] && canEmpty[i]) undecided--
            }
        }
    }
}
