package com.blanksstudio.gridpix.ui.navigation

/** String routes for Navigation Compose. Puzzle kinds map to the id formats in SPEC section 5. */
object Routes {
    const val HOME = "home"
    const val PACKS = "packs"
    const val PACK = "pack/{packId}"
    const val SHOP = "shop"
    const val SETTINGS = "settings"
    const val STATS = "stats"
    const val PUZZLE = "puzzle/{kind}/{a}/{b}"

    const val ARG_PACK_ID = "packId"
    const val ARG_KIND = "kind"
    const val ARG_A = "a"
    const val ARG_B = "b"

    const val KIND_ENDLESS = "endless"
    const val KIND_DAILY = "daily"
    const val KIND_PACK = "pack"
    const val KIND_TUTORIAL = "tutorial"

    fun pack(packId: String) = "pack/$packId"
    fun endless(size: Int, seed: Long) = "puzzle/$KIND_ENDLESS/$size/$seed"
    fun daily(isoDate: String) = "puzzle/$KIND_DAILY/$isoDate/0"
    fun packPuzzle(packId: String, index: Int) = "puzzle/$KIND_PACK/$packId/$index"
    fun tutorial(step: Int) = "puzzle/$KIND_TUTORIAL/$step/0"
}
