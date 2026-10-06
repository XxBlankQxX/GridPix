package com.blanksstudio.gridpix.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WinCheckerTest {

    private val picture = Solution.fromText(
        """
        #.#
        .#.
        #.#
        """,
    )

    private fun board(vararg rows: String) = GridState.decode(rows.size, rows.joinToString(""))

    @Test
    fun `exact fill wins`() {
        assertTrue(WinChecker.isSolved(picture, board("#.#", ".#.", "#.#")))
    }

    @Test
    fun `marks are ignored`() {
        assertTrue(WinChecker.isSolved(picture, board("#x#", "x#x", "#x#")))
    }

    @Test
    fun `missing or extra fills do not win`() {
        assertFalse(WinChecker.isSolved(picture, board("#.#", "...", "#.#")))
        assertFalse(WinChecker.isSolved(picture, board("###", ".#.", "#.#")))
        assertFalse(WinChecker.isSolved(picture, GridState.empty(3)))
    }

    @Test
    fun `mistakes lists wrongly filled cells only`() {
        assertEquals(listOf(0 to 1), WinChecker.mistakes(picture, board("###", "x..", "#.#")))
        assertEquals(emptyList<Pair<Int, Int>>(), WinChecker.mistakes(picture, board("#..", "...", "...")))
    }
}
