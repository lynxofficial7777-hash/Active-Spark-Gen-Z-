package com.activespark.gen7.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.activespark.gen7.data.models.AvatarConfig
import com.activespark.gen7.data.models.Challenge
import com.activespark.gen7.data.models.Difficulty
import com.activespark.gen7.data.models.ExerciseType
import com.activespark.gen7.data.models.LeaderboardEntry
import com.activespark.gen7.data.models.LeaderboardPeriod
import com.activespark.gen7.data.models.Match
import com.activespark.gen7.data.models.MatchLiveState
import com.activespark.gen7.data.models.MatchStatus
import com.activespark.gen7.data.models.ParentalSettings
import com.activespark.gen7.data.models.PlayerRank
import com.activespark.gen7.data.models.Score
import com.activespark.gen7.data.models.User
import kotlin.math.max
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRepository @Inject constructor() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance(
        "https://active-spark-gen7-default-rtdb.asia-southeast1.firebasedatabase.app"
    )

    // ─── Auth ────────────────────────────────────────────────────────

    val currentUser: FirebaseUser? get() = auth.currentUser
    val currentUid: String? get() = auth.currentUser?.uid
    val isLoggedIn: Boolean get() = auth.currentUser != null

    // DB refs
    private val usersRef            = db.getReference("users")
    private val matchesRef          = db.getReference("matches")
    private val activeMatchesRef    = db.getReference("active_matches")
    private val leaderboardRef      = db.getReference("leaderboard")
    private val matchmakingQueueRef = db.getReference("matchmaking_queue")
    private val challengesRef       = db.getReference("challenges")
    private val parentalSettingsRef = db.getReference("parental_settings")
    private val scoresRef           = db.getReference("scores")

    // ─── Fallback Challenges ─────────────────────────────────────────
    // Used when Firebase is unreachable or returns empty (e.g. emulator, no index)

    val fallbackChallenges: List<Challenge> = listOf(
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
            createdAt = 0L
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
            createdAt = 0L
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
            createdAt = 0L
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
            createdAt = 0L
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
            createdAt = 0L
        )
    )

    /** Seeds fallback challenges to Firebase in the background (fire-and-forget). */
    fun seedFallbackChallengesAsync() {
        fallbackChallenges.forEach { challenge ->
            challengesRef.child(challenge.challengeId).setValue(challenge)
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() = auth.signOut()

    // ─── User ────────────────────────────────────────────────────────

    suspend fun saveUser(user: User): Result<Unit> {
        return try {
            usersRef.child(user.uid).setValue(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createUser(user: User): Result<Unit> = saveUser(user)

    suspend fun getUser(uid: String): Result<User> {
        return try {
            val snapshot = usersRef.child(uid).get().await()
            val user = snapshot.getValue(User::class.java)
            if (user != null) Result.success(user)
            else Result.failure(Exception("User not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeUser(uid: String): Flow<User?> = callbackFlow {
        val listener = usersRef.child(uid).addValueEventListener(
            object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    trySend(snapshot.getValue(User::class.java))
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    close(error.toException())
                }
            }
        )
        awaitClose { usersRef.child(uid).removeEventListener(listener) }
    }

    // ─── Challenges ──────────────────────────────────────────────────

    suspend fun getChallenges(): Result<List<Challenge>> {
        return try {
            // Use a plain .get() — orderByChild("isActive") requires a Firebase index
            // that may not exist on emulator/fresh DB, causing silent empty results.
            val snapshot = challengesRef.get().await()
            val challenges = snapshot.children
                .mapNotNull { it.getValue(Challenge::class.java) }
                .filter { it.isActive }
            Result.success(challenges)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChallenge(challengeId: String): Result<Challenge> {
        return try {
            val snapshot = challengesRef.child(challengeId).get().await()
            val challenge = snapshot.getValue(Challenge::class.java)
            if (challenge != null) {
                Result.success(challenge)
            } else {
                // Fall back to local list before reporting failure
                val local = fallbackChallenges.find { it.challengeId == challengeId }
                if (local != null) Result.success(local)
                else Result.failure(Exception("Challenge not found: $challengeId"))
            }
        } catch (e: Exception) {
            // Network error — try local fallback
            val local = fallbackChallenges.find { it.challengeId == challengeId }
            if (local != null) Result.success(local)
            else Result.failure(e)
        }
    }

    // ─── Match ───────────────────────────────────────────────────────

    suspend fun createMatch(match: Match): Result<String> {
        return try {
            val matchId = match.matchId.ifEmpty { UUID.randomUUID().toString() }
            val matchWithId = if (match.matchId.isEmpty()) match.copy(matchId = matchId) else match
            matchesRef.child(matchId).setValue(matchWithId).await()
            Result.success(matchId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMatch(matchId: String): Result<Match?> {
        return try {
            val snapshot = matchesRef.child(matchId).get().await()
            Result.success(snapshot.getValue(Match::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** One-shot read of live match state (reps/scores written during the battle). */
    suspend fun getMatchLiveState(matchId: String): Result<MatchLiveState?> {
        return try {
            val snapshot = activeMatchesRef.child(matchId).get().await()
            Result.success(snapshot.getValue(MatchLiveState::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeMatchLiveState(matchId: String): Flow<MatchLiveState?> = callbackFlow {
        val listener = activeMatchesRef.child(matchId).addValueEventListener(
            object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    trySend(snapshot.getValue(MatchLiveState::class.java))
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    close(error.toException())
                }
            }
        )
        awaitClose { activeMatchesRef.child(matchId).removeEventListener(listener) }
    }

    suspend fun updateMatchLiveState(state: MatchLiveState): Result<Unit> {
        return try {
            activeMatchesRef.child(state.matchId).setValue(state).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Overload kept for legacy callers passing matchId separately
    suspend fun updateMatchLiveState(matchId: String, state: MatchLiveState): Result<Unit> =
        updateMatchLiveState(state.copy(matchId = matchId))

    // ─── Leaderboard ─────────────────────────────────────────────────

    suspend fun getLeaderboard(period: LeaderboardPeriod): Result<List<LeaderboardEntry>> {
        return try {
            val snapshot = leaderboardRef.child(period.name.lowercase())
                .orderByChild("totalXp").limitToLast(100).get().await()
            val entries = snapshot.children
                .mapNotNull { it.getValue(LeaderboardEntry::class.java) }
                .reversed()
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeLeaderboard(period: String): Flow<List<LeaderboardEntry>> = callbackFlow {
        val listener = leaderboardRef.child(period)
            .orderByChild("totalXp").limitToLast(100)
            .addValueEventListener(
                object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        val entries = snapshot.children
                            .mapNotNull { it.getValue(LeaderboardEntry::class.java) }
                            .reversed()
                        trySend(entries)
                    }
                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                        close(error.toException())
                    }
                }
            )
        awaitClose { leaderboardRef.child(period).removeEventListener(listener) }
    }

    // ─── Matchmaking Queue ───────────────────────────────────────────

    suspend fun joinMatchmakingQueue(uid: String, challengeId: String): Result<Unit> {
        return try {
            val queueEntry = mapOf(
                "uid" to uid,
                "challengeId" to challengeId,
                "joinedAt" to System.currentTimeMillis()
            )
            matchmakingQueueRef.child(uid).setValue(queueEntry).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Overload used by MatchmakingViewModel (passes rank instead of challengeId)
    suspend fun joinMatchmakingQueue(uid: String, rank: PlayerRank): Result<Unit> {
        return try {
            val queueEntry = mapOf(
                "uid" to uid,
                "rank" to rank.name,
                "joinedAt" to System.currentTimeMillis()
            )
            matchmakingQueueRef.child(uid).setValue(queueEntry).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveMatchmakingQueue(uid: String): Result<Unit> {
        return try {
            matchmakingQueueRef.child(uid).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeMatchmakingQueue(): Flow<Map<String, Any>?> = callbackFlow {
        val listener = matchmakingQueueRef.addValueEventListener(
            object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    @Suppress("UNCHECKED_CAST")
                    val map = snapshot.value as? Map<String, Any>
                    trySend(map)
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    close(error.toException())
                }
            }
        )
        awaitClose { matchmakingQueueRef.removeEventListener(listener) }
    }

    // ─── Live Match — Partial Update ────────────────────────────────
    // Uses updateChildren() instead of setValue() so each player only writes
    // their own fields and never wipes the opponent's reps/score.

    suspend fun updatePlayerScore(
        matchId: String,
        isPlayer1: Boolean,
        reps: Int,
        score: Float,
        secondsRemaining: Int,
        isOver: Boolean
    ): Result<Unit> {
        return try {
            val prefix = if (isPlayer1) "player1" else "player2"
            val updates = buildMap<String, Any> {
                put("${prefix}Reps", reps)
                put("${prefix}Score", score)
                put("lastUpdated", System.currentTimeMillis())
                // Only player1 is authoritative for shared timer & status
                if (isPlayer1) {
                    put("secondsRemaining", secondsRemaining)
                    put("status", if (isOver) MatchStatus.COMPLETED.name else MatchStatus.IN_PROGRESS.name)
                }
            }
            activeMatchesRef.child(matchId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Post-Battle Persistence ─────────────────────────────────────
    //
    // Call order after a battle ends:
    //   1. finalizeMatch()           — stamp winner + final scores on the match record
    //   2. updateUserStatsAfterBattle() — XP, level, rank, win/loss/streak on user profile
    //   3. upsertLeaderboardEntry()  — update all 4 leaderboard periods
    //   4. saveScore()               — store the individual performance record
    //
    // All four use partial updateChildren() writes so concurrent calls from both
    // players never overwrite each other's data.

    /**
     * Stamps the final outcome onto the match document.
     * Idempotency: safe to call again — repeated writes set the same values.
     */
    suspend fun finalizeMatch(
        matchId: String,
        winnerUid: String,
        player1Reps: Int,
        player2Reps: Int,
        player1Score: Float,
        player2Score: Float,
        xpAwarded: Int
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "winner"       to winnerUid,
                "player1Reps"  to player1Reps,
                "player2Reps"  to player2Reps,
                "player1Score" to player1Score,
                "player2Score" to player2Score,
                "xpAwarded"    to xpAwarded,
                "status"       to MatchStatus.COMPLETED.name,
                "endedAt"      to System.currentTimeMillis()
            )
            matchesRef.child(matchId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reads the user's current stats, applies the battle result, writes back
     * only the changed fields (partial update — never wipes other profile data).
     *
     * Returns the fully updated [User] so the ViewModel can detect a rank change.
     */
    suspend fun updateUserStatsAfterBattle(
        uid: String,
        xpEarned: Int,
        isWinner: Boolean,
        isDraw: Boolean
    ): Result<User> {
        return try {
            val current = getUser(uid).getOrElse { return Result.failure(it) }

            val newXp            = current.xp + xpEarned
            val newLevel         = calculateLevel(newXp)
            val newRank          = calculateRank(newXp)
            val newTotalBattles  = current.totalBattles + 1
            val newTotalWins     = current.totalWins    + if (isWinner) 1 else 0
            val newTotalLosses   = current.totalLosses  + if (!isWinner && !isDraw) 1 else 0
            val newCurrentStreak = if (isWinner) current.currentStreak + 1 else 0
            val newBestStreak    = max(current.bestStreak, newCurrentStreak)

            val updates = mapOf(
                "xp"             to newXp,
                "level"          to newLevel,
                "rank"           to newRank.name,
                "totalBattles"   to newTotalBattles,
                "totalWins"      to newTotalWins,
                "totalLosses"    to newTotalLosses,
                "currentStreak"  to newCurrentStreak,
                "bestStreak"     to newBestStreak,
                "lastSeenAt"     to System.currentTimeMillis()
            )
            usersRef.child(uid).updateChildren(updates).await()

            Result.success(
                current.copy(
                    xp = newXp, level = newLevel, rank = newRank,
                    totalBattles = newTotalBattles,
                    totalWins = newTotalWins, totalLosses = newTotalLosses,
                    currentStreak = newCurrentStreak, bestStreak = newBestStreak
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Read-then-write upsert of a player's entry across all 4 leaderboard periods.
     * Each player writes only their own UID node — no cross-player conflicts.
     */
    suspend fun upsertLeaderboardEntry(
        uid: String,
        username: String,
        displayName: String,
        avatarUrl: String,
        playerRank: PlayerRank,
        xpEarned: Int,
        totalScore: Float,
        isWinner: Boolean
    ): Result<Unit> {
        return try {
            for (period in LeaderboardPeriod.values()) {
                val ref      = leaderboardRef.child(period.name.lowercase()).child(uid)
                val snapshot = ref.get().await()
                val existing = snapshot.getValue(LeaderboardEntry::class.java)

                val updated = LeaderboardEntry(
                    playerUid   = uid,
                    username    = username,
                    displayName = displayName,
                    avatarUrl   = avatarUrl,
                    playerRank  = playerRank,
                    totalXp     = (existing?.totalXp    ?: 0)   + xpEarned,
                    totalWins   = (existing?.totalWins  ?: 0)   + if (isWinner) 1 else 0,
                    totalScore  = (existing?.totalScore ?: 0f)  + totalScore,
                    period      = period
                )
                ref.setValue(updated).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Stores a single match-performance record under /scores/{scoreId}.
     */
    suspend fun saveScore(score: Score): Result<Unit> {
        return try {
            val id = score.scoreId.ifEmpty { UUID.randomUUID().toString() }
            scoresRef.child(id).setValue(score.copy(scoreId = id)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Rank / Level helpers ──────────────────────────────────────────

    /** 1 level per 500 XP, minimum level 1. */
    fun calculateLevel(xp: Int): Int = (xp / 500) + 1

    /** Returns the correct [PlayerRank] for a given XP total. */
    fun calculateRank(xp: Int): PlayerRank = when {
        xp >= PlayerRank.MASTER.minXp   -> PlayerRank.MASTER
        xp >= PlayerRank.DIAMOND.minXp  -> PlayerRank.DIAMOND
        xp >= PlayerRank.PLATINUM.minXp -> PlayerRank.PLATINUM
        xp >= PlayerRank.GOLD.minXp     -> PlayerRank.GOLD
        xp >= PlayerRank.SILVER.minXp   -> PlayerRank.SILVER
        else                            -> PlayerRank.BRONZE
    }

    // ─── Avatar Config ───────────────────────────────────────────────

    /**
     * Writes only the avatarConfig sub-object on the user node.
     * Partial update — never wipes other profile fields.
     */
    suspend fun saveAvatarConfig(uid: String, config: AvatarConfig): Result<Unit> {
        return try {
            usersRef.child(uid).child("avatarConfig").setValue(config).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Parental Settings ───────────────────────────────────────────

    suspend fun saveParentalSettings(settings: ParentalSettings): Result<Unit> {
        return try {
            parentalSettingsRef
                .child(settings.childUid)
                .setValue(settings.copy(lastUpdated = System.currentTimeMillis()))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getParentalSettings(childUid: String): Result<ParentalSettings> {
        return try {
            val snapshot = parentalSettingsRef.child(childUid).get().await()
            val settings = snapshot.getValue(ParentalSettings::class.java)
            if (settings != null) Result.success(settings)
            else Result.success(ParentalSettings(childUid = childUid))  // sensible defaults
        } catch (e: Exception) {
            // Return defaults on network error — safer than blocking the parent
            Result.success(ParentalSettings(childUid = childUid))
        }
    }

    fun observeParentalSettings(childUid: String): Flow<ParentalSettings> = callbackFlow {
        val listener = parentalSettingsRef.child(childUid).addValueEventListener(
            object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val settings = snapshot.getValue(ParentalSettings::class.java)
                        ?: ParentalSettings(childUid = childUid)
                    trySend(settings)
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    close(error.toException())
                }
            }
        )
        awaitClose { parentalSettingsRef.child(childUid).removeEventListener(listener) }
    }
}
