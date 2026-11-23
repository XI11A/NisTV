package com.nis.nistv

import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.database.entity.ProfileEntity
import com.nis.nistv.core.database.entity.StreamSourceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelModelTest {

    @Test
    fun testChannelEntityCreation() {
        val channel = ChannelEntity(
            id = "AajTak.in",
            name = "Aaj Tak [720p]",
            cleanName = "Aaj Tak",
            altNames = "AajTak News",
            country = "IN",
            network = "Living Media",
            website = "https://aajtak.in",
            language = "Hindi",
            category = "News",
            logoUrl = "https://example.com/logo.png",
            resolution = "1080p",
            isNsfw = false,
            labels = ""
        )

        assertEquals("AajTak.in", channel.id)
        assertEquals("Aaj Tak", channel.cleanName)
        assertEquals("Hindi", channel.language)
        assertEquals("News", channel.category)
        assertFalse(channel.isNsfw)
    }

    @Test
    fun testStreamSourceEntityPriority() {
        val stream1 = StreamSourceEntity(
            id = 1,
            channelId = "AajTak.in",
            url = "https://stream1.m3u8",
            quality = "1080p",
            priority = 0,
            failCount = 0
        )

        val stream2 = StreamSourceEntity(
            id = 2,
            channelId = "AajTak.in",
            url = "https://stream2.m3u8",
            quality = "720p",
            priority = 1,
            failCount = 1
        )

        val streams = listOf(stream2, stream1).sortedWith(
            compareBy({ it.priority }, { it.failCount }, { it.id })
        )

        assertEquals(1L, streams[0].id)
        assertEquals(2L, streams[1].id)
    }

    @Test
    fun testProfileDefaults() {
        val profile = ProfileEntity(
            id = "default",
            name = "Primary Profile",
            preferredLanguage = "Hindi"
        )

        assertEquals("default", profile.id)
        assertEquals("Primary Profile", profile.name)
        assertEquals("Hindi", profile.preferredLanguage)
        assertNotNull(profile.createdAt)
    }
}
