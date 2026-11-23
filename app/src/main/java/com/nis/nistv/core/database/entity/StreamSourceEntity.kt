package com.nis.nistv.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stream_sources",
    foreignKeys = [
        ForeignKey(
            entity = ChannelEntity::class,
            parentColumns = ["id"],
            childColumns = ["channelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["channelId"]),
        Index(value = ["priority"])
    ]
)
data class StreamSourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val channelId: String,
    val url: String,
    val referrer: String? = null,
    val userAgent: String? = null,
    val quality: String? = null,
    val labels: String? = null,
    val priority: Int = 0,
    val failCount: Int = 0,
    val lastFailureAt: Long? = null
)
