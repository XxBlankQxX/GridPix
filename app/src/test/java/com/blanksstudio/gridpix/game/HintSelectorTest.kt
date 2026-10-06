package com.blanksstudio.gridpix.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HintSelectorTest {

    private val picture = Solution.fromText(
        """
        ###..
        #....
        #.#..
        .....
        ....#
        """,
    )

    @Test
    fun `hint comes from the line with most unresolved cells`() {
        // Row 0 and column 0 both have 3 filled cells; rows win ties.
        val hint = HintSelector.select(picture, GridState.empty(5))
        assertEquals(Hint(0, 0, CellState.FILLED), hint)
    }

    @Test
    fun `hint always reveals a correct cell`() {
        var board = GridState.empty(5)
        repeat(picture.filledCount) {
            val hint = HintSelector.select(picture, board) ?: error("ran out of hints early")
            assertTrue(picture[hint.row, hint.col])
            assertEquals(CellState.FILLED, hint.state)
            board = board.with(hint.row, hint.col, hint.state)
        }
        assertTrue(WinChecker.isSolved(picture, board))
        assertNull(HintSelector.select(picture, board))
    }

    @Test
    fun `a wrongly filled cell is corrected with a mark`() {
        // Fill the whole picture correctly, then add one wrong fill at (3,3).
        var board = GridState.empty(5)
        for (r in 0 until 5) for (c in 0 until 5) if (picture[r, c]) board = board.with(r, c, CellState.FILLED)
        board = board.with(3, 3, CellState.FILLED)
        assertEquals(Hint(3, 3, CellState.MARKED), HintSelector.select(picture, board))
    }

    @Test
    fun `marks on empty cells do not count as unresolved`() {
        var board = GridState.empty(5)
        for (r in 0 until 5) for (c in 0 until 5) if (picture[r, c]) board = board.with(r, c, CellState.FILLED)
        board = board.with(3, 0, CellState.MARKED)
        assertNull(HintSelector.select(picture, board))
    }

    @Test
    fun `column is chosen when it has strictly more unresolved cells`() {
        val tall = Solution.fromText(
            """
            #..
            #..
            #..
            """,
        )
        assertEquals(Hint(0, 0, CellState.FILLED), HintSelector.select(tall, GridState.empty(3)))
        // Resolve (0,0); column 0 still has 2 unresolved, each row has at most 1.
        val board = GridState.empty(3).with(0, 0, CellState.FILLED)
        assertEquals(Hint(1, 0, CellState.FILLED), HintSelector.select(tall, board))
    }
}
