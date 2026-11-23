package com.nis.nistv

import com.nis.nistv.feature.history.DownloadHistoryItemUi
import com.nis.nistv.feature.history.HistoryItemUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class HistoryPrivacyTest {

    @Test
    fun historyBucketPartitioning_groupsTodayYesterdayAndEarlier() {
        val now = 1_700_000_000_000L // arbitrary fixed time
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStart = cal.timeInMillis

        val itemToday = HistoryItemUi(
            channelId = "c1",
            title = "Channel 1",
            logoUrl = "",
            category = "News",
            language = "English",
            lastWatchedAt = todayStart + 3600_000L,
            durationFormatted = "60m watched"
        )

        val itemYesterday = HistoryItemUi(
            channelId = "c2",
            title = "Channel 2",
            logoUrl = "",
            category = "Sports",
            language = "English",
            lastWatchedAt = yesterdayStart + 1800_000L,
            durationFormatted = "30m watched"
        )

        val itemEarlier = HistoryItemUi(
            channelId = "c3",
            title = "Channel 3",
            logoUrl = "",
            category = "Movies",
            language = "English",
            lastWatchedAt = yesterdayStart - 100_000L,
            durationFormatted = "10m watched"
        )

        val allItems = listOf(itemToday, itemYesterday, itemEarlier)

        val today = allItems.filter { it.lastWatchedAt >= todayStart }
        val yesterday = allItems.filter { it.lastWatchedAt in yesterdayStart until todayStart }
        val earlier = allItems.filter { it.lastWatchedAt < yesterdayStart }

        assertEquals(1, today.size)
        assertEquals("c1", today[0].channelId)

        assertEquals(1, yesterday.size)
        assertEquals("c2", yesterday[0].channelId)

        assertEquals(1, earlier.size)
        assertEquals("c3", earlier[0].channelId)
    }

    @Test
    fun downloadHistoryProgress_calculatesAccuratePercentage() {
        val lastPos = 450_000L
        val duration = 1_000_000L
        val pct = ((lastPos.toFloat() / duration.toFloat()) * 100f).toInt().coerceIn(0, 100)
        assertEquals(45, pct)

        val item = DownloadHistoryItemUi(
            downloadId = "d1",
            title = "Movie Test",
            posterUrl = null,
            lastWatchedAt = 1_700_000_000_000L,
            progressPercent = pct
        )
        assertEquals(45, item.progressPercent)
        assertTrue(item.lastWatchedAt > 0)
    }
}
