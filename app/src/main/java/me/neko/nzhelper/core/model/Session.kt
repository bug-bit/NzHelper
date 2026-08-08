package me.neko.nzhelper.core.model

import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

@Immutable
data class Session(
    @SerializedName("timestamp") val timestamp: LocalDateTime,
    @SerializedName("duration") val duration: Int,
    @SerializedName("remark") val remark: String,
    @SerializedName("rating") val rating: Float,
    @SerializedName("climax") val climax: Boolean,
    @SerializedName("categoryId") val categoryId: String = DEFAULT_CATEGORY_ID,
    @SerializedName("tagIds") val tagIds: List<String> = emptyList(),

    // ── legacy（仅兼容旧数据 / 迁移用）──
    @SerializedName("location") val location: String = "",
    @SerializedName("watchedMovie") val watchedMovie: Boolean = false,
    @SerializedName("mood") val mood: String = "",
    @SerializedName("props") val props: String = ""
) {
    companion object {
        const val DEFAULT_CATEGORY_ID: String = "cat_self"
    }
}

@Immutable
data class RecycleBinItem(
    @SerializedName("session") val session: Session,
    @SerializedName("deletedTimestamp") val deletedTimestamp: Long = System.currentTimeMillis()
)

@Immutable
data class WebDavBackupPayload(
    @SerializedName("version") val version: Int = 2,
    @SerializedName("exportedAt") val exportedAt: Long,
    @SerializedName("sessions") val sessions: List<Session> = emptyList(),
    @SerializedName("recycleBin") val recycleBin: List<RecycleBinItem> = emptyList(),
    @SerializedName("categories") val categories: List<CategoryDef> = emptyList(),
    @SerializedName("tagGroups") val tagGroups: List<TagGroupDef> = emptyList(),
    @SerializedName("tags") val tags: List<TagDef> = emptyList(),
    @SerializedName("aiConfig") val aiConfig: Map<String, String>? = null
)

@Immutable
data class BackupModules(
    val sessions: Boolean = true,
    val recycleBin: Boolean = true,
    val taxonomy: Boolean = true,
    val aiConfig: Boolean = true
) {
    val noneSelected: Boolean get() = !sessions && !recycleBin && !taxonomy && !aiConfig

    companion object {
        val ALL = BackupModules()
    }
}