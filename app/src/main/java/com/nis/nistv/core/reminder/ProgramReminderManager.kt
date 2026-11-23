package com.nis.nistv.core.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nis.nistv.core.database.dao.ProgramReminderDao
import com.nis.nistv.core.database.entity.ProgramReminderEntity
import com.nis.nistv.data.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ProgramReminderManager(
    private val context: Context,
    private val reminderDao: ProgramReminderDao,
    private val profileRepository: ProfileRepository? = null
) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun currentProfileId(): String =
        profileRepository?.activeProfileId?.value ?: "default"

    fun getUpcomingReminders(fromTimeMs: Long = System.currentTimeMillis()): Flow<List<ProgramReminderEntity>> {
        val targetProfileFlow = profileRepository?.activeProfileId ?: flowOf("default")
        return targetProfileFlow.flatMapLatest { profileId ->
            reminderDao.getUpcomingReminders(profileId, fromTimeMs)
        }.flowOn(Dispatchers.IO)
    }

    fun getRemindersForChannel(channelId: String): Flow<List<ProgramReminderEntity>> {
        val targetProfileFlow = profileRepository?.activeProfileId ?: flowOf("default")
        return targetProfileFlow.flatMapLatest { profileId ->
            reminderDao.getRemindersForChannel(profileId, channelId)
        }.flowOn(Dispatchers.IO)
    }

    suspend fun isReminderSet(channelId: String, startEpochMs: Long): Boolean {
        val profileId = currentProfileId()
        val existing = reminderDao.findReminder(profileId, channelId, startEpochMs)
        return existing != null && !existing.notified
    }

    suspend fun scheduleReminder(
        channelId: String,
        programTitle: String,
        startEpochMs: Long
    ): String {
        val profileId = currentProfileId()
        val reminderId = "rem_${profileId}_${channelId}_$startEpochMs"
        val reminder = ProgramReminderEntity(
            id = reminderId,
            profileId = profileId,
            channelId = channelId,
            programTitle = programTitle,
            startEpochMs = startEpochMs,
            notified = false
        )
        reminderDao.insertReminder(reminder)

        // Lead time: 5 minutes before program start
        val leadTimeMs = 5 * 60 * 1000L
        val triggerTimeMs = (startEpochMs - leadTimeMs).coerceAtLeast(System.currentTimeMillis())

        val intent = Intent(context, ProgramReminderReceiver::class.java).apply {
            putExtra(ProgramReminderReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ProgramReminderReceiver.EXTRA_CHANNEL_ID, channelId)
            putExtra(ProgramReminderReceiver.EXTRA_PROGRAM_TITLE, programTitle)
            putExtra(ProgramReminderReceiver.EXTRA_START_EPOCH_MS, startEpochMs)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMs,
                pendingIntent
            )
        }

        return reminderId
    }

    suspend fun cancelReminder(channelId: String, startEpochMs: Long) {
        val profileId = currentProfileId()
        val reminderId = "rem_${profileId}_${channelId}_$startEpochMs"
        val intent = Intent(context, ProgramReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        reminderDao.deleteReminder(profileId, channelId, startEpochMs)
    }

    suspend fun cleanOldReminders() {
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        reminderDao.cleanOldReminders(cutoff)
    }
}
