package com.ghostgramlabs.pettibox.ui.nav

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ghostgramlabs.pettibox.data.preferences.ThemeMode
import com.ghostgramlabs.pettibox.ui.screens.categories.BrowseDestination
import com.ghostgramlabs.pettibox.ui.screens.categories.CategoriesScreen
import com.ghostgramlabs.pettibox.ui.screens.detail.DetailScreen
import com.ghostgramlabs.pettibox.ui.screens.home.HomeScreen
import com.ghostgramlabs.pettibox.ui.screens.search.SearchScreen
import com.ghostgramlabs.pettibox.ui.screens.settings.SettingsScreen

object Routes {
    const val Home = "home"
    const val Search = "search?q={q}&src={src}"
    const val Detail = "detail/{id}"
    const val Categories = "categories?cid={cid}"
    const val Settings = "settings"

    fun search(q: String = "", src: String = "") =
        "search?q=${Uri.encode(q)}&src=${Uri.encode(src)}"
    fun detail(id: Long) = "detail/$id"
    fun categories(cid: String? = null) = "categories?cid=${cid.orEmpty()}"
}

private data class TopTab(
    val route: String,
    val baseRoute: String,
    val label: String,
    val icon: ImageVector
)

private val topTabs = listOf(
    TopTab(Routes.Home, "home", "Home", Icons.Rounded.Home),
    TopTab(Routes.search(), "search", "Search", Icons.Rounded.Search),
    TopTab(Routes.categories(), "categories", "Browse", Icons.Rounded.GridView),
    TopTab(Routes.Settings, "settings", "Settings", Icons.Rounded.Settings)
)

@Composable
fun PettiBoxNavGraph(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route.orEmpty()
    val showBottomBar = topTabs.any { it.matches(currentRoute) }

    Scaffold(
        bottomBar = {
            if (showBottomBar) BottomNav(nav, currentRoute)
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.Home,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.Home) {
                HomeScreen(
                    onOpenItem = { id -> nav.navigate(Routes.detail(id)) },
                    onOpenSearch = { q -> nav.navigateTopLevel(Routes.search(q)) },
                    onOpenSource = { src -> nav.navigate(Routes.search(src = src)) },
                    onOpenCategory = { cid -> nav.navigate(Routes.categories(cid)) },
                    onOpenAllCategories = { nav.navigateTopLevel(Routes.categories()) }
                )
            }
            composable(Routes.Settings) {
                SettingsScreen(
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange
                )
            }
            composable(
                Routes.Search,
                arguments = listOf(
                    navArgument("q") { type = NavType.StringType; defaultValue = "" },
                    navArgument("src") { type = NavType.StringType; defaultValue = "" }
                )
            ) { entry ->
                val q = entry.arguments?.getString("q").orEmpty()
                val src = entry.arguments?.getString("src").orEmpty()
                SearchScreen(
                    initialQuery = q,
                    initialSource = src.ifBlank { null },
                    onOpenItem = { id -> nav.navigate(Routes.detail(id)) }
                )
            }
            composable(
                Routes.Detail,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) {
                DetailScreen(
                    onBack = { nav.popBackStack() },
                    onDeleted = { nav.popBackStack() },
                    onOpenTag = { name ->
                        nav.navigate(
                            Routes.categories(
                                BrowseDestination.toCid(BrowseDestination.Tag(name))
                            )
                        )
                    }
                )
            }
            composable(
                Routes.Categories,
                arguments = listOf(navArgument("cid") { type = NavType.StringType; defaultValue = "" })
            ) {
                CategoriesScreen(
                    onBack = { nav.popBackStack() },
                    onOpenItem = { id -> nav.navigate(Routes.detail(id)) }
                )
            }
        }
    }
}

@Composable
private fun BottomNav(nav: NavHostController, currentRoute: String) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.border(
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        )
    ) {
        topTabs.forEach { tab ->
            val selected = tab.matches(currentRoute)
            NavigationBarItem(
                selected = selected,
                onClick = { nav.navigateTopLevel(tab.route) },
                icon = { androidx.compose.material3.Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            )
        }
    }
}

private fun TopTab.matches(route: String): Boolean =
    route.startsWith(baseRoute)

/** Reset to start destination when switching top-level tabs to avoid a deep stack. */
private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
