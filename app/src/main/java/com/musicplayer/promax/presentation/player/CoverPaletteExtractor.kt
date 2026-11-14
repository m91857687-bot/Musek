package com.musicplayer.promax.presentation.player

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DynamicCoverColors(
    val primaryArgb: Int,
    val secondaryArgb: Int,
    val backdropArgb: Int
)

/** Lightweight, allocation-conscious album art palette extractor using HSV scoring. */
object CoverPaletteExtractor {
    suspend fun extract(bitmap: Bitmap?, fallbackPrimaryArgb: Int, fallbackSecondaryArgb: Int): DynamicCoverColors =
        withContext(Dispatchers.Default) {
            if (bitmap == null || bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
                return@withContext DynamicCoverColors(
                    primaryArgb = fallbackPrimaryArgb,
                    secondaryArgb = fallbackSecondaryArgb,
                    backdropArgb = ColorUtils.blendARGB(fallbackPrimaryArgb, Color.BLACK, 0.55f)
                )
            }
            val scaled = Bitmap.createScaledBitmap(bitmap, 24, 24, true)
            try {
                val pixels = IntArray(24 * 24)
                scaled.getPixels(pixels, 0, 24, 0, 0, 24, 24)
                extractFromPixels(pixels, fallbackPrimaryArgb, fallbackSecondaryArgb)
            } finally {
                if (scaled !== bitmap) scaled.recycle()
            }
        }

    fun extractFromPixels(pixels: IntArray, fallbackPrimaryArgb: Int, fallbackSecondaryArgb: Int): DynamicCoverColors {
        val hsv = FloatArray(3)
        var bestVibrant = 0
        var bestVibrantScore = -1f
        var secondVibrant = 0
        var secondScore = -1f
        var avgR = 0L
        var avgG = 0L
        var avgB = 0L
        var count = 0

        for (pixel in pixels) {
            val alpha = Color.alpha(pixel)
            if (alpha < 128) continue
            avgR += Color.red(pixel)
            avgG += Color.green(pixel)
            avgB += Color.blue(pixel)
            count++

            Color.colorToHSV(pixel, hsv)
            val sat = hsv[1]
            val valBright = hsv[2]
            if (valBright < 0.14f || valBright > 0.96f || sat < 0.16f) continue
            val score = sat * 1.4f + (1f - kotlin.math.abs(valBright - 0.62f))
            if (score > bestVibrantScore) {
                if (bestVibrant != 0 && colorHueDistance(bestVibrant, pixel) > 25f) {
                    secondVibrant = bestVibrant
                    secondScore = bestVibrantScore
                }
                bestVibrantScore = score
                bestVibrant = pixel
            } else if (score > secondScore && bestVibrant != 0 && colorHueDistance(bestVibrant, pixel) > 28f) {
                secondScore = score
                secondVibrant = pixel
            }
        }

        val primary = if (bestVibrant != 0) ensureReadableAccent(bestVibrant) else fallbackPrimaryArgb
        val secondary = when {
            secondVibrant != 0 -> ensureReadableAccent(secondVibrant)
            bestVibrant != 0 -> ColorUtils.blendARGB(primary, fallbackSecondaryArgb, 0.35f)
            else -> fallbackSecondaryArgb
        }
        val avgColor = if (count > 0) {
            Color.rgb((avgR / count).toInt(), (avgG / count).toInt(), (avgB / count).toInt())
        } else {
            primary
        }
        val backdrop = ColorUtils.blendARGB(ColorUtils.blendARGB(avgColor, primary, 0.45f), Color.BLACK, 0.48f)
        return DynamicCoverColors(
            primaryArgb = primary,
            secondaryArgb = secondary,
            backdropArgb = backdrop
        )
    }

    private fun ensureReadableAccent(argb: Int): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(argb, hsl)
        hsl[1] = hsl[1].coerceIn(0.45f, 0.92f)
        hsl[2] = hsl[2].coerceIn(0.50f, 0.72f)
        return ColorUtils.HSLToColor(hsl)
    }

    private fun colorHueDistance(c1: Int, c2: Int): Float {
        val h1 = FloatArray(3).apply { Color.colorToHSV(c1, this) }[0]
        val h2 = FloatArray(3).apply { Color.colorToHSV(c2, this) }[0]
        val diff = kotlin.math.abs(h1 - h2)
        return kotlin.math.min(diff, 360f - diff)
    }
}
