package com.nis.nistv

import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.playback.multiview.MultiViewLayoutHelper
import com.nis.nistv.playback.multiview.MultiViewSlot
import com.nis.nistv.playback.multiview.MultiViewSlotState
import com.nis.nistv.playback.multiview.MultiViewState
import org.junit.Assert.*
import org.junit.Test

class MultiViewTest {

    @Test
    fun testPortraitLayoutCalculatesStackedTiles() {
        val screenWidth = 360f
        val screenHeight = 800f

        val layout = MultiViewLayoutHelper.calculateTileDimensions(
            screenWidthDp = screenWidth,
            screenHeightDp = screenHeight,
            isLandscape = false
        )

        assertTrue(layout.isStackedVertically)
        assertEquals(360f, layout.widthDp, 0.01f)
        // 16:9 height for 360dp width = 360 * 9 / 16 = 202.5dp
        assertEquals(202.5f, layout.heightDp, 0.01f)
    }

    @Test
    fun testLandscapeLayoutCalculatesSideBySideTiles() {
        val screenWidth = 800f
        val screenHeight = 360f

        val layout = MultiViewLayoutHelper.calculateTileDimensions(
            screenWidthDp = screenWidth,
            screenHeightDp = screenHeight,
            isLandscape = true
        )

        assertFalse(layout.isStackedVertically)
        // Half screen width = 400dp
        assertEquals(400f, layout.widthDp, 0.01f)
        // 16:9 height for 400dp width = 400 * 9 / 16 = 225dp
        assertEquals(225f, layout.heightDp, 0.01f)
    }

    @Test
    fun testAudioSlotStateMachine() {
        val state = MultiViewState(
            primarySlot = MultiViewSlotState(channel = ChannelEntity(id = "1", name = "Cricket", cleanName = "Cricket")),
            secondarySlot = MultiViewSlotState(channel = ChannelEntity(id = "2", name = "News", cleanName = "News")),
            activeAudioSlot = MultiViewSlot.PRIMARY,
            isActive = true
        )

        assertTrue(state.isDualStreaming)
        assertEquals(MultiViewSlot.PRIMARY, state.activeAudioSlot)

        // Switch audio to secondary
        val secondaryActive = state.copy(activeAudioSlot = MultiViewSlot.SECONDARY)
        assertEquals(MultiViewSlot.SECONDARY, secondaryActive.activeAudioSlot)

        // Switch audio back to primary
        val primaryActive = secondaryActive.copy(activeAudioSlot = MultiViewSlot.PRIMARY)
        assertEquals(MultiViewSlot.PRIMARY, primaryActive.activeAudioSlot)
    }

    @Test
    fun testDualStreamingRequiresBothChannels() {
        val onlyPrimary = MultiViewState(
            primarySlot = MultiViewSlotState(channel = ChannelEntity(id = "1", name = "Cricket", cleanName = "Cricket")),
            secondarySlot = MultiViewSlotState(channel = null),
            isActive = true
        )
        assertFalse(onlyPrimary.isDualStreaming)

        val inactive = MultiViewState(
            primarySlot = MultiViewSlotState(channel = ChannelEntity(id = "1", name = "Cricket", cleanName = "Cricket")),
            secondarySlot = MultiViewSlotState(channel = ChannelEntity(id = "2", name = "News", cleanName = "News")),
            isActive = false
        )
        assertFalse(inactive.isDualStreaming)
    }
}
