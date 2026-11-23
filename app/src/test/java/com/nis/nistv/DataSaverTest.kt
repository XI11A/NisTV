package com.nis.nistv

import com.nis.nistv.core.database.entity.DataUsageDailyEntity
import com.nis.nistv.data.saver.DataSaverHelper
import com.nis.nistv.data.saver.ThresholdAlert
import org.junit.Assert.*
import org.junit.Test

class DataSaverTest {

    @Test
    fun testResolveMaxVideoSizeWhenUnmetered() {
        // Even if 480P is selected, unmetered (Wi-Fi) should not cap video resolution
        assertNull(DataSaverHelper.resolveMaxVideoSize("480P", isMetered = false))
        assertNull(DataSaverHelper.resolveMaxVideoSize("720P", isMetered = false))
    }

    @Test
    fun testResolveMaxVideoSizeWhenMetered() {
        val cap480 = DataSaverHelper.resolveMaxVideoSize("480P", isMetered = true)
        assertNotNull(cap480)
        assertEquals(Pair(854, 480), cap480)

        val cap720 = DataSaverHelper.resolveMaxVideoSize("720P", isMetered = true)
        assertNotNull(cap720)
        assertEquals(Pair(1280, 720), cap720)

        // OFF should return null
        assertNull(DataSaverHelper.resolveMaxVideoSize("OFF", isMetered = true))
    }

    @Test
    fun testCalculateThresholdAlert() {
        val limit = 10L * 1024L * 1024L * 1024L // 10 GB

        // 5 GB used (50%) -> NONE
        assertEquals(ThresholdAlert.NONE, DataSaverHelper.calculateThresholdAlert(5L * 1024L * 1024L * 1024L, limit))

        // 8.1 GB used (81%) -> EIGHTY_PERCENT
        assertEquals(ThresholdAlert.EIGHTY_PERCENT, DataSaverHelper.calculateThresholdAlert((8.1 * 1024 * 1024 * 1024).toLong(), limit))

        // 10.5 GB used (105%) -> ONE_HUNDRED_PERCENT
        assertEquals(ThresholdAlert.ONE_HUNDRED_PERCENT, DataSaverHelper.calculateThresholdAlert((10.5 * 1024 * 1024 * 1024).toLong(), limit))

        // No limit configured (0L) -> NONE
        assertEquals(ThresholdAlert.NONE, DataSaverHelper.calculateThresholdAlert(15L * 1024L * 1024L * 1024L, 0L))
    }

    @Test
    fun testAggregateByNetwork() {
        val records = listOf(
            DataUsageDailyEntity(date = "2026-11-01", networkType = "WIFI", streamBytes = 1000L, downloadBytes = 500L),
            DataUsageDailyEntity(date = "2026-11-01", networkType = "CELLULAR", streamBytes = 200L, downloadBytes = 100L),
            DataUsageDailyEntity(date = "2026-11-02", networkType = "WIFI", streamBytes = 2000L, downloadBytes = 1000L)
        )

        val aggregated = DataSaverHelper.aggregateByNetwork(records)
        assertEquals(4500L, aggregated["WIFI"])
        assertEquals(300L, aggregated["CELLULAR"])
    }

    @Test
    fun testAggregateByDay() {
        val records = listOf(
            DataUsageDailyEntity(date = "2026-11-01", networkType = "WIFI", streamBytes = 1000L, downloadBytes = 500L),
            DataUsageDailyEntity(date = "2026-11-01", networkType = "CELLULAR", streamBytes = 200L, downloadBytes = 100L),
            DataUsageDailyEntity(date = "2026-11-02", networkType = "WIFI", streamBytes = 2000L, downloadBytes = 0L)
        )

        val aggregated = DataSaverHelper.aggregateByDay(records)
        assertEquals(1800L, aggregated["2026-11-01"])
        assertEquals(2000L, aggregated["2026-11-02"])
    }
}
