package com.nis.nistv

import android.app.Application
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.nis.nistv.core.database.NisTvDatabase
import com.nis.nistv.core.preferences.UserPreferences
import com.nis.nistv.data.repository.ChannelRepository
import com.nis.nistv.data.repository.DefaultChannelRepository
import com.nis.nistv.data.repository.DefaultProfileRepository
import com.nis.nistv.data.repository.DefaultWatchLaterRepository
import com.nis.nistv.data.repository.ProfileRepository
import com.nis.nistv.data.repository.WatchLaterRepository
import com.nis.nistv.playback.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.nis.nistv.BuildConfig
import com.nis.nistv.core.auth.GoogleAuthManager
import com.nis.nistv.core.network.NetworkMonitor
import com.nis.nistv.data.download.engine.DownloadManager
import com.nis.nistv.data.download.offline.DefaultOfflineMediaRepository
import com.nis.nistv.data.download.offline.DownloadsRepairManager
import com.nis.nistv.data.download.offline.OfflineMediaRepository
import com.nis.nistv.data.download.provider.AfdsAuthService
import com.nis.nistv.data.download.provider.AfdsDownloadProvider
import com.nis.nistv.data.download.provider.GenericUrlDownloadProvider
import com.nis.nistv.data.epg.EpgManager
import com.nis.nistv.data.recommendation.IntelligentRecommendationEngine
import com.nis.nistv.data.tmdb.remote.TmdbApi
import com.nis.nistv.data.tmdb.repository.DefaultTmdbRepository
import com.nis.nistv.data.tmdb.repository.MediaPreviewStore
import com.nis.nistv.data.tmdb.repository.TmdbRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

interface AppContainer {
    val applicationContext: Context
    val database: NisTvDatabase
    val channelRepository: ChannelRepository
    val profileRepository: ProfileRepository
    val playerController: PlayerController
    val userPreferences: UserPreferences
    val epgManager: EpgManager
    val googleAuthManager: GoogleAuthManager
    val okHttpClient: OkHttpClient
    val tmdbApi: TmdbApi
    val tmdbRepository: TmdbRepository
    val downloadManager: DownloadManager
    val afdsDownloadProvider: AfdsDownloadProvider
    val afdsAuthService: AfdsAuthService
    val genericDownloadProvider: GenericUrlDownloadProvider
    val recommendationEngine: IntelligentRecommendationEngine
    val networkMonitor: NetworkMonitor
    val watchLaterRepository: WatchLaterRepository
    val offlineMediaRepository: OfflineMediaRepository
    val downloadsRepairManager: DownloadsRepairManager
    val mediaPreviewStore: MediaPreviewStore
    val sleepTimerManager: com.nis.nistv.core.playback.SleepTimerManager
    val playlistSyncManager: com.nis.nistv.data.playlist.PlaylistSyncManager
    val programReminderManager: com.nis.nistv.core.reminder.ProgramReminderManager
    val parentalControlManager: com.nis.nistv.data.parental.ParentalControlManager
    val backupRestoreManager: com.nis.nistv.data.backup.BackupRestoreManager
    val secureTokenStore: com.nis.nistv.core.security.SecureTokenStore
    val episodeRadarManager: com.nis.nistv.data.radar.EpisodeRadarManager
    val autopilotManager: com.nis.nistv.data.autopilot.AutopilotManager
    val streamHealthRecorder: com.nis.nistv.data.health.StreamHealthRecorder
    val dataUsageManager: com.nis.nistv.data.saver.DataUsageManager
    val zapContext: com.nis.nistv.data.zapper.ZapContext
    val multiViewController: com.nis.nistv.playback.multiview.MultiViewController
    val bootReady: Deferred<Unit>
}

@OptIn(UnstableApi::class)
class DefaultAppContainer(override val applicationContext: Context) : AppContainer {
    override val bootReady = CompletableDeferred<Unit>()
    override val database: NisTvDatabase by lazy {
        NisTvDatabase.getInstance(applicationContext)
    }

    override val userPreferences: UserPreferences by lazy {
        UserPreferences(applicationContext)
    }

