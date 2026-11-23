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

data class ProfileEditUiState(
    val profileId: String = "new",
    val isNew: Boolean = true,
    val name: String = "",
    val avatarId: String = "avatar_01",
    val preferredLanguage: String = "Hindi",
    val preferredCountry: String = "IN",
    val isKids: Boolean = false,
    val isLocked: Boolean = false,
    val existingProfiles: List<ProfileEntity> = emptyList(),
    val totalProfilesCount: Int = 1,
    val isParentalPinSet: Boolean = false,
    val nameError: String? = null,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null
)

class ProfileEditViewModel(
    private val profileId: String,
    private val profileRepository: ProfileRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val isNew = profileId == "new" || profileId.isBlank()
    private val _name = MutableStateFlow("")
    private val _avatarId = MutableStateFlow("avatar_01")
    private val _preferredLanguage = MutableStateFlow("Hindi")
    private val _preferredCountry = MutableStateFlow("IN")
    private val _isKids = MutableStateFlow(false)
    private val _isLocked = MutableStateFlow(false)
    private val _nameError = MutableStateFlow<String?>(null)
    private val _isSaving = MutableStateFlow(false)
    private val _isDeleting = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private var initialLoaded = false

    private val baseStateFlow = combine(
        _name,
        _avatarId,
        _preferredLanguage,
        _preferredCountry,
        _isKids
    ) { name, avatar, lang, country, kids ->
        FiveTuple(name, avatar, lang, country, kids)
    }

    private data class FiveTuple(
        val name: String,
        val avatar: String,
        val lang: String,
        val country: String,
        val kids: Boolean
    )

    val uiState: StateFlow<ProfileEditUiState> = combine(
        baseStateFlow,
        _isLocked,
        _nameError,
        _isSaving,
        combine(profileRepository.profiles, userPreferences.parentalPinHash, _error) { profs, pinHash, err ->
            Triple(profs, pinHash.isNotBlank(), err)
        }
    ) { five, locked, nameErr, saving, (profs, hasPin, err) ->
        ProfileEditUiState(
            profileId = profileId,
            isNew = isNew,
            name = five.name,
            avatarId = five.avatar,
            preferredLanguage = five.lang,
            preferredCountry = five.country,
            isKids = five.kids,
            isLocked = locked,
            existingProfiles = profs,
            totalProfilesCount = profs.size,
            isParentalPinSet = hasPin,
            nameError = nameErr,
            isSaving = saving,
            isDeleting = _isDeleting.value,
            error = err
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileEditUiState(profileId = profileId, isNew = isNew)
    )

    init {
        viewModelScope.launch {
            if (!isNew) {
                val profile = profileRepository.getProfile(profileId)
                if (profile != null && !initialLoaded) {
                    initialLoaded = true
                    _name.value = profile.name
                    _avatarId.value = profile.avatarId
                    _preferredLanguage.value = profile.preferredLanguage
                    _preferredCountry.value = profile.preferredCountry
                    _isKids.value = profile.isKids
                    _isLocked.value = profile.isLocked
                }
            } else {
                // Pick next default avatar
                val existing = profileRepository.profiles.firstOrNull() ?: emptyList()
                val usedAvatars = existing.map { it.avatarId }.toSet()
                val nextAvatar = (1..12).map { "avatar_%02d".format(it) }.firstOrNull { it !in usedAvatars } ?: "avatar_01"
                _avatarId.value = nextAvatar
            }
        }
    }

    fun setName(name: String) {
        val trimmed = if (name.length > 16) name.take(16) else name
        _name.value = trimmed
        _nameError.value = null
    }

    fun setAvatarId(avatarId: String) {
        _avatarId.value = avatarId
    }

    fun setPreferredLanguage(language: String) {
        _preferredLanguage.value = language
    }

    fun setPreferredCountry(country: String) {
        _preferredCountry.value = country
    }

    fun setKids(isKids: Boolean) {
        _isKids.value = isKids
    }

    fun setLocked(isLocked: Boolean) {
        _isLocked.value = isLocked
    }

    suspend fun verifyPin(pin: String): Boolean {
        val hash = userPreferences.parentalPinHash.firstOrNull().orEmpty()
        val salt = userPreferences.parentalPinSalt.firstOrNull().orEmpty()
        return ParentalPinHelper.verifyPin(pin, hash, salt)
    }

    fun save(onSuccess: () -> Unit) {
        val currentName = _name.value.trim()
        if (currentName.isBlank()) {
            _nameError.value = "Name cannot be empty"
            return
        }
        val existing = uiState.value.existingProfiles
        val duplicate = existing.any { it.name.equals(currentName, ignoreCase = true) && it.id != profileId }
        if (duplicate) {
            _nameError.value = "A profile with this name already exists"
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            try {
                if (isNew) {
                    val result = profileRepository.createProfile(
                        name = currentName,
                        avatarId = _avatarId.value,
                        preferredLanguage = _preferredLanguage.value,
                        preferredCountry = _preferredCountry.value,
                        isKids = _isKids.value,
                        isLocked = _isLocked.value
                    )
                    if (result.isSuccess) {
                        onSuccess()
                    } else {
                        _error.value = result.exceptionOrNull()?.message ?: "Failed to create profile"
                    }
                } else {
                    val currentProfile = profileRepository.getProfile(profileId)
                    if (currentProfile == null) {
                        _error.value = "Profile not found"
                        return@launch
                    }
                    val updated = currentProfile.copy(
                        name = currentName,
                        avatarId = _avatarId.value,
                        preferredLanguage = _preferredLanguage.value,
                        preferredCountry = _preferredCountry.value,
                        isKids = _isKids.value,
                        isLocked = _isLocked.value,
                        updatedAt = System.currentTimeMillis()
                    )
                    val result = profileRepository.updateProfile(updated)
                    if (result.isSuccess) {
                        onSuccess()
                    } else {
                        _error.value = result.exceptionOrNull()?.message ?: "Failed to update profile"
                    }
                }
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun delete(onSuccess: () -> Unit) {
        if (isNew || uiState.value.totalProfilesCount <= 1) return
        viewModelScope.launch {
            _isDeleting.value = true
            _error.value = null
            try {
                val result = profileRepository.deleteProfile(profileId)
                if (result.isSuccess) {
                    onSuccess()
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to delete profile"
                }
            } finally {
                _isDeleting.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    companion object {
        fun provideFactory(
            profileId: String,
            profileRepository: ProfileRepository,
            userPreferences: UserPreferences
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileEditViewModel(
                    profileId,
                    profileRepository,
                    userPreferences
                ) as T
            }
        }
    }
}
