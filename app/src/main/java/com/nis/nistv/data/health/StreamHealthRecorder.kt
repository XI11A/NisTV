package com.nis.nistv.data.health

import com.nis.nistv.core.database.dao.StreamHealthDao
import com.nis.nistv.core.database.entity.StreamHealthEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface StreamHealthRecorder {
    fun recordSuccess(streamUrl: String, channelId: String, startupMs: Long)
    fun recordFailure(streamUrl: String, channelId: String)
    fun resetHealthForChannel(channelId: String)
}

class DefaultStreamHealthRecorder(
    private val streamHealthDao: StreamHealthDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : StreamHealthRecorder {

    override fun recordSuccess(streamUrl: String, channelId: String, startupMs: Long) {
        if (streamUrl.isBlank() || channelId.isBlank()) return
        val key = StreamHealthKeyHelper.computeStreamKey(streamUrl)
        val now = System.currentTimeMillis()

        scope.launch {
            try {
                val existing = streamHealthDao.getHealth(key)
                val newSuccessCount = (existing?.successCount ?: 0) + 1
                val newAvgStartup = if (existing != null && existing.avgStartupMs > 0 && startupMs > 0) {
                    (existing.avgStartupMs * 3 + startupMs) / 4 // Exponential moving average
                } else if (startupMs > 0) {
                    startupMs
                } else {
                    existing?.avgStartupMs ?: 0L
                }

                val updated = StreamHealthEntity(
                    streamKey = key,
                    channelId = channelId,
                    successCount = newSuccessCount,
                    failureCount = existing?.failureCount ?: 0,
                    avgStartupMs = newAvgStartup,
                    lastSuccessAt = now,
                    lastFailureAt = existing?.lastFailureAt ?: 0L,
                    consecutiveFailures = 0 // Reset consecutive failures on success
                )
                streamHealthDao.insertOrUpdate(updated)
            } catch (_: Exception) {
                // Non-blocking telemetry
            }
        }
    }

    override fun recordFailure(streamUrl: String, channelId: String) {
        if (streamUrl.isBlank() || channelId.isBlank()) return
        val key = StreamHealthKeyHelper.computeStreamKey(streamUrl)
        val now = System.currentTimeMillis()

        scope.launch {
            try {
                val existing = streamHealthDao.getHealth(key)
                val updated = StreamHealthEntity(
                    streamKey = key,
                    channelId = channelId,
                    successCount = existing?.successCount ?: 0,
                    failureCount = (existing?.failureCount ?: 0) + 1,
                    avgStartupMs = existing?.avgStartupMs ?: 0L,
                    lastSuccessAt = existing?.lastSuccessAt ?: 0L,
                    lastFailureAt = now,
                    consecutiveFailures = (existing?.consecutiveFailures ?: 0) + 1
                )
                streamHealthDao.insertOrUpdate(updated)
            } catch (_: Exception) {
                // Non-blocking telemetry
            }
        }
    }

    override fun resetHealthForChannel(channelId: String) {
        if (channelId.isBlank()) return
        scope.launch {
            try {
                streamHealthDao.resetHealthForChannel(channelId)
            } catch (_: Exception) {}
        }
    }
}
