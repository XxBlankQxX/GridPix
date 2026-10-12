package com.blanksstudio.gridpix.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.blanksstudio.gridpix.R
import com.blanksstudio.gridpix.ui.collection.CollectionScreen
import com.blanksstudio.gridpix.ui.home.HomeScreen
import com.blanksstudio.gridpix.ui.packs.PackPuzzlesScreen
import com.blanksstudio.gridpix.ui.packs.PacksScreen
import com.blanksstudio.gridpix.ui.puzzle.PuzzleScreen
import com.blanksstudio.gridpix.ui.settings.SettingsScreen
import com.blanksstudio.gridpix.ui.shop.ShopScreen
import com.blanksstudio.gridpix.ui.stats.StatsScreen

/** The four big tabs at the bottom (decision D28: child-friendly navigation). Emoji icons read well for kids. */
private data class Tab(val route: String, val emoji: String, val label: Int)

private val TABS = listOf(
    Tab(Routes.HOME, "🏠", R.string.nav_home),
    Tab(Routes.PACKS, "🧩", R.string.nav_packs),
    Tab(Routes.COLLECTION, "🖼️", R.string.home_collection),
    Tab(Routes.STATS, "🏆", R.string.home_progress),
)

/**
 * Navigates only when the screen asking is the one on top and fully shown. A fast double tap (or a
 * tap during a transition) therefore can't open the same screen twice.
 */
private fun NavHostController.go(from: NavBackStackEntry, route: String) {
    if (from.lifecycle.currentState == Lifecycle.State.RESUMED) navigate(route)
}

@Composable
fun GridPixNavHost(navController: NavHostController = rememberNavController()) {
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val showTabs = TABS.any { it.route == currentRoute }

    // Tab switch. No saveState/restoreState: together with very fast taps during a transition that
    // combination crashes Navigation ("access the NavBackStackEntry's ViewModels after it is destroyed",
    // found by the monkey stress test). Tabs are simple pages, so nothing is lost. Taps are ignored
    // until the current screen is fully shown.
    fun openTab(route: String) {
        val current = navController.currentBackStackEntry ?: return
        if (current.lifecycle.currentState != Lifecycle.State.RESUMED) return
        if (current.destination.route == route) return
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id)
            launchSingleTop = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showTabs) {
                NavigationBar {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { if (currentRoute != tab.route) openTab(tab.route) },
                            icon = { Text(tab.emoji, style = androidx.compose.material3.MaterialTheme.typography.titleLarge) },
                            label = { Text(stringResource(tab.label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            // Instant screen changes: snappier for kids, and no half-animated screens to tap on.
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
            modifier = Modifier
                .padding(padding)
                .then(if (showTabs) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier),
        ) {
            composable(Routes.HOME) { entry ->
                HomeScreen(
                    onPlay = { size, seed -> navController.go(entry, Routes.endless(size, seed)) },
                    onDaily = { date -> navController.go(entry, Routes.daily(date)) },
                    onTutorial = { navController.go(entry, Routes.tutorial(1)) },
                    onPacks = { openTab(Routes.PACKS) },
                    onOpenPack = { packId -> navController.go(entry, Routes.pack(packId)) },
                    onProgress = { openTab(Routes.STATS) },
                    onSettings = { navController.go(entry, Routes.SETTINGS) },
                    onShop = { navController.go(entry, Routes.SHOP) },
                )
            }
            composable(Routes.PACKS) { entry ->
                PacksScreen(
                    onBack = null,
                    onOpenPack = { packId -> navController.go(entry, Routes.pack(packId)) },
                    onShop = { navController.go(entry, Routes.SHOP) },
                )
            }
            composable(
                Routes.PACK,
                arguments = listOf(navArgument(Routes.ARG_PACK_ID) { type = NavType.StringType }),
            ) { entry ->
                PackPuzzlesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPuzzle = { packId, index -> navController.go(entry, Routes.packPuzzle(packId, index)) },
                    onShop = { navController.go(entry, Routes.SHOP) },
                )
            }
            composable(Routes.SHOP) { ShopScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.SETTINGS) { entry ->
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onReplayTutorial = { navController.go(entry, Routes.tutorial(1)) },
                )
            }
            composable(Routes.STATS) { StatsScreen(onBack = null) }
            composable(Routes.COLLECTION) { entry ->
                CollectionScreen(
                    onBack = null,
                    onOpenPuzzle = { packId, index -> navController.go(entry, Routes.packPuzzle(packId, index)) },
                    onShop = { navController.go(entry, Routes.SHOP) },
                )
            }
            composable(
                Routes.PUZZLE,
                arguments = listOf(
                    navArgument(Routes.ARG_KIND) { type = NavType.StringType },
                    navArgument(Routes.ARG_A) { type = NavType.StringType },
                    navArgument(Routes.ARG_B) { type = NavType.StringType },
                ),
            ) { entry ->
                PuzzleScreen(
                    onBack = { navController.popBackStack() },
                    onHome = { navController.popBackStack(Routes.HOME, inclusive = false) },
                    onShop = { navController.go(entry, Routes.SHOP) },
                    onOpenPuzzle = { route ->
                        if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                            navController.navigate(route) {
                                popUpTo(Routes.PUZZLE) { inclusive = true }
                            }
                        }
                    },
                )
            }
        }
    }
}
