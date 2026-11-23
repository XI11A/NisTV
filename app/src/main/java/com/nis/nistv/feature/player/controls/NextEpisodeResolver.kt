package com.nis.nistv.feature.player.controls

import com.nis.nistv.core.database.entity.DownloadEntity
import com.nis.nistv.core.database.entity.DownloadStatus

/**
 * Pure function to resolve the next sequential episode in a series from completed downloads.
 * Checks for the next episode in the same season, or the first episode of subsequent seasons.
 */
fun resolveNextEpisode(
    current: DownloadEntity,
    candidates: List<DownloadEntity>
): DownloadEntity? {
    if (!current.mediaType.equals("tv", ignoreCase = true)) return null

    val targetSeriesId = current.effectiveSeriesId ?: current.seriesId ?: return null

    val seriesEpisodes = candidates.filter { candidate ->
        candidate.id != current.id &&
        candidate.mediaType.equals("tv", ignoreCase = true) &&
        (candidate.effectiveSeriesId == targetSeriesId || candidate.seriesId == targetSeriesId) &&
        (candidate.status.equals(DownloadStatus.COMPLETED.name, ignoreCase = true) ||
            (candidate.fileSizeBytes > 0 && candidate.downloadedBytes >= candidate.fileSizeBytes))
    }.sortedWith(
        compareBy<DownloadEntity> { it.seasonNumber ?: 1 }
            .thenBy { it.episodeNumber ?: 1 }
    )

    val currentSeason = current.seasonNumber ?: 1
    val currentEpisode = current.episodeNumber ?: 1

    // 1. Next episode in current season
    val nextInSeason = seriesEpisodes.firstOrNull { ep ->
        val s = ep.seasonNumber ?: 1
        val e = ep.episodeNumber ?: 1
        s == currentSeason && e > currentEpisode
    }
    if (nextInSeason != null) return nextInSeason

    // 2. First episode of next seasons
    return seriesEpisodes.firstOrNull { ep ->
        val s = ep.seasonNumber ?: 1
        s > currentSeason
    }
}

/**
 * Pure seek clamp utility.
 */
fun clampSeekPosition(currentPos: Long, deltaMs: Long, durationMs: Long): Long {
    val target = currentPos + deltaMs
    val max = if (durationMs > 0L) durationMs else Long.MAX_VALUE
    return target.coerceIn(0L, max)
}
