package com.activespark.gen7.data.models

import kotlinx.serialization.Serializable

/**
 * Represents an active or completed battle match between two players.
 * Stored in Firestore under /matches/{matchId}
 * Also synced to Realtime Database under /active_matches/{matchId} for live updates.
 */
@Serializable
data class Match(
    val matchId: String = "",
    val player1Uid: String = "",
    val player2Uid: String = "",
    val player1Username: String = "",
    val player2Username: String = "",
    val player1AvatarUrl: String = "",
    val player2AvatarUrl: String = "",
    val challengeId: String = "",
    val status: MatchStatus = MatchStatus.WAITING,
    val winner: String = "",                      // UID of winner, empty if draw/ongoing
    val player1Score: Float = 0f,
    val player2Score: Float = 0f,
    val player1Reps: Int = 0,
    val player2Reps: Int = 0,
    val durationSeconds: Int = 60,
    val startedAt: Long = 0L,
    val endedAt: Long = 0L,
    val createdAt: Long = 0L,
    val xpAwarded: Int = 0,
    val roomCode: String = ""                     // For private matches
)

/**
 * Real-time state of a match — synced via Realtime Database.
 */
@Serializable
data class MatchLiveState(
    val matchId: String = "",
    val player1Reps: Int = 0,
    val player2Reps: Int = 0,
    val player1Score: Float = 0f,
    val player2Score: Float = 0f,
    val secondsRemaining: Int = 60,
    val status: MatchStatus = MatchStatus.COUNTDOWN,
    val lastUpdated: Long = 0L
)

enum class MatchStatus {
    WAITING,        // Waiting for second player
    COUNTDOWN,      // 3-2-1 countdown
    IN_PROGRESS,    // Battle is active
    PAUSED,         // Battle paused
    COMPLETED,      // Battle finished normally
    CANCELLED       // Player left / disconnected
}
