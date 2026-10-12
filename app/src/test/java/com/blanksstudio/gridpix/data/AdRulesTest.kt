package com.blanksstudio.gridpix.data

import com.blanksstudio.gridpix.billing.Entitlements
import com.blanksstudio.gridpix.billing.Products
import com.blanksstudio.gridpix.data.rules.AdRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdRulesTest {
    private val hour = 60 * 60 * 1000L
    private val minute = 60 * 1000L
    private val installed = 1_000_000_000_000L

    @Test
    fun `no ads on the first day`() {
        assertFalse(AdRules.graceOver(null, installed))
        assertFalse(AdRules.graceOver(installed, installed + 23 * hour))
        assertTrue(AdRules.graceOver(installed, installed + 24 * hour))
    }

    @Test
    fun `setting the clock back does not extend the ad-free day`() {
        assertTrue(AdRules.graceOver(installed, installed - hour))
    }

    @Test
    fun `between-puzzle ad needs three solves and four minutes`() {
        val last = installed + 30 * hour
        assertFalse(AdRules.interstitialDue(2, last, last + 10 * minute))
        assertFalse(AdRules.interstitialDue(3, last, last + 3 * minute))
        assertTrue(AdRules.interstitialDue(3, last, last + 4 * minute))
        assertTrue(AdRules.interstitialDue(5, 0L, installed)) // never shown before
        assertTrue(AdRules.interstitialDue(3, last, last - hour)) // clock moved back
    }

    @Test
    fun `five video hints per day, reset only by a later date`() {
        assertEquals(5, AdRules.rewardedLeft("", 0, "2026-10-12"))
        assertEquals(2, AdRules.rewardedLeft("2026-10-12", 3, "2026-10-12"))
        assertEquals(0, AdRules.rewardedLeft("2026-10-12", 7, "2026-10-12"))
        assertEquals(0, AdRules.rewardedLeft("2026-10-12", 5, "2026-10-11")) // clock moved back
        assertEquals(5, AdRules.rewardedLeft("2026-10-12", 5, "2026-10-13"))
    }

    @Test
    fun `remove ads or everything turns ads off`() {
        assertFalse(Entitlements.adsRemoved(emptySet()))
        assertFalse(Entitlements.adsRemoved(setOf(Products.PACK_ANIME, Products.LARGE_GRIDS)))
        assertTrue(Entitlements.adsRemoved(setOf(Products.REMOVE_ADS)))
        assertTrue(Entitlements.adsRemoved(setOf(Products.EVERYTHING)))
    }
}
