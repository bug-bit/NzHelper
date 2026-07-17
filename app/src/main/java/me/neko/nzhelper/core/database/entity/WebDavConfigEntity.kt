package me.neko.nzhelper.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "webdav_config")
data class WebDavConfigEntity(
    @PrimaryKey val id: Int = 0,
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val remotePath: String = "/NzHelper",
    val autoBackup: Boolean = false,
    val lastBackupTime: Long = 0L
)
