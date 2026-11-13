package com.musicplayer.promax.data.metadata

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.musicplayer.promax.data.local.MusicLibraryDatabase
import com.musicplayer.promax.data.local.MusicTrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/** Local-only metadata helpers. Artwork is extracted from the audio file and cached in app files. */
class TrackMetadataRepository(context: Context) {
    private val appContext = context.applicationContext
    private val trackDao = MusicLibraryDatabase.getInstance(appContext).musicTrackDao()
    private val artworkDirectory = File(appContext.filesDir, "album_artwork").apply { mkdirs() }

    suspend fun extractArtwork(track: MusicTrackEntity): String? = withContext(Dispatchers.IO) {
        track.artworkFilePath?.let { existingPath ->
            if (existingPath.isEmpty()) return@withContext null
            if (File(existingPath).isFile) return@withContext existingPath
        }
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(appContext, Uri.parse(track.contentUri))
            val embeddedTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val embeddedArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val embeddedAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val embeddedDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            trackDao.updateEmbeddedMetadata(track.trackKey, embeddedTitle, embeddedArtist, embeddedAlbum, embeddedDuration)
            val embedded = retriever.embeddedPicture
            if (embedded == null) {
                trackDao.updateArtworkPath(track.trackKey, "")
                return@withContext null
            }
            if (embedded.size > MAX_ARTWORK_BYTES) {
                trackDao.updateArtworkPath(track.trackKey, "")
                return@withContext null
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(embedded, 0, embedded.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0 || bounds.outWidth > 12_000 || bounds.outHeight > 12_000) {
                trackDao.updateArtworkPath(track.trackKey, "")
                return@withContext null
            }
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = ArtworkSampling.calculateSampleSize(bounds.outWidth, bounds.outHeight, MAX_ARTWORK_DIMENSION)
                inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
            }
            val bitmap = BitmapFactory.decodeByteArray(embedded, 0, embedded.size, decodeOptions)
            if (bitmap == null) {
                trackDao.updateArtworkPath(track.trackKey, "")
                return@withContext null
            }
            val safeName = sha256(track.trackKey)
            val target = File(artworkDirectory, "$safeName.jpg")
            val temporary = File(artworkDirectory, "$safeName.tmp")
            val compressed = try {
                temporary.outputStream().use { output ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, output)
                }
            } finally {
                bitmap.recycle()
            }
            if (!compressed) {
                temporary.delete()
                return@withContext null
            }
            if (target.exists()) target.delete()
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }
            trackDao.updateArtworkPath(track.trackKey, target.absolutePath)
            target.absolutePath
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun prefetchMissingArtwork(limit: Int = 500) = withContext(Dispatchers.IO) {
        val pending = trackDao.tracksMissingArtwork(limit)
        for (track in pending) {
            if (!currentCoroutineContext().isActive) break
            extractArtwork(track)
        }
    }

    suspend fun saveLyricsFromUri(trackKey: String, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val text = appContext.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                val buffer = CharArray(8_192)
                val result = StringBuilder()
                while (true) {
                    val count = reader.read(buffer)
                    if (count < 0) break
                    require(result.length + count <= MAX_LRC_CHARACTERS) { "LRC file is too large" }
                    result.append(buffer, 0, count)
                }
                result.toString()
            } ?: error("Unable to read the selected lyrics file")
            val parsed = LrcParser.parse(text)
            require(parsed.isNotEmpty()) { "No valid LRC timestamps were found" }
            trackDao.updateLyrics(trackKey, text)
            parsed.size
        }
    }

    suspend fun updateDisplayMetadata(trackKey: String, title: String, artist: String, album: String) =
        withContext(Dispatchers.IO) {
            trackDao.updateDisplayMetadata(
                trackKey = trackKey,
                title = title.trim().takeIf(String::isNotEmpty),
                artist = artist.trim().takeIf(String::isNotEmpty),
                album = album.trim().takeIf(String::isNotEmpty)
            )
        }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    companion object {
        private const val MAX_LRC_CHARACTERS = 1_000_000
        private const val MAX_ARTWORK_BYTES = 20 * 1024 * 1024
        private const val MAX_ARTWORK_DIMENSION = 1_200
    }
}
