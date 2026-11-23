package com.nis.nistv

import androidx.sqlite.db.SupportSQLiteDatabase
import com.nis.nistv.core.database.NisTvDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

class DatabaseMigrationV8Test {

    @Test
    fun migrationVersions_areConfiguredSequentially() {
        assertEquals(7, NisTvDatabase.MIGRATION_7_8.startVersion)
        assertEquals(8, NisTvDatabase.MIGRATION_7_8.endVersion)
    }

    @Test
    fun migration_7_to_8_executesExpectedSchemaStatements() {
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
        NisTvDatabase.MIGRATION_7_8.migrate(dbProxy)

        assertTrue("Executed SQL list should not be empty", executedSqlStatements.isNotEmpty())

        // 1. Playlists table
        val createPlaylists = executedSqlStatements.find { it.contains("CREATE TABLE IF NOT EXISTS playlists") }
        assertTrue("Must create playlists table", createPlaylists != null)
        assertTrue(createPlaylists!!.contains("id TEXT NOT NULL PRIMARY KEY"))
        assertTrue(createPlaylists.contains("name TEXT NOT NULL"))
        assertTrue(createPlaylists.contains("sourceType TEXT NOT NULL"))
        assertTrue(createPlaylists.contains("url TEXT NOT NULL"))
        assertTrue(createPlaylists.contains("lastSyncedAt INTEGER NOT NULL DEFAULT 0"))
        assertTrue(createPlaylists.contains("channelCount INTEGER NOT NULL DEFAULT 0"))
        assertTrue(createPlaylists.contains("autoRefresh INTEGER NOT NULL DEFAULT 1"))
        assertTrue(createPlaylists.contains("enabled INTEGER NOT NULL DEFAULT 1"))

        // 2. Alteration on channels table
        assertTrue(
            "Must alter channels table to add playlistId",
            executedSqlStatements.any { it.contains("ALTER TABLE channels ADD COLUMN playlistId TEXT DEFAULT NULL") }
        )
        assertTrue(
            "Must index channels by playlistId",
            executedSqlStatements.any { it.contains("index_channels_playlistId") }
        )

        // 3. Program reminders table
        val createReminders = executedSqlStatements.find { it.contains("CREATE TABLE IF NOT EXISTS program_reminders") }
        assertTrue("Must create program_reminders table", createReminders != null)
        assertTrue(createReminders!!.contains("id TEXT NOT NULL PRIMARY KEY"))
        assertTrue(createReminders.contains("channelId TEXT NOT NULL"))
        assertTrue(createReminders.contains("programTitle TEXT NOT NULL"))
        assertTrue(createReminders.contains("startEpochMs INTEGER NOT NULL"))
        assertTrue(createReminders.contains("notified INTEGER NOT NULL DEFAULT 0"))

        assertTrue(
            "Must index program_reminders by channelId",
            executedSqlStatements.any { it.contains("index_program_reminders_channelId") }
        )
        assertTrue(
            "Must index program_reminders by startEpochMs",
            executedSqlStatements.any { it.contains("index_program_reminders_startEpochMs") }
        )
    }
}
