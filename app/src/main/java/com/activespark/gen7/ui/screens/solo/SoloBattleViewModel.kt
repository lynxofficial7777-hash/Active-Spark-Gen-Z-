package com.activespark.gen7.ui.screens.solo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.activespark.gen7.data.models.ExerciseType
import com.activespark.gen7.data.models.Score
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

data class SoloBattleUiState(
    val challengeName: String = "",
    val exerciseType: ExerciseType = ExerciseType.SQUAT,
    val targetReps: Int = 0,
    val myReps: Int = 0,
    val myScore: Float = 0f,
    val formAccuracy: Float = 1f,
    val secondsRemaining: Int = 60,
    val totalSeconds: Int = 60,
    val isCountdown: Boolean = false,
    val countdownValue: Int = 3,
    val showCameraSetup: Boolean = false,
    val isBattleOver: Boolean = false,

    // Results (populated when isBattleOver = true)
    val xpEarned: Int = 0,
    val challengeId: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val saveError: String? = null
)

@HiltViewModel
class SoloBattleViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SoloBattleUiState())
    val uiState: StateFlow<SoloBattleUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun initSoloBattle(challengeId: String) {
        viewModelScope.launch {
            val challenge = repository.getChallenge(challengeId).getOrNull() ?: return@launch
            val needsSetup = challenge.exerciseType in
                    setOf(ExerciseType.PUSH_UP, ExerciseType.PLANK)

            _uiState.update {
                it.copy(
                    challengeName    = challenge.name,
                    exerciseType     = challenge.exerciseType,
                    targetReps       = challenge.targetReps,
                    totalSeconds     = challenge.durationSeconds,
                    secondsRemaining = challenge.durationSeconds,
                    xpEarned         = challenge.xpReward,   // base XP (no opponent bonus)
                    challengeId      = challengeId,
                    showCameraSetup  = needsSetup
                )
            }
            if (!needsSetup) startCountdown()
        }
    }

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
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            val total = _uiState.value.totalSeconds
            for (s in total downTo 0) {
                _uiState.update { it.copy(secondsRemaining = s) }
                if (s == 0) {
                    endBattle()
                    break
                }
                delay(1000)
            }
        }
    }

    /** Called by CameraPreviewView for every confirmed rep. */
    fun onRepDetected(formScore: Float) {
        _uiState.update { state ->
            val newReps = state.myReps + 1
            state.copy(
                myReps       = newReps,
                myScore      = newReps * formScore,
                formAccuracy = formScore
            )
        }
    }

    private fun endBattle() {
        _uiState.update { it.copy(isBattleOver = true) }
        saveResults()
    }

    private fun saveResults() {
        val state = _uiState.value
        val uid   = repository.currentUid ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            // Update XP, level, rank, win streak (solo = always a "win" for progress)
            repository.updateUserStatsAfterBattle(
                uid      = uid,
                xpEarned = state.xpEarned,
                isWinner = true,
                isDraw   = false
            )

            // Save individual score record
            repository.saveScore(
                Score(
                    matchId       = "solo_${System.currentTimeMillis()}",
                    playerUid     = uid,
                    challengeId   = state.challengeId,
                    repsCompleted = state.myReps,
                    formAccuracy  = state.formAccuracy,
                    totalScore    = state.myScore,
                    xpEarned      = state.xpEarned,
                    isWinner      = true,
                    timestamp     = System.currentTimeMillis()
                )
            )

            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
