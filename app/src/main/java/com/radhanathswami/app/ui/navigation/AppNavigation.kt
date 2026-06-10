package com.radhanathswami.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.radhanathswami.app.data.model.AudioItem
import com.radhanathswami.app.ui.components.MiniPlayer
import com.radhanathswami.app.ui.player.PlayerController
import com.radhanathswami.app.ui.screens.categories.CategoryScreen
import com.radhanathswami.app.ui.screens.downloads.DownloadsScreen
import com.radhanathswami.app.ui.screens.history.HistoryScreen
import com.radhanathswami.app.ui.screens.home.HomeScreen
import com.radhanathswami.app.ui.screens.player.PlayerScreen
import com.radhanathswami.app.ui.screens.playlists.PlaylistDetailScreen
import com.radhanathswami.app.ui.screens.playlists.PlaylistsScreen
import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Downloads : Screen("downloads")
    object History : Screen("history")
    object Playlists : Screen("playlists")
    object Category : Screen("category/{path}/{name}") {
        fun createRoute(path: String, name: String): String {
            val encodedPath = URLEncoder.encode(path, "UTF-8")
            val encodedName = URLEncoder.encode(name, "UTF-8")
            return "category/$encodedPath/$encodedName"
        }
    }
    object Player : Screen("player")
    object PlaylistDetail : Screen("playlist/{playlistId}/{playlistName}") {
        fun createRoute(id: String, name: String): String {
            val encodedId = URLEncoder.encode(id, "UTF-8")
            val encodedName = URLEncoder.encode(name, "UTF-8")
            return "playlist/$encodedId/$encodedName"
        }
    }
}

@Composable
fun AppNavigation(playerController: PlayerController) {
    val navController = rememberNavController()
    val playerState by playerController.playerState.collectAsState()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val isPlayerScreen = currentRoute == Screen.Player.route

    val onOpenFolder: (() -> Unit)? = run {
        val playlistId = playerState.currentPlaylistId
        val playlistName = playerState.currentPlaylistName
        if (playlistId != null && playlistName != null) {
            { navController.navigate(Screen.PlaylistDetail.createRoute(playlistId, playlistName)) }
        } else {
            playerState.currentAudio
                ?.let { audio -> audioFolderRoute(audio) }
                ?.let { (path, name) -> { navController.navigate(Screen.Category.createRoute(path, name)) } }
        }
    }

    Scaffold(
        bottomBar = {
            Column {
                if (playerState.currentAudio != null && !isPlayerScreen) {
                    MiniPlayer(
                        playerController = playerController,
                        onExpand = { navController.navigate(Screen.Player.route) },
                        onOpenFolder = onOpenFolder
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
                    NavigationBarItem(
                        selected = currentRoute == Screen.History.route,
                        onClick = {
                            navController.navigate(Screen.History.route) {
                                popUpTo(Screen.Home.route)
                            }
                        },
                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                        label = { Text("History") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Playlists.route ||
                                currentRoute == Screen.PlaylistDetail.route,
                        onClick = {
                            navController.navigate(Screen.Playlists.route) {
                                popUpTo(Screen.Home.route)
                            }
                        },
                        icon = { Icon(Icons.Default.QueueMusic, contentDescription = "Playlists") },
                        label = { Text("Playlists") }
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

            composable(Screen.History.route) {
                HistoryScreen(playerController = playerController)
            }

            composable(Screen.Playlists.route) {
                PlaylistsScreen(
                    onPlaylistClick = { playlist ->
                        navController.navigate(Screen.PlaylistDetail.createRoute(playlist.id, playlist.name))
                    }
                )
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
                    playerController = playerController,
                    onOpenFolder = onOpenFolder
                )
            }

            composable(
                route = Screen.PlaylistDetail.route,
                arguments = listOf(
                    navArgument("playlistId") { type = NavType.StringType },
                    navArgument("playlistName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val playlistId = URLDecoder.decode(
                    backStackEntry.arguments?.getString("playlistId") ?: "", "UTF-8"
                )
                val playlistName = URLDecoder.decode(
                    backStackEntry.arguments?.getString("playlistName") ?: "", "UTF-8"
                )
                PlaylistDetailScreen(
                    playlistId = playlistId,
                    playlistName = playlistName,
                    onNavigateBack = { navController.popBackStack() },
                    onAddLecture = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    playerController = playerController
                )
            }
        }
    }
}

private const val AUDIO_BASE_URL = "https://audio.iskcondesiretree.com"

private fun audioFolderRoute(audio: AudioItem): Pair<String, String>? {
    val folderName = audio.category.ifBlank { return null }
    val folderPath = if (audio.url.startsWith(AUDIO_BASE_URL)) {
        // Keep the leading slash — browseDirectory expects paths like /02_-_.../2022
        audio.url.removePrefix(AUDIO_BASE_URL).substringBeforeLast("/")
    } else {
        folderName
    }
    return if (folderPath.isBlank()) null else Pair(folderPath, folderName.replace("_", " "))
}
