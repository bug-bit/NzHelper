package me.neko.nzhelper.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val timestampIso: String,
    val duration: Int,
    val remark: String,
    val rating: Float,
    val climax: Boolean,
    val categoryId: String,
    val tagIdsJson: String,
    val location: String,
    val watchedMovie: Boolean,
    val mood: String,
    val props: String
)
