package com.musicplayer.promax.domain.model

enum class AppLanguage(val languageTag: String) {
    SYSTEM(""),
    ENGLISH("en"),
    ARABIC("ar"),
    SPANISH("es");

    companion object {
        fun fromStoredValue(value: String?): AppLanguage =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}
