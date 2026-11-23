package com.nis.nistv.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nis.nistv.core.database.dao.ChannelDao
import com.nis.nistv.core.database.dao.DownloadDao
import com.nis.nistv.core.database.dao.FavoriteDao
import com.nis.nistv.core.database.dao.MediaInteractionDao
import com.nis.nistv.core.database.dao.OfflineMediaDao
import com.nis.nistv.core.database.dao.PlaylistDao
import com.nis.nistv.core.database.dao.ProfileDao
import com.nis.nistv.core.database.dao.ProgramReminderDao
import com.nis.nistv.core.database.dao.RecentSearchDao
import com.nis.nistv.core.database.dao.StreamSourceDao
import com.nis.nistv.core.database.dao.SubtitleTrackDao
import com.nis.nistv.core.database.dao.SyncMetadataDao
import com.nis.nistv.core.database.dao.TrackedSeriesDao
import com.nis.nistv.core.database.dao.AutopilotRuleDao
import com.nis.nistv.core.database.dao.StreamHealthDao
import com.nis.nistv.core.database.dao.DataUsageDailyDao
import com.nis.nistv.core.database.dao.PlaybackProgressDao
import com.nis.nistv.core.database.dao.WatchHistoryDao
import com.nis.nistv.core.database.dao.WatchLaterDao
import com.nis.nistv.core.database.entity.AutopilotRuleEntity
import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.database.entity.DataUsageDailyEntity
import com.nis.nistv.core.database.entity.DownloadEntity
import com.nis.nistv.core.database.entity.FavoriteEntity
import com.nis.nistv.core.database.entity.MediaInteractionEntity
import com.nis.nistv.core.database.entity.OfflineEpisodeEntity
import com.nis.nistv.core.database.entity.OfflineMediaEntity
import com.nis.nistv.core.database.entity.OfflineSeasonEntity
import com.nis.nistv.core.database.entity.PlaybackProgressEntity
import com.nis.nistv.core.database.entity.PlaylistEntity
import com.nis.nistv.core.database.entity.ProfileEntity
import com.nis.nistv.core.database.entity.ProgramReminderEntity
import com.nis.nistv.core.database.entity.RecentSearchEntity
import com.nis.nistv.core.database.entity.StreamHealthEntity
import com.nis.nistv.core.database.entity.StreamSourceEntity
import com.nis.nistv.core.database.entity.SubtitleTrackEntity
import com.nis.nistv.core.database.entity.SyncMetadataEntity
import com.nis.nistv.core.database.entity.TrackedSeriesEntity
import com.nis.nistv.core.database.entity.WatchHistoryEntity
import com.nis.nistv.core.database.entity.WatchLaterEntity

