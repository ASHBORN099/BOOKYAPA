package com.bookyapa.app.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.bookyapa.app.ui.explore.ExploreContainerScreen
import com.bookyapa.app.ui.history.HistoryScreen
import com.bookyapa.app.ui.library.LibraryContainerScreen
import com.bookyapa.app.ui.catalog.CatalogScreen
import com.bookyapa.app.ui.catalog.RepoManagerScreen
import com.bookyapa.app.ui.screens.AddEditSourceScreen
import com.bookyapa.app.reader.ReaderScreen
import com.bookyapa.app.ui.screens.BookDetailScreen
import com.bookyapa.app.ui.settings.SettingsScreen
import com.bookyapa.app.ui.bookmarks.BookmarksScreen

@Composable
fun BookyapaNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = Screen.bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.only(
            WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
        ),
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Library.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Library.route) {
                LibraryContainerScreen(navController = navController)
            }
            composable(Screen.History.route) {
                HistoryScreen(navController = navController)
            }
            composable(Screen.Explore.route) {
                ExploreContainerScreen(navController = navController)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }
            composable(
                route = Screen.AddEditSource.route,
                arguments = listOf(
                    navArgument("sourceId") { type = NavType.LongType; defaultValue = -1L },
                ),
            ) {
                AddEditSourceScreen(navController = navController)
            }
            composable(
                route = Screen.BookDetail.route,
                arguments = listOf(
                    navArgument("bookId") { type = NavType.LongType },
                    navArgument("sourceId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("bookUrl") { type = NavType.StringType; defaultValue = "" },
                ),
            ) {
                BookDetailScreen(navController = navController)
            }
            composable(
                route = Screen.Reader.route,
                arguments = listOf(
                    navArgument("bookId") { type = NavType.LongType },
                    navArgument("chapterIndex") { type = NavType.IntType },
                ),
            ) {
                ReaderScreen(navController = navController)
            }
            composable(Screen.Catalog.route) {
                val catalogViewModel: com.bookyapa.app.ui.catalog.CatalogViewModel = hiltViewModel()
                CatalogScreen(viewModel = catalogViewModel)
            }
            composable(Screen.RepoManager.route) {
                RepoManagerScreen(navController = navController)
            }
            composable(Screen.Bookmarks.route) {
                BookmarksScreen(navController = navController)
            }
        }
    }
}
