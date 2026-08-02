package me.neko.nzhelper.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_config")
data class AiConfigEntity(
    @PrimaryKey val key: String,
    val value: String
)
