package com.blanksstudio.gridpix.data.packs

import com.blanksstudio.gridpix.game.Puzzle
import com.blanksstudio.gridpix.game.PuzzleIds
import com.blanksstudio.gridpix.game.Solution

/** One hand-authored picture pack from `assets/packs/<id>.json` (SPEC section 3). */
data class Pack(
    val id: String,
    val name: String,
    /** Play product id, or null when the pack is free (Starter). */
    val productId: String?,
    val puzzles: List<PackPuzzle>,
) {
    val idPrefix: String get() = "pack-$id-"
}

data class PackPuzzle(
    /** 1-based position in the pack; the puzzle id is `pack-<pack>-NN`. */
    val index: Int,
    val name: String,
    val solution: Solution,
) {
    fun toPuzzle(packId: String): Puzzle = Puzzle.from(PuzzleIds.pack(packId, index), solution, name)
}

/** Parses the pack JSON text. Pure Kotlin apart from org.json, which the unit tests supply for the JVM. */
object PackParser {

    fun parse(json: String): Pack {
        val root = org.json.JSONObject(json)
        val id = root.getString("id")
        val name = root.getString("name")
        val productId = if (root.isNull("product_id")) null else root.getString("product_id")
        val array = root.getJSONArray("puzzles")
        val puzzles = List(array.length()) { i ->
            val obj = array.getJSONObject(i)
            val grid = obj.getJSONArray("grid")
            val rows = List(grid.length()) { r -> grid.getString(r) }
            PackPuzzle(index = i + 1, name = obj.getString("name"), solution = Solution.fromText(rows))
        }
        return Pack(id, name, productId, puzzles)
    }
}
