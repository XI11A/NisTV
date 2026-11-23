package com.nis.nistv.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subtitle_tracks",
    foreignKeys = [
        ForeignKey(
            entity = DownloadEntity::class,
            parentColumns = ["id"],
            childColumns = ["downloadId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["downloadId"])
    ]
)
data class SubtitleTrackEntity(
    @PrimaryKey
    val id: String,
    val downloadId: String,
    val path: String,
    val label: String,
    val languageTag: String? = null,
    val mimeType: String,
    val isSelected: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val delayMs: Long = 0L,
    val role: String = "PRIMARY" // "PRIMARY" or "SECONDARY"
)
