package com.nis.nistv.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "channels",
    indices = [
        Index(value = ["category"]),
        Index(value = ["language"]),
        Index(value = ["name"]),
        Index(value = ["cleanName"]),
        Index(value = ["playlistId"])
    ]
)
data class ChannelEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val cleanName: String,
    val altNames: String = "",
    val country: String = "IN",
    val network: String = "",
    val website: String = "",
    val language: String = "Hindi",
    val category: String = "Entertainment",
    val logoUrl: String = "",
    val resolution: String = "HD",
    val isNsfw: Boolean = false,
    val labels: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val playlistId: String? = null
)
