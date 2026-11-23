package com.nis.nistv.data.tmdb.repository

import androidx.collection.LruCache
import com.nis.nistv.data.tmdb.model.MediaPreview

/**
 * Thread-safe in-memory cache for media preview metadata used during container
 * transform animations and instant details rendering before network payloads arrive.
 */
class MediaPreviewStore(maxEntries: Int = 200) {
    private val cache = LruCache<String, MediaPreview>(maxEntries)

    private fun key(mediaType: String, tmdbId: Int): String =
        "${mediaType.lowercase()}_$tmdbId"

    fun put(preview: MediaPreview) {
        cache.put(key(preview.mediaType, preview.tmdbId), preview)
    }

    fun get(mediaType: String, tmdbId: Int): MediaPreview? =
        cache.get(key(mediaType, tmdbId))

    fun get(mediaType: String, tmdbId: Long): MediaPreview? =
        get(mediaType, tmdbId.toInt())

    fun size(): Int = cache.size()

    fun clear() {
        cache.evictAll()
    }
}
