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

    // ── 记录模式（男性单人 / 女性单人 / 双人）──
    @SerializedName("mode") val mode: String = SessionMode.SOLO_MALE.key,
    @SerializedName("climaxCount") val climaxCount: Int = 0,
    @SerializedName("partnerClimaxCount") val partnerClimaxCount: Int = 0,
    @SerializedName("partnerGender") val partnerGender: String = "",
    @SerializedName("partnerName") val partnerName: String = "",
    @SerializedName("contraception") val contraception: String = "",

    // ── legacy（仅兼容旧数据 / 迁移用）──
    @SerializedName("location") val location: String = "",
    @SerializedName("watchedMovie") val watchedMovie: Boolean = false,
    @SerializedName("mood") val mood: String = "",
    @SerializedName("props") val props: String = ""
) {
    companion object {
        const val DEFAULT_CATEGORY_ID: String = "cat_self"
    }

    fun normalized(): Session =
        if (climaxCount > 0 || !climax) this else copy(climaxCount = 1)
}

fun Session.sessionMode(): SessionMode = SessionMode.fromKey(mode)

@Immutable
data class RecycleBinItem(
    @SerializedName("session") val session: Session,
    @SerializedName("deletedTimestamp") val deletedTimestamp: Long = System.currentTimeMillis()
)

@Immutable
data class WebDavBackupPayload(
    @SerializedName("version") val version: Int = 3,
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