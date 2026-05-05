package com.activespark.gen7.ui.screens.battle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.activespark.gen7.data.models.ExerciseType
import com.activespark.gen7.data.models.MatchStatus
import com.activespark.gen7.data.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BattleUiState(
    val matchId: String = "",
    val opponentName: String = "Opponent",
    val myReps: Int = 0,
    val opponentReps: Int = 0,
    val myScore: Float = 0f,
    val opponentScore: Float = 0f,
    val formAccuracy: Float = 1f,
    val secondsRemaining: Int = 60,
    val totalSeconds: Int = 60,
    val isCountdown: Boolean = false,   // false until match data loads
    val countdownValue: Int = 3,
    val isBattleOver: Boolean = false,
    val exerciseType: ExerciseType = ExerciseType.SQUAT,
    val showCameraSetup: Boolean = false
)

/** Exercises that require phone placement to the side for accurate angle detection. */
val SIDE_CAMERA_EXERCISES = setOf(ExerciseType.PUSH_UP, ExerciseType.PLANK)

/**
 * BattleViewModel manages:
 * - Loading match data (exercise type + player role) from Firebase
 * - The camera setup screen (side-view exercises only)
 * - The 3-2-1 countdown timer
 * - The main 60-second battle timer
 * - Rep counting callbacks from MediaPipe
 * - Live state syncing via partial updateChildren (never wipes opponent data)
 * - Observing opponent's live reps/score
 */
@HiltViewModel
class BattleViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BattleUiState())
    val uiState: StateFlow<BattleUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var currentMatchId: String = ""
    private var currentIsPlayer1: Boolean = true   // Determines which DB slot we write to

    fun initBattle(matchId: String) {
        currentMatchId = matchId
        observeOpponent(matchId)

        viewModelScope.launch {
            // Load match → determine player role + exercise type
            val match = repository.getMatch(matchId).getOrNull()
            val uid = repository.currentUid ?: ""
            currentIsPlayer1 = match?.player1Uid == uid

            val exerciseType = match?.challengeId
                ?.let { repository.getChallenge(it).getOrNull()?.exerciseType }
                ?: ExerciseType.SQUAT   // safe fallback

            val needsSetup = exerciseType in SIDE_CAMERA_EXERCISES
            _uiState.update {
                it.copy(
                    matchId = matchId,
                    exerciseType = exerciseType,
                    showCameraSetup = needsSetup
                )
            }
            if (!needsSetup) startCountdown()
        }
    }

    /** Called when the player taps "I'm Ready" on the camera placement screen. */
    fun onCameraSetupDismissed() {
        _uiState.update { it.copy(showCameraSetup = false) }
        startCountdown()
    }

    private fun startCountdown() {
        viewModelScope.launch {
            for (count in 3 downTo 1) {
                _uiState.update { it.copy(isCountdown = true, countdownValue = count) }
                delay(1000)
            }
            _uiState.update { it.copy(isCountdown = false) }
            startBattleTimer()
        }
    }

    private fun startBattleTimer() {
        timerJob = viewModelScope.launch {
            val totalSeconds = _uiState.value.totalSeconds
            for (s in totalSeconds downTo 0) {
                _uiState.update { it.copy(secondsRemaining = s) }
                syncLiveState()
                if (s == 0) {
                    _uiState.update { it.copy(isBattleOver = true) }
                    break
                }
                delay(1000)
            }
        }
    }

    /** Called by CameraPreviewView when MediaPipe confirms a valid rep. */
    fun onRepDetected(formScore: Float) {
        _uiState.update { state ->
            val newReps = state.myReps + 1
            state.copy(
                myReps = newReps,
                myScore = newReps * formScore,
                formAccuracy = formScore
            )
        }
        syncLiveState()
    }

    /** Writes only the current player's fields — never overwrites opponent data. */
    private fun syncLiveState() {
        val state = _uiState.value
        viewModelScope.launch {
            repository.updatePlayerScore(
                matchId = currentMatchId,
                isPlayer1 = currentIsPlayer1,
                reps = state.myReps,
                score = state.myScore,
                secondsRemaining = state.secondsRemaining,
                isOver = state.isBattleOver
            )
        }
    }

    /** Reads the correct opponent slot based on whether we are player1 or player2. */
    private fun observeOpponent(matchId: String) {
        viewModelScope.launch {
            repository.observeMatchLiveState(matchId).collect { liveState ->
                liveState?.let { state ->
                    val opponentReps  = if (currentIsPlayer1) state.player2Reps  else state.player1Reps
                    val opponentScore = if (currentIsPlayer1) state.player2Score else state.player1Score
                    _uiState.update {
                        it.copy(opponentReps = opponentReps, opponentScore = opponentScore)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
