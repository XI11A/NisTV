package com.nis.nistv

import androidx.sqlite.db.SupportSQLiteDatabase
import com.nis.nistv.core.database.NisTvDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

class DatabaseMigrationV6Test {

    @Test
    fun migrationVersions_areConfiguredSequentially() {
        assertEquals(3, NisTvDatabase.MIGRATION_3_4.startVersion)
        assertEquals(4, NisTvDatabase.MIGRATION_3_4.endVersion)

        assertEquals(4, NisTvDatabase.MIGRATION_4_5.startVersion)
        assertEquals(5, NisTvDatabase.MIGRATION_4_5.endVersion)

        assertEquals(5, NisTvDatabase.MIGRATION_5_6.startVersion)
        assertEquals(6, NisTvDatabase.MIGRATION_5_6.endVersion)
    }

    @Test
    fun migration_5_to_6_executesExpectedSchemaStatements() {
        val executedSqlStatements = mutableListOf<String>()

        val handler = InvocationHandler { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedSqlStatements.add(args[0] as String)
            }
            null
        }

        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
            handler
        ) as SupportSQLiteDatabase

        // Execute migration
        NisTvDatabase.MIGRATION_5_6.migrate(dbProxy)

        assertTrue("Executed SQL list should not be empty", executedSqlStatements.isNotEmpty())

        // 1. Alterations on downloads table
        assertTrue(
            "Must alter downloads table to add seriesId",
            executedSqlStatements.any { it.contains("ALTER TABLE downloads ADD COLUMN seriesId TEXT") }
        )
        assertTrue(
            "Must alter downloads table to add seriesName",
            executedSqlStatements.any { it.contains("ALTER TABLE downloads ADD COLUMN seriesName TEXT") }
        )
        assertTrue(
            "Must alter downloads table to add lastPlaybackPositionMs",
            executedSqlStatements.any { it.contains("ALTER TABLE downloads ADD COLUMN lastPlaybackPositionMs INTEGER") }
        )

        // 2. Best-effort backfill
        assertTrue(
            "Must execute backfill for seriesId",
            executedSqlStatements.any { it.contains("UPDATE downloads") && it.contains("seriesId = SUBSTR(mediaId") }
        )
        assertTrue(
            "Must execute backfill for seriesName",
            executedSqlStatements.any { it.contains("UPDATE downloads") && it.contains("seriesName = TRIM(SUBSTR(title") }
        )

        // 3. New table offline_media
        val createOfflineMedia = executedSqlStatements.find { it.contains("CREATE TABLE IF NOT EXISTS offline_media") }
        assertTrue("Must create offline_media table", createOfflineMedia != null)
        assertTrue(createOfflineMedia!!.contains("id TEXT NOT NULL PRIMARY KEY"))
        assertTrue(createOfflineMedia.contains("tmdbId INTEGER NOT NULL"))
        assertTrue(createOfflineMedia.contains("mediaType TEXT NOT NULL"))
        assertTrue(createOfflineMedia.contains("castJson TEXT NOT NULL"))
        assertTrue(createOfflineMedia.contains("crewJson TEXT NOT NULL"))
        assertTrue(createOfflineMedia.contains("localPosterPath TEXT"))
        assertTrue(createOfflineMedia.contains("localBackdropPath TEXT"))

        // 4. New table offline_seasons
        val createOfflineSeasons = executedSqlStatements.find { it.contains("CREATE TABLE IF NOT EXISTS offline_seasons") }
        assertTrue("Must create offline_seasons table", createOfflineSeasons != null)
        assertTrue(createOfflineSeasons!!.contains("PRIMARY KEY(seriesKey, seasonNumber)"))

        // 5. New table offline_episodes
        val createOfflineEpisodes = executedSqlStatements.find { it.contains("CREATE TABLE IF NOT EXISTS offline_episodes") }
        assertTrue("Must create offline_episodes table", createOfflineEpisodes != null)
        assertTrue(createOfflineEpisodes!!.contains("PRIMARY KEY(seriesKey, seasonNumber, episodeNumber)"))

        // 6. Indices
        assertTrue(
            "Must index offline_media by tmdbId and mediaType",
            executedSqlStatements.any { it.contains("index_offline_media_tmdbId_mediaType") }
        )
        assertTrue(
            "Must index offline_seasons by seriesKey",
            executedSqlStatements.any { it.contains("index_offline_seasons_seriesKey") }
        )
        assertTrue(
            "Must index offline_episodes by seriesKey and seasonNumber",
            executedSqlStatements.any { it.contains("index_offline_episodes_seriesKey_seasonNumber") }
        )
    }
}
