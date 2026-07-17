package me.neko.nzhelper.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import me.neko.nzhelper.core.database.entity.RecycleBinEntity

@Dao
interface RecycleBinDao {

    @Query("SELECT * FROM recycle_bin ORDER BY deletedTimestamp DESC")
    suspend fun getAll(): List<RecycleBinEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<RecycleBinEntity>)

    @Query("DELETE FROM recycle_bin WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>): Int

    @Query("DELETE FROM recycle_bin WHERE sessionTimestampIso IN (:keys)")
    suspend fun deleteBySessionKeys(keys: List<String>): Int

    @Query("DELETE FROM recycle_bin WHERE deletedTimestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int

    @Query("DELETE FROM recycle_bin")
    suspend fun deleteAll()
}
