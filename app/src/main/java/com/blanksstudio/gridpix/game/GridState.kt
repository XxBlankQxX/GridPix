package com.blanksstudio.gridpix.game

/**
 * The player's board: an immutable N x N grid of [CellState]. Every move returns a new
 * instance, which keeps undo/redo and auto-save simple (SPEC section 2).
 *
 * [encode]/[decode] produce the N*N character string stored in `puzzle_progress.state`
 * (SPEC section 5), row-major using [CellState.code].
 */
class GridState private constructor(
    val size: Int,
    private val cells: Array<CellState>,
) {
    operator fun get(row: Int, col: Int): CellState = cells[row * size + col]

    fun with(row: Int, col: Int, state: CellState): GridState {
        if (this[row, col] == state) return this
        val copy = cells.copyOf()
        copy[row * size + col] = state
        return GridState(size, copy)
    }

    fun count(state: CellState): Int = cells.count { it == state }

    fun encode(): String = buildString(cells.size) { cells.forEach { append(it.code) } }

    override fun equals(other: Any?): Boolean =
        other is GridState && other.size == size && other.cells.contentEquals(cells)

    override fun hashCode(): Int = 31 * size + cells.contentHashCode()

    override fun toString(): String = encode().chunked(size).joinToString("\n")

    companion object {
        fun empty(size: Int): GridState {
            require(size > 0) { "size must be positive" }
            return GridState(size, Array(size * size) { CellState.EMPTY })
        }

        fun decode(size: Int, encoded: String): GridState {
            require(encoded.length == size * size) {
                "encoded state has ${encoded.length} chars, expected ${size * size}"
            }
            return GridState(size, Array(size * size) { CellState.fromCode(encoded[it]) })
        }
    }
}
