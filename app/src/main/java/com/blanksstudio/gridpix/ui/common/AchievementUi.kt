package com.blanksstudio.gridpix.ui.common

import androidx.annotation.StringRes
import com.blanksstudio.gridpix.R
import com.blanksstudio.gridpix.data.rules.Achievement
import com.blanksstudio.gridpix.data.rules.Trophy

/** Badge emoji plus title/description strings for each achievement. Emoji are symbols, not text, so they live here. */
data class AchievementUi(val emoji: String, @StringRes val title: Int, @StringRes val description: Int)

val Achievement.ui: AchievementUi
    get() = when (this) {
        Achievement.FIRST_SOLVE -> AchievementUi("🧩", R.string.ach_first_solve, R.string.ach_first_solve_desc)
        Achievement.SOLVES_10 -> AchievementUi("🎯", R.string.ach_solves_10, R.string.ach_solves_10_desc)
        Achievement.SOLVES_50 -> AchievementUi("🏅", R.string.ach_solves_50, R.string.ach_solves_50_desc)
        Achievement.SOLVES_100 -> AchievementUi("💯", R.string.ach_solves_100, R.string.ach_solves_100_desc)
        Achievement.SOLVES_250 -> AchievementUi("🏆", R.string.ach_solves_250, R.string.ach_solves_250_desc)
        Achievement.PURE_LOGIC -> AchievementUi("🧠", R.string.ach_pure_logic, R.string.ach_pure_logic_desc)
        Achievement.PURE_LOGIC_25 -> AchievementUi("🎓", R.string.ach_pure_logic_25, R.string.ach_pure_logic_25_desc)
        Achievement.BIG_GRID -> AchievementUi("🔍", R.string.ach_big_grid, R.string.ach_big_grid_desc)
        Achievement.GIANT_GRID -> AchievementUi("🗺️", R.string.ach_giant_grid, R.string.ach_giant_grid_desc)
        Achievement.SPEEDY -> AchievementUi("⚡", R.string.ach_speedy, R.string.ach_speedy_desc)
        Achievement.DAILY_FIRST -> AchievementUi("📅", R.string.ach_daily_first, R.string.ach_daily_first_desc)
        Achievement.STREAK_7 -> AchievementUi("🔥", R.string.ach_streak_7, R.string.ach_streak_7_desc)
        Achievement.STREAK_30 -> AchievementUi("🌋", R.string.ach_streak_30, R.string.ach_streak_30_desc)
        Achievement.PACK_COMPLETE -> AchievementUi("📦", R.string.ach_pack_complete, R.string.ach_pack_complete_desc)
        Achievement.COLLECTOR_50 -> AchievementUi("🖼️", R.string.ach_collector_50, R.string.ach_collector_50_desc)
        Achievement.COLLECTOR_ALL -> AchievementUi("👑", R.string.ach_collector_all, R.string.ach_collector_all_desc)
        Achievement.LEVEL_10 -> AchievementUi("⭐", R.string.ach_level_10, R.string.ach_level_10_desc)
        Achievement.LEVEL_25 -> AchievementUi("🌟", R.string.ach_level_25, R.string.ach_level_25_desc)
    }

val Trophy.emoji: String
    get() = when (this) {
        Trophy.NONE -> ""
        Trophy.BRONZE -> "🥉"
        Trophy.SILVER -> "🥈"
        Trophy.GOLD -> "🥇"
    }
