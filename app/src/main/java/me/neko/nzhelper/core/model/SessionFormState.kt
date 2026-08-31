package me.neko.nzhelper.core.model

import androidx.compose.runtime.Immutable
import java.time.LocalDateTime

@Immutable
data class SessionFormState(
    val remark: String = "",
    val categoryId: String = Session.DEFAULT_CATEGORY_ID,
    val tagIds: Set<String> = emptySet(),
    val autoTagIds: Set<String> = emptySet(),
    val mode: String = SessionMode.SOLO_MALE.key,
    val climaxCount: Int = 0,
    val partnerClimaxCount: Int = 0,
    val partnerGender: String = "",
    val partnerName: String = "",
    val contraception: String = Contraception.NONE.key,
    val partners: Set<String> = emptySet(),
    val initiator: String = "",
    val locations: Set<String> = emptySet(),
    val moods: Set<String> = emptySet(),
    val positions: Set<String> = emptySet(),
    val toys: Set<String> = emptySet(),
    val ejaculation: String = "",
    val rating: Float = 3f,
    val durationHour: String = "",
    val durationMinute: String = "",
    val durationSecond: String = "",
    val manualYear: Int = 0,
    val manualMonth: Int = 0,
    val manualDay: Int = 0,
    val manualHour: Int = 0,
    val manualMinute: Int = 0
) {
    val manualDurationSeconds: Int
        get() = (durationHour.toIntOrNull() ?: 0) * 3600 +
                (durationMinute.toIntOrNull() ?: 0) * 60 +
                (durationSecond.toIntOrNull() ?: 0)

    fun toLocalDateTime(): LocalDateTime {
        return LocalDateTime.of(
            manualYear,
            manualMonth,
            manualDay,
            manualHour,
            manualMinute,
            0,
            0
        )
    }
}