package com.nis.nistv.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nis.nistv.core.database.entity.MediaInteractionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaInteractionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordInteraction(interaction: MediaInteractionEntity): Long

    @Query("SELECT * FROM media_interactions WHERE profileId = :profileId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentInteractions(profileId: String, limit: Int = 40): Flow<List<MediaInteractionEntity>>

    @Query("SELECT * FROM media_interactions ORDER BY timestamp DESC LIMIT :limit")
    fun getAllRecentInteractions(limit: Int = 40): Flow<List<MediaInteractionEntity>>

    @Query("SELECT * FROM media_interactions WHERE profileId = :profileId AND mediaType = :mediaType ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentInteractionsByType(profileId: String, mediaType: String, limit: Int = 20): Flow<List<MediaInteractionEntity>>

    @Query("SELECT * FROM media_interactions WHERE profileId = :profileId AND mediaType != 'CHANNEL' ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTmdbInteractions(profileId: String, limit: Int = 20): Flow<List<MediaInteractionEntity>>

    @Query("SELECT COUNT(*) FROM media_interactions WHERE profileId = :profileId")
    fun getInteractionCount(profileId: String): Flow<Int>

    @Query("DELETE FROM media_interactions WHERE profileId = :profileId")
    suspend fun clearForProfile(profileId: String)

    @Query("DELETE FROM media_interactions")
    suspend fun clearAll()
}
