package com.blanksstudio.gridpix.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleGeneratorTest {

    @Test
    fun `same size and seed give the identical puzzle`() {
        for (size in PuzzleGenerator.SIZES) {
            val a = PuzzleGenerator.generate(size, 123_456L)
            val b = PuzzleGenerator.generate(size, 123_456L)
            assertEquals(a, b)
            assertEquals("endless-$size-123456", a.id)
        }
    }

    @Test
    fun `different seeds give different puzzles`() {
        val a = PuzzleGenerator.generate(10, 1L)
        val b = PuzzleGenerator.generate(10, 2L)
        assertNotEquals(a.solution, b.solution)
    }

    @Test
    fun `known seed is stable across releases`() {
        // Pin one output so a future change to the generator cannot silently alter
        // puzzles players already have in progress or share by id.
        assertEquals(
            listOf(".#.#.", "##.#.", ".####", ".####", "#...."),
            PuzzleGenerator.generate(5, 42L).solution.toText(),
        )
        assertEquals(
            listOf(
                "##.##.##.#", "...#######", ".#.####.##", "###..#####", ".#...##.##",
                "########.#", ".##...###.", "####...###", "..###.##..", "#.#.####.#",
            ),
            PuzzleGenerator.generate(10, 7L).solution.toText(),
        )
    }

    @Test
    fun `every generated puzzle is solvable by line logic without guessing`() {
        for (size in PuzzleGenerator.SIZES) {
            for (seed in 1L..25L) {
                val puzzle = PuzzleGenerator.generate(size, seed)
                assertEquals(size, puzzle.size)
                assertEquals(ClueComputer.compute(puzzle.solution), puzzle.clues)
                val result = PuzzleSolver.solve(puzzle.clues)
                assertTrue("size $size seed $seed not line-solvable", result is SolveResult.Solved)
                assertEquals(puzzle.solution, (result as SolveResult.Solved).solution)
            }
        }
    }

    @Test
    fun `generated pictures are not degenerate`() {
        for (size in PuzzleGenerator.SIZES) {
            for (seed in 1L..10L) {
                val filled = PuzzleGenerator.generate(size, seed).solution.filledCount
                val ratio = filled.toDouble() / (size * size)
                assertTrue("size $size seed $seed density $ratio", ratio in 0.3..0.85)
            }
        }
    }

    @Test
    fun `generation is fast enough for on-demand play`() {
        val start = System.nanoTime()
        repeat(5) { PuzzleGenerator.generate(20, 1000L + it) }
        val perPuzzleMs = (System.nanoTime() - start) / 1_000_000 / 5
        assertTrue("20x20 generation took ${perPuzzleMs}ms each", perPuzzleMs < 2_000)
    }

    @Test
    fun `daily puzzle is size 10 and stable for a date`() {
        val a = DailyPuzzle.generate("2026-10-06")
        val b = DailyPuzzle.generate("2026-10-06")
        assertEquals(a, b)
        assertEquals(10, a.size)
        assertEquals("daily-2026-10-06", a.id)
        assertNotEquals(a.solution, DailyPuzzle.generate("2026-10-07").solution)
    }

    @Test
    fun `puzzle ids follow the spec formats`() {
        assertEquals("endless-10-123456", PuzzleIds.endless(10, 123_456L))
        assertEquals("daily-2026-10-06", PuzzleIds.daily("2026-10-06"))
        assertEquals("pack-animals-07", PuzzleIds.pack("animals", 7))
        assertEquals("pack-animals-30", PuzzleIds.pack("animals", 30))
    }
}
