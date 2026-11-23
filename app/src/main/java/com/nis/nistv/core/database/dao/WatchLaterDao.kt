package com.nis.nistv.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nis.nistv.core.database.entity.WatchLaterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchLaterDao {

    @Query("SELECT * FROM watch_later WHERE profileId = :profileId ORDER BY addedAt DESC")
    fun getAllWatchLater(profileId: String = "default"): Flow<List<WatchLaterEntity>>

    @Query("SELECT * FROM watch_later WHERE profileId = :profileId AND mediaType = :mediaType ORDER BY addedAt DESC")
    fun getWatchLaterByType(profileId: String, mediaType: String): Flow<List<WatchLaterEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watch_later WHERE profileId = :profileId AND id = :id LIMIT 1)")
    fun isInWatchLater(profileId: String, id: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM watch_later WHERE profileId = :profileId AND id = :id LIMIT 1)")
    suspend fun isInWatchLaterSync(profileId: String, id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchLater(item: WatchLaterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchLaterItems(items: List<WatchLaterEntity>)

    @Query("SELECT * FROM watch_later WHERE profileId = :profileId ORDER BY addedAt DESC")
    suspend fun getAllWatchLaterSync(profileId: String = "default"): List<WatchLaterEntity>

    @Query("SELECT * FROM watch_later ORDER BY addedAt DESC")
    suspend fun getAllWatchLaterAcrossProfilesSync(): List<WatchLaterEntity>

    @Query("DELETE FROM watch_later WHERE profileId = :profileId AND id = :id")
    suspend fun deleteWatchLater(profileId: String, id: String)

    @Query("DELETE FROM watch_later WHERE profileId = :profileId AND mediaType = :mediaType AND mediaId = :mediaId")
    suspend fun deleteWatchLater(profileId: String, mediaType: String, mediaId: Long)

    @Query("DELETE FROM watch_later WHERE profileId = :profileId")
    suspend fun clearWatchLater(profileId: String = "default")

    @Query("DELETE FROM watch_later")
    suspend fun clearAll()

    @Query("UPDATE watch_later SET profileId = :targetProfileId WHERE profileId = :sourceProfileId")
    suspend fun migrateWatchLaterProfile(sourceProfileId: String, targetProfileId: String)
}
