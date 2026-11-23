package com.nis.nistv.data.repository

import com.nis.nistv.core.database.NisTvDatabase
import com.nis.nistv.core.database.dao.MediaInteractionDao
import com.nis.nistv.core.database.dao.ProfileDao
import com.nis.nistv.core.database.dao.ProgramReminderDao
import com.nis.nistv.core.database.dao.WatchLaterDao
import com.nis.nistv.core.database.entity.ProfileEntity
import com.nis.nistv.playback.PlayerController
import com.nis.nistv.core.playback.SleepTimerManager
import com.nis.nistv.core.preferences.UserPreferences
import com.nis.nistv.core.security.ParentalPinHelper
import com.nis.nistv.data.parental.ParentalControlManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface ProfileRepository {
    val activeProfile: StateFlow<ProfileEntity>
    val activeProfileId: StateFlow<String>
    val profiles: Flow<List<ProfileEntity>>

    suspend fun getProfile(id: String): ProfileEntity?
    suspend fun createProfile(
        name: String,
        avatarId: String = "avatar_01",
        preferredLanguage: String = "Hindi",
        preferredCountry: String = "IN",
        isKids: Boolean = false,
        isLocked: Boolean = false
    ): Result<ProfileEntity>

    suspend fun updateProfile(profile: ProfileEntity): Result<Unit>
    suspend fun deleteProfile(profileId: String): Result<Unit>
    suspend fun switchProfile(profileId: String): Result<Unit>
    suspend fun canLeaveProfile(pin: String? = null): Boolean
    suspend fun setHistoryPaused(profileId: String, paused: Boolean)
    suspend fun bootstrapProfilesIfNeeded()
}

