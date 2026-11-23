package com.nis.nistv.feature.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import coil3.imageLoader
import com.nis.nistv.core.auth.GoogleAuthManager
import com.nis.nistv.core.database.NisTvDatabase
import com.nis.nistv.core.database.entity.ProfileEntity
import com.nis.nistv.core.preferences.AppThemeConfig
import com.nis.nistv.core.preferences.GoogleUserInfo
import com.nis.nistv.core.preferences.UserPreferences
import com.nis.nistv.data.download.provider.AfdsAuthService
import com.nis.nistv.data.repository.ChannelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaybackPrefs(
    val language: String,
    val bgPlayback: Boolean,
    val pip: Boolean,
    val nsfw: Boolean = false,
    val autoplayNext: Boolean = true,
    val startupSound: Boolean = true,
    val episodeRadar: Boolean = true,
    val appTheme: AppThemeConfig = AppThemeConfig.DARK
)

data class AfdsLocalState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val otpStep: Int = 0, // 0 = email, 1 = otp
    val currentEmail: String = "",
    val botId: String? = null,
    val loginType: String? = null
)

data class ProfileUiState(
    val activeProfile: ProfileEntity? = null,
    val profiles: List<ProfileEntity> = emptyList(),
    val preferredLanguage: String = "Hindi",
    val preferredCountry: String = "IN",
    val tmdbApiKeyOverride: String = "",
    val afdsAuthToken: String = "",
    val afdsUserEmail: String = "",
    val afdsUserId: String = "",
    val isAfdsAuthenticated: Boolean = false,
    val isAfdsLoading: Boolean = false,
    val afdsAuthError: String? = null,
    val afdsOtpStep: Int = 0,
    val afdsCurrentEmail: String = "",
    val afdsBotId: String? = null,
    val afdsLoginType: String? = null,
    val isBackgroundPlaybackEnabled: Boolean = true,
    val isPipEnabled: Boolean = true,
    val isAutoplayNextEpisodeEnabled: Boolean = true,
    val isStartupSoundEnabled: Boolean = true,
    val isEpisodeRadarEnabled: Boolean = true,
    val isNsfwEnabled: Boolean = false,
    val availableLanguages: List<String> = emptyList(),
    val availableCountries: List<String> = listOf("IN", "US", "GB", "CA", "AU", "DE", "FR"),
    val totalChannelsCount: Int = 0,
    val isSyncing: Boolean = false,
    val syncStatusMessage: String? = null,
    val googleUser: GoogleUserInfo? = null,
    val todayDownloadedBytes: Long = 0L,
    val dailyDownloadLimitGb: Int = 10,
    val afdsDailyLinksUsed: Int = 0,
    val afdsDailyLinksLimit: Int = 100,
    val afdsDailySendsUsed: Int = 0,
    val afdsDailySendsLimit: Int = 50,
    val afdsLastQuotaSync: Long = 0L,
    val isStreamReliabilityDotEnabled: Boolean = true,
    val isHideDeadChannelsEnabled: Boolean = false,
    val cellularQualityCap: String = "OFF",
    val isAutoListenOnCellularScreenOffEnabled: Boolean = false,
    val monthlyCellularLimitGb: Int = 0,
    val blockOnCellularLimit: Boolean = false,
    val downloadThreads: Int = 4,
    val appTheme: AppThemeConfig = AppThemeConfig.DARK
)

