package com.nis.nistv.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tracks stream URL health telemetry across sessions to enable self-healing
 * stream ordering, reliability indicators, and dead-channel detection.
 */
@Entity(
    tableName = "stream_health",
    indices = [
        Index(value = ["channelId"], name = "index_stream_health_channelId")
    ]
)
data class StreamHealthEntity(
    @PrimaryKey
    val streamKey: String, // MD5 / SHA-256 hash or canonical representation of stream URL
    val channelId: String,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val avgStartupMs: Long = 0L,
    val lastSuccessAt: Long = 0L,
    val lastFailureAt: Long = 0L,
    val consecutiveFailures: Int = 0
)
