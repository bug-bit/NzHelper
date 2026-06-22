package me.neko.nzhelper.data

data class SessionFormState(
    val remark: String = "",
    val location: String = "",
    val watchedMovie: Boolean = false,
    val climax: Boolean = false,
    val rating: Float = 3f,
    val mood: String = "平静",
    val props: String = "手",
    val durationHour: String = "",
    val durationMinute: String = "",
    val durationSecond: String = ""
) {
    val manualDurationSeconds: Int
        get() = (durationHour.toIntOrNull() ?: 0) * 3600 +
                (durationMinute.toIntOrNull() ?: 0) * 60 +
                (durationSecond.toIntOrNull() ?: 0)
}