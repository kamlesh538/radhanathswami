package com.radhanathswami.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.radhanathswami.app.data.model.BrowseItem
import com.radhanathswami.app.ui.components.MiniPlayer
import com.radhanathswami.app.ui.player.PlayerController
import com.radhanathswami.app.ui.screens.categories.CategoryScreen
import com.radhanathswami.app.ui.screens.downloads.DownloadsScreen
import com.radhanathswami.app.ui.screens.home.HomeScreen
import com.radhanathswami.app.ui.screens.player.PlayerScreen
import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Downloads : Screen("downloads")
    object Category : Screen("category/{path}/{name}") {
        fun createRoute(path: String, name: String): String {
            val encodedPath = URLEncoder.encode(path, "UTF-8")
            val encodedName = URLEncoder.encode(name, "UTF-8")
            return "category/$encodedPath/$encodedName"
        }
    }
    object Player : Screen("player")
}

@Composable
fun AppNavigation(playerController: PlayerController) {
    val navController = rememberNavController()
    val playerState by playerController.playerState.collectAsState()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val isPlayerScreen = currentRoute == Screen.Player.route

    Scaffold(
        bottomBar = {
            Column {
                if (playerState.currentAudio != null && !isPlayerScreen) {
                    MiniPlayer(
                        playerController = playerController,
                        onExpand = { navController.navigate(Screen.Player.route) }
                    )
                }
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Home.route,
                        onClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Downloads.route,
                        onClick = {
                            navController.navigate(Screen.Downloads.route) {
                                popUpTo(Screen.Home.route)
                            }
                        },
                        icon = { Icon(Icons.Default.Download, contentDescription = "Downloads") },
                        label = { Text("Downloads") }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onCategoryClick = { folder ->
                        navController.navigate(Screen.Category.createRoute(folder.path, folder.name))
                    }
                )
            }

            composable(Screen.Downloads.route) {
                DownloadsScreen(playerController = playerController)
            }

            composable(
                route = Screen.Category.route,
                arguments = listOf(
                    navArgument("path") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val encodedPath = backStackEntry.arguments?.getString("path") ?: ""
                val encodedName = backStackEntry.arguments?.getString("name") ?: ""
                val path = URLDecoder.decode(encodedPath, "UTF-8")
                val name = URLDecoder.decode(encodedName, "UTF-8")

                CategoryScreen(
                    folderPath = path,
                    folderName = name,
                    onNavigateBack = { navController.popBackStack() },
                    onFolderClick = { folder ->
                        navController.navigate(Screen.Category.createRoute(folder.path, folder.name))
                    },
                    playerController = playerController
                )
            }

            composable(Screen.Player.route) {
                PlayerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    playerController = playerController
                )
            }
        }
    }
}
