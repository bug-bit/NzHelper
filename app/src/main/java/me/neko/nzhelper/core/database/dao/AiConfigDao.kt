package me.neko.nzhelper.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import me.neko.nzhelper.core.database.entity.AiConfigEntity

@Dao
interface AiConfigDao {

    @Query("SELECT value FROM ai_config WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: AiConfigEntity)

    @Query("DELETE FROM ai_config WHERE `key` = :key")
    suspend fun delete(key: String)
}
