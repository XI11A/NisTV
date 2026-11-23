package com.nis.nistv

import androidx.sqlite.db.SupportSQLiteDatabase
import com.nis.nistv.core.database.NisTvDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

class DatabaseMigrationV7Test {

    @Test
    fun migrationVersions_areConfiguredSequentially() {
        assertEquals(3, NisTvDatabase.MIGRATION_3_4.startVersion)
        assertEquals(4, NisTvDatabase.MIGRATION_3_4.endVersion)

        assertEquals(4, NisTvDatabase.MIGRATION_4_5.startVersion)
        assertEquals(5, NisTvDatabase.MIGRATION_4_5.endVersion)

        assertEquals(5, NisTvDatabase.MIGRATION_5_6.startVersion)
        assertEquals(6, NisTvDatabase.MIGRATION_5_6.endVersion)

        assertEquals(6, NisTvDatabase.MIGRATION_6_7.startVersion)
        assertEquals(7, NisTvDatabase.MIGRATION_6_7.endVersion)
    }

    @Test
    fun migration_6_to_7_executesExpectedSchemaStatements() {
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
        NisTvDatabase.MIGRATION_6_7.migrate(dbProxy)

        assertTrue("Executed SQL list should not be empty", executedSqlStatements.isNotEmpty())

        // 1. Alterations on downloads table
        assertTrue(
            "Must alter downloads table to add durationMs",
            executedSqlStatements.any { it.contains("ALTER TABLE downloads ADD COLUMN durationMs INTEGER NOT NULL DEFAULT 0") }
        )
        assertTrue(
            "Must alter downloads table to add lastWatchedAt",
            executedSqlStatements.any { it.contains("ALTER TABLE downloads ADD COLUMN lastWatchedAt INTEGER NOT NULL DEFAULT 0") }
        )
        assertTrue(
            "Must alter downloads table to add watchedCompleted",
            executedSqlStatements.any { it.contains("ALTER TABLE downloads ADD COLUMN watchedCompleted INTEGER NOT NULL DEFAULT 0") }
        )
        assertTrue(
            "Must alter downloads table to add hideFromContinueWatching",
            executedSqlStatements.any { it.contains("ALTER TABLE downloads ADD COLUMN hideFromContinueWatching INTEGER NOT NULL DEFAULT 0") }
        )

        // 2. Creation of subtitle_tracks table
        val createSubtitles = executedSqlStatements.find { it.contains("CREATE TABLE IF NOT EXISTS subtitle_tracks") }
        assertTrue("Must create subtitle_tracks table", createSubtitles != null)
        assertTrue(createSubtitles!!.contains("id TEXT NOT NULL PRIMARY KEY"))
        assertTrue(createSubtitles.contains("downloadId TEXT NOT NULL"))
        assertTrue(createSubtitles.contains("path TEXT NOT NULL"))
        assertTrue(createSubtitles.contains("label TEXT NOT NULL"))
        assertTrue(createSubtitles.contains("languageTag TEXT"))
        assertTrue(createSubtitles.contains("mimeType TEXT NOT NULL"))
        assertTrue(createSubtitles.contains("isSelected INTEGER NOT NULL DEFAULT 0"))
        assertTrue(createSubtitles.contains("createdAt INTEGER NOT NULL"))
        assertTrue(createSubtitles.contains("FOREIGN KEY(downloadId) REFERENCES downloads(id) ON DELETE CASCADE"))

        // 3. Subtitles index
        assertTrue(
            "Must index subtitle_tracks by downloadId",
            executedSqlStatements.any { it.contains("index_subtitle_tracks_downloadId") }
        )
    }
}
