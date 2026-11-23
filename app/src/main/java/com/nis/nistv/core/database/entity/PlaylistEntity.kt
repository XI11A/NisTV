package com.nis.nistv.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val sourceType: String, // "URL" or "FILE"
    val url: String,
    val lastSyncedAt: Long = 0L,
    val channelCount: Int = 0,
    val autoRefresh: Boolean = true,
    val enabled: Boolean = true
)
