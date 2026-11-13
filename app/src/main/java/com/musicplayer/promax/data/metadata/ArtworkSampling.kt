package com.musicplayer.promax.data.metadata

/** Computes a power-of-two decode sample so neither decoded dimension exceeds [maxDimension]. */
object ArtworkSampling {
    fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        require(width > 0) { "width must be positive" }
        require(height > 0) { "height must be positive" }
        require(maxDimension > 0) { "maxDimension must be positive" }

        var sample = 1
        while (width / sample > maxDimension || height / sample > maxDimension) {
            sample *= 2
        }
        return sample
    }
}