class ProfileViewModel(
    private val context: Context,
    private val database: NisTvDatabase,
    private val channelRepository: ChannelRepository,
    private val userPreferences: UserPreferences,
    private val googleAuthManager: GoogleAuthManager,
    private val afdsAuthService: AfdsAuthService
) : ViewModel() {

    private val profileDao = database.profileDao()
    private val channelDao = database.channelDao()

    private val _isSyncing = MutableStateFlow(false)
    private val _syncMessage = MutableStateFlow<String?>(null)
    private val _afdsLocalState = MutableStateFlow(AfdsLocalState())

    private val playbackPrefsFlow = combine(
        combine(
            userPreferences.preferredLanguage,
            userPreferences.isBackgroundPlaybackEnabled,
            userPreferences.isPipEnabled
        ) { lang, bg, pip -> Triple(lang, bg, pip) },
        combine(
            userPreferences.isNsfwEnabled,
            userPreferences.autoplayNextEpisode,
            userPreferences.isStartupSoundEnabled,
            userPreferences.isEpisodeRadarEnabled,
            userPreferences.appTheme
        ) { nsfw, autoplay, sound, radar, theme ->
            listOf(nsfw, autoplay, sound, radar, theme)
        }
    ) { (lang, bg, pip), list ->
        PlaybackPrefs(
            language = lang,
            bgPlayback = bg,
            pip = pip,
            nsfw = list[0] as Boolean,
            autoplayNext = list[1] as Boolean,
            startupSound = list[2] as Boolean,
            episodeRadar = list[3] as Boolean,
            appTheme = list[4] as AppThemeConfig
        )
    }

    private val channelsCountFlow = channelDao.getAllChannels().map { it.size }

    private val syncInfoFlow = combine(
        _isSyncing,
        _syncMessage,
        channelRepository.getSyncMetadata("CATALOGUE_FULL")
    ) { syncing, manualMsg, meta ->
        val statusMsg = when {
            syncing -> "Synchronizing catalogue with remote iptv-org..."
            manualMsg != null -> manualMsg
            meta == null -> "Never synchronized (Local seed active)"
            meta.status == "SUCCESS" -> {
                val timeStr = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(meta.lastSyncTimestamp))
                "Last successful refresh: $timeStr"
            }
            else -> "Refresh failed: ${meta.errorMessage ?: "Network error"} (Offline cache active)"
        }
        Pair(syncing, statusMsg)
    }

    private val profilesFlow = combine(
        profileDao.getAllProfiles(),
        userPreferences.activeProfileId
    ) { list, activeId ->
        Pair(list, list.find { it.id == activeId } ?: list.firstOrNull())
    }

    data class TmdbPrefs(
        val country: String,
        val tmdbKey: String,
        val afdsToken: String,
        val afdsEmail: String,
        val afdsUserId: String
    )

    private val tmdbPrefsFlow = combine(
        userPreferences.preferredCountry,
        userPreferences.tmdbApiKeyOverride,
        userPreferences.afdsAuthToken,
        userPreferences.afdsUserEmail,
        userPreferences.afdsUserId
    ) { country, key, token, email, uid ->
        TmdbPrefs(country, key, token, email, uid)
    }

    data class DownloadUsagePrefs(
        val todayBytes: Long,
        val limitGb: Int,
        val threads: Int
    )

    private val downloadUsageFlow = combine(
        userPreferences.todayDownloadedBytes,
        userPreferences.dailyDownloadLimitGb,
        userPreferences.downloadThreads
    ) { bytes, limit, threads ->
        DownloadUsagePrefs(bytes, limit, threads)
    }

    data class AfdsQuotaPrefs(
        val linksUsed: Int,
        val linksLimit: Int,
        val sendsUsed: Int,
        val sendsLimit: Int,
        val lastSync: Long
    )

    private val afdsQuotaFlow = combine(
        userPreferences.afdsDailyLinksUsed,
        userPreferences.afdsDailyLinksLimit,
        userPreferences.afdsDailySendsUsed,
        userPreferences.afdsDailySendsLimit,
        userPreferences.afdsLastQuotaSync
    ) { linksUsed, linksLimit, sendsUsed, sendsLimit, lastSync ->
        AfdsQuotaPrefs(linksUsed, linksLimit, sendsUsed, sendsLimit, lastSync)
    }

    data class Phase5Prefs(
        val reliabilityDot: Boolean,
        val hideDead: Boolean,
        val autoListen: Boolean,
        val cellularCap: String,
        val monthlyCellularLimitGb: Int,
        val blockOnCellularLimit: Boolean
    )

    private val phase5PrefsFlow = combine(
        combine(
            userPreferences.isStreamReliabilityDotEnabled,
            userPreferences.isHideDeadChannelsEnabled,
            userPreferences.isAutoListenOnCellularScreenOffEnabled
        ) { dot, hide, autoListen -> Triple(dot, hide, autoListen) },
        combine(
            userPreferences.cellularQualityCap,
            userPreferences.monthlyCellularLimitGb,
            userPreferences.blockOnCellularLimit
        ) { cap, limit, block -> Triple(cap, limit, block) }
    ) { (dot, hide, autoListen), (cap, limit, block) ->
        Phase5Prefs(dot, hide, autoListen, cap, limit, block)
    }

    private data class UserSettingsBundle(
        val googleUser: GoogleUserInfo?,
        val tmdb: TmdbPrefs,
        val afdsLocal: AfdsLocalState,
        val dlUsage: DownloadUsagePrefs,
        val afdsQuota: AfdsQuotaPrefs,
        val phase5: Phase5Prefs
    )

    val uiState: StateFlow<ProfileUiState> = combine(
        profilesFlow,
        playbackPrefsFlow,
        channelsCountFlow,
        syncInfoFlow,
        combine(
            combine(userPreferences.googleUser, tmdbPrefsFlow, _afdsLocalState) { user, tmdb, afdsLocal ->
                Triple(user, tmdb, afdsLocal)
            },
            downloadUsageFlow,
            afdsQuotaFlow,
            phase5PrefsFlow
        ) { sub, dlUsage, quota, p5 ->
            val (user, tmdb, afdsLocal) = sub
            UserSettingsBundle(user, tmdb, afdsLocal, dlUsage, quota, p5)
        }
    ) { profilesPair, prefs, count, syncInfo, bundle ->
        val profiles = profilesPair.first
        val active = profilesPair.second
        val googleUser = bundle.googleUser
        val tmdb = bundle.tmdb
        val afdsLocal = bundle.afdsLocal
        val isAfdsAuth = tmdb.afdsToken.isNotBlank()
        val quota = bundle.afdsQuota

        ProfileUiState(
            activeProfile = active,
            profiles = profiles,
            preferredLanguage = prefs.language,
            preferredCountry = tmdb.country,
            tmdbApiKeyOverride = tmdb.tmdbKey,
            afdsAuthToken = tmdb.afdsToken,
            afdsUserEmail = tmdb.afdsEmail,
            afdsUserId = tmdb.afdsUserId,
            isAfdsAuthenticated = isAfdsAuth,
            isAfdsLoading = afdsLocal.isLoading,
            afdsAuthError = afdsLocal.error,
            afdsOtpStep = afdsLocal.otpStep,
            afdsCurrentEmail = afdsLocal.currentEmail,
            afdsBotId = afdsLocal.botId,
            afdsLoginType = afdsLocal.loginType,
            isBackgroundPlaybackEnabled = prefs.bgPlayback,
            isPipEnabled = prefs.pip,
            isAutoplayNextEpisodeEnabled = prefs.autoplayNext,
            isStartupSoundEnabled = prefs.startupSound,
            isEpisodeRadarEnabled = prefs.episodeRadar,
            isNsfwEnabled = prefs.nsfw,
            availableLanguages = listOf("Hindi", "Tamil", "Telugu", "Malayalam", "Bengali", "Kannada", "Marathi", "Gujarati", "Punjabi"),
            totalChannelsCount = count,
            isSyncing = syncInfo.first,
            syncStatusMessage = syncInfo.second,
            googleUser = googleUser,
            todayDownloadedBytes = bundle.dlUsage.todayBytes,
            dailyDownloadLimitGb = bundle.dlUsage.limitGb,
            afdsDailyLinksUsed = quota.linksUsed,
            afdsDailyLinksLimit = quota.linksLimit,
            afdsDailySendsUsed = quota.sendsUsed,
            afdsDailySendsLimit = quota.sendsLimit,
            afdsLastQuotaSync = quota.lastSync,
            isStreamReliabilityDotEnabled = bundle.phase5.reliabilityDot,
            isHideDeadChannelsEnabled = bundle.phase5.hideDead,
            cellularQualityCap = bundle.phase5.cellularCap,
            isAutoListenOnCellularScreenOffEnabled = bundle.phase5.autoListen,
            monthlyCellularLimitGb = bundle.phase5.monthlyCellularLimitGb,
            blockOnCellularLimit = bundle.phase5.blockOnCellularLimit,
            downloadThreads = bundle.dlUsage.threads,
            appTheme = prefs.appTheme
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
    )

    init {
        viewModelScope.launch {
            val token = userPreferences.afdsAuthToken.firstOrNull()?.trim().orEmpty()
            if (token.isNotBlank()) {
                afdsAuthService.getProfile(token)
            }
        }
    }

    fun refreshAfdsQuota() {
        viewModelScope.launch {
            _afdsLocalState.update { it.copy(isLoading = true, error = null) }
            afdsAuthService.refreshProfile()
                .onSuccess {
                    _afdsLocalState.update { it.copy(isLoading = false) }
                }
                .onFailure { err ->
                    _afdsLocalState.update { it.copy(isLoading = false, error = err.message) }
                }
        }
    }

    fun setDailyDownloadLimitGb(limitGb: Int) {
        viewModelScope.launch {
            userPreferences.setDailyDownloadLimitGb(limitGb)
        }
    }

    fun setDownloadThreads(threads: Int) {
        viewModelScope.launch {
            userPreferences.setDownloadThreads(threads)
        }
    }

    fun resetTodayDownloadUsage() {
        viewModelScope.launch {
            userPreferences.resetTodayDownloadUsage()
        }
    }

    fun setPreferredCountry(country: String) {
        viewModelScope.launch {
            userPreferences.setPreferredCountry(country)
        }
    }

    fun setTmdbApiKeyOverride(key: String) {
        viewModelScope.launch {
            userPreferences.setTmdbApiKeyOverride(key.trim())
        }
    }

    fun setAppTheme(theme: AppThemeConfig) {
        viewModelScope.launch {
            userPreferences.setAppTheme(theme)
        }
    }

    fun setAfdsAuthToken(token: String) {
        viewModelScope.launch {
            userPreferences.setAfdsAuthToken(token.trim())
        }
    }

    fun requestAfdsOtp(email: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            _afdsLocalState.update { it.copy(error = "Please enter a valid email address.") }
            return
        }
        _afdsLocalState.update { it.copy(isLoading = true, error = null, currentEmail = cleanEmail) }
        viewModelScope.launch {
            afdsAuthService.requestLoginOtp(cleanEmail)
                .onSuccess { resp ->
                    _afdsLocalState.update {
                        it.copy(
                            isLoading = false,
                            otpStep = 1,
                            botId = resp.botId,
                            loginType = resp.loginType ?: "telegram",
                            error = null
                        )
                    }
                }
                .onFailure { err ->
                    // Attempt fallback to email OTP endpoint
                    afdsAuthService.requestLoginOtpEmail(cleanEmail)
                        .onSuccess { emailResp ->
                            _afdsLocalState.update {
                                it.copy(
                                    isLoading = false,
                                    otpStep = 1,
                                    botId = null,
                                    loginType = "email",
                                    error = null
                                )
                            }
                        }
                        .onFailure { emailErr ->
                            _afdsLocalState.update {
                                it.copy(
                                    isLoading = false,
                                    error = emailErr.message ?: err.message ?: "Failed to send login code"
                                )
                            }
                        }
                }
        }
    }

    fun requestAfdsOtpEmailDirect() {
        val email = _afdsLocalState.value.currentEmail
        if (email.isBlank()) return
        _afdsLocalState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            afdsAuthService.requestLoginOtpEmail(email)
                .onSuccess {
                    _afdsLocalState.update {
                        it.copy(
                            isLoading = false,
                            otpStep = 1,
                            botId = null,
                            loginType = "email",
                            error = null
                        )
                    }
                }
                .onFailure { err ->
                    _afdsLocalState.update {
                        it.copy(
                            isLoading = false,
                            error = err.message ?: "Failed to resend code to email."
                        )
                    }
                }
        }
    }

    fun verifyAfdsOtp(code: String) {
        val email = _afdsLocalState.value.currentEmail
        val cleanCode = code.trim()
        if (cleanCode.length != 6) {
            _afdsLocalState.update { it.copy(error = "Please enter the complete 6-digit code.") }
            return
        }
        _afdsLocalState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            afdsAuthService.verifyLoginOtp(email, cleanCode)
                .onSuccess {
                    _afdsLocalState.update { AfdsLocalState() }
                }
                .onFailure { err ->
                    _afdsLocalState.update {
                        it.copy(
                            isLoading = false,
                            error = err.message ?: "Verification failed. Check the code and try again."
                        )
                    }
                }
        }
    }

    fun connectAfdsWithGoogle() {
        val user = uiState.value.googleUser
        if (user == null || user.email.isBlank()) {
            _afdsLocalState.update { it.copy(error = "No active Google sign-in detected. Please enter your email.") }
            return
        }
        _afdsLocalState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            // Use existing valid JWT idToken or refresh genuine token via GoogleAuthManager
            var tokenToUse = user.idToken
            if (tokenToUse.isBlank() || tokenToUse.startsWith("google_auth_") || tokenToUse.startsWith("verified_google_auth_")) {
                val freshToken = googleAuthManager.fetchGoogleIdToken(user.email)
                if (!freshToken.isNullOrBlank()) {
                    tokenToUse = freshToken
                    userPreferences.setGoogleUser(user.copy(idToken = freshToken))
                } else {
                    _afdsLocalState.update {
                        it.copy(
                            isLoading = false,
                            error = "Google ID token required. Please sign in via the Google Sign-In button."
                        )
                    }
                    return@launch
                }
            }

            afdsAuthService.loginWithGoogle(tokenToUse)
                .onSuccess {
                    _afdsLocalState.update { AfdsLocalState() }
                }
                .onFailure { err ->
                    _afdsLocalState.update {
                        it.copy(
                            isLoading = false,
                            error = err.message ?: "Google sign-in to AFDS failed. Try Email OTP."
                        )
                    }
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            afdsAuthService.logout()
            googleAuthManager.signOut()
            userPreferences.clearAfdsSession()
            _afdsLocalState.update { AfdsLocalState() }
            _syncMessage.value = "Signed out of NisTV & AFDS."
        }
    }

    fun disconnectAfds() {
        signOut()
    }

    fun resetAfdsAuthFlow() {
        _afdsLocalState.update { AfdsLocalState() }
    }

    fun switchProfile(profileId: String) {
        viewModelScope.launch {
            userPreferences.setActiveProfileId(profileId)
        }
    }

    fun createProfile(name: String, preferredLanguage: String = "Hindi") {
        if (name.isNotBlank()) {
            viewModelScope.launch {
                val newProfile = ProfileEntity(
                    id = "profile_${System.currentTimeMillis()}",
                    name = name.trim(),
                    preferredLanguage = preferredLanguage
                )
                profileDao.insertProfile(newProfile)
                userPreferences.setActiveProfileId(newProfile.id)
            }
        }
    }

    fun deleteProfile(profileId: String) {
        if (profileId != "default") {
            viewModelScope.launch {
                profileDao.deleteProfile(profileId)
                userPreferences.setActiveProfileId("default")
            }
        }
    }

    fun setPreferredLanguage(language: String) {
        viewModelScope.launch {
            userPreferences.setPreferredLanguage(language)
        }
    }

    fun toggleBackgroundPlayback(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setBackgroundPlaybackEnabled(enabled)
        }
    }

    fun togglePip(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setPipEnabled(enabled)
        }
    }

    fun toggleAutoplayNextEpisode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setAutoplayNextEpisode(enabled)
        }
    }

    fun toggleStartupSound(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setStartupSoundEnabled(enabled)
        }
    }

    fun setEpisodeRadarEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setEpisodeRadarEnabled(enabled)
        }
    }

    fun setStreamReliabilityDotEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setStreamReliabilityDotEnabled(enabled)
        }
    }

    fun setHideDeadChannelsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setHideDeadChannelsEnabled(enabled)
        }
    }

    fun setCellularQualityCap(cap: String) {
        viewModelScope.launch {
            userPreferences.setCellularQualityCap(cap)
        }
    }

    fun setAutoListenOnCellularScreenOffEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setAutoListenOnCellularScreenOffEnabled(enabled)
        }
    }

    fun setMonthlyCellularLimitGb(limitGb: Int) {
        viewModelScope.launch {
            userPreferences.setMonthlyCellularLimitGb(limitGb)
        }
    }

    fun setBlockOnCellularLimit(block: Boolean) {
        viewModelScope.launch {
            userPreferences.setBlockOnCellularLimit(block)
        }
    }

    fun toggleNsfw(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setNsfwEnabled(enabled)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            context.imageLoader.memoryCache?.clear()
            context.imageLoader.diskCache?.clear()
            channelRepository.clearRecentSearches()
            _syncMessage.value = "Cache cleared successfully."
        }
    }

    fun triggerCatalogueSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Starting catalogue sync via WorkManager..."
            channelRepository.triggerWorkManagerSync()
            _isSyncing.value = false
            _syncMessage.value = "Catalogue sync task enqueued"
        }
    }

    // Google Authentication methods
    fun getGoogleOAuthUrl(): String {
        return googleAuthManager.getOAuthUrl()
    }

    fun onGoogleSignInSuccess(user: GoogleUserInfo) {
        viewModelScope.launch {
            userPreferences.setGoogleUser(user)

            // Also synchronize active profile name
            val active = uiState.value.activeProfile
            if (active != null) {
                profileDao.insertProfile(active.copy(name = user.displayName))
            }

            _syncMessage.value = "Signed in as ${user.displayName}"
        }
    }

    fun signInWithGoogleToken(idToken: String) {
        viewModelScope.launch {
            _syncMessage.value = "Verifying Google account..."
            val result = googleAuthManager.verifyGoogleIdToken(idToken)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                onGoogleSignInSuccess(user)
            } else {
                _syncMessage.value = "Google login error: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun signInWithGoogleDirect(
        email: String,
        firstName: String? = null,
        lastName: String? = null
    ) {
        viewModelScope.launch {
            val user = googleAuthManager.createVerifiedGoogleUser(
                email = email,
                firstName = firstName,
                lastName = lastName
            )
            onGoogleSignInSuccess(user)
        }
    }

    fun updateUserProfile(firstName: String, lastName: String) {
        val current = uiState.value.googleUser ?: return
        val cleanFirst = firstName.trim()
        val cleanLast = lastName.trim()
        val full = listOfNotNull(cleanFirst.takeIf { it.isNotBlank() }, cleanLast.takeIf { it.isNotBlank() }).joinToString(" ")
        val updated = current.copy(
            givenName = cleanFirst,
            familyName = cleanLast,
            name = if (full.isNotBlank()) full else current.name
        )
        onGoogleSignInSuccess(updated)
    }

    fun signOutGoogle() {
        signOut()
    }

    companion object {
        fun provideFactory(
            context: Context,
            database: NisTvDatabase,
            repository: ChannelRepository,
            preferences: UserPreferences,
            googleAuthManager: GoogleAuthManager,
            afdsAuthService: AfdsAuthService
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(
                    context,
                    database,
                    repository,
                    preferences,
                    googleAuthManager,
                    afdsAuthService
                ) as T
            }
        }
    }
}