class DefaultProfileRepository(
    private val profileDao: ProfileDao,
    private val mediaInteractionDao: MediaInteractionDao? = null,
    private val programReminderDao: ProgramReminderDao? = null,
    private val watchLaterDao: WatchLaterDao? = null,
    private val activeProfileIdFlow: Flow<String>,
    private val onSetActiveProfileId: suspend (String) -> Unit,
    private val parentalPinHashFlow: Flow<String>,
    private val parentalPinSaltFlow: Flow<String>,
    private val preferredLanguageFlow: Flow<String>,
    private val preferredCountryFlow: Flow<String>,
    private val watchHistoryPausedFlow: Flow<Boolean>,
    private val isBootstrapDoneFlow: Flow<Boolean>,
    private val onSetBootstrapDone: suspend (Boolean) -> Unit,
    private val playerControllerProvider: () -> PlayerController? = { null },
    private val sleepTimerManagerProvider: () -> SleepTimerManager? = { null },
    private val parentalControlManagerProvider: () -> ParentalControlManager? = { null },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : ProfileRepository {

    constructor(
        database: NisTvDatabase,
        userPreferences: UserPreferences,
        playerControllerProvider: () -> PlayerController?,
        sleepTimerManagerProvider: () -> SleepTimerManager?,
        parentalControlManagerProvider: () -> ParentalControlManager?,
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    ) : this(
        profileDao = database.profileDao(),
        mediaInteractionDao = database.mediaInteractionDao(),
        programReminderDao = database.programReminderDao(),
        watchLaterDao = database.watchLaterDao(),
        activeProfileIdFlow = userPreferences.activeProfileId,
        onSetActiveProfileId = { userPreferences.setActiveProfileId(it) },
        parentalPinHashFlow = userPreferences.parentalPinHash,
        parentalPinSaltFlow = userPreferences.parentalPinSalt,
        preferredLanguageFlow = userPreferences.preferredLanguage,
        preferredCountryFlow = userPreferences.preferredCountry,
        watchHistoryPausedFlow = userPreferences.isWatchHistoryPaused,
        isBootstrapDoneFlow = userPreferences.isProfilesBootstrapDone,
        onSetBootstrapDone = { userPreferences.setProfilesBootstrapDone(it) },
        playerControllerProvider = playerControllerProvider,
        sleepTimerManagerProvider = sleepTimerManagerProvider,
        parentalControlManagerProvider = parentalControlManagerProvider,
        scope = scope
    )

    private val defaultFallbackProfile = ProfileEntity(
        id = "default",
        name = "Me",
        avatarId = "avatar_01",
        preferredLanguage = "Hindi",
        preferredCountry = "IN"
    )

    private val _activeProfileId = MutableStateFlow(
        (activeProfileIdFlow as? StateFlow<String>)?.value ?: "default"
    )
    override val activeProfileId: StateFlow<String> = _activeProfileId.asStateFlow()

    private val _activeProfile = MutableStateFlow(defaultFallbackProfile)
    override val activeProfile: StateFlow<ProfileEntity> = _activeProfile.asStateFlow()

    override val profiles: Flow<List<ProfileEntity>> = profileDao.getAllProfiles()

    init {
        scope.launch {
            // Keep active profile synced with activeProfileId preference
            activeProfileIdFlow.distinctUntilChanged().collect { id ->
                _activeProfileId.value = id
                val entity = profileDao.getProfileById(id) ?: profileDao.getProfileById("default") ?: defaultFallbackProfile
                _activeProfile.value = entity
            }
        }

        scope.launch {
            bootstrapProfilesIfNeeded()
        }
    }

    override suspend fun getProfile(id: String): ProfileEntity? = withContext(Dispatchers.IO) {
        profileDao.getProfileById(id)
    }

    override suspend fun createProfile(
        name: String,
        avatarId: String,
        preferredLanguage: String,
        preferredCountry: String,
        isKids: Boolean,
        isLocked: Boolean
    ): Result<ProfileEntity> = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Profile name cannot be blank"))
        }
        if (trimmed.length > 16) {
            return@withContext Result.failure(IllegalArgumentException("Profile name must be at most 16 characters"))
        }

        val allProfiles = profileDao.getAllProfilesSync()
        if (allProfiles.size >= 5) {
            return@withContext Result.failure(IllegalStateException("Maximum of 5 profiles reached"))
        }

        if (allProfiles.any { it.name.equals(trimmed, ignoreCase = true) }) {
            return@withContext Result.failure(IllegalArgumentException("A profile with this name already exists"))
        }

        val newId = "profile_${System.currentTimeMillis()}"
        val newProfile = ProfileEntity(
            id = newId,
            name = trimmed,
            avatarId = avatarId,
            preferredLanguage = preferredLanguage,
            preferredCountry = preferredCountry,
            isKids = isKids,
            isLocked = isLocked,
            historyPaused = false,
            sortOrder = allProfiles.size,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        profileDao.insertProfile(newProfile)
        Result.success(newProfile)
    }

    override suspend fun updateProfile(profile: ProfileEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmed = profile.name.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Profile name cannot be blank"))
        }
        if (trimmed.length > 16) {
            return@withContext Result.failure(IllegalArgumentException("Profile name must be at most 16 characters"))
        }

        val allProfiles = profileDao.getAllProfilesSync()
        if (allProfiles.any { it.id != profile.id && it.name.equals(trimmed, ignoreCase = true) }) {
            return@withContext Result.failure(IllegalArgumentException("A profile with this name already exists"))
        }

        val updated = profile.copy(name = trimmed, updatedAt = System.currentTimeMillis())
        profileDao.updateProfile(updated)

        if (_activeProfileId.value == updated.id) {
            _activeProfile.value = updated
        }
        Result.success(Unit)
    }

    override suspend fun deleteProfile(profileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val allProfiles = profileDao.getAllProfilesSync()
        if (allProfiles.size <= 1) {
            return@withContext Result.failure(IllegalStateException("Cannot delete the last remaining profile"))
        }

        val target = allProfiles.find { it.id == profileId }
            ?: return@withContext Result.failure(IllegalArgumentException("Profile not found"))

        // If deleting active profile, switch to another first
        if (_activeProfileId.value == profileId) {
            val nextProfile = allProfiles.firstOrNull { it.id != profileId } ?: defaultFallbackProfile
            switchProfile(nextProfile.id)
        }

        // Clean up media interactions and reminders (which don't have DB FK cascade)
        mediaInteractionDao?.clearForProfile(profileId)
        programReminderDao?.clearRemindersForProfile(profileId)

        // Delete profile (DB foreign keys cascade favorites, watch_history, recent_searches, watch_later, playback_progress)
        profileDao.deleteProfile(profileId)

        Result.success(Unit)
    }

    override suspend fun switchProfile(profileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val target = profileDao.getProfileById(profileId)
            ?: return@withContext Result.failure(IllegalArgumentException("Profile not found"))

        // 1. Stop any active playback & timers
        try {
            playerControllerProvider()?.stop()
        } catch (_: Exception) {}

        try {
            sleepTimerManagerProvider()?.cancelTimer()
        } catch (_: Exception) {}

        // 2. Clear adult unlock session
        try {
            parentalControlManagerProvider()?.lockAdultContent()
        } catch (_: Exception) {}

        // 3. Update active profile state and persistence
        _activeProfileId.value = target.id
        _activeProfile.value = target
        onSetActiveProfileId(target.id)

        Result.success(Unit)
    }

    override suspend fun canLeaveProfile(pin: String?): Boolean = withContext(Dispatchers.IO) {
        val current = _activeProfile.value
        if (!current.isKids) return@withContext true

        val storedHash = parentalPinHashFlow.firstOrNull() ?: ""
        val storedSalt = parentalPinSaltFlow.firstOrNull() ?: ""
        if (storedHash.isBlank() || storedSalt.isBlank()) {
            // No PIN is set; allow leaving
            return@withContext true
        }

        if (pin.isNullOrBlank()) return@withContext false
        ParentalPinHelper.verifyPin(pin, storedHash, storedSalt)
    }

    override suspend fun setHistoryPaused(profileId: String, paused: Boolean) = withContext(Dispatchers.IO) {
        profileDao.setHistoryPaused(profileId, paused)
        if (_activeProfileId.value == profileId) {
            _activeProfile.value = _activeProfile.value.copy(historyPaused = paused)
        }
    }

    override suspend fun bootstrapProfilesIfNeeded() = withContext(Dispatchers.IO) {
        val isDone = isBootstrapDoneFlow.firstOrNull() ?: false
        if (isDone) return@withContext

        try {
            val storedActiveId = activeProfileIdFlow.firstOrNull() ?: "default"
            val storedLang = preferredLanguageFlow.firstOrNull() ?: "Hindi"
            val storedCountry = preferredCountryFlow.firstOrNull() ?: "IN"
            val storedHistoryPaused = watchHistoryPausedFlow.firstOrNull() ?: false

            val existingActive = profileDao.getProfileById(storedActiveId)
            if (existingActive == null) {
                val newActive = ProfileEntity(
                    id = storedActiveId,
                    name = if (storedActiveId == "default") "Me" else "Primary",
                    avatarId = "avatar_01",
                    preferredLanguage = storedLang,
                    preferredCountry = storedCountry,
                    isKids = false,
                    isLocked = false,
                    historyPaused = storedHistoryPaused,
                    sortOrder = 0,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                profileDao.insertProfile(newActive)
            } else {
                profileDao.updateProfile(
                    existingActive.copy(
                        preferredLanguage = storedLang,
                        preferredCountry = storedCountry,
                        historyPaused = storedHistoryPaused,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            // If active profile is non-default, migrate any watch later items from 'default'
            if (storedActiveId != "default" && watchLaterDao != null) {
                val defaultWatchLater = watchLaterDao.getAllWatchLaterSync("default")
                val activeWatchLater = watchLaterDao.getAllWatchLaterSync(storedActiveId)
                if (defaultWatchLater.isNotEmpty() && activeWatchLater.isEmpty()) {
                    watchLaterDao.migrateWatchLaterProfile("default", storedActiveId)
                }
            }

            onSetBootstrapDone(true)
        } catch (_: Exception) {}
    }
}
