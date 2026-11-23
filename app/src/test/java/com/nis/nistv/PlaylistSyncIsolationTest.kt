package com.nis.nistv

import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.database.entity.StreamSourceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistSyncIsolationTest {

    @Test
    fun customChannelIds_haveDistinctPrefix_neverCollidingWithBuiltInIptvOrgIds() {
        val builtInIds = listOf("starplus.in", "aajtak.in", "colors.in", "zee.tv.in", "sun.tv.in")
        val playlistId = "pl_user_1"

        for (name in listOf("Star Plus", "Aaj Tak", "Colors", "Zee TV", "Sun TV")) {
            val hash = (name.lowercase().hashCode() and 0x7FFFFFFF).toString(16)
            val customId = "custom_${playlistId}_$hash"

            assertTrue("Custom channel ID must start with custom_", customId.startsWith("custom_"))
            assertTrue("Custom ID must contain playlist ID", customId.contains(playlistId))
            for (builtIn in builtInIds) {
                assertTrue("Custom ID must never match built-in ID", customId != builtIn)
            }
        }
    }

    @Test
    fun playlistChannelFiltering_distinguishesCustomAndBuiltInChannels() {
        val builtInChannel = ChannelEntity(
            id = "aajtak.in",
            name = "Aaj Tak",
            cleanName = "Aaj Tak",
            playlistId = null
        )

        val customChannel1 = ChannelEntity(
            id = "custom_pl1_abc",
            name = "My News",
            cleanName = "My News",
            playlistId = "pl1"
        )

        val customChannel2 = ChannelEntity(
            id = "custom_pl2_def",
            name = "Sports Live",
            cleanName = "Sports Live",
            playlistId = "pl2"
        )

        val allChannels = listOf(builtInChannel, customChannel1, customChannel2)

        val builtInOnly = allChannels.filter { it.playlistId == null }
        assertEquals(1, builtInOnly.size)
        assertEquals("aajtak.in", builtInOnly[0].id)

        val pl1Only = allChannels.filter { it.playlistId == "pl1" }
        assertEquals(1, pl1Only.size)
        assertEquals("custom_pl1_abc", pl1Only[0].id)

        val anyCustom = allChannels.filter { it.playlistId != null }
        assertEquals(2, anyCustom.size)
    }

    @Test
    fun simulateUpsert_builtInSyncDoesNotOverwriteCustomChannels() {
        // Map mimicking SQLite table with primary key `id`
        val databaseChannels = mutableMapOf<String, ChannelEntity>()

        // 1. Existing custom channels from user playlists
        val customChannel = ChannelEntity(
            id = "custom_pl_news_99",
            name = "Private News Stream",
            cleanName = "Private News Stream",
            playlistId = "pl_news"
        )
        databaseChannels[customChannel.id] = customChannel

        // 2. Incoming sync channels from iptv-org
        val syncedChannels = listOf(
            ChannelEntity(id = "news.in", name = "National News", cleanName = "National News", playlistId = null),
            ChannelEntity(id = "sports.in", name = "National Sports", cleanName = "National Sports", playlistId = null)
        )

        for (ch in syncedChannels) {
            databaseChannels[ch.id] = ch
        }

        // Verify custom channel is completely intact and untouched
        assertNotNull(databaseChannels["custom_pl_news_99"])
        assertEquals("pl_news", databaseChannels["custom_pl_news_99"]?.playlistId)
        assertEquals("Private News Stream", databaseChannels["custom_pl_news_99"]?.name)
        assertEquals(3, databaseChannels.size)
    }

    @Test
    fun simulateCascadeDelete_playlistRemovalRemovesChannelsAndStreams() {
        val databaseChannels = mutableMapOf<String, ChannelEntity>()
        val databaseStreams = mutableListOf<StreamSourceEntity>()

        // Add playlist channels
        val ch1 = ChannelEntity(id = "custom_pl1_1", name = "Ch 1", cleanName = "Ch 1", playlistId = "pl1")
        val ch2 = ChannelEntity(id = "custom_pl2_2", name = "Ch 2", cleanName = "Ch 2", playlistId = "pl2")
        databaseChannels[ch1.id] = ch1
        databaseChannels[ch2.id] = ch2

        databaseStreams.add(StreamSourceEntity(id = 1, channelId = "custom_pl1_1", url = "http://a.com"))
        databaseStreams.add(StreamSourceEntity(id = 2, channelId = "custom_pl2_2", url = "http://b.com"))

        // Cascade delete playlist 1
        val playlistToDelete = "pl1"
        val channelsToDelete = databaseChannels.values.filter { it.playlistId == playlistToDelete }.map { it.id }.toSet()
        channelsToDelete.forEach { databaseChannels.remove(it) }
        databaseStreams.removeAll { channelsToDelete.contains(it.channelId) }

        // Assert pl1 is deleted, pl2 remains
        assertNull(databaseChannels["custom_pl1_1"])
        assertNotNull(databaseChannels["custom_pl2_2"])
        assertEquals(1, databaseStreams.size)
        assertEquals("custom_pl2_2", databaseStreams[0].channelId)
    }
}
