package com.activespark.gen7.navigation

/**
 * Defines all navigation routes in the Active Spark Gen 7 app.
 * Each screen has a unique string route and optional argument templates.
 */
sealed class Screen(val route: String) {

    // ─── Auth Flow ───────────────────────────────────────────────────────
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    // Note: Register is handled as a tab inside LoginScreen — no separate route needed

    // ─── Main App Flow ───────────────────────────────────────────────────
    object Home : Screen("home")
    object Profile : Screen("profile/{uid}") {
        fun createRoute(uid: String) = "profile/$uid"
    }

    // ─── Avatar Customization ────────────────────────────────────────────
    object AvatarCustomization : Screen("avatar_customization")

    // ─── Challenge & Matchmaking ─────────────────────────────────────────
    object Challenge : Screen("challenge/{challengeId}") {
        fun createRoute(challengeId: String) = "challenge/$challengeId"
    }
    object Matchmaking : Screen("matchmaking/{challengeId}") {
        fun createRoute(challengeId: String) = "matchmaking/$challengeId"
    }

    // ─── Battle ─────────────────────────────────────────────────────────
    object Battle : Screen("battle/{matchId}") {
        fun createRoute(matchId: String) = "battle/$matchId"
    }

    // ─── Post-Battle ─────────────────────────────────────────────────────
    object Results : Screen("results/{matchId}") {
        fun createRoute(matchId: String) = "results/$matchId"
    }

    // ─── Leaderboard ─────────────────────────────────────────────────────
    object Leaderboard : Screen("leaderboard")

    // ─── Parental Dashboard ───────────────────────────────────────────────
    object ParentDashboard : Screen("parent_dashboard/{childUid}") {
        fun createRoute(childUid: String) = "parent_dashboard/$childUid"
    }
}
