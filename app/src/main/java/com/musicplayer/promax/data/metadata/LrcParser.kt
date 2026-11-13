package com.musicplayer.promax.data.metadata

/** Parses standard LRC timestamps, including multiple timestamps on one lyric line. */
data class TimedLyric(val timeMs: Long, val text: String)

object LrcParser {
    private val timestampRegex = Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?]")

    fun parse(content: String): List<TimedLyric> = content.lineSequence()
        .flatMap { line ->
            val matches = timestampRegex.findAll(line).toList()
            val lyricText = timestampRegex.replace(line, "").trim()
            matches.mapNotNull { match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
                val seconds = match.groupValues[2].toLongOrNull() ?: return@mapNotNull null
                if (seconds !in 0..59) return@mapNotNull null
                val fractionRaw = match.groupValues.getOrNull(3).orEmpty()
                val fractionMs = when (fractionRaw.length) {
                    1 -> (fractionRaw.toLongOrNull() ?: 0L) * 100L
                    2 -> (fractionRaw.toLongOrNull() ?: 0L) * 10L
                    3 -> fractionRaw.toLongOrNull() ?: 0L
                    else -> 0L
                }
                if (lyricText.isBlank()) null
                else TimedLyric((minutes * 60_000L) + (seconds * 1_000L) + fractionMs, lyricText)
            }
        }
        .sortedBy { it.timeMs }
        .toList()

    fun currentIndex(lines: List<TimedLyric>, positionMs: Long): Int {
        var low = 0
        var high = lines.lastIndex
        var result = -1
        while (low <= high) {
            val middle = (low + high) ushr 1
            if (lines[middle].timeMs <= positionMs) {
                result = middle
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        return result
    }
}