    override val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 NisTV/2.0")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    override val tmdbApi: TmdbApi by lazy {
        TmdbApi(
            okHttpClient = okHttpClient,
            apiKeyProvider = {
                val override = runBlocking { userPreferences.tmdbApiKeyOverride.firstOrNull() }
                if (!override.isNullOrBlank()) override else BuildConfig.TMDB_API_KEY
            },
            regionProvider = {
                val pref = runBlocking { userPreferences.preferredCountry.firstOrNull() }
                if (!pref.isNullOrBlank()) pref else java.util.Locale.getDefault().country.ifBlank { "IN" }
            }
        )
    }

    override val tmdbRepository: TmdbRepository by lazy {
        DefaultTmdbRepository(tmdbApi)
    }

    override val offlineMediaRepository: OfflineMediaRepository by lazy {
        DefaultOfflineMediaRepository(
            context = applicationContext,
            offlineMediaDao = database.offlineMediaDao(),
            downloadDao = database.downloadDao(),
            tmdbRepository = tmdbRepository,
            okHttpClient = okHttpClient
        )
    }

    override val downloadsRepairManager: DownloadsRepairManager by lazy {
        DownloadsRepairManager(
            context = applicationContext,
            downloadDao = database.downloadDao(),
            offlineMediaDao = database.offlineMediaDao(),
            offlineMediaRepository = offlineMediaRepository,
            tmdbRepository = tmdbRepository,
            userPreferences = userPreferences
        )
    }

    override val downloadManager: DownloadManager by lazy {
        DownloadManager(
            context = applicationContext,
            downloadDao = database.downloadDao(),
            userPreferences = userPreferences,
            okHttpClient = okHttpClient,
            afdsDownloadProvider = afdsDownloadProvider,
            offlineMediaRepository = offlineMediaRepository
        )
    }

    override val profileRepository: ProfileRepository by lazy {
        DefaultProfileRepository(
            database = database,
            userPreferences = userPreferences,
            playerControllerProvider = { playerController },
            sleepTimerManagerProvider = { sleepTimerManager },
            parentalControlManagerProvider = { parentalControlManager }
        )
    }

    override val watchLaterRepository: WatchLaterRepository by lazy {
        DefaultWatchLaterRepository(database.watchLaterDao(), profileRepository)
    }

    override val afdsDownloadProvider: AfdsDownloadProvider by lazy {
        AfdsDownloadProvider(okHttpClient, userPreferences)
    }

    override val afdsAuthService: AfdsAuthService by lazy {
        AfdsAuthService(okHttpClient, userPreferences)
    }

    override val genericDownloadProvider: GenericUrlDownloadProvider by lazy {
        GenericUrlDownloadProvider()
    }

    override val channelRepository: ChannelRepository by lazy {
        DefaultChannelRepository(applicationContext, database, userPreferences, profileRepository)
    }

    override val streamHealthRecorder: com.nis.nistv.data.health.StreamHealthRecorder by lazy {
        com.nis.nistv.data.health.DefaultStreamHealthRecorder(database.streamHealthDao())
    }

    override val dataUsageManager: com.nis.nistv.data.saver.DataUsageManager by lazy {
        com.nis.nistv.data.saver.DataUsageManager(database.dataUsageDailyDao(), networkMonitor, userPreferences)
    }

    override val zapContext: com.nis.nistv.data.zapper.ZapContext by lazy {
        com.nis.nistv.data.zapper.ZapContext()
    }

    override val multiViewController: com.nis.nistv.playback.multiview.MultiViewController by lazy {
        com.nis.nistv.playback.multiview.MultiViewController(applicationContext, streamHealthRecorder)
    }

    override val playerController: PlayerController by lazy {
        PlayerController(applicationContext, channelRepository, streamHealthRecorder, dataUsageManager)
    }

    override val epgManager: EpgManager by lazy {
        EpgManager(applicationContext)
    }

    override val googleAuthManager: GoogleAuthManager by lazy {
        GoogleAuthManager(applicationContext)
    }

    override val recommendationEngine: IntelligentRecommendationEngine by lazy {
        IntelligentRecommendationEngine(
            mediaInteractionDao = database.mediaInteractionDao(),
            channelRepository = channelRepository,
            tmdbRepository = tmdbRepository,
            userPreferences = userPreferences,
            profileRepository = profileRepository
        )
    }

    override val networkMonitor: NetworkMonitor by lazy {
        NetworkMonitor(applicationContext)
    }

    override val mediaPreviewStore: MediaPreviewStore by lazy {
        MediaPreviewStore()
    }

    override val sleepTimerManager: com.nis.nistv.core.playback.SleepTimerManager by lazy {
        com.nis.nistv.core.playback.SleepTimerManager(
            CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        ).apply {
            setExternalExpiryAction {
                try {
                    playerController.fadeVolumeAndPause(5000L)
                } catch (_: Exception) {}
            }
        }
    }

    override val playlistSyncManager: com.nis.nistv.data.playlist.PlaylistSyncManager by lazy {
        com.nis.nistv.data.playlist.PlaylistSyncManager(
            context = applicationContext,
            database = database,
            okHttpClient = okHttpClient
        )
    }

    override val programReminderManager: com.nis.nistv.core.reminder.ProgramReminderManager by lazy {
        com.nis.nistv.core.reminder.ProgramReminderManager(
            context = applicationContext,
            reminderDao = database.programReminderDao(),
            profileRepository = profileRepository
        )
    }

    override val secureTokenStore: com.nis.nistv.core.security.SecureTokenStore by lazy {
        userPreferences.secureTokenStore
    }

    override val parentalControlManager: com.nis.nistv.data.parental.ParentalControlManager by lazy {
        com.nis.nistv.data.parental.ParentalControlManager(userPreferences)
    }

    override val backupRestoreManager: com.nis.nistv.data.backup.BackupRestoreManager by lazy {
        com.nis.nistv.data.backup.BackupRestoreManager(
            database = database,
            userPreferences = userPreferences,
            appVersion = BuildConfig.VERSION_NAME,
            profileRepository = profileRepository
        )
    }

    override val episodeRadarManager: com.nis.nistv.data.radar.EpisodeRadarManager by lazy {
        com.nis.nistv.data.radar.EpisodeRadarManager(
            context = applicationContext,
            trackedSeriesDao = database.trackedSeriesDao(),
            downloadDao = database.downloadDao(),
            watchLaterDao = database.watchLaterDao(),
            userPreferences = userPreferences
        )
    }

    override val autopilotManager: com.nis.nistv.data.autopilot.AutopilotManager by lazy {
        com.nis.nistv.data.autopilot.AutopilotManager(
            context = applicationContext,
            autopilotRuleDao = database.autopilotRuleDao(),
            downloadDao = database.downloadDao(),
            offlineMediaDao = database.offlineMediaDao(),
            downloadManager = downloadManager,
            afdsDownloadProvider = afdsDownloadProvider,
            userPreferences = userPreferences,
            networkMonitor = networkMonitor,
            playbackProgressDao = database.playbackProgressDao()
        )
    }
}

