package com.nis.nistv.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "autopilot_rules")
data class AutopilotRuleEntity(
    @PrimaryKey
    val seriesKey: String,
    val enabled: Boolean = false,
    val keepAhead: Int = 1, // 1, 2, 3, or 5 episodes
    val wifiOnly: Boolean = true,
    val chargingOnly: Boolean = false,
    val minFreeBytes: Long = 2L * 1024L * 1024L * 1024L, // 2 GB floor
    val autoDeleteWatched: Boolean = false,
    val deleteAfterDays: Int = 7,
    val preferredQuality: String = "1080p",
    val lastRunAt: Long = 0L,
    val lastResult: String? = null
)
