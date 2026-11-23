package com.nis.nistv.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.database.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

data class WatchHistoryItemWithChannel(
    val channelId: String,
    val lastWatchedAt: Long,
    val watchDurationMs: Long,
    val channelName: String,
    val channelLogoUrl: String,
    val category: String,
    val language: String
)

@Dao
interface WatchHistoryDao {

    @Query("""
        SELECT c.* FROM channels c
        INNER JOIN watch_history w ON c.id = w.channelId
        WHERE w.profileId = :profileId
        ORDER BY w.lastWatchedAt DESC
        LIMIT :limit
    """)
    fun getRecentlyWatchedChannels(profileId: String, limit: Int = 20): Flow<List<ChannelEntity>>

    @Query("""
        SELECT w.channelId, w.lastWatchedAt, w.watchDurationMs,
               c.name AS channelName, c.logoUrl AS channelLogoUrl,
               c.category AS category, c.language AS language
        FROM watch_history w
        INNER JOIN channels c ON w.channelId = c.id
        WHERE w.profileId = :profileId
        ORDER BY w.lastWatchedAt DESC
    """)
    fun getWatchHistoryWithDetails(profileId: String = "default"): Flow<List<WatchHistoryItemWithChannel>>

    @Query("DELETE FROM watch_history WHERE profileId = :profileId AND channelId = :channelId")
    suspend fun deleteWatchHistoryItem(profileId: String, channelId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordWatch(history: WatchHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchHistoryItems(items: List<WatchHistoryEntity>)

    @Query("SELECT * FROM watch_history")
    suspend fun getAllWatchHistorySync(): List<WatchHistoryEntity>

    @Query("SELECT * FROM watch_history WHERE profileId = :profileId")
    suspend fun getAllWatchHistoryForProfileSync(profileId: String): List<WatchHistoryEntity>

    @Query("DELETE FROM watch_history WHERE profileId = :profileId")
    suspend fun clearWatchHistory(profileId: String)
}
