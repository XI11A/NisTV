package com.nis.nistv.data.download.provider

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

enum class DownloadMediaType {
    MOVIE,
    TV
}

data class DownloadItemCandidate(
    val id: String,
    val title: String,
    val sizeBytes: Long,
    val quality: String, // e.g. "1080p", "720p", "480p", "HD"
    val extraInfo: String = "",
    val sourceProviderId: String
)

data class ResolvedDownloadSource(
    val url: String,
    val fileName: String,
    val sizeBytes: Long,
    val headers: Map<String, String> = emptyMap()
)

/**
 * Pluggable DownloadProvider interface.
 * Allows multiple backends/sources (custom providers, licensed archives, AFDS adapter)
 * to be registered into the download manager without coupling to UI or engine.
 */
interface DownloadProvider {
    val id: String
    val name: String

    suspend fun search(
        title: String,
        year: Int? = null,
        mediaType: DownloadMediaType = DownloadMediaType.MOVIE,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        originalTitle: String? = null
    ): List<DownloadItemCandidate>

    /**
     * Progressive streaming search that emits combined, deduplicated candidates
     * in real-time as parallel queries complete.
     */
    fun searchFlow(
        title: String,
        year: Int? = null,
        mediaType: DownloadMediaType = DownloadMediaType.MOVIE,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        originalTitle: String? = null
    ): Flow<List<DownloadItemCandidate>> = flow {
        emit(search(title, year, mediaType, seasonNumber, episodeNumber, originalTitle))
    }

    suspend fun resolve(candidate: DownloadItemCandidate): ResolvedDownloadSource
}
