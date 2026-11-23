package com.nis.nistv

import com.nis.nistv.data.playlist.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class M3uParserTest {

    @Test
    fun parse_validM3uWithAttributesAndCrlf_returnsChannelsAndStreams() {
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1 tvg-id="star.plus.in" tvg-name="Star Plus HD" tvg-logo="https://example.com/star.png" group-title="Entertainment" tvg-language="Hindi",Star Plus HD
            https://stream.example.com/starplus.m3u8
            #EXTINF:-1 tvg-id="sun.tv.in" tvg-name="Sun TV HD, Tamil" tvg-logo="https://example.com/sun.png" group-title="Entertainment" tvg-language="Tamil",Sun TV HD
            http://stream.example.com/suntv.m3u8
        """.trimIndent().replace("\n", "\r\n")

        val result = M3uParser.parse(
            inputStream = ByteArrayInputStream(m3uContent.toByteArray(Charsets.UTF_8)),
            playlistId = "pl_123",
            playlistName = "Test Playlist"
        )

        assertEquals(2, result.importedCount)
        assertEquals(0, result.skippedCount)
        assertEquals(2, result.channels.size)
        assertEquals(2, result.streams.size)

        val starChannel = result.channels.find { it.name == "Star Plus HD" }
        assertNotNull(starChannel)
        assertTrue(starChannel!!.id.startsWith("custom_pl_123_"))
        assertEquals("Entertainment", starChannel.category)
        assertEquals("Hindi", starChannel.language)
        assertEquals("https://example.com/star.png", starChannel.logoUrl)
        assertEquals("pl_123", starChannel.playlistId)

        val sunChannel = result.channels.find { it.name == "Sun TV HD" }
        assertNotNull(sunChannel)
        assertEquals("Tamil", sunChannel!!.language)
    }

    @Test
    fun parse_handlesCommasInAttributesGracefully() {
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1 tvg-name="Sports, Live 1" group-title="Sports, Global",Star Sports 1, Hindi
            rtsp://sports.example.com/live
        """.trimIndent()

        val result = M3uParser.parse(
            inputStream = ByteArrayInputStream(m3uContent.toByteArray(Charsets.UTF_8)),
            playlistId = "sports_pl",
            playlistName = "Sports"
        )

        assertEquals(1, result.importedCount)
        assertEquals(0, result.skippedCount)
        assertEquals("Star Sports 1, Hindi", result.channels[0].name)
        assertEquals("Sports, Global", result.channels[0].category)
        assertEquals("rtsp://sports.example.com/live", result.streams[0].url)
    }

    @Test
    fun parse_groupsMultipleStreamsForSameChannel() {
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1 tvg-id="sports1" group-title="Sports",Main Sports HD
            https://server1.example.com/stream.m3u8
            #EXTINF:-1 tvg-id="sports1" group-title="Sports",Main Sports HD (Backup)
            https://server2.example.com/stream_backup.m3u8
        """.trimIndent()

        val result = M3uParser.parse(
            inputStream = ByteArrayInputStream(m3uContent.toByteArray(Charsets.UTF_8)),
            playlistId = "sports_pl"
        )

        // 1 unique channel with 2 streams for failover
        assertEquals(1, result.importedCount)
        assertEquals(1, result.channels.size)
        assertEquals(2, result.streams.size)
        assertEquals(0, result.streams[0].priority)
        assertEquals(1, result.streams[1].priority)
        assertEquals("https://server1.example.com/stream.m3u8", result.streams[0].url)
        assertEquals("https://server2.example.com/stream_backup.m3u8", result.streams[1].url)
    }

    @Test
    fun parse_skipsInvalidProtocolsAndMalformedLines() {
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1,Valid Channel
            https://valid.com/live.m3u8
            #EXTINF:-1,FTP Protocol Invalid
            ftp://invalid.com/file.mp4
            #EXTINF:-1,File Protocol Invalid
            file:///storage/media.m3u8
            #EXTINF:-1,Space in URL
            https://invalid url with spaces.com/live.m3u8
        """.trimIndent()

        val result = M3uParser.parse(
            inputStream = ByteArrayInputStream(m3uContent.toByteArray(Charsets.UTF_8)),
            playlistId = "mixed_pl"
        )

        assertEquals(1, result.importedCount)
        assertEquals(3, result.skippedCount)
        assertEquals("1 imported, 3 skipped", result.summary)
    }

    @Test
    fun maskUrl_masksTokensAndSecrets() {
        val url = "https://example.com/live.m3u8?token=SECRET123&auth=AUTH_KEY&user=john"
        val masked = M3uParser.maskUrl(url)
        assertFalse(masked.contains("SECRET123"))
        assertFalse(masked.contains("AUTH_KEY"))
        assertTrue(masked.contains("token=***"))
        assertTrue(masked.contains("auth=***"))
        assertTrue(masked.contains("user=john"))
    }
}
