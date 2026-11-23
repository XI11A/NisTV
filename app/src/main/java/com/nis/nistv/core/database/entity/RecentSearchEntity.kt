package com.nis.nistv.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recent_searches",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profileId", "createdAt"])
    ]
)
data class RecentSearchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: String,
    val query: String,
    val createdAt: Long = System.currentTimeMillis()
)
