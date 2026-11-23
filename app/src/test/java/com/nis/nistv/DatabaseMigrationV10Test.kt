package com.nis.nistv

import androidx.sqlite.db.SupportSQLiteDatabase
import com.nis.nistv.core.database.NisTvDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

class DatabaseMigrationV10Test {

    @Test
    fun migrationVersions_areConfiguredSequentially() {
        assertEquals(9, NisTvDatabase.MIGRATION_9_10.startVersion)
        assertEquals(10, NisTvDatabase.MIGRATION_9_10.endVersion)
    }

    @Test
    fun migration_9_to_10_executesExpectedSchemaStatements() {
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
        NisTvDatabase.MIGRATION_9_10.migrate(dbProxy)

        assertTrue("Executed SQL list should not be empty", executedSqlStatements.isNotEmpty())

        // 1. Stream health table
        val createStreamHealth = executedSqlStatements.find { it.contains("CREATE TABLE IF NOT EXISTS stream_health") }
        assertTrue("Must create stream_health table", createStreamHealth != null)
        assertTrue(createStreamHealth!!.contains("streamKey TEXT NOT NULL PRIMARY KEY"))
        assertTrue(createStreamHealth.contains("channelId TEXT NOT NULL"))
        assertTrue(createStreamHealth.contains("successCount INTEGER NOT NULL DEFAULT 0"))
        assertTrue(createStreamHealth.contains("failureCount INTEGER NOT NULL DEFAULT 0"))
        assertTrue(createStreamHealth.contains("avgStartupMs INTEGER NOT NULL DEFAULT 0"))
        assertTrue(createStreamHealth.contains("lastSuccessAt INTEGER NOT NULL DEFAULT 0"))
        assertTrue(createStreamHealth.contains("lastFailureAt INTEGER NOT NULL DEFAULT 0"))
        assertTrue(createStreamHealth.contains("consecutiveFailures INTEGER NOT NULL DEFAULT 0"))
        assertTrue(
            "Must create index on channelId",
            executedSqlStatements.any { it.contains("index_stream_health_channelId") }
        )

        // 2. Data usage daily table
        val createDataUsage = executedSqlStatements.find { it.contains("CREATE TABLE IF NOT EXISTS data_usage_daily") }
        assertTrue("Must create data_usage_daily table", createDataUsage != null)
        assertTrue(createDataUsage!!.contains("date TEXT NOT NULL"))
        assertTrue(createDataUsage.contains("networkType TEXT NOT NULL"))
        assertTrue(createDataUsage.contains("streamBytes INTEGER NOT NULL DEFAULT 0"))
        assertTrue(createDataUsage.contains("downloadBytes INTEGER NOT NULL DEFAULT 0"))
        assertTrue(createDataUsage.contains("PRIMARY KEY (date, networkType)"))
        assertTrue(
            "Must create index on date",
            executedSqlStatements.any { it.contains("index_data_usage_daily_date") }
        )
    }
}
