package com.nis.nistv.core.database.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Daily aggregation of streamed and downloaded data bandwidth per network type.
 */
@Entity(
    tableName = "data_usage_daily",
    primaryKeys = ["date", "networkType"],
    indices = [
        Index(value = ["date"], name = "index_data_usage_daily_date")
    ]
)
data class DataUsageDailyEntity(
    val date: String,          // ISO-8601 date "YYYY-MM-DD"
    val networkType: String,   // "WIFI", "CELLULAR", "OTHER"
    val streamBytes: Long = 0L,
    val downloadBytes: Long = 0L
)
