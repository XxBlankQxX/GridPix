package com.blanksstudio.gridpix.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleSolverTest {

    @Test
    fun `solves a unique picture by line logic`() {
        // Full border pins every edge; the centre row [1,1,1] then forces the middle.
        val picture = Solution.fromText(
            """
            #####
            #...#
            #.#.#
            #...#
            #####
            """,
        )
        val result = PuzzleSolver.solve(ClueComputer.compute(picture))
        assertTrue(result is SolveResult.Solved)
        assertEquals(picture, (result as SolveResult.Solved).solution)
        assertTrue(PuzzleSolver.isLineSolvable(picture))
    }

    @Test
    fun `a ring with open corners is ambiguous`() {
        // Looks unique but has a second solution (corners can shift), so line logic must stop.
        val ring = Solution.fromText(
            """
            .###.
            #...#
            #.#.#
            #...#
            .###.
            """,
        )
        assertFalse(PuzzleSolver.isLineSolvable(ring))
    }

    @Test
    fun `reports stuck on an ambiguous picture`() {
        // A checkerboard pair: rows [1],[1] and cols [1],[1] fit two different pictures.
        val picture = Solution.fromText(
            """
            #.
            .#
            """,
        )
        val result = PuzzleSolver.solve(ClueComputer.compute(picture))
        assertTrue(result is SolveResult.Stuck)
        assertEquals(4, (result as SolveResult.Stuck).partial.unknownCount)
        assertFalse(PuzzleSolver.isLineSolvable(picture))
    }

    @Test
    fun `partial deductions on an ambiguous picture are sound`() {
        val picture = Solution.fromText(
            """
            #.....
            .#....
            ..####
            ..####
            ..####
            ..####
            """,
        )
        val result = PuzzleSolver.solve(ClueComputer.compute(picture))
        assertTrue(result is SolveResult.Stuck)
        val partial = (result as SolveResult.Stuck).partial
        val unknown = partial.unknownCells().toSet()
        // The top-left checkerboard can never be resolved by line logic.
        assertTrue(unknown.containsAll(setOf(0 to 0, 0 to 1, 1 to 0, 1 to 1)))
        // Something was deduced, and nothing deduced disagrees with the picture.
        assertTrue(partial.unknownCount < 36)
        for (r in 0 until 6) for (c in 0 until 6) {
            when (partial[r, c]) {
                LineSolver.FILLED -> assertTrue("($r,$c) wrongly filled", picture[r, c])
                LineSolver.EMPTY -> assertFalse("($r,$c) wrongly emptied", picture[r, c])
            }
        }
    }

    @Test
    fun `impossible clues are a contradiction`() {
        val clues = Clues(
            rows = listOf(listOf(2), listOf(2)),
            cols = listOf(emptyList(), emptyList()),
        )
        assertEquals(SolveResult.Contradiction, PuzzleSolver.solve(clues))
    }

    @Test
    fun `blank picture solves to all empty`() {
        val blank = Solution.fromText(listOf("...", "...", "..."))
        val result = PuzzleSolver.solve(ClueComputer.compute(blank))
        assertEquals(SolveResult.Solved(blank), result)
    }
}
