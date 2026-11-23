package com.nis.nistv

import com.nis.nistv.data.tmdb.model.MediaPreview
import com.nis.nistv.data.tmdb.repository.MediaPreviewStore
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MediaPreviewStoreTest {

    private lateinit var store: MediaPreviewStore

    @Before
    fun setup() {
        store = MediaPreviewStore(maxEntries = 3)
    }

    @Test
    fun putAndGet_returnsCachedPreview() {
        val preview = MediaPreview(
            tmdbId = 550,
            mediaType = "movie",
            title = "Fight Club",
            backdropPath = "/backdrop.jpg",
            posterPath = "/poster.jpg",
            releaseYear = "1999",
            voteAverage = 8.4
        )

        store.put(preview)

        val retrieved = store.get("movie", 550)
        assertNotNull(retrieved)
        assertEquals("Fight Club", retrieved?.title)
        assertEquals("/backdrop.jpg", retrieved?.backdropPath)
        assertEquals("/poster.jpg", retrieved?.posterPath)
    }

    @Test
    fun getWithLongId_returnsMatchingPreview() {
        val preview = MediaPreview(
            tmdbId = 999,
            mediaType = "tv",
            title = "Test Show",
            backdropPath = null,
            posterPath = "/tv_poster.jpg"
        )

        store.put(preview)

        val retrieved = store.get("tv", 999L)
        assertNotNull(retrieved)
        assertEquals("Test Show", retrieved?.title)
    }

    @Test
    fun getNonExistent_returnsNull() {
        val retrieved = store.get("movie", 12345)
        assertNull(retrieved)
    }

    @Test
    fun eviction_evictsOldestWhenMaxCapacityReached() {
        val item1 = MediaPreview(tmdbId = 1, mediaType = "movie", title = "One", backdropPath = null, posterPath = null)
        val item2 = MediaPreview(tmdbId = 2, mediaType = "movie", title = "Two", backdropPath = null, posterPath = null)
        val item3 = MediaPreview(tmdbId = 3, mediaType = "movie", title = "Three", backdropPath = null, posterPath = null)
        val item4 = MediaPreview(tmdbId = 4, mediaType = "movie", title = "Four", backdropPath = null, posterPath = null)

        store.put(item1)
        store.put(item2)
        store.put(item3)
        assertEquals(3, store.size())

        // Access item1 to make item2 the least recently used
        store.get("movie", 1)

        // Adding 4th item evicts item2
        store.put(item4)

        assertNotNull(store.get("movie", 1))
        assertNull(store.get("movie", 2))
        assertNotNull(store.get("movie", 3))
        assertNotNull(store.get("movie", 4))
    }

    @Test
    fun clear_evictsAllEntries() {
        val item = MediaPreview(tmdbId = 42, mediaType = "movie", title = "Answer", backdropPath = null, posterPath = null)
        store.put(item)
        assertEquals(1, store.size())

        store.clear()
        assertEquals(0, store.size())
        assertNull(store.get("movie", 42))
    }
}
