package com.blanksstudio.gridpix.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class GridModelTest {

    @Test
    fun `grid state encodes and decodes losslessly`() {
        val board = GridState.empty(3)
            .with(0, 0, CellState.FILLED)
            .with(1, 1, CellState.MARKED)
            .with(2, 2, CellState.FILLED)
        assertEquals("#...x...#", board.encode())
        assertEquals(board, GridState.decode(3, board.encode()))
        assertEquals(2, board.count(CellState.FILLED))
        assertEquals(1, board.count(CellState.MARKED))
    }

    @Test
    fun `with returns a new instance and leaves the original untouched`() {
        val a = GridState.empty(2)
        val b = a.with(0, 1, CellState.FILLED)
        assertNotSame(a, b)
        assertEquals(CellState.EMPTY, a[0, 1])
        assertEquals(CellState.FILLED, b[0, 1])
        assertSame(b, b.with(0, 1, CellState.FILLED)) // no-op move allocates nothing
    }

    @Test
    fun `decode rejects wrong length or unknown codes`() {
        assertThrows(IllegalArgumentException::class.java) { GridState.decode(3, "....") }
        assertThrows(IllegalArgumentException::class.java) { GridState.decode(2, "..?." ) }
    }

    @Test
    fun `solution text round trip`() {
        val lines = listOf("#.#", "...", ".##")
        val solution = Solution.fromText(lines)
        assertEquals(lines, solution.toText())
        assertEquals(4, solution.filledCount)
        assertEquals(solution, Solution.fromText(solution.toString()))
    }

    @Test
    fun `solution rejects non-square or bad characters`() {
        assertThrows(IllegalArgumentException::class.java) { Solution.fromText(listOf("##", "#")) }
        assertThrows(IllegalArgumentException::class.java) { Solution.fromText(listOf("#x", "..")) }
        assertThrows(IllegalArgumentException::class.java) { Solution.fromText(emptyList()) }
    }

    @Test
    fun `toggle inverts exactly one cell`() {
        val solution = Solution.fromText(listOf("..", ".."))
        val toggled = solution.withToggled(1, 0)
        assertEquals(listOf("..", "#."), toggled.toText())
        assertEquals(solution, toggled.withToggled(1, 0))
    }
}
