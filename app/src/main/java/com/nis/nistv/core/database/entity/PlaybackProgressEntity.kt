package com.nis.nistv.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "playback_progress",
    primaryKeys = ["profileId", "downloadId"],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DownloadEntity::class,
            parentColumns = ["id"],
            childColumns = ["downloadId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["downloadId"])
    ]
)
data class PlaybackProgressEntity(
    val profileId: String,
    val downloadId: String,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastWatchedAt: Long = 0L,
    val completed: Boolean = false,
    val hidden: Boolean = false
)
