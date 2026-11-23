package com.nis.nistv

import androidx.sqlite.db.SupportSQLiteDatabase
import com.nis.nistv.core.database.NisTvDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

class DatabaseMigrationV9Test {

    @Test
    fun migrationVersions_areConfiguredSequentially() {
        assertEquals(8, NisTvDatabase.MIGRATION_8_9.startVersion)
        assertEquals(9, NisTvDatabase.MIGRATION_8_9.endVersion)
    }

    @Test
    fun migration_8_to_9_executesExpectedSchemaStatements() {
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
        NisTvDatabase.MIGRATION_8_9.migrate(dbProxy)

        assertTrue("Executed SQL list should not be empty", executedSqlStatements.isNotEmpty())

        // 1. Tracked series table
        val createTrackedSeries = executedSqlStatements.find { it.contains("CREATE TABLE IF NOT EXISTS tracked_series") }
        assertTrue("Must create tracked_series table", createTrackedSeries != null)
        assertTrue(createTrackedSeries!!.contains("seriesKey TEXT NOT NULL PRIMARY KEY"))
        assertTrue(createTrackedSeries.contains("tmdbId INTEGER NOT NULL"))
        assertTrue(createTrackedSeries.contains("name TEXT NOT NULL"))
        assertTrue(createTrackedSeries.contains("notifyEnabled INTEGER NOT NULL DEFAULT 1"))
        assertTrue(createTrackedSeries.contains("lastKnownEpisodeKey TEXT"))
        assertTrue(createTrackedSeries.contains("nextAirDate TEXT"))
        assertTrue(createTrackedSeries.contains("lastNotifiedKey TEXT"))
        assertTrue(createTrackedSeries.contains("lastCheckedAt INTEGER NOT NULL DEFAULT 0"))
        assertTrue(
            "Must create index on notifyEnabled",
            executedSqlStatements.any { it.contains("index_tracked_series_notifyEnabled") }
        )

        // 2. Autopilot rules table
        val createAutopilot = executedSqlStatements.find { it.contains("CREATE TABLE IF NOT EXISTS autopilot_rules") }
        assertTrue("Must create autopilot_rules table", createAutopilot != null)
        assertTrue(createAutopilot!!.contains("seriesKey TEXT NOT NULL PRIMARY KEY"))
        assertTrue(createAutopilot.contains("enabled INTEGER NOT NULL DEFAULT 0"))
        assertTrue(createAutopilot.contains("keepAhead INTEGER NOT NULL DEFAULT 1"))
        assertTrue(createAutopilot.contains("wifiOnly INTEGER NOT NULL DEFAULT 1"))
        assertTrue(createAutopilot.contains("chargingOnly INTEGER NOT NULL DEFAULT 0"))
        assertTrue(createAutopilot.contains("minFreeBytes INTEGER NOT NULL DEFAULT 2147483648"))
        assertTrue(createAutopilot.contains("autoDeleteWatched INTEGER NOT NULL DEFAULT 0"))
        assertTrue(createAutopilot.contains("deleteAfterDays INTEGER NOT NULL DEFAULT 7"))
        assertTrue(createAutopilot.contains("preferredQuality TEXT NOT NULL DEFAULT '1080p'"))
        assertTrue(createAutopilot.contains("lastRunAt INTEGER NOT NULL DEFAULT 0"))
        assertTrue(createAutopilot.contains("lastResult TEXT"))

        // 3. Alterations on subtitle_tracks
        assertTrue(
            "Must alter subtitle_tracks to add delayMs",
            executedSqlStatements.any { it.contains("ALTER TABLE subtitle_tracks ADD COLUMN delayMs INTEGER NOT NULL DEFAULT 0") }
        )
        assertTrue(
            "Must alter subtitle_tracks to add role",
            executedSqlStatements.any { it.contains("ALTER TABLE subtitle_tracks ADD COLUMN role TEXT NOT NULL DEFAULT 'PRIMARY'") }
        )
    }
}
