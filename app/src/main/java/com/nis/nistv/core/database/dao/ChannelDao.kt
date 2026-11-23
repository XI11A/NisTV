package com.nis.nistv.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.database.entity.StreamSourceEntity
import com.nis.nistv.core.database.model.ChannelWithStreams
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Query("SELECT * FROM channels ORDER BY name ASC")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT COUNT(*) FROM channels")
    suspend fun getChannelsCount(): Int

    @Transaction
    @Query("SELECT * FROM channels WHERE id = :channelId")
    fun getChannelWithStreams(channelId: String): Flow<ChannelWithStreams?>

    @Transaction
    @Query("SELECT * FROM channels WHERE id = :channelId")
    suspend fun getChannelWithStreamsSync(channelId: String): ChannelWithStreams?

    @Query("SELECT * FROM channels WHERE category = :category ORDER BY name ASC")
    fun getChannelsByCategory(category: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE language = :language ORDER BY name ASC")
    fun getChannelsByLanguage(language: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE country = :country ORDER BY name ASC")
    fun getChannelsByCountry(country: String): Flow<List<ChannelEntity>>

    @Query("""
        SELECT * FROM channels 
        WHERE name LIKE '%' || :query || '%' 
           OR cleanName LIKE '%' || :query || '%' 
           OR category LIKE '%' || :query || '%' 
           OR language LIKE '%' || :query || '%'
        ORDER BY name ASC
    """)
    fun searchChannels(query: String): Flow<List<ChannelEntity>>

    @Query("SELECT DISTINCT category FROM channels WHERE category != '' ORDER BY category ASC")
    fun getCategories(): Flow<List<String>>

    @Query("SELECT DISTINCT language FROM channels WHERE language != '' ORDER BY language ASC")
    fun getLanguages(): Flow<List<String>>

    @Query("SELECT DISTINCT country FROM channels WHERE country != '' ORDER BY country ASC")
    fun getCountries(): Flow<List<String>>

    @Query("SELECT * FROM channels LIMIT :limit")
    fun getFeaturedChannels(limit: Int = 10): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId ORDER BY name ASC")
    fun getChannelsByPlaylist(playlistId: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId ORDER BY name ASC")
    suspend fun getChannelsByPlaylistSync(playlistId: String): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE playlistId IS NOT NULL ORDER BY name ASC")
    fun getAllCustomChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE playlistId IS NULL ORDER BY name ASC")
    fun getBuiltInChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT COUNT(*) FROM channels WHERE playlistId IS NULL")
    suspend fun getBuiltInChannelsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreams(streams: List<StreamSourceEntity>)

    @Query("DELETE FROM stream_sources WHERE channelId = :channelId")
    suspend fun deleteStreamsForChannel(channelId: String)

    @Query("DELETE FROM channels WHERE id IN (:ids)")
    suspend fun deleteChannelsByIds(ids: List<String>)
}
