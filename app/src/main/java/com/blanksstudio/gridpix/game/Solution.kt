package com.blanksstudio.gridpix.game

/**
 * The hidden picture: an immutable N x N grid of filled/empty cells.
 * Text form uses `#` for filled and `.` for empty, one row per line, which is
 * also the authoring format for pack puzzles (SPEC section 3).
 */
class Solution private constructor(
    val size: Int,
    private val cells: BooleanArray,
) {
    init {
        require(size > 0) { "size must be positive" }
        require(cells.size == size * size) { "expected ${size * size} cells, got ${cells.size}" }
    }

    operator fun get(row: Int, col: Int): Boolean = cells[row * size + col]

    val filledCount: Int get() = cells.count { it }

    fun row(row: Int): BooleanArray = BooleanArray(size) { col -> this[row, col] }

    fun col(col: Int): BooleanArray = BooleanArray(size) { row -> this[row, col] }

    /** Copy with one cell inverted. Used by the generator's repair step. */
    fun withToggled(row: Int, col: Int): Solution {
        val copy = cells.copyOf()
        copy[row * size + col] = !copy[row * size + col]
        return Solution(size, copy)
    }

    fun toText(): List<String> = List(size) { row ->
        buildString(size) { for (col in 0 until size) append(if (this@Solution[row, col]) FILLED else EMPTY) }
    }

    override fun equals(other: Any?): Boolean =
        other is Solution && other.size == size && other.cells.contentEquals(cells)

    override fun hashCode(): Int = 31 * size + cells.contentHashCode()

    override fun toString(): String = toText().joinToString("\n")

    companion object {
        const val FILLED = '#'
        const val EMPTY = '.'

        fun of(size: Int, cells: BooleanArray): Solution = Solution(size, cells.copyOf())

        /** Parses the `#`/`.` text form. Every line must be exactly [lines].size characters. */
        fun fromText(lines: List<String>): Solution {
            val size = lines.size
            require(size > 0) { "solution has no rows" }
            val cells = BooleanArray(size * size)
            lines.forEachIndexed { row, line ->
                require(line.length == size) { "row $row has ${line.length} chars, expected $size" }
                line.forEachIndexed { col, ch ->
                    cells[row * size + col] = when (ch) {
                        FILLED -> true
                        EMPTY -> false
                        else -> throw IllegalArgumentException("row $row col $col: unexpected '$ch'")
                    }
                }
            }
            return Solution(size, cells)
        }

        fun fromText(text: String): Solution = fromText(text.trim().lines().map { it.trim() })
    }
}
