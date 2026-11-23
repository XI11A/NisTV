package com.nis.nistv.data.health

import com.nis.nistv.core.database.entity.StreamHealthEntity
import com.nis.nistv.core.database.entity.StreamSourceEntity
import kotlin.math.exp

enum class ReliabilityStatus {
    HIGH,     // High reliability (Green)
    MEDIUM,   // Moderate reliability (Amber)
    LOW,      // Poor reliability (Red / Muted)
    DEAD,     // Completely dead (Exceeded threshold)
    UNKNOWN   // No telemetry recorded yet (Gray)
}

object StreamHealthScorer {

    const val THIRTY_DAYS_MS = 30L * 24L * 60L * 60L * 1000L
    const val THREE_DAYS_MS = 3L * 24L * 60L * 60L * 1000L
    const val DEAD_FAILURE_THRESHOLD = 5

    /**
     * Calculates a health score in [0.0, 1.0] for a stream health entry.
     * Incorporates raw success rate, consecutive failure penalty, startup time, and 30-day recency decay.
     */
    fun calculateScore(
        health: StreamHealthEntity?,
        nowMs: Long = System.currentTimeMillis()
    ): Double {
        if (health == null) return 0.5 // Neutral baseline for unprobed streams

        val totalAttempts = health.successCount + health.failureCount
        if (totalAttempts == 0) return 0.5

        val rawSuccessRate = health.successCount.toDouble() / totalAttempts.toDouble()

        // Consecutive failure penalty: drops score quickly if currently failing
        val consecutivePenalty = (health.consecutiveFailures * 0.12).coerceAtMost(0.60)

        // Startup latency adjustment: fast startups (< 2.5s) get a small boost, sluggish ones (> 8s) get penalized
        val startupAdjustment = when {
            health.avgStartupMs in 1..2500 -> 0.05
            health.avgStartupMs > 8000 -> -0.05
            else -> 0.0
        }

        val baseScore = (rawSuccessRate - consecutivePenalty + startupAdjustment).coerceIn(0.0, 1.0)

        // Recency decay: data older than 30 days decays exponentially towards neutral
        val lastEventAt = maxOf(health.lastSuccessAt, health.lastFailureAt)
        if (lastEventAt <= 0L) return baseScore

        val ageMs = (nowMs - lastEventAt).coerceAtLeast(0L)
        val decayFactor = if (ageMs > THIRTY_DAYS_MS) {
            val overdueDays = (ageMs - THIRTY_DAYS_MS).toDouble() / (24.0 * 60.0 * 60.0 * 1000.0)
            exp(-overdueDays / 15.0).coerceIn(0.1, 1.0)
        } else {
            1.0
        }

        return (baseScore * decayFactor).coerceIn(0.0, 1.0)
    }

    /**
     * Checks if a stream or channel is considered dead:
     * - At least 5 consecutive failures
     * - Failures persisted over at least 3 days (either lastSuccessAt == 0 or lastFailureAt - lastSuccessAt >= 3 days)
     */
    fun isDeadStream(
        health: StreamHealthEntity?,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (health == null) return false
        if (health.consecutiveFailures < DEAD_FAILURE_THRESHOLD) return false

        val failureSpan = if (health.lastSuccessAt > 0L) {
            health.lastFailureAt - health.lastSuccessAt
        } else {
            // Never succeeded; check duration since earliest tracked event
            nowMs - health.lastFailureAt.coerceAtLeast(1L)
        }

        return failureSpan >= THREE_DAYS_MS
    }

    /**
     * Resolves the channel reliability classification across all its streams.
     */
    fun resolveReliability(
        streams: List<StreamHealthEntity>,
        nowMs: Long = System.currentTimeMillis()
    ): ReliabilityStatus {
        if (streams.isEmpty()) return ReliabilityStatus.UNKNOWN

        // If all available streams are dead, the channel is dead
        if (streams.all { isDeadStream(it, nowMs) }) {
            return ReliabilityStatus.DEAD
        }

        // Use the highest score among available streams
        val bestScore = streams.maxOfOrNull { calculateScore(it, nowMs) } ?: 0.5
        return when {
            bestScore >= 0.70 -> ReliabilityStatus.HIGH
            bestScore >= 0.40 -> ReliabilityStatus.MEDIUM
            else -> ReliabilityStatus.LOW
        }
    }

    /**
     * Orders a channel's stream sources by health score descending, preserving stable tie-breakers.
     */
    fun sortStreamSources(
        sources: List<StreamSourceEntity>,
        healthMap: Map<String, StreamHealthEntity>,
        nowMs: Long = System.currentTimeMillis()
    ): List<StreamSourceEntity> {
        if (sources.size <= 1) return sources

        return sources.sortedWith(
            compareByDescending<StreamSourceEntity> { source ->
                val key = StreamHealthKeyHelper.computeStreamKey(source.url)
                val health = healthMap[key]
                calculateScore(health, nowMs)
            }.thenBy { it.priority }.thenBy { it.id }
        )
    }
}
