package com.activespark.gen7.ui.screens.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.activespark.gen7.data.models.PlayerRank
import com.activespark.gen7.data.models.Score
import com.activespark.gen7.data.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResultsUiState(
    // ── Match outcome ──────────────────────────────────────────────────
    val myReps: Int = 0,
    val opponentReps: Int = 0,
    val myScore: Float = 0f,
    val formAccuracy: Float = 0f,
    val isWinner: Boolean = false,
    val isDraw: Boolean = false,
    val xpEarned: Int = 0,
    val challengeId: String = "",

    // ── Rank-up celebration (non-null only when rank just changed) ─────
    val previousRank: PlayerRank? = null,
    val newRank: PlayerRank? = null,

    // ── Persistence state ──────────────────────────────────────────────
    val isSaving: Boolean = false,
    val saveError: String? = null
)

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    /**
     * Guard against double-saves.
     * The ViewModel instance survives config changes, so this flag is reliable
     * for the lifetime of the Results screen.
     */
    private var hasSaved = false

    fun loadResults(matchId: String) {
        viewModelScope.launch {

            // ── Read match metadata (/matches/{matchId}) ───────────────
            // Contains: player UIDs, challengeId, initial xpAwarded (0 = not yet saved)
            val match = repository.getMatch(matchId).getOrNull() ?: return@launch

            // ── Read final reps/scores (/active_matches/{matchId}) ─────
            // BattleViewModel syncs here every second and on every rep.
            // This is the authoritative source for what actually happened in the battle.
            val liveState = repository.getMatchLiveState(matchId).getOrNull()

            // Resolve player identities
            val uid       = repository.currentUid ?: return@launch
            val isPlayer1 = match.player1Uid == uid

            // Prefer live-state reps (reflect actual battle); fall back to match
            // record in case the live state node was cleaned up.
            val p1Reps  = liveState?.player1Reps  ?: match.player1Reps
            val p2Reps  = liveState?.player2Reps  ?: match.player2Reps
            val p1Score = liveState?.player1Score ?: match.player1Score
            val p2Score = liveState?.player2Score ?: match.player2Score

            val myReps       = if (isPlayer1) p1Reps  else p2Reps
            val opponentReps = if (isPlayer1) p2Reps  else p1Reps
            val myScore      = if (isPlayer1) p1Score else p2Score
            val formAccuracy = if (myReps > 0) (myScore / myReps).coerceIn(0f, 1f) else 0f

            // Determine winner by rep count (more reps = winner; tie = draw)
            val winnerUid: String = when {
                p1Reps > p2Reps -> match.player1Uid
                p2Reps > p1Reps -> match.player2Uid
                else            -> ""   // draw
            }
            val isWinner = winnerUid == uid
            val isDraw   = winnerUid.isEmpty()

            // ── Load challenge for correct XP values ───────────────────
            val challenge    = repository.getChallenge(match.challengeId).getOrNull()
            val baseXp       = challenge?.xpReward    ?: 100
            val bonusXp      = challenge?.bonusXpForWin ?: 50
            val xpEarned     = if (isWinner) baseXp + bonusXp else baseXp

            _uiState.update {
                it.copy(
                    myReps       = myReps,
                    opponentReps = opponentReps,
                    myScore      = myScore,
                    formAccuracy = formAccuracy,
                    isWinner     = isWinner,
                    isDraw       = isDraw,
                    xpEarned     = xpEarned,
                    challengeId  = match.challengeId
                )
            }

            // ── Idempotency guard ──────────────────────────────────────
            // If xpAwarded > 0 the match was already finalised by this device
            // (or the opponent's device). Skip the writes.
            if (hasSaved || match.xpAwarded > 0) return@launch
            hasSaved = true

            persistResults(
                matchId      = matchId,
                uid          = uid,
                p1Reps       = p1Reps,
                p2Reps       = p2Reps,
                p1Score      = p1Score,
                p2Score      = p2Score,
                myReps       = myReps,
                myScore      = myScore,
                formAccuracy = formAccuracy,
                isWinner     = isWinner,
                isDraw       = isDraw,
                xpEarned     = xpEarned,
                challengeId  = match.challengeId,
                winnerUid    = winnerUid
            )
        }
    }

    /**
     * Runs three Firebase writes in order:
     *
     *   1. finalizeMatch   — stamps winner + xpAwarded on the match document.
     *                        The Cloud Function `onMatchFinalized` triggers on this
     *                        write and updates the leaderboard server-side.
     *   2. updateUserStats — XP / level / rank / win streak on user profile
     *   3. score record    — individual performance log
     */
    private fun persistResults(
        matchId: String,
        uid: String,
        p1Reps: Int, p2Reps: Int,
        p1Score: Float, p2Score: Float,
        myReps: Int,
        myScore: Float,
        formAccuracy: Float,
        isWinner: Boolean,
        isDraw: Boolean,
        xpEarned: Int,
        challengeId: String,
        winnerUid: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }

            // Step 1 — Finalize match (sets xpAwarded → idempotency key for both players)
            repository.finalizeMatch(
                matchId      = matchId,
                winnerUid    = winnerUid,
                player1Reps  = p1Reps,
                player2Reps  = p2Reps,
                player1Score = p1Score,
                player2Score = p2Score,
                xpAwarded    = xpEarned
            )

            // Step 2 — Update user profile stats (sequential — we need the updated user
            //          for leaderboard rank and rank-up detection)
            val userResult = repository.updateUserStatsAfterBattle(
                uid      = uid,
                xpEarned = xpEarned,
                isWinner = isWinner,
                isDraw   = isDraw
            )

            // Detect rank-up by comparing old rank (derived from pre-battle XP) to new
            userResult.onSuccess { updatedUser ->
                val previousRank = repository.calculateRank(updatedUser.xp - xpEarned)
                val newRank      = updatedUser.rank
                if (newRank != previousRank) {
                    _uiState.update { it.copy(previousRank = previousRank, newRank = newRank) }
                }
            }

            // Step 3 — Score record
            // NOTE: Leaderboard is updated server-side by the Cloud Function
            //       `onMatchFinalized` which triggers when `finalizeMatch()` sets
            //       xpAwarded > 0 above. Updating it here too would double-count.
            val updatedUser = userResult.getOrNull()
            val scoreJob = async {
                repository.saveScore(
                    Score(
                        matchId       = matchId,
                        playerUid     = uid,
                        username      = updatedUser?.username ?: "",
                        challengeId   = challengeId,
                        repsCompleted = myReps,
                        formAccuracy  = formAccuracy,
                        totalScore    = myScore,
                        xpEarned      = xpEarned,
                        isWinner      = isWinner,
                        timestamp     = System.currentTimeMillis()
                    )
                )
            }

            scoreJob.await()

            _uiState.update {
                it.copy(
                    isSaving  = false,
                    saveError = if (userResult.isFailure)
                        "Progress saved locally — will sync when online" else null
                )
            }
        }
    }
}
