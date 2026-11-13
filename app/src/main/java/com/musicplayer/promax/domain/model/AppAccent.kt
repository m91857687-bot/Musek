package com.musicplayer.promax.domain.model

enum class AppAccent(val hex: Long) {
    ELECTRIC_BLUE(0xFF168BFF),
    VIOLET(0xFF9B72FF),
    EMERALD(0xFF20C997);

    companion object {
        fun fromStoredValue(value: String?): AppAccent = entries.firstOrNull { it.name == value } ?: ELECTRIC_BLUE
    }
}
