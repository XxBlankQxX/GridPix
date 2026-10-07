package com.blanksstudio.gridpix.data

import com.blanksstudio.gridpix.data.packs.PackParser
import com.blanksstudio.gridpix.data.packs.PackRepository
import com.blanksstudio.gridpix.data.packs.PackTranslations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every language names file must cover every pack with exactly one non-blank name per picture, and
 * every values-xx string file must define the same string names as the English ones (no missing text).
 */
class TranslationsTest {

    private val packs = PackRepository.PACK_ORDER.map { PackParser.parse(File("src/main/assets/packs/$it.json").readText()) }

    @Test
    fun `pack name files cover every pack and picture`() {
        for (lang in PackTranslations.LANGUAGES) {
            val file = File("src/main/assets/packs/i18n/$lang.json")
            assertTrue("missing ${file.path}", file.isFile)
            val names = PackTranslations.parse(file.readText())
            for (pack in packs) {
                val n = names[pack.id]
                assertTrue("$lang: pack ${pack.id} missing", n != null)
                assertTrue("$lang: pack ${pack.id} has no name", !n!!.packName.isNullOrBlank())
                assertEquals("$lang: ${pack.id} picture count", pack.puzzles.size, n.puzzleNames.size)
                assertTrue("$lang: ${pack.id} blank picture name", n.puzzleNames.none { it.isBlank() })
                assertTrue("$lang: forbidden word", n.puzzleNames.none { it.contains("picross", ignoreCase = true) })
            }
            val applied = PackTranslations.apply(packs, names)
            assertEquals(names["starter"]!!.puzzleNames[0], applied.first { it.id == "starter" }.puzzles[0].name)
        }
    }

    @Test
    fun `missing translations fall back to English`() {
        val applied = PackTranslations.apply(packs, emptyMap())
        assertEquals(packs.map { it.name }, applied.map { it.name })
    }

    private fun stringNames(file: File): Set<String> =
        Regex("""<(string|plurals) name="([^"]+)"""").findAll(file.readText()).map { it.groupValues[2] }.toSet()

    @Test
    fun `every language translates every app string`() {
        val english = File("src/main/res/values").listFiles { f -> f.name.startsWith("strings") }!!.associate { it.name to stringNames(it) }
        for (lang in PackTranslations.LANGUAGES) {
            val dir = File("src/main/res/values-$lang")
            for ((fileName, keys) in english) {
                val translated = File(dir, fileName)
                assertTrue("missing ${translated.path}", translated.isFile)
                val missing = keys - stringNames(translated) - setOf("app_name")
                assertTrue("${translated.path} missing: $missing", missing.isEmpty())
            }
        }
    }
}
