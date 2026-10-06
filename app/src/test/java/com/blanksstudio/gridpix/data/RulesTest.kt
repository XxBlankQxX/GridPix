package com.blanksstudio.gridpix.data

import com.blanksstudio.gridpix.billing.Entitlements
import com.blanksstudio.gridpix.billing.Products
import com.blanksstudio.gridpix.data.rules.HintRules
import com.blanksstudio.gridpix.data.rules.HintRules.Wallet
import com.blanksstudio.gridpix.data.rules.StreakRules
import com.blanksstudio.gridpix.data.rules.StreakRules.Streak
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesTest {

    @Test
    fun `free hints reset on a new day and are spent before the balance`() {
        val yesterday = Wallet(freeHintsDate = "2026-10-06", freeHintsLeft = 0, balance = 2)
        assertEquals(5, HintRules.available(yesterday, "2026-10-07"))
        assertEquals(2, HintRules.available(yesterday, "2026-10-06"))

        var w = HintRules.spend(yesterday, "2026-10-07")!!
        assertEquals(Wallet("2026-10-07", 2, 2), w)
        w = HintRules.spend(w, "2026-10-07")!!
        w = HintRules.spend(w, "2026-10-07")!!
        assertEquals(0, w.freeHintsLeft)
        w = HintRules.spend(w, "2026-10-07")!!
        assertEquals(Wallet("2026-10-07", 0, 1), w)
        w = HintRules.spend(w, "2026-10-07")!!
        assertNull(HintRules.spend(w, "2026-10-07"))
        assertEquals(Wallet("2026-10-07", 0, 10), HintRules.credit(w, 10))
    }

    @Test
    fun `fresh install has three free hints`() {
        val fresh = Wallet(freeHintsDate = "", freeHintsLeft = HintRules.FREE_PER_DAY, balance = 0)
        assertEquals(3, HintRules.available(fresh, "2026-10-07"))
    }

    @Test
    fun `streak extends on consecutive days and resets after a gap`() {
        var s = Streak(0, null)
        s = StreakRules.onDailySolved(s, "2026-10-06", "2026-10-05")
        assertEquals(Streak(1, "2026-10-06"), s)
        s = StreakRules.onDailySolved(s, "2026-10-06", "2026-10-05") // same day again
        assertEquals(Streak(1, "2026-10-06"), s)
        s = StreakRules.onDailySolved(s, "2026-10-07", "2026-10-06")
        assertEquals(Streak(2, "2026-10-07"), s)
        assertEquals(2, StreakRules.current(s, "2026-10-08", "2026-10-07")) // still alive today
        assertEquals(0, StreakRules.current(s, "2026-10-09", "2026-10-08")) // missed a day
        s = StreakRules.onDailySolved(s, "2026-10-09", "2026-10-08")
        assertEquals(Streak(1, "2026-10-09"), s)
    }

    @Test
    fun `entitlements follow the spec`() {
        val none = emptySet<String>()
        assertTrue(Entitlements.packUnlocked(none, null))
        assertFalse(Entitlements.packUnlocked(none, Products.PACK_ANIMALS))
        assertTrue(Entitlements.packUnlocked(setOf(Products.PACK_ANIMALS), Products.PACK_ANIMALS))
        assertTrue(Entitlements.packUnlocked(setOf(Products.EVERYTHING), Products.PACK_FOOD))

        assertTrue(Entitlements.sizeUnlocked(none, 5))
        assertTrue(Entitlements.sizeUnlocked(none, 10))
        assertFalse(Entitlements.sizeUnlocked(none, 15))
        assertFalse(Entitlements.sizeUnlocked(none, 20))
        assertTrue(Entitlements.sizeUnlocked(setOf(Products.LARGE_GRIDS), 20))
        assertTrue(Entitlements.sizeUnlocked(setOf(Products.EVERYTHING), 15))

        assertEquals(Products.NON_CONSUMABLES, Entitlements.effectivelyOwned(setOf(Products.EVERYTHING)))
        assertEquals(setOf(Products.PACK_FOOD), Entitlements.effectivelyOwned(setOf(Products.PACK_FOOD, Products.HINTS_10)))
    }

    @Test
    fun `hint grants per product`() {
        assertEquals(10, Products.hintsGranted(Products.HINTS_10))
        assertEquals(50, Products.hintsGranted(Products.HINTS_50))
        assertEquals(100, Products.hintsGranted(Products.EVERYTHING))
        assertEquals(0, Products.hintsGranted(Products.PACK_ANIMALS))
        assertEquals(8, Products.ALL.size)
    }
}
