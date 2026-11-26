package com.nis.nistv.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nis.nistv.core.security.SecureTokenStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nistv_preferences")

data class AfdsUserInfo(
    val email: String,
    val userId: String,
    val channelId: String,
    val token: String,
    val displayName: String,
    val avatarUrl: String
)

data class GoogleUserInfo(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String = "",
    val idToken: String = "",
    val isDemo: Boolean = false,
    val demoExpiresAt: Long = 0L,
    val givenName: String = "",
    val familyName: String = ""
) {
    val displayName: String
        get() {
            val fullFromParts = listOfNotNull(
                givenName.trim().takeIf { it.isNotBlank() },
                familyName.trim().takeIf { it.isNotBlank() }
            ).joinToString(" ").trim()
            if (fullFromParts.isNotBlank()) return fullFromParts

            val cleanName = name.trim()
            if (cleanName.isNotBlank() && !cleanName.contains("@") && !cleanName.equals(email.trim(), ignoreCase = true)) {
                return cleanName
            }

            if (email.isNotBlank()) {
                val prefix = email.substringBefore("@")
                val parts = prefix.split(".", "_", "-").filter { it.isNotBlank() }
                if (parts.isNotEmpty()) {
                    return parts.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                }
            }
            return if (cleanName.isNotBlank()) cleanName else "Google User"
        }
}

data class DailyLimitCheck(
    val isExceeded: Boolean,
    val limitGb: Int,
    val usedBytes: Long,
    val limitBytes: Long,
    val additionalBytes: Long
)

data class AfdsLimitCheck(
    val isExceeded: Boolean,
    val used: Int,
    val limit: Int
)

enum class AppThemeConfig {
    SYSTEM,
    DARK,
    LIGHT
}

