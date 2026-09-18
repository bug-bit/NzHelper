package me.neko.nzhelper.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import me.neko.nzhelper.core.database.entity.SessionEntity

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions ORDER BY timestampIso DESC")
    suspend fun getAll(): List<SessionEntity>

    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun count(): Int

    @Query(
        "SELECT COUNT(*) AS recordCount, " +
                "COALESCE(SUM(duration), 0) AS totalSeconds, " +
                "MIN(timestampIso) AS firstTimestampIso " +
                "FROM sessions"
    )
    suspend fun summary(): SessionSummaryRow

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<SessionEntity>)

    @Query("DELETE FROM sessions WHERE timestampIso IN (:keys)")
    suspend fun deleteByKeys(keys: List<String>): Int

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}

data class SessionSummaryRow(
    val recordCount: Int,
    val totalSeconds: Int,
    val firstTimestampIso: String?
)