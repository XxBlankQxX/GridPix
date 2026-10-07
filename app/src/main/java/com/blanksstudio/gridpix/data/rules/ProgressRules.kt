package com.blanksstudio.gridpix.data.rules

import java.time.LocalDate
import java.time.YearMonth

/**
 * Player progression derived entirely from solved rows in `puzzle_progress` (no extra storage, so it
 * can never drift out of sync and needs no schema change). Pure JVM code, unit-tested in ProgressRulesTest.
 */

/** One solved puzzle as stored in Room. [puzzleId] follows SPEC section 5 (`endless-10-123`, `daily-2026-10-07`, `pack-anime-07`). */
data class SolvedPuzzle(
    val puzzleId: String,
    val size: Int,
    val hintsUsed: Int,
    val elapsedMs: Long,
)

data class LevelInfo(
    val level: Int,
    val totalXp: Int,
    /** XP earned inside the current level. */
    val xpIntoLevel: Int,
    /** XP the current level needs in total before the next one. */
    val xpForNextLevel: Int,
) {
    val fraction: Float get() = if (xpForNextLevel == 0) 0f else xpIntoLevel.toFloat() / xpForNextLevel
}

object XpRules {
    /** Base XP by grid size; bigger grids are worth disproportionately more. */
    fun baseXp(size: Int): Int = when {
        size <= 5 -> 10
        size <= 10 -> 25
        size <= 15 -> 60
        else -> 120
    }

    /** XP for one solve: base, +50% when no hints were used, +20 for the daily puzzle. */
    fun xpFor(puzzle: SolvedPuzzle): Int {
        var xp = baseXp(puzzle.size)
        if (puzzle.hintsUsed == 0) xp += xp / 2
        if (puzzle.puzzleId.startsWith("daily-")) xp += 20
        return xp
    }

    /** Level n needs 100 + 50*(n-1) XP to reach level n+1: 100, 150, 200, ... Level 1 starts at 0 XP. */
    fun xpToAdvance(level: Int): Int = 100 + 50 * (level - 1)

    fun levelFor(totalXp: Int): LevelInfo {
        var level = 1
        var remaining = totalXp
        while (remaining >= xpToAdvance(level)) {
            remaining -= xpToAdvance(level)
            level++
        }
        return LevelInfo(level, totalXp, remaining, xpToAdvance(level))
    }
}

enum class Trophy { NONE, BRONZE, SILVER, GOLD }

object CalendarRules {
    fun dailyDate(puzzleId: String): LocalDate? =
        puzzleId.takeIf { it.startsWith("daily-") }?.removePrefix("daily-")?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    /** Consecutive days solved ending today, or ending yesterday if today is not solved yet (a streak is still alive until midnight). */
    fun currentStreak(days: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in days) today else today.minusDays(1)
        var count = 0
        while (day in days) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    fun longestStreak(days: Set<LocalDate>): Int {
        var best = 0
        for (day in days) {
            if (day.minusDays(1) in days) continue // only count from the start of each run
            var length = 0
            var d = day
            while (d in days) {
                length++
                d = d.plusDays(1)
            }
            best = maxOf(best, length)
        }
        return best
    }

    /** Monthly trophy for daily puzzles: bronze 10+ days, silver 20+, gold every day of the month. */
    fun trophy(month: YearMonth, days: Set<LocalDate>): Trophy {
        val solved = days.count { YearMonth.from(it) == month }
        return when {
            solved >= month.lengthOfMonth() -> Trophy.GOLD
            solved >= 20 -> Trophy.SILVER
            solved >= 10 -> Trophy.BRONZE
            else -> Trophy.NONE
        }
    }
}

/** Achievement ids are stable (they are not stored, but the UI maps each to strings and an icon). */
enum class Achievement(val target: Int) {
    FIRST_SOLVE(1),
    SOLVES_10(10),
    SOLVES_50(50),
    SOLVES_100(100),
    SOLVES_250(250),
    PURE_LOGIC(1),
    PURE_LOGIC_25(25),
    BIG_GRID(1),
    GIANT_GRID(1),
    SPEEDY(1),
    DAILY_FIRST(1),
    STREAK_7(7),
    STREAK_30(30),
    PACK_COMPLETE(1),
    COLLECTOR_50(50),
    COLLECTOR_ALL(1),
    LEVEL_10(10),
    LEVEL_25(25),
}

