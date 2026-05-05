package com.activespark.gen7.ui.screens.avatar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.activespark.gen7.data.models.AvatarConfig
import com.activespark.gen7.data.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── Static option catalogs ──────────────────────────────────────────────────

/** Map of bodyType key → display emoji. */
val AVATAR_CHARACTERS = linkedMapOf(
    "spark"     to "⚡",
    "hero"      to "🦸",
    "ninja"     to "🥷",
    "astronaut" to "🧑‍🚀",
    "robot"     to "🤖",
    "wizard"    to "🧙",
    "lifter"    to "🏋️",
    "cool"      to "😎"
)

/** Map of color-theme key → hex ARGB string stored in hairColor field. */
val AVATAR_COLORS = linkedMapOf(
    "cyan"   to "0xFF00F5FF",
    "pink"   to "0xFFFF1B8D",
    "green"  to "0xFF39FF14",
    "purple" to "0xFFBF00FF",
    "orange" to "0xFFFF6B00",
    "yellow" to "0xFFFFE600"
)

/** Map of outfit key → display label. */
val AVATAR_OUTFITS = linkedMapOf(
    "default_cyber" to "⚡ Cyber",
    "flame"         to "🔥 Flame",
    "electric"      to "🌩️ Electric",
    "ice"           to "❄️ Ice",
    "shadow"        to "🌑 Shadow",
    "gold"          to "✨ Gold"
)

/** Map of accessory key → display emoji (or "–" for none). */
val AVATAR_ACCESSORIES = linkedMapOf(
    "none"       to "–",
    "crown"      to "👑",
    "goggles"    to "🥽",
    "headphones" to "🎧",
    "mask"       to "😷",
    "star"       to "⭐"
)

// ─── UI State ─────────────────────────────────────────────────────────────────

data class AvatarCustomizationUiState(
    val config: AvatarConfig = AvatarConfig(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val saveError: String? = null
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class AvatarCustomizationViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AvatarCustomizationUiState())
    val uiState: StateFlow<AvatarCustomizationUiState> = _uiState.asStateFlow()

    init {
        loadCurrentAvatar()
    }

    private fun loadCurrentAvatar() {
        val uid = repository.currentUid ?: return
        viewModelScope.launch {
            repository.getUser(uid).onSuccess { user ->
                _uiState.update { it.copy(config = user.avatarConfig, isLoading = false) }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectCharacter(bodyType: String) =
        _uiState.update { it.copy(config = it.config.copy(bodyType = bodyType), isSaved = false) }

    fun selectColorTheme(colorKey: String) {
        val hex = AVATAR_COLORS[colorKey] ?: return
        _uiState.update { it.copy(config = it.config.copy(hairColor = hex), isSaved = false) }
    }

    fun selectOutfit(outfit: String) =
        _uiState.update { it.copy(config = it.config.copy(outfit = outfit), isSaved = false) }

    fun selectAccessory(accessory: String) =
        _uiState.update { it.copy(config = it.config.copy(accessory = accessory), isSaved = false) }

    fun saveAvatar() {
        val uid = repository.currentUid ?: return
        val config = _uiState.value.config
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            repository.saveAvatarConfig(uid, config)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSaving = false, saveError = e.message ?: "Failed to save")
                    }
                }
        }
    }
}
