package com.nis.nistv.data.backup

import androidx.room.withTransaction
import com.nis.nistv.core.database.NisTvDatabase
import com.nis.nistv.core.database.entity.FavoriteEntity
import com.nis.nistv.core.database.entity.PlaybackProgressEntity
import com.nis.nistv.core.database.entity.PlaylistEntity
import com.nis.nistv.core.database.entity.ProfileEntity
import com.nis.nistv.core.database.entity.ProgramReminderEntity
import com.nis.nistv.core.database.entity.RecentSearchEntity
import com.nis.nistv.core.database.entity.WatchHistoryEntity
import com.nis.nistv.core.database.entity.WatchLaterEntity
import com.nis.nistv.core.preferences.UserPreferences
import com.nis.nistv.data.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class BackupRestoreManager(
    private val database: NisTvDatabase,
    private val userPreferences: UserPreferences,
    private val appVersion: String,
    private val profileRepository: ProfileRepository? = null
) {

    suspend fun createExportJson(includeRecentSearches: Boolean = true): String = withContext(Dispatchers.IO) {
        // 1. Profiles
        val profiles = database.profileDao().getAllProfilesSync().map {
            BackupProfileItem(
                id = it.id,
                name = it.name,
                avatarId = it.avatarId,
                preferredLanguage = it.preferredLanguage,
                preferredCountry = it.preferredCountry,
                isKids = it.isKids,
                isLocked = it.isLocked,
                historyPaused = it.historyPaused,
                sortOrder = it.sortOrder,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }

        // 2. Favorites across all profiles
        val favorites = database.favoriteDao().getAllFavoritesAcrossProfilesSync().map {
            BackupFavoriteItem(
                profileId = it.profileId,
                channelId = it.channelId,
                createdAt = it.createdAt
            )
        }

        // 3. Watch Later across all profiles
        val watchLater = database.watchLaterDao().getAllWatchLaterAcrossProfilesSync().map {
            BackupWatchLaterItem(
                profileId = it.profileId,
                id = it.id,
                mediaId = it.mediaId,
                mediaType = it.mediaType,
                title = it.title,
                posterPath = it.posterPath,
                backdropPath = it.backdropPath,
                overview = it.overview,
                releaseYear = it.releaseYear,
                voteAverage = it.voteAverage,
                genres = it.genres,
                addedAt = it.addedAt
            )
        }

        // 4. Watch History across all profiles
        val watchHistory = database.watchHistoryDao().getAllWatchHistorySync().map {
            BackupWatchHistoryItem(
                profileId = it.profileId,
                channelId = it.channelId,
                lastWatchedAt = it.lastWatchedAt,
                watchDurationMs = it.watchDurationMs
            )
        }

        // 5. Playback Progress across all profiles
        val playbackProgress = database.playbackProgressDao().getAllProgressSync().map {
            BackupPlaybackProgressItem(
                profileId = it.profileId,
                downloadId = it.downloadId,
                positionMs = it.positionMs,
                durationMs = it.durationMs,
                lastWatchedAt = it.lastWatchedAt,
                completed = it.completed,
                hidden = it.hidden
            )
        }

        // 6. Program Reminders across all profiles
        val reminders = database.programReminderDao().getAllRemindersSync().map {
            BackupReminderItem(
                id = it.id,
                profileId = it.profileId,
                channelId = it.channelId,
                programTitle = it.programTitle,
                startEpochMs = it.startEpochMs,
                notified = it.notified
            )
        }

        // 7. Recent searches (active profile or default)
        val targetProfileId = profileRepository?.activeProfileId?.value ?: "default"
        val recentSearches = if (includeRecentSearches) {
            database.recentSearchDao().getAllSearchesSync(targetProfileId)
        } else {
            emptyList()
        }

        // 8. Custom Playlists
        val playlists = database.playlistDao().getAllPlaylistsSync().map {
            BackupPlaylistItem(
                id = it.id,
                name = it.name,
                sourceType = it.sourceType,
                url = it.url,
                lastSyncedAt = it.lastSyncedAt,
                channelCount = it.channelCount,
                autoRefresh = it.autoRefresh,
                enabled = it.enabled
            )
        }

        // 9. Non-sensitive Preferences (NEVER export PIN, tokens, or TMDB key)
        val prefs = BackupPreferences(
            preferredLanguage = userPreferences.preferredLanguage.firstOrNull() ?: "Hindi",
            preferredCountry = userPreferences.preferredCountry.firstOrNull() ?: "IN",
            backgroundPlayback = userPreferences.isBackgroundPlaybackEnabled.firstOrNull() ?: true,
            pipEnabled = userPreferences.isPipEnabled.firstOrNull() ?: true,
            autoplayNextEpisode = userPreferences.autoplayNextEpisode.firstOrNull() ?: true,
            playerAspectRatio = userPreferences.playerAspectRatio.firstOrNull() ?: 0,
            dailyDownloadLimitGb = userPreferences.dailyDownloadLimitGb.firstOrNull() ?: 10,
            wifiOnlyDownloads = userPreferences.isWifiOnlyDownloads.firstOrNull() ?: false,
            startupSoundEnabled = userPreferences.isStartupSoundEnabled.firstOrNull() ?: true,
            downloadThreads = userPreferences.downloadThreads.firstOrNull() ?: 4,
            askProfileOnLaunch = userPreferences.askProfileOnLaunch.firstOrNull() ?: true
        )

        // Note: Raw ephemeral media_interactions (taste data) are omitted to prevent payload bloat.
        // User taste profiles reconstruct automatically on device from watch history and favorites.

        val backupData = NisTvBackupData(
            profiles = profiles,
            favorites = favorites,
            watchLater = watchLater,
            watchHistory = watchHistory,
            playbackProgress = playbackProgress,
            reminders = reminders,
            recentSearches = recentSearches,
            playlists = playlists,
            preferences = prefs
        )

        BackupRestoreHelper.createBackupJson(appVersion, backupData)
    }

    fun parseBackupJson(jsonString: String): BackupParseResult {
        return BackupRestoreHelper.validateAndParseBackup(jsonString)
    }

    suspend fun restoreBackup(
        payload: NisTvBackupPayload,
        replaceExisting: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                val currentActiveId = profileRepository?.activeProfileId?.value ?: "default"

                if (replaceExisting) {
                    database.favoriteDao().clearAllFavorites("default")
                    database.watchLaterDao().clearAll()
                    database.recentSearchDao().clearSearches("default")
                    database.watchHistoryDao().clearWatchHistory("default")
                    database.playbackProgressDao().clearAll()
                    database.programReminderDao().cleanOldReminders(System.currentTimeMillis() + 10000000000L)
                    val existingPlaylists = database.playlistDao().getAllPlaylistsSync()
                    for (playlist in existingPlaylists) {
                        database.playlistDao().deleteChannelsForPlaylist(playlist.id)
                        database.playlistDao().deletePlaylistById(playlist.id)
                    }
                }

                // 1. Profiles (if v2 backup has profiles, restore up to 5)
                if (payload.data.profiles.isNotEmpty()) {
                    val existingProfiles = database.profileDao().getAllProfilesSync().associateBy { it.id }
                    for (p in payload.data.profiles.take(5)) {
                        val entity = ProfileEntity(
                            id = p.id,
                            name = p.name,
                            avatarId = p.avatarId,
                            preferredLanguage = p.preferredLanguage,
                            preferredCountry = p.preferredCountry,
                            isKids = p.isKids,
                            isLocked = p.isLocked,
                            historyPaused = p.historyPaused,
                            sortOrder = p.sortOrder,
                            createdAt = if (p.createdAt > 0) p.createdAt else System.currentTimeMillis(),
                            updatedAt = if (p.updatedAt > 0) p.updatedAt else System.currentTimeMillis()
                        )
                        if (existingProfiles.containsKey(p.id)) {
                            database.profileDao().updateProfile(entity)
                        } else {
                            database.profileDao().insertProfile(entity)
                        }
                    }
                }

                // Map target profile for v1 backups: map everything to current active profile
                val mapToProfile = if (payload.schemaVersion == 1) currentActiveId else null

                // 2. Favorites
                val favoriteEntities = payload.data.favorites.map {
                    FavoriteEntity(
                        profileId = mapToProfile ?: it.profileId.ifBlank { "default" },
                        channelId = it.channelId,
                        createdAt = if (it.createdAt > 0) it.createdAt else System.currentTimeMillis()
                    )
                }
                if (favoriteEntities.isNotEmpty()) {
                    database.favoriteDao().addFavorites(favoriteEntities)
                }

                // 3. Watch Later
                val watchLaterEntities = payload.data.watchLater.map {
                    val effectiveProfile = mapToProfile ?: it.profileId.ifBlank { "default" }
                    val cleanId = it.id.substringAfter("${effectiveProfile}_")
                    WatchLaterEntity(
                        profileId = effectiveProfile,
                        id = cleanId,
                        mediaId = it.mediaId,
                        mediaType = it.mediaType,
                        title = it.title,
                        posterPath = it.posterPath,
                        backdropPath = it.backdropPath,
                        overview = it.overview,
                        releaseYear = it.releaseYear,
                        voteAverage = it.voteAverage,
                        genres = it.genres,
                        addedAt = if (it.addedAt > 0) it.addedAt else System.currentTimeMillis()
                    )
                }
                if (watchLaterEntities.isNotEmpty()) {
                    database.watchLaterDao().insertWatchLaterItems(watchLaterEntities)
                }

                // 4. Watch History
                if (payload.data.watchHistory.isNotEmpty()) {
                    val historyEntities = payload.data.watchHistory.map {
                        WatchHistoryEntity(
                            profileId = mapToProfile ?: it.profileId.ifBlank { "default" },
                            channelId = it.channelId,
                            lastWatchedAt = if (it.lastWatchedAt > 0) it.lastWatchedAt else System.currentTimeMillis(),
                            watchDurationMs = it.watchDurationMs
                        )
                    }
                    database.watchHistoryDao().insertWatchHistoryItems(historyEntities)
                }

                // 5. Playback Progress
                if (payload.data.playbackProgress.isNotEmpty()) {
                    val progressEntities = payload.data.playbackProgress.map {
                        PlaybackProgressEntity(
                            profileId = mapToProfile ?: it.profileId.ifBlank { "default" },
                            downloadId = it.downloadId,
                            positionMs = it.positionMs,
                            durationMs = it.durationMs,
                            lastWatchedAt = if (it.lastWatchedAt > 0) it.lastWatchedAt else System.currentTimeMillis(),
                            completed = it.completed,
                            hidden = it.hidden
                        )
                    }
                    database.playbackProgressDao().insertAll(progressEntities)
                }

                // 6. Reminders
                if (payload.data.reminders.isNotEmpty()) {
                    val reminderEntities = payload.data.reminders.map {
                        ProgramReminderEntity(
                            id = it.id,
                            profileId = mapToProfile ?: it.profileId.ifBlank { "default" },
                            channelId = it.channelId,
                            programTitle = it.programTitle,
                            startEpochMs = it.startEpochMs,
                            notified = it.notified
                        )
                    }
                    database.programReminderDao().insertReminders(reminderEntities)
                }

                // 7. Recent Searches
                for (query in payload.data.recentSearches) {
                    if (query.isNotBlank()) {
                        database.recentSearchDao().insertSearch(
                            RecentSearchEntity(
                                profileId = currentActiveId,
                                query = query.trim(),
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    }
                }

                // 8. Custom Playlists
                for (pl in payload.data.playlists) {
                    database.playlistDao().insertPlaylist(
                        PlaylistEntity(
                            id = pl.id,
                            name = pl.name,
                            sourceType = pl.sourceType,
                            url = pl.url,
                            lastSyncedAt = pl.lastSyncedAt,
                            channelCount = pl.channelCount,
                            autoRefresh = pl.autoRefresh,
                            enabled = pl.enabled
                        )
                    )
                }
            }

            // 9. Non-sensitive Preferences
            val prefs = payload.data.preferences
            userPreferences.setPreferredLanguage(prefs.preferredLanguage)
            userPreferences.setPreferredCountry(prefs.preferredCountry)
            userPreferences.setBackgroundPlaybackEnabled(prefs.backgroundPlayback)
            userPreferences.setPipEnabled(prefs.pipEnabled)
            userPreferences.setAutoplayNextEpisode(prefs.autoplayNextEpisode)
            userPreferences.setPlayerAspectRatio(prefs.playerAspectRatio)
            userPreferences.setDailyDownloadLimitGb(prefs.dailyDownloadLimitGb)
            userPreferences.setWifiOnlyDownloads(prefs.wifiOnlyDownloads)
            userPreferences.setStartupSoundEnabled(prefs.startupSoundEnabled)
            userPreferences.setDownloadThreads(prefs.downloadThreads)
            userPreferences.setAskProfileOnLaunch(prefs.askProfileOnLaunch)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
