package com.nis.nistv

import com.nis.nistv.data.zapper.ZapContext
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ChannelZapperTest {

    private lateinit var zapContext: ZapContext

    @Before
    fun setUp() {
        zapContext = ZapContext()
    }

    @Test
    fun testEmptyContextReturnsNull() {
        assertNull(zapContext.getNextChannelId())
        assertNull(zapContext.getPreviousChannelId())
        assertNull(zapContext.getLastChannelId())
        assertNull(zapContext.getChannelByIndex(1))
        assertEquals(0, zapContext.getCurrentChannelNumber())
    }

    @Test
    fun testNextChannelWithWrapAround() {
        val channels = listOf("chan_1", "chan_2", "chan_3")
        zapContext.setContext("Tamil", channels, "chan_1")

        assertEquals(1, zapContext.getCurrentChannelNumber())
        assertEquals("chan_2", zapContext.getNextChannelId())

        zapContext.updateCurrentChannel("chan_2")
        assertEquals(2, zapContext.getCurrentChannelNumber())
        assertEquals("chan_3", zapContext.getNextChannelId())

        zapContext.updateCurrentChannel("chan_3")
        assertEquals(3, zapContext.getCurrentChannelNumber())
        // Wrap around back to chan_1
        assertEquals("chan_1", zapContext.getNextChannelId())
    }

    @Test
    fun testPreviousChannelWithWrapAround() {
        val channels = listOf("chan_1", "chan_2", "chan_3")
        zapContext.setContext("News", channels, "chan_1")

        // Wrapping backward from index 0 should give chan_3
        assertEquals("chan_3", zapContext.getPreviousChannelId())

        zapContext.updateCurrentChannel("chan_3")
        assertEquals("chan_2", zapContext.getPreviousChannelId())
    }

    @Test
    fun testSkipsParentalLockedChannels() {
        val channels = listOf("chan_open", "chan_locked_1", "chan_locked_2", "chan_next_open")
        zapContext.setContext("All", channels, "chan_open")

        val locked = setOf("chan_locked_1", "chan_locked_2")

        // Should jump straight over locked channels to chan_next_open
        val next = zapContext.getNextChannelId(locked)
        assertEquals("chan_next_open", next)

        zapContext.updateCurrentChannel("chan_next_open")
        // Going backward should jump over locked channels straight back to chan_open
        val prev = zapContext.getPreviousChannelId(locked)
        assertEquals("chan_open", prev)
    }

    @Test
    fun testLastChannelToggle() {
        val channels = listOf("chan_1", "chan_2", "chan_3")
        zapContext.setContext("Favorites", channels, "chan_1")

        // No previous channel yet
        assertNull(zapContext.getLastChannelId())

        // Switch to chan_2 -> previous becomes chan_1
        zapContext.updateCurrentChannel("chan_2")
        assertEquals("chan_1", zapContext.getLastChannelId())

        // Switch to chan_3 -> previous becomes chan_2
        zapContext.updateCurrentChannel("chan_3")
        assertEquals("chan_2", zapContext.getLastChannelId())
    }

    @Test
    fun testChannelByIndexJump() {
        val channels = listOf("chan_10", "chan_20", "chan_30", "chan_40")
        zapContext.setContext("Top", channels, "chan_10")

        assertEquals("chan_10", zapContext.getChannelByIndex(1))
        assertEquals("chan_30", zapContext.getChannelByIndex(3))
        assertEquals("chan_40", zapContext.getChannelByIndex(4))

        // Out of bounds
        assertNull(zapContext.getChannelByIndex(0))
        assertNull(zapContext.getChannelByIndex(5))
        assertNull(zapContext.getChannelByIndex(99))

        // Locked index
        val locked = setOf("chan_30")
        assertNull(zapContext.getChannelByIndex(3, locked))
    }
}
