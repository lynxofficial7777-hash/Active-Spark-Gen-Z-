package com.activespark.gen7

import android.app.Application
import com.activespark.gen7.data.models.Challenge
import com.activespark.gen7.data.models.Difficulty
import com.activespark.gen7.data.models.ExerciseType
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Active Spark Gen 7.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection
 * across the entire application.
 */
@HiltAndroidApp
class ActiveSparkApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Firebase is auto-initialized via google-services.json
        seedExerciseChallenges()
    }

    /**
     * Seeds 5 default exercise challenges to Firebase Realtime Database
     * only if the challenges node is empty.
     */
    private fun seedExerciseChallenges() {
        val db = FirebaseDatabase.getInstance(
            "https://active-spark-gen7-default-rtdb.asia-southeast1.firebasedatabase.app"
        )
        val challengesRef = db.getReference("challenges")

        challengesRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                val seeds = listOf(
                    Challenge(
                        challengeId = "challenge_push_ups",
                        name = "Push Ups",
                        description = "Upper body strength challenge",
                        instruction = "Keep your body straight, lower chest to floor, push back up.",
                        exerciseType = ExerciseType.PUSH_UP,
                        difficulty = Difficulty.BEGINNER,
                        durationSeconds = 60,
                        targetReps = 30,
                        xpReward = 100,
                        bonusXpForWin = 50,
                        isActive = true,
                        createdAt = System.currentTimeMillis()
                    ),
                    Challenge(
                        challengeId = "challenge_squats",
                        name = "Squats",
                        description = "Lower body power challenge",
                        instruction = "Feet shoulder-width apart, lower until thighs are parallel, drive back up.",
                        exerciseType = ExerciseType.SQUAT,
                        difficulty = Difficulty.BEGINNER,
                        durationSeconds = 60,
                        targetReps = 50,
                        xpReward = 120,
                        bonusXpForWin = 60,
                        isActive = true,
                        createdAt = System.currentTimeMillis()
                    ),
                    Challenge(
                        challengeId = "challenge_jumping_jacks",
                        name = "Jumping Jacks",
                        description = "Full body cardio challenge",
                        instruction = "Jump feet out while raising arms overhead, then return to start.",
                        exerciseType = ExerciseType.JUMPING_JACK,
                        difficulty = Difficulty.BEGINNER,
                        durationSeconds = 45,
                        targetReps = 40,
                        xpReward = 80,
                        bonusXpForWin = 40,
                        isActive = true,
                        createdAt = System.currentTimeMillis()
                    ),
                    Challenge(
                        challengeId = "challenge_burpees",
                        name = "Burpees",
                        description = "Elite full body explosive challenge",
                        instruction = "Squat down, kick feet back to plank, do a push-up, jump feet in, leap up.",
                        exerciseType = ExerciseType.BURPEE,
                        difficulty = Difficulty.ADVANCED,
                        durationSeconds = 60,
                        targetReps = 20,
                        xpReward = 150,
                        bonusXpForWin = 75,
                        isActive = true,
                        createdAt = System.currentTimeMillis()
                    ),
                    Challenge(
                        challengeId = "challenge_sit_ups",
                        name = "Sit Ups",
                        description = "Core strength challenge",
                        instruction = "Lie on back, knees bent, hands behind head, curl up to knees.",
                        exerciseType = ExerciseType.SIT_UP,
                        difficulty = Difficulty.BEGINNER,
                        durationSeconds = 60,
                        targetReps = 30,
                        xpReward = 100,
                        bonusXpForWin = 50,
                        isActive = true,
                        createdAt = System.currentTimeMillis()
                    )
                )
                seeds.forEach { challenge ->
                    challengesRef.child(challenge.challengeId).setValue(challenge)
                }
            }
        }
    }
}
