package com.nis.nistv

import com.nis.nistv.data.model.SeedChannelDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SeedValidationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun testSeedFileIntegrity() {
        val seedFile = File("src/main/assets/seed/channels_seed.json")
        assertTrue("channels_seed.json must exist in assets", seedFile.exists())

        val content = seedFile.readText()
        val channels = json.decodeFromString<List<SeedChannelDto>>(content)

        assertTrue("Seed data should contain channels", channels.isNotEmpty())
        assertEquals(905, channels.size)

        // Verify all channels have non-blank ID, name, stream URL, country IN
        for (channel in channels) {
            assertTrue("Channel ID must not be blank", channel.id.isNotBlank())
            assertTrue("Channel name must not be blank: ${channel.id}", channel.name.isNotBlank())
            assertTrue("Stream URL must not be blank: ${channel.id}", channel.streamUrl.isNotBlank())
            assertEquals("Channel must be from India", "IN", channel.country)
            assertFalse("Seed data must not contain NSFW channels: ${channel.id}", channel.isNsfw)
        }
    }

    @Test
    fun testFeaturedJsonIntegrity() {
        val featuredFile = File("src/main/assets/featured_in.json")
        assertTrue("featured_in.json must exist in assets", featuredFile.exists())

        val content = featuredFile.readText()
        val featuredIds = json.decodeFromString<List<String>>(content)

        assertTrue("Featured list must not be empty", featuredIds.isNotEmpty())
        for (id in featuredIds) {
            assertTrue("Featured ID must not be blank", id.isNotBlank())
        }
    }
}
