package com.nis.nistv.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupProfileItem(
    val id: String,
    val name: String,
    val avatarId: String = "avatar_01",
    val preferredLanguage: String = "Hindi",
    val preferredCountry: String = "IN",
    val isKids: Boolean = false,
    val isLocked: Boolean = false,
    val historyPaused: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class BackupFavoriteItem(
    val profileId: String = "default",
    val channelId: String,
    val createdAt: Long = 0L
)

@Serializable
data class BackupWatchLaterItem(
    val profileId: String = "default",
    val id: String,
    val mediaId: Long,
    val mediaType: String,
    val title: String,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val overview: String? = null,
    val releaseYear: String? = null,
    val voteAverage: Double? = null,
    val genres: String? = null,
    val addedAt: Long = 0L
)

@Serializable
data class BackupWatchHistoryItem(
    val profileId: String = "default",
    val channelId: String,
    val lastWatchedAt: Long = 0L,
    val watchDurationMs: Long = 0L
)

@Serializable
data class BackupPlaybackProgressItem(
    val profileId: String = "default",
    val downloadId: String,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastWatchedAt: Long = 0L,
    val completed: Boolean = false,
    val hidden: Boolean = false
)

@Serializable
data class BackupReminderItem(
    val id: String,
    val profileId: String = "default",
    val channelId: String,
    val programTitle: String,
    val startEpochMs: Long,
    val notified: Boolean = false
)

@Serializable
data class BackupPlaylistItem(
    val id: String,
    val name: String,
    val sourceType: String,
    val url: String,
    val lastSyncedAt: Long = 0L,
    val channelCount: Int = 0,
    val autoRefresh: Boolean = true,
    val enabled: Boolean = true
)

@Serializable
data class BackupPreferences(
    val preferredLanguage: String = "Hindi",
    val preferredCountry: String = "IN",
    val backgroundPlayback: Boolean = true,
    val pipEnabled: Boolean = true,
    val autoplayNextEpisode: Boolean = true,
    val playerAspectRatio: Int = 0,
    val dailyDownloadLimitGb: Int = 10,
    val wifiOnlyDownloads: Boolean = false,
    val startupSoundEnabled: Boolean = true,
    val downloadThreads: Int = 4,
    val askProfileOnLaunch: Boolean = true
)

@Serializable
data class NisTvBackupData(
    val profiles: List<BackupProfileItem> = emptyList(),
    val favorites: List<BackupFavoriteItem> = emptyList(),
    val watchLater: List<BackupWatchLaterItem> = emptyList(),
    val watchHistory: List<BackupWatchHistoryItem> = emptyList(),
    val playbackProgress: List<BackupPlaybackProgressItem> = emptyList(),
    val reminders: List<BackupReminderItem> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val playlists: List<BackupPlaylistItem> = emptyList(),
    val preferences: BackupPreferences = BackupPreferences()
)

@Serializable
data class NisTvBackupPayload(
    val schemaVersion: Int = 2,
    val appVersion: String,
    val createdAt: Long = System.currentTimeMillis(),
    val data: NisTvBackupData,
    val checksum: String = ""
)

data class BackupPreview(
    val schemaVersion: Int,
    val appVersion: String,
    val createdAt: Long,
    val favoritesCount: Int,
    val watchLaterCount: Int,
    val recentSearchesCount: Int,
    val playlistsCount: Int,
    val profilesCount: Int = 0,
    val payload: NisTvBackupPayload
)

sealed class BackupParseResult {
    data class Success(val preview: BackupPreview) : BackupParseResult()
    data class VersionMismatch(val foundVersion: Int, val maxSupportedVersion: Int) : BackupParseResult()
    data class ChecksumError(val message: String) : BackupParseResult()
    data class Corrupt(val error: String) : BackupParseResult()
}
