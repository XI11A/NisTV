package com.nis.nistv.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nis.nistv.core.database.entity.SyncMetadataEntity

@Dao
interface SyncMetadataDao {

    @Query("SELECT * FROM sync_metadata WHERE resourceKey = :key LIMIT 1")
    suspend fun getMetadata(key: String): SyncMetadataEntity?

    @Query("SELECT * FROM sync_metadata WHERE resourceKey = :key LIMIT 1")
    fun getMetadataFlow(key: String): kotlinx.coroutines.flow.Flow<SyncMetadataEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(metadata: SyncMetadataEntity)
}
