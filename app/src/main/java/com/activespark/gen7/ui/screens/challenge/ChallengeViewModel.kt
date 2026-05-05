package com.activespark.gen7.ui.screens.challenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.activespark.gen7.data.models.Challenge
import com.activespark.gen7.data.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class ChallengeUiState(
    val challenge: Challenge? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class ChallengeViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChallengeUiState())
    val uiState: StateFlow<ChallengeUiState> = _uiState.asStateFlow()

    fun loadChallenge(challengeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Try Firebase with a 3-second timeout.
            // getChallenge() already checks the local fallback list on failure,
            // but the timeout guards against a hanging network call on emulator.
            val challenge = withTimeoutOrNull(3_000L) {
                repository.getChallenge(challengeId).getOrNull()
            }
                ?: repository.fallbackChallenges.find { it.challengeId == challengeId }
                ?: repository.fallbackChallenges.firstOrNull()

            _uiState.update { it.copy(challenge = challenge, isLoading = false) }
        }
    }
}
