package com.musicplayer.promax.data.metadata

import kotlin.test.Test
import kotlin.test.assertEquals

class LrcParserTest {
    @Test
    fun parsesCentisecondsAndSortsLines() {
        val lines = LrcParser.parse("[00:02.50]Second\n[00:01.2]First")
        assertEquals(listOf(1_200L, 2_500L), lines.map { it.timeMs })
        assertEquals(listOf("First", "Second"), lines.map { it.text })
    }

    @Test
    fun supportsMultipleTimestampsOnOneLine() {
        val lines = LrcParser.parse("[00:01.00][00:03.00]Repeated")
        assertEquals(2, lines.size)
        assertEquals(listOf(1_000L, 3_000L), lines.map { it.timeMs })
    }

    @Test
    fun findsCurrentLyricForPlaybackPosition() {
        val lines = LrcParser.parse("[00:01.00]One\n[00:02.00]Two")
        assertEquals(-1, LrcParser.currentIndex(lines, 500L))
        assertEquals(0, LrcParser.currentIndex(lines, 1_500L))
        assertEquals(1, LrcParser.currentIndex(lines, 2_000L))
    }
}
