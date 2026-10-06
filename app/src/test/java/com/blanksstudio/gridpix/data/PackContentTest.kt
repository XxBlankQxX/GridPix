package com.blanksstudio.gridpix.data

import com.blanksstudio.gridpix.data.packs.PackParser
import com.blanksstudio.gridpix.data.packs.PackRepository
import com.blanksstudio.gridpix.game.PuzzleSolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * CLAUDE.md rule: every pack JSON in assets/packs/ is loaded here and must pass the solver.
 * Runs on the JVM with the real org.json (test dependency), reading the asset files from disk.
 */
class PackContentTest {

    private val packsDir = File("src/main/assets/packs")

    private fun packFiles() = PackRepository.PACK_ORDER.map { File(packsDir, "$it.json") }

    @Test
    fun `all five packs are present`() {
        for (file in packFiles()) assertTrue("missing ${file.path}", file.isFile)
    }

    @Test
    fun `every pack puzzle is well formed and solvable without guessing`() {
        for (file in packFiles()) {
            if (!file.isFile) continue
            val pack = PackParser.parse(file.readText())
            assertEquals(file.nameWithoutExtension, pack.id)
            assertEquals("${pack.id}: puzzle count", 30, pack.puzzles.size)
            assertEquals("${pack.id}: duplicate names", 30, pack.puzzles.map { it.name }.toSet().size)
            if (pack.id == "starter") assertEquals(null, pack.productId) else assertEquals("pack_${pack.id}", pack.productId)
            for (p in pack.puzzles) {
                assertTrue("${pack.id} #${p.index} ${p.name}: size ${p.solution.size}", p.solution.size in setOf(10, 15))
                assertTrue("${pack.id} #${p.index} ${p.name}: not line-solvable", PuzzleSolver.isLineSolvable(p.solution))
                assertTrue("${pack.id} #${p.index}: name contains forbidden word", !p.name.contains("picross", ignoreCase = true))
                assertEquals("pack-${pack.id}-" + p.index.toString().padStart(2, '0'), p.toPuzzle(pack.id).id)
            }
            assertEquals("${pack.id}: 10x10 count", 20, pack.puzzles.count { it.solution.size == 10 })
            assertEquals("${pack.id}: 15x15 count", 10, pack.puzzles.count { it.solution.size == 15 })
        }
    }

    @Test
    fun `parser reads the documented format`() {
        val pack = PackParser.parse(
            """
            {"id":"demo","name":"Demo","product_id":null,
             "puzzles":[{"name":"Dot","grid":["#..","...","..."]}]}
            """,
        )
        assertEquals("demo", pack.id)
        assertEquals(null, pack.productId)
        assertNotNull(pack.puzzles.single().solution)
        assertEquals("pack-demo-01", pack.puzzles.single().toPuzzle("demo").id)
        assertEquals("Dot", pack.puzzles.single().toPuzzle("demo").name)
    }
}