/** Everything the Home, Progress and Collection screens show, computed in one pass. */
data class ProgressSnapshot(
    val level: LevelInfo,
    val solvedCount: Int,
    val solvedBySize: Map<Int, Int>,
    val bestTimeBySize: Map<Int, Long>,
    val noHintSolves: Int,
    val dailyDays: Set<LocalDate>,
    val currentStreak: Int,
    val longestStreak: Int,
    /** Solved puzzle count per pack id. */
    val packSolved: Map<String, Int>,
    val completedPacks: Set<String>,
    val collected: Int,
    val collectible: Int,
    /** Progress toward each achievement, capped at its target. */
    val achievementProgress: Map<Achievement, Int>,
) {
    val unlocked: Set<Achievement> get() = achievementProgress.filter { (a, p) -> p >= a.target }.keys

    companion object {
        val EMPTY = ProgressRules.snapshot(emptyList(), emptyMap(), LocalDate.of(2026, 1, 1))
    }
}

object ProgressRules {

    /** 10x10 under 3 minutes counts as "Speedy". */
    const val SPEEDY_MS = 3 * 60 * 1000L

    fun packIdOf(puzzleId: String): String? =
        puzzleId.takeIf { it.startsWith("pack-") }?.removePrefix("pack-")?.substringBeforeLast('-')

    /**
     * @param packTotals puzzle count per pack id (from the pack JSON files).
     */
    fun snapshot(solved: List<SolvedPuzzle>, packTotals: Map<String, Int>, today: LocalDate): ProgressSnapshot {
        val totalXp = solved.sumOf { XpRules.xpFor(it) }
        val level = XpRules.levelFor(totalXp)
        val bySize = solved.groupingBy { it.size }.eachCount()
        val bestBySize = solved.filter { it.elapsedMs > 0 }.groupBy { it.size }.mapValues { (_, v) -> v.minOf { it.elapsedMs } }
        val noHints = solved.count { it.hintsUsed == 0 }
        val days = solved.mapNotNull { CalendarRules.dailyDate(it.puzzleId) }.toSet()
        val current = CalendarRules.currentStreak(days, today)
        val longest = CalendarRules.longestStreak(days)
        val packSolved = solved.mapNotNull { packIdOf(it.puzzleId) }.filter { it in packTotals }.groupingBy { it }.eachCount()
        val completed = packTotals.filter { (id, total) -> total > 0 && (packSolved[id] ?: 0) >= total }.keys
        val collected = packSolved.values.sum()
        val collectible = packTotals.values.sum()
        val speedy = solved.any { it.size >= 10 && it.elapsedMs in 1 until SPEEDY_MS && it.hintsUsed == 0 }

        fun cap(a: Achievement, value: Int) = a to value.coerceAtMost(a.target)
        val progress = mapOf(
            cap(Achievement.FIRST_SOLVE, solved.size),
            cap(Achievement.SOLVES_10, solved.size),
            cap(Achievement.SOLVES_50, solved.size),
            cap(Achievement.SOLVES_100, solved.size),
            cap(Achievement.SOLVES_250, solved.size),
            cap(Achievement.PURE_LOGIC, noHints),
            cap(Achievement.PURE_LOGIC_25, noHints),
            cap(Achievement.BIG_GRID, if (solved.any { it.size >= 15 }) 1 else 0),
            cap(Achievement.GIANT_GRID, if (solved.any { it.size >= 20 }) 1 else 0),
            cap(Achievement.SPEEDY, if (speedy) 1 else 0),
            cap(Achievement.DAILY_FIRST, days.size),
            cap(Achievement.STREAK_7, longest),
            cap(Achievement.STREAK_30, longest),
            cap(Achievement.PACK_COMPLETE, completed.size),
            cap(Achievement.COLLECTOR_50, collected),
            cap(Achievement.COLLECTOR_ALL, if (collectible > 0 && collected >= collectible) 1 else 0),
            cap(Achievement.LEVEL_10, level.level),
            cap(Achievement.LEVEL_25, level.level),
        )
        return ProgressSnapshot(
            level = level,
            solvedCount = solved.size,
            solvedBySize = bySize,
            bestTimeBySize = bestBySize,
            noHintSolves = noHints,
            dailyDays = days,
            currentStreak = current,
            longestStreak = longest,
            packSolved = packSolved,
            completedPacks = completed,
            collected = collected,
            collectible = collectible,
            achievementProgress = progress,
        )
    }
}
