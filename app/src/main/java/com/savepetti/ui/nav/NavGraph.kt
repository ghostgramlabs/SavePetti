package com.savepetti.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.savepetti.ui.screens.categories.CategoriesScreen
import com.savepetti.ui.screens.detail.DetailScreen
import com.savepetti.ui.screens.home.HomeScreen
import com.savepetti.ui.screens.search.SearchScreen

object Routes {
    const val Home = "home"
    const val Search = "search?q={q}&src={src}"
    const val Detail = "detail/{id}"
    const val Categories = "categories?cid={cid}"

    fun search(q: String = "", src: String = "") = "search?q=$q&src=$src"
    fun detail(id: Long) = "detail/$id"
    fun categories(cid: String? = null) = "categories?cid=${cid.orEmpty()}"
}

@Composable
fun SavePettiNavGraph() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.Home) {
        composable(Routes.Home) {
            HomeScreen(
                onOpenItem = { id -> nav.navigate(Routes.detail(id)) },
                onOpenSearch = { q -> nav.navigate(Routes.search(q)) },
                onOpenSource = { src -> nav.navigate(Routes.search(src = src)) },
                onOpenCategory = { cid -> nav.navigate(Routes.categories(cid)) },
                onOpenAllCategories = { nav.navigate(Routes.categories()) }
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
                onDeleted = { nav.popBackStack() }
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
