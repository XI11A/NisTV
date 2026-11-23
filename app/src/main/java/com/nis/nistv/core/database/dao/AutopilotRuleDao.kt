package com.nis.nistv.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nis.nistv.core.database.entity.AutopilotRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AutopilotRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(rule: AutopilotRuleEntity)

    @Query("SELECT * FROM autopilot_rules WHERE seriesKey = :seriesKey LIMIT 1")
    fun getRule(seriesKey: String): Flow<AutopilotRuleEntity?>

    @Query("SELECT * FROM autopilot_rules WHERE seriesKey = :seriesKey LIMIT 1")
    suspend fun getRuleSync(seriesKey: String): AutopilotRuleEntity?

    @Query("SELECT * FROM autopilot_rules WHERE enabled = 1")
    suspend fun getAllEnabledRules(): List<AutopilotRuleEntity>

    @Query("SELECT * FROM autopilot_rules")
    fun getAllRules(): Flow<List<AutopilotRuleEntity>>

    @Query("UPDATE autopilot_rules SET lastRunAt = :lastRunAt, lastResult = :result WHERE seriesKey = :seriesKey")
    suspend fun updateRunResult(seriesKey: String, lastRunAt: Long, result: String)

    @Query("DELETE FROM autopilot_rules WHERE seriesKey = :seriesKey")
    suspend fun deleteRule(seriesKey: String)
}
