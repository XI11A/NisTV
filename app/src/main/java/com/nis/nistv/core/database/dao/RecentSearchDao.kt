package com.nis.nistv.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nis.nistv.core.database.entity.RecentSearchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentSearchDao {

    @Query("SELECT query FROM recent_searches WHERE profileId = :profileId ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentSearches(profileId: String, limit: Int = 10): Flow<List<String>>

    @Query("SELECT query FROM recent_searches WHERE profileId = :profileId ORDER BY createdAt DESC")
    suspend fun getAllSearchesSync(profileId: String = "default"): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: RecentSearchEntity)

    @Query("DELETE FROM recent_searches WHERE profileId = :profileId AND query = :query")
    suspend fun deleteSearch(profileId: String, query: String)

    @Query("DELETE FROM recent_searches WHERE profileId = :profileId")
    suspend fun clearSearches(profileId: String)
}
