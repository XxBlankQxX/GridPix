package com.blanksstudio.gridpix.game

import kotlin.random.Random

/**
 * Endless-mode generator (SPEC section 3): seeded random fill at 55-65% density, then keep
 * the picture only if [PuzzleSolver] finishes it by line logic alone.
 *
 * Every random draw comes from one `Random(seed)`, and Kotlin's seeded Random is the same
 * algorithm on every platform, so a (size, seed) pair always yields the same puzzle.
 *
 * When a candidate leaves the solver stuck, a few of the still-unknown cells are inverted
 * in the picture and it is re-checked. This converges far faster than drawing fresh random
 * pictures, which at 15x15 and 20x20 are rarely line-solvable as-is.
 */
object PuzzleGenerator {

    val SIZES: List<Int> = listOf(5, 10, 15, 20)
    const val MIN_DENSITY = 0.55
    const val MAX_DENSITY = 0.65
    const val MAX_ATTEMPTS = 5_000

    fun generate(size: Int, seed: Long): Puzzle =
        generate(size, seed, PuzzleIds.endless(size, seed))

    fun generate(size: Int, seed: Long, id: String): Puzzle {
        require(size in SIZES) { "unsupported size $size, expected one of $SIZES" }
        val rng = Random(seed)
        val density = MIN_DENSITY + rng.nextDouble() * (MAX_DENSITY - MIN_DENSITY)
        var solution = Solution.of(size, BooleanArray(size * size) { rng.nextDouble() < density })

        repeat(MAX_ATTEMPTS) {
            when (val result = PuzzleSolver.solve(ClueComputer.compute(solution))) {
                is SolveResult.Solved -> {
                    check(result.solution == solution) // clues came from this picture
                    return Puzzle(id, solution, ClueComputer.compute(solution))
                }
                is SolveResult.Stuck -> solution = repair(solution, result.partial, rng)
                SolveResult.Contradiction -> error("clues from a real picture cannot contradict")
            }
        }
        error("no line-solvable $size x $size puzzle found for seed $seed in $MAX_ATTEMPTS attempts")
    }

    /** Inverts a small random subset of the cells the solver could not determine. */
    private fun repair(solution: Solution, partial: PartialGrid, rng: Random): Solution {
        val unknown = partial.unknownCells()
        val flips = 1 + unknown.size / 12
        var repaired = solution
        repeat(flips) {
            val (row, col) = unknown[rng.nextInt(unknown.size)]
            repaired = repaired.withToggled(row, col)
        }
        return repaired
    }
}

/** Daily puzzle (SPEC section 3): size 10, seed derived from the local date. */
object DailyPuzzle {
    const val SIZE = 10

    /** Stable 64-bit FNV-1a hash of the date string, so the same date gives the same puzzle on every phone. */
    fun seedFor(isoDate: String): Long {
        var hash = -0x340d631b7bdddcdbL // FNV offset basis
        for (ch in "gridpix-daily-$isoDate") {
            hash = hash xor ch.code.toLong()
            hash *= 0x100000001b3L // FNV prime
        }
        return hash
    }

    fun generate(isoDate: String): Puzzle =
        PuzzleGenerator.generate(SIZE, seedFor(isoDate), PuzzleIds.daily(isoDate))
}
