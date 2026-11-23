package com.nis.nistv.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.database.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("""
        SELECT c.* FROM channels c
        INNER JOIN favorites f ON c.id = f.channelId
        WHERE f.profileId = :profileId
        ORDER BY f.createdAt DESC
    """)
    fun getFavoriteChannels(profileId: String): Flow<List<ChannelEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE profileId = :profileId AND channelId = :channelId)")
    fun isFavorite(profileId: String, channelId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE profileId = :profileId AND channelId = :channelId)")
    suspend fun isFavoriteSync(profileId: String, channelId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorites(favorites: List<FavoriteEntity>)

    @Query("SELECT * FROM favorites WHERE profileId = :profileId")
    suspend fun getAllFavoritesSync(profileId: String = "default"): List<FavoriteEntity>

    @Query("SELECT * FROM favorites")
    suspend fun getAllFavoritesAcrossProfilesSync(): List<FavoriteEntity>

    @Query("DELETE FROM favorites WHERE profileId = :profileId AND channelId = :channelId")
    suspend fun removeFavorite(profileId: String, channelId: String)

    @Query("DELETE FROM favorites WHERE profileId = :profileId")
    suspend fun clearAllFavorites(profileId: String = "default")
}
