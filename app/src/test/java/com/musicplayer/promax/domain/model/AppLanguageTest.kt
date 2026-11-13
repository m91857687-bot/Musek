package com.musicplayer.promax.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class AppLanguageTest {
    @Test
    fun storedLanguageIsRestored() {
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromStoredValue("ARABIC"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromStoredValue("ENGLISH"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromStoredValue(null))
    }

    @Test
    fun unknownLanguageFallsBackToSystem() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromStoredValue("UNKNOWN"))
    }
}
