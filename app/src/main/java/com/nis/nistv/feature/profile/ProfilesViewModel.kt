package com.nis.nistv.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nis.nistv.core.database.entity.ProfileEntity
import com.nis.nistv.core.preferences.UserPreferences
import com.nis.nistv.core.security.ParentalPinHelper
import com.nis.nistv.data.parental.ParentalControlManager
import com.nis.nistv.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfilesUiState(
    val profiles: List<ProfileEntity> = emptyList(),
    val activeProfile: ProfileEntity? = null,
    val isManageMode: Boolean = false,
    val isParentalPinSet: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ProfilesViewModel(
    private val profileRepository: ProfileRepository,
    private val parentalControlManager: ParentalControlManager,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _isManageMode = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ProfilesUiState> = combine(
        profileRepository.profiles,
        profileRepository.activeProfile,
        _isManageMode,
        userPreferences.parentalPinHash,
        _error
    ) { profiles, active, isManage, pinHash, error ->
        ProfilesUiState(
            profiles = profiles.sortedBy { it.sortOrder },
            activeProfile = active,
            isManageMode = isManage,
            isParentalPinSet = pinHash.isNotBlank(),
            isLoading = false,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfilesUiState(isLoading = true)
    )

    fun toggleManageMode() {
        _isManageMode.update { !it }
    }

    fun setManageMode(enabled: Boolean) {
        _isManageMode.value = enabled
    }

    suspend fun verifyPin(pin: String): Boolean {
        val hash = userPreferences.parentalPinHash.firstOrNull().orEmpty()
        val salt = userPreferences.parentalPinSalt.firstOrNull().orEmpty()
        return ParentalPinHelper.verifyPin(pin, hash, salt)
    }

    fun switchProfile(profileId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = profileRepository.switchProfile(profileId)
            if (result.isSuccess) {
                onSuccess()
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to switch profile"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    companion object {
        fun provideFactory(
            profileRepository: ProfileRepository,
            parentalControlManager: ParentalControlManager,
            userPreferences: UserPreferences
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfilesViewModel(
                    profileRepository,
                    parentalControlManager,
                    userPreferences
                ) as T
            }
        }
    }
}
