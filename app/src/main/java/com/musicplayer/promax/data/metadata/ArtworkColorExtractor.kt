package com.musicplayer.promax.data.metadata

import android.graphics.BitmapFactory
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

data class ArtworkPalette(
    val primaryArgb: Int,
    val secondaryArgb: Int,
    val glowArgb: Int
)

/**
 * Extracts a legible, vivid color palette from local album artwork without external dependencies.
 * Pure pixel-clustering runs cleanly on downsampled bitmaps and is unit-tested on JVM.
 */
object ArtworkColorExtractor {
    private const val MAX_SAMPLE_DIM = 48
    private const val HUE_BUCKETS = 24

    suspend fun extractFromFile(artworkPath: String?): ArtworkPalette? {
        val file = artworkPath?.takeIf(String::isNotBlank)?.let(::File)?.takeIf(File::isFile) ?: return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = ArtworkSampling.calculateSampleSize(bounds.outWidth, bounds.outHeight, MAX_SAMPLE_DIM)
                }
                val bitmap = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return@runCatching null
                try {
                    val width = bitmap.width
                    val height = bitmap.height
                    val pixels = IntArray(width * height)
                    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                    extractAccentPalette(pixels)
                } finally {
                    bitmap.recycle()
                }
            }.getOrNull()
        }
    }

    fun extractAccentPalette(pixels: IntArray): ArtworkPalette? {
        if (pixels.isEmpty()) return null
        val bucketWeight = FloatArray(HUE_BUCKETS)
        val bucketHueSum = FloatArray(HUE_BUCKETS)
        val bucketSatSum = FloatArray(HUE_BUCKETS)
        val hsv = FloatArray(3)
        var totalSamples = 0

        for (argb in pixels) {
            if (Color.alpha(argb) < 128) continue
            Color.colorToHSV(argb, hsv)
            val hue = hsv[0]
            val sat = hsv[1]
            val value = hsv[2]
            if (value < 0.12f || (value > 0.96f && sat < 0.12f) || sat < 0.16f) continue
            val bucket = ((hue / 360f) * HUE_BUCKETS).toInt().coerceIn(0, HUE_BUCKETS - 1)
            val weight = (sat * 0.75f + value * 0.25f)
            bucketWeight[bucket] += weight
            bucketHueSum[bucket] += hue * weight
            bucketSatSum[bucket] += sat * weight
            totalSamples++
        }

        if (totalSamples == 0) return null

        var bestBucket = 0
        for (i in 1 until HUE_BUCKETS) {
            if (bucketWeight[i] > bucketWeight[bestBucket]) bestBucket = i
        }
        val bestWeight = bucketWeight[bestBucket]
        if (bestWeight <= 0f) return null

        val dominantHue = (bucketHueSum[bestBucket] / bestWeight).coerceIn(0f, 360f)
        val dominantSat = (bucketSatSum[bestBucket] / bestWeight).coerceIn(0.48f, 0.88f)
        val primaryArgb = Color.HSVToColor(floatArrayOf(dominantHue, dominantSat, 0.92f))

        // Pick a harmonic secondary bucket distinct enough from dominant, or shift luminance/hue softly.
        var secondBucket = -1
        for (i in 0 until HUE_BUCKETS) {
            if (bucketWeight[i] <= 0f || i == bestBucket) continue
            val distance = abs(i - bestBucket).let { minOf(it, HUE_BUCKETS - it) }
            if (distance >= 3 && (secondBucket == -1 || bucketWeight[i] > bucketWeight[secondBucket])) {
                secondBucket = i
            }
        }
        val secondaryArgb = if (secondBucket != -1 && bucketWeight[secondBucket] > bestWeight * 0.22f) {
            val w = bucketWeight[secondBucket]
            val h = (bucketHueSum[secondBucket] / w).coerceIn(0f, 360f)
            val s = (bucketSatSum[secondBucket] / w).coerceIn(0.45f, 0.85f)
            Color.HSVToColor(floatArrayOf(h, s, 0.96f))
        } else {
            val shiftedHue = (dominantHue + 28f) % 360f
            Color.HSVToColor(floatArrayOf(shiftedHue, (dominantSat * 0.82f).coerceIn(0.35f, 0.8f), 0.98f))
        }
        val glowArgb = Color.HSVToColor(floatArrayOf(dominantHue, (dominantSat * 0.72f).coerceIn(0.35f, 0.75f), 0.55f))
        return ArtworkPalette(primaryArgb = primaryArgb, secondaryArgb = secondaryArgb, glowArgb = glowArgb)
    }
}
