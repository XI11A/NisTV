package com.nis.nistv.data.radar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nis.nistv.MainActivity
import com.nis.nistv.NisTvApplication
import com.nis.nistv.R
import kotlinx.coroutines.flow.firstOrNull

class EpisodeRadarWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "new_episodes_channel"
        const val NOTIFICATION_ID = 4001
    }

    override suspend fun doWork(): Result {
        val app = context.applicationContext as? NisTvApplication ?: return Result.failure()
        val container = app.container

        val isRadarEnabled = container.userPreferences.isEpisodeRadarEnabled.firstOrNull() ?: true
        if (!isRadarEnabled) {
            return Result.success()
        }

        val trackedDao = container.database.trackedSeriesDao()
        val notifySeries = trackedDao.getNotifyEnabledSeries().take(50)
        if (notifySeries.isEmpty()) {
            return Result.success()
        }

        val newAlerts = mutableListOf<NewEpisodeAlert>()

        for (series in notifySeries) {
            try {
                val details = container.tmdbRepository.getTvDetails(series.tmdbId) ?: continue
                val lastEp = details.lastEpisodeToAir
                val nextEp = details.nextEpisodeToAir

                var newEpisodeKey: String? = null
                if (lastEp != null) {
                    val key = EpisodeKeyHelper.formatEpisodeKey(lastEp.seasonNumber, lastEp.episodeNumber)
                    if (EpisodeKeyHelper.isNewerEpisode(key, series.lastNotifiedKey)) {
                        newEpisodeKey = key
                        newAlerts.add(
                            NewEpisodeAlert(
                                seriesKey = series.seriesKey,
                                tmdbId = series.tmdbId,
                                seriesName = series.name.ifBlank { details.name },
                                seasonNumber = lastEp.seasonNumber,
                                episodeNumber = lastEp.episodeNumber,
                                episodeKey = key
                            )
                        )
                    }
                }

                trackedDao.updateRadarCheck(
                    seriesKey = series.seriesKey,
                    lastKnown = lastEp?.let { EpisodeKeyHelper.formatEpisodeKey(it.seasonNumber, it.episodeNumber) },
                    nextAir = nextEp?.airDate,
                    lastNotified = newEpisodeKey ?: series.lastNotifiedKey,
                    checkedAt = System.currentTimeMillis()
                )
            } catch (_: Exception) {
                // Continue with next series on failure
            }
        }

        if (newAlerts.isNotEmpty()) {
            sendGroupedNotification(newAlerts)
        }

        return Result.success()
    }

    private fun sendGroupedNotification(alerts: List<NewEpisodeAlert>) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "New Episode Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for newly released TV series episodes"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title: String
        val contentText: String
        val targetTmdbId: Long

        if (alerts.size == 1) {
            val alert = alerts.first()
            title = "New Episode Available"
            contentText = "Season ${alert.seasonNumber} Episode ${alert.episodeNumber} of ${alert.seriesName} is out"
            targetTmdbId = alert.tmdbId
        } else {
            val first = alerts.first()
            title = "${alerts.size} New Episodes Available"
            contentText = "Including Season ${first.seasonNumber} Episode ${first.episodeNumber} of ${first.seriesName}"
            targetTmdbId = first.tmdbId
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("shortcut_destination", "details/tv/$targetTmdbId")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    data class NewEpisodeAlert(
        val seriesKey: String,
        val tmdbId: Long,
        val seriesName: String,
        val seasonNumber: Int,
        val episodeNumber: Int,
        val episodeKey: String
    )
}
