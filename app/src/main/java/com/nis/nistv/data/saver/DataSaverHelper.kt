package com.nis.nistv.data.saver

import com.nis.nistv.core.database.entity.DataUsageDailyEntity

enum class ThresholdAlert {
    NONE,
    EIGHTY_PERCENT,
    ONE_HUNDRED_PERCENT
}

object DataSaverHelper {

    /**
     * Resolves the maximum video resolution bounding box for metered cellular connections.
     * Returns null if unmetered or if cap is "OFF".
     */
    fun resolveMaxVideoSize(cap: String, isMetered: Boolean): Pair<Int, Int>? {
        if (!isMetered) return null
        return when (cap.uppercase()) {
            "480P" -> Pair(854, 480)
            "720P" -> Pair(1280, 720)
            else -> null
        }
    }

    /**
     * Calculates whether a data usage threshold alert should fire.
     * Alerts at 80% and 100% of the monthly cellular quota.
     */
    fun calculateThresholdAlert(usedBytes: Long, limitBytes: Long): ThresholdAlert {
        if (limitBytes <= 0L || usedBytes <= 0L) return ThresholdAlert.NONE

        val fraction = usedBytes.toDouble() / limitBytes.toDouble()
        return when {
            fraction >= 1.0 -> ThresholdAlert.ONE_HUNDRED_PERCENT
            fraction >= 0.80 -> ThresholdAlert.EIGHTY_PERCENT
            else -> ThresholdAlert.NONE
        }
    }

    /**
     * Aggregates total bandwidth across records grouped by network type ("WIFI", "CELLULAR").
     */
    fun aggregateByNetwork(records: List<DataUsageDailyEntity>): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        for (r in records) {
            val total = r.streamBytes + r.downloadBytes
            result[r.networkType] = (result[r.networkType] ?: 0L) + total
        }
        return result
    }

    /**
     * Aggregates total bandwidth across records grouped by date string.
     */
    fun aggregateByDay(records: List<DataUsageDailyEntity>): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        for (r in records) {
            val total = r.streamBytes + r.downloadBytes
            result[r.date] = (result[r.date] ?: 0L) + total
        }
        return result
    }

    /**
     * Formats bytes into a human-readable storage string (e.g., "1.2 GB", "450 MB").
     */
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 MB"
        val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1.0) {
            String.format(java.util.Locale.US, "%.1f GB", gb)
        } else {
            val mb = bytes.toDouble() / (1024.0 * 1024.0)
            String.format(java.util.Locale.US, "%.0f MB", mb)
        }
    }
}
