package com.blanksstudio.gridpix.data.rules

/**
 * Free-hint allowance (SPEC section 2): 3 free hints per local day, reset at local midnight,
 * then purchased hints are consumed. Pure logic over the stored values so it is unit-tested.
 */
object HintRules {
    const val FREE_PER_DAY = 3

    data class Wallet(
        /** Local date `yyyy-MM-dd` the free allowance was last reset for. */
        val freeHintsDate: String,
        val freeHintsLeft: Int,
        /** Purchased hints (and the 100 from `everything`). */
        val balance: Int,
    )

    /** Rolls the free allowance forward when [today] differs from the stored date. */
    fun refreshed(wallet: Wallet, today: String): Wallet =
        if (wallet.freeHintsDate == today) wallet else wallet.copy(freeHintsDate = today, freeHintsLeft = FREE_PER_DAY)

    fun available(wallet: Wallet, today: String): Int =
        refreshed(wallet, today).let { it.freeHintsLeft + it.balance }

    /**
     * Spends one hint: free ones first, then the purchased balance.
     * Returns null when nothing is left (the UI then offers the Shop).
     */
    fun spend(wallet: Wallet, today: String): Wallet? {
        val w = refreshed(wallet, today)
        return when {
            w.freeHintsLeft > 0 -> w.copy(freeHintsLeft = w.freeHintsLeft - 1)
            w.balance > 0 -> w.copy(balance = w.balance - 1)
            else -> null
        }
    }

    fun credit(wallet: Wallet, hints: Int): Wallet {
        require(hints >= 0)
        return wallet.copy(balance = wallet.balance + hints)
    }

    /**
     * Level-up reward (decision D27): one free hint per level reached above level 1, paid once.
     * [rewardedLevel] is the highest level already paid out (stored), so the reward can never be
     * claimed twice, and players who levelled up before this feature get their hints on the next solve.
     */
    fun levelRewards(rewardedLevel: Int, currentLevel: Int): Int =
        (currentLevel - maxOf(rewardedLevel, 1)).coerceAtLeast(0)
}

/** Daily puzzle streak (SPEC section 3). Dates are local `yyyy-MM-dd` strings. */
object StreakRules {

    data class Streak(val count: Int, val lastDailyDate: String?)

    /**
     * Called when today's daily is solved. Consecutive calendar days extend the streak,
     * a gap restarts it at 1, and solving the same day twice changes nothing.
     * [yesterday] is supplied by the caller (date arithmetic lives in the Android layer).
     */
    fun onDailySolved(streak: Streak, today: String, yesterday: String): Streak = when (streak.lastDailyDate) {
        today -> streak
        yesterday -> Streak(streak.count + 1, today)
        else -> Streak(1, today)
    }

    /** The streak shown on Home is 0 once a day has been missed. */
    fun current(streak: Streak, today: String, yesterday: String): Int =
        if (streak.lastDailyDate == today || streak.lastDailyDate == yesterday) streak.count else 0
}
