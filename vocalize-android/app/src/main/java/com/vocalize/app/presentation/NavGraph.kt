package com.vocalize.app.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vocalize.app.presentation.calendar.CalendarScreen
import com.vocalize.app.presentation.detail.MemoDetailScreen
import com.vocalize.app.presentation.home.HomeScreen
import com.vocalize.app.presentation.recorder.RecorderScreen
import com.vocalize.app.presentation.search.SearchScreen
import com.vocalize.app.presentation.settings.SettingsScreen
import com.vocalize.app.presentation.splash.SplashScreen
import com.vocalize.app.presentation.playlist.PlaylistScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Recorder : Screen("recorder")
    object MemoDetail : Screen("memo_detail/{memoId}") {
        fun createRoute(memoId: String) = "memo_detail/$memoId"
    }
    object Calendar : Screen("calendar")
    object Search : Screen("search")
    object Settings : Screen("settings")
    object Playlist : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: String) = "playlist/$playlistId"
    }
}

@Composable
fun NavGraph(onSplashComplete: () -> Unit) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(400, easing = EaseOutCubic)
            ) + fadeIn(tween(400))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(400, easing = EaseInCubic)
            ) + fadeOut(tween(200))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(400, easing = EaseOutCubic)
            ) + fadeIn(tween(400))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(400, easing = EaseInCubic)
            ) + fadeOut(tween(200))
        }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    onSplashComplete()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToRecorder = { navController.navigate(Screen.Recorder.route) },
                onNavigateToMemoDetail = { memoId -> navController.navigate(Screen.MemoDetail.createRoute(memoId)) },
                onNavigateToCalendar = { navController.navigate(Screen.Calendar.route) },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToPlaylist = { id -> navController.navigate(Screen.Playlist.createRoute(id)) }
            )
        }
        composable(
            route = Screen.Recorder.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                )
            }
        ) {
            RecorderScreen(
                onClose = { navController.popBackStack() },
                onSaved = { memoId ->
                    navController.popBackStack()
                    navController.navigate(Screen.MemoDetail.createRoute(memoId))
                }
            )
        }
        composable(Screen.MemoDetail.route) { backStackEntry ->
            val memoId = backStackEntry.arguments?.getString("memoId") ?: return@composable
            MemoDetailScreen(
                memoId = memoId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Calendar.route) {
            CalendarScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMemoDetail = { id -> navController.navigate(Screen.MemoDetail.createRoute(id)) },
                onNavigateToRecorder = { navController.navigate(Screen.Recorder.route) }
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMemoDetail = { id -> navController.navigate(Screen.MemoDetail.createRoute(id)) }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Playlist.route) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
            PlaylistScreen(
                playlistId = playlistId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMemoDetail = { id -> navController.navigate(Screen.MemoDetail.createRoute(id)) }
            )
        }
    }
}
