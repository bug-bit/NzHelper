package me.neko.nzhelper.data

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

data class Session(
    @SerializedName("timestamp") val timestamp: LocalDateTime,
    @SerializedName("duration") val duration: Int,
    @SerializedName("remark") val remark: String,
    @SerializedName("location") val location: String,
    @SerializedName("watchedMovie") val watchedMovie: Boolean,
    @SerializedName("climax") val climax: Boolean,
    @SerializedName("rating") val rating: Float,
    @SerializedName("mood") val mood: String,
    @SerializedName("props") val props: String
)