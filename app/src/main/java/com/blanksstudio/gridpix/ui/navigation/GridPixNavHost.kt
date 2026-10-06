package com.blanksstudio.gridpix.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.blanksstudio.gridpix.ui.home.HomeScreen
import com.blanksstudio.gridpix.ui.packs.PackPuzzlesScreen
import com.blanksstudio.gridpix.ui.packs.PacksScreen
import com.blanksstudio.gridpix.ui.puzzle.PuzzleScreen
import com.blanksstudio.gridpix.ui.settings.SettingsScreen
import com.blanksstudio.gridpix.ui.shop.ShopScreen
import com.blanksstudio.gridpix.ui.stats.StatsScreen

@Composable
fun GridPixNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onPlay = { size, seed -> navController.navigate(Routes.endless(size, seed)) },
                onDaily = { date -> navController.navigate(Routes.daily(date)) },
                onTutorial = { navController.navigate(Routes.tutorial(1)) },
                onPacks = { navController.navigate(Routes.PACKS) },
                onStats = { navController.navigate(Routes.STATS) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onShop = { navController.navigate(Routes.SHOP) },
            )
        }
        composable(Routes.PACKS) {
            PacksScreen(
                onBack = { navController.popBackStack() },
                onOpenPack = { packId -> navController.navigate(Routes.pack(packId)) },
                onShop = { navController.navigate(Routes.SHOP) },
            )
        }
        composable(
            Routes.PACK,
            arguments = listOf(navArgument(Routes.ARG_PACK_ID) { type = NavType.StringType }),
        ) {
            PackPuzzlesScreen(
                onBack = { navController.popBackStack() },
                onOpenPuzzle = { packId, index -> navController.navigate(Routes.packPuzzle(packId, index)) },
                onShop = { navController.navigate(Routes.SHOP) },
            )
        }
        composable(Routes.SHOP) { ShopScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onReplayTutorial = { navController.navigate(Routes.tutorial(1)) },
            )
        }
        composable(Routes.STATS) { StatsScreen(onBack = { navController.popBackStack() }) }
        composable(
            Routes.PUZZLE,
            arguments = listOf(
                navArgument(Routes.ARG_KIND) { type = NavType.StringType },
                navArgument(Routes.ARG_A) { type = NavType.StringType },
                navArgument(Routes.ARG_B) { type = NavType.StringType },
            ),
        ) {
            PuzzleScreen(
                onBack = { navController.popBackStack() },
                onHome = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onShop = { navController.navigate(Routes.SHOP) },
                onOpenPuzzle = { route ->
                    navController.navigate(route) {
                        popUpTo(Routes.PUZZLE) { inclusive = true }
                    }
                },
            )
        }
    }
}
