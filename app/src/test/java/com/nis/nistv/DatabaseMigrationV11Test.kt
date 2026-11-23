package com.nis.nistv

import androidx.sqlite.db.SupportSQLiteDatabase
import com.nis.nistv.core.database.NisTvDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

class DatabaseMigrationV11Test {

    @Test
    fun migrationVersions_areConfiguredSequentially() {
        assertEquals(10, NisTvDatabase.MIGRATION_10_11.startVersion)
        assertEquals(11, NisTvDatabase.MIGRATION_10_11.endVersion)
    }

    @Test
    fun migration_10_to_11_executesExpectedSchemaStatements() {
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

        // Execute migration 10 -> 11
        NisTvDatabase.MIGRATION_10_11.migrate(dbProxy)

        assertTrue("Executed SQL list should not be empty", executedSqlStatements.isNotEmpty())

        // 1. Profiles table modifications
        assertTrue(
            "Must add isKids column to profiles",
            executedSqlStatements.any { it.contains("ALTER TABLE profiles ADD COLUMN isKids INTEGER NOT NULL DEFAULT 0") }
        )
        assertTrue(
            "Must add isLocked column to profiles",
            executedSqlStatements.any { it.contains("ALTER TABLE profiles ADD COLUMN isLocked INTEGER NOT NULL DEFAULT 0") }
        )
        assertTrue(
            "Must add preferredCountry column to profiles",
            executedSqlStatements.any { it.contains("ALTER TABLE profiles ADD COLUMN preferredCountry TEXT NOT NULL DEFAULT 'IN'") }
        )
        assertTrue(
            "Must add historyPaused column to profiles",
            executedSqlStatements.any { it.contains("ALTER TABLE profiles ADD COLUMN historyPaused INTEGER NOT NULL DEFAULT 0") }
        )
        assertTrue(
            "Must add sortOrder column to profiles",
            executedSqlStatements.any { it.contains("ALTER TABLE profiles ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0") }
        )
        assertTrue(
            "Must ensure default profile row exists",
            executedSqlStatements.any { it.contains("INSERT OR IGNORE INTO profiles") && it.contains("'default'") }
        )

        // 2. Watch later rebuild for composite primary key (profileId, id)
        assertTrue(
            "Must create watch_later_new table with composite PK",
            executedSqlStatements.any {
                it.contains("CREATE TABLE IF NOT EXISTS watch_later_new") &&
                it.contains("PRIMARY KEY(profileId, id)") &&
                it.contains("FOREIGN KEY(profileId) REFERENCES profiles(id) ON DELETE CASCADE")
            }
        )
        assertTrue(
            "Must copy existing watch later items with profileId default",
            executedSqlStatements.any {
                it.contains("INSERT INTO watch_later_new") && it.contains("'default'")
            }
        )
        assertTrue(
            "Must drop old watch_later table",
            executedSqlStatements.any { it.contains("DROP TABLE watch_later") }
        )
        assertTrue(
            "Must rename watch_later_new to watch_later",
            executedSqlStatements.any { it.contains("ALTER TABLE watch_later_new RENAME TO watch_later") }
        )
        assertTrue(
            "Must create index on watch_later profileId",
            executedSqlStatements.any { it.contains("index_watch_later_profileId") }
        )

        // 3. Media interactions profile isolation
        assertTrue(
            "Must add profileId column to media_interactions",
            executedSqlStatements.any { it.contains("ALTER TABLE media_interactions ADD COLUMN profileId TEXT NOT NULL DEFAULT 'default'") }
        )
        assertTrue(
            "Must create index on media_interactions (profileId, timestamp)",
            executedSqlStatements.any { it.contains("index_media_interactions_profileId_timestamp") }
        )

        // 4. Program reminders profile isolation
        assertTrue(
            "Must add profileId column to program_reminders",
            executedSqlStatements.any { it.contains("ALTER TABLE program_reminders ADD COLUMN profileId TEXT NOT NULL DEFAULT 'default'") }
        )
        assertTrue(
            "Must create index on program_reminders profileId",
            executedSqlStatements.any { it.contains("index_program_reminders_profileId") }
        )

        // 5. Playback progress table and backfill from downloads
        assertTrue(
            "Must create playback_progress table",
            executedSqlStatements.any {
                it.contains("CREATE TABLE IF NOT EXISTS playback_progress") &&
                it.contains("PRIMARY KEY(profileId, downloadId)") &&
                it.contains("FOREIGN KEY(profileId) REFERENCES profiles(id) ON DELETE CASCADE") &&
                it.contains("FOREIGN KEY(downloadId) REFERENCES downloads(id) ON DELETE CASCADE")
            }
        )
        assertTrue(
            "Must backfill playback_progress from downloads",
            executedSqlStatements.any {
                it.contains("playback_progress") &&
                it.contains("FROM downloads") &&
                it.contains("lastWatchedAt > 0 OR lastPlaybackPositionMs > 0 OR watchedCompleted = 1 OR hideFromContinueWatching = 1")
            }
        )
    }

    @Test
    fun migrationChain_6_to_11_executesSequentially() {
        val executedStatements = mutableListOf<String>()

        val handler = InvocationHandler { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedStatements.add(args[0] as String)
            }
            null
        }

        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
            handler
        ) as SupportSQLiteDatabase

        // Execute chain 6 -> 7 -> 8 -> 9 -> 10 -> 11
        NisTvDatabase.MIGRATION_6_7.migrate(dbProxy)
        NisTvDatabase.MIGRATION_7_8.migrate(dbProxy)
        NisTvDatabase.MIGRATION_8_9.migrate(dbProxy)
        NisTvDatabase.MIGRATION_9_10.migrate(dbProxy)
        NisTvDatabase.MIGRATION_10_11.migrate(dbProxy)

        assertTrue("Chain migrations executed SQL statements", executedStatements.size > 20)
    }
}
