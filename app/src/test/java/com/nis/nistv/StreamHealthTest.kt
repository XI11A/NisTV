package com.nis.nistv

import com.nis.nistv.core.database.entity.StreamHealthEntity
import com.nis.nistv.core.database.entity.StreamSourceEntity
import com.nis.nistv.data.health.ReliabilityStatus
import com.nis.nistv.data.health.StreamHealthKeyHelper
import com.nis.nistv.data.health.StreamHealthScorer
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class StreamHealthTest {

    @Test
    fun testUnprobedStreamHasNeutralScore() {
        val score = StreamHealthScorer.calculateScore(null)
        assertEquals(0.5, score, 0.01)
    }

    @Test
    fun testHealthyStreamHasHighScore() {
        val now = System.currentTimeMillis()
        val healthy = StreamHealthEntity(
            streamKey = "key_1",
            channelId = "chan_1",
            successCount = 95,
            failureCount = 5,
            avgStartupMs = 1200L,
            lastSuccessAt = now - 1000L,
            lastFailureAt = now - 50000L,
            consecutiveFailures = 0
        )
        val score = StreamHealthScorer.calculateScore(healthy, now)
        assertTrue("Healthy stream score should be > 0.85, got $score", score > 0.85)

        val reliability = StreamHealthScorer.resolveReliability(listOf(healthy), now)
        assertEquals(ReliabilityStatus.HIGH, reliability)
    }

    @Test
    fun testConsecutiveFailuresPenalizeScore() {
        val now = System.currentTimeMillis()
        val failing = StreamHealthEntity(
            streamKey = "key_fail",
            channelId = "chan_1",
            successCount = 50,
            failureCount = 10,
            avgStartupMs = 3000L,
            lastSuccessAt = now - TimeUnit.DAYS.toMillis(1),
            lastFailureAt = now - 1000L,
            consecutiveFailures = 4
        )
        val score = StreamHealthScorer.calculateScore(failing, now)
        // Raw rate is 50/60 = 0.833, but consecutive penalty of 4 * 0.12 = 0.48 brings it down to ~0.35
        assertTrue("Score should be reduced by consecutive failure penalty, got $score", score < 0.50)
    }

    @Test
    fun testRecencyDecayAfter30Days() {
        val now = System.currentTimeMillis()
        val freshSuccess = StreamHealthEntity(
            streamKey = "key_fresh",
            channelId = "chan_1",
            successCount = 10,
            failureCount = 0,
            lastSuccessAt = now - TimeUnit.DAYS.toMillis(5)
        )
        val staleSuccess = StreamHealthEntity(
            streamKey = "key_stale",
            channelId = "chan_1",
            successCount = 10,
            failureCount = 0,
            lastSuccessAt = now - TimeUnit.DAYS.toMillis(45) // 15 days past 30-day threshold
        )

        val freshScore = StreamHealthScorer.calculateScore(freshSuccess, now)
        val staleScore = StreamHealthScorer.calculateScore(staleSuccess, now)

        assertTrue("Stale data must decay compared to fresh data: $staleScore < $freshScore", staleScore < freshScore)
    }

    @Test
    fun testDeadChannelDetectionRule() {
        val now = System.currentTimeMillis()
        val threeDaysAgo = now - TimeUnit.DAYS.toMillis(4)

        // 5 failures across 4 days -> Dead
        val deadHealth = StreamHealthEntity(
            streamKey = "key_dead",
            channelId = "chan_dead",
            successCount = 2,
            failureCount = 10,
            lastSuccessAt = threeDaysAgo,
            lastFailureAt = now,
            consecutiveFailures = 5
        )
        assertTrue(StreamHealthScorer.isDeadStream(deadHealth, now))

        // 5 failures but all occurred within 1 hour -> Not dead yet
        val recentSpike = StreamHealthEntity(
            streamKey = "key_spike",
            channelId = "chan_spike",
            successCount = 10,
            failureCount = 5,
            lastSuccessAt = now - TimeUnit.HOURS.toMillis(2),
            lastFailureAt = now,
            consecutiveFailures = 5
        )
        assertFalse(StreamHealthScorer.isDeadStream(recentSpike, now))

        // Only 3 failures over 4 days -> Not dead (below failure threshold)
        val intermittent = deadHealth.copy(consecutiveFailures = 3)
        assertFalse(StreamHealthScorer.isDeadStream(intermittent, now))
    }

    @Test
    fun testSortStreamSourcesByHealth() {
        val source1 = StreamSourceEntity(id = 1, channelId = "chan_1", url = "https://stream1.m3u8", priority = 1)
        val source2 = StreamSourceEntity(id = 2, channelId = "chan_1", url = "https://stream2.m3u8", priority = 2)
        val source3 = StreamSourceEntity(id = 3, channelId = "chan_1", url = "https://stream3.m3u8", priority = 3)

        val key1 = StreamHealthKeyHelper.computeStreamKey(source1.url)
        val key2 = StreamHealthKeyHelper.computeStreamKey(source2.url)
        val key3 = StreamHealthKeyHelper.computeStreamKey(source3.url)

        val now = System.currentTimeMillis()
        val healthMap = mapOf(
            // Stream 1 has high failure count
            key1 to StreamHealthEntity(streamKey = key1, channelId = "chan_1", successCount = 2, failureCount = 8, consecutiveFailures = 3),
            // Stream 2 is super reliable
            key2 to StreamHealthEntity(streamKey = key2, channelId = "chan_1", successCount = 98, failureCount = 1, consecutiveFailures = 0),
            // Stream 3 is moderate
            key3 to StreamHealthEntity(streamKey = key3, channelId = "chan_1", successCount = 50, failureCount = 20, consecutiveFailures = 0)
        )

        val sorted = StreamHealthScorer.sortStreamSources(listOf(source1, source2, source3), healthMap, now)

        // Stream 2 should be first, then Stream 3, then failing Stream 1
        assertEquals("Stream 2 should be ranked #1", source2.id, sorted[0].id)
        assertEquals("Stream 3 should be ranked #2", source3.id, sorted[1].id)
        assertEquals("Stream 1 should be ranked #3", source1.id, sorted[2].id)
    }
}
