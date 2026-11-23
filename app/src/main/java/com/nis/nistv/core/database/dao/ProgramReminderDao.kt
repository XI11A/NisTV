package com.nis.nistv.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nis.nistv.core.database.entity.ProgramReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramReminderDao {

    @Query("SELECT * FROM program_reminders WHERE profileId = :profileId AND startEpochMs >= :fromTimeMs AND notified = 0 ORDER BY startEpochMs ASC")
    fun getUpcomingReminders(profileId: String, fromTimeMs: Long): Flow<List<ProgramReminderEntity>>

    @Query("SELECT * FROM program_reminders WHERE startEpochMs >= :fromTimeMs AND notified = 0 ORDER BY startEpochMs ASC")
    fun getAllUpcomingReminders(fromTimeMs: Long): Flow<List<ProgramReminderEntity>>

    @Query("SELECT * FROM program_reminders WHERE profileId = :profileId AND channelId = :channelId ORDER BY startEpochMs ASC")
    fun getRemindersForChannel(profileId: String, channelId: String): Flow<List<ProgramReminderEntity>>

    @Query("SELECT * FROM program_reminders WHERE id = :id")
    suspend fun getReminderById(id: String): ProgramReminderEntity?

    @Query("SELECT * FROM program_reminders WHERE profileId = :profileId AND channelId = :channelId AND startEpochMs = :startEpochMs LIMIT 1")
    suspend fun findReminder(profileId: String, channelId: String, startEpochMs: Long): ProgramReminderEntity?

    @Query("SELECT * FROM program_reminders WHERE channelId = :channelId AND startEpochMs = :startEpochMs LIMIT 1")
    suspend fun findAnyReminder(channelId: String, startEpochMs: Long): ProgramReminderEntity?

    @Query("SELECT * FROM program_reminders WHERE profileId = :profileId")
    suspend fun getAllRemindersForProfileSync(profileId: String): List<ProgramReminderEntity>

    @Query("SELECT * FROM program_reminders")
    suspend fun getAllRemindersSync(): List<ProgramReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ProgramReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ProgramReminderEntity>)

    @Query("DELETE FROM program_reminders WHERE id = :id")
    suspend fun deleteReminderById(id: String)

    @Query("DELETE FROM program_reminders WHERE profileId = :profileId AND channelId = :channelId AND startEpochMs = :startEpochMs")
    suspend fun deleteReminder(profileId: String, channelId: String, startEpochMs: Long)

    @Query("UPDATE program_reminders SET notified = 1 WHERE id = :id")
    suspend fun markNotified(id: String)

    @Query("DELETE FROM program_reminders WHERE startEpochMs < :cutoffMs")
    suspend fun cleanOldReminders(cutoffMs: Long)

    @Query("DELETE FROM program_reminders WHERE profileId = :profileId")
    suspend fun clearRemindersForProfile(profileId: String)
}
