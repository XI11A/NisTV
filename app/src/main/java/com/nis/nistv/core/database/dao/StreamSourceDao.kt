package com.nis.nistv.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nis.nistv.core.database.entity.StreamSourceEntity

@Dao
interface StreamSourceDao {

    @Query("SELECT * FROM stream_sources WHERE channelId = :channelId ORDER BY priority ASC, failCount ASC, id ASC")
    suspend fun getStreamsForChannel(channelId: String): List<StreamSourceEntity>

    @Query("UPDATE stream_sources SET failCount = failCount + 1, lastFailureAt = :timestamp WHERE id = :streamId")
    suspend fun recordStreamFailure(streamId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE stream_sources SET failCount = 0 WHERE id = :streamId")
    suspend fun resetStreamFailure(streamId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreams(streams: List<StreamSourceEntity>)
}
