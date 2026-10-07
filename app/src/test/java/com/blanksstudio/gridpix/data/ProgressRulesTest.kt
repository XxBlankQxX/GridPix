package com.blanksstudio.gridpix.data

import com.blanksstudio.gridpix.data.rules.Achievement
import com.blanksstudio.gridpix.data.rules.CalendarRules
import com.blanksstudio.gridpix.data.rules.ProgressRules
import com.blanksstudio.gridpix.data.rules.SolvedPuzzle
import com.blanksstudio.gridpix.data.rules.Trophy
import com.blanksstudio.gridpix.data.rules.XpRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ProgressRulesTest {

    private val today = LocalDate.of(2026, 10, 7)

    private fun endless(size: Int, seed: Int, hints: Int = 0, ms: Long = 600_000) =
        SolvedPuzzle("endless-$size-$seed", size, hints, ms)

    private fun daily(date: String, hints: Int = 0) = SolvedPuzzle("daily-$date", 10, hints, 400_000)

    private fun pack(id: String, index: Int) =
        SolvedPuzzle("pack-$id-" + index.toString().padStart(2, '0'), 10, 1, 500_000)

    @Test
    fun `xp scales with size, rewards no hints and dailies`() {
        assertEquals(15, XpRules.xpFor(endless(5, 1)))          // 10 + 50%
        assertEquals(10, XpRules.xpFor(endless(5, 1, hints = 2)))
        assertEquals(37, XpRules.xpFor(endless(10, 1)))         // 25 + 12
        assertEquals(90, XpRules.xpFor(endless(15, 1)))
        assertEquals(180, XpRules.xpFor(endless(20, 1)))
        assertEquals(57, XpRules.xpFor(daily("2026-10-07")))   // 37 + 20
    }

    @Test
    fun `level curve`() {
        assertEquals(1, XpRules.levelFor(0).level)
        assertEquals(1, XpRules.levelFor(99).level)
        val l2 = XpRules.levelFor(100)
        assertEquals(2, l2.level)
        assertEquals(0, l2.xpIntoLevel)
        assertEquals(150, l2.xpForNextLevel)
        assertEquals(3, XpRules.levelFor(250).level)
        val mid = XpRules.levelFor(325)
        assertEquals(3, mid.level)
        assertEquals(75, mid.xpIntoLevel)
        assertEquals(0.375f, mid.fraction)
    }

    @Test
    fun `streaks count consecutive daily solves`() {
        val days = setOf("2026-10-01", "2026-10-02", "2026-10-03", "2026-10-05", "2026-10-06").map(LocalDate::parse).toSet()
        assertEquals(2, CalendarRules.currentStreak(days, today))               // 5th, 6th; today not done yet
        assertEquals(3, CalendarRules.currentStreak(days + today, today))       // 5th, 6th, 7th
        assertEquals(0, CalendarRules.currentStreak(days, LocalDate.of(2026, 10, 9)))
        assertEquals(3, CalendarRules.longestStreak(days))
        assertEquals(0, CalendarRules.longestStreak(emptySet()))
    }

    @Test
    fun `monthly trophies`() {
        val oct = YearMonth.of(2026, 10)
        fun daysUpTo(n: Int) = (1..n).map { oct.atDay(it) }.toSet()
        assertEquals(Trophy.NONE, CalendarRules.trophy(oct, daysUpTo(9)))
        assertEquals(Trophy.BRONZE, CalendarRules.trophy(oct, daysUpTo(10)))
        assertEquals(Trophy.SILVER, CalendarRules.trophy(oct, daysUpTo(20)))
        assertEquals(Trophy.SILVER, CalendarRules.trophy(oct, daysUpTo(30)))
        assertEquals(Trophy.GOLD, CalendarRules.trophy(oct, daysUpTo(31)))
        assertEquals(Trophy.NONE, CalendarRules.trophy(YearMonth.of(2026, 11), daysUpTo(31)))
    }

    @Test
    fun `snapshot aggregates packs, collection and achievements`() {
        val totals = mapOf("starter" to 3, "anime" to 30)
        val solved = listOf(
            pack("starter", 1), pack("starter", 2), pack("starter", 3),
            pack("anime", 4),
            pack("removed", 1), // pack no longer shipped: ignored for the collection
            endless(15, 9, ms = 120_000),
            daily("2026-10-06"), daily("2026-10-07"),
        )
        val s = ProgressRules.snapshot(solved, totals, today)
        assertEquals(8, s.solvedCount)
        assertEquals(setOf("starter"), s.completedPacks)
        assertEquals(mapOf("starter" to 3, "anime" to 1), s.packSolved)
        assertEquals(4, s.collected)
        assertEquals(33, s.collectible)
        assertEquals(2, s.currentStreak)
        assertEquals(setOf(LocalDate.parse("2026-10-06"), today), s.dailyDays)
        assertEquals(120_000L, s.bestTimeBySize[15])

        val unlocked = s.unlocked
        assertTrue(Achievement.FIRST_SOLVE in unlocked)
        assertTrue(Achievement.PACK_COMPLETE in unlocked)
        assertTrue(Achievement.BIG_GRID in unlocked)
        assertTrue(Achievement.SPEEDY in unlocked)       // 15x15 in 2 min, no hints
        assertTrue(Achievement.DAILY_FIRST in unlocked)
        assertTrue(Achievement.PURE_LOGIC in unlocked)
        assertFalse(Achievement.SOLVES_10 in unlocked)
        assertFalse(Achievement.GIANT_GRID in unlocked)
        assertEquals(2, s.achievementProgress[Achievement.STREAK_7])
        assertEquals("anime", ProgressRules.packIdOf("pack-anime-07"))
        assertEquals(null, ProgressRules.packIdOf("endless-10-5"))
    }

    @Test
    fun `empty progress is level 1 with nothing unlocked`() {
        val s = ProgressRules.snapshot(emptyList(), mapOf("starter" to 30), today)
        assertEquals(1, s.level.level)
        assertTrue(s.unlocked.isEmpty())
        assertEquals(0, s.currentStreak)
    }
}
