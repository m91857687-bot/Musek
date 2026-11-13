package com.musicplayer.promax.domain.model

enum class AppTheme {
    CLASSIC,
    CINEMATIC;

    companion object {
        fun fromStoredValue(value: String?): AppTheme =
            entries.firstOrNull { it.name == value } ?: CLASSIC
    }
}
