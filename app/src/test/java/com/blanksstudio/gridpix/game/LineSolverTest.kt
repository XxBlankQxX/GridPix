package com.blanksstudio.gridpix.game

import com.blanksstudio.gridpix.game.LineSolver.EMPTY
import com.blanksstudio.gridpix.game.LineSolver.FILLED
import com.blanksstudio.gridpix.game.LineSolver.UNKNOWN
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LineSolverTest {

    /** `?` unknown, `#` filled, `.` empty. */
    private fun line(s: String) = IntArray(s.length) {
        when (s[it]) {
            '#' -> FILLED
            '.' -> EMPTY
            else -> UNKNOWN
        }
    }

    private fun assertSolves(clue: List<Int>, input: String, expected: String) {
        assertArrayEquals("clue $clue on $input", line(expected), LineSolver.solve(clue, line(input)))
    }

    @Test
    fun `full clue fills the whole line`() = assertSolves(listOf(5), "?????", "#####")

    @Test
    fun `empty clue empties the whole line`() = assertSolves(emptyList(), "?????", ".....")

    @Test
    fun `overlap deduces the middle cells`() {
        assertSolves(listOf(3), "?????", "??#??")
        assertSolves(listOf(4), "?????", "?###?")
    }

    @Test
    fun `blocks that exactly fit are fully placed`() = assertSolves(listOf(2, 2), "?????", "##.##")

    @Test
    fun `nothing deducible leaves the line unknown`() = assertSolves(listOf(1), "?????", "?????")

    @Test
    fun `known cells constrain the placement`() {
        // A filled cell at the edge pins the block there.
        assertSolves(listOf(2), "#????", "##...")
        // A known empty splits the line; the block must be on the right and cell 0 cannot hold it.
        assertSolves(listOf(3), "?.???", "..###")
        // Filled cell in the middle with clue 1: everything else is empty.
        assertSolves(listOf(1), "??#??", "..#..")
    }

    @Test
    fun `fully known line is accepted unchanged`() {
        assertSolves(listOf(2, 1), "##.#.", "##.#.")
        assertSolves(emptyList(), ".....", ".....")
    }

    @Test
    fun `block cannot touch a filled cell beyond its end`() {
        // Clue 1 with two separated filled cells can never fit.
        assertNull(LineSolver.solve(listOf(1), line("#.#..")))
    }

    @Test
    fun `contradictions return null`() {
        assertNull(LineSolver.solve(listOf(5), line("?.???")))
        assertNull(LineSolver.solve(emptyList(), line("??#??")))
        assertSolves(listOf(3, 3), "???????", "###.###") // needs exactly 7 cells
        assertNull(LineSolver.solve(listOf(3, 3), line("??????"))) // one short
    }

    @Test
    fun `long line with many single blocks still solves quickly`() {
        val clue = List(7) { 1 }
        val result = LineSolver.solve(clue, line("?".repeat(20)))
        assertArrayEquals(line("?".repeat(20)), result)
    }
}
