package com.blanksstudio.gridpix.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AutoCrossTest {

    private val picture = Solution.fromText(
        """
        ##...
        .....
        #.#.#
        .....
        ....#
        """,
    )
    private val clues = ClueComputer.compute(picture)

    private fun board(vararg rows: String) = GridState.decode(rows.size, rows.joinToString(""))

    @Test
    fun `completed row gets its empty cells crossed`() {
        val result = AutoCross.apply(clues, board("##...", ".....", ".....", ".....", "....."))
        assertEquals("##xxx", result.encode().substring(0, 5))
    }

    @Test
    fun `empty lines with clue 0 are crossed straight away`() {
        val result = AutoCross.apply(clues, GridState.empty(5))
        assertEquals("xxxxx", result.encode().substring(5, 10)) // row 1 has clue 0
        assertEquals("xxxxx", result.encode().substring(15, 20)) // row 3 has clue 0
        assertEquals(CellState.MARKED, result[0, 3]) // column 3 has clue 0
    }

    @Test
    fun `incomplete lines are left alone and existing marks are kept`() {
        val start = board("#....", ".....", "x....", ".....", ".....")
        val result = AutoCross.apply(clues, start)
        assertEquals(CellState.EMPTY, result[0, 1]) // row 0 needs 2, has 1
        assertEquals(CellState.MARKED, result[2, 0]) // player's own mark untouched
    }

    @Test
    fun `nothing to cross returns the same board`() {
        val solved = board("##xxx", "xxxxx", "#x#x#", "xxxxx", "xxxx#")
        assertSame(solved, AutoCross.apply(clues, solved))
    }
}