class NisTvApplication : Application(), SingletonImageLoader.Factory {

    lateinit var container: AppContainer
        private set

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val stackTrace = android.util.Log.getStackTraceString(throwable)
                val logMessage = "=== NISTV CRASH ===\nThread: ${thread.name}\n$stackTrace\n"
                try {
                    val appFile = java.io.File(filesDir, "crash.log")
                    appFile.writeText(logMessage)
                } catch (_: Exception) {}
                try {
                    val dlFile = java.io.File("/sdcard/Download/nistv_crash.log")
                    dlFile.writeText(logMessage)
                } catch (_: Exception) {}
                android.util.Log.e("NisTvApp", logMessage)
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 NisTV/2.0")
                    .build()
                chain.proceed(request)
            }
            .build()

        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
            }
            .crossfade(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(applicationContext)

        // Seed data initialization in background
        applicationScope.launch {
            try {
                container.channelRepository.ensureSeeded()
            } catch (e: Exception) {
                android.util.Log.e("NisTvApp", "Error during seed init", e)
            } finally {
                (container.bootReady as? CompletableDeferred<Unit>)?.complete(Unit)
            }
        }

        // Run idempotent v6 downloads repair and folder scan on startup
        applicationScope.launch {
            try {
                container.downloadsRepairManager.runRepairIfNeeded()
            } catch (e: Exception) {
                android.util.Log.e("NisTvApp", "Error running downloads repair on startup", e)
            }
        }

        // Backfill offline media metadata for active/completed downloads
        applicationScope.launch {
            try {
                container.offlineMediaRepository.backfillPendingMetadata()
            } catch (e: Exception) {
                android.util.Log.e("NisTvApp", "Error backfilling offline metadata on startup", e)
            }
        }

        // Retry downloads repair & offline metadata backfill whenever device comes online
        applicationScope.launch {
            container.networkMonitor.isOnline.collect { isOnline ->
                if (isOnline) {
                    try {
                        container.downloadsRepairManager.resolvePendingFallbackSeries()
                        container.offlineMediaRepository.backfillPendingMetadata()
                    } catch (_: Exception) {}
                }
            }
        }

        // Schedule periodic catalogue sync with WorkManager in background scope safely
        applicationScope.launch {
            try {
                val syncConstraints = androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
                val periodicSync = androidx.work.PeriodicWorkRequestBuilder<com.nis.nistv.data.sync.CatalogueSyncWorker>(
                    24, java.util.concurrent.TimeUnit.HOURS
                )
                    .setConstraints(syncConstraints)
                    .setBackoffCriteria(
                        androidx.work.BackoffPolicy.EXPONENTIAL,
                        15, java.util.concurrent.TimeUnit.MINUTES
                    )
                    .build()
                androidx.work.WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                    "catalogue_periodic_sync",
                    androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                    periodicSync
                )

                // Schedule Episode Radar periodic checks & sync tracked series from library
                container.episodeRadarManager.schedulePeriodicCheck()
                container.episodeRadarManager.syncTrackedSeriesFromLibrary()

                // Schedule Smart Downloads Autopilot periodic checks
                container.autopilotManager.schedulePeriodicCheck()
            } catch (e: Throwable) {
                android.util.Log.w("NisTvApp", "WorkManager scheduling skipped or failed", e)
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
    }
}
