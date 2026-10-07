package com.blanksstudio.gridpix.data.packs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the picture packs from `assets/packs/` once and keeps them in memory
 * (5 packs x 30 small grids). Pack order is fixed by [PACK_ORDER]: Starter first, then paid packs.
 */
@Singleton
class PackRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val mutex = Mutex()
    private var cache: List<Pack>? = null

    suspend fun packs(): List<Pack> = cache ?: mutex.withLock {
        cache ?: load().also { cache = it }
    }

    suspend fun pack(packId: String): Pack? = packs().firstOrNull { it.id == packId }

    /** Packs to list right now: seasonal packs (listed first) appear only in their month. */
    suspend fun visiblePacks(month: Int = java.time.LocalDate.now().monthValue): List<Pack> =
        packs().filter { it.visibleIn(month) }

    suspend fun puzzle(packId: String, index: Int): PackPuzzle? =
        pack(packId)?.puzzles?.getOrNull(index - 1)

    private suspend fun load(): List<Pack> = withContext(Dispatchers.IO) {
        val available = context.assets.list(PACKS_DIR).orEmpty().toSet()
        val packs = PACK_ORDER
            .filter { "$it.json" in available }
            .map { id ->
                val text = context.assets.open("$PACKS_DIR/$id.json").bufferedReader().use { it.readText() }
                PackParser.parse(text)
            }
        // Pack and picture names in the phone's language, when a names file exists (English otherwise).
        val language = java.util.Locale.getDefault().language
        if (language in PackTranslations.LANGUAGES) {
            runCatching {
                val json = context.assets.open("$PACKS_DIR/i18n/$language.json").bufferedReader().use { it.readText() }
                PackTranslations.apply(packs, PackTranslations.parse(json))
            }.getOrDefault(packs)
        } else {
            packs
        }
    }

    companion object {
        const val PACKS_DIR = "packs"
        val PACK_ORDER = listOf("halloween", "christmas", "starter", "anime", "animals", "vehicles", "food", "nature")
    }
}