class UserPreferences(
    private val context: Context,
    val secureTokenStore: SecureTokenStore = SecureTokenStore(context)
) {

    private val dataStore = context.dataStore

    companion object {
        val KEY_APP_THEME = stringPreferencesKey("app_theme")
        val KEY_ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
        val KEY_PROFILES_BOOTSTRAP_V11_DONE = booleanPreferencesKey("profiles_bootstrap_v11_done")
        val KEY_ASK_PROFILE_ON_LAUNCH = booleanPreferencesKey("ask_profile_on_launch")
        val KEY_PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
        val KEY_BACKGROUND_PLAYBACK = booleanPreferencesKey("background_playback_enabled")
        val KEY_PIP_ENABLED = booleanPreferencesKey("pip_enabled")
        val KEY_LAST_SYNC_TIME = longPreferencesKey("last_sync_timestamp")

        // Phase 3: Startup sound & privacy keys
        val KEY_STARTUP_SOUND_ENABLED = booleanPreferencesKey("startup_sound_enabled")
        val KEY_PAUSE_WATCH_HISTORY = booleanPreferencesKey("pause_watch_history")

        // Phase 3: Parental controls
        val KEY_PARENTAL_ENABLED = booleanPreferencesKey("parental_enabled")
        val KEY_PARENTAL_PIN_HASH = stringPreferencesKey("parental_pin_hash")
        val KEY_PARENTAL_PIN_SALT = stringPreferencesKey("parental_pin_salt")
        val KEY_PARENTAL_LOCKED_CATEGORIES = stringSetPreferencesKey("parental_locked_categories")
        val KEY_PARENTAL_WRONG_ATTEMPTS = intPreferencesKey("parental_wrong_attempts")
        val KEY_PARENTAL_LOCKOUT_UNTIL_MS = longPreferencesKey("parental_lockout_until_ms")

        // Google Authentication keys
        val KEY_EPISODE_RADAR_ENABLED = booleanPreferencesKey("episode_radar_enabled")
        val KEY_GOOGLE_LOGGED_IN = booleanPreferencesKey("google_logged_in")
        val KEY_GOOGLE_USER_ID = stringPreferencesKey("google_user_id")
        val KEY_GOOGLE_USER_NAME = stringPreferencesKey("google_user_name")
        val KEY_GOOGLE_USER_GIVEN_NAME = stringPreferencesKey("google_user_given_name")
        val KEY_GOOGLE_USER_FAMILY_NAME = stringPreferencesKey("google_user_family_name")
        val KEY_GOOGLE_USER_EMAIL = stringPreferencesKey("google_user_email")
        val KEY_GOOGLE_USER_AVATAR = stringPreferencesKey("google_user_avatar")
        val KEY_GOOGLE_ID_TOKEN = stringPreferencesKey("google_id_token")
        val KEY_IS_DEMO = booleanPreferencesKey("is_demo")
        val KEY_DEMO_EXPIRES_AT = longPreferencesKey("demo_expires_at")
        val KEY_DEMO_USED = booleanPreferencesKey("demo_used")
        val KEY_HAS_SELECTED_LANGUAGE = booleanPreferencesKey("has_selected_language")
        val KEY_NSFW_ENABLED = booleanPreferencesKey("nsfw_enabled")
        val KEY_PREFERRED_COUNTRY = stringPreferencesKey("preferred_country")
        val KEY_TMDB_API_KEY_OVERRIDE = stringPreferencesKey("tmdb_api_key_override")
        val KEY_WIFI_ONLY_DOWNLOADS = booleanPreferencesKey("wifi_only_downloads")
        val KEY_DOWNLOAD_THREADS = intPreferencesKey("download_threads")
        val KEY_DAILY_DOWNLOAD_LIMIT_GB = intPreferencesKey("daily_download_limit_gb")
        val KEY_TODAY_DOWNLOAD_DATE = stringPreferencesKey("today_download_date")
        val KEY_TODAY_DOWNLOAD_BYTES = longPreferencesKey("today_download_bytes")
        val KEY_AFDS_AUTH_TOKEN = stringPreferencesKey("afds_auth_token")
        val KEY_AFDS_USER_EMAIL = stringPreferencesKey("afds_user_email")
        val KEY_AFDS_USER_ID = stringPreferencesKey("afds_user_id")
        val KEY_AFDS_CHANNEL_ID = stringPreferencesKey("afds_channel_id")
        val KEY_AFDS_DAILY_LINKS_USED = intPreferencesKey("afds_daily_links_used")
        val KEY_AFDS_DAILY_LINKS_LIMIT = intPreferencesKey("afds_daily_links_limit")
        val KEY_AFDS_DAILY_SENDS_USED = intPreferencesKey("afds_daily_sends_used")
        val KEY_AFDS_DAILY_SENDS_LIMIT = intPreferencesKey("afds_daily_sends_limit")
        val KEY_AFDS_LAST_QUOTA_SYNC = longPreferencesKey("afds_last_quota_sync")
        val KEY_DOWNLOADS_REPAIR_V6_DONE = booleanPreferencesKey("downloads_repair_v6_done")
        val KEY_AUTOPLAY_NEXT_EPISODE = booleanPreferencesKey("autoplay_next_episode")
        val KEY_PLAYER_ASPECT_RATIO = intPreferencesKey("player_aspect_ratio")

        // Subtitle appearance preferences
        val KEY_SUBTITLE_TEXT_SIZE = stringPreferencesKey("subtitle_text_size")
        val KEY_SUBTITLE_TEXT_COLOR = stringPreferencesKey("subtitle_text_color")

        // Phase 5: Stream Health Intelligence
        val KEY_STREAM_RELIABILITY_DOT_ENABLED = booleanPreferencesKey("stream_reliability_dot_enabled")
        val KEY_HIDE_DEAD_CHANNELS = booleanPreferencesKey("hide_dead_channels")

        // Phase 5: Data Saver suite
        val KEY_AUTO_LISTEN_CELLULAR_SCREEN_OFF = booleanPreferencesKey("auto_listen_cellular_screen_off")
        val KEY_CELLULAR_QUALITY_CAP = stringPreferencesKey("cellular_quality_cap")
        val KEY_MONTHLY_CELLULAR_LIMIT_GB = intPreferencesKey("monthly_cellular_limit_gb")
        val KEY_BLOCK_ON_CELLULAR_LIMIT = booleanPreferencesKey("block_on_cellular_limit")
        val KEY_SUBTITLE_BG_COLOR = stringPreferencesKey("subtitle_bg_color")
        val KEY_SUBTITLE_EDGE_STYLE = stringPreferencesKey("subtitle_edge_style")
        val KEY_SUBTITLE_FONT_STYLE = stringPreferencesKey("subtitle_font_style")
        val KEY_SUBTITLE_CUSTOM_PREVIEW = stringPreferencesKey("subtitle_custom_preview")
    }

    val subtitleStylePreferences: Flow<SubtitleStylePreferences> = dataStore.data.map { prefs ->
        SubtitleStylePreferences(
            textSize = prefs[KEY_SUBTITLE_TEXT_SIZE]?.let { runCatching { SubtitleTextSize.valueOf(it) }.getOrNull() } ?: SubtitleTextSize.NORMAL,
            textColor = prefs[KEY_SUBTITLE_TEXT_COLOR]?.let { runCatching { SubtitleColor.valueOf(it) }.getOrNull() } ?: SubtitleColor.WHITE,
            backgroundColor = prefs[KEY_SUBTITLE_BG_COLOR]?.let { runCatching { SubtitleBackground.valueOf(it) }.getOrNull() } ?: SubtitleBackground.SEMI_TRANSPARENT,
            edgeStyle = prefs[KEY_SUBTITLE_EDGE_STYLE]?.let { runCatching { SubtitleEdgeStyle.valueOf(it) }.getOrNull() } ?: SubtitleEdgeStyle.OUTLINE,
            fontStyle = prefs[KEY_SUBTITLE_FONT_STYLE]?.let { runCatching { SubtitleFontStyle.valueOf(it) }.getOrNull() } ?: SubtitleFontStyle.SANS_SERIF,
            customPreviewText = prefs[KEY_SUBTITLE_CUSTOM_PREVIEW] ?: "Here is how your subtitles will look during playback"
        )
    }

    suspend fun saveSubtitleStylePreferences(style: SubtitleStylePreferences) {
        dataStore.edit { prefs ->
            prefs[KEY_SUBTITLE_TEXT_SIZE] = style.textSize.name
            prefs[KEY_SUBTITLE_TEXT_COLOR] = style.textColor.name
            prefs[KEY_SUBTITLE_BG_COLOR] = style.backgroundColor.name
            prefs[KEY_SUBTITLE_EDGE_STYLE] = style.edgeStyle.name
            prefs[KEY_SUBTITLE_FONT_STYLE] = style.fontStyle.name
            prefs[KEY_SUBTITLE_CUSTOM_PREVIEW] = style.customPreviewText
        }
    }

    val isDownloadsRepairV6Done: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_DOWNLOADS_REPAIR_V6_DONE] ?: false
    }

    suspend fun setDownloadsRepairV6Done(done: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_DOWNLOADS_REPAIR_V6_DONE] = done
        }
    }

    val dailyDownloadLimitGb: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_DAILY_DOWNLOAD_LIMIT_GB] ?: 0 // 0 = Unlimited
    }

    suspend fun setDailyDownloadLimitGb(limitGb: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_DAILY_DOWNLOAD_LIMIT_GB] = limitGb
        }
    }

    private fun getTodayDateString(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return sdf.format(java.util.Date())
    }

    val todayDownloadedBytes: Flow<Long> = dataStore.data.map { prefs ->
        val today = getTodayDateString()
        val savedDate = prefs[KEY_TODAY_DOWNLOAD_DATE] ?: ""
        if (savedDate == today) {
            prefs[KEY_TODAY_DOWNLOAD_BYTES] ?: 0L
        } else {
            0L
        }
    }

    suspend fun recordDownloadedBytes(bytes: Long) {
        if (bytes <= 0) return
        val today = getTodayDateString()
        dataStore.edit { prefs ->
            val savedDate = prefs[KEY_TODAY_DOWNLOAD_DATE] ?: ""
            val current = if (savedDate == today) prefs[KEY_TODAY_DOWNLOAD_BYTES] ?: 0L else 0L
            prefs[KEY_TODAY_DOWNLOAD_DATE] = today
            prefs[KEY_TODAY_DOWNLOAD_BYTES] = current + bytes
        }
    }

    suspend fun resetTodayDownloadUsage() {
        val today = getTodayDateString()
        dataStore.edit { prefs ->
            prefs[KEY_TODAY_DOWNLOAD_DATE] = today
            prefs[KEY_TODAY_DOWNLOAD_BYTES] = 0L
        }
    }

    suspend fun checkDailyLimitExceeded(additionalBytes: Long = 0L): DailyLimitCheck {
        val limitGb = dailyDownloadLimitGb.firstOrNull() ?: 0
        if (limitGb <= 0) {
            return DailyLimitCheck(
                isExceeded = false,
                limitGb = 0,
                usedBytes = 0L,
                limitBytes = 0L,
                additionalBytes = additionalBytes
            )
        }
        val todayBytes = todayDownloadedBytes.firstOrNull() ?: 0L
        val limitBytes = limitGb.toLong() * 1024L * 1024L * 1024L
        val isExceeded = (todayBytes + additionalBytes) > limitBytes
        return DailyLimitCheck(
            isExceeded = isExceeded,
            limitGb = limitGb,
            usedBytes = todayBytes,
            limitBytes = limitBytes,
            additionalBytes = additionalBytes
        )
    }

    val autoplayNextEpisode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_AUTOPLAY_NEXT_EPISODE] ?: true
    }

    suspend fun setAutoplayNextEpisode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTOPLAY_NEXT_EPISODE] = enabled
        }
    }

    val playerAspectRatio: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_PLAYER_ASPECT_RATIO] ?: 0 // 0 = RESIZE_MODE_FIT
    }

    suspend fun setPlayerAspectRatio(mode: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_PLAYER_ASPECT_RATIO] = mode
        }
    }

    val afdsUserEmail: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_AFDS_USER_EMAIL] ?: ""
    }

    val afdsUserId: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_AFDS_USER_ID] ?: ""
    }

    val afdsChannelId: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_AFDS_CHANNEL_ID] ?: ""
    }

    suspend fun saveAfdsSession(token: String, email: String = "", userId: String = "", channelId: String = "") {
        secureTokenStore.saveAfdsToken(token)
        dataStore.edit { prefs ->
            prefs[KEY_AFDS_AUTH_TOKEN] = token
            if (email.isNotBlank()) prefs[KEY_AFDS_USER_EMAIL] = email
            if (userId.isNotBlank()) prefs[KEY_AFDS_USER_ID] = userId
            if (channelId.isNotBlank()) prefs[KEY_AFDS_CHANNEL_ID] = channelId

            // Unify with app user profile
            val effectiveEmail = if (email.isNotBlank()) email else prefs[KEY_AFDS_USER_EMAIL] ?: ""
            val nameParts = effectiveEmail.substringBefore("@").split(".", "_", "-").filter { it.isNotBlank() }
            val resolvedName = if (nameParts.isNotEmpty()) {
                nameParts.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            } else if (userId.isNotBlank()) {
                "AFDS User #$userId"
            } else {
                "AFDS Member"
            }
            val encodedName = try { java.net.URLEncoder.encode(resolvedName, "UTF-8") } catch (_: Exception) { "User" }
            val avatar = "https://ui-avatars.com/api/?name=$encodedName&background=E50914&color=ffffff&size=256&bold=true&rounded=true"

            prefs[KEY_GOOGLE_LOGGED_IN] = true
            prefs[KEY_GOOGLE_USER_ID] = if (userId.isNotBlank()) userId else "afds_${token.hashCode()}"
            prefs[KEY_GOOGLE_USER_NAME] = resolvedName
            prefs[KEY_GOOGLE_USER_EMAIL] = effectiveEmail
            prefs[KEY_GOOGLE_USER_AVATAR] = avatar
            prefs[KEY_IS_DEMO] = false
        }
    }

    suspend fun clearAfdsSession() {
        secureTokenStore.clearAllTokens()
        dataStore.edit { prefs ->
            prefs.remove(KEY_AFDS_AUTH_TOKEN)
            prefs.remove(KEY_AFDS_USER_EMAIL)
            prefs.remove(KEY_AFDS_USER_ID)
            prefs.remove(KEY_AFDS_CHANNEL_ID)
            prefs.remove(KEY_AFDS_DAILY_LINKS_USED)
            prefs.remove(KEY_AFDS_DAILY_LINKS_LIMIT)
            prefs.remove(KEY_AFDS_DAILY_SENDS_USED)
            prefs.remove(KEY_AFDS_DAILY_SENDS_LIMIT)
            prefs.remove(KEY_AFDS_LAST_QUOTA_SYNC)

            // App-level logout
            prefs[KEY_GOOGLE_LOGGED_IN] = false
            prefs.remove(KEY_GOOGLE_USER_ID)
            prefs.remove(KEY_GOOGLE_USER_NAME)
            prefs.remove(KEY_GOOGLE_USER_GIVEN_NAME)
            prefs.remove(KEY_GOOGLE_USER_FAMILY_NAME)
            prefs.remove(KEY_GOOGLE_USER_EMAIL)
            prefs.remove(KEY_GOOGLE_USER_AVATAR)
            prefs.remove(KEY_GOOGLE_ID_TOKEN)
            prefs.remove(KEY_IS_DEMO)
            prefs.remove(KEY_DEMO_EXPIRES_AT)
        }
    }

    val isAfdsLoggedIn: Flow<Boolean> = dataStore.data.map { prefs ->
        val secureToken = secureTokenStore.getAfdsToken()
        if (secureToken.isNotBlank()) return@map true
        val token = prefs[KEY_AFDS_AUTH_TOKEN]
        if (!token.isNullOrBlank()) {
            secureTokenStore.saveAfdsToken(token)
            true
        } else {
            false
        }
    }

    val isAppLoggedIn: Flow<Boolean> = isAfdsLoggedIn

    val afdsUser: Flow<AfdsUserInfo?> = dataStore.data.map { prefs ->
        val secureToken = secureTokenStore.getAfdsToken()
        val token = if (secureToken.isNotBlank()) secureToken else prefs[KEY_AFDS_AUTH_TOKEN]
        if (token.isNullOrBlank()) return@map null
        val email = prefs[KEY_AFDS_USER_EMAIL] ?: ""
        val userId = prefs[KEY_AFDS_USER_ID] ?: ""
        val channelId = prefs[KEY_AFDS_CHANNEL_ID] ?: ""

        val parts = email.substringBefore("@").split(".", "_", "-").filter { it.isNotBlank() }
        val displayName = if (parts.isNotEmpty()) {
            parts.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        } else if (userId.isNotBlank()) {
            "AFDS User #$userId"
        } else {
            "AFDS Member"
        }
        val encodedName = try { java.net.URLEncoder.encode(displayName, "UTF-8") } catch (_: Exception) { "User" }
        val avatar = "https://ui-avatars.com/api/?name=$encodedName&background=E50914&color=ffffff&size=256&bold=true&rounded=true"

        AfdsUserInfo(
            email = email,
            userId = userId,
            channelId = channelId,
            token = token,
            displayName = displayName,
            avatarUrl = avatar
        )
    }

    val afdsDailyLinksUsed: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_AFDS_DAILY_LINKS_USED] ?: 0
    }

    val afdsDailyLinksLimit: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_AFDS_DAILY_LINKS_LIMIT] ?: 100
    }

    val afdsDailySendsUsed: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_AFDS_DAILY_SENDS_USED] ?: 0
    }

    val afdsDailySendsLimit: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_AFDS_DAILY_SENDS_LIMIT] ?: 50
    }

    val afdsLastQuotaSync: Flow<Long> = dataStore.data.map { prefs ->
        prefs[KEY_AFDS_LAST_QUOTA_SYNC] ?: 0L
    }

    suspend fun saveAfdsQuota(linksUsed: Int, linksLimit: Int, sendsUsed: Int = 0, sendsLimit: Int = 50) {
        dataStore.edit { prefs ->
            prefs[KEY_AFDS_DAILY_LINKS_USED] = linksUsed
            prefs[KEY_AFDS_DAILY_LINKS_LIMIT] = linksLimit
            prefs[KEY_AFDS_DAILY_SENDS_USED] = sendsUsed
            prefs[KEY_AFDS_DAILY_SENDS_LIMIT] = sendsLimit
            prefs[KEY_AFDS_LAST_QUOTA_SYNC] = System.currentTimeMillis()
        }
    }

    suspend fun incrementAfdsLinksUsed() {
        dataStore.edit { prefs ->
            val current = prefs[KEY_AFDS_DAILY_LINKS_USED] ?: 0
            prefs[KEY_AFDS_DAILY_LINKS_USED] = current + 1
        }
    }

    suspend fun checkAfdsLimitExceeded(): AfdsLimitCheck {
        val used = afdsDailyLinksUsed.firstOrNull() ?: 0
        val limit = afdsDailyLinksLimit.firstOrNull() ?: 100
        val isExceeded = limit in 1..used
        return AfdsLimitCheck(
            isExceeded = isExceeded,
            used = used,
            limit = limit
        )
    }

    val tmdbApiKeyOverride: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_TMDB_API_KEY_OVERRIDE] ?: ""
    }

    suspend fun setTmdbApiKeyOverride(key: String) {
        dataStore.edit { prefs ->
            prefs[KEY_TMDB_API_KEY_OVERRIDE] = key
        }
    }

    val isWifiOnlyDownloads: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_WIFI_ONLY_DOWNLOADS] ?: false
    }

    suspend fun setWifiOnlyDownloads(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_WIFI_ONLY_DOWNLOADS] = enabled
        }
    }

    val downloadThreads: Flow<Int> = dataStore.data.map { prefs ->
        (prefs[KEY_DOWNLOAD_THREADS] ?: 4).coerceIn(1, 8)
    }

    suspend fun setDownloadThreads(threads: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_DOWNLOAD_THREADS] = threads.coerceIn(1, 8)
        }
    }

    val afdsAuthToken: Flow<String> = dataStore.data.map { prefs ->
        val secure = secureTokenStore.getAfdsToken()
        if (secure.isNotBlank()) return@map secure
        val legacy = prefs[KEY_AFDS_AUTH_TOKEN] ?: ""
        if (legacy.isNotBlank()) {
            secureTokenStore.saveAfdsToken(legacy)
        }
        legacy
    }

    suspend fun setAfdsAuthToken(token: String) {
        secureTokenStore.saveAfdsToken(token)
        dataStore.edit { prefs ->
            prefs[KEY_AFDS_AUTH_TOKEN] = token
        }
    }

    // Startup sound (Always enabled)
    val isStartupSoundEnabled: Flow<Boolean> = kotlinx.coroutines.flow.flowOf(true)

    suspend fun setStartupSoundEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_STARTUP_SOUND_ENABLED] = true
        }
    }

    // Pause watch history (Phase 3 F6)
    val isWatchHistoryPaused: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_PAUSE_WATCH_HISTORY] ?: false
    }

    suspend fun setWatchHistoryPaused(paused: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_PAUSE_WATCH_HISTORY] = paused
        }
    }

    // Parental Controls (Phase 3 F7)
    val isParentalControlEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_PARENTAL_ENABLED] ?: false
    }

    suspend fun setParentalControlEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_PARENTAL_ENABLED] = enabled
        }
    }

    val parentalPinHash: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_PARENTAL_PIN_HASH] ?: ""
    }

    val parentalPinSalt: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_PARENTAL_PIN_SALT] ?: ""
    }

    suspend fun setParentalPin(hash: String, salt: String) {
        dataStore.edit { prefs ->
            prefs[KEY_PARENTAL_PIN_HASH] = hash
            prefs[KEY_PARENTAL_PIN_SALT] = salt
            prefs[KEY_PARENTAL_WRONG_ATTEMPTS] = 0
            prefs[KEY_PARENTAL_LOCKOUT_UNTIL_MS] = 0L
        }
    }

    suspend fun clearParentalPin() {
        dataStore.edit { prefs ->
            prefs[KEY_PARENTAL_ENABLED] = false
            prefs.remove(KEY_PARENTAL_PIN_HASH)
            prefs.remove(KEY_PARENTAL_PIN_SALT)
            prefs.remove(KEY_PARENTAL_WRONG_ATTEMPTS)
            prefs.remove(KEY_PARENTAL_LOCKOUT_UNTIL_MS)
        }
    }

    val parentalLockedCategories: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[KEY_PARENTAL_LOCKED_CATEGORIES] ?: emptySet()
    }

    suspend fun setParentalLockedCategories(categories: Set<String>) {
        dataStore.edit { prefs ->
            prefs[KEY_PARENTAL_LOCKED_CATEGORIES] = categories
        }
    }

    val parentalWrongAttempts: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_PARENTAL_WRONG_ATTEMPTS] ?: 0
    }

    suspend fun setParentalWrongAttempts(attempts: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_PARENTAL_WRONG_ATTEMPTS] = attempts
        }
    }

    val parentalLockoutUntilMs: Flow<Long> = dataStore.data.map { prefs ->
        prefs[KEY_PARENTAL_LOCKOUT_UNTIL_MS] ?: 0L
    }

    suspend fun setParentalLockoutUntilMs(lockoutUntilMs: Long) {
        dataStore.edit { prefs ->
            prefs[KEY_PARENTAL_LOCKOUT_UNTIL_MS] = lockoutUntilMs
        }
    }

    // Episode Radar master switch (Phase 4 U1)
    val isEpisodeRadarEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_EPISODE_RADAR_ENABLED] ?: true
    }

    suspend fun setEpisodeRadarEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_EPISODE_RADAR_ENABLED] = enabled
        }
    }

    val preferredCountry: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_PREFERRED_COUNTRY] ?: run {
            val localeCountry = java.util.Locale.getDefault().country
            if (!localeCountry.isNullOrBlank()) localeCountry else "IN"
        }
    }

    suspend fun setPreferredCountry(country: String) {
        dataStore.edit { prefs ->
            prefs[KEY_PREFERRED_COUNTRY] = country
        }
    }

    val isNsfwEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_NSFW_ENABLED] ?: false
    }

    suspend fun setNsfwEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_NSFW_ENABLED] = enabled
        }
    }

    val activeProfileId: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_PROFILE_ID] ?: "default"
    }

    val preferredLanguage: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_PREFERRED_LANGUAGE] ?: "Hindi"
    }

    val isBackgroundPlaybackEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_BACKGROUND_PLAYBACK] ?: true
    }

    val isPipEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_PIP_ENABLED] ?: true
    }

    val lastSyncTimestamp: Flow<Long> = dataStore.data.map { prefs ->
        prefs[KEY_LAST_SYNC_TIME] ?: 0L
    }

    val googleUser: Flow<GoogleUserInfo?> = dataStore.data.map { prefs ->
        val afdsToken = prefs[KEY_AFDS_AUTH_TOKEN]
        if (afdsToken.isNullOrBlank()) {
            return@map null
        }
        val email = prefs[KEY_AFDS_USER_EMAIL] ?: prefs[KEY_GOOGLE_USER_EMAIL] ?: ""
        val userId = prefs[KEY_AFDS_USER_ID] ?: prefs[KEY_GOOGLE_USER_ID] ?: ""
        val storedName = prefs[KEY_GOOGLE_USER_NAME] ?: ""

        val resolvedName = when {
            storedName.isNotBlank() && !storedName.contains("@") -> storedName
            email.isNotBlank() -> {
                val parts = email.substringBefore("@").split(".", "_", "-").filter { it.isNotBlank() }
                if (parts.isNotEmpty()) {
                    parts.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                } else if (userId.isNotBlank()) {
                    "AFDS User #$userId"
                } else {
                    "AFDS Member"
                }
            }
            userId.isNotBlank() -> "AFDS User #$userId"
            else -> "AFDS Member"
        }

        val encodedName = try { java.net.URLEncoder.encode(resolvedName, "UTF-8") } catch (_: Exception) { "User" }
        val avatar = prefs[KEY_GOOGLE_USER_AVATAR].let {
            if (it.isNullOrBlank() || it.contains("default-user=s96-c")) {
                "https://ui-avatars.com/api/?name=$encodedName&background=E50914&color=ffffff&size=256&bold=true&rounded=true"
            } else {
                it
            }
        }

        GoogleUserInfo(
            id = if (userId.isNotBlank()) userId else "afds_${afdsToken.hashCode()}",
            name = resolvedName,
            email = email,
            avatarUrl = avatar,
            idToken = afdsToken,
            isDemo = false,
            demoExpiresAt = 0L,
            givenName = resolvedName.substringBefore(" "),
            familyName = resolvedName.substringAfter(" ", "")
        )
    }

    val isGoogleLoggedIn: Flow<Boolean> = isAfdsLoggedIn

    val isDemoUsed: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_DEMO_USED] ?: false
    }

    val hasSelectedLanguage: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_HAS_SELECTED_LANGUAGE] ?: false
    }

    suspend fun setHasSelectedLanguage(selected: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_HAS_SELECTED_LANGUAGE] = selected
        }
    }

    suspend fun setActiveProfileId(id: String) {
        dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_PROFILE_ID] = id
        }
    }

    val isProfilesBootstrapDone: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_PROFILES_BOOTSTRAP_V11_DONE] ?: false
    }

    suspend fun setProfilesBootstrapDone(done: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_PROFILES_BOOTSTRAP_V11_DONE] = done
        }
    }

    val askProfileOnLaunch: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ASK_PROFILE_ON_LAUNCH] ?: true
    }

    suspend fun setAskProfileOnLaunch(ask: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_ASK_PROFILE_ON_LAUNCH] = ask
        }
    }

    suspend fun setPreferredLanguage(language: String) {
        dataStore.edit { prefs ->
            prefs[KEY_PREFERRED_LANGUAGE] = language
        }
    }

    suspend fun setBackgroundPlaybackEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_BACKGROUND_PLAYBACK] = enabled
        }
    }

    suspend fun setPipEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_PIP_ENABLED] = enabled
        }
    }

    suspend fun setLastSyncTimestamp(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_SYNC_TIME] = timestamp
        }
    }

    suspend fun setGoogleUser(user: GoogleUserInfo) {
        if (user.idToken.isNotBlank()) {
            secureTokenStore.saveGoogleIdToken(user.idToken)
        }
        dataStore.edit { prefs ->
            prefs[KEY_GOOGLE_LOGGED_IN] = true
            prefs[KEY_GOOGLE_USER_ID] = user.id
            prefs[KEY_GOOGLE_USER_NAME] = user.displayName
            prefs[KEY_GOOGLE_USER_GIVEN_NAME] = user.givenName
            prefs[KEY_GOOGLE_USER_FAMILY_NAME] = user.familyName
            prefs[KEY_GOOGLE_USER_EMAIL] = user.email
            prefs[KEY_GOOGLE_USER_AVATAR] = user.avatarUrl
            prefs[KEY_GOOGLE_ID_TOKEN] = user.idToken
            prefs[KEY_IS_DEMO] = user.isDemo
            prefs[KEY_DEMO_EXPIRES_AT] = user.demoExpiresAt
            if (user.isDemo) {
                prefs[KEY_DEMO_USED] = true
            }
        }
    }

    suspend fun clearGoogleUser() {
        secureTokenStore.clearGoogleIdToken()
        dataStore.edit { prefs ->
            prefs[KEY_GOOGLE_LOGGED_IN] = false
            prefs.remove(KEY_GOOGLE_USER_ID)
            prefs.remove(KEY_GOOGLE_USER_NAME)
            prefs.remove(KEY_GOOGLE_USER_GIVEN_NAME)
            prefs.remove(KEY_GOOGLE_USER_FAMILY_NAME)
            prefs.remove(KEY_GOOGLE_USER_EMAIL)
            prefs.remove(KEY_GOOGLE_USER_AVATAR)
            prefs.remove(KEY_GOOGLE_ID_TOKEN)
            prefs.remove(KEY_IS_DEMO)
            prefs.remove(KEY_DEMO_EXPIRES_AT)
        }
    }

    // Phase 5: Stream Health Intelligence
    val isStreamReliabilityDotEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_STREAM_RELIABILITY_DOT_ENABLED] ?: true
    }

    suspend fun setStreamReliabilityDotEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_STREAM_RELIABILITY_DOT_ENABLED] = enabled }
    }

    val isHideDeadChannelsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_HIDE_DEAD_CHANNELS] ?: false
    }

    suspend fun setHideDeadChannelsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_HIDE_DEAD_CHANNELS] = enabled }
    }

    // Phase 5: Data Saver suite
    val isAutoListenOnCellularScreenOffEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_AUTO_LISTEN_CELLULAR_SCREEN_OFF] ?: false
    }

    suspend fun setAutoListenOnCellularScreenOffEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_LISTEN_CELLULAR_SCREEN_OFF] = enabled }
    }

    val cellularQualityCap: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_CELLULAR_QUALITY_CAP] ?: "OFF"
    }

    suspend fun setCellularQualityCap(cap: String) {
        dataStore.edit { it[KEY_CELLULAR_QUALITY_CAP] = cap }
    }

    val monthlyCellularLimitGb: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_MONTHLY_CELLULAR_LIMIT_GB] ?: 0
    }

    suspend fun setMonthlyCellularLimitGb(limitGb: Int) {
        dataStore.edit { it[KEY_MONTHLY_CELLULAR_LIMIT_GB] = limitGb }
    }

    val blockOnCellularLimit: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_BLOCK_ON_CELLULAR_LIMIT] ?: false
    }

    suspend fun setBlockOnCellularLimit(block: Boolean) {
        dataStore.edit { it[KEY_BLOCK_ON_CELLULAR_LIMIT] = block }
    }

    val appTheme: Flow<AppThemeConfig> = dataStore.data.map { prefs ->
        prefs[KEY_APP_THEME]?.let {
            runCatching { AppThemeConfig.valueOf(it) }.getOrNull()
        } ?: AppThemeConfig.DARK
    }

    suspend fun setAppTheme(theme: AppThemeConfig) {
        dataStore.edit { it[KEY_APP_THEME] = theme.name }
    }
}
