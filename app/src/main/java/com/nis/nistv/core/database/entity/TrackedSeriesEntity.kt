package com.nis.nistv.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracked_series",
    indices = [
        Index(value = ["notifyEnabled"])
    ]
)
data class TrackedSeriesEntity(
    @PrimaryKey
    val seriesKey: String,
    val tmdbId: Long,
    val name: String,
    val notifyEnabled: Boolean = true,
    val lastKnownEpisodeKey: String? = null,
    val nextAirDate: String? = null,
    val lastNotifiedKey: String? = null,
    val lastCheckedAt: Long = 0L
)
