package com.musicplayer.promax.data.metadata

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Lightweight bounded in-memory thumbnail cache that downsamples cached cover art
 * to list-item dimensions instead of decoding full 1200px bitmaps on every scroll.
 */
object ArtworkThumbnailCache {
    private const val MAX_CACHE_BYTES = 6 * 1024 * 1024

    private val cache = object : LruCache<String, Bitmap>(MAX_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    suspend fun load(path: String?, targetPx: Int): Bitmap? {
        if (path.isNullOrBlank()) return null
        val cacheKey = "$path@$targetPx"
        cache.get(cacheKey)?.let { cached ->
            if (!cached.isRecycled) return cached
        }
        return withContext(Dispatchers.IO) {
            cache.get(cacheKey)?.takeIf { !it.isRecycled }?.let { return@withContext it }
            val file = File(path)
            if (!file.isFile) return@withContext null
            val decoded = decodeSampled(file.absolutePath, targetPx) ?: return@withContext null
            cache.put(cacheKey, decoded)
            decoded
        }
    }

    fun evictAll() {
        cache.evictAll()
    }

    private fun decodeSampled(filePath: String, targetPx: Int): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(filePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

        var sampleSize = 1
        while ((bounds.outWidth / sampleSize) > targetPx * 2 || (bounds.outHeight / sampleSize) > targetPx * 2) {
            sampleSize *= 2
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        BitmapFactory.decodeFile(filePath, opts)
    }.getOrNull()
}
