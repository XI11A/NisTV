package com.nis.nistv.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nis.nistv.core.database.entity.TrackedSeriesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedSeriesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(trackedSeries: TrackedSeriesEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(trackedSeries: TrackedSeriesEntity): Long

    @Query("SELECT * FROM tracked_series WHERE seriesKey = :seriesKey LIMIT 1")
    fun getTrackedSeries(seriesKey: String): Flow<TrackedSeriesEntity?>

    @Query("SELECT * FROM tracked_series WHERE seriesKey = :seriesKey LIMIT 1")
    suspend fun getTrackedSeriesSync(seriesKey: String): TrackedSeriesEntity?

    @Query("SELECT * FROM tracked_series ORDER BY name ASC")
    fun getAllTrackedSeries(): Flow<List<TrackedSeriesEntity>>

    @Query("SELECT * FROM tracked_series WHERE notifyEnabled = 1 ORDER BY lastCheckedAt ASC")
    suspend fun getNotifyEnabledSeries(): List<TrackedSeriesEntity>

    @Query("UPDATE tracked_series SET notifyEnabled = :enabled WHERE seriesKey = :seriesKey")
    suspend fun setNotifyEnabled(seriesKey: String, enabled: Boolean)

    @Query("""
        UPDATE tracked_series 
        SET lastKnownEpisodeKey = :lastKnown,
            nextAirDate = :nextAir,
            lastNotifiedKey = :lastNotified,
            lastCheckedAt = :checkedAt
        WHERE seriesKey = :seriesKey
    """)
    suspend fun updateRadarCheck(
        seriesKey: String,
        lastKnown: String?,
        nextAir: String?,
        lastNotified: String?,
        checkedAt: Long
    )

    @Query("DELETE FROM tracked_series WHERE seriesKey = :seriesKey")
    suspend fun deleteTrackedSeries(seriesKey: String)
}
