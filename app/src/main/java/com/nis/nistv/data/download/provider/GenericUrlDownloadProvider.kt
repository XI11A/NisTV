package com.nis.nistv.data.download.provider

/**
 * Generic URL Download Provider
 * Enables adding direct HTTP/HTTPS URLs to the download queue.
 */
class GenericUrlDownloadProvider : DownloadProvider {
    override val id: String = "generic"
    override val name: String = "Direct URL Download"

    override suspend fun search(
        title: String,
        year: Int?,
        mediaType: DownloadMediaType,
        seasonNumber: Int?,
        episodeNumber: Int?,
        originalTitle: String?
    ): List<DownloadItemCandidate> {
        return emptyList()
    }

    override suspend fun resolve(candidate: DownloadItemCandidate): ResolvedDownloadSource {
        val fileName = if (candidate.title.contains(".")) candidate.title else "${candidate.title}.mp4"
        return ResolvedDownloadSource(
            url = candidate.id,
            fileName = fileName,
            sizeBytes = candidate.sizeBytes
        )
    }
}
