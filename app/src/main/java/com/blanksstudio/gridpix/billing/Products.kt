package com.blanksstudio.gridpix.billing

/** Play product ids fixed by SPEC section 6. Never invent new ones. */
object Products {
    const val HINTS_10 = "hints_10"
    const val HINTS_50 = "hints_50"
    const val PACK_ANIMALS = "pack_animals"
    const val PACK_VEHICLES = "pack_vehicles"
    const val PACK_FOOD = "pack_food"
    const val PACK_NATURE = "pack_nature"
    /** Added 2026-10-07 (Christopher approved, decision D25). */
    const val PACK_ANIME = "pack_anime"
    const val LARGE_GRIDS = "large_grids"
    const val EVERYTHING = "everything"

    val CONSUMABLES: Set<String> = setOf(HINTS_10, HINTS_50)
    val NON_CONSUMABLES: Set<String> =
        setOf(PACK_ANIMALS, PACK_VEHICLES, PACK_FOOD, PACK_NATURE, PACK_ANIME, LARGE_GRIDS, EVERYTHING)
    val ALL: Set<String> = CONSUMABLES + NON_CONSUMABLES

    /** Hints granted when a consumable is bought (SPEC section 6; `everything` grants 100 once). */
    fun hintsGranted(productId: String): Int = when (productId) {
        HINTS_10 -> 10
        HINTS_50 -> 50
        EVERYTHING -> 100
        else -> 0
    }
}

/**
 * What the owned product set unlocks. Pure logic so it is unit-tested; the Shop, Packs and
 * size picker all go through here instead of checking product ids themselves.
 */
object Entitlements {

    fun ownsEverything(owned: Set<String>): Boolean = Products.EVERYTHING in owned

    /** [packProductId] is the pack's `product_id` from its JSON; null means the pack is free. */
    fun packUnlocked(owned: Set<String>, packProductId: String?): Boolean =
        packProductId == null || packProductId in owned || ownsEverything(owned)

    /** Sizes 5 and 10 are free; 15 and 20 need `large_grids` or `everything` (SPEC section 3). */
    fun sizeUnlocked(owned: Set<String>, size: Int): Boolean =
        size <= 10 || Products.LARGE_GRIDS in owned || ownsEverything(owned)

    /** Products that are already covered and so should show as owned in the Shop. */
    fun effectivelyOwned(owned: Set<String>): Set<String> =
        if (ownsEverything(owned)) Products.NON_CONSUMABLES else owned.intersect(Products.NON_CONSUMABLES)
}
