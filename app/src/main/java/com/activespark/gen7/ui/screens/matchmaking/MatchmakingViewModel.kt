package com.activespark.gen7.ui.screens.matchmaking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.activespark.gen7.data.models.Match
import com.activespark.gen7.data.models.MatchStatus
import com.activespark.gen7.data.models.PlayerRank
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

data class MatchmakingUiState(
    val matchId: String? = null,
    val queueSize: Int = 1,
    val elapsedSeconds: Int = 0,
    val isSearching: Boolean = false
)

@HiltViewModel
class MatchmakingViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchmakingUiState())
    val uiState: StateFlow<MatchmakingUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var queueJob: Job? = null
    private var currentChallengeId: String = ""

    fun startMatchmaking(challengeId: String) {
        currentChallengeId = challengeId
        val uid = repository.currentUid ?: return
        _uiState.update { it.copy(isSearching = true) }

        // Start elapsed timer
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }

        // Load actual player rank, then join the queue
        viewModelScope.launch {
            val rank = repository.getUser(uid)
                .getOrNull()?.rank
                ?: PlayerRank.BRONZE
            repository.joinMatchmakingQueue(uid, rank)
        }

        // Observe the queue for an opponent
        queueJob = viewModelScope.launch {
            repository.observeMatchmakingQueue().collect { queueMap ->
                val size = queueMap?.size ?: 1
                _uiState.update { it.copy(queueSize = size) }

                // Only the alphabetically-first UID creates the match — prevents duplicates
                if (size >= 2 && uid == queueMap?.keys?.sorted()?.first()) {
                    val opponentUid = queueMap.keys.sorted()[1]   // index 1, not last()
                    if (opponentUid != uid) {
                        createMatch(uid, opponentUid, challengeId)
                    }
                }
            }
        }
    }

    private fun createMatch(player1Uid: String, player2Uid: String, challengeId: String) {
        viewModelScope.launch {
            val match = Match(
                player1Uid = player1Uid,
                player2Uid = player2Uid,
                challengeId = challengeId,
                status = MatchStatus.COUNTDOWN,
                durationSeconds = 60,
                createdAt = System.currentTimeMillis()
            )
            repository.createMatch(match).onSuccess { matchId ->
                repository.leaveMatchmakingQueue(player1Uid)
                repository.leaveMatchmakingQueue(player2Uid)
                _uiState.update { it.copy(matchId = matchId) }
            }
        }
    }

    fun cancelMatchmaking() {
        timerJob?.cancel()
        queueJob?.cancel()
        viewModelScope.launch {
            repository.currentUid?.let { uid ->
                repository.leaveMatchmakingQueue(uid)
            }
        }
        _uiState.update { it.copy(isSearching = false) }
    }

    override fun onCleared() {
        super.onCleared()
        cancelMatchmaking()
    }
}
