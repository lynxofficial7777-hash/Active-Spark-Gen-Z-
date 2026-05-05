package com.activespark.gen7.ui.screens.parentdashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.activespark.gen7.data.models.ParentalSettings
import com.activespark.gen7.data.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ParentDashboardUiState(
    val childName: String = "Loading...",
    val childLevel: Int = 1,
    val childXp: Int = 0,
    val weeklyBattles: Int = 0,
    val weeklyMinutes: Int = 0,
    val weeklyCalories: Int = 0,
    val weeklyWins: Int = 0,
    val dailyLimitHours: Int = 2,
    val allowOnlineBattles: Boolean = true,
    val allowChat: Boolean = false,
    val allowNotifications: Boolean = true,
    val recentBattles: List<Pair<String, String>> = emptyList(),
    val isSaving: Boolean = false,
    val saveError: String? = null
)

@HiltViewModel
class ParentDashboardViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParentDashboardUiState())
    val uiState: StateFlow<ParentDashboardUiState> = _uiState.asStateFlow()

    private var currentChildUid: String = ""

    fun loadChild(childUid: String) {
        currentChildUid = childUid
        viewModelScope.launch {
            // Load child profile stats
            repository.getUser(childUid).onSuccess { user ->
                user?.let {
                    _uiState.update { state ->
                        state.copy(
                            childName = it.displayName.ifEmpty { it.username },
                            childLevel = it.level,
                            childXp = it.xp,
                            weeklyBattles = it.totalBattles,
                            weeklyWins = it.totalWins,
                            weeklyMinutes = it.totalActiveMinutes
                        )
                    }
                }
            }

            // Load saved parental settings
            repository.getParentalSettings(childUid).onSuccess { settings ->
                _uiState.update { state ->
                    state.copy(
                        dailyLimitHours = settings.dailyLimitHours,
                        allowOnlineBattles = settings.allowOnlineBattles,
                        allowChat = settings.allowChat,
                        allowNotifications = settings.allowNotifications
                    )
                }
            }
        }

        // Observe real-time parental settings changes (e.g. other device updates)
        viewModelScope.launch {
            repository.observeParentalSettings(childUid).collect { settings ->
                _uiState.update { state ->
                    state.copy(
                        dailyLimitHours = settings.dailyLimitHours,
                        allowOnlineBattles = settings.allowOnlineBattles,
                        allowChat = settings.allowChat,
                        allowNotifications = settings.allowNotifications
                    )
                }
            }
        }
    }

    fun setDailyLimit(hours: Int) {
        _uiState.update { it.copy(dailyLimitHours = hours) }
        persistSettings()
    }

    fun toggleOnlineBattles(enabled: Boolean) {
        _uiState.update { it.copy(allowOnlineBattles = enabled) }
        persistSettings()
    }

    fun toggleChat(enabled: Boolean) {
        _uiState.update { it.copy(allowChat = enabled) }
        persistSettings()
    }

    fun toggleNotifications(enabled: Boolean) {
        _uiState.update { it.copy(allowNotifications = enabled) }
        persistSettings()
    }

    /** Writes the current settings snapshot to Firebase Realtime Database. */
    private fun persistSettings() {
        if (currentChildUid.isEmpty()) return
        val state = _uiState.value
        val settings = ParentalSettings(
            childUid = currentChildUid,
            dailyLimitHours = state.dailyLimitHours,
            allowOnlineBattles = state.allowOnlineBattles,
            allowChat = state.allowChat,
            allowNotifications = state.allowNotifications
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            repository.saveParentalSettings(settings)
                .onFailure { e ->
                    _uiState.update { it.copy(saveError = "Failed to save: ${e.message}") }
                }
            _uiState.update { it.copy(isSaving = false) }
        }
    }
}
