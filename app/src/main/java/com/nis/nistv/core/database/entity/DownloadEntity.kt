package com.nis.nistv.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey
    val id: String,
    val mediaId: String,
    val title: String,
    val posterUrl: String? = null,
    val mediaType: String = "movie", // "movie" or "tv"
    val downloadUrl: String,
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val status: String = DownloadStatus.QUEUED.name,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // TMDB metadata saved locally for rich offline display
    val overview: String? = null,
    val releaseDate: String? = null,
    val rating: Double? = null,
    val backdropUrl: String? = null,
    val genres: String? = null,
    val runtimeMinutes: Int? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeName: String? = null,
    // Source provider details for automatic link renewal on resume
    val afdsFileId: String? = null,
    val afdsSourceTable: String? = null,
    val sourceProviderId: String? = null,
    // V6 columns: Dedicated series identifiers and offline playback resume
    val seriesId: String? = null,
    val seriesName: String? = null,
    val lastPlaybackPositionMs: Long = 0L,
    // V7 columns: Continue watching and playback tracking
    val durationMs: Long = 0L,
    val lastWatchedAt: Long = 0L,
    val watchedCompleted: Boolean = false,
    val hideFromContinueWatching: Boolean = false
) {
    val effectiveSeriesId: String?
        get() = seriesId ?: if (mediaType == "tv") {
            if (mediaId.contains('_')) mediaId.substringBefore('_') else mediaId
        } else null

    val effectiveSeriesName: String?
        get() = seriesName ?: if (mediaType == "tv") title.replace(Regex("\\s+S\\d+E\\d+.*$", RegexOption.IGNORE_CASE), "").trim() else null

    val progressFloat: Float
        get() = if (fileSizeBytes > 0) (downloadedBytes.toFloat() / fileSizeBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val progressPercent: Int
        get() = if (status == DownloadStatus.COMPLETED.name || (fileSizeBytes > 0 && downloadedBytes >= fileSizeBytes)) {
            100
        } else {
            (progressFloat * 100).toInt().coerceIn(0, 99)
        }

    val formattedDownloaded: String
        get() = formatBytes(downloadedBytes)

    val formattedTotal: String
        get() = if (fileSizeBytes > 0) formatBytes(fileSizeBytes) else "Unknown"

    val formattedSpeed: String
        get() = if (speedBytesPerSec > 0) "${formatBytes(speedBytesPerSec)}/s" else "--"

    val isTerminal: Boolean
        get() = status == DownloadStatus.COMPLETED.name || status == DownloadStatus.FAILED.name || status == DownloadStatus.CANCELLED.name

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(java.util.Locale.US, "%.1f GB", gb)
                mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(java.util.Locale.US, "%.1f KB", kb)
                else -> "$bytes B"
            }
        }
    }
}
