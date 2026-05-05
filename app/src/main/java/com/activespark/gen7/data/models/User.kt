package com.activespark.gen7.data.models

import kotlinx.serialization.Serializable

/**
 * Represents a user / player in Active Spark Gen 7.
 * Stored in Firestore under /users/{uid}
 */
@Serializable
data class User(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val age: Int = 0,
    val parentEmail: String = "",         // For parental dashboard
    val isParentalConsentGiven: Boolean = false,
    val rank: PlayerRank = PlayerRank.BRONZE,
    val xp: Int = 0,
    val level: Int = 1,
    val totalWins: Int = 0,
    val totalLosses: Int = 0,
    val totalBattles: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalCaloriesBurned: Float = 0f,
    val totalActiveMinutes: Int = 0,
    val badges: List<String> = emptyList(),
    val friends: List<String> = emptyList(),    // List of UIDs
    val isOnline: Boolean = false,
    val fcmToken: String = "",
    val createdAt: Long = 0L,
    val lastSeenAt: Long = 0L,
    val avatarConfig: AvatarConfig = AvatarConfig()
)

/**
 * Player ranking tiers.
 */
enum class PlayerRank(val displayName: String, val minXp: Int) {
    BRONZE("Bronze", 0),
    SILVER("Silver", 500),
    GOLD("Gold", 1500),
    PLATINUM("Platinum", 3500),
    DIAMOND("Diamond", 7500),
    MASTER("Master", 15000)
}

/**
 * Avatar customization options stored as part of user profile.
 */
@Serializable
data class AvatarConfig(
    val bodyType: String = "default",
    val hairStyle: String = "default",
    val hairColor: String = "0xFF00F5FF",
    val skinTone: String = "default",
    val outfit: String = "default_cyber",
    val accessory: String = "none"
)
