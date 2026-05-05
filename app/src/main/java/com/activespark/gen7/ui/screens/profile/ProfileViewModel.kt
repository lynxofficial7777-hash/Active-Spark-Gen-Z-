package com.activespark.gen7.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.activespark.gen7.data.models.User
import com.activespark.gen7.data.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val isOwnProfile: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadUser(uid: String) {
        val resolvedUid = if (uid == "me") repository.currentUid ?: uid else uid
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getUser(resolvedUid).onSuccess { user ->
                _uiState.update {
                    it.copy(
                        user = user,
                        isOwnProfile = resolvedUid == repository.currentUid,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun signOut() = repository.signOut()
}
