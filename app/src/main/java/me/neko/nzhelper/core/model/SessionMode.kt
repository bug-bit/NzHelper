package me.neko.nzhelper.core.model

import androidx.compose.runtime.Immutable

@Immutable
enum class SessionMode(val key: String, val label: String) {
    SOLO_MALE("SOLO_MALE", "男性"),
    SOLO_FEMALE("SOLO_FEMALE", "女性"),
    PAIR("PAIR", "双人");

    val isPair: Boolean get() = this == PAIR

    companion object {
        fun fromKey(key: String?): SessionMode =
            entries.firstOrNull { it.key == key } ?: SOLO_MALE
    }
}

@Immutable
enum class PartnerGender(val key: String, val label: String) {
    MALE("male", "男"),
    FEMALE("female", "女");

    companion object {
        fun fromKey(key: String?): PartnerGender? =
            entries.firstOrNull { it.key == key }
    }
}

@Immutable
enum class Contraception(val key: String, val label: String) {
    NONE("none", "无"),
    CONDOM("condom", "避孕套"),
    PILL("pill", "避孕药品"),
    OTHER("other", "其他");

    companion object {
        fun fromKey(key: String?): Contraception =
            entries.firstOrNull { it.key == key } ?: NONE
    }
}
