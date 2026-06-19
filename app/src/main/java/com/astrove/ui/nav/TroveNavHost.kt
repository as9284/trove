package com.astrove.ui.nav

import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.astrove.TroveApp
import com.astrove.data.ShotCategory
import com.astrove.ui.cleanup.CleanupRoute
import com.astrove.ui.detail.DetailRoute
import com.astrove.ui.gallery.GalleryRoute
import com.astrove.ui.home.HomeRoute
import com.astrove.ui.search.SearchRoute
import com.astrove.ui.settings.SettingsRoute
import com.astrove.ui.theme.TroveMotion

object Routes {
    const val HOME = "home"
    const val SEARCH = "search?q={q}"
    const val GALLERY = "gallery?category={category}"
    const val CLEANUP = "cleanup"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{id}"

    fun search(query: String = "") = "search?q=${Uri.encode(query)}"
    fun gallery(category: ShotCategory? = null) = "gallery?category=${category?.name ?: ""}"
    fun detail(id: Long) = "detail/$id"
}

private data class Tab(val route: String, val base: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.HOME, "home", "Home", Icons.Outlined.Home),
    Tab(Routes.search(), "search", "Search", Icons.Outlined.Search),
    Tab(Routes.CLEANUP, "cleanup", "Clean up", Icons.Outlined.AutoAwesome),
    Tab(Routes.SETTINGS, "settings", "Settings", Icons.Outlined.Settings),
)

/** Kicks off indexing on entry, then hosts the screen graph. */
@Composable
fun TroveAppContent() {
    val container = (LocalContext.current.applicationContext as TroveApp).container
    LaunchedEffect(Unit) { container.repository.requestIndexing() }
    TroveNavHost()
}

@Composable
fun TroveNavHost() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentBase = backStack?.destination?.route?.substringBefore("?")?.substringBefore("/")
    val onTab = TABS.any { it.base == currentBase }

    Scaffold(
        bottomBar = {
            if (onTab) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = tab.base == currentBase,
                            onClick = { nav.navigateToTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(inner),
            enterTransition = { TroveMotion.ScreenEnter },
            exitTransition = { TroveMotion.ScreenExit },
            popEnterTransition = { TroveMotion.ScreenPopEnter },
            popExitTransition = { TroveMotion.ScreenPopExit },
        ) {
            composable(Routes.HOME) {
                HomeRoute(
                    onOpenSearch = { nav.navigateToTab(Routes.search()) },
                    onSearchQuery = { q -> nav.navigate(Routes.search(q)) },
                    onOpenShot = { id -> nav.navigate(Routes.detail(id)) },
                    onOpenGallery = { nav.navigate(Routes.gallery()) },
                    onOpenCleanup = { nav.navigateToTab(Routes.CLEANUP) },
                )
            }
            composable(
                route = Routes.SEARCH,
                arguments = listOf(navArgument("q") { type = NavType.StringType; defaultValue = "" }),
            ) { entry ->
                SearchRoute(
                    initialQuery = entry.arguments?.getString("q").orEmpty(),
                    onOpenShot = { id -> nav.navigate(Routes.detail(id)) },
                )
            }
            composable(
                route = Routes.GALLERY,
                arguments = listOf(navArgument("category") { type = NavType.StringType; defaultValue = "" }),
            ) { entry ->
                val category = entry.arguments?.getString("category")
                    ?.takeIf { it.isNotBlank() }
                    ?.let { runCatching { ShotCategory.valueOf(it) }.getOrNull() }
                GalleryRoute(
                    category = category,
                    onBack = { nav.popBackStack() },
                    onOpenShot = { id -> nav.navigate(Routes.detail(id)) },
                    onOpenCategory = { cat -> nav.navigate(Routes.gallery(cat)) },
                )
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
                enterTransition = { TroveMotion.DetailEnter },
                popExitTransition = { TroveMotion.DetailPopExit },
            ) { entry ->
                DetailRoute(
                    id = entry.arguments?.getLong("id") ?: return@composable,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.CLEANUP) {
                CleanupRoute(onOpenShot = { id -> nav.navigate(Routes.detail(id)) })
            }
            composable(Routes.SETTINGS) {
                SettingsRoute()
            }
        }
    }
}

private fun NavController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
