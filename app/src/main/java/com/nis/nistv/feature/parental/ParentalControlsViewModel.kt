package com.nis.nistv.feature.parental

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nis.nistv.core.database.NisTvDatabase
import com.nis.nistv.core.preferences.UserPreferences
import com.nis.nistv.core.security.ParentalPinHelper
import com.nis.nistv.data.parental.ParentalControlManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ParentalControlsUiState(
    val isParentalEnabled: Boolean = false,
    val hasPinSet: Boolean = false,
    val lockedCategories: Set<String> = emptySet(),
    val availableCategories: List<String> = emptyList(),
    val isLockedOut: Boolean = false,
    val lockoutRemainingSec: Long = 0L,
    val wrongAttempts: Int = 0,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ParentalControlsViewModel(
    private val database: NisTvDatabase,
    private val userPreferences: UserPreferences,
    private val parentalControlManager: ParentalControlManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParentalControlsUiState())
    val uiState: StateFlow<ParentalControlsUiState> = _uiState.asStateFlow()

    init {
        observeState()
        startLockoutTicker()
    }

    private fun observeState() {
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                combine(
                    userPreferences.isParentalControlEnabled,
                    userPreferences.parentalPinHash,
                    userPreferences.parentalLockedCategories
                ) { isEnabled, pinHash, lockedCats -> Triple(isEnabled, pinHash, lockedCats) },
                userPreferences.parentalWrongAttempts,
                userPreferences.parentalLockoutUntilMs,
                database.channelDao().getCategories()
            ) { (isEnabled, pinHash, lockedCats), wrongAttempts, lockoutUntil, dbCategories ->
                val remainingMs = ParentalPinHelper.calculateLockoutRemainingMs(lockoutUntil)
                val defaultCategories = setOf("Adult", "XXX", "Erotic", "18+")
                val combinedCategories = (defaultCategories + dbCategories)
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()

                ParentalControlsUiState(
                    isParentalEnabled = isEnabled,
                    hasPinSet = pinHash.isNotBlank(),
                    lockedCategories = lockedCats,
                    availableCategories = combinedCategories,
                    isLockedOut = remainingMs > 0,
                    lockoutRemainingSec = remainingMs / 1000L,
                    wrongAttempts = wrongAttempts,
                    errorMessage = _uiState.value.errorMessage,
                    successMessage = _uiState.value.successMessage
                )
            }.collect { newState ->
                _uiState.update { newState }
            }
        }
    }

    private fun startLockoutTicker() {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val lockoutUntil = userPreferences.parentalLockoutUntilMs.firstOrNull() ?: 0L
                val remainingMs = ParentalPinHelper.calculateLockoutRemainingMs(lockoutUntil)
                if (remainingMs > 0) {
                    _uiState.update {
                        it.copy(
                            isLockedOut = true,
                            lockoutRemainingSec = (remainingMs / 1000L).coerceAtLeast(1)
                        )
                    }
                } else if (_uiState.value.isLockedOut) {
                    _uiState.update {
                        it.copy(
                            isLockedOut = false,
                            lockoutRemainingSec = 0L
                        )
                    }
                }
                delay(1000L)
            }
        }
    }

    suspend fun verifyPin(enteredPin: String): Boolean {
        val lockoutUntil = userPreferences.parentalLockoutUntilMs.firstOrNull() ?: 0L
        if (ParentalPinHelper.calculateLockoutRemainingMs(lockoutUntil) > 0) {
            _uiState.update { it.copy(errorMessage = "Parental controls are locked out. Please wait.") }
            return false
        }

        val storedHash = userPreferences.parentalPinHash.firstOrNull() ?: ""
        val storedSalt = userPreferences.parentalPinSalt.firstOrNull() ?: ""

        val isValid = ParentalPinHelper.verifyPin(enteredPin, storedHash, storedSalt)
        if (isValid) {
            userPreferences.setParentalWrongAttempts(0)
            userPreferences.setParentalLockoutUntilMs(0L)
            _uiState.update { it.copy(errorMessage = null) }
            return true
        } else {
            val currentWrong = userPreferences.parentalWrongAttempts.firstOrNull() ?: 0
            val (nextAttempts, newLockout) = ParentalPinHelper.onFailedAttempt(currentWrong)
            userPreferences.setParentalWrongAttempts(nextAttempts)
            userPreferences.setParentalLockoutUntilMs(newLockout)

            if (newLockout > 0) {
                _uiState.update {
                    it.copy(
                        isLockedOut = true,
                        lockoutRemainingSec = 300L,
                        errorMessage = "Too many failed attempts. Locked for 5 minutes."
                    )
                }
            } else {
                val remaining = ParentalPinHelper.MAX_ATTEMPTS - nextAttempts
                _uiState.update {
                    it.copy(errorMessage = "Incorrect PIN. $remaining attempts remaining.")
                }
            }
            return false
        }
    }

    fun setInitialPin(pin: String) {
        if (pin.length !in 4..6 || !pin.all { it.isDigit() }) {
            _uiState.update { it.copy(errorMessage = "PIN must be between 4 and 6 digits") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val salt = ParentalPinHelper.generateSalt()
            val hash = ParentalPinHelper.hashPin(pin, salt)
            userPreferences.setParentalPin(hash, salt)
            userPreferences.setParentalControlEnabled(true)
            // Default locked categories include adult tags
            val currentLocked = userPreferences.parentalLockedCategories.firstOrNull() ?: emptySet()
            if (currentLocked.isEmpty()) {
                userPreferences.setParentalLockedCategories(setOf("Adult", "XXX", "Erotic", "18+"))
            }
            _uiState.update {
                it.copy(
                    hasPinSet = true,
                    isParentalEnabled = true,
                    successMessage = "Parental PIN created and protection enabled",
                    errorMessage = null
                )
            }
        }
    }

    fun changePin(oldPin: String, newPin: String) {
        if (newPin.length !in 4..6 || !newPin.all { it.isDigit() }) {
            _uiState.update { it.copy(errorMessage = "New PIN must be between 4 and 6 digits") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (!verifyPin(oldPin)) return@launch

            val salt = ParentalPinHelper.generateSalt()
            val hash = ParentalPinHelper.hashPin(newPin, salt)
            userPreferences.setParentalPin(hash, salt)
            _uiState.update {
                it.copy(
                    successMessage = "Parental PIN successfully changed",
                    errorMessage = null
                )
            }
        }
    }

    fun disableParentalControls(currentPin: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!verifyPin(currentPin)) return@launch

            userPreferences.clearParentalPin()
            parentalControlManager.unlockAdultContent()
            _uiState.update {
                it.copy(
                    hasPinSet = false,
                    isParentalEnabled = false,
                    successMessage = "Parental controls disabled",
                    errorMessage = null
                )
            }
        }
    }

    fun resetPinViaDeviceCredential() {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.clearParentalPin()
            _uiState.update {
                it.copy(
                    hasPinSet = false,
                    isParentalEnabled = false,
                    isLockedOut = false,
                    lockoutRemainingSec = 0L,
                    wrongAttempts = 0,
                    successMessage = "PIN reset via device screen lock",
                    errorMessage = null
                )
            }
        }
    }

    fun toggleCategoryLock(category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userPreferences.parentalLockedCategories.firstOrNull() ?: emptySet()
            val updated = if (current.contains(category)) {
                current - category
            } else {
                current + category
            }
            userPreferences.setParentalLockedCategories(updated)
        }
    }

    fun toggleParentalEnabled(enabled: Boolean, pin: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!verifyPin(pin)) return@launch
            userPreferences.setParentalControlEnabled(enabled)
            if (!enabled) {
                parentalControlManager.unlockAdultContent()
            } else {
                parentalControlManager.lockAdultContent()
            }
            _uiState.update {
                it.copy(
                    isParentalEnabled = enabled,
                    successMessage = if (enabled) "Parental controls enabled" else "Parental controls disabled",
                    errorMessage = null
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
