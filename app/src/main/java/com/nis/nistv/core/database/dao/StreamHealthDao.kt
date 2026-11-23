package com.nis.nistv.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nis.nistv.core.database.entity.StreamHealthEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StreamHealthDao {

    @Query("SELECT * FROM stream_health WHERE streamKey = :streamKey LIMIT 1")
    suspend fun getHealth(streamKey: String): StreamHealthEntity?

    @Query("SELECT * FROM stream_health WHERE channelId = :channelId")
    suspend fun getHealthForChannelSync(channelId: String): List<StreamHealthEntity>

    @Query("SELECT * FROM stream_health WHERE channelId = :channelId")
    fun getHealthForChannel(channelId: String): Flow<List<StreamHealthEntity>>

    @Query("SELECT * FROM stream_health")
    suspend fun getAllHealthSync(): List<StreamHealthEntity>

    @Query("SELECT * FROM stream_health")
    fun getAllHealth(): Flow<List<StreamHealthEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: StreamHealthEntity)

    @Query("DELETE FROM stream_health WHERE channelId = :channelId")
    suspend fun resetHealthForChannel(channelId: String)

    @Query("DELETE FROM stream_health WHERE streamKey = :streamKey")
    suspend fun deleteHealth(streamKey: String)

    @Query("DELETE FROM stream_health")
    suspend fun clearAll()
}
