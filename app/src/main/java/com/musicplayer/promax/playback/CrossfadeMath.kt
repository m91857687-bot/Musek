package com.musicplayer.promax.playback

/** Pure crossfade math helpers shared by playback service and unit tests. */
object CrossfadeMath {
    fun clampDurationSeconds(seconds: Int): Int = seconds.coerceIn(0, 12)

    fun fadeOutVolume(remainingMs: Long, fadeWindowMs: Long): Float {
        if (fadeWindowMs <= 0L) return 1f
        val ratio = (remainingMs.toFloat() / fadeWindowMs.toFloat()).coerceIn(0f, 1f)
        return (0.08f + 0.92f * ratio).coerceIn(0.08f, 1f)
    }

    fun fadeInVolume(elapsedMs: Long, fadeWindowMs: Long): Float {
        if (fadeWindowMs <= 0L) return 1f
        val ratio = (elapsedMs.toFloat() / fadeWindowMs.toFloat()).coerceIn(0f, 1f)
        return (0.08f + 0.92f * ratio).coerceIn(0.08f, 1f)
    }
}