@Database(
    entities = [
        ChannelEntity::class,
        StreamSourceEntity::class,
        ProfileEntity::class,
        FavoriteEntity::class,
        WatchHistoryEntity::class,
        RecentSearchEntity::class,
        SyncMetadataEntity::class,
        DownloadEntity::class,
        MediaInteractionEntity::class,
        WatchLaterEntity::class,
        OfflineMediaEntity::class,
        OfflineSeasonEntity::class,
        OfflineEpisodeEntity::class,
        SubtitleTrackEntity::class,
        PlaylistEntity::class,
        ProgramReminderEntity::class,
        TrackedSeriesEntity::class,
        AutopilotRuleEntity::class,
        StreamHealthEntity::class,
        DataUsageDailyEntity::class,
        PlaybackProgressEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class NisTvDatabase : RoomDatabase() {

    abstract fun channelDao(): ChannelDao
    abstract fun streamSourceDao(): StreamSourceDao
    abstract fun profileDao(): ProfileDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun recentSearchDao(): RecentSearchDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun downloadDao(): DownloadDao
    abstract fun mediaInteractionDao(): MediaInteractionDao
    abstract fun watchLaterDao(): WatchLaterDao
    abstract fun offlineMediaDao(): OfflineMediaDao
    abstract fun subtitleTrackDao(): SubtitleTrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun programReminderDao(): ProgramReminderDao
    abstract fun trackedSeriesDao(): TrackedSeriesDao
    abstract fun autopilotRuleDao(): AutopilotRuleDao
    abstract fun streamHealthDao(): StreamHealthDao
    abstract fun dataUsageDailyDao(): DataUsageDailyDao
    abstract fun playbackProgressDao(): PlaybackProgressDao

    companion object {
        private const val DATABASE_NAME = "nistv_database.db"

        @Volatile
        private var INSTANCE: NisTvDatabase? = null

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE downloads ADD COLUMN overview TEXT")
                db.execSQL("ALTER TABLE downloads ADD COLUMN releaseDate TEXT")
                db.execSQL("ALTER TABLE downloads ADD COLUMN rating REAL")
                db.execSQL("ALTER TABLE downloads ADD COLUMN backdropUrl TEXT")
                db.execSQL("ALTER TABLE downloads ADD COLUMN genres TEXT")
                db.execSQL("ALTER TABLE downloads ADD COLUMN runtimeMinutes INTEGER")
                db.execSQL("ALTER TABLE downloads ADD COLUMN seasonNumber INTEGER")
                db.execSQL("ALTER TABLE downloads ADD COLUMN episodeNumber INTEGER")
                db.execSQL("ALTER TABLE downloads ADD COLUMN episodeName TEXT")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE downloads ADD COLUMN afdsFileId TEXT")
                db.execSQL("ALTER TABLE downloads ADD COLUMN afdsSourceTable TEXT")
                db.execSQL("ALTER TABLE downloads ADD COLUMN sourceProviderId TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS watch_later (
                        id TEXT NOT NULL PRIMARY KEY,
                        mediaId INTEGER NOT NULL,
                        mediaType TEXT NOT NULL,
                        title TEXT NOT NULL,
                        posterPath TEXT,
                        backdropPath TEXT,
                        overview TEXT,
                        releaseYear TEXT,
                        voteAverage REAL,
                        genres TEXT,
                        addedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Downloads table alterations
                db.execSQL("ALTER TABLE downloads ADD COLUMN seriesId TEXT")
                db.execSQL("ALTER TABLE downloads ADD COLUMN seriesName TEXT")
                db.execSQL("ALTER TABLE downloads ADD COLUMN lastPlaybackPositionMs INTEGER NOT NULL DEFAULT 0")

                // Best-effort backfill for existing TV series downloads
                db.execSQL(
                    """
                    UPDATE downloads 
                    SET seriesId = SUBSTR(mediaId, 1, INSTR(mediaId, '_') - 1)
                    WHERE mediaType = 'tv' AND mediaId LIKE '%_%' AND (seriesId IS NULL OR seriesId = '')
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    UPDATE downloads 
                    SET seriesName = TRIM(SUBSTR(title, 1, INSTR(title, ' S') - 1))
                    WHERE mediaType = 'tv' AND title LIKE '% S%E%' AND (seriesName IS NULL OR seriesName = '')
                    """.trimIndent()
                )

                // Create offline_media table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS offline_media (
                        id TEXT NOT NULL PRIMARY KEY,
                        tmdbId INTEGER NOT NULL,
                        mediaType TEXT NOT NULL,
                        title TEXT NOT NULL,
                        originalTitle TEXT,
                        tagline TEXT,
                        overview TEXT NOT NULL,
                        releaseDate TEXT,
                        status TEXT,
                        rating REAL NOT NULL,
                        voteCount INTEGER NOT NULL,
                        runtimeMinutes INTEGER,
                        genres TEXT,
                        originalLanguage TEXT,
                        certification TEXT,
                        numberOfSeasons INTEGER,
                        numberOfEpisodes INTEGER,
                        castJson TEXT NOT NULL,
                        crewJson TEXT NOT NULL,
                        localPosterPath TEXT,
                        localBackdropPath TEXT,
                        localLogoPath TEXT,
                        savedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_offline_media_tmdbId_mediaType ON offline_media (tmdbId, mediaType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_offline_media_savedAt ON offline_media (savedAt)")

                // Create offline_seasons table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS offline_seasons (
                        seriesKey TEXT NOT NULL,
                        seasonNumber INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        overview TEXT,
                        airDate TEXT,
                        episodeCount INTEGER NOT NULL,
                        localPosterPath TEXT,
                        PRIMARY KEY(seriesKey, seasonNumber)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_offline_seasons_seriesKey ON offline_seasons (seriesKey)")

                // Create offline_episodes table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS offline_episodes (
                        seriesKey TEXT NOT NULL,
                        seasonNumber INTEGER NOT NULL,
                        episodeNumber INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        overview TEXT NOT NULL,
                        airDate TEXT,
                        runtimeMinutes INTEGER,
                        rating REAL NOT NULL,
                        localStillPath TEXT,
                        PRIMARY KEY(seriesKey, seasonNumber, episodeNumber)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_offline_episodes_seriesKey ON offline_episodes (seriesKey)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_offline_episodes_seriesKey_seasonNumber ON offline_episodes (seriesKey, seasonNumber)")
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Downloads table alterations for Continue Watching & playback tracking
                db.execSQL("ALTER TABLE downloads ADD COLUMN durationMs INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE downloads ADD COLUMN lastWatchedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE downloads ADD COLUMN watchedCompleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE downloads ADD COLUMN hideFromContinueWatching INTEGER NOT NULL DEFAULT 0")

                // Subtitle tracks table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS subtitle_tracks (
                        id TEXT NOT NULL PRIMARY KEY,
                        downloadId TEXT NOT NULL,
                        path TEXT NOT NULL,
                        label TEXT NOT NULL,
                        languageTag TEXT,
                        mimeType TEXT NOT NULL,
                        isSelected INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(downloadId) REFERENCES downloads(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_subtitle_tracks_downloadId ON subtitle_tracks (downloadId)")
            }
        }

        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Playlists table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS playlists (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        sourceType TEXT NOT NULL,
                        url TEXT NOT NULL,
                        lastSyncedAt INTEGER NOT NULL DEFAULT 0,
                        channelCount INTEGER NOT NULL DEFAULT 0,
                        autoRefresh INTEGER NOT NULL DEFAULT 1,
                        enabled INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent()
                )

                // Add playlistId column to channels
                db.execSQL("ALTER TABLE channels ADD COLUMN playlistId TEXT DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_channels_playlistId ON channels (playlistId)")

                // Program reminders table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS program_reminders (
                        id TEXT NOT NULL PRIMARY KEY,
                        channelId TEXT NOT NULL,
                        programTitle TEXT NOT NULL,
                        startEpochMs INTEGER NOT NULL,
                        notified INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_program_reminders_channelId ON program_reminders (channelId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_program_reminders_startEpochMs ON program_reminders (startEpochMs)")
            }
        }

        val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Tracked series table for Episode Radar
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tracked_series (
                        seriesKey TEXT NOT NULL PRIMARY KEY,
                        tmdbId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        notifyEnabled INTEGER NOT NULL DEFAULT 1,
                        lastKnownEpisodeKey TEXT,
                        nextAirDate TEXT,
                        lastNotifiedKey TEXT,
                        lastCheckedAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tracked_series_notifyEnabled ON tracked_series (notifyEnabled)")

                // Autopilot rules table for Smart Downloads Autopilot
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS autopilot_rules (
                        seriesKey TEXT NOT NULL PRIMARY KEY,
                        enabled INTEGER NOT NULL DEFAULT 0,
                        keepAhead INTEGER NOT NULL DEFAULT 1,
                        wifiOnly INTEGER NOT NULL DEFAULT 1,
                        chargingOnly INTEGER NOT NULL DEFAULT 0,
                        minFreeBytes INTEGER NOT NULL DEFAULT 2147483648,
                        autoDeleteWatched INTEGER NOT NULL DEFAULT 0,
                        deleteAfterDays INTEGER NOT NULL DEFAULT 7,
                        preferredQuality TEXT NOT NULL DEFAULT '1080p',
                        lastRunAt INTEGER NOT NULL DEFAULT 0,
                        lastResult TEXT
                    )
                    """.trimIndent()
                )

                // Subtitle tracks alterations for dual subtitles & subtitle delay
                db.execSQL("ALTER TABLE subtitle_tracks ADD COLUMN delayMs INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE subtitle_tracks ADD COLUMN role TEXT NOT NULL DEFAULT 'PRIMARY'")
            }
        }

        val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Stream health table for stream intelligence & failover ranking
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS stream_health (
                        streamKey TEXT NOT NULL PRIMARY KEY,
                        channelId TEXT NOT NULL,
                        successCount INTEGER NOT NULL DEFAULT 0,
                        failureCount INTEGER NOT NULL DEFAULT 0,
                        avgStartupMs INTEGER NOT NULL DEFAULT 0,
                        lastSuccessAt INTEGER NOT NULL DEFAULT 0,
                        lastFailureAt INTEGER NOT NULL DEFAULT 0,
                        consecutiveFailures INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_stream_health_channelId ON stream_health (channelId)")

                // Data usage daily aggregation table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS data_usage_daily (
                        date TEXT NOT NULL,
                        networkType TEXT NOT NULL,
                        streamBytes INTEGER NOT NULL DEFAULT 0,
                        downloadBytes INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (date, networkType)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_data_usage_daily_date ON data_usage_daily (date)")
            }
        }

        val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // 1. Alter profiles table: add isKids, isLocked, preferredCountry, historyPaused, sortOrder
                db.execSQL("ALTER TABLE profiles ADD COLUMN isKids INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE profiles ADD COLUMN isLocked INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE profiles ADD COLUMN preferredCountry TEXT NOT NULL DEFAULT 'IN'")
                db.execSQL("ALTER TABLE profiles ADD COLUMN historyPaused INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE profiles ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")

                // Ensure 'default' profile row exists so foreign key copies below succeed
                val now = System.currentTimeMillis()
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO profiles (id, name, avatarId, preferredLanguage, preferredCountry, isKids, isLocked, historyPaused, sortOrder, createdAt, updatedAt)
                    VALUES ('default', 'Me', 'avatar_01', 'Hindi', 'IN', 0, 0, 0, 0, $now, $now)
                    """.trimIndent()
                )

                // 2. Rebuild watch_later with composite primary key (profileId, id) and FK cascade
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS watch_later_new (
                        profileId TEXT NOT NULL,
                        id TEXT NOT NULL,
                        mediaId INTEGER NOT NULL,
                        mediaType TEXT NOT NULL,
                        title TEXT NOT NULL,
                        posterPath TEXT,
                        backdropPath TEXT,
                        overview TEXT,
                        releaseYear TEXT,
                        voteAverage REAL,
                        genres TEXT,
                        addedAt INTEGER NOT NULL,
                        PRIMARY KEY(profileId, id),
                        FOREIGN KEY(profileId) REFERENCES profiles(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO watch_later_new (profileId, id, mediaId, mediaType, title, posterPath, backdropPath, overview, releaseYear, voteAverage, genres, addedAt)
                    SELECT 'default', id, mediaId, mediaType, title, posterPath, backdropPath, overview, releaseYear, voteAverage, genres, addedAt
                    FROM watch_later
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE watch_later")
                db.execSQL("ALTER TABLE watch_later_new RENAME TO watch_later")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_watch_later_profileId ON watch_later (profileId)")

                // 3. Alter media_interactions table: add profileId and index
                db.execSQL("ALTER TABLE media_interactions ADD COLUMN profileId TEXT NOT NULL DEFAULT 'default'")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_interactions_profileId_timestamp ON media_interactions (profileId, timestamp)")

                // 4. Alter program_reminders table: add profileId and index
                db.execSQL("ALTER TABLE program_reminders ADD COLUMN profileId TEXT NOT NULL DEFAULT 'default'")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_program_reminders_profileId ON program_reminders (profileId)")

                // 5. Create playback_progress table and backfill from downloads
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS playback_progress (
                        profileId TEXT NOT NULL,
                        downloadId TEXT NOT NULL,
                        positionMs INTEGER NOT NULL DEFAULT 0,
                        durationMs INTEGER NOT NULL DEFAULT 0,
                        lastWatchedAt INTEGER NOT NULL DEFAULT 0,
                        completed INTEGER NOT NULL DEFAULT 0,
                        hidden INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(profileId, downloadId),
                        FOREIGN KEY(profileId) REFERENCES profiles(id) ON DELETE CASCADE,
                        FOREIGN KEY(downloadId) REFERENCES downloads(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playback_progress_profileId ON playback_progress (profileId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playback_progress_downloadId ON playback_progress (downloadId)")

                db.execSQL(
                    """
                    INSERT OR REPLACE INTO playback_progress (profileId, downloadId, positionMs, durationMs, lastWatchedAt, completed, hidden)
                    SELECT 'default', id, lastPlaybackPositionMs, durationMs, lastWatchedAt,
                           CASE WHEN watchedCompleted = 1 THEN 1 ELSE 0 END,
                           CASE WHEN hideFromContinueWatching = 1 THEN 1 ELSE 0 END
                    FROM downloads
                    WHERE lastWatchedAt > 0 OR lastPlaybackPositionMs > 0 OR watchedCompleted = 1 OR hideFromContinueWatching = 1
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): NisTvDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NisTvDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
