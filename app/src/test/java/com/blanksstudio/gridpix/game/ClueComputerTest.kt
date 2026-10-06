package com.blanksstudio.gridpix.game

import org.junit.Assert.assertEquals
import org.junit.Test

class ClueComputerTest {

    private fun line(s: String) = BooleanArray(s.length) { s[it] == '#' }

    @Test
    fun `empty line has no clue`() {
        assertEquals(emptyList<Int>(), ClueComputer.lineClue(line(".....")))
    }

    @Test
    fun `full line is one block`() {
        assertEquals(listOf(5), ClueComputer.lineClue(line("#####")))
    }

    @Test
    fun `runs are reported in order`() {
        assertEquals(listOf(3, 1), ClueComputer.lineClue(line("###.#")))
        assertEquals(listOf(1, 1, 1), ClueComputer.lineClue(line("#.#.#")))
        assertEquals(listOf(2), ClueComputer.lineClue(line(".##..")))
    }

    @Test
    fun `rows and columns of a picture`() {
        val solution = Solution.fromText(
            """
            ##..#
            .#.#.
            ..#..
            .#.#.
            #...#
            """,
        )
        val clues = ClueComputer.compute(solution)
        assertEquals(listOf(listOf(2, 1), listOf(1, 1), listOf(1), listOf(1, 1), listOf(1, 1)), clues.rows)
        assertEquals(listOf(listOf(1, 1), listOf(2, 1), listOf(1), listOf(1, 1), listOf(1, 1)), clues.cols)
        assertEquals(5, clues.size)
    }
}
