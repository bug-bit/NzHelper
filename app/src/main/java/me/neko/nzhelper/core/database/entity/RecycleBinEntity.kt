package me.neko.nzhelper.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recycle_bin")
data class RecycleBinEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deletedTimestamp: Long,
    val sessionTimestampIso: String,
    val sessionJson: String
)
