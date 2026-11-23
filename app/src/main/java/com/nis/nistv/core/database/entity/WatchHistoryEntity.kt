package com.nis.nistv.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "watch_history",
    primaryKeys = ["profileId", "channelId"],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChannelEntity::class,
            parentColumns = ["id"],
            childColumns = ["channelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["channelId"]),
        Index(value = ["lastWatchedAt"])
    ]
)
data class WatchHistoryEntity(
    val profileId: String,
    val channelId: String,
    val lastWatchedAt: Long = System.currentTimeMillis(),
    val watchDurationMs: Long = 0L
)
