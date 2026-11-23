package com.nis.nistv.data.recommendation

import java.util.Calendar
import kotlin.random.Random

data class CandidatePick(
    val id: String,
    val tmdbId: Long? = null,
    val title: String,
    val mediaType: String, // "movie", "tv", "channel"
    val posterUrl: String? = null,
    val runtimeMinutes: Int? = null,
    val affinityScore: Double = 1.0,
    val isDownloaded: Boolean = false,
    val isWatched: Boolean = false
)

data class TimeContext(
    val hourOfDay: Int,
    val isWeekend: Boolean
) {
    companion object {
        fun current(currentTimeMs: Long = System.currentTimeMillis()): TimeContext {
            val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMs }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val day = cal.get(Calendar.DAY_OF_WEEK)
            val isWeekend = day == Calendar.FRIDAY && hour >= 18 ||
                    day == Calendar.SATURDAY ||
                    day == Calendar.SUNDAY
            return TimeContext(hour, isWeekend)
        }
    }
}

object TonightPicksHelper {

    /**
     * Ranks and picks 5 items blended with time-of-day affinity.
     */
    fun rankTonightPicks(
        candidates: List<CandidatePick>,
        timeContext: TimeContext,
        isOnline: Boolean
    ): List<CandidatePick> {
        // 1. Exclude watched items
        val unwatched = candidates.filter { !it.isWatched }

        // 2. Offline filter: only downloaded items when offline
        val pool = if (!isOnline) {
            unwatched.filter { it.isDownloaded }
        } else {
            unwatched
        }

        if (pool.isEmpty()) return emptyList()

        // 3. Time-of-day scoring adjustments
        val isLateNight = timeContext.hourOfDay >= 22 || timeContext.hourOfDay < 4

        val scored = pool.map { candidate ->
            var multiplier = 1.0

            if (isLateNight) {
                // Late night: prefer shorter content (episodes or < 100 min movies)
                val runtime = candidate.runtimeMinutes ?: 45
                multiplier = when {
                    candidate.mediaType == "tv" -> 1.5
                    runtime <= 30 -> 1.45
                    runtime <= 90 -> 1.3
                    runtime > 130 -> 0.6
                    else -> 1.0
                }
            } else if (timeContext.isWeekend) {
                // Weekend: prefer feature-length cinematic movies
                val runtime = candidate.runtimeMinutes ?: 100
                multiplier = when {
                    runtime >= 110 && candidate.mediaType == "movie" -> 1.35
                    else -> 1.0
                }
            }

            val finalScore = candidate.affinityScore * multiplier
            Pair(candidate, finalScore)
        }

        return scored
            .sortedByDescending { it.second }
            .map { it.first }
            .take(5)
    }

    /**
     * Picks one item from [picks] weighted by affinity score using a seedable [Random].
     */
    fun pickSurpriseItem(
        picks: List<CandidatePick>,
        random: Random = Random.Default
    ): CandidatePick? {
        if (picks.isEmpty()) return null
        if (picks.size == 1) return picks.first()

        val totalWeight = picks.sumOf { it.affinityScore.coerceAtLeast(0.1) }
        val r = random.nextDouble() * totalWeight
        var cumulative = 0.0

        for (item in picks) {
            cumulative += item.affinityScore.coerceAtLeast(0.1)
            if (r <= cumulative) {
                return item
            }
        }

        return picks.last()
    }
}
