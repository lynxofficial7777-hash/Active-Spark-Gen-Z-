package com.activespark.gen7.data.models

import kotlinx.serialization.Serializable

/**
 * Represents a fitness challenge that forms the basis of each battle.
 * Stored in Firestore under /challenges/{challengeId}
 */
@Serializable
data class Challenge(
    val challengeId: String = "",
    val name: String = "",
    val description: String = "",
    val instruction: String = "",       // Step-by-step instructions shown to player
    val exerciseType: ExerciseType = ExerciseType.SQUAT,
    val difficulty: Difficulty = Difficulty.BEGINNER,
    val durationSeconds: Int = 60,
    val targetReps: Int = 20,
    val xpReward: Int = 100,
    val bonusXpForWin: Int = 50,
    val thumbnailUrl: String = "",
    val animationUrl: String = "",      // Lottie animation URL for exercise demo
    val targetAgeMin: Int = 8,
    val targetAgeMax: Int = 16,
    val requiredBodyParts: List<String> = emptyList(), // MediaPipe landmarks needed
    val isActive: Boolean = true,
    val createdAt: Long = 0L
)

/**
 * Exercise types supported by MediaPipe pose detection.
 */
enum class ExerciseType(
    val displayName: String,
    val icon: String,
    val muscleGroups: List<String>
) {
    SQUAT("Squats", "🏋️", listOf("Legs", "Glutes", "Core")),
    PUSH_UP("Push-Ups", "💪", listOf("Chest", "Arms", "Core")),
    JUMPING_JACK("Jumping Jacks", "⚡", listOf("Full Body")),
    HIGH_KNEE("High Knees", "🦵", listOf("Legs", "Core", "Cardio")),
    BURPEE("Burpees", "🔥", listOf("Full Body")),
    LUNGE("Lunges", "🎯", listOf("Legs", "Glutes")),
    PLANK("Plank Hold", "🛡️", listOf("Core", "Arms")),
    MOUNTAIN_CLIMBER("Mountain Climbers", "⛰️", listOf("Core", "Arms", "Legs")),
    STAR_JUMP("Star Jumps", "⭐", listOf("Full Body")),
    DANCE_MOVE("Dance Battle", "🎵", listOf("Full Body", "Coordination")),
    SIT_UP("Sit Ups", "🧘", listOf("Core", "Abs"))
}

/**
 * Difficulty levels for challenges.
 */
enum class Difficulty(val displayName: String, val colorHex: String) {
    BEGINNER("Beginner", "0xFF39FF14"),
    INTERMEDIATE("Intermediate", "0xFFFFE600"),
    ADVANCED("Advanced", "0xFFFF1B8D"),
    ELITE("Elite", "0xFFBF00FF")
}
