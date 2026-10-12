package com.blanksstudio.gridpix.data.rules

/**
 * When ads may show (decision D29, "fair ads"). Pure logic over stored values so it is unit-tested.
 * Times are epoch milliseconds; dates are local `yyyy-MM-dd`.
 */
object AdRules {
    /** No ads at all during the first day after install. */
    const val GRACE_MS = 24 * 60 * 60 * 1000L
    /** A between-puzzles ad at most after every 3rd solve... */
    const val SOLVES_PER_INTERSTITIAL = 3
    /** ...and never twice within 4 minutes. */
    const val MIN_INTERSTITIAL_GAP_MS = 4 * 60 * 1000L
    /** Watch-a-video hints per day, so videos can't replace bought hints completely. */
    const val REWARDED_PER_DAY = 5

    /**
     * True once the free first day is over. Unknown install time (not recorded yet) means still in
     * the grace period. A clock set back before the install time ends the grace period, so moving
     * the clock can't keep the first day going for ever.
     */
    fun graceOver(firstLaunchAt: Long?, now: Long): Boolean = when {
        firstLaunchAt == null -> false
        now < firstLaunchAt -> true
        else -> now - firstLaunchAt >= GRACE_MS
    }

    /**
     * A between-puzzles ad is due after [SOLVES_PER_INTERSTITIAL] solves since the last one and at
     * least [MIN_INTERSTITIAL_GAP_MS] later. A clock set back before the last ad counts as enough time.
     */
    fun interstitialDue(solvesSinceLast: Int, lastShownAt: Long, now: Long): Boolean =
        solvesSinceLast >= SOLVES_PER_INTERSTITIAL &&
            (now < lastShownAt || now - lastShownAt >= MIN_INTERSTITIAL_GAP_MS)

    /** Watched videos left today. Only a later date resets the count (same idea as free hints). */
    fun rewardedLeft(countDate: String, count: Int, today: String): Int =
        if (today > countDate) REWARDED_PER_DAY else (REWARDED_PER_DAY - count).coerceAtLeast(0)
}
