package com.activespark.gen7.data.models

import kotlinx.serialization.Serializable

/**
 * Represents a player's score/performance record from a single match.
 * Stored in Firestore under /scores/{scoreId}
 */
@Serializable
data class Score(
    val scoreId: String = "",
    val matchId: String = "",
    val playerUid: String = "",
    val username: String = "",
    val challengeId: String = "",
    val exerciseType: ExerciseType = ExerciseType.SQUAT,
    val repsCompleted: Int = 0,
    val formAccuracy: Float = 0f,       // 0.0 – 1.0, from MediaPipe analysis
    val totalScore: Float = 0f,          // reps * formAccuracy * difficulty multiplier
    val caloriesBurned: Float = 0f,
    val activeSeconds: Int = 0,
    val xpEarned: Int = 0,
    val isWinner: Boolean = false,
    val timestamp: Long = 0L
)

/**
 * Aggregated leaderboard entry for the global / weekly / friends leaderboard.
 * Stored in Firestore under /leaderboard/{period}/{playerUid}
 */
@Serializable
data class LeaderboardEntry(
    val rank: Int = 0,
    val playerUid: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val playerRank: PlayerRank = PlayerRank.BRONZE,
    val totalXp: Int = 0,
    val totalWins: Int = 0,
    val totalScore: Float = 0f,
    val period: LeaderboardPeriod = LeaderboardPeriod.ALL_TIME
)

enum class LeaderboardPeriod(val displayName: String) {
    DAILY("Today"),
    WEEKLY("This Week"),
    MONTHLY("This Month"),
    ALL_TIME("All Time")
}

/**
 * Notification model for FCM push notifications.
 */
@Serializable
data class SparkNotification(
    val notificationId: String = "",
    val type: NotificationType = NotificationType.BATTLE_INVITE,
    val fromUid: String = "",
    val fromUsername: String = "",
    val toUid: String = "",
    val title: String = "",
    val body: String = "",
    val matchId: String = "",
    val isRead: Boolean = false,
    val timestamp: Long = 0L
)

enum class NotificationType {
    BATTLE_INVITE,
    BATTLE_RESULT,
    FRIEND_REQUEST,
    ACHIEVEMENT_UNLOCKED,
    DAILY_CHALLENGE,
    RANK_UP,
    SYSTEM
}
