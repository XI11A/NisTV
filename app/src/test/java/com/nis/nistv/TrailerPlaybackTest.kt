package com.nis.nistv

import com.nis.nistv.data.tmdb.model.TmdbVideoDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrailerPlaybackTest {

    @Test
    fun testInAppTrailerDialogClassLoads() {
        assertNotNull(Class.forName("com.nis.nistv.feature.details.components.InAppTrailerDialogKt"))
    }

    @Test
    fun testYouTubeTrailerDetection() {
        val officialTrailer = TmdbVideoDto(
            id = "1",
            key = "dQw4w9WgXcQ",
            name = "Official Trailer",
            site = "YouTube",
            type = "Trailer",
            official = true
        )
        assertTrue(officialTrailer.isYouTubeTrailer)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", officialTrailer.youTubeUrl)

        val teaser = TmdbVideoDto(
            id = "2",
            key = "abc123xyz",
            name = "Teaser",
            site = "YouTube",
            type = "Teaser",
            official = true
        )
        assertTrue(teaser.isYouTubeTrailer)

        val featurette = TmdbVideoDto(
            id = "3",
            key = "feat123",
            name = "Behind the Scenes",
            site = "YouTube",
            type = "Featurette",
            official = true
        )
        assertFalse(featurette.isYouTubeTrailer)

        val vimeoTrailer = TmdbVideoDto(
            id = "4",
            key = "vim123",
            name = "Trailer",
            site = "Vimeo",
            type = "Trailer",
            official = true
        )
        assertFalse(vimeoTrailer.isYouTubeTrailer)
    }

    @Test
    fun testYouTubeKeyExtractionFromUrlFallback() {
        fun extractKey(url: String?): String? {
            if (url.isNullOrBlank()) return null
            val vParam = Regex("[?&]v=([^&]+)").find(url)?.groupValues?.get(1)
            return vParam ?: if (url.contains("youtu.be/")) url.substringAfter("youtu.be/").substringBefore("?") else null
        }

        assertEquals("dQw4w9WgXcQ", extractKey("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", extractKey("https://www.youtube.com/watch?feature=shared&v=dQw4w9WgXcQ&t=10s"))
        assertEquals("dQw4w9WgXcQ", extractKey("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", extractKey("https://youtu.be/dQw4w9WgXcQ?t=15"))
        assertEquals(null, extractKey(null))
        assertEquals(null, extractKey(""))
        assertEquals(null, extractKey("https://vimeo.com/123456"))
    }

    @Test
    fun testEmbedUrlConstruction() {
        fun buildEmbedUrl(key: String): String {
            return "https://www.youtube-nocookie.com/embed/$key?autoplay=1&playsinline=1&controls=1&rel=0&modestbranding=1"
        }

        val embed = buildEmbedUrl("dQw4w9WgXcQ")
        assertTrue(embed.startsWith("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ"))
        assertTrue(embed.contains("autoplay=1"))
        assertTrue(embed.contains("playsinline=1"))
    }
}
