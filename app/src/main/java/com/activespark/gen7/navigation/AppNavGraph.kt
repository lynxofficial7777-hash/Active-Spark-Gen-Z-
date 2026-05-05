package com.activespark.gen7.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.activespark.gen7.ui.screens.battle.BattleScreen
import com.activespark.gen7.ui.screens.challenge.ChallengeScreen
import com.activespark.gen7.ui.screens.home.HomeScreen
import com.activespark.gen7.ui.screens.leaderboard.LeaderboardScreen
import com.activespark.gen7.ui.screens.login.LoginScreen
import com.activespark.gen7.ui.screens.matchmaking.MatchmakingScreen
import com.activespark.gen7.ui.screens.onboarding.OnboardingScreen
import com.activespark.gen7.ui.screens.parentdashboard.ParentDashboardScreen
import com.activespark.gen7.ui.screens.profile.ProfileScreen
import com.activespark.gen7.ui.screens.avatar.AvatarCustomizationScreen
import com.activespark.gen7.ui.screens.solo.SoloBattleScreen
import com.activespark.gen7.ui.screens.results.ResultsScreen
import com.activespark.gen7.ui.screens.splash.SplashScreen

/**
 * Root navigation graph for Active Spark Gen 7.
 *
 * Deep linking: when a notification taps in, MainActivity passes [deepLinkMatchId]
 * and [deepLinkType] here. After the NavHost is set up we navigate to the correct
 * screen — Battle for BATTLE_INVITE, Results for BATTLE_RESULT.
 */
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    deepLinkMatchId: String? = null,
    deepLinkType: String? = null
) {
    // Handle notification deep links after nav graph initializes
    LaunchedEffect(deepLinkMatchId, deepLinkType) {
        if (!deepLinkMatchId.isNullOrEmpty()) {
            when (deepLinkType) {
                "BATTLE_INVITE" -> navController.navigate(Screen.Battle.createRoute(deepLinkMatchId))
                "BATTLE_RESULT" -> navController.navigate(Screen.Results.createRoute(deepLinkMatchId))
                else -> { /* No-op for other types */ }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(350)
            ) + fadeIn(animationSpec = tween(350))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tween(350)
            ) + fadeOut(animationSpec = tween(200))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(350)
            ) + fadeIn(animationSpec = tween(350))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(350)
            ) + fadeOut(animationSpec = tween(200))
        }
    ) {
        // ─── Auth Flow ────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(navController = navController)
        }

        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }

        // ─── Main App ─────────────────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }

        composable(
            route = Screen.Profile.route,
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            ProfileScreen(navController = navController, uid = uid)
        }

        // ─── Avatar Customization ─────────────────────────────────────
        composable(Screen.AvatarCustomization.route) {
            AvatarCustomizationScreen(navController = navController)
        }

        // ─── Challenge ────────────────────────────────────────────────
        composable(
            route = Screen.Challenge.route,
            arguments = listOf(navArgument("challengeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val challengeId = backStackEntry.arguments?.getString("challengeId") ?: ""
            ChallengeScreen(navController = navController, challengeId = challengeId)
        }

        // ─── Matchmaking ──────────────────────────────────────────────
        composable(
            route = Screen.Matchmaking.route,
            arguments = listOf(navArgument("challengeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val challengeId = backStackEntry.arguments?.getString("challengeId") ?: ""
            MatchmakingScreen(navController = navController, challengeId = challengeId)
        }

        // ─── Solo Practice ────────────────────────────────────────────
        composable(
            route = Screen.SoloBattle.route,
            arguments = listOf(navArgument("challengeId") { type = NavType.StringType }),
            enterTransition = {
                scaleIn(initialScale = 0.92f, animationSpec = tween(400)) +
                        fadeIn(animationSpec = tween(400))
            }
        ) { backStackEntry ->
            val challengeId = backStackEntry.arguments?.getString("challengeId") ?: ""
            SoloBattleScreen(navController = navController, challengeId = challengeId)
        }

        // ─── Battle ───────────────────────────────────────────────────
        composable(
            route = Screen.Battle.route,
            arguments = listOf(navArgument("matchId") { type = NavType.StringType }),
            enterTransition = {
                scaleIn(initialScale = 0.92f, animationSpec = tween(400)) +
                        fadeIn(animationSpec = tween(400))
            }
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
            BattleScreen(navController = navController, matchId = matchId)
        }

        // ─── Results ──────────────────────────────────────────────────
        composable(
            route = Screen.Results.route,
            arguments = listOf(navArgument("matchId") { type = NavType.StringType }),
            enterTransition = {
                scaleIn(initialScale = 0.85f, animationSpec = tween(500)) +
                        fadeIn(animationSpec = tween(500))
            }
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
            ResultsScreen(navController = navController, matchId = matchId)
        }

        // ─── Leaderboard ──────────────────────────────────────────────
        composable(Screen.Leaderboard.route) {
            LeaderboardScreen(navController = navController)
        }

        // ─── Parent Dashboard ─────────────────────────────────────────
        composable(
            route = Screen.ParentDashboard.route,
            arguments = listOf(navArgument("childUid") { type = NavType.StringType })
        ) { backStackEntry ->
            val childUid = backStackEntry.arguments?.getString("childUid") ?: ""
            ParentDashboardScreen(navController = navController, childUid = childUid)
        }
    }
}
