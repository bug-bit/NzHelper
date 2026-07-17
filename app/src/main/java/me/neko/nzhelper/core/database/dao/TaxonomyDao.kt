package me.neko.nzhelper.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import me.neko.nzhelper.core.database.entity.TaxonomyEntity

@Dao
interface TaxonomyDao {

    @Query("SELECT payloadJson FROM taxonomy WHERE type = :type")
    suspend fun get(type: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: TaxonomyEntity)

    @Query("DELETE FROM taxonomy WHERE type = :type")
    suspend fun delete(type: String)
}
