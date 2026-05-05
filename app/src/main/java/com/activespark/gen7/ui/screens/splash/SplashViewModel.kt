package com.activespark.gen7.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.activespark.gen7.data.repository.FirebaseRepository
import com.activespark.gen7.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SplashViewModel — checks auth state and routes to Login or Home.
 * 2-second delay for the splash animation to complete.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {

    private val _navigationDestination = MutableStateFlow<String?>(null)
    val navigationDestination: StateFlow<String?> = _navigationDestination.asStateFlow()

    init {
        checkAuthAndNavigate()
    }

    private fun checkAuthAndNavigate() {
        viewModelScope.launch {
            // Allow splash animation to play
            delay(2500L)
            val destination = if (repository.currentUser != null) {
                Screen.Home.route
            } else {
                Screen.Onboarding.route
            }
            _navigationDestination.value = destination
        }
    }
}
