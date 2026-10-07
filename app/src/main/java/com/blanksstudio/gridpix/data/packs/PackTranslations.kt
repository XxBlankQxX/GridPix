package com.blanksstudio.gridpix.data.packs

/**
 * Translated pack and picture names from `assets/packs/i18n/<language>.json` (decision D27):
 *
 *   { "language": "es",
 *     "packs": { "starter": { "name": "Inicial", "puzzles": ["Corazón", "Estrella", ...] }, ... } }
 *
 * "puzzles" lists the names in pack order. Anything missing falls back to the English name, so a
 * new pack never breaks a language file. App UI text uses the normal res/values-xx/strings*.xml.
 */
object PackTranslations {

    /** Languages with a names file, besides English. */
    val LANGUAGES = listOf("es", "pt", "fr", "de")

    data class Names(val packName: String?, val puzzleNames: List<String>)

    fun parse(json: String): Map<String, Names> {
        val packs = org.json.JSONObject(json).getJSONObject("packs")
        return packs.keys().asSequence().associateWith { id ->
            val obj = packs.getJSONObject(id)
            val list = obj.optJSONArray("puzzles")
            Names(
                packName = obj.optString("name").takeIf { it.isNotBlank() },
                puzzleNames = if (list == null) emptyList() else List(list.length()) { list.getString(it) },
            )
        }
    }

    fun apply(packs: List<Pack>, names: Map<String, Names>): List<Pack> = packs.map { pack ->
        val n = names[pack.id] ?: return@map pack
        pack.copy(
            name = n.packName ?: pack.name,
            puzzles = pack.puzzles.map { p -> p.copy(name = n.puzzleNames.getOrNull(p.index - 1)?.takeIf { it.isNotBlank() } ?: p.name) },
        )
    }
}
