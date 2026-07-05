package com.bookyapa.app.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.bookyapa.app.ui.theme.AppColors
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBar = Screen.bottomNavItems.any { it.route == currentRoute }

    AnimatedVisibility(
        visible = showBar,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
    ) {
        NavigationBar(
            containerColor = Color(0xFF1A1A1A),
            contentColor = Color(0xFFE0E0E0),
        ) {
            Screen.bottomNavItems.forEach { screen ->
                val selected = currentRoute == screen.route
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.title,
                        )
                    },
                    label = { Text(screen.title) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF008080),
                        selectedTextColor = Color(0xFF008080),
                        unselectedIconColor = AppColors.bodyText,
                        unselectedTextColor = AppColors.bodyText,
                        indicatorColor = Color(0xFF2D2D2D),
                    ),
                )
            }
        }
    }
}
