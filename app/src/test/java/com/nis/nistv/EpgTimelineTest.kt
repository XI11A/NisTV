package com.nis.nistv

import com.nis.nistv.data.epg.EpgTimelineHelper
import com.nis.nistv.data.epg.TimelineProgram
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EpgTimelineTest {

    private val windowStart = 10000L
    private val windowEnd = 20000L

    @Test
    fun emptyList_returnsFullWindowPlaceholder() {
        val result = EpgTimelineHelper.buildEpgTimeline(emptyList(), windowStart, windowEnd)
        assertEquals(1, result.size)
        val item = result[0]
        assertEquals("No Programme Information", item.title)
        assertEquals(windowStart, item.startEpochMs)
        assertEquals(windowEnd, item.endEpochMs)
        assertTrue(item.isPlaceholder)
        assertTrue(item.isEstimated)
    }

    @Test
    fun invalidWindow_returnsEmptyList() {
        val result = EpgTimelineHelper.buildEpgTimeline(emptyList(), 20000L, 10000L)
        assertTrue(result.isEmpty())
    }

    @Test
    fun programsOutsideWindow_returnsFullWindowPlaceholder() {
        val programs = listOf(
            TimelineProgram(title = "Old", startEpochMs = 1000L, endEpochMs = 5000L),
            TimelineProgram(title = "Future", startEpochMs = 25000L, endEpochMs = 30000L)
        )
        val result = EpgTimelineHelper.buildEpgTimeline(programs, windowStart, windowEnd)
        assertEquals(1, result.size)
        assertEquals("No Programme Information", result[0].title)
        assertEquals(windowStart, result[0].startEpochMs)
        assertEquals(windowEnd, result[0].endEpochMs)
    }

    @Test
    fun singleProgramWithinWindow_fillsGapsAroundIt() {
        val programs = listOf(
            TimelineProgram(title = "News", startEpochMs = 12000L, endEpochMs = 16000L)
        )
        val result = EpgTimelineHelper.buildEpgTimeline(programs, windowStart, windowEnd)
        assertEquals(3, result.size)

        // Gap before
        assertEquals("No Programme Information", result[0].title)
        assertEquals(10000L, result[0].startEpochMs)
        assertEquals(12000L, result[0].endEpochMs)

        // Program
        assertEquals("News", result[1].title)
        assertEquals(12000L, result[1].startEpochMs)
        assertEquals(16000L, result[1].endEpochMs)

        // Gap after
        assertEquals("No Programme Information", result[2].title)
        assertEquals(16000L, result[2].startEpochMs)
        assertEquals(20000L, result[2].endEpochMs)
    }

    @Test
    fun boundaryClamping_clampsToWindow() {
        val programs = listOf(
            TimelineProgram(title = "Morning Show", startEpochMs = 8000L, endEpochMs = 14000L),
            TimelineProgram(title = "Late Movie", startEpochMs = 18000L, endEpochMs = 24000L)
        )
        val result = EpgTimelineHelper.buildEpgTimeline(programs, windowStart, windowEnd)
        assertEquals(3, result.size)

        assertEquals("Morning Show", result[0].title)
        assertEquals(10000L, result[0].startEpochMs)
        assertEquals(14000L, result[0].endEpochMs)

        // Gap between 14000 and 18000
        assertEquals("No Programme Information", result[1].title)
        assertEquals(14000L, result[1].startEpochMs)
        assertEquals(18000L, result[1].endEpochMs)

        assertEquals("Late Movie", result[2].title)
        assertEquals(18000L, result[2].startEpochMs)
        assertEquals(20000L, result[2].endEpochMs)
    }

    @Test
    fun overlappingPrograms_trimsPreviousProgram() {
        val programs = listOf(
            TimelineProgram(title = "Movie 1", startEpochMs = 10000L, endEpochMs = 15000L),
            TimelineProgram(title = "Movie 2", startEpochMs = 14000L, endEpochMs = 20000L)
        )
        val result = EpgTimelineHelper.buildEpgTimeline(programs, windowStart, windowEnd)
        assertEquals(2, result.size)

        assertEquals("Movie 1", result[0].title)
        assertEquals(10000L, result[0].startEpochMs)
        assertEquals(14000L, result[0].endEpochMs) // Trimmed from 15000 to 14000

        assertEquals("Movie 2", result[1].title)
        assertEquals(14000L, result[1].startEpochMs)
        assertEquals(20000L, result[1].endEpochMs)
    }
}
