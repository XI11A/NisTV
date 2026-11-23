package com.nis.nistv.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nis.nistv.core.database.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles ORDER BY sortOrder ASC, createdAt ASC")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getAllProfilesSync(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): ProfileEntity?

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun getProfileCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Query("UPDATE profiles SET historyPaused = :paused, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setHistoryPaused(id: String, paused: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfile(id: String)
}
