package com.nis.nistv.feature.home

import com.nis.nistv.core.database.entity.DownloadEntity
import com.nis.nistv.core.database.entity.DownloadStatus
import com.nis.nistv.core.database.entity.PlaybackProgressEntity

data class ContinueWatchingItem(
    val download: DownloadEntity,
    val progressFraction: Float,
    val remainingTimeText: String,
    val subtitleText: String? = null
)

fun formatRemainingTime(remainingMs: Long): String {
    if (remainingMs <= 0L) return "Finished"
    val totalSec = remainingMs / 1000L
    val minutes = (totalSec / 60L) % 60L
    val hours = totalSec / 3600L
    return when {
        hours > 0 -> "${hours}h ${minutes}m left"
        minutes > 0 -> "${minutes}m left"
        else -> "< 1m left"
    }
}

fun formatEpisodeTag(seasonNumber: Int?, episodeNumber: Int?): String? {
    if (seasonNumber == null && episodeNumber == null) return null
    val s = seasonNumber ?: 1
    val e = episodeNumber ?: 1
    return "S$s E$e"
}

fun buildContinueWatching(
    downloads: List<DownloadEntity>,
    playbackProgressList: List<PlaybackProgressEntity> = emptyList(),
    profileId: String = "default",
    now: Long = System.currentTimeMillis()
): List<ContinueWatchingItem> {
    val progressMap = playbackProgressList.associateBy { it.downloadId }

    // 1. Filter completed and unhidden downloads for this profile
    val completed = downloads.filter { item ->
        val progress = progressMap[item.id]
        val isHidden = progress?.hidden ?: if (profileId == "default") item.hideFromContinueWatching else false
        val isDownloaded = item.status.equals(DownloadStatus.COMPLETED.name, ignoreCase = true) ||
            (item.fileSizeBytes > 0 && item.downloadedBytes >= item.fileSizeBytes)

        !isHidden && isDownloaded
    }

    val resultItems = mutableListOf<ContinueWatchingItem>()

    // 2. Separate into Standalone Movies and TV Series
    val (tvItems, movieItems) = completed.partition { it.mediaType.equals("tv", ignoreCase = true) }

    // Process Standalone Movies
    for (movie in movieItems) {
        val progress = progressMap[movie.id]
        val duration = progress?.durationMs?.takeIf { it > 0 } ?: movie.durationMs
        val pos = progress?.positionMs ?: if (profileId == "default") movie.lastPlaybackPositionMs else 0L
        val isCompleted = progress?.completed ?: if (profileId == "default") movie.watchedCompleted else false

        if (duration <= 0L) continue // Legacy row with durationMs = 0 is omitted
        if (isCompleted) continue

        val frac = pos.toFloat() / duration.toFloat()
        // Between 5% and 95% of duration
        if (frac in 0.05f..0.95f) {
            val remainingMs = (duration - pos).coerceAtLeast(0L)
            resultItems.add(
                ContinueWatchingItem(
                    download = movie,
                    progressFraction = frac,
                    remainingTimeText = formatRemainingTime(remainingMs),
                    subtitleText = null
                )
            )
        }
    }

    // Process TV Series: Group by effective series identifier
    val seriesGroups = tvItems.groupBy { it.effectiveSeriesId ?: it.seriesId ?: it.title }

    for ((_, episodes) in seriesGroups) {
        val sortedEpisodes = episodes.sortedWith(
            compareBy<DownloadEntity> { it.seasonNumber ?: 1 }
                .thenBy { it.episodeNumber ?: 1 }
        )

        // Find in-progress episodes: duration > 0, progress in 0.05..0.95, and not watchedCompleted
        val inProgressEpisodes = sortedEpisodes.filter { ep ->
            val progress = progressMap[ep.id]
            val d = progress?.durationMs?.takeIf { it > 0 } ?: ep.durationMs
            val p = progress?.positionMs ?: if (profileId == "default") ep.lastPlaybackPositionMs else 0L
            val completedFlag = progress?.completed ?: if (profileId == "default") ep.watchedCompleted else false

            !completedFlag && d > 0L && (p.toFloat() / d.toFloat()) in 0.05f..0.95f
        }

        if (inProgressEpisodes.isNotEmpty()) {
            val currentEp = inProgressEpisodes.maxByOrNull { ep ->
                val progress = progressMap[ep.id]
                val watchedAt = progress?.lastWatchedAt ?: if (profileId == "default") ep.lastWatchedAt else 0L
                if (watchedAt > 0L) watchedAt else ep.updatedAt
            } ?: inProgressEpisodes.first()

            val progress = progressMap[currentEp.id]
            val dur = progress?.durationMs?.takeIf { it > 0 } ?: currentEp.durationMs
            val pos = progress?.positionMs ?: if (profileId == "default") currentEp.lastPlaybackPositionMs else 0L
            val frac = if (dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
            val remainingMs = (dur - pos).coerceAtLeast(0L)

            resultItems.add(
                ContinueWatchingItem(
                    download = currentEp,
                    progressFraction = frac,
                    remainingTimeText = formatRemainingTime(remainingMs),
                    subtitleText = formatEpisodeTag(currentEp.seasonNumber, currentEp.episodeNumber)
                )
            )
        } else {
            // Check if any episode finished
            val completedEpisodes = sortedEpisodes.filter { ep ->
                val progress = progressMap[ep.id]
                val d = progress?.durationMs?.takeIf { it > 0 } ?: ep.durationMs
                val p = progress?.positionMs ?: if (profileId == "default") ep.lastPlaybackPositionMs else 0L
                val completedFlag = progress?.completed ?: if (profileId == "default") ep.watchedCompleted else false

                completedFlag || (d > 0L && p >= d * 0.95f)
            }

            if (completedEpisodes.isNotEmpty()) {
                val lastCompleted = completedEpisodes.maxWithOrNull(
                    compareBy<DownloadEntity> { it.seasonNumber ?: 1 }
                        .thenBy { it.episodeNumber ?: 1 }
                ) ?: completedEpisodes.last()

                val lastS = lastCompleted.seasonNumber ?: 1
                val lastE = lastCompleted.episodeNumber ?: 1

                val nextEp = sortedEpisodes.firstOrNull { ep ->
                    val s = ep.seasonNumber ?: 1
                    val e = ep.episodeNumber ?: 1
                    val progress = progressMap[ep.id]
                    val completedFlag = progress?.completed ?: if (profileId == "default") ep.watchedCompleted else false
                    !completedFlag && (s > lastS || (s == lastS && e > lastE))
                }

                if (nextEp != null) {
                    val progress = progressMap[nextEp.id]
                    val d = progress?.durationMs?.takeIf { it > 0 } ?: nextEp.durationMs
                    val pos = progress?.positionMs ?: if (profileId == "default") nextEp.lastPlaybackPositionMs else 0L
                    val frac = if (d > 0L) (pos.toFloat() / d.toFloat()).coerceIn(0f, 1f) else 0f
                    val remainingMs = if (d > 0L) (d - pos).coerceAtLeast(0L) else 0L

                    resultItems.add(
                        ContinueWatchingItem(
                            download = nextEp,
                            progressFraction = frac,
                            remainingTimeText = if (d > 0L) formatRemainingTime(remainingMs) else "Start watching",
                            subtitleText = formatEpisodeTag(nextEp.seasonNumber, nextEp.episodeNumber)
                        )
                    )
                }
            }
        }
    }

    // Sort resulting cards by most recent activity
    return resultItems.sortedByDescending { item ->
        val progress = progressMap[item.download.id]
        val watchedAt = progress?.lastWatchedAt ?: if (profileId == "default") item.download.lastWatchedAt else 0L
        if (watchedAt > 0L) watchedAt else item.download.updatedAt
    }
}
