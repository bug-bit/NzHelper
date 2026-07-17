package me.neko.nzhelper.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import me.neko.nzhelper.core.database.entity.WebDavConfigEntity

@Dao
interface WebDavConfigDao {

    @Query("SELECT * FROM webdav_config WHERE id = 0")
    suspend fun get(): WebDavConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: WebDavConfigEntity)
}
