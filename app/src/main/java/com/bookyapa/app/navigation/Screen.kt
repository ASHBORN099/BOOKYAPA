package com.bookyapa.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import android.net.Uri

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    data object Library : Screen("library", "Library", Icons.Default.Book)
    data object History : Screen("history", "History", Icons.Default.History)
    data object Search : Screen("search", "Search", Icons.Default.Search)
    data object Explore : Screen("explore", "Explore", Icons.Default.Explore)
    data object Sources : Screen("sources", "Sources", Icons.Default.Settings)
    data object AddEditSource : Screen("add_edit_source/{sourceId}", "", Icons.Default.Add) {
        fun createRoute(sourceId: Long = -1L) = "add_edit_source/$sourceId"
    }

    data object BookDetail : Screen("book_detail/{bookId}?sourceId={sourceId}&bookUrl={bookUrl}", "", Icons.Default.Book) {
        fun createRoute(bookId: Long) = "book_detail/$bookId"
        fun createRoute(sourceId: Long, bookUrl: String) = "book_detail/-1?sourceId=$sourceId&bookUrl=${Uri.encode(bookUrl)}"
    }

    data object Reader : Screen("reader/{bookId}/{chapterIndex}", "", Icons.Default.Book) {
        fun createRoute(bookId: Long, chapterIndex: Int) = "reader/$bookId/$chapterIndex"
    }

    data object Catalog : Screen("catalog", "", Icons.Default.Add) {
        fun createRoute() = "catalog"
    }

    data object RepoManager : Screen("repo_manager", "", Icons.Default.Add) {
        fun createRoute() = "repo_manager"
    }

    data object Bookmarks : Screen("bookmarks", "", Icons.Default.Bookmark) {
        fun createRoute() = "bookmarks"
    }

    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    companion object {
        val bottomNavItems = listOf(Library, History, Explore, Settings)
    }
}
