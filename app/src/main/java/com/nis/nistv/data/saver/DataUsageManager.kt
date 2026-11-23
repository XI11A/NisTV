package com.nis.nistv.data.saver

import com.nis.nistv.core.database.dao.DataUsageDailyDao
import com.nis.nistv.core.database.entity.DataUsageDailyEntity
import com.nis.nistv.core.network.NetworkMonitor
import com.nis.nistv.core.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DataUsageManager(
    private val dataUsageDailyDao: DataUsageDailyDao,
    private val networkMonitor: NetworkMonitor,
    private val userPreferences: UserPreferences,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun getTodayDate(): String = dateFormat.format(Date())

    fun recordStreamBytes(bytes: Long) {
        if (bytes <= 0L) return
        val today = getTodayDate()
        val networkType = networkMonitor.getActiveNetworkType()

        scope.launch {
            try {
                dataUsageDailyDao.addStreamBytes(today, networkType, bytes)
            } catch (_: Exception) {}
        }
    }

    fun recordDownloadBytes(bytes: Long, networkType: String? = null) {
        if (bytes <= 0L) return
        val today = getTodayDate()
        val resolvedType = networkType ?: networkMonitor.getActiveNetworkType()

        scope.launch {
            try {
                dataUsageDailyDao.addDownloadBytes(today, resolvedType, bytes)
            } catch (_: Exception) {}
        }
    }

    fun getTodayUsage(): Flow<Pair<Long, Long>> {
        val today = getTodayDate()
        return dataUsageDailyDao.getUsageForDate(today).map { records ->
            var stream = 0L
            var download = 0L
            for (r in records) {
                stream += r.streamBytes
                download += r.downloadBytes
            }
            Pair(stream, download)
        }.flowOn(Dispatchers.IO)
    }

    fun getRecentUsage(days: Int): Flow<List<DataUsageDailyEntity>> {
        val cal = Calendar.getInstance()
        val end = dateFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -days)
        val start = dateFormat.format(cal.time)

        return dataUsageDailyDao.getUsageBetween(start, end).flowOn(Dispatchers.IO)
    }

    fun getMonthlyCellularUsageBytes(): Flow<Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val startOfMonth = dateFormat.format(cal.time)
        val today = getTodayDate()

        return dataUsageDailyDao.getUsageBetween(startOfMonth, today).map { records ->
            records.filter { it.networkType.equals("CELLULAR", ignoreCase = true) }
                .sumOf { it.streamBytes + it.downloadBytes }
        }.flowOn(Dispatchers.IO)
    }

    val cellularThresholdAlert: Flow<ThresholdAlert> = combine(
        getMonthlyCellularUsageBytes(),
        userPreferences.monthlyCellularLimitGb
    ) { usedBytes, limitGb ->
        val limitBytes = limitGb.toLong() * 1024L * 1024L * 1024L
        DataSaverHelper.calculateThresholdAlert(usedBytes, limitBytes)
    }.flowOn(Dispatchers.IO)
}
