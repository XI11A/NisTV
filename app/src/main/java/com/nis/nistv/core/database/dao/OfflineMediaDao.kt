package com.nis.nistv.core.database.dao

import androidx.room.*
import com.nis.nistv.core.database.entity.OfflineEpisodeEntity
import com.nis.nistv.core.database.entity.OfflineMediaEntity
import com.nis.nistv.core.database.entity.OfflineSeasonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineMediaDao {

    @Query("SELECT * FROM offline_media WHERE id = :id LIMIT 1")
    fun getMedia(id: String): Flow<OfflineMediaEntity?>

    @Query("SELECT * FROM offline_media WHERE id = :id LIMIT 1")
    suspend fun getMediaSync(id: String): OfflineMediaEntity?

    @Query("SELECT * FROM offline_media ORDER BY savedAt DESC")
    fun getAllMedia(): Flow<List<OfflineMediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: OfflineMediaEntity)

    @Query("DELETE FROM offline_media WHERE id = :id")
    suspend fun deleteMedia(id: String)

    // --- Seasons ---

    @Query("SELECT * FROM offline_seasons WHERE seriesKey = :seriesKey ORDER BY seasonNumber ASC")
    fun getSeasons(seriesKey: String): Flow<List<OfflineSeasonEntity>>

    @Query("SELECT * FROM offline_seasons WHERE seriesKey = :seriesKey ORDER BY seasonNumber ASC")
    suspend fun getSeasonsSync(seriesKey: String): List<OfflineSeasonEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeasons(seasons: List<OfflineSeasonEntity>)

    @Query("DELETE FROM offline_seasons WHERE seriesKey = :seriesKey")
    suspend fun deleteSeasons(seriesKey: String)

    @Query("DELETE FROM offline_seasons WHERE seriesKey = :seriesKey AND seasonNumber = :seasonNumber")
    suspend fun deleteSeason(seriesKey: String, seasonNumber: Int)

    // --- Episodes ---

    @Query("SELECT * FROM offline_episodes WHERE seriesKey = :seriesKey ORDER BY seasonNumber ASC, episodeNumber ASC")
    fun getEpisodes(seriesKey: String): Flow<List<OfflineEpisodeEntity>>

    @Query("SELECT * FROM offline_episodes WHERE seriesKey = :seriesKey ORDER BY seasonNumber ASC, episodeNumber ASC")
    suspend fun getEpisodesSync(seriesKey: String): List<OfflineEpisodeEntity>

    @Query("SELECT * FROM offline_episodes WHERE seriesKey = :seriesKey AND seasonNumber = :seasonNumber ORDER BY episodeNumber ASC")
    fun getEpisodesForSeason(seriesKey: String, seasonNumber: Int): Flow<List<OfflineEpisodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<OfflineEpisodeEntity>)

    @Query("DELETE FROM offline_episodes WHERE seriesKey = :seriesKey")
    suspend fun deleteEpisodes(seriesKey: String)

    @Query("DELETE FROM offline_episodes WHERE seriesKey = :seriesKey AND seasonNumber = :seasonNumber")
    suspend fun deleteEpisodesForSeason(seriesKey: String, seasonNumber: Int)
}
