package com.nis.nistv.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nis.nistv.core.database.entity.DataUsageDailyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DataUsageDailyDao {

    @Query("SELECT * FROM data_usage_daily WHERE date = :date AND networkType = :networkType LIMIT 1")
    suspend fun getUsage(date: String, networkType: String): DataUsageDailyEntity?

    @Query("SELECT * FROM data_usage_daily WHERE date = :date")
    suspend fun getUsageForDateSync(date: String): List<DataUsageDailyEntity>

    @Query("SELECT * FROM data_usage_daily WHERE date = :date")
    fun getUsageForDate(date: String): Flow<List<DataUsageDailyEntity>>

    @Query("SELECT * FROM data_usage_daily WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getUsageBetweenSync(startDate: String, endDate: String): List<DataUsageDailyEntity>

    @Query("SELECT * FROM data_usage_daily WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getUsageBetween(startDate: String, endDate: String): Flow<List<DataUsageDailyEntity>>

    @Query("SELECT * FROM data_usage_daily ORDER BY date DESC")
    fun getAllUsage(): Flow<List<DataUsageDailyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: DataUsageDailyEntity)

    @Query("""
        INSERT INTO data_usage_daily (date, networkType, streamBytes, downloadBytes)
        VALUES (:date, :networkType, :bytes, 0)
        ON CONFLICT(date, networkType) DO UPDATE SET
            streamBytes = streamBytes + :bytes
    """)
    suspend fun addStreamBytes(date: String, networkType: String, bytes: Long)

    @Query("""
        INSERT INTO data_usage_daily (date, networkType, streamBytes, downloadBytes)
        VALUES (:date, :networkType, 0, :bytes)
        ON CONFLICT(date, networkType) DO UPDATE SET
            downloadBytes = downloadBytes + :bytes
    """)
    suspend fun addDownloadBytes(date: String, networkType: String, bytes: Long)

    @Query("DELETE FROM data_usage_daily WHERE date < :beforeDate")
    suspend fun pruneOldUsage(beforeDate: String)
}
