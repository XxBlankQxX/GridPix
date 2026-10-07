package com.blanksstudio.gridpix.data

import com.blanksstudio.gridpix.data.packs.PackParser
import com.blanksstudio.gridpix.data.packs.PackRepository
import com.blanksstudio.gridpix.game.PuzzleSolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * CLAUDE.md rule: every pack JSON in assets/packs/ is loaded here and must pass the solver.
 * Runs on the JVM with the real org.json (test dependency), reading the asset files from disk.
 * Size split must match tools/nonogram_check.py: puzzles 1-20 small size, 21-30 large size.
 */
class PackContentTest {

    private val packsDir = File("src/main/assets/packs")

    private fun packFiles() = PackRepository.PACK_ORDER.map { File(packsDir, "$it.json") }

    private fun sizesFor(packId: String) = if (packId == "anime") 15 to 20 else 10 to 15

    @Test
    fun `all packs are present`() {
        for (file in packFiles()) assertTrue("missing ${file.path}", file.isFile)
    }

    @Test
    fun `every pack puzzle is well formed, coloured and solvable without guessing`() {
        for (file in packFiles()) {
            if (!file.isFile) continue
            val pack = PackParser.parse(file.readText())
            assertEquals(file.nameWithoutExtension, pack.id)
            assertEquals("${pack.id}: puzzle count", 30, pack.puzzles.size)
            assertEquals("${pack.id}: duplicate names", 30, pack.puzzles.map { it.name }.toSet().size)
            if (pack.id == "starter") assertEquals(null, pack.productId) else assertEquals("pack_${pack.id}", pack.productId)
            val (small, large) = sizesFor(pack.id)
            for (p in pack.puzzles) {
                val where = "${pack.id} #${p.index} ${p.name}"
                assertEquals("$where: size", if (p.index <= 20) small else large, p.solution.size)
                assertTrue("$where: not line-solvable", PuzzleSolver.isLineSolvable(p.solution))
                assertTrue("$where: forbidden word", !p.name.contains("picross", ignoreCase = true))
                assertEquals("pack-${pack.id}-" + p.index.toString().padStart(2, '0'), p.toPuzzle(pack.id).id)
                val colors = p.colors
                assertNotNull("$where: missing colour map", colors)
                for (r in 0 until p.solution.size) for (c in 0 until p.solution.size) {
                    // Parser already enforces this; asserting keeps the rule visible in the test.
                    assertEquals("$where ($r,$c)", p.solution[r, c], colors!![r, c] != 0)
                }
            }
        }
    }

    @Test
    fun `parser reads the documented format with colours`() {
        val pack = PackParser.parse(
            """
            {"id":"demo","name":"Demo","product_id":null,
             "puzzles":[{"name":"Dot","grid":["#..","...","..."],
                         "palette":{"a":"#FF8000"},"colors":["a..","...","..."]}]}
            """,
        )
        val p = pack.puzzles.single()
        assertEquals("pack-demo-01", p.toPuzzle("demo").id)
        assertEquals(0xFFFF8000.toInt(), p.colors!![0, 0])
        assertEquals(0, p.colors!![0, 1])
    }

    @Test
    fun `colour map that disagrees with the grid is rejected`() {
        fun parse(colors: String) = PackParser.parse(
            """{"id":"d","name":"D","product_id":null,"puzzles":[{"name":"X","grid":["#.","..",],
               "palette":{"a":"#112233"},"colors":$colors}]}""".replace(",]", "]"),
        )
        assertThrows(IllegalArgumentException::class.java) { parse("""["a.","a."]""") } // colour on empty cell
        assertThrows(IllegalArgumentException::class.java) { parse("""["..",".."]""") } // filled cell uncoloured
        assertThrows(IllegalArgumentException::class.java) { parse("""["b.",".."]""") } // unknown letter
        assertNotEquals(0, parse("""["a.",".."]""").puzzles.single().colors!![0, 0])
    }

    @Test
    fun `pack without colours still parses`() {
        val pack = PackParser.parse("""{"id":"d","name":"D","product_id":null,"puzzles":[{"name":"X","grid":["#.",".."]}]}""")
        assertEquals(null, pack.puzzles.single().colors)
    }
}
