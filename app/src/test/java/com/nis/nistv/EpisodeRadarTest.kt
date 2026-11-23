package com.nis.nistv

import com.nis.nistv.data.radar.EpisodeKeyHelper
import org.junit.Assert.*
import org.junit.Test

class EpisodeRadarTest {

    @Test
    fun testBuildEpisodeKey() {
        assertEquals("S01E05", EpisodeKeyHelper.buildEpisodeKey(1, 5))
        assertEquals("S02E12", EpisodeKeyHelper.buildEpisodeKey(2, 12))
        assertEquals("S10E01", EpisodeKeyHelper.buildEpisodeKey(10, 1))
    }

    @Test
    fun testParseEpisodeKey() {
        val parsed = EpisodeKeyHelper.parseEpisodeKey("S03E08")
        assertNotNull(parsed)
        assertEquals(3, parsed?.first)
        assertEquals(8, parsed?.second)

        // Invalid key
        assertNull(EpisodeKeyHelper.parseEpisodeKey("Invalid"))
        assertNull(EpisodeKeyHelper.parseEpisodeKey(null))
    }

    @Test
    fun testIsNewerEpisodeAcrossSeasons() {
        // S2 is newer than S1
        assertTrue(EpisodeKeyHelper.isNewerEpisode("S02E01", "S01E10"))
        // S1E6 is newer than S1E5
        assertTrue(EpisodeKeyHelper.isNewerEpisode("S01E06", "S01E05"))
        // Same episode is not newer
        assertFalse(EpisodeKeyHelper.isNewerEpisode("S01E05", "S01E05"))
        // Older episode is not newer
        assertFalse(EpisodeKeyHelper.isNewerEpisode("S01E04", "S01E05"))
        assertFalse(EpisodeKeyHelper.isNewerEpisode("S01E10", "S02E01"))
        // When previous is null, any valid key is newer
        assertTrue(EpisodeKeyHelper.isNewerEpisode("S01E01", null))
        // When newKey is invalid, returns false
        assertFalse(EpisodeKeyHelper.isNewerEpisode("invalid", "S01E01"))
    }

    @Test
    fun testKeyStability() {
        for (season in 1..20) {
            for (episode in 1..25) {
                val key = EpisodeKeyHelper.buildEpisodeKey(season, episode)
                val parsed = EpisodeKeyHelper.parseEpisodeKey(key)
                assertEquals(season, parsed?.first)
                assertEquals(episode, parsed?.second)
            }
        }
    }
}
