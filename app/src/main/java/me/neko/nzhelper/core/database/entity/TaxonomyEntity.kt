package me.neko.nzhelper.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "taxonomy")
data class TaxonomyEntity(
    @PrimaryKey val type: String,
    val payloadJson: String
)
