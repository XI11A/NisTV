package com.nis.nistv.data.autopilot

import com.nis.nistv.core.database.entity.AutopilotRuleEntity

data class EpisodeLibraryItem(
    val downloadId: String? = null,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val isWatchedCompleted: Boolean = false,
    val lastWatchedAt: Long = 0L,
    val fileSizeBytes: Long = 0L
)

data class EpisodeRemovalCandidate(
    val downloadId: String,
    val seasonNumber: Int = 0,
    val episodeNumber: Int = 0
)

data class AutopilotPlanInput(
    val seriesKey: String,
    val rule: AutopilotRuleEntity,
    val episodes: List<EpisodeLibraryItem>,
    val availableStorageBytes: Long,
    val remainingDailyQuotaBytes: Long,
    val isAfdsLoggedIn: Boolean,
    val isOnline: Boolean,
    val isWifi: Boolean,
    val isCharging: Boolean,
    val currentTimeMs: Long = System.currentTimeMillis()
)

data class AutopilotPlanResult(
    val episodesToFetch: List<Pair<Int, Int>> = emptyList(),
    val episodesToRemove: List<String> = emptyList(),
    val episodesPendingRemoval: List<String> = emptyList(),
    val skippedReason: String? = null
) {
    val reason: String? get() = skippedReason
    val episodesEligibleForRemoval: List<EpisodeRemovalCandidate>
        get() = episodesToRemove.map { EpisodeRemovalCandidate(it) }
}

object AutopilotPlanner {

    private const val ESTIMATED_EPISODE_BYTES = 750L * 1024L * 1024L // 750 MB fallback estimate
    private const val ONE_DAY_MS = 24L * 60L * 60L * 1000L

    fun plan(input: AutopilotPlanInput): AutopilotPlanResult {
        val rule = input.rule
        if (!rule.enabled) {
            return AutopilotPlanResult(skippedReason = "Autopilot rule disabled")
        }
        if (!input.isAfdsLoggedIn) {
            return AutopilotPlanResult(skippedReason = "AFDS authentication required")
        }
        if (!input.isOnline) {
            return AutopilotPlanResult(skippedReason = "Device is offline")
        }
        if (rule.wifiOnly && !input.isWifi) {
            return AutopilotPlanResult(skippedReason = "Waiting for Wi-Fi network")
        }
        if (rule.chargingOnly && !input.isCharging) {
            return AutopilotPlanResult(skippedReason = "Waiting for device to charge")
        }
        if (input.availableStorageBytes <= rule.minFreeBytes) {
            return AutopilotPlanResult(skippedReason = "Available storage below safety floor")
        }
        if (input.remainingDailyQuotaBytes <= 0) {
            return AutopilotPlanResult(skippedReason = "Daily download quota exhausted")
        }

        // 1. Sort all episodes chronologically by season and episode
        val sortedEpisodes = input.episodes.sortedWith(
            compareBy<EpisodeLibraryItem> { it.seasonNumber }.thenBy { it.episodeNumber }
        )

        // 2. Determine which unwatched episodes need to be fetched up to keepAhead
        val unwatched = sortedEpisodes.filter { !it.isWatchedCompleted }
        val targetUpcoming = unwatched.take(rule.keepAhead.coerceIn(1, 10))

        var currentFreeBytes = input.availableStorageBytes
        var currentQuotaBytes = input.remainingDailyQuotaBytes
        val toFetch = mutableListOf<Pair<Int, Int>>()

        for (ep in targetUpcoming) {
            if (!ep.isDownloaded && !ep.isDownloading) {
                val estimatedSize = if (ep.fileSizeBytes > 0) ep.fileSizeBytes else ESTIMATED_EPISODE_BYTES
                val spaceAfter = currentFreeBytes - estimatedSize
                if (spaceAfter < rule.minFreeBytes) {
                    break // Cannot download more without crossing floor
                }
                if (currentQuotaBytes < estimatedSize) {
                    break // Cannot download more without exceeding quota
                }

                toFetch.add(Pair(ep.seasonNumber, ep.episodeNumber))
                currentFreeBytes -= estimatedSize
                currentQuotaBytes -= estimatedSize
            }
        }

        // 3. Determine watched episodes eligible for auto-deletion
        val toRemove = mutableListOf<String>()
        val pendingRemoval = mutableListOf<String>()

        if (rule.autoDeleteWatched && rule.deleteAfterDays > 0) {
            val deleteThreshold = input.currentTimeMs - (rule.deleteAfterDays * ONE_DAY_MS)
            val warningThreshold = deleteThreshold + ONE_DAY_MS // within 24 hours of deletion

            for (ep in sortedEpisodes) {
                if (ep.isDownloaded && ep.isWatchedCompleted && !ep.downloadId.isNullOrBlank()) {
                    if (ep.lastWatchedAt in 1..deleteThreshold) {
                        toRemove.add(ep.downloadId)
                    } else if (ep.lastWatchedAt in (deleteThreshold + 1)..warningThreshold) {
                        pendingRemoval.add(ep.downloadId)
                    }
                }
            }
        }

        return AutopilotPlanResult(
            episodesToFetch = toFetch,
            episodesToRemove = toRemove,
            episodesPendingRemoval = pendingRemoval
        )
    }
}
