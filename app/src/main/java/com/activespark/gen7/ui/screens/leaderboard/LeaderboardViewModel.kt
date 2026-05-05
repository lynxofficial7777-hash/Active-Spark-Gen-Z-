package com.activespark.gen7.ui.screens.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.activespark.gen7.data.models.LeaderboardEntry
import com.activespark.gen7.data.models.LeaderboardPeriod
import com.activespark.gen7.data.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeaderboardUiState(
    val entries: List<LeaderboardEntry> = emptyList(),
    val selectedPeriod: LeaderboardPeriod = LeaderboardPeriod.ALL_TIME,
    val selectedPeriodIndex: Int = LeaderboardPeriod.entries.indexOf(LeaderboardPeriod.ALL_TIME),
    val currentUserId: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(currentUserId = repository.currentUid ?: "") }
        loadLeaderboard(LeaderboardPeriod.ALL_TIME)
    }

    fun selectPeriod(period: LeaderboardPeriod) {
        _uiState.update {
            it.copy(
                selectedPeriod = period,
                selectedPeriodIndex = LeaderboardPeriod.entries.indexOf(period)
            )
        }
        loadLeaderboard(period)
    }

    private fun loadLeaderboard(period: LeaderboardPeriod) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getLeaderboard(period).onSuccess { entries ->
                _uiState.update {
                    it.copy(
                        entries = entries.mapIndexed { i, e -> e.copy(rank = i + 1) },
                        isLoading = false
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
