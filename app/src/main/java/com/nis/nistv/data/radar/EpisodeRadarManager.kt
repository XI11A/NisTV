package com.nis.nistv.data.radar

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.nis.nistv.core.database.dao.DownloadDao
import com.nis.nistv.core.database.dao.TrackedSeriesDao
import com.nis.nistv.core.database.dao.WatchLaterDao
import com.nis.nistv.core.database.entity.TrackedSeriesEntity
import com.nis.nistv.core.preferences.UserPreferences
import com.nis.nistv.data.download.offline.SeriesKeyHelper
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class EpisodeRadarManager(
    private val context: Context,
    private val trackedSeriesDao: TrackedSeriesDao,
    private val downloadDao: DownloadDao,
    private val watchLaterDao: WatchLaterDao,
    private val userPreferences: UserPreferences
) {

    companion object {
        const val WORK_NAME = "EpisodeRadarPeriodicCheck"
        const val NOTIFICATION_CHANNEL_ID = "new_episodes_channel"
    }

    fun isSeriesTracked(seriesKey: String): Flow<TrackedSeriesEntity?> =
        trackedSeriesDao.getTrackedSeries(seriesKey)

    suspend fun isSeriesTrackedSync(seriesKey: String): TrackedSeriesEntity? =
        trackedSeriesDao.getTrackedSeriesSync(seriesKey)

    suspend fun setSeriesNotification(seriesKey: String, tmdbId: Long, name: String, enabled: Boolean) {
        val existing = trackedSeriesDao.getTrackedSeriesSync(seriesKey)
        if (existing != null) {
            trackedSeriesDao.setNotifyEnabled(seriesKey, enabled)
        } else {
            trackedSeriesDao.insertOrUpdate(
                TrackedSeriesEntity(
                    seriesKey = seriesKey,
                    tmdbId = tmdbId,
                    name = name,
                    notifyEnabled = enabled
                )
            )
        }
    }

    /**
     * Scans completed downloads and watch later items to ensure active series are tracked in Room.
     * Series with downloads default to notifyEnabled = true.
     */
    suspend fun syncTrackedSeriesFromLibrary() {
        // 1. Downloaded series
        val downloads = downloadDao.getAllDownloadsSync()
        val tvDownloads = downloads.filter { it.mediaType.equals("tv", ignoreCase = true) }
        val tvGroups = tvDownloads.groupBy { dl ->
            SeriesKeyHelper.getSeriesKey(dl.seriesId, dl.effectiveSeriesName)
        }

        for ((seriesKey, list) in tvGroups) {
            if (seriesKey == SeriesKeyHelper.UNSORTED_KEY) continue
            val tmdbId = SeriesKeyHelper.extractTmdbId(seriesKey) ?: continue
            val first = list.firstOrNull() ?: continue
            val name = first.effectiveSeriesName?.takeIf { it.isNotBlank() } ?: first.title
            val existing = trackedSeriesDao.getTrackedSeriesSync(seriesKey)
            if (existing == null) {
                trackedSeriesDao.insertOrUpdate(
                    TrackedSeriesEntity(
                        seriesKey = seriesKey,
                        tmdbId = tmdbId,
                        name = name,
                        notifyEnabled = true
                    )
                )
            }
        }

        // 2. Watch Later series
        val watchLater = watchLaterDao.getAllWatchLaterSync()
        val tvWatchLater = watchLater.filter { it.mediaType.equals("tv", ignoreCase = true) }
        for (item in tvWatchLater) {
            val seriesKey = SeriesKeyHelper.buildSeriesKey(item.mediaId)
            val existing = trackedSeriesDao.getTrackedSeriesSync(seriesKey)
            if (existing == null) {
                trackedSeriesDao.insertIfNotExists(
                    TrackedSeriesEntity(
                        seriesKey = seriesKey,
                        tmdbId = item.mediaId,
                        name = item.title,
                        notifyEnabled = false
                    )
                )
            }
        }
    }

    fun schedulePeriodicCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<EpisodeRadarWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
