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
    /** Months (1-12) a free seasonal pack is shown in; empty for regular packs. */
    val seasonMonths: List<Int> = emptyList(),
) {
    val idPrefix: String get() = "pack-$id-"
    val isSeasonal: Boolean get() = seasonMonths.isNotEmpty()

    /** Regular packs are always listed; seasonal ones only in their months. */
    fun visibleIn(month: Int): Boolean = !isSeasonal || month in seasonMonths
}

data class PackPuzzle(
    /** 1-based position in the pack; the puzzle id is `pack-<pack>-NN`. */
    val index: Int,
    val name: String,
    val solution: Solution,
    /** Colours for the solved reveal (see [PixelColors]); null for packs without colour data. */
    val colors: PixelColors? = null,
) {
    fun toPuzzle(packId: String): Puzzle = Puzzle.from(PuzzleIds.pack(packId, index), solution, name)
}

/**
 * Per-cell colours for a solved picture: 0xAARRGGBB, or 0 for an empty cell. Only used for the
 * reveal, thumbnails and the collection; solving stays one colour (SPEC section 7: no colour nonograms).
 */
class PixelColors(val size: Int, private val argb: IntArray) {
    init {
        require(argb.size == size * size)
    }

    operator fun get(row: Int, col: Int): Int = argb[row * size + col]

    companion object {
        /**
         * Builds from the pack JSON `palette` (letter -> #RRGGBB) and `colors` rows. Every filled cell of
         * [solution] must have a palette letter and every empty cell must be '.', so the colour map can
         * never disagree with the puzzle.
         */
        fun parse(solution: Solution, palette: Map<Char, String>, rows: List<String>): PixelColors {
            val n = solution.size
            require(rows.size == n) { "colors has ${rows.size} rows, expected $n" }
            val parsed = palette.mapValues { (key, hex) ->
                require(Regex("#[0-9A-Fa-f]{6}").matches(hex)) { "palette '$key' has bad colour $hex" }
                (0xFF000000L or hex.substring(1).toLong(16)).toInt()
            }
            val argb = IntArray(n * n)
            rows.forEachIndexed { r, row ->
                require(row.length == n) { "colors row $r has ${row.length} chars, expected $n" }
                row.forEachIndexed { c, ch ->
                    if (solution[r, c]) {
                        argb[r * n + c] = parsed[ch] ?: throw IllegalArgumentException("colors row $r col $c: '$ch' not in palette")
                    } else {
                        require(ch == '.') { "colors row $r col $c: '$ch' on an empty cell" }
                    }
                }
            }
            return PixelColors(n, argb)
        }
    }
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
            val solution = Solution.fromText(List(grid.length()) { r -> grid.getString(r) })
            val colors = if (obj.has("palette") && obj.has("colors")) {
                val paletteJson = obj.getJSONObject("palette")
                val palette = paletteJson.keys().asSequence().associate { key -> key.single() to paletteJson.getString(key) }
                val colorRows = obj.getJSONArray("colors")
                PixelColors.parse(solution, palette, List(colorRows.length()) { r -> colorRows.getString(r) })
            } else {
                null
            }
            PackPuzzle(index = i + 1, name = obj.getString("name"), solution = solution, colors = colors)
        }
        val months = root.optJSONArray("season_months")?.let { a -> List(a.length()) { a.getInt(it) } } ?: emptyList()
        return Pack(id, name, productId, puzzles, months)
    }
}
