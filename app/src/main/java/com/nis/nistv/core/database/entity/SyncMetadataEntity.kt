package com.nis.nistv.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    val resourceKey: String,
    val etag: String? = null,
    val lastModified: String? = null,
    val lastSyncTimestamp: Long = 0L,
    val status: String = "SUCCESS",
    val errorMessage: String? = null
)
