package com.blanksstudio.gridpix.game

/**
 * State of one cell on the player's board (SPEC section 2).
 * [code] is the single character used when the board is saved as text (SPEC section 5):
 * `.` empty, `#` filled, `x` marked as known-empty.
 */
enum class CellState(val code: Char) {
    EMPTY('.'),
    FILLED('#'),
    MARKED('x');

    companion object {
        fun fromCode(code: Char): CellState =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown cell code '$code'")
    }
}
