package com.nis.nistv.data.autopilot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.StatFs
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.nis.nistv.R
import com.nis.nistv.core.database.dao.AutopilotRuleDao
import com.nis.nistv.core.database.dao.DownloadDao
import com.nis.nistv.core.database.dao.OfflineMediaDao
import com.nis.nistv.core.database.dao.PlaybackProgressDao
import com.nis.nistv.core.database.entity.AutopilotRuleEntity
import com.nis.nistv.core.network.NetworkMonitor
import com.nis.nistv.core.preferences.UserPreferences
import com.nis.nistv.data.download.engine.DownloadManager
import com.nis.nistv.data.download.offline.MediaNameParser
import com.nis.nistv.data.download.offline.SeriesKeyHelper
import com.nis.nistv.data.download.provider.AfdsDownloadProvider
import com.nis.nistv.data.download.provider.DownloadMediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class AutopilotManager(
    private val context: Context,
    private val autopilotRuleDao: AutopilotRuleDao,
    private val downloadDao: DownloadDao,
    private val offlineMediaDao: OfflineMediaDao,
    private val downloadManager: DownloadManager,
    private val afdsDownloadProvider: AfdsDownloadProvider,
    private val userPreferences: UserPreferences,
    private val networkMonitor: NetworkMonitor,
    private val playbackProgressDao: PlaybackProgressDao? = null
) {

    companion object {
        const val WORK_NAME = "AutopilotPeriodicCheck"
        const val NOTIFICATION_CHANNEL_ID = "autopilot_channel"
        const val NOTIF_CANT_FIND_ID = 5001
        const val NOTIF_REMOVAL_ID = 5002
    }

    fun getRule(seriesKey: String): Flow<AutopilotRuleEntity?> =
        autopilotRuleDao.getRule(seriesKey)

    suspend fun saveRule(rule: AutopilotRuleEntity) {
        autopilotRuleDao.insertOrUpdate(rule)
    }

    suspend fun runAutopilotForSeries(seriesKey: String) = withContext(Dispatchers.IO) {
        val rule = autopilotRuleDao.getRuleSync(seriesKey) ?: return@withContext
        if (!rule.enabled) return@withContext

        val isOnline = networkMonitor.isOnline.firstOrNull() ?: false
        val isWifi = isOnline // On typical local connections network monitor detects online
        val isCharging = checkIsDeviceCharging()
        val afdsToken = userPreferences.afdsAuthToken.firstOrNull().orEmpty()
        val isAfdsLoggedIn = afdsToken.isNotBlank()

        val stat = StatFs(context.filesDir.absolutePath)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong

        val todayBytes = userPreferences.todayDownloadedBytes.firstOrNull() ?: 0L
        val limitGb = userPreferences.dailyDownloadLimitGb.firstOrNull() ?: 0
        val remainingQuotaBytes = if (limitGb > 0) {
            (limitGb.toLong() * 1024L * 1024L * 1024L) - todayBytes
        } else {
            Long.MAX_VALUE
        }

        // Assemble library items for this series
        val seriesDownloads = downloadDao.getAllDownloadsSync().filter {
            val sKey = SeriesKeyHelper.getSeriesKey(it.seriesId, it.effectiveSeriesName)
            sKey == seriesKey
        }

        val offlineEpisodes = offlineMediaDao.getEpisodesSync(seriesKey)
        val downloadMap = seriesDownloads.associateBy { Pair(it.seasonNumber ?: 1, it.episodeNumber ?: 1) }

        val libraryItems = mutableListOf<EpisodeLibraryItem>()
        if (offlineEpisodes.isNotEmpty()) {
            for (ep in offlineEpisodes) {
                val dl = downloadMap[Pair(ep.seasonNumber, ep.episodeNumber)]
                // Autopilot rule: an episode counts as watched when ANY profile completed it
                val isCompletedAny = dl?.let {
                    playbackProgressDao?.isCompletedByAnyProfile(it.id) == true || it.watchedCompleted
                } ?: false

                libraryItems.add(
                    EpisodeLibraryItem(
                        downloadId = dl?.id,
                        seasonNumber = ep.seasonNumber,
                        episodeNumber = ep.episodeNumber,
                        isDownloaded = dl?.status?.equals("COMPLETED", ignoreCase = true) == true,
                        isDownloading = dl?.status?.equals("DOWNLOADING", ignoreCase = true) == true ||
                                dl?.status?.equals("QUEUED", ignoreCase = true) == true,
                        isWatchedCompleted = isCompletedAny,
                        lastWatchedAt = dl?.lastWatchedAt ?: 0L,
                        fileSizeBytes = dl?.fileSizeBytes ?: 0L
                    )
                )
            }
        } else {
            for (dl in seriesDownloads) {
                // Autopilot rule: an episode counts as watched when ANY profile completed it
                val isCompletedAny = playbackProgressDao?.isCompletedByAnyProfile(dl.id) == true || dl.watchedCompleted

                libraryItems.add(
                    EpisodeLibraryItem(
                        downloadId = dl.id,
                        seasonNumber = dl.seasonNumber ?: 1,
                        episodeNumber = dl.episodeNumber ?: 1,
                        isDownloaded = dl.status.equals("COMPLETED", ignoreCase = true),
                        isDownloading = dl.status.equals("DOWNLOADING", ignoreCase = true) ||
                                dl.status.equals("QUEUED", ignoreCase = true),
                        isWatchedCompleted = isCompletedAny,
                        lastWatchedAt = dl.lastWatchedAt,
                        fileSizeBytes = dl.fileSizeBytes
                    )
                )
            }
        }

        val planInput = AutopilotPlanInput(
            seriesKey = seriesKey,
            rule = rule,
            episodes = libraryItems,
            availableStorageBytes = availableBytes,
            remainingDailyQuotaBytes = remainingQuotaBytes,
            isAfdsLoggedIn = isAfdsLoggedIn,
            isOnline = isOnline,
            isWifi = isWifi,
            isCharging = isCharging,
            currentTimeMs = System.currentTimeMillis()
        )

        val planResult = AutopilotPlanner.plan(planInput)

        // 1. Process fetching upcoming episodes
        var fetchedCount = 0
        val seriesName = seriesDownloads.firstOrNull()?.effectiveSeriesName
            ?: offlineMediaDao.getMediaSync(seriesKey)?.title ?: "Series"

        for ((season, episode) in planResult.episodesToFetch) {
            val candidates = try {
                afdsDownloadProvider.search(
                    title = seriesName,
                    year = null,
                    mediaType = DownloadMediaType.TV,
                    seasonNumber = season,
                    episodeNumber = episode
                )
            } catch (_: Exception) {
                emptyList()
            }

            // Verify with MediaNameParser: must match season and episode exactly
            val matchingCandidate = candidates.firstOrNull { candidate ->
                val parsed = MediaNameParser.parse(candidate.title)
                parsed.seasonNumber == season && parsed.episodeNumber == episode
            }

            if (matchingCandidate != null) {
                try {
                    val resolved = afdsDownloadProvider.resolve(matchingCandidate)
                    downloadManager.enqueueDownload(
                        mediaId = "${seriesKey}_${season}_${episode}",
                        title = "$seriesName S%02dE%02d".format(season, episode),
                        downloadUrl = resolved.url,
                        fileName = resolved.fileName.ifBlank { "$seriesName.S%02dE%02d.mp4".format(season, episode) },
                        posterUrl = seriesDownloads.firstOrNull()?.posterUrl,
                        mediaType = "tv",
                        sizeBytes = resolved.sizeBytes,
                        seasonNumber = season,
                        episodeNumber = episode,
                        seriesId = seriesKey,
                        seriesName = seriesName
                    )
                    fetchedCount++
                } catch (_: Exception) {
                    // Skip and continue
                }
            } else {
                notifyMissingEpisode(seriesName, season, episode)
            }
        }

        // 2. Process pending removals notification (24-hour warning)
        if (planResult.episodesPendingRemoval.isNotEmpty()) {
            notifyPendingRemoval(seriesName, planResult.episodesPendingRemoval.size)
        }

        // 3. Process removals of watched episodes
        var removedCount = 0
        for (downloadId in planResult.episodesToRemove) {
            try {
                downloadManager.deleteDownload(downloadId)
                removedCount++
            } catch (_: Exception) {
                // Ignore failure
            }
        }

        val runResult = "Fetched $fetchedCount episodes, removed $removedCount watched episodes"
        autopilotRuleDao.updateRunResult(seriesKey, System.currentTimeMillis(), runResult)
    }

    private fun checkIsDeviceCharging(): Boolean {
        return try {
            val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        } catch (_: Exception) {
            false
        }
    }

    private fun notifyMissingEpisode(seriesName: String, season: Int, episode: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        ensureChannel(nm)

        val text = "Couldn't find S%02dE%02d for $seriesName on AFDS".format(season, episode)
        val notif = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Autopilot Search Alert")
            .setContentText(text)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIF_CANT_FIND_ID + season * 100 + episode, notif)
    }

    private fun notifyPendingRemoval(seriesName: String, count: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        ensureChannel(nm)

        val text = "Will remove $count watched episodes of $seriesName in 24 h"
        val notif = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Smart Downloads Cleanup")
            .setContentText(text)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIF_REMOVAL_ID, notif)
    }

    private fun ensureChannel(nm: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Smart Downloads Autopilot",
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }
    }

    fun schedulePeriodicCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<AutopilotWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
