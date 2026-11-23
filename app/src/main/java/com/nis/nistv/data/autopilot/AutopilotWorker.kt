package com.nis.nistv.data.autopilot

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nis.nistv.NisTvApplication

class AutopilotWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = context.applicationContext as? NisTvApplication ?: return Result.failure()
        val container = app.container

        val enabledRules = container.database.autopilotRuleDao().getAllEnabledRules()
        if (enabledRules.isEmpty()) {
            return Result.success()
        }

        val autopilotManager = container.autopilotManager
        for (rule in enabledRules) {
            try {
                autopilotManager.runAutopilotForSeries(rule.seriesKey)
            } catch (_: Exception) {
                // Continue with next rule
            }
        }

        return Result.success()
    }
}
