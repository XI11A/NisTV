package com.nis.nistv

import com.nis.nistv.core.database.entity.AutopilotRuleEntity
import com.nis.nistv.data.autopilot.AutopilotPlanInput
import com.nis.nistv.data.autopilot.AutopilotPlanner
import com.nis.nistv.data.autopilot.EpisodeLibraryItem
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class AutopilotPlannerTest {

    private val baseRule = AutopilotRuleEntity(
        seriesKey = "series_100",
        enabled = true,
        keepAhead = 2,
        wifiOnly = true,
        chargingOnly = false,
        minFreeBytes = 2L * 1024L * 1024L * 1024L, // 2 GB
        autoDeleteWatched = true,
        deleteAfterDays = 3
    )

    @Test
    fun testPlannerSkipsWhenDisabled() {
        val input = AutopilotPlanInput(
            seriesKey = "series_100",
            rule = baseRule.copy(enabled = false),
            episodes = listOf(EpisodeLibraryItem(seasonNumber = 1, episodeNumber = 1)),
            availableStorageBytes = 10L * 1024L * 1024L * 1024L,
            remainingDailyQuotaBytes = 5L * 1024L * 1024L * 1024L,
            isAfdsLoggedIn = true,
            isOnline = true,
            isWifi = true,
            isCharging = false
        )
        val result = AutopilotPlanner.plan(input)
        assertTrue(result.episodesToFetch.isEmpty())
        assertTrue(result.episodesEligibleForRemoval.isEmpty())
        assertEquals("Autopilot rule disabled", result.reason)
    }

    @Test
    fun testPlannerSkipsWhenLoggedOutOfAfds() {
        val input = AutopilotPlanInput(
            seriesKey = "series_100",
            rule = baseRule,
            episodes = listOf(EpisodeLibraryItem(seasonNumber = 1, episodeNumber = 1)),
            availableStorageBytes = 10L * 1024L * 1024L * 1024L,
            remainingDailyQuotaBytes = 5L * 1024L * 1024L * 1024L,
            isAfdsLoggedIn = false,
            isOnline = true,
            isWifi = true,
            isCharging = false
        )
        val result = AutopilotPlanner.plan(input)
        assertTrue(result.episodesToFetch.isEmpty())
        assertEquals("AFDS authentication required", result.reason)
    }

    @Test
    fun testStorageSafetyFloorEnforced() {
        // Available storage is 1.5 GB, but min free floor is 2.0 GB
        val input = AutopilotPlanInput(
            seriesKey = "series_100",
            rule = baseRule,
            episodes = listOf(EpisodeLibraryItem(seasonNumber = 1, episodeNumber = 1)),
            availableStorageBytes = 1500L * 1024L * 1024L,
            remainingDailyQuotaBytes = 5L * 1024L * 1024L * 1024L,
            isAfdsLoggedIn = true,
            isOnline = true,
            isWifi = true,
            isCharging = false
        )
        val result = AutopilotPlanner.plan(input)
        assertTrue(result.episodesToFetch.isEmpty())
        assertEquals("Available storage below safety floor", result.reason)
    }

    @Test
    fun testDailyQuotaExhaustion() {
        val input = AutopilotPlanInput(
            seriesKey = "series_100",
            rule = baseRule,
            episodes = listOf(EpisodeLibraryItem(seasonNumber = 1, episodeNumber = 1)),
            availableStorageBytes = 10L * 1024L * 1024L * 1024L,
            remainingDailyQuotaBytes = 0L,
            isAfdsLoggedIn = true,
            isOnline = true,
            isWifi = true,
            isCharging = false
        )
        val result = AutopilotPlanner.plan(input)
        assertTrue(result.episodesToFetch.isEmpty())
        assertEquals("Daily download quota exhausted", result.reason)
    }

    @Test
    fun testWifiAndChargingRestrictions() {
        // On cellular when wifiOnly is true
        val cellularInput = AutopilotPlanInput(
            seriesKey = "series_100",
            rule = baseRule.copy(wifiOnly = true),
            episodes = listOf(EpisodeLibraryItem(seasonNumber = 1, episodeNumber = 1)),
            availableStorageBytes = 10L * 1024L * 1024L * 1024L,
            remainingDailyQuotaBytes = 5L * 1024L * 1024L * 1024L,
            isAfdsLoggedIn = true,
            isOnline = true,
            isWifi = false,
            isCharging = false
        )
        val cellularResult = AutopilotPlanner.plan(cellularInput)
        assertTrue(cellularResult.episodesToFetch.isEmpty())
        assertEquals("Waiting for Wi-Fi network", cellularResult.reason)

        // Unplugged when chargingOnly is true
        val unpluggedInput = AutopilotPlanInput(
            seriesKey = "series_100",
            rule = baseRule.copy(wifiOnly = false, chargingOnly = true),
            episodes = listOf(EpisodeLibraryItem(seasonNumber = 1, episodeNumber = 1)),
            availableStorageBytes = 10L * 1024L * 1024L * 1024L,
            remainingDailyQuotaBytes = 5L * 1024L * 1024L * 1024L,
            isAfdsLoggedIn = true,
            isOnline = true,
            isWifi = true,
            isCharging = false
        )
        val unpluggedResult = AutopilotPlanner.plan(unpluggedInput)
        assertTrue(unpluggedResult.episodesToFetch.isEmpty())
        assertEquals("Waiting for device to charge", unpluggedResult.reason)
    }

    @Test
    fun testNormalKeepAheadPlanning() {
        val episodes = listOf(
            EpisodeLibraryItem(downloadId = "dl_1", seasonNumber = 1, episodeNumber = 1, isDownloaded = true, isWatchedCompleted = true),
            EpisodeLibraryItem(downloadId = "dl_2", seasonNumber = 1, episodeNumber = 2, isDownloaded = true, isWatchedCompleted = true),
            EpisodeLibraryItem(seasonNumber = 1, episodeNumber = 3, isDownloaded = false),
            EpisodeLibraryItem(seasonNumber = 1, episodeNumber = 4, isDownloaded = false),
            EpisodeLibraryItem(seasonNumber = 1, episodeNumber = 5, isDownloaded = false)
        )

        val input = AutopilotPlanInput(
            seriesKey = "series_100",
            rule = baseRule.copy(keepAhead = 2),
            episodes = episodes,
            availableStorageBytes = 10L * 1024L * 1024L * 1024L,
            remainingDailyQuotaBytes = 5L * 1024L * 1024L * 1024L,
            isAfdsLoggedIn = true,
            isOnline = true,
            isWifi = true,
            isCharging = false
        )
        val result = AutopilotPlanner.plan(input)
        assertEquals(2, result.episodesToFetch.size)
        assertEquals(Pair(1, 3), result.episodesToFetch[0])
        assertEquals(Pair(1, 4), result.episodesToFetch[1])
    }

    @Test
    fun testAutoDeleteWatchedEligibility() {
        val now = System.currentTimeMillis()
        val fourDaysAgo = now - TimeUnit.DAYS.toMillis(4)
        val oneDayAgo = now - TimeUnit.DAYS.toMillis(1)

        val episodes = listOf(
            EpisodeLibraryItem(
                downloadId = "dl_old_watched",
                seasonNumber = 1,
                episodeNumber = 1,
                isDownloaded = true,
                isWatchedCompleted = true,
                lastWatchedAt = fourDaysAgo
            ),
            EpisodeLibraryItem(
                downloadId = "dl_recent_watched",
                seasonNumber = 1,
                episodeNumber = 2,
                isDownloaded = true,
                isWatchedCompleted = true,
                lastWatchedAt = oneDayAgo
            ),
            EpisodeLibraryItem(
                downloadId = "dl_unwatched",
                seasonNumber = 1,
                episodeNumber = 3,
                isDownloaded = true,
                isWatchedCompleted = false
            )
        )

        val input = AutopilotPlanInput(
            seriesKey = "series_100",
            rule = baseRule.copy(autoDeleteWatched = true, deleteAfterDays = 3),
            episodes = episodes,
            availableStorageBytes = 10L * 1024L * 1024L * 1024L,
            remainingDailyQuotaBytes = 5L * 1024L * 1024L * 1024L,
            isAfdsLoggedIn = true,
            isOnline = true,
            isWifi = true,
            isCharging = false,
            currentTimeMs = now
        )
        val result = AutopilotPlanner.plan(input)

        // Only dl_old_watched was watched > 3 days ago
        assertEquals(1, result.episodesEligibleForRemoval.size)
        assertEquals("dl_old_watched", result.episodesEligibleForRemoval[0].downloadId)
    }
}
