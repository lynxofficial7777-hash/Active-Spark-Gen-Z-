package com.activespark.gen7.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.activespark.gen7.data.models.Challenge
import com.activespark.gen7.data.models.User
import com.activespark.gen7.data.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class HomeUiState(
    val user: User? = null,
    val challenges: List<Challenge> = emptyList(),
    val dailyChallenge: Challenge? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Load current user
            repository.currentUid?.let { uid ->
                repository.getUser(uid).onSuccess { user ->
                    _uiState.update { it.copy(user = user) }
                }
                // Observe real-time user updates
                launch {
                    repository.observeUser(uid).collect { user ->
                        _uiState.update { it.copy(user = user) }
                    }
                }
            }

            // Try Firebase with a 3-second timeout; fall back to local list if
            // the result is empty, times out, or fails (common on emulator).
            val firebaseChallenges = withTimeoutOrNull(3_000L) {
                repository.getChallenges().getOrNull()
            }

            val challenges = if (!firebaseChallenges.isNullOrEmpty()) {
                firebaseChallenges
            } else {
                // Seed to Firebase in the background so future loads succeed
                repository.seedFallbackChallengesAsync()
                repository.fallbackChallenges
            }

            _uiState.update { state ->
                state.copy(
                    challenges = challenges,
                    dailyChallenge = challenges.firstOrNull(),
                    isLoading = false
                )
            }
        }
    }

    fun refresh() = loadData()
}
