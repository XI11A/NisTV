package com.nis.nistv.core.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nis.nistv.MainActivity
import com.nis.nistv.NisTvApplication
import com.nis.nistv.R
import kotlinx.coroutines.launch

class ProgramReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "program_reminders"
        const val EXTRA_REMINDER_ID = "reminderId"
        const val EXTRA_CHANNEL_ID = "channelId"
        const val EXTRA_PROGRAM_TITLE = "programTitle"
        const val EXTRA_START_EPOCH_MS = "startEpochMs"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID) ?: return
        val programTitle = intent.getStringExtra(EXTRA_PROGRAM_TITLE) ?: "TV Program"

        val app = context.applicationContext as? NisTvApplication ?: return

        // 1. Check Do Not Disturb (DND)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE) {
            // Total silence DND is active; suppress alert
            return
        }

        // 2. Check if user is currently watching this channel
        val playerUiState = app.container.playerController.uiState.value
        val isCurrentlyWatching = playerUiState.channel?.id == channelId && playerUiState.isPlaying
        if (isCurrentlyWatching) {
            // Already watching; no need to annoy user
            app.applicationScope.launch {
                app.container.database.programReminderDao().markNotified(reminderId)
            }
            return
        }

        // 3. Ensure Notification Channel exists
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Program Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for scheduled live TV programs"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 4. Intent to open player directly
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("channelId", channelId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            channelId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Starting Soon: $programTitle")
            .setContentText("Starts in 5 minutes. Tap to watch live.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.mipmap.ic_launcher,
                "Watch Now",
                pendingIntent
            )
            .build()

        notificationManager.notify(reminderId.hashCode(), notification)

        // 5. Mark as notified in database
        app.applicationScope.launch {
            app.container.database.programReminderDao().markNotified(reminderId)
        }
    }
}
